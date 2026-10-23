package com.cameronbatten.dadmusic.band

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.cameronbatten.dadmusic.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/** Public role of this device in the current band-mode session. */
enum class BandMode { OFF, HOSTING, JOINED }

/** A peer discovered over mDNS. */
data class BandPeer(val name: String, val host: String, val port: Int)

/**
 * Band-mode orchestrator. Owns:
 * - An mDNS registration (when hosting) so other tablets on the LAN discover
 *   this device under service type `_dadmusic._tcp.`.
 * - A tiny TCP server (when hosting) that accepts followers and streams
 *   [BandSyncMessage] JSON-lines to each.
 * - An mDNS discovery (when joined) that resolves available hosts.
 * - A single TCP client (when joined) that reads JSON lines and republishes
 *   them through [incoming].
 *
 * The networking is skeletal on purpose — this is the M9 scaffold. Actual
 * two-tablet verification needs a smoke-test on real hardware.
 */
@Singleton
class BandSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val _mode = MutableStateFlow(BandMode.OFF)
    val mode: StateFlow<BandMode> = _mode.asStateFlow()

    private val _peers = MutableStateFlow<List<BandPeer>>(emptyList())
    val peers: StateFlow<List<BandPeer>> = _peers.asStateFlow()

    private val _incoming = MutableSharedFlow<BandSyncMessage>(extraBufferCapacity = 32)
    val incoming: SharedFlow<BandSyncMessage> = _incoming.asSharedFlow()

    private var hostJob: Job? = null
    private var joinJob: Job? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val hostSockets = mutableListOf<Socket>()

    /** Start hosting on a free port and advertise via mDNS. */
    fun startHosting() {
        if (_mode.value != BandMode.OFF) return
        _mode.value = BandMode.HOSTING
        hostJob = scope.launch(Dispatchers.IO) { runHost() }
    }

    /** Join the first discovered peer. */
    fun startJoining() {
        if (_mode.value != BandMode.OFF) return
        _mode.value = BandMode.JOINED
        joinJob = scope.launch(Dispatchers.IO) { runJoinDiscover() }
    }

    /** Tear everything down. */
    fun stop() {
        runCatching { registrationListener?.let(nsdManager::unregisterService) }
        runCatching { discoveryListener?.let(nsdManager::stopServiceDiscovery) }
        registrationListener = null
        discoveryListener = null
        hostSockets.forEach { runCatching { it.close() } }
        hostSockets.clear()
        hostJob?.cancel()
        joinJob?.cancel()
        hostJob = null
        joinJob = null
        _peers.value = emptyList()
        _mode.value = BandMode.OFF
    }

    /** Hosting: broadcast [msg] to every connected follower. */
    fun broadcast(msg: BandSyncMessage) = scope.launch(Dispatchers.IO) {
        val line = BandSyncProtocol.encode(msg) + "\n"
        synchronized(hostSockets) {
            val dead = mutableListOf<Socket>()
            for (s in hostSockets) {
                runCatching {
                    val w = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                    w.write(line); w.flush()
                }.onFailure { dead += s }
            }
            hostSockets.removeAll(dead)
        }
    }

    private suspend fun runHost() {
        val server = ServerSocket(0)
        val port = server.localPort
        registerMdns(port)
        try {
            while (scope.isActive && !server.isClosed) {
                val client = server.accept()
                synchronized(hostSockets) { hostSockets += client }
            }
        } finally {
            runCatching { server.close() }
        }
    }

    private fun registerMdns(port: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = "DadMusic"
            serviceType = "_dadmusic._tcp."
            this.port = port
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        runCatching { nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
    }

    private suspend fun runJoinDiscover() {
        val resolved = MutableSharedFlow<NsdServiceInfo>(extraBufferCapacity = 8)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        scope.launch { resolved.emit(info) }
                    }
                })
            }
            override fun onServiceLost(info: NsdServiceInfo) {}
        }
        discoveryListener = listener
        runCatching {
            nsdManager.discoverServices("_dadmusic._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        }

        resolved.collect { info ->
            val host = info.host?.hostAddress ?: return@collect
            val port = info.port
            _peers.value = _peers.value + BandPeer(info.serviceName ?: "peer", host, port)
            scope.launch(Dispatchers.IO) { runClient(host, port) }
        }
    }

    private suspend fun runClient(host: String, port: Int) = withContext(Dispatchers.IO) {
        runCatching {
            Socket(host, port).use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (scope.isActive) {
                    val line = reader.readLine() ?: break
                    val msg = BandSyncProtocol.decode(line) ?: continue
                    _incoming.tryEmit(msg)
                }
            }
        }
    }
}

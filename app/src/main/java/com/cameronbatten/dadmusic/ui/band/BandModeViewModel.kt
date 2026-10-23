package com.cameronbatten.dadmusic.ui.band

import androidx.lifecycle.ViewModel
import com.cameronbatten.dadmusic.band.BandMode
import com.cameronbatten.dadmusic.band.BandPeer
import com.cameronbatten.dadmusic.band.BandSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BandModeViewModel @Inject constructor(
    private val manager: BandSyncManager,
) : ViewModel() {
    val mode: StateFlow<BandMode> = manager.mode
    val peers: StateFlow<List<BandPeer>> = manager.peers

    fun startHosting() = manager.startHosting()
    fun startJoining() = manager.startJoining()
    fun stop() = manager.stop()
}

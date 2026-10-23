package com.cameronbatten.dadmusic

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.cameronbatten.dadmusic.pedal.PedalBindings
import com.cameronbatten.dadmusic.pedal.PedalEventBus
import com.cameronbatten.dadmusic.preferences.UserPreferences
import com.cameronbatten.dadmusic.ui.theme.DadMusicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    @Inject lateinit var pedalBus: PedalEventBus

    /** Cached decoded bindings; kept in sync with the preference flow. */
    private val _bindings = MutableStateFlow<List<com.cameronbatten.dadmusic.pedal.PedalBinding>>(emptyList())
    private val bindings: StateFlow<List<com.cameronbatten.dadmusic.pedal.PedalBinding>> =
        _bindings.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            userPreferences.pedalBindingsText.collect { text ->
                _bindings.value = PedalBindings.decode(text)
            }
        }

        setContent {
            DadMusicTheme {
                MainNavHost()
            }
        }
    }

    /**
     * Pedal integration (M6): BT HID page-turners (AirTurn, PageFlip, etc.)
     * fan their presses in as KeyEvents through the hardware-keyboard path.
     * We look the keyCode up in the user's current [PedalBinding]s and, on a
     * hit, consume the event and dispatch an action through [PedalEventBus].
     *
     * Note: ACTION_DOWN only — ACTION_UP for the same binding is swallowed
     * silently so a sustained press isn't interpreted as two actions.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val action = PedalBindings.actionFor(bindings.value, keyCode)
        if (action != null) {
            pedalBus.emit(action)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // Swallow key-up so the system doesn't also treat a bound pedal press
        // as a focus-change / tab-switch keystroke.
        if (PedalBindings.actionFor(bindings.value, keyCode) != null) return true
        return super.onKeyUp(keyCode, event)
    }
}

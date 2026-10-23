package com.cameronbatten.dadmusic.pedal

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide fan-out for pedal events. [MainActivity] publishes to this bus
 * from its `onKeyDown` after resolving the keycode through [PedalBindings];
 * ViewModels (ZenPlayerViewModel, NavigatorViewModel) subscribe to react.
 *
 * Replay = 0 so a pedal press that arrives before any subscriber is dropped —
 * we never want to replay an old action onto a newly-mounted screen.
 */
@Singleton
class PedalEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<PedalAction>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<PedalAction> = _events.asSharedFlow()

    fun emit(action: PedalAction) {
        _events.tryEmit(action)
    }
}

package com.cameronbatten.dadmusic.pedal

/**
 * What a pedal press does. Mapped from an Android `KeyEvent.keyCode` to one of
 * these actions via the user's current [PedalBinding]s.
 *
 * The `label` is a user-facing short string; it lives here (not strings.xml)
 * so test and persistence code can match on a stable identifier.
 */
enum class PedalAction(val label: String) {
    NEXT("Next song"),
    PREV("Previous song"),
    PLAY_PAUSE("Play / pause"),
    SCROLL_UP("Scroll lyrics up"),
    SCROLL_DOWN("Scroll lyrics down"),
}

data class PedalBinding(val keyCode: Int, val action: PedalAction)

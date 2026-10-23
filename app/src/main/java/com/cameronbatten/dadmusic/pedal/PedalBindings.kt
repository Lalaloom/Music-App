package com.cameronbatten.dadmusic.pedal

/**
 * Pure encode/decode helpers for the pedal-binding persistence format.
 *
 * Serialised format: semicolon-separated `keyCode:ACTION` pairs.
 *   `92:SCROLL_DOWN;93:SCROLL_UP;126:PLAY_PAUSE`
 *
 * - KeyEvent keyCode is just an Int so we avoid dragging in `android.view.KeyEvent`
 *   at this layer — the test suite stays JVM-only.
 * - Unknown actions / malformed pairs are dropped silently; a corrupt
 *   preference should not crash the app.
 * - At most one binding per `keyCode`; later entries win on collision.
 */
object PedalBindings {

    fun encode(bindings: Collection<PedalBinding>): String =
        bindings.joinToString(";") { "${it.keyCode}:${it.action.name}" }

    fun decode(text: String?): List<PedalBinding> {
        if (text.isNullOrBlank()) return emptyList()
        val out = mutableMapOf<Int, PedalAction>()
        for (chunk in text.split(';')) {
            val parts = chunk.split(':', limit = 2)
            if (parts.size != 2) continue
            val code = parts[0].trim().toIntOrNull() ?: continue
            val action = runCatching { PedalAction.valueOf(parts[1].trim()) }.getOrNull() ?: continue
            out[code] = action
        }
        return out.map { (c, a) -> PedalBinding(c, a) }
    }

    /** Convenience: pull the action bound to [keyCode] from a flat list of bindings. */
    fun actionFor(bindings: Collection<PedalBinding>, keyCode: Int): PedalAction? =
        bindings.firstOrNull { it.keyCode == keyCode }?.action
}

package com.cameronbatten.dadmusic.band

/**
 * Hand-rolled JSON-lines codec for [BandSyncMessage]. No external library —
 * one message per line, each line a well-formed JSON object with a `kind`
 * discriminator and the relevant payload fields.
 *
 * The grammar is tight so a corrupted line is cheap to reject.
 */
object BandSyncProtocol {

    /** Serialize [msg] to a single-line JSON string (no trailing newline). */
    fun encode(msg: BandSyncMessage): String = when (msg) {
        is BandSyncMessage.Play ->
            """{"kind":"play","songUri":${quote(msg.songUri)},"positionMs":${msg.positionMs}}"""
        is BandSyncMessage.Pause ->
            """{"kind":"pause","positionMs":${msg.positionMs}}"""
        is BandSyncMessage.Heartbeat ->
            """{"kind":"heartbeat","epochMs":${msg.epochMs}}"""
    }

    /** Parse a single JSON line. Returns null for malformed or unknown input. */
    fun decode(line: String): BandSyncMessage? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val kind = extract(trimmed, "kind") ?: return null
        return when (kind) {
            "play" -> {
                val uri = extract(trimmed, "songUri") ?: return null
                val pos = extractNumber(trimmed, "positionMs") ?: return null
                BandSyncMessage.Play(uri, pos)
            }
            "pause" -> {
                val pos = extractNumber(trimmed, "positionMs") ?: return null
                BandSyncMessage.Pause(pos)
            }
            "heartbeat" -> {
                val ep = extractNumber(trimmed, "epochMs") ?: return null
                BandSyncMessage.Heartbeat(ep)
            }
            else -> null
        }
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(c)
        }
        sb.append('"')
        return sb.toString()
    }

    private fun unquote(json: String): String {
        require(json.startsWith("\"") && json.endsWith("\""))
        val body = json.substring(1, json.length - 1)
        val sb = StringBuilder()
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c == '\\' && i + 1 < body.length) {
                when (body[i + 1]) {
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> sb.append(body[i + 1])
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    private fun extract(line: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")")
        val m = pattern.find(line) ?: return null
        return unquote(m.groupValues[1])
    }

    private fun extractNumber(line: String, key: String): Long? {
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        val m = pattern.find(line) ?: return null
        return m.groupValues[1].toLongOrNull()
    }
}

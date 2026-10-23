package com.cameronbatten.dadmusic.setlist.sharing

/**
 * Wire-format for shared setlists (M10). Encoded as a UTF-8 `.setlist.json`
 * file with one top-level object:
 *
 * ```
 * {
 *   "name": "Set A",
 *   "songs": [
 *     { "title": "Hotel California", "artist": "Eagles" },
 *     { "title": "Wonderwall", "artist": "Oasis" }
 *   ]
 * }
 * ```
 *
 * Hand-rolled encoder/decoder — no external dep. Matches on (title, artist);
 * duration and album are intentionally not round-tripped so the receiving
 * device can match against its own tagged files.
 */
data class SharedSetlistSong(val title: String, val artist: String?)
data class SharedSetlist(val name: String, val songs: List<SharedSetlistSong>)

object SharedSetlistCodec {

    fun encode(sl: SharedSetlist): String {
        val sb = StringBuilder()
        sb.append("{\"name\":").append(quote(sl.name)).append(",\"songs\":[")
        for ((i, s) in sl.songs.withIndex()) {
            if (i > 0) sb.append(',')
            sb.append("{\"title\":").append(quote(s.title))
            s.artist?.let { sb.append(",\"artist\":").append(quote(it)) }
            sb.append('}')
        }
        sb.append("]}")
        return sb.toString()
    }

    fun decode(text: String): SharedSetlist? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val name = extractString(trimmed, "name") ?: return null
        val songsArray = extractArray(trimmed, "songs") ?: return null
        val songs = splitObjects(songsArray).mapNotNull { obj ->
            val t = extractString(obj, "title") ?: return@mapNotNull null
            SharedSetlistSong(t, extractString(obj, "artist"))
        }
        return SharedSetlist(name, songs)
    }

    // ── tiny JSON helpers (string + array value only) ─────────────────────────

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

    private fun unquote(raw: String): String {
        require(raw.startsWith("\"") && raw.endsWith("\""))
        val body = raw.substring(1, raw.length - 1)
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

    private fun extractString(scope: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")")
        val m = pattern.find(scope) ?: return null
        return unquote(m.groupValues[1])
    }

    /** Returns the inner contents of a JSON array value, excluding the brackets. */
    private fun extractArray(scope: String, key: String): String? {
        val idx = scope.indexOf("\"$key\"")
        if (idx < 0) return null
        val colon = scope.indexOf(':', idx)
        if (colon < 0) return null
        var cursor = colon + 1
        while (cursor < scope.length && scope[cursor].isWhitespace()) cursor++
        if (cursor >= scope.length || scope[cursor] != '[') return null
        var depth = 1
        var i = cursor + 1
        while (i < scope.length) {
            val c = scope[i]
            if (c == '[') depth++
            else if (c == ']') {
                depth--
                if (depth == 0) return scope.substring(cursor + 1, i)
            } else if (c == '"') {
                i++
                while (i < scope.length && !(scope[i] == '"' && scope[i - 1] != '\\')) i++
            }
            i++
        }
        return null
    }

    /** Split the array body into balanced {...} chunks. */
    private fun splitObjects(arrayBody: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = -1
        var i = 0
        while (i < arrayBody.length) {
            val c = arrayBody[i]
            when (c) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        out += arrayBody.substring(start, i + 1)
                        start = -1
                    }
                }
                '"' -> {
                    i++
                    while (i < arrayBody.length && !(arrayBody[i] == '"' && arrayBody[i - 1] != '\\')) i++
                }
            }
            i++
        }
        return out
    }
}

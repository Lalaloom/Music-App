package com.cameronbatten.dadmusic.data.scan

data class ParsedFilename(val title: String?, val artist: String?)

object FilenameParser {
    fun parse(displayName: String): ParsedFilename {
        if (displayName.isBlank()) {
            return ParsedFilename(null, null)
        }

        // Strip the extension (everything after the last dot)
        val baseName = if (displayName.contains('.')) {
            displayName.substringBeforeLast('.')
        } else {
            displayName
        }

        // If no base name after trimming, return nulls
        if (baseName.isBlank()) {
            return ParsedFilename(null, null)
        }

        // Split on " - " (space-hyphen-space)
        val parts = baseName.split(" - ")

        return when {
            parts.isEmpty() || parts.all { it.isBlank() } -> {
                ParsedFilename(null, null)
            }
            parts.size == 1 -> {
                // No split occurred
                val title = parts[0].trim().takeIf { it.isNotEmpty() }
                ParsedFilename(title, null)
            }
            parts.size == 2 -> {
                // One split: left is title, right is artist
                val title = parts[0].trim().takeIf { it.isNotEmpty() }
                val artist = parts[1].trim().takeIf { it.isNotEmpty() }
                ParsedFilename(title, artist)
            }
            else -> {
                // 2+ splits: title is everything before last split, artist is last split
                val artist = parts.last().trim().takeIf { it.isNotEmpty() }
                val titleParts = parts.dropLast(1)
                val title = titleParts.joinToString(" - ").trim().takeIf { it.isNotEmpty() }
                ParsedFilename(title, artist)
            }
        }
    }
}

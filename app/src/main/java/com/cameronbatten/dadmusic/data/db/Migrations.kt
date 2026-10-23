package com.cameronbatten.dadmusic.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for [AppDatabase]. Each migration's DDL is hand-crafted to
 * produce the exact table/index shape Room's generator emits at the target
 * version — see `app/schemas/com.cameronbatten.dadmusic.data.db.AppDatabase/4.json`
 * for the authoritative v4 definition we validate against.
 *
 * ### Schema history
 * - v1 (M1): `songs` only.
 * - v2 (M1 post-smoke): + `sizeBytes` on `songs` (rename detection).
 * - v3 (M2 Task 1): + `lyricsStatus`, `lyricsCachePath` on `songs`; `lyrics` table.
 * - v4 (M2 Tasks 7+9): `setlists`, `setlist_entries`, `live_queue` tables.
 *
 * ### DEFAULT values on added NOT NULL columns
 * Room's generated schema entries for these columns don't carry a default,
 * but SQLite requires one when adding a NOT NULL column to a table with
 * existing rows. Room's own `validateMigration` only inspects affinity +
 * nullability + primary-key, so including a `DEFAULT` in the ALTER is safe.
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `sizeBytes` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `lyricsStatus` TEXT NOT NULL DEFAULT 'MISSING'")
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `lyricsCachePath` TEXT")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `lyrics` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`songId` INTEGER NOT NULL, " +
                "`rawLrc` TEXT NOT NULL, " +
                "`hasTimings` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`modifiedUtc` INTEGER NOT NULL, " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_lyrics_songId` ON `lyrics` (`songId`)",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `setlists` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`sourceFolderUri` TEXT, " +
                "`orderIndex` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`modifiedUtc` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_setlists_sourceFolderUri` ON `setlists` (`sourceFolderUri`)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `setlist_entries` " +
                "(`setlistId` INTEGER NOT NULL, " +
                "`songId` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "PRIMARY KEY(`setlistId`, `songId`), " +
                "FOREIGN KEY(`setlistId`) REFERENCES `setlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_setlist_entries_songId` ON `setlist_entries` (`songId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_setlist_entries_setlistId` ON `setlist_entries` (`setlistId`)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `live_queue` " +
                "(`position` INTEGER NOT NULL, " +
                "`songId` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`parentSetlistId` INTEGER, " +
                "PRIMARY KEY(`position`), " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_live_queue_songId` ON `live_queue` (`songId`)",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `chord_sheets` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`uri` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`artist` TEXT, " +
                "`folderUri` TEXT, " +
                "`folderName` TEXT, " +
                "`dateAdded` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_chord_sheets_uri` ON `chord_sheets` (`uri`)",
        )
    }
}

val ALL_MIGRATIONS: Array<Migration> =
    arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

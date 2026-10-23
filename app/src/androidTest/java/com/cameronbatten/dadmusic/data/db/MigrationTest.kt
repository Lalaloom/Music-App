package com.cameronbatten.dadmusic.data.db

import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    /**
     * Hand-seed a v1 database with the smallest valid shape, then open it through
     * Room with the migration ladder and assert that the pre-migration row is
     * still readable at v4 and the fields added along the way have their defaults.
     *
     * We build the v1 DB directly through SupportSQLiteOpenHelper rather than
     * MigrationTestHelper's `createDatabase`, which would require checked-in
     * schema JSONs for every historical version. Room's `room_master_table`
     * row is populated with v1's identity hash so Room accepts the DB as
     * genuinely at v1 when the migration ladder runs.
     */
    @Test
    fun v1_to_v4_preservesSongRowAndFillsDefaults() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.deleteDatabase(dbName)

        // --- 1. Build a v1-shape DB by hand via SupportSQLiteOpenHelper ---
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(ctx)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `songs` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`artist` TEXT, " +
                            "`album` TEXT, " +
                            "`uri` TEXT NOT NULL, " +
                            "`fileType` TEXT NOT NULL, " +
                            "`durationMs` INTEGER NOT NULL, " +
                            "`dateAdded` INTEGER NOT NULL, " +
                            "`confirmed` INTEGER NOT NULL)",
                    )
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_uri` ON `songs` (`uri`)")
                    db.execSQL(
                        "INSERT INTO songs(title, artist, album, uri, fileType, durationMs, dateAdded, confirmed) " +
                            "VALUES('Pre-migration Song', 'Old Artist', 'Old Album', 'content://old/song.mp3', " +
                            "'AUDIO', 180000, 1700000000000, 1)",
                    )
                    // Room identity table — Room checks this on first open at
                    // the target version. Dropping it on open lets Room re-stamp
                    // it with the v4 hash after our migrations run.
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS room_master_table " +
                            "(id INTEGER PRIMARY KEY, identity_hash TEXT)",
                    )
                }
                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) { /* no-op */ }
            })
            .build()
        factory.create(config).writableDatabase.close()

        // --- 2. Open through Room with the full migration ladder ---
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, dbName)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        // --- 3. Assert the pre-migration row survived with correct defaults ---
        val song = db.songDao().findByUri("content://old/song.mp3")
        assertNotNull("pre-migration row must survive v1 -> v4", song)
        assertEquals("Pre-migration Song", song!!.title)
        assertEquals(0L, song.sizeBytes) // added v1 -> v2, DEFAULT 0
        assertEquals(LyricsStatus.MISSING, song.lyricsStatus) // added v2 -> v3, DEFAULT 'MISSING'
        assertNull(song.lyricsCachePath) // added v2 -> v3, nullable
        // Lyrics and setlist tables must exist (new in v3 / v4) and be empty.
        assertNull(db.lyricsDao().findBySongId(song.id))
        assertEquals(0, db.setlistDao().getAll().size)
        assertEquals(0, db.liveQueueDao().observeAll().first().size)

        db.close()
    }

    /**
     * Ensure a fresh install (no prior DB) still opens cleanly at v4 — the
     * migration list must not fire in the greenfield case.
     */
    @Test
    fun freshInstall_opensDirectlyAtV4() = runTest {
        val freshName = "fresh-install.db"
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.deleteDatabase(freshName)

        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, freshName)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        // Writing + reading a song should succeed without throwing.
        val id = db.songDao().insert(
            Song(
                title = "New",
                artist = null,
                album = null,
                uri = "content://new/1.mp3",
                fileType = FileType.AUDIO,
                durationMs = 1_000L,
                dateAdded = 0L,
                confirmed = true,
            ),
        )
        assertEquals(id, db.songDao().findByUri("content://new/1.mp3")!!.id)

        db.close()
        ctx.deleteDatabase(freshName)
    }
}

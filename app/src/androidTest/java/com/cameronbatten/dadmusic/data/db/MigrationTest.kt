package com.cameronbatten.dadmusic.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /**
     * Hand-seed a v1 database with the smallest valid shape, then open it through
     * Room with the migration ladder and assert that the pre-migration row is
     * still readable at v4 and the fields added along the way have their defaults.
     *
     * MigrationTestHelper's `createDatabase` normally needs schema JSONs for every
     * historical version. We only check in `4.json`, so we fall back to manual DDL
     * for the v1 shape and let Room drive the migrations from there.
     */
    @Test
    fun v1_to_v4_preservesSongRowAndFillsDefaults() = runTest {
        // --- 1. Create a v1-shape DB by hand ---
        val raw = helper.createDatabase(dbName, 1).apply {
            // MigrationTestHelper opens v1 with a stub schema (identity hash check
            // disabled when no schema JSONs are provided). Create the v1 songs
            // table that matches M1's shape and insert a song.
            execSQL(
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
            execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_uri` ON `songs` (`uri`)")
            execSQL(
                "INSERT INTO songs(title, artist, album, uri, fileType, durationMs, dateAdded, confirmed) " +
                    "VALUES('Pre-migration Song', 'Old Artist', 'Old Album', 'content://old/song.mp3', " +
                    "'AUDIO', 180000, 1700000000000, 1)",
            )
            close()
        }

        // --- 2. Open through Room with the full migration ladder ---
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
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

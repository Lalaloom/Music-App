package com.cameronbatten.dadmusic.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ── Fakes ─────────────────────────────────────────────────────────────────────

/**
 * Fake [SongSource] backed by a [MutableStateFlow] so tests control the song list
 * without a real Room database.
 */
private class FakeSongSource(
    initialSongs: List<Song> = emptyList(),
) : SongSource {
    private val _songs = MutableStateFlow(initialSongs)

    override fun observeAll(): Flow<List<Song>> = _songs

    fun setSongs(songs: List<Song>) {
        _songs.value = songs
    }
}

/**
 * Fake [PlaybackCommands] that records which songs were played.
 */
private class FakePlaybackCommands : PlaybackCommands {
    val playedSongs = mutableListOf<Song>()

    override suspend fun play(song: Song) {
        playedSongs.add(song)
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun buildViewModel(
    source: FakeSongSource = FakeSongSource(),
    commands: FakePlaybackCommands = FakePlaybackCommands(),
): LibraryViewModel = LibraryViewModel(source, commands)

private fun makeSong(
    id: Long,
    title: String,
    artist: String? = null,
): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = null,
    uri = "content://test/$id",
    fileType = FileType.AUDIO,
    durationMs = 180_000L,
    dateAdded = 0L,
    confirmed = true,
)

// ── Tests ──────────────────────────────────────────────────────────────────────

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── 1. shows_songs_from_dao ───────────────────────────────────────────────

    /**
     * When the DAO emits a non-empty list the screen must show each song title
     * in the lazy column.
     */
    @Test
    fun shows_songs_from_dao() {
        val song1 = makeSong(id = 1, title = "Bohemian Rhapsody", artist = "Queen")
        val song2 = makeSong(id = 2, title = "Hotel California", artist = "Eagles")
        val source = FakeSongSource(listOf(song1, song2))
        val vm = buildViewModel(source = source)

        composeTestRule.setContent {
            LibraryScreen(
                onOpenFolders = {},
                onSongPlaying = {},
                viewModel = vm,
            )
        }

        composeTestRule
            .onNodeWithText("Bohemian Rhapsody")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Hotel California")
            .assertIsDisplayed()

        // Two rows present
        assertEquals(2, composeTestRule.onAllNodes(hasText("Queen") or hasText("Eagles")).fetchSemanticsNodes().size)
    }

    // ── 2. empty_state_prompts_user_to_add_folders ────────────────────────────

    /**
     * When the DAO emits an empty list the empty-state prompt and CTA button
     * must both be visible.
     */
    @Test
    fun empty_state_prompts_user_to_add_folders() {
        val vm = buildViewModel(source = FakeSongSource(emptyList()))

        composeTestRule.setContent {
            LibraryScreen(
                onOpenFolders = {},
                onSongPlaying = {},
                viewModel = vm,
            )
        }

        // Empty-state text from strings.xml
        composeTestRule
            .onNodeWithText("No songs yet. Pick a folder to get started.")
            .assertIsDisplayed()

        // CTA button from strings.xml
        composeTestRule
            .onNodeWithText("Pick a folder")
            .assertIsDisplayed()
    }

    // ── 3. tap_row_invokes_playbackController_play_and_navigates ─────────────

    /**
     * Tapping a song row must:
     *   a) call [PlaybackCommands.play] with the correct song, and
     *   b) invoke the [onSongPlaying] callback (navigation to player).
     */
    @Test
    fun tap_row_invokes_playbackController_play_and_navigates() {
        val song = makeSong(id = 42, title = "Stairway to Heaven", artist = "Led Zeppelin")
        val source = FakeSongSource(listOf(song))
        val fakeCommands = FakePlaybackCommands()
        val vm = buildViewModel(source = source, commands = fakeCommands)

        var navigated = false

        composeTestRule.setContent {
            LibraryScreen(
                onOpenFolders = {},
                onSongPlaying = { navigated = true },
                viewModel = vm,
            )
        }

        composeTestRule
            .onNodeWithText("Stairway to Heaven")
            .performClick()

        composeTestRule.waitForIdle()

        // Navigation callback must have fired
        assertTrue("onSongPlaying was not called", navigated)

        // PlaybackCommands.play() must have been called with the correct song
        assertEquals(1, fakeCommands.playedSongs.size)
        assertEquals(42L, fakeCommands.playedSongs.first().id)
        assertEquals("Stairway to Heaven", fakeCommands.playedSongs.first().title)
    }
}

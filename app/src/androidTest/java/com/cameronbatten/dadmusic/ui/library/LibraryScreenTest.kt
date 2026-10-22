package com.cameronbatten.dadmusic.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.playback.PlaybackSession
import com.cameronbatten.dadmusic.playback.PlaybackState
import com.cameronbatten.dadmusic.playback.SongLookup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

/** Fake [PlaybackSession] that records play invocations and holds a mutable state. */
private class FakePlaybackSession(
    initialState: PlaybackState = PlaybackState(),
) : PlaybackSession {
    val playedSongs = mutableListOf<Song>()
    private val _state = MutableStateFlow(initialState)

    override fun state(): StateFlow<PlaybackState> = _state
    override suspend fun play(song: Song) {
        playedSongs.add(song)
    }
    override fun pause() {}
    override fun resume() {}
    override fun seekTo(ms: Long) {}
    override fun skipNext() {}
    override fun skipPrevious() {}
    override fun getPlayer(): Player? = null
}

/** Fake [SongLookup] that returns null (no mini-player). */
private class FakeSongLookup : SongLookup {
    override suspend fun findById(id: Long): Song? = null
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun buildViewModel(
    source: FakeSongSource = FakeSongSource(),
    session: FakePlaybackSession = FakePlaybackSession(),
    lookup: FakeSongLookup = FakeSongLookup(),
): LibraryViewModel = LibraryViewModel(source, session, lookup)

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

    @Test
    fun tap_row_invokes_playbackController_play_and_navigates() {
        val song = makeSong(id = 42, title = "Stairway to Heaven", artist = "Led Zeppelin")
        val source = FakeSongSource(listOf(song))
        val session = FakePlaybackSession()
        val vm = buildViewModel(source = source, session = session)

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

        // PlaybackSession.play() must have been called with the correct song
        assertEquals(1, session.playedSongs.size)
        assertEquals(42L, session.playedSongs.first().id)
        assertEquals("Stairway to Heaven", session.playedSongs.first().title)
    }
}

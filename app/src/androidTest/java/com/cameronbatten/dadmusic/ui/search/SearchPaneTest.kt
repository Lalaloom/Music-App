package com.cameronbatten.dadmusic.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.LiveQueueEntry
import com.cameronbatten.dadmusic.data.db.QueueSource
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.setlist.AppendResult
import com.cameronbatten.dadmusic.setlist.QueueAppender
import com.cameronbatten.dadmusic.ui.library.SongSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeSongSource(initial: List<Song> = emptyList()) : SongSource {
    private val _songs = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<Song>> = _songs
    fun setSongs(songs: List<Song>) { _songs.value = songs }
}

private class FakeQueueAppender : QueueAppender {
    val appended = mutableListOf<Pair<Long, QueueSource>>()
    private val present = mutableSetOf<Long>()
    private var nextPosition = 1000

    fun markPresent(songId: Long) {
        present += songId
    }

    override suspend fun append(songId: Long, source: QueueSource): AppendResult {
        appended += songId to source
        return if (songId in present) {
            AppendResult.AlreadyPresent(
                LiveQueueEntry(position = 0, songId = songId, source = source, parentSetlistId = null),
            )
        } else {
            present += songId
            val entry = LiveQueueEntry(
                position = nextPosition.also { nextPosition += 1000 },
                songId = songId,
                source = source,
                parentSetlistId = null,
            )
            AppendResult.Added(entry)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun song(
    id: Long,
    title: String,
    artist: String? = null,
    fileType: FileType = FileType.AUDIO,
): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = null,
    uri = "content://test/$id",
    fileType = fileType,
    durationMs = 180_000L,
    dateAdded = 0L,
    confirmed = true,
)

private fun buildVm(
    source: FakeSongSource = FakeSongSource(),
    appender: FakeQueueAppender = FakeQueueAppender(),
): SearchViewModel = SearchViewModel(source, appender)

// ── Tests ──────────────────────────────────────────────────────────────────────

@RunWith(AndroidJUnit4::class)
class SearchPaneTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyQuery_showsEverySong() {
        val vm = buildVm(FakeSongSource(listOf(
            song(1, "Alpha"),
            song(2, "Beta"),
        )))

        composeTestRule.setContent {
            SearchPane(onAddResult = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta").assertIsDisplayed()
    }

    @Test
    fun typingQuery_filtersByTitle() {
        val vm = buildVm(FakeSongSource(listOf(
            song(1, "Hotel California", "Eagles"),
            song(2, "Stairway to Heaven", "Led Zeppelin"),
            song(3, "Bohemian Rhapsody", "Queen"),
        )))

        composeTestRule.setContent {
            SearchPane(onAddResult = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("hotel")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Hotel California").assertIsDisplayed()
    }

    @Test
    fun typingQuery_filtersByArtist() {
        val vm = buildVm(FakeSongSource(listOf(
            song(1, "Hotel California", "Eagles"),
            song(2, "Desperado", "Eagles"),
            song(3, "Bohemian Rhapsody", "Queen"),
        )))

        composeTestRule.setContent {
            SearchPane(onAddResult = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Eagles")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Hotel California").assertIsDisplayed()
        composeTestRule.onNodeWithText("Desperado").assertIsDisplayed()
    }

    @Test
    fun queryWithNoMatches_showsNoMatchesHint() {
        val vm = buildVm(FakeSongSource(listOf(song(1, "Alpha"))))

        composeTestRule.setContent {
            SearchPane(onAddResult = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("zzz")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No matches.").assertIsDisplayed()
    }

    @Test
    fun tapRow_callsOnAddResultWithAdded() {
        val target = song(42, "Target Song", "Target Artist")
        val vm = buildVm(FakeSongSource(listOf(target)))
        val received = mutableListOf<AddToQueueOutcome>()

        composeTestRule.setContent {
            SearchPane(onAddResult = { received += it }, viewModel = vm)
        }

        composeTestRule.onNodeWithText("Target Song").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, received.size)
        val outcome = received.first()
        assertTrue(outcome is AddToQueueOutcome.Added)
        assertEquals("Target Song", (outcome as AddToQueueOutcome.Added).title)
    }

    @Test
    fun tapRow_whenAlreadyInQueue_callsOnAddResultWithAlreadyPresent() {
        val target = song(42, "Target Song")
        val appender = FakeQueueAppender().also { it.markPresent(42) }
        val vm = buildVm(FakeSongSource(listOf(target)), appender)
        val received = mutableListOf<AddToQueueOutcome>()

        composeTestRule.setContent {
            SearchPane(onAddResult = { received += it }, viewModel = vm)
        }

        composeTestRule.onNodeWithText("Target Song").performClick()
        composeTestRule.waitForIdle()

        assertTrue(received.first() is AddToQueueOutcome.AlreadyPresent)
    }

    @Test
    fun clearButton_resetsQueryToEmpty() {
        val vm = buildVm(FakeSongSource(listOf(song(1, "Alpha"), song(2, "Beta"))))

        composeTestRule.setContent {
            SearchPane(onAddResult = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("alpha")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Beta").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta").assertIsDisplayed()
    }
}


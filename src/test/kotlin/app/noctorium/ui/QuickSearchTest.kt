package app.noctorium.ui

import androidx.compose.ui.input.key.Key
import app.noctorium.core.AppUiState
import app.noctorium.core.SearchBangs
import app.noctorium.core.SearchMode
import app.noctorium.domain.ProviderType
import app.noctorium.domain.SearchResults
import app.noctorium.domain.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The quick search bar's keys, its bang chip and what it lists, without drawing it. */
class QuickSearchTest {

    @Test
    fun `the arrows choose, Enter plays, Shift+Enter queues, Ctrl+Enter opens the page and Escape closes`() {
        assertEquals(QuickKey.DOWN, quickSearchKey(Key.DirectionDown))
        assertEquals(QuickKey.UP, quickSearchKey(Key.DirectionUp))
        assertEquals(QuickKey.PLAY, quickSearchKey(Key.Enter))
        assertEquals(QuickKey.PLAY, quickSearchKey(Key.NumPadEnter))
        assertEquals(QuickKey.QUEUE, quickSearchKey(Key.Enter, shift = true))
        assertEquals(QuickKey.OPEN_PAGE, quickSearchKey(Key.Enter, command = true))
        assertEquals(QuickKey.OPEN_PAGE, quickSearchKey(Key.Enter, shift = true, command = true), "the page wins over the queue")
        assertEquals(QuickKey.CLOSE, quickSearchKey(Key.Escape))
        assertNull(quickSearchKey(Key.A), "a letter is typing")
        assertNull(quickSearchKey(Key.Spacebar))
    }

    @Test
    fun `the choice stays on the list`() {
        assertEquals(1, quickChoice(0, QuickKey.DOWN, 3))
        assertEquals(2, quickChoice(2, QuickKey.DOWN, 3), "down from the last stays on the last")
        assertEquals(0, quickChoice(0, QuickKey.UP, 3))
        assertEquals(0, quickChoice(0, QuickKey.DOWN, 0), "nothing to choose from")
    }

    @Test
    fun `a bang at the start or the end shows its service on the chip`() {
        assertEquals(BangChip("!ytm", "YouTube Music", SearchMode.YOUTUBE_MUSIC), bangChip("!ytm daft punk"))
        assertEquals(BangChip("!sc", "SoundCloud", SearchMode.SOUNDCLOUD), bangChip("daft punk !sc"))
        assertEquals(BangChip("!SP", "Spotify", SearchMode.SPOTIFY), bangChip("!SP around the world"), "in any case, shown as typed")
        assertEquals("Bandcamp", bangChip("!bc")?.service, "a bang alone is a chip with nothing searched yet")
        assertEquals("VK Music", bangChip("kino !vk")?.service)
        assertEquals("Every service", bangChip("!all night")?.service, "!all is every service, which the page calls Hybrid")
        assertNull(bangChip("daft punk"))
        assertNull(bangChip("!!! (chk chk chk)"), "a band called !!! is a search for it, not a bang")
        assertNull(bangChip("!nope daft punk"), "an unknown bang is a word like any other")
    }

    private val song = Track(ProviderType.YOUTUBE_MUSIC, "q1", "Around the World", emptyList(), sourceUrl = "https://example.invalid/1")

    @Test
    fun `the bar lists the search's songs only once they are the songs for what is in the box`() {
        val ui = AppUiState(searchQuery = "!ytm daft punk", searchBang = SearchBangs.parse("!ytm daft punk").bang, searchResults = SearchResults(tracks = List(12) { song.copy(id = "q$it") }))
        assertEquals(QUICK_RESULTS, quickResults("!ytm daft punk", ui).size, "a short list: it is meant to be quick")
        assertTrue(quickResults("!ytm daft pun", ui).isEmpty(), "the results a letter ago are not the results for this")
        assertTrue(quickResults("", ui).isEmpty())
        assertTrue(quickResults("!ytm", ui.copy(searchQuery = "!ytm")).isEmpty(), "a bang alone has searched nothing yet")
    }
}

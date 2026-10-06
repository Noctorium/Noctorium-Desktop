package app.noctorium.ui

import app.noctorium.bandcamp.BandcampGenre
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.settings.BandcampConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The desktop's decisions about Bandcamp: what Settings says, the genres' order, and what is offered for a song. */
class BandcampUiTest {

    private fun track(id: String, provider: ProviderType) = Track(
        provider = provider,
        id = id,
        title = "Song $id",
        artists = listOf(Artist("a", "Someone", provider)),
        sourceUrl = "https://example.invalid/$id",
    )

    /** What [app.noctorium.core.AppState.canKeep] answers: everything but Bandcamp. */
    private val canKeep: (Track) -> Boolean = { it.provider != ProviderType.BANDCAMP }

    // --- Settings ---

    @Test
    fun `the collection is named by the fan once Bandcamp has said who that is, and by the address before`() {
        assertNull(bandcampCollectionLine("", ""))
        assertEquals("Showing the collection of Sample Fan", bandcampCollectionLine("sample-fan", "Sample Fan"))
        // After a restart only the name in the address is known, until Bandcamp is asked again.
        assertEquals("Showing the collection at bandcamp.com/sample-fan", bandcampCollectionLine("sample-fan", ""))
    }

    @Test
    fun `the Settings list says whether there is a collection, and when one is being checked`() {
        assertEquals(
            "No sign-in needed — add your name to see your collection",
            bandcampSummary("", BandcampConnectionState()),
        )
        assertEquals("Showing the collection of Sample Fan", bandcampSummary("sample-fan", BandcampConnectionState(fanName = "Sample Fan")))
        assertEquals("Checking the name with Bandcamp…", bandcampSummary("", BandcampConnectionState(checking = true)))
        // A failed check leaves the name already in force, which is still what the library shows.
        assertEquals(
            "Showing the collection at bandcamp.com/sample-fan",
            bandcampSummary("sample-fan", BandcampConnectionState(message = "Bandcamp has no fan called \"sample-fen\".")),
        )
    }

    @Test
    fun `a genre switched on goes after the others, which keep their order`() {
        val chosen = listOf(BandcampGenre.AMBIENT, BandcampGenre.ELECTRONIC)
        assertEquals(
            listOf(BandcampGenre.AMBIENT, BandcampGenre.ELECTRONIC, BandcampGenre.JAZZ),
            chosen.withGenre(BandcampGenre.JAZZ, on = true),
        )
        // Not BandcampGenre's own order, which would put Electronic first.
        assertEquals(listOf(BandcampGenre.ELECTRONIC), chosen.withGenre(BandcampGenre.AMBIENT, on = false))
        // On twice is still once, in its place.
        assertEquals(chosen, chosen.withGenre(BandcampGenre.AMBIENT, on = true))
        assertEquals(chosen, chosen.withGenre(BandcampGenre.METAL, on = false))
        assertEquals(emptyList(), listOf(BandcampGenre.ROCK).withGenre(BandcampGenre.ROCK, on = false))
    }

    @Test
    fun `the line under the genres says what none means`() {
        assertTrue(genreRowsLine(emptyList()).startsWith("None picked"))
        assertTrue(genreRowsLine(BandcampGenre.DEFAULT_HOME).startsWith("Numbered"))
    }

    // --- Keeping songs ---

    @Test
    fun `an album from Bandcamp offers no download at all`() {
        val album = (1..9).map { track("$it", ProviderType.BANDCAMP) }
        assertNull(downloadsLeft(album, canKeep) { false })
    }

    @Test
    fun `a Bandcamp song among others is not counted as one more to download`() {
        val mixed = listOf(
            track("a", ProviderType.YOUTUBE_MUSIC),
            track("b", ProviderType.BANDCAMP),
            track("c", ProviderType.SOUNDCLOUD),
            track("d", ProviderType.BANDCAMP),
        )
        assertEquals(2, downloadsLeft(mixed, canKeep) { false })
        assertEquals(1, downloadsLeft(mixed, canKeep) { it.id == "a" })
        // Everything that may be kept is kept: "All downloaded", not a button that has vanished.
        assertEquals(0, downloadsLeft(mixed, canKeep) { true })
    }

    @Test
    fun `an empty playlist has nothing to download`() {
        assertNull(downloadsLeft(emptyList(), canKeep) { false })
    }

    // --- Writing to the services ---

    @Test
    fun `no service is offered a Bandcamp song`() {
        val song = track("1", ProviderType.BANDCAMP)
        listOf(ProviderType.YOUTUBE_MUSIC, ProviderType.YOUTUBE_VIDEO, ProviderType.SOUNDCLOUD).forEach { service ->
            assertFalse(service.acceptsTrack(song), "$service was offered a Bandcamp song")
        }
    }

    @Test
    fun `saving a mixed playlist to YouTube Music takes only what YouTube can hold`() {
        val playlist = listOf(
            track("yt", ProviderType.YOUTUBE_MUSIC),
            track("video", ProviderType.YOUTUBE_VIDEO),
            track("bc", ProviderType.BANDCAMP),
            track("sc", ProviderType.SOUNDCLOUD),
            track("sp", ProviderType.SPOTIFY),
        )
        assertEquals(listOf("yt", "video"), playlist.filter { ProviderType.YOUTUBE_MUSIC.acceptsTrack(it) }.map { it.id })
        assertEquals(listOf("sc"), playlist.filter { ProviderType.SOUNDCLOUD.acceptsTrack(it) }.map { it.id })
    }
}

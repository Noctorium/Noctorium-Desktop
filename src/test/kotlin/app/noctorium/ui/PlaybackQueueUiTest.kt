package app.noctorium.ui

import app.noctorium.core.SearchMode
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.QueueState
import app.noctorium.playback.RepeatMode
import app.noctorium.settings.AutoplaySource
import app.noctorium.settings.DEFAULT_HYBRID_SEARCH
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.SpotifyConnectionState
import app.noctorium.settings.VkConnectionState
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** The desktop's decisions about how listening goes on: the speed, the fade, autoplay under the queue, search. */
class PlaybackQueueUiTest {

    private fun track(id: String, provider: ProviderType = ProviderType.YOUTUBE_MUSIC) = Track(
        provider = provider,
        id = id,
        title = "Song $id",
        artists = listOf(Artist("a", "Sample Artist", provider)),
        sourceUrl = "https://example.invalid/$id",
    )

    // --- Speed and the fade ---

    @Test
    fun `a speed is said as the player says it`() {
        assertEquals("1×", speedLabel(1f))
        assertEquals("1.25×", speedLabel(1.25f))
        assertEquals("0.5×", speedLabel(0.5f))
        assertEquals("2×", speedLabel(2f))
        assertEquals("1.05×", speedLabel(1.05f))
        assertEquals("1.1×", speedLabel(1.1f))
    }

    @Test
    fun `a dragged speed lands on a twentieth inside the range`() {
        assertEquals(1.25f, snapSpeed(1.23f))
        assertEquals(1f, snapSpeed(1.02f))
        assertEquals(0.5f, snapSpeed(0.2f))
        assertEquals(2f, snapSpeed(3f))
    }

    @Test
    fun `the fade is named in the units it comes in`() {
        assertEquals(listOf("Off", "15 s", "30 s", "1 min"), SLEEP_FADES.map(::sleepFadeLabel))
    }

    @Test
    fun `the Settings list says autoplay, the speed and whether the queue is kept`() {
        assertEquals("Autoplay on · normal speed · the queue kept", playbackSummary(NoctoriumPreferences()))
        assertEquals(
            "Autoplay off · 1.5× speed",
            playbackSummary(NoctoriumPreferences(autoplay = false, playbackSpeed = 1.5f, keepQueue = false)),
        )
    }

    @Test
    fun `a queue saved as a playlist is named for the day`() {
        assertEquals("Queue, 6 October", queuePlaylistName(LocalDate.of(2026, 10, 6)))
    }

    // --- Autoplay under the queue ---

    private val radio = listOf(track("r1"), track("r2"))
    private val nearEnd = QueueState(tracks = listOf(track("1"), track("2"), track("3")), currentIndex = 1)
    private val spotifyOff = SpotifyConnectionState()

    @Test
    fun `autoplay's songs are shown with where they come from`() {
        val lined = nearEnd.copy(suggestions = radio, suggestionsFrom = "YouTube Music radio")
        assertEquals(
            AutoplayShown.Lined("YouTube Music radio", radio),
            autoplayShown(lined, autoplay = true, from = AutoplaySource.SAME_SERVICE, spotify = spotifyOff),
        )
    }

    @Test
    fun `switched off, autoplay says so whatever was lined up`() {
        val lined = nearEnd.copy(suggestions = radio, suggestionsFrom = "Related on SoundCloud")
        assertEquals(AutoplayShown.Off, autoplayShown(lined, autoplay = false, from = AutoplaySource.SAME_SERVICE, spotify = spotifyOff))
    }

    @Test
    fun `nothing is said for an empty queue, or one that repeats and so never runs out`() {
        assertEquals(AutoplayShown.Hidden, autoplayShown(QueueState(), true, AutoplaySource.SAME_SERVICE, spotifyOff))
        assertEquals(
            AutoplayShown.Hidden,
            autoplayShown(nearEnd.copy(repeatMode = RepeatMode.ALL, suggestions = radio), true, AutoplaySource.SAME_SERVICE, spotifyOff),
        )
    }

    @Test
    fun `with nothing lined up yet, autoplay waits for the end of the queue`() {
        assertEquals(AutoplayShown.Waiting, autoplayShown(nearEnd, true, AutoplaySource.SAME_SERVICE, spotifyOff))
    }

    @Test
    fun `a queue ending on a song Spotify plays is carried on by Spotify itself`() {
        val premium = SpotifyConnectionState(connected = true, canPlay = true, playsOnSpotify = true)
        val endsOnSpotify = nearEnd.copy(tracks = nearEnd.tracks.dropLast(1) + track("s", ProviderType.SPOTIFY))
        assertEquals(
            AutoplayShown.SpotifyChooses("Spotify chooses what comes next"),
            autoplayShown(endsOnSpotify, true, AutoplaySource.SAME_SERVICE, premium),
        )
        // Asked for YouTube Music's radio instead, the core lines that up, and it is waited for as usual.
        assertEquals(AutoplayShown.Waiting, autoplayShown(endsOnSpotify, true, AutoplaySource.YOUTUBE_MUSIC, premium))
        // Played matched, a Spotify song is no longer Spotify's by the time it plays.
        assertEquals(AutoplayShown.Waiting, autoplayShown(endsOnSpotify, true, AutoplaySource.SAME_SERVICE, spotifyOff))
    }

    // --- Hybrid search ---

    @Test
    fun `Hybrid search's services are the core's, with YouTube's videos named as videos`() {
        assertEquals(DEFAULT_HYBRID_SEARCH.toList(), HYBRID_SEARCH_SERVICES)
        assertEquals("YouTube videos", hybridServiceName(ProviderType.YOUTUBE_VIDEO))
        assertEquals("SoundCloud", hybridServiceName(ProviderType.SOUNDCLOUD))
    }

    @Test
    fun `the search page names only the services chosen for Hybrid search`() {
        val chosen = setOf(ProviderType.YOUTUBE_MUSIC, ProviderType.YOUTUBE_VIDEO)
        assertEquals(
            "YouTube Music and YouTube videos results appear together.",
            searchHint(SearchMode.HYBRID, SpotifyConnectionState(), VkConnectionState(), chosen),
        )
        // Spotify alone, while its songs are matched, can answer nothing, and the page says where to change that.
        assertEquals(
            "None of the services chosen for Hybrid search can answer yet. Choose them under Settings › Customization.",
            searchHint(SearchMode.HYBRID, SpotifyConnectionState(), VkConnectionState(), setOf(ProviderType.SPOTIFY)),
        )
    }
}

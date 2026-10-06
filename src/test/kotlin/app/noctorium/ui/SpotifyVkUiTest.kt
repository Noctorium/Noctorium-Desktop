package app.noctorium.ui

import app.noctorium.auth.HarvestedCookie
import app.noctorium.auth.vkSessionCookies
import app.noctorium.core.SearchMode
import app.noctorium.domain.Artist
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.settings.HomePart
import app.noctorium.settings.SpotifyConnectionState
import app.noctorium.settings.VkConnectionState
import app.noctorium.spotify.SpotifyDevice
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The desktop's decisions about Spotify and VK: where songs play, what each page says, and VK's sign-in. */
class SpotifyVkUiTest {

    private fun track(id: String, provider: ProviderType) = Track(
        provider = provider,
        id = id,
        title = "Song $id",
        artists = listOf(Artist("a", "Someone", provider)),
        sourceUrl = "https://example.invalid/$id",
    )

    private val premium = SpotifyConnectionState(connected = true, accountName = "Sample Listener", canPlay = true, playsOnSpotify = true)

    // --- Where a song plays ---

    @Test
    fun `only a Spotify song, with Spotify songs set to play on Spotify, is on Spotify`() {
        assertTrue(playsOnSpotify(track("1", ProviderType.SPOTIFY), premium))
        assertFalse(playsOnSpotify(track("1", ProviderType.SPOTIFY), premium.copy(playsOnSpotify = false)))
        // A Spotify song played matched has become its YouTube recording by the time it plays.
        assertFalse(playsOnSpotify(track("1", ProviderType.YOUTUBE_MUSIC), premium))
        assertFalse(playsOnSpotify(null, premium))
    }

    // --- Spotify's page ---

    @Test
    fun `the Settings list says which Spotify sign-in is in use and where its songs play`() {
        assertEquals("Connect any account, or Premium to play on Spotify", spotifySummary(SpotifyConnectionState()))
        assertEquals("Waiting for Spotify…", spotifySummary(SpotifyConnectionState(connecting = true)))
        assertEquals("Reading Sample Listener's library", spotifySummary(SpotifyConnectionState(connected = true, accountName = "Sample Listener")))
        assertEquals("Sample Listener · Premium · songs play on Spotify", spotifySummary(premium))
        assertEquals("Premium · songs matched on YouTube Music", spotifySummary(premium.copy(accountName = "", playsOnSpotify = false)))
    }

    @Test
    fun `a device says what it is, and what Spotify said about it`() {
        assertEquals("Computer · playing now", spotifyDeviceLine(SpotifyDevice("a", "Studio Desk", "Computer", isActive = true)))
        assertEquals("Speaker · Spotify won't take commands for it", spotifyDeviceLine(SpotifyDevice("b", "Hall", "Speaker", isActive = false, isRestricted = true)))
        assertEquals("Smartphone", spotifyDeviceLine(SpotifyDevice("c", "Pocket Phone", "Smartphone", isActive = false)))
    }

    @Test
    fun `a chosen device missing from Spotify's list is noticed, and any active device never is`() {
        val devices = listOf(SpotifyDevice("a", "Studio Desk", "Computer", isActive = true))
        assertFalse(chosenDeviceAway(devices, "a"))
        assertTrue(chosenDeviceAway(devices, "gone"))
        assertFalse(chosenDeviceAway(devices, ""))
        assertFalse(chosenDeviceAway(emptyList(), ""))
    }

    // --- VK ---

    @Test
    fun `the Settings list says whose VK music it is`() {
        assertEquals("Sign in with your vk.ru account", vkSummary(VkConnectionState()))
        assertEquals("Checking the session with VK…", vkSummary(VkConnectionState(checking = true)))
        assertEquals("Signed in as Sample Listener", vkSummary(VkConnectionState(connected = true, accountName = "Sample Listener")))
        assertEquals("Signed in", vkSummary(VkConnectionState(connected = true)))
    }

    private fun cookie(name: String, value: String, domain: String, expires: Long = 0) =
        HarvestedCookie(domain = domain, path = "/", name = name, value = value, secure = true, expiresEpochSeconds = expires)

    @Test
    fun `VK's session is complete only with remixsid from vk ru and p from login vk ru`() {
        val site = listOf(cookie("remixlang", "3", ".vk.ru"), cookie("remixsid", "sample-sid", ".vk.ru"))
        val login = listOf(cookie("p", "sample-p", "login.vk.ru"))
        assertEquals("p=sample-p; remixsid=sample-sid", vkSessionCookies(site, login))
        // The p cookie lives on VK's sign-in host, and is not looked for on the site's.
        assertNull(vkSessionCookies(site + login, emptyList()))
        assertNull(vkSessionCookies(emptyList(), login))
    }

    @Test
    fun `an expired VK cookie is not a session`() {
        val past = Instant.now().epochSecond - 60
        val site = listOf(cookie("remixsid", "sample-sid", ".vk.ru", expires = past))
        val login = listOf(cookie("p", "sample-p", "login.vk.ru"))
        assertNull(vkSessionCookies(site, login))
    }

    // --- Songs that are not kept, hearts, and search ---

    @Test
    fun `each service says in its own words why its songs are not downloaded`() {
        assertEquals("Bandcamp songs are bought, not downloaded", notKeptReason(ProviderType.BANDCAMP))
        assertEquals("VK songs play here but can't be downloaded", notKeptReason(ProviderType.VK))
        assertEquals("Open on Bandcamp", servicePageLabel(ProviderType.BANDCAMP))
        assertEquals("Open on VK", servicePageLabel(ProviderType.VK))
        assertEquals("to buy", servicePageHint(ProviderType.BANDCAMP))
        assertNull(servicePageHint(ProviderType.VK))
    }

    @Test
    fun `a heart says where the song goes`() {
        assertEquals("Add to My music on VK", likeLabel(ProviderType.VK, liked = false))
        assertEquals("Remove from My music on VK", likeLabel(ProviderType.VK, liked = true))
        assertEquals("Save to Liked Songs on Spotify", likeLabel(ProviderType.SPOTIFY, liked = false))
        assertEquals("Remove from Liked Songs on Spotify", likeLabel(ProviderType.SPOTIFY, liked = true))
        assertEquals("Like on SoundCloud", likeLabel(ProviderType.SOUNDCLOUD, liked = false))
    }

    @Test
    fun `a heart that cannot be pressed says why, without sending anybody to sign in to Bandcamp`() {
        assertEquals("Sign in to VK Music to like tracks", likeUnavailable(ProviderType.VK))
        assertEquals("Sign in to Spotify to like tracks", likeUnavailable(ProviderType.SPOTIFY))
        assertTrue(likeUnavailable(ProviderType.BANDCAMP).startsWith("Bandcamp has no likes"))
    }

    @Test
    fun `services are named as a sentence names them`() {
        assertEquals("", servicesLine(emptyList()))
        assertEquals("Bandcamp", servicesLine(listOf("Bandcamp")))
        assertEquals("Bandcamp and Spotify", servicesLine(listOf("Bandcamp", "Spotify")))
        assertEquals("Bandcamp, Spotify and VK Music", servicesLine(listOf("Bandcamp", "Spotify", "VK Music")))
    }

    @Test
    fun `mixed search names exactly the services that will answer`() {
        assertEquals(
            "YouTube Music, YouTube videos, SoundCloud and Bandcamp results appear together.",
            searchHint(SearchMode.HYBRID, SpotifyConnectionState(), VkConnectionState()),
        )
        // Spotify joins only while its songs play on Spotify, and VK only once signed in -- the core's rules.
        assertEquals(
            "YouTube Music, YouTube videos, SoundCloud, Bandcamp, Spotify and VK Music results appear together.",
            searchHint(SearchMode.HYBRID, premium, VkConnectionState(connected = true)),
        )
        assertEquals(
            "YouTube Music, YouTube videos, SoundCloud and Bandcamp results appear together.",
            searchHint(SearchMode.HYBRID, premium.copy(playsOnSpotify = false), VkConnectionState()),
        )
    }

    @Test
    fun `searching one service that needs a sign-in says so first`() {
        assertTrue(searchHint(SearchMode.SPOTIFY, SpotifyConnectionState(), VkConnectionState()).startsWith("Connect Spotify"))
        assertEquals("Only Spotify results will appear.", searchHint(SearchMode.SPOTIFY, premium, VkConnectionState()))
        assertTrue(searchHint(SearchMode.VK, premium, VkConnectionState()).startsWith("Sign in to VK"))
        assertEquals("Only VK Music results will appear.", searchHint(SearchMode.VK, premium, VkConnectionState(connected = true)))
    }

    @Test
    fun `artists from Bandcamp and Spotify are told apart from their albums`() {
        assertTrue(Playlist("band:1", "Sample Band", ProviderType.BANDCAMP).isArtist())
        assertTrue(Playlist("artist:0sampleartist00000000", "Sample Artist", ProviderType.SPOTIFY, ownerName = "Artist").isArtist())
        assertFalse(Playlist("album:1:2", "Sample Album", ProviderType.BANDCAMP).isArtist())
        assertFalse(Playlist("album:0samplealbum000000000", "Sample Album", ProviderType.SPOTIFY).isArtist())
        // Somebody else's id that happens to start the same way is no artist of Spotify's.
        assertFalse(Playlist("artist:x", "Sample", ProviderType.YOUTUBE_MUSIC).isArtist())
    }

    // --- Home ---

    @Test
    fun `Spotify and VK are among the parts of Home that are a service's rows`() {
        assertEquals(
            setOf(HomePart.YOUTUBE_MUSIC, HomePart.SOUNDCLOUD, HomePart.BANDCAMP, HomePart.SPOTIFY, HomePart.VK),
            SERVICE_HOME_PARTS,
        )
    }
}

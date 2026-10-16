package app.noctorium.ui

import app.noctorium.domain.Artist
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.ServiceMarks
import app.noctorium.domain.Track
import app.noctorium.settings.AppleMusicConnectionState
import app.noctorium.settings.DeezerConnectionState
import app.noctorium.settings.TidalConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The desktop's decisions about Apple Music, Deezer and Tidal: what each new page and tile says in each state, and how
 * the three are drawn and told apart wherever songs are listed. Every name here is made up.
 */
class NewServicesUiTest {

    private fun track(id: String, provider: ProviderType) = Track(
        provider = provider,
        id = id,
        title = "Song $id",
        artists = listOf(Artist("a", "Sample Artist", provider)),
        sourceUrl = "https://example.invalid/$id",
    )

    // --- The tiles and pages ---

    @Test
    fun `Deezer's tile and page say whose profile is shown, or that none is`() {
        assertEquals("No sign-in needed — add your profile to see your library", deezerSummary(DeezerConnectionState()))
        assertEquals("Looking for the profile on Deezer…", deezerSummary(DeezerConnectionState(checking = true)))
        val found = DeezerConnectionState(profileId = "1234567", found = true, name = "Sample Listener")
        assertEquals("Showing Sample Listener's library", deezerSummary(found))
        assertEquals("Found on Deezer: Sample Listener, profile 1234567", deezerProfileLine(found))
        // A profile kept without its name, which Deezer has not said yet.
        assertEquals("Showing profile 1234567", deezerSummary(DeezerConnectionState(profileId = "1234567")))
        assertEquals("Deezer profile 1234567", deezerProfileLine(DeezerConnectionState(profileId = "1234567")))
        assertNull(deezerProfileLine(DeezerConnectionState()))
    }

    @Test
    fun `Tidal's tile says how far its setup has got`() {
        assertEquals("Needs a free Tidal developer app of your own", tidalSummary(TidalConnectionState()))
        assertEquals("Your Tidal app is saved · connect to sign in", tidalSummary(TidalConnectionState(configured = true, ownApp = true)))
        assertEquals(
            "Tidal can be searched · connect to see your collection",
            tidalSummary(TidalConnectionState(configured = true, ownApp = true, readsCatalogue = true)),
        )
        assertEquals("Waiting for Tidal…", tidalSummary(TidalConnectionState(configured = true, connecting = true)))
        assertEquals(
            "Connected as Sample Listener",
            tidalSummary(TidalConnectionState(configured = true, readsCatalogue = true, connected = true, accountName = "Sample Listener")),
        )
        assertEquals("Your Tidal collection is connected", tidalSummary(TidalConnectionState(configured = true, connected = true)))
    }

    @Test
    fun `Tidal's app is needed first until there is one, and then in use or a choice`() {
        assertEquals("Needed first", tidalAppStatus(TidalConnectionState()))
        assertEquals("In use", tidalAppStatus(TidalConnectionState(configured = true, ownApp = true)))
        // An app this build was pointed at, which the listener may still replace with their own.
        assertEquals("Optional", tidalAppStatus(TidalConnectionState(configured = true)))
    }

    @Test
    fun `Apple Music's tile says whether it is signed in, and in which storefront`() {
        assertEquals("Search works now · sign in for your library", appleMusicSummary(AppleMusicConnectionState(storefront = "us")))
        assertEquals("Checking the sign-in with Apple…", appleMusicSummary(AppleMusicConnectionState(checking = true)))
        assertEquals("Signed in · United Kingdom storefront", appleMusicSummary(AppleMusicConnectionState(connected = true, storefront = "gb")))
        assertEquals("Signed in", appleMusicSummary(AppleMusicConnectionState(connected = true)))
    }

    @Test
    fun `a storefront is named as its country is`() {
        assertEquals("United States", storefrontName("us"))
        assertEquals("Japan", storefrontName("JP"))
        // Anything that is not a country is shown as Apple wrote it.
        assertEquals("NOT-A-PLACE", storefrontName("not-a-place"))
    }

    // --- Where their songs and albums are listed ---

    @Test
    fun `the new services' badges have their own colours and letters, and Tidal's black has an edge on a dark page`() {
        val new = listOf(ProviderType.APPLE_MUSIC, ProviderType.DEEZER, ProviderType.TIDAL)
        val colours = ProviderType.entries.map { it.badgeColour() }
        assertEquals(colours.size, colours.toSet().size, "two services share a badge colour")
        new.forEach { provider -> assertEquals(ServiceMarks.letters(provider), provider.badgeLabel(compact = true)) }
        assertEquals("APPLE MUSIC", ProviderType.APPLE_MUSIC.badgeLabel(compact = false))
        assertNotNull(ProviderType.TIDAL.badgeEdge(darkPage = true))
        assertNull(ProviderType.TIDAL.badgeEdge(darkPage = false))
        ProviderType.entries.filter { it != ProviderType.TIDAL }.forEach { provider -> assertNull(provider.badgeEdge(darkPage = true), "$provider has an edge") }
    }

    @Test
    fun `a card says whether it is an artist, an album or a playlist, whichever service it is from`() {
        assertEquals("Artist", Playlist("artist:27", "Sample Artist", ProviderType.DEEZER, ownerName = "Artist").cardLine())
        assertEquals("Album · Sample Artist", Playlist("album:302127", "Sample Album", ProviderType.DEEZER, ownerName = "Sample Artist").cardLine())
        assertEquals("Album · Sample Artist", Playlist("album:1000001", "Sample Album", ProviderType.TIDAL, ownerName = "Sample Artist").cardLine())
        assertEquals("Artist", Playlist("artist:1000002", "Sample Artist", ProviderType.APPLE_MUSIC, ownerName = "Artist").cardLine())
        assertEquals("Apple Music", Playlist("pl.sample", "Sample Mix", ProviderType.APPLE_MUSIC, ownerName = "Apple Music").cardLine())
        // A Bandcamp artist keeps where they are from, and a YouTube playlist that happens to have an album's id is a playlist.
        assertEquals("Artist · Bristol", Playlist("band:1", "Sample Band", ProviderType.BANDCAMP, ownerName = "Bristol").cardLine())
        assertEquals("Sample Channel", Playlist("album:x", "Sample", ProviderType.YOUTUBE_MUSIC, ownerName = "Sample Channel").cardLine())
        assertNull(Playlist("PL1", "Sample", ProviderType.YOUTUBE_MUSIC).cardLine())
    }

    @Test
    fun `no other service's playlist is offered an Apple Music, Deezer or Tidal song`() {
        listOf(ProviderType.APPLE_MUSIC, ProviderType.DEEZER, ProviderType.TIDAL).forEach { from ->
            listOf(ProviderType.YOUTUBE_MUSIC, ProviderType.YOUTUBE_VIDEO, ProviderType.SOUNDCLOUD).forEach { service ->
                assertFalse(service.acceptsTrack(track("1", from)), "$service was offered a $from song")
            }
        }
    }

    @Test
    fun `a heart that cannot be pressed on the new services says why, without sending anybody to sign in`() {
        listOf(ProviderType.APPLE_MUSIC, ProviderType.DEEZER, ProviderType.TIDAL).forEach { provider ->
            val why = likeUnavailable(provider)
            assertTrue(why.startsWith("${provider.displayName} songs can't be liked"), why)
            assertFalse(why.startsWith("Sign in"), why)
        }
    }
}

package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import app.noctorium.core.LinkAction
import app.noctorium.core.LinkState
import app.noctorium.core.LinkStatus
import app.noctorium.domain.LinkKind
import app.noctorium.domain.MusicLink
import app.noctorium.domain.ProviderType
import app.noctorium.playlists.LocalPlaylist
import app.noctorium.playlists.PlaylistImport
import app.noctorium.settings.SpotifyConnectionState
import app.noctorium.settings.SpotifyDataImportState
import app.noctorium.spotify.SpotifyImportSummary
import app.noctorium.spotify.SpotifyPublicPlaylistState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The desktop's decisions about Spotify with nobody signed in: what a followed public playlist says, what the
 * Settings list says, what the import says when it is done, which files the chooser offers, and when a pasted
 * playlist is offered to the library. Every name here is made up.
 */
class SpotifyWithoutSignInUiTest {

    private val id = "0sampleplaylist0000000"

    // --- A followed public playlist ---

    @Test
    fun `a followed playlist says whose it is and how many songs, and its id until it has been read`() {
        val read = SpotifyPublicPlaylistState(id, title = "Night Bus Mix", owner = "Sample Owner", songCount = 42)
        assertEquals("Night Bus Mix", read.shownTitle())
        assertEquals("by Sample Owner · 42 songs", spotifyPublicLine(read))
        assertEquals("1 song", spotifyPublicLine(read.copy(owner = "", songCount = 1)))
        // Read, and empty: that is what it has, not that it has not been read.
        assertEquals("by Sample Owner · no songs", spotifyPublicLine(read.copy(songCount = 0)))
        // Cut at a hundred is said on its tag, not in the line as well.
        assertEquals("by Sample Owner · 100 songs", spotifyPublicLine(read.copy(songCount = 100, mayBeLonger = true)))

        val unread = SpotifyPublicPlaylistState(id)
        assertEquals(id, unread.shownTitle())
        assertEquals("Not read yet", spotifyPublicLine(unread))
        // Never read, and the read went wrong: the problem says it, under the name, and the line keeps quiet.
        assertEquals("", spotifyPublicLine(unread.copy(lastError = "Could not reach Spotify's public page.")))
        // Read before, and not this time: what it was, with the problem under it.
        assertEquals("by Sample Owner · 42 songs", spotifyPublicLine(read.copy(lastError = "Could not reach Spotify's public page.")))
    }

    // --- The Settings list ---

    @Test
    fun `with nobody signed in, the Settings list says what is followed and imported`() {
        val nobody = SpotifyConnectionState()
        assertEquals("Connect any account, or Premium to play on Spotify", spotifySummary(nobody))
        val three = nobody.copy(publicPlaylists = listOf("a", "b", "c").map { SpotifyPublicPlaylistState(it) })
        assertEquals("3 public playlists", spotifySummary(three))
        assertEquals("1 public playlist", spotifySummary(nobody.copy(publicPlaylists = three.publicPlaylists.take(1))))
        assertEquals("3 public playlists · 12 imported playlists", spotifySummary(three, imported = 12))
        assertEquals("1 imported playlist", spotifySummary(nobody, imported = 1))
        assertNull(spotifyWithoutSignInLine(0, 0))
        // Signed in, the sign-in is what it says, as before; and waiting for one says that first.
        assertEquals("Reading Sample Listener's library", spotifySummary(three.copy(connected = true, accountName = "Sample Listener"), imported = 12))
        assertEquals("Waiting for Spotify…", spotifySummary(three.copy(connecting = true), imported = 12))
    }

    @Test
    fun `only the playlists an import of Spotify's download made count as imported`() {
        val own = LocalPlaylist("own", "Road trip")
        val liked = LocalPlaylist("liked", "Liked Songs from Spotify", importedFrom = PlaylistImport(PlaylistImport.SPOTIFY_DATA, "liked-songs"))
        val mix = LocalPlaylist("mix", "Kitchen radio", importedFrom = PlaylistImport(PlaylistImport.SPOTIFY_DATA, "playlist:Kitchen radio"))
        val other = LocalPlaylist("other", "From elsewhere", importedFrom = PlaylistImport("somewhere-else", "1"))
        assertEquals(2, spotifyImportedCount(listOf(own, liked, mix, other)))
        assertEquals(0, spotifyImportedCount(listOf(own)))
    }

    @Test
    fun `a playlist an import made says so, and one made here says that`() {
        val own = LocalPlaylist("own", "Road trip")
        val liked = LocalPlaylist("liked", "Liked Songs from Spotify", importedFrom = PlaylistImport(PlaylistImport.SPOTIFY_DATA, "liked-songs"))
        assertEquals("0 tracks • made in Noctorium", localPlaylistLine(own))
        assertEquals("0 tracks • imported from Spotify", localPlaylistLine(liked))
        assertEquals("0 tracks • imported", localPlaylistLine(own.copy(importedFrom = PlaylistImport("somewhere-else", "1"))))
    }

    // --- The import ---

    @Test
    fun `the import says nothing while it runs, then what it made or why it did not`() {
        assertNull(spotifyImportOutcome(SpotifyDataImportState()))
        assertNull(spotifyImportOutcome(SpotifyDataImportState(importing = true, progress = "Opening my_spotify_data.zip…")))
        val summary = SpotifyImportSummary(playlists = 12, songs = 840, likedSongs = 310)
        assertEquals(summary.message, spotifyImportOutcome(SpotifyDataImportState(message = summary.message, summary = summary)))
        // A summary without a message of its own still says what it made.
        assertEquals(summary.message, spotifyImportOutcome(SpotifyDataImportState(summary = summary)))
        val refused = SpotifyDataImportState(message = "That file is not Spotify's data download.", failed = true, summary = summary)
        assertEquals("That file is not Spotify's data download.", spotifyImportOutcome(refused))
    }

    @Test
    fun `the file chooser offers Spotify's zip and the json files from inside it`() {
        assertTrue(isSpotifyDataFileName("my_spotify_data.zip"))
        assertTrue(isSpotifyDataFileName("my_spotify_data (1).ZIP"))
        assertTrue(isSpotifyDataFileName("Playlist1.json"))
        assertTrue(isSpotifyDataFileName("YourLibrary.JSON"))
        assertFalse(isSpotifyDataFileName("cookies.txt"))
        assertFalse(isSpotifyDataFileName("my_spotify_data.zip.part"))
        assertFalse(isSpotifyDataFileName("zip"))
    }

    // --- A pasted link ---

    private val pasted = LinkState(
        LinkStatus.DONE,
        MusicLink(ProviderType.SPOTIFY, LinkKind.PLAYLIST, id, "https://open.spotify.com/playlist/$id"),
        LinkAction.PLAY,
        canKeepInLibrary = true,
    )

    @Test
    fun `a public playlist from a pasted link is offered to the library until it is kept`() {
        val nobody = SpotifyConnectionState()
        assertEquals(KeepOffer.OFFERED, keepOffer(pasted, nobody))
        assertEquals(KeepOffer.KEEPING, keepOffer(pasted, nobody.copy(addingPublicPlaylist = true)))
        val kept = nobody.copy(publicPlaylists = listOf(SpotifyPublicPlaylistState(id, title = "Night Bus Mix")))
        assertEquals(KeepOffer.KEPT, keepOffer(pasted.copy(canKeepInLibrary = false), kept))
        // Not while it is still being opened, nor when it could not be.
        assertNull(keepOffer(pasted.copy(status = LinkStatus.OPENING), nobody))
        assertNull(keepOffer(pasted.copy(status = LinkStatus.FAILED), nobody))
    }

    @Test
    fun `nothing is offered for a playlist the core cannot keep, an album, or any other service`() {
        val nobody = SpotifyConnectionState()
        assertNull(keepOffer(pasted.copy(canKeepInLibrary = false), nobody))
        val album = pasted.copy(link = MusicLink(ProviderType.SPOTIFY, LinkKind.PLAYLIST, "album:$id", "https://open.spotify.com/album/$id"), canKeepInLibrary = false)
        assertNull(keepOffer(album, nobody.copy(publicPlaylists = listOf(SpotifyPublicPlaylistState(id)))))
        val youTube = pasted.copy(link = MusicLink(ProviderType.YOUTUBE_MUSIC, LinkKind.PLAYLIST, id, "https://music.youtube.com/playlist?list=$id"), canKeepInLibrary = false)
        assertNull(keepOffer(youTube, nobody.copy(publicPlaylists = listOf(SpotifyPublicPlaylistState(id)))))
        assertNull(keepOffer(LinkState(), nobody))
    }

    // --- The link box ---

    /** Enter in the box adds what is in it, as Add does; and does nothing while a playlist is being read. */
    @OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun `Enter in the link box adds the playlist`() {
        var added = 0
        var adding by mutableStateOf(false)
        val scene = ImageComposeScene(640, 240, Density(1f)) {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Box {
                        Column {
                            var link by remember { mutableStateOf("https://open.spotify.com/playlist/$id") }
                            SpotifyLinkBox(link, { link = it }, adding) { added++ }
                        }
                    }
                }
            }
        }
        try {
            fun enter() {
                listOf(KeyEventType.KeyDown, KeyEventType.KeyUp).forEach { type ->
                    scene.sendKeyEvent(KeyEvent(Key.Enter, type, codePoint = '\n'.code))
                }
                scene.render(0)
            }
            scene.render(0)
            // Into the box, for the caret.
            val box = Offset(200f, 30f)
            scene.sendPointerEvent(PointerEventType.Move, box)
            scene.sendPointerEvent(PointerEventType.Press, box)
            scene.sendPointerEvent(PointerEventType.Release, box)
            scene.render(0)
            enter()
            assertEquals(1, added, "Enter did not add the playlist")
            adding = true
            scene.render(0)
            enter()
            assertEquals(1, added, "Enter added a second time while the first was being read")
        } finally {
            scene.close()
        }
    }
}

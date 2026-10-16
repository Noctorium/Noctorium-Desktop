package app.noctorium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.core.LinkAction
import app.noctorium.core.LinkState
import app.noctorium.core.LinkStatus
import app.noctorium.desktopAppState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.LinkKind
import app.noctorium.domain.MusicLink
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playlists.LocalPlaylist
import app.noctorium.playlists.PlaylistImport
import app.noctorium.settings.SettingsState
import app.noctorium.settings.SpotifyConnectionState
import app.noctorium.settings.SpotifyDataImportState
import app.noctorium.settings.ThemePreset
import app.noctorium.spotify.SPOTIFY_LIKED_SONGS_TITLE
import app.noctorium.spotify.SpotifyExportSkips
import app.noctorium.spotify.SpotifyImportSummary
import app.noctorium.spotify.SpotifyPublicLibrary
import app.noctorium.spotify.SpotifyPublicList
import app.noctorium.spotify.SpotifyPublicPlaylistState
import app.noctorium.spotify.SpotifyPublicRead
import app.noctorium.ui.skins.Surface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws what Spotify with nobody signed in added to the desktop, off screen, for looking at: Spotify's page in
 * Settings with nothing followed, with three public playlists followed -- one cut at a hundred songs, one whose last
 * read failed -- and with the import of Spotify's data download running, done and turned down; then a pasted public
 * playlist offered to the library, being kept and kept; and the library and a followed playlist's page, with the
 * playlists an import made among the listener's own. In Night, Day, 98, Noctorium 98 and XP. Off unless a folder is
 * named, like [PlayerBarRenderCheck]; the pictures go to `spotify-public/` under it:
 *
 *     ./gradlew :test --tests "*SpotifyWithoutSignInRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * Every state is put straight into what is drawn: no playlist is followed in the settings, so nothing reads
 * Spotify's pages, nothing is imported and nothing signs in. Every name is made up, and the covers are painted here.
 */
class SpotifyWithoutSignInRenderCheck {

    private val root: File? = System.getProperty("noctorium.renderBars")?.let { File(it, "spotify-public") }

    private val themes = listOf(
        ThemePreset.NOCTORIUM_NIGHT,
        ThemePreset.NOCTORIUM_DAY,
        ThemePreset.WINDOWS_98,
        ThemePreset.WINDOWS_98_NOCTORIUM,
        ThemePreset.WINDOWS_XP,
    )

    private val followed = listOf(
        SpotifyPublicPlaylistState("0nightbusmix0000000000", "Night Bus Mix", "Sample Owner", songCount = 42, artworkUrl = COVER + "bus"),
        SpotifyPublicPlaylistState(
            "0harbourradio000000000",
            "Harbour Radio, All of It",
            "Paper Lantern Fan Club",
            songCount = 100,
            mayBeLonger = true,
            artworkUrl = COVER + "harbour",
        ),
        SpotifyPublicPlaylistState(
            "0slowrivers00000000000",
            "Slow Rivers",
            "Another Listener",
            songCount = 18,
            lastError = SpotifyPublicRead.MISSING_PLAYLIST,
            artworkUrl = COVER + "rivers",
        ),
    )

    private val summary = SpotifyImportSummary(
        playlists = 12,
        songs = 840,
        likedSongs = 310,
        emptyPlaylists = 1,
        skipped = SpotifyExportSkips(episodes = 3, localFiles = 2),
        savedAlbums = 4,
    )

    /** Spotify's page in each state worth seeing, by the name its picture is given. */
    private fun states(base: SettingsState): List<Pair<String, SettingsState>> {
        val nobody = SpotifyConnectionState(configured = true)
        val three = nobody.copy(publicPlaylists = followed, publicMessage = "Added “Night Bus Mix”: 42 songs.")
        return listOf(
            "nothing-followed" to nobody,
            "three-followed" to three,
            "adding" to nobody.copy(publicPlaylists = followed.take(1), addingPublicPlaylist = true),
            "importing" to three.copy(
                publicMessage = null,
                dataImport = SpotifyDataImportState(importing = true, progress = "Reading the playlists in Playlist1.json…"),
            ),
            "imported" to three.copy(publicMessage = null, dataImport = SpotifyDataImportState(message = summary.message, summary = summary)),
            "import-failed" to three.copy(
                publicMessage = null,
                dataImport = SpotifyDataImportState(
                    message = "That download is your listening history, which has no playlists in it. Ask Spotify for " +
                        "Account data instead: that is the one with your playlists and Liked Songs.",
                    failed = true,
                ),
            ),
            "signed-in" to three.copy(publicMessage = null, connected = true, accountName = "Sample Listener"),
        ).map { (name, spotify) -> name to base.copy(spotify = spotify, message = null) }
    }

    @Test
    fun `Spotify's page draws with public playlists and the import, in every state, in each theme`() {
        val root = root ?: return
        withCovers {
            val state = desktopAppState()
            try {
                state.setAnimations(false)
                val base = state.settings.value
                themes.forEach { theme ->
                    val folder = File(root, theme.name.lowercase()).also { it.mkdirs() }
                    states(base).forEach { (name, settings) ->
                        // The page as it is, beta warning and all; and the two new parts alone, larger, to read.
                        draw(File(folder, "page-$name.png"), theme, 820, 2900) { SettingsPageContent(SettingsPage.SPOTIFY, settings, state) }
                        draw(File(folder, "parts-$name.png"), theme, 760, 1400) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SpotifyPublicPlaylistsCard(settings.spotify, state)
                                SpotifyImportCard(settings.spotify, state)
                            }
                        }
                    }
                }
            } finally {
                state.setAnimations(true)
                Thread.sleep(300)
                state.close()
            }
        }
    }

    @Test
    fun `a pasted public playlist is offered to the library, being kept, and kept`() {
        val root = root ?: return
        withCovers {
            val state = desktopAppState()
            try {
                state.setAnimations(false)
                val id = "0harbourradio000000000"
                val link = LinkState(
                    LinkStatus.DONE,
                    MusicLink(ProviderType.SPOTIFY, LinkKind.PLAYLIST, id, "https://open.spotify.com/playlist/$id"),
                    LinkAction.PLAY,
                    songs.take(6),
                    "Playing “Harbour Radio, All of It”: 100 songs. Spotify's public page shows only a playlist's first 100 songs.",
                    canKeepInLibrary = true,
                )
                val nobody = SpotifyConnectionState(configured = true)
                val shown = listOf(
                    "link-offered" to (link to nobody),
                    "link-keeping" to (link to nobody.copy(addingPublicPlaylist = true)),
                    "link-kept" to (link.copy(canKeepInLibrary = false) to nobody.copy(publicPlaylists = followed)),
                )
                themes.forEach { theme ->
                    val folder = File(root, theme.name.lowercase()).also { it.mkdirs() }
                    shown.forEach { (name, pair) ->
                        val (linkState, spotify) = pair
                        draw(File(folder, "$name.png"), theme, 1000, 560) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                linkOutcome(linkState, PlaybackState(), spotify, state)
                            }
                        }
                    }
                }
            } finally {
                state.setAnimations(true)
                Thread.sleep(300)
                state.close()
            }
        }
    }

    @Test
    fun `the library lists followed and imported playlists, and opens a followed one, and the Settings list says so`() {
        val root = root ?: return
        withCovers {
            val state = desktopAppState()
            try {
                state.setAnimations(false)
                state.setConnectEnabled(false)
                state.setUpdateCheckOnLaunch(false)
                listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_XP).forEach { theme ->
                    state.setTheme(theme)
                    val tour = WindowTour(state, File(root, "window-${theme.name.lowercase()}"))
                    try {
                        tour.open()
                        tour.go(Destination.LIBRARY)
                        tour.settle()
                        tour.library { libraryFixture(it) }
                        tour.capture("library")
                        tour.library { it.copy(openPlaylist = publicLists[1].let(SpotifyPublicLibrary::playlistOf).copy(tracks = songs)) }
                        tour.capture("library-public-playlist")
                        tour.library { it.copy(openPlaylist = null, openLocalPlaylist = imported.first()) }
                        tour.capture("library-imported-liked-songs")
                        tour.library { it.copy(openLocalPlaylist = null) }

                        // The list of Settings, tall enough for the Services, with nobody signed in to Spotify.
                        tour.open(height = 1500)
                        tour.library { libraryFixture(it) }
                        tour.settings { it.copy(message = null, spotify = SpotifyConnectionState(configured = true, publicPlaylists = followed)) }
                        tour.ui { it.copy(destination = Destination.SETTINGS) }
                        tour.capture("settings-home", frames = 10)
                    } finally {
                        tour.close()
                    }
                }
            } finally {
                state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
                state.setAnimations(true)
                state.setConnectEnabled(true)
                state.setUpdateCheckOnLaunch(true)
                Thread.sleep(300)
                state.close()
            }
        }
    }

    // --- Made-up music ---

    private val nightTram = Artist("night-tram", "Night Tram", ProviderType.SPOTIFY)
    private val lanterns = Artist("paper-lanterns", "Paper Lanterns", ProviderType.SPOTIFY)

    private val songs: List<Track> = listOf(
        "Lamplight", "Low Tide", "Platform Nine", "Window Seat", "Sodium Lamps", "Static on the Line", "Harbour Lights", "Slow Ferry",
    ).mapIndexed { index, title ->
        val artist = if (index % 3 == 2) lanterns else nightTram
        Track(
            provider = ProviderType.SPOTIFY,
            id = "0sample${index}song".padEnd(22, '0'),
            title = title,
            artists = listOf(artist),
            album = Album("0harbourlights00000000", "Harbour Lights", listOf(nightTram), ProviderType.SPOTIFY, COVER + "harbour"),
            durationMs = 180_000L + index * 11_000L,
            artworkUrl = COVER + "harbour",
            sourceUrl = "https://open.spotify.com/track/0sample${index}song",
        )
    }

    /** The followed playlists as their pages would have read, for the library to list as the core lists them. */
    private val publicLists = followed.map { playlist ->
        SpotifyPublicList(
            id = playlist.id,
            title = playlist.title,
            owner = playlist.owner,
            artworkUrl = playlist.artworkUrl,
            tracks = songs.take(minOf(playlist.songCount ?: 0, songs.size)),
            mayBeLonger = playlist.mayBeLonger,
        )
    }

    private val imported = listOf(
        LocalPlaylist("sample-liked", SPOTIFY_LIKED_SONGS_TITLE, songs, importedFrom = PlaylistImport(PlaylistImport.SPOTIFY_DATA, "liked-songs")),
        LocalPlaylist("sample-kitchen", "Kitchen radio", songs.take(5), importedFrom = PlaylistImport(PlaylistImport.SPOTIFY_DATA, "playlist:Kitchen radio")),
    )

    private fun libraryFixture(library: app.noctorium.core.LibraryState) = library.copy(
        localPlaylists = listOf(LocalPlaylist("sample-own", "Night bus", songs.take(4))) + imported,
        // As the core lists them: the counts are what the page showed, not the few songs painted here.
        playlists = publicLists.map(SpotifyPublicLibrary::playlistOf).mapIndexed { index, playlist ->
            playlist.copy(trackCount = followed[index].songCount)
        } + Playlist("PLsample1", "Late night drive", ProviderType.YOUTUBE_MUSIC, ownerName = "You", artworkUrl = COVER + "bus", trackCount = 24, isPublic = false),
        loading = false,
        loaded = true,
        errorMessage = null,
        notice = null,
        needsSoundCloudUsername = false,
        openPlaylist = null,
        openPlaylistLoading = false,
        openPlaylistError = null,
        loadedAtMillis = System.currentTimeMillis(),
    )

    // --- Drawing ---

    /** [content] in [theme]'s colours, skin and type, on its page colour, a few frames in. */
    private fun draw(file: File, theme: ThemePreset, width: Int, height: Int, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width, height, Density(1f)) {
            BadgeTheme(theme) {
                CompositionLocalProvider(LocalMotion provides false) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.padding(20.dp)) { content() }
                    }
                }
            }
        }
        try {
            var image = scene.render(0)
            repeat(4) { frame ->
                Thread.sleep(40)
                image = scene.render((frame + 1) * 100_000_000L)
            }
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for ${file.name}")
        } finally {
            scene.close()
        }
    }

    /** Painted covers for every address above, in each size the loader might ask the store for. */
    private fun withCovers(draw: () -> Unit) {
        listOf("bus", "harbour", "rivers").forEachIndexed { index, name ->
            val cover = cover(index * 83f + 30f)
            listOf(64, 128, 256, 512, 1024).forEach { size -> ArtworkStore.put(COVER + name + ArtworkStore.SIZE_SEPARATOR + size, cover) }
        }
        try {
            draw()
        } finally {
            ArtworkStore.clear()
        }
    }

    private fun cover(hue: Float): ImageBitmap {
        val size = 256f
        val image = ImageBitmap(size.toInt(), size.toInt())
        val canvas = Canvas(image)
        canvas.drawRect(
            Rect(0f, 0f, size, size),
            Paint().apply {
                shader = LinearGradientShader(
                    Offset.Zero,
                    Offset(size, size),
                    listOf(Color.hsv(hue % 360f, .55f, .8f), Color.hsv((hue + 70f) % 360f, .7f, .35f)),
                )
            },
        )
        canvas.drawCircle(Offset(size * .62f, size * .38f), size * .18f, Paint().apply { color = Color.White.copy(alpha = .25f) })
        return image
    }

    private companion object {
        /** An address no loader can fetch, so a painted cover is all there is for it. */
        const val COVER = "test://cover/"
    }
}

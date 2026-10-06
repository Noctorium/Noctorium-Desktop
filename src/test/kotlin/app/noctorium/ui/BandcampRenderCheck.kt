package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.bandcamp.BandcampGenre
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.LibraryState
import app.noctorium.core.SearchMode
import app.noctorium.desktopAppState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.SearchResults
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.AccentPreset
import app.noctorium.settings.BandcampConnectionState
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.FontChoice
import app.noctorium.settings.HomePart
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.SettingsState
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws what Bandcamp added to the desktop to pictures, off screen, for looking at: a search that found
 * Bandcamp albums and artists beside songs from every service, a Bandcamp album's page, the player with a
 * Bandcamp song in it, and the Bandcamp settings page empty, checking, showing a collection and turned down.
 *
 * Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*BandcampRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * The covers are painted here and handed to the artwork store under addresses nothing can fetch, so the
 * pictures are the same every time and nothing goes to the network for them.
 */
class BandcampRenderCheck {

    private val folder: File? = System.getProperty("noctorium.renderBars")?.let(::File)?.also { it.mkdirs() }

    private val burial = Artist("1234", "Burial", ProviderType.BANDCAMP)

    private fun song(id: String, title: String, artist: String, provider: ProviderType, cover: String?, album: Album? = null) = Track(
        provider = provider,
        id = id,
        title = title,
        artists = listOf(Artist(artist.lowercase(), artist, provider)),
        album = album,
        durationMs = 240_000,
        artworkUrl = cover,
        sourceUrl = when (provider) {
            ProviderType.BANDCAMP -> "https://burial.bandcamp.com/track/${title.lowercase().replace(' ', '-')}#bandcamp-track=$id&band=1234"
            else -> "https://example.invalid/$id"
        },
    )

    private val untrue = Album("2220", "Untrue", listOf(burial), ProviderType.BANDCAMP, COVER + "untrue")

    private val albums = listOf(
        Playlist("album:1234:2220", "Untrue", ProviderType.BANDCAMP, "Burial", COVER + "untrue", sourceUrl = "https://burial.bandcamp.com/album/untrue"),
        Playlist("album:1234:2221", "Rival Dealer", ProviderType.BANDCAMP, "Burial", COVER + "rival", sourceUrl = "https://burial.bandcamp.com/album/rival-dealer"),
        Playlist("album:1234:2222", "Tunes 2011 to 2019, with a title long enough to need two lines", ProviderType.BANDCAMP, "Burial", COVER + "tunes"),
        Playlist("band:1234", "Burial", ProviderType.BANDCAMP, "London, UK", COVER + "artist", sourceUrl = "https://burial.bandcamp.com"),
        // Bandcamp has no picture for everybody.
        Playlist("band:5678", "Burial Hex", ProviderType.BANDCAMP, "Madison, Wisconsin", null),
    )

    private val songs = listOf(
        song("PzYrr7K1dvU", "Archangel", "Burial", ProviderType.YOUTUBE_MUSIC, COVER + "yt1"),
        song("3001", "Archangel", "Burial", ProviderType.BANDCAMP, COVER + "untrue", untrue),
        song("burial/near-dark", "Near Dark (live at Fabric)", "nightshift", ProviderType.SOUNDCLOUD, COVER + "sc1"),
        song("3002", "Etched Headplate", "Burial", ProviderType.BANDCAMP, COVER + "untrue", untrue),
        song("ytv1", "Burial - Untrue (full album)", "Hyperdub", ProviderType.YOUTUBE_VIDEO, COVER + "yt2"),
    )

    @Test
    fun `search draws Bandcamp's albums and artists above the songs`() {
        val folder = folder ?: return
        withCovers {
            withState { state ->
                val found = AppUiState(
                    searchQuery = "burial",
                    searchResults = SearchResults(tracks = songs, playlists = albums),
                    homeLoading = false,
                )
                draw(folder, "search-bandcamp-mixed.png", state, 1100, 860) { SearchScreen(found, state) }
                // The same search with no albums or artists: the list of songs as it always was.
                draw(folder, "search-songs-only.png", state, 1100, 520) {
                    SearchScreen(found.copy(searchResults = SearchResults(tracks = songs)), state)
                }
                draw(folder, "search-empty-hybrid.png", state, 1100, 420) { SearchScreen(AppUiState(homeLoading = false), state) }
                draw(folder, "search-empty-bandcamp.png", state, 1100, 420) {
                    SearchScreen(AppUiState(searchMode = SearchMode.BANDCAMP, homeLoading = false), state)
                }

                // The menu of a Bandcamp song, opened the way a listener opens it: by pointing and clicking.
                val scene = scene(state, 1100, 860) { SearchScreen(found, state) }
                try {
                    scene.render(0)
                    scene.render(100_000_000L)
                    val menu = Offset(MENU_X, BANDCAMP_ROW_Y)
                    scene.sendPointerEvent(PointerEventType.Move, menu)
                    scene.render(150_000_000L)
                    scene.sendPointerEvent(PointerEventType.Press, menu)
                    scene.render(200_000_000L)
                    scene.sendPointerEvent(PointerEventType.Release, menu)
                    // The menu opens in a popup that grows into place, so a few frames for it to arrive.
                    var image = scene.render(250_000_000L)
                    repeat(10) { frame ->
                        Thread.sleep(60)
                        image = scene.render(300_000_000L + frame * 60_000_000L)
                    }
                    File(folder, "search-bandcamp-menu.png").writeBytes(image.encodeToData()!!.bytes)
                } finally {
                    scene.close()
                }

                // Pointing at the faded download button, which says why it does nothing once the pointer rests.
                val hinted = scene(state, 1100, 860) { SearchScreen(found, state) }
                try {
                    hinted.render(0)
                    hinted.sendPointerEvent(PointerEventType.Move, Offset(DOWNLOAD_X, BANDCAMP_ROW_Y))
                    var image = hinted.render(50_000_000L)
                    repeat(12) { frame ->
                        Thread.sleep(80)
                        image = hinted.render(100_000_000L + frame * 80_000_000L)
                    }
                    File(folder, "search-bandcamp-download-hint.png").writeBytes(image.encodeToData()!!.bytes)
                } finally {
                    hinted.close()
                }

                // And on a light page, where a faded button and Bandcamp's teal have the least to stand on.
                state.setTheme(ThemePreset.NOCTORIUM_DAY)
                draw(folder, "search-bandcamp-mixed-day.png", state, 1100, 860) { SearchScreen(found, state) }
            }
        }
    }

    @Test
    fun `a Bandcamp album's page and the player draw`() {
        val folder = folder ?: return
        withCovers {
            withState { state ->
                val tracks = (1..9).map { n ->
                    song("40$n", listOf("Untitled", "Archangel", "Near Dark", "Ghost Hardware", "Endorphin", "Etched Headplate", "In McDonalds", "Untrue", "Shell of Light")[n - 1], "Burial", ProviderType.BANDCAMP, COVER + "untrue", untrue)
                }
                val album = albums.first().copy(tracks = tracks)
                draw(folder, "playlist-bandcamp-album.png", state, 1100, 640) {
                    PlaylistDetail(album, LibraryState(openPlaylist = album), state)
                }
                val playing = tracks[1]
                val queue = QueueState(tracks = tracks, currentIndex = 1)
                val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = playing, positionMs = 61_000, durationMs = 238_000)
                draw(folder, "bar-bandcamp-inline.png", state, 1280, 120, padded = false) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { PlayerBar(queue, playback, state) }
                }
            }
        }
    }

    @Test
    fun `the Bandcamp settings page draws in each of its states`() {
        val folder = folder ?: return
        withState { state ->
            val base = state.settings.value
            fun settings(
                username: String = "",
                connection: BandcampConnectionState = BandcampConnectionState(),
                genres: List<BandcampGenre> = BandcampGenre.DEFAULT_HOME,
                hidden: Set<HomePart> = emptySet(),
            ) = base.copy(
                preferences = base.preferences.copy(bandcampUsername = username, bandcampGenres = genres, hiddenHomeParts = hidden),
                bandcamp = connection,
            )

            panel(folder, "bandcamp-settings-empty.png", state, settings())
            panel(folder, "bandcamp-settings-checking.png", state, settings(connection = BandcampConnectionState(checking = true)))
            panel(
                folder,
                "bandcamp-settings-connected.png",
                state,
                settings(
                    "sample-fan",
                    BandcampConnectionState(fanName = "Sample Fan"),
                    genres = listOf(BandcampGenre.JAZZ, BandcampGenre.AMBIENT, BandcampGenre.ELECTRONIC, BandcampGenre.LOFI),
                ),
            )
            // After a restart: the name is kept, the fan's own name is not until Bandcamp is asked again.
            panel(folder, "bandcamp-settings-after-restart.png", state, settings("sample-fan", genres = emptyList(), hidden = setOf(HomePart.BANDCAMP)))
            panel(
                folder,
                "bandcamp-settings-error.png",
                state,
                settings(
                    connection = BandcampConnectionState(
                        message = "Bandcamp has no fan called \"sample-fann\". It is the name at the end of your Bandcamp address.",
                    ),
                ),
            )
            // The list of settings, with Bandcamp's tile among the services.
            draw(folder, "settings-home.png", state, 1000, 1180, padded = false) { SettingsScreen(state) }
            state.setTheme(ThemePreset.NOCTORIUM_DAY)
            panel(
                folder,
                "bandcamp-settings-connected-day.png",
                state,
                settings("sample-fan", BandcampConnectionState(fanName = "Sample Fan"), genres = listOf(BandcampGenre.JAZZ, BandcampGenre.AMBIENT)),
            )
        }
    }

    // --- Drawing ---

    private fun panel(folder: File, name: String, state: AppState, settings: SettingsState) =
        draw(folder, name, state, 760, 1040) { BandcampSettingsPanel(settings, state) }

    private fun draw(folder: File, name: String, state: AppState, width: Int, height: Int, padded: Boolean = true, content: @Composable () -> Unit) {
        val scene = scene(state, width, height, padded, content)
        try {
            // Twice: the first frame lays out, the second has what the first one's state changes asked for.
            scene.render(0)
            val file = File(folder, name)
            file.writeBytes(scene.render(100_000_000L).encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for $name")
        } finally {
            scene.close()
        }
    }

    /** The application's own theme and typeface on the page colour, still, as [PersonalisationRenderCheck] has it. */
    private fun scene(state: AppState, width: Int, height: Int, padded: Boolean = true, content: @Composable () -> Unit) =
        ImageComposeScene(width, height, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(
                colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null))),
                typography = noctoriumTypography(preferences.font),
            ) {
                CompositionLocalProvider(LocalMotion provides false) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (padded) Box(Modifier.padding(20.dp)) { content() } else content()
                    }
                }
            }
        }

    /**
     * A state for one check, with the look put back to the application's own before and after.
     *
     * The settings these read are the test run's, under build/test-home, and the other render checks leave
     * their themes there -- a check that stops short of its own reset leaves Windows 98 for the next one.
     * The pause before closing gives the last of the saves its moment, as in [PersonalisationRenderCheck].
     */
    private fun withState(check: (AppState) -> Unit) {
        val state = desktopAppState()
        try {
            reset(state)
            check(state)
        } finally {
            reset(state)
            Thread.sleep(400)
            state.close()
        }
    }

    private fun reset(state: AppState) {
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAccent(AccentPreset.THEME)
        state.setFont(FontChoice.DEFAULT)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setCornerStyle(CornerStyle.SOFT)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
    }

    /** Painted covers, one per address used above, in every size the loader might ask the store for. */
    private fun withCovers(draw: () -> Unit) {
        val names = listOf("untrue", "rival", "tunes", "artist", "yt1", "yt2", "sc1")
        names.forEachIndexed { index, name ->
            val cover = cover(index * 47f)
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
        canvas.drawCircle(Offset(size * .62f, size * .4f), size * .22f, Paint().apply { color = Color.White.copy(alpha = .22f) })
        return image
    }

    private companion object {
        /** An address no loader can fetch, so a painted cover is all there is for it. */
        const val COVER = "test://cover/"

        /** Where the menu and download buttons of the first Bandcamp song sit in the 1100 by 860 search picture. */
        const val MENU_X = 1021f
        const val DOWNLOAD_X = 987f
        const val BANDCAMP_ROW_Y = 741f
    }
}

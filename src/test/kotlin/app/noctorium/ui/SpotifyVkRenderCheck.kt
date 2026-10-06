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
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.LibraryState
import app.noctorium.core.SearchMode
import app.noctorium.desktopAppState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.HomeSection
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.SearchResults
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.AccentPreset
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.EqualizerPreset
import app.noctorium.settings.EqualizerSettings
import app.noctorium.settings.FontChoice
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.SettingsRepository
import app.noctorium.settings.SettingsState
import app.noctorium.settings.SpotifyConnectionState
import app.noctorium.settings.SpotifyPlayback
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.VkConnectionState
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.spotify.SpotifyDevice
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws what Spotify's two sign-ins and VK Music added to the desktop to pictures, off screen, for looking
 * at: both Settings pages in each of their states, a search with Spotify's albums and artists in it, a
 * Spotify album and a VK playlist, the player bars and now playing with a song playing in Spotify's own
 * app, the equaliser resting while it does, and Home's filter with every service in it.
 *
 * Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*SpotifyVkRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * Every name and value here is made up. The settings it reads and writes are the test run's own, under
 * build/test-home, and the covers are painted here, so nothing goes to the network for them.
 */
class SpotifyVkRenderCheck {

    private val folder: File? = System.getProperty("noctorium.renderBars")?.let(::File)?.also { it.mkdirs() }

    private fun song(id: String, title: String, artist: String, provider: ProviderType, cover: String?, album: Album? = null) = Track(
        provider = provider,
        id = id,
        title = title,
        artists = listOf(Artist(artist.lowercase(), artist, provider)),
        album = album,
        durationMs = 214_000,
        artworkUrl = cover,
        sourceUrl = when (provider) {
            ProviderType.SPOTIFY -> "https://open.spotify.com/track/$id"
            ProviderType.VK -> "https://vk.ru/audio-1000_$id#vk-access=sample"
            else -> "https://example.invalid/$id"
        },
    )

    private val harbour = Album("0harbourlights00000000", "Harbour Lights", listOf(Artist("night-tram", "Night Tram", ProviderType.SPOTIFY)), ProviderType.SPOTIFY, COVER + "harbour")

    private val spotifyShelf = listOf(
        Playlist("album:0harbourlights00000000", "Harbour Lights", ProviderType.SPOTIFY, "Night Tram", COVER + "harbour", sourceUrl = "https://open.spotify.com/album/0harbourlights00000000"),
        Playlist("album:0slowrivers0000000000", "Slow Rivers", ProviderType.SPOTIFY, "Paper Lanterns, Night Tram", COVER + "rivers"),
        Playlist("artist:0nighttram000000000000", "Night Tram", ProviderType.SPOTIFY, "Artist", COVER + "tram"),
        Playlist("band:7700", "Night Tram Collective", ProviderType.BANDCAMP, "Leeds, UK", COVER + "collective"),
        Playlist("artist:0papelanterns00000000", "Paper Lanterns", ProviderType.SPOTIFY, "Artist", null),
    )

    private val spotifySongs = listOf(
        song("0lamplight0000000000000", "Lamplight", "Night Tram", ProviderType.SPOTIFY, COVER + "harbour", harbour),
        song("ytm1", "Lamplight (live)", "Night Tram", ProviderType.YOUTUBE_MUSIC, COVER + "live"),
        song("88001", "Lamplight", "Night Tram", ProviderType.VK, COVER + "vk"),
        song("0lowtide000000000000000", "Low Tide", "Night Tram", ProviderType.SPOTIFY, COVER + "harbour", harbour),
    )

    private val devices = listOf(
        SpotifyDevice("dev-desk", "Studio Desk", "Computer", isActive = true, volumePercent = 64),
        SpotifyDevice("dev-phone", "Pocket Phone", "Smartphone", isActive = false),
        SpotifyDevice("dev-hall", "Hall Speaker", "Speaker", isActive = false, isRestricted = true),
    )

    @Test
    fun `the Spotify page draws not connected, with the library, and with Premium`() {
        val folder = folder ?: return
        withState { state ->
            val base = state.settings.value
            fun with(spotify: SpotifyConnectionState) = base.copy(spotify = spotify)
            panel(folder, "spotify-not-connected.png", state, with(SpotifyConnectionState())) { SpotifySettingsPanel(it, state) }
            panel(
                folder,
                "spotify-library-only.png",
                state,
                with(SpotifyConnectionState(connected = true, accountName = "Sample Listener", message = "Spotify connected as Sample Listener. Your playlists are in the library.")),
            ) { SpotifySettingsPanel(it, state) }
            panel(
                folder,
                "spotify-premium-devices.png",
                state,
                with(SpotifyConnectionState(connected = true, accountName = "Sample Listener", canPlay = true, playsOnSpotify = true, devices = devices, device = "dev-phone")),
            ) { SpotifySettingsPanel(it, state) }
            // A device chosen before that Spotify did not list this time.
            panel(
                folder,
                "spotify-premium-device-away.png",
                state,
                with(SpotifyConnectionState(connected = true, accountName = "Sample Listener", canPlay = true, playsOnSpotify = false, devices = devices.take(1), device = "dev-gone")),
            ) { SpotifySettingsPanel(it, state) }
        }
    }

    @Test
    fun `the VK page draws signed out, checking, signed in and turned down`() {
        val folder = folder ?: return
        withState { state ->
            val base = state.settings.value
            fun with(vk: VkConnectionState) = base.copy(vk = vk)
            panel(folder, "vk-signed-out.png", state, with(VkConnectionState())) { VkSettingsPanel(it, state) }
            // A few frames, so the spinner is caught turning rather than at its first dot.
            draw(folder, "vk-checking.png", state, 760, 1400, frames = 6) { VkSettingsPanel(with(VkConnectionState(checking = true)), state) }
            // The pasting fold, opened by clicking it; whatever is pasted there is shown as dots.
            drawAfterClick(folder, "vk-paste-cookies.png", state, 760, 1400, Offset(PASTE_X, PASTE_Y)) { VkSettingsPanel(with(VkConnectionState()), state) }
            panel(
                folder,
                "vk-signed-in.png",
                state,
                with(VkConnectionState(connected = true, accountName = "Sample Listener", message = "Signed in to VK as Sample Listener. VK may hold some songs back outside Russia.")),
            ) { VkSettingsPanel(it, state) }
            panel(
                folder,
                "vk-error.png",
                state,
                with(VkConnectionState(message = "VK says the session has ended. Sign in on vk.ru again.")),
            ) { VkSettingsPanel(it, state) }
            // The list of settings, with both tiles among the services -- and without the note a test run's own
            // settings file can leave there, when two saves of it meet.
            Thread.sleep(300)
            state.clearSettingsMessage()
            draw(folder, "settings-home-services.png", state, 1000, 1320, padded = false) { SettingsScreen(state) }
        }
    }

    @Test
    fun `search, an album, a VK playlist and Home draw with Spotify and VK in them`() {
        val folder = folder ?: return
        withCovers {
            withState { state ->
                val found = AppUiState(
                    searchQuery = "night tram",
                    searchResults = SearchResults(tracks = spotifySongs, playlists = spotifyShelf),
                    homeLoading = false,
                )
                draw(folder, "search-spotify-shelf.png", state, 1100, 860) { SearchScreen(found, state) }
                // Spotify picked with nobody signed in to it, before and after typing.
                draw(folder, "search-spotify-empty-not-connected.png", state, 1100, 420) {
                    SearchScreen(AppUiState(searchMode = SearchMode.SPOTIFY, homeLoading = false), state)
                }
                draw(folder, "search-spotify-not-connected.png", state, 1100, 420) {
                    SearchScreen(found.copy(searchMode = SearchMode.SPOTIFY), state)
                }

                // The menu of the VK song in that search, opened as a listener opens it.
                drawAfterClick(folder, "search-vk-menu.png", state, 1100, 860, Offset(MENU_X, VK_ROW_Y)) { SearchScreen(found, state) }

                val album = spotifyShelf.first().copy(tracks = spotifySongs.filter { it.provider == ProviderType.SPOTIFY })
                draw(folder, "playlist-spotify-album.png", state, 1100, 420) { PlaylistDetail(album, LibraryState(openPlaylist = album), state) }
                val vkPlaylist = Playlist(
                    "my-music",
                    "My music",
                    ProviderType.VK,
                    "VK",
                    sourceUrl = "https://vk.ru/audios1000",
                    tracks = (1..5).map { song("8800$it", "Song for a tram, part $it", "Night Tram", ProviderType.VK, COVER + "vk") },
                )
                draw(folder, "playlist-vk-my-music.png", state, 1100, 520) { PlaylistDetail(vkPlaylist, LibraryState(openPlaylist = vkPlaylist), state) }

                // Home's filter with every service in it, at a wide window and at the narrowest the window goes.
                val home = AppUiState(
                    homeSections = listOf(
                        HomeSection("spotify:top", "Your top songs on Spotify", ProviderType.SPOTIFY, "The last few weeks", spotifySongs.filter { it.provider == ProviderType.SPOTIFY }),
                        HomeSection("vk:recommended", "Suggested on VK", ProviderType.VK, "Picked for your account", spotifySongs.filter { it.provider == ProviderType.VK }),
                    ),
                    homeLoading = false,
                )
                draw(folder, "home-filters-wide.png", state, 1100, 520, padded = false) { HomeScreen(home, state) }
                // 760 points of window, less the 196 the sidebar takes.
                draw(folder, "home-filters-narrow.png", state, 564, 520, padded = false) { HomeScreen(home, state) }
            }
        }
    }

    @Test
    fun `the player shows a song playing on Spotify, and the equaliser rests`() {
        val folder = folder ?: return
        // Spotify songs set to play on Spotify, in the test run's own settings: the state reads them when it
        // starts, and says so in what it publishes about Spotify.
        val repository = SettingsRepository()
        val before = repository.load()
        repository.save(before.copy(spotifyCanPlay = true, spotifyPlayback = SpotifyPlayback.ON_SPOTIFY))
        try {
            withCovers {
                withState { state ->
                    val deadline = System.currentTimeMillis() + 10_000
                    while (!state.settings.value.spotify.playsOnSpotify && System.currentTimeMillis() < deadline) Thread.sleep(50)
                    assertTrue(state.settings.value.spotify.playsOnSpotify, "the state never said Spotify songs play on Spotify")

                    val playing = spotifySongs.first()
                    val queue = QueueState(tracks = spotifySongs, currentIndex = 0)
                    val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = playing, positionMs = 71_000, durationMs = 214_000)
                    PlayerBarStyle.entries.forEach { style ->
                        state.setPlayerBarStyle(style)
                        draw(folder, "bar-on-spotify-${style.name.lowercase()}.png", state, 1280, 160, padded = false) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { PlayerBar(queue, playback, state) }
                        }
                    }
                    state.setPlayerBarStyle(PlayerBarStyle.INLINE)

                    // The volume, opened from the bar, with the boost resting.
                    drawAfterClick(folder, "bar-on-spotify-volume.png", state, 1280, 420, Offset(VOLUME_X, VOLUME_Y), padded = false) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { PlayerBar(queue, playback, state) }
                    }

                    draw(folder, "now-playing-on-spotify.png", state, 1280, 760, padded = false, frames = 30) {
                        NowPlayingScreen(queue, playback, state)
                    }
                    state.updateEqualizer { EqualizerSettings(enabled = true, preset = EqualizerPreset.ROCK) }
                    draw(folder, "sound-equaliser-on-spotify.png", state, 760, 760) {
                        val equalizer = state.settings.collectAsState().value.preferences.equalizer
                        EqualizerCard(equalizer, state, onSpotify = true)
                    }
                    state.updateEqualizer { EqualizerSettings() }
                }
            }
        } finally {
            // Back as it was, after the state has gone, so nothing it saves afterwards undoes this.
            repository.save(repository.load().copy(spotifyCanPlay = before.spotifyCanPlay, spotifyPlayback = before.spotifyPlayback))
        }
    }

    // --- Drawing ---

    private fun panel(folder: File, name: String, state: AppState, settings: SettingsState, content: @Composable (SettingsState) -> Unit) =
        draw(folder, name, state, 760, 1400) { content(settings) }

    private fun draw(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        padded: Boolean = true,
        frames: Int = 1,
        content: @Composable () -> Unit,
    ) {
        val scene = scene(state, width, height, padded, content)
        try {
            var image = scene.render(0)
            repeat(frames) { frame ->
                if (frames > 1) Thread.sleep(80)
                image = scene.render((frame + 1) * 100_000_000L)
            }
            val file = File(folder, name)
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for $name")
        } finally {
            scene.close()
        }
    }

    /**
     * Draws [content] after a click [at] a point, the way a listener opens a menu or a fold: the pointer arrives,
     * presses and lets go, and a few frames pass for whatever opens to grow into place.
     */
    private fun drawAfterClick(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        at: Offset,
        padded: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        val scene = scene(state, width, height, padded, content)
        try {
            scene.render(0)
            scene.render(100_000_000L)
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.render(150_000_000L)
            scene.sendPointerEvent(PointerEventType.Press, at)
            scene.render(200_000_000L)
            scene.sendPointerEvent(PointerEventType.Release, at)
            var image = scene.render(250_000_000L)
            repeat(10) { frame ->
                Thread.sleep(60)
                image = scene.render(300_000_000L + frame * 60_000_000L)
            }
            File(folder, name).writeBytes(image.encodeToData()!!.bytes)
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

    /** A state for one check, with the look put back to the application's own before and after. See [BandcampRenderCheck]. */
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

    /** Painted covers for every address above, in each size the loader might ask the store for. */
    private fun withCovers(draw: () -> Unit) {
        val names = listOf("harbour", "rivers", "tram", "collective", "live", "vk")
        names.forEachIndexed { index, name ->
            val cover = cover(index * 61f + 20f)
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
        canvas.drawCircle(Offset(size * .38f, size * .62f), size * .2f, Paint().apply { color = Color.White.copy(alpha = .22f) })
        return image
    }

    private companion object {
        /** An address no loader can fetch, so a painted cover is all there is for it. */
        const val COVER = "test://cover/"

        /** Where the VK song's menu button sits in the 1100 by 860 search picture. */
        const val MENU_X = 1021f
        const val VK_ROW_Y = 788f

        /** Where "Paste cookies instead" sits on the signed-out VK page, in a 760-wide picture. */
        const val PASTE_X = 290f
        const val PASTE_Y = 582f

        /** Where the volume button sits on the Inline bar, at the foot of a 1280 by 420 picture. */
        const val VOLUME_X = 1248f
        const val VOLUME_Y = 383f
    }
}

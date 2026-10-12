package app.noctorium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
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
import app.noctorium.account.NoctoriumAccountClient
import app.noctorium.core.AppState
import app.noctorium.core.LibraryState
import app.noctorium.domain.Artist
import app.noctorium.domain.PlaybackContext
import app.noctorium.domain.Playlist
import app.noctorium.domain.PlaylistVisibility
import app.noctorium.domain.ProviderType
import app.noctorium.domain.SearchResults
import app.noctorium.domain.HomeSection
import app.noctorium.domain.Track
import app.noctorium.domain.withVisibility
import app.noctorium.platform.SystemBridge
import app.noctorium.playback.MusicBackend
import app.noctorium.playback.PlaybackEngine
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.QueueStore
import app.noctorium.providers.MusicProvider
import app.noctorium.settings.ConnectPreferences
import app.noctorium.settings.CookieSource
import app.noctorium.settings.InMemorySecretStore
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.SettingsRepository
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.themeSkin
import app.noctorium.settings.windows98Palette
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.ui.skins.SkinnedMaterialTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws a YouTube Music playlist's header and its Edit dialog to pictures, off screen, for looking at: the
 * listener's own playlist when public, unlisted and private, one they only saved, one whose page has not said
 * yet, what is shown when YouTube turns a change down, the privacy menu, and SoundCloud's header beside them to
 * see it has not moved.
 *
 * Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*YouTubePlaylistRenderCheck*" -Dnoctorium.renderBars=build/youtube
 *
 * Nothing here is the desktop's own state: the services are stand-ins that answer from memory and the settings
 * are a file of the check's own, so nothing reaches YouTube or any other service, and every name and title is
 * made up.
 */
class YouTubePlaylistRenderCheck {

    private val folder: File? = System.getProperty("noctorium.renderBars")?.let(::File)?.also { it.mkdirs() }

    private fun song(id: String, title: String, artist: String, cover: String) = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = id,
        title = title,
        artists = listOf(Artist(artist.lowercase(), artist, ProviderType.YOUTUBE_MUSIC)),
        durationMs = 214_000,
        artworkUrl = COVER + cover,
        sourceUrl = "https://music.youtube.com/watch?v=$id",
    )

    private val songs = listOf(
        song("nTlamplight", "Lamplight", "Night Tram", "tram"),
        song("pLlowtide00", "Low Tide", "Paper Lanterns", "tide"),
        song("sRtakethree", "Slow Rivers", "Paper Lanterns", "rivers"),
        song("hLharbour00", "Harbour Lights", "Night Tram", "harbour"),
    )

    private val own = Playlist(
        "PLnightBusHome000000000000000000aa",
        "Night Bus Home",
        ProviderType.YOUTUBE_MUSIC,
        ownerName = "Sample Listener",
        artworkUrl = COVER + "tram",
        tracks = songs,
        sourceUrl = "https://music.youtube.com/playlist?list=PLnightBusHome000000000000000000aa",
        description = "Songs for the last bus home, window seat, rain on the glass.",
        ownedByListener = true,
    )

    @Test
    fun `the header draws for each privacy, for a saved playlist, and for one not read yet`() {
        val folder = folder ?: return
        withCovers {
            withState { state ->
                PlaylistVisibility.entries.forEach { visibility ->
                    val playlist = own.withVisibility(visibility)
                    draw(folder, "header-own-${visibility.name.lowercase()}.png", state, 1100, 460) {
                        PlaylistDetail(playlist, LibraryState(openPlaylist = playlist), state)
                    }
                }
                val saved = own.copy(title = "Tram Stop Classics", ownerName = "Another Listener", description = null, ownedByListener = false)
                draw(folder, "header-saved.png", state, 1100, 460) { PlaylistDetail(saved, LibraryState(openPlaylist = saved), state) }

                // Before the page has said: while it loads nothing is offered, and if it never says, Edit is.
                val unread = own.copy(ownedByListener = null, description = null)
                draw(folder, "header-unread-loading.png", state, 1100, 300) {
                    PlaylistDetail(unread, LibraryState(openPlaylist = unread, openPlaylistLoading = true), state)
                }
                draw(folder, "header-unread.png", state, 1100, 460) { PlaylistDetail(unread, LibraryState(openPlaylist = unread), state) }

                val private = own.withVisibility(PlaylistVisibility.PRIVATE)
                draw(folder, "header-refused.png", state, 1100, 460) {
                    PlaylistDetail(
                        private,
                        LibraryState(
                            openPlaylist = private,
                            notice = "YouTube Music turned the change down (HTTP 403): the playlist may not be yours, or the " +
                                "sign-in has run out. Sign in again under Settings › YouTube Music if it keeps happening.",
                            noticeIsProblem = true,
                        ),
                        state,
                    )
                }
                val unlisted = own.withVisibility(PlaylistVisibility.UNLISTED)
                draw(folder, "header-saved-notice.png", state, 1100, 460) {
                    PlaylistDetail(unlisted, LibraryState(openPlaylist = unlisted, notice = "Playlist is now unlisted on YouTube Music."), state)
                }

                // The badge opened as a listener opens it, by clicking it.
                drawAfterClick(folder, "header-privacy-menu.png", state, 1100, 460, Offset(BADGE_X, BADGE_Y)) {
                    PlaylistDetail(private, LibraryState(openPlaylist = private), state)
                }

                // SoundCloud's two answers, as before.
                val soundCloud = Playlist("1001", "Rainy Sunday", ProviderType.SOUNDCLOUD, "Sample Listener", COVER + "rivers", songs, isPublic = true)
                draw(folder, "header-soundcloud.png", state, 1100, 460) { PlaylistDetail(soundCloud, LibraryState(openPlaylist = soundCloud), state) }

                // The small badges the library list carries, one of each.
                draw(folder, "badges-compact.png", state, 400, 80) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaylistVisibility.entries.forEach { VisibilityBadge(it, compact = true) }
                    }
                }
            }
        }
    }

    @Test
    fun `the Edit dialog draws for each privacy, unknown, chosen, and under a Windows skin`() {
        val folder = folder ?: return
        withState { state ->
            PlaylistVisibility.entries.forEach { visibility ->
                draw(folder, "edit-${visibility.name.lowercase()}.png", state, 760, 640) {
                    YouTubePlaylistEditDialog(own.withVisibility(visibility)) {}
                }
            }
            draw(folder, "edit-unknown.png", state, 760, 680) {
                YouTubePlaylistEditDialog(own.copy(description = null)) {}
            }
            // Unlisted picked on a private playlist: Save wakes up.
            drawAfterClick(folder, "edit-private-choosing-unlisted.png", state, 760, 640, Offset(UNLISTED_X, UNLISTED_Y)) {
                YouTubePlaylistEditDialog(own.withVisibility(PlaylistVisibility.PRIVATE)) {}
            }
            // Under the 98 skin, in the 98 theme's own colours, as a listener who chose it sees it.
            state.setTheme(ThemePreset.WINDOWS_98)
            draw(folder, "edit-windows-98.png", state, 760, 680) {
                YouTubePlaylistEditDialog(own.withVisibility(PlaylistVisibility.UNLISTED)) {}
            }
        }
    }

    // --- Drawing ---

    private fun draw(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        content: @Composable () -> Unit,
    ) {
        val scene = scene(state, width, height, content)
        try {
            scene.render(0)
            var image = scene.render(100_000_000L)
            repeat(4) { frame ->
                Thread.sleep(40)
                image = scene.render(200_000_000L + frame * 100_000_000L)
            }
            val file = File(folder, name)
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for $name")
        } finally {
            scene.close()
        }
    }

    /** [content] after a click [at] a point, with a few frames for whatever opens to grow into place. */
    private fun drawAfterClick(folder: File, name: String, state: AppState, width: Int, height: Int, at: Offset, content: @Composable () -> Unit) {
        val scene = scene(state, width, height, content)
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

    private fun scene(state: AppState, width: Int, height: Int, content: @Composable () -> Unit) =
        ImageComposeScene(width, height, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            SkinnedMaterialTheme(
                preferences.themeSkin,
                preferences.themeColours(),
                Color(preferences.resolvedAccent(null)),
                preferences.font,
                preferences.cornerStyle,
                preferences.windows98Palette,
            ) {
                CompositionLocalProvider(LocalMotion provides false) {
                    app.noctorium.ui.skins.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.padding(20.dp)) { content() }
                    }
                }
            }
        }

    /**
     * A state with nothing real behind it, so that drawing a playlist cannot reach a service: stand-in services
     * with nothing to say, a player that plays nothing, no YouTube sign-in, Connect off, and a settings file of
     * its own under the test run's folder.
     */
    private fun withState(check: (AppState) -> Unit) {
        val home = Path.of(System.getProperty("noctorium.home") ?: "build/test-home").resolve("youtube-render")
        Files.createDirectories(home)
        val settings = Files.createTempDirectory(home, "state").resolve("settings.json")
        SettingsRepository(settings).save(
            NoctoriumPreferences(autoplay = false, connect = ConnectPreferences(enabled = false), youtubeCookies = CookieSource()),
        )
        val state = AppState(
            ytDlp = Backend,
            credentials = InMemorySecretStore(),
            system = Bridge,
            playbackEngine = Engine(),
            injectedProviders = ProviderType.entries.filter { it != ProviderType.LOCAL }.map(::Silent),
            settingsRepository = SettingsRepository(settings),
            queueStore = QueueStore(null),
            checkForUpdatesAtLaunch = false,
            refreshAtLaunch = false,
            accountClient = NoctoriumAccountClient(baseUrl = "http://127.0.0.1:9"),
        )
        try {
            check(state)
        } finally {
            state.close()
        }
    }

    private class Silent(override val type: ProviderType) : MusicProvider {
        override suspend fun getHome(): List<HomeSection> = emptyList()
        override suspend fun search(query: String): SearchResults = SearchResults()
        override suspend fun getTrack(id: String): Track? = null
        override suspend fun getRecommendations(context: PlaybackContext): List<Track> = emptyList()
    }

    private class Engine : PlaybackEngine {
        override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
        override suspend fun play(track: Track) = Unit
        override suspend fun pause() = Unit
        override suspend fun resume() = Unit
        override suspend fun setVolume(value: Float) = Unit
        override suspend fun setVolumeBoost(enabled: Boolean) = Unit
        override suspend fun setMuted(muted: Boolean) = Unit
        override suspend fun seekTo(positionMs: Long) = Unit
        override suspend fun stop() = Unit
        override fun close() = Unit
    }

    private object Backend : MusicBackend {
        override fun useSession(provider: ProviderType, source: CookieSource) = Unit
        override fun useSoundCloudProfile(username: String) = Unit
        override val soundCloudProfile: String = ""
        override suspend fun search(provider: ProviderType, query: String, limit: Int): List<Track> = emptyList()
        override suspend fun listPlaylists(provider: ProviderType, url: String, limit: Int): List<Playlist> = emptyList()
        override suspend fun listTracks(provider: ProviderType, url: String, limit: Int): List<Track> = emptyList()
        override suspend fun resolveTracks(provider: ProviderType, url: String, from: Int, to: Int): List<Track> = emptyList()
        override suspend fun enrichMetadata(track: Track): Track = track
        override suspend fun resolveAudio(sourceUrl: String): String = sourceUrl
        override suspend fun resolveSoundCloudPermalink(userId: String): String? = null
        override suspend fun downloadAudio(sourceUrl: String, outputTemplate: String, onProgress: (Float) -> Unit) = Unit
        override suspend fun exportAudio(sourceUrl: String, outputTemplate: String, onProgress: (Float) -> Unit) = Unit
        override suspend fun describe(): String = "nothing"
    }

    private object Bridge : SystemBridge {
        override fun openUrl(url: String) = Unit
        override fun copyToClipboard(text: String) = Unit
        override fun defaultExportFolder(): Path? = null
    }

    /** Painted covers for every address above, in each size the loader might ask the store for. */
    private fun withCovers(draw: () -> Unit) {
        listOf("tram", "tide", "rivers", "harbour").forEachIndexed { index, name ->
            val cover = cover(index * 73f + 200f)
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
        return image
    }

    private companion object {
        /** An address no loader can fetch, so a painted cover is all there is for it. */
        const val COVER = "test://cover/"

        /** Where the privacy badge sits in the 1100 by 460 header picture. */
        const val BADGE_X = 95f
        const val BADGE_Y = 138f

        /** Where "Unlisted" sits in the 760 by 640 Edit dialog picture. */
        const val UNLISTED_X = 380f
        const val UNLISTED_Y = 420f
    }
}

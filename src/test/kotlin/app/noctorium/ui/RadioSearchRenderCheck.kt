package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.account.NoctoriumAccountClient
import app.noctorium.core.AppState
import app.noctorium.core.SearchKind
import app.noctorium.domain.HomeSection
import app.noctorium.domain.PlaybackContext
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.SearchResults
import app.noctorium.domain.Track
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
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.settings.themeSkin
import app.noctorium.settings.windows98Palette
import app.noctorium.ui.skins.SkinnedMaterialTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

/**
 * Draws the search with Music and with Radio chosen -- the page, its Music / Radio menu open, a country picked,
 * and the Ctrl+F bar -- for looking at. Radio's stations are Radio Browser's own, fetched live, so this asks the
 * network; every music service is a stand-in with nothing to say.
 *
 * Off unless a folder is named:
 *
 *     ./gradlew :test --tests "*RadioSearchRenderCheck*" -Dnoctorium.renderBars=build/radio
 */
class RadioSearchRenderCheck {

    private val folder: File? = System.getProperty("noctorium.renderBars")?.let(::File)?.also { it.mkdirs() }

    @Test
    fun `the search draws as Music and as Radio, and so does the quick bar`() {
        val folder = folder ?: return
        withState { state ->
            draw(folder, "search-music.png", state, 1100, 520) { val ui by state.ui.collectAsState(); SearchScreen(ui, state) }
            drawAfterClick(folder, "search-kind-menu.png", state, 1100, 520, Offset(205f, 75f)) { val ui by state.ui.collectAsState(); SearchScreen(ui, state) }

            state.setSearchKind(SearchKind.RADIO)
            waitFor { state.ui.value.radioStations.isNotEmpty() && !state.ui.value.searchLoading && state.ui.value.radioCountries.isNotEmpty() }
            draw(folder, "search-radio-world.png", state, 1100, 820) { val ui by state.ui.collectAsState(); SearchScreen(ui, state) }

            state.setRadioCountry("FR")
            state.search("jazz")
            waitFor { state.ui.value.searchQuery == "jazz" && !state.ui.value.searchLoading && state.ui.value.radioStations.isNotEmpty() }
            draw(folder, "search-radio-jazz-france.png", state, 1100, 820) { val ui by state.ui.collectAsState(); SearchScreen(ui, state) }
            drawAfterClick(folder, "search-country-menu.png", state, 1100, 820, Offset(110f, 199f)) { val ui by state.ui.collectAsState(); SearchScreen(ui, state) }

            draw(folder, "quick-radio.png", state, 760, 560) {
                QuickSearchBar(state, leave = {}, modifier = Modifier.fillMaxWidth(), initial = "jazz")
            }
        }
    }

    private fun waitFor(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 30_000
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "Radio Browser did not answer in time" }
            Thread.sleep(100)
        }
        // A moment for the logos to start arriving.
        Thread.sleep(2_500)
    }

    private fun draw(folder: File, name: String, state: AppState, width: Int, height: Int, content: @Composable () -> Unit) {
        val scene = scene(state, width, height, content)
        try {
            scene.render(0)
            var image = scene.render(100_000_000L)
            repeat(12) { frame ->
                Thread.sleep(150)
                image = scene.render(200_000_000L + frame * 100_000_000L)
            }
            File(folder, name).writeBytes(image.encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }

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
                Thread.sleep(80)
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

    private fun withState(check: (AppState) -> Unit) {
        val home = Path.of(System.getProperty("noctorium.home") ?: "build/test-home").resolve("radio-render")
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
            injectedProviders = ProviderType.entries.filter { it != ProviderType.LOCAL && it != ProviderType.RADIO }.map(::Silent),
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
}

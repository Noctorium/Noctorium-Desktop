package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.desktopAppState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.CoverStyle
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.nowPlayingBackdrop
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the now playing screen in every layout, and in a few of its looks, to pictures, off screen; and the
 * Settings card and the screen's own menu that choose them. Off unless a folder is named, like
 * [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*NowPlayingRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class NowPlayingRenderCheck {

    private val artist = Artist("judy-collins", "Judy Collins", ProviderType.YOUTUBE_MUSIC)

    private val track = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "dQw4w9WgXcQ",
        title = "Amazing Grace",
        artists = listOf(artist),
        album = Album("whales-and-nightingales", "Whales & Nightingales", listOf(artist), ProviderType.YOUTUBE_MUSIC),
        durationMs = 245_000,
        artworkUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
        sourceUrl = "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
    )

    private val others = (1..8).map { n ->
        Track(
            provider = if (n % 2 == 0) ProviderType.SOUNDCLOUD else ProviderType.YOUTUBE_MUSIC,
            id = "other-$n",
            title = "Another song $n",
            artists = listOf(Artist("someone-$n", "Someone $n", ProviderType.YOUTUBE_MUSIC)),
            durationMs = 180_000L + n * 7_000L,
            sourceUrl = "https://example.invalid/$n",
        )
    }

    @Test
    fun `every now playing layout draws`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val queue = QueueState(tracks = listOf(others[0], others[1], track) + others.drop(2), currentIndex = 2)
        val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = track, positionMs = 83_000, durationMs = 245_000)
        val state = desktopAppState()
        try {
            state.setTheme(ThemePreset.CRIMSON)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            NowPlayingLayout.entries.forEach { layout ->
                state.updateNowPlaying { NowPlayingPreferences(layout = layout) }
                screen(folder, "now-playing-${layout.name.lowercase()}.png", state, queue, playback)
            }
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.STAGE, cover = CoverStyle.RECORD) }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.COVER)
            screen(folder, "now-playing-stage-record-blurred.png", state, queue, playback)
            state.updateNowPlaying { copy(layout = NowPlayingLayout.FOCUS, cover = CoverStyle.CIRCLE) }
            screen(folder, "now-playing-focus-circle-blurred.png", state, queue, playback)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.SIDE_BY_SIDE, panelHidden = true) }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
            screen(folder, "now-playing-panel-hidden.png", state, queue, playback)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.SING_ALONG) }
            screen(folder, "now-playing-sing_along-narrow.png", state, queue, playback, width = 800)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.STAGE) }
            screen(folder, "now-playing-stage-narrow.png", state, queue, playback, width = 800)
            // The menu, opened the way a listener opens it: a click on the sliders at the foot of the hero.
            state.updateNowPlaying { NowPlayingPreferences() }
            screen(folder, "now-playing-menu-open.png", state, queue, playback, click = Offset(765f, 705f))
            state.setTheme(ThemePreset.WINDOWS_XP)
            state.setProgressBarStyle(ProgressBarStyle.CLASSIC)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.BANNER, cover = CoverStyle.SQUARE) }
            screen(folder, "now-playing-banner-xp.png", state, queue, playback)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            state.updateNowPlaying { NowPlayingPreferences() }
            panel(folder, "now-playing-settings.png", state, 760, 1180) { NowPlayingSettingsCard(it, state) }
            panel(folder, "now-playing-menu.png", state, 420, 560) {
                Surface(Modifier.width(384.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    NowPlayingArranger(it.desktop.nowPlaying, it.nowPlayingBackdrop, state) {}
                }
            }
        } finally {
            state.updateNowPlaying { NowPlayingPreferences() }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            state.close()
        }
    }

    private fun screen(
        folder: File,
        name: String,
        state: AppState,
        queue: QueueState,
        playback: PlaybackState,
        width: Int = 1280,
        click: Offset? = null,
    ) {
        val scene = ImageComposeScene(width, 760, Density(1f)) {
            themed(state) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NowPlayingScreen(queue, playback, state)
                }
            }
        }
        try {
            // Long enough for the cover to arrive over the network the first time, and for lyrics.
            var image = scene.render(0)
            repeat(60) { frame ->
                Thread.sleep(100)
                image = scene.render((frame + 1) * 100_000_000L)
            }
            if (click != null) {
                scene.sendPointerEvent(PointerEventType.Move, click)
                scene.sendPointerEvent(PointerEventType.Press, click)
                scene.sendPointerEvent(PointerEventType.Release, click)
                repeat(10) { frame ->
                    Thread.sleep(50)
                    image = scene.render((61 + frame) * 100_000_000L)
                }
            }
            val file = File(folder, name)
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000)
        } finally {
            scene.close()
        }
    }

    private fun panel(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        content: @Composable (app.noctorium.settings.NoctoriumPreferences) -> Unit,
    ) {
        val scene = ImageComposeScene(width, height, Density(1f)) {
            themed(state) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(18.dp)) { content(state.settings.collectAsState().value.preferences) }
                }
            }
        }
        try {
            scene.render(0)
            File(folder, name).writeBytes(scene.render(100_000_000L).encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }

    @Composable
    private fun themed(state: AppState, content: @Composable () -> Unit) {
        val preferences = state.settings.collectAsState().value.preferences
        MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
            // Still, so each picture is the arrangement rather than a moment of the change into it.
            CompositionLocalProvider(LocalMotion provides false, content = content)
        }
    }
}

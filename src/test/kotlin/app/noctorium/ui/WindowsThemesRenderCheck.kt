package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.PlaybackToolInstaller
import app.noctorium.playback.QueueState
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the Windows 98 and XP themes to pictures, off screen: the player bar with the Classic seek bar,
 * and the whole window. Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*WindowsThemesRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * The same tour through Night and Day goes to a folder of its own, and is compared picture by picture with
 * an earlier one when that is named too, to show a change to the skins left every other theme where it was:
 *
 *     -Dnoctorium.compareWith=some/earlier/bars
 */
class WindowsThemesRenderCheck {

    private val track = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "PzYrr7K1dvU",
        title = "Archangel",
        artists = listOf(Artist("burial", "Burial", ProviderType.YOUTUBE_MUSIC)),
        durationMs = 240_000,
        sourceUrl = "https://music.youtube.com/watch?v=PzYrr7K1dvU",
    )

    @Test
    fun `the Windows themes draw`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val queue = QueueState(tracks = listOf(track), currentIndex = 0)
        val playback = PlaybackState(status = PlaybackStatus.PAUSED, track = track, positionMs = 83_000, durationMs = 240_000)
        val state = desktopAppState()
        try {
            state.setProgressBarStyle(ProgressBarStyle.CLASSIC)
            state.setCornerStyle(CornerStyle.SHARP)
            listOf(ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_XP).forEach { theme ->
                state.setTheme(theme)
                listOf(PlayerBarStyle.CENTERED, PlayerBarStyle.INLINE).forEach { layout ->
                    state.setPlayerBarStyle(layout)
                    val scene = ImageComposeScene(1180, 150, Density(1f)) {
                        val preferences = state.settings.collectAsState().value.preferences
                        MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.BottomCenter) {
                                PlayerBar(queue, playback, state)
                            }
                        }
                    }
                    try {
                        scene.render(0)
                        val file = File(folder, "windows-${theme.displayName.lowercase()}-${layout.name.lowercase()}.png")
                        file.writeBytes(scene.render(500_000_000L).encodeToData()!!.bytes)
                        assertTrue(file.length() > 1_000)
                    } finally {
                        scene.close()
                    }
                }
                state.setPlayerBarStyle(PlayerBarStyle.INLINE)
                val window = ImageComposeScene(1280, 800, Density(1f)) { NoctoriumApp(state) }
                try {
                    var image = window.render(0)
                    repeat(10) { frame ->
                        Thread.sleep(100)
                        image = window.render((frame + 1) * 100_000_000L)
                    }
                    File(folder, "windows-${theme.displayName.lowercase()}-window.png").writeBytes(image.encodeToData()!!.bytes)
                } finally {
                    window.close()
                }
            }
        } finally {
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            state.setCornerStyle(CornerStyle.SOFT)
            state.setPlayerBarStyle(PlayerBarStyle.INLINE)
            state.close()
        }
    }

    @Test
    fun `the standard themes draw as they did`() {
        val root = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        val state = desktopAppState()
        try {
            prepare(state)
            listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY).forEach { theme ->
                val folder = File(root, "standard/${theme.name.lowercase()}")
                state.setTheme(theme)
                tour(WindowTour(state, folder), state)
                System.getProperty("noctorium.compareWith")?.let(::File)?.let { earlier ->
                    val before = File(earlier, "standard/${theme.name.lowercase()}")
                    if (before.isDirectory) WindowTour.compare(before, folder, File(folder, "comparison.txt"))
                }
            }
        } finally {
            restore(state)
            state.close()
        }
    }

    /** Settings that would otherwise make two drawings of the same screen differ: motion, devices, updates. */
    private fun prepare(state: AppState) {
        state.setAnimations(false)
        state.setConnectEnabled(false)
        state.setUpdateCheckOnLaunch(false)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.updateNowPlaying { NowPlayingPreferences() }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
    }

    private fun restore(state: AppState) {
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAnimations(true)
        state.setConnectEnabled(true)
        state.setUpdateCheckOnLaunch(true)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.updateNowPlaying { NowPlayingPreferences() }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
    }

    /**
     * The screens a theme is judged on, in one window: Home, the library and a playlist in it, a search,
     * the queue, now playing on each backdrop, Settings and some of its pages, a dialog, and every player bar.
     */
    private fun tour(tour: WindowTour, state: AppState) {
        try {
            tour.open()
            waitForTools()
            tour.fill()
            tour.go(Destination.HOME)
            tour.capture("home")
            tour.go(Destination.LIBRARY)
            tour.settle()
            tour.fill()
            tour.capture("library")
            tour.library { it.copy(openPlaylist = Fixtures.openPlaylist, openPlaylistLoading = false, openPlaylistError = null) }
            tour.capture("playlist")
            tour.library { it.copy(openPlaylist = null) }
            tour.go(Destination.SEARCH)
            tour.fill()
            tour.capture("search")
            tour.go(Destination.QUEUE)
            tour.capture("queue")
            tour.go(Destination.NOW_PLAYING)
            listOf(NowPlayingBackdrop.WASH, NowPlayingBackdrop.COVER, NowPlayingBackdrop.PLAIN).forEach { backdrop ->
                state.setNowPlayingBackdrop(backdrop)
                tour.capture("now-playing-${backdrop.name.lowercase()}", frames = 14)
            }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
            tour.go(Destination.SETTINGS)
            tour.capture("settings")
            listOf("CUSTOMIZATION", "SOUND", "PLAYBACK_QUEUE", "LYRICS", "STARTUP").forEach { page ->
                tour.settingsPage(page)
                tour.capture("settings-${page.lowercase()}")
            }
            tour.go(Destination.HOME)
            tour.settle()
            tour.press(androidx.compose.ui.input.key.Key.Slash, '?', shift = true)
            tour.capture("dialog-shortcuts")
            tour.press(androidx.compose.ui.input.key.Key.Escape, '\u001b')
            tour.settle()
            PlayerBarStyle.entries.forEach { style ->
                state.setPlayerBarStyle(style)
                tour.capture("bar-${style.name.lowercase()}", crop = Rect.makeXYWH(0f, 800f - 170f, 1280f, 170f))
            }
            state.setPlayerBarPosition(PlayerBarPosition.TOP)
            listOf(PlayerBarStyle.INLINE, PlayerBarStyle.TASKBAR).forEach { style ->
                state.setPlayerBarStyle(style)
                tour.capture("bar-${style.name.lowercase()}-top", crop = Rect.makeXYWH(0f, 0f, 1280f, 170f))
            }
            state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
            state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        } finally {
            tour.close()
        }
    }

    /** Waits out a first run's fetching of yt-dlp and mpv, whose banner would otherwise be in every picture. */
    private fun waitForTools() {
        val until = System.currentTimeMillis() + 180_000
        while (PlaybackToolInstaller.state.value.installing != null && System.currentTimeMillis() < until) Thread.sleep(500)
    }
}

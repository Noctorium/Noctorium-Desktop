package app.noctorium.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.playback.PlaybackToolInstaller
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.ThemePreset
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.test.Test

/**
 * Draws the Windows 98 and XP skins to pictures, off screen: the whole window, screen by screen, with made-up
 * music in it -- Home, the library, a playlist, a search, the queue, now playing on each backdrop, Settings and
 * its pages, dialogs and menus, and the player bar in every layout. Off unless a folder is named, like
 * [PlayerBarRenderCheck]; the pictures go to `windows/` under it:
 *
 *     ./gradlew :test --tests "*WindowsThemesRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * The same tour through Night and Day goes to `standard/`, and is compared picture by picture with an earlier
 * one when that is named too, to show a change to the skins left every other theme where it was:
 *
 *     -Dnoctorium.compareWith=some/earlier/bars
 *
 * Run it with `--no-configuration-cache`: a cached configuration keeps the folders the last run was given.
 */
class WindowsThemesRenderCheck {

    @Test
    fun `the Windows themes draw`() {
        val root = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        val state = desktopAppState()
        try {
            prepare(state)
            // The standard slider of the day was the trackbar, which is what the Material seek bar becomes.
            state.setProgressBarStyle(ProgressBarStyle.MATERIAL)
            listOf(ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_XP).forEach { theme ->
                state.setTheme(theme)
                val folder = File(root, "windows/${theme.name.lowercase()}")
                tour(WindowTour(state, folder), state)
                handled(WindowTour(state, folder), state, xp = theme == ThemePreset.WINDOWS_XP)
            }
        } finally {
            restore(state)
            state.close()
        }
    }

    /**
     * The skins in use rather than at rest: a card and a row under the pointer, the menu on a song, the volume
     * in the tray, a dialog with a box to type in, the Taskbar along the top and in a narrow window, and the old
     * seek bars on the skin's own bar. Where things are on the screen differs between the two skins, so the
     * places clicked are each skin's own.
     */
    private fun handled(tour: WindowTour, state: AppState, xp: Boolean) {
        try {
            tour.open()
            tour.fill()
            tour.go(Destination.HOME)
            tour.hover(if (xp) Offset(320f, 230f) else Offset(207f, 240f))
            tour.capture("home-pointed")
            tour.go(Destination.DOWNLOADS)
            tour.capture("downloads")
            tour.go(Destination.LINK)
            tour.capture("link")
            // The pages of Settings the tour leaves out, which are mostly each service's account.
            listOf(
                "ACCOUNT", "PROFILE", "YOUTUBE", "SOUNDCLOUD", "SPOTIFY", "BANDCAMP", "VK", "SCROBBLING", "DISCORD",
                "UPDATES", "PLAYBACK_TOOLS", "DIAGNOSTICS",
            ).forEach { page ->
                tour.settingsPage(page)
                tour.capture("settings-${page.lowercase()}")
            }
            tour.go(Destination.LIBRARY)
            tour.settle()
            tour.fill()
            tour.library { it.copy(openPlaylist = Fixtures.openPlaylist, openPlaylistLoading = false, openPlaylistError = null) }
            tour.hover(if (xp) Offset(600f, 222f) else Offset(600f, 231f))
            tour.capture("playlist-pointed")
            tour.click(if (xp) Offset(1138f, 151f) else Offset(1135f, 160f))
            tour.capture("menu-track")
            tour.press(Key.Escape, '\u001b')
            tour.settle()
            tour.click(Offset(1247f, 763f))
            tour.capture("menu-volume")
            tour.press(Key.Escape, '\u001b')
            tour.library { it.copy(openPlaylist = null) }
            tour.settle()
            tour.fill()
            tour.click(if (xp) Offset(1074f, 49f) else Offset(1069f, 56f))
            tour.capture("dialog-new-playlist")
            tour.press(Key.Escape, '\u001b')
            tour.go(Destination.HOME)
            tour.fill()
            listOf(ProgressBarStyle.CLASSIC, ProgressBarStyle.LUNA, ProgressBarStyle.MINIMAL).forEach { style ->
                state.setProgressBarStyle(style)
                tour.capture("bar-inline-${style.name.lowercase()}", crop = Rect.makeXYWH(0f, 800f - 170f, 1280f, 170f))
            }
            state.setProgressBarStyle(ProgressBarStyle.MATERIAL)
            state.setPlayerBarStyle(PlayerBarStyle.TASKBAR)
            tour.go(Destination.NOW_PLAYING)
            tour.capture("taskbar-now-playing")
            // The panel's Lyrics tab, which made-up songs have none behind.
            tour.click(if (xp) Offset(913f, 63f) else Offset(913f, 60f))
            tour.capture("now-playing-lyrics", frames = 14)
            state.setPlayerBarPosition(PlayerBarPosition.TOP)
            tour.go(Destination.HOME)
            tour.capture("taskbar-top")
            state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
            tour.open(width = 760, height = 560)
            tour.fill()
            tour.capture("taskbar-narrow")
            state.setPlayerBarStyle(PlayerBarStyle.INLINE)
            // The whole of Customization at once, in a window tall enough to hold it.
            tour.open(width = 1280, height = 3600)
            tour.fill()
            tour.settingsPage("CUSTOMIZATION")
            tour.capture("settings-customization-whole")
        } finally {
            tour.close()
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
                folder.listFiles { file -> file.name.startsWith("diff-") }?.forEach(File::delete)
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
            tour.fill()
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

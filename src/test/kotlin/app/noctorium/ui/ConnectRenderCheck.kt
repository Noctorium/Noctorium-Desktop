package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.noctorium.connect.ConnectState
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.SurfaceStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.test.Test

/**
 * Noctorium Connect drawn off screen, the whole window at a time, with made-up devices on a made-up network: the
 * Connect page with nothing found, with three devices, driving one of them, being driven, and with the music on a
 * device playing by itself; the strip along every player bar layout; Now playing and the queue showing a song that
 * plays on another device; and all of it under 98, Noctorium 98 and XP. Off unless a folder is named:
 *
 *     ./gradlew :test --tests "*ConnectRenderCheck*" -Dnoctorium.renderConnect=build/connect
 *
 * Nothing is sent anywhere: the devices are put straight into Connect's state (see [fakeConnect]), and the songs are
 * WindowTour's invented ones with their painted covers.
 *
 * Each window has an AppState of its own. The window closes its AppState when it goes, as the real one does when
 * Noctorium quits, and what the player shows of another device is worked out on that AppState's own time -- so a
 * second window on the same AppState would draw what the first one last saw.
 */
class ConnectRenderCheck {

    @Test
    fun `the Connect page and the player as the remote draw`() {
        val root = System.getProperty("noctorium.renderConnect")?.let(::File) ?: return
        val standard = File(root, "standard")
        window(standard) { tour, state -> pages(tour, state) }
        window(standard) { tour, state -> bars(tour, state) }
        window(standard, theme = ThemePreset.NOCTORIUM_DAY) { tour, state ->
            driving(tour, state)
            tour.capture("tab-driving-day", frames = 12)
            tour.go(Destination.NOW_PLAYING)
            tour.capture("now-playing-driving-day", frames = 14)
        }
        window(standard, width = 760, height = 560) { tour, state ->
            tour.go(Destination.CONNECT)
            driving(tour, state)
            tour.capture("tab-driving-narrow", frames = 12)
            show(tour, state, nothingFound)
            tour.capture("tab-nothing-found-narrow")
            show(tour, state, ConnectSamples.three())
            tour.capture("tab-three-devices-narrow")
        }
        window(standard) { tour, state ->
            show(tour, state, ConnectSamples.three())
            openSettingsSearch(state, "devices")
            tour.capture("settings-search-devices")
            openSettingsSearch(state, "firewall")
            tour.capture("settings-search-firewall")
        }
        listOf(ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_98_NOCTORIUM, ThemePreset.WINDOWS_XP).forEach { theme ->
            window(File(root, "windows/${theme.name.lowercase()}"), theme = theme) { tour, state -> skinned(tour, state) }
        }
        // Connect's own settings, which are on the account's page and so only drawn there once signed in: drawn here
        // on their own, with three devices found, for the way to the page they now carry.
        val state = desktopAppState()
        val tour = WindowTour(state, standard)
        try {
            waitForConnect(state)
            fakeConnect(state, ConnectSamples.three())
            tour.open(width = 760, height = 330) {
                val preferences = state.settings.collectAsState().value.preferences
                MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.padding(20.dp)) { SettingsPanelCard { ConnectSettingsRows(state) } }
                    }
                }
            }
            fakeConnect(state, ConnectSamples.three())
            tour.capture("settings-connect-rows")
        } finally {
            tour.close()
            fakeConnect(state, ConnectState())
            state.close()
        }
    }

    /** Connect starting up, which with no account here says so once and does nothing more: waited out. */
    private fun waitForConnect(state: AppState) {
        val until = System.currentTimeMillis() + 10_000
        while (state.connect.value.thisDeviceId.isBlank() && System.currentTimeMillis() < until) Thread.sleep(50)
    }

    /** The page in each of its states, Now playing and the queue while driving, and being driven. */
    private fun pages(tour: WindowTour, state: AppState) {
        tour.go(Destination.CONNECT)
        // Signed out: the page says what Connect needs, and the Devices button is not on the bar.
        show(tour, state, ConnectState())
        tour.capture("tab-signed-out")
        show(tour, state, nothingFound)
        tour.capture("tab-nothing-found")
        show(tour, state, ConnectSamples.three())
        tour.capture("tab-three-devices")
        // Switched off at the top of the page, and on again.
        state.setConnectEnabled(false)
        tour.capture("tab-off")
        state.setConnectEnabled(true)
        show(tour, state, ConnectSamples.three().copy(message = "Laptop did not answer. Its firewall may be keeping Noctorium out on this network."))
        tour.capture("tab-refused")
        driving(tour, state)
        tour.capture("tab-driving", frames = 12)
        show(tour, state, ConnectSamples.driving(System.currentTimeMillis()).copy(busy = true), local = PlaybackState())
        tour.capture("tab-busy")
        driving(tour, state)
        tour.go(Destination.NOW_PLAYING)
        tour.capture("now-playing-driving", frames = 14)
        state.updateNowPlaying { copy(layout = NowPlayingLayout.PLAYLIST) }
        tour.capture("now-playing-driving-playlist", frames = 14)
        state.updateNowPlaying { copy(layout = NowPlayingLayout.IMMERSIVE) }
        tour.capture("now-playing-driving-immersive", frames = 14)
        state.updateNowPlaying { NowPlayingPreferences() }
        tour.go(Destination.QUEUE)
        tour.capture("queue-driving")
        // Being driven: this computer plays, Laptop is its remote control.
        tour.go(Destination.CONNECT)
        show(tour, state, ConnectSamples.driven(), local = playingHere)
        tour.capture("tab-driven")
        // The music on Laptop, by itself; this computer idle.
        show(tour, state, ConnectSamples.elsewhere(), local = PlaybackState())
        tour.capture("tab-elsewhere")
        state.setSurfaceStyle(SurfaceStyle.GLASS)
        driving(tour, state)
        tour.capture("tab-driving-glass", frames = 12)
        state.setSurfaceStyle(SurfaceStyle.SOLID)
    }

    /** The strip along every layout, at the foot of the window, at the top, under glass, and while being driven. */
    private fun bars(tour: WindowTour, state: AppState) {
        tour.go(Destination.HOME)
        driving(tour, state)
        PlayerBarStyle.entries.forEach { style ->
            state.setPlayerBarStyle(style)
            madeUpHome(tour)
            tour.capture("bar-${style.name.lowercase()}-driving", crop = foot)
        }
        state.setPlayerBarPosition(PlayerBarPosition.TOP)
        listOf(PlayerBarStyle.INLINE, PlayerBarStyle.ISLAND, PlayerBarStyle.TASKBAR).forEach { style ->
            state.setPlayerBarStyle(style)
            madeUpHome(tour)
            tour.capture("bar-${style.name.lowercase()}-driving-top", crop = top)
        }
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.setSurfaceStyle(SurfaceStyle.GLASS)
        listOf(PlayerBarStyle.INLINE, PlayerBarStyle.FLOATING, PlayerBarStyle.ISLAND, PlayerBarStyle.SPOTLIGHT).forEach { style ->
            state.setPlayerBarStyle(style)
            madeUpHome(tour)
            tour.capture("bar-${style.name.lowercase()}-driving-glass", crop = foot)
        }
        state.setSurfaceStyle(SurfaceStyle.SOLID)
        show(tour, state, ConnectSamples.driven(), local = playingHere)
        listOf(PlayerBarStyle.INLINE, PlayerBarStyle.STACKED, PlayerBarStyle.FLOATING, PlayerBarStyle.ISLAND, PlayerBarStyle.TASKBAR).forEach { style ->
            state.setPlayerBarStyle(style)
            madeUpHome(tour)
            tour.capture("bar-${style.name.lowercase()}-driven", crop = foot)
        }
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
    }

    private fun skinned(tour: WindowTour, state: AppState) {
        tour.go(Destination.CONNECT)
        show(tour, state, nothingFound)
        tour.capture("tab-nothing-found")
        show(tour, state, ConnectSamples.three())
        tour.capture("tab-three-devices")
        // Switched off at the top of the page, and on again.
        state.setConnectEnabled(false)
        tour.capture("tab-off")
        state.setConnectEnabled(true)
        driving(tour, state)
        tour.capture("tab-driving", frames = 12)
        tour.go(Destination.NOW_PLAYING)
        tour.capture("now-playing-driving", frames = 14)
        state.updateNowPlaying { copy(layout = NowPlayingLayout.PLAYLIST) }
        tour.capture("now-playing-driving-playlist", frames = 14)
        state.updateNowPlaying { NowPlayingPreferences() }
        tour.go(Destination.HOME)
        listOf(PlayerBarStyle.INLINE, PlayerBarStyle.DISPLAY, PlayerBarStyle.FLOATING, PlayerBarStyle.ISLAND, PlayerBarStyle.TASKBAR, PlayerBarStyle.TOOLBAR).forEach { style ->
            state.setPlayerBarStyle(style)
            madeUpHome(tour)
            tour.capture("bar-${style.name.lowercase()}-driving", crop = foot)
        }
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        show(tour, state, ConnectSamples.driven(), local = playingHere)
        madeUpHome(tour)
        tour.capture("bar-inline-driven", crop = foot)
        tour.go(Destination.CONNECT)
        tour.capture("tab-driven")
    }

    /** Home with the made-up music in it again: what Home fetches for itself can arrive after the pictures began. */
    private fun madeUpHome(tour: WindowTour) = tour.ui {
        it.copy(recentTracks = Fixtures.recent, pinnedTracks = Fixtures.pinned, homeSections = Fixtures.home, homeLoading = false, errorMessage = null)
    }

    private val nothingFound = ConnectState(available = true, listening = true, thisDevice = "Desk", thisDeviceId = "desk-id")

    /** This computer as Pocket's remote control, its own player idle, as it is once the music has gone there. */
    private fun driving(tour: WindowTour, state: AppState) =
        show(tour, state, ConnectSamples.driving(System.currentTimeMillis()), local = PlaybackState())

    /** This computer playing Harbour Lights. */
    private val playingHere = PlaybackState(status = PlaybackStatus.PLAYING, track = Fixtures.queue[2], positionMs = 83_000, durationMs = 245_000)

    /**
     * Puts [connect] in place, with [local] as this computer's own player -- paused on Harbour Lights unless said --
     * and again after a moment, in case Connect starting up had its own say in between.
     */
    private fun show(tour: WindowTour, state: AppState, connect: ConnectState, local: PlaybackState? = null) {
        // Let go of first: music starting here while another device is driven would be taken for choosing here.
        fakeConnect(state, ConnectState())
        tour.settle(1)
        tour.playback(local ?: PlaybackState(status = PlaybackStatus.PAUSED, track = Fixtures.queue[2], positionMs = 83_000, durationMs = 245_000))
        tour.settle(1)
        fakeConnect(state, connect)
        tour.settle(4)
        fakeConnect(state, connect)
    }

    private val foot = Rect.makeXYWH(0f, 800f - 200f, 1280f, 200f)
    private val top = Rect.makeXYWH(0f, 0f, 1280f, 200f)

    /** A window of its own, on an AppState of its own, in [theme]; everything changed for it put back afterwards. */
    private fun window(
        folder: File,
        width: Int = 1280,
        height: Int = 800,
        theme: ThemePreset = ThemePreset.NOCTORIUM_NIGHT,
        draw: (WindowTour, AppState) -> Unit,
    ) {
        val state = desktopAppState()
        val tour = WindowTour(state, folder)
        try {
            prepare(state, theme)
            tour.open(width, height)
            tour.fill()
            draw(tour, state)
        } finally {
            // Put back while the AppState is still running to save it: the window going closes it.
            fakeConnect(state, ConnectState())
            restore(state)
            tour.close()
            state.close()
        }
    }

    private fun prepare(state: AppState, theme: ThemePreset) {
        // Switched on, as it is out of the box; with no account here it starts, says so once and does nothing more.
        // That is waited out, so it cannot undo what is put in.
        if (!state.settings.value.preferences.connect.enabled) state.setConnectEnabled(true)
        waitForConnect(state)
        state.setAnimations(false)
        state.setUpdateCheckOnLaunch(false)
        state.setTheme(theme)
        state.setProgressBarStyle(if (theme.name.startsWith("WINDOWS")) ProgressBarStyle.MATERIAL else ProgressBarStyle.MINIMAL)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.setSurfaceStyle(SurfaceStyle.SOLID)
        state.updateNowPlaying { NowPlayingPreferences() }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
    }

    private fun restore(state: AppState) {
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAnimations(true)
        state.setUpdateCheckOnLaunch(true)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.setSurfaceStyle(SurfaceStyle.SOLID)
        state.updateNowPlaying { NowPlayingPreferences() }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
        Thread.sleep(400)
    }
}

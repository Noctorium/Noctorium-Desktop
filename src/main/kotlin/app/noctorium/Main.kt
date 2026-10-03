package app.noctorium

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.noctorium.platform.LaunchAtLogin
import app.noctorium.platform.MacBundle
import app.noctorium.platform.SelfTest
import app.noctorium.platform.SingleInstance
import app.noctorium.playback.PlaybackStatus
import app.noctorium.settings.AppDirectories
import app.noctorium.ui.AppIcon
import app.noctorium.ui.DesktopTray
import app.noctorium.ui.NoctoriumApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Frame
import java.awt.GraphicsEnvironment

fun main(args: Array<String>) {
    // Answers its questions and leaves, before anything else -- including the claim below, so it can run
    // beside a copy that is already open.
    if (SelfTest.ARGUMENT in args) SelfTest.run()
    MacBundle.tidy()

    // Started with the computer into the tray: no window until somebody asks for one.
    val intoTray = LaunchAtLogin.TRAY_ARGUMENT in args
    // Counts the times a later start asked this one to show itself; the window follows the count.
    val wakes = MutableStateFlow(0)

    // Claimed before anything is built, so a second start costs a lock check and not a second player.
    // Where the claim cannot be made at all -- no folder to make it in -- Noctorium simply starts, as it
    // always did; only a claim that is somebody else's sends this one away.
    val claim = AppDirectories.base()?.let { folder ->
        runCatching { SingleInstance.claim(folder, wake = !intoTray) { wakes.update { it + 1 } } }
    }
    if (claim != null && claim.isSuccess && claim.getOrNull() == null) return
    val instance = claim?.getOrNull()

    application {
        // Held here rather than inside the interface so it can be shut down before the process ends. Leaving
        // that to the composition being disposed is a race the player can lose, and losing it means mpv is
        // still playing after the window has gone.
        val appState = remember { desktopAppState() }
        val settings by appState.settings.collectAsState()
        val playback by appState.playback.collectAsState()
        val desktop = settings.preferences.desktop

        val trayAvailable = remember { DesktopTray.supported && !GraphicsEnvironment.isHeadless() }
        // Where there is no tray to wait in, a start into it opens the window minimised instead, so it is
        // still there to find on the taskbar.
        var visible by remember { mutableStateOf(!(intoTray && trayAvailable)) }
        var raises by remember { mutableIntStateOf(0) }
        val windowState = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            width = 1280.dp,
            height = 800.dp,
        )
        LaunchedEffect(Unit) { if (intoTray && !trayAvailable) windowState.isMinimized = true }

        val quit: () -> Unit = {
            appState.close()
            instance?.close()
            exitApplication()
        }
        val open: () -> Unit = {
            visible = true
            windowState.isMinimized = false
            raises++
        }

        val tray = remember {
            if (trayAvailable) {
                DesktopTray(
                    open = open,
                    playPause = appState::togglePlayback,
                    next = appState::next,
                    previous = appState::previous,
                    quit = quit,
                )
            } else {
                null
            }
        }
        // In the tray whenever closing the window leads there, and whenever the window is waiting in it.
        val inTray = tray != null && (desktop.closeToTray || !visible)
        DisposableEffect(tray, inTray) {
            if (inTray) tray?.install()
            onDispose { tray?.close() }
        }
        LaunchedEffect(tray, playback.track, playback.status) {
            tray?.show(playback.track?.title, playback.track?.artistLine, playback.status == PlaybackStatus.PLAYING)
        }

        val wake by wakes.collectAsState()
        LaunchedEffect(wake) { if (wake > 0) open() }

        // On a Mac, Quit in the menu or Command-Q goes through the system rather than through the window,
        // so the player is shut down here before the process ends; and clicking the Dock icon of a copy
        // whose window was closed into the menu bar brings the window back, as a Mac application does.
        DisposableEffect(Unit) {
            if (MacBundle.isMac && Desktop.isDesktopSupported()) {
                val system = Desktop.getDesktop()
                if (system.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                    system.setQuitHandler { _, response ->
                        runCatching { appState.close() }
                        runCatching { instance?.close() }
                        response.performQuit()
                    }
                }
                if (system.isSupported(Desktop.Action.APP_EVENT_REOPENED)) {
                    system.addAppEventListener(java.awt.desktop.AppReopenedListener { open() })
                }
            }
            onDispose { }
        }

        Window(
            onCloseRequest = {
                if (tray != null && desktop.closeToTray) {
                    visible = false
                    // Said once, the first time: closing a music player and hearing it carry on is only
                    // unsettling until somebody has been told where it went.
                    if (!desktop.trayHintShown) {
                        tray.install()
                        tray.note(
                            "Noctorium is still playing",
                            "It is in the tray now. Click the icon to open it again, or right-click it to quit.",
                        )
                        appState.updateDesktop { copy(trayHintShown = true) }
                    }
                } else {
                    quit()
                }
            },
            visible = visible,
            title = "Noctorium",
            // Alt-Tab and the window itself take one image; the sizes below are for everywhere that wants a
            // smaller one and would otherwise scale this one down.
            icon = BitmapPainter(AppIcon.image(256).toComposeImageBitmap()),
            state = windowState,
        ) {
            window.minimumSize = Dimension(760, 560)
            // Each size is drawn at its own size, so the taskbar's small icon is rendered rather than shrunk.
            window.iconImages = AppIcon.images()
            LaunchedEffect(raises) { if (raises > 0) raise(window) }
            NoctoriumApp(appState, window)
        }
    }
}

/**
 * Brings the window in front of whatever is covering it.
 *
 * Windows does not let a program that is not in front take the front by asking, and flashes its taskbar
 * button instead -- right for a program interrupting somebody, wrong for one they have just clicked on.
 * Being briefly always-on-top is the accepted way round it: the window comes up over the others, and then
 * goes back to being an ordinary window.
 */
private fun raise(window: java.awt.Window) {
    (window as? Frame)?.let { frame ->
        if (frame.extendedState and Frame.ICONIFIED != 0) frame.extendedState = frame.extendedState and Frame.ICONIFIED.inv()
    }
    window.isAlwaysOnTop = true
    window.toFront()
    window.requestFocus()
    window.isAlwaysOnTop = false
}

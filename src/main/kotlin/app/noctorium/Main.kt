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
import app.noctorium.auth.CefRuntime
import app.noctorium.platform.LaunchAtLogin
import app.noctorium.platform.MacBundle
import app.noctorium.platform.SelfTest
import app.noctorium.platform.SingleInstance
import app.noctorium.playback.PlaybackStatus
import app.noctorium.settings.AppDirectories
import app.noctorium.ui.AppIcon
import app.noctorium.ui.DesktopTray
import app.noctorium.ui.NoctoriumApp
import app.noctorium.ui.TrackAnnouncer
import app.noctorium.ui.DesktopHooks
import app.noctorium.ui.NoctoriumLook
import app.noctorium.ui.QuickExit
import app.noctorium.ui.QuickSearchWindowContent
import app.noctorium.ui.Shortcut
import app.noctorium.ui.ShortcutAction
import app.noctorium.ui.globalBindings
import app.noctorium.ui.perform
import app.noctorium.hotkeys.GlobalHotKeys
import app.noctorium.hotkeys.SystemHotKeys
import app.noctorium.platform.Foreground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.KeyboardFocusManager
import java.util.concurrent.atomic.AtomicReference

fun main(args: Array<String>) {
    // Answers its questions and leaves, before anything else -- including the claim below, so it can run
    // beside a copy that is already open.
    if (SelfTest.ARGUMENT in args) SelfTest.run()
    MacBundle.tidy()
    if (SelfTest.START_CHROMIUM in args) SelfTest.startChromium()

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

        // The global shortcuts, registered with the system while they are switched on and given back when they are
        // switched off or Noctorium quits. A press arrives on a thread of their own and is handed to this one.
        val globalPresses = remember { AtomicReference<(String) -> Unit>({}) }
        val globalHotKeys = remember {
            GlobalHotKeys(
                SystemHotKeys.support(),
                onPressed = { action -> javax.swing.SwingUtilities.invokeLater { globalPresses.get()(action) } },
                title = { action -> ShortcutAction.byId(action)?.title ?: action },
            )
        }
        LaunchedEffect(desktop.shortcuts) {
            globalHotKeys.apply(desktop.shortcuts.globalEnabled, globalBindings(desktop.shortcuts).mapKeys { it.key.id })
        }
        DisposableEffect(Unit) { onDispose { globalHotKeys.close() } }

        var quitting by remember { mutableStateOf(false) }
        val quit: () -> Unit = {
            if (!quitting) {
                quitting = true
                visible = false
                globalHotKeys.close()
                appState.close()
                instance?.close()
                // Chromium, once a sign-in has started it, is shut down and waited for before the process
                // ends -- ending it while Chromium is still closing can crash it on the way out. Waited for on
                // a thread of its own, because Chromium finishes on this one.
                if (CefRuntime.running) {
                    Thread({
                        CefRuntime.shutdown(timeoutMillis = 5_000)
                        javax.swing.SwingUtilities.invokeLater { exitApplication() }
                    }, "noctorium-quit").start()
                } else {
                    exitApplication()
                }
            }
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

        // The quick search bar from a global shortcut: over Noctorium's own window when that is in front, and
        // otherwise in a small window of its own, which gives the front back to whatever had it when it closes.
        var quickSearchAsked by remember { mutableIntStateOf(0) }
        var quickWindow by remember { mutableStateOf(false) }
        val beforeQuickSearch = remember { AtomicReference<Any?>(null) }
        val frame = remember { AtomicReference<java.awt.Window?>(null) }
        val inFront: () -> Boolean = { visible && !windowState.isMinimized && isInFront(frame.get()) }
        // Out of the way: into the tray where there is one, which is where closing it leads, or down to the taskbar.
        val toggleWindow: () -> Unit = {
            if (inFront()) {
                if (tray != null) visible = false else windowState.isMinimized = true
            } else {
                open()
            }
        }
        val closeQuickWindow: (QuickExit) -> Unit = { exit ->
            quickWindow = false
            if (exit == QuickExit.SEARCH_PAGE) open() else Foreground.restore(beforeQuickSearch.getAndSet(null), visible && !windowState.isMinimized)
        }
        globalPresses.set { id ->
            val action = ShortcutAction.byId(id)
            when (action?.shortcut) {
                null -> Unit
                Shortcut.ToggleWindow -> toggleWindow()
                Shortcut.QuickSearch -> when {
                    inFront() -> quickSearchAsked++
                    quickWindow -> Unit
                    else -> {
                        beforeQuickSearch.set(Foreground.capture())
                        quickWindow = true
                    }
                }
                else -> appState.perform(action.shortcut)
            }
        }

        // In the tray whenever closing the window leads there, and whenever the window is waiting in it; and
        // while tracks are announced, because the note comes from the icon and there is no note without one.
        val inTray = tray != null && (desktop.closeToTray || !visible || desktop.announceTracks)
        DisposableEffect(tray, inTray) {
            if (inTray) tray?.install()
            onDispose { tray?.close() }
        }
        LaunchedEffect(tray, playback.track, playback.status) {
            tray?.show(playback.track?.title, playback.track?.artistLine, playback.status == PlaybackStatus.PLAYING)
        }

        // A note from the tray when a new song starts, if the listener asked for one. Told about every change
        // to what is playing; the announcer decides which of them is a start worth announcing, and the window
        // is asked whether it is in front at that moment rather than watched the rest of the time.
        val announcer = remember { TrackAnnouncer() }
        LaunchedEffect(tray, playback.track?.queueKey, playback.status == PlaybackStatus.PLAYING) {
            val track = playback.track ?: return@LaunchedEffect
            val inFront = visible && !windowState.isMinimized && isInFront(frame.get())
            if (announcer.shouldAnnounce(track.queueKey, playback.status == PlaybackStatus.PLAYING, desktop.announceTracks, inFront)) {
                tray?.announce(track.title, track.artistLine)
            }
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
                        runCatching { globalHotKeys.close() }
                        runCatching { appState.close() }
                        runCatching { instance?.close() }
                        // macOS waits for the answer, so Chromium can be shut down first, as above.
                        if (CefRuntime.running) {
                            Thread({
                                CefRuntime.shutdown(timeoutMillis = 5_000)
                                response.performQuit()
                            }, "noctorium-quit").start()
                        } else {
                            response.performQuit()
                        }
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
            LaunchedEffect(window) { frame.set(window) }
            // Each size is drawn at its own size, so the taskbar's small icon is rendered rather than shrunk.
            window.iconImages = AppIcon.images()
            LaunchedEffect(raises) { if (raises > 0) raise(window) }
            NoctoriumApp(appState, window, DesktopHooks(globalHotKeys.state, quickSearchAsked, toggleWindow))
        }

        // The quick search's own window, for its global shortcut from another program: small, always on top,
        // with no frame of its own -- the bar is the window -- a fifth of the way down the screen.
        if (quickWindow) {
            val screen = remember { java.awt.Toolkit.getDefaultToolkit().screenSize }
            val quickState = rememberWindowState(
                position = WindowPosition(((screen.width - QUICK_WINDOW_WIDTH) / 2).dp, (screen.height / 5).dp),
                width = QUICK_WINDOW_WIDTH.dp,
                height = QUICK_WINDOW_HEIGHT.dp,
            )
            Window(
                onCloseRequest = { closeQuickWindow(QuickExit.CLOSED) },
                state = quickState,
                title = "Noctorium quick search",
                icon = BitmapPainter(AppIcon.image(64).toComposeImageBitmap()),
                undecorated = true,
                transparent = true,
                resizable = false,
                alwaysOnTop = true,
            ) {
                // In front and with the keyboard, and left on top: raise() would take the always-on-top away again.
                LaunchedEffect(window) {
                    window.toFront()
                    window.requestFocus()
                }
                // Clicking anywhere else puts it away, as a launcher's bar does.
                DisposableEffect(window) {
                    val leaving = object : java.awt.event.WindowAdapter() {
                        override fun windowDeactivated(event: java.awt.event.WindowEvent?) {
                            if (quickWindow) closeQuickWindow(QuickExit.CLOSED)
                        }
                    }
                    window.addWindowListener(leaving)
                    onDispose { window.removeWindowListener(leaving) }
                }
                NoctoriumLook(appState) { QuickSearchWindowContent(appState, closeQuickWindow) }
            }
        }
    }
}

/** The quick search window's size: the bar, its seven results and its footer, with room for the shadow. */
private const val QUICK_WINDOW_WIDTH = 700
private const val QUICK_WINDOW_HEIGHT = 540

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

/**
 * Whether Noctorium is the window being looked at: its own window is the active one, or a window of its own
 * is -- a sign-in window opened from it still counts as looking at Noctorium.
 */
private fun isInFront(window: java.awt.Window?): Boolean {
    if (window == null) return false
    val active = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow ?: return false
    return generateSequence(active) { it.owner }.any { it === window }
}

package app.noctorium.ui

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The play, pause and track keys on a keyboard, while Noctorium is not the window in front.
 *
 * The shortcuts in [Shortcut] only reach an application that has focus, which is right for a letter and
 * useless for these: the point of the button with the triangle on it is that it is pressed while doing
 * something else entirely.
 *
 * Windows hands these out with `RegisterHotKey`, which is the polite way to ask. The alternative, a
 * low-level keyboard hook, sees every key the machine types, has to return fast enough not to stutter the
 * whole system, and is the shape of thing a virus scanner asks questions about. A hotkey is one call, no
 * hook, no privileges, and the operating system does the filtering.
 *
 * It has one cost and it is an honest one: a media key belongs to whoever registered it first. If another
 * player is already running it has them, and Noctorium does without rather than fighting over them.
 * [MediaKeyGrab.registered] says which were actually granted, so nothing has to pretend.
 *
 * Windows only. The same job is a portal request on Wayland, a grab under X11 and an accessibility
 * permission on macOS, none of which resemble each other or this; elsewhere [start] returns null.
 */
object MediaKeys {
    /** Windows' own numbers for the keys along the top of a keyboard. */
    const val VK_MEDIA_NEXT_TRACK = 0xB0
    const val VK_MEDIA_PREV_TRACK = 0xB1
    const val VK_MEDIA_STOP = 0xB2
    const val VK_MEDIA_PLAY_PAUSE = 0xB3

    /** What each key means, kept apart from the registering so it can be checked on its own. */
    fun shortcutForVirtualKey(vk: Int): Shortcut? = when (vk) {
        VK_MEDIA_PLAY_PAUSE -> Shortcut.PlayPause
        VK_MEDIA_NEXT_TRACK -> Shortcut.Next
        VK_MEDIA_PREV_TRACK -> Shortcut.Previous
        // Stop is not pause, and nothing here stops. The key is left to whoever else wants it.
        else -> null
    }

    private val windows: Boolean
        get() = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    /**
     * Starts listening, or returns null where this cannot be done at all.
     *
     * [onPress] arrives on a thread of this object's own rather than the interface's, so what it does has
     * to be safe from there. Handing straight to `AppState` is.
     */
    fun start(onPress: (Shortcut) -> Unit): MediaKeyGrab? {
        if (!windows) return null
        return runCatching { WindowsMediaKeys(onPress) }.getOrNull()
    }
}

/** What was granted, and how to give it back. */
interface MediaKeyGrab : AutoCloseable {
    /** The keys this machine let Noctorium have. Empty when another application already holds them. */
    val registered: Set<String>
}

/**
 * A thread that owns the hotkeys and pumps the messages they arrive as.
 *
 * It has to be its own thread, and the registering has to happen on that thread: Windows delivers
 * `WM_HOTKEY` to whichever thread asked, and only one running a message loop ever sees it. Borrowing the
 * interface's thread would mean a second loop inside Compose's, which is a good way to stop a window
 * drawing.
 */
private class WindowsMediaKeys(private val onPress: (Shortcut) -> Unit) : MediaKeyGrab {
    private val granted = linkedSetOf<String>()
    @Volatile private var threadId: Int = 0
    private val ready = CountDownLatch(1)

    override val registered: Set<String> get() = granted.toSet()

    private val worker = Thread({ run() }, "noctorium-media-keys").apply {
        isDaemon = true
        start()
    }

    init {
        // Long enough to know what was granted, short enough that a wedged call cannot hold up start-up.
        ready.await(2, TimeUnit.SECONDS)
    }

    private fun run() {
        val user32 = User32.INSTANCE
        threadId = Kernel32.INSTANCE.GetCurrentThreadId()
        val keys = listOf(
            Triple(ID_PLAY, MediaKeys.VK_MEDIA_PLAY_PAUSE, "Play/Pause"),
            Triple(ID_NEXT, MediaKeys.VK_MEDIA_NEXT_TRACK, "Next"),
            Triple(ID_PREVIOUS, MediaKeys.VK_MEDIA_PREV_TRACK, "Previous"),
        )
        keys.forEach { (id, vk, name) ->
            // False means somebody else has it. Not an error worth surfacing: it is how the operating
            // system says another player got there first.
            val ok = runCatching { user32.RegisterHotKey(null, id, MOD_NOREPEAT, vk) }.getOrDefault(false)
            if (ok) granted += name
        }
        ready.countDown()
        if (granted.isEmpty()) return

        val message = WinUser.MSG()
        // Zero ends the loop, which is what the WM_QUIT below produces. Negative is an error.
        while (user32.GetMessage(message, null, 0, 0) > 0) {
            if (message.message == WM_HOTKEY) {
                // The virtual key is the high half of lParam; the modifiers are the low half.
                val lParam = Pointer.nativeValue(message.lParam.toPointer())
                val vk = ((lParam shr 16) and 0xFFFF).toInt()
                MediaKeys.shortcutForVirtualKey(vk)?.let { runCatching { onPress(it) } }
            }
        }
        keys.forEach { (id, _, _) -> runCatching { user32.UnregisterHotKey(null, id) } }
    }

    override fun close() {
        val id = threadId
        if (id == 0) return
        // Ends the loop above, which unregisters on its way out. The thread is a daemon, so a machine
        // that somehow ignores this still does not keep the process alive.
        runCatching {
            User32.INSTANCE.PostThreadMessage(id, WinUser.WM_QUIT, WinDef.WPARAM(0), WinDef.LPARAM(0))
        }
        runCatching { worker.join(1_000) }
    }

    private companion object {
        const val WM_HOTKEY = 0x0312

        /** No modifier, and no repeat while held: a held play button is one press, not forty. */
        const val MOD_NOREPEAT = 0x4000

        // Only have to be distinct within this thread.
        const val ID_PLAY = 1
        const val ID_NEXT = 2
        const val ID_PREVIOUS = 3
    }
}

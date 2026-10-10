package app.noctorium.hotkeys

import app.noctorium.settings.KeyChord
import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Global shortcuts on Windows, with `RegisterHotKey`: the same polite request [app.noctorium.ui.MediaKeys] makes
 * for the media keys, for chords the listener chose.
 *
 * A hot key belongs to the thread that registered it, and arrives as `WM_HOTKEY` in that thread's message queue,
 * so one thread of this object's own does all of it: registers, unregisters, and runs the loop the presses
 * arrive in. Anything asked of it from elsewhere is handed over as a task and the thread woken to run it.
 */
internal class WindowsHotKeys(private val onPressed: (Int) -> Unit) : HotKeyBackend {
    private val tasks = ConcurrentLinkedQueue<() -> Unit>()
    private val ready = CountDownLatch(1)
    @Volatile private var threadId = 0

    // Only touched on the worker thread.
    private val held = HashMap<Int, KeyChord>()

    private val worker = Thread({ run() }, "noctorium-global-shortcuts").apply {
        isDaemon = true
        start()
    }

    init {
        check(ready.await(2, TimeUnit.SECONDS) && threadId != 0) { "The shortcuts thread did not start" }
    }

    private fun run() {
        val user32 = User32.INSTANCE
        threadId = Kernel32.INSTANCE.GetCurrentThreadId()
        val message = WinUser.MSG()
        // A thread has no message queue until it asks for one, and a message posted to it before then is lost.
        // Peeking makes the queue, so the tasks posted from now on are kept until the loop below reads them.
        user32.PeekMessage(message, null, 0, 0, PM_NOREMOVE)
        ready.countDown()
        // Zero ends the loop, which is what the WM_QUIT from close() produces. Negative is an error.
        while (user32.GetMessage(message, null, 0, 0) > 0) {
            when (message.message) {
                WM_HOTKEY -> {
                    val number = message.wParam.toInt()
                    runCatching { onPressed(number) }
                }
                WM_RUN_TASKS -> while (true) {
                    val task = tasks.poll() ?: break
                    runCatching { task() }
                }
            }
        }
        held.keys.forEach { runCatching { user32.UnregisterHotKey(null, it) } }
        held.clear()
    }

    /** Runs [task] on the worker, where hot keys have to be registered, and waits for its answer. */
    private fun <T> onWorker(task: () -> T): T {
        val answer = CompletableFuture<T>()
        tasks.add { runCatching(task).fold(answer::complete, answer::completeExceptionally) }
        User32.INSTANCE.PostThreadMessage(threadId, WM_RUN_TASKS, WinDef.WPARAM(0), WinDef.LPARAM(0))
        return answer.get(5, TimeUnit.SECONDS)
    }

    override fun bind(wanted: Map<Int, KeyChord>, names: Map<Int, String>): Map<Int, HotKeyResult> = onWorker {
        val user32 = User32.INSTANCE
        // Let go of what is no longer wanted, or wanted on another chord, first: a chord moving from one action
        // to another would otherwise be refused as already taken -- by this very thread.
        held.entries.filter { (number, chord) -> wanted[number] != chord }.toList().forEach { (number, _) ->
            user32.UnregisterHotKey(null, number)
            held.remove(number)
        }
        wanted.mapValues { (number, chord) ->
            if (held[number] == chord) return@mapValues HotKeyResult.Registered
            val key = WindowsKeys.virtualKey(chord.key)
                ?: return@mapValues HotKeyResult.Unsupported("Windows has no hot key for ${chord.key}")
            if (user32.RegisterHotKey(null, number, WindowsKeys.modifiers(chord) or MOD_NOREPEAT, key)) {
                held[number] = chord
                HotKeyResult.Registered
            } else {
                when (val error = Native.getLastError()) {
                    ERROR_HOTKEY_ALREADY_REGISTERED -> HotKeyResult.Taken
                    else -> HotKeyResult.Failed("Windows said no (error $error)")
                }
            }
        }
    }

    override fun close() {
        val id = threadId
        if (id == 0) return
        // Ends the loop, which unregisters on its way out. The thread is a daemon, so a machine that somehow
        // ignores this still does not keep the process alive.
        runCatching { User32.INSTANCE.PostThreadMessage(id, WinUser.WM_QUIT, WinDef.WPARAM(0), WinDef.LPARAM(0)) }
        runCatching { worker.join(1_000) }
    }

    private companion object {
        const val WM_HOTKEY = 0x0312
        /** A message of this object's own, past the range Windows uses: "there are tasks waiting". */
        const val WM_RUN_TASKS = 0x8000 + 0x4E
        const val PM_NOREMOVE = 0
        /** A held chord is one press, not forty. */
        const val MOD_NOREPEAT = 0x4000
        const val ERROR_HOTKEY_ALREADY_REGISTERED = 1409
    }
}

/** Windows' names for keys: the virtual-key codes and the modifier bits `RegisterHotKey` takes. */
internal object WindowsKeys {
    private const val MOD_ALT = 0x0001
    private const val MOD_CONTROL = 0x0002
    private const val MOD_SHIFT = 0x0004
    private const val MOD_WIN = 0x0008

    fun modifiers(chord: KeyChord): Int =
        (if (chord.alt) MOD_ALT else 0) or (if (chord.ctrl) MOD_CONTROL else 0) or
            (if (chord.shift) MOD_SHIFT else 0) or (if (chord.meta) MOD_WIN else 0)

    private val NAMED = mapOf(
        "Space" to 0x20, "Enter" to 0x0D, "Escape" to 0x1B, "Tab" to 0x09, "Backspace" to 0x08, "Delete" to 0x2E,
        "Insert" to 0x2D, "Home" to 0x24, "End" to 0x23, "PageUp" to 0x21, "PageDown" to 0x22,
        "Left" to 0x25, "Up" to 0x26, "Right" to 0x27, "Down" to 0x28,
        "Comma" to 0xBC, "Period" to 0xBE, "Slash" to 0xBF, "Semicolon" to 0xBA, "Quote" to 0xDE,
        "BracketLeft" to 0xDB, "BracketRight" to 0xDD, "Backslash" to 0xDC, "Minus" to 0xBD, "Equals" to 0xBB,
        "Backquote" to 0xC0,
        "MediaPlayPause" to 0xB3, "MediaNext" to 0xB0, "MediaPrevious" to 0xB1, "MediaStop" to 0xB2,
        "VolumeUp" to 0xAF, "VolumeDown" to 0xAE, "VolumeMute" to 0xAD,
    )

    /** The virtual-key code for one of [KeyChord]'s keys, or null for one Windows has none for. */
    fun virtualKey(key: String): Int? = when {
        key.length == 1 && key[0] in 'A'..'Z' -> key[0].code
        key.length == 1 && key[0] in '0'..'9' -> key[0].code
        key.matches(Regex("F([1-9]|1[0-9]|2[0-4])")) -> 0x70 + key.drop(1).toInt() - 1
        else -> NAMED[key]
    }
}

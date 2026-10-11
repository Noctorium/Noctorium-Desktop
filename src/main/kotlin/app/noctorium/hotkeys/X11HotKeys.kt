package app.noctorium.hotkeys

import app.noctorium.settings.KeyChord
import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * Global shortcuts under X11, with `XGrabKey`: a passive grab of one key and its modifiers on the root window,
 * which is how X has always given a chord to one program -- the window managers take theirs the same way, so a
 * chord one of them holds is refused as taken.
 *
 * Xlib is not safe across threads, so everything here happens on one thread of this object's own, over a
 * connection to the display of its own rather than AWT's: the grabs, the polling for presses, and the letting go.
 * A refusal arrives as an X error, which Xlib's default handler answers by ending the program; the grabs are made
 * inside a handler of this object's own, which notes errors on its own connection and passes anything else to the
 * handler that was there before, and is taken out again straight after.
 *
 * Not run under X while it was written; see the release notes.
 */
internal class X11HotKeys(private val onPressed: (Int) -> Unit) : HotKeyBackend {
    private val x: Xlib = Native.load("X11", Xlib::class.java)
    private val tasks = ConcurrentLinkedQueue<() -> Unit>()
    @Volatile private var running = true

    // Only touched on the worker thread.
    @Volatile private var display: Pointer? = null
    private var root: NativeLong = NativeLong(0)
    private val held = HashMap<Int, Pair<KeyChord, Int>>()
    private var refusedOnOurs = false
    private val errorHandler = object : Xlib.XErrorHandler {
        override fun apply(display: Pointer?, event: Pointer?): Int {
            if (display == this@X11HotKeys.display) {
                refusedOnOurs = true
                return 0
            }
            return previousHandler?.let { Function.getFunction(it).invokeInt(arrayOf(display, event)) } ?: 0
        }
    }
    private var previousHandler: Pointer? = null

    private val ready = java.util.concurrent.CountDownLatch(1)

    private val worker = Thread({ run() }, "noctorium-global-shortcuts").apply {
        isDaemon = true
        start()
    }

    init {
        ready.await(2, TimeUnit.SECONDS)
        check(display != null) { "No X display to take keys from" }
    }

    private fun run() {
        display = runCatching { x.XOpenDisplay(null) }.getOrNull()
        ready.countDown()
        val opened = display ?: return
        root = x.XDefaultRootWindow(opened)
        val event = Memory(EVENT_SIZE)
        while (running) {
            while (true) {
                val task = tasks.poll() ?: break
                runCatching { task() }
            }
            while (x.XPending(opened) > 0) {
                x.XNextEvent(opened, event)
                if (event.getInt(0) == KEY_PRESS) {
                    val state = event.getInt(STATE_OFFSET) and CHORD_MODIFIERS
                    val code = event.getInt(KEYCODE_OFFSET)
                    held.entries.firstOrNull { (_, value) -> value.second == code && X11Keys.modifiers(value.first) == state }
                        ?.let { runCatching { onPressed(it.key) } }
                }
            }
            // Thirty times a second is quick enough for a person and costs next to nothing.
            Thread.sleep(33)
        }
        held.keys.toList().forEach { ungrab(opened, it) }
        x.XCloseDisplay(opened)
        display = null
    }

    private fun <T> onWorker(task: () -> T): T {
        val answer = CompletableFuture<T>()
        tasks.add { runCatching(task).fold(answer::complete, answer::completeExceptionally) }
        return answer.get(5, TimeUnit.SECONDS)
    }

    override fun bind(wanted: Map<Int, KeyChord>, names: Map<Int, String>): Map<Int, HotKeyResult> = onWorker {
        val opened = display ?: error("No X display to take keys from")
        held.entries.filter { (number, value) -> wanted[number] != value.first }.toList().forEach { (number, _) -> ungrab(opened, number) }
        wanted.mapValues { (number, chord) ->
            if (held[number]?.first == chord) return@mapValues HotKeyResult.Registered
            val keysym = X11Keys.keysym(chord.key) ?: return@mapValues HotKeyResult.Unsupported("X has no key named ${chord.key}")
            val code = x.XKeysymToKeycode(opened, NativeLong(keysym)).toInt() and 0xFF
            if (code == 0) return@mapValues HotKeyResult.Unsupported("This keyboard has no ${chord.key} key")
            refusedOnOurs = false
            previousHandler = x.XSetErrorHandler(errorHandler)
            try {
                // Once for each state of Caps Lock and Num Lock, which X counts as modifiers: without these, the
                // chord would stop working the moment Num Lock was on.
                LOCKS.forEach { lock -> x.XGrabKey(opened, code, X11Keys.modifiers(chord) or lock, root, 1, GRAB_MODE_ASYNC, GRAB_MODE_ASYNC) }
                x.XSync(opened, 0)
            } finally {
                x.XSetErrorHandler(previousHandler)
            }
            if (refusedOnOurs) {
                LOCKS.forEach { lock -> x.XUngrabKey(opened, code, X11Keys.modifiers(chord) or lock, root) }
                x.XSync(opened, 0)
                HotKeyResult.Taken
            } else {
                held[number] = chord to code
                HotKeyResult.Registered
            }
        }
    }

    private fun ungrab(opened: Pointer, number: Int) {
        val (chord, code) = held.remove(number) ?: return
        LOCKS.forEach { lock -> x.XUngrabKey(opened, code, X11Keys.modifiers(chord) or lock, root) }
        x.XSync(opened, 0)
    }

    override fun close() {
        running = false
        runCatching { worker.join(1_000) }
    }

    @Suppress("FunctionName")
    interface Xlib : Library {
        interface XErrorHandler : Callback {
            fun apply(display: Pointer?, event: Pointer?): Int
        }

        fun XOpenDisplay(name: String?): Pointer?
        fun XCloseDisplay(display: Pointer): Int
        fun XDefaultRootWindow(display: Pointer): NativeLong
        fun XKeysymToKeycode(display: Pointer, keysym: NativeLong): Byte
        fun XGrabKey(display: Pointer, keycode: Int, modifiers: Int, window: NativeLong, ownerEvents: Int, pointerMode: Int, keyboardMode: Int): Int
        fun XUngrabKey(display: Pointer, keycode: Int, modifiers: Int, window: NativeLong): Int
        fun XSync(display: Pointer, discard: Int): Int
        fun XPending(display: Pointer): Int
        fun XNextEvent(display: Pointer, event: Pointer): Int
        fun XSetErrorHandler(handler: XErrorHandler): Pointer?
        fun XSetErrorHandler(handler: Pointer?): Pointer?
    }

    private companion object {
        const val KEY_PRESS = 2
        const val GRAB_MODE_ASYNC = 1
        /** An XEvent is a union 24 longs wide. */
        const val EVENT_SIZE = 24L * 8
        /** Where XKeyEvent keeps its modifier state and its key code, on a 64-bit machine. */
        const val STATE_OFFSET = 80L
        const val KEYCODE_OFFSET = 84L
        /** Caps Lock and Num Lock, in each combination. */
        val LOCKS = listOf(0, 2, 16, 2 or 16)
        /** Shift, Control, Alt and Super: the modifiers a chord is made of, without the locks. */
        const val CHORD_MODIFIERS = 1 or 4 or 8 or 64
    }
}

/** X's names for keys -- keysyms -- and its modifier bits. */
internal object X11Keys {
    fun modifiers(chord: KeyChord): Int =
        (if (chord.shift) 1 else 0) or (if (chord.ctrl) 4 else 0) or (if (chord.alt) 8 else 0) or (if (chord.meta) 64 else 0)

    private val NAMED = mapOf(
        "Space" to 0x20L, "Enter" to 0xFF0DL, "Escape" to 0xFF1BL, "Tab" to 0xFF09L, "Backspace" to 0xFF08L,
        "Delete" to 0xFFFFL, "Insert" to 0xFF63L, "Home" to 0xFF50L, "End" to 0xFF57L, "PageUp" to 0xFF55L,
        "PageDown" to 0xFF56L, "Left" to 0xFF51L, "Up" to 0xFF52L, "Right" to 0xFF53L, "Down" to 0xFF54L,
        "Comma" to 0x2CL, "Period" to 0x2EL, "Slash" to 0x2FL, "Semicolon" to 0x3BL, "Quote" to 0x27L,
        "BracketLeft" to 0x5BL, "BracketRight" to 0x5DL, "Backslash" to 0x5CL, "Minus" to 0x2DL, "Equals" to 0x3DL,
        "Backquote" to 0x60L,
        "MediaPlayPause" to 0x1008FF14L, "MediaNext" to 0x1008FF17L, "MediaPrevious" to 0x1008FF16L,
        "MediaStop" to 0x1008FF15L, "VolumeUp" to 0x1008FF13L, "VolumeDown" to 0x1008FF11L, "VolumeMute" to 0x1008FF12L,
    )

    /** The keysym for one of [KeyChord]'s keys: a letter's is its lower-case character, a digit's its own. */
    fun keysym(key: String): Long? = when {
        key.length == 1 && key[0] in 'A'..'Z' -> key[0].lowercaseChar().code.toLong()
        key.length == 1 && key[0] in '0'..'9' -> key[0].code.toLong()
        key.matches(Regex("F([1-9]|1[0-9]|2[0-4])")) -> 0xFFBEL + key.drop(1).toLong() - 1
        else -> NAMED[key]
    }
}

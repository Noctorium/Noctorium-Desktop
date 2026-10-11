package app.noctorium.hotkeys

import app.noctorium.settings.KeyChord
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

/**
 * Global shortcuts on a Mac, with the Carbon Event Manager's `RegisterEventHotKey`.
 *
 * Old, and still how every Mac launcher and clipboard manager takes a chord: it needs no Accessibility permission
 * -- unlike watching the keyboard, which is what that permission is for -- and the system says when another
 * program already has the chord. Presses arrive as Carbon events on the application's main run loop, which AWT
 * already runs, through the handler installed here.
 *
 * Written against Apple's documented interface, but not run on a Mac while it was written; see the release notes.
 */
internal class MacHotKeys(private val onPressed: (Int) -> Unit) : HotKeyBackend {
    private val carbon: Carbon = Native.load("Carbon", Carbon::class.java)
    private val refs = HashMap<Int, Pointer>()
    private val chords = HashMap<Int, KeyChord>()

    // Kept in a field for as long as this lives: a callback the garbage collector takes is a crash on the next press.
    private val handler = object : Carbon.EventHandlerProc {
        override fun callback(nextHandler: Pointer?, event: Pointer?, userData: Pointer?): Int {
            if (event == null) return EVENT_NOT_HANDLED
            val id = EventHotKeyID()
            val status = carbon.GetEventParameter(event, PARAM_DIRECT_OBJECT, TYPE_HOT_KEY_ID, null, id.size(), null, id)
            if (status != 0 || id.signature != SIGNATURE) return EVENT_NOT_HANDLED
            runCatching { onPressed(id.id) }
            return 0
        }
    }
    private val handlerRef = PointerByReference()

    init {
        val spec = EventTypeSpec().apply {
            eventClass = KEYBOARD_CLASS
            eventKind = HOT_KEY_PRESSED
        }
        val status = carbon.InstallEventHandler(carbon.GetEventDispatcherTarget(), handler, 1, spec, null, handlerRef)
        check(status == 0) { "Could not listen for hot keys (Carbon said $status)" }
    }

    @Synchronized
    override fun bind(wanted: Map<Int, KeyChord>, names: Map<Int, String>): Map<Int, HotKeyResult> {
        chords.entries.filter { (number, chord) -> wanted[number] != chord }.toList().forEach { (number, _) ->
            refs.remove(number)?.let { carbon.UnregisterEventHotKey(it) }
            chords.remove(number)
        }
        return wanted.mapValues { (number, chord) ->
            if (chords[number] == chord) return@mapValues HotKeyResult.Registered
            val code = MacKeys.keyCode(chord.key)
                ?: return@mapValues HotKeyResult.Unsupported("A Mac has no hot key for ${chord.key}")
            val id = EventHotKeyID.ByValue().apply {
                signature = SIGNATURE
                this.id = number
            }
            val ref = PointerByReference()
            when (val status = carbon.RegisterEventHotKey(code, MacKeys.modifiers(chord), id, carbon.GetEventDispatcherTarget(), 0, ref)) {
                0 -> {
                    refs[number] = ref.value
                    chords[number] = chord
                    HotKeyResult.Registered
                }
                HOT_KEY_EXISTS -> HotKeyResult.Taken
                else -> HotKeyResult.Failed("macOS said no ($status)")
            }
        }
    }

    @Synchronized
    override fun close() {
        refs.values.forEach { runCatching { carbon.UnregisterEventHotKey(it) } }
        refs.clear()
        chords.clear()
        handlerRef.value?.let { runCatching { carbon.RemoveEventHandler(it) } }
    }

    @Suppress("FunctionName")
    interface Carbon : Library {
        interface EventHandlerProc : Callback {
            fun callback(nextHandler: Pointer?, event: Pointer?, userData: Pointer?): Int
        }

        fun GetEventDispatcherTarget(): Pointer
        fun InstallEventHandler(target: Pointer, handler: EventHandlerProc, count: Int, types: EventTypeSpec, userData: Pointer?, out: PointerByReference): Int
        fun RemoveEventHandler(handler: Pointer): Int
        fun RegisterEventHotKey(keyCode: Int, modifiers: Int, id: EventHotKeyID.ByValue, target: Pointer, options: Int, out: PointerByReference): Int
        fun UnregisterEventHotKey(ref: Pointer): Int
        fun GetEventParameter(event: Pointer, name: Int, type: Int, actualType: Pointer?, size: Int, actualSize: Pointer?, data: EventHotKeyID): Int
    }

    @Structure.FieldOrder("eventClass", "eventKind")
    class EventTypeSpec : Structure() {
        @JvmField var eventClass: Int = 0
        @JvmField var eventKind: Int = 0
    }

    @Structure.FieldOrder("signature", "id")
    open class EventHotKeyID : Structure() {
        @JvmField var signature: Int = 0
        @JvmField var id: Int = 0

        class ByValue : EventHotKeyID(), Structure.ByValue
    }

    private companion object {
        /** A four-letter code, as Carbon has them, marking the hot keys as this program's: "noct". */
        val SIGNATURE = fourCharCode("noct")
        val KEYBOARD_CLASS = fourCharCode("keyb")
        const val HOT_KEY_PRESSED = 5
        val PARAM_DIRECT_OBJECT = fourCharCode("----")
        val TYPE_HOT_KEY_ID = fourCharCode("hkid")
        const val EVENT_NOT_HANDLED = -9874
        /** eventHotKeyExistsErr: somebody else registered that chord. */
        const val HOT_KEY_EXISTS = -9878

        fun fourCharCode(text: String): Int = text.fold(0) { code, c -> (code shl 8) or c.code }
    }
}

/** The Mac's names for keys: the virtual key codes of a US layout, which Carbon's hot keys take, and its modifier bits. */
internal object MacKeys {
    private const val CMD = 1 shl 8
    private const val SHIFT = 1 shl 9
    private const val OPTION = 1 shl 11
    private const val CONTROL = 1 shl 12

    fun modifiers(chord: KeyChord): Int =
        (if (chord.meta) CMD else 0) or (if (chord.shift) SHIFT else 0) or (if (chord.alt) OPTION else 0) or (if (chord.ctrl) CONTROL else 0)

    private val CODES = mapOf(
        "A" to 0x00, "S" to 0x01, "D" to 0x02, "F" to 0x03, "H" to 0x04, "G" to 0x05, "Z" to 0x06, "X" to 0x07,
        "C" to 0x08, "V" to 0x09, "B" to 0x0B, "Q" to 0x0C, "W" to 0x0D, "E" to 0x0E, "R" to 0x0F, "Y" to 0x10,
        "T" to 0x11, "1" to 0x12, "2" to 0x13, "3" to 0x14, "4" to 0x15, "6" to 0x16, "5" to 0x17, "Equals" to 0x18,
        "9" to 0x19, "7" to 0x1A, "Minus" to 0x1B, "8" to 0x1C, "0" to 0x1D, "BracketRight" to 0x1E, "O" to 0x1F,
        "U" to 0x20, "BracketLeft" to 0x21, "I" to 0x22, "P" to 0x23, "Enter" to 0x24, "L" to 0x25, "J" to 0x26,
        "Quote" to 0x27, "K" to 0x28, "Semicolon" to 0x29, "Backslash" to 0x2A, "Comma" to 0x2B, "Slash" to 0x2C,
        "N" to 0x2D, "M" to 0x2E, "Period" to 0x2F, "Tab" to 0x30, "Space" to 0x31, "Backquote" to 0x32,
        "Backspace" to 0x33, "Escape" to 0x35,
        "F1" to 0x7A, "F2" to 0x78, "F3" to 0x63, "F4" to 0x76, "F5" to 0x60, "F6" to 0x61, "F7" to 0x62, "F8" to 0x64,
        "F9" to 0x65, "F10" to 0x6D, "F11" to 0x67, "F12" to 0x6F, "F13" to 0x69, "F14" to 0x6B, "F15" to 0x71,
        "F16" to 0x6A, "F17" to 0x40, "F18" to 0x4F, "F19" to 0x50, "F20" to 0x5A,
        "Insert" to 0x72, "Home" to 0x73, "PageUp" to 0x74, "Delete" to 0x75, "End" to 0x77, "PageDown" to 0x79,
        "Left" to 0x7B, "Right" to 0x7C, "Down" to 0x7D, "Up" to 0x7E,
    )

    /** The key code for one of [KeyChord]'s keys, or null for one a Mac's hot keys cannot take -- the media keys among them. */
    fun keyCode(key: String): Int? = CODES[key]
}

/**
 * Hides Noctorium on a Mac, as ⌘H does, so the program that was in front before the quick search window came up
 * is in front again. Done through the Objective-C runtime because AWT has no word for it; quietly nothing where
 * that is not to be had.
 */
internal object MacApplication {
    private interface ObjC : Library {
        fun objc_getClass(name: String): Pointer?
        fun sel_registerName(name: String): Pointer?
        fun objc_msgSend(receiver: Pointer?, selector: Pointer?, argument: Pointer?): Pointer?
        fun objc_msgSend(receiver: Pointer?, selector: Pointer?, action: Pointer?, argument: Pointer?, wait: Boolean): Pointer?
    }

    fun hide() {
        runCatching {
            val objc = Native.load("objc", ObjC::class.java)
            val application = objc.objc_msgSend(objc.objc_getClass("NSApplication"), objc.sel_registerName("sharedApplication"), null)
            // Handed to AppKit's main thread, the only one it takes this from; AWT's event thread is not that one.
            objc.objc_msgSend(
                application,
                objc.sel_registerName("performSelectorOnMainThread:withObject:waitUntilDone:"),
                objc.sel_registerName("hide:"),
                null,
                false,
            )
        }
    }
}

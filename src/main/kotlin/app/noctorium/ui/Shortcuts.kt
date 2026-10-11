package app.noctorium.ui

import androidx.compose.ui.input.key.Key
import app.noctorium.core.Destination
import app.noctorium.settings.KeyChord
import app.noctorium.settings.ShortcutPreferences
import java.awt.event.KeyEvent as AwtKeyEvent

/**
 * Driving Noctorium from the keyboard.
 *
 * Desktop only, and deliberately: a phone has no keyboard to speak of and the gestures there already do
 * this. What is here by default is what somebody reaches for without being taught -- space to pause, arrows to
 * seek, the numbers for the places in the sidebar -- borrowed from the players people already use rather than
 * invented. Every one of them can be changed, in Settings, under Keyboard shortcuts; `?` shows the list, because
 * a shortcut nobody can find is not a shortcut.
 *
 * The mapping is a function of a key, the modifiers and the listener's bindings, and nothing else. That is what
 * makes it testable: every binding below is checked without a window, a player, or a running application.
 */
sealed interface Shortcut {
    data object PlayPause : Shortcut
    data object Next : Shortcut
    data object Previous : Shortcut

    /** Pause and go back to the start, as a Stop button does. */
    data object Stop : Shortcut

    /** Stop when the song playing ends, once. Pressed again, it is taken back. */
    data object StopAfterCurrent : Shortcut

    /** Negative rewinds. Milliseconds, so the caller does not have to know the step. */
    data class Seek(val deltaMs: Long) : Shortcut

    /** A fraction of full scale, positive or negative. */
    data class Volume(val delta: Float) : Shortcut
    data object Mute : Shortcut

    /** The volume booster, up or down by this many percent; up from off switches it on. */
    data class Boost(val deltaPercent: Int) : Shortcut
    data object Shuffle : Shortcut
    data object Repeat : Shortcut

    /** Likes the track that is playing, on whichever service it came from. */
    data object Like : Shortcut

    /** The queue becomes the song playing alone. */
    data object ClearAllButCurrent : Shortcut

    data class Go(val destination: Destination) : Shortcut

    /** Goes to the Search page and puts the caret in its box. */
    data object Search : Shortcut

    /** The quick search bar, over whatever is on the screen. */
    data object QuickSearch : Shortcut

    /** Plays the link on the clipboard, from anywhere that is not a box being typed in. */
    data object PasteLink : Shortcut

    data object Help : Shortcut

    /** Hides the window, into the tray where there is one; shows it again from anywhere else. */
    data object ToggleWindow : Shortcut

    /** Closes whatever is over the page: the help sheet first, then an open panel. */
    data object Dismiss : Shortcut
}

/** How the Shortcuts page groups the actions, the way somebody would look for one. */
enum class ShortcutGroup(val title: String) {
    PLAYING("Playing"),
    SOUND("Sound"),
    QUEUE("The queue"),
    PLACES("Going places"),
    WINDOW("The window"),
}

/**
 * Everything the keyboard can do, by the name it is kept under in the settings file.
 *
 * [id] is that name and must never change: the listener's own chords are kept by it. [defaults] are the chords
 * each action comes with, up to two -- Space and K both pause, because everybody tries one of the two -- written
 * the way [KeyChord] writes them, with `Mod` for the key a platform's commands use: Ctrl on Windows and Linux, ⌘
 * on a Mac. [global] actions are the ones that make sense from another program, and can also be bound there;
 * [globalSuggestion] is the chord offered for one, which is never taken without being asked for.
 */
enum class ShortcutAction(
    val id: String,
    val title: String,
    val group: ShortcutGroup,
    val shortcut: Shortcut,
    val defaults: List<String> = emptyList(),
    val global: Boolean = false,
    val globalSuggestion: String? = null,
) {
    PLAY_PAUSE("playPause", "Play or pause", ShortcutGroup.PLAYING, Shortcut.PlayPause, listOf("Space", "K"), global = true, globalSuggestion = "Ctrl+Alt+Home"),
    STOP("stop", "Stop", ShortcutGroup.PLAYING, Shortcut.Stop, listOf("Mod+Period"), global = true, globalSuggestion = "Ctrl+Alt+End"),
    STOP_AFTER_CURRENT("stopAfterCurrent", "Stop after this song", ShortcutGroup.PLAYING, Shortcut.StopAfterCurrent, listOf("Mod+Shift+Period")),
    NEXT("next", "Next song", ShortcutGroup.PLAYING, Shortcut.Next, listOf("N", "Mod+Right"), global = true, globalSuggestion = "Ctrl+Alt+PageDown"),
    PREVIOUS("previous", "Previous song", ShortcutGroup.PLAYING, Shortcut.Previous, listOf("P", "Mod+Left"), global = true, globalSuggestion = "Ctrl+Alt+PageUp"),
    FORWARD("seekForward", "Forward 5 seconds", ShortcutGroup.PLAYING, Shortcut.Seek(FINE_SEEK_MS), listOf("Right")),
    BACK("seekBack", "Back 5 seconds", ShortcutGroup.PLAYING, Shortcut.Seek(-FINE_SEEK_MS), listOf("Left")),
    FORWARD_MEDIUM("seekForward10", "Forward 10 seconds", ShortcutGroup.PLAYING, Shortcut.Seek(MEDIUM_SEEK_MS), listOf("L")),
    BACK_MEDIUM("seekBack10", "Back 10 seconds", ShortcutGroup.PLAYING, Shortcut.Seek(-MEDIUM_SEEK_MS), listOf("J")),
    FORWARD_COARSE("seekForward30", "Forward 30 seconds", ShortcutGroup.PLAYING, Shortcut.Seek(COARSE_SEEK_MS), listOf("Shift+Right")),
    BACK_COARSE("seekBack30", "Back 30 seconds", ShortcutGroup.PLAYING, Shortcut.Seek(-COARSE_SEEK_MS), listOf("Shift+Left")),
    SHUFFLE("shuffle", "Shuffle", ShortcutGroup.PLAYING, Shortcut.Shuffle, listOf("S")),
    REPEAT("repeat", "Repeat", ShortcutGroup.PLAYING, Shortcut.Repeat, listOf("R")),
    LIKE("like", "Like the song playing", ShortcutGroup.PLAYING, Shortcut.Like, listOf("F"), global = true),

    VOLUME_UP("volumeUp", "Volume up", ShortcutGroup.SOUND, Shortcut.Volume(VOLUME_STEP), listOf("Up"), global = true, globalSuggestion = "Ctrl+Alt+Up"),
    VOLUME_DOWN("volumeDown", "Volume down", ShortcutGroup.SOUND, Shortcut.Volume(-VOLUME_STEP), listOf("Down"), global = true, globalSuggestion = "Ctrl+Alt+Down"),
    MUTE("mute", "Mute", ShortcutGroup.SOUND, Shortcut.Mute, listOf("M"), global = true),
    BOOST_UP("boostUp", "Volume booster up", ShortcutGroup.SOUND, Shortcut.Boost(BOOST_STEP_PERCENT), listOf("Shift+Up")),
    BOOST_DOWN("boostDown", "Volume booster down", ShortcutGroup.SOUND, Shortcut.Boost(-BOOST_STEP_PERCENT), listOf("Shift+Down")),

    // Nothing by default: it throws away the rest of the queue, which is not a thing to do by brushing a key.
    CLEAR_ALL_BUT_CURRENT("clearAllButCurrent", "Clear all but this song", ShortcutGroup.QUEUE, Shortcut.ClearAllButCurrent),

    QUICK_SEARCH("quickSearch", "Quick search", ShortcutGroup.PLACES, Shortcut.QuickSearch, listOf("Mod+F"), global = true, globalSuggestion = "Ctrl+Alt+Space"),
    SEARCH("search", "Search, with the caret in the box", ShortcutGroup.PLACES, Shortcut.Search, listOf("Slash")),
    GO_HOME("goHome", "Go to Home", ShortcutGroup.PLACES, Shortcut.Go(Destination.HOME), listOf("1")),
    GO_SEARCH("goSearch", "Go to Search", ShortcutGroup.PLACES, Shortcut.Go(Destination.SEARCH), listOf("2")),
    GO_LINK("goLink", "Go to Paste link", ShortcutGroup.PLACES, Shortcut.Go(Destination.LINK), listOf("3")),
    GO_LIBRARY("goLibrary", "Go to the library", ShortcutGroup.PLACES, Shortcut.Go(Destination.LIBRARY), listOf("4")),
    GO_DOWNLOADS("goDownloads", "Go to Downloads", ShortcutGroup.PLACES, Shortcut.Go(Destination.DOWNLOADS), listOf("5")),
    GO_NOW_PLAYING("goNowPlaying", "Go to Now playing", ShortcutGroup.PLACES, Shortcut.Go(Destination.NOW_PLAYING), listOf("6")),
    GO_QUEUE("goQueue", "Go to the queue", ShortcutGroup.PLACES, Shortcut.Go(Destination.QUEUE), listOf("7")),
    GO_SETTINGS("goSettings", "Go to Settings", ShortcutGroup.PLACES, Shortcut.Go(Destination.SETTINGS), listOf("8")),
    PASTE_LINK("pasteLink", "Play the link you copied", ShortcutGroup.PLACES, Shortcut.PasteLink, listOf("Mod+V")),
    HELP("help", "The list of shortcuts", ShortcutGroup.PLACES, Shortcut.Help, listOf("Shift+Slash")),

    TOGGLE_WINDOW("toggleWindow", "Show or hide Noctorium", ShortcutGroup.WINDOW, Shortcut.ToggleWindow, global = true),
    ;

    /** The name the chord in [slot] is kept under: the action's own for its first, with the number after for its second. */
    fun storedName(slot: Int): String = if (slot == 0) id else "$id.${slot + 1}"

    /** The chord [slot] comes with on this platform, or null where it comes with none. */
    fun defaultChord(slot: Int, mac: Boolean): KeyChord? = defaults.getOrNull(slot)?.let { platformChord(it, mac) }

    companion object {
        /** An action has at most this many chords: the one it is known by, and another. */
        const val SLOTS = 2

        fun byId(id: String): ShortcutAction? = entries.firstOrNull { it.id == id }
    }
}

/** One of an action's places for a chord: its first, or its second. */
data class ShortcutSlot(val action: ShortcutAction, val index: Int) {
    val storedName: String get() = action.storedName(index)
}

/**
 * [text] as a chord, with `Mod` read as the key this platform's commands use: Ctrl, or ⌘ -- Meta -- on a Mac.
 * So "Mod+F" is Ctrl+F on Windows and Linux and ⌘F on a Mac, which is what each of them means by Find.
 */
fun platformChord(text: String, mac: Boolean): KeyChord? =
    KeyChord.parse(text.split('+').joinToString("+") { part -> if (part.trim().equals("Mod", ignoreCase = true)) (if (mac) "Meta" else "Ctrl") else part })

/**
 * The in-app shortcuts as the listener has them: each action's defaults, with what they changed on top.
 *
 * Two chords for one action is fine; one chord for two actions is a conflict, which the Shortcuts page shows and
 * offers to settle. Until it is settled the action listed first keeps the chord, so a press is never ambiguous.
 */
class InAppShortcuts(val chords: Map<ShortcutSlot, KeyChord>) {
    private val byChord: Map<KeyChord, ShortcutAction> = buildMap {
        ShortcutAction.entries.forEach { action ->
            (0 until ShortcutAction.SLOTS).forEach { slot -> chords[ShortcutSlot(action, slot)]?.let { putIfAbsent(it, action) } }
        }
    }

    /** [action]'s chords, one per slot, null where a slot is empty. */
    fun chordsOf(action: ShortcutAction): List<KeyChord?> = (0 until ShortcutAction.SLOTS).map { chords[ShortcutSlot(action, it)] }

    /** What [chord] does, or null for nothing. */
    fun actionFor(chord: KeyChord): ShortcutAction? = byChord[chord]

    /** The other places [chord] is already bound, leaving out [except]. */
    fun conflicts(chord: KeyChord, except: ShortcutSlot? = null): List<ShortcutSlot> =
        chords.filter { (slot, bound) -> bound == chord && slot != except }.keys.sortedWith(compareBy({ it.action.ordinal }, { it.index }))

    /** Every chord bound to more than one place, and those places. */
    fun allConflicts(): Map<KeyChord, List<ShortcutSlot>> =
        chords.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }

    companion object {
        /** The shortcuts as they come, before anybody changed one. */
        fun defaults(mac: Boolean): InAppShortcuts = from(ShortcutPreferences(), mac)

        /** The shortcuts with the listener's changes from [preferences] applied. */
        fun from(preferences: ShortcutPreferences, mac: Boolean): InAppShortcuts = InAppShortcuts(
            buildMap {
                ShortcutAction.entries.forEach { action ->
                    (0 until ShortcutAction.SLOTS).forEach { slot ->
                        val default = action.defaultChord(slot, mac)?.toString()
                        preferences.inAppChord(action.storedName(slot), default)?.let(KeyChord::parse)?.let { put(ShortcutSlot(action, slot), it) }
                    }
                }
            },
        )
    }
}

/**
 * What a key press means, or null for one that means nothing here.
 *
 * [typing] is the whole reason this takes more than a key. With the caret in the search box, space is a
 * space and `n` is a letter; only the chords with Ctrl, Alt or ⌘ in them survive -- and not the ones a box
 * has always used for itself, copying, pasting, undoing -- so somebody searching for "no surprises" does not
 * pause the music four times and skip a track.
 */
fun shortcutFor(
    key: Key,
    ctrl: Boolean = false,
    shift: Boolean = false,
    alt: Boolean = false,
    typing: Boolean = false,
    meta: Boolean = false,
    shortcuts: InAppShortcuts = InAppShortcuts.defaults(isMacPlatform),
): Shortcut? {
    val chord = chordOf(key, ctrl = ctrl, alt = alt, shift = shift, meta = meta)
    val bound = chord?.let(shortcuts::actionFor)
    if (bound != null && (!typing || worksWhileTyping(chord))) return bound.shortcut
    // The media keys a keyboard may have of its own, which arrive as ordinary key events here and belong to
    // nothing else, typing or not.
    when (key) {
        Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> return Shortcut.PlayPause
        Key.MediaNext -> return Shortcut.Next
        Key.MediaPrevious -> return Shortcut.Previous
        Key.MediaStop -> return Shortcut.Stop
    }
    // Escape gets out of things even while typing -- that is most of what it is for -- and is not for changing.
    if (key == Key.Escape && !ctrl && !alt && !shift && !meta) return Shortcut.Dismiss
    return null
}

/**
 * Whether [chord] still works with the caret in a box: only one that could not be a character, and is not one of
 * the chords a box answers itself. Ctrl+V in a box has always meant putting the text there.
 */
fun worksWhileTyping(chord: KeyChord): Boolean {
    if (chord.key.startsWith("Media") || chord.key.startsWith("Volume") || chord.key.matches(Regex("F\\d+"))) return true
    if (!chord.ctrl && !chord.alt && !chord.meta) return false
    val command = (chord.ctrl || chord.meta) && !chord.alt
    return !(command && chord.key in TEXT_BOX_KEYS)
}

/** What a box does with Ctrl, or ⌘, and these: select all, copy, cut, paste, undo and redo. */
private val TEXT_BOX_KEYS = setOf("A", "C", "V", "X", "Z", "Y")

/** Steps, in one place, so the help sheet and the bindings cannot disagree about what a press does. */
const val FINE_SEEK_MS = 5_000L
const val MEDIUM_SEEK_MS = 10_000L
const val COARSE_SEEK_MS = 30_000L
const val VOLUME_STEP = 0.05f

/** How far one press moves the volume booster: 100% to 1000% in eighteen presses rather than ninety. */
const val BOOST_STEP_PERCENT = 50

/** Whether this is a Mac, where commands are ⌘ and chords are written in symbols. */
val isMacPlatform: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

// --- Keys and their names ---

/**
 * The keys a chord can be made of, as Compose names them and as [KeyChord] writes them. Both ways round: a key
 * pressed becomes a chord to look up or to record, and a chord shown on the page names its key.
 */
private val KEY_NAMES: List<Pair<Key, String>> = buildList {
    listOf(
        Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
        Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
    ).forEachIndexed { i, key -> add(key to ('A' + i).toString()) }
    listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
        .forEachIndexed { i, key -> add(key to i.toString()) }
    // The keypad's digits are the same digits: 1 on either goes Home.
    listOf(Key.NumPad0, Key.NumPad1, Key.NumPad2, Key.NumPad3, Key.NumPad4, Key.NumPad5, Key.NumPad6, Key.NumPad7, Key.NumPad8, Key.NumPad9)
        .forEachIndexed { i, key -> add(key to i.toString()) }
    listOf(Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12)
        .forEachIndexed { i, key -> add(key to "F${i + 1}") }
    // Compose has no names past F12; the keyboards that have them send AWT's codes, which follow on in order.
    (13..24).forEach { n -> add(Key(AwtKeyEvent.VK_F13 + (n - 13)) to "F$n") }
    addAll(
        listOf(
            Key.Spacebar to "Space", Key.Enter to "Enter", Key.NumPadEnter to "Enter", Key.Escape to "Escape",
            Key.Tab to "Tab", Key.Backspace to "Backspace", Key.Delete to "Delete", Key.Insert to "Insert",
            Key.MoveHome to "Home", Key.MoveEnd to "End", Key.PageUp to "PageUp", Key.PageDown to "PageDown",
            Key.DirectionUp to "Up", Key.DirectionDown to "Down", Key.DirectionLeft to "Left", Key.DirectionRight to "Right",
            Key.Comma to "Comma", Key.Period to "Period", Key.Slash to "Slash", Key.Semicolon to "Semicolon",
            Key.Apostrophe to "Quote", Key.LeftBracket to "BracketLeft", Key.RightBracket to "BracketRight",
            Key.Backslash to "Backslash", Key.Minus to "Minus", Key.Equals to "Equals", Key.Grave to "Backquote",
            Key.MediaPlayPause to "MediaPlayPause", Key.MediaNext to "MediaNext", Key.MediaPrevious to "MediaPrevious",
            Key.MediaStop to "MediaStop", Key.VolumeUp to "VolumeUp", Key.VolumeDown to "VolumeDown", Key.VolumeMute to "VolumeMute",
        ),
    )
}

private val NAME_OF: Map<Key, String> = KEY_NAMES.toMap()

/** The keys that are only ever held with another: pressed alone while recording, they are waited past. */
private val MODIFIER_KEYS = setOf(
    Key.ShiftLeft, Key.ShiftRight, Key.CtrlLeft, Key.CtrlRight, Key.AltLeft, Key.AltRight, Key.MetaLeft, Key.MetaRight,
)

/** Whether [key] is a modifier on its own. */
fun isModifierKey(key: Key): Boolean = key in MODIFIER_KEYS

/** The chord a press makes, or null for a key no chord can be made of. */
fun chordOf(key: Key, ctrl: Boolean = false, alt: Boolean = false, shift: Boolean = false, meta: Boolean = false): KeyChord? =
    NAME_OF[key]?.let { KeyChord(it, ctrl = ctrl, alt = alt, shift = shift, meta = meta) }

// --- Writing a chord down ---

/**
 * A chord as the listener reads it, one key at a time: Ctrl, Shift, "." on Windows and Linux; ⌃, ⌥, ⇧, ⌘ and
 * the key on a Mac, in the order the Mac's own menus put them. Arrows are arrows and the punctuation is itself,
 * since nobody calls the full stop "Period".
 */
fun chordParts(chord: KeyChord, mac: Boolean = isMacPlatform, linux: Boolean = isLinuxPlatform): List<String> = buildList {
    if (mac) {
        if (chord.ctrl) add("⌃")
        if (chord.alt) add("⌥")
        if (chord.shift) add("⇧")
        if (chord.meta) add("⌘")
    } else {
        if (chord.ctrl) add("Ctrl")
        if (chord.alt) add("Alt")
        if (chord.shift) add("Shift")
        if (chord.meta) add(if (linux) "Super" else "Win")
    }
    add(keyLabel(chord.key, mac))
}

/** [chord] in one piece: "Ctrl+Shift+." or "⇧⌘." */
fun chordLabel(chord: KeyChord, mac: Boolean = isMacPlatform, linux: Boolean = isLinuxPlatform): String =
    chordParts(chord, mac, linux).joinToString(if (mac) "" else "+")

/** A key's name as it is shown, rather than as it is kept. */
fun keyLabel(key: String, mac: Boolean = isMacPlatform): String = when (key) {
    "Up" -> "↑"
    "Down" -> "↓"
    "Left" -> "←"
    "Right" -> "→"
    "Comma" -> ","
    "Period" -> "."
    "Slash" -> "/"
    "Semicolon" -> ";"
    "Quote" -> "'"
    "BracketLeft" -> "["
    "BracketRight" -> "]"
    "Backslash" -> "\\"
    "Minus" -> "-"
    "Equals" -> "="
    "Backquote" -> "`"
    "Escape" -> if (mac) "⎋" else "Esc"
    "Enter" -> if (mac) "↩" else "Enter"
    "Backspace" -> if (mac) "⌫" else "Backspace"
    "Delete" -> if (mac) "⌦" else "Delete"
    "Tab" -> if (mac) "⇥" else "Tab"
    "PageUp" -> if (mac) "⇞" else "Page Up"
    "PageDown" -> if (mac) "⇟" else "Page Down"
    "Home" -> if (mac) "↖" else "Home"
    "End" -> if (mac) "↘" else "End"
    "MediaPlayPause" -> "Play/Pause"
    "MediaNext" -> "Next track"
    "MediaPrevious" -> "Previous track"
    "MediaStop" -> "Stop"
    "VolumeUp" -> "Volume up"
    "VolumeDown" -> "Volume down"
    "VolumeMute" -> "Mute"
    else -> key
}

/** Whether this is Linux, where the key with the window on it is called Super. */
val isLinuxPlatform: Boolean = System.getProperty("os.name").orEmpty().startsWith("Linux", ignoreCase = true)

// --- Global shortcuts ---

/**
 * Why [chord] cannot be a global shortcut, or null where it can.
 *
 * A global chord is taken from every other program on the machine, so it has to be one they are not using: two
 * of Ctrl, Alt and the Windows key or ⌘ together, or one of the keys nobody types with (F13 and up, Stop). A key
 * alone, or with Shift, is typing; Ctrl or ⌘ and a letter is every program's own commands -- Ctrl+F above all,
 * which is Find everywhere, and Noctorium's quick search only inside Noctorium. The media keys already work from
 * anywhere, and the volume keys belong to the computer.
 */
fun globalRefusal(chord: KeyChord, mac: Boolean = isMacPlatform, linux: Boolean = isLinuxPlatform): String? {
    val command = if (mac) "⌘" else "Ctrl"
    val strong = listOf(chord.ctrl, chord.alt, chord.meta).count { it }
    val key = chord.key
    return when {
        key == "MediaPlayPause" || key == "MediaNext" || key == "MediaPrevious" ->
            "The media keys already work from anywhere, whichever program is in front."
        key == "VolumeUp" || key == "VolumeDown" || key == "VolumeMute" -> "The volume keys are left to the computer's own volume."
        key == "F" && strong == 1 && (if (mac) chord.meta else chord.ctrl) ->
            "$command+F is Find in every program. Inside Noctorium it is already the quick search; from anywhere, use another chord."
        key == "MediaStop" || key.matches(Regex("F(1[3-9]|2[0-4])")) -> null
        !linux && !mac && chord.ctrl && chord.alt && key == "Delete" -> "Windows keeps Ctrl+Alt+Delete for itself."
        linux && chord.ctrl && chord.alt && (key == "Backspace" || key == "Delete" || key.matches(Regex("F([1-9]|1[0-2])"))) ->
            "Linux keeps ${chordLabel(chord, mac, linux)} for itself."
        strong < 2 -> "Hold two of ${if (mac) "⌃, ⌥ and ⌘" else if (linux) "Ctrl, Alt and Super" else "Ctrl, Alt and Win"} with the key: " +
            "with fewer, it is a key other programs are using."
        else -> null
    }
}

// --- The help sheet ---

/** One row of the help sheet. */
data class ShortcutHelp(val keys: String, val what: String)

/**
 * What `?` puts on the screen, grouped the way somebody would look for it, from the chords as they are now --
 * so the sheet cannot disagree with the keys. Actions with no chord are left off; the Shortcuts page has them.
 */
fun shortcutHelp(shortcuts: InAppShortcuts, mac: Boolean = isMacPlatform): List<Pair<String, List<ShortcutHelp>>> {
    val groups = ShortcutGroup.entries.mapNotNull { group ->
        val rows = ShortcutAction.entries.filter { it.group == group }.mapNotNull { action ->
            val chords = shortcuts.chordsOf(action).filterNotNull()
            if (chords.isEmpty()) null else ShortcutHelp(chords.joinToString("  ·  ") { chordLabel(it, mac) }, action.title)
        }
        if (rows.isEmpty()) null else group.title to rows
    }
    return groups + listOf(
        "Always" to listOf(ShortcutHelp(if (mac) "⎋" else "Esc", "Close what is open, or leave the box you are typing in")),
        "Anywhere on the machine" to listOf(
            ShortcutHelp("▶❚❚  ⏭  ⏮", "The media keys work while Noctorium is behind another window"),
        ),
    )
}

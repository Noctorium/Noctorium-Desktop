package app.noctorium.ui

import androidx.compose.ui.input.key.Key
import app.noctorium.core.Destination

/**
 * Driving Noctorium from the keyboard.
 *
 * Desktop only, and deliberately: a phone has no keyboard to speak of and the gestures there already do
 * this. What is here is what somebody reaches for without being taught -- space to pause, arrows to seek,
 * the numbers for the places in the sidebar -- borrowed from the players people already use rather than
 * invented. `?` shows the list, because a shortcut nobody can find is not a shortcut.
 *
 * The mapping is a function of a key and the modifiers, and nothing else. That is what makes it testable:
 * every binding below is checked without a window, a player, or a running application.
 */
sealed interface Shortcut {
    data object PlayPause : Shortcut
    data object Next : Shortcut
    data object Previous : Shortcut

    /** Negative rewinds. Milliseconds, so the caller does not have to know the step. */
    data class Seek(val deltaMs: Long) : Shortcut

    /** A fraction of full scale, positive or negative. */
    data class Volume(val delta: Float) : Shortcut
    data object Mute : Shortcut
    data object Shuffle : Shortcut
    data object Repeat : Shortcut

    /** Likes the track that is playing, on whichever service it came from. */
    data object Like : Shortcut

    data class Go(val destination: Destination) : Shortcut

    /** Puts the caret in the search box, wherever the listener happens to be. */
    data object Search : Shortcut

    data object Help : Shortcut

    /** Closes whatever is over the page: the help sheet first, then an open panel. */
    data object Dismiss : Shortcut
}

/**
 * What a key press means, or null for one that means nothing here.
 *
 * [typing] is the whole reason this takes more than a key. With the caret in the search box, space is a
 * space and `n` is a letter; only the modified bindings survive, so somebody searching for "no surprises"
 * does not pause the music four times and skip a track.
 */
fun shortcutFor(
    key: Key,
    ctrl: Boolean = false,
    shift: Boolean = false,
    alt: Boolean = false,
    typing: Boolean = false,
): Shortcut? {
    // Modified bindings work everywhere, including mid-word, because none of them could be a character.
    when {
        ctrl && key == Key.F -> return Shortcut.Search
        ctrl && key == Key.DirectionRight -> return Shortcut.Next
        ctrl && key == Key.DirectionLeft -> return Shortcut.Previous
        // The media keys a keyboard may have of its own, and which arrive as ordinary key events here.
        key == Key.MediaPlayPause || key == Key.MediaPlay || key == Key.MediaPause -> return Shortcut.PlayPause
        key == Key.MediaNext -> return Shortcut.Next
        key == Key.MediaPrevious -> return Shortcut.Previous
    }
    // Escape gets out of things even while typing -- that is most of what it is for.
    if (key == Key.Escape) return Shortcut.Dismiss
    if (typing || ctrl || alt) return null

    return when (key) {
        // Space is the one everybody tries first. K is what anybody who uses YouTube tries second.
        Key.Spacebar, Key.K -> Shortcut.PlayPause

        // Seeking: the arrows in small steps, J and L in the ten-second ones YouTube taught everybody,
        // and holding shift makes the arrows coarse for finding a place in something long.
        Key.DirectionRight -> Shortcut.Seek(if (shift) COARSE_SEEK_MS else FINE_SEEK_MS)
        Key.DirectionLeft -> Shortcut.Seek(if (shift) -COARSE_SEEK_MS else -FINE_SEEK_MS)
        Key.L -> Shortcut.Seek(MEDIUM_SEEK_MS)
        Key.J -> Shortcut.Seek(-MEDIUM_SEEK_MS)

        Key.DirectionUp -> Shortcut.Volume(VOLUME_STEP)
        Key.DirectionDown -> Shortcut.Volume(-VOLUME_STEP)
        Key.M -> Shortcut.Mute

        Key.N -> Shortcut.Next
        Key.P -> Shortcut.Previous
        Key.S -> Shortcut.Shuffle
        Key.R -> Shortcut.Repeat
        Key.F -> Shortcut.Like

        // The sidebar, in the order it is drawn.
        Key.One -> Shortcut.Go(Destination.HOME)
        Key.Two -> Shortcut.Go(Destination.SEARCH)
        Key.Three -> Shortcut.Go(Destination.LIBRARY)
        Key.Four -> Shortcut.Go(Destination.NOW_PLAYING)
        Key.Five -> Shortcut.Go(Destination.QUEUE)
        Key.Six -> Shortcut.Go(Destination.SETTINGS)

        // One key, two meanings: `?` is shift and slash on most layouts, so they are decided together
        // rather than as two branches, where the unshifted one would match first and the other would be
        // unreachable.
        Key.Slash -> if (shift) Shortcut.Help else Shortcut.Search
        else -> null
    }
}

/** Steps, in one place, so the help sheet and the bindings cannot disagree about what a press does. */
const val FINE_SEEK_MS = 5_000L
const val MEDIUM_SEEK_MS = 10_000L
const val COARSE_SEEK_MS = 30_000L
const val VOLUME_STEP = 0.05f

/** One row of the help sheet. */
data class ShortcutHelp(val keys: String, val what: String)

/** What `?` puts on the screen, grouped the way somebody would look for it. */
val shortcutHelp: List<Pair<String, List<ShortcutHelp>>> = listOf(
    "Playing" to listOf(
        ShortcutHelp("Space  ·  K", "Play or pause"),
        ShortcutHelp("N  ·  Ctrl →", "Next track"),
        ShortcutHelp("P  ·  Ctrl ←", "Previous track"),
        ShortcutHelp("← →", "Back or forward 5 seconds"),
        ShortcutHelp("Shift ← →", "Back or forward 30 seconds"),
        ShortcutHelp("J  ·  L", "Back or forward 10 seconds"),
        ShortcutHelp("S", "Shuffle"),
        ShortcutHelp("R", "Repeat"),
        ShortcutHelp("F", "Like the track that is playing"),
    ),
    "Sound" to listOf(
        ShortcutHelp("↑ ↓", "Volume"),
        ShortcutHelp("M", "Mute"),
    ),
    "Going places" to listOf(
        ShortcutHelp("1 … 6", "Home, Search, Library, Now playing, Queue, Settings"),
        ShortcutHelp("/  ·  Ctrl F", "Search"),
        ShortcutHelp("?", "This list"),
        ShortcutHelp("Esc", "Close what is open, or leave the box you are typing in"),
    ),
    "Anywhere on the machine" to listOf(
        ShortcutHelp("▶❚❚  ⏭  ⏮", "The media keys work while Noctorium is behind another window"),
    ),
)

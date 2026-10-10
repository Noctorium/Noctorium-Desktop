package app.noctorium.ui

import androidx.compose.ui.input.key.Key
import app.noctorium.core.Destination
import app.noctorium.settings.KeyChord
import app.noctorium.settings.ShortcutPreferences
import java.awt.event.KeyEvent as AwtKeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every binding, checked without a window.
 *
 * The mapping is a function of a key, its modifiers and the listener's changes precisely so this can exist: a
 * keyboard shortcut is the kind of thing that is wrong in one direction only -- it does nothing, or it does
 * something else -- and neither shows up until somebody presses it.
 */
class ShortcutsTest {

    private val windows = InAppShortcuts.defaults(mac = false)
    private val mac = InAppShortcuts.defaults(mac = true)

    private fun press(key: Key, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false, typing: Boolean = false, meta: Boolean = false, shortcuts: InAppShortcuts = windows) =
        shortcutFor(key, ctrl = ctrl, shift = shift, alt = alt, typing = typing, meta = meta, shortcuts = shortcuts)

    // --- What the keys do as they come ---

    @Test
    fun `space and K both pause, because everybody tries one of the two`() {
        assertEquals(Shortcut.PlayPause, press(Key.Spacebar))
        assertEquals(Shortcut.PlayPause, press(Key.K))
    }

    @Test
    fun `the arrows seek, and shift makes the step a coarse one`() {
        assertEquals(Shortcut.Seek(FINE_SEEK_MS), press(Key.DirectionRight))
        assertEquals(Shortcut.Seek(-FINE_SEEK_MS), press(Key.DirectionLeft))
        assertEquals(Shortcut.Seek(COARSE_SEEK_MS), press(Key.DirectionRight, shift = true))
        assertEquals(Shortcut.Seek(-COARSE_SEEK_MS), press(Key.DirectionLeft, shift = true))
    }

    @Test
    fun `J and L are the ten-second steps YouTube taught everybody`() {
        assertEquals(Shortcut.Seek(MEDIUM_SEEK_MS), press(Key.L))
        assertEquals(Shortcut.Seek(-MEDIUM_SEEK_MS), press(Key.J))
    }

    @Test
    fun `control and an arrow changes track rather than seeking`() {
        assertEquals(Shortcut.Next, press(Key.DirectionRight, ctrl = true))
        assertEquals(Shortcut.Previous, press(Key.DirectionLeft, ctrl = true))
    }

    @Test
    fun `the numbers go where the sidebar goes, in the order it is drawn, from the keypad too`() {
        assertEquals(Shortcut.Go(Destination.HOME), press(Key.One))
        assertEquals(Shortcut.Go(Destination.SEARCH), press(Key.Two))
        assertEquals(Shortcut.Go(Destination.LINK), press(Key.Three))
        assertEquals(Shortcut.Go(Destination.LIBRARY), press(Key.Four))
        assertEquals(Shortcut.Go(Destination.DOWNLOADS), press(Key.Five))
        assertEquals(Shortcut.Go(Destination.NOW_PLAYING), press(Key.Six))
        assertEquals(Shortcut.Go(Destination.QUEUE), press(Key.Seven))
        assertEquals(Shortcut.Go(Destination.SETTINGS), press(Key.Eight))
        assertEquals(Shortcut.Go(Destination.HOME), press(Key.NumPad1))
    }

    /** Ctrl+V plays a copied link, except in a box, where it has always meant putting the text there. */
    @Test
    fun `Ctrl V plays the link on the clipboard, but not while typing`() {
        assertEquals(Shortcut.PasteLink, press(Key.V, ctrl = true))
        assertNull(press(Key.V, ctrl = true, typing = true))
        assertNull(press(Key.V), "a plain V is not a paste")
    }

    /** Ctrl+F used to go to the Search page; it is the quick search bar now, and / still goes to the page. */
    @Test
    fun `Ctrl F is the quick search, slash the Search page and question mark the list`() {
        assertEquals(Shortcut.QuickSearch, press(Key.F, ctrl = true))
        assertEquals(Shortcut.QuickSearch, press(Key.F, ctrl = true, typing = true), "Ctrl+F works from inside a box too")
        assertEquals(Shortcut.Search, press(Key.Slash))
        assertEquals(Shortcut.Help, press(Key.Slash, shift = true))
    }

    @Test
    fun `on a Mac the commands are Command, so Command F is the quick search and Control F is nothing`() {
        assertEquals(Shortcut.QuickSearch, press(Key.F, meta = true, shortcuts = mac))
        assertNull(press(Key.F, ctrl = true, shortcuts = mac))
        assertEquals(Shortcut.PasteLink, press(Key.V, meta = true, shortcuts = mac))
        assertEquals(Shortcut.Next, press(Key.DirectionRight, meta = true, shortcuts = mac))
        assertEquals(Shortcut.Stop, press(Key.Period, meta = true, shortcuts = mac))
    }

    /**
     * The binding that matters most, because getting it wrong is not a missing feature but a broken
     * search box: somebody typing "no surprises" would pause four times and skip a track.
     */
    @Test
    fun `while typing, a letter is a letter and a space is a space`() {
        listOf(Key.Spacebar, Key.N, Key.K, Key.S, Key.One, Key.Slash, Key.DirectionLeft).forEach { key ->
            assertNull(press(key, typing = true), "$key while typing")
        }
        assertNull(press(Key.DirectionRight, shift = true, typing = true), "Shift and an arrow selects text in a box")
        assertNull(press(Key.DirectionUp, shift = true, typing = true))
    }

    @Test
    fun `the modified bindings still work mid-word, since none of them could be a character`() {
        assertEquals(Shortcut.Next, press(Key.DirectionRight, ctrl = true, typing = true))
        assertEquals(Shortcut.Previous, press(Key.DirectionLeft, ctrl = true, typing = true))
        assertEquals(Shortcut.Stop, press(Key.Period, ctrl = true, typing = true))
    }

    /** Getting out of something is most of what Escape is for, including out of the search box. */
    @Test
    fun `escape works whether or not the caret is in a field`() {
        assertEquals(Shortcut.Dismiss, press(Key.Escape))
        assertEquals(Shortcut.Dismiss, press(Key.Escape, typing = true))
    }

    @Test
    fun `a keyboard's own media keys are honoured, typing or not`() {
        assertEquals(Shortcut.PlayPause, press(Key.MediaPlayPause))
        assertEquals(Shortcut.PlayPause, press(Key.MediaPlayPause, typing = true))
        assertEquals(Shortcut.Next, press(Key.MediaNext, typing = true))
        assertEquals(Shortcut.Previous, press(Key.MediaPrevious, typing = true))
        assertEquals(Shortcut.Stop, press(Key.MediaStop))
    }

    @Test
    fun `the rest of the playing bindings, and the new ones`() {
        assertEquals(Shortcut.Next, press(Key.N))
        assertEquals(Shortcut.Previous, press(Key.P))
        assertEquals(Shortcut.Shuffle, press(Key.S))
        assertEquals(Shortcut.Repeat, press(Key.R))
        assertEquals(Shortcut.Like, press(Key.F))
        assertEquals(Shortcut.Mute, press(Key.M))
        assertEquals(Shortcut.Volume(VOLUME_STEP), press(Key.DirectionUp))
        assertEquals(Shortcut.Volume(-VOLUME_STEP), press(Key.DirectionDown))
        assertEquals(Shortcut.Stop, press(Key.Period, ctrl = true))
        assertEquals(Shortcut.StopAfterCurrent, press(Key.Period, ctrl = true, shift = true))
        assertEquals(Shortcut.Boost(BOOST_STEP_PERCENT), press(Key.DirectionUp, shift = true))
        assertEquals(Shortcut.Boost(-BOOST_STEP_PERCENT), press(Key.DirectionDown, shift = true))
    }

    @Test
    fun `alt is left to the window manager, and an action with no keys has none`() {
        assertNull(press(Key.Spacebar, alt = true))
        assertNull(press(Key.F, alt = true))
        assertTrue(windows.chordsOf(ShortcutAction.CLEAR_ALL_BUT_CURRENT).all { it == null }, "clearing the queue is not a key to brush")
        assertTrue(windows.chordsOf(ShortcutAction.TOGGLE_WINDOW).all { it == null })
    }

    @Test
    fun `a key that means nothing here means nothing`() {
        assertNull(press(Key.Q))
        assertNull(press(Key.Tab))
        assertNull(press(Key.Enter))
    }

    // --- Changed by the listener ---

    @Test
    fun `a changed shortcut takes the new keys and lets go of the old`() {
        val changed = InAppShortcuts.from(ShortcutPreferences(inApp = mapOf("stop" to "Ctrl+S", "clearAllButCurrent" to "Ctrl+Shift+Delete")), mac = false)
        assertEquals(Shortcut.Stop, press(Key.S, ctrl = true, shortcuts = changed))
        assertNull(press(Key.Period, ctrl = true, shortcuts = changed), "Stop's old keys do nothing now")
        assertEquals(Shortcut.ClearAllButCurrent, press(Key.Delete, ctrl = true, shift = true, shortcuts = changed))
        assertEquals(Shortcut.Shuffle, press(Key.S, shortcuts = changed), "S alone is still shuffle")
    }

    @Test
    fun `a blank chord takes a shortcut away, the second of an action's two places included`() {
        val changed = InAppShortcuts.from(ShortcutPreferences(inApp = mapOf("playPause.2" to "", "next.2" to "")), mac = false)
        assertNull(press(Key.K, shortcuts = changed))
        assertEquals(Shortcut.PlayPause, press(Key.Spacebar, shortcuts = changed))
        assertNull(press(Key.DirectionRight, ctrl = true, shortcuts = changed))
        assertEquals(listOf(KeyChord("N"), null), changed.chordsOf(ShortcutAction.NEXT))
    }

    @Test
    fun `a chord another action has is a conflict, and the first one listed keeps it until it is settled`() {
        val clash = InAppShortcuts.from(ShortcutPreferences(inApp = mapOf("stop" to "Ctrl+F")), mac = false)
        val chord = KeyChord("F", ctrl = true)
        assertEquals(listOf(ShortcutSlot(ShortcutAction.QUICK_SEARCH, 0)), clash.conflicts(chord, except = ShortcutSlot(ShortcutAction.STOP, 0)))
        assertEquals(setOf(ShortcutSlot(ShortcutAction.STOP, 0), ShortcutSlot(ShortcutAction.QUICK_SEARCH, 0)), clash.allConflicts()[chord]?.toSet())
        // Stop is listed before Quick search, so it is Stop until the listener chooses.
        assertEquals(Shortcut.Stop, press(Key.F, ctrl = true, shortcuts = clash))
        assertTrue(windows.conflicts(KeyChord("Q", ctrl = true)).isEmpty(), "a free chord clashes with nothing")
    }

    @Test
    fun `the shortcuts as they come have no conflicts, on Windows or on a Mac`() {
        assertEquals(emptyMap(), windows.allConflicts())
        assertEquals(emptyMap(), mac.allConflicts())
    }

    @Test
    fun `every default is a chord, and every action's stored names are its own`() {
        ShortcutAction.entries.forEach { action ->
            action.defaults.forEach { written ->
                assertNotNull(platformChord(written, mac = false), "$action's $written")
                assertNotNull(platformChord(written, mac = true), "$action's $written on a Mac")
            }
            assertTrue(action.defaults.size <= ShortcutAction.SLOTS)
        }
        val names = ShortcutAction.entries.flatMap { action -> (0 until ShortcutAction.SLOTS).map(action::storedName) }
        assertEquals(names.size, names.toSet().size)
        assertEquals(ShortcutAction.entries.size, ShortcutAction.entries.map { it.id }.toSet().size)
    }

    // --- Keys and how they are written ---

    @Test
    fun `Mod is Ctrl on Windows and Linux and Command on a Mac`() {
        assertEquals(KeyChord("F", ctrl = true), platformChord("Mod+F", mac = false))
        assertEquals(KeyChord("F", meta = true), platformChord("Mod+F", mac = true))
        assertEquals("Ctrl+Shift+Period", platformChord("Mod+Shift+Period", mac = false).toString())
    }

    @Test
    fun `a press becomes the chord it is, and keys with no name become none`() {
        assertEquals(KeyChord("Slash", shift = true), chordOf(Key.Slash, shift = true))
        assertEquals(KeyChord("Space", ctrl = true, alt = true), chordOf(Key.Spacebar, ctrl = true, alt = true))
        assertEquals(KeyChord("F13"), chordOf(Key(AwtKeyEvent.VK_F13)))
        assertEquals(KeyChord("F24"), chordOf(Key(AwtKeyEvent.VK_F24)))
        assertEquals(KeyChord("Quote"), chordOf(Key.Apostrophe))
        assertNull(chordOf(Key.ShiftLeft, shift = true), "a modifier alone is not a chord yet")
        assertTrue(isModifierKey(Key.CtrlRight))
        // Every key a chord can name comes back the same through KeyChord's own writing.
        listOf(Key.A, Key.Zero, Key.F12, Key.PageDown, Key.Grave, Key.MediaNext).forEach { key ->
            val chord = assertNotNull(chordOf(key, ctrl = true))
            assertEquals(chord, KeyChord.parse(chord.toString()))
        }
    }

    @Test
    fun `chords are written the way each platform writes them`() {
        val stop = KeyChord("Period", ctrl = true, shift = true)
        assertEquals("Ctrl+Shift+.", chordLabel(stop, mac = false, linux = false))
        assertEquals("⌃⇧.", chordLabel(stop, mac = true, linux = false))
        assertEquals("⇧⌘F", chordLabel(KeyChord("F", shift = true, meta = true), mac = true, linux = false))
        assertEquals("Ctrl+Alt+Space", chordLabel(KeyChord("Space", ctrl = true, alt = true), mac = false, linux = false))
        assertEquals("⌃⌥Space", chordLabel(KeyChord("Space", ctrl = true, alt = true), mac = true, linux = false))
        assertEquals("Alt+Win+→", chordLabel(KeyChord("Right", alt = true, meta = true), mac = false, linux = false))
        assertEquals("Alt+Super+→", chordLabel(KeyChord("Right", alt = true, meta = true), mac = false, linux = true))
        assertEquals(listOf("Ctrl", "Page Down"), chordParts(KeyChord("PageDown", ctrl = true), mac = false, linux = false))
        assertEquals("Esc", keyLabel("Escape", mac = false))
        assertEquals("⎋", keyLabel("Escape", mac = true))
    }

    @Test
    fun `a box keeps the chords it answers itself, and the rest still work in it`() {
        assertTrue(!worksWhileTyping(KeyChord("C", ctrl = true)))
        assertTrue(!worksWhileTyping(KeyChord("Z", meta = true)))
        assertTrue(!worksWhileTyping(KeyChord("K")))
        assertTrue(!worksWhileTyping(KeyChord("Left", shift = true)))
        assertTrue(worksWhileTyping(KeyChord("F", ctrl = true)))
        assertTrue(worksWhileTyping(KeyChord("C", ctrl = true, alt = true)))
        assertTrue(worksWhileTyping(KeyChord("F5")))
        assertTrue(worksWhileTyping(KeyChord("MediaPlayPause")))
    }

    // --- Global shortcuts ---

    @Test
    fun `a global chord has to be one no other program is using`() {
        val find = assertNotNull(globalRefusal(KeyChord("F", ctrl = true), mac = false, linux = false))
        assertTrue("Find" in find, find)
        assertNotNull(globalRefusal(KeyChord("F", meta = true), mac = true, linux = false), "⌘F is Find on a Mac")
        assertNotNull(globalRefusal(KeyChord("P"), mac = false, linux = false), "a key alone is typing")
        assertNotNull(globalRefusal(KeyChord("P", shift = true), mac = false, linux = false))
        assertNotNull(globalRefusal(KeyChord("S", ctrl = true), mac = false, linux = false), "Ctrl and a letter is a program's own command")
        assertNotNull(globalRefusal(KeyChord("Delete", ctrl = true, alt = true), mac = false, linux = false))
        assertNotNull(globalRefusal(KeyChord("F2", ctrl = true, alt = true), mac = false, linux = true), "Linux switches consoles with it")
        assertNotNull(globalRefusal(KeyChord("MediaPlayPause"), mac = false, linux = false), "the media keys already work")
        assertNotNull(globalRefusal(KeyChord("VolumeUp"), mac = false, linux = false))
        assertNull(globalRefusal(KeyChord("Space", ctrl = true, alt = true), mac = false, linux = false))
        assertNull(globalRefusal(KeyChord("F", ctrl = true, alt = true), mac = false, linux = false), "Ctrl+Alt+F is not Find")
        assertNull(globalRefusal(KeyChord("F13"), mac = false, linux = false), "nobody types with F13")
        assertNull(globalRefusal(KeyChord("MediaStop"), mac = false, linux = false))
    }

    @Test
    fun `every suggested global chord is allowed everywhere, and no two are the same`() {
        val suggested = ShortcutAction.entries.mapNotNull { action -> action.globalSuggestion?.let { action to it } }
        assertTrue(suggested.isNotEmpty())
        suggested.forEach { (action, written) ->
            assertTrue(action.global, "$action has a suggestion but is not offered globally")
            listOf(false to false, true to false, false to true).forEach { (mac, linux) ->
                val chord = assertNotNull(platformChord(written, mac), written)
                assertNull(globalRefusal(chord, mac, linux), "$written for $action, mac=$mac linux=$linux")
            }
        }
        assertEquals(suggested.size, suggested.map { it.second }.toSet().size)
        assertEquals("Ctrl+Alt+Space", ShortcutAction.QUICK_SEARCH.globalSuggestion, "the quick search's own chord from anywhere")
    }

    @Test
    fun `global bindings keep the ones bound and turn away the ones no longer allowed`() {
        val bound = globalBindings(
            ShortcutPreferences(global = mapOf("quickSearch" to "Ctrl+Alt+Space", "like" to "Ctrl+F", "stop" to "not a chord", "seekBack" to "Ctrl+Alt+B")),
            mac = false,
            linux = false,
        )
        // Like on Ctrl+F is refused, Stop's is no chord, and going back is not offered globally at all.
        assertEquals(mapOf(ShortcutAction.QUICK_SEARCH to KeyChord("Space", ctrl = true, alt = true)), bound)
    }

    // --- The help sheet ---

    /** The sheet is the only way anybody finds these without Settings, so it has to describe the ones that exist. */
    @Test
    fun `the help sheet lists every group from the keys as they are`() {
        val sheet = shortcutHelp(windows, mac = false)
        assertEquals(listOf("Playing", "Sound", "Going places", "Always", "Anywhere on the machine"), sheet.map { it.first })
        val rows = sheet.flatMap { it.second }
        assertTrue(rows.any { it.keys.contains("Space") }, "the first thing anybody tries is not listed")
        assertTrue(rows.any { it.what.contains("5 seconds") }, "the fine step is not described")
        assertTrue(rows.any { it.what.contains("30 seconds") }, "the coarse step is not described")
        assertTrue(rows.any { it.keys == "Ctrl+F" && it.what == "Quick search" })
        assertTrue(rows.all { it.keys.isNotBlank() && it.what.isNotBlank() })
        val changed = shortcutHelp(InAppShortcuts.from(ShortcutPreferences(inApp = mapOf("quickSearch" to "Ctrl+K")), mac = false), mac = false)
        assertTrue(changed.flatMap { it.second }.any { it.keys == "Ctrl+K" && it.what == "Quick search" }, "the sheet follows a changed key")
    }
}

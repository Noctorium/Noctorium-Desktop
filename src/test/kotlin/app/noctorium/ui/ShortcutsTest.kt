package app.noctorium.ui

import androidx.compose.ui.input.key.Key
import app.noctorium.core.Destination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every binding, checked without a window.
 *
 * The mapping is a function of a key and its modifiers precisely so this can exist: a keyboard shortcut is
 * the kind of thing that is wrong in one direction only -- it does nothing, or it does something else --
 * and neither shows up until somebody presses it.
 */
class ShortcutsTest {

    @Test
    fun `space and K both pause, because everybody tries one of the two`() {
        assertEquals(Shortcut.PlayPause, shortcutFor(Key.Spacebar))
        assertEquals(Shortcut.PlayPause, shortcutFor(Key.K))
    }

    @Test
    fun `the arrows seek, and shift makes the step a coarse one`() {
        assertEquals(Shortcut.Seek(FINE_SEEK_MS), shortcutFor(Key.DirectionRight))
        assertEquals(Shortcut.Seek(-FINE_SEEK_MS), shortcutFor(Key.DirectionLeft))
        assertEquals(Shortcut.Seek(COARSE_SEEK_MS), shortcutFor(Key.DirectionRight, shift = true))
        assertEquals(Shortcut.Seek(-COARSE_SEEK_MS), shortcutFor(Key.DirectionLeft, shift = true))
    }

    @Test
    fun `J and L are the ten-second steps YouTube taught everybody`() {
        assertEquals(Shortcut.Seek(MEDIUM_SEEK_MS), shortcutFor(Key.L))
        assertEquals(Shortcut.Seek(-MEDIUM_SEEK_MS), shortcutFor(Key.J))
    }

    @Test
    fun `control and an arrow changes track rather than seeking`() {
        assertEquals(Shortcut.Next, shortcutFor(Key.DirectionRight, ctrl = true))
        assertEquals(Shortcut.Previous, shortcutFor(Key.DirectionLeft, ctrl = true))
    }

    @Test
    fun `the numbers go where the sidebar goes, in the order it is drawn`() {
        assertEquals(Shortcut.Go(Destination.HOME), shortcutFor(Key.One))
        assertEquals(Shortcut.Go(Destination.SEARCH), shortcutFor(Key.Two))
        assertEquals(Shortcut.Go(Destination.LIBRARY), shortcutFor(Key.Three))
        assertEquals(Shortcut.Go(Destination.NOW_PLAYING), shortcutFor(Key.Four))
        assertEquals(Shortcut.Go(Destination.QUEUE), shortcutFor(Key.Five))
        assertEquals(Shortcut.Go(Destination.SETTINGS), shortcutFor(Key.Six))
    }

    /**
     * The one key with two meanings, and the reason they are decided in a single branch: written as two,
     * the unshifted one matches first and `?` never fires at all.
     */
    @Test
    fun `slash searches and question mark asks for the list`() {
        assertEquals(Shortcut.Search, shortcutFor(Key.Slash))
        assertEquals(Shortcut.Help, shortcutFor(Key.Slash, shift = true))
    }

    /**
     * The binding that matters most, because getting it wrong is not a missing feature but a broken
     * search box: somebody typing "no surprises" would pause four times and skip a track.
     */
    @Test
    fun `while typing, a letter is a letter and a space is a space`() {
        assertNull(shortcutFor(Key.Spacebar, typing = true))
        assertNull(shortcutFor(Key.N, typing = true))
        assertNull(shortcutFor(Key.K, typing = true))
        assertNull(shortcutFor(Key.S, typing = true))
        assertNull(shortcutFor(Key.One, typing = true))
        assertNull(shortcutFor(Key.Slash, typing = true))
        assertNull(shortcutFor(Key.DirectionLeft, typing = true))
    }

    @Test
    fun `the modified bindings still work mid-word, since none of them could be a character`() {
        assertEquals(Shortcut.Search, shortcutFor(Key.F, ctrl = true, typing = true))
        assertEquals(Shortcut.Next, shortcutFor(Key.DirectionRight, ctrl = true, typing = true))
        assertEquals(Shortcut.Previous, shortcutFor(Key.DirectionLeft, ctrl = true, typing = true))
    }

    /** Getting out of something is most of what Escape is for, including out of the search box. */
    @Test
    fun `escape works whether or not the caret is in a field`() {
        assertEquals(Shortcut.Dismiss, shortcutFor(Key.Escape))
        assertEquals(Shortcut.Dismiss, shortcutFor(Key.Escape, typing = true))
    }

    @Test
    fun `a keyboard's own media keys are honoured, typing or not`() {
        assertEquals(Shortcut.PlayPause, shortcutFor(Key.MediaPlayPause))
        assertEquals(Shortcut.PlayPause, shortcutFor(Key.MediaPlayPause, typing = true))
        assertEquals(Shortcut.Next, shortcutFor(Key.MediaNext, typing = true))
        assertEquals(Shortcut.Previous, shortcutFor(Key.MediaPrevious, typing = true))
    }

    @Test
    fun `the rest of the playing bindings`() {
        assertEquals(Shortcut.Next, shortcutFor(Key.N))
        assertEquals(Shortcut.Previous, shortcutFor(Key.P))
        assertEquals(Shortcut.Shuffle, shortcutFor(Key.S))
        assertEquals(Shortcut.Repeat, shortcutFor(Key.R))
        assertEquals(Shortcut.Like, shortcutFor(Key.F))
        assertEquals(Shortcut.Mute, shortcutFor(Key.M))
        assertEquals(Shortcut.Volume(VOLUME_STEP), shortcutFor(Key.DirectionUp))
        assertEquals(Shortcut.Volume(-VOLUME_STEP), shortcutFor(Key.DirectionDown))
    }

    @Test
    fun `alt is left to the window manager`() {
        assertNull(shortcutFor(Key.Spacebar, alt = true))
        assertNull(shortcutFor(Key.F, alt = true))
    }

    @Test
    fun `a key that means nothing here means nothing`() {
        assertNull(shortcutFor(Key.Q))
        assertNull(shortcutFor(Key.Tab))
        assertNull(shortcutFor(Key.Enter))
    }

    /** The sheet is the only way anybody finds these, so it has to describe the ones that exist. */
    @Test
    fun `the help sheet lists every group and says what the steps are`() {
        val rows = shortcutHelp.flatMap { it.second }
        assertEquals(listOf("Playing", "Sound", "Going places"), shortcutHelp.map { it.first })
        assertTrue(rows.any { it.keys.contains("Space") }, "the first thing anybody tries is not listed")
        assertTrue(rows.any { it.what.contains("5 seconds") }, "the fine step is not described")
        assertTrue(rows.any { it.what.contains("30 seconds") }, "the coarse step is not described")
        assertTrue(rows.all { it.keys.isNotBlank() && it.what.isNotBlank() })
    }

    private fun assertTrue(value: Boolean, message: String = "expected true") =
        kotlin.test.assertTrue(value, message)
}

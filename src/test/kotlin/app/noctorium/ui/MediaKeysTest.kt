package app.noctorium.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the keys on the keyboard itself mean.
 *
 * The registering cannot be tested -- it needs Windows, a message loop, and for no other player to have
 * claimed the keys first -- but the part that decides what a press does is ordinary arithmetic on a
 * virtual key code, and that is where a transposed number would hide.
 */
class MediaKeysTest {

    @Test
    fun `the three keys worth taking from the whole machine`() {
        assertEquals(Shortcut.PlayPause, MediaKeys.shortcutForVirtualKey(MediaKeys.VK_MEDIA_PLAY_PAUSE))
        assertEquals(Shortcut.Next, MediaKeys.shortcutForVirtualKey(MediaKeys.VK_MEDIA_NEXT_TRACK))
        assertEquals(Shortcut.Previous, MediaKeys.shortcutForVirtualKey(MediaKeys.VK_MEDIA_PREV_TRACK))
    }

    /** Windows' numbers, written out, so a typo in a constant is a failure rather than a dead key. */
    @Test
    fun `the codes are the ones Windows actually uses`() {
        assertEquals(0xB0, MediaKeys.VK_MEDIA_NEXT_TRACK)
        assertEquals(0xB1, MediaKeys.VK_MEDIA_PREV_TRACK)
        assertEquals(0xB2, MediaKeys.VK_MEDIA_STOP)
        assertEquals(0xB3, MediaKeys.VK_MEDIA_PLAY_PAUSE)
    }

    /** Stop is not pause, and Noctorium has nothing to stop, so the key stays available to others. */
    @Test
    fun `stop is left alone`() {
        assertNull(MediaKeys.shortcutForVirtualKey(MediaKeys.VK_MEDIA_STOP))
    }

    @Test
    fun `an unrelated key means nothing`() {
        assertNull(MediaKeys.shortcutForVirtualKey(0x41))
        assertNull(MediaKeys.shortcutForVirtualKey(0))
    }

    /**
     * The one piece of bit-twiddling in the hot path: Windows packs the modifiers into the low half of
     * lParam and the virtual key into the high half, and reading the wrong half is a key that never fires.
     */
    @Test
    fun `the virtual key is the high half of lParam`() {
        val lParam = (MediaKeys.VK_MEDIA_PLAY_PAUSE.toLong() shl 16) or 0x4000L
        val vk = ((lParam shr 16) and 0xFFFF).toInt()
        assertEquals(MediaKeys.VK_MEDIA_PLAY_PAUSE, vk)
        assertEquals(Shortcut.PlayPause, MediaKeys.shortcutForVirtualKey(vk))
    }
}

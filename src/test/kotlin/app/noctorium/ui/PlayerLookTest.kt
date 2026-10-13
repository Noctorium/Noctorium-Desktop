package app.noctorium.ui

import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.AudioAnalysisKind
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.playback.StreamFormat
import app.noctorium.settings.DesktopPreferences
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.PlayerButton
import app.noctorium.settings.QueueColumn
import app.noctorium.settings.VisualiserStyle
import app.noctorium.settings.withPlayerButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The decisions behind the desktop-music-player look: which buttons the Toolbar and the other bars draw now there
 * is a Stop, what the queue's table shows at a width, what its status bar says, how the visualiser moves, and when
 * the queue can be cleared down to the song playing.
 */
class PlayerLookTest {

    // --- Stop, and the Toolbar's buttons ---

    @Test
    fun `Stop waits to be asked for on every bar but the Toolbar`() {
        PlayerBarStyle.entries.filter { it != PlayerBarStyle.TOOLBAR }.forEach { style ->
            listOf(false, true).forEach { narrow ->
                assertFalse(PlayerButton.STOP in playerBarButtons(style, narrow, emptySet()), "$style draws Stop unasked (narrow: $narrow)")
            }
        }
        assertTrue(PlayerButton.STOP in playerBarButtons(PlayerBarStyle.TOOLBAR, narrow = false, emptySet()))
    }

    @Test
    fun `Stop asked for is drawn where the bar has its transport, and put away on the Toolbar it goes`() {
        val asked = DesktopPreferences().withPlayerButton(PlayerButton.STOP, shown = true)
        listOf(PlayerBarStyle.INLINE, PlayerBarStyle.STACKED, PlayerBarStyle.CENTERED, PlayerBarStyle.SLIM, PlayerBarStyle.TASKBAR).forEach { style ->
            assertTrue(PlayerButton.STOP in playerBarButtons(style, narrow = false, asked, emptySet()), "$style leaves out Stop asked for")
        }
        val putAway = DesktopPreferences().withPlayerButton(PlayerButton.STOP, shown = false)
        assertFalse(PlayerButton.STOP in playerBarButtons(PlayerBarStyle.TOOLBAR, narrow = false, putAway, emptySet()))
    }

    @Test
    fun `the Toolbar draws a desktop player's row, and only what is under way beside it`() {
        assertEquals(
            setOf(PlayerButton.STOP, PlayerButton.REPEAT, PlayerButton.SHUFFLE, PlayerButton.VOLUME),
            playerBarButtons(PlayerBarStyle.TOOLBAR, narrow = false, emptySet()),
        )
        val busy = setOf(PlayerButton.SLEEP_TIMER, PlayerButton.DEVICES, PlayerButton.SPEED)
        assertEquals(TOOLBAR_BUTTONS + busy, playerBarButtons(PlayerBarStyle.TOOLBAR, narrow = false, emptySet(), busy))
        // Tight, repeat and shuffle go; the transport's Stop and the volume stay.
        assertEquals(setOf(PlayerButton.STOP, PlayerButton.VOLUME), playerBarButtons(PlayerBarStyle.TOOLBAR, narrow = true, emptySet(), tight = true))
        // What was put away stays away there too.
        assertFalse(PlayerButton.SHUFFLE in playerBarButtons(PlayerBarStyle.TOOLBAR, narrow = false, setOf(PlayerButton.SHUFFLE)))
    }

    @Test
    fun `Toolbar 2 draws every button but Stop, which waits to be asked for, and gives up the ways elsewhere first`() {
        val everything = PlayerButton.entries.toSet() - PlayerButton.STOP
        assertEquals(everything, playerBarButtons(PlayerBarStyle.TOOLBAR_2, narrow = false, emptySet()))
        // Narrower, lyrics and the queue go, both only ways to other pages.
        assertEquals(everything - PlayerButton.LYRICS - PlayerButton.QUEUE, playerBarButtons(PlayerBarStyle.TOOLBAR_2, narrow = true, emptySet()))
        // Tight, the toggles, the timer and the speed too -- the timer kept while it counts down.
        assertEquals(
            setOf(PlayerButton.LIKE, PlayerButton.VOLUME, PlayerButton.DEVICES, PlayerButton.SLEEP_TIMER),
            playerBarButtons(PlayerBarStyle.TOOLBAR_2, narrow = true, emptySet(), setOf(PlayerButton.SLEEP_TIMER), tight = true),
        )
        // Stop asked for is there; anything put away is not.
        val asked = DesktopPreferences().withPlayerButton(PlayerButton.STOP, shown = true).withPlayerButton(PlayerButton.LIKE, shown = false)
        val drawn = playerBarButtons(PlayerBarStyle.TOOLBAR_2, narrow = false, asked, emptySet())
        assertTrue(PlayerButton.STOP in drawn)
        assertFalse(PlayerButton.LIKE in drawn)
    }

    // --- The queue's table ---

    private fun song(n: Int, album: String? = "An album", duration: Long? = 200_000) = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "song-$n",
        title = "A song $n",
        artists = listOf(Artist("someone", "Someone", ProviderType.YOUTUBE_MUSIC)),
        album = album?.let { Album("album", it, emptyList(), ProviderType.YOUTUBE_MUSIC) },
        durationMs = duration,
        sourceUrl = "https://example.invalid/$n",
    )

    @Test
    fun `a wide table draws every column chosen, in its order`() {
        val chosen = listOf(QueueColumn.DURATION, QueueColumn.TITLE, QueueColumn.SERVICE, QueueColumn.NUMBER)
        assertEquals(chosen, queueColumnsThatFit(chosen, 1_200f))
        assertEquals(QueueColumn.DEFAULT, queueColumnsThatFit(QueueColumn.DEFAULT, 900f))
    }

    @Test
    fun `a narrow table gives up the least needed columns first, and never the title`() {
        val all = QueueColumn.entries.toList()
        val narrower = queueColumnsThatFit(all, (leastWidth(all) - 1).toFloat())
        assertEquals(all - QueueColumn.SERVICE, narrower)
        val narrowest = queueColumnsThatFit(all, 10f)
        assertEquals(listOf(QueueColumn.TITLE), narrowest)
        // Whatever is left fits, down to the title alone.
        listOf(300f, 420f, 560f, 700f).forEach { width ->
            val columns = queueColumnsThatFit(all, width)
            assertTrue(QueueColumn.TITLE in columns)
            assertTrue(leastWidth(columns) <= width || columns == listOf(QueueColumn.TITLE), "$columns at $width")
        }
    }

    @Test
    fun `columns switched on go to the end, off come out, and the title stays`() {
        val columns = QueueColumn.DEFAULT
        assertEquals(columns + QueueColumn.SERVICE, queueColumnsToggled(columns, QueueColumn.SERVICE, on = true))
        assertEquals(columns - QueueColumn.ALBUM, queueColumnsToggled(columns, QueueColumn.ALBUM, on = false))
        // On again after off is the way to the end of the row.
        assertEquals(columns - QueueColumn.NUMBER + QueueColumn.NUMBER, queueColumnsToggled(columns - QueueColumn.NUMBER, QueueColumn.NUMBER, on = true))
        assertEquals(columns, queueColumnsToggled(columns, QueueColumn.TITLE, on = false))
    }

    @Test
    fun `each column says its part of the song`() {
        val track = song(3)
        assertEquals("3", cellText(track, 2, QueueColumn.NUMBER))
        assertEquals("A song 3", cellText(track, 2, QueueColumn.TITLE))
        assertEquals("Someone", cellText(track, 2, QueueColumn.ARTIST))
        assertEquals("An album", cellText(track, 2, QueueColumn.ALBUM))
        assertEquals("3:20", cellText(track, 2, QueueColumn.DURATION))
        assertEquals("YouTube Music", cellText(track, 2, QueueColumn.SERVICE))
        // What is not known is left blank rather than guessed.
        assertEquals("", cellText(song(4, album = null, duration = null), 0, QueueColumn.ALBUM))
        assertEquals("", cellText(song(4, album = null, duration = null), 0, QueueColumn.DURATION))
    }

    // --- The status bar ---

    @Test
    fun `the status bar says what the stream is, or why it cannot`() {
        val track = song(1)
        val playing = PlaybackState(status = PlaybackStatus.PLAYING, track = track, format = StreamFormat("opus", 48_000, 2, 128_000))
        assertEquals("Opus, stereo, 48 kHz, 128 kbit/s", statusBarFormat(playing, onSpotify = false))
        assertEquals("Playing in Spotify's own app", statusBarFormat(playing, onSpotify = true))
        assertEquals("Finding the song…", statusBarFormat(playing.copy(status = PlaybackStatus.RESOLVING), onSpotify = false))
        assertEquals("Stopped", statusBarFormat(PlaybackState(), onSpotify = false))
        assertEquals("", statusBarFormat(playing.copy(format = null), onSpotify = false))
    }

    @Test
    fun `the status bar's times are how far into the whole queue`() {
        val queue = QueueState(tracks = listOf(song(1), song(2), song(3)), currentIndex = 1)
        assertEquals("4:51 / 10:00", statusBarTimes(queue, 91_000))
        assertEquals("0:12 / --:--", statusBarTimes(QueueState(tracks = listOf(song(1, duration = null)), currentIndex = 0), 12_000))
    }

    // --- The visualiser ---

    @Test
    fun `a bar rises at once and falls no faster than it may`() {
        assertEquals(.8f, approach(.2f, .8f, .01f))
        assertEquals(.8f - VISUALISER_FALL_PER_SECOND * .1f, approach(.8f, 0f, .1f), .0001f)
        // Not past what was measured, however long it has been.
        assertEquals(.3f, approach(.8f, .3f, 10f))
    }

    @Test
    fun `the visualiser draws what the player can give`() {
        assertEquals(VisualiserStyle.BARS, visualiserShown(VisualiserStyle.BARS, AudioAnalysisKind.SPECTRUM))
        assertEquals(VisualiserStyle.LEVEL_METER, visualiserShown(VisualiserStyle.BARS, AudioAnalysisKind.LEVELS))
        assertEquals(VisualiserStyle.OFF, visualiserShown(VisualiserStyle.BARS, AudioAnalysisKind.NONE))
        assertEquals(VisualiserStyle.OFF, visualiserShown(VisualiserStyle.OFF, AudioAnalysisKind.SPECTRUM))
    }

    // --- The queue and the booster ---

    @Test
    fun `the queue is cleared down to the song playing only when there is more than it`() {
        assertFalse(canClearAllButCurrent(QueueState()))
        assertFalse(canClearAllButCurrent(QueueState(tracks = listOf(song(1)), currentIndex = 0)))
        assertFalse(canClearAllButCurrent(QueueState(tracks = listOf(song(1), song(2)), currentIndex = -1)))
        assertTrue(canClearAllButCurrent(QueueState(tracks = listOf(song(1), song(2)), currentIndex = 1)))
    }

    @Test
    fun `the booster is offered only where the player can reach the sound`() {
        assertTrue(boostAvailable(PlaybackState(), onSpotify = false))
        assertFalse(boostAvailable(PlaybackState(), onSpotify = true))
        assertFalse(boostAvailable(PlaybackState(volumeBoostAvailable = false), onSpotify = false))
    }
}

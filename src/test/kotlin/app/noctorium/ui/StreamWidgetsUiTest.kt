package app.noctorium.ui

import app.noctorium.widgets.StreamWidgetPreferences
import app.noctorium.widgets.StreamWidgetStatus
import app.noctorium.widgets.StreamWidgetTheme
import app.noctorium.widgets.StreamWidgetTheme.AFTERGLOW
import app.noctorium.widgets.StreamWidgetTheme.GLASS
import app.noctorium.widgets.StreamWidgetTheme.MINIMAL
import app.noctorium.widgets.StreamWidgetTheme.NEON
import app.noctorium.widgets.StreamWidgetTheme.TICKER
import app.noctorium.widgets.StreamWidgetTheme.VINYL
import app.noctorium.widgets.StreamWidgetTheme.WINDOWS_98
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the stream widgets' page and its tile say, worked out apart from drawing them. */
class StreamWidgetsUiTest {

    private val off = StreamWidgetPreferences()
    private val on = off.copy(enabled = true)
    private val running = StreamWidgetStatus.Running(4851, "http://127.0.0.1:4851")
    private val failed = StreamWidgetStatus.Failed(4851, "Port 4851 is in use by another program. Choose another port.")

    @Test
    fun `the tile says Off, or the default look and where the widgets are`() {
        assertEquals("Off", streamWidgetsSummary(off, StreamWidgetStatus.Off))
        // A server still stopping after the switch went off is off, as the switch says.
        assertEquals("Off", streamWidgetsSummary(off, running))
        assertEquals("On · Glass · 127.0.0.1:4851", streamWidgetsSummary(on, running))
        assertEquals("On · Vinyl · 127.0.0.1:4851", streamWidgetsSummary(on.copy(theme = VINYL), running))
        assertEquals(
            "On · Glass · 127.0.0.1:4851 · on the network too",
            streamWidgetsSummary(on.copy(allowNetwork = true), running.copy(networkAddress = "http://192.168.1.20:4851")),
        )
        assertEquals("Starting on port 5000…", streamWidgetsSummary(on.copy(port = 5000), StreamWidgetStatus.Starting(5000)))
        // Switched on a moment ago, before the server has heard: starting, not off.
        assertEquals("Starting on port 4851…", streamWidgetsSummary(on, StreamWidgetStatus.Off))
        assertEquals("On, but could not start on port 4851", streamWidgetsSummary(on, failed))
    }

    @Test
    fun `the status says what the server is doing, and a failure in the core's own words`() {
        assertEquals("Off. Nothing on this computer is listening for a streaming program.", streamWidgetsStatusLine(StreamWidgetStatus.Off))
        assertEquals("Starting on port 4851…", streamWidgetsStatusLine(StreamWidgetStatus.Starting(4851)))
        assertEquals("Running at http://127.0.0.1:4851", streamWidgetsStatusLine(running))
        assertEquals("Port 4851 is in use by another program. Choose another port.", streamWidgetsStatusLine(failed))
        assertEquals(StreamWidgetStatus.Starting(4851), streamWidgetsShown(on, StreamWidgetStatus.Off))
        assertEquals(StreamWidgetStatus.Off, streamWidgetsShown(off, failed))
        assertEquals(failed, streamWidgetsShown(on, failed))
    }

    @Test
    fun `a port no program could listen on is refused, saying why`() {
        assertNull(streamWidgetPortProblem("4851"))
        assertNull(streamWidgetPortProblem("1024"))
        assertNull(streamWidgetPortProblem("65535"))
        assertNull(streamWidgetPortProblem(" 5000 "))
        assertEquals("Type a port. Choose one from 1024 to 65535.", streamWidgetPortProblem(""))
        assertEquals("Type a port. Choose one from 1024 to 65535.", streamWidgetPortProblem("  "))
        assertEquals("A port below 1024 needs an administrator to listen on. Choose one from 1024 to 65535.", streamWidgetPortProblem("80"))
        assertEquals("A port below 1024 needs an administrator to listen on. Choose one from 1024 to 65535.", streamWidgetPortProblem("0"))
        assertEquals("Ports go no higher than 65535. Choose one from 1024 to 65535.", streamWidgetPortProblem("65536"))
        assertEquals("Ports go no higher than 65535. Choose one from 1024 to 65535.", streamWidgetPortProblem("99999999999"))
        assertEquals("A port is a whole number. Choose one from 1024 to 65535.", streamWidgetPortProblem("48.51"))
        // Whatever the box refuses, the core refuses too: nothing it lets through is one the core would not save.
        (0..70_000 step 7).forEach { port ->
            assertEquals(port in StreamWidgetPreferences.PORTS, streamWidgetPortProblem("$port") == null, "port $port")
        }
    }

    @Test
    fun `each look's address is where it is served, or where it will be`() {
        assertEquals("http://127.0.0.1:4851/widget/glass", streamWidgetAddress(off, StreamWidgetStatus.Off, GLASS))
        assertEquals("http://127.0.0.1:5000/widget/windows98", streamWidgetAddress(off.copy(port = 5000), StreamWidgetStatus.Off, WINDOWS_98))
        // A port a settings file holds that no program could listen on is not the one used, and not the one shown.
        assertEquals("http://127.0.0.1:4851/widget/neon", streamWidgetAddress(off.copy(port = 80), StreamWidgetStatus.Off, NEON))
        assertEquals(
            "http://127.0.0.1:6000/widget/afterglow",
            streamWidgetAddress(on, StreamWidgetStatus.Running(6000, "http://127.0.0.1:6000"), AFTERGLOW),
        )
    }

    @Test
    fun `the gallery gives the ticker a row of its own and leaves no row with a gap`() {
        assertEquals(listOf(listOf(GLASS, MINIMAL), listOf(VINYL, NEON), listOf(TICKER), listOf(AFTERGLOW, WINDOWS_98)), streamWidgetGalleryRows(2))
        assertEquals(StreamWidgetTheme.entries.map { listOf(it) }, streamWidgetGalleryRows(1))
        (1..4).forEach { columns ->
            val rows = streamWidgetGalleryRows(columns)
            assertEquals(StreamWidgetTheme.entries.toSet(), rows.flatten().toSet(), "$columns columns")
            assertEquals(StreamWidgetTheme.entries.size, rows.flatten().size, "$columns columns")
            assertTrue(rows.all { it.size <= columns }, "$columns columns: $rows")
        }
        assertEquals(listOf(TICKER), StreamWidgetTheme.entries.filter { it.runsTheWidth })
    }

    @Test
    fun `the size for OBS is the look's own, and the ticker's any width`() {
        assertEquals("640 × 168", streamWidgetSourceSize(GLASS))
        assertEquals("360 × 500", streamWidgetSourceSize(AFTERGLOW))
        assertEquals("1280 × 44, or any width", streamWidgetSourceSize(TICKER))
    }

    @Test
    fun `the network option says where another computer goes, once it is allowed`() {
        assertNull(streamWidgetNetworkLine(on, running))
        val allowed = on.copy(allowNetwork = true)
        val line = assertNotNull(streamWidgetNetworkLine(allowed, running.copy(networkAddress = "http://192.168.1.20:4851")))
        assertTrue("http://192.168.1.20:4851" in line, line)
        assertEquals(
            "This computer is not on a network just now, so there is no address for another computer to use.",
            streamWidgetNetworkLine(allowed, running),
        )
        assertEquals("Once the widgets are running, the address for another computer is shown here.", streamWidgetNetworkLine(allowed, StreamWidgetStatus.Starting(4851)))
    }

    @Test
    fun `the gallery says its addresses lead nowhere yet only while they do`() {
        assertTrue(streamWidgetAddressNote(off, StreamWidgetStatus.Off)!!.contains("once stream widgets are switched on"))
        assertTrue(streamWidgetAddressNote(on, failed)!!.contains("once the widgets have started"))
        assertNull(streamWidgetAddressNote(on, running))
        assertNull(streamWidgetAddressNote(on, StreamWidgetStatus.Starting(4851)))
        assertEquals("Hide after 1 second of pause", streamWidgetHideAfterLabel(1))
        assertEquals("Hide after 5 seconds of pause", streamWidgetHideAfterLabel(5))
    }
}

package app.noctorium.core

import app.noctorium.desktopAppState
import app.noctorium.settings.SettingsRepository
import app.noctorium.widgets.StreamWidgetPreferences
import app.noctorium.widgets.StreamWidgetStatus
import app.noctorium.widgets.StreamWidgetTheme
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The stream widgets switched on as their page in Settings switches them on, in the application state the desktop
 * builds: the server starts, serves a look, stops when they are switched off, and stops when Noctorium quits.
 *
 * Nothing the desktop does at launch or on the way out is specific to the widgets -- the state starts their watcher
 * itself, and quitting closes the state, which closes their server -- so this is the whole of their wiring, checked.
 * On a port the system has just said is free rather than the widgets' own, which may be in use on the machine running
 * the tests; and the settings it changed are put back, in this run's test folder, before it ends.
 */
class StreamWidgetsStartTest {

    @Test
    fun `switched on from Settings, the desktop's state serves the widgets, and stops on being switched off or quitting`() {
        val port = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { it.localPort }
        val state = desktopAppState()
        try {
            assertEquals(StreamWidgetStatus.Off, state.streamWidgets.value, "off until switched on")
            assertTrue(state.setStreamWidgetPort(port))
            state.setStreamWidgetsEnabled(true)
            val running = awaitRunning(state)
            assertEquals("http://127.0.0.1:$port", running.address)
            assertEquals(200, status(running.url(StreamWidgetTheme.AFTERGLOW)))

            state.setStreamWidgetsEnabled(false)
            await("switched off") { state.streamWidgets.value == StreamWidgetStatus.Off }
            assertFalse(listening(port), "still listening once switched off")

            state.setStreamWidgetsEnabled(true)
            awaitRunning(state)
            assertTrue(listening(port))
            // Writes of the settings changed above, so none of them lands after the ones put back below.
            Thread.sleep(500)
        } finally {
            state.close()
        }
        assertFalse(listening(port), "still listening after Noctorium quit")

        // As this run's test folder had them, so the next state built from it starts no server of its own.
        val settings = SettingsRepository()
        settings.save(settings.load().copy(streamWidgets = StreamWidgetPreferences()))
        assertEquals(StreamWidgetPreferences(), settings.load().streamWidgets)
    }

    private fun awaitRunning(state: AppState): StreamWidgetStatus.Running {
        await("running") { state.streamWidgets.value is StreamWidgetStatus.Running || state.streamWidgets.value is StreamWidgetStatus.Failed }
        return state.streamWidgets.value as? StreamWidgetStatus.Running ?: fail("did not start: ${state.streamWidgets.value}")
    }

    private fun await(what: String, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (!condition()) {
            if (System.currentTimeMillis() > until) fail("never $what")
            Thread.sleep(25)
        }
    }

    private fun status(url: String): Int {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000
        return try {
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private fun listening(port: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 1_000) }
    }.isSuccess
}

package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.desktopAppState
import app.noctorium.remote.RemoteControlPreferences
import app.noctorium.remote.RemoteControlStatus
import app.noctorium.settings.ThemePreset
import app.noctorium.ui.skins.Surface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The remote control's page in Settings: what it says, and -- with a folder named -- pictures of it off, starting,
 * running and failed, in Night, Day and 98:
 *
 *     ./gradlew :test --tests "*RemoteControlRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class RemoteControlRenderCheck {
    private val root: File? = System.getProperty("noctorium.renderBars")?.let { File(it, "remote-control") }
    private val on = RemoteControlPreferences(enabled = true)

    @Test
    fun `the page says what the server is doing, and never the opposite of its switch`() {
        assertEquals("Off", remoteControlSummary(RemoteControlPreferences(), RemoteControlStatus.Running(6932)))
        assertEquals("Starting on port 6932…", remoteControlSummary(on, RemoteControlStatus.Off))
        assertEquals("On · localhost:6932", remoteControlSummary(on, RemoteControlStatus.Running(6932)))
        assertEquals("On, but could not start on port 6932", remoteControlSummary(on, RemoteControlStatus.Failed(6932, "in use")))
        assertEquals("Running at http://localhost:7000", remoteControlStatusLine(RemoteControlStatus.Running(7000)))
        assertNull(remoteControlPortProblem("6932"))
        assertTrue(remoteControlPortProblem("80")!!.contains("administrator"))
        assertTrue(remoteControlPortProblem("")!!.startsWith("Type a port"))
        assertTrue(remoteControlPortProblem("99999")!!.startsWith("Ports go no higher"))
    }

    @Test
    fun `the settings search finds it`() {
        val found = SettingsIndex.settings.filter { it.page == SettingsPage.REMOTE_CONTROL }
        assertTrue(found.any { it.anchor == REMOTE_CONTROL_SWITCH }, "the switch is not in the settings search")
        assertTrue(found.any { it.anchor == REMOTE_CONTROL_PORT }, "the port is not in the settings search")
        assertTrue(found.any { "6932" in it.entry.keywords || "localhost" in it.entry.keywords }, "nobody finds it by its address")
    }

    @Test
    fun `the page draws in every state`() {
        val root = root ?: return
        val state = desktopAppState()
        try {
            listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY, ThemePreset.WINDOWS_98).forEach { theme ->
                val folder = File(root, theme.name.lowercase()).also { it.mkdirs() }
                listOf(
                    "off" to (RemoteControlPreferences() to RemoteControlStatus.Off),
                    "starting" to (on to RemoteControlStatus.Starting(6932)),
                    "running" to (on to RemoteControlStatus.Running(6932)),
                    "failed" to (on.copy(port = 7001) to RemoteControlStatus.Failed(7001, "Port 7001 is in use by another program. Choose another port.")),
                ).forEach { (name, setup) ->
                    draw(File(folder, "$name.png"), theme, 820, 1300) { RemoteControlSettingsPanel(setup.first, setup.second, state) }
                }
            }
        } finally {
            state.close()
        }
    }

    private fun draw(file: File, theme: ThemePreset, width: Int, height: Int, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width, height, Density(1f)) {
            BadgeTheme(theme) {
                CompositionLocalProvider(LocalMotion provides false) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.padding(20.dp)) { content() }
                    }
                }
            }
        }
        try {
            var image = scene.render(0)
            repeat(4) { frame ->
                Thread.sleep(40)
                image = scene.render((frame + 1) * 100_000_000L)
            }
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for ${file.name}")
        } finally {
            scene.close()
        }
    }
}

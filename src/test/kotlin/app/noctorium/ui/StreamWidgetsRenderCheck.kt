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
import app.noctorium.settings.ThemePreset
import app.noctorium.ui.skins.Surface
import app.noctorium.widgets.StreamWidgetPreferences
import app.noctorium.widgets.StreamWidgetStatus
import app.noctorium.widgets.StreamWidgetTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the stream widgets' page in Settings to pictures, off screen, for looking at: off, starting, running, failed
 * on a port another program has, and running for the network -- and once with "More" open, as the search opens it --
 * in Night, Day, 98 and XP, at a window's usual width and a narrow one. Off unless a folder is named, like
 * [PlayerBarRenderCheck]; the pictures go to `stream-widgets/` under it:
 *
 *     ./gradlew :test --tests "*StreamWidgetsRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * Every status is put straight into what is drawn, so no server listens anywhere while it draws, and nothing on the
 * page is pressed: the state it is handed is only there for the buttons that are never clicked.
 */
class StreamWidgetsRenderCheck {

    private val root: File? = System.getProperty("noctorium.renderBars")?.let { File(it, "stream-widgets") }

    private val themes = listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY, ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_XP)

    /** A window's usual width for Settings' pages, and one narrow enough that the looks go one to a row. */
    private val widths = listOf("wide" to 820, "narrow" to 560)

    private val on = StreamWidgetPreferences(enabled = true)

    /** The page in each state worth seeing, by the name its picture is given. */
    private val states: List<Triple<String, StreamWidgetPreferences, StreamWidgetStatus>> = listOf(
        Triple("off", StreamWidgetPreferences(), StreamWidgetStatus.Off),
        Triple("starting", on, StreamWidgetStatus.Starting(4851)),
        Triple(
            "running",
            on.copy(theme = StreamWidgetTheme.AFTERGLOW, hideWhenPaused = false),
            StreamWidgetStatus.Running(4851, "http://127.0.0.1:4851"),
        ),
        Triple(
            "failed",
            on.copy(port = 4851),
            StreamWidgetStatus.Failed(4851, "Port 4851 is in use by another program. Choose another port."),
        ),
        Triple(
            "network",
            on.copy(allowNetwork = true, port = 5120, showCover = false),
            StreamWidgetStatus.Running(5120, "http://127.0.0.1:5120", networkAddress = "http://192.168.1.20:5120"),
        ),
    )

    @Test
    fun `the stream widgets' page draws in every state, in each theme, wide and narrow`() {
        val root = root ?: return
        val state = desktopAppState()
        try {
            themes.forEach { theme ->
                val folder = File(root, theme.name.lowercase()).also { it.mkdirs() }
                widths.forEach { (width, pixels) ->
                    val height = if (width == "wide") 2900 else 4900
                    states.forEach { (name, widgets, status) ->
                        draw(File(folder, "$name-$width.png"), theme, pixels, height) {
                            StreamWidgetsSettingsPanel(widgets, status, state)
                        }
                    }
                    // "More" open, as the settings search opens it when it finds what is folded there.
                    draw(File(folder, "more-$width.png"), theme, pixels, height) {
                        CompositionLocalProvider(LocalSettingsAnchors provides SettingsAnchors(STREAM_WIDGET_OVERRIDES, 1)) {
                            StreamWidgetsSettingsPanel(on, StreamWidgetStatus.Running(4851, "http://127.0.0.1:4851"), state)
                        }
                    }
                }
                // Where the page is on the list of Settings, with its tile, as the test folder's settings have it: off.
                draw(File(folder, "settings-home.png"), theme, 1100, 1700) { SettingsScreen(state) }
            }
        } finally {
            state.close()
        }
    }

    /** [content] in [theme]'s colours, skin and type, on its page colour, a few frames in. */
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

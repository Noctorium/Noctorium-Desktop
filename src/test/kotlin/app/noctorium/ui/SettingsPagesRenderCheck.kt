package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.desktopAppState
import app.noctorium.settings.SettingsState
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test

/**
 * Draws the Startup and tray, Spotify and Lyrics pages to pictures, off screen, for looking at -- the last
 * under a crimson theme, so the new red on black is seen too. Off unless a folder is named, like
 * [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*SettingsPagesRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class SettingsPagesRenderCheck {

    @Test
    fun `the new settings pages draw`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val state = desktopAppState()
        try {
            render(folder, "startup.png", state) { settings -> StartupSettingsPanel(settings.preferences, state) }
            render(folder, "spotify.png", state, height = 900) { settings -> SpotifySettingsPanel(settings, state) }
            state.setTheme(ThemePreset.CRIMSON_SCARLET)
            render(folder, "lyrics-scarlet.png", state) { settings -> LyricsSettingsPanel(settings.preferences, state) }
        } finally {
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.close()
        }
    }

    private fun render(
        folder: File,
        name: String,
        state: AppState,
        height: Int = 620,
        content: @Composable (SettingsState) -> Unit,
    ) {
        val scene = ImageComposeScene(760, height, Density(1f)) {
            val settings = state.settings.collectAsState().value
            val preferences = settings.preferences
            MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                // A Surface rather than a painted Box, for the writing colour the application's own gives
                // everything inside it; without one every unstyled label draws black on black.
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(20.dp)) { content(settings) }
                }
            }
        }
        try {
            // Twice: the first frame lays out, the second has what the first one's state changes asked for.
            scene.render()
            File(folder, name).writeBytes(scene.render().encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }
}

package app.noctorium.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import app.noctorium.account.AccountStats
import app.noctorium.desktopAppState
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test

/**
 * Draws Stats to a picture, off screen, from the service's answer for invented listening (`account-stats.json`, from a
 * local stats service seeded by its `scripts/seed-local.mjs`). Off unless a folder is named:
 *
 *     ./gradlew :test --tests "*StatsRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class StatsRenderCheck {

    @Test
    fun `stats draw`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val full = AccountStats.parse(javaClass.getResource("/account-stats.json")!!.readText())!!
        val state = desktopAppState()
        try {
            val scene = ImageComposeScene(1200, 1500, Density(1f)) {
                val preferences = state.settings.collectAsState().value.preferences
                MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                    // The window's Surface, which gives the text its colour.
                    androidx.compose.material3.Surface(Modifier.fillMaxSize()) { StatsScreen(state, load = { full }) }
                }
            }
            try {
                repeat(5) { scene.render(it * 100_000_000L) }
                File(folder, "stats.png").writeBytes(scene.render(1_000_000_000L).encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        } finally {
            state.close()
        }
    }
}

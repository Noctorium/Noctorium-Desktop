package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.desktopAppState
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.settings.themeGlow
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Draws the secret Shadow theme to pictures, off screen, for looking at: Settings › Experimental under it, with a
 * picture behind, and the takeover the code starts with, early and late -- and checks the takeover ends of itself
 * after its five seconds. Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*ShadowRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * A `shadow-test-picture.png` in that folder is put behind the page.
 */
class ShadowRenderCheck {

    @Test
    fun `the Shadow theme and its takeover draw`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val picture = File(folder, "shadow-test-picture.png").takeIf { it.isFile }?.let { file ->
            org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
        }
        val state = desktopAppState()
        // The tests share one settings file: the theme as it was is put back after, or the next test meets the secret.
        val themeBefore = state.settings.value.preferences.theme
        try {
            state.unlockShadowTheme()
            assertEquals(ThemePreset.SHADOW, state.settings.value.preferences.theme)
            val page = ImageComposeScene(1000, 640, Density(1f)) {
                val settings = state.settings.collectAsState().value
                val preferences = settings.preferences
                MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                    CompositionLocalProvider(
                        LocalPageGlow provides preferences.themeGlow?.let { PageGlow(it, Color(it.colour)) },
                        LocalShadowBackdrop provides ShadowBackdrop(picture),
                    ) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pageGlow().padding(20.dp)) {
                            ExperimentalSettingsPanel(preferences, state)
                        }
                    }
                }
            }
            try {
                File(folder, "shadow-experimental.png").writeBytes(page.render().encodeToData()!!.bytes)
            } finally {
                page.close()
            }

            var done = false
            val takeover = ImageComposeScene(1000, 640, Density(1f)) { ShadowTakeover { done = true } }
            try {
                val started = System.currentTimeMillis()
                fun renderAt(millis: Long, name: String) {
                    while (System.currentTimeMillis() - started < millis) {
                        takeover.render((System.currentTimeMillis() - started) * 1_000_000)
                        Thread.sleep(16)
                    }
                    File(folder, name).writeBytes(takeover.render((System.currentTimeMillis() - started) * 1_000_000).encodeToData()!!.bytes)
                }
                renderAt(800, "shadow-takeover-early.png")
                assertTrue(!done, "still going after a moment")
                renderAt(4_500, "shadow-takeover-late.png")
                assertTrue(!done, "still going before five seconds")
                renderAt(5_600, "shadow-takeover-over.png")
                assertTrue(done, "over, and Noctorium told to close, after five seconds")
            } finally {
                takeover.close()
            }
        } finally {
            state.setTheme(themeBefore)
            Thread.sleep(500)
            state.close()
        }
    }
}

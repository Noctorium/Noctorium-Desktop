package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import app.noctorium.domain.ProviderType
import app.noctorium.domain.ServiceMarks
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.ServiceBadgeStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.settings.themeSkin
import app.noctorium.settings.windows98Palette
import app.noctorium.ui.icons.markPath
import app.noctorium.ui.icons.markVector
import app.noctorium.ui.icons.separatedArcFlags
import app.noctorium.ui.skins.SkinnedMaterialTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The service badges drawn with marks: every service's mark is a shape that fills its square without leaving it, and
 * a badge with a mark is exactly as tall as the same badge with letters, under every skin and at a bigger text size --
 * so that changing the setting moves nothing on the page.
 */
class ServiceBadgeTest {

    @Test
    fun `every service's mark is drawn on its square and fills it across or down`() {
        ProviderType.entries.forEach { provider ->
            // The text the vector is built from, which is Base's with its arcs' flags written out.
            val bounds = PathParser().parsePathString(markPath(provider)).toPath().getBounds()
            val square = 0f - EDGE..ServiceMarks.VIEWPORT + EDGE
            assertTrue(bounds.left in square && bounds.right in square && bounds.top in square && bounds.bottom in square, "$provider's mark leaves its square: $bounds")
            // Simple Icons' marks run to the square's edges; Material's folder keeps Material's margin of two.
            assertTrue(maxOf(bounds.width, bounds.height) >= ServiceMarks.VIEWPORT - 4, "$provider's mark is lost in its square: $bounds")
            assertTrue(provider.markVector().root.size > 0, "$provider's mark draws nothing")
        }
    }

    @Test
    fun `an arc's flags are written apart from what follows them, and nothing else about a path changes`() {
        // Simple Icons' way of writing an arc, as Apple Music's mark has it: the two flags run into the next figure.
        assertEquals("M23.994 6.124a9.23 9.23 0 0 0 -.24 -2.19c-.317 -1.31", separatedArcFlags("M23.994 6.124a9.23 9.23 0 00-.24-2.19c-.317-1.31"))
        assertEquals("a1 1 0 1 1 2 2 1 1 0 0 1 3 3", separatedArcFlags("a1 1 0 112 2 1 1 0 013 3"))
        // Two points in a row are two numbers; an exponent stays with its number.
        assertEquals("l.5 .5 1e-3 2", separatedArcFlags("l.5.5 1e-3,2"))
        // A path with nothing compact in it draws exactly as it did.
        listOf(ProviderType.YOUTUBE_VIDEO, ProviderType.TIDAL).forEach { provider ->
            assertEquals(
                PathParser().parsePathString(ServiceMarks.path(provider)).toPath().getBounds(),
                PathParser().parsePathString(markPath(provider)).toPath().getBounds(),
            )
        }
    }

    @Test
    fun `a badge with a mark is as tall as one with letters, under every skin and at any text size`() {
        listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_XP).forEach { preset ->
            listOf(1f, 1.3f).forEach { fontScale ->
                listOf(true, false).forEach { compact ->
                    ProviderType.entries.forEach { provider ->
                        val letters = measure(preset, fontScale) {
                            CompositionLocalProvider(LocalServiceBadgeStyle provides ServiceBadgeStyle.LETTERS) { ProviderBadge(provider, compact) }
                        }
                        val marked = measure(preset, fontScale) { ProviderBadge(provider, compact) }
                        assertEquals(letters.height, marked.height, "$provider's ${if (compact) "compact " else ""}badge in $preset at $fontScale")
                    }
                }
            }
        }
    }

    /** The size [content] is laid out at, drawn off screen in [preset] with the text [fontScale] times its size. */
    private fun measure(preset: ThemePreset, fontScale: Float, content: @Composable () -> Unit): IntSize {
        var size = IntSize.Zero
        val scene = ImageComposeScene(400, 120, Density(1f, fontScale)) {
            BadgeTheme(preset) { Box(Modifier.onSizeChanged { size = it }) { content() } }
        }
        try {
            scene.render()
        } finally {
            scene.close()
        }
        assertTrue(size.height > 0, "nothing was laid out")
        return size
    }

    private companion object {
        /** How far past its square a mark's bounds may reach: the curves' handles, which the shape itself does not. */
        const val EDGE = 0.5f
    }
}

/** [preset]'s colours, skin and type, as the window draws in them, for drawing a piece of it off screen. */
@Composable
internal fun BadgeTheme(preset: ThemePreset, content: @Composable () -> Unit) {
    val preferences = NoctoriumPreferences(theme = preset)
    SkinnedMaterialTheme(
        preferences.themeSkin,
        preferences.themeColours(),
        Color(preferences.resolvedAccent(null)),
        preferences.font,
        preferences.cornerStyle,
        preferences.windows98Palette,
        content,
    )
}

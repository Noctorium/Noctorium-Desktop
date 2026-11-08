package app.noctorium.ui

import androidx.compose.ui.graphics.Color
import app.noctorium.settings.ThemePreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Glass theme's page, panels and cards as drawn over the desktop. */
class SeeThroughThemeTest {
    private val glass = ThemePreset.GLASS.colours!!

    @Test
    fun `glazing changes only how much of each surface is drawn, never its colour or the writing`() {
        val glazed = glass.glazed()
        listOf(glass.background to glazed.background, glass.panel to glazed.panel, glass.card to glazed.card).forEach { (solid, see) ->
            assertEquals(solid and 0xFFFFFF, see and 0xFFFFFF)
            assertTrue((see ushr 24) < 0xFF, "a glazed surface lets something through")
        }
        assertEquals(glass.text, glazed.text)
        assertEquals(glass.subtext, glazed.subtext)
        assertEquals(glass.accent, glazed.accent)
    }

    @Test
    fun `writing sits on the more frosted surfaces`() {
        val glazed = glass.glazed()
        assertTrue((glazed.background ushr 24) < (glazed.panel ushr 24))
        assertTrue((glazed.panel ushr 24) < (glazed.card ushr 24))
    }

    @Test
    fun `the colour scheme carries the glass through to Material's surfaces`() {
        val scheme = noctoriumColorScheme(glass.glazed(), Color(glass.accent))
        assertTrue(scheme.background.alpha < 1f)
        assertTrue(scheme.surfaceContainer.alpha < 1f)
        assertEquals(1f, scheme.onSurface.alpha, "the writing stays solid")
        assertEquals(1f, scheme.primary.alpha, "and so does the accent")
    }

    @Test
    fun `glass nothing blurs is drawn more solid than glass the system blurs`() {
        val blurred = glass.glazed(blurred = true)
        val clear = glass.glazed(blurred = false)
        listOf(blurred.background to clear.background, blurred.panel to clear.panel, blurred.card to clear.card).forEach { (soft, sharp) ->
            assertTrue((soft ushr 24) < (sharp ushr 24))
        }
    }

    @Test
    fun `a window that cannot be see-through is left alone`() {
        // No window at all, as in a render check, is the commonest case of all.
        assertEquals(SeeThroughWindow.Glazing.NONE, SeeThroughWindow.apply(null, on = true))
    }
}

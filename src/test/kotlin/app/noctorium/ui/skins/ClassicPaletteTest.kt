package app.noctorium.ui.skins

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.ThemeSkin
import app.noctorium.settings.Windows98Colours
import app.noctorium.settings.Windows98Palette
import app.noctorium.settings.windows98Palette
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The 98 skin drawn from a palette: 98's own, which must come out as exactly the colours 98 shipped, and
 * Noctorium 98's, which must come out as its own numbers and still be readable everywhere 98 put writing.
 *
 * The pictures show the rest; these pin the mapping the pictures depend on, so a colour read from the wrong role
 * or a role left on 98's grey is caught without anybody having to look.
 */
class ClassicPaletteTest {

    @AfterTest
    fun `back to 98's own`() {
        Classic.palette = Windows98Palette.STANDARD
    }

    @Test
    fun `98's own palette draws exactly the colours 98 shipped`() {
        Classic.palette = Windows98Palette.STANDARD
        assertEquals(Color(Windows98Colours.FACE), Classic.Face)
        assertEquals(Color(Windows98Colours.HIGHLIGHT), Classic.Highlight)
        assertEquals(Color(Windows98Colours.LIGHT), Classic.Light)
        assertEquals(Color(Windows98Colours.SHADOW), Classic.Shadow)
        assertEquals(Color(Windows98Colours.DARK_SHADOW), Classic.DarkShadow)
        assertEquals(Color(Windows98Colours.WINDOW), Classic.Window)
        assertEquals(Color(Windows98Colours.TEXT), Classic.Text)
        assertEquals(Color(Windows98Colours.GREY_TEXT), Classic.GreyText)
        assertEquals(Color(Windows98Colours.SELECTION), Classic.Selection)
        assertEquals(Color(Windows98Colours.SELECTION_TEXT), Classic.SelectionText)
        assertEquals(Color(Windows98Colours.TITLE), Classic.Title)
        assertEquals(Color(Windows98Colours.TITLE_END), Classic.TitleEnd)
        assertEquals(Color(Windows98Colours.INACTIVE_TITLE), Classic.InactiveTitle)
        assertEquals(Color(Windows98Colours.INACTIVE_TITLE_END), Classic.InactiveTitleEnd)
        assertEquals(Color(Windows98Colours.TITLE_TEXT), Classic.TitleText)
        assertEquals(Color(Windows98Colours.TOOLTIP), Classic.Tooltip)
        assertEquals(Color(Windows98Colours.DESKTOP), Classic.Desktop)
    }

    @Test
    fun `the roles 98 filled with a pale grey are that grey under 98 itself`() {
        Classic.palette = Windows98Palette.STANDARD
        // An inactive title was written in the face's grey, and the display's quieter writing in the bevel's.
        assertEquals(Classic.Face, Classic.InactiveTitleText)
        assertEquals(Classic.Light, Classic.PaleText)
    }

    @Test
    fun `Noctorium 98 is drawn in its own colours`() {
        val night = Windows98Palette.NOCTORIUM
        Classic.palette = night
        assertEquals(Color(night.face), Classic.Face)
        assertEquals(Color(night.highlight), Classic.Highlight)
        assertEquals(Color(night.light), Classic.Light)
        assertEquals(Color(night.shadow), Classic.Shadow)
        assertEquals(Color(night.darkShadow), Classic.DarkShadow)
        assertEquals(Color(night.window), Classic.Window)
        assertEquals(Color(night.text), Classic.Text)
        assertEquals(Color(night.greyText), Classic.GreyText)
        assertEquals(Color(night.selection), Classic.Selection)
        assertEquals(Color(night.selectionText), Classic.SelectionText)
        assertEquals(Color(night.title), Classic.Title)
        assertEquals(Color(night.titleEnd), Classic.TitleEnd)
        assertEquals(Color(night.inactiveTitle), Classic.InactiveTitle)
        assertEquals(Color(night.inactiveTitleEnd), Classic.InactiveTitleEnd)
        assertEquals(Color(night.titleText), Classic.TitleText)
        assertEquals(Color(night.tooltip), Classic.Tooltip)
        assertEquals(Color(night.desktop), Classic.Desktop)
    }

    @Test
    fun `a bevel is made of the palette in force`() {
        Classic.palette = Windows98Palette.NOCTORIUM
        val raised = Bevel.Raised
        assertEquals(Classic.Highlight, raised.outerLight)
        assertEquals(Classic.DarkShadow, raised.outerDark)
        assertEquals(Classic.Light, raised.innerLight)
        assertEquals(Classic.Shadow, raised.innerDark)
        assertEquals(Classic.Face, Art.bevelPalette.getValue('f'))
        // And follows it when the theme changes, rather than keeping the colours it was first made with.
        Classic.palette = Windows98Palette.STANDARD
        assertEquals(Color(Windows98Colours.HIGHLIGHT), Bevel.Raised.outerLight)
        assertEquals(Color(Windows98Colours.FACE), Art.bevelPalette.getValue('f'))
    }

    @Test
    fun `every bevel stands out of the face in both palettes`() {
        // The rule written on Windows98Colours: lit edges paler than the face, shaded ones darker, outermost most.
        listOf(Windows98Palette.STANDARD, Windows98Palette.NOCTORIUM).forEach { palette ->
            Classic.palette = palette
            val face = Classic.Face.luminance()
            assertTrue(Classic.Highlight.luminance() > Classic.Light.luminance(), "$palette: highlight over light")
            assertTrue(Classic.Light.luminance() > face, "$palette: light over the face")
            assertTrue(Classic.Shadow.luminance() < face, "$palette: shadow under the face")
            assertTrue(Classic.DarkShadow.luminance() <= Classic.Shadow.luminance(), "$palette: dark shadow under shadow")
        }
    }

    @Test
    fun `writing reads wherever 98 put it, in both palettes`() {
        listOf(Windows98Palette.STANDARD, Windows98Palette.NOCTORIUM).forEach { palette ->
            Classic.palette = palette
            assertReads(Classic.Text, Classic.Face, "writing on the face", palette)
            assertReads(Classic.Text, Classic.Window, "writing in a list or a field", palette)
            assertReads(Classic.Text, Classic.Tooltip, "a tooltip or a notice", palette)
            assertReads(Classic.SelectionText, Classic.Selection, "a highlighted row", palette)
            assertReads(Classic.TitleText, Classic.Title, "an active title", palette)
            assertReads(Classic.SelectionText, Classic.DarkShadow, "the display's writing", palette)
            assertReads(Classic.PaleText, Classic.DarkShadow, "the display's quieter writing", palette)
            // Quieter by design -- 98's own grey on grey is only a little over two to one -- but never gone.
            assertTrue(contrast(Classic.InactiveTitleText, Classic.InactiveTitle) >= 2.0, "$palette: an inactive title")
        }
    }

    @Test
    fun `Material's colours under the 98 skin are the palette's`() {
        val theme = ThemePreset.WINDOWS_98_NOCTORIUM
        Classic.palette = theme.windows98Palette
        val scheme = skinColorScheme(ThemeSkin.WINDOWS_98, theme.colours!!, Color(theme.colours!!.accent))
        assertEquals(Classic.Face, scheme.background)
        assertEquals(Classic.Face, scheme.surface)
        assertEquals(Classic.Text, scheme.onSurface)
        assertEquals(Classic.Window, scheme.surfaceVariant)
        assertEquals(Classic.Window, scheme.surfaceContainerHigh)
        assertEquals(Classic.Shadow, scheme.outline)
        assertEquals(Classic.Tooltip, scheme.inverseSurface)
        assertEquals(Classic.Text, scheme.inverseOnSurface)
        // The quieter writing is the theme's own, and must read on the black lists as well as on the face.
        assertTrue(contrast(scheme.onSurfaceVariant, scheme.surfaceVariant) >= 4.5, "subtext in a list")
        assertTrue(contrast(scheme.onSurfaceVariant, scheme.background) >= 4.5, "subtext on the face")
    }

    @Test
    fun `98's Material colours are what they were`() {
        val theme = ThemePreset.WINDOWS_98
        Classic.palette = theme.windows98Palette
        val scheme = skinColorScheme(ThemeSkin.WINDOWS_98, theme.colours!!, Color(theme.colours!!.accent))
        assertEquals(Color(Windows98Colours.FACE), scheme.background)
        assertEquals(Color(Windows98Colours.TEXT), scheme.onSurface)
        assertEquals(Color(Windows98Colours.WINDOW), scheme.surfaceVariant)
        assertEquals(Color(Windows98Colours.SHADOW), scheme.outline)
        assertEquals(Color(Windows98Colours.LIGHT), scheme.outlineVariant)
    }

    @Test
    fun `the window's own title bar is the palette's`() {
        val night = Windows98Palette.NOCTORIUM
        assertEquals(night.title.toInt() to night.titleText.toInt(), ThemeSkin.WINDOWS_98.caption(night))
        assertEquals(
            Windows98Colours.TITLE.toInt() to Windows98Colours.TITLE_TEXT.toInt(),
            ThemeSkin.WINDOWS_98.caption(Windows98Palette.STANDARD),
        )
        // XP's is Luna's whatever the palette, and the standard skin leaves the title bar to the theme.
        assertEquals(ThemeSkin.WINDOWS_XP.caption(), ThemeSkin.WINDOWS_XP.caption(night))
        assertNull(ThemeSkin.STANDARD.caption(night))
    }

    @Test
    fun `the theme picker offers Noctorium 98 between 98 and XP`() {
        // Settings lists the themes by family, in the order the core gives them.
        val windows = ThemePreset.entries.groupBy { it.family }.getValue("Windows")
        assertEquals(listOf(ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_98_NOCTORIUM, ThemePreset.WINDOWS_XP), windows)
        assertEquals(ThemeSkin.WINDOWS_98, ThemePreset.WINDOWS_98_NOCTORIUM.skin)
        assertEquals(Windows98Palette.NOCTORIUM, ThemePreset.WINDOWS_98_NOCTORIUM.windows98Palette)
        assertEquals(Windows98Palette.STANDARD, ThemePreset.WINDOWS_98.windows98Palette)
    }

    private fun assertReads(writing: Color, ground: Color, what: String, palette: Windows98Palette) {
        val ratio = contrast(writing, ground)
        assertTrue(ratio >= 4.5, "$what reads at only %.2f:1 in %s".format(ratio, palette))
    }

    private fun contrast(a: Color, b: Color): Double =
        app.noctorium.settings.contrastRatio(a.toArgb().toLong() and 0xFFFFFFFFL, b.toArgb().toLong() and 0xFFFFFFFFL)
}

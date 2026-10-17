package app.noctorium.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.noctorium.settings.AccentPreset
import app.noctorium.settings.ArtworkColour
import app.noctorium.settings.GlowShape
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.onAccent
import app.noctorium.settings.resolvedAccent
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The desktop's half of the Glow themes: the light drawn as core describes it, the cover's colour taken from core
 * without the accent "Match the artwork" gives changing, and what is written on the accent -- now that three of the
 * themes have a white one -- taken from core too, without anything an older theme writes on its accent changing.
 */
class GlowThemesTest {

    private val glowing = ThemePreset.entries.filter { it.glow != null }

    // --- The light ---

    @Test
    fun `every shape is one gradient of one colour, its opacity the strength times the falloff`() {
        assertEquals(listOf(ThemePreset.AFTERGLOW, ThemePreset.CINDER, ThemePreset.VELVET, ThemePreset.SMOKE), glowing)
        glowing.forEach { theme ->
            val glow = theme.glow!!
            val colour = Color(glow.colour)
            glow.placement.shapes.forEach { shape ->
                val stops = glowStops(glow, shape, colour)
                val own = when (shape) {
                    is GlowShape.Pool -> shape.strength
                    is GlowShape.Band -> shape.strength
                }
                assertEquals(GlowShape.FALLOFF.map { it.first }, stops.map { it.first }, "$theme's $shape stops are not the falloff's places")
                stops.zip(GlowShape.FALLOFF).forEach { (stop, falloff) ->
                    assertEquals(colour.copy(alpha = 1f), stop.second.copy(alpha = 1f), "$theme's $shape changes colour along the way")
                    assertEquals(glow.strength * own * falloff.second, stop.second.alpha, 1f / 255f, "$theme's $shape at ${stop.first}")
                }
                assertEquals(0f, stops.last().second.alpha, "$theme's $shape does not end clear")
            }
        }
    }

    @Test
    fun `a pool is a radial gradient and a band a vertical one, and a page with no size has none`() {
        val afterglow = ThemePreset.AFTERGLOW.glow!!
        val velvet = ThemePreset.VELVET.glow!!
        val page = Size(1280f, 800f)
        assertEquals(afterglow.placement.shapes.size, glowBrushes(afterglow, Color.Red, page).size)
        assertTrue(glowBrushes(afterglow, Color.Red, page).all { it is RadialGradient })
        assertTrue(glowBrushes(velvet, Color.Red, page).single() is LinearGradient)
        assertTrue(glowBrushes(afterglow, Color.Red, Size.Zero).isEmpty())
        assertTrue(glowBrushes(velvet, Color.Red, Size(1280f, 0f)).isEmpty())
    }

    @Test
    fun `the light drawn is the light core describes, point for point`() {
        val width = 320
        val height = 200
        glowing.forEach { theme ->
            val glow = theme.glow!!
            val bitmap = ImageBitmap(width, height)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
                drawGlow(PageGlow(glow, Color(glow.colour)))
            }
            val pixels = bitmap.toPixelMap()
            listOf(0 to 0, 104 to 144, 179 to 72, 160 to 100, 40 to 20, 300 to 190, 10 to 180, 250 to 120, 160 to 0, 160 to 60, 160 to 150).forEach { (x, y) ->
                val expected = glow.opacityAt(x + .5f, y + .5f, width.toFloat(), height.toFloat())
                val drawn = pixels[x, y].alpha
                assertTrue(abs(expected - drawn) <= 3f / 255f, "$theme at $x, $y: drawn $drawn, described $expected")
            }
        }
    }

    @Test
    fun `the light is drawn over the part of the page asked for and nowhere else`() {
        val glow = PageGlow(ThemePreset.VELVET.glow!!, Color(ThemePreset.VELVET.glow!!.colour))
        val bitmap = ImageBitmap(100, 100)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(100f, 100f)) {
            drawGlow(glow, page = Size(100f, 100f), at = androidx.compose.ui.geometry.Offset(50f, 0f), area = Size(50f, 100f))
        }
        val pixels = bitmap.toPixelMap()
        assertEquals(0f, pixels[10, 2].alpha, "drawn left of the part asked for")
        assertTrue(pixels[70, 2].alpha > .15f, "not drawn on the part asked for")
    }

    // --- The cover's colour ---

    /**
     * The colours the desktop took from a cover before core had a way of its own, exactly as it took them: the
     * most common vivid hue, and the next one or the first taken towards black.
     */
    private fun before(image: ImageBitmap): Pair<Color, Color>? {
        val pixels = image.toPixelMap()
        val step = maxOf(1, minOf(image.width, image.height) / 48)
        val buckets = Array(12) { floatArrayOf(0f, 0f, 0f, 0f) }
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                val colour = pixels[x, y]
                val max = maxOf(colour.red, colour.green, colour.blue)
                val min = minOf(colour.red, colour.green, colour.blue)
                val saturation = if (max <= 0f) 0f else (max - min) / max
                if (max > 0.15f && max < 0.97f && saturation > 0.22f) {
                    val delta = max - min
                    val hue = when {
                        delta <= 0f -> 0f
                        max == colour.red -> 60f * (((colour.green - colour.blue) / delta) % 6f)
                        max == colour.green -> 60f * (((colour.blue - colour.red) / delta) + 2f)
                        else -> 60f * (((colour.red - colour.green) / delta) + 4f)
                    }.let { if (it < 0f) it + 360f else it }
                    val bucket = buckets[(hue / 30f).toInt().coerceIn(0, 11)]
                    bucket[0] += colour.red
                    bucket[1] += colour.green
                    bucket[2] += colour.blue
                    bucket[3] += 1f
                }
                x += step
            }
            y += step
        }
        val ranked = buckets.filter { it[3] > 0f }.sortedByDescending { it[3] }
        if (ranked.isEmpty()) return null
        val primary = ranked.first().let { Color(it[0] / it[3], it[1] / it[3], it[2] / it[3]) }
        val secondary = ranked.getOrNull(1)?.let { Color(it[0] / it[3], it[1] / it[3], it[2] / it[3]) }
            ?: Color(primary.red * .45f, primary.green * .45f, primary.blue * .45f)
        return primary to secondary
    }

    /** Covers painted for the test: a red bar on a dark sleeve, two-hue gradients, noise, greys, a lone colour. */
    private fun covers(): List<Pair<String, ImageBitmap>> {
        fun paint(name: String, size: Int = 128, draw: (org.jetbrains.skia.Canvas, Float) -> Unit): Pair<String, ImageBitmap> {
            val surface = Surface.makeRasterN32Premul(size, size)
            draw(surface.canvas, size.toFloat())
            return name to surface.makeImageSnapshot().toComposeImageBitmap()
        }
        val random = Random(20261017)
        return listOf(
            paint("red bar") { canvas, s ->
                canvas.drawPaint(Paint().apply { color = 0xFF1A1A1A.toInt() })
                canvas.drawRect(Rect.makeXYWH(s * .42f, s * .15f, s * .17f, s * .62f), Paint().apply { color = 0xFFC2261A.toInt() })
            },
            paint("grey") { canvas, s ->
                canvas.drawPaint(Paint().apply { shader = Shader.makeLinearGradient(0f, 0f, s, s, intArrayOf(0xFF202020.toInt(), 0xFFE0E0E0.toInt()), null, GradientStyle.DEFAULT) })
            },
            paint("lone teal", size = 96) { canvas, s ->
                canvas.drawPaint(Paint().apply { color = 0xFF2E9C8C.toInt() })
            },
        ) + (0 until 12).map { n ->
            paint("gradient $n", size = 100 + n * 7) { canvas, s ->
                fun hsb(h: Float, sat: Float, b: Float) = java.awt.Color.HSBtoRGB(h, sat, b) or (0xFF shl 24)
                val hue = random.nextFloat()
                canvas.drawPaint(
                    Paint().apply {
                        shader = Shader.makeLinearGradient(
                            0f, 0f, s, s,
                            intArrayOf(hsb(hue, .4f + random.nextFloat() * .5f, .3f + random.nextFloat() * .6f), hsb((hue + .2f + random.nextFloat() * .4f) % 1f, .4f + random.nextFloat() * .5f, .3f + random.nextFloat() * .6f)),
                            null,
                            GradientStyle.DEFAULT,
                        )
                    },
                )
                repeat(40) {
                    canvas.drawCircle(random.nextFloat() * s, random.nextFloat() * s, 2f + random.nextFloat() * s * .08f, Paint().apply { color = hsb(random.nextFloat(), random.nextFloat(), random.nextFloat()) })
                }
            }
        }
    }

    @Test
    fun `the cover's colour comes from core and is the colour the desktop always took`() {
        covers().forEach { (name, image) ->
            val sample = sampleArtwork(image)
            val earlier = before(image)
            if (earlier == null) {
                assertNull(sample.colour, "$name has a colour now and had none")
                return@forEach
            }
            val colour = assertNotNull(sample.colour, "$name had a colour and has none now")
            assertEquals(earlier.first.toArgb(), Color(colour).toArgb(), "$name's colour moved")
            // The backdrops' far end, where there was a second hue to take, is the same second hue.
            sample.companion?.let { assertEquals(earlier.second.toArgb(), Color(it).toArgb(), "$name's companion moved") }
            // And it is core's own answer, which every player gives for the same pixels.
            val pixels = IntArray(image.width * image.height).also { image.readPixels(it) }
            assertEquals(ArtworkColour.of(pixels, image.width, image.height), sample.colour)
        }
    }

    @Test
    fun `Match the artwork and Afterglow's light take the one colour`() {
        val red = sampleArtwork(covers().first { it.first == "red bar" }.second).colour!!
        val preferences = NoctoriumPreferences(theme = ThemePreset.AFTERGLOW, accent = AccentPreset.ARTWORK)
        assertEquals(red, preferences.resolvedAccent(red))
        // The light keeps the cover's hue, so it is the red the accent is.
        val light = ThemePreset.AFTERGLOW.glow!!.resolvedColour(red)
        val hue = { argb: Long -> FloatArray(3).also { java.awt.Color.RGBtoHSB((argb shr 16 and 0xFF).toInt(), (argb shr 8 and 0xFF).toInt(), (argb and 0xFF).toInt(), it) }[0] }
        assertEquals(hue(red), hue(light), .02f)
        // A cover with no colour leaves both on the theme's own.
        assertEquals(ThemePreset.AFTERGLOW.colours!!.accent, preferences.resolvedAccent(null))
        assertEquals(ThemePreset.AFTERGLOW.glow!!.colour, ThemePreset.AFTERGLOW.glow!!.resolvedColour(null))
    }

    // --- Writing on the accent ---

    private val accents: List<Long> =
        ThemePreset.entries.mapNotNull { it.colours?.accent } + AccentPreset.entries.mapNotNull { it.argb } + NoctoriumPreferences().customAccent

    @Test
    fun `writing on the accent is core's, and nothing an older theme writes on its accent changes`() {
        accents.forEach { argb ->
            val accent = Color(argb)
            val scheme = noctoriumColorScheme(ThemePreset.NOCTORIUM_NIGHT.colours!!, accent)
            assertEquals(Color(onAccent(argb)), scheme.onPrimary, "on ${argb.toString(16)}")
            assertEquals(Color(onAccent(argb)), accent.onAccent())
            // What the desktop worked out for itself before, which every theme but the Glow ones was drawn with.
            val earlier = if (accent.luminance() > .35f) accent.mix(Color.Black, .84f) else Color.White
            assertEquals(earlier.toArgb(), scheme.onPrimary.toArgb(), "on ${argb.toString(16)}")
        }
    }

    @Test
    fun `a white accent is written on in near-black, and a deep one in white`() {
        assertEquals(Color(0xFF292929), noctoriumColorScheme(ThemePreset.VELVET.colours!!, Color.White).onPrimary)
        assertEquals(Color.White, noctoriumColorScheme(ThemePreset.NOCTORIUM_NIGHT.colours!!, Color(0xFFB47CFF)).onPrimary)
        listOf(ThemePreset.AFTERGLOW, ThemePreset.VELVET, ThemePreset.SMOKE).forEach { theme ->
            val accent = Color(theme.colours!!.accent)
            assertTrue(accent.onAccent().luminance() < .05f, "$theme writes on its white in a pale colour")
        }
    }

    @Test
    fun `only a white, a grey or a black accent is plain, so only those get a second sign of what is on`() {
        val plain = ThemePreset.entries.filter { it.colours?.accent?.let { argb -> Color(argb).isPlainAccent() } == true }
        assertEquals(listOf(ThemePreset.AFTERGLOW, ThemePreset.VELVET, ThemePreset.SMOKE), plain)
        AccentPreset.entries.mapNotNull { it.argb }.forEach { assertFalse(Color(it).isPlainAccent(), "${it.toString(16)} counted as plain") }
        assertFalse(Color(NoctoriumPreferences().customAccent).isPlainAccent())
        listOf(Color.Black, Color.White, Color(0xFF808080), Color(0xFFF4F4F6)).forEach { assertTrue(it.isPlainAccent(), "$it is a colour") }
        // A cover's colour is always vivid enough not to be, since core only takes vivid pixels.
        covers().mapNotNull { sampleArtwork(it.second).colour }.forEach { assertFalse(Color(it).isPlainAccent(), "${it.toString(16)} from a cover counted as plain") }
    }
}

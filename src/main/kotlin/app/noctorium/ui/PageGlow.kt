package app.noctorium.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import app.noctorium.settings.GlowShape
import app.noctorium.settings.ThemeGlow
import kotlin.math.max

/**
 * The light a Glow theme lays across its page, as the window draws it: [glow] is the theme's, and [colour] the
 * colour it is in at this moment -- Afterglow's eased from one cover's to the next, as the accent is.
 */
@Immutable
data class PageGlow(val glow: ThemeGlow, val colour: Color)

/** The glow in force, or null on every theme without one, which is every theme but the Glow ones. */
val LocalPageGlow = compositionLocalOf<PageGlow?> { null }

/**
 * The stops of one of [glow]'s shapes as a gradient: [GlowShape.FALLOFF]'s places, each [colour] as opaque as
 * the glow's strength times the shape's own times what the falloff leaves there.
 *
 * One colour all the way through, only its opacity changing. Compose blends a gradient's stops without
 * premultiplying them, and a fade from the light into a transparent black would have greyed it on the way out;
 * with one colour there is nothing to grey, and it comes out as the phone and a browser draw it.
 */
internal fun glowStops(glow: ThemeGlow, shape: GlowShape, colour: Color): List<Pair<Float, Color>> {
    val peak = glow.strength * when (shape) {
        is GlowShape.Pool -> shape.strength
        is GlowShape.Band -> shape.strength
    }
    return GlowShape.FALLOFF.map { (at, left) -> at to colour.copy(alpha = peak * left) }
}

/**
 * One gradient for each of [glow]'s shapes, in [colour], on a page [page] big: a pool as a radial gradient from its
 * middle, measured against the page's longer side so it is round; a band as a vertical one from the top edge down
 * to its depth. Each is clear past its rim, since its last stop is, and a gradient holds its last stop beyond it.
 * None for a page with no size, which has nothing to light and would make a gradient of no radius.
 */
internal fun glowBrushes(glow: ThemeGlow, colour: Color, page: Size): List<Brush> {
    if (page.width <= 0f || page.height <= 0f) return emptyList()
    return glow.placement.shapes.map { shape ->
        val stops = glowStops(glow, shape, colour).toTypedArray()
        when (shape) {
            is GlowShape.Pool -> Brush.radialGradient(
                *stops,
                center = Offset(shape.x * page.width, shape.y * page.height),
                radius = shape.radius * max(page.width, page.height),
            )
            is GlowShape.Band -> Brush.verticalGradient(*stops, startY = 0f, endY = shape.depth * page.height)
        }
    }
}

/**
 * [glow]'s light on a page [page] big that starts at the top left of what is drawing, painted only over the part of
 * it that is [at] and [area] big: what is drawing is often a part of the page, and nothing it draws should spill
 * past its own edges.
 */
internal fun DrawScope.drawGlow(glow: PageGlow, page: Size = size, at: Offset = Offset.Zero, area: Size = page) {
    glowBrushes(glow.glow, glow.colour, page).forEach { brush -> drawRect(brush, at, area) }
}

/**
 * This colour as a card or a panel has it on a Glow theme: laid over the bare page and made solid.
 *
 * Most of Noctorium's cards are a little see-through, a faint grey over the page, so that one colour serves on
 * every theme's page. Over a Glow theme's light that lets the light through, and a card reads as lit glass rather
 * than as a card. Solid, it is exactly the colour it would have been on the unlit page, and the light stays the
 * page's, round the cards and between them, as the screenshots the themes came from have it. On every other theme
 * nothing changes, and a card under liquid glass still shows the wash.
 */
@Composable
@ReadOnlyComposable
fun Color.solidOnGlow(): Color =
    if (LocalPageGlow.current == null) this else compositeOver(MaterialTheme.colorScheme.background)

/**
 * The glow in force, drawn behind whatever this is on, as the part of it that falls here.
 *
 * Measured against the whole window rather than against this, so the light is one light: the same in the content
 * area as on the page under the glass, and it does not move when a screen changes or the player moves from the
 * foot of the window to the top. The page is drawn first, then this, then everything else -- so whatever has a
 * colour of its own, the sidebar, the player bar, a card, covers it where it is, and a gap between the cards is
 * lit. Nothing at all where the theme has no glow.
 */
@Composable
fun Modifier.pageGlow(): Modifier {
    val glow = LocalPageGlow.current ?: return this
    var origin by remember { mutableStateOf(Offset.Zero) }
    var window by remember { mutableStateOf(Size.Zero) }
    return onGloballyPositioned { coordinates ->
        origin = coordinates.positionInRoot()
        window = coordinates.findRootCoordinates().size.toSize()
    }.drawBehind {
        val own = size
        translate(-origin.x, -origin.y) { drawGlow(glow, window, at = origin, area = own) }
    }
}

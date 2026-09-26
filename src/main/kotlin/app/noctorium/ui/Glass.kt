package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.noctorium.domain.ProviderType
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.Glass
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.min

/**
 * How much of the window the floating glass covers, so a list can pad its end by it.
 *
 * Content scrolls underneath the glass -- that is the entire point of it -- which means the last row of
 * every list would otherwise finish underneath the player with no way to scroll it into view.
 */
@Immutable
data class ChromeInsets(val top: Dp = 0.dp, val bottom: Dp = 0.dp)

val LocalChromeInsets = compositionLocalOf { ChromeInsets() }

/** Whether what is drawing sits inside a pane of glass, and should leave its own background off. */
val LocalInGlass = staticCompositionLocalOf { false }

/** A lazy list's own padding, plus whatever the floating glass covers at each end. */
@Composable
fun chromePadding(top: Dp = 0.dp, bottom: Dp = 0.dp): PaddingValues {
    val chrome = LocalChromeInsets.current
    return PaddingValues(top = top + chrome.top, bottom = bottom + chrome.bottom)
}

/** The same for a scrolled column, which pads inside its scroll rather than taking content padding. */
@Composable
fun chromeBottom(): Dp = LocalChromeInsets.current.bottom

/**
 * What the glass sees through: the content, recorded, so every pane can redraw its own part of it.
 *
 * This is the step the first version believed could not be done. A composable cannot sample what
 * happens to be behind it -- but it does not have to. The content is recorded into a layer as it draws,
 * and each pane draws that layer again underneath itself, translated so its own patch lines up, and
 * runs the lens over it. The pane is looking at a copy, and the copy is exact.
 */
@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    /** Where the recorded content sits in the window, so a pane can find its own part of it. */
    internal var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Records whatever this draws, for the panes in front of it.
 *
 * The panes must not be inside what this records, or each would be drawing itself through itself. They
 * are siblings drawn after it, which is also why this sits in its own layer: redrawing a pane must never
 * send this back round to record again.
 *
 * Nothing tells the panes when this re-records, and nothing has to: a pane draws this layer by reference,
 * so what it shows is whatever was last recorded here. The first version bumped a counter the panes read,
 * on the theory that Skia would copy the recording where Android references it. It does not, and the
 * counter was worse than useless -- its change landed a frame late, so every change behind the glass cost
 * two frames and both re-ran the frost and the lens. Playing, with nothing on screen moving, that took
 * the process from 14% of a core under Solid to 28%; without it, 14%. Checked for staleness by scrolling
 * and confirming that the card titles above the player carry on, a line lower, inside it.
 */
fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .graphicsLayer()
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * A pane of liquid glass over [backdrop], holding [content].
 *
 * Three layers, bottom to top. What is behind it, frosted lightly and put through the lens -- bent at
 * the rim, clear in the middle. Then the pane's own tint, a sheen from above and the specular rim. Then
 * whatever it holds, which is never itself blurred or bent, and which is told it is in glass so it can
 * leave its own background off.
 *
 * [cornerRadius] of null makes a pill: the ends are exactly half the height, whatever the height is.
 */
@Composable
fun GlassPane(
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var viewOrigin by remember { mutableStateOf(Offset.Zero) }
    val shape = if (cornerRadius == null) RoundedCornerShape(50) else RoundedCornerShape(cornerRadius)
    val tint = MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier
            .shadow(22.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .40f), spotColor = Color.Black.copy(alpha = .55f))
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        // The lens's view of the backdrop, a margin wider than the pane on every side: the rim samples
        // from beyond the pane's edge, and a copy cut exactly to the pane has nothing there to sample.
        Box(
            Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    val margin = LENS_MARGIN.roundToPx()
                    val wide = measurable.measure(
                        Constraints.fixed(constraints.maxWidth + margin * 2, constraints.maxHeight + margin * 2),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) { wide.place(-margin, -margin) }
                }
                .onGloballyPositioned { viewOrigin = it.positionInRoot() }
                .graphicsLayer {
                    clip = true
                    this.shape = RectangleShape
                    val margin = LENS_MARGIN.toPx()
                    val pane = Size(size.width - margin * 2, size.height - margin * 2)
                    renderEffect = lensEffect(Offset(margin, margin), pane, radiusFor(cornerRadius, pane))
                }
                .drawBehind {
                    translate(backdrop.origin.x - viewOrigin.x, backdrop.origin.y - viewOrigin.y) {
                        drawLayer(backdrop.layer)
                    }
                },
        )
        Box(Modifier.matchParentSize().drawBehind { drawGlassFace(radiusFor(cornerRadius, size), tint) })
        CompositionLocalProvider(LocalInGlass provides true) { content() }
    }
}

private fun Density.radiusFor(cornerRadius: Dp?, size: Size): Float =
    cornerRadius?.toPx() ?: (min(size.width, size.height) / 2f)

/** How far past the pane's edge the lens can see: the rim's reach, plus room for the frost to spread. */
private val LENS_MARGIN = (Glass.REFRACTION_DP + Glass.FROST_DP * 3 + 4).dp

/**
 * The lens, compiled once for the whole process.
 *
 * Skia's runtime effects are the desktop's half of the same shader the phone runs as AGSL; the source
 * lives in core so there is exactly one lens. Compiled lazily and kept, because compiling it is the
 * slow part and every pane in every window would otherwise pay for it.
 */
private val LENS: RuntimeEffect by lazy { RuntimeEffect.makeForShader(Glass.LENS_SHADER) }

/**
 * Frost first, then the lens: blurring after bending would soften the very edge that makes it glass.
 * Clamped at the borders, so the frost does not pull transparent black in from outside the copy.
 */
private fun GraphicsLayerScope.lensEffect(origin: Offset, pane: Size, radius: Float): RenderEffect {
    // Skia's blur takes a sigma rather than a radius; this is its own conversion between the two.
    val sigma = Glass.FROST_DP.dp.toPx() * .57735f + .5f
    val frost = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
    val builder = RuntimeShaderBuilder(LENS).apply {
        uniform("origin", origin.x, origin.y)
        uniform("size", pane.width, pane.height)
        uniform("radius", radius)
        uniform("bezel", Glass.BEZEL_DP.dp.toPx())
        uniform("strength", Glass.REFRACTION_DP.dp.toPx())
        uniform("dispersion", Glass.DISPERSION)
    }
    return ImageFilter.makeRuntimeShader(builder, "content", frost).asComposeRenderEffect()
}

/**
 * The pane's own face: a tint so it has a colour, a sheen from above, and the specular rim.
 *
 * The rim is brightest along the top and fades down the sides, with a second, dimmer catch along the
 * bottom -- light from overhead. It is what makes the edge read as the edge of something thick; without
 * it the lens alone looks like a distortion filter with no object.
 */
private fun DrawScope.drawGlassFace(radius: Float, tint: Color) {
    val corner = CornerRadius(radius, radius)
    drawRoundRect(tint.copy(alpha = Glass.TINT_ALPHA), cornerRadius = corner)
    drawRoundRect(
        Brush.verticalGradient(0f to Color.White.copy(alpha = .10f), .55f to Color.Transparent),
        cornerRadius = corner,
    )
    val line = 1.dp.toPx()
    val inset = line / 2f
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = Glass.RIM_ALPHA),
            .45f to Color.White.copy(alpha = .06f),
            .75f to Color.White.copy(alpha = .03f),
            1f to Color.White.copy(alpha = Glass.RIM_ALPHA * .45f),
        ),
        topLeft = Offset(inset, inset),
        size = Size(size.width - line, size.height - line),
        cornerRadius = CornerRadius(radius - inset, radius - inset),
        style = Stroke(line),
    )
}

/**
 * The page under glass: two colours from the cover that is playing, spread corner to corner and pulled
 * most of the way back toward the theme's own background.
 *
 * Not glass itself, and not trying to be. It is what the page is made of, so that what scrolls beneath
 * the panes carries the colour of the record. Toward the background rather than toward black, which
 * leaves a light theme with a bruise on it. With nothing playing there is no wash, only the page.
 */
@Composable
fun GlassWash(
    artworkUrl: String?,
    provider: ProviderType,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val palette = rememberArtworkPalette(artworkUrl, provider, fallback = ArtworkPalette(background, background))
    val near by animateColorAsState(palette.primary.mix(background, Glass.BACKDROP_TOWARD_BACKGROUND), tween(700), label = "washNear")
    val far by animateColorAsState(palette.secondary.mix(background, Glass.BACKDROP_TOWARD_BACKGROUND), tween(700), label = "washFar")
    Box(modifier.fillMaxSize().background(background).background(Brush.linearGradient(listOf(near, far, near))))
}

/**
 * Material's shape scale, multiplied.
 *
 * One knob for the whole application rather than a number per control, because what is being chosen is
 * not the radius of a chip but whether Noctorium looks machined or soft. Soft is 1, which is exactly the
 * set of shapes that were there before this was a choice.
 */
fun noctoriumShapes(corner: CornerStyle): Shapes {
    fun radius(dp: Int) = RoundedCornerShape((dp * corner.scale).dp)
    return Shapes(
        extraSmall = radius(4),
        small = radius(8),
        medium = radius(12),
        large = radius(16),
        extraLarge = radius(28),
    )
}

package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.noctorium.domain.ProviderType
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.Glass
import app.noctorium.settings.SurfaceStyle

/**
 * The same palette with its surfaces turned to glass.
 *
 * Applied after the scheme is worked out rather than woven into it, so that Solid -- which is what almost
 * everybody will be looking at -- goes down exactly the path it always did, and glass is visibly one
 * transformation on top rather than a second copy of the derivation to keep in step.
 *
 * Only the surfaces move. The accent, the writing, and above all the error colours keep their opacity: a
 * warning you can see the wallpaper through is a worse warning, and there is no version of translucency
 * that is worth that.
 */
fun ColorScheme.asGlass(style: SurfaceStyle): ColorScheme {
    if (!style.isGlass) return this
    fun Color.pane(alpha: Float) = copy(alpha = alpha)
    return copy(
        surface = surfaceContainer.pane(Glass.PANEL_ALPHA),
        surfaceVariant = surfaceVariant.pane(Glass.CARD_ALPHA),
        surfaceBright = surfaceBright.pane(Glass.CARD_ALPHA),
        surfaceDim = surfaceDim.pane(Glass.PANEL_ALPHA),
        surfaceContainer = surfaceContainer.pane(Glass.PANEL_ALPHA),
        surfaceContainerHigh = surfaceContainerHigh.pane(Glass.CARD_ALPHA),
        surfaceContainerHighest = surfaceContainerHighest.pane(Glass.CARD_ALPHA),
        surfaceContainerLow = surfaceContainerLow.pane(Glass.PANEL_ALPHA),
        surfaceContainerLowest = surfaceContainerLowest.pane(Glass.PANEL_ALPHA),
    )
}

/**
 * The wash behind everything, which is the half of glass that is easy to forget.
 *
 * Translucent panels over a flat black page are not glass, they are slightly different black panels.
 * There has to be something behind them worth seeing, and the thing Noctorium always has to hand is the
 * cover that is playing -- so the page becomes two of its colours, spread corner to corner and pulled
 * most of the way back toward whatever the theme calls its background.
 *
 * Two colours and a gradient rather than the cover itself blurred: a gradient *is* the blur, it costs
 * nothing to draw at any window size, and it cannot accidentally leave a recognisable face on the wall
 * behind somebody's library. It changes with the record, which is the whole point.
 */
@Composable
fun GlassBackdrop(
    artworkUrl: String?,
    provider: ProviderType,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val palette = rememberArtworkPalette(
        artworkUrl,
        provider,
        fallback = ArtworkPalette(background, background),
    )
    // Toward the background, not toward black: mixing toward black leaves a light theme with a bruise on
    // it. Every theme gets the same restraint for the same reason.
    val near by animateColorAsState(
        palette.primary.mix(background, Glass.BACKDROP_TOWARD_BACKGROUND),
        tween(700),
        label = "glassNear",
    )
    val far by animateColorAsState(
        palette.secondary.mix(background, Glass.BACKDROP_TOWARD_BACKGROUND),
        tween(700),
        label = "glassFar",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(background)
            .background(Brush.linearGradient(listOf(near, far, near))),
    )
}

/**
 * Material's shape scale, multiplied.
 *
 * One knob for the whole application rather than a number per control, because what is being chosen is
 * not the radius of a chip but whether Noctorium looks machined or soft -- and corners that disagree
 * with each other read as an oversight rather than a taste. Soft is 1, which is exactly the set of
 * shapes that were there before this was a choice.
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

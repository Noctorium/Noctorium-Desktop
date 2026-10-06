package app.noctorium.ui.skins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.noctorium.settings.ThemeSkin
import kotlin.math.abs

/*
 * The tiles, pills and cards a screen is built of, drawn with the corners of the time: square under 98, where
 * nothing on the desktop was round, and Luna's few small radii under XP. What is truly round -- a swatch, a
 * picture of somebody -- stays round, and so does every corner under the standard skin.
 */

/** Material's surface, with the skin's corners; and with no soft shadow under 98, which had none. */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val skin = skin()
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = skin.corners(shape),
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (skin == ThemeSkin.WINDOWS_98) 0.dp else shadowElevation,
        border = border,
        content = content,
    )
}

/** Material's clickable surface, with the skin's corners; and with no soft shadow under 98, which had none. */
@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val skin = skin()
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = skin.corners(shape),
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (skin == ThemeSkin.WINDOWS_98) 0.dp else shadowElevation,
        border = border,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [shape] as this skin draws it. Under 98 every rounded corner is squared, except a circle's on something as
 * wide as it is tall; a pill is a rectangle. Under XP a rounded corner is Luna's small one, and circles and
 * pills are left alone. Anything that is not rounded corners, and everything under the standard skin, is as
 * it was asked for.
 */
internal fun ThemeSkin.corners(shape: Shape): Shape = when {
    this == ThemeSkin.STANDARD || shape !is RoundedCornerShape -> shape
    shape == CircleShape -> if (this == ThemeSkin.WINDOWS_98) RoundOnlyWhenSquare else shape
    this == ThemeSkin.WINDOWS_98 -> RectangleShape
    else -> LunaCorner
}

/** Luna's corner on a panel or a tile. */
private val LunaCorner = RoundedCornerShape(3.dp)

/** A circle on something square, and a rectangle on anything else: a swatch stays round, a pill does not. */
private object RoundOnlyWhenSquare : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        if (abs(size.width - size.height) < 1f) {
            CircleShape.createOutline(size, layoutDirection, density)
        } else {
            RectangleShape.createOutline(size, layoutDirection, density)
        }
}

/** [shape] with this skin's corners, for something rounded that is not drawn as a [Surface]. */
@Composable
fun skinCorners(shape: Shape): Shape = skin().corners(shape)

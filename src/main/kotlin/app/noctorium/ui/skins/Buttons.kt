package app.noctorium.ui.skins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.noctorium.settings.ThemeSkin
import kotlin.math.floor

/*
 * Buttons, as the two desktops drew them.
 *
 * Two kinds, because both desktops had two. A push button is the slab that says what it does in words --
 * OK, Cancel, Create -- standing out of the face until it is pressed into it. A tool button is a picture on a
 * toolbar that is flat until the pointer finds it: 98 raised it a pixel, XP put a rounded box round it. Every
 * Material button with writing on it is a push button here, and every icon button is a tool button.
 */

/** How a button is standing just now, from what its interaction source reports. */
private class ButtonState(val hovered: Boolean, val pressed: Boolean, val focused: Boolean)

@Composable
private fun stateOf(source: MutableInteractionSource): ButtonState {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    return ButtonState(hovered, pressed, focused)
}

/** One pixel of the old desktop, in layout terms, for nudging a pressed label down and to the right. */
@Composable
private fun nudge(on: Boolean): Modifier {
    if (!on) return Modifier
    val step = floor(LocalDensity.current.density).coerceAtLeast(1f).toInt()
    return Modifier.offset { IntOffset(step, step) }
}

/**
 * A push button: seventy-five by twenty-three at the least, as a dialog's buttons were, with its words in the
 * middle. [latched] keeps it pressed in, for a button that stays on -- a toggle on a toolbar, a chip that is
 * chosen -- which 98 filled with its checkerboard.
 */
@Composable
internal fun PushButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    latched: Boolean = false,
    padding: PaddingValues = PaddingValues(horizontal = 10.dp),
    minWidth: Dp = 75.dp,
    minHeight: Dp = 23.dp,
    /** Whether a latched button is filled with 98's checkerboard; the Start button never was. */
    checkered: Boolean = true,
    /** How tall what is on it may be: a line of writing, or [Dp.Unspecified] for a button with a picture on it. */
    contentHeight: Dp = 18.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = stateOf(source)
    val skin = skin()
    val isDefault = LocalDefaultButton.current && enabled
    val down = enabled && state.pressed || latched
    Box(
        modifier
            .defaultMinSize(minWidth, minHeight)
            .hoverable(source, enabled)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind {
                if (skin == ThemeSkin.WINDOWS_XP) {
                    drawLunaButton(enabled, state.hovered, enabled && state.pressed, latched, isDefault || state.focused)
                } else {
                    drawClassicButton(down, checkered && latched && !state.pressed, isDefault, state.focused && enabled)
                }
            }
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        ButtonContent(enabled, nudged = skin == ThemeSkin.WINDOWS_98 && down, contentHeight, content = content)
    }
}

/**
 * What is written on a button: black, in the button's size of the skin's type. Unusable, 98 embossed it --
 * grey, with white a pixel below and to the right, so the words looked pressed into the face -- and XP greyed it.
 */
@Composable
private fun ButtonContent(enabled: Boolean, nudged: Boolean, contentHeight: Dp, content: @Composable RowScope.() -> Unit) {
    val skin = skin()
    val row: @Composable (Modifier) -> Unit = { extra ->
        Row(
            extra.heightIn(max = contentHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
        when {
            enabled -> CompositionLocalProvider(LocalContentColor provides if (skin == ThemeSkin.WINDOWS_XP) Luna.Text else Classic.Text) {
                row(nudge(nudged))
            }
            skin == ThemeSkin.WINDOWS_98 -> Box {
                CompositionLocalProvider(LocalContentColor provides Classic.Highlight) { row(nudge(true)) }
                CompositionLocalProvider(LocalContentColor provides Classic.GreyText) { row(Modifier) }
            }
            else -> CompositionLocalProvider(LocalContentColor provides Luna.GreyText) { row(Modifier) }
        }
    }
}

/**
 * 98's push button: the face, raised, or pressed in. The window's default button has a black line round it
 * and its bevel a pixel inside that; the one with the keyboard has dotted lines a little inside its edge.
 */
private fun DrawScope.drawClassicButton(down: Boolean, latched: Boolean, isDefault: Boolean, focused: Boolean) {
    val p = pixel
    drawRect(Classic.Face)
    var inset = 0f
    if (isDefault) {
        edge(Classic.DarkShadow, Classic.DarkShadow)
        inset = p
    }
    if (latched) checkerboard(topLeft = Offset(inset + p * 2, inset + p * 2), size = Size(size.width - (inset + p * 2) * 2, size.height - (inset + p * 2) * 2))
    bevel(if (down) Bevel.Pressed else Bevel.Raised, inset, inset, size.width - inset, size.height - inset)
    if (focused) focusDots(inset + p * 3)
}

/**
 * Luna's push button: a dark blue edge with soft corners round white fading to beige at the foot. The pointer
 * lights an orange glow inside the edge, pressing turns the fade over and darkens it, and the default button
 * and the one with the keyboard glow blue.
 */
private fun DrawScope.drawLunaButton(enabled: Boolean, hovered: Boolean, pressed: Boolean, latched: Boolean, ringed: Boolean) {
    val p = pixel
    val radius = CornerRadius(3.dp.toPx())
    val fill = when {
        !enabled -> Brush.verticalGradient(listOf(Luna.Face.towards(Luna.Window, .4f), Luna.Face.towards(Luna.Window, .4f)))
        pressed || latched -> Brush.verticalGradient(listOf(Luna.ButtonFoot.towards(Luna.Face, .2f), Luna.Face, Luna.Face.towards(Luna.Window, .55f)))
        else -> Brush.verticalGradient(0f to Luna.Window, .55f to Luna.Window.towards(Luna.Face, .45f), .92f to Luna.ButtonFoot, 1f to Luna.ButtonFoot.towards(Luna.Face, .5f))
    }
    drawRoundRect(fill, cornerRadius = radius)
    val glow = when {
        !enabled -> null
        hovered && !pressed -> Brush.verticalGradient(listOf(Luna.Hot.towards(Luna.Window, .55f), Luna.Hot))
        ringed && !pressed -> Brush.verticalGradient(listOf(Luna.Focus.towards(Luna.Window, .45f), Luna.Focus))
        else -> null
    }
    if (glow != null) {
        drawRoundRect(
            glow,
            topLeft = Offset(p * 2, p * 2),
            size = Size(size.width - p * 4, size.height - p * 4),
            cornerRadius = CornerRadius((3.dp.toPx() - p).coerceAtLeast(0f)),
            style = Stroke(p * 2),
        )
    }
    val edge = if (enabled) Luna.ButtonEdge else Luna.GreyText.towards(Luna.Face, .3f)
    drawRoundRect(edge, topLeft = Offset(p / 2, p / 2), size = Size(size.width - p, size.height - p), cornerRadius = radius, style = Stroke(p))
}

/**
 * A tool button: a picture that is flat until pointed at. Under 98 it rises a pixel, and sinks one when pressed
 * or latched on, a latched one filled with the checkerboard; under XP a soft box comes up round it, and on the
 * blue of the taskbar the box is a lighter blue.
 */
@Composable
internal fun ToolButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    latched: Boolean = false,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = stateOf(source)
    val skin = skin()
    val ground = LocalGround.current
    val down = enabled && state.pressed || latched
    Box(
        modifier
            .defaultMinSize(26.dp, 26.dp)
            .hoverable(source, enabled)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind {
                if (skin == ThemeSkin.WINDOWS_XP) {
                    drawLunaTool(ground, enabled && state.hovered, enabled && state.pressed, latched)
                } else {
                    when {
                        down -> {
                            if (latched && !state.pressed) checkerboard(topLeft = Offset(pixel, pixel), size = Size(size.width - pixel * 2, size.height - pixel * 2))
                            thinSunken()
                        }
                        enabled && state.hovered -> thinRaised()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val ink = when {
            ground == Ground.SELECTION -> LocalContentColor.current
            ground == Ground.TASKBAR && skin == ThemeSkin.WINDOWS_XP -> Color.White
            skin == ThemeSkin.WINDOWS_XP -> Luna.Text
            else -> Classic.Text
        }
        CompositionLocalProvider(LocalContentColor provides if (enabled) ink else ink.copy(alpha = .38f)) {
            Box(nudge(skin == ThemeSkin.WINDOWS_98 && down), contentAlignment = Alignment.Center) { content() }
        }
    }
}

private fun DrawScope.drawLunaTool(ground: Ground, hovered: Boolean, pressed: Boolean, latched: Boolean) {
    if (!hovered && !pressed && !latched) return
    val p = pixel
    val radius = CornerRadius(3.dp.toPx())
    val inner = Size(size.width - p, size.height - p)
    if (ground != Ground.FACE) {
        val fill = if (pressed || latched) Color.Black.copy(alpha = .2f) else Color.White.copy(alpha = .16f)
        val line = if (pressed || latched) Color.Black.copy(alpha = .3f) else Color.White.copy(alpha = .32f)
        drawRoundRect(fill, cornerRadius = radius)
        drawRoundRect(line, topLeft = Offset(p / 2, p / 2), size = inner, cornerRadius = radius, style = Stroke(p))
        return
    }
    val fill = if (pressed || latched) {
        Brush.verticalGradient(listOf(Luna.ButtonFoot.towards(Luna.Face, .3f), Luna.Face.towards(Luna.Window, .4f)))
    } else {
        Brush.verticalGradient(listOf(Luna.Window, Luna.Face.towards(Luna.Window, .35f)))
    }
    drawRoundRect(fill, cornerRadius = radius)
    drawRoundRect(if (pressed) Luna.TabEdge else Luna.GroupEdge, topLeft = Offset(p / 2, p / 2), size = inner, cornerRadius = radius, style = Stroke(p))
}

// --- Material's buttons, as the skin draws them ---

/** Material's filled button; a push button under a Windows skin. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.Button(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
        return
    }
    PushButton(onClick, modifier, enabled, interactionSource, content = content)
}

/** Material's outlined button; a push button under a Windows skin. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.OutlinedButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
        return
    }
    PushButton(onClick, modifier, enabled, interactionSource, content = content)
}

/** Material's text button; a push button under a Windows skin, which had no buttons that were only words. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.TextButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
        return
    }
    PushButton(onClick, modifier, enabled, interactionSource, content = content)
}

/** Material's tonal button; a push button under a Windows skin. */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.FilledTonalButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
        return
    }
    PushButton(onClick, modifier, enabled, interactionSource, content = content)
}

/** Material's icon button; a tool button under a Windows skin. */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.IconButton(onClick, modifier, enabled, colors, interactionSource, content)
        return
    }
    ToolButton(onClick, modifier, enabled, interactionSource, content = content)
}

/**
 * Material's filled icon button, which the player bars play and pause with; under a Windows skin a square push
 * button with the picture on it, so the one control that matters most still stands out of the bar.
 */
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.FilledIconButton(onClick, modifier, enabled, shape, colors, interactionSource, content)
        return
    }
    PushButton(onClick, modifier, enabled, interactionSource, padding = PaddingValues(0.dp), minWidth = 0.dp, minHeight = 0.dp) { content() }
}

/** Material's tonal icon button; the same square push button under a Windows skin. */
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.FilledTonalIconButton(onClick, modifier, enabled, shape, colors, interactionSource, content)
        return
    }
    PushButton(onClick, modifier, enabled, interactionSource, padding = PaddingValues(0.dp), minWidth = 0.dp, minHeight = 0.dp) { content() }
}

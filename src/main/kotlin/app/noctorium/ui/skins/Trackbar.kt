package app.noctorium.ui.skins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.noctorium.settings.SeekBar
import app.noctorium.settings.ThemeSkin
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * The trackbar: what both desktops had where Material has a slider, and so what the seek bar called Material
 * becomes under them -- the standard slider of its day.
 *
 * Under 98 a sunken channel four pixels deep with a raised pointer standing in it, eleven wide and twenty-one
 * tall with a pointed foot; under XP a thin pale groove and Luna's rounded pointer. Clicking the channel puts
 * the pointer there, as Material's slider does, rather than paging towards it as Windows did: on a seek bar
 * that is what anybody means by clicking.
 */

private const val THUMB_WIDTH = 11
private const val THUMB_HEIGHT = 21

/** Where the channel runs, measured down the pointer from its top. */
private const val CHANNEL_FROM_TOP = 7

@Composable
internal fun Trackbar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val skin = skin()
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    var dragging by remember { mutableStateOf(false) }

    fun snapped(fraction: Float): Float {
        val clamped = fraction.coerceIn(0f, 1f)
        val stepped = if (steps > 0) (clamped * (steps + 1)).roundToInt() / (steps + 1).toFloat() else clamped
        return valueRange.start + stepped * span
    }

    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(26.dp)
            .hoverable(interactionSource, enabled)
            .focusable(enabled, interactionSource)
            .onKeyEvent { event ->
                if (!enabled) return@onKeyEvent false
                val step = if (steps > 0) 1f / (steps + 1) else .05f
                val delta = when (event.key) {
                    Key.DirectionLeft, Key.DirectionDown -> -step
                    Key.DirectionRight, Key.DirectionUp -> step
                    else -> return@onKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) latestChange(snapped(fraction + delta)) else latestFinished?.invoke()
                true
            }
            .pointerInput(enabled, span, steps) {
                if (!enabled) return@pointerInput
                val thumb = THUMB_WIDTH * floor(density).coerceAtLeast(1f)
                fun at(x: Float) = snapped((x - thumb / 2f) / (size.width - thumb).coerceAtLeast(1f))
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    dragging = true
                    latestChange(at(down.position.x))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        latestChange(at(change.position.x))
                    }
                    dragging = false
                    latestFinished?.invoke()
                }
            },
    ) {
        val p = pixel
        val thumbWidth = THUMB_WIDTH * p
        val thumbHeight = THUMB_HEIGHT * p
        val top = floor((size.height - thumbHeight) / 2f / p) * p
        val channel = top + CHANNEL_FROM_TOP * p
        val x = floor((fraction * (size.width - thumbWidth)) / p) * p
        if (skin == ThemeSkin.WINDOWS_XP) {
            lunaGroove(channel)
            if (steps > 0) ticks(steps, thumbWidth, top + thumbHeight + p, Luna.TabEdge)
            lunaThumb(x, top, enabled, hovered || dragging || focused)
        } else {
            classicChannel(channel)
            if (steps > 0) ticks(steps, thumbWidth, top + thumbHeight + p, Classic.Text)
            pixelArt(Art.trackbarThumb, Art.bevelPalette, x, top)
            if (!enabled) checkerboard(Classic.Highlight, Classic.Face, Offset(x + p * 2, top + p * 2), Size(thumbWidth - p * 4, p * 12))
            if (focused) focusDotsAround(0f, top - p * 2, size.width, top + thumbHeight + p * 2)
        }
    }
}

/** 98's channel: four pixels deep, sunk the way a field is, and running past the pointer's reach at each end. */
private fun DrawScope.classicChannel(y: Float) {
    val p = pixel
    edge(Classic.Shadow, Classic.Highlight, p * 2, y, size.width - p * 2, y + p * 4)
    edge(Classic.DarkShadow, Classic.Light, p * 3, y + p, size.width - p * 3, y + p * 3)
}

/** XP's groove: a pale rounded line edged in grey. */
private fun DrawScope.lunaGroove(y: Float) {
    val p = pixel
    drawRoundRect(Luna.Face.towards(Luna.Window, .6f), Offset(p * 2, y), Size(size.width - p * 4, p * 4), CornerRadius(p * 2))
    drawRoundRect(Luna.TabEdge, Offset(p * 2.5f, y + p / 2), Size(size.width - p * 5, p * 3), CornerRadius(p * 2), style = Stroke(p))
}

/** A tick under the channel at each step, as a trackbar that stepped showed them. */
private fun DrawScope.ticks(steps: Int, thumbWidth: Float, y: Float, colour: androidx.compose.ui.graphics.Color) {
    val p = pixel
    for (i in 0..steps + 1) {
        val x = floor((thumbWidth / 2f + i * (size.width - thumbWidth) / (steps + 1)) / p) * p
        drawRect(colour, Offset(x, y), Size(p, p * 3))
    }
}

/**
 * Luna's pointer: a rounded slab coming to a point, white fading to beige, edged in the blue-grey of the small
 * controls, with green along the inside of its foot -- orange while the pointer is on it or it is moving.
 */
private fun DrawScope.lunaThumb(x: Float, y: Float, enabled: Boolean, lit: Boolean) {
    val p = pixel
    val w = THUMB_WIDTH * p
    val h = THUMB_HEIGHT * p
    val shoulder = h - w / 2f
    val r = p * 2
    val outline = Path().apply {
        moveTo(x + r, y)
        lineTo(x + w - r, y)
        quadraticTo(x + w, y, x + w, y + r)
        lineTo(x + w, y + shoulder)
        lineTo(x + w / 2f, y + h)
        lineTo(x, y + shoulder)
        lineTo(x, y + r)
        quadraticTo(x, y, x + r, y)
        close()
    }
    drawPath(
        outline,
        if (enabled) Brush.verticalGradient(listOf(Luna.Window, Luna.Face.towards(Luna.Window, .3f), Luna.ButtonFoot), startY = y, endY = y + h)
        else Brush.verticalGradient(listOf(Luna.Face, Luna.Face)),
    )
    if (enabled) {
        val foot = if (lit) Luna.Hot else Luna.Progress
        val inner = Path().apply {
            moveTo(x + p * 1.5f, y + shoulder - p * 2)
            lineTo(x + p * 1.5f, y + shoulder - p)
            lineTo(x + w / 2f, y + h - p * 2)
            lineTo(x + w - p * 1.5f, y + shoulder - p)
            lineTo(x + w - p * 1.5f, y + shoulder - p * 2)
        }
        drawPath(inner, foot, style = Stroke(p * 2))
    }
    drawPath(outline, if (enabled) LunaToggleEdge else Luna.GreyText, style = Stroke(p))
}

/** The dotted focus line round a rectangle that is not the whole of what is being drawn. */
private fun DrawScope.focusDotsAround(left: Float, top: Float, right: Float, bottom: Float) {
    val p = pixel
    var x = left
    while (x < right) {
        drawRect(Classic.Text, Offset(x, top), Size(p, p))
        drawRect(Classic.Text, Offset(x, bottom - p), Size(p, p))
        x += p * 2
    }
    var y = top
    while (y < bottom) {
        drawRect(Classic.Text, Offset(left, y), Size(p, p))
        drawRect(Classic.Text, Offset(right - p, y), Size(p, p))
        y += p * 2
    }
}

/** Material's slider; the trackbar under a Windows skin. */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    if (!skin().isWindows) {
        androidx.compose.material3.Slider(value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished, colors, interactionSource)
        return
    }
    Trackbar(value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished, interactionSource)
}

/**
 * The face the Classic seek bar's slab is drawn in: the face of the palette in force under the 98 skin, which is
 * 98's grey under 98 itself and Noctorium 98's violet under that, and 98's grey under every other theme, where the
 * slab is a little piece of 98 set into a modern bar.
 */
@Composable
fun classicSlabFace(): Color = if (skin() == ThemeSkin.WINDOWS_98) Classic.Face else Color(SeekBar.CLASSIC_FACE)

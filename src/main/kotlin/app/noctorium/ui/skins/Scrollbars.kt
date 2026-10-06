package app.noctorium.ui.skins

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import app.noctorium.settings.ThemeSkin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max

/*
 * The scroll bar both desktops had, which Compose's own cannot be: it has no arrow buttons.
 *
 * Sixteen pixels wide under 98 and seventeen under XP. The arrows step a line at a time and keep stepping while
 * held; a press on the track above or below the thumb moves a page towards it, and keeps paging until the thumb
 * reaches the pointer; the thumb is as long as the window is of the whole, and can be dragged. Under 98 the track
 * is the white and grey checkerboard and every part is a raised slab; under XP the thumb and arrows are Luna's
 * pale blue, the thumb with its grip.
 *
 * It is a modifier rather than a control beside the list, so a list gains one by adding it to the list itself:
 * the modifier keeps a strip down the right for the bar, draws the bar there and answers the pointer there, and
 * under the standard skin it is nothing at all.
 */

/** How wide the scroll bar is under each skin. */
internal fun ThemeSkin.scrollbarWidth(): Dp = if (this == ThemeSkin.WINDOWS_XP) 17.dp else 16.dp

/** The parts of a scroll bar a press can land on. */
private enum class Part { BACK, PAGE_BACK, THUMB, PAGE_FORWARD, FORWARD }

/** How far an arrow, or a turn of the wheel over the bar, moves the list. */
private val LINE = 40.dp

/** How long an arrow or the track waits before it starts repeating, and how often it repeats after that. */
private const val REPEAT_AFTER_MS = 350L
private const val REPEAT_EVERY_MS = 50L

/**
 * The skin's scroll bar down the right of a lazy list, which must be scrolled by [state]. Put it before the
 * list's padding to have the bar at the very edge. Under the standard skin it changes nothing.
 */
@Composable
fun Modifier.classicScrollbar(state: LazyListState): Modifier {
    val skin = skin()
    if (!skin.isWindows) return this
    return scrollbar(rememberScrollbarAdapter(state), skin)
}

/** The same for a column scrolled by [state]: put it before the column's own verticalScroll. */
@Composable
fun Modifier.classicScrollbar(state: ScrollState): Modifier {
    val skin = skin()
    if (!skin.isWindows) return this
    return scrollbar(rememberScrollbarAdapter(state), skin)
}

/**
 * A page that scrolls, as `verticalScroll` with a state of its own does -- and under a Windows skin with the
 * skin's scroll bar down its right.
 */
@Composable
fun Modifier.scrollingPage(): Modifier {
    val state = rememberScrollState()
    return classicScrollbar(state).verticalScroll(state)
}

@Composable
private fun Modifier.scrollbar(adapter: ScrollbarAdapter, skin: ThemeSkin): Modifier {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf<Part?>(null) }
    val width = skin.scrollbarWidth()
    return this
        .drawWithContent {
            drawContent()
            val bar = width.roundToPx().toFloat()
            val geometry = geometry(adapter, bar, size.height) ?: return@drawWithContent
            translate(left = size.width - bar) {
                clipRect(0f, 0f, bar, size.height) {
                    if (skin == ThemeSkin.WINDOWS_XP) drawLunaScrollbar(geometry, pressed) else drawClassicScrollbar(geometry, pressed)
                }
            }
        }
        .pointerInput(adapter) {
            val bar = width.roundToPx().toFloat()
            fun strip(x: Float) = x >= size.width - bar
            awaitEachGesture {
                // A turn of the wheel over the bar scrolls the list it belongs to, as it did.
                var event = awaitPointerEvent()
                while (event.type == PointerEventType.Scroll || event.type == PointerEventType.Move || event.type == PointerEventType.Enter || event.type == PointerEventType.Exit) {
                    val change = event.changes.first()
                    if (event.type == PointerEventType.Scroll && strip(change.position.x)) {
                        val step = LINE.toPx() * change.scrollDelta.y
                        scope.launch { adapter.scrollTo((adapter.scrollOffset + step).coerceIn(0.0, max(0.0, adapter.contentSize - adapter.viewportSize))) }
                        change.consume()
                    }
                    event = awaitPointerEvent()
                }
                val down = event.changes.firstOrNull { it.pressed && !it.isConsumed } ?: return@awaitEachGesture
                if (!strip(down.position.x)) return@awaitEachGesture
                val geometry = geometry(adapter, bar, size.height.toFloat()) ?: return@awaitEachGesture
                down.consume()
                val y = down.position.y
                val part = geometry.partAt(y)
                pressed = part
                val line = LINE.toPx().toDouble()
                var repeating: Job? = null
                fun repeat(step: () -> Double?) {
                    repeating = scope.launch {
                        var first = true
                        while (isActive) {
                            val target = step() ?: break
                            adapter.scrollTo(target.coerceIn(0.0, max(0.0, adapter.contentSize - adapter.viewportSize)))
                            delay(if (first) REPEAT_AFTER_MS else REPEAT_EVERY_MS)
                            first = false
                        }
                    }
                }
                when (part) {
                    Part.BACK -> repeat { adapter.scrollOffset - line }
                    Part.FORWARD -> repeat { adapter.scrollOffset + line }
                    // Paging stops once the thumb has come to the pointer, as it did.
                    Part.PAGE_BACK -> repeat {
                        geometry(adapter, bar, size.height.toFloat())
                            ?.takeIf { it.thumbTop > y }
                            ?.let { adapter.scrollOffset - adapter.viewportSize }
                    }
                    Part.PAGE_FORWARD -> repeat {
                        geometry(adapter, bar, size.height.toFloat())
                            ?.takeIf { it.thumbTop + it.thumbLength < y }
                            ?.let { adapter.scrollOffset + adapter.viewportSize }
                    }
                    Part.THUMB -> Unit
                }
                val grab = y - geometry.thumbTop
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    if (part == Part.THUMB) {
                        val now = geometry(adapter, bar, size.height.toFloat()) ?: break
                        val travel = (now.trackLength - now.thumbLength).coerceAtLeast(1f)
                        val along = ((change.position.y - grab - now.trackTop) / travel).coerceIn(0f, 1f)
                        val target = along * max(0.0, adapter.contentSize - adapter.viewportSize)
                        scope.launch { adapter.scrollTo(target) }
                    }
                }
                repeating?.cancel()
                pressed = null
            }
        }
        .layout { measurable, constraints ->
            val bar = width.roundToPx()
            val inner = measurable.measure(constraints.offset(horizontal = -bar))
            layout(inner.width + bar, inner.height) { inner.place(0, 0) }
        }
}

/** Where the parts of a scroll bar [width] by [height] are, or null when there is nothing to scroll. */
private fun geometry(adapter: ScrollbarAdapter, width: Float, height: Float): Geometry? {
    val content = adapter.contentSize
    val viewport = adapter.viewportSize
    if (content <= viewport + .5 || height <= width * 2) return null
    val track = height - width * 2
    val length = max(8f, (track * viewport / content).toFloat()).coerceAtMost(track)
    val travel = track - length
    val along = (adapter.scrollOffset / (content - viewport)).toFloat().coerceIn(0f, 1f)
    return Geometry(width, height, trackTop = width, trackLength = track, thumbTop = width + travel * along, thumbLength = length)
}

private class Geometry(
    val width: Float,
    val height: Float,
    val trackTop: Float,
    val trackLength: Float,
    val thumbTop: Float,
    val thumbLength: Float,
) {
    fun partAt(y: Float): Part = when {
        y < trackTop -> Part.BACK
        y >= trackTop + trackLength -> Part.FORWARD
        y < thumbTop -> Part.PAGE_BACK
        y > thumbTop + thumbLength -> Part.PAGE_FORWARD
        else -> Part.THUMB
    }
}

private fun DrawScope.drawClassicScrollbar(geometry: Geometry, pressed: Part?) {
    val p = pixel
    val w = geometry.width
    val thumbTop = floor(geometry.thumbTop / p) * p
    val thumbLength = floor(geometry.thumbLength / p) * p
    checkerboard(topLeft = Offset(0f, geometry.trackTop), size = Size(w, geometry.trackLength))
    // The part of the track being paged through goes dark, as it did while the mouse was held on it.
    when (pressed) {
        Part.PAGE_BACK -> checkerboard(Classic.Text, Classic.Shadow, Offset(0f, geometry.trackTop), Size(w, thumbTop - geometry.trackTop))
        Part.PAGE_FORWARD -> checkerboard(
            Classic.Text,
            Classic.Shadow,
            Offset(0f, thumbTop + thumbLength),
            Size(w, geometry.trackTop + geometry.trackLength - thumbTop - thumbLength),
        )
        else -> Unit
    }
    fun button(top: Float, art: List<String>, down: Boolean) {
        drawRect(Classic.Face, Offset(0f, top), Size(w, w))
        if (down) edge(Classic.Shadow, Classic.Shadow, 0f, top, w, top + w) else bevel(Bevel.Raised, 0f, top, w, top + w)
        val artSize = artSize(art)
        val x = floor((w - artSize.width) / 2f / p) * p + if (down) p else 0f
        val y = top + floor((w - artSize.height) / 2f / p) * p + if (down) p else 0f
        pixelArt(art, mapOf('#' to Classic.Text), x, y)
    }
    button(0f, Art.up, pressed == Part.BACK)
    button(geometry.height - w, Art.down, pressed == Part.FORWARD)
    drawRect(Classic.Face, Offset(0f, thumbTop), Size(w, thumbLength))
    bevel(Bevel.Raised, 0f, thumbTop, w, thumbTop + thumbLength)
}

private fun DrawScope.drawLunaScrollbar(geometry: Geometry, pressed: Part?) {
    val p = pixel
    val w = geometry.width
    drawRect(
        Brush.horizontalGradient(listOf(Luna.Face.towards(Luna.Window, .55f), Luna.Window, Luna.Face.towards(Luna.Window, .7f)), startX = 0f, endX = w),
        size = Size(w, geometry.height),
    )
    val radius = CornerRadius(2.dp.toPx())
    fun slab(top: Float, length: Float, down: Boolean) {
        val fill = if (down) {
            Brush.horizontalGradient(listOf(Luna.ScrollEdge, Luna.ScrollThumb.towards(Luna.ScrollEdge, .4f)), startX = 0f, endX = w)
        } else {
            Brush.horizontalGradient(
                listOf(Luna.ScrollThumb.towards(Luna.Window, .55f), Luna.ScrollThumb, Luna.ScrollThumb.towards(Luna.ScrollEdge, .35f)),
                startX = 0f,
                endX = w,
            )
        }
        drawRoundRect(fill, Offset(p, top + p), Size(w - p * 2, length - p * 2), radius)
        drawRoundRect(Luna.ScrollEdge, Offset(p * 1.5f, top + p * 1.5f), Size(w - p * 3, length - p * 3), radius, style = Stroke(p))
    }
    fun arrow(top: Float, art: List<String>, down: Boolean) {
        slab(top, w, down)
        val artSize = artSize(art)
        pixelArt(art, mapOf('#' to Luna.ScrollArrow), floor((w - artSize.width) / 2f / p) * p, top + floor((w - artSize.height) / 2f / p) * p)
    }
    arrow(0f, Art.chevronUp, pressed == Part.BACK)
    arrow(geometry.height - w, Art.chevronDown, pressed == Part.FORWARD)
    val thumbTop = floor(geometry.thumbTop / p) * p
    val thumbLength = floor(geometry.thumbLength / p) * p
    slab(thumbTop, thumbLength, pressed == Part.THUMB)
    // The grip: three short ridges across the middle of the thumb, where there is room for them.
    if (thumbLength >= p * 14) {
        val middle = floor((thumbTop + thumbLength / 2f) / p) * p
        val left = floor((w / 2f - p * 4) / p) * p
        for (i in -1..1) {
            val y = middle + i * p * 2 - p
            drawRect(Luna.Window, Offset(left, y), Size(p * 7, p))
            drawRect(Luna.ScrollEdge, Offset(left + p, y + p), Size(p * 7, p))
        }
    }
}

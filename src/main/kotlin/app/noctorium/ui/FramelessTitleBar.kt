package app.noctorium.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

/**
 * The title bar of a window without the system's frame, which is what the Glass theme's window is on Linux: see
 * SeeThroughWindow.
 *
 * Only what a title bar has to do -- the name, dragging the window by it, a double-click to maximise and back, and
 * the three buttons -- drawn on the glass rather than on a strip of solid colour, so the window is see-through from
 * its top edge down, as it is on Windows. The edges to resize by are Compose's own, which it gives any window
 * without a frame.
 */
@Composable
fun FrameWindowScope.FramelessTitleBar(state: WindowState, close: () -> Unit) {
    val maximised = state.placement == WindowPlacement.Maximized
    val toggleMaximised = {
        state.placement = if (maximised) WindowPlacement.Floating else WindowPlacement.Maximized
    }
    // The double-click is listened for outside the dragging, so the dragging sees a press first and moves the window.
    WindowDraggableArea(
        Modifier.fillMaxWidth().height(TITLE_BAR_HEIGHT.dp).pointerInput(maximised) { detectTapGestures(onDoubleTap = { toggleMaximised() }) },
    ) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = remember { BitmapPainter(AppIcon.image(32).toComposeImageBitmap()) }
            Spacer(Modifier.width(10.dp))
            Image(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Noctorium", fontSize = 12.sp, color = ink(.82f))
            Spacer(Modifier.weight(1f))
            CaptionButton("Minimise", onClick = { state.isMinimized = true }) { colour ->
                drawLine(colour, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.dp.toPx())
            }
            CaptionButton(if (maximised) "Restore" else "Maximise", onClick = toggleMaximised) { colour ->
                val stroke = Stroke(1.dp.toPx())
                if (maximised) {
                    val inset = size.width * .25f
                    drawRect(colour, Offset(inset, 0f), size.copy(size.width - inset, size.height - inset), style = stroke)
                    drawRect(colour, Offset(0f, inset), size.copy(size.width - inset, size.height - inset), style = stroke)
                } else {
                    drawRect(colour, style = stroke)
                }
            }
            CaptionButton("Close", hover = CLOSE_RED, onClick = close) { colour ->
                val width = 1.dp.toPx()
                drawLine(colour, Offset.Zero, Offset(size.width, size.height), strokeWidth = width)
                drawLine(colour, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = width)
            }
        }
    }
}

/** One of the three buttons at the right of the title bar: a glyph drawn in [draw], lit when pointed at. */
@Composable
private fun CaptionButton(
    label: String,
    onClick: () -> Unit,
    hover: Color? = null,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lit = hover ?: MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
    val glyph = if (hovered && hover != null) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = .86f)
    Box(
        Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(if (hovered) lit else Color.Transparent)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(10.dp).padding(0.dp)) { draw(glyph) }
    }
}

/** As tall as the title bars the desktops draw themselves. */
private const val TITLE_BAR_HEIGHT = 32

private val CLOSE_RED = Color(0xFFC42B1C)

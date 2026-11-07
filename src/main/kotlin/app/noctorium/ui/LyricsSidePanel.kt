package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.playback.QueueState
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.Text
import java.awt.Cursor

/*
 * The lyrics beside whatever page is open, as Noctorium Rust has them: the player bar's lyrics button, and
 * Ctrl+L (Cmd+L on a Mac), open and close a panel down the right of the window rather than leaving the page
 * for Now playing. The panel is the lyrics Now playing shows -- the same lines keeping time, the same sources to
 * switch between -- in a column of their own, which can be dragged wider or narrower.
 */

/** Whether the lyrics panel is open, and how wide: for as long as the window is. */
internal object LyricsSidePanelState {
    val DEFAULT_WIDTH: Dp = 360.dp
    val MIN_WIDTH: Dp = 260.dp
    val MAX_WIDTH: Dp = 560.dp

    var open by mutableStateOf(false)
    var width by mutableStateOf(DEFAULT_WIDTH)

    fun toggle() {
        open = !open
    }
}

/** The screens, with the lyrics panel down their right while it is open. */
@Composable
internal fun WithLyricsPanel(queue: QueueState, state: AppState, screens: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) { screens() }
        if (LyricsSidePanelState.open) LyricsSidePanel(queue, state)
    }
}

/** The panel itself: its edge to drag, its heading with the way to close it, and the lyrics of the song playing. */
@Composable
internal fun LyricsSidePanel(queue: QueueState, state: AppState) {
    val density = LocalDensity.current
    val dragging = rememberDraggableState { delta ->
        // The edge is on the left, so dragging it left makes the panel wider.
        val change = with(density) { delta.toDp() }
        LyricsSidePanelState.width = (LyricsSidePanelState.width - change)
            .coerceIn(LyricsSidePanelState.MIN_WIDTH, LyricsSidePanelState.MAX_WIDTH)
    }
    val edge = remember { MutableInteractionSource() }
    val edgeHovered by edge.collectIsHoveredAsState()
    Row(Modifier.width(LyricsSidePanelState.width).fillMaxHeight()) {
        // A rule to drag by, lit in the accent while the pointer is on it.
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .hoverable(edge)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .draggable(dragging, Orientation.Horizontal),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(if (edgeHovered) 2.dp else 1.dp)
                    .fillMaxHeight()
                    .background(
                        if (edgeHovered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f),
                    ),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f).solidOnGlow())
                .padding(bottom = chromeBottom()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Lyrics, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Lyrics", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                IconButton({ LyricsSidePanelState.open = false }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Close the lyrics", Modifier.size(18.dp))
                }
            }
            val track = queue.current
            if (track == null) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Play something to see its lyrics.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Box(Modifier.weight(1f)) { LyricsPanel(track, state) }
            }
        }
    }
}

/** The lyrics button's colour: the accent while the panel is open, as a switch that is on. */
@Composable
internal fun lyricsButtonTint(quiet: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) =
    if (LyricsSidePanelState.open) MaterialTheme.colorScheme.primary else quiet

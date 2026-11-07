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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.playback.QueueState
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.Text
import java.awt.Cursor

/*
 * A panel down the right of the window, beside whatever page is open, as Noctorium Rust has its lyrics: the player
 * bar's lyrics button opens the lyrics in it, and its queue button the queue, rather than either leaving the page
 * for one of its own. One panel, holding one or the other -- two at once would leave the page a strip -- so the
 * other button switches it over, and the same button again closes it. Ctrl+L and Ctrl+U (Cmd on a Mac) do the same.
 * Its edge can be dragged wider or narrower.
 */

/** What the side panel can hold. */
internal enum class SidePanel(val title: String, val icon: ImageVector) {
    LYRICS("Lyrics", Icons.Default.Lyrics),
    QUEUE("Up next", Icons.AutoMirrored.Filled.QueueMusic),
}

/** Which panel is open beside the page, if one is, and how wide: for as long as the window is. */
internal object SidePanelState {
    val DEFAULT_WIDTH: Dp = 360.dp
    val MIN_WIDTH: Dp = 260.dp
    val MAX_WIDTH: Dp = 560.dp

    var shown by mutableStateOf<SidePanel?>(null)
    var width by mutableStateOf(DEFAULT_WIDTH)

    /** Opens [panel], or closes it if it is the one open; the other one open is switched over to it. */
    fun toggle(panel: SidePanel) {
        shown = if (shown == panel) null else panel
    }

    fun shows(panel: SidePanel): Boolean = shown == panel
}

/** The screens, with the side panel down their right while one is open. */
@Composable
internal fun WithSidePanel(queue: QueueState, state: AppState, screens: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) { screens() }
        SidePanelState.shown?.let { panel -> SidePanelColumn(panel, queue, state) }
    }
}

/** The panel itself: its edge to drag, its heading with the way to close it, and what it holds. */
@Composable
internal fun SidePanelColumn(panel: SidePanel, queue: QueueState, state: AppState) {
    val density = LocalDensity.current
    val dragging = rememberDraggableState { delta ->
        // The edge is on the left, so dragging it left makes the panel wider.
        val change = with(density) { delta.toDp() }
        SidePanelState.width = (SidePanelState.width - change).coerceIn(SidePanelState.MIN_WIDTH, SidePanelState.MAX_WIDTH)
    }
    val edge = remember { MutableInteractionSource() }
    val edgeHovered by edge.collectIsHoveredAsState()
    Row(Modifier.width(SidePanelState.width).fillMaxHeight()) {
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
                Icon(panel.icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(panel.title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (panel == SidePanel.QUEUE) {
                    // The whole queue, on its own page, for clearing all but one song and the rest.
                    IconButton({ state.navigate(Destination.QUEUE) }, Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open the queue page", Modifier.size(17.dp))
                    }
                }
                IconButton({ SidePanelState.shown = null }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Close the ${panel.title.lowercase()}", Modifier.size(18.dp))
                }
            }
            Box(Modifier.weight(1f)) {
                when (panel) {
                    SidePanel.LYRICS -> {
                        val track = queue.current
                        if (track == null) Nothing("Play something to see its lyrics.") else LyricsPanel(track, state)
                    }
                    SidePanel.QUEUE -> if (queue.tracks.isEmpty()) Nothing("Nothing queued. Songs you play, play next or add to the queue line up here.") else UpNextPanel(queue, state, compact = true)
                }
            }
        }
    }
}

/** What the panel says when it has nothing to show. */
@Composable
private fun Nothing(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

/** A side panel button's colour: the accent while its panel is open, as a switch that is on. */
@Composable
internal fun sidePanelButtonTint(panel: SidePanel, quiet: Color = MaterialTheme.colorScheme.onSurfaceVariant): Color =
    if (SidePanelState.shows(panel)) MaterialTheme.colorScheme.primary else quiet

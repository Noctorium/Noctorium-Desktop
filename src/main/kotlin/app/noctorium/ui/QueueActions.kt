package app.noctorium.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.playback.QueueState
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.AssistChip
import app.noctorium.ui.skins.DropdownMenuItem
import app.noctorium.ui.skins.MenuCheck
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton

/*
 * What the queue can be told beyond play and clear: clear all but the song playing, and stop after it. On the queue's
 * own page, in its menu, and over the Playlist layout's table.
 */

/** Whether there is anything for Clear all but this song to clear: a song playing, and others in the queue with it. */
internal fun canClearAllButCurrent(queue: QueueState): Boolean = queue.current != null && queue.tracks.size > 1

/** A button that clears all but the song playing: the queue becomes that song alone. Beside the queue's Clear. */
@Composable
internal fun ClearAllButThisSongButton(enabled: Boolean, short: Boolean = false, clear: () -> Unit) {
    TextButton(clear, enabled = enabled) {
        Icon(Icons.Default.PlaylistRemove, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (short) "Clear others" else "Clear all but this song", fontSize = 12.sp, maxLines = 1)
    }
}

/** Stop after this song, as a button that stays lit while it is set: the music stops when the song playing ends. */
@Composable
internal fun StopAfterThisSongButton(on: Boolean, enabled: Boolean, short: Boolean = false, toggle: () -> Unit) {
    TextButton(toggle, enabled = enabled) {
        Icon(Icons.Default.StopCircle, null, Modifier.size(16.dp), tint = if (on) MaterialTheme.colorScheme.primary else ink(.7f))
        Spacer(Modifier.width(6.dp))
        Text(
            if (short) "Stop after" else "Stop after this song",
            fontSize = 12.sp,
            maxLines = 1,
            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Stop after this song, among the queue page's chips beside shuffle, repeat and autoplay, and lit like them while it
 * is set. It is about the song playing now, so it is not kept between launches and clears itself once used.
 */
@Composable
internal fun StopAfterChip(queue: QueueState, state: AppState) {
    val on by state.stopAfterCurrent.collectAsState()
    AssistChip(
        onClick = state::toggleStopAfterCurrent,
        enabled = queue.current != null,
        label = { Text(if (on) "Stopping after this song" else "Stop after this song") },
        leadingIcon = { Icon(Icons.Default.StopCircle, null, Modifier.size(18.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            labelColor = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconContentColor = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/** Clear all but this song and Stop after this song, among the queue menu's items. */
@Composable
internal fun QueueMenuExtras(queue: QueueState, state: AppState, close: () -> Unit) {
    val stopAfter by state.stopAfterCurrent.collectAsState()
    DropdownMenuItem(
        text = { Text("Clear all but this song") },
        leadingIcon = { Icon(Icons.Default.PlaylistRemove, null) },
        enabled = canClearAllButCurrent(queue),
        onClick = { state.clearAllButCurrent(); close() },
    )
    DropdownMenuItem(
        text = { Text("Stop after this song") },
        leadingIcon = { if (stopAfter) MenuCheck(true) else Icon(Icons.Default.StopCircle, null) },
        enabled = queue.current != null,
        onClick = { state.toggleStopAfterCurrent(); close() },
    )
}

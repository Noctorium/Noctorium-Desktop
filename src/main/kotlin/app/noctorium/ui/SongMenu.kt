package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.Track
import app.noctorium.domain.pageUrl
import app.noctorium.downloads.DownloadStage
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.DropdownMenu
import app.noctorium.ui.skins.DropdownMenuItem
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.skins.MenuCheck
import app.noctorium.ui.skins.Text
import kotlin.math.roundToInt

/*
 * A song's menu: the one menu of things to do with a song, wherever a song has one -- the ⋮ on a card or a row,
 * and a right click on the song in the player bar, in every layout and every skin.
 */

/**
 * The song's menu, open while [expanded], and the dialogs it leads to: adding to a playlist and editing the
 * details. Always composed, so a dialog opened from it outlives the menu closing behind it.
 *
 * For the song playing now it also carries Stop after this song, ticked while it is set, since the playing song's
 * menu is where somebody looks for what happens when it ends.
 */
@Composable
internal fun SongMenu(track: Track, state: AppState, expanded: Boolean, dismiss: () -> Unit) {
    var addToPlaylistOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    val library by state.library.collectAsState()
    val ui by state.ui.collectAsState()
    DropdownMenu(expanded = expanded, onDismissRequest = dismiss) {
        SongMenuItems(track, state, dismiss, addToPlaylist = { addToPlaylistOpen = true }, edit = { editOpen = true })
    }
    if (addToPlaylistOpen) {
        AddToPlaylistDialog(track, library.localPlaylists, state) { addToPlaylistOpen = false }
    }
    if (editOpen) {
        EditTrackDialog(track, ui.trackEdits[track.queueKey], state) { editOpen = false }
    }
}

/** The items of [SongMenu]. [close] closes the menu; the two that open a dialog close it on the way. */
@Composable
private fun SongMenuItems(track: Track, state: AppState, close: () -> Unit, addToPlaylist: () -> Unit, edit: () -> Unit) {
    val likes by state.likes.collectAsState()
    val ui by state.ui.collectAsState()
    val queue by state.queue.state.collectAsState()
    val stopAfter by state.stopAfterCurrent.collectAsState()
    // While this computer is another device's remote control, the queue these add to is that device's, and the
    // menu says so: Play next and Add to queue go there.
    val driven = state.connect.collectAsState().value.target?.name
    val likedNow = likes.isLiked(track)
    val pinned = ui.pinnedTracks.any { it.queueKey == track.queueKey }
    DropdownMenuItem(
        text = { Text(drivenLabel("Play next", driven)) },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) },
        onClick = { state.playNext(track); close() },
    )
    DropdownMenuItem(
        text = { Text(drivenLabel("Add to queue", driven)) },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
        onClick = { state.addToQueue(track); close() },
    )
    // Stop after this song is this computer's own player's, so it is not offered for a song playing elsewhere.
    if (driven == null && queue.current?.queueKey == track.queueKey) {
        DropdownMenuItem(
            text = { Text("Stop after this song") },
            leadingIcon = { if (stopAfter) MenuCheck(true) else Icon(Icons.Default.StopCircle, null) },
            onClick = { state.toggleStopAfterCurrent(); close() },
        )
    }
    if (likes.supports(track)) {
        DropdownMenuItem(
            text = { Text(likeLabel(track.provider, likedNow)) },
            leadingIcon = {
                Icon(if (likedNow) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
            },
            onClick = { state.toggleLike(track); close() },
        )
    }
    val downloads by state.downloadState.collectAsState()
    // A Spotify track has no audio of its own, so what is on the disk is filed under the recording
    // matched to it. Asking about the Spotify entry would report it as never downloaded.
    val onDisk = state.downloadableTrack(track)
    val job = downloads.jobFor(onDisk)
    when {
        // A Bandcamp or VK song is neither downloaded nor saved -- the file is the artist's to sell, or
        // VK's to license -- so its own page takes the place of both: where a Bandcamp song is bought,
        // and where a VK one lives. The page alone: the address kept for the song carries what finds
        // its stream, which means nothing in a browser.
        !state.canKeep(track) -> DropdownMenuItem(
            text = { Text(servicePageLabel(track.provider)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
            trailingIcon = servicePageHint(track.provider)?.let { hint ->
                { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
            },
            onClick = { state.openExternalUrl(track.pageUrl); close() },
        )
        // In progress, and tapping it stops it. A download nobody can call off is worse than none.
        job != null && job.stage != DownloadStage.FAILED -> DropdownMenuItem(
            text = { Text("Downloading… ${(job.progress * 100).roundToInt()}%") },
            leadingIcon = { Icon(Icons.Default.Downloading, null) },
            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
            onClick = { state.cancelDownload(onDisk.queueKey); close() },
        )
        job != null -> DropdownMenuItem(
            text = { Text("Download failed — try again") },
            leadingIcon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
            onClick = { state.cancelDownload(onDisk.queueKey); state.downloadTrack(track); close() },
        )
        downloads.isDownloaded(onDisk) -> DropdownMenuItem(
            text = { Text("Remove download") },
            leadingIcon = { Icon(Icons.Default.DownloadDone, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { state.deleteDownload(onDisk.queueKey); close() },
        )
        else -> DropdownMenuItem(
            text = { Text("Download for offline") },
            leadingIcon = { Icon(Icons.Default.Download, null) },
            onClick = { state.downloadTrack(track); close() },
        )
    }
    if (state.canKeep(track)) {
        DropdownMenuItem(
            text = { Text(if (state.canSaveAsMp3()) "Save as MP3…" else "Save a copy…") },
            leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
            // Named for where it lands, because the difference between this and the item above it is
            // exactly that one is Noctorium's copy and this one is the listener's.
            trailingIcon = {
                Text(
                    state.exportFolder()?.fileName?.toString() ?: "no folder",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            },
            onClick = { state.exportTrack(track); close() },
        )
    }
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text(if (pinned) "Unpin from Home" else "Pin to Home") },
        leadingIcon = { Icon(if (pinned) Icons.Default.PushPin else Icons.Outlined.PushPin, null) },
        onClick = { state.togglePin(track); close() },
    )
    DropdownMenuItem(
        text = { Text("Edit details…") },
        leadingIcon = { Icon(Icons.Default.Edit, null) },
        onClick = { close(); edit() },
    )
    DropdownMenuItem(
        text = { Text("Copy link") },
        leadingIcon = { Icon(Icons.Default.Link, null) },
        onClick = { state.copyTrackLink(track); close() },
    )
    DropdownMenuItem(
        text = { Text("Add to playlist") },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
        trailingIcon = { Icon(Icons.Default.ChevronRight, null, Modifier.size(17.dp)) },
        onClick = { close(); addToPlaylist() },
    )
}

/**
 * [content] -- the song, in a player bar -- with the song's menu on a right click anywhere on it, opened where the
 * pointer was. A left click still does whatever it did. How the song in the player bar gives up its menu, in
 * every layout and every skin; with nothing playing ([track] null) it is only [content].
 */
@Composable
internal fun SongMenuArea(track: Track?, state: AppState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var at by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier.pointerInput(track?.queueKey) {
            if (track == null) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.firstOrNull()?.let { change ->
                            at = change.position
                            // Taken, so a bar with a menu of its own -- the Taskbar's -- leaves this click to the song.
                            change.consume()
                        }
                    }
                }
            }
        },
    ) {
        content()
        if (track != null) {
            // Nothing in size, where the pointer was, for the menu to open from.
            Box(Modifier.offset { IntOffset((at?.x ?: 0f).roundToInt(), (at?.y ?: 0f).roundToInt()) }) {
                SongMenu(track, state, expanded = at != null) { at = null }
            }
        }
    }
}

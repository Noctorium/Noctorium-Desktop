package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.Playlist
import app.noctorium.domain.PlaylistEdit
import app.noctorium.domain.PlaylistVisibility
import app.noctorium.domain.shownVisibility
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.AlertDialog
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.DropdownMenu
import app.noctorium.ui.skins.DropdownMenuItem
import app.noctorium.ui.skins.OptionWithNote
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.RadioButton
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.skinned

/*
 * The listener's own YouTube Music playlists: who can see one, and its name and description.
 *
 * YouTube has three answers to who can see a playlist where SoundCloud has two, so a YouTube playlist gets
 * these rather than SoundCloud's "Make public" and "Make private": a badge that says which of the three it is
 * and changes it from a menu, and an Edit dialog that saves the name, the description and the privacy
 * together, the way YouTube Music's own does. The words for each privacy are YouTube's, so the choice reads
 * the same here as it does there.
 */

/** What choosing [visibility] means, in YouTube's own words for it. */
internal fun privacyNote(visibility: PlaylistVisibility): String = when (visibility) {
    PlaylistVisibility.PUBLIC -> "Anyone can search for and view"
    PlaylistVisibility.UNLISTED -> "Anyone with the link can view"
    PlaylistVisibility.PRIVATE -> "Only you can view"
}

/** The picture for each: the world for public, a link for unlisted, a padlock for private. */
internal fun privacyIcon(visibility: PlaylistVisibility): ImageVector = when (visibility) {
    PlaylistVisibility.PUBLIC -> Icons.Default.Public
    PlaylistVisibility.UNLISTED -> Icons.Default.Link
    PlaylistVisibility.PRIVATE -> Icons.Default.Lock
}

/**
 * Says who can see a playlist. Absent when the service has not told us, rather than guessing "public".
 *
 * Given [onClick], it is also the way to change it: a little arrow says so, and the menu it opens hangs off it.
 */
@Composable
internal fun VisibilityBadge(visibility: PlaylistVisibility?, compact: Boolean = false, onClick: (() -> Unit)? = null) {
    if (visibility == null) return
    // Public is the one that is out in the world, so it alone takes the accent; the other two keep quiet.
    val loud = visibility == PlaylistVisibility.PUBLIC
    val tint = if (loud) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(20.dp)
    val color = if (loud) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else ink(.08f)
    val border = BorderStroke(1.dp, ink(.1f))
    val face: @Composable () -> Unit = {
        Row(
            // As tall as the buttons beside it when it is one of them.
            (if (onClick != null) Modifier.heightIn(min = 38.dp) else Modifier).padding(
                start = if (compact) 8.dp else 11.dp,
                end = if (compact) 8.dp else if (onClick != null) 6.dp else 11.dp,
                top = if (compact) 3.dp else 7.dp,
                bottom = if (compact) 3.dp else 7.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(privacyIcon(visibility), null, Modifier.size(if (compact) 11.dp else 14.dp), tint = tint)
            Spacer(Modifier.width(5.dp))
            Text(
                visibility.displayName,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                color = tint,
            )
            if (onClick != null) {
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.ArrowDropDown, "Change who can see it", Modifier.size(18.dp), tint = tint)
            }
        }
    }
    if (onClick == null) {
        Surface(shape = shape, color = color, border = border, content = face)
    } else {
        Surface(onClick = onClick, shape = shape, color = color, border = border, content = face)
    }
}

/**
 * What can be done to one of the listener's own YouTube playlists: its privacy from the badge, everything
 * else from Edit, and Delete.
 */
@Composable
internal fun YouTubePlaylistActions(playlist: Playlist, state: AppState) {
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var choosingPrivacy by remember { mutableStateOf(false) }
    val visibility = playlist.shownVisibility

    if (editing) {
        YouTubePlaylistEditDialog(playlist) { edit ->
            editing = false
            edit?.let { state.editYouTubePlaylist(playlist.id, it) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${playlist.title}\" from ${playlist.provider.displayName}?") },
            text = {
                Text(
                    "This removes the playlist from your ${playlist.provider.displayName} account, not just from " +
                        "Noctorium. The tracks themselves are untouched.",
                )
            },
            confirmButton = {
                Button({
                    confirmDelete = false
                    state.deleteYouTubePlaylist(playlist.id)
                }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton({ confirmDelete = false }) { Text("Keep") } },
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        // Only once YouTube has said which it is, so the menu never ticks a guess. Until then -- or if the page
        // never says -- the privacy is still there to set in Edit.
        if (visibility != null) {
            Box {
                VisibilityBadge(visibility, onClick = { choosingPrivacy = true })
                PrivacyMenu(choosingPrivacy, visibility, { choosingPrivacy = false }) { chosen ->
                    choosingPrivacy = false
                    if (chosen != visibility) state.setYouTubePlaylistVisibility(playlist.id, chosen)
                }
            }
        }
        OutlinedButton({ editing = true }) {
            Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("Edit")
        }
        OutlinedButton({ confirmDelete = true }) { Text("Delete") }
    }
}

/** The three privacies to pick from, the current one ticked. */
@Composable
private fun PrivacyMenu(
    open: Boolean,
    current: PlaylistVisibility,
    dismiss: () -> Unit,
    choose: (PlaylistVisibility) -> Unit,
) {
    DropdownMenu(open, dismiss) {
        PlaylistVisibility.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    // A skin's menu has room for one line a row, so the explanation waits for the Edit dialog there.
                    if (skinned()) {
                        Text(option.displayName)
                    } else {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(option.displayName, fontSize = 13.sp)
                            Text(privacyNote(option), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                leadingIcon = { Icon(privacyIcon(option), null, Modifier.size(18.dp)) },
                trailingIcon = if (option == current) {
                    { Icon(Icons.Default.Check, "Current", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                } else {
                    null
                },
                onClick = { choose(option) },
            )
        }
    }
}

/**
 * Name, description and privacy for one of the listener's own YouTube playlists, saved together.
 *
 * [finish] is handed only what was changed -- see [PlaylistEdit.between] -- or null for Cancel. Save waits until
 * something has changed and the name is not blank, so it never sends a request that does nothing.
 */
@Composable
internal fun YouTubePlaylistEditDialog(playlist: Playlist, finish: (PlaylistEdit?) -> Unit) {
    var name by remember(playlist.playlistKey) { mutableStateOf(playlist.title) }
    var description by remember(playlist.playlistKey) { mutableStateOf(playlist.description.orEmpty()) }
    var visibility by remember(playlist.playlistKey) { mutableStateOf(playlist.shownVisibility) }
    val edit = PlaylistEdit.between(playlist, name, description, visibility)

    AlertDialog(
        onDismissRequest = { finish(null) },
        title = { Text("Edit on ${playlist.provider.displayName}") },
        text = {
            Column(Modifier.widthIn(min = 380.dp, max = 460.dp)) {
                OutlinedTextField(
                    name,
                    { name = it.take(150) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    description,
                    { description = it.take(5_000) },
                    label = { Text("Description") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Privacy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PlaylistVisibility.entries.forEach { option ->
                        PrivacyOption(option, visibility == option) { visibility = option }
                    }
                }
                if (playlist.shownVisibility == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "YouTube did not say who can see this playlist now. Choose one to set it, or leave it as it is.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = { Button({ finish(edit) }, enabled = name.isNotBlank() && !edit.isEmpty) { Text("Save") } },
        dismissButton = { OutlinedButton({ finish(null) }) { Text("Cancel") } },
    )
}

/** One of the three, laid out as the new-playlist dialog lays out where a playlist goes. */
@Composable
private fun PrivacyOption(option: PlaylistVisibility, selected: Boolean, choose: () -> Unit) {
    // Option buttons with what each means beneath them, under a Windows skin.
    if (skinned()) {
        OptionWithNote(option.displayName, privacyNote(option), selected, true, choose)
        return
    }
    Surface(
        onClick = choose,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else ink(.04f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else ink(.08f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, choose)
            Icon(
                privacyIcon(option),
                null,
                Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(option.displayName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(privacyNote(option), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

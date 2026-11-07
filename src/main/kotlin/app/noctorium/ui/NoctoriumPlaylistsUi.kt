package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.playlists.PlaylistCloudKind
import app.noctorium.playlists.PlaylistSyncState
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import java.io.File

/*
 * Settings › You › Noctorium playlists: where the playlists every Noctorium shares are kept. A folder that Google
 * Drive, OneDrive or Dropbox already syncs, chosen here; or the listener's Google Drive itself, signed in to.
 */

/** What a new Noctorium playlist would be, in the dialog that makes one: where it goes, or that it has nowhere yet. */
internal fun noctoriumPlaylistNote(sync: PlaylistSyncState): String = when {
    sync.kind == PlaylistCloudKind.NONE ->
        "On every device you use Noctorium on. Choose where they are kept in Settings › Noctorium playlists."
    sync.location != null -> "On every device you use Noctorium on, kept in ${sync.location}."
    else -> "On every device you use Noctorium on."
}

/** The settings page's tile: where they are kept, and how the last sync went. */
internal fun noctoriumPlaylistsSubtitle(sync: PlaylistSyncState): String {
    // Into a local first: a property from another module is never smart-cast.
    val problem = sync.problem
    return noctoriumPlaylistsSubtitle(sync, problem)
}

private fun noctoriumPlaylistsSubtitle(sync: PlaylistSyncState, problem: String?): String = when {
    sync.kind == PlaylistCloudKind.NONE -> "Your playlists on every device: choose where to keep them"
    problem != null -> problem
    else -> "In ${sync.location ?: sync.kind.displayName}${sync.lastSyncedAtMillis?.let { " · synced ${agoText(it)}" } ?: ""}"
}

private fun agoText(at: Long): String {
    val seconds = (System.currentTimeMillis() - at) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3_600} h ago"
        else -> "${seconds / 86_400} days ago"
    }
}

/**
 * The folders on this computer that a cloud's own app keeps in step, where they are found: Google Drive for
 * desktop's drive, OneDrive, Dropbox, iCloud Drive. Offered as one-click choices beside choosing any folder.
 */
internal fun syncedFolders(): List<Pair<String, File>> {
    val home = File(System.getProperty("user.home"))
    val candidates = buildList {
        // Google Drive for desktop mounts a drive letter with "My Drive" on it on Windows, and a CloudStorage folder on a Mac.
        ('D'..'Z').forEach { letter -> add("Google Drive" to File("$letter:\\My Drive")) }
        File(home, "Library/CloudStorage").listFiles()?.filter { it.name.startsWith("GoogleDrive") }?.forEach {
            add("Google Drive" to File(it, "My Drive"))
        }
        System.getenv("OneDrive")?.let { add("OneDrive" to File(it)) }
        add("OneDrive" to File(home, "OneDrive"))
        add("Dropbox" to File(home, "Dropbox"))
        add("iCloud Drive" to File(home, "Library/Mobile Documents/com~apple~CloudDocs"))
    }
    return candidates.filter { it.second.isDirectory }.distinctBy { it.second.absolutePath }
}

/** A folder chosen in a folder picker, or null when it was closed without one. */
private fun chooseFolder(): File? {
    val chooser = javax.swing.JFileChooser().apply {
        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Where to keep Noctorium playlists"
    }
    return if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NoctoriumPlaylistsSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    val sync by state.playlistSyncState.collectAsState()
    val cloud = preferences.playlistCloud
    var folder by remember(cloud.folder) { mutableStateOf(cloud.folder) }
    var clientId by remember(cloud.googleClientId) { mutableStateOf(cloud.googleClientId) }
    var clientSecret by remember { mutableStateOf("") }
    val found = remember { syncedFolders() }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = chromePadding(bottom = 24.dp)) {
        item {
            SettingsPanelCard {
                CardHeading(Icons.Default.CloudSync, "Noctorium playlists")
                Spacer(Modifier.height(6.dp))
                Text(
                    "A Noctorium playlist is on every device you use Noctorium on -- this computer, your phone, Noctorium Rust -- " +
                        "kept in storage of your own rather than on a server. A local playlist stays on this computer. " +
                        "You choose which when you make one, and can change it from the playlist's menu.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                SyncStatusLine(sync, state)
            }
        }
        item {
            SettingsPanelCard {
                Text("Keep them in", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                CloudOption(
                    "A synced folder",
                    "A folder that Google Drive, OneDrive, Dropbox or Syncthing already keeps in step. On your phone, choose the same folder in its own settings.",
                    selected = cloud.kind == PlaylistCloudKind.FOLDER,
                ) { if (folder.isNotBlank()) state.setPlaylistCloud(PlaylistCloudKind.FOLDER, folder) else chooseFolder()?.let { state.setPlaylistCloud(PlaylistCloudKind.FOLDER, it.absolutePath) } }
                if (cloud.kind == PlaylistCloudKind.FOLDER || cloud.kind == PlaylistCloudKind.NONE) {
                    Column(Modifier.padding(start = 40.dp, top = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (found.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                found.forEach { (name, place) ->
                                    OutlinedButton({ folder = place.absolutePath; state.setPlaylistCloud(PlaylistCloudKind.FOLDER, place.absolutePath) }) {
                                        Icon(Icons.Default.Cloud, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(name)
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(folder, { folder = it }, Modifier.weight(1f), label = { Text("Folder") }, singleLine = true)
                            OutlinedButton({ chooseFolder()?.let { folder = it.absolutePath; state.setPlaylistCloud(PlaylistCloudKind.FOLDER, it.absolutePath) } }) {
                                Icon(Icons.Default.Folder, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Choose…")
                            }
                            if (folder.isNotBlank() && folder != cloud.folder) {
                                Button({ state.setPlaylistCloud(PlaylistCloudKind.FOLDER, folder) }) { Text("Use it") }
                            }
                        }
                        Text(
                            "They go in a folder called \"Noctorium Playlists\" inside it, a file each.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                CloudOption(
                    "Google Drive",
                    "Signed in to your Google Drive. Noctorium can see only the files it makes there, nothing else in your Drive.",
                    selected = cloud.kind == PlaylistCloudKind.GOOGLE_DRIVE,
                ) { state.setPlaylistCloud(PlaylistCloudKind.GOOGLE_DRIVE) }
                if (cloud.kind == PlaylistCloudKind.GOOGLE_DRIVE) {
                    Column(Modifier.padding(start = 40.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sync.googleSignedIn) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Signed in to Google Drive", Modifier.weight(1f), fontSize = 13.sp)
                                TextButton(state::signOutOfGoogleDrive) { Text("Sign out") }
                            }
                        } else {
                            Text(
                                "Google Drive needs an OAuth client of your own, made once: in Google Cloud Console, make a project, " +
                                    "turn on the Google Drive API, and under Credentials make an OAuth client of the \"Desktop app\" type. " +
                                    "Paste its client id and secret here.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                            )
                            OutlinedTextField(clientId, { clientId = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Client id") }, singleLine = true)
                            OutlinedTextField(
                                clientSecret,
                                { clientSecret = it.trim() },
                                Modifier.fillMaxWidth(),
                                label = { Text(if (sync.googleAppReady) "Client secret (saved)" else "Client secret") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (clientId != cloud.googleClientId || clientSecret.isNotBlank()) {
                                    OutlinedButton({ state.setGoogleDriveApp(clientId, clientSecret); clientSecret = "" }) { Text("Save") }
                                }
                                Button(state::signInToGoogleDrive, enabled = sync.googleAppReady && !sync.googleSigningIn) {
                                    if (sync.googleSigningIn) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text("Sign in with Google")
                                }
                                TextButton({ openGoogleConsole(state) }) { Text("Open Google Cloud Console") }
                            }
                        }
                    }
                }
                if (cloud.kind != PlaylistCloudKind.NONE) {
                    Spacer(Modifier.height(10.dp))
                    TextButton({ state.setPlaylistCloud(PlaylistCloudKind.NONE) }) {
                        Icon(Icons.Default.CloudOff, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Stop syncing")
                    }
                }
            }
        }
    }
}

private fun openGoogleConsole(state: AppState) = state.openExternalUrl("https://console.cloud.google.com/apis/credentials")

/** How syncing stands: where, when last, and what went wrong; with the way to sync now. */
@Composable
private fun SyncStatusLine(sync: PlaylistSyncState, state: AppState) {
    val problem = sync.problem
    val (icon, tint, text) = when {
        sync.kind == PlaylistCloudKind.NONE -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant, "Not set up: Noctorium playlists stay here until you choose where to keep them.")
        problem != null -> Triple(Icons.Default.WarningAmber, MaterialTheme.colorScheme.tertiary, problem)
        sync.syncing -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.primary, "Syncing with ${sync.location ?: sync.kind.displayName}…")
        else -> Triple(
            Icons.Default.CloudDone,
            MaterialTheme.colorScheme.primary,
            "Kept in ${sync.location ?: sync.kind.displayName}" + (sync.lastSyncedAtMillis?.let { ". Synced ${agoText(it)}." } ?: "."),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.width(9.dp))
        Text(text, Modifier.weight(1f), fontSize = 12.sp)
        if (sync.kind != PlaylistCloudKind.NONE) {
            TextButton(state::syncPlaylistsNow, enabled = !sync.syncing) { Text("Sync now") }
        }
    }
}

/** One place to keep them, with what it means beneath it. */
@Composable
private fun CloudOption(title: String, note: String, selected: Boolean, choose: () -> Unit) {
    Surface(
        onClick = choose,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else MaterialTheme.colorScheme.outline.copy(alpha = .15f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, choose)
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

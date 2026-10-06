package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import app.noctorium.ui.skins.GroupLegend
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.skins.PageCard
import app.noctorium.ui.skins.TooltipBox
import app.noctorium.ui.skins.skinned
import androidx.compose.material3.Icon
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.pluralTracks
import androidx.compose.ui.unit.Dp
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.downloads.DownloadStage
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.downloads.DownloadsState
import java.util.Locale
import kotlin.math.roundToInt

/** A size somebody can judge at a glance, rather than a number of bytes. */
internal fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "nothing yet"
    bytes < 1024L * 1024 -> "${(bytes / 1024.0).roundToInt()} KB"
    bytes < 1024L * 1024 * 1024 -> "${(bytes / (1024.0 * 1024)).roundToInt()} MB"
    else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
}

/**
 * Why a song from [provider] is not downloaded, said wherever a download would otherwise be offered.
 *
 * [AppState.canKeep] decides which songs those are, and each service has its own reason. What Bandcamp
 * streams to everybody is there to be heard on the way to being bought, and the file is the artist's to
 * sell; VK licenses its music for playing in its own apps, and not for keeping. Either way the song's own
 * page is offered where the download would have been.
 */
internal fun notKeptReason(provider: ProviderType): String = when (provider) {
    ProviderType.BANDCAMP -> "Bandcamp songs are bought, not downloaded"
    ProviderType.VK -> "VK songs play here but can't be downloaded"
    else -> "${provider.displayName} songs can't be downloaded"
}

/**
 * What the button to a song's page on its service says, offered in place of keeping a song that may not be
 * kept. VK is named as its address names it: the page is on vk.ru, not on anything called VK Music.
 */
internal fun servicePageLabel(provider: ProviderType): String = when (provider) {
    ProviderType.VK -> "Open on VK"
    else -> "Open on ${provider.displayName}"
}

/** Why that page is worth opening, where there is something to say: a Bandcamp song is bought there. */
internal fun servicePageHint(provider: ProviderType): String? = when (provider) {
    ProviderType.BANDCAMP -> "to buy"
    else -> null
}

/**
 * How many of [tracks] a playlist's download button would still fetch, or null when none of them may be
 * kept at all and there should be no button.
 *
 * Counted over the songs that may be kept, so a playlist with a Bandcamp song in it does not promise to
 * download that one too, and an album opened from Bandcamp offers no download rather than one that would
 * only say no.
 */
internal fun downloadsLeft(tracks: List<Track>, canKeep: (Track) -> Boolean, isDownloaded: (Track) -> Boolean): Int? =
    tracks.filter(canKeep).takeIf { it.isNotEmpty() }?.count { !isDownloaded(it) }

/** How many tracks are kept, what they occupy, and what is still arriving. */
internal fun describeDownloads(downloads: DownloadsState): String = buildString {
    append(
        when (downloads.entries.size) {
            0 -> "Nothing kept yet"
            1 -> "1 track"
            else -> pluralTracks(downloads.entries.size)
        },
    )
    if (downloads.entries.isNotEmpty()) {
        append(" · ")
        append(formatBytes(downloads.totalBytes))
    }
    if (downloads.active.isNotEmpty()) {
        append(" · ")
        append(downloads.active.size)
        append(" on the way")
    }
}

/**
 * What is kept on this machine, and what is on its way here.
 *
 * Sits at the top of the library because it is the one part of it that still works with no connection,
 * which is exactly when somebody goes looking for it.
 */
@Composable
internal fun OfflineDownloadsCard(downloads: DownloadsState, state: AppState) {
    var expanded by remember { mutableStateOf(false) }

    // A group box under a Windows skin.
    PageCard(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DownloadForOffline,
                null,
                Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Available offline", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    describeDownloads(downloads),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            if (downloads.entries.isNotEmpty()) {
                OutlinedButton({ state.playDownloads() }) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }
                Spacer(Modifier.width(8.dp))
                TextButton({ expanded = !expanded }) { Text(if (expanded) "Hide" else "Show") }
            }
        }

        // Anything still arriving is shown whether or not the list is open. A download in progress is
        // the thing most likely to be wondered about, and hiding it invites pressing download twice.
        downloads.active.forEach { job ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(job.track.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    if (job.stage == DownloadStage.FAILED) {
                        Text(
                            job.detail ?: "Could not download this one.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { job.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    when (job.stage) {
                        DownloadStage.QUEUED -> "Waiting"
                        DownloadStage.DOWNLOADING -> "${(job.progress * 100).roundToInt()}%"
                        DownloadStage.FAILED -> "Failed"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                IconButton({ state.cancelDownload(job.track.queueKey) }, Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, "Stop this download", Modifier.size(16.dp))
                }
            }
        }

        if (expanded && downloads.entries.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = ink(.07f))
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                downloads.entries.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { state.playDownloads(entry.toTrack()) }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${entry.artistName} · ${entry.provider.displayName} · ${formatBytes(entry.bytes)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton({ state.deleteDownload(entry.queueKey) }, Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Remove this download", Modifier.size(17.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(state::deleteAllDownloads) {
                Text("Remove all downloads", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Keeping the track in hand, from wherever it happens to be on screen.
 *
 * Sits beside the heart in the player bar and on the now playing screen, because that is where somebody
 * decides they want to keep something. The action was at first only in a track's menu, which on the home
 * screen appears when a card is pointed at — findable if you already know it is there, and invisible
 * otherwise.
 */
@Composable
internal fun DownloadButton(track: Track, state: AppState, size: Dp = 36.dp) {
    // Faded rather than gone. A row of search results or a player bar would otherwise shift every button
    // after this one whenever a Bandcamp song came along; pointing at it says why it does nothing.
    if (!state.canKeep(track)) {
        val reason = notKeptReason(track.provider)
        return HoverHint(reason) {
            IconButton({}, Modifier.size(size), enabled = false) {
                Icon(
                    Icons.Default.Download,
                    reason,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .38f),
                )
            }
        }
    }
    val downloads by state.downloadState.collectAsState()
    // What is on the disk is the recording, which for a Spotify track is not the track itself. Asking about
    // the Spotify entry would report every one of them as not downloaded, however many times it was kept.
    val onDisk = state.downloadableTrack(track)
    val job = downloads.jobFor(onDisk)
    val kept = downloads.isDownloaded(onDisk)

    IconButton(
        onClick = {
            when {
                job != null && job.stage == DownloadStage.FAILED -> {
                    state.cancelDownload(onDisk.queueKey)
                    state.downloadTrack(track)
                }
                job != null -> state.cancelDownload(onDisk.queueKey)
                kept -> state.deleteDownload(onDisk.queueKey)
                // The original, not the resolved one: an unmatched Spotify track has no recording to keep
                // yet, and downloading it is what goes and finds one.
                else -> state.downloadTrack(track)
            }
        },
        modifier = Modifier.size(size),
    ) {
        when {
            job != null && job.stage == DownloadStage.FAILED -> Icon(
                Icons.Default.ErrorOutline,
                "Download failed — press to try again",
                Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            // The ring doubles as the progress: a spinner would say something is happening without ever
            // saying how much is left, which on a long track reads as stuck.
            job != null -> Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { job.progress.coerceAtLeast(.03f) },
                    modifier = Modifier.size(size * .55f),
                    strokeWidth = 2.dp,
                )
                Icon(
                    Icons.Default.Close,
                    "Stop this download",
                    Modifier.size(size * .28f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            kept -> Icon(
                Icons.Default.DownloadDone,
                "Downloaded — press to remove",
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            else -> Icon(
                Icons.Default.Download,
                "Download for offline",
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A few words that appear when [content] is pointed at, for a control that cannot say for itself why it
 * does nothing.
 *
 * Drawn on the theme's own raised card rather than as a system tooltip, which would be the one pale thing in
 * a dark window -- except under a Windows skin, where the pale yellow box with its black edge is the window.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HoverHint(text: String, content: @Composable () -> Unit) {
    val skinned = skinned()
    TooltipArea(
        tooltip = {
            if (skinned) {
                TooltipBox(text)
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ink(.1f)),
                    shadowElevation = 4.dp,
                ) {
                    Text(text, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        },
        delayMillis = 400,
        content = content,
    )
}

/**
 * Where saved music goes, and what it will be.
 *
 * The format is not a choice offered here, because it is not one: MP3 is produced whenever this machine can
 * produce it, and it almost always can, since mpv has to be installed for anything to play at all and its
 * builds carry an MP3 encoder. Saying so plainly is more use than a menu whose second option nobody wants.
 *
 * [boxed] when it is a card of its own, whose title a Windows skin sets into the edge of its group box.
 */
@Composable
internal fun SaveMusicSetting(preferences: NoctoriumPreferences, state: AppState, boxed: Boolean = false) {
    var folder by remember(preferences.exportFolder) { mutableStateOf(preferences.exportFolder) }
    val resolved = state.exportFolder()
    val mp3 = state.canSaveAsMp3()

    if (boxed && skinned()) {
        GroupLegend("Saving music")
    } else {
        Text("Saving music", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(9.dp))
    OutlinedTextField(
        folder,
        { folder = it },
        label = { Text("Folder") },
        placeholder = { Text(resolved?.toString() ?: "Your desktop") },
        singleLine = true,
        supportingText = {
            Text(
                if (mp3) {
                    "Saved as MP3, tagged with the title, the artist and the cover."
                } else {
                    "Saved in the original format. MP3 needs mpv, which Settings, then Playback tools, installs."
                },
            )
        },
        trailingIcon = {
            if (folder != preferences.exportFolder) {
                TextButton({ state.setExportFolder(folder) }) { Text("Save") }
            }
        },
        modifier = Modifier.fillMaxWidth().tracksTyping(),
    )
    Spacer(Modifier.height(7.dp))
    Text(
        "Leave it empty for your desktop. Files are named \"Artist - Title\", so they sort together on a phone.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
    )
}

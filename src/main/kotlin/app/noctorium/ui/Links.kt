package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.LinkAction
import app.noctorium.core.LinkState
import app.noctorium.core.LinkStatus
import app.noctorium.domain.LinkKind
import app.noctorium.domain.PlaybackOrigin
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.domain.arrivedAtOnce
import app.noctorium.domain.findMusicLink
import app.noctorium.domain.pageUrl
import app.noctorium.domain.placeholderTrack
import app.noctorium.domain.pluralTracks
import app.noctorium.downloads.DownloadStage
import app.noctorium.playback.PlaybackState
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import kotlin.math.roundToInt

/** What is on the system clipboard, as text, or null when it holds something else or cannot be read. */
internal fun clipboardText(): String? = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
}.getOrNull()?.takeIf(String::isNotBlank)

/**
 * The place to paste a link and have it play.
 *
 * Pasting is the whole interaction: the moment the box holds a link to a song or a playlist it is opened,
 * with no button to find. The buttons that are here are for the other things somebody might want from the
 * same link -- the queue, a download, a file -- and for pasting without reaching for the keyboard.
 *
 * Ctrl+V anywhere else in Noctorium does the same and comes here to show what happened, which is why this
 * screen keeps no state of its own that matters: what the last link did lives in [AppState.linkState].
 */
@Composable
internal fun LinkScreen(state: AppState) {
    val link by state.linkState.collectAsState()
    val playback by state.playback.collectAsState()
    var text by remember { mutableStateOf(link.link?.url.orEmpty()) }
    // A link can arrive without going through the box -- Ctrl+V from anywhere -- and the box then shows it,
    // rather than sitting empty above a song it apparently had nothing to do with.
    LaunchedEffect(link.link) {
        link.link?.let { arrived -> if (findMusicLink(text) != arrived) text = arrived.url }
    }
    // Checked now and again rather than once, so a link copied in the browser while this screen is open
    // is offered without having to leave and come back.
    var clipboard by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            clipboard = clipboardText()?.takeIf { findMusicLink(it) != null }
            delay(1_500)
        }
    }

    fun submit(value: String, action: LinkAction = LinkAction.PLAY) {
        text = findMusicLink(value)?.url ?: value
        state.openLink(value, action)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentPadding = chromePadding(top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Paste a link", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                "A song, an album or a playlist from YouTube Music, YouTube, SoundCloud or Bandcamp. It plays as " +
                    "soon as it is pasted.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        item {
            LinkField(
                text = text,
                onText = { value ->
                    val before = findMusicLink(text)

                    val pasted = arrivedAtOnce(text, value)
                    text = value
                    // Opened on arrival, when it was pasted, and only once per link: a link typed by hand
                    // is a valid link several letters before it is the right one, and editing around a link
                    // that is already playing must not start it again. Typed links play on Enter.
                    val now = findMusicLink(value)
                    if (pasted && now != null && now != before) submit(value)
                },
                onSubmit = { submit(text) },
                onPaste = { clipboardText()?.let(::submit) },
                onClear = { text = ""; state.clearLink() },
                placeholder = "https://music.youtube.com/watch?v=…",
            )
        }
        // Not offered when it is the link already in hand: "play it" for what is playing is noise.
        clipboard?.takeIf { findMusicLink(it).let { copied -> copied != findMusicLink(text) && copied != link.link } }?.let { copied ->
            item {
                Surface(
                    onClick = { submit(copied) },
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("There is a link on your clipboard", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                findMusicLink(copied)?.url ?: copied,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button({ submit(copied) }) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play it")
                        }
                    }
                }
            }
        }
        linkOutcome(link, playback, state)
        item {
            Text(
                "Anywhere in Noctorium, Ctrl+V plays the link on your clipboard.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

/** The box a link is pasted into, shared by this screen and the downloads one. */
@Composable
internal fun LinkField(
    text: String,
    onText: (String) -> Unit,
    onSubmit: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            text,
            onText,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Link, null) },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClear) { Icon(Icons.Default.Close, "Clear") }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).tracksTyping(),
        )
        Spacer(Modifier.width(10.dp))
        FilledTonalButton(onPaste, Modifier.height(54.dp)) {
            Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Paste")
        }
    }
}

/** What came of the last link: working on it, what went wrong, or what it turned out to be. */
private fun androidx.compose.foundation.lazy.LazyListScope.linkOutcome(link: LinkState, playback: PlaybackState, state: AppState) {
    when (link.status) {
        LinkStatus.IDLE -> Unit
        LinkStatus.OPENING -> item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        link.link?.kind == LinkKind.SHORT -> "Following the share link…"
                        link.link?.kind == LinkKind.PLAYLIST -> "Reading the playlist…"
                        link.action == LinkAction.PLAY -> "Opening the song…"
                        else -> "Looking the song up…"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinkStatus.FAILED -> item {
            Text(link.message ?: "Could not open that link.", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
        LinkStatus.DONE -> {
            link.message?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
            }
            if (link.link?.kind == LinkKind.PLAYLIST) {
                item { PlaylistActions(link.tracks, state) }
                itemsIndexed(link.tracks, key = { index, track -> "${track.queueKey}:$index" }) { index, track ->
                    LinkTrackRow(track, playback.track?.queueKey == track.queueKey) {
                        state.play(track, PlaybackOrigin.PLAYLIST, link.tracks)
                    }
                }
            } else {
                link.tracks.firstOrNull()?.let { found ->
                    item {
                        // The link's own track until the player has read what it is, and the player's
                        // version -- title, artist, cover -- from then on.
                        val shown = playback.track?.takeIf { it.queueKey == found.queueKey } ?: found
                        LinkTrackCard(shown, playing = playback.track?.queueKey == found.queueKey, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkTrackCard(track: Track, playing: Boolean, state: AppState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderBadge(track.provider, compact = true)
                    if (playing) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.GraphicEq, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Playing", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(track.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    track.artistLine.ifBlank { "Reading the artist…" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!playing) {
                        Button({ state.play(track) }) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Play")
                        }
                    }
                    OutlinedButton({ state.addToQueue(track) }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Queue")
                    }
                    if (state.canKeep(track)) {
                        DownloadLabelButton(track, state)
                        OutlinedButton({ state.exportTrack(track) }) {
                            Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save as file")
                        }
                    } else {
                        // Not kept here -- bought on Bandcamp, or only played from VK -- so the song's own page instead.
                        OpenPageButton(track, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistActions(tracks: List<Track>, state: AppState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(pluralTracks(tracks.size), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Button({ tracks.firstOrNull()?.let { state.play(it, PlaybackOrigin.PLAYLIST, tracks) } }) {
            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Play all")
        }
        OutlinedButton({ tracks.forEach(state::addToQueue) }) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Queue all")
        }
        // A Bandcamp album or a VK playlist has nothing on it that may be kept, so it offers neither rather
        // than two buttons that would only say no.
        if (tracks.any(state::canKeep)) {
            OutlinedButton({ state.downloadAll(tracks) }) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Download all")
            }
            OutlinedButton({ state.exportAll(tracks) }) {
                Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save all")
            }
        }
    }
}

/**
 * The song's page on its service, for a song that may not be kept here: where a Bandcamp song is bought,
 * and where a VK one lives.
 *
 * The page alone, as [pageUrl] gives it: the address Noctorium keeps for a Bandcamp or VK song carries what
 * finds its stream, which means nothing in a browser.
 */
@Composable
private fun OpenPageButton(track: Track, state: AppState) {
    OutlinedButton({ state.openExternalUrl(track.pageUrl) }) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp))
        Text(servicePageLabel(track.provider))
    }
}

/**
 * The service a pasted link leads to when its songs may not be kept, known before anything is fetched: a
 * Bandcamp address is a Bandcamp song or album. Null for a link whose songs may be kept, or for no link.
 */
private fun AppState.keepingRefused(text: String): ProviderType? =
    findMusicLink(text)?.takeIf { link -> !canKeep(link.placeholderTrack()) }?.provider

/** Said under the download box about such a link: why not, and what can be done with it instead. */
private fun notKeptNote(provider: ProviderType): String = notKeptReason(provider) + when (provider) {
    ProviderType.BANDCAMP -> ". Buying one on its Bandcamp page makes the file yours, and Paste link plays it here meanwhile."
    else -> ". Paste link plays it here."
}

@Composable
private fun LinkTrackRow(track: Track, playing: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (playing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal)
                Text(track.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            if (playing) Icon(Icons.Default.GraphicEq, "Playing", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Everything kept on this computer, what is still on its way, and a box to download more by link.
 *
 * Downloads were a card at the top of the library, open or shut, and a button on each track. Both stay;
 * this is the place that shows all of it at once, with room for the progress of a playlist's worth
 * arriving one after another.
 */
@Composable
internal fun DownloadsScreen(state: AppState) {
    val downloads by state.downloadState.collectAsState()
    val link by state.linkState.collectAsState()
    val settings by state.settings.collectAsState()
    var text by remember { mutableStateOf("") }
    // Said here, before anything is fetched. Asked to download a Bandcamp address, the core declines with a
    // note on the library page, and this screen would have looked as though it had simply ignored the press.
    val refused = state.keepingRefused(text)

    fun submit(action: LinkAction) {
        if (text.isNotBlank() && state.keepingRefused(text) == null) state.openLink(text, action)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentPadding = chromePadding(top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Downloads", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        describeDownloads(downloads) + if (downloads.entries.isNotEmpty()) " · they play with no connection" else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
                if (downloads.entries.isNotEmpty()) {
                    Button({ state.playDownloads() }) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Play all")
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            LinkField(
                text = text,
                onText = { text = it },
                onSubmit = { submit(LinkAction.DOWNLOAD) },
                onPaste = { clipboardText()?.let { text = it } },
                onClear = { text = "" },
                placeholder = "Paste a link to download it",
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ submit(LinkAction.DOWNLOAD) }, enabled = findMusicLink(text) != null && refused == null) {
                    Icon(Icons.Default.DownloadForOffline, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Download")
                }
                OutlinedButton({ submit(LinkAction.SAVE) }, enabled = findMusicLink(text) != null && refused == null) {
                    Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (state.canSaveAsMp3()) "Save as MP3" else "Save as file")
                }
            }
            refused?.let { provider ->
                Text(
                    notKeptNote(provider),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            // The link box's own news, when it was this box that asked.
            if (link.action == LinkAction.DOWNLOAD || link.action == LinkAction.SAVE) {
                when (link.status) {
                    LinkStatus.OPENING -> Text("Looking it up…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    LinkStatus.FAILED -> Text(link.message ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    else -> Unit
                }
            }
            downloads.message?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (downloads.active.isNotEmpty()) {
            item { SectionLabel("On the way") }
            itemsIndexed(downloads.active, key = { _, job -> "active:" + job.track.queueKey }) { _, job ->
                DownloadJobRow(job.track, job.stage, job.progress, job.detail) { state.cancelDownload(job.track.queueKey) }
            }
        }
        item { SectionLabel("On this computer") }
        if (downloads.entries.isEmpty()) {
            item {
                Text(
                    "Nothing yet. Download a song with the arrow beside it anywhere in Noctorium, or paste its link above.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        } else {
            itemsIndexed(downloads.entries, key = { _, entry -> "kept:" + entry.queueKey }) { _, entry ->
                val track = entry.toTrack()
                Surface(
                    onClick = { state.playDownloads(track) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemoteArtwork(entry.artworkUrl, entry.provider, Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${entry.artistName} · ${formatBytes(entry.bytes)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ProviderBadge(entry.provider, compact = true)
                        IconButton({ state.exportTrack(track) }, Modifier.size(34.dp)) {
                            Icon(Icons.Default.SaveAlt, "Save as a file", Modifier.size(18.dp))
                        }
                        IconButton({ state.deleteDownload(entry.queueKey) }, Modifier.size(34.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Remove this download", Modifier.size(18.dp))
                        }
                    }
                }
            }
            item {
                TextButton(state::deleteAllDownloads) {
                    Text("Remove all downloads", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) { SaveMusicSetting(settings.preferences, state) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun DownloadJobRow(track: Track, stage: DownloadStage, progress: Float, detail: String?, cancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                if (stage == DownloadStage.FAILED) {
                    Text(detail ?: "Could not download this one.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                when (stage) {
                    DownloadStage.QUEUED -> "Waiting"
                    DownloadStage.DOWNLOADING -> "${(progress * 100).roundToInt()}%"
                    DownloadStage.FAILED -> "Failed"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
            IconButton(cancel, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Stop this download", Modifier.size(16.dp)) }
        }
    }
}

/** Download as a labelled button, for beside other labelled buttons. It says how far along it is. */
@Composable
private fun DownloadLabelButton(track: Track, state: AppState) {
    val downloads by state.downloadState.collectAsState()
    val onDisk = state.downloadableTrack(track)
    val job = downloads.jobFor(onDisk)
    val kept = downloads.isDownloaded(onDisk)
    OutlinedButton(
        onClick = {
            when {
                job?.stage == DownloadStage.FAILED -> { state.cancelDownload(onDisk.queueKey); state.downloadTrack(track) }
                job != null -> state.cancelDownload(onDisk.queueKey)
                else -> state.downloadTrack(track)
            }
        },
        enabled = !kept,
    ) {
        Icon(if (kept) Icons.Default.DownloadDone else Icons.Default.Download, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                kept -> "Downloaded"
                job?.stage == DownloadStage.FAILED -> "Try again"
                job?.stage == DownloadStage.DOWNLOADING -> "Downloading ${(job.progress * 100).roundToInt()}%"
                job != null -> "Waiting"
                else -> "Download"
            },
        )
    }
}

package app.noctorium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import app.noctorium.ui.skins.Switch
import app.noctorium.ui.skins.DropdownMenu
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.IconButton
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.Slider
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.connect.ConnectState
import app.noctorium.connect.DeviceKind
import app.noctorium.connect.RemoteState
import app.noctorium.core.AppState
import kotlinx.coroutines.delay

/**
 * Noctorium Connect, from the desktop.
 *
 * One button. It lists the listener's other devices on this network, and clicking one moves the music
 * there from exactly where it is. While something else is playing it, the same button is its remote
 * control -- play, skip, the place in the song, the volume -- and how it is taken back.
 *
 * The menu stays open once a device has been clicked, and says how it went. It used to close at once, and a
 * handover that failed did so with nothing on the screen at all.
 *
 * Nothing appears here for anybody else's devices, and nothing appears at all without an account: the
 * whole thing is keyed on a secret only devices signed in to the same Noctorium can produce.
 */
@Composable
internal fun ConnectButton(state: AppState, modifier: Modifier = Modifier) {
    val connect by state.connect.collectAsState()
    val playback by state.playback.collectAsState()
    var open by remember { mutableStateOf(false) }

    // Nothing to offer and nothing to say, so nothing in the way.
    if (!connect.available && connect.devices.isEmpty() && connect.target == null) return

    Row {
        IconButton({ open = true }, modifier) {
            Icon(
                Icons.Default.Devices,
                if (connect.target != null) "Playing on ${connect.target?.name}" else "Noctorium Connect",
                tint = if (connect.target != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        val close = {
            open = false
            // Read by now, or it would greet the next opening as though it were news.
            state.dismissConnectMessage()
        }
        DropdownMenu(open, close, modifier = Modifier.widthIn(min = 280.dp)) {
            ConnectMenu(state, connect, playback.isPlaying, close)
        }
    }
}

/** What the menu holds, apart from the menu: drawn on its own by ConnectMenuRenderCheck. */
@Composable
internal fun ColumnScope.ConnectMenu(state: AppState, connect: ConnectState, playingHere: Boolean, close: () -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).widthIn(max = 300.dp)) {
        Text("Noctorium Connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        val subtitle = connectSubtitle(connect)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        connectNews(connect, subtitle)?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider()

    if (connect.target != null) {
        RemoteControls(state, connect.remote)
        HorizontalDivider()
    }

    DeviceLine(
        name = connect.thisDevice.ifBlank { "This computer" },
        detail = localDetail(connect.target != null, playingHere),
        kind = DeviceKind.DESKTOP,
        current = connect.target == null,
        enabled = connect.target != null && !connect.busy,
    ) {
        state.bringPlaybackBack()
    }

    connect.devices.forEach { peer ->
        DeviceLine(
            name = peer.name,
            detail = when {
                connect.target?.id == peer.id -> "Playing here"
                connect.busy -> "Wait a moment"
                else -> "Click to play here"
            },
            kind = peer.kind,
            current = connect.target?.id == peer.id,
            enabled = connect.target?.id != peer.id && !connect.busy,
        ) {
            state.playOn(peer)
        }
    }

    if (connect.devices.isEmpty()) {
        Text(
            if (connect.available) {
                "No other devices yet. Open Noctorium on your phone, on the same wifi, signed in to this account."
            } else {
                "Sign in to your Noctorium account to use Connect."
            },
            Modifier.widthIn(max = 280.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }

    if (connect.target != null) {
        HorizontalDivider()
        Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton({ state.bringPlaybackBack(); close() }) { Text("Bring it back", fontSize = 12.sp) }
            TextButton({ state.stopControlling(); close() }) { Text("Leave it playing", fontSize = 12.sp) }
        }
    }
}

/**
 * The other device's transport, place in the song and volume, and what it is playing.
 *
 * Drawn from what that device says about itself, read every second, so it shows what is happening there
 * rather than what was last asked for. Seeking and the volume are sent when the mouse lets go, not on every
 * step of the drag: each one is a request to the other device.
 */
@Composable
private fun RemoteControls(state: AppState, remote: RemoteState?) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).widthIn(max = 300.dp)) {
        Text(
            remote?.track?.title ?: "Nothing playing there yet",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            remoteLine(remote),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val duration = remote?.durationMs ?: 0
        if (duration > 0) {
            // Where the thumb was let go, until the other device has had a moment to get there.
            var sought by remember { mutableStateOf<Float?>(null) }
            LaunchedEffect(sought) {
                if (sought != null) {
                    delay(SEEK_SETTLES_MS)
                    sought = null
                }
            }
            var dragging by remember { mutableStateOf<Float?>(null) }
            Slider(
                value = dragging ?: sought ?: (remote!!.positionMs.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { fraction ->
                        state.connectSeekTo((fraction * duration).toLong())
                        sought = fraction
                    }
                    dragging = null
                },
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(state::connectPrevious) { Icon(Icons.Default.SkipPrevious, "Previous, there") }
            IconButton(state::connectPlayPause) { PlayPauseIcon(remote?.playing == true, Modifier.size(26.dp)) }
            IconButton(state::connectNext) { Icon(Icons.Default.SkipNext, "Next, there") }
        }

        var volume by remember(remote?.volume) { mutableFloatStateOf(remote?.volume ?: 1f) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, "Volume, there", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = volume,
                onValueChange = { volume = it },
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                onValueChangeFinished = { state.connectSetVolume(volume) },
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeviceLine(
    name: String,
    detail: String,
    kind: DeviceKind,
    current: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick, Modifier.fillMaxWidth(), enabled = enabled) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (kind == DeviceKind.PHONE) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                null,
                Modifier.size(18.dp),
                tint = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 13.sp,
                    color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

/** What this device is doing, said accurately. "Playing here" with nothing playing reads as a fault. */
private fun localDetail(elsewhere: Boolean, playing: Boolean): String = when {
    elsewhere -> "Idle"
    playing -> "Playing here"
    else -> "Ready"
}

internal fun connectSubtitle(connect: ConnectState): String {
    val others = connect.devices.size
    return when {
        connect.busy -> "Sending the music…"
        connect.target != null -> "Playing on ${connect.target?.name}"
        connect.controlledBy != null -> "Being controlled by ${connect.controlledBy}"
        others == 1 -> "1 other device on this network"
        others > 1 -> "$others other devices on this network"
        else -> "Your devices on this network"
    }
}

/** What Connect last had to say, unless the line above it says it already. */
internal fun connectNews(connect: ConnectState, subtitle: String): String? =
    connect.message?.takeIf { it.trimEnd('.') != subtitle.trimEnd('.') }

/** Who it is by, where the other device is in its song, and in its queue: "Burial — 1:02 of 3:45 · 2 of 9". */
internal fun remoteLine(remote: RemoteState?): String {
    val track = remote?.track ?: return ""
    val artists = track.artists.joinToString { it.name }
    val clock = if (remote.durationMs > 0) {
        "${clockTime(remote.positionMs)} of ${clockTime(remote.durationMs)}"
    } else {
        clockTime(remote.positionMs)
    }
    val place = if (remote.queueSize > 1 && remote.queueIndex >= 0) " · ${remote.queueIndex + 1} of ${remote.queueSize}" else ""
    val paused = if (remote.playing) "" else " · paused"
    return listOf(artists, clock + place + paused).filter(String::isNotBlank).joinToString(" — ")
}

private fun clockTime(ms: Long): String {
    val seconds = ms / 1_000
    return if (seconds >= 3_600) "%d:%02d:%02d".format(seconds / 3_600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Long enough for the other device to have sought and been asked again, so the bar does not jump back. */
private const val SEEK_SETTLES_MS = 2_000L

/** Somewhere to put the device name, and every device to play on, whichever player bar is in use. */
@Composable
internal fun ConnectSettingsRows(state: AppState) = SettingAnchor("Noctorium Connect") {
    val connect by state.connect.collectAsState()
    val settings by state.settings.collectAsState()
    var name by remember(settings.preferences.connect.deviceName) {
        mutableStateOf(settings.preferences.connect.deviceName)
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Noctorium Connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(
            if (connect.available) {
                "This device appears as \"${connect.thisDevice}\" to your other devices on this network."
            } else {
                "Sign in to your Noctorium account, above, and your devices will find each other."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                name,
                { name = it },
                label = { Text("Name this device") },
                placeholder = { Text(connect.thisDevice) },
                singleLine = true,
                modifier = Modifier.weight(1f).tracksTyping(),
            )
            Spacer(Modifier.width(8.dp))
            TextButton({ state.renameThisDevice(name) }) { Text("Save") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                settings.preferences.connect.enabled,
                state::setConnectEnabled,
            )
            Spacer(Modifier.width(10.dp))
            Text("Let my other devices find this one", fontSize = 12.sp)
        }
        // The devices themselves, here as well as on the player bar: the Island and Toolbar bars show the
        // Connect button only while another device is being driven, which left no way to start driving one.
        if (connect.devices.isNotEmpty() || connect.target != null) {
            Spacer(Modifier.height(6.dp))
            connect.devices.forEach { peer ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (peer.kind == DeviceKind.PHONE) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                        null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(peer.name, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (connect.target?.id == peer.id) {
                        TextButton(state::bringPlaybackBack) { Text("Bring it back", fontSize = 12.sp) }
                    } else {
                        TextButton({ state.playOn(peer) }, enabled = !connect.busy) { Text("Play there", fontSize = 12.sp) }
                    }
                }
            }
        }
        connect.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        }
    }
}

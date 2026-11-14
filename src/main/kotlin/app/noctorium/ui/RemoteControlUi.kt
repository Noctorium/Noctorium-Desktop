package app.noctorium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.platform.DesktopBridge
import app.noctorium.remote.RemoteControlPreferences
import app.noctorium.remote.RemoteControlStatus
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Switch
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.scrollingPage
import kotlinx.coroutines.delay

/*
 * The remote control's page in Settings: a switch for the server in the core (see RemoteControlServer), where it
 * is, its port, and what it can do. Off until switched on.
 */

/** What the page and its tile go by: the switch's side in the moment before the server has followed it. */
internal fun remoteControlShown(remote: RemoteControlPreferences, status: RemoteControlStatus): RemoteControlStatus = when {
    !remote.enabled -> RemoteControlStatus.Off
    status == RemoteControlStatus.Off -> RemoteControlStatus.Starting(remote.portInUse)
    else -> status
}

internal fun remoteControlSummary(remote: RemoteControlPreferences, status: RemoteControlStatus): String =
    when (val shown = remoteControlShown(remote, status)) {
        RemoteControlStatus.Off -> "Off"
        is RemoteControlStatus.Starting -> "Starting on port ${shown.port}…"
        is RemoteControlStatus.Running -> "On · ${shown.address.removePrefix("http://")}"
        is RemoteControlStatus.Failed -> "On, but could not start on port ${shown.port}"
    }

internal fun remoteControlStatusLine(status: RemoteControlStatus): String = when (status) {
    RemoteControlStatus.Off -> "Off. Nothing on this computer is listening for a remote."
    is RemoteControlStatus.Starting -> "Starting on port ${status.port}…"
    is RemoteControlStatus.Running -> "Running at ${status.address}"
    is RemoteControlStatus.Failed -> status.reason
}

/** What is wrong with a typed port, or null when it is one the remote control can listen on. */
internal fun remoteControlPortProblem(typed: String): String? {
    val digits = typed.trim()
    val ports = RemoteControlPreferences.PORTS
    val choose = "Choose one from ${ports.first} to ${ports.last}."
    val port = digits.toIntOrNull()
    return when {
        digits.isEmpty() -> "Type a port. $choose"
        port == null && digits.all(Char::isDigit) -> "Ports go no higher than ${ports.last}. $choose"
        port == null -> "A port is a whole number. $choose"
        port < ports.first -> "A port below ${ports.first} needs an administrator to listen on. $choose"
        port > ports.last -> "Ports go no higher than ${ports.last}. $choose"
        else -> null
    }
}

internal const val REMOTE_CONTROL_SWITCH = "Control Noctorium from a browser"
internal const val REMOTE_CONTROL_PORT = "Remote control port"

private val bridge = DesktopBridge()

@Composable
internal fun RemoteControlSettingsPanel(remote: RemoteControlPreferences, state: AppState) {
    val status by state.remoteControl.collectAsState()
    RemoteControlSettingsPanel(remote, status, state)
}

@Composable
internal fun RemoteControlSettingsPanel(remote: RemoteControlPreferences, status: RemoteControlStatus, state: AppState) {
    val shown = remoteControlShown(remote, status)
    val address = (shown as? RemoteControlStatus.Running)?.address ?: "http://localhost:${remote.portInUse}"
    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            SettingAnchor(REMOTE_CONTROL_SWITCH) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SettingsRemote, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(REMOTE_CONTROL_SWITCH, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "A page at $address that plays, pauses and skips, changes the queue, searches and shows the " +
                                "lyrics, and an API for scripts, a Stream Deck or a hotkey tool to do the same.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(remote.enabled, state::setRemoteControlEnabled)
                }
            }
            Spacer(Modifier.height(14.dp))
            RemoteServerStatus(shown, state)
            if (shown is RemoteControlStatus.Running) {
                Spacer(Modifier.height(12.dp))
                AddressButtons(shown.address)
            }
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Tune, "What it can do")
            Spacer(Modifier.height(8.dp))
            listOf(
                "Play, pause, stop, next and previous; seek, volume, mute, speed, shuffle and repeat.",
                "The queue: play any song in it, move songs up and down, remove them, clear it, and autoplay's songs.",
                "Search music or radio, and play a result now, next or at the end of the queue; or paste a link.",
                "The lyrics of the song playing, following along line by line when they are synced.",
                "Your playlists and what you played lately, the like button, the sleep timer and stop after this song.",
            ).forEach { line ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(line, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Only this computer can reach it: it listens on 127.0.0.1 and nowhere else, and it refuses any other web " +
                    "page open in your browser that tries to press its buttons.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Terminal, "For scripts")
            Spacer(Modifier.height(8.dp))
            Text(
                "GET reads and POST does, with JSON. $address/api lists everything; the page's API tab shows it too.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "curl -X POST $address/api/play-pause",
                        "curl -X POST $address/api/next",
                        "curl -X POST $address/api/volume -d \"{\\\"volume\\\":0.5}\"",
                        "curl $address/api/state",
                    ).forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                }
            }
        }

        SettingsPanelCard {
            RemotePortField(remote, state)
        }
    }
}

@Composable
private fun RemoteServerStatus(status: RemoteControlStatus, state: AppState) {
    val failed = status is RemoteControlStatus.Failed
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 1.dp).size(18.dp), contentAlignment = Alignment.Center) {
            when (status) {
                RemoteControlStatus.Off -> Icon(Icons.Default.PowerSettingsNew, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                is RemoteControlStatus.Starting -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                is RemoteControlStatus.Running -> Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                is RemoteControlStatus.Failed -> Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            SelectionContainer {
                Text(
                    remoteControlStatusLine(status),
                    color = when {
                        failed -> MaterialTheme.colorScheme.error
                        status == RemoteControlStatus.Off -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 12.sp,
                    fontWeight = if (status is RemoteControlStatus.Running) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            if (failed) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(state::retryRemoteControl) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Try again")
                    }
                    TextButton({ openSettingsPage(state, SettingsPage.REMOTE_CONTROL, REMOTE_CONTROL_PORT) }) { Text("Choose another port") }
                }
            }
        }
    }
}

@Composable
private fun AddressButtons(address: String) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_800)
            copied = false
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Button({ runCatching { bridge.openUrl(address) } }) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Open the remote")
        }
        OutlinedButton({
            runCatching { bridge.copyToClipboard(address) }
            copied = true
        }) {
            Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text(if (copied) "Copied" else "Copy address")
        }
    }
}

@Composable
private fun RemotePortField(remote: RemoteControlPreferences, state: AppState) {
    val saved = remote.portInUse
    var typed by remember(saved) { mutableStateOf(saved.toString()) }
    var problem by remember(saved) { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    val save = {
        problem = remoteControlPortProblem(typed)
        val port = typed.trim().toIntOrNull()
        if (problem == null && port != null) {
            if (port != saved) state.setRemoteControlPort(port)
            typed = port.toString()
        }
    }
    SettingAnchor(REMOTE_CONTROL_PORT) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    typed,
                    { typed = it.filter(Char::isDigit).take(5); problem = null },
                    label = { Text("Port") },
                    singleLine = true,
                    isError = problem != null,
                    modifier = Modifier
                        .width(140.dp)
                        .tracksTyping()
                        .onFocusChanged {
                            if (focused && !it.isFocused) save()
                            focused = it.isFocused
                        }
                        .onPreviewKeyEvent { event ->
                            val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                            if (enter && event.type == KeyEventType.KeyDown) save()
                            enter
                        },
                )
                if (saved != RemoteControlPreferences.DEFAULT_PORT) {
                    Spacer(Modifier.width(10.dp))
                    TextButton({ state.setRemoteControlPort(RemoteControlPreferences.DEFAULT_PORT) }) {
                        Text("Use ${RemoteControlPreferences.DEFAULT_PORT}")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                problem ?: ("Saved when you press Enter or leave the box. Change it only if another program on this " +
                    "computer already has ${RemoteControlPreferences.DEFAULT_PORT}."),
                color = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

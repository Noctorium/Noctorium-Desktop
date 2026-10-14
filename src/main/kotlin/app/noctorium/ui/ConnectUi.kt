package app.noctorium.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.connect.ConnectState
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.Classic
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.Luna
import app.noctorium.ui.skins.Switch
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.isWindows
import app.noctorium.ui.skins.pixel
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.thinSunken

/*
 * Noctorium Connect on the player: the Devices button, and the strip that says where the music is.
 *
 * Connect has a page of its own (see ConnectTab), and the player is the other device's remote control: while this
 * computer drives another, the bars and Now playing show that device's song, and their buttons, seek bar and volume
 * drive it. What the player carries for Connect is a way to the page -- the Devices button -- and, while the music
 * is somewhere else or somebody else is driving this one, a strip along the bar that says so and is a way there too.
 *
 * It used to be a small menu with a remote control of its own, beside a player that went on showing this computer.
 * Two remote controls, one of them the size of a menu, were what did not feel right.
 */

/**
 * What the strip on the player bar says: "Playing on Pocket" while this computer is Pocket's remote control,
 * "Controlled from Desk" while Desk is driving this one -- or nothing, with the music here and nobody else driving.
 */
internal fun connectStripText(connect: ConnectState): String? = when {
    connect.target != null -> "Playing on ${connect.target?.name}"
    connect.controlledBy != null -> "Controlled from ${connect.controlledBy}"
    else -> null
}

/**
 * Whether the Devices button is drawn: always while something is going on, and otherwise once Connect is running.
 * Without an account it has nothing to show and nowhere useful to lead, so a bar is not given a button for it.
 */
internal fun devicesButtonShown(connect: ConnectState): Boolean =
    connect.available || connect.devices.isNotEmpty() || connect.target != null || connect.controlledBy != null

/**
 * A word for starting music, or for adding to the queue, as it reads while the music is on [driven], another device
 * this computer is driving: "Play all on Pocket", "Play next on Pocket", "Add to queue on Pocket". As it always read
 * with the music here.
 */
internal fun drivenLabel(label: String, driven: String?): String = if (driven == null) label else "$label on $driven"

/**
 * A Play button's word, saying where the music will start while this computer is another device's remote control:
 * "Play all on Pocket". A song started here plays there then, with the list it came from, so the button says so.
 */
@Composable
internal fun whereItPlays(label: String, state: AppState): String =
    drivenLabel(label, state.connect.collectAsState().value.target?.name)

/** The Devices button's name for itself, as a screen reader says it. */
internal fun devicesButtonLabel(connect: ConnectState): String = connectStripText(connect) ?: "Noctorium Connect"

/**
 * The Devices button on the player bars and Now playing: the Connect page, a click away. Lit while the music is on
 * another device or another device is driving this one, as it always was while one was being driven.
 */
@Composable
internal fun DevicesButton(
    state: AppState,
    modifier: Modifier = Modifier,
    /** The picture's size, where the button sits among smaller ones -- Now playing's footer; the icon's own otherwise. */
    iconSize: Dp = Dp.Unspecified,
    quiet: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val connect by state.connect.collectAsState()
    if (!devicesButtonShown(connect)) return
    val lit = connect.target != null || connect.controlledBy != null
    IconButton({ state.navigate(Destination.CONNECT) }, modifier) {
        Icon(
            Icons.Default.Devices,
            devicesButtonLabel(connect),
            if (iconSize == Dp.Unspecified) Modifier else Modifier.size(iconSize),
            tint = if (lit) MaterialTheme.colorScheme.primary else quiet,
        )
    }
}

/**
 * The strip along a player bar while the music is on another device, or another device is driving this one, on the
 * edge the bar sits on -- under it at the foot of the window, over it at the top -- as a music player's "Playing on"
 * strip is. A click on it opens the Connect page.
 *
 * Across the whole width under a bar that takes a strip of the window; a small pill under a bar that floats, and
 * inside a pane of glass, where a band would run into the rounded ends. Under a Windows skin, the status bar of
 * the window, set into the face. Nothing at all with nothing to say, so a bar with the music here is as it was.
 */
@Composable
internal fun ConnectStrip(state: AppState, pill: Boolean) {
    val connect by state.connect.collectAsState()
    val text = connectStripText(connect) ?: return
    val atTop = state.settings.collectAsState().value.preferences.playerBarPosition == PlayerBarPosition.TOP
    val open = { state.navigate(Destination.CONNECT) }
    val driving = connect.target != null
    val name = connect.target?.name ?: connect.controlledBy.orEmpty()
    val skin = skin()
    when {
        skin.isWindows -> SkinStrip(text, skin, pill, atTop, open)
        pill -> Box(Modifier.fillMaxWidth().padding(top = if (atTop) 8.dp else 0.dp, bottom = if (atTop) 0.dp else 8.dp), contentAlignment = Alignment.Center) {
            StripContent(text, name, driving, Modifier.clip(RoundedCornerShape(50)), pill = true, open)
        }
        else -> StripContent(text, name, driving, Modifier.fillMaxWidth(), pill = false, open)
    }
}

/**
 * The strip under the standard skin: the accent, with the writing on it, while the music is on another device; the
 * accent faintly, with the writing in it, while another device is driving this one and the music is still here.
 */
@Composable
private fun StripContent(text: String, name: String, driving: Boolean, modifier: Modifier, pill: Boolean, open: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    // A faint accent over the panels' own colour, not over whatever is behind: a pill under a floating bar has the
    // page behind it, and would show it through.
    val background = if (driving) accent else accent.copy(alpha = .16f).compositeOver(MaterialTheme.colorScheme.surfaceContainer)
    val writing = if (driving) MaterialTheme.colorScheme.onPrimary else accent
    Row(
        modifier
            .background(background)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClickLabel = "Open Connect", onClick = open)
            .height(if (pill) 26.dp else STRIP_HEIGHT)
            .padding(start = if (pill) 12.dp else 18.dp, end = if (pill) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Devices, null, Modifier.size(15.dp), tint = writing)
        Spacer(Modifier.width(8.dp))
        Text(
            stripWords(text, name),
            color = writing,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (pill) Modifier.widthIn(max = 360.dp) else Modifier.weight(1f),
        )
        if (!pill) {
            Text("Connect", color = writing.copy(alpha = .8f), fontSize = 11.sp, maxLines = 1)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = writing.copy(alpha = .8f))
    }
}

/** "Playing on **Pocket**": the device's name the part of the line that stands out. */
private fun stripWords(text: String, name: String) = buildAnnotatedString {
    val at = if (name.isNotEmpty()) text.lastIndexOf(name) else -1
    if (at < 0) {
        append(text)
        return@buildAnnotatedString
    }
    append(text.substring(0, at))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
    append(text.substring(at + name.length))
}

/** How tall the strip is across a bar: enough for a line of small writing, and a target to click. */
internal val STRIP_HEIGHT = 26.dp

/**
 * The strip under a Windows skin: the window's status bar, on the face, with the line in a pane sunk into it under 98
 * and set off by a rule under XP -- a status bar's pane was where a window said what it was connected to. Under a
 * bar that floats, a tooltip's pale yellow box instead, as there is no face there to set it into.
 */
@Composable
private fun SkinStrip(text: String, skin: ThemeSkin, pill: Boolean, atTop: Boolean, open: () -> Unit) {
    val xp = skin == ThemeSkin.WINDOWS_XP
    if (pill) {
        Box(Modifier.fillMaxWidth().padding(top = if (atTop) 6.dp else 0.dp, bottom = if (atTop) 0.dp else 6.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .background(if (xp) Luna.Tooltip else Classic.Tooltip)
                    .drawBehind {
                        val edge = if (xp) Luna.GreyText else Classic.Text
                        drawRect(edge, Offset.Zero, Size(size.width, pixel))
                        drawRect(edge, Offset(0f, size.height - pixel), Size(size.width, pixel))
                        drawRect(edge, Offset.Zero, Size(pixel, size.height))
                        drawRect(edge, Offset(size.width - pixel, 0f), Size(pixel, size.height))
                    }
                    .clickable(onClickLabel = "Open Connect", onClick = open)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Devices, null, Modifier.size(14.dp), tint = Classic.Text)
                Spacer(Modifier.width(6.dp))
                Text(text, fontSize = 11.sp, color = Classic.Text, maxLines = 1)
            }
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .drawBehind { drawRect(if (xp) Luna.Face else Classic.Face) }
            .clickable(onClickLabel = "Open Connect", onClick = open)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(18.dp)
                .drawBehind {
                    if (xp) drawRect(Luna.GroupEdge, Offset.Zero, Size(pixel, size.height)) else thinSunken()
                }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Devices, null, Modifier.size(14.dp), tint = if (xp) Luna.TaskPanelTitle else Classic.Text)
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 11.sp, color = if (xp) Luna.Text else Classic.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Connect's own settings, on the Noctorium account's page: what this computer is called on the others, and whether
 * they may find it -- and the Connect page, where the devices themselves are.
 */
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
            app.noctorium.ui.skins.OutlinedTextField(
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
            Text("Noctorium Connect: find my other devices, and let them find this one", fontSize = 12.sp)
        }
        // The devices, where the music is and moving it between them are the Connect page's, a click from here.
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton({ state.navigate(Destination.CONNECT) }) {
                Icon(Icons.Default.Devices, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(connectSettingsLink(connect), fontSize = 12.sp)
            }
        }
    }
}

/** The settings' way to the Connect page, saying what is there: "Open Connect · 2 devices found". */
internal fun connectSettingsLink(connect: ConnectState): String = when (val found = connect.devices.size) {
    0 -> "Open Connect"
    1 -> "Open Connect · 1 device found"
    else -> "Open Connect · $found devices found"
}

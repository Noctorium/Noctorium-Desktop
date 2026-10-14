package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.connect.ConnectPeer
import app.noctorium.connect.ConnectState
import app.noctorium.connect.DeviceKind
import app.noctorium.connect.MusicPlace
import app.noctorium.connect.NowPlaying
import app.noctorium.core.AppState
import app.noctorium.domain.ProviderType
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.GroupBox
import app.noctorium.ui.skins.GroupLegend
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.InfoBar
import app.noctorium.ui.skins.LinearProgressIndicator
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.ScreenTitle
import app.noctorium.ui.skins.SectionTitle
import app.noctorium.ui.skins.SkinCover
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Switch
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.scrollingPage
import app.noctorium.ui.skins.skinned
import kotlinx.coroutines.delay

/*
 * The Connect page: Noctorium Connect as a place of its own, rather than a menu off the player bar.
 *
 * Where the music is comes first, large -- this computer, or the device whose remote control this one is, or one
 * playing by itself -- with what is playing there and how far in. Then every one of the listener's devices on the
 * network, each with what it is playing and one thing to do: Play here, for this computer, and Play on <name> for
 * the others, which moves the music from wherever it is to there, at the same second and with the whole queue.
 * Then who is controlling whom, a device added by its address, and, when something cannot be found, the honest
 * reasons why: the account, the Wi-Fi, a firewall, a phone that is not allowed to run in the background.
 *
 * The phone has the same page with the same sections and the same words, and both show the same picture within a
 * moment of a change anywhere: everything here is read from what the devices say about themselves.
 *
 * The decisions -- what each line says, which device has the music, what may be pressed -- are plain functions at
 * the top, apart from the drawing, so they are tested without a window.
 */

// --- Decisions ---

/** What a kind of device is called on its line: "Computer", "Phone", "Terminal". */
internal fun DeviceKind.word(): String = when (this) {
    DeviceKind.DESKTOP -> "Computer"
    DeviceKind.PHONE -> "Phone"
    DeviceKind.TERMINAL -> "Terminal"
}

/** Its picture: a screen, a phone, or the keyboard a terminal is worked from. */
internal fun DeviceKind.picture(): ImageVector = when (this) {
    DeviceKind.DESKTOP -> Icons.Default.Computer
    DeviceKind.PHONE -> Icons.Default.PhoneAndroid
    DeviceKind.TERMINAL -> Icons.Default.Keyboard
}

/** What this computer is called on its own page, where its name would be read as somebody else's. */
internal const val THIS_COMPUTER = "This computer"

/** What a device is doing, from the one thing it says: playing, paused, or the song it has not yet started. */
private fun PlaybackState.verb(): String = when (status) {
    PlaybackStatus.PLAYING -> "Playing"
    PlaybackStatus.PAUSED -> "Paused"
    PlaybackStatus.RESOLVING -> "Starting"
    else -> "Ready"
}

/**
 * The line over the song at the top of the page: where the music is and whether it is playing there -- "Playing on
 * Pocket", "Paused on this computer" -- or that there is none anywhere.
 */
internal fun musicPlaceLabel(place: MusicPlace, shown: PlaybackState): String = when (place) {
    MusicPlace.Here -> "${shown.verb()} on this computer"
    is MusicPlace.Driving -> when (shown.status) {
        PlaybackStatus.RESOLVING -> "Reaching ${place.peer.name}…"
        PlaybackStatus.IDLE, PlaybackStatus.ERROR -> "Nothing playing on ${place.peer.name}"
        else -> "${shown.verb()} on ${place.peer.name}"
    }
    is MusicPlace.Elsewhere -> (if (place.peer.nowPlaying?.playing == true) "Playing on " else "Paused on ") + place.peer.name
    MusicPlace.Nowhere -> "Nothing playing"
}

/**
 * Who has the say over the music, under the song: this computer as the other device's remote control, another
 * device driving it, or it playing by itself.
 */
internal fun musicPlaceRelation(place: MusicPlace, connect: ConnectState): String? = when (place) {
    MusicPlace.Here -> connect.controlledBy?.let { "$it is controlling this computer." }
    is MusicPlace.Driving -> "The player here is the remote control for ${place.peer.name}."
    is MusicPlace.Elsewhere -> place.peer.drivenBy?.let { driver ->
        if (driver.id == connect.thisDeviceId) "This computer is controlling it." else "${driver.name} is controlling it."
    } ?: "Playing by itself."
    MusicPlace.Nowhere -> null
}

/** The device the music is on, by id, or null for this one -- and for nowhere, which [MusicPlace.Nowhere] says. */
private fun MusicPlace.peerId(): String? = when (this) {
    is MusicPlace.Driving -> peer.id
    is MusicPlace.Elsewhere -> peer.id
    else -> null
}

/** What a device's line offers: the music brought here, or sent to it. */
internal sealed interface DeviceAction {
    data object PlayHere : DeviceAction
    data class PlayOn(val peer: ConnectPeer) : DeviceAction
}

/** One device's line on the page, decided apart from how it is drawn. */
internal data class DeviceLine(
    val name: String,
    val kind: DeviceKind,
    /** This computer's own line, which comes first. */
    val thisDevice: Boolean,
    /** What it is and who drives whom: "Phone · controlled from this computer". */
    val detail: String,
    /** What it is playing, or null: "Idle". */
    val song: NowPlaying?,
    /** How far into [song] it is, as of the moment the lines were made. */
    val positionMs: Long,
    /** The music is on this device: it is marked, and offers nothing to press. */
    val hasMusic: Boolean,
    /** Play here, or Play on <name>; null where the music already is. */
    val action: DeviceAction?,
    /** What its button says. */
    val actionLabel: String?,
    /** Whether the button may be pressed: not while the music is on its way, nor with no music anywhere to move. */
    val enabled: Boolean,
    /** Where it answers, "192.168.1.30:41234": what to add it by on a device that cannot hear it. Not for this one. */
    val address: String? = null,
)

/** The name a device has in a sentence on this page: its own, or "this computer" when it is this one. */
private fun nameFor(id: String?, name: String, connect: ConnectState): String =
    if (id != null && id == connect.thisDeviceId) "this computer" else name

/** Who drives [peer], or whom it drives, as its line says it: "controlled from Desk", "controlling this computer". */
internal fun peerRelation(peer: ConnectPeer, connect: ConnectState): String? = listOfNotNull(
    when {
        connect.target?.id == peer.id -> "controlled from this computer"
        else -> peer.drivenBy?.let { "controlled from ${nameFor(it.id, it.name, connect)}" }
    },
    peer.driving?.takeIf { connect.target?.id != peer.id }?.let { "controlling ${nameFor(it.id, it.name, connect)}" },
).joinToString(", ").ifEmpty { null }

/** The same for this computer: "controlling Pocket", "controlled from Desk". */
internal fun thisDeviceRelation(connect: ConnectState): String? = listOfNotNull(
    connect.target?.let { "controlling ${it.name}" },
    connect.controlledBy?.let { "controlled from $it" },
).joinToString(", ").ifEmpty { null }

/**
 * Every device's line: this computer first, playing [here] -- its own player, which is idle while it drives another
 * -- and then the others as they last said, their places in their songs carried on to [nowMs].
 */
internal fun deviceLines(
    connect: ConnectState,
    place: MusicPlace,
    here: NowPlaying?,
    nowMs: Long,
    /** What the player shows of the device this computer drives: its song and place, fresher than its announcements. */
    driven: NowPlaying? = null,
): List<DeviceLine> {
    val somewhere = place != MusicPlace.Nowhere
    val moving = !connect.busy && somewhere
    val thisOne = DeviceLine(
        name = connect.thisDevice.ifBlank { THIS_COMPUTER },
        kind = DeviceKind.DESKTOP,
        thisDevice = true,
        detail = listOfNotNull(THIS_COMPUTER, thisDeviceRelation(connect)).joinToString(" · "),
        song = here,
        positionMs = here?.positionMs ?: 0,
        hasMusic = place == MusicPlace.Here,
        action = if (place == MusicPlace.Here) null else DeviceAction.PlayHere,
        actionLabel = if (place == MusicPlace.Here) null else "Play here",
        enabled = moving,
    )
    val others = connect.devices.map { peer ->
        val hasMusic = place.peerId() == peer.id
        DeviceLine(
            name = peer.name,
            kind = peer.kind,
            thisDevice = false,
            detail = listOfNotNull(peer.kind.word(), peerRelation(peer, connect)).joinToString(" · "),
            song = driven?.takeIf { connect.target?.id == peer.id } ?: peer.nowPlaying,
            positionMs = driven?.takeIf { connect.target?.id == peer.id }?.positionMs ?: peer.positionAt(nowMs),
            hasMusic = hasMusic,
            action = if (hasMusic) null else DeviceAction.PlayOn(peer),
            actionLabel = if (hasMusic) null else "Play on ${peer.name}",
            enabled = moving,
            address = "${hostAndPortOf(peer.address)}:${peer.port}",
        )
    }
    return listOf(thisOne) + others
}

/** An address as it is written beside a port: an IPv6 one in brackets, so the port is not read as part of it. */
private fun hostAndPortOf(address: String): String = if (':' in address && !address.startsWith("[")) "[$address]" else address

/** This computer's own song as the other devices are told it, for its line beside theirs. */
internal fun PlaybackState.asNowPlaying(): NowPlaying? {
    val song = track ?: return null
    return NowPlaying(
        title = song.title,
        artist = song.artistLine,
        provider = song.provider.name,
        artworkUrl = song.artworkUrl,
        playing = isPlaying,
        positionMs = positionMs,
        durationMs = durationMs.takeIf { it > 0 } ?: song.durationMs ?: 0,
    )
}

/**
 * Who is controlling whom, a sentence a pair: "This computer is controlling Pocket.", "Desk is controlling Laptop."
 * Taken from this computer's own remote control, from whoever drives it, and from what every other device says of
 * itself, each pair once.
 */
internal fun controlLines(connect: ConnectState): List<String> {
    // This computer by its id where it has one, so what the others say about it is recognised as about it.
    val me = connect.thisDeviceId.ifBlank { THIS_DEVICE }
    fun called(id: String, name: String) = if (id == me) "this computer" else name
    val lines = LinkedHashMap<Pair<String, String>, String>()
    fun add(driverId: String, driverName: String, drivenId: String, drivenName: String) {
        lines.getOrPut(driverId to drivenId) {
            "${called(driverId, driverName).replaceFirstChar(Char::uppercase)} is controlling ${called(drivenId, drivenName)}."
        }
    }
    connect.target?.let { add(me, THIS_COMPUTER, it.id, it.name) }
    connect.controlledBy?.let { add(connect.controlledById ?: it, it, me, THIS_COMPUTER) }
    connect.devices.forEach { peer ->
        peer.driving?.let { add(peer.id, peer.name, it.id, it.name) }
        peer.drivenBy?.let { add(it.id, it.name, peer.id, peer.name) }
    }
    return lines.values.toList()
}

/** This computer, before Connect has given it an id: never one another device has. */
private const val THIS_DEVICE = "this-device"

/**
 * An address and a port as somebody types them -- "192.168.1.30:41234", "[fe80::1]:41234", "studio.local:41234" --
 * or null when it is not one: the port is needed, since every Noctorium answers on one of its own.
 */
internal fun parseDeviceAddress(text: String): Pair<String, Int>? {
    val typed = text.trim().removePrefix("http://")
    if (typed.isEmpty()) return null
    val (host, port) = if (typed.startsWith("[")) {
        val end = typed.indexOf("]:")
        if (end < 0) return null
        typed.substring(1, end) to typed.substring(end + 2)
    } else {
        val colon = typed.lastIndexOf(':')
        if (colon <= 0 || typed.count { it == ':' } > 1) return null
        typed.substring(0, colon) to typed.substring(colon + 1)
    }
    val number = port.trimEnd('/').toIntOrNull()?.takeIf { it in 1..65_535 } ?: return null
    if (host.isBlank() || host.any { it.isWhitespace() }) return null
    return host to number
}

/** One of the reasons a device may not be found, and whether it is known to be fine here. */
internal data class ConnectCheck(
    val title: String,
    val detail: String,
    /** True once known to be fine on this computer; false known not to be; null for what only the listener can check. */
    val fine: Boolean?,
    val kind: CheckKind,
)

internal enum class CheckKind { ACCOUNT, SWITCHED_OFF, WIFI, FIREWALL, PHONE }

/**
 * Why a device may not be found, in the order worth checking -- the same honest reasons Connect has always given:
 * the account, Connect switched off here, the Wi-Fi, the firewall on a Windows computer, and a phone not allowed to
 * run in the background.
 */
internal fun connectChecks(connect: ConnectState, enabledHere: Boolean): List<ConnectCheck> = buildList {
    when {
        !enabledHere -> add(
            ConnectCheck(
                "Connect is switched off on this computer",
                "Switch Noctorium Connect on at the top of this page, and the others can find it and be found from it.",
                false,
                CheckKind.SWITCHED_OFF,
            ),
        )
        !connect.available -> add(
            ConnectCheck(
                "Sign in to your Noctorium account",
                "Your devices find each other through it: every one of them signed in to the same account.",
                false,
                CheckKind.ACCOUNT,
            ),
        )
        else -> add(
            ConnectCheck(
                "Signed in to your Noctorium account",
                "Your other devices have to be signed in to the same one.",
                true,
                CheckKind.ACCOUNT,
            ),
        )
    }
    add(
        ConnectCheck(
            "On the same Wi-Fi",
            "Every device on the same network. A guest network keeps its devices apart, and they cannot see each other there.",
            null,
            CheckKind.WIFI,
        ),
    )
    add(
        ConnectCheck(
            "Let through the firewall on Windows",
            FIREWALL_ADVICE,
            null,
            CheckKind.FIREWALL,
        ),
    )
    add(
        ConnectCheck(
            "On a phone, allowed to run in the background",
            "A phone is found while Noctorium is open on it, and plays music sent to it with the screen off once " +
                "Android lets Noctorium run in the background.",
            null,
            CheckKind.PHONE,
        ),
    )
}

/** What Connect last had to say, unless the strip on the player bar says it already ("Playing on Pocket."). */
internal fun connectNews(connect: ConnectState): String? {
    val message = connect.message?.takeIf { it.isNotBlank() } ?: return null
    val said = connectStripText(connect)
    return message.takeUnless { said != null && it.trimEnd('.') == said.trimEnd('.') }
}

/**
 * What to do about [news], where Connect has said what went wrong and there is something to do: a computer that did
 * not answer is nearly always its firewall, and Windows asks about it in a way that leaves most home networks out.
 */
internal fun connectAdvice(news: String): String? = when {
    "firewall" in news -> FIREWALL_ADVICE
    else -> null
}

/** The firewall on Windows, said the same everywhere Connect says it. */
internal const val FIREWALL_ADVICE = "When Windows asks about Noctorium, allow it on the network you are on. Windows calls many home networks " +
    "Public, and its question ticks only Private: tick Public too, or set the network to Private."

/** The heading over the devices, with how many there are: "Devices on this network · 3". */
internal fun devicesHeading(connect: ConnectState): String = "Devices on this network · ${connect.devices.size + 1}"

// --- The page ---

/**
 * The Connect page. [shown] is what the player shows -- this computer's playback, or the driven device's -- which is
 * what the top of the page is about while the music is here or driven from here.
 */
@Composable
internal fun ConnectScreen(shown: PlaybackState, state: AppState) {
    val connect by state.connect.collectAsState()
    val place by state.musicPlace.collectAsState()
    val local by state.playback.collectAsState()
    val enabled = state.settings.collectAsState().value.preferences.connect.enabled
    // The other devices' places in their songs, carried on between what they say: once a second is enough for a
    // clock that shows seconds.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    ConnectPage(connect, place, shown, local, enabled, now, state)
}

@Composable
internal fun ConnectPage(
    connect: ConnectState,
    place: MusicPlace,
    shown: PlaybackState,
    local: PlaybackState,
    enabledHere: Boolean,
    nowMs: Long,
    state: AppState,
) {
    val classic = skinned()
    Column(
        Modifier
            .fillMaxSize()
            .scrollingPage()
            .padding(horizontal = if (classic) 18.dp else 32.dp)
            .padding(top = if (classic) 16.dp else 28.dp, bottom = chromeBottom() + 32.dp),
    ) {
        Column(Modifier.widthIn(max = 940.dp)) {
            ScreenTitle("Noctorium Connect", 32.sp)
            Text(
                "Where the music is, and every device on this network that can play it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(if (classic) 14.dp else 20.dp))
            ConnectSwitch(connect, enabledHere, state)
            if (!enabledHere) {
                // Off is a choice, not a fault: a quiet page that says so, rather than the list of what to check.
                Spacer(Modifier.height(if (classic) 14.dp else 20.dp))
                ConnectOff()
                Spacer(Modifier.height(if (classic) 14.dp else 24.dp))
                ThisDeviceFooter(connect, enabledHere = false, state)
                return@Column
            }
            connectNews(connect)?.let { news ->
                Spacer(Modifier.height(14.dp))
                ConnectNotice(news, state::dismissConnectMessage)
            }
            Spacer(Modifier.height(if (classic) 14.dp else 20.dp))
            MusicPlaceCard(place, connect, shown, nowMs, state)

            val lines = deviceLines(connect, place, local.asNowPlaying(), nowMs, driven = shown.asNowPlaying()?.takeIf { place is MusicPlace.Driving })
            val checks = connectChecks(connect, enabledHere)
            if (connect.available) {
                Spacer(Modifier.height(if (classic) 18.dp else 30.dp))
                SectionTitle(devicesHeading(connect), 18.sp)
                Spacer(Modifier.height(10.dp))
                DeviceList(lines, state)
                if (connect.devices.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Nothing else found yet. Open Noctorium on your phone or another computer, on this Wi-Fi, " +
                            "signed in to the same account, and it shows up here within a few seconds.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                controlLines(connect).takeIf { it.isNotEmpty() }?.let { control ->
                    Spacer(Modifier.height(if (classic) 18.dp else 28.dp))
                    SectionTitle("Who is controlling whom", 18.sp)
                    Spacer(Modifier.height(10.dp))
                    ControlCard(control)
                }
            }
            Spacer(Modifier.height(if (classic) 18.dp else 28.dp))
            // Open while there is nothing to see, which is when it is wanted; a click away the rest of the time.
            HelpCard(checks, open = connect.devices.isEmpty(), state)
            if (connect.available) {
                Spacer(Modifier.height(if (classic) 18.dp else 28.dp))
                AddByAddress(state)
            }
            Spacer(Modifier.height(if (classic) 14.dp else 24.dp))
            ThisDeviceFooter(connect, enabledHere, state)
        }
    }
}

/** What the switch at the top of the page says Connect does, on or off. */
internal fun connectSwitchLine(enabled: Boolean, connect: ConnectState): String = when {
    !enabled -> "Off: this computer is not looking for your other devices, and they cannot see it."
    connect.target != null || connect.controlledBy != null ->
        "This computer finds your other devices, and they can find it. Switching it off ends the remote control."
    else -> "This computer finds your other devices, and they can find it."
}

/**
 * Connect on or off, at the top of its own page, where somebody looking for the switch looks. Off stops it outright --
 * nothing is said on the network or listened for -- and the remote control with it, either way round; the music
 * itself plays on wherever it is.
 */
@Composable
private fun ConnectSwitch(connect: ConnectState, enabledHere: Boolean, state: AppState) {
    val content: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Hub,
                null,
                Modifier.size(20.dp),
                tint = if (enabledHere) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Noctorium Connect", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(connectSwitchLine(enabledHere, connect), fontSize = 12.sp, color = ink(.62f))
            }
            Spacer(Modifier.width(12.dp))
            Switch(enabledHere, state::setConnectEnabled)
        }
    }
    if (skinned()) {
        GroupBox(Modifier.fillMaxWidth(), content = content)
        return
    }
    Surface(shape = RoundedCornerShape(16.dp), color = ink(.045f), border = BorderStroke(1.dp, ink(.07f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), content = content)
    }
}

/** The page while Connect is off: what that means, with the switch above to turn it back on. */
@Composable
private fun ConnectOff() {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.PowerSettingsNew, null, Modifier.size(40.dp), tint = ink(.4f))
        Spacer(Modifier.height(12.dp))
        Text("Noctorium Connect is off", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "This computer plays only its own music, and your phone and other computers do not see it. " +
                "Switch Connect on above to find them again.",
            color = ink(.62f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 520.dp),
        )
    }
}

/** Connect's news across the page: a pale strip with a cross, as the library's notices are. */
@Composable
private fun ConnectNotice(news: String, dismiss: () -> Unit) {
    val advice = connectAdvice(news)
    if (skinned()) {
        InfoBar(Icons.Default.Info, listOfNotNull(news, advice).joinToString(" "), MaterialTheme.colorScheme.primary, dismiss)
        return
    }
    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(news, fontSize = 13.sp)
                advice?.let { Text(it, fontSize = 12.sp, color = ink(.68f)) }
            }
            IconButton(dismiss, Modifier.size(34.dp)) { Icon(Icons.Default.Close, "Dismiss", Modifier.size(16.dp)) }
        }
    }
}

// --- Where the music is ---

/**
 * The top of the page: the song where the music is, large -- its cover, its title and artist, playing or paused, how
 * far in and moving -- the device it is on, who is driving it, and what can be done from here: bring it here, or let
 * go of it and leave it playing there. Over the colours of the cover, as the Spotlight bar is.
 */
@Composable
private fun MusicPlaceCard(place: MusicPlace, connect: ConnectState, shown: PlaybackState, nowMs: Long, state: AppState) {
    val classic = skinned()
    // What is playing where the music is: the player's own while it is here or driven from here, and otherwise what
    // that device last said, carried on by the clock.
    val elsewhere = (place as? MusicPlace.Elsewhere)?.peer
    val song: NowPlaying? = when (place) {
        MusicPlace.Here, is MusicPlace.Driving -> shown.asNowPlaying()
        is MusicPlace.Elsewhere -> place.peer.nowPlaying
        MusicPlace.Nowhere -> null
    }
    val position = elsewhere?.positionAt(nowMs) ?: shown.positionMs
    val duration = song?.durationMs ?: 0
    val kind = when (place) {
        is MusicPlace.Driving -> place.peer.kind
        is MusicPlace.Elsewhere -> place.peer.kind
        else -> DeviceKind.DESKTOP
    }
    val label = musicPlaceLabel(place, shown)
    val relation = musicPlaceRelation(place, connect)
    val provider = song?.provider?.let { name -> runCatching { ProviderType.valueOf(name) }.getOrNull() } ?: ProviderType.LOCAL
    val track = shown.track?.takeIf { place == MusicPlace.Here || place is MusicPlace.Driving }

    val body: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(kind.picture(), null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(7.dp))
            Text(
                if (classic) label else label.uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (classic) 12.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (classic) 0.sp else 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(if (classic) 4.dp else 6.dp))
        Text(
            song?.title ?: "Nothing playing",
            fontSize = if (classic) 20.sp else 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = if (classic) 24.sp else 33.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            song?.artist?.takeIf { it.isNotBlank() }
                ?: if (song == null) "Play something here or on any of your devices, and it shows here." else "",
            color = ink(.7f),
            fontSize = if (classic) 12.sp else 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (song != null) {
            Spacer(Modifier.height(if (classic) 10.dp else 16.dp))
            SongProgress(position, duration, playing = song.playing)
        }
        relation?.let {
            Spacer(Modifier.height(if (classic) 8.dp else 12.dp))
            Text(it, color = ink(.62f), fontSize = 12.sp)
        }
        val canBringHere = place is MusicPlace.Driving || place is MusicPlace.Elsewhere
        if (canBringHere || connect.busy) {
            Spacer(Modifier.height(if (classic) 10.dp else 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (connect.busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Sending the music…", fontSize = 13.sp, color = ink(.75f))
                } else {
                    Button(state::playHere) {
                        Icon(Icons.Default.Computer, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play here")
                    }
                    if (place is MusicPlace.Driving) {
                        OutlinedButton(state::stopControlling) { Text("Leave it playing") }
                    }
                }
            }
        }
    }

    if (classic) {
        GroupBox(Modifier.fillMaxWidth()) {
            GroupLegend("Where the music is")
            Row(verticalAlignment = Alignment.Top) {
                if (track != null) {
                    SkinCover(track, 112.dp)
                } else {
                    Box(Modifier.size(112.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (song?.artworkUrl != null) {
                            RemoteArtwork(song.artworkUrl, provider, Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Default.MusicNote, null, Modifier.size(40.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), content = body)
            }
        }
        return
    }

    val fallback = ArtworkPalette(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    val palette = rememberArtworkPalette(song?.artworkUrl, provider, fallback)
    val lead by animateColorAsState(palette.primary, tween(700), label = "connectLead")
    val deep by animateColorAsState(palette.secondary, tween(700), label = "connectDeep")
    val strength = if (song == null) .18f else .42f
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ink(.045f),
        border = BorderStroke(1.dp, ink(.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.background(Brush.horizontalGradient(listOf(lead.copy(alpha = strength), deep.copy(alpha = strength * .55f), Color.Transparent)))) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(22.dp)) {
                val cover = if (maxWidth < 560.dp) 132.dp else 184.dp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(cover).clip(RoundedCornerShape(16.dp))) {
                        if (song != null) {
                            MotionContent(song.artworkUrl, Modifier.fillMaxSize(), kind = MotionKind.TRACK) { url ->
                                RemoteArtwork(url, provider, Modifier.fillMaxSize())
                            }
                        } else {
                            Box(Modifier.fillMaxSize().background(ink(.07f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, Modifier.size(56.dp), tint = ink(.35f))
                            }
                        }
                    }
                    Spacer(Modifier.width(24.dp))
                    Column(Modifier.weight(1f), content = body)
                }
            }
        }
    }
}

/** How far in, as a line part lit with the times after it, moving while it plays. Read only: the player seeks. */
@Composable
private fun SongProgress(positionMs: Long, durationMs: Long, playing: Boolean) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (playing) Icons.Default.PlayArrow else Icons.Default.Pause,
            if (playing) "Playing" else "Paused",
            Modifier.size(16.dp),
            tint = ink(.7f),
        )
        Spacer(Modifier.width(8.dp))
        if (skinned()) {
            LinearProgressIndicator({ fraction }, Modifier.weight(1f).height(14.dp))
        } else {
            val lit = MaterialTheme.colorScheme.primary
            val groove = ink(.14f)
            Canvas(Modifier.weight(1f).height(4.dp)) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(groove, cornerRadius = radius)
                drawRoundRect(lit, size = Size(size.width * fraction, size.height), cornerRadius = radius)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${formatPlaybackTime(positionMs)} / ${if (durationMs > 0) formatPlaybackTime(durationMs) else UNKNOWN_PLAYBACK_TIME}",
            color = ink(.7f),
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

// --- The devices ---

@Composable
private fun DeviceList(lines: List<DeviceLine>, state: AppState) {
    if (skinned()) {
        GroupBox(Modifier.fillMaxWidth()) {
            lines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                DeviceRow(line, state)
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val accent = MaterialTheme.colorScheme.primary
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (line.hasMusic) accent.copy(alpha = .13f) else ink(.045f),
                border = BorderStroke(1.dp, if (line.hasMusic) accent.copy(alpha = .55f) else ink(.07f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { DeviceRow(line, state) }
            }
        }
    }
}

/**
 * One device: its picture and name, what it is and who drives whom, what it is playing and how far in -- or Idle --
 * and its one action. Wide, in three columns; narrower, the song goes under the name.
 */
@Composable
private fun DeviceRow(line: DeviceLine, state: AppState) {
    val classic = skinned()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 680.dp
        Row(verticalAlignment = Alignment.CenterVertically) {
            DeviceBadge(line.kind, line.hasMusic, if (classic) 32.dp else 42.dp)
            Spacer(Modifier.width(if (classic) 10.dp else 14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        line.name,
                        fontSize = if (classic) 13.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // Where it answers, quietly: what to type on a device that cannot hear it, under Add a device.
                    line.address?.let { address ->
                        Spacer(Modifier.width(8.dp))
                        Text(address, color = ink(.38f), fontSize = 11.sp, maxLines = 1)
                    }
                }
                Text(line.detail, color = ink(.55f), fontSize = if (classic) 11.sp else 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!wide) {
                    Spacer(Modifier.height(6.dp))
                    DeviceSong(line)
                }
            }
            if (wide) {
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(300.dp)) { DeviceSong(line) }
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.widthIn(min = if (wide) 150.dp else 0.dp), contentAlignment = Alignment.CenterEnd) { DeviceActionButton(line, state) }
        }
    }
}

/** The device's picture in a round tile, lit in the accent on the one the music is on. */
@Composable
private fun DeviceBadge(kind: DeviceKind, lit: Boolean, size: Dp) {
    val accent = MaterialTheme.colorScheme.primary
    if (skinned()) {
        Icon(kind.picture(), null, Modifier.size(size), tint = if (lit) accent else MaterialTheme.colorScheme.onSurface)
        return
    }
    Box(
        Modifier.size(size).clip(CircleShape).background(if (lit) accent.copy(alpha = .22f) else ink(.07f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(kind.picture(), null, Modifier.size(size * .52f), tint = if (lit) accent else ink(.75f))
    }
}

/** What a device is playing: a small cover, the song over its artist, playing or paused and how far in; or Idle. */
@Composable
private fun DeviceSong(line: DeviceLine) {
    val song = line.song
    if (song == null) {
        Text("Idle", color = ink(.45f), fontSize = 12.sp)
        return
    }
    val provider = song.provider?.let { name -> runCatching { ProviderType.valueOf(name) }.getOrNull() } ?: ProviderType.LOCAL
    Row(verticalAlignment = Alignment.CenterVertically) {
        RemoteArtwork(song.artworkUrl, provider, Modifier.size(36.dp).clip(RoundedCornerShape(if (skinned()) 0.dp else 7.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(song.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (song.playing) Icons.Default.PlayArrow else Icons.Default.Pause,
                    if (song.playing) "Playing" else "Paused",
                    Modifier.size(12.dp),
                    tint = ink(.55f),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    listOfNotNull(
                        song.artist.takeIf { it.isNotBlank() },
                        formatPlaybackTime(line.positionMs) + (song.durationMs.takeIf { it > 0 }?.let { " / ${formatPlaybackTime(it)}" } ?: ""),
                    ).joinToString(" · "),
                    color = ink(.55f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Play here, Play on <name>, or -- on the device the music is on -- a mark saying so. */
@Composable
private fun DeviceActionButton(line: DeviceLine, state: AppState) {
    if (line.hasMusic) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text("The music is here", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        return
    }
    val action = line.action ?: return
    val press: () -> Unit = {
        when (action) {
            DeviceAction.PlayHere -> state.playHere()
            is DeviceAction.PlayOn -> state.playOn(action.peer)
        }
    }
    OutlinedButton(press, enabled = line.enabled) {
        Text(line.actionLabel.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
    }
}

/** Who is controlling whom, a line each, beside the picture of a remote control's signal. */
@Composable
private fun ControlCard(lines: List<String>) {
    val content: @Composable ColumnScope.() -> Unit = {
        lines.forEach { line ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cast, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(line, fontSize = 13.sp)
            }
        }
    }
    if (skinned()) {
        GroupBox(Modifier.fillMaxWidth(), content = content)
        return
    }
    Surface(shape = RoundedCornerShape(16.dp), color = ink(.045f), border = BorderStroke(1.dp, ink(.07f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), content = content)
    }
}

// --- When a device is not found ---

/**
 * Why a device may not be there, as a list to go down: each reason with a tick once it is known to be fine here, and
 * the thing to press where there is one -- signing in, switching Connect on. Open when nothing is found; a heading
 * to open it otherwise.
 */
@Composable
private fun HelpCard(checks: List<ConnectCheck>, open: Boolean, state: AppState) {
    var shown by remember(open) { mutableStateOf(open) }
    val content: @Composable ColumnScope.() -> Unit = {
        checks.forEachIndexed { index, check ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    when (check.kind) {
                        CheckKind.ACCOUNT -> if (check.fine == true) Icons.Default.CheckCircle else Icons.Default.AccountCircle
                        CheckKind.SWITCHED_OFF -> Icons.Default.PowerSettingsNew
                        CheckKind.WIFI -> Icons.Default.Hub
                        CheckKind.FIREWALL -> Icons.Default.Security
                        CheckKind.PHONE -> Icons.Default.PhoneAndroid
                    },
                    null,
                    Modifier.size(20.dp),
                    tint = when (check.fine) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.error
                        null -> ink(.6f)
                    },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(check.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(check.detail, fontSize = 12.sp, color = ink(.62f))
                    when (check.kind) {
                        CheckKind.ACCOUNT -> if (check.fine == false) {
                            Spacer(Modifier.height(6.dp))
                            Button({ openSettingsPage(state, SettingsPage.ACCOUNT) }) { Text("Sign in") }
                        }
                        CheckKind.SWITCHED_OFF -> {
                            Spacer(Modifier.height(6.dp))
                            Button({ state.setConnectEnabled(true) }) { Text("Switch it on") }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
    val heading = "Not finding a device?"
    if (skinned()) {
        GroupBox(Modifier.fillMaxWidth()) {
            GroupLegend(heading)
            if (shown) content() else TextButton({ shown = true }) { Text("Show why a device may not be found") }
        }
        return
    }
    Surface(shape = RoundedCornerShape(16.dp), color = ink(.045f), border = BorderStroke(1.dp, ink(.07f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(heading, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton({ shown = !shown }, Modifier.size(32.dp)) {
                    Icon(if (shown) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (shown) "Hide" else "Show", Modifier.size(20.dp))
                }
            }
            if (shown) {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

/**
 * One of the listener's devices looked for at an address as well as by its announcements: for a network that keeps
 * the announcements from getting from one device to the other. Only a device on the same account answers.
 */
@Composable
private fun AddByAddress(state: AppState) {
    var text by remember { mutableStateOf("") }
    var said by remember { mutableStateOf<String?>(null) }
    var wrong by remember { mutableStateOf(false) }
    val add = {
        val parsed = parseDeviceAddress(text)
        if (parsed == null) {
            wrong = true
        } else {
            state.addConnectAddress(parsed.first, parsed.second)
            said = "Looking for a device at ${text.trim()}. It appears above once it answers."
            text = ""
            wrong = false
        }
    }
    SectionTitle("Add a device by its address", 18.sp)
    Spacer(Modifier.height(6.dp))
    Text(
        "For a network that keeps devices from hearing each other. Each device's address is on its line above, on " +
            "any device that can see it. Only a device signed in to your account answers.",
        color = ink(.6f),
        fontSize = 12.sp,
    )
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            text,
            { text = it; wrong = false },
            label = { Text("Address and port") },
            placeholder = { Text("192.168.1.30:41234") },
            singleLine = true,
            isError = wrong,
            modifier = Modifier.widthIn(max = 340.dp).weight(1f, fill = false).tracksTyping(),
        )
        Spacer(Modifier.width(10.dp))
        Button(add, enabled = text.isNotBlank()) {
            Icon(Icons.Default.Add, null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add")
        }
    }
    val note = if (wrong) "Write it as an address and a port, such as 192.168.1.30:41234." else said
    note?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, color = if (wrong) MaterialTheme.colorScheme.error else ink(.6f), fontSize = 12.sp)
    }
}

/** What this computer is called on the others, and the settings where it is renamed or kept from being found. */
@Composable
private fun ThisDeviceFooter(connect: ConnectState, enabledHere: Boolean, state: AppState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            when {
                !enabledHere -> "Your other devices cannot find this computer while Connect is switched off."
                connect.available -> "This computer is “${connect.thisDevice}” to your other devices."
                else -> "Once you are signed in, this computer shows up on your other devices."
            },
            color = ink(.55f),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        TextButton({ openSettingsPage(state, SettingsPage.ACCOUNT, "Noctorium Connect") }) {
            Text("Connect settings", fontSize = 12.sp)
        }
    }
}

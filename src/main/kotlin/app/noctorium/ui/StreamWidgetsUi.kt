package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.platform.DesktopBridge
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Slider
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Switch
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.listWell
import app.noctorium.ui.skins.scrollingPage
import app.noctorium.ui.skins.skinCorners
import app.noctorium.ui.skins.skinned
import app.noctorium.widgets.StreamWidgetPreferences
import app.noctorium.widgets.StreamWidgetStatus
import app.noctorium.widgets.StreamWidgetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/*
 * The stream widgets' page in Settings.
 *
 * A stream widget is a page Noctorium serves on this computer, which OBS, Streamlabs and the rest lay over a stream
 * as a "Browser source", saying what is playing. The server and the seven looks are the core's (see
 * [StreamWidgetServer]); this page switches them on, says whether they are running and where, shows each look with
 * its own address to copy into the streaming program, and holds the few options every look shares.
 *
 * Off until switched on, since they are a web server, however small. The addresses are shown while they are off
 * too, dimmed, so that OBS can be set up first; nothing is opened in the browser until there is something to open.
 */

// --- Decisions ---

/**
 * What the page and its tile go by: the status as it is, except in the moment between the switch moving and the
 * server following it -- switched on and not yet starting is starting, and switched off is off -- so that the page
 * never says the opposite of the switch beside it.
 */
internal fun streamWidgetsShown(widgets: StreamWidgetPreferences, status: StreamWidgetStatus): StreamWidgetStatus = when {
    !widgets.enabled -> StreamWidgetStatus.Off
    status == StreamWidgetStatus.Off -> StreamWidgetStatus.Starting(widgets.portInUse)
    else -> status
}

/** The line under Stream widgets on the Settings list: "Off", or the look the plain address shows and where. */
internal fun streamWidgetsSummary(widgets: StreamWidgetPreferences, status: StreamWidgetStatus): String =
    when (val shown = streamWidgetsShown(widgets, status)) {
        StreamWidgetStatus.Off -> "Off"
        is StreamWidgetStatus.Starting -> "Starting on port ${shown.port}…"
        is StreamWidgetStatus.Running -> "On · ${widgets.theme.displayName} · ${shown.address.removePrefix("http://")}" +
            if (shown.networkAddress != null) " · on the network too" else ""
        is StreamWidgetStatus.Failed -> "On, but could not start on port ${shown.port}"
    }

/** What the top of the page says about the server, beside the switch: off, starting, where it is, or why it is not. */
internal fun streamWidgetsStatusLine(status: StreamWidgetStatus): String = when (status) {
    StreamWidgetStatus.Off -> "Off. Nothing on this computer is listening for a streaming program."
    is StreamWidgetStatus.Starting -> "Starting on port ${status.port}…"
    is StreamWidgetStatus.Running -> "Running at ${status.address}"
    is StreamWidgetStatus.Failed -> status.reason
}

/**
 * Why [typed] cannot be the widgets' port, in a sentence for under the box, or null when it can be. Whether another
 * program already has it is only known by trying, which the status at the top says once it has been tried.
 */
internal fun streamWidgetPortProblem(typed: String): String? {
    val digits = typed.trim()
    val ports = StreamWidgetPreferences.PORTS
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

/**
 * [theme]'s address on this computer: where it is being served, or -- while the widgets are not running -- where it
 * will be once they are, which is already what goes into OBS.
 */
internal fun streamWidgetAddress(widgets: StreamWidgetPreferences, status: StreamWidgetStatus, theme: StreamWidgetTheme): String =
    (status as? StreamWidgetStatus.Running)?.url(theme)
        ?: "http://$LOOPBACK:${widgets.portInUse}/widget/${theme.id}"

/** Where the core's server listens, unless the network is allowed: this computer, and nothing else. */
private const val LOOPBACK = "127.0.0.1"

/** The size to give [theme]'s browser source: its own, or, for the ticker, which runs the width it is given, any width. */
internal fun streamWidgetSourceSize(theme: StreamWidgetTheme): String =
    if (theme.runsTheWidth) "${theme.width} × ${theme.height}, or any width" else "${theme.width} × ${theme.height}"

/**
 * A look many times wider than it is tall -- the ticker, a bar for the edge of the screen -- whose picture is too thin
 * to read at half a row, and which the gallery gives a row of its own.
 */
internal val StreamWidgetTheme.runsTheWidth: Boolean get() = width >= height * 6

/**
 * The looks as the gallery lays them out, [columns] to a row, in their own order -- except that one that
 * [runsTheWidth] has a row to itself, put after the row it would have been in, so that no row is left with a gap.
 */
internal fun streamWidgetGalleryRows(
    columns: Int,
    looks: List<StreamWidgetTheme> = StreamWidgetTheme.entries,
): List<List<StreamWidgetTheme>> {
    val rows = mutableListOf<List<StreamWidgetTheme>>()
    val row = mutableListOf<StreamWidgetTheme>()
    val waiting = ArrayDeque<StreamWidgetTheme>()
    for (look in looks) {
        if (columns > 1 && look.runsTheWidth) waiting += look else row += look
        if (row.size == columns.coerceAtLeast(1)) {
            rows += row.toList()
            row.clear()
        }
        if (row.isEmpty()) while (waiting.isNotEmpty()) rows += listOf(waiting.removeFirst())
    }
    if (row.isNotEmpty()) rows += row.toList()
    waiting.forEach { rows += listOf(it) }
    return rows
}

/**
 * What the network option says under itself once it is on: the address another computer uses, that there is none
 * for want of a network, or that there will be one once the widgets are running.
 */
internal fun streamWidgetNetworkLine(widgets: StreamWidgetPreferences, status: StreamWidgetStatus): String? {
    if (!widgets.allowNetwork) return null
    val running = status as? StreamWidgetStatus.Running
    return when {
        running?.networkAddress != null ->
            "Another computer on your network opens them at ${running.networkAddress}: each look's address for it is under its own."
        running != null -> "This computer is not on a network just now, so there is no address for another computer to use."
        else -> "Once the widgets are running, the address for another computer is shown here."
    }
}

/** The gallery's note while its addresses lead nowhere yet, or null once they lead to the widgets. */
internal fun streamWidgetAddressNote(widgets: StreamWidgetPreferences, status: StreamWidgetStatus): String? =
    when (streamWidgetsShown(widgets, status)) {
        StreamWidgetStatus.Off -> "These addresses work once stream widgets are switched on, above. They can go into OBS already."
        is StreamWidgetStatus.Failed -> "These addresses work once the widgets have started: see above for why they have not."
        else -> null
    }

/** How long a pause hides a widget after, as the slider's label says it. */
internal fun streamWidgetHideAfterLabel(seconds: Int): String =
    "Hide after $seconds ${if (seconds == 1) "second" else "seconds"} of pause"

// The settings on the page that are drawn some other way than the shared controls, by the names the search finds.
internal const val STREAM_WIDGETS_SWITCH = "Show what is playing on your stream"
internal const val STREAM_WIDGET_HIDE_AFTER = "How long a pause hides it after"
internal const val STREAM_WIDGET_PORT = "Port"
internal const val STREAM_WIDGET_OVERRIDES = "One widget's own settings"

// --- The Settings page ---

/** The page, following the widgets' status as the core reports it. */
@Composable
internal fun StreamWidgetsSettingsPanel(widgets: StreamWidgetPreferences, state: AppState) {
    val status by state.streamWidgets.collectAsState()
    StreamWidgetsSettingsPanel(widgets, status, state)
}

/**
 * The page with [status] given, which is how its pictures are drawn without a server listening anywhere: the switch
 * and what the server is doing; the looks, each with its address; what every widget shows; where they are served;
 * and how one goes into OBS.
 */
@Composable
internal fun StreamWidgetsSettingsPanel(widgets: StreamWidgetPreferences, status: StreamWidgetStatus, state: AppState) {
    val shown = streamWidgetsShown(widgets, status)
    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            SettingAnchor(STREAM_WIDGETS_SWITCH) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tv, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(STREAM_WIDGETS_SWITCH, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "A page on this computer that OBS or Streamlabs lays over your stream, saying what Noctorium is playing.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(widgets.enabled, state::setStreamWidgetsEnabled)
                }
            }
            Spacer(Modifier.height(14.dp))
            WidgetServerStatus(shown, state)
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Image, "Widget looks")
            Spacer(Modifier.height(7.dp))
            Text(
                "Seven of them, each at its own address, drawn in your accent colour from Theme and colours. A stream can " +
                    "carry several at once. The default is also at ${streamWidgetAddress(widgets, shown, widgets.theme).substringBefore("/widget")}/widget, " +
                    "with no look named.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            WidgetGallery(widgets, shown, state)
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Tune, "What the widgets show")
            Spacer(Modifier.height(7.dp))
            Text(
                "Every widget, in every look, unless its own address says otherwise. A change reaches a widget already on " +
                    "your stream within a second, with nothing to refresh in OBS.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                "Show the cover",
                "The song's cover, in the looks with room for one. Minimal and Ticker are words only.",
                widgets.showCover,
            ) { state.setStreamWidgetCover(it) }
            Spacer(Modifier.height(10.dp))
            ToggleRow("Show how far into the song", "A thin bar of the song's progress, moving on smoothly.", widgets.showProgress) {
                state.setStreamWidgetProgress(it)
            }
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                "Show the service it comes from",
                "The service's logo: Spotify's for a Spotify song, even one played from its match on YouTube Music.",
                widgets.showServiceLogo,
            ) { state.setStreamWidgetServiceLogo(it) }
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                "Hide while paused",
                "The widget fades away once the music has been paused a moment, and comes back with it: a caption for " +
                    "a song nobody can hear says nothing.",
                widgets.hideWhenPaused,
            ) { state.setStreamWidgetHideWhenPaused(it) }
            Spacer(Modifier.height(8.dp))
            HideAfter(widgets, state)
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Public, "Where they are served")
            Spacer(Modifier.height(12.dp))
            WidgetPortField(widgets, state)
            Spacer(Modifier.height(14.dp))
            ToggleRow(
                "Allow other devices on the network",
                "For a second computer that does the streaming, on the same network as this one. Off, only this " +
                    "computer can open the widgets.",
                widgets.allowNetwork,
            ) { state.setStreamWidgetAllowNetwork(it) }
            streamWidgetNetworkLine(widgets, shown)?.let { line ->
                Spacer(Modifier.height(8.dp))
                Text(line, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(FIREWALL_ADVICE, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        AddingToObs(widgets, shown)
    }
}

/**
 * What the server is doing, under the switch: nothing, starting, running at its address, or not running and why --
 * in the error colour, with a way to try the same port again once whatever had it has let it go, and a way to the
 * port, to choose another.
 */
@Composable
private fun WidgetServerStatus(status: StreamWidgetStatus, state: AppState) {
    val failed = status is StreamWidgetStatus.Failed
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 1.dp).size(18.dp), contentAlignment = Alignment.Center) {
            when (status) {
                StreamWidgetStatus.Off -> Icon(Icons.Default.PowerSettingsNew, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                is StreamWidgetStatus.Starting -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                is StreamWidgetStatus.Running -> Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                is StreamWidgetStatus.Failed -> Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            SelectionContainer {
                Text(
                    streamWidgetsStatusLine(status),
                    color = when {
                        failed -> MaterialTheme.colorScheme.error
                        status == StreamWidgetStatus.Off -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 12.sp,
                    fontWeight = if (status is StreamWidgetStatus.Running) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            val network = (status as? StreamWidgetStatus.Running)?.networkAddress
            if (network != null) {
                SelectionContainer {
                    Text("and at $network for the network", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            if (failed) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(state::retryStreamWidgets) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Try again")
                    }
                    TextButton({ openSettingsPage(state, SettingsPage.STREAM_WIDGETS, STREAM_WIDGET_PORT) }) { Text("Choose another port") }
                }
            }
        }
    }
}

/**
 * The desktop's clipboard and browser, as the core reaches them, for a widget's address: the core has nothing that
 * copies or opens an address it did not make itself.
 */
private val bridge = DesktopBridge()

/** How long a Copy button says it copied, before it is a Copy button again. */
private const val COPIED_FOR_MS = 1_800L

/**
 * The seven looks, each with its picture, what it is, its size in OBS, its address to copy or open and the way to
 * make it the default: two to a row where there is room, one where there is not, the ticker always a row of its own.
 * Each row's pictures are framed alike, so a tall look beside a wide one is a picture in a taller frame, not a card
 * that has come apart.
 */
@Composable
private fun WidgetGallery(widgets: StreamWidgetPreferences, status: StreamWidgetStatus, state: AppState) {
    var copied by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(copied) {
        if (copied == null) return@LaunchedEffect
        delay(COPIED_FOR_MS)
        copied = null
    }
    val copy: (String) -> Unit = { address ->
        runCatching { bridge.copyToClipboard(address) }
            .onSuccess { copied = address; problem = null }
            .onFailure { problem = "Could not reach the clipboard. The address is $address" }
    }
    val open: (String) -> Unit = { address ->
        scope.launch(Dispatchers.IO) {
            runCatching { bridge.openOwnPage(address) }
                .onSuccess { problem = null }
                .onFailure { problem = it.message ?: "No browser opened. Copy the address and paste it into one." }
        }
    }
    val running = status is StreamWidgetStatus.Running
    val note = streamWidgetAddressNote(widgets, status)
    Column {
        if (note != null || problem != null) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (problem != null) Icons.Default.ErrorOutline else Icons.Default.Info,
                    null,
                    Modifier.size(15.dp),
                    tint = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    problem ?: note.orEmpty(),
                    color = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val whole = maxWidth
            val gap = if (skinned()) 8.dp else 12.dp
            val columns = if (whole >= TILE_MIN_WIDTH * 2 + gap) 2 else 1
            val half = (whole - gap * (columns - 1)) / columns
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                streamWidgetGalleryRows(columns).forEach { row ->
                    val alone = row.size == 1 && row.single().runsTheWidth
                    val inside = (if (alone || columns == 1) whole else half) - TILE_PADDING * 2
                    val tallest = if (columns == 1 || alone) STAGE_TALLEST_ALONE else STAGE_TALLEST
                    val stage = row.maxOf { pictureSize(it, inside, tallest).height }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { theme ->
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                WidgetTile(
                                    theme = theme,
                                    isDefault = theme == widgets.theme,
                                    address = streamWidgetAddress(widgets, status, theme),
                                    networkAddress = (status as? StreamWidgetStatus.Running)?.networkUrl(theme),
                                    running = running,
                                    stage = stage,
                                    picture = pictureSize(theme, inside, stage),
                                    copied = copied,
                                    copy = copy,
                                    open = open,
                                    makeDefault = { state.setStreamWidgetTheme(theme) },
                                )
                            }
                        }
                        if (!alone) repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** Narrower than this, two looks side by side would squeeze their addresses and buttons, and the gallery is one wide. */
private val TILE_MIN_WIDTH = 290.dp
private val TILE_PADDING = 12.dp

/** The tallest a row's pictures are framed: Afterglow, the one tall look, as large as it can be without towering. */
private val STAGE_TALLEST = 210.dp
private val STAGE_TALLEST_ALONE = 260.dp

/**
 * How large [theme]'s picture is drawn within [width] and [height]: at its own size where it fits, smaller where it
 * does not, and never larger -- each picture is the look at half its size, a pixel to a point, and blown up it would
 * only blur. Worked out here rather than measured, since a row's cards are measured for their height, which a layout
 * that waits to be told its size cannot answer.
 *
 * The ticker is the exception: squeezed into a narrow card its line of words is too small to read, so it keeps its
 * own height there and runs out of the frame, as the bar it is runs off along the edge of a screen.
 */
private fun pictureSize(theme: StreamWidgetTheme, width: Dp, height: Dp): DpSize {
    val (pictureWidth, pictureHeight) = previewSize(theme)
    val scale = minOf(1f, width / pictureWidth.dp, height / pictureHeight.dp)
    if (theme.runsTheWidth && scale < SMALLEST_TICKER) return DpSize(width, pictureHeight.dp.coerceAtMost(height))
    return DpSize(pictureWidth.dp * scale, pictureHeight.dp * scale)
}

/** Smaller than this, the ticker's picture is cut short rather than shrunk: see [pictureSize]. */
private const val SMALLEST_TICKER = .85f

/** The picture's size, read from it, or the look's own halved where the picture could not be read. */
private fun previewSize(theme: StreamWidgetTheme): Pair<Int, Int> =
    widgetPreview(theme)?.let { it.width to it.height } ?: (theme.width / 2 to theme.height / 2)

/** Each look's picture, read once from the `jvm` module's resources: seven small PNGs, made from the real pages. */
private val previews = ConcurrentHashMap<StreamWidgetTheme, ImageBitmap>()

private fun widgetPreview(theme: StreamWidgetTheme): ImageBitmap? = previews[theme] ?: runCatching {
    StreamWidgetTheme::class.java.getResourceAsStream(theme.previewResource)?.use { it.readBytes() }
        ?.let { org.jetbrains.skia.Image.makeFromEncoded(it).toComposeImageBitmap() }
}.getOrNull()?.also { previews[theme] = it }

/** What a look's picture is framed on, where it does not fill its frame: the dusk of the made-up scene it is shown over. */
private val STAGE = Color(0xFF15131F)

/**
 * One look: its picture, [picture] large in a frame [stage] tall; its name, beside the default's mark or the way to
 * make it the default; what it is and its size in OBS; its address, dimmed while the widgets are not running, with
 * its address for another computer beneath when there is one; and Copy and Open in browser.
 */
@Composable
private fun WidgetTile(
    theme: StreamWidgetTheme,
    isDefault: Boolean,
    address: String,
    networkAddress: String?,
    running: Boolean,
    stage: Dp,
    picture: DpSize,
    copied: String?,
    copy: (String) -> Unit,
    open: (String) -> Unit,
    makeDefault: () -> Unit,
) {
    val classic = skinned()
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = if (classic) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = .55f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isDefault) 2.dp else 1.dp, if (isDefault) accent else ink(if (classic) .35f else .1f)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize().padding(TILE_PADDING)) {
            WidgetPicture(theme, stage, picture)
            Spacer(Modifier.height(10.dp))
            // The default's mark, or the way to make this the default, beside the name: as tall as each other, so the
            // descriptions of a row's cards start level.
            Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(theme.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                if (isDefault) {
                    DefaultMark()
                } else {
                    TextButton(makeDefault, Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                        Text("Use as default", fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(theme.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
            Text("Size in OBS: ${streamWidgetSourceSize(theme)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            // What follows sits at the foot of the card, so the addresses and buttons of a row's cards line up.
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(9.dp))
            AddressWell(address, dimmed = !running)
            if (networkAddress != null) {
                Spacer(Modifier.height(5.dp))
                Text("From another computer:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Spacer(Modifier.height(2.dp))
                AddressWell(networkAddress, dimmed = false, copied = copied == networkAddress, copy = { copy(networkAddress) })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                val small = PaddingValues(horizontal = 11.dp, vertical = 4.dp)
                OutlinedButton({ copy(address) }, Modifier.height(32.dp), contentPadding = small) {
                    val done = copied == address
                    Icon(if (done) Icons.Default.Check else Icons.Default.ContentCopy, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (done) "Copied" else "Copy", fontSize = 12.sp)
                }
                OutlinedButton({ open(address) }, Modifier.height(32.dp), enabled = running, contentPadding = small) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open in browser", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * The look's picture, [size] large, in the middle of a frame [height] tall, with the skin's corners. A picture cut
 * short (see [pictureSize]) starts at its beginning and fades into the frame where it is cut, so it reads as going on.
 */
@Composable
private fun WidgetPicture(theme: StreamWidgetTheme, height: Dp, size: DpSize) {
    val picture = remember(theme) { widgetPreview(theme) }
    Box(
        Modifier.fillMaxWidth().height(height).clip(skinCorners(RoundedCornerShape(9.dp))).background(STAGE),
        contentAlignment = Alignment.Center,
    ) {
        if (picture == null) {
            Text(theme.displayName, color = Color.White.copy(alpha = .6f), fontSize = 12.sp)
            return@Box
        }
        val cut = size.width < picture.width.dp * (size.height / picture.height.dp) - 1.dp
        Box(Modifier.size(size)) {
            Image(
                picture,
                "${theme.displayName}, playing a made-up song",
                Modifier.fillMaxSize(),
                alignment = Alignment.CenterStart,
                contentScale = if (cut) ContentScale.FillHeight else ContentScale.FillBounds,
                filterQuality = FilterQuality.High,
            )
            if (cut) {
                Box(
                    Modifier.align(Alignment.CenterEnd).width(48.dp).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(STAGE.copy(alpha = 0f), STAGE))),
                )
            }
        }
    }
}

/** The default look's mark beside its name: the look the plain address shows. */
@Composable
private fun DefaultMark() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text("Default", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * An address, in type that tells a 1 from an l, to be selected; faded while there is nothing at it yet. With [copy],
 * a button of its own to copy it, for the one address -- another computer's -- that has no Copy button under it.
 */
@Composable
private fun AddressWell(address: String, dimmed: Boolean, copied: Boolean = false, copy: (() -> Unit)? = null) {
    val classic = skinned()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .then(if (classic) Modifier.listWell() else Modifier.clip(RoundedCornerShape(8.dp)).background(ink(.06f)))
            .padding(start = 9.dp, end = if (copy != null) 3.dp else 9.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionContainer(Modifier.weight(1f).alpha(if (dimmed) .5f else 1f)) {
            Text(address, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        if (copy != null) {
            IconButton(copy, Modifier.size(24.dp)) {
                Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, if (copied) "Copied" else "Copy the address", Modifier.size(13.dp))
            }
        }
    }
}

/** How long a pause lasts before a widget hides, from a moment to a minute; faded while widgets do not hide at all. */
@Composable
private fun HideAfter(widgets: StreamWidgetPreferences, state: AppState) {
    val saved = widgets.hideAfterSecondsInUse
    var seconds by remember(saved) { mutableIntStateOf(saved) }
    val range = StreamWidgetPreferences.HIDE_AFTER_SECONDS
    SettingAnchor(STREAM_WIDGET_HIDE_AFTER) {
        Row(Modifier.alpha(if (widgets.hideWhenPaused) 1f else .45f), verticalAlignment = Alignment.CenterVertically) {
            Text(streamWidgetHideAfterLabel(seconds), fontSize = 12.sp, modifier = Modifier.width(190.dp))
            Spacer(Modifier.width(10.dp))
            Slider(
                seconds.toFloat(),
                { seconds = it.roundToInt().coerceIn(range) },
                Modifier.weight(1f).widthIn(max = 360.dp),
                enabled = widgets.hideWhenPaused,
                valueRange = range.first.toFloat()..range.last.toFloat(),
                // Saved when let go of, rather than at every step of a drag, each of which would write the settings.
                onValueChangeFinished = { if (seconds != widgets.hideAfterSecondsInUse) state.setStreamWidgetHideAfterSeconds(seconds) },
            )
        }
    }
}

/**
 * The port, in a box of its own: saved on Enter or on leaving the box, and refused -- saying why, in the error colour
 * -- for one no program could listen on, which is then neither saved nor tried.
 */
@Composable
private fun WidgetPortField(widgets: StreamWidgetPreferences, state: AppState) {
    val saved = widgets.portInUse
    var typed by remember(saved) { mutableStateOf(saved.toString()) }
    var problem by remember(saved) { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    val save = {
        problem = streamWidgetPortProblem(typed)
        val port = typed.trim().toIntOrNull()
        if (problem == null && port != null) {
            if (port != saved) state.setStreamWidgetPort(port)
            typed = port.toString()
        }
    }
    SettingAnchor(STREAM_WIDGET_PORT) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    typed,
                    { typed = it.filter(Char::isDigit).take(5); problem = null },
                    label = { Text(STREAM_WIDGET_PORT) },
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
                if (saved != StreamWidgetPreferences.DEFAULT_PORT) {
                    Spacer(Modifier.width(10.dp))
                    TextButton({ state.setStreamWidgetPort(StreamWidgetPreferences.DEFAULT_PORT) }) {
                        Text("Use ${StreamWidgetPreferences.DEFAULT_PORT}")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                problem ?: ("Saved when you press Enter or leave the box. ${StreamWidgetPreferences.DEFAULT_PORT} suits " +
                    "nearly everybody: OBS's own remote control is on 4455. Change it only if another program on this " +
                    "computer has it, and change the addresses in OBS with it."),
                color = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * How a widget goes into OBS, in the few steps it takes, the one that is easy to miss among them -- refreshing when
 * the scene is shown, without which a source that loaded before the widgets were there stays empty -- and, folded
 * away, how one widget can be told otherwise than the rest by its own address.
 */
@Composable
private fun AddingToObs(widgets: StreamWidgetPreferences, status: StreamWidgetStatus) {
    val anchors = LocalSettingsAnchors.current
    var more by remember { mutableStateOf(false) }
    // Found by the search, the fold opens to show what was searched for.
    LaunchedEffect(anchors?.request, anchors?.target) { if (anchors?.target == STREAM_WIDGET_OVERRIDES) more = true }
    SettingsPanelCard {
        CardHeading(Icons.Default.Info, "Adding it to OBS")
        Spacer(Modifier.height(10.dp))
        SetupStep(1, "In OBS, under Sources, click + and choose Browser. Call it what you like, such as Now playing.")
        SetupStep(2, "Paste a look's address, copied from above, into URL, and set the Width and Height to the size under it.")
        SetupStep(3, "Tick “Refresh browser when scene becomes active”, and click OK.")
        Text(
            "OBS loads the page once. If it starts before Noctorium does, or while the widgets are off, it finds nothing " +
                "there and does not look again by itself; this has it look again whenever the scene is shown. Once it " +
                "has loaded, the widget hides when Noctorium closes and comes back when it opens.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 26.dp, top = 2.dp, bottom = 6.dp),
        )
        Text(
            "Streamlabs is the same: add a Browser Source, and paste the address and the size into it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(14.dp))
        SettingAnchor(STREAM_WIDGET_OVERRIDES) {
            Row(Modifier.fillMaxWidth().clickable { more = !more }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("More", fontWeight = FontWeight.SemiBold)
                    Text(
                        "One widget's own settings, in its address",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Icon(if (more) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (more) "Fold away" else "Show")
            }
        }
        if (more) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Every widget follows the options above unless its own address says otherwise. That is how two widgets " +
                    "can run on one stream, one with a cover and one without. After the address put a ?, then any of " +
                    "these, with & between them:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "cover=0" to "without the cover",
                "progress=0" to "without the progress bar",
                "logo=0" to "without the service's logo",
                "hidePaused=0" to "stays on screen while the music is paused",
                "hideAfter=10" to "hides after 10 seconds of pause, or as many as you write",
                "accent=ff3366" to "in another colour, written as six hexadecimal digits",
                "align=left" to "for a source bigger than the widget: left, right, top or bottom of it, rather than the middle",
            ).forEach { (option, meaning) -> WidgetOption(option, meaning) }
            Spacer(Modifier.height(8.dp))
            Text("For example:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            SelectionContainer {
                Text(
                    streamWidgetAddress(widgets, status, widgets.theme) + "?cover=0&accent=ff3366",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/** One of the options a widget's address can carry, and what it does. */
@Composable
private fun WidgetOption(option: String, meaning: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        SelectionContainer(Modifier.width(118.dp)) {
            Text(option, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(8.dp))
        Text(meaning, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

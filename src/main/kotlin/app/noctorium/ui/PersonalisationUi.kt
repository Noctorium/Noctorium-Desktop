package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.Destination
import app.noctorium.core.ProviderFilter
import app.noctorium.domain.HomeSection
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.lyrics.LyricLine
import app.noctorium.settings.Equalizer
import app.noctorium.settings.EqualizerPreset
import app.noctorium.settings.EqualizerSettings
import app.noctorium.settings.FontChoice
import app.noctorium.settings.HomePart
import app.noctorium.settings.LyricsAlignment
import app.noctorium.settings.LyricsLook
import app.noctorium.settings.LyricsSize
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.PlayerButton
import app.noctorium.settings.contrastRatio
import app.noctorium.settings.parseHexColour
import app.noctorium.settings.toHexColour
import app.noctorium.settings.withoutHidden
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

/*
 * The desktop's half of making Noctorium the listener's own: the equaliser, their own accent, the typeface,
 * how the lyrics are set, which buttons the player bar carries, what Home is made of, what the sidebar
 * offers, and a note from the tray when a song starts. The settings themselves live in the core, so the
 * phone means the same thing by each; what is here is how the desktop draws them and decides with them.
 *
 * The decisions are plain functions at the top, apart from the drawing, so they can be tested without a
 * window: which buttons a bar draws, what Home shows, what the sidebar keeps, when a track is announced and
 * how a colour turns into the three numbers a picker shows.
 */

// --- The player bar's buttons ---

/**
 * The optional buttons a player bar layout draws, at the width it has, without the ones put away.
 *
 * Every entry in [PlayerButton] has a button on the desktop's bars -- Devices is the Connect button -- so
 * none is skipped here. Play, pause, next and previous are not in it and are always drawn. Download and
 * Add to playlist are not in it either, so they stay where each layout has always put them.
 *
 * What each layout leaves out by itself is decided here too, so that one place says what a bar shows:
 * Stacked has never had a lyrics button and gives up the volume when it is narrow, and the newer layouts
 * give up the lyrics button when narrow. [narrow] is whichever width the layout itself calls narrow.
 *
 * [tight] is narrower again, where Floating and Display give up shuffle, repeat and an idle sleep timer as well,
 * so the song keeps its room. The Island has the transport and the song and opens to the volume, and nothing
 * more but a timer running or music playing elsewhere, which every bar shows.
 */
internal fun playerBarButtons(
    style: PlayerBarStyle,
    narrow: Boolean,
    hidden: Set<PlayerButton>,
    inUse: Set<PlayerButton> = emptySet(),
    tight: Boolean = false,
): Set<PlayerButton> {
    val drawn = PlayerButton.entries.toMutableSet()
    when (style) {
        PlayerBarStyle.INLINE -> Unit
        // Drawn by the Windows skin work.
        PlayerBarStyle.TASKBAR -> Unit
        PlayerBarStyle.STACKED -> {
            drawn -= PlayerButton.LYRICS
            if (narrow) drawn -= PlayerButton.VOLUME
        }
        PlayerBarStyle.CENTERED, PlayerBarStyle.SLIM, PlayerBarStyle.SLIM_LEFT, PlayerBarStyle.SPOTLIGHT ->
            if (narrow) drawn -= PlayerButton.LYRICS
        PlayerBarStyle.FLOATING, PlayerBarStyle.DISPLAY -> {
            if (narrow || tight) drawn -= PlayerButton.LYRICS
            if (tight) drawn -= setOf(PlayerButton.SHUFFLE, PlayerButton.REPEAT, PlayerButton.SLEEP_TIMER) - inUse
        }
        PlayerBarStyle.ISLAND -> drawn.retainAll(setOf(PlayerButton.VOLUME) + (inUse intersect setOf(PlayerButton.SLEEP_TIMER, PlayerButton.DEVICES)))
    }
    // A button showing something under way stays, put away or not: a sleep timer counting down, or the music
    // playing on another device, is not something to lose sight of because its button was hidden.
    return drawn - (hidden - inUse)
}

/** The buttons showing something under way just now, which [playerBarButtons] keeps whatever was put away. */
@Composable
internal fun playerButtonsInUse(state: AppState): Set<PlayerButton> {
    val timer by state.sleepTimer.collectAsState()
    val connect by state.connect.collectAsState()
    return buildSet {
        if (timer != null) add(PlayerButton.SLEEP_TIMER)
        if (connect.target != null) add(PlayerButton.DEVICES)
    }
}

/** The picture each button has on the bar, so the choice in Settings looks like the thing it puts away. */
internal fun PlayerButton.icon(): ImageVector = when (this) {
    PlayerButton.SHUFFLE -> Icons.Default.Shuffle
    PlayerButton.REPEAT -> Icons.Default.Repeat
    PlayerButton.LIKE -> Icons.Default.FavoriteBorder
    PlayerButton.LYRICS -> Icons.Default.Lyrics
    PlayerButton.QUEUE -> Icons.AutoMirrored.Filled.QueueMusic
    PlayerButton.SLEEP_TIMER -> Icons.Default.Bedtime
    PlayerButton.VOLUME -> Icons.AutoMirrored.Filled.VolumeUp
    PlayerButton.DEVICES -> Icons.Default.Devices
}

// --- Home ---

/** What Home draws, once the filter at the top and the parts put away have both had their say. */
internal data class HomeContent(
    val greeting: Boolean,
    val pinned: List<Track>,
    val recent: List<Track>,
    val sections: List<HomeSection>,
    val loading: Boolean,
) {
    /** Nothing at all would be drawn: Home says something kind instead of showing an empty page. */
    val nothingToShow: Boolean
        get() = !greeting && pinned.isEmpty() && recent.isEmpty() && sections.isEmpty() && !loading
}

/**
 * The parts of Home that are a service's rows, rather than the listener's own.
 *
 * Everything but the three that are the listener's, so that a service the core adds to Home is counted here
 * without anybody remembering to.
 */
internal val SERVICE_HOME_PARTS: Set<HomePart> = HomePart.entries.toSet() - setOf(HomePart.GREETING, HomePart.PINNED, HomePart.RECENT)

internal fun homeContent(ui: AppUiState, hidden: Set<HomePart>): HomeContent {
    val filter = ui.providerFilter
    fun ProviderType.passes() = filter.matches(this)
    // The placeholders stand in for the services' rows, so there are none when every row they could
    // become has been put away.
    val servicesShown = when (filter) {
        ProviderFilter.ALL -> SERVICE_HOME_PARTS.any { it !in hidden }
        ProviderFilter.YOUTUBE_MUSIC -> HomePart.YOUTUBE_MUSIC !in hidden
        ProviderFilter.SOUNDCLOUD -> HomePart.SOUNDCLOUD !in hidden
        ProviderFilter.BANDCAMP -> HomePart.BANDCAMP !in hidden
        ProviderFilter.SPOTIFY -> HomePart.SPOTIFY !in hidden
        ProviderFilter.VK -> HomePart.VK !in hidden
    }
    return HomeContent(
        greeting = HomePart.GREETING !in hidden,
        pinned = if (HomePart.PINNED in hidden) emptyList() else ui.pinnedTracks.filter { it.provider.passes() },
        recent = if (HomePart.RECENT in hidden) emptyList() else ui.recentTracks.filter { it.provider.passes() },
        sections = ui.homeSections.withoutHidden(hidden).filter { it.provider.passes() },
        loading = ui.homeLoading && servicesShown,
    )
}

// --- The sidebar ---

/** The sidebar's items that can be put away, in the order it draws them. Home and Settings are not among them. */
internal val HIDEABLE_DESTINATIONS: List<Destination> = listOf(
    Destination.SEARCH,
    Destination.LINK,
    Destination.LIBRARY,
    Destination.DOWNLOADS,
    Destination.NOW_PLAYING,
    Destination.QUEUE,
)

/**
 * Whether the sidebar shows a destination. Home and Settings always: without Home there is nowhere to
 * start from, and without Settings there would be no way to bring the others back.
 */
internal fun sidebarShows(destination: Destination, hidden: Set<Destination>): Boolean =
    destination !in HIDEABLE_DESTINATIONS || destination !in hidden

/** The name each destination has in the sidebar, which is also its name in Settings. */
internal fun Destination.sidebarLabel(): String = when (this) {
    Destination.HOME -> "Home"
    Destination.SEARCH -> "Search"
    Destination.LINK -> "Paste link"
    Destination.LIBRARY -> "Library"
    Destination.DOWNLOADS -> "Downloads"
    Destination.NOW_PLAYING -> "Now playing"
    Destination.QUEUE -> "Queue"
    Destination.SETTINGS -> "Settings"
}

internal fun Destination.sidebarIcon(): ImageVector = when (this) {
    Destination.HOME -> Icons.Default.Home
    Destination.SEARCH -> Icons.Default.Search
    Destination.LINK -> Icons.Default.Link
    Destination.LIBRARY -> Icons.Default.LibraryMusic
    Destination.DOWNLOADS -> Icons.Default.DownloadForOffline
    Destination.NOW_PLAYING -> Icons.Default.GraphicEq
    Destination.QUEUE -> Icons.AutoMirrored.Filled.QueueMusic
    Destination.SETTINGS -> Icons.Default.Settings
}

// --- Announcing tracks ---

/**
 * Decides when a song that has just started is worth a note from the tray.
 *
 * Told about every change to what is playing and whether it is playing. A track counts as started the first
 * time it is heard playing, so pausing and carrying on is not a new start, and nor is the same track again
 * straight after itself -- a song on repeat would otherwise announce itself every four minutes. A start is
 * remembered even while announcing is off or the window is in front, so switching it on halfway through a
 * song does not announce the song already playing.
 */
internal class TrackAnnouncer {
    private var lastStarted: String? = null

    /** True when this is the moment to announce [trackKey]. */
    fun shouldAnnounce(trackKey: String?, playing: Boolean, enabled: Boolean, windowInFront: Boolean): Boolean {
        if (trackKey == null || !playing || trackKey == lastStarted) return false
        lastStarted = trackKey
        // No point announcing what the listener is already looking at.
        return enabled && !windowInFront
    }
}

// --- Colour ---

/** A colour as a picker lays it out: hue from 0 to 360, saturation and value from 0 to 1. */
internal data class Hsv(val hue: Float, val saturation: Float, val value: Float)

/** An opaque ARGB colour as hue, saturation and value. A grey has no hue, and is given 0. */
internal fun Long.toHsv(): Hsv {
    val r = ((this shr 16) and 0xFF) / 255f
    val g = ((this shr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val delta = max - minOf(r, g, b)
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }
    return Hsv(
        hue = if (hue < 0f) hue + 360f else hue,
        saturation = if (max == 0f) 0f else delta / max,
        value = max,
    )
}

/** Back to an opaque ARGB colour, each channel rounded to the nearest of its 256 steps. */
internal fun Hsv.toArgb(): Long {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val chroma = v * s
    val x = chroma * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - chroma
    val (r, g, b) = when ((h / 60f).toInt()) {
        0 -> Triple(chroma, x, 0f)
        1 -> Triple(x, chroma, 0f)
        2 -> Triple(0f, chroma, x)
        3 -> Triple(0f, x, chroma)
        4 -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    fun channel(c: Float): Long = ((c + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
    return 0xFF000000L or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
}

/**
 * The accent being dragged to in the colour picker, before it is chosen.
 *
 * Dragging across the picker asks for a new colour dozens of times a second, and choosing each one would
 * write the settings file dozens of times a second. So the window is recoloured from this while the pointer
 * is down, and the colour is only chosen -- and saved -- when it is let go. A shared value rather than
 * something passed down, because the picker is several screens below the theme it is recolouring.
 */
internal val accentPreview = mutableStateOf<Color?>(null)

// --- The equaliser ---

/** A band's gain rounded to the half decibel the curve moves in, and kept within range. */
internal fun snapGain(gain: Float): Float = (round(gain * 2f) / 2f).coerceIn(-Equalizer.MAX_GAIN_DB, Equalizer.MAX_GAIN_DB)

/** "+3", "−1.5", "0": a gain as the curve labels it, with a real minus sign rather than a hyphen. */
internal fun formatGain(gain: Float): String {
    val rounded = round(gain * 2f) / 2f
    if (rounded == 0f) return "0"
    val magnitude = abs(rounded)
    val digits = if (magnitude % 1f == 0f) magnitude.toInt().toString() else String.format(Locale.ROOT, "%.1f", magnitude)
    return (if (rounded > 0f) "+" else "−") + digits
}

/** The Sound page: for now the equaliser, which is all there is to say about how things sound. */
@Composable
internal fun SoundSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EqualizerCard(preferences.equalizer, state)
    }
}

/**
 * The equaliser: a switch, the presets, the curve with a handle on each band, and the preamp.
 *
 * Moving anything switches it on, the way the core's own setters do for the bands and the presets: nobody
 * drags the bass up and means for nothing to happen. Bands and preamp are only written when they are let
 * go of, because every write is a settings save and a new filter for mpv, and a drag is dozens of writes.
 *
 * While [onSpotify] -- the song playing is played by the account's own Spotify app -- nothing here can reach
 * it, so the controls rest and say why rather than appearing to do nothing.
 */
@Composable
internal fun EqualizerCard(equalizer: EqualizerSettings, state: AppState, onSpotify: Boolean = playingOnSpotify(state)) {
    SettingsPanelCard {
        CardHeading(Icons.Default.Equalizer, "Equaliser")
        Spacer(Modifier.height(6.dp))
        Text(
            "Ten bands from the deepest bass to the air above the cymbals, applied by the player itself as the " +
                "music plays. Drag a band to shape the sound; the presets are a gentle place to start.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        if (onSpotify) {
            Spacer(Modifier.height(12.dp))
            Surface(color = SPOTIFY_GREEN.copy(alpha = .1f), shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    OnSpotifyMark()
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Spotify plays this song in its own app, where the equaliser cannot reach. It shapes the songs " +
                            "played here again from the next one.",
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ToggleRow(
            "Equaliser",
            if (equalizer.enabled) {
                "On. The music plays through the curve below."
            } else {
                "Off: the music plays as it arrives. Choosing a preset or moving a band switches it on."
            },
            equalizer.enabled,
            enabled = !onSpotify,
        ) { on -> state.updateEqualizer { copy(enabled = on) } }
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Preset", EqualizerPreset.entries, equalizer.preset, { it.displayName }, enabled = !onSpotify, state::setEqualizerPreset)
        Spacer(Modifier.height(18.dp))
        EqualizerCurve(equalizer.gains, equalizer.enabled && !onSpotify, state::setEqualizerBand, enabled = !onSpotify)
        Spacer(Modifier.height(16.dp))
        PreampRow(equalizer.preampDb, enabled = !onSpotify) { db -> state.updateEqualizer { copy(preampDb = db, enabled = true) } }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Your own curve is kept when you choose a preset, for coming back to under Your own.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            // Back to flat and no preamp, leaving the switch where it is and the listener's own curve kept:
            // a reset clicked by mistake should not cost anybody the curve they spent ten minutes on.
            TextButton(
                { state.updateEqualizer { copy(preset = EqualizerPreset.FLAT, preampDb = 0f) } },
                enabled = !onSpotify && (equalizer.preset != EqualizerPreset.FLAT || equalizer.preampDb != 0f),
            ) {
                Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset to flat", fontSize = 12.sp)
            }
        }
    }
}

/**
 * The curve, drawn, with a handle on each of the ten bands.
 *
 * Each band is a vertical slider: pressing anywhere in its column takes hold of it, and it follows the
 * pointer up and down until let go, staying with that band even if the pointer wanders sideways. The line
 * through the handles is the curve as the player will apply it, filled toward the middle line so a boost
 * and a cut read at a glance. The value is written once, on letting go.
 */
@Composable
internal fun EqualizerCurve(gains: List<Float>, active: Boolean, commit: (Int, Float) -> Unit, enabled: Boolean = true) {
    val bands = Equalizer.BANDS_HZ
    val latestCommit by rememberUpdatedState(commit)
    // The band being dragged and where it has got to; and, after letting go, the value written, held until
    // the settings come back with it so the handle does not jump back for a frame on the way.
    var held by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableFloatStateOf(0f) }
    var written by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    LaunchedEffect(gains) { written = null }
    val shown = gains.mapIndexed { index, gain ->
        when {
            index == held -> draft
            written?.first == index -> written!!.second
            else -> gain
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = ink(.07f)
    val middle = ink(.2f)
    val ring = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = if (active) 1f else .55f }) {
        Row(Modifier.fillMaxWidth()) {
            shown.forEachIndexed { index, gain ->
                Text(
                    formatGain(gain),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = if (index == held) accent else quiet,
                    fontSize = 11.sp,
                    fontWeight = if (index == held) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(172.dp)
                .semantics {
                    contentDescription = "Equaliser curve: " + bands.indices.joinToString { i ->
                        "${Equalizer.label(bands[i])} hertz ${formatGain(shown[i])} decibels"
                    }
                }
                .pointerInput(enabled) {
                    // Resting: nothing it set would reach what is playing. See EqualizerCard.
                    if (!enabled) return@pointerInput
                    val inset = 12.dp.toPx()
                    fun gainAt(y: Float): Float {
                        val plot = (size.height - inset * 2).coerceAtLeast(1f)
                        return snapGain(Equalizer.MAX_GAIN_DB - (y - inset) / plot * (2 * Equalizer.MAX_GAIN_DB))
                    }
                    var band = 0
                    followPointer(
                        down = { position ->
                            band = (position.x / size.width * bands.size).toInt().coerceIn(0, bands.lastIndex)
                            held = band
                            draft = gainAt(position.y)
                        },
                        move = { position -> draft = gainAt(position.y) },
                        release = {
                            written = band to draft
                            held = null
                            latestCommit(band, draft)
                        },
                    )
                },
        ) {
            val inset = 12.dp.toPx()
            val top = inset
            val bottom = size.height - inset
            val column = size.width / bands.size
            fun yOf(gain: Float) = top + (Equalizer.MAX_GAIN_DB - gain) / (2 * Equalizer.MAX_GAIN_DB) * (bottom - top)
            val zero = yOf(0f)
            val points = shown.mapIndexed { index, gain -> Offset(column * (index + .5f), yOf(gain)) }

            // Lines at six and twelve each way, and a firmer one where nothing is changed.
            listOf(12f, 6f, -6f, -12f).forEach { db ->
                drawLine(grid, Offset(0f, yOf(db)), Offset(size.width, yOf(db)), strokeWidth = 1.dp.toPx())
            }
            drawLine(middle, Offset(0f, zero), Offset(size.width, zero), strokeWidth = 1.dp.toPx())
            points.forEach { point ->
                drawLine(grid, Offset(point.x, top), Offset(point.x, bottom), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(accent.copy(alpha = .4f), Offset(point.x, zero), point, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }

            // A smooth line through the handles, held flat beyond the outermost bands as the player holds it.
            // The phantom points either side are level with their neighbours, so the curve leaves the end
            // handles horizontally and meets the flat ends without a corner.
            val curve = Path().apply {
                moveTo(0f, points.first().y)
                lineTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points.getOrElse(i - 1) { Offset(points[0].x - column, points[1].y) }
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = points.getOrElse(i + 2) { Offset(points.last().x + column, points[points.lastIndex - 1].y) }
                    cubicTo(
                        p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                        p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                        p2.x, p2.y,
                    )
                }
                lineTo(size.width, points.last().y)
            }
            val fill = Path().apply {
                addPath(curve)
                lineTo(size.width, zero)
                lineTo(0f, zero)
                close()
            }
            // Strongest at the extremes and clear at the middle line, so the shape is in the boosts and cuts.
            drawPath(
                fill,
                Brush.verticalGradient(
                    0f to accent.copy(alpha = .30f),
                    .5f to accent.copy(alpha = .04f),
                    1f to accent.copy(alpha = .30f),
                    startY = top,
                    endY = bottom,
                ),
            )
            drawPath(curve, accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            points.forEachIndexed { index, point ->
                if (index == held) {
                    drawCircle(accent.copy(alpha = .18f), radius = 15.dp.toPx(), center = point)
                    drawCircle(ring, radius = 9.dp.toPx(), center = point)
                    drawCircle(accent, radius = 7.dp.toPx(), center = point)
                } else {
                    drawCircle(ring, radius = 7.5.dp.toPx(), center = point)
                    drawCircle(accent, radius = 5.5.dp.toPx(), center = point)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            bands.forEach { hz ->
                Text(
                    Equalizer.label(hz),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = quiet,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The preamp: decibels added before the bands, written when the slider is let go of. */
@Composable
private fun PreampRow(preampDb: Float, enabled: Boolean = true, commit: (Float) -> Unit) {
    var dragging by remember(preampDb) { mutableFloatStateOf(preampDb) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(132.dp)) {
            Text("Preamp", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Before the bands", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Slider(
            value = dragging,
            onValueChange = { dragging = snapGain(it) },
            onValueChangeFinished = { if (dragging != preampDb) commit(dragging) },
            valueRange = -Equalizer.MAX_GAIN_DB..Equalizer.MAX_GAIN_DB,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${formatGain(dragging)} dB",
            Modifier.width(64.dp),
            textAlign = TextAlign.End,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Follows one pointer from pressing to letting go, for the controls here that are dragged rather than
 * clicked: [down] where it lands, [move] wherever it goes, [release] once it lets go.
 */
private suspend fun PointerInputScope.followPointer(
    down: (Offset) -> Unit,
    move: (Offset) -> Unit,
    release: () -> Unit,
) {
    awaitEachGesture {
        val first = awaitFirstDown()
        first.consume()
        down(first.position)
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == first.id } ?: break
            change.consume()
            if (!change.pressed) break
            move(change.position)
        }
        release()
    }
}

// --- The accent picker ---

/**
 * The listener's own accent: a square for how strong and how bright, a strip for the hue, and the hex box
 * for anybody who already knows the number.
 *
 * The window follows the pointer while it is down (see [accentPreview]), and the colour is chosen when it
 * lets go. Typing a whole colour into the box chooses it straight away. [page] is the theme's background,
 * for warning about an accent that would vanish into it.
 */
@Composable
internal fun AccentColourPicker(current: Long, page: Long, choose: (Long) -> Unit) {
    val latestCurrent by rememberUpdatedState(current)
    val latestChoose by rememberUpdatedState(choose)
    var hsv by remember { mutableStateOf(current.toHsv()) }
    var hex by remember { mutableStateOf(current.toHexColour()) }
    // The colour changed from somewhere else, or arrived after being chosen here: follow it, unless it is the
    // colour already showing, so a grey keeps the hue it was dragged from. Arriving is also the moment the
    // preview can let go, now that the settings carry the same colour.
    LaunchedEffect(current) {
        if (hsv.toArgb() != current) hsv = current.toHsv()
        if (parseHexColour(hex) != current) hex = current.toHexColour()
        accentPreview.value = null
    }
    DisposableEffect(Unit) { onDispose { accentPreview.value = null } }

    fun drag(next: Hsv) {
        hsv = next
        val argb = next.toArgb()
        hex = argb.toHexColour()
        accentPreview.value = Color(argb)
    }
    fun release() {
        val argb = hsv.toArgb()
        if (argb == latestCurrent) accentPreview.value = null else latestChoose(argb)
    }

    val colour = Color(hsv.toArgb())
    Row {
        Column {
            // Saturation across, brightness down, in the hue chosen below.
            Box(
                Modifier
                    .size(272.dp, 156.dp)
                    .pointerInput(Unit) {
                        fun at(position: Offset) = drag(
                            hsv.copy(
                                saturation = (position.x / size.width).coerceIn(0f, 1f),
                                value = 1f - (position.y / size.height).coerceIn(0f, 1f),
                            ),
                        )
                        followPointer(down = ::at, move = ::at, release = ::release)
                    }
                    .drawBehind {
                        // The three layers inside one rounded edge, rather than three rounded shapes on top of one
                        // another, whose softened corners would let the hue show round the bottom of the black.
                        val edge = Path().apply {
                            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(10.dp.toPx())))
                        }
                        clipPath(edge) {
                            drawRect(Color.hsv(hsv.hue, 1f, 1f))
                            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        }
                        val centre = Offset(hsv.saturation * size.width, (1f - hsv.value) * size.height)
                        drawCircle(Color.Black.copy(alpha = .35f), radius = 9.5.dp.toPx(), center = centre, style = Stroke(1.dp.toPx()))
                        drawCircle(Color.White, radius = 8.dp.toPx(), center = centre, style = Stroke(2.5.dp.toPx()))
                    }
                    .semantics { contentDescription = "Saturation and brightness" },
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .size(272.dp, 16.dp)
                    .pointerInput(Unit) {
                        fun at(position: Offset) = drag(hsv.copy(hue = (position.x / size.width).coerceIn(0f, 1f) * 360f))
                        followPointer(down = ::at, move = ::at, release = ::release)
                    }
                    .drawBehind {
                        drawRoundRect(
                            Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }),
                            cornerRadius = CornerRadius(size.height / 2),
                        )
                        val radius = size.height / 2
                        val centre = Offset((hsv.hue / 360f * size.width).coerceIn(radius, size.width - radius), size.height / 2)
                        drawCircle(Color.hsv(hsv.hue, 1f, 1f), radius = radius + 1.dp.toPx(), center = centre)
                        drawCircle(Color.Black.copy(alpha = .35f), radius = radius + 3.5.dp.toPx(), center = centre, style = Stroke(1.dp.toPx()))
                        drawCircle(Color.White, radius = radius + 2.dp.toPx(), center = centre, style = Stroke(2.5.dp.toPx()))
                    }
                    .semantics { contentDescription = "Hue" },
            )
        }
        Spacer(Modifier.width(22.dp))
        Column(Modifier.width(196.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colour)
                        .border(1.dp, ink(.18f), CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Your accent", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(hsv.toArgb().toHexColour(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                hex,
                { typed ->
                    hex = typed.take(9)
                    parseHexColour(typed)?.let { argb ->
                        hsv = argb.toHsv()
                        if (argb != latestCurrent) latestChoose(argb)
                    }
                },
                label = { Text("Hex", fontSize = 11.sp) },
                singleLine = true,
                isError = parseHexColour(hex) == null,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth().tracksTyping(),
            )
            // Three to one is where a button and a seek bar stop standing out from the page they sit on.
            val contrast = contrastRatio(hsv.toArgb(), page)
            if (contrast < 3.0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Hard to see on this theme's page: buttons and the seek bar would all but disappear.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

// --- The typeface ---

/** The three typefaces side by side, each written in itself, so the choice is made by reading them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FontChooser(selected: FontChoice, choose: (FontChoice) -> Unit) {
    Column {
        Text("Typeface", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FontChoice.entries.forEach { font ->
                // As tall as the tallest, so a one-line description does not leave its tile short of the others.
                FontOption(font, font == selected, Modifier.fillMaxRowHeight()) { choose(font) }
            }
        }
    }
}

@Composable
private fun FontOption(font: FontChoice, selected: Boolean, modifier: Modifier, choose: () -> Unit) {
    // The system's own is asked for by name here, so it still previews as itself while another is chosen.
    val family = font.fontFamily() ?: FontFamily.Default
    Surface(
        onClick = choose,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else ink(.04f),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else ink(.1f),
        ),
        modifier = modifier.width(206.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                font.displayName,
                fontFamily = family,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text("Late night songs", fontFamily = family, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(font.description, fontFamily = family, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
}

// --- The lyrics ---

/** How the lyrics are set: their size, which way they lean, whether the lines not being sung step back. */
@Composable
internal fun LyricsLookCard(look: LyricsLook, state: AppState) {
    SettingsPanelCard {
        CardHeading(Icons.Default.Lyrics, "Lyrics")
        Spacer(Modifier.height(6.dp))
        Text(
            "How the words are set on the now playing screen, in every layout, sing along included.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(15.dp))
        ChoiceRow("Size", LyricsSize.entries, look.size, { it.displayName }) { size -> state.updateLyricsLook { copy(size = size) } }
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Alignment", LyricsAlignment.entries, look.alignment, { it.displayName }) { alignment ->
            state.updateLyricsLook { copy(alignment = alignment) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "As laid out keeps each layout's own: to the left beside the cover, centred for singing along.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        ToggleRow(
            "Dim the lines not being sung",
            "Off keeps every line at full strength, with the one being sung still picked out in the accent.",
            look.dimOtherLines,
        ) { on -> state.updateLyricsLook { copy(dimOtherLines = on) } }
        Spacer(Modifier.height(14.dp))
        // The real line drawing with three made-up lines, so each choice is seen as it will be.
        Surface(color = MaterialTheme.colorScheme.background.copy(alpha = .55f), shape = RoundedCornerShape(12.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp * look.size.scale),
            ) {
                LyricLineText(LyricLine("The streetlights hum a little tune", 1_000), active = false, synced = true, look = look)
                LyricLineText(LyricLine("and every window keeps the time", 4_000), active = true, synced = true, look = look)
                LyricLineText(LyricLine("until the morning finds us singing", 7_000), active = false, synced = true, look = look)
            }
        }
    }
}

// --- Choosing several at once ---

/**
 * A labelled row of pills that each switch on and off, for choosing which of several things are shown.
 *
 * Drawn like [ChoiceRow]'s pills, so the two read as one family: lit is on, faint is put away. Each pill
 * carries a tick or its own picture, which is what tells this row apart from a choice of one.
 *
 * [mark] is for a choice whose order matters, such as the genres Home draws a row for: a pill that is on
 * carries its place in that order instead of the tick, so the order can be read off the pills themselves.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ToggleChips(
    label: String?,
    options: List<T>,
    on: (T) -> Boolean,
    name: (T) -> String,
    icon: ((T) -> ImageVector)? = null,
    mark: ((T) -> String?)? = null,
    toggle: (T, Boolean) -> Unit,
) {
    Column {
        label?.let {
            Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val active = on(option)
                val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .75f)
                Surface(
                    onClick = { toggle(option, !active) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = .55f) else ink(.14f)),
                ) {
                    Row(
                        Modifier.padding(start = 11.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val marked = if (active) mark?.invoke(option) else null
                        if (marked != null) {
                            Box(
                                Modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                // A line exactly as tall as the figure: the body text's own line is taller
                                // than this whole circle, and set in it the figure sat at the bottom edge.
                                Text(
                                    marked,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = LocalTextStyle.current.copy(
                                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                                    ),
                                )
                            }
                        } else {
                            // Without a picture of its own, a pill says what clicking it would do: a tick
                            // for on, a plus for something that can be brought back.
                            Icon(
                                when {
                                    icon != null -> icon(option)
                                    active -> Icons.Default.Check
                                    else -> Icons.Default.Add
                                },
                                null,
                                Modifier.size(15.dp),
                                tint = tint,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            name(option),
                            color = tint,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

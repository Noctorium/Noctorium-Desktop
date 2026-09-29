package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.playback.RepeatMode
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.TimeDisplay

/*
 * The player bar layouts added beside Inline and Stacked.
 *
 * Each is a different answer to how much of the window the player is worth. Centred is the arrangement
 * most music players settled on and people arrive already knowing. Slim gives the page nearly everything,
 * for somebody who browses more than they look at the bar. Spotlight goes the other way, and makes the bar
 * about the record that is playing.
 *
 * They share their parts -- the cover, the title, the transport, the tools -- so a control behaves the same
 * whichever layout it is in, and a fix to one is a fix to all of them.
 */

/**
 * The track on the left, the transport in the middle with the seek bar under it, the tools on the right.
 *
 * The middle column is sized from the window rather than fixed, and the two sides share what is left
 * equally, which is what keeps the controls in the true centre of the window whatever the title's length.
 */
@Composable
internal fun CenteredPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    BarSurface(preferences.playerBarPosition, height = 88.dp) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val middle = (maxWidth * .42f).coerceIn(300.dp, 560.dp)
            val narrow = maxWidth < 900.dp
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    BarCover(current, 52.dp, 10.dp, state)
                    Spacer(Modifier.width(12.dp))
                    BarTitle(current, playback, Modifier.weight(1f, fill = false), state = state)
                    current?.let { track ->
                        Spacer(Modifier.width(4.dp))
                        LikeButton(track, state, size = 34.dp)
                        if (!narrow) DownloadButton(track, state, size = 34.dp)
                    }
                }
                Column(Modifier.width(middle), horizontalAlignment = Alignment.CenterHorizontally) {
                    Transport(queue, playback, state, playSize = 40.dp, modes = true)
                    PlaybackProgressBar(
                        playback = playback,
                        onSeek = state::seekTo,
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        style = preferences.progressBarStyle,
                        timeDisplay = preferences.timeDisplay,
                    )
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    BarTools(queue, playback, state, lyrics = !narrow)
                }
            }
        }
    }
}

/**
 * One short row, and the progress as a hairline along the edge that faces the page.
 *
 * Fifty points against Inline's seventy-four and Stacked's hundred, and nothing left out to get there:
 * shuffle and repeat, like and download, and every tool the other bars carry. The first version dropped
 * shuffle and repeat to save room, and a short bar that sends you elsewhere to turn shuffle on is not
 * saving anybody anything. On a narrow window it is the song's line that gives way, not the controls.
 *
 * [controlsFirst] is Slim left: the same row with the transport at the start and the track after it.
 *
 * The hairline is still a seek bar -- a click puts the song there, and a drag scrubs -- with a band taller
 * than the line to aim at, since a three-pixel target is not one anybody can hit.
 */
@Composable
internal fun SlimPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState, controlsFirst: Boolean = false) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    val atTop = preferences.playerBarPosition == PlayerBarPosition.TOP
    val inGlass = LocalInGlass.current
    Surface(
        color = if (inGlass) Color.Transparent else MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // In a pane of glass the line goes along the bottom, inset from the rounded ends, as on the
            // phone: a line across the top of a pill has no straight edge to sit on.
            if (!atTop && !inGlass) Hairline(playback, state::seekTo)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val narrow = maxWidth < 960.dp
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val controls: @Composable () -> Unit = {
                        Transport(queue, playback, state, playSize = 34.dp, modes = true, buttonSize = 30.dp)
                    }
                    if (controlsFirst) {
                        controls()
                        Spacer(Modifier.width(14.dp))
                    }
                    BarCover(current, 32.dp, 6.dp, state)
                    Spacer(Modifier.width(10.dp))
                    SlimLine(current, playback, state, Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        elapsedOverTotal(playback, preferences.timeDisplay),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (!controlsFirst) controls()
                    current?.let { track ->
                        LikeButton(track, state, size = 32.dp)
                        if (!narrow) DownloadButton(track, state, size = 32.dp)
                    }
                    BarTools(queue, playback, state, lyrics = !narrow)
                }
            }
            if (atTop && !inGlass) Hairline(playback, state::seekTo)
            if (inGlass) Box(Modifier.padding(start = 28.dp, end = 28.dp, bottom = 4.dp)) { Hairline(playback, state::seekTo) }
        }
    }
}

/**
 * The record, given room: a large cover and a large title over the colours of the artwork.
 *
 * The tint is the same two colours the now playing screen's backdrop takes from the cover, and moves when
 * the song does. Under liquid glass it is fainter, so the pane is still a pane.
 */
@Composable
internal fun SpotlightPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    val inGlass = LocalInGlass.current
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val palette = rememberArtworkPalette(
        current?.artworkUrl,
        current?.provider ?: ProviderType.LOCAL,
        fallback = ArtworkPalette(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
    )
    val lead by animateColorAsState(palette.primary, tween(700), label = "spotlightLead")
    val deep by animateColorAsState(palette.secondary, tween(700), label = "spotlightDeep")
    val strength = if (inGlass) .30f else .55f
    BarSurface(preferences.playerBarPosition, height = 120.dp, color = if (inGlass) Color.Transparent else surface) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(lead.copy(alpha = strength), deep.copy(alpha = strength * .7f), Color.Transparent),
                ),
            ),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val narrow = maxWidth < 900.dp
                Row(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    BarCover(current, 92.dp, 14.dp, state)
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        BarTitle(current, playback, state = state, titleSize = 19.sp, detailSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        PlaybackProgressBar(
                            playback = playback,
                            onSeek = state::seekTo,
                            modifier = Modifier.fillMaxWidth().height(30.dp),
                            style = preferences.progressBarStyle,
                            timeDisplay = preferences.timeDisplay,
                        )
                    }
                    Spacer(Modifier.width(18.dp))
                    Transport(queue, playback, state, playSize = 52.dp, modes = true, buttonSize = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    current?.let { LikeButton(it, state, size = 36.dp) }
                    BarTools(queue, playback, state, lyrics = !narrow)
                }
            }
        }
    }
}

// --- The parts ---

/** The bar's own background and the rule on the edge that faces the page, as Inline and Stacked have. */
@Composable
private fun BarSurface(
    position: PlayerBarPosition,
    height: Dp,
    color: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    val inGlass = LocalInGlass.current
    val rule = MaterialTheme.colorScheme.primary.copy(alpha = .22f)
    Surface(color = if (inGlass) Color.Transparent else color, modifier = Modifier.fillMaxWidth().height(height)) {
        Column {
            if (position == PlayerBarPosition.BOTTOM && !inGlass) HorizontalDivider(color = rule)
            Box(Modifier.weight(1f)) { content() }
            if (position == PlayerBarPosition.TOP && !inGlass) HorizontalDivider(color = rule)
        }
    }
}

/** The cover, or a note where there is none yet. Clicking it opens the now playing screen. */
@Composable
private fun BarCover(track: Track?, size: Dp, corner: Dp, state: AppState) {
    val shape = RoundedCornerShape(corner)
    val modifier = Modifier.size(size).clip(shape).clickable(enabled = track != null) { state.navigate(Destination.NOW_PLAYING) }
    if (track != null) {
        RemoteArtwork(track.artworkUrl, track.provider, modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, Modifier.size(size * .4f))
        }
    }
}

/** Title and artist on one line, the artist quieter, or what went wrong in its place, in red. */
@Composable
private fun SlimLine(current: Track?, playback: PlaybackState, state: AppState, modifier: Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(current?.title ?: "Nothing playing") }
            val second = playback.errorMessage ?: current?.artistLine?.takeIf(String::isNotBlank)
            if (second != null) {
                withStyle(
                    SpanStyle(
                        color = if (playback.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) { append("  ·  $second") }
            }
        },
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.clickable(enabled = current != null) { state.navigate(Destination.NOW_PLAYING) },
    )
}

/**
 * Title over artist, or over what went wrong, in red, when something did.
 *
 * A new track's name rises into place as the last one lifts away, so a skip is seen as well as heard.
 */
@Composable
private fun BarTitle(
    current: Track?,
    playback: PlaybackState,
    modifier: Modifier = Modifier,
    state: AppState,
    titleSize: TextUnit = 14.sp,
    detailSize: TextUnit = 12.sp,
) {
    MotionContent(
        current,
        modifier.clickable(enabled = current != null) { state.navigate(Destination.NOW_PLAYING) },
        kind = MotionKind.TRACK,
        contentKey = { it?.queueKey },
    ) { track ->
        Column {
            Text(
                track?.title ?: "Nothing playing",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleSize,
            )
            Text(
                playback.errorMessage ?: track?.artistLine ?: "Choose a track to start",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (playback.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = detailSize,
            )
        }
    }
}

/** Play or pause, the one giving way to the other with a small scale rather than a cut. */
@Composable
internal fun PlayPauseIcon(playing: Boolean, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    MotionContent(playing, kind = MotionKind.ICON, contentAlignment = Alignment.Center) { isPlaying ->
        Icon(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            if (isPlaying) "Pause" else "Play",
            modifier,
            tint = tint,
        )
    }
}

/** Previous, play and next, with shuffle and repeat either side when [modes] is asked for. */
@Composable
private fun Transport(
    queue: QueueState,
    playback: PlaybackState,
    state: AppState,
    playSize: Dp,
    modes: Boolean,
    buttonSize: Dp = 36.dp,
) {
    val lit = MaterialTheme.colorScheme.primary
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (modes) {
            IconButton(state::toggleShuffle, Modifier.size(buttonSize)) {
                Icon(Icons.Default.Shuffle, "Shuffle", Modifier.size(18.dp), tint = if (queue.shuffleEnabled) lit else quiet)
            }
        }
        IconButton(state::previous, Modifier.size(buttonSize)) {
            Icon(Icons.Default.SkipPrevious, "Previous track", Modifier.size(buttonSize * .58f))
        }
        FilledIconButton(state::togglePlayback, Modifier.size(playSize), enabled = queue.current != null) {
            if (playback.status == PlaybackStatus.RESOLVING) {
                CircularProgressIndicator(Modifier.size(playSize * .42f), strokeWidth = 2.dp)
            } else {
                PlayPauseIcon(playback.isPlaying, Modifier.size(playSize * .56f))
            }
        }
        IconButton(state::next, Modifier.size(buttonSize)) {
            Icon(Icons.Default.SkipNext, "Next track", Modifier.size(buttonSize * .58f))
        }
        if (modes) {
            IconButton(state::cycleRepeat, Modifier.size(buttonSize)) {
                Icon(
                    if (queue.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    when (queue.repeatMode) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat all"
                        RepeatMode.ONE -> "Repeat one"
                    },
                    Modifier.size(18.dp),
                    tint = if (queue.repeatMode != RepeatMode.OFF) lit else quiet,
                )
            }
        }
    }
}

/** Connect, the sleep timer, the queue, lyrics and volume, in the order Inline has them. */
@Composable
private fun BarTools(queue: QueueState, playback: PlaybackState, state: AppState, lyrics: Boolean) {
    ConnectButton(state)
    SleepTimerButton(state)
    QueueButton(queue, state, 34.dp)
    if (lyrics) {
        IconButton({ state.navigate(Destination.NOW_PLAYING) }, Modifier.size(34.dp)) {
            Icon(Icons.Default.Lyrics, "Lyrics", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    VolumeControl(playback, state)
}

@Composable
private fun QueueButton(queue: QueueState, state: AppState, size: Dp) {
    BadgedBox(badge = { if (queue.tracks.isNotEmpty()) Badge { Text(queue.tracks.size.toString()) } }) {
        IconButton({ state.navigate(Destination.QUEUE) }, Modifier.size(size)) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", Modifier.size(19.dp))
        }
    }
}

/**
 * The progress as a line, which is also a seek bar.
 *
 * The line is three points; what can be clicked is the ten-point band it sits in. While the pointer is
 * down the line follows the pointer, not the player, so a drag is not fought by the position ticker.
 */
@Composable
private fun Hairline(playback: PlaybackState, seekTo: (Long) -> Unit) {
    val duration = playback.durationMs
    val canSeek = playback.track != null && duration > 0 && playback.status != PlaybackStatus.RESOLVING
    var widthPx by remember { mutableIntStateOf(1) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubbed by remember { mutableFloatStateOf(0f) }
    val fraction = if (scrubbing) scrubbed else playbackFraction(playback.positionMs.toFloat(), duration)
    val filled = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(canSeek, widthPx, duration) {
                if (!canSeek) return@pointerInput
                detectTapGestures { offset -> seekTo(((offset.x / widthPx).coerceIn(0f, 1f) * duration).toLong()) }
            }
            .pointerInput(canSeek, widthPx, duration) {
                if (!canSeek) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> scrubbing = true; scrubbed = (offset.x / widthPx).coerceIn(0f, 1f) },
                    onDragEnd = { seekTo((scrubbed * duration).toLong()); scrubbing = false },
                    onDragCancel = { scrubbing = false },
                    onHorizontalDrag = { change, _ ->
                        scrubbed = (change.position.x / widthPx).coerceIn(0f, 1f)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50))) {
            drawRect(track)
            drawRect(filled, topLeft = Offset.Zero, size = Size(size.width * fraction, size.height))
        }
    }
}

/** "1:23 / 4:00", or with the time left, as the listener asked for the bar's right-hand number. */
internal fun elapsedOverTotal(playback: PlaybackState, display: TimeDisplay): String {
    val duration = playback.durationMs
    if (playback.track == null) return ""
    val elapsed = formatPlaybackTime(playback.positionMs)
    if (duration <= 0) return "$elapsed / $UNKNOWN_PLAYBACK_TIME"
    val trailing = when (display) {
        TimeDisplay.REMAINING -> "-" + formatPlaybackTime((duration - playback.positionMs).coerceAtLeast(0))
        TimeDisplay.TOTAL -> formatPlaybackTime(duration)
    }
    return "$elapsed / $trailing"
}

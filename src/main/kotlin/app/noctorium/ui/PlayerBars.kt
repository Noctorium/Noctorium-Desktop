package app.noctorium.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import app.noctorium.ui.skins.FilledIconButton
import app.noctorium.ui.skins.skinShape
import app.noctorium.ui.skins.HorizontalDivider
import androidx.compose.material3.Icon
import app.noctorium.ui.skins.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
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
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.Glass
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.PlayerButton
import app.noctorium.settings.TimeDisplay
import kotlinx.coroutines.delay

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
            val shown = playerBarButtons(PlayerBarStyle.CENTERED, narrow, preferences.desktop.hiddenPlayerButtons, playerButtonsInUse(state))
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    BarCover(current, 52.dp, 10.dp, state)
                    Spacer(Modifier.width(12.dp))
                    BarTitle(current, playback, Modifier.weight(1f, fill = false), state = state)
                    current?.let { track ->
                        Spacer(Modifier.width(4.dp))
                        if (PlayerButton.LIKE in shown) LikeButton(track, state, size = 34.dp)
                        if (!narrow) DownloadButton(track, state, size = 34.dp)
                    }
                }
                Column(Modifier.width(middle), horizontalAlignment = Alignment.CenterHorizontally) {
                    Transport(queue, playback, state, playSize = 40.dp, shown = shown)
                    PlaybackProgressBar(
                        playback = playback,
                        onSeek = state::seekTo,
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        style = preferences.progressBarStyle,
                        timeDisplay = preferences.timeDisplay,
                    )
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    BarTools(queue, playback, state, shown)
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
                val shown = playerBarButtons(
                    if (controlsFirst) PlayerBarStyle.SLIM_LEFT else PlayerBarStyle.SLIM,
                    narrow,
                    preferences.desktop.hiddenPlayerButtons,
                    playerButtonsInUse(state),
                )
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val controls: @Composable () -> Unit = {
                        Transport(queue, playback, state, playSize = 34.dp, shown = shown, buttonSize = 30.dp)
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
                        if (PlayerButton.LIKE in shown) LikeButton(track, state, size = 32.dp)
                        if (!narrow) DownloadButton(track, state, size = 32.dp)
                    }
                    BarTools(queue, playback, state, shown)
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
                val shown = playerBarButtons(PlayerBarStyle.SPOTLIGHT, narrow, preferences.desktop.hiddenPlayerButtons, playerButtonsInUse(state))
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
                    Transport(queue, playback, state, playSize = 52.dp, shown = shown, buttonSize = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    if (PlayerButton.LIKE in shown) current?.let { LikeButton(it, state, size = 36.dp) }
                    BarTools(queue, playback, state, shown)
                }
            }
        }
    }
}

/**
 * Whether the bar floats over the page rather than taking a strip of the window to itself: the page runs on
 * beneath it, and is told how much of it is covered so its last rows can still be scrolled clear, as under glass.
 */
internal val PlayerBarStyle.floats: Boolean get() = this == PlayerBarStyle.FLOATING || this == PlayerBarStyle.ISLAND

/** The Floating bar's height, and the Island's at rest. */
internal val FLOATING_HEIGHT = 64.dp
private val ISLAND_HEIGHT = 52.dp

/**
 * How round a lifted bar is: the corner setting's largest shape, which is nearly a pill at Soft and square at
 * Sharp, but never past a pill. Glass takes a radius rather than a shape, and draws its lens to it.
 */
internal fun liftedCorner(corner: CornerStyle, height: Dp): Dp = (28.dp * corner.scale).coerceAtMost(height / 2)

/**
 * Inline's row lifted off the edge of the window and rounded, with a gap all round it, like a dock.
 *
 * The page carries on beneath it and shows in the gap, which is the reason for lifting it; the window tells the
 * page how much of it the bar covers, as it does under glass, so the last row of a list can still be scrolled
 * clear. Under glass the window's own pane is the lifted, rounded part, and this is only the row inside it.
 *
 * Narrower, it gives things up in turn, as the others do: download, add to playlist and lyrics first; then the
 * seek bar, for a line along the bar's foot that still seeks, before the seek bar is squeezed to a stub; then
 * shuffle, repeat and an idle sleep timer, so the song keeps its name to the last.
 */
@Composable
internal fun FloatingPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    var addToPlaylist by remember { mutableStateOf(false) }
    if (addToPlaylist && current != null) {
        val library by state.library.collectAsState()
        AddToPlaylistDialog(current, library.localPlaylists, state) { addToPlaylist = false }
    }
    val inGlass = LocalInGlass.current
    BoxWithConstraints(Modifier.fillMaxWidth().then(if (inGlass) Modifier else Modifier.padding(Glass.FLOAT_INSET_DP.dp))) {
        val narrow = maxWidth < 900.dp
        val lineOnly = maxWidth < 820.dp
        val tight = maxWidth < 680.dp
        val shown = playerBarButtons(
            PlayerBarStyle.FLOATING,
            narrow,
            preferences.desktop.hiddenPlayerButtons,
            playerButtonsInUse(state),
            tight = tight,
        )
        LiftedSurface(RoundedCornerShape(liftedCorner(preferences.cornerStyle, FLOATING_HEIGHT)), Modifier.fillMaxWidth().height(FLOATING_HEIGHT)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Transport(queue, playback, state, playSize = 40.dp, shown = shown, buttonSize = 34.dp)
                current?.let { track ->
                    Spacer(Modifier.width(4.dp))
                    if (PlayerButton.LIKE in shown) LikeButton(track, state, size = 34.dp)
                    if (!narrow) {
                        DownloadButton(track, state, size = 34.dp)
                        IconButton({ addToPlaylist = true }, Modifier.size(34.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                "Add to playlist",
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Row(
                    if (lineOnly) Modifier.weight(1f) else Modifier.width(if (narrow) 176.dp else 230.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BarCover(current, 40.dp, 9.dp, state)
                    Spacer(Modifier.width(10.dp))
                    BarTitle(current, playback, Modifier.weight(1f, fill = false), state = state, titleSize = 13.sp, detailSize = 11.sp)
                }
                if (lineOnly) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        elapsedOverTotal(playback, preferences.timeDisplay),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                } else {
                    Spacer(Modifier.width(16.dp))
                    PlaybackProgressBar(
                        playback = playback,
                        onSeek = state::seekTo,
                        modifier = Modifier.weight(1f),
                        style = preferences.progressBarStyle,
                        timeDisplay = preferences.timeDisplay,
                    )
                }
                Spacer(Modifier.width(10.dp))
                BarTools(queue, playback, state, shown)
            }
            // Too narrow for a seek bar beside the song: a line along the foot instead, which still seeks, kept
            // in from the rounded ends so it sits on the straight.
            if (lineOnly) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = liftedCorner(preferences.cornerStyle, FLOATING_HEIGHT) + 6.dp, end = liftedCorner(preferences.cornerStyle, FLOATING_HEIGHT) + 6.dp),
                ) { Hairline(playback, state::seekTo) }
            }
        }
    }
}

/**
 * Only a small pill in the middle of the row: the cover, the song, previous, play and next, and the progress as
 * a hairline along its foot. The rest of the row is the page's, which runs on beneath it.
 *
 * Pointed at, the pill grows into the seek bar in the chosen style, with its times and the volume, and shrinks
 * back once the pointer has gone -- after a moment, so a pointer only crossing its edge on the way somewhere
 * does not set it pulsing. It stays open while something that started in it is still going: a drag along the
 * seek bar, or the volume's menu.
 *
 * At rest it takes no more of the window than its own height and the gap under it, which is all the page has
 * to scroll clear of. Opened, it grows over the page rather than pushing it, away from the edge it sits on, and
 * keeps the row with play in it where it was, under the pointer.
 */
@Composable
internal fun IslandPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    val atTop = preferences.playerBarPosition == PlayerBarPosition.TOP
    val backdrop = LocalGlassBackdrop.current
    val moving = LocalMotion.current
    val shown = playerBarButtons(PlayerBarStyle.ISLAND, narrow = false, preferences.desktop.hiddenPlayerButtons, playerButtonsInUse(state))

    var hovered by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(hovered, held, menuOpen) {
        if (hovered || held || menuOpen) {
            open = true
        } else {
            delay(ISLAND_LINGER_MS)
            open = false
        }
    }

    // Under the window's glass, the gap is the window's; otherwise the Island keeps its own off the edge.
    val gap = if (backdrop == null) Glass.FLOAT_INSET_DP.dp else 0.dp
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(ISLAND_HEIGHT + gap),
        contentAlignment = if (atTop) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        val wanted by animateDpAsState(if (open) 540.dp else 420.dp, motionSpec(), label = "islandWidth")
        val shape = RoundedCornerShape(liftedCorner(preferences.cornerStyle, ISLAND_HEIGHT))
        Box(
            Modifier
                .padding(top = if (atTop) gap else 0.dp, bottom = if (atTop) 0.dp else gap)
                .width(wanted.coerceAtMost(maxWidth - 24.dp))
                .wrapContentHeight(if (atTop) Alignment.Top else Alignment.Bottom, unbounded = true)
                .pointerInput(Unit) {
                    // Watches without consuming: whether the pointer is over the pill, and whether it is held.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val position = event.changes.firstOrNull()?.position
                            hovered = when (event.type) {
                                PointerEventType.Exit -> false
                                PointerEventType.Enter -> true
                                else -> position != null && position.x in 0f..size.width.toFloat() && position.y in 0f..size.height.toFloat()
                            }
                            held = event.changes.any { it.pressed }
                        }
                    }
                },
        ) {
            val content: @Composable () -> Unit = {
                Column(Modifier.fillMaxWidth()) {
                    val seekRow: @Composable () -> Unit = {
                        AnimatedVisibility(
                            open,
                            enter = if (moving) expandVertically(tween(MotionTiming.STANDARD)) + fadeIn(tween(MotionTiming.STANDARD)) else EnterTransition.None,
                            exit = if (moving) shrinkVertically(tween(MotionTiming.STANDARD)) + fadeOut(tween(MotionTiming.LEAVING)) else ExitTransition.None,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = if (atTop) 0.dp else 6.dp, bottom = if (atTop) 6.dp else 0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlaybackProgressBar(
                                    playback = playback,
                                    onSeek = state::seekTo,
                                    modifier = Modifier.weight(1f),
                                    style = preferences.progressBarStyle,
                                    timeDisplay = preferences.timeDisplay,
                                )
                                if (PlayerButton.VOLUME in shown) {
                                    Spacer(Modifier.width(4.dp))
                                    VolumeControl(playback, state) { menuOpen = it }
                                }
                            }
                        }
                    }
                    // Away from the edge it sits on, so the row with play in it stays under the pointer.
                    if (!atTop) seekRow()
                    Box(Modifier.fillMaxWidth().height(ISLAND_HEIGHT)) {
                        Row(
                            Modifier.fillMaxSize().padding(start = 8.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BarCover(current, 36.dp, (liftedCorner(preferences.cornerStyle, ISLAND_HEIGHT) - 8.dp).coerceAtLeast(4.dp), state)
                            Spacer(Modifier.width(10.dp))
                            BarTitle(current, playback, Modifier.weight(1f), state = state, titleSize = 13.sp, detailSize = 11.sp, snug = true)
                            Spacer(Modifier.width(6.dp))
                            Transport(queue, playback, state, playSize = 36.dp, shown = shown, buttonSize = 32.dp)
                            if (PlayerButton.SLEEP_TIMER in shown) SleepTimerButton(state)
                            if (PlayerButton.DEVICES in shown) ConnectButton(state)
                        }
                        // The song's place, along the foot of the pill and kept off its rounded ends, while it is
                        // shut. Open, the seek bar says the same thing larger.
                        val line by animateFloatAsState(if (open) 0f else 1f, motionSpec(MotionTiming.QUICK), label = "islandLine")
                        if (line > 0f) {
                            ProgressLine(
                                playback,
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = liftedCorner(preferences.cornerStyle, ISLAND_HEIGHT).coerceAtLeast(14.dp))
                                    .padding(bottom = 3.dp)
                                    .graphicsLayer { alpha = line },
                            )
                        }
                    }
                    if (atTop) seekRow()
                }
            }
            if (backdrop != null) {
                GlassPane(backdrop, Modifier.fillMaxWidth(), cornerRadius = liftedCorner(preferences.cornerStyle, ISLAND_HEIGHT)) { content() }
            } else {
                LiftedSurface(shape, Modifier.fillMaxWidth()) { content() }
            }
        }
    }
}

/** How long an Island waits after the pointer leaves before it shuts. */
private const val ISLAND_LINGER_MS = 420L

/**
 * The song in a display at the centre, the way a stereo shows it and the Mac's Music app has it: the transport
 * to its left, the volume and the tools to its right.
 *
 * The display is a card set a little into the bar, with the cover flush against its left end, the title and the
 * artist centred in the rest, and a thin seek bar of the chosen style along its foot with the times at its ends,
 * so the song's place is read where its name is. It stays in the true middle of the window whatever is either
 * side, and is as wide as those leave it, up to a point.
 */
@Composable
internal fun DisplayPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    BarSurface(preferences.playerBarPosition, height = 76.dp) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val narrow = maxWidth < 900.dp
            val tight = maxWidth < 700.dp
            val shown = playerBarButtons(
                PlayerBarStyle.DISPLAY,
                narrow,
                preferences.desktop.hiddenPlayerButtons,
                playerButtonsInUse(state),
                tight = tight,
            )
            // What each side needs at this width, so the display has the rest and the sides never squeeze.
            val side = when {
                tight -> 118.dp
                narrow -> 178.dp
                else -> 214.dp
            }
            val middle = minOf(maxWidth * .46f, 660.dp, maxWidth - 32.dp - side * 2).coerceAtLeast(200.dp)
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Transport(queue, playback, state, playSize = 38.dp, shown = shown, buttonSize = 32.dp)
                }
                SongDisplay(
                    current,
                    playback,
                    state,
                    showLike = PlayerButton.LIKE in shown,
                    modifier = Modifier.width(middle).fillMaxHeight().padding(vertical = 7.dp),
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (!narrow) current?.let { DownloadButton(it, state, size = 34.dp) }
                    BarTools(queue, playback, state, shown)
                }
            }
        }
    }
}

/**
 * The Display bar's display: the cover flush on the left, the song centred, the seek bar along the foot.
 *
 * Shaded a little along its top edge, the way light falls into something set below the surface round it, which
 * is what makes it read as a display and not as one more card. The heart sits at its right end, where the Mac
 * puts its own.
 */
@Composable
private fun SongDisplay(current: Track?, playback: PlaybackState, state: AppState, showLike: Boolean, modifier: Modifier) {
    val preferences = state.settings.collectAsState().value.preferences
    val spotify = state.settings.collectAsState().value.spotify
    val shape = MaterialTheme.shapes.small
    val shade = ink(.07f)
    Row(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(listOf(shade, Color.Transparent), endY = 7.dp.toPx()))
            }
            .border(1.dp, ink(.09f), shape),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clickable(enabled = current != null) { state.navigate(Destination.NOW_PLAYING) },
        ) {
            if (current != null) {
                RemoteArtwork(current.artworkUrl, current.provider, Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(
                Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MotionContent(
                    current,
                    Modifier
                        // Clear of the heart at the right, and as clear on the left, so the song stays centred.
                        .padding(horizontal = if (showLike && current != null) 26.dp else 0.dp)
                        .clickable(enabled = current != null) { state.navigate(Destination.NOW_PLAYING) },
                    kind = MotionKind.TRACK,
                    contentAlignment = Alignment.TopCenter,
                    contentKey = { it?.queueKey },
                ) { track ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            track?.title ?: "Nothing playing",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                playback.errorMessage
                                    ?: track?.let { listOfNotNull(it.artistLine.takeIf(String::isNotBlank), it.album?.title?.takeIf(String::isNotBlank)).joinToString(" — ") }
                                    ?: "Choose a track to start",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (playback.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (playsOnSpotify(track, spotify)) {
                                Spacer(Modifier.width(6.dp))
                                OnSpotifyMark()
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                PlaybackProgressBar(
                    playback = playback,
                    onSeek = state::seekTo,
                    modifier = Modifier.fillMaxWidth(),
                    style = preferences.progressBarStyle,
                    timeDisplay = preferences.timeDisplay,
                    compact = true,
                )
            }
            if (showLike && current != null) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp)) { LikeButton(current, state, size = 28.dp) }
            }
        }
    }
}

/**
 * What a lifted bar is made of when the surfaces are solid: the card's colour on a dark page and the panel's on a
 * pale one, so it stands off either; a hairline edge, for a black page where no shadow shows; and a soft shadow.
 * Inside the window's glass, nothing but what it holds: the pane is already all of that.
 */
@Composable
private fun LiftedSurface(shape: Shape, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    if (LocalInGlass.current) {
        Box(modifier, content = content)
        return
    }
    val light = MaterialTheme.colorScheme.background.luminance() > .5f
    Box(
        modifier
            .shadow(16.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .30f), spotColor = Color.Black.copy(alpha = .42f))
            .clip(shape)
            .background(if (light) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, ink(if (light) .07f else .09f), shape),
        content = content,
    )
}

/** The song's place as a thin line and nothing else: the Island's, at rest, where seeking is the open pill's. */
@Composable
private fun ProgressLine(playback: PlaybackState, modifier: Modifier) {
    val filled = MaterialTheme.colorScheme.primary
    val track = ink(.14f)
    val fraction = playbackFraction(playback.positionMs.toFloat(), playback.durationMs)
    Canvas(modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50))) {
        drawRect(track)
        drawRect(filled, size = Size(size.width * fraction, size.height))
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
    val shape = skinShape(RoundedCornerShape(corner))
    val modifier = Modifier.size(size).clip(shape).clickable(enabled = track != null) { state.navigate(Destination.NOW_PLAYING) }
    if (track != null) {
        RemoteArtwork(track.artworkUrl, track.provider, modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, Modifier.size(size * .4f))
        }
    }
}

/**
 * Title and artist on one line, the artist quieter, or what went wrong in its place, in red -- and the
 * On Spotify tag after them when Spotify's own app is the one playing.
 */
@Composable
private fun SlimLine(current: Track?, playback: PlaybackState, state: AppState, modifier: Modifier) {
    val onSpotify = playsOnSpotify(current, state.settings.collectAsState().value.spotify)
    Row(
        modifier.clickable(enabled = current != null) { state.navigate(Destination.NOW_PLAYING) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onSpotify) {
            Spacer(Modifier.width(8.dp))
            OnSpotifyMark()
        }
    }
}

/**
 * Title over artist, or over what went wrong, in red, when something did -- with the On Spotify tag after the
 * artist when Spotify's own app is the one playing.
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
    /** Lines no taller than the type, for a bar with little height to spare. */
    snug: Boolean = false,
) {
    val spotify = state.settings.collectAsState().value.spotify
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
                lineHeight = if (snug) titleSize * 1.3f else TextUnit.Unspecified,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    playback.errorMessage ?: track?.artistLine ?: "Choose a track to start",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (playback.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = detailSize,
                    lineHeight = if (snug) detailSize * 1.3f else TextUnit.Unspecified,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (playsOnSpotify(track, spotify)) {
                    Spacer(Modifier.width(6.dp))
                    OnSpotifyMark()
                }
            }
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

/** Previous, play and next, with shuffle and repeat either side when the bar has them in [shown]. */
@Composable
private fun Transport(
    queue: QueueState,
    playback: PlaybackState,
    state: AppState,
    playSize: Dp,
    shown: Set<PlayerButton>,
    buttonSize: Dp = 36.dp,
) {
    val lit = MaterialTheme.colorScheme.primary
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (PlayerButton.SHUFFLE in shown) {
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
        // Lit while next leads somewhere, autoplay's lined-up songs included: see QueueState.hasNext.
        IconButton(state::next, Modifier.size(buttonSize), enabled = queue.hasNext) {
            Icon(Icons.Default.SkipNext, "Next track", Modifier.size(buttonSize * .58f))
        }
        if (PlayerButton.REPEAT in shown) {
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

/**
 * Connect, the sleep timer, the queue, lyrics and volume, in the order Inline has them -- those of them in
 * [shown], which is the layout's own choice at its width less whatever the listener put away.
 */
@Composable
private fun BarTools(queue: QueueState, playback: PlaybackState, state: AppState, shown: Set<PlayerButton>) {
    if (PlayerButton.DEVICES in shown) ConnectButton(state)
    if (PlayerButton.SLEEP_TIMER in shown) SleepTimerButton(state)
    if (PlayerButton.QUEUE in shown) QueueButton(queue, state, 34.dp)
    if (PlayerButton.LYRICS in shown) {
        IconButton({ state.navigate(Destination.NOW_PLAYING) }, Modifier.size(34.dp)) {
            Icon(Icons.Default.Lyrics, "Lyrics", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (PlayerButton.VOLUME in shown) VolumeControl(playback, state)
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

// --- The pictures ---

/**
 * The layouts, each as a picture of itself with its name under it, as the now playing screen's are offered.
 *
 * Pictures rather than a row of names, because the difference between Slim and Slim left, or Floating and the
 * Island, is where things are, which is quicker seen than read.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlayerBarStylePicker(selected: PlayerBarStyle, choose: (PlayerBarStyle) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PlayerBarStyle.entries.forEach { style ->
            val active = style == selected
            val border by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else ink(.1f),
                motionSpec(MotionTiming.QUICK),
                label = "bar-style-border",
            )
            Surface(
                onClick = { choose(style) },
                shape = RoundedCornerShape(12.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else ink(.04f),
                border = BorderStroke(if (active) 2.dp else 1.dp, border),
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerBarPicture(style, active, Modifier.size(112.dp, 70.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        style.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * A small picture of [style]: a window with a page of faint cards, and the bar where it sits on it -- the cover in
 * the accent, the writing as bars, the buttons as dots, the seek bar as a line part lit. The same marks the now
 * playing layouts are drawn in, so the two pickers read as one family.
 */
@Composable
internal fun PlayerBarPicture(style: PlayerBarStyle, selected: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val writing = MaterialTheme.colorScheme.onSurface
    val lightPage = MaterialTheme.colorScheme.background.luminance() > .5f
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = h * .04f

        fun bar(x: Float, y: Float, length: Float, alpha: Float = .32f, thick: Float = stroke, colour: Color = writing) =
            drawRoundRect(colour.copy(alpha = alpha), Offset(x, y - thick / 2), Size(length, thick), CornerRadius(thick / 2))

        fun text(x: Float, cy: Float, length: Float) {
            bar(x, cy - h * .028f, length, alpha = .62f, thick = stroke * 1.15f)
            bar(x, cy + h * .03f, length * .65f, alpha = .3f, thick = stroke * .9f)
        }

        fun cover(x: Float, cy: Float, side: Float) = drawRoundRect(
            accent.copy(alpha = if (selected) .9f else .6f),
            Offset(x, cy - side / 2),
            Size(side, side),
            CornerRadius(side * .18f),
        )

        fun dot(x: Float, cy: Float, radius: Float = h * .022f, alpha: Float = .4f) =
            drawCircle(writing.copy(alpha = alpha), radius, Offset(x, cy))

        fun transport(cx: Float, cy: Float, gap: Float = h * .085f, play: Float = h * .045f) {
            dot(cx - gap, cy)
            drawCircle(accent, play, Offset(cx, cy))
            dot(cx + gap, cy)
        }

        fun tools(right: Float, cy: Float, count: Int = 3) = repeat(count) { i -> dot(right - i * h * .075f, cy, alpha = .28f) }

        fun seek(x: Float, cy: Float, length: Float, lit: Float = .4f) {
            bar(x, cy, length, alpha = .2f, thick = stroke * .7f)
            bar(x, cy, length * lit, alpha = 1f, thick = stroke * .7f, colour = accent)
        }

        // The page: two rows of cards, stopping where a bar that takes a strip of the window begins.
        fun page(bottom: Float) {
            val width = w * .2f
            listOf(h * .08f, h * .44f).forEach { top ->
                repeat(4) { i ->
                    val left = w * .055f + i * w * .232f
                    val cardBottom = minOf(top + h * .3f, bottom)
                    if (cardBottom > top) {
                        drawRoundRect(writing.copy(alpha = .07f), Offset(left, top), Size(width, cardBottom - top), CornerRadius(h * .04f))
                    }
                }
            }
        }

        // A strip along the foot of the window, ruled along its top edge, as the bars that take one are.
        fun strip(height: Float): Float {
            val top = h - height
            page(top)
            drawRect(writing.copy(alpha = .05f), Offset(0f, top), Size(w, height))
            drawRect(accent.copy(alpha = .3f), Offset(0f, top), Size(w, h * .012f))
            return top
        }

        // A bar lifted off the page, which runs on beneath it: a soft shadow, then the bar.
        fun lifted(left: Float, top: Float, width: Float, height: Float) {
            page(h)
            val corner = CornerRadius(height / 2)
            drawRoundRect(Color.Black.copy(alpha = if (lightPage) .1f else .3f), Offset(left, top + h * .02f), Size(width, height), corner)
            // White on a pale page, as the bar itself is there; lighter than the page on a dark one.
            drawRoundRect(if (lightPage) Color.White else writing.copy(alpha = .14f), Offset(left, top), Size(width, height), corner)
            drawRoundRect(writing.copy(alpha = .14f), Offset(left, top), Size(width, height), corner, style = Stroke(h * .01f))
        }

        when (style) {
            // Taskbar is drawn by the Windows skin work.
            PlayerBarStyle.INLINE, PlayerBarStyle.TASKBAR -> {
                val cy = strip(h * .2f) + h * .1f
                transport(w * .1f, cy)
                cover(w * .21f, cy, h * .11f)
                text(w * .21f + h * .15f, cy, w * .1f)
                seek(w * .42f, cy, w * .37f)
                tools(w * .94f, cy, 2)
            }
            PlayerBarStyle.STACKED -> {
                val top = strip(h * .3f)
                seek(w * .05f, top + h * .055f, w * .9f, lit = .35f)
                val cy = top + h * .18f
                cover(w * .05f, cy, h * .12f)
                text(w * .05f + h * .16f, cy, w * .14f)
                transport(w * .5f, cy)
                tools(w * .94f, cy, 2)
            }
            PlayerBarStyle.CENTERED -> {
                val top = strip(h * .26f)
                val cy = top + h * .13f
                cover(w * .05f, cy, h * .12f)
                text(w * .05f + h * .16f, cy, w * .14f)
                transport(w * .5f, cy - h * .035f)
                seek(w * .37f, cy + h * .065f, w * .26f)
                tools(w * .94f, cy, 2)
            }
            PlayerBarStyle.SLIM, PlayerBarStyle.SLIM_LEFT -> {
                val top = strip(h * .14f)
                // Slim's progress is the hairline along the edge that faces the page.
                bar(0f, top + h * .006f, w, alpha = .14f, thick = h * .012f)
                bar(0f, top + h * .006f, w * .42f, alpha = 1f, thick = h * .012f, colour = accent)
                val cy = top + h * .075f
                val gap = h * .06f
                if (style == PlayerBarStyle.SLIM_LEFT) {
                    transport(w * .09f, cy, gap, h * .035f)
                    cover(w * .17f, cy, h * .07f)
                    bar(w * .17f + h * .1f, cy, w * .3f, alpha = .55f)
                } else {
                    cover(w * .04f, cy, h * .07f)
                    bar(w * .04f + h * .1f, cy, w * .36f, alpha = .55f)
                    transport(w * .72f, cy, gap, h * .035f)
                }
                tools(w * .95f, cy, 2)
            }
            PlayerBarStyle.SPOTLIGHT -> {
                val top = h - h * .36f
                page(top)
                drawRect(
                    Brush.horizontalGradient(listOf(accent.copy(alpha = .38f), accent.copy(alpha = .12f), Color.Transparent)),
                    Offset(0f, top),
                    Size(w, h - top),
                )
                val cy = top + h * .18f
                cover(w * .05f, cy, h * .24f)
                val column = w * .05f + h * .3f
                bar(column, cy - h * .06f, w * .26f, alpha = .7f, thick = stroke * 1.4f)
                bar(column, cy, w * .16f)
                seek(column, cy + h * .075f, w * .34f)
                transport(w * .8f, cy, h * .1f, h * .06f)
            }
            PlayerBarStyle.FLOATING -> {
                val height = h * .19f
                val top = h - height - h * .06f
                lifted(w * .04f, top, w * .92f, height)
                val cy = top + height / 2
                transport(w * .12f, cy)
                cover(w * .25f, cy, h * .1f)
                text(w * .25f + h * .14f, cy, w * .1f)
                seek(w * .46f, cy, w * .3f)
                tools(w * .9f, cy, 2)
            }
            PlayerBarStyle.ISLAND -> {
                val height = h * .15f
                val width = w * .5f
                val left = (w - width) / 2
                val top = h - height - h * .06f
                lifted(left, top, width, height)
                val cy = top + height / 2
                cover(left + height * .2f, cy, height * .66f)
                text(left + height * 1.05f, cy, width * .26f)
                transport(left + width - h * .14f, cy, h * .065f, h * .038f)
                // The progress along its foot, kept off the rounded ends.
                bar(left + height * .5f, top + height - h * .018f, width - height, alpha = .16f, thick = h * .012f)
                bar(left + height * .5f, top + height - h * .018f, (width - height) * .42f, alpha = 1f, thick = h * .012f, colour = accent)
            }
            PlayerBarStyle.DISPLAY -> {
                val top = strip(h * .24f)
                val cy = top + h * .12f
                transport(w * .13f, cy)
                tools(w * .94f, cy, 2)
                // The display: the cover flush on its left, the song centred, the seek bar along its foot.
                val left = w * .3f
                val width = w * .4f
                val displayTop = top + h * .035f
                val displayHeight = h * .17f
                val corner = CornerRadius(h * .025f)
                drawRoundRect(writing.copy(alpha = .1f), Offset(left, displayTop), Size(width, displayHeight), corner)
                drawRoundRect(writing.copy(alpha = .16f), Offset(left, displayTop), Size(width, displayHeight), corner, style = Stroke(h * .008f))
                drawRoundRect(accent.copy(alpha = if (selected) .9f else .6f), Offset(left, displayTop), Size(displayHeight, displayHeight), corner)
                val textLeft = left + displayHeight
                val textWidth = width - displayHeight
                bar(textLeft + textWidth * .28f, displayTop + displayHeight * .3f, textWidth * .44f, alpha = .62f)
                bar(textLeft + textWidth * .35f, displayTop + displayHeight * .55f, textWidth * .3f, alpha = .3f, thick = stroke * .8f)
                seek(textLeft + textWidth * .12f, displayTop + displayHeight * .82f, textWidth * .76f)
            }
        }
    }
}

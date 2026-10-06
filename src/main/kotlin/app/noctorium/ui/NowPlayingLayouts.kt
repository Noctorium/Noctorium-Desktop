package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.noctorium.core.AppState
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.CoverStyle
import app.noctorium.settings.NowPlayingPreferences
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/*
 * The now playing layouts added beside the first six.
 *
 * Each is a different answer to what the screen is for. Full cover and Split give the cover the room it was
 * made for. Cover flow shows the queue as the row of records it is. Turntable makes the song's place something
 * that can be read from across the room. Big type forgets the cover for the words. They are made of the same
 * parts as the first six -- the transport, the seek bar, the row of actions under it, the panel -- so a control
 * behaves the same whichever layout it is in.
 */

/**
 * Full cover: the cover is the screen, cropped to fill it, and the song, its seek bar, its controls and its
 * actions sit over its foot, where the page's own colour rises behind them. The page's colour rather than black,
 * so that the theme's own writing reads on it, on a pale theme as on a dark one.
 *
 * The cover comes at the largest size the artwork loader holds. Its style and size are for a cover set on the
 * page; this one is the page, so they are not asked.
 */
@Composable
internal fun ImmersiveNowPlaying(
    track: Track,
    playback: PlaybackState,
    state: AppState,
    look: NowPlayingPreferences,
    arrange: () -> Unit,
    hasNext: Boolean,
) {
    val page = MaterialTheme.colorScheme.background
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
        // The scrim: nothing over most of the cover, rising to nearly the page over the bottom part of it.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to page.copy(alpha = .22f),
                    .12f to Color.Transparent,
                    .55f to Color.Transparent,
                    .78f to page.copy(alpha = .74f),
                    1f to page.copy(alpha = .95f),
                ),
            ),
        )
        val wide = maxWidth >= 900.dp
        val titleSize = (maxWidth.value / 21f).coerceIn(30f, 76f).sp
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = if (wide) 44.dp else 24.dp, vertical = if (wide) 30.dp else 16.dp)
                .widthIn(max = 900.dp),
        ) {
            SongHeading(track, state, titleSize, artistSize = (titleSize.value * .36f).coerceIn(15f, 27f).sp)
            Spacer(Modifier.height(if (wide) 20.dp else 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportControls(playback, state, hasNext)
                Spacer(Modifier.width(20.dp))
                LayoutSeekBar(playback, state, Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            HeroFooter(track, playback, state, look, arrange)
        }
    }
}

/**
 * Split: the cover fills the left half from top to bottom, cropped to fill it, and the right half has the song,
 * its controls and its actions at the top and the panel -- up next, the lyrics -- in the rest. With the panel
 * tucked away, the song takes the middle of its half instead.
 *
 * The right half is never narrower than the panel width asks for, so a wide panel keeps its width in a smaller
 * window. In a narrow window the halves stack, the cover a band across the top, and in one too short for the
 * panel as well it waits until there is room, rather than being squeezed to its tabs.
 */
@Composable
internal fun SplitNowPlaying(
    track: Track,
    playback: PlaybackState,
    state: AppState,
    look: NowPlayingPreferences,
    showPanel: Boolean,
    wide: Boolean,
    panelWidth: Dp,
    panel: @Composable (Modifier, Boolean) -> Unit,
    arrange: () -> Unit,
    hasNext: Boolean,
) {
    val details: @Composable (TextUnit) -> Unit = { titleSize ->
        SongHeading(track, state, titleSize, artistSize = 15.sp, maxLines = 2)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TransportControls(playback, state, hasNext)
            Spacer(Modifier.width(16.dp))
            LayoutSeekBar(playback, state, Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        HeroFooter(track, playback, state, look, arrange)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val across = maxWidth
        val down = maxHeight
        if (wide) {
            val right = maxOf(across / 2, panelWidth + 56.dp).coerceAtMost(across * .62f)
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                    RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
                }
                Column(Modifier.width(right).fillMaxHeight().padding(horizontal = 28.dp, vertical = 24.dp)) {
                    if (!showPanel) Spacer(Modifier.weight(1f))
                    details(if (across >= 1500.dp) 36.sp else 30.sp)
                    if (showPanel) {
                        Spacer(Modifier.height(16.dp))
                        panel(Modifier.fillMaxWidth().weight(1f), false)
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            val stacked = showPanel && down >= ROOM_FOR_PANEL
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(down * (if (stacked) .32f else .5f)).clipToBounds()) {
                    RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
                }
                Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 14.dp)) {
                    details(24.sp)
                    if (stacked) {
                        Spacer(Modifier.height(12.dp))
                        panel(Modifier.fillMaxWidth().weight(1f), false)
                    }
                }
            }
        }
    }
}

/**
 * How tall a narrow window has to be for a layout to put its panel under the rest. Shorter, and the panel would
 * be squeezed to its row of tabs, while what it pushed up lost the room it needs.
 */
private val ROOM_FOR_PANEL = 620.dp

/** How far a sleeve's reflection reaches below it, against the sleeve itself. */
private const val FLOW_REFLECTION = .3f

/** How many of the queue's covers stand either side of the one playing. */
private const val FLOW_REACH = 4

/** Room above and below the row, against a cover's side, for the near edges of the turned ones. */
private const val FLOW_MARGIN = .07f

/**
 * Cover flow, as iTunes had it: the queue's covers in a row, the one playing large and facing forward in the
 * middle, and up to four either side turned towards it, overlapping, smaller and dimmer the further out they
 * are, each over a faded reflection of itself. A click on one of them plays it. The row glides along when the
 * song changes, and jumps where nothing is to move. The song, the seek bar and the controls are under it.
 *
 * The covers take the cover's corners, round included; a record's sleeve is square, so Record is drawn rounded.
 */
@Composable
internal fun CoverFlowNowPlaying(
    queue: QueueState,
    track: Track,
    playback: PlaybackState,
    state: AppState,
    look: NowPlayingPreferences,
    arrange: () -> Unit,
    hasNext: Boolean,
) {
    // The row and the song under it kept together in the middle, however much room is left round them.
    Column(
        Modifier.fillMaxSize().padding(top = 20.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverFlow(queue, state, look, Modifier.weight(1f, fill = false).fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SongHeading(track, state, 24.sp, artistSize = 14.sp, centred = true)
            Spacer(Modifier.height(10.dp))
            LayoutSeekBar(playback, state, Modifier.fillMaxWidth())
            TransportControls(playback, state, hasNext)
            HeroFooter(track, playback, state, look, arrange)
        }
    }
}

@Composable
private fun CoverFlow(queue: QueueState, state: AppState, look: NowPlayingPreferences, modifier: Modifier) {
    val tracks = queue.tracks
    val position by animateFloatAsState(queue.currentIndex.coerceAtLeast(0).toFloat(), motionSpec(520), label = "coverFlow")
    val page = MaterialTheme.colorScheme.background
    BoxWithConstraints(modifier.clipToBounds()) {
        // Room above and below for the turned covers, whose near edges stand taller than the one in the middle.
        val side = minOf(maxHeight / (1f + FLOW_REFLECTION + FLOW_MARGIN * 2), maxWidth * .34f, 470.dp * look.coverSize.scale)
        val top = side * FLOW_MARGIN
        val shape = when (look.cover) {
            CoverStyle.SQUARE -> RectangleShape
            CoverStyle.CIRCLE -> CircleShape
            else -> RoundedCornerShape(side * .03f)
        }
        val middle = position.roundToInt()
        // As tall as the row and no taller, so whatever is under it sits right under it.
        Box(Modifier.fillMaxWidth().height(side * (1f + FLOW_REFLECTION + FLOW_MARGIN * 2))) {
            if (tracks.isNotEmpty()) {
                for (index in (middle - FLOW_REACH - 1).coerceAtLeast(0)..(middle + FLOW_REACH + 1).coerceAtMost(tracks.lastIndex)) {
                    key(tracks[index].queueKey, index) {
                        FlowSleeve(
                            tracks[index],
                            offset = index - position,
                            side = side,
                            top = top,
                            shape = shape,
                            page = page,
                            play = if (index == queue.currentIndex) null else ({ state.jumpToQueueItem(index) }),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One cover in the flow, [offset] places from the middle, fractional while the row glides. Within one place of
 * the middle it turns from facing forward to facing in; past that the covers stand stacked and step back.
 */
@Composable
private fun BoxScope.FlowSleeve(
    track: Track,
    offset: Float,
    side: Dp,
    top: Dp,
    shape: Shape,
    page: Color,
    play: (() -> Unit)?,
) {
    val distance = abs(offset)
    val turned = distance.coerceAtMost(1f)
    val beyond = (distance - 1f).coerceAtLeast(0f)
    val direction = sign(offset)
    val sidePx = with(LocalDensity.current) { side.toPx() }
    val density = LocalDensity.current.density
    // The further out, the more of the page shows through: darker on a dark page, paler on a pale one.
    val dim = (.2f * turned + .13f * beyond).coerceAtMost(.75f)
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .padding(top = top)
            .zIndex(-distance)
            .graphicsLayer {
                translationX = direction * (turned * sidePx * .64f + beyond * sidePx * .25f)
                rotationY = -direction * 62f * turned
                val scale = 1f - .14f * turned - .05f * beyond
                scaleX = scale
                scaleY = scale
                cameraDistance = 14f * density
                transformOrigin = TransformOrigin(.5f, .5f / (1f + FLOW_REFLECTION))
                alpha = (FLOW_REACH + 1 - distance).coerceIn(0f, 1f)
            }
            // The one playing takes its clicks and does nothing with them, so none falls through to a cover behind it.
            .then(if (play != null) Modifier.clickable(onClick = play) else Modifier.pointerInput(Unit) { detectTapGestures {} }),
    ) {
        Box(Modifier.size(side).clip(shape)) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
            if (dim > 0f) Box(Modifier.fillMaxSize().background(page.copy(alpha = dim)))
        }
        // The reflection: the cover upside down under itself, fading out as it goes.
        Box(
            Modifier
                .padding(top = side * .015f)
                .size(side, side * FLOW_REFLECTION)
                .clipToBounds()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Gone well before the foot, and laid a pixel past every edge: a partly covered pixel at the edge
                    // would keep part of the cover, and turned in three dimensions that shows as a line.
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black.copy(alpha = .36f), .8f to Color.Transparent, startY = 0f, endY = size.height),
                        topLeft = Offset(-1f, -1f),
                        size = Size(size.width + 2f, size.height + 2f),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            Box(
                Modifier
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .size(side)
                    .graphicsLayer { scaleY = -1f }
                    .clip(shape),
            ) {
                RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
                if (dim > 0f) Box(Modifier.fillMaxSize().background(page.copy(alpha = dim)))
            }
        }
    }
}

/**
 * Turntable: the record on a turntable, the cover for its label, turning at 33 and a third while the music
 * plays, with an arm that crosses it as the song goes; the song and the controls under the deck, and the panel
 * beside it -- under it in a narrow window, and in one too short for both, left out until there is room. The arm
 * rests on its stand while nothing is loaded -- while a song is still being found, say -- and swings down onto the
 * record when it starts.
 *
 * The cover's size makes the deck larger or smaller; its style is the record's, whatever is chosen.
 */
@Composable
internal fun TurntableNowPlaying(
    track: Track,
    playback: PlaybackState,
    state: AppState,
    look: NowPlayingPreferences,
    showPanel: Boolean,
    wide: Boolean,
    panelWidth: Dp,
    panel: @Composable (Modifier, Boolean) -> Unit,
    arrange: () -> Unit,
    hasNext: Boolean,
) {
    val loaded = playback.track?.queueKey == track.queueKey &&
        playback.durationMs > 0 &&
        (playback.status == PlaybackStatus.PLAYING || playback.status == PlaybackStatus.PAUSED)
    val progress = if (loaded) playbackFraction(playback.positionMs.toFloat(), playback.durationMs) else null
    val deck: @Composable (Modifier) -> Unit = { modifier ->
        // The deck and the song under it kept together in the middle, however much room is left round them.
        Column(
            modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val width = minOf(maxWidth, maxHeight * TURNTABLE_ASPECT, 860.dp * look.coverSize.scale)
                Turntable(track, playback.isPlaying, progress, Modifier.size(width, width / TURNTABLE_ASPECT))
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.widthIn(max = 660.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SongHeading(track, state, 22.sp, artistSize = 14.sp, centred = true, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransportControls(playback, state, hasNext)
                    Spacer(Modifier.width(16.dp))
                    LayoutSeekBar(playback, state, Modifier.weight(1f))
                }
                HeroFooter(track, playback, state, look, arrange)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        when {
            showPanel && wide -> Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                deck(Modifier.weight(1f).fillMaxHeight())
                panel(Modifier.width(panelWidth).fillMaxHeight(), false)
            }
            showPanel && maxHeight >= ROOM_FOR_PANEL -> Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                deck(Modifier.fillMaxWidth().weight(1.3f))
                panel(Modifier.fillMaxWidth().weight(1f), false)
            }
            else -> deck(Modifier.fillMaxSize())
        }
    }
}

/**
 * Big type: no cover, only the words, as a poster sets them. The title as large as it will go in three lines at
 * most -- the heaviest weight, the letters drawn close -- the artist under it in a light one, the album small, and
 * then the seek bar and the controls.
 *
 * The title is coloured from the accent into the colour the backdrop takes from the cover, each drawn a little
 * towards the theme's writing so it reads on the page whatever the cover is.
 */
@Composable
internal fun PosterNowPlaying(
    track: Track,
    playback: PlaybackState,
    state: AppState,
    look: NowPlayingPreferences,
    arrange: () -> Unit,
    hasNext: Boolean,
) {
    val accent = MaterialTheme.colorScheme.primary
    val writing = MaterialTheme.colorScheme.onSurface
    val palette = rememberArtworkPalette(track.artworkUrl, track.provider, fallback = ArtworkPalette(accent, accent))
    val from by animateColorAsState(accent.mix(writing, .12f), tween(700), label = "posterFrom")
    val to by animateColorAsState(palette.primary.mix(writing, .3f), tween(700), label = "posterTo")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val margin = if (maxWidth >= 900.dp) 56.dp else 26.dp
        Column(Modifier.fillMaxSize().padding(horizontal = margin, vertical = 26.dp)) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
                val base = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-.035).em,
                    lineHeight = .98.em,
                )
                val titleSize = fittedSize(track.title, base, maxWidth, (maxHeight - 104.dp).coerceAtLeast(56.dp), maxLines = 3)
                Column {
                    Text(
                        track.title,
                        style = base.copy(fontSize = titleSize, brush = Brush.linearGradient(listOf(from, to))),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        track.artistLine,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = (titleSize.value * .3f).coerceIn(17f, 44f).sp,
                        fontWeight = FontWeight.Light,
                        color = ink(.86f),
                    )
                    track.album?.title?.takeIf { it.isNotBlank() && it != track.title }?.let { album ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            album.uppercase(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            color = ink(.5f),
                        )
                    }
                    if (look.followButton) FollowArtistChip(track, state, Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            // Under the words rather than the width of a large screen, where a seek bar would be a thread.
            Column(Modifier.widthIn(max = 1100.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransportControls(playback, state, hasNext)
                    Spacer(Modifier.width(20.dp))
                    LayoutSeekBar(playback, state, Modifier.weight(1f))
                }
                HeroFooter(track, playback, state, look, arrange)
            }
        }
    }
}

/** The most a poster's title is ever set at, in sp: about two lines of a word across a large screen. */
private const val LARGEST_TITLE = 560f

/**
 * The largest size, in steps down from as tall as [height] allows, at which [text] fits [width] in [maxLines]
 * lines within [height], with no word broken across two: a poster's title shrinks to fit, it does not wrap
 * inside a word.
 */
@Composable
private fun fittedSize(text: String, style: TextStyle, width: Dp, height: Dp, maxLines: Int): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, width, height, density) {
        val widthPx = with(density) { width.roundToPx() }.coerceAtLeast(1)
        val heightPx = with(density) { height.toPx() }
        val longestWord = text.split(' ').maxByOrNull { it.length }.orEmpty()
        val smallest = 26f
        var size = with(density) { (height / 1.05f).toSp().value }.coerceIn(smallest, LARGEST_TITLE)
        while (size > smallest) {
            val sized = style.copy(fontSize = size.sp)
            val laid = measurer.measure(text, sized, maxLines = maxLines, constraints = Constraints(maxWidth = widthPx))
            val word = measurer.measure(longestWord, sized, maxLines = 1, softWrap = false)
            if (!laid.hasVisualOverflow && laid.size.height <= heightPx && word.size.width <= widthPx) break
            size *= .93f
        }
        size.coerceAtLeast(smallest).sp
    }
}

/**
 * The song's name, its artist and the follow button, for the layouts that set them over or beside something:
 * the album first, small and spaced, where it says something the title does not.
 */
@Composable
private fun SongHeading(
    track: Track,
    state: AppState,
    titleSize: TextUnit,
    artistSize: TextUnit,
    centred: Boolean = false,
    maxLines: Int = 2,
) {
    val look = state.settings.collectAsState().value.preferences.desktop.nowPlaying
    Column(horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start) {
        if (!centred) {
            track.album?.title?.takeIf { it.isNotBlank() && it != track.title }?.let { album ->
                Text(
                    album.uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ink(.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(6.dp))
            }
        }
        MotionContent(track, kind = MotionKind.TRACK, contentKey = { it.queueKey }) { shown ->
            Column(horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start) {
                Text(
                    shown.title,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = titleSize * 1.12f,
                    textAlign = if (centred) TextAlign.Center else TextAlign.Start,
                )
                Spacer(Modifier.height(3.dp))
                Text(shown.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.66f), fontSize = artistSize)
            }
        }
        if (look.followButton) FollowArtistChip(track, state, Modifier.padding(top = 8.dp))
    }
}

/** The seek bar in the style chosen, as every layout has it. */
@Composable
private fun LayoutSeekBar(playback: PlaybackState, state: AppState, modifier: Modifier) {
    val preferences = state.settings.collectAsState().value.preferences
    PlaybackProgressBar(playback, state::seekTo, modifier, preferences.progressBarStyle, preferences.timeDisplay)
}

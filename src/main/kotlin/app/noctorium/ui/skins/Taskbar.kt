package app.noctorium.ui.skins

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.QueueState
import app.noctorium.playback.RepeatMode
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.PlayerButton
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.ConnectButton
import app.noctorium.ui.LikeButton
import app.noctorium.ui.LocalInGlass
import app.noctorium.ui.PlayPauseIcon
import app.noctorium.ui.PlaybackProgressBar
import app.noctorium.ui.RemoteArtwork
import app.noctorium.ui.SleepTimerButton
import app.noctorium.ui.VolumeControl
import app.noctorium.ui.playerBarButtons
import app.noctorium.ui.playerButtonsInUse
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * The Taskbar player bar: the bar laid out as a desktop's taskbar, in any theme.
 *
 * Along it, left to right: a Start button -- Noctorium's mark and its name -- that opens now playing, as Start
 * opened what mattered; previous, play and next as a Quick Launch toolbar; the song as the taskbar button of the
 * window in front, pressed in; the seek bar across whatever is left; and the tray, with the bar's other buttons
 * as tray icons, the volume, and the clock.
 *
 * Under 98 it is the grey taskbar with its raised edge and the checkered button of the active window; under XP,
 * Luna's blue with the green Start button and the paler blue tray; under every other theme a flat modern
 * taskbar in the theme's own colours. It works along the top of the window as well as the foot, and on a narrow
 * window the song's button goes first and then the tray's icons, so Start, the transport, the seek bar and the
 * clock always stay.
 */
@Composable
fun TaskbarPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val destination = state.ui.collectAsState().value.destination
    val skin = skin()
    val atTop = preferences.playerBarPosition == PlayerBarPosition.TOP
    val inGlass = LocalInGlass.current
    val current = queue.current
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val rule = MaterialTheme.colorScheme.primary.copy(alpha = .22f)
    val height = if (skin.isWindows) 30.dp else 52.dp
    val bar: @Composable () -> Unit = {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(height)
                .drawBehind {
                    when (skin) {
                        ThemeSkin.WINDOWS_98 -> {
                            drawRect(Classic.Face)
                            val p = pixel
                            if (atTop) {
                                drawRect(Classic.Shadow, Offset(0f, size.height - p * 2), Size(size.width, p))
                                drawRect(Classic.DarkShadow, Offset(0f, size.height - p), Size(size.width, p))
                            } else {
                                drawRect(Classic.Light, Offset.Zero, Size(size.width, p))
                                drawRect(Classic.Highlight, Offset(0f, p), Size(size.width, p))
                            }
                        }
                        ThemeSkin.WINDOWS_XP -> drawLunaTaskbar(atTop)
                        ThemeSkin.STANDARD -> {
                            if (!inGlass) {
                                drawRect(surface)
                                drawRect(rule, Offset(0f, if (atTop) size.height - 1.dp.toPx() else 0f), Size(size.width, 1.dp.toPx()))
                            }
                        }
                    }
                },
        ) {
            val narrow = maxWidth < 900.dp
            val cramped = maxWidth < 680.dp
            val shown = playerBarButtons(PlayerBarStyle.TASKBAR, narrow, preferences.desktop.hiddenPlayerButtons, playerButtonsInUse(state))
            Row(
                Modifier.fillMaxSize().padding(horizontal = if (skin.isWindows) 0.dp else 10.dp, vertical = if (skin == ThemeSkin.WINDOWS_98) 3.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StartButton(skin, open = destination == Destination.NOW_PLAYING) { state.navigate(Destination.NOW_PLAYING) }
                QuickLaunch(skin, queue, playback, state)
                if (!cramped) {
                    SongButton(skin, current, Modifier.weight(1f, fill = false).widthIn(max = 280.dp)) {
                        if (current != null) state.navigate(Destination.NOW_PLAYING)
                    }
                }
                PlaybackProgressBar(
                    playback = playback,
                    onSeek = state::seekTo,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = preferences.progressBarStyle,
                    timeDisplay = preferences.timeDisplay,
                )
                Tray(skin) {
                    if (!cramped) TrayIcons(skin, queue, playback, state, shown, current)
                    if (PlayerButton.VOLUME in shown) VolumeControl(playback, state, size = trayButton(skin))
                    Clock(skin)
                }
            }
        }
    }
    if (skin == ThemeSkin.WINDOWS_XP) OnTaskbar(bar) else bar()
}

/** How large a tray icon's button is: the period's sixteen-pixel icons with a little room, or the theme's own. */
private fun trayButton(skin: ThemeSkin): Dp = if (skin.isWindows) 22.dp else 34.dp

/**
 * The Start button: Noctorium's mark and its name, opening now playing, and held in while now playing is open
 * as Start was while its menu was. 98's is a raised button with the name in bold; XP's is the green tab with a
 * rounded right end and the name in white bold italic.
 */
@Composable
private fun StartButton(skin: ThemeSkin, open: Boolean, action: () -> Unit) {
    when (skin) {
        ThemeSkin.WINDOWS_98 -> PushButton(
            action,
            Modifier.padding(start = 2.dp).height(22.dp),
            latched = open,
            checkered = false,
            padding = PaddingValues(start = 3.dp, end = 5.dp),
            minWidth = 0.dp,
            minHeight = 22.dp,
        ) {
            Image(painterResource("noctorium-mark.png"), null, Modifier.size(16.dp), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(3.dp))
            Text("Noctorium", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        ThemeSkin.WINDOWS_XP -> {
            val source = remember { MutableInteractionSource() }
            val hovered by source.collectIsHoveredAsState()
            val pressed by source.collectIsPressedAsState()
            val down = pressed || open
            val pixels = with(LocalDensity.current) { 15.sp.toPx() }
            Row(
                Modifier
                    .fillMaxHeight()
                    .hoverable(source)
                    .clickable(source, indication = null, role = Role.Button, onClick = action)
                    .drawBehind {
                        val radius = size.height * .45f
                        val shape = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width - radius, 0f)
                            quadraticTo(size.width, 0f, size.width, radius)
                            lineTo(size.width, size.height - radius)
                            quadraticTo(size.width, size.height, size.width - radius, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        val light = if (hovered && !down) Luna.StartLight.towards(Color.White, .2f) else Luna.StartLight
                        val deep = if (down) Luna.Start.towards(Color.Black, .25f) else Luna.Start
                        drawPath(shape, Brush.verticalGradient(0f to light, .15f to deep.towards(light, .5f), .5f to deep, 1f to deep.towards(Color.Black, .2f)))
                        drawPath(shape, Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .18f)), startX = size.width * .7f, endX = size.width))
                    }
                    .padding(start = 8.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource("noctorium-mark.png"), null, Modifier.size(20.dp).clip(RoundedCornerShape(3.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(5.dp))
                androidx.compose.material3.Text(
                    "Noctorium",
                    maxLines = 1,
                    style = TextStyle(
                        fontFamily = SkinFonts.trebuchet,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        color = Color.White,
                        shadow = Shadow(Color.Black.copy(alpha = .55f), Offset(1f, 1f), 2f),
                    ).rasterisedFor(pixels),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        ThemeSkin.STANDARD -> androidx.compose.material3.Surface(
            onClick = action,
            shape = RoundedCornerShape(12.dp),
            color = if (open) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = .06f),
        ) {
            Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource("noctorium-mark.png"), null, Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(8.dp))
                Text("Noctorium", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** Previous, play and next as a Quick Launch toolbar: small, flat, raised when pointed at. */
@Composable
private fun QuickLaunch(skin: ThemeSkin, queue: QueueState, playback: PlaybackState, state: AppState) {
    val button = if (skin.isWindows) 24.dp else 38.dp
    val icon = if (skin.isWindows) 16.dp else 22.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (skin.isWindows) 1.dp else 2.dp)) {
        Separator(skin)
        IconButton(state::previous, Modifier.size(button)) { Icon(Icons.Default.SkipPrevious, "Previous track", Modifier.size(icon)) }
        IconButton(state::togglePlayback, Modifier.size(button), enabled = queue.current != null) {
            PlayPauseIcon(playback.isPlaying, Modifier.size(icon))
        }
        IconButton(state::next, Modifier.size(button), enabled = queue.hasNext) { Icon(Icons.Default.SkipNext, "Next track", Modifier.size(icon)) }
        Separator(skin)
    }
}

/** The line between two parts of the taskbar: 98's etched ridge, XP's faint pair of lines, nothing in a modern one. */
@Composable
private fun Separator(skin: ThemeSkin) {
    when (skin) {
        ThemeSkin.WINDOWS_98 -> Box(
            Modifier.padding(horizontal = 3.dp).width(3.dp).height(20.dp).drawBehind {
                thinRaised(0f, 0f, pixel * 3, size.height)
            },
        )
        ThemeSkin.WINDOWS_XP -> Box(
            Modifier.padding(horizontal = 3.dp).width(2.dp).height(20.dp).drawBehind {
                drawRect(Luna.TaskbarFoot, Offset.Zero, Size(pixel, size.height))
                drawRect(Luna.TaskbarTop.towards(Color.White, .35f), Offset(pixel, 0f), Size(pixel, size.height))
            },
        )
        ThemeSkin.STANDARD -> Spacer(Modifier.width(6.dp))
    }
}

/**
 * The song, as the taskbar button of the window in front: its cover for the icon and "Title – Artist" for the
 * window's title, cut short to fit. Under 98 pressed in over the checkerboard, in bold; under XP the darker blue
 * of the active button; in a modern taskbar a soft tile with the accent's short line beneath it.
 */
@Composable
private fun SongButton(skin: ThemeSkin, track: Track?, modifier: Modifier, action: () -> Unit) {
    val label = track?.let { song -> listOf(song.title, song.artistLine).filter(String::isNotBlank).joinToString(" – ") } ?: "Nothing playing"
    val icon: @Composable (Dp) -> Unit = { size ->
        if (track != null) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(size).then(if (skin.isWindows) Modifier else Modifier.clip(RoundedCornerShape(6.dp))))
        } else {
            Icon(Icons.Default.MusicNote, null, Modifier.size(size))
        }
    }
    when (skin) {
        ThemeSkin.WINDOWS_98 -> PushButton(
            action,
            modifier.padding(horizontal = 2.dp).height(22.dp),
            latched = true,
            padding = PaddingValues(horizontal = 4.dp),
            minWidth = 120.dp,
            minHeight = 22.dp,
        ) {
            icon(16.dp)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        ThemeSkin.WINDOWS_XP -> {
            val source = remember { MutableInteractionSource() }
            val hovered by source.collectIsHoveredAsState()
            Row(
                modifier
                    .padding(horizontal = 3.dp)
                    .height(24.dp)
                    .widthIn(min = 120.dp)
                    .hoverable(source)
                    .clickable(source, indication = null, role = Role.Button, onClick = action)
                    .drawBehind {
                        val radius = CornerRadius(3.dp.toPx())
                        val face = if (hovered) Luna.TaskbarFoot.towards(Luna.TaskbarTop, .35f) else Luna.TaskbarFoot.towards(Color.Black, .08f)
                        drawRoundRect(Brush.verticalGradient(listOf(face.towards(Color.Black, .2f), face, face.towards(Luna.TaskbarTop, .3f))), cornerRadius = radius)
                        drawRoundRect(Color.Black.copy(alpha = .35f), topLeft = Offset(pixel / 2, pixel / 2), size = Size(size.width - pixel, size.height - pixel), cornerRadius = radius, style = Stroke(pixel))
                    }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon(16.dp)
                Spacer(Modifier.width(5.dp))
                Text(label, fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        ThemeSkin.STANDARD -> androidx.compose.material3.Surface(
            onClick = action,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .07f),
            modifier = modifier.padding(horizontal = 6.dp),
        ) {
            Box {
                Row(Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    icon(30.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // The short line under the window in front.
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(18.dp, 3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

/**
 * The tray at the far end: 98's sunken well, XP's paler blue with its dark edge on the left, a modern tray's
 * plain run of icons -- with the clock last.
 */
@Composable
private fun Tray(skin: ThemeSkin, content: @Composable RowScope.() -> Unit) {
    val modifier = when (skin) {
        ThemeSkin.WINDOWS_98 -> Modifier
            .padding(end = 2.dp)
            .height(22.dp)
            .drawBehind { thinSunken() }
            .padding(horizontal = 3.dp)
        ThemeSkin.WINDOWS_XP -> Modifier
            .fillMaxHeight()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Luna.Tray.towards(Color.White, .2f), Luna.Tray, Luna.Tray.towards(Luna.TaskbarFoot, .35f))))
                drawRect(Luna.TrayEdge, Offset.Zero, Size(pixel, size.height))
                drawRect(Luna.Tray.towards(Color.White, .45f), Offset(pixel, 0f), Size(pixel, size.height))
            }
            .padding(start = 8.dp, end = 10.dp)
        ThemeSkin.STANDARD -> Modifier.padding(start = 4.dp)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, content = content)
}

/** The bar's other buttons, as the icons in a tray: the heart, shuffle and repeat, the queue, lyrics, the timer and Connect. */
@Composable
private fun TrayIcons(skin: ThemeSkin, queue: QueueState, playback: PlaybackState, state: AppState, shown: Set<PlayerButton>, current: Track?) {
    val button = trayButton(skin)
    val icon = if (skin.isWindows) 16.dp else 19.dp
    val lit = MaterialTheme.colorScheme.primary
    val quiet = LocalContentColor.current.copy(alpha = if (skin.isWindows) 1f else .7f)
    if (PlayerButton.LIKE in shown && current != null) LikeButton(current, state, size = button)
    if (PlayerButton.SHUFFLE in shown) {
        IconButton(state::toggleShuffle, Modifier.size(button)) {
            Icon(Icons.Default.Shuffle, "Shuffle", Modifier.size(icon), tint = if (queue.shuffleEnabled) lit else quiet)
        }
    }
    if (PlayerButton.REPEAT in shown) {
        IconButton(state::cycleRepeat, Modifier.size(button)) {
            Icon(
                if (queue.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                "Repeat",
                Modifier.size(icon),
                tint = if (queue.repeatMode != RepeatMode.OFF) lit else quiet,
            )
        }
    }
    if (PlayerButton.QUEUE in shown) {
        IconButton({ state.navigate(Destination.QUEUE) }, Modifier.size(button)) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", Modifier.size(icon), tint = quiet)
        }
    }
    if (PlayerButton.LYRICS in shown) {
        IconButton({ state.navigate(Destination.NOW_PLAYING) }, Modifier.size(button)) {
            Icon(Icons.Default.Lyrics, "Lyrics", Modifier.size(icon), tint = quiet)
        }
    }
    if (PlayerButton.SLEEP_TIMER in shown) SleepTimerButton(state, size = button)
    if (PlayerButton.DEVICES in shown) ConnectButton(state, Modifier.size(button))
}

/** The time, as the clock in the corner of the screen showed it, moving on at each minute. */
@Composable
private fun Clock(skin: ThemeSkin) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val untilNextMinute = (60 - LocalTime.now().second) * 1_000L - System.currentTimeMillis() % 1_000
            delay(untilNextMinute.coerceAtLeast(250))
            now = LocalTime.now()
        }
    }
    // Newer locale data puts a narrow no-break space before AM and PM, which the period's fonts have no letter for.
    val text = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)).replace(' ', ' ').replace(' ', ' ')
    Text(
        text,
        fontSize = if (skin.isWindows) 11.sp else 12.sp,
        color = when (skin) {
            ThemeSkin.WINDOWS_XP -> Color.White
            ThemeSkin.WINDOWS_98 -> Classic.Text
            ThemeSkin.STANDARD -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        modifier = Modifier.padding(start = if (skin.isWindows) 4.dp else 8.dp, end = if (skin == ThemeSkin.WINDOWS_98) 2.dp else 0.dp),
    )
}

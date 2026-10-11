package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.playback.RepeatMode
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.PlayerButton
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.Luna
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.ToolButton
import app.noctorium.ui.skins.etchedUpright
import app.noctorium.ui.skins.pixel
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.skinShape
import app.noctorium.ui.skins.towards

/*
 * The Toolbar player bar: a desktop music player's toolbar, flat buttons in one row.
 *
 * Add the song to a playlist; previous, play, pause, stop and next, play and pause each a button of their own as
 * they were on the players this is drawn after; the song; the seek bar with the time after it, "0:58 / 2:31";
 * then repeat, shuffle and the volume. Play is lit while the song plays and pause while it is paused -- under a
 * Windows skin they stay pressed in instead, as a toolbar's toggles did -- so the row says what is happening
 * without a button that changes its picture.
 *
 * It sits at the top or the foot of the window as the position setting says. Narrower, the song keeps only its
 * cover and repeat and shuffle go, and narrowest the song and the add button go as well; the transport, the seek bar,
 * the time and the volume always stay.
 */
@Composable
internal fun ToolbarPlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    val skinned = skin() != ThemeSkin.STANDARD
    var adding by remember { mutableStateOf(false) }
    if (adding && current != null) {
        val library by state.library.collectAsState()
        AddToPlaylistDialog(current, library.localPlaylists, state) { adding = false }
    }
    val inGlass = LocalInGlass.current
    val atTop = preferences.playerBarPosition == PlayerBarPosition.TOP
    val rule = MaterialTheme.colorScheme.primary.copy(alpha = .22f)
    // A Windows toolbar's buttons were the size of their sixteen-pixel pictures and a little more.
    val button = if (skinned) 28.dp else 34.dp
    val glyph = if (skinned) 18.dp else 20.dp
    val loaded = playback.status == PlaybackStatus.PLAYING || playback.status == PlaybackStatus.PAUSED
    Surface(color = if (inGlass) Color.Transparent else MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (!atTop && !inGlass) HorizontalDivider(color = rule)
            BoxWithConstraints(Modifier.fillMaxWidth().height(if (skinned) 38.dp else 52.dp)) {
                val narrow = maxWidth < 900.dp
                val tight = maxWidth < 680.dp
                // Narrower again, the song and the add button go too, so the seek bar keeps room to be one.
                val cramped = maxWidth < 540.dp
                val shown = playerBarButtons(PlayerBarStyle.TOOLBAR, narrow, preferences.desktop, playerButtonsInUse(state), tight)
                Row(
                    // Room at the far end for the boost the speaker wears.
                    Modifier.fillMaxSize().padding(start = if (skinned) 4.dp else 10.dp, end = if (skinned) 12.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    if (!cramped) {
                        ToolbarButton(Icons.Default.Add, "Add to playlist", button, glyph, enabled = current != null) { adding = true }
                        ToolbarSeparator()
                    }
                    ToolbarButton(Icons.Default.SkipPrevious, "Previous track", button, glyph, onClick = state::previous)
                    if (playback.status == PlaybackStatus.RESOLVING) {
                        // Pressing it gives up on a song that is taking too long, as every bar's play button does.
                        ToolbarButton(
                            Icons.Default.PlayArrow,
                            "Stop looking for this song",
                            button,
                            glyph,
                            picture = { CircularProgressIndicator(Modifier.size(glyph * .8f), strokeWidth = 2.dp, color = LocalContentColor.current) },
                            onClick = state::togglePlayback,
                        )
                    } else {
                        ToolbarButton(
                            Icons.Default.PlayArrow,
                            "Play",
                            button,
                            glyph,
                            enabled = current != null,
                            lit = playback.status == PlaybackStatus.PLAYING,
                        ) { if (playback.status != PlaybackStatus.PLAYING) state.togglePlayback() }
                    }
                    // Pause again carries on, as a desktop player's pause button did.
                    ToolbarButton(
                        Icons.Default.Pause,
                        "Pause",
                        button,
                        glyph,
                        enabled = loaded,
                        lit = playback.status == PlaybackStatus.PAUSED,
                        onClick = state::togglePlayback,
                    )
                    if (PlayerButton.STOP in shown) {
                        ToolbarButton(Icons.Default.Stop, "Stop", button, glyph, enabled = current != null, onClick = state::stop)
                    }
                    // Lit while next leads somewhere, autoplay's lined-up songs included: see QueueState.hasNext.
                    ToolbarButton(Icons.Default.SkipNext, "Next track", button, glyph, enabled = queue.hasNext, onClick = state::next)
                    ToolbarSeparator()
                    if (!cramped) {
                        ToolbarSong(current, playback, state, words = !tight, Modifier.width(if (tight) 34.dp else if (narrow) 170.dp else 230.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    PlaybackProgressBar(
                        playback = playback,
                        onSeek = state::seekTo,
                        modifier = Modifier.weight(1f),
                        style = preferences.progressBarStyle,
                        timeDisplay = preferences.timeDisplay,
                        compact = skinned,
                        timesAfter = true,
                    )
                    ToolbarSeparator()
                    if (PlayerButton.REPEAT in shown) {
                        ToolbarButton(
                            if (queue.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            when (queue.repeatMode) {
                                RepeatMode.OFF -> "Repeat off"
                                RepeatMode.ALL -> "Repeat all"
                                RepeatMode.ONE -> "Repeat one"
                            },
                            button,
                            glyph,
                            lit = queue.repeatMode != RepeatMode.OFF,
                            onClick = state::cycleRepeat,
                        )
                    }
                    if (PlayerButton.SHUFFLE in shown) {
                        ToolbarButton(Icons.Default.Shuffle, "Shuffle", button, glyph, lit = queue.shuffleEnabled, onClick = state::toggleShuffle)
                    }
                    // What is under way stays on any bar: a sleep timer counting down, the music playing elsewhere, a speed.
                    if (PlayerButton.SLEEP_TIMER in shown) SleepTimerButton(state, size = button)
                    if (PlayerButton.DEVICES in shown) ConnectButton(state, Modifier.size(button))
                    if (PlayerButton.SPEED in shown) SpeedButton(playback, state, size = button)
                    if (PlayerButton.VOLUME in shown) VolumeControl(playback, state, size = button)
                }
            }
            if (atTop && !inGlass) HorizontalDivider(color = rule)
        }
    }
}

/**
 * One of the toolbar's buttons. Flat, with its picture [lit] in the accent while what it does is in force; under a
 * Windows skin a toolbar button of that desktop, held in while lit, as a toggle on a toolbar was.
 */
@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    glyph: Dp,
    enabled: Boolean = true,
    lit: Boolean = false,
    picture: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    if (skin() != ThemeSkin.STANDARD) {
        ToolButton(onClick, Modifier.size(size), enabled = enabled, latched = lit) {
            picture?.invoke() ?: Icon(icon, description, Modifier.size(glyph))
        }
        return
    }
    IconButton(onClick, Modifier.size(size), enabled = enabled) {
        picture?.invoke() ?: Icon(icon, description, Modifier.size(glyph), tint = if (lit) MaterialTheme.colorScheme.primary else LocalContentColor.current)
    }
}

/** The line between two groups of the toolbar's buttons: 98's etched ridge, XP's pair of lines, a faint rule otherwise. */
@Composable
internal fun ToolbarSeparator() {
    when (skin()) {
        ThemeSkin.WINDOWS_98 -> Box(Modifier.padding(horizontal = 4.dp).width(2.dp).height(22.dp).drawBehind { etchedUpright(0f, 0f, size.height) })
        ThemeSkin.WINDOWS_XP -> Box(
            Modifier.padding(horizontal = 4.dp).width(2.dp).height(22.dp).drawBehind {
                drawRect(Luna.TaskbarFoot, Offset.Zero, Size(pixel, size.height))
                drawRect(Luna.TaskbarTop.towards(Color.White, .35f), Offset(pixel, 0f), Size(pixel, size.height))
            },
        )
        ThemeSkin.STANDARD -> Box(Modifier.padding(horizontal = 7.dp).width(1.dp).height(22.dp).background(ink(.12f)))
    }
}

/**
 * The song on the toolbar: a small cover and its title over its artist, opening now playing, and the song's menu on
 * a right click. With no room for [words], the cover alone.
 */
@Composable
private fun ToolbarSong(current: Track?, playback: PlaybackState, state: AppState, words: Boolean, modifier: Modifier) {
    val skinned = skin() != ThemeSkin.STANDARD
    val side = if (skinned) 24.dp else 32.dp
    SongMenuArea(current, state, modifier) {
        Row(
            Modifier.clickable(enabled = current != null) { state.navigate(Destination.NOW_PLAYING) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = skinShape(RoundedCornerShape(6.dp))
            if (current != null) {
                RemoteArtwork(current.artworkUrl, current.provider, Modifier.size(side).clip(shape))
            } else {
                Box(Modifier.size(side).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(side * .55f))
                }
            }
            if (words) {
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        current?.title ?: "Nothing playing",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (skinned) 11.sp else 12.sp,
                        lineHeight = if (skinned) 13.sp else 15.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            playback.errorMessage ?: current?.artistLine ?: "Choose a track to start",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (playback.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = if (skinned) 10.sp else 11.sp,
                            lineHeight = if (skinned) 12.sp else 14.sp,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        // Spotify's own app is the one playing, as every bar says.
                        if (playsOnSpotify(current, state.settings.collectAsState().value.spotify)) {
                            Spacer(Modifier.width(6.dp))
                            OnSpotifyMark()
                        }
                    }
                }
            }
        }
    }
}

/** The two times as a desktop player's toolbar writes them after its slider: "0:58 / 2:31". */
@Composable
internal fun TimesAfter(elapsed: String, trailing: String, colour: Color) {
    Text(
        "$elapsed / $trailing",
        color = colour,
        fontSize = 12.sp,
        maxLines = 1,
        modifier = Modifier.padding(start = 10.dp, end = 2.dp).widthIn(min = 72.dp),
    )
}

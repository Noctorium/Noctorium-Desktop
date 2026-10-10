package app.noctorium.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.VolumeBoost
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.DropdownMenu
import app.noctorium.ui.skins.FilterChip
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.Slider
import app.noctorium.ui.skins.Text
import kotlin.math.roundToInt

/*
 * The volume, its mute and the booster, in the one popover every player bar opens from its speaker.
 *
 * The booster goes from 100% -- off, the sound as it came -- to 1000%, ten times the amplitude, always behind the
 * player's limiter so it gets loud rather than broken (see VolumeBoost in the core). It belongs to this computer's
 * player: where the song is made somewhere Noctorium cannot reach -- Spotify in its own app -- it says so and
 * rests, rather than moving a slider that does nothing.
 */

/**
 * The level the booster is set to, as chosen: the settings', not the player's, so it is still right while Spotify
 * plays in its own app and the player has no booster to report -- and right after a restart, which keeps it.
 */
@Composable
internal fun chosenBoost(state: AppState): Int = VolumeBoost.clamp(state.settings.collectAsState().value.preferences.volumeBoostPercent)

/** Whether the booster can reach the song playing: not in Spotify's own app, nor where the player says it cannot. */
internal fun boostAvailable(playback: PlaybackState, onSpotify: Boolean): Boolean = !onSpotify && playback.volumeBoostAvailable

/**
 * The speaker, and the popover behind it: the volume, mute, and the booster.
 *
 * The speaker is lit while muted or boosted, and while boosted it wears the boost -- "×4" -- as the queue's button
 * wears its count, so a boost left on is never forgotten: a restart keeps it.
 *
 * The volume and mute reach Spotify's own app when that is what is playing, and Spotify turns its device up and
 * down; the boost belongs to this computer's player, and rests until a song plays here again.
 */
@Composable
internal fun VolumeControl(
    playback: PlaybackState,
    state: AppState,
    size: Dp = 36.dp,
    /** Told when the popover opens and closes, for a bar that must stay open while it is: the Island. */
    onOpenChange: (Boolean) -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    val reportOpen by rememberUpdatedState(onOpenChange)
    LaunchedEffect(open) { reportOpen(open) }
    // Gone while open still counts as closed, or whatever was waiting for it would wait for ever.
    DisposableEffect(Unit) { onDispose { reportOpen(false) } }
    val onSpotify = playsOnSpotify(playback.track, state.settings.collectAsState().value.spotify)
    val boost = chosenBoost(state)
    val boosted = VolumeBoost.isOn(boost)
    Box {
        BadgedBox(
            badge = {
                if (boosted) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        Text(VolumeBoost.timesLabel(boost), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            },
        ) {
            IconButton({ open = true }, Modifier.size(size)) {
                Icon(
                    if (playback.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    if (boosted) "Volume, boosted to ${VolumeBoost.percentLabel(boost)}" else "Volume",
                    tint = if (playback.isMuted || boosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(open, { open = false }) {
            VolumePopover(playback, state, onSpotify)
        }
    }
}

/** What the speaker opens: the volume, mute and the boost button, and the booster's slider while it is on. */
@Composable
internal fun VolumePopover(playback: PlaybackState, state: AppState, onSpotify: Boolean) {
    val boost = chosenBoost(state)
    val boosted = VolumeBoost.isOn(boost)
    val available = boostAvailable(playback, onSpotify)
    Column(Modifier.width(284.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volume", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${(playback.volume * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Straight through. The slider used to stretch to ten times unity when the boost was on, which was only
        // ever a way of asking mpv to clip harder; the boost is a filter now, with a slider of its own below.
        Slider(
            value = playback.volume.coerceIn(0f, 1f),
            onValueChange = state::setVolume,
            valueRange = 0f..1f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = playback.isMuted,
                onClick = state::toggleMute,
                label = { Text(if (playback.isMuted) "Muted" else "Mute", fontSize = 11.sp) },
                modifier = Modifier.height(30.dp),
            )
            FilterChip(
                selected = boosted,
                onClick = state::toggleVolumeBoost,
                label = { Text(if (boosted) "Boost ${VolumeBoost.timesLabel(boost)}" else "Boost", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Bolt, null, Modifier.size(14.dp)) },
                enabled = available,
                modifier = Modifier.height(30.dp),
            )
        }
        val moving = LocalMotion.current
        AnimatedVisibility(
            boosted && available,
            enter = if (moving) expandVertically(tween(MotionTiming.STANDARD)) + fadeIn(tween(MotionTiming.STANDARD)) else EnterTransition.None,
            exit = if (moving) shrinkVertically(tween(MotionTiming.STANDARD)) + fadeOut(tween(MotionTiming.LEAVING)) else ExitTransition.None,
        ) {
            BoostSlider(boost, state::setVolumeBoostPercent)
        }
        if (onSpotify) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Spotify plays this song in its own app, so the boost and the equaliser wait for the next " +
                    "song played here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        } else if (!playback.volumeBoostAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(VolumeBoost.UNAVAILABLE, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

/**
 * The booster's own slider, from 100% to 1000% in tens, with the level beside its name and a warning once it is
 * past twice the amplitude. Written when it is let go of: every write is a settings save and a new gain for the
 * player, and a drag is dozens of them.
 */
@Composable
internal fun BoostSlider(boost: Int, commit: (Int) -> Unit) {
    var dragging by remember(boost) { mutableFloatStateOf(boost.toFloat()) }
    val shown = VolumeBoost.snap(dragging)
    Column {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Boost", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${VolumeBoost.percentLabel(shown)} · ${VolumeBoost.timesLabel(shown)}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = dragging,
            onValueChange = { dragging = VolumeBoost.snap(it).toFloat() },
            onValueChangeFinished = { if (shown != boost) commit(shown) },
            valueRange = VolumeBoost.OFF_PERCENT.toFloat()..VolumeBoost.MAX_PERCENT.toFloat(),
        )
        if (VolumeBoost.isLoud(shown)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text(VolumeBoost.LOUD_WARNING, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

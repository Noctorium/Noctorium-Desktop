package app.noctorium.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.Track
import app.noctorium.playback.MAX_SPEED
import app.noctorium.playback.MIN_SPEED
import app.noctorium.playback.QueueState
import app.noctorium.playback.RepeatMode
import app.noctorium.settings.AutoplaySource
import app.noctorium.settings.HoverControls
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.SpotifyConnectionState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/*
 * How listening goes on: the speed, autoplay after the queue, keeping the queue, and the sleep timer's fade --
 * and Up next, where autoplay's songs are shown before they play.
 *
 * The settings and the queue are the core's, shared with the phone. What is here is where the desktop puts
 * them and how it draws them; the decisions are plain functions at the top, to be tested without a window.
 */

// --- Decisions ---

/** A speed as the player says it: "1.25×", "0.5×", "2×". */
internal fun speedLabel(speed: Float): String {
    val hundredths = Math.round(speed * 100)
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    return "${hundredths / 100}" + (if (fraction.isEmpty()) "" else ".$fraction") + "×"
}

/** Where a dragged speed lands: on the twentieths the core keeps, within the range it allows. */
internal fun snapSpeed(speed: Float): Float = Math.round(speed.coerceIn(MIN_SPEED, MAX_SPEED) * 20) / 20f

/** The sleep timer's fades on offer, in seconds: none, then a quarter, a half and a whole minute. */
internal val SLEEP_FADES = listOf(0, 15, 30, 60)

internal fun sleepFadeLabel(seconds: Int): String = when {
    seconds <= 0 -> "Off"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "$seconds s"
}

/** The line under Playback and queue on the Settings list. */
internal fun playbackSummary(preferences: NoctoriumPreferences): String = listOfNotNull(
    if (preferences.autoplay) "Autoplay on" else "Autoplay off",
    if (preferences.playbackSpeed == 1f) "normal speed" else "${speedLabel(preferences.playbackSpeed)} speed",
    "the queue kept".takeIf { preferences.keepQueue },
).joinToString(" · ")

/** What a queue saved as a playlist is called unless it is named otherwise: the day it was saved. */
internal fun queuePlaylistName(day: LocalDate): String =
    "Queue, ${day.dayOfMonth} ${day.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}"

/** How the core says it when Spotify's own autoplay carries on, and Noctorium follows it. */
private const val SPOTIFY_CHOOSES = "Spotify chooses what comes next"

/** What Up next says about autoplay, under the songs that were chosen. */
internal sealed interface AutoplayShown {
    /** Nothing at all: an empty queue has nothing to follow, and one that repeats never runs out. */
    data object Hidden : AutoplayShown

    /** Switched off, so the music stops at the end of the queue; said quietly, with a way to switch it on. */
    data object Off : AutoplayShown

    /** On, with nothing lined up yet, because the queue is not near its end. */
    data object Waiting : AutoplayShown

    /** Spotify's own app carries on after the queue by itself, and is followed. */
    data class SpotifyChooses(val line: String) : AutoplayShown

    /** Songs lined up, and where they come from. */
    data class Lined(val from: String, val songs: List<Track>) : AutoplayShown
}

/**
 * What Up next shows for autoplay, from the queue and the settings.
 *
 * The Spotify case is decided as the core decides it -- the queue ends on a song Spotify's own app plays, and
 * autoplay is to come from the same service -- so it is said from the moment that is so, rather than only once
 * the core has got round to saying it near the end of the queue.
 */
internal fun autoplayShown(
    queue: QueueState,
    autoplay: Boolean,
    from: AutoplaySource,
    spotify: SpotifyConnectionState,
): AutoplayShown = when {
    queue.tracks.isEmpty() || queue.repeatMode != RepeatMode.OFF -> AutoplayShown.Hidden
    !autoplay -> AutoplayShown.Off
    queue.suggestions.isNotEmpty() -> AutoplayShown.Lined(queue.suggestionsFrom ?: "Songs like the last one", queue.suggestions)
    from == AutoplaySource.SAME_SERVICE && playsOnSpotify(queue.tracks.last(), spotify) ->
        AutoplayShown.SpotifyChooses(queue.suggestionsFrom ?: SPOTIFY_CHOOSES)
    else -> AutoplayShown.Waiting
}

// --- The speed and the fade, wherever they are offered ---

/**
 * The speed: a slider moving in twentieths, the figure beside it, and a way back to normal.
 *
 * Written when the slider is let go of, not at every step of a drag, which would be a settings save and a
 * new speed for the player a dozen times over. It rests while Spotify's own app plays the song, since Spotify
 * plays at its own speed; [compact] is the volume popover's narrower version.
 */
@Composable
internal fun SpeedControl(speed: Float, onSpotify: Boolean, compact: Boolean, set: (Float) -> Unit) {
    var dragging by remember(speed) { mutableFloatStateOf(speed) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Speed",
                fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = if (compact) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                speedLabel(dragging),
                color = if (dragging != 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compact) 11.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (speed != 1f) {
                Spacer(Modifier.width(4.dp))
                TextButton({ dragging = 1f; set(1f) }, enabled = !onSpotify) { Text("Normal", fontSize = 11.sp) }
            }
        }
        // No marks along it: thirty of them read as a dotted line. The value still moves in twentieths.
        Slider(
            value = dragging,
            onValueChange = { dragging = snapSpeed(it) },
            onValueChangeFinished = { if (dragging != speed) set(dragging) },
            valueRange = MIN_SPEED..MAX_SPEED,
            enabled = !onSpotify,
        )
        // Its two ends, where there is room to say them, so the slider says how far it goes.
        if (!compact) {
            Row {
                Text(speedLabel(MIN_SPEED), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(speedLabel(MAX_SPEED), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

/** How long the sleep timer's fade is, as four small choices; [choose] is given seconds. */
@Composable
internal fun SleepFadeChoice(seconds: Int, choose: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SLEEP_FADES.forEach { option ->
            FilterChip(
                selected = seconds == option,
                onClick = { choose(option) },
                label = { Text(sleepFadeLabel(option), fontSize = 11.sp) },
                modifier = Modifier.height(30.dp),
            )
        }
    }
}

// --- The Settings page ---

/**
 * Playback and queue: the speed, autoplay, keeping the queue, and the sleep timer's fade.
 *
 * Its own page because none of it is how Noctorium looks, and all of it is how listening goes on. The speed
 * is also in the volume popover and the fade beside the sleep timer, where they are wanted mid-song; this is
 * where they are found by somebody looking for them.
 */
@Composable
internal fun PlaybackQueueSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    val onSpotify = playingOnSpotify(state)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            CardHeading(Icons.Default.Speed, "Speed")
            Spacer(Modifier.height(6.dp))
            Text(
                "Faster or slower without changing the pitch, from half to double. For a talk or a podcast as much as " +
                    "for music.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            SpeedControl(preferences.playbackSpeed, onSpotify, compact = false, set = state::setPlaybackSpeed)
            if (onSpotify) {
                Text(
                    "Spotify plays the song now playing in its own app, at its own speed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.AllInclusive, "Autoplay")
            Spacer(Modifier.height(6.dp))
            Text(
                "When the queue runs out, songs like the last one carry on, shown under the queue before they play.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                "Autoplay",
                if (preferences.autoplay) "On: the music carries on when the queue ends." else "Off: the music stops when the queue ends.",
                preferences.autoplay,
            ) { on -> state.setAutoplay(on) }
            Spacer(Modifier.height(14.dp))
            Text("Where its songs come from", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            AutoplaySource.entries.forEach { source ->
                val selected = preferences.autoplayFrom == source
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = preferences.autoplay) { state.setAutoplayFrom(source) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RadioButton(selected, { state.setAutoplayFrom(source) }, enabled = preferences.autoplay)
                    // With autoplay off the choice waits, name and description alike, as the switch below does.
                    Column(
                        Modifier.weight(1f).padding(top = 11.dp, bottom = 6.dp).alpha(if (preferences.autoplay) 1f else .45f),
                    ) {
                        Text(source.displayName, fontSize = 13.sp)
                        Text(source.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                "Skip songs played lately",
                "Leaves out what you played recently, so the same few do not keep coming round.",
                preferences.autoplayAvoidRecent,
                enabled = preferences.autoplay,
            ) { on -> state.setAutoplayAvoidRecent(on) }
        }

        SettingsPanelCard {
            CardHeading(Icons.AutoMirrored.Filled.QueueMusic, "The queue")
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                "Keep the queue between launches",
                if (preferences.keepQueue) {
                    "Noctorium opens with the queue as you left it, and picks the song up where it was."
                } else {
                    "Off: each launch starts with an empty queue."
                },
                preferences.keepQueue,
            ) { on -> state.setKeepQueue(on) }
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Bedtime, "Sleep timer")
            Spacer(Modifier.height(12.dp))
            ChoiceRow("Fade out", SLEEP_FADES, preferences.sleepFadeSeconds, ::sleepFadeLabel, state::setSleepFade)
            Spacer(Modifier.height(7.dp))
            Text(
                "The music grows quieter over the timer's last moments instead of stopping dead, and the volume is " +
                    "back where it was for the next song. Also in the sleep timer's own menu.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

// --- Up next ---

/**
 * Autoplay's part of Up next, under the queue: the songs it has lined up and where from, or why there are
 * none.
 *
 * Quieter than the queue above it on purpose. These songs were not chosen, and the eye should read the queue
 * as the queue and these as what follows it -- until one is kept, and joins the songs that were. [compact] is
 * the now playing panel's narrower list, whose rows show their buttons when pointed at.
 */
internal fun LazyListScope.autoplaySection(shown: AutoplayShown, state: AppState, compact: Boolean) {
    when (shown) {
        AutoplayShown.Hidden -> Unit
        AutoplayShown.Off -> item(key = "autoplay:off") {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AllInclusive, null, Modifier.size(16.dp), tint = ink(.35f))
                Spacer(Modifier.width(9.dp))
                Text(
                    "Autoplay is off, so the music stops when the queue ends.",
                    color = ink(.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton({ state.setAutoplay(true) }) { Text("Turn on", fontSize = 12.sp) }
            }
        }
        AutoplayShown.Waiting -> item(key = "autoplay:waiting") {
            AutoplayNote("Autoplay lines up more songs when the queue is about to end.")
        }
        is AutoplayShown.SpotifyChooses -> item(key = "autoplay:spotify") {
            Column {
                AutoplayHeading(shown.line, refresh = null)
                AutoplayNote("Spotify's own autoplay carries on after the queue, and each song it plays joins the queue as it starts.")
            }
        }
        is AutoplayShown.Lined -> {
            item(key = "autoplay:heading") { AutoplayHeading(shown.from, refresh = state::refreshSuggestions) }
            itemsIndexed(shown.songs, key = { index, track -> "autoplay:${track.queueKey}:$index" }) { index, track ->
                if (compact) CompactSuggestionRow(track, index, state) else SuggestionRow(track, index, state)
            }
        }
    }
}

@Composable
private fun AutoplayHeading(from: String, refresh: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AllInclusive, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = .75f))
        Spacer(Modifier.width(9.dp))
        Text(
            "Autoplay · $from",
            color = ink(.72f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        refresh?.let {
            IconButton(it, Modifier.size(30.dp)) {
                Icon(Icons.Default.Refresh, "Look for other songs", Modifier.size(16.dp), tint = ink(.55f))
            }
        }
    }
}

@Composable
private fun AutoplayNote(text: String) {
    Text(text, color = ink(.45f), fontSize = 11.sp, modifier = Modifier.padding(start = 31.dp, top = 6.dp, end = 6.dp))
}

/** One of autoplay's songs in the Queue page's list: play it now, keep it in the queue, or leave it out. */
@Composable
private fun SuggestionRow(track: Track, index: Int, state: AppState) {
    Surface(
        onClick = { state.playSuggestion(index) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .12f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AllInclusive, null, Modifier.size(14.dp), tint = ink(.3f))
            }
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)).alpha(.7f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.72f))
                Text(track.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.45f), fontSize = 12.sp)
            }
            Box(Modifier.alpha(.7f)) { ProviderBadge(track.provider, compact = true) }
            SuggestionActions(index, state, size = 34.dp)
        }
    }
}

/** The same in the now playing panel, quieter still, with its buttons shown when the row is pointed at. */
@Composable
private fun CompactSuggestionRow(track: Track, index: Int, state: AppState) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val hovered = isHovered || state.settings.collectAsState().value.preferences.hoverControls == HoverControls.ALWAYS
    Surface(
        onClick = { state.playSuggestion(index) },
        color = if (hovered) ink(.05f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        interactionSource = interaction,
        modifier = Modifier.hoverable(interaction),
    ) {
        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).alpha(.65f))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = ink(.66f))
                Text(track.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.4f), fontSize = 11.sp)
            }
            if (hovered) {
                SuggestionActions(index, state, size = 30.dp)
            } else {
                track.durationMs?.let { Text(formatPlaybackTime(it), color = ink(.35f), fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun SuggestionActions(index: Int, state: AppState, size: androidx.compose.ui.unit.Dp) {
    IconButton({ state.playSuggestion(index) }, Modifier.size(size)) {
        Icon(Icons.Default.PlayArrow, "Play now", Modifier.size(18.dp), tint = ink(.8f))
    }
    IconButton({ state.keepSuggestion(index) }, Modifier.size(size)) {
        Icon(Icons.Default.Add, "Add to the queue", Modifier.size(18.dp), tint = ink(.7f))
    }
    IconButton({ state.removeSuggestion(index) }, Modifier.size(size)) {
        Icon(Icons.Default.Close, "Leave this one out", Modifier.size(16.dp), tint = ink(.55f))
    }
}

/**
 * The queue's own menu: shuffle or clear only what is still to come, and keep the whole queue as a playlist.
 *
 * Kept apart from the shuffle mode and Clear beside it, which change how the whole queue plays and empty all
 * of it; these leave the song playing exactly where it is. [saved] hears the name it was saved under.
 */
@Composable
internal fun QueueActionsMenu(queue: QueueState, state: AppState, saved: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf(false) }
    if (naming) {
        PlaylistNameDialog("Save the queue as a playlist", queuePlaylistName(LocalDate.now()), "Save") { title ->
            naming = false
            title?.let { state.saveQueueAsPlaylist(it)?.let { playlist -> saved(playlist.title) } }
        }
    }
    Box {
        IconButton({ open = true }, Modifier.size(36.dp)) {
            Icon(Icons.Default.MoreVert, "Queue actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(
                text = { Text("Shuffle what's next") },
                leadingIcon = { Icon(Icons.Default.Shuffle, null) },
                enabled = queue.upNext.size >= 2,
                onClick = { state.shuffleUpcoming(); open = false },
            )
            DropdownMenuItem(
                text = { Text("Clear what's next") },
                leadingIcon = { Icon(Icons.Default.ClearAll, null) },
                enabled = queue.upNext.isNotEmpty(),
                onClick = { state.clearUpcoming(); open = false },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Save queue as playlist…") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                enabled = queue.tracks.isNotEmpty(),
                onClick = { open = false; naming = true },
            )
        }
    }
}

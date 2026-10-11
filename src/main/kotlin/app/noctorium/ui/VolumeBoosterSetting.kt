package app.noctorium.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.playback.VolumeBoost
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.ui.skins.Slider
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton

/**
 * The volume booster on the Sound page: the same level the volume button's booster sets, from 100% -- off -- to
 * 1000%, behind a limiter so it gets loud rather than broken. Here so that somebody looking in Settings for a way
 * to make a quiet song louder finds one, and so the settings search has somewhere to take them.
 *
 * Written when the slider is let go of, as every slider in Settings is. Where the sound is out of Noctorium's
 * reach -- a song Spotify plays in its own app -- it says so rather than pretending.
 */
@Composable
internal fun VolumeBoosterCard(preferences: NoctoriumPreferences, state: AppState) {
    val playback by state.playback.collectAsState()
    val level = preferences.volumeBoostPercent
    var dragging by remember(level) { mutableFloatStateOf(level.toFloat()) }
    val shown = VolumeBoost.snap(dragging)
    SettingsPanelCard {
        CardHeading(Icons.AutoMirrored.Filled.VolumeUp, "Volume booster")
        Spacer(Modifier.height(6.dp))
        Text(
            "Louder than full volume, up to ten times, for a song mastered quietly or a laptop's small speakers. " +
                "A limiter keeps the loudest moments from breaking up. 100% is off.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = dragging,
                onValueChange = { dragging = VolumeBoost.snap(it).toFloat() },
                onValueChangeFinished = { if (shown != level) state.setVolumeBoostPercent(shown) },
                valueRange = VolumeBoost.OFF_PERCENT.toFloat()..VolumeBoost.MAX_PERCENT.toFloat(),
                enabled = playback.volumeBoostAvailable,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (VolumeBoost.isOn(shown)) "${VolumeBoost.percentLabel(shown)} · ${VolumeBoost.timesLabel(shown)}" else "Off",
                color = if (VolumeBoost.isOn(shown)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(96.dp),
            )
            if (VolumeBoost.isOn(level)) {
                TextButton({ state.setVolumeBoostPercent(VolumeBoost.OFF_PERCENT) }) { Text("Off", fontSize = 12.sp) }
            }
        }
        when {
            !playback.volumeBoostAvailable -> Text(VolumeBoost.UNAVAILABLE, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            VolumeBoost.isLoud(shown) -> Text(VolumeBoost.LOUD_WARNING, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }
    }
}

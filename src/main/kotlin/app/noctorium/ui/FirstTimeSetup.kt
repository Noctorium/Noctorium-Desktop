package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.settings.SetupStep

/**
 * The first-time setup, in place of the window's pages until it is finished or skipped: a few steps of the choices
 * most people make first -- a theme, where searches go, autoplay, signing in -- each with Back, Next and "Skip setup".
 * What the steps are, and what they say, is the core's ([SetupStep]); every Noctorium asks the same.
 */
/** Where the setup starts: the first step, except in the pictures of each step the render check takes. */
internal var setupStartsAt: SetupStep = SetupStep.WELCOME

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstTimeSetup(state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val ui by state.ui.collectAsState()
    var step by rememberSaveable { mutableStateOf(setupStartsAt) }
    val scheme = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(scheme.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Where it is in the steps, as dots, and the way out of all of them.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SetupStep.entries.forEach { s ->
                        Box(
                            Modifier.size(if (s == step) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                                .background(if (s.ordinal <= step.ordinal) scheme.primary else scheme.onSurface.copy(alpha = .18f)),
                        )
                    }
                }
                if (step != SetupStep.DONE) TextButton(state::finishSetup) { Text("Skip setup") }
            }
            Spacer(Modifier.height(36.dp))
            Text(SetupStep.progress(step).uppercase(), color = scheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(8.dp))
            Text(step.title, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(step.about, color = scheme.onSurfaceVariant, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
            Spacer(Modifier.height(28.dp))

            when (step) {
                SetupStep.WELCOME, SetupStep.DONE -> Unit
                SetupStep.LOOK -> FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SetupStep.themes.forEach { theme ->
                        val colours = theme.colours ?: return@forEach
                        val chosen = preferences.theme == theme
                        Column(
                            Modifier.width(128.dp).clip(RoundedCornerShape(14.dp))
                                .border(BorderStroke(if (chosen) 2.dp else 1.dp, if (chosen) scheme.primary else scheme.onSurface.copy(alpha = .14f)), RoundedCornerShape(14.dp))
                                .clickable { state.setTheme(theme) }
                                .padding(10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(9.dp)).background(Color(colours.background))) {
                                Box(Modifier.weight(1f).fillMaxSize().background(Color(colours.panel)))
                                Box(Modifier.weight(1f).fillMaxSize().background(Color(colours.card)))
                                Box(Modifier.weight(1f).fillMaxSize().background(Color(colours.accent)))
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(theme.displayName, fontSize = 13.sp, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                SetupStep.SEARCH -> FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SetupStep.searchModes.forEach { mode ->
                        FilterChip(
                            selected = ui.searchMode == mode,
                            onClick = { state.setSearchMode(mode) },
                            label = { Text(if (mode == app.noctorium.core.SearchMode.HYBRID) "Every service at once" else mode.displayName) },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
                SetupStep.LISTENING -> Surface(shape = RoundedCornerShape(14.dp), color = scheme.surfaceVariant.copy(alpha = .5f)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Autoplay", fontWeight = FontWeight.SemiBold)
                            Text("Songs like the last one, when the queue runs out", color = scheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Switch(preferences.autoplay, state::setAutoplay)
                    }
                }
                SetupStep.ACCOUNTS -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton({
                        state.finishSetup()
                        openSettingsPage(state, SettingsPage.ACCOUNT)
                    }) { Text("Sign in now — YouTube Music, SoundCloud, Spotify and more") }
                    Spacer(Modifier.height(6.dp))
                    Text("Opens Settings › Accounts; the setup is done either way.", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(36.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                step.previous?.let { back -> TextButton({ step = back }) { Text("Back") } }
                Spacer(Modifier.weight(1f))
                val next = step.next
                Button({ if (next == null) state.finishSetup() else step = next }) {
                    Text(
                        when (step) {
                            SetupStep.WELCOME -> "Get started"
                            SetupStep.DONE -> "Start listening"
                            else -> "Next"
                        },
                    )
                }
            }
        }
    }
}

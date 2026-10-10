package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.settings.HomePlay
import app.noctorium.settings.IconSet
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.QueueColumn
import app.noctorium.settings.VisualiserStyle
import app.noctorium.ui.icons.DESKTOP_ICON_SETS
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.icons.LocalIconSet
import app.noctorium.ui.icons.onDesktop
import app.noctorium.ui.skins.PreviewedOption
import app.noctorium.ui.skins.RadioButton
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.skinShape
import app.noctorium.ui.skins.skinned

/*
 * The settings that came with the desktop-music-player look: the icon set, and the Playlist layout's visualiser, its
 * table's columns and its status bar. Each is one composable here, called from one line on the page it belongs to.
 */

// --- Icons ---

/**
 * The icons each set is shown by in its picker: some of the player's own buttons, and some from the sidebar and the
 * pages, chosen as the ones whose shapes differ most between the sets -- a play button and a skip look much the same
 * in every one of Material's styles, a house and a gear do not.
 */
private val ICON_SAMPLES: List<ImageVector> = listOf(
    Icons.Default.PlayCircle,
    Icons.Default.SkipNext,
    Icons.Default.Shuffle,
    Icons.AutoMirrored.Filled.VolumeUp,
    Icons.Default.Home,
    Icons.Default.Settings,
    Icons.Default.LibraryMusic,
    Icons.Default.Album,
    Icons.Default.Lyrics,
    Icons.AutoMirrored.Filled.QueueMusic,
)

/** The icon set: each one as a row of the buttons it draws, so the choice is made by looking at them. */
@Composable
internal fun IconSetCard(preferences: NoctoriumPreferences, state: AppState) {
    val chosen = preferences.iconSet.onDesktop()
    SettingsPanelCard {
        CardHeading(Icons.Default.Category, "Icons")
        Spacer(Modifier.height(6.dp))
        Text(
            "How every button, menu and page draws its icons. Standard is Noctorium's own; Line is thin strokes in " +
                "one colour, the way a desktop's own icon theme draws a music player.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        IconSetPicker(chosen, state::setIconSet)
    }
}

/** The sets the desktop offers, each a tile with its name, its icons and what it is. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IconSetPicker(selected: IconSet, choose: (IconSet) -> Unit) {
    // An option button with the icons in a sample well beneath, under a Windows skin.
    if (skinned()) {
        Column {
            DESKTOP_ICON_SETS.forEach { set ->
                PreviewedOption(set.displayName, set.description, set == selected, { choose(set) }) { IconSample(set, 16) }
            }
        }
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DESKTOP_ICON_SETS.forEach { set ->
            val active = set == selected
            Surface(
                onClick = { choose(set) },
                shape = skinShape(RoundedCornerShape(12.dp)),
                color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else ink(.04f),
                border = BorderStroke(if (active) 2.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else ink(.1f)),
                modifier = Modifier.width(214.dp).fillMaxRowHeight(),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        set.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    IconSample(set, 18)
                    Spacer(Modifier.height(8.dp))
                    Text(set.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 13.sp)
                }
            }
        }
    }
}

/** A row of the sample icons as [set] draws them, whatever set the rest of the window is in. */
@Composable
private fun IconSample(set: IconSet, size: Int) {
    CompositionLocalProvider(LocalIconSet provides set) {
        Row(horizontalArrangement = Arrangement.spacedBy(if (size > 16) 4.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
            ICON_SAMPLES.take(if (size > 16) 8 else 10).forEach { icon ->
                Icon(icon, null, Modifier.size(size.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// --- The Playlist layout ---

/**
 * The Playlist layout's own choices: the visualiser beside the title, the table's columns and their order, and the
 * status bar along the foot.
 */
@Composable
internal fun PlaylistLayoutCard(preferences: NoctoriumPreferences, state: AppState) {
    val columns = QueueColumn.normalised(preferences.queueColumns)
    SettingsPanelCard {
        CardHeading(Icons.Default.TableRows, "Playlist layout")
        Spacer(Modifier.height(6.dp))
        Text(
            "For now playing's Playlist layout: the queue as a table, an info bar with a visualiser, and a status bar.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(15.dp))
        ChoiceRow("Visualiser", VisualiserStyle.entries, preferences.visualiser, { it.displayName }, state::setVisualiser)
        Spacer(Modifier.height(6.dp))
        Text(
            preferences.visualiser.description + " Drawn from the sound itself, so where the player cannot hear it — " +
                "a song Spotify plays in its own app — there is none, and nothing is measured while it is off screen.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        ToggleChips(
            "Columns",
            QueueColumn.entries,
            on = { it in columns },
            name = { it.displayName },
            mark = { column -> (columns.indexOf(column) + 1).takeIf { it > 0 }?.toString() },
        ) { column, on -> state.setQueueColumns(queueColumnsToggled(columns, column, on)) }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Numbered in their order, left to right. Switch one off and on again to send it to the end; the title " +
                    "always stays. A narrow window gives up the service, the album and the number first.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton({ state.setQueueColumns(QueueColumn.DEFAULT) }, enabled = columns != QueueColumn.DEFAULT) {
                Text("As they were", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow(
            "Status bar",
            "Along the foot: what is playing, \"Opus, stereo, 48 kHz, 128 kbit/s\", and how far into the whole queue.",
            preferences.statusBar,
            change = state::setStatusBar,
        )
    }
}

/**
 * The columns with [column] switched [on] -- added at the end -- or off. The title cannot be switched off: a table
 * of songs without their names is not one, and taking it out would only put it back first.
 */
internal fun queueColumnsToggled(columns: List<QueueColumn>, column: QueueColumn, on: Boolean): List<QueueColumn> = when {
    column == QueueColumn.TITLE -> columns
    on -> QueueColumn.normalised(columns - column + column)
    else -> QueueColumn.normalised(columns - column)
}

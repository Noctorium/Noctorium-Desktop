package app.noctorium.ui.skins

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Settings under a Windows skin, which is to say a property sheet: options gathered in group boxes on the
 * window's face, on and off as checkboxes with their words beside them, a choice of one as a row of option
 * buttons -- and the list of pages a Control Panel, each a picture and a name in a white pane.
 */

/** A card of settings as a group box. */
@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    GroupBox(Modifier.widthIn(max = 720.dp).fillMaxWidth(), content = content)
}

/** A choice of one as a labelled row of option buttons, wrapping onto more rows where it runs out of room. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> OptionButtons(label: String, options: List<T>, selected: T, name: (T) -> String, enabled: Boolean, choose: (T) -> Unit) {
    Column {
        Text("$label:", fontSize = 12.sp)
        Spacer(Modifier.height(5.dp))
        FlowRow(
            Modifier.padding(start = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option -> RadioRow(name(option), option == selected, enabled, { choose(option) }) }
        }
    }
}

/**
 * Several things each on or off, as a labelled run of checkboxes, each with its own picture where it has one --
 * and its place in the order after its name, where the order is the point.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> CheckBoxes(
    label: String?,
    options: List<T>,
    on: (T) -> Boolean,
    name: (T) -> String,
    icon: ((T) -> ImageVector)?,
    mark: ((T) -> String?)?,
    toggle: (T, Boolean) -> Unit,
) {
    val skin = skin()
    Column {
        if (label != null) {
            Text("$label:", fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
        }
        FlowRow(
            Modifier.padding(start = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val checked = on(option)
                val source = remember(option) { MutableInteractionSource() }
                Row(
                    Modifier
                        .hoverable(source)
                        .toggleable(checked, source, indication = null, role = Role.Checkbox) { toggle(option, it) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.Canvas(Modifier.size(17.dp)) { checkBox(skin, checked, true, false, false) }
                    Spacer(Modifier.width(4.dp))
                    if (icon != null) {
                        Icon(icon(option), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(4.dp))
                    }
                    val place = if (checked) mark?.invoke(option) else null
                    Text(if (place != null) "${name(option)} ($place)" else name(option), fontSize = 12.sp)
                }
            }
        }
    }
}

/** An option button with a line under its name saying what choosing it means, greyed when it cannot be chosen. */
@Composable
internal fun OptionWithNote(label: String, note: String, selected: Boolean, enabled: Boolean, choose: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        RadioRow(label, selected, enabled, choose)
        Text(note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 21.dp))
    }
}

/**
 * A choice with a picture of itself: its option button and name, what it is in grey, and the thing itself in a
 * sample well beneath, the way Display Properties showed the scheme it was about to apply.
 */
@Composable
internal fun PreviewedOption(name: String, description: String, selected: Boolean, choose: () -> Unit, preview: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        RadioRow(name, selected, true, choose)
        Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 21.dp))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.padding(start = 21.dp).fillMaxWidth().listWell().padding(horizontal = 10.dp, vertical = 2.dp)) { preview() }
    }
}

/**
 * One of the pages of Settings, as an item in a Control Panel: its picture, its name, and what it holds in grey
 * beneath; a tick where it is switched on or signed in.
 */
@Composable
internal fun ControlPanelItem(title: String, subtitle: String, icon: ImageVector, action: () -> Unit, active: Boolean) {
    ListRow(action, shape = RectangleShape, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (active) Icon(Icons.Default.CheckCircle, "Active", Modifier.size(16.dp).alpha(.85f), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

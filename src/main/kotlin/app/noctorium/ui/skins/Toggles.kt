package app.noctorium.ui.skins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.settings.ThemeSkin
import kotlin.math.floor

/*
 * Checkboxes and radio buttons.
 *
 * Neither desktop had a switch -- an on and off thing was a checkbox, with its words beside it -- so Material's
 * switch becomes one here too. Both are pixel art under 98, the white well and the black tick of the bitmaps
 * they were, and under XP the white square with its blue edge and the green tick, lit orange when pointed at.
 */

/**
 * XP's checkbox and radio edge, and its trackbar pointer's: the dark blue of a button's edge, a little paler,
 * which is the blue-grey those small controls were drawn in.
 */
internal val LunaToggleEdge = Luna.ButtonEdge.towards(Luna.FieldEdge, .2f)

/** The tick of a chosen XP checkbox, in the green of its start button. */
private val LunaTick = Luna.Start

/** A checkbox's square, [checked] or not, drawn into whatever room it is given. */
internal fun DrawScope.checkBox(skin: ThemeSkin, checked: Boolean, enabled: Boolean, hovered: Boolean, pressed: Boolean) {
    val p = pixel
    val side = 13 * p
    val left = floor((size.width - side) / 2f / p) * p
    val top = floor((size.height - side) / 2f / p) * p
    if (skin == ThemeSkin.WINDOWS_XP) {
        val inner = Brush.linearGradient(
            listOf(if (pressed) Luna.ButtonFoot else Luna.Face.towards(Luna.Window, .2f), Luna.Window),
            start = Offset(left, top),
            end = Offset(left + side, top + side),
        )
        drawRect(if (enabled) inner else Brush.linearGradient(listOf(Luna.Face, Luna.Face)), Offset(left, top), Size(side, side))
        if (hovered && enabled) {
            drawRect(Luna.Hot, Offset(left + p, top + p), Size(side - p * 2, side - p * 2), style = Stroke(p * 2))
        }
        drawRect(if (enabled) LunaToggleEdge else Luna.GreyText, Offset(left + p / 2, top + p / 2), Size(side - p, side - p), style = Stroke(p))
        if (checked) pixelArt(Art.tick, mapOf('#' to if (enabled) LunaTick else Luna.GreyText), left + p * 3, top + p * 3)
        return
    }
    drawRect(if (enabled && !pressed) Classic.Window else Classic.Face, Offset(left, top), Size(side, side))
    bevel(Bevel.Sunken, left, top, left + side, top + side)
    if (checked) pixelArt(Art.tick, mapOf('#' to if (enabled) Classic.Text else Classic.GreyText), left + p * 3, top + p * 3)
}

/** A radio button's circle, [selected] or not. */
internal fun DrawScope.radioCircle(skin: ThemeSkin, selected: Boolean, enabled: Boolean, hovered: Boolean, pressed: Boolean) {
    val p = pixel
    if (skin == ThemeSkin.WINDOWS_XP) {
        val diameter = 13 * p
        val left = floor((size.width - diameter) / 2f / p) * p
        val top = floor((size.height - diameter) / 2f / p) * p
        val centre = Offset(left + diameter / 2f, top + diameter / 2f)
        drawCircle(
            Brush.linearGradient(
                listOf(if (pressed) Luna.ButtonFoot else Luna.Face.towards(Luna.Window, .2f), Luna.Window),
                start = Offset(left, top),
                end = Offset(left + diameter, top + diameter),
            ),
            radius = diameter / 2f,
            center = centre,
        )
        if (hovered && enabled) drawCircle(Luna.Hot, radius = diameter / 2f - p * 1.5f, center = centre, style = Stroke(p * 2))
        drawCircle(if (enabled) LunaToggleEdge else Luna.GreyText, radius = diameter / 2f - p / 2, center = centre, style = Stroke(p))
        if (selected) {
            drawCircle(
                Brush.radialGradient(listOf(Luna.ProgressLight, if (enabled) LunaTick else Luna.GreyText), center = centre - Offset(p, p), radius = p * 3.5f),
                radius = p * 2.5f,
                center = centre,
            )
        }
        return
    }
    val left = floor((size.width - 12 * p) / 2f / p) * p
    val top = floor((size.height - 12 * p) / 2f / p) * p
    val well = if (enabled && !pressed) Classic.Window else Classic.Face
    pixelArt(Art.radio, Art.bevelPalette + ('w' to well), left, top)
    if (selected) pixelArt(Art.radioDot, mapOf('#' to if (enabled) Classic.Text else Classic.GreyText), left + p * 4, top + p * 4)
}

@Composable
private fun ToggleMark(
    modifier: Modifier,
    source: MutableInteractionSource,
    draw: DrawScope.(hovered: Boolean, pressed: Boolean) -> Unit,
) {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    Canvas(modifier.size(17.dp)) { draw(hovered, pressed) }
}

/**
 * A checkbox and its words, the box first, as every option of the time was set out: [title] beside the box and
 * [description] under it in grey. Clicking the words ticks it as well, which they always did.
 */
@Composable
internal fun CheckRow(
    title: String,
    description: String?,
    checked: Boolean,
    enabled: Boolean,
    change: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val skin = skin()
    Row(
        modifier
            .hoverable(source, enabled)
            .toggleable(checked, source, indication = null, enabled = enabled, role = Role.Checkbox, onValueChange = change),
        verticalAlignment = Alignment.Top,
    ) {
        ToggleMark(Modifier, source) { hovered, pressed -> checkBox(skin, checked, enabled, hovered, pressed) }
        Spacer(Modifier.width(5.dp))
        Column(Modifier.padding(top = 1.dp).alpha(if (enabled) 1f else .55f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Normal)
            if (!description.isNullOrBlank()) {
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

/** A radio button and its name, for a choice of one among several. */
@Composable
internal fun RadioRow(label: String, selected: Boolean, enabled: Boolean, choose: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val skin = skin()
    Row(
        modifier
            .hoverable(source, enabled)
            .selectable(selected, source, indication = null, enabled = enabled, role = Role.RadioButton, onClick = choose),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleMark(Modifier, source) { hovered, pressed -> radioCircle(skin, selected, enabled, hovered, pressed) }
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Material's toggles, as the skin draws them ---

/** Material's checkbox; the skin's own under a Windows skin. */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.Checkbox(checked, onCheckedChange, modifier, enabled, colors, interactionSource)
        return
    }
    // Material's footprint, so the rows laid out round its checkbox still line up with this one.
    SkinCheckbox(skin, checked, onCheckedChange, modifier.minimumInteractiveComponentSize(), enabled, interactionSource)
}

/** The skin's checkbox, in as much room as [modifier] gives it. */
@Composable
private fun SkinCheckbox(
    skin: ThemeSkin,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val toggle = if (onCheckedChange != null) {
        Modifier.hoverable(source, enabled).toggleable(checked, source, indication = null, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
    } else {
        Modifier
    }
    Box(modifier.then(toggle), contentAlignment = Alignment.Center) {
        ToggleMark(Modifier, source) { hovered, pressed -> checkBox(skin, checked, enabled, hovered, pressed) }
    }
}

/**
 * Material's switch. Neither desktop had one -- on and off was a checkbox -- so under a Windows skin this is a
 * checkbox, standing where the switch stood: in a little room of its own rather than Material's footprint, so the
 * words beside it sit close, as a checkbox's did.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.Switch(checked, onCheckedChange, modifier, thumbContent, enabled, colors, interactionSource)
        return
    }
    SkinCheckbox(skin, checked, onCheckedChange, modifier.size(19.dp), enabled, interactionSource)
}

/** Material's radio button; the skin's own under a Windows skin. */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.RadioButton(selected, onClick, modifier, enabled, colors, interactionSource)
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val select = if (onClick != null) {
        Modifier.hoverable(source, enabled).selectable(selected, source, indication = null, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier
    }
    Box(modifier.minimumInteractiveComponentSize().then(select), contentAlignment = Alignment.Center) {
        ToggleMark(Modifier, source) { hovered, pressed -> radioCircle(skin, selected, enabled, hovered, pressed) }
    }
}

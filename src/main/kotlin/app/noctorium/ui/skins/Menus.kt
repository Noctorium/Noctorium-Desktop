package app.noctorium.ui.skins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.noctorium.settings.ThemeSkin

/*
 * Menus.
 *
 * 98's was the window face in a window's raised frame, its items highlighted navy with white writing and its
 * separators etched; XP's was white in a thin grey edge with a soft shadow, highlighted in Luna's blue. Both
 * appeared at once under what opened them -- no growing out of the button -- and a menu that would run off the
 * bottom of the window opened upwards instead.
 */

/** Whether what is being drawn is inside a menu, where a divider is a menu's separator. */
private val LocalInMenu = staticCompositionLocalOf { false }

/** Under the thing that opened it, or above it when there is no room below; never off either side. */
private class MenuPosition(private val offset: DpOffset, private val density: Density) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val dx = with(density) { offset.x.roundToPx() }
        val dy = with(density) { offset.y.roundToPx() }
        val x = (anchorBounds.left + dx).coerceAtMost(windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val below = anchorBounds.bottom + dy
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - dy - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

/** A menu's own frame round [content], in the skin's way. */
@Composable
internal fun MenuFrame(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val skin = skin()
    val framed = if (skin == ThemeSkin.WINDOWS_XP) {
        Modifier
            .shadow(4.dp, RectangleShape, clip = false, ambientColor = Color.Black.copy(alpha = .3f), spotColor = Color.Black.copy(alpha = .4f))
            .drawBehind {
                drawRect(Luna.Window)
                edge(Luna.GreyText, Luna.GreyText)
            }
            .padding(2.dp)
    } else {
        Modifier
            .drawBehind {
                drawRect(Classic.Face)
                bevel(Bevel.Window)
            }
            .padding(3.dp)
    }
    WindowColours {
        CompositionLocalProvider(LocalInMenu provides true) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Column(framed.then(modifier), content = content)
            }
        }
    }
}

/** Material's dropdown menu; the skin's own under a Windows skin. */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.DropdownMenu(
            expanded, onDismissRequest, modifier, offset, scrollState, properties, shape, containerColor, tonalElevation, shadowElevation, border, content,
        )
        return
    }
    if (!expanded) return
    val density = LocalDensity.current
    val position = remember(offset, density) { MenuPosition(offset, density) }
    Popup(popupPositionProvider = position, onDismissRequest = onDismissRequest, properties = properties) {
        MenuFrame(modifier.width(IntrinsicSize.Max).verticalScroll(scrollState), content = content)
    }
}

/**
 * Material's menu item; under a Windows skin a row of the menu with the picture in the margin where the
 * period kept its ticks, highlighted all the way across while the pointer is on it.
 */
@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.DropdownMenuItem(text, onClick, modifier, leadingIcon, trailingIcon, enabled, colors, contentPadding, interactionSource)
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val lit = hovered && enabled
    Row(
        modifier
            .widthIn(min = 150.dp)
            .fillMaxWidth()
            .height(22.dp)
            .hoverable(source, enabled)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind { if (lit) drawRect(skin.selection) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Highlighted(lit) {
            CompositionLocalProvider(LocalContentColor provides if (lit) LocalContentColor.current else if (enabled) LocalContentColor.current else greyText(skin)) {
                Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                    if (leadingIcon != null) Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { leadingIcon() }
                }
                Box(Modifier.weight(1f, fill = false).padding(end = 12.dp)) { text() }
                Spacer(Modifier.weight(1f))
                if (trailingIcon != null) {
                    Box(Modifier.padding(end = 6.dp).height(16.dp), contentAlignment = Alignment.Center) { trailingIcon() }
                } else {
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

private fun greyText(skin: ThemeSkin): Color = if (skin == ThemeSkin.WINDOWS_XP) Luna.GreyText else Classic.GreyText

/**
 * Material's divider: 98's etched line, a menu's separator inside a menu, and under XP the thin pale line its
 * group boxes were edged with.
 */
@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.HorizontalDivider(modifier, thickness, color)
        return
    }
    val inMenu = LocalInMenu.current
    Canvas(
        modifier
            .then(if (inMenu) Modifier.padding(horizontal = 1.dp, vertical = 3.dp) else Modifier)
            .fillMaxWidth()
            .height(2.dp),
    ) {
        if (skin == ThemeSkin.WINDOWS_XP) {
            drawRect(if (inMenu) Luna.GreyText.towards(Luna.Window, .4f) else Luna.GroupEdge, Offset.Zero, Size(size.width, pixel))
        } else {
            etchedLine(0f, 0f, size.width)
        }
    }
}

/**
 * The tick beside a menu item that is on: the period's small tick under a Windows skin, in the item's writing so
 * it turns white with it while the item is lit, and Material's check under the standard skin. The same room is
 * kept while it is off, so the words of every item in the menu start in line.
 */
@Composable
fun MenuCheck(on: Boolean) {
    val skin = skin()
    if (!skin.isWindows) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (on) androidx.compose.material3.Icon(Icons.Default.Check, null, Modifier.size(20.dp))
        }
        return
    }
    val ink = LocalContentColor.current
    Canvas(Modifier.size(16.dp)) { if (on) pixelArtCentred(Art.tick, mapOf('#' to ink)) }
}

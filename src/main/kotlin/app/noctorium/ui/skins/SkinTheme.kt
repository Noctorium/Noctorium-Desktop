package app.noctorium.ui.skins

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.FontChoice
import app.noctorium.settings.ThemeColours
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.noctoriumColorScheme
import app.noctorium.ui.noctoriumShapes

/**
 * Material's palette for [skin]: the theme's own under the standard skin, and under a Windows one the same
 * worked out from the theme's six colours and then put right where a role is something the desktop had a
 * system colour for -- the face, the window white, the shadow an outline is drawn in.
 *
 * The accent stays the listener's. Under 98 and XP the theme's own accent is the colour of a selection, so
 * choosing another is choosing what 98 called the Selected Items colour, which it let anybody change too.
 */
internal fun skinColorScheme(skin: ThemeSkin, theme: ThemeColours, accent: Color): ColorScheme {
    val base = noctoriumColorScheme(theme, accent)
    val subtext = Color(theme.subtext)
    return when (skin) {
        ThemeSkin.STANDARD -> base
        ThemeSkin.WINDOWS_98 -> base.copy(
            background = Classic.Face,
            onBackground = Classic.Text,
            surface = Classic.Face,
            onSurface = Classic.Text,
            surfaceVariant = Classic.Window,
            onSurfaceVariant = subtext,
            surfaceContainerLowest = Classic.Face,
            surfaceContainerLow = Classic.Face,
            surfaceContainer = Classic.Face,
            surfaceContainerHigh = Classic.Window,
            surfaceContainerHighest = Classic.Window,
            surfaceBright = Classic.Window,
            surfaceDim = Classic.Face,
            primaryContainer = accent.towards(Classic.Window, .8f),
            onPrimaryContainer = accent,
            secondaryContainer = accent.towards(Classic.Window, .86f),
            outline = Classic.Shadow,
            outlineVariant = Classic.Light,
            inverseSurface = Classic.Tooltip,
            inverseOnSurface = Classic.Text,
        )
        ThemeSkin.WINDOWS_XP -> base.copy(
            background = Luna.Face,
            onBackground = Luna.Text,
            surface = Luna.Face,
            onSurface = Luna.Text,
            surfaceVariant = Luna.Window,
            onSurfaceVariant = subtext,
            surfaceContainerLowest = Luna.Face,
            surfaceContainerLow = Luna.Face,
            surfaceContainer = Luna.Face,
            surfaceContainerHigh = Luna.Window,
            surfaceContainerHighest = Luna.Window,
            surfaceBright = Luna.Window,
            surfaceDim = Luna.Face,
            primaryContainer = accent.towards(Luna.Window, .82f),
            onPrimaryContainer = accent,
            secondaryContainer = accent.towards(Luna.Window, .88f),
            outline = Luna.FieldEdge,
            outlineVariant = Luna.GroupEdge,
            inverseSurface = Luna.Tooltip,
            inverseOnSurface = Luna.Text,
        )
    }
}

/**
 * The corners: square under 98, whatever the corner setting says, because nothing on that desktop was round;
 * Luna's few small radii under XP; and the listener's own choice under the standard skin.
 */
internal fun skinShapes(skin: ThemeSkin, corners: CornerStyle): Shapes = when (skin) {
    ThemeSkin.STANDARD -> noctoriumShapes(corners)
    ThemeSkin.WINDOWS_98 -> noctoriumShapes(CornerStyle.SHARP)
    ThemeSkin.WINDOWS_XP -> Shapes(
        extraSmall = RoundedCornerShape(3.dp),
        small = RoundedCornerShape(3.dp),
        medium = RoundedCornerShape(5.dp),
        large = RoundedCornerShape(7.dp),
        extraLarge = RoundedCornerShape(7.dp),
    )
}

/**
 * Material's theme in the skin's colours, corners and type, with the skin itself provided below it.
 *
 * What the window is drawn in, and what a test drawing part of the window should wrap it in. Under the
 * standard skin it is exactly the theme the window always had.
 */
@Composable
fun SkinnedMaterialTheme(
    skin: ThemeSkin,
    theme: ThemeColours,
    accent: Color,
    font: FontChoice,
    corners: CornerStyle,
    content: @Composable () -> Unit,
) {
    val typography = remember(skin, font) { skinTypography(skin, font) }
    MaterialTheme(
        colorScheme = skinColorScheme(skin, theme, accent),
        shapes = skinShapes(skin, corners),
        typography = typography,
    ) {
        if (!skin.isWindows) {
            CompositionLocalProvider(LocalSkin provides skin, content = content)
        } else {
            // Selected writing went navy -- or Luna's blue -- behind white letters. Compose can only tint behind
            // the letters, so the tint is the selection colour at the strength that still lets them be read.
            val selection = if (skin == ThemeSkin.WINDOWS_98) Classic.Selection else Luna.Selection
            CompositionLocalProvider(
                LocalSkin provides skin,
                LocalWindowColours provides MaterialTheme.colorScheme,
                LocalTextSelectionColors provides TextSelectionColors(selection, selection.copy(alpha = .35f)),
                content = content,
            )
        }
    }
}

/**
 * Whether the push buttons below are the window's default -- the one Enter presses -- which 98 drew with an
 * extra black line round it and XP with a blue glow inside its edge. A dialog sets it round its confirming
 * button, as Windows did.
 */
internal val LocalDefaultButton = staticCompositionLocalOf { false }

/**
 * What a control sits on when it is not the plain face: the player bar dressed as a taskbar, which is blue
 * under XP, or a highlighted row; both want white writing and pale highlights from everything on them.
 */
internal enum class Ground { FACE, TASKBAR, SELECTION }

internal val LocalGround = staticCompositionLocalOf { Ground.FACE }

/**
 * Selection colours for whatever is drawn inside, while [on]: the writing, the icons and Material's own colours
 * all turned to the selection's white, so a highlighted row or menu item reads as one piece whatever colours its
 * parts asked for.
 *
 * The same composition whether on or off, only with other values in it: a row lights up the moment the pointer
 * reaches it, and wrapping its contents only then would build them afresh -- dropping a menu that was opening
 * from it, and the press that was opening it.
 */
@Composable
internal fun Highlighted(on: Boolean, content: @Composable () -> Unit) {
    val white = if (skin() == ThemeSkin.WINDOWS_XP) Luna.SelectionText else Classic.SelectionText
    val scheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = if (!on) scheme else scheme.copy(
            onSurface = white,
            onSurfaceVariant = white,
            onBackground = white,
            primary = white,
            tertiary = white,
            onPrimaryContainer = white,
        ),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (on) white else LocalContentColor.current,
            LocalGround provides if (on) Ground.SELECTION else LocalGround.current,
            content = content,
        )
    }
}

/**
 * The colours of the window itself, kept aside from any that a part of it -- the taskbar, a highlighted row --
 * put in their place, so a menu or a dialog opened from that part is drawn on its own face again.
 */
internal val LocalWindowColours = staticCompositionLocalOf<ColorScheme?> { null }

/** Back to the window's own colours, for what opens over a part of it that has colours of its own. */
@Composable
internal fun WindowColours(content: @Composable () -> Unit) {
    val window = LocalWindowColours.current ?: MaterialTheme.colorScheme
    val ink = if (skin() == ThemeSkin.WINDOWS_XP) Luna.Text else Classic.Text
    MaterialTheme(colorScheme = window, shapes = MaterialTheme.shapes, typography = MaterialTheme.typography) {
        CompositionLocalProvider(LocalContentColor provides ink, LocalGround provides Ground.FACE, content = content)
    }
}

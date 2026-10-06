package app.noctorium.ui.skins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.noctorium.settings.ThemeSkin
import app.noctorium.settings.Windows98Colours
import app.noctorium.settings.WindowsXpColours

/*
 * The Windows skins: the 98 and XP themes drawn as those desktops were, rather than as their colours.
 *
 * A palette changes what colour things are; a skin changes what they are. A Windows 98 button is a grey slab
 * with a bevel, not a rounded pill in grey, and nobody who used one would take the second for the first. So
 * the skins draw their own buttons, fields, title bars, menus and scroll bars, from the system colours the core
 * keeps in [Windows98Colours] and [WindowsXpColours], and everything else in the application is told which skin
 * it is in through [LocalSkin] and asks only where it must.
 *
 * Nearly all of it reaches the application through controls with Material's own names, imported in Material's
 * place: a screen that asks for a Button gets a bevelled one under 98 and Material's under every other theme,
 * without a line of it changing. Under the standard skin each of them hands its arguments straight on, so the
 * other themes draw exactly what they drew before the skins existed.
 */

/**
 * The skin in force, provided at the root from the theme.
 *
 * Standard wherever nothing provides one, so a test that draws a single control on its own draws it as it
 * always was.
 */
val LocalSkin = staticCompositionLocalOf { ThemeSkin.STANDARD }

/** The skin in force here. */
@Composable
@ReadOnlyComposable
internal fun skin(): ThemeSkin = LocalSkin.current

/** Whether one of the Windows skins is in force, rather than the plain interface in the theme's colours. */
internal val ThemeSkin.isWindows: Boolean get() = this != ThemeSkin.STANDARD

/** Windows 98's system colours as Compose colours, so the drawing reads like the palette it comes from. */
internal object Classic {
    val Face = Color(Windows98Colours.FACE)
    val Highlight = Color(Windows98Colours.HIGHLIGHT)
    val Light = Color(Windows98Colours.LIGHT)
    val Shadow = Color(Windows98Colours.SHADOW)
    val DarkShadow = Color(Windows98Colours.DARK_SHADOW)
    val Window = Color(Windows98Colours.WINDOW)
    val Text = Color(Windows98Colours.TEXT)
    val GreyText = Color(Windows98Colours.GREY_TEXT)
    val Selection = Color(Windows98Colours.SELECTION)
    val SelectionText = Color(Windows98Colours.SELECTION_TEXT)
    val Title = Color(Windows98Colours.TITLE)
    val TitleEnd = Color(Windows98Colours.TITLE_END)
    val InactiveTitle = Color(Windows98Colours.INACTIVE_TITLE)
    val InactiveTitleEnd = Color(Windows98Colours.INACTIVE_TITLE_END)
    val TitleText = Color(Windows98Colours.TITLE_TEXT)
    val Tooltip = Color(Windows98Colours.TOOLTIP)
    val Desktop = Color(Windows98Colours.DESKTOP)
}

/** Luna's colours, the same way. */
internal object Luna {
    val Face = Color(WindowsXpColours.FACE)
    val Window = Color(WindowsXpColours.WINDOW)
    val Text = Color(WindowsXpColours.TEXT)
    val GreyText = Color(WindowsXpColours.GREY_TEXT)
    val Selection = Color(WindowsXpColours.SELECTION)
    val SelectionText = Color(WindowsXpColours.SELECTION_TEXT)
    val TitleTop = Color(WindowsXpColours.TITLE_TOP)
    val Title = Color(WindowsXpColours.TITLE)
    val TitleLow = Color(WindowsXpColours.TITLE_LOW)
    val TitleFoot = Color(WindowsXpColours.TITLE_FOOT)
    val InactiveTitle = Color(WindowsXpColours.INACTIVE_TITLE)
    val TitleText = Color(WindowsXpColours.TITLE_TEXT)
    val Frame = Color(WindowsXpColours.FRAME)
    val CaptionButton = Color(WindowsXpColours.CAPTION_BUTTON)
    val Close = Color(WindowsXpColours.CLOSE)
    val ButtonEdge = Color(WindowsXpColours.BUTTON_EDGE)
    val ButtonFoot = Color(WindowsXpColours.BUTTON_FOOT)
    val Hot = Color(WindowsXpColours.HOT)
    val Focus = Color(WindowsXpColours.FOCUS)
    val FieldEdge = Color(WindowsXpColours.FIELD_EDGE)
    val GroupEdge = Color(WindowsXpColours.GROUP_EDGE)
    val GroupTitle = Color(WindowsXpColours.GROUP_TITLE)
    val TabEdge = Color(WindowsXpColours.TAB_EDGE)
    val TabChosen = Color(WindowsXpColours.TAB_CHOSEN)
    val ProgressLight = Color(WindowsXpColours.PROGRESS_LIGHT)
    val Progress = Color(WindowsXpColours.PROGRESS)
    val ScrollThumb = Color(WindowsXpColours.SCROLL_THUMB)
    val ScrollEdge = Color(WindowsXpColours.SCROLL_EDGE)
    val ScrollArrow = Color(WindowsXpColours.SCROLL_ARROW)
    val TaskbarTop = Color(WindowsXpColours.TASKBAR_TOP)
    val Taskbar = Color(WindowsXpColours.TASKBAR)
    val TaskbarFoot = Color(WindowsXpColours.TASKBAR_FOOT)
    val Start = Color(WindowsXpColours.START)
    val StartLight = Color(WindowsXpColours.START_LIGHT)
    val Tray = Color(WindowsXpColours.TRAY)
    val TrayEdge = Color(WindowsXpColours.TRAY_EDGE)
    val TaskPaneTop = Color(WindowsXpColours.TASK_PANE_TOP)
    val TaskPaneFoot = Color(WindowsXpColours.TASK_PANE_FOOT)
    val TaskPanel = Color(WindowsXpColours.TASK_PANEL)
    val TaskPanelTitle = Color(WindowsXpColours.TASK_PANEL_TITLE)
    val Tooltip = Color(WindowsXpColours.TOOLTIP)
    val Sky = Color(WindowsXpColours.SKY)
    val SkyLow = Color(WindowsXpColours.SKY_LOW)
    val Hill = Color(WindowsXpColours.HILL)
    val HillShade = Color(WindowsXpColours.HILL_SHADE)
}

/** One colour part of the way to another, for the in-between shades a gradient's stops imply. */
internal fun Color.towards(other: Color, amount: Float): Color = Color(
    red = red + (other.red - red) * amount,
    green = green + (other.green - green) * amount,
    blue = blue + (other.blue - blue) * amount,
    alpha = alpha + (other.alpha - alpha) * amount,
)

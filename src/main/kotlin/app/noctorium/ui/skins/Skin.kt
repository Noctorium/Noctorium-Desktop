package app.noctorium.ui.skins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.noctorium.settings.ThemeSkin
import app.noctorium.settings.Windows98Colours
import app.noctorium.settings.Windows98Palette
import app.noctorium.settings.WindowsXpColours

/*
 * The Windows skins: the 98 and XP themes drawn as those desktops were, rather than as their colours.
 *
 * A palette changes what colour things are; a skin changes what they are. A Windows 98 button is a grey slab
 * with a bevel, not a rounded pill in grey, and nobody who used one would take the second for the first. So
 * the skins draw their own buttons, fields, title bars, menus and scroll bars, from the system colours the core
 * keeps in a [Windows98Palette] -- [Windows98Colours] as 98 shipped them, or Noctorium 98's night -- and in
 * [WindowsXpColours], and everything else in the application is told which skin it is in through [LocalSkin] and
 * asks only where it must.
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

/**
 * The 98 skin's colours as Compose colours, so the drawing reads like the palette it comes from: 98's own grey,
 * or Noctorium 98's night, whichever [Windows98Palette] the theme in force asks for.
 *
 * The palette is held here in snapshot state, set by [SkinnedMaterialTheme], rather than handed down through a
 * CompositionLocal. Most of the skin reads these colours inside draw lambdas, pixel-art maps and default
 * arguments, where no CompositionLocal can be read; snapshot state can be read anywhere, and whatever read it --
 * a composition or a drawing -- is redone when it changes, which is all a switch of theme needs. There is one
 * theme for the whole application, so one palette at a time is all there ever is.
 */
internal object Classic {
    /** The palette in force, which [SkinnedMaterialTheme] sets from the theme: 98's own until it has. */
    var palette: Windows98Palette by mutableStateOf(Windows98Palette.STANDARD)

    val Face: Color get() = Color(palette.face)
    val Highlight: Color get() = Color(palette.highlight)
    val Light: Color get() = Color(palette.light)
    val Shadow: Color get() = Color(palette.shadow)
    val DarkShadow: Color get() = Color(palette.darkShadow)
    val Window: Color get() = Color(palette.window)
    val Text: Color get() = Color(palette.text)
    val GreyText: Color get() = Color(palette.greyText)
    val Selection: Color get() = Color(palette.selection)
    val SelectionText: Color get() = Color(palette.selectionText)
    val Title: Color get() = Color(palette.title)
    val TitleEnd: Color get() = Color(palette.titleEnd)
    val InactiveTitle: Color get() = Color(palette.inactiveTitle)
    val InactiveTitleEnd: Color get() = Color(palette.inactiveTitleEnd)
    val TitleText: Color get() = Color(palette.titleText)
    val Tooltip: Color get() = Color(palette.tooltip)
    val Desktop: Color get() = Color(palette.desktop)

    /*
     * Two roles 98 had no colour of its own for, and filled with one that is only right on a pale face. Each is
     * that colour exactly under 98's own palette, and something that reads under a dark one.
     */

    /**
     * The title of a window not in use. 98 wrote it in the face's grey, paler than the grey bar it sat on; on a
     * dark face that would be darker than the bar and all but gone, so there it is the grey text lifted a little
     * towards the writing -- still quieter than an active title's white.
     */
    val InactiveTitleText: Color get() = if (palette.dark) GreyText.towards(Text, .35f) else Face

    /**
     * Quieter writing on black: the Display bar's display, which under 98 took its bevel's pale grey. A dark
     * palette's bevel light is dark too, so there it is the writing dimmed halfway to the grey text.
     */
    val PaleText: Color get() = if (palette.dark) Text.towards(GreyText, .5f) else Light
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

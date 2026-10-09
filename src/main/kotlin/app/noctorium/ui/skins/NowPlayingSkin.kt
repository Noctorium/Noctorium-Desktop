package app.noctorium.ui.skins

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import app.noctorium.domain.Track
import app.noctorium.settings.ThemeSkin
import app.noctorium.settings.Windows98Palette
import app.noctorium.ui.RemoteArtwork

/*
 * Now playing under a Windows skin: the screen is a desktop, and what is on it stands in windows -- the record
 * in one, the queue and the lyrics in another, behind tabs.
 */

/** Whether a Windows skin is in force, for the few places in the application that draw something else then. */
@Composable
fun skinned(): Boolean = skin().isWindows

/**
 * The record's part of the screen in a window of its own, titled with the song, under a Windows skin; the part
 * as it was under the standard skin. [content] is handed the modifier it should fill.
 */
@Composable
fun HeroWindow(modifier: Modifier, track: Track, content: @Composable (Modifier) -> Unit) {
    if (!skinned()) {
        content(modifier)
        return
    }
    // Named the way Windows named a window: what is open in it, then the program it is open in.
    SkinWindow(title = { Text("${track.title} - Noctorium") }, modifier = modifier.padding(6.dp)) {
        content(Modifier.fillMaxSize())
    }
}

/**
 * A window titled with the song under a Windows skin, holding [content] on its face: as tall as what is in it, or
 * all of what it is given when [fill]. The layouts that give the cover a window of its own keep the song and its
 * controls in one of these beside it.
 */
@Composable
fun SongWindow(track: Track, modifier: Modifier, fill: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    SkinWindow(title = { Text("${track.title} - Noctorium") }, modifier = modifier) {
        Column(if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth(), content = content)
    }
}

/**
 * The cover filling a window's client area, cropped to fill it and set into the window's edge, the way a picture
 * opened in a viewer of the time filled its window; titled [title]. [footer], where there is one, sits on the
 * window's face under the picture.
 */
@Composable
fun PictureWindow(track: Track, title: String, modifier: Modifier, footer: (@Composable ColumnScope.() -> Unit)? = null) {
    val skin = skin()
    SkinWindow(title = { Text(title) }, modifier = modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
                .drawWithContent {
                    drawContent()
                    if (skin == ThemeSkin.WINDOWS_XP) {
                        drawRect(Luna.FieldEdge, Offset(pixel / 2, pixel / 2), Size(size.width - pixel, size.height - pixel), style = Stroke(pixel))
                    } else {
                        bevel(Bevel.Sunken)
                    }
                },
        ) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
        }
        if (footer != null) Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp), content = footer)
    }
}

/**
 * The type Big type sets the song's name in under a Windows skin: Arial Black in the plain writing colour under 98
 * -- black, as a poster made on a 98 machine would have it, and white on Noctorium 98's black page -- and Luna's
 * Trebuchet at its boldest, in the blue XP headed things with, under XP.
 */
@Composable
fun posterType(): TextStyle = if (skin() == ThemeSkin.WINDOWS_XP) {
    TextStyle(fontFamily = SkinFonts.trebuchet, fontWeight = FontWeight.Bold, color = Luna.TaskPanelTitle, lineHeight = 1.02.em, letterSpacing = (-.01).em)
} else {
    TextStyle(fontFamily = SkinFonts.black, fontWeight = FontWeight.Black, color = Classic.Text, lineHeight = 1.08.em, letterSpacing = (-.01).em)
}

/** The panel of the queue and the lyrics in a window titled [title], under a Windows skin. */
@Composable
fun PanelWindow(modifier: Modifier, title: String, content: @Composable BoxScope.() -> Unit) {
    SkinWindow(title = { Text(title) }, modifier = modifier) {
        Box(Modifier.fillMaxSize(), content = content)
    }
}

/**
 * The panel's tabs over their page, as a property sheet's: [tabs] along the top with the chosen one joined to
 * the page, and the page holding [content].
 */
@Composable
fun <T> PanelTabs(tabs: List<T>, selected: T, name: (T) -> String, choose: (T) -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp)) {
        TabStrip(tabs, selected, name, choose, Modifier.fillMaxWidth())
        TabPage(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(6.dp)) { content() }
        }
    }
}

/**
 * A list set into a white well, as a list box was: sunk under 98, edged in Luna's pale blue under XP. Nothing
 * at all under the standard skin.
 */
@Composable
fun Modifier.listWell(): Modifier {
    val skin = skin()
    if (!skin.isWindows) return this
    return drawWithContent {
        if (skin == ThemeSkin.WINDOWS_XP) {
            drawRect(Luna.Window)
            drawContent()
            drawRect(Luna.FieldEdge, Offset(pixel / 2, pixel / 2), Size(size.width - pixel, size.height - pixel), style = Stroke(pixel))
        } else {
            drawRect(Classic.Window)
            drawContent()
            bevel(Bevel.Sunken)
        }
    }.padding(2.dp)
}

/** The cover, square in its frame and without the shadow it casts elsewhere. */
@Composable
fun SkinCover(track: Track, size: Dp, modifier: Modifier = Modifier) {
    val skin = skin()
    Box(modifier.coverFrame(skin)) {
        RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(size).then(if (skin == ThemeSkin.WINDOWS_98) Modifier.padding(2.dp) else Modifier))
    }
}

/**
 * The colour Windows is asked to paint the title bar it draws above the window, and its writing, under a skin:
 * under 98 the active title bar's first colour in the [palette] in force, so the window's own bar starts where
 * the windows inside it do.
 */
fun ThemeSkin.caption(palette: Windows98Palette = Windows98Palette.STANDARD): Pair<Int, Int>? = when (this) {
    ThemeSkin.STANDARD -> null
    ThemeSkin.WINDOWS_98 -> palette.title.toInt() to palette.titleText.toInt()
    ThemeSkin.WINDOWS_XP -> WindowsXpCaption
}

private val WindowsXpCaption = app.noctorium.settings.WindowsXpColours.TITLE.toInt() to app.noctorium.settings.WindowsXpColours.TITLE_TEXT.toInt()

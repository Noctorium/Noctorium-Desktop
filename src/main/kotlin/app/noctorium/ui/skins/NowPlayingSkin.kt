package app.noctorium.ui.skins

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.noctorium.domain.Track
import app.noctorium.settings.ThemeSkin
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
    SkinWindow(title = { Text("Now Playing - ${track.title}") }, modifier = modifier.padding(6.dp)) {
        content(Modifier.fillMaxSize())
    }
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

/** The colour Windows is asked to paint the title bar it draws above the window, and its writing, under a skin. */
fun ThemeSkin.caption(): Pair<Int, Int>? = when (this) {
    ThemeSkin.STANDARD -> null
    ThemeSkin.WINDOWS_98 -> Windows98Caption
    ThemeSkin.WINDOWS_XP -> WindowsXpCaption
}

private val Windows98Caption = app.noctorium.settings.Windows98Colours.TITLE.toInt() to app.noctorium.settings.Windows98Colours.TITLE_TEXT.toInt()
private val WindowsXpCaption = app.noctorium.settings.WindowsXpColours.TITLE.toInt() to app.noctorium.settings.WindowsXpColours.TITLE_TEXT.toInt()

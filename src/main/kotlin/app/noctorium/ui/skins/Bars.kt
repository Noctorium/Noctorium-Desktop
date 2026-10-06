package app.noctorium.ui.skins

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.LocalInGlass

/**
 * The player bar's ground under a Windows skin, whichever layout is drawn on it.
 *
 * Under 98 it is the window's grey face with a raised edge towards the page, the way the taskbar met the
 * desktop, and its transport is a row of toolbar buttons. Under XP it is the taskbar's blue, lit along the
 * top, with everything on it written white. The layout inside is told it is in glass, which is how a bar
 * already knows to leave its own background and its rule to whatever holds it. The Taskbar layout draws its
 * own taskbar and is passed through untouched, as every layout is under the standard skin.
 */
@Composable
fun SkinnedPlayerBar(position: PlayerBarPosition, style: PlayerBarStyle, content: @Composable () -> Unit) {
    val skin = skin()
    if (!skin.isWindows || style == PlayerBarStyle.TASKBAR) {
        content()
        return
    }
    val atTop = position == PlayerBarPosition.TOP
    if (skin == ThemeSkin.WINDOWS_98) {
        Box(Modifier.fillMaxWidth().drawBehind { drawClassicBar(atTop) }) {
            CompositionLocalProvider(LocalInGlass provides true, content = content)
        }
        return
    }
    Box(Modifier.fillMaxWidth().drawBehind { drawLunaTaskbar(atTop) }) {
        OnTaskbar {
            CompositionLocalProvider(LocalInGlass provides true, content = content)
        }
    }
}

/** The face, with the raised edge on the side that faces the page. */
private fun DrawScope.drawClassicBar(atTop: Boolean) {
    val p = pixel
    drawRect(Classic.Face)
    if (atTop) {
        drawRect(Classic.Shadow, Offset(0f, size.height - p * 2), Size(size.width, p))
        drawRect(Classic.DarkShadow, Offset(0f, size.height - p), Size(size.width, p))
    } else {
        drawRect(Classic.Light, Offset.Zero, Size(size.width, p))
        drawRect(Classic.Highlight, Offset(0f, p), Size(size.width, p))
    }
}

/** Luna's taskbar: a bright line at its top edge, the blue, and a darker foot. */
internal fun DrawScope.drawLunaTaskbar(atTop: Boolean = false) {
    drawRect(
        Brush.verticalGradient(
            0f to Luna.TaskbarTop.towards(Color.White, .3f),
            .06f to Luna.TaskbarTop,
            .22f to Luna.Taskbar,
            .8f to Luna.Taskbar,
            1f to Luna.TaskbarFoot,
        ),
    )
    if (atTop) drawRect(Luna.TaskbarFoot.towards(Color.Black, .3f), Offset(0f, size.height - pixel), Size(size.width, pixel))
}

/**
 * Colours for what sits on Luna's blue: white writing and white pictures, a softer white for what is quieter,
 * and the orange Luna lit things with where the theme would use its accent.
 */
@Composable
internal fun OnTaskbar(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = scheme.copy(
            onSurface = Color.White,
            onBackground = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = .82f),
            primary = Luna.Hot,
            onPrimary = Luna.Text,
            surfaceVariant = Luna.TaskbarFoot,
            primaryContainer = Luna.TaskbarTop,
        ),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White, LocalGround provides Ground.TASKBAR, content = content)
    }
}

/**
 * A bar lifted off the window's edge under a Windows skin, for the two that float over the page.
 *
 * Floating is a toolbar torn off and left standing above the foot of the window: under 98 the face inside a
 * window's raised edge, with the gripper a toolbar was dragged by at its left end; under XP a length of the
 * taskbar's blue with its ends softened, written white. The Island is a small window of its own: raised the
 * same way under 98, and under XP Luna's beige face in its blue frame, with the soft shadow XP put under what
 * stood above everything else.
 */
@Composable
fun LiftedBar(island: Boolean, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val skin = skin()
    if (skin == ThemeSkin.WINDOWS_98) {
        Box(
            modifier.drawBehind {
                drawRect(Classic.Face)
                bevel(Bevel.Window)
                if (!island) drawGripper()
            },
            content = content,
        )
        return
    }
    if (island) {
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier
                .shadow(6.dp, shape, clip = false)
                .clip(shape)
                .drawBehind {
                    drawRect(Luna.Face)
                    val p = pixel
                    drawRoundRect(Luna.Frame, Offset(p, p), Size(size.width - p * 2, size.height - p * 2), CornerRadius(4.dp.toPx()), style = Stroke(p * 2))
                },
            content = content,
        )
        return
    }
    val shape = RoundedCornerShape(3.dp)
    Box(modifier.shadow(6.dp, shape, clip = false).clip(shape).drawBehind { drawLunaTaskbar() }) {
        val box = this
        OnTaskbar { box.content() }
    }
}

/** The two raised ridges at a toolbar's left end that it was taken hold of by. */
private fun DrawScope.drawGripper() {
    val p = pixel
    val top = p * 5
    val height = size.height - p * 10
    listOf(p * 5, p * 9).forEach { x ->
        drawRect(Classic.Highlight, Offset(x, top), Size(p, height))
        drawRect(Classic.Highlight, Offset(x, top), Size(p * 2, p))
        drawRect(Classic.Shadow, Offset(x + p * 2, top), Size(p, height))
        drawRect(Classic.Shadow, Offset(x, top + height - p), Size(p * 3, p))
    }
}

/**
 * The Display bar's display under a Windows skin. Under 98 a sunken pane of black, as the CD Player and the Media
 * Player had theirs, with the song written in white and its place lit in the light blue a title bar ran out to.
 * Under XP the taskbar's tray -- the lighter well at its end that held the clock -- with everything in it white.
 */
@Composable
fun SkinDisplay(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val skin = skin()
    if (skin == ThemeSkin.WINDOWS_XP) {
        val shape = RoundedCornerShape(3.dp)
        Row(
            modifier
                .clip(shape)
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(Luna.Tray.towards(Luna.TaskbarFoot, .25f), Luna.Tray)))
                    val p = pixel
                    drawRoundRect(Luna.TrayEdge, Offset(p / 2, p / 2), Size(size.width - p, size.height - p), CornerRadius(3.dp.toPx()), style = Stroke(p))
                },
            content = content,
        )
        return
    }
    val scheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = scheme.copy(
            onSurface = Classic.SelectionText,
            onBackground = Classic.SelectionText,
            onSurfaceVariant = Classic.Light,
            primary = Classic.TitleEnd,
            surfaceVariant = Classic.DarkShadow,
            background = Classic.DarkShadow,
        ),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(LocalContentColor provides Classic.SelectionText, LocalGround provides Ground.SELECTION) {
            Row(
                modifier.drawWithContent {
                    drawRect(Classic.DarkShadow)
                    drawContent()
                    bevel(Bevel.Sunken)
                }.padding(2.dp),
                content = content,
            )
        }
    }
}

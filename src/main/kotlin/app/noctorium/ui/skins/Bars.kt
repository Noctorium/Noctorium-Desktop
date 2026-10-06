package app.noctorium.ui.skins

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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

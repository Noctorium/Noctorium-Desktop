package app.noctorium.ui.skins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.settings.ThemeSkin

/*
 * Headings within a page, notices across it, choices made by picture, and the buttons a media player of the
 * time was worked with.
 */

/**
 * The heading over one of a page's shelves. Under the standard skin the bold line every shelf had; under 98 a
 * bold label over an etched line the width of the page, as a property page divided itself; under XP the
 * heading Explorer gave each group when a folder was shown in groups -- its blue, over a line fading out.
 */
@Composable
fun SectionTitle(text: String, fontSize: TextUnit) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold)
        return
    }
    val xp = skin == ThemeSkin.WINDOWS_XP
    Column(Modifier.fillMaxWidth()) {
        Text(
            text,
            fontSize = if (xp) 13.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (xp) Luna.TaskPanelTitle else Classic.Text,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            if (xp) {
                drawRect(
                    Brush.horizontalGradient(listOf(Luna.TaskPanelTitle.towards(Luna.Window, .35f), Luna.Window)),
                    size = Size(size.width, pixel),
                )
            } else {
                etchedLine(0f, 0f, size.width)
            }
        }
    }
}

/**
 * A notice across the page: the pale yellow strip, edged in grey, that Internet Explorer brought down from
 * under its toolbar to say something had happened -- [icon], [message] and, when it can be put away, the cross.
 */
@Composable
fun InfoBar(icon: ImageVector, message: String, iconTint: Color = Color.Unspecified, dismiss: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    val skin = skin()
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (skin == ThemeSkin.WINDOWS_XP) Luna.Tooltip else Classic.Tooltip)
            .drawBehind {
                val edge = if (skin == ThemeSkin.WINDOWS_XP) Luna.GreyText else Classic.Shadow
                drawRect(edge, Offset.Zero, Size(size.width, pixel))
                drawRect(edge, Offset(0f, size.height - pixel), Size(size.width, pixel))
                drawRect(edge, Offset.Zero, Size(pixel, size.height))
                drawRect(edge, Offset(size.width - pixel, 0f), Size(pixel, size.height))
            }
            .padding(start = 6.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides Classic.Text) {
            Icon(icon, null, Modifier.size(16.dp), tint = if (iconTint == Color.Unspecified) Classic.Text else iconTint)
            Spacer(Modifier.width(6.dp))
            SelectionContainer(Modifier.weight(1f).padding(vertical = 3.dp)) {
                Text(message, fontSize = 12.sp, color = Classic.Text)
            }
            if (action != null) {
                Spacer(Modifier.width(6.dp))
                action()
            }
            if (dismiss != null) {
                ToolButton(dismiss, Modifier.size(22.dp)) { Icon(Icons.Default.Close, "Dismiss", Modifier.size(14.dp)) }
            }
        }
    }
}

/**
 * A choice made by its picture: a button with the picture on it and the name under that, pressed in while it is
 * the one chosen, as the choices of a view or a scheme were.
 */
@Composable
fun PictureChoice(selected: Boolean, choose: () -> Unit, label: String, picture: @Composable () -> Unit) {
    PushButton(
        choose,
        latched = selected,
        padding = androidx.compose.foundation.layout.PaddingValues(6.dp),
        minWidth = 0.dp,
        contentHeight = Dp.Unspecified,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) { picture() }
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    }
}

/**
 * Previous, play and next as a media player of the time had them: square buttons standing out of its face,
 * play the larger, an hourglass's worth of waiting drawn as Material's spinner while a song is found.
 */
@Composable
fun MediaButtons(
    resolving: Boolean,
    hasNext: Boolean,
    previous: () -> Unit,
    toggle: () -> Unit,
    next: () -> Unit,
    playIcon: @Composable (Modifier) -> Unit,
    previousIcon: ImageVector,
    nextIcon: ImageVector,
) {
    val square = androidx.compose.foundation.layout.PaddingValues(0.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        PushButton(previous, Modifier.size(34.dp), padding = square, minWidth = 0.dp, minHeight = 0.dp, contentHeight = Dp.Unspecified) {
            Icon(previousIcon, "Previous track", Modifier.size(20.dp))
        }
        PushButton(toggle, Modifier.size(42.dp), padding = square, minWidth = 0.dp, minHeight = 0.dp, contentHeight = Dp.Unspecified) {
            if (resolving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current) else playIcon(Modifier.size(26.dp))
        }
        PushButton(next, Modifier.size(34.dp), enabled = hasNext, padding = square, minWidth = 0.dp, minHeight = 0.dp, contentHeight = Dp.Unspecified) {
            Icon(nextIcon, "Next track", Modifier.size(20.dp))
        }
    }
}

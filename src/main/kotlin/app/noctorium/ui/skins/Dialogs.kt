package app.noctorium.ui.skins

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.noctorium.settings.ThemeSkin

/**
 * Material's alert dialog; under a Windows skin a window of its own, with a title bar whose close button
 * dismisses it, the question or the form on the window's face, and the buttons along the bottom on the right in
 * the order Windows put them -- the one that goes ahead first, marked as the default, then the one that does not.
 *
 * Nothing dims the window behind: a dialog of the time simply stood in front of it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest, confirmButton, modifier, dismissButton, icon, title, text, shape, containerColor,
            iconContentColor, titleContentColor, textContentColor, tonalElevation, properties,
        )
        return
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            usePlatformDefaultWidth = false,
            usePlatformInsets = properties.usePlatformInsets,
            useSoftwareKeyboardInset = properties.useSoftwareKeyboardInset,
            scrimColor = Color.Transparent,
        ),
    ) {
        WindowColours {
            SkinWindow(
                title = title ?: { Text("Noctorium") },
                modifier = modifier.widthIn(min = 300.dp, max = 560.dp),
                onClose = onDismissRequest,
            ) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
                    val ink = if (skin == ThemeSkin.WINDOWS_XP) Luna.Text else Classic.Text
                    CompositionLocalProvider(LocalContentColor provides ink) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = ink)) {
                            Row(verticalAlignment = Alignment.Top) {
                                if (icon != null) {
                                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { icon() }
                                    Spacer(Modifier.width(12.dp))
                                }
                                if (text != null) Box(Modifier.weight(1f, fill = false)) { text() }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                        CompositionLocalProvider(LocalDefaultButton provides true) { confirmButton() }
                        dismissButton?.invoke()
                    }
                }
            }
        }
    }
}

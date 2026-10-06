package app.noctorium.ui.skins

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.settings.ThemeSkin

/**
 * Material's outlined text field; under a Windows skin an edit box -- white, sunk into the face under 98 and
 * edged in Luna's pale blue under XP -- with its label written above it rather than inside the edge, the way a
 * dialog labelled its boxes, and a note under it in the small type. The label and the note are drawn by the
 * field itself, so whatever the caller hangs on it -- a focus requester, the typing flag -- still finds the box.
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.OutlinedTextField(
            value, onValueChange, modifier, enabled, readOnly, textStyle, label, placeholder, leadingIcon, trailingIcon,
            prefix, suffix, supportingText, isError, visualTransformation, keyboardOptions, keyboardActions, singleLine,
            maxLines, minLines, interactionSource, shape, colors,
        )
        return
    }
    val xp = skin == ThemeSkin.WINDOWS_XP
    val ink = if (xp) Luna.Text else Classic.Text
    val grey = if (xp) Luna.GreyText else Classic.GreyText
    val error = MaterialTheme.colorScheme.error
    val fontSize = textStyle.fontSize.takeIf { it.isSp && it.value > 0f && it.value <= 13f } ?: 12.sp
    val pixels = with(LocalDensity.current) { fontSize.toPx() }
    val style = textStyle.merge(TextStyle(color = if (enabled) ink else grey, fontSize = fontSize, fontFamily = skin.bodyFamily())).rasterisedFor(pixels)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = style,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        interactionSource = source,
        cursorBrush = SolidColor(ink),
        decorationBox = { inner ->
            Column {
                if (label != null) {
                    CompositionLocalProvider(LocalContentColor provides if (isError) error else ink) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = if (isError) error else ink)) { label() }
                    }
                    Spacer(Modifier.height(3.dp))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = if (xp) 21.dp else 22.dp)
                        .drawBehind {
                            if (xp) {
                                drawRect(if (enabled) Luna.Window else Luna.Face)
                                drawRect(
                                    if (isError) error else Luna.FieldEdge,
                                    Offset(pixel / 2, pixel / 2),
                                    Size(size.width - pixel, size.height - pixel),
                                    style = Stroke(pixel),
                                )
                            } else {
                                drawRect(if (enabled) Classic.Window else Classic.Face)
                                bevel(Bevel.Sunken)
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides grey) {
                        if (leadingIcon != null) {
                            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { leadingIcon() }
                            Spacer(Modifier.width(4.dp))
                        }
                        prefix?.invoke()
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            CompositionLocalProvider(LocalContentColor provides grey) {
                                ProvideTextStyle(style.copy(color = grey)) { placeholder() }
                            }
                        }
                        inner()
                    }
                    CompositionLocalProvider(LocalContentColor provides grey) {
                        suffix?.invoke()
                        if (trailingIcon != null) {
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { trailingIcon() }
                        }
                    }
                }
                if (supportingText != null) {
                    Spacer(Modifier.height(2.dp))
                    CompositionLocalProvider(LocalContentColor provides if (isError) error else grey) {
                        ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(color = if (isError) error else grey)) { supportingText() }
                    }
                }
            }
        },
    )
}

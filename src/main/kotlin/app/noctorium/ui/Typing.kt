package app.noctorium.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Whether the caret is in a box somewhere, which is the one thing the keyboard shortcuts must know.
 *
 * The obvious assumption is wrong and was worth finding out the hard way: a focused text field does not
 * swallow an ordinary letter on its way past. The character goes into the box *and* the key event carries
 * on to the window, so typing "no surprises" into the search box put the text in and also toggled shuffle
 * three times and cycled repeat twice. Nothing about that is visible from reading either piece of code.
 *
 * So focus is tracked rather than assumed. Every text field says when it has the caret; the shortcut
 * handler ignores the unmodified bindings while one of them does.
 */
val LocalTyping = compositionLocalOf { mutableStateOf(false) }

/**
 * Marks a text field as one that takes the keyboard while it has focus.
 *
 * Losing focus clears the flag, including when the window does -- a field cannot be typed in while it is
 * not focused, so there is no state to leak between screens.
 */
@Composable
fun Modifier.tracksTyping(): Modifier {
    val typing = LocalTyping.current
    return onFocusChanged { typing.value = it.isFocused }
}

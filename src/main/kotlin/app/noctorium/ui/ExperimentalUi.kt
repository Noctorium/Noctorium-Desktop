package app.noctorium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.ui.skins.Text

/*
 * Settings › About › Experimental: a box for codes, which do things not yet worth a setting of their own -- and, for
 * now, one easter egg.
 */

/** The codes there are, by what is typed for them. Typed in any case, with spaces either side or not. */
internal enum class ExperimentalCode(val code: String, val what: String) {
    /** Closes Noctorium, as Quit does. Nothing actually crashes. */
    CRASH("crash", "Closes Noctorium."),
    ;

    companion object {
        fun find(typed: String): ExperimentalCode? = entries.firstOrNull { it.code.equals(typed.trim(), ignoreCase = true) }
    }
}

/** Quits Noctorium as its own Quit does, handed down from the application; null where there is none to quit. */
internal val LocalQuit = compositionLocalOf<(() -> Unit)?> { null }

@Composable
internal fun ExperimentalSettingsPanel() {
    val quit = LocalQuit.current
    var typed by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    val enter: () -> Unit = {
        when (ExperimentalCode.find(typed)) {
            ExperimentalCode.CRASH -> if (quit != null) quit() else answer = "Nothing to close here."
            null -> answer = if (typed.isBlank()) null else "Nothing happens. Not yet, anyway."
        }
        typed = ""
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = chromePadding(bottom = 24.dp)) {
        item {
            SettingsPanelCard {
                CardHeading(Icons.Default.Terminal, "Codes")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Things that are not settings yet. Type a code and press Enter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        typed,
                        { typed = it.take(64); answer = null },
                        Modifier.width(320.dp).tracksTyping(),
                        label = { Text("Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { enter() }),
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(enter, enabled = typed.isNotBlank()) { Text("Enter") }
                }
                answer?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

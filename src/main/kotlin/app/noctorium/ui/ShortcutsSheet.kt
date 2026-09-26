package app.noctorium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The list of shortcuts, on `?`.
 *
 * A shortcut nobody can find is not a shortcut, and there is nowhere else in the window this could
 * reasonably live. Read from the same table the bindings are described by, so the sheet cannot drift from
 * what the keys actually do.
 */
@Composable
fun ShortcutsSheet(dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Keyboard shortcuts", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.widthIn(max = 460.dp).heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                shortcutHelp.forEach { (group, rows) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            group.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        rows.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    color = ink(.06f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.widthIn(min = 116.dp),
                                ) {
                                    Text(
                                        row.keys,
                                        // Monospaced so the columns line up whatever the keys are called.
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                                Text(row.what, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "While the caret is in a box, letters are letters — only the combinations with Ctrl, " +
                        "and Escape, still do anything.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(dismiss) { Text("Close") } },
    )
}

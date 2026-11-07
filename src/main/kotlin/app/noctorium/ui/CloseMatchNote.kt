package app.noctorium.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.lyrics.LyricsResult
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text

/** The switch for the note, by the name Settings and its search know it by. */
internal const val CLOSE_MATCH_WARNING = "Warn when lyrics are a close match"

/** The songs whose close-match note was closed: closed for that song until Noctorium is next opened. */
internal object CloseMatchNotes {
    val closed = mutableStateSetOf<String>()
}

/**
 * A line above lyrics found by a close match -- a song uploaded under somebody else's name, whose lyrics were
 * found under the name the lyrics source knows it by -- saying whose lyrics they are, and that they may not be
 * this song's. Small, and closed with its ✕ for the song; switched off for good in Settings › Lyrics providers,
 * which leaves the close match itself as it is.
 */
@Composable
internal fun CloseMatchNote(result: LyricsResult?, trackKey: String, state: AppState) {
    val match = result?.closeMatch ?: return
    val settings by state.settings.collectAsState()
    if (!settings.preferences.lyricsCloseMatchWarning || trackKey in CloseMatchNotes.closed) return
    val tint = MaterialTheme.colorScheme.tertiary
    Surface(
        color = tint.copy(alpha = .10f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(Modifier.padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, Modifier.size(14.dp), tint = tint)
            Spacer(Modifier.width(7.dp))
            Text(
                "Close match: “${match.title}” by ${match.artist}. May not be this song's lyrics.",
                Modifier.weight(1f).padding(vertical = 5.dp),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton({ CloseMatchNotes.closed += trackKey }, Modifier.size(26.dp)) {
                Icon(Icons.Default.Close, "Hide this note for this song", Modifier.size(13.dp))
            }
        }
    }
}

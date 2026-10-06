package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import app.noctorium.ui.skins.AlertDialog
import app.noctorium.ui.skins.Button
import androidx.compose.material3.CircularProgressIndicator
import app.noctorium.ui.skins.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.RadioButton
import androidx.compose.material3.Surface
import app.noctorium.ui.skins.Switch
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.settings.SettingsState
import app.noctorium.social.YouTubeChannel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Every account and channel the session can act as, grouped by the Google account that owns it, each with
 * its picture. Choosing one is choosing both which account requests go as and which channel.
 */
@Composable
internal fun YouTubeChannelChooser(channels: List<YouTubeChannel>, settings: SettingsState, state: AppState) {
    val preferences = settings.preferences
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        channels.groupBy { it.email }.forEach { (email, owned) ->
            if (email != null && channels.map { it.email }.distinct().size > 1) {
                Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            owned.forEach { channel ->
                val chosen = channel.pageId == preferences.youtubePageId && channel.authUser == preferences.youtubeAuthUser
                Surface(
                    onClick = { state.setYouTubeChannel(channel) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (chosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else ink(.04f),
                    border = BorderStroke(1.dp, if (chosen) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else ink(.08f)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(chosen, { state.setYouTubeChannel(channel) })
                        RemoteArtwork(channel.photoUrl, ProviderType.YOUTUBE_MUSIC, Modifier.size(34.dp).clip(CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(channel.name, fontSize = 13.sp, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                listOfNotNull(
                                    channel.handle,
                                    if (channel.isDefault) "the account's own channel" else "a brand channel",
                                ).joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The switch for filing what is played here in the account's YouTube Music history. */
@Composable
internal fun YouTubeHistorySetting(settings: SettingsState, state: AppState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.History, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Add what I play to my YouTube Music history", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Songs from YouTube and YouTube Music show up in your history there, after a few seconds of " +
                    "playing, and YouTube Music's own recommendations learn from them.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        Switch(settings.preferences.youtubeHistory, state::setYouTubeHistory)
    }
}

/**
 * The QR code a phone scans to send its YouTube Music sign-in to this computer.
 *
 * Opening it starts the listener, and closing it stops it. See SessionTransfer for what the code holds and
 * why what crosses the Wi-Fi cannot be read by anyone else on it.
 */
@Composable
internal fun PhoneSignInDialog(state: AppState, close: () -> Unit) {
    val transfer by state.signInTransfer.collectAsState()
    DisposableEffect(Unit) {
        state.receiveYouTubeSignIn()
        onDispose { if (state.signInTransfer.value !is AppState.SignInTransfer.Done) state.cancelSignInTransfer() }
    }
    AlertDialog(
        onDismissRequest = close,
        icon = { Icon(Icons.Default.PhoneAndroid, null) },
        title = { Text("Sign in with your phone") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(360.dp)) {
                when (val current = transfer) {
                    is AppState.SignInTransfer.Waiting -> {
                        Text(
                            "In Noctorium on your phone, open Settings, then YouTube Music, and press Send to computer. " +
                                "Point the phone at this code. Both need to be on the same Wi-Fi.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        QrCode(current.code, Modifier.size(260.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "The code works once, for five minutes, and only for this computer.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AppState.SignInTransfer.Idle, AppState.SignInTransfer.Checking -> {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (current == AppState.SignInTransfer.Checking) "Checking the session with YouTube…" else "Getting ready…",
                            fontSize = 12.sp,
                        )
                    }
                    is AppState.SignInTransfer.Done -> StatusLine(
                        ok = true,
                        text = "Signed in" + (current.channel?.let { " as $it" } ?: "") + ". This computer has its own copy of the session now.",
                    )
                    is AppState.SignInTransfer.Failed -> StatusLine(ok = false, text = current.message)
                }
            }
        },
        confirmButton = {
            when (transfer) {
                is AppState.SignInTransfer.Failed -> TextButton(state::receiveYouTubeSignIn) { Text("Show a new code") }
                is AppState.SignInTransfer.Done -> TextButton(close) { Text("Done") }
                else -> Unit
            }
        },
        dismissButton = { if (transfer !is AppState.SignInTransfer.Done) TextButton(close) { Text("Cancel") } },
    )
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp)
    }
}

/**
 * A QR code, drawn as squares on a white field. Dark on light whatever the theme, because that is what a
 * camera is built to read, with the quiet margin round it that the format requires.
 */
@Composable
internal fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) {
        runCatching {
            QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 4),
            )
        }.getOrNull()
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White)) {
        matrix ?: return@Box
        Canvas(Modifier.matchParentSize()) {
            val cell = size.minDimension / matrix.width
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix[x, y]) {
                        drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + .5f, cell + .5f))
                    }
                }
            }
        }
    }
}

/**
 * Signing in from cookies copied out of a browser: a cookies.txt, a cookie-editor export, or a request
 * header from the network panel. The way SimpMusic's desktop signs in, kept here for when the sign-in
 * window will not cooperate and there is no phone to hand.
 */
@Composable
internal fun PasteCookiesDialog(state: AppState, close: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Paste cookies") },
        text = {
            Column(Modifier.width(520.dp)) {
                Text(
                    "Sign in to music.youtube.com in a private browser window, copy its cookies with a cookie " +
                        "export extension, or the Cookie header of any request in the network panel, and paste " +
                        "them here. Then close that private window without using it again, so the browser " +
                        "does not renew the cookies out from under Noctorium.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    text,
                    { text = it },
                    placeholder = { Text("SAPISID=…; SID=…   or a cookies.txt, or a JSON export") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).tracksTyping(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "These cookies are as good as your password. They stay on this computer, and are only sent to YouTube.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button({ state.importYouTubeCookies(text); close() }, enabled = text.isNotBlank()) { Text("Sign in") }
        },
        dismissButton = { TextButton(close) { Text("Cancel") } },
    )
}

/**
 * Follow the artist of the YouTube song that is playing, on YouTube Music itself.
 *
 * Only once the artist is known -- read from the song's own byline, a moment after it starts -- and only for
 * YouTube songs, which are the ones with an artist to follow there. Pressing it shows the change at once and
 * takes it back if YouTube refuses.
 */
@Composable
internal fun FollowArtistChip(track: Track, state: AppState, modifier: Modifier = Modifier) {
    val artist by state.artistFollow.collectAsState()
    if (track.provider != ProviderType.YOUTUBE_MUSIC && track.provider != ProviderType.YOUTUBE_VIDEO) return
    val known = artist ?: return
    val following = known.following == true
    FilterChip(
        selected = following,
        onClick = state::toggleFollowArtist,
        label = { Text(if (following) "Following ${known.name}" else "Follow ${known.name}", fontSize = 12.sp) },
        leadingIcon = { Icon(if (following) Icons.Default.Check else Icons.Default.PersonAdd, null, Modifier.size(16.dp)) },
        modifier = modifier,
    )
}

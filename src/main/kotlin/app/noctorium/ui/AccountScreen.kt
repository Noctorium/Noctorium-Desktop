package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.account.NoctoriumUser
import app.noctorium.core.AppState
import app.noctorium.core.NoctoriumAccountState
import app.noctorium.domain.ProviderType
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.ScreenTitle
import java.awt.FileDialog
import java.awt.Frame
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** The Noctorium account as a screen of its own, opened from the picture on Home: the account panel, with the profile. */
@Composable
internal fun AccountScreen(state: AppState) {
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 28.dp)) {
        ScreenTitle("Account", 32.sp)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.widthIn(max = 720.dp)) { NoctoriumAccountPanel(state) }
    }
}

/** The listener's picture in a circle: the one on the account, or their initial, or an empty head when signed out. */
@Composable
internal fun AccountPicture(state: AppState, size: Dp, open: () -> Unit) {
    val user = state.account.collectAsState().value.user
    val circle = Modifier.size(size).clip(CircleShape).clickable(onClickLabel = "Account", onClick = open)
    when {
        user?.avatarUrl != null -> RemoteArtwork(user.avatarUrl, ProviderType.YOUTUBE_MUSIC, circle)
        user != null -> Box(circle.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(user.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = (size.value * .42f).sp)
        }
        else -> Box(circle, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountCircle, "Account", Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The website's Profile and danger zone, in the app: the picture, the name, the email, the password and deleting the
 * account. A change of email, password or deleting asks for the password (when the account has one) and the two-step
 * code (when that is on), as the service does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NoctoriumProfileCards(state: AppState, user: NoctoriumUser, account: NoctoriumAccountState) {
    var name by remember(user.displayName) { mutableStateOf(user.displayName) }
    var email by remember(user.email) { mutableStateOf(user.email) }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var proofPassword by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val busy = account.profileBusy
    val twoStep = user.twoStep.totp || user.twoStep.telegram
    fun proof() = buildMap {
        if (user.hasPassword) put("password", proofPassword)
        if (twoStep) put("code", code)
    }
    fun sent() { proofPassword = ""; code = ""; newPassword = ""; confirm = "" }

    SettingsPanelCard {
        CardHeading(Icons.Default.Badge, "Profile")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountPicture(state, 64.dp) { choosePicture()?.let(state::uploadNoctoriumAvatar) }
            Spacer(Modifier.width(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton({ choosePicture()?.let(state::uploadNoctoriumAvatar) }, enabled = !busy) { Text("Choose a picture…") }
                if (user.linked.discord) OutlinedButton({ state.changeNoctoriumProfile("avatar-discord") }, enabled = !busy) { Text("Use Discord's") }
                if (user.linked.telegram) OutlinedButton({ state.changeNoctoriumProfile("avatar-telegram") }, enabled = !busy) { Text("Use Telegram's") }
                if (user.avatarUrl != null) TextButton({ state.changeNoctoriumProfile("avatar-remove") }, enabled = !busy) { Text("Remove") }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.weight(1f).tracksTyping())
            Spacer(Modifier.width(9.dp))
            Button({ state.changeNoctoriumProfile("name", mapOf("name" to name)) }, enabled = !busy && name.isNotBlank() && name != user.displayName) { Text("Save") }
        }
    }
    Spacer(Modifier.height(14.dp))
    SettingsPanelCard {
        CardHeading(Icons.Default.Lock, "Email and password")
        Text(
            "Changing either signs your other devices out. This one stays signed in.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        ProofFields(state, user, proofPassword, { proofPassword = it }, code, { code = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.weight(1f).tracksTyping())
            Spacer(Modifier.width(9.dp))
            Button(
                { state.changeNoctoriumProfile("email", proof() + ("email" to email)); sent() },
                enabled = !busy && email.isNotBlank() && email != user.email,
            ) { Text("Change email") }
        }
        if (user.hasPassword) {
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    newPassword, { newPassword = it }, label = { Text("New password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = { Text("At least 10 characters.") },
                    modifier = Modifier.weight(1f).tracksTyping(),
                )
                Spacer(Modifier.width(9.dp))
                Button(
                    {
                        state.changeNoctoriumProfile("password", mapOf("current" to proofPassword, "password" to newPassword) + (if (twoStep) mapOf("code" to code) else emptyMap()))
                        sent()
                    },
                    enabled = !busy && newPassword.isNotBlank() && proofPassword.isNotBlank(),
                ) { Text("Change password") }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    SettingsPanelCard {
        CardHeading(Icons.Default.DeleteOutline, "Delete the account")
        Text(
            "Deletes the account and everything it counted, for good. Type \"delete my account\" to confirm; the password and code above prove it is you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(confirm, { confirm = it }, label = { Text("delete my account") }, singleLine = true, modifier = Modifier.weight(1f).tracksTyping())
            Spacer(Modifier.width(9.dp))
            Button(
                { state.changeNoctoriumProfile("delete", proof() + ("confirm" to confirm)); sent() },
                enabled = !busy && confirm.trim().equals("delete my account", ignoreCase = true),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        }
    }
    if (busy) {
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    account.profileError?.let { message ->
        Spacer(Modifier.height(10.dp))
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, Modifier.weight(1f), fontSize = 12.sp)
                TextButton(state::clearNoctoriumProfileError) { Text("OK") }
            }
        }
    }
}

/** The current password and the two-step code, shown only when the account has them. */
@Composable
private fun ProofFields(state: AppState, user: NoctoriumUser, password: String, setPassword: (String) -> Unit, code: String, setCode: (String) -> Unit) {
    if (user.hasPassword) {
        OutlinedTextField(
            password, setPassword, label = { Text("Current password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().tracksTyping(),
        )
        Spacer(Modifier.height(9.dp))
    }
    if (user.twoStep.totp || user.twoStep.telegram) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                code, setCode, singleLine = true,
                label = { Text(if (user.twoStep.totp) "Code from your authenticator app" else "Code from Telegram") },
                modifier = Modifier.weight(1f).tracksTyping(),
            )
            if (user.twoStep.telegram) {
                Spacer(Modifier.width(9.dp))
                TextButton(state::sendNoctoriumProfileTelegramCode) { Text("Send on Telegram") }
            }
        }
        Spacer(Modifier.height(9.dp))
    }
}

/** Asks for a picture with the system's file dialog; null when none was chosen or it is not one ImageIO reads. */
private fun choosePicture(): ByteArray? {
    val dialog = FileDialog(null as Frame?, "Choose a profile picture", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in setOf("jpg", "jpeg", "png", "gif", "bmp") }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return runCatching { ImageIO.read(File(dialog.directory, file)) }.getOrNull()?.let(::squareJpeg)
}

/** The middle square of [image], at 256 pixels, as a JPEG: what the account service keeps. */
internal fun squareJpeg(image: BufferedImage, size: Int = 256): ByteArray {
    val side = minOf(image.width, image.height)
    val out = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    out.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        drawImage(image, 0, 0, size, size, (image.width - side) / 2, (image.height - side) / 2, (image.width + side) / 2, (image.height + side) / 2, java.awt.Color.WHITE, null)
        dispose()
    }
    return ByteArrayOutputStream().also { ImageIO.write(out, "jpg", it) }.toByteArray()
}

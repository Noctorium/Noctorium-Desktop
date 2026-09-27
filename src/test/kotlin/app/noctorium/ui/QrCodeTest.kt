package app.noctorium.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.auth.SessionTransfer
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.jetbrains.skia.Bitmap
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The code the phone scans, drawn the way the dialog draws it and then read back the way a camera reads it.
 *
 * A QR code that looks right and does not scan is the one failure nobody can see on the screen, so this
 * does not look: it decodes the pixels and compares what comes out with what went in.
 */
class QrCodeTest {

    @Test
    fun `the code on the screen scans back to exactly the invite`() {
        val invite = SessionTransfer.Invite(listOf("192.168.1.20", "10.0.0.5"), 51234, ByteArray(32) { (it * 7).toByte() })
        val code = invite.code()

        val scene = ImageComposeScene(300, 300, Density(1f)) { QrCode(code, Modifier.size(300.dp)) }
        val image = try {
            scene.render()
        } finally {
            scene.close()
        }
        val bitmap = Bitmap.makeFromImage(image)
        val pixels = IntArray(bitmap.width * bitmap.height) { index ->
            bitmap.getColor(index % bitmap.width, index / bitmap.width)
        }
        val read = QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))),
        ).text

        assertEquals(code, read)
        val parsed = SessionTransfer.parseInvite(read)!!
        assertEquals(invite.hosts, parsed.hosts)
        assertEquals(invite.port, parsed.port)
    }
}

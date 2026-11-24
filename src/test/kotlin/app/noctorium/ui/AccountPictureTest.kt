package app.noctorium.ui

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountPictureTest {
    @Test
    fun `a wide picture becomes its middle square, 256 pixels, as a JPEG`() {
        val wide = BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB)
        val jpeg = squareJpeg(wide)
        val back = ImageIO.read(jpeg.inputStream())
        assertEquals(256, back.width)
        assertEquals(256, back.height)
        assertEquals(0xFF.toByte(), jpeg[0])
        assertEquals(0xD8.toByte(), jpeg[1])
    }
}

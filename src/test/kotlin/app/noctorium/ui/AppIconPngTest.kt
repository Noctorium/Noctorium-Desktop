package app.noctorium.ui

import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Linux packages take a PNG where Windows takes an .ico, and it is generated from the same artwork.
 *
 * Guarded the same way as the .ico: a committed file that drifts from the code means the installed
 * application and the running window stop looking like each other, and nothing else would notice.
 */
class AppIconPngTest {

    private val committed = File("src/main/resources/noctorium.png")

    /** As a picture rather than byte for byte; see the note on the same guard in AppIconTest. */
    @Test
    fun `the committed png shows what the code draws`() {
        assertTrue(committed.isFile, "src/main/resources/noctorium.png is missing")
        assertSamePicture(
            ImageIO.read(ByteArrayInputStream(AppIcon.pngBytes())),
            ImageIO.read(committed),
            "the committed png is out of date; regenerate it with -Dnoctorium.writeIcons=true",
        )
    }

    @Test
    fun `it is a real png, square, and the size the packagers are given`() {
        val image = ImageIO.read(committed)
        assertEquals(AppIcon.LINUX_ICON_SIZE, image.width)
        assertEquals(AppIcon.LINUX_ICON_SIZE, image.height)
        // A transparent middle would package and install and then show as nothing in the launcher.
        assertEquals(255, image.getRGB(image.width / 2, image.height / 2) ushr 24, "the icon is transparent")
    }

    /** The artwork it is made from, which is the one file that has to be there for any of this to work. */
    @Test
    fun `the artwork is on the classpath and square`() {
        val stream = AppIcon::class.java.getResourceAsStream("/noctorium-mark.png")
        assertTrue(stream != null, "noctorium-mark.png is missing from the resources")
        val artwork = stream!!.use(ImageIO::read)
        assertEquals(artwork.width, artwork.height, "the artwork is not square, so every icon would be stretched")
        assertTrue(artwork.width >= AppIcon.LINUX_ICON_SIZE, "the artwork is smaller than the icon drawn from it")
    }
}

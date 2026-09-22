package app.noctorium.ui

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The application's mark.
 *
 * Scaled from one piece of artwork, so what is worth checking is that it arrives: that every size is
 * actually the picture rather than a blank square, a black square or a smear, and that the files the
 * packagers read still show what the code draws. A transparent image would compile, be set on the window,
 * and show as nothing.
 */
class AppIconTest {
    @Test
    fun `every size Windows asks for is drawn at that size`() {
        AppIcon.SIZES.forEach { size ->
            val image = AppIcon.image(size)
            assertEquals(size, image.width, "the $size icon is the wrong width")
            assertEquals(size, image.height, "the $size icon is the wrong height")
        }
        // The shell wants the small ones; a set that stops short leaves them scaled down from a big one.
        assertTrue(16 in AppIcon.SIZES && 32 in AppIcon.SIZES && 256 in AppIcon.SIZES)
    }

    @Test
    fun `the middle is painted, so the icon is not simply blank`() {
        AppIcon.SIZES.forEach { size ->
            val centre = AppIcon.image(size).getRGB(size / 2, size / 2)
            assertEquals(255, centre ushr 24, "the $size icon is transparent in the middle")
        }
    }

    /** Square corners would sit oddly against the rounded mark inside the window. */
    @Test
    fun `the corners are rounded away`() {
        val image = AppIcon.image(64)

        listOf(0 to 0, 63 to 0, 0 to 63, 63 to 63).forEach { (x, y) ->
            assertEquals(0, image.getRGB(x, y) ushr 24, "the corner at $x,$y is not rounded")
        }
    }

    /**
     * The artwork is a bright figure on a near-black field, so both have to survive the scaling.
     *
     * At sixteen pixels a careless downscale averages the two into an even grey, which is the failure
     * this guards: the icon would still be opaque, still be the right size, and read as a smudge.
     */
    @Test
    fun `the picture survives every size, dark field and bright figure both`() {
        AppIcon.SIZES.forEach { size ->
            val image = AppIcon.image(size)
            var darkest = 255
            var brightest = 0
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val pixel = image.getRGB(x, y)
                    if (pixel ushr 24 < 128) continue
                    val luma = luma(pixel)
                    if (luma < darkest) darkest = luma
                    if (luma > brightest) brightest = luma
                }
            }
            assertTrue(darkest < 40, "the $size icon has no dark field: darkest is $darkest")
            assertTrue(brightest > 170, "the $size icon has no bright figure: brightest is $brightest")
        }
    }

    @Test
    fun `the icon file is a real ico, with an entry for every size`() {
        val bytes = AppIcon.icoBytes()

        assertEquals(0, bytes[0].toInt())
        assertEquals(0, bytes[1].toInt())
        // Type 1 is an icon; 2 would be a cursor and Windows would refuse it.
        assertEquals(1, bytes[2].toInt())
        assertEquals(0, bytes[3].toInt())
        assertEquals(AppIcon.SIZES.size, bytes[4].toInt() or (bytes[5].toInt() shl 8))
    }

    @Test
    fun `256 is recorded as zero, which is how the format says 256`() {
        val bytes = AppIcon.icoBytes(listOf(256))

        // First entry begins at byte 6; its width and height are the first two bytes of it.
        assertEquals(0, bytes[6].toInt())
        assertEquals(0, bytes[7].toInt())
    }

    @Test
    fun `each entry points at data that is inside the file`() {
        val bytes = AppIcon.icoBytes()
        val count = bytes[4].toInt() or (bytes[5].toInt() shl 8)

        repeat(count) { index ->
            val entry = 6 + index * 16
            val length = int(bytes, entry + 8)
            val offset = int(bytes, entry + 12)
            assertTrue(length > 0, "entry $index has no data")
            assertTrue(offset + length <= bytes.size, "entry $index points past the end of the file")
        }
    }

    /**
     * Everything but the largest is an uncompressed DIB.
     *
     * A file written entirely in PNG is legal and Explorer reads it, but the older imaging APIs do not:
     * one loaded through `System.Drawing` as a semi-transparent smear and silently dropped its 256 entry.
     * Whatever stamps the executable during packaging is one of those, so the encoding is worth pinning.
     */
    @Test
    fun `the small sizes are plain DIBs and only the largest is a PNG`() {
        val bytes = AppIcon.icoBytes()
        val count = bytes[4].toInt() or (bytes[5].toInt() shl 8)

        repeat(count) { index ->
            val entry = 6 + index * 16
            val declared = bytes[entry].toInt() and 0xFF
            val offset = int(bytes, entry + 12)
            val isPng = bytes[offset] == 0x89.toByte() && bytes[offset + 1] == 'P'.code.toByte()

            if (declared == 0) {
                assertTrue(isPng, "the 256 entry should be a PNG")
            } else {
                assertTrue(!isPng, "the $declared entry should be a DIB, not a PNG")
                // A DIB begins with the length of its own header, which is always forty.
                assertEquals(40, int(bytes, offset), "the $declared entry has no DIB header")
                // And claims twice its height, counting the mask stacked beneath it.
                assertEquals(declared * 2, int(bytes, offset + 8), "the $declared entry's height is wrong")
            }
        }
    }

    /**
     * The icon shipped with the build is a file on disk, while the one in the running window is drawn.
     * If the two drift, the taskbar and the installed application stop looking like each other.
     *
     * Compared as a picture rather than byte for byte. Scaling six hundred pixels down to two hundred and
     * fifty-six is arithmetic the platform is free to round its own way, and the encoder is free to
     * deflate the result differently, so identical bytes were never the claim worth making -- the old
     * guard sidestepped this by comparing bytes only on a machine with a particular font, which meant it
     * never ran on the build server at all. Different artwork differs by hundreds per channel; the same
     * artwork through a different rounding differs by one or two.
     */
    @Test
    fun `the committed icon file shows what the code draws`() {
        val committed = File("src/main/resources/noctorium.ico")
        assertTrue(committed.isFile, "src/main/resources/noctorium.ico is missing")
        val bytes = committed.readBytes()

        assertEquals(1, bytes[2].toInt(), "the committed file is not an icon")
        assertEquals(
            AppIcon.SIZES.size,
            bytes[4].toInt() or (bytes[5].toInt() shl 8),
            "the committed icon has the wrong number of sizes",
        )

        // The 256 entry is the PNG one, so it can be decoded here without a DIB reader; it is also the
        // one that carries the most of the picture, which is what is being compared.
        val entry = 6 + AppIcon.SIZES.indexOf(256) * 16
        val length = int(bytes, entry + 8)
        val offset = int(bytes, entry + 12)
        val largest = ImageIO.read(ByteArrayInputStream(bytes, offset, length))
            ?: error("the 256 entry of the committed icon could not be decoded")

        assertSamePicture(
            AppIcon.image(256),
            largest,
            "the committed icon is out of date; regenerate it with -Dnoctorium.writeIcons=true",
        )
    }

    private fun luma(argb: Int): Int =
        ((argb shr 16 and 0xFF) * 299 + (argb shr 8 and 0xFF) * 587 + (argb and 0xFF) * 114) / 1000

    private fun int(bytes: ByteArray, at: Int) = (bytes[at].toInt() and 0xFF) or
        ((bytes[at + 1].toInt() and 0xFF) shl 8) or
        ((bytes[at + 2].toInt() and 0xFF) shl 16) or
        ((bytes[at + 3].toInt() and 0xFF) shl 24)
}

/**
 * Whether two images are the same picture, allowing for a platform rounding a scale differently.
 *
 * A changed picture is out by hundreds on some channel somewhere; a differently rounded one is out by a
 * unit or two on a handful of pixels. Both a worst case and an average are checked, because either alone
 * can be fooled: a mean hides one glaring pixel, and a maximum trips over a single rounding.
 */
internal fun assertSamePicture(expected: BufferedImage, actual: BufferedImage, message: String) {
    assertEquals(expected.width, actual.width, "$message (width)")
    assertEquals(expected.height, actual.height, "$message (height)")

    var worst = 0
    var total = 0L
    for (x in 0 until expected.width) {
        for (y in 0 until expected.height) {
            val a = expected.getRGB(x, y)
            val b = actual.getRGB(x, y)
            listOf(24, 16, 8, 0).forEach { shift ->
                val difference = kotlin.math.abs(((a shr shift) and 0xFF) - ((b shr shift) and 0xFF))
                if (difference > worst) worst = difference
                total += difference
            }
        }
    }
    val mean = total.toDouble() / (expected.width * expected.height * 4)
    assertTrue(worst <= 8, "$message (a pixel is out by $worst)")
    assertTrue(mean <= 1.0, "$message (out by $mean per channel on average)")
}

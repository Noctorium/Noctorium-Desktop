package app.noctorium.ui

import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * The application's mark: the artwork, drawn into whatever size is asked for.
 *
 * One picture, shipped beside the code, is the mark in the title bar, on the taskbar, in the installer,
 * in the Linux packages and beside the name inside the window -- and the same picture is the phone's
 * launcher icon. It used to be a gradient square with an N drawn into it, which had the advantage of
 * being sharp at sixteen pixels and the disadvantage of being a letter in a box.
 *
 * Scaling is done here rather than by shipping a bitmap per size, so there is one file to change.
 */
object AppIcon {
    /** Where the artwork lives on the classpath. Square; everything below assumes that. */
    private const val ARTWORK = "/noctorium-mark.png"

    /** The corner radius, as a fraction of the side. The same proportion the mark has always had. */
    private const val CORNER_RATIO = 12f / 36f

    /** The sizes Windows picks between for the title bar, the task bar, Alt-Tab and the shell. */
    val SIZES = listOf(16, 20, 24, 32, 48, 64, 128, 256)

    /**
     * The artwork, read once.
     *
     * Converted to ARGB on the way in: the file is opaque, and every image this object draws into has an
     * alpha channel for the rounded corners, so doing the conversion once keeps the scaling below on one
     * pixel layout instead of two.
     */
    private val artwork: BufferedImage by lazy {
        val stream = AppIcon::class.java.getResourceAsStream(ARTWORK)
            ?: error("$ARTWORK is missing from the resources")
        val loaded = stream.use(ImageIO::read) ?: error("$ARTWORK could not be decoded")
        toArgb(loaded)
    }

    fun image(size: Int): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            // Clipped rather than drawn and then masked: the corners have to be genuinely transparent, or
            // the icon is a black square against every taskbar that is not black.
            val radius = size * CORNER_RATIO
            g.clip = RoundRectangle2D.Float(0f, 0f, size.toFloat(), size.toFloat(), radius * 2, radius * 2)
            g.drawImage(scaled(size), 0, 0, null)
        } finally {
            g.dispose()
        }
        return image
    }

    /** Every size at once, for handing to a window that will choose between them itself. */
    fun images(): List<BufferedImage> = SIZES.map(::image)

    /**
     * The artwork at [target] pixels, halved repeatedly rather than scaled in one step.
     *
     * One bilinear pass from six hundred pixels to sixteen samples a handful of them and misses the rest,
     * which on artwork this fine turns the wings into speckle and the face into noise. Halving until the
     * target is within reach averages every pixel on the way down. Each size is drawn once per run and
     * the results are small, so they are kept.
     */
    private val scaledCache = java.util.concurrent.ConcurrentHashMap<Int, BufferedImage>()

    private fun scaled(target: Int): BufferedImage = scaledCache.getOrPut(target) {
        var current = artwork
        var width = current.width
        while (width / 2 > target) {
            width /= 2
            current = resize(current, width)
        }
        resize(current, target)
    }

    private fun resize(source: BufferedImage, size: Int): BufferedImage {
        val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(source, 0, 0, size, size, null)
        } finally {
            g.dispose()
        }
        return out
    }

    private fun toArgb(source: BufferedImage): BufferedImage {
        if (source.type == BufferedImage.TYPE_INT_ARGB) return source
        val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.drawImage(source, 0, 0, null)
        } finally {
            g.dispose()
        }
        return out
    }

    /**
     * The mark as a Windows `.ico`, for the installer and the built executable.
     *
     * Sizes up to 128 go in as uncompressed DIBs and only 256 as PNG. The format has allowed PNG for every
     * size since Vista, and Explorer reads it — but the older imaging APIs do not, and a file written
     * entirely in PNG loaded as a semi-transparent smear through `System.Drawing` and lost its largest
     * entry altogether. Since whatever `jpackage` uses to stamp the executable is one of those older paths,
     * the conservative encoding is the one worth shipping.
     */
    fun icoBytes(sizes: List<Int> = SIZES): ByteArray {
        val entries = sizes.map { size -> size to if (size >= 256) pngEntry(size) else dibEntry(size) }
        val out = ByteArrayOutputStream()

        fun short(value: Int) {
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
        }

        fun int(value: Int) {
            short(value and 0xFFFF)
            short((value shr 16) and 0xFFFF)
        }

        short(0) // reserved
        short(1) // 1 means an icon rather than a cursor
        short(entries.size)

        // Every directory entry is the same width, so the first image starts after all of them.
        var offset = 6 + entries.size * 16
        entries.forEach { (size, data) ->
            // 256 does not fit in a byte and is written as 0, which the format defines to mean 256.
            out.write(if (size >= 256) 0 else size)
            out.write(if (size >= 256) 0 else size)
            out.write(0) // a palette of no fixed size
            out.write(0) // reserved
            short(1) // colour planes
            short(32) // bits per pixel
            int(data.size)
            int(offset)
            offset += data.size
        }
        entries.forEach { (_, data) -> out.write(data) }
        return out.toByteArray()
    }

    /**
     * The mark as a PNG, which is what Linux packaging wants.
     *
     * dpkg and rpm both take a single square PNG rather than a container of sizes, so there is no .ico
     * equivalent to build here -- one image, large enough that a desktop environment can scale it down
     * for whichever slot it needs.
     */
    fun pngBytes(size: Int = LINUX_ICON_SIZE): ByteArray = pngEntry(size)

    /** Big enough for an application grid on a high-resolution display to scale from, and no bigger. */
    const val LINUX_ICON_SIZE = 512

    private fun pngEntry(size: Int): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(image(size), "png", it) }.toByteArray()

    /**
     * One image as the icon format's original DIB: a header, the pixels bottom-up in BGRA, then a
     * one-bit-per-pixel mask.
     *
     * The header claims twice the real height because it counts the mask as a second image stacked under
     * the first. The mask is left empty: with a real alpha channel present it is redundant, but a reader
     * that skips the alpha still expects to find it, and one that is missing shifts everything after it.
     */
    private fun dibEntry(size: Int): ByteArray {
        val image = image(size)
        val out = ByteArrayOutputStream()

        fun short(value: Int) {
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
        }

        fun int(value: Int) {
            short(value and 0xFFFF)
            short((value shr 16) and 0xFFFF)
        }

        // Each mask row is padded out to a whole number of four-byte words.
        val maskStride = ((size + 31) / 32) * 4

        int(40) // the header's own length
        int(size)
        int(size * 2) // the image and its mask together
        short(1) // colour planes
        short(32) // bits per pixel
        int(0) // uncompressed
        int(size * size * 4 + maskStride * size)
        int(0) // horizontal resolution, unused
        int(0) // vertical resolution, unused
        int(0) // colours used
        int(0) // colours that matter

        // Bottom-up, which is how a DIB stores its rows.
        for (y in size - 1 downTo 0) {
            for (x in 0 until size) {
                val argb = image.getRGB(x, y)
                out.write(argb and 0xFF) // blue
                out.write((argb shr 8) and 0xFF) // green
                out.write((argb shr 16) and 0xFF) // red
                out.write((argb shr 24) and 0xFF) // alpha
            }
        }
        repeat(size * maskStride) { out.write(0) }
        return out.toByteArray()
    }
}

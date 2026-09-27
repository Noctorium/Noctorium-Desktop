package app.noctorium.ui

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArtworkLoaderTest {
    // Buckets exist so two rows a few pixels apart do not each decode and cache the same cover.
    @Test
    fun `nearby sizes land in the same bucket`() {
        assertEquals(artworkSizeBucket(120), artworkSizeBucket(128))
        assertEquals(artworkSizeBucket(200), artworkSizeBucket(256))
        assertEquals(artworkSizeBucket(400), artworkSizeBucket(512))
    }

    @Test
    fun `a bucket is never smaller than what was asked for`() {
        listOf(1, 42, 63, 64, 65, 127, 129, 255, 257, 511, 513, 900).forEach { requested ->
            assertTrue(
                artworkSizeBucket(requested) >= requested,
                "bucket for $requested was ${artworkSizeBucket(requested)}",
            )
        }
    }

    @Test
    fun `distinct display sizes stay in distinct buckets`() {
        // A row thumbnail and the now playing hero must not evict one another.
        assertTrue(artworkSizeBucket(42) < artworkSizeBucket(430))
    }

    @Test
    fun `an unmeasured box falls back to a usable size rather than zero`() {
        assertEquals(384, artworkSizeBucket(0))
        assertEquals(384, artworkSizeBucket(-1))
    }

    @Test
    fun `very large requests are capped so one cover cannot dominate the cache`() {
        assertEquals(1024, artworkSizeBucket(4000))
    }

    /**
     * Thirty now playing covers at 1024 pixels are 120 megabytes of pixels, outside the Java heap. The
     * count bound alone would have kept every one of them.
     */
    @Test
    fun `covers are held to a budget in bytes, not only a count`() {
        ArtworkStore.clear()
        try {
            fun key(n: Int) = "https://c/$n" + ArtworkStore.SIZE_SEPARATOR + 1024
            repeat(30) { ArtworkStore.put(key(it), ImageBitmap(1024, 1024)) }
            assertTrue(ArtworkStore.bytesHeld() <= 96L * 1024 * 1024, "holding ${ArtworkStore.bytesHeld()} bytes")
            assertNotNull(ArtworkStore.get(key(29)), "the cover just added was the one let go")
            assertNull(ArtworkStore.get(key(0)), "the oldest cover was kept past the budget")

            // Small ones are not pushed out by the budget: a screen of thumbnails is well inside it.
            ArtworkStore.clear()
            repeat(150) { ArtworkStore.put("https://t/$it" + ArtworkStore.SIZE_SEPARATOR + 128, ImageBitmap(128, 128)) }
            assertNotNull(ArtworkStore.get("https://t/0" + ArtworkStore.SIZE_SEPARATOR + 128))
        } finally {
            ArtworkStore.clear()
        }
    }

    @Test
    fun `nothing is cached for a blank address`() {
        assertEquals(null, ArtworkLoader.cached(null))
        assertEquals(null, ArtworkLoader.cached(""))
        assertEquals(null, ArtworkLoader.cached("   "))
    }
}

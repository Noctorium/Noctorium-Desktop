package app.noctorium.ui

import app.noctorium.domain.artworkAt
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.noctorium.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import org.jetbrains.skia.Image as SkiaImage

/**
 * Decoded covers, kept to a fixed number of entries.
 *
 * Covers used to be held in a map that only ever grew: a long playlist meant hundreds of full-size bitmaps
 * resident at once. Least-recently-used eviction bounds that, and the size the cover was decoded at is part of
 * the key so a thumbnail and a hero image never fight over one slot.
 *
 * Bounded by bytes as well as by count. A count alone was enough while every cover arrived at 120 pixels:
 * a hundred and sixty of those are nine megabytes. Covers now come at the size they are drawn, and a now
 * playing cover at 1024 pixels is four megabytes on its own -- in Skia's memory, outside the Java heap,
 * where no garbage collector is watching the total. A long listening session left one behind for every
 * track it played.
 */
internal object ArtworkStore {
    private const val MAX_ENTRIES = 160
    private const val MAX_BYTES = 96L * 1024 * 1024

    private val memory = LinkedHashMap<String, ImageBitmap>(64, .75f, true)
    private var bytes = 0L

    @Synchronized
    fun get(key: String): ImageBitmap? = memory[key]

    @Synchronized
    fun put(key: String, image: ImageBitmap) {
        memory.put(key, image)?.let { bytes -= sizeOf(it) }
        bytes += sizeOf(image)
        // Least recently used first, and never the one just added, however large: it is on screen.
        val oldest = memory.entries.iterator()
        while ((memory.size > MAX_ENTRIES || bytes > MAX_BYTES) && oldest.hasNext()) {
            val entry = oldest.next()
            if (entry.key == key) continue
            bytes -= sizeOf(entry.value)
            oldest.remove()
        }
    }

    private fun sizeOf(image: ImageBitmap): Long = image.width.toLong() * image.height * 4

    @Synchronized
    internal fun bytesHeld(): Long = bytes

    @Synchronized
    internal fun clear() {
        memory.clear()
        bytes = 0
    }

    /** Any decoded size of this cover, for callers that only need the pixels — the palette sampler included. */
    @Synchronized
    fun anySizeOf(url: String): ImageBitmap? =
        memory.entries.lastOrNull { it.key.startsWith(url + SIZE_SEPARATOR) }?.value

    /** A character no address contains, so no address can end in something that looks like a size. */
    const val SIZE_SEPARATOR = '\u0000'
}

/** Encoded covers on disk, so relaunching does not re-download everything the listener already looked at. */
private object ArtworkDiskCache {
    private const val MAX_FILES = 500
    private val writesSinceSweep = AtomicInteger()

    private val directory: Path? by lazy {
        SettingsRepository.defaultSettingsPath()?.resolveSibling("artwork")?.also {
            runCatching { Files.createDirectories(it) }
        }
    }

    fun read(url: String): ByteArray? {
        val file = fileFor(url) ?: return null
        return runCatching { if (Files.isRegularFile(file)) Files.readAllBytes(file) else null }.getOrNull()
    }

    fun write(url: String, bytes: ByteArray) {
        val file = fileFor(url) ?: return
        runCatching { Files.write(file, bytes) }
        // Sweeping on every write would stat the whole directory constantly; every fiftieth is plenty.
        if (writesSinceSweep.incrementAndGet() >= 50) {
            writesSinceSweep.set(0)
            sweep()
        }
    }

    private fun sweep() {
        val root = directory ?: return
        runCatching {
            Files.list(root).use { stream ->
                val files = stream.toList()
                if (files.size <= MAX_FILES) return
                files.sortedBy { runCatching { Files.getLastModifiedTime(it).toMillis() }.getOrDefault(0L) }
                    .take(files.size - MAX_FILES)
                    .forEach { runCatching { Files.deleteIfExists(it) } }
            }
        }
    }

    private fun fileFor(url: String): Path? {
        val root = directory ?: return null
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        return root.resolve(digest.joinToString("") { "%02x".format(it) })
    }
}

/**
 * Fetches and decodes cover art for the interface.
 *
 * Three things keep scrolling smooth. Only a few downloads run at once, so a long list cannot open a hundred
 * connections at the same moment. Requests for the same cover share one fetch, however many rows ask. And a
 * fetch is not abandoned when its row scrolls away, because finishing and caching it is cheaper than starting
 * again when the listener scrolls back.
 */
internal object ArtworkLoader {
    private const val PARALLEL_DOWNLOADS = 4

    private val gate = Semaphore(PARALLEL_DOWNLOADS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    /** Already-decoded pixels for this cover, at whatever size was loaded. Never touches the network. */
    fun cached(url: String?): ImageBitmap? = url?.takeIf(String::isNotBlank)?.let(ArtworkStore::anySizeOf)

    suspend fun load(url: String, targetPx: Int): ImageBitmap? {
        if (url.isBlank()) return null
        val key = url + ArtworkStore.SIZE_SEPARATOR + targetPx
        ArtworkStore.get(key)?.let { return it }

        val work = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    gate.withPermit {
                        ArtworkStore.get(key) ?: run {
                            // The cover at the size it will be drawn, not the size the listing named. See
                            // artworkAt: a YouTube Music listing names 120 pixels, which is the blur on the
                            // now playing screen. The listing's own address stays as the fallback, in case
                            // a service ever refuses the sized one.
                            val sized = artworkAt(url, targetPx)
                            val bytes = fetchCached(sized) ?: if (sized != url) fetchCached(url) else null
                            bytes?.let { decodeScaled(it, targetPx) }?.also { ArtworkStore.put(key, it) }
                        }
                    }
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return runCatching { work.await() }.getOrNull()
    }

    /** From the disk if it has been fetched before, otherwise from the network and then kept. */
    private fun fetchCached(url: String): ByteArray? =
        ArtworkDiskCache.read(url) ?: download(url)?.also { ArtworkDiskCache.write(url, it) }

    private fun download(url: String): ByteArray? = runCatching {
        val connection = URI(url).toURL().openConnection().apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "Noctorium/0.1")
        }
        connection.getInputStream().use { it.readBytes() }
    }.getOrNull()

    /**
     * Decodes to roughly the size it will be drawn at. A 500 by 500 cover is a megabyte of pixels; a row
     * forty-two points tall needs a fraction of that, and decoding small is what keeps a long list affordable.
     */
    private fun decodeScaled(bytes: ByteArray, targetPx: Int): ImageBitmap? = runCatching {
        val decoded = SkiaImage.makeFromEncoded(bytes)
        val longestEdge = max(decoded.width, decoded.height)
        if (longestEdge <= targetPx) return@runCatching decoded.toComposeImageBitmap()
        val scale = targetPx.toFloat() / longestEdge
        val width = (decoded.width * scale).toInt().coerceAtLeast(1)
        val height = (decoded.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(width, height)
        surface.canvas.drawImageRect(
            decoded,
            Rect.makeWH(decoded.width.toFloat(), decoded.height.toFloat()),
            Rect.makeWH(width.toFloat(), height.toFloat()),
        )
        surface.makeImageSnapshot().toComposeImageBitmap()
    }.getOrNull()
}

/**
 * Rounds a requested size up to one of a few buckets.
 *
 * Without this, a handful of pixels' difference between two rows would mean two decodes and two cache entries
 * of the same cover.
 */
internal fun artworkSizeBucket(requestedPx: Int): Int = when {
    requestedPx <= 0 -> 384
    requestedPx <= 64 -> 64
    requestedPx <= 128 -> 128
    requestedPx <= 256 -> 256
    requestedPx <= 512 -> 512
    else -> 1024
}

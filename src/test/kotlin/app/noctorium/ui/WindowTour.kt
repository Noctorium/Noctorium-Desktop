package app.noctorium.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.Destination
import app.noctorium.core.LibraryState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.HomeSection
import app.noctorium.domain.PlaybackContext
import app.noctorium.domain.PlaybackOrigin
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.SearchResults
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playlists.LocalPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.random.Random

/**
 * The whole window, drawn off screen with made-up music in it, a screen at a time.
 *
 * What the Windows skins are looked at with, and what shows the other themes have not moved: the same tour
 * drawn before a change and after it should come out the same, pixel for pixel. The music is invented and
 * its covers are painted here, so nothing on the pictures depends on the network or on anybody's account.
 *
 * The state is put straight into the [AppState]'s own flows, which are private: there is no other way to show
 * a library, a search or a Home without signing in to a service, and this is a picture, not a test of them.
 */
internal class WindowTour(private val state: AppState, private val folder: File) {

    private var time = 0L
    private var scene: ImageComposeScene? = null

    fun open(width: Int = 1280, height: Int = 800, content: @Composable () -> Unit = { NoctoriumApp(state) }) {
        close()
        folder.mkdirs()
        scene = ImageComposeScene(width, height, Density(1f)) { content() }
        settle(12)
    }

    fun close() {
        scene?.close()
        scene = null
    }

    /** Renders a few frames a moment apart, so effects run, covers arrive and nothing is caught mid-change. */
    fun settle(frames: Int = 8): Image {
        val scene = checkNotNull(scene)
        var image = scene.render(time)
        repeat(frames) {
            Thread.sleep(60)
            time += 100_000_000L
            image = scene.render(time)
        }
        return image
    }

    fun capture(name: String, frames: Int = 8, crop: Rect? = null) {
        var image = settle(frames)
        if (crop != null) image = image.cropped(crop)
        File(folder, "$name.png").writeBytes(checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
    }

    fun click(at: Offset) {
        val scene = checkNotNull(scene)
        scene.sendPointerEvent(PointerEventType.Move, at)
        scene.sendPointerEvent(PointerEventType.Press, at)
        scene.sendPointerEvent(PointerEventType.Release, at)
    }

    fun hover(at: Offset) {
        checkNotNull(scene).sendPointerEvent(PointerEventType.Move, at)
    }

    /** A key as the keyboard would send it, with shift held for [shift]. Only ever into this picture. */
    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    fun press(key: Key, char: Char, shift: Boolean = false) {
        val scene = checkNotNull(scene)
        listOf(KeyEventType.KeyDown, KeyEventType.KeyUp).forEach { type ->
            scene.sendKeyEvent(KeyEvent(key, type, codePoint = char.code, isShiftPressed = shift))
        }
    }

    // --- Putting the fixtures in ---

    fun ui(change: (AppUiState) -> AppUiState) {
        val flow = state.privateFlow<AppUiState>("mutableUi")
        flow.value = change(flow.value)
    }

    fun library(change: (LibraryState) -> LibraryState) {
        val flow = state.privateFlow<LibraryState>("mutableLibrary")
        flow.value = change(flow.value)
    }

    /**
     * What the player says it is doing. Said by this device's own engine as well as by the one that routes
     * between engines, because the router passes on whatever the engine says next -- and an idle engine saying
     * so over a paused song reads to the queue as the song having ended, and it moves on.
     */
    fun playback(playback: PlaybackState) {
        val router = AppState::class.java.getDeclaredField("player").apply { isAccessible = true }.get(state)
        val local = router.javaClass.getDeclaredField("local").apply { isAccessible = true }.get(router)
        listOf(local, router).forEach { engine ->
            @Suppress("UNCHECKED_CAST")
            val flow = engine.javaClass.getDeclaredField("mutableState").apply { isAccessible = true }.get(engine) as MutableStateFlow<PlaybackState>
            flow.value = playback
        }
    }

    /** Opens a Settings page by name, the way the playback tools banner does: asked for, then gone to. */
    fun settingsPage(page: String) {
        ui { it.copy(destination = Destination.HOME) }
        settle(2)
        val pages = Class.forName("app.noctorium.ui.SettingsPage")
        val wanted = pages.enumConstants.first { (it as Enum<*>).name == page }
        Class.forName("app.noctorium.ui.NoctoriumAppKt").getDeclaredField("pendingSettingsPage").apply {
            isAccessible = true
            set(null, wanted)
        }
        ui { it.copy(destination = Destination.SETTINGS) }
    }

    /** Everything a tour shows: Home's rows, the library, a playlist, a search and the queue. */
    fun fill() {
        state.queue.playQueue(Fixtures.queue, 2, PlaybackContext(ProviderType.YOUTUBE_MUSIC, PlaybackOrigin.HOME))
        playback(PlaybackState(status = PlaybackStatus.PAUSED, track = Fixtures.queue[2], positionMs = 83_000, durationMs = 245_000))
        ui {
            it.copy(
                recentTracks = Fixtures.recent,
                pinnedTracks = Fixtures.pinned,
                homeSections = Fixtures.home,
                homeLoading = false,
                searchQuery = "night",
                searchResults = SearchResults(tracks = Fixtures.searchTracks, playlists = Fixtures.albums),
                searchLoading = false,
                errorMessage = null,
            )
        }
        library {
            it.copy(
                localPlaylists = Fixtures.localPlaylists,
                playlists = Fixtures.playlists,
                loading = false,
                loaded = true,
                errorMessage = null,
                needsSoundCloudUsername = false,
                loadedAtMillis = System.currentTimeMillis(),
            )
        }
    }

    fun go(destination: Destination) = ui { it.copy(destination = destination) }

    private fun Image.cropped(rect: Rect): Image {
        val surface = Surface.makeRasterN32Premul(rect.width.toInt(), rect.height.toInt())
        surface.canvas.drawImage(this, -rect.left, -rect.top)
        return surface.makeImageSnapshot()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> AppState.privateFlow(name: String): MutableStateFlow<T> =
            AppState::class.java.getDeclaredField(name).apply { isAccessible = true }.get(this) as MutableStateFlow<T>

        /**
         * Compares every picture in [after] with the one of the same name in [before], and writes what it found
         * to [report]: the number of pixels that differ in each, and where they do, a picture of where.
         */
        fun compare(before: File, after: File, report: File): Int {
            var differing = 0
            val lines = mutableListOf<String>()
            after.listFiles { file -> file.extension == "png" && !file.name.startsWith("diff-") }.orEmpty().sortedBy { it.name }.forEach { picture ->
                val earlier = File(before, picture.name)
                if (!earlier.isFile) {
                    lines += "${picture.name}: new"
                    return@forEach
                }
                val a = Image.makeFromEncoded(earlier.readBytes())
                val b = Image.makeFromEncoded(picture.readBytes())
                if (a.width != b.width || a.height != b.height) {
                    lines += "${picture.name}: size changed"
                    differing++
                    return@forEach
                }
                val pa = org.jetbrains.skia.Bitmap.makeFromImage(a)
                val pb = org.jetbrains.skia.Bitmap.makeFromImage(b)
                var count = 0
                val marks = Surface.makeRasterN32Premul(a.width, a.height)
                marks.canvas.drawImage(b, 0f, 0f)
                val red = Paint().apply { color = SkiaColor.makeARGB(255, 255, 0, 0) }
                for (y in 0 until a.height) for (x in 0 until a.width) {
                    if (pa.getColor(x, y) != pb.getColor(x, y)) {
                        count++
                        marks.canvas.drawRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), 1f, 1f), red)
                    }
                }
                lines += "${picture.name}: " + if (count == 0) "same" else "$count pixels differ"
                if (count > 0) {
                    differing++
                    File(report.parentFile, "diff-${picture.name}").writeBytes(
                        checkNotNull(marks.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)).bytes,
                    )
                }
            }
            report.writeText(lines.joinToString("\n") + "\n")
            return differing
        }
    }
}

/**
 * Made-up music for the pictures: invented artists and titles, besides the two the other render checks
 * already use, with covers painted in [covers] rather than fetched.
 */
internal object Fixtures {
    private val covers: File = File(System.getProperty("user.dir"), "build/skin-covers").apply { mkdirs() }

    private val burial = Artist("burial", "Burial", ProviderType.YOUTUBE_MUSIC)
    private val judy = Artist("judy-collins", "Judy Collins", ProviderType.YOUTUBE_MUSIC)
    // Not on Spotify, though the albums below are: a Spotify song in the queue is looked up ahead of being played,
    // and made-up music is never found, which would put a banner saying so over the picture.
    private val nightTram = Artist("night-tram", "Night Tram", ProviderType.YOUTUBE_MUSIC)
    private val lamplighters = Artist("lamplighters", "The Lamplighters", ProviderType.SOUNDCLOUD)
    private val signalBox = Artist("signal-box", "Signal Box", ProviderType.YOUTUBE_MUSIC)
    private val ferry = Artist("morning-ferry", "Morning Ferry", ProviderType.SOUNDCLOUD)
    private val satellites = Artist("paper-satellites", "Paper Satellites", ProviderType.YOUTUBE_MUSIC)

    private fun track(n: Int, title: String, artist: Artist, provider: ProviderType = artist.provider, album: String? = null) = Track(
        provider = provider,
        id = "tour-$n",
        title = title,
        artists = listOf(artist),
        album = album?.let { Album("album-$n", it, listOf(artist), provider) },
        durationMs = 150_000L + n * 13_000L,
        artworkUrl = cover("track-$n", n),
        sourceUrl = "https://example.invalid/track/$n",
    )

    val tracks: List<Track> = listOf(
        track(1, "Archangel", burial, album = "Untrue"),
        track(2, "Amazing Grace", judy, album = "Whales & Nightingales"),
        track(3, "Harbour Lights", nightTram, album = "Harbour Lights"),
        track(4, "Platform Nine", nightTram, album = "Harbour Lights"),
        track(5, "Sodium Lamps", lamplighters),
        track(6, "Under the Viaduct", signalBox, album = "Viaducts"),
        track(7, "Slow Ferry", ferry),
        track(8, "Paper Moonrise", satellites, album = "Orbit of Paper"),
        track(9, "Static on the Line", signalBox, album = "Viaducts"),
        track(10, "Window Seat", nightTram, album = "Harbour Lights"),
        track(11, "Low Tide Radio", ferry),
        track(12, "Antarctica", burial, album = "Untrue"),
    )

    val queue: List<Track> = tracks.take(9)
    val recent: List<Track> = tracks.subList(3, 10)
    val pinned: List<Track> = listOf(tracks[0], tracks[7], tracks[4])
    val searchTracks: List<Track> = listOf(tracks[2], tracks[9], tracks[3], tracks[4], tracks[10], tracks[6], tracks[8])

    val albums: List<Playlist> = listOf(
        Playlist("tour-album-1", "Harbour Lights", ProviderType.SPOTIFY, ownerName = "Night Tram", artworkUrl = cover("album-1", 31), trackCount = 9),
        Playlist("tour-album-2", "Night Shift", ProviderType.SPOTIFY, ownerName = "Signal Box", artworkUrl = cover("album-2", 32), trackCount = 11),
        Playlist("tour-album-3", "Orbit of Paper", ProviderType.SPOTIFY, ownerName = "Paper Satellites", artworkUrl = cover("album-3", 33), trackCount = 8),
        Playlist("tour-album-4", "Night Bus Home", ProviderType.SPOTIFY, ownerName = "The Lamplighters", artworkUrl = cover("album-4", 34), trackCount = 12),
    )

    val home: List<HomeSection> = listOf(
        HomeSection("tour-quick", "Quick picks", ProviderType.YOUTUBE_MUSIC, "From YouTube Music", tracks.take(7)),
        HomeSection("tour-albums", "Albums for you", ProviderType.SPOTIFY, "From Spotify", emptyList(), playlists = albums),
        HomeSection("tour-late", "Late night", ProviderType.SOUNDCLOUD, "From SoundCloud", tracks.drop(5)),
    )

    val playlists: List<Playlist> = listOf(
        Playlist("PLtour1", "Late night drive", ProviderType.YOUTUBE_MUSIC, ownerName = "You", artworkUrl = cover("list-1", 41), trackCount = 24, isPublic = false),
        Playlist("1001", "Rainy Sunday", ProviderType.SOUNDCLOUD, ownerName = "You", artworkUrl = cover("list-2", 42), trackCount = 17, isPublic = true),
        Playlist("tour-list-3", "Kitchen radio", ProviderType.SPOTIFY, ownerName = "You", artworkUrl = cover("list-3", 43), trackCount = 52),
        Playlist("PLtour4", "Running", ProviderType.YOUTUBE_MUSIC, ownerName = "You", artworkUrl = cover("list-4", 44), trackCount = 31, isPublic = true),
        Playlist("1005", "Field recordings", ProviderType.SOUNDCLOUD, ownerName = "You", artworkUrl = cover("list-5", 45), trackCount = 9, isPublic = false),
        Playlist("tour-list-6", "Old favourites", ProviderType.SPOTIFY, ownerName = "You", artworkUrl = cover("list-6", 46), trackCount = 76),
    )

    val localPlaylists: List<LocalPlaylist> = listOf(
        LocalPlaylist("tour-local-1", "Night bus", tracks.take(6)),
        LocalPlaylist("tour-local-2", "For the train", tracks.drop(4).take(5)),
    )

    /** A playlist opened in the library, with its songs in. */
    val openPlaylist: Playlist = playlists[0].copy(tracks = tracks.take(10))

    /**
     * A cover, painted: two colours from [seed] across the square, a disc or bands over them, the way record
     * sleeves are made of a few shapes. Written once and kept, by name.
     */
    private fun cover(name: String, seed: Int): String {
        val file = File(covers, "$name.png")
        if (!file.isFile) {
            val random = Random(seed * 7919)
            val size = 320
            val surface = Surface.makeRasterN32Premul(size, size)
            val canvas = surface.canvas
            fun colour(hue: Float, saturation: Float, value: Float): Int {
                val c = java.awt.Color.HSBtoRGB(hue, saturation, value)
                return c or (0xFF shl 24)
            }
            val hue = random.nextFloat()
            val first = colour(hue, .55f + random.nextFloat() * .35f, .55f + random.nextFloat() * .4f)
            val second = colour((hue + .12f + random.nextFloat() * .3f) % 1f, .5f + random.nextFloat() * .4f, .25f + random.nextFloat() * .5f)
            canvas.drawPaint(
                Paint().apply {
                    shader = Shader.makeLinearGradient(0f, 0f, size.toFloat(), size.toFloat(), intArrayOf(first, second), null, GradientStyle.DEFAULT)
                },
            )
            val ink = colour((hue + .5f) % 1f, .25f + random.nextFloat() * .5f, .9f)
            when (seed % 3) {
                0 -> canvas.drawCircle(size * (.3f + random.nextFloat() * .4f), size * (.3f + random.nextFloat() * .4f), size * (.18f + random.nextFloat() * .14f), Paint().apply { color = ink })
                1 -> repeat(5) { band ->
                    canvas.drawRect(
                        Rect.makeXYWH(0f, size * (.18f + band * .14f), size.toFloat(), size * .05f),
                        Paint().apply { color = ink; alpha = 200 - band * 30 },
                    )
                }
                else -> canvas.drawCircle(
                    size * .5f,
                    size * .5f,
                    size * .3f,
                    Paint().apply { color = ink; mode = PaintMode.STROKE; strokeWidth = size * .06f },
                )
            }
            file.writeBytes(checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)).bytes)
        }
        return file.toURI().toString()
    }
}

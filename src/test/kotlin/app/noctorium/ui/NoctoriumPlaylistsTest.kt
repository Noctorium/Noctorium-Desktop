package app.noctorium.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.desktopAppState
import app.noctorium.playlists.NoctoriumPlaylistFile
import app.noctorium.playlists.PlaylistCloudKind
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Noctorium playlists through the desktop's own state: a synced folder chosen, a Noctorium playlist made, and its
 * file there a moment later -- and a local one, which never leaves. With `-Dnoctorium.renderBars=<folder>` the
 * settings page is drawn as well, for looking at.
 */
class NoctoriumPlaylistsTest {
    private fun waitFor(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < until) {
            if (check()) return
            Thread.sleep(100)
        }
        error("Timed out waiting for $what")
    }

    @Test
    fun `a Noctorium playlist reaches the synced folder, and a local one stays here`() {
        val folder = Files.createTempDirectory("noctorium-playlists-sync")
        val state = desktopAppState()
        try {
            state.setPlaylistCloud(PlaylistCloudKind.FOLDER, folder.toString())
            val shared = state.createPlaylist("Road trip", noctorium = true)!!
            val local = state.createPlaylist("Only here")!!
            val file = folder.resolve(NoctoriumPlaylistFile.FOLDER).resolve(NoctoriumPlaylistFile.fileNameFor(shared.id))
            waitFor("the playlist's file") { Files.isRegularFile(file) }
            assertEquals("Road trip", NoctoriumPlaylistFile.decode(Files.readString(file))!!.title)
            assertTrue(!Files.exists(file.resolveSibling(NoctoriumPlaylistFile.fileNameFor(local.id))), "a local playlist never leaves")

            // Another device renames it: the next sync brings the new name here.
            val renamed = NoctoriumPlaylistFile.decode(Files.readString(file))!!.copy(title = "Road trip 2026", updatedAtMillis = System.currentTimeMillis() + 1_000)
            Files.writeString(file, NoctoriumPlaylistFile.encode(renamed))
            state.syncPlaylistsNow()
            waitFor("the new name") { state.library.value.localPlaylists.any { it.title == "Road trip 2026" } }

            // Deleted here: deleted there too, as a file saying so.
            state.deletePlaylist(shared.id)
            waitFor("the deletion") { NoctoriumPlaylistFile.decode(Files.readString(file))?.deleted == true }

            draw(state)
        } finally {
            state.setPlaylistCloud(PlaylistCloudKind.NONE)
            state.library.value.localPlaylists.forEach { state.deletePlaylist(it.id) }
            Thread.sleep(300)
            state.close()
        }
    }

    private fun draw(state: app.noctorium.core.AppState) {
        val out = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        out.mkdirs()
        val scene = ImageComposeScene(900, 760, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    androidx.compose.foundation.layout.Box(Modifier.padding(24.dp)) { NoctoriumPlaylistsSettingsPanel(preferences, state) }
                }
            }
        }
        try {
            var image = scene.render(0)
            repeat(10) { image = scene.render((it + 1) * 100_000_000L) }
            File(out, "noctorium-playlists-settings.png").writeBytes(image.encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }
}

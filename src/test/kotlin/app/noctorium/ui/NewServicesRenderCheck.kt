package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.settings.AppleMusicConnectionState
import app.noctorium.settings.DeezerConnectionState
import app.noctorium.settings.SettingsState
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.TidalConnectionState
import app.noctorium.ui.skins.Surface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws Apple Music's, Deezer's and Tidal's pages in Settings to pictures, off screen, in each of their states, in
 * Night, 98 and XP; then the whole window at each of those pages and at the list of Settings with the new tiles. Off
 * unless a folder is named, like [PlayerBarRenderCheck]; the pictures go to `new-services/` under it:
 *
 *     ./gradlew :test --tests "*NewServicesRenderCheck*" -Dnoctorium.renderBars=build/bars --no-configuration-cache
 *
 * The states are put straight into what the pages are drawn from: nothing here signs in to any service, looks a
 * profile up or registers an app, and every name and message is made up.
 */
class NewServicesRenderCheck {

    private val themes = listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_XP)

    /** Each page in the states worth seeing, by the name its picture is given. */
    private fun pages(base: SettingsState): List<Triple<String, SettingsPage, SettingsState>> = listOf(
        Triple("apple-music-signed-out", SettingsPage.APPLE_MUSIC, base.copy(appleMusic = AppleMusicConnectionState(storefront = "gb"))),
        Triple(
            "apple-music-turned-down",
            SettingsPage.APPLE_MUSIC,
            base.copy(appleMusic = AppleMusicConnectionState(storefront = "gb", message = "Apple Music did not accept that sign-in. Sign in again on Apple's page.")),
        ),
        Triple("apple-music-checking", SettingsPage.APPLE_MUSIC, base.copy(appleMusic = AppleMusicConnectionState(checking = true, storefront = "gb"))),
        Triple(
            "apple-music-signed-in",
            SettingsPage.APPLE_MUSIC,
            base.copy(
                appleMusic = AppleMusicConnectionState(
                    connected = true,
                    storefront = "gb",
                    ownDeveloperToken = true,
                    message = "Signed in to Apple Music. Your playlists and songs are in the library, and play matched on YouTube Music.",
                ),
            ),
        ),
        Triple("deezer-none", SettingsPage.DEEZER, base.copy(deezer = DeezerConnectionState())),
        Triple("deezer-checking", SettingsPage.DEEZER, base.copy(deezer = DeezerConnectionState(checking = true))),
        Triple(
            "deezer-found",
            SettingsPage.DEEZER,
            base.copy(
                deezer = DeezerConnectionState(
                    profileId = "1234567",
                    found = true,
                    name = "Sample Listener",
                    message = "Showing Sample Listener's playlists, loved tracks and albums from Deezer.",
                ),
            ),
        ),
        Triple(
            "deezer-not-found",
            SettingsPage.DEEZER,
            base.copy(deezer = DeezerConnectionState(message = "Deezer has no profile numbered 99. It is the number at the end of your profile's address.")),
        ),
        Triple("tidal-no-app", SettingsPage.TIDAL, base.copy(tidal = TidalConnectionState())),
        Triple(
            "tidal-app-saved",
            SettingsPage.TIDAL,
            base.copy(
                tidal = TidalConnectionState(
                    configured = true,
                    ownApp = true,
                    message = "Your Tidal app is saved. Connect Tidal to sign in through it; add its secret to search Tidal without signing in.",
                ),
            ),
        ),
        Triple(
            "tidal-connecting",
            SettingsPage.TIDAL,
            base.copy(tidal = TidalConnectionState(configured = true, ownApp = true, connecting = true, message = "Finish signing in to Tidal in your browser.")),
        ),
        Triple(
            "tidal-connected",
            SettingsPage.TIDAL,
            base.copy(
                tidal = TidalConnectionState(
                    configured = true,
                    ownApp = true,
                    readsCatalogue = true,
                    connected = true,
                    accountName = "Sample Listener",
                    message = "Tidal connected as Sample Listener. Your collection is in the library.",
                ),
            ),
        ),
    )

    @Test
    fun `the new pages draw, in Night, 98 and XP`() {
        val root = System.getProperty("noctorium.renderBars")?.let { File(it, "new-services") } ?: return
        val state = desktopAppState()
        try {
            state.setAnimations(false)
            val base = state.settings.value.copy(message = null)
            // Day as well here, for the pages on a light theme.
            (themes + ThemePreset.NOCTORIUM_DAY).forEach { theme ->
                val folder = File(root, theme.name.lowercase()).also { it.mkdirs() }
                pages(base).forEach { (name, page, settings) ->
                    draw(File(folder, "$name.png"), theme, 820, 1500) { SettingsPageContent(page, settings, state) }
                }
            }
        } finally {
            state.setAnimations(true)
            Thread.sleep(300)
            state.close()
        }
    }

    @Test
    fun `the whole window at each new page, and the list of Settings with the new tiles`() {
        val root = System.getProperty("noctorium.renderBars")?.let { File(it, "new-services") } ?: return
        val state = desktopAppState()
        try {
            state.setAnimations(false)
            state.setConnectEnabled(false)
            state.setUpdateCheckOnLaunch(false)
            themes.forEach { theme ->
                state.setTheme(theme)
                val tour = WindowTour(state, File(root, "window-${theme.name.lowercase()}"))
                try {
                    // Tall enough for the Services tiles, far down the list.
                    tour.open(height = 1500)
                    tour.settings {
                        it.copy(
                            message = null,
                            deezer = DeezerConnectionState(profileId = "1234567", found = true, name = "Sample Listener"),
                            appleMusic = AppleMusicConnectionState(storefront = "gb"),
                            tidal = TidalConnectionState(),
                        )
                    }
                    tour.ui { it.copy(destination = Destination.SETTINGS) }
                    tour.capture("settings-home", frames = 10)
                    tour.open()
                    listOf("APPLE_MUSIC", "DEEZER", "TIDAL").forEach { page ->
                        tour.settingsPage(page)
                        tour.capture("settings-${page.lowercase()}")
                    }
                } finally {
                    tour.close()
                }
            }
        } finally {
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setAnimations(true)
            state.setConnectEnabled(true)
            state.setUpdateCheckOnLaunch(true)
            Thread.sleep(300)
            state.close()
        }
    }

    /** [content] in [theme]'s colours, skin and type, on its page colour, a few frames in. */
    private fun draw(file: File, theme: ThemePreset, width: Int, height: Int, content: @androidx.compose.runtime.Composable () -> Unit) {
        val scene = ImageComposeScene(width, height, Density(1f)) {
            BadgeTheme(theme) {
                CompositionLocalProvider(LocalMotion provides false) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.padding(20.dp)) { content() }
                    }
                }
            }
        }
        try {
            var image = scene.render(0)
            repeat(4) { frame -> image = scene.render((frame + 1) * 100_000_000L) }
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for ${file.name}")
        } finally {
            scene.close()
        }
    }
}

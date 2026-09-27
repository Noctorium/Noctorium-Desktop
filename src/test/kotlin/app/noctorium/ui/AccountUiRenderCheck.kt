package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.desktopAppState
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.social.YouTubeChannel
import java.io.File
import kotlin.test.Test

/**
 * Draws the YouTube Music account picker to a picture, off screen, for looking at: two Google accounts in
 * one session, the second with a brand channel, as the account switcher lists them. Off unless a folder is
 * named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*AccountUiRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class AccountUiRenderCheck {

    @Test
    fun `the account picker draws`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val channels = listOf(
            YouTubeChannel("", "Cem", authUser = 0, handle = "@cem", email = "first@example.com", selected = true),
            YouTubeChannel("", "Work", authUser = 1, email = "second@example.com"),
            YouTubeChannel("109876543210", "Night Shift Radio", authUser = 1, handle = "@nightshift", email = "second@example.com"),
        )
        val state = desktopAppState()
        try {
            val scene = ImageComposeScene(620, 520, Density(1f)) {
                val settings = state.settings.collectAsState().value
                val preferences = settings.preferences
                MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
                        YouTubeChannelChooser(channels, settings, state)
                        Column(Modifier.padding(top = 18.dp)) { YouTubeHistorySetting(settings, state) }
                    }
                }
            }
            try {
                File(folder, "account-picker.png").writeBytes(scene.render().encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        } finally {
            state.close()
        }
    }
}

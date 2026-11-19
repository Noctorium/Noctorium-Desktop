package app.noctorium.ui

import app.noctorium.desktopAppState
import app.noctorium.settings.SetupStep
import app.noctorium.settings.ThemePreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The first-time setup, a picture of each step, and the window's pages once it is finished. Off unless a folder is
 * named:
 *
 *     ./gradlew :test --tests "*FirstTimeSetupRenderCheck*" -Dnoctorium.renderSetup=build/setup
 */
class FirstTimeSetupRenderCheck {

    @Test
    fun `each step of the setup draws, and finishing it shows the window`() {
        val root = System.getProperty("noctorium.renderSetup")?.let(::File) ?: return
        val state = desktopAppState()
        val tour = WindowTour(state, root)
        val wasDone = state.settings.value.preferences.setupDone
        try {
            state.setAnimations(false)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            tour.settings { it.copy(preferences = it.preferences.copy(setupDone = false)) }
            SetupStep.entries.forEach { step ->
                setupStartsAt = step
                tour.open(1100, 760)
                tour.capture("setup-${step.ordinal + 1}-${step.name.lowercase()}", frames = 12)
            }
            setupStartsAt = SetupStep.WELCOME
            state.finishSetup()
            assertTrue(state.settings.value.preferences.setupDone)
            tour.fill()
            tour.capture("after-setup", frames = 12)
        } finally {
            setupStartsAt = SetupStep.WELCOME
            if (!wasDone) tour.settings { it.copy(preferences = it.preferences.copy(setupDone = false)) }
            tour.close()
            state.close()
        }
    }
}

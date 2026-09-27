package app.noctorium.core

import app.noctorium.desktopAppState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Starting the whole application state, as the desktop does, throws nothing in the background.
 *
 * The watchers started while AppState is being built run on their own threads, so a mistake in one does not
 * fail the build of the object -- it fails later, on a worker thread, where nothing notices. One did: a watcher
 * reached for a field declared further down the class than the code that started it, found it still null,
 * and died the moment the desktop opened. This catches that whole kind of mistake by listening for it.
 */
class AppStartupTest {

    @Test
    fun `nothing started at launch dies on a background thread`() {
        val thrown = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> thrown += error }
        val state = desktopAppState()
        try {
            Thread.sleep(3_000)
        } finally {
            state.close()
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
        assertTrue(thrown.isEmpty(), "died in the background: " + thrown.joinToString { it.toString() })
    }
}

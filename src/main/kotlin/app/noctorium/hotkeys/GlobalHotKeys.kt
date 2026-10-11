package app.noctorium.hotkeys

import app.noctorium.settings.KeyChord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/*
 * Shortcuts that work from any program: the listener presses Ctrl+Alt+Space in a text editor and Noctorium's
 * quick search comes up.
 *
 * Always through the system's own hot-key service -- RegisterHotKey on Windows, the Carbon hot keys on a Mac,
 * a passive key grab under X11, the GlobalShortcuts portal on Wayland -- and never a keyboard hook. A hook sees
 * every key the machine types, passwords included, which is not something a music player has any business
 * doing; a hot key is one chord, asked for by name, and the system hands over that chord and nothing else.
 *
 * It also means a chord can be refused: whoever asked for it first has it. That is reported as taken, and left
 * alone, rather than fought over.
 */

/** What became of one chord asked for. */
sealed interface HotKeyResult {
    /** The system gave it to Noctorium: pressing it anywhere arrives here. */
    data object Registered : HotKeyResult

    /** Asked for, and waiting on the desktop -- the portal asks the listener first. */
    data object Pending : HotKeyResult

    /** Another program already holds it. */
    data object Taken : HotKeyResult

    /** This system has no way to take this key at all: a key the Mac's hot keys cannot name, say. */
    data class Unsupported(val reason: String) : HotKeyResult

    /** Something went wrong asking. */
    data class Failed(val reason: String) : HotKeyResult
}

/**
 * One system's hot keys. [bind] makes the set held exactly what is wanted -- letting go of what is no longer
 * wanted and asking for what is new -- and says what became of each. A press of one arrives through the
 * callback the backend was made with, as its number, on a thread of the backend's own.
 *
 * Closing lets go of every one of them.
 */
interface HotKeyBackend : AutoCloseable {
    fun bind(wanted: Map<Int, KeyChord>, names: Map<Int, String> = emptyMap()): Map<Int, HotKeyResult>
}

/** Whether this desktop can have global shortcuts at all, and how. */
sealed interface HotKeySupport {
    /** [how] says what is used, for the page to say so; [create] starts it, with what to do on a press. */
    data class Available(val how: String, val create: (onPressed: (Int) -> Unit) -> HotKeyBackend) : HotKeySupport

    /** Not here, and why, in words for the listener. */
    data class Unavailable(val reason: String) : HotKeySupport
}

/** What the global shortcuts are doing now, for the Shortcuts page to show. */
data class GlobalHotKeysState(
    /** How they are registered here, or null where they cannot be. */
    val how: String? = null,
    /** Why they cannot be, where they cannot. */
    val unavailable: String? = null,
    /** Whether they are switched on. */
    val enabled: Boolean = false,
    /** What became of each one bound, by action. Empty while they are off. */
    val results: Map<String, HotKeyResult> = emptyMap(),
) {
    val supported: Boolean get() = unavailable == null
}

/**
 * The global shortcuts: told what the listener wants with [apply], it asks the system for those chords and lets
 * go of the rest, and hands every press to [onPressed] by the action's name.
 *
 * Nothing is asked of the system while they are switched off or none is bound -- not even a thread is started --
 * and switching them off, or [close], gives every chord back. The asking happens on [worker], off the interface's
 * thread, because a portal may wait on the listener and a Mac on its run loop; [state] says how it went.
 */
class GlobalHotKeys(
    private val support: HotKeySupport,
    private val onPressed: (String) -> Unit,
    /** What each action is called, for a desktop that lists the shortcuts it granted -- the portal does. */
    private val title: (String) -> String = { it },
    private val worker: Executor = defaultWorker(),
) : AutoCloseable {
    private val mutableState = MutableStateFlow(
        when (support) {
            is HotKeySupport.Available -> GlobalHotKeysState(how = support.how)
            is HotKeySupport.Unavailable -> GlobalHotKeysState(unavailable = support.reason)
        },
    )
    val state: StateFlow<GlobalHotKeysState> = mutableState.asStateFlow()

    // Only ever touched on the worker.
    private var backend: HotKeyBackend? = null
    private val numbers = LinkedHashMap<String, Int>()
    @Volatile private var actions: Map<Int, String> = emptyMap()
    @Volatile private var closed = false

    /**
     * Holds exactly [bindings] -- an action's name and its chord -- while [enabled], and nothing otherwise. Two
     * actions on one chord cannot both have it; the first keeps it, and the page shows the clash.
     */
    fun apply(enabled: Boolean, bindings: Map<String, KeyChord>) {
        if (closed) return
        val wanted = if (enabled) bindings.entries.distinctBy { it.value }.associate { it.key to it.value } else emptyMap()
        mutableState.update { it.copy(enabled = enabled, results = if (it.supported) wanted.mapValues { HotKeyResult.Pending } else emptyMap()) }
        if (support !is HotKeySupport.Available) return
        worker.execute { bindNow(support, wanted) }
    }

    private fun bindNow(support: HotKeySupport.Available, wanted: Map<String, KeyChord>) {
        if (closed) return
        if (wanted.isEmpty()) {
            // Off, or nothing bound: let go of everything, down to the thread that held it.
            backend?.let { runCatching { it.close() } }
            backend = null
            mutableState.update { it.copy(results = emptyMap()) }
            return
        }
        val held = backend ?: runCatching { support.create { number -> actions[number]?.let(onPressed) } }
            .onFailure { failure ->
                mutableState.update { state -> state.copy(results = wanted.mapValues { HotKeyResult.Failed(failure.message ?: "Could not start") }) }
            }
            .getOrNull()
            ?.also { backend = it }
            ?: return
        val byNumber = wanted.mapKeys { (action, _) -> numbers.getOrPut(action) { numbers.size + 1 } }
        actions = numbers.entries.associate { (action, number) -> number to action }
        val names = byNumber.keys.associateWith { number -> title(actions.getValue(number)) }
        val results = runCatching { held.bind(byNumber, names) }.getOrElse { failure ->
            byNumber.mapValues { HotKeyResult.Failed(failure.message ?: failure.javaClass.simpleName) }
        }
        mutableState.update { state ->
            state.copy(results = wanted.keys.associateWith { action -> results[numbers.getValue(action)] ?: HotKeyResult.Failed("No answer") })
        }
    }

    /** Gives every chord back to the system. Called when Noctorium quits. */
    override fun close() {
        if (closed) return
        closed = true
        worker.execute {
            backend?.let { runCatching { it.close() } }
            backend = null
        }
        // Waited for, briefly: the system would take them back when the process ends anyway, but a chord held
        // by a program that is busy quitting is a chord that does nothing in the meantime.
        (worker as? ExecutorService)?.let { executor ->
            executor.shutdown()
            runCatching { executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS) }
        }
    }

    companion object {
        private fun defaultWorker(): ExecutorService = Executors.newSingleThreadExecutor { task ->
            Thread(task, "noctorium-global-shortcuts-setup").apply { isDaemon = true }
        }
    }
}

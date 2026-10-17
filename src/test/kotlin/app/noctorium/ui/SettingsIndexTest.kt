package app.noctorium.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import app.noctorium.core.AppState
import app.noctorium.desktopAppState
import app.noctorium.domain.inBeta
import app.noctorium.settings.AccentPreset
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.SettingMatchKind
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The settings search's index against the pages themselves.
 *
 * Every page is drawn, off screen, in the states that show the most of it, and each setting it draws with the
 * shared controls -- or marks with [SettingAnchor] -- is checked for an entry in [SettingsIndex]. So a setting
 * added to a page cannot be left out of the search: this fails, naming it, until it has an entry. And each entry
 * is checked the other way, for a setting by that name on its page, so an entry cannot outlive its setting.
 */
class SettingsIndexTest {

    private val made = lazy { desktopAppState() }
    private val state: AppState by made

    @AfterTest
    fun close() {
        if (!made.isInitialized()) return
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAccent(AccentPreset.THEME)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.close()
    }

    /** Every setting each page draws, by page, in the state the settings are in now. */
    private fun drawn(): Map<SettingsPage, Set<String>> = SettingsPage.entries.associateWith { page ->
        val names = linkedSetOf<String>()
        val scene = ImageComposeScene(1100, 6000, Density(1f)) {
            val settings = state.settings.collectAsState().value
            val preferences = settings.preferences
            MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                Surface(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalSettingsAnchors provides SettingsAnchors(record = { names += it })) {
                        SettingsPageContent(page, settings, state)
                    }
                }
            }
        }
        try {
            scene.render()
            scene.render()
        } finally {
            scene.close()
        }
        names
    }

    @Test
    fun `every setting on every page has an entry in the index, and every entry a setting`() {
        val seen = SettingsPage.entries.associateWith { mutableSetOf<String>() }
        fun look() = drawn().forEach { (page, names) -> seen.getValue(page) += names }
        look()
        // The settings that only show once something above them is chosen: the clock with the Taskbar, the six
        // colours with the Custom theme, the picker with an accent of your own.
        state.setPlayerBarStyle(PlayerBarStyle.TASKBAR)
        state.setTheme(ThemePreset.CUSTOM)
        state.setAccent(AccentPreset.CUSTOM)
        look()

        val missing = seen.flatMap { (page, names) ->
            names.filter { name -> SettingsIndex.settings.none { it.page == page && it.anchor == name } }.map { "${page.name}: \"$it\"" }
        }
        if (missing.isNotEmpty()) {
            fail("These settings are on their pages but not in SettingsIndex, so the search cannot find them:\n" + missing.joinToString("\n"))
        }
        // Those only there once signed in, or in an installed copy, say so in the index and are taken at their word.
        val stale = SettingsIndex.settings.filter { it.anchor != null && it.onlyWhen == null && it.anchor !in seen.getValue(it.page) }
            .map { "${it.page.name}: \"${it.entry.title}\" at \"${it.anchor}\"" }
        if (stale.isNotEmpty()) {
            fail("These entries in SettingsIndex point at settings their pages do not draw:\n" + stale.joinToString("\n"))
        }
    }

    @Test
    fun `every page has its settings in the index, and no two entries share a name`() {
        SettingsPage.entries.forEach { page ->
            assertTrue(SettingsIndex.settings.any { it.page == page }, "$page has nothing in the index")
        }
        val ids = SettingsIndex.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "two entries with one name: " + ids.groupBy { it }.filterValues { it.size > 1 }.keys)
        SettingsIndex.settings.forEach { setting ->
            assertEquals(setting.where, setting.entry.section)
            assertTrue(setting.entry.description.isNotBlank(), "${setting.entry.title} says nothing about what it does")
        }
    }

    private fun first(query: String) = SettingsIndex.search(query).firstOrNull()?.setting
        ?: fail("Nothing found for \"$query\"")

    @Test
    fun `what people call a setting finds it, as well as its name`() {
        assertEquals("Theme", first("dark mode").entry.title)
        assertEquals("Theme", first("light theme").entry.title)
        assertEquals("Volume booster", first("louder").entry.title)
        assertEquals("Volume booster", first("boost").entry.title)
        assertEquals(SettingsPage.SHORTCUTS, first("hotkeys").page)
        assertEquals(SettingsPage.SHORTCUTS, first("keyboard").page)
        assertEquals("Equaliser", first("eq").entry.title)
        assertEquals("Equaliser", first("equalizer").entry.title)
        assertEquals("Equaliser", first("equaliser").entry.title)
        assertEquals("Accent", first("accent color").entry.title)
        assertEquals("Skip the parts of a YouTube video that are not the music", first("sponsorblock").entry.title)
        assertEquals(SettingsPage.STARTUP, first("start with windows").page)
        assertEquals("Text size", first("font size").entry.title)
        assertEquals("When you close Noctorium", first("minimize to tray").entry.title)
        assertEquals("Quick search", first("ctrl+f").entry.title)
        assertEquals(SettingsPage.SCROBBLING, first("last.fm").page)
        assertEquals("Libre.fm", first("libre.fm").entry.title)
        assertEquals("Libre.fm", first("libre").entry.title)
        assertEquals("Last.fm", first("browser did not open").entry.title)
        // The history on the profile page, before Home's row of the songs played lately and the YouTube history.
        assertEquals("History", first("history").entry.title)
        assertEquals("History", first("recently played").entry.title)
        assertEquals("History", first("listening history").entry.title)
        assertEquals(SettingsPage.PROFILE, first("clear history").page)
    }

    @Test
    fun `the Glow themes are found by their names, the family's, and the black they are`() {
        listOf("glow", "afterglow", "cinder", "velvet", "smoke").forEach { word ->
            assertEquals("Theme", first(word).entry.title, "\"$word\" does not find the themes first")
        }
        // After Backdrop, whose name "black" begins with a letter forgiven, which the search puts before a word.
        assertTrue(SettingsIndex.search("black").any { it.setting.entry.title == "Theme" }, "\"black\" does not find the themes")
    }

    @Test
    fun `a name is found before a description, and a typo in a long word is forgiven`() {
        val found = SettingsIndex.search("autoplay")
        assertEquals("Autoplay", found.first().setting.entry.title)
        assertEquals(SettingMatchKind.TITLE_START, found.first().match.kind)
        assertEquals("Equaliser", first("equaliserr").entry.title, "one letter too many")
        assertEquals("Animations", first("animatoins").entry.title, "two letters the wrong way round")
        assertTrue(SettingsIndex.search("").isEmpty())
        assertTrue(SettingsIndex.search("zzzz qqqq").isEmpty())
    }

    @Test
    fun `every shortcut can be found by what it does, on the Shortcuts page`() {
        ShortcutAction.entries.forEach { action ->
            assertTrue(
                SettingsIndex.settings.any { it.page == SettingsPage.SHORTCUTS && it.anchor == action.title },
                "${action.title} cannot be found",
            )
        }
        assertEquals(SettingsPage.SHORTCUTS, first("stop after this song").page)
    }

    @Test
    fun `the badges' marks are found by what people call them, and YTM still finds YouTube Music first`() {
        listOf("logo", "service icons", "badge style", "marks").forEach { query ->
            assertEquals(SERVICE_BADGE_STYLE, first(query).entry.title, query)
        }
        listOf("ytm", "icons", "badges", "letters").forEach { query ->
            assertTrue(SettingsIndex.search(query).any { it.setting.entry.title == SERVICE_BADGE_STYLE }, "\"$query\" does not find the badges")
        }
        assertEquals("YouTube Music account", first("ytm").entry.title)
        assertEquals("Icons", first("icons").entry.title)
        assertEquals(SettingsPage.THEME, first("logo").page)
    }

    @Test
    fun `Apple Music, Deezer and Tidal are found by name, and what each needs by what it is called`() {
        assertEquals(SettingsPage.APPLE_MUSIC, first("apple music").page)
        assertEquals(SettingsPage.APPLE_MUSIC, first("itunes").page)
        assertEquals(SettingsPage.APPLE_MUSIC, first("media-user-token").page)
        assertEquals(SettingsPage.DEEZER, first("deezer").page)
        assertEquals("Your Deezer profile", first("deezer profile").entry.title)
        assertEquals(SettingsPage.TIDAL, first("tidal").page)

        // Apple's developer token, folded away on its page, is found and opened at its fold.
        val token = first("developer token")
        assertEquals(SettingsPage.APPLE_MUSIC, token.page)
        assertEquals(APPLE_DEVELOPER_TOKEN, token.anchor)
        assertEquals(SettingsPage.APPLE_MUSIC, first("musickit").page)

        // A client id finds Tidal's first, the service that cannot go without one -- and Spotify's still.
        val clientId = SettingsIndex.search("client id")
        assertEquals(SettingsPage.TIDAL, clientId.first().setting.page)
        assertEquals("Your Tidal app", clientId.first().setting.anchor)
        assertTrue(clientId.any { it.setting.page == SettingsPage.SPOTIFY }, "\"client id\" no longer finds Spotify's own app")
        assertEquals(SettingsPage.TIDAL, first("client secret").page)
        assertEquals(SettingsPage.TIDAL, first("developer.tidal.com").page)
    }

    @Test
    fun `what a listener without a working Spotify sign-in types finds Spotify's two ways in without one`() {
        listOf("spotify public playlist", "public playlist", "follow playlist", "without premium", "keep in library").forEach { query ->
            assertEquals(SPOTIFY_PUBLIC_PLAYLISTS, first(query).anchor, query)
        }
        listOf("import spotify", "spotify data", "my_spotify_data", "liked songs", "download your data", "account data").forEach { query ->
            assertEquals(SPOTIFY_DATA_IMPORT, first(query).anchor, query)
        }
        listOf("spotify public playlist", "import spotify", "spotify data", "liked songs", "without premium", "my_spotify_data").forEach { query ->
            assertEquals(SettingsPage.SPOTIFY, first(query).page, query)
        }
        // Both, for somebody who only knows Premium is what they lack; and Spotify's own page still first for its name.
        val noPremium = SettingsIndex.search("without premium").map { it.setting.anchor }
        assertTrue(SPOTIFY_DATA_IMPORT in noPremium, "\"without premium\" does not find the import")
        assertEquals(SettingsPage.SPOTIFY, first("spotify").page)
        assertEquals(null, first("spotify").anchor)
    }

    @Test
    fun `beta finds every service whose page warns that it is in beta, and nothing else`() {
        val found = SettingsIndex.search("beta").map { it.setting.page }.toSet()
        assertEquals(SettingsPage.entries.filter { it.service?.inBeta == true }.toSet(), found)
    }

    @Test
    fun `the Connect page is found from Settings, and opens itself rather than a page of Settings`() {
        val devices = first("devices")
        assertEquals("Devices on this network", devices.entry.title)
        assertEquals(app.noctorium.core.Destination.CONNECT, devices.destination)
        assertEquals("Connect", devices.where)
        assertEquals(app.noctorium.core.Destination.CONNECT, first("connect").destination)
        assertEquals(app.noctorium.core.Destination.CONNECT, first("cast").destination)
        assertEquals("Not finding a device?", first("firewall").entry.title)
        assertEquals("Not finding a device?", first("windows firewall").entry.title)
        assertEquals("Add a device by its address", first("ip address").entry.title)
        assertEquals("Play here, or play on another device", first("play on my phone").entry.title)
        assertEquals("Who is controlling whom", first("controlled from").entry.title)
        // Its settings are where they were: naming this computer, and whether it can be found.
        assertEquals("Name this device", first("device name").entry.title)
        assertEquals("Noctorium Connect on or off", first("discoverable").entry.title)

        var opened: Pair<SettingsPage, String?>? = null
        var went: app.noctorium.core.Destination? = null
        devices.openWith({ page, anchor -> opened = page to anchor }, { went = it })
        assertEquals(app.noctorium.core.Destination.CONNECT, went)
        assertEquals(null, opened)
        first("discoverable").openWith({ page, anchor -> opened = page to anchor }, { went = null })
        assertEquals(SettingsPage.ACCOUNT to "Noctorium Connect", opened)
    }

    @Test
    fun `what a streamer types finds the stream widgets, and what they share words with is found as before`() {
        listOf("stream", "obs", "streamlabs", "widget", "overlay", "browser source", "now playing overlay", "twitch").forEach { query ->
            assertEquals(SettingsPage.STREAM_WIDGETS, first(query).page, query)
        }
        // The page itself for its name, at its top.
        assertEquals("Stream widgets", first("stream").entry.title)
        assertEquals(null, first("stream widgets").anchor)
        assertEquals("Adding it to OBS", first("obs").entry.title)
        assertEquals("Adding it to OBS", first("refresh browser when scene becomes active").entry.title)
        assertEquals(STREAM_WIDGET_PORT, first("widget port").anchor)
        assertEquals(STREAM_WIDGET_OVERRIDES, first("url parameters").anchor)
        assertEquals("Widget looks", first("afterglow").entry.title)
        assertEquals("Allow other devices on the network", first("second pc").entry.title)
        // Its options say what they are without taking words the other pages are found by.
        assertEquals("Seek bar", first("progress bar").entry.title)
        assertEquals("Cover", first("cover").entry.title)
        assertEquals(SettingsPage.THEME, first("windows 98").page)
    }
}

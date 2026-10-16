package app.noctorium.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import app.noctorium.ui.icons.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.domain.ProviderType
import app.noctorium.playback.PlaybackToolInstaller
import app.noctorium.settings.AccountConnectionStatus
import app.noctorium.settings.ScrobbleConnectionStatus
import app.noctorium.settings.SettingsState
import app.noctorium.settings.nowPlayingBackdrop
import app.noctorium.ui.skins.BrowsingPane
import app.noctorium.ui.skins.ControlPanelItem
import app.noctorium.ui.skins.HeadingRule
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.InfoBar
import app.noctorium.ui.skins.ScreenTitle
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.classicScrollbar
import app.noctorium.ui.skins.scrollingPage
import app.noctorium.ui.skins.skinned
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.Luna
import app.noctorium.settings.ThemeSkin
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

/*
 * Settings: a home with every page on it, grouped into what each is about, and a search box above them that finds
 * any one setting by name, by what it does or by what people call it, and goes straight to it.
 */

/** The groups the pages of Settings come in, in the order the home shows them. */
internal enum class SettingsSection(val title: String, val description: String) {
    YOU("You", "Your account and your profile"),
    LOOK("Look", "Colours, type, the player bar, now playing and Home"),
    LISTENING("Listening", "How the music sounds, and how it goes on"),
    SERVICES("Services", "Where the music comes from, and who hears about it"),
    DEVICE("This device", "This computer: starting up, the keyboard, files and the tools that play"),
    ABOUT("About", "Noctorium itself"),
}

/**
 * Every page of Settings, in its section.
 *
 * Customization used to be one page, so long that a setting near its foot was a minute's scrolling away; it is
 * the five pages of Look now. [title] heads the page and names it in the search's results.
 */
internal enum class SettingsPage(val title: String, val section: SettingsSection) {
    ACCOUNT("Noctorium account", SettingsSection.YOU),
    PROFILE("Your Noctorium profile", SettingsSection.YOU),
    THEME("Theme and colours", SettingsSection.LOOK),
    TEXT("Typeface and text", SettingsSection.LOOK),
    PLAYER_BAR("Player bar", SettingsSection.LOOK),
    NOW_PLAYING("Now playing", SettingsSection.LOOK),
    BROWSING("Home and browsing", SettingsSection.LOOK),
    SOUND("Sound", SettingsSection.LISTENING),
    PLAYBACK_QUEUE("Playback and queue", SettingsSection.LISTENING),
    LYRICS("Lyrics providers", SettingsSection.LISTENING),
    YOUTUBE("YouTube Music account", SettingsSection.SERVICES),
    SOUNDCLOUD("SoundCloud account", SettingsSection.SERVICES),
    SPOTIFY("Spotify library", SettingsSection.SERVICES),
    BANDCAMP("Bandcamp", SettingsSection.SERVICES),
    VK("VK Music", SettingsSection.SERVICES),
    SCROBBLING("Scrobbling", SettingsSection.SERVICES),
    DISCORD("Discord Rich Presence", SettingsSection.SERVICES),
    STARTUP("Startup and tray", SettingsSection.DEVICE),
    SHORTCUTS("Keyboard shortcuts", SettingsSection.DEVICE),
    DOWNLOADS("Saving music", SettingsSection.DEVICE),
    PLAYBACK_TOOLS("Playback tools", SettingsSection.DEVICE),
    UPDATES("Updates", SettingsSection.ABOUT),
    DIAGNOSTICS("Diagnostics", SettingsSection.ABOUT),
    ;

    /** Where it lives, as the search shows it: "Look › Player bar". */
    val path: String get() = "${section.title} › $title"
}

/** A page something elsewhere has asked for and the setting on it to show -- or, with no page, a search to show. */
private data class SettingsRequest(val page: SettingsPage?, val anchor: String?, val query: String? = null)

/**
 * A settings page something elsewhere has asked for.
 *
 * Which page is open is local to [SettingsScreen] and should stay that way -- but the banner shown when
 * mpv is missing has to be able to land on the page that fixes it, rather than on the settings list with
 * an instruction to go looking. Read once and cleared, so it cannot pin the screen open.
 */
private var pendingSettingsPage: SettingsRequest? = null

/** Counts the pages asked for, so that a Settings screen already open goes to one as well. */
private val settingsPageAsked = mutableIntStateOf(0)

/** Opens Settings at [page], and lights up the setting called [anchor] on it if one is named. */
internal fun openSettingsPage(state: AppState, page: SettingsPage, anchor: String? = null) {
    pendingSettingsPage = SettingsRequest(page, anchor)
    settingsPageAsked.intValue++
    state.navigate(Destination.SETTINGS)
}

/** Opens Settings with [query] in its search box, and the caret there to go on typing. */
internal fun openSettingsSearch(state: AppState, query: String) {
    pendingSettingsPage = SettingsRequest(null, null, query)
    settingsPageAsked.intValue++
    state.navigate(Destination.SETTINGS)
}

/**
 * Settings, open at the player bar's page: where the taskbar's own menu sends "Properties", as Windows' taskbar
 * sent its Properties to the taskbar's settings. Settings may be open already, at another page, and goes there too.
 */
internal fun openCustomization(state: AppState) = openSettingsPage(state, SettingsPage.PLAYER_BAR)

@Composable
internal fun SettingsScreen(state: AppState) {
    val settings by state.settings.collectAsState()
    val first = remember { pendingSettingsPage.also { pendingSettingsPage = null } }
    var page by remember { mutableStateOf(first?.page) }
    // The setting to show on the page, and a count of the times one was asked for, so the same one asked for
    // twice is shown twice.
    var anchor by remember { mutableStateOf(first?.anchor) }
    var request by remember { mutableIntStateOf(if (first?.anchor != null) 1 else 0) }
    // Kept while a page is open, so coming back from a setting the search found shows the same results again.
    var query by remember { mutableStateOf(first?.query.orEmpty()) }
    // Counts the times the box was asked to take the caret: a search asked for from elsewhere goes on in it.
    var typeHere by remember { mutableIntStateOf(if (first?.query != null) 1 else 0) }
    val asked by settingsPageAsked
    LaunchedEffect(asked) {
        pendingSettingsPage?.let {
            page = it.page
            anchor = it.anchor
            request++
            it.query?.let { asked -> query = asked; typeHere++ }
            pendingSettingsPage = null
        }
    }
    val open: (SettingsPage, String?) -> Unit = { to, setting ->
        page = to
        anchor = setting
        request++
    }
    // Going into a page and back out again slides, the way a phone's settings do, rather than cutting.
    MotionContent(page, Modifier.fillMaxSize(), kind = MotionKind.PAGE, forward = { _, to -> to != null }) { shown ->
        if (shown != null) {
            SettingsDetailHeader(shown, { page = null; anchor = null }) {
                CompositionLocalProvider(LocalSettingsAnchors provides SettingsAnchors(anchor, request)) {
                    SettingsPageContent(shown, settings, state)
                }
            }
        } else {
            // A Control Panel, in a folder's white pane, under a Windows skin; the pages are property sheets.
            BrowsingPane { SettingsHome(settings, state, query, { query = it }, open, typeHere) }
        }
    }
}

/** What each page is: the panel that draws it. */
@Composable
internal fun SettingsPageContent(page: SettingsPage, settings: SettingsState, state: AppState) {
    when (page) {
        SettingsPage.ACCOUNT -> NoctoriumAccountPanel(state)
        SettingsPage.PROFILE -> ProfileSettingsPanel(settings.preferences, state)
        SettingsPage.THEME -> ThemeSettingsPanel(settings.preferences, state)
        SettingsPage.TEXT -> TextSettingsPanel(settings.preferences, state)
        SettingsPage.PLAYER_BAR -> PlayerBarSettingsPanel(settings.preferences, state)
        SettingsPage.NOW_PLAYING -> NowPlayingSettingsPanel(settings.preferences, state)
        SettingsPage.BROWSING -> BrowsingSettingsPanel(settings.preferences, state)
        SettingsPage.SOUND -> SoundSettingsPanel(settings.preferences, state)
        SettingsPage.PLAYBACK_QUEUE -> PlaybackQueueSettingsPanel(settings.preferences, state)
        SettingsPage.LYRICS -> LyricsSettingsPanel(settings.preferences, state)
        SettingsPage.YOUTUBE -> YouTubeAccountPanel(settings, state)
        SettingsPage.SOUNDCLOUD -> AccountConnectionPanel(
            ProviderType.SOUNDCLOUD,
            settings.preferences.soundCloudCookies,
            settings.soundCloudAccount,
            state,
            settings.preferences.soundCloudUsername,
        )
        SettingsPage.SPOTIFY -> SpotifySettingsPanel(settings, state)
        SettingsPage.BANDCAMP -> BandcampSettingsPanel(settings, state)
        SettingsPage.VK -> VkSettingsPanel(settings, state)
        SettingsPage.SCROBBLING -> ScrobblingSettingsPanel(settings, state)
        SettingsPage.DISCORD -> DiscordSettingsPanel(settings.preferences, state)
        SettingsPage.STARTUP -> StartupSettingsPanel(settings.preferences, state)
        SettingsPage.SHORTCUTS -> ShortcutsSettingsPanel(settings.preferences, state)
        SettingsPage.DOWNLOADS -> SettingsPageColumn { SettingsPanelCard { SaveMusicSetting(settings.preferences, state, boxed = true) } }
        SettingsPage.PLAYBACK_TOOLS -> PlaybackToolsPanel()
        SettingsPage.UPDATES -> UpdatePanel(state)
        SettingsPage.DIAGNOSTICS -> DiagnosticsPanel(settings, state)
    }
}

/** A page of cards, one under another, scrolling as one: what most pages of Settings are. */
@Composable
internal fun SettingsPageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * The home of Settings: its title, the search box, and every page under the section it belongs to -- or, while
 * something is typed in the box, the settings that match it.
 */
@Composable
private fun SettingsHome(
    settings: SettingsState,
    state: AppState,
    query: String,
    search: (String) -> Unit,
    open: (SettingsPage, String?) -> Unit,
    typeHere: Int = 0,
) {
    val list = rememberLazyListState()
    val classic = skinned()
    val matches = remember(query) { SettingsIndex.search(query) }
    var selected by remember(query) { mutableIntStateOf(0) }
    // The arrows can choose a setting below the fold; the list follows, keeping the chosen one in view.
    LaunchedEffect(selected, query) {
        if (query.isBlank()) return@LaunchedEffect
        val row = (if (settings.message != null) 3 else 2) + selected
        val shown = list.layoutInfo.visibleItemsInfo.filter { it.offset >= 0 && it.offset + it.size <= list.layoutInfo.viewportEndOffset }.map { it.index }
        if (row !in shown) list.animateScrollToItem((row - 3).coerceAtLeast(0))
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Two pages to a row where the window has the room, three under a Windows skin, whose items are smaller.
        val width = maxWidth - 60.dp
        val columns = when {
            classic && width >= 900.dp -> 3
            width >= 720.dp -> 2
            else -> 1
        }
        LazyColumn(
            Modifier.fillMaxSize().classicScrollbar(list).padding(horizontal = 30.dp),
            state = list,
            contentPadding = chromePadding(top = 26.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (classic) 2.dp else 10.dp),
        ) {
            item {
                ScreenTitle("Settings", 34.sp)
                Text("Accounts, services and how Noctorium behaves.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(if (classic) 10.dp else 14.dp))
                SettingsSearchBox(query, search, matches, selected, { selected = it }, open, go = state::navigate, typeHere = typeHere)
                Spacer(Modifier.height(if (classic) 6.dp else 4.dp))
            }
            settings.message?.let { message ->
                item { SettingsMessage(message, state) }
            }
            if (query.isNotBlank()) {
                settingsResults(query, matches, selected, open, go = state::navigate)
            } else {
                SettingsSection.entries.forEach { section ->
                    item(key = "section-${section.name}") { SectionHeading(section) }
                    SettingsPage.entries.filter { it.section == section }.chunked(columns).forEach { row ->
                        item(key = "row-${row.first().name}") {
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(if (classic) 4.dp else 10.dp)) {
                                row.forEach { page ->
                                    Box(Modifier.weight(1f).fillMaxHeight()) { SettingsTile(page, settings, state) { open(page, null) } }
                                }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The note the settings leave after something changed, such as an account connected. */
@Composable
private fun SettingsMessage(message: String, state: AppState) {
    if (skinned()) {
        InfoBar(Icons.Default.CheckCircle, message, MaterialTheme.colorScheme.primary, state::clearSettingsMessage)
        return
    }
    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f), shape = RoundedCornerShape(11.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp)); Text(message, Modifier.weight(1f), fontSize = 12.sp)
            IconButton(state::clearSettingsMessage, Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Dismiss", Modifier.size(16.dp)) }
        }
    }
}

/**
 * A section's name over its pages: in small capitals as the sidebar labels its groups, or, under a Windows skin,
 * bold over the rule a folder's web view drew under its headings.
 */
@Composable
private fun SectionHeading(section: SettingsSection) {
    if (skinned()) {
        // XP wrote a folder's headings in its own blue over a pale rule; 98 over the rule from the title bar's navy.
        val xp = skin() == ThemeSkin.WINDOWS_XP
        Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(section.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (xp) Luna.GroupTitle else Color.Unspecified)
                Spacer(Modifier.width(8.dp))
                Text(section.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(3.dp))
            if (xp) Box(Modifier.fillMaxWidth().height(1.dp).background(Luna.GroupEdge)) else HeadingRule()
        }
        return
    }
    Row(Modifier.padding(top = 16.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            section.title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(10.dp))
        Text(section.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A page's picture, on its tile and beside its settings in the search's results. */
internal fun SettingsPage.icon(): ImageVector = when (this) {
    SettingsPage.ACCOUNT -> Icons.Default.Insights
    SettingsPage.PROFILE -> Icons.Default.AccountCircle
    SettingsPage.THEME -> Icons.Default.Palette
    SettingsPage.TEXT -> Icons.Default.TextFields
    SettingsPage.PLAYER_BAR -> Icons.Default.Tune
    SettingsPage.NOW_PLAYING -> Icons.Default.Album
    SettingsPage.BROWSING -> Icons.Default.GridView
    SettingsPage.SOUND -> Icons.Default.Equalizer
    SettingsPage.PLAYBACK_QUEUE -> Icons.AutoMirrored.Filled.QueueMusic
    SettingsPage.LYRICS -> Icons.Default.Lyrics
    SettingsPage.YOUTUBE -> Icons.Default.PlayCircle
    SettingsPage.SOUNDCLOUD -> Icons.Default.Cloud
    SettingsPage.SPOTIFY -> Icons.Default.LibraryMusic
    SettingsPage.BANDCAMP -> Icons.Default.Album
    SettingsPage.VK -> Icons.Default.Headphones
    SettingsPage.SCROBBLING -> Icons.Default.History
    SettingsPage.DISCORD -> Icons.Default.SportsEsports
    SettingsPage.STARTUP -> Icons.Default.PowerSettingsNew
    SettingsPage.SHORTCUTS -> Icons.Default.Keyboard
    SettingsPage.DOWNLOADS -> Icons.Default.SaveAlt
    SettingsPage.PLAYBACK_TOOLS -> Icons.Default.Extension
    SettingsPage.UPDATES -> Icons.Default.SystemUpdateAlt
    SettingsPage.DIAGNOSTICS -> Icons.Default.MonitorHeart
}

/** What a page's tile says: its name -- the account's and the profile's are the listener's own -- what it holds, and whether it is on. */
private data class TileText(val title: String, val subtitle: String, val active: Boolean = false)

@Composable
private fun tileText(page: SettingsPage, settings: SettingsState, state: AppState): TileText {
    val preferences = settings.preferences
    return when (page) {
        SettingsPage.ACCOUNT -> {
            val account by state.account.collectAsState()
            TileText(
                account.user?.displayName ?: "Noctorium account",
                if (account.signedIn) {
                    "${account.stats.streams} streamed · ${account.stats.uniqueTracks} different · ${formatHours(account.stats.hours)} listened"
                } else {
                    "Sign in to count what you listen to"
                },
            )
        }
        // The history is on the profile's page, so the tile says so: it is where somebody looks for what they played.
        SettingsPage.PROFILE -> TileText(preferences.profileName, "Your local profile and history")
        SettingsPage.THEME -> TileText(page.title, "${preferences.theme.displayName} · accent: ${preferences.accent.displayName.lowercase()}")
        SettingsPage.TEXT -> TileText(page.title, "${preferences.font.displayName} typeface · ${preferences.textSize.displayName.lowercase()} size")
        SettingsPage.PLAYER_BAR -> TileText(
            page.title,
            "${preferences.playerBarStyle.displayName}, ${preferences.playerBarPosition.displayName.lowercase()} · " +
                "${preferences.progressBarStyle.displayName} seek bar",
        )
        SettingsPage.NOW_PLAYING -> TileText(
            page.title,
            "${preferences.desktop.nowPlaying.layout.displayName} · ${preferences.nowPlayingBackdrop.displayName.lowercase()} backdrop · lyrics",
        )
        SettingsPage.BROWSING -> TileText(page.title, "Cards, badges, what Home shows and the sidebar")
        // Its own page rather than a card under Look: how the music sounds is not how Noctorium looks, and
        // somebody looking for an equaliser looks for the word Sound.
        SettingsPage.SOUND -> TileText(
            page.title,
            if (preferences.equalizer.enabled) "Equaliser on · ${preferences.equalizer.preset.displayName}" else "Equaliser off",
            preferences.equalizer.enabled,
        )
        // Beside Sound, as how the music goes on rather than how it sounds: its speed, what follows the
        // queue, whether the queue is kept, and how the sleep timer ends it.
        SettingsPage.PLAYBACK_QUEUE -> TileText(page.title, playbackSummary(preferences))
        SettingsPage.LYRICS -> TileText(
            page.title,
            preferences.lyricsProvider?.let { "Opens on ${it.displayName} · 8 sources" } ?: "LRCLIB, Better Lyrics, Genius and 5 more",
        )
        SettingsPage.YOUTUBE -> TileText(
            "YouTube Music",
            accountSummary(preferences.youtubeCookies, settings.youtubeAccount),
            settings.youtubeAccount.status == AccountConnectionStatus.CONNECTED,
        )
        SettingsPage.SOUNDCLOUD -> TileText(
            "SoundCloud",
            accountSummary(preferences.soundCloudCookies, settings.soundCloudAccount),
            settings.soundCloudAccount.status == AccountConnectionStatus.CONNECTED,
        )
        SettingsPage.SPOTIFY -> TileText(page.title, spotifySummary(settings.spotify), settings.spotify.connected)
        SettingsPage.BANDCAMP -> TileText(page.title, bandcampSummary(preferences.bandcampUsername, settings.bandcamp), preferences.bandcampUsername.isNotBlank())
        SettingsPage.VK -> TileText(page.title, vkSummary(settings.vk), settings.vk.connected)
        SettingsPage.SCROBBLING -> {
            // The services by name, now that there are three: the same name signed in to two of them said nothing.
            val connected = listOf(
                "Last.fm" to settings.scrobbling.lastFm,
                "Libre.fm" to settings.scrobbling.libreFm,
                "ListenBrainz" to settings.scrobbling.listenBrainz,
            ).filter { (_, service) -> service.status == ScrobbleConnectionStatus.CONNECTED }.map { it.first }
            TileText(page.title, if (connected.isEmpty()) "Connect Last.fm, Libre.fm or ListenBrainz" else "Connected: ${connected.joinToString()}", connected.isNotEmpty())
        }
        SettingsPage.DISCORD -> {
            // Read from the live setting. This once read the field that only exists to carry a choice over
            // from an older build, which is never written and so always said Disabled however it was set.
            val discord = preferences.discord
            TileText(page.title, if (discord.enabled) "Enabled · ${discord.timestamps.displayName}" else "Disabled", discord.enabled)
        }
        SettingsPage.STARTUP -> TileText(page.title, startupSubtitle(preferences))
        SettingsPage.SHORTCUTS -> TileText(page.title, shortcutsSubtitle(preferences.desktop.shortcuts), preferences.desktop.shortcuts.globalEnabled)
        SettingsPage.DOWNLOADS -> TileText(
            page.title,
            preferences.exportFolder.ifBlank { null }?.let { "Into $it" } ?: "Onto your desktop, as MP3 where it can",
        )
        SettingsPage.PLAYBACK_TOOLS -> TileText(page.title, playbackToolsSubtitle())
        SettingsPage.UPDATES -> TileText(page.title, updatesSubtitle(state))
        SettingsPage.DIAGNOSTICS -> TileText(page.title, "Check yt-dlp, mpv and storage")
    }
}

/** The state of the two programs playback depends on, on the tile rather than one click inside it. */
@Composable
private fun playbackToolsSubtitle(): String {
    val tools by PlaybackToolInstaller.state.collectAsState()
    val installing = tools.installing
    return when {
        installing != null -> "Installing ${installing.displayName}…"
        tools.tools.isEmpty() -> "yt-dlp and mpv"
        tools.missingRequired.isNotEmpty() ->
            tools.missingRequired.joinToString(" and ") { it.displayName } + " is missing"
        else -> "yt-dlp and mpv are ready"
    }
}

/** Says on the tile itself when there is something new, so it is not hidden one click away. */
@Composable
private fun updatesSubtitle(state: AppState): String {
    val updates by state.updates.collectAsState()
    val available = updates.available
    return when {
        available != null -> "Noctorium ${available.version} is available"
        updates.currentVersion.isNotBlank() -> "You have ${updates.currentVersion}"
        else -> "Check for a newer Noctorium"
    }
}

/** A page's tile on the home: its picture, its name and what it holds, and a tick where it is on or signed in. */
@Composable
private fun SettingsTile(page: SettingsPage, settings: SettingsState, state: AppState, action: () -> Unit) {
    val text = tileText(page, settings, state)
    // An item in a Control Panel under a Windows skin.
    if (skinned()) {
        ControlPanelItem(text.title, text.subtitle, page.icon(), action, text.active)
        return
    }
    Surface(onClick = action, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(page.icon(), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(text.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (text.active) {
                Icon(Icons.Default.CheckCircle, "Active", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

/**
 * A page's header: the way back to Settings, the section it is in, and its name. The section is a way back too,
 * for somebody who came by the search and wants to see what else is there.
 */
@Composable
private fun SettingsDetailHeader(page: SettingsPage, back: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to settings") }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    "Settings  ›  ${page.section.title}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = back),
                )
                ScreenTitle(page.title, 28.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxSize()) { content() }
    }
}

package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.skinCorners
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.*
import app.noctorium.desktopAppState
import app.noctorium.discord.DiscordPreview
import app.noctorium.discord.DiscordPresenceSettings
import app.noctorium.discord.PausedBehaviour
import app.noctorium.discord.PresenceActivityKind
import app.noctorium.discord.PresenceArtwork
import app.noctorium.discord.PresenceButton
import app.noctorium.discord.PresenceTimestamps
import app.noctorium.downloads.DownloadStage
import app.noctorium.domain.*
import app.noctorium.lyrics.LyricLine
import app.noctorium.lyrics.LyricsProviderId
import app.noctorium.lyrics.LyricsProviderOutcome
import app.noctorium.lyrics.LyricsProviderStatus
import app.noctorium.library.TrackEdit
import androidx.compose.material.icons.outlined.PushPin
import app.noctorium.playback.PlaybackToolInstaller
import app.noctorium.playback.QueueState
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.RepeatMode
import app.noctorium.auth.EmbeddedBrowserSession
import app.noctorium.auth.SOUNDCLOUD_SESSION_URLS
import app.noctorium.auth.SOUNDCLOUD_SIGN_IN
import app.noctorium.auth.YOUTUBE_SESSION_URLS
import app.noctorium.auth.YOUTUBE_HARVEST_URLS
import app.noctorium.auth.mergeCookies
import app.noctorium.auth.SOUNDCLOUD_OWN_LIKES
import app.noctorium.auth.YOUTUBE_MUSIC_HOME
import app.noctorium.auth.YOUTUBE_SIGN_IN
import app.noctorium.auth.isYouTubeSignedIn
import app.noctorium.auth.isSoundCloudSignedIn
import app.noctorium.auth.permalinkFromBrowserUrl
import app.noctorium.auth.adoptCookieFile
import app.noctorium.auth.writeCookieFile
import app.noctorium.bandcamp.BandcampMusicProvider
import app.noctorium.spotify.isSpotifyArtist
import app.noctorium.playlists.LocalPlaylist
import app.noctorium.playlists.PlaylistShareLink
import app.noctorium.settings.*
import app.noctorium.platform.LaunchAtLogin
import app.noctorium.platform.StartupMode
// The skin's controls, in Material's place: each is Material's own under the standard skin.
import app.noctorium.ui.skins.AlertDialog
import app.noctorium.ui.skins.AssistChip
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.DropdownMenu
import app.noctorium.ui.skins.DropdownMenuItem
import app.noctorium.ui.skins.FilledIconButton
import app.noctorium.ui.skins.FilledTonalButton
import app.noctorium.ui.skins.FilledTonalIconButton
import app.noctorium.ui.skins.FilterChip
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.LinearProgressIndicator
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.RadioButton
import app.noctorium.ui.skins.Slider
import app.noctorium.ui.skins.Switch
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.BrowsingPane
import app.noctorium.ui.skins.HeroWindow
import app.noctorium.ui.skins.ListRow
import app.noctorium.ui.skins.listWell
import app.noctorium.ui.skins.PanelTabs
import app.noctorium.ui.skins.PanelWindow
import app.noctorium.ui.skins.ScreenTitle
import app.noctorium.ui.skins.classicScrollbar
import app.noctorium.ui.skins.SkinnedMaterialTheme
import app.noctorium.ui.skins.CheckRow
import app.noctorium.ui.skins.ControlPanelItem
import app.noctorium.ui.skins.GroupLegend
import app.noctorium.ui.skins.OptionButtons
import app.noctorium.ui.skins.OptionWithNote
import app.noctorium.ui.skins.PreviewedOption
import app.noctorium.ui.skins.SettingsGroup
import app.noctorium.ui.skins.SkinPlaylistCard
import app.noctorium.ui.skins.SkinRail
import app.noctorium.ui.skins.SkinTrackCard
import app.noctorium.ui.skins.SkinnedPlayerBar
import app.noctorium.ui.skins.TaskbarPlayerBar
import app.noctorium.ui.skins.caption
import app.noctorium.ui.skins.isWindows
import app.noctorium.ui.skins.scrollingPage
import app.noctorium.ui.skins.skinShape
import app.noctorium.ui.skins.skinned
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NoctoriumApp(appState: AppState = remember { desktopAppState() }, window: java.awt.Window? = null) {
    val ui by appState.ui.collectAsState()
    val queue by appState.queue.state.collectAsState()
    val playback by appState.playback.collectAsState()
    val preferences = appState.settings.collectAsState().value.preferences

    DisposableEffect(appState) {
        onDispose { appState.close() }
    }

    // The theme decides the page, the panels and the writing; the accent is chosen separately and may be
    // the theme's own, one of the named ones, or -- "Match the artwork" -- the palette already sampled
    // for the now playing backdrop, so the whole interface drifts with whatever is on.
    val theme = preferences.themeColours()
    // Nothing playing, or a cover with no colour in it, leaves the theme's own accent in force rather
    // than a fixed violet: "Match the artwork" should never paint a Gruvbox window lilac.
    val artworkPalette = rememberArtworkPalette(
        queue.current?.artworkUrl,
        queue.current?.provider ?: ProviderType.LOCAL,
        fallback = ArtworkPalette(Color(theme.accent), Color(theme.card)),
    )
    // A colour being dragged to in the accent picker wins while the pointer is down, and is followed at once
    // rather than eased towards: easing would leave the window trailing behind the pointer.
    val previewAccent = accentPreview.value
    val accentTarget = previewAccent
        ?: if (preferences.accent == AccentPreset.ARTWORK) artworkPalette.primary else Color(preferences.resolvedAccent(null))
    val accent by animateColorAsState(accentTarget, if (previewAccent != null) snap() else tween(600), label = "accent")
    // Beyond the colours: 98's bevels and Luna's blue, for the two Windows themes, and nothing for the rest.
    val skin = preferences.themeSkin

    // The title bar is Windows', not ours, so it has to be told the colour separately — and told again
    // whenever the theme changes, or a switch to a light theme would leave a black strip above it. Under a
    // Windows skin it is that desktop's own title bar colour, with white writing, as the windows inside are.
    LaunchedEffect(window, theme, skin) {
        val caption = skin.caption()
        WindowChrome.applyDarkTitleBar(
            window,
            backgroundArgb = caption?.first ?: Color(theme.background).toArgb(),
            foregroundArgb = caption?.second ?: Color(theme.text).copy(alpha = .88f).toArgb(),
        )
    }

    // Nothing on a desktop ships with yt-dlp or mpv, and until this ran a fresh install could play
    // nothing at all. Done here rather than asked about: there is no version of this the listener has a
    // useful opinion on, and the alternative was a dialog in front of an application that cannot work yet.
    LaunchedEffect(Unit) { PlaybackToolInstaller.ensureReady() }

    var shortcutsOpen by remember { mutableStateOf(false) }
    // Bumped by the shortcut that means "search"; the search box watches it and takes the caret. A
    // counter rather than a flag, so pressing it twice while already there works the second time too.
    var focusSearch by remember { mutableStateOf(0) }
    val keyboard = remember { FocusRequester() }

    // Whether the caret is in a box. Text fields set it themselves, because a focused one does not stop
    // an ordinary letter reaching here -- typing "no surprises" into the search box put the words in and
    // toggled shuffle three times on the way past, which is not something either piece of code admits to.
    val typing = remember { mutableStateOf(false) }

    fun handle(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val shortcut = shortcutFor(
            key = event.key,
            ctrl = event.isCtrlPressed,
            shift = event.isShiftPressed,
            alt = event.isAltPressed,
            typing = typing.value,
        ) ?: return false
        when (shortcut) {
            Shortcut.PlayPause -> appState.togglePlayback()
            Shortcut.Next -> appState.next()
            Shortcut.Previous -> appState.previous()
            is Shortcut.Seek -> {
                val duration = playback.durationMs
                if (duration > 0) {
                    appState.seekTo((playback.positionMs + shortcut.deltaMs).coerceIn(0, duration))
                }
            }
            is Shortcut.Volume -> appState.setVolume((playback.volume + shortcut.delta).coerceIn(0f, 1f))
            Shortcut.Mute -> appState.toggleMute()
            Shortcut.Shuffle -> appState.toggleShuffle()
            Shortcut.Repeat -> appState.cycleRepeat()
            Shortcut.Like -> queue.current?.let(appState::toggleLike)
            is Shortcut.Go -> appState.navigate(shortcut.destination)
            Shortcut.Search -> {
                appState.navigate(Destination.SEARCH)
                focusSearch++
            }
            Shortcut.Help -> shortcutsOpen = true
            // The clipboard read at the moment of pressing, and the link screen shown so what happened to
            // it is visible: playing, or why not.
            Shortcut.PasteLink -> clipboardText()?.let { copied ->
                appState.openLink(copied)
                appState.navigate(Destination.LINK)
            }
            // The sheet first, and otherwise the caret: having typed in a box, there is no way back to
            // the keyboard without reaching for the mouse, which rather defeats the point of all this.
            // Escape puts focus back where the shortcuts live.
            Shortcut.Dismiss -> when {
                shortcutsOpen -> shortcutsOpen = false
                typing.value -> runCatching { keyboard.requestFocus() }
                // Nothing of ours is open, so it belongs to whatever else is listening.
                else -> return false
            }
        }
        return true
    }

    // Something has to hold focus for a key to arrive at all, and on a fresh window nothing does.
    LaunchedEffect(Unit) { runCatching { keyboard.requestFocus() } }

    // The keys on the keyboard itself, which reach Noctorium whether or not it is the window in front.
    // Only play, pause and the two track keys: those are the ones somebody presses without looking, and
    // they are the ones an application may reasonably take from the whole machine.
    DisposableEffect(Unit) {
        val grab = MediaKeys.start { shortcut ->
            when (shortcut) {
                Shortcut.PlayPause -> appState.togglePlayback()
                Shortcut.Next -> appState.next()
                Shortcut.Previous -> appState.previous()
                else -> Unit
            }
        }
        onDispose { grab?.close() }
    }

    val density = LocalDensity.current
    SkinnedMaterialTheme(skin, theme, accent, preferences.font, preferences.cornerStyle) {
      // Text size, applied to the density rather than to the typography.
      //
      // Almost every size in this application is written at the call site as a literal `.sp`, so scaling
      // MaterialTheme's typography would have moved about four labels and left the rest where they were.
      // fontScale is the one lever that reaches every `.sp` there is, whether it came from a theme or
      // from somebody typing 12 into a Text. It multiplies whatever the system is already asking for,
      // so a machine set to large text and Noctorium set to large text get both.
      CompositionLocalProvider(
          LocalTyping provides typing,
          LocalDensity provides Density(density.density, density.fontScale * preferences.textSize.scale),
          LocalMotion provides preferences.animations,
      ) {
        if (shortcutsOpen) ShortcutsSheet { shortcutsOpen = false }
        // The launch check has an answer nobody asked for; this is where it gets to say so once.
        UpdatePrompt(appState)
        Surface(
            Modifier
                .fillMaxSize()
                .focusRequester(keyboard)
                .focusable()
                .onKeyEvent(::handle),
        ) {
            Row {
                if (skin.isWindows) {
                    SkinRail(ui.destination, preferences.desktop.hiddenDestinations, queue.current, appState::navigate)
                } else {
                    NavigationRail(ui.destination, preferences.desktop.hiddenDestinations, appState::navigate)
                }
                Column(Modifier.weight(1f)) {
                    PlaybackToolsBanner {
                        pendingSettingsPage = SettingsPage.PLAYBACK_TOOLS
                        appState.navigate(Destination.SETTINGS)
                    }
                    val playerAtTop = preferences.playerBarPosition == PlayerBarPosition.TOP
                    val screens: @Composable () -> Unit = {
                        // One screen easing in over the last rather than the window cutting between them.
                        MotionContent(ui.destination, Modifier.fillMaxSize()) { destination ->
                            // The pages for browsing sit in a folder's white pane under a Windows skin.
                            when (destination) {
                                Destination.HOME -> BrowsingPane { HomeScreen(ui, appState) }
                                Destination.SEARCH -> BrowsingPane { SearchScreen(ui, appState, focusSearch) }
                                Destination.LINK -> BrowsingPane { LinkScreen(appState) }
                                Destination.LIBRARY -> BrowsingPane { LibraryScreen(appState) }
                                Destination.DOWNLOADS -> BrowsingPane { DownloadsScreen(appState) }
                                Destination.NOW_PLAYING -> NowPlayingScreen(queue, playback, appState)
                                Destination.QUEUE -> BrowsingPane { QueueScreen(queue, appState) }
                                Destination.SETTINGS -> SettingsScreen(appState)
                            }
                        }
                    }
                    // Glass belongs to the standard look: a 98 window had no panes of it to float a player in.
                    if (preferences.surfaceStyle.isGlass && !skin.isWindows) {
                        Box(Modifier.weight(1f)) {
                            GlassContent(queue, playback, appState, Color(theme.background), playerAtTop, screens)
                        }
                    } else {
                        val bar: @Composable () -> Unit = {
                            SkinnedPlayerBar(preferences.playerBarPosition, preferences.playerBarStyle) { PlayerBar(queue, playback, appState) }
                        }
                        if (playerAtTop) bar()
                        Box(Modifier.weight(1f)) { screens() }
                        if (!playerAtTop) bar()
                    }
                }
            }
        }
      }
    }
}

/**
 * The content area under liquid glass: the screens running its full height, and the player floating
 * over them as a pane.
 *
 * The content goes *underneath*, which is the whole reason for the arrangement: a pane over a flat patch
 * of page has nothing to bend, and a pane over a grid of covers bends every one of them as they scroll
 * past. The screens are told how much of them the player covers, so their last rows can still be
 * scrolled clear of it.
 *
 * The pane is drawn after the content and outside what is recorded for it, or it would be looking at
 * itself.
 */
@Composable
private fun BoxScope.GlassContent(
    queue: QueueState,
    playback: PlaybackState,
    state: AppState,
    background: Color,
    playerAtTop: Boolean,
    screens: @Composable () -> Unit,
) {
    val backdrop = rememberGlassBackdrop()
    val density = LocalDensity.current
    var cover by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().glassSource(backdrop)) {
        GlassWash(queue.current?.artworkUrl, queue.current?.provider ?: ProviderType.LOCAL, background)
        val covered = with(density) { cover.toDp() }
        if (playerAtTop) {
            // At the top the screens start below the pane rather than under it. Half of them have a
            // title that sits above their list instead of in it -- the settings pages, search, the
            // library -- and a player floating over those covers the title, which no list padding can
            // move. The phone solves this in its one shared scaffold; the desktop has no such scaffold,
            // so it is solved here, and the wash still runs behind the pane, which is still glass.
            Box(Modifier.fillMaxSize().padding(top = covered)) { screens() }
        } else {
            CompositionLocalProvider(LocalChromeInsets provides ChromeInsets(bottom = covered)) { screens() }
        }
    }
    Box(
        Modifier
            .align(if (playerAtTop) Alignment.TopCenter else Alignment.BottomCenter)
            .onSizeChanged { cover = it.height }
            .padding(Glass.FLOAT_INSET_DP.dp),
    ) {
        GlassPane(backdrop, cornerRadius = 28.dp) { PlayerBar(queue, playback, state) }
    }
}

/**
 * The sidebar, without the items the listener put away in [hidden].
 *
 * Putting one away only takes it out of here. Its number key, the player bar and every link that leads to
 * it still go there, so a page somebody rarely visits costs them no room and is still a key away.
 */
@Composable
private fun NavigationRail(selected: Destination, hidden: Set<Destination>, navigate: (Destination) -> Unit) {
    @Composable
    fun item(destination: Destination) {
        if (sidebarShows(destination, hidden)) {
            NavItem(destination.sidebarLabel(), destination.sidebarIcon(), selected == destination) { navigate(destination) }
        }
    }
    Column(
        Modifier.width(RAIL_WIDTH).fillMaxHeight().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The same artwork the window, the taskbar and the phone's launcher show, so the mark beside
            // the name and the mark in the title bar are recognisably one thing.
            Image(
                painterResource("noctorium-mark.png"),
                null,
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(9.dp))
            // One line, and sized to fit on it. The rail was 176dp wide and the name grew by three letters
            // at the rename, so at the old size it broke across two lines as "Noctoriu / m" -- which is
            // the first thing anybody saw of the application.
            Text(
                "Noctorium",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.height(26.dp))
        item(Destination.HOME)
        item(Destination.SEARCH)
        item(Destination.LINK)
        item(Destination.LIBRARY)
        item(Destination.DOWNLOADS)
        // Now Playing and Queue were reachable only by clicking the player bar; they are destinations, so they
        // belong in the rail alongside everything else. Their heading goes when both of them have.
        if (sidebarShows(Destination.NOW_PLAYING, hidden) || sidebarShows(Destination.QUEUE, hidden)) {
            Spacer(Modifier.height(18.dp))
            Text(
                "PLAYING",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
            )
            item(Destination.NOW_PLAYING)
            item(Destination.QUEUE)
        }
        Spacer(Modifier.weight(1f))
        item(Destination.SETTINGS)
    }
}

/**
 * Wide enough for "Now playing" and "Paste link" on one line at the ordinary text size, which at 176dp
 * neither was: every label past eight letters broke in two, and a selected one, being bold, broke where
 * it had not a moment before. Past that, a label is cut rather than wrapped, so the rail keeps its rhythm
 * at the largest text size too.
 */
private val RAIL_WIDTH = 196.dp

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, action: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = action,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, tint = color)
            Spacer(Modifier.width(12.dp))
            Text(label, color = color, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun HomeScreen(ui: AppUiState, state: AppState) {
    val hidden = state.settings.collectAsState().value.preferences.hiddenHomeParts
    // The filter at the top and the parts put away in Settings, decided together in one place.
    val home = homeContent(ui, hidden)
    val list = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize().classicScrollbar(list).padding(horizontal = 32.dp), state = list, contentPadding = chromePadding(bottom = 36.dp)) {
        item {
            Spacer(Modifier.height(28.dp))
            // The filter stays when the greeting goes: it is how the rest of the page is chosen.
            HomeHeader(greeting().takeIf { home.greeting }) { FilterChips(ui.providerFilter, state::setFilter) }
            Spacer(Modifier.height(20.dp))
        }
        ui.errorMessage?.let { message ->
            item { PlaybackError(message) }
        }
        // The listener's own row first: what they decided to keep within reach beats what happened to be
        // played last, which beats anything the services suggest.
        if (home.pinned.isNotEmpty()) {
            item {
                TrackRowSection("Pinned", "Kept here by you", home.pinned, state)
            }
        }
        if (home.recent.isNotEmpty()) {
            item {
                TrackRowSection("Jump back in", "Where you left off", home.recent, state)
            }
        }
        if (home.nothingToShow) {
            item { HomePutAway(everything = hidden.containsAll(HomePart.entries), state) }
        }
        if (home.loading) {
            items(2) { LoadingSection() }
        } else {
            items(home.sections, key = { it.id }) { section ->
                // A row is songs or cards, never both -- the services build them that way, and a row
                // mixing things that play with things that open would make every click a guess.
                if (section.playlists.isNotEmpty()) {
                    PlaylistRowSection(section.title, section.subtitle, section.playlists, state)
                } else {
                    TrackRowSection(section.title, section.subtitle, section.tracks, state)
                }
            }
        }
    }
}

/**
 * What Home says when there is nothing left on it to draw, rather than an empty page that looks broken.
 *
 * [everything] is every part put away on purpose, which deserves a different word from parts that are
 * still on but have nothing in them yet. Either way the way back is one click.
 */
@Composable
private fun HomePutAway(everything: Boolean, state: AppState) {
    Column(Modifier.fillMaxWidth().padding(vertical = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AutoAwesome, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(if (everything) "Home is all put away" else "Nothing here just now", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (everything) {
                "You have hidden every part of it. The music is a search away, and Home comes back whenever you like."
            } else {
                "Play something and it will be waiting here, or bring back more of Home."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 460.dp),
        )
        Spacer(Modifier.height(18.dp))
        FilledTonalButton({
            pendingSettingsPage = SettingsPage.CUSTOMIZATION
            state.navigate(Destination.SETTINGS)
        }) {
            Icon(Icons.Default.Tune, null, Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("Choose what Home shows")
        }
    }
}

/**
 * The greeting and the filter: on one line, the filter at the far end, where both fit, and the filter under
 * the greeting where they do not.
 *
 * With every service in it the filter is wider than a narrow window has room for beside the greeting, and
 * made to share the line anyway the greeting stood one letter to a line. Measured rather than guessed at a
 * window width, since the names, the text size and the greeting itself all change how much room it takes.
 * Without a greeting the filter keeps the far end on its own.
 */
@Composable
private fun HomeHeader(greeting: String?, filter: @Composable () -> Unit) {
    Layout(
        content = {
            if (greeting != null) ScreenTitle(greeting, 32.sp, maxLines = 1)
            Box { filter() }
        },
    ) { measurables, constraints ->
        val title = if (greeting != null) measurables.first() else null
        val chips = measurables.last()
        val width = constraints.maxWidth
        val gap = 24.dp.roundToPx()
        val oneLine = title == null ||
            title.maxIntrinsicWidth(constraints.maxHeight) + gap + chips.maxIntrinsicWidth(constraints.maxHeight) <= width
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val titlePlaced = title?.measure(loose)
        val chipsPlaced = chips.measure(loose)
        if (oneLine) {
            val height = maxOf(titlePlaced?.height ?: 0, chipsPlaced.height)
            layout(width, height) {
                // Along the bottom, where the greeting's letters sit, as the row had them.
                titlePlaced?.place(0, height - titlePlaced.height)
                chipsPlaced.place(width - chipsPlaced.width, height - chipsPlaced.height)
            }
        } else {
            val between = 14.dp.roundToPx()
            layout(width, titlePlaced!!.height + between + chipsPlaced.height) {
                titlePlaced.place(0, 0)
                chipsPlaced.place(0, titlePlaced.height + between)
            }
        }
    }
}

/** Greeting keyed to the actual clock rather than a fixed string. */
private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    in 18..22 -> "Good evening"
    else -> "Still up"
}

@Composable
private fun FilterChips(selected: ProviderFilter, select: (ProviderFilter) -> Unit) {
    // A row of buttons that stay pressed in under a Windows skin, as a toolbar's choice of view did.
    if (skinned()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            ProviderFilter.entries.forEach { filter ->
                FilterChip(selected == filter, { select(filter) }, label = { Text(filter.displayName) })
            }
        }
        return
    }
    // Sideways, when even a line of its own is narrower than every service's name: see HomeHeader.
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProviderFilter.entries.forEach { filter ->
            val active = selected == filter
            Surface(
                onClick = { select(filter) },
                shape = RoundedCornerShape(10.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = .45f) else MaterialTheme.colorScheme.outline.copy(alpha = .55f)),
            ) {
                Text(
                    filter.displayName,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TrackRowSection(title: String, subtitle: String?, tracks: List<Track>, state: AppState) {
    // A section already names its service, so a per-card badge would be the same word a third time. The badge
    // only earns its place when a row actually mixes services.
    val preferences = state.settings.collectAsState().value.preferences
    val mixedProviders = when (preferences.badgePolicy) {
        BadgePolicy.ALWAYS -> true
        BadgePolicy.NEVER -> false
        BadgePolicy.AUTO -> tracks.map { it.provider }.distinct().size > 1
    }
    Column(Modifier.padding(vertical = 14.dp)) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(tracks, key = { it.queueKey }) { track ->
                TrackCard(track, tracks, state, showBadge = mixedProviders)
            }
        }
    }
}

/**
 * A shelf of playlists and albums, which open rather than play.
 *
 * Most of what a service puts on its home page is this. Clicking one goes to the library, because that
 * is where the playlist screen lives -- opening something the listener cannot then see would be the same
 * as doing nothing at all.
 */
@Composable
private fun PlaylistRowSection(
    title: String,
    subtitle: String?,
    playlists: List<Playlist>,
    state: AppState,
) {
    val preferences = state.settings.collectAsState().value.preferences
    val cardWidth = preferences.cardSize.widthDp.dp
    Column(Modifier.padding(vertical = 14.dp)) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(playlists, key = { it.playlistKey }) { playlist -> PlaylistCard(playlist, cardWidth, state) }
        }
    }
}

/**
 * Whether a playlist stands for an artist -- everything they have put out -- rather than for one release or
 * list, which is how Bandcamp and Spotify both open an artist. Each service's own core says which are.
 */
internal fun Playlist.isArtist(): Boolean = BandcampMusicProvider.isArtist(this) || isSpotifyArtist()

/**
 * One playlist, album or artist on a shelf: its cover and its name, opening its page when clicked.
 *
 * An artist is drawn round and says it is one where an album says whose it is. A shelf of search results
 * mixes the two, and a square photograph of somebody reads as an album cover.
 */
@Composable
private fun PlaylistCard(playlist: Playlist, width: Dp, state: AppState) {
    val artist = playlist.isArtist()
    if (skinned()) {
        val owner = playlist.ownerName?.takeIf { it.isNotBlank() }
        val line = if (artist) listOfNotNull("Artist", owner?.takeUnless { it.equals("Artist", ignoreCase = true) }).joinToString(" · ") else owner
        SkinPlaylistCard(playlist, width, artist, line) {
            state.openPlaylist(playlist)
            state.navigate(Destination.LIBRARY)
        }
        return
    }
    val shape = if (artist) CircleShape else RoundedCornerShape(11.dp)
    Column(
        Modifier
            .width(width)
            .clickable {
                state.openPlaylist(playlist)
                state.navigate(Destination.LIBRARY)
            },
    ) {
        Box(Modifier.size(width).clip(shape)) {
            if (playlist.artworkUrl != null) {
                RemoteArtwork(playlist.artworkUrl, playlist.provider, Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artist) Icon(Icons.Default.Person, null, Modifier.size(width / 3), tint = ink(.45f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            playlist.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // For a Bandcamp artist the owner is where they are from, when Bandcamp says: "Artist · Bristol".
        // Spotify's artists come with "Artist" there already, which is not said twice.
        val owner = playlist.ownerName?.takeIf { it.isNotBlank() }
        (if (artist) listOfNotNull("Artist", owner?.takeUnless { it.equals("Artist", ignoreCase = true) }).joinToString(" · ") else owner)?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackCard(track: Track, sourceQueue: List<Track>, state: AppState, showBadge: Boolean = false) {
    val preferences = state.settings.collectAsState().value.preferences
    if (skinned()) {
        SkinTrackCard(
            track,
            track.displayArtist(),
            preferences.cardSize.widthDp.dp,
            showBadge,
            alwaysShowControls = preferences.hoverControls == HoverControls.ALWAYS,
            play = { state.play(track, PlaybackOrigin.HOME, sourceQueue) },
        ) { TrackMenu(track, state) }
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    // Whether controls hide until pointed at is a preference; some people would rather always see them.
    val hovered = isHovered || preferences.hoverControls == HoverControls.ALWAYS
    val cardWidth = preferences.cardSize.widthDp.dp
    // The cover rises a touch under the pointer, which says "this can be played" before anything is clicked.
    val lift by animateFloatAsState(if (isHovered) 1.035f else 1f, motionSpec(MotionTiming.QUICK + 40), label = "card-lift")
    Surface(
        onClick = { state.play(track, PlaybackOrigin.HOME, sourceQueue) },
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        interactionSource = interaction,
        modifier = Modifier.width(cardWidth).hoverable(interaction),
    ) {
        Column {
            Box(Modifier.size(cardWidth).graphicsLayer { scaleX = lift; scaleY = lift }.clip(RoundedCornerShape(14.dp))) {
                RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
                if (showBadge) {
                    Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { ProviderBadge(track.provider, compact = true) }
                }
                if (hovered) {
                    if (isHovered) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)))
                    Box(Modifier.align(Alignment.TopEnd).padding(2.dp)) { TrackMenu(track, state) }
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(10.dp).size(38.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.onPrimary) }
                }
            }
            Spacer(Modifier.height(9.dp))
            // Two lines, because one-line titles were cutting off mid-word.
            Text(
                track.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            )
            // yt-dlp leaves the uploader blank in flat listings, which used to render as the service name under
            // every single card. Better to show nothing than to repeat the row heading.
            track.displayArtist()?.let { artist ->
                Text(
                    artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .78f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** The artist worth showing, or null when all we have is a stand-in for the service. */
private fun Track.displayArtist(): String? = artistLine
    .takeIf { line ->
        line.isNotBlank() &&
            !line.equals(provider.displayName, ignoreCase = true) &&
            !line.equals("YouTube", ignoreCase = true) &&
            !line.equals("YouTube Music", ignoreCase = true) &&
            !line.equals("SoundCloud", ignoreCase = true)
    }

@Composable
private fun LibraryScreen(state: AppState) {
    val library by state.library.collectAsState()

    LaunchedEffect(Unit) { state.refreshLibrary() }

    var newPlaylistOpen by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }

    if (newPlaylistOpen) {
        val likes by state.likes.collectAsState()
        NewPlaylistDialog(
            title = "New playlist",
            confirmLabel = "Create",
            soundCloudReady = likes.soundCloudReady,
            youTubeReady = likes.youTubeReady,
        ) { name, destination ->
            newPlaylistOpen = false
            if (name != null) {
                when (destination) {
                    PlaylistDestination.SPICE -> state.createPlaylist(name)
                    PlaylistDestination.SOUNDCLOUD -> state.createSoundCloudPlaylist(name)
                    PlaylistDestination.YOUTUBE -> state.createYouTubePlaylist(name)
                }
            }
        }
    }
    if (importOpen) {
        ImportLinkDialog { link ->
            importOpen = false
            link?.let { state.importSharedPlaylist(it) }
        }
    }

    // Opening a playlist slides it in, and going back slides it out again, the way the phone does.
    val view = library.openLocalPlaylist?.let { "local:${it.id}" }
        ?: library.openPlaylist?.let { "playlist:${it.playlistKey}" }
        ?: "list"
    MotionContent(view, Modifier.fillMaxSize(), kind = MotionKind.PAGE, forward = { _, to -> to != "list" }) { shown ->
        // Kept after they are let go of, so a page sliding away still shows what it was showing.
        val local = rememberLast(library.openLocalPlaylist?.takeIf { "local:${it.id}" == shown })
        val service = rememberLast(library.openPlaylist?.takeIf { "playlist:${it.playlistKey}" == shown })
        when {
            shown.startsWith("local:") -> local?.let { LocalPlaylistDetail(it, library.notice, state) }
            shown.startsWith("playlist:") -> service?.let { PlaylistDetail(it, library, state) }
            else -> LibraryList(library, state, { newPlaylistOpen = true }, { importOpen = true })
        }
    }
}

/** The library itself: the playlists made here, and the ones from the connected accounts. */
@Composable
private fun LibraryList(library: LibraryState, state: AppState, newPlaylist: () -> Unit, importLink: () -> Unit) {
    val accountLists = rememberLazyListState()
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ScreenTitle("Your library", 34.sp)
                Text(
                    "Playlists you made here, and playlists from the accounts you connected.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Button(newPlaylist) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text("New playlist")
            }
            Spacer(Modifier.width(9.dp))
            OutlinedButton(importLink) {
                Icon(Icons.Default.Link, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Paste link")
            }
            IconButton({ state.refreshLibrary(force = true) }, enabled = !library.loading) {
                Icon(Icons.Default.Refresh, "Reload playlists")
            }
        }
        Spacer(Modifier.height(10.dp))
        library.notice?.let { LibraryNotice(it, state::clearLibraryNotice) }
        val downloads by state.downloadState.collectAsState()
        downloads.message?.let { LibraryNotice(it, state::clearDownloadMessage) }
        if (downloads.entries.isNotEmpty() || downloads.active.isNotEmpty()) {
            OfflineDownloadsCard(downloads, state)
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(6.dp))
        if (library.localPlaylists.isNotEmpty()) {
            LazyColumn(
                Modifier.heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(library.localPlaylists, key = { it.id }) { playlist ->
                    LocalPlaylistRow(playlist, state) { state.openLocalPlaylist(playlist) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "From your accounts",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(9.dp))
        }
        when {
            library.loading && library.playlists.isEmpty() -> LoadingSection()
            library.needsSoundCloudUsername && library.playlists.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) { SoundCloudNamePrompt(state) }
            library.errorMessage != null && library.playlists.isEmpty() -> LibraryProblem(library.errorMessage!!, state)
            library.playlists.isEmpty() -> EmptyScreen("No playlists yet", "Playlists you own on a connected service appear here.")
            else -> LazyColumn(
                Modifier.classicScrollbar(accountLists),
                state = accountLists,
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = chromePadding(bottom = 28.dp),
            ) {
                library.errorMessage?.let { message ->
                    item { LibraryProblemBanner(message) }
                }
                if (library.needsSoundCloudUsername) {
                    item { SoundCloudNamePrompt(state) }
                }
                items(library.playlists, key = { it.playlistKey }) { playlist ->
                    PlaylistRow(playlist) { state.openPlaylist(playlist) }
                }
            }
        }
    }
}

@Composable
private fun LibraryNotice(message: String, dismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f), shape = RoundedCornerShape(11.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 12.sp) }
            IconButton(dismiss, Modifier.size(26.dp)) { Icon(Icons.Default.Close, "Dismiss", Modifier.size(15.dp)) }
        }
    }
}

@Composable
private fun LocalPlaylistRow(playlist: LocalPlaylist, state: AppState, open: () -> Unit) {
    ListRow(onClick = open, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape = RoundedCornerShape(13.dp)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(skinShape(RoundedCornerShape(9.dp)))
                    .background(Brush.linearGradient(listOf(NoctoriumLavender, NoctoriumPurpleStrong))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(24.dp), tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${playlist.trackCount} ${if (playlist.trackCount == 1) "track" else "tracks"} • made in Noctorium",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            IconButton({ state.copyPlaylistShareLink(playlist) }) {
                Icon(Icons.Default.Share, "Copy share link", Modifier.size(18.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LocalPlaylistDetail(playlist: LocalPlaylist, notice: String?, state: AppState) {
    var renameOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val songs = rememberLazyListState()

    if (renameOpen) {
        PlaylistNameDialog("Rename playlist", playlist.title, "Rename") { title ->
            renameOpen = false
            title?.let { state.renamePlaylist(playlist.id, it) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${playlist.title}\"?") },
            text = { Text("The playlist is removed from Noctorium. The tracks themselves are untouched.") },
            confirmButton = {
                Button({ confirmDelete = false; state.deletePlaylist(playlist.id) }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton({ confirmDelete = false }) { Text("Keep") } },
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(state::closeLocalPlaylist) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to library") }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                ScreenTitle(playlist.title, 27.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${playlist.trackCount} ${if (playlist.trackCount == 1) "track" else "tracks"} • made in Noctorium",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            if (playlist.tracks.isNotEmpty()) {
                Button({ state.playLocalPlaylist(playlist) }) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(7.dp)); Text("Play all")
                }
                val downloads by state.downloadState.collectAsState()
                // Counted over what may be kept: a Bandcamp song in the list is not one more to download.
                downloadsLeft(playlist.tracks, state::canKeep) { downloads.isDownloaded(it) }?.let { missing ->
                    Spacer(Modifier.width(9.dp))
                    OutlinedButton({ state.downloadAll(playlist.tracks) }, enabled = missing > 0) {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        // Naming how many are left makes clear that a second press is not needed, and that a
                        // playlist already kept is finished rather than the button being broken.
                        Text(if (missing == 0) "All downloaded" else "Download $missing")
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Only the songs each service can hold go up to it: a Bandcamp or Spotify song's id means nothing to
        // YouTube, and the core's own filter keeps out SoundCloud alone. See acceptsTrack.
        val forSoundCloud = playlist.tracks.filter { ProviderType.SOUNDCLOUD.acceptsTrack(it) }
        val forYouTube = playlist.tracks.filter { ProviderType.YOUTUBE_MUSIC.acceptsTrack(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton({ state.copyPlaylistShareLink(playlist) }) {
                Icon(Icons.Default.Share, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Copy share link")
            }
            OutlinedButton({ state.copyPlaylistAsText(playlist) }) {
                Icon(Icons.Default.ContentCopy, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Copy as text")
            }
            OutlinedButton({ renameOpen = true }) { Text("Rename") }
            OutlinedButton({ confirmDelete = true }) { Text("Delete") }
            if (forSoundCloud.isNotEmpty()) {
                OutlinedButton({ state.publishPlaylistToSoundCloud(playlist.copy(tracks = forSoundCloud)) }) {
                    Icon(Icons.Default.CloudUpload, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Save to SoundCloud")
                }
            }
            if (forYouTube.isNotEmpty()) {
                OutlinedButton({ state.publishPlaylistToYouTube(playlist.copy(tracks = forYouTube)) }) {
                    Icon(Icons.Default.CloudUpload, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Save to YouTube Music")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        notice?.let { LibraryNotice(it, state::clearLibraryNotice) }
        Spacer(Modifier.height(8.dp))
        Text(
            "A share link carries the whole playlist inside it, so anyone with Noctorium can paste it and get the same tracks.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(12.dp))
        if (playlist.tracks.isEmpty()) {
            EmptyScreen("Nothing here yet", "Use the ⋮ menu on any track to add it to this playlist.")
        } else {
            LazyColumn(Modifier.classicScrollbar(songs), state = songs, verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = chromePadding(bottom = 28.dp)) {
                items(playlist.tracks, key = { it.queueKey }) { track ->
                    // Rows glide to where they belong when one is taken out, instead of the rest jumping up.
                    Row(if (LocalMotion.current) Modifier.animateItem() else Modifier, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { TrackRow(track, playlist.tracks, state) }
                        IconButton({ state.removeTrackFromPlaylist(playlist.id, track.queueKey) }) {
                            Icon(Icons.Default.RemoveCircleOutline, "Remove from playlist", Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Where a new playlist should live. Noctorium always works; the services need a signed-in account. */
private enum class PlaylistDestination(val label: String) {
    SPICE("In Noctorium"),
    SOUNDCLOUD("On SoundCloud"),
    YOUTUBE("On YouTube Music"),
}

/**
 * Names a new playlist and chooses where it is created.
 *
 * A playlist made here can live only in Noctorium or go straight onto a connected account, so the choice sits
 * beside the name rather than being decided for the listener.
 */
@Composable
private fun NewPlaylistDialog(
    title: String,
    confirmLabel: String,
    soundCloudReady: Boolean,
    youTubeReady: Boolean,
    /**
     * Why a service is unavailable, when it is not the sign-in.
     *
     * A SoundCloud track cannot go in a YouTube Music playlist and the option is rightly greyed out, but
     * saying "sign in under Settings" about an account that is signed in sends somebody to check it, which
     * cost me a detour on an account that was working perfectly.
     */
    unavailableNote: String = "Sign in under Settings to use this.",
    finish: (String?, PlaylistDestination) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf(PlaylistDestination.SPICE) }

    AlertDialog(
        onDismissRequest = { finish(null, destination) },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    name,
                    { name = it.take(120) },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Create it", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PlaylistDestination.entries.forEach { option ->
                        val available = when (option) {
                            PlaylistDestination.SPICE -> true
                            PlaylistDestination.SOUNDCLOUD -> soundCloudReady
                            PlaylistDestination.YOUTUBE -> youTubeReady
                        }
                        val selected = destination == option
                        val note = when {
                            option == PlaylistDestination.SPICE -> "Stays on this computer, any service."
                            available && option == PlaylistDestination.SOUNDCLOUD ->
                                "Made on your account, private until you change it."
                            available -> "Made on your YouTube account."
                            else -> unavailableNote
                        }
                        // Option buttons with what each means beneath them, under a Windows skin.
                        if (skinned()) {
                            OptionWithNote(option.label, note, selected, available) { destination = option }
                            return@forEach
                        }
                        Surface(
                            onClick = { destination = option },
                            enabled = available,
                            shape = RoundedCornerShape(11.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f)
                            else ink(.04f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f)
                                else ink(.08f),
                            ),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected, { destination = option }, enabled = available)
                                Spacer(Modifier.width(4.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        option.label,
                                        fontSize = 13.sp,
                                        color = if (available) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
                                    )
                                    Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button({ finish(name, destination) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { OutlinedButton({ finish(null, destination) }) { Text("Cancel") } },
    )
}

@Composable
internal fun PlaylistNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    finish: (String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { finish(null) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name,
                { name = it.take(120) },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tracksTyping(),
            )
        },
        confirmButton = { Button({ finish(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { OutlinedButton({ finish(null) }) { Text("Cancel") } },
    )
}

@Composable
private fun ImportLinkDialog(finish: (String?) -> Unit) {
    var link by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { finish(null) },
        title = { Text("Paste a shared playlist") },
        text = {
            Column {
                Text(
                    "Paste a noctorium://playlist link someone sent you. The tracks travel inside the link, so nothing needs to be online.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    link,
                    { link = it },
                    label = { Text("Share link") },
                    placeholder = { Text(PlaylistShareLink.PREFIX + "…") },
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                    maxLines = 4,
                )
            }
        },
        confirmButton = { Button({ finish(link) }, enabled = link.isNotBlank()) { Text("Import") } },
        dismissButton = { OutlinedButton({ finish(null) }) { Text("Cancel") } },
    )
}

@Composable
private fun SoundCloudNamePrompt(state: AppState) {
    var username by remember { mutableStateOf("") }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.widthIn(max = 620.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cloud, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Text("One thing needed for SoundCloud", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "SoundCloud finds your playlists by profile name, and the browser session does not reveal it. " +
                    "Open your SoundCloud profile and copy the last part of the address — soundcloud.com/<name>.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    username,
                    { username = it.take(80) },
                    label = { Text("Profile name or profile link") },
                    placeholder = { Text("your-name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).tracksTyping(),
                )
                Spacer(Modifier.width(10.dp))
                Button({ state.setSoundCloudUsername(username) }, enabled = username.isNotBlank()) {
                    Icon(Icons.Default.Search, null); Spacer(Modifier.width(7.dp)); Text("Load playlists")
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "Your own sets are public, so this works whether or not the browser session is connected.",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, open: () -> Unit) {
    ListRow(onClick = open, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape = RoundedCornerShape(13.dp)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(skinShape(RoundedCornerShape(9.dp))), contentAlignment = Alignment.Center) {
                if (playlist.artworkUrl != null) {
                    RemoteArtwork(playlist.artworkUrl, playlist.provider, Modifier.fillMaxSize())
                } else {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, Modifier.size(24.dp)) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        playlist.ownerName,
                        playlist.trackCount?.let(::pluralTracks),
                    ).joinToString(" • ").ifBlank { playlist.provider.displayName },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            VisibilityBadge(playlist.isPublic, compact = true)
            Spacer(Modifier.width(7.dp))
            ProviderBadge(playlist.provider, compact = true)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun PlaylistDetail(playlist: Playlist, library: LibraryState, state: AppState) {
    val songs = rememberLazyListState()
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(state::closePlaylist) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to library") }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                ScreenTitle(playlist.title, 27.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(
                            playlist.provider.displayName,
                            playlist.ownerName,
                            playlist.tracks.size.takeIf { it > 0 }?.let { count ->
                                "$count ${if (count == 1) "track" else "tracks"}"
                            },
                        ).joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Artwork arrives in the background; a bare spinner says so without a running commentary.
                    if (library.openPlaylistEnriching) {
                        Spacer(Modifier.width(9.dp))
                        CircularProgressIndicator(Modifier.size(11.dp), strokeWidth = 1.5.dp)
                    }
                }
            }
            if (playlist.tracks.isNotEmpty()) {
                Button({ state.playPlaylist(playlist) }) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(7.dp)); Text("Play all")
                }
                val downloads by state.downloadState.collectAsState()
                // None for a Bandcamp album or artist, whose songs are bought rather than downloaded.
                downloadsLeft(playlist.tracks, state::canKeep) { downloads.isDownloaded(it) }?.let { missing ->
                    Spacer(Modifier.width(9.dp))
                    OutlinedButton({ state.downloadAll(playlist.tracks) }, enabled = missing > 0) {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        // Naming how many are left makes clear that a second press is not needed, and that a
                        // playlist already kept is finished rather than the button being broken.
                        Text(if (missing == 0) "All downloaded" else "Download $missing")
                    }
                }
            }
            // Where the download would be, for the services whose songs are not kept here: a Bandcamp release
            // or artist is a page on Bandcamp, where its music is bought, and a VK playlist one on vk.ru.
            playlist.sourceUrl?.takeIf { playlist.provider == ProviderType.BANDCAMP || playlist.provider == ProviderType.VK }?.let { page ->
                Spacer(Modifier.width(9.dp))
                OutlinedButton({ state.openExternalUrl(page) }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(servicePageLabel(playlist.provider))
                }
            }
        }
        // Account actions sit on their own line: in the title row they crowded out the name entirely.
        if (playlist.editableOnService()) {
            Spacer(Modifier.height(12.dp))
            ServicePlaylistActions(playlist, state)
        }
        Spacer(Modifier.height(16.dp))
        when {
            library.openPlaylistLoading -> LoadingSection()
            library.openPlaylistError != null -> LibraryProblem(library.openPlaylistError!!, state)
            playlist.tracks.isEmpty() -> EmptyScreen("Nothing to play", "This playlist came back empty.")
            else -> LazyColumn(
                Modifier.classicScrollbar(songs),
                state = songs,
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = chromePadding(bottom = 28.dp),
            ) {
                // The account's own YouTube playlists can be put in order from here, as on YouTube Music.
                val reorderable = playlist.editableOnService() &&
                    (playlist.provider == ProviderType.YOUTUBE_MUSIC || playlist.provider == ProviderType.YOUTUBE_VIDEO)
                itemsIndexed(playlist.tracks, key = { _, track -> track.queueKey }) { index, track ->
                    // A moved track glides to its new place, so the order visibly changes rather than reshuffling.
                    Box(if (LocalMotion.current) Modifier.animateItem() else Modifier) {
                        if (!reorderable) {
                            TrackRow(track, playlist.tracks, state)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) { TrackRow(track, playlist.tracks, state) }
                                IconButton({ state.moveInYouTubePlaylist(index, index - 1) }, enabled = index > 0, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, "Move up", Modifier.size(19.dp))
                                }
                                IconButton(
                                    { state.moveInYouTubePlaylist(index, index + 1) },
                                    enabled = index < playlist.tracks.lastIndex,
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Move down", Modifier.size(19.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Says who can see a playlist. Absent when the service has not told us, rather than guessing "public". */
@Composable
private fun VisibilityBadge(isPublic: Boolean?, compact: Boolean = false) {
    if (isPublic == null) return
    val label = if (isPublic) "Public" else "Private"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isPublic) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)
        else ink(.08f),
        border = BorderStroke(1.dp, ink(.1f)),
    ) {
        Row(
            Modifier.padding(horizontal = if (compact) 8.dp else 11.dp, vertical = if (compact) 3.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                null,
                Modifier.size(if (compact) 11.dp else 14.dp),
                tint = if (isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Rename, delete and privacy for a playlist that lives on a service account rather than only in Noctorium.
 *
 * Named service by service rather than SoundCloud-or-else-YouTube: a Bandcamp collection or a Spotify list
 * that ever got this far would otherwise have been renamed on YouTube.
 */
@Composable
private fun ServicePlaylistActions(playlist: Playlist, state: AppState) {
    when (playlist.provider) {
        ProviderType.SOUNDCLOUD -> SoundCloudPlaylistActions(playlist, state, onSoundCloud = true)
        ProviderType.YOUTUBE_MUSIC, ProviderType.YOUTUBE_VIDEO -> SoundCloudPlaylistActions(playlist, state, onSoundCloud = false)
        // Read-only here: nothing of theirs is renamed, deleted or made public from Noctorium.
        ProviderType.SPOTIFY, ProviderType.BANDCAMP, ProviderType.VK, ProviderType.LOCAL -> Unit
    }
}

@Composable
private fun SoundCloudPlaylistActions(playlist: Playlist, state: AppState, onSoundCloud: Boolean = true) {
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (renaming) {
        PlaylistNameDialog("Rename on ${playlist.provider.displayName}", playlist.title, "Rename") { title ->
            renaming = false
            title?.let {
                if (onSoundCloud) state.renameSoundCloudPlaylist(playlist.id, it)
                else state.renameYouTubePlaylist(playlist.id, it)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${playlist.title}\" from ${playlist.provider.displayName}?") },
            text = {
                Text(
                    "This removes the playlist from your ${playlist.provider.displayName} account, not just from " +
                        "Noctorium. The tracks themselves are untouched.",
                )
            },
            confirmButton = {
                Button({
                    confirmDelete = false
                    if (onSoundCloud) state.deleteSoundCloudPlaylist(playlist.id) else state.deleteYouTubePlaylist(playlist.id)
                }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton({ confirmDelete = false }) { Text("Keep") } },
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        VisibilityBadge(playlist.isPublic)
        // Only offered when SoundCloud has actually told us the current state, so the button cannot claim to
        // flip something whose value is unknown.
        playlist.isPublic?.let { isPublic ->
            OutlinedButton({
                if (onSoundCloud) state.setSoundCloudPlaylistVisibility(playlist.id, !isPublic)
                else state.setYouTubePlaylistVisibility(playlist.id, !isPublic)
            }) {
                Icon(
                    if (isPublic) Icons.Default.Lock else Icons.Default.Public,
                    null,
                    Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(if (isPublic) "Make private" else "Make public")
            }
        }
        OutlinedButton({ renaming = true }) {
            Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("Rename")
        }
        OutlinedButton({ confirmDelete = true }) { Text("Delete") }
    }
}

@Composable
private fun LibraryProblem(message: String, state: AppState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 520.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(12.dp))
            Text("Playlists could not be loaded", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            SelectionContainer {
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button({ state.navigate(Destination.SETTINGS) }) {
                    Icon(Icons.Default.Settings, null); Spacer(Modifier.width(7.dp)); Text("Open settings")
                }
                OutlinedButton({ state.refreshLibrary(force = true) }) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun LibraryProblemBanner(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .35f), shape = RoundedCornerShape(11.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WarningAmber, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(9.dp))
            Text(message, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun SearchScreen(ui: AppUiState, state: AppState, focusRequest: Int = 0) {
    val box = remember { FocusRequester() }
    // Zero is the screen simply being opened, which should not steal the caret from somebody who came
    // here with the mouse. Every value after that is the shortcut asking for it.
    LaunchedEffect(focusRequest) { if (focusRequest > 0) runCatching { box.requestFocus() } }

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        ScreenTitle("Search", 32.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = ui.searchQuery,
            onValueChange = state::search,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Artists, songs, albums and playlists") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).focusRequester(box).tracksTyping(),
        )
        Spacer(Modifier.height(12.dp))
        SearchModePicker(ui.searchMode, state::setSearchMode)
        Spacer(Modifier.height(18.dp))
        val settings by state.settings.collectAsState()
        when {
            ui.searchLoading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.searchQuery.isBlank() -> EmptyScreen(
                "Search ${ui.searchMode.displayName}",
                searchHint(ui.searchMode, settings.spotify, settings.vk, settings.preferences.hybridSearch),
            )
            ui.errorMessage != null -> PlaybackError(ui.errorMessage.orEmpty())
            // Spotify answers a search with nothing at all when nobody is signed in, which would otherwise
            // read as Spotify having nothing by that name.
            ui.searchMode == SearchMode.SPOTIFY && !settings.spotify.connected -> EmptyScreen(
                "Spotify is not connected",
                searchHint(ui.searchMode, settings.spotify, settings.vk, settings.preferences.hybridSearch),
            )
            else -> SearchResultsList(ui.searchResults, state)
        }
    }
}

/** "A", "A and B", "A, B and C": services named in a sentence. */
internal fun servicesLine(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.single()
    else -> names.dropLast(1).joinToString() + " and " + names.last()
}

/** Every service a Hybrid search can ask, in the order the choice of them is shown. The core's own list. */
internal val HYBRID_SEARCH_SERVICES: List<ProviderType> = DEFAULT_HYBRID_SEARCH.toList()

/** A service as Hybrid search names it: YouTube's videos by what they are, to tell them from YouTube Music. */
internal fun hybridServiceName(provider: ProviderType): String =
    if (provider == ProviderType.YOUTUBE_VIDEO) "YouTube videos" else provider.displayName

/**
 * What the search page says before anything is typed: which services a search asks, or what one of them
 * needs first.
 *
 * Mixed search asks the services chosen for it in Settings, and of those, Spotify only when its songs play
 * on Spotify -- matched, each would be a slower copy of the YouTube Music result beside it -- and VK only for
 * an account signed in to it. Those are the core's rules; this says them, so the line names exactly the
 * services that will answer.
 */
internal fun searchHint(
    mode: SearchMode,
    spotify: SpotifyConnectionState,
    vk: VkConnectionState,
    hybrid: Set<ProviderType> = DEFAULT_HYBRID_SEARCH,
): String = when {
    mode == SearchMode.HYBRID -> {
        val answering = HYBRID_SEARCH_SERVICES.filter { service ->
            service in hybrid &&
                (service != ProviderType.SPOTIFY || spotify.playsOnSpotify) &&
                (service != ProviderType.VK || vk.connected)
        }
        if (answering.isEmpty()) {
            "None of the services chosen for Hybrid search can answer yet. Choose them under Settings › Customization."
        } else {
            servicesLine(answering.map(::hybridServiceName)) + " results appear together."
        }
    }
    mode == SearchMode.SPOTIFY && !spotify.connected -> "Connect Spotify under Settings › Spotify library to search it."
    mode == SearchMode.VK && !vk.connected -> "Sign in to VK under Settings › VK Music to search it."
    else -> "Only ${mode.displayName} results will appear."
}

/**
 * What a search found: albums and artists on a shelf of their own, then the songs.
 *
 * The shelf is Home's, cards and all, because these open rather than play, which is what a card on Home
 * already means; mixed in among the songs, one click would play and the next would go somewhere else. Only
 * Bandcamp and Spotify answer with albums and artists so far, so with nothing from either the page is the
 * list of songs it always was, without a heading over it. The line under the shelf names whichever of them
 * did answer.
 */
@Composable
private fun SearchResultsList(results: SearchResults, state: AppState) {
    // The cards are keyed, and a service that listed the same album twice would otherwise end the screen.
    val shelf = remember(results.playlists) { results.playlists.distinctBy { it.playlistKey } }
    val list = rememberLazyListState()
    LazyColumn(Modifier.classicScrollbar(list), state = list, verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = chromePadding()) {
        if (shelf.isNotEmpty()) {
            item(key = "search:shelf") {
                PlaylistRowSection(
                    "Albums and artists",
                    "From " + servicesLine(shelf.map { it.provider.displayName }.distinct()),
                    shelf,
                    state,
                )
            }
            if (results.tracks.isNotEmpty()) {
                item(key = "search:songs") {
                    Text("Songs", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 7.dp))
                }
            }
        }
        items(results.tracks, key = { it.queueKey }) { track ->
            TrackRow(track, results.tracks, state)
        }
    }
}

@Composable
private fun SearchModePicker(selected: SearchMode, select: (SearchMode) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SearchMode.entries, key = { it.name }) { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { select(mode) },
                label = { Text(mode.displayName) },
                leadingIcon = if (mode == SearchMode.HYBRID) {
                    { Icon(Icons.Default.Hub, null, Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}

@Composable
private fun TrackRow(track: Track, sourceQueue: List<Track>, state: AppState) {
    ListRow(
        onClick = { state.play(track, PlaybackOrigin.SEARCH, sourceQueue) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .36f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(48.dp).clip(skinShape(RoundedCornerShape(9.dp))))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.SemiBold)
                Text(track.artistLine, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            ProviderBadge(track.provider)
            DownloadButton(track, state, size = 34.dp)
            TrackMenu(track, state)
        }
    }
}

/**
 * What a heart does to a song, in the words of the service it goes to.
 *
 * A heart is a like on YouTube and SoundCloud, but on VK it adds the song to My music and on Spotify it saves
 * it to Liked Songs, and saying so is what tells somebody where to find it afterwards.
 */
internal fun likeLabel(provider: ProviderType, liked: Boolean): String = when (provider) {
    ProviderType.VK -> if (liked) "Remove from My music on VK" else "Add to My music on VK"
    ProviderType.SPOTIFY -> if (liked) "Remove from Liked Songs on Spotify" else "Save to Liked Songs on Spotify"
    else -> if (liked) "Remove from your ${provider.displayName} likes" else "Like on ${provider.displayName}"
}

/**
 * Why a heart cannot be pressed: usually a sign-in still to make, but Bandcamp has no likes to make, and
 * telling somebody to sign in to a service with nothing to sign in to would send them looking for it.
 */
internal fun likeUnavailable(provider: ProviderType): String = when (provider) {
    ProviderType.BANDCAMP -> "Bandcamp has no likes; its wishlist is kept on Bandcamp"
    ProviderType.LOCAL -> "Files on this computer cannot be liked"
    else -> "Sign in to ${provider.displayName} to like tracks"
}

/**
 * Writes the like to the provider account, not just to Noctorium, so the heart reflects what the service holds.
 *
 * Which services can be written to is [LikeState.supports]'s decision, not this button's: naming SoundCloud
 * here is what left the heart permanently dead on YouTube Music long after YouTube liking worked.
 */
@Composable
internal fun LikeButton(track: Track, state: AppState, size: Dp = 36.dp) {
    val likes by state.likes.collectAsState()
    val liked = likes.isLiked(track)
    val busy = likes.isBusy(track)
    val supported = likes.supports(track)
    // The heart pops as it fills, so a like is seen to land and not only to change colour.
    IconButton({ state.toggleLike(track) }, Modifier.size(size).popOn(liked, pop = liked), enabled = supported && !busy) {
        when {
            busy -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            liked -> Icon(
                Icons.Default.Favorite,
                likeLabel(track.provider, liked = true),
                tint = MaterialTheme.colorScheme.primary,
            )
            else -> Icon(
                Icons.Default.FavoriteBorder,
                if (supported) likeLabel(track.provider, liked = false) else likeUnavailable(track.provider),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (supported) 1f else .4f),
            )
        }
    }
}

@Composable
private fun TrackMenu(track: Track, state: AppState) {
    var expanded by remember { mutableStateOf(false) }
    var addToPlaylistOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    val library by state.library.collectAsState()
    val likes by state.likes.collectAsState()
    val ui by state.ui.collectAsState()
    val likedNow = likes.isLiked(track)
    val pinned = ui.pinnedTracks.any { it.queueKey == track.queueKey }
    Box {
        IconButton({ expanded = true }, Modifier.size(36.dp)) {
            Icon(Icons.Default.MoreVert, "Track actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Play next") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) },
                onClick = { state.playNext(track); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Add to queue") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                onClick = { state.addToQueue(track); expanded = false },
            )
            if (likes.supports(track)) {
                DropdownMenuItem(
                    text = { Text(likeLabel(track.provider, likedNow)) },
                    leadingIcon = {
                        Icon(if (likedNow) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    },
                    onClick = { state.toggleLike(track); expanded = false },
                )
            }
            val downloads by state.downloadState.collectAsState()
            // A Spotify track has no audio of its own, so what is on the disk is filed under the recording
            // matched to it. Asking about the Spotify entry would report it as never downloaded.
            val onDisk = state.downloadableTrack(track)
            val job = downloads.jobFor(onDisk)
            when {
                // A Bandcamp or VK song is neither downloaded nor saved -- the file is the artist's to sell, or
                // VK's to license -- so its own page takes the place of both: where a Bandcamp song is bought,
                // and where a VK one lives. The page alone: the address kept for the song carries what finds
                // its stream, which means nothing in a browser.
                !state.canKeep(track) -> DropdownMenuItem(
                    text = { Text(servicePageLabel(track.provider)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                    trailingIcon = servicePageHint(track.provider)?.let { hint ->
                        { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                    },
                    onClick = { state.openExternalUrl(track.pageUrl); expanded = false },
                )
                // In progress, and tapping it stops it. A download nobody can call off is worse than none.
                job != null && job.stage != DownloadStage.FAILED -> DropdownMenuItem(
                    text = { Text("Downloading… ${(job.progress * 100).roundToInt()}%") },
                    leadingIcon = { Icon(Icons.Default.Downloading, null) },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                    onClick = { state.cancelDownload(onDisk.queueKey); expanded = false },
                )
                job != null -> DropdownMenuItem(
                    text = { Text("Download failed — try again") },
                    leadingIcon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { state.cancelDownload(onDisk.queueKey); state.downloadTrack(track); expanded = false },
                )
                downloads.isDownloaded(onDisk) -> DropdownMenuItem(
                    text = { Text("Remove download") },
                    leadingIcon = { Icon(Icons.Default.DownloadDone, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { state.deleteDownload(onDisk.queueKey); expanded = false },
                )
                else -> DropdownMenuItem(
                    text = { Text("Download for offline") },
                    leadingIcon = { Icon(Icons.Default.Download, null) },
                    onClick = { state.downloadTrack(track); expanded = false },
                )
            }
            if (state.canKeep(track)) {
                DropdownMenuItem(
                    text = { Text(if (state.canSaveAsMp3()) "Save as MP3…" else "Save a copy…") },
                    leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                    // Named for where it lands, because the difference between this and the item above it is
                    // exactly that one is Noctorium's copy and this one is the listener's.
                    trailingIcon = {
                        Text(
                            state.exportFolder()?.fileName?.toString() ?: "no folder",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    },
                    onClick = { state.exportTrack(track); expanded = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (pinned) "Unpin from Home" else "Pin to Home") },
                leadingIcon = { Icon(if (pinned) Icons.Default.PushPin else Icons.Outlined.PushPin, null) },
                onClick = { state.togglePin(track); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Edit details…") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { expanded = false; editOpen = true },
            )
            DropdownMenuItem(
                text = { Text("Copy link") },
                leadingIcon = { Icon(Icons.Default.Link, null) },
                onClick = { state.copyTrackLink(track); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Add to playlist") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                trailingIcon = { Icon(Icons.Default.ChevronRight, null, Modifier.size(17.dp)) },
                onClick = { expanded = false; addToPlaylistOpen = true },
            )
        }
        if (addToPlaylistOpen) {
            AddToPlaylistDialog(track, library.localPlaylists, state) { addToPlaylistOpen = false }
        }
        if (editOpen) {
            EditTrackDialog(track, ui.trackEdits[track.queueKey], state) { editOpen = false }
        }
    }
}

/**
 * Where the listener corrects what a service calls a track.
 *
 * Uploader titles are the reason this exists: "Artist - Song (Official Video) [4K]" credited to a channel.
 * Both fields start as whatever is showing now; a field left blank keeps the service's own, and the
 * third button forgets the edit altogether. Nothing here is sent anywhere -- it is this device's opinion.
 */
@Composable
private fun EditTrackDialog(track: Track, existing: TrackEdit?, state: AppState, dismiss: () -> Unit) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artistLine) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Edit details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Changes what this track is called here, in the queue, in lyrics searches and in what is " +
                        "scrobbled. Only on this device; the service is not told.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth().tracksTyping())
                OutlinedTextField(artist, { artist = it }, label = { Text("Artist") }, singleLine = true, modifier = Modifier.fillMaxWidth().tracksTyping())
            }
        },
        confirmButton = {
            TextButton({
                // A field the listener left as it was is not an edit; only what they changed is kept.
                state.editTrack(
                    track,
                    title.takeIf { it.trim() != track.title.trim() || existing?.title != null },
                    artist.takeIf { it.trim() != track.artistLine.trim() || existing?.artist != null },
                )
                dismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton({ state.clearTrackEdit(track); dismiss() }) { Text("Use the service's") }
                }
                TextButton(dismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun AddToPlaylistDialog(
    track: Track,
    playlists: List<LocalPlaylist>,
    state: AppState,
    dismiss: () -> Unit,
) {
    val library by state.library.collectAsState()
    // Playlists on the account itself, which Noctorium can write to. The likes page is a listing, not a
    // playlist, so it is excluded by requiring a numeric id.
    val servicePlaylists = library.playlists.filter { it.editableOnService() && it.provider.acceptsTrack(track) }
    // Straight to making one only when there is genuinely nothing to add to. Counting the Noctorium
    // playlists alone was wrong, and wrong in a way that hid a whole feature: on a machine with none of
    // those -- an ordinary one -- every "Add to playlist" opened the create form, and the playlists on the
    // account, sitting right there in the library, could never be picked.
    var creating by remember { mutableStateOf(playlists.isEmpty() && servicePlaylists.isEmpty()) }
    if (creating) {
        val likes by state.likes.collectAsState()
        NewPlaylistDialog(
            title = "New playlist for ${track.title}",
            confirmLabel = "Create and add",
            // A SoundCloud playlist can only hold SoundCloud tracks, so the option is offered only when the
            // track being added could actually go in one -- and the same for YouTube, which used to be offered
            // anything that was not SoundCloud's, a Bandcamp song included.
            soundCloudReady = likes.soundCloudReady && ProviderType.SOUNDCLOUD.acceptsTrack(track),
            youTubeReady = likes.youTubeReady && ProviderType.YOUTUBE_MUSIC.acceptsTrack(track),
            // Here the other service is greyed out because the track cannot go there, whatever the state
            // of that account, so the note says that rather than blaming the sign-in.
            unavailableNote = "Not where a ${track.provider.displayName} track can go.",
        ) { name, destination ->
            dismiss()
            if (name != null) {
                when (destination) {
                    PlaylistDestination.SOUNDCLOUD -> state.createSoundCloudPlaylist(name, listOf(track))
                    PlaylistDestination.YOUTUBE -> state.createYouTubePlaylist(name, listOf(track))
                    else -> state.createPlaylist(name, firstTrack = track)
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (servicePlaylists.isNotEmpty()) {
                    item {
                        Text(
                            "ON ${track.provider.displayName.uppercase()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(servicePlaylists, key = { "svc:${it.id}" }) { playlist ->
                        Surface(
                            onClick = {
                                dismiss()
                                // The core names each service, and turns down the ones that cannot be written to.
                                state.addTrackToPlaylist(playlist, track)
                            },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cloud, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(playlist.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        playlist.trackCount?.let { "$it tracks • on your account" } ?: "on your account",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "IN SPICE",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                items(playlists, key = { it.id }) { playlist ->
                    Surface(
                        onClick = { dismiss(); state.addTrackToPlaylist(playlist.id, track) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    pluralTracks(playlist.trackCount ?: 0),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                )
                            }
                            if (playlist.tracks.any { it.queueKey == track.queueKey }) {
                                Icon(Icons.Default.Check, "Already added", Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button({ creating = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("New playlist") } },
        dismissButton = { OutlinedButton(dismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun ProviderBadge(provider: ProviderType, compact: Boolean = false) {
    val background = when (provider) {
        ProviderType.YOUTUBE_MUSIC -> Color(0xFF8B2AB8)
        ProviderType.YOUTUBE_VIDEO -> Color(0xFFB32C35)
        ProviderType.SOUNDCLOUD -> Color(0xFFC45A16)
        ProviderType.SPOTIFY -> Color(0xFF1DB954)
        ProviderType.BANDCAMP -> Color(0xFF408294)
        ProviderType.VK -> Color(0xFF0077FF)
        ProviderType.LOCAL -> Color(0xFF4F46E5)
    }
    val label = when (provider) {
        ProviderType.YOUTUBE_MUSIC -> if (compact) "YTM" else "YT MUSIC"
        ProviderType.YOUTUBE_VIDEO -> if (compact) "YT" else "YOUTUBE"
        ProviderType.SOUNDCLOUD -> if (compact) "SC" else "SOUNDCLOUD"
        ProviderType.SPOTIFY -> if (compact) "SP" else "SPOTIFY"
        ProviderType.BANDCAMP -> if (compact) "BC" else "BANDCAMP"
        ProviderType.VK -> if (compact) "VK" else "VK MUSIC"
        ProviderType.LOCAL -> if (compact) "LOCAL" else "LOCAL"
    }
    Text(
        label,
        color = Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(skinCorners(RoundedCornerShape(6.dp))).background(background.copy(alpha = .9f)).padding(horizontal = if (compact) 6.dp else 7.dp, vertical = 4.dp),
    )
}

/**
 * Volume, boost, mute and speed in one popover, so the bar carries a single icon instead of five controls.
 *
 * The volume and mute reach Spotify's own app when that is what is playing, and Spotify turns its device
 * up and down; the boost and the speed belong to this computer's player, and rest until a song plays here
 * again.
 */
@Composable
internal fun VolumeControl(playback: PlaybackState, state: AppState, size: Dp = 36.dp) {
    var open by remember { mutableStateOf(false) }
    val onSpotify = playsOnSpotify(playback.track, state.settings.collectAsState().value.spotify)
    Box {
        IconButton({ open = true }, Modifier.size(size)) {
            Icon(
                if (playback.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                "Volume",
                // Lit for the boost, not for a volume above unity: there is no longer such a thing, so
                // that test was left never being true and the closed button stopped saying anything.
                tint = if (playback.isMuted || playback.volumeBoostEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        DropdownMenu(open, { open = false }) {
            Column(Modifier.width(248.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Volume", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        "${(playback.volume * 100).roundToInt()}%",
                        color = if (playback.volumeBoostEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                // Straight through. The slider used to stretch to ten times unity when the boost was on,
                // which was only ever a way of asking mpv to clip harder; the boost is a filter now and
                // the slider means one thing.
                Slider(
                    value = playback.volume.coerceIn(0f, 1f),
                    onValueChange = state::setVolume,
                    valueRange = 0f..1f,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = playback.isMuted,
                        onClick = state::toggleMute,
                        label = { Text(if (playback.isMuted) "Muted" else "Mute", fontSize = 11.sp) },
                        modifier = Modifier.height(30.dp),
                    )
                    FilterChip(
                        selected = playback.volumeBoostEnabled,
                        onClick = state::toggleVolumeBoost,
                        label = { Text("Boost", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Bolt, null, Modifier.size(14.dp)) },
                        enabled = !onSpotify,
                        modifier = Modifier.height(30.dp),
                    )
                }
                // The speed sits with the volume because both are reached for mid-song, and a podcast is the
                // likeliest reason to want either.
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = ink(.08f))
                SpeedControl(
                    state.settings.collectAsState().value.preferences.playbackSpeed,
                    onSpotify,
                    compact = true,
                    set = state::setPlaybackSpeed,
                )
                if (onSpotify) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Spotify plays this song in its own app, so the speed, the boost and the equaliser wait for " +
                            "the next song played here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/**
 * One row, left to right: transport, the track, the seek bar, then the tools. Denser than the stacked layout
 * and it gives the rest of the window back about thirty pixels.
 */
@Composable
private fun InlinePlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val preferences = state.settings.collectAsState().value.preferences
    val current = queue.current
    var addToPlaylist by remember { mutableStateOf(false) }
    val library by state.library.collectAsState()
    val shown = playerBarButtons(PlayerBarStyle.INLINE, narrow = false, preferences.desktop.hiddenPlayerButtons, playerButtonsInUse(state))

    if (addToPlaylist && current != null) {
        AddToPlaylistDialog(current, library.localPlaylists, state) { addToPlaylist = false }
    }

    // The rule separates the bar from what it sits against, so it belongs on whichever edge faces the
    // content: below the bar when the bar is at the top of the window, above it when it is at the foot.
    val atTop = preferences.playerBarPosition == PlayerBarPosition.TOP
    val rule = MaterialTheme.colorScheme.primary.copy(alpha = .22f)
    Surface(color = if (LocalInGlass.current) Color.Transparent else MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().height(74.dp)) {
        Column {
            if (!atTop && !LocalInGlass.current) HorizontalDivider(color = rule)
            Row(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (PlayerButton.SHUFFLE in shown) {
                    IconButton(state::toggleShuffle, Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.Shuffle,
                            "Shuffle",
                            Modifier.size(18.dp),
                            tint = if (queue.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(state::previous, Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous track", Modifier.size(20.dp))
                }
                // The one filled control in the bar, so the eye lands on it first.
                FilledIconButton(
                    state::togglePlayback,
                    Modifier.size(42.dp),
                    // Pressable while it spins: that press gives up on the track, which is the only way
                    // out of a resolve that is not going to finish.
                    enabled = current != null,
                ) {
                    if (playback.status == PlaybackStatus.RESOLVING) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        PlayPauseIcon(playback.isPlaying, Modifier.size(24.dp))
                    }
                }
                // Lit while next leads somewhere, autoplay's lined-up songs included: see QueueState.hasNext.
                IconButton(state::next, Modifier.size(34.dp), enabled = queue.hasNext) {
                    Icon(Icons.Default.SkipNext, "Next track", Modifier.size(20.dp))
                }
                if (PlayerButton.REPEAT in shown) {
                    IconButton(state::cycleRepeat, Modifier.size(34.dp)) {
                        Icon(
                            if (queue.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            "Repeat",
                            Modifier.size(18.dp),
                            tint = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                current?.let { track ->
                    if (PlayerButton.LIKE in shown) LikeButton(track, state, size = 34.dp)
                    DownloadButton(track, state, size = 34.dp)
                    IconButton({ addToPlaylist = true }, Modifier.size(34.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            "Add to playlist",
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))
                Row(
                    Modifier.width(230.dp).clickable(enabled = current != null) { state.navigate(Destination.NOW_PLAYING) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (current != null) {
                        RemoteArtwork(current.artworkUrl, current.provider, Modifier.size(40.dp).clip(skinShape(RoundedCornerShape(7.dp))))
                    } else {
                        Box(
                            Modifier.size(40.dp).clip(skinShape(RoundedCornerShape(7.dp))).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.MusicNote, null, Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            current?.title ?: "Nothing playing",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        BarArtistLine(current, playback, state, 11.sp)
                    }
                }

                Spacer(Modifier.width(16.dp))
                PlaybackProgressBar(
                    playback = playback,
                    onSeek = state::seekTo,
                    modifier = Modifier.weight(1f),
                    style = preferences.progressBarStyle,
                    timeDisplay = preferences.timeDisplay,
                )
                Spacer(Modifier.width(12.dp))

                // Outside the badge, not inside it. BadgedBox has one content slot: a second child in
                // there is laid on top of the first, so this button was drawn over the queue button and
                // every click on it went to the queue instead.
                if (PlayerButton.DEVICES in shown) ConnectButton(state)
                if (PlayerButton.SLEEP_TIMER in shown) SleepTimerButton(state)
                if (PlayerButton.QUEUE in shown) {
                    BadgedBox(badge = { if (queue.tracks.isNotEmpty()) Badge { Text(queue.tracks.size.toString()) } }) {
                        IconButton({ state.navigate(Destination.QUEUE) }, Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", Modifier.size(19.dp))
                        }
                    }
                }
                if (PlayerButton.LYRICS in shown) {
                    IconButton({ state.navigate(Destination.NOW_PLAYING) }, Modifier.size(34.dp)) {
                        Icon(Icons.Default.Lyrics, "Lyrics", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (PlayerButton.VOLUME in shown) VolumeControl(playback, state)
            }
            if (atTop && !LocalInGlass.current) HorizontalDivider(color = rule)
        }
    }
}

/**
 * The line under the song's title in the Inline and Stacked bars: its artist, or what went wrong, in red --
 * and the On Spotify tag after it when Spotify's own app is the one playing.
 */
@Composable
private fun BarArtistLine(current: Track?, playback: PlaybackState, state: AppState, fontSize: TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            playback.errorMessage ?: current?.artistLine ?: "Choose a track to start",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (playback.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = fontSize,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (playsOnSpotify(current, state.settings.collectAsState().value.spotify)) {
            Spacer(Modifier.width(6.dp))
            OnSpotifyMark()
        }
    }
}

@Composable
internal fun PlayerBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    when (state.settings.collectAsState().value.preferences.playerBarStyle) {
        PlayerBarStyle.INLINE -> return InlinePlayerBar(queue, playback, state)
        PlayerBarStyle.CENTERED -> return CenteredPlayerBar(queue, playback, state)
        PlayerBarStyle.SLIM -> return SlimPlayerBar(queue, playback, state)
        PlayerBarStyle.SLIM_LEFT -> return SlimPlayerBar(queue, playback, state, controlsFirst = true)
        PlayerBarStyle.SPOTLIGHT -> return SpotlightPlayerBar(queue, playback, state)
        PlayerBarStyle.TASKBAR -> return TaskbarPlayerBar(queue, playback, state)
        // Not drawn on this branch yet: each stands in as the nearest layout there is.
        PlayerBarStyle.FLOATING -> return InlinePlayerBar(queue, playback, state)
        PlayerBarStyle.ISLAND -> return SlimPlayerBar(queue, playback, state)
        PlayerBarStyle.DISPLAY -> return CenteredPlayerBar(queue, playback, state)
        // Stacked is the one written out below, and was the only layout before there was a choice.
        PlayerBarStyle.STACKED -> Unit
    }
    val current = queue.current
    val playerPreferences = state.settings.collectAsState().value.preferences
    val progressStyle = playerPreferences.progressBarStyle
    val timeDisplay = playerPreferences.timeDisplay
    val stackedAtTop = playerPreferences.playerBarPosition == PlayerBarPosition.TOP
    val stackedRule = MaterialTheme.colorScheme.primary.copy(alpha = .35f)
    Surface(
        shadowElevation = 0.dp,
        color = if (LocalInGlass.current) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable(
            enabled = current != null,
            onClickLabel = "Open now playing",
            onClick = { state.navigate(Destination.NOW_PLAYING) },
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp
            val shown = playerBarButtons(PlayerBarStyle.STACKED, narrow = compact, playerPreferences.desktop.hiddenPlayerButtons, playerButtonsInUse(state))
            Column {
                // As above: the rule marks the edge the content is on.
                if (!stackedAtTop && !LocalInGlass.current) HorizontalDivider(color = stackedRule)
                PlaybackProgressBar(
                    playback = playback,
                    onSeek = state::seekTo,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 18.dp),
                    style = progressStyle,
                    timeDisplay = timeDisplay,
                )
                Row(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = if (compact) 10.dp else 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (!compact) {
                            if (current != null) {
                                RemoteArtwork(current.artworkUrl, current.provider, Modifier.size(52.dp).clip(skinShape(RoundedCornerShape(11.dp))))
                            } else {
                                Box(
                                    Modifier.size(52.dp).clip(skinShape(RoundedCornerShape(11.dp))).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Default.MusicNote, null) }
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                current?.title ?: "Nothing playing",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                            BarArtistLine(current, playback, state, 12.sp)
                        }
                        if (PlayerButton.LIKE in shown) current?.let { LikeButton(it, state) }
                        current?.let { DownloadButton(it, state) }
                    }

                    Row(
                        Modifier.width(if (compact) 236.dp else 270.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (PlayerButton.SHUFFLE in shown) {
                            IconButton(state::toggleShuffle, Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    "Shuffle",
                                    tint = if (queue.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        FilledTonalIconButton(state::previous, Modifier.size(40.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Previous track")
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            state::togglePlayback,
                            Modifier.size(50.dp),
                            enabled = current != null,
                        ) {
                            if (playback.status == PlaybackStatus.RESOLVING) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                PlayPauseIcon(playback.isPlaying, Modifier.size(30.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(state::next, Modifier.size(40.dp), enabled = queue.hasNext) {
                            Icon(Icons.Default.SkipNext, "Next track")
                        }
                        if (PlayerButton.REPEAT in shown) {
                            IconButton(state::cycleRepeat, Modifier.size(36.dp)) {
                                Icon(
                                    if (queue.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    when (queue.repeatMode) {
                                        RepeatMode.OFF -> "Repeat off"
                                        RepeatMode.ALL -> "Repeat all"
                                        RepeatMode.ONE -> "Repeat one"
                                    },
                                    tint = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Volume, its readout and the boost toggle used to sit permanently in the bar. They now
                        // live behind the speaker icon, which is the only one of the four you reach for often.
                        // A compact bar leaves it off; playerBarButtons says so along with the rest.
                        if (PlayerButton.VOLUME in shown) {
                            VolumeControl(playback, state)
                            Spacer(Modifier.width(4.dp))
                        }
                        if (PlayerButton.DEVICES in shown) ConnectButton(state)
                        if (PlayerButton.SLEEP_TIMER in shown) SleepTimerButton(state)
                        if (PlayerButton.QUEUE in shown) {
                            BadgedBox(
                                badge = {
                                    if (queue.tracks.isNotEmpty()) Badge { Text(queue.tracks.size.toString()) }
                                },
                            ) {
                                IconButton({ state.navigate(Destination.QUEUE) }) {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Open queue")
                                }
                            }
                        }
                    }
                }
                if (stackedAtTop && !LocalInGlass.current) HorizontalDivider(color = stackedRule)
            }
        }
    }
}

/** What is on screen: the layout, whether its panel is out, and whether the window is wide enough for both. */
private data class NowPlayingArrangement(val layout: NowPlayingLayout, val panel: Boolean, val wide: Boolean)

/** How the track, its controls and its cover are set out, within whatever room the layout gives them. */
private enum class HeroArrangement {
    /** Title and controls above the cover, the way the screen always was. */
    STANDARD,

    /** The cover first, everything else centred beneath it. */
    CENTRED,

    /** The cover large, with the track and the controls in a column beside it. */
    BESIDE,

    /** One band: a small cover and everything else in a row after it. */
    BANNER,
}

/** The column beside the lyrics in the sing-along layout. */
private val SING_ALONG_COLUMN = 380.dp

@Composable
internal fun NowPlayingScreen(queue: QueueState, playback: PlaybackState, state: AppState) {
    val current = queue.current
    val preferences = state.settings.collectAsState().value.preferences
    val look = preferences.desktop.nowPlaying
    var selectedTab by remember {
        mutableStateOf(if (look.layout == NowPlayingLayout.SING_ALONG) NowPlayingTab.LYRICS else look.openOn)
    }
    var arranging by remember { mutableStateOf(false) }
    // Choosing the sing-along layout brings the lyrics forward, which is what it is for.
    LaunchedEffect(look.layout) {
        if (look.layout == NowPlayingLayout.SING_ALONG) selectedTab = NowPlayingTab.LYRICS
    }

    if (current == null) {
        EmptyScreen("Nothing playing", "Choose a track, then click the player to open this view.")
        return
    }

    Box(Modifier.fillMaxSize()) {
        // The backdrop takes its colour from the cover, so the room changes with the record.
        NowPlayingBackground(current, preferences.nowPlayingBackdrop)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val arrangement = NowPlayingArrangement(
                layout = look.layout,
                panel = look.layout.hasPanel && !look.panelHidden,
                wide = maxWidth >= 840.dp,
            )
            val panel: @Composable (Modifier, Boolean) -> Unit = { modifier, largeLyrics ->
                GlassPanel(modifier, selectedTab.displayName) {
                    NowPlayingPanel(
                        queue = queue,
                        selectedTab = selectedTab,
                        selectTab = { selectedTab = it },
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        largeLyrics = largeLyrics,
                    )
                }
            }
            // One arrangement easing into the next, so a change made from the menu is seen happening.
            MotionContent(arrangement, Modifier.fillMaxSize()) { shown ->
                NowPlayingArranged(shown, current, playback, state, look.panelWidth.widthDp.dp, { arranging = true }, panel, queue.hasNext)
            }
        }
        // Anchored to the top right whatever the layout, so the menu stays put while the screen rearranges
        // behind it, and each choice can be seen before the next is made.
        Box(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 20.dp)) {
            DropdownMenu(arranging, { arranging = false }, Modifier.width(384.dp)) {
                NowPlayingArranger(look, preferences.nowPlayingBackdrop, state) { arranging = false }
            }
        }
    }
}

@Composable
private fun NowPlayingArranged(
    shown: NowPlayingArrangement,
    track: Track,
    playback: PlaybackState,
    state: AppState,
    panelWidth: Dp,
    arrange: () -> Unit,
    panel: @Composable (Modifier, Boolean) -> Unit,
    hasNext: Boolean,
) {
    val layout = shown.layout
    val hero: @Composable (HeroArrangement, Modifier) -> Unit = { heroArrangement, modifier ->
        // In a window of its own on the desktop, under a Windows skin.
        HeroWindow(modifier, track) { inner -> NowPlayingHero(track, playback, state, heroArrangement, arrange, inner, hasNext) }
    }
    when {
        layout == NowPlayingLayout.BANNER -> Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (!shown.panel) Spacer(Modifier.weight(1f))
            hero(HeroArrangement.BANNER, Modifier.fillMaxWidth())
            if (shown.panel) panel(Modifier.fillMaxWidth().weight(1f), false) else Spacer(Modifier.weight(1f))
        }
        layout == NowPlayingLayout.STAGE && shown.wide -> hero(HeroArrangement.BESIDE, Modifier.fillMaxSize())
        layout == NowPlayingLayout.STAGE || layout == NowPlayingLayout.FOCUS ->
            hero(HeroArrangement.CENTRED, Modifier.fillMaxSize())
        // The panel tucked away: the record takes the middle, rather than a seek bar the width of the window.
        !shown.panel -> hero(HeroArrangement.CENTRED, Modifier.fillMaxSize())
        !shown.wide -> Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            hero(HeroArrangement.STANDARD, Modifier.fillMaxWidth().height(360.dp))
            panel(Modifier.fillMaxWidth().weight(1f), false)
        }
        else -> Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            when (layout) {
                NowPlayingLayout.PANEL_LEFT -> {
                    panel(Modifier.width(panelWidth).fillMaxHeight(), false)
                    hero(HeroArrangement.STANDARD, Modifier.weight(1f).fillMaxHeight())
                }
                NowPlayingLayout.SING_ALONG -> {
                    hero(HeroArrangement.CENTRED, Modifier.width(SING_ALONG_COLUMN).fillMaxHeight())
                    panel(Modifier.weight(1f).fillMaxHeight(), true)
                }
                else -> {
                    hero(HeroArrangement.STANDARD, Modifier.weight(1f).fillMaxHeight())
                    panel(Modifier.width(panelWidth).fillMaxHeight(), false)
                }
            }
        }
    }
}

/**
 * Translucent rounded container that lets the ambient backdrop show through; under a Windows skin a window on
 * the desktop, titled [title].
 */
@Composable
private fun GlassPanel(modifier: Modifier = Modifier, title: String = "Noctorium", content: @Composable BoxScope.() -> Unit) {
    if (skinned()) {
        PanelWindow(modifier, title, content)
        return
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = ink(.055f),
        border = BorderStroke(1.dp, ink(.08f)),
        content = { Box(Modifier.fillMaxSize(), content = content) },
    )
}

@Composable
private fun NowPlayingHero(
    track: Track,
    playback: PlaybackState,
    state: AppState,
    arrangement: HeroArrangement,
    arrange: () -> Unit,
    modifier: Modifier = Modifier,
    hasNext: Boolean,
) {
    val preferences = state.settings.collectAsState().value.preferences
    val look = preferences.desktop.nowPlaying
    val scale = look.coverSize.scale
    val seekBar: @Composable (Modifier) -> Unit = {
        PlaybackProgressBar(playback, state::seekTo, it, preferences.progressBarStyle, preferences.timeDisplay)
    }
    val cover: @Composable (Dp) -> Unit = { size -> NowPlayingCover(track, playback.isPlaying, look.cover, size) }
    val follow: @Composable (Modifier) -> Unit = { if (look.followButton) FollowArtistChip(track, state, it) }
    val footer: @Composable () -> Unit = { HeroFooter(track, playback, state, look, arrange) }

    when (arrangement) {
        HeroArrangement.STANDARD -> Column(modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
            // Title and artist lead, above the cover, the way a record sleeve is captioned.
            Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
            Spacer(Modifier.height(3.dp))
            Text(track.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.62f), fontSize = 14.sp)
            follow(Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportControls(playback, state, hasNext)
                Spacer(Modifier.width(20.dp))
                seekBar(Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            // Whatever room is left, so a larger cover grows into the space rather than over the footer.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                cover(minOf(430.dp * scale, maxWidth * .82f, maxHeight))
            }
            Spacer(Modifier.height(14.dp))
            footer()
        }

        HeroArrangement.CENTRED -> Column(
            modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                cover(minOf(460.dp * scale, maxWidth * .9f, maxHeight))
            }
            Spacer(Modifier.height(22.dp))
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    track.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 29.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(3.dp))
                Text(track.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.62f), fontSize = 14.sp)
                follow(Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(14.dp))
                seekBar(Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                TransportControls(playback, state, hasNext)
                Spacer(Modifier.height(8.dp))
                footer()
            }
        }

        HeroArrangement.BESIDE -> BoxWithConstraints(modifier.padding(28.dp)) {
            val gap = 48.dp
            val column = minOf(440.dp, maxWidth * .42f)
            val side = minOf(560.dp * scale, maxHeight, maxWidth - gap - column).coerceAtLeast(160.dp)
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            ) {
                cover(side)
                Column(Modifier.width(column)) {
                    track.album?.title?.takeIf { it.isNotBlank() && it != track.title }?.let { album ->
                        Text(
                            album.uppercase(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = ink(.45f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(track.title, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(track.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.62f), fontSize = 16.sp)
                    follow(Modifier.padding(top = 10.dp))
                    Spacer(Modifier.height(28.dp))
                    seekBar(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    TransportControls(playback, state, hasNext)
                    Spacer(Modifier.height(16.dp))
                    footer()
                }
            }
        }

        HeroArrangement.BANNER -> Row(modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            cover(176.dp * scale)
            Spacer(Modifier.width(26.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.artistLine,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = ink(.62f),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    follow(Modifier.padding(start = 12.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransportControls(playback, state, hasNext)
                    Spacer(Modifier.width(20.dp))
                    seekBar(Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                footer()
            }
        }
    }
}

/**
 * Large borderless transport, weighted so play is unmistakably the primary action. Next is lit while it
 * leads somewhere -- autoplay's lined-up songs count -- as on the bars; see QueueState.hasNext.
 */
@Composable
private fun TransportControls(playback: PlaybackState, state: AppState, hasNext: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(state::previous, Modifier.size(44.dp)) {
            Icon(Icons.Default.SkipPrevious, "Previous track", Modifier.size(28.dp), tint = ink(.82f))
        }
        IconButton(
            state::togglePlayback,
            Modifier.size(56.dp),
        ) {
            if (playback.status == PlaybackStatus.RESOLVING) {
                CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface)
            } else {
                PlayPauseIcon(playback.isPlaying, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        IconButton(state::next, Modifier.size(44.dp), enabled = hasNext) {
            Icon(Icons.Default.SkipNext, "Next track", Modifier.size(28.dp), tint = ink(if (hasNext) .82f else .3f))
        }
    }
}

/**
 * The line being sung right now, flanked by the actions that belong to this track, and the two that belong
 * to the screen: the panel tucked away or brought back, and the menu that rearranges it.
 */
@Composable
private fun HeroFooter(track: Track, playback: PlaybackState, state: AppState, look: NowPlayingPreferences, arrange: () -> Unit) {
    val lyrics by state.lyrics.collectAsState()
    // Not where the lyrics are already the size of the screen beside it: the same line twice is one too many.
    val singingBeside = look.layout == NowPlayingLayout.SING_ALONG && !look.panelHidden
    val activeLine = if (!look.lyricLine || singingBeside) null else lyrics.outcomes
        .firstOrNull { it.provider == lyrics.selectedProvider }
        ?.result
        ?.takeIf { it.synced }
        ?.lines
        ?.lastOrNull { line -> (line.startTimeMs ?: Long.MAX_VALUE) <= playback.positionMs }
        ?.text
        ?.takeIf { it.isNotBlank() }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LikeButton(track, state, size = 34.dp)
        DownloadButton(track, state, size = 34.dp)
        IconButton({ state.copyTrackLink(track) }, Modifier.size(34.dp)) {
            Icon(Icons.Default.Link, "Copy link", Modifier.size(17.dp), tint = ink(.6f))
        }
        Box(Modifier.weight(1f).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
            Text(
                activeLine ?: track.album?.title ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = ink(if (activeLine != null) .92f else .45f),
                fontSize = 14.sp,
                fontWeight = if (activeLine != null) FontWeight.Medium else FontWeight.Normal,
            )
        }
        // Beside the badge, which says Spotify either way: this says it is Spotify's own app making the sound.
        if (playsOnSpotify(track, state.settings.collectAsState().value.spotify)) {
            OnSpotifyMark()
            Spacer(Modifier.width(6.dp))
        }
        ProviderBadge(track.provider, compact = true)
        if (look.layout.hasPanel) {
            IconButton({ state.updateNowPlaying { copy(panelHidden = !panelHidden) } }, Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.ViewSidebar,
                    if (look.panelHidden) "Show the panel" else "Hide the panel",
                    Modifier.size(18.dp),
                    tint = if (look.panelHidden) ink(.45f) else ink(.82f),
                )
            }
        }
        IconButton(arrange, Modifier.size(34.dp)) {
            Icon(Icons.Default.Tune, "Arrange this screen", Modifier.size(18.dp), tint = ink(.6f))
        }
    }
}

/**
 * The now playing screen's own menu: the layouts as pictures, and the cover, its size, the backdrop and the
 * panel. Everything here is also in Settings; this is so it can be chosen while looking at the result.
 */
@Composable
internal fun NowPlayingArranger(look: NowPlayingPreferences, backdrop: NowPlayingBackdrop, state: AppState, close: () -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text("Arrange now playing", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        NowPlayingLayoutPicker(look.layout, compact = true) { layout -> state.updateNowPlaying { copy(layout = layout) } }
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Cover", CoverStyle.entries, look.cover, { it.displayName }) { cover -> state.updateNowPlaying { copy(cover = cover) } }
        Spacer(Modifier.height(14.dp))
        ChoiceRow("Cover size", CoverSize.entries, look.coverSize, { it.displayName }) { size -> state.updateNowPlaying { copy(coverSize = size) } }
        Spacer(Modifier.height(14.dp))
        ChoiceRow("Backdrop", NowPlayingBackdrop.entries, backdrop, { it.displayName }, state::setNowPlayingBackdrop)
        if (look.layout == NowPlayingLayout.SIDE_BY_SIDE || look.layout == NowPlayingLayout.PANEL_LEFT) {
            Spacer(Modifier.height(14.dp))
            ChoiceRow("Panel width", NowPlayingPanelWidth.entries, look.panelWidth, { it.displayName }) { width ->
                state.updateNowPlaying { copy(panelWidth = width) }
            }
        }
        Spacer(Modifier.height(10.dp))
        TextButton({
            close()
            pendingSettingsPage = SettingsPage.CUSTOMIZATION
            state.navigate(Destination.SETTINGS)
        }) {
            Text("More in Settings", fontSize = 12.sp)
        }
    }
}

/** The six layouts, each as a picture of itself with its name under it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NowPlayingLayoutPicker(selected: NowPlayingLayout, compact: Boolean = false, choose: (NowPlayingLayout) -> Unit) {
    val pictureWidth = if (compact) 90.dp else 132.dp
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NowPlayingLayout.entries.forEach { layout ->
            val active = layout == selected
            val border by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else ink(.1f),
                motionSpec(MotionTiming.QUICK),
                label = "layout-border",
            )
            Surface(
                onClick = { choose(layout) },
                shape = RoundedCornerShape(12.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else ink(.04f),
                border = BorderStroke(if (active) 2.dp else 1.dp, border),
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    NowPlayingLayoutPicture(layout, active, Modifier.size(pictureWidth, pictureWidth * .62f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        layout.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingPanel(
    queue: QueueState,
    selectedTab: NowPlayingTab,
    selectTab: (NowPlayingTab) -> Unit,
    state: AppState,
    modifier: Modifier = Modifier,
    largeLyrics: Boolean = false,
) {
    // A property sheet's tabs over their page, under a Windows skin.
    if (skinned()) {
        PanelTabs(NowPlayingTab.entries, selectedTab, { it.displayName }, selectTab, modifier) {
            when (selectedTab) {
                NowPlayingTab.UP_NEXT -> UpNextPanel(queue, state)
                NowPlayingTab.LYRICS -> queue.current?.let { LyricsPanel(it, state, large = largeLyrics) }
                NowPlayingTab.RELATED -> PanelPlaceholder(
                    icon = Icons.Default.AutoAwesome,
                    title = "Related music",
                    message = "More music based on the current track will appear here.",
                )
            }
        }
        return
    }
    // Transparent so the ambient backdrop reads through the glass panel behind this.
    Surface(modifier, color = Color.Transparent) {
        Column(Modifier.fillMaxSize().padding(top = 14.dp)) {
            // Rounded chips rather than a segmented bar, matching the pill language of the rest of the panel.
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NowPlayingTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Surface(
                        onClick = { selectTab(tab) },
                        color = if (selected) ink(.16f) else Color.Transparent,
                        border = BorderStroke(1.dp, ink(if (selected) .18f else .09f)),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            tab.displayName,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else ink(.6f),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            when (selectedTab) {
                NowPlayingTab.UP_NEXT -> UpNextPanel(queue, state)
                NowPlayingTab.LYRICS -> queue.current?.let { LyricsPanel(it, state, large = largeLyrics) }
                NowPlayingTab.RELATED -> PanelPlaceholder(
                    icon = Icons.Default.AutoAwesome,
                    title = "Related music",
                    message = "More music based on the current track will appear here.",
                )
            }
        }
    }
}

@Composable
private fun LyricsPanel(track: Track, state: AppState, large: Boolean = false) {
    val lyrics by state.lyrics.collectAsState()
    val playback by state.playback.collectAsState()
    val selectedOutcome = lyrics.outcomes.firstOrNull { it.provider == lyrics.selectedProvider }
    val selectedResult = selectedOutcome?.result
    val sourceListState = rememberLazyListState()
    val sourceScrollScope = rememberCoroutineScope()

    LaunchedEffect(track.queueKey, track.title, track.artistLine, track.durationMs) {
        state.loadLyrics(track)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sources", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton({ state.loadLyrics(track, forceRefresh = true) }, Modifier.size(34.dp)) {
                Icon(Icons.Default.Refresh, "Refresh lyrics", Modifier.size(18.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    sourceScrollScope.launch {
                        sourceListState.animateScrollToItem((sourceListState.firstVisibleItemIndex - 1).coerceAtLeast(0))
                    }
                },
                enabled = sourceListState.canScrollBackward,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous lyric sources", Modifier.size(18.dp))
            }
            Box(Modifier.weight(1f).height(53.dp)) {
                LazyRow(
                    state = sourceListState,
                    modifier = Modifier.fillMaxSize().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp),
                ) {
                    items(lyrics.outcomes, key = { it.provider.name }) { outcome ->
                        FilterChip(
                            selected = lyrics.selectedProvider == outcome.provider,
                            onClick = { state.selectLyricsProvider(outcome.provider) },
                            label = { Text(outcome.provider.displayName, fontSize = 10.sp) },
                            leadingIcon = { LyricsProviderStatusIcon(outcome) },
                            shape = RoundedCornerShape(9.dp),
                        )
                    }
                }
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(sourceListState),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(5.dp),
                )
            }
            IconButton(
                onClick = {
                    sourceScrollScope.launch {
                        val lastVisible = sourceListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        sourceListState.animateScrollToItem((lastVisible + 1).coerceAtMost(lyrics.outcomes.lastIndex.coerceAtLeast(0)))
                    }
                },
                enabled = sourceListState.canScrollForward,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.ChevronRight, "More lyric sources", Modifier.size(18.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .18f))

        // Read into a local before the branch below tests it. A public property from another module is
        // never smart-cast — nothing there stops it becoming a computed one that answers differently on
        // each read — so testing the property and then using it would leave it nullable.
        val providerPage = selectedResult?.sourceUrl

        when {
            lyrics.loading && selectedResult == null -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(34.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Checking ${lyrics.outcomes.size} lyric sources…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            selectedResult != null && selectedResult.lines.isNotEmpty() -> {
                LyricsContent(selectedOutcome, playback.positionMs, state, large)
            }
            selectedResult != null && providerPage != null -> {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                    Text(selectedResult.provider.displayName, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        selectedResult.message ?: "Open this provider to view the lyrics.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    FilledTonalButton({ state.openExternalUrl(providerPage) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open ${selectedResult.provider.displayName}")
                    }
                }
            }
            selectedOutcome != null -> LyricsProviderUnavailable(selectedOutcome) {
                state.loadLyrics(track, forceRefresh = true)
            }
            else -> LyricsNotFound(lyrics.outcomes, lyrics.errorMessage)
        }
    }
}

@Composable
private fun LyricsProviderUnavailable(outcome: LyricsProviderOutcome, refresh: () -> Unit) {
    val (title, message) = when (outcome.status) {
        LyricsProviderStatus.NOT_FOUND -> "No match on ${outcome.provider.displayName}" to
            "This source does not currently have lyrics for this version of the track."
        LyricsProviderStatus.NEEDS_KEY -> "${outcome.provider.displayName} needs access" to
            "This optional source requires its own API key before Noctorium can search it."
        LyricsProviderStatus.ERROR -> "${outcome.provider.displayName} is unavailable" to
            "The source did not respond correctly. Refresh to try it again."
        LyricsProviderStatus.SEARCHING -> "Checking ${outcome.provider.displayName}" to
            "Waiting for this source to respond…"
        else -> "Lyrics unavailable" to (outcome.detail ?: "This source did not return lyrics.")
    }
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (outcome.status == LyricsProviderStatus.NEEDS_KEY) Icons.Default.Key else Icons.Default.Lyrics,
            null,
            Modifier.size(40.dp),
            tint = if (outcome.status == LyricsProviderStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (outcome.status == LyricsProviderStatus.ERROR) {
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(refresh) {
                Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Try again")
            }
        }
    }
}

@Composable
private fun LyricsProviderStatusIcon(outcome: LyricsProviderOutcome) {
    val (icon, tint) = when (outcome.status) {
        LyricsProviderStatus.SEARCHING -> Icons.Default.HourglassTop to MaterialTheme.colorScheme.onSurfaceVariant
        LyricsProviderStatus.FOUND -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        LyricsProviderStatus.LINK_ONLY -> Icons.AutoMirrored.Filled.OpenInNew to MaterialTheme.colorScheme.primary
        LyricsProviderStatus.NOT_FOUND -> Icons.Default.RemoveCircleOutline to MaterialTheme.colorScheme.onSurfaceVariant
        LyricsProviderStatus.NEEDS_KEY -> Icons.Default.Key to MaterialTheme.colorScheme.tertiary
        LyricsProviderStatus.ERROR -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
    }
    Icon(icon, outcome.detail, Modifier.size(15.dp), tint = tint)
}

@Composable
internal fun LyricsContent(outcome: LyricsProviderOutcome, positionMs: Long, state: AppState, large: Boolean = false) {
    val result = outcome.result ?: return
    val look = state.settings.collectAsState().value.preferences.lyrics
    // The gaps grow with the words, so a larger size reads as larger lyrics rather than as a crowded page.
    val spacing = look.size.scale
    val listState = rememberLazyListState()
    val activeIndex = remember(result.lines, positionMs) {
        if (!result.synced) -1 else result.lines.indexOfLast { (it.startTimeMs ?: Long.MAX_VALUE) <= positionMs }
    }

    LaunchedEffect(activeIndex, result.provider) {
        // Large lines sit further down, so the one being sung is nearer the middle than the top.
        if (activeIndex >= 0) listState.animateScrollToItem((activeIndex - if (large) 3 else 2).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(result.provider.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    if (result.synced) "Synced lyrics" else "Plain lyrics",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            result.sourceUrl?.let { url ->
                TextButton({ state.openExternalUrl(url) }) {
                    Text("Source", fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(14.dp))
                }
            }
        }
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = if (large) 36.dp else 18.dp,
                    end = if (large) 36.dp else 18.dp,
                    top = 12.dp,
                    bottom = 12.dp + chromeBottom(),
                ),
                verticalArrangement = Arrangement.spacedBy(
                    when {
                        large && result.synced -> 20.dp
                        large -> 12.dp
                        result.synced -> 13.dp
                        else -> 8.dp
                    } * spacing,
                ),
            ) {
                itemsIndexed(result.lines) { index, line ->
                    LyricLineText(line, active = index == activeIndex, synced = result.synced, large = large, look = look)
                }
                result.attribution?.let { attribution ->
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(attribution, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

/**
 * One line of the lyrics, as the listener asked for them to be set in [look]: its size scaled, leaning the
 * way they chose, and the lines not being sung dimmed or not. The defaults draw exactly what this always
 * drew.
 */
@Composable
internal fun LyricLineText(line: LyricLine, active: Boolean, synced: Boolean, large: Boolean = false, look: LyricsLook = LyricsLook()) {
    // The line being sung brightens into the accent, and the one before fades back, rather than flicking.
    // Without the dimming the others stay at full strength, and the accent alone says which line is sung.
    val colour by animateColorAsState(
        when {
            active -> MaterialTheme.colorScheme.primary
            synced && look.dimOtherLines -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        },
        motionSpec(MotionTiming.STANDARD),
        label = "lyric-line",
    )
    val scale = look.size.scale
    // Large for singing along: centred, and big enough to read from across the room.
    Text(
        line.text,
        color = colour,
        fontSize = when {
            large && active -> 30.sp
            large && synced -> 22.sp
            large -> 19.sp
            active -> 18.sp
            synced -> 15.sp
            else -> 14.sp
        } * scale,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        lineHeight = when {
            large && active -> 38.sp
            large -> 29.sp
            active -> 23.sp
            else -> 20.sp
        } * scale,
        textAlign = when (look.alignment) {
            LyricsAlignment.AUTO -> if (large) androidx.compose.ui.text.style.TextAlign.Center else null
            LyricsAlignment.START -> androidx.compose.ui.text.style.TextAlign.Start
            LyricsAlignment.CENTRE -> androidx.compose.ui.text.style.TextAlign.Center
        },
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    )
}

@Composable
private fun LyricsNotFound(outcomes: List<LyricsProviderOutcome>, errorMessage: String?) {
    val needsKeys = outcomes.filter { it.status == LyricsProviderStatus.NEEDS_KEY }
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Lyrics, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("No lyrics found", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            errorMessage ?: "None of the configured sources matched this track.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (needsKeys.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Optional keys unlock: ${needsKeys.joinToString { it.provider.displayName }}",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 10.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun UpNextPanel(queue: QueueState, state: AppState) {
    val settings by state.settings.collectAsState()
    var savedAs by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    queue.context?.originType?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Queue",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "${queue.currentIndex + 1} of ${queue.tracks.size}",
                    color = ink(.5f),
                    fontSize = 11.sp,
                )
            }
            // Pills rather than icon buttons, so the two destructive-ish actions read clearly.
            QueueActionPill("Shuffle", Icons.Default.Shuffle, queue.shuffleEnabled, state::toggleShuffle)
            QueueActionPill("Clear", Icons.Default.Close, false, state::clearQueue)
            if (queue.tracks.isNotEmpty()) QueueActionsMenu(queue, state) { savedAs = it }
        }
        savedAs?.let { name ->
            Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                LibraryNotice("Saved the queue as \"$name\". It is in your library.") { savedAs = null }
            }
        }
        HorizontalDivider(color = ink(.07f))
        val list = rememberLazyListState()
        LazyColumn(
            Modifier.fillMaxSize().listWell().classicScrollbar(list).padding(horizontal = 10.dp),
            state = list,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = chromePadding(bottom = 18.dp),
        ) {
            itemsIndexed(queue.tracks, key = { index, track -> "expanded:${track.queueKey}:$index" }) { index, track ->
                QueueRow(track, index, index == queue.currentIndex, state)
            }
            val preferences = settings.preferences
            autoplaySection(
                autoplayShown(queue, preferences.autoplay, preferences.autoplayFrom, settings.spotify),
                state,
                compact = true,
            )
        }
    }
}

/** Small pill action for the queue header. */
@Composable
private fun QueueActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    action: () -> Unit,
) {
    // A button that stays pressed in while it is on, under a Windows skin.
    if (skinned()) {
        FilterChip(active, action, label = { Text(label) }, leadingIcon = { Icon(icon, null, Modifier.size(14.dp)) })
        return
    }
    Surface(
        onClick = action,
        shape = RoundedCornerShape(20.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .28f) else ink(.08f),
        border = BorderStroke(1.dp, ink(if (active) .22f else .1f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                null,
                Modifier.size(14.dp),
                tint = if (active) MaterialTheme.colorScheme.primary else ink(.75f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.primary else ink(.82f),
            )
        }
    }
}

/**
 * One line of the queue. Play and remove sit on the row itself, appearing on hover the way the reference does,
 * so the list stays quiet until you reach for it.
 */
@Composable
private fun QueueRow(track: Track, index: Int, playing: Boolean, state: AppState) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val hovered = isHovered ||
        state.settings.collectAsState().value.preferences.hoverControls == HoverControls.ALWAYS
    ListRow(
        onClick = { state.jumpToQueueItem(index) },
        color = when {
            playing -> ink(.11f)
            hovered -> ink(.06f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(12.dp),
        selected = playing,
        interactionSource = interaction,
        modifier = Modifier.hoverable(interaction),
    ) {
        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(skinShape(RoundedCornerShape(8.dp))), contentAlignment = Alignment.Center) {
                RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize())
                if (playing) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GraphicEq, "Playing", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                    color = if (playing) MaterialTheme.colorScheme.primary else ink(.92f),
                )
                Text(
                    track.artistLine,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ink(.5f),
                    fontSize = 11.sp,
                )
            }
            if (hovered) {
                IconButton({ state.jumpToQueueItem(index) }, Modifier.size(30.dp)) {
                    Icon(Icons.Default.PlayArrow, "Play now", Modifier.size(18.dp), tint = ink(.85f))
                }
                IconButton({ state.removeQueueItem(index) }, Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, "Remove from queue", Modifier.size(16.dp), tint = ink(.6f))
                }
            } else {
                track.durationMs?.let {
                    Text(formatPlaybackTime(it), color = ink(.45f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PanelPlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun QueueScreen(queue: QueueState, state: AppState) {
    val settings by state.settings.collectAsState()
    // The name the queue was just saved under, said here, where it was saved from, rather than only in the
    // library where the playlist went.
    var savedAs by remember { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                ScreenTitle("Queue", 32.sp)
                Text(
                    if (queue.tracks.isEmpty()) "Nothing queued" else "${pluralTracks(queue.tracks.size)} • ${queue.currentIndex + 1} playing",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            if (queue.tracks.isNotEmpty()) {
                TextButton(state::clearQueue) { Icon(Icons.Default.ClearAll, null); Spacer(Modifier.width(6.dp)); Text("Clear") }
                QueueActionsMenu(queue, state) { savedAs = it }
            }
        }
        savedAs?.let { name ->
            Spacer(Modifier.height(10.dp))
            LibraryNotice("Saved the queue as \"$name\". It is in your library.") { savedAs = null }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = state::toggleShuffle,
                label = { Text(if (queue.shuffleEnabled) "Shuffle on" else "Shuffle") },
                leadingIcon = { Icon(Icons.Default.Shuffle, null, Modifier.size(18.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (queue.shuffleEnabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    labelColor = if (queue.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIconContentColor = if (queue.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            AssistChip(
                onClick = state::cycleRepeat,
                label = {
                    Text(
                        when (queue.repeatMode) {
                            RepeatMode.OFF -> "Repeat"
                            RepeatMode.ALL -> "Repeat all"
                            RepeatMode.ONE -> "Repeat one"
                        },
                    )
                },
                leadingIcon = {
                    Icon(if (queue.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null, Modifier.size(18.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    labelColor = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIconContentColor = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        Spacer(Modifier.height(18.dp))
        if (queue.tracks.isEmpty()) {
            EmptyScreen("Your queue is empty", "Play a section or add tracks from the ⋮ menu.")
        } else {
            LazyColumn(Modifier.classicScrollbar(list), state = list, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = chromePadding(bottom = 20.dp)) {
                itemsIndexed(queue.tracks, key = { index, track -> "${track.queueKey}:$index" }) { index, track ->
                    QueueTrackRow(track, index, queue, state)
                }
                val preferences = settings.preferences
                autoplaySection(
                    autoplayShown(queue, preferences.autoplay, preferences.autoplayFrom, settings.spotify),
                    state,
                    compact = false,
                )
            }
        }
    }
}

@Composable
private fun QueueTrackRow(track: Track, index: Int, queue: QueueState, state: AppState) {
    val playing = index == queue.currentIndex
    ListRow(
        onClick = { state.jumpToQueueItem(index) },
        color = if (playing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f),
        shape = RoundedCornerShape(12.dp),
        selected = playing,
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (playing) Icon(Icons.Default.GraphicEq, "Playing", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                else Text((index + 1).toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(48.dp).clip(skinShape(RoundedCornerShape(9.dp))))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal)
                Text(track.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            ProviderBadge(track.provider, compact = true)
            IconButton({ state.moveQueueItem(index, index - 1) }, enabled = index > 0, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, "Move up", Modifier.size(19.dp))
            }
            IconButton({ state.moveQueueItem(index, index + 1) }, enabled = index < queue.tracks.lastIndex, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Move down", Modifier.size(19.dp))
            }
            IconButton({ state.removeQueueItem(index) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun PlaybackProgressBar(
    playback: PlaybackState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    style: ProgressBarStyle = ProgressBarStyle.MINIMAL,
    timeDisplay: TimeDisplay = TimeDisplay.TOTAL,
) {
    val durationMs = playback.durationMs.coerceAtLeast(0)
    // Whether the track's length is known yet. Standing in 1 ms for the unknown made every ratio below round
    // to a full bar, so a track of unknown length showed as finished the moment it started playing.
    val hasDuration = durationMs > 0
    val canSeek = playback.track != null && hasDuration && playback.status != PlaybackStatus.RESOLVING
    var dragging by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableFloatStateOf(0f) }
    val displayedPosition = if (dragging) draggedPosition else playback.positionMs.toFloat()
    val maximum = durationMs.coerceAtLeast(1).toFloat()
    val fraction = playbackFraction(displayedPosition, durationMs)
    // The right-hand figure is either the length of the track or what is left of it. An unknown length is
    // said to be unknown rather than reported as zero.
    val trailing = when {
        !hasDuration -> UNKNOWN_PLAYBACK_TIME
        timeDisplay == TimeDisplay.REMAINING ->
            "-" + formatPlaybackTime((maximum - displayedPosition).toLong().coerceAtLeast(0))
        else -> formatPlaybackTime(durationMs)
    }

    if (style.isDrawn) {
        DrawnProgressBar(
            style = style,
            positionMs = displayedPosition,
            fraction = fraction,
            trailingLabel = trailing,
            canSeek = canSeek,
            moving = playback.isPlaying && !dragging,
            onScrub = { fraction ->
                dragging = true
                draggedPosition = (fraction * maximum).coerceIn(0f, maximum)
            },
            onScrubFinished = {
                val target = draggedPosition.toLong()
                dragging = false
                onSeek(target)
            },
            modifier = modifier,
        )
        return
    }

    Row(modifier.height(34.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatPlaybackTime(displayedPosition.toLong()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.width(44.dp),
        )
        Slider(
            value = if (hasDuration) displayedPosition.coerceIn(0f, maximum) else 0f,
            onValueChange = {
                dragging = true
                draggedPosition = it
            },
            onValueChangeFinished = {
                val target = draggedPosition.toLong()
                dragging = false
                onSeek(target)
            },
            enabled = canSeek,
            valueRange = 0f..maximum,
            modifier = Modifier.weight(1f),
        )
        Text(
            trailing,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.width(48.dp),
        )
    }
}

/**
 * Every seek bar that is not Material's own: times at each end, and the track drawn between them.
 *
 * Drawn rather than built out of a slider, because what these styles differ in is exactly the furniture
 * a slider insists on. One composable for all of them and one [drawSeekBar] underneath, so seeking --
 * tap anywhere, drag from anywhere -- is written once and cannot work in one style and not another.
 */
@Composable
private fun DrawnProgressBar(
    style: ProgressBarStyle,
    positionMs: Float,
    /** How much of the track has played, already resolved by the caller so an unknown length reads as empty. */
    fraction: Float,
    trailingLabel: String,
    canSeek: Boolean,
    /** Whether the music is actually running, which is the only time the wave has any business moving. */
    moving: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableIntStateOf(1) }
    val trackColour = ink(SeekBar.TRACK_ALPHA)
    val filledColour = MaterialTheme.colorScheme.primary

    /*
     * The wave's travel, and the two reasons it is animated the way it is.
     *
     * The phase runs 0..1 and is multiplied into the sine below, so one cycle of the animation moves the
     * crests exactly one wavelength: the wave slides rather than shimmering in place.
     *
     * The amplitude is a separate animation to zero when nothing is playing. Stopping the phase instead
     * would freeze the wave mid-crest, which looks like a rendering fault rather than like a paused
     * song; easing the height out leaves the flat line the other styles draw, which is what a stopped
     * player should look like.
     */
    val travel = rememberInfiniteTransition(label = "wave")
    val phase by travel.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((SeekBar.WAVE_SECONDS_PER_CYCLE * 1000).toInt(), easing = LinearEasing),
        ),
        label = "wavePhase",
    )
    val amplitude by animateFloatAsState(
        if (style == ProgressBarStyle.WAVE && moving) 1f else 0f,
        tween(450),
        label = "waveAmplitude",
    )

    Row(modifier.height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatPlaybackTime(positionMs.toLong()),
            color = ink(.68f),
            fontSize = 11.sp,
            modifier = Modifier.width(42.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(24.dp)
                .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
                .pointerInput(canSeek, widthPx) {
                    if (!canSeek) return@pointerInput
                    detectTapGestures { offset ->
                        onScrub(offset.x / widthPx)
                        onScrubFinished()
                    }
                }
                .pointerInput(canSeek, widthPx) {
                    if (!canSeek) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> onScrub(offset.x / widthPx) },
                        onDragEnd = { onScrubFinished() },
                        onDragCancel = { onScrubFinished() },
                        onHorizontalDrag = { change, _ ->
                            onScrub(change.position.x / widthPx)
                            change.consume()
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                drawSeekBar(style, fraction, canSeek, trackColour, filledColour, phase, amplitude)
            }
        }
        Text(
            trailingLabel,
            color = ink(.68f),
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(42.dp),
        )
    }
}

/** Stands in for a length that is not known yet, so an unknown track does not claim to be zero seconds long. */
internal const val UNKNOWN_PLAYBACK_TIME = "--:--"

/**
 * How much of the seek bar is filled.
 *
 * A track whose length is not known has nothing to measure a position against. Standing in 1 ms for the
 * unknown made every position round to a full bar, so a YouTube Music track — whose listing carries no
 * duration at all — showed as finished the moment it started playing.
 */
internal fun playbackFraction(positionMs: Float, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs / durationMs.toFloat()).coerceIn(0f, 1f)

internal fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

@Composable
private fun PlaybackError(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(10.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

private enum class SettingsPage {
    ACCOUNT, PROFILE, CUSTOMIZATION, SOUND, PLAYBACK_QUEUE, YOUTUBE, SOUNDCLOUD, SPOTIFY, BANDCAMP, VK, SCROBBLING, LYRICS,
    STARTUP, DISCORD, UPDATES, PLAYBACK_TOOLS, DIAGNOSTICS
}

/**
 * A settings page something elsewhere has asked for.
 *
 * Which page is open is local to [SettingsScreen] and should stay that way -- but the banner shown when
 * mpv is missing has to be able to land on the page that fixes it, rather than on the settings list with
 * a instruction to go looking. Read once and cleared, so it cannot pin the screen open.
 */
private var pendingSettingsPage: SettingsPage? = null

@Composable
internal fun SettingsScreen(state: AppState) {
    val settings by state.settings.collectAsState()
    var page by remember { mutableStateOf(pendingSettingsPage.also { pendingSettingsPage = null }) }
    // Going into a page and back out again slides, the way a phone's settings do, rather than cutting.
    MotionContent(page, Modifier.fillMaxSize(), kind = MotionKind.PAGE, forward = { _, to -> to != null }) { shown ->
        if (shown != null) {
            SettingsDetailHeader(pageTitle(shown), { page = null }) {
                when (shown) {
                    SettingsPage.ACCOUNT -> NoctoriumAccountPanel(state)
                    SettingsPage.PROFILE -> ProfileSettingsPanel(settings.preferences, state)
                    SettingsPage.CUSTOMIZATION -> CustomizationPanel(settings.preferences, state)
                    SettingsPage.SOUND -> SoundSettingsPanel(settings.preferences, state)
                    SettingsPage.PLAYBACK_QUEUE -> PlaybackQueueSettingsPanel(settings.preferences, state)
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
                    SettingsPage.LYRICS -> LyricsSettingsPanel(settings.preferences, state)
                    SettingsPage.STARTUP -> StartupSettingsPanel(settings.preferences, state)
                    SettingsPage.DISCORD -> DiscordSettingsPanel(settings.preferences, state)
                    SettingsPage.UPDATES -> UpdatePanel(state)
                    SettingsPage.PLAYBACK_TOOLS -> PlaybackToolsPanel()
                    SettingsPage.DIAGNOSTICS -> DiagnosticsPanel(settings, state)
                }
            }
        } else {
            // A Control Panel, in a folder's white pane, under a Windows skin; the pages are property sheets.
            BrowsingPane { SettingsHome(settings, state) { page = it } }
        }
    }
}

/** The list of settings, each tile a way into its page. */
@Composable
private fun SettingsHome(settings: SettingsState, state: AppState, open: (SettingsPage) -> Unit) {
    val list = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize().classicScrollbar(list).padding(horizontal = 30.dp),
        state = list,
        contentPadding = chromePadding(top = 26.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(if (skinned()) 2.dp else 11.dp),
    ) {
        item {
            ScreenTitle("Settings", 34.sp)
            Text("Accounts, services and how Noctorium behaves.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
        }
        settings.message?.let { message ->
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f), shape = RoundedCornerShape(11.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp)); Text(message, Modifier.weight(1f), fontSize = 12.sp)
                        IconButton(state::clearSettingsMessage, Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Dismiss", Modifier.size(16.dp)) }
                    }
                }
            }
        }
        item {
            val account by state.account.collectAsState()
            SettingsCard(
                account.user?.displayName ?: "Noctorium account",
                if (account.signedIn) {
                    "${account.stats.streams} streamed · ${account.stats.uniqueTracks} different · " +
                        "${formatHours(account.stats.hours)} listened"
                } else {
                    "Sign in to count what you listen to"
                },
                Icons.Default.Insights,
                { open(SettingsPage.ACCOUNT) },
            )
        }
        item {
            SettingsCard(
                settings.preferences.profileName,
                "Your local Noctorium profile",
                Icons.Default.AccountCircle,
                { open(SettingsPage.PROFILE) },
            )
        }
        item {
            SettingsCard(
                "Customization",
                "Theme, colour, typeface, lyrics, Home and the player",
                Icons.Default.Tune,
                { open(SettingsPage.CUSTOMIZATION) },
            )
        }
        item {
            // Its own page rather than a card under Customization: how the music sounds is not how
            // Noctorium looks, and somebody looking for an equaliser looks for the word Sound.
            val equalizer = settings.preferences.equalizer
            SettingsCard(
                "Sound",
                if (equalizer.enabled) "Equaliser on · ${equalizer.preset.displayName}" else "Equaliser off",
                Icons.Default.Equalizer,
                { open(SettingsPage.SOUND) },
                equalizer.enabled,
            )
        }
        item {
            // Beside Sound, as how the music goes on rather than how it sounds: its speed, what follows the
            // queue, whether the queue is kept, and how the sleep timer ends it.
            SettingsCard(
                "Playback and queue",
                playbackSummary(settings.preferences),
                Icons.AutoMirrored.Filled.QueueMusic,
                { open(SettingsPage.PLAYBACK_QUEUE) },
            )
        }
        item {
            SettingsCard(
                "YouTube Music",
                accountSummary(settings.preferences.youtubeCookies, settings.youtubeAccount),
                Icons.Default.PlayCircle,
                { open(SettingsPage.YOUTUBE) },
                settings.youtubeAccount.status == AccountConnectionStatus.CONNECTED,
            )
        }
        item {
            SettingsCard(
                "SoundCloud",
                accountSummary(settings.preferences.soundCloudCookies, settings.soundCloudAccount),
                Icons.Default.Cloud,
                { open(SettingsPage.SOUNDCLOUD) },
                settings.soundCloudAccount.status == AccountConnectionStatus.CONNECTED,
            )
        }
        item {
            SettingsCard(
                "Spotify library",
                spotifySummary(settings.spotify),
                Icons.Default.LibraryMusic,
                { open(SettingsPage.SPOTIFY) },
                settings.spotify.connected,
            )
        }
        item {
            SettingsCard(
                "Bandcamp",
                bandcampSummary(settings.preferences.bandcampUsername, settings.bandcamp),
                Icons.Default.Album,
                { open(SettingsPage.BANDCAMP) },
                settings.preferences.bandcampUsername.isNotBlank(),
            )
        }
        item {
            SettingsCard(
                "VK Music",
                vkSummary(settings.vk),
                Icons.Default.Headphones,
                { open(SettingsPage.VK) },
                settings.vk.connected,
            )
        }
        item {
            val connected = listOf(settings.scrobbling.lastFm, settings.scrobbling.listenBrainz)
                .filter { it.status == ScrobbleConnectionStatus.CONNECTED }
                .mapNotNull { it.username }
            SettingsCard(
                "Scrobbling",
                if (connected.isEmpty()) "Connect Last.fm or ListenBrainz" else "Connected: ${connected.joinToString()}",
                Icons.Default.History,
                { open(SettingsPage.SCROBBLING) },
                connected.isNotEmpty(),
            )
        }
        item {
            SettingsCard(
                "Lyrics providers",
                settings.preferences.lyricsProvider?.let { "Opens on ${it.displayName} · 8 sources" }
                    ?: "LRCLIB, Better Lyrics, Genius and 5 more",
                Icons.Default.Lyrics,
                { open(SettingsPage.LYRICS) },
            )
        }
        item {
            SettingsCard(
                "Startup and tray",
                startupSubtitle(settings.preferences),
                Icons.Default.PowerSettingsNew,
                { open(SettingsPage.STARTUP) },
            )
        }
        item {
            // Read from the live setting. This once read the field that only exists to carry a choice over
            // from an older build, which is never written and so always said Disabled however it was set.
            val discord = settings.preferences.discord
            SettingsCard(
                "Discord Rich Presence",
                if (discord.enabled) "Enabled · ${discord.timestamps.displayName}" else "Disabled",
                Icons.Default.SportsEsports,
                { open(SettingsPage.DISCORD) },
                discord.enabled,
            )
        }
        item { SettingsCard("Updates", updatesSubtitle(state), Icons.Default.SystemUpdateAlt, { open(SettingsPage.UPDATES) }) }
        item {
            SettingsCard(
                "Playback tools",
                playbackToolsSubtitle(),
                Icons.Default.Extension,
                { open(SettingsPage.PLAYBACK_TOOLS) },
            )
        }
        item { SettingsCard("Diagnostics", "Check yt-dlp, mpv and storage", Icons.Default.MonitorHeart, { open(SettingsPage.DIAGNOSTICS) }) }
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

private fun pageTitle(page: SettingsPage) = when (page) {
    SettingsPage.ACCOUNT -> "Noctorium account"
    SettingsPage.PROFILE -> "Your Noctorium profile"
    SettingsPage.CUSTOMIZATION -> "Customization"
    SettingsPage.SOUND -> "Sound"
    SettingsPage.PLAYBACK_QUEUE -> "Playback and queue"
    SettingsPage.YOUTUBE -> "YouTube Music account"
    SettingsPage.SOUNDCLOUD -> "SoundCloud account"
    SettingsPage.SPOTIFY -> "Spotify library"
    SettingsPage.BANDCAMP -> "Bandcamp"
    SettingsPage.VK -> "VK Music"
    SettingsPage.SCROBBLING -> "Scrobbling"
    SettingsPage.LYRICS -> "Lyrics providers"
    SettingsPage.STARTUP -> "Startup and tray"
    SettingsPage.DISCORD -> "Discord Rich Presence"
    SettingsPage.UPDATES -> "Updates"
    SettingsPage.PLAYBACK_TOOLS -> "Playback tools"
    SettingsPage.DIAGNOSTICS -> "Diagnostics"
}

@Composable
private fun SettingsDetailHeader(title: String, back: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to settings") }
            Spacer(Modifier.width(6.dp)); ScreenTitle(title, 28.sp)
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxSize()) { content() }
    }
}

/**
 * Choices about how Noctorium looks. Each option previews itself with a real, working control rather than a
 * screenshot, so the choice is made by looking at the thing itself.
 */
@Composable
private fun CustomizationPanel(preferences: NoctoriumPreferences, state: AppState) {
    Column(Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanelCard {
            CardHeading(Icons.Default.Tune, "Seek bar")
            Spacer(Modifier.height(6.dp))
            Text(
                "Used in the player bar and on the now playing screen. Both can be dragged and clicked to seek.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(15.dp))
            ChoiceRow(
                "Player bar layout",
                PlayerBarStyle.entries,
                preferences.playerBarStyle,
                { it.displayName },
                state::setPlayerBarStyle,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                preferences.playerBarStyle.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(15.dp))
            ChoiceRow(
                "Player bar position",
                PlayerBarPosition.entries,
                preferences.playerBarPosition,
                { it.displayName },
                state::setPlayerBarPosition,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                preferences.playerBarPosition.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(15.dp))
            PlayerButtonsSetting(preferences, state)
            Spacer(Modifier.height(18.dp))
            SaveMusicSetting(preferences, state)
            Spacer(Modifier.height(18.dp))
            Text("Seek bar style", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(9.dp))
            ProgressBarStyle.entries.forEach { option ->
                ProgressStyleOption(
                    option = option,
                    selected = preferences.progressBarStyle == option,
                    choose = { state.setProgressBarStyle(option) },
                )
                Spacer(Modifier.height(10.dp))
            }
            ChoiceRow("Right-hand figure", TimeDisplay.entries, preferences.timeDisplay, { it.displayName }, state::setTimeDisplay)
        }

        NowPlayingSettingsCard(preferences, state)

        LyricsLookCard(preferences.lyrics, state)

        SettingsPanelCard {
            CardHeading(Icons.Default.Palette, "Theme")
            Spacer(Modifier.height(6.dp))
            Text(
                "Noctorium's own, and the palettes you may already know from your editor and terminal. " +
                    "The desktop and the phone share the choice.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            // Grouped by family, because "Mocha" on its own means nothing and "Catppuccin Mocha" does.
            ThemePreset.entries.groupBy { it.family }.forEach { (family, presets) ->
                Text(family, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.forEach { preset ->
                        ThemeSwatch(
                            preset = preset,
                            colours = preset.colours ?: preferences.customTheme,
                            selected = preferences.theme == preset,
                            choose = { state.setTheme(preset) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (preferences.theme == ThemePreset.CUSTOM) {
                CustomThemeEditor(preferences.customTheme, state::setCustomTheme)
                Spacer(Modifier.height(12.dp))
            }
        }

        ColourSettingsCard(preferences, state)

        SettingsPanelCard {
            CardHeading(Icons.Default.PlayCircle, "Playback")
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                "Skip the parts of a YouTube video that are not the music",
                "Intros, outros, sponsor reads and talking, as marked by SponsorBlock's contributors. " +
                    "YouTube Music tracks are never touched. Asks sponsor.ajay.app by a hash of the video id.",
                preferences.skipNonMusic,
                change = state::setSkipNonMusic,
            )
        }

        BrowsingSettingsCard(preferences, state)

        SettingsPanelCard {
            CardHeading(Icons.Default.Start, "Startup")
            Spacer(Modifier.height(12.dp))
            ChoiceRow("Open Noctorium on", StartPage.entries, preferences.startPage, { it.displayName }, state::setStartPage)
        }
    }
}

/**
 * The colours and the type: the accent, with a picker for the listener's own, then animations, surfaces,
 * corners, the text size and the typeface.
 */
@Composable
internal fun ColourSettingsCard(preferences: NoctoriumPreferences, state: AppState) {
    SettingsPanelCard {
        CardHeading(Icons.Default.Palette, "Colour")
        Spacer(Modifier.height(12.dp))
        Text("Accent", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        // Swatches rather than names: the colour is the label.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPreset.entries.forEach { option ->
                AccentSwatch(
                    option,
                    Color(preferences.themeColours().accent),
                    Color(preferences.customAccent or 0xFF000000L),
                    preferences.accent == option,
                ) { state.setAccent(option) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when (preferences.accent) {
                AccentPreset.ARTWORK -> "The interface follows the cover of whatever is playing."
                AccentPreset.THEME -> "The accent the theme was designed with."
                AccentPreset.CUSTOM -> "Your own colour, wherever the theme would have put its accent."
                else -> preferences.accent.displayName
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        // Shown only once Your own is chosen, so the card stays short for everybody happy with a swatch.
        if (preferences.accent == AccentPreset.CUSTOM) {
            Spacer(Modifier.height(14.dp))
            AccentColourPicker(preferences.customAccent or 0xFF000000L, preferences.themeColours().background, state::setCustomAccent)
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow(
            "Animations",
            "Screens ease in, pages slide, and a new track's name rises into place. Off makes every change instant.",
            preferences.animations,
            change = state::setAnimations,
        )
        Spacer(Modifier.height(16.dp))
        ChoiceRow(
            "Surfaces",
            SurfaceStyle.entries,
            preferences.surfaceStyle,
            { it.displayName },
            state::setSurfaceStyle,
        )
        Spacer(Modifier.height(6.dp))
        Text(preferences.surfaceStyle.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Corners", CornerStyle.entries, preferences.cornerStyle, { it.displayName }, state::setCornerStyle)
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Text size", TextSize.entries, preferences.textSize, { it.displayName }, state::setTextSize)
        Spacer(Modifier.height(6.dp))
        Text(
            "Everything at once, on top of whatever text size the machine is already set to.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        FontChooser(preferences.font, state::setFont)
    }
}

/** Which of the player bar's buttons it carries, under the layout and the position that arrange them. */
@Composable
internal fun PlayerButtonsSetting(preferences: NoctoriumPreferences, state: AppState) {
    val hidden = preferences.desktop.hiddenPlayerButtons
    Column {
        ToggleChips(
            "Buttons on the player bar",
            PlayerButton.entries,
            on = { it !in hidden },
            name = { it.displayName },
            icon = { it.icon() },
        ) { button, shown ->
            state.updateDesktop { copy(hiddenPlayerButtons = if (shown) hiddenPlayerButtons - button else hiddenPlayerButtons + button) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Lit ones are on the bar, in every layout. Play, pause and the two skips always stay, and so do " +
                "download and add to playlist. Connect only appears once there is a device to play on, and a hidden sleep timer or Connect still shows while it is in use.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

/** How the pages are browsed: the cards, the badges, the buttons on them, what Home holds, what the sidebar offers. */
@Composable
internal fun BrowsingSettingsCard(preferences: NoctoriumPreferences, state: AppState) {
    SettingsPanelCard {
        CardHeading(Icons.Default.GridView, "Browsing")
        Spacer(Modifier.height(12.dp))
        ChoiceRow("Card size", CardSize.entries, preferences.cardSize, { it.displayName }, state::setCardSize)
        Spacer(Modifier.height(16.dp))
        ChoiceRow(
            "Service badges",
            BadgePolicy.entries,
            preferences.badgePolicy,
            { it.displayName },
            state::setBadgePolicy,
        )
        Spacer(Modifier.height(6.dp))
        Text(preferences.badgePolicy.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        ChoiceRow(
            "Play and menu buttons",
            HoverControls.entries,
            preferences.hoverControls,
            { it.displayName },
            state::setHoverControls,
        )
        Spacer(Modifier.height(6.dp))
        Text(preferences.hoverControls.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        val hiddenHome = preferences.hiddenHomeParts
        ToggleChips(
            "On Home",
            HomePart.entries,
            on = { it !in hiddenHome },
            name = { it.displayName },
        ) { part, shown -> state.setHomePartHidden(part, !shown) }
        Spacer(Modifier.height(6.dp))
        Text(
            // The parts put away are named, so nobody is left wondering where a row went.
            if (hiddenHome.isEmpty()) {
                "Everything is on Home. Click a part to put it away."
            } else {
                "Put away: " + HomePart.entries.filter { it in hiddenHome }.joinToString { it.displayName } + "."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        // What a Hybrid search asks, beside what Home shows, since both are which services fill a page.
        val asked = preferences.hybridSearch
        ToggleChips(
            "Hybrid search asks",
            HYBRID_SEARCH_SERVICES,
            on = { it in asked },
            name = ::hybridServiceName,
        ) { service, on -> state.setHybridSearchService(service, on) }
        Spacer(Modifier.height(6.dp))
        Text(
            "At least one always stays. Spotify answers only while its songs play on Spotify, and VK Music once " +
                "you are signed in to it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        val hiddenPages = preferences.desktop.hiddenDestinations
        ToggleChips(
            "In the sidebar",
            HIDEABLE_DESTINATIONS,
            on = { sidebarShows(it, hiddenPages) },
            name = { it.sidebarLabel() },
            icon = { it.sidebarIcon() },
        ) { destination, shown ->
            state.updateDesktop { copy(hiddenDestinations = if (shown) hiddenDestinations - destination else hiddenDestinations + destination) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Home and Settings are always there. A page left out is still a key away, since 1 to 8 go to each " +
                "in the sidebar's order, and the player bar still opens now playing and the queue.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

/** The now playing screen: its layout, the cover, what is behind it and what sits beside it. */
@Composable
internal fun NowPlayingSettingsCard(preferences: NoctoriumPreferences, state: AppState) {
    val look = preferences.desktop.nowPlaying
    val backdrop = preferences.nowPlayingBackdrop
    SettingsPanelCard {
        CardHeading(Icons.Default.Album, "Now playing")
        Spacer(Modifier.height(6.dp))
        Text(
            "The screen the player bar opens. The sliders button on that screen changes all of this in place, " +
                "and the sidebar button beside it tucks the panel away.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(15.dp))
        Text("Layout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        NowPlayingLayoutPicker(look.layout) { layout -> state.updateNowPlaying { copy(layout = layout) } }
        Spacer(Modifier.height(7.dp))
        Text(look.layout.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Cover", CoverStyle.entries, look.cover, { it.displayName }) { cover -> state.updateNowPlaying { copy(cover = cover) } }
        Spacer(Modifier.height(6.dp))
        Text(look.cover.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Cover size", CoverSize.entries, look.coverSize, { it.displayName }) { size -> state.updateNowPlaying { copy(coverSize = size) } }
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Backdrop", NowPlayingBackdrop.entries, backdrop, { it.displayName }, state::setNowPlayingBackdrop)
        Spacer(Modifier.height(6.dp))
        Text(backdrop.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        ChoiceRow("Panel width", NowPlayingPanelWidth.entries, look.panelWidth, { it.displayName }) { width ->
            state.updateNowPlaying { copy(panelWidth = width) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "For the side by side layouts. Sing along gives the lyrics everything the column leaves.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        // Related is not offered: it has nothing to show yet, and opening on it would be opening on nothing.
        ChoiceRow("Open the panel on", listOf(NowPlayingTab.UP_NEXT, NowPlayingTab.LYRICS), look.openOn, { it.displayName }) { tab ->
            state.updateNowPlaying { copy(openOn = tab) }
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow(
            "The line being sung, under the cover",
            "Off shows the album's name there instead.",
            look.lyricLine,
        ) { on -> state.updateNowPlaying { copy(lyricLine = on) } }
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            "Follow button under the artist",
            "For following the artist on the service the track came from.",
            look.followButton,
        ) { on -> state.updateNowPlaying { copy(followButton = on) } }
    }
}

@Composable
internal fun CardHeading(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    // Set into the group box's top edge under a Windows skin, as a group box's caption was.
    if (skinned()) {
        GroupLegend(title)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(9.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** A labelled row of pills. Generic so every choice in this panel looks and behaves identically. */
@Composable
internal fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    name: (T) -> String,
    choose: (T) -> Unit,
) = ChoiceRow(label, options, selected, name, enabled = true, choose)

/**
 * The same, faded and unclickable while [enabled] is false: for a choice that has nothing to act on just
 * now, such as the equaliser while Spotify's own app plays the song.
 *
 * An overload rather than a parameter on the one above, which is called both with its last argument in
 * place and as a trailing block, and neither would survive a new parameter before it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    name: (T) -> String,
    enabled: Boolean,
    choose: (T) -> Unit,
) {
    // Option buttons under a Windows skin, which is what a choice of one was on those desktops.
    if (skinned()) {
        OptionButtons(label, options, selected, name, enabled, choose)
        return
    }
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        // Wrapped rather than cut off: five layouts and five seek bars do not fit a narrow window in one line.
        FlowRow(
            Modifier.graphicsLayer { alpha = if (enabled) 1f else .45f },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val active = option == selected
                Surface(
                    onClick = { choose(option) },
                    enabled = enabled,
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else ink(.05f),
                    border = BorderStroke(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = .55f) else ink(.1f),
                    ),
                ) {
                    Text(
                        name(option),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ToggleRow(title: String, description: String, checked: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
    // A checkbox with its words beside it under a Windows skin: neither desktop had a switch.
    if (skinned()) {
        CheckRow(title, description, checked, enabled, change)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Held off by a switch above it, the words fade with the switch, so the row reads as waiting and
        // not as a switch that has stuck.
        Column(Modifier.weight(1f).alpha(if (enabled) 1f else .45f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        // A gap, so a description long enough to wrap does not run right up against the switch.
        Spacer(Modifier.width(16.dp))
        Switch(checked, change, enabled = enabled)
    }
}

@Composable
private fun ThemeSwatch(preset: ThemePreset, colours: ThemeColours, selected: Boolean, choose: () -> Unit) {
    // A miniature of the theme itself: the page, a card on it, a line of writing and the accent. The name
    // underneath is for the ones that look alike at this size.
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
        Surface(
            onClick = choose,
            shape = RoundedCornerShape(10.dp),
            color = Color(colours.background),
            border = BorderStroke(
                2.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.size(84.dp, 56.dp),
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Box(
                    Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(5.dp)).background(Color(colours.card)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(Modifier.padding(start = 5.dp).size(width = 30.dp, height = 4.dp).clip(CircleShape).background(Color(colours.text)))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(colours.accent)))
                    Spacer(Modifier.width(5.dp))
                    Box(Modifier.size(width = 26.dp, height = 3.dp).clip(CircleShape).background(Color(colours.subtext)))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            preset.displayName,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Six hex colours and a switch: the listener's own theme.
 *
 * Applied only when every field reads as a colour, and warned about -- not refused -- when the writing
 * would sit too close to the page to read. SpMp offered a colour wheel here; six boxes that take the
 * values people copy out of a palette's README are what a theme actually arrives as.
 */
@Composable
private fun CustomThemeEditor(current: ThemeColours, apply: (ThemeColours) -> Unit) {
    var background by remember(current) { mutableStateOf(current.background.toHexColour()) }
    var panel by remember(current) { mutableStateOf(current.panel.toHexColour()) }
    var card by remember(current) { mutableStateOf(current.card.toHexColour()) }
    var text by remember(current) { mutableStateOf(current.text.toHexColour()) }
    var subtext by remember(current) { mutableStateOf(current.subtext.toHexColour()) }
    var accent by remember(current) { mutableStateOf(current.accent.toHexColour()) }
    var light by remember(current) { mutableStateOf(current.light) }

    val parsed = listOf(background, panel, card, text, subtext, accent).map(::parseHexColour)
    val complete = parsed.all { it != null }
    val readable = complete && contrastRatio(parsed[3]!!, parsed[0]!!) >= 4.5

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your colours", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HexField("Background", background) { background = it }
            HexField("Panel", panel) { panel = it }
            HexField("Card", card) { card = it }
            HexField("Text", text) { text = it }
            HexField("Muted text", subtext) { subtext = it }
            HexField("Accent", accent) { accent = it }
        }
        ToggleRow("Light theme", "Dark writing on a pale page. Tells the window and the title bar which way round the theme is.", light) { light = it }
        if (complete && !readable) {
            Text(
                "The text and the background are too close to read comfortably (contrast ${"%.1f".format(contrastRatio(parsed[3]!!, parsed[0]!!))}, where 4.5 is the floor).",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
            )
        }
        Button(
            onClick = { apply(ThemeColours(parsed[0]!!, parsed[1]!!, parsed[2]!!, parsed[3]!!, parsed[4]!!, parsed[5]!!, light)) },
            enabled = complete,
        ) { Text("Apply") }
    }
}

@Composable
private fun HexField(label: String, value: String, change: (String) -> Unit) {
    val colour = parseHexColour(value)
    OutlinedTextField(
        value,
        { change(it.take(9)) },
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        isError = colour == null,
        leadingIcon = {
            Box(
                Modifier.size(16.dp).clip(CircleShape)
                    .background(colour?.let { Color(it) } ?: Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
        modifier = Modifier.width(150.dp),
    )
}

@Composable
private fun AccentSwatch(option: AccentPreset, themeAccent: Color, customAccent: Color, selected: Boolean, choose: () -> Unit) {
    // The theme's own accent is shown as itself, since it is a real colour, and so is the listener's own;
    // only the artwork option has none.
    val colour = option.argb?.let { Color(it) }
        ?: themeAccent.takeIf { option == AccentPreset.THEME }
        ?: customAccent.takeIf { option == AccentPreset.CUSTOM }
    Surface(
        onClick = choose,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            2.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.size(40.dp),
    ) {
        Box(Modifier.padding(5.dp), contentAlignment = Alignment.Center) {
            if (colour != null) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(colour))
                // A dropper on the listener's own, which is the one swatch that opens something.
                if (option == AccentPreset.CUSTOM) {
                    Icon(
                        Icons.Default.Colorize,
                        "Your own colour",
                        Modifier.size(14.dp),
                        tint = if (colour.luminance() > .45f) Color.Black.copy(alpha = .7f) else Color.White,
                    )
                }
            } else {
                // The artwork option shows a spectrum rather than a single colour, since it has none of its own.
                Box(
                    Modifier.fillMaxSize().clip(CircleShape).background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF6EC7), Color(0xFFFF9757), Color(0xFF5FE3B0),
                                Color(0xFF5AB2FF), Color(0xFFB47CFF), Color(0xFFFF6EC7),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProgressStyleOption(option: ProgressBarStyle, selected: Boolean, choose: () -> Unit) {
    // A live bar at a plausible position, so each option shows exactly what it will be. Playing rather
    // than paused, because one of the styles is only itself while the music is running: a paused Wave
    // is a flat line, and previewing it as one would be advertising the wrong thing.
    val preview = remember {
        PlaybackState(
            track = null,
            status = PlaybackStatus.PLAYING,
            positionMs = 156_000,
            durationMs = 258_000,
        )
    }
    // An option button and its words, with the bar in a sample well beneath, under a Windows skin.
    if (skinned()) {
        PreviewedOption(option.displayName, option.description, selected, choose) {
            PlaybackProgressBar(preview, {}, Modifier.fillMaxWidth(), option)
        }
        return
    }
    Surface(
        onClick = choose,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f) else ink(.04f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else ink(.08f),
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = choose)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(option.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(option.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = .55f), shape = RoundedCornerShape(10.dp)) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    PlaybackProgressBar(preview, {}, Modifier.fillMaxWidth(), option)
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    var name by remember(preferences.profileName) { mutableStateOf(preferences.profileName) }
    SettingsPanelCard {
        Icon(Icons.Default.AccountCircle, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Local profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("This name stays on this computer and identifies your Noctorium setup.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(name, { name = it.take(40) }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth().tracksTyping())
        Spacer(Modifier.height(14.dp))
        Button({ state.setProfileName(name) }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(7.dp)); Text("Save profile") }
    }
}

/**
 * Hosts SoundCloud's own sign-in page in embedded Chromium. SoundCloud has no OAuth to build against — app
 * registration has been closed for years — so signing in on their real page inside Noctorium is the closest thing
 * to a first-party login, and the session it produces serves both playback and liking.
 */
@Composable
private fun SoundCloudSignInWindow(state: AppState, close: () -> Unit) {
    var status by remember { mutableStateOf("Starting the embedded browser…") }
    var component by remember { mutableStateOf<java.awt.Component?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var finishing by remember { mutableStateOf(false) }
    val session = remember { state.dataDirectory()?.let { EmbeddedBrowserSession(it.resolve("chromium")) } }

    DisposableEffect(session) { onDispose { session?.dispose() } }

    LaunchedEffect(session) {
        if (session == null) {
            status = "Noctorium has nowhere to store the browser on this system."
            return@LaunchedEffect
        }
        runCatching {
            session.start(
                url = SOUNDCLOUD_SIGN_IN,
                onProgress = { progress -> status = "Preparing Chromium — $progress" },
                onPageLoaded = { url -> currentUrl = url },
            )
        }.onSuccess { ui ->
            component = ui
            // As with Google: the store outlives the application, so a dead session still in it is enough
            // for SoundCloud to skip the form and return to the signed-in page, leaving the same dead
            // cookies to be harvested again. Clearing turns the button back into a login.
            if (session.clearCookies(SOUNDCLOUD_SESSION_URLS)) {
                session.navigate(SOUNDCLOUD_SIGN_IN)
            }
            status = "Sign in to SoundCloud below. Noctorium picks the session up on its own."
        }.onFailure { error ->
            status = "Could not start the embedded browser: ${error.message?.take(180)}"
        }
    }

    // Cookies are read from a coroutine rather than inside a Chromium callback, which keeps the browser's own
    // threads free and avoids waiting on it from inside its own event.
    LaunchedEffect(component) {
        val live = session ?: return@LaunchedEffect
        if (component == null) return@LaunchedEffect
        // A token already found to be dead. Kept so the same one is not tested repeatedly: each test is a
        // real request, and a stale store returns the same value every couple of seconds.
        var rejected: String? = null
        while (true) {
            delay(2_500)
            val cookies = runCatching { live.harvestCookies("https://soundcloud.com") }.getOrDefault(emptyList())
            if (!isSoundCloudSignedIn(cookies)) continue

            val token = cookies.firstOrNull { it.name == "oauth_token" }?.value
            if (token != null && token == rejected) continue

            finishing = true
            status = "Checking the session with SoundCloud…"
            val destination = state.dataDirectory()?.resolve("soundcloud.cookies") ?: return@LaunchedEffect
            // Beside the real file, not over it: see the YouTube window for why.
            val candidate = runCatching {
                writeCookieFile(cookies, destination.resolveSibling("soundcloud.cookies.checking"))
            }.getOrNull()
            if (candidate == null) {
                status = "Signed in, but the session could not be written to disk."
                finishing = false
                continue
            }

            // Present is not the same as accepted; one real request is what tells them apart.
            if (!state.sessionIsAccepted(ProviderType.SOUNDCLOUD, candidate)) {
                runCatching { java.nio.file.Files.deleteIfExists(candidate) }
                rejected = token
                finishing = false
                status = "That session has expired. Clearing it so SoundCloud asks you to sign in again…"
                live.clearCookies(SOUNDCLOUD_SESSION_URLS)
                live.navigate(SOUNDCLOUD_SIGN_IN)
                continue
            }
            val saved = runCatching { adoptCookieFile(candidate, destination) }.getOrNull()
            if (saved == null) {
                status = "Signed in, but the session could not be saved."
                finishing = false
                continue
            }

            status = "Signed in — finding your profile…"
            // SoundCloud answers its own /you/ routes by moving to the real profile, so a short detour
            // through the signed-in browser names the account with no request of Noctorium's own.
            live.navigate(SOUNDCLOUD_OWN_LIKES)
            var permalink: String? = null
            repeat(8) {
                delay(1_000)
                permalink = permalinkFromBrowserUrl(live.currentUrl())
                if (permalink != null) return@repeat
            }
            state.completeSoundCloudSignIn(saved.toString(), token, permalink)
            close()
            return@LaunchedEffect
        }

    }

    DialogWindow(
        onCloseRequest = close,
        state = rememberDialogState(width = 980.dp, height = 760.dp),
        title = "Sign in to SoundCloud",
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (finishing || component == null) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(status, fontSize = 12.sp)
                    if (currentUrl.isNotBlank()) {
                        Text(currentUrl, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton({
                    session?.clearCookies(SOUNDCLOUD_SESSION_URLS)
                    session?.navigate(SOUNDCLOUD_SIGN_IN)
                    status = "Starting over. Sign in to SoundCloud below."
                }) { Text("Start over") }
                Spacer(Modifier.width(8.dp))
                Button(close) { Text("Done") }
            }
            HorizontalDivider()
            component?.let { ui ->
                SwingPanel(background = Color.Black, factory = { ui }, modifier = Modifier.fillMaxSize())
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 520.dp)) {
                    Text("Getting the browser ready", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Chromium ships with Noctorium, so nothing is being downloaded — it is unpacked once on this " +
                            "computer and every sign-in after this opens straight away.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

/**
 * Signs in to YouTube Music on Google's own page inside Noctorium.
 *
 * Sign-in starts at the accounts page and finishes on the music site, because the cookies that authorise the
 * API belong to that domain rather than to the accounts one.
 */
@Composable
private fun YouTubeSignInWindow(state: AppState, close: () -> Unit) {
    var status by remember { mutableStateOf("Starting the embedded browser…") }
    var component by remember { mutableStateOf<java.awt.Component?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var finishing by remember { mutableStateOf(false) }
    val session = remember { state.dataDirectory()?.let { EmbeddedBrowserSession(it.resolve("chromium")) } }

    DisposableEffect(session) { onDispose { session?.dispose() } }

    LaunchedEffect(session) {
        if (session == null) {
            status = "Noctorium has nowhere to store the browser on this system."
            return@LaunchedEffect
        }
        runCatching {
            session.start(
                url = YOUTUBE_SIGN_IN,
                onProgress = { progress -> status = "Preparing Chromium — $progress" },
                onPageLoaded = { url -> currentUrl = url },
            )
        }.onSuccess { ui ->
            component = ui
            // The store is on disk and outlives the application, so pressing sign-in while a dead session
            // is still in it sends Google a session it recognises: it skips the form, returns to the
            // signed-in page, and the same dead cookies get harvested again. Clearing turns the button
            // back into a login. Done after the browser exists, because there is no cookie manager before.
            if (session.clearCookies(YOUTUBE_SESSION_URLS)) {
                session.navigate(YOUTUBE_SIGN_IN)
            }
            status = "Sign in with Google below. Noctorium picks the session up on its own."
        }.onFailure { error ->
            status = "Could not start the embedded browser: ${error.message?.take(180)}"
        }
    }

    LaunchedEffect(component) {
        val live = session ?: return@LaunchedEffect
        if (component == null) return@LaunchedEffect
        var visitedMusic = false
        // The signing cookie of a session already found to be dead. Kept so the same one is not tested
        // over and over: each test is a real request, and a stale store hands back the same value every
        // couple of seconds. Once a person actually signs in the value changes, and that is the signal.
        var rejected: String? = null
        while (true) {
            delay(2_500)
            val cookies = runCatching { live.harvestCookies(YOUTUBE_MUSIC_HOME) }.getOrDefault(emptyList())
            if (!isYouTubeSignedIn(cookies)) {
                // The signing cookie is set for the music site once it has been visited, so go there after
                // the accounts page has done its part.
                if (!visitedMusic && live.currentUrl()?.contains("accounts.google.com") == false) {
                    visitedMusic = true
                    live.navigate(YOUTUBE_MUSIC_HOME)
                }
                continue
            }
            val signature = cookies.firstOrNull { it.name == "SAPISID" || it.name == "__Secure-3PAPISID" }?.value
            if (signature != null && signature == rejected) continue

            finishing = true
            status = "Checking the session with YouTube…"
            val destination = state.dataDirectory()?.resolve("youtube.cookies") ?: return@LaunchedEffect
            // Written beside the real file and only moved over it once the provider has accepted it. The
            // check needs a file to point at, and writing that file over the working session first would
            // destroy a good one every time an old cookie turned out to be dead.
            // Google's own cookies as well as YouTube's: they are what renews the YouTube ones once those go
            // stale, and without them a stale session could only be signed in again. See YOUTUBE_HARVEST_URLS.
            val google = runCatching { live.harvestCookies(YOUTUBE_HARVEST_URLS.last()) }.getOrDefault(emptyList())
            val candidate = runCatching {
                writeCookieFile(mergeCookies(cookies, google), destination.resolveSibling("youtube.cookies.checking"))
            }.getOrNull()
            if (candidate == null) {
                status = "Signed in, but the session could not be written to disk."
                finishing = false
                continue
            }

            // A cookie being present is not a session. One real request tells the two apart, and without
            // it a dead session is saved, reported as success, and refused by everything afterwards.
            if (state.sessionIsAccepted(ProviderType.YOUTUBE_MUSIC, candidate)) {
                status = "Signed in — saving the session…"
                val saved = runCatching { adoptCookieFile(candidate, destination) }.getOrNull()
                if (saved == null) {
                    status = "Signed in, but the session could not be saved."
                    finishing = false
                    continue
                }
                state.completeYouTubeSignIn(saved.toString(), live.userAgent())
                // The browser's copy goes once Noctorium has its own, as SimpMusic does it. Two holders of
                // one session is how a copy dies: whichever renews the cookies turns the other's into
                // yesterday's, and the browser would be renewing them every time it touched Google.
                live.clearCookies(YOUTUBE_SESSION_URLS)
                close()
                return@LaunchedEffect
            }
            runCatching { java.nio.file.Files.deleteIfExists(candidate) }


            // Present but not accepted: the store is holding an old session, and Google will keep skipping
            // the login form while it is there. Clearing it is what turns this window back into a login.
            rejected = signature
            finishing = false
            status = "That session has expired. Clearing it so Google asks you to sign in again…"
            live.clearCookies(YOUTUBE_SESSION_URLS)
            live.navigate(YOUTUBE_SIGN_IN)
            visitedMusic = false
        }
    }


    DialogWindow(
        onCloseRequest = close,
        state = rememberDialogState(width = 1000.dp, height = 780.dp),
        title = "Sign in to YouTube Music",
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (finishing || component == null) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(status, fontSize = 12.sp)
                    if (currentUrl.isNotBlank()) {
                        Text(
                            currentUrl,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Signing out at Google's end as well, for when it has decided you are still signed in
                // and there is no form to type into.
                OutlinedButton({
                    session?.clearCookies(YOUTUBE_SESSION_URLS)
                    session?.navigate(YOUTUBE_SIGN_IN)
                    status = "Starting over. Sign in with Google below."
                }) { Text("Start over") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton({ session?.navigate(YOUTUBE_MUSIC_HOME) }) { Text("Go to YouTube Music") }
                Spacer(Modifier.width(8.dp))
                // Named for what it does rather than Cancel, which after a successful sign-in reads as
                // though it would undo one.
                Button(close) { Text("Done") }
            }
            HorizontalDivider()
            component?.let { ui ->
                SwingPanel(background = Color.Black, factory = { ui }, modifier = Modifier.fillMaxSize())
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Chromium ships with Noctorium and is unpacked once on this computer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** YouTube Music through a signed-in browser session, the same arrangement SoundCloud uses. */
@Composable
private fun YouTubeAccountPanel(settings: SettingsState, state: AppState) {
    val likes by state.likes.collectAsState()
    val source = settings.preferences.youtubeCookies
    var signInOpen by remember { mutableStateOf(false) }
    var phoneOpen by remember { mutableStateOf(false) }
    var pasteOpen by remember { mutableStateOf(false) }
    if (signInOpen) YouTubeSignInWindow(state) { signInOpen = false }
    if (phoneOpen) PhoneSignInDialog(state) { phoneOpen = false }
    if (pasteOpen) PasteCookiesDialog(state) { pasteOpen = false }
    // The channels are read as soon as there is a session to read them with, so the account and its
    // pictures are simply there rather than behind a button.
    LaunchedEffect(likes.youTubeReady) {
        if (likes.youTubeReady && likes.youTubeChannels.isEmpty()) state.loadYouTubeChannels()
    }

    Column(Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (likes.youTubeReady) "Signed in to YouTube Music" else "Sign in to YouTube Music",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Signs in on Google's own page inside Noctorium. Your playlists and likes then work through " +
                            "the same interface the YouTube Music site uses, with no Google Cloud project involved.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            AccountStatusRow(settings.youtubeAccount)
            Spacer(Modifier.height(15.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ signInOpen = true }) {
                    Icon(Icons.Default.Login, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (source.isConfigured) "Sign in again" else "Sign in to YouTube Music")
                }
                // The way SimpMusic signs its desktop in: on the phone, where the account usually already is.
                OutlinedButton({ phoneOpen = true }) {
                    Icon(Icons.Default.PhoneAndroid, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Sign in with your phone")
                }
                OutlinedButton({ pasteOpen = true }) { Text("Paste cookies") }
                if (source.isConfigured) {
                    OutlinedButton(state::checkYouTubeSignIn) { Text("Check now") }
                    OutlinedButton({ state.disconnectAccount(ProviderType.YOUTUBE_MUSIC) }) { Text("Disconnect") }
                }
            }
            likes.message?.let { message ->
                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                        IconButton(state::clearLikeMessage, Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Dismiss", Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        if (likes.youTubeReady) {
            SettingsPanelCard {
                CardHeading(Icons.Default.SwitchAccount, "Account and channel")
                Spacer(Modifier.height(7.dp))
                Text(
                    "Every Google account signed in to this session, and every channel each one owns. Whichever is " +
                        "chosen is the one Noctorium acts as: its likes, its playlists, its history.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(11.dp))
                if (likes.youTubeChannels.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(7.dp))
                        Text(settings.preferences.youtubeChannelName.ifBlank { "Default channel" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    YouTubeChannelChooser(likes.youTubeChannels, settings, state)
                }
                Spacer(Modifier.height(11.dp))
                OutlinedButton(state::loadYouTubeChannels) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (likes.youTubeChannels.isEmpty()) "Find my channels" else "Refresh")
                }
            }
            SettingsPanelCard { YouTubeHistorySetting(settings, state) }
        }

        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp)); Text("What signing in covers", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("Your playlists and liked songs, read and written the way the YouTube Music site does.")
            AccountFact("Liking a YouTube track in Noctorium marks it liked on your account.")
            AccountFact("Following an artist from the now playing screen follows them on YouTube Music.")
            AccountFact("What you play here can go into your YouTube Music history, as it would playing it there.")
            AccountFact("Your library and playlists come from this session; playback itself resolves without it.")
            AccountFact("Your password goes to Google's page, never to Noctorium, and only the session is kept.")
            AccountFact("Noctorium checks every few hours that the session still works, renews it if it has gone stale, and says so if it cannot.")
        }
    }
}

@Composable
private fun AccountConnectionPanel(
    provider: ProviderType,
    source: CookieSource,
    connection: AccountConnectionState,
    state: AppState,
    savedSoundCloudUsername: String = "",
) {
    var soundCloudUsername by remember(savedSoundCloudUsername) { mutableStateOf(savedSoundCloudUsername) }
    var useCookieFile by remember(source) { mutableStateOf(source.usesCookieFile) }
    var selectedBrowser by remember(source) { mutableStateOf(source.browser ?: BrowserSession.FIREFOX) }
    var profile by remember(source) { mutableStateOf(source.profile) }
    var container by remember(source) { mutableStateOf(source.container) }
    var cookieFile by remember(source) { mutableStateOf(source.cookieFile) }
    var showAdvanced by remember(source) { mutableStateOf(source.profile.isNotBlank() || source.container.isNotBlank()) }
    var browserMenuOpen by remember { mutableStateOf(false) }

    val checking = connection.status == AccountConnectionStatus.CHECKING
    val draft = if (useCookieFile) {
        CookieSource.ofFile(cookieFile)
    } else {
        CookieSource.ofBrowser(selectedBrowser, profile, container)
    }

    Column(Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (provider == ProviderType.SOUNDCLOUD) Icons.Default.Cloud else Icons.Default.PlayCircle,
                    null,
                    Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (source.isConfigured) "Signed in through ${source.describe()}" else "Public mode",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Noctorium reuses an account you are already signed into. Only the location of the cookies is saved — never the cookies themselves.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            AccountStatusRow(connection)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button({ state.connectAccount(provider, draft) }, enabled = !checking && draft.isConfigured) {
                    if (checking) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Link, null)
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(if (checking) "Checking…" else if (source.isConfigured) "Save and check" else "Connect and check")
                }
                if (source.isConfigured) {
                    OutlinedButton({ state.verifyAccount(provider) }, enabled = !checking) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Check again")
                    }
                    OutlinedButton({ state.disconnectAccount(provider) }, enabled = !checking) { Text("Disconnect") }
                }
            }
        }

        if (provider == ProviderType.SOUNDCLOUD) {
            val likes by state.likes.collectAsState()
            var signInOpen by remember { mutableStateOf(false) }
            if (signInOpen) SoundCloudSignInWindow(state) { signInOpen = false }

            SettingsPanelCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Text("Sign in to SoundCloud", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "Signs in on SoundCloud's own page inside Noctorium. SoundCloud issues no API credentials to new " +
                        "apps, so this is how a first-party login is done — one sign-in covers playback, your " +
                        "playlists and liking, with no browser cookie fiddling.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(13.dp))
                Button({ signInOpen = true }) {
                    Icon(Icons.Default.Login, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (source.usesCookieFile) "Sign in again" else "Sign in to SoundCloud")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Chromium is bundled with Noctorium, so the first sign-in unpacks it rather than downloading it. " +
                        "Your password goes to SoundCloud's page, never to Noctorium.",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp,
                )
            }

            SettingsPanelCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (likes.soundCloudReady) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        Modifier.size(20.dp),
                        tint = if (likes.soundCloudReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(9.dp))
                    Text("Liking tracks on SoundCloud", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    if (likes.soundCloudReady) {
                        "Connected. Hearts in Noctorium are written to your SoundCloud likes."
                    } else {
                        "Reading needs only cookies, but liking writes to your account, so Noctorium needs the session " +
                            "token from the browser you connected. It is stored encrypted for your Windows account " +
                            "and nothing else from the cookie jar is kept."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(state::connectSoundCloudLiking) {
                        Icon(Icons.Default.Lock, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (likes.soundCloudReady) "Refresh token" else "Connect liking")
                    }
                    if (likes.soundCloudReady) {
                        OutlinedButton(state::disconnectSoundCloudLiking) { Text("Disconnect") }
                    }
                }
                likes.message?.let { message ->
                    Spacer(Modifier.height(11.dp))
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                            IconButton(state::clearLikeMessage, Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, "Dismiss", Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            SettingsPanelCard {
                Text("Your SoundCloud profile name", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "SoundCloud finds your own playlists by profile name, and cookies do not reveal it. Copy the last part of your profile link — soundcloud.com/<name>.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(13.dp))
                OutlinedTextField(
                    soundCloudUsername,
                    { soundCloudUsername = it.take(80) },
                    label = { Text("Profile name") },
                    placeholder = { Text("your-name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        { state.setSoundCloudUsername(soundCloudUsername) },
                        enabled = soundCloudUsername.isNotBlank(),
                    ) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(7.dp)); Text("Save profile name") }
                    OutlinedButton({ state.detectSoundCloudProfile() }) {
                        Icon(Icons.Default.Search, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Detect from my session")
                    }
                }
            }
        }

        SettingsPanelCard {
            Text("Where cookies come from", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FilterChip(!useCookieFile, { useCookieFile = false }, { Text("Browser session") })
                FilterChip(useCookieFile, { useCookieFile = true }, { Text("cookies.txt file") })
            }
            Spacer(Modifier.height(16.dp))
            if (useCookieFile) {
                OutlinedTextField(
                    cookieFile,
                    { cookieFile = it.trim() },
                    label = { Text("Path to cookies.txt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton({ pickCookieFile()?.let { cookieFile = it } }) {
                    Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Browse…")
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Export the file with a cookies.txt browser extension while you are signed in. This is the option that works when a Chrome-based browser refuses to hand over its cookies.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            } else {
                Box {
                    OutlinedButton({ browserMenuOpen = true }, Modifier.widthIn(min = 280.dp)) {
                        Icon(Icons.Default.Language, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(selectedBrowser.displayName + if (selectedBrowser == BrowserSession.FIREFOX) " · recommended" else "")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(browserMenuOpen, { browserMenuOpen = false }) {
                        BrowserSession.entries.forEach { browser ->
                            DropdownMenuItem(
                                text = {
                                    Text(browser.displayName + if (browser == BrowserSession.FIREFOX) " · recommended" else "")
                                },
                                leadingIcon = { if (browser == selectedBrowser) Icon(Icons.Default.Check, null) },
                                onClick = { selectedBrowser = browser; browserMenuOpen = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (showAdvanced) {
                    OutlinedTextField(
                        profile,
                        { profile = it.take(120) },
                        label = { Text("Browser profile (optional)") },
                        placeholder = { Text(if (selectedBrowser.chromium) "Profile 2" else "default-release") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().tracksTyping(),
                    )
                    if (!selectedBrowser.chromium) {
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(
                            container,
                            { container = it.take(120) },
                            label = { Text("Firefox container (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().tracksTyping(),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Leave the profile empty to use the browser's default. Name it when the signed-in account lives in a second profile.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                } else {
                    TextButton({ showAdvanced = true }) { Text("Choose a specific profile or container") }
                }
            }
        }

        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp)); Text("What to expect", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("Firefox is the reliable choice — yt-dlp reads its cookie database directly.")
            AccountFact(
                "Chrome, Edge, Brave, Opera and Vivaldi lock their cookie database while running, and on Windows " +
                    "Chrome 127 and later encrypt it in a way yt-dlp cannot read. Close the browser and try, then " +
                    "fall back to a cookies.txt file.",
            )
            AccountFact(
                if (provider == ProviderType.SOUNDCLOUD) {
                    "SoundCloud has no cheap private page to load, so Noctorium confirms the cookies are read and accepted rather than naming your account."
                } else {
                    "Connecting loads your subscriptions feed, which only a signed-in account can see — that is how Noctorium knows the session is real."
                },
            )
            AccountFact("Noctorium never asks for your password, and disconnecting removes the session immediately.")
        }
    }
}

@Composable
private fun AccountStatusRow(connection: AccountConnectionState) {
    val (icon, tint) = when (connection.status) {
        AccountConnectionStatus.CONNECTED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        AccountConnectionStatus.CHECKING -> Icons.Default.Sync to MaterialTheme.colorScheme.onSurfaceVariant
        AccountConnectionStatus.WARNING -> Icons.Default.Warning to MaterialTheme.colorScheme.tertiary
        AccountConnectionStatus.ERROR -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
        AccountConnectionStatus.DISCONNECTED -> Icons.Default.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .55f), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp)) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    connection.detail ?: "Not connected — searches and playback use public access only.",
                    fontSize = 12.sp,
                )
                connection.hint?.let {
                    Spacer(Modifier.height(5.dp))
                    SelectionContainer { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
internal fun AccountFact(text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

private fun accountSummary(source: CookieSource, connection: AccountConnectionState): String = when {
    !source.isConfigured -> "Public mode — connect a browser session"
    connection.status == AccountConnectionStatus.CHECKING -> "Checking ${source.describe()}…"
    connection.status == AccountConnectionStatus.ERROR -> "${source.describe()} — needs attention"
    connection.status == AccountConnectionStatus.WARNING -> "${source.describe()} — not confirmed"
    else -> "Using ${source.describe()}"
}

private fun pickCookieFile(): String? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Choose your exported cookies.txt", java.awt.FileDialog.LOAD)
    dialog.file = "*.txt"
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val name = dialog.file ?: return null
    return java.io.File(directory, name).absolutePath
}

@Composable
private fun ScrobblingSettingsPanel(settings: SettingsState, state: AppState) {
    var listenBrainzToken by remember { mutableStateOf("") }
    var lastFmApiKey by remember { mutableStateOf("") }
    var lastFmSharedSecret by remember { mutableStateOf("") }
    var useCustomLastFmApplication by remember { mutableStateOf(false) }
    val scrobbling = settings.scrobbling
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = chromePadding(bottom = 24.dp)) {
        if (scrobbling.lastEvent != null || scrobbling.scrobblesThisSession > 0) {
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp)); Column {
                            Text(
                                buildString {
                                    append("${scrobbling.scrobblesThisSession} scrobbled this session")
                                    if (scrobbling.pendingScrobbles > 0) append(" • ${scrobbling.pendingScrobbles} queued")
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                            scrobbling.lastEvent?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
        item {
            SettingsPanelCard {
                ScrobbleServiceHeader("Last.fm", scrobbling.lastFm)
                Spacer(Modifier.height(8.dp))
                Text("Noctorium opens Last.fm in your browser for approval and finishes sign-in by itself once you allow it. Your Last.fm password is never entered into Noctorium.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(15.dp))
                when (scrobbling.lastFm.status) {
                    ScrobbleConnectionStatus.CONNECTED -> OutlinedButton(state::disconnectLastFm) { Icon(Icons.Default.LinkOff, null); Spacer(Modifier.width(7.dp)); Text("Disconnect") }
                    ScrobbleConnectionStatus.AWAITING_APPROVAL -> Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Waiting for your approval in the browser…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Button(state::finishLastFmLogin) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(7.dp)); Text("I approved Noctorium") }
                            OutlinedButton(state::beginLastFmLogin) { Text("Reopen browser") }
                        }
                    }
                    ScrobbleConnectionStatus.CONNECTING -> Button({}, enabled = false) { CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Connecting…") }
                    else -> {
                        Button(state::beginLastFmLogin, enabled = scrobbling.lastFmConfigured) {
                            Icon(Icons.Default.OpenInBrowser, null); Spacer(Modifier.width(7.dp)); Text("Connect Last.fm")
                        }
                        Spacer(Modifier.height(8.dp))
                        if (!useCustomLastFmApplication) {
                            TextButton({ useCustomLastFmApplication = true }) { Text("Use my own Last.fm application keys") }
                        } else {
                            OutlinedTextField(
                                lastFmApiKey,
                                { lastFmApiKey = it.trim().take(32) },
                                label = { Text("Last.fm API key") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().tracksTyping(),
                            )
                            Spacer(Modifier.height(9.dp))
                            OutlinedTextField(
                                lastFmSharedSecret,
                                { lastFmSharedSecret = it.trim().take(32) },
                                label = { Text("Last.fm shared secret") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().tracksTyping(),
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Button(
                                    onClick = {
                                        val apiKey = lastFmApiKey
                                        val secret = lastFmSharedSecret
                                        lastFmApiKey = ""
                                        lastFmSharedSecret = ""
                                        useCustomLastFmApplication = false
                                        state.configureLastFmApplication(apiKey, secret)
                                    },
                                    enabled = lastFmApiKey.length == 32 && lastFmSharedSecret.length == 32,
                                ) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(7.dp)); Text("Save securely") }
                                TextButton({ state.openExternalUrl("https://www.last.fm/api/account/create") }) { Text("Create credentials") }
                                TextButton({ lastFmApiKey = ""; lastFmSharedSecret = ""; useCustomLastFmApplication = false }) { Text("Cancel") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Optional — Noctorium already ships with application credentials. These only replace which application Last.fm sees; account approval still happens in your browser.", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item {
            SettingsPanelCard {
                ScrobbleServiceHeader("ListenBrainz", scrobbling.listenBrainz)
                Spacer(Modifier.height(8.dp))
                Text("Paste the user token from your ListenBrainz settings. It is encrypted for your Windows account before being saved.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                if (scrobbling.listenBrainz.status == ScrobbleConnectionStatus.CONNECTED) {
                    OutlinedButton(state::disconnectListenBrainz) { Icon(Icons.Default.LinkOff, null); Spacer(Modifier.width(7.dp)); Text("Disconnect") }
                } else {
                    OutlinedTextField(
                        listenBrainzToken,
                        { listenBrainzToken = it.take(200) },
                        label = { Text("ListenBrainz user token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().tracksTyping(),
                    )
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Button(
                            onClick = { val token = listenBrainzToken; listenBrainzToken = ""; state.connectListenBrainz(token) },
                            enabled = listenBrainzToken.isNotBlank() && scrobbling.listenBrainz.status != ScrobbleConnectionStatus.CONNECTING,
                        ) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(7.dp)); Text("Validate and connect") }
                        TextButton({ state.openExternalUrl("https://listenbrainz.org/settings/") }) { Text("Get token") }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp)); Text("Tracks count after half their duration or 4 minutes, whichever comes first. Tracks 30 seconds or shorter are ignored.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ScrobbleServiceHeader(name: String, service: ScrobbleServiceState) {
    val color = when (service.status) {
        ScrobbleConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
        ScrobbleConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
        ScrobbleConnectionStatus.AWAITING_APPROVAL -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            when (service.status) {
                ScrobbleConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                ScrobbleConnectionStatus.ERROR -> Icons.Default.ErrorOutline
                ScrobbleConnectionStatus.AWAITING_APPROVAL -> Icons.Default.OpenInBrowser
                else -> Icons.Default.Radio
            },
            null,
            tint = color,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                service.username ?: service.message ?: service.status.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase),
                color = color,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
internal fun LyricsSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    val providers = listOf(
        "LRCLIB" to true,
        "Better Lyrics" to true,
        "Karalyr" to true,
        "SyncLRC" to true,
        "lyrics.ovh" to true,
        "Musixmatch" to !System.getenv("NOCTORIUM_MUSIXMATCH_API_KEY").isNullOrBlank(),
        "Happi" to !System.getenv("NOCTORIUM_HAPPI_API_KEY").isNullOrBlank(),
        "Genius" to true,
    )
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = chromePadding()) {
        item {
            SettingsPanelCard {
                // The same choice as picking a source above the lyrics themselves, which is where it is
                // usually made; this is where it can be seen and put back.
                ChoiceRow(
                    "Open lyrics on",
                    listOf<LyricsProviderId?>(null) + LyricsProviderId.entries,
                    preferences.lyricsProvider,
                    { it?.displayName ?: "The best answer" },
                    { choice -> if (choice == null) state.clearPreferredLyricsProvider() else state.selectLyricsProvider(choice) },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Every source is still asked. A song your choice has nothing for opens on the best of the others.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        item { Text("Core providers work without an account. Optional commercial sources show whether their API key is available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
        items(providers, key = { it.first }) { (name, ready) ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape = RoundedCornerShape(11.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (ready) Icons.Default.CheckCircle else Icons.Default.Key, null, tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(11.dp)); Text(name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text(if (ready) "Ready" else "API key needed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

/** What the startup tile says: how Noctorium starts, and what closing it does. */
@Composable
private fun startupSubtitle(preferences: NoctoriumPreferences): String {
    val mode = remember { LaunchAtLogin.system.mode() }
    val closing = if (preferences.desktop.closeToTray && DesktopTray.supported) "Closes to the tray" else "Closing quits"
    val starting = when (mode) {
        StartupMode.OFF -> null
        StartupMode.WINDOW -> "Starts with ${computerName()}"
        StartupMode.TRAY -> "Starts in the tray"
    }
    return listOfNotNull(starting, closing).joinToString(" · ")
}

/** Windows by name, where it is Windows; the Mac, on a Mac; anything else is the computer. */
private fun computerName(): String {
    val os = System.getProperty("os.name").orEmpty()
    return when {
        os.startsWith("Windows", ignoreCase = true) -> "Windows"
        os.startsWith("Mac", ignoreCase = true) -> "your Mac"
        else -> "the computer"
    }
}

/**
 * What the close button does, and whether Noctorium starts with the computer.
 *
 * The start is read back from the operating system after every change rather than assumed, because that is
 * where it lives -- the listener can switch it off in Task Manager just as well as here -- and a page that
 * said "on" about an entry Windows refused to write would be the worst of both.
 */
@Composable
internal fun StartupSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    val startup = LaunchAtLogin.system
    var mode by remember { mutableStateOf(startup.mode()) }
    var refused by remember { mutableStateOf(false) }
    val trayAvailable = DesktopTray.supported

    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            CardHeading(Icons.Default.Close, "Closing the window")
            Spacer(Modifier.height(12.dp))
            ChoiceRow(
                "When you close Noctorium",
                listOf(false, true),
                preferences.desktop.closeToTray && trayAvailable,
                { toTray -> if (toTray) "Keep playing in the tray" else "Quit" },
                { toTray -> if (trayAvailable) state.updateDesktop { copy(closeToTray = toTray) } },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    !trayAvailable ->
                        "This desktop has no system tray to wait in, so closing always quits. The music would " +
                            "otherwise go on with nothing on screen to stop it."
                    preferences.desktop.closeToTray ->
                        "The music carries on and Noctorium waits in the tray. Click its icon to open the window " +
                            "again, or right-click it for the controls and Quit."
                    else -> "Closing the window stops the music and quits Noctorium."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.NotificationsActive, "Announcing tracks")
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                "Say what is playing when a song starts",
                if (trayAvailable) {
                    "A small note from the tray with the title and the artist. Not while you are looking at " +
                        "Noctorium, and not for the same song twice in a row. The note comes from Noctorium's " +
                        "icon, so the icon stays in the tray while this is on."
                } else {
                    "This desktop has no system tray for the note to come from."
                },
                preferences.desktop.announceTracks && trayAvailable,
                enabled = trayAvailable,
            ) { on -> state.updateDesktop { copy(announceTracks = on) } }
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.PowerSettingsNew, "Start with ${computerName()}")
            Spacer(Modifier.height(12.dp))
            if (startup.available) {
                ChoiceRow(
                    "When you sign in",
                    StartupMode.entries,
                    mode,
                    { it.displayName },
                    { choice ->
                        refused = !startup.set(choice)
                        mode = startup.mode()
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(mode.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                if (mode == StartupMode.TRAY && !trayAvailable) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "With no tray on this desktop it opens minimised instead, so it can still be found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                if (refused) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "That could not be changed. Your computer's own startup settings may be managed by " +
                            "somebody else.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }
            } else {
                Text(
                    "Only an installed Noctorium can start with ${computerName()}. This one is running from its " +
                        "source, and would not be there to start next time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun DiscordSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    val discord = preferences.discord
    val status by state.discordStatus.collectAsState()
    var applicationId by remember(discord.applicationId) { mutableStateOf(discord.applicationId) }

    Column(Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsEsports, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Show listening activity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Publishes the current track to your Discord profile over the local client. Nothing leaves this computer except what you see below.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                Switch(discord.enabled, state::setDiscordPresence)
            }
            Spacer(Modifier.height(14.dp))
            DiscordCardPreview(status.preview, discord)
            status.lastMessage?.let { message ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (status.connected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        null,
                        Modifier.size(15.dp),
                        tint = if (status.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Badge, "Discord application")
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(7.dp))
                Text(
                    if (discord.usesOwnApplication) {
                        "Using your application · ${discord.applicationId}"
                    } else {
                        "Using the application Noctorium ships with · ${discord.resolvedApplicationId()}"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Discord shows this application's name above the card. Noctorium already has one, so nothing is needed " +
                    "here — set an id only to appear as a different application, or to upload your own named " +
                    "images under its Rich Presence assets.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                applicationId,
                { applicationId = it.filter(Char::isDigit).take(25) },
                label = { Text("Application ID (optional)") },
                placeholder = { Text("Leave empty to use Noctorium's own") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tracksTyping(),
            )
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    { state.updateDiscord { it.copy(applicationId = applicationId) } },
                    enabled = applicationId != discord.applicationId,
                ) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(7.dp)); Text("Save id") }
                OutlinedButton(state::testDiscordConnection) { Text("Test connection") }
                if (discord.usesOwnApplication) {
                    TextButton({ applicationId = ""; state.updateDiscord { it.copy(applicationId = "") } }) {
                        Text("Use Noctorium's")
                    }
                }
                TextButton({ state.openExternalUrl("https://discord.com/developers/applications") }) {
                    Text("Developer portal")
                }
            }
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.TextFields, "What the card says")
            Spacer(Modifier.height(7.dp))
            Text(
                "Each line is a template. Use {title}, {artist}, {album}, {provider}, {duration} and {position}; " +
                    "anything with no value disappears cleanly.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(13.dp))
            TemplateField("First line", discord.detailsTemplate) { value ->
                state.updateDiscord { it.copy(detailsTemplate = value) }
            }
            Spacer(Modifier.height(9.dp))
            TemplateField("Second line", discord.stateTemplate) { value ->
                state.updateDiscord { it.copy(stateTemplate = value) }
            }
            Spacer(Modifier.height(9.dp))
            TemplateField("Cover tooltip", discord.largeTextTemplate) { value ->
                state.updateDiscord { it.copy(largeTextTemplate = value) }
            }
            Spacer(Modifier.height(9.dp))
            TemplateField("Small icon tooltip", discord.smallTextTemplate) { value ->
                state.updateDiscord { it.copy(smallTextTemplate = value) }
            }
            Spacer(Modifier.height(16.dp))
            ChoiceRow(
                "Verb",
                PresenceActivityKind.entries,
                discord.activityKind,
                { it.displayName },
                { kind -> state.updateDiscord { it.copy(activityKind = kind) } },
            )
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Image, "Images and clock")
            Spacer(Modifier.height(12.dp))
            ChoiceRow(
                "Large image",
                PresenceArtwork.entries,
                discord.artwork,
                { it.displayName },
                { artwork -> state.updateDiscord { it.copy(artwork = artwork) } },
            )
            if (discord.artwork == PresenceArtwork.ASSET) {
                Spacer(Modifier.height(10.dp))
                TemplateField("Asset name", discord.artworkAssetKey) { value ->
                    state.updateDiscord { it.copy(artworkAssetKey = value) }
                }
            }
            Spacer(Modifier.height(10.dp))
            TemplateField("Small icon asset (optional)", discord.smallImageAssetKey) { value ->
                state.updateDiscord { it.copy(smallImageAssetKey = value) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Leave the small icon empty to use the service name — youtube_music, youtube or soundcloud — as the asset name.",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(16.dp))
            ChoiceRow(
                "Clock",
                PresenceTimestamps.entries,
                discord.timestamps,
                { it.displayName },
                { stamps -> state.updateDiscord { it.copy(timestamps = stamps) } },
            )
            Spacer(Modifier.height(6.dp))
            Text(discord.timestamps.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.SmartButton, "Buttons")
            Spacer(Modifier.height(7.dp))
            Text(
                "Discord allows two. A button needs both a label and a link, and {url} is the track's page.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(13.dp))
            PresenceButtonFields("First button", discord.firstButton) { button ->
                state.updateDiscord { it.copy(firstButton = button) }
            }
            Spacer(Modifier.height(14.dp))
            PresenceButtonFields("Second button", discord.secondButton) { button ->
                state.updateDiscord { it.copy(secondButton = button) }
            }
        }

        SettingsPanelCard {
            CardHeading(Icons.Default.Security, "While paused, and privacy")
            Spacer(Modifier.height(12.dp))
            ChoiceRow(
                "When playback pauses",
                PausedBehaviour.entries,
                discord.paused,
                { it.displayName },
                { behaviour -> state.updateDiscord { it.copy(paused = behaviour) } },
            )
            Spacer(Modifier.height(6.dp))
            Text(discord.paused.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            if (discord.paused == PausedBehaviour.SHOW_PAUSED) {
                Spacer(Modifier.height(10.dp))
                TemplateField("Paused suffix", discord.pausedSuffix) { value ->
                    state.updateDiscord { it.copy(pausedSuffix = value) }
                }
            }
            Spacer(Modifier.height(16.dp))
            ToggleRow(
                "Hide what I am listening to",
                "Replaces every line with one fixed message and drops the cover and buttons.",
                discord.hideTrackDetails,
            ) { hidden -> state.updateDiscord { it.copy(hideTrackDetails = hidden) } }
            if (discord.hideTrackDetails) {
                Spacer(Modifier.height(10.dp))
                TemplateField("Shown instead", discord.privateDetails) { value ->
                    state.updateDiscord { it.copy(privateDetails = value) }
                }
            }
        }
    }
}

/** Mirrors Discord's card so the templates can be judged without alt-tabbing. */
@Composable
private fun DiscordCardPreview(preview: DiscordPreview?, settings: DiscordPresenceSettings) {
    Surface(color = Color(0xFF1E1F22), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                settings.activityKind.displayName.uppercase(),
                color = ink(.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(ink(.08f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.MusicNote, null, Modifier.size(22.dp), tint = ink(.5f)) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        preview?.details?.takeIf { it.isNotBlank() } ?: "Nothing playing yet",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    preview?.state?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = ink(.75f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (settings.timestamps != PresenceTimestamps.NONE) {
                        Text(
                            if (settings.timestamps == PresenceTimestamps.ELAPSED) "0:42 elapsed" else "2:31 left",
                            color = ink(.55f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            preview?.buttons?.takeIf { it.isNotEmpty() }?.let { buttons ->
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    buttons.forEach { label ->
                        Surface(color = ink(.1f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                label,
                                color = ink(.9f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateField(label: String, value: String, change: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        text,
        {
            text = it.take(128)
            change(text)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().tracksTyping(),
    )
}

@Composable
private fun PresenceButtonFields(label: String, button: PresenceButton, change: (PresenceButton) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.weight(1f)) {
                TemplateField("Label", button.label) { change(button.copy(label = it)) }
            }
            Box(Modifier.weight(1.4f)) {
                TemplateField("Link", button.url) { change(button.copy(url = it)) }
            }
        }
    }
}

@Composable
private fun DiagnosticsPanel(settings: SettingsState, state: AppState) {
    Column {
        Button(state::runDiagnostics, enabled = !settings.diagnosticsRunning) {
            if (settings.diagnosticsRunning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.MonitorHeart, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text(if (settings.diagnosticsRunning) "Checking…" else "Run diagnostics")
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = chromePadding()) {
            items(settings.diagnostics, key = { it.name }) { result ->
                val color = when (result.level) { DiagnosticLevel.PASS -> MaterialTheme.colorScheme.primary; DiagnosticLevel.WARNING -> MaterialTheme.colorScheme.tertiary; DiagnosticLevel.FAIL -> MaterialTheme.colorScheme.error }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape = RoundedCornerShape(11.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (result.level == DiagnosticLevel.PASS) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null, tint = color)
                        Spacer(Modifier.width(11.dp)); Column { Text(result.name, fontWeight = FontWeight.SemiBold); Text(result.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsPanelCard(content: @Composable ColumnScope.() -> Unit) {
    // A group box on the window's face under a Windows skin, its heading set into its edge.
    if (skinned()) {
        SettingsGroup(content)
        return
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 720.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), content = content)
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: () -> Unit,
    active: Boolean = false,
) {
    // An item in a Control Panel under a Windows skin.
    if (skinned()) {
        ControlPanelItem(title, subtitle, icon, action, active)
        return
    }
    Surface(onClick = action, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            if (active) { Icon(Icons.Default.CheckCircle, "Active", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(9.dp)) }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun EmptyScreen(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingSection() {
    Column(Modifier.padding(vertical = 18.dp)) {
        Box(Modifier.width(190.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            repeat(5) { Box(Modifier.size(140.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f))) }
        }
    }
}

/**
 * Whether a playlist on this service can hold the given track: services do not take each other's music.
 *
 * A Spotify track is refused everywhere, in both directions. Its id means nothing to YouTube, and Noctorium
 * never writes to Spotify at all — so neither service could be asked to store it. Bandcamp and VK tracks are
 * the same: no other service can hold them. A Noctorium playlist takes all of them happily, which is where a
 * mixed collection belongs.
 */
internal fun ProviderType.acceptsTrack(track: Track): Boolean = when {
    track.provider == ProviderType.SPOTIFY || track.provider == ProviderType.BANDCAMP || track.provider == ProviderType.VK -> false
    this == ProviderType.SOUNDCLOUD -> track.provider == ProviderType.SOUNDCLOUD
    this == ProviderType.YOUTUBE_MUSIC || this == ProviderType.YOUTUBE_VIDEO ->
        track.provider != ProviderType.SOUNDCLOUD
    else -> false
}

/** Hours as something readable: a fresh account should not be told it has listened for "0.0" hours. */
private fun formatHours(hours: Double): String = when {
    hours <= 0.0 -> "no time yet"
    hours < 1.0 -> "${(hours * 60).roundToInt()} min"
    hours < 10.0 -> "${String.format(java.util.Locale.US, "%.1f", hours)} h"
    else -> "${hours.roundToInt()} h"
}

/**
 * Signing in to the Noctorium account, and what it has counted.
 *
 * The same account as the website. Only the address and a session token are held here; the password is
 * sent once and never written down.
 */
@Composable
private fun NoctoriumAccountPanel(state: AppState) {
    val account by state.account.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Column(Modifier.scrollingPage().padding(bottom = chromeBottom())) {
        if (account.signedIn) {
            val user = account.user!!
            SettingsPanelCard {
                Text(user.displayName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListeningStat("${account.stats.streams}", "Streamed", Modifier.weight(1f))
                    ListeningStat("${account.stats.uniqueTracks}", "Different", Modifier.weight(1f))
                    ListeningStat(formatHours(account.stats.hours), "Listened", Modifier.weight(1f))
                    ListeningStat("${account.stats.artists}", "Artists", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(state::refreshListeningStats) { Text("Refresh") }
                    OutlinedButton(state::signOutOfNoctorium) { Text("Sign out") }
                }
                Spacer(Modifier.height(12.dp))
                AccountFact("A song counts once you have heard 30 seconds of it, or half of it.")
                AccountFact("The same figures are on the website, under your account.")
            }
            Spacer(Modifier.height(14.dp))
            SettingsPanelCard { ConnectSettingsRows(state) }
        } else {
            SettingsPanelCard {
                Text(
                    if (creating) "Create a Noctorium account" else "Sign in to Noctorium",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Counts how many songs you stream, how many different ones, and for how long.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(14.dp))
                if (creating) {
                    OutlinedTextField(
                        displayName,
                        { displayName = it },
                        label = { Text("Display name") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().tracksTyping(),
                    )
                    Spacer(Modifier.height(9.dp))
                }
                OutlinedTextField(
                    email,
                    { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    // The password is never shown, and never leaves this field except in the one request.
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = if (creating) {
                        { Text("At least 10 characters.") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        {
                            if (creating) state.signUpToNoctorium(email, password, displayName)
                            else state.logInToNoctorium(email, password)
                            password = ""
                        },
                        enabled = !account.busy && email.isNotBlank() && password.isNotBlank(),
                    ) {
                        if (account.busy) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (creating) "Create account" else "Sign in")
                        }
                    }
                    TextButton({ creating = !creating; password = "" }) {
                        Text(if (creating) "I already have one" else "Create an account")
                    }
                }
            }
        }

        account.message?.let { message ->
            Spacer(Modifier.height(10.dp))
            SettingsPanelCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), fontSize = 12.sp)
                    IconButton(state::clearNoctoriumMessage, Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, "Dismiss", Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

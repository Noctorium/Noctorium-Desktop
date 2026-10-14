package app.noctorium.ui

import androidx.compose.ui.graphics.vector.ImageVector
import app.noctorium.core.Destination
import app.noctorium.settings.SettingEntry
import app.noctorium.settings.SettingsSearch

/**
 * One setting as the settings search knows it: the [entry] it is found by, the [page] it is on, and the label it
 * is drawn with there -- its [anchor] -- which is what the page scrolls to and lights up. A null anchor is a setting
 * that is the page itself, or a part of it drawn some other way: choosing it opens the page at the top.
 */
internal data class IndexedSetting(
    val page: SettingsPage,
    val anchor: String?,
    val entry: SettingEntry,
    /** When the setting is only on its page some of the time -- "once you are signed in to YouTube Music" -- when. */
    val onlyWhen: String? = null,
    /**
     * A place of its own outside Settings that this is found on and opens -- the Connect page, which is where
     * somebody searching Settings for "devices" or "firewall" wants to be. [page] is then where its settings are.
     */
    val destination: Destination? = null,
) {
    /** Where it is, as the search shows it: "You › Noctorium account", or the page outside Settings, "Connect". */
    val where: String get() = destination?.sidebarLabel() ?: page.path

    /** Its picture in the search: its page's, or the sidebar's for a page outside Settings. */
    val icon: ImageVector get() = destination?.sidebarIcon() ?: page.icon()

    /** Opens it: its page in Settings, at the setting, with [open]; or the page outside Settings, with [go]. */
    fun openWith(open: (SettingsPage, String?) -> Unit, go: (Destination) -> Unit) {
        if (destination != null) go(destination) else open(page, anchor)
    }
}

/**
 * Every setting in the desktop's Settings, in one place, for the search box at the top of them.
 *
 * Each has its name as the page shows it, what it does in a line, and the other words people use for it: what
 * they call it when they do not know its name ("dark mode" for Theme, "louder" for the volume booster, "hotkeys"
 * for the shortcuts) and the American spellings of the British ones ("color", "equalizer"). Listed page by page
 * in the order the pages draw them, which is the order the search falls back on.
 *
 * Adding a setting to a page with [ToggleRow], [ChoiceRow], [ToggleChips] or [CardHeading] adds an anchor, and
 * `SettingsIndexTest` fails until it has an entry here too -- so the search cannot quietly fall behind the pages.
 */
internal object SettingsIndex {
    val settings: List<IndexedSetting> by lazy { build() }

    /** The entries alone, as [SettingsSearch] takes them. */
    val entries: List<SettingEntry> by lazy { settings.map { it.entry } }

    private val byId: Map<String, IndexedSetting> by lazy { settings.associateBy { it.entry.id } }

    /** The settings that match [query], best first. */
    fun search(query: String): List<FoundSetting> =
        SettingsSearch.search(query, entries).map { FoundSetting(byId.getValue(it.entry.id), it) }

    private fun build(): List<IndexedSetting> = buildList {
        // The Connect page is not in Settings, but Settings is where somebody looks for "play on my phone" or "why
        // does it not see my laptop": these open the page itself. First, so "devices" finds the devices before the
        // account's settings for them.
        outside(Destination.CONNECT, settingsOn = SettingsPage.ACCOUNT) {
            setting(
                "Connect", "Where the music is playing, every one of your devices on this network, and moving the music between them.",
                "noctorium connect", "cast", "remote", "remote control", "play on another device", "multiroom",
                "spotify connect", "listening on", "playing on",
            )
            setting(
                "Devices on this network", "Every device of yours with Noctorium open, what each is playing, and whether it is a computer, a phone or a terminal.",
                "my devices", "other devices", "phone", "laptop", "other computer", "speakers", "terminal", "idle",
            )
            setting(
                "Play here, or play on another device", "Moves the music from wherever it is to any of your devices, at the same second, with the whole queue.",
                "play here", "play on", "play on my phone", "play on my computer", "move the music", "send to phone",
                "send to computer", "hand over", "handover", "transfer", "bring it back", "take it back", "switch device",
            )
            setting(
                "Who is controlling whom", "Which device is the remote control of which: this computer driving the phone, or the phone driving it.",
                "controlled from", "controlling", "driving", "remote control", "being controlled",
            )
            setting(
                "Add a device by its address", "Finds one of your devices by its address and port, on a network that keeps devices from hearing each other.",
                "ip address", "address", "port", "manual", "add device", "guest network", "virtual machine", "adb",
            )
            setting(
                "Not finding a device?", "The same account, the same Wi-Fi, the firewall on Windows, and a phone allowed to run in the background.",
                "firewall", "windows firewall", "public network", "private network", "wifi", "wi-fi", "same account",
                "not showing", "missing device", "cannot find", "can't find", "not found", "troubleshoot", "background",
                "battery", "help",
            )
        }
        page(SettingsPage.ACCOUNT) {
            setting(
                "Noctorium account", "Sign in to count what you stream, how many different songs and for how long. The same account as the website.",
                "sign in", "log in", "login", "sign up", "register", "create account", "stats", "statistics", "listening time",
                "email", "password", "sign out", "log out",
                anchor = null,
            )
            setting(
                "Noctorium Connect", "What this computer is called on your other devices, whether they can find it, and the way to the Connect page.",
                "devices", "cast", "remote", "play on another device", "other computer", "phone", "multiroom", "network",
                "connect settings",
                onlyWhen = "once you are signed in",
            )
            setting(
                "Name this device", "What your other devices call this computer.", "device name", "rename", "computer name",
                anchor = "Noctorium Connect", onlyWhen = "once you are signed in",
            )
            setting(
                "Noctorium Connect on or off", "Whether this computer finds your other devices and shows up on theirs. Off stops Connect altogether.",
                "discoverable", "visible", "hide this device", "turn off connect", "disable connect", "enable connect", "connect switch",
                anchor = "Noctorium Connect", onlyWhen = "once you are signed in",
            )
        }
        page(SettingsPage.PROFILE) {
            setting(
                "Profile name", "The name this computer's Noctorium goes by. It stays on this computer.",
                "display name", "username", "user name", "nickname", "my name", "local profile", anchor = null,
            )
            setting(
                "History", "Every song you have listened to on this computer, by day, to play again. It stays on this computer.",
                "recently played", "listening history", "play history", "played songs", "song history", "what did i listen to",
                "what was that song", "last played", "previously played", "listened to",
            )
            setting(
                "Keep a history of what I play", "A song goes in once you have heard half a minute of it. Off adds nothing more.",
                "history", "pause history", "stop history", "turn off history", "private", "privacy", "incognito",
            )
            setting(
                "Clear history", "Forgets every song in the history on this computer, after asking.",
                "delete history", "erase history", "forget", "remove history", "wipe",
            )
        }

        page(SettingsPage.THEME) {
            setting(
                "Theme", "Noctorium's own palettes, your editor's and terminal's favourites, and Windows 98 and XP.",
                "dark mode", "light mode", "night mode", "dark theme", "light theme", "colour scheme", "color scheme", "colours",
                "colors", "palette", "skin", "appearance", "look", "catppuccin", "dracula", "nord", "gruvbox", "tokyo night",
                "windows 98", "windows xp", "retro", "classic",
            )
            setting(
                "Your own colours", "Six colours of your own, for the Custom theme: the page, the panels, the writing and the accent.",
                "custom theme", "custom colors", "hex", "make my own theme", anchor = "Light theme",
            )
            setting("Colour", "The accent, the animations, the surfaces and the corners.", "color", "finish", "style")
            setting(
                "Accent", "The colour buttons, sliders and highlights are drawn in: the theme's own, a named one, your own, " +
                    "or the cover of whatever is playing.",
                "accent colour", "accent color", "highlight colour", "highlight color", "tint", "match the artwork",
                "album art colours", "cover colours", "colour picker", "color picker",
            )
            setting(
                "Animations", "Screens easing in, pages sliding, a new song's name rising into place. Off makes every change instant.",
                "motion", "reduce motion", "transitions", "effects", "animation", "instant",
            )
            setting(
                "Surfaces", "Solid panels, or liquid glass with the page bending through it.",
                "glass", "liquid glass", "blur", "transparency", "translucent", "acrylic", "frosted",
            )
            setting("Corners", "How round the corners of cards, buttons and covers are.", "rounded corners", "square", "roundness", "radius")
            setting(
                "Icons", "How every button, menu and page draws its icons: Standard, Outlined, Rounded, Sharp, Two-tone or Line.",
                "icon set", "icon theme", "icon style", "glyphs", "symbols", "outlined", "rounded", "sharp", "two-tone",
                "line icons", "thin icons", "monochrome", "lucide", "breeze",
            )
            // YTM is in the description rather than the other words, so that it finds this after the YouTube Music
            // account, which is what somebody typing it is more likely to be after.
            setting(
                SERVICE_BADGE_STYLE, "Each service's own mark on the badges that say where a song comes from, or the letters, YTM, SC or SP, as before.",
                "logo", "logos", "service logos", "service icons", "brand icons", "provider icons", "badge", "badges",
                "marks", "letters", "abbreviations", "youtube logo", "spotify logo", "soundcloud logo", "simple icons",
            )
        }
        page(SettingsPage.TEXT) {
            setting("Text", "How big everything is written, and in what typeface.", "type", "writing", "font")
            setting(
                "Text size", "Everything written, bigger or smaller at once, on top of the computer's own text size.",
                "font size", "bigger text", "larger text", "smaller text", "zoom", "scale", "accessibility", "readability",
            )
            setting("Typeface", "The typeface everything is written in.", "font", "font family", "serif", "monospace", "typography")
        }
        page(SettingsPage.PLAYER_BAR) {
            setting("Player bar", "The bar with the controls on it: its layout, where it sits and its buttons.", "controls", "now playing bar", "toolbar", "bottom bar")
            setting(
                "Player bar layout", "Inline, Stacked, Centred, Slim, Spotlight, Floating, the Island, Display, Taskbar, Toolbar, Toolbar 2 and more.",
                "player style", "bar style", "controls layout", "toolbar", "island", "floating", "taskbar", "compact",
                "classic player", "audacious", "winamp", "flat buttons", "stop button", "toolbar 2", "more buttons",
            )
            setting("Show the clock", "The time in the Taskbar layout's tray.", "clock", "time", "tray clock")
            setting(
                "Player bar position", "Along the foot of the window, or the top.", "top", "bottom", "move the player bar", "bar at the top",
            )
            setting(
                "Buttons on the player bar", "Which of shuffle, repeat, like, lyrics, the queue, the sleep timer, volume, speed, devices and stop it carries.",
                "hide buttons", "show buttons", "player buttons", "stop button", "shuffle button", "repeat button", "like button",
            )
            setting("Seek bar", "The bar a song is sought along, in the player bar and on now playing.", "progress bar", "scrubber", "timeline", "position")
            setting(
                "Seek bar style", "Material, Minimal, Wave, Segments, Capsule, Classic, Bars, Beads, Neon, Ruler, Slider and more.",
                "progress bar style", "scrubber", "slider", "round handle", "wave", "waveform", "timeline style",
            )
            setting(
                "Right-hand figure", "The time on the right of the seek bar: the song's length, or the time left.",
                "time remaining", "time left", "duration", "countdown", "elapsed",
            )
        }
        page(SettingsPage.NOW_PLAYING) {
            setting("Now playing", "The screen the player bar opens: its layout, the cover, the backdrop and the panel.", "full screen player", "player screen")
            setting(
                "Layout", "How the now playing screen is arranged: side by side, stage, focus, immersive, split, turntable, poster, sing along, playlist.",
                "now playing layout", "arrangement", "turntable", "vinyl", "poster", "immersive", "sing along", "karaoke",
                "playlist view", "table view", "desktop player", "audacious", "foobar",
            )
            setting("Cover", "How the cover is drawn: square, rounded, as a record and more.", "album art", "artwork", "cover art", "record", "vinyl")
            setting("Cover size", "How big the cover is drawn.", "album art size", "artwork size", "big cover")
            setting(
                "Backdrop", "What is behind the now playing screen: a wash of the cover's colours, the cover itself, or plain.",
                "background", "wallpaper", "blur", "ambient", "wash",
            )
            setting("Panel width", "How wide the panel beside the cover is, for the layouts that have one.", "sidebar width", "queue panel", "lyrics panel")
            setting("Open the panel on", "Whether the panel opens on Up next or on the lyrics.", "up next", "lyrics tab", "default tab")
            setting(
                "The line being sung, under the cover", "The line of the lyrics being sung, under the cover; off shows the album instead.",
                "lyric line", "current lyric", "album name",
            )
            setting("Follow button under the artist", "For following the artist on the service the song came from.", "follow artist", "subscribe")
            setting("Playlist layout", "The Playlist layout's visualiser, the columns of its table and its status bar.", "playlist view", "table view")
            setting(
                "Visualiser", "Bars or a level meter drawn from the sound itself, in the Playlist layout's info bar.",
                "visualizer", "spectrum", "analyser", "analyzer", "audio bars", "vu meter", "level meter", "sound bars",
            )
            setting(
                "Columns", "Which columns the Playlist layout's table has, and in what order.",
                "queue columns", "table columns", "playlist columns", "track number", "duration", "album", "service", "column order",
            )
            setting(
                "Status bar", "Along the foot of the Playlist layout: the stream's format, and how far into the whole queue.",
                "codec", "bitrate", "sample rate", "kbps", "khz", "stream info", "audio format", "queue length", "total time",
            )
            setting("Lyrics", "How the words are set on the now playing screen.", "lyrics look", "words", "karaoke")
            setting("Size", "How big the lyrics are written.", "lyrics size", "lyric font size", "bigger lyrics")
            setting("Alignment", "Lyrics to the left, centred, or as each layout lays them out.", "lyrics alignment", "centre", "center", "left")
            setting(
                "Dim the lines not being sung", "Off keeps every line at full strength, with the one being sung still picked out.",
                "fade lyrics", "lyrics contrast", "highlight current line",
            )
        }
        page(SettingsPage.BROWSING) {
            setting("Browsing", "Cards, badges, the buttons on them, what Home shows and what the sidebar offers.", "browse", "pages")
            setting("Card size", "How big the covers on Home and in the library are.", "tile size", "thumbnail size", "grid size", "covers")
            setting(
                "Service badges", "Whether a cover carries the mark of the service it comes from.",
                "badges", "icons on covers", "youtube icon", "spotify icon", "logos",
            )
            setting("Play and menu buttons", "Whether a card's buttons are always there or only when pointed at.", "hover buttons", "card buttons")
            setting(
                "Parts of Home", "Which rows Home shows: the greeting, pinned songs, played lately and each service's shelves.",
                "home page", "home screen", "hide home rows", "shelves", "greeting", "recently played", "pinned", anchor = "On Home",
            )
            setting(
                "Hybrid search asks", "Which services a search of all of them asks.", "search all", "all services", "search services", "!all",
            )
            setting(
                "Pages in the sidebar", "Which pages the sidebar offers. Home and Settings always stay.",
                "sidebar", "navigation", "menu", "hide pages", "rail", anchor = "In the sidebar",
            )
        }

        page(SettingsPage.SOUND) {
            setting(
                "Equaliser", "Ten bands from the deepest bass to the air above the cymbals, and presets to start from.",
                "equalizer", "eq", "bass", "treble", "bass boost", "tone", "frequencies", "sound", "audio",
            )
            setting("Preset", "Flat, Rock, Pop, Jazz, Classical, Bass booster and more, or your own curve.", "equaliser preset", "equalizer preset", "eq preset", "rock", "jazz")
            setting("Preamp", "Decibels added before the bands, to keep a boosted curve from clipping.", "pre-amp", "gain", "headroom", "clipping")
            setting(
                "Volume booster", "Louder than full volume, up to 1000%, behind a limiter so it gets loud, not broken.",
                "louder", "boost", "volume boost", "amplify", "amplifier", "gain", "quiet songs", "too quiet", "1000%", "loudness",
            )
        }
        page(SettingsPage.PLAYBACK_QUEUE) {
            setting("Speed", "Faster or slower without changing the pitch, from half to double.", "playback speed", "tempo", "faster", "slower", "podcast", "rate")
            setting("Autoplay", "When the queue runs out, songs like the last one carry on.", "radio", "endless", "keep playing", "continue", "infinite play", "recommendations")
            setting(
                "Playing a song from Home or a search", "A song picked on Home or from a search starts a radio from it, or plays the rest of its shelf or results after it.",
                "home autoplay", "search autoplay", "start radio", "shelf", "home tab", "search results", "what plays next",
            )
            setting(
                "Where its songs come from", "Whether autoplay's songs come from the same service as the last song, or any.",
                "autoplay source", "radio source", "recommendations from",
            )
            setting(
                "Skip songs played lately", "Autoplay leaves out what you played recently, so the same few do not keep coming round.",
                "no repeats", "avoid repeats", "recently played",
            )
            setting("The queue", "What happens to the queue between launches.", "queue", "up next", "playlist")
            setting(
                "Keep the queue between launches", "Opens with the queue as you left it, and picks the song up where it was.",
                "remember queue", "restore queue", "resume", "save queue", "restore session",
            )
            setting("Sleep timer", "How the sleep timer ends the music.", "sleep", "timer", "bedtime", "stop playing after")
            setting("Fade out", "The music grows quieter over the sleep timer's last moments instead of stopping dead.", "fade", "sleep fade", "crossfade")
            setting("Skipping", "Leaving out the parts of a YouTube video that are not the music.", "skip")
            setting(
                "Skip the parts of a YouTube video that are not the music", "Intros, outros, sponsor reads and talking, as SponsorBlock's contributors mark them.",
                "sponsorblock", "sponsor block", "skip sponsors", "skip intro", "skip outro", "adverts", "ads",
            )
        }
        page(SettingsPage.LYRICS) {
            setting(
                "Open lyrics on", "Which source the lyrics open on. Every source is still asked.",
                "lyrics provider", "lyrics source", "lrclib", "genius", "musixmatch", "synced lyrics", "lyrics",
            )
        }

        page(SettingsPage.YOUTUBE) {
            setting(
                "YouTube Music account", "Sign in to YouTube Music for your likes, playlists and history, from this computer or your phone.",
                "youtube", "yt music", "ytm", "sign in", "google account", "cookies", "brand channel", "channel", anchor = null,
            )
            setting(
                "Add what I play to my YouTube Music history", "Songs you play show up in your history there, and its recommendations learn from them.",
                "history", "watch history", "listening history", "recommendations",
                onlyWhen = "once you are signed in to YouTube Music",
            )
        }
        page(SettingsPage.SOUNDCLOUD) {
            setting(
                "SoundCloud account", "Sign in to SoundCloud for your likes and playlists, or use a browser session's cookies.",
                "soundcloud", "sc", "sign in", "cookies", "cookies.txt", "browser session", "firefox", anchor = null,
            )
        }
        page(SettingsPage.SPOTIFY) {
            setting(
                "Spotify", "Your Spotify playlists, Liked Songs and search; with Premium, playing on Spotify itself.",
                "spotify", "sp", "sign in", "connect spotify", "premium", "client id", "spotify app", anchor = null,
            )
            setting(
                "Play Spotify songs on Spotify", "With Premium, Spotify's songs play in your Spotify app, on the device you choose.",
                "spotify connect", "play on spotify", "spotify device", "premium playback",
                onlyWhen = "once Spotify is connected",
            )
        }
        page(SettingsPage.BANDCAMP) {
            setting(
                "Bandcamp", "Albums, artists and songs from Bandcamp, with nothing to sign in to; your collection with your name.",
                "bandcamp", "bc", "collection", "wishlist", "bandcamp name", anchor = null,
            )
            setting("Genres on Home", "Each genre picked adds a row of its best-sellers to Home.", "bandcamp genres", "genres", "best sellers", anchor = null)
        }
        page(SettingsPage.VK) {
            setting(
                "VK Music", "Your music and playlists from VK, through your vk.ru session.", "vk", "vkontakte", "vk.com", "vk.ru", "sign in", "cookies",
                anchor = null,
            )
        }
        page(SettingsPage.SCROBBLING) {
            setting(
                "Scrobbling", "Tell Last.fm or ListenBrainz what you listen to.", "last.fm", "lastfm", "listenbrainz", "scrobble", "listening history",
                "statistics", anchor = null,
            )
            setting(
                "Last.fm", "Connect Last.fm, with Noctorium's application keys or your own.", "lastfm", "api key", "shared secret", anchor = null,
            )
            setting("ListenBrainz", "Connect ListenBrainz with your user token.", "listen brainz", "token", anchor = null)
        }
        page(SettingsPage.DISCORD) {
            setting(
                "Show listening activity", "Puts the song playing on your Discord profile, over the Discord app on this computer.",
                "discord", "rich presence", "status", "activity", "now playing on discord",
            )
            setting("Discord application", "Noctorium's own Discord application, or yours with images of your own.", "application id", "client id", "developer portal")
            setting("What the card says", "The lines, the tooltips and the verb on your Discord card.", "card text", "template", "first line", "second line", "listening to")
            setting("Verb", "What the card says you are doing: listening, playing or watching.", "listening", "playing", "activity type")
            setting("Images and clock", "The cover and the small icon on the card, and whether it shows the time.", "cover", "artwork", "large image", "small icon")
            setting("Large image", "What the card's big picture is: the song's cover, or your own image.", "cover", "artwork", "album art")
            setting("Clock", "Whether the card shows time elapsed, time left or nothing.", "timestamps", "elapsed", "time left", "progress")
            setting("Buttons", "Up to two buttons under the card, with links.", "links", "listen along", "button")
            setting("While paused, and privacy", "What the card says while the music is paused, and keeping it private.", "privacy", "paused")
            setting("When playback pauses", "Clear the card, keep it, or mark it paused.", "pause", "paused", "idle")
            setting("Hide what I am listening to", "Replaces every line with one fixed message and drops the cover and buttons.", "private", "privacy", "incognito", "hide song")
        }

        page(SettingsPage.STARTUP) {
            setting("Closing the window", "What the close button does.", "close", "exit", "quit")
            setting(
                "When you close Noctorium", "Quit, or keep playing in the tray.", "close to tray", "minimise to tray", "minimize to tray",
                "system tray", "background", "keep playing", "menu bar",
            )
            setting("Announcing tracks", "A note from the tray when a song starts.", "notifications", "notify")
            setting(
                "Say what is playing when a song starts", "A small note from the tray with the title and the artist.",
                "notification", "toast", "now playing notification", "song change", "popup",
            )
            setting("Start with ${computerName()}", "Whether Noctorium starts when you sign in to the computer.", "startup", "autostart", "launch at login", "start with the computer")
            setting(
                "When you sign in", "Start Noctorium with the computer, in its window or in the tray.",
                "start with windows", "launch at login", "autostart", "run at startup", "open at login",
                onlyWhen = "in an installed Noctorium",
            )
            setting("Opening", "The page Noctorium opens on.", "start page", "first page")
            setting("Open Noctorium on", "Which page Noctorium opens on: Home, Search, the library and more.", "start page", "home page", "default page", "launch page")
        }
        page(SettingsPage.SHORTCUTS) {
            setting(
                "In Noctorium", "Every key that does something while Noctorium is the window in front, each one changeable.",
                "keyboard shortcuts", "hotkeys", "hot keys", "keyboard", "keybindings", "key bindings", "keys", "shortcuts", "rebind",
            )
            setting(
                "Global shortcuts", "Keys that work from any program, even with Noctorium in the tray. Off until switched on.",
                "global hotkeys", "system wide", "anywhere", "media keys", "hotkeys", "keyboard",
            )
            setting(
                "Use global shortcuts", "Switches the global shortcuts on, through the system's own hot keys.",
                "global hotkeys", "enable global shortcuts", "system wide shortcuts", "background shortcuts",
            )
            setting(
                "Quick search", "A search bar over everything, on Ctrl+F, that understands !ytm, !sc, !sp, !bc, !vk and !all.",
                "ctrl+f", "cmd+f", "find", "spotlight", "launcher", "bangs", "search bar", "quick find",
            )
            // Every shortcut is a setting of its own, found by what it does: "stop" finds Stop's keys.
            ShortcutAction.entries.filter { action -> out.none { it.page == SettingsPage.SHORTCUTS && it.entry.title == action.title } }.forEach { action ->
                setting(
                    action.title,
                    "A keyboard shortcut" + (if (action.global) ", in Noctorium or from anywhere." else ", in Noctorium."),
                    "shortcut", "hotkey", "key", "keyboard", "keybinding",
                )
            }
        }
        page(SettingsPage.DOWNLOADS) {
            setting(
                "Saving music", "Where saved music goes: a folder of your choosing, or your desktop, as MP3 tagged with the cover.",
                "downloads", "download folder", "export", "save as file", "mp3", "save location", "folder", anchor = null,
            )
        }
        page(SettingsPage.PLAYBACK_TOOLS) {
            setting(
                "Playback tools", "yt-dlp and mpv, the two programs Noctorium plays with: whether they are here, and installing them.",
                "yt-dlp", "youtube-dl", "mpv", "player", "install", "missing", "codecs", anchor = null,
            )
        }
        page(SettingsPage.UPDATES) {
            setting("Updates", "Whether a newer Noctorium is out, and installing it.", "update", "upgrade", "new version", "check for updates", "release", anchor = null)
            setting("Look for updates when Noctorium starts", "One request to GitHub at start; nothing is downloaded without asking.", "auto update", "check for updates", "automatic updates")
        }
        page(SettingsPage.DIAGNOSTICS) {
            setting("Diagnostics", "Checks that yt-dlp, mpv and storage are working, for when something is not.", "troubleshoot", "problems", "debug", "logs", "help", anchor = null)
        }
    }

    private fun MutableList<IndexedSetting>.page(page: SettingsPage, entries: PageEntries.() -> Unit) {
        PageEntries(page, this).entries()
    }

    /** What is found on [destination], a page outside Settings, whose own settings are on [settingsOn]. */
    private fun MutableList<IndexedSetting>.outside(destination: Destination, settingsOn: SettingsPage, entries: PageEntries.() -> Unit) {
        PageEntries(settingsOn, this, destination).entries()
    }

    private class PageEntries(private val page: SettingsPage, val out: MutableList<IndexedSetting>, private val destination: Destination? = null) {
        /** A setting called [title], drawn on the page under [anchor] -- its own name unless said otherwise. */
        fun setting(title: String, description: String, vararg keywords: String, anchor: String? = title, onlyWhen: String? = null) {
            out += IndexedSetting(
                page,
                // A page outside Settings opens at its top: it has no settings of its own to light up.
                anchor.takeIf { destination == null },
                SettingEntry(
                    id = "${destination?.name ?: page.name}:$title",
                    title = title,
                    section = destination?.sidebarLabel() ?: page.path,
                    description = description,
                    keywords = keywords.toList(),
                ),
                onlyWhen,
                destination,
            )
        }
    }
}

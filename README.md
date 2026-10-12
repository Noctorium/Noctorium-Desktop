# Noctorium for the desktop

The Windows, macOS and Linux application. Compose Desktop for the interface, mpv for audio, yt-dlp for reading the
services, an embedded Chromium for the SoundCloud sign-in window. Everything that is the same on the phone
— the library, the queue, settings, scrobbling, lyrics, the account, Connect — comes from
[Noctorium-Base](https://github.com/Noctorium/Noctorium-Base), which this repository includes as `core`.

> Noctorium is an independent third-party client. It is not affiliated with Google, YouTube, SoundCloud,
> Spotify, Last.fm, ListenBrainz or Discord.

## Getting the code

```bash
git clone --recursive https://github.com/Noctorium/Noctorium-Desktop.git
```

The `base/` submodule is Noctorium-Base. If you also have Noctorium-Base checked out beside this
repository as `../Noctorium-Base`, that checkout is used instead and edits there are seen by the next
build here — see `settings.gradle.kts` for the lookup order.

## Running

```bash
./gradlew run
```

JDK 21. On Windows and the Mac the first build fetches mpv and yt-dlp and lays them beside the application,
so nothing has to be installed first; on Linux both come from the package manager (`mpv`, `yt-dlp`).
Settings, credentials and downloads live in `%LOCALAPPDATA%\Noctorium` on Windows,
`~/Library/Application Support/Noctorium` on a Mac (sign-ins in the Keychain) and `~/.local/share/noctorium`
on Linux, and a folder left by an earlier name of the application is adopted on first run.

## Keyboard shortcuts

Every shortcut can be changed in Settings, under Keyboard shortcuts; `?` lists them, and Ctrl+F (⌘F on a Mac)
brings up the quick search bar, which takes `!ytm`, `!sc`, `!sp`, `!bc`, `!vk` and `!all` at the start or the end.
Global shortcuts — from any program — are off until switched on there, and are registered with the system's own
hot-key service, never a keyboard hook: `RegisterHotKey` on Windows, Carbon's `RegisterEventHotKey` on a Mac,
`XGrabKey` under X11, and on Wayland the desktop portal's GlobalShortcuts, where the desktop offers it (KDE Plasma,
GNOME 48 and newer); elsewhere the page says they are not supported. A chord another program holds is shown as
taken.

## Testing

```bash
./gradlew test
```

Two groups of tests are off unless asked for, because they need the network and the real programs:

```bash
./gradlew test --tests "*RealPlaybackTest*" -Dnoctorium.installTools=true
./gradlew test -Dnoctorium.writeIcons=true      # regenerates the committed icon files from AppIcon
```

## Packaging

```bash
./gradlew packageMsi packageExe   # Windows, on Windows
./gradlew packageDeb packageRpm   # Debian and Fedora, on Linux
./gradlew macDmg                  # the disk image for this Mac's chip, on a Mac
```

jpackage cannot cross-build, which is why releases are made by
[Noctorium-Installer](https://github.com/Noctorium/Noctorium-Installer) with a runner per platform. That is
also where the installers are published and what the in-app updater watches.

## Icons

The icon sets offered in Settings › Customization are Compose's Material icons — Standard (Material's filled
set, as Noctorium has always drawn), Outlined, Rounded, Sharp and Two-tone — under the Apache License 2.0, and
**Line**, drawn from [Lucide](https://lucide.dev)'s shapes (lucide-react 1.50.0) under the ISC licence, with the
icons Lucide took from [Feather](https://feathericons.com) under the MIT licence. Both notices are reproduced in
`src/main/kotlin/app/noctorium/ui/icons/LineIcons.kt`, where the shapes are kept.

The badges that say which service a song comes from carry each service's mark, from
[Simple Icons](https://simpleicons.org) (simple-icons 16.35.0, CC0 1.0), unless they are set back to letters
beside the icon set. The marks are trademarks of the services they stand for, drawn only to say which service a
song is from; music on the computer has Material Icons' folder (Apache License 2.0). The shapes are kept in
Noctorium-Base, `core/src/main/kotlin/app/noctorium/domain/ServiceMarks.kt`, which the phone draws them from too.

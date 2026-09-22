# Noctorium for the desktop

The Windows and Linux application. Compose Desktop for the interface, mpv for audio, yt-dlp for reading the
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

JDK 21. On Windows the first build fetches mpv and yt-dlp and lays them beside the application, so nothing
has to be installed first; on Linux both come from the package manager (`mpv`, `yt-dlp`). Settings,
credentials and downloads live in `%LOCALAPPDATA%\Noctorium` on Windows and `~/.local/share/noctorium` on
Linux, and a folder left by an earlier name of the application is adopted on first run.

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
```

jpackage cannot cross-build, which is why releases are made by
[Noctorium-Installer](https://github.com/Noctorium/Noctorium-Installer) with a runner per platform. That is
also where the installers are published and what the in-app updater watches.

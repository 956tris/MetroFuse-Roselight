# Roselight Music for MetroFuse

Roselight Music is a native MetroFuse interface based on the mobile and web layouts of Apple Music. It uses its own name and music-note mark. The plugin runs on MetroFuse's existing player and sources. Account sign-in and catalog access come from whichever provider plugin is active.

Android uses a scrolling feed, a floating tab dock that condenses as you scroll, a separate Search control, and a glass mini-player. The full-screen player uses the current cover as a blurred backdrop. Desktop uses web-style sidebar navigation, a fixed top playback bar, a central artwork and scrubber display, and horizontally scrolling featured shelves. Both follow the system light or dark theme. Library Artists and Albums group tracks from the current library page. New and Radio depend on the selected source's shelves.

The glass surfaces use Compose gradients, shadows, translucent borders and artwork blur. They approximate the iOS material. Apple artwork, fonts, logos and frameworks are not bundled. MetroFuse controls protected host routes such as Settings and Plugins. Sync, lyrics, catalog and radio availability depend on the host and installed providers.

## Install

Use [MetroFuse KMP Beta](https://github.com/956tris/MetroFuse/releases) with API 1.1.0 and Compose ABI `kotlin-2.4.21_compose-1.13.0-beta02_api-1.1.0`. Source selection requires the host `ui.frontends/v1` service. UI SDK preview 003 can render this interface but cannot switch sources from Roselight.

- On Android, import `roselight.apk` from MetroFuse **Settings → Plugins → Import or update plugin**. **Do not install this plugin APK with Android's package installer.** Install the separate MetroFuse application APK, then import the plugin inside that app.
- On desktop, import `roselight.jar` through the same Plugins screen.
- Enable Roselight Music and choose it under **App interface**. The host's restore icon at the top right returns to the built-in interface.

## Migration and beta limitations

Back up your library, playlists and settings before updating the host or importing new plugins. Return to the original interface during migration, update the MetroFuse host first, and import Roselight 1.0.1 afterward. Use plugins built for the matching API and Compose ABI; incompatible native plugins can fail to load or crash the app. Re-enable Roselight after confirming that the original interface works.

MetroFuse KMP Beta and this interface may be unstable, crash, or have incomplete features. Migration can cause data loss. Source libraries, radio, account synchronization, lyrics and playback depend on the selected provider and host capabilities. Roselight 1.0.1 passed its build, navigation tests and package-isolation checks; live rendering and playback were not verified for this version. Use the host's restore icon to return to the original interface if a screen misbehaves.

## Build

Supply the matching host API and SDK JARs, JDK 25, and Android SDK platform/build tools 37.0.0. In PowerShell:

```powershell
$env:ANDROID_HOME = 'C:/path/to/android-sdk'
.\gradlew.bat :native:desktopTest :native:packagePluginJar :native:packagePluginApk `
  '-PmetrofuseApiJar=C:/path/to/plugin-api-desktop-1.1.0.jar' `
  '-PmetrofuseSdkJar=C:/path/to/plugin-sdk-desktop-1.1.0.jar'
python tests/check_native_package.py
```

Artifacts and SHA-256 checksums are in `build/outputs/plugins`. The build checks cover provider shelf selection, library grouping, and package isolation. Version 1.0.1 adds source selection, catalog/library search scopes, page and artwork transitions, and revised mobile navigation. Switching sources requires a host with the ui.frontends/v1 service; preview 003 can render the interface but does not provide that service. Live verification of version 1.0.1 is pending.

Design references: [Apple’s mobile design announcement](https://www.apple.com/newsroom/2025/06/apple-introduces-a-delightful-and-elegant-new-software-design/) and the [Apple Music web guide](https://support.apple.com/guide/music-web/welcome/web). MetroFuse plugin code is derived from MetroFlow. See `SOURCE` and `LICENSE`.

## Versioned GitHub Actions builds

The Roselight versioned builds workflow runs for `v*` tags and manual dispatches. It builds a pinned matching MetroFuse API/SDK, runs native tests and package checks, and uploads `roselight-VERSION.apk`, `roselight-VERSION.jar`, build metadata and checksums as versioned Actions artifacts. A tag must match `native/manifest.json`; edit that version before pushing the corresponding release tag. Successful tag builds automatically publish normal GitHub releases. Manual builds default to `publish=false`; set it to true only when the checked packages should be published. The optional host ref must provide the matching API and Compose ABI.

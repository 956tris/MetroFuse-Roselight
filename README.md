# Roselight Music for MetroFuse

Roselight Music is a native MetroFuse interface based on the mobile and web layouts of Apple Music. It uses its own name and music-note mark. The plugin runs on MetroFuse's existing player and sources. Account sign-in and catalog access come from whichever provider plugin is active.

Android uses a scrolling feed, a floating tab dock that condenses as you scroll, a separate Search control, and a glass mini-player. The full-screen player uses the current cover as a blurred backdrop. Desktop uses web-style sidebar navigation, a fixed top playback bar, a central artwork and scrubber display, and horizontally scrolling featured shelves. Both follow the system light or dark theme. Library Artists and Albums group tracks from the current library page. New and Radio depend on the selected source's shelves.

The glass surfaces use Compose gradients, shadows, translucent borders and artwork blur. They approximate the iOS material. Apple artwork, fonts, logos and frameworks are not bundled. MetroFuse controls protected host routes such as Settings and Plugins. Sync, lyrics, catalog and radio availability depend on the host and installed providers.

## Install

Use [MetroFuse UI SDK preview 003](https://github.com/956tris/MetroFuse/releases/tag/v10.0.0-ui-sdk-preview-003) or a compatible host with Compose ABI `kotlin-2.4.21_compose-1.13.0-beta02_api-1.1.0`.

- On Android, import `roselight.apk` from MetroFuse **Settings → Plugins → Import or update plugin**. The system package installer is for the separate host APK.
- On desktop, import `roselight.jar` through the same Plugins screen.
- Enable Roselight Music and choose it under **App interface**. The host's restore icon at the top right returns to the built-in interface.

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

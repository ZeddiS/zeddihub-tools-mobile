# ZeddiHub Tools — Mobile

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/github/v/release/ZeddiS/zeddihub-tools-mobile?color=orange" alt="Release">
  <img src="https://img.shields.io/github/downloads/ZeddiS/zeddihub-tools-mobile/total?color=blueviolet" alt="Downloads">
  <img src="https://img.shields.io/badge/license-Proprietary-red" alt="License">
</p>

> **Android companion to [ZeddiHub Tools Desktop](https://github.com/ZeddiS/zeddihub-tools-desktop).** Monitor game servers, manage players, check connectivity, and administer the full stack — directly from your phone.

---

## Overview

**ZeddiHub Tools Mobile** is the official Android application for the ZeddiHub platform. It shares the same authentication backend as the desktop toolkit and the web dashboard, giving operators and community members a fast way to check server status, ping endpoints, look up IPs, and — for admins — access the full admin panel on the go.

The app is built on modern Android foundations: **Jetpack Compose**, **Material 3**, **Hilt DI**, **Retrofit**, **Coroutines/Flow**, **EncryptedSharedPreferences**, and the **AndroidX Biometric** library.

---

## Features

### Authentication & Session
- Shared account with [zeddihub.eu](https://zeddihub.eu) and the desktop toolkit
- **Biometric unlock** (fingerprint / face) after first login
- **Remember me** with hardware-backed encrypted credential storage
- Role-aware UI — admin features appear only for admin accounts
- Runtime **theme** (light / dark) and **language** switching (English / Čeština)

### Home Dashboard
- Gradient welcome header with server summary chips
- Quick-action cards — Servers, Discord, Web, Admin Panel
- Compact live server list with status dots
- Announcements feed

### Game Servers
- Live status for **Rust PVE**, **CS2 AWP**, **CS:GO AWP**, **CS:GO Surf Combat**, **CS:GO MultiGames**
- Player counts, uptime, map info
- One-tap **copy IP:PORT** to clipboard

### Network Tools
- **Ping Tester** — parallel TCP reachability checks with latency histogram
- **IP Lookup** — GeoIP / ASN / ISP / timezone via `ip-api.com`
- **Device Info** — hardware, memory, battery, active network, local IP

### Admin Panel
- Full desktop admin panel loaded via secure WebView
- Auto-login with saved session — no double sign-in
- Admin-only — hidden for regular users

### Community
- Direct links to Discord, GitHub, donations, and the ZeddiHub website

### Settings
- Theme (light / dark / system)
- Language (en / cs)
- Push notification categories (server down / events / announcements)
- **Auto-update** via GitHub Releases — checks on startup and offers a one-tap install
- **Check for updates** manual button
- **Factory reset** — wipes all stored credentials and preferences
- About section with version, build number, links

---

## Screenshots

> Coming soon — screenshot set in `docs/screenshots/`.

---

## Install

### From Releases (recommended)
1. Open the latest release on the [releases page](https://github.com/ZeddiS/zeddihub-tools-mobile/releases/latest).
2. Download `ZeddiHub-App-<version>.apk`.
3. On your device, enable **Install from unknown sources** for your browser / file manager.
4. Open the APK and install.

Once installed, the app will check for new releases on launch (if **Auto-update** is enabled in Settings) and prompt you to install them.

### System requirements
- Android **8.0** (API 26) or higher
- ~20 MB of free space
- A ZeddiHub account — sign up at [zeddihub.eu](https://zeddihub.eu)

---

## Build from source

### Prerequisites
- Android Studio **Giraffe** or newer (AGP 8.3+)
- JDK **17**
- Android SDK Platform **34**

### Quick build
```bash
git clone https://github.com/ZeddiS/zeddihub-tools-mobile.git
cd zeddihub-tools-mobile
./build.sh          # macOS / Linux
build.bat           # Windows
```

Output APK: `app/build/outputs/apk/debug/ZeddiHub-App-<version>.apk`

### Gradle directly
```bash
./gradlew :app:assembleDebug
```

### Configuration
Edit `app/build.gradle.kts` to point at your own backend if self-hosting:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://your-backend.example/\"")
buildConfigField("String", "WS_BASE_URL",  "\"wss://your-backend.example/ws\"")
```

Optional Firebase Cloud Messaging: drop a `google-services.json` into `app/` and uncomment the FCM plugin block in `app/build.gradle.kts` and the `<service>` in `AndroidManifest.xml`.

---

## Tech stack

| Layer        | Libraries |
|--------------|-----------|
| UI           | Jetpack Compose, Material 3, Navigation Compose |
| DI           | Hilt |
| Networking   | Retrofit, OkHttp, Moshi |
| Async        | Kotlin Coroutines, Flow |
| Storage      | EncryptedSharedPreferences, DataStore |
| Security     | AndroidX Biometric (BIOMETRIC_WEAK) |
| Updates      | GitHub Releases API + FileProvider |

---

## Project structure

```
app/src/main/java/com/zeddihub/mobile
├── ZeddiHubApp.kt              # Application (Hilt entry, notif channels)
├── MainActivity.kt             # Compose host, locale + theme bootstrap
├── di/                         # Hilt modules (network, storage, update)
├── data/
│   ├── local/                  # AppPreferences, CredentialStore
│   ├── remote/                 # ApiService, interceptors, DTOs
│   ├── repository/             # Auth, Server
│   └── update/UpdateChecker.kt # GitHub releases + APK install
├── ui/
│   ├── theme/                  # Color, Type, Theme
│   ├── navigation/             # AppNavGraph, Destinations
│   ├── common/AppShell.kt      # ModalNavigationDrawer shell
│   ├── login/                  # LoginScreen + VM (biometric + remember-me)
│   ├── home/                   # HomeScreen dashboard
│   ├── servers/                # ServersScreen (live status, copy IP)
│   ├── tools/                  # Ping / IpLookup / DeviceInfo
│   ├── profile/                # ProfileScreen
│   ├── notifications/          # NotificationsScreen
│   ├── community/              # CommunityScreen
│   ├── admin/                  # AdminScreen (WebView)
│   └── settings/               # SettingsScreen (theme/lang/update/reset)
└── util/                       # Constants, LocaleManager
```

---

## Roadmap

- [ ] RCON console with command history
- [ ] WebSocket live telemetry streams
- [ ] Foreground *Watch Mode* service with home-screen widget
- [ ] FCM push with deep links to server detail
- [ ] Player ban / kick shortcuts with biometric confirmation
- [ ] Map thumbnails + per-mode badges

---

## Related projects

- **[ZeddiHub Tools Desktop](https://github.com/ZeddiS/zeddihub-tools-desktop)** — Windows TUI toolkit for operators
- **[zeddihub.eu](https://zeddihub.eu)** — Web dashboard

---

## License

Proprietary — © ZeddiS. All rights reserved. Contact the maintainers for usage outside the ZeddiHub ecosystem.

---

<p align="center">
  Made with ❤️ for the ZeddiHub community.<br>
  <sub>Join us on <a href="https://discord.gg/zeddihub">Discord</a></sub>
</p>

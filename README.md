## WebApps

---
## LICENSE

[![License: GPLV3.0](https://img.shields.io/badge/LICENSE-GPLV3.0-red?style=plastic&logo=gplv3&logoColor=%23BD0000)](./LICENSE)

![GitHub Release](https://img.shields.io/github/v/release/hastagaming/webapps?sort=date&display_name=release&style=plastic&logo=github&logoSize=amd)![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/hastagaming/webapps/android-ci.yml?branch=main&style=plastic&logo=github&logoSize=amd)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/hastagaming/webapps/total?style=plastic&label=downloads)![GitHub top language](https://img.shields.io/github/languages/top/hastagaming/webapps?style=plastic)

---

A production-ready, multi-container web browser for Android. WebApps lets you run multiple independent web sessions side by side, each with its own cookies, storage, permissions, and lifecycle — organized into groups, protected with PIN locks, and kept alive in the background through a dedicated foreground service.

## Features

- **Multi Container** — run many isolated web sessions simultaneously, each with independent cookies and local storage.
- **Multi Group** — organize containers into color-coded groups for easier navigation.
- **Fullscreen Mode** — hide the app bar for an immersive, app-like browsing experience.
- **Keep Alive** — allow selected containers to keep running in the background even when not visible.
- **Persistent Session** — cookies and site data survive app restarts.
- **Desktop Mode** — switch a container's user agent to a desktop browser string on demand.
- **Orientation Mode** — lock a container to portrait, landscape, or follow the system setting.
- **Swipe Up App Switcher** — swipe up from the bottom of the browser screen to see and manage all active containers, similar to a mobile task switcher.
- **Favicon Detection** — containers automatically display the favicon of the site they point to.
- **URL Validation & Typo Detection** — catches common domain typos (e.g. `gogle.com` → `google.com`) before loading.
- **HTTPS Enforcement & HTTP Opt-In** — HTTP connections are blocked by default, with an explicit one-time bypass dialog.
- **Dangerous Website Protection** — blocks known malicious domains and detects suspicious homograph attacks.
- **Permission Manager** — per-container control over Camera, Microphone, Location, Notifications, and Storage access (Always Allow / Always Deny / Ask Every Time).
- **Container Lock** — protect any container with a PIN, stored as a SHA-256 hash.
- **Source Inspector** — view a container's live page source (HTML) and a running log of network resource requests.
- **Recovery Mode** — automatically detects crash loops, stuck page loads, and renderer crashes, offering Soft Reset (reload) or Hard Reset (clear cache and cookies).
- **Download Support** — captures download requests from any container and tracks their progress.
- **Backup & Restore** — export all groups and containers to a portable file, optionally encrypted with Android Keystore (AES-256-GCM), with Merge or Replace All restore strategies.
- **Foreground Service** — keeps active containers alive with a persistent notification supporting Open, Refresh, Stop, Refresh All, and Stop All actions.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Architecture:** MVVM (ViewModel + StateFlow, unidirectional Event/State pattern)
- **Dependency Injection:** Hilt
- **Persistence:** Room (SQLite)
- **Serialization:** kotlinx.serialization
- **Security:** Android Keystore, EncryptedSharedPreferences
- **Navigation:** Navigation Compose
- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 35 (Android 15)

## Requirements

- An Android device or emulator running Android 8.0 (API 26) or higher

## Project Structure
```code
app/src/main/java/com/web/apps/
├── WebAppsApplication.kt
├── MainActivity.kt
├── core/
│   ├── container/        # Multi-container session lifecycle management
│   ├── inspector/        # Source Inspector (page source + network log)
│   ├── permission/       # Per-container permission decisions
│   ├── recovery/         # Crash loop / timeout detection and recovery
│   ├── security/         # Keystore, encrypted preferences, safe browsing
│   └── webview/          # WebView client, chrome client, and factory
├── backup/                # Backup export/import and serialization models
├── data/
│   ├── local/             # Room entities, DAOs, database, converters
│   └── repository/        # Repositories and URL validation
├── di/                    # Hilt modules
├── service/               # Foreground service and its controller
└── ui/
    ├── appswitcher/        # Swipe Up App Switcher overlay
    ├── backup/             # Backup & Restore screen
    ├── browser/            # Main browser screen and ViewModel
    ├── containerlist/      # Container list and multi-group grid
    ├── containerlock/      # PIN lock setup and unlock UI
    ├── inspector/          # Source Inspector screen
    ├── navigation/         # NavHost and destinations
    ├── permission/         # Permission Manager screen
    ├── recovery/           # Recovery dialog and ViewModel
    └── theme/              # Material 3 theme
```

## Security Notes
- All PINs are hashed with SHA-256 before being stored; the raw PIN is never persisted.
- Encrypted backups use AES-256-GCM keys generated and stored inside the Android Keystore, so the encryption key never leaves secure hardware-backed storage where available.
- Dangerous Website Protection blocks known malicious domains and detects suspicious non-Latin homograph characters in hostnames.
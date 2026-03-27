# Android Client

## Overview

`android-client` is the Android host app for Nexus Platform. It provides:

- Native shell and UI (Compose)
- WebView runtime container for game packages
- JS bridge (`AndroidApp` / `AndroidAppSync`)
- Backend integration for auth and approved game list

## Architecture

The project now uses layered + feature-oriented structure:

- `core/`
  - `bridge/`: JS bridge and bridge API adapters
  - `network/`: backend endpoint configuration
- `data/`
  - `local/`: local session storage
  - `remote/`: backend API clients
  - `repository/`: repository implementations
- `domain/`
  - `model/`: domain models
  - `usecase/`: business use cases
- `feature/`
  - `auth/`, `main/`, `library/`, `discover/`, `profile/`, `community/`, `game/`
- `ui/`
  - `components/`: reusable UI components
  - `navigation/`: centralized navigation graph
  - `theme/`: app theme

## Build Requirements

- Android Studio Hedgehog+
- JDK 17
- Android SDK 34
- Gradle 8.2 (wrapper)

## Quick Start

1. Check required Android files:

```bash
npm run check:android-setup
```

2. Build debug APK:

```bash
cd android-client
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

## Runtime Notes

- Local backend is accessed through emulator host `10.0.2.2`.
- Auth uses backend login and stores `token + refreshToken` in local session store.
- Main tabs are managed by a centralized nav graph.
- Game runtime is handled by `feature/game/runtime/GameRuntimeActivity`.

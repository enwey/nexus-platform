# Minigame Secure Package Update 2026-04-17

## Scope

This update covers the new secure minigame packaging pipeline, runtime decryption flow, offline runtime policy, source package retention, package-format negotiation, certificate pinning hooks, and the companion documentation added across backend, Android, and iOS.

## Changes

- Added a secure runtime package pipeline for uploaded minigames based on `NEXUS_SECURE_ZIP_V1`.
- Kept the original uploaded developer archive as a separate `source package` while still generating and distributing a separate encrypted `runtime package`.
- Introduced runtime authorization with short-lived signed tickets and one-time key redemption, bound to user, device, app, version, and package format.
- Added package format capability negotiation through `X-Nexus-Supported-Package-Formats` so future secure package versions can be rolled out more safely.
- Refactored Android and iOS runtime decryption paths to dispatch by secure package `format` instead of assuming a single hardcoded flow.
- Added device identity persistence on Android for runtime authorization requests.
- Added optional backend certificate fingerprint pinning hooks for Android and iOS HTTPS requests.
- Strengthened backend production safety checks around secure-package keys and signing configuration.
- Added a dedicated tutorial document for the secure package solution and updated backend/template docs to explain the deployment model.

## Product / Runtime Policy

- First install still requires network access because the client must obtain a runtime ticket and redeem a runtime key before decrypting the package.
- After successful installation, the client should prefer the locally installed playable version, which allows already-installed games to continue running offline.
- Future encryption upgrades are designed to be smoother because the platform now preserves the original upload and the clients report which package formats they support.

## Root Cause / Motivation

The platform previously risked exposing minigame source code through direct download, package interception, or static extraction from downloaded archives. At the same time, switching encryption formats later would have been operationally brittle if we only preserved the encrypted distribution artifact. This update addresses both concerns by separating `source package` from `runtime package`, moving decryption behind a short-lived authorized runtime flow, and preparing both server and clients for versioned secure package formats.

## Validation

- Backend compilation passed after integrating the secure package and ticket flow changes.
- `android-client/gradlew -p android-client compileDebugKotlin`
- iOS host app built, installed, and launched successfully on the connected device.
- `git diff --check`

## Files

- `backend/src/main/java/com/nexus/platform/config/GamePackageProperties.java`
- `backend/src/main/java/com/nexus/platform/config/SecuritySanityCheck.java`
- `backend/src/main/java/com/nexus/platform/controller/GameController.java`
- `backend/src/main/java/com/nexus/platform/entity/Game.java`
- `backend/src/main/java/com/nexus/platform/entity/GameVersion.java`
- `backend/src/main/java/com/nexus/platform/repository/GameVersionRepository.java`
- `backend/src/main/java/com/nexus/platform/service/GameService.java`
- `backend/src/main/java/com/nexus/platform/service/SecureGamePackageService.java`
- `backend/src/main/java/com/nexus/platform/service/UploadProcessingService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/V8__add_secure_package_fields.sql`
- `backend/src/main/resources/db/migration/V9__add_source_package_fields.sql`
- `backend/README.md`
- `android-client/app/build.gradle.kts`
- `android-client/app/src/main/java/com/nexus/platform/core/network/BackendConfig.kt`
- `android-client/app/src/main/java/com/nexus/platform/core/network/BackendHttpClientFactory.kt`
- `android-client/app/src/main/java/com/nexus/platform/data/local/DeviceIdentityStore.kt`
- `android-client/app/src/main/java/com/nexus/platform/data/remote/BackendAuthApi.kt`
- `android-client/app/src/main/java/com/nexus/platform/data/remote/PlatformBackendApi.kt`
- `android-client/app/src/main/java/com/nexus/platform/feature/game/data/GameManager.kt`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/AuthService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/BackendAPIClient.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/BillingService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/DeviceSessionService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/DiscoverHomeService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/GameCatalogService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/GameRuntimeProfileService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/LegalConfigService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/LibraryHomeService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/ReferralService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/UserProfileService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Network/WalletService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Core/Storage/VersionedGameStorageManager.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Features/Recommend/RecommendService.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Runtime/Bridge/APIs/DefaultBridgeAPIs.swift`
- `ios-client/NexusPlatform/Sources/NexusPlatform/Runtime/Lifecycle/UpdateCheckService.swift`
- `ios-client/NexusPlatformApp/Info.plist`
- `ios-client/scripts/run-ios-device.sh`
- `templates/minigame-starter/README.md`
- `docs/minigame-secure-package-guide.md`
- `docs/2026-04-17-minigame-secure-package-update.md`

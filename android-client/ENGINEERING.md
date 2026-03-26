# Android Engineering Notes

## Current Status

- Architecture migrated to feature-oriented + layered structure.
- Legacy screen packages removed.
- Android build wrapper repaired (`gradle-wrapper.jar` restored).
- `:app:assembleDebug` passes.

## Key Technical Decisions

1. UI logic moved behind ViewModel for key flows (`auth`, `library`).
2. Backend calls are now behind repositories + use cases.
3. JS bridge APIs live under `core/bridge/api` to avoid confusion with backend remote APIs.
4. Main screen routing is centralized in `ui/navigation/MainNavGraph.kt`.

## Build Commands

```bash
npm run check:android-setup
```

```bash
cd android-client
./gradlew --no-daemon :app:assembleDebug
```

## Follow-up Recommendations

- Add DI framework (Hilt/Koin) to replace manual ViewModel factory wiring.
- Introduce `Result` sealed types for repository/usecase return consistency.
- Add UI tests for login and library loading states.
- Add token refresh flow in Android client for long sessions.

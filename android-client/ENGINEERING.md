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
5. Typography is aligned to Material 3 official type scale in `ui/theme/Type.kt`.

## Typography Standard

- Source of truth: `app/src/main/java/com/nexus/platform/ui/theme/Type.kt`.
- Base scale: Material 3 (`display/headline/title/body/label`) with official size, line-height, and letter-spacing tokens.
- Usage convention:
  - Page hero title: `headlineLarge`
  - Section title: `titleLarge`
  - Body text: `bodyMedium` / `bodyLarge`
  - Secondary hint: `bodySmall`
  - Buttons and interactive labels: `labelLarge`

## Interaction Standard

- Back behavior:
  - System back and UI back must map to the same destination.
  - Predictive back is enabled via `android:enableOnBackInvokedCallback="true"`.
- Motion:
  - Navigation transitions use one consistent duration family (enter ~220ms, exit ~180ms) and `FastOutSlowInEasing`.
  - Forward transition direction: left; back transition direction: right.
- No white flash policy:
  - `windowBackground` and Compose root background must be the same base color.
  - `Scaffold` container color is fixed to `BackgroundBase`.
- Touch targets:
  - Interactive controls should keep a minimum visual/touch size near 48dp.

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

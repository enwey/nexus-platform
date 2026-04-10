# Android Client Update 2026-04-10

## Scope

This update covers the Android game/library entry wording refresh and the library section navigation fix.

## Changes

- Renamed the main library tab label from "游戏库 / 遊戲庫 / Library" to "游戏 / 遊戲 / Games".
- Updated related cold-start copy to match the new "Games" naming.
- Fixed `LibrarySectionListScreen.kt` compile issue by importing `androidx.compose.foundation.lazy.grid.item`.
- Replaced the library section back icon resource with a valid Android `VectorDrawable` instead of raw SVG content.
- Updated the section page top-left back button sizing to a larger WeChat-like touch target and visual size.

## Root Cause

The "Recent Plays -> More" page used `R.drawable.ic_back` via `painterResource(...)`, but the resource file contained raw SVG markup rather than an Android-parseable drawable. That caused the section screen to fail when loading the icon.

## Validation

- `android-client/gradlew -p android-client :app:compileDebugKotlin`
- `android-client/gradlew -p android-client assembleDebug`
- Installed debug APK to connected Android device with `adb install -r`
- Confirmed app can launch to `MainActivity`

## Files

- `android-client/app/src/main/java/com/nexus/platform/feature/library/ui/LibrarySectionListScreen.kt`
- `android-client/app/src/main/res/drawable/ic_back.xml`
- `android-client/app/src/main/res/values/strings.xml`
- `android-client/app/src/main/res/values-zh-rTW/strings.xml`
- `android-client/app/src/main/res/values-zh-rCN/strings.xml`
- `android-client/app/src/main/res/values-en/strings.xml`

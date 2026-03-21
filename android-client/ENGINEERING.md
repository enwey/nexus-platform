# Android Engineering Notes

## Build Target

The Android client is expected to build as a standalone Gradle project under `android-client`.

## Required Wrapper Files

The project should contain:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`

## Required App Files

- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/layout/activity_game.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

## Current Build Status

- Project structure has been normalized for Gradle.
- Runtime bridge and minimum resources are present.
- Wrapper scripts and properties can be checked with:

```bash
npm run check:android-setup
```

## Remaining Requirement

`gradle-wrapper.jar` still needs to be provided before the project can be built with `./gradlew`.

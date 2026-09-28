# Alarm

A vibrate-only Android alarm app, built with Kotlin.

## Build

Open this project in Android Studio and sync the Gradle project. The build requires
JDK 17 and Android SDK Platform 35 (Android Gradle Plugin 8.9.2 / Gradle 8.11.1).

The app module is `app`. Its launcher activity is `MainActivity`.

Build and run with `./gradlew assembleDebug`, or open the project in Android Studio.
Run the repeat-calculation tests with `./gradlew test`.

On Android 12 and newer, allow exact alarms for on-time delivery. Android 13 and
newer also require notification access for alarm controls; Android 14 and newer
may require full-screen alarm access to show the ringing screen over the lock
screen. If full-screen access is unavailable, the soundless notification still
provides snooze and dismiss actions.

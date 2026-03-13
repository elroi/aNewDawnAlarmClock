# LemurLoop: Android Alarm Clock Xtreme Reimagined

Welcome to **LemurLoop**, your new accountability partner and gentle wake-up assistant.

## Features at a Glance
- 🌞 **Gentle Wake**: Volume crescendo over 60 seconds.
- 🗣 **Alarm-Pal Briefing**: Wakes you up with weather, calendar, and motivation.
- 🤝 **Accountability**: Texts a friend if you oversleep!
- 🧠 **Smart Defaults**: Suggests alarms based on your Calendar.
- 🧮 **Mean Alarms**: Solve math or shake to dismiss.
- 🌙 **Sleep Tracking**: Monitors your restlessness.

## How to Run
1.  **Open in Android Studio**: Select `Open` and choose this folder (`/Users/elroiluria/dev/aNewDawnAlarmClock`).
2.  **Sync Gradle**: Allow Android Studio to download dependencies.
3.  **Run**: Press the green Play button.
4.  **Permissions**: Make sure to grant requested permissions (Notification, Alarm, SMS, Calendar) on the first run.

## Running Unit Tests

- **JDK requirement**: Make sure you have a recent JDK installed (the project is built and tested with a JBR/JDK compatible with AGP 8.2; JDK 17+ is recommended).
- **Gradle JDK configuration**:
  - By default, `gradle.properties` points `org.gradle.java.home` at Android Studio's bundled JBR.
  - If that path does not exist on your machine or CI, either:
    - Update `org.gradle.java.home` to a valid local JDK install, or
    - Comment it out and rely on a correctly configured `JAVA_HOME`.
- **Run JVM unit tests**:
  - From the command line: `./gradlew :app:testDebugUnitTest`
  - From Android Studio: use the Gradle tool window or run tests from the gutter in `app/src/test/java`.
- **CI**: GitHub Actions runs the same unit tests on push/PR to `main` (see `.github/workflows/unit-tests.yml`); the workflow uses JDK 17 and does not rely on a local `org.gradle.java.home` path.

## Architecture
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material You)
- **DI**: Hilt
- **DB**: Room

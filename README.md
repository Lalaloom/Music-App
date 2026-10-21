# Dad Music App

A native Android music player for local mp3 and mp4 karaoke files.

Built with Kotlin, Jetpack Compose, Media3 ExoPlayer, Room, and Hilt.

## Requirements

- Android 8.0+ (API 26) target device
- Android SDK with build-tools 34 and platform 34 (AGP will auto-download on first build if `sdk.dir` is set in `local.properties`)
- **JDK 17 or 21** on your development machine (the Android Gradle Plugin and Kotlin DSL require it)

### Pointing Gradle at your JDK

The project does NOT pin a JDK path (so clones and CI stay portable). Pick one of these:

1. **Set `JAVA_HOME`** to a JDK 17 or 21 install (recommended; Temurin / Microsoft Build of OpenJDK / Zulu / Corretto all work):
    ```bash
    export JAVA_HOME="/path/to/jdk-17"      # macOS / Linux / Git-Bash
    setx JAVA_HOME "C:\path\to\jdk-17"      # Windows PowerShell (one-time)
    ```

2. **Or** add this line to `~/.gradle/gradle.properties` (user-level, not committed):
    ```properties
    org.gradle.java.home=C:\\path\\to\\jdk-17
    ```

> Avoid using the Java 25 JBR bundled with Android Studio — the Kotlin DSL embedded in current Gradle versions cannot parse Java 25's version string yet.

## Building

### Debug APK

```bash
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Running unit tests

```bash
./gradlew :app:testDebugUnitTest
```

### Running instrumented tests (requires connected device)

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Release signing (Task 12)

To be documented once navigation and signing are wired in Milestone 1, Task 12.

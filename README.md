# Dad Music App

A native Android music player for local mp3 and mp4 karaoke files.

Built with Kotlin, Jetpack Compose, Media3 ExoPlayer, Room, and Hilt.

## Requirements

- Android 8.0+ (API 26)
- Xiaomi tablet or any Android device

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

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

## Release signing

### Generating a keystore (one-time, developer)

Run this once on your development machine and keep the resulting `.jks` file somewhere safe (outside the repository):

```bash
keytool -genkeypair \
  -keystore ~/dad-music-release.jks \
  -alias dadmusic \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass your_store_password \
  -keypass your_key_password \
  -dname "CN=Dad Music, OU=Personal, O=Cameron Batten, L=, S=, C=AU"
```

Replace `your_store_password` and `your_key_password` with strong passwords you choose.

### Putting credentials in Gradle properties

Open (or create) `~/.gradle/gradle.properties` (this file is **never** committed to any repo) and add the four properties:

```properties
MUSICAPP_KEYSTORE_PATH=/Users/you/dad-music-release.jks
MUSICAPP_KEYSTORE_PASSWORD=your_store_password
MUSICAPP_KEY_ALIAS=dadmusic
MUSICAPP_KEY_PASSWORD=your_key_password
```

On Windows use double backslashes for the path — `gradle.properties` follows Java-properties
escape rules, so `\\` is a single `\`. Type the double-backslash form literally:

```properties
MUSICAPP_KEYSTORE_PATH=C:\\Users\\you\\dad-music-release.jks
```

> Note: if you type `C:\Users\...` (single backslash) Gradle will silently misparse the path
> and signing will fail with a "keystore not found" error.

### Building a signed release APK

```bash
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk  (signed)
# Without the four properties above the output will be app-release-unsigned.apk
```

---

## Installing the APK on an Android device (musician's guide)

You do not need a Google Play account. Follow these steps on the Android device (tablet or phone):

1. **Receive the APK file.** Your developer can send it via email, a Dropbox link, or a USB cable transfer. Save it somewhere easy to find, such as the Downloads folder.

2. **Allow installation from unknown sources.**
   - Open **Settings → Apps** (or **Settings → Apps & notifications**).
   - Tap **Special app access** → **Install unknown apps**.
   - Find the app you will use to open the APK (for example **Files** or **My Files** or your email app) and turn **Allow from this source** on.

3. **Open the APK.** Use your file manager app to navigate to the APK and tap it. Android will ask "Do you want to install this app?" — tap **Install**.

4. **Grant storage access.** On first launch the app will ask for permission to access your music files. Tap **Allow** (or **Allow access to media files**) so the app can scan your folders.

5. **Pick your music folder.** Tap the folder icon, select the folder that contains your mp3 and mp4 karaoke files, and the library will populate automatically.

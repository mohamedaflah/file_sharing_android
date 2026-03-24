# ShareFast

Native **Kotlin** + **Jetpack Compose** app for sending files, media, and apps to nearby devices over the same Wi‑Fi or hotspot (LAN transfer, similar in spirit to Xender / AirDrop-style workflows).

## Requirements

- **Android Studio** (recent stable) with **Android SDK 35** and **Build-Tools** installed  
- **JDK 17** (Android Studio’s embedded JDK is fine)  
- **Physical device** recommended for real discovery and transfers; emulator is fine for UI work  

`compileSdk` / `targetSdk`: **35** · `minSdk`: **26** — see `app/build.gradle.kts`.

## Setup

1. Clone or copy this repository.
2. Open the **project root** in Android Studio (the folder that contains `settings.gradle.kts`).
3. Let **Gradle sync** finish. If prompted, accept SDK licenses and install missing components.
4. **`local.properties`** is created by Android Studio with your `sdk.dir=...`. It is gitignored; do not commit it.

### Command line only

If the Gradle wrapper is present (`gradlew` / `gradlew.bat`), you do not need a global Gradle install.

## Run the app

### Android Studio

1. Select the **`app`** run configuration.
2. Pick a device or emulator.
3. **Run** (▶).

### Command line

**Windows (PowerShell or CMD), from the project root:**

```bat
gradlew.bat assembleDebug
gradlew.bat installDebug
```

**macOS / Linux:**

```bash
chmod +x gradlew
./gradlew assembleDebug
./gradlew installDebug
```

The debug APK is output under `app/build/outputs/apk/debug/` (that tree is ignored by git when `build/` is ignored).

## Permissions

On first use the app may request **notifications**, **nearby Wi‑Fi**, and **media** access depending on Android version. **Camera** is requested when you use **Scan QR**. For a full permission table and manifest summary, see **[SETUP.md](SETUP.md)**.

## More documentation

- **[SETUP.md](SETUP.md)** — architecture overview, two-device transfer test, QR flow, troubleshooting, release builds.

# ShareFast — installation and setup (zero to running)

This project is a **native Kotlin + Jetpack Compose** LAN file-sharing app (offline over Wi‑Fi / hotspot). Use this guide to install tools, create/sync the project, grant permissions, and run transfers between two devices.

---

## 1. Install tools

### Android Studio

1. Download and install the latest **Android Studio** from [https://developer.android.com/studio](https://developer.android.com/studio).
2. In the SDK Manager, install:
   - **Android SDK Platform 35** (or the `compileSdk` shown in `app/build.gradle.kts`)
   - **Android SDK Build-Tools**
   - **Android Emulator** (optional) and a system image (e.g. API 34 or 35)

### JDK

- Android Studio bundles a suitable **JDK 17** (JBR). Use **File → Settings → Build, Execution, Deployment → Build Tools → Gradle** and set **Gradle JDK** to **Embedded JDK 17** (or any JDK 17+).

### Device

- **Physical device (recommended for Wi‑Fi / NSD / UDP):** enable **Developer options** and **USB debugging**.
- **Emulator:** useful for UI work; real LAN discovery and hotspot scenarios are easier on hardware.

---

## 2. Open the project and sync Gradle

1. **File → Open** and select the `native_android` folder (the one that contains `settings.gradle.kts`).
2. If Gradle asks to create a **Gradle Wrapper**, accept it, or run **File → Settings → Build Tools → Gradle →** use **Gradle wrapper**.
3. Let **Sync Project with Gradle Files** finish. Resolve any SDK/license prompts.

### If the wrapper is missing

From a machine with Gradle installed:

```bash
gradle wrapper --gradle-version 8.7
```

Then open the project again in Android Studio.

---

## 3. Project layout (Clean Architecture)

| Layer | Package |
|--------|---------|
| **Presentation** | `com.sharefast.presentation` — Compose UI, ViewModels, navigation |
| **Domain** | `com.sharefast.domain` — models, repository interfaces |
| **Data** | `com.sharefast.data` — Room, repository implementations, `SendQueueStore` |
| **Services** | `com.sharefast.services` — TCP transfer, foreground service, discovery helpers |
| **Core** | `com.sharefast.core` — ports, wire protocol |
| **Utils / DI** | `com.sharefast.utils`, `com.sharefast.di` |

---

## 4. Required permissions (declared + requested at runtime)

On first launch the app requests **core** permissions together:

| Android version | What is requested |
|-----------------|-------------------|
| **13+ (API 33+)** | `POST_NOTIFICATIONS`, `NEARBY_WIFI_DEVICES`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` |
| **12 and below** | `READ_EXTERNAL_STORAGE` (for gallery / indexed documents) |

**Camera** is requested only when you open **Scan QR**.

Without media permissions, **Images** / **Videos** show a settings shortcut; **Documents** can still use **Pick files** (system picker) to queue items.

---

## 4b. Required permissions (manifest summary)

Declared in `app/src/main/AndroidManifest.xml`:

| Area | Permissions |
|------|-------------|
| **Network / LAN** | `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `NEARBY_WIFI_DEVICES` |
| **Foreground work** | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` |
| **Notifications** | `POST_NOTIFICATIONS` (Android 13+) |
| **Media** | `READ_MEDIA_*`, legacy `READ_EXTERNAL_STORAGE` (≤ API 32) |
| **Apps / APK** | `QUERY_ALL_PACKAGES` (for listing user apps) |
| **Camera** | `CAMERA` (QR scan) |
| **Haptics** | `VIBRATE` |

Runtime prompts: **camera** (QR) and **notifications** should be accepted when the OS asks. Media access follows scoped storage rules; documents are queried via `MediaStore`.

**Optional (not in manifest):** `MANAGE_EXTERNAL_STORAGE` is **not** required for the default media/document queries; only add it if you expand to full file-manager scope (Play policy applies).

---

## 5. Run the app

1. Select a **run configuration** for module **`app`**.
2. Choose a **device** or emulator.
3. **Run ▶**.

---

## 6. End-to-end transfer test (two phones)

1. Connect **both devices** to the **same Wi‑Fi** or put one in **Wi‑Fi hotspot** mode and join from the other.
2. Install and open **ShareFast** on **both**.
3. On the **receiver:** stay on **Home** — a **TCP server** listens on port **`17342`** (see `ShareConstants`). A **foreground notification** indicates the service.
4. On the **sender:** open **Images / Videos / Documents / Apps**, multi-select items, tap **Add … to queue**.
5. Return to **Home**. Under **Nearby devices**, tap the peer — transfer starts; **progress / speed / pause / resume / cancel** appear in the bottom sheet.
6. Received files are written under:
   - `Android/data/com.sharefast/files/Download/ShareFast/` (app-specific storage).

### QR connect

- **Show QR** on Home encodes `sharefast://<ip>:17342`.
- **Scan** (toolbar) opens **CameraX + ML Kit**; a valid payload triggers the same send flow as tapping a peer (queue must not be empty).

---

## 7. Typography (Poppins / Inter)

The theme uses the **system sans-serif** stack with tuned `Typography`. To use **Poppins** or **Inter**:

1. Add `.ttf` files under `app/src/main/res/font/`.
2. In `presentation/theme/Type.kt`, set `fontFamily = FontFamily(Font(R.font.your_font))` on the desired `TextStyle`s.

---

## 8. Build release

1. **Build → Generate Signed App Bundle / APK** (or configure signing in Gradle).
2. Keep `minifyEnabled` rules updated if you add reflection-based libraries.

---

## 9. Troubleshooting

| Issue | What to check |
|--------|----------------|
| No peers | Same subnet, firewall, VPN off; try hotspot; UDP port `17341` and NSD type `_sharefast._tcp.` |
| Connect fails | Receiver on Home, not killed by battery saver; port `17342` not blocked |
| QR scan no-op | Camera permission; payload must match `sharefast://host:port` |
| APK send fails | Sender must be able to read `ApplicationInfo.sourceDir` (same app, user apps only) |

---

## 10. Commands (when Gradle wrapper exists)

```bash
./gradlew assembleDebug
./gradlew installDebug
```

On Windows: `gradlew.bat assembleDebug`.

This completes **full setup from zero** through **advanced LAN transfer** for this repository.

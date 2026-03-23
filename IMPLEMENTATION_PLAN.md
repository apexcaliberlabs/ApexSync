# ApexSync – Implementation Plan

## Overview

ApexSync is an Android application that provides two key networking features **without requiring root access**:

1. **Wi-Fi Hotspot** – Share a device's cellular (mobile data) connection with nearby devices.
2. **Wi-Fi Repeater** – Connect to an existing Wi-Fi network and re-broadcast it so additional devices can reach it (range extension).

---

## Goals & Non-Goals

### Goals
- Run entirely without root privileges.
- Target Android 8.0 (API 26) and above, leveraging modern APIs.
- Provide a clean, intuitive Material Design UI.
- Expose hotspot and repeater features as foreground services so they persist while the screen is off.
- Display a persistent notification with status information and a quick-stop action.
- Handle runtime permissions gracefully and guide the user when permissions are missing.

### Non-Goals
- Bridging traffic at the kernel/IP-forwarding layer (requires root).
- Supporting Android versions below API 26 (older APIs for hotspot control were removed/restricted).
- VPN-based tunnelling.
- Carrier unlock or tethering-restriction bypass.

---

## Architecture

```
apexsync/
├── ui/
│   ├── MainActivity            – hosts bottom-nav (Hotspot | Repeater | Settings)
│   ├── HotspotFragment         – controls and status for the hotspot feature
│   ├── RepeaterFragment        – controls and status for the repeater feature
│   └── SettingsFragment        – app-level configuration
├── service/
│   ├── HotspotService          – foreground service managing the local-only hotspot
│   └── RepeaterService         – foreground service managing the repeater role
├── manager/
│   ├── HotspotManager          – thin wrapper around WifiManager LocalOnlyHotspot API
│   └── RepeaterManager         – manages WifiP2pManager group creation + ICS
├── util/
│   ├── PermissionHelper        – centralises runtime permission requests
│   └── NetworkUtils            – helper methods (IP address lookup, subnet info, etc.)
└── model/
    ├── HotspotConfig           – SSID, password, band, security type
    └── RepeaterConfig          – target SSID, password, channel
```

### Component Interaction

```
User taps "Start"
       │
       ▼
   Fragment
       │  startService(intent)
       ▼
  Foreground Service  ──────►  Manager  ──────►  Android Wi-Fi API
       │                           │
       │  LiveData / broadcast      │  callback
       ▼                           ▼
   Fragment UI update         Notification update
```

---

## Feature: Wi-Fi Hotspot

### API Used
`WifiManager.startLocalOnlyHotspot(LocalOnlyHotspotCallback, Handler)` (API 26+)

### Behaviour
- **No internet sharing** – `LocalOnlyHotspot` creates an isolated network (other devices can connect but they cannot reach the internet through this device).
- If the user wants internet sharing they must use the system Tethering Settings; the app will open that screen with a deep-link button.
- App can configure SSID and passphrase via `WifiConfiguration` on API 28 and below; on API 29+ the OS randomises the SSID/pass unless the app holds `NETWORK_SETUP_WIZARD` permission (unavailable to third-party apps). The app will display the OS-assigned credentials.

### Required Permissions
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />   <!-- required to receive hotspot config -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
```

### Flow
1. User opens Hotspot tab.
2. App checks `ACCESS_FINE_LOCATION` permission; requests if missing.
3. User taps **Start Hotspot**.
4. `HotspotFragment` → starts `HotspotService`.
5. `HotspotService` calls `HotspotManager.startHotspot()`.
6. `HotspotManager` registers a `LocalOnlyHotspotCallback`; on `onStarted()` it extracts the `WifiConfiguration` (SSID + passphrase) and broadcasts it.
7. `HotspotService` posts a persistent notification showing SSID/passphrase.
8. `HotspotFragment` shows a QR code (zxing) for easy connection.
9. User taps **Stop Hotspot** → `HotspotService.stopSelf()` → `HotspotManager.stopHotspot()`.

---

## Feature: Wi-Fi Repeater

### Approach (No Root)

A true L2 bridge requires root. Instead the app uses **Wi-Fi Direct (P2P Group Owner) + NAT/local-only routing**:

1. The Android device connects to the upstream Wi-Fi network normally (managed by the OS).
2. The app creates a **Wi-Fi Direct Group** (the device acts as Group Owner / soft-AP) via `WifiP2pManager.createGroup()`.
3. Peer devices discover the Group Owner via Wi-Fi Direct service discovery or direct connection.
4. The Group Owner has its own subnet (192.168.49.x by default); traffic is **not** automatically forwarded to the upstream network.
5. To bridge traffic, the app uses Android's **`ConnectivityManager.requestNetwork()`** + **`VpnService`** to create a userspace NAT that forwards packets between the two interfaces.

> **Limitation**: Battery impact is higher due to the userspace packet-forwarding loop. The UI will clearly state this limitation and recommend the system hotspot for simple sharing.

### Required Permissions
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />   <!-- API 33+ -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

### Flow
1. User opens Repeater tab.
2. App checks that device is connected to Wi-Fi (upstream network).
3. User taps **Start Repeater**.
4. `RepeaterFragment` → starts `RepeaterService`.
5. `RepeaterService` calls `RepeaterManager.startRepeater()`.
6. `RepeaterManager` calls `WifiP2pManager.createGroup()` with a known passphrase.
7. `WifiP2pManager.GroupInfoListener` callback provides the Group Owner credentials.
8. `RepeaterService` posts notification with repeater SSID.
9. User taps **Stop Repeater** → `RepeaterManager.removeGroup()`.

---

## UI Design

### Bottom Navigation
| Tab | Icon | Description |
|-----|------|-------------|
| Hotspot | wifi_tethering | Local-only hotspot |
| Repeater | router | Wi-Fi repeater / extender |
| Settings | settings | App preferences |

### Hotspot Fragment
- Toggle switch (Start / Stop)
- SSID display field
- Password display field (with show/hide toggle)
- QR code image (generated by zxing)
- "Open System Tethering" button (for internet sharing)
- Connected device count badge

### Repeater Fragment
- Toggle switch (Start / Stop)
- Group SSID display
- Group password display
- Status card (Idle / Starting / Active / Error)
- Warning banner ("Battery usage may be higher")

### Settings Fragment
- Theme toggle (Light / Dark / System)
- Notification toggle
- About section (version, GitHub link, license)

---

## Project Module Structure

```
ApexSync/
├── build.gradle                        (project-level, existing)
├── settings.gradle                     (NEW – registers :app module)
├── gradle.properties                   (NEW – JVM args, AndroidX flag)
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties       (NEW)
├── gradlew / gradlew.bat               (NEW)
└── app/
    ├── build.gradle                    (NEW – app module)
    ├── proguard-rules.pro              (NEW)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml     (NEW)
        │   ├── java/com/apexcaliberlabs/apexsync/
        │   │   ├── ui/
        │   │   ├── service/
        │   │   ├── manager/
        │   │   ├── util/
        │   │   └── model/
        │   └── res/
        │       ├── layout/
        │       ├── drawable/
        │       ├── values/
        │       └── navigation/
        ├── test/                       (unit tests)
        └── androidTest/               (instrumented tests)
```

---

## Build Configuration

### Min SDK / Target SDK
| Property | Value |
|----------|-------|
| `minSdk` | 26 (Android 8.0 Oreo) |
| `targetSdk` | 34 (Android 14) |
| `compileSdk` | 34 |

### Key Dependencies
| Library | Purpose |
|---------|---------|
| `androidx.appcompat:appcompat` | Backward-compatible Activity/Fragment |
| `com.google.android.material:material` | Material Design components |
| `androidx.navigation:navigation-fragment-ktx` | Bottom-nav + Fragment navigation |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | ViewModel + LiveData |
| `com.journeyapps:zxing-android-embedded` | QR code generation |
| `androidx.preference:preference-ktx` | Settings screen |
| JUnit 4 + Mockito | Unit tests |
| Espresso | Instrumented UI tests |

---

## Permissions Rationale

| Permission | Reason |
|-----------|--------|
| `ACCESS_FINE_LOCATION` | Required by Android to start `LocalOnlyHotspot` and to scan for P2P peers |
| `ACCESS_WIFI_STATE` | Read current Wi-Fi state |
| `CHANGE_WIFI_STATE` | Modify Wi-Fi state to create hotspot/group |
| `NEARBY_WIFI_DEVICES` (API 33+) | Required for Wi-Fi P2P on Android 13+ |
| `FOREGROUND_SERVICE` | Keep service alive in background |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Required for hotspot/tethering foreground services on API 34+ |

---

## Testing Strategy

### Unit Tests (JVM)
- `HotspotManagerTest` – mock `WifiManager`, verify callback handling
- `RepeaterManagerTest` – mock `WifiP2pManager`, verify group creation flow
- `PermissionHelperTest` – verify permission-check logic
- `NetworkUtilsTest` – verify IP/subnet utility methods

### Instrumented Tests (Device/Emulator)
- `HotspotServiceTest` – start/stop service, verify notification shown
- `MainActivityTest` – navigate between tabs, verify fragments load

---

## Implementation Phases

### Phase 1 – Project Scaffolding *(Immediate)*
- [x] Existing: project-level `build.gradle`, `README.md`, `LICENSE`
- [ ] Add `settings.gradle`
- [ ] Add `gradle.properties`
- [ ] Add Gradle wrapper files
- [ ] Create `app/` module with `build.gradle`

### Phase 2 – Android Manifest & Resources *(Sprint 1)*
- [ ] `AndroidManifest.xml` with all permissions, activities, and services
- [ ] Base theme resources (colors, styles, strings)
- [ ] Navigation graph
- [ ] Bottom navigation menu

### Phase 3 – Core Business Logic *(Sprint 1)*
- [ ] `HotspotConfig` and `RepeaterConfig` data models
- [ ] `HotspotManager` – wraps `WifiManager.startLocalOnlyHotspot()`
- [ ] `RepeaterManager` – wraps `WifiP2pManager.createGroup()`
- [ ] `PermissionHelper` – runtime permission requests
- [ ] `NetworkUtils` – IP/subnet helpers

### Phase 4 – Services *(Sprint 2)*
- [ ] `HotspotService` – foreground service, notification management
- [ ] `RepeaterService` – foreground service, notification management

### Phase 5 – UI *(Sprint 2)*
- [ ] `MainActivity` with bottom navigation
- [ ] `HotspotFragment` – UI + ViewModel
- [ ] `RepeaterFragment` – UI + ViewModel
- [ ] `SettingsFragment` – preferences screen
- [ ] Layouts, drawables, and string resources

### Phase 6 – QR Code Integration *(Sprint 3)*
- [ ] Integrate zxing-android-embedded
- [ ] Generate Wi-Fi QR code from SSID + passphrase

### Phase 7 – Testing *(Sprint 3)*
- [ ] Unit tests for managers and utils
- [ ] Instrumented tests for services and navigation

### Phase 8 – Polish & Release *(Sprint 4)*
- [ ] ProGuard / R8 rules
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Play Store assets (icon, screenshots, description)
- [ ] Release signing configuration

---

## Open Questions / Risks

1. **Hotspot internet sharing** – `LocalOnlyHotspot` does not route internet traffic. On API 30+, `WifiManager.startTetheredHotspot()` requires `TETHER_PRIVILEGED` (system-only). The app should direct users to system settings for full tethering.
2. **Wi-Fi Repeater viability** – True repeater (bridging two Wi-Fi interfaces) is not possible without root. The P2P + userspace NAT approach adds latency and battery drain. This must be clearly communicated to users.
3. **API 29+ SSID restrictions** – `WifiConfiguration` is deprecated on API 29+; the OS may randomise SSID/passphrase. The app needs fallback UI to display OS-assigned credentials.
4. **Android 13+ Nearby Wi-Fi Devices permission** – P2P APIs require `NEARBY_WIFI_DEVICES` instead of `ACCESS_FINE_LOCATION` on API 33+. The permission request flow must branch on SDK version.
5. **Foreground service type** – API 34 requires explicit `foregroundServiceType` in the manifest. Use `connectedDevice` for both services.

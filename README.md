# ApexSync

An Android app that creates a Wi-Fi Hotspot to share cellular data or extend an existing Wi-Fi connection as a Wi-Fi repeater. **No root required.**

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Building the Project](#building-the-project)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**ApexSync** is a no-root Android application that lets users:

1. **Share cellular data** — broadcast a Wi-Fi hotspot so other devices can connect to the internet through the phone's mobile data connection.
2. **Extend a Wi-Fi connection** — act as a Wi-Fi repeater by connecting to an upstream network and rebroadcasting it, expanding coverage without additional hardware.

The app targets Android devices and leverages built-in Android Wi-Fi and hotspot APIs, requiring no superuser (root) access.

---

## Features

| Feature | Description |
|---|---|
| Wi-Fi Hotspot | Create a personal hotspot to share cellular data with nearby devices |
| Wi-Fi Repeater | Connect to an existing Wi-Fi network and rebroadcast it |
| No Root Required | Works entirely within the standard Android permission model |

---

## Technologies Used

| Technology | Version / Notes |
|---|---|
| **Android SDK** | Target/Compile SDK defined per module |
| **Android Gradle Plugin** | 7.0.0 |
| **Gradle** | Build automation and dependency management |
| **Google Maven Repository** | Android libraries and tools |
| **Maven Central** | Third-party dependencies |

The project is a standard Android application built with the **Gradle** build system. It uses:

- **Android Wi-Fi APIs** (`WifiManager`, `WifiConfiguration`) for hotspot and repeater functionality.
- **Android ConnectivityManager** for monitoring and managing network connections.
- The app's UI is built with standard Android Views/Layouts (XML-based).

---

## Project Structure

```
ApexSync/
├── build.gradle          # Project-level Gradle build file; configures repositories
│                         # and the Android Gradle Plugin for all sub-projects/modules
├── LICENSE               # Apache License 2.0
└── README.md             # This file

# Standard Android modules (added as development progresses):
# app/
#   ├── src/
#   │   ├── main/
#   │   │   ├── java/         # Application source code (Java or Kotlin)
#   │   │   ├── res/          # UI resources (layouts, drawables, strings, etc.)
#   │   │   └── AndroidManifest.xml  # App manifest, permissions, component declarations
#   │   ├── test/             # Unit tests
#   │   └── androidTest/      # Instrumented (on-device) tests
#   └── build.gradle          # Module-level build file (SDK versions, dependencies)
```

### Key Files

| File | Purpose |
|---|---|
| `build.gradle` | Project-level build configuration. Declares the Android Gradle Plugin (`com.android.tools.build:gradle:7.0.0`) and repository sources (`google()`, `mavenCentral()`). |
| `LICENSE` | Apache License 2.0 — governs use, reproduction, and distribution of the project. |
| `README.md` | Project documentation (this file). |

---

## Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Hedgehog / 2023.1.1 or later recommended)
- Android SDK Platform Tools
- JDK 11 or later

### Clone the Repository

```bash
git clone https://github.com/apexcaliberlabs/ApexSync.git
cd ApexSync
```

### Open in Android Studio

1. Launch **Android Studio**.
2. Select **File → Open** and navigate to the cloned `ApexSync` folder.
3. Wait for Gradle to sync and download dependencies.

---

## Building the Project

Build the debug APK from the command line:

```bash
# On macOS/Linux
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

The resulting APK will be located at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install directly to a connected device:

```bash
./gradlew installDebug
```

Run unit tests:

```bash
./gradlew test
```

Run instrumented (on-device) tests:

```bash
./gradlew connectedAndroidTest
```

---

## Contributing

Contributions are welcome! To get started:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-feature`.
3. Commit your changes: `git commit -m "Add my feature"`.
4. Push to your fork: `git push origin feature/my-feature`.
5. Open a Pull Request against `main`.

Please follow Android and Java/Kotlin coding conventions and include relevant tests where applicable.

---

## License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for full terms.


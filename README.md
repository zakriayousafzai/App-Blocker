# mindful space. — App Blocker

A sleek, modern, and minimalist digital wellness application for Android. Designed to foster focus and mindful technology habits, **mindful space** helps users monitor their screen time and enforce intentional usage limits for application packages. When a configured limit is exceeded, a system overlay screen gently intervenes to guide the user back to real-world productivity.

---

## 🎨 Design Philosophy & UI

Built entirely using **Jetpack Compose** and Android's modern **Material Design 3 (M3)** guidelines:
- **Zen Status Display**: Featuring a smooth breathing pulse animation around a tactile master status shield, emphasizing visual harmony and immediate status clarity ("Guarded" vs. "Halted").
- **Dynamic Accent Styling**: Embraces high-contrast colors, elegant typography pairing, and rich structural spacing that adapts cleanly between light and dark themes.
- **Granular Supervision Catalog**: A clean, real-time list displaying monitored packages, their progress bars matching daily time allocation, and instant delete affordances.
- **Interactive Preset Chips**: Includes quick-add templates for common productivity and social applications (e.g., social media, video streaming) along with physical custom input cards for package domains.

---

## 🚀 Core Features

- **Local Foreground Supervision Service**: Runs a battery-optimized background daemon (`AppBlockerService`) that tracks active applications using system-level usage APIs.
- **Intervention Overlays (`SYSTEM_ALERT_WINDOW`)**: Once a monitored package's limit is reached, a custom Jetpack Compose fullscreen overlay displays over the offending app, discouraging compulsive usage.
- **Device-Local Database Storage**: Persists monitored apps, daily quotas, and accurate session statistics locally on-device leveraging a **Room** database (`AppDatabase` / `SQLite`).
- **Tactile Authorization Hierarchy**: Provides a visual checklist highlighting each system permission required for the app's operation (Notifications, Usage Access, Drawing over other apps, and Battery Optimization exemption) with seamless deep link routing into android settings.
- **Developer Sandbox Tools**: Includes developer utility actions to reset daily records instantly to simplify testing and tracking setup.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin & Coroutines / Flows for safe asynchronous status propagation.
- **UI Framework**: Jetpack Compose (Material Theme 3, Animation, and Vector Graphics).
- **Architecture Pattern**: Model-View-ViewModel (MVVM) separating database state maps from responsive UI composables.
- **Data Persistence**: Room Database (`BlockedAppEntity`, `BlockedAppDao`, `BlockedAppRepository`).
- **Service Integration**: System `UsageStatsManager` tracking foreground application activity.

---

## 📋 System Requirements & Permissions

To allow background monitoring and intervention on Android devices, the application requires the following key permissions:
1. `PACKAGE_USAGE_STATS` – Needed to observe active device packages and determine screen time.
2. `SYSTEM_ALERT_WINDOW` – Required to draw the custom wellness shield overlay on top of blocked apps.
3. `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` – Ensuring continuous, unimpeded supervision in the background.
4. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` – Preventing the Android OS from pausing the monitoring daemon.
5. `POST_NOTIFICATIONS` – Showing operational alarms and foreground service status updates.

---

## 📦 Project Directory Layout

```
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── AndroidManifest.xml             # Declares components and system permissions
│   │   │   ├── java/com/example
│   │   │   │   ├── MainActivity.kt             # Main entrypoint, Compose screens & permissions flows
│   │   │   │   ├── data/                       # Room database, DAO, entities, and repositories
│   │   │   │   ├── service/                    # Foreground supervision and overlay window drawing logic
│   │   │   │   ├── ui/                         # Main ViewModel, colors, typography, Theme configuration
│   │   │   └── res/                            # Resource icons and styling definitions
│   │   └── test/                               # Robolectric Unit & Screenshot test suites
```

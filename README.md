# BetterTimer

Android app for sequential cooking timers with customizable notifications.

## Features

- Create multiple named timers
- Three notification modes:
  - **Silent**: Notification only
  - **Sound**: Notification with beep
  - **Alarm**: Full-screen alert that must be dismissed
- Timers continue running in background
- Data persists across app restarts

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Repository Pattern
- **DI**: Hilt
- **Database**: Room
- **Min SDK**: 26 (Android 8.0)

## Building

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/hazron/sequencetimer/
├── data/
│   ├── local/          # Room database & DAO
│   └── repository/     # Data repository
├── di/                 # Hilt modules
├── domain/
│   └── model/          # Domain models (Timer, NotificationType)
├── service/            # Foreground timer service
├── receiver/           # Boot receiver
└── ui/
    ├── navigation/     # Navigation graph
    ├── screens/        # UI screens (home, timer, edit, alarm)
    ├── components/     # Reusable UI components
    └── theme/          # Material theme
```

## License

Private project

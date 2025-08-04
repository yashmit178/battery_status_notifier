# Battery Percentage Notifier

A simple Android app that notifies you with an alarm and persistent notification when your device battery charge reaches a set threshold, so you can unplug the charger to preserve battery health.

![App Icon](app/src/main/res/mipmap-anydpi/ic_launcher_round.xml)

## Features

- Monitor your phone’s battery percentage in real time
- Set a user-defined battery percentage threshold (e.g., 80% or 85%)
- Receive a full-screen alarm and a notification once your battery reaches the threshold
- Notification will repeat every second until the charger is disconnected
- Easy-to-use Material design interface
- No collection or sharing of personal data

## How It Works

1. Set your desired battery percentage threshold in the app.
2. Tap **Save Threshold** to store your setting.
3. Tap **Start Monitoring** to activate battery monitoring.
4. When your phone is charging and reaches the set threshold, the app will:
    - Show a persistent notification and a full-screen alert
    - Repeat the notification every second until you unplug the charger

## Installation

- Download and install the latest APK from [Releases](https://github.com/YOUR_USERNAME/BatteryPercentageNotifier), **OR**
- Build it yourself in Android Studio (Min SDK: 30, Target SDK: 36)

## Permissions

- **POST_NOTIFICATIONS:** To send alert notifications when threshold is reached
- **FOREGROUND_SERVICE:** To monitor battery status in the background

## Privacy

This app **does not collect, store, or share any personal data**.

## License

MIT License

## Author

[Yashmit178](https://github.com/yashmit178) *(update as appropriate)*



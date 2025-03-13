# Step Counter App

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange.svg)

> A simple, accurate step counter app that uses your device's built-in step counter sensor to track your daily steps.

## Features

- Real-time step counting using device's step counter sensor
- Clean, minimalist UI built with Jetpack Compose
- Step count persists between app sessions
- Count only resets when reset button is explicitly clicked
- Battery efficient - uses hardware sensor data


## Requirements

- Android 8.0 (API level 26) or higher
- Device with step counter sensor
- ACTIVITY_RECOGNITION permission

## Technology Stack

- Kotlin
- Jetpack Compose for UI
- Android Sensor Framework
- SharedPreferences for data persistence

## Setup and Installation

1. Clone this repository
   ```
   git clone https://github.com/codemhax/stepcounter.git
   ```

2. Open the project in Android Studio

3. Build and run the application on your device
   
Note: The app requires a physical device with a step counter sensor. Most emulators do not support this functionality.

## How to Use

1. Launch the app
2. Grant the necessary permissions when prompted
3. Press "Start" to begin counting steps
4. The app will continue counting even when closed
5. Press "Stop" to pause counting
6. Press "Reset" to set the counter back to zero


## Acknowledgements

- Android Jetpack Compose documentation
- Android Sensor API documentation
- [Google Accompanist Permissions library](https://github.com/google/accompanist)
```

Built with ❤️ by CodemHax
```

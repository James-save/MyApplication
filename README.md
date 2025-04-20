# RobotCarArm Project

This project includes an Android application and ESP32 code for controlling a robot car with an arm. The Android app connects to the ESP32 via Bluetooth to send movement commands and via Wi-Fi to display a live MJPEG stream from the ESP32-CAM and monitor temperature and smoke data.

## Structure
- `android/`: Android application (built with Android Studio)
- `esp32/`: ESP32 code (built with Arduino IDE)

## Setup
1. Update Wi-Fi credentials in `esp32/RobotCarArm.ino`.
2. Update the ESP32 IP address in `android/app/src/main/java/com/example/myapplication/MainActive.java`.
3. Upload the ESP32 code to your ESP32-CAM board.
4. Build and run the Android app on your device.

## Features
- Control robot car movement (forward, backward, left, right) via Bluetooth.
- Display live video stream from ESP32-CAM using WebView.
- Monitor temperature and smoke data via HTTP requests.

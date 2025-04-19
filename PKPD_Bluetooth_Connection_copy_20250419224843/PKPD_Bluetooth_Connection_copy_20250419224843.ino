#include <WiFi.h>
#include <FirebaseESP32.h>
#include "BluetoothSerial.h"

// Firebase configuration
#define FIREBASE_HOST "your-project.firebaseio.com"
#define FIREBASE_AUTH "your-firebase-database-secret"

// WiFi credentials
const char* ssid = "Your_SSID";
const char* password = "Your_PASSWORD";

BluetoothSerial SerialBT;  // Bluetooth object
FirebaseData firebaseData; // Firebase object

void setup() {
  Serial.begin(115200);               // Initialize Serial for debugging
  SerialBT.begin("ESP32_BT_Device");  // Start Bluetooth service with the device name
  Serial.println("Bluetooth device started. Ready for pairing!");

  // Configure LED pin
  pinMode(13, OUTPUT);
  digitalWrite(13, LOW);              // Ensure LED starts OFF

  // Connect to WiFi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");

  // Connect to Firebase
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.reconnectWiFi(true);
  Serial.println("Connected to Firebase");
}

void loop() {
  // Check Bluetooth connection
  if (SerialBT.hasClient()) {
    Serial.println("Client connected!");
  } else {
    Serial.println("Waiting for client...");
    delay(2000);
  }

  // Handling incoming Bluetooth data
  if (SerialBT.available()) {
    String command = SerialBT.readString();
    Serial.println("Received via Bluetooth: " + command);

    if (command == "ON") {
      Serial.println("Turning LED ON");
      digitalWrite(13, HIGH); // Turn LED on
      SerialBT.println("LED is ON");

      // Update Firebase
      Firebase.setString(firebaseData, "/device/command", "LED_ON");
    } else if (command == "OFF") {
      Serial.println("Turning LED OFF");
      digitalWrite(13, LOW); // Turn LED off
      SerialBT.println("LED is OFF");

      // Update Firebase
      Firebase.setString(firebaseData, "/device/command", "LED_OFF");
    } else {
      Serial.println("Unknown command received: " + command);
      SerialBT.println("Error: Unknown command received");
    }
  }

  // Retrieve data from Firebase and send response via Bluetooth
  if (Firebase.getString(firebaseData, "/device/response")) {
    String response = firebaseData.stringData();
    Serial.println("Response from Firebase: " + response);
    SerialBT.println(response);
  } else {
    Serial.println("Failed to read response from Firebase");
  }

  // Send periodic update via Bluetooth
  SerialBT.println("ESP32 is active!");
  delay(5000);  // Wait for 5 seconds before repeating
}

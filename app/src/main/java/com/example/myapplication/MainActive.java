package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

public class MainActive extends AppCompatActivity {

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String ESP32_NAME = "RobotCarArm";
    private static final String ESP32_IP = "192.168.1.xxx";  // 改成你 ESP32 嘅 IP（喺 Serial Monitor 睇）

    private ImageView leftCircleIcon, rightCircleIcon;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // 初始化 UI
        leftCircleIcon = findViewById(R.id.leftCircleIcon);
        rightCircleIcon = findViewById(R.id.rightCircleIcon);
        Button buttonUp = findViewById(R.id.buttonUp);
        Button buttonDown = findViewById(R.id.buttonDown);
        Button buttonLeft = findViewById(R.id.buttonLeft);
        Button buttonRight = findViewById(R.id.buttonRight);

        // Event handlers
        ButtonHandler buttonHandler = new ButtonHandler();

        // Bluetooth 設定
        setupBluetooth();

        // Wi-Fi 數據監控
        startWiFiMonitoring();

        // 按鈕事件
        buttonUp.setOnClickListener(v -> {
            buttonHandler.handleUp();
            sendBluetoothCommand('U');
        });
        buttonDown.setOnClickListener(v -> {
            buttonHandler.handleDown();
            sendBluetoothCommand('D');
        });
        buttonLeft.setOnClickListener(v -> {
            buttonHandler.handleLeft();
            sendBluetoothCommand('L');
        });
        buttonRight.setOnClickListener(v -> {
            buttonHandler.handleRight();
            sendBluetoothCommand('R');
        });
    }

    private void setupBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) return;

        if (!bluetoothAdapter.isEnabled()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 1);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice esp32Device = null;
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(ESP32_NAME)) {
                esp32Device = device;
                break;
            }
        }

        if (esp32Device == null) return;

        try {
            bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBluetoothCommand(char command) {
        if (outputStream != null) {
            try {
                outputStream.write(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startWiFiMonitoring() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        URL url = new URL("http://" + ESP32_IP + "/data");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len = is.read(buffer);
                        String json = new String(buffer, 0, len);
                        JSONObject data = new JSONObject(json);
                        float temp = (float) data.getDouble("temperature");
                        float smoke = (float) data.getDouble("smoke");

                        runOnUiThread(() -> updateIcons(temp, smoke));
                        is.close();
                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateIcons(float temp, float smoke) {
        if (temp > 50.0f) {
            leftCircleIcon.setColorFilter(android.graphics.Color.RED);
        } else {
            leftCircleIcon.setColorFilter(android.graphics.Color.GREEN);
        }
        if (smoke > 0.5f) {
            rightCircleIcon.setColorFilter(android.graphics.Color.YELLOW);
        } else {
            rightCircleIcon.setColorFilter(android.graphics.Color.BLUE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
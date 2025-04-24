package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

public class MainActive extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String ESP32_NAME = "RobotCarArm";
    private static final String ESP32_IP = "192.168.1.100"; // 換成你嘅 ESP32 IP
    private CarControlFragment carControlFragment;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable wifiMonitoringRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isNetworkAvailable()) {
                Toast.makeText(MainActive.this, "請連繫網絡以監控數據", Toast.LENGTH_LONG).show();
                handler.postDelayed(this, 1000);
                return;
            }
            new Thread(() -> {
                try {
                    URL url = new URL("http://" + ESP32_IP + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len = is.read(buffer);
                        if (len > 0) {
                            String json = new String(buffer, 0, len);
                            JSONObject data = new JSONObject(json);
                            float temp = (float) data.getDouble("temperature");
                            float smoke = (float) data.getDouble("smoke");

                            runOnUiThread(() -> {
                                if (carControlFragment != null && carControlFragment.getTempSmokeHandler() != null) {
                                    carControlFragment.getTempSmokeHandler().updateIcons(temp, smoke);
                                }
                            });
                        } else {
                            Log.e("WiFiMonitoring", "無數據返回");
                        }
                        is.close();
                    } else {
                        Log.e("WiFiMonitoring", "HTTP 錯誤: " + responseCode);
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActive.this, "獲取數據失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    Log.e("WiFiMonitoring", "獲取數據失敗", e);
                }
                handler.postDelayed(this, 1000);
            }).start();
        }
    };

    private final Runnable heatmapUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isNetworkAvailable()) {
                handler.postDelayed(this, 1000);
                return;
            }
            new Thread(() -> {
                try {
                    URL url = new URL("http://" + ESP32_IP + "/heatmap");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = conn.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        runOnUiThread(() -> {
                            if (carControlFragment != null) {
                                ImageView heatmapView = carControlFragment.getHeatmapView();
                                if (heatmapView != null) {
                                    heatmapView.setImageBitmap(bitmap);
                                }
                            }
                        });
                        is.close();
                    } else {
                        Log.e("HeatmapUpdate", "HTTP 錯誤: " + responseCode);
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e("HeatmapUpdate", "獲取熱成像圖失敗", e);
                }
                handler.postDelayed(this, 1000); // 每秒更新一次
            }).start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // 初始化 ViewPager2 同 TabLayout
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        setupViewPager();

        // 初始化藍牙
        setupBluetooth();

        // 開始 Wi-Fi 數據監控同熱成像圖更新
        startWiFiMonitoring();
        startHeatmapUpdate();
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2; // 兩個頁面
            }

            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    carControlFragment = new CarControlFragment();
                    return carControlFragment;
                } else {
                    return new ArmControlFragment();
                }
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Car Control");
            } else {
                tab.setText("Arm Control");
            }
        }).attach();
    }

    private void setupBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "設備唔支援藍牙", Toast.LENGTH_LONG).show();
            Log.e("Bluetooth", "設備唔支援藍牙");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "請開啟藍牙", Toast.LENGTH_LONG).show();
            Log.e("Bluetooth", "藍牙未開啟");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
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

        if (esp32Device == null) {
            Toast.makeText(this, "搵唔到 ESP32 設備 (RobotCarArm)", Toast.LENGTH_LONG).show();
            Log.e("Bluetooth", "搵唔到 ESP32 設備 (RobotCarArm)");
            return;
        }

        try {
            bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "藍牙連繫成功", Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth", "藍牙連繫成功: " + esp32Device.getName());
        } catch (IOException e) {
            Toast.makeText(this, "藍牙連繫失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Bluetooth", "藍牙連繫失敗", e);
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException closeException) {
                Log.e("Bluetooth", "關閉 socket 失敗", closeException);
            }
        }
    }

    public void sendBluetoothCommand(char command) {
        if (bluetoothSocket == null || !bluetoothSocket.isConnected() || outputStream == null) {
            Toast.makeText(this, "藍牙未連繫，請檢查 ESP32", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            outputStream.write((command + "\n").getBytes());
            Toast.makeText(this, "指令已發送: " + command, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "發送指令失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Bluetooth", "發送指令失敗", e);
        }
    }

    public void setupWebView(WebView webView) {
        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    Toast.makeText(MainActive.this, "無法加載 ESP32-CAM 影像: " + error.getDescription(), Toast.LENGTH_LONG).show();
                }
            });
            webView.loadUrl("http://" + ESP32_IP + "/stream");
        } else {
            Toast.makeText(this, "無法顯示 ESP32-CAM 影像", Toast.LENGTH_LONG).show();
        }
    }

    public void setupHeatmap(ImageView imageView) {
        // 熱成像圖嘅初始化（如果需要）
    }

    private void startWiFiMonitoring() {
        handler.postDelayed(wifiMonitoringRunnable, 1000);
    }

    private void startHeatmapUpdate() {
        handler.postDelayed(heatmapUpdateRunnable, 1000);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupBluetooth();
            } else {
                Toast.makeText(this, "需要藍牙權限以連繫 ESP32", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(wifiMonitoringRunnable);
        handler.removeCallbacks(heatmapUpdateRunnable);
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Toast.makeText(this, "關閉藍牙連繫失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("Bluetooth", "關閉藍牙連繫失敗", e);
        }
    }
}
package com.example.myapplication;

import android.graphics.Color;
import android.os.Handler;
import android.widget.ImageView;

public class TemperatureAndSmokeHandler {

    private final ImageView leftCircleIcon;
    private final ImageView rightCircleIcon;

    
    private float currentTemperature = 25.0f;
    private float smokeLevel = 0.1f;


    private static final float TEMPERATURE_THRESHOLD = 50.0f;
    private static final float SMOKE_THRESHOLD = 0.5f;

    public TemperatureAndSmokeHandler(ImageView leftCircleIcon, ImageView rightCircleIcon) {
        this.leftCircleIcon = leftCircleIcon;
        this.rightCircleIcon = rightCircleIcon;
    }

    // monitor data
    public void startMonitoring() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                currentTemperature += 1.0f;
                smokeLevel += 0.05f;


                updateIcons();


                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateIcons() {

        if (currentTemperature > TEMPERATURE_THRESHOLD) {
            leftCircleIcon.setColorFilter(Color.RED);
        } else {
            leftCircleIcon.setColorFilter(Color.GREEN);
        }


        if (smokeLevel > SMOKE_THRESHOLD) {
            rightCircleIcon.setColorFilter(Color.YELLOW);
        } else {
            rightCircleIcon.setColorFilter(Color.BLUE);
        }
    }
}
package com.example.myapplication;

import android.widget.ImageView;

public class TemperatureAndSmokeHandler {

    private final ImageView leftCircleIcon;
    private final ImageView rightCircleIcon;

    private static final float TEMPERATURE_THRESHOLD = 50.0f;
    private static final float SMOKE_THRESHOLD = 0.5f;

    public TemperatureAndSmokeHandler(ImageView leftCircleIcon, ImageView rightCircleIcon) {
        this.leftCircleIcon = leftCircleIcon;
        this.rightCircleIcon = rightCircleIcon;
    }

    public void updateIcons(float temperature, float smokeLevel) {
        if (leftCircleIcon == null || rightCircleIcon == null) {
            return; // 避免 NullPointerException
        }
        if (temperature > TEMPERATURE_THRESHOLD) {
            leftCircleIcon.setImageResource(R.drawable.circle_red);
        } else {
            leftCircleIcon.setImageResource(R.drawable.circle_green);
        }
        if (smokeLevel > SMOKE_THRESHOLD) {
            rightCircleIcon.setImageResource(R.drawable.circle_yellow);
        } else {
            rightCircleIcon.setImageResource(R.drawable.circle_blue);
        }
    }
}
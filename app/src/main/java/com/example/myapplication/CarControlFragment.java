package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;

public class CarControlFragment extends Fragment {
    private ImageView imageViewHeatmap;
    private Button buttonUp, buttonDown, buttonLeft, buttonRight;
    private ImageView leftCircleIcon, rightCircleIcon;
    private TemperatureAndSmokeHandler tempSmokeHandler;

    public CarControlFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_car_control, container, false);

        // 初始化熱成像圖 ImageView
        imageViewHeatmap = view.findViewById(R.id.imageViewHeatmap);
        MainActive activity = (MainActive) getActivity();
        if (activity != null) {
            activity.setupHeatmap(imageViewHeatmap);
        }

        // 初始化圖標
        leftCircleIcon = view.findViewById(R.id.leftCircleIcon);
        rightCircleIcon = view.findViewById(R.id.rightCircleIcon);
        tempSmokeHandler = new TemperatureAndSmokeHandler(leftCircleIcon, rightCircleIcon);

        // 初始化按鈕
        buttonUp = view.findViewById(R.id.buttonUp);
        buttonDown = view.findViewById(R.id.buttonDown);
        buttonLeft = view.findViewById(R.id.buttonLeft);
        buttonRight = view.findViewById(R.id.buttonRight);

        // 設置按鈕事件
        buttonUp.setOnClickListener(v -> sendCommand('U'));
        buttonDown.setOnClickListener(v -> sendCommand('D'));
        buttonLeft.setOnClickListener(v -> sendCommand('L'));
        buttonRight.setOnClickListener(v -> sendCommand('R'));

        return view;
    }

    private void sendCommand(char command) {
        MainActive activity = (MainActive) getActivity();
        if (activity != null) {
            activity.sendBluetoothCommand(command);
        }
    }

    public TemperatureAndSmokeHandler getTempSmokeHandler() {
        return tempSmokeHandler;
    }

    public ImageView getHeatmapView() {
        return imageViewHeatmap;
    }
}
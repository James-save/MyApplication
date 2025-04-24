package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;

public class ArmControlFragment extends Fragment {

    public ArmControlFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arm_control, container, false);

        // 初始化按鈕
        Button buttonArmUp = view.findViewById(R.id.buttonArmUp);
        Button buttonArmDown = view.findViewById(R.id.buttonArmDown);
        Button buttonArmLeft = view.findViewById(R.id.buttonArmLeft);
        Button buttonArmRight = view.findViewById(R.id.buttonArmRight);

        // 設置按鈕事件
        buttonArmUp.setOnClickListener(v -> sendCommand('A')); // 手臂上升
        buttonArmDown.setOnClickListener(v -> sendCommand('B')); // 手臂下降
        buttonArmLeft.setOnClickListener(v -> sendCommand('C')); // 手臂左旋
        buttonArmRight.setOnClickListener(v -> sendCommand('D')); // 手臂右旋

        return view;
    }

    private void sendCommand(char command) {
        MainActive activity = (MainActive) getActivity();
        if (activity != null) {
            activity.sendBluetoothCommand(command);
        }
    }
}
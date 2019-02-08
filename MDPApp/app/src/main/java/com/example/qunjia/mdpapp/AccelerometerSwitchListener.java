package com.example.qunjia.mdpapp;

import android.app.Activity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.andretietz.android.controller.DirectionView;

public class AccelerometerSwitchListener implements Switch.OnCheckedChangeListener {
    private OrientationEventListener orientationEventListener = null;
    private static int currentOrientation;
    @Override
    public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
        if(orientationEventListener == null){
            orientationEventListener = new OrientationEventListener(compoundButton.getContext()) {
                @Override
                public void onOrientationChanged(int orientation) {
                    final int range = 20;
                    final int down = 0, down_2 = 360;
                    final int down_right = 45;
                    final int right = 90;
                    final int up_right = 135;
                    final int up = 180;
                    final int up_left = 225;
                    final int left = 270;
                    final int down_left = 315;

                    TextView statusWindow = ((Activity) compoundButton.getContext()).findViewById(R.id.statusWindowTV);
                    String currentText = statusWindow.getText().toString();
                    ScrollView scrollView = ((Activity) compoundButton.getContext()).findViewById(R.id.scrollView);

                    if((orientation < down + range || orientation > down_2 - range)&& currentOrientation != down){ 
                        statusWindow.setText(currentText + "\nDOWN");
                        BluetoothFragment.sendMessage("DOWN");
                        currentOrientation = down;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    else if(orientation > left - range && orientation < left +range&& currentOrientation != left){
                       statusWindow.setText(currentText + "\nLEFT");
                        BluetoothFragment.sendMessage("LEFT");
                        currentOrientation = left;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    else if(orientation > right - range && orientation < right +range&& currentOrientation != right){
                       statusWindow.setText(currentText + "\nRIGHT");
                        BluetoothFragment.sendMessage("RIGHT");
                        currentOrientation = right;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    else if(orientation > up - range && orientation < up +range&& currentOrientation != up){
                       statusWindow.setText(currentText + "\nUP");
                        BluetoothFragment.sendMessage("UP");
                        currentOrientation = up;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    else if(orientation > down_left - range && orientation < down_left +range&& currentOrientation != down_left){
                       statusWindow.setText(currentText + "\nDOWN_LEFT");
                        BluetoothFragment.sendMessage("DOWN_LEFT");
                        currentOrientation = down_left;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    else if(orientation > up_left - range && orientation < up_left +range && currentOrientation != up_left){
                       statusWindow.setText(currentText + "\nUP_LEFT");
                        BluetoothFragment.sendMessage("UP_LEFT");
                        currentOrientation = up_left;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    else if(orientation > down_right - range && orientation < down_right +range && currentOrientation != down_right){
                       statusWindow.setText(currentText + "\nDOWN_RIGHT");
                        BluetoothFragment.sendMessage("DOWN_RIGHT");
                        currentOrientation = down_right;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    else if(orientation > up_right - range && orientation < up_right +range&& currentOrientation != up_right){
                       statusWindow.setText(currentText + "\nUP_RIGHT");
                        BluetoothFragment.sendMessage("UP_RIGHT");
                        currentOrientation = up_right;
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                }
            };
        }

        if(compoundButton.isChecked()){
            orientationEventListener.enable();
        }
        else {
            orientationEventListener.disable();
            orientationEventListener = null;
        }

    }
}

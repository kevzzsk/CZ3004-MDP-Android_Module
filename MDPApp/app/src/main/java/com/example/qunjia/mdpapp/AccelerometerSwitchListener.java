package com.example.qunjia.mdpapp;

import android.view.OrientationEventListener;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.andretietz.android.controller.DirectionView;

public class AccelerometerSwitchListener implements Switch.OnCheckedChangeListener {
    private OrientationEventListener orientationEventListener = null;
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

                    if(orientation < down + range || orientation > down_2 - range){
                        Toast.makeText(compoundButton.getContext(), "DOWN", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("DOWN");
                    }
                    else if(orientation > left - range && orientation < left +range){
                        Toast.makeText(compoundButton.getContext(), "LEFT", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("LEFT");
                    }
                    else if(orientation > right - range && orientation < right +range){
                        Toast.makeText(compoundButton.getContext(), "RIGHT", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("RIGHT");
                    }
                    else if(orientation > up - range && orientation < up +range){
                        Toast.makeText(compoundButton.getContext(), "UP", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("UP");
                    }
                    else if(orientation > down_left - range && orientation < down_left +range){
                        Toast.makeText(compoundButton.getContext(), "DOWN_LEFT", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("DOWN_LEFT");
                    }
                    else if(orientation > up_left - range && orientation < up_left +range){
                        Toast.makeText(compoundButton.getContext(), "UP_LEFT", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("UP_LEFT");
                    }
                    else if(orientation > down_right - range && orientation < down_right +range){
                        Toast.makeText(compoundButton.getContext(), "DOWN_RIGHT", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("DOWN_RIGHT");
                    }
                    else if(orientation > up_right - range && orientation < up_right +range){
                        Toast.makeText(compoundButton.getContext(), "UP_RIGHT", Toast.LENGTH_LONG).show();
                        BluetoothFragment.sendMessage("UP_RIGHT");
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

package com.example.qunjia.mdpapp;

import android.app.Activity;
import android.view.OrientationEventListener;
import android.widget.CompoundButton;
import android.widget.Switch;

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
                    

                    if((orientation < down + range || orientation > down_2 - range)&& currentOrientation != down){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_DOWN);
                        currentOrientation =down;
                    }
                    else if(orientation > left - range && orientation < left +range&& currentOrientation != left){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_LEFT);
                        currentOrientation = left;
                    }
                    else if(orientation > right - range && orientation < right +range&& currentOrientation != right){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_RIGHT);
                        currentOrientation = right;
                    }
                    else if(orientation > up - range && orientation < up +range&& currentOrientation != up){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_UP);
                        currentOrientation = up;
                    }
                    else if(orientation > down_left - range && orientation < down_left +range&& currentOrientation != down_left){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_DOWN_LEFT);
                        currentOrientation = down_left;
                    }
                    else if(orientation > up_left - range && orientation < up_left +range && currentOrientation != up_left){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_UP_LEFT);
                        currentOrientation = up_left;
                    }
                    else if(orientation > down_right - range && orientation < down_right +range && currentOrientation != down_right){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_DOWN_RIGHT);
                        currentOrientation = down_right;
                    }
                    else if(orientation > up_right - range && orientation < up_right +range&& currentOrientation != up_right){
                        GridMapFragment.MoveRobot(compoundButton.getContext(), GridMapFragment.MOVE_UP_RIGHT);
                        currentOrientation = up_right;
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

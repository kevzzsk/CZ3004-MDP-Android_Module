package com.example.qunjia.mdpapp;

import android.app.Activity;
import android.content.Context;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.andretietz.android.controller.DirectionView;

import java.util.logging.Handler;

public class AccelerometerSwitchListener implements Switch.OnCheckedChangeListener {

    private OrientationEventListener orientationEventListener = null;
    private static int currentOrientation;
    private android.os.Handler handler = null;
    private Runnable myRunnable;


    @Override
    public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
        if (orientationEventListener == null) {
            orientationEventListener = new OrientationEventListener(compoundButton.getContext()) {
                @Override
                public void onOrientationChanged(int orientation) {
                    final int range = 20;
                    final int down = 0, down_2 = 360;
                    final int right = 90;
                    final int up = 180;
                    final int left = 270;

                    if ((orientation < down + range || orientation > down_2 - range) && currentOrientation != down) {
                        AccelerometerMoveRobotHandler(compoundButton.getContext(),GridMapFragment.MOVE_DOWN);
                        currentOrientation = down;
                        SetVisibilityGone(((Activity) compoundButton.getContext()), GridMapFragment.MOVE_DOWN);
                    } else if (orientation > left - range && orientation < left + range && currentOrientation != left) {
                        AccelerometerMoveRobotHandler(compoundButton.getContext(),GridMapFragment.MOVE_LEFT);
                        currentOrientation = left;
                        SetVisibilityGone(((Activity) compoundButton.getContext()), GridMapFragment.MOVE_LEFT);
                    } else if (orientation > right - range && orientation < right + range && currentOrientation != right) {
                        AccelerometerMoveRobotHandler(compoundButton.getContext(),GridMapFragment.MOVE_RIGHT);
                        currentOrientation = right;
                        SetVisibilityGone(((Activity) compoundButton.getContext()), GridMapFragment.MOVE_RIGHT);
                    } else if (orientation > up - range && orientation < up + range && currentOrientation != up) {
                        AccelerometerMoveRobotHandler(compoundButton.getContext(),GridMapFragment.MOVE_UP);
                        currentOrientation = up;
                        SetVisibilityGone(((Activity) compoundButton.getContext()), GridMapFragment.MOVE_UP);
                    } else {
                        //directionViewDisabled.setVisibility(View.VISIBLE);
                    }
                }
            };
        }

        if (compoundButton.isChecked()) {
            orientationEventListener.enable();
            GridMapFragment.DirectionViewSetEnabled(((Activity) compoundButton.getContext()), false);
        } else {
            orientationEventListener.disable();
            orientationEventListener = null;
            SetVisibilityGone(((Activity) compoundButton.getContext()), 999);//set all visibility gone
            GridMapFragment.DirectionViewSetEnabled(((Activity) compoundButton.getContext()), true);

            if (handler != null) {
                handler.removeCallbacks(myRunnable);
                handler.removeMessages(0);
            }
        }
    }
    
    private void AccelerometerMoveRobotHandler(final Context context, final int direction){
        final int delay = 300; //milliseconds

        if (handler != null) {
            handler.removeCallbacks(myRunnable);
            handler.removeMessages(0);
        }

        handler = new android.os.Handler();

        myRunnable = new Runnable() {
            public void run(){
                GridMapFragment.MoveRobot(context,direction);
                        handler.postDelayed(this, delay);
            }
        };

        handler.postDelayed(myRunnable, delay);
    }
    
    private void SetVisibilityGone(Activity activity, int DirectionVisible){
        //DirectionView directionViewDisabled = activity.findViewById(R.id.viewDirectionDisabled);
        DirectionView directionViewDisabledUp = activity.findViewById(R.id.viewDirectionDisabledUp);
        DirectionView directionViewDisabledDown = activity.findViewById(R.id.viewDirectionDisabledDown);
        DirectionView directionViewDisabledLeft = activity.findViewById(R.id.viewDirectionDisabledLeft);
        DirectionView directionViewDisabledRight = activity.findViewById(R.id.viewDirectionDisabledRight);

        //directionViewDisabled.setVisibility(View.GONE);
        directionViewDisabledUp.setVisibility(View.GONE);
        directionViewDisabledDown.setVisibility(View.GONE);
        directionViewDisabledLeft.setVisibility(View.GONE);
        directionViewDisabledRight.setVisibility(View.GONE);

        switch (DirectionVisible){
            case GridMapFragment.MOVE_DOWN:
                directionViewDisabledDown.setVisibility(View.VISIBLE);
                break;
            case GridMapFragment.MOVE_UP:
                directionViewDisabledUp.setVisibility(View.VISIBLE);
                break;
            case GridMapFragment.MOVE_LEFT:
                directionViewDisabledLeft.setVisibility(View.VISIBLE);
                break;
            case GridMapFragment.MOVE_RIGHT:
                directionViewDisabledRight.setVisibility(View.VISIBLE);
                break;
            default://set all gone
                    break;
        }
    }


}

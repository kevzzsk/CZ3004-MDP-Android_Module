package com.example.kevzzsk.mdpapp.Handler;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.andretietz.android.controller.DirectionView;
import com.example.kevzzsk.mdpapp.Fragment.GridMapFragment;
import com.example.kevzzsk.mdpapp.R;

public class AccelerometerSwitchListener implements Switch.OnCheckedChangeListener {

    private android.os.Handler handler = null;
    private Runnable myRunnable;

    //sensor variables
    private SensorManager mSensorManager;
    private SensorListener sensorListener;
    private int currentOrientation;

    @Override
    public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {

        if (compoundButton.isChecked()) {
            sensorListener = new SensorListener((Activity) compoundButton.getContext());
            GridMapFragment.directionViewSetEnabled(((Activity) compoundButton.getContext()), false);
        } else {
            sensorListener.disable();
            sensorListener = null;
            setVisibilityGone(((Activity) compoundButton.getContext()), 999);//set all visibility gone
            GridMapFragment.directionViewSetEnabled(((Activity) compoundButton.getContext()), true);
            GridMapFragment.directionViewSetEnabled(((Activity) compoundButton.getContext()), true);

            if (handler != null) {
                handler.removeCallbacks(myRunnable);
                handler.removeMessages(0);
            }
        }
    }

    private void accelerometerMoveRobotHandler(final Context context, final int direction) {
        final int delay = 500; //milliseconds

        if (handler != null) {
            handler.removeCallbacks(myRunnable);
            handler.removeMessages(0);
        }

        handler = new android.os.Handler();

        myRunnable = new Runnable() {
            public void run() {
                GridMapFragment.moveRobot(context, direction);
                handler.postDelayed(this, delay);
            }
        };

        handler.postDelayed(myRunnable, delay);
    }

    private void setVisibilityGone(Activity activity, int DirectionVisible) {
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

        switch (DirectionVisible) {
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
            case GridMapFragment.MOVE_NONE://set all gone
                break;
        }
    }

    private class SensorListener implements SensorEventListener {
        private Activity activity;

        SensorListener(Activity activity) {
            this.activity = activity;
            mSensorManager = (SensorManager) activity.getSystemService(activity.SENSOR_SERVICE);
            Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        void disable(){
            mSensorManager.unregisterListener(this);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] g = new float[3];
            g = event.values.clone();

            float norm_Of_g = (float) Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);

            // Normalize the accelerometer vector
            g[0] = g[0] / norm_Of_g;
            g[1] = g[1] / norm_Of_g;
            g[2] = g[2] / norm_Of_g;
            int inclination = (int) Math.round(Math.toDegrees(Math.acos(g[2])));
            int orientationX = (int) Math.round(Math.toDegrees(Math.acos(g[0])));
            int orientationY = (int) Math.round(Math.toDegrees(Math.acos(g[1])));
            final int range = 60;

            if ((inclination < 25 || inclination > 155)&&
                    currentOrientation != GridMapFragment.MOVE_NONE) {

                accelerometerMoveRobotHandler(activity, GridMapFragment.MOVE_NONE);
                currentOrientation = GridMapFragment.MOVE_NONE;
                setVisibilityGone(activity, GridMapFragment.MOVE_NONE);
            } else if ((orientationX < 90 + range && orientationX > 90 - range) &&
                    (orientationY < 0 + range && orientationY > 0 - range) &&
                    currentOrientation != GridMapFragment.MOVE_DOWN) {

                accelerometerMoveRobotHandler(activity, GridMapFragment.MOVE_DOWN);
                currentOrientation = GridMapFragment.MOVE_DOWN;
                setVisibilityGone(activity, GridMapFragment.MOVE_DOWN);
            }
            else if ((orientationX < 90 + range && orientationX > 90 - range) &&
                    (orientationY < 180 + range && orientationY > 180 - range)&&
                    currentOrientation != GridMapFragment.MOVE_UP) {

                accelerometerMoveRobotHandler(activity, GridMapFragment.MOVE_UP);
                currentOrientation = GridMapFragment.MOVE_UP;
                setVisibilityGone(activity, GridMapFragment.MOVE_UP);
            }
            else if ((orientationX < 0 + range && orientationX > 0 - range) &&
                    (orientationY < 90 + range && orientationY > 90 - range)&&
                    currentOrientation != GridMapFragment.MOVE_LEFT) {

                accelerometerMoveRobotHandler(activity, GridMapFragment.MOVE_LEFT);
                currentOrientation = GridMapFragment.MOVE_LEFT;
                setVisibilityGone(activity, GridMapFragment.MOVE_LEFT);
            }
            else if ((orientationX < 180 + range && orientationX > 180 - range) &&
                    (orientationY < 90 + range && orientationY > 90 - range)&&
                    currentOrientation != GridMapFragment.MOVE_RIGHT) {

                accelerometerMoveRobotHandler(activity, GridMapFragment.MOVE_RIGHT);
                currentOrientation = GridMapFragment.MOVE_RIGHT;
                setVisibilityGone(activity, GridMapFragment.MOVE_RIGHT);
            }
        }
    }

}

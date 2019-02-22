package com.example.qunjia.mdpapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.andretietz.android.controller.DirectionView;
import com.andretietz.android.controller.InputView;

import java.io.InputStream;

public class GridMapFragment extends Fragment {

    public static final int MOVE_NONE = 0, MOVE_UP = 1, MOVE_DOWN = 2,
            MOVE_LEFT = 3, MOVE_RIGHT = 4;


    public static GridMapFragment newInstance(int position) {
        GridMapFragment f = new GridMapFragment();
        Bundle b = new Bundle();
        b.putInt("position", position);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_grid_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InitSwitchListener();
        GridMapHandler.CreateGridMap(getActivity());
        DirectionViewSetup(getActivity());
        InitWaypointToggleBtnListener();
        InitAutoManualToggleBtnListener();
        //
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                GridMapHandler.SetRobotPosition(getActivity(), 1, 18);
                GridMapHandler.SetArrowPicture(getActivity(), 0, 5, 5);
                GridMapHandler.ChangeCellColor(getActivity(), Color.BLACK, 5, 6);
                GridMapHandler.ChangeCellColor(getActivity(), Color.BLACK, 5, 7);
                GridMapHandler.ChangeCellColor(getActivity(), Color.BLACK, 5, 8);
                GridMapHandler.ChangeCellColor(getActivity(), Color.BLACK, 6, 8);
                GridMapHandler.ChangeCellColor(getActivity(), Color.BLACK, 7, 8);
                GridMapHandler.ChangeCellColor(getActivity(), Color.BLACK, 8, 8);
                GridMapHandler.SetArrowPicture(getActivity(), 90, 9, 8);
            }
        }, 200);
    }

    private void InitSwitchListener() {
        Switch directionSwitch = (Switch) getActivity().findViewById(R.id.directionToggleBtn);
        directionSwitch.setOnCheckedChangeListener(new AccelerometerSwitchListener());
    }

    private void DirectionViewSetup(final Context context) {
        DirectionView directionView = (DirectionView) ((Activity) context).findViewById(R.id.viewDirection);
        directionView.setOnButtonListener(new InputView.InputEventListener() {
            @Override
            public void onInputEvent(View view, int buttons) {
                switch (buttons & 0xff) {
                    case DirectionView.DIRECTION_DOWN:
                        MoveRobot(context, MOVE_DOWN);
                        break;
                    case DirectionView.DIRECTION_LEFT:
                        MoveRobot(context, MOVE_LEFT);
                        break;
                    case DirectionView.DIRECTION_RIGHT:
                        MoveRobot(context, MOVE_RIGHT);
                        break;
                    case DirectionView.DIRECTION_UP:
                        MoveRobot(context, MOVE_UP);
                        break;
                }
            }
        });
    }

    private void InitWaypointToggleBtnListener() {
        ToggleButton toggleButton = getActivity().findViewById(R.id.waypointToggleBtn);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Activity activity = (Activity) compoundButton.getContext();
                ToggleButton autoManualToggleBtn = activity.findViewById(R.id.autoManualToggleBtn);
                Switch accelerometerSwitch = activity.findViewById(R.id.directionToggleBtn);
                Button updateBtn = activity.findViewById(R.id.updateBtn);
                Button exploreBtn = activity.findViewById(R.id.exploreBtn);
                Button fastestBtn = activity.findViewById(R.id.fastestBtn);
                Button stopBtn = activity.findViewById(R.id.stopBtn);

                if (compoundButton.isChecked()) {
                    GridMapHandler.SetRobotDragListener(compoundButton.getContext(), true);
                    DirectionViewSetEnabled(activity, false);

                    autoManualToggleBtn.setEnabled(false);
                    accelerometerSwitch.setEnabled(false);
                    updateBtn.setEnabled(false);
                    exploreBtn.setEnabled(false);
                    fastestBtn.setEnabled(false);
                    stopBtn.setEnabled(false);

                } else {
                    GridMapHandler.SetRobotDragListener(compoundButton.getContext(), false);
                    DirectionViewSetEnabled(activity, true);

                    autoManualToggleBtn.setEnabled(true);
                    accelerometerSwitch.setEnabled(true);
                    updateBtn.setEnabled(true);
                    exploreBtn.setEnabled(true);
                    fastestBtn.setEnabled(true);
                    stopBtn.setEnabled(true);
                }
            }
        });
    }

    private void InitAutoManualToggleBtnListener() {
        ToggleButton toggleButton = getActivity().findViewById(R.id.autoManualToggleBtn);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Activity activity = (Activity) compoundButton.getContext();
                Button updateBtn = activity.findViewById(R.id.updateBtn);

                if (compoundButton.isChecked()) {
                    updateBtn.setEnabled(true);
                } else {
                    updateBtn.setEnabled(false);
                }
            }
        });
    }

    public static void DirectionViewSetEnabled(Activity activity, Boolean enabled) {
        DirectionView directionView = activity.findViewById(R.id.viewDirection);
        DirectionView directionViewDisabled = activity.findViewById(R.id.viewDirectionDisabled);
        View directionViewDisabledBlocker = activity.findViewById(R.id.viewDirectionDisabledBlocker);
        if (enabled) {
            directionView.setVisibility(View.VISIBLE);
            directionViewDisabled.setVisibility(View.GONE);
            directionViewDisabledBlocker.setVisibility(View.GONE);
        } else {
            directionView.setVisibility(View.GONE);
            directionViewDisabled.setVisibility(View.VISIBLE);
            directionViewDisabledBlocker.setVisibility(View.VISIBLE);
        }
    }

    public static void myClickMethod(final View v) {
        switch (v.getId()) {
            case R.id.clearStatusWindowBtn:
                TextView statusWindowTV = ((Activity) v.getContext()).findViewById(R.id.statusWindowTV);
                statusWindowTV.setText("");
                return;
            case R.id.reconfigureBtn:
                ReconfigureHandler.ReconfigBtnOnCLick(v.getContext());
                return;
            case R.id.F1_btn:
                ReconfigureHandler.F1BtnOnCLick(v.getContext());
                return;
            case R.id.F2_btn:
                ReconfigureHandler.F2BtnOnCLick(v.getContext());
                return;
            case R.id.stopBtn:
                //RobotMovingSimulator(v);
                return;
        }

        Toast.makeText(v.getContext(), "to be updated", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("SetTextI18n")
    public static void MoveRobot(Context context, int direction) {
        switch (direction) {
            case MOVE_UP:
                if (GridMapHandler.robotCurrentRow + 1 == GridMapHandler.rowTotalNumber - 1) {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "UP");
                    BluetoothFragment.sendMessage("UP");
                } else {
                    GridMapFragment.AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move up anymore\n");

                }
                break;
            case MOVE_DOWN:
                if (GridMapHandler.robotCurrentRow - 1 == 0) {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "DOWN");
                    BluetoothFragment.sendMessage("DOWN");
                } else {
                    GridMapFragment.AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move down anymore\n");

                }
                break;
            case MOVE_LEFT:
                if (GridMapHandler.robotCurrentColumn + 1 == GridMapHandler.columnTotalNumber - 1) {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "LEFT");
                    BluetoothFragment.sendMessage("LEFT");
                } else {
                    GridMapFragment.AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move left anymore\n");

                }
                break;
            case MOVE_RIGHT:
                if (GridMapHandler.robotCurrentColumn - 1 == 0) {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "RIGHT");
                    BluetoothFragment.sendMessage("RIGHT");
                } else {
                    GridMapFragment.AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move right anymore\n");

                }
                break;
        }
    }

    public static void AddTextToStatusWindow(Activity activity, String stringToAdd) {
        String currentText;
        TextView statusWindow;
        ScrollView scrollView;

        try {
            scrollView = activity.findViewById(R.id.scrollView);
            statusWindow = activity.findViewById(R.id.statusWindowTV);
            currentText = statusWindow.getText().toString();
            currentText = currentText.replace("\n\n", "\n");
        } catch (Exception e) {
            //user switched fragment
            return;
        }

        statusWindow.setText(currentText + stringToAdd + "\n");
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    /*
    //temporary variables for testing
    private static int timer = 39;
    private static Handler handler = new Handler();
    private static Runnable runnable;

    private static void RobotMovingSimulator(final View v) {
        if (handler.hasMessages(0)) {
            handler.removeCallbacks(runnable);
        } else {
            runnable = new Runnable() {
                @Override
                public void run() {
                    timer++;
                    timer = timer % 40;

                    if (timer < 10) {
                        GridMapHandler.robotCurrentRow--;
                        SetArrowPicture(v.getContext(), 0, GridMapHandler.robotCurrentRow, 4);
                        SetArrowPicture(v.getContext(), 90, GridMapHandler.robotCurrentRow, 5);
                    } else if (timer < 20) {
                        GridMapHandler.robotCurrentColumn++;
                        ChangeCellColor(v.getContext(), Color.BLACK, 2, GridMapHandler.robotCurrentColumn);
                    } else if (timer < 30) {
                        GridMapHandler.robotCurrentRow++;
                        RemoveArrowPicture(v.getContext(), GridMapHandler.robotCurrentRow, 4);
                        RemoveArrowPicture(v.getContext(), GridMapHandler.robotCurrentRow, 5);
                    } else {
                        GridMapHandler.robotCurrentColumn--;
                        ChangeCellColor(v.getContext(), Color.WHITE, 2, GridMapHandler.robotCurrentColumn);
                    }

                    SetRobotPosition(v.getContext(), false);

                    handler.postDelayed(this, 100);
                }
            };
            handler.postDelayed(runnable, 100);
        }
    }*/
}

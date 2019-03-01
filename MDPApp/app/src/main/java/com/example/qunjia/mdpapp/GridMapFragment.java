package com.example.qunjia.mdpapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.andretietz.android.controller.DirectionView;
import com.andretietz.android.controller.InputView;
import com.example.qunjia.mdpapp.OpenGL.myGlSurfaceView;
import com.example.qunjia.mdpapp.OpenGL.myRenderer;


public class GridMapFragment extends Fragment {

    public static final int MOVE_NONE = 0, MOVE_UP = 1, MOVE_DOWN = 2,
            MOVE_LEFT = 3, MOVE_RIGHT = 4;

    public static Boolean is3Dmode = false;
    private static String statusWindowTxt = "";


    public static GridMapFragment newInstance(int position) {
        GridMapFragment f = new GridMapFragment();
        Bundle b = new Bundle();
        b.putInt("position", position);
        f.setArguments(b);
        return f;
    }

    static GridMapUpdateManager mapUpdateManager;

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
        GridMapHandler2D.CreateGridMap(getActivity());
        DirectionViewSetup(getActivity());
        InitWaypointToggleBtnListener();
        InitAutoManualToggleBtnListener();
        Init3DToggleBtnListener();
        getActivity().findViewById(R.id.updateBtn).setEnabled(false);

        //
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /*GridMapHandler2D.SetRobotPosition(getActivity(), 18, 1);
                GridMapHandler2D.SetArrowPicture(getActivity(), 0, 5, 5);
                GridMapHandler2D.ChangeCellColor(getActivity(), Color.BLACK, 5, 6);
                GridMapHandler2D.ChangeCellColor(getActivity(), Color.BLACK, 5, 7);
                GridMapHandler2D.ChangeCellColor(getActivity(), Color.BLACK, 5, 8);
                GridMapHandler2D.ChangeCellColor(getActivity(), Color.BLACK, 6, 8);
                GridMapHandler2D.ChangeCellColor(getActivity(), Color.BLACK, 7, 8);
                GridMapHandler2D.ChangeCellColor(getActivity(), Color.BLACK, 8, 8);
                GridMapHandler2D.SetArrowPicture(getActivity(), 90, 9, 8);*/
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
                    case DirectionView.DIRECTION_UP:
                        MoveRobot(context, MOVE_UP);
                        break;
                    case DirectionView.DIRECTION_DOWN:
                        MoveRobot(context,MOVE_DOWN);
                        break;
                    case DirectionView.DIRECTION_LEFT:
                        MoveRobot(context, MOVE_LEFT);
                        break;
                    case DirectionView.DIRECTION_RIGHT:
                        MoveRobot(context, MOVE_RIGHT);
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
                mapUpdateManager.toggleDisplayMode();

                if (compoundButton.isChecked()) {
                    GridMapHandler2D.SetRobotDragListener(compoundButton.getContext(), true);
                    DirectionViewSetEnabled(activity, false);

                    autoManualToggleBtn.setEnabled(false);
                    accelerometerSwitch.setEnabled(false);
                    updateBtn.setEnabled(false);
                    exploreBtn.setEnabled(false);
                    fastestBtn.setEnabled(false);
                    stopBtn.setEnabled(false);

                } else {
                    GridMapHandler2D.SetRobotDragListener(compoundButton.getContext(), false);
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

    private void Init3DToggleBtnListener() {
        ToggleButton toggleButton = getActivity().findViewById(R.id.DToggleBtn);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ConstraintLayout constraintLayout2D = getActivity().findViewById(R.id.constraintLayout2D);
                ConstraintLayout constraintLayout3D = getActivity().findViewById(R.id.constraintLayout3D);

                if (compoundButton.isChecked()) {
                    is3Dmode = true;
                    GridMapHandler2D.SetRobotPosition(getContext(), -1,-1);//hide robot
                    constraintLayout2D.setVisibility(View.GONE);
                    constraintLayout3D.setVisibility(View.VISIBLE);
                    AddTextToStatusWindow(getActivity(), null);

                    Robot3DMapSimulator(true);


                } else {
                    is3Dmode = false;
                    GridMapHandler2D.SetRobotPosition(getContext(), 100,100);//show robot
                    constraintLayout2D.setVisibility(View.VISIBLE);
                    constraintLayout3D.setVisibility(View.GONE);
                    AddTextToStatusWindow(getActivity(), null);
                }
            }
        });
    }



    private int simulatorInt = 0;//for 3d map simulator
    private Boolean forward = true;
    private void Robot3DMapSimulator(Boolean moving){
       /* final int[] gridMap = new int[300];
        for(int i = 0; i < 300; i++){
            double rand = Math.random();
            if(i % 15 == 14 || i % 15 == 13 ) gridMap[i] = 0;
            else if(rand < 0.8) {
                gridMap[i] = 0;
            }
            else {
                gridMap[i] = 1;
            }
        }

        if(!moving){
            gridMap[14] = -1;
            RelativeLayout relativeLayout = getActivity().findViewById(R.id.gridMap3DOne);
            myGlSurfaceView openGLView = new myGlSurfaceView(getContext(), gridMap);
            relativeLayout.removeAllViews();
            relativeLayout.addView(openGLView);
            return;
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            public void run(){
                //do something
                handler.postDelayed(this, 500);

                if(forward){
                    gridMap[14 + 15 * simulatorInt] = -1;
                    if(simulatorInt != 0){
                        gridMap[14 + 15 * (simulatorInt - 1)] = 0;
                    }
                    simulatorInt++;
                }else {
                    gridMap[14 + 15 * simulatorInt] = -1;
                    if(simulatorInt != 0){
                        gridMap[14 + 15 * (simulatorInt + 1)] = 0;
                    }
                    simulatorInt--;
                }

                if(simulatorInt == 18 || simulatorInt == 0) forward = !forward;

                if(usingFirstLayout){
                    RelativeLayout relativeLayout = getActivity().findViewById(R.id.gridMap3DOne);
                    myGlSurfaceView openGLView = new myGlSurfaceView(getContext(), gridMap);
                    relativeLayout.removeAllViews();
                    relativeLayout.addView(openGLView);
                } else {
                    RelativeLayout relativeLayout = getActivity().findViewById(R.id.gridMap3DTwo);
                    myGlSurfaceView openGLView = new myGlSurfaceView(getContext(), gridMap);
                    relativeLayout.removeAllViews();
                    relativeLayout.addView(openGLView);
                }

                Handler handler2 = new Handler();
                handler2.postDelayed(new Runnable(){
                    public void run(){
                        RelativeLayout relativeLayoutOne = getActivity().findViewById(R.id.gridMap3DOne);
                        RelativeLayout relativeLayoutTwo = getActivity().findViewById(R.id.gridMap3DTwo);

                        if(usingFirstLayout){
                            relativeLayoutOne.setVisibility(View.VISIBLE);
                            relativeLayoutTwo.setVisibility(View.GONE);
                        }
                        else {
                            relativeLayoutOne.setVisibility(View.GONE);
                            relativeLayoutTwo.setVisibility(View.VISIBLE);
                        }
                        usingFirstLayout = !usingFirstLayout;
                    }
                }, 100);
            }
        }, 500);*/
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
            case R.id.clearStatusWindowBtn2D:
            case R.id.clearStatusWindowBtn3D:
                statusWindowTxt = "";
                TextView statusWindowTV2D = ((Activity) v.getContext()).findViewById(R.id.statusWindowTV2D);
                statusWindowTV2D.setText("");
                TextView statusWindowTV3D = ((Activity) v.getContext()).findViewById(R.id.statusWindowTV3D);
                statusWindowTV3D.setText("");
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
                String msg = "MDF|F8007800F001F003F807F00FE01F800000000000000000000000000000000000000000000003|000000000700|N|13|4";
                mapUpdateManager.decodeMessage(v.getContext(), msg);
                //RobotMovingSimulator(v);
                return;
            case R.id.updateBtn:
                mapUpdateManager.updateAll(v.getContext());
                return;
            case R.id.fastestBtn:
                //GridMapHandler2D.SetRobotPosition(v.getContext(), 5, 5);
                return;
            case R.id.rotateRightBtn:
                myRenderer.rotateRight();
                return;
            case R.id.rotateLeftBtn:
                myRenderer.rotateLeft();
                return;
        }

        Toast.makeText(v.getContext(), "to be updated", Toast.LENGTH_LONG).show();
    }



    @SuppressLint("SetTextI18n")
    public static void MoveRobot(Context context, int direction) {

        if(is3Dmode){
            direction = RotateDirection(direction);
            if(direction == MOVE_NONE) return;
        }

        switch (direction) {
            case MOVE_UP:myRenderer.setZ(myRenderer.getZ() - 1);
                if (GridMapUpdateManager.RobotDescriptor.getRowNumber() - 1 == 0) {
                    AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move up anymore\n");
                } else {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "UP");
                    BluetoothFragment.sendMessage("UP");
                }
                break;
            case MOVE_DOWN:myRenderer.setZ(myRenderer.getZ()+1);
                if (GridMapUpdateManager.RobotDescriptor.getRowNumber() + 1 == GridMapHandler2D.rowTotalNumber - 1) {
                    AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move down anymore\n");
                } else {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "DOWN");
                    BluetoothFragment.sendMessage("DOWN");

                }
                break;
            case MOVE_LEFT:myRenderer.setX(myRenderer.getX() - 1);
                if (GridMapUpdateManager.RobotDescriptor.getColumnNumber() - 1 == 0) {
                    AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move left anymore\n");
                } else {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "LEFT");
                    BluetoothFragment.sendMessage("LEFT");
                }
                break;
            case MOVE_RIGHT:myRenderer.setX(myRenderer.getX() + 1);
                if (GridMapUpdateManager.RobotDescriptor.getColumnNumber() + 1 == GridMapHandler2D.columnTotalNumber - 1) {
                    AddTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move right anymore\n");
                } else {
                    //SetRobotPosition(context, false);
                    AddTextToStatusWindow((Activity) context, "RIGHT");
                    BluetoothFragment.sendMessage("RIGHT");
                }
                break;
        }
    }

    public static int RotateDirection(int direction){
        int[] d = new int[4];
        int facing = (int)myRenderer.getRotation()%360;
        switch (facing){
            case 0://face north
                d[0] = MOVE_UP;
                d[1] = MOVE_DOWN;
                d[2] = MOVE_LEFT;
                d[3] = MOVE_RIGHT;
                break;
            case 90://face east
            case -270:
                d[0] = MOVE_RIGHT;
                d[1] = MOVE_LEFT;
                d[2] = MOVE_UP;
                d[3] = MOVE_DOWN;
                break;
            case 180://face south
            case -180:
                d[0] = MOVE_DOWN;
                d[1] = MOVE_UP;
                d[2] = MOVE_RIGHT;
                d[3] = MOVE_LEFT;
                break;
            case 270://face west
            case -90:
                d[0] = MOVE_LEFT;
                d[1] = MOVE_RIGHT;
                d[2] = MOVE_DOWN;
                d[3]= MOVE_UP;
                break;
            default: return MOVE_NONE; //error
        }

        switch (direction){
            case MOVE_UP:
                direction = d[0];
                break;
            case MOVE_DOWN:
                direction = d[1];
                break;
            case MOVE_LEFT:
                direction = d[2];
                break;
            case MOVE_RIGHT:
                direction = d[3];
                break;
        }
        return direction;
    }

    public static void AddTextToStatusWindow(Activity activity, String stringToAdd) {
        try {
            TextView statusWindow;
            ScrollView scrollView;
            if(is3Dmode) {
                scrollView = activity.findViewById(R.id.scrollView3D);
                statusWindow = activity.findViewById(R.id.statusWindowTV3D);
            }
            else {
                scrollView = activity.findViewById(R.id.scrollView2D);
                statusWindow = activity.findViewById(R.id.statusWindowTV2D);
            }
            if(stringToAdd != null) {
                statusWindowTxt = statusWindowTxt + stringToAdd + "\n";
                statusWindowTxt = statusWindowTxt.replace("\n\n", "\n");
            }
            statusWindow.setText(statusWindowTxt);
            scrollView.fullScroll(View.FOCUS_DOWN);
        } catch (Exception e) {
            //user switched fragment
            return;
        }

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
                        GridMapHandler2D.robotCurrentRow--;
                        SetArrowPicture(v.getContext(), 0, GridMapHandler2D.robotCurrentRow, 4);
                        SetArrowPicture(v.getContext(), 90, GridMapHandler2D.robotCurrentRow, 5);
                    } else if (timer < 20) {
                        GridMapHandler2D.robotCurrentColumn++;
                        ChangeCellColor(v.getContext(), Color.BLACK, 2, GridMapHandler2D.robotCurrentColumn);
                    } else if (timer < 30) {
                        GridMapHandler2D.robotCurrentRow++;
                        RemoveArrowPicture(v.getContext(), GridMapHandler2D.robotCurrentRow, 4);
                        RemoveArrowPicture(v.getContext(), GridMapHandler2D.robotCurrentRow, 5);
                    } else {
                        GridMapHandler2D.robotCurrentColumn--;
                        ChangeCellColor(v.getContext(), Color.WHITE, 2, GridMapHandler2D.robotCurrentColumn);
                    }

                    SetRobotPosition(v.getContext(), false);

                    handler.postDelayed(this, 100);
                }
            };
            handler.postDelayed(runnable, 100);
        }
    }*/
}

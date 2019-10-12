package com.example.kevzzsk.mdpapp.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.andretietz.android.controller.DirectionView;
import com.andretietz.android.controller.InputView;
import com.example.kevzzsk.mdpapp.*;
import com.example.kevzzsk.mdpapp.Handler.AccelerometerSwitchListener;
import com.example.kevzzsk.mdpapp.Handler.ReconfigureHandler;
import com.example.kevzzsk.mdpapp.Manager.BluetoothService;
import com.example.kevzzsk.mdpapp.Manager.GridMapHandler2D;
import com.example.kevzzsk.mdpapp.Manager.GridMapUpdateManager;
import com.example.kevzzsk.mdpapp.OpenGL.myRenderer;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class GridMapFragment extends Fragment {

    public static final int MOVE_NONE = 0, MOVE_UP = 1, MOVE_DOWN = 2,
            MOVE_LEFT = 3, MOVE_RIGHT = 4;

    public static Boolean is3Dmode = false;
    private static String statusWindowTxt = "";

    public static CountDownTimer timer;
    public static int playbackCounter = 0;
    public static Boolean isExploring = false;

    //debug var
    public static int arrowCounter = 0;
    public static Boolean isDebug = false;
    private static int  debugCounter = 0, debugdirection = 0;
    private static String[] debugDirectionStr = {"N", "E", "S", "W"};

    //for demo
    private static JSONArray jsonArray;
    private static int democounter = 0;



    public static GridMapFragment newInstance(int position) {
        GridMapFragment f = new GridMapFragment();
        Bundle b = new Bundle();
        b.putInt("position", position);
        f.setArguments(b);
        return f;
    }

    public static GridMapUpdateManager mapUpdateManager;

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
        initSwitchListener();
        GridMapHandler2D.createGridMap(getActivity());
        directionViewSetup(getActivity());
        initPositionToggleBtnListener();
        initAutoManualToggleBtnListener();
        init3DToggleBtnListener();
        getActivity().findViewById(R.id.updateBtn).setEnabled(false);

        //
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                GridMapUpdateManager.MDFArrayList = new ArrayList<>();

                SharedPreferences prefs = getContext().getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE);
                String savedMDF = prefs.getString("MDFArrayList", "");
                if(!savedMDF.equals("")){
                    String[] decoded = savedMDF.split("divider");

                    GridMapUpdateManager.MDFArrayList = new ArrayList<>(Arrays.asList(decoded));
                }

                String msg = "MDF|C000000000000000000000000000000000000000000000000000000000000000000000000003|000000000000|N|1|1|0";
                //String msg = "MDF|FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF|000000000080010042038400000000000000010C000000000000021F84000800000000000400|W|17|2|1";
                mapUpdateManager.decodeMessage(getContext(), msg);
            }
        }, 200);
    }

    private void initSwitchListener() {
        Switch directionSwitch = (Switch) getActivity().findViewById(R.id.directionToggleBtn);
        directionSwitch.setOnCheckedChangeListener(new AccelerometerSwitchListener());
    }

    private void directionViewSetup(final Context context) {
        DirectionView directionView = (DirectionView) ((Activity) context).findViewById(R.id.viewDirection);
        directionView.setOnButtonListener(new InputView.InputEventListener() {
            @Override
            public void onInputEvent(View view, int buttons) {


                switch (buttons & 0xff) {
                    case DirectionView.DIRECTION_UP:
                        moveRobot(context, MOVE_UP);
                        break;
                    case DirectionView.DIRECTION_DOWN:
                        moveRobot(context,MOVE_DOWN);
                        break;
                    case DirectionView.DIRECTION_LEFT:
                        moveRobot(context, MOVE_LEFT);
                        break;
                    case DirectionView.DIRECTION_RIGHT:
                        moveRobot(context, MOVE_RIGHT);
                        break;
                }
            }
        });
    }

    private void initPositionToggleBtnListener() {
        ToggleButton toggleButton = getActivity().findViewById(R.id.positionToggleBtn);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
                final Activity activity = (Activity) compoundButton.getContext();
                final ToggleButton autoManualToggleBtn = activity.findViewById(R.id.autoManualToggleBtn);
                final Switch accelerometerSwitch = activity.findViewById(R.id.directionToggleBtn);
                final Button updateBtn = activity.findViewById(R.id.updateBtn);
                //final Button exploreBtn = activity.findViewById(R.id.exploreBtn);
                final Button fastestBtn = activity.findViewById(R.id.fastestBtn);
                final Button stopBtn = activity.findViewById(R.id.stopBtn);

                if (compoundButton.isChecked()) {
                    GridMapHandler2D.SetRobotDragListener(compoundButton.getContext(), true);
                    directionViewSetEnabled(activity, false);

                    autoManualToggleBtn.setEnabled(false);
                    accelerometerSwitch.setEnabled(false);
                    updateBtn.setEnabled(false);
                    //exploreBtn.setEnabled(false);
                    fastestBtn.setEnabled(false);
                    stopBtn.setEnabled(false);

                    AlertDialog.Builder adb = new AlertDialog.Builder(compoundButton.getContext());
                    CharSequence items[] = new CharSequence[] {"North", "South", "East", "West"};
                    adb.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int n) {
                            GridMapHandler2D.robotStartCoordinateDirection = n;
                        }

                    });
                    adb.setPositiveButton("Waypoint", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            GridMapHandler2D.isWaypointSelected = true;
                        }
                    });
                    adb.setNegativeButton("Start", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            GridMapHandler2D.isWaypointSelected = false;
                        }
                    });
                    adb.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            GridMapHandler2D.SetRobotDragListener(compoundButton.getContext(), false);
                            directionViewSetEnabled(activity, true);

                            autoManualToggleBtn.setEnabled(true);
                            accelerometerSwitch.setEnabled(true);
                            updateBtn.setEnabled(true);
                            //exploreBtn.setEnabled(true);
                            fastestBtn.setEnabled(true);
                            stopBtn.setEnabled(true);
                            compoundButton.toggle();
                        }
                    });
                    adb.setTitle("Choose a direction");
                    adb.setCancelable(false);
                    adb.show();
                } else {
                    GridMapHandler2D.SetRobotDragListener(compoundButton.getContext(), false);
                    directionViewSetEnabled(activity, true);

                    autoManualToggleBtn.setEnabled(true);
                    accelerometerSwitch.setEnabled(true);
                    updateBtn.setEnabled(true);
                    //exploreBtn.setEnabled(true);
                    fastestBtn.setEnabled(true);
                    stopBtn.setEnabled(true);
                }
            }
        });
    }

    private void initAutoManualToggleBtnListener() {
        ToggleButton toggleButton = getActivity().findViewById(R.id.autoManualToggleBtn);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Activity activity = (Activity) compoundButton.getContext();
                Button updateBtn = activity.findViewById(R.id.updateBtn);
                mapUpdateManager.toggleDisplayMode();
                if (compoundButton.isChecked()) {
                    updateBtn.setEnabled(true);
                } else {
                    updateBtn.setEnabled(false);
                }
            }
        });
    }

    private void init3DToggleBtnListener() {
        ToggleButton toggleButton = getActivity().findViewById(R.id.DToggleBtn);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ConstraintLayout constraintLayout2D = getActivity().findViewById(R.id.constraintLayout2D);
                ConstraintLayout constraintLayout3D = getActivity().findViewById(R.id.constraintLayout3D);
                ToggleButton posToggleBtn = getActivity().findViewById(R.id.positionToggleBtn);

                if (compoundButton.isChecked()) {
                    is3Dmode = true;
                    GridMapHandler2D.setRobotPosition(getContext(), -1,-1,-1);//hide robot
                    constraintLayout2D.setVisibility(View.GONE);
                    constraintLayout3D.setVisibility(View.VISIBLE);
                    addTextToStatusWindow(getActivity(), null);
                    posToggleBtn.setEnabled(false);


                } else {
                    is3Dmode = false;
                    GridMapHandler2D.setRobotPosition(getContext(), 1000, 100,100);//show robot
                    constraintLayout2D.setVisibility(View.VISIBLE);
                    constraintLayout3D.setVisibility(View.GONE);
                    addTextToStatusWindow(getActivity(), null);
                    posToggleBtn.setEnabled(true);
                }
            }
        });
    }


    public static void directionViewSetEnabled(Activity activity, Boolean enabled) {
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
        Button playbackForward = ((Activity) v.getContext()).findViewById(R.id.playback_forward);
        Button playbackBackward = ((Activity) v.getContext()).findViewById(R.id.playback_backward);
        Button exploreBtn = ((Activity) v.getContext()).findViewById(R.id.exploreBtn);

        switch (v.getId()) {
            case R.id.clearStatusWindowBtn2D:
            case R.id.clearStatusWindowBtn3D:
                statusWindowTxt = "";
                TextView statusWindowTV2D = ((Activity) v.getContext()).findViewById(R.id.statusWindowTV2D);
                statusWindowTV2D.setText("");
                TextView statusWindowTV3D = ((Activity) v.getContext()).findViewById(R.id.statusWindowTV3D);
                statusWindowTV3D.setText("");

                arrowCounter++;
                return;
            case R.id.reconfigureBtn:
                ReconfigureHandler.reconfigBtnOnClick(v.getContext());
                return;
            case R.id.F1_btn:
                ReconfigureHandler.F1BtnOnCLick(v.getContext());
                return;
            case R.id.F2_btn:
                ReconfigureHandler.F2BtnOnCLick(v.getContext());
                return;
            case R.id.stopBtn:
                addTextToStatusWindow((Activity)v.getContext(), "Stop");
                GridMapUpdateManager.explorationDone(v.getContext());

                //send stop message to rpi
                String stopMsg = "SZ";
                //BluetoothService.getInstance(null, null).sendMessage(stopMsg);
                if(isDebug){
                    addTextToStatusWindow((Activity) v.getContext(), "Bluetooth:" + stopMsg);
                }
                return;
            case R.id.updateBtn:
                mapUpdateManager.updateAll(v.getContext());
                return;
            case R.id.fastestBtn:
                String fastMsg = "A|F";
                BluetoothService.getInstance(null, null).sendMessage(fastMsg);
                addTextToStatusWindow((Activity)v.getContext(), "Fastest");
                if(isDebug){
                    addTextToStatusWindow((Activity) v.getContext(), "Bluetooth:" + fastMsg);
                }
                //robotFastestSimulator(v.getContext());
                return;
            case R.id.exploreBtn:
                isExploring = true;
                GridMapUpdateManager.MDFArrayList = new ArrayList<>();
                playbackCounter = 0;
                playbackForward.setEnabled(false);
                playbackBackward.setEnabled(false);
                String exploreMsg = "SV";
                BluetoothService.getInstance(null, null).sendMessage(exploreMsg);
                addTextToStatusWindow((Activity)v.getContext(), "Explore");

                final long totalTime = 30 * 60 * 1000;//30 mins
                long intervalSeconds = 1;
                final TextView textView = (TextView) v;
                textView.setEnabled(false);

                timer = new CountDownTimer(totalTime, intervalSeconds * 1000) {

                    @SuppressLint("SetTextI18n")
                    public void onTick(long millisUntilFinished) {
                        int secondsInt = (int)((totalTime - millisUntilFinished) / 1000 % 60);
                        String seconds;
                        if(secondsInt < 10) {
                            seconds = "0" + secondsInt;
                        }else {
                            seconds = "" + secondsInt;
                        }
                        String mins = "0" + ((totalTime - millisUntilFinished) / 1000 / 60) + "";
                        textView.setText(mins + ":" + seconds);
                    }

                    public void onFinish() {
                        textView.setText("Explore");
                        timer = null;
                    }

                };

                timer.start();
                for(int r = 0; r < 20; r++){
                    for(int c = 0; c < 15; c++){
                        GridMapHandler2D.removeArrowPicture(v.getContext(),r ,c);
                    }
                }

                if(isDebug){
                    addTextToStatusWindow((Activity) v.getContext(), "Bluetooth:" + exploreMsg);
                }
                //isExploring = false;
                robotExploreSimulator(v);
                return;
            case R.id.rotateRightBtn:
                myRenderer.rotateRight();
                return;
            case R.id.rotateLeftBtn:
                myRenderer.rotateLeft();
                return;
            case R.id.playback_forward:
                if(playbackCounter < GridMapUpdateManager.MDFArrayList.size() - 1){
                    playbackCounter++;
                    mapUpdateManager.decodeMessage(v.getContext(), GridMapUpdateManager.MDFArrayList.get(playbackCounter));
                }

                return;
            case R.id.playback_backward:
                if(playbackCounter > 0){
                    playbackCounter--;
                    String removeArrowStr = GridMapUpdateManager.MDFArrayList.get(playbackCounter).substring(0,GridMapUpdateManager.MDFArrayList.get(playbackCounter).length() -1);
                    String addZero = removeArrowStr + "0";
                    mapUpdateManager.decodeMessage(v.getContext(), addZero);
                }
                return;
        }

        Toast.makeText(v.getContext(), "to be updated", Toast.LENGTH_LONG).show();
    }

    private static String getRotationPayload(int facing_direction, int target_direction) {
        int delta_direction = GridMapUpdateManager.calculate_delta(facing_direction, target_direction);
        String payload = "";
        while (delta_direction == -90 || delta_direction == -180 || delta_direction == 270) { // turn left
            payload += "L";
            facing_direction -= 90;
            delta_direction = GridMapUpdateManager.calculate_delta(facing_direction, target_direction);
        }
        while (delta_direction == 90 || delta_direction == 180 || delta_direction == -270) { // turn right
            payload += "R";
            facing_direction += 90;
            delta_direction = GridMapUpdateManager.calculate_delta(facing_direction, target_direction);
        }

        return payload;
    }

    @SuppressLint("SetTextI18n")
    public static void moveRobot(Context context, int direction) {
//        if(is3Dmode){
//            direction = RotateDirection(direction);
//            if(direction == MOVE_NONE) return;
//        }
        int facing = (int)myRenderer.getRotation() % 360;
        String payload = "";

        switch (direction) {
            case MOVE_UP:
                if(isDebug){
                    payload = "SS1";

                    int row = 0;
                    int col = 0;

                    switch (debugdirection){
                        case 0: if(GridMapUpdateManager.RobotDescriptor.getRowNumber() != 1) row = -1;
                            break;
                        case 1: if(GridMapUpdateManager.RobotDescriptor.getColumnNumber() != 14) col = 1;
                            break;
                        case 2: if(GridMapUpdateManager.RobotDescriptor.getRowNumber() != 18) row = 1;
                            break;
                        case 3: if(GridMapUpdateManager.RobotDescriptor.getColumnNumber() != 1)col = -1;
                            break;
                    }

                    addTextToStatusWindow((Activity) context, "Bluetooth: " + payload);
                    String msg = "MDF|"
                            + GridMapUpdateManager.fullMapStr + "|"
                            + GridMapUpdateManager.obstaclesStr + "|"
                            + debugDirectionStr[debugdirection] + "|"
                            + (GridMapUpdateManager.RobotDescriptor.getRowNumber() + row) + "|"
                            + (GridMapUpdateManager.RobotDescriptor.getColumnNumber() + col)
                            + "|0";
                    GridMapFragment.mapUpdateManager.decodeMessage(context, msg);
                    myRenderer.setZ(myRenderer.getZ() - 1);
                    break;
                }

                if (GridMapUpdateManager.RobotDescriptor.getRowNumber() - 1 == 0) {
                    addTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move up anymore\n");
                } else {
                    addTextToStatusWindow((Activity) context, "NORTH");
                    payload += "S" + getRotationPayload(facing, GridMapUpdateManager.FacingDirection.NORTH) + "S1";
                    GridMapUpdateManager.RobotDescriptor.setRowNumber(GridMapUpdateManager.RobotDescriptor.getRowNumber()-1);
                    GridMapUpdateManager.RobotDescriptor.setFaceAngle(180);
                }
                break;
            case MOVE_DOWN:
                if(isDebug)
                    break;

                if (GridMapUpdateManager.RobotDescriptor.getRowNumber() + 1 == GridMapHandler2D.rowTotalNumber - 1) {
                    addTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move down anymore\n");
                } else {
                    addTextToStatusWindow((Activity) context, "SOUTH");
                    payload += "S" + getRotationPayload(facing, GridMapUpdateManager.FacingDirection.SOUTH) + "S1";
                    GridMapUpdateManager.RobotDescriptor.setRowNumber(GridMapUpdateManager.RobotDescriptor.getRowNumber()+1);
                    GridMapUpdateManager.RobotDescriptor.setFaceAngle(0);
                }
                break;
            case MOVE_LEFT:
                if(isDebug){
                    payload = "SL";
                    addTextToStatusWindow((Activity) context, "Bluetooth: " + payload);
                    debugdirection -= 1;
                    if(debugdirection == -1) debugdirection = 3;


                    String msg = "MDF|"
                            + GridMapUpdateManager.fullMapStr + "|"
                            + GridMapUpdateManager.obstaclesStr + "|"
                            + debugDirectionStr[debugdirection] + "|"
                            + GridMapUpdateManager.RobotDescriptor.getRowNumber() + "|"
                            + GridMapUpdateManager.RobotDescriptor.getColumnNumber()
                            + "|0";
                    GridMapFragment.mapUpdateManager.decodeMessage(context, msg);
                    //myRenderer.setX(myRenderer.getX() - 1);
                    break;
                }

                if (GridMapUpdateManager.RobotDescriptor.getColumnNumber() - 1 == 0) {
                    addTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move left anymore\n");
                } else {
                    addTextToStatusWindow((Activity) context, "WEST");
                    payload += "S" + getRotationPayload(facing, GridMapUpdateManager.FacingDirection.WEST) + "S1";
                    GridMapUpdateManager.RobotDescriptor.setColumnNumber(GridMapUpdateManager.RobotDescriptor.getColumnNumber()-1);
                    GridMapUpdateManager.RobotDescriptor.setFaceAngle(90);
                }
                break;
            case MOVE_RIGHT:
                if(GridMapFragment.isDebug){
                    payload = "SR";
                    addTextToStatusWindow((Activity) context, "Bluetooth: " + payload);
                    debugdirection = (debugdirection + 1)%4;

                    String msg = "MDF|"
                            + GridMapUpdateManager.fullMapStr + "|"
                            + GridMapUpdateManager.obstaclesStr + "|"
                            + debugDirectionStr[debugdirection] + "|"
                            + GridMapUpdateManager.RobotDescriptor.getRowNumber() + "|"
                            + GridMapUpdateManager.RobotDescriptor.getColumnNumber()
                            + "|0";
                    GridMapFragment.mapUpdateManager.decodeMessage(context, msg);
                    //myRenderer.setX(myRenderer.getX() + 1);
                    break;
                }

                if (GridMapUpdateManager.RobotDescriptor.getColumnNumber() + 1 == GridMapHandler2D.columnTotalNumber - 1) {
                    addTextToStatusWindow(((Activity) context), "ERROR: Robot cannot move right anymore\n");
                } else {
                    addTextToStatusWindow((Activity) context, "EAST");
                    payload += "S" + getRotationPayload(facing, GridMapUpdateManager.FacingDirection.EAST) + "S1";
                    GridMapUpdateManager.RobotDescriptor.setColumnNumber(GridMapUpdateManager.RobotDescriptor.getColumnNumber()+1);
                    GridMapUpdateManager.RobotDescriptor.setFaceAngle(270);
                }
                break;
        }

        if (!payload.equals("")) {
            mapUpdateManager.updateAll(context);
            BluetoothService.getInstance(null, null).sendMessage(payload);
        }
    }


    public static void addTextToStatusWindow(Activity activity, String stringToAdd) {
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

    private static void robotExploreSimulator(final View v){
        try {
            jsonArray = new JSONArray(v.getContext().getResources().getString(R.string.sampleMDF));
        }catch (Exception e){
        }
        final Context context = v.getContext();
        final Handler handler = new Handler();
        final int delay = 300; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){
                String msg = "";
                try{
                    Random rand = new Random();
                    int n = rand.nextInt(100);
                    String[] decoded = jsonArray.getString(democounter).split("\\|");
                    if(n < 90)
                        msg = jsonArray.getString(democounter).substring(0,jsonArray.getString(democounter).length() - 1)
                            + "0";
                    else if (n < 93){
                        msg = jsonArray.getString(democounter).substring(0,jsonArray.getString(democounter).length() - 1)
                                + "F";
                        //addTextToStatusWindow((Activity)v.getContext(), "F " + "R" + decoded[4] + " C" + decoded[5] + decoded[3]);
                    }
                    else if (n < 96){
                        msg = jsonArray.getString(democounter).substring(0,jsonArray.getString(democounter).length() - 1)
                                + "M";
                        //addTextToStatusWindow((Activity)v.getContext(), "M " + "R" + decoded[4] + " C" + decoded[5] + decoded[3]);
                    }
                    else if (n < 100){
                        msg = jsonArray.getString(democounter).substring(0,jsonArray.getString(democounter).length() - 1)
                                + "B";
                        //addTextToStatusWindow((Activity)v.getContext(), "B " + "R" + decoded[4] + " C" + decoded[5] + decoded[3]);
                    }
                }catch (Exception e){
                    return;
                }
                mapUpdateManager.decodeMessage(context, msg);
                democounter++;
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private static void robotFastestSimulator(Context c){
        //String fastString = "FAST|REME9ME3RNRWMW4RNMN5REME4RNMN9MN3";
        String fastString = "F|RS9S3LLS4RS5RS4LS9S3";
        mapUpdateManager.decodeMessage(c, fastString);
    }
}

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

    public static final int MOVE_UP = 0, MOVE_DOWN = 1,
            MOVE_LEFT = 2, MOVE_RIGHT = 3;

    public static int cellSize, robotCurrentRow, robotCurrentColumn;
    private final static int cellMargin = 2, rowTotalNumber = 20, columnTotalNumber = 15;

    //temporary variables for testing
    private static int timer = 39;
    private static Handler handler = new Handler();
    private static Runnable runnable;

    public GridMapFragment() {

    }

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
        CreateGridMap(getActivity());
        DirectionViewSetup(getActivity());
        InitWaypointToggleBtnListener();
        InitAutoManualToggleBtnListener();
        //
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                robotCurrentColumn = 1;
                robotCurrentRow = 18;
                setRobotPosition(getActivity(), false);

            }
        }, 200);
    }

    private void InitSwitchListener(){
        Switch directionSwitch = (Switch) getActivity().findViewById(R.id.directionToggleBtn);
        directionSwitch.setOnCheckedChangeListener(new AccelerometerSwitchListener());
    }

    private void CreateGridMap (final Context context){
        TableLayout tbl= ((Activity) context).findViewById(R.id.gridMap);

        //Get cell size
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float gridMapWidth = (float) (size.x * 0.65);
        int cellWidth = (int) (gridMapWidth / columnTotalNumber);
        float gridMapHeight = (float) (size.y * 0.65);
        int cellHeight = (int) (gridMapHeight / rowTotalNumber);
        if(cellHeight<cellWidth) cellSize = cellHeight;
        else cellSize = cellWidth;

        //Create Grid Map
        for(int i=0; i<rowTotalNumber; i++) {
            //create a new tableRow with imageViews and add into tableLayout
            final TableRow row = new TableRow(context);
            row.setBackgroundColor(Color.BLACK);
            for(int j=0; j<columnTotalNumber; j++) {
                //create new imageView and add into row
                final ImageView imageView = new ImageView(context);
                TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                        cellSize, cellSize);
                layoutParams.setMargins(0,0,cellMargin,cellMargin);
                imageView.setLayoutParams(layoutParams);
                imageView.setBackgroundColor(Color.WHITE);
                imageView.setId(getImageViewID(j,i));
                imageView.setOnDragListener(new View.OnDragListener() {
                    @Override
                    public boolean onDrag(View view, DragEvent dragEvent) {
                        final int action = dragEvent.getAction();

                        switch (action){
                            case DragEvent.ACTION_DROP:
                                int rowNumber = (int) view.getId() %1000;
                                int columnNumber = (int)view.getId()/1000;
                                if(rowNumber>0 && rowNumber< rowTotalNumber-1 && columnNumber > 0 &&
                                        columnNumber < columnTotalNumber -1){
                                    robotCurrentColumn = columnNumber;
                                    robotCurrentRow = rowNumber;
                                    setRobotPosition(getActivity(), true);
                                }

                                break;
                            case DragEvent.ACTION_DRAG_LOCATION:
                                break;
                        }
                        return true;
                    }
                });
                row.addView(imageView);
            }
            row.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tbl.addView(row);
        }
    }

    private void DirectionViewSetup(final Context context){
        DirectionView directionView = (DirectionView) ((Activity) context).findViewById(R.id.viewDirection);
        directionView.setOnButtonListener(new InputView.InputEventListener() {
            @Override public void onInputEvent(View view, int buttons) {
                switch (buttons&0xff) {
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

    private void InitWaypointToggleBtnListener(){
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

                if(compoundButton.isChecked()){
                    setRobotPosition(compoundButton.getContext(), true);
                    DirectionViewSetEnabled(activity, false);

                    autoManualToggleBtn.setEnabled(false);
                    accelerometerSwitch.setEnabled(false);
                    updateBtn.setEnabled(false);
                    exploreBtn.setEnabled(false);
                    fastestBtn.setEnabled(false);
                    stopBtn.setEnabled(false);

                }else {
                    setRobotPosition(compoundButton.getContext(), false);
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

    private void InitAutoManualToggleBtnListener(){
        ToggleButton toggleButton = getActivity().findViewById(R.id.autoManualToggleBtn);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Activity activity = (Activity) compoundButton.getContext();
                Button updateBtn = activity.findViewById(R.id.updateBtn);

                if(compoundButton.isChecked()){
                    updateBtn.setEnabled(true);
                }else {
                    updateBtn.setEnabled(false);
                }
            }
        });
    }

    public static void DirectionViewSetEnabled(Activity activity, Boolean enabled){
        DirectionView directionView = activity.findViewById(R.id.viewDirection);
        DirectionView directionViewDisabled = activity.findViewById(R.id.viewDirectionDisabled);
        View directionViewDisabledBlocker = activity.findViewById(R.id.viewDirectionDisabledBlocker);
        if(enabled){
            directionView.setVisibility(View.VISIBLE);
            directionViewDisabled.setVisibility(View.GONE);
            directionViewDisabledBlocker.setVisibility(View.GONE);
        }else {
            directionView.setVisibility(View.GONE);
            directionViewDisabled.setVisibility(View.VISIBLE);
            directionViewDisabledBlocker.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void setRobotPosition(Context context, Boolean setRobotDragListener){
        RelativeLayout relativeLayout = ((Activity) context).findViewById(R.id.gridMapFragmentLayout);
        relativeLayout.removeAllViews();

        //create an transparent image view at robot new position
        final TextView textView = new TextView(((Activity) context));
        int[] location = new int[2];
        ImageView robotCellPosition = ((Activity) context).findViewById(getImageViewID(robotCurrentColumn,robotCurrentRow));
        robotCellPosition.getLocationOnScreen(location);
        int x = location[0] - cellSize - cellMargin;
        int y = location[1] - cellSize * 4 - (int) (cellMargin * 8.5);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                cellSize*3 + 2 * cellMargin, cellSize*3 + 3 * cellMargin);
        params.setMargins(x, y,0,0);
        textView.setLayoutParams(params);
        textView.setBackgroundColor(Color.BLUE);
        textView.setText("ROBOT");
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        if(setRobotDragListener) {
            textView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    ClipData data = ClipData.newPlainText("", "");
                    View.DragShadowBuilder shadow = new View.DragShadowBuilder(textView);
                    view.startDrag(data, shadow, null, 0);
                    return false;
                }
            });
        }
        relativeLayout.addView(textView);


    }

    private static int getImageViewID(int columnNumber, int rowNumber){
        return columnNumber * 1000 + rowNumber;
    }

    public static void ChangeCellColor(Context context,int color, int rowNumber, int columnNumber){
        try{
            ImageView imageView = ((Activity) context).findViewById(getImageViewID(columnNumber, rowNumber));
            imageView.setBackgroundColor(color);
        }catch (Exception e){
            Toast.makeText(context, "Change Cell Color Error at row " + rowNumber + " col " + columnNumber, Toast.LENGTH_LONG).show();
        }
    }

    public static void SetArrowPicture(Context context, int rotationAngle,int rowNumber, int columnNumber){
        try{
            ImageView imageView = ((Activity) context).findViewById(getImageViewID(columnNumber, rowNumber));
            AssetManager assetManager = context.getAssets();

            InputStream istr = assetManager.open("arrow.webp");
            Bitmap bitmap = BitmapFactory.decodeStream(istr);
            imageView.setImageBitmap(bitmap);
            imageView.setRotation(rotationAngle);
            istr.close();
        }catch (Exception e){
            Toast.makeText(context, "Set arrow Error at row " + rowNumber + " col " + columnNumber, Toast.LENGTH_LONG).show();
        }
    }

    public static void RemoveArrowPicture(Context context, int rowNumber, int columnNumber){
        try{
            ImageView imageView = ((Activity) context).findViewById(getImageViewID(columnNumber, rowNumber));
            imageView.setImageBitmap(null);
        }catch (Exception e){
            Toast.makeText(context, "Set arrow Error at row " + rowNumber + " col " + columnNumber, Toast.LENGTH_LONG).show();
        }
    }

    public static void myClickMethod(final View v) {
        switch(v.getId()) {
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
        }

        Toast.makeText(v.getContext(), "to be updated", Toast.LENGTH_LONG).show();
    }

    private static void RobotMovingSimulator(final View v){
        if(handler.hasMessages(0)){
            handler.removeCallbacks(runnable);
        }
        else {
            runnable = new Runnable() {
                @Override
                public void run() {
                    timer++;
                    timer = timer % 40;

                    if(timer<10){
                        robotCurrentRow--;
                        SetArrowPicture(v.getContext(), 0,robotCurrentRow,4);
                        SetArrowPicture(v.getContext(), 90,robotCurrentRow,5);
                    }
                    else if(timer <20){
                        robotCurrentColumn++;
                        ChangeCellColor(v.getContext(), Color.BLACK, 2, robotCurrentColumn);
                    }
                    else if(timer < 30){
                        robotCurrentRow++;
                        RemoveArrowPicture(v.getContext(), robotCurrentRow,4);
                        RemoveArrowPicture(v.getContext(), robotCurrentRow,5);
                    }
                    else {
                        robotCurrentColumn--;
                        ChangeCellColor(v.getContext(), Color.WHITE, 2, robotCurrentColumn);
                    }

                    setRobotPosition(v.getContext(), false);

                    handler.postDelayed(this, 100);
                }
            };
            handler.postDelayed(runnable, 100);
        }
    }

    public static void GridMapBluetoothHandler(Activity activity, String readMessage){
        switch (readMessage){
            case "2"://unexplored
                ChangeCellColor(activity,Color.RED,5,5);
                return;
            case "1"://obstacles
                ChangeCellColor(activity,Color.WHITE,5,5);
                return;
            case "0"://no obstacles
                ChangeCellColor(activity,Color.BLACK,5,5);
                return;
            case "-1"://current position
                setRobotPosition(activity, false);
                return;
            case "-2"://start
                //ChangeCellColor(activity,Color.RED,5,5);
                return;
            case "-3"://goal usually is top right)
                //ChangeCellColor(activity,Color.RED,5,5);
                return;
            case "-4"://way point (android set this)
                //ChangeCellColor(activity,Color.RED,5,5);
                return;
        }
    }

    @SuppressLint("SetTextI18n")
    public static void MoveRobot(Context context, int direction){

        String currentText;
        TextView statusWindow;
        ScrollView scrollView;

        try {
            statusWindow = ((Activity) context).findViewById(R.id.statusWindowTV);
            currentText = statusWindow.getText().toString();
            scrollView = ((Activity) context).findViewById(R.id.scrollView);
        }
        catch (Exception e){
            //user switched fragment
            return;
        }

        switch (direction){
            case MOVE_UP:
                robotCurrentRow--;
                if(CurrentOrientationInsideMap(context)){
                    setRobotPosition(context, false);
                    statusWindow.setText(currentText + "\nUP");
                    BluetoothFragment.sendMessage("UP");
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
                else {
                    robotCurrentRow++;
                }
                break;
            case MOVE_DOWN:
                robotCurrentRow++;
                if(CurrentOrientationInsideMap(context)){
                    setRobotPosition(context, false);
                    statusWindow.setText(currentText + "\nDOWN");
                    BluetoothFragment.sendMessage("DOWN");
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
                else {
                    robotCurrentRow--;
                }
                break;
            case MOVE_LEFT:
                robotCurrentColumn--;
                if(CurrentOrientationInsideMap(context)){
                    setRobotPosition(context, false);
                    statusWindow.setText(currentText + "\nLEFT");
                    BluetoothFragment.sendMessage("LEFT");
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
                else {
                    robotCurrentColumn++;
                }
                break;
            case MOVE_RIGHT:
                statusWindow.setText(currentText + "\nRIGHT");
                BluetoothFragment.sendMessage("RIGHT");
                scrollView.fullScroll(View.FOCUS_DOWN);
                robotCurrentColumn++;
                if(CurrentOrientationInsideMap(context)){
                    setRobotPosition(context, false);
                }
                else {
                    robotCurrentColumn--;
                }
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private static Boolean CurrentOrientationInsideMap(Context context){
        TextView statusWindow = ((Activity) context).findViewById(R.id.statusWindowTV);
        String currentText = statusWindow.getText().toString();
        ScrollView scrollView = ((Activity) context).findViewById(R.id.scrollView);
        if(robotCurrentColumn == 0){
            statusWindow.setText(currentText + "\nERROR: Robot cannot move left anymore");
            scrollView.fullScroll(View.FOCUS_DOWN);
            return false;
        }else if(robotCurrentColumn == columnTotalNumber - 1){
            statusWindow.setText(currentText + "\nERROR: Robot cannot move right anymore");
            scrollView.fullScroll(View.FOCUS_DOWN);
            return false;
        }else if (robotCurrentRow == 0){
            statusWindow.setText(currentText + "\nERROR: Robot cannot move up anymore");
            scrollView.fullScroll(View.FOCUS_DOWN);
            return false;
        }else if(robotCurrentRow == rowTotalNumber - 1){
            statusWindow.setText(currentText + "\nERROR: Robot cannot move down anymore");
            scrollView.fullScroll(View.FOCUS_DOWN);
            return false;
        }
        return true;
    }
}

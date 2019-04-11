package com.example.qunjia.mdpapp.Manager;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.example.qunjia.mdpapp.Fragment.GridMapFragment;
import com.example.qunjia.mdpapp.Manager.GridMapHandler2D;
import com.example.qunjia.mdpapp.OpenGL.myGlSurfaceView;
import com.example.qunjia.mdpapp.OpenGL.myRenderer;
import com.example.qunjia.mdpapp.R;

import org.json.JSONArray;

import java.math.BigInteger;
import java.util.ArrayList;

import static com.example.qunjia.mdpapp.Manager.GridMapHandler2D.*;
import static java.lang.Integer.parseInt;

/**
 * This class handles decoding of incoming meblussages from Raspberry Pi,
 * buffering of latest map descriptor string and conversion of raw coordinates
 * (from Raspberry Pi) into row number and column number.
 */
public class GridMapUpdateManager {
    private boolean isAutoMode = true;
    private MapDescriptor map;
    private RobotDescriptor robot;
    private static ArrowDescriptor arrow;
    public static String fullMapStr = "0", obstaclesStr = "0";
    public static ArrayList<String> MDFArrayList;

    public GridMapUpdateManager(Context context) {
        robot = new RobotDescriptor(18, 1, FacingDirection.NORTH);
        map = new MapDescriptor(context);
        arrow = new ArrowDescriptor(context);
    }

    public void toggleDisplayMode() {
        this.isAutoMode = !this.isAutoMode;
    }

    public static int calculate_delta(int current_direction, int target_direction) {
        current_direction = current_direction % 360;
        if (current_direction == -270) {
            current_direction = 90;
        } else if (current_direction == -180) {
            current_direction = 180;
        } else if (current_direction == -90) {
            current_direction = 270;
        }

        return target_direction - current_direction;
    }

    public void updateAll(Context context) {
        if (map != null) {
            map.update();
        }
        setRobotPosition(context, RobotDescriptor.getFaceAngle() + 180, RobotDescriptor.rowNumber, RobotDescriptor.columnNumber);
        myRenderer.setX(-RobotDescriptor.columnNumber);
        myRenderer.setZ(-RobotDescriptor.rowNumber);

        int delta_direction = calculate_delta((int) myRenderer.getRotation(), RobotDescriptor.faceAngle);
        while (delta_direction == -90 || delta_direction == -180 || delta_direction == 270) { // turn left
            myRenderer.rotateLeft();
            delta_direction = calculate_delta((int) myRenderer.getRotation(), RobotDescriptor.faceAngle);
        }
        while (delta_direction == 90 || delta_direction == 180 || delta_direction == -270) { // turn right
            myRenderer.rotateRight();
            delta_direction = calculate_delta((int) myRenderer.getRotation(), RobotDescriptor.faceAngle);
        }

        if (arrow != null) {
            arrow.updateArrow();
        }
    }

    public static class MapDescriptor {
        private static int[][] MapArr;
        Boolean usingFirstLayout; //for 3D map
        int exploredNo, unexploredNo, obstaclesNo;
        Context context;

        MapDescriptor(Context c) {
            MapArr = new int[20][15];
            usingFirstLayout = true;
            context = c;
            exploredNo = Integer.parseInt(context.getResources().getString(R.string.exploredNo));
            unexploredNo = Integer.parseInt(context.getResources().getString(R.string.unexploredNo));
            obstaclesNo = Integer.parseInt(context.getResources().getString(R.string.obstaclesNo));
        }

        void fromString(String full_map, String obstacles){
            //full_map is part 1 of MDF string
            //obstacles is part 2 of MDF string

            full_map = new BigInteger(full_map, 16).toString(2);//convert hex to binary
            full_map = full_map.substring(2, full_map.length()-2);//remove 11 from start and end of full_map

            obstacles = "F" + obstacles;//add "F" to prevent BigInteger from removing 0s from the front of binary string.
            obstacles = new BigInteger(obstacles, 16).toString(2);//convert hext to binary
            obstacles = obstacles.substring(4);//remove "F"

            //part 1: read full_map and store into MapArr[][]
            int row = 19;
            for(int col = 0; col < full_map.length(); col++){
                MapArr[row][(col)%15] = Character.getNumericValue(full_map.charAt(col));
                if(Character.getNumericValue(full_map.charAt(col)) == 0){
                    arrow.removeArrowFromString(String.valueOf(row), String.valueOf(col%15));
                }
                if((col) % 15 == 14){
                    row--;
                }
            }

            //part 2: read obstacles and store into MapArr[][]
            int o = 0;
            for (int r = MapArr.length - 1; r >= 0; r--) {
                for (int c = 0; c < MapArr[r].length; c++) {
                    if (MapArr[r][c] == 1) {
                        if (Character.getNumericValue(obstacles.charAt(o)) == 1) {
                            MapArr[r][c] = obstaclesNo;
                        }
                        o++;
                    }
                }
            }
        }


        void update() {
            // Color cell according to obstacles and explored
            for (int row=0; row < MapArr.length; row++) {
                for (int col=0; col < MapArr[0].length; col++) {
                    int code = MapArr[row][col];
                    if (code == unexploredNo) {
                        changeCellColor(context, ContextCompat.getColor(context, R.color.unexplored), row, col);
                    } else if (code == exploredNo) {
                        changeCellColor(context, ContextCompat.getColor(context, R.color.explored), row, col);
                    } else  if (code == obstaclesNo) {
                        changeCellColor(context, ContextCompat.getColor(context, R.color.obstacle), row, col);
                    }
                }
            }

            final RelativeLayout relativeLayoutOne = ((Activity) context).findViewById(R.id.gridMap3DOne);
            final RelativeLayout relativeLayoutTwo = ((Activity) context).findViewById(R.id.gridMap3DTwo);
            if(usingFirstLayout){
                myGlSurfaceView openGLView = new myGlSurfaceView(context, MapArr);
                relativeLayoutOne.removeAllViews();
                relativeLayoutOne.addView(openGLView);
            } else {
                myGlSurfaceView openGLView = new myGlSurfaceView(context, MapArr);
                relativeLayoutTwo.removeAllViews();
                relativeLayoutTwo.addView(openGLView);
            }

            Handler handler2 = new Handler();
            handler2.postDelayed(new Runnable(){
                public void run(){
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

        public static int[][] getMapArr() {
            return MapArr;
        }
    }

    public interface FacingDirection {
        int SOUTH = 0;
        int WEST = 90;
        int NORTH = 180;
        int EAST = 270;
    }

    public static class RobotDescriptor {
        private static int rowNumberPrevious2 = -1;
        private static int columnNumberPrevious2 = -1;
        private static int faceAnglePrevious2 = -1;

        private static int rowNumberPrevious = -1;
        private static int columnNumberPrevious = -1;
        private static int faceAnglePrevious = -1;
        private static int rowNumber = -1;
        private static int columnNumber = -1;
        private static int faceAngle = -1;

        RobotDescriptor(int rowNumber, int columnNumber, int faceAngleNumber) {
            if(rowNumber != -1 && columnNumber != -1){
                rowNumberPrevious = RobotDescriptor.rowNumber;
                columnNumberPrevious = RobotDescriptor.columnNumber;
                faceAnglePrevious = RobotDescriptor.faceAngle;
            }

            RobotDescriptor.rowNumber = rowNumber;
            RobotDescriptor.columnNumber = columnNumber;
            RobotDescriptor.faceAngle = faceAngleNumber;
        }

        void fromString(String rowString, String columnString, String faceAngleString) {
            rowNumberPrevious2 = rowNumberPrevious;
            columnNumberPrevious2 = columnNumberPrevious;
            faceAnglePrevious2 = faceAnglePrevious;

            rowNumberPrevious = RobotDescriptor.rowNumber;
            columnNumberPrevious = RobotDescriptor.columnNumber;
            faceAnglePrevious = RobotDescriptor.faceAngle;

            rowNumber = parseInt(rowString);
            columnNumber = parseInt(columnString);
            switch (faceAngleString) {
                case "S":
                    faceAngle = FacingDirection.SOUTH;
                    break;
                case "W":
                    faceAngle = FacingDirection.WEST;
                    break;
                case "N":
                    faceAngle = FacingDirection.NORTH;
                    break;
                case "E":
                    faceAngle = FacingDirection.EAST;
                    break;
            }
        }

        public static void setRowNumber(int rowNumber) {
            RobotDescriptor.rowNumber = rowNumber;
        }

        public static void setColumnNumber(int columnNumber) {
            RobotDescriptor.columnNumber = columnNumber;
        }

        public static void setFaceAngle(int faceAngle) {
            RobotDescriptor.faceAngle = faceAngle;
        }

        public static int getFaceAngle() {
            return faceAngle;
        }

        public static int getRowNumber() {
            return rowNumber;
        }

        public static int getColumnNumber() {
            return columnNumber;
        }

        public static int getFaceAnglePrevious() {
            return faceAnglePrevious;
        }

        public static int getRowNumberPrevious() {
            return rowNumberPrevious;
        }

        public static int getColumnNumberPrevious() {
            return columnNumberPrevious;
        }

        public static int getRowNumberPrevious2() {
            return rowNumberPrevious2;
        }

        public static int getColumnNumberPrevious2() {
            return columnNumberPrevious2;
        }

        public static int getFaceAnglePrevious2() {
            return faceAnglePrevious2;
        }
    }

    public static class ArrowDescriptor {
        static ArrayList<Integer> rotationAngle;
        static ArrayList<Integer> rowNumber;
        static ArrayList<Integer> columnNumber;
        ArrayList<Integer> rowNumberRemove;
        ArrayList<Integer> columnNumberRemove;
        static ArrayList<String> whichGrid;
        Context context;
        static Boolean haveArrowInPreviousGrid = false;
        static int arrowStepCounter = 0, robotSavedRow = -1, robotSavedCol = -1, robotSavedFacing = -1;

        public static ArrayList<Integer> getRowNumber(){
            return rowNumber;
        }

        public static ArrayList<Integer> getColumnNumber(){
            return columnNumber;
        }

        public static ArrayList<Integer> getRotationAngle(){
            return rotationAngle;
        }

        ArrowDescriptor(Context c) {
            context = c;
            rowNumber = new ArrayList<>();
            columnNumber = new ArrayList<>();
            rotationAngle = new ArrayList<>();
            rowNumberRemove = new ArrayList<>();
            columnNumberRemove= new ArrayList<>();
            whichGrid = new ArrayList<>();
        }

        void addArrowFromString(String haveArrow){
            haveArrow = haveArrow.toUpperCase();

            //if no arrow
            if(haveArrow.equals("0")){
                if(haveArrowInPreviousGrid){
                    //if not calibrating, increase arrow step counter
                    if(!(robotSavedRow == RobotDescriptor.getRowNumber() &&
                            robotSavedCol == RobotDescriptor.getColumnNumber() &&
                            robotSavedFacing == RobotDescriptor.getFaceAngle())){
                        arrowStepCounter++;
                        if(arrowStepCounter >= 2) {
                            arrowStepCounter = 0;
                            haveArrowInPreviousGrid = false;
                        }
                    }
                }
                return;
            }

            //if have arrow
            if(haveArrow.equals("F") || haveArrow.equals("M") || haveArrow.equals("B")){

                String directionStr = "error";
                switch (RobotDescriptor.getFaceAngle()){
                    case FacingDirection.NORTH:
                        directionStr = "N";
                        break;
                    case FacingDirection.SOUTH:
                        directionStr = "S";
                        break;
                    case FacingDirection.WEST:
                        directionStr = "W";
                        break;
                    case FacingDirection.EAST:
                        directionStr = "E";
                        break;
                }

                //if calibrating, return
                if(robotSavedRow == RobotDescriptor.getRowNumber() &&
                        robotSavedCol == RobotDescriptor.getColumnNumber() &&
                        robotSavedFacing == RobotDescriptor.getFaceAngle()){
                    return;
                }

                //if detected arrow recently (2 step), ignore arrow
                if(haveArrowInPreviousGrid){
                    arrowStepCounter++;
                    if(arrowStepCounter >= 2) {
                        arrowStepCounter = 0;
                        haveArrowInPreviousGrid = false;
                        GridMapFragment.addTextToStatusWindow((Activity) context, haveArrow + "|" +
                                directionStr + "|" +
                                RobotDescriptor.getRowNumber() + "|"+
                                RobotDescriptor.getColumnNumber() + " (rejected)");
                    }
                    return;
                }
                haveArrowInPreviousGrid = true;
                robotSavedCol = RobotDescriptor.getColumnNumber();
                robotSavedRow = RobotDescriptor.getRowNumber();
                robotSavedFacing = RobotDescriptor.getFaceAngle();

                GridMapFragment.addTextToStatusWindow((Activity) context, haveArrow + "|" +
                                                                directionStr + "|" +
                                                                RobotDescriptor.getRowNumber() + "|"+
                                                                RobotDescriptor.getColumnNumber());
                whichGrid.add(haveArrow);
                //for debugging
                int isPrevious = -1;
                int facing = 0;
                int row = 0;
                int col = 0;
                if(isPrevious == 0){
                    facing = RobotDescriptor.getFaceAngle();
                    row = RobotDescriptor.getRowNumber();
                    col = RobotDescriptor.getColumnNumber();
                }
                else if(isPrevious == -1){
                    facing = RobotDescriptor.getFaceAnglePrevious();
                    row = RobotDescriptor.getRowNumberPrevious();
                    col = RobotDescriptor.getColumnNumberPrevious();
                }

                else if(isPrevious == -2){
                    facing = RobotDescriptor.getFaceAnglePrevious2();
                    row = RobotDescriptor.getRowNumberPrevious2();
                    col = RobotDescriptor.getColumnNumberPrevious2();
                }

                rowNumber.add(row);
                columnNumber.add(col);
                switch (facing){
                    case FacingDirection.NORTH:
                        rotationAngle.add(FacingDirection.WEST);
                        break;
                    case FacingDirection.SOUTH:
                        rotationAngle.add(FacingDirection.EAST);
                        break;
                    case FacingDirection.EAST:
                        rotationAngle.add(FacingDirection.NORTH);
                        break;
                    case FacingDirection.WEST:
                        rotationAngle.add(FacingDirection.SOUTH);
                        break;
                }
            }
        }

        void removeArrowFromString(String row, String col){
            rowNumberRemove.add(Integer.parseInt(row));
            columnNumberRemove.add(Integer.parseInt(col));
        }

        void updateArrow(){

            //remove all arrows
            //if(GridMapFragment.isExploring) {
                for (int r = 0; r < 20; r++) {
                    for (int c = 0; c < 15; c++) {
                        //GridMapHandler2D.removeArrowPicture(context, r, c);
                    }
                }
            //}
            for(int i = 0; i < rotationAngle.size(); i++){
                String direction = "";
                int rowOffset = 0;
                int colOffset = 0;
                int rowGridOffset = 0;
                int colGridOffset = 0;

                int gridOffset = 0;
                switch (whichGrid.get(i)){
                    case"F":
                        gridOffset = 1;
                        break;
                    case"M":
                        gridOffset = 0;
                        break;
                    case"B":
                        gridOffset = -1;
                        break;
                }

                //set offset
                switch (rotationAngle.get(i)){
                    case FacingDirection.NORTH:
                        rowOffset = -1;
                        //colOffset = -1;
                        colGridOffset = gridOffset;
                        direction = "D";
                        break;
                    case FacingDirection.SOUTH:
                        rowOffset = 1;
                        //colOffset = 1;
                        colGridOffset = -gridOffset;
                        direction = "U";
                        break;
                    case FacingDirection.EAST:
                        colOffset = 1;
                        //rowOffset = -1;
                        rowGridOffset = gridOffset;
                        direction = "L";
                        break;
                    case FacingDirection.WEST:
                        colOffset = -1;
                        //rowOffset = 1;
                        rowGridOffset = -gridOffset;
                        direction = "R";
                        break;

                }

                //Algo to fix image on nearest wall
                try{
                    int[][] mapArr = MapDescriptor.getMapArr();
                    int row = rowNumber.get(i);
                    int col = columnNumber.get(i);
                    Boolean haveArrow = false;

                    for(int j = 0; j < 20; j++){
                        if(haveArrow)break;
                        for(int k = 0; k < 15; k++){
                            if(mapArr[row + j*rowOffset + rowGridOffset][col + k*colOffset + colGridOffset] == 2){
                                setArrowPicture(context, rotationAngle.get(i),
                                        row + j*rowOffset + rowGridOffset,
                                        col + k*colOffset + colGridOffset);
                                haveArrow = true;
                                rowNumber.set(i,row + j*rowOffset);
                                columnNumber.set(i,col + k*colOffset);
                                break;
                            }
                        }
                    }
                    if(!haveArrow){
                        rotationAngle.remove(i);
                        rowNumber.remove(i);
                        columnNumber.remove(i);
                        whichGrid.remove(i);
                    }
                }catch (Exception e){
                    rotationAngle.remove(i);
                    rowNumber.remove(i);
                    columnNumber.remove(i);
                    whichGrid.remove(i);
                }
            }
        }
    }

    private static int counter = 0;
    public void decodeMessage(Context context, String message) {
        String[] decoded = message.split("\\|");
        if (decoded.length > 0) {
            String header = decoded[0].toUpperCase();

            switch (header) {
                case "MDF":
                    if(GridMapFragment.isExploring){
                        MDFArrayList.add(message);
                        //save MDF string
                        String strToSave = "";
                        for(int i = 0 ; i < MDFArrayList.size(); i++){
                            strToSave += MDFArrayList.get(i) + "divider";
                        }
                        SharedPreferences.Editor editor = context.getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE).edit();
                        editor.putString("MDFArrayList", strToSave);
                        editor.apply();
                    }
                    map.fromString(decoded[1],decoded[2]);
                    robot.fromString(decoded[4], decoded[5], decoded[3]);
                    try{
                        arrow.addArrowFromString(decoded[6]);
                    }catch (Exception e){}

                    fullMapStr = decoded[1];
                    obstaclesStr = decoded[2];
                    if (this.isAutoMode) {
                        this.updateAll(context);
                    }
                    break;
                case "F":
                    fastestPathWithDirection(context, decoded[1]);
                    break;
                case "MDF STRING DONE":
                    //for simulator
                case "MDF STRING DON0":
                case "MDF STRING DONF":
                case "MDF STRING DONM":
                case "MDF STRING DONB":
                    explorationDone(context);
                    break;
                //case "ARW":
                //arrow.addArrowFromString(decoded[1], decoded[2],decoded[3]);
                //break;

                // case "ARWR":
                // arrow.removeArrowFromString(decoded[2], decoded[3]);
                // break;
            }
        }
    }

    public static void explorationDone(Context context){
        GridMapFragment.isExploring = false;
        if(GridMapFragment.timer != null) {
            GridMapFragment.timer.cancel();
            GridMapFragment.timer = null;
        }

        TextView textView = ((Activity)context).findViewById(R.id.exploreBtn);
        if(GridMapUpdateManager.MDFArrayList.size() > 0){
            Button playbackForward = ((Activity) context).findViewById(R.id.playback_forward);
            Button playbackBackward = ((Activity) context).findViewById(R.id.playback_backward);
            playbackForward.setEnabled(true);
            playbackBackward.setEnabled(true);
            GridMapFragment.playbackCounter = MDFArrayList.size() - 1;
        }

        String timeTaken = textView.getText().toString();
        textView.setText("Explore");
        textView.setEnabled(true);

        GridMapFragment.addTextToStatusWindow((Activity) context,
                "==Exploration Done==");
        GridMapFragment.addTextToStatusWindow((Activity) context,
                "Time taken : " + timeTaken);
        GridMapFragment.addTextToStatusWindow((Activity) context,
                "Part 1\n"+ fullMapStr);
        GridMapFragment.addTextToStatusWindow((Activity) context,
                "Part 2\n" + obstaclesStr);
        GridMapFragment.addTextToStatusWindow((Activity) context,
                "\nImage Position:");

        String imagePositionStr = "";
        for (int r = 0; r < 20; r++) {
            for (int c = 0; c < 15; c++) {
                String direction = "";
                switch (GridMapHandler2D.getGridArrowRotation(context, r, c)){
                    case FacingDirection.NORTH:
                        direction = "D";
                        break;
                    case FacingDirection.SOUTH:
                        direction = "U";
                        break;
                    case FacingDirection.WEST:
                        direction = "R";
                        break;
                    case FacingDirection.EAST:
                        direction = "L";
                        break;
                    case -1:
                        continue;
                }

                imagePositionStr += "(" + c + "," +
                        (19 - r) + "," + direction + ") , ";
            }
        }

        if(imagePositionStr.length() != 0){
            imagePositionStr = imagePositionStr.substring(0, imagePositionStr.length()-2);
        }
        else {
            imagePositionStr = "none!";
        }
        GridMapFragment.addTextToStatusWindow((Activity) context,
                imagePositionStr);
    }

    private String getDirectionString(int rowNumber, int columnNumber){
        int[][] mapArr = MapDescriptor.getMapArr();
        try {
            if (mapArr[rowNumber - 1][columnNumber] == 1) {
                return "U";
            }
        }catch (Exception ignored){}
        try {
            if (mapArr[rowNumber + 1][columnNumber] == 1){
                return "D";
            }
        }catch (Exception ignored){}
        try {
            if (mapArr[rowNumber][columnNumber + 1] == 1) {
                return "R";
            }
        }catch (Exception ignored){}
        try {
            if (mapArr[rowNumber][columnNumber - 1] == 1) {
                return "L";
            }
        }catch (Exception ignored){}
        return "U";
    }
    //F|REME9ME3RNRWMW4RNMN5REME4RNMN9MN3
    private void fastestPathWithNSEW(final Context c, final String fastStr){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(counter >= fastStr.length()){
                    return;
                }
                int row = RobotDescriptor.getRowNumber();
                int col = RobotDescriptor.getColumnNumber();
                String direction = String.valueOf(fastStr.charAt(counter+1));
                char fastChar = fastStr.charAt(counter);
                if(fastChar == 'M'){

                    switch (direction) {
                        case "S":
                            row += Integer.parseInt(String.valueOf(fastStr.charAt(counter+2)));
                            break;
                        case "W":
                            col -= Integer.parseInt(String.valueOf(fastStr.charAt(counter+2)));
                            break;
                        case "N":
                            row -= Integer.parseInt(String.valueOf(fastStr.charAt(counter+2)));
                            break;
                        case "E":
                            col += Integer.parseInt(String.valueOf(fastStr.charAt(counter+2)));
                            break;
                    }
                    robot.fromString(String.valueOf(row), String.valueOf(col),direction);
                    counter += 3;
                }else if(fastChar == 'R'){
                    robot.fromString(String.valueOf(row), String.valueOf(col),direction);
                    counter += 2;
                }

                if (isAutoMode) {
                    updateAll(c);
                }
                handler.postDelayed(this, 300);
            }
        }, 300);
    }
    //F|LLS4RS5RS4LS9S3
    private void fastestPathWithDirection(final Context c, final String fastStr){
        if(counter != 0)
            return;
        int firstDelay = 0;
        final int rotateDelay = 500, moveDelay = 300, stepDelay = 200;
        if(fastStr.charAt(0) == 'L' || fastStr.charAt(0) == 'R'){
            firstDelay = 500;
        } else if(fastStr.charAt(0) == 'S' ){
            int steps = Integer.parseInt(String.valueOf(fastStr.charAt(1)));
            firstDelay = moveDelay + steps * stepDelay;
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(counter >= fastStr.length()){
                    counter = 0;
                    if(RobotDescriptor.getRowNumber() == GridMapHandler2D.robotWaypointRow &&
                            RobotDescriptor.getColumnNumber() == GridMapHandler2D.robotWaypointColumn){
                        setRobotWaypointPosition(c, -2, -2);//remove robot waypoint forever
                    }
                    return;
                }
                int row = RobotDescriptor.getRowNumber();
                int col = RobotDescriptor.getColumnNumber();
                int direction = RobotDescriptor.getFaceAngle();
                char fastChar = fastStr.charAt(counter);
                switch (fastChar) {
                    case 'L':
                        direction -= 90;
                        if(direction == -90)
                            direction = 270;
                        break;
                    case 'R':
                        direction = (direction + 90) % 360;
                        break;
                    case 'S':
                        int steps = Integer.parseInt(String.valueOf(fastStr.charAt(counter+1)));
                        switch (direction) {
                            case FacingDirection.SOUTH:
                                row += steps;
                                break;
                            case FacingDirection.WEST:
                                col -= steps;
                                break;
                            case FacingDirection.NORTH:
                                row -= steps;
                                break;
                            case FacingDirection.EAST:
                                col += steps;
                                break;
                        }
                        break;
                }
                String directionStr = "";
                switch (direction){
                    case FacingDirection.NORTH:
                        directionStr = "N";
                        break;
                    case FacingDirection.SOUTH:
                        directionStr = "S";
                        break;
                    case FacingDirection.WEST:
                        directionStr = "W";
                        break;
                    case FacingDirection.EAST:
                        directionStr = "E";
                        break;
                }
                robot.fromString(String.valueOf(row), String.valueOf(col),directionStr);

                if (isAutoMode) {
                    updateAll(c);
                }

                //create delay
                int delay = 0;
                if(fastChar == 'L' || fastChar == 'R'){
                    delay = rotateDelay;
                    counter++;
                } else if(fastChar == 'S' ){
                    int steps = Integer.parseInt(String.valueOf(fastStr.charAt(counter+1)));
                    delay = moveDelay + steps * stepDelay;
                    counter += 2;
                }
                handler.postDelayed(this, delay);
            }
        }, firstDelay);
    }
}
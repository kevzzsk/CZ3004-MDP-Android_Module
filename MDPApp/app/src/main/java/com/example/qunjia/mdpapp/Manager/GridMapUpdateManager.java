package com.example.qunjia.mdpapp.Manager;


import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RelativeLayout;


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
 * This class handles decoding of incoming messages from Raspberry Pi,
 * buffering of latest map descriptor string and conversion of raw coordinates
 * (from Raspberry Pi) into row number and column number.
 */
public class GridMapUpdateManager {
    private boolean isAutoMode = true;
    private MapDescriptor map;
    private RobotDescriptor robot;
    private static ArrowDescriptor arrow;

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

    private static class MapDescriptor {
        int[][] MapArr;
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
            full_map = new BigInteger(full_map, 16).toString(2);
            full_map = full_map.substring(2, full_map.length()-2);

            obstacles = "F" + obstacles;//add "F" to prevent BigInteger from removing 0s from the front of binary string.
            obstacles = new BigInteger(obstacles, 16).toString(2);
            obstacles = obstacles.substring(4);

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
    }

    public static interface FacingDirection {
        final static int SOUTH = 0;
        final static int WEST = 90;
        final static int NORTH = 180;
        final static int EAST = 270;
    }

    public static class RobotDescriptor {
        private static int rowNumber;
        private static int columnNumber;
        private static int faceAngle;

        RobotDescriptor(int rowNumber, int columnNumber, int faceAngleNumber) {
            RobotDescriptor.rowNumber = rowNumber;
            RobotDescriptor.columnNumber = columnNumber;
            RobotDescriptor.faceAngle = faceAngleNumber;
        }

        void fromString(String rowString, String columnString, String faceAngleString) {
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

        public static int getFaceAngle() {
            return faceAngle;
        }

        public static int getRowNumber() {
            return rowNumber;
        }

        public static int getColumnNumber() {
            return columnNumber;
        }
    }

    private static class ArrowDescriptor {
        ArrayList<Integer> rotationAngle;
        ArrayList<Integer> rowNumber;
        ArrayList<Integer> columnNumber;
        ArrayList<Integer> rowNumberRemove;
        ArrayList<Integer> columnNumberRemove;
        Context context;

        ArrowDescriptor(Context c) {
            context = c;
            rowNumber = new ArrayList<>();
            columnNumber = new ArrayList<>();
            rotationAngle = new ArrayList<>();
            rowNumberRemove = new ArrayList<>();
            columnNumberRemove= new ArrayList<>();
        }

        /*void addArrowFromString(String direction,String row, String col){
            switch (direction){
                case "S":
                    rotationAngle.add(FacingDirection.SOUTH);
                    break;
                case "W":
                    rotationAngle.add(FacingDirection.WEST);
                    break;
                case "N":
                    rotationAngle.add(FacingDirection.NORTH);
                    break;
                case "E":
                    rotationAngle.add(FacingDirection.EAST);
                    break;
            }
            rowNumber.add(Integer.parseInt(row));
            columnNumber.add(Integer.parseInt(col));
        }*/

        void addArrowFromString(String haveArrow){
            if(haveArrow.equals("1")){
                int offset = 2;

                switch (RobotDescriptor.getFaceAngle()){
                    case FacingDirection.NORTH:
                        rowNumber.add(RobotDescriptor.getRowNumber());
                        columnNumber.add(RobotDescriptor.getColumnNumber() - offset);
                        rotationAngle.add(0);
                        break;
                    case FacingDirection.SOUTH:
                        rowNumber.add(RobotDescriptor.getRowNumber());
                        columnNumber.add(RobotDescriptor.getColumnNumber() + offset);
                        rotationAngle.add(180);
                        break;
                    case FacingDirection.EAST:
                        rowNumber.add(RobotDescriptor.getRowNumber() - offset);
                        columnNumber.add(RobotDescriptor.getColumnNumber());
                        rotationAngle.add(90);
                        break;
                    case FacingDirection.WEST:
                        rowNumber.add(RobotDescriptor.getRowNumber() + offset);
                        columnNumber.add(RobotDescriptor.getColumnNumber());
                        rotationAngle.add(270);
                        break;
                }
            }
        }

        void removeArrowFromString(String row, String col){
            rowNumberRemove.add(Integer.parseInt(row));
            columnNumberRemove.add(Integer.parseInt(col));
        }

        void updateArrow(){
            while(rowNumberRemove.size()>0){
                GridMapHandler2D.removeArrowPicture(context, rowNumberRemove.get(0), columnNumberRemove.get(0));
                rowNumberRemove.remove(0);
                columnNumberRemove.remove(0);
            }
            while(rotationAngle.size()>0){
                setArrowPicture(context, rotationAngle.get(0), rowNumber.get(0), columnNumber.get(0));
                rotationAngle.remove(0);
                rowNumber.remove(0);
                columnNumber.remove(0);
            }
        }
    }

    public void decodeMessage(Context context, String message) {
        String[] decoded = message.split("\\|");
        if (decoded.length > 0) {
            String header = decoded[0];

            switch (header) {
                case "MDF":
                    map.fromString(decoded[1],decoded[2]);
                    robot.fromString(decoded[4], decoded[5], decoded[3]);
                    arrow.addArrowFromString(decoded[6]);
                    break;
                //case "ARW":
                    //arrow.addArrowFromString(decoded[1], decoded[2],decoded[3]);
                    //break;

               // case "ARWR":
                   // arrow.removeArrowFromString(decoded[2], decoded[3]);
                   // break;
            }

            if (this.isAutoMode) {
                this.updateAll(context);
            }
        }
    }
}

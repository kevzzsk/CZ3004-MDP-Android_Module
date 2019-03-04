package com.example.qunjia.mdpapp;


import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;


import com.example.qunjia.mdpapp.OpenGL.myGlSurfaceView;
import com.example.qunjia.mdpapp.OpenGL.myRenderer;

import java.math.BigInteger;

import static com.example.qunjia.mdpapp.GridMapHandler2D.*;
import static java.lang.Integer.parseInt;

/**
 * This class handles decoding of incoming messages from Raspberry Pi,
 * buffering of latest map descriptor string and conversion of raw coordinates
 * (from Raspberry Pi) into row number and column number.
 */
class GridMapUpdateManager {
    private boolean isAutoMode = true;

    private MapDescriptor map;
    private RobotDescriptor robot;
    private ArrowDescriptor arrow;

    GridMapUpdateManager (Context context) {
        robot = new RobotDescriptor(18, 1, FacingDirection.NORTH);
        map = new MapDescriptor(context);
        arrow = new ArrowDescriptor();
    }

    void toggleDisplayMode() {
        this.isAutoMode = !this.isAutoMode;
    }

    private static int calculate_delta(int current_direction, int target_direction) {
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

    void updateAll(Context context) {
        if (map != null) {
            map.update();
        }

        setRobotPosition(context, RobotDescriptor.rowNumber, RobotDescriptor.columnNumber);
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
            //SetArrowPicture(context, arrow.rotationAngle, arrow.rowNumber, arrow.columnNumber);
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
                if((col) % 15 == 14)
                    row--;
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

    static interface FacingDirection {
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

        static int getRowNumber() {
            return rowNumber;
        }

        static int getColumnNumber() {
            return columnNumber;
        }
    }

    private static class ArrowDescriptor {
        int rotationAngle;
        int rowNumber;
        int columnNumber;

        ArrowDescriptor() {
            this.rotationAngle = 90;
            this.rowNumber = 5;
            this.columnNumber = 5;
        }

        void fromString(String message){
            // TODO: decode message
        }
    }

    void decodeMessage(Context context, String message) {
        String[] decoded = message.split("\\|");
        if (decoded.length > 0) {
            String header = decoded[0];

            switch (header) {
                case "MDF":
                    map.fromString(decoded[1],decoded[2]);
                    robot.fromString(decoded[4], decoded[5], decoded[3]);
                    break;
                case "ARW":
                    // TODO: decode arrow message
                    arrow.fromString(message);
                    break;
            }

            if (this.isAutoMode) {
                this.updateAll(context);
            }
        }
    }
}

package com.example.qunjia.mdpapp;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
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

    GridMapUpdateManager () {
        robot = new RobotDescriptor(18, 1/*, 180*/);
        map = new MapDescriptor();
        arrow = new ArrowDescriptor();
    }

    void toggleDisplayMode() {
        this.isAutoMode = !this.isAutoMode;
    }

    void updateAll(Context context) {
        if (map != null) {
            map.update(context);
        }
        SetRobotPosition(context, RobotDescriptor.rowNumber, RobotDescriptor.columnNumber);
        myRenderer.setX(-RobotDescriptor.columnNumber);
        myRenderer.setZ(-RobotDescriptor.rowNumber);
        if (arrow != null) {
            SetArrowPicture(context, arrow.rotationAngle, arrow.rowNumber, arrow.columnNumber);
        }
    }

    private static class MapDescriptor {
        int[][] MapArr;
        Boolean usingFirstLayout; //for 3D map

        MapDescriptor() {
            MapArr = new int[20][15];
            usingFirstLayout = true;
        }

        void setMapArr(String full_map, String obstacles){
            // TODO: (full map) convert hex to binary

            full_map = new BigInteger(full_map, 16).toString(2);
            full_map = full_map.substring(2, full_map.length()-2);

            obstacles = "F" + obstacles;//prevent BigInteger from removing 0s from the front of binary string.
            obstacles = new BigInteger(obstacles, 16).toString(2);
            obstacles = obstacles.substring(4);

            int row = 0;
            for(int col = 0; col < full_map.length(); col++){
                MapArr[row][(col)%15] = Character.getNumericValue(full_map.charAt(col));
                if((col) % 15 == 14)
                    row++;
            }

            int o = 0;
            for (int r = 0; r < MapArr.length; r++) {
                for (int c = 0; c < MapArr[r].length; c++) {
                    if (MapArr[r][c] == 1) {
                        if (Character.getNumericValue(obstacles.charAt(o)) == 1) {
                            MapArr[r][c]++;
                        }
                        o++;
                    }
                }
            }

            // TODO: (explored region) convert hex to binary

        }

        void update(Context context) {
            // Color cell according to obstacles and explored
            for (int row=0; row < MapArr.length; row++) {
                for (int col=0; col < MapArr[0].length; col++) {
                    int code = MapArr[row][col];
                    if (code == 0) {
                        ChangeCellColor(context, ContextCompat.getColor(context, R.color.unexplored), row, col);
                    } else if (code == 1) {
                        ChangeCellColor(context, ContextCompat.getColor(context, R.color.explored), row, col);
                    } else  if (code == 2) {
                        ChangeCellColor(context, ContextCompat.getColor(context, R.color.obstacle), row, col);
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

    public static class RobotDescriptor {
        private static int rowNumber;
        private static int columnNumber;
        //private static int faceAngle;

        RobotDescriptor(int rowNumber, int columnNumber/*, int faceAngleNumber*/) {
            RobotDescriptor.rowNumber = rowNumber;
            RobotDescriptor.columnNumber = columnNumber;
            //RobotDescriptor.faceAngle = faceAngleNumber;
        }

        void update(String rowString, String columnString /*,String faceAngleString*/) {
            rowNumber = parseInt(rowString);
            columnNumber = parseInt(columnString);
            //faceAngle = parseInt(faceAngleString);
        }

        public static int getRowNumber() {
            return rowNumber;
        }

        public static int getColumnNumber() {
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

        void setMessage(String message){
            // TODO: decode message
        }
    }

    void decodeMessage(Context context, String message) {
        String[] decoded = message.split("\\|");
        if (decoded.length > 0) {
            String header = decoded[0];

            switch (header) {
                case "MDF":
                    map.setMapArr(decoded[1],decoded[2]);
                    // robot position
                    robot.update(decoded[4], decoded[5]);
                    //robot.update(decoded[4], decoded[5], "180");

                    break;
                case "ARW":
                    // TODO: decode arrow message
                    arrow.setMessage(message);

                    break;
            }

            if (this.isAutoMode) {
                this.updateAll(context);
            }
        }
    }
}

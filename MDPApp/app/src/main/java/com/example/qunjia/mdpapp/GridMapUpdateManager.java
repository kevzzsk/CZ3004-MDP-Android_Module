package com.example.qunjia.mdpapp;


import android.content.Context;


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
        robot = new RobotDescriptor(18, 1);
    }

    void toggleDisplayMode() {
        this.isAutoMode = !this.isAutoMode;
    }

    void updateAll(Context context) {
        if (map != null) {
            map.update(context);
        }
        SetRobotPosition(context, robot.columnNumber, robot.rowNumber);
        if (arrow != null) {
            SetArrowPicture(context, arrow.rotationAngle, arrow.rowNumber, arrow.columnNumber);
        }
    }

    private static class MapDescriptor {
        int[][] obstaclesArr = new int[20][15];

        MapDescriptor(String full_map, String obstacles) {

            // TODO: (full map) convert hex to binary
            full_map = new BigInteger(full_map, 16).toString(2);
            obstacles = new BigInteger(obstacles, 16).toString(2);

            int row = 0;
            for(int col = 2; col < full_map.length() -2 ; col++){
                exploredArr[row][(col-2)%15] = Character.getNumericValue(full_map.charAt(col));
                if((col-2) % 15 == 14)
                    row++;
            }

            // TODO: (explored region) convert hex to binary

        }

        void update(Context context) {
            // Color cell according to obstacles and explored
            for (int row=0; row < obstaclesArr.length; row++) {
                for (int col=0; col < obstaclesArr[0].length; col++) {
                    int code = obstaclesArr[row][col];
                    if (code == 0) {
                        ChangeCellColor(context, R.color.unexplored, row, col);
                    } else if (code == 1) {
                        ChangeCellColor(context, R.color.explored, row, col);
                    } else  if (code == 2) {
                        ChangeCellColor(context, R.color.obstacle, row, col);
                    }
                }
            }
        }
    }

    private static class RobotDescriptor {
        int rowNumber;
        int columnNumber;

        RobotDescriptor(int rowNumber, int columnNumber) {
            this.rowNumber = rowNumber;
            this.columnNumber = columnNumber;
        }

        void update(String rowString, String columnString) {
            this.rowNumber = parseInt(rowString);
            this.columnNumber = parseInt(columnString);
        }
    }

    private static class ArrowDescriptor {
        int rotationAngle;
        int rowNumber;
        int columnNumber;

        ArrowDescriptor(String message) {
            // TODO: decode message

            this.rotationAngle = 90;
            this.rowNumber = 5;
            this.columnNumber = 5;
        }
    }

    void decodeMessage(Context context, String message) {
        String[] decoded = message.split("\\|");
        if (decoded.length > 0) {
            String header = decoded[0];

            switch (header) {
                case "MDF":
                    map = new MapDescriptor(decoded[1], decoded[2]);

                    // robot position
                    robot.update(decoded[4], decoded[5]);

                    break;
                case "ARW":
                    // TODO: decode arrow message
                    arrow = new ArrowDescriptor(message);

                    break;
            }

            if (this.isAutoMode) {
                this.updateAll(context);
            }
        }
    }
}

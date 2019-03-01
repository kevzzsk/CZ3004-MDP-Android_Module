package com.example.qunjia.mdpapp;


import android.content.Context;


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
        robot = new RobotDescriptor(0, 0);
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
        int[] exploredRows = new int[20];
        int[] exploredColumns = new int[15];

        int[] obstaclesRows = new int[20];
        int[] getObstaclesColumns = new int[15];

        MapDescriptor(String full_map, String explored) {
            // TODO: (full map) convert hex to binary

            // TODO: (explored region) convert hex to binary

        }

        void update(Context context) {
            // Color cell according to obstacles and explored
            ChangeCellColor(context, 255,  5, 5);
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
        String[] decoded = message.split("|");
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

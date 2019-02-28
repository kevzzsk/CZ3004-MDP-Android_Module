package com.example.qunjia.mdpapp;


import android.content.Context;


import static com.example.qunjia.mdpapp.GridMapHandler.*;

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

    private static int convertRow(int rowCoordinate) {
        int rowNumber = rowCoordinate;

        return rowNumber;
    }

    private static int convertColumn(int columnCoordinate) {
        int columnNumber = columnCoordinate;

        return columnNumber;
    }

    private static class MapDescriptor {
        int[] exploredRows;
        int[] exploredColumns;

        int[] obstaclesRows;
        int[] getObstaclesColumns;

        MapDescriptor(String msg) {
            // TODO: parse MDF String
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

        void update(String msg) {
            // TODO: decode message
            this.rowNumber = convertRow(5);
            this.columnNumber = convertColumn(5);
        }
    }

    private static class ArrowDescriptor {
        int rotationAngle;
        int rowNumber;
        int columnNumber;

        ArrowDescriptor(String message) {
            // TODO: decode message

            this.rotationAngle = 90;
            this.rowNumber = convertRow(5);
            this.columnNumber = convertColumn(5);
        }
    }

    void decodeMessage(Context context, String message) {
        // TODO: decode the header to determine next action

        map = new MapDescriptor(message);
        robot.update(message);
        arrow = new ArrowDescriptor(message);

        if (this.isAutoMode) {
            this.updateAll(context);
        }
    }
}

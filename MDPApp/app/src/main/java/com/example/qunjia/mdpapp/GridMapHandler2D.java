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
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.InputStream;

public class GridMapHandler2D {
    //robot IDs
    private final static int robotID = 3141592, robotWaypointID = 2951413;
    //Grid map Variables
    private static int cellSize;
    private final static int cellMargin = 2;
    public final static int rowTotalNumber = 20, columnTotalNumber = 15;

    private static int robotWaypointColumn, robotWaypointRow;

    public static void SetRobotPosition(Context context, int row, int column) {
        RelativeLayout relativeLayout = ((Activity) context).findViewById(R.id.gridMapFragmentLayout);

        //remove old robot text view
        TextView oldTV = ((Activity) context).findViewById(robotID);
        if(oldTV != null){
            relativeLayout.removeViewInLayout(oldTV);
        }

        if(row == -1 && column == -1){
            //remove both robot TV & waypoint TV
            SetRobotWaypointPosition(context, -1, -1);
            return;
        }
        else if(row > 19 && column > 14){
            row = GridMapUpdateManager.RobotDescriptor.getRowNumber();
            column = GridMapUpdateManager.RobotDescriptor.getColumnNumber();
            if(robotWaypointColumn != 0 && robotWaypointRow != 0){
                SetRobotWaypointPosition(context, robotWaypointColumn, robotWaypointRow);
            }
        }
        if(row == robotWaypointRow && column == robotWaypointColumn){
            SetRobotWaypointPosition(context, -1, -1);//remove robot waypoint
        }
        else if(GridMapFragment.is3Dmode){
            return;
        }

        //create new robot text view
        final TextView textView = new TextView(((Activity) context));
        textView.setLayoutParams(GetRobotLayoutParams((Activity) context, column, row));
        textView.setText("ROBOT");
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        textView.setId(robotID);
        textView.setBackgroundColor(Color.parseColor("#1100ce"));//blue

        relativeLayout.addView(textView);
    }

    public static void ChangeCellColor(Context context, int color, int rowNumber, int columnNumber) {
        try {
            ImageView imageView = ((Activity) context).findViewById(getImageViewID(columnNumber, rowNumber));
            imageView.setBackgroundColor(color);
        } catch (Exception e) {
            Toast.makeText(context, "Change Cell Color Error at row " + rowNumber + " col " + columnNumber, Toast.LENGTH_LONG).show();
        }
    }

    public static void SetArrowPicture(Context context, int rotationAngle, int rowNumber, int columnNumber) {
        try {
            ImageView imageView = ((Activity) context).findViewById(getImageViewID(columnNumber, rowNumber));
            AssetManager assetManager = context.getAssets();

            InputStream istr = assetManager.open("arrow.webp");
            Bitmap bitmap = BitmapFactory.decodeStream(istr);
            imageView.setImageBitmap(bitmap);
            imageView.setRotation(rotationAngle);
            istr.close();
        } catch (Exception e) {
            Toast.makeText(context, "Set arrow Error at row " + rowNumber + " col " + columnNumber, Toast.LENGTH_LONG).show();
        }
    }

    public static void RemoveArrowPicture(Context context, int rowNumber, int columnNumber) {
        try {
            ImageView imageView = ((Activity) context).findViewById(getImageViewID(columnNumber, rowNumber));
            imageView.setImageBitmap(null);
        } catch (Exception e) {
            Toast.makeText(context, "Set arrow Error at row " + rowNumber + " col " + columnNumber, Toast.LENGTH_LONG).show();
        }
    }

    //////you dunnid anything below this line/////


    //for waypoint only
    @SuppressLint("ClickableViewAccessibility")
    public static void SetRobotDragListener(Context context, Boolean enableListener){
        final TextView textView = ((Activity) context).findViewById(robotID);
        if(textView != null){
            if(enableListener){
                textView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        ClipData data = ClipData.newPlainText("", "");
                        View.DragShadowBuilder shadow = new View.DragShadowBuilder(textView);
                        view.startDrag(data, shadow, null, 0);
                        return false;
                    }
                });
            }else {
                textView.setOnTouchListener(null);
            }
        }
    }

    public static void CreateGridMap(final Context context) {
        TableLayout tbl = ((Activity) context).findViewById(R.id.gridMap2D);

        //Get cell size
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float gridMapWidth = (float) (size.x * 0.65);
        int cellWidth = (int) (gridMapWidth / columnTotalNumber);
        float gridMapHeight = (float) (size.y * 0.65);
        final int cellHeight = (int) (gridMapHeight / rowTotalNumber);
        if (cellHeight < cellWidth) cellSize = cellHeight;
        else cellSize = cellWidth;

        //Create Grid Map
        for (int i = 0; i < rowTotalNumber; i++) {
            //create a new tableRow with imageViews and add into tableLayout
            final TableRow row = new TableRow(context);
            row.setBackgroundColor(Color.BLACK);
            for (int j = 0; j < columnTotalNumber; j++) {
                //create new imageView and add into row
                final ImageView imageView = new ImageView(context);
                TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                        cellSize, cellSize);
                layoutParams.setMargins(0, 0, cellMargin, cellMargin);
                imageView.setLayoutParams(layoutParams);
                imageView.setBackgroundColor(Color.WHITE);
                imageView.setId(getImageViewID(j, i));
                imageView.setOnDragListener(new View.OnDragListener() {
                    @Override
                    public boolean onDrag(View view, DragEvent dragEvent) {
                        final int action = dragEvent.getAction();

                        switch (action) {
                            case DragEvent.ACTION_DROP:
                                int rowNumber = (int) view.getId() % 1000;
                                int columnNumber = (int) view.getId() / 1000;
                                if (rowNumber > 0 && rowNumber < rowTotalNumber - 1 && columnNumber > 0 &&
                                        columnNumber < columnTotalNumber - 1) {
                                    //display robot waypoint
                                    SetRobotWaypointPosition(context, columnNumber, rowNumber);

                                    //send string to bluetooth
                                    BluetoothFragment.sendMessage("C" + columnNumber + " R" + rowNumber);
                                    GridMapFragment.AddTextToStatusWindow((Activity) context,"C" + columnNumber + " R" + rowNumber);

                                    //toggle waypoint btn
                                    ToggleButton toggleButton = ((Activity) context).findViewById(R.id.waypointToggleBtn);
                                    toggleButton.toggle();
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

    private static RelativeLayout.LayoutParams GetRobotLayoutParams(Activity activity, int column, int row){
        int[] location = new int[2];
        ImageView robotCellPosition = activity.findViewById(getImageViewID(column, row));
        robotCellPosition.getLocationOnScreen(location);
        int x = location[0] - cellSize - cellMargin;
        int y = location[1] - cellSize * 4 - (int) (cellMargin * 8.5);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                cellSize * 3 + 2 * cellMargin, cellSize * 3 + 3 * cellMargin);
        params.setMargins(x, y, 0, 0);
        return params;
    }

    private static int getImageViewID(int columnNumber, int rowNumber) {
        return columnNumber * 1000 + rowNumber;
    }

    //for waypoint only
    private static void SetRobotWaypointPosition(Context context, int column, int row) {
        RelativeLayout relativeLayout = ((Activity) context).findViewById(R.id.gridMapFragmentLayout);

        //remove old robot text view
        TextView oldTV = ((Activity) context).findViewById(robotWaypointID);
        if(oldTV != null){
            relativeLayout.removeViewInLayout(oldTV);
        }

        //return if robot reached the waypoint
        if(row == -1 && column == -1){
            return;
        }
        robotWaypointRow = row;
        robotWaypointColumn = column;

        //create new robot text view
        final TextView textView = new TextView(((Activity) context));
        textView.setLayoutParams(GetRobotLayoutParams((Activity) context,column, row));
        textView.setText("ROBOT\nWAYPOINT");
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        textView.setId(robotWaypointID);
        textView.setBackgroundColor(Color.parseColor("#3281ff"));//light blue
        textView.setTextColor(Color.BLACK);

        relativeLayout.addView(textView);
    }
}

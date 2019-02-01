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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.andretietz.android.controller.DirectionView;
import com.andretietz.android.controller.InputView;

import java.io.InputStream;

public class GridMapFragment extends Fragment {

    private static int cellSize, robotCurrentRow = 18, robotCurrentColumn = 1;
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
        CreateGridMap(getActivity());
        DirectionViewSetup(getActivity());
        //
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setRobotPosition(getActivity(), 1,18);
            }
        }, 200);
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
                                    setRobotPosition(getActivity(),  columnNumber, rowNumber);
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
                        Toast.makeText(context, "DOWN", Toast.LENGTH_LONG).show();
                        break;
                    case DirectionView.DIRECTION_LEFT:
                        Toast.makeText(context, "LEFT", Toast.LENGTH_LONG).show();
                        break;
                    case DirectionView.DIRECTION_RIGHT:
                        Toast.makeText(context, "RIGHT", Toast.LENGTH_LONG).show();
                        break;
                    case DirectionView.DIRECTION_UP:
                        Toast.makeText(context, "UP", Toast.LENGTH_LONG).show();
                        break;
                    case DirectionView.DIRECTION_DOWN_LEFT:
                        Toast.makeText(context, "DOWN_LEFT", Toast.LENGTH_LONG).show();
                        break;
                    case DirectionView.DIRECTION_UP_LEFT:
                        Toast.makeText(context, "UP_LEFT", Toast.LENGTH_LONG).show();
                        break;
                    case DirectionView.DIRECTION_DOWN_RIGHT:
                        Toast.makeText(context, "DOWN_RIGHT", Toast.LENGTH_LONG).show();
                        break;
                    case DirectionView.DIRECTION_UP_RIGHT:
                        Toast.makeText(context, "UP_RIGHT", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void setRobotPosition(Context context, int columnNumber, int rowNumber){
        /*ChangeCellColor(context,Color.RED, rowNumber,columnNumber);
        ChangeCellColor(context,Color.RED, rowNumber - 1,columnNumber);
        ChangeCellColor(context,Color.RED, rowNumber + 1,columnNumber);
        ChangeCellColor(context,Color.RED, rowNumber,columnNumber - 1);
        ChangeCellColor(context,Color.RED, rowNumber,columnNumber + 1);
        ChangeCellColor(context,Color.RED, rowNumber - 1,columnNumber - 1);
        ChangeCellColor(context,Color.RED, rowNumber + 1,columnNumber + 1);
        ChangeCellColor(context,Color.RED, rowNumber - 1,columnNumber + 1);
        ChangeCellColor(context,Color.RED, rowNumber + 1,columnNumber - 1);*/

        RelativeLayout relativeLayout = ((Activity) context).findViewById(R.id.gridMapFragmentLayout);
        relativeLayout.removeAllViews();

        //create an transparent image view at robot new position
        final TextView textView = new TextView(((Activity) context));
        int[] location = new int[2];
        ImageView robotCellPosition = ((Activity) context).findViewById(getImageViewID(columnNumber,rowNumber));
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

        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ClipData data = ClipData.newPlainText("","" );
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(textView);
                view.startDrag(data,shadow,null, 0);
                return false;
            }
        });

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
            case R.id.extraBtn:
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

                            setRobotPosition(v.getContext(), robotCurrentColumn,robotCurrentRow);

                            handler.postDelayed(this, 100);
                        }
                    };
                    handler.postDelayed(runnable, 100);
                }

                break;
           /* case R.id.
                break;
            case R.id.
                break;*/
        }

        Toast.makeText(v.getContext(), "to be updated", Toast.LENGTH_LONG).show();
    }
}

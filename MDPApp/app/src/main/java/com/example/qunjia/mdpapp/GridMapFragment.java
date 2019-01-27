package com.example.qunjia.mdpapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class GridMapFragment extends Fragment {

    private static int positionNo;
    public GridMapFragment() {

    }

    public static GridMapFragment newInstance(int position) {
        positionNo = position;
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

        //for testing only
        ChangeCellColor(getActivity(),Color.BLACK, 5,5);
        ChangeCellColor(getActivity(),Color.BLACK, 6,5);
        ChangeCellColor(getActivity(),Color.BLACK, 5,4);
    }
    private void CreateGridMap (Activity activity){
        TableLayout tbl= activity.findViewById(R.id.gridMap);

        int rowNumber = 20, columnNumber = 15;
        int cellSize = GetCellSize(activity, rowNumber, columnNumber);

        for(int i=0; i<rowNumber; i++) {
            TableRow row = new TableRow(activity);
            row.setBackgroundColor(Color.BLACK);
            for(int j=0; j<columnNumber; j++) {
                ImageView imageView = CreateImageView(activity, cellSize);
                imageView.setId(j * 10 + i);
                row.addView(imageView);
            }
            row.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tbl.addView(row);
        }
    }

    private ImageView CreateImageView(Activity activity, int cellSize){
        ImageView imageView = new ImageView(activity);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                cellSize, cellSize);
        layoutParams.setMargins(0,0,2,2);
        imageView.setLayoutParams(layoutParams);
        imageView.setBackgroundColor(Color.WHITE);

        return imageView;
    }

    private int GetCellSize(Activity activity, int rowNumber, int columnNumber){

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float gridMapWidth = (float) (size.x * 0.65);
        int cellWidth = (int) (gridMapWidth / columnNumber);

        float gridMapHeight = (float) (size.y * 0.65);
        int cellHeight = (int) (gridMapHeight / rowNumber);

        if(cellHeight<cellWidth) return cellHeight;
        else return cellWidth;
    }

    public static void ChangeCellColor(Activity activity,int color, int rowNumber, int columnNumber){
        ImageView imageView = activity.findViewById(columnNumber * 10 + rowNumber);
        imageView.setBackgroundColor(color);
    }

    public static void myClickMethod(View v) {
        /*switch(v.getId()) {
            case R.id.
                break;
            case R.id.
                break;
            case R.id.
                break;
        }*/

        Toast.makeText(v.getContext(), "to be updated", Toast.LENGTH_LONG).show();
    }
}

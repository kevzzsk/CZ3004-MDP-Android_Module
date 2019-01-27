package com.example.qunjia.mdpapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ReconfigureFragment extends Fragment {

    private static int positionNo;
    public ReconfigureFragment() {

    }

    public static ReconfigureFragment newInstance(int position) {
        positionNo = position;
        ReconfigureFragment f = new ReconfigureFragment();
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
        return inflater.inflate(R.layout.fragment_reconfigure, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    }

    public static void myClickMethod(View v) {
        switch(v.getId()) {
            case R.id.F1Btn: F1BtnOnCLick(v.getContext());
                break;
            case R.id.F2Btn: F2BtnOnCLick(v.getContext());
                break;
            case R.id.reconfigBtn: ReconfigBtnOnCLick(v.getContext());
                break;
        }
    }

    public static void F1BtnOnCLick (Context context){
        String savedString = GetSharedPreferencesStr(context, "F1");
        Toast.makeText(context, savedString, Toast.LENGTH_LONG).show();
    }

    public static void F2BtnOnCLick (Context context){
        String savedString = GetSharedPreferencesStr(context, "F2");
        Toast.makeText(context, savedString, Toast.LENGTH_LONG).show();
    }

    public static void ReconfigBtnOnCLick (final Context context){

        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        final EditText input = new EditText(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder1.setView(input);

        builder1.setMessage("Write your message here.");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "F1",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SetSharedPreferencesStr(context, "F1", input.getText().toString());
                    }
                });

        builder1.setNegativeButton(
                "F2",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SetSharedPreferencesStr(context, "F2", input.getText().toString());
                    }
                });
        builder1.setNeutralButton(
                "Cancel",
                null);

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    private static String GetSharedPreferencesStr(Context context, String btnStr){
        SharedPreferences prefs = context.getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE);
        return prefs.getString(btnStr, btnStr); //return "F1" or "F2" by default
    }

    private static void SetSharedPreferencesStr(Context context, String btnStr, String strToSave){
        SharedPreferences.Editor editor = context.getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE).edit();
        editor.putString(btnStr, strToSave);
        editor.apply();
    }


}

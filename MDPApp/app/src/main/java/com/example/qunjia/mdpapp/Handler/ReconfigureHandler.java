package com.example.qunjia.mdpapp.Handler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.example.qunjia.mdpapp.Fragment.BluetoothFragment;
import com.example.qunjia.mdpapp.Fragment.GridMapFragment;

public class ReconfigureHandler {
    public static void F1BtnOnCLick (Context context){
        String savedString = GetSharedPreferencesStr(context, "F1");
        GridMapFragment.addTextToStatusWindow((Activity) context, savedString);
        BluetoothFragment.sendMessage(savedString);
    }

    public static void F2BtnOnCLick (Context context){
        String savedString = GetSharedPreferencesStr(context, "F2");
        GridMapFragment.addTextToStatusWindow((Activity) context, savedString);
        BluetoothFragment.sendMessage(savedString);
    }

    public static void reconfigBtnOnClick (final Context context){

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
                "F2",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SetSharedPreferencesStr(context, "F2", input.getText().toString());
                    }
                });

        builder1.setNegativeButton(
                "F1",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SetSharedPreferencesStr(context, "F1", input.getText().toString());
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

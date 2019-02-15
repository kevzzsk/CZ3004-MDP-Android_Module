package com.example.qunjia.mdpapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;

public class BluetoothFragment extends Fragment {
    // Intent request codes
    private static final int REQUEST_SCAN = 1;
    private static final int REQUEST_CONNECT = 2;
    private static ProgressDialog progressDialog = null;

    // Widgets
    private BluetoothDataAdapter mDataAdapter;

    // Bluetooth service
    private BluetoothAdapter mBluetoothAdapter;
    private static BluetoothService mBluetoothService;

    // Connection handler
    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case HandlerConstants.MESSAGE_STATE_CHANGE:
                    showLastConnected();
                    switch (msg.arg1) {
                        case ConnectionConstants.STATE_CONNECTED:
                            setConnectionStatus("Connected");
                            break;
                        case ConnectionConstants.STATE_CONNECTING:
                            setConnectionStatus("Connecting...");
                            break;
                        case ConnectionConstants.STATE_DISCONNECTED:
                            setConnectionStatus("Disconnected");
                            break;
                    }
                    break;
                case HandlerConstants.MESSAGE_CONNECTED_DEVICE:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    if (activity != null) {
                        saveLastConnectedDevice(activity, device);
                    }
                    break;
                case HandlerConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d("Message received", readMessage);
                    if (activity != null) {
                        TextView status_view = (TextView) activity.findViewById(R.id.statusWindowTV);
                        status_view.setText(status_view.getText() + readMessage + "\n");

                        try
                        {
                            // checking valid integer using parseInt() method
                            Integer.parseInt(readMessage);
                            GridMapFragment.GridMapBluetoothHandler(activity, readMessage);
                        }
                        catch (NumberFormatException e)
                        {/*do nothing*/}

                    }
                    break;
                case HandlerConstants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.d("Message sent", writeMessage);
                    break;
                case HandlerConstants.MESSAGE_DEVICE_FOUND:
                    BluetoothDevice found_device = (BluetoothDevice) msg.obj;
                    if (activity != null) {
                        String last_connected_MAC = getLastConnectedDevice(activity).first;
                        if (mDataAdapter != null &&
                                !(last_connected_MAC != null && last_connected_MAC.equals(found_device.getAddress()))) {
                            mDataAdapter.add(found_device);
                        } else if (mDataAdapter == null) {
                            Log.e(BluetoothService.BLUETOOTH_SCAN_TAG, "Data adapter is dereference!");
                        }
                    }
                    String deviceName = found_device.getName();
                    String deviceHardwareAddress = found_device.getAddress(); // MAC address
                    if (deviceName != null) {
                        Log.d(BluetoothService.BLUETOOTH_SCAN_TAG,
                                String.format("Found device (Name: %s)", deviceName));
                    } else {
                        Log.d(BluetoothService.BLUETOOTH_SCAN_TAG,
                                String.format("Found device (MAC address: %s)", deviceHardwareAddress));
                    }
                    break;
                case HandlerConstants.MESSAGE_START_DISCOVERY:
                    Log.d(BluetoothService.BLUETOOTH_SCAN_TAG, "Started Bluetooth scanning");
                    mDataAdapter.clear();
                    View view = getView();
                    if (view != null) {
                        getView().findViewById(R.id.bluetooth_scan_btn).setVisibility(View.GONE);
                        getView().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    }
                    break;
                case HandlerConstants.MESSAGE_FINISH_DISCOVERY:
                    Log.d(BluetoothService.BLUETOOTH_SCAN_TAG, "Finished Bluetooth scanning");
                    boolean reconnecting_ = (boolean) msg.obj;
                    View view_ = getView();
                    if (view_ != null) {
                        getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
                        getView().findViewById(R.id.bluetooth_scan_btn).setVisibility(View.VISIBLE);
                    }
                    break;
                case HandlerConstants.MESSAGE_DEVICE_BONDED:
                    BluetoothDevice bonded_device = (BluetoothDevice) msg.obj;
                    mDataAdapter.remove(bonded_device);
                    HideProgressDialog();
                    mBluetoothService.connect(bonded_device, true);
                    if (activity != null) {
                        saveLastConnectedDevice(activity, bonded_device);
                    }
                    break;
                case HandlerConstants.MESSAGE_TOAST:
                    showToast((String) msg.obj, getContext());
                    break;
            }

            return true;
        }
    });

    // initializations
    public BluetoothFragment() {}

    static BluetoothFragment newInstance(int position) {
        BluetoothFragment f = new BluetoothFragment();
        Bundle b = new Bundle();
        b.putInt("position", position);
        f.setArguments(b);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBluetoothService = new BluetoothService(getContext(), mBluetoothAdapter, mHandler);

        // Register for bluetooth scanning broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        Activity activity = getActivity();
        if (activity != null) {
            activity.registerReceiver(mReceiver, filter);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root_view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // Setup RecyclerView
        RecyclerView mRecyclerView = root_view.findViewById(R.id.device_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(root_view.getContext()));
        mDataAdapter = new BluetoothDataAdapter(new ArrayList<BluetoothDevice>());
        mRecyclerView.setAdapter(mDataAdapter);

        return root_view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onResume() {
        super.onResume();
        showLastConnected();
        connectLastDevice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Activity activity = getActivity();
        if (activity != null) {
            activity.unregisterReceiver(mReceiver);
            mBluetoothService.stop(activity);
        }
    }

    // utility methods
    private void showToast(CharSequence msg, Context context) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showLastConnected() {
        Activity activity = getActivity();
        if (activity != null) {
            String display_text = getLastConnectedDevice(activity).first;
            if (display_text != null) {
                activity.findViewById(R.id.last_connected_device).setVisibility(View.VISIBLE);
                String device_name = getLastConnectedDevice(activity).second;
                if (device_name != null) {
                    display_text = display_text + String.format(" (%s)", device_name);
                }
                ((TextView) activity.findViewById(R.id.connected_device)).setText(display_text);
            }
        }
    }

    private void hideLastConnected() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.findViewById(R.id.last_connected_device).setVisibility(View.GONE);
        }
    }

    private void setConnectionStatus(String message) {
        Activity activity = getActivity();
        if (activity != null) {
            ((TextView) activity.findViewById(R.id.device_status)).setText(message);
        }
    }

    // click handlers
    void myClickMethod(View v, Activity activity) {
        switch(v.getId()) {
            case R.id.bluetooth_scan_btn:
                accessBluetooth(REQUEST_SCAN, activity);
                break;
            case R.id.bluetooth_connect_btn:
                BluetoothDevice device = (BluetoothDevice) v.getTag();
                v.setVisibility(View.GONE);
                ShowProgressDialog(v.getContext(), "Loading...");

                mBluetoothService.setDevice(device);
                accessBluetooth(REQUEST_CONNECT, activity);
                break;
        }
    }

    private void connect() {
        BluetoothDevice device = mBluetoothService.getDevice();
        if (device.getBondState() != BOND_BONDED) {
            mBluetoothService.pair(device);
        } else {
            mBluetoothService.connect(device, true);
        }
    }

    /**
     * Handle permissions and actions necessary for Bluetooth operations
     */
    private void accessBluetooth(int request_code, Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, request_code);
            return;
        } else if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, request_code);
            return;
        }

        if (mBluetoothAdapter != null) {
            switch (request_code) {
                case REQUEST_SCAN:
                    mBluetoothService.scan();
                    break;
                case REQUEST_CONNECT:
                    this.connect();
                    break;
            }
        }
    }

    // event listeners
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == REQUEST_SCAN || requestCode == REQUEST_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                accessBluetooth(requestCode, getActivity());
            } else {
                this.showToast("Bluetooth scanning requires location access to work", getContext());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SCAN || requestCode == REQUEST_CONNECT) {
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SCAN:
                        mBluetoothService.scan();
                        break;
                    case REQUEST_CONNECT:
                        this.connect();
                        break;
                }
            } else if (resultCode == RESULT_CANCELED) {
                this.showToast("Bluetooth scanning requires bluetooth to work", getContext());
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_ON) {
                    connectLastDevice();
                }
            }
        }
    };

    static void sendMessage(String msg) {
        mBluetoothService.write(msg.getBytes());
    }

    private void connectLastDevice() {
        Context context = getContext();
        if (mBluetoothAdapter.isEnabled() && context != null) {
            String mac_address = getLastConnectedDevice(context).first;
            if (mac_address != null) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getAddress().equals(mac_address)) {
                            Log.d(BluetoothService.BLUETOOTH_PAIR_TAG, "Detected paired device");
                            if (mBluetoothService.getState() == ConnectionConstants.STATE_DISCONNECTED) {
                                mBluetoothService.setDevice(device);
                                accessBluetooth(REQUEST_CONNECT, getActivity());
                            }
                            return;
                        }
                    }
                }

                // device is unpaired
                hideLastConnected();
                saveLastConnectedDevice(context, null);
                mBluetoothService.removeDevice();
                accessBluetooth(REQUEST_SCAN, getActivity());
            }
        }
    }

    // Manage shared preference
    private static Pair<String, String> getLastConnectedDevice(Context context){
        SharedPreferences prefs = context.getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE);
        return Pair.create(prefs.getString("LAST_CONNECTED_MAC", null), prefs.getString("LAST_CONNECTED_NAME", null));
    }

    private static void saveLastConnectedDevice(Context context, BluetoothDevice device){
        SharedPreferences.Editor editor = context.getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE).edit();
        String address = null;
        String name = null;
        if (device != null) {
            address = device.getAddress();
            name = device.getName();
        }
        editor.putString("LAST_CONNECTED_MAC", address);
        editor.putString("LAST_CONNECTED_NAME", name);

        editor.apply();
    }

    public static void ShowProgressDialog(Context context, String string){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(context, ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(string);
            progressDialog.show();
        }
    }

    public static void HideProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}

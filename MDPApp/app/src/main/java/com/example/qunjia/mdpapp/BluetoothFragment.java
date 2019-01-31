package com.example.qunjia.mdpapp;

import android.Manifest;
import android.app.Activity;
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

    // Widgets
    private BluetoothDataAdapter mDataAdapter;

    // Bluetooth service
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothService;
    private BluetoothDevice connecting_device;

    // Connection handler
    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case HandlerConstants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ConnectionConstants.STATE_CONNECTED:
                            setConnectionStatus("Connected");
                            break;
                        case ConnectionConstants.STATE_CONNECTING:
                            setConnectionStatus("Connecting...");
                            break;
                        case ConnectionConstants.STATE_DISCONNECTED:
                            setConnectionStatus("Disconnected...");
                            break;
                    }
                    break;
                case HandlerConstants.MESSAGE_CONNECTED_DEVICE:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    if (activity != null) {
                        showLastConnected(device);
                        saveLastConnectedDevice(activity, device);
                    }
                    break;
                case HandlerConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d("Message received", readMessage);
                    break;
                case HandlerConstants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.d("Message sent", writeMessage);
                    break;
                case HandlerConstants.MESSAGE_DEVICE_FOUND:
                    BluetoothDevice device_ = (BluetoothDevice) msg.obj;
                    if (activity != null) {
                        String last_connected_MAC = getLastConnectedDevice(activity).first;
                        if (last_connected_MAC != null && last_connected_MAC.equals(device_.getAddress())) {
                            if (mBluetoothAdapter.isEnabled()) {
                                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                                if (pairedDevices.size() > 0) {
                                    for (BluetoothDevice device__ : pairedDevices) {
                                        if (device__.getAddress().equals(last_connected_MAC)) {
                                            showLastConnected(device__);
                                            connecting_device = device__;
                                            checkBluetoothAccess(REQUEST_CONNECT, getActivity());
                                        }
                                    }
                                }
                                hideLastConnected();
                                saveLastConnectedDevice(activity, null);
                            }
                        } else if (mDataAdapter != null) {
                            mDataAdapter.add(device_);
                        } else {
                            Log.e(BluetoothService.BLUETOOTH_SCAN_TAG, "Data adapter is dereference!");
                        }
                    }
                    String deviceName = device_.getName();
                    String deviceHardwareAddress = device_.getAddress(); // MAC address
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
                    View view_ = getView();
                    if (view_ != null) {
                        getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
                        getView().findViewById(R.id.bluetooth_scan_btn).setVisibility(View.VISIBLE);
                    }
                    break;
                case HandlerConstants.MESSAGE_DEVICE_BONDED:
                    mBluetoothService.connect((BluetoothDevice) msg.obj, true);
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
        Context context = getContext();
        if (context != null) {
            String mac_address = getLastConnectedDevice(getContext()).first;
            if (mac_address != null) {
                checkBluetoothAccess(REQUEST_SCAN, getActivity());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // utility methods
    private void showToast(CharSequence msg, Context context) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showLastConnected(BluetoothDevice device) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.findViewById(R.id.last_connected_device).setVisibility(View.VISIBLE);
            String display_text = device.getAddress();
            if (device.getName() != null) {
                display_text = display_text + String.format(" (%s)", device.getName());
            }
            ((TextView) activity.findViewById(R.id.connected_device)).setText(display_text);
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
                checkBluetoothAccess(REQUEST_SCAN, activity);
                break;
            case R.id.bluetooth_connect_btn:
                connecting_device = (BluetoothDevice) v.getTag();
                checkBluetoothAccess(REQUEST_CONNECT, activity);
                break;
        }
    }

    private void connect() {
        if (connecting_device.getBondState() != BOND_BONDED) {
            mBluetoothService.pair(connecting_device);
        } else {
            mBluetoothService.connect(connecting_device, true);
        }
    }

    /**
     * Handle permissions and actions necessary for Bluetooth operations
     */
    private void checkBluetoothAccess(int request_code, Context context) {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, request_code);
        } else if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) context).startActivityForResult(intent, request_code);
        }
    }

    // event listeners
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == REQUEST_SCAN || requestCode == REQUEST_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothAccess(requestCode, getContext());
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
                    case REQUEST_CONNECT:
                        this.connect();
                }
            } else if (resultCode == RESULT_CANCELED) {
                this.showToast("Bluetooth scanning requires bluetooth to work", getContext());
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
}

package com.example.qunjia.mdpapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;

public class BluetoothFragment extends Fragment {
    // Request codes
    private static int REQUEST_ENABLE_BT_SCAN = 1;
    private static int REQUEST_ENABLE_BT_RECONNECT = 2;

    // Debug tags
    private static String BLUETOOTH_SCAN = "Bluetooth Scan";
    private static String BLUETOOTH_PAIR = "Bluetooth Pair";

    // Widgets
    private RecyclerView mRecyclerView;

    // Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;
    private UUID DEFAULT_UUID = UUID.fromString("0000111f-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice last_connected_device;
    private final static int CONNECTED = 0;
    private final static int CONNECTING = 1;
    private final static int DISCONNECTED = 2;

    // Broadcast receivers
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDataAdapter adapter = (BluetoothDataAdapter) mRecyclerView.getAdapter();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String last_connected_MAC = GetLastConnectedMAC(getContext()).first;
                if (last_connected_MAC != null && last_connected_MAC.equals(device.getAddress())) {
                    last_connected_device = device;
                } else if (adapter != null) {
                    adapter.add(device);
                } else {
                    Log.e(BLUETOOTH_SCAN, "Data adapter is dereference!");
                }
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName != null) {
                    Log.d(BLUETOOTH_SCAN, String.format("Found device (Name: %s)", deviceName));
                } else {
                    Log.d(BLUETOOTH_SCAN, String.format("Found device (MAC address: %s)", deviceHardwareAddress));
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(BLUETOOTH_SCAN, "Started Bluetooth scanning");
                adapter.clear();
                getView().findViewById(R.id.bluetooth_scan_btn).setVisibility(View.GONE);
                getView().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(BLUETOOTH_SCAN, "Finished Bluetooth scanning");
                getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
                getView().findViewById(R.id.bluetooth_scan_btn).setVisibility(View.VISIBLE);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bond_state = intent.getIntExtra(EXTRA_BOND_STATE, 0);
                if (bond_state == BOND_BONDED) {
                    Log.d(BLUETOOTH_SCAN, "Finished Bluetooth pairing");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (adapter != null) {
                        SetLastConnectedMAC(getContext(), device.getAddress(), device.getName());
                        adapter.remove(device);
                        showLastConnected(device.getAddress(), device.getName());
                    } else {
                        Log.e(BLUETOOTH_PAIR, "adapter is dereference!");
                    }
                    ConnectThread thread = new ConnectThread(device);
                    thread.run();
                }
            }
        }
    };

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

        // Register for bluetooth scanning broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        Activity activity = getActivity();
        if (activity != null) {
            activity.registerReceiver(mReceiver, filter);
        } else {
            Log.e(BLUETOOTH_SCAN, "Can't get current activity!");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root_view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // Setup RecyclerView
        mRecyclerView = root_view.findViewById(R.id.device_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(root_view.getContext()));
        mRecyclerView.setAdapter(new BluetoothDataAdapter(new ArrayList<BluetoothDevice>()));

        return root_view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) { }

    @Override
    public void onResume() {
        super.onResume();
        String mac_address = GetLastConnectedMAC(getContext()).first;
        String device_name = GetLastConnectedMAC(getContext()).second;
        if (mac_address != null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getAddress().equals(mac_address)) {
                            showLastConnected(mac_address, device_name);
                            this.scanBluetoothDevice(getActivity());
                            return;
                        }
                    }
                }
                hideLastConnected();
                SetLastConnectedMAC(getContext(), null, null);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Activity activity = getActivity();
        if (activity != null) {
            activity.unregisterReceiver(mReceiver);
        } else {
            Log.e(BLUETOOTH_SCAN, "Can't get current activity!");
        }
    }

    // Thread class for Bluetooth connection
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                ParcelUuid[] uuids = null;
                while (uuids == null) {
                    uuids = device.getUuids();
                }
                tmp = device.createRfcommSocketToServiceRecord(uuids[3].getUuid());
            } catch (IOException e) {
                Log.e(BLUETOOTH_PAIR, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.e(BLUETOOTH_PAIR, "Unable to establish connection");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(BLUETOOTH_PAIR, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            setConnectionStatus(CONNECTED);
            ConnectionThread thread = new ConnectionThread(mmSocket);
            thread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(BLUETOOTH_PAIR, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectionThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectionThread(BluetoothSocket socket) {
            mmSocket = socket;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();
            while (mmSocket.isConnected()) {
                Log.d("CONNECTION ACTIVE", "currently connected");
            }
            setConnectionStatus(DISCONNECTED);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(BLUETOOTH_PAIR, "Could not close the client socket", e);
            }
        }
    }

    // utility methods
    private void showToast(CharSequence msg, Context context) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showLastConnected(String device_address, String device_name) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.findViewById(R.id.last_connected_device).setVisibility(View.VISIBLE);
            String display_text = device_address;
            if (device_name != null) {
                display_text = display_text + String.format(" (%s)", device_name);
            }
            ((TextView) activity.findViewById(R.id.connected_device)).setText(display_text);
            this.setConnectionStatus(CONNECTING);
        }
    }

    private void setConnectionStatus(int status) {
        Activity activity = getActivity();
        if (activity != null) {
            String message = "";
            switch (status) {
                case CONNECTED:
                    message = "Connected";
                    break;
                case DISCONNECTED:
                    message = "Disconnected";
                    break;
                case CONNECTING:
                    message = "Connecting...";
                    break;
            }
            ((TextView) activity.findViewById(R.id.device_status)).setText(message);
        }
    }

    private void hideLastConnected() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.findViewById(R.id.last_connected_device).setVisibility(View.GONE);
        }
    }

    // click handlers
    void myClickMethod(View v, Activity activity) {
        switch(v.getId()) {
            case R.id.bluetooth_scan_btn: this.scanBluetoothDevice(activity);
                break;
            case R.id.bluetooth_connect_btn: this.pairBluetoothDevice(v);
                break;
        }
    }

    private void pairBluetoothDevice(View v){
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            BluetoothDevice device = (BluetoothDevice) v.getTag();
            if (device.getBondState() == BOND_BONDED){
                ConnectThread thread = new ConnectThread(device);
                thread.run();
            } else {
                device.createBond();
            }
        }
    }

    private void scanBluetoothDevice(Activity activity){
        mBluetoothAdapter = getBluetoothAdapter(REQUEST_ENABLE_BT_SCAN, activity);
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startDiscovery();
        }
    }

    private BluetoothAdapter getBluetoothAdapter(int request_code, Activity activity) {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            return mBluetoothAdapter;
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission(request_code);
            return null;
        } else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(enableBtIntent, request_code);
            }

            return mBluetoothAdapter;
        }
    }

    private void getLocationPermission(int request_code) {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, request_code);
    }

    // event listeners
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT_SCAN || requestCode == REQUEST_ENABLE_BT_RECONNECT) {
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_ENABLE_BT_SCAN) {
                    scanBluetoothDevice(getActivity());
                }
            }
            else if (resultCode == RESULT_CANCELED) {
                this.showToast("Bluetooth scanning requires bluetooth to work", getContext());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ENABLE_BT_SCAN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanBluetoothDevice(getActivity());
            } else {
                this.showToast("Bluetooth scanning requires location access to work", getContext());
            }
        }
    }

    // Manage shared preference
    private static Pair<String, String> GetLastConnectedMAC(Context context){
        SharedPreferences prefs = context.getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE);
        return Pair.create(prefs.getString("LAST_CONNECTED_MAC", null), prefs.getString("LAST_CONNECTED_NAME", null));
    }

    private static void SetLastConnectedMAC(Context context, String mac_address, String device_name){
        SharedPreferences.Editor editor = context.getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE).edit();
        editor.putString("LAST_CONNECTED_MAC", mac_address);
        editor.putString("LAST_CONNECTED_NAME", device_name);

        editor.apply();
    }
}

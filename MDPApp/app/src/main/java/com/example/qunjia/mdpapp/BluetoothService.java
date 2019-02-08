package com.example.qunjia.mdpapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.*;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 *
 * Adapted and modified from https://github.com/googlesamples/android-BluetoothChat
 */
interface HandlerConstants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_CONNECTED_DEVICE = 2;
    int MESSAGE_READ = 3;
    int MESSAGE_WRITE = 4;
    int MESSAGE_DEVICE_FOUND = 5;
    int MESSAGE_START_DISCOVERY = 6;
    int MESSAGE_FINISH_DISCOVERY = 7;
    int MESSAGE_DEVICE_BONDED = 8;
    int MESSAGE_TOAST = 9;
}

interface ConnectionConstants {
    // Constants that indicate the current connection state
    int STATE_DISCONNECTED = 0;       // we're doing nothing
    int STATE_CONNECTING = 1; // now initiating an outgoing connection
    int STATE_CONNECTED = 2;  // now connected to a remote device
}

public class BluetoothService {
    // Debug tag
    private static String BLUETOOTH_CONNECTION_TAG = "Bluetooth (Connection)";
    static String BLUETOOTH_SCAN_TAG = "Bluetooth (Scan)";
    static String BLUETOOTH_PAIR_TAG = "Bluetooth (Pair)";

    // Unique UUID for this application
    private static UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private ReconnectThread mReconnectThread;
    private int mState;
    private int mNewState;
    private BluetoothDevice current_device;
    boolean reconnecting = false;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mHandler.obtainMessage(HandlerConstants.MESSAGE_DEVICE_FOUND, device).sendToTarget();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mHandler.obtainMessage(HandlerConstants.MESSAGE_START_DISCOVERY).sendToTarget();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mHandler.obtainMessage(HandlerConstants.MESSAGE_FINISH_DISCOVERY, reconnecting).sendToTarget();
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bond_state = intent.getIntExtra(EXTRA_BOND_STATE, 0);
                if (bond_state == BOND_BONDED) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mHandler.obtainMessage(HandlerConstants.MESSAGE_DEVICE_BONDED, device).sendToTarget();
                }
            }
        }
    };

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Context context, BluetoothAdapter adapter, Handler handler) {
        if (adapter == null) {
            throw new RuntimeException("Bluetooth is not supported!");
        }

        mAdapter = adapter;
        mState = ConnectionConstants.STATE_DISCONNECTED;
        mNewState = mState;
        mHandler = handler;

        // Register for bluetooth scanning broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(mReceiver, filter);
    }

    /**
     * Update UI according to the current state of the connection
     */
    private synchronized void updateUIStatus() {
        mState = getState();
        Log.d(BLUETOOTH_CONNECTION_TAG, "updateUIStatus() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(HandlerConstants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    synchronized int getState() {
        return mState;
    }

    void setDevice(BluetoothDevice device) {
        current_device = device;
    }

    BluetoothDevice getDevice() {
        return current_device;
    }

    void removeDevice() {
        current_device = null;
    }

    void scan(){
        Log.d(BLUETOOTH_SCAN_TAG, "Scanning for Bluetooth devices");
        mAdapter.startDiscovery();
    }

    void pair(BluetoothDevice device){
        if (device.getBondState() == BOND_NONE){
            Log.d(BLUETOOTH_PAIR_TAG, "Pairing with " + device);
            device.createBond();
        }
    }

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(BLUETOOTH_CONNECTION_TAG, "Connecting to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == ConnectionConstants.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();

        updateUIStatus();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        Log.d(BLUETOOTH_CONNECTION_TAG, "Device connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mReconnectThread != null) {
            mReconnectThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the connected device back to the UI Activity
        mHandler.obtainMessage(HandlerConstants.MESSAGE_CONNECTED_DEVICE, device).sendToTarget();
        updateUIStatus();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop(Activity activity) {
        Log.d(BLUETOOTH_CONNECTION_TAG, "Connection stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mReconnectThread != null) {
            mReconnectThread = null;
        }

        mState = ConnectionConstants.STATE_DISCONNECTED;
        updateUIStatus();

        activity.unregisterReceiver(mReceiver);
    }

    void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != ConnectionConstants.STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
//        mHandler.obtainMessage(HandlerConstants.MESSAGE_TOAST, "Unable to connect device").sendToTarget();
        Log.d(BLUETOOTH_CONNECTION_TAG, "Connection failed");
        mState = ConnectionConstants.STATE_DISCONNECTED;
        reconnecting = true;
        if (mReconnectThread == null && mAdapter.isEnabled()) {
            mReconnectThread = new ReconnectThread(current_device);
            mReconnectThread.start();
        } else if (!mAdapter.isEnabled()){
            if (mReconnectThread != null) {
                mReconnectThread.cancel();
            }
            mReconnectThread = null;
        }
        updateUIStatus();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Log.d(BLUETOOTH_CONNECTION_TAG, "Connection lost");
        mHandler.obtainMessage(HandlerConstants.MESSAGE_TOAST, "Device connection was lost").sendToTarget();
        mState = ConnectionConstants.STATE_DISCONNECTED;
        reconnecting = true;
        if (mReconnectThread == null && mAdapter.isEnabled()) {
            mReconnectThread = new ReconnectThread(current_device);
            mReconnectThread.start();
        } else if (!mAdapter.isEnabled()){
            if (mReconnectThread != null) {
                mReconnectThread.cancel();
            }
            mReconnectThread = null;
        }
        updateUIStatus();
    }

    /**
     * This thread runs when connection is lost to scan
     * for last connected device and reconnect with it.
     */
    private class ReconnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private boolean flag = true;

        ReconnectThread(BluetoothDevice device) {
            mmDevice = device;
        }

        public void run() {
            Log.i(BLUETOOTH_CONNECTION_TAG, "Attempting to reconnect");
            while (mState != ConnectionConstants.STATE_CONNECTED && mAdapter.isEnabled() && flag) {
                if (reconnecting) {
                    connect(mmDevice, true);
                }
                reconnecting = false;
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            this.flag = false;
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            } catch (IOException e) {
                Log.e(BLUETOOTH_CONNECTION_TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = ConnectionConstants.STATE_CONNECTING;
        }

        public void run() {
            Log.i(BLUETOOTH_CONNECTION_TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
//            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(BLUETOOTH_CONNECTION_TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(BLUETOOTH_CONNECTION_TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(BLUETOOTH_CONNECTION_TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(BLUETOOTH_CONNECTION_TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = ConnectionConstants.STATE_CONNECTED;
            reconnecting = false;
            mHandler.obtainMessage(HandlerConstants.MESSAGE_TOAST, "Device connected").sendToTarget();
        }

        public void run() {
            Log.i(BLUETOOTH_CONNECTION_TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == ConnectionConstants.STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(HandlerConstants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(BLUETOOTH_CONNECTION_TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(HandlerConstants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(BLUETOOTH_CONNECTION_TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(BLUETOOTH_CONNECTION_TAG, "close() of connect socket failed", e);
            }
        }
    }
}

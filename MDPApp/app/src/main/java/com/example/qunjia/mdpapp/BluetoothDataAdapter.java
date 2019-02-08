package com.example.qunjia.mdpapp;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothDataAdapter extends RecyclerView.Adapter<BluetoothDataAdapter.BluetoothViewHolder> {
    protected ArrayList<BluetoothDevice> devices;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class BluetoothViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        View view;
        BluetoothViewHolder(View v) {
            super(v);
            view = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    BluetoothDataAdapter(ArrayList<BluetoothDevice> devices) {
        this.devices = devices;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BluetoothViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_available_bluetooth_devices,
                parent, false);
        return new BluetoothViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(BluetoothViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        String device_name = this.devices.get(position).getName();
        String address = this.devices.get(position).getAddress();
        TextView textview = holder.view.findViewById(R.id.bluetooth_device_text);
        Button button = holder.view.findViewById(R.id.bluetooth_connect_btn);
        button.setTag(this.devices.get(position));
        View progress_bar = holder.view.findViewById(R.id.bluetooth_progressBar_connect);
        progress_bar.setTag(address);
        if (device_name != null) {
            textview.setText(String.format("%s (%s)", address, device_name));
        } else {
            textview.setText(String.format("%s", address));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void add(BluetoothDevice device) {
        this.devices.add(device);
        this.notifyDataSetChanged();
    }

    public void remove(BluetoothDevice device) {
        this.devices.remove(device);
        this.notifyDataSetChanged();
    }

    public void clear() {
        this.devices.clear();
        this.notifyDataSetChanged();
    }
}

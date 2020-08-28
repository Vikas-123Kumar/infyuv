package com.infyuv_app.infyuv.bluetoothConnection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

class ConnectionBean {
    BluetoothDevice device;
    boolean Switch = false;
    BluetoothSocket socket;

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public boolean isSwitch() {
        return Switch;
    }

    public void setSwitch(boolean aSwitch) {
        Switch = aSwitch;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    public void setSocket(BluetoothSocket socket) {
        this.socket = socket;
    }
}

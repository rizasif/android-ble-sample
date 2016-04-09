package riz.com.bletest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import riz.com.bletest.MainActivity;

import java.util.ArrayList;

/**
 * Created by Rizwan Asif on 12/16/2014.
 */
public class BT_callback implements BluetoothAdapter.LeScanCallback {
//    private ArrayList<BluetoothDevice> mLeDevices = new ArrayList<BluetoothDevice>();
    String LOG_TAG = "Rizi: BT_callback";
    BluetoothAdapter btAdapter;
    BluetoothDevice btDevice;
    TextView txt;

    String savedDeviceName = "";

    BluetoothLeServices bleService;


    public BluetoothAdapter sendAdapter(){
        return btAdapter;
    }

    public void getAdapter(BluetoothAdapter btA, TextView t, String device){
        btAdapter = btA;
        txt = t;
        savedDeviceName = device;
    }

    public BluetoothDevice getDevice(){
        return btDevice;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Device scan callback.
        Log.e(LOG_TAG,"In onLeScan");
        btAdapter.stopLeScan(this);
        btDevice = device;
        Log.e(LOG_TAG,"Displayed device name: " + btDevice.getName());

        if(savedDeviceName.equals(btDevice.getName())){
            txt.setText(btDevice.getName());
        }
        else{
            txt.setText("Saved Device Not Found");
        }

//        if(!mLeDevices.contains(device)) {
//            mLeDevices.add(device);
//        }
    }

}

package riz.com.bletest;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Rizwan Asif on 12/21/2014.
 */
public class BluetoothLeServices extends Service {
    private final static String TAG = BluetoothLeServices.class.getSimpleName();
    String LOG_TAG = "Rizi: BluetoothLeservices";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;
    private int mConnectionState = STATE_DISCONNECTED;
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothLeServices getService() {
            return BluetoothLeServices.this;
        }
    }

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "riz.com.bletest.BluetoothLeServices.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "riz.com.bletest.BluetoothLeServices.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "riz.com.bletest.BluetoothLeServices.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "riz.com.bletest.BluetoothLeServices.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "riz.com.bletest.BluetoothLeServices.EXTRA_DATA";

    public final static UUID uuid_service =
            UUID.fromString("00002220-0000-1000-8000-00805f9b34fb");
    public final static UUID uuid_recieve =
            UUID.fromString("00002221-0000-1000-8000-00805f9b34fb");
    public final static UUID uuid_config =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    public void start(BluetoothDevice device, BluetoothAdapter bta)
    {
        mBluetoothAdapter = bta;
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    Log.e(LOG_TAG, "onConnectionStateChange()");
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.e(LOG_TAG, "onServicesDiscovered()");
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        mBluetoothGattService = gatt.getService(uuid_service);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }

                    BluetoothGattCharacteristic receiveCharacteristic =
                            mBluetoothGattService.getCharacteristic(uuid_recieve);
                    if (receiveCharacteristic != null) {
                        BluetoothGattDescriptor receiveConfigDescriptor =
                                receiveCharacteristic.getDescriptor(uuid_config);
                        if (receiveConfigDescriptor != null) {
                            gatt.setCharacteristicNotification(receiveCharacteristic, true);

                            receiveConfigDescriptor.setValue(
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(receiveConfigDescriptor);
                        } else {
                            Log.e(TAG, "RFduino receive config descriptor not found!");
                        }

                    } else {
                        Log.e(TAG, "RFduino receive characteristic not found!");
                    }

                    broadcastUpdate(ACTION_GATT_CONNECTED);

                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.e(LOG_TAG, "onCharachterisricRead()");
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
//                    Log.e(LOG_TAG, "onCharachterisricChanged()");
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (uuid_service.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }
        sendBroadcast(intent, Manifest.permission.BLUETOOTH);
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        return filter;
    }

}

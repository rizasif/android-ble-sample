package riz.com.bletest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    String LOG_TAG = "Rizi: MainActivity";

    public BluetoothAdapter btAdapter;
    static String uuid = "00002220-0000-1000-8000-00805f9b34fb";
    BT_callback callback;
    BluetoothLeServices mBluetoothLeServices;

    TextView txt;
    EditText edt;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            Log.e(LOG_TAG, "Action: " + action);
            if(mBluetoothLeServices.ACTION_DATA_AVAILABLE.equals(action)){
                //refineData(intent.getStringExtra(mBluetoothLeServices.EXTRA_DATA));

                byte [] data = intent.getByteArrayExtra(mBluetoothLeServices.EXTRA_DATA);

                try{
                    int i = data.length;
                    Log.i(LOG_TAG, "Data Received: " + data);
                } catch (Exception e){
                    Log.e(LOG_TAG, e.toString());
                }


            }
        }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        txt = (TextView) findViewById(R.id.textView);
        edt = (EditText) findViewById(R.id.editText);

        callback = new BT_callback();
        mBluetoothLeServices = new BluetoothLeServices();

        registerReceiver(mGattUpdateReceiver, BluetoothLeServices.getIntentFilter());
    }

    public void enableBluetooth(View view){
        if (!btAdapter.isEnabled())
        {
            btAdapter.enable();
        }
    }

    public void scanBLEdevices(View view){
        Log.e(LOG_TAG, "scanning module called");
        callback.getAdapter(btAdapter, txt, edt.getText().toString());
        btAdapter.startLeScan(new UUID[]{UUID.fromString(uuid)}, callback);
        Log.e(LOG_TAG, "started LeScan");
    }

    public void connectDevice(View view){
        Intent intent = new Intent(MainActivity.this, BluetoothLeServices.class);
        bindService(intent, rfduinoServiceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(LOG_TAG, "onServiceConnected");
            mBluetoothLeServices = ((BluetoothLeServices.LocalBinder) service).getService();
            mBluetoothLeServices.start(callback.getDevice(), callback.sendAdapter());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private final void refineData(String data){
        if(data != null) {
            String refinedData= "";
            for(int i=0; i<data.length(); i++){
                if(data.charAt(i) >= '0' && data.charAt(i) <= 'Z'){
                    refinedData = refinedData + data.charAt(i);
                }
            }
            txt.setText(convert2Dec(refinedData));
//            Log.e(LOG_TAG, "String: " + data + "Length: " + data.length() + "\n" + "newString: " + refinedData + "\tnewLength: " + refinedData.length());
        }
    }

    private final String removeZeros(String data){
        String removedData = "";
        int i=1;
        if(!data.equals("00")) {
            while (data.charAt(data.length() - i) == '0') {
                i++;
            }
        }

        for(int j=data.length()-i; j>0; j--){
            removedData = removedData + data.charAt(j);
        }

        return removedData;
//            Log.e(LOG_TAG, "String: " + data + "Length: " + data.length() + "\n" + "newString: " + removedData + "\tnewLength: " + removedData.length() + "  " + data.charAt(1));
    }

    private final String convert2Dec(String data){
        //data = removeZeros(data);
        String converted = "";
        double num = 0;
        if(data.length()==1){
            num = Character.getNumericValue(data.charAt(0));
        } else {
            for (int i = 0; i < data.length(); i++) {
                double charNum = Character.getNumericValue(data.charAt((data.length()-1)-i));
                double mul = Math.pow(16,i);
                num = num + (mul * charNum);
//                Log.e(LOG_TAG, data + "\tChar: " + data.charAt((data.length()-1)-i) + "\tCharNum: " + charNum + "\tnum: " + num + "\tmul: " + mul);
            }
        }
//        Log.e(LOG_TAG, "Recieved Hex: " + data + "\tConverted Int: " + num );
        return String.valueOf(num);
    }

    /*Causes delay for duration time and a code after*/
    public void Delay(int duration){
        new Handler().postDelayed(new Runnable(){  //Import android.os.handler
            @Override
            public void run(){
                //Your function e.g StartActivity();
            }}, duration);
    }
}

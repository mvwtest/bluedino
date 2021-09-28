package aal.arduino_bluetooth_controller_blueduino;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class BluetoothConnection extends AppCompatActivity {
    private String TAG = "BluetoothConnection";

    // ** Variables **
    BluetoothAdapter bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme", false)){
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );
        }else{
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );
        }
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());

        setTitle(R.string.title_activity_bluetooth_connection);
        setContentView(R.layout.activity_bluetooth_connection);
        bt = BluetoothAdapter.getDefaultAdapter();
        try_to_enable_bt();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[0] = true;
        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[0] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    // ** Enable bluetooth **
    public static int BLUETOOTH_ENABLED = 1;
    private void try_to_enable_bt() {
        if (bt == null){
            Log.d(TAG, "Device unable to use bluetooth");
            Toast.makeText(BluetoothConnection.this, R.string.bt_unable, Toast.LENGTH_SHORT).show();
            this.finish();
        }else if (bt.isEnabled()){
            Log.d(TAG, "Bluetooth enabled.");
            open(Devices.class);
        }else{
            Log.d(TAG, "Needs to enable bluetooth.");
            Intent enable_bt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_bt, BLUETOOTH_ENABLED);
        }
    }

    // ** Ensure that user enabled bluetooth **
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ENABLED){
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "User: OK");
                open(Devices.class);
            } else if (resultCode == RESULT_CANCELED){
                Log.d(TAG, "User: CANCELED");
                BluetoothConnection.this.finish();
            }
        }
    }

    // ** This opens activities **
    private void open(Class target){
        Intent go = new Intent(BluetoothConnection.this, target);
        startActivity(go);
        BluetoothConnection.this.finish();
    }
}

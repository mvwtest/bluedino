package aal.arduino_bluetooth_controller_blueduino;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by Arturo on 26/06/2018.
 */

public class GlobalVariables {
    // Bluetooth
    public static BluetoothSocket global_socket;
    public static BluetoothDevice connected_device;
    public static boolean connection;

    // Terminal receive message from bluetooth module
    public static String received_message;
    public static boolean listening;

    // Debugging
    public static boolean debugging_mode;
    public static BluetoothDevice debugging_device;

    // Current activities: 9
    public static boolean[] activity_on = new boolean[9];
    // [0] -> BluetoothConnection.java
    // [8] -> GlobalSettings.java

    public static boolean is_app_in_foreground() {
        boolean foreground = true;
        for (boolean anActivityOn : activity_on) {
            if (anActivityOn)
                foreground = false;
        }
        return foreground;
    }

    // BlueDuino.db
    public static DataBase db;
}

package aal.arduino_bluetooth_controller_blueduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by Arturo on 23/06/2018.
 */
public class SocketConnection{

    private static final String TAG = "SocketConnection";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice device;

    private ConnectedThread thread = null;

    public SocketConnection(BluetoothDevice targetDevice){
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        device = bt.getRemoteDevice(targetDevice.getAddress());
        local_socket = GlobalVariables.global_socket;
    }

    // ** Create and connect to socket functions **
    private BluetoothSocket local_socket;
    public void connect_to_socket(){
        Log.d(TAG, "Trying to create socket.");
        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());
        // Create socket
        try {
            local_socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            Log.d(TAG, "Socket created.");
        } catch (IOException e) {
            Log.d(TAG, "Socket creation failed.");

        }
        // Connect to socket
        try {
            Log.d(TAG, "Connecting.");
            local_socket.connect();
            Log.d(TAG, "Connected.");

        } catch (IOException e) {
            Log.d(TAG, "Connection failed.");
            close_socket();
        }
    }

    // ** Closes socket **
    public void close_socket(){
        GlobalVariables.listening = false;
        try {
            local_socket.close();
            Log.d(TAG, "Socket closed.");
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    // ** Check if connected to socket **
    public boolean is_socket_connected(){
        if (local_socket.isConnected())
            Log.d(TAG, "Is connected.");
        else
            Log.d(TAG, "Is not connected.");
        return local_socket.isConnected();
    }

    // ** Returns socket **
    public BluetoothSocket get_socket(){
        return local_socket;
    }

    // ** Write and receive data **
    private class ConnectedThread extends Thread {
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            try {
                mmInStream = socket.getInputStream(); //
                mmOutStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        // Add code later (Terminal: Add this function in a thread)
        public void listen(int read_rate) {
            try{
                Thread.sleep(read_rate);
                byte[] buffer = new byte[1024]; // 256 || 1024
                int buffer_position = mmInStream.read(buffer);
                GlobalVariables.received_message = new String(buffer, 0, buffer_position);
                Log.d(TAG, GlobalVariables.received_message);
            }catch (Exception e){
                GlobalVariables.received_message = e.getMessage();
            }
        }

        // Sends message
        public void write(String input) {
            try {
                // Writing message
                if (input == null){
                    Log.d(TAG, "Null message.");
                }else {
                    Log.d(TAG, "Writing message: " + input);
                    mmOutStream.write(input.getBytes());
                }
            }
            catch (IOException e) {
                // Write failed
                Log.d(TAG, "Write message failed.");
                Log.d(TAG, e.getMessage());
            }
        }
    }

    // ** Writes message **
    public void write(String data){
        if (thread == null) {
            Log.d(TAG, "Thread empty.");
            thread = new ConnectedThread(local_socket);
        }
        thread.write(data);
    }

    // ** Listen message **
    public void listen(int read_rate){
        if (thread == null) {
            Log.d(TAG, "Thread empty.");
            thread = new ConnectedThread(local_socket);
        }
        thread.listen(read_rate);
    }

    public void clean_stream(int read_rate){
        listen(read_rate);
        GlobalVariables.received_message = "";
        Log.d(TAG, "Message cleaned.");
    }
}

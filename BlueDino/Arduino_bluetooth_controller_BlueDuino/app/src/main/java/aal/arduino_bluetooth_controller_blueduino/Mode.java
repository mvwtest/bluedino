package aal.arduino_bluetooth_controller_blueduino;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Mode extends AppCompatActivity {
    private static final String TAG = "Mode";

    // ** xml items **
    private ListView lv_modes;
    private ProgressBar progress;

    // ** Variables **
    private BluetoothDevice paired_device;
    private SocketConnection connection;
    private ConnectionTask connection_task;

    private boolean debugging_back; //********************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());

        debugging_back = false; //*************************************************************

        // ** Broadcast receivers **
        IntentFilter disconnected = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(disconnected_receiver, disconnected);

        // ** Variables **
        paired_device = GlobalVariables.connected_device;

        // ** xml items
        lv_modes = (ListView) findViewById(R.id.lv_modes);
        progress = (ProgressBar) findViewById(R.id.progress_connection);

        // Start connection task
        connection = new SocketConnection(paired_device);
        connection_task = new ConnectionTask();
        connection_task.execute();

        // ** onClick : lv_modes
        lv_modes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        Log.d(TAG, "Clicked on item: " + position);
                        open(Terminal.class);
                        break;
                    case 1:
                        Log.d(TAG, "Clicked on item: " + position);
                        open(Digital.class);
                        break;
                    case 2:
                        Log.d(TAG, "Clicked on item: " + position);
                        open(Analog.class);
                        break;
                    case 3:
                        Log.d(TAG, "Clicked on item: " + position);
                        open(Joystick.class);
                        break;
                }
            }
        });

    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[2] = true;
        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");

        // If bluetooth is not enabled or lost connection close Activity
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled() || !GlobalVariables.connection){
            Log.d(TAG, "Bluetooth disabled or disconnected finishing Mode activity.");
            Mode.this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[2] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                close_connection();
                return true;
            case R.id.action_example:
                open_github();
                return true;
            case R.id.action_info:
                show_about_alert();
                return true;
            case R.id.action_global_settings:
                startActivity(new Intent(Mode.this, GlobalSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Goes back to Devices activity **
    @Override
    public void onBackPressed() {
        debugging_back = true;
        close_connection();
    }

    // ** Close connection and go back **
    private void close_connection(){
        super.onBackPressed();
        Log.d(TAG, "Close activity.");
        connection.close_socket();
        GlobalVariables.global_socket = connection.get_socket();
        if (!connection.is_socket_connected())
            Toast.makeText(Mode.this, R.string.disconnected, Toast.LENGTH_SHORT).show();
    }

    // ** Show items **
    private void show_items(){
        progress.setVisibility(View.GONE);
        lv_modes.setVisibility(View.VISIBLE);
        Log.d(TAG, "Items visible");
    }

    // ** Hide items **
    private void hide_items(){
        progress.setVisibility(View.VISIBLE);
        lv_modes.setVisibility(View.GONE);
        Log.d(TAG, "Items gone.");
    }

    // ** Create about alert **
    private void show_about_alert(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(Mode.this);
        View alert_about_view = Mode.this.getLayoutInflater().inflate(R.layout.alert_about, null);

        String version = BuildConfig.VERSION_NAME;
        String[] developers     = getResources().getStringArray(R.array.developers);
        String[] translators    = getResources().getStringArray(R.array.translators);

        TextView tv_version     = (TextView) alert_about_view.findViewById(R.id.tv_version);
        TextView tv_developers  = (TextView) alert_about_view.findViewById(R.id.tv_developers);
        TextView tv_translators = (TextView) alert_about_view.findViewById(R.id.tv_translators);
        TextView tv_e_mail      = (TextView) alert_about_view.findViewById(R.id.tv_e_mail);

        // Append version
        tv_version.append(" " + version);

        // Set the developers' names
        tv_developers.setText("");
        for (int i = 0; i < developers.length; i++){
            if (i == developers.length -1)
                tv_developers.append(developers[i]);
            else
                tv_developers.append(developers[i] + "\n");
        }

        // Set the translators' names
        tv_translators.setText("");
        for (int i = 0; i < developers.length; i++){
            if (i == developers.length -1)
                tv_translators.append(translators[i]);
            else
                tv_translators.append(translators[i] + "\n");
        }

        // Set e-mail
        tv_e_mail.setText(getString(R.string.e_mail));
        tv_e_mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent open_mail = new Intent(Intent.ACTION_VIEW);
                open_mail.setData(
                        Uri.parse(
                                "mailto:" + getString(R.string.e_mail) + "?subject=" + getString(R.string.app_name)
                        )
                );
                startActivity(open_mail);
            }
        });

        alert_builder.setPositiveButton(R.string.alert_info_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(Mode.this, R.string.alert_info_toast, Toast.LENGTH_LONG).show();
            }
        });

        alert_builder.setView(alert_about_view);
        alert_builder.show();
    }

    // ** Open github in browser **
    private void open_github(){
        Intent open_browser = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)));
        startActivity(open_browser);
    }

    // ** Open activity **
    private void open(Class target){
        Intent go = new Intent(Mode.this, target);
        startActivity(go);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(disconnected_receiver);
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    // ** Connection state **
    private final BroadcastReceiver disconnected_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                Log.d(TAG, "Disconnected.");
                // Close and update socket
                connection.close_socket();
                GlobalVariables.global_socket = connection.get_socket();
                GlobalVariables.connection = false;
                // Only execute if app is not in foreground
                if (!GlobalVariables.is_app_in_foreground()){
                    // Device is disconnected
                    Toast.makeText(Mode.this, R.string.connection_lost, Toast.LENGTH_SHORT).show();
                    // Open Devices activity and close the rest
                    Intent go = new Intent(getApplicationContext(), Devices.class);
                    go.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(go);
                }
            }
        }
    };

    // --- Subclass start connection ---
    private class ConnectionTask extends AsyncTask<Void, Void, Boolean> {
        // Executed in UI thread
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute.");
            hide_items();
            if (GlobalVariables.connection){
                connection.close_socket();
                GlobalVariables.global_socket = connection.get_socket();
            }
            GlobalVariables.debugging_device = null; //*******************************************
        }

        // Executed in background
        @Override
        protected Boolean doInBackground(Void... params) {
            Log.d(TAG, "Background task started, debugging mode: " + GlobalVariables.debugging_mode);
            if (GlobalVariables.debugging_mode){ //*************************************************
                connection.connect_to_socket();
                return true;
            }else {//*******************************************************************************
                connection.connect_to_socket();
                return connection.is_socket_connected();
            }
        }

        // Executed in UI thread
        @SuppressLint("MissingPermission")
        @Override
        protected void onPostExecute(Boolean connected) {
            Log.d(TAG, "onPostExecute");
            super.onPostExecute(connected);
            if (connected){
                Toast.makeText(Mode.this, R.string.connected, Toast.LENGTH_SHORT).show();
                show_items();
                // Update global socket
                Log.d(TAG, "Global socket updated.");
                GlobalVariables.global_socket = connection.get_socket();
                // Display list
                int[] images = new int[] {
                        R.drawable.mode_terminal,
                        R.drawable.mode_switch,
                        R.drawable.mode_analog,
                        R.drawable.mode_joystick
                };
                String[] title = getResources().getStringArray(R.array.mode_titles);
                String[] description = getResources().getStringArray(R.array.mode_descriptions);

                CustomListAdapter modes_list = new CustomListAdapter(
                        images,                      // Items' icons
                        title,                       // Items' title
                        description,                 // Items' description
                        getLayoutInflater(),         // Layout inflater
                        R.layout.custom_list_modes   // Custom xml design
                );
                lv_modes.setAdapter(modes_list);
                GlobalVariables.connection = true;
                // Create database
                GlobalVariables.db = new DataBase(getApplicationContext());
                GlobalVariables.db.ifNotGeneratedGenerateSettings();
            }
            else{
                Toast.makeText(Mode.this, R.string.connection_failed, Toast.LENGTH_SHORT).show();
                connection.close_socket();
                GlobalVariables.global_socket = connection.get_socket();
                Mode.this.finish();
                GlobalVariables.connection = false;
                if (debugging_back){ //***********************************************************
                    Log.d(TAG, "Debugging device: " + paired_device.getName());
                    GlobalVariables.debugging_device = paired_device;
                } //******************************************************************************
            }
        }
    }
}

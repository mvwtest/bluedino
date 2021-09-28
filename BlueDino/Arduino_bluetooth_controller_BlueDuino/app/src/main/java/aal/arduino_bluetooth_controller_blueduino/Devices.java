package aal.arduino_bluetooth_controller_blueduino;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;


public class Devices extends AppCompatActivity {
    private static final String TAG = "Devices";

    // ** Variables **
    private BluetoothAdapter bt;

    private ArrayList<BluetoothDevice> paired_devices = new ArrayList<>();    // Paired devices
    private ArrayList<BluetoothDevice> available_devices = new ArrayList<>(); // Available devices
    private ArrayList<BluetoothDevice> unpaired_devices = new ArrayList<>();  // Unpaired devices

    private boolean discovering = false;
    private int attempt; //****************************************************************Debugging
    private SharedPreferences preferences;

    // ** xml items **
    private ListView lv_paired;
    private ListView lv_unpaired;
    private TextView paired_devices_text;
    private TextView unpaired_devices_text;
    private ProgressBar progress_unpaired;
    private FloatingActionButton fab;

    // ** Animations **
    private Animation fab_clicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ** Get preferences **
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // ** Broadcast receivers **
        IntentFilter bt_state = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bt_state_receiver, bt_state);
        GlobalVariables.connected_device = null;

        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());

        // ** xml items **
        lv_paired = (ListView) findViewById(R.id.lv_paired);
        lv_unpaired = (ListView) findViewById(R.id.lv_unpaired);
        paired_devices_text = (TextView) findViewById(R.id.text_paired);
        unpaired_devices_text = (TextView) findViewById(R.id.text_unpaired);
        progress_unpaired = (ProgressBar) findViewById(R.id.progress_unpaired);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        unpaired_devices_text.setText(R.string.text_unpaired);
        unpaired_devices_text.append(": " + available_devices.size());

        // ** Animations **
        fab_clicked = AnimationUtils.loadAnimation(Devices.this, R.anim.fab_loop);

        // ** Variables **
        bt = BluetoothAdapter.getDefaultAdapter();

        // Query paired devices
        // Look for unpaired devices
        if (preferences.getBoolean("auto_discover", true))
            discover_devices();
        else
            update_paired_devices();

        // ** onClick for paired devices**
        lv_paired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.d(TAG, "Paired devices: User click on item.");
                open_mode(paired_devices.get(position));
                GlobalVariables.debugging_mode = false; //******************************************
            }
        });

        // ** Create menu for paired devices list **
        registerForContextMenu(lv_paired);

        // ** onClick for unpaired devices**
        lv_unpaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Unpaired devices: User click on item.");
                // Trying to pair with device
                if (bt.isDiscovering()) {
                    Log.d(TAG, "Device is discovering, canceling.");
                    bt.cancelDiscovery();
                }
                create_bond(unpaired_devices.get(position));
                GlobalVariables.debugging_mode = false; //******************************************

            }
        });

        // ** Floating Action Button: onClick for discover devices**
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If device is not discovering
                if (!discovering){
                    discover_devices();
                    //******************************************************************************
                    GlobalVariables.debugging_mode = false;
                    attempt = 0;
                    //******************************************************************************
                }else {
                    Log.d(TAG, "Already discovering.");
                    //******************************************************************************
                    attempt ++;
                    Log.d(TAG, "Attempt: " + String.valueOf(attempt));
                    if (attempt == 10){
                        if(GlobalVariables.debugging_device != null){
                            Log.d(TAG, "Debugging mode: ON");
                            GlobalVariables.debugging_mode = true;
                            open_mode(paired_devices.get(0));
                        }
                    }
                    //******************************************************************************
                }
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[1] = true;
        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");

        // If bt is not enabled go to main activity
        if (!bt.isEnabled()){
            Log.d(TAG, "Bluetooth disabled finishing Devices activity.");
            Devices.this.finish();
            Intent go = new Intent(Devices.this, BluetoothConnection.class);
            startActivity(go);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[1] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return true;
    }
// is settings was closed print connection lost
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_example:
                open_github();
                return true;
            case R.id.action_info:
                show_about_alert();
                return true;
            case R.id.action_global_settings:
                startActivity(new Intent(Devices.this, GlobalSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Create popup menu for list view **
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_unpair, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()){
            case R.id.action_unpair:
                unpair_device(paired_devices.get(info.position));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Update list of paired devices **
    private void update_paired_devices(){
        Log.d(TAG, "Updating paired devices.");
        Set<BluetoothDevice> pairedDevices = bt.getBondedDevices(); // Paired devices' list
        Log.d(TAG, "Paired devices: " + pairedDevices.size());
        paired_devices.clear();

        if (pairedDevices.size() > 0) {
            int i = 0;
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                paired_devices.add(device);
                i++;
            }
        }
        // Show items on list view
        show_me_list(lv_paired, paired_devices);
        // Update labels
        paired_devices_text.setText(R.string.text_paired);
        paired_devices_text.append(": " + pairedDevices.size());
    }

    // ** Update list of unpaired devices **
    private void update_unpaired_devices(){
        Log.d(TAG, "Updating unpaired devices.");
        unpaired_devices.clear();

        // Create list of unpaired devices
        if(available_devices.size() > 0){
            Log.d(TAG, "List is not empty.");
            Log.d(TAG, "Available: " + available_devices.size());
            Log.d(TAG, "Paired: " + paired_devices.size());
            for (int i = 0; i < available_devices.size(); i++){
                boolean unique = true;
                for (int j = 0; j < paired_devices.size(); j++){
                    Log.d(TAG, available_devices.get(i).getAddress() + " : " + paired_devices.get(j).getAddress());
                    if (available_devices.get(i).getAddress().equals(paired_devices.get(j).getAddress())){
                        unique = false;
                        Log.d(TAG, "This is not unique.");
                    }
                }
                for (int j = 0; j < unpaired_devices.size(); j++){
                    if (available_devices.get(i).getAddress().equals(unpaired_devices.get(j).getAddress())){
                        unique = false;
                        Log.d(TAG, "This is not unique.");
                    }
                }
                if (unique){
                    Log.d(TAG, "There's a unique.");
                    unpaired_devices.add(available_devices.get(i));
                }
            }

            // Update list
            show_me_list(lv_unpaired, unpaired_devices);
        }else {
            // Update list
            Log.d(TAG, "Empty list.");
            show_me_list(lv_unpaired, unpaired_devices);
        }
        // Update labels
        unpaired_devices_text.setText(R.string.text_unpaired);
        unpaired_devices_text.append(": " + unpaired_devices.size());
    }

    // ** Try to pair **
    private void create_bond(BluetoothDevice target){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Discovery canceled. Trying to pair with: " + target.getName());
            target.createBond();
            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(receiver_bond_state, intentFilter);
        }else{
            Toast.makeText(Devices.this, R.string.no_bond, Toast.LENGTH_SHORT).show();
            Devices.this.finish();
        }
    }

    // ** Discover devices **
    Thread discover_thread;
    private void discover_devices(){
        Log.d(TAG, "Clearing available devices.");
        available_devices.clear();
        update_paired_devices();
        update_unpaired_devices();

        if (bt.isDiscovering()) {
            bt.cancelDiscovery();
            Log.d(TAG, "Device is discovering, Canceling.");
        }

        bt.startDiscovery();
        discovering = true;

        Log.d(TAG, "Discovering.");

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver_discovery_state, filter);

        start_discovery_animation();

        // Keep track of the discovery state
        discover_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Running on thread: " + Thread.currentThread().getId());
                try {
                    Log.d(TAG, "Loading.");
                    Thread.sleep(12000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupted.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stop_discovery_animation();
                            if (unpaired_devices.isEmpty())
                                Log.d(TAG, "No devices were found.");
                            discovering = false;
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stop_discovery_animation();
                        discovering = false;
                    }
                });
            }
        });
        discover_thread.start();
    }

    // ** Discover animation **
    private void start_discovery_animation(){
        // Change icon
        fab.setImageResource(R.drawable.devices_searching);
        // Start animation
        fab.startAnimation(fab_clicked);
        // Start progress bar
        progress_unpaired.setVisibility(View.VISIBLE);
    }

    // ** Stop discover animation **
    private void stop_discovery_animation(){
        // Change icon
        fab.setImageResource(R.drawable.devices_search);
        // Start animation
        fab.clearAnimation();
        // Start progress bar
        progress_unpaired.setVisibility(View.GONE);
    }

    // ** Unpair devices **
    private void unpair_device(final BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Wait to update unpair device
                    Thread.sleep(300);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_paired_devices();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        Log.d(TAG, "Unpaired.");
        Toast.makeText(Devices.this, R.string.unpaired, Toast.LENGTH_SHORT).show();
    }

    // ** Check if user wants to exit **
    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back pressed to exit.");
        AlertDialog.Builder builder = new AlertDialog.Builder(Devices.this);
        builder.setTitle(R.string.title_popup)
                .setMessage(R.string.confirm_exit)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User wants to exit
                        Devices.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing
                    }
                }).show();
    }

    // ** Show a list **
    private void show_me_list(ListView lv, ArrayList<BluetoothDevice> list){
        String[] name = new String[list.size()];
        String[] address = new String[list.size()];

        for (int i = 0; i < list.size(); i++){
            // Get name for each device
            if (list.get(i).getName() == null)
                name[i] = "null";
            else
                name[i] = list.get(i).getName();
            // Get the address for each device
            if (list.get(i).getAddress() == null)
                address[i] = "null";
            else
                address[i] = list.get(i).getAddress();
            // Log each device
            Log.d(TAG, name[i] + " : " + address[i]);
        }
        CustomListAdapter devices_list = new CustomListAdapter(name, address, getLayoutInflater(), R.layout.custom_list_devices);
        lv.setAdapter(devices_list);
    }

    // ** Open activity and send device to later connect **
    private void open_mode(BluetoothDevice device){
        Log.d(TAG, "Global device: " + device.getName() + " : " + device.getAddress() + ".");
        if (bt.isDiscovering()){
            Log.d(TAG, "Device is discovering, cancelling");
            bt.cancelDiscovery();
        }
        GlobalVariables.connected_device = device;
        Intent go = new Intent(Devices.this, Mode.class);
        GlobalVariables.connection = true;
        startActivity(go);
        if (discover_thread != null){
            if (discover_thread.isAlive())
                discover_thread.interrupt();
        }
    }

    // ** Create about alert **
    private void show_about_alert(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(Devices.this);
        View alert_about_view = Devices.this.getLayoutInflater().inflate(R.layout.alert_about, null);

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
                Toast.makeText(Devices.this, R.string.alert_info_toast, Toast.LENGTH_LONG).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bt_state_receiver);
            unregisterReceiver(receiver_discovery_state);
            unregisterReceiver(receiver_bond_state);
        }catch (Exception e){
            Log.d(TAG, "Error on Broadcast receiver: " + e.getMessage());
        }
    }

    // ** Bluetooth state **
    private final BroadcastReceiver bt_state_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bt.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bt.ERROR);
                Log.d(TAG, "State changed.");
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "State off.");
                        // Only execute if app is not in foreground
                        if (!GlobalVariables.is_app_in_foreground()){
                            Toast.makeText(Devices.this, R.string.bt_disabled, Toast.LENGTH_SHORT).show();
                            // Open BluetoothConnection activity and close the rest
                            Intent go = new Intent(getApplicationContext(), BluetoothConnection.class);
                            go.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(go);
                            Devices.this.finish();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "State turning off.");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "State on.");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "State turning on.");
                        break;
                }
            }
        }
    };

    // ** Discovery state **
    private final BroadcastReceiver receiver_discovery_state = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                // Action found
                Log.d(TAG, "Action found.");
                // Discovery has found a device. Get the BluetoothDevice
                BluetoothDevice extra_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                available_devices.add(extra_device); // Create list of available devices
                update_unpaired_devices();
            }
        }
    };

    // ** Bond state **
    private final BroadcastReceiver receiver_bond_state = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action =  intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice extra_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (extra_device.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "Bond: Bonding.");
                    Toast.makeText(Devices.this, R.string.bonding, Toast.LENGTH_SHORT).show();
                }else if (extra_device.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "Bond: Bonded, update lists.");
                    update_paired_devices();
                    update_unpaired_devices();
                    // Show menu of activities
                    open_mode(extra_device);
                }else if (extra_device.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "Bond: None.");
                    update_paired_devices();
                }
            }
        }
    };
}

package aal.arduino_bluetooth_controller_blueduino;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class Digital extends AppCompatActivity {

    private static final String TAG = "Digital";

    private ImageView toggle_send;
    private TextView tv_output;

    // ** Variables **
    private SocketConnection connection;
    private DataBase db;
    private Cursor cursor;

    private String[] toggle_message = new String[2];
    private boolean arduino_output;
    private boolean toggle_on;
    private boolean pin_enabled;
    private String pin;

    private SharedPreferences preferences;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());

        // ** Get preferences **
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // ** Bluetooth connection **
        connection = new SocketConnection(GlobalVariables.connected_device);

        // ** xml items **
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        toggle_send = (ImageView) findViewById(R.id.iv_toggle);
        tv_output = (TextView) findViewById(R.id.tv_output);

        // ** BlueDuino.db **
        db = GlobalVariables.db;
        db.log_table("digital");

        cursor = db.query("select is_on from global where item = 'pin_enabled_digital'");
        cursor.moveToFirst();
        pin_enabled = (cursor.getInt(0) == 1);
        Log.d(TAG, "PIN ENABLED: " + pin_enabled);

        cursor = db.query("select is_on from global where item = 'arduino_output_digital'");
        cursor.moveToFirst();
        arduino_output = (cursor.getInt(0) == 1);
        if (cursor.getInt(0) == 1)
            tv_output.setVisibility(View.VISIBLE);
        else
            tv_output.setVisibility(View.GONE);
        Log.d(TAG, "ARDUINO OUTPUT STATE: " + arduino_output);

        cursor = db.query("select value_off, value_on, state from digital");
        cursor.moveToFirst();
        toggle_message[0] = cursor.getString(0);
        toggle_message[1] = cursor.getString(1);
        toggle_on  = (cursor.getInt(2) == 1);

        Log.d(TAG, "ON VALUE: " + toggle_message[1]
                + " | OFF VALUE: " + toggle_message[0]
                + " | TOGGLE ON: " + toggle_on
        );

        cursor = db.query("select pin from digital");
        cursor.moveToFirst();
        pin = cursor.getString(0);
        Log.d(TAG, "PIN: " + pin);

        if (toggle_on){
            toggle_send.setImageResource(R.drawable.digital_on);
            tv_output.setText(format_message(pin, toggle_message[1]));
        } else{
            toggle_send.setImageResource(R.drawable.digital_off);
            tv_output.setText(format_message(pin, toggle_message[0]));
        }

        // ** TouchListener: Toggle image **
        toggle_send.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Toggle down.");
                        toggle_send.setImageResource(R.drawable.digital_pressed);
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Toggle up.");
                        String message;
                        if (toggle_on) {
                            Log.d(TAG, "SEND OFF VALUE");
                            message = format_message(pin, toggle_message[0]);
                            toggle_send.setImageResource(R.drawable.digital_off);
                            db.update(
                                    "digital",
                                    "state = 0",
                                    "id = 1"
                            );
                        } else {
                            Log.d(TAG, "SEND ON VALUE");
                            message = format_message(pin, toggle_message[1]);
                            toggle_send.setImageResource(R.drawable.digital_on);
                            db.update(
                                    "digital",
                                    "state = 1",
                                    "id = 1"
                            );
                        }
                        connection.write(message);
                        tv_output.setText(message);
                        toggle_on = !toggle_on;
                        return true;
                    default:
                        return false;
                }
            }
        });

        // ** fab: onClick **
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create_alert_settings();
            }
        });

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[4] = true;
        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");

        // If bluetooth is not enabled or lost connection close Activity
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled() || !GlobalVariables.connection){
            Log.d(TAG, "Bluetooth disabled or disconnected finishing Digital activity.");
            Digital.this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[4] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_digital, menu);
        if (arduino_output) {
            tv_output.setVisibility(View.VISIBLE);
            menu.getItem(0).getSubMenu().getItem(0).setTitle(R.string.action_hide_output);
        }
        else {
            tv_output.setVisibility(View.GONE);
            menu.getItem(0).getSubMenu().getItem(0).setTitle(R.string.action_show_output);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_output:
                if (arduino_output){
                    tv_output.setVisibility(View.GONE);
                    item.setTitle(R.string.action_show_output);
                    db.update("global", "is_on = 0", "item = 'arduino_output_digital'");
                } else {
                    tv_output.setVisibility(View.VISIBLE);
                    item.setTitle(R.string.action_hide_output);
                    db.update("global", "is_on = 1", "item = 'arduino_output_digital'");
                }
                arduino_output = !arduino_output;
                return true;
            case R.id.action_example:
                open_github();
                return true;
            case R.id.action_info:
                show_about_alert();
                return true;
            case R.id.action_global_settings:
                startActivity(new Intent(Digital.this, GlobalSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Create about alert **
    private void show_about_alert(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(Digital.this);
        View alert_about_view = Digital.this.getLayoutInflater().inflate(R.layout.alert_about, null);

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
                Toast.makeText(Digital.this, R.string.alert_info_toast, Toast.LENGTH_LONG).show();
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

    // ** Alert dialog: Settings **
    private String preview_pin;
    private String[] preview_toggle_message = new String[2];
    private boolean[] text_changed = new boolean[3];
    private void create_alert_settings(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(Digital.this);
        View alert_settings_view = Digital.this.getLayoutInflater().inflate(R.layout.alert_digital_settings, null);

        // ** xml items **
        final EditText et_pin = (EditText) alert_settings_view.findViewById(R.id.pin);
        final EditText et_on = (EditText) alert_settings_view.findViewById(R.id.on);
        final EditText et_off = (EditText) alert_settings_view.findViewById(R.id.off);
        final Switch sw_disable_pin = (Switch) alert_settings_view.findViewById(R.id.switch_disable_pin);

        // ** Get preview data **
        preview_pin = pin;
        preview_toggle_message = toggle_message.clone();
        text_changed[0] = false;
        text_changed[1] = false;
        text_changed[2] = false;

        cursor = db.query("select is_on from global where item = 'pin_enabled_digital'");
        cursor.moveToFirst();
        pin_enabled = (cursor.getInt(0) == 1);

        // ** Set items' state **
        sw_disable_pin.setChecked(!pin_enabled);
        et_pin.setEnabled(pin_enabled);
        et_pin.setLongClickable(pin_enabled);

        sw_disable_pin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isDisable) {
                pin_enabled = !isDisable;
                if (isDisable)
                    et_pin.setText("");
                et_pin.setEnabled(!isDisable);
                et_pin.setLongClickable(isDisable);
            }
        });

        et_pin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()){
                    Log.d(TAG, "onTextChanged: Empty");
                    preview_pin = pin;
                    text_changed[0] = false;
                }else{
                    Log.d(TAG, "onTextChanged: " + s.toString());
                    preview_pin = s.toString();
                    text_changed[0] = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et_on.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()){
                    Log.d(TAG, "onTextChanged: Empty");
                    preview_toggle_message[1] = toggle_message[get_toggle_value()];
                    text_changed[1] = false;
                }else{
                    Log.d(TAG, "onTextChanged: " + s.toString());
                    preview_toggle_message[1] = s.toString();
                    text_changed[1] = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et_off.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()){
                    Log.d(TAG, "onTextChanged: Empty");
                    preview_toggle_message[0] = toggle_message[get_toggle_value()];
                    text_changed[2] = false;
                }else{
                    Log.d(TAG, "onTextChanged: " + s.toString());
                    preview_toggle_message[0] = s.toString();
                    text_changed[2] = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        alert_builder.setPositiveButton(R.string.alert_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // ** Update variables **
                toggle_message = preview_toggle_message.clone();
                pin = preview_pin;

                // ** Update db **
                db.update(
                        "digital",
                        "pin = '" + preview_pin + "', value_on = '" + preview_toggle_message[1] + "', value_off = '" + preview_toggle_message[0] + "'",
                        "id = 1"
                );
                if (pin_enabled)
                    db.update("global", "is_on = 1" , "item = 'pin_enabled_digital'");
                else
                    db.update("global", "is_on = 0" , "item = 'pin_enabled_digital'");
                Toast.makeText(Digital.this, R.string.alert_saved, Toast.LENGTH_SHORT).show();
            }
        });

        alert_builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        alert_builder.setView(alert_settings_view);
        alert_builder.show();
        Log.d(TAG, "Alert dialog created.");
    }

    // ** Get toggle value **
    private int get_toggle_value(){
        if (toggle_on)
            return  1;
        else
            return 0;
    }

    // ** Format message **
    private String format_message(String pin, String toggle_message){
        cursor = db.query("select is_on from global where item = 'pin_enabled_digital'");
        cursor.moveToFirst();
        pin_enabled = (cursor.getInt(0) == 1);
        String separator = preferences.getString("separator_character", ".");
        if (pin_enabled){
            return preferences.getString("begin_character", "")
                    + pin + separator + toggle_message
                    + preferences.getString("end_character", "");
        }else{
            return preferences.getString("begin_character", "")
                    + toggle_message
                    + preferences.getString("end_character", "");
        }
    }
}
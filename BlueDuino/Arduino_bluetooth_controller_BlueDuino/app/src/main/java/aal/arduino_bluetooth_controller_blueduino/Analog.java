package aal.arduino_bluetooth_controller_blueduino;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class Analog extends AppCompatActivity {

    private static final String TAG = "Analog";

    // ** xml items **
    private RelativeLayout rl_rgb;
    private RelativeLayout rl_analog;

    private SeekBar[] seek_bars = new SeekBar[4];

    private ImageView rgb;
    private ImageView settings_r;
    private ImageView settings_g;
    private ImageView settings_b;
    private ImageView settings_analog;

    private TextView tv_r;
    private TextView tv_g;
    private TextView tv_b;
    private TextView[] tv_pines = new TextView[4];
    private TextView general_output;

    // ** Variables **
    private DataBase db = null;
    private Cursor cursor = null;

    private String[] pin = new String[4];
    private int[] max = new int[4];
    private int[] progress = new int[4];

    private String message;

    private boolean pin_enabled;
    private boolean output_visible;
    private boolean rgb_seek_bar;

    private SocketConnection connection;
    private ProgressTask delay_progress;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analog);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());

        // ** Get preferences **
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // ** xml items **
        rl_rgb = (RelativeLayout) findViewById(R.id.rl_rgb);
        rl_analog = (RelativeLayout) findViewById(R.id.rl_analog);

        seek_bars[0] = (SeekBar) findViewById(R.id.r_sb);
        seek_bars[1] = (SeekBar) findViewById(R.id.g_sb);
        seek_bars[2] = (SeekBar) findViewById(R.id.b_sb);
        seek_bars[3] = (SeekBar) findViewById(R.id.analog_sb);

        rgb = (ImageView) findViewById(R.id.rgb_iv);
        settings_r = (ImageView) findViewById(R.id.r_iv);
        settings_g = (ImageView) findViewById(R.id.g_iv);
        settings_b = (ImageView) findViewById(R.id.b_iv);
        settings_analog = (ImageView) findViewById(R.id.settings_iv);

        tv_r = (TextView) findViewById(R.id.tv_output_r);
        tv_g = (TextView) findViewById(R.id.tv_output_g);
        tv_b = (TextView) findViewById(R.id.tv_output_b);
        tv_pines[0] = (TextView) findViewById(R.id.tv_r_pin);
        tv_pines[1] = (TextView) findViewById(R.id.tv_g_pin);
        tv_pines[2] = (TextView) findViewById(R.id.tv_b_pin);
        tv_pines[3] = (TextView) findViewById(R.id.tv_analog_pin);
        general_output = (TextView) findViewById(R.id.tv_general_output);

        connection = new SocketConnection(GlobalVariables.connected_device);

        // ** Get settings from BlueDuino.db **
        db = GlobalVariables.db;

        // Settings: Pin, Max
        cursor = db.query("select pin, max from analog");
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++){
            pin[i] = cursor.getString(0);
            max[i] = cursor.getInt(1);
            seek_bars[i].setMax(max[i]);
            tv_pines[i].setText(String.valueOf(pin[i]));
            cursor.moveToNext();
        }
        db.log_table("analog");

        // Settings: Pin enabled
        cursor = db.query("select is_on from global where item = 'pin_enabled_analog'");
        cursor.moveToFirst();
        pin_enabled = (cursor.getInt(0) == 1);
        Log.d(TAG, "PIN ENABLED: " + pin_enabled);

        // ** Progress listener **
        seek_bars[0].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Analog.this.progress[0] = progress;
                message = format_message(pin[0], progress);
                // Update labels
                update_output();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch");
                delay_progress = new ProgressTask();
                delay_progress.set_delay(Integer.parseInt(preferences.getString("send_rate", "20")));
                delay_progress.execute();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch");
                delay_progress.cancel(true);
                connection.write(message);
            }
        });
        seek_bars[1].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Analog.this.progress[1] = progress;
                message = format_message(pin[1], progress);
                // Update labels
                update_output();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch");
                delay_progress = new ProgressTask();
                delay_progress.set_delay(Integer.parseInt(preferences.getString("send_rate", "20")));
                delay_progress.execute();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch");
                delay_progress.cancel(true);
                connection.write(message);
            }
        });
        seek_bars[2].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Analog.this.progress[2] = progress;
                message = format_message(pin[2], progress);
                // Update labels
                update_output();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch");
                delay_progress = new ProgressTask();
                delay_progress.set_delay(Integer.parseInt(preferences.getString("send_rate", "20")));
                delay_progress.execute();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch");
                delay_progress.cancel(true);
                connection.write(message);
            }
        });
        seek_bars[3].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Format message to send
                message = format_message(pin[3], progress);
                // Update label
                general_output.setText(message);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch");
                delay_progress = new ProgressTask();
                delay_progress.set_delay(Integer.parseInt(preferences.getString("send_rate", "20")));
                delay_progress.execute();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch");
                delay_progress.cancel(true);
                connection.write(message);
            }
        });

        // ** Settings for each seek bar **
        settings_r.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Action down");
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Action up");
                        create_analog_dialog(0);
                        return true;
                    default:
                        return false;
                }
            }
        });

        settings_g.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Action down");
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Action up");
                        create_analog_dialog(1);
                        return true;
                    default:
                        return false;
                }
            }
        });

        settings_b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Action down");
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Action up");
                        create_analog_dialog(2);
                        return true;
                    default:
                        return false;
                }
            }
        });

        settings_analog.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Action down");
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Action up");
                        create_analog_dialog(3);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[3] = true;
        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");

        // If bluetooth is not enabled or lost connection close Activity
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled() || !GlobalVariables.connection){
            Log.d(TAG, "Bluetooth disabled or disconnected finishing Analog activity.");
            Analog.this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[3] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_analog, menu);

        // Settings analog-rgb layout
        cursor = db.query("select is_on from global where item = 'analog_view'");
        cursor.moveToFirst();
        if(cursor.getInt(0) == 1) {
            rgb_seek_bar = false;
            show_analog_layout();
            menu.getItem(0).getSubMenu().getItem(1).setTitle(R.string.action_only_rgb);
            general_output.setText(format_message(tv_pines[3].getText().toString(), max[3]));
            Log.d(TAG, "Analog on");
        } else{
            rgb_seek_bar = true;
            show_rgb_layout();
            menu.getItem(0).getSubMenu().getItem(1).setTitle(R.string.action_only_analog);
            general_output.setText(format_message(tv_pines[0].getText().toString(), max[0]));
            Log.d(TAG, "RGB on");
        }

        // Settings arduino output
        cursor = db.query("select is_on from global where item = 'arduino_output_analog'");
        cursor.moveToFirst();
        if(cursor.getInt(0) == 1) {
            output_visible = true;
            general_output.setVisibility(View.VISIBLE);
            menu.getItem(0).getSubMenu().getItem(0).setTitle(R.string.action_hide_output);
            Log.d(TAG, "Output on");
        }
        else{
            output_visible = false;
            general_output.setVisibility(View.GONE);
            menu.getItem(0).getSubMenu().getItem(0).setTitle(R.string.action_show_output);
            Log.d(TAG, "Output off");
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_seek_bar:
                if (rgb_seek_bar) {
                    item.setTitle(R.string.action_only_rgb);
                    show_analog_layout();
                    db.update("global", "is_on = 1", "item = 'analog_view'");
                }
                else{
                    item.setTitle(R.string.action_only_analog);
                    show_rgb_layout();
                    db.update("global", "is_on = 0", "item = 'analog_view'");
                }
                rgb_seek_bar = !rgb_seek_bar;
                return true;
            case R.id.action_output:
                if (output_visible) {
                    item.setTitle(R.string.action_show_output);
                    general_output.setVisibility(View.GONE);
                    db.update("global", "is_on = 0", "item = 'arduino_output_analog'");
                }
                else{
                    item.setTitle(R.string.action_hide_output);
                    general_output.setVisibility(View.VISIBLE);
                    db.update("global", "is_on = 1", "item = 'arduino_output_analog'");
                }
                output_visible = !output_visible;
                return true;
            case R.id.action_example:
                open_github();
                return true;
            case R.id.action_info:
                show_about_alert();
                return true;
            case R.id.action_global_settings:
                startActivity(new Intent(Analog.this, GlobalSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Create about alert **
    private void show_about_alert(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(Analog.this);
        View alert_about_view = Analog.this.getLayoutInflater().inflate(R.layout.alert_about, null);

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
                Toast.makeText(Analog.this, R.string.alert_info_toast, Toast.LENGTH_LONG).show();
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

    // ** Show rgb layout **
    private void show_rgb_layout(){
        rl_rgb.setVisibility(View.VISIBLE);
        rl_analog.setVisibility(View.GONE);
    }

    // ** Show analog layout **
    private void show_analog_layout(){
        rl_analog.setVisibility(View.VISIBLE);
        rl_rgb.setVisibility(View.GONE);
    }

    // ** Update output for rgb seek bars **
    private void update_output(){
        tv_r.setText(String.valueOf(progress[0]));
        tv_g.setText(String.valueOf(progress[1]));
        tv_b.setText(String.valueOf(progress[2]));

        general_output.setText(message);

        rgb.setBackgroundColor(Color.rgb(progress[0], progress[1], progress[2]));
    }

    // ** Alert dialog to customize settings **
    private String preview_pin;
    private int preview_max;
    private void create_analog_dialog(final int id){
        preview_pin = Analog.this.pin[id];
        preview_max = Analog.this.max[id];
        final AlertDialog.Builder settings = new AlertDialog.Builder(Analog.this);
        final View dialogView = Analog.this.getLayoutInflater().inflate(R.layout.alert_analog_settings, null);
        settings.setView(dialogView);

        final EditText et_pin =  (EditText) dialogView.findViewById(R.id.pin);
        final EditText et_max =  (EditText) dialogView.findViewById(R.id.max);
        final Switch sw_disable_pin = (Switch) dialogView.findViewById(R.id.switch_disable_pin);

        cursor = db.query("select is_on from global where item = 'pin_enabled_analog'");
        cursor.moveToFirst();
        pin_enabled = (cursor.getInt(0) == 1);

        sw_disable_pin.setChecked(!pin_enabled);
        et_pin.setEnabled(pin_enabled);
        et_pin.setLongClickable(pin_enabled);

        sw_disable_pin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isDisable) {
                pin_enabled = !isDisable;
                if (isDisable)
                    et_pin.setText("");
                et_pin.setEnabled(!isDisable);
                et_pin.setLongClickable(isDisable);
            }
        });

        // PIN: onTextChanged
        et_pin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    Log.d(TAG, "Not empty: " + s.toString());
                    preview_pin = s.toString();
                } else {
                    Log.d(TAG, "Empty");
                    preview_pin = Analog.this.pin[id];
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });

        // MAX: onTextChanged
        et_max.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0){
                    Log.d(TAG, "Not Empty: " + s.toString());
                    preview_max = Integer.parseInt(s.toString());
                }else{
                    Log.d(TAG, "Empty");
                    preview_max = Analog.this.max[id];
                }
            }
        });

        settings.setPositiveButton(R.string.alert_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int max;
                final String pin;

                // ** Check if any field is empty **
                                                                             // Empty fields: None
                if (!et_max.getText().toString().isEmpty() && !et_pin.getText().toString().isEmpty()) {
                    max = Integer.parseInt(et_max.getText().toString());
                    pin = et_pin.getText().toString();

                    // ** Update xml items **
                    Analog.this.pin[id] = pin;
                    tv_pines[id].setText(pin);
                    Analog.this.max[id] = max;
                    seek_bars[id].setMax(max);

                    // ** Update database **
                    db.update("analog", "pin = '" + pin + "', max = " + max, "id = " + (id + 1));
                } else {
                    if (!et_max.getText().toString().isEmpty()){             // Empty fields: Pin
                        max = Integer.parseInt(et_max.getText().toString());

                        // ** Update xml items **
                        Analog.this.max[id] = max;
                        seek_bars[id].setMax(max);

                        // ** Update database **
                        db.update("analog", "max = " + max, "id = " + (id + 1));
                    }else if (!et_pin.getText().toString().isEmpty()){       // Empty fields: Max
                        pin = et_pin.getText().toString();

                        // ** Update xml items **
                        Analog.this.pin[id] = pin;
                        tv_pines[id].setText(String.valueOf(pin));

                        // ** Update database **
                        db.update("analog", "pin = '" + pin + "'", "id = " + (id + 1));
                    }
                    if (pin_enabled)
                        db.update("global", "is_on = 1" , "item = 'pin_enabled_analog'");
                    else
                        db.update("global", "is_on = 0" , "item = 'pin_enabled_analog'");
                }
                Toast.makeText(Analog.this, R.string.alert_saved, Toast.LENGTH_SHORT).show();

            }
        });
        settings.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Cancelled");
            }
        });

        AlertDialog alertDialog = settings.create();
        alertDialog.show();
    }

    // ** Format message **
    private String format_message(String pin, int max){
        cursor = db.query("select is_on from global where item = 'pin_enabled_analog'");
        cursor.moveToFirst();
        pin_enabled = (cursor.getInt(0) == 1);
        String separator = preferences.getString("separator_character", ".");
        if (pin_enabled){
            return preferences.getString("begin_character", "")
                    + pin + separator + max
                    + preferences.getString("end_character", "");
        }else{
            return preferences.getString("begin_character", "")
                    + max
                    + preferences.getString("end_character", "");
        }
    }

    // ** Send rate task **
    private class ProgressTask extends AsyncTask <Integer, Integer, Void>{
        private int rate;
        public void set_delay(int delay){
            rate = delay;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            Log.d(TAG, "In background.");
            while (!isCancelled()){
                try {
                    Thread.sleep(rate);
                    connection.write(message);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupted.");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "Finished.");
        }
    }
}

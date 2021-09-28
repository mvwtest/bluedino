package aal.arduino_bluetooth_controller_blueduino;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class JoystickConfigure extends AppCompatActivity {
    private static final String TAG = "JoystickConfigure";
    // ** Variables **
    private String[] key_down = new String[10];
    private String[] key_up   = new String[10];

    private Cursor cursor;
    private DataBase db;

    // ** xml items **
    private ImageView[] iv_key      = new ImageView[10];
    private EditText[]  et_key_down = new EditText[10];
    private EditText[]  et_key_up   = new EditText[10];
    private Switch output_switch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick_configure);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // ** xml items **
        iv_key[0]  =  (ImageView) findViewById(R.id.iv_up);
        iv_key[1]  =  (ImageView) findViewById(R.id.iv_right);
        iv_key[2]  =  (ImageView) findViewById(R.id.iv_down);
        iv_key[3]  =  (ImageView) findViewById(R.id.iv_left);
        iv_key[4]  =  (ImageView) findViewById(R.id.iv_w);
        iv_key[5]  =  (ImageView) findViewById(R.id.iv_d);
        iv_key[6]  =  (ImageView) findViewById(R.id.iv_s);
        iv_key[7]  =  (ImageView) findViewById(R.id.iv_a);
        iv_key[8]  =  (ImageView) findViewById(R.id.iv_start);
        iv_key[9]  =  (ImageView) findViewById(R.id.iv_stop);

        et_key_down[0]  =  (EditText)  findViewById(R.id.et_up);
        et_key_down[1]  =  (EditText)  findViewById(R.id.et_right);
        et_key_down[2]  =  (EditText)  findViewById(R.id.et_down);
        et_key_down[3]  =  (EditText)  findViewById(R.id.et_left);
        et_key_down[4]  =  (EditText)  findViewById(R.id.et_w);
        et_key_down[5]  =  (EditText)  findViewById(R.id.et_d);
        et_key_down[6]  =  (EditText)  findViewById(R.id.et_s);
        et_key_down[7]  =  (EditText)  findViewById(R.id.et_a);
        et_key_down[8]  =  (EditText)  findViewById(R.id.et_start);
        et_key_down[9]  =  (EditText)  findViewById(R.id.et_stop);

        et_key_up[0]  =  (EditText)  findViewById(R.id.et_up_up);
        et_key_up[1]  =  (EditText)  findViewById(R.id.et_right_up);
        et_key_up[2]  =  (EditText)  findViewById(R.id.et_down_up);
        et_key_up[3]  =  (EditText)  findViewById(R.id.et_left_up);
        et_key_up[4]  =  (EditText)  findViewById(R.id.et_w_up);
        et_key_up[5]  =  (EditText)  findViewById(R.id.et_d_up);
        et_key_up[6]  =  (EditText)  findViewById(R.id.et_s_up);
        et_key_up[7]  =  (EditText)  findViewById(R.id.et_a_up);
        et_key_up[8]  =  (EditText)  findViewById(R.id.et_start_up);
        et_key_up[9]  =  (EditText)  findViewById(R.id.et_stop_up);

        output_switch = findViewById(R.id.sw_output);

        FloatingActionButton edit = (FloatingActionButton) findViewById(R.id.edit);
        FloatingActionButton save = (FloatingActionButton) findViewById(R.id.save);

        // ** Get settings **
        db = GlobalVariables.db;

        cursor = db.query("select is_on from global where item = 'arduino_output_joystick'");
        cursor.moveToFirst();
        output_switch.setChecked((cursor.getInt(0) == 1));
        db.log_table("global");

        cursor = db.query("select key_down, key_up from joystick");
        cursor.moveToFirst();
        Log.d(TAG, "ROWS: " + cursor.getCount() + " | COLUMNS: " + cursor.getColumnCount());
        for (int i = 0; i < cursor.getCount(); i++){
            for (int j = 0; j < cursor.getColumnCount(); j++){
                switch (j){
                    case 0: // Key down values
                        key_down[i] = cursor.getString(j);
                        et_key_down[i].setHint(getString(R.string.key_pressed) + ": " + key_down[i]);
                        break;
                    case 1: // Key up values
                        key_up[i] = cursor.getString(j);
                        et_key_up[i].setHint(getString(R.string.key_released) + ": " + key_up[i]);
                        break;
                }
            }
            cursor.moveToNext();
        }
        db.log_table("joystick");

        // ** fab: OnClick **
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(JoystickConfigure.this);
                String[] items = new String[] {
                        getString(R.string.alert_clear),
                        getString(R.string.alert_reset)
                };
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                clear_fields();
                                break;
                            case 1:
                                reset_settings();
                                break;
                        }
                    }
                });
                builder.show();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save_settings();
                go_to_joystick();
                Toast.makeText(JoystickConfigure.this, R.string.alert_saved, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[7] = true;

        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");

        // If bluetooth is not enabled or lost connection close Activity
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled() || !GlobalVariables.connection){
            Log.d(TAG, "Bluetooth disabled or disconnected finishing JoystickConfigure activity.");
            JoystickConfigure.this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[7] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    @Override
    public void onBackPressed() {
        go_to_joystick();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                go_to_joystick();
                return true;
            case R.id.action_example:
                open_github();
                return true;
            case R.id.action_info:
                show_about_alert();
                return true;
            case R.id.action_global_settings:
                startActivity(new Intent(JoystickConfigure.this, GlobalSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Create about alert **
    private void show_about_alert(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(JoystickConfigure.this);
        View alert_about_view = JoystickConfigure.this.getLayoutInflater().inflate(R.layout.alert_about, null);

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
                Toast.makeText(JoystickConfigure.this, R.string.alert_info_toast, Toast.LENGTH_LONG).show();
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

    // ** Clear edit text **
    private void clear_fields(){
        int size = 0;
        if (et_key_down.length == et_key_up.length)
            size = (et_key_down.length + et_key_up.length) / 2;
        for (int i = 0; i < size; i ++){
            et_key_down[i].setText("");
            et_key_up[i].setText("");
        }
    }

    // ** Overwrite edit text **
    private void reset_settings(){
        et_key_up[0].setText("UP");
        et_key_up[1].setText("RIGHT");
        et_key_up[2].setText("DOWN");
        et_key_up[3].setText("LEFT");
        et_key_up[4].setText("W");
        et_key_up[5].setText("D");
        et_key_up[6].setText("S");
        et_key_up[7].setText("A");
        et_key_up[8].setText("START");
        et_key_up[9].setText("STOP");

        et_key_down[0].setText("up");
        et_key_down[1].setText("right");
        et_key_down[2].setText("down");
        et_key_down[3].setText("left");
        et_key_down[4].setText("w");
        et_key_down[5].setText("d");
        et_key_down[6].setText("s");
        et_key_down[7].setText("a");
        et_key_down[8].setText("start");
        et_key_down[9].setText("stop");
    }

    // ** Save the settings **
    private void save_settings(){
        Log.d(TAG, "Saving.");
        int size = 0;
        if (et_key_down.length == et_key_up.length){
            size = (et_key_down.length + et_key_up.length) / 2;

            for (int i = 0; i < size; i ++){
                if (!et_key_down[i].getText().toString().isEmpty())
                    key_down[i] = et_key_down[i].getText().toString();
                if (!et_key_up[i].getText().toString().isEmpty())
                    key_up[i] = et_key_up[i].getText().toString();
            }
        }

        for (int i = 0; i < size; i ++){
            db.update("joystick",
                    "key_down = '" + key_down[i] + "', key_up= '" + key_up[i] + "'",
                    "id = " + (i+1)
            );
        }

        if (output_switch.isChecked()){
            db.update(
                    "global",
                    "is_on = 1",
                    "item = 'arduino_output_joystick'"
            );
        }else{
            db.update(
                    "global",
                    "is_on = 0",
                    "item = 'arduino_output_joystick'"
            );
        }
    }

    // ** Go to Joystick **
    private void go_to_joystick(){
        Intent intent = new Intent(JoystickConfigure.this, Joystick.class);
        startActivity(intent);
        JoystickConfigure.this.finish();
    }
}
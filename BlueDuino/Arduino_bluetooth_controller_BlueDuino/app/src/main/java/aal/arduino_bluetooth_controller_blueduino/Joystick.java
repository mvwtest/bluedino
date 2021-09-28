package aal.arduino_bluetooth_controller_blueduino;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class Joystick extends AppCompatActivity {

    private static final String TAG = "Joystick";

    // ** xml items **

    private ImageButton buttons[] = new ImageButton[10];
    /* ORDER [0:9]
            db.execSQL("insert into joystick (id, key_down, key_up) values (1, 'up', 'UP');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (2, 'right', 'RIGHT');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (3, 'down', 'DOWN');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (4, 'left', 'LEFT');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (5, 'w', 'W');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (6, 'd', 'D');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (7, 's', 'S');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (8, 'a', 'A');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (9, 'start', 'START');");
            db.execSQL("insert into joystick (id, key_down, key_up) values (10, 'stop', 'STOP');");
     */

    // ** Variables **
    private String[] down_messages =  new String[10];
    private String[] up_messages =  new String[10];

    private SocketConnection connection;
    private DataBase db;
    private Cursor cursor;

    private SharedPreferences preferences;

    @SuppressLint({"ClickableViewAccessibility", "SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());

        // ** Get preferences **
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // ** xml items **
        buttons[0] = (ImageButton) findViewById(R.id.bt_up);
        buttons[1] = (ImageButton) findViewById(R.id.bt_right);
        buttons[2] = (ImageButton) findViewById(R.id.bt_down);
        buttons[3] = (ImageButton) findViewById(R.id.bt_left);
        buttons[4] = (ImageButton) findViewById(R.id.bt_w);
        buttons[5] = (ImageButton) findViewById(R.id.bt_d);
        buttons[6] = (ImageButton) findViewById(R.id.bt_s);
        buttons[7] = (ImageButton) findViewById(R.id.bt_a);
        buttons[8] = (ImageButton) findViewById(R.id.bt_start);
        buttons[9] = (ImageButton) findViewById(R.id.bt_stop);

        final TextView tv_output = findViewById(R.id.tv_output);

        connection = new SocketConnection(GlobalVariables.connected_device);
        db = GlobalVariables.db;

        // ** Get settings **
        db = GlobalVariables.db;
        cursor = db.query("select is_on from global where item = 'arduino_output_joystick'");
        cursor.moveToFirst();
        db.log_table("global");
        if (cursor.getInt(0) == 1)
            tv_output.setVisibility(View.VISIBLE);
        else
            tv_output.setVisibility(View.GONE);

        cursor = db.query("select key_down, key_up from joystick");
        cursor.moveToFirst();

        Log.d(TAG, "ROWS: " + cursor.getCount() + " | COLUMNS: " + cursor.getColumnCount());

        for (int i = 0; i < cursor.getCount(); i++){
            for (int j = 0; j < cursor.getColumnCount(); j++){
                switch (j){
                    case 0:
                        down_messages[i] = cursor.getString(j);
                        break;
                    case 1:
                        up_messages[i] = cursor.getString(j);
                        break;
                    default:
                        Log.d(TAG, "Error in cursor-db.");
                }
            }
            cursor.moveToNext();
        }
        db.log_table("joystick");

        // ** JS Left: OnTouchListener **
        for (int i = 0; i < buttons.length; i++){
            final int j = i;
            buttons[j].setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    String m;
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            Log.d(TAG, "onTouch: down.");

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                if (j < 4)
                                    buttons[j].setBackground(getDrawable(R.drawable.round_key_button_pressed));
                                else if (j < 8)
                                    buttons[j].setBackground(getDrawable(R.drawable.circular_button_pressed));
                                else
                                    buttons[j].setBackground(getDrawable(R.drawable.round_button_pressed));
                            }
                            m = format_message(down_messages[j]);
                            connection.write(m);
                            tv_output.setText(m);
                            return true;
                        case MotionEvent.ACTION_UP:
                            Log.d(TAG, "onTouch: up.");

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                if (j < 4)
                                    buttons[j].setBackground(getDrawable(R.drawable.round_key_button));
                                else if (j < 8)
                                    buttons[j].setBackground(getDrawable(R.drawable.circular_button));
                                else
                                    buttons[j].setBackground(getDrawable(R.drawable.round_button));
                            }
                            m = format_message(up_messages[j]);
                            connection.write(m);
                            tv_output.setText(m);
                            return true;
                        default:
                            return false;
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[5] = true;

        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");

        // If bluetooth is not enabled or lost connection close Activity
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled() || !GlobalVariables.connection){
            Log.d(TAG, "Bluetooth disabled or disconnected finishing Joystick activity.");
            Joystick.this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[5] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_joystick, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(Joystick.this, JoystickConfigure.class);
                startActivity(intent);
                Joystick.this.finish();
                return true;
            case R.id.action_example:
                open_github();
                return true;
            case R.id.action_info:
                show_about_alert();
                return true;
            case R.id.action_global_settings:
                startActivity(new Intent(Joystick.this, GlobalSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Create about alert **
    private void show_about_alert(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(Joystick.this);
        View alert_about_view = Joystick.this.getLayoutInflater().inflate(R.layout.alert_about, null);

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
                Toast.makeText(Joystick.this, R.string.alert_info_toast, Toast.LENGTH_LONG).show();
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

    // ** Format message **
    private String format_message(String message){
        return preferences.getString("begin_character", "")
                + message
                + preferences.getString("end_character", "");
    }
}

package aal.arduino_bluetooth_controller_blueduino;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;


public class Terminal extends AppCompatActivity {

    private static final String TAG = "Terminal";

    // ** xml items **
    private EditText et_message;
    private ImageView send;
    private TextView terminal;
    private ScrollView scroll;

    // ** Variables **
    private BluetoothDevice paired_device;
    private SocketConnection connection;
    private Thread receive_thread;
    private SharedPreferences preferences;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(TAG, "Running in thread: " + Thread.currentThread().getId());

        // ** Variables **
        paired_device = GlobalVariables.connected_device;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // ** xml items **
        et_message = (EditText) findViewById(R.id.message);
        send = (ImageView) findViewById(R.id.send);
        terminal = (TextView) findViewById(R.id.terminal);
        scroll = (ScrollView) findViewById(R.id.sv);

        // Start connection task
        connection = new SocketConnection(paired_device);

        // ** TouchListener: Send **
        send.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Send: Action down.");
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Send: Action up.");
                        String user_text = et_message.getText().toString();

                        // Toast if empty message
                        if (user_text.isEmpty())
                            Toast.makeText(Terminal.this, R.string.empty, Toast.LENGTH_SHORT).show();
                        else {
                            // Clean text
                            et_message.setText("");
                            // Write message
                            user_text = preferences.getString("begin_character", "")
                                    + user_text
                                    + preferences.getString("end_character", "");
                            Log.d(TAG, "Message: " + user_text);
                            connection.write(user_text);
                            String terminal_string = terminal.getText().toString();
                            if (!terminal_string.isEmpty()){
                                if (terminal.getText().toString().charAt(terminal.getText().toString().length() - 1) != '\n')
                                    terminal.append("\n");
                            }
                            append_colored_text(terminal,
                                    preferences.getString("username",
                                            getResources().getString(R.string.app_name))
                                            + ": " + user_text + "\n",
                                    getResources().getColor(R.color.colorTerminalSendText)
                            );
                            // Full scroll down
                            scroll.fullScroll(View.FOCUS_DOWN);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        // ** Add listener for incoming data from bluetooth module **
        final Handler handler = new Handler();
        receive_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Listening");
                GlobalVariables.listening = true;
                connection.clean_stream(Integer.parseInt(preferences.getString("receive_rate", "150")));
                while(!Thread.currentThread().isInterrupted() && GlobalVariables.listening) {
                    connection.listen(Integer.parseInt(preferences.getString("receive_rate", "150")));
                    handler.post(new Runnable() {
                        public void run() {
                            String terminal_string = terminal.getText().toString();
                            String output = "";
                            if (terminal_string.isEmpty()){
                                output = output.concat(paired_device.getName()+": ");
                            }else {
                                if (terminal_string.charAt(terminal_string.length() - 1) == '\n'){
                                    output = output.concat(paired_device.getName()+": ");
                                }
                            }
                            append_colored_text(terminal,
                                    output + GlobalVariables.received_message,
                                    getResources().getColor(R.color.colorTerminalReceiveText)
                            );
                            if (preferences.getBoolean("auto_scroll", true)){
                                scroll.fullScroll(View.FOCUS_DOWN);
                            }
                        }
                    });
                }
            }
        });
        receive_thread.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        interrupt_receive();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();
        GlobalVariables.activity_on[6] = true;
        if (!GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IS RUNNING===============");

        // If bluetooth is not enabled or lost connection close Activity
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled() || !GlobalVariables.connection){
            Log.d(TAG, "Bluetooth disabled or disconnected finishing Terminal activity.");
            interrupt_receive();
            Terminal.this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called.");
        GlobalVariables.activity_on[6] = false;
        if (GlobalVariables.is_app_in_foreground())
            Log.d(TAG, "===============APP IN FOREGROUND===============");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_terminal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                interrupt_receive();
                return true;
            case R.id.action_clean:
                terminal.setText("");
                return true;
            case R.id.action_example:
                open_github();
                return true;
            case R.id.action_info:
                show_about_alert();
                return true;
            case R.id.action_global_settings:
                startActivity(new Intent(Terminal.this, GlobalSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ** Close receive_thread **
    private void interrupt_receive(){
        GlobalVariables.listening = false;
        receive_thread.interrupt();
        Log.d(TAG, "Listening thread: " + receive_thread.isAlive());
    }
    // ** Append text with color **
    private void append_colored_text(TextView tv, String text, int color){
        int start = tv.getText().length();
        tv.append(text);
        int end = tv.getText().length();
        Spannable spannable_text = (Spannable) tv.getText();
        spannable_text.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }
    // ** Create about alert **
    private void show_about_alert(){
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(Terminal.this);
        View alert_about_view = Terminal.this.getLayoutInflater().inflate(R.layout.alert_about, null);

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
                Toast.makeText(Terminal.this, R.string.alert_info_toast, Toast.LENGTH_LONG).show();
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
}

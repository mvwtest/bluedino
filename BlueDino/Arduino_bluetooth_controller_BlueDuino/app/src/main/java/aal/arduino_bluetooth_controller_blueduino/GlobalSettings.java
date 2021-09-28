package aal.arduino_bluetooth_controller_blueduino;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class GlobalSettings extends AppCompatActivity {

    private static final String TAG = "GlobalSettings";
    private boolean already_disconnected;
    private static Context this_context;
    private static SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        already_disconnected = (GlobalVariables.connected_device == null);
        this_context = this;
    }

    static EditTextPreference username;
    static Preference reset;
    static SwitchPreferenceCompat dark_theme;
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            username = findPreference("username");
            reset = findPreference("reset");
            dark_theme = findPreference("theme");
            if (Build.VERSION_CODES.KITKAT > Build.VERSION.SDK_INT){
                reset.setVisible(false);
                reset.setEnabled(false);
                reset.setSelectable(false);
            }else {
                reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this_context);
                        builder.setTitle(R.string.title_popup_reset)
                                .setMessage(R.string.confirm_close_app)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User wants to erase app data
                                        ((ActivityManager) getContext().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // Do nothing
                                    }
                                }).show();
                        return true;
                    }
                });
            }
            dark_theme.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final boolean dark_enabled = preferences.getBoolean("theme", false);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this_context);
                    builder.setTitle(R.string.title_popup_theme)
                            .setMessage(R.string.confirm_change_theme)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (dark_enabled){
                                        AppCompatDelegate.setDefaultNightMode(
                                                AppCompatDelegate.MODE_NIGHT_YES
                                        );
                                    }else{
                                        AppCompatDelegate.setDefaultNightMode(
                                                AppCompatDelegate.MODE_NIGHT_NO
                                        );
                                    }
                                }
                            })
                            .setCancelable(false)
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dark_theme.setChecked(!dark_enabled);
                                }
                            }).show();
                    return false;
                }
            });
        }
    }
    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("username")){
                if (preferences.getString("username", "").compareTo("") == 0)
                    username.setText(getResources().getString(R.string.app_name));
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Listener registered");
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Listener unregistered");
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!GlobalVariables.connection && !already_disconnected)
            Toast.makeText(GlobalSettings.this, R.string.connection_lost, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
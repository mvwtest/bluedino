package aal.arduino_bluetooth_controller_blueduino;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by Arturo on 04/07/2018.
 */
public class DataBase {
    private static final String TAG = "DataBase";

    private SQLiteDatabase db = null;
    private Cursor cursor = null;
    private Context application;

    DataBase(Context context){
        application = context;
    }

    public void ifNotGeneratedGenerateSettings(){
        Log.d(TAG, "========Setting up BlueDuino.db========");

        // Check if BlueDuino.db exists
        boolean blueduino_db = false;
        if (application.databaseList().length > 0){
            if (application.databaseList()[0].equals("BlueDuino.db"))
                blueduino_db = true;
        }

        // If BlueDuino.db exists
        if (blueduino_db){
            Log.d(TAG, "BlueDuino.db exists.");
            db = application.openOrCreateDatabase("BlueDuino.db", application.MODE_PRIVATE, null);
        }
        else{
            Log.d(TAG, "Brand new BlueDuino.db.");
            db = application.openOrCreateDatabase("BlueDuino.db", application.MODE_PRIVATE, null);

            // Setup [Global settings]
            db.execSQL("create table if not exists global (id integer primary key, item text, is_on integer);");

            db.execSQL("insert into global (id, item, is_on) values (1, 'pin_enabled_digital', 0);");
            db.execSQL("insert into global (id, item, is_on) values (2, 'analog_view', 1);");
            db.execSQL("insert into global (id, item, is_on) values (3, 'arduino_output_digital', 1);");
            db.execSQL("insert into global (id, item, is_on) values (4, 'pin_enabled_analog', 0);");
            db.execSQL("insert into global (id, item, is_on) values (5, 'arduino_output_analog', 1);");
            db.execSQL("insert into global (id, item, is_on) values (6, 'arduino_output_joystick', 1);");

            // Setup [Analog]
            db.execSQL("create table if not exists analog (id integer primary key, pin text, max integer);");

            db.execSQL("insert into analog (id, pin, max) values (1, 'R', 255);"); // R
            db.execSQL("insert into analog (id, pin, max) values (2, 'G', 255);"); // G
            db.execSQL("insert into analog (id, pin, max) values (3, 'B', 255);"); // B
            db.execSQL("insert into analog (id, pin, max) values (4, 'A', 255);"); // Slider

            // Setup [Digital]
            db.execSQL("create table if not exists digital (id integer primary key, pin text, value_on text, value_off text, state integer);");

            db.execSQL("insert into digital (id, pin, value_on, value_off, state) values (1, 'D', 'ON', 'OFF', 0);");

            // Setup [Joystick]
            db.execSQL("create table if not exists joystick (id integer primary key, key_down text, key_up text);");

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
        }
    }

    public void log_table(String target){
        // Query
        cursor = db.rawQuery("select * from " + target, null);
        cursor.moveToFirst();

        // Table info
        Log.d(TAG, "====|Table: " + target + "|Rows: " + cursor.getCount() + "|Columns " + cursor.getColumnCount() + "|====");

        // Print rows
        for (int i = 0; i < cursor.getCount(); i ++){
            String row = "";
            for (int j = 0; j < cursor.getColumnCount(); j ++){
                if (j == cursor.getColumnCount()-1)
                    row += cursor.getColumnName(j) + " : " + cursor.getString(j) + ".";
                else
                    row += cursor.getColumnName(j) + " : " + cursor.getString(j) + ", ";
            }
            Log.d(TAG, row);
            cursor.moveToNext();
        }
    }

    public void update(String table, String values, String condition){
        String command = "update " + table + " set " + values + " where " + condition + ";";
        Log.d(TAG, "Query: " + command);
        db.execSQL(command);
        log_table(table);
    }

    public Cursor query(String query){
        return db.rawQuery(query, null);
    }
}

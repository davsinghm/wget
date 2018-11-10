package com.davsinghm.wget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("HardCodedStringLiteral")
public class DInfoHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "dinfo.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_VIDEO = "info_video";
    public static final String TABLE_AUDIO = "info_audio";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_STRING = "string";
    private static final String COLUMN_STATE = "state";

    private static final String CREATE_TABLE = "("
            + COLUMN_ID + " text primary key, "
            + COLUMN_STRING + " text, "
            + COLUMN_STATE + " text"
            + ");";

    private static DInfoHelper sInstance;

    private SQLiteDatabase database;

    private DInfoHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static synchronized void createInstance(Context context) {
        sInstance = new DInfoHelper(context);
    }

    public static synchronized DInfoHelper getInstance(Context context) {
        if (sInstance == null)
            createInstance(context.getApplicationContext());
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("create table " + TABLE_VIDEO + CREATE_TABLE);
        database.execSQL("create table " + TABLE_AUDIO + CREATE_TABLE);
    }

    public synchronized void open() throws SQLException {
        if (database == null || !database.isOpen())
            database = getWritableDatabase();
    }

    public synchronized void addInfo(String TABLE, String id, String string, String state) {
        open();

        ContentValues info = new ContentValues();
        info.put(COLUMN_ID, id);
        info.put(COLUMN_STRING, string);
        info.put(COLUMN_STATE, state);

        if (checkInfoExists(TABLE, id))
            database.update(TABLE, info, COLUMN_ID + "=?", new String[]{id});
        else
            database.insert(TABLE, null, info);

    }

    public synchronized void addInfoString(String TABLE, String id, String string) {
        open();

        ContentValues info = new ContentValues();
        info.put(COLUMN_ID, id);
        info.put(COLUMN_STRING, string);

        if (checkInfoExists(TABLE, id))
            database.update(TABLE, info, COLUMN_ID + "=?", new String[]{id});
        else
            database.insert(TABLE, null, info);

    }

    public synchronized void addInfoState(String TABLE, String id, String state) {
        open();

        ContentValues info = new ContentValues();
        info.put(COLUMN_ID, id);
        info.put(COLUMN_STATE, state);

        if (checkInfoExists(TABLE, id))
            database.update(TABLE, info, COLUMN_ID + "=?", new String[]{id});
        else
            database.insert(TABLE, null, info);
    }

    @Nullable
    public synchronized String getInfoString(String TABLE, String id) {
        open();

        Cursor cursor = database.query(TABLE,
                new String[]{COLUMN_STRING}, COLUMN_ID + "=?", new String[]{id}, null, null, null);
        if (cursor.getCount() <= 0)
            return null;
        cursor.moveToFirst();
        String string = cursor.getString(0);

        cursor.close();
        return string;
    }

    /**
     * @param id    Download UID
     * @param TABLE Video or Audio table
     * @return ONGOING, COMPLETE, ERROR, STOPPED, MUX_ERROR, ENCODE_ERROR
     */
    public synchronized String getInfoState(String TABLE, String id) {
        open();

        Cursor cursor = database.query(TABLE,
                new String[]{COLUMN_STATE}, COLUMN_ID + "=?", new String[]{id}, null, null, null);
        if (cursor.getCount() <= 0)
            return null;
        cursor.moveToFirst();
        String string = cursor.getString(0);

        cursor.close();
        return string;
    }

    //this ignores the ONGOING state, as it's checked by DManager, so only inactive states are needed.
    @NonNull
    public static DState getInactiveDStateFromString(String state) {
        if (state != null)
            switch (state) {
                // case "DONE":
                // return DState.DONE;
                // case "ONGOING": return "STOPPED";
                // case "STOPPED": return "STOPPED";
                case "COMPLETE":
                    return DState.COMPLETE;
                case "ERROR":
                    return DState.ERROR;
                case "MUX_ERROR":
                    return DState.MUX_ERROR;
                case "ENCODE_ERROR":
                    return DState.ENCODE_ERROR;
                // default: return DState.STOPPED;
            }

        return DState.STOPPED;
    }

    public class TempDInfo {
        public DState dInactiveState;
        public long count;
        public long length;
    }

    public synchronized HashMap<String, TempDInfo> getAllInactiveStatesAndInfo(String table) {
        open();

        HashMap<String, TempDInfo> hashMap = new HashMap<>();
        Cursor cursor = database.query(table,
                new String[]{COLUMN_ID, COLUMN_STRING, COLUMN_STATE}, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            TempDInfo dInfo = new TempDInfo();
            dInfo.dInactiveState = getInactiveDStateFromString(cursor.getString(2)); //only inactive states are added.
            getCountAndLengthFromInfoString(dInfo, cursor.getString(1));
            hashMap.put(cursor.getString(0), dInfo);
            cursor.moveToNext();
        }

        cursor.close();
        return hashMap;
    }

    private void getCountAndLengthFromInfoString(TempDInfo dInfo, String str) {
        try {
            String[] ss1 = str.split("<l>");
            long count = Long.valueOf(ss1[0]);
            long size = Long.valueOf(ss1[1]);
            dInfo.count = count;
            dInfo.length = size;
        } catch (Exception ignore) {
        }
    }

    public static long getCountFromInfoString(String string) {
        try {
            return Long.valueOf(string.split("<l>")[0]);
        } catch (Exception ignore) {
        }

        return 0;
    }

    public static long getLengthFromInfoString(String string) {
        try {
            return Long.valueOf(string.split("<l>")[1]);
        } catch (Exception ignore) {
        }

        return 0;
    }

    public synchronized void deleteInfo(String id) {
        open();
        database.delete(TABLE_VIDEO, COLUMN_ID + "=?", new String[]{id});
        database.delete(TABLE_AUDIO, COLUMN_ID + "=?", new String[]{id});
    }

    public synchronized boolean checkInfoExists(String TABLE, String downloadID) {
        open();
        Cursor cursor = database.query(TABLE, new String[]{COLUMN_ID}, COLUMN_ID + "=?", new String[]{downloadID}, null, null, null);
        boolean b = cursor.getCount() > 0;
        cursor.close();
        return b;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VIDEO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUDIO);
        onCreate(db);
    }

}
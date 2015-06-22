package com.ludoscity.bikeactivityexplorer.DBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Looney on 06-04-15.
 */
public class BixiStationDatabase extends SQLiteOpenHelper {
    static final String DB_NAME = "bixi.db";
    static final int DB_VERSION = 13;

    //Stations TABLE
    static final String TABLE_NAME = "Stations";
    static final String COLUMN_ID = "id";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_LATITUDE = "latitude";
    static final String COLUMN_LONGITUDE = "longitude";
    static final String COLUMN_NB_BIKES_AVAILABLE = "bikes_available";
    static final String COLUMN_NB_DOCKS_AVAILABLE = "docks_available";
    static final String COLUMN_FAVORITE = "favorite";
    static final String COLUMN_IS_LOCKED = "is_locked";
    static final String COLUMN_LAST_UPDATE = "last_update";

    private static BixiStationDatabase instance = null;

    //Thanks to http://www.androiddesignpatterns.com/2012/05/correctly-managing-your-sqlite-database.html
    public static synchronized BixiStationDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new BixiStationDatabase(context);
            //J'appel cette méthode pour être sur que la base de donnée va être créé pour que les autres appels n'aient pas à la créé.
            SQLiteDatabase writableDatabase = instance.getWritableDatabase();
        }
        return instance;
    }

    private BixiStationDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_LATITUDE + " REAL, "
                + COLUMN_LONGITUDE + " REAL, "
                + COLUMN_NB_BIKES_AVAILABLE + " INTEGER, "
                + COLUMN_NB_DOCKS_AVAILABLE + " INTEGER, "
                + COLUMN_FAVORITE + " NUMERIC, " //BOOLEAN
                + COLUMN_IS_LOCKED + " NUMERIC, " //BOOLEAN
                + COLUMN_LAST_UPDATE + " NUMERIC" //DATETIME
                + ")";

        db.execSQL(sql);
        Log.d("DB", "database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists "+ TABLE_NAME);
        onCreate(db);
    }
    public Cursor getStations(){
        String sql = "select * from " + TABLE_NAME
                +" order by " + COLUMN_ID + " asc";
        Cursor c = getWritableDatabase().rawQuery(sql, null);
        return c;
    }

    public boolean isExist(long id) {
        String args[] = new String[] { Long.toString(id) };
        Cursor c = getWritableDatabase().rawQuery("select * from "+TABLE_NAME+" where "+COLUMN_ID+" = ?", args);

        return c.moveToFirst();
    }

    public boolean isFavorite(long id) {
        String args[] = new String[] { Long.toString(id) };
        Cursor c = getWritableDatabase().rawQuery("select * from "+TABLE_NAME+" where "+COLUMN_ID+" = ?", args);

        boolean isFavorite = false;

        if (c.moveToFirst())
            isFavorite = c.getInt(c.getColumnIndex(COLUMN_FAVORITE)) > 0;

        c.close();

        return isFavorite;
    }

    public void addRow(ContentValues cv) {
        long k = getWritableDatabase().insert(TABLE_NAME, null, cv);
        if (k<0){
            Log.e(TABLE_NAME,"insertion impossible..");
        }
    }

    public void updateRow(ContentValues cv, long id) {
        long k = getWritableDatabase().update(TABLE_NAME,cv,COLUMN_ID+" = '"+id + "'",null);
        if (k<0){
            Log.e(TABLE_NAME, "mise à jour impossible..");
        }
    }

    public Cursor getStation(long id) {
        String args[] = new String[] { Long.toString(id) };
        Cursor c = getWritableDatabase().rawQuery("select * from "+TABLE_NAME+" where "+COLUMN_ID+" = ?", args);

        return c;
    }

    public Cursor getFavoriteStations() {
        String args[] = new String[] { "1" };
        Cursor c = getWritableDatabase().rawQuery("select * from "+TABLE_NAME+" where "+COLUMN_FAVORITE+" = ?", args);

        return c;
    }
}

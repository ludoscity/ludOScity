package com.udem.ift2906.bixitracksexplorer.DBHelper;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryOptions;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiNetwork;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiStation;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;

import org.json.JSONException;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by F8Full on 2015-04-02.
 * This file is part of BixiTrackExplorer
 * Helper class providing static method to save and retrieve object in database
 */
@SuppressWarnings("unchecked") //(List<QueryRow>) allDocs.get("rows");
public class DBHelper {

    private static Manager mManager = null;
    private static final String mDbName = "appdb";
    private static Context context;

    private DBHelper() {}

    public static void init(Activity activity, Context c) throws IOException, CouchbaseLiteException {
        mManager = new Manager(new AndroidContext(activity), Manager.DEFAULT_OPTIONS);
        deleteDB(); //As an exercise so that data will be requested from the web
        context = c;
    }

    public static void deleteDB() throws CouchbaseLiteException {
        mManager.getDatabase(mDbName).delete();
    }

    /*public static Manager get() {
        return mManager;
    }*/

    public static void saveTrack(Track toSave) throws CouchbaseLiteException, JSONException {
        Document doc = mManager.getDatabase(mDbName).getDocument(toSave.getKeyTimeUTC());
        doc.putProperties(new Gson().<Map<String, Object>>fromJson(toSave.toString(), new TypeToken<HashMap<String, Object>>() {}.getType()));
    }

    public static List<QueryRow> getAllTracks() throws CouchbaseLiteException {
        Map<String, Object> allDocs;
        allDocs = mManager.getDatabase(mDbName).getAllDocs(new QueryOptions());

        return (List<QueryRow>) allDocs.get("rows");
    }

    public static BixiStation getStation(String id) {
        BixiStation station = new BixiStation();

        Cursor cursor = BixiStationDatabase.getInstance(context).getStation(id);

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            station = createStation(cursor);
        }

        return station;
    }

    private static BixiStation createStation(Cursor cursor) {
        BixiStation station = new BixiStation();

        station.id = cursor.getString(cursor.getColumnIndex(BixiStationDatabase.COLUMN_ID));
        station.name = cursor.getString(cursor.getColumnIndex(BixiStationDatabase.COLUMN_NAME));
        station.latitude = cursor.getDouble(cursor.getColumnIndex(BixiStationDatabase.COLUMN_LATITUDE));
        station.longitude = cursor.getDouble(cursor.getColumnIndex(BixiStationDatabase.COLUMN_LONGITUDE));
        station.free_bikes = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_NB_BIKES_AVAILABLE));
        station.empty_slots = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_NB_DOCKS_AVAILABLE));
        station.timestamp = cursor.getString(cursor.getColumnIndex(BixiStationDatabase.COLUMN_LAST_UPDATE));
        station.favorite = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_FAVORITE)) > 0;

        return station;
    }

    public static ArrayList<BixiStation> listStations(){
        ArrayList<BixiStation> stations = new ArrayList<>();
        Cursor cursor = BixiStationDatabase.getInstance(context).listStations();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            stations.add(createStation(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return stations;
    }

    public static boolean isExist(String id) {
        return BixiStationDatabase.getInstance(context).isExist(id);
    }



    public static boolean isFavorite(String id) {
        return BixiStationDatabase.getInstance(context).isFavorite(id);
    }

    public static void addRow(BixiStation station) {
        ContentValues cv = new ContentValues();

        cv.put(BixiStationDatabase.COLUMN_ID, station.id);
        cv.put(BixiStationDatabase.COLUMN_LATITUDE, station.latitude);
        cv.put(BixiStationDatabase.COLUMN_LONGITUDE, station.longitude);
        cv.put(BixiStationDatabase.COLUMN_NAME, station.name);
        cv.put(BixiStationDatabase.COLUMN_LAST_UPDATE, station.timestamp);
        cv.put(BixiStationDatabase.COLUMN_FAVORITE, false);

        BixiStationDatabase.getInstance(context).addRow(cv);
    }

    public static boolean updateRow(BixiStation station, String id) {
        ContentValues cv = new ContentValues();

        boolean isFavorite;

        if (isExist(id))
            isFavorite = isFavorite(id);
        else
            return false;

        cv.put(BixiStationDatabase.COLUMN_ID, id);
        cv.put(BixiStationDatabase.COLUMN_LATITUDE, station.latitude);
        cv.put(BixiStationDatabase.COLUMN_LONGITUDE, station.longitude);
        cv.put(BixiStationDatabase.COLUMN_NAME, station.name);
        cv.put(BixiStationDatabase.COLUMN_LAST_UPDATE, station.timestamp);
        cv.put(BixiStationDatabase.COLUMN_FAVORITE, isFavorite);

        BixiStationDatabase.getInstance(context).updateRow(cv, id);

        return true;
    }

    public static void addNetwork(BixiNetwork bixiNetwork) {
        for (BixiStation station : bixiNetwork.network.stations) {
            if (isExist(station.id))
                updateRow(station, station.id);
            else
                addRow(station);
        }
    }
}

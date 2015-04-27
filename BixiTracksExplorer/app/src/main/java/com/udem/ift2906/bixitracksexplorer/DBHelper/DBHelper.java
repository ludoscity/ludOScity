package com.udem.ift2906.bixitracksexplorer.DBHelper;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryOptions;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.udem.ift2906.bixitracksexplorer.StationItem;
import com.udem.ift2906.bixitracksexplorer.StationsNetwork;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;

import org.json.JSONException;

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
    private static boolean mGotTracks;

    private DBHelper() {}

    public static void init(Activity activity, Context c) throws IOException, CouchbaseLiteException {
        mManager = new Manager(new AndroidContext(activity), Manager.DEFAULT_OPTIONS);
        context = c;
        mGotTracks = !getAllTracks().isEmpty();
    }

    public static void deleteDB() throws CouchbaseLiteException {
        //If it crashes here because getDatabase returns null, uninstall and reinstall the app
        mManager.getDatabase(mDbName).delete();
    }

    public static boolean gotTracks() throws CouchbaseLiteException {
        return mGotTracks;
    }

    /*public static Manager get() {
        return mManager;
    }*/

    public static void saveTrack(Track toSave) throws CouchbaseLiteException, JSONException {
        Document doc = mManager.getDatabase(mDbName).getDocument(toSave.getKeyTimeUTC());
        doc.putProperties(new Gson().<Map<String, Object>>fromJson(toSave.toString(), new TypeToken<HashMap<String, Object>>() {
        }.getType()));
        mGotTracks = true; // mGotTracks = !getAllTracks().isEmpty(); in init()
    }

    public static List<QueryRow> getAllTracks() throws CouchbaseLiteException {
        Map<String, Object> allDocs;
        allDocs = mManager.getDatabase(mDbName).getAllDocs(new QueryOptions());

        return (List<QueryRow>) allDocs.get("rows");
    }

    //Not used because only potential client (so far) BudgetTrackDetails duplicates this data
    /*public static boolean isTrackPointDataCached(String trackID) throws CouchbaseLiteException {
        Document doc = mManager.getDatabase(mDbName).getExistingDocument(trackID);

        return doc.getProperties().containsKey("points");
    }*/

    //Used to add a new entry in corresponding Couchbase Document, only if not already present
    public static void putNewTrackPropertyAndSave(String _trackID, final String _newPropertyKey, final Object _newPropertyObject ) throws CouchbaseLiteException {
        Document doc = mManager.getDatabase(mDbName).getExistingDocument(_trackID);

        if (!doc.getProperties().containsKey(_newPropertyKey))
        {
            doc.update(new Document.DocumentUpdater() {
                @Override
                public boolean update(UnsavedRevision newRevision) {
                    newRevision.getProperties().put(_newPropertyKey, _newPropertyObject);
                    return true;
                }
            });
        }
    }


    /**
     * retrieveTrack
     * @return Map String&Object
     * @param trackID in form "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * Retrieves a track from Couchbase from a String complete id. Can't be of API model Track type
     * because processed data like cost is added to documents and wouldn't map to model fields.
     */
    public static Map<String,Object> retrieveTrack(String trackID) throws CouchbaseLiteException {
        Document doc = mManager.getDatabase(mDbName).getExistingDocument(trackID);

        if (doc != null){
            return doc.getCurrentRevision().getProperties();
        }

        return null;

        //This is a failed attempt at converting directly into a Track class
        //It is not usefull for this case right now but I just want to keep this piece of code around
        //https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/TypeAdapter.html
        /*Map<String, Object> convertedProperties = new HashMap<>();
        for(String key : docProperties.keySet())
        {
            if (key.equalsIgnoreCase("rating"))
            {
                Double rating = (Double) docProperties.get(key);
                int intRating = rating.intValue();
                convertedProperties.put(key, (Integer)intRating);
            }
            else
            {
                convertedProperties.put(key,docProperties.get(key));
            }

        }

        //convertedProperties.remove("rating");
        //convertedProperties.put("rating", 666);

        String JSONTruc = convertedProperties.toString();

        return new Gson().fromJson(convertedProperties.toString(), new TypeToken<Track>() {
        }.getType());
        END of failed attempts*/
    }

    private static StationItem createStation(Cursor cursor) {

        long uid = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndex(BixiStationDatabase.COLUMN_NAME));
        double latitude = cursor.getDouble(cursor.getColumnIndex(BixiStationDatabase.COLUMN_LATITUDE));
        double longitude = cursor.getDouble(cursor.getColumnIndex(BixiStationDatabase.COLUMN_LONGITUDE));
        int free_bikes = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_NB_BIKES_AVAILABLE));
        int empty_slots = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_NB_DOCKS_AVAILABLE));
        String timestamp = cursor.getString(cursor.getColumnIndex(BixiStationDatabase.COLUMN_LAST_UPDATE));
        boolean locked = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_IS_LOCKED)) > 0;
        boolean isFavorite = cursor.getInt(cursor.getColumnIndex(BixiStationDatabase.COLUMN_FAVORITE)) > 0;

        LatLng position = new LatLng(latitude,longitude);
        return new StationItem(uid,name,position,free_bikes,empty_slots,timestamp,locked,isFavorite);
    }

    public static StationsNetwork getStationsNetwork(){
        StationsNetwork stationsNetwork = new StationsNetwork();
        Cursor cursor = BixiStationDatabase.getInstance(context).getStations();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            stationsNetwork.stations.add(createStation(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return stationsNetwork;
    }

    public static ArrayList<StationItem> getFavoriteStations(){
        ArrayList<StationItem> items = new ArrayList<>();
        Cursor cursor = BixiStationDatabase.getInstance(context).getFavoriteStations();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            items.add(createStation(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return items;
    }

    /*public static StationItem getStationItem(long id) {
        Cursor cursor = BixiStationDatabase.getInstance(context).getStation(id);

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            return createStation(cursor);
        }

        return null;
    }*/

    public static boolean isExist(long id) {
        return BixiStationDatabase.getInstance(context).isExist(id);
    }

    public static boolean isFavorite(long id) {
        return BixiStationDatabase.getInstance(context).isFavorite(id);
    }

    public static void addRow(StationItem station) {
        ContentValues cv = new ContentValues();

        cv.put(BixiStationDatabase.COLUMN_ID, station.getUid());
        cv.put(BixiStationDatabase.COLUMN_LATITUDE, station.getPosition().latitude);
        cv.put(BixiStationDatabase.COLUMN_LONGITUDE, station.getPosition().longitude);
        cv.put(BixiStationDatabase.COLUMN_NAME, station.getName());
        cv.put(BixiStationDatabase.COLUMN_LAST_UPDATE, station.getTimestamp());
        cv.put(BixiStationDatabase.COLUMN_FAVORITE, 0);
        cv.put(BixiStationDatabase.COLUMN_IS_LOCKED, station.isLocked() ? 1 : 0);
        cv.put(BixiStationDatabase.COLUMN_NB_BIKES_AVAILABLE, station.getFree_bikes());
        cv.put(BixiStationDatabase.COLUMN_NB_DOCKS_AVAILABLE, station.getEmpty_slots());

        BixiStationDatabase.getInstance(context).addRow(cv);
    }

    public static boolean updateRow(StationItem station, long id) {
        ContentValues cv = new ContentValues();

        cv.put(BixiStationDatabase.COLUMN_ID, id);
        cv.put(BixiStationDatabase.COLUMN_LATITUDE, station.getPosition().latitude);
        cv.put(BixiStationDatabase.COLUMN_LONGITUDE, station.getPosition().longitude);
        cv.put(BixiStationDatabase.COLUMN_NAME, station.getName());
        cv.put(BixiStationDatabase.COLUMN_LAST_UPDATE, station.getTimestamp());
        cv.put(BixiStationDatabase.COLUMN_IS_LOCKED, station.isLocked() ? 1 : 0);
        cv.put(BixiStationDatabase.COLUMN_NB_BIKES_AVAILABLE, station.getFree_bikes());
        cv.put(BixiStationDatabase.COLUMN_NB_DOCKS_AVAILABLE, station.getEmpty_slots());

        BixiStationDatabase.getInstance(context).updateRow(cv, id);

        return true;
    }

    public static boolean updateFavorite(Boolean isFavorite, long id) {
        ContentValues cv = new ContentValues();

        cv.put(BixiStationDatabase.COLUMN_ID, id);
        cv.put(BixiStationDatabase.COLUMN_FAVORITE, isFavorite ? 1 : 0);

        BixiStationDatabase.getInstance(context).updateRow(cv, id);

        return true;
    }

    public static void addNetwork(StationsNetwork stationsNetwork) {
        for (StationItem station : stationsNetwork.stations) {
            if (isExist(station.getUid()))
                updateRow(station, station.getUid());
            else
                addRow(station);
        }
    }

    public static void closeDatabase() {
        BixiStationDatabase.getInstance(context).close();
    }
}

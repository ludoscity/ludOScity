package com.ludoscity.bikeactivityexplorer.DBHelper;

import android.app.Activity;
import android.content.Context;

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
import com.ludoscity.bikeactivityexplorer.StationItem;
import com.ludoscity.bikeactivityexplorer.StationsNetwork;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final String mTRACKS_DB_NAME = "tracksdb";
    private static final String mSTATIONS_DB_NAME = "stationsdb";
    private static Context context;
    private static boolean mGotTracks;

    private DBHelper() {}

    public static void init(Activity activity, Context c) throws IOException, CouchbaseLiteException {
        mManager = new Manager(new AndroidContext(activity), Manager.DEFAULT_OPTIONS);
        context = c;
        mGotTracks = !getAllTracks().isEmpty();
    }

    /*public static void deleteDB() throws CouchbaseLiteException {
        //If it crashes here because getDatabase returns null, uninstall and reinstall the app
        mManager.getDatabase(mTRACKS_DB_NAME).delete();
    }*/

    public static boolean gotTracks() throws CouchbaseLiteException {
        return mGotTracks;
    }

    /*public static Manager get() {
        return mManager;
    }*/

    public static void saveTrack(Track toSave) throws CouchbaseLiteException, JSONException {
        Document doc = mManager.getDatabase(mTRACKS_DB_NAME).getDocument(toSave.getKeyTimeUTC());
        doc.putProperties(new Gson().<Map<String, Object>>fromJson(toSave.toString(), new TypeToken<HashMap<String, Object>>() {
        }.getType()));
        mGotTracks = true; // mGotTracks = !getAllTracks().isEmpty(); in init()
    }

    public static void saveStation(final StationItem toSave) throws CouchbaseLiteException, JSONException {
        Document doc = mManager.getDatabase(mSTATIONS_DB_NAME).getDocument(String.valueOf(toSave.getUid()));

        doc.update(new Document.DocumentUpdater() {
            @Override
            public boolean update(UnsavedRevision newRevision) {
                Map<String, Object> properties = newRevision.getUserProperties();
                properties.put("uid", toSave.getUid());
                properties.put("name", toSave.getName());
                properties.put("locked", toSave.isLocked());
                properties.put("empty_slots", toSave.getEmpty_slots());
                properties.put("free_bikes", toSave.getFree_bikes());
                properties.put("latitude", toSave.getPosition().latitude);
                properties.put("longitude", toSave.getPosition().longitude);
                properties.put("isFavorite", toSave.isFavorite());
                properties.put("timestamp", toSave.getTimestamp());
                newRevision.setUserProperties(properties);
                return true;
            }
        });

        //doc.putProperties(new Gson().<Map<String, Object>>fromJson(new Gson().toJson(toSave), new TypeToken<HashMap<String, Object>>() {
        //}.getType()));
        //mGotTracks = true; // mGotTracks = !getAllTracks().isEmpty(); in init()
    }

    public static List<QueryRow> getAllTracks() throws CouchbaseLiteException {
        Map<String, Object> allDocs;
        allDocs = mManager.getDatabase(mTRACKS_DB_NAME).getAllDocs(new QueryOptions());

        return (List<QueryRow>) allDocs.get("rows");
    }

    private static List<QueryRow> getAllStations() throws CouchbaseLiteException{
        Map<String, Object> allDocs;
        allDocs = mManager.getDatabase(mSTATIONS_DB_NAME).getAllDocs(new QueryOptions());

        return (List<QueryRow>) allDocs.get("rows");
    }

    //Not used because only potential client (so far) BudgetTrackDetails duplicates this data
    /*public static boolean isTrackPointDataCached(String trackID) throws CouchbaseLiteException {
        Document doc = mManager.getDatabase(mTRACKS_DB_NAME).getExistingDocument(trackID);

        return doc.getProperties().containsKey("points");
    }*/

    //Used to add a new entry in corresponding Couchbase Document, only if not already present
    public static void putNewTrackPropertyAndSave(String _trackID, final String _newPropertyKey, final Object _newPropertyObject ) throws CouchbaseLiteException {
        Document doc = mManager.getDatabase(mTRACKS_DB_NAME).getExistingDocument(_trackID);

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
        Document doc = mManager.getDatabase(mTRACKS_DB_NAME).getExistingDocument(trackID);

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

    private static StationItem createStationItem(Document d){

        Map<String, Object> properties = d.getProperties();

        long uid = ((Number) properties.get("uid")).longValue();
        String name = (String)properties.get("name");
        double latitude = (Double) properties.get("latitude");
        double longitude = (Double) properties.get("longitude");
        int free_bikes = ((Number) properties.get("free_bikes")).intValue();
        int empty_slots = ((Number) properties.get("empty_slots")).intValue();
        String timestamp = (String) properties.get("timestamp");
        boolean locked = (Boolean) properties.get("locked");
        boolean isFavorite = (Boolean) properties.get("isFavorite");

        LatLng position = new LatLng(latitude,longitude);

        return new StationItem(uid,name,position,free_bikes,empty_slots,timestamp,locked,isFavorite);

    }

    public static StationsNetwork getStationsNetwork() throws CouchbaseLiteException {
        StationsNetwork stationsNetwork = new StationsNetwork();

        List<QueryRow> allStations = getAllStations();

        for (QueryRow qr : allStations)
        {
            Document d = qr.getDocument();

            stationsNetwork.stations.add(createStationItem(d));
        }

        return stationsNetwork;
    }

    public static ArrayList<StationItem> getFavoriteStations() throws CouchbaseLiteException {
        ArrayList<StationItem> items = new ArrayList<>();

        List<QueryRow> allStations = getAllStations();

        for (QueryRow qr : allStations) {
            Document d = qr.getDocument();

            Map<String, Object> properties = d.getProperties();

            if((Boolean) properties.get("isFavorite"))
            {
                items.add(createStationItem(d));
            }
        }

        return items;
    }

    public static boolean isFavorite(long id) throws CouchbaseLiteException {

        boolean toReturn = false;

        Document doc = mManager.getDatabase(mSTATIONS_DB_NAME).getExistingDocument(String.valueOf(id));

        if (doc != null)
        {
            Map<String, Object> properties = doc.getProperties();
            toReturn = (Boolean) properties.get("isFavorite");
        }

        return toReturn;
    }

    public static void updateFavorite(final Boolean isFavorite, long id) throws CouchbaseLiteException {

        Document doc = mManager.getDatabase(mSTATIONS_DB_NAME).getExistingDocument(String.valueOf(id));


        doc.update(new Document.DocumentUpdater() {
            @Override
            public boolean update(UnsavedRevision newRevision) {
                newRevision.getProperties().put("isFavorite", isFavorite);
                return true;
            }
        });

    }

    public static void addNetwork(StationsNetwork stationsNetwork) throws Exception {
        LinkedHashMap<Long, StationItem> map = new LinkedHashMap<>();

        for (StationItem station : stationsNetwork.stations) {
            if (map.containsKey(station.getUid())) {
                throw new Exception("Erreur de duplication dans la DB. Des stations pourraient être manquantent");
            }
            map.put(station.getUid(), station);
        }

        for(Map.Entry<Long, StationItem> entry : map.entrySet()) {

            saveStation(entry.getValue());
        }
    }
}

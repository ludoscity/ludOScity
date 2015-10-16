package com.ludoscity.findmybikes.helpersTMP;

import android.content.Context;
import android.content.SharedPreferences;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryOptions;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ludoscity.findmybikes.citybik_esAPITMP.model.NetworkDesc;
import com.ludoscity.findmybikes.StationItem;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by F8Full on 2015-04-02.
 * This file is part of BixiTrackExplorer
 * Helper class providing static method to save and retrieve data from storage
 * Internally, it uses both SharedPreferences and couchbase
 */
@SuppressWarnings("unchecked") //(List<QueryRow>) allDocs.get("rows");
public class DBHelper {

    private static Manager mManager = null;
    private static final String mTRACKS_DB_NAME = "tracksdb";
    private static final String mSTATIONS_DB_NAME = "stationsdb";
    private static boolean mGotTracks;

    public static final String SHARED_PREF_FILENAME = "FindMyBikes_prefs";

    private static final String PREF_CURRENT_BIKE_NETWORK_ID = "current_bike_network_id";
    private static final String PREF_NEARBY_AUTO_UPDATE = "setting.auto_update.nearby";

    private static final String PREF_SUFFIX_FAVORITES_SET = "_favorites";
    private static final String PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS = "_last_refresh_timestamp";
    private static final String PREF_SUFFIX_NETWORK_NAME = "_network_name";
    private static final String PREF_SUFFIX_NETWORK_HREF = "_network_href";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE = "_network_bounds_sw_lat";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE = "_network_bounds_sw_lng";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE = "_network_bounds_ne_lat";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE = "_network_bounds_ne_lng";


    private DBHelper() {}

    public static void init(Context context) throws IOException, CouchbaseLiteException {
        mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        mGotTracks = !getAllTracks().isEmpty();
    }

    public static boolean getAutoUpdate(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getBoolean(PREF_NEARBY_AUTO_UPDATE, true);
    }

    public static void setAutoUpdate(boolean toSet, Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        sp.edit().putBoolean(PREF_NEARBY_AUTO_UPDATE, toSet).apply();
    }

    public static long getLastUpdateTimestamp(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getLong(buildNetworkSpecificKey(PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS, ctx), 0);
    }

    public static void saveLastUpdateTimestampAsNow(Context ctx){

        ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putLong(buildNetworkSpecificKey(PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS, ctx),
                        Calendar.getInstance().getTimeInMillis()).apply();
    }

    public static boolean isBikeNetworkIdAvailable(Context ctx){

        return !getBikeNetworkId(ctx).equalsIgnoreCase("");
    }

    public static String getBikeNetworkName(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_NAME, ctx), "");
    }

    public static String getBikeNetworkHRef(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_HREF, ctx), "/v2/networks/bixi-montreal");
    }

    public static void saveBikeNetworkDesc(NetworkDesc networkDesc, Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor IdEditor = sp.edit();

        //Important to apply right away so that subsequent calls to buildNetworkSpecificKey work
        IdEditor.putString(PREF_CURRENT_BIKE_NETWORK_ID, networkDesc.id).apply();

        SharedPreferences.Editor editor = sp.edit();

        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_NAME, ctx), networkDesc.name);
        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_HREF, ctx), networkDesc.href);

        editor.apply();
    }

    public static void saveBikeNetworkBounds(LatLngBounds bounds, Context ctx){

        if (!bounds.equals(getBikeNetworkBounds(ctx))){

            SharedPreferences.Editor editor = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit();

            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE, ctx),
                    Double.doubleToLongBits(bounds.southwest.latitude));
            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE, ctx),
                    Double.doubleToLongBits(bounds.southwest.longitude));
            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE, ctx),
                    Double.doubleToLongBits(bounds.northeast.latitude));
            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE, ctx),
                    Double.doubleToLongBits(bounds.northeast.longitude));

            editor.apply();
        }
    }

    public static LatLngBounds getBikeNetworkBounds(Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        LatLng southwest = new LatLng(
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE, ctx), 0)),
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE, ctx), 0))
        );

        LatLng northeast = new LatLng(
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE, ctx), 0)),
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE, ctx), 0))
        );

        return new LatLngBounds(southwest, northeast);
    }

    private static String buildNetworkSpecificKey(String suffix, Context ctx){
        return getBikeNetworkId(ctx) + suffix;
    }

    public static String getBikeNetworkId(Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getString(PREF_CURRENT_BIKE_NETWORK_ID, "");
    }

    //Helper function for ugly fix regarding accented characters
    public static boolean isBixiNetwork(Context ctx){
        return getBikeNetworkId(ctx).equalsIgnoreCase("bixi-montreal");
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
        Document doc = mManager.getDatabase(mSTATIONS_DB_NAME).getDocument(toSave.getId());

        doc.update(new Document.DocumentUpdater() {
            @Override
            public boolean update(UnsavedRevision newRevision) {
                Map<String, Object> properties = newRevision.getUserProperties();
                properties.put("id", toSave.getId());
                properties.put("name", toSave.getName());
                properties.put("locked", toSave.isLocked());
                properties.put("empty_slots", toSave.getEmpty_slots());
                properties.put("free_bikes", toSave.getFree_bikes());
                properties.put("latitude", toSave.getPosition().latitude);
                properties.put("longitude", toSave.getPosition().longitude);
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

    public static void deleteAllStations() throws CouchbaseLiteException{
        mManager.getDatabase(mSTATIONS_DB_NAME).delete();
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

        String id = (String) properties.get("id");
        String name = (String)properties.get("name");
        double latitude = (Double) properties.get("latitude");
        double longitude = (Double) properties.get("longitude");
        int free_bikes = ((Number) properties.get("free_bikes")).intValue();
        int empty_slots = ((Number) properties.get("empty_slots")).intValue();
        String timestamp = (String) properties.get("timestamp");
        boolean locked = (Boolean) properties.get("locked");

        LatLng position = new LatLng(latitude,longitude);

        return new StationItem(id,name,position,free_bikes,empty_slots,timestamp,locked);

    }

    public static ArrayList<StationItem> getStationsNetwork() throws CouchbaseLiteException {
        ArrayList<StationItem> stationsNetwork = new ArrayList<>();

        List<QueryRow> allStations = getAllStations();

        for (QueryRow qr : allStations)
        {
            Document d = qr.getDocument();

            stationsNetwork.add(createStationItem(d));
        }

        return stationsNetwork;
    }

    public static ArrayList<StationItem> getFavoriteStations(Context ctx) throws CouchbaseLiteException {
        ArrayList<StationItem> items = new ArrayList<>();

        List<QueryRow> allStations = getAllStations();

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        Set<String> favorites = sp.getStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), new HashSet<String>());

        for (QueryRow qr : allStations) {
            Document d = qr.getDocument();

            Map<String, Object> properties = d.getProperties();

            //noinspection SuspiciousMethodCalls
            if (favorites.contains(properties.get("id")))
            {
                items.add(createStationItem(d));
            }
        }

        return items;
    }

    public static boolean isFavorite(String id, Context ctx) {

        boolean toReturn = false;

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        Set<String> favorites = sp.getStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), new HashSet<String>());

        if (favorites.contains(id))
            toReturn = true;


        return toReturn;
    }

    public static void updateFavorite(final Boolean isFavorite, String id, Context ctx) {

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        Set<String> oldFavorites = sp.getStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), new HashSet<String>());

        /*http://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String, java.util.Set)

        Note that you must not modify the set instance returned by this call.
        The consistency of the stored data is not guaranteed if you do, nor is your ability to modify the instance at all.*/

        Set<String> newFavorites = new HashSet<>(oldFavorites);

        if (isFavorite)
            newFavorites.add(id);
        else
            newFavorites.remove(id);

        sp.edit().putStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), newFavorites).apply();

        /*Document doc = mManager.getDatabase(mSTATIONS_DB_NAME).getExistingDocument(id);


        doc.update(new Document.DocumentUpdater() {
            @Override
            public boolean update(UnsavedRevision newRevision) {
                newRevision.getProperties().put("isFavorite", isFavorite);
                return true;
            }
        });*/

    }
}

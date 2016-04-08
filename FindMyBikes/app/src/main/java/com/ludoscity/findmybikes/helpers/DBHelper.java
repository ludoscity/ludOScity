package com.ludoscity.findmybikes.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

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
import com.ludoscity.findmybikes.FavoriteItem;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.citybik_es.model.NetworkDesc;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static final String SHARED_PREF_VERSION_CODE = "FindMyBikes_prefs_version_code";

    private static final String PREF_CURRENT_BIKE_NETWORK_ID = "current_bike_network_id";

    private static final String PREF_SUFFIX_FAVORITES_JSONARRAY = "_favorites";
    private static final String PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS = "_last_refresh_timestamp";
    private static final String PREF_SUFFIX_NETWORK_NAME = "_network_name";
    private static final String PREF_SUFFIX_NETWORK_HREF = "_network_href";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE = "_network_bounds_sw_lat";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE = "_network_bounds_sw_lng";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE = "_network_bounds_ne_lat";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE = "_network_bounds_ne_lng";


    private DBHelper() {}

    public static void init(Context context) throws IOException, CouchbaseLiteException, PackageManager.NameNotFoundException {
        mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        mGotTracks = !getAllTracks().isEmpty();

        //Check for SharedPreferences versioning
        int sharedPrefVersion = context.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).getInt(SHARED_PREF_VERSION_CODE, 0);
        PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        int currentVersionCode = pinfo.versionCode;

        if (sharedPrefVersion != currentVersionCode){
            if (sharedPrefVersion == 0 && currentVersionCode >= 8){
                SharedPreferences settings;
                SharedPreferences.Editor editor;

                settings = context.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
                editor = settings.edit();

                editor.clear();
                editor.commit(); //I do want commit and not apply

                editor.putInt(SHARED_PREF_VERSION_CODE, currentVersionCode);
                editor.apply();
            }
        }
    }

    public static boolean getAutoUpdate(Context ctx){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        return sp.getBoolean(ctx.getString(R.string.pref_refresh_options_key), true);
    }

    public static boolean getWalkingProximityAsDistance(Context _ctx){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(_ctx);

        return sp.getBoolean(_ctx.getString(R.string.pref_walking_proximity_key), false);
    }

    public static boolean getBikingProximityAsDistance(Context _ctx){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(_ctx);

        return sp.getBoolean(_ctx.getString(R.string.pref_biking_proximity_key), false);
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

        if (!bounds.equals(getBikeNetworkBounds(ctx, 0))){

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

    public static LatLngBounds getBikeNetworkBounds(Context _ctx, double _paddingKms){

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        LatLng southwestRaw = new LatLng(
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE, _ctx), 0)),
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE, _ctx), 0))
        );

        LatLng northeastRaw = new LatLng(
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE, _ctx), 0)),
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE, _ctx), 0))
        );

        //http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
        //http://stackoverflow.com/questions/29478463/offset-latlng-by-some-amount-of-meters-in-android
        //Latitude : easy, 1 degree = 111111m (historically because of the French :D)
        //Longitude : 1 degree = 111111 * cos (latitude)m
        LatLng southwestPadded = new LatLng(southwestRaw.latitude - (_paddingKms*1000.d) / 111111.d,
                southwestRaw.longitude - (_paddingKms*1000.d) / 111111.d * Math.cos(southwestRaw.latitude)  );
        LatLng northeastPadded = new LatLng(northeastRaw.latitude + (_paddingKms*1000.d) / 111111.d,
                northeastRaw.longitude + (_paddingKms*1000.d) / 111111.d * Math.cos(northeastRaw.latitude)  );

        return new LatLngBounds(southwestPadded, northeastPadded);
    }

    private static String buildNetworkSpecificKey(String suffix, Context ctx){
        return getBikeNetworkId(ctx) + suffix;
    }

    public static String getBikeNetworkId(Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getString(PREF_CURRENT_BIKE_NETWORK_ID, "");
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

    //TODO: Add validation of IDs to handle the case were a favorite station been removed
    //Replace edit fab with red delete one
    public static ArrayList<FavoriteItem> getFavoriteItems(Context _ctx){
        ArrayList<FavoriteItem> toReturn = new ArrayList<>();

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        try {
            JSONArray favoritesJSONArray = new JSONArray(sp.getString(
                    buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, _ctx), "[]" ));

            for (int i=0; i<favoritesJSONArray.length(); i+=2){
                toReturn.add(new FavoriteItem(favoritesJSONArray.getString(i), favoritesJSONArray.getString(i+1)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return toReturn;
    }

    public static boolean isFavorite(String id, Context ctx) {

        boolean toReturn = false;

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        try {
            JSONArray favoritesJSONArray = new JSONArray(sp.getString(
                    buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, ctx), "[]" ));

            for (int i=0; i<favoritesJSONArray.length(); ++i){
                if (favoritesJSONArray.getString(i).equalsIgnoreCase(id)){
                    toReturn = true;
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return toReturn;
    }

    public static void updateFavorite(final Boolean isFavorite, String id, String displayName, Context ctx) {

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        //JSONArray favoriteJSONArray;
        //contains id followed by favorite display name
        try {
            JSONArray oldFavoriteJSONArray = new JSONArray(sp.getString(
                    buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, ctx), "[]" ));
            JSONArray newFavoriteJSONArray;

            int existingIndex = -1;

            for (int i=0; i<oldFavoriteJSONArray.length(); i+=2){
                if (oldFavoriteJSONArray.getString(i).equalsIgnoreCase(id)){
                    existingIndex = i;
                    break;
                }
            }

            if (isFavorite){
                if (existingIndex == -1) {
                    oldFavoriteJSONArray.put(id);
                    oldFavoriteJSONArray.put(displayName);
                }
                else{

                    oldFavoriteJSONArray.put(existingIndex + 1, displayName);
                }

                newFavoriteJSONArray = oldFavoriteJSONArray;
            }
            else{
                //Requires API 19
                //oldFavoriteJSONArray.remove(existingIndex);
                newFavoriteJSONArray = new JSONArray();
                for (int i=0; i<oldFavoriteJSONArray.length(); ++i){
                    if (i != existingIndex && i != existingIndex+1)
                        newFavoriteJSONArray.put(oldFavoriteJSONArray.getString(i));
                }
            }

            sp.edit().putString(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, ctx), newFavoriteJSONArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }



        //Set<String> oldFavorites = sp.getStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), new HashSet<String>());

        /*http://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String, java.util.Set)

        Note that you must not modify the set instance returned by this call.
        The consistency of the stored data is not guaranteed if you do, nor is your ability to modify the instance at all.*/

        //Set<String> newFavorites = new HashSet<>(oldFavorites);

        //if (isFavorite)
        //    newFavorites.add(id);
        //else
        //    newFavorites.remove(id);

        //sp.edit().putStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), newFavorites).apply();

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

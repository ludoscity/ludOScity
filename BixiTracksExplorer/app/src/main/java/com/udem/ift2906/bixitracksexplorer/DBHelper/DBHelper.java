package com.udem.ift2906.bixitracksexplorer.DBHelper;

import android.app.Activity;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryOptions;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;

import org.json.JSONException;

import java.io.IOException;
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

    private DBHelper() {}

    public static void init(Activity activity) throws IOException, CouchbaseLiteException {
        mManager = new Manager(new AndroidContext(activity), Manager.DEFAULT_OPTIONS);
        deleteDB(); //As an exercise so that data will be requested from the web
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


}

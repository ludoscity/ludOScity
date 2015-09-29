package com.ludoscity.bikeactivityexplorer;

import android.app.Application;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.ludoscity.bikeactivityexplorer.DBHelper.DBHelper;

import java.io.IOException;

/**
 * Created by F8Full on 2015-09-28.
 * This class is used to maintain global states and also safely initialize static singletons
 * See http://stackoverflow.com/questions/3826905/singletons-vs-application-context-in-android
 */
public class RootApplication extends Application {

    private static final String TAG = "RootApplication";


    @Override
    public void onCreate() {
        super.onCreate();

        try {
            DBHelper.init(this);
        } catch (IOException | CouchbaseLiteException e) {
            Log.d(TAG, "Error initializing database", e);
        }



        //BixiTracksExplorerAPIHelper.init();


        //googleMapsDirectionsApi = buildGMapsDirectionsApi();
        //accessToken = PreferencesUtils.retrieveAccessToken(this);
    }
}

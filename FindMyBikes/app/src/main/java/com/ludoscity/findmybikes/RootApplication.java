package com.ludoscity.findmybikes;

import android.app.Application;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.ludoscity.findmybikes.citybik_esAPITMP.Citybik_esAPI;
import com.ludoscity.findmybikes.helpersTMP.DBHelper;

import java.io.IOException;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by F8Full on 2015-09-28.
 * This class is used to maintain global states and also safely initialize static singletons
 * See http://stackoverflow.com/questions/3826905/singletons-vs-application-context-in-android
 */
public class RootApplication extends Application {

    private static final String TAG = "RootApplication";

    static final String ENDPOINT = "http://api.citybik.es";

    Citybik_esAPI mCitybik_esAPI;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            DBHelper.init(this);
        } catch (IOException | CouchbaseLiteException e) {
            Log.d(TAG, "Error initializing database", e);
        }

        mCitybik_esAPI = buildCitybik_esAPI();
    }

    private Citybik_esAPI buildCitybik_esAPI() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(Citybik_esAPI.class);
    }

    public Citybik_esAPI getCitybik_esApi() {
        return mCitybik_esAPI;
    }
}

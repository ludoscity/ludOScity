package com.ludoscity.findmybikes;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.ludoscity.findmybikes.citybik_es.Citybik_esAPI;
import com.ludoscity.findmybikes.citybik_es.model.Station;
import com.ludoscity.findmybikes.helpers.DBHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by F8Full on 2015-09-28.
 * This class is used to maintain global states and also safely initialize static singletons
 * See http://stackoverflow.com/questions/3826905/singletons-vs-application-context-in-android
 */
public class RootApplication extends Application {

    private static final String TAG = "RootApplication";

    static final String ENDPOINT = "http://api.citybik.es";

    Citybik_esAPI mCitybik_esAPI;
    Twitter mTwitterAPI;
    //Station list data is kept here to survive screen orientation change
    //It's built from the database on launch (if there's a complete record available) and updated in memory after data download
    private static ArrayList<StationItem> mBikeshareStationList;
    //TODO: refactor with MVC in mind. This is model

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            DBHelper.init(this);
        } catch (IOException | CouchbaseLiteException | PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Error initializing database", e);
        }

        if(!DBHelper.wasLastSavePartial(this)){

            try {
                mBikeshareStationList = DBHelper.getStationsNetwork();
            } catch (CouchbaseLiteException | IllegalStateException e) {
                Log.d("RootApplication", "Couldn't retrieve Station Network from db",e );
                mBikeshareStationList = new ArrayList<>();
            }

            Log.i("RootApplication", mBikeshareStationList.size() + " stations loaded from DB");
        }
        else
            mBikeshareStationList = new ArrayList<>();


        mCitybik_esAPI = buildCitybik_esAPI();
        mTwitterAPI = buildTwitterAPI();
    }

    private Citybik_esAPI buildCitybik_esAPI() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(Citybik_esAPI.class);
    }

    //They are packaged indeed, but at least they don't show up on github ^^
    private  Twitter buildTwitterAPI(){
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(getResources().getString(R.string.twitter_consumer_key))
                .setOAuthConsumerSecret(getResources().getString(R.string.twitter_consumer_secret))
                .setOAuthAccessToken(getResources().getString(R.string.twitter_access_token))
                .setOAuthAccessTokenSecret(getResources().getString(R.string.twitter_access_token_secret));
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    public Citybik_esAPI getCitybik_esApi() {
        return mCitybik_esAPI;
    }

    public Twitter getTwitterApi(){
        return mTwitterAPI;
    }

    public static ArrayList<StationItem> getBikeNetworkStationList(){
        return mBikeshareStationList;
    }

    public static ArrayList<StationItem> addAllToBikeNetworkStationList(List<Station> _stationList, Context _ctx){

        ArrayList<StationItem> newList = new ArrayList<>(mBikeshareStationList.size());

        for (Station station : _stationList) {

            if (station.empty_slots == null)
                station.empty_slots = -1;
            //Some systems have empty_slots to null (like nextbike SZ-bike in Dresden, Germany)
            //-1 is used to encode this case

            StationItem stationItem = new StationItem(station);
            newList.add(stationItem);
        }

        mBikeshareStationList = newList;

        return mBikeshareStationList;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}

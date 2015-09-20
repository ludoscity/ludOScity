package com.ludoscity.bikeactivityexplorer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.model.CameraPosition;
import com.ludoscity.bikeactivityexplorer.BixiAPI.BixiAPI;
import com.ludoscity.bikeactivityexplorer.DBHelper.DBHelper;
import com.ludoscity.bikeactivityexplorer.Utils.Utils;

import java.io.IOException;
import java.util.Calendar;

/**
 * Created by F8Full on 2015-07-26.
 * Activity used to display the nearby section
 */
public class NearbyActivity extends BaseActivity
        implements NearbyFragment.OnFragmentInteractionListener,
        StationMapFragment.OnStationMapFragmentInteractionListener{

    public static Resources resources;

    private NearbyFragment mNearbyFragment = null;

    private StationMapFragment mStationMapFragment = null;


    private static final String PREF_WEBTASK_LAST_TIMESTAMP_MS = "last_refresh_timestamp";

    private Handler mUpdateRefreshHandler = null;
    private Runnable mUpdateRefreshRunnableCode = null;

    private DownloadWebTask mDownloadWebTask = null;
    //TODO : investigate having an Application class providing APIs like as done there
    //https://github.com/f8full/DirectionsOnMapv2WithRetrofit/blob/master/app/src/main/java/com/f8full/sample/directionsonmapv2withretrofit/RootApplication.java
    private BixiAPI mBixiApiInstance;

    private StationsNetwork mStationsNetwork;




    private TextView mUpdateTextView;
    private ProgressBar mUpdateProgressBar;
    private ImageView mRefreshButton;
    private View mDownloadBar;

    private boolean mIsAlreadyZoomedToUser;
    private boolean mRefreshMarkers = true;





    private CameraPosition mBackCameraPosition;

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_NEARBY;
    }

    @Override
    public void onNearbyFragmentInteraction(String title, boolean isDrawerIndicatorEnabled) {
        setActivityTitle(title);

        mDrawerToggle.setDrawerIndicatorEnabled(isDrawerIndicatorEnabled);
        restoreActionBar();

    }

    @Override
    public void onStart(){
        super.onStart();



        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (Utils.Connectivity.isConnected(getApplicationContext()) && sp.getLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, 0) == 0) { //Means ask never successfully completed
            //Because webtask launches DB task, we know that a value there means actual data in the DB
            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute(getApplicationContext());
        }
        else{   //Having a timestamp means some data exists in the db, as both task are intimately linked
            try {
                mStationsNetwork = DBHelper.getStationsNetwork();
            } catch (CouchbaseLiteException e) {
                Log.d("nearbyActivity", "Exception ! :(",e );
            }
            Log.d("nearbyActivity", mStationsNetwork.stations.size() + " stations loaded from DB");


        }
    }

    @Override
    public void onResume(){

        super.onResume();

        mRefreshMarkers = true;



        mUpdateRefreshHandler = new Handler();

        setupUI();


    }

    @Override
    public void onPause() {

        cancelDownloadWebTask();
        stopUIRefresh();

        super.onPause();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resources = getResources();

        //Read app params and apply them
        /*if (getResources().getBoolean(R.bool.allow_portrait)) {
            if (!getResources().getBoolean(R.bool.allow_landscape)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            if (getResources().getBoolean(R.bool.allow_landscape)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }*/

        //Initialize couchbase database
        //TODO: now we have multiple activities, this should not be done here
        try {
            DBHelper.init(this, this);
            BixiTracksExplorerAPIHelper.init();
        } catch (IOException | CouchbaseLiteException e) {
            e.printStackTrace();
        }


        setContentView(R.layout.activity_nearby);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        setActivityTitle(getTitle());
        setActivitySubtitle("");

        // Update Bar
        mUpdateTextView = (TextView) findViewById(com.ludoscity.bikeactivityexplorer.R.id.update_textView);
        mUpdateTextView.setTextColor(Color.LTGRAY);
        mUpdateProgressBar = (ProgressBar) findViewById(com.ludoscity.bikeactivityexplorer.R.id.refreshDatabase_progressbar);
        mUpdateProgressBar.setVisibility(View.INVISIBLE);
        mRefreshButton = (ImageView) findViewById(com.ludoscity.bikeactivityexplorer.R.id.refreshDatabase_button);
        mDownloadBar = findViewById(com.ludoscity.bikeactivityexplorer.R.id.downloadBar);
        setRefreshButtonListener();

        //COUCHBASE INIT
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setActivityTitle(getString(R.string.title_section_nearby));

        mNearbyFragment = (NearbyFragment)getSupportFragmentManager().findFragmentById(
                R.id.nearby_fragment);
        mNearbyFragment.setHasOptionsMenu(true);

        mStationMapFragment = (StationMapFragment)getSupportFragmentManager().findFragmentById(
                R.id.station_map_fragment);


        //if (mNearbyFragment != null && savedInstanceState == null) {
        //    Bundle args = intentToFragmentArguments(getIntent());
        //    mNearbyFragment.reloadFromArguments(args);
        //}
    }

    private void stopUIRefresh() {
        if (mUpdateRefreshHandler != null) {
            mUpdateRefreshHandler.removeCallbacks(mUpdateRefreshRunnableCode);
            mUpdateRefreshRunnableCode = null;
            mUpdateRefreshHandler = null;
        }
    }

    //Safe to call from multiple point in code, refreshing the UI elements with the most recent data available
    //Takes care of map readyness check
    //Safely updates everything based on checking the last update timestamp
    private void setupUI(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        long lastRefreshTimestamp = sp.getLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, 0);
        if (lastRefreshTimestamp != 0){

            if (mUpdateRefreshRunnableCode == null) {
                //BEWARE THE INLINE NON TRIVIAL CLASS DECLARATION !!
                mUpdateRefreshRunnableCode = new Runnable() {

                    /*private final long startTime = System.currentTimeMillis();
                    private long lastRunTime;
                    private long lastUpdateTime = System.currentTimeMillis();   //Update should be run automatically ?
                    */
                    @Override
                    public void run() {

                        long now = System.currentTimeMillis();

                        //Update not already in progress
                        if (mDownloadWebTask == null) {

                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            long runnableLastRefreshTimestamp = sp.getLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, 0);

                            long difference = now - runnableLastRefreshTimestamp;

                            StringBuilder updateTextBuilder = new StringBuilder();

                            //First taking care of past time...
                            if (difference < DateUtils.MINUTE_IN_MILLIS)
                                updateTextBuilder.append(getString(com.ludoscity.bikeactivityexplorer.R.string.momentsAgo)).append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.fromCitibik_es));//mUpdateTextView.setText();
                            else
                                updateTextBuilder.append(Long.toString(difference / DateUtils.MINUTE_IN_MILLIS)).append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.minsAgo)).append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.fromCitibik_es));
                            //mUpdateTextView.setText(Long.toString(difference / DateUtils.MINUTE_IN_MILLIS) +" "+ getString(R.string.minsAgo) + " " + getString(R.string.fromCitibik_es) );

                            //long differenceInMinutes = difference / DateUtils.MINUTE_IN_MILLIS;

                            //from : http://stackoverflow.com/questions/25355611/how-to-get-time-difference-between-two-dates-in-android-app
                            //long differenceInSeconds = difference / DateUtils.SECOND_IN_MILLIS;
// formatted will be HH:MM:SS or MM:SS
                            //String formatted = DateUtils.formatElapsedTime(differenceInSeconds);

                            //... then about next update
                            if (Utils.Connectivity.isConnected(getApplicationContext())) {

                                boolean autoUpdate = sp.getBoolean(UserSettingsFragment.PREF_NEARBY_AUTO_UPDATE, true);

                                if (!autoUpdate){
                                    updateTextBuilder.append(" - ").append(getString(R.string.nearbyfragment_no_auto_update));

                                    mRefreshButton.setVisibility(View.VISIBLE);
                                }
                                else {

                                    //Should come from something keeping tabs on time, maybe this runnable itself
                                    long wishedUpdateTime = runnableLastRefreshTimestamp + 5 * 1000 * 60;  //comes from Prefs

                                    if (now >= wishedUpdateTime) {

                                        //Put a string same length as the other one ?
                                        updateTextBuilder.append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.updating));

                                        //Run update

                                        mDownloadWebTask = new DownloadWebTask();
                                        mDownloadWebTask.execute(getApplicationContext());


                                        //lastUpdateTime = now;
                                    } else {

                                        updateTextBuilder.append(" - ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.nextUpdate)).append(" ");


                                        long differenceSecond = (wishedUpdateTime - now) / DateUtils.SECOND_IN_MILLIS;

                                        // formatted will be HH:MM:SS or MM:SS
                                        updateTextBuilder.append(DateUtils.formatElapsedTime(differenceSecond));

                                        mRefreshButton.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                            else{
                                updateTextBuilder.append( " ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.no_connectivity));
                                mRefreshButton.setVisibility(View.GONE);
                            }

                            mUpdateTextView.setText(updateTextBuilder.toString());
                        }

                        //lastRunTime = now;

                        //Update UI will be refreshed every second
                        mUpdateRefreshHandler.postDelayed(mUpdateRefreshRunnableCode, 1000);
                    }
                };

                mUpdateRefreshHandler.post(mUpdateRefreshRunnableCode);
            }


            if(mStationMapFragment.isMapReady()) {
                if (mStationsNetwork != null && mRefreshMarkers) {

                    //Gfx data not available yet
                    //if(mMapMarkersGfxData.isEmpty())
                    if(mStationMapFragment.isMarkerListEmpty())
                    {
                        //SETUP MARKERS DATA
                        for (StationItem item : mStationsNetwork.stations){
                            mStationMapFragment.addMarkerForStationItem(item);
                        }
                    }

                    mStationMapFragment.redrawMarkers();

                    //mStationMapFragment.clearMap();


                    mRefreshMarkers = false;
                }

                /*int listPosition = mStationListView.getFirstVisiblePosition();
                int itemSelected = -1;
                if (mStationListViewAdapter != null)
                    itemSelected = mStationListViewAdapter.getCurrentItemSelected();
                if (mStationsNetwork != null) {
                    mStationListViewAdapter = new StationListViewAdapter(getApplicationContext(), mStationsNetwork, mCurrentUserLatLng, true);//mParkingSwitch.isChecked());
                    mStationListViewAdapter.setItemSelected(itemSelected);
                    mStationListView.setAdapter(mStationListViewAdapter);
                    mStationListView.setSelectionFromTop(listPosition, 0);
                }*/
            }

        } else{
            mUpdateTextView.setText(getString(com.ludoscity.bikeactivityexplorer.R.string.nearbyfragment_default_never_web_updated));
        }
    }

    private void setRefreshButtonListener() {
        mDownloadBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.Connectivity.isConnected(getApplicationContext()) && mDownloadWebTask == null) {
                    mDownloadWebTask = new DownloadWebTask();
                    mDownloadWebTask.execute(getApplicationContext());
                }
            }
        });
    }



    @Override
    public void onStationMapFragmentInteraction(Uri uri) {
        //Will be warned of station details click, will make info fragment to replace list fragment

        if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_READY_PATH))
        {
            setupUI();
        }

        //IF SETUP UI SETUP UI


        /*@Override
    public void onInfoWindowClick(Marker marker) {
        if(!isStationInfoVisible) {
            for (StationItem station : mStationsNetwork.stations) {
                if (station.getPosition().equals(marker.getPosition())) {
                    replaceListViewByInfoView(station, false);
                    return;
                }
            }
        }
    }*/


        /*@Override
    public boolean onMarkerClick(Marker marker) {
        int i = mStationListViewAdapter.getPositionInList(marker);
        mStationListViewAdapter.setItemSelected(i);
        Log.d("onMarkerClick", "Scroll view to " + i);
        if (i != -1) {
            mStationListView.smoothScrollToPositionFromTop(i, 0, 300);
        }

    }*/

        /*@Override
    public void onMyLocationChange(Location location) {
        if(location != null) {
            Log.d("onMyLocationChange", "new location " + location.toString());
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mStationListViewAdapter != null)
                mStationListViewAdapter.setCurrentUserLatLng(mCurrentUserLatLng);

            if (mCurrentInfoStation != null){
                mStationInfoDistanceView.setText(String.valueOf(mCurrentInfoStation.getDistanceStringFromLatLng(mCurrentUserLatLng)));
            }
        }
    }*/

    }

    private void cancelDownloadWebTask() {
        if (mDownloadWebTask != null && !mDownloadWebTask.isCancelled())
        {
            mDownloadWebTask.cancel(false);
            mDownloadWebTask = null;
        }
    }

    public class DownloadWebTask extends AsyncTask<Context, Void, Void> {
        @Override
        protected Void doInBackground(Context... params) {
            mBixiApiInstance = new BixiAPI(params[0]);
            //A Task that launches an other task, ok I want it to show the progress in the user interface
            //I finally advised against changing anything, instead I'll add a setting to display Database toast, and OFF by default
            //I do that because it seems it's not blocking / crashing if we try to navigate the interface anyway
            //Let the user choose when to update.
            mStationsNetwork = mBixiApiInstance.downloadBixiNetwork();
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mUpdateTextView.setText(getString(com.ludoscity.bikeactivityexplorer.R.string.updating));
            mUpdateProgressBar.setVisibility(View.VISIBLE);
            mRefreshButton.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onCancelled (Void aVoid){
            super.onCancelled(aVoid);
            //Set interface back -- Not even nescessary right now as fragment is completely
            //scrapped each time. Might be usefull in the future.
            mUpdateProgressBar.setVisibility(View.INVISIBLE);
            mRefreshButton.setVisibility(View.VISIBLE);

            //SETUP MARKERS DATA
            //TODO Seen null callstack on weird network conditions
            //08-04 21:52:01.693    2108-2108/? E/AndroidRuntime? FATAL EXCEPTION: main
            //Process: com.ludoscity.bikeactivityexplorer, PID: 2108
            //java.lang.NullPointerException: Attempt to read from field 'java.util.ArrayList com.ludoscity.bikeactivityexplorer.StationsNetwork.stations' on a null object reference
            //at com.ludoscity.bikeactivityexplorer.NearbyFragment$DownloadWebTask.onCancelled(NearbyFragment.java:730)


            //This was done cause task cancelattion replaces onPostExecute call by this onCancelled one
            //it was a way of profitting things were done anyway
            //for (StationItem item : mStationsNetwork.stations){
            //    mMapMarkersGfxData.add(new StationMapGfx(item));
            //}
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //switch progressbar view visibility
            mUpdateProgressBar.setVisibility(View.INVISIBLE);
            mRefreshButton.setVisibility(View.VISIBLE);

            //Removed this Toast as progressBar AND updated textView with time in minutes already convey the idea
            //Maybe have a toat if it was NOT a success
            //Toast.makeText(mContext, R.string.download_success, Toast.LENGTH_SHORT).show();

            //DO SET HERE
            /*SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);*/
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            sp.edit().putLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, Calendar.getInstance().getTimeInMillis()).apply();
            mRefreshMarkers = true;
            setupUI();
            Log.d("nearbyFragment", mStationsNetwork.stations.size() + " stations downloaded from citibik.es");

            //must be done last
            mDownloadWebTask = null;
        }
    }
}

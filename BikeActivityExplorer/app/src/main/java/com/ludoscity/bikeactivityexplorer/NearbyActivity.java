package com.ludoscity.bikeactivityexplorer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
        implements StationMapFragment.OnStationMapFragmentInteractionListener,
        StationListFragment.OnStationListFragmentInteractionListener,
        StationInfoFragment.OnStationInfoFragmentInteractionListener{

    private StationMapFragment mStationMapFragment = null;

    private StationListFragment mStationListFragment = null;

    private StationInfoFragment mStationInfoFragment = null;


    private static final String PREF_WEBTASK_LAST_TIMESTAMP_MS = "last_refresh_timestamp";

    private Handler mUpdateRefreshHandler = null;
    private Runnable mUpdateRefreshRunnableCode = null;

    private DownloadWebTask mDownloadWebTask = null;

    private StationsNetwork mStationsNetwork;

    private LatLng mCurrentUserLatLng = null;


    private TextView mUpdateTextView;
    private ProgressBar mUpdateProgressBar;
    private ImageView mRefreshButton;
    private View mDownloadBar;

    private boolean mRefreshMarkers = true;

    private MenuItem mParkingSwitch;
    private CameraPosition mBackCameraPosition;

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_NEARBY;
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


        //if (savedInstanceState == null){

            //Create fragments programatically
            //Parameters could come from an Intent ?
            mStationListFragment = StationListFragment.newInstance("bidon", "bidon");

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.station_list_or_info_container, mStationListFragment).commit();

        //}
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setActivityTitle(getString(R.string.title_section_nearby));

        mStationMapFragment = (StationMapFragment)getSupportFragmentManager().findFragmentById(
                R.id.station_map_fragment);


        //if (mNearbyFragment != null && savedInstanceState == null) {
        //    Bundle args = intentToFragmentArguments(getIntent());
        //    mNearbyFragment.reloadFromArguments(args);
        //}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nearby, menu);

        mParkingSwitch = menu.findItem(com.ludoscity.bikeactivityexplorer.R.id.findBikeParkingSwitchMenuItem);

        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.station_list_or_info_container);

        mParkingSwitch.setVisible(frag instanceof StationListFragment);

        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(com.ludoscity.bikeactivityexplorer.R.id.action_bar_find_bike_parking_switch)).setChecked(true);

        setOnClickFindSwitchListener();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        if (null != mParkingSwitch)
            mParkingSwitch.setVisible(!isOpen);
    }

    private void setOnClickFindSwitchListener() {
        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(com.ludoscity.bikeactivityexplorer.R.id.action_bar_find_bike_parking_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                mStationListFragment.lookingForBikes(isChecked);
                mStationMapFragment.lookingForBikes(isChecked);
                //if(isChecked){
                //Hackfix, the UX REALLY is improved by a toast like graphical element, though it seems bugged by recent changes (mea culpa)
                //toastText = getString(com.ludoscity.bikeactivityexplorer.R.string.findABikes);
                //icon = getResources().getDrawable(com.ludoscity.bikeactivityexplorer.R.drawable.bike_icon_toast);
                //}
                //else{
                //toastText = getString(com.ludoscity.bikeactivityexplorer.R.string.findAParking);
                //icon = getResources().getDrawable(com.ludoscity.bikeactivityexplorer.R.drawable.parking_icon_toast);
                //}

                // Create a toast with icon and text
                //TODO: create this as XML layout
                /*TextView toastView = new TextView(mContext);
                toastView.setAlpha(0.25f);
                toastView.setBackgroundColor(getResources().getColor(com.ludoscity.bikeactivityexplorer.R.color.background_floating_material_dark));
                toastView.setShadowLayer(2.75f, 0, 0, com.ludoscity.bikeactivityexplorer.R.color.background_floating_material_dark);
                toastView.setText(toastText);
                toastView.setTextSize(24f);
                toastView.setTextColor(getResources().getColor(com.ludoscity.bikeactivityexplorer.R.color.primary_text_default_material_dark));
                toastView.setGravity(Gravity.CENTER);
                icon.setBounds(0, 0, 64, 64);
                toastView.setCompoundDrawables(icon, null, null, null);
                toastView.setCompoundDrawablePadding(16);
                toastView.setPadding(5, 5, 5, 5);
                Toast toast = new Toast(mContext);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(toastView);
                toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();*/
            }
        });
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

                mUpdateRefreshRunnableCode = createUpdateRefreshRunnableCode();

                mUpdateRefreshHandler.post(mUpdateRefreshRunnableCode);
            }

            if(mStationMapFragment.isMapReady()) {
                if (mStationsNetwork != null && mRefreshMarkers) {

                    mRefreshMarkers = false;

                    new RedrawMarkersTask().execute();
                }

                if (null != mStationListFragment) {
                    if (null == mParkingSwitch)
                        mStationListFragment.setupUI(mStationsNetwork, mCurrentUserLatLng, true);
                    else
                        mStationListFragment.setupUI(mStationsNetwork, mCurrentUserLatLng, !mParkingSwitch.isChecked());
                }

                if (null != mBackCameraPosition){
                    mStationMapFragment.showAllMarkers();
                    mStationMapFragment.animateCamera(CameraUpdateFactory.newCameraPosition(mBackCameraPosition));

                    mBackCameraPosition = null;
                }
            }

        } else{
            mUpdateTextView.setText(getString(com.ludoscity.bikeactivityexplorer.R.string.nearbyfragment_default_never_web_updated));
        }
    }

    private Runnable createUpdateRefreshRunnableCode(){
        return new Runnable() {

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

                //UI will be refreshed every second
                mUpdateRefreshHandler.postDelayed(mUpdateRefreshRunnableCode, 1000);
            }
        };
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

        //Map ready
        if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_READY_PATH))
        {
            setupUI();
        }
        //User loc changed
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.LOCATION_CHANGED_PATH))
        {
            mCurrentUserLatLng = new LatLng(Double.valueOf(uri.getQueryParameter(StationMapFragment.LOCATION_CHANGED_LATITUDE_PARAM)),
                    Double.valueOf(uri.getQueryParameter(StationMapFragment.LOCATION_CHANGED_LONGITUDE_PARAM)));

            mStationListFragment.setCurrentUserLatLng(mCurrentUserLatLng);

            if (mStationInfoFragment != null){
                mStationInfoFragment.updateUserLatLng(mCurrentUserLatLng);
            }
        }
        //Marker click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MARKER_CLICK_PATH)){

            mStationListFragment.highlightStationFromName(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));

        }
        //InfoWindow click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.INFOWINDOW_CLICK_PATH)){

            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.station_list_or_info_container);
            if (frag instanceof StationListFragment){
                LatLng clickedMarkerPos = new LatLng(Double.valueOf(uri.getQueryParameter(StationMapFragment.INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM)),
                        Double.valueOf(uri.getQueryParameter(StationMapFragment.INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM)));

                for (StationItem station : mStationsNetwork.stations) {
                    if (station.getPosition().equals(clickedMarkerPos)) {

                        mBackCameraPosition = mStationMapFragment.getCameraPosition();

                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                        boundsBuilder.include(station.getPosition());

                        if (mCurrentUserLatLng != null)
                            boundsBuilder.include(mCurrentUserLatLng);

                        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));

                        mStationMapFragment.hideAllMarkers();
                        mStationMapFragment.showMarkerForStationUid(station.getUid());

                        mStationInfoFragment = StationInfoFragment.newInstance(station, mCurrentUserLatLng);

                        disableDrawer();

                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                        // Replace whatever is in the fragment_container view with this fragment,
                        // and add the transaction to the back stack so the user can navigate back
                        transaction.replace(R.id.station_list_or_info_container, mStationInfoFragment);
                        transaction.addToBackStack(null);

                        // Commit the transaction
                        transaction.commit();
                        break;
                    }
                }
            }
        }
    }

    private void cancelDownloadWebTask() {
        if (mDownloadWebTask != null && !mDownloadWebTask.isCancelled())
        {
            mDownloadWebTask.cancel(false);
            mDownloadWebTask = null;
        }
    }

    @Override
    public void onStationListFragmentInteraction(Uri uri) {

        if (uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_FRAG_ONRESUME_PATH))
        {
            enableDrawer();
            setActivityTitle(getString(R.string.title_section_nearby));
            setupUI();
        }
    }

    @Override
    public void onStationInfoFragmentInteraction(Uri uri) {

    }

    public class RedrawMarkersTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            mStationMapFragment.clearMarkerGfxData();

            //SETUP MARKERS DATA
            for (StationItem item : mStationsNetwork.stations){
                mStationMapFragment.addMarkerForStationItem(item);
            }

            return null;
        }

        @Override
        protected void onCancelled (Void aVoid) {
            super.onCancelled(aVoid);

            mRefreshMarkers = true;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mStationMapFragment.redrawMarkers();
            mStationMapFragment.lookingForBikes(mParkingSwitch.isChecked());
        }


    }

    public class DownloadWebTask extends AsyncTask<Context, Void, Void> {
        @Override
        protected Void doInBackground(Context... params) {
            //TODO : investigate having an Application class providing APIs like as done there
            //https://github.com/f8full/DirectionsOnMapv2WithRetrofit/blob/master/app/src/main/java/com/f8full/sample/directionsonmapv2withretrofit/RootApplication.java
            BixiAPI bixiApi = new BixiAPI(params[0]);
            //A Task that launches an other task, ok I want it to show the progress in the user interface
            //I finally advised against changing anything, instead I'll add a setting to display Database toast, and OFF by default
            //I do that because it seems it's not blocking / crashing if we try to navigate the interface anyway
            //Let the user choose when to update.
            mStationsNetwork = bixiApi.downloadBixiNetwork();
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
            //Set interface back
            mUpdateProgressBar.setVisibility(View.INVISIBLE);
            mRefreshButton.setVisibility(View.VISIBLE);

            //must be done last
            mDownloadWebTask = null;
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

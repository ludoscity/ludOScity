package com.ludoscity.bikeactivityexplorer;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.ludoscity.bikeactivityexplorer.Citybik_esAPI.Citybik_esAPI;
import com.ludoscity.bikeactivityexplorer.Citybik_esAPI.model.ListNetworksAnswerRoot;
import com.ludoscity.bikeactivityexplorer.Citybik_esAPI.model.NetworkDesc;
import com.ludoscity.bikeactivityexplorer.Citybik_esAPI.model.NetworkStatusAnswerRoot;
import com.ludoscity.bikeactivityexplorer.Citybik_esAPI.model.Station;
import com.ludoscity.bikeactivityexplorer.DBHelper.DBHelper;
import com.ludoscity.bikeactivityexplorer.Utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import retrofit.Call;
import retrofit.Response;

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
    private static final String PREF_NETWORK_NAME = "network_name";
    private static final String PREF_NETWORK_ID = "network_id";
    private static final String PREF_NETWORK_LATITUDE = "network_lat";
    private static final String PREF_NETWORK_LONGITUDE = "network_lng";
    private static final String PREF_NETWORK_HREF = "network_href";

    private Handler mUpdateRefreshHandler = null;
    private Runnable mUpdateRefreshRunnableCode = null;

    private DownloadWebTask mDownloadWebTask = null;
    private RedrawMarkersTask mRedrawMarkersTask = null;
    private FindNetworkTask mFindNetworkTask = null;

    private StationsNetwork mStationsNetwork;

    private LatLng mCurrentUserLatLng = null;


    private TextView mUpdateTextView;

    private View mDownloadBar;

    private boolean mRefreshMarkers = true;

    private boolean mLookingForBike = true;
    private MenuItem mParkingSwitch;
    private MenuItem mRefreshMenuItem;
    private MenuItem mFavoriteMenuItem;
    private MenuItem mDirectionsMenuItem;

    private CameraPosition mUnselectStationCameraPosition;
    private CameraPosition mSavedInstanceCameraPosition;

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_NEARBY;
    }

    @Override
    public void onStart(){
        super.onStart();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (Utils.Connectivity.isConnected(getApplicationContext()) && sp.getString(PREF_NETWORK_ID, "none").equalsIgnoreCase("none")) {

            mFindNetworkTask = new FindNetworkTask();
            mFindNetworkTask.execute();
        }
        else{
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

        setContentView(R.layout.activity_nearby);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        setActivityTitle(sp.getString(PREF_NETWORK_NAME, getTitle().toString()));
        setActivitySubtitle("");

        restoreActionBar();

        // Update Bar
        mUpdateTextView = (TextView) findViewById(com.ludoscity.bikeactivityexplorer.R.id.update_textView);
        mUpdateTextView.setTextColor(Color.LTGRAY);
        mDownloadBar = findViewById(com.ludoscity.bikeactivityexplorer.R.id.downloadBar);
        setDownloadBarListener();


        if (savedInstanceState == null){

            //Create fragments programatically
            //Parameters could come from an Intent ?
            mStationListFragment = StationListFragment.newInstance("bidon", "bidon");

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.station_list_or_info_container, mStationListFragment, "stationListFragTag").commit();

        }
        else{
            mStationListFragment = (StationListFragment)getSupportFragmentManager().findFragmentByTag("stationListFragTag");

            mUnselectStationCameraPosition = savedInstanceState.getParcelable("back_camera_pos");
            mSavedInstanceCameraPosition = savedInstanceState.getParcelable("saved_camera_pos");
            mLookingForBike = savedInstanceState.getBoolean("looking_for_bike");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("back_camera_pos", mUnselectStationCameraPosition);
        outState.putParcelable("saved_camera_pos", mStationMapFragment.getCameraPosition());
        outState.putBoolean("looking_for_bike", mLookingForBike);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

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

        mParkingSwitch = menu.findItem(com.ludoscity.bikeactivityexplorer.R.id.bike_parking_switch_menu_item);

        if (!mStationListFragment.isListReady())
            mParkingSwitch.setVisible(false);

        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(com.ludoscity.bikeactivityexplorer.R.id.action_bar_find_bike_parking_switch)).setChecked(mLookingForBike);

        setOnClickFindSwitchListener();

        mRefreshMenuItem = menu.findItem(R.id.refresh_menu_item);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        //This is here instead of findNetworkTask because menu is created late
        if (Utils.Connectivity.isConnected(getApplicationContext()) && sp.getString(PREF_NETWORK_ID, "none").equalsIgnoreCase("none")) {
            setRefreshActionButtonState(true);
        }
        else if (!Utils.Connectivity.isConnected(getApplicationContext()))
            mRefreshMenuItem.setVisible(false);

        mFavoriteMenuItem = menu.findItem(R.id.favorite_menu_item);

        mDirectionsMenuItem = menu.findItem(R.id.directions_menu_item);

        if (null != mStationListFragment) {

            StationItem highlightedStation = mStationListFragment.getHighlightedStation();
            if (null != highlightedStation) {
                setupFavoriteActionIcon(highlightedStation);
                mDirectionsMenuItem.setVisible(true);
            }

            else {
                mFavoriteMenuItem.setVisible(false);
                mDirectionsMenuItem.setVisible(false);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.refresh_menu_item:

                if (Utils.Connectivity.isConnected(getApplicationContext()) && mDownloadWebTask == null) {
                    mDownloadWebTask = new DownloadWebTask();
                    mDownloadWebTask.execute();
                }

                return true;

            case R.id.favorite_menu_item:

                StationItem station = mStationListFragment.getHighlightedStation();

                boolean newState = !station.isFavorite();

                station.setFavorite(newState);

                if (newState)
                    mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite);
                else
                    mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite_outline);

                return true;

            case R.id.directions_menu_item:

                StationItem targetStation = mStationListFragment.getHighlightedStation();

                StringBuilder builder = new StringBuilder("http://maps.google.com/maps?&saddr=").
                        append(mCurrentUserLatLng.latitude).
                        append(",").
                        append(mCurrentUserLatLng.longitude).
                        append("&daddr=").
                        append(targetStation.getPosition().latitude).
                        append(",").
                        append(targetStation.getPosition().longitude).
                        append("&dirflg=");

                if (mLookingForBike)
                    builder.append("w");
                else
                    builder.append("b");

                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(builder.toString()));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                    startActivity(intent); // launch the map activity
                } else {
                    Toast.makeText(this, getString(R.string.google_maps_not_installed), Toast.LENGTH_LONG).show();

                }


                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {

        if (null != mParkingSwitch)
            mParkingSwitch.setVisible(!isOpen && !isAnimating);

        if (null != mStationListFragment.getHighlightedStation())
        {
            if (null != mFavoriteMenuItem)
                mFavoriteMenuItem.setVisible(!isOpen && !isAnimating);
            if (null != mDirectionsMenuItem)
                mDirectionsMenuItem.setVisible(!isOpen && !isAnimating);
        }
    }

    private void setOnClickFindSwitchListener() {
        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(com.ludoscity.bikeactivityexplorer.R.id.action_bar_find_bike_parking_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                mStationListFragment.lookingForBikes(isChecked);
                mStationMapFragment.lookingForBikes(isChecked);
                mLookingForBike = isChecked;
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

        if (mUpdateRefreshRunnableCode == null) {

            mUpdateRefreshRunnableCode = createUpdateRefreshRunnableCode();

            mUpdateRefreshHandler.post(mUpdateRefreshRunnableCode);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String currNetworkId = sp.getString(PREF_NETWORK_ID, "none");
        if (!currNetworkId.equalsIgnoreCase("none")){

            if(mStationMapFragment.isMapReady()) {
                if (mStationsNetwork != null && mRefreshMarkers && mRedrawMarkersTask == null) {

                    mRedrawMarkersTask = new RedrawMarkersTask();
                    mRedrawMarkersTask.execute();

                    mRefreshMarkers = false;
                }

                if (null != mSavedInstanceCameraPosition){
                    mStationMapFragment.doInitialCameraSetup(CameraUpdateFactory.newCameraPosition(mSavedInstanceCameraPosition), false);
                    mSavedInstanceCameraPosition = null;
                }

                if (null != mStationListFragment){
                    mStationListFragment.setupUI(mStationsNetwork, mCurrentUserLatLng, mLookingForBike);

                    if (null != mParkingSwitch && !mParkingSwitch.isVisible())
                        mParkingSwitch.setVisible(true);
                }
            }
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
                if (mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null) {

                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    long runnableLastRefreshTimestamp = sp.getLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, 0);

                    long difference = now - runnableLastRefreshTimestamp;

                    StringBuilder updateTextBuilder = new StringBuilder();

                    if (!sp.getString(PREF_NETWORK_ID, "none").equalsIgnoreCase("none")) {
                        //First taking care of past time...
                        if (difference < DateUtils.MINUTE_IN_MILLIS)
                            updateTextBuilder.append(getString(com.ludoscity.bikeactivityexplorer.R.string.momentsAgo)).append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.fromCitibik_es));//mUpdateTextView.setText();
                        else
                            updateTextBuilder.append(Long.toString(difference / DateUtils.MINUTE_IN_MILLIS)).append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.minsAgo)).append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.fromCitibik_es));
                    }
                    //mUpdateTextView.setText(Long.toString(difference / DateUtils.MINUTE_IN_MILLIS) +" "+ getString(R.string.minsAgo) + " " + getString(R.string.fromCitibik_es) );

                    //long differenceInMinutes = difference / DateUtils.MINUTE_IN_MILLIS;

                    //from : http://stackoverflow.com/questions/25355611/how-to-get-time-difference-between-two-dates-in-android-app
                    //long differenceInSeconds = difference / DateUtils.SECOND_IN_MILLIS;
// formatted will be HH:MM:SS or MM:SS
                    //String formatted = DateUtils.formatElapsedTime(differenceInSeconds);

                    //... then about next update
                    if (Utils.Connectivity.isConnected(getApplicationContext())) {

                        if (!mRefreshMenuItem.isVisible())
                            mRefreshMenuItem.setVisible(true);

                        if (!sp.getString(PREF_NETWORK_ID, "none").equalsIgnoreCase("none")) {

                            boolean autoUpdate = sp.getBoolean(UserSettingsFragment.PREF_NEARBY_AUTO_UPDATE, true);

                            if (!autoUpdate) {
                                //updateTextBuilder.append(" - ").append(getString(R.string.nearbyfragment_no_auto_update));

                                setRefreshActionButtonState(false);
                            } else {

                                //Should come from something keeping tabs on time, maybe this runnable itself
                                long wishedUpdateTime = runnableLastRefreshTimestamp + 5 * 1000 * 60;  //comes from Prefs

                                if (now >= wishedUpdateTime) {

                                    //Put a string same length as the other one ?
                                    updateTextBuilder.append(" ").append(getString(R.string.downloading));

                                    //Run update

                                    mDownloadWebTask = new DownloadWebTask();
                                    mDownloadWebTask.execute();


                                    //lastUpdateTime = now;
                                } else {

                                    updateTextBuilder.append(" - ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.nextUpdate)).append(" ");


                                    long differenceSecond = (wishedUpdateTime - now) / DateUtils.SECOND_IN_MILLIS;

                                    // formatted will be HH:MM:SS or MM:SS
                                    updateTextBuilder.append(DateUtils.formatElapsedTime(differenceSecond));

                                    setRefreshActionButtonState(false);
                                }
                            }
                        }
                        else{
                            mFindNetworkTask = new FindNetworkTask();
                            mFindNetworkTask.execute();
                        }
                    }
                    else{
                        updateTextBuilder.append(" - ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.no_connectivity));

                        setRefreshActionButtonState(false);
                        if (null != mRefreshMenuItem)
                            mRefreshMenuItem.setVisible(false);
                    }

                    mUpdateTextView.setText(updateTextBuilder.toString());
                }

                //UI will be refreshed every second
                mUpdateRefreshHandler.postDelayed(mUpdateRefreshRunnableCode, 1000);
            }
        };
    }

    private void setDownloadBarListener() {
        mDownloadBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.Connectivity.isConnected(getApplicationContext())) {
                    Intent implicit = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.citybik.es"));
                    startActivity(implicit);
                }
            }
        });
    }

    private void setRefreshActionButtonState(final boolean refreshing) {
        if (mRefreshMenuItem != null) {

            if (null == mRefreshMenuItem.getActionView())
            {
                if (refreshing)
                    mRefreshMenuItem.setActionView(R.layout.action_refresh_progress);
            }
            else
            {
                if (!refreshing)
                    mRefreshMenuItem.setActionView(null);
            }
        }
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
            setupFavoriteActionIcon(mStationListFragment.getHighlightedStation());
            mDirectionsMenuItem.setVisible(true);
            mStationMapFragment.oversizeMarkerUniqueForStationName(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));

        }
        //Map click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_CLICK_PATH)){

            StationItem highlightedStation = mStationListFragment.getHighlightedStation();

            if (null != highlightedStation){

                mFavoriteMenuItem.setVisible(false);
                mDirectionsMenuItem.setVisible(false);
                mStationListFragment.removeStationHighlight();
                mStationMapFragment.resetMarkerSizeAll();
            }

        }
        //InfoWindow click
        /*else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.INFOWINDOW_CLICK_PATH)){

            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.station_list_or_info_container);
            if (frag instanceof StationListFragment){
                LatLng clickedMarkerPos = new LatLng(Double.valueOf(uri.getQueryParameter(StationMapFragment.INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM)),
                        Double.valueOf(uri.getQueryParameter(StationMapFragment.INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM)));

                for (StationItem station : mStationsNetwork.stations) {
                    if (station.getPosition().equals(clickedMarkerPos)) {

                        mUnselectStationCameraPosition = mStationMapFragment.getCameraPosition();

                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                        boundsBuilder.include(station.getPosition());

                        if (mCurrentUserLatLng != null)
                            boundsBuilder.include(mCurrentUserLatLng);

                        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));

                        mStationMapFragment.hideAllMarkers();
                        mStationMapFragment.showMarkerForStationUid(station.getUid());
                        mStationMapFragment.setEnforceMaxZoom(true);

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
        }*/
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

        /*if (uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_FRAG_ONRESUME_PATH))
        {
            enableDrawer();
            setActivityTitle(getString(R.string.title_section_nearby));
            mStationMapFragment.setEnforceMaxZoom(false);
            setupUI();
        }
        else*/ if (uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_ITEM_CLICK_PATH))
        {
            //if null, means the station was clicked twice, hence unchecked
            StationItem clickedStation = mStationListFragment.getHighlightedStation();
            if (null == clickedStation){

                mFavoriteMenuItem.setVisible(false);
                mDirectionsMenuItem.setVisible(false);
                mStationMapFragment.resetMarkerSizeAll();
                mStationMapFragment.animateCamera(CameraUpdateFactory.newCameraPosition(mUnselectStationCameraPosition));

            }
            else {

                setupFavoriteActionIcon(clickedStation);
                mDirectionsMenuItem.setVisible(true);

                mStationMapFragment.oversizeMarkerUniqueForStationName(clickedStation.getName());

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                boundsBuilder.include(clickedStation.getPosition());

                if (mCurrentUserLatLng != null)
                    boundsBuilder.include(mCurrentUserLatLng);

                mUnselectStationCameraPosition = mStationMapFragment.getCameraPosition();

                mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 200));
            }
        }
    }

    private void setupFavoriteActionIcon(StationItem station) {
        if (station.isFavorite())
            mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite);
        else
            mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite_outline);

        mFavoriteMenuItem.setVisible(true);
    }

    @Override
    public void onStationInfoFragmentInteraction(Uri uri) {

    }

    public class RedrawMarkersTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //mStationMapFragment.invalidateAllMarker();

            mStationMapFragment.clearMarkerGfxData();

            mUpdateTextView.setText(getString(R.string.refreshing));
            setRefreshActionButtonState(true);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            //The ugliest part of this app. After numerous tests, it became evident that I cannot
            //manipulate GroundOverlays from a background thread AND that for some reason I also
            //can't clear and recreate them too rapidly. Hence this.
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onCancelled (Void aVoid) {
            super.onCancelled(aVoid);

            mRefreshMarkers = true;

            mRedrawMarkersTask = null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            setRefreshActionButtonState(false);

            //SETUP MARKERS DATA
            for (StationItem item : mStationsNetwork.stations){
                mStationMapFragment.addMarkerForStationItem(item, mLookingForBike);
            }

            mStationMapFragment.redrawMarkers();

            StationItem highlighted = mStationListFragment.getHighlightedStation();

            if (null != highlighted)
                mStationMapFragment.oversizeMarkerUniqueForStationName(highlighted.getName());

            mRedrawMarkersTask = null;
        }
    }

    public class FindNetworkTask extends AsyncTask<Void, Void, Void> {

        //private final ProgressDialog mFindNetworkDialog = new ProgressDialog(NearbyActivity.this, R.style.BikeActivityExplorerTheme);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mUpdateTextView.setText(getString(R.string.searching_wait_location));

            setRefreshActionButtonState(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            setRefreshActionButtonState(false);

            mFindNetworkTask = null;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            //noinspection StatementWithEmptyBody
            while (mCurrentUserLatLng == null)
            {

            }

            publishProgress();

            Citybik_esAPI api = ((RootApplication) getApplication()).getCitybik_esApi();

            final Call<ListNetworksAnswerRoot> call = api.listNetworks();

            Response<ListNetworksAnswerRoot> listAnswer;

            try {
                listAnswer = call.execute();

                ArrayList<NetworkDesc> answerList = listAnswer.body().networks;

                Collections.sort(answerList, new Comparator<NetworkDesc>() {
                    @Override
                    public int compare(NetworkDesc networkDesc, NetworkDesc t1) {

                        return (int) (networkDesc.getMeterFromLatLng(mCurrentUserLatLng) - t1.getMeterFromLatLng(mCurrentUserLatLng));
                    }
                });

                NetworkDesc closestNetwork = answerList.get(0);

                //TODO: multiple networks

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NearbyActivity.this);

                SharedPreferences.Editor editor = sp.edit();

                editor.putString(PREF_NETWORK_ID, closestNetwork.id);
                editor.putString(PREF_NETWORK_NAME, closestNetwork.name);
                editor.putString(PREF_NETWORK_HREF, closestNetwork.href);
                editor.putLong(PREF_NETWORK_LATITUDE, Double.doubleToLongBits(closestNetwork.location.latitude));
                editor.putLong(PREF_NETWORK_LONGITUDE, Double.doubleToLongBits(closestNetwork.location.longitude));

                editor.apply();

            } catch (IOException e) {
                Toast toast;

                toast = Toast.makeText(getApplicationContext(),getString(R.string.download_failed),Toast.LENGTH_LONG);

                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                cancel(false); //No need to try to interrupt the thread
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            mUpdateTextView.setText(getString(R.string.searching_bike_network));

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NearbyActivity.this);

            setActivityTitle(sp.getString(PREF_NETWORK_NAME, getTitle().toString()));
            restoreActionBar();

            AlertDialog alertDialog = new AlertDialog.Builder(NearbyActivity.this).create();
            //alertDialog.setTitle(getString(R.string.network_found_title));
            alertDialog.setMessage(Html.fromHtml(String.format(getString(R.string.network_found), sp.getString(NearbyActivity.PREF_NETWORK_NAME, "Bixi"))));

            alertDialog.show();

            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();

            mFindNetworkTask = null;
        }
    }

    public class addNetworkDatabase extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                DBHelper.addNetwork(mStationsNetwork);
            } catch (Exception e) {
                Log.d("BixiAPI", "Error saving network", e );
            }
            return null;
        }

        //Should happen or not on settings fragment/prefs ?
        /*@Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Toast.makeText(context, "DatabaseUpdate Successful!", Toast.LENGTH_LONG).show();
        }*/
    }

    public class DownloadWebTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... aVoid) {

            Map<String, String> UrlParams = new HashMap<>();
            UrlParams.put("fields", "stations");

            Citybik_esAPI api = ((RootApplication) getApplication()).getCitybik_esApi();

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NearbyActivity.this);
            final Call<NetworkStatusAnswerRoot> call = api.getNetworkStatus(sp.getString(NearbyActivity.PREF_NETWORK_HREF, "/v2/networks/bixi-montreal"), UrlParams);

            Response<NetworkStatusAnswerRoot> statusAnswer;

            try {
                statusAnswer = call.execute();

                mStationsNetwork = new StationsNetwork();

                for (Station station : statusAnswer.body().network.stations) {
                    StationItem stationItem = new StationItem(station, DBHelper.isFavorite(station.extra.uid));
                    mStationsNetwork.stations.add(stationItem);
                }
            } catch (CouchbaseLiteException e) {
                Log.d("TAG", "Problem with the database", e);
            } catch (IOException e) {
                Toast toast;

                toast = Toast.makeText(getApplicationContext(),getString(R.string.download_failed),Toast.LENGTH_LONG);

                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                cancel(false); //No need to try to interrupt the thread
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mUpdateTextView.setText(getString(R.string.downloading));

            setRefreshActionButtonState(true);
        }

        @Override
        protected void onCancelled (Void aVoid){
            super.onCancelled(aVoid);
            //Set interface back
            setRefreshActionButtonState(false);

            //must be done last
            mDownloadWebTask = null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //switch progressbar view visibility

            setRefreshActionButtonState(false);

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

            new addNetworkDatabase().execute();

            //must be done last
            mDownloadWebTask = null;
        }
    }
}

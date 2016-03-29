package com.ludoscity.findmybikes.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.RootApplication;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationListPagerAdapter;
import com.ludoscity.findmybikes.citybik_es.Citybik_esAPI;
import com.ludoscity.findmybikes.citybik_es.model.ListNetworksAnswerRoot;
import com.ludoscity.findmybikes.citybik_es.model.NetworkDesc;
import com.ludoscity.findmybikes.citybik_es.model.NetworkStatusAnswerRoot;
import com.ludoscity.findmybikes.citybik_es.model.Station;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.fragments.StationMapFragment;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by F8Full on 2015-07-26.
 * Activity used to display the nearby section
 */
public class NearbyActivity extends AppCompatActivity
        implements StationMapFragment.OnStationMapFragmentInteractionListener,
        StationListFragment.OnStationListFragmentInteractionListener,
        SwipeRefreshLayout.OnRefreshListener,
        ViewPager.OnPageChangeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private StationMapFragment mStationMapFragment = null;

    private Handler mUpdateRefreshHandler = null;
    private Runnable mUpdateRefreshRunnableCode = null;

    private DownloadWebTask mDownloadWebTask = null;
    private RedrawMarkersTask mRedrawMarkersTask = null;
    private FindNetworkTask mFindNetworkTask = null;

    private ArrayList<StationItem> mStationsNetwork = new ArrayList<>();

    private LatLng mCurrentUserLatLng = null;

    private TextView mStatusTextView;
    private View mStatusBar;
    private ViewPager mStationListViewPager;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBarLayout;
    private CoordinatorLayout mCoordinatorLayout;

    private FloatingActionButton mPlacePickerFAB;
    private FloatingActionButton mFavoriteToggleFAB;
    private boolean mFavoriteToggled = false;

    private boolean mRefreshMarkers = true;

    private CameraPosition mSavedInstanceCameraPosition;

    private static final int[] TABS_ICON_RES_ID = new int[]{
            R.drawable.ic_a_bike,
            R.drawable.ic_bike_b
    };

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = false;

    private boolean mClosestBikeAutoSelected = false;

    @Override
    public void onStart() {

        mGoogleApiClient.connect();

        if (Utils.Connectivity.isConnected(getApplicationContext()) && !DBHelper.isBikeNetworkIdAvailable(this)) {

            mFindNetworkTask = new FindNetworkTask();
            mFindNetworkTask.execute();
        }
        else if(mStationsNetwork.isEmpty()){

            boolean needDownload = false;

            try {
                mStationsNetwork = DBHelper.getStationsNetwork();
            } catch (CouchbaseLiteException e) {
                Log.d("nearbyActivity", "Couldn't retrieve Station Network from db, trying to get a fresh copy from network",e );

                needDownload = true;
            }

            if (needDownload || mStationsNetwork.isEmpty()){
                mDownloadWebTask = new DownloadWebTask();
                mDownloadWebTask.execute();
            }

            Log.d("nearbyActivity", mStationsNetwork.size() + " stations loaded from DB");
        }

        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    public void onResume() {

        super.onResume();

        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        mUpdateRefreshHandler = new Handler();

        setupUI();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mRequestingLocationUpdates = true;
        }
    }

    @Override
    public void onPause() {

        super.onPause();

        cancelDownloadWebTask();
        stopUIRefresh();

        if (mGoogleApiClient.isConnected()) {

            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            mRequestingLocationUpdates = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

            mSavedInstanceCameraPosition = savedInstanceState.getParcelable("saved_camera_pos");
            mStationsNetwork = savedInstanceState.getParcelableArrayList("network_data");
            mRequestingLocationUpdates = savedInstanceState.getBoolean("requesting_location_updates");
            mCurrentUserLatLng = savedInstanceState.getParcelable("user_location_latlng");
            mClosestBikeAutoSelected = savedInstanceState.getBoolean("closest_bike_auto_selected");
            mFavoriteToggled = savedInstanceState.getBoolean("favorite_toggled");
        }

        setContentView(R.layout.activity_nearby);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(getString(R.string.app_name));
        getSupportActionBar().setSubtitle(DBHelper.getBikeNetworkName(this));

        // Update Bar
        mStatusTextView = (TextView) findViewById(R.id.status_textView);
        mStatusBar = findViewById(R.id.statusBar);

        mStationListViewPager = (ViewPager)findViewById(R.id.station_list_viewpager);
        mStationListViewPager.setAdapter(new StationListPagerAdapter(getSupportFragmentManager()));
        mStationListViewPager.addOnPageChangeListener(this);

        // Give the TabLayout the ViewPager
        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mStationListViewPager);

        //Taking care of tabs icons here as pageradapter handles only title CharSequence for now
        for (int i=0; i<mTabLayout.getTabCount() && i<TABS_ICON_RES_ID.length; ++i)
        {
            //noinspection ConstantConditions
            mTabLayout.getTabAt(i).setCustomView(R.layout.tab_custom_view);
            mTabLayout.getTabAt(i).setIcon(ContextCompat.getDrawable(this,TABS_ICON_RES_ID[i]));
        }

        mAppBarLayout = (AppBarLayout) findViewById(R.id.action_toolbar_layout);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.appbar_coordinator);

        mPlacePickerFAB = (FloatingActionButton) findViewById(R.id.place_picker_fab);
        mFavoriteToggleFAB = (FloatingActionButton) findViewById(R.id.favorite_toggle_fab);
        mFavoriteToggleFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFavoriteToggled = !mFavoriteToggled;
                setupFavoriteFab();
            }
        });

        setupFavoriteFab();

        setStatusBarClickListener();

        getListPagerAdapter().setCurrentUserLatLng(mCurrentUserLatLng);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setupFavoriteFab() {
        if (mFavoriteToggled){
            mFavoriteToggleFAB.setImageResource(R.drawable.ic_action_favorite_toggle_on_24dp);
            mFavoriteToggleFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.favorite_fab_toggle_on_background)));
            mPlacePickerFAB.hide();
        }
        else {
            mFavoriteToggleFAB.setImageResource(R.drawable.ic_action_favorite_toggle_off_24dp);
            mFavoriteToggleFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.theme_primary_dark)));

            if (!isLookingForBike())
                mPlacePickerFAB.show();
        }
    }

    @NonNull
    private String getActionbarTitle() {
        String titleToSet = getTitle().toString();

        if (DBHelper.isBikeNetworkIdAvailable(this))
        {
            titleToSet = DBHelper.getBikeNetworkName(this);
        }
        return titleToSet;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("saved_camera_pos", mStationMapFragment.getCameraPosition());
        outState.putParcelableArrayList("network_data", mStationsNetwork);
        outState.putBoolean("requesting_location_updates", mRequestingLocationUpdates);
        outState.putParcelable("user_location_latlng", mCurrentUserLatLng);
        outState.putBoolean("closest_bike_auto_selected", mClosestBikeAutoSelected);
        outState.putBoolean("favorite_toggled", mFavoriteToggled);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mStationMapFragment = (StationMapFragment)getSupportFragmentManager().findFragmentById(
                R.id.station_map_fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nearby, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // permission was granted, yay! Do the
            mStationMapFragment.enableMyLocationCheckingPermission();

        }else {

            //TODO: Actually do something so that it doesn't look and feel horribly horribly broken
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            case R.id.settings_menu_item:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeFavorite(final StationItem station) {
        station.setFavorite(false, this);

        if (mCoordinatorLayout != null)
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_removed, Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    /*.setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            addFavorite(station);

                        }
                    })*/.show();
        else //TODO: Rework landscape layout
            Toast.makeText(this, getString(R.string.favorite_removed), Toast.LENGTH_SHORT).show();
    }

    private void addFavorite(final StationItem station) {
        station.setFavorite(true, this);

        //getListPagerAdapter().addStationForPage(StationListPagerAdapter.DOCK_STATIONS, station);

        if (mCoordinatorLayout != null)
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_added, Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    /*.setAction(R.string.undo,new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            removeFavorite(station);

                        }
                    })*/.show();
        else //TODO: Rework landscape layout
            Toast.makeText(this, getString(R.string.favorite_added),Toast.LENGTH_SHORT).show();
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

        if (mUpdateRefreshRunnableCode == null && mUpdateRefreshHandler != null) {

            mUpdateRefreshRunnableCode = createUpdateRefreshRunnableCode();

            mUpdateRefreshHandler.post(mUpdateRefreshRunnableCode);
        }

        if (DBHelper.isBikeNetworkIdAvailable(this)){

            if(mStationMapFragment.isMapReady()) {
                if (mStationsNetwork != null && mRefreshMarkers && mRedrawMarkersTask == null) {

                    mRedrawMarkersTask = new RedrawMarkersTask();
                    mRedrawMarkersTask.execute(isLookingForBike());

                    mRefreshMarkers = false;
                }

                if (null != mSavedInstanceCameraPosition){
                    mStationMapFragment.doInitialCameraSetup(CameraUpdateFactory.newCameraPosition(mSavedInstanceCameraPosition), false);
                    mSavedInstanceCameraPosition = null;
                }

                if (getListPagerAdapter().isViewPagerReady()){

                    getListPagerAdapter().setupUI(StationListPagerAdapter.BIKE_STATIONS, mStationsNetwork, "", mCurrentUserLatLng, mCurrentUserLatLng);

                    LatLng stationBLatLng = mStationMapFragment.getMarkerBLatLng();

                    if (stationBLatLng == null)
                        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(), getString(R.string.tab_b_instructions), null, null);
                    else
                        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, mStationsNetwork, "",
                                stationBLatLng, mStationMapFragment.getMarkerALatLng());
                }
            }
        }
    }

    private Runnable createUpdateRefreshRunnableCode(){
        return new Runnable() {

            private boolean mPagerReady = false;

            /*private final long startTime = System.currentTimeMillis();
            private long lastRunTime;
            private long lastUpdateTime = System.currentTimeMillis();   //Update should be run automatically ?
            */
            @Override
            public void run() {

                long now = System.currentTimeMillis();

                if (!mPagerReady && getListPagerAdapter().isViewPagerReady()){
                    //TODO: calling DBHelper.getFavoriteStations can return incomplete result when db is updating
                    //it happens when data is downladed and screen configuration is changed shortly after
                    getListPagerAdapter().setupUI(StationListPagerAdapter.BIKE_STATIONS, mStationsNetwork, "", mCurrentUserLatLng, mCurrentUserLatLng);

                    LatLng stationBLatLng = mStationMapFragment.getMarkerBLatLng();

                    if (stationBLatLng == null)
                        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(), getString(R.string.tab_b_instructions), null, null);
                    else
                        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, mStationsNetwork, "",
                                stationBLatLng, mStationMapFragment.getMarkerALatLng());

                    mPagerReady = true;
                }

                //Update not already in progress
                if (mPagerReady && mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null) {

                    long runnableLastRefreshTimestamp = DBHelper.getLastUpdateTimestamp(getApplicationContext());

                    long difference = now - runnableLastRefreshTimestamp;

                    StringBuilder pastStringBuilder = new StringBuilder();
                    StringBuilder futureStringBuilder = new StringBuilder();

                    if (DBHelper.isBikeNetworkIdAvailable(getApplicationContext())) {
                        //First taking care of past time...
                        if (difference < DateUtils.MINUTE_IN_MILLIS)
                            pastStringBuilder.append(getString(R.string.moments));
                        else
                            pastStringBuilder.append(getString(R.string.il_y_a)).append(Long.toString(difference / DateUtils.MINUTE_IN_MILLIS)).append(" ").append(getString(R.string.min));
                    }
                    //mStatusTextView.setText(Long.toString(difference / DateUtils.MINUTE_IN_MILLIS) +" "+ getString(R.string.minsAgo) + " " + getString(R.string.fromCitibik_es) );

                    //long differenceInMinutes = difference / DateUtils.MINUTE_IN_MILLIS;

                    //from : http://stackoverflow.com/questions/25355611/how-to-get-time-difference-between-two-dates-in-android-app
                    //long differenceInSeconds = difference / DateUtils.SECOND_IN_MILLIS;
// formatted will be HH:MM:SS or MM:SS
                    //String formatted = DateUtils.formatElapsedTime(differenceInSeconds);

                    //... then about next update
                    if (Utils.Connectivity.isConnected(getApplicationContext())) {

                        getListPagerAdapter().setRefreshEnableAll(true);

                        if (DBHelper.isBikeNetworkIdAvailable(getApplicationContext())) {

                            if (!DBHelper.getAutoUpdate(getApplicationContext())) {
                                futureStringBuilder.append(getString(R.string.pull_to_refresh));

                            } else {

                                //Should come from something keeping tabs on time, maybe this runnable itself
                                long wishedUpdateTime = runnableLastRefreshTimestamp + 5 * 1000 * 60;  //comes from Prefs
                                //Debug
                                //long wishedUpdateTime = runnableLastRefreshTimestamp + 15 * 1000;  //comes from Prefs

                                if (now >= wishedUpdateTime) {

                                    mDownloadWebTask = new DownloadWebTask();
                                    mDownloadWebTask.execute();

                                } else {

                                    futureStringBuilder.append(getString(R.string.nextUpdate)).append(" ");
                                    long differenceSecond = (wishedUpdateTime - now) / DateUtils.SECOND_IN_MILLIS;

                                    // formatted will be HH:MM:SS or MM:SS
                                    futureStringBuilder.append(DateUtils.formatElapsedTime(differenceSecond));
                                }
                            }
                        }
                        else{
                            mFindNetworkTask = new FindNetworkTask();
                            mFindNetworkTask.execute();
                        }
                    } else {
                        futureStringBuilder.append(getString(R.string.no_connectivity));

                        getListPagerAdapter().setRefreshEnableAll(false);
                    }

                    if (mDownloadWebTask == null)
                        mStatusTextView.setText(String.format(getString(R.string.status_string),
                                pastStringBuilder.toString(), futureStringBuilder.toString()));

                    if (mDownloadWebTask == null && mRedrawMarkersTask == null &&
                            !mClosestBikeAutoSelected &&
                            ((StationListPagerAdapter)mStationListViewPager.getAdapter()).isRecyclerViewReadyForItemSelection(StationListPagerAdapter.BIKE_STATIONS)){

                        ((StationListPagerAdapter) mStationListViewPager.getAdapter()).highlightClosestStationWithAvailability(true);
                        StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
                        mStationMapFragment.setPinOnStation(true, closestBikeStation.getName());

                        if (isLookingForBike())
                            animateCameraToShowUserAndStation(closestBikeStation);

                        mClosestBikeAutoSelected = true;
                    }
                }

                //UI will be refreshed every second
                int nextTimeMillis = 1000;

                if (!mPagerReady) //Except on init
                    nextTimeMillis = 100;

                mUpdateRefreshHandler.postDelayed(mUpdateRefreshRunnableCode, nextTimeMillis);
            }
        };
    }

    private void setStatusBarClickListener() {
        mStatusBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.Connectivity.isConnected(getApplicationContext())) {
                    Intent implicit = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.citybik.es"));
                    startActivity(implicit);
                }
            }
        });
    }


    @Override
    public void onStationMapFragmentInteraction(final Uri uri) {
        //Will be warned of station details click, will make info fragment to replace list fragment

        //Map ready
        if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_READY_PATH))
        {
            setupUI();
        }
        //Marker click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MARKER_CLICK_PATH)){

            if (isLookingForBike()) {
                if (mAppBarLayout != null)
                    mAppBarLayout.setExpanded(false, true);

                if (getListPagerAdapter().highlightStationFromNameForPage(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM),
                        StationListPagerAdapter.BIKE_STATIONS)) {

                    mStationMapFragment.setPinOnStation(true,
                            uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));

                    if (mStationMapFragment.getMarkerBLatLng() != null)
                        getListPagerAdapter().notifyStationAUpdate(mStationMapFragment.getMarkerALatLng());
                }
            }
            else {
                //B Tab, looking for dock
                //Remove any previous selection
                getListPagerAdapter().removeStationHighlightForPage(StationListPagerAdapter.DOCK_STATIONS);

                mStationMapFragment.setPinOnStation(false,
                        uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));

                mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBLatLng(), 15));

                //Replace recyclerview content
                getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, mStationsNetwork, "",
                        mStationMapFragment.getMarkerBLatLng(),  mStationMapFragment.getMarkerALatLng());

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (getListPagerAdapter().isRecyclerViewReadyForItemSelection(StationListPagerAdapter.DOCK_STATIONS)) {
                            //highlight B station in list
                            getListPagerAdapter().highlightStationFromNameForPage(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM),
                                    StationListPagerAdapter.DOCK_STATIONS);
                        } else
                            handler.postDelayed(this, 10);
                    }
                }, 10);

            }
        }
        //Map click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_CLICK_PATH)){


        }
    }

    private boolean isLookingForBike() {
        return mStationListViewPager.getCurrentItem() == StationListPagerAdapter.BIKE_STATIONS;
    }

    private void cancelDownloadWebTask() {
        if (mDownloadWebTask != null && !mDownloadWebTask.isCancelled())
        {
            mDownloadWebTask.cancel(false);
            mDownloadWebTask = null;
        }
    }

    @Override
    public void onStationListFragmentInteraction(final Uri uri) {

        if (uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_ITEM_CLICK_PATH))
        {
            //if null, means the station was clicked twice, hence unchecked
            final StationItem clickedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

            //TODO: Rework landscape layout
            //hackfix
            //if (mAppBarLayout != null)
            //    mAppBarLayout.setExpanded(true , true);

            mStationMapFragment.setPinOnStation(isLookingForBike(), clickedStation.getName());

            if (isLookingForBike())
                animateCameraToShowUserAndStation(clickedStation);
            else{

                mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBLatLng(), 15));

                //Replace recyclerview content
                getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, mStationsNetwork, "",
                        mStationMapFragment.getMarkerBLatLng(), mStationMapFragment.getMarkerALatLng());

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (getListPagerAdapter().isRecyclerViewReadyForItemSelection(StationListPagerAdapter.DOCK_STATIONS)) {
                            //highlight B station in list
                            getListPagerAdapter().highlightStationFromNameForPage(clickedStation.getName(),
                                    StationListPagerAdapter.DOCK_STATIONS);
                        } else
                            handler.postDelayed(this, 10);
                    }
                }, 10);

            }
        }
        else if (uri.getPath().equalsIgnoreCase("/"+ StationListFragment.STATION_LIST_FAVORITE_FAB_CLICK_PATH)){

            //Setup favorite icon should be done at view binding time, might be inefficient though

            final StationItem curSelectedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

            //should not be null, fabs are showed only after selection
            if (null != curSelectedStation) {

                boolean newState = !curSelectedStation.isFavorite(this);

                if (newState) {
                    addFavorite(curSelectedStation);
                } else {
                    removeFavorite(curSelectedStation);
                }
            }
        }
        else if (uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_DIRECTIONS_FAB_CLICK_PATH)){

            final StationItem curSelectedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

            // Seen NullPointerException in crash report.
            if (null != curSelectedStation) {
                StringBuilder builder = new StringBuilder("http://maps.google.com/maps?&saddr=");

                if (isLookingForBike()){
                    builder.append(mCurrentUserLatLng.latitude).
                            append(",").
                            append(mCurrentUserLatLng.longitude);
                }
                else{
                    builder.append(mStationMapFragment.getMarkerALatLng().latitude).
                            append(",").
                            append(mStationMapFragment.getMarkerALatLng().longitude);
                }

                        builder.append("&daddr=").
                        append(curSelectedStation.getPosition().latitude).
                        append(",").
                        append(curSelectedStation.getPosition().longitude).
                        append("&dirflg=");

                if (isLookingForBike())
                    builder.append("w");
                else
                    builder.append("b");

                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(builder.toString()));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                    startActivity(intent); // launch the map activity
                } else {
                    //TODO: replace by Snackbar
                    Toast.makeText(this, getString(R.string.google_maps_not_installed), Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    private void animateCameraToShowUserAndStation(StationItem station) {

        if (mCurrentUserLatLng != null) {
            animateCameraToShow(station.getPosition(), mCurrentUserLatLng);
        }
        else{
            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(station.getPosition(), 15));
        }
    }

    private void animateCameraToShow(LatLng _latLng0, LatLng _latLng1){
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        boundsBuilder.include(_latLng0).include(_latLng1);

        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), Utils.dpToPx(66, this)));
    }

    //Callback from pull-to-refresh
    @Override
    public void onRefresh() {

        if (mDownloadWebTask == null){
            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();
        }

    }

    private StationListPagerAdapter getListPagerAdapter(){
        return (StationListPagerAdapter) mStationListViewPager.getAdapter();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        StationItem highlightedStation = getListPagerAdapter().getHighlightedStationForPage(position);

        if (position == StationListPagerAdapter.BIKE_STATIONS){

            mFavoriteToggled = false;
            setupFavoriteFab();

            if (mAppBarLayout != null) {
                mAppBarLayout.setExpanded(true, true);
                getListPagerAdapter().notifyAppBarExpansionForPage(position);
            }

            //TODO: rework landscape layout
            if (mPlacePickerFAB != null)
                mPlacePickerFAB.hide();
            if (mFavoriteToggleFAB != null)
                mFavoriteToggleFAB.hide();

            //just to be on the safe side
            if (highlightedStation != null){
                mStationMapFragment.setPinOnStation(true, highlightedStation.getName());

                animateCameraToShowUserAndStation(highlightedStation);

                mStationMapFragment.lookingForBikes(true);
            }
        } else {

            if (mAppBarLayout != null)
                mAppBarLayout.setExpanded(false , true);

            //TODO: rework landscape layout
            if (mPlacePickerFAB != null)
                mPlacePickerFAB.show();
            if (mFavoriteToggleFAB != null)
                mFavoriteToggleFAB.show();

            if (highlightedStation == null)
                //TODO: Maybe bounds containing all stations accessible for free ? (needs clustering to look good)
                //TODO: Seen mStationMapFragment being null on app resuming
                mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13));
            else
                animateCameraToShow(mStationMapFragment.getMarkerALatLng(), highlightedStation.getPosition());


            mStationMapFragment.lookingForBikes(false);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    //Google API client
    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();

    }

    //Google API client
    @Override
    public void onConnectionSuspended(int i) {

    }

    //Google API client
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        getListPagerAdapter().setCurrentUserLatLng(mCurrentUserLatLng);

        if (mStationMapFragment != null){
            mStationMapFragment.onUserLocationChange(location);
        }
    }

    public class RedrawMarkersTask extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mStatusTextView.setText(getString(R.string.refreshing));
        }

        @Override
        protected Void doInBackground(Boolean... bools) {

            //This improves the UX by giving time to the listview to render
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            mStationMapFragment.clearMarkerGfxData();
            //SETUP MARKERS DATA
            for (StationItem item : mStationsNetwork){
                mStationMapFragment.addMarkerForStationItem(item, bools[0]);
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

            mStationMapFragment.redrawMarkers();

            if (getListPagerAdapter().isViewPagerReady()) {
                StationItem highlighted = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

                if (null != highlighted)
                    mStationMapFragment.setPinOnStation(isLookingForBike(), highlighted.getName());
            }

            mRedrawMarkersTask = null;
        }
    }

    public class FindNetworkTask extends AsyncTask<Void, Void, Map<String,String>> {

        private void checkAndAskLocationPermission(){
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(NearbyActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                //if (ActivityCompat.shouldShowRequestPermissionRationale(NearbyActivity.this,
                //        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                //} else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(NearbyActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            0);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                //}
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            checkAndAskLocationPermission();

            mStatusTextView.setText(getString(R.string.searching_wait_location));

            mClosestBikeAutoSelected = false;

            if (getListPagerAdapter().isViewPagerReady())
                getListPagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            getListPagerAdapter().setRefreshingAll(false);

            mFindNetworkTask = null;
        }

        @Override
        protected Map<String,String> doInBackground(Void... voids) {

            //noinspection StatementWithEmptyBody
            while (!getListPagerAdapter().isViewPagerReady()){
                //Waiting on viewpager init
            }

            publishProgress();

            //noinspection StatementWithEmptyBody
            while (mCurrentUserLatLng == null)
            {
                //Waiting on location
            }

            publishProgress();

            Map<String,String> toReturn = new HashMap<>();

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

                //It seems we don't have a better candidate than the one we're presently using
                if (closestNetwork.id.equalsIgnoreCase(DBHelper.getBikeNetworkId(NearbyActivity.this))){
                    cancel(false);
                }
                else{

                    if (DBHelper.isBikeNetworkIdAvailable(NearbyActivity.this)){
                        toReturn.put("old_network_name", DBHelper.getBikeNetworkName(NearbyActivity.this));
                    }

                    toReturn.put("new_network_city", closestNetwork.location.city);

                    DBHelper.saveBikeNetworkDesc(closestNetwork, NearbyActivity.this);
                }

            } catch (IOException e) {
                Toast toast;

                toast = Toast.makeText(getApplicationContext(),getString(R.string.download_failed),Toast.LENGTH_LONG);

                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                cancel(false); //No need to try to interrupt the thread
            }

            return toReturn;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            if (getListPagerAdapter().isViewPagerReady())
                getListPagerAdapter().setRefreshingAll(true);

            if (mCurrentUserLatLng != null)
                mStatusTextView.setText(getString(R.string.searching_bike_network));

        }

        @Override
        protected void onPostExecute(Map<String,String> backgroundResults) {
            super.onPostExecute(backgroundResults);

            //noinspection ConstantConditions
            getSupportActionBar().setSubtitle(DBHelper.getBikeNetworkName(NearbyActivity.this));

            AlertDialog alertDialog = new AlertDialog.Builder(NearbyActivity.this).create();
            //alertDialog.setTitle(getString(R.string.network_found_title));
            if (!backgroundResults.keySet().contains("old_network_name")) {
                alertDialog.setTitle(R.string.welcome);
                alertDialog.setMessage(Html.fromHtml(String.format(getString(R.string.bike_network_found_message),
                        DBHelper.getBikeNetworkName(NearbyActivity.this), backgroundResults.get("new_network_city"))));
            }
            else{
                //alertDialog.setTitle(R.string.bike_network_change_title);
                alertDialog.setMessage(Html.fromHtml(String.format(getString(R.string.bike_network_change_message),
                        DBHelper.getBikeNetworkName(NearbyActivity.this), backgroundResults.get("new_network_city"))));
                mStationMapFragment.doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(mCurrentUserLatLng, 15), true);
            }

            alertDialog.show();

            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();

            mFindNetworkTask = null;
        }
    }

    //TODO: NOT use an asynchtask for this long running database operation
    public class SaveNetworkToDatabaseTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

            for (StationItem station : mStationsNetwork){
                boundsBuilder.include(station.getPosition());
            }

            DBHelper.saveBikeNetworkBounds(boundsBuilder.build(), NearbyActivity.this);

            //User is not in coverage area, postExecute will launch appropriate task
            if (mCurrentUserLatLng != null && !boundsBuilder.build().contains(mCurrentUserLatLng)){
                return null;
            }

            try {
                //TODO: This strategy of deleting everything and re adding is broken
                //when screen orientation changes happen while it's saving stations
                //Anyone using DBHelper at that time get fed partial results
                //This process shouldn't use an asynctask anyway as it's long running :/
                DBHelper.deleteAllStations();

                for (StationItem station : mStationsNetwork) {
                    DBHelper.saveStation(station);
                }
            } catch (Exception e) {
                Log.d("NearbyActivity", "Error saving network", e );
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (mCurrentUserLatLng != null && !DBHelper.getBikeNetworkBounds(NearbyActivity.this).contains(mCurrentUserLatLng)){

                getListPagerAdapter().removeStationHighlightForPage(mTabLayout.getSelectedTabPosition());

                mFindNetworkTask = new FindNetworkTask();
                mFindNetworkTask.execute();
            }

            //Toast.makeText(context, "DatabaseUpdate Successful!", Toast.LENGTH_LONG).show();
        }
    }

    public class DownloadWebTask extends AsyncTask<Void, Void, Void> {

        private void checkAndAskLocationPermission(){
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(NearbyActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                //if (ActivityCompat.shouldShowRequestPermissionRationale(NearbyActivity.this,
                //        Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //} else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(NearbyActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        0);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
                //}
            }
        }

        @Override
        protected Void doInBackground(Void... aVoid) {

            //noinspection StatementWithEmptyBody
            while (!getListPagerAdapter().isViewPagerReady()){
                //Waiting on viewpager init
            }

            publishProgress();

            Map<String, String> UrlParams = new HashMap<>();
            UrlParams.put("fields", "stations");

            Citybik_esAPI api = ((RootApplication) getApplication()).getCitybik_esApi();

            final Call<NetworkStatusAnswerRoot> call = api.getNetworkStatus(DBHelper.getBikeNetworkHRef(NearbyActivity.this), UrlParams);

            Response<NetworkStatusAnswerRoot> statusAnswer;

            try {
                statusAnswer = call.execute();

                mStationsNetwork.clear();

                for (Station station : statusAnswer.body().network.stations) {
                    StationItem stationItem = new StationItem(station, NearbyActivity.this);
                    mStationsNetwork.add(stationItem);
                }
            } catch (IOException e) {

                cancel(false); //No need to try to interrupt the thread
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            getListPagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mStatusTextView.setText(getString(R.string.downloading));

            //Cannot do that, for some obscure reason, task gets automatically
            //cancelled when the permission dialog is visible
            //checkAndAskLocationPermission();

            mClosestBikeAutoSelected = false;

            if (getListPagerAdapter().isViewPagerReady())
                getListPagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onCancelled (Void aVoid){
            super.onCancelled(aVoid);
            //Set interface back
            getListPagerAdapter().setRefreshingAll(false);

            Toast toast;

            toast = Toast.makeText(getApplicationContext(),getString(R.string.download_failed),Toast.LENGTH_LONG);

            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            //must be done last
            mDownloadWebTask = null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //switch progressbar view visibility

            getListPagerAdapter().setRefreshingAll(false);

            //Removed this Toast as progressBar AND updated textView with time in minutes already convey the idea
            //Maybe have a toast if it was NOT a success
            //Toast.makeText(mContext, R.string.download_success, Toast.LENGTH_SHORT).show();


            DBHelper.saveLastUpdateTimestampAsNow(getApplicationContext());
            mRefreshMarkers = true;
            setupUI();
            Log.d("nearbyFragment", mStationsNetwork.size() + " stations downloaded from citibik.es");

            new SaveNetworkToDatabaseTask().execute();

            checkAndAskLocationPermission();

            //must be done last
            mDownloadWebTask = null;
        }
    }
}

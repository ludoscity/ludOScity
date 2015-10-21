package com.ludoscity.findmybikes.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.ludoscity.findmybikes.StationListPagerAdapter;
import com.ludoscity.findmybikes.citybik_es.Citybik_esAPI;
import com.ludoscity.findmybikes.citybik_es.model.ListNetworksAnswerRoot;
import com.ludoscity.findmybikes.citybik_es.model.NetworkDesc;
import com.ludoscity.findmybikes.citybik_es.model.NetworkStatusAnswerRoot;
import com.ludoscity.findmybikes.citybik_es.model.Station;
import com.ludoscity.findmybikes.fragments.FavoritesFragment;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.RootApplication;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.fragments.StationMapFragment;
import com.ludoscity.findmybikes.utils.Utils;
import com.ludoscity.findmybikes.R;
import java.io.IOException;
import java.util.ArrayList;
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
        FavoritesFragment.OnFragmentInteractionListener,
        SwipeRefreshLayout.OnRefreshListener{

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

    // titles for tabs (indices must correspond to the viewPager)
    private static final int[] TABS_TITLE_RES_ID = new int[]{
            R.string.title_section_nearby,
            R.string.title_section_favorites
    };

    private boolean mRefreshMarkers = true;
    private boolean mLookingForBike = true;

    private MenuItem mParkingSwitch;
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
    }

    @Override
    public void onResume(){

        super.onResume();

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

        setContentView(R.layout.activity_nearby);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        setActivityTitle(getActionbarTitle());
        setActivitySubtitle("");

        restoreActionBar();

        // Update Bar
        mStatusTextView = (TextView) findViewById(R.id.status_textView);
        mStatusBar = findViewById(R.id.statusBar);

        mStationListViewPager = (ViewPager)findViewById(R.id.station_list_viewpager);
        mStationListViewPager.setAdapter(new StationListPagerAdapter(getSupportFragmentManager()));

        // Give the TabLayout the ViewPager
        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mStationListViewPager);

        setStatusBarListener();


        if (savedInstanceState != null) {

            mUnselectStationCameraPosition = savedInstanceState.getParcelable("back_camera_pos");
            mSavedInstanceCameraPosition = savedInstanceState.getParcelable("saved_camera_pos");
            mLookingForBike = savedInstanceState.getBoolean("looking_for_bike");
            mStationsNetwork = savedInstanceState.getParcelableArrayList("network_data");
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

        outState.putParcelable("back_camera_pos", mUnselectStationCameraPosition);
        outState.putParcelable("saved_camera_pos", mStationMapFragment.getCameraPosition());
        outState.putBoolean("looking_for_bike", mLookingForBike);
        outState.putParcelableArrayList("network_data", mStationsNetwork);
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

        mParkingSwitch = menu.findItem(R.id.bike_parking_switch_menu_item);

        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(R.id.action_bar_find_bike_parking_switch)).setChecked(mLookingForBike);

        setupTabTitles();

        setOnClickFindSwitchListener();

        mFavoriteMenuItem = menu.findItem(R.id.favorite_menu_item);

        mDirectionsMenuItem = menu.findItem(R.id.directions_menu_item);

        if (null != retrieveStationListFragment()) {

            StationItem highlightedStation = retrieveStationListFragment().getHighlightedStation();
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

            case R.id.favorite_menu_item:

                StationItem station = retrieveStationListFragment().getHighlightedStation();

                boolean newState = !station.isFavorite(this);

                station.setFavorite(newState, this);

                if (newState) {
                    mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite);
                    Toast.makeText(this, getString(R.string.favorite_added),Toast.LENGTH_SHORT).show();
                }
                else {
                    mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite_outline);
                    Toast.makeText(this, getString(R.string.favorite_removed),Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.directions_menu_item:

                StationItem targetStation = retrieveStationListFragment().getHighlightedStation();

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

        if (null != retrieveStationListFragment().getHighlightedStation())
        {
            if (null != mFavoriteMenuItem)
                mFavoriteMenuItem.setVisible(!isOpen && !isAnimating);
            if (null != mDirectionsMenuItem)
                mDirectionsMenuItem.setVisible(!isOpen && !isAnimating);
        }
    }

    private void setOnClickFindSwitchListener() {
        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(R.id.action_bar_find_bike_parking_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                retrieveStationListFragment().lookingForBikes(isChecked);
                mStationMapFragment.lookingForBikes(isChecked);
                mLookingForBike = isChecked;
                setupTabTitles();
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

        if (mUpdateRefreshRunnableCode == null && mUpdateRefreshHandler != null) {

            mUpdateRefreshRunnableCode = createUpdateRefreshRunnableCode();

            mUpdateRefreshHandler.post(mUpdateRefreshRunnableCode);
        }

        if (DBHelper.isBikeNetworkIdAvailable(this)){

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

                if (null != retrieveStationListFragment()){
                    retrieveStationListFragment().setupUI(mStationsNetwork, mLookingForBike);

                    if (null != mParkingSwitch && !mParkingSwitch.isVisible() && !isNavDrawerOpen())
                        mParkingSwitch.setVisible(true);
                }
            }
        }
    }

    private Runnable createUpdateRefreshRunnableCode(){
        return new Runnable() {

            private boolean mStationListReady = false;

            /*private final long startTime = System.currentTimeMillis();
            private long lastRunTime;
            private long lastUpdateTime = System.currentTimeMillis();   //Update should be run automatically ?
            */
            @Override
            public void run() {

                long now = System.currentTimeMillis();

                if (!mStationListReady && retrieveStationListFragment() != null){
                    retrieveStationListFragment().setupUI(mStationsNetwork, mLookingForBike);
                    mStationListReady = true;
                }

                //Update not already in progress
                if (mStationListReady && mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null) {

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

                        retrieveStationListFragment().setRefreshEnable(true);

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
                    }
                    else{
                        futureStringBuilder.append(getString(R.string.no_connectivity));

                        retrieveStationListFragment().setRefreshEnable(false);
                    }

                    if (mDownloadWebTask == null)
                        mStatusTextView.setText(String.format(getString(R.string.status_string),
                                pastStringBuilder.toString(), futureStringBuilder.toString()));
                }

                //UI will be refreshed every second
                mUpdateRefreshHandler.postDelayed(mUpdateRefreshRunnableCode, 1000);
            }
        };
    }

    private void setStatusBarListener() {
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

            if(retrieveStationListFragment() != null)
                retrieveStationListFragment().setCurrentUserLatLng(mCurrentUserLatLng);
        }
        //Marker click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MARKER_CLICK_PATH)){

            retrieveStationListFragment().highlightStationFromName(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));
            setupFavoriteActionIcon(retrieveStationListFragment().getHighlightedStation());
            mDirectionsMenuItem.setVisible(true);
            mStationMapFragment.oversizeMarkerUniqueForStationName(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));

        }
        //Map click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_CLICK_PATH)){

            StationItem highlightedStation = retrieveStationListFragment().getHighlightedStation();

            if (null != highlightedStation){

                mFavoriteMenuItem.setVisible(false);
                mDirectionsMenuItem.setVisible(false);
                retrieveStationListFragment().removeStationHighlight();
                mStationMapFragment.resetMarkerSizeAll();
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

        if (uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_ITEM_CLICK_PATH))
        {
            //if null, means the station was clicked twice, hence unchecked
            StationItem clickedStation = retrieveStationListFragment().getHighlightedStation();
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

                mUnselectStationCameraPosition = mStationMapFragment.getCameraPosition();

                if (mCurrentUserLatLng != null) {
                    boundsBuilder.include(mCurrentUserLatLng);
                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 200));
                }
                else{
                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(clickedStation.getPosition(), 15));
                }
            }
        }
    }

    private void setupFavoriteActionIcon(StationItem station) {
        if (station.isFavorite(this))
            mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite);
        else
            mFavoriteMenuItem.setIcon(R.drawable.ic_action_action_favorite_outline);

        mFavoriteMenuItem.setVisible(true);
    }

    //Callback from pull-to-refresh
    @Override
    public void onRefresh() {

        if (mDownloadWebTask == null){
            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();
        }

    }

    @Override
    public void onFavoritesFragmentInteraction(StationItem stationToShow) {

    }

    private StationListFragment retrieveStationListFragment(){
        return (StationListFragment)((StationListPagerAdapter) mStationListViewPager.getAdapter()).getRegisteredFragment(StationListPagerAdapter.ALL_STATIONS);
    }

    private void setupTabTitles() {
        String tabTitlePostfix;

        if (mLookingForBike)
            tabTitlePostfix = getString(R.string.bikes);
        else
            tabTitlePostfix = getString(R.string.parking);

        for (int i=0; i<TABS_TITLE_RES_ID.length; ++i){
            //noinspection ConstantConditions
            mTabLayout.getTabAt(i).setText(getString(TABS_TITLE_RES_ID[i]) + " - " + tabTitlePostfix);
        }
    }

    public class RedrawMarkersTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mStatusTextView.setText(getString(R.string.refreshing));
        }

        @Override
        protected Void doInBackground(Void... voids) {

            //This improves the UX by giving time to the listview to render
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            mStationMapFragment.clearMarkerGfxData();
            //SETUP MARKERS DATA
            for (StationItem item : mStationsNetwork){
                mStationMapFragment.addMarkerForStationItem(item, mLookingForBike);
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

            if (null != retrieveStationListFragment()) {
                StationItem highlighted = retrieveStationListFragment().getHighlightedStation();

                if (null != highlighted)
                    mStationMapFragment.oversizeMarkerUniqueForStationName(highlighted.getName());
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

            if (retrieveStationListFragment() != null)
                retrieveStationListFragment().setRefreshing(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            retrieveStationListFragment().setRefreshing(false);

            mFindNetworkTask = null;
        }

        @Override
        protected Map<String,String> doInBackground(Void... voids) {

            //noinspection StatementWithEmptyBody
            while (retrieveStationListFragment() == null){
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

            if (retrieveStationListFragment() != null)
                retrieveStationListFragment().setRefreshing(true);

            if (mCurrentUserLatLng != null)
                mStatusTextView.setText(getString(R.string.searching_bike_network));

        }

        @Override
        protected void onPostExecute(Map<String,String> backgroundResults) {
            super.onPostExecute(backgroundResults);

            setActivityTitle(getActionbarTitle());
            restoreActionBar();

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

    public class SaveNetworkToDatabaseTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

            for (StationItem station : mStationsNetwork){
                boundsBuilder.include(station.getPosition());
            }

            DBHelper.saveBikeNetworkBounds(boundsBuilder.build(), NearbyActivity.this);

            //User is not in coverage are, postExecute will launch appropriate task
            if (mCurrentUserLatLng != null && !boundsBuilder.build().contains(mCurrentUserLatLng)){
                return null;
            }

            try {
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

                retrieveStationListFragment().removeStationHighlight();
                mFavoriteMenuItem.setVisible(false);
                mDirectionsMenuItem.setVisible(false);

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
            while (retrieveStationListFragment() == null){
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

            retrieveStationListFragment().setRefreshing(true);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mStatusTextView.setText(getString(R.string.downloading));

            //Cannot do that, for some obscure reason, task gets automatically
            //cancelled when the permission dialog is visible
            //checkAndAskLocationPermission();

            if (retrieveStationListFragment() != null)
                retrieveStationListFragment().setRefreshing(true);
        }

        @Override
        protected void onCancelled (Void aVoid){
            super.onCancelled(aVoid);
            //Set interface back
            retrieveStationListFragment().setRefreshing(false);

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

            retrieveStationListFragment().setRefreshing(false);

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

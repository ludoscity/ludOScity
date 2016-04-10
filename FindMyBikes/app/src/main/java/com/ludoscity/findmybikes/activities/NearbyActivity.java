package com.ludoscity.findmybikes.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;
import com.ludoscity.findmybikes.Fab;
import com.ludoscity.findmybikes.FavoriteItem;
import com.ludoscity.findmybikes.FavoriteRecyclerViewAdapter;
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
import com.ludoscity.findmybikes.utils.DividerItemDecoration;
import com.ludoscity.findmybikes.utils.ScrollingLinearLayoutManager;
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
        FavoriteRecyclerViewAdapter.OnFavoriteListItemClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        ViewPager.OnPageChangeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private StationMapFragment mStationMapFragment = null;

    private Handler mUpdateRefreshHandler = null;
    private Runnable mUpdateRefreshRunnableCode = null;

    private Interpolator mCircularRevealInterpolator;

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
    private ProgressBar mPlaceAutocompleteLoadingProgressBar;
    private View mTripDetailsWidget;
    private TextView mTripDetailsProximityA;
    private TextView mTripDetailsProximityB;
    private TextView mTripDetailsProximitySearch;
    private TextView mTripDetailsProximityTotal;
    private FrameLayout mTripDetailsSumSeparator;
    private View mTripDetailsBToSearchRow;

    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private FloatingActionButton mDirectionsLocToAFab;
    private FloatingActionButton mSearchFAB;
    private MaterialSheetFab mFavoritesSheetFab;
    private boolean mFavoriteSheetVisible = false;
    private FloatingActionButton mClearFAB;
    private FloatingActionButton mAutoSelectBikeFab;

    private FavoriteRecyclerViewAdapter mFavoriteRecyclerViewAdapter;

    private boolean mRefreshMarkers = true;
    private boolean mRefreshTabs = true;

    private CameraPosition mSavedInstanceCameraPosition;

    private static final int[] TABS_ICON_RES_ID = new int[]{
            R.drawable.ic_pin_a_36dp_white,
            R.drawable.ic_pin_b_36dp_white
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

        mUpdateRefreshRunnableCode = createUpdateRefreshRunnableCode();

        mUpdateRefreshHandler.post(mUpdateRefreshRunnableCode);
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
        } else {
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

        boolean autoCompleteLoadingProgressBarVisible = false;

        if (savedInstanceState != null) {

            mSavedInstanceCameraPosition = savedInstanceState.getParcelable("saved_camera_pos");
            mStationsNetwork = savedInstanceState.getParcelableArrayList("network_data");
            mRequestingLocationUpdates = savedInstanceState.getBoolean("requesting_location_updates");
            mCurrentUserLatLng = savedInstanceState.getParcelable("user_location_latlng");
            mClosestBikeAutoSelected = savedInstanceState.getBoolean("closest_bike_auto_selected");
            mFavoriteSheetVisible = savedInstanceState.getBoolean("favorite_sheet_visible");
            autoCompleteLoadingProgressBarVisible = savedInstanceState.getBoolean("place_autocomplete_loading");
            mRefreshTabs = savedInstanceState.getBoolean("refresh_tabs");
        }

        setContentView(R.layout.activity_nearby);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(getString(R.string.app_name));
        getSupportActionBar().setSubtitle(DBHelper.getBikeNetworkName(this));

        // Update Bar
        mStatusTextView = (TextView) findViewById(R.id.status_textView);
        mStatusBar = findViewById(R.id.app_status_bar);

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

        mTripDetailsWidget = findViewById(R.id.trip_details);
        mTripDetailsProximityA = (TextView) findViewById(R.id.trip_details_proximity_a);
        mTripDetailsProximityB = (TextView) findViewById(R.id.trip_details_proximity_b);
        mTripDetailsProximitySearch = (TextView) findViewById(R.id.trip_details_proximity_search);
        mTripDetailsProximityTotal = (TextView) findViewById(R.id.trip_details_proximity_total);
        mTripDetailsSumSeparator = (FrameLayout) findViewById(R.id.trip_details_sum_separator);
        mTripDetailsBToSearchRow = findViewById(R.id.trip_details_b_to_search);

        mSearchFAB = (FloatingActionButton) findViewById(R.id.search_fab);
        mDirectionsLocToAFab = (FloatingActionButton) findViewById(R.id.directions_loc_to_a_fab);
        mPlaceAutocompleteLoadingProgressBar = (ProgressBar) findViewById(R.id.place_autocomplete_loading);
        if (autoCompleteLoadingProgressBarVisible)
            mPlaceAutocompleteLoadingProgressBar.setVisibility(View.VISIBLE);

        setupDirectionsLocToAFab();
        setupSearchFab();
        setupFavoriteFab();
        setupClearFab();
        setupAutoselectBikeFab();

        setStatusBarClickListener();

        getListPagerAdapter().setCurrentUserLatLng(mCurrentUserLatLng);

        setupFavoriteSheet();

        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_loc_to_a).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchGoogleMapsForDirections(mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), true);
            }
        });
        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_a_to_b).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchGoogleMapsForDirections(mStationMapFragment.getMarkerALatLng(), mStationMapFragment.getMarkerBVisibleLatLng(), false);
            }
        });
        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_b_to_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchGoogleMapsForDirections(mStationMapFragment.getMarkerBVisibleLatLng(), mStationMapFragment.getMarkerPickedPlaceVisibleLatLng(), true);
            }
        });

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

        mCircularRevealInterpolator = AnimationUtils.loadInterpolator(this, R.interpolator.msf_interpolator);
    }

    private void setupAutoselectBikeFab() {
        mAutoSelectBikeFab = (FloatingActionButton) findViewById(R.id.autoselect_closest_bike);

        mAutoSelectBikeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mClosestBikeAutoSelected = false;
            }
        });
    }

    private void setupDirectionsLocToAFab() {
        mDirectionsLocToAFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StationItem curSelectedStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);

                // Seen NullPointerException in crash report.
                if (null != curSelectedStation) {

                    LatLng tripLegOrigin = isLookingForBike() ? mCurrentUserLatLng : mStationMapFragment.getMarkerALatLng();
                    LatLng tripLegDestination = curSelectedStation.getPosition();
                    boolean walkMode = isLookingForBike();

                    launchGoogleMapsForDirections(tripLegOrigin, tripLegDestination, walkMode);
                }
            }
        });
    }

    private void setupSearchFab() {

        mSearchFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mPlaceAutocompleteLoadingProgressBar.getVisibility() != View.GONE)
                    return;

                try {

                    Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .setBoundsBias(DBHelper.getBikeNetworkBounds(NearbyActivity.this,
                                    NearbyActivity.this.getResources().getInteger(R.integer.average_biking_speed_kmh)))
                            .build(NearbyActivity.this);

                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);


                    mFavoritesSheetFab.hideSheetThenFab();
                    mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(NearbyActivity.this, R.color.light_gray));

                    mPlaceAutocompleteLoadingProgressBar.setVisibility(View.VISIBLE);

                    getListPagerAdapter().hideEmptyString(StationListPagerAdapter.DOCK_STATIONS);

                } catch (GooglePlayServicesRepairableException e) {
                    Log.d("mPlacePickerFAB onClick", "oops", e);
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.d("mPlacePickerFAB onClick", "oops", e);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){

            mPlaceAutocompleteLoadingProgressBar.setVisibility(View.GONE);
            mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(NearbyActivity.this, R.color.theme_primary_dark));

            if (resultCode == RESULT_OK){
                mSearchFAB.hide();
                final Place place = PlaceAutocomplete.getPlace(this, data);

                final Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (NearbyActivity.this.getListPagerAdapter().isViewPagerReady()) {
                            setupBTabSelectionClosestDock(place);
                        }
                        else
                            handler.postDelayed(this,10);
                    }
                },50);
            } else {

                mFavoritesSheetFab.showFab();
                mSearchFAB.show();

                getListPagerAdapter().showEmptyString(StationListPagerAdapter.DOCK_STATIONS);
            }
        }
    }

    private void setupFavoriteSheet() {

        RecyclerView favoriteRecyclerView = (RecyclerView) findViewById(R.id.favorites_sheet_recyclerview);

        favoriteRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        favoriteRecyclerView.setLayoutManager(new ScrollingLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false, 300));

        mFavoriteRecyclerViewAdapter = new FavoriteRecyclerViewAdapter(this, this);

        ArrayList<FavoriteItem> favoriteList = DBHelper.getFavoriteItems(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());
        mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);
        favoriteRecyclerView.setAdapter(mFavoriteRecyclerViewAdapter);
    }

    private void setupFavoriteListFeedback(boolean _noFavorite) {
        if (_noFavorite){
            ((TextView)findViewById(R.id.empty_favorite_list_text)).setText(
                    Html.fromHtml(String.format(getResources().getString(R.string.no_favorite), DBHelper.getBikeNetworkName(this))));
            findViewById(R.id.empty_favorite_list_text).setVisibility(View.VISIBLE);
            findViewById(R.id.favorites_sheet_content).setVisibility(View.GONE);
        }
        else{
            ((TextView)findViewById(R.id.favorites_sheet_header)).setText(
                    Html.fromHtml(String.format(getResources().getString(R.string.favorites_sheet_header), DBHelper.getBikeNetworkName(this))));
            findViewById(R.id.empty_favorite_list_text).setVisibility(View.GONE);
            findViewById(R.id.favorites_sheet_content).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("saved_camera_pos", mStationMapFragment.getCameraPosition());
        outState.putParcelableArrayList("network_data", mStationsNetwork);
        outState.putBoolean("requesting_location_updates", mRequestingLocationUpdates);
        outState.putParcelable("user_location_latlng", mCurrentUserLatLng);
        outState.putBoolean("closest_bike_auto_selected", mClosestBikeAutoSelected);
        outState.putBoolean("favorite_sheet_visible", mFavoriteSheetVisible);
        outState.putBoolean("place_autocomplete_loading", mPlaceAutocompleteLoadingProgressBar.getVisibility() == View.VISIBLE);
        outState.putBoolean("refresh_tabs", mRefreshTabs);
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
        ArrayList<FavoriteItem> favoriteList = DBHelper.getFavoriteItems(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());
        mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);

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
        ArrayList<FavoriteItem> favoriteList = DBHelper.getFavoriteItems(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());
        mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);

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

    private void setupFavoriteFab() {

        Fab mFavoritePickerFAB = (Fab) findViewById(R.id.favorite_picker_fab);
        View sheetView = findViewById(R.id.fab_sheet);
        View overlay = findViewById(R.id.overlay);
        int sheetColor = ContextCompat.getColor(this, R.color.cardview_light_background);
        int fabColor = ContextCompat.getColor(this, R.color.theme_primary_dark);

        //Caused by: java.lang.NullPointerException (sheetView)
        // Create material sheet FAB
        mFavoritesSheetFab = new MaterialSheetFab<>(mFavoritePickerFAB, sheetView, overlay, sheetColor, fabColor);

        mFavoritesSheetFab.setEventListener(new MaterialSheetFabEventListener() {
            @Override
            public void onShowSheet() {

                mSearchFAB.hide();
                mFavoriteSheetVisible = true;
            }

            @Override
            public void onSheetHidden() {
                if (!isLookingForBike() && mStationMapFragment.getMarkerBVisibleLatLng() == null)
                    mSearchFAB.show();

                mFavoriteSheetVisible = false;
            }
        });
    }

    private void setupClearFab() {
        mClearFAB = (FloatingActionButton) findViewById(R.id.clear_fab);

        mClearFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStationMapFragment.setMapPaddingLeft(0);
                mStationMapFragment.setMapPaddingRight(0);
                hideTripDetailsWidget();
                clearBTab();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mFavoritesSheetFab.isSheetVisible()) {
            mFavoritesSheetFab.hideSheet();
        } else if(mStationMapFragment.getMarkerBVisibleLatLng() != null){

            mStationMapFragment.setMapPaddingLeft(0);
            if (!isLookingForBike())
                mStationMapFragment.setMapPaddingRight(0);
            hideTripDetailsWidget();
            clearBTab();

        } else {
            super.onBackPressed();
        }
    }

    private void stopUIRefresh() {
        if (mUpdateRefreshHandler != null) {
            mUpdateRefreshHandler.removeCallbacks(mUpdateRefreshRunnableCode);
            mUpdateRefreshRunnableCode = null;
            mUpdateRefreshHandler = null;
        }
    }

    private void refreshMap(){

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
            }
        }
    }

    private void setupTabPages() {
        //Tab A
        getListPagerAdapter().setupUI(StationListPagerAdapter.BIKE_STATIONS, mStationsNetwork, "", mCurrentUserLatLng, mCurrentUserLatLng);

        LatLng stationBLatLng = mStationMapFragment.getMarkerBVisibleLatLng();
        //Tab B
        if (stationBLatLng == null)
            getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(), getString(R.string.tab_b_instructions), null, null);
        else
            setupBTabSelection(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.DOCK_STATIONS).getId(), isLookingForBike());

        mRefreshTabs = false;
    }

    //TODO : This clearly turned into spaghetti. At least extract methods.
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

                if ( mRedrawMarkersTask == null && getListPagerAdapter().isViewPagerReady() &&
                        (!mPagerReady || mRefreshTabs ) ){
                    //TODO: calling DBHelper.getFavoriteStations can return incomplete result when db is updating
                    //it happens when data is downladed and screen configuration is changed shortly after
                    //When restoring, we don't need to setup everything rom here
                    if (!mStationMapFragment.isRestoring()) {
                        setupTabPages();
                        if(isLookingForBike())  //onPageSelected is called by framework on B tab restoration
                            onPageSelected(StationListPagerAdapter.BIKE_STATIONS);
                    }

                    mPagerReady = true;
                }

                if ( DBHelper.isDataCorrupted(NearbyActivity.this) && mPagerReady && mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null){
                    mDownloadWebTask = new DownloadWebTask();
                    mDownloadWebTask.execute();
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
                        if (!mSearchFAB.isEnabled()) {
                            mSearchFAB.setEnabled(true);
                            mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(NearbyActivity.this, R.color.theme_primary_dark));
                            mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));
                        }

                        if (DBHelper.isBikeNetworkIdAvailable(getApplicationContext())) {

                            if (difference >= NearbyActivity.this.getApplicationContext().getResources().getInteger(R.integer.outdated_data_warning_time_min) * 60 * 1000)
                                mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this,R.color.theme_accent));
                            else
                                mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));

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
                        mSearchFAB.setEnabled(false);
                        mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(NearbyActivity.this, R.color.light_gray));
                        mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this,R.color.theme_accent));
                    }

                    if (mDownloadWebTask == null)
                        mStatusTextView.setText(String.format(getString(R.string.status_string),
                                pastStringBuilder.toString(), futureStringBuilder.toString()));

                    //pulling the trigger on auto select
                    if (mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null &&
                            !mClosestBikeAutoSelected &&
                            getListPagerAdapter().isRecyclerViewReadyForItemSelection(StationListPagerAdapter.BIKE_STATIONS)){

                        getListPagerAdapter().highlightClosestStationWithAvailability(true);
                        getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.BIKE_STATIONS, isAppBarExpanded());
                        StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
                        mStationMapFragment.setPinOnStation(true, closestBikeStation.getId());

                        if (isLookingForBike()) {
                            if (mTripDetailsWidget.getVisibility() == View.INVISIBLE) {
                                mDirectionsLocToAFab.show();
                            }

                            mAutoSelectBikeFab.hide();
                            mStationMapFragment.setMapPaddingRight(0);

                            animateCameraToShowUserAndStation(closestBikeStation);
                        }

                        mClosestBikeAutoSelected = true;
                    }

                    //Checking if station is closest bike
                    if (mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null){

                        if (!isStationAClosestBike()){
                            if (mStationMapFragment.getMarkerBVisibleLatLng() == null){
                                //TODO: this piece of code fights with the user if no station B is selected
                                //Solution : implement occasional / regular modes and have the setting impact this
                                mClosestBikeAutoSelected = false;
                            }
                            else if (isLookingForBike() && mAutoSelectBikeFab.getVisibility() != View.VISIBLE) {
                                mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                                mAutoSelectBikeFab.show();
                                animateCameraToShowUserAndStation(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS));
                            }
                        }
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
            refreshMap();
        }
        //Marker click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MARKER_CLICK_PATH)){

            if (isLookingForBike()) {
                if (mAppBarLayout != null)
                    mAppBarLayout.setExpanded(false, true);

                if (getListPagerAdapter().highlightStationForPage(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM),
                        StationListPagerAdapter.BIKE_STATIONS)) {

                    getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.BIKE_STATIONS, isAppBarExpanded());

                    mStationMapFragment.setPinOnStation(true,
                            uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));

                    if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                        getListPagerAdapter().notifyStationAUpdate(mStationMapFragment.getMarkerALatLng());
                        hideSetupShowTripDetailsWidget();
                    }
                }
            }
            else {
                //B Tab, looking for dock
                final String clickedStationId = uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM);
                setupBTabSelection(clickedStationId, false);
            }
        }
        //Map click
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_CLICK_PATH)){

        }
    }

    private void setupBTabSelectionClosestDock(final Place _from){
        setupBTabSelection(null, _from, false);
    }

    private void setupBTabSelection(final String _selectedStationId, boolean _silent){
        setupBTabSelection(_selectedStationId, null, _silent);
    }

    //Both _selectedStationId and _targetDestination shouldn't be null at the same time
    // silent parameter is used when the UI shouldn't be impacted
    private void setupBTabSelection(final String _selectedStationId, final Place _targetDestination, final boolean _silent) {
        //Remove any previous selection
        getListPagerAdapter().removeStationHighlightForPage(StationListPagerAdapter.DOCK_STATIONS);

        //Silent parameter is ignored because widget needs refresh
        //TODO: find a more elegant solution than this damn _silent boolean, which is a hackfix - probably a refactor by splitting method in pieces
        //and call them independently as required from client
        if (mTripDetailsWidget.getVisibility() == View.INVISIBLE){
            mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));
            setupTripDetailsWidget();
            showTripDetailsWidget();
        }
        else{
            hideSetupShowTripDetailsWidget();
        }


        if (!mStationMapFragment.isPickedPlaceMarkerVisible()) {

            LatLng sortRef = _targetDestination != null ? _targetDestination.getLatLng() : getLatLngForStation(_selectedStationId);
            //Replace recyclerview content
            getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, mStationsNetwork, "",
                    sortRef, mStationMapFragment.getMarkerALatLng());

            if (!_silent) {
                mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                mClearFAB.show();
                mFavoritesSheetFab.hideSheetThenFab();
                mSearchFAB.hide();
            }

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (getListPagerAdapter().isRecyclerViewReadyForItemSelection(StationListPagerAdapter.DOCK_STATIONS)) {
                        //highlight B station in list
                        if (_selectedStationId != null) {
                            getListPagerAdapter().highlightStationForPage(_selectedStationId, StationListPagerAdapter.DOCK_STATIONS);
                            if (!_silent)
                                mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));
                        } else {
                            mStationMapFragment.setPinOnStation(false, getListPagerAdapter().highlightClosestStationWithAvailability(false));
                            if(!_silent)
                                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                                        _targetDestination.getLatLng(),
                                        mStationMapFragment.getMarkerBVisibleLatLng(),
                                        null);
                        }

                        getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.DOCK_STATIONS, isAppBarExpanded());
                    } else
                        handler.postDelayed(this, 10);
                }
            }, 10);
        }
        else {

            if (!_silent)
                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                        getLatLngForStation(_selectedStationId),
                        mStationMapFragment.getMarkerPickedPlaceVisibleLatLng(),
                        null);

            getListPagerAdapter().highlightStationForPage(_selectedStationId, StationListPagerAdapter.DOCK_STATIONS);
            getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.DOCK_STATIONS, isAppBarExpanded());
        }

        if (_selectedStationId != null) {
            mStationMapFragment.setPinOnStation(false, _selectedStationId);
        }
        else
            mStationMapFragment.setPinForPickedPlace(_targetDestination.getName().toString(),
                    _targetDestination.getLatLng(), _targetDestination.getAttributions());
    }

    //Assumption here is that there is an A and a B station selected (or soon will be)
    private void setupTripDetailsWidget() {

        final Handler handler = new Handler();    //Need to wait for list selection

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.DOCK_STATIONS) != null) {
                    boolean totalOver1h = false;

                    int locToAMinutes = 0;
                    int AToBMinutes = 0;
                    int BToSearchMinutes = 0;

                    StationItem selectedStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
                    String rawProximityString = selectedStation.getProximityStringFromLatLng(mCurrentUserLatLng,
                            false, getResources().getInteger(R.integer.average_walking_speed_kmh), NearbyActivity.this);//getListPagerAdapter().getSelectedStationProximityStringForPage(StationListPagerAdapter.BIKE_STATIONS);

                    String formattedProximityString = getTripDetailsWdigetFormattedString(rawProximityString);
                    if (formattedProximityString.startsWith(">"))
                        totalOver1h = true;
                    else if (!formattedProximityString.startsWith("<"))
                        locToAMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                    mTripDetailsProximityA.setText(formattedProximityString);


                    selectedStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.DOCK_STATIONS);
                    rawProximityString = selectedStation.getProximityStringFromLatLng(getListPagerAdapter().getDistanceDisplayReferenceForPage(StationListPagerAdapter.DOCK_STATIONS),
                            false, getResources().getInteger(R.integer.average_biking_speed_kmh), NearbyActivity.this);//getListPagerAdapter().getSelectedStationProximityStringForPage(StationListPagerAdapter.DOCK_STATIONS);

                    formattedProximityString = getTripDetailsWdigetFormattedString(rawProximityString);
                    if (formattedProximityString.startsWith(">"))
                        totalOver1h = true;
                    else if (!formattedProximityString.startsWith("<"))
                        AToBMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                    mTripDetailsProximityB.setText(formattedProximityString);


                    if (mStationMapFragment.getMarkerPickedPlaceVisibleLatLng() == null) {
                        mTripDetailsBToSearchRow.setVisibility(View.GONE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_a_to_b);
                    } else {

                        rawProximityString = selectedStation.getProximityStringFromLatLng(mStationMapFragment.getMarkerPickedPlaceVisibleLatLng(),
                                false, getResources().getInteger(R.integer.average_walking_speed_kmh), NearbyActivity.this);
                        formattedProximityString = getTripDetailsWdigetFormattedString(rawProximityString);

                        if (formattedProximityString.startsWith(">"))
                            totalOver1h = true;
                        else if (!formattedProximityString.startsWith("<"))
                            BToSearchMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                        mTripDetailsProximitySearch.setText(formattedProximityString);

                        mTripDetailsBToSearchRow.setVisibility(View.VISIBLE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_b_to_search);
                    }

                    int total = 0;

                    if (!totalOver1h) {
                        total = locToAMinutes + AToBMinutes + BToSearchMinutes;
                        if (total >= 60)
                            totalOver1h = true;
                    }

                    if (totalOver1h)
                        mTripDetailsProximityTotal.setText("> 1h");
                    else
                        mTripDetailsProximityTotal.setText("~" + total + getResources().getString(R.string.min));

                } else
                    handler.postDelayed(this, 10);
            }
        }, 10);
    }

    private String getTripDetailsWdigetFormattedString(String _poximityString){

        String toReturn;

        if (_poximityString.length() == 5)   //'~1min' .. '~9min'
            toReturn = "~0" + _poximityString.substring(1); //'~01min' .. '~09min'
        else
            toReturn = _poximityString;

        return toReturn;
    }

    //For reusable Animators (which most Animators are, apart from the one-shot animator produced by createCircularReveal()
    private Animator buildTripDetailsWidgetAnimators(boolean _show, long _duration, float _minRadiusMultiplier){

        float minRadiusMultiplier = Math.min(1.f, _minRadiusMultiplier);

        Animator toReturn = null;

        // Use native circular reveal on Android 5.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // Native circular reveal uses coordinates relative to the view
            int revealStartX = 0;
            int revealStartY = mTripDetailsWidget.getHeight();

            float radiusMax = (float)Math.hypot(mTripDetailsWidget.getHeight(), mTripDetailsWidget.getWidth());
            float radiusMin = radiusMax * minRadiusMultiplier;

            if (_show)
            {
                toReturn = ViewAnimationUtils.createCircularReveal(mTripDetailsWidget, revealStartX,
                                revealStartY, radiusMin, radiusMax);
            } else {
                toReturn = ViewAnimationUtils.createCircularReveal(mTripDetailsWidget, revealStartX,
                        revealStartY, radiusMax, radiusMin);
            }

            toReturn.setDuration(_duration);
            toReturn.setInterpolator(mCircularRevealInterpolator);
        }

        return toReturn;
    }

    private void showTripDetailsWidget(){

        mTripDetailsWidget.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            buildTripDetailsWidgetAnimators(true, 850, 0).start();
        }
    }

    private void hideSetupShowTripDetailsWidget(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Animator hideAnimator = buildTripDetailsWidgetAnimators(false, 850/2, .23f);

            hideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    setupTripDetailsWidget();
                    buildTripDetailsWidgetAnimators(true, 850/2, .23f).start();
                }
            });

            hideAnimator.start();
        }
        else{
            setupTripDetailsWidget();
        }
    }

    private void hideTripDetailsWidget(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animator hideAnimator = buildTripDetailsWidgetAnimators(false, 850, 0);
            // make the view invisible when the animation is done
            hideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mTripDetailsWidget.setVisibility(View.INVISIBLE);
                }
            });
            hideAnimator.start();
        }
        else
            mTripDetailsWidget.setVisibility(View.INVISIBLE);
    }

    private void clearBTab(){
        getListPagerAdapter().removeStationHighlightForPage(StationListPagerAdapter.DOCK_STATIONS);

        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(), getString(R.string.tab_b_instructions), null, null);

        mStationMapFragment.clearMarkerB();
        mStationMapFragment.clearMarkerPickedPlace();

        if (!isLookingForBike()) {
            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13));
            mFavoritesSheetFab.showFab();
            mSearchFAB.show();
            mClearFAB.hide();
        }
        else{
            StationItem highlightedStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
            animateCameraToShowUserAndStation(highlightedStation);
        }
    }

    private boolean isLookingForBike() {
        return mStationListViewPager.getCurrentItem() == StationListPagerAdapter.BIKE_STATIONS;
    }

    private LatLng getLatLngForStation(String _stationId){
        LatLng toReturn = null;

        for(StationItem station : mStationsNetwork){
            if (station.getId().equalsIgnoreCase(_stationId)){
                toReturn = station.getPosition();
                break;
            }
        }

        return toReturn;
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

            if (isLookingForBike()) {

                if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                    getListPagerAdapter().notifyStationAUpdate(mStationMapFragment.getMarkerALatLng());
                }

                animateCameraToShowUserAndStation(clickedStation);

                mStationMapFragment.setPinOnStation(true, clickedStation.getId());
            }
            else
                setupBTabSelection(clickedStation.getId(), false);
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
            //http://stackoverflow.com/questions/6205827/how-to-open-standard-google-map-application-from-my-application

            final StationItem curSelectedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

            // Seen NullPointerException in crash report.
            if (null != curSelectedStation) {

                LatLng tripLegOrigin = isLookingForBike() ? mCurrentUserLatLng : mStationMapFragment.getMarkerALatLng();
                LatLng tripLegDestination = curSelectedStation.getPosition();
                boolean walkMode = isLookingForBike();

                launchGoogleMapsForDirections(tripLegOrigin, tripLegDestination, walkMode);
            }
        }
    }

    private void launchGoogleMapsForDirections(LatLng _origin, LatLng _destination, boolean _walking) {
        StringBuilder builder = new StringBuilder("http://maps.google.com/maps?&saddr=");

        builder.append(_origin.latitude).
                append(",").
                append(_origin.longitude);

        builder.append("&daddr=").
            append(_destination.latitude).
            append(",").
            append(_destination.longitude).
            //append("B"). Labeling doesn't work :'(
            append("&dirflg=");

        if (_walking)
            builder.append("w");
        else
            builder.append("b");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(builder.toString()));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
            startActivity(intent); // launch the map activity
        } else {
            //TODO: replace by Snackbar
            Toast.makeText(this, getString(R.string.google_maps_not_installed), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAppBarExpanded(){
        return mAppBarLayout.getHeight() - mAppBarLayout.getBottom() == 0;
    }

    private void animateCameraToShowUserAndStation(StationItem station) {

        if (mCurrentUserLatLng != null) {
            if (mTripDetailsWidget.getVisibility() != View.VISIBLE) //Directions to A fab is visible
                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_fab_padding), station.getPosition(), mCurrentUserLatLng, null);
            else    //Map id padded on the left and interface is clear on the right
                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_ab_pin_padding), station.getPosition(), mCurrentUserLatLng, null);

        }
        else{
            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(station.getPosition(), 15));
        }
    }

    private void animateCameraToShow(int _cameraPaddingPx, LatLng _latLng0, LatLng _latLng1, LatLng _latLng2){
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        boundsBuilder.include(_latLng0).include(_latLng1);

        if (_latLng2 != null)
            boundsBuilder.include(_latLng2);

        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), _cameraPaddingPx)); //Pin icon is 36 dp
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
    public void onPageSelected(final int position) {

        //Happens on screen orientation change
        if (mStationMapFragment == null ||
                (mStationMapFragment.getMarkerBVisibleLatLng() != null && getListPagerAdapter().getHighlightedStationForPage(position) == null)){
            Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onPageSelected(position);

                }
            }, 1000); //second long delay gives a nice UX with camera animation
        }
        else {

            StationItem highlightedStation = getListPagerAdapter().getHighlightedStationForPage(position);

            //A TAB
            if (position == StationListPagerAdapter.BIKE_STATIONS) {

                mStationMapFragment.setScrollGesturesEnabled(false);

                if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                    mStationMapFragment.setMapPaddingLeft(0);
                    hideTripDetailsWidget();
                    mDirectionsLocToAFab.show();
                }

                mAppBarLayout.setExpanded(true, true);
                getListPagerAdapter().smoothScrollHighlightedInViewForPage(position, true);

                mSearchFAB.hide();
                mFavoritesSheetFab.hideSheetThenFab();
                mClearFAB.hide();
                mStationMapFragment.setMapPaddingRight(0);

                if (!isStationAClosestBike()){
                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                    mAutoSelectBikeFab.show();
                }

                //just to be on the safe side
                if (highlightedStation != null ) {

                    mStationMapFragment.setPinOnStation(true, highlightedStation.getId());

                    animateCameraToShowUserAndStation(highlightedStation);

                    mStationMapFragment.lookingForBikes(true);
                }
            } else { //B TAB

                mAutoSelectBikeFab.hide();
                mStationMapFragment.setMapPaddingRight(0);

                mStationMapFragment.setScrollGesturesEnabled(true);

                mAppBarLayout.setExpanded(false, true);

                if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13.75f));

                    if (mFavoriteSheetVisible)
                        mFavoritesSheetFab.showSheet();
                    else if (mPlaceAutocompleteLoadingProgressBar.getVisibility() != View.GONE){
                        mFavoritesSheetFab.hideSheetThenFab();
                        mSearchFAB.show();
                        mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_gray));
                    }
                    else {
                        mDirectionsLocToAFab.hide();
                        mFavoritesSheetFab.showFab();
                        mSearchFAB.show();
                    }

                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));

                } else {

                    getListPagerAdapter().smoothScrollHighlightedInViewForPage(position, false);
                    mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));

                    if (mTripDetailsWidget.getVisibility() == View.INVISIBLE) {
                        showTripDetailsWidget();
                    }

                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                    mClearFAB.show();

                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));
                }

                mStationMapFragment.lookingForBikes(false);
            }
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

        boolean highlightedStationAVisibleInRecyclerViewBefore =
                getListPagerAdapter().isHighlightedVisibleInRecyclerView();

        getListPagerAdapter().setCurrentUserLatLng(mCurrentUserLatLng);

        if (mStationMapFragment != null){
            mStationMapFragment.onUserLocationChange(location);
            if (mStationMapFragment.getMarkerBVisibleLatLng() != null && mTripDetailsWidget.getVisibility() == View.VISIBLE)
                setupTripDetailsWidget();
        }

        if (highlightedStationAVisibleInRecyclerViewBefore && !getListPagerAdapter().isHighlightedVisibleInRecyclerView())
            getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.BIKE_STATIONS, true);

    }

    private boolean isStationAClosestBike(){

        LatLng ALatLng = mStationMapFragment.getMarkerALatLng();
        LatLng closestBikeLatLng = getListPagerAdapter().getClosestBikeLatLng();

        return closestBikeLatLng != null &&
                ALatLng.latitude == closestBikeLatLng.latitude &&
                ALatLng.longitude == closestBikeLatLng.longitude;
    }

    @Override
    public void onFavoriteListItemClick(String _stationID) {
        mStationMapFragment.clearMarkerPickedPlace();
        setupBTabSelection(_stationID, false);
    }

    @Override
    public void onFavoristeListItemEditDone(String _stationId, String _newName) {
        DBHelper.updateFavorite(true, _stationId, _newName, this);
        mFavoriteRecyclerViewAdapter.setupFavoriteList(DBHelper.getFavoriteItems(this));
    }

    public class RedrawMarkersTask extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mStatusTextView.setText(getString(R.string.refreshing));
            mStationMapFragment.hideAllStations();
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
            mStationMapFragment.showAllStations();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mStationMapFragment.redrawMarkers();

            if (getListPagerAdapter().isViewPagerReady()) {
                StationItem highlighted = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

                if (null != highlighted)
                    mStationMapFragment.setPinOnStation(isLookingForBike(), highlighted.getId());
            }

            mRedrawMarkersTask = null;
            mStationMapFragment.showAllStations();
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

            mClosestBikeAutoSelected = false;

            //noinspection ConstantConditions
            getSupportActionBar().setSubtitle(DBHelper.getBikeNetworkName(NearbyActivity.this));
            setupFavoriteSheet();

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

            mStationMapFragment.clearMarkerB();

            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();

            mFindNetworkTask = null;
        }
    }

    //TODO: NOT use an asynchtask for this long running database operation
    public class SaveNetworkToDatabaseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            DBHelper.notifyBeginSavingStations(NearbyActivity.this);
        }


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
                //TODO: This is ugly.
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

            DBHelper.notifyEndSavingStations(NearbyActivity.this);

            if (mCurrentUserLatLng != null && !DBHelper.getBikeNetworkBounds(NearbyActivity.this, 0).contains(mCurrentUserLatLng)){

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
            mRefreshTabs = true;
            refreshMap();
            Log.d("nearbyFragment", mStationsNetwork.size() + " stations downloaded from citibik.es");

            new SaveNetworkToDatabaseTask().execute();

            checkAndAskLocationPermission();

            //must be done last
            mDownloadWebTask = null;
        }
    }
}

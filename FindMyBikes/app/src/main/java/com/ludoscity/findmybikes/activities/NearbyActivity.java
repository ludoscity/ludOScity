package com.ludoscity.findmybikes.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
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
import com.ludoscity.findmybikes.StationRecyclerViewAdapter;
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
import java.util.List;
import java.util.Map;

import de.psdev.licensesdialog.LicensesDialog;
import retrofit2.Call;
import retrofit2.Response;
import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

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
    private UpdateTwitterStatusTask mUpdateTwitterTask = null;

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

    private static int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static int SETTINGS_REQUEST_CODE = 2;
    private FloatingActionButton mDirectionsLocToAFab;
    private FloatingActionButton mSearchFAB;
    private MaterialSheetFab mFavoritesSheetFab;
    private boolean mFavoriteSheetVisible = false;
    private FloatingActionButton mClearFAB;
    private Fab mFavoritePickerFAB;
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

    private boolean mOnboardingInProgress = false;
    Snackbar mOnboardingSnackbar; //indefinite snackbar have buggy behavior on older platform if we let the framework dismiss them
    Snackbar mFindBikesSnackbar;

    @Override
    public void onStart() {

        mGoogleApiClient.connect();

        if (Utils.Connectivity.isConnected(getApplicationContext()) && !DBHelper.isBikeNetworkIdAvailable(this)) {

            mFindNetworkTask = new FindNetworkTask(DBHelper.getBikeNetworkName(this));
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
        getSupportActionBar().setTitle(Html.fromHtml(String.format(getResources().getString(R.string.appbar_title_formatting),
                getResources().getString(R.string.appbar_title_prefix),
                DBHelper.getHashtaggableNetworkName(this),
                getResources().getString(R.string.appbar_title_postfix))));
        //doesn't scale well, but just a little touch for my fellow Montréalers
        String city_hashtag = "";
        String bikeNetworkCity = DBHelper.getBikeNetworkCity(this);
        if (bikeNetworkCity.contains(", QC")){
            city_hashtag = " @mtlvi";
        }
        String hastagedEnhanced_bikeNetworkCity = bikeNetworkCity + city_hashtag;
        getSupportActionBar().setSubtitle(Html.fromHtml(String.format(getResources().getString(R.string.appbar_subtitle_formatted), hastagedEnhanced_bikeNetworkCity)));

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

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.snackbar_coordinator);

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

        if (!mClosestBikeAutoSelected){
            mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_finding,
                    Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(this, R.color.theme_primary_dark));
            mFindBikesSnackbar.show();
        }

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

        //Flipping this bool gives way to bike auto selection and found Snackbar animation
        //TODO : BonPlatDePates. Spaghetti monster must be contained.
        //In need an FSM of some kind. States being A selected Y/N B selected Y/N ....
        //TODO: Think about it more
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

                    getListPagerAdapter().hideStationRecap(StationListPagerAdapter.DOCK_STATIONS);

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

                getListPagerAdapter().showStationRecap(StationListPagerAdapter.DOCK_STATIONS);
            }
        } else if (requestCode == SETTINGS_REQUEST_CODE){

            getListPagerAdapter().highlightStationforId(true, Utils.extractClosestAvailableStationIdFromProcessedString(getListPagerAdapter().retrieveClosestRawIdAndAvailability(true)));
            getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.BIKE_STATIONS, isAppBarExpanded());

            mRefreshMarkers = true;
            refreshMap();
            mRefreshTabs = true;
        }
    }

    private void setupFavoriteSheet() {

        //ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
        //        ItemTouchHelper.LEFT) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback( 0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder favViewHolder = (FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder)viewHolder;

                removeFavorite(getStation(favViewHolder.getStationId()), true);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);

        RecyclerView favoriteRecyclerView = (RecyclerView) findViewById(R.id.favorites_sheet_recyclerview);

        favoriteRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        favoriteRecyclerView.setLayoutManager(new ScrollingLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false, 300));

        mFavoriteRecyclerViewAdapter = new FavoriteRecyclerViewAdapter(this, this);

        ArrayList<FavoriteItem> favoriteList = DBHelper.getFavoriteItems(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());
        mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);
        favoriteRecyclerView.setAdapter(mFavoriteRecyclerViewAdapter);

        itemTouchHelper.attachToRecyclerView(favoriteRecyclerView);
    }

    private void setupFavoriteListFeedback(boolean _noFavorite) {
        if (_noFavorite){
            ((TextView)findViewById(R.id.favorites_sheet_header)).setText(
                    Html.fromHtml(String.format(getResources().getString(R.string.no_favorite), DBHelper.getBikeNetworkName(this))));
        }
        else{
            ((TextView)findViewById(R.id.favorites_sheet_header)).setText(
                    Html.fromHtml(String.format(getResources().getString(R.string.favorites_sheet_header), DBHelper.getBikeNetworkName(this))));
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
                startActivityForResult(settingsIntent, SETTINGS_REQUEST_CODE);
                return true;
            case R.id.legal_notices_menu_item:
                new LicensesDialog.Builder(this)
                        .setNotices(R.raw.notices)
                        .build()
                        .show();
                return true;

            case R.id.privacy_policy_menu_item:
                Intent implicit = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/f8full/ludOScity/blob/master/FindMyBikes/Privacy_policy.md"));
                startActivity(implicit);
                return true;

            case R.id.source_code_menu_item:
                Intent implicit2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/f8full/ludOScity/tree/master/FindMyBikes"));
                startActivity(implicit2);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    //TODO: refactor add and remove favorite
    private void removeFavorite(final StationItem _station, boolean _showUndo) {
        _station.setFavorite(false, this);
        ArrayList<FavoriteItem> favoriteList = DBHelper.getFavoriteItems(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());
        mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);

        //To setup correct name
        StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
        getListPagerAdapter().setupBTabStationARecap(closestBikeStation);

        if (!_showUndo) {

            if (!mOnboardingInProgress) {

                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_removed,
                        Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                        .show();
            } else {

                mOnboardingSnackbar.dismiss();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        mOnboardingSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_onboarding_complete,
                                Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));

                        mOnboardingSnackbar.show();
                    }
                }, 500);
            }
        }
        else{
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_removed,
                    Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        addFavorite(_station, false);
                        getListPagerAdapter().setupBTabStationARecap(_station);
                    }
                }).show();
        }
    }

    private void addFavorite(final StationItem _station, boolean _showUndo) {
        _station.setFavorite(true, this);
        ArrayList<FavoriteItem> favoriteList = DBHelper.getFavoriteItems(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());
        mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);

        //To setup correct name
        StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
        getListPagerAdapter().setupBTabStationARecap(closestBikeStation);

        //getListPagerAdapter().addStationForPage(StationListPagerAdapter.DOCK_STATIONS, _station);

        if (!_showUndo) {
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_added,
                    Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                 .show();
        }
        else {
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_added, Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        removeFavorite(_station, false);
                        getListPagerAdapter().setupBTabStationARecap(_station);
                    }
                }).show();
        }
    }

    private void setupFavoriteFab() {

        mFavoritePickerFAB = (Fab) findViewById(R.id.favorite_picker_fab);

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

                if (mOnboardingInProgress) {

                    mOnboardingSnackbar.dismiss();

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_complete,
                                    Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent))
                                    .show();
                        }
                    }, 500);

                    mOnboardingInProgress = false;
                }

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

        //TAB A
        getListPagerAdapter().setupUI(StationListPagerAdapter.BIKE_STATIONS, mStationsNetwork, "", mCurrentUserLatLng, mCurrentUserLatLng);

        LatLng stationBLatLng = mStationMapFragment.getMarkerBVisibleLatLng();

        if (stationBLatLng == null) {
            //TAB B
            getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(), getString(R.string.b_tab_question), null, null);

            //TAB A
            getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, false);
        }
        else
        //Crash if B is selected and user changed cities (not a typical use case)
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
                    //When restoring, we don't need to setup everything from here
                    if (!mStationMapFragment.isRestoring()) { //TODO figure out how to properly determine restoration
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
                            mFindNetworkTask = new FindNetworkTask(DBHelper.getBikeNetworkName(NearbyActivity.this));
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

                        //Requesting raw string with availability
                        String rawClosest = getListPagerAdapter().retrieveClosestRawIdAndAvailability(true);
                        getListPagerAdapter().highlightStationforId(true, Utils.extractClosestAvailableStationIdFromProcessedString(rawClosest));

                        getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.BIKE_STATIONS, isAppBarExpanded());
                        final StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
                        mStationMapFragment.setPinOnStation(true, closestBikeStation.getId());
                        getListPagerAdapter().setupBTabStationARecap(closestBikeStation);

                        if (isLookingForBike()) {
                            if (mTripDetailsWidget.getVisibility() == View.INVISIBLE) {
                                mDirectionsLocToAFab.show();
                            }

                            mAutoSelectBikeFab.hide();
                            mStationMapFragment.setMapPaddingRight(0);

                            if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                                mStationListViewPager.setCurrentItem(StationListPagerAdapter.DOCK_STATIONS, true);

                                mFavoritesSheetFab.showFab();

                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        if (!mFavoritePickerFAB.isShowRunning()){
                                            mFavoritesSheetFab.showSheet();
                                        }
                                        else
                                            handler.postDelayed(this, 10);
                                    }
                                }, 50);
                            }
                            else {
                                animateCameraToShowUserAndStation(closestBikeStation);
                            }
                        }

                        //Bug on older API levels. Dismissing by hand fixes it.
                        // First biggest bug happened here. Putting defensive code
                        //TODO: investigate how state is maintained, Snackbar is destroyed by framework on screen orientation change
                        //TODO: Refactor this spgathetti is **TOP** priosity
                        //and probably long background state.
                        if (mFindBikesSnackbar != null){

                            mFindBikesSnackbar.dismiss();
                        }

                        Handler handler = new Handler();

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                if(!closestBikeStation.isLocked() && closestBikeStation.getFree_bikes() > DBHelper.getCriticalAvailabilityMax(NearbyActivity.this)) {

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_found,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.snackbar_green));
                                    mFindBikesSnackbar.show();
                                }
                                else{

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_error,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));
                                    mFindBikesSnackbar.show();
                                }
                            }
                        }, 500);

                        if (mOnboardingInProgress){
                            //Adding onboarding Favorite
                            StationItem stationA = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);

                            for(StationItem station : mStationsNetwork) {
                                if (!station.getId().equalsIgnoreCase(stationA.getId())) {

                                    //station.setFavorite(true, NearbyActivity.this); //We want to manipulate everything, hence go directly to DBHelper
                                    DBHelper.updateFavorite(true, station.getId(), getResources().getString(R.string.onboarding_favorite_name), false, NearbyActivity.this);
                                    ArrayList<FavoriteItem> favoriteList = DBHelper.getFavoriteItems(NearbyActivity.this);
                                    setupFavoriteListFeedback(favoriteList.isEmpty());
                                    mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);

                                    if (mFindBikesSnackbar != null){

                                        mFindBikesSnackbar.dismiss();
                                    }

                                    Handler snackbarHandler = new Handler();
                                    snackbarHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mOnboardingSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_start,
                                                    Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));
                                            mOnboardingSnackbar.show();
                                        }
                                    }, 500);

                                    break;
                                }
                            }
                        }

                        mClosestBikeAutoSelected = true;
                        //launch twitter task if not already running, pass it the raw String
                        if ( Utils.Connectivity.isConnected(getApplicationContext()) && //data network available
                                mUpdateTwitterTask == null &&   //not already tweeting
                                rawClosest.length() > 32 + StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length() && //there was trouble
                                difference < NearbyActivity.this.getApplicationContext().getResources().getInteger(R.integer.outdated_data_warning_time_min) * 60 * 1000){ //data is fresh enough

                            mUpdateTwitterTask = new UpdateTwitterStatusTask(mStationsNetwork);
                            mUpdateTwitterTask.execute(rawClosest);

                        }
                    }

                    //Checking if station is closest bike
                    if (mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null){

                        if (!isStationAClosestBike()){
                            if (mStationMapFragment.getMarkerBVisibleLatLng() == null){
                                mClosestBikeAutoSelected = false;
                            }
                            else if (isLookingForBike() && !mClosestBikeAutoSelected) {
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
        else if ( uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MARKER_CLICK_PATH)){

            if(isLookingForBike() && mStationMapFragment.getMarkerBVisibleLatLng() != null ||
                    !isLookingForBike()) {

                if (isLookingForBike()) {

                    if (getListPagerAdapter().highlightStationForPage(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM),
                            StationListPagerAdapter.BIKE_STATIONS)) {

                        getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.BIKE_STATIONS, isAppBarExpanded());

                        mStationMapFragment.setPinOnStation(true,
                                uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));
                        getListPagerAdapter().setupBTabStationARecap(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS));

                        if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                            LatLng newALatLng = mStationMapFragment.getMarkerALatLng();
                            getListPagerAdapter().notifyStationAUpdate(newALatLng);
                            hideSetupShowTripDetailsWidget();

                            if ( (getListPagerAdapter().getClosestBikeLatLng().latitude != newALatLng.latitude) &&
                                    (getListPagerAdapter().getClosestBikeLatLng().longitude != newALatLng.longitude) ){

                                mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                                mAutoSelectBikeFab.show();
                                animateCameraToShowUserAndStation(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS));

                            }
                        }
                    }
                } else {

                    if (mAppBarLayout != null)
                        mAppBarLayout.setExpanded(false, true);

                    //B Tab, looking for dock
                    final String clickedStationId = uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM);
                    setupBTabSelection(clickedStationId, false);
                }
            } else {

                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.please_answer_first, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                        .show();

                mStationListViewPager.setCurrentItem(StationListPagerAdapter.DOCK_STATIONS, true);
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
                            String stationId = Utils.extractClosestAvailableStationIdFromProcessedString(getListPagerAdapter().retrieveClosestRawIdAndAvailability(false));

                            getListPagerAdapter().hideStationRecap(StationListPagerAdapter.DOCK_STATIONS);
                            mStationMapFragment.setPinOnStation(false, stationId);
                            getListPagerAdapter().highlightStationForPage(stationId, StationListPagerAdapter.DOCK_STATIONS);
                            getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, true);
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
            getListPagerAdapter().hideStationRecap(StationListPagerAdapter.DOCK_STATIONS);
            mStationMapFragment.setPinOnStation(false, _selectedStationId);
            getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, true);
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

            buildTripDetailsWidgetAnimators(true, getResources().getInteger(R.integer.camera_animation_duration), 0).start();
        }
    }

    private void hideSetupShowTripDetailsWidget(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Animator hideAnimator = buildTripDetailsWidgetAnimators(false, getResources().getInteger(R.integer.camera_animation_duration) / 2, .23f);

            hideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    setupTripDetailsWidget();
                    buildTripDetailsWidgetAnimators(true, getResources().getInteger(R.integer.camera_animation_duration) / 2, .23f).start();
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
            Animator hideAnimator = buildTripDetailsWidgetAnimators(false, getResources().getInteger(R.integer.camera_animation_duration), 0);
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

        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(), getString(R.string.b_tab_question), null, null);

        mStationMapFragment.clearMarkerB();
        mStationMapFragment.clearMarkerPickedPlace();

        //A TAB
        getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, false);

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

    private StationItem getStation(String _stationId){
        StationItem toReturn = null;

        for(StationItem station : mStationsNetwork){
            if (station.getId().equalsIgnoreCase(_stationId)){
                toReturn = station;
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
            if(isLookingForBike() && mStationMapFragment.getMarkerBVisibleLatLng() != null ||
                    !isLookingForBike()) {
                //if null, means the station was clicked twice, hence unchecked
                final StationItem clickedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

                if (isLookingForBike()) {

                    if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {

                        LatLng newALatLng = mStationMapFragment.getMarkerALatLng();
                        getListPagerAdapter().notifyStationAUpdate(newALatLng);

                        if ( (getListPagerAdapter().getClosestBikeLatLng().latitude != newALatLng.latitude) &&
                                (getListPagerAdapter().getClosestBikeLatLng().longitude != newALatLng.longitude) ){

                            mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                            mAutoSelectBikeFab.show();
                            animateCameraToShowUserAndStation(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS));

                        }


                        hideSetupShowTripDetailsWidget();
                    }

                    animateCameraToShowUserAndStation(clickedStation);

                    mStationMapFragment.setPinOnStation(true, clickedStation.getId());
                    getListPagerAdapter().setupBTabStationARecap(clickedStation);
                } else
                    setupBTabSelection(clickedStation.getId(), false);
            }
        }
        else if(uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_INACTIVE_ITEM_CLICK_PATH)){

            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.please_answer_first, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();

            mStationListViewPager.setCurrentItem(StationListPagerAdapter.DOCK_STATIONS, true);
        }
        else if (uri.getPath().equalsIgnoreCase("/"+ StationListFragment.STATION_LIST_FAVORITE_FAB_CLICK_PATH) ||
                uri.getPath().equalsIgnoreCase("/"+ StationListFragment.STATION_LIST_STATION_RECAP_FAVORITE_FAB_CLICK_PATH)){


            StationItem clickedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

            boolean showUndo = false;


            if (uri.getPath().equalsIgnoreCase("/"+ StationListFragment.STATION_LIST_STATION_RECAP_FAVORITE_FAB_CLICK_PATH)){

                clickedStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
                showUndo = true;
            }

            if (null != clickedStation) {

                boolean newState = !clickedStation.isFavorite(this);

                if (newState) {
                    addFavorite(clickedStation, showUndo);
                } else {
                    removeFavorite(clickedStation, showUndo);
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
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.google_maps_not_installed,
                    Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();
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

        StationItem stationA = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);

        if (stationA != null)
            getListPagerAdapter().setupBTabStationARecap(stationA);

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

                mAppBarLayout.setExpanded(false, true);
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

                //TODO: Should I lock that for regular users ?
                mStationMapFragment.setScrollGesturesEnabled(true);

                mAppBarLayout.setExpanded(true, true);

                if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13.75f));

                    if (mFavoriteSheetVisible && !mFavoritesSheetFab.isSheetVisible())
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

        String stationAId = mStationMapFragment.getMarkerAStationId();
        String closestBikeId = Utils.extractClosestAvailableStationIdFromProcessedString(getListPagerAdapter().retrieveClosestRawIdAndAvailability(true));

        return stationAId.equalsIgnoreCase(closestBikeId);
    }

    @Override
    public void onFavoriteListItemClick(String _stationID) {

        if (!mOnboardingInProgress) {
            StationItem stationA = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);

            if (stationA.getId().equalsIgnoreCase(_stationID)) {

                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.such_short_trip, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                        .show();

            } else {
                mStationMapFragment.clearMarkerPickedPlace();
                setupBTabSelection(_stationID, false);
            }
        }
        else{
            mOnboardingSnackbar.dismiss();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mOnboardingSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_onboarding_start,
                            Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));

                    mOnboardingSnackbar.show();
                }
            }, 500);

            mStationMapFragment.clearMarkerPickedPlace();
            setupBTabSelection(_stationID, false);
        }
    }

    @Override
    public void onFavoristeListItemEditDone(String _stationId, String _newName) {
        DBHelper.updateFavorite(true, _stationId, _newName, false, this);
        mFavoriteRecyclerViewAdapter.setupFavoriteList(DBHelper.getFavoriteItems(this));
        StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
        getListPagerAdapter().setupBTabStationARecap(closestBikeStation);
        getListPagerAdapter().notifyRecyclerViewDatasetChangedForAllPages();
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

        private static final String NEW_YORK_HUDSON_BIKESHARE_ID = "hudsonbikeshare-hoboken" ;
        String mOldBikeNetworkName = "";

        public FindNetworkTask(String _currentNetworkName){ mOldBikeNetworkName = _currentNetworkName; }

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

                if (closestNetwork.id.equalsIgnoreCase(NEW_YORK_HUDSON_BIKESHARE_ID)){
                    closestNetwork = answerList.get(1);
                }

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

                DBHelper.pauseAutoUpdate();

                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.download_failed,
                        Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.resume, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DBHelper.resumeAutoUpdate();
                            }
                        }).show();

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
                alertDialog.setTitle(Html.fromHtml(String.format(getResources().getString(R.string.hello_city), "", backgroundResults.get("new_network_city") )));
                alertDialog.setMessage(Html.fromHtml(String.format(getString(R.string.bike_network_found_message),
                        DBHelper.getBikeNetworkName(NearbyActivity.this) )));
                Message toPass = null; //To resolve ambiguous call
                //noinspection ConstantConditions
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), toPass);

                mOnboardingInProgress = true;
            }
            else{
                alertDialog.setTitle(Html.fromHtml(String.format(getResources().getString(R.string.hello_city), getResources().getString(R.string.hello_travel), backgroundResults.get("new_network_city"))));
                alertDialog.setMessage(Html.fromHtml(String.format(getString(R.string.bike_network_change_message),
                        DBHelper.getBikeNetworkName(NearbyActivity.this), mOldBikeNetworkName)));
                Message toPass = null; //To resolve ambiguous call
                //noinspection ConstantConditions
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), toPass);
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

                mFindNetworkTask = new FindNetworkTask(DBHelper.getBikeNetworkName(NearbyActivity.this));
                mFindNetworkTask.execute();
            }
        }
    }

    public class UpdateTwitterStatusTask extends AsyncTask<String, Void, Void>{

        private static final int NEW_STATUS_STATION_NAME_MAX_LENGTH = 29;
        private static final int REPLY_STATION_NAME_MAX_LENGTH = 54;

        private Map<String, StationItem> mTrustedEfficientMap;

        public UpdateTwitterStatusTask(List<StationItem> _stationsNetwork){

            mTrustedEfficientMap = new HashMap<>();

            //first, build an efficient map of stations from stationnetwork
            //because I don't trust the DBHelper.getStationForId just yet
            //TODO: reowrk database code to use versioning
            for (StationItem station : mStationsNetwork){
                mTrustedEfficientMap.put(station.getId(), station);
            }
        }

        @Override
        protected Void doInBackground(String... params) {

            Twitter api = ((RootApplication) getApplication()).getTwitterApi();


            //Extract all stations from raw string
            //if only one station, call updateStatus with intended one and selected one + deduplication
            //if multiple stations, post selected one first and then all other in replies
            //////////////////////////////////////////////////////////////////////////////////////
            //FORMAT -- ROOT STATUS
            /*#findmybixibikes bike is not closest! Bikes:X BAD at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID ~XXmin walk stationnamestationnamestation deduplicateZ
              #findmybixibikes bike is not closest! Bikes:XX AOK at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID ~XXmin walk stationnamestationnamestatio deduplicateZ

              -- REPLIES
              #findmybixibikes discarded closer! Bikes:Y CRI at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID stationnamestationnamestationnamestationnamestationnam
              #findmybixibikes discarded closer! Bikes:Y LCK at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID stationnamestationnamestationnamestationnamestationnam

              */
            String systemHashtag = getResources().getString(R.string.appbar_title_prefix) +
                    DBHelper.getHashtaggableNetworkName(NearbyActivity.this) + //must be hastagable
                    getResources().getString(R.string.appbar_title_postfix);
            int selectedNbBikes = -1;
            String selectedBadorAok = "BAD";    //hashtagged
            String selectedStationId = "";
            String selectedProximityString = "XXmin";
            String selectedStationName = "Laurier / De Lanaudière";
            int newStatusStationNameMaxLength = NEW_STATUS_STATION_NAME_MAX_LENGTH;
            String deduplicate = "deduplicate";    //hashtagged

            //Pair of station id and availability code (always 'CRI' as of now)
            List<Pair<String,String>> discardedStations = new ArrayList<>();

            StationItem selectedStation = null;
            List<String> extracted = Utils.extractOrderedStationIdsFromProcessedString(params[0]);


            for (String e : extracted)
            {
                //e could be either : f132843c3c740cce6760167985bc4d17AVAILABILITY_BAD_ (selected station)
                //or AVAILABILITY_CRI_92d97d6adec177649b366c36f3e8e2ff for subsequent discarded stations

                if (e.indexOf(StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE) != 0){
                    //f132843c3c740cce6760167985bc4d17AVAILABILITY_BAD_ or
                    //f132843c3c740cce6760167985bc4d17AVAILABILITY_AOK_

                    selectedStationId = e.substring(0,32);
                    selectedBadorAok = e.substring(32 + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length() ,
                            32 + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length() + 3); //'BAD' oe 'AOK'

                    selectedStation = mTrustedEfficientMap.get(selectedStationId);

                    selectedNbBikes = selectedStation.getFree_bikes();

                    selectedProximityString = selectedStation.getProximityStringFromLatLng(mCurrentUserLatLng,
                            false, getResources().getInteger(R.integer.average_walking_speed_kmh), NearbyActivity.this);


                    int maxStationNameIdx = 138 - (deduplicate.length()+" ".length()
                            + " walk ".length()
                            + selectedProximityString.length()
                            + " ".length()
                            + 32
                            + " at ".length()
                            + selectedBadorAok.length()
                            + " #".length()
                            + Integer.toString(selectedNbBikes).length()
                            + " bike is not closest! Bikes:".length()
                            + systemHashtag.length());

                    selectedStationName = selectedStation.getName().substring(0, Math.min(selectedStation.getName().length(), maxStationNameIdx));
                }
                else { //AVAILABILITY_CRI_92d97d6adec177649b366c36f3e8e2ff

                    Pair<String, String> discarded = new Pair<>(e.substring(e.length()-32), e.substring(
                            StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length(),
                            StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length() + 3
                    ));

                    discardedStations.add(discarded);



                }

            }

            int deduplicateCounter = 0;

            deduplicate = deduplicate + deduplicateCounter;

            String newStatusString = String.format(getResources().getString(R.string.twitter_not_closest_bike_data_format),
                    systemHashtag, selectedNbBikes, selectedBadorAok, selectedStationId, selectedProximityString, selectedStationName, deduplicate);

            StatusUpdate newStatus = new StatusUpdate(newStatusString);
            //noinspection ConstantConditions
            newStatus.displayCoordinates(true).location(new GeoLocation(selectedStation.getPosition().latitude, selectedStation.getPosition().longitude));

            boolean deduplicationDone = false;


            while (!deduplicationDone){

                //post status before adding replies
                try {
                    //can be interrupted here (duplicate)
                    twitter4j.Status answerStatus = api.updateStatus(newStatus);

                    long replyToId = answerStatus.getId();

                    List<StatusUpdate> replies = new ArrayList<>();

                    for (Pair<String, String> discarded : discardedStations ){
                        StationItem discardedStationItem = mTrustedEfficientMap.get(discarded.first);

                        String replyStatusString = String.format(getResources().getString(R.string.twitter_closer_discarded_reply_data_format),
                                systemHashtag, discardedStationItem.getFree_bikes(), discarded.second, discarded.first,
                                discardedStationItem.getName().substring(0, Math.min(discardedStationItem.getName().length(), REPLY_STATION_NAME_MAX_LENGTH)));

                        StatusUpdate replyStatus = new StatusUpdate(replyStatusString);

                        replyStatus.inReplyToStatusId(replyToId)
                                .displayCoordinates(true)
                                .location(new GeoLocation(discardedStationItem.getPosition().latitude, discardedStationItem.getPosition().longitude));

                        //that can also raise exception
                        api.updateStatus(replyStatus);

                    }

                    deduplicationDone = true;


                } catch (TwitterException e) {
                    String errorMessage = e.getErrorMessage();
                    if (errorMessage.contains("Status is a duplicate.")){
                        ++deduplicateCounter;

                        deduplicate = "deduplicate" + deduplicateCounter;

                        newStatusString = String.format(getResources().getString(R.string.twitter_not_closest_bike_data_format),
                                systemHashtag, selectedNbBikes, selectedBadorAok, selectedStationId, selectedProximityString, selectedStationName, deduplicate);

                        newStatus = new StatusUpdate(newStatusString);
                        //noinspection ConstantConditions
                        newStatus.displayCoordinates(true).location(new GeoLocation(selectedStation.getPosition().latitude, selectedStation.getPosition().longitude));

                        Log.d("TwitterUpdate", "TwitterUpdate duplication -- deduplicating now", e);

                    } else {
                        deduplicationDone = true;
                    }
                    String message = e.getMessage();

                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mUpdateTwitterTask = null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            mUpdateTwitterTask = null;
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

            DBHelper.resumeAutoUpdate();

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

            DBHelper.pauseAutoUpdate();

            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.download_failed,
                    Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                    .setAction(R.string.resume, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DBHelper.resumeAutoUpdate();
                        }
                    }).show();

            //must be done last
            mDownloadWebTask = null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //switch progressbar view visibility

            getListPagerAdapter().setRefreshingAll(false);
            mClosestBikeAutoSelected = false;

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

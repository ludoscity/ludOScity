package com.ludoscity.findmybikes.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
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
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;
import com.ludoscity.findmybikes.EditableMaterialSheetFab;
import com.ludoscity.findmybikes.Fab;
import com.ludoscity.findmybikes.FavoriteItemBase;
import com.ludoscity.findmybikes.FavoriteItemPlace;
import com.ludoscity.findmybikes.FavoriteItemStation;
import com.ludoscity.findmybikes.FavoriteRecyclerViewAdapter;
import com.ludoscity.findmybikes.ItemTouchHelperAdapter;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.RootApplication;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationListPagerAdapter;
import com.ludoscity.findmybikes.StationRecyclerViewAdapter;
import com.ludoscity.findmybikes.citybik_es.Citybik_esAPI;
import com.ludoscity.findmybikes.citybik_es.model.ListNetworksAnswerRoot;
import com.ludoscity.findmybikes.citybik_es.model.NetworkDesc;
import com.ludoscity.findmybikes.citybik_es.model.NetworkStatusAnswerRoot;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.fragments.StationMapFragment;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.DividerItemDecoration;
import com.ludoscity.findmybikes.utils.ScrollingLinearLayoutManager;
import com.ludoscity.findmybikes.utils.Utils;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
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
        FavoriteRecyclerViewAdapter.OnFavoriteListItemClickListener,//TODO: investigate making the sheet listening and forwarding
        FavoriteRecyclerViewAdapter.OnFavoriteListItemStartDragListener,//TODO: investigate making the sheet listening and forwarding
        EditableMaterialSheetFab.OnFavoriteSheetEventListener,
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
    private View mTripDetailsBToDestinationRow;
    private View mTripDetailsPinSearch;
    private View mTripDetailsPinFavorite;
    private View mSplashScreen;
    private TextView mSplashScreenText;

    private ItemTouchHelper mFavoriteItemTouchHelper;

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int SETTINGS_ACTIVITY_REQUEST_CODE = 2;
    private static final int CHECK_GPS_REQUEST_CODE = 3;
    private static final int CHECK_PERMISSION_REQUEST_CODE = 4;
    private FloatingActionButton mDirectionsLocToAFab;
    private FloatingActionButton mSearchFAB;
    private FloatingActionButton mAddFavoriteFAB;
    private EditableMaterialSheetFab mFavoritesSheetFab;
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

    private boolean mFavoriteItemEditInProgress = false;

    private Snackbar mFindBikesSnackbar;

    private ShowcaseView mOnboardingShowcaseView = null;
    private Snackbar mOnboardingSnackBar = null;    //Used to display hints

    //Places favorites stressed the previous design
    //TODO: explore refactoring with the following considerations
    //-stop relying on mapfragment markers visibility to branch code
    private boolean mFavoritePicked = false;    //True from the moment a favorite is picked until it's cleared
    //also set to true when a place is converted to a favorite
    //-consider moving it to some central location (pager adapter also has a copy)
    private boolean mDataOutdated = false;

    @Override
    public void onStart() {

        mGoogleApiClient.connect();

        checkAndAskLocationPermission();

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

    private void checkAndAskLocationPermission(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            mSplashScreenText.setText(R.string.location_please);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

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
        //So that Utils::getBitmapDescriptor works on API < 21
        //when doing Drawable vectorDrawable = ResourcesCompat.getDrawable(ctx.getResources(), id, null);
        //see https://medium.com/@chrisbanes/appcompat-v23-2-age-of-the-vectors-91cbafa87c88#.i8luinewc
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setTheme(R.style.FindMyBikesTheme); //https://developer.android.com/topic/performance/launch-time.html
        super.onCreate(savedInstanceState);

        boolean autoCompleteLoadingProgressBarVisible = false;
        String showcaseTripTotalPlaceName = null;

        if (savedInstanceState != null) {

            mSavedInstanceCameraPosition = savedInstanceState.getParcelable("saved_camera_pos");
            mRequestingLocationUpdates = savedInstanceState.getBoolean("requesting_location_updates");
            mCurrentUserLatLng = savedInstanceState.getParcelable("user_location_latlng");
            mClosestBikeAutoSelected = savedInstanceState.getBoolean("closest_bike_auto_selected");
            mFavoriteSheetVisible = savedInstanceState.getBoolean("favorite_sheet_visible");
            autoCompleteLoadingProgressBarVisible = savedInstanceState.getBoolean("place_autocomplete_loading");
            mRefreshTabs = savedInstanceState.getBoolean("refresh_tabs");
            mFavoritePicked = savedInstanceState.getBoolean("favorite_picked");
            mDataOutdated = savedInstanceState.getBoolean("data_outdated");
            showcaseTripTotalPlaceName = savedInstanceState.getString("onboarding_showcase_trip_total_place_name", null);
        }

        setContentView(R.layout.activity_nearby);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));
        setupActionBarStrings();


        // Update Bar
        mStatusTextView = (TextView) findViewById(R.id.status_textView);
        mStatusBar = findViewById(R.id.app_status_bar);

        if(mDataOutdated)
            mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));

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
            //noinspection ConstantConditions
            mTabLayout.getTabAt(i).setIcon(ContextCompat.getDrawable(this,TABS_ICON_RES_ID[i]));
        }

        mAppBarLayout = (AppBarLayout) findViewById(R.id.action_toolbar_layout);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.snackbar_coordinator);
        mSplashScreen = findViewById(R.id.splashscreen);
        mSplashScreenText = (TextView)findViewById(R.id.splashscreen_text);

        mTripDetailsWidget = findViewById(R.id.trip_details);
        mTripDetailsProximityA = (TextView) findViewById(R.id.trip_details_proximity_a);
        mTripDetailsProximityB = (TextView) findViewById(R.id.trip_details_proximity_b);
        mTripDetailsProximitySearch = (TextView) findViewById(R.id.trip_details_proximity_search);
        mTripDetailsProximityTotal = (TextView) findViewById(R.id.trip_details_proximity_total);
        mTripDetailsSumSeparator = (FrameLayout) findViewById(R.id.trip_details_sum_separator);
        mTripDetailsBToDestinationRow = findViewById(R.id.trip_details_b_to_search);
        mTripDetailsPinSearch = findViewById(R.id.trip_details_to_search);
        mTripDetailsPinFavorite = findViewById(R.id.trip_details_to_favorite);

        mSearchFAB = (FloatingActionButton) findViewById(R.id.search_fab);
        mAddFavoriteFAB = (FloatingActionButton) findViewById(R.id.favorite_add_remove_fab);
        mDirectionsLocToAFab = (FloatingActionButton) findViewById(R.id.directions_loc_to_a_fab);
        mPlaceAutocompleteLoadingProgressBar = (ProgressBar) findViewById(R.id.place_autocomplete_loading);
        if (autoCompleteLoadingProgressBarVisible)
            mPlaceAutocompleteLoadingProgressBar.setVisibility(View.VISIBLE);

        if (showcaseTripTotalPlaceName != null){
            setupShowcaseTripTotal();
            mOnboardingShowcaseView.setContentText(String.format(getString(R.string.onboarding_showcase_total_time_text),
                    DBHelper.getBikeNetworkName(this), showcaseTripTotalPlaceName));
            mOnboardingShowcaseView.setTag(showcaseTripTotalPlaceName);
        }

        if(savedInstanceState == null)
            mSplashScreen.setVisibility(View.VISIBLE);

        setupDirectionsLocToAFab();
        setupSearchFab();
        setupFavoritePickerFab();
        if (savedInstanceState != null && savedInstanceState.getParcelable("add_favorite_fab_data") != null){
            setupAddFavoriteFab((FavoriteItemPlace)savedInstanceState.getParcelable("add_favorite_fab_data"));
            mAddFavoriteFAB.show();
        }
        setupClearFab();
        setupAutoselectBikeFab();

        setStatusBarClickListener();

        getListPagerAdapter().setCurrentUserLatLng(mCurrentUserLatLng);

        setupFavoriteSheet();

        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_loc_to_a).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOnboardingShowcaseView != null){
                    mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
                }
                launchGoogleMapsForDirections(mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), true);
            }
        });
        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_a_to_b).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOnboardingShowcaseView != null){
                    mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
                }
                launchGoogleMapsForDirections(mStationMapFragment.getMarkerALatLng(), mStationMapFragment.getMarkerBVisibleLatLng(), false);
            }
        });
        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_b_to_destination).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOnboardingShowcaseView != null){
                    mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
                }
                if (mStationMapFragment.isPickedPlaceMarkerVisible())
                    launchGoogleMapsForDirections(mStationMapFragment.getMarkerBVisibleLatLng(), mStationMapFragment.getMarkerPickedPlaceVisibleLatLng(), true);
                else //Either Place marker or Favorite marker is visible, but not both at once
                    launchGoogleMapsForDirections(mStationMapFragment.getMarkerBVisibleLatLng(), mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng(), true);
            }
        });
        findViewById(R.id.trip_details_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Je serai à la station Bixi Hutchison/beaubien dans ~15min ! Partagé via #findmybikes
                //I will be at the Bixi station Hutchison/beaubien in ~15min ! Shared via #findmybikes
                String message = String.format(getResources().getString(R.string.trip_details_share_message_content),
                        DBHelper.getBikeNetworkName(getApplicationContext()), getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.DOCK_STATIONS).getName(),
                        mTripDetailsProximityTotal.getText().toString());

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.trip_details_share_title)));
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

        setupLocationRequest();

        mCircularRevealInterpolator = AnimationUtils.loadInterpolator(this, R.interpolator.msf_interpolator);

        //Not empty if RootApplication::onCreate got database data
        if(RootApplication.getBikeNetworkStationList().isEmpty()){

            tryInitialSetup();
        }
    }

    private void tryInitialSetup(){

        if (Utils.Connectivity.isConnected(this)) {
            mSplashScreenText.setText(getString(R.string.auto_bike_select_finding));

            if (DBHelper.isBikeNetworkIdAvailable(this)) {

                mDownloadWebTask = new DownloadWebTask();
                mDownloadWebTask.execute();

                Log.i("nearbyActivity", "No stationList data in RootApplication but bike network id available in DBHelper- launching first download");
            }
            else{

                mFindNetworkTask = new FindNetworkTask(DBHelper.getBikeNetworkName(this));
                mFindNetworkTask.execute();
            }
        }
        else{
            Utils.Snackbar.makeStyled(mSplashScreen, R.string.connectivity_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                    .setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            tryInitialSetup();
                        }
                    }).show();
        }
    }

    private void setupActionBarStrings() {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(Utils.fromHtml(String.format(getResources().getString(R.string.appbar_title_formatting),
                getResources().getString(R.string.appbar_title_prefix),
                DBHelper.getHashtaggableNetworkName(this),
                getResources().getString(R.string.appbar_title_postfix))));
        //doesn't scale well, but just a little touch for my fellow Montréalers
        String city_hashtag = "";
        String bikeNetworkCity = DBHelper.getBikeNetworkCity(this);
        if (bikeNetworkCity.contains("Montreal")){
            city_hashtag = " @mtlvi";
        }
        String hastagedEnhanced_bikeNetworkCity = bikeNetworkCity + city_hashtag;
        getSupportActionBar().setSubtitle(Utils.fromHtml(String.format(getResources().getString(R.string.appbar_subtitle_formatted), hastagedEnhanced_bikeNetworkCity)));
    }

    private void setupLocationRequest(){

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();

                if(status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(
                                NearbyActivity.this,
                                CHECK_GPS_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        // Ignore the error.
                    }
                }

            }
        });

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
                    LatLng tripLegDestination = curSelectedStation.getLocation();
                    boolean walkMode = isLookingForBike();

                    launchGoogleMapsForDirections(tripLegOrigin, tripLegDestination, walkMode);
                }
            }
        });
    }

    //returns true if _toAdd.getId is NOT already in favorites
    private boolean setupAddFavoriteFab(final FavoriteItemBase _toAdd){
        //This is a full add/remove code

        mAddFavoriteFAB.setTag(_toAdd);

        boolean alreadyFavorite = DBHelper.getFavoriteItemForId(this, _toAdd.getId()) != null;

        if (!alreadyFavorite)
            mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
        else{
            mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_24dp);
            return false;
        }

        mAddFavoriteFAB.setOnClickListener(new View.OnClickListener() {

            boolean mIsFavorite = false;
            @Override
            public void onClick(View view) {

                if (mOnboardingShowcaseView != null){
                    if (checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_CHECKONLY))
                        animateShowcaseToItinerary(); //TODO: have those elevated to steps and have animate* methods called from checkOnboarding
                    else{
                        mOnboardingShowcaseView.hide();
                        mOnboardingShowcaseView = null;
                    }
                }

                if (_toAdd instanceof FavoriteItemPlace) {
                    getListPagerAdapter().showFavoriteHeaderInBTab();
                    mStationMapFragment.clearMarkerPickedPlace();
                    mFavoritePicked = true;
                    hideSetupShowTripDetailsWidget();
                    mStationMapFragment.setPinForPickedFavorite(_toAdd.getDisplayName(),
                            _toAdd.getLocation(),
                            _toAdd.getAttributions());
                }
                else{   //_toAdd instanceof FavoriteItemStation == true

                    mStationMapFragment.setPinForPickedFavorite(_toAdd.getDisplayName(),
                            getLatLngForStation(_toAdd.getId()),
                            null);
                }

                if (!mIsFavorite)
                {
                    mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_24dp);
                    addFavorite(_toAdd, false, false);
                    mFavoritesSheetFab.scrollToTop();

                    mAddFavoriteFAB.hide();
                }
                else{
                    mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
                    removeFavorite(_toAdd, false);
                }

                mIsFavorite = !mIsFavorite;
            }
        });

        return true;
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
                                    Utils.getAverageBikingSpeedKmh(NearbyActivity.this)))
                            .build(NearbyActivity.this);

                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);


                    mFavoritesSheetFab.hideSheetThenFab();
                    mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(NearbyActivity.this, R.color.light_gray));

                    mPlaceAutocompleteLoadingProgressBar.setVisibility(View.VISIBLE);

                    getListPagerAdapter().hideStationRecap();

                    //onboarding is in progress, showcasing search action button, give way to search
                    //(google provided autocompletewidget)
                    if (mOnboardingShowcaseView != null){
                        mOnboardingShowcaseView.hide();
                        mOnboardingShowcaseView = null;
                    }

                    if(!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_HINT))
                        dismissOnboardingHint();

                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
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

                final Place place = PlaceAutocomplete.getPlace(this, data);

                mSearchFAB.hide();

                //IDs are not guaranteed stable over long periods of time
                //but searching for a place already in favorites is not a typical use case
                //TODO: implement best practice of updating favorite places IDs once per month
                FavoriteItemPlace newFavForPlace = new FavoriteItemPlace(place.getId(),
                        place.getName().toString(),
                        place.getLatLng(),
                        place.getAttributions());

                final FavoriteItemBase existingFavForPlace = DBHelper.getFavoriteItemForId(this, newFavForPlace.getId());

                if ( existingFavForPlace == null) {

                    if(setupAddFavoriteFab(newFavForPlace))
                        mAddFavoriteFAB.show();
                    else
                        mAddFavoriteFAB.hide();

                    //User selected a search result, onboarding showcases total trip time and favorite action button
                    if (checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_TRIP_TOTAL_SHOWCASE))
                    //Because destination name is available here and can't be passed down checkOnboarding
                    {
                        mOnboardingShowcaseView.setContentText(String.format(getString(R.string.onboarding_showcase_total_time_text),
                                DBHelper.getBikeNetworkName(this), place.getName()));

                        mOnboardingShowcaseView.setTag(place.getName());
                    }

                    final Handler handler = new Handler();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (NearbyActivity.this.getListPagerAdapter().isViewPagerReady()) {
                                setupBTabSelectionClosestDock(place);
                            } else
                                handler.postDelayed(this, 10);
                        }
                    }, 50);
                }
                else{   //Place was already a favorite
                    final Handler handler = new Handler();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (NearbyActivity.this.getListPagerAdapter().isViewPagerReady()) {
                                setupBTabSelectionClosestDock(existingFavForPlace.getId());
                            } else
                                handler.postDelayed(this, 10);
                        }
                    }, 50);
                }
            } else { //user pressed back, there's no search result available

                mFavoritesSheetFab.showFab();
                mAddFavoriteFAB.hide();
                mSearchFAB.show();

                getListPagerAdapter().showStationRecap();

                //in case of full onboarding, setup search showcase (user cancelled previous showcased search)
                //... check if full onboarding should happen
                if( !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) ) {

                    //... if it doesn't, display hint if required
                    checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);
                }
            }
        } else if (requestCode == SETTINGS_ACTIVITY_REQUEST_CODE){

            mClosestBikeAutoSelected = false;
            mRefreshMarkers = true;
            refreshMap();
        } else if (requestCode == CHECK_PERMISSION_REQUEST_CODE){

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                // permission was granted, yay! Do the thing
                mSplashScreenText.setText(R.string.auto_bike_select_finding);
                mStationMapFragment.enableMyLocationCheckingPermission();
            }
            else
                checkAndAskLocationPermission();
        }
        else if(requestCode == CHECK_GPS_REQUEST_CODE){
            //getting here when GPS been activated through system settings dialog
            if (resultCode != RESULT_OK){

                if (mSplashScreen.isShown()){
                    mSplashScreenText.setText(R.string.sad_emoji);

                    Utils.Snackbar.makeStyled(mSplashScreen, R.string.location_turn_on, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                            .setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    setupLocationRequest();
                                }
                            }).show();
                }
            }
            else{
                mSplashScreenText.setText(R.string.auto_bike_select_finding);
            }
        }
    }

    private void setupFavoriteSheet() {

        //ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
        //        ItemTouchHelper.LEFT) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback( ItemTouchHelper.UP | ItemTouchHelper.DOWN
                , ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                ((ItemTouchHelperAdapter)recyclerView.getAdapter()).onItemMove(viewHolder.getAdapterPosition(),
                                                                                target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder favViewHolder = (FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder)viewHolder;

                removeFavorite(DBHelper.getFavoriteItemForId(NearbyActivity.this, favViewHolder.getFavoriteId()), true);
            }

            @Override
            public boolean isLongPressDragEnabled() {

                return mFavoriteRecyclerViewAdapter.getSheetEditing();

            }

            @Override
            public boolean isItemViewSwipeEnabled() {

                return !mFavoriteRecyclerViewAdapter.getSheetEditing() && !mFavoriteItemEditInProgress;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder _viewHolder, int _actionState){
                if (_actionState != ItemTouchHelper.ACTION_STATE_IDLE){
                    if (_viewHolder instanceof FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder){
                        FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder favoriteItemViewHolder =
                                (FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder) _viewHolder;
                        favoriteItemViewHolder.onItemSelected();
                    }
                }
                super.onSelectedChanged(_viewHolder, _actionState);
            }

            @Override
            public void clearView(RecyclerView _recyclerView, RecyclerView.ViewHolder _viewHolder){
                super.clearView(_recyclerView, _viewHolder);
                if (_viewHolder instanceof FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder){
                    FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder favoriteItemViewHolder =
                            (FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder) _viewHolder;
                    favoriteItemViewHolder.onItemClear();
                }
            }
        };

        mFavoriteItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);

        RecyclerView favoriteRecyclerView = (RecyclerView) findViewById(R.id.favorites_sheet_recyclerview);

        favoriteRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        favoriteRecyclerView.setLayoutManager(new ScrollingLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false, 300));

        mFavoriteRecyclerViewAdapter = new FavoriteRecyclerViewAdapter(this, this, this);

        ArrayList<FavoriteItemBase> favoriteList = DBHelper.getFavoriteAll(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());
        mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);
        favoriteRecyclerView.setAdapter(mFavoriteRecyclerViewAdapter);

        mFavoriteItemTouchHelper.attachToRecyclerView(favoriteRecyclerView);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupFavoriteListFeedback(boolean _noFavorite) {
        if (_noFavorite){
            ((TextView)findViewById(R.id.favorites_sheet_header_textview)).setText(
                    Utils.fromHtml(String.format(getResources().getString(R.string.no_favorite), DBHelper.getBikeNetworkName(this))));
            findViewById(R.id.favorite_sheet_edit_fab).setVisibility(View.INVISIBLE);
            findViewById(R.id.favorite_sheet_edit_done_fab).setVisibility(View.INVISIBLE);
            mFavoriteRecyclerViewAdapter.setSheetEditing(false);
        }
        else{
            ((TextView)findViewById(R.id.favorites_sheet_header_textview)).setText(
                    Utils.fromHtml(String.format(getResources().getString(R.string.favorites_sheet_header), DBHelper.getBikeNetworkName(this))));

            ((FloatingActionButton)findViewById(R.id.favorite_sheet_edit_fab)).show();

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("saved_camera_pos", mStationMapFragment.getCameraPosition());
        outState.putBoolean("requesting_location_updates", mRequestingLocationUpdates);
        outState.putParcelable("user_location_latlng", mCurrentUserLatLng);
        outState.putBoolean("closest_bike_auto_selected", mClosestBikeAutoSelected);
        outState.putBoolean("favorite_sheet_visible", mFavoriteSheetVisible);
        outState.putBoolean("place_autocomplete_loading", mPlaceAutocompleteLoadingProgressBar.getVisibility() == View.VISIBLE);
        outState.putBoolean("refresh_tabs", mRefreshTabs);
        outState.putBoolean("favorite_picked", mFavoritePicked);
        outState.putBoolean("data_outdated", mDataOutdated);

        if (mOnboardingShowcaseView != null && mOnboardingShowcaseView.getTag() != null){
            outState.putString("onboarding_showcase_trip_total_place_name", (String)mOnboardingShowcaseView.getTag());
        }

        if (mAddFavoriteFAB.isShown() && mAddFavoriteFAB.getTag() instanceof FavoriteItemPlace){
            outState.putParcelable("add_favorite_fab_data", (FavoriteItemPlace) mAddFavoriteFAB.getTag());
        } else{
            outState.putParcelable("add_favorite_fab_data", null);
        }
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

            // permission was granted, yay! Do the thing
            mSplashScreenText.setText(R.string.auto_bike_select_finding);
            mStationMapFragment.enableMyLocationCheckingPermission();

        }else {

            mSplashScreenText.setText(R.string.sad_emoji);

            if (ActivityCompat.shouldShowRequestPermissionRationale(NearbyActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                Utils.Snackbar.makeStyled(mSplashScreen, R.string.location_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                checkAndAskLocationPermission();
                            }
                        }).show();
            }
            else{
                Utils.Snackbar.makeStyled(mSplashScreen, R.string.location_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, CHECK_PERMISSION_REQUEST_CODE);
                            }
                        }).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            case R.id.settings_menu_item:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsIntent, SETTINGS_ACTIVITY_REQUEST_CODE);
                return true;

            case R.id.about_menu_item:
                new MaterialDialog.Builder(this)
                        .title(getString(R.string.app_name) + " – " + getString(R.string.app_version_name) + " ©2015–2017     F8Full") //http://stackoverflow.com/questions/4471025/how-can-you-get-the-manifest-version-number-from-the-apps-layout-xml-variable-->
                        .items(R.array.about_dialog_items)
                        .icon(ContextCompat.getDrawable(NearbyActivity.this,R.drawable.logo_48dp))
                        .autoDismiss(false)
                        .itemsCallback(new MaterialDialog.ListCallback() {

                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                                Intent intent;

                                switch (which){
                                    case 0:
                                        startActivity(Utils.getWebIntent(NearbyActivity.this, "http://www.citybik.es", true, text.toString()));
                                        break;
                                    case 1:
                                        intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse("market://details?id="+getPackageName()));
                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent);
                                        }
                                        break;
                                    case 2:
                                        String url = "https://www.facebook.com/findmybikes/";
                                        Uri uri;
                                        try {
                                            getPackageManager().getPackageInfo("com.facebook.katana", 0);
                                            // http://stackoverflow.com/questions/24526882/open-facebook-page-from-android-app-in-facebook-version-v11
                                            uri = Uri.parse("fb://facewebmodal/f?href=" + url);
                                            intent = new Intent(Intent.ACTION_VIEW, uri);
                                        } catch (PackageManager.NameNotFoundException e) {
                                            intent = Utils.getWebIntent(NearbyActivity.this, url, true, text.toString());
                                        }

                                        startActivity(intent);
                                        break;
                                    case 3:
                                        intent = new Intent(Intent.ACTION_SENDTO);
                                        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"ludos+findmybikesfeedback" + getString(R.string.app_version_name) + "@ludoscity.com"});
                                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent);
                                        }
                                        break;
                                    case 4:
                                        new LicensesDialog.Builder(NearbyActivity.this)
                                                .setNotices(R.raw.notices)
                                                .build()
                                                .show();
                                        break;
                                    case 5:
                                        intent = new Intent(NearbyActivity.this, WebViewActivity.class);
                                        intent.putExtra(WebViewActivity.EXTRA_URL, "file:///android_res/raw/privacy_policy.html");
                                        intent.putExtra(WebViewActivity.EXTRA_ACTIONBAR_SUBTITLE, getString(R.string.menu_privacy));
                                        startActivity(intent);
                                        break;
                                    case 6:
                                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/f8full/ludOScity/tree/master/FindMyBikes"));
                                        startActivity(intent);
                                        break;

                                }

                            }
                        })
                        .show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeFavorite(final FavoriteItemBase _toRemove, boolean _showUndo) {

        DBHelper.updateFavorite(false, _toRemove, this);

        ArrayList<FavoriteItemBase> favoriteList = DBHelper.getFavoriteAll(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());

        mFavoriteRecyclerViewAdapter.removeFavorite(_toRemove);

        //To setup correct name
        final StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
        getListPagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);

        if (_toRemove instanceof FavoriteItemStation)
            getListPagerAdapter().notifyStationChangedAll(_toRemove.getId());

        if (!_showUndo) {

            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_removed,
                    Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();
        }
        else{
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_removed,
                    Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        addFavorite(_toRemove, false, false);
                        mFavoritesSheetFab.scrollToTop();
                        getListPagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);
                    }
                }).show();
        }
    }

    private void addFavorite(final FavoriteItemBase _toAdd, boolean _silent, boolean _showUndo) {

        DBHelper.updateFavorite(true, _toAdd, this);

        ArrayList<FavoriteItemBase> favoriteList = DBHelper.getFavoriteAll(this);
        setupFavoriteListFeedback(favoriteList.isEmpty());

        mFavoriteRecyclerViewAdapter.addFavorite(_toAdd);

        //To setup correct name
        final StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
        getListPagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);

        if (_toAdd instanceof FavoriteItemStation)
            getListPagerAdapter().notifyStationChangedAll(_toAdd.getId());

        if (!_silent) {
            if (!_showUndo) {
                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_added,
                        Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                        .show();
            } else {
                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_added, Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                        .setAction(R.string.undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                removeFavorite(_toAdd, false);
                                getListPagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);
                            }
                        }).show();
            }
        }
    }

    private void setupFavoritePickerFab() {

        mFavoritePickerFAB = (Fab) findViewById(R.id.favorite_picker_fab);

        View sheetView = findViewById(R.id.fab_sheet);
        View overlay = findViewById(R.id.overlay);
        int sheetColor = ContextCompat.getColor(this, R.color.cardview_light_background);
        int fabColor = ContextCompat.getColor(this, R.color.theme_primary_dark);

        //Caused by: java.lang.NullPointerException (sheetView)
        // Create material sheet FAB
        mFavoritesSheetFab = new EditableMaterialSheetFab(mFavoritePickerFAB, sheetView, overlay, sheetColor, fabColor, this);

        mFavoritesSheetFab.setEventListener(new MaterialSheetFabEventListener() {
            @Override
            public void onShowSheet() {

                mSearchFAB.hide();
                mFavoriteSheetVisible = true;

                if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_ULTRA_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_TAP_FAV_NAME_HINT))
                    dismissOnboardingHint();
            }

            @Override
            public void onSheetHidden() {
                if (!isLookingForBike() && mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                    //B tab with no selection
                    if (Utils.Connectivity.isConnected(NearbyActivity.this))
                        mSearchFAB.show();

                    if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) &&
                            !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT))
                    {
                        dismissOnboardingHint();
                    }
                }

                mFavoriteSheetVisible = false;
            }
        });
    }

    private void setupClearFab() {
        mClearFAB = (FloatingActionButton) findViewById(R.id.clear_fab);

        mClearFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                clearBSelection();
            }
        });
    }

    private void clearBSelection() {
        mFavoritePicked = false;
        mStationMapFragment.setMapPaddingLeft(0);
        mStationMapFragment.setMapPaddingRight(0);
        hideTripDetailsWidget();
        clearBTab();

        if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE)) {
            checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);
        }
    }

    private enum eONBOARDING_STEP { ONBOARDING_STEP_CHECKONLY, ONBOARDING_STEP_SEARCH_SHOWCASE, ONBOARDING_STEP_TRIP_TOTAL_SHOWCASE,
        ONBOARDING_STEP_MAIN_CHOICE_HINT, ONBOARDING_STEP_TAP_FAV_NAME_HINT, ONBOARDING_STEP_SEARCH_HINT }

    private enum eONBOARDING_LEVEL{ONBOARDING_LEVEL_FULL, ONBOARDING_LEVEL_LIGHT, ONBOARDING_LEVEL_ULTRA_LIGHT}

    //returns true if conditions satisfied (onboarding is showed)
    private boolean checkOnboarding(eONBOARDING_LEVEL _level, eONBOARDING_STEP _step){

        boolean toReturn = false;

        int minValidFavorites = -1;

        if (_level == eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL)
            minValidFavorites = getApplicationContext().getResources().getInteger(R.integer.onboarding_light_min_valid_favorites_count);
        else if(_level == eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT)
            minValidFavorites = getApplicationContext().getResources().getInteger(R.integer.onboarding_ultra_light_min_valid_favorites_count);
        else if (_level == eONBOARDING_LEVEL.ONBOARDING_LEVEL_ULTRA_LIGHT)
            minValidFavorites = getApplicationContext().getResources().getInteger(R.integer.onboarding_none_min_valid_favorites_count);

        //count valid favorites
        if ( !DBHelper.hasAtLeastNValidFavorites(
                getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS),
                minValidFavorites,
                NearbyActivity.this) ){

            if (_step == eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE)
                setupShowcaseSearch();
            else if(_step == eONBOARDING_STEP.ONBOARDING_STEP_TRIP_TOTAL_SHOWCASE)
                setupShowcaseTripTotal();
            else if(_step == eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT)
                setupHintMainChoice();
            else if (_step == eONBOARDING_STEP.ONBOARDING_STEP_TAP_FAV_NAME_HINT)
                setupHintTapFavName();
            else if(_step == eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_HINT)
                setupHintSearch();

            toReturn = true;
        }

        return toReturn;
    }

    @Override
    public void onBackPressed() {
        if (mFavoritesSheetFab.isSheetVisible()) {
            mFavoritesSheetFab.hideSheet();
            dismissOnboardingHint();
        } else //noinspection StatementWithEmptyBody
            if(mOnboardingShowcaseView != null){
            //do nothing if onboarding is in progress
        } else if(isLookingForBike()){
            //A tab exploring

            //go back to B tab
            mStationListViewPager.setCurrentItem(StationListPagerAdapter.DOCK_STATIONS, true);

            //if no selection in B tab...
            if(mStationMapFragment.getMarkerBVisibleLatLng() == null)
            {
                //... check if full onboarding should happen
                if( !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) ) {

                    //... if it doesn't, display hint
                    setupHintMainChoice();
                }
            }
        }
        else if(!isLookingForBike() && mStationMapFragment.getMarkerBVisibleLatLng() != null){
            //B tab exploring and a station is selected

            clearBSelection();
            //in case of full onboarding, showcase search
            checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE);

        } else if (!isLookingForBike() && mOnboardingSnackBar == null) {
            setupHintMainChoice();

        }else {
            //otherwise, pass it up ("exiting" app)
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
                if (mRefreshMarkers && mRedrawMarkersTask == null) {

                    mRedrawMarkersTask = new RedrawMarkersTask();
                    mRedrawMarkersTask.execute(mDataOutdated, isLookingForBike());

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
        getListPagerAdapter().setupUI(StationListPagerAdapter.BIKE_STATIONS, RootApplication.getBikeNetworkStationList(),
                true, null, R.drawable.ic_walking_24dp_white,
                "",
                mCurrentUserLatLng != null ? new StationRecyclerViewAdapter.DistanceComparator(mCurrentUserLatLng) : null);

        LatLng stationBLatLng = mStationMapFragment.getMarkerBVisibleLatLng();

        if (stationBLatLng == null) {
            //TAB B
            getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(),
                    false, null, null,
                    getString(R.string.b_tab_question), null);

            //TAB A
            getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, false);
        }
        else {
            StationItem highlighthedDockStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.DOCK_STATIONS);

            if (highlighthedDockStation != null) {
                setupBTabSelection(highlighthedDockStation.getId(), isLookingForBike());

                FavoriteItemBase newFavForStation = new FavoriteItemStation(highlighthedDockStation.getId(),
                        highlighthedDockStation.getName(), true);

                boolean showFavoriteAddFab = false;

                if (!mStationMapFragment.isPickedFavoriteMarkerVisible()) {
                    if (mStationMapFragment.isPickedPlaceMarkerVisible())
                        showFavoriteAddFab = true;  //Don't setup the fab as it's been done in OnActivityResult
                    else if (setupAddFavoriteFab(newFavForStation))
                        showFavoriteAddFab = true;
                }

                if (showFavoriteAddFab)
                    mAddFavoriteFAB.show();
                else
                    mAddFavoriteFAB.hide();
            }
            else    //B pin on map with no list selection can happen on rapid multiple screen orientation change
                clearBTab();
        }

        mRefreshTabs = false;
    }

    //TODO : This clearly turned into spaghetti. At least extract methods.
    private Runnable createUpdateRefreshRunnableCode(){
        return new Runnable() {

            private boolean mPagerReady = false;
            private NumberFormat mNumberFormat = NumberFormat.getInstance();

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
                            pastStringBuilder.append(getString(R.string.il_y_a)).append(mNumberFormat.format(difference / DateUtils.MINUTE_IN_MILLIS)).append(" ").append(getString(R.string.min_abbreviated));
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
                            mSearchFAB.show();
                            mSearchFAB.setEnabled(true);

                            if (mOnboardingSnackBar != null && mOnboardingSnackBar.getView().getTag() != null)
                                if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE))
                                    checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);

                            mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));
                        }

                        if (DBHelper.isBikeNetworkIdAvailable(getApplicationContext())) {

                            String rawClosest = getListPagerAdapter().retrieveClosestRawIdAndAvailability(true);
                            StationItem closestStation = getStation(Utils.extractClosestAvailableStationIdFromProcessedString(rawClosest));

                            if (difference >= NearbyActivity.this.getApplicationContext().getResources().getInteger(R.integer.outdated_data_time_minute) * 60 * 1000 &&
                                    !mDataOutdated) {

                                mDataOutdated = true;

                                mRefreshMarkers = true;
                                refreshMap();

                                mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));

                                getListPagerAdapter().setupBTabStationARecap(closestStation, mDataOutdated);
                                getListPagerAdapter().setOutdatedDataAll(true);
                            }

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
                    } else {
                        futureStringBuilder.append(getString(R.string.no_connectivity));

                        getListPagerAdapter().setRefreshEnableAll(false);
                        mSearchFAB.setEnabled(false);
                        mSearchFAB.hide();

                        if(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS) != null &&
                                mOnboardingSnackBar != null && mOnboardingSnackBar.getView().getTag() != null && !((String)mOnboardingSnackBar.getView().getTag()).equalsIgnoreCase("NO_CONNECTIVITY"))
                            checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);

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
                        getListPagerAdapter().notifyStationAUpdate(closestBikeStation.getLocation(), mCurrentUserLatLng);
                        hideSetupShowTripDetailsWidget();
                        getListPagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);

                        if (isLookingForBike()) {
                            if (mTripDetailsWidget.getVisibility() == View.INVISIBLE) {
                                mDirectionsLocToAFab.show();
                            }

                            mAutoSelectBikeFab.hide();
                            mStationMapFragment.setMapPaddingRight(0);

                            if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                                mStationListViewPager.setCurrentItem(StationListPagerAdapter.DOCK_STATIONS, true);

                                mFavoritesSheetFab.showFab();

                                //if onboarding not happening...
                                if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) &&
                                        !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT))
                                {
                                    //...open favorites sheet
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

                                if (mDataOutdated){

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_outdated,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));

                                }
                                else if(!closestBikeStation.isLocked() && closestBikeStation.getFree_bikes() > DBHelper.getCriticalAvailabilityMax(NearbyActivity.this)) {

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_found,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.snackbar_green));
                                }
                                else{

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_none,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));
                                }

                                if (mOnboardingSnackBar == null)
                                    mFindBikesSnackbar.show();
                            }
                        }, 500);

                        mClosestBikeAutoSelected = true;
                        //launch twitter task if not already running, pass it the raw String
                        if ( Utils.Connectivity.isConnected(getApplicationContext()) && //data network available
                                mUpdateTwitterTask == null &&   //not already tweeting
                                rawClosest.length() > 32 + StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length() && //validate format - 32 is station ID length
                                (rawClosest.contains(StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX) || rawClosest.contains(StationRecyclerViewAdapter.BAD_AVAILABILITY_POSTFIX) ) && //validate content
                                !mDataOutdated){

                            mUpdateTwitterTask = new UpdateTwitterStatusTask();
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

    private void setupShowcaseSearch() {

        if (Utils.Connectivity.isConnected(getApplicationContext())) {

            if (mOnboardingShowcaseView == null) {
                mOnboardingShowcaseView =
                        new ShowcaseView.Builder(NearbyActivity.this)
                                .setTarget(new ViewTarget(R.id.search_framelayout, NearbyActivity.this))
                                .setStyle(R.style.OnboardingShowcaseTheme)
                                .setContentTitle(R.string.onboarding_showcase_search_title)
                                .setContentText(R.string.onboarding_showcase_search_text)

                                .withMaterialShowcase()
                                .build();

                mOnboardingShowcaseView.hideButton();
            } else {
                mOnboardingShowcaseView.setContentTitle(getString(R.string.onboarding_showcase_search_title));
                mOnboardingShowcaseView.setContentText(getString(R.string.onboarding_showcase_search_text));
            }
        }
        else{
            setupHintMainChoice();
        }
    }

    private void setupShowcaseTripTotal() {
        if (mOnboardingShowcaseView != null)
            mOnboardingShowcaseView.hide();

        mOnboardingShowcaseView =
                new ShowcaseView.Builder(NearbyActivity.this)
                        .setTarget(new ViewTarget(R.id.trip_details_total, NearbyActivity.this))
                        .setStyle(R.style.OnboardingShowcaseTheme)
                        .setContentTitle(R.string.onboarding_showcase_total_time_title)
                        .withMaterialShowcase()
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!mStationMapFragment.isPickedFavoriteMarkerVisible())
                                    animateShowcaseToAddFavorite();
                                else
                                    animateShowcaseToItinerary();
                            }
                        })
                        .build();

        //TODO: position button depending on screen orientation
        //RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        /*lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.RIGHT_OF, R.id.trip_details_total);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            lps.addRule(RelativeLayout.END_OF, R.id.trip_details_total);
        }*/

        //lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        //lps.addRule(RelativeLayout.CENTER_IN_PARENT);
        //int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        //lps.setMargins(margin, margin, margin, margin);

        //mOnboardingShowcaseView.setButtonPosition(lps);

    }

    private void setupHintMainChoice(){

        int messageResourceId = R.string.onboarding_hint_main_choice;

        if (!Utils.Connectivity.isConnected(getApplicationContext()))
            messageResourceId = R.string.onboarding_hint_main_choice_no_connectivity;

        mOnboardingSnackBar =  Utils.Snackbar.makeStyled(mCoordinatorLayout, messageResourceId, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(this, R.color.theme_primary_dark))
                /*.setAction(R.string.gotit, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Snackbar dismisses itself on click
                    }
                })*/;
        if (!Utils.Connectivity.isConnected(getApplicationContext()))
            mOnboardingSnackBar.getView().setTag("NO_CONNECTIVITY");
        else
            mOnboardingSnackBar.getView().setTag("CONNECTIVITY");

        mOnboardingSnackBar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == DISMISS_EVENT_SWIPE)
                    mOnboardingSnackBar = null;
            }
        });


        mOnboardingSnackBar.show();
    }

    private void setupHintTapFavName(){
        mOnboardingSnackBar =  Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_hint_tap_favorite_name, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));
        mOnboardingSnackBar.getView().setTag(null);
        mOnboardingSnackBar.show();
    }

    private void setupHintSearch(){
        mOnboardingSnackBar =  Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_hint_search, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));
        mOnboardingSnackBar.getView().setTag(null);
        mOnboardingSnackBar.show();
    }

    private void animateShowcaseToAddFavorite() {
        mOnboardingShowcaseView.hideButton();

        mOnboardingShowcaseView.setShowcase(new ViewTarget(mAddFavoriteFAB) , true);
        mOnboardingShowcaseView.setContentTitle(getString(R.string.onboarding_showcase_add_favorite_title));
        mOnboardingShowcaseView.setContentText(getString(R.string.onboarding_showcase_add_favorite_text));
    }

    private void animateShowcaseToItinerary(){
        mOnboardingShowcaseView.hideButton();

        mOnboardingShowcaseView.setShowcase(new ViewTarget(R.id.trip_details_directions_a_to_b, this), true);
        mOnboardingShowcaseView.setContentTitle(getString(R.string.onboarding_showcase_itinerary_title));
        mOnboardingShowcaseView.setContentText(getString(R.string.onboarding_showcase_itinerary_favorite_text));
    }

    private void setStatusBarClickListener() {
        //Because the citybik.es landing page is javascript heavy
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            mStatusBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Utils.Connectivity.isConnected(getApplicationContext())) {
                        // use the android system webview
                        Intent intent = new Intent(NearbyActivity.this, WebViewActivity.class);
                        intent.putExtra(WebViewActivity.EXTRA_URL, "http://www.citybik.es");
                        intent.putExtra(WebViewActivity.EXTRA_ACTIONBAR_SUBTITLE, getString(R.string.cities));
                        intent.putExtra(WebViewActivity.EXTRA_JAVASCRIPT_ENABLED, true);
                        startActivity(intent);
                    }
                }
            });
        }
    }

    @Override
    public void onStationMapFragmentInteraction(final Uri uri) {
        //Will be warned of station details click, will make info fragment to replace list fragment

        //Map ready
        if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MAP_READY_PATH))
        {
            refreshMap();
        }
        //Marker click - ignored if onboarding is in progress
        else if ( uri.getPath().equalsIgnoreCase("/" + StationMapFragment.MARKER_CLICK_PATH) && mOnboardingShowcaseView == null){

            if(!isLookingForBike() || mStationMapFragment.getMarkerBVisibleLatLng() != null) {

                if (isLookingForBike()) {

                    if (getListPagerAdapter().highlightStationForPage(uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM),
                            StationListPagerAdapter.BIKE_STATIONS)) {

                        getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.BIKE_STATIONS, isAppBarExpanded());

                        mStationMapFragment.setPinOnStation(true,
                                uri.getQueryParameter(StationMapFragment.MARKER_CLICK_TITLE_PARAM));
                        getListPagerAdapter().setupBTabStationARecap(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS), mDataOutdated);

                        if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                            LatLng newALatLng = mStationMapFragment.getMarkerALatLng();
                            getListPagerAdapter().notifyStationAUpdate(newALatLng, mCurrentUserLatLng);
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

                    boolean showFavoriteAddFab = false;

                    if(!mStationMapFragment.isPickedFavoriteMarkerVisible()){
                        if (mStationMapFragment.isPickedPlaceMarkerVisible())
                            showFavoriteAddFab = true;  //Don't setup the fab as it's been done in OnActivityResult
                        else if (setupAddFavoriteFab(new FavoriteItemStation(clickedStationId,
                                getStation(clickedStationId).getName(),
                                true)))
                            showFavoriteAddFab = true;
                    }

                    if (showFavoriteAddFab)
                        mAddFavoriteFAB.show();
                    else
                        mAddFavoriteFAB.hide();
                }
            } else {

                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_hint_main_choice, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                        .show();

                mStationListViewPager.setCurrentItem(StationListPagerAdapter.DOCK_STATIONS, true);
            }
        }
    }

    //TODO: explore refactoring with the following considerations
    //-stop relying on mapfragment markers visibility to branch code

    //Final destination is a place from the search widget
    //that means no markers are currently on map (due to app flow)
    private void setupBTabSelectionClosestDock(final Place _from){

        dismissOnboardingHint();

        //Remove any previous selection
        getListPagerAdapter().removeStationHighlightForPage(StationListPagerAdapter.DOCK_STATIONS);

        if (mTripDetailsWidget.getVisibility() == View.INVISIBLE){
            mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));
            setupTripDetailsWidget();
            showTripDetailsWidget();
        }
        else{
            hideSetupShowTripDetailsWidget();
        }

        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, RootApplication.getBikeNetworkStationList(),
                true ,
                R.drawable.ic_destination_arrow_white_24dp,
                R.drawable.ic_pin_search_24dp_white,
                "",
                new StationRecyclerViewAdapter.TotalTripTimeComparator(
                        Utils.getAverageWalkingSpeedKmh(this),
                        Utils.getAverageBikingSpeedKmh(this),
                        mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), _from.getLatLng()));


        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
        mClearFAB.show();
        mFavoritesSheetFab.hideSheetThenFab();
        mSearchFAB.hide();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (getListPagerAdapter().isRecyclerViewReadyForItemSelection(StationListPagerAdapter.DOCK_STATIONS)) {
                    //highlight B station in list

                    //the following is why the handler is required (to let time for things to settle after calling getListPagerAdapter().setupUI)
                    String stationId = Utils.extractClosestAvailableStationIdFromProcessedString(getListPagerAdapter().retrieveClosestRawIdAndAvailability(false));

                    getListPagerAdapter().hideStationRecap();
                    mStationMapFragment.setPinOnStation(false, stationId);//set B pin on closest station with available dock
                    getListPagerAdapter().highlightStationForPage(stationId, StationListPagerAdapter.DOCK_STATIONS);
                    getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, true);

                    mStationMapFragment.setPinForPickedPlace(_from.getName().toString(),
                            _from.getLatLng(),
                            _from.getAttributions());

                    mStationMapFragment.setMapPaddingRight(0);
                    animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                            _from.getLatLng(),
                            mStationMapFragment.getMarkerBVisibleLatLng(),
                            null);

                    getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.DOCK_STATIONS, isAppBarExpanded());
                } else {    //This is a repost if RecyclerView is not ready for selection

                    //hackfix. On some devices timing issues led to infinite loop with isRecyclerViewReadyForItemSelection always returning false
                    //so, retry setting up the UI before repost
                    //Replace recyclerview content
                    getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, RootApplication.getBikeNetworkStationList(),
                            true ,
                            R.drawable.ic_destination_arrow_white_24dp,
                            R.drawable.ic_pin_search_24dp_white,
                            "",
                            new StationRecyclerViewAdapter.TotalTripTimeComparator(
                                    Utils.getAverageWalkingSpeedKmh(NearbyActivity.this),
                                    Utils.getAverageBikingSpeedKmh(NearbyActivity.this),
                                    mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), _from.getLatLng()));
                    //end hackfix

                    handler.postDelayed(this, 10);
                }
            }
        }, 10);
    }

    //Final destination is a favorite
    //that means no markers are currently on map (due to app flow)
    private void setupBTabSelectionClosestDock(final String _favoriteId){

        dismissOnboardingHint();

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

        final FavoriteItemBase favorite = DBHelper.getFavoriteItemForId(this, _favoriteId);

        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, RootApplication.getBikeNetworkStationList(),
                true,
                R.drawable.ic_destination_arrow_white_24dp,
                R.drawable.ic_pin_favorite_24dp_white,
                "",
                new StationRecyclerViewAdapter.TotalTripTimeComparator(
                        Utils.getAverageWalkingSpeedKmh(this),
                        Utils.getAverageBikingSpeedKmh(this),
                        mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId())));


        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
        mClearFAB.show();
        mFavoritesSheetFab.hideSheetThenFab();
        mSearchFAB.hide();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (getListPagerAdapter().isRecyclerViewReadyForItemSelection(StationListPagerAdapter.DOCK_STATIONS)) {
                    //highlight B station in list

                    //the following is why the handler is required (to let time for things to settle after calling getListPagerAdapter().setupUI)
                    String stationId = Utils.extractClosestAvailableStationIdFromProcessedString(getListPagerAdapter().retrieveClosestRawIdAndAvailability(false));

                    getListPagerAdapter().hideStationRecap();
                    mStationMapFragment.setPinOnStation(false, stationId);//set B pin on closest station with available dock
                    getListPagerAdapter().highlightStationForPage(stationId, StationListPagerAdapter.DOCK_STATIONS);
                    getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, true);

                    if (!stationId.equalsIgnoreCase(favorite.getId())) {
                        //This is a three legged journey (either to a favorite station that has no dock or a place)

                        LatLng location = favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId());

                        mStationMapFragment.setPinForPickedFavorite(favorite.getDisplayName(),
                                location,
                                favorite.getAttributions());

                        mStationMapFragment.setMapPaddingRight(0);
                        animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                                location,
                                mStationMapFragment.getMarkerBVisibleLatLng(),
                                null);
                    }
                    else    //trip to a favorite station that has docks
                    {
                        mStationMapFragment.setPinForPickedFavorite(favorite.getDisplayName(), favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId()), favorite.getAttributions());
                        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));
                    }

                    getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.DOCK_STATIONS, isAppBarExpanded());

                } else {    //This is a repost if RecyclerView is not ready for selection

                    //hackfix. On some devices timing issues led to infinite loop with isRecyclerViewReadyForItemSelection always returning false
                    //so, retry stting up the UI before repost
                    //Replace recyclerview content
                    getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, RootApplication.getBikeNetworkStationList(),
                            true,
                            R.drawable.ic_destination_arrow_white_24dp,
                            R.drawable.ic_pin_favorite_24dp_white,
                            "",
                            new StationRecyclerViewAdapter.TotalTripTimeComparator(
                                    Utils.getAverageWalkingSpeedKmh(NearbyActivity.this),
                                    Utils.getAverageBikingSpeedKmh(NearbyActivity.this),
                                    mCurrentUserLatLng,
                                    mStationMapFragment.getMarkerALatLng(),
                                    favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId())));
                    //end hackfix

                    handler.postDelayed(this, 10);
                }
            }
        }, 10);
    }

    private void setupBTabSelection(final String _selectedStationId, final boolean _silent){

        dismissOnboardingHint();

        //Remove any previous selection
        getListPagerAdapter().removeStationHighlightForPage(StationListPagerAdapter.DOCK_STATIONS);

        if (mTripDetailsWidget.getVisibility() == View.INVISIBLE){
            mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));
            setupTripDetailsWidget();
            showTripDetailsWidget();
        }
        else{
            hideSetupShowTripDetailsWidget();
        }

        final StationItem selectedStation = getStation(_selectedStationId);

        getListPagerAdapter().hideStationRecap();
        mStationMapFragment.setPinOnStation(false, _selectedStationId);
        getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, true);

        if (!mFavoritePicked)
            mStationMapFragment.clearMarkerPickedFavorite();

        if (mStationMapFragment.isPickedPlaceMarkerVisible() || mStationMapFragment.isPickedFavoriteMarkerVisible())
        {
            LatLng locationToShow;

            if (mStationMapFragment.isPickedPlaceMarkerVisible())
                locationToShow = mStationMapFragment.getMarkerPickedPlaceVisibleLatLng();
            else
                locationToShow = mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng();

            if (!_silent) {
                if (locationToShow.latitude != selectedStation.getLocation().latitude ||
                        locationToShow.longitude != selectedStation.getLocation().longitude)
                {
                    mStationMapFragment.setMapPaddingRight(0);
                    animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                            selectedStation.getLocation(),//getLatLngForStation(_selectedStationId),
                            locationToShow,
                            null);
                }
                else{
                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedStation.getLocation(), 15));
                }
            }

            getListPagerAdapter().highlightStationForPage(_selectedStationId, StationListPagerAdapter.DOCK_STATIONS);
            getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.DOCK_STATIONS, isAppBarExpanded());
        }
        else{   //it's just an A-B trip
            getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, RootApplication.getBikeNetworkStationList(),
                    false,
                    null,
                    null,
                    "",
                    new StationRecyclerViewAdapter.TotalTripTimeComparator(
                            Utils.getAverageWalkingSpeedKmh(this),
                            Utils.getAverageBikingSpeedKmh(this),
                            mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), selectedStation.getLocation()));

            if (!mFavoritePicked){
                FavoriteItemBase fav = DBHelper.getFavoriteItemForId(this, _selectedStationId);
                if (fav != null)
                    mStationMapFragment.setPinForPickedFavorite(fav.getDisplayName(), getLatLngForStation(_selectedStationId), null );
            }

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

                        getListPagerAdapter().highlightStationForPage(_selectedStationId, StationListPagerAdapter.DOCK_STATIONS);
                        if (!_silent)
                            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));

                        getListPagerAdapter().smoothScrollHighlightedInViewForPage(StationListPagerAdapter.DOCK_STATIONS, isAppBarExpanded());

                    } else {    //This is a repost if RecyclerView is not ready for selection

                        //hackfix. On some devices timing issues led to infinite loop with isRecyclerViewReadyForItemSelection always returning false
                        //so, retry stting up the UI before repost
                        //Replace recyclerview content
                        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, RootApplication.getBikeNetworkStationList(),
                                false,
                                null,
                                null,
                                "",
                                new StationRecyclerViewAdapter.TotalTripTimeComparator(
                                        Utils.getAverageWalkingSpeedKmh(NearbyActivity.this),
                                        Utils.getAverageBikingSpeedKmh(NearbyActivity.this),
                                        mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), selectedStation.getLocation()));
                        //end hackfix

                        handler.postDelayed(this, 10);
                    }
                }
            }, 10);
        }
    }

    private void dismissOnboardingHint()
    {
        if (mOnboardingSnackBar != null)
        {
            mOnboardingSnackBar.dismiss();
            mOnboardingSnackBar = null;
        }
    }

    //Assumption here is that there is an A and a B station selected (or soon will be)
    private void setupTripDetailsWidget() {

        final Handler handler = new Handler();    //Need to wait for list selection

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.DOCK_STATIONS) != null &&
                        getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS) != null) {

                    int locToAMinutes = 0;
                    int AToBMinutes = 0;
                    int BToSearchMinutes = 0;

                    StationItem selectedStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
                    String formattedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                            mCurrentUserLatLng, true, null, NearbyActivity.this);
                    if (formattedProximityString.startsWith(">"))
                        locToAMinutes = 61;
                    else if (!formattedProximityString.startsWith("<"))
                        locToAMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                    mTripDetailsProximityA.setText(formattedProximityString);


                    selectedStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.DOCK_STATIONS);
                    formattedProximityString = Utils.getBikingProximityString(selectedStation.getLocation(),
                            getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS).getLocation(),
                            true, null, NearbyActivity.this);
                    if (formattedProximityString.startsWith(">"))
                        AToBMinutes = 61;
                    else if (!formattedProximityString.startsWith("<"))
                        AToBMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                    mTripDetailsProximityB.setText(formattedProximityString);


                    //TODO: this string of if...elseif...elseif...else needs refactoring.
                    // Explore extract methods or create some kind of tripdetailswidget configurator
                    if (mStationMapFragment.getMarkerPickedPlaceVisibleLatLng() == null &&
                            mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng() == null) {
                        //no marker is showed

                        mTripDetailsBToDestinationRow.setVisibility(View.GONE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_a_to_b);
                    }
                    else if(mStationMapFragment.getMarkerPickedPlaceVisibleLatLng() != null){
                        //Place marker is showed

                        formattedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                                mStationMapFragment.getMarkerPickedPlaceVisibleLatLng(),
                                true, null, NearbyActivity.this);

                        if (formattedProximityString.startsWith(">"))
                            BToSearchMinutes = 61;
                        else if (!formattedProximityString.startsWith("<"))
                            BToSearchMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                        mTripDetailsProximitySearch.setText(formattedProximityString);

                        mTripDetailsPinSearch.setVisibility(View.VISIBLE);
                        mTripDetailsPinFavorite.setVisibility(View.INVISIBLE);
                        mTripDetailsBToDestinationRow.setVisibility(View.VISIBLE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_b_to_search);
                    }
                    else if (mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().latitude != mStationMapFragment.getMarkerBVisibleLatLng().latitude ||
                                mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().longitude != mStationMapFragment.getMarkerBVisibleLatLng().longitude) {
                        //Favorite marker is showed and not on B station

                        formattedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                                mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng(),
                                true, null, NearbyActivity.this);

                        if (formattedProximityString.startsWith(">"))
                            BToSearchMinutes = 61;
                        else if (!formattedProximityString.startsWith("<"))
                            BToSearchMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                        mTripDetailsProximitySearch.setText(formattedProximityString);

                        mTripDetailsPinSearch.setVisibility(View.INVISIBLE);
                        mTripDetailsPinFavorite.setVisibility(View.VISIBLE);
                        mTripDetailsBToDestinationRow.setVisibility(View.VISIBLE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_b_to_search);
                    }
                    else{
                        //Favorite marker is showed and on B station

                        mTripDetailsBToDestinationRow.setVisibility(View.GONE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_a_to_b);
                    }

                    int total = locToAMinutes + AToBMinutes + BToSearchMinutes;

                    mTripDetailsProximityTotal.setText(Utils.durationToProximityString(total, false, null, NearbyActivity.this));

                } else
                    handler.postDelayed(this, 10);
            }
        }, 10);
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

            try {
                buildTripDetailsWidgetAnimators(true, getResources().getInteger(R.integer.camera_animation_duration), 0).start();
            }
            catch (IllegalStateException e){
                Log.i("NearbyActivity", "Trip widget show animation end encountered some trouble, skipping", e);
            }
        }
    }

    private void hideSetupShowTripDetailsWidget(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Animator hideAnimator = buildTripDetailsWidgetAnimators(false, getResources().getInteger(R.integer.camera_animation_duration) / 2, .23f);

            hideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    try {
                        setupTripDetailsWidget();
                        buildTripDetailsWidgetAnimators(true, getResources().getInteger(R.integer.camera_animation_duration) / 2, .23f).start();
                    }
                    catch (IllegalStateException e){
                        Log.i("NearbyActivity", "Trip widget hide animation end encountered some trouble, skipping", e);
                    }
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

        getListPagerAdapter().setupUI(StationListPagerAdapter.DOCK_STATIONS, new ArrayList<StationItem>(),
                false, null, null,
                getString(R.string.b_tab_question), null);

        mStationMapFragment.clearMarkerB();
        mStationMapFragment.clearMarkerPickedPlace();
        mStationMapFragment.clearMarkerPickedFavorite();

        //A TAB
        getListPagerAdapter().setClickResponsivenessForPage(StationListPagerAdapter.BIKE_STATIONS, false);

        if (!isLookingForBike()) {
            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13));
            mFavoritesSheetFab.showFab();
            if (Utils.Connectivity.isConnected(NearbyActivity.this))
                mSearchFAB.show();
            mClearFAB.hide();
            mAddFavoriteFAB.hide();
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

        StationItem station = getStation(_stationId);

        if (station != null)
            toReturn = station.getLocation();

        return toReturn;
    }

    private StationItem getStation(String _stationId){
        StationItem toReturn = null;

        ArrayList<StationItem> networkStationList = RootApplication.getBikeNetworkStationList();
        for(StationItem station : networkStationList){
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
            if(!isLookingForBike() || mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                //if null, means the station was clicked twice, hence unchecked
                final StationItem clickedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

                if (isLookingForBike()) {

                    if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {

                        LatLng newALatLng = clickedStation.getLocation();
                        getListPagerAdapter().notifyStationAUpdate(newALatLng, mCurrentUserLatLng);

                        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                        mAutoSelectBikeFab.show();
                        animateCameraToShowUserAndStation(getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS));

                        hideSetupShowTripDetailsWidget();
                    } else{

                        animateCameraToShowUserAndStation(clickedStation);
                    }

                    mStationMapFragment.setPinOnStation(true, clickedStation.getId());
                    getListPagerAdapter().setupBTabStationARecap(clickedStation, mDataOutdated);
                } else {

                    if (mStationMapFragment.isPickedFavoriteMarkerVisible()) {

                        if(clickedStation.getLocation().latitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().latitude ||
                                clickedStation.getLocation().longitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().longitude)
                        {
                            mStationMapFragment.pickedFavoriteMarkerInfoWindowShow();
                        }
                        else {
                            mStationMapFragment.pickedFavoriteMarkerInfoWindowHide();
                        }
                    }

                    setupBTabSelection(clickedStation.getId(), false);

                    FavoriteItemStation newFavForStation = new FavoriteItemStation(clickedStation.getId(),
                            clickedStation.getName(), true);

                    boolean showFavoriteAddFab = false;

                    if(!mStationMapFragment.isPickedFavoriteMarkerVisible()){
                        if (mStationMapFragment.isPickedPlaceMarkerVisible())
                            showFavoriteAddFab = true;  //Don't setup the fab as it's been done in OnActivityResult
                        else if (setupAddFavoriteFab(newFavForStation))
                            showFavoriteAddFab = true;
                    }

                    if (showFavoriteAddFab)
                        mAddFavoriteFAB.show();
                    else
                        mAddFavoriteFAB.hide();
                }
            }
        }
        else if(uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_INACTIVE_ITEM_CLICK_PATH)){

            mStationListViewPager.setCurrentItem(StationListPagerAdapter.DOCK_STATIONS, true);
            setupHintMainChoice();
        }
        else if (uri.getPath().equalsIgnoreCase("/"+ StationListFragment.STATION_LIST_FAVORITE_FAB_CLICK_PATH) ){

            StationItem clickedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

            if (null != clickedStation) {

                boolean newState = !clickedStation.isFavorite(this);

                if (newState) {

                    if(mOnboardingShowcaseView != null){
                        mOnboardingShowcaseView.hide();
                        mOnboardingShowcaseView = null;
                    }

                    if (mStationMapFragment.getMarkerPickedPlaceVisibleName().isEmpty())
                        addFavorite(clickedStation.getFavoriteItemForDisplayName(clickedStation.getName()), false, false);
                    else {   //there's a third destination
                        addFavorite(clickedStation.getFavoriteItemForDisplayName(mStationMapFragment.getMarkerPickedPlaceVisibleName()), false, false);
                    }
                    mFavoritesSheetFab.scrollToTop();

                } else {
                    removeFavorite(DBHelper.getFavoriteItemForId(this, clickedStation.getId()), false);
                }
            }
        }
        else if (uri.getPath().equalsIgnoreCase("/" + StationListFragment.STATION_LIST_DIRECTIONS_FAB_CLICK_PATH)){
            //http://stackoverflow.com/questions/6205827/how-to-open-standard-google-map-application-from-my-application

            final StationItem curSelectedStation = getListPagerAdapter().getHighlightedStationForPage(mTabLayout.getSelectedTabPosition());

            // Seen NullPointerException in crash report.
            if (null != curSelectedStation) {

                LatLng tripLegOrigin = isLookingForBike() ? mCurrentUserLatLng : mStationMapFragment.getMarkerALatLng();
                LatLng tripLegDestination = curSelectedStation.getLocation();
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
                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_fab_padding), station.getLocation(), mCurrentUserLatLng, null);
            else    //Map id padded on the left and interface is clear on the right
                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_ab_pin_padding), station.getLocation(), mCurrentUserLatLng, null);

        }
        else{
            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(station.getLocation(), 15));
        }
    }

    //TODO: refactor this method such as
    //-passing only one valid LatLng leads to a regular animateCamera
    //-passing identical LatLng leads to a regular animateCamera, maybe with client code provided zoom level or a default one
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

        if (stationA != null) {
            if(!getListPagerAdapter().setupBTabStationARecap(stationA, mDataOutdated))
                mClosestBikeAutoSelected = false;   //to handle rapid multiple screen orientation change
        }

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

                dismissOnboardingHint();

                mStationMapFragment.setScrollGesturesEnabled(false);

                if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                    mStationMapFragment.setMapPaddingLeft(0);
                    hideTripDetailsWidget();
                    mDirectionsLocToAFab.show();
                }

                mAppBarLayout.setExpanded(false, true);
                getListPagerAdapter().smoothScrollHighlightedInViewForPage(position, true);

                mSearchFAB.hide();
                mAddFavoriteFAB.hide();
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

                    mStationMapFragment.lookingForBikes(mDataOutdated, true);
                }
            } else { //B TAB

                mAutoSelectBikeFab.hide();
                mStationMapFragment.setMapPaddingRight(0);

                //TODO: Should I lock that for regular users ?
                mStationMapFragment.setScrollGesturesEnabled(true);

                mAppBarLayout.setExpanded(true, true);

                if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {

                    //check if showcasing should happen, if not check if hint should happen
                    if(!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE))
                        checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);

                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13.75f));

                    if (mPlaceAutocompleteLoadingProgressBar.getVisibility() != View.GONE){
                        mSearchFAB.show();
                        mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_gray));
                    }
                    else {
                        mDirectionsLocToAFab.hide();
                        mFavoritesSheetFab.showFab();
                        if (Utils.Connectivity.isConnected(NearbyActivity.this))
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

                    LatLng locationToShow = null;

                    if (mStationMapFragment.isPickedPlaceMarkerVisible()) {
                        locationToShow = mStationMapFragment.getMarkerPickedPlaceVisibleLatLng();
                        mAddFavoriteFAB.show();
                    }
                    else if(mStationMapFragment.isPickedFavoriteMarkerVisible() &&
                            (mStationMapFragment.getMarkerBVisibleLatLng().latitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().latitude ||
                            mStationMapFragment.getMarkerBVisibleLatLng().longitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().longitude)
                            )
                        locationToShow = mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng();
                    else if(!mStationMapFragment.isPickedFavoriteMarkerVisible() && DBHelper.getFavoriteItemForId(this, highlightedStation.getId()) == null)
                        mAddFavoriteFAB.show();

                    if (locationToShow != null) {
                        mStationMapFragment.setMapPaddingRight(0);
                        animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                                mStationMapFragment.getMarkerBVisibleLatLng(),
                                locationToShow,
                                null);
                    }
                    else
                        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));
                }

                mStationMapFragment.lookingForBikes(mDataOutdated, false);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

        //pager will always transition from tab A to tab B on app launch
        if (state == ViewPager.SCROLL_STATE_IDLE && mSplashScreen.isShown()) {

            //If update mode is set to manual and user are out of the bound of the bike network
            //let's check if there's a better bike network avaialble.
            if (!DBHelper.getAutoUpdate(this) && Utils.Connectivity.isConnected(NearbyActivity.this))
            {
                if (mCurrentUserLatLng != null && !DBHelper.getBikeNetworkBounds(NearbyActivity.this, 5).contains(mCurrentUserLatLng)) {
                    //This task will possibly auto cancel if it can't find a better bike network
                    mFindNetworkTask = new FindNetworkTask(DBHelper.getBikeNetworkName(NearbyActivity.this));
                    mFindNetworkTask.execute();
                }
            }

            mSplashScreen.setVisibility(View.GONE);
        }
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

        if (mCurrentUserLatLng != null &&
                mCurrentUserLatLng.latitude == location.getLatitude() &&
                mCurrentUserLatLng.longitude == location.getLongitude())
            return;

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
    public void onFavoriteListItemClick(String _favoriteID) {

        StationItem stationA = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);

        if (stationA.getId().equalsIgnoreCase(_favoriteID)) {

            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.such_short_trip, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();

        } else {
            mFavoritePicked = true;
            setupBTabSelectionClosestDock(_favoriteID);
        }
    }

    @Override
    public void onFavoristeListItemNameEditBegin() {
        mFavoritesSheetFab.hideEditFab();
        mFavoriteItemEditInProgress = true;
    }

    @Override
    public void onFavoristeListItemNameEditAbort() {
        mFavoritesSheetFab.showEditFab();
        mFavoriteItemEditInProgress = false;
    }

    @Override
    public void onFavoriteListItemDelete(String _favoriteId) {
        removeFavorite(DBHelper.getFavoriteItemForId(this,_favoriteId), true);
    }

    @Override
    public void onFavoristeListItemNameEditDone(String _favoriteId, String _newName) {

        if (!_favoriteId.startsWith(FavoriteItemPlace.PLACE_ID_PREFIX)) {
            DBHelper.updateFavorite(true, getStation(_favoriteId).getFavoriteItemForDisplayName(_newName), this);
            StationItem closestBikeStation = getListPagerAdapter().getHighlightedStationForPage(StationListPagerAdapter.BIKE_STATIONS);
            getListPagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);
            getListPagerAdapter().notifyStationChangedAll(_favoriteId);
        }
        else{
            DBHelper.updateFavorite(true, new FavoriteItemPlace(DBHelper.getFavoriteItemForId(this, _favoriteId), _newName), this);
        }

        mFavoritesSheetFab.showEditFab();
        mFavoriteRecyclerViewAdapter.setupFavoriteList(DBHelper.getFavoriteAll(this));
        mFavoriteItemEditInProgress = false;
    }

    @Override
    public void onFavoriteListItemStartDrag(RecyclerView.ViewHolder _viewHolder){
        mFavoriteItemTouchHelper.startDrag(_viewHolder);
    }

    @Override
    public void onFavoriteSheetEditDone() {

        ArrayList<FavoriteItemBase> newlyOrderedFavList = new ArrayList<>();
        newlyOrderedFavList.addAll(mFavoriteRecyclerViewAdapter.getCurrentFavoriteList());

        DBHelper.dropFavoriteAll(this);
        mFavoriteRecyclerViewAdapter.clearFavoriteList();

        ListIterator<FavoriteItemBase> li = newlyOrderedFavList.listIterator(newlyOrderedFavList.size());

        while (li.hasPrevious())
        {
            addFavorite(li.previous(), true, false);
        }
    }

    @Override
    public void onFavoriteSheetEditCancel(){

        mFavoriteRecyclerViewAdapter.setupFavoriteList(DBHelper.getFavoriteAll(this));
    }

    private class RedrawMarkersTask extends AsyncTask<Boolean, Void, Void> {

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
            ArrayList<StationItem> networkStationList = RootApplication.getBikeNetworkStationList();
            for (StationItem item : networkStationList){
                mStationMapFragment.addMarkerForStationItem(bools[0], item, bools[1]);
            }

            return null;
        }

        @Override
        protected void onCancelled (Void aVoid) {
            //super.onCancelled(aVoid);
            //https://developer.android.com/reference/android/os/AsyncTask.html#onCancelled(Result)
            //" If you write your own implementation, do not call super.onCancelled(result)."

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

        FindNetworkTask(String _currentNetworkName){ mOldBikeNetworkName = _currentNetworkName; }

        private static final String ERROR_KEY_NO_BETTER = "NO_BETTER_NETWORK" ;
        private static final String ERROR_KEY_IOEXCEPTION = "IOEXCEPTION" ;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mStatusTextView.setText(getString(R.string.searching_wait_location));

            if (getListPagerAdapter().isViewPagerReady())
                getListPagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onCancelled(Map<String,String> _result) {
            //super.onCancelled(aVoid);
            //https://developer.android.com/reference/android/os/AsyncTask.html#onCancelled(Result)
            //" If you write your own implementation, do not call super.onCancelled(result)."

            getListPagerAdapter().setRefreshingAll(false);

            //This was initial setup
            if (!DBHelper.isBikeNetworkIdAvailable(NearbyActivity.this)){
                mSplashScreenText.setText(getString(R.string.sad_emoji));
                Utils.Snackbar.makeStyled(mSplashScreen, R.string.connectivity_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                tryInitialSetup();
                            }
                        }).show();
            }
            else if (_result.containsKey(ERROR_KEY_IOEXCEPTION)){   //HTTP session ran into troubles
                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_download_failed,
                        Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.resume, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DBHelper.resumeAutoUpdate();
                            }
                        }).show();
            }
            else{//_result.containsValue(ERROR_VALUE_NO_BETTER)
                //new SaveNetworkToDatabaseTask().execute();
                //Saving to database executes in parallel. Maybe a service should be used in place
                new SaveNetworkToDatabaseTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

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
                    toReturn.put(ERROR_KEY_NO_BETTER, "dummy");
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
                toReturn.put(ERROR_KEY_IOEXCEPTION, "dummy");

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

            //We get here only if the network was actually changed
            if(mStationMapFragment != null && mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                clearBTab();
            }

            mClosestBikeAutoSelected = false;

            //noinspection ConstantConditions
            setupActionBarStrings();
            setupFavoriteSheet();

            if (mSplashScreen.isShown()){
                mSplashScreen.setVisibility(View.GONE);
            }

            AlertDialog alertDialog = new AlertDialog.Builder(NearbyActivity.this).create();
            //alertDialog.setTitle(getString(R.string.network_found_title));
            if (!backgroundResults.keySet().contains("old_network_name")) {
                alertDialog.setTitle(Utils.fromHtml(String.format(getResources().getString(R.string.hello_city), "", backgroundResults.get("new_network_city") )));
                alertDialog.setMessage(Utils.fromHtml(String.format(getString(R.string.bike_network_found_message),
                        DBHelper.getBikeNetworkName(NearbyActivity.this) )));
                Message toPass = null; //To resolve ambiguous call
                //noinspection ConstantConditions
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), toPass);
            }
            else{
                alertDialog.setTitle(Utils.fromHtml(String.format(getResources().getString(R.string.hello_city), getResources().getString(R.string.hello_travel), backgroundResults.get("new_network_city"))));
                alertDialog.setMessage(Utils.fromHtml(String.format(getString(R.string.bike_network_change_message),
                        DBHelper.getBikeNetworkName(NearbyActivity.this), mOldBikeNetworkName)));
                Message toPass = null; //To resolve ambiguous call
                //noinspection ConstantConditions
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), toPass);
                mStationMapFragment.doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(mCurrentUserLatLng, 15), true);
            }

            alertDialog.show();

            if(!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE)){
                checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);
            }

            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();

            mFindNetworkTask = null;
        }
    }

    //TODO: NOT use an asynchtask for this long running database operation
    private class SaveNetworkToDatabaseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            DBHelper.notifyBeginSavingStations(NearbyActivity.this);
        }


        @Override
        protected Void doInBackground(Void... params) {

            ArrayList<StationItem> networkStationList = RootApplication.getBikeNetworkStationList();

            try {
                //TODO: This is ugly.
                //This process shouldn't use an asynctask anyway as it's long running :/
                DBHelper.deleteAllStations();

                for (StationItem station : networkStationList) {
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
        }
    }

    public class UpdateTwitterStatusTask extends AsyncTask<String, Void, Void>{

        private static final int REPLY_STATION_NAME_MAX_LENGTH = 54;
        private static final int STATION_ID_LENGTH = 32;

        @Override
        protected Void doInBackground(String... params) {

            List<StationItem> networkStationList = RootApplication.getBikeNetworkStationList();

            Map<String, StationItem> networkStationMap = new HashMap<>(networkStationList.size());

            for (StationItem station : networkStationList){
                networkStationMap.put(station.getId(), station);
            }

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
            String deduplicate = "deduplicate";    //hashtagged

            //Pair of station id and availability code (always 'CRI' as of now)
            List<Pair<String,String>> discardedStations = new ArrayList<>();

            StationItem selectedStation = null;
            List<String> extracted = Utils.extractOrderedStationIdsFromProcessedString(params[0]);
            //extracted will contain as firt element
            //   359f354466083c962d243bc238c95245_AVAILABILITY_AOK
            //OR 359f354466083c962d243bc238c95245_AVAILABILITY_BAD
            //followed by 1 or more string in the form of
            //   3c3bf5e74cb938e7d57641edaf909d24_AVAILABILITY_CRI

            boolean firstString = true;

            for (String e : extracted)
            {
                if (firstString){
                    //359f354466083c962d243bc238c95245_AVAILABILITY_BAD or
                    //359f354466083c962d243bc238c95245_AVAILABILITY_AOK

                    selectedStationId = e.substring(0,STATION_ID_LENGTH);
                    selectedBadorAok = e.substring(STATION_ID_LENGTH + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length() ,
                            STATION_ID_LENGTH + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length() + 3); //'BAD' or 'AOK'

                    selectedStation = networkStationMap.get(selectedStationId);

                    selectedNbBikes = selectedStation.getFree_bikes();

                    selectedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                            mCurrentUserLatLng, false, null, NearbyActivity.this);

                    //station name will be truncated to fit everything in a single tweet
                    //see R.string.twitter_not_closest_bike_data_format
                    int maxStationNameIdx = 138 - (deduplicate.length()+" ".length()
                            + " walk ".length()
                            + selectedProximityString.length()
                            + " ".length()
                            + STATION_ID_LENGTH
                            + " at ".length()
                            + selectedBadorAok.length()
                            + " #".length()
                            + Integer.toString(selectedNbBikes).length()
                            + " bike is not closest! Bikes:".length()
                            + systemHashtag.length());

                    selectedStationName = selectedStation.getName().substring(0, Math.min(selectedStation.getName().length(), maxStationNameIdx));

                    firstString = false;
                }
                else { //3c3bf5e74cb938e7d57641edaf909d24_AVAILABILITY_CRI

                    Pair<String, String> discarded = new Pair<>(e.substring(0,STATION_ID_LENGTH), e.substring(
                            STATION_ID_LENGTH + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length(),
                            STATION_ID_LENGTH + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length() + 3
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
            newStatus.displayCoordinates(true).location(new GeoLocation(selectedStation.getLocation().latitude, selectedStation.getLocation().longitude));

            boolean deduplicationDone = false;


            while (!deduplicationDone){

                //post status before adding replies
                try {
                    //can be interrupted here (duplicate)
                    twitter4j.Status answerStatus = api.updateStatus(newStatus);

                    long replyToId = answerStatus.getId();

                    for (Pair<String, String> discarded : discardedStations ){
                        StationItem discardedStationItem = networkStationMap.get(discarded.first);

                        String replyStatusString = String.format(getResources().getString(R.string.twitter_closer_discarded_reply_data_format),
                                systemHashtag, discardedStationItem.getFree_bikes(), discarded.second, discarded.first,
                                discardedStationItem.getName().substring(0, Math.min(discardedStationItem.getName().length(), REPLY_STATION_NAME_MAX_LENGTH)));

                        StatusUpdate replyStatus = new StatusUpdate(replyStatusString);

                        replyStatus.inReplyToStatusId(replyToId)
                                .displayCoordinates(true)
                                .location(new GeoLocation(discardedStationItem.getLocation().latitude, discardedStationItem.getLocation().longitude));

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
                        newStatus.displayCoordinates(true).location(new GeoLocation(selectedStation.getLocation().latitude, selectedStation.getLocation().longitude));

                        Log.d("TwitterUpdate", "TwitterUpdate duplication -- deduplicating now", e);

                    } else {
                        deduplicationDone = true;
                    }
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

        private LatLngBounds mDownloadedBikeNetworkBounds;

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

                ArrayList<StationItem> newBikeNetworkStationList = RootApplication.addAllToBikeNetworkStationList(statusAnswer.body().network.stations, NearbyActivity.this);

                //Calculate bounds
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                for (StationItem station : newBikeNetworkStationList){
                    boundsBuilder.include(station.getLocation());
                }

                mDownloadedBikeNetworkBounds = boundsBuilder.build();

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

            if (getListPagerAdapter().isViewPagerReady())
                getListPagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onCancelled (Void aVoid){
            //super.onCancelled(aVoid);
            //https://developer.android.com/reference/android/os/AsyncTask.html#onCancelled(Result)
            //" If you write your own implementation, do not call super.onCancelled(result)."
            //Set interface back
            getListPagerAdapter().setRefreshingAll(false);

            //wasn't initial download
            if(!RootApplication.getBikeNetworkStationList().isEmpty()) {

                DBHelper.pauseAutoUpdate();

                if (DBHelper.getAutoUpdate(NearbyActivity.this)) {
                    Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_download_failed,
                            Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                            .setAction(R.string.resume, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DBHelper.resumeAutoUpdate();
                                }
                            }).show();
                } else {
                    Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.manual_download_failed,
                            Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                            .setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mDownloadWebTask = new DownloadWebTask();
                                    mDownloadWebTask.execute();
                                }
                            }).show();
                }
            }
            else {

                mSplashScreenText.setText(getString(R.string.sad_emoji));
                Utils.Snackbar.makeStyled(mSplashScreen, R.string.connectivity_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                tryInitialSetup();
                            }
                        }).show();
            }

            //must be done last
            mDownloadWebTask = null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //switch progressbar view visibility

            getListPagerAdapter().setRefreshingAll(false);
            mClosestBikeAutoSelected = false;

            DBHelper.saveLastUpdateTimestampAsNow(getApplicationContext());
            DBHelper.saveBikeNetworkBounds(mDownloadedBikeNetworkBounds, NearbyActivity.this);

            mDataOutdated = false;
            mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));

            getListPagerAdapter().setOutdatedDataAll(false);
            String rawClosest = getListPagerAdapter().retrieveClosestRawIdAndAvailability(true);
            StationItem closestStation = getStation(Utils.extractClosestAvailableStationIdFromProcessedString(rawClosest));
            if (closestStation != null) //null if downloading immediately after app launch
                getListPagerAdapter().setupBTabStationARecap(closestStation, mDataOutdated);

            mRefreshMarkers = true;
            mRefreshTabs = true;
            refreshMap();
            Log.d("nearbyActivity", RootApplication.getBikeNetworkStationList().size() + " stations downloaded from citibik.es");

            //users are inside bounds
            if (mCurrentUserLatLng == null || DBHelper.getBikeNetworkBounds(NearbyActivity.this, 5).contains(mCurrentUserLatLng) ){

                //new SaveNetworkToDatabaseTask().execute();
                //Saving to database executes in parallel. Maybe a service should be used in place
                new SaveNetworkToDatabaseTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
            else if(mCurrentUserLatLng != null){    //users are outside bounds

                //This task will possibly auto cancel if it can't find a better bike network
                mFindNetworkTask = new FindNetworkTask(DBHelper.getBikeNetworkName(NearbyActivity.this));
                mFindNetworkTask.execute();
            }

            //must be done last
            mDownloadWebTask = null;

            //special case for test versions in firebase lab
            //full onboarding prevents meaningful coverage (robo test don't input anything in search autocomplete widget)
            if (getString(R.string.app_version_name).contains("test") || getString(R.string.app_version_name).contains("alpha")){

                int addedCount = 0;

                ArrayList<StationItem> networkStationList = RootApplication.getBikeNetworkStationList();
                for(StationItem station : networkStationList) {
                    if (!DBHelper.isFavorite(station.getId(), NearbyActivity.this)) {

                        if (addedCount > 3)
                            break;

                        else if (addedCount % 2 == 0) { //non default favorite name
                            //station.setFavorite(true, NearbyActivity.this); //We want to manipulate everything, hence go directly to DBHelper
                            DBHelper.updateFavorite(true, new FavoriteItemStation(station.getId(), station.getName() + "-test", false), NearbyActivity.this);
                            ArrayList<FavoriteItemBase> favoriteList = DBHelper.getFavoriteAll(NearbyActivity.this);
                            setupFavoriteListFeedback(favoriteList.isEmpty());
                            mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);
                        }
                        else{   //default favorite name
                            DBHelper.updateFavorite(true, new FavoriteItemStation(station.getId(), station.getName(), true), NearbyActivity.this);
                            ArrayList<FavoriteItemBase> favoriteList = DBHelper.getFavoriteAll(NearbyActivity.this);
                            setupFavoriteListFeedback(favoriteList.isEmpty());
                            mFavoriteRecyclerViewAdapter.setupFavoriteList(favoriteList);
                        }

                        ++addedCount;
                    }
                }

                if (mOnboardingShowcaseView != null)
                    mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
            }
        }
    }
}

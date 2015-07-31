package com.ludoscity.bikeactivityexplorer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.ludoscity.bikeactivityexplorer.BixiAPI.BixiAPI;
import com.ludoscity.bikeactivityexplorer.DBHelper.DBHelper;
import com.ludoscity.bikeactivityexplorer.Utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;

public class NearbyFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationChangeListener, GoogleMap.OnCameraChangeListener, GoogleMap.OnInfoWindowClickListener {
    private Context mContext;
    private OnFragmentInteractionListener mListener;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String PREF_WEBTASK_LAST_TIMESTAMP_MS = "last_refresh_timestamp";
    private DownloadWebTask mDownloadWebTask = null;
    private BixiAPI bixiApiInstance;

    private Handler mUpdateRefreshHandler = null;
    private Runnable mUpdateRefreshRunnableCode = null;

    private GoogleMap nearbyMap = null;
    private LatLng mCurrentUserLatLng;
    private CameraPosition mBackCameraPosition;
    private float mMaxZoom = 16f;

    private MenuItem mFavoriteStar;
    private MenuItem mParkingSwitch;
    private View mStationInfoViewHolder;
    private View mStationListViewHolder;
    private StationsNetwork mStationsNetwork;
    private ArrayList<StationMapGfx> mMapMarkersGfxData = new ArrayList<>();
    private StationListViewAdapter mStationListViewAdapter;
    private TextView mBikesOrParkingColumn;
    private ListView mStationListView;
    private StationItem mCurrentInfoStation;
    private TextView mStationInfoNameView;
    private TextView mStationInfoBikeAvailView;
    private TextView mStationInfoParkingAvailView;
    private TextView mStationInfoDistanceView;
    private ImageView mDirectionArrow;
    private TextView mUpdateTextView;
    private ProgressBar mUpdateProgressBar;
    private ImageView mRefreshButton;
    private View mDownloadBar;

    private int mIconStarOn = com.ludoscity.bikeactivityexplorer.R.drawable.abc_btn_rating_star_on_mtrl_alpha;
    private int mIconStarOff = com.ludoscity.bikeactivityexplorer.R.drawable.abc_btn_rating_star_off_mtrl_alpha;

    private boolean isStationInfoVisible;
    private boolean isAlreadyZoomedToUser;
    private boolean refreshMarkers = true;
    private boolean mIsFromFavoriteSection;


    public static NearbyFragment newInstance(int sectionNumber) {
        NearbyFragment fragment = new NearbyFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setHasOptionsMenu(true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (Utils.Connectivity.isConnected(getContext()) && sp.getLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, 0) == 0) { //Means ask never successfully completed
            //Because webtask launches DB task, we know that a value there means actual data in the DB
            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();
        }
        else{   //Having a timestamp means some data exists in the db, as both task are intimately linked
            try {
                mStationsNetwork = DBHelper.getStationsNetwork();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            Log.d("nearbyFragment", mStationsNetwork.stations.size() + " stations loaded from DB");

            //Create map marker gfx data
            for (StationItem item : mStationsNetwork.stations){
                mMapMarkersGfxData.add(new StationMapGfx(item));
            }
        }
    }

    //Safe to call from multiple point in code, refreshing the UI elements with the most recent data available
    //Takes care of map readyness check
    //Safely updates everything based on checking the last update timestamp
    private void setupUI(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
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

                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
                            if (Utils.Connectivity.isConnected(getContext())) {
                                //Should come from something keeping tabs on time, maybe this runnable itself
                                long wishedUpdateTime = runnableLastRefreshTimestamp + 5 * 1000 * 60;  //comes from Prefs

                                if (now >= wishedUpdateTime) {

                                    //Put a string same length as the other one ?
                                    updateTextBuilder.append(" ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.updating));

                                    //Run update

                                    mDownloadWebTask = new DownloadWebTask();
                                    mDownloadWebTask.execute();


                                    //lastUpdateTime = now;
                                } else {

                                    updateTextBuilder.append(" - ").append(getString(com.ludoscity.bikeactivityexplorer.R.string.nextUpdate)).append(" ");


                                    long differenceSecond = (wishedUpdateTime - now) / DateUtils.SECOND_IN_MILLIS;

                                    // formatted will be HH:MM:SS or MM:SS
                                    updateTextBuilder.append(DateUtils.formatElapsedTime(differenceSecond));

                                    mRefreshButton.setVisibility(View.VISIBLE);
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


            if(nearbyMap != null) {
                if (mStationsNetwork != null && refreshMarkers) {

                    //Gfx data not available yet
                    if(mMapMarkersGfxData.isEmpty())
                    {
                        //SETUP MARKERS DATA
                        for (StationItem item : mStationsNetwork.stations){
                            mMapMarkersGfxData.add(new StationMapGfx(item));
                        }
                    }

                    nearbyMap.clear();

                    for (StationMapGfx markerData : mMapMarkersGfxData){
                        markerData.addMarkerToMap(nearbyMap);
                    }
                     refreshMarkers = false;
                }
                int listPosition = mStationListView.getFirstVisiblePosition();
                int itemSelected = -1;
                if (mStationListViewAdapter != null)
                    itemSelected = mStationListViewAdapter.getCurrentItemSelected();
                if (mStationsNetwork != null) {
                    mStationListViewAdapter = new StationListViewAdapter(mContext, mStationsNetwork, mCurrentUserLatLng, true);//mParkingSwitch.isChecked());
                    mStationListViewAdapter.setItemSelected(itemSelected);
                    mStationListView.setAdapter(mStationListViewAdapter);
                    mStationListView.setSelectionFromTop(listPosition, 0);
                }
            }

        } else{
            mUpdateTextView.setText(getString(com.ludoscity.bikeactivityexplorer.R.string.nearbyfragment_default_never_web_updated));
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden){
            //Recreate handler to maintain update TextView String
            mUpdateRefreshHandler = new Handler();
            setupUI();

            ((MainActivity) getActivity()).onSectionHiddenChanged(
                    getArguments().getInt(ARG_SECTION_NUMBER));
            if (isStationInfoVisible && !mIsFromFavoriteSection) replaceInfoViewByListView();

        }
        else{
            stopUIRefresh();
        }
    }

    private void stopUIRefresh() {
        if (mUpdateRefreshHandler != null) {
            mUpdateRefreshHandler.removeCallbacks(mUpdateRefreshRunnableCode);
            mUpdateRefreshRunnableCode = null;
            mUpdateRefreshHandler = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        try {
            mListener = (OnFragmentInteractionListener) activity;
            super.onAttach(activity);
            //((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        mUpdateRefreshHandler = new Handler();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        cancelDownloadWebTask();

        mListener = null;

        stopUIRefresh();

    }

    private void cancelDownloadWebTask() {
        if (mDownloadWebTask != null && !mDownloadWebTask.isCancelled())
        {
            mDownloadWebTask.cancel(false);
            mDownloadWebTask = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public void onPause() {

        cancelDownloadWebTask();
        stopUIRefresh();

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshMarkers = true;
        if(mUpdateRefreshHandler == null)
            mUpdateRefreshHandler = new Handler();
        setupUI();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View inflatedView = layoutInflater.inflate(com.ludoscity.bikeactivityexplorer.R.layout.fragment_nearby, viewGroup, false);
        if(nearbyMap == null)
            ((MapFragment) getActivity().getFragmentManager().findFragmentById(com.ludoscity.bikeactivityexplorer.R.id.mapNearby)).getMapAsync(this);
        // List view
        mStationListView = (ListView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.stationListView);
        mStationListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        setOnClickItemListenerStationListView();
        mStationListViewHolder = inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.stationList);
        mBikesOrParkingColumn = (TextView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.bikesOrParkingColumn);
        // Station Info
        mStationInfoViewHolder = inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.stationInfo);
        mDirectionArrow = (ImageView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.arrowImage);
        mStationInfoNameView = (TextView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.stationInfo_name);
        mStationInfoDistanceView = (TextView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.stationInfo_distance);
        mStationInfoBikeAvailView = (TextView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.stationInfo_bikeAvailability);
        mStationInfoParkingAvailView = (TextView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.stationInfo_parkingAvailability);
        // Update Bar
        mUpdateTextView = (TextView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.update_textView);
        mUpdateTextView.setTextColor(Color.LTGRAY);
        mUpdateProgressBar = (ProgressBar) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.refreshDatabase_progressbar);
        mUpdateProgressBar.setVisibility(View.INVISIBLE);
        mRefreshButton = (ImageView) inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.refreshDatabase_button);
        mDownloadBar = inflatedView.findViewById(com.ludoscity.bikeactivityexplorer.R.id.downloadBar);
        setRefreshButtonListener();

        setupUI();
        return inflatedView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(com.ludoscity.bikeactivityexplorer.R.menu.menu_nearby,menu);
        mFavoriteStar = menu.findItem(com.ludoscity.bikeactivityexplorer.R.id.favoriteStar);
        mParkingSwitch = menu.findItem(com.ludoscity.bikeactivityexplorer.R.id.findBikeParkingSwitchMenuItem);

        setOnClickFindSwitchListener();
        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(com.ludoscity.bikeactivityexplorer.R.id.action_bar_find_bike_parking_switch)).setChecked(true);
        mParkingSwitch.setVisible(!isStationInfoVisible);

        mFavoriteStar.setVisible(isStationInfoVisible);
        if (mCurrentInfoStation != null){
            if (mCurrentInfoStation.isFavorite()) {
                mFavoriteStar.setIcon(mIconStarOn);
            }else{
                mFavoriteStar.setIcon(mIconStarOff);
            }
        }

        Log.d("onCreateOptionsMenu","menu created");
    }

    private void setOnClickFindSwitchListener() {
        ((SwitchCompat)mParkingSwitch.getActionView().findViewById(com.ludoscity.bikeactivityexplorer.R.id.action_bar_find_bike_parking_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mStationListViewAdapter != null) {
                    lookingForBikes(isChecked);
                }
            }
        });
    }

    private void setOnClickItemListenerStationListView() {
        mStationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                replaceListViewByInfoView(mStationsNetwork.stations.get(position), false);
                //mStationListViewAdapter.setItemSelected(position);
            }
        });
    }

    private void replaceListViewByInfoView(StationItem stationItem, boolean isFromOutsideNearby) {
        mIsFromFavoriteSection = isFromOutsideNearby;
        isStationInfoVisible = true;
        if (isFromOutsideNearby){
            for (StationItem station: mStationsNetwork.stations)
                if (station.getPosition().equals(stationItem.getPosition())) {
                    mCurrentInfoStation = station;
                    break;
                }
        } else {
            mCurrentInfoStation = stationItem;
        }
        getActivity().invalidateOptionsMenu();
        // Switch views
        mStationListViewHolder.setVisibility(View.GONE);
        mStationInfoViewHolder.setVisibility(View.VISIBLE);
        if(mListener != null)
            mListener.onNearbyFragmentInteraction(getString(com.ludoscity.bikeactivityexplorer.R.string.stationDetails), false);
        //Remember the current cameraPosition
        mBackCameraPosition = nearbyMap.getCameraPosition();
        // Hide all ground overlays
        for (StationMapGfx markerData : mMapMarkersGfxData){
            markerData.setGroundOverlayVisible(false);
            markerData.setInfoWindowVisible(false);
        }
        // Show only current one
        //TODO : REFACTOR INFO WINDOW ND LIST WITH FRAGMENTS
        //mCurrentInfoStation.getGroundOverlay().setVisible(true);
        //mCurrentInfoStation.getMarker().showInfoWindow();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(mCurrentInfoStation.getPosition());
        // Direction arrow and distance only available if user location is known
        if(mCurrentUserLatLng != null) {
            boundsBuilder.include(mCurrentUserLatLng);
            mDirectionArrow.setRotation((float) mCurrentInfoStation.getBearingFromLatLng(mCurrentUserLatLng));
            mStationInfoDistanceView.setText(mCurrentInfoStation.getDistanceStringFromLatLng(mCurrentUserLatLng));
            mDirectionArrow.setVisibility(View.VISIBLE);
            mStationInfoDistanceView.setVisibility(View.VISIBLE);
        }else {
            mDirectionArrow.setVisibility(View.INVISIBLE);
            mStationInfoDistanceView.setVisibility(View.INVISIBLE);
        }
        // Set station information
        mStationInfoNameView.setText(mCurrentInfoStation.getName());
        if (mCurrentInfoStation.getFree_bikes() < 2)
            mStationInfoBikeAvailView.setText(mCurrentInfoStation.getFree_bikes() +" "+ getString(com.ludoscity.bikeactivityexplorer.R.string.bikeAvailable_sing));
        else
            mStationInfoBikeAvailView.setText(mCurrentInfoStation.getFree_bikes() +" "+ getString(com.ludoscity.bikeactivityexplorer.R.string.bikesAvailable_plur));

        if (mCurrentInfoStation.getEmpty_slots() < 2)
            mStationInfoParkingAvailView.setText(mCurrentInfoStation.getEmpty_slots()+" "+ getString(com.ludoscity.bikeactivityexplorer.R.string.parkingAvailable_sing));
        else
            mStationInfoParkingAvailView.setText(mCurrentInfoStation.getEmpty_slots()+" "+ getString(com.ludoscity.bikeactivityexplorer.R.string.parkingsAvailable_plur));
        // Move map camera to focus station and user
        nearbyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
        // Set back button to return to normal nearby view with list
        this.getView().setFocusableInTouchMode(true);
        this.getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && isStationInfoVisible) {
                    replaceInfoViewByListView();
                    return !mIsFromFavoriteSection;
                }
                return false;
            }
        });
    }

    private void replaceInfoViewByListView(){
        isStationInfoVisible = false;
        mStationListViewHolder.setVisibility(View.VISIBLE);
        mStationInfoViewHolder.setVisibility(View.GONE);
        // Put 'nearby' as title in the action bar and reset access to drawer
        if (mListener != null)
            mListener.onNearbyFragmentInteraction(getString(com.ludoscity.bikeactivityexplorer.R.string.title_section_nearby), true);
        nearbyMap.animateCamera(CameraUpdateFactory.newCameraPosition(mBackCameraPosition));
        // Restore map
        for (StationMapGfx markerData : mMapMarkersGfxData){
            markerData.setGroundOverlayVisible(true);
            markerData.setInfoWindowVisible(false);
        }

        mCurrentInfoStation = null;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get item selected and deal with it
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mIsFromFavoriteSection){
                    getFragmentManager().popBackStackImmediate();
                    mListener.onNearbyFragmentInteraction(getString(com.ludoscity.bikeactivityexplorer.R.string.title_section_favorites), true);
                    mIsFromFavoriteSection = false;
                }else {
                    replaceInfoViewByListView();
                }
                return true;
            case com.ludoscity.bikeactivityexplorer.R.id.favoriteStar:
                if(isStationInfoVisible)
                    changeFavoriteValue(mCurrentInfoStation.isFavorite());
                return true;
            //////////////
            //Because it's a switch, this calbback don't get invoked, we register a clicklistener directly
            /*case R.id.findBikeParkingSwitchMenuItem:
                //mIsLookingForBikes = !mIsLookingForBikes;
                lookingForBikes(mParkingSwitch.isChecked());
                return true;*/
        }
        return false;
    }

    private void changeFavoriteValue(boolean isFavorite) {
        Toast toast;
        if (!isStationInfoVisible)
            return;
        if(isFavorite){
            mCurrentInfoStation.setFavorite(false);
            mFavoriteStar.setIcon(mIconStarOff);
            toast = Toast.makeText(mContext,getString(com.ludoscity.bikeactivityexplorer.R.string.removedFromFavorites),Toast.LENGTH_SHORT);
        } else {
            mCurrentInfoStation.setFavorite(true);
            mFavoriteStar.setIcon(mIconStarOn);
            toast = Toast.makeText(mContext,getString(com.ludoscity.bikeactivityexplorer.R.string.addedToFavorites),Toast.LENGTH_SHORT);
        }
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MapFragment f = (MapFragment) getActivity().getFragmentManager()
                .findFragmentById(com.ludoscity.bikeactivityexplorer.R.id.mapNearby);
        if (f != null) {
            getActivity().getFragmentManager().beginTransaction().remove(f).commit();
            nearbyMap = null;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        isAlreadyZoomedToUser = false;
        refreshMarkers = true;
        nearbyMap = googleMap;
        nearbyMap.setMyLocationEnabled(true);
        nearbyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 13));
        nearbyMap.setOnMarkerClickListener(this);
        nearbyMap.setOnInfoWindowClickListener(this);
        nearbyMap.setOnMyLocationChangeListener(this);
        nearbyMap.setOnCameraChangeListener(this);
        setupUI();
    }

    private void setRefreshButtonListener() {
        mDownloadBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.Connectivity.isConnected(getContext()) && mDownloadWebTask == null) {
                    mDownloadWebTask = new DownloadWebTask();
                    mDownloadWebTask.execute();
                }
            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        int i = mStationListViewAdapter.getPositionInList(marker);
        mStationListViewAdapter.setItemSelected(i);
        Log.d("onMarkerClick", "Scroll view to " + i);
        if (i != -1) {
            mStationListView.smoothScrollToPositionFromTop(i, 0, 300);
        }
        return false;
    }

    @Override
    public void onMyLocationChange(Location location) {
        if(location != null) {
            Log.d("onMyLocationChange", "new location " + location.toString());
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mStationListViewAdapter != null)
                mStationListViewAdapter.setCurrentUserLatLng(mCurrentUserLatLng);
            if (!isAlreadyZoomedToUser && nearbyMap != null) {
                Log.d("onMyLocationChange","isAlreadyZoomedToUser = "+isAlreadyZoomedToUser);
                nearbyMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrentUserLatLng, 15));
                isAlreadyZoomedToUser = true;
            }
            if (mCurrentInfoStation != null){
                mStationInfoDistanceView.setText(String.valueOf(mCurrentInfoStation.getDistanceStringFromLatLng(mCurrentUserLatLng)));
            }
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.d("CameraZoomLevel", Float.toString(cameraPosition.zoom));
        if (cameraPosition.zoom > mMaxZoom){
            nearbyMap.animateCamera(CameraUpdateFactory.zoomTo(mMaxZoom));
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if(!isStationInfoVisible) {
            for (StationItem station : mStationsNetwork.stations) {
                if (station.getPosition().equals(marker.getPosition())) {
                    replaceListViewByInfoView(station, false);
                    return;
                }
            }
        }
    }

    public void lookingForBikes(boolean isLookingForBikes){
        String toastText;
        Drawable icon;
        mStationListViewAdapter.lookingForBikesNotify(isLookingForBikes);
        if(isLookingForBikes) {
            mBikesOrParkingColumn.setText(com.ludoscity.bikeactivityexplorer.R.string.bikes);
            //Some icons tests
            //((Switch)mParkingSwitch.getActionView().findViewById(R.id.action_bar_find_bike_parking_switch)).setThumbResource(R.drawable.ic_action_find_bike);
            //((Switch)mParkingSwitch.getActionView().findViewById(R.id.action_bar_find_bike_parking_switch)).setTrackResource(R.drawable.ic_action_find_bike);
            //mParkingSwitch.setIcon(R.drawable.ic_action_find_bike);
            toastText = getString(com.ludoscity.bikeactivityexplorer.R.string.findABikes);
            icon = getResources().getDrawable(com.ludoscity.bikeactivityexplorer.R.drawable.bike_icon_toast);
        } else {
            mBikesOrParkingColumn.setText(com.ludoscity.bikeactivityexplorer.R.string.parking);
            //Some icons tests
            //((Switch)mParkingSwitch.getActionView().findViewById(R.id.action_bar_find_bike_parking_switch)).setThumbResource(R.drawable.ic_action_find_dock);
            //((Switch)mParkingSwitch.getActionView().findViewById(R.id.action_bar_find_bike_parking_switch)).setTrackResource(R.drawable.ic_action_find_dock);
            //mParkingSwitch.setIcon(R.drawable.ic_action_find_dock);
            toastText = getString(com.ludoscity.bikeactivityexplorer.R.string.findAParking);
            icon = getResources().getDrawable(com.ludoscity.bikeactivityexplorer.R.drawable.parking_icon_toast);
        }
        // Create a toast with icon and text
        //Todo create this as XML layout
        TextView toastView = new TextView(mContext);
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
        toast.show();

        for (StationMapGfx markerData : mMapMarkersGfxData){
            markerData.updateMarker(isLookingForBikes);
        }
    }

    public void showStationInfoFromFavoriteSection(StationItem stationToShow) {
        mParkingSwitch.setVisible(false);
        replaceListViewByInfoView(stationToShow, true);
    }

    //Pour interaction avec mainActivity
    public interface OnFragmentInteractionListener {
        void onNearbyFragmentInteraction(String title, boolean isNavDrawerEnabled);
    }

    public Context getContext() {
        return mContext;
    }

    public class DownloadWebTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            bixiApiInstance = new BixiAPI(mContext);
            //A Task that launches an other task, ok I want it to show the progress in the user interface
            //I finally advised gainst cvhanging anything, instead I'll add a setting to display Database toast, and OFF by default
            //I do that because it seems it's not blocking / crasing if we try to navigate the interface anyway
            //Let the user choose when to update.
            //TODO : have the auto update function activated through settings, expressed in maximum rotteness of record to be refreshed automatically on NearbyFragment launch
            mStationsNetwork = bixiApiInstance.downloadBixiNetwork();
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
            for (StationItem item : mStationsNetwork.stations){
                mMapMarkersGfxData.add(new StationMapGfx(item));
            }
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
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.edit().putLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, Calendar.getInstance().getTimeInMillis()).apply();
            refreshMarkers = true;
            setupUI();
            Log.d("nearbyFragment",mStationsNetwork.stations.size()+" stations downloaded from citibik.es");

            //must be done last
            mDownloadWebTask = null;
            }
    }
}



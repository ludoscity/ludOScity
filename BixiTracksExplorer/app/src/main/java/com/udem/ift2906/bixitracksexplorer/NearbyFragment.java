package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiAPI;
import com.udem.ift2906.bixitracksexplorer.DBHelper.DBHelper;

import java.util.Calendar;

public class NearbyFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationChangeListener, GoogleMap.OnCameraChangeListener, GoogleMap.OnInfoWindowClickListener {
    private Context mContext;
    private OnFragmentInteractionListener mListener;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String PREF_WEBTASK_LAST_TIMESTAMP_MS = "last_refresh_timestamp";
    private DownloadWebTask mDownloadWebTask;
    private BixiAPI bixiApiInstance;

    private GoogleMap nearbyMap = null;
    private LatLng mCurrentUserLatLng;
    private CameraPosition mBackCameraPosition;
    private float mMaxZoom = 16f;

    private MenuItem mFavoriteStarOn;
    private MenuItem mFavoriteStarOff;
    private MenuItem mParkingSwitch;
    private View mStationInfoViewHolder;
    private View mStationListViewHolder;
    private StationsNetwork mStationsNetwork;
    private StationListViewAdapter mStationListViewAdapter;
    private TextView mBikesOrParkingColumn;
    private ListView mStationListView;
    private StationItem mCurrentInfoStation;
    private TextView mStationInfoNameView;
    private TextView mStationInfoBikeAvailView;
    private TextView mStationInfoParkingAvailView;
    private TextView mStationInfoDistanceView;
    private ImageView mDirectionArrow;
    private TextView mLastUpdatedTextView;
    private ProgressBar mUpdateProgressBar;
    private ImageView mRefreshButton;
    private View mDownloadBar;

    private boolean mIsLookingForBikes;
    private boolean isDownloadCurrentlyExecuting;
    private boolean isStationInfoVisible;
    private boolean isAlreadyZoomedToUser;
    private boolean isMarkersUpdated;

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
        if (sp.getLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, 0) == 0) { //Means ask never successfully completed
            //Because webtask launches DB task, we know that a value there means actual data in the DB
            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();
        }
        else{   //Having a timestamp means some data exists in the db, as both task are intimately linked
            mStationsNetwork = DBHelper.getStationsNetwork();
        }
    }

    //Safe to call from multiple point in code, refreshing the UI elements with the most recent data available
    //Takes care of map readyness check
    //Safely updates everything based on checking the last update timestamp
    private void setupUI(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        long lastRefreshTimestamp = sp.getLong(PREF_WEBTASK_LAST_TIMESTAMP_MS, 0);
        if (lastRefreshTimestamp != 0){
            long now = System.currentTimeMillis();
            long difference = now - lastRefreshTimestamp;
            if (difference < DateUtils.MINUTE_IN_MILLIS)
                mLastUpdatedTextView.setText(getString(R.string.lastUpdated)+" "+ getString(R.string.momentsAgo));
            else
                mLastUpdatedTextView.setText(getString(R.string.lastUpdated)+" "+ Long.toString(difference / DateUtils.MINUTE_IN_MILLIS) +" "+ getString(R.string.minsAgo));

            if(nearbyMap != null) {
                if (mStationsNetwork != null && !isMarkersUpdated) {
                    nearbyMap.clear();
                    mStationsNetwork.addMarkersToMap(nearbyMap);
                    isMarkersUpdated = true;
                }
                mStationListViewAdapter = new StationListViewAdapter(mContext, mStationsNetwork, mCurrentUserLatLng, mIsLookingForBikes);
                mStationListView.setAdapter(mStationListViewAdapter);
            }

        } else{
            mLastUpdatedTextView.setText(getString(R.string.nearbyfragment_default_never_web_updated));
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden){
            ((MainActivity) getActivity()).onSectionHiddenChanged(
                    getArguments().getInt(ARG_SECTION_NUMBER));

        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        try {
            mListener = (OnFragmentInteractionListener) activity;
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        //TODO move this affectation
        mIsLookingForBikes = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mDownloadWebTask != null && !mDownloadWebTask.isCancelled())
        {
            mDownloadWebTask.cancel(false);
            mDownloadWebTask = null;
        }

        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        isMarkersUpdated = false;
        setupUI();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View inflatedView = layoutInflater.inflate(R.layout.fragment_nearby, viewGroup, false);
        if(nearbyMap == null)
            ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.mapNearby)).getMapAsync(this);
        // List view
        mStationListView = (ListView) inflatedView.findViewById(R.id.stationListView);
        setOnClickItemListenerStationListView();
        mStationListViewHolder = inflatedView.findViewById(R.id.stationList);
        mBikesOrParkingColumn = (TextView) inflatedView.findViewById(R.id.bikesOrParkingColumn);
        // Station Info
        mStationInfoViewHolder = inflatedView.findViewById(R.id.stationInfo);
        mDirectionArrow = (ImageView) inflatedView.findViewById(R.id.arrowImage);
        mStationInfoNameView = (TextView) inflatedView.findViewById(R.id.stationInfo_name);
        mStationInfoDistanceView = (TextView) inflatedView.findViewById(R.id.stationInfo_distance);
        mStationInfoBikeAvailView = (TextView) inflatedView.findViewById(R.id.stationInfo_bikeAvailability);
        mStationInfoParkingAvailView = (TextView) inflatedView.findViewById(R.id.stationInfo_parkingAvailability);
        // Update Bar
        mLastUpdatedTextView = (TextView) inflatedView.findViewById(R.id.lastUpdated_textView);
        mLastUpdatedTextView.setTextColor(Color.LTGRAY);
        mUpdateProgressBar = (ProgressBar) inflatedView.findViewById(R.id.refreshDatabase_progressbar);
        mUpdateProgressBar.setVisibility(View.INVISIBLE);
        mRefreshButton = (ImageView) inflatedView.findViewById(R.id.refreshDatabase_button);
        mDownloadBar = inflatedView.findViewById(R.id.downloadBar);
        setRefreshButtonListener();

        setupUI();
        return inflatedView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_nearby,menu);
        mFavoriteStarOn = menu.findItem(R.id.favoriteStarOn);
        mFavoriteStarOff = menu.findItem(R.id.favoriteStarOff);
        mParkingSwitch = menu.findItem(R.id.showParkingAvailability);
        if (!isStationInfoVisible) {
            mFavoriteStarOn.setVisible(false);
            mFavoriteStarOff.setVisible(false);
        } else {
            mParkingSwitch.setVisible(false);
        }
        Log.d("onCreateOptionsMenu","menu created");
    }

    private void setOnClickItemListenerStationListView() {
        mStationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                replaceListViewByInfoView(mStationsNetwork.stations.get(position));
                //mStationListViewAdapter.setItemSelected(position);
            }
        });
    }

    private void replaceListViewByInfoView(StationItem stationItem) {
        isStationInfoVisible = true;
        mCurrentInfoStation = stationItem;
        // Add favorite button
        if (stationItem.isFavorite())
            mFavoriteStarOn.setVisible(true);
        else mFavoriteStarOff.setVisible(true);
        // Switch views
        mStationListViewHolder.setVisibility(View.GONE);
        mStationInfoViewHolder.setVisibility(View.VISIBLE);
        if(mListener != null)
            mListener.onNearbyFragmentInteraction(stationItem.getName(), false);
        //Remember the current cameraPosition
        mBackCameraPosition = nearbyMap.getCameraPosition();
        // Hide all ground overlays
        for (StationItem station: mStationsNetwork.stations) {
            station.getGroundOverlay().setVisible(false);
            station.getMarker().hideInfoWindow();
        }
        // Show only current one
        mCurrentInfoStation.getGroundOverlay().setVisible(true);
        mCurrentInfoStation.getMarker().showInfoWindow();
        // Show InfoWindow only if the station would appear small on the map
        if(mCurrentUserLatLng != null && stationItem.getMeterFromLatLng(mCurrentUserLatLng)>1000)
            mCurrentInfoStation.getMarker().showInfoWindow();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(stationItem.getPosition());
        // Direction arrow and distance only available if user location is known
        if(mCurrentUserLatLng != null) {
            boundsBuilder.include(mCurrentUserLatLng);
            mDirectionArrow.setRotation((float) stationItem.getBearingFromLatLng(mCurrentUserLatLng));
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
            mStationInfoBikeAvailView.setText(mCurrentInfoStation.getFree_bikes() +" "+ getString(R.string.bikeAvailable_sing));
        else
            mStationInfoBikeAvailView.setText(mCurrentInfoStation.getFree_bikes() +" "+ getString(R.string.bikesAvailable_plur));

        if (mCurrentInfoStation.getEmpty_slots() < 2)
            mStationInfoParkingAvailView.setText(mCurrentInfoStation.getEmpty_slots()+" "+ getString(R.string.parkingAvailable_sing));
        else
            mStationInfoParkingAvailView.setText(mCurrentInfoStation.getEmpty_slots()+" "+ getString(R.string.parkingsAvailable_plur));
        // Move map camera to focus station and user
        nearbyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
        // Set back button to return to normal nearby view with list
        this.getView().setFocusableInTouchMode(true);
        this.getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && isStationInfoVisible) {
                    replaceInfoViewByListView();
                    return true;
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
            mListener.onNearbyFragmentInteraction(getString(R.string.title_section_nearby), true);
        nearbyMap.animateCamera(CameraUpdateFactory.newCameraPosition(mBackCameraPosition));
        // Restore map
        for (StationItem station: mStationsNetwork.stations)
            station.getGroundOverlay().setVisible(true);
        mCurrentInfoStation.getMarker().hideInfoWindow();
        mCurrentInfoStation = null;
        // Hide the star
        mFavoriteStarOn.setVisible(false);
        mFavoriteStarOff.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get item selected and deal with it
        switch (item.getItemId()) {
            case android.R.id.home:
                //called when the up affordance/carat in actionbar is pressed
                replaceInfoViewByListView();
                return true;
            case R.id.favoriteStarOn:
                if(isStationInfoVisible)
                    changeFavoriteValue();
                return true;
            case R.id.favoriteStarOff:
                if(isStationInfoVisible)
                    changeFavoriteValue();
                return true;
            case R.id.showParkingAvailability:
                mIsLookingForBikes = !mIsLookingForBikes;
                lookingForBikes(mIsLookingForBikes);
                return true;
        }
        return false;
    }

    private void changeFavoriteValue() {
        Toast toast;
        if (!isStationInfoVisible)
            return;
        if(mCurrentInfoStation.isFavorite()){
            mCurrentInfoStation.setFavorite(false);
            mFavoriteStarOff.setVisible(true);
            mFavoriteStarOn.setVisible(false);
            toast = Toast.makeText(mContext,getString(R.string.removedFromFavorites),Toast.LENGTH_SHORT);
        } else {
            mCurrentInfoStation.setFavorite(true);
            mFavoriteStarOff.setVisible(false);
            mFavoriteStarOn.setVisible(true);
            toast = Toast.makeText(mContext,getString(R.string.addedToFavorites),Toast.LENGTH_SHORT);
        }
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MapFragment f = (MapFragment) getActivity().getFragmentManager()
                .findFragmentById(R.id.mapNearby);
        if (f != null) {
            getActivity().getFragmentManager().beginTransaction().remove(f).commit();
            nearbyMap = null;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        isAlreadyZoomedToUser = false;
        isMarkersUpdated = false;
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
        // TODO maybe not a good idea, the whole view is clickable to start a download
        mDownloadBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDownloadCurrentlyExecuting) {
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
            mStationListView.smoothScrollToPosition(i);
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
                    replaceListViewByInfoView(station);
                    return;
                }
            }
        }
    }

    public void lookingForBikes(boolean isLookingForBikes){
        String toastText;
        Drawable icon;
        mStationListViewAdapter.lookingForBikesNotify(isLookingForBikes);
        Typeface textTypeface = mStationInfoBikeAvailView.getTypeface();
        if(isLookingForBikes) {
            mBikesOrParkingColumn.setText(R.string.bikes);
            mStationInfoBikeAvailView.setTypeface(textTypeface, Typeface.BOLD);
            mStationInfoParkingAvailView.setTypeface(textTypeface, Typeface.NORMAL);
            mParkingSwitch.setIcon(R.drawable.ic_action_find_bike);
            toastText = getString(R.string.findABikes);
            icon = getResources().getDrawable(R.drawable.bike_icon_toast);
        } else {
            mBikesOrParkingColumn.setText(R.string.parking);
            mStationInfoBikeAvailView.setTypeface(textTypeface, Typeface.NORMAL);
            mStationInfoParkingAvailView.setTypeface(textTypeface, Typeface.BOLD);
            mParkingSwitch.setIcon(R.drawable.ic_action_find_dock);
            toastText = getString(R.string.findAParking);
            icon = getResources().getDrawable(R.drawable.parking_icon_toast);
        }
        // Create a toast with icon and text
        //Todo create this as XML layout
        TextView toastView = new TextView(mContext);
        toastView.setAlpha(0.25f);
        toastView.setBackgroundColor(getResources().getColor(R.color.background_floating_material_dark));
        toastView.setShadowLayer(2.75f,0,0,R.color.background_floating_material_dark);
        toastView.setText(toastText);
        toastView.setTextSize(24f);
        toastView.setTextColor(getResources().getColor(R.color.primary_text_default_material_dark));
        toastView.setGravity(Gravity.CENTER);
        icon.setBounds(0,0,64,64);
        toastView.setCompoundDrawables(icon,null,null,null);
        toastView.setCompoundDrawablePadding(16);
        toastView.setPadding(5,5,5,5);
        Toast toast = new Toast(mContext);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL,0,0);
        toast.show();

        for(StationItem station: mStationsNetwork.stations){
            station.updateMarker(isLookingForBikes);
        }
    }

    //Pour interaction avec mainActivity
    public interface OnFragmentInteractionListener {
        public void onNearbyFragmentInteraction(String title,boolean isNavDrawerEnabled);
    }

    public Context getContext() {
        return mContext;
    }

    public class DownloadWebTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            isDownloadCurrentlyExecuting = true;
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
            isDownloadCurrentlyExecuting = false;
            isMarkersUpdated = false;
            setupUI();
        }
    }
}



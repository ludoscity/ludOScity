package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
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


public class NearbyFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationChangeListener, GoogleMap.OnCameraChangeListener, GoogleMap.OnInfoWindowClickListener {
    private Context mContext;
    private OnFragmentInteractionListener mListener;
    private static final String ARG_SECTION_NUMBER = "section_number";

    private GoogleMap nearbyMap = null;
    private LatLng mCurrentUserLatLng;
    private CameraPosition mBackCameraPosition;
    private float mMaxZoom = 16f;

    private StationsNetwork mStationsNetwork;
    private StationItem mCurrentInfoStation;
    private StationListViewAdapter mStationListViewAdapter;

    private ListView mStationListView;
    private TextView mStationNameView;
    private TextView mStationBikeAvailView;
    private TextView mStationParkingAvailView;
    private TextView mLastUpdatedTextView;
    private TextView mBikesOrParkingColumn;
    private ImageButton mRefreshButton;
    private View mStationInfoView;
    private ImageView mDirectionArrow;
    private MenuItem mFavoriteStarOn;
    private MenuItem mFavoriteStarOff;
    private MenuItem mParkingSwitch;

    private DownloadWebTask mDownloadWebTask;
    private BixiAPI bixiApiInstance;
    private boolean mIsLookingForBikes;
    private boolean isDownloadCurrentlyExecuting;
    private boolean isStationInfoVisible;

    public static NearbyFragment newInstance(int sectionNumber) {
        NearbyFragment fragment = new NearbyFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setHasOptionsMenu(true);
        fragment.setArguments(args);
        return fragment;
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

        mDownloadWebTask = new DownloadWebTask();
        mDownloadWebTask.execute();

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
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View inflatedView = layoutInflater.inflate(R.layout.fragment_nearby, viewGroup, false);
        if(nearbyMap == null)
            ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.mapNearby)).getMapAsync(this);
        mRefreshButton = (ImageButton) inflatedView.findViewById(R.id.refreshDatabase_button);
        setRefreshButtonListener();
        mLastUpdatedTextView = (TextView) inflatedView.findViewById(R.id.lastUpdated_textView);
        mStationListView = (ListView) inflatedView.findViewById(R.id.stationListView);
        setOnClickItemListenerStationListView();
        mStationInfoView = inflatedView.findViewById(R.id.stationInfo);
        mDirectionArrow = (ImageView) inflatedView.findViewById(R.id.arrowImage);
        mStationNameView = (TextView) inflatedView.findViewById(R.id.stationInfo_name);
        mStationBikeAvailView = (TextView) inflatedView.findViewById(R.id.stationInfo_bikeAvailability);
        mStationParkingAvailView = (TextView) inflatedView.findViewById(R.id.stationInfo_parkingAvailability);
        mBikesOrParkingColumn = (TextView) inflatedView.findViewById(R.id.bikesOrParkingColumn);
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
                mStationListViewAdapter.setItemSelected(position);
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
        mStationListView.setVisibility(View.GONE);
        mStationInfoView.setVisibility(View.VISIBLE);
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
        // Direction arrow only available if user location is known
        if(mCurrentUserLatLng != null) {
            boundsBuilder.include(mCurrentUserLatLng);
            mDirectionArrow.setRotation((float) stationItem.getBearingFromLatLng(mCurrentUserLatLng));
        }else
            mDirectionArrow.setVisibility(View.INVISIBLE);
        // Set station information
        mStationNameView.setText(mCurrentInfoStation.getName());
        if (mCurrentInfoStation.getFree_bikes() < 2)
            mStationBikeAvailView.setText(mCurrentInfoStation.getFree_bikes() +" "+ getString(R.string.bikeAvailable_sing));
        else
            mStationBikeAvailView.setText(mCurrentInfoStation.getFree_bikes() +" "+ getString(R.string.bikesAvailable_plur));
        if (mCurrentInfoStation.getEmpty_slots() < 2)
            mStationParkingAvailView.setText(mCurrentInfoStation.getEmpty_slots()+" "+ getString(R.string.parkingAvailable_sing));
        else
            mStationParkingAvailView.setText(mCurrentInfoStation.getEmpty_slots()+" "+ getString(R.string.parkingsAvailable_plur));
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
        mStationListView.setVisibility(View.VISIBLE);
        mStationInfoView.setVisibility(View.GONE);
        // Put 'nearby' as title in the action bar and reset access to drawer
        mListener.onNearbyFragmentInteraction(getString(R.string.title_section_nearby), true);
        nearbyMap.animateCamera(CameraUpdateFactory.newCameraPosition(mBackCameraPosition));
        // Restore map
        for (StationItem station: mStationsNetwork.stations)
            station.getGroundOverlay().setVisible(true);
        mCurrentInfoStation.getMarker().hideInfoWindow();
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
        if (f != null)
            getActivity().getFragmentManager().beginTransaction().remove(f).commit();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        nearbyMap = googleMap;
        nearbyMap.setMyLocationEnabled(true);
        if (mCurrentUserLatLng != null)
            nearbyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentUserLatLng, 15));
        else nearbyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 13));
        nearbyMap.setOnMarkerClickListener(this);
        nearbyMap.setOnInfoWindowClickListener(this);
        nearbyMap.setOnMyLocationChangeListener(this);
        nearbyMap.setOnCameraChangeListener(this);
    }

    private void setRefreshButtonListener() {
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
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
            Log.d("onMyLocationChange", "new location "+location.toString());
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if(mStationListViewAdapter != null)
                mStationListViewAdapter.setCurrentUserLatLng(mCurrentUserLatLng);
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

        if(isLookingForBikes) {
            mBikesOrParkingColumn.setText(R.string.bikes);
            mParkingSwitch.setIcon(R.drawable.ic_action_find_bike);
            toastText = getString(R.string.findABikes);
            icon = getResources().getDrawable(R.drawable.bike_icon_toast);
        } else {
            mBikesOrParkingColumn.setText(R.string.parking);
            mParkingSwitch.setIcon(R.drawable.ic_action_find_dock);
            toastText = getString(R.string.findAParkings);
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
            mStationsNetwork = bixiApiInstance.downloadBixiNetwork();
            return null;
        }

        @Override
        protected void onCancelled (Void aVoid){
            super.onCancelled(aVoid);
            //Do nothing. task is cancelled if fragment is detached
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(mContext, R.string.download_success, Toast.LENGTH_SHORT).show();

            //TODO : What if map is not ready when we're done here
            mStationsNetwork.addMarkersToMap(nearbyMap);
            if(mCurrentUserLatLng != null)
                nearbyMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrentUserLatLng, 15));
            mStationListViewAdapter = new StationListViewAdapter(mContext, mStationsNetwork, mCurrentUserLatLng, mIsLookingForBikes);
            mStationListView.setAdapter(mStationListViewAdapter);
            //TODO add time awareness
            mLastUpdatedTextView.setText(getString(R.string.lastUpdated) +" "+ getString(R.string.momentsAgo));
            mLastUpdatedTextView.setTextColor(Color.LTGRAY);
            isDownloadCurrentlyExecuting = false;
        }
    }
}



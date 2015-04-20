package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationChangeListener {
    private Context mContext;
    private GoogleMap nearbyMap = null;
    private LatLng mCurrentUserLatLng;

    private OnFragmentInteractionListener mListener;
    private StationListViewAdapter mStationListViewAdapter;
    private ListView mStationListView;
    private StationsNetwork mStationsNetwork;

    private static final String ARG_SECTION_NUMBER = "section_number";

    private TextView mLastUpdatedTextView;
    private ImageButton mRefreshButton;
    private View mStationInfo;
    private boolean isStationInfoVisible;
    private ImageView mDirectionArrow;
    private boolean isDownloadCurrentlyExecuting;

    private DownloadWebTask mDownloadWebTask;
    private BixiAPI bixiApiInstance;
    private CameraPosition mBackCameraPosition;

    public static NearbyFragment newInstance(int sectionNumber) {
        NearbyFragment fragment = new NearbyFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
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
        mStationInfo = inflatedView.findViewById(R.id.stationInfo);
        mDirectionArrow = (ImageView) inflatedView.findViewById(R.id.arrowImage);
        return inflatedView;
    }

    private void setOnClickItemListenerStationListView() {
        mStationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                replaceListViewByInfoView(mStationsNetwork.stations.get(position));
            }
        });
    }

    private void replaceListViewByInfoView(StationItem stationItem) {
        isStationInfoVisible = true;
        mStationListView.setVisibility(View.GONE);
        mStationInfo.setVisibility(View.VISIBLE);
        mListener.onNearbyFragmentInteraction(stationItem.getName(), false);
        //Remember the current cameraPosition
        mBackCameraPosition = nearbyMap.getCameraPosition();
        //Build new map
        nearbyMap.clear();
        nearbyMap.addMarker(stationItem.getMarkerOptions());
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(stationItem.getPosition());
        setCurrentLocation();
        if(mCurrentUserLatLng != null) {
            boundsBuilder.include(mCurrentUserLatLng);
            mDirectionArrow.setRotation((float) stationItem.getBearingFromLatLng(mCurrentUserLatLng));
        }else
            mDirectionArrow.setVisibility(View.INVISIBLE);
        // Move map camera to focus station and user
        nearbyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(),50));
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
        mStationInfo.setVisibility(View.GONE);
        // Put 'nearby' as title in the action bar and reset access to drawer
        mListener.onNearbyFragmentInteraction(getString(R.string.title_section_nearby), true);
        nearbyMap.animateCamera(CameraUpdateFactory.newCameraPosition(mBackCameraPosition));
        nearbyMap.clear();
        mStationsNetwork.addMarkersToMap(nearbyMap);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get item selected and deal with it
        switch (item.getItemId()) {
            case android.R.id.home:
                //called when the up affordance/carat in actionbar is pressed
                replaceInfoViewByListView();
                return true;
        }
        return false;
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
        setCurrentLocation();
        if (mCurrentUserLatLng != null)
            nearbyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentUserLatLng, 15));
        else nearbyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 13));
        nearbyMap.setOnMarkerClickListener(this);
        nearbyMap.setOnMyLocationChangeListener(this);
    }


    private void setRefreshButtonListener() {
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isDownloadCurrentlyExecuting) {
                    mDownloadWebTask = new DownloadWebTask();
                    mDownloadWebTask.execute();
                }
            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        int i = mStationListViewAdapter.getPositionInList(marker);
        Log.d("onMarkerClick","Scroll view to " + i);
        if (i!= -1)
            mStationListView.smoothScrollToPosition(i);
        return false;
    }

    public void setCurrentLocation() {
        Location location = nearbyMap.getMyLocation();
        if (location != null)
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
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
            mStationListViewAdapter = new StationListViewAdapter(mContext, mStationsNetwork, mCurrentUserLatLng);
            mStationListView.setAdapter(mStationListViewAdapter);
            //TODO add time awareness
            mLastUpdatedTextView.setText(getString(R.string.lastUpdated) + "1 min ago");
            isDownloadCurrentlyExecuting = false;
        }
    }
}



package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiAPI;


public class NearbyFragment extends Fragment
        implements OnMapReadyCallback {
    private GoogleMap nearbyMap = null;
    private BixiAPI bixiApiInstance;
    private LatLng mCurrentUserLatLng;
    private LocationManager mLocationManager;

    private Context mContext;

    private OnFragmentInteractionListener mListener;
    private StationListViewAdapter mStationListViewAdapter;
    private ListView mStationListView;
    private StationsNetwork mStationsNetwork;

    private DownloadWebTask mDownloadWebTask;

    private static final String ARG_SECTION_NUMBER = "section_number";
    private ImageButton mRefreshButton;
    private TextView mLastUpdatedTextView;
    private Boolean isDownloadCurrentlyExecuting;


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

        setCurrentLocation();
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
        return inflatedView;
    }

    private void setOnClickItemListenerStationListView() {
        mStationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StationItem stationClicked = mStationsNetwork.stations.get(position);
                long uid = stationClicked.getUid();
                String stationName = stationClicked.getName();
                mListener.onNearbyFragmentInteraction(uid, stationName);
            }
        });
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
        nearbyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 10));
    }

    public void setCurrentLocation() {
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
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

    //Pour interaction avec mainActivity
    public interface OnFragmentInteractionListener {
        public void onNearbyFragmentInteraction(long uid, String stationName);
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
            mStationsNetwork.setUpMarkers();
            mStationsNetwork.addMarkersToMap(nearbyMap);

            mStationListViewAdapter = new StationListViewAdapter(mContext, mStationsNetwork, mCurrentUserLatLng);
            mStationListView.setAdapter(mStationListViewAdapter);

            //TODO add time awareness
            mLastUpdatedTextView.setText(R.string.lastUpdated + "1 min ago");

            isDownloadCurrentlyExecuting = false;
        }
    }
}



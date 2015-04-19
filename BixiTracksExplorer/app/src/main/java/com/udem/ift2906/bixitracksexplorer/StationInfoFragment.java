package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.udem.ift2906.bixitracksexplorer.DBHelper.DBHelper;

public class StationInfoFragment extends Fragment implements OnMapReadyCallback {
    private static final String ARG_stationUID = "stationUID";
    private static Context mContext;
    private String TAG = "stationInfo";

    private OnFragmentInteractionListener mListener;
    private StationItem mStationItem;
    private ImageView mDirectionArrow;
    private LocationManager mLocationManager;
    private LatLng mCurrentUserLatLng;

    public static StationInfoFragment newInstance(long stationUID, Context _context) {
        StationInfoFragment fragment = new StationInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_stationUID, stationUID);
        mContext = _context;
        fragment.setArguments(args);
        return fragment;
    }

    public StationInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long mStationUID = getArguments().getLong(ARG_stationUID);
        Log.d(TAG, "Requesting station from DBHelper: " + mStationUID);
        mStationItem = DBHelper.getStation(mStationUID);
        setCurrentLocation();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.fragment_station_info, container, false);
        mDirectionArrow = (ImageView) inflatedView.findViewById(R.id.arrow_container);
        double rotation = mStationItem.getBearingFromLatLng(mCurrentUserLatLng);
        mDirectionArrow.setRotation((float)rotation);

        // Inflate the layout for this fragment
        return inflatedView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    public void setCurrentLocation() {
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onStationInfoFragmentInteraction(Uri uri);
    }

}

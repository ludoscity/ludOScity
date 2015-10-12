package com.ludoscity.findmybikes;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnStationInfoFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StationInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StationInfoFragment extends Fragment {

    private static final String ARG_STATION_ITEM = "station_item";
    private static final String ARG_USER_LATLNG = "user_latlng";


    ///////////////////////////////////////////////////3
    private TextView mStationInfoNameView;
    private TextView mStationInfoBikeAvailView;
    private TextView mStationInfoParkingAvailView;
    private TextView mStationInfoDistanceView;
    private ImageView mDirectionArrow;

    private LatLng mUserLatLng;
    private StationItem mStationItem;
    private MenuItem mFavoriteStar;

    private int mIconStarOn = com.ludoscity.findmybikes.R.drawable.abc_btn_rating_star_on_mtrl_alpha;
    private int mIconStarOff = com.ludoscity.findmybikes.R.drawable.abc_btn_rating_star_off_mtrl_alpha;

    private OnStationInfoFragmentInteractionListener mListener;


    public static StationInfoFragment newInstance(StationItem station, LatLng userLatLng) {
        StationInfoFragment fragment = new StationInfoFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_STATION_ITEM, station);
        args.putParcelable(ARG_USER_LATLNG, userLatLng);

        fragment.setArguments(args);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    public StationInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mStationItem = getArguments().getParcelable(ARG_STATION_ITEM);
            mUserLatLng = getArguments().getParcelable(ARG_USER_LATLNG);
        }

        ((BaseActivity)getActivity()).setActivityTitle(getString(R.string.stationDetails));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView =  inflater.inflate(R.layout.fragment_station_info, container, false);
        mDirectionArrow = (ImageView) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.arrowImage);
        mStationInfoNameView = (TextView) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.stationInfo_name);
        mStationInfoDistanceView = (TextView) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.stationInfo_distance);
        mStationInfoBikeAvailView = (TextView) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.stationInfo_bikeAvailability);
        mStationInfoParkingAvailView = (TextView) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.stationInfo_parkingAvailability);

        setupUI();

        return inflatedView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_station_info, menu);

        mFavoriteStar = menu.findItem(com.ludoscity.findmybikes.R.id.favoriteStar);

        if (mStationItem.isFavorite(getActivity())){
            mFavoriteStar.setIcon(mIconStarOn);
        }
        else{
            mFavoriteStar.setIcon(mIconStarOff);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.favoriteStar:
                switchFavoriteState();
                return true;
        }

        return false;
    }

    private void switchFavoriteState(){
        Toast toast;

        boolean newState = !mStationItem.isFavorite(getActivity());

        mStationItem.setFavorite(newState, getActivity());

        if (newState){
            mFavoriteStar.setIcon(mIconStarOn);
            toast = Toast.makeText(getActivity().getApplicationContext(),getString(com.ludoscity.findmybikes.R.string.favorite_added),Toast.LENGTH_SHORT);
        }
        else{
            mFavoriteStar.setIcon(mIconStarOff);
            toast = Toast.makeText(getActivity().getApplicationContext(),getString(com.ludoscity.findmybikes.R.string.favorite_removed),Toast.LENGTH_SHORT);
        }

        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnStationInfoFragmentInteractionListener) activity;
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

    public void updateUserLatLng(LatLng userLatLng){
        mUserLatLng = userLatLng;

        if (isAdded()){
            setupUI();
        }
    }

    public void setupUI() {

        if(mUserLatLng != null) {
            mDirectionArrow.setRotation((float) mStationItem.getBearingFromLatLng(mUserLatLng));
            mStationInfoDistanceView.setText(mStationItem.getDistanceStringFromLatLng(mUserLatLng));
            mDirectionArrow.setVisibility(View.VISIBLE);
            mStationInfoDistanceView.setVisibility(View.VISIBLE);
        }else {
            mDirectionArrow.setVisibility(View.INVISIBLE);
            mStationInfoDistanceView.setVisibility(View.INVISIBLE);
        }

        mStationInfoNameView.setText(mStationItem.getName());

        if (mStationItem.getFree_bikes() < 2)
            mStationInfoBikeAvailView.setText(mStationItem.getFree_bikes() +" "+ getString(com.ludoscity.findmybikes.R.string.bikeAvailable_sing));
        else
            mStationInfoBikeAvailView.setText(mStationItem.getFree_bikes() +" "+ getString(com.ludoscity.findmybikes.R.string.bikesAvailable_plur));

        if (mStationItem.getEmpty_slots() < 2)
            mStationInfoParkingAvailView.setText(mStationItem.getEmpty_slots()+" "+ getString(com.ludoscity.findmybikes.R.string.parkingAvailable_sing));
        else
            mStationInfoParkingAvailView.setText(mStationItem.getEmpty_slots()+" "+ getString(com.ludoscity.findmybikes.R.string.parkingsAvailable_plur));
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
    public interface OnStationInfoFragmentInteractionListener {
        // TODO: Update argument type and name
        void onStationInfoFragmentInteraction(Uri uri);
    }

}

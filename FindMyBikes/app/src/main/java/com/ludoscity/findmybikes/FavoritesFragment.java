package com.ludoscity.findmybikes;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.Helpers.DBHelper;

import java.util.ArrayList;

public class FavoritesFragment extends Fragment {
    private Context mContext;
    private OnFragmentInteractionListener mListener;
    private View mFavoritesView;
    private TextView mNoFavoritesView;

    private static final String ARG_SECTION_NUMBER = "section_number";
    private LocationManager mLocationManager;
    private LatLng mCurrentUserLatLng;
    private FavoritesListViewAdapter mFavoritesStationListViewAdapter;
    private boolean hasFavorite;
    private ListView mFavoritesList;
    private ArrayList<StationItem> mStationsFavorites;

    public interface OnFragmentInteractionListener {
        void onFavoritesFragmentInteraction(StationItem stationToShow);
    }

    public static FavoritesFragment newInstance(int sectionNumber) {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {   //Only happening on orientation change now
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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        //setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = layoutInflater.inflate(com.ludoscity.findmybikes.R.layout.fragment_favoris, container, false);
        mFavoritesView = inflatedView.findViewById(com.ludoscity.findmybikes.R.id.favoritesListView_Holder);
        mNoFavoritesView = (TextView) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.noFavorite_holder);
        mFavoritesList = (ListView) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.favorites_listView);
        setUpUI();
        return inflatedView;
    }

    public void setUpUI(){
        try {
            mStationsFavorites = DBHelper.getFavoriteStations(getActivity());
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        mFavoritesStationListViewAdapter = new FavoritesListViewAdapter(mContext,mStationsFavorites,mCurrentUserLatLng);
        mFavoritesList.setAdapter(mFavoritesStationListViewAdapter);
        setOnClickItemListenerStationListView();
        setCurrentLocation();
        hasFavorite = !mStationsFavorites.isEmpty();
        // Show the correct view if user has favorites or not
        if (!hasFavorite) {
            mNoFavoritesView.setVisibility(View.VISIBLE);
            mFavoritesView.setVisibility(View.GONE);
        } else {
            mNoFavoritesView.setVisibility(View.GONE);
            mFavoritesView.setVisibility(View.VISIBLE);
        }
    }

    public void setCurrentLocation() {
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
    }

    private void setOnClickItemListenerStationListView() {
        mFavoritesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StationItem stationToShow = mStationsFavorites.get(position);
                mListener.onFavoritesFragmentInteraction(stationToShow);
            }
        });
    }
}




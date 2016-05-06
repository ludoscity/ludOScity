package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationRecyclerViewAdapter;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.DividerItemDecoration;
import com.ludoscity.findmybikes.utils.ScrollingLinearLayoutManager;
import com.ludoscity.findmybikes.utils.Utils;

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class StationListFragment extends Fragment
            implements StationRecyclerViewAdapter.OnStationListItemClickListener{

    public static final String STATION_LIST_ITEM_CLICK_PATH = "station_list_item_click";
    public static final String STATION_LIST_INACTIVE_ITEM_CLICK_PATH = "station_list_inactive_item_click";
    public static final String STATION_LIST_FAVORITE_FAB_CLICK_PATH = "station_list_fav_fab_click";
    public static final String STATION_LIST_STATION_RECAP_FAVORITE_FAB_CLICK_PATH = "station_list_station_recap_fav_fab_click";
    public static final String STATION_LIST_DIRECTIONS_FAB_CLICK_PATH = "station_list_dir_fab_click";
    public static final String STATION_LIST_ARG_BACKGROUND_RES_ID = "station_list_arg_background_res_id";

    private RecyclerView mStationRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private int mRecyclerViewScrollingState = SCROLL_STATE_IDLE;
    private TextView mEmptyListTextView;
    private View mProximityHeader;
    private View mStationRecap;
    private TextView mStationRecapName;
    private TextView mStationRecapAvailability;
    private FloatingActionButton mStationRecapFavoriteFab;
    private ImageView mProximityHeaderFromImageView;
    private ImageView mProximityHeaderToImageView;
    private TextView mAvailabilityTextView;

    private StationItem mStationToRecap;

    private OnStationListFragmentInteractionListener mListener;

    private StationRecyclerViewAdapter getStationRecyclerViewAdapter(){
        return (StationRecyclerViewAdapter)mStationRecyclerView.getAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView =  inflater.inflate(R.layout.fragment_station_list, container, false);
        mEmptyListTextView = (TextView) inflatedView.findViewById(R.id.empty_list_text);
        mStationRecap = inflatedView.findViewById(R.id.station_recap);
        mStationRecapName = (TextView) inflatedView.findViewById(R.id.station_recap_name);
        mStationRecapAvailability = (TextView) inflatedView.findViewById(R.id.station_recap_availability);
        mStationRecapFavoriteFab = (FloatingActionButton) inflatedView.findViewById(R.id.station_recap_favorite_fab);
        setupStationRecapFavoriteFab();
        mStationRecyclerView = (RecyclerView) inflatedView.findViewById(R.id.station_list_recyclerview);
        mStationRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        //mStationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mStationRecyclerView.setLayoutManager(new ScrollingLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false, 300));
        mStationRecyclerView.setAdapter(new StationRecyclerViewAdapter(this, getContext()));
        mStationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                mRecyclerViewScrollingState = newState;

            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) inflatedView.findViewById(R.id.station_list_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener((SwipeRefreshLayout.OnRefreshListener) getActivity());
        mSwipeRefreshLayout.setColorSchemeResources(R.color.stationlist_refresh_spinner_red,
                R.color.stationlist_refresh_spinner_yellow,
                R.color.stationlist_refresh_spinner_grey,
                R.color.stationlist_refresh_spinner_green);

        mAvailabilityTextView = (TextView) inflatedView.findViewById(R.id.availability_header);
        mProximityHeader = inflatedView.findViewById(R.id.proximity_header);
        mProximityHeaderFromImageView = (ImageView) inflatedView.findViewById(R.id.proximity_header_from);
        mProximityHeaderToImageView = (ImageView) inflatedView.findViewById(R.id.proximity_header_to);

        Bundle args = getArguments();
        if (args != null){

            mStationRecyclerView.setBackground(ContextCompat.getDrawable(this.getContext(), args.getInt(STATION_LIST_ARG_BACKGROUND_RES_ID)));
            mProximityHeader.setVisibility(View.GONE);
        }

        return inflatedView;
    }

    private void setupStationRecapFavoriteFab() {

        mStationRecapFavoriteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StationListFragment.this.onStationListItemClick(StationListFragment.STATION_LIST_STATION_RECAP_FAVORITE_FAB_CLICK_PATH);
                //ordering matters
                if (mStationToRecap.isFavorite(getContext()))
                    mStationRecapFavoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp);
                else
                    mStationRecapFavoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnStationListFragmentInteractionListener) activity;
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("selected_pos", getStationRecyclerViewAdapter().getSelectedPos());
        outState.putParcelable("sort_reference_LatLng", getStationRecyclerViewAdapter().getSortReferenceLatLng());
        outState.putParcelable("distance_reference_latlng", getStationRecyclerViewAdapter().getDistanceReferenceLatLng());
        outState.putString("string_if_empty", mEmptyListTextView.getText().toString());
        outState.putString("station_recap_name", mStationRecapName.getText().toString());
        outState.putString("station_recap_availability_string", mStationRecapAvailability.getText().toString());
        outState.putInt("station_recap_availability_color", mStationRecapAvailability.getCurrentTextColor());
        outState.putBoolean("station_recap_visible", mStationRecap.getVisibility() == View.VISIBLE);

        getStationRecyclerViewAdapter().saveStationList(outState);
        getStationRecyclerViewAdapter().saveLookingForBike(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {

            int selectedPos = savedInstanceState.getInt("selected_pos");

            if (selectedPos != NO_POSITION)
                getStationRecyclerViewAdapter().setSelectedPos(selectedPos, false);

            LatLng sortReferenceLatLng = savedInstanceState.getParcelable("sort_reference_LatLng");
            LatLng distanceReferenceLatLng = savedInstanceState.getParcelable("distance_reference_latlng");
            ArrayList<StationItem> stationList = savedInstanceState.getParcelableArrayList("stationitem_arraylist");

            setupUI(stationList, savedInstanceState.getBoolean("looking_for_bike"),
                    savedInstanceState.getString("string_if_empty"),
                    sortReferenceLatLng, distanceReferenceLatLng);

            if (!savedInstanceState.getBoolean("station_recap_visible")) {
                mStationRecap.setVisibility(View.GONE);
            }

            mStationRecapName.setText(savedInstanceState.getString("station_recap_name"));
            mStationRecapAvailability.setText(savedInstanceState.getString("station_recap_availability_string"));

            if (savedInstanceState.getInt("station_recap_availability_color") == ContextCompat.getColor(getContext(), R.color.station_recap_green))
                mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.station_recap_green));
            else
                mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.station_recap_yellow));
        }
    }

    public void setupUI(ArrayList<StationItem> stationsNetwork, boolean lookingForBike, String stringIfEmpty,
                        LatLng _sortReferenceLatLng, LatLng _distanceReferenceLatLng) {

        if (stationsNetwork != null) {

            //TODO: fix glitch when coming back from place widget
            if (!stationsNetwork.isEmpty()) {
                mStationRecyclerView.setVisibility(View.VISIBLE);
                mEmptyListTextView.setVisibility(View.GONE);
                mStationRecap.setVisibility(View.GONE);
            }
            else{
                mStationRecyclerView.setVisibility(View.GONE);
                mEmptyListTextView.setText(stringIfEmpty);
                mEmptyListTextView.setVisibility(View.VISIBLE);
                mStationRecap.setVisibility(View.VISIBLE);
            }

            getStationRecyclerViewAdapter().setupStationList(stationsNetwork, _sortReferenceLatLng, _distanceReferenceLatLng);
            lookingForBikes(lookingForBike);
        }
    }

    public void hideStationRecap(){
        mStationRecap.setVisibility(View.GONE);
    }

    public void showStationRecap(){
        mStationRecap.setVisibility(View.VISIBLE);
    }

    public void setupStationRecap(StationItem _station){

        mStationToRecap = _station;

        if (_station.isFavorite(getContext())) {
            mStationRecapFavoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp);
            mStationRecapName.setText(_station.getFavoriteName(getContext(), true));
        }
        else {
            mStationRecapFavoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
            mStationRecapName.setText(_station.getName());
        }


        mStationRecapAvailability.setText(String.format(getResources().getString(R.string.station_recap_bikes), _station.getFree_bikes()));

        mStationRecapAvailability.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.theme_window_background));

        if (_station.getFree_bikes() <= DBHelper.getCriticalAvailabilityMax(getContext())){

            mStationRecapAvailability.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.station_recap_error));
            mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.theme_textcolor_primary));
        }
        else if (_station.getFree_bikes() <= DBHelper.getBadAvailabilityMax(getContext()))
            mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.station_recap_yellow));
        else
            mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.station_recap_green));
    }

    public void setDistanceSortReferenceLatLngAndSort(LatLng _toSet) {

        if (mRecyclerViewScrollingState == SCROLL_STATE_IDLE) {

            getStationRecyclerViewAdapter().setDistanceSortReferenceLatLngAndSortIfRequired(_toSet, false);
        }
    }

    public void setDistanceDisplayReferenceLatLng(LatLng _toSet, boolean _notify) {
        getStationRecyclerViewAdapter().setDistanceDisplayReferenceLatLng(_toSet, _notify);
    }

    public String retrieveClosestRawIdAndAvailability(boolean _lookingForBike){

        return getStationRecyclerViewAdapter().retrieveClosestRawIdAndAvailability(_lookingForBike);

    }

    public LatLng getClosestAvailabilityLatLng(boolean _lookingForBike){
        return getStationRecyclerViewAdapter().getClosestAvailabilityLatLng(_lookingForBike);
    }

    public LatLng getDistanceDisplayReference(){
        return getStationRecyclerViewAdapter().getDistanceDisplayReference();
    }

    public boolean isRecyclerViewReadyForItemSelection(){
        return mStationRecyclerView != null && getStationRecyclerViewAdapter().getSortReferenceLatLng() != null &&
                ((ScrollingLinearLayoutManager)mStationRecyclerView.getLayoutManager()).findFirstVisibleItemPosition() !=
                        NO_POSITION;
    }

    public boolean highlightStation(String _stationId) {

        int selectedPos = getStationRecyclerViewAdapter().setSelection(_stationId, false);

        ((StationRecyclerViewAdapter)mStationRecyclerView.getAdapter()).requestFabAnimation();

        return selectedPos != NO_POSITION;
    }

    public StationItem getHighlightedStation(){

        return getStationRecyclerViewAdapter().getSelected();
    }

    public void removeStationHighlight(){
        getStationRecyclerViewAdapter().clearSelection();
    }

    public void lookingForBikes(boolean lookingForBike) {

        getStationRecyclerViewAdapter().lookingForBikesNotify(lookingForBike);

        if (lookingForBike) {

            mAvailabilityTextView.setText(getString(R.string.bikes));

            if (getArguments()!= null){
                mProximityHeader.setVisibility(View.GONE);
            }
            else {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mProximityHeaderFromImageView.setPaddingRelative(0,0, Utils.dpToPx(1.f,getContext()),0);
                } else {
                    mProximityHeaderFromImageView.setPadding(0,0, Utils.dpToPx(1.f,getContext()),0);
                }

                mProximityHeaderFromImageView.setVisibility(View.GONE);
                mProximityHeaderToImageView.setImageResource(R.drawable.ic_walking_24dp_white);

                mProximityHeader.setVisibility(View.VISIBLE);
            }
        }
        else {

            mAvailabilityTextView.setText(getString(R.string.docks));

            if (getArguments()!= null){
                mProximityHeader.setVisibility(View.GONE);
            }
            else {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mProximityHeaderToImageView.setPaddingRelative(Utils.dpToPx(3.f,getContext()),0,0,0);
                } else {
                    mProximityHeaderToImageView.setPadding(Utils.dpToPx(3.f,getContext()),0,0,0);
                }

                mProximityHeaderFromImageView.setVisibility(View.VISIBLE);
                mProximityHeaderFromImageView.setImageResource(R.drawable.ic_pin_a_24dp_white);
                mProximityHeaderToImageView.setImageResource(R.drawable.ic_biking_24dp_white);

                mProximityHeader.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onStationListItemClick(String _path) {
        Uri.Builder builder = new Uri.Builder();

        builder.appendPath(_path);

        if (mListener != null) {
            mListener.onStationListFragmentInteraction(builder.build());
        }
    }

    public void setRefreshing(boolean toSet) {
        if (toSet != mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(toSet);
    }

    public void setRefreshEnable(boolean toSet) {
        mSwipeRefreshLayout.setEnabled(toSet);
    }

    public void smoothScrollSelectionInView(boolean _appBarExpanded) {
        //Not very proud of the defensive coding but some code path which are required do call this in invalid contexts
        if (getStationRecyclerViewAdapter().getSelectedPos() != NO_POSITION) {
            if (_appBarExpanded && getStationRecyclerViewAdapter().getSelectedPos() >=
                    ((LinearLayoutManager) mStationRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition()) {
                mStationRecyclerView.smoothScrollToPosition(getStationRecyclerViewAdapter().getSelectedPos() + 1);
            } else
                mStationRecyclerView.smoothScrollToPosition(getStationRecyclerViewAdapter().getSelectedPos());
        }
    }

    public boolean isHighlightedVisibleInRecyclerView() {
        return  getStationRecyclerViewAdapter().getSelectedPos() <
                        ((LinearLayoutManager)mStationRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition()-1 && //Minus 1 is for appbar
                getStationRecyclerViewAdapter().getSelectedPos() >=
                        ((LinearLayoutManager)mStationRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
    }

    public void setResponsivenessToClick(boolean _toSet) {
        getStationRecyclerViewAdapter().setClickResponsiveness(_toSet);
    }

    public void notifyDatasetChangedToRecyclerView() {
        getStationRecyclerViewAdapter().notifyDataSetChanged();
    }

    public interface OnStationListFragmentInteractionListener {

        void onStationListFragmentInteraction(Uri uri);
    }

}

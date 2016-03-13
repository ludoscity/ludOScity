package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class StationListFragment extends Fragment
            implements StationRecyclerViewAdapter.OnStationListItemClickListener{

    public static final String STATION_LIST_ITEM_CLICK_PATH = "station_list_item_click";
    public static final String STATION_LIST_ARG_BACKGROUND_RES_ID = "station_list_arg_background_res_id";

    private RecyclerView mStationRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private int mRecyclerViewScrollingState = SCROLL_STATE_IDLE;
    private TextView mEmptyListTextView;
    private ImageView mProximityHeaderImageView;
    private TextView mProximityHeaderTextView;
    private TextView mAvailabilityTextView;

    private OnStationListFragmentInteractionListener mListener;

    private StationRecyclerViewAdapter getRecyclerViewAdapter(){
        return (StationRecyclerViewAdapter)mStationRecyclerView.getAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView =  inflater.inflate(R.layout.fragment_station_list, container, false);
        mEmptyListTextView = (TextView) inflatedView.findViewById(R.id.empty_list_text);
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
        mSwipeRefreshLayout.setColorSchemeResources(R.color.stationlist_refresh_spinner_green, R.color.stationlist_refresh_spinner_yellow, R.color.stationlist_refresh_spinner_red);

        mAvailabilityTextView = (TextView) inflatedView.findViewById(R.id.availability_header);
        mProximityHeaderImageView = (ImageView) inflatedView.findViewById(R.id.time_header_iv);
        mProximityHeaderTextView = (TextView) inflatedView.findViewById(R.id.time_header_tv);

        Bundle args = getArguments();
        if (args != null){

            mStationRecyclerView.setBackground(ContextCompat.getDrawable(this.getContext(), args.getInt(STATION_LIST_ARG_BACKGROUND_RES_ID)));
            mProximityHeaderImageView.setVisibility(View.GONE);
            mProximityHeaderTextView.setVisibility(View.GONE);
        }

        return inflatedView;
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

        outState.putInt("selected_pos", getRecyclerViewAdapter().getSelectedPos());
        outState.putParcelable("user_current_LatLng", getRecyclerViewAdapter().getCurrentUserLatLng());

        getRecyclerViewAdapter().saveStationList(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {

            int selectedPos = savedInstanceState.getInt("selected_pos");

            if (selectedPos != NO_POSITION)
                getRecyclerViewAdapter().setSelectedPos(selectedPos, false);

            LatLng currentUserLatLng = savedInstanceState.getParcelable("user_current_LatLng");

            getRecyclerViewAdapter().setCurrentUserLatLng(currentUserLatLng, false);

            ArrayList<StationItem> stationList = savedInstanceState.getParcelableArrayList("stationitem_arraylist");
            getRecyclerViewAdapter().setupStationList(stationList);
        }
    }

    public void setupUI(ArrayList<StationItem> stationsNetwork, boolean lookingForBike, String stringIfEmpty) {

        if (stationsNetwork != null) {

            if (!stationsNetwork.isEmpty()) {
                mStationRecyclerView.setVisibility(View.VISIBLE);
                mEmptyListTextView.setVisibility(View.GONE);
                getRecyclerViewAdapter().setupStationList(stationsNetwork);
            }
            else{
                mStationRecyclerView.setVisibility(View.GONE);
                mEmptyListTextView.setText(stringIfEmpty);
                mEmptyListTextView.setVisibility(View.VISIBLE);

            }

            lookingForBikes(lookingForBike);
        }
    }

    public void setCurrentUserLatLng(LatLng currentUserLatLng) {

        if (mRecyclerViewScrollingState == SCROLL_STATE_IDLE) {

            getRecyclerViewAdapter().setCurrentUserLatLng(currentUserLatLng, true);
        }
    }

    public boolean highlightStationFromName(String stationName) {

        boolean toReturn = false;

        int selectedPos = getRecyclerViewAdapter().setSelectionFromName(stationName, false);

        if (selectedPos != NO_POSITION) {
            mStationRecyclerView.smoothScrollToPosition(selectedPos);
            toReturn = true;
        }

        return toReturn;
    }

    public StationItem getHighlightedStation(){

        return getRecyclerViewAdapter().getSelected();
    }

    public void removeStationHighlight(){
        getRecyclerViewAdapter().clearSelection();
    }

    public void lookingForBikes(boolean lookingForBike) {

        getRecyclerViewAdapter().lookingForBikesNotify(lookingForBike);

        if (lookingForBike) {

            mAvailabilityTextView.setText(getString(R.string.bikes));

            if (getArguments()!= null){
                mProximityHeaderTextView.setVisibility(View.GONE);
                mProximityHeaderImageView.setVisibility(View.GONE);
            }
            else if (DBHelper.getWalkingProximityAsDistance(this.getContext())){
                mProximityHeaderTextView.setVisibility(View.VISIBLE);
                mProximityHeaderImageView.setVisibility(View.GONE);
            }
            else {
                mProximityHeaderTextView.setVisibility(View.GONE);
                mProximityHeaderImageView.setVisibility(View.VISIBLE);
                mProximityHeaderImageView.setImageDrawable(ContextCompat.getDrawable(this.getContext(), R.drawable.ic_walking));
            }
        }
        else {

            mAvailabilityTextView.setText(getString(R.string.parking));

            if (getArguments()!= null){
                mProximityHeaderTextView.setVisibility(View.GONE);
                mProximityHeaderImageView.setVisibility(View.GONE);
            }
            else if (DBHelper.getBikingProximityAsDistance(this.getContext())){
                mProximityHeaderTextView.setVisibility(View.VISIBLE);
                mProximityHeaderImageView.setVisibility(View.GONE);
            }
            else {
                mProximityHeaderTextView.setVisibility(View.GONE);
                mProximityHeaderImageView.setVisibility(View.VISIBLE);
                mProximityHeaderImageView.setImageDrawable(ContextCompat.getDrawable(this.getContext(), R.drawable.ic_biking));
            }
        }
    }

    @Override
    public void onStationListItemClick() {
        Uri.Builder builder = new Uri.Builder();

        builder.appendPath(STATION_LIST_ITEM_CLICK_PATH);

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

    public void removeStation(StationItem toRemove, String stringIfEmpty) {
        if (getRecyclerViewAdapter().removeItem(toRemove)){
            mStationRecyclerView.setVisibility(View.GONE);
            mEmptyListTextView.setText(stringIfEmpty);
            mEmptyListTextView.setVisibility(View.VISIBLE);
        }
    }

    public void addStation(StationItem toAdd) {
        mEmptyListTextView.setVisibility(View.GONE);
        mStationRecyclerView.setVisibility(View.VISIBLE);
        getRecyclerViewAdapter().addItem(toAdd);
    }

    public interface OnStationListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onStationListFragmentInteraction(Uri uri);
    }

}
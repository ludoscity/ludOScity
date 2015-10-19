package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationRecyclerViewAdapter;
import com.ludoscity.findmybikes.utils.DividerItemDecoration;
import com.ludoscity.findmybikes.utils.ScrollingLinearLayoutManager;

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class StationListFragment extends Fragment
            implements StationRecyclerViewAdapter.OnStationListItemClickListener{

    public static final String STATION_LIST_ITEM_CLICK_PATH = "station_list_item_click";

    private TextView mBikesOrParkingHeader;
    private TextView mDistanceHeader;
    private RecyclerView mStationRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private int mRecyclerViewScrollingState = SCROLL_STATE_IDLE;

    private OnStationListFragmentInteractionListener mListener;

    private StationRecyclerViewAdapter getRecyclerViewAdapter(){
        return (StationRecyclerViewAdapter)mStationRecyclerView.getAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView =  inflater.inflate(R.layout.fragment_station_list, container, false);
        mStationRecyclerView = (RecyclerView) inflatedView.findViewById(R.id.station_list_recyclerview);
        mStationRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        //mStationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mStationRecyclerView.setLayoutManager(new ScrollingLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false, 300));
        mStationRecyclerView.setAdapter(new StationRecyclerViewAdapter(this));
        mStationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                mRecyclerViewScrollingState = newState;

            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) inflatedView.findViewById(R.id.station_list_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener((SwipeRefreshLayout.OnRefreshListener)getActivity());
        mSwipeRefreshLayout.setColorSchemeResources(R.color.stationlist_refresh_spinner_green, R.color.stationlist_refresh_spinner_yellow, R.color.stationlist_refresh_spinner_red);

        mBikesOrParkingHeader = (TextView) inflatedView.findViewById(R.id.station_list_bike_parking_header);
        mDistanceHeader = (TextView) inflatedView.findViewById(R.id.station_list_distance_header);

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

        LatLng currentUserLatLng = null;

        if (savedInstanceState != null) {

            int selectedPos = savedInstanceState.getInt("selected_pos");

            if (selectedPos != NO_POSITION)
                getRecyclerViewAdapter().setSelectedPos(selectedPos, false);

            currentUserLatLng = savedInstanceState.getParcelable("user_current_LatLng");

            getRecyclerViewAdapter().setCurrentUserLatLng(currentUserLatLng, false);

            ArrayList<StationItem> stationList = savedInstanceState.getParcelableArrayList("stationitem_arraylist");
            getRecyclerViewAdapter().setupStationList(stationList);
        }

        if (currentUserLatLng == null)
            mDistanceHeader.setVisibility(View.GONE);
        else
            mDistanceHeader.setVisibility(View.VISIBLE);
    }

    public void setupUI(ArrayList<StationItem> stationsNetwork, boolean lookingForBike) {

        if (stationsNetwork != null) {
            getRecyclerViewAdapter().setupStationList(stationsNetwork);
            lookingForBikes(lookingForBike);
        }
    }

    public void setCurrentUserLatLng(LatLng currentUserLatLng) {

        if (mRecyclerViewScrollingState == SCROLL_STATE_IDLE) {

            getRecyclerViewAdapter().setCurrentUserLatLng(currentUserLatLng, true);
            mDistanceHeader.setVisibility(View.VISIBLE);
        }
    }

    public void highlightStationFromName(String stationName) {

        mStationRecyclerView.smoothScrollToPosition(getRecyclerViewAdapter().setSelectionFromName(stationName, false));
    }

    public StationItem getHighlightedStation(){

        return getRecyclerViewAdapter().getSelected();
    }

    public void removeStationHighlight(){
        getRecyclerViewAdapter().clearSelection();
    }

    public void lookingForBikes(boolean lookingForBike) {

        getRecyclerViewAdapter().lookingForBikesNotify(lookingForBike);

        if (lookingForBike)
            mBikesOrParkingHeader.setText(R.string.bikes);
        else
            mBikesOrParkingHeader.setText(R.string.parking);
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
        mSwipeRefreshLayout.setRefreshing(toSet);
    }

    public void setRefreshEnable(boolean toSet) {
        mSwipeRefreshLayout.setEnabled(toSet);
    }

    public interface OnStationListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onStationListFragmentInteraction(Uri uri);
    }

}

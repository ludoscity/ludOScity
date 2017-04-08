package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.graphics.Typeface;
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

import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.RootApplication;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationRecyclerViewAdapter;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.DividerItemDecoration;
import com.ludoscity.findmybikes.utils.ScrollingLinearLayoutManager;

import java.util.ArrayList;
import java.util.Comparator;

import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class StationListFragment extends Fragment
            implements StationRecyclerViewAdapter.OnStationListItemClickListener{

    public static final String STATION_LIST_ITEM_CLICK_PATH = "station_list_item_click";
    public static final String STATION_LIST_INACTIVE_ITEM_CLICK_PATH = "station_list_inactive_item_click";
    public static final String STATION_LIST_FAVORITE_FAB_CLICK_PATH = "station_list_fav_fab_click";
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
    private ImageView mProximityHeaderFromImageView;
    private ImageView mProximityHeaderToImageView;
    private TextView mAvailabilityTextView;

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
        Comparator<StationItem> comparator = getStationRecyclerViewAdapter().getSortComparator();
        if (comparator instanceof StationRecyclerViewAdapter.DistanceComparator)
            outState.putParcelable("sort_comparator", (StationRecyclerViewAdapter.DistanceComparator) comparator);
        else
            outState.putParcelable("sort_comparator", (StationRecyclerViewAdapter.TotalTripTimeComparator)comparator);
        outState.putString("string_if_empty", mEmptyListTextView.getText().toString());
        outState.putString("station_recap_name", mStationRecapName.getText().toString());
        outState.putString("station_recap_availability_string", mStationRecapAvailability.getText().toString());
        outState.putInt("station_recap_availability_color", mStationRecapAvailability.getCurrentTextColor());
        outState.putBoolean("station_recap_visible", mStationRecap.getVisibility() == View.VISIBLE);
        outState.putBoolean("proximity_header_visible", mProximityHeader.getVisibility() == View.VISIBLE);
        outState.putInt("proximity_header_from_icon_resid", mProximityHeaderFromImageView.getTag() == null ? -1 : (Integer)mProximityHeaderFromImageView.getTag());
        outState.putInt("proximity_header_to_icon_resid", mProximityHeaderToImageView.getTag() == null ? -1 : (Integer)mProximityHeaderToImageView.getTag());

        getStationRecyclerViewAdapter().saveLookingForBike(outState);
        getStationRecyclerViewAdapter().saveIsAvailabilityOutdated(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {

            Comparator<StationItem> comparator = savedInstanceState.getParcelable("sort_comparator");

            if (savedInstanceState.getBoolean("availability_outdated")) {
                getStationRecyclerViewAdapter().setAvailabilityOutdated(true);
                mStationRecapAvailability.getPaint().setStrikeThruText(true);
                mStationRecapAvailability.getPaint().setTypeface(Typeface.DEFAULT);
            }
            else{
                getStationRecyclerViewAdapter().setAvailabilityOutdated(false);
                mStationRecapAvailability.getPaint().setStrikeThruText(false);
                mStationRecapAvailability.getPaint().setTypeface(Typeface.DEFAULT_BOLD);
            }

            setupUI(RootApplication.getBikeNetworkStationList(), savedInstanceState.getBoolean("looking_for_bike"),
                    savedInstanceState.getBoolean("proximity_header_visible"),
                    savedInstanceState.getInt("proximity_header_from_icon_resid") == -1 ? null : savedInstanceState.getInt("proximity_header_from_icon_resid"),
                    savedInstanceState.getInt("proximity_header_to_icon_resid") == -1 ? null : savedInstanceState.getInt("proximity_header_to_icon_resid"),
                    savedInstanceState.getString("string_if_empty"),
                    comparator);

            int selectedPos = savedInstanceState.getInt("selected_pos");

            if (selectedPos != NO_POSITION)
                getStationRecyclerViewAdapter().setSelectedPos(selectedPos, false);

            mStationRecapName.setText(savedInstanceState.getString("station_recap_name"));
            mStationRecapAvailability.setText(savedInstanceState.getString("station_recap_availability_string"));

            mStationRecapAvailability.setTextColor(savedInstanceState.getInt("station_recap_availability_color"));

            if (savedInstanceState.getBoolean("station_recap_visible")) {
                mStationRecyclerView.setVisibility(View.GONE);
                mEmptyListTextView.setVisibility(View.VISIBLE);
                mStationRecap.setVisibility(View.VISIBLE);
            }
            else{
                mStationRecyclerView.setVisibility(View.VISIBLE);
                mEmptyListTextView.setVisibility(View.GONE);
                mStationRecap.setVisibility(View.GONE);
            }
        }
    }

    public void setupUI(ArrayList<StationItem> _stationsNetwork, boolean _lookingForBike, boolean _showProximity,
                        Integer _headerFromIconResId, Integer _headerToIconResId,
                        String _stringIfEmpty,
                        Comparator<StationItem> _sortComparator) {

        //TODO: fix glitch when coming back from place widget (Note to past self : describe glitch)
        mEmptyListTextView.setText(_stringIfEmpty);
        if (!_stationsNetwork.isEmpty()) {
            mStationRecyclerView.setVisibility(View.VISIBLE);
            mEmptyListTextView.setVisibility(View.GONE);
            mStationRecap.setVisibility(View.GONE);
        }
        else{
            mStationRecyclerView.setVisibility(View.GONE);
            mEmptyListTextView.setVisibility(View.VISIBLE);
            mStationRecap.setVisibility(View.VISIBLE);
        }

        getStationRecyclerViewAdapter().setupStationList(_stationsNetwork, _sortComparator);
        getStationRecyclerViewAdapter().setShowProximity(_showProximity);
        setupHeaders(_lookingForBike, _showProximity, _headerFromIconResId, _headerToIconResId);
    }

    public void hideStationRecap(){
        mStationRecap.setVisibility(View.GONE);
    }

    public void showStationRecap(){
        mStationRecap.setVisibility(View.VISIBLE);
    }

    public boolean setupStationRecap(StationItem _station, boolean _outdated){

        if (getContext() == null)
            return false;

        if (_station.isFavorite(getContext())) {
            mStationRecapName.setText(_station.getFavoriteName(getContext(), true));
        }
        else {
            mStationRecapName.setText(_station.getName());
        }

        mStationRecapAvailability.setText(String.format(getResources().getString(R.string.station_recap_bikes), _station.getFree_bikes()));

        if (_outdated){
            mStationRecapAvailability.getPaint().setStrikeThruText(true);
            mStationRecapAvailability.getPaint().setTypeface(Typeface.DEFAULT);
            mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.theme_accent));
        }
        else {

            mStationRecapAvailability.getPaint().setTypeface(Typeface.DEFAULT_BOLD);
            mStationRecapAvailability.getPaint().setStrikeThruText(false);

            if (_station.getFree_bikes() <= DBHelper.getCriticalAvailabilityMax(getContext()))
                mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.station_recap_red));
            else if (_station.getFree_bikes() <= DBHelper.getBadAvailabilityMax(getContext()))
                mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.station_recap_yellow));
            else
                mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.station_recap_green));

        }

        return true;
    }

    public void setSortComparatorAndSort(Comparator<StationItem> _toSet){

        if (mRecyclerViewScrollingState == SCROLL_STATE_IDLE) {

            getStationRecyclerViewAdapter().setStationSortComparatorAndSort(_toSet);
        }
    }

    //_stationALatLng can be null
    public void updateTotalTripSortComparator(LatLng _userLatLng, LatLng _stationALatLng){
        getStationRecyclerViewAdapter().updateTotalTripSortComparator(_userLatLng, _stationALatLng);
    }

    public String retrieveClosestRawIdAndAvailability(boolean _lookingForBike){

        return getStationRecyclerViewAdapter().retrieveClosestRawIdAndAvailability(_lookingForBike);

    }

    public LatLng getClosestAvailabilityLatLng(boolean _lookingForBike){
        return getStationRecyclerViewAdapter().getClosestAvailabilityLatLng(_lookingForBike);
    }

    public boolean isRecyclerViewReadyForItemSelection(){
        return mStationRecyclerView != null && getStationRecyclerViewAdapter().getSortComparator() != null &&
                ((ScrollingLinearLayoutManager)mStationRecyclerView.getLayoutManager()).findFirstVisibleItemPosition() !=
                        NO_POSITION;
    }

    public ViewTarget getHighlightedFavoriteFabViewTarget(){

        return getStationRecyclerViewAdapter().getSelectedItemFavoriteFabViewTarget(mStationRecyclerView);
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

    public void setupHeaders(boolean lookingForBike, boolean _showProximityHeader, Integer _headerFromIconResId, Integer _headerToIconResId) {

        getStationRecyclerViewAdapter().lookingForBikesNotify(lookingForBike);

        if (lookingForBike) {

            mAvailabilityTextView.setText(getString(R.string.bikes));

            if (getArguments()!= null){
                mProximityHeader.setVisibility(View.GONE);
            }
            else {

                mProximityHeaderFromImageView.setVisibility(View.GONE);
                mProximityHeaderFromImageView.setTag(-1);
                mProximityHeaderToImageView.setTag(_headerToIconResId);
                mProximityHeaderToImageView.setImageResource(_headerToIconResId);

                mProximityHeader.setVisibility(View.VISIBLE);
            }
        }
        else {

            mAvailabilityTextView.setText(getString(R.string.docks));

            if (getArguments()!= null || !_showProximityHeader){
                mProximityHeader.setVisibility(View.INVISIBLE);
            }
            else {

                mProximityHeaderFromImageView.setVisibility(View.VISIBLE);
                mProximityHeaderFromImageView.setImageResource(_headerFromIconResId);
                mProximityHeaderToImageView.setImageResource(_headerToIconResId);
                mProximityHeaderFromImageView.setTag(_headerFromIconResId);
                mProximityHeaderToImageView.setTag(_headerToIconResId);

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

    public void notifyStationChanged(String _stationId) {
        getStationRecyclerViewAdapter().notifyStationChanged(_stationId);
    }

    public void setOutdatedData(boolean _availabilityOutdated) {
        //TODO: refactor with MVC in mind. Outdated status is model
        if (_availabilityOutdated){
            mStationRecapAvailability.getPaint().setStrikeThruText(true);
            mStationRecapAvailability.getPaint().setTypeface(Typeface.DEFAULT);
            mStationRecapAvailability.setTextColor(ContextCompat.getColor(getContext(), R.color.theme_accent));
        }
        getStationRecyclerViewAdapter().setAvailabilityOutdated(_availabilityOutdated);
    }

    public void showFavoriteHeader() {

        mProximityHeaderToImageView.setImageResource(R.drawable.ic_pin_favorite_24dp_white);
    }

    public interface OnStationListFragmentInteractionListener {

        void onStationListFragmentInteraction(Uri uri);
    }

}

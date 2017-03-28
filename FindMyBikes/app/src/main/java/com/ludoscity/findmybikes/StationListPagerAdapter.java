package com.ludoscity.findmybikes;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.utils.SmartFragmentPagerAdapter;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by F8Full on 2015-10-19.
 * Adapter for view pager displaying station lists
 */
public class StationListPagerAdapter extends SmartFragmentPagerAdapter {

    @SuppressWarnings("FieldCanBeLocal")
    private static int NUM_ITEMS = 2;

    public static int BIKE_STATIONS = 0;
    public static int DOCK_STATIONS = 1;

    public StationListPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        /*Fragment toReturn;
        if (position == BIKE_STATIONS)
            toReturn = new StationListFragment();
        else {
            toReturn = new StationListFragment();
            Bundle args = new Bundle();
            args.putInt(StationListFragment.STATION_LIST_ARG_BACKGROUND_RES_ID, R.drawable.ic_favorites_background);
            toReturn.setArguments(args);
        }*/

        return new StationListFragment();
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    public void setupUI(int _pageID, ArrayList<StationItem> _stationsList, boolean _showProximity,
                        Integer _headerFromIconResId, Integer _headerToIconResId,
                        String _stringIfEmpty,
                        Comparator<StationItem> _sortComparator){
        retrieveListFragment(_pageID).setupUI(_stationsList, _pageID == BIKE_STATIONS,
                _showProximity, _headerFromIconResId, _headerToIconResId,
                _stringIfEmpty, _sortComparator);
    }

    public void hideStationRecap(){
        retrieveListFragment(DOCK_STATIONS).hideStationRecap();
    }

    public void showStationRecap(){
        retrieveListFragment(DOCK_STATIONS).showStationRecap();
    }

    public void setRefreshEnableAll(boolean toSet) {
        retrieveListFragment(BIKE_STATIONS).setRefreshEnable(toSet);
        retrieveListFragment(DOCK_STATIONS).setRefreshEnable(toSet);
    }

    private StationListFragment retrieveListFragment(int position){
        return ((StationListFragment)getRegisteredFragment(position));
    }

    public StationItem getHighlightedStationForPage(int position) {
        StationItem toReturn = null;

        if (isViewPagerReady())
            toReturn = retrieveListFragment(position).getHighlightedStation();

        return toReturn;
    }

    public LatLng getClosestBikeLatLng(){
        return retrieveListFragment(BIKE_STATIONS).getClosestAvailabilityLatLng(true);
    }

    public void setCurrentUserLatLng(LatLng currentUserLatLng) {
        if (isViewPagerReady()) {
            retrieveListFragment(BIKE_STATIONS).setSortComparatorAndSort(new StationRecyclerViewAdapter.DistanceComparator(currentUserLatLng));
            retrieveListFragment(DOCK_STATIONS).updateTotalTripSortComparator(currentUserLatLng, null);
        }
    }

    public void notifyStationChangedAll(String _stationId) {
        retrieveListFragment(BIKE_STATIONS).notifyStationChanged(_stationId);
        retrieveListFragment(DOCK_STATIONS).notifyStationChanged(_stationId);
    }

    public String retrieveClosestRawIdAndAvailability(boolean _lookingForBike){
        String toReturn = null;

        if (isViewPagerReady()){
            if (_lookingForBike)
                toReturn = retrieveListFragment(BIKE_STATIONS).retrieveClosestRawIdAndAvailability(true);
            else
                toReturn = retrieveListFragment(DOCK_STATIONS).retrieveClosestRawIdAndAvailability(false);
        }

        return toReturn;
    }

    public void highlightStationforId(boolean _lookingForBike, String _stationId){

        if (isViewPagerReady()){
            if (_lookingForBike)
                retrieveListFragment(BIKE_STATIONS).highlightStation(_stationId);
            else
                retrieveListFragment(DOCK_STATIONS).highlightStation(_stationId);
        }
    }

    public boolean isRecyclerViewReadyForItemSelection(int pageID){
        return retrieveListFragment(pageID).isRecyclerViewReadyForItemSelection();
    }

    public boolean highlightStationForPage(String _stationId, int _pagePosition) {
        return retrieveListFragment(_pagePosition).highlightStation(_stationId);
    }

    public void removeStationHighlightForPage(int position) {
        retrieveListFragment(position).removeStationHighlight();
    }

    public void setRefreshingAll(boolean toSet) {
        retrieveListFragment(BIKE_STATIONS).setRefreshing(toSet);
        retrieveListFragment(DOCK_STATIONS).setRefreshing(toSet);
    }

    public void notifyStationAUpdate(LatLng _newALatLng, LatLng _userLatLng) {
        retrieveListFragment(DOCK_STATIONS).updateTotalTripSortComparator(_userLatLng, _newALatLng);
    }

    public void smoothScrollHighlightedInViewForPage(int _pageID, boolean _appBarExpanded) {
        retrieveListFragment(_pageID).smoothScrollSelectionInView(_appBarExpanded);
    }

    public boolean isHighlightedVisibleInRecyclerView() {
        return retrieveListFragment(BIKE_STATIONS).isHighlightedVisibleInRecyclerView();
    }

    public void setupBTabStationARecap(StationItem _stationA, boolean _outdated) {
        retrieveListFragment(DOCK_STATIONS).setupStationRecap(_stationA, _outdated);
    }

    public void setClickResponsivenessForPage(int _pageID, boolean _toSet) {
        retrieveListFragment(_pageID).setResponsivenessToClick(_toSet);
    }

    public void setOutdatedDataAll(boolean _isDataOutdated){
        retrieveListFragment(BIKE_STATIONS).setOutdatedData(_isDataOutdated);
        retrieveListFragment(DOCK_STATIONS).setOutdatedData(_isDataOutdated);
    }

    public void showFavoriteHeaderInBTab() {
        retrieveListFragment(DOCK_STATIONS).showFavoriteHeader();
    }
}

package com.ludoscity.findmybikes;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.utils.SmartFragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Created by F8Full on 2015-10-19.
 * Adapter for view pager displaying station lists
 */
public class StationListPagerAdapter extends SmartFragmentPagerAdapter {

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

    public void setupUI(int _pageID, ArrayList<StationItem> _stationsList, String _stringIfEmpty,
                        LatLng _sortReferenceLatLng, LatLng _distanceReferenceLatLng){
        retrieveListFragment(_pageID).setupUI(_stationsList, _pageID == BIKE_STATIONS, _stringIfEmpty, _sortReferenceLatLng, _distanceReferenceLatLng);
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

    public void setCurrentUserLatLng(LatLng currentUserLatLng) {
        if (isViewPagerReady()) {
            //We don't notify as the call to setDistanceSortReferenceLatLngAndSort will do it
            retrieveListFragment(BIKE_STATIONS).setDistanceDisplayReferenceLatLng(currentUserLatLng, false);
            retrieveListFragment(BIKE_STATIONS).setDistanceSortReferenceLatLngAndSort(currentUserLatLng);
        }
    }

    public String highlightClosestStationWithAvailability(boolean _lookingForBike){
        String toReturn = null;

        if (isViewPagerReady()){
            if (_lookingForBike)
                toReturn = retrieveListFragment(BIKE_STATIONS).highlightClosestStationWithAvailability(true);
            else
                toReturn = retrieveListFragment(DOCK_STATIONS).highlightClosestStationWithAvailability(false);
        }

        return toReturn;
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

    public void removeStationForPage(int position, StationItem station, String stringIfEmpty) {
        retrieveListFragment(position).removeStation(station, stringIfEmpty);
    }

    public void addStationForPage(int position, StationItem station) {
        retrieveListFragment(position).addStation(station);
    }

    public void notifyStationAUpdate(LatLng _newALatLng) {
        retrieveListFragment(DOCK_STATIONS).setDistanceDisplayReferenceLatLng(_newALatLng, true);
    }

    public void smoothScrollHighlightedInViewForPage(int _pageID, boolean _appBarExpanded) {
        retrieveListFragment(_pageID).smoothScrollSelectionInView(_appBarExpanded);
    }
}

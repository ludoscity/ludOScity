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

    public void setupUIAll(ArrayList<StationItem> nearbyStations, ArrayList<StationItem> favoriteStations,
                           String noFavoritesString) {
        retrieveListFragment(BIKE_STATIONS).setupUI(nearbyStations, true, "");
        retrieveListFragment(DOCK_STATIONS).setupUI(new ArrayList<StationItem>(), false, "Pick station");
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
            retrieveListFragment(BIKE_STATIONS).setCurrentUserLatLng(currentUserLatLng);
            retrieveListFragment(DOCK_STATIONS).setCurrentUserLatLng(currentUserLatLng);
        }
    }

    public boolean highlightStationFromNameForPage(String stationName, int position) {
        return retrieveListFragment(position).highlightStationFromName(stationName);
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
}

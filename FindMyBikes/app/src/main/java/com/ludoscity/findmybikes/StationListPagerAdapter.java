package com.ludoscity.findmybikes;

import android.content.Context;
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

    public static int ALL_STATIONS = 0;
    public static int FAVORITE_STATIONS = 1;

    private static final int[] TABS_TITLE_RES_ID = new int[]{
            R.string.title_section_nearby,
            R.string.title_section_favorites
    };

    private Context mCtx;

    public StationListPagerAdapter(FragmentManager fm, Context _ctx) {
        super(fm);
        mCtx = _ctx;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment toReturn;
        if (position == ALL_STATIONS)
            toReturn = new StationListFragment();
        else
            toReturn = new StationListFragment();

        return toReturn;
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    public void setupUIAll(ArrayList<StationItem> nearbyStations, ArrayList<StationItem> favoriteStations,
                           String noFavoritesString, boolean lookingForBike) {
        retrieveListFragment(ALL_STATIONS).setupUI(nearbyStations, lookingForBike, "");
        retrieveListFragment(FAVORITE_STATIONS).setupUI(favoriteStations, lookingForBike, noFavoritesString);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mCtx.getString(TABS_TITLE_RES_ID[position]);
    }

    public void setRefreshEnableAll(boolean toSet) {
        retrieveListFragment(ALL_STATIONS).setRefreshEnable(toSet);
        retrieveListFragment(FAVORITE_STATIONS).setRefreshEnable(toSet);
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

    public void setCurrentUserLatLngForNearby(LatLng currentUserLatLng) {
        if (isViewPagerReady())
            retrieveListFragment(ALL_STATIONS).setCurrentUserLatLng(currentUserLatLng);
    }

    public boolean highlightStationFromNameForPage(String stationName, int position) {
        return retrieveListFragment(position).highlightStationFromName(stationName);
    }

    public void removeStationHighlightForPage(int position) {
        retrieveListFragment(position).removeStationHighlight();
    }

    public void lookingForBikesAll(boolean lookingForBikes) {
        retrieveListFragment(ALL_STATIONS).lookingForBikes(lookingForBikes);
        retrieveListFragment(FAVORITE_STATIONS).lookingForBikes(lookingForBikes);
    }

    public void setRefreshingAll(boolean toSet) {
        retrieveListFragment(ALL_STATIONS).setRefreshing(toSet);
        retrieveListFragment(FAVORITE_STATIONS).setRefreshing(toSet);
    }

    public void removeStationForPage(int position, StationItem station, String stringIfEmpty) {
        retrieveListFragment(position).removeStation(station, stringIfEmpty);
    }

    public void addStationForPage(int position, StationItem station) {
        retrieveListFragment(position).addStation(station);
    }
}

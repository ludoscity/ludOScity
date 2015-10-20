package com.ludoscity.findmybikes;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.ludoscity.findmybikes.fragments.FavoritesFragment;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.utils.SmartFragmentPagerAdapter;

/**
 * Created by F8Full on 2015-10-19.
 * Adapter for view pager displaying station lists
 */
public class StationListPagerAdapter extends SmartFragmentPagerAdapter {

    private static int NUM_ITEMS = 2;

    public static int ALL_STATIONS = 0;
    public static int FAVORITE_STATIONS = 1;

    public StationListPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment toReturn;
        if (position == ALL_STATIONS)
            toReturn = new StationListFragment();
        else
            toReturn = new FavoritesFragment();

        return toReturn;
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }
}

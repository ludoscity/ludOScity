package com.ludoscity.findmybikes;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

/**
 * Created by F8Full on 2015-08-09.
 * Activity used to display the favorites section
 */
public class FavoritesActivity extends BaseActivity
        implements FavoritesFragment.OnFragmentInteractionListener{

    private FavoritesFragment mFavoritesFragment = null;

    @Override
    protected int getSelfNavDrawerItem() { return NAVDRAWER_ITEM_FAVORITES; }


    @Override
    public void onFavoritesFragmentInteraction(StationItem stationToShow) {
        //This should run a special intent to launch NearbyActivity displaying correct data
        //From MainActivity
        //mNearbyFragment.showStationInfoFromFavoriteSection(stationToShow);
        //invalidateOptionsMenu();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_favorites);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        setActivityTitle(getTitle());
        setActivitySubtitle("");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setActivityTitle(getString(R.string.title_section_favorites));

        mFavoritesFragment = (FavoritesFragment)getSupportFragmentManager().findFragmentById(
                R.id.favorites_fragment);
        //mNearbyFragment.setHasOptionsMenu(true);


        //if (mNearbyFragment != null && savedInstanceState == null) {
        //    Bundle args = intentToFragmentArguments(getIntent());
        //    mNearbyFragment.reloadFromArguments(args);
        //}
    }
}

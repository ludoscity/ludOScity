package com.ludoscity.findmybikes.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.ludoscity.findmybikes.fragments.FavoritesFragment;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;

/**
 * Created by F8Full on 2015-08-09.
 * Activity used to display the favorites section
 */
public class FavoritesActivity extends BaseActivity
        implements FavoritesFragment.OnFragmentInteractionListener {

    @Override
    protected int getSelfNavDrawerItem() { return NAVDRAWER_ITEM_FAVORITES; }


    @Override
    public void onFavoritesFragmentInteraction(StationItem stationToShow) {

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
    }
}

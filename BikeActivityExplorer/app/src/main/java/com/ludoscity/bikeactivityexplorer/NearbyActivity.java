package com.ludoscity.bikeactivityexplorer;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.couchbase.lite.CouchbaseLiteException;
import com.ludoscity.bikeactivityexplorer.DBHelper.DBHelper;

import java.io.IOException;

/**
 * Created by F8Full on 2015-07-26.
 * Activity used to display the nearby section
 */
public class NearbyActivity extends BaseActivity
        implements NearbyFragment.OnFragmentInteractionListener {

    public static Resources resources;

    private NearbyFragment mNearbyFragment = null;

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_NEARBY;
    }

    @Override
    public void onNearbyFragmentInteraction(String title, boolean isDrawerIndicatorEnabled) {
        setActivityTitle(title);

        mDrawerToggle.setDrawerIndicatorEnabled(isDrawerIndicatorEnabled);
        restoreActionBar();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resources = getResources();

        //Read app params and apply them
        /*if (getResources().getBoolean(R.bool.allow_portrait)) {
            if (!getResources().getBoolean(R.bool.allow_landscape)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            if (getResources().getBoolean(R.bool.allow_landscape)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }*/


        setContentView(R.layout.activity_nearby);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        setActivityTitle(getTitle());
        setActivitySubtitle("");

        //Initialize couchbase database
        try {
            DBHelper.init(this, this);
            BixiTracksExplorerAPIHelper.init();
        } catch (IOException | CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setActivityTitle(getString(R.string.title_section_nearby));

        mNearbyFragment = (NearbyFragment)getSupportFragmentManager().findFragmentById(
                R.id.nearby_fragment);
        mNearbyFragment.setHasOptionsMenu(true);


        //if (mNearbyFragment != null && savedInstanceState == null) {
        //    Bundle args = intentToFragmentArguments(getIntent());
        //    mNearbyFragment.reloadFromArguments(args);
        //}
    }
}

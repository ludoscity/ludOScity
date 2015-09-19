package com.ludoscity.bikeactivityexplorer;

import android.content.res.Resources;
import android.net.Uri;
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
        implements NearbyFragment.OnFragmentInteractionListener,
        StationMapFragment.OnStationMapFragmentInteractionListener{

    public static Resources resources;

    private NearbyFragment mNearbyFragment = null;

    private StationMapFragment mStationMapFragment = null;

    public static final String SETUP_UI_PATH = "setup_ui";

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
        //TODO: now we have multiple activities, this should not be done here
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

        mStationMapFragment = (StationMapFragment)getSupportFragmentManager().findFragmentById(
                R.id.station_map_fragment);


        //if (mNearbyFragment != null && savedInstanceState == null) {
        //    Bundle args = intentToFragmentArguments(getIntent());
        //    mNearbyFragment.reloadFromArguments(args);
        //}
    }

    @Override
    public void onStationMapFragmentInteraction(Uri uri) {
        //Will be warned of station details click, will make info fragment to replace list fragment




        /*@Override
    public void onInfoWindowClick(Marker marker) {
        if(!isStationInfoVisible) {
            for (StationItem station : mStationsNetwork.stations) {
                if (station.getPosition().equals(marker.getPosition())) {
                    replaceListViewByInfoView(station, false);
                    return;
                }
            }
        }
    }*/


        /*@Override
    public boolean onMarkerClick(Marker marker) {
        int i = mStationListViewAdapter.getPositionInList(marker);
        mStationListViewAdapter.setItemSelected(i);
        Log.d("onMarkerClick", "Scroll view to " + i);
        if (i != -1) {
            mStationListView.smoothScrollToPositionFromTop(i, 0, 300);
        }

    }*/

        /*@Override
    public void onMyLocationChange(Location location) {
        if(location != null) {
            Log.d("onMyLocationChange", "new location " + location.toString());
            mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mStationListViewAdapter != null)
                mStationListViewAdapter.setCurrentUserLatLng(mCurrentUserLatLng);

            if (mCurrentInfoStation != null){
                mStationInfoDistanceView.setText(String.valueOf(mCurrentInfoStation.getDistanceStringFromLatLng(mCurrentUserLatLng)));
            }
        }
    }*/



    }
}

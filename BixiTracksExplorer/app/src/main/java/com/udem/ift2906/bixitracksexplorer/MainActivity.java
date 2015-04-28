package com.udem.ift2906.bixitracksexplorer;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.couchbase.lite.CouchbaseLiteException;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetInfoFragment;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetInfoItem;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetOverviewFragment;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetTrackDetailsFragment;
import com.udem.ift2906.bixitracksexplorer.DBHelper.DBHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        BudgetOverviewFragment.OnFragmentInteractionListener,
        BudgetInfoFragment.OnFragmentInteractionListener,
        NearbyFragment.OnFragmentInteractionListener,
        UserSettingsFragment.OnFragmentInteractionListener,
        BudgetTrackDetailsFragment.OnBudgetTrackDetailsFragmentInteractionListener,
        FavoritesFragment.OnFragmentInteractionListener{

    //Test test
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private static final String TAG_NEARBY_FRAGMENT = "nearby_fragment";
    private static final String TAG_FAVORITES_FRAGMENT = "favorites_fragment";
    private static final String TAG_BUDGET_FRAGMENT = "budget_fragment";
    private static final String TAG_SETTINGS_FRAGMENT = "settings_fragment";
    private Map<Integer, Fragment> mFragmentPerSectionPos = new HashMap<>();

    //Not a section fragment, just keeping track for back navigation handling
    private static final String TAG_BUDGETINFO_FRAGMENT = "budgetinfo_fragment";

    private DrawerLayout mDrawerLayout;
    public static Resources resources;

    public NearbyFragment mNearbyFragment;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private int mPositionLastItemSelected = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resources = getResources();

        //Read app params and apply them
        if(getResources().getBoolean(R.bool.allow_portrait)){
            if (!getResources().getBoolean(R.bool.allow_landscape)){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        else{
            if (getResources().getBoolean(R.bool.allow_landscape)){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }


        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_main));

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        setActivityTitle(getTitle());
        setActivitySubtitle("");

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                mDrawerLayout);

        //Initialize couchbase database
        try {
            DBHelper.init(this, this);
            BixiTracksExplorerAPIHelper.init();
        } catch (IOException | CouchbaseLiteException e) {
            e.printStackTrace();
        }

        FrameLayout endFragment = (FrameLayout)findViewById(R.id.end_fragment_container);
        if (endFragment != null){
            endFragment.setVisibility(View.GONE);   //Second fragment only when getting to budgetinfo
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("lastSelected", mPositionLastItemSelected);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mPositionLastItemSelected = savedInstanceState.getInt("lastSelected");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DBHelper.closeDatabase();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //TODO Do it better: don't replace fragment if its the same as current
        if (position == mPositionLastItemSelected)
            return;
        mPositionLastItemSelected = position;
        setupFragmentsForPos(mPositionLastItemSelected);
    }

    private void setupFragmentsForPos(int position) {
        /////////////////////////////////////////////////////////////////////////////
        //TODO : Sort out this spaghetti monster in formation
        if (position == 0){
            FragmentManager fragmentManager = getSupportFragmentManager();

            Fragment frag =  fragmentManager.findFragmentByTag(TAG_NEARBY_FRAGMENT);
            // If the Fragment is non-null, then it is currently being
            // retained across a configuration change.
            if (frag == null) {
            //if (!mFragmentPerSectionPos.containsKey(position)){
                frag = NearbyFragment.newInstance(position + 1);
                mNearbyFragment = (NearbyFragment) frag;
                //Can't be optimised in if : need to be called right before visibilitySwitch
                mFragmentPerSectionPos.put(position, frag);
                switchFragmentVisibility(fragmentManager.beginTransaction().add(R.id.start_fragment_container, frag, TAG_NEARBY_FRAGMENT), position).commit();
            }else{
                mFragmentPerSectionPos.put(position, frag);
                switchFragmentVisibility(fragmentManager.beginTransaction(), position).commit();
            }
        }
        else if (position == 2){
            FragmentManager fragmentManager = getSupportFragmentManager();

            Fragment frag =  fragmentManager.findFragmentByTag(TAG_BUDGET_FRAGMENT);
            // If the Fragment is non-null, then it is currently being
            // retained across a configuration change.
            if (frag == null) {
            //if (!mFragmentPerSectionPos.containsKey(position)){
                frag = BudgetOverviewFragment.newInstance(position + 1);

                //Can't be optimised in if : need to be called right before visibilitySwitch
                mFragmentPerSectionPos.put(position, frag);
                //fragmentManager.beginTransaction().add(R.id.start_fragment_container, newFrag, TAG_BUDGET_FRAGMENT).commit();
                switchFragmentVisibility(fragmentManager.beginTransaction().add(R.id.start_fragment_container, frag, TAG_BUDGET_FRAGMENT), position).commit();
            }else{
                mFragmentPerSectionPos.put(position, frag);
                switchFragmentVisibility(fragmentManager.beginTransaction(), position).commit();
            }
        }
        else if (position == 3){
            FragmentManager fragmentManager = getSupportFragmentManager();

            Fragment frag =  fragmentManager.findFragmentByTag(TAG_SETTINGS_FRAGMENT);
            // If the Fragment is non-null, then it is currently being
            // retained across a configuration change.
            if (frag == null) {
                //if (!mFragmentPerSectionPos.containsKey(position)){
                frag = UserSettingsFragment.newInstance(position + 1);

                //Can't be optimised in if : need to be called right before visibilitySwitch
                mFragmentPerSectionPos.put(position, frag);
                //fragmentManager.beginTransaction().add(R.id.start_fragment_container, newFrag, TAG_BUDGET_FRAGMENT).commit();
                switchFragmentVisibility(fragmentManager.beginTransaction().add(R.id.start_fragment_container, frag, TAG_SETTINGS_FRAGMENT), position).commit();
            }else{
                mFragmentPerSectionPos.put(position, frag);
                switchFragmentVisibility(fragmentManager.beginTransaction(), position).commit();
            }

        }
        else if (position == 1){
            FragmentManager fragmentManager = getSupportFragmentManager();

            Fragment frag =  fragmentManager.findFragmentByTag(TAG_FAVORITES_FRAGMENT);
            // If the Fragment is non-null, then it is currently being
            // retained across a configuration change.
            if (frag == null) {
            //if (!mFragmentPerSectionPos.containsKey(position)){
                frag = FavoritesFragment.newInstance(position + 1);

                //Can't be optimised in if : need to be called right before visibilitySwitch
                mFragmentPerSectionPos.put(position, frag);
                switchFragmentVisibility(fragmentManager.beginTransaction().add(R.id.start_fragment_container, frag, TAG_FAVORITES_FRAGMENT), position).commit();
            }else{
                mFragmentPerSectionPos.put(position, frag);
                switchFragmentVisibility(fragmentManager.beginTransaction(), position).commit();
            }
        }
    }

    private FragmentTransaction switchFragmentVisibility(FragmentTransaction _inTrans, int _posToShow){

        for (int pos : mFragmentPerSectionPos.keySet()){
            if (pos == _posToShow){
                _inTrans.show(mFragmentPerSectionPos.get(pos));
            }
            else{
                _inTrans.hide(mFragmentPerSectionPos.get(pos));
            }
        }

        return _inTrans;
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                setActivityTitle(getString(R.string.title_section_nearby));
                break;
            case 2:
                setActivityTitle(getString(R.string.title_section_favorites));
                break;
            case 3:
                setActivityTitle(getString(R.string.title_section_budget));
                break;
            case 4:
                setActivityTitle(getString(R.string.title_section_settings));
                break;
        }
        setActivitySubtitle("");

    }

    public void onSectionHiddenChanged(int number) {
        switch (number) {
            case 1:
                setActivityTitle(getString(R.string.title_section_nearby));
                break;
            case 2:
                setActivityTitle(getString(R.string.title_section_favorites));
                break;
            case 3:
                setActivityTitle(getString(R.string.title_section_budget));
                break;
            case 4:
                setActivityTitle(getString(R.string.title_section_settings));
                break;
        }
        setActivitySubtitle("");

    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getActivityTitle());
        actionBar.setSubtitle(getActivitySubtitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // turn on the Navigation Drawer image;
        // this is called in the LowerLevelFragments
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            mNavigationDrawerFragment.getToggle().setDrawerIndicatorEnabled(true);
            switchFragmentVisibility(getSupportFragmentManager().beginTransaction(), mPositionLastItemSelected).commit();
        }

        Fragment frag = fm.findFragmentByTag(TAG_BUDGETINFO_FRAGMENT);

        if (frag !=null)
            frag.setMenuVisibility(true);
    }

    @Override
    public void onBudgetOverviewFragmentInteraction(Uri uri, ArrayList<BudgetInfoItem> _budgetInfoItemList) {

        if (uri.getPath().equalsIgnoreCase("/budget_overview_onresume"))
        {
            setActivityTitle(getString(R.string.title_section_budget));
            setActivitySubtitle("");
            restoreActionBar();

            FrameLayout endFragment = (FrameLayout)findViewById(R.id.end_fragment_container);
            if (endFragment != null){
                endFragment.setVisibility(View.GONE);   //Second fragment only when getting to budgetinfo
            }

            //Unlocking swipe gesture
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

            switchFragmentVisibility(getSupportFragmentManager().beginTransaction(), 2).commit();
        }
        else if(uri.getPath().equalsIgnoreCase("/" + BudgetOverviewFragment.BUDGETOVERVIEW_INFO_CLICK_PATH))
        {
            mNavigationDrawerFragment.getToggle().setDrawerIndicatorEnabled(false);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            setActivityTitle(uri.getQueryParameter(BudgetOverviewFragment.BUDGETOVERVIEW_INFO_CLICK_TYPE_PARAM));
            setActivitySubtitle(uri.getQueryParameter(BudgetOverviewFragment.BUDGETOVERVIEW_INFO_CLICK_TIMEPERIOD_PARAM));

            // Create fragment and give it required info to set itselfs up
            BudgetInfoFragment newFragment = BudgetInfoFragment.newInstance(getActivityTitle().toString(), getActivitySubtitle().toString(), _budgetInfoItemList);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.add(R.id.start_fragment_container, newFragment, TAG_BUDGETINFO_FRAGMENT);

            FrameLayout endContainer = (FrameLayout)findViewById(R.id.end_fragment_container);

            //Create second fragment right away
            if(endContainer != null){
                endContainer.setVisibility(View.VISIBLE);

                BudgetTrackDetailsFragment newEndFragment = BudgetTrackDetailsFragment.newInstance(-1, null, -1, true);

                transaction.replace(R.id.end_fragment_container, newEndFragment);
            }

            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();

            switchFragmentVisibility(getSupportFragmentManager().beginTransaction(), -1).commit();
        }


    }

    @Override
    public void onBudgetInfoFragmentInteraction(Uri _uri, ArrayList<BudgetInfoItem> _budgetInfoItemList) {
        if (_uri.getPath().equalsIgnoreCase("/" + BudgetInfoFragment.BUDGETINFOITEM_SORT_CHANGED_PATH)){

            setActivitySubtitle(_uri.getQueryParameter(BudgetInfoFragment.SORT_CHANGED_SUBTITLE_PARAM));
        }
        else if (_uri.getPath().equalsIgnoreCase("/" + BudgetInfoFragment.BUDGETINFOITEM_CLICK_PATH)){

            //check if we're too screens configuration
            FrameLayout endFragmentContainer = (FrameLayout)findViewById(R.id.end_fragment_container);
            if (endFragmentContainer == null){
                //One fragment at a time on screen

                BudgetTrackDetailsFragment newFragment = BudgetTrackDetailsFragment.newInstance(Integer.parseInt(_uri.getQueryParameter(BudgetInfoFragment.CLICK_ITEMPOS_PARAM)),
                        _budgetInfoItemList,
                        Integer.parseInt(_uri.getQueryParameter(BudgetInfoFragment.CLICK_SORT_CRITERIA_PARAM)),
                        false);
                setActivitySubtitle(getString(R.string.budgettrackdetails_subtitle));

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                transaction.setCustomAnimations(R.animator.slide_in_top, R.animator.fade_out, R.animator.fade_in, R.animator.slide_out_bottom);


                //TODO : Figure out how to get the opacity animation lost since I replaced the .replace by .add
                transaction.add(R.id.start_fragment_container, newFragment);

                transaction.addToBackStack(null);

                // Commit the transaction
                transaction.commit();
            }
            else{
                //We're in two fragments configuration, meaning they are both on screen and will stay there
                //retrieve it
                BudgetTrackDetailsFragment detailsFragment = (BudgetTrackDetailsFragment) getSupportFragmentManager().findFragmentById(R.id.end_fragment_container);
                //pass it new data
                detailsFragment.updateWithNewTrack(Integer.parseInt(_uri.getQueryParameter(BudgetInfoFragment.CLICK_ITEMPOS_PARAM)),
                        _budgetInfoItemList,
                        Integer.parseInt(_uri.getQueryParameter(BudgetInfoFragment.CLICK_SORT_CRITERIA_PARAM)));
            }
        }

        restoreActionBar();

    }

    @Override
    public void onNearbyFragmentInteraction(String title, boolean isDrawerIndicatorEnabled) {
        setActivityTitle(title);
        mNavigationDrawerFragment.getToggle().setDrawerIndicatorEnabled(isDrawerIndicatorEnabled);
        restoreActionBar();
    }

    @Override
    public void onBudgetTrackDetailsFragmentInteraction(Uri uri) {

    }

    @Override
    public void onFavoritesFragmentInteraction(StationItem stationToShow) {
        //mPositionLastItemSelected = -1;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.addToBackStack("favorite section");
        switchFragmentVisibility(ft,0).commit();
        mNearbyFragment.showStationInfoFromOutside(stationToShow);
    }

    @Override
    public void onSettingsFragmentInteraction() {

    }

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    public CharSequence getActivityTitle() {
        return mTitle;
    }

    public void setActivityTitle(CharSequence mTitle) {
        this.mTitle = mTitle;
    }

    public CharSequence getActivitySubtitle() {
        return mSubtitle;
    }

    public void setActivitySubtitle(CharSequence mSubtitle) {
        this.mSubtitle = mSubtitle;
    }
}

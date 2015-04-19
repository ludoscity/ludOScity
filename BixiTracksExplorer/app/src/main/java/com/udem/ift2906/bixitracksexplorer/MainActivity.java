package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import com.couchbase.lite.CouchbaseLiteException;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetInfoFragment;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetInfoItem;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetOverviewFragment;
import com.udem.ift2906.bixitracksexplorer.Budget.BudgetTrackDetailsFragment;
import com.udem.ift2906.bixitracksexplorer.DBHelper.DBHelper;

import java.io.IOException;
import java.util.ArrayList;


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

    private DrawerLayout mDrawerLayout;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private CharSequence mSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mSubtitle = "";

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
    public void onNavigationDrawerItemSelected(int position) {

        //En attendant d'avoir un menu bien rempli, juste pour tester la class NearbyFragment
        if (position == 0){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.start_fragment_container, NearbyFragment.newInstance(position + 1))
                    .commit();
        }
        else if (position == 2){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.start_fragment_container, BudgetOverviewFragment.newInstance(position + 1))
                    .commit();
        }
        else if (position == 3){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.start_fragment_container, UserSettingsFragment.newInstance(position + 1))
                    .commit();
        }
        else if (position == 1){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.start_fragment_container, FavoritesFragment.newInstance(position + 1))
                    .commit();
        }
        else
        {
            // update the main content by replacing fragments
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.start_fragment_container, PlaceholderFragment.newInstance(position + 1))
                    .commit();
        }


    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section_nearby);
                break;
            case 2:
                mTitle = getString(R.string.title_section_favorites);
                break;
            case 3:
                mTitle = getString(R.string.title_section_budget);
                break;
            case 4:
                mTitle = getString(R.string.title_section_settings);
                break;
        }
        mSubtitle = "";

    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
        actionBar.setSubtitle(mSubtitle);
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
        if (id == R.id.action_settings) {
            return true;
        }

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
        }
    }

    @Override
    public void onBudgetOverviewFragmentInteraction(Uri uri, ArrayList<BudgetInfoItem> _budgetInfoItemList) {

        if (uri.getPath().equalsIgnoreCase("/budget_overview_onresume"))
        {
            mTitle = getString(R.string.title_section_budget);
            mSubtitle = "";
            restoreActionBar();

            FrameLayout endFragment = (FrameLayout)findViewById(R.id.end_fragment_container);
            if (endFragment != null){
                endFragment.setVisibility(View.GONE);   //Second fragment only when getting to budgetinfo
            }

            //Unlocking swipe gesture
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        else if(uri.getPath().equalsIgnoreCase("/" + BudgetOverviewFragment.BUDGETOVERVIEW_INFO_CLICK_PATH))
        {
            mNavigationDrawerFragment.getToggle().setDrawerIndicatorEnabled(false);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            mTitle = uri.getQueryParameter(BudgetOverviewFragment.BUDGETOVERVIEW_INFO_CLICK_TYPE_PARAM);
            mSubtitle = uri.getQueryParameter(BudgetOverviewFragment.BUDGETOVERVIEW_INFO_CLICK_TIMEPERIOD_PARAM);

            // Create fragment and give it required info to set itselfs up
            BudgetInfoFragment newFragment = BudgetInfoFragment.newInstance(mTitle.toString(), mSubtitle.toString(), _budgetInfoItemList);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.start_fragment_container, newFragment);

            FrameLayout endContainer = (FrameLayout)findViewById(R.id.end_fragment_container);

            //Create second fragment right away
            if(endContainer != null){
                endContainer.setVisibility(View.VISIBLE);

                BudgetTrackDetailsFragment newEndFragment = BudgetTrackDetailsFragment.newInstance("null", null);

                transaction.replace(R.id.end_fragment_container, newEndFragment);
            }






            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }


    }

    @Override
    public void onBudgetInfoFragmentInteraction(Uri _uri, Bitmap _infoListRowBitmapRender) {
        if (_uri.getPath().equalsIgnoreCase("/" + BudgetInfoFragment.BUDGETINFOITEM_SORT_CHANGED_PATH)){

            mSubtitle = _uri.getQueryParameter(BudgetInfoFragment.SORT_CHANGED_SUBTITLE_PARAM);
        }
        else if (_uri.getPath().equalsIgnoreCase("/" + BudgetInfoFragment.BUDGETINFOITEM_CLICK_PATH)){

            //check if we're too screens configuration
            FrameLayout endFragmentContainer = (FrameLayout)findViewById(R.id.end_fragment_container);
            if (endFragmentContainer == null){
                //One fragment at a time on screen

                BudgetTrackDetailsFragment newFragment = BudgetTrackDetailsFragment.newInstance(_uri.getQueryParameter(BudgetInfoFragment.CLICK_TRACKID_PARAM), _infoListRowBitmapRender );
                mSubtitle = getString(R.string.budgettrackdetails_subtitle);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                transaction.setCustomAnimations(R.animator.slide_in_top, R.animator.fade_out, R.animator.fade_in, R.animator.slide_out_bottom);

                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack so the user can navigate back
                transaction.replace(R.id.start_fragment_container, newFragment);

                transaction.addToBackStack(null);

                // Commit the transaction
                transaction.commit();
            }
            else{
                //We're in two fragments configuration, meaning they are both on screen and will stay there
                //retrieve it
                BudgetTrackDetailsFragment detailsFragment = (BudgetTrackDetailsFragment) getSupportFragmentManager().findFragmentById(R.id.end_fragment_container);
                //pass it new data
                detailsFragment.updateWithNewTrack(_uri.getQueryParameter(BudgetInfoFragment.CLICK_TRACKID_PARAM), _infoListRowBitmapRender);
            }
        }

        restoreActionBar();

    }

    @Override
    public void onNearbyFragmentInteraction() {

    }

    @Override
    public void onBudgetTrackDetailsFragmentInteraction(Uri uri) {

    }

    @Override
    public void onFavoritesFragmentInteraction() {

    }

    @Override
    public void onSettingsFragmentInteraction() {

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        //final static String ARG_POSITION = "position";
        //int mCurrentPosition = -1;

        ExpandableListView mExpListView;
        //ExpandableListAdapter mListAdapter;

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }



        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }

        @Override
        public void onStart() {
            super.onStart();

//            // During startup, check if there are arguments passed to the fragment.
//            // onStart is a good place to do this because the layout has already been
//            // applied to the fragment at this point so we can safely call the method
//            // below that sets the article text.
//            Bundle args = getArguments();
//            if (args != null) {
//                // Set article based on argument passed in
//                updateArticleView(args.getInt(ARG_POSITION));
//            } else if (mCurrentPosition != -1) {
//                // Set article based on saved instance state defined during onCreateView
//                updateArticleView(mCurrentPosition);
//            }

            // get the listview
            mExpListView = (ExpandableListView) getActivity().findViewById(R.id.lvExp);

            //I should have a progress bar in my group / item layouts and activate them
            //OR have a completely separated loading fragment
            // preparing list data
            prepareListData();


        }

        //public void updateArticleView(int position) {
            //TextView article = (TextView) getActivity().findViewById(R.id.article);
            //article.setText(Ipsum.Articles[position]);
            //mCurrentPosition = position;
        //}

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            // Save the current article selection in case we need to recreate the fragment
            //outState.putInt(ARG_POSITION, mCurrentPosition);
        }

        private void prepareListData()
        {

            //start ASynchTask that retrieves data over the web
            new RetrieveTrackListTask(getActivity()).execute(mExpListView);
        }
    }


}

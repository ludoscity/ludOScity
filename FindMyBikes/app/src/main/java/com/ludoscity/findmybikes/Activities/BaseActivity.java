package com.ludoscity.findmybikes.Activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ludoscity.findmybikes.R;

import java.util.ArrayList;

/**
 * Created by F8Full on 2015-07-21.
 * This class manages navdrawer interactions. Taken from Google I/O 2014 app
 * https://github.com/google/iosched
 *
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    protected ActionBarDrawerToggle mDrawerToggle;

    // Navigation drawer:
    private DrawerLayout mDrawerLayout;

    private ObjectAnimator mStatusBarColorAnimator;
    private ViewGroup mDrawerItemsListContainer;
    private Handler mHandler;

    protected CharSequence mTitle;
    protected CharSequence mSubtitle;

    // symbols for navdrawer items (indices must correspond to array below). This is
    // not a list of items that are necessarily *present* in the Nav Drawer; rather,
    // it's a list of all possible items.
    protected static final int NAVDRAWER_ITEM_NEARBY = 0;
    protected static final int NAVDRAWER_ITEM_FAVORITES = 1;
    //protected static final int NAVDRAWER_ITEM_BUDGET = 2;
    protected static final int NAVDRAWER_ITEM_SETTINGS = 2;
    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    protected static final int NAVDRAWER_ITEM_SEPARATOR = -2;
    protected static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -3;

    // titles for navdrawer items (indices must correspond to the above)
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.title_section_nearby,
            R.string.title_section_favorites,
            //R.string.title_section_budget,
            R.string.title_section_settings,
    };

    // icons for navdrawer items (indices must correspond to above array)
    private static final int[] NAVDRAWER_ICON_RES_ID = new int[] {
            R.drawable.ic_drawer_nearby,
            R.drawable.ic_drawer_favorites,
            R.drawable.ic_drawer_settings,
    };

    // delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    // list of navdrawer items that were actually added to the navdrawer, in order
    private ArrayList<Integer> mNavDrawerItems = new ArrayList<>();

    // views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;

    // Primary toolbar and drawer toggle
    private Toolbar mActionBarToolbar;

    // A Runnable that we should execute when the navigation drawer finishes its closing animation
    private Runnable mDeferredOnDrawerClosedRunnable;

    //private int mThemedStatusBarColor;
    //private int mNormalStatusBarColor;
    //private int mProgressBarTopWhenActionBarShown;
    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    //private ImageLoader mImageLoader;

    private CharSequence mRestorableTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        //mThemedStatusBarColor = getResources().getColor(R.color.theme_primary_dark);
        //mNormalStatusBarColor = mThemedStatusBarColor;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses
     * of BaseActivity override this to indicate what nav drawer item corresponds to them
     * Return NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    protected void disableDrawer(){
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    protected void enableDrawer(){
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
    }

    private void setupNavDrawer() {
        // What nav drawer item should be selected?
        int selfItem = getSelfNavDrawerItem();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }
        //mDrawerLayout.setStatusBarBackgroundColor(
        //        getResources().getColor(R.color.theme_primary_dark));

        /*ScrimInsetsScrollView navDrawer = (ScrimInsetsScrollView)
                mDrawerLayout.findViewById(R.id.navdrawer);

        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            // do not show a nav drawer
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }*/

        if (mActionBarToolbar != null) {
            //mActionBarToolbar.setNavigationIcon(R.drawable.ic_);
            mActionBarToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (mDrawerToggle.isDrawerIndicatorEnabled()) {

                        if (isNavDrawerOpen())
                            mDrawerLayout.closeDrawer(GravityCompat.START);
                        else
                            mDrawerLayout.openDrawer(GravityCompat.START);
                    }
                    else{
                        onBackPressed();
                    }
                }
            });



            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
                ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }

                setActivityTitle(mRestorableTitle);
                restoreActionBar();

                onNavDrawerStateChanged(false, false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mRestorableTitle = mTitle;
                setActivityTitle(getTitle());
                restoreActionBar();
                onNavDrawerStateChanged(true, false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                onNavDrawerStateChanged(isNavDrawerOpen(), newState != DrawerLayout.STATE_IDLE);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                onNavDrawerSlide(slideOffset);
            }

        };

        /*mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mDrawerToggle.isDrawerIndicatorEnabled()) {
                    onBackPressed();
                }
            }
        });*/

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // populate the nav drawer with the correct items
        populateNavDrawer();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        //if (mActionBarAutoHideEnabled && isOpen) {
        //    autoShowOrHideActionBar(true);
        //}

        if (isOpen)
        {
            supportInvalidateOptionsMenu();
        }
    }

    protected void onNavDrawerSlide(float offset) {}

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void populateNavDrawer() {
        mNavDrawerItems.clear();

        mNavDrawerItems.add(NAVDRAWER_ITEM_NEARBY);
        mNavDrawerItems.add(NAVDRAWER_ITEM_FAVORITES);
        //mNavDrawerItems.add(NAVDRAWER_ITEM_BUDGET);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SETTINGS);

        createNavDrawerItems();
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isNavDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    protected void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
        actionBar.setSubtitle(mSubtitle);
    }

    private void createNavDrawerItems() {
        mDrawerItemsListContainer = (ViewGroup) findViewById(R.id.navdrawer_items_list);
        if (mDrawerItemsListContainer == null) {
            return;
        }

        mNavDrawerItemViews = new View[mNavDrawerItems.size()];
        mDrawerItemsListContainer.removeAllViews();
        int i = 0;
        for (int itemId : mNavDrawerItems) {
            mNavDrawerItemViews[i] = makeNavDrawerItem(itemId, mDrawerItemsListContainer);
            mDrawerItemsListContainer.addView(mNavDrawerItemViews[i]);
            ++i;
        }
    }

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could
     * also be accomplished (perhaps more cleanly) with state-based layouts.
     */
    private void setSelectedNavDrawerItem(int itemId) {
        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                if (i < mNavDrawerItems.size()) {
                    int thisItemId = mNavDrawerItems.get(i);
                    formatNavDrawerItem(mNavDrawerItemViews[i], thisItemId, itemId == thisItemId);
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavDrawer();

        View mainContent = findViewById(R.id.main_content); //each activity layout must define it
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        } //else {
          //  LOGW(TAG, "No view with ID main_content to fade in.");
        //}
    }

    //////////////////////////////////////////////////////////////////////////////
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
    ////////////////////////////////////////////////////////////////////////////////////

    private void goToNavDrawerItem(int item) {
        Intent intent;
        switch (item) {
            case NAVDRAWER_ITEM_NEARBY:
                intent = new Intent(this, NearbyActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                break;
            case NAVDRAWER_ITEM_FAVORITES:
                intent = new Intent(this, FavoritesActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                break;
            /*case NAVDRAWER_ITEM_EXPLORE:
                intent = new Intent(this, BrowseSessionsActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_MAP:
                intent = new Intent(this, UIUtils.getMapActivityClass(this));
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_SOCIAL:
                intent = new Intent(this, SocialActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_EXPERTS_DIRECTORY:
                intent = new Intent(this, ExpertsDirectoryActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_PEOPLE_IVE_MET:
                intent = new Intent(this, PeopleIveMetActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_SIGN_IN:
                signInOrCreateAnAccount();
                break;*/
            case NAVDRAWER_ITEM_SETTINGS:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                break;
            /*case NAVDRAWER_ITEM_VIDEO_LIBRARY:
                intent = new Intent(this, VideoLibraryActivity.class);
                startActivity(intent);
                finish();
                break;*/
        }
    }

    private void onNavDrawerItemClicked(final int itemId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (isSpecialItem(itemId)) {
            goToNavDrawerItem(itemId);
        } else {
            // launch the target Activity after a short delay, to allow the close animation to play
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToNavDrawerItem(itemId);
                }
            }, NAVDRAWER_LAUNCH_DELAY);

            // change the active item on the list so the user can see the item changed
            setSelectedNavDrawerItem(itemId);
            // fade out the main content
            View mainContent = findViewById(R.id.main_content);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
            }
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Verifies the proper version of Google Play Services exists on the device.
        //PlayServicesUtils.checkGooglePlaySevices(this);

        // Watch for sync state changes
        //mSyncStatusObserver.onStatusChanged(0);
        //final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
        //        ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        //mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }*/
    }

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    public static Intent fragmentArgumentsToIntent(Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }

    @Override
    public void onStart() {
        //LOGD(TAG, "onStart");
        super.onStart();

        // Perform one-time bootstrap setup, if needed
        /*if (!PrefUtils.isDataBootstrapDone(this) && mDataBootstrapThread == null) {
            LOGD(TAG, "One-time data bootstrap not done yet. Doing now.");
            performDataBootstrap();
        }

        startLoginProcess();*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (mLoginAndAuthHelper == null || !mLoginAndAuthHelper.onActivityResult(requestCode,
        //        resultCode, data)) {
        super.onActivityResult(requestCode, resultCode, data);
        //}
    }

    @Override
    public void onStop() {
        //LOGD(TAG, "onStop");
        super.onStop();
        //if (mLoginAndAuthHelper != null) {
        //    mLoginAndAuthHelper.stop();
        //}
    }

    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_main);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    private View makeNavDrawerItem(final int itemId, ViewGroup container) {
        boolean selected = getSelfNavDrawerItem() == itemId;
        int layoutToInflate;
        if (itemId == NAVDRAWER_ITEM_SEPARATOR) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else if (itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else {
            layoutToInflate = R.layout.navdrawer_item;
        }
        View view = getLayoutInflater().inflate(layoutToInflate, container, false);

        if (isSeparator(itemId)) {
            // we are done
            //UIUtils.setAccessibilityIgnore(view);
            return view;
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        int iconId = itemId >= 0 && itemId < NAVDRAWER_ICON_RES_ID.length ?
                NAVDRAWER_ICON_RES_ID[itemId] : 0;
        int titleId = itemId >= 0 && itemId < NAVDRAWER_TITLE_RES_ID.length ?
                NAVDRAWER_TITLE_RES_ID[itemId] : 0;

        // set icon and text
        iconView.setVisibility(iconId > 0 ? View.VISIBLE : View.GONE);
        if (iconId > 0) {
            iconView.setImageResource(iconId);
        }
        titleView.setText(getString(titleId));

        formatNavDrawerItem(view, itemId, selected);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavDrawerItemClicked(itemId);
            }
        });

        return view;
    }

    private boolean isSpecialItem(int itemId) {
        return itemId == NAVDRAWER_ITEM_SETTINGS;
    }

    private boolean isSeparator(int itemId) {
        return itemId == NAVDRAWER_ITEM_SEPARATOR || itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL;
    }

    private void formatNavDrawerItem(View view, int itemId, boolean selected) {
        if (isSeparator(itemId)) {
            // not applicable
            return;
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);

        if (selected) {
            view.setBackgroundResource(R.drawable.selected_navdrawer_item_background);
        }

        // configure its appearance according to whether or not it's selected
        titleView.setTextColor(selected ?
                ContextCompat.getColor(this, R.color.navdrawer_text_color_selected) :
                ContextCompat.getColor(this, R.color.navdrawer_text_color));
        iconView.setColorFilter(selected ?
                ContextCompat.getColor(this, R.color.navdrawer_icon_tint_selected) :
                ContextCompat.getColor(this, R.color.navdrawer_icon_tint));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*if (mGCMRegisterTask != null) {
            LOGD(TAG, "Cancelling GCM registration task.");
            mGCMRegisterTask.cancel(true);
        }

        try {
            GCMRegistrar.onDestroy(this);
        } catch (Exception e) {
            LOGW(TAG, "C2DM unregistration error", e);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);*/
    }

    public void setActivityTitle(CharSequence mTitle) {
        this.mTitle = mTitle;
    }

    public void setActivitySubtitle(CharSequence mSubtitle) {
        this.mSubtitle = mSubtitle;
    }









}

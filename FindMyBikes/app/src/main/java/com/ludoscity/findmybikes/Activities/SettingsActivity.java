package com.ludoscity.findmybikes.Activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.Fragments.UserSettingsFragment;

/**
 * Created by F8Full on 2015-08-10.
 * Activity used to display Settings fragment
 */
public class SettingsActivity extends BaseActivity
        implements UserSettingsFragment.OnFragmentInteractionListener {

    @Override
    protected int getSelfNavDrawerItem() { return NAVDRAWER_ITEM_SETTINGS; }

    @Override
    public void onSettingsFragmentInteraction() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        setActivityTitle(getTitle());
        setActivitySubtitle("");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setActivityTitle(getString(R.string.title_section_settings));
    }
}

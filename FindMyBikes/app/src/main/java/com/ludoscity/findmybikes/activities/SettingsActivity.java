package com.ludoscity.findmybikes.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.fragments.SettingsFragment;

/**
 * Created by F8Full on 2015-08-10.
 * Activity used to display Settings fragment
 */
public class SettingsActivity extends AppCompatActivity
        implements SettingsFragment.OnFragmentInteractionListener {

    @Override
    public void onSettingsFragmentInteraction() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.main_content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.title_section_settings));
    }
}

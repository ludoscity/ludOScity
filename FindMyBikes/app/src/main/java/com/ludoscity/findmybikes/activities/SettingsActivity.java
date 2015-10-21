package com.ludoscity.findmybikes.activities;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

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

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

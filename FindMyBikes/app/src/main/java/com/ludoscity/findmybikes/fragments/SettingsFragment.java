package com.ludoscity.findmybikes.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.ludoscity.findmybikes.R;

/**
 * Created by Looney on 19-04-15.
 * Used to handle the Settings section
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.user_settings);
    }
}

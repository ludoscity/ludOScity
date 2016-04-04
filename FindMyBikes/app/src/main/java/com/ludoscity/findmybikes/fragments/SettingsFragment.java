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

        //TODO: figure this shit out
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            ((SwitchPreference)findPreference(getString(R.string.pref_walking_proximity_key))).setSwitchTextOff(
                    String.format(getResources().getString(R.string.pref_walking_proximity_duration),
                            getResources().getInteger(R.integer.average_walking_speed_kmh))
            );


            ((SwitchPreference)findPreference(getString(R.string.pref_biking_proximity_key))).setSwitchTextOff(
                    String.format(getResources().getString(R.string.pref_biking_proximity_duration),
                            getResources().getInteger(R.integer.average_biking_speed_kmh))
            );

        } else {

            ((CheckBoxPreference)findPreference(getString(R.string.pref_walking_proximity_key))).setSummaryOff(
                    String.format(getResources().getString(R.string.pref_walking_proximity_duration),
                            getResources().getInteger(R.integer.average_walking_speed_kmh))
            );

            ((CheckBoxPreference)findPreference(getString(R.string.pref_biking_proximity_key))).setSummaryOff(
                    String.format(getResources().getString(R.string.pref_biking_proximity_duration),
                            getResources().getInteger(R.integer.average_biking_speed_kmh))
            );

        }*/
    }
}

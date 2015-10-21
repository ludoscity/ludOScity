package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.ludoscity.findmybikes.R;

/**
 * Created by Looney on 19-04-15.
 * Used to handle the Settings section
 */
public class SettingsFragment extends PreferenceFragment {

    public interface OnFragmentInteractionListener {
        void onSettingsFragmentInteraction();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.user_settings);
    }

   @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            OnFragmentInteractionListener mListener = (OnFragmentInteractionListener) activity;
            super.onAttach(activity);
            //((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
}

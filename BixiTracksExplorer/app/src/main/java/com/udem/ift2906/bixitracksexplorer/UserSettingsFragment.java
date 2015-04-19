package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Created by Looney on 19-04-15.
 */
public class UserSettingsFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";

    public interface OnFragmentInteractionListener {
        public void onSettingsFragmentInteraction();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            OnFragmentInteractionListener mListener = (OnFragmentInteractionListener) activity;
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public static UserSettingsFragment newInstance(int sectionNumber) {
            UserSettingsFragment fragment = new UserSettingsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            fragment.setHasOptionsMenu(true);
            return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = layoutInflater.inflate(R.layout.fragment_settings, container, false);
        return inflatedView;
    }
}

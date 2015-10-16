package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.R;

/**
 * Created by Looney on 19-04-15.
 * Used to handle the Settings section
 */
public class UserSettingsFragment extends Fragment {

    private CheckBox autoUpdate_nearby;

    public interface OnFragmentInteractionListener {
        void onSettingsFragmentInteraction();
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

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = layoutInflater.inflate(R.layout.fragment_settings, container, false);

        autoUpdate_nearby = (CheckBox) inflatedView.findViewById(R.id.checkBox_setting_auto_update);
        autoUpdate_nearby.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DBHelper.setAutoUpdate(isChecked, getActivity());
            }
        });

        autoUpdate_nearby.setChecked(DBHelper.getAutoUpdate(getActivity()));

        return inflatedView;
    }
}

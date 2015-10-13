package com.ludoscity.findmybikes;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.ludoscity.findmybikes.Helpers.DBHelper;

/**
 * Created by Looney on 19-04-15.
 * Used to handle the Settings section
 */
public class UserSettingsFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    public static final String PREF_NEARBY_AUTO_UPDATE = "setting.auto_update.nearby";

    private CheckBox autoUpdate_nearby;
    private CheckBox mLockScreenOrientation;

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
        View inflatedView = layoutInflater.inflate(com.ludoscity.findmybikes.R.layout.fragment_settings, container, false);

        autoUpdate_nearby = (CheckBox) inflatedView.findViewById(com.ludoscity.findmybikes.R.id.checkBox_setting_auto_update);
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

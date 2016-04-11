package com.ludoscity.findmybikes;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.ludoscity.findmybikes.helpers.DBHelper;

/**
 * Created by F8Full on 2016-04-10.
 * Provides a Setting dialog to configure map marker colors availability values
 * http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes
 */
public class AvailabilitySettingsDialogPreference extends DialogPreference {

    NumberPicker mCriticalMaxPicker;
    NumberPicker mBadMaxPicker;
    TextView mBadMinText;
    TextView mGreatMinText;

    public AvailabilitySettingsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPersistent(false);
        setDialogLayoutResource(R.layout.map_settings_dialog_content);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mCriticalMaxPicker = (NumberPicker) view.findViewById(R.id.pref_availability_critical_max_picker);
        mBadMaxPicker = (NumberPicker) view.findViewById(R.id.pref_availability_bad_max_picker);
        mBadMinText = (TextView) view.findViewById(R.id.pref_availability_bad_min_text);
        mGreatMinText = (TextView) view.findViewById(R.id.pref_availability_great_min_text);

        int redUpperValue = DBHelper.getCriticalAvailabilityMax(getContext());
        int yellowUpperValue = DBHelper.getBadAvailabilityMax(getContext());


        mCriticalMaxPicker.setMinValue(0);
        mCriticalMaxPicker.setMaxValue(3);

        mBadMinText.setText(String.format(getContext().getString(R.string.pref_availability_bad_min_label), redUpperValue + 1));

        mBadMaxPicker.setMinValue(redUpperValue + 1);
        mBadMaxPicker.setMaxValue(redUpperValue + 4);

        mCriticalMaxPicker.setValue(redUpperValue);
        mBadMaxPicker.setValue(yellowUpperValue);

        mGreatMinText.setText(String.format(getContext().getString(R.string.pref_availability_great_min_label), yellowUpperValue + 1));

        mCriticalMaxPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) { //i1 is new value

                mBadMinText.setText(String.format(getContext().getString(R.string.pref_availability_bad_min_label), i1 + 1));

                mBadMaxPicker.setMinValue(i1 + 1);
                mBadMaxPicker.setMaxValue(i1 + 4);

                mGreatMinText.setText(String.format(getContext().getString(R.string.pref_availability_great_min_label), mBadMaxPicker.getValue() + 1));
            }
        });

        mBadMaxPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {

                mGreatMinText.setText(String.format(getContext().getString(R.string.pref_availability_great_min_label), i1 + 1));

            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {

        if (positiveResult) {
            DBHelper.saveCriticalAvailabilityMax(getContext(), mCriticalMaxPicker.getValue());
            DBHelper.saveBadAvailabilityMax(getContext(), mBadMaxPicker.getValue());
        }
        super.onDialogClosed(positiveResult);
    }
}

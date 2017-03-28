package com.ludoscity.findmybikes;

import android.annotation.SuppressLint;
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

    private NumberPicker mCriticalMaxPicker;
    private NumberPicker mBadMaxPicker;
    private TextView mBadMinText;
    private TextView mGreatMinText;
    private TextView mCriticalHint;

    public AvailabilitySettingsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPersistent(false);
        setDialogLayoutResource(R.layout.map_settings_dialog_content);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mCriticalMaxPicker = (NumberPicker) view.findViewById(R.id.pref_availability_critical_max_picker);
        mBadMaxPicker = (NumberPicker) view.findViewById(R.id.pref_availability_bad_max_picker);
        mBadMinText = (TextView) view.findViewById(R.id.pref_availability_bad_min_text);
        mGreatMinText = (TextView) view.findViewById(R.id.pref_availability_great_min_text);
        mCriticalHint = (TextView) view.findViewById(R.id.pref_availability_critical_hint);

        int redUpperValue = DBHelper.getCriticalAvailabilityMax(getContext());
        int yellowUpperValue = DBHelper.getBadAvailabilityMax(getContext());


        mCriticalMaxPicker.setMinValue(0);
        mCriticalMaxPicker.setMaxValue(3);

        mBadMinText.setText(String.format(getContext().getString(R.string.pref_availability_bad_min_label), redUpperValue + 1));
        ((TextView) view.findViewById(R.id.pref_availability_cri_min_text)).setText(String.format(getContext().getString(R.string.pref_availability_cri_min_label), 0));

        mBadMaxPicker.setMinValue(redUpperValue + 1);
        mBadMaxPicker.setMaxValue(redUpperValue + 4);

        mCriticalMaxPicker.setValue(redUpperValue);
        mBadMaxPicker.setValue(yellowUpperValue);

        setupCriticalHint();

        mGreatMinText.setText(String.format(getContext().getString(R.string.pref_availability_great_min_label), yellowUpperValue + 1));

        mCriticalMaxPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @SuppressLint("StringFormatInvalid")
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int _newValue) {
                setupCriticalHint();

                mBadMinText.setText(String.format(getContext().getString(R.string.pref_availability_bad_min_label), _newValue + 1));

                mBadMaxPicker.setMinValue(_newValue + 1);
                mBadMaxPicker.setMaxValue(_newValue + 4);

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

    private void setupCriticalHint(){

        switch (mCriticalMaxPicker.getValue()){
            case 0:
                mCriticalHint.setText(R.string.availability_hint_never);
                break;
            case 1:
                mCriticalHint.setText(R.string.availability_hint_sometime);
                break;
            case 2:
                mCriticalHint.setText(R.string.availability_hint_often);
                break;
            case 3:
                mCriticalHint.setText(R.string.availability_hint_veryoften);
                break;
        }
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

package com.android.deskclock.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.deskclock.NumberPickerCompat;
import com.android.deskclock.R;

/**
 * A dialog preference that shows a number picker for selecting crescendo length
 */
public final class CrescendoLengthDialog extends DialogPreference {

    private static final String DEFAULT_CRESCENDO_TIME = "0";
    private static final int CRESCENDO_TIME_STEP = 5;

    private NumberPickerCompat mNumberPickerView;
    private TextView mNumberPickerSecondsView;
    private int mCrescendoSeconds;

    public CrescendoLengthDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.crescendo_length_picker);
        setTitle(R.string.crescendo_duration_title);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(getContext().getString(R.string.crescendo_duration_title))
                .setCancelable(true);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        final String[] displayedValues = new String[13];
        displayedValues[0] = getContext().getString(R.string.no_crescendo_duration);
        for (int i = 1; i < displayedValues.length; i++) {
            displayedValues[i] = String.valueOf(i * CRESCENDO_TIME_STEP);
        }

        mNumberPickerSecondsView = (TextView) view.findViewById(R.id.title);
        mNumberPickerSecondsView.setText(getContext().getString(R.string.crescendo_picker_label));
        mNumberPickerView = (NumberPickerCompat) view.findViewById(R.id.seconds_picker);
        mNumberPickerView.setDisplayedValues(displayedValues);
        mNumberPickerView.setMinValue(0);
        mNumberPickerView.setMaxValue(displayedValues.length - 1);
        mNumberPickerView.setValue(mCrescendoSeconds / CRESCENDO_TIME_STEP);
        updateUnits();

        mNumberPickerView.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateUnits();
            }
        });
        mNumberPickerView.setOnAnnounceValueChangedListener(
                new NumberPickerCompat.OnAnnounceValueChangedListener() {
            @Override
            public void onAnnounceValueChanged(NumberPicker picker, int value,
                    String displayedValue) {
                final String announceString;
                if (value == 0) {
                    announceString = getContext().getString(R.string.no_crescendo_duration);
                } else {
                    announceString = getContext().getString(
                            R.string.crescendo_duration, displayedValue);
                }
                picker.announceForAccessibility(announceString);
            }
        });
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String val;
        if (restorePersistedValue) {
            val = getPersistedString(DEFAULT_CRESCENDO_TIME);
            if (val != null) {
                mCrescendoSeconds = Integer.parseInt(val);
            }
        } else {
            val = (String) defaultValue;
            if (val != null) {
                mCrescendoSeconds = Integer.parseInt(val);
            }
            persistString(val);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Restore the value to the NumberPicker.
        super.onRestoreInstanceState(state);

        // Update the unit display in response to the new value.
        updateUnits();
    }

    private void updateUnits() {
        if (mNumberPickerView != null) {
            final int value = mNumberPickerView.getValue();
            final int visibility = value == 0 ? View.INVISIBLE : View.VISIBLE;
            mNumberPickerSecondsView.setVisibility(visibility);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mNumberPickerView.clearFocus();
            mCrescendoSeconds = mNumberPickerView.getValue() * CRESCENDO_TIME_STEP;
            persistString(Integer.toString(mCrescendoSeconds));
            setSummary();
        }
    }

    public void setSummary() {
        if (mCrescendoSeconds == 0) {
            setSummary(getContext().getString(R.string.no_crescendo_duration));
        } else {
            setSummary(getContext().getString(R.string.crescendo_duration, mCrescendoSeconds));
        }
    }
}
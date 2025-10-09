// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.R;
import com.best.deskclock.utils.SdkUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

/**
 * Custom component to display a delay selection dialog using spinners.
 * This custom implementation allows the user to pick the hour and minute through spinners.
 *
 * <p>Note:</p>
 * <ul>
 *  <li>The hours do not automatically change when the minutes change from 59 to 0 or from 0 to 59,
 *  allowing for more precise control over the time selection;
 *  <li>Time can be set with the enter key on the keyboard.
 * </ul>
 */
public class AlarmDelayPickerDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of AlarmDelayPickerDialogFragment in the fragment manager.
     */
    private static final String TAG = "alarm_delay_dialog";
    private static final String ARG_EDIT_HOURS = "arg_edit_hours";
    private static final String ARG_EDIT_MINUTES = "arg_edit_minutes";
    public static final String REQUEST_KEY = "alarm_delay_result";
    public static final String BUNDLE_KEY_HOURS = "hours";
    public static final String BUNDLE_KEY_MINUTES = "minutes";

    private Context mContext;
    private Button mOkButton;
    private NumberPicker mHourPicker;
    private NumberPicker mMinutePicker;

    /**
     * Creates a new instance of {@link AlarmDelayPickerDialogFragment} for use
     * in the alarm view, where the volume value is configured for a specific alarm.
     *
     * @param hours    The alarm hours.
     * @param minutes  The alarm minutes.
     */
    public static AlarmDelayPickerDialogFragment newInstance(int hours, int minutes) {
        final Bundle args = new Bundle();
        args.putInt(ARG_EDIT_HOURS, hours);
        args.putInt(ARG_EDIT_MINUTES, minutes);

        final AlarmDelayPickerDialogFragment frag = new AlarmDelayPickerDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Replaces any existing AlarmDelayPickerDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, AlarmDelayPickerDialogFragment fragment) {
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Finish any outstanding fragment work.
        manager.executePendingTransactions();

        final FragmentTransaction tx = manager.beginTransaction();

        // Remove existing instance of AlarmDelayPickerDialogFragment if necessary.
        final Fragment existing = manager.findFragmentByTag(TAG);
        if (existing != null) {
            tx.remove(existing);
        }
        tx.addToBackStack(null);

        fragment.show(tx, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as the dialog exists, save its state.
        if (mHourPicker != null) {
            outState.putInt(ARG_EDIT_HOURS, mHourPicker.getValue());
        }

        if (mMinutePicker != null) {
            outState.putInt(ARG_EDIT_MINUTES, mMinutePicker.getValue());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = requireContext();
        final Bundle args = requireArguments();
        int hourValue = args.getInt(ARG_EDIT_HOURS, 0);
        int minuteValue = args.getInt(ARG_EDIT_MINUTES, 0);

        if (savedInstanceState != null) {
            hourValue = savedInstanceState.getInt(ARG_EDIT_HOURS, hourValue);
            minuteValue = savedInstanceState.getInt(ARG_EDIT_MINUTES, minuteValue);
        }

        View view = getLayoutInflater().inflate(R.layout.alarm_spinner_delay_picker, null);
        mHourPicker = view.findViewById(R.id.hour);
        mMinutePicker = view.findViewById(R.id.minute);

        // Hours and minutes setup
        mHourPicker.setHapticFeedbackEnabled(true);
        mHourPicker.setMinValue(0);
        mHourPicker.setMaxValue(24);
        mHourPicker.setNextFocusForwardId(R.id.minute);

        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(59);
        mMinutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
        mMinutePicker.setNextFocusForwardId(View.NO_ID);

        // Set default values at start
        mHourPicker.setValue(hourValue);
        mMinutePicker.setValue(minuteValue);

        setupEditTextInput(mHourPicker);
        setupEditTextInput(mMinutePicker);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext(), R.style.SpinnerDialogTheme)
                .setTitle(R.string.delay_picker_dialog_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mHourPicker.clearFocus();
                    mMinutePicker.clearFocus();
                    setAlarmDelay();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());

        AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(dialog -> {
            mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

            mHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                updateUiState();
            });
            mMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                updateUiState();
            });

            // Initial state
            updateUiState();
        });

        return alertDialog;
    }

    /**
     * Sets a new alarm based on the values selected in the hour and minute pickers.
     * <p>If the selected hour is 24, minutes are forced to 0.
     * The normalized data is then sent via a FragmentResult to the calling fragment
     * for it to take effect.</p>
     */
    private void setAlarmDelay() {
        int hours = mHourPicker.getValue();
        int minutes = mMinutePicker.getValue();

        if (hours == 24) {
            minutes = 0;
        }

        Bundle result = new Bundle();
        result.putInt(BUNDLE_KEY_HOURS, hours);
        result.putInt(BUNDLE_KEY_MINUTES, minutes);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    /**
     * Updates the dialog's user interface state based on the current values
     * selected in the NumberPickers:
     *
     * <ul>
     *   <li>Disables the minute picker and resets its value to zero if the selected hour equals 24.</li>
     *   <li>Enables or disables the OK button depending on whether the input is considered valid
     *       (at least one of the hour or minute values must be non-zero).</li>
     * </ul>
     */
    private void updateUiState() {
        boolean minutesEnabled = mHourPicker.getValue() != 24;

        updateMinutePickerEnabledState(minutesEnabled);

        if (mOkButton != null) {
            mOkButton.setEnabled(isAlarmDelayValid());
        }
    }

    /**
     * @return {@code true} if either the hour or minute picker value is greater than zero.
     * {@code false} otherwise.
     */
    private boolean isAlarmDelayValid() {
        return !(mHourPicker.getValue() == 0 && mMinutePicker.getValue() == 0);
    }

    /**
     * Enables or disables the minute picker and updates its visual appearance accordingly.
     *
     * <p>If disabling, the minute picker is visually dimmed (by changing text color or alpha)
     * to indicate it is inactive. When enabled, it restores the default enabled appearance.</p>
     */
    private void updateMinutePickerEnabledState(boolean enabled) {
        mMinutePicker.setEnabled(enabled);

        if (SdkUtils.isAtLeastAndroid10()) {
            mMinutePicker.setTextColor(enabled
                    ? mHourPicker.getTextColor()
                    : mContext.getColor(R.color.colorDisabled));
        } else {
            mMinutePicker.setAlpha(enabled ? 1f : 0.5f);
        }
    }

    /**
     * This method is used to customize the keyboard input handling in a NumberPicker of
     * type EditText for a time picker.
     * It configures the keyboard and validation behavior when the user changes the hour or minute.
     */
    private void setupEditTextInput(NumberPicker numberPicker) {
        int count = numberPicker.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText editText) {
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
                editText.setClickable(true);
                editText.setLongClickable(true);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                if (numberPicker == mHourPicker) {
                    editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                } else if (numberPicker == mMinutePicker) {
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                }
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        int hour = mHourPicker.getValue();
                        int minute = mMinutePicker.getValue();

                        if (numberPicker == mHourPicker) {
                            if (s.toString().isEmpty()) {
                                hour = 0;
                            } else {
                                hour = Integer.parseInt(s.toString());
                            }

                            editText.setImeOptions(hour == 24
                                    ? EditorInfo.IME_ACTION_DONE
                                    : EditorInfo.IME_ACTION_NEXT);

                            final InputMethodManager inputMethodManager =
                                    (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            inputMethodManager.restartInput(editText);
                        } else if (numberPicker == mMinutePicker) {
                            if (s.toString().isEmpty()) {
                                minute = 0;
                            } else {
                                minute = Integer.parseInt(s.toString());
                            }
                        }

                        updateMinutePickerEnabledState(hour != 24);

                        if (mOkButton != null) {
                            mOkButton.setEnabled(!(hour == 0 && minute == 0));
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
                editText.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        numberPicker.clearFocus();

                        if (isAlarmDelayValid()) {
                            setAlarmDelay();
                        }

                        dismiss();

                        return true;
                    }

                    return false;
                });
            }
        }
    }

}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmSpinnerDelayPickerBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

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
    public static final String TAG = "alarm_delay_dialog";
    private static final String ARG_EDIT_HOURS = "arg_edit_hours";
    private static final String ARG_EDIT_MINUTES = "arg_edit_minutes";
    public static final String REQUEST_KEY = "alarm_delay_result";
    public static final String BUNDLE_KEY_HOURS = "alarm_delay_dialog_hours";
    public static final String BUNDLE_KEY_MINUTES = "alarm_delay_dialog_minutes";

    private AlarmSpinnerDelayPickerBinding mBinding;
    private Button mOkButton;

    /**
     * Creates a new instance of {@link AlarmDelayPickerDialogFragment} for use
     * in the alarm view, where the volume value is configured for a specific alarm.
     *
     * @param hours   The alarm hours.
     * @param minutes The alarm minutes.
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
     * Displays {@link AlarmDelayPickerDialogFragment}.
     */
    public static void show(FragmentManager manager, AlarmDelayPickerDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as the dialog exists, save its state.
        outState.putInt(ARG_EDIT_HOURS, mBinding.hourPicker.getValue());
        outState.putInt(ARG_EDIT_MINUTES, mBinding.minutePicker.getValue());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();
        int hourValue = args.getInt(ARG_EDIT_HOURS, 0);
        int minuteValue = args.getInt(ARG_EDIT_MINUTES, 0);

        if (savedInstanceState != null) {
            hourValue = savedInstanceState.getInt(ARG_EDIT_HOURS, hourValue);
            minuteValue = savedInstanceState.getInt(ARG_EDIT_MINUTES, minuteValue);
        }

        mBinding = AlarmSpinnerDelayPickerBinding.inflate(getLayoutInflater());

        mBinding.hourTitle.setTypeface(typeface);
        mBinding.minuteTitle.setTypeface(typeface);

        // Hours and minutes setup
        mBinding.hourPicker.setHapticFeedbackEnabled(true);
        mBinding.hourPicker.setMinValue(0);
        mBinding.hourPicker.setMaxValue(24);
        mBinding.hourPicker.setNextFocusForwardId(R.id.minute_picker);

        mBinding.minutePicker.setMinValue(0);
        mBinding.minutePicker.setMaxValue(59);
        mBinding.minutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
        mBinding.minutePicker.setNextFocusForwardId(View.NO_ID);

        // Set default values at start
        mBinding.hourPicker.setValue(hourValue);
        mBinding.minutePicker.setValue(minuteValue);

        setupEditTextInput(mBinding.hourPicker);
        setupEditTextInput(mBinding.minutePicker);

        return CustomDialog.create(
            requireContext(),
            R.style.SpinnerDialogTheme,
            null,
            getString(R.string.delay_picker_dialog_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> {
                mBinding.hourPicker.clearFocus();
                mBinding.minutePicker.clearFocus();
                setAlarmDelay();
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            alertDialog -> {
                mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                mBinding.hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                    picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    updateUiState();
                });
                mBinding.minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                    picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    updateUiState();
                });

                // Initial state
                updateUiState();
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mBinding = null;
        mOkButton = null;
    }

    /**
     * Sets a new alarm based on the values selected in the hour and minute pickers.
     * <p>If the selected hour is 24, minutes are forced to 0.
     * The normalized data is then sent via a FragmentResult to the calling fragment
     * for it to take effect.</p>
     */
    private void setAlarmDelay() {
        int hours = mBinding.hourPicker.getValue();
        int minutes = mBinding.minutePicker.getValue();

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
        boolean minutesEnabled = mBinding.hourPicker.getValue() != 24;

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
        return !(mBinding.hourPicker.getValue() == 0 && mBinding.minutePicker.getValue() == 0);
    }

    /**
     * Enables or disables the minute picker and updates its visual appearance accordingly.
     *
     * <p>If disabling, the minute picker is visually dimmed (by changing text color or alpha)
     * to indicate it is inactive. When enabled, it restores the default enabled appearance.</p>
     */
    private void updateMinutePickerEnabledState(boolean enabled) {
        mBinding.minutePicker.setEnabled(enabled);

        if (SdkUtils.isAtLeastAndroid10()) {
            mBinding.minutePicker.setTextColor(enabled
                ? mBinding.hourPicker.getTextColor()
                : ContextCompat.getColor(requireContext(), R.color.colorDisabled));
        } else {
            mBinding.minutePicker.setAlpha(enabled ? 1f : 0.5f);
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
                if (numberPicker == mBinding.hourPicker) {
                    editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                } else if (numberPicker == mBinding.minutePicker) {
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                }
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        int hour = mBinding.hourPicker.getValue();
                        int minute = mBinding.minutePicker.getValue();

                        if (numberPicker == mBinding.hourPicker) {
                            if (s.toString().isEmpty()) {
                                hour = 0;
                            } else {
                                hour = Integer.parseInt(s.toString());
                            }

                            editText.setImeOptions(hour == 24
                                ? EditorInfo.IME_ACTION_DONE
                                : EditorInfo.IME_ACTION_NEXT);

                            final InputMethodManager inputMethodManager =
                                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            inputMethodManager.restartInput(editText);
                        } else if (numberPicker == mBinding.minutePicker) {
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

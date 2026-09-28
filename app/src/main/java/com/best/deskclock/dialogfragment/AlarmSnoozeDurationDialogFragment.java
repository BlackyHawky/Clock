// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_SNOOZE_DURATION;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmSnoozeDurationDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/**
 * DialogFragment to set a new snooze duration for alarms.
 */
public class AlarmSnoozeDurationDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of AlarmSnoozeDurationDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_alarm_snooze_duration_dialog";

    private static final String ALARM_SNOOZE_DURATION = "alarm_snooze_duration_";
    private static final String ARG_PREF_KEY = ALARM_SNOOZE_DURATION + "arg_pref_key";
    private static final String ARG_EDIT_ALARM_DAYS = ALARM_SNOOZE_DURATION + "arg_edit_alarm_days";
    private static final String ARG_EDIT_ALARM_HOURS = ALARM_SNOOZE_DURATION + "arg_edit_alarm_hours";
    private static final String ARG_EDIT_ALARM_MINUTES = ALARM_SNOOZE_DURATION + "arg_edit_alarm_minutes";
    private static final String ARG_SNOOZE_DURATION_NONE = ALARM_SNOOZE_DURATION + "arg_crescendo_duration_none";
    public static final String RESULT_PREF_KEY = ALARM_SNOOZE_DURATION + "result_pref_key";
    public static final String REQUEST_KEY = ALARM_SNOOZE_DURATION + "request_key";
    public static final String ALARM_SNOOZE_DURATION_VALUE = ALARM_SNOOZE_DURATION + "value";

    private AlarmSnoozeDurationDialogBinding mBinding;
    private String mPrefKey;
    private Button mOkButton;
    private Button mDefaultButton;
    private Typeface mTypeFace;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;
    private boolean isUpdatingCheckboxes = false;

    /**
     * Creates a new instance of {@link AlarmSnoozeDurationDialogFragment} for use
     * in the settings screen, where the snooze duration is configured independently
     * of a specific alarm.
     *
     * @param key            The shared preference key used to identify the setting.
     * @param snoozeDuration The snooze duration in minutes.
     */
    @NonNull
    public static AlarmSnoozeDurationDialogFragment newInstance(@NonNull String key, int snoozeDuration) {

        Bundle args = new Bundle();

        boolean isNone = snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED;

        int days = 0;
        int hours = 0;
        int minutes = 0;

        if (!isNone) {
            // 1 day = 24 hours * 60 minutes = 1440 minutes
            days = snoozeDuration / 1440;
            hours = (snoozeDuration % 1440) / 60;
            minutes = snoozeDuration % 60;
        }

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_ALARM_DAYS, days);
        args.putInt(ARG_EDIT_ALARM_HOURS, hours);
        args.putInt(ARG_EDIT_ALARM_MINUTES, minutes);
        args.putBoolean(ARG_SNOOZE_DURATION_NONE, isNone);

        AlarmSnoozeDurationDialogFragment frag = new AlarmSnoozeDurationDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link AlarmSnoozeDurationDialogFragment} for use
     * in the alarm editing panel, where the snooze duration is configured for a specific alarm.
     *
     * @param snoozeDuration The snooze duration in minutes.
     */
    @NonNull
    public static AlarmSnoozeDurationDialogFragment newInstance(int snoozeDuration) {
        final Bundle args = new Bundle();

        boolean isNone = snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED;

        int days = 0;
        int hours = 0;
        int minutes = 0;

        if (!isNone) {
            // 1 day = 24 hours * 60 minutes = 1440 minutes
            days = snoozeDuration / 1440;
            hours = (snoozeDuration % 1440) / 60;
            minutes = snoozeDuration % 60;
        }

        args.putInt(ARG_EDIT_ALARM_DAYS, days);
        args.putInt(ARG_EDIT_ALARM_HOURS, hours);
        args.putInt(ARG_EDIT_ALARM_MINUTES, minutes);
        args.putBoolean(ARG_SNOOZE_DURATION_NONE, isNone);

        final AlarmSnoozeDurationDialogFragment fragment = new AlarmSnoozeDurationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link AlarmSnoozeDurationDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull AlarmSnoozeDurationDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        String daysStr = mBinding.editDays.getText() != null ? mBinding.editDays.getText().toString() : "";
        String hoursStr = mBinding.editHours.getText() != null ? mBinding.editHours.getText().toString() : "";
        String minutesStr = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

        int days = daysStr.isEmpty() ? 0 : Integer.parseInt(daysStr);
        int hours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);
        int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);

        outState.putInt(ARG_EDIT_ALARM_DAYS, days);
        outState.putInt(ARG_EDIT_ALARM_HOURS, hours);
        outState.putInt(ARG_EDIT_ALARM_MINUTES, minutes);

        outState.putBoolean(ARG_SNOOZE_DURATION_NONE, mBinding.snoozeDurationNoneCheckbox.isChecked());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        mTypeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();

        mPrefKey = args.getString(ARG_PREF_KEY, null);
        int editDays = args.getInt(ARG_EDIT_ALARM_DAYS, 0);
        int editHours = args.getInt(ARG_EDIT_ALARM_HOURS, 0);
        int editMinutes = args.getInt(ARG_EDIT_ALARM_MINUTES, 0);
        boolean isNone = args.getBoolean(ARG_SNOOZE_DURATION_NONE, false);

        if (savedInstanceState != null) {
            editDays = savedInstanceState.getInt(ARG_EDIT_ALARM_DAYS, editDays);
            editHours = savedInstanceState.getInt(ARG_EDIT_ALARM_HOURS, editHours);
            editMinutes = savedInstanceState.getInt(ARG_EDIT_ALARM_MINUTES, editMinutes);
            isNone = savedInstanceState.getBoolean(ARG_SNOOZE_DURATION_NONE, isNone);
        }

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        mBinding = AlarmSnoozeDurationDialogBinding.inflate(getLayoutInflater());

        mBinding.snoozeDurationNoneCheckbox.setTypeface(mTypeFace);
        mBinding.snoozeDurationNoneCheckbox.setChecked(isNone);

        mBinding.editDays.setTypeface(mTypeFace);
        mBinding.editDays.setText(String.valueOf(editDays));

        updateInputSate();

        mBinding.editDays.selectAll();
        mBinding.editDays.requestFocus();
        mBinding.editDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editDays.addTextChangedListener(mTextWatcher);
        mBinding.editDays.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editDays.selectAll();
            }
        });

        mBinding.editHours.setTypeface(mTypeFace);
        if (!isNone) {
            mBinding.editHours.setText(String.valueOf(editHours));
        }
        mBinding.editHours.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mBinding.editHours.selectAll();
        mBinding.editHours.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editHours.addTextChangedListener(mTextWatcher);
        mBinding.editHours.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editHours.selectAll();
            }
        });

        mBinding.editMinutes.setTypeface(mTypeFace);
        if (!isNone) {
            mBinding.editMinutes.setText(String.valueOf(editMinutes));
        }
        mBinding.editMinutes.selectAll();
        mBinding.editMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editMinutes.setOnEditorActionListener(new ImeDoneListener());
        mBinding.editMinutes.addTextChangedListener(mTextWatcher);
        mBinding.editMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editMinutes.selectAll();
            }
        });

        mBinding.snoozeDurationNoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;
            updateInputSate();
            maybeRequestDaysFocus();
            isUpdatingCheckboxes = false;
        });

        return CustomDialog.create(
            requireContext(),
            null,
            mPrefKey != null ? null : AppCompatResources.getDrawable(requireContext(), R.drawable.ic_snooze),
            getString(R.string.snooze_duration_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> setAlarmSnoozeDurationInMinutes(),
            getString(android.R.string.cancel),
            null,
            getString(R.string.label_default),
            (d, w) -> applySnoozeDurationInMinutes(DEFAULT_ALARM_SNOOZE_DURATION),
            alertDialog -> {
                mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                mDefaultButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                String inputDaysText = mBinding.editDays.getText() != null ? mBinding.editDays.getText().toString() : "";
                String inputHoursText = mBinding.editHours.getText() != null ? mBinding.editHours.getText().toString() : "";
                String inputMinutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

                mOkButton.setEnabled(!isInvalidInput(inputDaysText, inputHoursText, inputMinutesText));
                mDefaultButton.setEnabled(isNotDefaultAlarmSnoozeDuration(inputDaysText, inputHoursText, inputMinutesText));
            },
            isNone ? CustomDialog.SoftInputMode.NONE : CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBinding.snoozeDurationNoneCheckbox.isChecked()) {
            mBinding.editDays.requestFocus();
            mBinding.editDays.postDelayed(() -> {
                if (getDialog() != null) {
                    Utils.showKeyboard(getDialog().getWindow(), mBinding.editDays);
                }
            }, Utils.UI_SETTLE_DELAY_MS);
        }
    }

    @Override
    public void onDestroyView() {
        // Stop callbacks from the IME since there is no view to process them.
        mBinding.editDays.setOnEditorActionListener(null);
        mBinding.editDays.removeTextChangedListener(mTextWatcher);
        mBinding.editDays.setOnFocusChangeListener(null);

        mBinding.editHours.setOnEditorActionListener(null);
        mBinding.editHours.removeTextChangedListener(mTextWatcher);
        mBinding.editHours.setOnFocusChangeListener(null);

        mBinding.editMinutes.setOnEditorActionListener(null);
        mBinding.editMinutes.removeTextChangedListener(mTextWatcher);
        mBinding.editMinutes.setOnFocusChangeListener(null);

        mInput = null;

        mBinding = null;

        mOkButton = null;
        mDefaultButton = null;

        mTypeFace = null;

        super.onDestroyView();
    }

    /**
     * Updates the enabled state and helper text of the input fields based on the state
     * of the "None" checkbox.
     *
     * <p>If the checkbox is checked, the inputs are disabled and their helper texts are cleared.
     * Otherwise, the inputs are enabled and appropriate helper texts are shown.</p>
     */
    private void updateInputSate() {
        boolean disable = mBinding.snoozeDurationNoneCheckbox.isChecked();

        mBinding.textInputLayoutDays.setTypeface(mTypeFace);
        mBinding.textInputLayoutHours.setTypeface(mTypeFace);
        mBinding.textInputLayoutMinutes.setTypeface(mTypeFace);

        mBinding.textInputLayoutDays.setEnabled(!disable);
        mBinding.textInputLayoutHours.setEnabled(!disable);
        mBinding.textInputLayoutMinutes.setEnabled(!disable);

        if (disable) {
            mBinding.textInputLayoutDays.setHelperText(null);
            mBinding.textInputLayoutHours.setHelperText(null);
            mBinding.textInputLayoutMinutes.setHelperText(null);

            mBinding.editDays.setText("");
            mBinding.editHours.setText("");
            mBinding.editMinutes.setText("");
        } else {
            mBinding.textInputLayoutDays.setHelperText(getString(R.string.alarm_days_warning_box_text));
            mBinding.textInputLayoutHours.setHelperText(getString(R.string.alarm_hours_snooze_warning_box_text));
            mBinding.textInputLayoutMinutes.setHelperText(getString(R.string.alarm_minutes_warning_box_text));

            TextView daysHelper = mBinding.textInputLayoutDays.findViewById(com.google.android.material.R.id.textinput_helper_text);
            daysHelper.setTypeface(mTypeFace);

            TextView hoursHelper = mBinding.textInputLayoutHours.findViewById(com.google.android.material.R.id.textinput_helper_text);
            hoursHelper.setTypeface(mTypeFace);

            TextView minutesHelper = mBinding.textInputLayoutMinutes.findViewById(com.google.android.material.R.id.textinput_helper_text);
            minutesHelper.setTypeface(mTypeFace);

            String daysText = mBinding.editDays.getText() != null ? mBinding.editDays.getText().toString() : "";

            if ("31".equals(daysText)) {
                mBinding.editDays.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mBinding.editDays.setOnEditorActionListener(new ImeDoneListener());
                mBinding.textInputLayoutHours.setEnabled(false);
                mBinding.textInputLayoutMinutes.setEnabled(false);
            } else {
                mBinding.editDays.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mBinding.textInputLayoutHours.setEnabled(true);
                mBinding.textInputLayoutMinutes.setEnabled(true);
            }

            mBinding.editDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }

    /**
     * Requests focus for the days input field and shows the keyboard if the "None" checkbox is not selected.
     *
     * <p>This method ensures that the user can immediately start typing a duration when the dialog is in manual entry mode.</p>
     */
    private void maybeRequestDaysFocus() {
        if (!mBinding.snoozeDurationNoneCheckbox.isChecked()) {
            mBinding.editDays.requestFocus();

            if (getDialog() != null) {
                Utils.showKeyboard(getDialog().getWindow(), mBinding.editDays);
            }
        }
    }

    /**
     * Set the alarm snooze duration in minutes.
     */
    private void setAlarmSnoozeDurationInMinutes() {
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int snoozeDurationInMinutes;

        if (mBinding.snoozeDurationNoneCheckbox.isChecked()) {
            snoozeDurationInMinutes = ALARM_SNOOZE_DURATION_DISABLED;
        } else {
            String daysText = mBinding.editDays.getText() != null ? mBinding.editDays.getText().toString() : "";
            String hoursText = mBinding.editHours.getText() != null ? mBinding.editHours.getText().toString() : "";
            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

            if (!daysText.isEmpty()) {
                days = Integer.parseInt(daysText);
            }

            if (!hoursText.isEmpty()) {
                hours = Integer.parseInt(hoursText);
            }

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (days == 0 && hours == 0 && minutes == 0) {
                mBinding.snoozeDurationNoneCheckbox.setChecked(true);
                snoozeDurationInMinutes = ALARM_SNOOZE_DURATION_DISABLED;
            } else {
                snoozeDurationInMinutes = days * 1440 + hours * 60 + minutes;
            }
        }

        applySnoozeDurationInMinutes(snoozeDurationInMinutes);
    }

    /**
     * Apply the snooze duration in minutes.
     */
    private void applySnoozeDurationInMinutes(int snoozeDurationInMinutes) {
        Bundle result = new Bundle();
        result.putInt(ALARM_SNOOZE_DURATION_VALUE, snoozeDurationInMinutes);

        if (mPrefKey != null) {
            result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
        }

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    /**
     * @return {@code true} if:
     * <ul>
     *     <li>hours are less than 0 or greater than 24</li>
     *     <li>minutes are less than 0 or greater than 59</li>
     * </ul>
     * {@code false} otherwise.
     */
    private boolean isInvalidInput(@NonNull String daysText, @NonNull String hoursText, @NonNull String minutesText) {
        int days = 0;
        int hours = 0;
        int minutes = 0;

        if (!daysText.isEmpty()) {
            days = Integer.parseInt(daysText);
        }

        if (!hoursText.isEmpty()) {
            hours = Integer.parseInt(hoursText);
        }

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        return days < 0 || days > 31 || hours < 0 || hours > 23 || minutes < 0 || minutes > 59;
    }

    /**
     * Update the dialog icon, title, and OK button for invalid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForInvalidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(
                requireContext(), R.drawable.ic_error), null, null, null);
            if (mPrefKey != null) {
                titleText.setCompoundDrawablePadding((int) dpToPx(18, getResources().getDisplayMetrics()));
            }
            titleText.setText(getString(R.string.timer_time_warning_box_title));
        }

        String daysText = Objects.requireNonNull(mBinding.editDays.getText()).toString();
        String hoursText = Objects.requireNonNull(mBinding.editHours.getText()).toString();
        String minutesText = Objects.requireNonNull(mBinding.editMinutes.getText()).toString();

        boolean daysInvalid = !daysText.isEmpty() && (Integer.parseInt(daysText) < 0 || Integer.parseInt(daysText) > 31);
        boolean hoursInvalid = !hoursText.isEmpty() && (Integer.parseInt(hoursText) < 0 || Integer.parseInt(hoursText) > 23);
        boolean minutesInvalid = !minutesText.isEmpty() && (Integer.parseInt(minutesText) < 0 || Integer.parseInt(minutesText) > 59);

        int invalidColor = ContextCompat.getColor(requireContext(), R.color.md_theme_error);
        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mBinding.textInputLayoutDays.setBoxStrokeColor(daysInvalid ? invalidColor : validColor);
        mBinding.textInputLayoutDays.setHintTextColor(daysInvalid
            ? ColorStateList.valueOf(invalidColor)
            : ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutDays.setEnabled(!(hoursInvalid || minutesInvalid));

        mBinding.textInputLayoutHours.setBoxStrokeColor(hoursInvalid ? invalidColor : validColor);
        mBinding.textInputLayoutHours.setHintTextColor(hoursInvalid
            ? ColorStateList.valueOf(invalidColor)
            : ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutHours.setEnabled(!(daysInvalid || minutesInvalid));

        mBinding.textInputLayoutMinutes.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mBinding.textInputLayoutMinutes.setHintTextColor(minutesInvalid
            ? ColorStateList.valueOf(invalidColor)
            : ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutMinutes.setEnabled(!(daysInvalid || hoursInvalid));

        if (mOkButton != null) {
            mOkButton.setEnabled(false);
        }
    }

    /**
     * Update the dialog icon, title, and OK button for valid entries.
     * The dialog default button is enabled if the typed value is not the default value.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForValidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            if (mPrefKey != null) {
                titleText.setCompoundDrawables(null, null, null, null);
            } else {
                titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(
                    requireContext(), R.drawable.ic_snooze), null, null, null);
            }

            titleText.setText(getString(R.string.snooze_duration_title));
        }

        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mBinding.textInputLayoutDays.setBoxStrokeColor(validColor);
        mBinding.textInputLayoutDays.setHintTextColor(ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutDays.setEnabled(!mBinding.snoozeDurationNoneCheckbox.isChecked());

        mBinding.textInputLayoutHours.setBoxStrokeColor(validColor);
        mBinding.textInputLayoutHours.setHintTextColor(ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutHours.setEnabled(!mBinding.snoozeDurationNoneCheckbox.isChecked());

        mBinding.textInputLayoutMinutes.setBoxStrokeColor(validColor);
        mBinding.textInputLayoutMinutes.setHintTextColor(ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutMinutes.setEnabled(!mBinding.snoozeDurationNoneCheckbox.isChecked());

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }

        if (mDefaultButton != null) {
            String daysText = mBinding.editDays.getText() != null ? mBinding.editDays.getText().toString() : "";
            String hoursText = mBinding.editHours.getText() != null ? mBinding.editHours.getText().toString() : "";
            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

            mDefaultButton.setEnabled(isNotDefaultAlarmSnoozeDuration(daysText, hoursText, minutesText));
        }
    }

    /**
     * @return {@code true} if the alarm snooze duration is not the default value;
     * {@code false} otherwise.
     */
    private boolean isNotDefaultAlarmSnoozeDuration(@NonNull String daysText, @NonNull String hoursText, @NonNull String minutesText) {
        int days = daysText.isEmpty() ? 0 : Integer.parseInt(daysText);
        int hours = hoursText.isEmpty() ? 0 : Integer.parseInt(hoursText);
        int minutes = minutesText.isEmpty() ? 0 : Integer.parseInt(minutesText);

        int snoozeDuration = days * 1440 + hours * 60 + minutes;

        return snoozeDuration != DEFAULT_ALARM_SNOOZE_DURATION;
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the hours field, if the hours are equal to 24, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(@Nullable CharSequence charSequence, int start, int before, int count) {
            if (mBinding.snoozeDurationNoneCheckbox.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String daysText = mBinding.editDays.getText() != null ? mBinding.editDays.getText().toString() : "";
            String hoursText = mBinding.editHours.getText() != null ? mBinding.editHours.getText().toString() : "";
            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

            if (isInvalidInput(daysText, hoursText, minutesText)) {
                updateDialogForInvalidInput();
                return;
            }

            updateDialogForValidInput();

            int days = 0;

            if (!daysText.isEmpty()) {
                days = Integer.parseInt(daysText);
            }

            if (days == 31) {
                mBinding.editDays.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mBinding.editDays.setOnEditorActionListener(new ImeDoneListener());
                mBinding.textInputLayoutHours.setEnabled(false);
                mBinding.textInputLayoutMinutes.setEnabled(false);

                if (!"0".equals(hoursText)) {
                    mBinding.editHours.setText("0");
                }

                if (!"0".equals(minutesText)) {
                    mBinding.editMinutes.setText("0");
                }
            } else {
                mBinding.editDays.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mBinding.textInputLayoutHours.setEnabled(true);
                mBinding.textInputLayoutMinutes.setEnabled(true);
            }

            mBinding.editDays.setInputType(InputType.TYPE_CLASS_NUMBER);
            mInput.restartInput(mBinding.editDays);
        }

        @Override
        public void beforeTextChanged(@Nullable CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(@Nullable Editable editable) {
        }
    }

    /**
     * Handles completing the new alarm snooze duration from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(@NonNull TextView v, int actionId, @Nullable KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputDaysText = Objects.requireNonNull(mBinding.editDays.getText()).toString();
                String inputHoursText = Objects.requireNonNull(mBinding.editHours.getText()).toString();
                String inputMinutesText = Objects.requireNonNull(mBinding.editMinutes.getText()).toString();

                if (isInvalidInput(inputDaysText, inputHoursText, inputMinutesText)) {
                    updateDialogForInvalidInput();
                } else {
                    setAlarmSnoozeDurationInMinutes();
                    dismiss();
                }

                return true;
            }

            return false;
        }
    }

}

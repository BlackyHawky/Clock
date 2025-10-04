// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.provider.Alarm;
import com.best.deskclock.utils.SdkUtils;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
    private static final String ARG_EDIT_ALARM_HOURS = ALARM_SNOOZE_DURATION + "arg_edit_alarm_hours";
    private static final String ARG_EDIT_ALARM_MINUTES = ALARM_SNOOZE_DURATION + "arg_edit_alarm_minutes";
    private static final String ARG_CRESCENDO_DURATION_NONE = ALARM_SNOOZE_DURATION + "arg_crescendo_duration_none";
    public static final String RESULT_PREF_KEY = ALARM_SNOOZE_DURATION + "result_pref_key";
    public static final String REQUEST_KEY = ALARM_SNOOZE_DURATION + "request_key";
    public static final String ALARM_SNOOZE_DURATION_VALUE = ALARM_SNOOZE_DURATION + "value";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_TAG = "arg_tag";

    private Context mContext;
    private Alarm mAlarm;
    private String mTag;
    private TextInputLayout mHoursInputLayout;
    private TextInputLayout mMinutesInputLayout;
    private TextInputEditText mEditHours;
    private TextInputEditText mEditMinutes;
    private MaterialCheckBox mNoneCheckbox;
    private Button mOkButton;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;
    private boolean isUpdatingCheckboxes = false;

    /**
     * Creates a new instance of {@link AlarmSnoozeDurationDialogFragment} for use
     * in the settings screen, where the snooze duration is configured independently
     * of a specific alarm.
     *
     * @param key          The shared preference key used to identify the setting.
     * @param totalMinutes The snooze duration in minutes.
     */
    public static AlarmSnoozeDurationDialogFragment newInstance(String key, int totalMinutes,
                                                                boolean isNone) {

        Bundle args = new Bundle();

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_ALARM_HOURS, hours);
        args.putInt(ARG_EDIT_ALARM_MINUTES, minutes);
        args.putBoolean(ARG_CRESCENDO_DURATION_NONE, isNone);

        AlarmSnoozeDurationDialogFragment frag = new AlarmSnoozeDurationDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link AlarmSnoozeDurationDialogFragment} for use
     * in the expanded alarm view, where the snooze duration is configured for a specific alarm.
     *
     * @param alarm           The alarm instance being edited.
     * @param snoozeDuration  The snooze duration in minutes.
     * @param tag             A tag identifying the fragment in the fragment manager.
     */
    public static AlarmSnoozeDurationDialogFragment newInstance(Alarm alarm, int snoozeDuration,
                                                                boolean isNone, String tag) {
        final Bundle args = new Bundle();

        int hours = snoozeDuration / 60;
        int minutes = snoozeDuration % 60;

        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);
        args.putInt(ARG_EDIT_ALARM_HOURS, hours);
        args.putInt(ARG_EDIT_ALARM_MINUTES, minutes);
        args.putBoolean(ARG_CRESCENDO_DURATION_NONE, isNone);

        final AlarmSnoozeDurationDialogFragment fragment = new AlarmSnoozeDurationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Replaces any existing AlarmSnoozeDurationDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, AlarmSnoozeDurationDialogFragment fragment) {
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Finish any outstanding fragment work.
        manager.executePendingTransactions();

        final FragmentTransaction tx = manager.beginTransaction();

        // Remove existing instance of this DialogFragment if necessary.
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
        // As long as this dialog exists, save its state.
        if (mEditHours != null && mEditMinutes != null) {
            String hoursStr = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
            String minutesStr = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            int hours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);
            int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);

            outState.putInt(ARG_EDIT_ALARM_HOURS, hours);
            outState.putInt(ARG_EDIT_ALARM_MINUTES, minutes);
        }

        outState.putBoolean(ARG_CRESCENDO_DURATION_NONE, mNoneCheckbox.isChecked());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = requireContext();

        final Bundle args = requireArguments();
        mAlarm = SdkUtils.isAtLeastAndroid13()
                ? args.getParcelable(ARG_ALARM, Alarm.class)
                : args.getParcelable(ARG_ALARM);
        mTag = args.getString(ARG_TAG);

        int editHours = args.getInt(ARG_EDIT_ALARM_HOURS, 0);
        int editMinutes = args.getInt(ARG_EDIT_ALARM_MINUTES, 0);
        boolean isNone = args.getBoolean(ARG_CRESCENDO_DURATION_NONE, false);
        if (savedInstanceState != null) {
            editHours = savedInstanceState.getInt(ARG_EDIT_ALARM_HOURS, editHours);
            editMinutes = savedInstanceState.getInt(ARG_EDIT_ALARM_MINUTES, editMinutes);
            isNone = savedInstanceState.getBoolean(ARG_CRESCENDO_DURATION_NONE, isNone);
        }

        mInput = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        View view = getLayoutInflater().inflate(R.layout.alarm_snooze_duration_dialog, null);

        mHoursInputLayout = view.findViewById(R.id.dialog_input_layout_hours);
        mMinutesInputLayout = view.findViewById(R.id.dialog_input_layout_minutes);
        mEditHours = view.findViewById(R.id.edit_hours);
        mEditMinutes = view.findViewById(R.id.edit_minutes);
        mNoneCheckbox = view.findViewById(R.id.snooze_duration_none);

        mNoneCheckbox.setChecked(isNone);

        mEditHours.setText(String.valueOf(editHours));

        updateInputSate();

        mEditHours.selectAll();
        mEditHours.requestFocus();
        mEditHours.addTextChangedListener(mTextWatcher);
        mEditHours.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditHours.selectAll();
            }
        });

        if (editMinutes == ALARM_SNOOZE_DURATION_DISABLED) {
            mEditMinutes.setText("");
        } else {
            mEditMinutes.setText(String.valueOf(editMinutes));
        }
        mEditMinutes.selectAll();
        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
        mEditMinutes.addTextChangedListener(mTextWatcher);
        mEditMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditMinutes.selectAll();
            }
        });

        mNoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;

            updateInputSate();

            maybeRequestHoursFocus();

            isUpdatingCheckboxes = false;
        });

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(R.string.snooze_duration_title))
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        setAlarmSnoozeDuration())
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(dialog -> {
            mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

            String hoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            mOkButton.setEnabled(!isInvalidInput(hoursText, minutesText));
        });

        final Window alertDialogWindow = alertDialog.getWindow();
        if (alertDialogWindow != null) {
            alertDialogWindow.setSoftInputMode(SOFT_INPUT_ADJUST_PAN | SOFT_INPUT_STATE_VISIBLE);
        }

        return alertDialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mNoneCheckbox.isChecked()) {
            mEditHours.requestFocus();
            mEditHours.postDelayed(() -> {
                if (mInput != null) {
                    mInput.showSoftInput(mEditHours, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop callbacks from the IME since there is no view to process them.
        mEditHours.setOnEditorActionListener(null);
        mEditHours.removeTextChangedListener(mTextWatcher);

        mEditMinutes.setOnEditorActionListener(null);
        mEditMinutes.removeTextChangedListener(mTextWatcher);
    }

    /**
     * Updates the enabled state and helper text of the input fields based on the state
     * of the "None" checkbox.
     *
     * <p>If the checkbox is checked, the inputs are disabled and their helper texts are cleared.
     * Otherwise, the inputs are enabled and appropriate helper texts are shown.</p>
     */
    private void updateInputSate() {
        boolean disable = mNoneCheckbox.isChecked();

        mHoursInputLayout.setEnabled(!disable);
        mMinutesInputLayout.setEnabled(!disable);

        if (disable) {
            mHoursInputLayout.setHelperText(null);
            mMinutesInputLayout.setHelperText(null);
            mEditHours.setText("");
            mEditMinutes.setText("");
        } else {
            mHoursInputLayout.setHelperText(getString(R.string.timer_hours_warning_box_text));
            mMinutesInputLayout.setHelperText(getString(R.string.timer_button_time_minutes_warning_box_text));

            String hoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";

            if ("24".equals(hoursText)) {
                mEditHours.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mEditHours.setOnEditorActionListener(new ImeDoneListener());
                mMinutesInputLayout.setEnabled(false);
            } else {
                mEditHours.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mMinutesInputLayout.setEnabled(true);
            }

            mEditHours.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }

    /**
     * Requests focus for the hours input field and shows the keyboard
     * if the "None" checkbox is not selected.
     *
     * <p>This method ensures that the user can immediately start typing a duration
     * when the dialog is in manual entry mode.</p>
     */
    private void maybeRequestHoursFocus() {
        if (!mNoneCheckbox.isChecked()) {
            mEditHours.requestFocus();
            mInput.showSoftInput(mEditHours, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Set the alarm snooze duration.
     */
    private void setAlarmSnoozeDuration() {
        int hours = 0;
        int minutes = 0;
        int snoozeDuration;

        if (mNoneCheckbox.isChecked()) {
            snoozeDuration = ALARM_SNOOZE_DURATION_DISABLED;
        } else {
            String hoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            if (!hoursText.isEmpty()) {
                hours = Integer.parseInt(hoursText);
            }

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (hours == 0 && minutes == 0) {
                mNoneCheckbox.setChecked(true);
                snoozeDuration = ALARM_SNOOZE_DURATION_DISABLED;
            } else {
                snoozeDuration = hours * 60 + minutes;
            }
        }

        if (mAlarm != null) {
            ((SnoozeDurationDialogHandler) requireActivity())
                    .onDialogSnoozeDurationSet(mAlarm, snoozeDuration, mTag);
        } else {
            Bundle result = new Bundle();
            result.putInt(ALARM_SNOOZE_DURATION_VALUE, snoozeDuration);
            result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }
    }

    /**
     * @return {@code true} if:
     * <ul>
     *     <li>hours are less than 0 or greater than 24</li>
     *     <li>minutes are less than 0 or greater than 59</li>
     * </ul>
     * {@code false} otherwise.
     */
    private boolean isInvalidInput(String hoursText, String minutesText) {
        int hours = 0;
        int minutes = 0;

        if (!hoursText.isEmpty()) {
            hours = Integer.parseInt(hoursText);
        }

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        return hours < 0 || hours > 24 || minutes < 0 || minutes > 59;
    }

    /**
     * Update the dialog icon, title, and OK button for invalid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForInvalidInput() {
        final Drawable drawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_error);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    mContext, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        AlertDialog alertDialog = (AlertDialog) requireDialog();
        alertDialog.setIcon(drawable);
        alertDialog.setTitle(getString(R.string.timer_time_warning_box_title));

        String hoursText = Objects.requireNonNull(mEditHours.getText()).toString();
        String minutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
        boolean hoursInvalid = (!hoursText.isEmpty() && Integer.parseInt(hoursText) < 0)
                || (!hoursText.isEmpty() && Integer.parseInt(hoursText) > 24);
        boolean minutesInvalid = (!minutesText.isEmpty() && Integer.parseInt(minutesText) < 0)
                || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 59);
        int invalidColor = ContextCompat.getColor(mContext, R.color.md_theme_error);
        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mHoursInputLayout.setBoxStrokeColor(hoursInvalid ? invalidColor : validColor);
        mHoursInputLayout.setHintTextColor(hoursInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
        mHoursInputLayout.setEnabled(!minutesInvalid);

        mMinutesInputLayout.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mMinutesInputLayout.setHintTextColor(minutesInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(!hoursInvalid);

        if (mOkButton != null) {
            mOkButton.setEnabled(false);
        }
    }

    /**
     * Update the dialog icon, title, and OK button for valid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForValidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();
        alertDialog.setIcon(null);
        alertDialog.setTitle(getString(R.string.snooze_duration_title));

        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mHoursInputLayout.setBoxStrokeColor(validColor);
        mHoursInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mHoursInputLayout.setEnabled(!mNoneCheckbox.isChecked());

        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(!mNoneCheckbox.isChecked());

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the hours field, if the hours are equal to 24, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mNoneCheckbox.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String hoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            if (isInvalidInput(hoursText, minutesText)) {
                updateDialogForInvalidInput();
                return;
            }

            updateDialogForValidInput();

            int hours = 0;

            if (!hoursText.isEmpty()) {
                hours = Integer.parseInt(hoursText);
            }

            if (hours == 24) {
                mEditHours.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mEditHours.setOnEditorActionListener(new ImeDoneListener());
                mMinutesInputLayout.setEnabled(false);

                if(!"0".equals(minutesText)) {
                    mEditMinutes.setText("0");
                }
            } else {
                mEditHours.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mMinutesInputLayout.setEnabled(true);
            }

            mEditHours.setInputType(InputType.TYPE_CLASS_NUMBER);
            mInput.restartInput(mEditHours);
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    /**
     * Handles completing the new alarm snooze duration from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputHoursText = Objects.requireNonNull(mEditHours.getText()).toString();
                String inputMinutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
                if (isInvalidInput(inputHoursText, inputMinutesText)) {
                    updateDialogForInvalidInput();
                } else {
                    setAlarmSnoozeDuration();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

    public interface SnoozeDurationDialogHandler {
        void onDialogSnoozeDurationSet(Alarm alarm, int crescendoDuration, String tag);
    }

}

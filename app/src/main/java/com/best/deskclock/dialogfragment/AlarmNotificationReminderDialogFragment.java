// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_NOTIFICATION_REMINDER;

import android.annotation.SuppressLint;
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
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

/**
 * DialogFragment to set a new notification reminder time for alarms.
 */
public class AlarmNotificationReminderDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of AlarmNotificationReminderDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_alarm_notification_reminder_dialog";

    private static final String ALARM_NOTIFICATION_REMINDER_TIME = "alarm_notification_reminder_time_";
    private static final String ARG_PREF_KEY = ALARM_NOTIFICATION_REMINDER_TIME + "arg_pref_key";
    private static final String ARG_EDIT_HOURS = ALARM_NOTIFICATION_REMINDER_TIME + "arg_edit_alarm_hours";
    private static final String ARG_EDIT_MINUTES = ALARM_NOTIFICATION_REMINDER_TIME + "arg_edit_alarm_minutes";
    public static final String RESULT_PREF_KEY = ALARM_NOTIFICATION_REMINDER_TIME + "result_pref_key";
    public static final String REQUEST_KEY = ALARM_NOTIFICATION_REMINDER_TIME + "request_key";
    public static final String ALARM_NOTIFICATION_REMINDER_VALUE = ALARM_NOTIFICATION_REMINDER_TIME + "value";

    private Context mContext;
    private TextInputLayout mHoursInputLayout;
    private TextInputLayout mMinutesInputLayout;
    private TextInputEditText mEditHours;
    private TextInputEditText mEditMinutes;
    private Button mOkButton;
    private Button mDefaultButton;
    private Typeface mTypeFace;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;

    /**
     * Creates a new instance of {@link AlarmNotificationReminderDialogFragment} for use in the settings screen
     *
     * @param key                      The shared preference key used to identify the setting.
     * @param notificationReminderTime The notification reminder time in minutes.
     */
    public static AlarmNotificationReminderDialogFragment newInstance(String key, int notificationReminderTime) {

        Bundle args = new Bundle();

        int hours = notificationReminderTime / 60;
        int minutes = notificationReminderTime % 60;

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_HOURS, hours);
        args.putInt(ARG_EDIT_MINUTES, minutes);

        AlarmNotificationReminderDialogFragment frag = new AlarmNotificationReminderDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Displays {@link AlarmNotificationReminderDialogFragment}.
     */
    public static void show(FragmentManager manager, AlarmNotificationReminderDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
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

            outState.putInt(ARG_EDIT_HOURS, hours);
            outState.putInt(ARG_EDIT_MINUTES, minutes);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = requireContext();
        SharedPreferences prefs = getDefaultSharedPreferences(mContext);
        mTypeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();

        int editHours = args.getInt(ARG_EDIT_HOURS, 0);
        int editMinutes = args.getInt(ARG_EDIT_MINUTES, 0);

        if (savedInstanceState != null) {
            editHours = savedInstanceState.getInt(ARG_EDIT_HOURS, editHours);
            editMinutes = savedInstanceState.getInt(ARG_EDIT_MINUTES, editMinutes);
        }

        mInput = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        @SuppressLint("InflateParams")
        View dialogView = getLayoutInflater().inflate(R.layout.alarm_notification_reminder_dialog, null);

        mHoursInputLayout = dialogView.findViewById(R.id.dialog_input_layout_hours);
        mMinutesInputLayout = dialogView.findViewById(R.id.dialog_input_layout_minutes);
        mEditHours = dialogView.findViewById(R.id.edit_hours);
        mEditMinutes = dialogView.findViewById(R.id.edit_minutes);

        mEditHours.setTypeface(mTypeFace);
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

        mEditMinutes.setTypeface(mTypeFace);
        mEditMinutes.setText(String.valueOf(editMinutes));
        mEditMinutes.selectAll();
        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
        mEditMinutes.addTextChangedListener(mTextWatcher);
        mEditMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditMinutes.selectAll();
            }
        });

        return CustomDialog.create(
            mContext,
            null,
            null,
            getString(R.string.alarm_notification_reminder_title),
            null,
            dialogView,
            getString(android.R.string.ok),
            (d, w) -> setAlarmNotificationReminderTime(),
            getString(android.R.string.cancel),
            null,
            getString(R.string.label_default),
            (d, w) -> applyAlarmNotificationReminderTimeInMinutes(DEFAULT_ALARM_NOTIFICATION_REMINDER),
            alertDialog -> {
                mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                mDefaultButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                String inputHoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
                String inputMinutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

                mOkButton.setEnabled(!isInvalidInput(inputHoursText, inputMinutesText));
                mDefaultButton.setEnabled(isNotDefaultNotificationReminderTime(inputHoursText, inputMinutesText));
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        mEditHours.requestFocus();
        mEditHours.postDelayed(() -> {
            if (mInput != null) {
                mInput.showSoftInput(mEditHours, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
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
     * Updates the enabled state and helper text of the input fields.
     */
    private void updateInputSate() {
        mHoursInputLayout.setTypeface(mTypeFace);
        mMinutesInputLayout.setTypeface(mTypeFace);

        mHoursInputLayout.setHelperText(getString(R.string.alarm_hours_warning_box_text));
        mMinutesInputLayout.setHelperText(getString(R.string.alarm_minutes_warning_box_text));

        TextView hoursHelper = mHoursInputLayout.findViewById(com.google.android.material.R.id.textinput_helper_text);
        hoursHelper.setTypeface(mTypeFace);

        TextView minutesHelper = mMinutesInputLayout.findViewById(com.google.android.material.R.id.textinput_helper_text);
        minutesHelper.setTypeface(mTypeFace);

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

    /**
     * Set the alarm notification reminder time in minutes.
     */
    private void setAlarmNotificationReminderTime() {
        int hours = 0;
        int minutes = 0;
        int notificationReminderTimeInMinutes;

        String hoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
        String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

        if (!hoursText.isEmpty()) {
            hours = Integer.parseInt(hoursText);
        }

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        notificationReminderTimeInMinutes = hours * 60 + minutes;

        if (notificationReminderTimeInMinutes == 0) {
            notificationReminderTimeInMinutes = 1;
        }

        applyAlarmNotificationReminderTimeInMinutes(notificationReminderTimeInMinutes);
    }

    /**
     * Apply the notification reminder time in minutes.
     */
    private void applyAlarmNotificationReminderTimeInMinutes(int notificationReminderTimeInMinutes) {
        Bundle result = new Bundle();
        result.putInt(ALARM_NOTIFICATION_REMINDER_VALUE, notificationReminderTimeInMinutes);
        result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
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
        AlertDialog alertDialog = (AlertDialog) requireDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            titleText.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(
                mContext, R.drawable.ic_error), null, null, null);
            titleText.setCompoundDrawablePadding((int) dpToPx(18, getResources().getDisplayMetrics()));
            titleText.setText(getString(R.string.timer_time_warning_box_title));
        }

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
     * The dialog default button is enabled if the typed value is not the default value.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForValidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            titleText.setCompoundDrawables(null, null, null, null);
            titleText.setText(getString(R.string.alarm_notification_reminder_title));
        }

        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mHoursInputLayout.setBoxStrokeColor(validColor);
        mHoursInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));

        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }

        if (mDefaultButton != null) {
            String hoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            mDefaultButton.setEnabled(isNotDefaultNotificationReminderTime(hoursText, minutesText));
        }
    }

    /**
     * @return {@code true} if the alarm notification reminder time is not the default value; {@code false} otherwise.
     */
    private boolean isNotDefaultNotificationReminderTime(String hoursText, String minutesText) {
        int hours = hoursText.isEmpty() ? 0 : Integer.parseInt(hoursText);
        int minutes = minutesText.isEmpty() ? 0 : Integer.parseInt(minutesText);

        int notificationReminderTime = hours * 60 + minutes;

        return notificationReminderTime != DEFAULT_ALARM_NOTIFICATION_REMINDER;
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     *
     * <p>Note: In the hours field, if the hours are equal to 24, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.</p>
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
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

                if (!"0".equals(minutesText)) {
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
     * Handles completing the new alarm notification reminder time from the IME keyboard.
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
                    setAlarmNotificationReminderTime();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

}

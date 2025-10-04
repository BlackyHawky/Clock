// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_AUTO_SILENCE_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;

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
 * DialogFragment to set the auto silence duration for alarms.
 * <ul>
 *  <li>For alarms, the duration is configured in minutes.</li>
 *  <li>For timers, the duration is configured in minutes and seconds, and stored as total seconds.</li>
 * </ul>
 */
public class AutoSilenceDurationDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of AutoSilenceDurationDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_auto_silence_duration_dialog";

    private static final String AUTO_SILENCE_DURATION = "auto_silence_duration_";
    private static final String ARG_PREF_KEY = AUTO_SILENCE_DURATION + "arg_pref_key";
    private static final String ARG_EDIT_AUTO_SILENCE_MINUTES = AUTO_SILENCE_DURATION + "arg_edit_auto_silence_minutes";
    private static final String ARG_EDIT_AUTO_SILENCE_SECONDS = AUTO_SILENCE_DURATION + "arg_edit_auto_silence_seconds";
    private static final String ARG_END_OF_RINGTONE = AUTO_SILENCE_DURATION + "arg_end_of_ringtone";
    private static final String ARG_NEVER = AUTO_SILENCE_DURATION + "arg_never";
    public static final String RESULT_PREF_KEY = AUTO_SILENCE_DURATION + "result_pref_key";
    public static final String REQUEST_KEY = AUTO_SILENCE_DURATION + "request_key";
    public static final String AUTO_SILENCE_DURATION_VALUE = AUTO_SILENCE_DURATION + "value";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_TAG = "arg_tag";

    private Context mContext;
    private Alarm mAlarm;
    private String mTag;
    private TextInputLayout mMinutesInputLayout;
    private TextInputLayout mSecondsInputLayout;
    private TextInputEditText mEditMinutes;
    private TextInputEditText mEditSeconds;
    private MaterialCheckBox mEndOfRingtoneCheckbox;
    private MaterialCheckBox mNeverCheckbox;
    private Button mOkButton;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;
    private boolean isUpdatingCheckboxes = false;

    /**
     * Creates a new instance of {@link AutoSilenceDurationDialogFragment} for use
     * in the settings screen, where the auto silence duration is configured independently
     * of a specific alarm or for timers.
     *
     * @param key              The shared preference key used to identify the setting.
     * @param totalDuration    The auto silence duration, in seconds for timers, or in minutes for alarms.
     * @param isEndOfRingtone  {@code true} if the auto silence duration should correspond
     *                         to the end of the ringtone playback rather than a fixed time.
     * @param isNever          {@code true} if the alarm or timer should never be automatically silenced.
     *                         This overrides the duration value if set.
     */
    public static AutoSilenceDurationDialogFragment newInstance(String key, int totalDuration,
                                                                boolean isEndOfRingtone,
                                                                boolean isNever,
                                                                boolean isForTimer) {

        Bundle args = new Bundle();

        int minutes = isForTimer ? totalDuration / 60 : totalDuration;
        int seconds = totalDuration % 60;

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_AUTO_SILENCE_MINUTES, minutes);
        if (isForTimer) {
            args.putInt(ARG_EDIT_AUTO_SILENCE_SECONDS, seconds);
        } else {
            args.putInt(ARG_EDIT_AUTO_SILENCE_SECONDS, 0);
        }
        args.putBoolean(ARG_END_OF_RINGTONE, isEndOfRingtone);
        args.putBoolean(ARG_NEVER, isNever);

        AutoSilenceDurationDialogFragment frag = new AutoSilenceDurationDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link AutoSilenceDurationDialogFragment} for use
     * in the expanded alarm view, where the auto silence duration is configured for a specific alarm.
     *
     * @param alarm                The alarm instance being edited.
     * @param autoSilenceDuration  The silence duration in minutes.
     * @param isEndOfRingtone      true if the auto silence duration should correspond
     *                             to the end of the ringtone playback rather than a fixed time.
     * @param isNever              {@code true} if the alarm should never be automatically silenced.
     *                             This overrides the duration value if set.
     * @param tag                  A tag identifying the fragment in the fragment manager.
     */
    public static AutoSilenceDurationDialogFragment newInstance(Alarm alarm, int autoSilenceDuration,
                                                                boolean isEndOfRingtone, boolean isNever,
                                                                String tag) {
        final Bundle args = new Bundle();

        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);
        args.putInt(ARG_EDIT_AUTO_SILENCE_MINUTES, autoSilenceDuration);
        args.putInt(ARG_EDIT_AUTO_SILENCE_SECONDS, 0);
        args.putBoolean(ARG_END_OF_RINGTONE, isEndOfRingtone);
        args.putBoolean(ARG_NEVER, isNever);

        final AutoSilenceDurationDialogFragment fragment = new AutoSilenceDurationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Replaces any existing AutoSilenceDurationDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, AutoSilenceDurationDialogFragment fragment) {
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
        if (mEditMinutes != null && mEditSeconds != null) {
            String minutesStr = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsStr = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

            int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
            int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);

            outState.putInt(ARG_EDIT_AUTO_SILENCE_MINUTES, minutes);
            outState.putInt(ARG_EDIT_AUTO_SILENCE_SECONDS, seconds);
        }

        outState.putBoolean(ARG_END_OF_RINGTONE, mEndOfRingtoneCheckbox.isChecked());
        outState.putBoolean(ARG_NEVER, mNeverCheckbox.isChecked());
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

        int editMinutes = args.getInt(ARG_EDIT_AUTO_SILENCE_MINUTES, 0);
        int editSeconds = args.getInt(ARG_EDIT_AUTO_SILENCE_SECONDS, 0);
        boolean isEndOfRingtone = args.getBoolean(ARG_END_OF_RINGTONE, false);
        boolean isNever = args.getBoolean(ARG_NEVER, false);

        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getInt(ARG_EDIT_AUTO_SILENCE_MINUTES, editMinutes);
            editSeconds = savedInstanceState.getInt(ARG_EDIT_AUTO_SILENCE_SECONDS, editSeconds);
            isEndOfRingtone = savedInstanceState.getBoolean(ARG_END_OF_RINGTONE, isEndOfRingtone);
            isNever = savedInstanceState.getBoolean(ARG_NEVER, isNever);
        }

        mInput = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        View view = getLayoutInflater().inflate(R.layout.alarm_auto_silence_duration_dialog, null);

        mMinutesInputLayout = view.findViewById(R.id.dialog_input_layout_minutes);
        mSecondsInputLayout = view.findViewById(R.id.dialog_input_layout_seconds);
        mEditMinutes = view.findViewById(R.id.edit_minutes);
        mEditSeconds = view.findViewById(R.id.edit_seconds);
        mEndOfRingtoneCheckbox = view.findViewById(R.id.end_of_ringtone);
        mNeverCheckbox = view.findViewById(R.id.auto_silence_never);

        mEndOfRingtoneCheckbox.setChecked(isEndOfRingtone);
        mNeverCheckbox.setChecked(isNever);

        mEditMinutes.setText(String.valueOf(editMinutes));

        updateInputSate();

        if (isForTimer()) {
            mEditSeconds.setText(String.valueOf(editSeconds));

            if (editSeconds == TIMEOUT_END_OF_RINGTONE || editSeconds == TIMEOUT_NEVER) {
                mEditMinutes.setText("");
                mEditSeconds.setText("");
            }
        } else {
            if (editMinutes == TIMEOUT_END_OF_RINGTONE || editMinutes == TIMEOUT_NEVER) {
                mEditMinutes.setText("");
            }
        }

        mEditMinutes.selectAll();
        mEditMinutes.requestFocus();
        mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
        mEditMinutes.addTextChangedListener(mTextWatcher);

        if (isForTimer()) {
            mEditMinutes.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    mEditMinutes.selectAll();
                }
            });
            mEditSeconds.selectAll();
            mEditSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
            mEditSeconds.setOnEditorActionListener(new ImeDoneListener());
            mEditSeconds.addTextChangedListener(mTextWatcher);
            mEditSeconds.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    mEditSeconds.selectAll();
                }
            });
        }

        mEndOfRingtoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;

            if (isChecked && mNeverCheckbox.isChecked()) {
                mNeverCheckbox.setChecked(false);
            }

            updateInputSate();

            maybeRequestMinutesFocus();

            isUpdatingCheckboxes = false;
        });

        mNeverCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;

            if (isChecked && mEndOfRingtoneCheckbox.isChecked()) {
                mEndOfRingtoneCheckbox.setChecked(false);
            }

            updateInputSate();

            maybeRequestMinutesFocus();

            isUpdatingCheckboxes = false;
        });

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.auto_silence_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        setAutoSilenceDuration()
                )
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(dialog -> {
            mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsText = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

            mOkButton.setEnabled(!isInvalidInput(minutesText, secondsText));
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

        if (!mEndOfRingtoneCheckbox.isChecked() && !mNeverCheckbox.isChecked()) {
            mEditMinutes.requestFocus();
            mEditMinutes.postDelayed(() -> {
                if (mInput != null) {
                    mInput.showSoftInput(mEditMinutes, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop callbacks from the IME since there is no view to process them.
        mEditMinutes.setOnEditorActionListener(null);
        mEditMinutes.removeTextChangedListener(mTextWatcher);

        if (isForTimer()) {
            mEditSeconds.setOnEditorActionListener(null);
            mEditSeconds.removeTextChangedListener(mTextWatcher);
        }
    }

    /**
     * Updates the enabled state, visibility, and helper text of the input fields
     * based on the state of the "End of Ringtone" and "Never" checkboxes.
     *
     * <p>If either checkbox is checked, the inputs are disabled and their helper texts are cleared.
     * Otherwise, the inputs are enabled and appropriate helper texts are shown.
     * Additionally, seconds input visibility is toggled depending on whether this dialog is for a timer.</p>
     */
    private void updateInputSate() {
        boolean disable = mEndOfRingtoneCheckbox.isChecked() || mNeverCheckbox.isChecked();

        mMinutesInputLayout.setEnabled(!disable);

        if (disable) {
            mEditMinutes.setText("");
            mEditSeconds.setText("");
        }

        if (isForTimer()) {
            mSecondsInputLayout.setEnabled(!disable);
            mSecondsInputLayout.setVisibility(VISIBLE);
            mSecondsInputLayout.setHelperText(getString(R.string.timer_button_time_seconds_warning_box_text));
        } else {
            mSecondsInputLayout.setVisibility(GONE);
        }

        if (!disable) {
            mMinutesInputLayout.setHelperText(getString(R.string.timer_button_time_minutes_warning_box_text));
        } else {
            mMinutesInputLayout.setHelperText(null);
            mSecondsInputLayout.setHelperText(null);
        }

        if (!disable && isForTimer()) {
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            if ("60".equals(minutesText)) {
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
                mSecondsInputLayout.setEnabled(false);
            } else {
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mSecondsInputLayout.setEnabled(true);
            }
        }

        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    /**
     * Requests focus for the minutes input field and shows the keyboard
     * if neither the "End of Ringtone" nor the "Never" checkboxes are selected.
     *
     * <p>This method ensures that the user can immediately start typing a duration
     * when the dialog is in manual entry mode.</p>
     */
    private void maybeRequestMinutesFocus() {
        if (!mEndOfRingtoneCheckbox.isChecked() && !mNeverCheckbox.isChecked()) {
            mEditMinutes.requestFocus();
            mInput.showSoftInput(mEditMinutes, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Set the auto silence duration.
     */
    private void setAutoSilenceDuration() {
        int minutes = 0;
        int seconds = 0;

        if (mEndOfRingtoneCheckbox.isChecked()) {
            if (isForTimer()) {
                seconds = TIMEOUT_END_OF_RINGTONE;
            } else {
                minutes = TIMEOUT_END_OF_RINGTONE;
            }
        } else if (mNeverCheckbox.isChecked()) {
            if (isForTimer()) {
                seconds = TIMEOUT_NEVER;
            } else {
                minutes = TIMEOUT_NEVER;
            }
        } else {
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsText = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (isForTimer() && !secondsText.isEmpty()) {
                seconds = Integer.parseInt(secondsText);
            }

            if (minutes == 0 && seconds == 0) {
                mNeverCheckbox.setChecked(true);

                if (isForTimer()) {
                    seconds = TIMEOUT_NEVER;
                } else {
                    minutes = TIMEOUT_NEVER;
                }
            }
        }

        int autoSilenceDurationInSeconds = minutes * 60 + seconds;

        if (mAlarm != null) {
            ((AutoSilenceDurationDialogHandler) requireActivity())
                    .onDialogAutoSilenceDurationSet(mAlarm, minutes, mTag);
        } else {
            Bundle result = new Bundle();
            result.putInt(AUTO_SILENCE_DURATION_VALUE, isForTimer()
                    ? autoSilenceDurationInSeconds
                    : minutes);
            result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }
    }

    /**
     * @return {@code true} if:
     * <ul>
     *     <li>minutes are less than 0 or greater than 60</li>
     *     <li>seconds are less than 0 or greater than 59 for timers</li>
     * </ul>
     * {@code false} otherwise.
     */
    private boolean isInvalidInput(String minutesText, String secondsText) {
        int minutes = 0;
        int seconds = 0;

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        if (isForTimer() && !secondsText.isEmpty()) {
            seconds = Integer.parseInt(secondsText);
        }

        boolean minutesInvalid = minutes < 0 || minutes > 60;
        boolean secondsInvalid = isForTimer() && (seconds < 0 || seconds > 59);

        return minutesInvalid || secondsInvalid;
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

        String minutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
        boolean minutesInvalid = (!minutesText.isEmpty() && Integer.parseInt(minutesText) < 0)
                || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 60);
        int invalidColor = ContextCompat.getColor(mContext, R.color.md_theme_error);
        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mMinutesInputLayout.setHintTextColor(minutesInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));

        if (isForTimer()) {
            String secondsText = Objects.requireNonNull(mEditSeconds.getText()).toString();
            boolean secondsInvalid = (!secondsText.isEmpty() && Integer.parseInt(secondsText) < 0)
                    || (!secondsText.isEmpty() && Integer.parseInt(secondsText) > 59);

            mSecondsInputLayout.setBoxStrokeColor(secondsInvalid ? invalidColor : validColor);
            mSecondsInputLayout.setHintTextColor(secondsInvalid
                    ? ColorStateList.valueOf(invalidColor)
                    : ColorStateList.valueOf(validColor));
            mMinutesInputLayout.setEnabled(!secondsInvalid);
            mSecondsInputLayout.setEnabled(!minutesInvalid);
        }

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
        alertDialog.setTitle(getString(R.string.auto_silence_title));

        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(!mEndOfRingtoneCheckbox.isChecked() && !mNeverCheckbox.isChecked());

        if (isForTimer()) {
            mSecondsInputLayout.setBoxStrokeColor(validColor);
            mSecondsInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
            mSecondsInputLayout.setEnabled(!mEndOfRingtoneCheckbox.isChecked() && !mNeverCheckbox.isChecked());
        }

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }
    }

    /**
     * @return {@code true} if the dialog is for timers; {@code false} if it is for alarms.
     */
    private boolean isForTimer() {
        String prefKey = requireArguments().getString(ARG_PREF_KEY);
        return KEY_TIMER_AUTO_SILENCE_DURATION.equals(prefKey);
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mEndOfRingtoneCheckbox.isChecked() || mNeverCheckbox.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsText = isForTimer() && mEditSeconds.getText() != null
                    ? mEditSeconds.getText().toString()
                    : "";

            if (isInvalidInput(minutesText, secondsText)) {
                updateDialogForInvalidInput();
                return;
            }

            updateDialogForValidInput();

            int minutes = 0;

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (minutes == 60) {
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mEditMinutes.setOnEditorActionListener(new ImeDoneListener());

                if (isForTimer()) {
                    mSecondsInputLayout.setEnabled(false);

                    if(!"0".equals(secondsText)) {
                        mEditSeconds.setText("0");
                    }
                }
            } else {
                mEditMinutes.setImeOptions(isForTimer()
                        ? EditorInfo.IME_ACTION_NEXT
                        : EditorInfo.IME_ACTION_DONE);

                if (isForTimer()) {
                    mSecondsInputLayout.setEnabled(true);
                }
            }

            mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
            mInput.restartInput(mEditMinutes);
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    /**
     * Handles completing the new auto silence duration from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputMinutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
                String inputSecondsText = Objects.requireNonNull(mEditSeconds.getText()).toString();
                if (isInvalidInput(inputMinutesText, inputSecondsText)) {
                    updateDialogForInvalidInput();
                } else {
                    setAutoSilenceDuration();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

    public interface AutoSilenceDurationDialogHandler {
        void onDialogAutoSilenceDurationSet(Alarm alarm, int autoSilenceDuration, String tag);
    }
}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;

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
 * DialogFragment to set the volume crescendo duration for alarms and timers.
 */
public class VolumeCrescendoDurationDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of VolumeCrescendoDurationDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_volume_crescendo_duration_dialog";

    private static final String VOLUME_CRESCENDO_DURATION = "volume_crescendo_duration_";
    private static final String ARG_PREF_KEY = VOLUME_CRESCENDO_DURATION + "arg_pref_key";
    private static final String ARG_EDIT_VOLUME_CRESCENDO_MINUTES =
            VOLUME_CRESCENDO_DURATION + "arg_edit_volume_crescendo_minutes";
    private static final String ARG_EDIT_VOLUME_CRESCENDO_SECONDS =
            VOLUME_CRESCENDO_DURATION + "arg_edit_volume_crescendo_seconds";
    private static final String ARG_CRESCENDO_OFF = "arg_crescendo_off";
    public static final String RESULT_PREF_KEY = VOLUME_CRESCENDO_DURATION + "result_pref_key";
    public static final String REQUEST_KEY = VOLUME_CRESCENDO_DURATION + "request_key";
    public static final String VOLUME_CRESCENDO_DURATION_VALUE = VOLUME_CRESCENDO_DURATION + "value";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_TAG = "arg_tag";

    private Context mContext;
    private Alarm mAlarm;
    private String mTag;
    private TextInputLayout mMinutesInputLayout;
    private TextInputLayout mSecondsInputLayout;
    private TextInputEditText mEditMinutes;
    private TextInputEditText mEditSeconds;
    private MaterialCheckBox mOffCheckbox;
    private Button mOkButton;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;
    private boolean isUpdatingCheckboxes = false;

    /**
     * Creates a new instance of {@link VolumeCrescendoDurationDialogFragment} for use
     * in the settings screen, where the crescendo duration is configured independently
     * of a specific alarm.
     *
     * @param key          The shared preference key used to identify the setting.
     * @param totalSeconds The crescendo duration in seconds.
     */
    public static VolumeCrescendoDurationDialogFragment newInstance(String key, int totalSeconds, boolean isOff) {
        Bundle args = new Bundle();

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, minutes);
        args.putInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, seconds);
        args.putBoolean(ARG_CRESCENDO_OFF, isOff);

        VolumeCrescendoDurationDialogFragment frag = new VolumeCrescendoDurationDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link VolumeCrescendoDurationDialogFragment} for use
     * in the expanded alarm view, where the crescendo duration is configured for a specific alarm.
     *
     * @param alarm              The alarm instance being edited.
     * @param crescendoDuration  The crescendo duration in seconds.
     * @param tag                A tag identifying the fragment in the fragment manager.
     */
    public static VolumeCrescendoDurationDialogFragment newInstance(Alarm alarm, int crescendoDuration,
                                                                    boolean isOff, String tag) {

        final Bundle args = new Bundle();

        int minutes = crescendoDuration / 60;
        int seconds = crescendoDuration % 60;

        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);
        args.putInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, minutes);
        args.putInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, seconds);
        args.putBoolean(ARG_CRESCENDO_OFF, isOff);

        final VolumeCrescendoDurationDialogFragment fragment = new VolumeCrescendoDurationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Replaces any existing VolumeCrescendoDurationDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, VolumeCrescendoDurationDialogFragment fragment) {
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

            outState.putInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, minutes);
            outState.putInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, seconds);
        }

        outState.putBoolean(ARG_CRESCENDO_OFF, mOffCheckbox.isChecked());
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

        int editMinutes = args.getInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, 0);
        int editSeconds = args.getInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, 0);
        boolean isOff = args.getBoolean(ARG_CRESCENDO_OFF, true);
        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, editMinutes);
            editSeconds = savedInstanceState.getInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, editSeconds);
            isOff = savedInstanceState.getBoolean(ARG_CRESCENDO_OFF, isOff);
        }

        mInput = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        View view = getLayoutInflater().inflate(R.layout.volume_crescendo_duration_dialog, null);

        mMinutesInputLayout = view.findViewById(R.id.dialog_input_layout_minutes);
        mSecondsInputLayout = view.findViewById(R.id.dialog_input_layout_seconds);
        mEditMinutes = view.findViewById(R.id.edit_minutes);
        mEditSeconds = view.findViewById(R.id.edit_seconds);
        mOffCheckbox = view.findViewById(R.id.crescendo_off);

        mOffCheckbox.setChecked(isOff);

        mEditMinutes.setText(String.valueOf(editMinutes));

        updateInputSate();

        mEditMinutes.selectAll();
        mEditMinutes.requestFocus();
        mEditMinutes.addTextChangedListener(mTextWatcher);
        mEditMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditMinutes.selectAll();
            }
        });

        if (editSeconds == DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION) {
            mEditSeconds.setText("");
        } else {
            mEditSeconds.setText(String.valueOf(editSeconds));
        }
        mEditSeconds.selectAll();
        mEditSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditSeconds.setOnEditorActionListener(new ImeDoneListener());
        mEditSeconds.addTextChangedListener(mTextWatcher);
        mEditSeconds.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditSeconds.selectAll();
            }
        });

        mOffCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;

            updateInputSate();

            maybeRequestHoursFocus();

            isUpdatingCheckboxes = false;
        });

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(R.string.crescendo_duration_title))
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        setVolumeCrescendoDuration())
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

        if (!mOffCheckbox.isChecked()) {
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

        mEditSeconds.setOnEditorActionListener(null);
        mEditSeconds.removeTextChangedListener(mTextWatcher);
    }

    /**
     * Updates the enabled state and helper text of the input fields based on the state
     * of the "Off" checkbox.
     *
     * <p>If the checkbox is checked, the inputs are disabled and their helper texts are cleared.
     * Otherwise, the inputs are enabled and appropriate helper texts are shown.</p>
     */
    private void updateInputSate() {
        boolean disable = mOffCheckbox.isChecked();

        mMinutesInputLayout.setEnabled(!disable);
        mSecondsInputLayout.setEnabled(!disable);

        if (disable) {
            mMinutesInputLayout.setHelperText(null);
            mSecondsInputLayout.setHelperText(null);
            mEditMinutes.setText("");
            mEditSeconds.setText("");
        } else {
            mMinutesInputLayout.setHelperText(getString(R.string.timer_button_time_minutes_warning_box_text));
            mSecondsInputLayout.setHelperText(getString(R.string.timer_button_time_seconds_warning_box_text));

            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            if ("60".equals(minutesText)) {
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
                mSecondsInputLayout.setEnabled(false);
            } else {
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mSecondsInputLayout.setEnabled(true);
            }

            mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
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
        if (!mOffCheckbox.isChecked()) {
            mEditMinutes.requestFocus();
            mInput.showSoftInput(mEditMinutes, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Set the volume crescendo duration.
     */
    private void setVolumeCrescendoDuration() {
        int minutes = 0;
        int seconds = 0;
        int crescendoDuration;

        if (mOffCheckbox.isChecked()) {
            crescendoDuration = DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;
        } else {
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsText = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (!secondsText.isEmpty()) {
                seconds = Integer.parseInt(secondsText);
            }

            if (minutes == 0 && seconds == 0) {
                mOffCheckbox.setChecked(true);
                crescendoDuration = DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;
            } else {
                crescendoDuration = minutes * 60 + seconds;
            }
        }

        if (mAlarm != null) {
            ((VolumeCrescendoDurationDialogHandler) requireActivity())
                    .onDialogCrescendoDurationSet(mAlarm, crescendoDuration, mTag);
        } else {
            Bundle result = new Bundle();
            result.putInt(VOLUME_CRESCENDO_DURATION_VALUE, crescendoDuration);
            result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }
    }

    /**
     * @return {@code true} if:
     * <ul>
     *     <li>minutes are less than 0 or greater than 60</li>
     *     <li>seconds are less than 0 or greater than 59</li>
     * </ul>
     * {@code false} otherwise.
     */
    private boolean isInvalidInput(String minutesText, String secondsText) {
        int minutes = 0;
        int seconds = 0;

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        if (!secondsText.isEmpty()) {
            seconds = Integer.parseInt(secondsText);
        }

        return minutes < 0 || minutes > 60 || seconds < 0 || seconds > 59;
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
        String secondsText = Objects.requireNonNull(mEditSeconds.getText()).toString();
        boolean minutesInvalid = (!minutesText.isEmpty() && Integer.parseInt(minutesText) < 0)
                || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 60);
        boolean secondsInvalid = (!secondsText.isEmpty() && Integer.parseInt(secondsText) < 0)
                || (!secondsText.isEmpty() && Integer.parseInt(secondsText) > 59);
        int invalidColor = ContextCompat.getColor(mContext, R.color.md_theme_error);
        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mMinutesInputLayout.setHintTextColor(minutesInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(!secondsInvalid);

        mSecondsInputLayout.setBoxStrokeColor(secondsInvalid ? invalidColor : validColor);
        mSecondsInputLayout.setHintTextColor(secondsInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
        mSecondsInputLayout.setEnabled(!minutesInvalid);

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
        alertDialog.setTitle(getString(R.string.crescendo_duration_title));

        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(!mOffCheckbox.isChecked());

        mSecondsInputLayout.setBoxStrokeColor(validColor);
        mSecondsInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mSecondsInputLayout.setEnabled(!mOffCheckbox.isChecked());

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
            if (mOffCheckbox.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsText = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

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
                mSecondsInputLayout.setEnabled(false);

                if(!"0".equals(secondsText)) {
                    mEditSeconds.setText("0");
                }
            } else {
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mSecondsInputLayout.setEnabled(true);
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
     * Handles completing the new alarm snooze duration from the IME keyboard.
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
                    setVolumeCrescendoDuration();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

    public interface VolumeCrescendoDurationDialogHandler {
        void onDialogCrescendoDurationSet(Alarm alarm, int crescendoDuration, String tag);
    }

}

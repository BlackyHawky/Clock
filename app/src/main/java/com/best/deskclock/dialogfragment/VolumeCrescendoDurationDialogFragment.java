// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VOLUME_CRESCENDO_DURATION;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.VolumeCrescendoDurationDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

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

    private VolumeCrescendoDurationDialogBinding mBinding;

    private String mPrefKey;
    private Button mOkButton;
    private Button mDefaultButton;
    private Typeface mTypeFace;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;
    private boolean isUpdatingCheckboxes = false;

    /**
     * Creates a new instance of {@link VolumeCrescendoDurationDialogFragment} for use
     * in the settings screen, where the crescendo duration is configured independently
     * of a specific alarm.
     *
     * @param key               The shared preference key used to identify the setting.
     * @param crescendoDuration The crescendo duration in seconds.
     */
    public static VolumeCrescendoDurationDialogFragment newInstance(String key, int crescendoDuration) {
        Bundle args = new Bundle();

        boolean isOff = crescendoDuration == DEFAULT_VOLUME_CRESCENDO_DURATION;

        int minutes = 0;
        int seconds = 0;

        if (!isOff) {
            minutes = crescendoDuration / 60;
            seconds = crescendoDuration % 60;
        }

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
     * in the alarm editing panel, where the crescendo duration is configured for a specific alarm.
     *
     * @param crescendoDuration The crescendo duration in seconds.
     */
    public static VolumeCrescendoDurationDialogFragment newInstance(int crescendoDuration) {

        final Bundle args = new Bundle();

        boolean isOff = crescendoDuration == DEFAULT_VOLUME_CRESCENDO_DURATION;

        int minutes = 0;
        int seconds = 0;

        if (!isOff) {
            minutes = crescendoDuration / 60;
            seconds = crescendoDuration % 60;
        }

        args.putInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, minutes);
        args.putInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, seconds);
        args.putBoolean(ARG_CRESCENDO_OFF, isOff);

        final VolumeCrescendoDurationDialogFragment fragment = new VolumeCrescendoDurationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link VolumeCrescendoDurationDialogFragment}.
     */
    public static void show(FragmentManager manager, VolumeCrescendoDurationDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        String minutesStr = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";
        String secondsStr = mBinding.editSeconds.getText() != null ? mBinding.editSeconds.getText().toString() : "";

        int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
        int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);

        outState.putInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, minutes);
        outState.putInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, seconds);

        outState.putBoolean(ARG_CRESCENDO_OFF, mBinding.crescendoOffButton.isChecked());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        mTypeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();

        mPrefKey = args.getString(ARG_PREF_KEY, null);
        int editMinutes = args.getInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, 0);
        int editSeconds = args.getInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, 0);
        boolean isOff = args.getBoolean(ARG_CRESCENDO_OFF, true);
        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getInt(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, editMinutes);
            editSeconds = savedInstanceState.getInt(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, editSeconds);
            isOff = savedInstanceState.getBoolean(ARG_CRESCENDO_OFF, isOff);
        }

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        mBinding = VolumeCrescendoDurationDialogBinding.inflate(getLayoutInflater());

        mBinding.crescendoOffButton.setTypeface(mTypeFace);
        mBinding.crescendoOffButton.setChecked(isOff);

        mBinding.editMinutes.setText(String.valueOf(editMinutes));
        mBinding.editMinutes.setTypeface(mTypeFace);

        updateInputSate();

        mBinding.editMinutes.selectAll();
        mBinding.editMinutes.requestFocus();
        mBinding.editMinutes.addTextChangedListener(mTextWatcher);
        mBinding.editMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editMinutes.selectAll();
            }
        });

        if (!isOff) {
            mBinding.editSeconds.setText(String.valueOf(editSeconds));
        }
        mBinding.editSeconds.setTypeface(mTypeFace);
        mBinding.editSeconds.selectAll();
        mBinding.editSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editSeconds.setOnEditorActionListener(new ImeDoneListener());
        mBinding.editSeconds.addTextChangedListener(mTextWatcher);
        mBinding.editSeconds.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editSeconds.selectAll();
            }
        });

        mBinding.crescendoOffButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;

            updateInputSate();

            maybeRequestHoursFocus();

            isUpdatingCheckboxes = false;
        });

        return CustomDialog.create(
            requireContext(),
            null,
            mPrefKey != null ? null : AppCompatResources.getDrawable(requireContext(), R.drawable.ic_crescendo),
            getString(R.string.crescendo_duration_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> setVolumeCrescendoDurationInSeconds(),
            getString(android.R.string.cancel),
            null,
            getString(R.string.label_default),
            (d, w) -> applyVolumeCrescendoDurationInSeconds(DEFAULT_VOLUME_CRESCENDO_DURATION),
            alertDialog -> {
                mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                mDefaultButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                String inputMinutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";
                String inputSecondsText = mBinding.editSeconds.getText() != null ? mBinding.editSeconds.getText().toString() : "";

                mOkButton.setEnabled(!isInvalidInput(inputMinutesText, inputSecondsText));
                mDefaultButton.setEnabled(isNotDefaultVolumeCrescendoDuration(inputMinutesText, inputSecondsText));
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBinding.crescendoOffButton.isChecked()) {
            mBinding.editMinutes.requestFocus();
            mBinding.editMinutes.postDelayed(() -> {
                if (mInput != null) {
                    mInput.showSoftInput(mBinding.editMinutes, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
        }
    }

    @Override
    public void onDestroyView() {
        // Stop callbacks from the IME since there is no view to process them.
        mBinding.editMinutes.setOnEditorActionListener(null);
        mBinding.editMinutes.removeTextChangedListener(mTextWatcher);
        mBinding.editMinutes.setOnFocusChangeListener(null); // Ne pas oublier le focus !

        mBinding.editSeconds.setOnEditorActionListener(null);
        mBinding.editSeconds.removeTextChangedListener(mTextWatcher);
        mBinding.editSeconds.setOnFocusChangeListener(null);

        mBinding.crescendoOffButton.setOnCheckedChangeListener(null);

        mInput = null;

        mBinding = null;

        mOkButton = null;
        mDefaultButton = null;

        mTypeFace = null;

        super.onDestroyView();
    }

    /**
     * Updates the enabled state and helper text of the input fields based on the state
     * of the "Off" checkbox.
     *
     * <p>If the checkbox is checked, the inputs are disabled and their helper texts are cleared.
     * Otherwise, the inputs are enabled and appropriate helper texts are shown.</p>
     */
    private void updateInputSate() {
        boolean disable = mBinding.crescendoOffButton.isChecked();

        mBinding.textInputLayoutMinutes.setTypeface(mTypeFace);
        mBinding.textInputLayoutSeconds.setTypeface(mTypeFace);

        mBinding.textInputLayoutMinutes.setEnabled(!disable);
        mBinding.textInputLayoutSeconds.setEnabled(!disable);

        if (disable) {
            mBinding.textInputLayoutMinutes.setHelperText(null);
            mBinding.textInputLayoutSeconds.setHelperText(null);
            mBinding.editMinutes.setText("");
            mBinding.editSeconds.setText("");
        } else {
            mBinding.textInputLayoutMinutes.setHelperText(getString(R.string.timer_button_time_minutes_warning_box_text));
            mBinding.textInputLayoutSeconds.setHelperText(getString(R.string.timer_button_time_seconds_warning_box_text));

            TextView minutesHelper = mBinding.textInputLayoutMinutes.findViewById(com.google.android.material.R.id.textinput_helper_text);
            minutesHelper.setTypeface(mTypeFace);

            TextView secondsHelper = mBinding.textInputLayoutSeconds.findViewById(com.google.android.material.R.id.textinput_helper_text);
            secondsHelper.setTypeface(mTypeFace);

            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

            if ("60".equals(minutesText)) {
                mBinding.editMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mBinding.editMinutes.setOnEditorActionListener(new ImeDoneListener());
                mBinding.textInputLayoutSeconds.setEnabled(false);
            } else {
                mBinding.editMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mBinding.textInputLayoutSeconds.setEnabled(true);
            }

            mBinding.editMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
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
        if (!mBinding.crescendoOffButton.isChecked()) {
            mBinding.editMinutes.requestFocus();
            mInput.showSoftInput(mBinding.editMinutes, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Set the volume crescendo duration in seconds for alarms or timers.
     */
    private void setVolumeCrescendoDurationInSeconds() {
        int minutes = 0;
        int seconds = 0;
        int crescendoDurationInSeconds;

        if (mBinding.crescendoOffButton.isChecked()) {
            crescendoDurationInSeconds = DEFAULT_VOLUME_CRESCENDO_DURATION;
        } else {
            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";
            String secondsText = mBinding.editSeconds.getText() != null ? mBinding.editSeconds.getText().toString() : "";

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (!secondsText.isEmpty()) {
                seconds = Integer.parseInt(secondsText);
            }

            if (minutes == 0 && seconds == 0) {
                mBinding.crescendoOffButton.setChecked(true);
                crescendoDurationInSeconds = DEFAULT_VOLUME_CRESCENDO_DURATION;
            } else {
                crescendoDurationInSeconds = minutes * 60 + seconds;
            }
        }

        applyVolumeCrescendoDurationInSeconds(crescendoDurationInSeconds);
    }

    /**
     * Apply the volume crescendo duration in seconds for alarms or timers.
     */
    private void applyVolumeCrescendoDurationInSeconds(int crescendoDurationInSeconds) {
        Bundle result = new Bundle();
        result.putInt(VOLUME_CRESCENDO_DURATION_VALUE, crescendoDurationInSeconds);

        if (mPrefKey != null) {
            result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
        }

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
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
        AlertDialog alertDialog = (AlertDialog) requireDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            titleText.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(
                requireContext(), R.drawable.ic_error), null, null, null);
            if (mPrefKey != null) {
                titleText.setCompoundDrawablePadding((int) dpToPx(18, getResources().getDisplayMetrics()));
            }
            titleText.setText(getString(R.string.timer_time_warning_box_title));
        }

        String minutesText = Objects.requireNonNull(mBinding.editMinutes.getText()).toString();
        String secondsText = Objects.requireNonNull(mBinding.editSeconds.getText()).toString();
        boolean minutesInvalid = (!minutesText.isEmpty() && Integer.parseInt(minutesText) < 0)
            || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 60);
        boolean secondsInvalid = (!secondsText.isEmpty() && Integer.parseInt(secondsText) < 0)
            || (!secondsText.isEmpty() && Integer.parseInt(secondsText) > 59);
        int invalidColor = ContextCompat.getColor(requireContext(), R.color.md_theme_error);
        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mBinding.textInputLayoutMinutes.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mBinding.textInputLayoutMinutes.setHintTextColor(minutesInvalid
            ? ColorStateList.valueOf(invalidColor)
            : ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutMinutes.setEnabled(!secondsInvalid);

        mBinding.textInputLayoutSeconds.setBoxStrokeColor(secondsInvalid ? invalidColor : validColor);
        mBinding.textInputLayoutSeconds.setHintTextColor(secondsInvalid
            ? ColorStateList.valueOf(invalidColor)
            : ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutSeconds.setEnabled(!minutesInvalid);

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
                titleText.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(
                    requireContext(), R.drawable.ic_crescendo), null, null, null);
            }
            titleText.setText(getString(R.string.crescendo_duration_title));
        }

        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mBinding.textInputLayoutMinutes.setBoxStrokeColor(validColor);
        mBinding.textInputLayoutMinutes.setHintTextColor(ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutMinutes.setEnabled(!mBinding.crescendoOffButton.isChecked());

        mBinding.textInputLayoutSeconds.setBoxStrokeColor(validColor);
        mBinding.textInputLayoutSeconds.setHintTextColor(ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutSeconds.setEnabled(!mBinding.crescendoOffButton.isChecked());

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }

        if (mDefaultButton != null) {
            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";
            String secondsText = mBinding.editSeconds.getText() != null ? mBinding.editSeconds.getText().toString() : "";
            mDefaultButton.setEnabled(isNotDefaultVolumeCrescendoDuration(minutesText, secondsText));
        }
    }

    /**
     * @return {@code true} if the alarm volume crescendo duration or the timer volume crescendo
     * duration is not the default value; {@code false} otherwise.
     */
    private boolean isNotDefaultVolumeCrescendoDuration(String minutesText, String secondsText) {
        int minutes = minutesText.isEmpty() ? 0 : Integer.parseInt(minutesText);
        int seconds = secondsText.isEmpty() ? 0 : Integer.parseInt(secondsText);

        int crescendoDuration = minutes * 60 + seconds;

        return crescendoDuration != DEFAULT_VOLUME_CRESCENDO_DURATION;
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the hours field, if the hours are equal to 24, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mBinding.crescendoOffButton.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";
            String secondsText = mBinding.editSeconds.getText() != null ? mBinding.editSeconds.getText().toString() : "";

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
                mBinding.editMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mBinding.editMinutes.setOnEditorActionListener(new ImeDoneListener());
                mBinding.textInputLayoutSeconds.setEnabled(false);

                if (!"0".equals(secondsText)) {
                    mBinding.editSeconds.setText("0");
                }
            } else {
                mBinding.editMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mBinding.textInputLayoutSeconds.setEnabled(true);
            }

            mBinding.editMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
            mInput.restartInput(mBinding.editMinutes);
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
                String inputMinutesText = Objects.requireNonNull(mBinding.editMinutes.getText()).toString();
                String inputSecondsText = Objects.requireNonNull(mBinding.editSeconds.getText()).toString();
                if (isInvalidInput(inputMinutesText, inputSecondsText)) {
                    updateDialogForInvalidInput();
                } else {
                    setVolumeCrescendoDurationInSeconds();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

}

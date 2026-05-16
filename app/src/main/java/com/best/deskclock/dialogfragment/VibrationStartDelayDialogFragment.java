// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_START_DELAY;

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
import com.best.deskclock.databinding.VibrationStartDelayDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/**
 * DialogFragment to set a new vibration start delay.
 */
public class VibrationStartDelayDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of VibrationStartDelayDialogFragment in the fragment manager.
     */
    private static final String TAG = "vibration_start_delay_dialog";

    private static final String VIBRATION_START_DELAY = "vibration_start_delay_";
    private static final String ARG_PREF_KEY = VIBRATION_START_DELAY + "arg_pref_key";
    private static final String ARG_EDIT_MINUTES = VIBRATION_START_DELAY + "arg_edit_minutes";
    private static final String ARG_VIBRATION_DELAY_NONE = "arg_vibration_delay_none";
    public static final String RESULT_PREF_KEY = VIBRATION_START_DELAY + "result_pref_key";
    public static final String REQUEST_KEY = VIBRATION_START_DELAY + "request_key";
    public static final String VIBRATION_DELAY_VALUE = VIBRATION_START_DELAY + "value";

    private VibrationStartDelayDialogBinding mBinding;

    private Button mOkButton;
    private Button mDefaultButton;
    private Typeface mTypeFace;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;
    private boolean isUpdatingCheckboxes = false;

    /**
     * Creates a new instance of {@link VibrationStartDelayDialogFragment} for use
     * in the settings screen, where the crescendo duration is configured independently
     * of a specific alarm.
     *
     * @param key          The shared preference key used to identify the setting.
     * @param totalSeconds The crescendo duration in seconds.
     */
    public static VibrationStartDelayDialogFragment newInstance(String key, int totalSeconds, boolean isNone) {
        Bundle args = new Bundle();

        int minutes = totalSeconds / 60;

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_MINUTES, minutes);
        args.putBoolean(ARG_VIBRATION_DELAY_NONE, isNone);

        VibrationStartDelayDialogFragment frag = new VibrationStartDelayDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Displays {@link VibrationStartDelayDialogFragment}.
     */
    public static void show(FragmentManager manager, VibrationStartDelayDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        String minutesStr = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";
        int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);

        outState.putInt(ARG_EDIT_MINUTES, minutes);
        outState.putBoolean(ARG_VIBRATION_DELAY_NONE, mBinding.vibrationStartDelayNoneCheckbox.isChecked());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        mTypeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();
        int editMinutes = args.getInt(ARG_EDIT_MINUTES, 0);
        boolean isNone = args.getBoolean(ARG_VIBRATION_DELAY_NONE, true);
        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getInt(ARG_EDIT_MINUTES, editMinutes);
            isNone = savedInstanceState.getBoolean(ARG_VIBRATION_DELAY_NONE, isNone);
        }

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        mBinding = VibrationStartDelayDialogBinding.inflate(getLayoutInflater());

        mBinding.vibrationStartDelayNoneCheckbox.setTypeface(mTypeFace);
        mBinding.vibrationStartDelayNoneCheckbox.setChecked(isNone);

        if (editMinutes == DEFAULT_VIBRATION_START_DELAY) {
            mBinding.editMinutes.setText("");
        } else {
            mBinding.editMinutes.setText(String.valueOf(editMinutes));
        }
        mBinding.editMinutes.setTypeface(mTypeFace);

        updateInputSate();

        mBinding.editMinutes.selectAll();
        mBinding.editMinutes.requestFocus();
        mBinding.editMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mBinding.editMinutes.setOnEditorActionListener(new ImeDoneListener());
        mBinding.editMinutes.addTextChangedListener(mTextWatcher);

        mBinding.vibrationStartDelayNoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;

            updateInputSate();

            maybeRequestMinutesFocus();

            isUpdatingCheckboxes = false;
        });

        return CustomDialog.create(
            requireContext(),
            null,
            null,
            getString(R.string.vibration_start_delay_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> setVibrationStartDelayInSeconds(),
            getString(android.R.string.cancel),
            null,
            getString(R.string.label_default),
            (d, w) -> applyVibrationStartDelayInSeconds(DEFAULT_VIBRATION_START_DELAY),
            alertDialog -> {
                mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                mDefaultButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

                mOkButton.setEnabled(!isInvalidInput(minutesText));
                mDefaultButton.setEnabled(isNotDefaultVibrationStartDelay(minutesText));
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBinding.vibrationStartDelayNoneCheckbox.isChecked()) {
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

        super.onDestroyView();

        mInput = null;

        mBinding = null;

        mOkButton = null;
        mDefaultButton = null;

        mTypeFace = null;
    }

    /**
     * Updates the enabled state and helper text of the input fields based on the state
     * of the "None" checkbox.
     *
     * <p>If the checkbox is checked, the inputs are disabled and their helper texts are cleared.
     * Otherwise, the inputs are enabled and appropriate helper texts are shown.</p>
     */
    private void updateInputSate() {
        boolean disable = mBinding.vibrationStartDelayNoneCheckbox.isChecked();

        mBinding.textInputLayoutMinutes.setTypeface(mTypeFace);
        mBinding.textInputLayoutMinutes.setEnabled(!disable);

        if (disable) {
            mBinding.textInputLayoutMinutes.setHelperText(null);
            mBinding.editMinutes.setText("");
        } else {
            mBinding.textInputLayoutMinutes.setHelperText(getString(R.string.vibration_start_delay_warning_box_text));
            TextView minuteHelper = mBinding.textInputLayoutMinutes.findViewById(com.google.android.material.R.id.textinput_helper_text);
            minuteHelper.setTypeface(mTypeFace);
        }
    }

    /**
     * Requests focus for the minutes input field and shows the keyboard
     * if the "None" checkbox is not selected.
     *
     * <p>This method ensures that the user can immediately start typing a duration
     * when the dialog is in manual entry mode.</p>
     */
    private void maybeRequestMinutesFocus() {
        if (!mBinding.vibrationStartDelayNoneCheckbox.isChecked()) {
            mBinding.editMinutes.requestFocus();
            mInput.showSoftInput(mBinding.editMinutes, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Set the vibration start delay in seconds for alarms.
     */
    private void setVibrationStartDelayInSeconds() {
        int minutes = 0;
        int vibrationStartDelayInSeconds;

        if (mBinding.vibrationStartDelayNoneCheckbox.isChecked()) {
            vibrationStartDelayInSeconds = DEFAULT_VIBRATION_START_DELAY;
        } else {
            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (minutes == 0) {
                mBinding.vibrationStartDelayNoneCheckbox.setChecked(true);
                vibrationStartDelayInSeconds = DEFAULT_VIBRATION_START_DELAY;
            } else {
                vibrationStartDelayInSeconds = minutes * 60;
            }
        }

        applyVibrationStartDelayInSeconds(vibrationStartDelayInSeconds);
    }

    /**
     * Apply the vibration start delay in seconds for alarms.
     */
    private void applyVibrationStartDelayInSeconds(int vibrationStartDelayInSeconds) {
        Bundle result = new Bundle();
        result.putInt(VIBRATION_DELAY_VALUE, vibrationStartDelayInSeconds);
        result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    /**
     * @return {@code true} if minutes are less than 0 or greater than 10; {@code false} otherwise.
     */
    private boolean isInvalidInput(String minutesText) {
        int minutes = 0;

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        return minutes < 0 || minutes > 10;
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
            titleText.setCompoundDrawablePadding((int) dpToPx(18, getResources().getDisplayMetrics()));
            titleText.setText(getString(R.string.timer_time_warning_box_title));
        }

        String minutesText = Objects.requireNonNull(mBinding.editMinutes.getText()).toString();
        boolean minutesInvalid = (!minutesText.isEmpty() && Integer.parseInt(minutesText) < 0)
            || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 10);
        int invalidColor = ContextCompat.getColor(requireContext(), R.color.md_theme_error);
        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mBinding.textInputLayoutMinutes.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mBinding.textInputLayoutMinutes.setHintTextColor(minutesInvalid
            ? ColorStateList.valueOf(invalidColor)
            : ColorStateList.valueOf(validColor));

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
            titleText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            titleText.setText(getString(R.string.vibration_start_delay_title));
        }

        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mBinding.textInputLayoutMinutes.setBoxStrokeColor(validColor);
        mBinding.textInputLayoutMinutes.setHintTextColor(ColorStateList.valueOf(validColor));
        mBinding.textInputLayoutMinutes.setEnabled(!mBinding.vibrationStartDelayNoneCheckbox.isChecked());

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }

        if (mDefaultButton != null) {
            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";
            mDefaultButton.setEnabled(isNotDefaultVibrationStartDelay(minutesText));
        }
    }

    /**
     * @return {@code true} if the alarm vibration start delay is not the default value;
     * {@code false} otherwise.
     */
    private boolean isNotDefaultVibrationStartDelay(String minutesText) {
        int minutes = minutesText.isEmpty() ? 0 : Integer.parseInt(minutesText);

        int vibrationStartDelay = minutes * 60;

        return vibrationStartDelay != DEFAULT_VIBRATION_START_DELAY;
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the hours field, if the minutes are equal to 10, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mBinding.vibrationStartDelayNoneCheckbox.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String minutesText = mBinding.editMinutes.getText() != null ? mBinding.editMinutes.getText().toString() : "";

            if (isInvalidInput(minutesText)) {
                updateDialogForInvalidInput();
                return;
            }

            updateDialogForValidInput();
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    /**
     * Handles completing the new vibration start delay from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputMinutesText = Objects.requireNonNull(mBinding.editMinutes.getText()).toString();
                if (isInvalidInput(inputMinutesText)) {
                    updateDialogForInvalidInput();
                } else {
                    setVibrationStartDelayInSeconds();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }
}

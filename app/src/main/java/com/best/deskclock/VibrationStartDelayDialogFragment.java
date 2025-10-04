// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_START_DELAY;

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

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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

    private Context mContext;
    private TextInputLayout mMinutesInputLayout;
    private TextInputEditText mEditMinutes;
    private MaterialCheckBox mNoneCheckbox;
    private Button mOkButton;
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
     * Replaces any existing {@link VibrationStartDelayDialogFragment} with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, VibrationStartDelayDialogFragment fragment) {
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
        if (mEditMinutes != null) {
            String minutesStr = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);

            outState.putInt(ARG_EDIT_MINUTES, minutes);
        }

        outState.putBoolean(ARG_VIBRATION_DELAY_NONE, mNoneCheckbox.isChecked());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = requireContext();

        final Bundle args = requireArguments();
        int editMinutes = args.getInt(ARG_EDIT_MINUTES, 0);
        boolean isNone = args.getBoolean(ARG_VIBRATION_DELAY_NONE, true);
        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getInt(ARG_EDIT_MINUTES, editMinutes);
            isNone = savedInstanceState.getBoolean(ARG_VIBRATION_DELAY_NONE, isNone);
        }

        mInput = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        View view = getLayoutInflater().inflate(R.layout.vibration_start_delay_dialog, null);

        mMinutesInputLayout = view.findViewById(R.id.dialog_input_layout_minutes);
        mEditMinutes = view.findViewById(R.id.edit_minutes);
        mNoneCheckbox = view.findViewById(R.id.vibration_start_delay_none);

        mNoneCheckbox.setChecked(isNone);

        if (editMinutes == DEFAULT_VIBRATION_START_DELAY) {
            mEditMinutes.setText("");
        } else {
            mEditMinutes.setText(String.valueOf(editMinutes));
        }

        updateInputSate();

        mEditMinutes.selectAll();
        mEditMinutes.requestFocus();
        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
        mEditMinutes.addTextChangedListener(mTextWatcher);

        mNoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingCheckboxes) {
                return;
            }

            isUpdatingCheckboxes = true;

            updateInputSate();

            maybeRequestMinutesFocus();

            isUpdatingCheckboxes = false;
        });

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(R.string.vibration_start_delay_title))
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        setVibrationStartDelay())
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(dialog -> {
            mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            mOkButton.setEnabled(!isInvalidInput(minutesText));
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

        mMinutesInputLayout.setEnabled(!disable);

        if (disable) {
            mMinutesInputLayout.setHelperText(null);
            mEditMinutes.setText("");
        } else {
            mMinutesInputLayout.setHelperText(getString(R.string.vibration_start_delay_warning_box_text));
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
        if (!mNoneCheckbox.isChecked()) {
            mEditMinutes.requestFocus();
            mInput.showSoftInput(mEditMinutes, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Set the vibration start delay.
     */
    private void setVibrationStartDelay() {
        int minutes = 0;
        int vibrationStartDelay;

        if (mNoneCheckbox.isChecked()) {
            vibrationStartDelay = DEFAULT_VIBRATION_START_DELAY;
        } else {
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (minutes == 0) {
                mNoneCheckbox.setChecked(true);
                vibrationStartDelay = DEFAULT_VIBRATION_START_DELAY;
            } else {
                vibrationStartDelay = minutes * 60;
            }
        }

        Bundle result = new Bundle();
        result.putInt(VIBRATION_DELAY_VALUE, vibrationStartDelay);
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
                || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 10);
        int invalidColor = ContextCompat.getColor(mContext, R.color.md_theme_error);
        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mMinutesInputLayout.setHintTextColor(minutesInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));

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
        alertDialog.setTitle(getString(R.string.vibration_start_delay_title));

        int validColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(!mNoneCheckbox.isChecked());

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the hours field, if the minutes are equal to 10, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mNoneCheckbox.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

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
                String inputMinutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
                if (isInvalidInput(inputMinutesText)) {
                    updateDialogForInvalidInput();
                } else {
                    setVibrationStartDelay();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }
}

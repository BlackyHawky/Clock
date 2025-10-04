/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

/**
 * DialogFragment to edit timer add time button.
 */
public class TimerAddTimeButtonDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of TimerAddTimeButtonDialogFragment in the fragment manager.
     */
    private static final String TAG = "add_time_button_dialog";

    private static final String ARG_EDIT_MINUTES = "arg_edit_minutes";
    private static final String ARG_EDIT_SECONDS = "arg_edit_seconds";
    private static final String ARG_TIMER_ID = "arg_timer_id";
    private static final String ARG_PREF_KEY = "arg_pref_key";
    public static final String RESULT_PREF_KEY = "result_pref_key";
    public static final String REQUEST_KEY = "request_key";
    public static final String ADD_TIME_BUTTON_VALUE = "add_time_button_value";

    private TextInputLayout mMinutesInputLayout;
    private TextInputLayout mSecondsInputLayout;
    private EditText mEditMinutes;
    private EditText mEditSeconds;
    private Button mOkButton;
    private int mTimerId;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;

    /**
     * Creates a new instance of {@link TimerAddTimeButtonDialogFragment} to be used
     * in the settings screen for configuring the default time added by the timer button.
     *
     * @param key           The shared preference key used to persist the selected duration.
     * @param totalDuration The default duration in seconds to pre-fill the dialog with.
     *                      This value is split into minutes and seconds internally.
     */
    public static TimerAddTimeButtonDialogFragment newInstance(String key, int totalDuration) {
        final Bundle args = new Bundle();

        int minutesButtonTime = totalDuration / 60;
        int secondsButtonTime = totalDuration % 60;

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_MINUTES, minutesButtonTime);
        args.putInt(ARG_EDIT_SECONDS, secondsButtonTime);

        final TimerAddTimeButtonDialogFragment frag = new TimerAddTimeButtonDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link TimerAddTimeButtonDialogFragment} to be used
     * directly from an existing timer instance, typically when modifying the button time
     * from within the timer UI.
     *
     * @param timer The {@link Timer} instance containing the current button time and ID.
     *              The button time is expected to be stored as a string representing seconds.
     */
    public static TimerAddTimeButtonDialogFragment newInstance(Timer timer) {
        final Bundle args = new Bundle();

        int totalSecondsButtonTime = Integer.parseInt(timer.getButtonTime());
        int minutesButtonTime = totalSecondsButtonTime / 60;
        int secondsButtonTime = totalSecondsButtonTime % 60;

        args.putInt(ARG_EDIT_MINUTES, minutesButtonTime);
        args.putInt(ARG_EDIT_SECONDS, secondsButtonTime);
        args.putInt(ARG_TIMER_ID, timer.getId());

        final TimerAddTimeButtonDialogFragment frag = new TimerAddTimeButtonDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Replaces any existing TimerAddTimeButtonDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, TimerAddTimeButtonDialogFragment fragment) {
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
            outState.putString(ARG_EDIT_MINUTES, Objects.requireNonNull(mEditMinutes.getText()).toString());
            outState.putString(ARG_EDIT_SECONDS, Objects.requireNonNull(mEditSeconds.getText()).toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        mTimerId = args.getInt(ARG_TIMER_ID, -1);

        int editMinutes = args.getInt(ARG_EDIT_MINUTES, 0);
        int editSeconds = args.getInt(ARG_EDIT_SECONDS, 0);
        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getInt(ARG_EDIT_MINUTES, editMinutes);
            editSeconds = savedInstanceState.getInt(ARG_EDIT_SECONDS, editSeconds);
        }

        View view = getLayoutInflater().inflate(R.layout.timer_dialog_edit_add_time, null);

        mMinutesInputLayout = view.findViewById(R.id.dialog_input_layout_minutes);
        mMinutesInputLayout.setHelperText(getString(R.string.timer_button_time_minutes_warning_box_text));

        mSecondsInputLayout = view.findViewById(R.id.dialog_input_layout_seconds);
        mSecondsInputLayout.setHelperText(getString(R.string.timer_button_time_seconds_warning_box_text));

        mEditMinutes = view.findViewById(R.id.edit_minutes);
        mEditSeconds = view.findViewById(R.id.edit_seconds);

        mEditMinutes.setText(String.valueOf(editMinutes));
        if (editMinutes == 60) {
            mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
            mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
            mSecondsInputLayout.setEnabled(false);
        } else {
            mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            mSecondsInputLayout.setEnabled(true);
        }
        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditMinutes.selectAll();
        mEditMinutes.requestFocus();
        mEditMinutes.addTextChangedListener(mTextWatcher);
        mEditMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditMinutes.selectAll();
            }
        });

        mEditSeconds.setText(String.valueOf(editSeconds));
        mEditSeconds.selectAll();
        mEditSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditSeconds.setOnEditorActionListener(new ImeDoneListener());
        mEditSeconds.addTextChangedListener(mTextWatcher);
        mEditSeconds.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditSeconds.selectAll();
            }
        });

        String inputMinutesText = mEditMinutes.getText().toString();
        String inputSecondsText = mEditSeconds.getText().toString();

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        final Drawable drawable = AppCompatResources.getDrawable(requireContext(), isInvalidInput(inputMinutesText, inputSecondsText)
                ? R.drawable.ic_error
                : R.drawable.ic_hourglass_top);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        final MaterialAlertDialogBuilder dialogBuilder =
                new MaterialAlertDialogBuilder(requireContext())
                        .setIcon(drawable)
                        .setTitle(isInvalidInput(inputMinutesText, inputSecondsText)
                                ? getString(R.string.timer_time_warning_box_title)
                                : getString(R.string.timer_button_time_box_title))
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (isInvalidInput(inputMinutesText, inputSecondsText)) {
                                updateDialogForInvalidInput();
                            } else {
                                setAddButtonText();
                                dismiss();
                            }
                        })
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

        mEditMinutes.requestFocus();
        mEditMinutes.postDelayed(() -> {
            if (mInput != null) {
                mInput.showSoftInput(mEditMinutes, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
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
     * Sets the new time into the timer add button.
     */
    private void setAddButtonText() {
        String minutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
        String secondsText = Objects.requireNonNull(mEditSeconds.getText()).toString();

        int minutes = 0;
        int seconds = 0;

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        if (!secondsText.isEmpty()) {
            seconds = Integer.parseInt(secondsText);
        }

        int totalSeconds = minutes * 60 + seconds;

        if (mTimerId >= 0) {
            final Timer timer = DataModel.getDataModel().getTimer(mTimerId);
            if (timer != null) {
                DataModel.getDataModel().setTimerButtonTime(timer, String.valueOf(totalSeconds));
            }
        } else {
            Bundle result = new Bundle();
            result.putInt(ADD_TIME_BUTTON_VALUE, totalSeconds);
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
        final Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
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
        int invalidColor = ContextCompat.getColor(requireContext(), R.color.md_theme_error);
        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

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
        final Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_hourglass_top);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        AlertDialog alertDialog = (AlertDialog) requireDialog();
        alertDialog.setIcon(drawable);
        alertDialog.setTitle(getString(R.string.timer_button_time_box_title));

        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(true);

        mSecondsInputLayout.setBoxStrokeColor(validColor);
        mSecondsInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mSecondsInputLayout.setEnabled(true);

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the minutes field, if the minutes are equal to 60, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
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
     * Handles completing the add time button edit from the IME keyboard.
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
                    setAddButtonText();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

}

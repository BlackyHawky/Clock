// SPDX-License-Identifier: GPL-3.0-only

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
import java.util.concurrent.TimeUnit;

/**
 * DialogFragment to set a new duration to the timer.
 */
public class TimerSetNewDurationDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of TimerSetNewDurationDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_new_duration_dialog";

    private static final String ARG_EDIT_HOURS = "arg_edit_hours";
    private static final String ARG_EDIT_MINUTES = "arg_edit_minutes";
    private static final String ARG_EDIT_SECONDS = "arg_edit_seconds";
    private static final String ARG_TIMER_ID = "arg_timer_id";

    private TextInputLayout mHoursInputLayout;
    private TextInputLayout mMinutesInputLayout;
    private TextInputLayout mSecondsInputLayout;
    private EditText mEditHours;
    private EditText mEditMinutes;
    private EditText mEditSeconds;
    private Button mOkButton;
    private int mTimerId;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;

    public static TimerSetNewDurationDialogFragment newInstance(Timer timer) {
        final Bundle args = new Bundle();

        long remainingTime = timer.getRemainingTime();
        int hours = (int) TimeUnit.MILLISECONDS.toHours(remainingTime);
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60;

        args.putInt(ARG_EDIT_HOURS, hours);
        args.putInt(ARG_EDIT_MINUTES, minutes);
        args.putInt(ARG_EDIT_SECONDS, seconds);
        args.putInt(ARG_TIMER_ID, timer.getId());

        final TimerSetNewDurationDialogFragment frag = new TimerSetNewDurationDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Replaces any existing TimerSetNewDurationDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, TimerSetNewDurationDialogFragment fragment) {
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
        if (mEditHours != null && mEditMinutes != null && mEditSeconds != null) {
            outState.putString(ARG_EDIT_HOURS, Objects.requireNonNull(mEditHours.getText()).toString());
            outState.putString(ARG_EDIT_MINUTES, Objects.requireNonNull(mEditMinutes.getText()).toString());
            outState.putString(ARG_EDIT_SECONDS, Objects.requireNonNull(mEditSeconds.getText()).toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        mTimerId = args.getInt(ARG_TIMER_ID, -1);

        int editHours = args.getInt(ARG_EDIT_HOURS, 0);
        int editMinutes = args.getInt(ARG_EDIT_MINUTES, 0);
        int editSeconds = args.getInt(ARG_EDIT_SECONDS, 0);
        if (savedInstanceState != null) {
            editHours = savedInstanceState.getInt(ARG_EDIT_HOURS, editHours);
            editMinutes = savedInstanceState.getInt(ARG_EDIT_MINUTES, editMinutes);
            editSeconds = savedInstanceState.getInt(ARG_EDIT_SECONDS, editSeconds);
        }

        View view = getLayoutInflater().inflate(R.layout.timer_dialog_edit_new_time, null);

        mHoursInputLayout = view.findViewById(R.id.dialog_input_layout_hours);
        mHoursInputLayout.setHelperText(getString(R.string.timer_hours_warning_box_text));

        mMinutesInputLayout = view.findViewById(R.id.dialog_input_layout_minutes);
        mMinutesInputLayout.setHelperText(getString(R.string.timer_minutes_warning_box_text));

        mSecondsInputLayout = view.findViewById(R.id.dialog_input_layout_seconds);
        mSecondsInputLayout.setHelperText(getString(R.string.timer_seconds_warning_box_text));

        mEditHours = view.findViewById(R.id.edit_hours);
        mEditMinutes = view.findViewById(R.id.edit_minutes);
        mEditSeconds = view.findViewById(R.id.edit_seconds);

        mEditHours.setText(String.valueOf(editHours));
        if (editHours == 24) {
            mEditHours.setImeOptions(EditorInfo.IME_ACTION_DONE);
            mEditHours.setOnEditorActionListener(new ImeDoneListener());
            mMinutesInputLayout.setEnabled(false);
            mSecondsInputLayout.setEnabled(false);
        } else {
            mEditHours.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            mMinutesInputLayout.setEnabled(true);
            mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT & EditorInfo.IME_ACTION_PREVIOUS);
            mSecondsInputLayout.setEnabled(true);
        }
        mEditHours.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditHours.selectAll();
        mEditHours.requestFocus();
        mEditHours.addTextChangedListener(mTextWatcher);
        mEditHours.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditHours.selectAll();
            }
        });

        mEditMinutes.setText(String.valueOf(editMinutes));
        mEditMinutes.selectAll();
        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
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

        String inputHoursText = mEditHours.getText().toString();
        String inputMinutesText = mEditMinutes.getText().toString();
        String inputSecondsText = mEditSeconds.getText().toString();

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        final Drawable drawable = AppCompatResources.getDrawable(requireContext(),
                isInvalidInput(inputHoursText, inputMinutesText, inputSecondsText)
                        ? R.drawable.ic_error
                        : R.drawable.ic_hourglass_top);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        final MaterialAlertDialogBuilder dialogBuilder =
                new MaterialAlertDialogBuilder(requireContext())
                        .setIcon(drawable)
                        .setTitle(isInvalidInput(inputHoursText, inputMinutesText, inputSecondsText)
                                ? getString(R.string.timer_time_warning_box_title)
                                : getString(R.string.timer_time_box_title))
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (isInvalidInput(inputHoursText, inputMinutesText, inputSecondsText)) {
                                updateDialogForInvalidInput();
                            } else {
                                setNewDuration();
                                dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(dialog -> {
            mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

            String hoursText = Objects.requireNonNull(mEditHours.getText()).toString();
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsText = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

            mOkButton.setEnabled(!isInvalidInput(hoursText, minutesText, secondsText));
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
        mEditSeconds.setOnEditorActionListener(null);
        mEditSeconds.removeTextChangedListener(mTextWatcher);
    }

    /**
     * Sets the new duration to the timer.
     */
    private void setNewDuration() {
        String hoursText = Objects.requireNonNull(mEditHours.getText()).toString();
        String minutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
        String secondsText = Objects.requireNonNull(mEditSeconds.getText()).toString();

        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        if (!hoursText.isEmpty()) {
            hours = Integer.parseInt(hoursText);
        }

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        if (!secondsText.isEmpty()) {
            seconds = Integer.parseInt(secondsText);
        }

        if ((hoursText.isEmpty() && minutesText.isEmpty() && secondsText.isEmpty())
                || (hours == 0 && minutes == 0 && seconds == 0)) {
            seconds = 1;
        }

        if (mTimerId >= 0) {
            final Timer timer = DataModel.getDataModel().getTimer(mTimerId);
            if (timer != null) {
                int totalSeconds = hours * 3600 + minutes * 60 + seconds;
                long newLengthMillis = totalSeconds * 1000L;
                DataModel.getDataModel().setNewTimerDuration(timer, newLengthMillis);
            }
        }
    }

    /**
     * @return {@code true} if:
     * <ul>
     *     <li>hours are less than 0 or greater than 24</li>
     *     <li>minutes are less than 0 or greater than 59</li>
     *     <li>seconds are less than 0 or greater than 59</li>
     * </ul>
     * {@code false} otherwise.
     */
    private boolean isInvalidInput(String hoursText, String minutesText, String secondsText) {
        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        if (!hoursText.isEmpty()) {
            hours = Integer.parseInt(hoursText);
        }

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        if (!secondsText.isEmpty()) {
            seconds = Integer.parseInt(secondsText);
        }

        return hours < 0 || hours > 24 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59;
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

        String hoursText = Objects.requireNonNull(mEditHours.getText()).toString();
        String minutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
        String secondsText = Objects.requireNonNull(mEditSeconds.getText()).toString();
        boolean hoursInvalid = (!hoursText.isEmpty() && Integer.parseInt(hoursText) < 0)
                || (!hoursText.isEmpty() && Integer.parseInt(hoursText) > 24);
        boolean minutesInvalid = (!minutesText.isEmpty() && Integer.parseInt(minutesText) < 0)
                || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 59);
        boolean secondsInvalid = (!secondsText.isEmpty() && Integer.parseInt(secondsText) < 0)
                || (!secondsText.isEmpty() && Integer.parseInt(secondsText) > 59);
        boolean disableHours = minutesInvalid || secondsInvalid;
        boolean disableMinutes = hoursInvalid || secondsInvalid;
        boolean disableSeconds = hoursInvalid|| minutesInvalid;
        int invalidColor = ContextCompat.getColor(requireContext(), R.color.md_theme_error);
        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mHoursInputLayout.setBoxStrokeColor(hoursInvalid ? invalidColor : validColor);
        mHoursInputLayout.setHintTextColor(hoursInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
        mHoursInputLayout.setEnabled(!disableHours);

        mMinutesInputLayout.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mMinutesInputLayout.setHintTextColor(minutesInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
        mMinutesInputLayout.setEnabled(!disableMinutes);

        mSecondsInputLayout.setBoxStrokeColor(secondsInvalid ? invalidColor : validColor);
        mSecondsInputLayout.setHintTextColor(secondsInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
        mSecondsInputLayout.setEnabled(!disableSeconds);

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

        mHoursInputLayout.setBoxStrokeColor(validColor);
        mHoursInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mHoursInputLayout.setEnabled(true);

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
     * Note: In the hours field, if the hours are equal to 24, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            String hoursText = mEditHours.getText() != null ? mEditHours.getText().toString() : "";
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
            String secondsText = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

            if (isInvalidInput(hoursText, minutesText, secondsText)) {
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
                mSecondsInputLayout.setEnabled(false);

                if (!"0".equals(minutesText)) {
                    mEditMinutes.setText("0");
                }

                if(!"0".equals(secondsText)) {
                    mEditSeconds.setText("0");
                }
            } else {
                mEditHours.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mMinutesInputLayout.setEnabled(true);
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT & EditorInfo.IME_ACTION_PREVIOUS);
                mSecondsInputLayout.setEnabled(true);
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
     * Handles completing the new duration from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputHoursText = Objects.requireNonNull(mEditHours.getText()).toString();
                String inputMinutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
                String inputSecondsText = Objects.requireNonNull(mEditSeconds.getText()).toString();
                if (isInvalidInput(inputHoursText, inputMinutesText, inputSecondsText)) {
                    updateDialogForInvalidInput();
                } else {
                    setNewDuration();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

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

    public static final String RESULT_PREF_KEY = VOLUME_CRESCENDO_DURATION + "result_pref_key";
    public static final String REQUEST_KEY = VOLUME_CRESCENDO_DURATION + "request_key";
    public static final String VOLUME_CRESCENDO_DURATION_VALUE = VOLUME_CRESCENDO_DURATION + "value";

    private TextInputLayout mMinutesInputLayout;
    private TextInputLayout mSecondsInputLayout;
    private TextInputEditText mEditMinutes;
    private TextInputEditText mEditSeconds;
    private InputMethodManager mInput;

    public static VolumeCrescendoDurationDialogFragment newInstance(String key, int totalSeconds) {
        Bundle args = new Bundle();

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        args.putString(ARG_PREF_KEY, key);
        args.putLong(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, minutes);
        args.putLong(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, seconds);

        VolumeCrescendoDurationDialogFragment frag = new VolumeCrescendoDurationDialogFragment();
        frag.setArguments(args);
        return frag;
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
            outState.putString(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, Objects.requireNonNull(mEditMinutes.getText()).toString());
            outState.putString(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, Objects.requireNonNull(mEditSeconds.getText()).toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();

        long editMinutes = args.getLong(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, 0);
        long editSeconds = args.getLong(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, 0);
        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getLong(ARG_EDIT_VOLUME_CRESCENDO_MINUTES, editMinutes);
            editSeconds = savedInstanceState.getLong(ARG_EDIT_VOLUME_CRESCENDO_SECONDS, editSeconds);
        }

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.settings_volume_crescendo_duration_dialog, null);

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
            mEditSeconds.setEnabled(false);
        } else {
            mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            mEditSeconds.setEnabled(true);
        }
        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditMinutes.selectAll();
        mEditMinutes.requestFocus();
        mEditMinutes.addTextChangedListener(new TextChangeListener());
        mEditMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditMinutes.selectAll();
            }
        });

        mEditSeconds.setText(String.valueOf(editSeconds));
        mEditSeconds.selectAll();
        mEditSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditSeconds.setOnEditorActionListener(new ImeDoneListener());
        mEditSeconds.addTextChangedListener(new TextChangeListener());
        mEditSeconds.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mEditSeconds.selectAll();
            }
        });

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.crescendo_duration_title))
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        setVolumeCrescendoDuration())
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog = dialogBuilder.create();

        final Window alertDialogWindow = dialog.getWindow();
        if (alertDialogWindow != null) {
            alertDialogWindow.setSoftInputMode(SOFT_INPUT_ADJUST_PAN | SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop callbacks from the IME since there is no view to process them.
        mEditMinutes.setOnEditorActionListener(null);
        mEditSeconds.setOnEditorActionListener(null);
    }

    /**
     * Set the volume crescendo duration.
     */
    private void setVolumeCrescendoDuration() {
        String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";
        String secondsText = mEditSeconds.getText() != null ? mEditSeconds.getText().toString() : "";

        int minutes = 0;
        int seconds = 0;

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        if (!secondsText.isEmpty()) {
            seconds = Integer.parseInt(secondsText);
        }

        if (minutes == 60) {
            seconds = 0;
        }

        int total = minutes * 60 + seconds;

        Bundle result = new Bundle();
        result.putInt(VOLUME_CRESCENDO_DURATION_VALUE, total);
        result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
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
     * Update the dialog icon and title for invalid entries.
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
        int validColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mMinutesInputLayout.setHintTextColor(minutesInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));

        mSecondsInputLayout.setBoxStrokeColor(secondsInvalid ? invalidColor : validColor);
        mSecondsInputLayout.setHintTextColor(secondsInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
    }

    /**
     * Update the dialog icon and title for valid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForValidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();
        alertDialog.setIcon(null);
        alertDialog.setTitle(getString(R.string.crescendo_duration_title));

        int validColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLACK);
        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
        mSecondsInputLayout.setBoxStrokeColor(validColor);
        mSecondsInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the hours field, if the hours are equal to 24, the entry can be validated with
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
                mEditSeconds.setEnabled(false);
            } else {
                mEditMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                mEditSeconds.setEnabled(true);
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
                String inputHoursText = Objects.requireNonNull(mEditMinutes.getText()).toString();
                String inputMinutesText = Objects.requireNonNull(mEditSeconds.getText()).toString();
                if (isInvalidInput(inputHoursText, inputMinutesText)) {
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

}

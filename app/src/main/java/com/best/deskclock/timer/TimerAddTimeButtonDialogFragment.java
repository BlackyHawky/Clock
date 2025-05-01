/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

    private static final String ARG_TIME_BUTTON = "arg_time_button";
    private static final String ARG_TIMER_ID = "arg_timer_id";

    private TextInputLayout mLabelInputLayout;
    private EditText mEditAddTimeButton;
    private int mTimerId;
    private InputMethodManager mInput;


    public static TimerAddTimeButtonDialogFragment newInstance(Timer timer) {
        final Bundle args = new Bundle();
        args.putString(ARG_TIME_BUTTON, timer.getButtonTime());
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

        // Remove existing instance of LabelDialogFragment if necessary.
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
        // As long as the add time button box exists, save its state.
        if (mEditAddTimeButton != null) {
            outState.putString(ARG_TIME_BUTTON, Objects.requireNonNull(mEditAddTimeButton.getText()).toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        mTimerId = args.getInt(ARG_TIMER_ID, -1);

        String addButtonText = args.getString(ARG_TIME_BUTTON);
        if (savedInstanceState != null) {
            addButtonText = savedInstanceState.getString(ARG_TIME_BUTTON, addButtonText);
        }

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null);

        mLabelInputLayout = view.findViewById(R.id.dialog_input_layout);
        mLabelInputLayout.setHelperText(getString(R.string.timer_button_time_warning_box_text));

        mEditAddTimeButton = view.findViewById(android.R.id.edit);
        mEditAddTimeButton.setText(addButtonText);
        mEditAddTimeButton.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditAddTimeButton.selectAll();
        mEditAddTimeButton.requestFocus();
        mEditAddTimeButton.setOnEditorActionListener(new ImeDoneListener());
        mEditAddTimeButton.addTextChangedListener(new TextChangeListener());

        String inputText = mEditAddTimeButton.getText().toString();
        if (isInvalidInput(inputText)) {
            mLabelInputLayout.setBoxStrokeColor(ContextCompat.getColor(requireContext(), R.color.md_theme_error));
        }

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mInput.showSoftInput(mEditAddTimeButton, InputMethodManager.SHOW_IMPLICIT);

        final Drawable drawable = AppCompatResources.getDrawable(requireContext(), isInvalidInput(inputText)
                ? R.drawable.ic_error
                : R.drawable.ic_hourglass_top);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        final MaterialAlertDialogBuilder dialogBuilder =
                new MaterialAlertDialogBuilder(requireContext())
                        .setIcon(drawable)
                        .setTitle(isInvalidInput(inputText)
                                ? getString(R.string.timer_button_time_warning_box_title)
                                : getString(R.string.timer_button_time_box_title))
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, new OkListener())
                        .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog = dialogBuilder.create();

        final Window alertDialogWindow = dialog.getWindow();
        if (alertDialogWindow != null) {
            alertDialogWindow.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop callbacks from the IME since there is no view to process them.
        mEditAddTimeButton.setOnEditorActionListener(null);
    }

    /**
     * Sets the new time into the timer add button.
     */
    private void setAddButtonText() {
        String addButtonText = Objects.requireNonNull(mEditAddTimeButton.getText()).toString();

        if (mTimerId >= 0) {
            final Timer timer = DataModel.getDataModel().getTimer(mTimerId);
            if (timer != null) {
                DataModel.getDataModel().setTimerButtonTime(timer, addButtonText);
            }
        }
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            String inputText = charSequence.toString();
            if (isInvalidInput(inputText)) {
                updateDialogForInvalidInput();
            } else {
                updateDialogForValidInput();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    /**
     * @return {@code true} if the entry is empty, less than 1 minute or greater than 120 minutes.
     * {@code false} otherwise.
     */
    private boolean isInvalidInput(String inputText) {
        if (inputText.isEmpty()) {
            return true;
        }

        int minutes = Integer.parseInt(inputText);
        return minutes < 1 || minutes > 120;
    }

    /**
     * Update the dialog icon and title for invalid entries. The outline color of the edit box is also changed.
     */
    private void updateDialogForInvalidInput() {
        final Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }
        AlertDialog alertDialog = (AlertDialog) requireDialog();
        alertDialog.setIcon(drawable);
        alertDialog.setTitle(getString(R.string.timer_button_time_warning_box_title));
        mLabelInputLayout.setBoxStrokeColor(ContextCompat.getColor(requireContext(), R.color.md_theme_error));
    }

    /**
     * Update the dialog icon and title for valid entries. The outline color of the edit box is also changed.
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
        mLabelInputLayout.setBoxStrokeColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
    }


    /**
     * Handles completing the add time button edit from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputText = Objects.requireNonNull(mEditAddTimeButton.getText()).toString();
                if (isInvalidInput(inputText)) {
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

    /**
     * Handles completing the add time button edit from the Ok button of the dialog.
     */
    private class OkListener implements DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mInput.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
            String inputText = Objects.requireNonNull(mEditAddTimeButton.getText()).toString();
            if (isInvalidInput(inputText)) {
                updateDialogForInvalidInput();
            } else {
                setAddButtonText();
                dismiss();
            }
        }
    }

}

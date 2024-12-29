/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.utils.Utils;

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

    private AppCompatEditText mAddTimeButtonBox;
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
        if (mAddTimeButtonBox != null) {
            outState.putString(ARG_TIME_BUTTON, Objects.requireNonNull(mAddTimeButtonBox.getText()).toString());
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

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setPositiveButton(android.R.string.ok, new OkListener())
                .setNegativeButton(android.R.string.cancel, null)
                .setTitle(R.string.timer_button_time_box_title)
                .create();

        mAddTimeButtonBox = new AppCompatEditText(requireContext());
        mAddTimeButtonBox.requestFocus();
        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mInput.showSoftInput(mAddTimeButtonBox, InputMethodManager.SHOW_IMPLICIT);
        mAddTimeButtonBox.setOnEditorActionListener(new ImeDoneListener());
        mAddTimeButtonBox.addTextChangedListener(new TextChangeListener());
        mAddTimeButtonBox.setSingleLine();
        mAddTimeButtonBox.setInputType(InputType.TYPE_CLASS_NUMBER);
        mAddTimeButtonBox.setText(addButtonText);
        mAddTimeButtonBox.selectAll();

        // The line at the bottom of EditText is part of its background therefore the padding
        // must be added to its container.
        final int padding = Utils.toPixel(21, requireContext());
        dialog.setView(mAddTimeButtonBox, padding, 0, padding, 0);

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
        mAddTimeButtonBox.setOnEditorActionListener(null);
    }

    /**
     * Sets the new time into the timer add button.
     */
    private void setAddButtonText() {
        String addButtonText = Objects.requireNonNull(mAddTimeButtonBox.getText()).toString();

        if (mTimerId >= 0 && !addButtonText.isEmpty()) {
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
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mAddTimeButtonBox.setActivated(!TextUtils.isEmpty(s));
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
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
                setAddButtonText();
                dismissAllowingStateLoss();
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
            setAddButtonText();
            dismiss();
        }
    }
}

/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.provider.Alarm;

import com.best.deskclock.utils.SdkUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

/**
 * DialogFragment to edit label.
 */
public class LabelDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of LabelDialogFragment in the fragment manager.
     */
    private static final String TAG = "label_dialog";

    private static final String ARG_LABEL = "arg_label";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_TIMER_ID = "arg_timer_id";
    private static final String ARG_TAG = "arg_tag";

    private EditText mEditLabel;
    private Alarm mAlarm;
    private int mTimerId;
    private String mTag;

    public static LabelDialogFragment newInstance(Alarm alarm, String label, String tag) {
        final Bundle args = new Bundle();
        args.putString(ARG_LABEL, label);
        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);

        final LabelDialogFragment frag = new LabelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    public static LabelDialogFragment newInstance(Timer timer) {
        final Bundle args = new Bundle();
        args.putString(ARG_LABEL, timer.getLabel());
        args.putInt(ARG_TIMER_ID, timer.getId());

        final LabelDialogFragment frag = new LabelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Replaces any existing LabelDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, LabelDialogFragment fragment) {
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
        // As long as the label box exists, save its state.
        if (mEditLabel != null) {
            outState.putString(ARG_LABEL, Objects.requireNonNull(mEditLabel.getText()).toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        mAlarm = SdkUtils.isAtLeastAndroid13()
                ? args.getParcelable(ARG_ALARM, Alarm.class)
                : args.getParcelable(ARG_ALARM);
        mTimerId = args.getInt(ARG_TIMER_ID, -1);
        mTag = args.getString(ARG_TAG);

        String label = args.getString(ARG_LABEL);
        if (savedInstanceState != null) {
            label = savedInstanceState.getString(ARG_LABEL, label);
        }

        final Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_label);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null);

        mEditLabel = view.findViewById(android.R.id.edit);
        mEditLabel.setText(label);
        mEditLabel.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mEditLabel.selectAll();
        mEditLabel.requestFocus();
        mEditLabel.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setLabel();
                dismiss();
                return true;
            }
            return false;
        });

        final MaterialAlertDialogBuilder dialogBuilder =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(mAlarm != null
                                ? R.string.alarm_label_box_title
                                : mTimerId >= 0
                                ? R.string.timer_label_box_title
                                : 0)
                        .setIcon(drawable)
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            setLabel();
                            dismiss();
                        })
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
        mEditLabel.setOnEditorActionListener(null);
    }

    /**
     * Sets the new label into the timer or alarm.
     */
    private void setLabel() {
        String label = Objects.requireNonNull(mEditLabel.getText()).toString();
        if (label.trim().isEmpty()) {
            // Don't allow user to input label with only whitespace.
            label = "";
        }

        if (mAlarm != null) {
            ((AlarmLabelDialogHandler) requireActivity()).onDialogLabelSet(mAlarm, label, mTag);
        } else if (mTimerId >= 0) {
            final Timer timer = DataModel.getDataModel().getTimer(mTimerId);
            if (timer != null) {
                DataModel.getDataModel().setTimerLabel(timer, label);
            }
        }
    }

    public interface AlarmLabelDialogHandler {
        void onDialogLabelSet(Alarm alarm, String label, String tag);
    }

}

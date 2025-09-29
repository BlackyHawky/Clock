/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.provider.Alarm;

import com.best.deskclock.utils.SdkUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

/**
 * DialogFragment to edit either the alarm label, timer label, or city note.
 */
public class LabelDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of LabelDialogFragment in the fragment manager.
     */
    private static final String TAG = "label_dialog";

    private static final String ARG_TAG = "arg_tag";

    private static final String ARG_LABEL = "arg_label";
    private static final String ARG_ALARM = "arg_alarm";

    private static final String ARG_TIMER_ID = "arg_timer_id";

    private static final String ARG_CITY_ID = "city_id";
    private static final String ARG_CITY_NAME = "city_name";

    private Context mContext;
    private EditText mEditLabel;
    private Alarm mAlarm;
    private int mTimerId;
    private String mCityId;
    private String mTag;

    /**
     * Creates a new instance of {@link LabelDialogFragment} to edit the label of the given alarm.
     *
     * @param alarm the {@link Alarm} whose label will be edited
     * @param label the current label of the alarm, or an empty string if none
     * @param tag   the tag used to identify the fragment that will receive the result
     */
    public static LabelDialogFragment newInstance(Alarm alarm, String label, String tag) {
        final Bundle args = new Bundle();
        args.putString(ARG_LABEL, label);
        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);

        final LabelDialogFragment frag = new LabelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link LabelDialogFragment} to edit the label of the given timer.
     *
     * @param timer the {@link Timer} whose label will be edited
     */
    public static LabelDialogFragment newInstance(Timer timer) {
        final Bundle args = new Bundle();
        args.putString(ARG_LABEL, timer.getLabel());
        args.putInt(ARG_TIMER_ID, timer.getId());

        final LabelDialogFragment frag = new LabelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link LabelDialogFragment} to edit or add a note for the specified city.
     *
     * @param cityId      the unique identifier of the city
     * @param cityName    the name of the {@link City} to display in the dialog title
     * @param currentNote the existing note for the city, or an empty string if none
     * @param tag         the tag used to identify the fragment that will receive the result
     */
    public static LabelDialogFragment newInstance(String cityId, String cityName, String currentNote,
                                                  String tag) {

        LabelDialogFragment fragment = new LabelDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CITY_ID, cityId);
        args.putString(ARG_CITY_NAME, cityName);
        args.putString(ARG_LABEL, currentNote);
        args.putString(ARG_TAG, tag);
        fragment.setArguments(args);
        return fragment;
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
        mContext = requireContext();

        final Bundle args = requireArguments();
        mTag = args.getString(ARG_TAG);

        mAlarm = SdkUtils.isAtLeastAndroid13()
                ? args.getParcelable(ARG_ALARM, Alarm.class)
                : args.getParcelable(ARG_ALARM);

        mTimerId = args.getInt(ARG_TIMER_ID, -1);

        mCityId = requireArguments().getString(ARG_CITY_ID);
        String cityName = requireArguments().getString(ARG_CITY_NAME);

        String label = args.getString(ARG_LABEL);
        if (savedInstanceState != null) {
            label = savedInstanceState.getString(ARG_LABEL, label);
        }

        final Drawable drawable;
        int iconResId;
        final CharSequence title;

        if (mAlarm != null) {
            iconResId = R.drawable.ic_label;
            title = getString(R.string.alarm_label_box_title);
        } else if (mTimerId >= 0) {
            iconResId = R.drawable.ic_label;
            title = getString(R.string.timer_label_box_title);
        } else if (mCityId != null) {
            iconResId = R.drawable.ic_note;
            title = getString(R.string.city_note_dialog_title, cityName);
        } else {
            iconResId = 0;
            title = null;
        }

        // Load and tint the icon if applicable
        if (iconResId != 0) {
            drawable = AppCompatResources.getDrawable(mContext, iconResId);
            if (drawable != null) {
                drawable.setTint(MaterialColors.getColor(
                        mContext, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
            }
        } else {
            drawable = null;
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);

        mEditLabel = view.findViewById(android.R.id.edit);
        mEditLabel.setText(label);
        mEditLabel.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mEditLabel.selectAll();
        mEditLabel.requestFocus();
        mEditLabel.setMaxLines(2);
        mEditLabel.setHorizontallyScrolling(false);
        mEditLabel.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setLabel();
                dismiss();
                return true;
            }
            return false;
        });

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(title)
                .setIcon(drawable)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> setLabel())
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog = dialogBuilder.create();

        final Window alertDialogWindow = dialog.getWindow();
        if (alertDialogWindow != null) {
            alertDialogWindow.setSoftInputMode(SOFT_INPUT_ADJUST_PAN | SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        mEditLabel.requestFocus();
        mEditLabel.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mEditLabel, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop callbacks from the IME since there is no view to process them.
        mEditLabel.setOnEditorActionListener(null);
    }

    /**
     * Applies the label or note entered by the user to the appropriate target:
     * an {@link Alarm}, a {@link Timer}, or a {@link City}.
     * <p>
     * If the input is only whitespace, it will be treated as an empty label.
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
        } else if (mCityId != null) {
            ((CityNoteDialogHandler) requireActivity()).onDialogCityNoteSet(mCityId, label, mTag);
        }
    }

    /**
     * Callback interface for handling the result of the alarm label dialog.
     */
    public interface AlarmLabelDialogHandler {

        /**
         * Called when the user confirms the new label for the given alarm.
         *
         * @param alarm the {@link Alarm} that was labeled
         * @param label the new label entered by the user
         * @param tag   an optional tag used to identify the target fragment or context
         */
        void onDialogLabelSet(Alarm alarm, String label, String tag);
    }

    /**
     * Callback interface for handling the result of the city note dialog.
     */
    public interface CityNoteDialogHandler {

        /**
         * Called when the user confirms a note for a city.
         *
         * @param cityId the ID of the city being edited
         * @param note   the note entered by the user
         * @param tag    an optional tag used to identify the target fragment or context
         */
        void onDialogCityNoteSet(String cityId, String note, String tag);
    }
}

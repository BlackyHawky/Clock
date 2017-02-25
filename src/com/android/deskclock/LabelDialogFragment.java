/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.deskclock;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.provider.Alarm;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

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

    private AppCompatEditText mLabelBox;
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
        if (mLabelBox != null) {
            outState.putString(ARG_LABEL, mLabelBox.getText().toString());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        mAlarm = args.getParcelable(ARG_ALARM);
        mTimerId = args.getInt(ARG_TIMER_ID, -1);
        mTag = args.getString(ARG_TAG);

        String label = args.getString(ARG_LABEL);
        if (savedInstanceState != null) {
            label = savedInstanceState.getString(ARG_LABEL, label);
        }

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setPositiveButton(android.R.string.ok, new OkListener())
                .setNegativeButton(android.R.string.cancel, null /* listener */)
                .setMessage(R.string.label)
                .create();
        final Context context = dialog.getContext();

        final int colorControlActivated =
                ThemeUtils.resolveColor(context, R.attr.colorControlActivated);
        final int colorControlNormal =
                ThemeUtils.resolveColor(context, R.attr.colorControlNormal);

        mLabelBox = new AppCompatEditText(context);
        mLabelBox.setSupportBackgroundTintList(new ColorStateList(
                new int[][] { { android.R.attr.state_activated }, {} },
                new int[] { colorControlActivated, colorControlNormal }));
        mLabelBox.setOnEditorActionListener(new ImeDoneListener());
        mLabelBox.addTextChangedListener(new TextChangeListener());
        mLabelBox.setSingleLine();
        mLabelBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mLabelBox.setText(label);
        mLabelBox.selectAll();

        // The line at the bottom of EditText is part of its background therefore the padding
        // must be added to its container.
        final int padding = context.getResources()
                .getDimensionPixelSize(R.dimen.label_edittext_padding);
        dialog.setView(mLabelBox, padding, 0, padding, 0);

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
        mLabelBox.setOnEditorActionListener(null);
    }

    /**
     * Sets the new label into the timer or alarm.
     */
    private void setLabel() {
        String label = mLabelBox.getText().toString();
        if (label.trim().isEmpty()) {
            // Don't allow user to input label with only whitespace.
            label = "";
        }

        if (mAlarm != null) {
            ((AlarmLabelDialogHandler) getActivity()).onDialogLabelSet(mAlarm, label, mTag);
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

    /**
     * Alters the UI to indicate when input is valid or invalid.
     */
    private class TextChangeListener implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mLabelBox.setActivated(!TextUtils.isEmpty(s));
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    /**
     * Handles completing the label edit from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setLabel();
                dismissAllowingStateLoss();
                return true;
            }
            return false;
        }
    }

    /**
     * Handles completing the label edit from the Ok button of the dialog.
     */
    private class OkListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            setLabel();
            dismiss();
        }
    }
}

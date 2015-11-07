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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.provider.Alarm;

import static android.graphics.Color.RED;
import static android.graphics.Color.WHITE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

/**
 * DialogFragment to edit label.
 */
public class LabelDialogFragment extends DialogFragment {

    private static final String KEY_LABEL = "label";
    private static final String KEY_ALARM = "alarm";
    private static final String KEY_TIMER_ID = "timer_id";
    private static final String KEY_TAG = "tag";

    private AppCompatEditText mLabelBox;
    private Alarm mAlarm;
    private int mTimerId;
    private String mTag;

    public static LabelDialogFragment newInstance(Alarm alarm, String label, String tag) {
        final Bundle args = new Bundle();
        args.putString(KEY_LABEL, label);
        args.putParcelable(KEY_ALARM, alarm);
        args.putString(KEY_TAG, tag);

        final LabelDialogFragment frag = new LabelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    public static LabelDialogFragment newInstance(Timer timer) {
        final Bundle args = new Bundle();
        args.putString(KEY_LABEL, timer.getLabel());
        args.putInt(KEY_TIMER_ID, timer.getId());

        final LabelDialogFragment frag = new LabelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LABEL, mLabelBox.getText().toString());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        mAlarm = bundle.getParcelable(KEY_ALARM);
        mTimerId = bundle.getInt(KEY_TIMER_ID, -1);
        mTag = bundle.getString(KEY_TAG);

        final String label = savedInstanceState != null ?
                savedInstanceState.getString(KEY_LABEL) : bundle.getString(KEY_LABEL);

        final Context context = getActivity();

        mLabelBox = new AppCompatEditText(context);
        mLabelBox.setOnEditorActionListener(new ImeDoneListener());
        mLabelBox.addTextChangedListener(new TextChangeListener(context));
        mLabelBox.setSingleLine();
        mLabelBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mLabelBox.setText(label);
        mLabelBox.selectAll();

        final int padding = getResources().getDimensionPixelSize(R.dimen.label_edittext_padding);
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(mLabelBox, padding, 0, padding, 0)
                .setPositiveButton(R.string.time_picker_set, new OkListener())
                .setNegativeButton(R.string.time_picker_cancel, new CancelListener())
                .setMessage(R.string.label)
                .create();

        alertDialog.getWindow().setSoftInputMode(SOFT_INPUT_STATE_VISIBLE);
        return alertDialog;
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

    interface AlarmLabelDialogHandler {
        void onDialogLabelSet(Alarm alarm, String label, String tag);
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     */
    private class TextChangeListener implements TextWatcher {

        private final int colorAccent;
        private final int colorControlNormal;

        public TextChangeListener(Context context) {
            colorAccent = Utils.obtainStyledColor(context, R.attr.colorAccent, RED);
            colorControlNormal = Utils.obtainStyledColor(context, R.attr.colorControlNormal, WHITE);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            final int color = TextUtils.isEmpty(s) ? colorControlNormal : colorAccent;
            mLabelBox.setSupportBackgroundTintList(ColorStateList.valueOf(color));
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable editable) {}
    }

    /**
     * Handles completing the label edit from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setLabel();
                dismiss();
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

    /**
     * Handles discarding the label edit from the Cancel button of the dialog.
     */
    private class CancelListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dismiss();
        }
    }
}
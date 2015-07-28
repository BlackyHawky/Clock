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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.timer.TimerObj;

/**
 * DialogFragment to edit label.
 */
public class LabelDialogFragment extends DialogFragment {

    private static final String KEY_LABEL = "label";
    private static final String KEY_ALARM = "alarm";
    private static final String KEY_TIMER = "timer";
    private static final String KEY_TAG = "tag";

    private AppCompatEditText mLabelBox;

    public static LabelDialogFragment newInstance(Alarm alarm, String label, String tag) {
        final Bundle args = new Bundle();
        args.putString(KEY_LABEL, label);
        args.putParcelable(KEY_ALARM, alarm);
        args.putString(KEY_TAG, tag);

        final LabelDialogFragment frag = new LabelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    public static LabelDialogFragment newInstance(TimerObj timer, String label, String tag) {
        final Bundle args = new Bundle();
        args.putString(KEY_LABEL, label);
        args.putParcelable(KEY_TIMER, timer);
        args.putString(KEY_TAG, tag);

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
        final Alarm alarm = bundle.getParcelable(KEY_ALARM);
        final TimerObj timer = bundle.getParcelable(KEY_TIMER);
        final String tag = bundle.getString(KEY_TAG);
        final String label = savedInstanceState != null ?
                savedInstanceState.getString(KEY_LABEL) : bundle.getString(KEY_LABEL);

        final Context context = getActivity();
        final int colorAccent = Utils.obtainStyledColor(context, R.attr.colorAccent, Color.RED);
        final int colorControlNormal =
                Utils.obtainStyledColor(context, R.attr.colorControlNormal, Color.WHITE);

        mLabelBox = new AppCompatEditText(context);
        mLabelBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    set(alarm, timer, tag);
                    return true;
                }
                return false;
            }
        });
        mLabelBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final int color = TextUtils.isEmpty(s) ? colorControlNormal : colorAccent;
                mLabelBox.setSupportBackgroundTintList(ColorStateList.valueOf(color));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mLabelBox.setText(label);
        mLabelBox.selectAll();

        final int padding = getResources().getDimensionPixelSize(R.dimen.label_edittext_padding);
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(mLabelBox, padding, 0, padding, 0)
                .setPositiveButton(R.string.time_picker_set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        set(alarm, timer, tag);
                    }
                })
                .setNegativeButton(R.string.time_picker_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                })
                .setMessage(R.string.label)
                .create();

        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return alertDialog;
    }

    private void set(Alarm alarm, TimerObj timer, String tag) {
        String label = mLabelBox.getText().toString();
        if (label.trim().isEmpty()) {
            // Don't allow user to input label with only whitespace.
            label = "";
        }

        if (alarm != null) {
            set(alarm, tag, label);
        } else if (timer != null) {
            set(timer, tag, label);
        } else {
            LogUtils.e("No alarm or timer available.");
        }
    }

    private void set(Alarm alarm, String tag, String label) {
        final Activity activity = getActivity();
        if (activity instanceof AlarmLabelDialogHandler) {
            ((AlarmLabelDialogHandler) activity).onDialogLabelSet(alarm, label, tag);
        } else {
            LogUtils.e("Error! Activities that use LabelDialogFragment must implement "
                    + "AlarmLabelDialogHandler or TimerLabelDialogHandler");
        }
        dismiss();
    }

    private void set(TimerObj timer, String tag, String label) {
        final Activity activity = getActivity();
        if (activity instanceof TimerLabelDialogHandler){
            ((TimerLabelDialogHandler) activity).onDialogLabelSet(timer, label, tag);
        } else {
            LogUtils.e("Error! Activities that use LabelDialogFragment must implement "
                    + "AlarmLabelDialogHandler or TimerLabelDialogHandler");
        }
        dismiss();
    }

    interface AlarmLabelDialogHandler {
        void onDialogLabelSet(Alarm alarm, String label, String tag);
    }

    interface TimerLabelDialogHandler {
        void onDialogLabelSet(TimerObj timer, String label, String tag);
    }
}

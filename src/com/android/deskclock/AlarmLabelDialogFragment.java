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
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * DialogFragment to edit label.
 */
public class AlarmLabelDialogFragment extends DialogFragment {

    private static final String KEY_LABEL = "label";
    private static final String KEY_ALARM = "alarm";

    private EditText mLabelBox;

    public static AlarmLabelDialogFragment newInstance(Alarm alarm, String label) {
        final AlarmLabelDialogFragment frag = new AlarmLabelDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_LABEL, label);
        args.putParcelable(KEY_ALARM, alarm);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final String label = getArguments().getString(KEY_LABEL);
        final Alarm alarm = getArguments().getParcelable(KEY_ALARM);

        final View view = inflater.inflate(R.layout.alarm_label_dialog, container, false);

        mLabelBox = (EditText) view.findViewById(R.id.labelBox);
        mLabelBox.setText(label);

        final Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        final Button setButton = (Button) view.findViewById(R.id.setButton);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Activity activity = getActivity();
                if (activity instanceof AlarmLabelDialogHandler) {
                    ((AlarmClock) getActivity()).onDialogLabelSet(alarm,
                            mLabelBox.getText().toString());
                } else {
                    Log.e("Error! Activities that use AlarmLabelDialogFragment must implement "
                            + "AlarmLabelDialogHandler");
                }
                dismiss();
            }
        });

        return view;
    }

    interface AlarmLabelDialogHandler {
        void onDialogLabelSet(Alarm alarm, String label);
    }
}

/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.deskclock.timer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.R;

public class TimerItemFragment extends Fragment {
    private static final String TAG = "TimerItemFragment_tag";
    private TimerObj mTimerObj;

    public TimerItemFragment() {
    }

    public static TimerItemFragment newInstance(TimerObj timerObj) {
        final TimerItemFragment fragment = new TimerItemFragment();
        final Bundle args = new Bundle();
        args.putParcelable(TAG, timerObj);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        if (bundle != null) {
            mTimerObj = (TimerObj) bundle.getParcelable(TAG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final TimerListItem v = (TimerListItem) inflater.inflate(R.layout.timer_list_item,
                null);
        mTimerObj.mView = v;
        final long timeLeft = mTimerObj.updateTimeLeft(false);
        final boolean drawWithColor = mTimerObj.mState != TimerObj.STATE_RESTART;
        v.set(mTimerObj.mOriginalLength, timeLeft, drawWithColor);
        v.setTime(timeLeft, true);
        v.setResetAddButton(mTimerObj.mState == TimerObj.STATE_RUNNING ||
                mTimerObj.mState == TimerObj.STATE_TIMESUP, new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Fragment parent = getParentFragment();
                if (parent instanceof TimerFragment) {
                    ((TimerFragment) parent).onPlusOneButtonPressed(mTimerObj);
                }
            }
        });
        switch (mTimerObj.mState) {
            case TimerObj.STATE_RUNNING:
                v.start();
                break;
            case TimerObj.STATE_TIMESUP:
                v.timesUp();
                break;
            default:
                break;
        }

        final CircleButtonsLayout circleLayout =
                (CircleButtonsLayout) v.findViewById(R.id.timer_circle);
        circleLayout.setCircleTimerViewIds(R.id.timer_time, R.id.reset_add, R.id.timer_label);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final View v = mTimerObj.mView;
        if (v == null) {
            return;
        }

        TextView label = (TextView) v.findViewById(R.id.timer_label);
        label.setText(mTimerObj.mLabel);
        if (getActivity() instanceof DeskClock) {
            label.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onLabelPressed(mTimerObj);
                }
            });
        } else if (TextUtils.isEmpty(mTimerObj.mLabel)) {
            label.setVisibility(View.INVISIBLE);
        }
    }

    private void onLabelPressed(TimerObj t) {
        final String dialogTag = "label_dialog";
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(dialogTag);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(t, t.mLabel, getParentFragment().getTag());
        newFragment.show(ft, dialogTag);
    }
}

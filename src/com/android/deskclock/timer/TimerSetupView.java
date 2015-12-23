/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.deskclock.FabContainer;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

import java.io.Serializable;
import java.util.Arrays;

import static com.android.deskclock.FabContainer.UpdateType.FAB_ONLY_ANIMATED;

public class TimerSetupView extends LinearLayout implements Button.OnClickListener,
        Button.OnLongClickListener {

    private final Button[] mNumbers = new Button[10];
    private final int[] mInput = {0, 0, 0, 0, 0, 0};
    private int mInputPointer = -1;
    private ImageButton mDelete;
    private TimerView mEnteredTime;
    private View mDivider;

    /** Updates to the fab are requested via this container. */
    private FabContainer mFabContainer;

    private final int mColorAccent;
    private final int mColorHairline;

    public TimerSetupView(Context context) {
        this(context, null /* attrs */);
    }

    public TimerSetupView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mColorAccent = Utils.obtainStyledColor(context, R.attr.colorAccent, Color.RED);
        mColorHairline = ContextCompat.getColor(context, R.color.hairline);

        LayoutInflater.from(context).inflate(R.layout.time_setup_view, this);
    }

    void setFabContainer(FabContainer fabContainer) {
        mFabContainer = fabContainer;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final View v1 = findViewById(R.id.first);
        final View v2 = findViewById(R.id.second);
        final View v3 = findViewById(R.id.third);
        final View v4 = findViewById(R.id.fourth);

        mDivider = findViewById(R.id.divider);
        mDelete = (ImageButton) findViewById(R.id.delete);
        mDelete.setOnClickListener(this);
        mDelete.setOnLongClickListener(this);
        mEnteredTime = (TimerView) findViewById(R.id.timer_time_text);

        mNumbers[1] = (Button) v1.findViewById(R.id.key_left);
        mNumbers[2] = (Button) v1.findViewById(R.id.key_middle);
        mNumbers[3] = (Button) v1.findViewById(R.id.key_right);

        mNumbers[4] = (Button) v2.findViewById(R.id.key_left);
        mNumbers[5] = (Button) v2.findViewById(R.id.key_middle);
        mNumbers[6] = (Button) v2.findViewById(R.id.key_right);

        mNumbers[7] = (Button) v3.findViewById(R.id.key_left);
        mNumbers[8] = (Button) v3.findViewById(R.id.key_middle);
        mNumbers[9] = (Button) v3.findViewById(R.id.key_right);

        mNumbers[0] = (Button) v4.findViewById(R.id.key_middle);
        v4.findViewById(R.id.key_left).setVisibility(INVISIBLE);
        v4.findViewById(R.id.key_right).setVisibility(INVISIBLE);

        for (int i = 0; i < mNumbers.length; i++) {
            mNumbers[i].setOnClickListener(this);
            mNumbers[i].setText(String.valueOf(i));
            mNumbers[i].setTextColor(Color.WHITE);
            mNumbers[i].setTag(R.id.numbers_key, i);
        }

        updateTime();
    }

    @Override
    public void onClick(View v) {
        final boolean validInputBeforeClick = hasValidInput();
        final Integer n = (Integer) v.getTag(R.id.numbers_key);
        // A number was pressed
        if (n != null) {
            // pressing "0" as the first digit does nothing
            if (mInputPointer == -1 && n == 0) {
                return;
            }

            // No space for more digits, so ignore input.
            if (mInputPointer == mInput.length - 1) {
                return;
            }

            // Append the new digit.
            System.arraycopy(mInput, 0, mInput, 1, mInputPointer + 1);
            mInput[0] = n;
            mInputPointer++;
            updateTime();

            // Update talkback to read the number being deleted
            final Resources resources = getResources();
            final String cd = resources.getString(R.string.timer_descriptive_delete, n.toString());
            mDelete.setContentDescription(cd);
        }

        // other keys
        if (v == mDelete) {
            if (mInputPointer < 0) {
                // Nothing exists to delete so return.
                return;
            }

            System.arraycopy(mInput, 1, mInput, 0, mInputPointer);
            mInput[mInputPointer] = 0;
            mInputPointer--;
            updateTime();

            // Update talkback to read the number being deleted or its original description.
            final String number = mInputPointer < 0 ? "" : Integer.toString(mInput[mInputPointer]);
            final String cd = getResources().getString(R.string.timer_descriptive_delete, number);
            mDelete.setContentDescription(cd);
        }

        if (validInputBeforeClick != hasValidInput()) {
            updateFab();
            updateDeleteButtonAndDivider();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == mDelete) {
            reset();
            return true;
        }
        return false;
    }

    public void reset() {
        if (mInputPointer != -1) {
            Arrays.fill(mInput, 0);
            mInputPointer = -1;
            updateFab();
            updateTime();
            updateDeleteButtonAndDivider();
        }
    }

    public long getTimeInMillis() {
        final int hoursInSeconds = mInput[5] * 36000 + mInput[4] * 3600;
        final int minutesInSeconds = mInput[3] * 600 + mInput[2] * 60;
        final int seconds = mInput[1] * 10 + mInput[0];
        final int totalSeconds = hoursInSeconds + minutesInSeconds + seconds;

        return totalSeconds * DateUtils.SECOND_IN_MILLIS;
    }

    /**
     * @return an opaque representation of the state of timer setup
     */
    public Serializable getState() {
        return Arrays.copyOf(mInput, mInput.length);
    }

    /**
     * @param state an opaque state of this view previously produced by {@link #getState()}
     */
    public void setState(Serializable state) {
        final int[] input = (int[]) state;
        if (input != null && mInput.length == input.length) {
            for (int i = 0; i < mInput.length; i++) {
                mInput[i] = input[i];
                if (mInput[i] != 0) {
                    mInputPointer = i;
                }
            }
            updateTime();
            updateDeleteButtonAndDivider();
        }
    }

    protected boolean hasValidInput() {
        return mInputPointer != -1;
    }

    private void updateTime() {
        final int seconds = mInput[1] * 10 + mInput[0];
        mEnteredTime.setTime(mInput[5], mInput[4], mInput[3], mInput[2], seconds);
    }

    private void updateDeleteButtonAndDivider() {
        final boolean enabled = hasValidInput();
        mDelete.setEnabled(enabled);
        mDivider.setBackgroundColor(enabled ? mColorAccent : mColorHairline);
    }

    private void updateFab() {
        mFabContainer.updateFab(FAB_ONLY_ANIMATED);
    }
}

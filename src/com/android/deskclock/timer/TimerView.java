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
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.R;


public class TimerView extends LinearLayout {
	TextView mHours, mMinutes, mSeconds, mHunderdths;
	TextView mHoursLabel, mMinutesLabel, mSecondsLabel;
	boolean mShowTimeStr = true;
	Typeface mRobotoThin;

    Runnable mBlinkThread = new Runnable() {
        @Override
        public void run() {
            mShowTimeStr = !mShowTimeStr;
            TimerView.this.setVisibility(mShowTimeStr ? View.VISIBLE : View.INVISIBLE);
            postDelayed(mBlinkThread, 1000);
        }

    };


    public TimerView(Context context) {
        this(context, null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRobotoThin = Typeface.createFromAsset(context.getAssets(),"fonts/Roboto-Thin.ttf");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

    	mHours = (TextView)findViewById(R.id.hours);
    	mMinutes = (TextView)findViewById(R.id.minutes);
    	mSeconds = (TextView)findViewById(R.id.seconds);
    	mSeconds.setTypeface(mRobotoThin);
    	mHunderdths = (TextView)findViewById(R.id.hundreds_seconds);
    	mHoursLabel = (TextView)findViewById(R.id.hours_label);
    }

    public void setTime(long time) {
        if (time < 0) {
            time = 0;
        }
        long hundreds, seconds, minutes, hours;
        seconds = time / 100;
        hundreds = (time - seconds * 100);
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 99) {
            hours = 0;
        }
        // TODO: must build to account for localization
		if (hours >= 10) {
			mHours.setText(String.format("%02d",hours));
			mHours.setVisibility(View.VISIBLE);
			mHoursLabel.setVisibility(View.VISIBLE);
		} else if (hours > 0) {
			mHours.setText(String.format("%01d",hours));
			mHours.setVisibility(View.VISIBLE);
			mHoursLabel.setVisibility(View.VISIBLE);
		} else {
			mHours.setVisibility(View.GONE);
			mHoursLabel.setVisibility(View.GONE);
		}

		if (minutes >= 10) {
			mMinutes.setText(String.format("%02d",minutes));
		} else {
			mMinutes.setText(String.format("%01d",minutes));
		}

		mSeconds.setText(String.format("%02d",seconds));
		mHunderdths.setText(String.format("%02d",hundreds));
    }

    public void setTime(String hours, String minutes, String seconds, String hundreds) {
        if (hours != null) {
            mHours.setText(hours);
        } else {
            mHours.setText("0");
        }
        if (minutes != null) {
            mMinutes.setText(minutes);
        } else {
            mMinutes.setText("00");
        }
        if (seconds != null) {
            mSeconds.setText(seconds);
        } else {
            mSeconds.setText("00");
        }
        if (hundreds != null) {
            mHunderdths.setText(hundreds);
            mHunderdths.setVisibility(View.VISIBLE);
        } else {
            mHunderdths.setVisibility(View.GONE);
        }
    }

    public void blinkTimeStr(boolean blink) {
        if (blink) {
            postDelayed(mBlinkThread, 1000);
        } else {
            removeCallbacks(mBlinkThread);
            mShowTimeStr = true;
            this.setVisibility(View.VISIBLE);
        }
    }

    public String getTimeString() {
        return String.format("%s:%s:%s.%s",mHours.getText(), mMinutes.getText(), mSeconds.getText(),
                mHunderdths.getText());
    }

}

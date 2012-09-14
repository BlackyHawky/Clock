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

package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Displays the time
 */
public class OldDigitalClock extends RelativeLayout {

    private final static String M12 = "h:mm";

    private Calendar mCalendar;
    private String mFormat;
    private AndroidClockTextView mTimeDisplay;
    private AmPm mAmPm;
    private ContentObserver mFormatChangeObserver;
    private boolean mLive = true;
    private boolean mAttached;

    /* called by system on minute ticks */
    private final Handler mHandler = new Handler();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mLive && intent.getAction().equals(
                            Intent.ACTION_TIMEZONE_CHANGED)) {
                    mCalendar = Calendar.getInstance();
                }
                // Post a runnable to avoid blocking the broadcast.
                mHandler.post(new Runnable() {
                        public void run() {
                            updateTime();
                        }
                });
            }
        };

    static class AmPm {
        private final AndroidClockTextView mAmPm;
        private final String mAmString, mPmString;

        AmPm(View parent) {
            mAmPm = (AndroidClockTextView) parent.findViewById(R.id.am_pm);

            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            mAmString = ampm[0];
            mPmString = ampm[1];
        }

        void setShowAmPm(boolean show) {
            mAmPm.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }

        void setIsMorning(boolean isMorning) {
            mAmPm.setText(isMorning ? mAmString : mPmString);
        }
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }
        @Override
        public void onChange(boolean selfChange) {
            setDateFormat();
            updateTime();
        }
    }

    public OldDigitalClock(Context context) {
        this(context, null);
    }

    public OldDigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTimeDisplay = (AndroidClockTextView) findViewById(R.id.timeDisplay);
        mAmPm = new AmPm(this);
        mCalendar = Calendar.getInstance();

        setDateFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (Log.LOGV) Log.v("onAttachedToWindow " + this);

        if (mAttached) return;
        mAttached = true;

        if (mLive) {
            /* monitor time ticks, time changed, timezone */
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getContext().registerReceiver(mIntentReceiver, filter);
        }

        /* monitor 12/24-hour display preference */
        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);

        updateTime();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (!mAttached) return;
        mAttached = false;

        if (mLive) {
            getContext().unregisterReceiver(mIntentReceiver);
        }
        getContext().getContentResolver().unregisterContentObserver(
                mFormatChangeObserver);
    }


    void updateTime(Calendar c) {
        mCalendar = c;
        updateTime();
    }

    private void updateTime() {
        if (mLive) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }

        CharSequence newTime = DateFormat.format(mFormat, mCalendar);
        mTimeDisplay.setText(newTime);
        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);
    }

    private void setDateFormat() {
        mFormat = Alarms.get24HourMode(getContext()) ? Alarms.M24 : M12;
        mAmPm.setShowAmPm(mFormat == M12);
    }

    void setLive(boolean live) {
        mLive = live;
    }
}

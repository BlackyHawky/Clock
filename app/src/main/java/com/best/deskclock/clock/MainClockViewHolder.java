/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.AutoSizingTextClock;
import com.best.deskclock.utils.ClockUtils;

import java.util.List;

public class MainClockViewHolder extends RecyclerView.ViewHolder {

    private final SharedPreferences mPrefs;
    private final DisplayMetrics mDisplayMetrics;
    private final View mMainClockContainer;
    private final View mEmptyCityView;
    private final TextView mDate;
    private final TextView mNextAlarmIcon;
    private final TextView mNextAlarm;
    private final AutoSizingTextClock mDigitalClock;
    private final AnalogClock mAnalogClock;
    private final DataModel.ClockStyle mClockStyle;
    private final boolean mAreClockSecondsDisplayed;
    private final Typeface mDigitalClockTypeface;
    private final float mDigitalClockFontSize;

    public MainClockViewHolder(View itemView, SharedPreferences prefs, DisplayMetrics displayMetrics, DataModel.ClockStyle clockStyle,
                               Typeface digitalClockTypeface, float digitalClockFontSize, Typeface boldTypeface, Typeface alarmIconTypeface,
                               boolean areClockSecondsDisplayed) {

        super(itemView);

        mPrefs = prefs;
        mDisplayMetrics = displayMetrics;
        mMainClockContainer = itemView.findViewById(R.id.main_clock_container);
        mDate = itemView.findViewById(R.id.date);
        mNextAlarmIcon = itemView.findViewById(R.id.nextAlarmIcon);
        mNextAlarm = itemView.findViewById(R.id.nextAlarm);
        mEmptyCityView = itemView.findViewById(R.id.cities_empty_view);
        mDigitalClock = itemView.findViewById(R.id.digital_clock);
        mAnalogClock = itemView.findViewById(R.id.analog_clock);
        mClockStyle = clockStyle;
        mAreClockSecondsDisplayed = areClockSecondsDisplayed;
        mDigitalClockTypeface = digitalClockTypeface;
        mDigitalClockFontSize = digitalClockFontSize;

        if (mDate != null) {
            mDate.setTypeface(boldTypeface);
        }
        if (mNextAlarmIcon != null) {
            mNextAlarmIcon.setTypeface(alarmIconTypeface);
        }
        if (mNextAlarm != null) {
            mNextAlarm.setTypeface(boldTypeface);
        }
    }

    public void bind(Context context, List<City> selectedCities, boolean showHomeClock, boolean isPortrait, boolean isTextUppercase,
                     String formattedDate, String dateDescription, String formattedNextAlarm) {

        ViewGroup.LayoutParams mainClockParams = mMainClockContainer.getLayoutParams();

        if (isPortrait) {
            if (selectedCities.isEmpty() && !showHomeClock) {
                mainClockParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mMainClockContainer.setPadding(0, 0, 0, 0);

                mEmptyCityView.setVisibility(View.VISIBLE);
            } else {
                mainClockParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mMainClockContainer.setPadding(0, 0, 0, (int) dpToPx(20, mDisplayMetrics));
                mEmptyCityView.setVisibility(View.GONE);
            }

            mMainClockContainer.setLayoutParams(mainClockParams);
        } else {
            mEmptyCityView.setVisibility(View.GONE);
        }

        ClockUtils.setClockStyle(mClockStyle, mDigitalClock, mAnalogClock);
        if (mClockStyle == DataModel.ClockStyle.DIGITAL) {
            mDigitalClock.setTypeface(mDigitalClockTypeface);
            ClockUtils.setDigitalClockTimeFormat(
                mDigitalClock, 0.4f, mAreClockSecondsDisplayed, false, true, false);
            mDigitalClock.applyUserPreferredTextSizeSp(mDigitalClockFontSize);
        } else {
            ClockUtils.adjustAnalogClockSize(mAnalogClock, mPrefs, false, true, false);
            ClockUtils.setAnalogClockSecondsEnabled(mClockStyle, mAnalogClock, mAreClockSecondsDisplayed);
        }

        if (mDate != null) {
            mDate.setAllCaps(isTextUppercase);
            mDate.setText(formattedDate);
            mDate.setContentDescription(dateDescription);
            mDate.setVisibility(View.VISIBLE);
        }

        if (mNextAlarm != null && mNextAlarmIcon != null) {
            if (TextUtils.isEmpty(formattedNextAlarm)) {
                mNextAlarm.setVisibility(View.GONE);
                mNextAlarmIcon.setVisibility(View.GONE);
            } else {
                String description = context.getString(R.string.next_alarm_description, formattedNextAlarm);
                mNextAlarm.setAllCaps(isTextUppercase);
                mNextAlarm.setText(formattedNextAlarm);
                mNextAlarm.setContentDescription(description);
                mNextAlarm.setVisibility(View.VISIBLE);

                mNextAlarmIcon.setContentDescription(description);
                mNextAlarmIcon.setVisibility(View.VISIBLE);
            }
        }
    }

}

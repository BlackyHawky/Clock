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

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.databinding.MainClockFrameBinding;
import com.best.deskclock.utils.ClockUtils;

import java.util.List;

public class MainClockViewHolder extends RecyclerView.ViewHolder {

    private final MainClockFrameBinding mBinding;
    private final SharedPreferences mPrefs;
    private final DisplayMetrics mDisplayMetrics;
    private final DataModel.ClockStyle mClockStyle;
    private final boolean mAreClockSecondsDisplayed;
    private final Typeface mDigitalClockTypeface;
    private final float mDigitalClockFontSize;

    public MainClockViewHolder(MainClockFrameBinding binding, SharedPreferences prefs, DisplayMetrics displayMetrics,
                               DataModel.ClockStyle clockStyle, Typeface digitalClockTypeface, float digitalClockFontSize,
                               Typeface boldTypeface, Typeface alarmIconTypeface, boolean areClockSecondsDisplayed) {

        super(binding.getRoot());

        mBinding = binding;
        mPrefs = prefs;
        mDisplayMetrics = displayMetrics;
        mClockStyle = clockStyle;
        mAreClockSecondsDisplayed = areClockSecondsDisplayed;
        mDigitalClockTypeface = digitalClockTypeface;
        mDigitalClockFontSize = digitalClockFontSize;

        mBinding.dateAndNextAlarmTime.date.setTypeface(boldTypeface);

        mBinding.dateAndNextAlarmTime.nextAlarmIcon.setTypeface(alarmIconTypeface);

        mBinding.dateAndNextAlarmTime.nextAlarm.setTypeface(boldTypeface);
    }

    public void bind(Context context, List<City> selectedCities, boolean showHomeClock, boolean isPortrait, boolean isTextUppercase,
                     String formattedDate, String dateDescription, String formattedNextAlarm) {

        ViewGroup.LayoutParams mainClockParams = mBinding.mainClockContainer.getLayoutParams();

        if (isPortrait) {
            if (selectedCities.isEmpty() && !showHomeClock) {
                mainClockParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mBinding.mainClockContainer.setPadding(0, 0, 0, 0);

                mBinding.citiesEmptyView.setVisibility(View.VISIBLE);
            } else {
                mainClockParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mBinding.mainClockContainer.setPadding(0, 0, 0, (int) dpToPx(20, mDisplayMetrics));
                mBinding.citiesEmptyView.setVisibility(View.GONE);
            }

            mBinding.mainClockContainer.setLayoutParams(mainClockParams);
        } else {
            mBinding.citiesEmptyView.setVisibility(View.GONE);
        }

        ClockUtils.setClockStyle(mClockStyle, mBinding.digitalClock, mBinding.analogClock);
        if (mClockStyle == DataModel.ClockStyle.DIGITAL) {
            mBinding.digitalClock.setTypeface(mDigitalClockTypeface);
            ClockUtils.setDigitalClockTimeFormat(
                mBinding.digitalClock, 0.4f, mAreClockSecondsDisplayed, false, true, false);
            mBinding.digitalClock.applyUserPreferredTextSizeSp(mDigitalClockFontSize);
        } else {
            ClockUtils.adjustAnalogClockSize(mBinding.analogClock, mPrefs, false, true, false);
            ClockUtils.setAnalogClockSecondsEnabled(mClockStyle, mBinding.analogClock, mAreClockSecondsDisplayed);
        }

        mBinding.dateAndNextAlarmTime.date.setAllCaps(isTextUppercase);
        mBinding.dateAndNextAlarmTime.date.setText(formattedDate);
        mBinding.dateAndNextAlarmTime.date.setContentDescription(dateDescription);
        mBinding.dateAndNextAlarmTime.date.setVisibility(View.VISIBLE);

        if (TextUtils.isEmpty(formattedNextAlarm)) {
            mBinding.dateAndNextAlarmTime.nextAlarm.setVisibility(View.GONE);
            mBinding.dateAndNextAlarmTime.nextAlarmIcon.setVisibility(View.GONE);
        } else {
            String description = context.getString(R.string.next_alarm_description, formattedNextAlarm);
            mBinding.dateAndNextAlarmTime.nextAlarm.setAllCaps(isTextUppercase);
            mBinding.dateAndNextAlarmTime.nextAlarm.setText(formattedNextAlarm);
            mBinding.dateAndNextAlarmTime.nextAlarm.setContentDescription(description);
            mBinding.dateAndNextAlarmTime.nextAlarm.setVisibility(View.VISIBLE);

            mBinding.dateAndNextAlarmTime.nextAlarmIcon.setContentDescription(description);
            mBinding.dateAndNextAlarmTime.nextAlarmIcon.setVisibility(View.VISIBLE);
        }
    }

}

/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.AutoSizingTextClock;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;

import java.util.List;

public class MainClockViewHolder extends RecyclerView.ViewHolder {

    private final SharedPreferences mPrefs;
    private final View mMainClockContainer;
    private final View mEmptyCityView;
    private final AutoSizingTextClock mDigitalClock;
    private final AnalogClock mAnalogClock;
    private final DataModel.ClockStyle mClockStyle;
    private final boolean mAreClockSecondsDisplayed;
    private final String mDigitalClockFontPath;
    private final float mDigitalClockFontSize;

    public MainClockViewHolder(View itemView) {
        super(itemView);

        mPrefs = getDefaultSharedPreferences(itemView.getContext());
        mMainClockContainer = itemView.findViewById(R.id.main_clock_container);
        mEmptyCityView = itemView.findViewById(R.id.cities_empty_view);
        mDigitalClock = itemView.findViewById(R.id.digital_clock);
        mAnalogClock = itemView.findViewById(R.id.analog_clock);
        mClockStyle = SettingsDAO.getClockStyle(mPrefs);
        mAreClockSecondsDisplayed = SettingsDAO.areClockSecondsDisplayed(mPrefs);
        mDigitalClockFontPath = SettingsDAO.getDigitalClockFont(mPrefs);
        mDigitalClockFontSize = SettingsDAO.getDigitalClockFontSize(mPrefs);
    }

    public void bind(Context context, String dateFormat, String dateFormatForAccessibility,
                      List<City> selectedCities, boolean showHomeClock, boolean isPortrait) {

        ViewGroup.LayoutParams mainClockparams = mMainClockContainer.getLayoutParams();

        if (isPortrait) {
            if (selectedCities.isEmpty() && !showHomeClock) {
                mainClockparams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mMainClockContainer.setPadding(0, 0, 0, 0);

                mEmptyCityView.setVisibility(View.VISIBLE);
            } else {
                mainClockparams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mMainClockContainer.setPadding(0, 0, 0,
                        (int) dpToPx(20, context.getResources().getDisplayMetrics()));
                mEmptyCityView.setVisibility(View.GONE);
            }

            mMainClockContainer.setLayoutParams(mainClockparams);
        } else {
            mEmptyCityView.setVisibility(View.GONE);
        }

        ClockUtils.setClockStyle(mClockStyle, mDigitalClock, mAnalogClock);
        if (mClockStyle == DataModel.ClockStyle.DIGITAL) {
            ClockUtils.setDigitalClockFont(mDigitalClock, mDigitalClockFontPath);
            ClockUtils.setDigitalClockTimeFormat(mDigitalClock, 0.4f, mAreClockSecondsDisplayed,
                    false, true, false);
            mDigitalClock.applyUserPreferredTextSizeSp(mDigitalClockFontSize);
        } else {
            ClockUtils.adjustAnalogClockSize(mAnalogClock, mPrefs, false, true, false);
            ClockUtils.setAnalogClockSecondsEnabled(mClockStyle, mAnalogClock, mAreClockSecondsDisplayed);
        }

        ClockUtils.updateDate(dateFormat, dateFormatForAccessibility, itemView);
        ClockUtils.applyBoldDateTypeface(itemView);
        ClockUtils.setClockIconTypeface(itemView);
        AlarmUtils.refreshAlarm(itemView, false);
        AlarmUtils.applyBoldNextAlarmTypeface(itemView);
    }

}

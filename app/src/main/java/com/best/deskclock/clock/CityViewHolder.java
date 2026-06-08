/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.util.Calendar.DAY_OF_WEEK;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.databinding.WorldClockItemBinding;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.Calendar;
import java.util.TimeZone;

public class CityViewHolder extends RecyclerView.ViewHolder {

    private final WorldClockItemBinding mBinding;
    private ClockSettings mSettings;
    private final Context mContext;
    private final SelectedCitiesAdapter mAdapter;
    private boolean mIsDigitalClock;

    public CityViewHolder(WorldClockItemBinding binding, SelectedCitiesAdapter adapter, Typeface regularTypeface, Typeface boldTypeface,
                          boolean hasBlackAccentColor) {

        super(binding.getRoot());

        mBinding = binding;
        mContext = binding.getRoot().getContext();
        mAdapter = adapter;

        mBinding.worldClockCityContainer.cityName.setTypeface(boldTypeface);
        // Allow text scrolling by clicking on the item (all other attributes are indicated
        // in the "world_clock_city_container.xml" file)
        mBinding.worldClockCityContainer.cityName.setSelected(true);

        mBinding.worldClockCityContainer.hoursAhead.setTypeface(regularTypeface);
        mBinding.worldClockCityContainer.cityNote.setTypeface(regularTypeface);

        if (hasBlackAccentColor) {
            mBinding.digitalClock.setTextColor(Color.WHITE);
        }
    }

    public void applySettings(ClockSettings settings) {
        mSettings = settings;
        mIsDigitalClock = settings.clockStyle == DataModel.ClockStyle.DIGITAL;

        if (mIsDigitalClock) {
            mBinding.analogClock.setVisibility(View.GONE);

            mBinding.digitalClock.setBackground(ThemeUtils.pillBackgroundFromAttr(mContext, com.google.android.material.R.attr.colorSecondary));
            mBinding.digitalClock.setTypeface(settings.digitalClockTypeface);
            ClockUtils.setDigitalClockTimeFormat(mBinding.digitalClock, 0.3f, false, false, true, false);

            mBinding.digitalClock.setVisibility(View.VISIBLE);
        } else {
            mBinding.digitalClock.setVisibility(View.GONE);
            mBinding.analogClock.setVisibility(View.VISIBLE);
            mBinding.analogClock.enableSeconds(false);
            mBinding.analogClock.updateClockStyle();
        }

    }

    public void bind(City city) {
        final String cityTimeZoneId = city.getTimeZone().getID();

        updateBackground();

        // Configure the digital clock or analog clock depending on the user preference.
        if (mIsDigitalClock) {
            mBinding.digitalClock.setTimeZone(cityTimeZoneId);
        } else {
            mBinding.analogClock.setTimeZone(cityTimeZoneId);
        }

        // Bind the city name.
        mBinding.worldClockCityContainer.cityName.setText(city.getName());

        // Compute if the city week day matches the weekday of the current timezone.
        final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
        final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
        final boolean displayDayOfWeek = localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

        // Compare offset from UTC time on today's date (daylight savings time, etc.)
        final TimeZone currentTimeZone = TimeZone.getDefault();
        final TimeZone cityTimeZone = TimeZone.getTimeZone(cityTimeZoneId);
        final long currentTimeMillis = System.currentTimeMillis();
        final long currentUtcOffset = currentTimeZone.getOffset(currentTimeMillis);
        final long cityUtcOffset = cityTimeZone.getOffset(currentTimeMillis);
        final long offsetDelta = cityUtcOffset - currentUtcOffset;

        final int hoursDifferent = (int) (offsetDelta / DateUtils.HOUR_IN_MILLIS);
        final int minutesDifferent = (int) (offsetDelta / DateUtils.MINUTE_IN_MILLIS) % 60;
        final boolean displayMinutes = offsetDelta % DateUtils.HOUR_IN_MILLIS != 0;
        final boolean isAhead = hoursDifferent > 0 || (hoursDifferent == 0 && minutesDifferent > 0);
        final boolean displayDifference = hoursDifferent != 0 || displayMinutes;

        mBinding.worldClockCityContainer.hoursAhead.setVisibility(displayDifference ? VISIBLE : GONE);
        final String timeString = createHoursDifferentString(mContext, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
        mBinding.worldClockCityContainer.hoursAhead.setText(displayDayOfWeek
            ? (mContext.getString(isAhead
            ? R.string.world_hours_tomorrow
            : R.string.world_hours_yesterday, timeString))
            : timeString);

        if (mSettings.isCityNoteEnabled) {
            String note = mAdapter.getCityNote(city.getId());
            if (note != null && !note.trim().isEmpty()) {
                mBinding.worldClockCityContainer.cityNote.setText(note.trim());
                mBinding.worldClockCityContainer.cityNote.setVisibility(VISIBLE);
            } else {
                mBinding.worldClockCityContainer.cityNote.setVisibility(GONE);
            }

            mBinding.getRoot().setOnClickListener(v -> {
                LabelDialogFragment labelDialogFragment = LabelDialogFragment.newInstance(city.getId(), city.getName(), note);

                LabelDialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), labelDialogFragment);
            });
        } else {
            mBinding.getRoot().setOnClickListener(null);
            mBinding.worldClockCityContainer.cityNote.setVisibility(View.GONE);
        }
    }

    public void updateBackground() {
        int cityPosition = getBindingAdapterPosition();

        if (cityPosition == RecyclerView.NO_POSITION || mAdapter == null) {
            return;
        }

        int totalCities = mAdapter.getItemCount();

        if (cityPosition >= 0) {
            Drawable.ConstantState bgState;

            if (totalCities <= 1) {
                bgState = mAdapter.getBgSingle();
            } else if (cityPosition == 0) {
                bgState = mAdapter.getBgTop();
            } else if (cityPosition == totalCities - 1) {
                bgState = mAdapter.getBgBottom();
            } else {
                bgState = mAdapter.getBgMiddle();
            }

            if (bgState != null) {
                mBinding.getRoot().setBackground(bgState.newDrawable());
            }
        }
    }

    /**
     * @param context          to obtain strings.
     * @param displayMinutes   whether minutes should be included
     * @param isAhead          {@code true} if the time should be marked 'ahead', else 'behind'
     * @param hoursDifferent   the number of hours the time is ahead/behind
     * @param minutesDifferent the number of minutes the time is ahead/behind
     * @return String describing the hours/minutes ahead or behind
     */
    public static String createHoursDifferentString(Context context, boolean displayMinutes, boolean isAhead, int hoursDifferent,
                                                    int minutesDifferent) {

        String timeString;
        if (displayMinutes && hoursDifferent != 0) {
            // Both minutes and hours
            final String hoursShortQuantityString = FormattedTextUtils.getNumberFormattedQuantityString(
                context, R.plurals.hours_short, Math.abs(hoursDifferent));
            final String minsShortQuantityString = FormattedTextUtils.getNumberFormattedQuantityString(
                context, R.plurals.minutes_short, Math.abs(minutesDifferent));
            final @StringRes int stringType = isAhead ? R.string.world_hours_minutes_ahead : R.string.world_hours_minutes_behind;
            timeString = context.getString(stringType, hoursShortQuantityString, minsShortQuantityString);
        } else {
            // Minutes alone or hours alone
            final String hoursQuantityString = FormattedTextUtils.getNumberFormattedQuantityString(
                context, R.plurals.hours, Math.abs(hoursDifferent));
            final String minutesQuantityString = FormattedTextUtils.getNumberFormattedQuantityString(
                context, R.plurals.minutes, Math.abs(minutesDifferent));
            final @StringRes int stringType = isAhead ? R.string.world_time_ahead : R.string.world_time_behind;
            timeString = context.getString(stringType, displayMinutes ? minutesQuantityString : hoursQuantityString);
        }
        return timeString;
    }

}

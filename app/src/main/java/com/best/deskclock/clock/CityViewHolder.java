/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static java.util.Calendar.DAY_OF_WEEK;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.Calendar;
import java.util.TimeZone;

public class CityViewHolder extends RecyclerView.ViewHolder {

    private final Context mContext;
    private final SelectedCitiesAdapter mAdapter;
    private final TextView mName;
    private final TextView mCityNoteView;
    private final TextClock mDigitalClock;
    private final AnalogClock mAnalogClock;
    private final TextView mHoursAhead;
    private final boolean mIsPortrait;
    private final boolean mIsCityNoteEnabled;
    private final boolean mIsDigitalClock;

    public CityViewHolder(View itemView, SelectedCitiesAdapter adapter) {
        super(itemView);

        mContext = itemView.getContext();
        mAdapter = adapter;
        SharedPreferences prefs = getDefaultSharedPreferences(mContext);
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        String fontPath = SettingsDAO.getGeneralFont(prefs);
        Typeface regularTypeface = ThemeUtils.loadFont(fontPath);
        boolean isTablet = ThemeUtils.isTablet();
        mIsPortrait = ThemeUtils.isPortrait();
        mIsCityNoteEnabled = SettingsDAO.isCityNoteEnabled(prefs);
        mIsDigitalClock = SettingsDAO.getClockStyle(prefs) == DataModel.ClockStyle.DIGITAL;

        mName = itemView.findViewById(R.id.city_name);
        mHoursAhead = itemView.findViewById(R.id.hours_ahead);
        mCityNoteView = itemView.findViewById(R.id.city_note);
        mDigitalClock = itemView.findViewById(R.id.digital_clock);
        mAnalogClock = itemView.findViewById(R.id.analog_clock);

        int paddingVertical = (int) dpToPx(mIsDigitalClock ? 18 : 12, displayMetrics);
        itemView.setPadding(itemView.getPaddingLeft(), paddingVertical, itemView.getPaddingRight(), paddingVertical);

        mName.setTypeface(ThemeUtils.boldTypeface(fontPath));
        // Allow text scrolling by clicking on the item (all other attributes are indicated
        // in the "world_clock_city_container.xml" file)
        mName.setSelected(true);

        mHoursAhead.setTypeface(regularTypeface);
        mCityNoteView.setTypeface(regularTypeface);

        if (mIsDigitalClock) {
            mAnalogClock.setVisibility(View.GONE);

            mDigitalClock.setBackground(ThemeUtils.pillBackgroundFromAttr(mContext, com.google.android.material.R.attr.colorSecondary));
            ClockUtils.setDigitalClockFont(mDigitalClock, SettingsDAO.getDigitalClockFont(prefs));
            ClockUtils.setDigitalClockTimeFormat(mDigitalClock, 0.3f, false, false, true, false);

            if (SettingsDAO.getAccentColor(prefs).equals(BLACK_ACCENT_COLOR)) {
                mDigitalClock.setTextColor(Color.WHITE);
            }

            mDigitalClock.setVisibility(View.VISIBLE);
        } else {
            mDigitalClock.setVisibility(View.GONE);
            mAnalogClock.setVisibility(View.VISIBLE);

            mAnalogClock.getLayoutParams().height = (int) dpToPx(isTablet ? 150 : 80, displayMetrics);
            mAnalogClock.getLayoutParams().width = (int) dpToPx(isTablet ? 150 : 80, displayMetrics);
            mAnalogClock.enableSeconds(false);
        }
    }

    public void bind(City city) {
        final String cityTimeZoneId = city.getTimeZone().getID();

        updateBackground();

        // Configure the digital clock or analog clock depending on the user preference.
        if (mIsDigitalClock) {
            mDigitalClock.setTimeZone(cityTimeZoneId);
        } else {
            mAnalogClock.setTimeZone(cityTimeZoneId);
        }

        // Bind the city name.
        mName.setText(city.getName());

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

        mHoursAhead.setVisibility(displayDifference ? VISIBLE : GONE);
        final String timeString = createHoursDifferentString(mContext, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
        mHoursAhead.setText(displayDayOfWeek
            ? (mContext.getString(isAhead
            ? R.string.world_hours_tomorrow
            : R.string.world_hours_yesterday, timeString))
            : timeString);

        if (mIsCityNoteEnabled) {
            String note = mAdapter.getCityNote(city.getId());
            if (note != null && !note.trim().isEmpty()) {
                mCityNoteView.setVisibility(VISIBLE);
                mCityNoteView.setText(note.trim());
            } else {
                mCityNoteView.setVisibility(GONE);
            }

            itemView.setOnClickListener(v -> {
                LabelDialogFragment labelDialogFragment = LabelDialogFragment.newInstance(city.getId(), city.getName(), note);

                LabelDialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), labelDialogFragment);
            });
        } else {
            itemView.setOnClickListener(null);
            mCityNoteView.setVisibility(View.GONE);
        }
    }

    public void updateBackground() {
        int absolutePosition = getBindingAdapterPosition();

        if (absolutePosition == RecyclerView.NO_POSITION || mAdapter == null) {
            return;
        }

        int mainClockCount = mIsPortrait ? 1 : 0;

        int cityPosition = absolutePosition - mainClockCount;
        int totalCities = mAdapter.getItemCount() - mainClockCount;

        if (cityPosition >= 0) {
            itemView.setBackground(ThemeUtils.expressiveCardBackground(mContext, cityPosition, totalCities));
        }
    }

    /**
     * @param context          to obtain strings.
     * @param displayMinutes   whether or not minutes should be included
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

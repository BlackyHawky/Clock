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
import static com.best.deskclock.uidata.UiDataModel.Tab.CLOCKS;
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

    private final SharedPreferences mPrefs;
    private final Typeface mRegularTypeface;
    private final Typeface mBoldTypeface;
    private final SelectedCitiesAdapter mAdapter;
    private final TextView mName;
    private final DataModel.ClockStyle mClockStyle;
    private final TextClock mDigitalClock;
    private final AnalogClock mAnalogClock;
    private final String mDigitalClockFont;
    private final TextView mHoursAhead;
    private final boolean mIsTablet;

    public CityViewHolder(View itemView, SelectedCitiesAdapter adapter) {
        super(itemView);

        mPrefs = getDefaultSharedPreferences(itemView.getContext());
        String fontPath = SettingsDAO.getGeneralFont(mPrefs);
        mRegularTypeface = ThemeUtils.loadFont(fontPath);
        mBoldTypeface = ThemeUtils.boldTypeface(fontPath);
        mAdapter = adapter;
        mClockStyle = SettingsDAO.getClockStyle(mPrefs);
        mName = itemView.findViewById(R.id.city_name);
        mDigitalClock = itemView.findViewById(R.id.digital_clock);
        mAnalogClock = itemView.findViewById(R.id.analog_clock);
        mDigitalClockFont = SettingsDAO.getDigitalClockFont(mPrefs);
        mHoursAhead = itemView.findViewById(R.id.hours_ahead);
        mIsTablet = ThemeUtils.isTablet();
    }

    public void bind(Context context, City city, boolean isPortrait) {
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final String cityTimeZoneId = city.getTimeZone().getID();
        final boolean isDigitalClock = mClockStyle == DataModel.ClockStyle.DIGITAL;
        int paddingVertical = (int) dpToPx(isDigitalClock ? 18 : 12, displayMetrics);
        int absolutePosition = getBindingAdapterPosition();
        int mainClockCount = isPortrait ? 1 : 0;
        int cityPosition = absolutePosition != RecyclerView.NO_POSITION
                ? absolutePosition - mainClockCount
                : -1;
        int totalCities = mAdapter.getItemCount() - mainClockCount;

        if (cityPosition >= 0) {
            itemView.setBackground(ThemeUtils.expressiveCardBackground(context, cityPosition, totalCities));
        }

        itemView.setPadding(itemView.getPaddingLeft(), paddingVertical, itemView.getPaddingRight(), paddingVertical);

        // Configure the digital clock or analog clock depending on the user preference.
        if (isDigitalClock) {
            mAnalogClock.setVisibility(GONE);
            mDigitalClock.setBackground(ThemeUtils.pillBackgroundFromAttr(
                    context, com.google.android.material.R.attr.colorSecondary));
            ClockUtils.setDigitalClockFont(mDigitalClock, mDigitalClockFont);
            ClockUtils.setDigitalClockTimeFormat(mDigitalClock, 0.3f,
                    false, false, true, false);
            if (SettingsDAO.getAccentColor(mPrefs).equals(BLACK_ACCENT_COLOR)) {
                mDigitalClock.setTextColor(Color.WHITE);
            }
            mDigitalClock.setTimeZone(cityTimeZoneId);
            mDigitalClock.setVisibility(VISIBLE);
        } else {
            mDigitalClock.setVisibility(GONE);
            mAnalogClock.getLayoutParams().height = (int) dpToPx(mIsTablet ? 150 : 80, displayMetrics);
            mAnalogClock.getLayoutParams().width = (int) dpToPx(mIsTablet ? 150 : 80, displayMetrics);
            mAnalogClock.setVisibility(VISIBLE);
            mAnalogClock.setTimeZone(cityTimeZoneId);
            mAnalogClock.enableSeconds(false);
        }

        // Bind the city name.
        mName.setText(city.getName());
        mName.setTypeface(mBoldTypeface);

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
        final String timeString = createHoursDifferentString(
                context, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
        mHoursAhead.setText(displayDayOfWeek
                ? (context.getString(isAhead
                ? R.string.world_hours_tomorrow
                : R.string.world_hours_yesterday, timeString))
                : timeString);
        mHoursAhead.setTypeface(mRegularTypeface);

        // Allow text scrolling by clicking on the item (all other attributes are indicated
        // in the "world_clock_city_container.xml" file)
        mName.setSelected(true);

        TextView cityNoteView = itemView.findViewById(R.id.city_note);
        String note = mAdapter.getCityNote(city.getId());

        if (SettingsDAO.isCityNoteEnabled(mPrefs)) {
            if (note != null && !note.trim().isEmpty()) {
                cityNoteView.setVisibility(View.VISIBLE);
                cityNoteView.setText(note.trim());
                cityNoteView.setTypeface(mRegularTypeface);
            } else {
                cityNoteView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                LabelDialogFragment labelDialogFragment = LabelDialogFragment.newInstance(
                        city.getId(),
                        city.getName(),
                        note,
                        CLOCKS.name());

                LabelDialogFragment.show(
                        ((AppCompatActivity) context).getSupportFragmentManager(),
                        labelDialogFragment);
            });
        } else {
            cityNoteView.setVisibility(View.GONE);
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
    public static String createHoursDifferentString(Context context, boolean displayMinutes,
                                                    boolean isAhead, int hoursDifferent, int minutesDifferent) {

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

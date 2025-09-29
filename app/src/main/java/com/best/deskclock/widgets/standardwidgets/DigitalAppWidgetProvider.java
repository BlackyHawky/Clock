/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widgets.standardwidgets;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_CUSTOM_COLOR;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.widgets.AppWidgetCityService;
import com.best.deskclock.widgets.BaseDigitalAppWidgetProvider;
import com.best.deskclock.widgets.DigitalWidgetSizes;
import com.best.deskclock.utils.WidgetUtils;

/**
 * <p>This provider produces a widget resembling one of the formats below.</p>
 * <p>
 * If an alarm is scheduled to ring in the future:
 * <pre>
 *         12:59 AM
 * WED, FEB 3 ⏰ THU 9:30 AM
 * </pre>
 * <p>
 * If no alarm is scheduled to ring in the future:
 * <pre>
 *         12:59 AM
 *        WED, FEB 3
 * </pre>
 * <p>
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
public class DigitalAppWidgetProvider extends BaseDigitalAppWidgetProvider {

    @Override
    protected int getLayoutId() {
        return R.layout.standard_digital_widget;
    }

    @Override
    protected int getSizerLayoutId() {
        return R.layout.standard_digital_widget_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.digitalWidget;
    }

    @Override
    protected int getDateViewId() {
        return R.id.date;
    }

    @Override
    protected int getClockViewId() {
        return R.id.clock;
    }

    @Override
    protected int getClockHoursViewId() {
        return 0;
    }

    @Override
    protected int getClockMinutesViewId() {
        return 0;
    }

    @Override
    protected int getNextAlarmIconId() {
        return R.id.nextAlarmIcon;
    }

    @Override
    protected int getNextAlarmViewId() {
        return R.id.nextAlarm;
    }

    @Override
    protected int getNextAlarmTextViewId() {
        return 0;
    }

    @Override
    protected int getNextAlarmTitleViewId() {
        return 0;
    }

    @Override
    protected int getWorldCityListViewId() {
        return R.id.worldCityList;
    }

    @Override
    protected int getClockCustomViewId() {
        return 0;
    }

    @Override
    protected int getClockHoursCustomViewId() {
        return 0;
    }

    @Override
    protected int getClockMinutesCustomViewId() {
        return 0;
    }

    @Override
    protected int getDateCustomViewId() {
        return 0;
    }

    @Override
    protected int getNextAlarmIconCustomId() {
        return 0;
    }

    @Override
    protected int getNextAlarmCustomViewId() {
        return 0;
    }

    @Override
    protected int getNextAlarmTextCustomViewId() {
        return 0;
    }

    @Override
    protected int getNextAlarmTitleCustomViewId() {
        return 0;
    }

    @Override
    protected boolean areWorldCitiesDisplayed(SharedPreferences prefs) {
        return WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(prefs);
    }

    @Override
    protected boolean isHorizontalPaddingApplied(SharedPreferences prefs) {
        return WidgetDAO.isDigitalWidgetHorizontalPaddingApplied(prefs);
    }

    @Override
    protected int getMaxWidgetFontSize(SharedPreferences prefs) {
        return WidgetDAO.getDigitalWidgetMaxClockFontSize(prefs);
    }

    @Override
    protected float getFontScaleFactor() {
        return 5f;
    }

    @Override
    protected Class<?> getCityServiceClass() {
        return AppWidgetCityService.DigitalAppWidgetCityService.class;
    }

    @Override
    protected void bindDateClickAction(RemoteViews rv, SharedPreferences prefs, PendingIntent calendarPendingIntent) {
        rv.setOnClickPendingIntent(getDateViewId(), calendarPendingIntent);
    }

    @Override
    protected void configureClock(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (DataModel.getDataModel().is24HourFormat()) {
            rv.setCharSequence(getClockViewId(), "setFormat24Hour",
                    ClockUtils.get24ModeFormat(context, WidgetDAO.areSecondsDisplayedOnDigitalWidget(prefs)));
        } else {
            rv.setCharSequence(getClockViewId(), "setFormat12Hour",
                    ClockUtils.get12ModeFormat(context, WidgetUtils.getAmPmRatio(false, prefs),
                            WidgetDAO.areSecondsDisplayedOnDigitalWidget(prefs)));
        }

        int color = WidgetDAO.isDigitalWidgetDefaultClockColor(prefs)
                ? DEFAULT_WIDGETS_CUSTOM_COLOR
                : WidgetDAO.getDigitalWidgetCustomClockColor(prefs);
        rv.setTextColor(getClockViewId(), color);
    }

    @Override
    protected void configureDate(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isDateDisplayedOnDigitalWidget(prefs)) {
            rv.setViewVisibility(getDateViewId(), VISIBLE);
            rv.setTextViewText(getDateViewId(), WidgetUtils.getDateFormat(context));

            int color = WidgetDAO.isDigitalWidgetDefaultDateColor(prefs)
                    ? DEFAULT_WIDGETS_CUSTOM_COLOR
                    : WidgetDAO.getDigitalWidgetCustomDateColor(prefs);
            rv.setTextColor(getDateViewId(), color);
        } else {
            rv.setViewVisibility(getDateViewId(), GONE);
        }
    }

    @Override
    protected void configureNextAlarm(RemoteViews rv, Context context, SharedPreferences prefs, String nextAlarmTime) {
        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(prefs)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
        } else {
            rv.setTextViewText(getNextAlarmViewId(), nextAlarmTime);
            rv.setViewVisibility(getNextAlarmViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmIconId(), VISIBLE);

            int color = WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(prefs)
                    ? DEFAULT_WIDGETS_CUSTOM_COLOR
                    : WidgetDAO.getDigitalWidgetCustomNextAlarmColor(prefs);
            rv.setTextColor(getNextAlarmViewId(), color);
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime,
                                           String nextAlarmTitle) {
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs) {
        int color = WidgetDAO.isBackgroundDisplayedOnDigitalWidget(prefs)
                ? WidgetDAO.getDigitalWidgetBackgroundColor(prefs)
                : Color.TRANSPARENT;
        rv.setInt(R.id.digitalWidgetBackground, "setBackgroundColor", color);
    }

    @Override
    protected void configureSizerClock(View sizer, SharedPreferences prefs) {
    }

    @Override
    protected void configureSizerDate(View sizer, Context context, SharedPreferences prefs) {
        final TextView date = sizer.findViewById(getDateViewId());

        if (WidgetDAO.isDateDisplayedOnDigitalWidget(prefs)) {
            date.setVisibility(VISIBLE);
            date.setText(WidgetUtils.getDateFormat(context));
        } else {
            date.setVisibility(GONE);
        }
    }

    @Override
    protected void configureSizerNextAlarm(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
        final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());

        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(prefs)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
        } else {
            nextAlarm.setText(nextAlarmTime);
            nextAlarm.setVisibility(VISIBLE);
            nextAlarmIcon.setVisibility(VISIBLE);
            nextAlarmIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));

            if (WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(prefs)) {
                nextAlarmIcon.setTextColor(DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                nextAlarmIcon.setTextColor(WidgetDAO.getDigitalWidgetCustomNextAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
    }

    @Override
    protected void configureClockForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        TextClock clock = sizer.findViewById(getClockViewId());
        clock.setText(WidgetUtils.getLongestTimeString(clock, false));
        clock.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
    }

    @Override
    protected void configureDateForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        TextView date = sizer.findViewById(getDateViewId());
        date.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
    }

    @Override
    protected void configureNextAlarmForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
        TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());

        nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
        nextAlarmIcon.setPadding(measuredSizes.mIconPaddingPx, 0, measuredSizes.mIconPaddingPx, 0);
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        TextClock clock = sizer.findViewById(getClockViewId());
        TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());

        measuredSizes.mMeasuredTextWidthPx = clock.getMeasuredWidth();
        measuredSizes.mMeasuredTextHeightPx = clock.getMeasuredHeight();

        if (nextAlarmIcon.getVisibility() == VISIBLE) {
            measuredSizes.mIconBitmap = ThemeUtils.createBitmap(nextAlarmIcon);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new DigitalAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

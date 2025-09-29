// SPDX-License-Identifier: GPL-3.0-only

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
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.BaseDigitalAppWidgetProvider;
import com.best.deskclock.widgets.DigitalWidgetSizes;

/**
 * <p>This provider produces a widget resembling one of the formats below.</p>
 * <p>
 * If an alarm is scheduled to ring in the future:
 * <pre>
 *        WED, FEB 3
 *           12
 *           59
 *      ⏰ THU 9:30 AM
 * </pre>
 * <p>
 * If no alarm is scheduled to ring in the future:
 * <pre>
 *        WED, FEB 3
 *           12
 *           59
 * </pre>
 * <p>
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
public class VerticalDigitalAppWidgetProvider extends BaseDigitalAppWidgetProvider {

    @Override
    protected int getLayoutId() {
        return R.layout.standard_vertical_digital_widget;
    }

    @Override
    protected int getSizerLayoutId() {
        return R.layout.standard_vertical_digital_widget_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.verticalDigitalWidget;
    }

    @Override
    protected int getDateViewId() {
        return R.id.date;
    }

    @Override
    protected int getClockViewId() {
        return 0;
    }

    @Override
    protected int getClockHoursViewId() {
        return R.id.clockHours;
    }

    @Override
    protected int getClockMinutesViewId() {
        return R.id.clockMinutes;
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
        return 0;
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
        return false;
    }

    @Override
    protected boolean isHorizontalPaddingApplied(SharedPreferences prefs) {
        return WidgetDAO.isVerticalDigitalWidgetHorizontalPaddingApplied(prefs);
    }

    @Override
    protected int getMaxWidgetFontSize(SharedPreferences prefs) {
        return WidgetDAO.getVerticalDigitalWidgetMaxClockFontSize(prefs);
    }

    @Override
    protected float getFontScaleFactor() {
        return 5f;
    }

    @Override
    protected Class<?> getCityServiceClass() {
        return null;
    }

    @Override
    protected void bindDateClickAction(RemoteViews rv, SharedPreferences prefs, PendingIntent calendarPendingIntent) {
        rv.setOnClickPendingIntent(getDateViewId(), calendarPendingIntent);
    }

    @Override
    protected void configureClock(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isVerticalDigitalWidgetDefaultHoursColor(prefs)) {
            rv.setTextColor(getClockHoursViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
        } else {
            rv.setTextColor(getClockHoursViewId(), WidgetDAO.getVerticalDigitalWidgetCustomHoursColor(prefs));
        }

        if (WidgetDAO.isVerticalDigitalWidgetDefaultMinutesColor(prefs)) {
            rv.setTextColor(getClockMinutesViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
        } else {
            rv.setTextColor(getClockMinutesViewId(), WidgetDAO.getVerticalDigitalWidgetCustomMinutesColor(prefs));
        }
    }

    @Override
    protected void configureDate(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isDateDisplayedOnVerticalDigitalWidget(prefs)) {
            rv.setViewVisibility(getDateViewId(), VISIBLE);
            rv.setTextViewText(getDateViewId(), WidgetUtils.getDateFormat(context));

            if (WidgetDAO.isVerticalDigitalWidgetDefaultDateColor(prefs)) {
                rv.setTextColor(getDateViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                rv.setTextColor(getDateViewId(), WidgetDAO.getVerticalDigitalWidgetCustomDateColor(prefs));
            }
        } else {
            rv.setViewVisibility(getDateViewId(), GONE);
        }
    }

    @Override
    protected void configureNextAlarm(RemoteViews rv, Context context, SharedPreferences prefs, String nextAlarmTime) {
        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnVerticalDigitalWidget(prefs)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
        } else {
            rv.setTextViewText(getNextAlarmViewId(), nextAlarmTime);
            rv.setViewVisibility(getNextAlarmViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmIconId(), VISIBLE);

            if (WidgetDAO.isVerticalDigitalWidgetDefaultNextAlarmColor(prefs)) {
                rv.setTextColor(getNextAlarmViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                rv.setTextColor(getNextAlarmViewId(), WidgetDAO.getVerticalDigitalWidgetCustomNextAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime,
                                           String nextAlarmTitle) {
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs) {
        int color = WidgetDAO.isBackgroundDisplayedOnVerticalDigitalWidget(prefs)
                ? WidgetDAO.getVerticalDigitalWidgetBackgroundColor(prefs)
                : Color.TRANSPARENT;

        rv.setInt(R.id.digitalWidgetBackground, "setBackgroundColor", color);
    }

    @Override
    protected void configureSizerClock(View sizer, SharedPreferences prefs) {
    }

    @Override
    protected void configureSizerDate(View sizer, Context context, SharedPreferences prefs) {
        final TextView date = sizer.findViewById(getDateViewId());

        if (WidgetDAO.isDateDisplayedOnVerticalDigitalWidget(prefs)) {
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

        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnVerticalDigitalWidget(prefs)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
        } else {
            nextAlarm.setText(nextAlarmTime);
            nextAlarm.setVisibility(VISIBLE);
            nextAlarmIcon.setVisibility(VISIBLE);
            nextAlarmIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));

            if (WidgetDAO.isVerticalDigitalWidgetDefaultNextAlarmColor(prefs)) {
                nextAlarmIcon.setTextColor(DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                nextAlarmIcon.setTextColor(WidgetDAO.getVerticalDigitalWidgetCustomNextAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
    }

    @Override
    protected void configureClockForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        final TextClock hours = sizer.findViewById(getClockHoursViewId());
        final TextClock minutes = sizer.findViewById(getClockMinutesViewId());

        hours.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        minutes.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
    }

    @Override
    protected void configureDateForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        final TextView date = sizer.findViewById(getDateViewId());
        date.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
    }

    @Override
    protected void configureNextAlarmForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
        final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
        // On some devices, the text shadow is cut off, so we have to add it to the end of the next alarm text.
        // The result is that next alarm text and the icon are perfectly centered.
        final int textShadowPadding = ThemeUtils.convertDpToPixels(3, sizer.getContext());

        nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarm.setPadding(0, 0, measuredSizes.mIconFontSizePx + textShadowPadding, 0);
        nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
        nextAlarmIcon.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
        final TextClock hours = sizer.findViewById(getClockHoursViewId());
        final TextClock minutes = sizer.findViewById(getClockMinutesViewId());

        measuredSizes.mMeasuredTextWidthPx = hours.getMeasuredWidth();
        measuredSizes.mMeasuredTextHeightPx = hours.getMeasuredHeight();
        measuredSizes.mMeasuredTextWidthPx = minutes.getMeasuredWidth();
        measuredSizes.mMeasuredTextHeightPx = minutes.getMeasuredHeight();

        if (nextAlarmIcon.getVisibility() == VISIBLE) {
            measuredSizes.mIconBitmap = ThemeUtils.createBitmap(nextAlarmIcon);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new VerticalDigitalAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets.materialyouwidgets;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Icon;
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
public class MaterialYouVerticalDigitalAppWidgetProvider extends BaseDigitalAppWidgetProvider {

    @Override
    protected int getLayoutId() {
        return R.layout.material_you_vertical_digital_widget;
    }

    @Override
    protected int getSizerLayoutId() {
        return R.layout.material_you_vertical_digital_widget_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.material_you_vertical_digital_widget;
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
    protected int getDateViewId() {
        return R.id.date;
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
        return R.id.clockHoursForCustomColor;
    }

    @Override
    protected int getClockMinutesCustomViewId() {
        return R.id.clockMinutesForCustomColor;
    }

    @Override
    protected int getDateCustomViewId() {
        return R.id.dateForCustomColor;
    }

    @Override
    protected int getNextAlarmIconCustomId() {
        return R.id.nextAlarmIconForCustomColor;
    }

    @Override
    protected int getNextAlarmCustomViewId() {
        return R.id.nextAlarmForCustomColor;
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
        return WidgetDAO.isMaterialYouVerticalDigitalWidgetHorizontalPaddingApplied(prefs);
    }

    @Override
    protected int getMaxWidgetFontSize(SharedPreferences prefs) {
        return WidgetDAO.getMaterialYouVerticalDigitalWidgetMaxClockFontSize(prefs);
    }

    @Override
    protected float getFontScaleFactor() {
        return 4f;
    }

    @Override
    protected Class<?> getCityServiceClass() {
        return null;
    }

    @Override
    protected void bindDateClickAction(RemoteViews rv, SharedPreferences prefs, PendingIntent calendarPendingIntent) {
        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(prefs)) {
            rv.setOnClickPendingIntent(getDateViewId(), calendarPendingIntent);
        } else {
            rv.setOnClickPendingIntent(getDateCustomViewId(), calendarPendingIntent);
        }
    }

    @Override
    protected void configureClock(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultHoursColor(prefs)) {
            rv.setViewVisibility(getClockHoursViewId(), VISIBLE);
            rv.setViewVisibility(getClockHoursCustomViewId(), GONE);
        } else {
            rv.setViewVisibility(getClockHoursViewId(), GONE);
            rv.setViewVisibility(getClockHoursCustomViewId(), VISIBLE);
            rv.setTextColor(getClockHoursCustomViewId(), WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomHoursColor(prefs));
        }

        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(prefs)) {
            rv.setViewVisibility(getClockMinutesViewId(), VISIBLE);
            rv.setViewVisibility(getClockMinutesCustomViewId(), GONE);
        } else {
            rv.setViewVisibility(getClockMinutesViewId(), GONE);
            rv.setViewVisibility(getClockMinutesCustomViewId(), VISIBLE);
            rv.setTextColor(getClockMinutesCustomViewId(),
                    WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomMinutesColor(prefs));
        }
    }

    @Override
    protected void configureDate(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isDateDisplayedOnMaterialYouVerticalDigitalWidget(prefs)) {
            if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(prefs)) {
                rv.setViewVisibility(getDateViewId(), VISIBLE);
                rv.setViewVisibility(getDateCustomViewId(), GONE);
                rv.setTextViewText(getDateViewId(), WidgetUtils.getDateFormat(context));
            } else {
                rv.setViewVisibility(getDateViewId(), GONE);
                rv.setViewVisibility(getDateCustomViewId(), VISIBLE);
                rv.setTextViewText(getDateCustomViewId(), WidgetUtils.getDateFormat(context));
                rv.setTextColor(getDateCustomViewId(), WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomDateColor(prefs));
            }
        } else {
            rv.setViewVisibility(getDateViewId(), GONE);
            rv.setViewVisibility(getDateCustomViewId(), GONE);
        }
    }

    @Override
    protected void configureNextAlarm(RemoteViews rv, Context context, SharedPreferences prefs, String nextAlarmTime) {
        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnMaterialYouVerticalDigitalWidget(prefs)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);
        } else {
            if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(prefs)) {
                rv.setViewVisibility(getNextAlarmViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmIconId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
                rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);
                rv.setTextViewText(getNextAlarmViewId(), nextAlarmTime);
            } else {
                rv.setViewVisibility(getNextAlarmViewId(), GONE);
                rv.setViewVisibility(getNextAlarmIconId(), GONE);
                rv.setViewVisibility(getNextAlarmCustomViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmIconCustomId(), VISIBLE);
                rv.setTextViewText(getNextAlarmCustomViewId(), nextAlarmTime);
                rv.setTextColor(getNextAlarmCustomViewId(),
                        WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime,
                                           String nextAlarmTitle) {
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs) {
        final Icon backgroundIcon = Icon.createWithResource(context, R.drawable.material_you_vertical_digital_widget_background);
        rv.setIcon(R.id.materialYouDigitalWidgetBackground, "setImageIcon", backgroundIcon);

        if (WidgetDAO.isBackgroundDisplayedOnMaterialYouVerticalDigitalWidget(prefs)) {
            if (!WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultBackgroundColor(prefs)) {
                backgroundIcon.setTint(WidgetDAO.getMaterialYouVerticalDigitalWidgetBackgroundColor(prefs));
            }
        } else {
            backgroundIcon.setTint(Color.TRANSPARENT);
        }
    }

    @Override
    protected void configureSizerClock(View sizer, SharedPreferences prefs) {
        final TextClock hours = sizer.findViewById(getClockHoursViewId());
        final TextClock hoursForCustomColor = sizer.findViewById(getClockHoursCustomViewId());
        final TextClock minutes = sizer.findViewById(getClockMinutesViewId());
        final TextClock minutesForCustomColor = sizer.findViewById(getClockMinutesCustomViewId());

        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(prefs)) {
            hours.setVisibility(VISIBLE);
            hoursForCustomColor.setVisibility(GONE);
        } else {
            hours.setVisibility(GONE);
            hoursForCustomColor.setVisibility(VISIBLE);
        }

        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(prefs)) {
            minutes.setVisibility(VISIBLE);
            minutesForCustomColor.setVisibility(GONE);
        } else {
            minutes.setVisibility(GONE);
            minutesForCustomColor.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void configureSizerDate(View sizer, Context context, SharedPreferences prefs) {
        final TextView date = sizer.findViewById(getDateViewId());
        final TextView dateForCustomColor = sizer.findViewById(getDateCustomViewId());

        if (WidgetDAO.isDateDisplayedOnMaterialYouVerticalDigitalWidget(prefs)) {
            if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(prefs)) {
                date.setVisibility(VISIBLE);
                dateForCustomColor.setVisibility(GONE);
                date.setText(WidgetUtils.getDateFormat(context));
            } else {
                date.setVisibility(GONE);
                dateForCustomColor.setVisibility(VISIBLE);
                dateForCustomColor.setText(WidgetUtils.getDateFormat(context));
            }
        } else {
            date.setVisibility(GONE);
            dateForCustomColor.setVisibility(GONE);
        }
    }

    @Override
    protected void configureSizerNextAlarm(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
        final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
        final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());
        final TextView nextAlarmForCustomColor = sizer.findViewById(getNextAlarmCustomViewId());

        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnMaterialYouVerticalDigitalWidget(prefs)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
            nextAlarmForCustomColor.setVisibility(GONE);
            nextAlarmIconForCustomColor.setVisibility(GONE);
        } else {
            if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(prefs)) {
                nextAlarm.setText(nextAlarmTime);
                nextAlarm.setVisibility(VISIBLE);
                nextAlarmIcon.setVisibility(VISIBLE);
                nextAlarmForCustomColor.setVisibility(GONE);
                nextAlarmIconForCustomColor.setVisibility(GONE);
                nextAlarmIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            } else {
                nextAlarmForCustomColor.setText(nextAlarmTime);
                nextAlarm.setVisibility(GONE);
                nextAlarmIcon.setVisibility(GONE);
                nextAlarmForCustomColor.setVisibility(VISIBLE);
                nextAlarmIconForCustomColor.setVisibility(VISIBLE);
                nextAlarmIconForCustomColor.setTypeface(ClockUtils.getAlarmIconTypeface(context));
                nextAlarmIconForCustomColor.setTextColor(
                        WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
    }

    @Override
    protected void configureClockForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultHoursColor(prefs)) {
            final TextClock hours = sizer.findViewById(getClockHoursViewId());
            hours.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        } else {
            final TextClock hoursForCustomColor = sizer.findViewById(getClockHoursCustomViewId());
            hoursForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        }

        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(prefs)) {
            final TextClock minutes = sizer.findViewById(getClockMinutesViewId());
            minutes.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        } else {
            final TextClock minutesForCustomColor = sizer.findViewById(getClockMinutesCustomViewId());
            minutesForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        }
    }

    @Override
    protected void configureDateForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(prefs)) {
            final TextView date = sizer.findViewById(getDateViewId());
            date.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView dateForCustomColor = sizer.findViewById(getDateCustomViewId());
            dateForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }
    }

    @Override
    protected void configureNextAlarmForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(prefs)) {
            final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
            final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());

            nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
            nextAlarm.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
            nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            nextAlarmIcon.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
        } else {
            final TextView nextAlarmForCustomColor = sizer.findViewById(getNextAlarmCustomViewId());
            final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());

            nextAlarmForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
            nextAlarmForCustomColor.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
            nextAlarmIconForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            nextAlarmIconForCustomColor.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
        }
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultHoursColor(prefs)) {
            final TextClock hours = sizer.findViewById(getClockHoursViewId());
            final TextClock minutes = sizer.findViewById(getClockMinutesViewId());

            measuredSizes.mMeasuredTextWidthPx = hours.getMeasuredWidth();
            measuredSizes.mMeasuredTextHeightPx = hours.getMeasuredHeight();
            measuredSizes.mMeasuredTextWidthPx = minutes.getMeasuredWidth();
            measuredSizes.mMeasuredTextHeightPx = minutes.getMeasuredHeight();
        } else {
            final TextClock hoursForCustomColor = sizer.findViewById(getClockHoursCustomViewId());
            final TextClock minutesForCustomColor = sizer.findViewById(getClockMinutesCustomViewId());

            measuredSizes.mMeasuredTextWidthPx = hoursForCustomColor.getMeasuredWidth();
            measuredSizes.mMeasuredTextHeightPx = hoursForCustomColor.getMeasuredHeight();
            measuredSizes.mMeasuredTextWidthPx = minutesForCustomColor.getMeasuredWidth();
            measuredSizes.mMeasuredTextHeightPx = minutesForCustomColor.getMeasuredHeight();
        }

        if (WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(prefs)) {
            final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());

            if (nextAlarmIcon.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = ThemeUtils.createBitmap(nextAlarmIcon);
            }
        } else {
            final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());

            if (nextAlarmIconForCustomColor.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = ThemeUtils.createBitmap(nextAlarmIconForCustomColor);
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new MaterialYouVerticalDigitalAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

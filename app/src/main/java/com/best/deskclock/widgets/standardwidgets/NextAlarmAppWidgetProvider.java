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
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widgets.BaseDigitalAppWidgetProvider;
import com.best.deskclock.widgets.DigitalWidgetSizes;

/**
 * <p>This provider produces a widget resembling one of the formats below.</p>
 * <p>
 * If an alarm is scheduled to ring in the future:
 * <pre>
 *        Next alarm
 *        Alarm title
 *      ⏰ THU 9:30 AM
 * </pre>
 * <p>
 * If no alarm is scheduled to ring in the future:
 * <pre>
 *      No upcoming alarm
 * </pre>
 * <p>
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
public class NextAlarmAppWidgetProvider extends BaseDigitalAppWidgetProvider {

    @Override
    protected int getLayoutId() {
        return R.layout.standard_next_alarm_widget;
    }

    @Override
    protected int getSizerLayoutId() {
        return R.layout.standard_next_alarm_widget_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.nextAlarmWidget;
    }

    @Override
    protected int getDateViewId() {
        return 0;
    }

    @Override
    protected int getClockViewId() {
        return 0;
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
        return R.id.nextAlarmText;
    }

    @Override
    protected int getNextAlarmTitleViewId() {
        return R.id.nextAlarmTitle;
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
        return WidgetDAO.isNextAlarmWidgetHorizontalPaddingApplied(prefs);
    }

    @Override
    protected int getMaxWidgetFontSize(SharedPreferences prefs) {
        return WidgetDAO.getNextAlarmWidgetMaxFontSize(prefs);
    }

    @Override
    protected float getFontScaleFactor() {
        return 3f;
    }

    @Override
    protected Class<?> getCityServiceClass() {
        return null;
    }

    @Override
    protected void bindDateClickAction(RemoteViews rv, SharedPreferences prefs, PendingIntent calendarPendingIntent) {
    }

    @Override
    protected void configureClock(RemoteViews rv, Context context, SharedPreferences prefs) {
    }

    @Override
    protected void configureDate(RemoteViews rv, Context context, SharedPreferences prefs) {
    }

    @Override
    protected void configureNextAlarm(RemoteViews rv, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final Context localizedContext = Utils.getLocalizedContext(context);
        final boolean isDefaultTitleColor = WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getNextAlarmWidgetCustomTitleColor(prefs);

        if (TextUtils.isEmpty(nextAlarmTime)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setTextViewText(getNextAlarmTextViewId(), localizedContext.getString(R.string.next_alarm_widget_title_no_alarm));

            if (isDefaultTitleColor) {
                rv.setTextColor(getNextAlarmTextViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                rv.setTextColor(getNextAlarmTextViewId(), customTitleColor);
            }
        } else {
            rv.setViewVisibility(getNextAlarmViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmIconId(), VISIBLE);
            rv.setTextViewText(getNextAlarmTextViewId(), localizedContext.getString(R.string.next_alarm_widget_text));
            rv.setTextViewText(getNextAlarmViewId(), nextAlarmTime);

            if (isDefaultTitleColor) {
                rv.setTextColor(getNextAlarmTextViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                rv.setTextColor(getNextAlarmTextViewId(), customTitleColor);
            }

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
                rv.setTextColor(getNextAlarmViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                rv.setTextColor(getNextAlarmViewId(), WidgetDAO.getNextAlarmWidgetCustomAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime,
                                           String nextAlarmTitle) {

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
        } else {
            rv.setViewVisibility(getNextAlarmTitleViewId(), VISIBLE);
            rv.setTextViewText(getNextAlarmTitleViewId(), nextAlarmTitle);

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
                rv.setTextColor(getNextAlarmTitleViewId(), DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                rv.setTextColor(getNextAlarmTitleViewId(), WidgetDAO.getNextAlarmWidgetCustomAlarmTitleColor(prefs));
            }
        }
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs) {
        int color = WidgetDAO.isBackgroundDisplayedOnNextAlarmWidget(prefs)
                ? WidgetDAO.getNextAlarmWidgetBackgroundColor(prefs)
                : Color.TRANSPARENT;
        rv.setInt(R.id.digitalWidgetBackground, "setBackgroundColor", color);
    }

    @Override
    protected void configureSizerClock(View sizer, SharedPreferences prefs) {
    }

    @Override
    protected void configureSizerDate(View sizer, Context context, SharedPreferences prefs) {
    }

    @Override
    protected void configureSizerNextAlarm(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final Context localizedContext = Utils.getLocalizedContext(context);
        final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
        final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
        final TextView nextAlarmText = sizer.findViewById(getNextAlarmTextViewId());
        final boolean isDefaultTitleColor = WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getNextAlarmWidgetCustomTitleColor(prefs);
        final int customAlarmColor = WidgetDAO.getNextAlarmWidgetCustomAlarmColor(prefs);

        if (TextUtils.isEmpty(nextAlarmTime)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
            nextAlarmText.setText(localizedContext.getString(R.string.next_alarm_widget_title_no_alarm));

            if (isDefaultTitleColor) {
                nextAlarmText.setTextColor(DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                nextAlarmText.setTextColor(customTitleColor);
            }
        } else {
            nextAlarm.setVisibility(VISIBLE);
            nextAlarmIcon.setVisibility(VISIBLE);
            nextAlarm.setText(nextAlarmTime);
            nextAlarmIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            nextAlarmText.setText(localizedContext.getString(R.string.next_alarm_widget_text));

            if (isDefaultTitleColor) {
                nextAlarmText.setTextColor(DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                nextAlarmText.setTextColor(customTitleColor);
            }

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
                nextAlarm.setTextColor(DEFAULT_WIDGETS_CUSTOM_COLOR);
                nextAlarmIcon.setTextColor(DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                nextAlarm.setTextColor(customAlarmColor);
                nextAlarmIcon.setTextColor(customAlarmColor);
            }
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);
        final TextView nextAlarmTitleView = sizer.findViewById(getNextAlarmTitleViewId());

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            nextAlarmTitleView.setVisibility(GONE);
        } else {
            nextAlarmTitleView.setVisibility(VISIBLE);
            nextAlarmTitleView.setText(nextAlarmTitle);

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
                nextAlarmTitleView.setTextColor(DEFAULT_WIDGETS_CUSTOM_COLOR);
            } else {
                nextAlarmTitleView.setTextColor(WidgetDAO.getNextAlarmWidgetCustomAlarmTitleColor(prefs));
            }
        }
    }

    @Override
    protected void configureClockForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
    }

    @Override
    protected void configureDateForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
    }

    @Override
    protected void configureNextAlarmForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
        final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
        final TextView nextAlarmText = sizer.findViewById(getNextAlarmTextViewId());
        final TextView nextAlarmTitle = sizer.findViewById(getNextAlarmTitleViewId());
        // On some devices, the text shadow is cut off, so we have to add it to the end of the next alarm text.
        // The result is that next alarm text and the icon are perfectly centered.
        final int textShadowPadding = ThemeUtils.convertDpToPixels(3, sizer.getContext());

        nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarm.setPadding(0, 0, measuredSizes.mIconPaddingPx + textShadowPadding, 0);
        nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
        nextAlarmIcon.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
        nextAlarmText.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarmTitle.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());

        if (nextAlarmIcon.getVisibility() == VISIBLE) {
            measuredSizes.mIconBitmap = ThemeUtils.createBitmap(nextAlarmIcon);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new NextAlarmAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

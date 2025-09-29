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
public class MaterialYouNextAlarmAppWidgetProvider extends BaseDigitalAppWidgetProvider {

    @Override
    protected int getLayoutId() {
        return R.layout.material_you_next_alarm_widget;
    }

    @Override
    protected int getSizerLayoutId() {
        return R.layout.material_you_next_alarm_widget_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.material_you_next_alarm_widget;
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
        return R.id.nextAlarmIconForCustomColor;
    }

    @Override
    protected int getNextAlarmCustomViewId() {
        return R.id.nextAlarmForCustomColor;
    }

    @Override
    protected int getNextAlarmTextCustomViewId() {
        return R.id.nextAlarmTextForCustomColor;
    }

    @Override
    protected int getNextAlarmTitleCustomViewId() {
        return R.id.nextAlarmTitleForCustomColor;
    }

    @Override
    protected boolean areWorldCitiesDisplayed(SharedPreferences prefs) {
        return false;
    }

    @Override
    protected boolean isHorizontalPaddingApplied(SharedPreferences prefs) {
        return WidgetDAO.isMaterialYouNextAlarmWidgetHorizontalPaddingApplied(prefs);
    }

    @Override
    protected int getMaxWidgetFontSize(SharedPreferences prefs) {
        return WidgetDAO.getMaterialYouNextAlarmWidgetMaxFontSize(prefs);
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
        final String nextAlarmText = localizedContext.getString(R.string.next_alarm_widget_text);
        final String noAlarmTitle = localizedContext.getString(R.string.next_alarm_widget_title_no_alarm);
        final boolean isDefaultTitleColor = WidgetDAO.isMaterialYouNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getMaterialYouNextAlarmWidgetCustomTitleColor(prefs);

        if (TextUtils.isEmpty(nextAlarmTime)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);

            if (isDefaultTitleColor) {
                rv.setViewVisibility(getNextAlarmTextViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmTextCustomViewId(), GONE);
                rv.setTextViewText(getNextAlarmTextViewId(), noAlarmTitle);
            } else {
                rv.setViewVisibility(getNextAlarmTextViewId(), GONE);
                rv.setViewVisibility(getNextAlarmTextCustomViewId(), VISIBLE);
                rv.setTextViewText(getNextAlarmTextCustomViewId(), noAlarmTitle);
                rv.setTextColor(getNextAlarmTextCustomViewId(), customTitleColor);
            }
        } else {
            if (isDefaultTitleColor) {
                rv.setViewVisibility(getNextAlarmTextViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmTextCustomViewId(), GONE);
                rv.setTextViewText(getNextAlarmTextViewId(), nextAlarmText);
            } else {
                rv.setViewVisibility(getNextAlarmTextViewId(), GONE);
                rv.setViewVisibility(getNextAlarmTextCustomViewId(), VISIBLE);
                rv.setTextViewText(getNextAlarmTextCustomViewId(), nextAlarmText);
                rv.setTextColor(getNextAlarmTextCustomViewId(), customTitleColor);
            }

            if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(prefs)) {
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
                        WidgetDAO.getMaterialYouNextAlarmWidgetCustomAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime,
                                           String nextAlarmTitle) {

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
        } else {
            if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
                rv.setViewVisibility(getNextAlarmTitleViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
                rv.setTextViewText(getNextAlarmTitleViewId(), nextAlarmTitle);
            } else {
                rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
                rv.setViewVisibility(getNextAlarmTitleCustomViewId(), VISIBLE);
                rv.setTextViewText(getNextAlarmTitleCustomViewId(), nextAlarmTitle);
                rv.setTextColor(getNextAlarmTitleCustomViewId(),
                        WidgetDAO.getMaterialYouNextAlarmWidgetCustomAlarmTitleColor(prefs));
            }
        }
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs) {
        final Icon backgroundIcon = Icon.createWithResource(context, R.drawable.material_you_digital_widget_background);
        rv.setIcon(R.id.materialYouDigitalWidgetBackground, "setImageIcon", backgroundIcon);

        if (WidgetDAO.isBackgroundDisplayedOnMaterialYouNextAlarmWidget(prefs)) {
            if (!WidgetDAO.isMaterialYouNextAlarmWidgetDefaultBackgroundColor(prefs)) {
                backgroundIcon.setTint(WidgetDAO.getMaterialYouNextAlarmWidgetBackgroundColor(prefs));
            }
        } else {
            backgroundIcon.setTint(Color.TRANSPARENT);
        }
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
        final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());
        final TextView nextAlarmForCustomColor = sizer.findViewById(getNextAlarmCustomViewId());
        final TextView nextAlarmText = sizer.findViewById(getNextAlarmTextViewId());
        final TextView nextAlarmTextForCustomColor = sizer.findViewById(getNextAlarmTextCustomViewId());

        final boolean isDefaultTitleColor = WidgetDAO.isMaterialYouNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getMaterialYouNextAlarmWidgetCustomTitleColor(prefs);
        final int customAlarmColor = WidgetDAO.getMaterialYouNextAlarmWidgetCustomAlarmColor(prefs);

        if (TextUtils.isEmpty(nextAlarmTime)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
            nextAlarmForCustomColor.setVisibility(GONE);
            nextAlarmIconForCustomColor.setVisibility(GONE);

            if (isDefaultTitleColor) {
                nextAlarmText.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setVisibility(GONE);
                nextAlarmText.setText(localizedContext.getString(R.string.next_alarm_widget_title_no_alarm));
            } else {
                nextAlarmText.setVisibility(GONE);
                nextAlarmTextForCustomColor.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setText(localizedContext.getString(R.string.next_alarm_widget_title_no_alarm));
                nextAlarmTextForCustomColor.setTextColor(customTitleColor);
            }
        } else {
            if (isDefaultTitleColor) {
                nextAlarmText.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setVisibility(GONE);
                nextAlarmText.setText(localizedContext.getString(R.string.next_alarm_widget_text));
            } else {
                nextAlarmText.setVisibility(GONE);
                nextAlarmTextForCustomColor.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setText(localizedContext.getString(R.string.next_alarm_widget_text));
                nextAlarmTextForCustomColor.setTextColor(customTitleColor);
            }

            if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(prefs)) {
                nextAlarm.setVisibility(VISIBLE);
                nextAlarmIcon.setVisibility(VISIBLE);
                nextAlarmForCustomColor.setVisibility(GONE);
                nextAlarmIconForCustomColor.setVisibility(GONE);
                nextAlarm.setText(nextAlarmTime);
                nextAlarmIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            } else {
                nextAlarm.setVisibility(GONE);
                nextAlarmIcon.setVisibility(GONE);
                nextAlarmForCustomColor.setVisibility(VISIBLE);
                nextAlarmIconForCustomColor.setVisibility(VISIBLE);
                nextAlarmForCustomColor.setText(nextAlarmTime);
                nextAlarmForCustomColor.setTextColor(customAlarmColor);
                nextAlarmIconForCustomColor.setTypeface(ClockUtils.getAlarmIconTypeface(context));
                nextAlarmIconForCustomColor.setTextColor(customAlarmColor);
            }
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);
        final TextView nextAlarmTitleView = sizer.findViewById(getNextAlarmTitleViewId());
        final TextView nextAlarmTitleViewForCustomColor = sizer.findViewById(getNextAlarmTitleCustomViewId());

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            nextAlarmTitleView.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
        } else {
            if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
                nextAlarmTitleView.setVisibility(VISIBLE);
                nextAlarmTitleViewForCustomColor.setVisibility(GONE);
                nextAlarmTitleView.setText(nextAlarmTitle);
            } else {
                nextAlarmTitleView.setVisibility(GONE);
                nextAlarmTitleViewForCustomColor.setVisibility(VISIBLE);
                nextAlarmTitleView.setText(nextAlarmTitle);
                nextAlarmTitleViewForCustomColor.setTextColor(
                        WidgetDAO.getMaterialYouNextAlarmWidgetCustomAlarmTitleColor(prefs));
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
        if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(prefs)) {
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

        if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultTitleColor(prefs)) {
            final TextView nextAlarmText = sizer.findViewById(getNextAlarmTextViewId());
            nextAlarmText.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView nextAlarmTextForCustomColor = sizer.findViewById(getNextAlarmTextCustomViewId());
            nextAlarmTextForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }

        if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
            final TextView nextAlarmTitle = sizer.findViewById(getNextAlarmTitleViewId());
            nextAlarmTitle.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView nextAlarmTitleForCustomColor = sizer.findViewById(getNextAlarmTitleCustomViewId());
            nextAlarmTitleForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(prefs)) {
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
        new MaterialYouNextAlarmAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets.materialyouwidgets;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_MATERIAL_YOU_WIDGET_BACKGROUND_CORNER_RADIUS;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.SdkUtils;
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
 *        WED, FEB 3
 *         12:59 AM
 *      ⏰ THU 9:30 AM
 * </pre>
 * <p>
 * If no alarm is scheduled to ring in the future:
 * <pre>
 *        WED, FEB 3
 *         12:59 AM
 * </pre>
 * <p>
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
public class MaterialYouDigitalAppWidgetProvider extends BaseDigitalAppWidgetProvider {

    @Override
    protected int getLayoutWithShadowId() {
        return 0;
    }

    @Override
    protected int getLayoutWithoutShadowId() {
        return R.layout.material_you_digital_widget;
    }

    @Override
    protected int getSizerLayoutWithShadowId() {
        return 0;
    }

    @Override
    protected int getSizerLayoutWithoutShadowId() {
        return R.layout.material_you_digital_widget_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.material_you_digital_widget;
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
        return R.id.worldCityList;
    }

    @Override
    protected int getClockCustomViewId() {
        return R.id.clockForCustomColor;
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
    protected boolean isTextShadowDisplayed(SharedPreferences prefs) {
        return false;
    }

    @Override
    protected boolean areWorldCitiesDisplayed(SharedPreferences prefs) {
        return WidgetDAO.areWorldCitiesDisplayedOnMaterialYouDigitalWidget(prefs);
    }

    @Override
    protected boolean isHorizontalPaddingApplied(SharedPreferences prefs) {
        return WidgetDAO.isMaterialYouDigitalWidgetHorizontalPaddingApplied(prefs);
    }

    @Override
    protected int getMaxWidgetFontSize(SharedPreferences prefs) {
        return WidgetDAO.getMaterialYouDigitalWidgetMaxClockFontSize(prefs);
    }

    @Override
    protected float getFontScaleFactor(SharedPreferences prefs) {
        return 4f;
    }

    @Override
    protected Class<?> getCityServiceClass() {
        return AppWidgetCityService.MaterialYouDigitalAppWidgetCityService.class;
    }

    @Override
    protected void bindDateClickAction(RemoteViews rv, SharedPreferences prefs, PendingIntent calendarPendingIntent) {
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(prefs)) {
            rv.setOnClickPendingIntent(getDateViewId(), calendarPendingIntent);
        } else {
            rv.setOnClickPendingIntent(getDateCustomViewId(), calendarPendingIntent);
        }
    }

    @Override
    protected void configureClock(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(prefs)) {
            rv.setViewVisibility(getClockViewId(), VISIBLE);
            rv.setViewVisibility(getClockCustomViewId(), GONE);

            if (DataModel.getDataModel().is24HourFormat()) {
                rv.setCharSequence(getClockViewId(), "setFormat24Hour", ClockUtils.get24ModeFormat(
                        WidgetDAO.areSecondsDisplayedOnMaterialYouDigitalWidget(prefs), false));
            } else {
                rv.setCharSequence(getClockViewId(), "setFormat12Hour", ClockUtils.get12ModeFormat(
                        context, WidgetUtils.getAmPmRatio(true, prefs),
                        WidgetDAO.areSecondsDisplayedOnMaterialYouDigitalWidget(prefs),
                        false, false, false));
            }
        } else {
            rv.setViewVisibility(getClockViewId(), GONE);
            rv.setViewVisibility(getClockCustomViewId(), VISIBLE);
            rv.setCharSequence(getClockCustomViewId(), "setFormat12Hour",
                    ClockUtils.get12ModeFormat(context, WidgetUtils.getAmPmRatio(true, prefs),
                            WidgetDAO.areSecondsDisplayedOnMaterialYouDigitalWidget(prefs),
                            false, false, false));
            rv.setCharSequence(getClockCustomViewId(), "setFormat24Hour",
                    ClockUtils.get24ModeFormat(
                            WidgetDAO.areSecondsDisplayedOnMaterialYouDigitalWidget(prefs), false));
            rv.setTextColor(getClockCustomViewId(),
                    WidgetDAO.getMaterialYouDigitalWidgetCustomClockColor(prefs));
        }
    }

    @Override
    protected void configureDate(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isDateDisplayedOnMaterialYouDigitalWidget(prefs)) {
            if (WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(prefs)) {
                rv.setViewVisibility(getDateViewId(), VISIBLE);
                rv.setViewVisibility(getDateCustomViewId(), GONE);
                rv.setTextViewText(getDateViewId(), WidgetUtils.getDateFormat(context));
            } else {
                rv.setViewVisibility(getDateViewId(), GONE);
                rv.setViewVisibility(getDateCustomViewId(), VISIBLE);
                rv.setTextViewText(getDateCustomViewId(), WidgetUtils.getDateFormat(context));
                rv.setTextColor(getDateCustomViewId(), WidgetDAO.getMaterialYouDigitalWidgetCustomDateColor(prefs));
            }
        } else {
            rv.setViewVisibility(getDateViewId(), GONE);
            rv.setViewVisibility(getDateCustomViewId(), GONE);
        }
    }

    @Override
    protected void configureNextAlarm(RemoteViews rv, Context context, SharedPreferences prefs, String nextAlarmTime) {
        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnMaterialYouDigitalWidget(prefs)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);
        } else {
            if (WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(prefs)) {
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
                        WidgetDAO.getMaterialYouDigitalWidgetCustomNextAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime,
                                           String nextAlarmTitle) {
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs,
                                       int widthPx, int heightPx) {

        if (!WidgetDAO.isBackgroundDisplayedOnMaterialYouDigitalWidget(prefs)
                || widthPx <= 0 || heightPx <= 0) {
            rv.setIcon(R.id.materialYouDigitalWidgetBackground, "setImageIcon", null);
            return;
        }

        int radius = (int) dpToPx(WidgetDAO.isMaterialYouDigitalWidgetBackgroundCornerRadiusCustomizable(prefs)
                ? WidgetDAO.getMaterialYouDigitalWidgetBackgroundCornerRadius(prefs)
                : DEFAULT_MATERIAL_YOU_WIDGET_BACKGROUND_CORNER_RADIUS, context.getResources().getDisplayMetrics());

        int color = WidgetDAO.getMaterialYouDigitalWidgetBackgroundColor(prefs);

        boolean isDefaultBackgroundColor = WidgetDAO.isMaterialYouDigitalWidgetDefaultBackgroundColor(prefs);

        if (SdkUtils.isAtLeastAndroid12()) {
            if (isDefaultBackgroundColor) {
                Icon dayIcon = WidgetUtils.createRoundedIcon(widthPx, heightPx,
                        WidgetUtils.getMaterialBackgroundColorDay(context), radius);

                Icon nightIcon = WidgetUtils.createRoundedIcon(widthPx, heightPx,
                        WidgetUtils.getMaterialBackgroundColorNight(context), radius);

                rv.setIcon(R.id.materialYouDigitalWidgetBackground, "setImageIcon", dayIcon, nightIcon);
            } else {
                Icon icon = WidgetUtils.createRoundedIcon(widthPx, heightPx, color, radius);
                rv.setIcon(R.id.materialYouDigitalWidgetBackground, "setImageIcon", icon);
            }
        } else {
            if (isDefaultBackgroundColor) {
                final Icon backgroundIcon = Icon.createWithResource(context, R.drawable.material_you_digital_widget_background);
                rv.setIcon(R.id.materialYouDigitalWidgetBackground, "setImageIcon", backgroundIcon);
            } else {
                Icon icon = WidgetUtils.createRoundedIcon(widthPx, heightPx, color, radius);
                rv.setIcon(R.id.materialYouDigitalWidgetBackground, "setImageIcon", icon);
            }
        }
    }

    @Override
    protected void configureSizerClock(View sizer, SharedPreferences prefs) {
        final TextClock clock = sizer.findViewById(getClockViewId());
        final TextClock clockForCustomColor = sizer.findViewById(getClockCustomViewId());
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(prefs)) {
            clock.setVisibility(VISIBLE);
            clockForCustomColor.setVisibility(GONE);
        } else {
            clock.setVisibility(GONE);
            clockForCustomColor.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void configureSizerDate(View sizer, Context context, SharedPreferences prefs) {
        final TextView date = sizer.findViewById(getDateViewId());
        final TextView dateForCustomColor = sizer.findViewById(getDateCustomViewId());
        if (WidgetDAO.isDateDisplayedOnMaterialYouDigitalWidget(prefs)) {
            if (WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(prefs)) {
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

        if (TextUtils.isEmpty(nextAlarmTime) || !WidgetDAO.isNextAlarmDisplayedOnMaterialYouDigitalWidget(prefs)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
            nextAlarmForCustomColor.setVisibility(GONE);
            nextAlarmIconForCustomColor.setVisibility(GONE);
        } else {
            if (WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(prefs)) {
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
                        WidgetDAO.getMaterialYouDigitalWidgetCustomNextAlarmColor(prefs));
            }
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
    }

    @Override
    protected void configureClockForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(prefs)) {
            TextClock clock = sizer.findViewById(getClockViewId());
            clock.setText(WidgetUtils.getLongestTimeString(clock, true));
            clock.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        } else {
            final TextClock clockForCustomColor = sizer.findViewById(getClockCustomViewId());
            clockForCustomColor.setText(WidgetUtils.getLongestTimeString(clockForCustomColor, true));
            clockForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
            clockForCustomColor.setText(WidgetUtils.getLongestTimeString(clockForCustomColor, true));
            clockForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        }
    }

    @Override
    protected void configureDateForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(prefs)) {
            final TextView date = sizer.findViewById(getDateViewId());
            date.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView dateForCustomColor = sizer.findViewById(getDateCustomViewId());
            dateForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }
    }

    @Override
    protected void configureNextAlarmForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(prefs)) {
            final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
            final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());

            nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
            nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            nextAlarmIcon.setPadding(measuredSizes.mIconPaddingPx, 0, measuredSizes.mIconPaddingPx, 0);
        } else {
            final TextView nextAlarmForCustomColor = sizer.findViewById(getNextAlarmCustomViewId());
            final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());

            nextAlarmForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
            nextAlarmIconForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            nextAlarmIconForCustomColor.setPadding(measuredSizes.mIconPaddingPx, 0, measuredSizes.mIconPaddingPx, 0);
        }
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(prefs)) {
            TextClock clock = sizer.findViewById(getClockViewId());
            measuredSizes.mMeasuredTextWidthPx = clock.getMeasuredWidth();
            measuredSizes.mMeasuredTextHeightPx = clock.getMeasuredHeight();
        } else {
            TextClock clockForCustomColor = sizer.findViewById(getClockCustomViewId());
            measuredSizes.mMeasuredTextForCustomColorWidthPx = clockForCustomColor.getMeasuredWidth();
            measuredSizes.mMeasuredTextForCustomColorHeightPx = clockForCustomColor.getMeasuredHeight();
        }

        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(prefs)) {
            TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
            if (nextAlarmIcon.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = ThemeUtils.createBitmap(nextAlarmIcon);
            }
        } else {
            TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());
            if (nextAlarmIconForCustomColor.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = ThemeUtils.createBitmap(nextAlarmIconForCustomColor);
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new MaterialYouDigitalAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

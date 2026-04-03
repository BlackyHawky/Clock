// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.utils.WidgetUtils.METHOD_SET_IMAGE_ICON;

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
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.SdkUtils;
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
public class DigitalAppWidgetProvider extends BaseDigitalAppWidgetProvider {

    @Override
    protected int getLayoutWithShadowId() {
        return R.layout.appwidget_digital_with_shadow;
    }

    @Override
    protected int getLayoutWithoutShadowId() {
        return R.layout.appwidget_digital;
    }

    @Override
    protected int getSizerLayoutWithShadowId() {
        return R.layout.appwidget_digital_sizer_with_shadow;
    }

    @Override
    protected int getSizerLayoutWithoutShadowId() {
        return R.layout.appwidget_digital_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.digital_widget;
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
    protected int getLabelIconViewId() {
        return R.id.labelIcon;
    }

    @Override
    protected int getNextAlarmTitleViewId() {
        return R.id.nextAlarmTitle;
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
    protected int getLabelIconCustomViewId() {
        return R.id.labelIconForCustomColor;
    }

    @Override
    protected int getNextAlarmTitleCustomViewId() {
        return R.id.nextAlarmTitleForCustomColor;
    }

    @Override
    protected boolean isTextUppercase(SharedPreferences prefs) {
        return WidgetDAO.isTextUppercaseDisplayedOnDigitalWidget(prefs);
    }

    @Override
    protected boolean isTextShadowDisplayed(SharedPreferences prefs) {
        return WidgetDAO.isTextShadowDisplayedOnDigitalWidget(prefs);
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
        return 4f;
    }

    @Override
    protected String getNextAlarmTime(Context context) {
        return AlarmUtils.getNextAlarm(context);
    }

    @Override
    protected Class<?> getCityServiceClass() {
        return AppWidgetCityService.DigitalAppWidgetCityService.class;
    }

    @Override
    protected int getCityLayoutId() {
        return R.layout.world_clock_remote_list_item_modern;
    }

    @Override
    protected boolean isDefaultCityClockColor(SharedPreferences prefs) {
        return WidgetDAO.isDigitalWidgetDefaultCityClockColor(prefs);
    }

    @Override
    protected int getCityClockColor(SharedPreferences prefs) {
        return WidgetDAO.getDigitalWidgetCustomCityClockColor(prefs);
    }

    @Override
    protected boolean isDefaultCityNameColor(SharedPreferences prefs) {
        return WidgetDAO.isDigitalWidgetDefaultCityNameColor(prefs);
    }

    @Override
    protected int getCityNameColor(SharedPreferences prefs) {
        return WidgetDAO.getDigitalWidgetCustomCityNameColor(prefs);
    }

    @Override
    protected boolean isDefaultCityNoteColor(SharedPreferences prefs) {
        return WidgetDAO.isDigitalWidgetDefaultCityNoteColor(prefs);
    }

    @Override
    protected int getCityNoteColor(SharedPreferences prefs) {
        return WidgetDAO.getDigitalWidgetCustomCityNoteColor(prefs);
    }

    @Override
    protected void bindDateClickAction(RemoteViews rv, SharedPreferences prefs, PendingIntent calendarPendingIntent) {
        if (WidgetDAO.isDigitalWidgetDefaultDateColor(prefs)) {
            rv.setOnClickPendingIntent(getDateViewId(), calendarPendingIntent);
        } else {
            rv.setOnClickPendingIntent(getDateCustomViewId(), calendarPendingIntent);
        }
    }

    @Override
    protected void configureClock(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (WidgetDAO.isDigitalWidgetDefaultClockColor(prefs)) {
            rv.setViewVisibility(getClockViewId(), VISIBLE);
            rv.setViewVisibility(getClockCustomViewId(), GONE);

            WidgetUtils.applyClockFormat(rv, context, getClockViewId(), WidgetUtils.getAmPmRatio(prefs),
                WidgetDAO.areSecondsDisplayedOnDigitalWidget(prefs));
        } else {
            rv.setViewVisibility(getClockViewId(), GONE);
            rv.setViewVisibility(getClockCustomViewId(), VISIBLE);

            WidgetUtils.applyClockFormat(rv, context, getClockCustomViewId(), WidgetUtils.getAmPmRatio(prefs),
                WidgetDAO.areSecondsDisplayedOnDigitalWidget(prefs));

            rv.setTextColor(getClockCustomViewId(), WidgetDAO.getDigitalWidgetCustomClockColor(prefs));
        }
    }

    @Override
    protected void configureDate(RemoteViews rv, Context context, SharedPreferences prefs) {
        if (!WidgetDAO.isDateDisplayedOnDigitalWidget(prefs)) {
            rv.setViewVisibility(getDateViewId(), GONE);
            rv.setViewVisibility(getDateCustomViewId(), GONE);
            return;
        }

        String dateFormat = WidgetUtils.getDateFormat(context);

        if (WidgetDAO.isDigitalWidgetDefaultDateColor(prefs)) {
            rv.setViewVisibility(getDateViewId(), VISIBLE);
            rv.setViewVisibility(getDateCustomViewId(), GONE);
            rv.setTextViewText(getDateViewId(), isTextUppercase(prefs) ? dateFormat.toUpperCase() : dateFormat);
        } else {
            rv.setViewVisibility(getDateViewId(), GONE);
            rv.setViewVisibility(getDateCustomViewId(), VISIBLE);
            rv.setTextColor(getDateCustomViewId(), WidgetDAO.getDigitalWidgetCustomDateColor(prefs));
            rv.setTextViewText(getDateCustomViewId(), isTextUppercase(prefs) ? dateFormat.toUpperCase() : dateFormat);
        }
    }

    @Override
    protected void configureNextAlarm(RemoteViews rv, Context context, SharedPreferences prefs, String nextAlarmTime) {
        if (!WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(prefs) || TextUtils.isEmpty(nextAlarmTime)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);
            return;
        }

        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(prefs)) {
            rv.setViewVisibility(getNextAlarmViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmIconId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);
            rv.setTextViewText(getNextAlarmViewId(), isTextUppercase(prefs) ? nextAlarmTime.toUpperCase() : nextAlarmTime);
        } else {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), VISIBLE);
            rv.setTextColor(getNextAlarmCustomViewId(), WidgetDAO.getDigitalWidgetCustomNextAlarmColor(prefs));
            rv.setTextViewText(getNextAlarmCustomViewId(), isTextUppercase(prefs) ? nextAlarmTime.toUpperCase() : nextAlarmTime);
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime, String nextAlarmTitle) {
        if (!WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(prefs) || !WidgetDAO.isNextAlarmTitleDisplayedOnDigitalWidget(prefs)) {
            rv.setViewVisibility(getLabelIconViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getLabelIconCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
            return;
        }

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            rv.setViewVisibility(getLabelIconViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getLabelIconCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
            return;
        }

        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(prefs)) {
            rv.setViewVisibility(getLabelIconViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmTitleViewId(), VISIBLE);
            rv.setViewVisibility(getLabelIconCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
            rv.setTextViewText(getNextAlarmTitleViewId(), isTextUppercase(prefs) ? nextAlarmTitle.toUpperCase() : nextAlarmTitle);
        } else {
            rv.setViewVisibility(getLabelIconViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getLabelIconCustomViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), VISIBLE);
            rv.setTextColor(getNextAlarmTitleCustomViewId(), WidgetDAO.getDigitalWidgetCustomNextAlarmTitleColor(prefs));
            rv.setTextViewText(getNextAlarmTitleCustomViewId(), isTextUppercase(prefs) ? nextAlarmTitle.toUpperCase() : nextAlarmTitle);
        }
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs, int widthPx, int heightPx) {
        if (!WidgetDAO.isBackgroundDisplayedOnDigitalWidget(prefs) || widthPx <= 0 || heightPx <= 0) {
            rv.setIcon(R.id.digitalWidgetBackground, METHOD_SET_IMAGE_ICON, null);
            return;
        }

        int radius = (int) dpToPx(WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(prefs)
            ? WidgetDAO.getDigitalWidgetBackgroundCornerRadius(prefs)
            : DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS, context.getResources().getDisplayMetrics());

        int color = WidgetDAO.getDigitalWidgetBackgroundColor(prefs);

        boolean isDefaultBackgroundColor = WidgetDAO.isDigitalWidgetDefaultBackgroundColor(prefs);

        if (SdkUtils.isAtLeastAndroid12()) {
            if (isDefaultBackgroundColor) {
                Icon dayIcon = WidgetUtils.createRoundedIcon(widthPx, heightPx, WidgetUtils.getBackgroundColorDay(context), radius);

                Icon nightIcon = WidgetUtils.createRoundedIcon(
                    widthPx, heightPx, WidgetUtils.getBackgroundColorNight(context), radius);

                rv.setIcon(R.id.digitalWidgetBackground, METHOD_SET_IMAGE_ICON, dayIcon, nightIcon);
            } else {
                Icon icon = WidgetUtils.createRoundedIcon(widthPx, heightPx, color, radius);
                rv.setIcon(R.id.digitalWidgetBackground, METHOD_SET_IMAGE_ICON, icon);
            }
        } else {
            if (isDefaultBackgroundColor) {
                final Icon backgroundIcon = Icon.createWithResource(context, R.drawable.appwidget_digital_background);
                rv.setIcon(R.id.digitalWidgetBackground, METHOD_SET_IMAGE_ICON, backgroundIcon);
            } else {
                Icon icon = WidgetUtils.createRoundedIcon(widthPx, heightPx, color, radius);
                rv.setIcon(R.id.digitalWidgetBackground, METHOD_SET_IMAGE_ICON, icon);
            }
        }
    }

    @Override
    protected void configureSizerClock(View sizer, SharedPreferences prefs) {
        final TextClock clock = sizer.findViewById(getClockViewId());
        final TextClock clockForCustomColor = sizer.findViewById(getClockCustomViewId());
        if (WidgetDAO.isDigitalWidgetDefaultClockColor(prefs)) {
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

        if (!WidgetDAO.isDateDisplayedOnDigitalWidget(prefs)) {
            date.setVisibility(GONE);
            dateForCustomColor.setVisibility(GONE);
            return;
        }

        String dateFormat = WidgetUtils.getDateFormat(context);

        if (WidgetDAO.isDigitalWidgetDefaultDateColor(prefs)) {
            date.setVisibility(VISIBLE);
            dateForCustomColor.setVisibility(GONE);
            date.setText(dateFormat);
        } else {
            date.setVisibility(GONE);
            dateForCustomColor.setVisibility(VISIBLE);
            dateForCustomColor.setText(dateFormat);
        }
    }

    @Override
    protected void configureSizerNextAlarm(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
        final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
        final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());
        final TextView nextAlarmForCustomColor = sizer.findViewById(getNextAlarmCustomViewId());

        if (!WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(prefs) || TextUtils.isEmpty(nextAlarmTime)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
            nextAlarmForCustomColor.setVisibility(GONE);
            nextAlarmIconForCustomColor.setVisibility(GONE);
            return;
        }

        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(prefs)) {
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
            nextAlarmIconForCustomColor.setTextColor(WidgetDAO.getDigitalWidgetCustomNextAlarmColor(prefs));
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);
        final TextView labelIcon = sizer.findViewById(getLabelIconViewId());
        final TextView nextAlarmTitleView = sizer.findViewById(getNextAlarmTitleViewId());
        final TextView labelIconForCustomColor = sizer.findViewById(getLabelIconCustomViewId());
        final TextView nextAlarmTitleViewForCustomColor = sizer.findViewById(getNextAlarmTitleCustomViewId());

        if (!WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(prefs) || !WidgetDAO.isNextAlarmTitleDisplayedOnDigitalWidget(prefs)) {
            labelIcon.setVisibility(GONE);
            nextAlarmTitleView.setVisibility(GONE);
            labelIconForCustomColor.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
            return;
        }

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            labelIcon.setVisibility(GONE);
            nextAlarmTitleView.setVisibility(GONE);
            labelIconForCustomColor.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
            return;
        }

        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(prefs)) {
            labelIcon.setVisibility(VISIBLE);
            nextAlarmTitleView.setVisibility(VISIBLE);
            labelIconForCustomColor.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
            labelIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            nextAlarmTitleView.setText(nextAlarmTitle);
        } else {
            int nextAlarmTitleColor = WidgetDAO.getDigitalWidgetCustomNextAlarmTitleColor(prefs);

            labelIcon.setVisibility(GONE);
            nextAlarmTitleView.setVisibility(GONE);
            labelIconForCustomColor.setVisibility(VISIBLE);
            nextAlarmTitleViewForCustomColor.setVisibility(VISIBLE);
            labelIconForCustomColor.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            labelIconForCustomColor.setTextColor(nextAlarmTitleColor);
            nextAlarmTitleView.setText(nextAlarmTitle);
            nextAlarmTitleViewForCustomColor.setTextColor(nextAlarmTitleColor);
        }
    }

    @Override
    protected void configureClockForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isDigitalWidgetDefaultClockColor(prefs)) {
            TextClock clock = sizer.findViewById(getClockViewId());
            clock.setText(WidgetUtils.getLongestTimeString(clock));
            clock.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        } else {
            final TextClock clockForCustomColor = sizer.findViewById(getClockCustomViewId());
            clockForCustomColor.setText(WidgetUtils.getLongestTimeString(clockForCustomColor));
            clockForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
            clockForCustomColor.setText(WidgetUtils.getLongestTimeString(clockForCustomColor));
            clockForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mWidgetFontSizePx);
        }
    }

    @Override
    protected void configureDateForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isDigitalWidgetDefaultDateColor(prefs)) {
            final TextView date = sizer.findViewById(getDateViewId());
            date.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView dateForCustomColor = sizer.findViewById(getDateCustomViewId());
            dateForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }
    }

    @Override
    protected void configureNextAlarmForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(prefs)) {
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

        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(prefs)) {
            final TextView labelIcon = sizer.findViewById(getLabelIconViewId());
            final TextView nextAlarmTitle = sizer.findViewById(getNextAlarmTitleViewId());
            labelIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            labelIcon.setPadding(measuredSizes.mIconPaddingPx, 0, measuredSizes.mIconPaddingPx, 0);
            nextAlarmTitle.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView labelIconForCustomColor = sizer.findViewById(getLabelIconCustomViewId());
            final TextView nextAlarmTitleForCustomColor = sizer.findViewById(getNextAlarmTitleCustomViewId());
            labelIconForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            labelIconForCustomColor.setPadding(measuredSizes.mIconPaddingPx, 0, measuredSizes.mIconPaddingPx, 0);
            nextAlarmTitleForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isDigitalWidgetDefaultClockColor(prefs)) {
            TextClock clock = sizer.findViewById(getClockViewId());
            measuredSizes.mMeasuredTextWidthPx = clock.getMeasuredWidth();
            measuredSizes.mMeasuredTextHeightPx = clock.getMeasuredHeight();
        } else {
            TextClock clockForCustomColor = sizer.findViewById(getClockCustomViewId());
            measuredSizes.mMeasuredTextForCustomColorWidthPx = clockForCustomColor.getMeasuredWidth();
            measuredSizes.mMeasuredTextForCustomColorHeightPx = clockForCustomColor.getMeasuredHeight();
        }

        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(prefs)) {
            TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
            if (nextAlarmIcon.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = WidgetUtils.createBitmap(nextAlarmIcon);
            }
        } else {
            TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());
            if (nextAlarmIconForCustomColor.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = WidgetUtils.createBitmap(nextAlarmIconForCustomColor);
            }
        }

        if (WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(prefs)) {
            TextView labelIcon = sizer.findViewById(getLabelIconViewId());
            if (labelIcon.getVisibility() == VISIBLE) {
                measuredSizes.mLabelBitmap = WidgetUtils.createBitmap(labelIcon);
            }
        } else {
            TextView labelIconForCustomColor = sizer.findViewById(getLabelIconCustomViewId());
            if (labelIconForCustomColor.getVisibility() == VISIBLE) {
                measuredSizes.mLabelBitmap = WidgetUtils.createBitmap(labelIconForCustomColor);
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new DigitalAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

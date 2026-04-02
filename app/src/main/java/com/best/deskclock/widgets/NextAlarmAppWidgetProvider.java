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
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;

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
    protected int getLayoutWithShadowId() {
        return R.layout.appwidget_next_alarm_with_shadow;
    }

    @Override
    protected int getLayoutWithoutShadowId() {
        return R.layout.appwidget_next_alarm;
    }

    @Override
    protected int getSizerLayoutWithShadowId() {
        return R.layout.appwidget_next_alarm_sizer_with_shadow;
    }

    @Override
    protected int getSizerLayoutWithoutShadowId() {
        return R.layout.appwidget_next_alarm_sizer;
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.next_alarm_widget;
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
    protected int getLabelIconViewId() {
        return 0;
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
    protected int getLabelIconCustomViewId() {
        return 0;
    }

    @Override
    protected int getNextAlarmTitleCustomViewId() {
        return R.id.nextAlarmTitleForCustomColor;
    }

    @Override
    protected boolean isTextUppercase(SharedPreferences prefs) {
        return WidgetDAO.isTextUppercaseDisplayedOnNextAlarmWidget(prefs);
    }

    @Override
    protected boolean isTextShadowDisplayed(SharedPreferences prefs) {
        return WidgetDAO.isTextShadowDisplayedOnNextAlarmWidget(prefs);
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
    protected int getCityLayoutId() {
        return 0;
    }

    @Override
    protected boolean isDefaultCityClockColor(SharedPreferences prefs) {
        return true;
    }

    @Override
    protected int getCityClockColor(SharedPreferences prefs) {
        return 0;
    }

    @Override
    protected boolean isDefaultCityNameColor(SharedPreferences prefs) {
        return true;
    }

    @Override
    protected int getCityNameColor(SharedPreferences prefs) {
        return 0;
    }

    @Override
    protected boolean isDefaultCityNoteColor(SharedPreferences prefs) {
        return true;
    }

    @Override
    protected int getCityNoteColor(SharedPreferences prefs) {
        return 0;
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
        final String noAlarmTitle = localizedContext.getString(R.string.next_alarm_widget_title_no_alarm);
        final boolean isDefaultTitleColor = WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getNextAlarmWidgetCustomTitleColor(prefs);

        if (TextUtils.isEmpty(nextAlarmTime)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);

            if (isDefaultTitleColor) {
                rv.setViewVisibility(getNextAlarmTextViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmTextCustomViewId(), GONE);
                rv.setTextViewText(getNextAlarmTextViewId(), isTextUppercase(prefs) ? noAlarmTitle.toUpperCase() : noAlarmTitle);
            } else {
                rv.setViewVisibility(getNextAlarmTextViewId(), GONE);
                rv.setViewVisibility(getNextAlarmTextCustomViewId(), VISIBLE);
                rv.setTextColor(getNextAlarmTextCustomViewId(), customTitleColor);
                rv.setTextViewText(getNextAlarmTextCustomViewId(), isTextUppercase(prefs) ? noAlarmTitle.toUpperCase() : noAlarmTitle);
            }
        } else {
            rv.setViewVisibility(getNextAlarmTextViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTextCustomViewId(), GONE);

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
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
                rv.setTextColor(getNextAlarmCustomViewId(), WidgetDAO.getNextAlarmWidgetCustomAlarmColor(prefs));
                rv.setTextViewText(getNextAlarmCustomViewId(), isTextUppercase(prefs) ? nextAlarmTime.toUpperCase() : nextAlarmTime);
            }
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime, String nextAlarmTitle) {
        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
            return;
        }

        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
            rv.setViewVisibility(getNextAlarmTitleViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
            rv.setTextViewText(getNextAlarmTitleViewId(), isTextUppercase(prefs) ? nextAlarmTitle.toUpperCase() : nextAlarmTitle);
        } else {
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), VISIBLE);
            rv.setTextColor(getNextAlarmTitleCustomViewId(), WidgetDAO.getNextAlarmWidgetCustomAlarmTitleColor(prefs));
            rv.setTextViewText(getNextAlarmTitleCustomViewId(), isTextUppercase(prefs) ? nextAlarmTitle.toUpperCase() : nextAlarmTitle);
        }
    }

    @Override
    protected void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs, int widthPx, int heightPx) {
        if (!WidgetDAO.isBackgroundDisplayedOnNextAlarmWidget(prefs) || widthPx <= 0 || heightPx <= 0) {
            rv.setIcon(R.id.digitalWidgetBackground, METHOD_SET_IMAGE_ICON, null);
            return;
        }

        int radius = (int) dpToPx(WidgetDAO.isNextAlarmWidgetBackgroundCornerRadiusCustomizable(prefs)
            ? WidgetDAO.getNextAlarmWidgetBackgroundCornerRadius(prefs)
            : DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS, context.getResources().getDisplayMetrics());

        int color = WidgetDAO.getNextAlarmWidgetBackgroundColor(prefs);

        boolean isDefaultBackgroundColor = WidgetDAO.isNextAlarmWidgetDefaultBackgroundColor(prefs);

        if (SdkUtils.isAtLeastAndroid12()) {
            if (isDefaultBackgroundColor) {
                Icon dayIcon = WidgetUtils.createRoundedIcon(widthPx, heightPx, WidgetUtils.getBackgroundColorDay(context), radius);

                Icon nightIcon = WidgetUtils.createRoundedIcon(widthPx, heightPx, WidgetUtils.getBackgroundColorNight(context), radius);

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

        final boolean isDefaultTitleColor = WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getNextAlarmWidgetCustomTitleColor(prefs);
        final int customAlarmColor = WidgetDAO.getNextAlarmWidgetCustomAlarmColor(prefs);

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
            nextAlarmText.setVisibility(GONE);
            nextAlarmTextForCustomColor.setVisibility(GONE);

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
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
            return;
        }

        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
            nextAlarmTitleView.setVisibility(VISIBLE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
            nextAlarmTitleView.setText(nextAlarmTitle);
        } else {
            nextAlarmTitleView.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(VISIBLE);
            nextAlarmTitleView.setText(nextAlarmTitle);
            nextAlarmTitleViewForCustomColor.setTextColor(WidgetDAO.getNextAlarmWidgetCustomAlarmTitleColor(prefs));
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
        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
            final TextView nextAlarm = sizer.findViewById(getNextAlarmViewId());
            final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());

            nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
            nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            nextAlarmIcon.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
        } else {
            final TextView nextAlarmForCustomColor = sizer.findViewById(getNextAlarmCustomViewId());
            final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());

            nextAlarmForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
            nextAlarmIconForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            nextAlarmIconForCustomColor.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
        }

        if (WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs)) {
            final TextView nextAlarmText = sizer.findViewById(getNextAlarmTextViewId());
            nextAlarmText.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView nextAlarmTextForCustomColor = sizer.findViewById(getNextAlarmTextCustomViewId());
            nextAlarmTextForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }

        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
            final TextView nextAlarmTitle = sizer.findViewById(getNextAlarmTitleViewId());
            nextAlarmTitle.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView nextAlarmTitleForCustomColor = sizer.findViewById(getNextAlarmTitleCustomViewId());
            nextAlarmTitleForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }
    }

    @Override
    protected void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs) {
        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
            final TextView nextAlarmIcon = sizer.findViewById(getNextAlarmIconId());
            if (nextAlarmIcon.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = WidgetUtils.createBitmap(nextAlarmIcon);
            }
        } else {
            final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());
            if (nextAlarmIconForCustomColor.getVisibility() == VISIBLE) {
                measuredSizes.mIconBitmap = WidgetUtils.createBitmap(nextAlarmIconForCustomColor);
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new NextAlarmAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

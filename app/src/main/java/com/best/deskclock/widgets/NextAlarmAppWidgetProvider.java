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
    protected int getNoAlarmTitleViewId() {
        return R.id.noAlarmTitle;
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
    protected int getNoAlarmTitleCustomViewId() {
        return R.id.noAlarmTitleForCustomColor;
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
    protected String getNextAlarmTime(Context context) {
        return AlarmUtils.getNextAlarm(context);
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
        final boolean isDefaultTitleColor = WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getNextAlarmWidgetCustomTitleColor(prefs);

        if (TextUtils.isEmpty(nextAlarmTime)) {
            rv.setViewVisibility(getNextAlarmViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconId(), GONE);
            rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);

            String noAlarmTitle = localizedContext.getString(R.string.next_alarm_widget_title_no_alarm);
            String noAlarmTitleText = isTextUppercase(prefs) ? noAlarmTitle.toUpperCase() : noAlarmTitle;

            if (isDefaultTitleColor) {
                rv.setViewVisibility(getNoAlarmTitleViewId(), VISIBLE);
                rv.setViewVisibility(getNoAlarmTitleCustomViewId(), GONE);
                rv.setTextViewText(getNoAlarmTitleViewId(), noAlarmTitleText);
            } else {
                rv.setViewVisibility(getNoAlarmTitleViewId(), GONE);
                rv.setViewVisibility(getNoAlarmTitleCustomViewId(), VISIBLE);
                rv.setTextColor(getNoAlarmTitleCustomViewId(), customTitleColor);
                rv.setTextViewText(getNoAlarmTitleCustomViewId(), noAlarmTitleText);
            }
        } else {
            rv.setViewVisibility(getNoAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getNoAlarmTitleCustomViewId(), GONE);

            String nextAlarmText = isTextUppercase(prefs) ? nextAlarmTime.toUpperCase() : nextAlarmTime;

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
                rv.setViewVisibility(getNextAlarmViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmIconId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmCustomViewId(), GONE);
                rv.setViewVisibility(getNextAlarmIconCustomId(), GONE);
                rv.setTextViewText(getNextAlarmViewId(), nextAlarmText);
            } else {
                rv.setViewVisibility(getNextAlarmViewId(), GONE);
                rv.setViewVisibility(getNextAlarmIconId(), GONE);
                rv.setViewVisibility(getNextAlarmCustomViewId(), VISIBLE);
                rv.setViewVisibility(getNextAlarmIconCustomId(), VISIBLE);
                rv.setTextColor(getNextAlarmCustomViewId(), WidgetDAO.getNextAlarmWidgetCustomAlarmColor(prefs));
                rv.setTextViewText(getNextAlarmCustomViewId(), nextAlarmText);
            }
        }
    }

    @Override
    protected void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime, String nextAlarmTitle) {
        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            rv.setViewVisibility(getLabelIconViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getLabelIconCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
            return;
        }

        String nextAlarmTitleText = isTextUppercase(prefs) ? nextAlarmTitle.toUpperCase() : nextAlarmTitle;

        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
            rv.setViewVisibility(getLabelIconViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmTitleViewId(), VISIBLE);
            rv.setViewVisibility(getLabelIconCustomViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), GONE);
            rv.setTextViewText(getNextAlarmTitleViewId(), nextAlarmTitleText);
        } else {
            rv.setViewVisibility(getLabelIconViewId(), GONE);
            rv.setViewVisibility(getNextAlarmTitleViewId(), GONE);
            rv.setViewVisibility(getLabelIconCustomViewId(), VISIBLE);
            rv.setViewVisibility(getNextAlarmTitleCustomViewId(), VISIBLE);
            rv.setTextColor(getNextAlarmTitleCustomViewId(), WidgetDAO.getNextAlarmWidgetCustomAlarmTitleColor(prefs));
            rv.setTextViewText(getNextAlarmTitleCustomViewId(), nextAlarmTitleText);
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
        final TextView noAlarmTitle = sizer.findViewById(getNoAlarmTitleViewId());
        final TextView noAlarmTitleForCustomColor = sizer.findViewById(getNoAlarmTitleCustomViewId());

        final boolean isDefaultTitleColor = WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs);
        final int customTitleColor = WidgetDAO.getNextAlarmWidgetCustomTitleColor(prefs);
        final int customAlarmColor = WidgetDAO.getNextAlarmWidgetCustomAlarmColor(prefs);

        if (TextUtils.isEmpty(nextAlarmTime)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
            nextAlarmForCustomColor.setVisibility(GONE);
            nextAlarmIconForCustomColor.setVisibility(GONE);

            String noAlarm = localizedContext.getString(R.string.next_alarm_widget_title_no_alarm);
            String noAlarmText = isTextUppercase(prefs) ? noAlarm.toUpperCase() : noAlarm;

            if (isDefaultTitleColor) {
                noAlarmTitle.setVisibility(VISIBLE);
                noAlarmTitleForCustomColor.setVisibility(GONE);
                noAlarmTitle.setText(noAlarmText);
            } else {
                noAlarmTitle.setVisibility(GONE);
                noAlarmTitleForCustomColor.setVisibility(VISIBLE);
                noAlarmTitleForCustomColor.setText(noAlarmText);
                noAlarmTitleForCustomColor.setTextColor(customTitleColor);
            }
        } else {
            noAlarmTitle.setVisibility(GONE);
            noAlarmTitleForCustomColor.setVisibility(GONE);

            String nextAlarmText = isTextUppercase(prefs) ? nextAlarmTime.toUpperCase() : nextAlarmTime;

            if (WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(prefs)) {
                nextAlarm.setVisibility(VISIBLE);
                nextAlarmIcon.setVisibility(VISIBLE);
                nextAlarmForCustomColor.setVisibility(GONE);
                nextAlarmIconForCustomColor.setVisibility(GONE);
                nextAlarm.setText(nextAlarmText);
                nextAlarmIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            } else {
                nextAlarm.setVisibility(GONE);
                nextAlarmIcon.setVisibility(GONE);
                nextAlarmForCustomColor.setVisibility(VISIBLE);
                nextAlarmIconForCustomColor.setVisibility(VISIBLE);
                nextAlarmForCustomColor.setText(nextAlarmText);
                nextAlarmForCustomColor.setTextColor(customAlarmColor);
                nextAlarmIconForCustomColor.setTypeface(ClockUtils.getAlarmIconTypeface(context));
                nextAlarmIconForCustomColor.setTextColor(customAlarmColor);
            }
        }
    }

    @Override
    protected void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime) {
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);
        final TextView labelIcon = sizer.findViewById(getLabelIconViewId());
        final TextView nextAlarmTitleView = sizer.findViewById(getNextAlarmTitleViewId());
        final TextView labelIconForCustomColor = sizer.findViewById(getLabelIconCustomViewId());
        final TextView nextAlarmTitleViewForCustomColor = sizer.findViewById(getNextAlarmTitleCustomViewId());

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            labelIcon.setVisibility(GONE);
            nextAlarmTitleView.setVisibility(GONE);
            labelIconForCustomColor.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
            return;
        }

        String nextAlarmTitleText = isTextUppercase(prefs) ? nextAlarmTitle.toUpperCase() : nextAlarmTitle;

        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
            labelIcon.setVisibility(VISIBLE);
            nextAlarmTitleView.setVisibility(VISIBLE);
            labelIconForCustomColor.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
            labelIcon.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            nextAlarmTitleView.setText(nextAlarmTitleText);
        } else {
            int nextAlarmTitleColor = WidgetDAO.getNextAlarmWidgetCustomAlarmTitleColor(prefs);

            labelIcon.setVisibility(GONE);
            nextAlarmTitleView.setVisibility(GONE);
            labelIconForCustomColor.setVisibility(VISIBLE);
            nextAlarmTitleViewForCustomColor.setVisibility(VISIBLE);
            labelIconForCustomColor.setTypeface(ClockUtils.getAlarmIconTypeface(context));
            labelIconForCustomColor.setTextColor(nextAlarmTitleColor);
            nextAlarmTitleViewForCustomColor.setText(nextAlarmTitleText);
            nextAlarmTitleViewForCustomColor.setTextColor(nextAlarmTitleColor);
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
            nextAlarmIcon.setPadding(measuredSizes.mIconPaddingPx, 0, measuredSizes.mIconPaddingPx, 0);
        } else {
            final TextView nextAlarmForCustomColor = sizer.findViewById(getNextAlarmCustomViewId());
            final TextView nextAlarmIconForCustomColor = sizer.findViewById(getNextAlarmIconCustomId());

            nextAlarmForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
            nextAlarmIconForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
            nextAlarmIconForCustomColor.setPadding(measuredSizes.mIconPaddingPx, 0, measuredSizes.mIconPaddingPx, 0);
        }

        if (WidgetDAO.isNextAlarmWidgetDefaultTitleColor(prefs)) {
            final TextView nextAlarmText = sizer.findViewById(getNoAlarmTitleViewId());
            nextAlarmText.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        } else {
            final TextView nextAlarmTextForCustomColor = sizer.findViewById(getNoAlarmTitleCustomViewId());
            nextAlarmTextForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        }

        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
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

        if (WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(prefs)) {
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
        new NextAlarmAppWidgetProvider().relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

}

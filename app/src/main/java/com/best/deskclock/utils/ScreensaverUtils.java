// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

import com.best.deskclock.AnalogClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;

public class ScreensaverUtils {

    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     *
     * @param digitalClock if the view concerned is the digital clock
     * @param analogClock  if the view concerned is the analog clock
     */
    public static void setScreensaverClockStyle(View digitalClock, View analogClock) {
        final DataModel.ClockStyle screensaverClockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        switch (screensaverClockStyle) {
            case ANALOG -> {
                final Context context = analogClock.getContext();
                analogClock.getLayoutParams().height = Utils.toPixel(Utils.isTablet(context) ? 300 : 220, context);
                analogClock.getLayoutParams().width = Utils.toPixel(Utils.isTablet(context) ? 300 : 220, context);
                digitalClock.setVisibility(View.GONE);
                analogClock.setVisibility(View.VISIBLE);
                return;
            }
            case DIGITAL -> {
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + screensaverClockStyle);
    }

    /**
     * For screensaver, dim the color.
     */
    public static void dimScreensaverView(Context context, View view, int color) {
        String colorFilter = getScreensaverColorFilter(context, color);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);

        paint.setColorFilter(new PorterDuffColorFilter(Color.parseColor(colorFilter), PorterDuff.Mode.SRC_IN));

        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * For screensaver, calculate the color filter to use to dim the color.
     *
     * @param color the color selected in the screensaver color picker
     */
    public static String getScreensaverColorFilter(Context context, int color) {
        final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();

        if (areScreensaverClockDynamicColors()) {
            color = context.getColor(R.color.md_theme_inversePrimary);
        }

        String colorFilter = String.format("%06X", 0xFFFFFF & color);
        // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
        String alpha = String.format("%02X", 16 + (192 * brightnessPercentage / 100));

        colorFilter = "#" + alpha + colorFilter;

        return colorFilter;
    }

    public static boolean areScreensaverClockDynamicColors() {
        return DataModel.getDataModel().areScreensaverClockDynamicColors();
    }

    /**
     * For screensaver, configure the clock that is visible to display seconds. The clock that is not visible never
     * displays seconds to avoid it scheduling unnecessary ticking runnable.
     *
     * @param digitalClock if the view concerned is the digital clock
     * @param analogClock  if the view concerned is the analog clock
     */
    public static void setScreensaverClockSecondsEnabled(TextClock digitalClock, AnalogClock analogClock) {
        final boolean areScreensaverClockSecondsDisplayed = DataModel.getDataModel().areScreensaverClockSecondsDisplayed();
        final DataModel.ClockStyle screensaverClockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        switch (screensaverClockStyle) {
            case ANALOG -> {
                setScreensaverTimeFormat(digitalClock, false);
                analogClock.enableSeconds(areScreensaverClockSecondsDisplayed);
                return;
            }
            case DIGITAL -> {
                analogClock.enableSeconds(false);
                setScreensaverTimeFormat(digitalClock, areScreensaverClockSecondsDisplayed);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + screensaverClockStyle);
    }

    /**
     * For screensaver, format the digital clock to be bold and/or italic or not.
     *
     * @param screensaverDigitalClock TextClock to format
     * @param includeSeconds          whether seconds are displayed or not
     */
    public static void setScreensaverTimeFormat(TextClock screensaverDigitalClock, boolean includeSeconds) {
        final boolean isScreensaverDigitalClockInBold = DataModel.getDataModel().isScreensaverDigitalClockInBold();
        final boolean isScreensaverDigitalClockInItalic = DataModel.getDataModel().isScreensaverDigitalClockInItalic();

        if (screensaverDigitalClock == null) {
            return;
        }

        screensaverDigitalClock.setFormat12Hour(
                ClockUtils.get12ModeFormat(screensaverDigitalClock.getContext(), 0.4f, includeSeconds));
        screensaverDigitalClock.setFormat24Hour(
                ClockUtils.get24ModeFormat(screensaverDigitalClock.getContext(), includeSeconds));

        if (isScreensaverDigitalClockInBold && isScreensaverDigitalClockInItalic) {
            screensaverDigitalClock.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        } else if (isScreensaverDigitalClockInBold) {
            screensaverDigitalClock.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (isScreensaverDigitalClockInItalic) {
            screensaverDigitalClock.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        } else {
            screensaverDigitalClock.setTypeface(Typeface.DEFAULT);
        }
    }

    /**
     * For screensaver, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param date Date to format
     */
    public static void setScreensaverDateFormat(TextView date) {
        final boolean isScreensaverDateInBold = DataModel.getDataModel().isScreensaverDateInBold();
        final boolean isScreensaverDateInItalic = DataModel.getDataModel().isScreensaverDateInItalic();

        if (isScreensaverDateInBold && isScreensaverDateInItalic) {
            date.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        } else if (isScreensaverDateInBold) {
            date.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (isScreensaverDateInItalic) {
            date.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        } else {
            date.setTypeface(Typeface.DEFAULT);
        }
    }

    /**
     * For screensaver, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param nextAlarm Next alarm to format
     */
    public static void setScreensaverNextAlarmFormat(TextView nextAlarm) {
        final boolean isScreensaverNextAlarmInBold = DataModel.getDataModel().isScreensaverNextAlarmInBold();
        final boolean isScreensaverNextAlarmInItalic = DataModel.getDataModel().isScreensaverNextAlarmInItalic();
        if (nextAlarm == null) {
            return;
        }
        if (isScreensaverNextAlarmInBold && isScreensaverNextAlarmInItalic) {
            nextAlarm.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        } else if (isScreensaverNextAlarmInBold) {
            nextAlarm.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (isScreensaverNextAlarmInItalic) {
            nextAlarm.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        } else {
            nextAlarm.setTypeface(Typeface.DEFAULT);
        }
    }

    /**
     * For screensaver, set the margins and the style of the clock.
     */
    public static void setScreensaverMarginsAndClockStyle(final Context context, final View clock) {
        final View mainClockView = clock.findViewById(R.id.main_clock);

        // Margins
        final int mainClockMarginLeft = Utils.toPixel(Utils.isTablet(context) ? 20 : 16, context);
        final int mainClockMarginRight = Utils.toPixel(Utils.isTablet(context) ? 20 : 16, context);
        final int mainClockMarginTop = Utils.toPixel(Utils.isTablet(context)
                ? Utils.isLandscape(context) ? 32 : 48
                : Utils.isLandscape(context) ? 16 : 24, context);
        final int mainClockMarginBottom = Utils.toPixel(Utils.isTablet(context) ? 20 : 16, context);
        final ViewGroup.MarginLayoutParams paramsForMainClock = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        paramsForMainClock.setMargins(mainClockMarginLeft, mainClockMarginTop, mainClockMarginRight, mainClockMarginBottom);
        mainClockView.setLayoutParams(paramsForMainClock);

        final int digitalClockMarginBottom = Utils.toPixel(Utils.isTablet(context) ? -18 : -8, context);
        final ViewGroup.MarginLayoutParams paramsForDigitalClock = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        paramsForMainClock.setMargins(0, 0, 0, digitalClockMarginBottom);
        mainClockView.setLayoutParams(paramsForDigitalClock);

        final int analogClockMarginBottom = Utils.toPixel(Utils.isLandscape(context)
                ? 5
                : Utils.isTablet(context) ? 18 : 14, context);
        final ViewGroup.MarginLayoutParams paramsForAnalogClock = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        paramsForMainClock.setMargins(0, 0, 0, analogClockMarginBottom);
        mainClockView.setLayoutParams(paramsForAnalogClock);

        // Style
        final AnalogClock analogClock = mainClockView.findViewById(R.id.analog_clock);
        final TextClock textClock = mainClockView.findViewById(R.id.digital_clock);
        final TextView date = mainClockView.findViewById(R.id.date);
        final TextView nextAlarmIcon = mainClockView.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarm = mainClockView.findViewById(R.id.nextAlarm);
        final int screenSaverClockColorPicker = DataModel.getDataModel().getScreensaverClockColorPicker();
        final int screensaverDateColorPicker = DataModel.getDataModel().getScreensaverDateColorPicker();
        final int screensaverNextAlarmColorPicker = DataModel.getDataModel().getScreensaverNextAlarmColorPicker();

        setScreensaverClockStyle(textClock, analogClock);
        dimScreensaverView(context, textClock, screenSaverClockColorPicker);
        dimScreensaverView(context, analogClock, screenSaverClockColorPicker);
        dimScreensaverView(context, date, screensaverDateColorPicker);
        dimScreensaverView(context, nextAlarmIcon, screensaverNextAlarmColorPicker);
        dimScreensaverView(context, nextAlarm, screensaverNextAlarmColorPicker);
        setScreensaverClockSecondsEnabled(textClock, analogClock);
        setScreensaverDateFormat(date);
        ClockUtils.setClockIconTypeface(nextAlarmIcon);
        setScreensaverNextAlarmFormat(nextAlarm);
    }
}

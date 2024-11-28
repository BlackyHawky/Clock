/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Bitmap.Config.ARGB_8888;

import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLACK_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DARK_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AMOLED_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_DEFAULT_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.LIGHT_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLUE_GRAY_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BROWN_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.GREEN_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.INDIGO_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.ORANGE_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PINK_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PURPLE_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.RED_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.SYSTEM_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.YELLOW_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.YELLOW_NIGHT_ACCENT_COLOR;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Looper;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.AnyRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableKt;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.google.android.material.color.MaterialColors;

import java.io.IOException;
import java.text.NumberFormat;

public class Utils {

    /**
     * {@link Uri} signifying the "silent" ringtone.
     */
    public static final Uri RINGTONE_SILENT = Uri.EMPTY;

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    /**
     * @param resourceId identifies an application resource
     * @return the Uri by which the application resource is accessed
     */
    public static Uri getResourceUri(Context context, @AnyRes int resourceId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .path(String.valueOf(resourceId))
                .build();
    }

    /**
     * @param view the scrollable view to test
     * @return {@code true} iff the {@code view} content is currently scrolled to the top
     */
    public static boolean isScrolledToTop(View view) {
        return !view.canScrollVertically(-1);
    }

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the service
     * @param intent  an Intent describing the service to be started
     * @return a PendingIntent that will start a service
     */
    public static PendingIntent pendingServiceIntent(Context context, Intent intent) {
        return PendingIntent.getService(context, 0, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the activity
     * @param intent  an Intent describing the activity to be started
     * @return a PendingIntent that will start an activity
     */
    public static PendingIntent pendingActivityIntent(Context context, Intent intent) {
        // explicitly set the flag here, as getActivity() documentation states we must do so
        return PendingIntent.getActivity(context, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public static String getNumberFormattedQuantityString(Context context, int id, int quantity) {
        final String localizedQuantity = NumberFormat.getInstance().format(quantity);
        return context.getResources().getQuantityString(id, quantity, localizedQuantity);
    }

    /**
     * This method assumes the given {@code view} has already been layed out.
     *
     * @return a Bitmap containing an image of the {@code view} at its current size
     */
    public static Bitmap createBitmap(View view) {
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * Convenience method for creating card background.
     */
    public static Drawable cardBackground (Context context) {
        final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
        final int radius = toPixel(12, context);
        final GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(radius);
        if (isCardBackgroundDisplayed) {
            gradientDrawable.setColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.BLACK));
        } else {
            gradientDrawable.setColor(Color.TRANSPARENT);
        }

        final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();
        if (isCardBorderDisplayed) {
            gradientDrawable.setShape(GradientDrawable.RECTANGLE);
            gradientDrawable.setStroke(toPixel(2, context),
                    MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
            );
        }

        return gradientDrawable;
    }

    /**
     * Convenience method for scaling Drawable.
     */
    public static BitmapDrawable toScaledBitmapDrawable(Context context, int drawableResId, float scale) {
        final Drawable drawable = AppCompatResources.getDrawable(context, drawableResId);
        if (drawable == null) return null;
        return new BitmapDrawable(context.getResources(), DrawableKt.toBitmap(drawable,
                (int) (scale * drawable.getIntrinsicHeight()), (int) (scale * drawable.getIntrinsicWidth()), null));
    }

    /**
     * Convenience method for converting dp to pixel.
     */
    public static int toPixel(int dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * @param context from which to query the current device configuration
     * @return {@code true} if the device is currently in portrait or reverse portrait orientation
     */
    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    /**
     * @param context from which to query the current device configuration
     * @return {@code true} if the device is currently in landscape or reverse landscape orientation
     */
    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
    }

    /**
     * @param context from which to query the current device
     * @return {@code true} if the device is a tablet
     */
    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.rotateAlarmAlert);
    }

    public static long now() {
        return DataModel.getDataModel().elapsedRealtime();
    }

    public static long wallClock() {
        return DataModel.getDataModel().currentTimeMillis();
    }

    /**
     * @param context The context from which to obtain strings
     * @param hours   Hours to display (if any)
     * @param minutes Minutes to display (if any)
     * @param seconds Seconds to display
     * @return Provided time formatted as a String
     */
    public static String getTimeString(Context context, int hours, int minutes, int seconds) {
        if (hours != 0) {
            return context.getString(R.string.hours_minutes_seconds, hours, minutes, seconds);
        }
        if (minutes != 0) {
            return context.getString(R.string.minutes_seconds, minutes, seconds);
        }
        return context.getString(R.string.seconds, seconds);
    }

    /**
     * Set the vibration duration if the device is equipped with a vibrator and if vibrations are enabled in the settings.
     *
     * @param context to define whether the device is equipped with a vibrator.
     * @param milliseconds Hours to display (if any)
     */
    public static void setVibrationTime(Context context, long milliseconds) {
        final boolean isVibrationsEnabled = DataModel.getDataModel().isVibrationsEnabled();
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator() && isVibrationsEnabled) {
            vibrator.vibrate(milliseconds);
        }
    }

    /**
     * @return {@code true} if the device is in dark mode.
     * @param res Access application resources.
     */
    public static boolean isNight(final Resources res) {
        return (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Apply the theme and the accent color to the activities.
     */
    public static void applyThemeAndAccentColor(final AppCompatActivity activity) {
        final String theme = DataModel.getDataModel().getTheme();
        final String darkMode = DataModel.getDataModel().getDarkMode();
        final String accentColor = DataModel.getDataModel().getAccentColor();
        final boolean isAutoNightAccentColorEnabled = DataModel.getDataModel().isAutoNightAccentColorEnabled();
        final String nightAccentColor = DataModel.getDataModel().getNightAccentColor();

        if (darkMode.equals(KEY_DEFAULT_DARK_MODE)) {
            switch (theme) {
                case SYSTEM_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                case LIGHT_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                case DARK_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } else if (darkMode.equals(KEY_AMOLED_DARK_MODE)
                && !theme.equals(SYSTEM_THEME) || !theme.equals(LIGHT_THEME)) {
                activity.setTheme(R.style.AmoledTheme);
        }

        if (isAutoNightAccentColorEnabled) {
            switch (accentColor) {
                case BLACK_ACCENT_COLOR -> activity.setTheme(R.style.BlackAccentColor);
                case BLUE_GRAY_ACCENT_COLOR -> activity.setTheme(R.style.BlueGrayAccentColor);
                case BROWN_ACCENT_COLOR -> activity.setTheme(R.style.BrownAccentColor);
                case GREEN_ACCENT_COLOR -> activity.setTheme(R.style.GreenAccentColor);
                case INDIGO_ACCENT_COLOR -> activity.setTheme(R.style.IndigoAccentColor);
                case ORANGE_ACCENT_COLOR -> activity.setTheme(R.style.OrangeAccentColor);
                case PINK_ACCENT_COLOR -> activity.setTheme(R.style.PinkAccentColor);
                case PURPLE_ACCENT_COLOR -> activity.setTheme(R.style.PurpleAccentColor);
                case RED_ACCENT_COLOR -> activity.setTheme(R.style.RedAccentColor);
                case YELLOW_ACCENT_COLOR -> activity.setTheme(R.style.YellowAccentColor);
            }
        } else {
            if (isNight(activity.getResources())) {
                switch (nightAccentColor) {
                    case BLACK_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.BlackAccentColor);
                    case BLUE_GRAY_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.BlueGrayAccentColor);
                    case BROWN_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.BrownAccentColor);
                    case GREEN_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.GreenAccentColor);
                    case INDIGO_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.IndigoAccentColor);
                    case ORANGE_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.OrangeAccentColor);
                    case PINK_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.PinkAccentColor);
                    case PURPLE_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.PurpleAccentColor);
                    case RED_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.RedAccentColor);
                    case YELLOW_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.YellowAccentColor);
                }
            } else {
                switch (accentColor) {
                    case BLACK_ACCENT_COLOR -> activity.setTheme(R.style.BlackAccentColor);
                    case BLUE_GRAY_ACCENT_COLOR -> activity.setTheme(R.style.BlueGrayAccentColor);
                    case BROWN_ACCENT_COLOR -> activity.setTheme(R.style.BrownAccentColor);
                    case GREEN_ACCENT_COLOR -> activity.setTheme(R.style.GreenAccentColor);
                    case INDIGO_ACCENT_COLOR -> activity.setTheme(R.style.IndigoAccentColor);
                    case ORANGE_ACCENT_COLOR -> activity.setTheme(R.style.OrangeAccentColor);
                    case PINK_ACCENT_COLOR -> activity.setTheme(R.style.PinkAccentColor);
                    case PURPLE_ACCENT_COLOR -> activity.setTheme(R.style.PurpleAccentColor);
                    case RED_ACCENT_COLOR -> activity.setTheme(R.style.RedAccentColor);
                    case YELLOW_ACCENT_COLOR -> activity.setTheme(R.style.YellowAccentColor);
                }
            }
        }

        if (activity instanceof CollapsingToolbarBaseActivity) {
            if (isNight(activity.getResources()) && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
                activity.getWindow().setNavigationBarColor(Color.BLACK);
                activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            } else {
                activity.getWindow().setNavigationBarColor(
                        MaterialColors.getColor(activity, android.R.attr.colorBackground, Color.BLACK)
                );
            }
        } else {
            if (isNight(activity.getResources()) && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
                activity.getWindow().setNavigationBarColor(Color.BLACK);
                activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            }
        }
    }

    /**
     * @param context The context from which to obtain the duration
     * @param ringtoneUri the ringtone path
     * @return the duration of the ringtone
     */
    public static int getRingtoneDuration(Context context, Uri ringtoneUri) {
        // Using the MediaMetadataRetriever method causes a bug when using the default ringtone:
        // the ringtone stops before the end of the melody.
        // So, we'll use the MediaPlayer class to obtain the ringtone duration.
        // Bug found with debug version on Huawei (Android 12) and Samsung (Android 14) devices.

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, ringtoneUri);
            mediaPlayer.prepare();
            return mediaPlayer.getDuration();
        } catch (IOException e) {
            LogUtils.e("Error while preparing MediaPlayer", e);
            return 0;
        } finally {
            mediaPlayer.release();
        }
    }

    /**
     * Checks if the user is pressing inside of the timer circle or the stopwatch circle.
     */
    public static final class CircleTouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int actionMasked = event.getActionMasked();
            if (actionMasked != MotionEvent.ACTION_DOWN) {
                return false;
            }
            final float rX = view.getWidth() / 2f;
            final float rY = (view.getHeight() - view.getPaddingBottom()) / 2f;
            final float r = Math.min(rX, rY);

            final float x = event.getX() - rX;
            final float y = event.getY() - rY;

            final boolean inCircle = Math.pow(x / r, 2.0) + Math.pow(y / r, 2.0) <= 1.0;

            // Consume the event if it is outside the circle
            return !inCircle;
        }
    }

}

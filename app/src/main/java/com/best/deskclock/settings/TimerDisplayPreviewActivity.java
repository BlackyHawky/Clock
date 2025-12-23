/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.WindowCompat;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.timer.TimerItem;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.io.File;

public class TimerDisplayPreviewActivity extends BaseActivity {

    private SharedPreferences mPrefs;

    private ViewGroup mExpiredTimersView;
    private ImageView mRingtoneIcon;
    private TextView mRingtoneTitle;
    private boolean mIsFadeTransitionsEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getDefaultSharedPreferences(this);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Honor rotation on tablets; fix the orientation on phones.
        if (ThemeUtils.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        setContentView(R.layout.expired_timers_activity);

        mIsFadeTransitionsEnabled = SettingsDAO.isFadeTransitionsEnabled(mPrefs);
        mExpiredTimersView = findViewById(R.id.expired_timers_list);
        final ImageView timerBackgroundImage = findViewById(R.id.timer_background_image);
        final String imagePath = SettingsDAO.getTimerBackgroundImage(mPrefs);

        if (SettingsDAO.isTimerBackgroundTransparent(mPrefs)) {
            timerBackgroundImage.setVisibility(View.GONE);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } else {
            // Apply a background image and a blur effect.
            if (imagePath != null) {
                timerBackgroundImage.setVisibility(View.VISIBLE);

                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        timerBackgroundImage.setImageBitmap(bitmap);

                        if (SdkUtils.isAtLeastAndroid12() && SettingsDAO.isTimerBlurEffectEnabled(mPrefs)) {
                            float intensity = SettingsDAO.getTimerBlurIntensity(mPrefs);
                            RenderEffect blur = RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP);
                            timerBackgroundImage.setRenderEffect(blur);
                        }
                    } else {
                        LogUtils.e("Bitmap null for path: " + imagePath);
                        timerBackgroundImage.setVisibility(View.GONE);
                    }
                } else {
                    LogUtils.e("Image file not found: " + imagePath);
                    timerBackgroundImage.setVisibility(View.GONE);
                }
            } else {
                timerBackgroundImage.setVisibility(View.GONE);
            }
        }

        // Creating a dummy timer
        Timer fakeTimer = new Timer(-1, Timer.State.EXPIRED, 60_000L, 60_000L,
                System.currentTimeMillis(), System.currentTimeMillis(), 0L,
                "Timer preview", "60", false);

        // Add dummy timer to view
        addTimer(fakeTimer);

        if (SettingsDAO.isTimerRingtoneTitleDisplayed(mPrefs)) {
            mRingtoneTitle = findViewById(R.id.ringtone_title);
            mRingtoneIcon = findViewById(R.id.ringtone_icon);
            displayRingtoneTitle();
        }

        AlarmUtils.hideSystemBarsOfTriggeredAlarms(getWindow(), getWindow().getDecorView());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivity();
            }
        });
    }

    /**
     * Display ringtone title if enabled in Timer settings.
     */
    private void displayRingtoneTitle() {
        final boolean silent = RingtoneUtils.RINGTONE_SILENT.equals(DataModel.getDataModel().getTimerRingtoneUri());
        final Drawable iconRingtone = silent
                ? AppCompatResources.getDrawable(this, R.drawable.ic_ringtone_silent)
                : AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        int iconRingtoneSize = (int) dpToPx(24, getResources().getDisplayMetrics());
        final int ringtoneTitleColor = SettingsDAO.getTimerRingtoneTitleColor(mPrefs);
        final int shadowOffset = SettingsDAO.getTimerShadowOffset(mPrefs);
        final float shadowRadius = shadowOffset * 0.5f;
        final int shadowColor = SettingsDAO.getTimerShadowColor(mPrefs);

        if (iconRingtone != null) {
            iconRingtone.setTint(ringtoneTitleColor);

            if (SettingsDAO.isTimerTextShadowDisplayed(mPrefs)) {
                // Convert the drawable to a bitmap
                Bitmap iconBitmap = Bitmap.createBitmap(iconRingtoneSize, iconRingtoneSize, Bitmap.Config.ARGB_8888);
                Canvas iconCanvas = new Canvas(iconBitmap);
                iconRingtone.setBounds(0, 0, iconRingtoneSize, iconRingtoneSize);
                iconRingtone.draw(iconCanvas);

                // Create the alpha mask for the shadow
                Bitmap shadowBitmap = iconBitmap.extractAlpha();
                Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shadowPaint.setColor(shadowColor);
                shadowPaint.setMaskFilter(new BlurMaskFilter(shadowRadius * 1.5f, BlurMaskFilter.Blur.NORMAL));

                // Create the final bitmap with space for the shadow
                int finalWidth = iconRingtoneSize + shadowOffset;
                int finalHeight = iconRingtoneSize + shadowOffset;
                Bitmap finalBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
                Canvas finalCanvas = new Canvas(finalBitmap);

                // Draw the blurred shadow with an offset
                finalCanvas.drawBitmap(shadowBitmap, shadowOffset, shadowOffset, shadowPaint);

                // Draw the normal icon on top
                finalCanvas.drawBitmap(iconBitmap, 0, 0, null);

                // Apply the result to the ImageView
                mRingtoneIcon.setImageBitmap(finalBitmap);

                mRingtoneTitle.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, shadowColor);
            } else {
                mRingtoneIcon.setImageDrawable(iconRingtone);
            }
        }

        mRingtoneTitle.setText(DataModel.getDataModel().getTimerRingtoneTitle());
        mRingtoneTitle.setTypeface(ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs)));
        mRingtoneTitle.setTextColor(ringtoneTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "expired_timers_activity.xml" file)
        mRingtoneTitle.setSelected(true);
    }

    /**
     * Create and add a new view that corresponds with the given {@code timer}.
     */
    private void addTimer(Timer timer) {
        final int timerId = timer.getId();
        final TimerItem timerItem = (TimerItem)
                getLayoutInflater().inflate(R.layout.timer_item, mExpiredTimersView, false);
        // Store the timer id as a tag on the view so it can be located on delete.
        timerItem.setId(timerId);
        timerItem.bindTimer(timer);
        mExpiredTimersView.addView(timerItem);

        // Hide the label hint for expired timers.
        final TextView labelView = timerItem.findViewById(R.id.timer_label);
        labelView.setVisibility(View.VISIBLE);

        // Add logic to hide the 'X' and reset buttons
        final View deleteButton = timerItem.findViewById(R.id.delete_timer);
        deleteButton.setVisibility(View.GONE);
        final View resetButton = timerItem.findViewById(R.id.reset);
        resetButton.setVisibility(View.GONE);

        // Add logic to the "Stop" button
        final View stopButton = timerItem.findViewById(R.id.play_pause);
        stopButton.setOnClickListener(v -> finishActivity());

        // If the first timer was just added, center it.
        centerFirstTimer();
    }

    private void centerFirstTimer() {
        final FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mExpiredTimersView.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        mExpiredTimersView.requestLayout();
    }

    private void finishActivity() {
        finish();
        if (mIsFadeTransitionsEnabled) {
            if (SdkUtils.isAtLeastAndroid14()) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.fade_out);
            } else {
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        } else {
            if (SdkUtils.isAtLeastAndroid14()) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,
                        R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
            } else {
                overridePendingTransition(
                        R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
            }
        }
    }
}

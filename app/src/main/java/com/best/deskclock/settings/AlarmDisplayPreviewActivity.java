/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowCompat;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.AutoSizingTextClock;
import com.best.deskclock.uicomponents.PillView;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.io.File;

public class AlarmDisplayPreviewActivity extends BaseActivity
        implements View.OnClickListener, View.OnTouchListener {

    private static final float TEXT_FADE_START_THRESHOLD = 0.5f;
    private static final int TRANSLATION_DURATION_START_DELAY = 1000;
    private static final int TRANSLATION_DURATION_DELAY = 400;
    private static final int TRANSLATION_DURATION_MILLIS = 1000;
    private static final int ALPHA_DURATION_MILLIS = 400;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2500;

    private SharedPreferences mPrefs;
    private Typeface mGeneralBoldTypeface;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private float mAlarmTitleFontSize;
    private int mAlarmTitleColor;
    private int mAlarmButtonColor;
    private int mDefaultSnoozeMinutes;
    private int mSnoozeMinutes;
    private boolean mIsSwipeActionEnabled;
    private ViewGroup mAlertView;
    private TextView mAlertTitleView;
    private TextView mAlertInfoView;
    private TextView mRingtoneTitle;
    private ImageView mRingtoneIcon;
    private ViewGroup mContentView;
    private ConstraintLayout mSlideZoneLayout;
    private PillView mPillView;
    private MaterialButton mAlarmButton;
    private MaterialButton mSnoozeButton;
    private MaterialButton mDismissButton;
    private TextView mSnoozeActionText;
    private TextView mDismissActionText;
    private boolean mIsSnoozeSelectorDisplayed;
    private ConstraintLayout mSnoozeSelectorLayout;
    private ImageView mSnoozeSelectorPlusButton;
    private ImageView mSnoozeSelectorMinusButton;
    private TextView mSnoozeSelectorText;
    private String[] mSnoozeSelectorEntries;
    private int[] mSnoozeSelectorValues;
    private int mSnoozeSelectorIndex = 0;
    private int mSnoozeMinusButtonColor;
    private int mSnoozePlusButtonColor;
    private int mSnoozeMinusSymbolColor;
    private int mSnoozePlusSymbolColor;
    private Animator mTranslationAnimator;
    private int mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;
    private float mInitialTouchX = 0;
    private Vibrator mVibrator;
    private boolean mAreSnoozedOrDismissedAlarmVibrationsEnabled;
    private boolean mIsFadeTransitionsEnabled;
    private boolean mIsTextShadowDisplayed;
    private int mShadowColor;
    private int mShadowOffset;
    private float mShadowRadius;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getDefaultSharedPreferences(this);
        mGeneralBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs));
        mVibrator = getSystemService(Vibrator.class);
        mAreSnoozedOrDismissedAlarmVibrationsEnabled = SettingsDAO.areSnoozedOrDismissedAlarmVibrationsEnabled(mPrefs);

        // Honor rotation on tablets; fix the orientation on phones.
        if (ThemeUtils.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        initDefaultSnoozeValue();

        setContentView(R.layout.alarm_activity);

        initAlarmBackground();

        mIsSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(mPrefs);
        mIsSnoozeSelectorDisplayed = SettingsDAO.isSnoozeSelectorDisplayed(mPrefs);
        mIsFadeTransitionsEnabled = SettingsDAO.isFadeTransitionsEnabled(mPrefs);
        mAlarmTitleFontSize = SettingsDAO.getAlarmTitleFontSize(mPrefs);
        mAlarmTitleColor = SettingsDAO.getAlarmTitleColor(mPrefs);
        mAlarmButtonColor = SettingsDAO.getAlarmButtonColor(mPrefs, this);
        mSnoozeMinusButtonColor = SettingsDAO.getSnoozeMinusButtonColor(mPrefs);
        mSnoozePlusButtonColor = SettingsDAO.getSnoozePlusButtonColor(mPrefs);
        mSnoozeMinusSymbolColor = SettingsDAO.getSnoozeMinusSymbolColor(mPrefs);
        mSnoozePlusSymbolColor = SettingsDAO.getSnoozePlusSymbolColor(mPrefs);
        mIsTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(mPrefs);
        mShadowColor = SettingsDAO.getAlarmShadowColor(mPrefs);
        mShadowOffset = SettingsDAO.getAlarmShadowOffset(mPrefs);
        mShadowRadius = mShadowOffset * 0.5f;

        mContentView = findViewById(R.id.content);
        mSnoozeButton = mContentView.findViewById(R.id.snooze_button);
        mDismissButton = mContentView.findViewById(R.id.dismiss_button);
        mSlideZoneLayout = mContentView.findViewById(R.id.slide_zone_layout);
        mAlarmButton = mSlideZoneLayout.findViewById(R.id.alarm_button);
        mSnoozeActionText = mSlideZoneLayout.findViewById(R.id.snooze_text);
        mDismissActionText = mSlideZoneLayout.findViewById(R.id.dismiss_text);
        mPillView = mSlideZoneLayout.findViewById(R.id.pill);
        mSnoozeSelectorLayout = mContentView.findViewById(R.id.snooze_selector_layout);

        mAlertView = findViewById(R.id.alert);
        mAlertTitleView = mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = mAlertView.findViewById(R.id.alert_info);

        initAlarmClock();

        initAlarmTitle();

        initDismissOnlyButton();

        if (mIsSwipeActionEnabled) {
            initSlideModeUI();
        } else {
            initButtonModeUI();
        }

        if (mIsSnoozeSelectorDisplayed) {
            initSnoozeSelector();
            updateSnoozeText();
        } else {
            mSnoozeSelectorLayout.setVisibility(GONE);
        }

        initRingtoneTitle();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivity();
            }
        });

        AlarmUtils.hideSystemBarsOfTriggeredAlarms(getWindow(), getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();

        resetAnimations();
    }

    @Override
    public void onClick(View view) {
        // If alarm swiping is disabled in settings, allow snooze/dismiss by tapping on respective buttons.
        if (!mIsSwipeActionEnabled) {
            if (view == mSnoozeButton) {
                snooze();
            } else if (view == mDismissButton) {
                dismiss();
            }
        }

        if (mIsSnoozeSelectorDisplayed) {
            if (view == mSnoozeSelectorPlusButton) {
                if (mSnoozeSelectorIndex < mSnoozeSelectorEntries.length - 1) {
                    mSnoozeSelectorIndex++;
                    updateSnoozeText();
                    updateSnoozeButtonsState();
                }
            } else if (view == mSnoozeSelectorMinusButton) {
                if (mSnoozeSelectorIndex > 0) {
                    mSnoozeSelectorIndex--;
                    updateSnoozeText();
                    updateSnoozeButtonsState();
                }
            }

            mSnoozeMinutes = mSnoozeSelectorValues[mSnoozeSelectorIndex];
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        final int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            // Track the pointer that initiated the touch sequence.
            mInitialPointerIndex = event.getPointerId(event.getActionIndex());

            // Stop the translation
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }

            final int[] contentLocation = {0, 0};
            mContentView.getLocationOnScreen(contentLocation);

            mInitialTouchX = event.getRawX() - contentLocation[0];
        } else if (action == MotionEvent.ACTION_CANCEL) {
            // Clear the pointer index.
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            // Reset everything.
            resetAnimations();
            return true;
        }

        final int actionIndex = event.getActionIndex();
        if (mInitialPointerIndex == MotionEvent.INVALID_POINTER_ID
                || mInitialPointerIndex != event.getPointerId(actionIndex)) {
            // Ignore any pointers other than the initial one, bail early.
            return true;
        }

        final int[] contentLocation = {0, 0};
        mContentView.getLocationOnScreen(contentLocation);

        final float x = event.getRawX() - contentLocation[0];

        float deltaX = x - mInitialTouchX;

        // Limit movement within the parent
        float maxDeltaX = (getAvailableSlideZoneWidth() - mAlarmButton.getWidth()) / 2f;
        deltaX = Math.max(-maxDeltaX, Math.min(deltaX, maxDeltaX));
        mAlarmButton.setTranslationX(deltaX);

        if (Math.abs(deltaX) >= maxDeltaX) {
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }
        }

        updateTextAlpha(deltaX);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            if (mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                if (deltaX <= -maxDeltaX) {
                    dismiss(); // Left = Dismiss in RTL
                } else if (deltaX >= maxDeltaX) {
                    snooze(); // Right = Snooze en RTL
                } else {
                    resetAnimations();
                }
            } else {
                if (deltaX >= maxDeltaX) {
                    dismiss(); // Right = Dismiss in RTL
                } else if (deltaX <= -maxDeltaX) {
                    snooze(); // Left = snooze in LTR
                } else {
                    resetAnimations();
                }
            }
        }

        return true;
    }

    private void initDefaultSnoozeValue() {
        mDefaultSnoozeMinutes = SettingsDAO.getSnoozeLength(mPrefs);
        mSnoozeMinutes = mDefaultSnoozeMinutes;
    }

    /**
     * Initializes the background.
     */
    private void initAlarmBackground() {
        final String getDarkMode = SettingsDAO.getDarkMode(mPrefs);
        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && getDarkMode.equals(AMOLED_DARK_MODE);
        int alarmBackgroundColor = isAmoledMode
                ? SettingsDAO.getAlarmBackgroundAmoledColor(mPrefs)
                : SettingsDAO.getAlarmBackgroundColor(mPrefs);
        final ImageView alarmBackgroundImage = findViewById(R.id.alarm_background_image);
        final String imagePath = SettingsDAO.getAlarmBackgroundImage(mPrefs);

        // Apply a background image and a blur effect.
        if (imagePath != null) {
            alarmBackgroundImage.setVisibility(View.VISIBLE);

            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    alarmBackgroundImage.setImageBitmap(bitmap);
                    alarmBackgroundImage.setColorFilter(alarmBackgroundColor);

                    if (SdkUtils.isAtLeastAndroid12() && SettingsDAO.isAlarmBlurEffectEnabled(mPrefs)) {
                        float intensity = SettingsDAO.getAlarmBlurIntensity(mPrefs);
                        RenderEffect blur = RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP);
                        alarmBackgroundImage.setRenderEffect(blur);
                    }
                } else {
                    LogUtils.e("Bitmap null for path: " + imagePath);
                    getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
                }
            } else {
                LogUtils.e("Image file not found: " + imagePath);
                getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
            }
        } else {
            getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
        }
    }

    /**
     * Initializes the digital or analog clock.
     */
    private void initAlarmClock() {
        final AnalogClock analogClock = findViewById(R.id.analog_clock);
        final AutoSizingTextClock digitalClock = mContentView.findViewById(R.id.digital_clock);
        final DataModel.ClockStyle alarmClockStyle = SettingsDAO.getAlarmClockStyle(mPrefs);
        final boolean isAlarmSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(mPrefs);
        int alarmClockColor = SettingsDAO.getAlarmClockColor(mPrefs);
        float alarmDigitalClockFontSize = SettingsDAO.getAlarmDigitalClockFontSize(mPrefs);

        ClockUtils.setClockStyle(alarmClockStyle, digitalClock, analogClock);

        if (alarmClockStyle == DataModel.ClockStyle.DIGITAL) {
            ClockUtils.setDigitalClockFont(digitalClock, SettingsDAO.getAlarmFont(mPrefs));
            ClockUtils.setDigitalClockTimeFormat(digitalClock, 0.4f, false,
                    true, false, false);
            digitalClock.applyUserPreferredTextSizeSp(alarmDigitalClockFontSize);
            digitalClock.setTextColor(alarmClockColor);

            // Display a shadow if enabled in the settings
            if (mIsTextShadowDisplayed) {
                digitalClock.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            }
        } else {
            ClockUtils.adjustAnalogClockSize(analogClock, mPrefs, true, false, false);
            ClockUtils.setAnalogClockSecondsEnabled(alarmClockStyle, analogClock, isAlarmSecondHandDisplayed);
        }
    }

    /**
     * Initializes the alarm title.
     */
    private void initAlarmTitle() {
        final TextView titleView = mContentView.findViewById(R.id.alarm_title);
        titleView.setText(R.string.app_label);
        titleView.setTypeface(mGeneralBoldTypeface);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        titleView.setTextColor(mAlarmTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        titleView.setSelected(true);

        // Display a shadow if enabled in the settings
        if (mIsTextShadowDisplayed) {
            titleView.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
        }
    }

    /**
     * Initializes the slide mode.
     */
    private void initSlideModeUI() {
        mSlideZoneLayout.setVisibility(VISIBLE);
        mSnoozeButton.setVisibility(GONE);
        mDismissButton.setVisibility(GONE);

        initSlideColors();
        initSlideTexts();
        initSlideAnimations();
    }

    /**
     * Initializes the slide mode colors.
     */
    private void initSlideColors() {
        int slideZoneColor = SettingsDAO.getSlideZoneColor(mPrefs);

        Drawable background = AppCompatResources.getDrawable(this, R.drawable.bg_alarm_slide_zone);
        if (background != null) {
            DrawableCompat.setTint(background, slideZoneColor);
        }

        mSlideZoneLayout.setBackground(background);
        mAlarmButton.setBackgroundColor(mAlarmButtonColor);
    }

    /**
     * Initializes the slide mode texts.
     */
    private void initSlideTexts() {
        mAlarmButton.setContentDescription(getString(R.string.description_direction_both));

        mSnoozeActionText.setTypeface(mGeneralBoldTypeface);
        mSnoozeActionText.setTextColor(SettingsDAO.getSnoozeTitleColor(mPrefs));
        mSnoozeActionText.setText(getString(R.string.button_action_snooze));

        mDismissActionText.setTypeface(mGeneralBoldTypeface);
        mDismissActionText.setTextColor(SettingsDAO.getDismissTitleColor(mPrefs));
        mDismissActionText.setText(getString(R.string.button_action_dismiss));
    }

    /**
     * Initializes the slide mode animations.
     */
    private void initSlideAnimations() {
        mAlarmButton.setOnTouchListener(this);

        mPillView.setFillColor(ColorUtils.setAlphaComponent(mAlarmButtonColor, 128));

        mPillView.post(() -> {
            mPillView.setPillHeight(mAlarmButton.getHeight() - mAlarmButton.getInsetTop() - mAlarmButton.getInsetBottom());
            final float pillStretchWidth = getAvailableSlideZoneWidth() / 2f;
            final int originalFillColor = mPillView.getFillColor();

            // Move to left
            AnimatorSet toLeftAnimator = new AnimatorSet();
            toLeftAnimator.playTogether(
                    translationAnimator(mPillView, pillStretchWidth,
                            mPillView.getPillCenterX() - pillStretchWidth / 2),
                    alphaAnimator(mPillView, originalFillColor)
            );
            toLeftAnimator.setStartDelay(TRANSLATION_DURATION_START_DELAY);
            toLeftAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

            // Apply alpha
            Animator alphaLeft = alphaAnimator(mPillView, ColorUtils.setAlphaComponent(originalFillColor, 0));
            alphaLeft.setDuration(ALPHA_DURATION_MILLIS);

            // Reset position and alpha
            AnimatorSet resetAndRestoreLeft = new AnimatorSet();
            resetAndRestoreLeft.playTogether(
                    translationAnimator(mPillView, 0, mPillView.getPillCenterX()),
                    alphaAnimator(mPillView, originalFillColor)
            );
            resetAndRestoreLeft.setDuration(0);

            // Move to right
            Animator toRightAnimator = translationAnimator(mPillView, pillStretchWidth,
                    mPillView.getPillCenterX() + pillStretchWidth / 2);
            toRightAnimator.setStartDelay(TRANSLATION_DURATION_DELAY);
            toRightAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

            // Apply alpha
            Animator alphaRight = alphaAnimator(mPillView, ColorUtils.setAlphaComponent(originalFillColor, 0));
            alphaRight.setDuration(ALPHA_DURATION_MILLIS);

            // Reset position and alpha
            AnimatorSet resetAndRestoreRight = new AnimatorSet();
            resetAndRestoreRight.playTogether(
                    translationAnimator(mPillView, 0, mPillView.getPillCenterX()),
                    alphaAnimator(mPillView, originalFillColor)
            );
            resetAndRestoreRight.setDuration(0);

            // Sequence
            AnimatorSet translationSequence = new AnimatorSet();
            translationSequence.playSequentially(toLeftAnimator, alphaLeft, resetAndRestoreLeft,
                    toRightAnimator, alphaRight, resetAndRestoreRight);
            translationSequence.setInterpolator(new AccelerateDecelerateInterpolator());
            // Listener to repeat animation if needed
            translationSequence.addListener(new AnimatorListenerAdapter() {

                private boolean wasCancelled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    mPillView.setFillColor(Color.TRANSPARENT);

                    wasCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!wasCancelled && mTranslationAnimator == animation) {
                        mTranslationAnimator.start();
                    }

                    wasCancelled = false;
                }
            });

            mTranslationAnimator = translationSequence;
            mTranslationAnimator.start();
        });
    }

    /**
     * Initializes the button mode.
     */
    private void initButtonModeUI() {
        mSlideZoneLayout.setVisibility(GONE);

        initSnoozeAndDismissButtons();
    }

    /**
     * Initializes the "Dismiss only" button.
     */
    private void initDismissOnlyButton() {
        final MaterialButton dismissOnlyButton = mContentView.findViewById(R.id.dismiss_only_button);
        dismissOnlyButton.setVisibility(GONE);
    }

    /**
     * Initializes the "Snooze" and "Dismiss" buttons.
     */
    private void initSnoozeAndDismissButtons() {
        mSnoozeButton.setBackgroundColor(SettingsDAO.getSnoozeButtonColor(mPrefs, this));
        mSnoozeButton.setText(getString(R.string.button_action_snooze));
        mSnoozeButton.setTypeface(mGeneralBoldTypeface);
        mSnoozeButton.setContentDescription(getString(R.string.description_snooze_button));
        mSnoozeButton.setVisibility(VISIBLE);
        mSnoozeButton.setOnClickListener(this);

        mDismissButton.setBackgroundColor(SettingsDAO.getDismissButtonColor(mPrefs, this));
        mDismissButton.setText(getString(R.string.button_action_dismiss));
        mDismissButton.setTypeface(mGeneralBoldTypeface);
        mDismissButton.setContentDescription(getString(R.string.description_dismiss_button));
        mDismissButton.setVisibility(VISIBLE);
        mDismissButton.setOnClickListener(this);

        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mSnoozeButton.setSelected(true);
        mDismissButton.setSelected(true);
    }

    /**
     * Initializes the snooze selector.
     */
    private void initSnoozeSelector() {
        mSnoozeSelectorPlusButton = mSnoozeSelectorLayout.findViewById(R.id.snooze_selector_plus);
        mSnoozeSelectorMinusButton = mSnoozeSelectorLayout.findViewById(R.id.snooze_selector_minus);
        mSnoozeSelectorText = mSnoozeSelectorLayout.findViewById(R.id.snooze_selector_text);

        mSnoozeSelectorEntries = getResources().getStringArray(R.array.alarm_snooze_selector_entries);

        String[] valuesStr = getResources().getStringArray(R.array.alarm_snooze_selector_values);
        mSnoozeSelectorValues = new int[valuesStr.length];
        for (int i = 0; i < valuesStr.length; i++) {
            if ("-1".equals(valuesStr[i])) {
                mSnoozeSelectorValues[i] = mDefaultSnoozeMinutes;
            } else {
                mSnoozeSelectorValues[i] = Integer.parseInt(valuesStr[i]);
            }
        }

        initSnoozeSelectorStyle();
        initSnoozeSelectorListeners();
        updateSnoozeButtonsState();
        mSnoozeSelectorLayout.setVisibility(VISIBLE);
    }

    /**
     * Initializes the snooze selector style.
     */
    private void initSnoozeSelectorStyle() {
        int snoozeZoneColor = SettingsDAO.getSnoozeZoneColor(mPrefs);
        int snoozeTextColor = SettingsDAO.getSnoozeSelectorTextColor(mPrefs);
        int rippleColor = MaterialColors.getColor(this,
                androidx.appcompat.R.attr.colorControlHighlight, Color.BLACK);

        RippleDrawable rippleTextBackground = new RippleDrawable(ColorStateList.valueOf(rippleColor),
                ThemeUtils.pillBackground(this, snoozeZoneColor), null);

        mSnoozeSelectorText.setBackground(rippleTextBackground);
        mSnoozeSelectorText.setTypeface(mGeneralBoldTypeface);
        mSnoozeSelectorText.setTextColor(snoozeTextColor);

        styleSnoozeButton(mSnoozeSelectorMinusButton, mSnoozeMinusButtonColor, mSnoozeMinusSymbolColor, true);

        styleSnoozeButton(mSnoozeSelectorPlusButton, mSnoozePlusButtonColor, mSnoozePlusSymbolColor, true);
    }

    /**
     * Initializes the snooze selector listeners when the buttons are pressed and the selector area
     * is long-pressed.
     */
    private void initSnoozeSelectorListeners() {
        mSnoozeSelectorText.setOnLongClickListener(v -> {
            snooze();
            return true;
        });

        mSnoozeSelectorMinusButton.setOnClickListener(this);
        mSnoozeSelectorPlusButton.setOnClickListener(this);
    }

    /**
     * Initializes the ringtone title.
     */
    private void initRingtoneTitle() {
        LinearLayout ringtoneLayout = mContentView.findViewById(R.id.ringtone_layout);
        if (SettingsDAO.isRingtoneTitleDisplayed(mPrefs)) {
            ringtoneLayout.setVisibility(VISIBLE);
            mRingtoneTitle = mContentView.findViewById(R.id.ringtone_title);
            mRingtoneIcon = mContentView.findViewById(R.id.ringtone_icon);
            displayRingtoneTitle();
        } else {
            ringtoneLayout.setVisibility(GONE);
        }
    }

    /**
     * Enables or disables the plus and minus buttons based on the current snooze selector index.
     */
    private void updateSnoozeButtonsState() {
        boolean minusEnabled = mSnoozeSelectorIndex > 0;
        boolean plusEnabled = mSnoozeSelectorIndex < mSnoozeSelectorEntries.length - 1;

        styleSnoozeButton(mSnoozeSelectorMinusButton, mSnoozeMinusButtonColor, mSnoozeMinusSymbolColor, minusEnabled);

        styleSnoozeButton(mSnoozeSelectorPlusButton, mSnoozePlusButtonColor, mSnoozePlusSymbolColor, plusEnabled);
    }

    /**
     * Applies visual styling to a snooze button, including background, symbol color, and
     * enabled/disabled state.
     *
     * @param imageView       the button to style
     * @param backgroundColor the background color when enabled
     * @param symbolColor     the symbol color when enabled
     * @param enabled         true to enable the button, false to disable it
     */
    private void styleSnoozeButton(ImageView imageView, int backgroundColor, int symbolColor,
                                   boolean enabled) {

        GradientDrawable circle = (GradientDrawable) ThemeUtils.circleDrawable();
        int rippleColor = MaterialColors.getColor(this,
                androidx.appcompat.R.attr.colorControlHighlight, Color.BLACK);

        if (!enabled) {
            circle.setColor(Color.parseColor("#80808080"));
            imageView.setBackground(circle);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.colorDisabled));
            imageView.setClickable(false);
            return;
        }

        circle.setColor(backgroundColor);
        RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(rippleColor),
                circle, null);

        imageView.setBackground(ripple);
        imageView.setColorFilter(symbolColor);
        imageView.setClickable(true);
    }

    /**
     * Updates the displayed snooze text according to the current selector index.
     */
    private void updateSnoozeText() {
        if (mSnoozeSelectorIndex == 0) {
            String minShort = Utils.getNumberFormattedQuantityString(this, R.plurals.minutes_short, mDefaultSnoozeMinutes);
            mSnoozeSelectorText.setText(getString(R.string.snooze_selector_default, minShort));
        } else {
            mSnoozeSelectorText.setText(mSnoozeSelectorEntries[mSnoozeSelectorIndex]);
        }
    }

    /**
     * Returns the width available for animations or interactions in the slide area,
     * excluding the left and right paddings of the layout.
     */
    private float getAvailableSlideZoneWidth() {
        return mSlideZoneLayout.getWidth() - mSlideZoneLayout.getPaddingStart() - mSlideZoneLayout.getPaddingEnd();
    }

    /**
     * Helper method to create a translation animation.
     */
    private Animator translationAnimator(View view, float targetWidth, float targetCenterX) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(PillView.PILL_WIDTH, targetWidth),
                PropertyValuesHolder.ofFloat(PillView.PILL_CENTER_X, targetCenterX));
    }

    /**
     * Helper method to create an alpha color change animation.
     */
    private Animator alphaAnimator(View view, int alphaColor) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofObject(PillView.FILL_COLOR, AnimatorUtils.ARGB_EVALUATOR, alphaColor));
    }

    /**
     * Apply transparency to "Snooze" and "Dismiss" texts based on movement direction.
     */
    private void updateTextAlpha(float deltaX) {
        final View parentView = (View) mAlarmButton.getParent();
        int parentPaddingHorizontal = parentView.getPaddingStart() + parentView.getPaddingLeft();
        int parentWidth = parentView.getWidth() - (parentPaddingHorizontal);
        float maxDeltaX = parentWidth - mAlarmButton.getWidth();
        maxDeltaX /= 2f; // since the displacement is centered

        float threshold = TEXT_FADE_START_THRESHOLD * maxDeltaX;
        float absDeltaX = Math.abs(deltaX);

        if (absDeltaX <= threshold) {
            mSnoozeActionText.setAlpha(1.0f);
            mDismissActionText.setAlpha(1.0f);
            return;
        }

        float fadeFraction = (absDeltaX - threshold) / (maxDeltaX - threshold);
        fadeFraction = Math.min(fadeFraction, 1.0f);
        float alpha = 1.0f - fadeFraction;

        boolean isRTL = mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        if ((deltaX > 0 && !isRTL) || (deltaX < 0 && isRTL)) {
            // Swipe right (Dismiss right side in LTR, left in RTL)
            mDismissActionText.setAlpha(alpha);
            mSnoozeActionText.setAlpha(1.0f);
        } else {
            // Swipe left (Snooze left in LTR, right in RTL)
            mSnoozeActionText.setAlpha(alpha);
            mDismissActionText.setAlpha(1.0f);
        }
    }

    /**
     * Set animators to initial values, reset text transparency and restart translation on pill view.
     */
    private void resetAnimations() {
        mSnoozeActionText.setAlpha(1.0f);
        mDismissActionText.setAlpha(1.0f);

        mAlarmButton.animate()
                .translationX(0)
                .setDuration(200)
                .start();

        if (mTranslationAnimator != null && !mTranslationAnimator.isRunning()) {
            mTranslationAnimator.start();
        }
    }

    /**
     * Perform snooze animation.
     */
    private void snooze() {
        if (mAreSnoozedOrDismissedAlarmVibrationsEnabled) {
            performDoubleVibration();
        }

        final String infoText = getResources().getQuantityString(
                    R.plurals.alarm_alert_snooze_duration, mSnoozeMinutes, mSnoozeMinutes);
        showAlert(R.string.alarm_alert_snoozed_text, infoText);
    }

    /**
     * Perform dismiss animation.
     */
    private void dismiss() {
        if (mAreSnoozedOrDismissedAlarmVibrationsEnabled) {
            performSingleVibration();
        }
        showAlert(R.string.alarm_alert_off_text, null);
    }

    /**
     * Perform single vibration if alarm is dismissed.
     */
    private void performSingleVibration() {
        if (SdkUtils.isAtLeastAndroid8()) {
            mVibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{700, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            mVibrator.vibrate(new long[]{700, 500}, -1);
        }
    }

    /**
     * Perform double vibration if alarm is snoozed.
     */
    private void performDoubleVibration() {
        if (SdkUtils.isAtLeastAndroid8()) {
            mVibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{700, 200, 100, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            mVibrator.vibrate(new long[]{700, 200, 100, 500}, -1);
        }
    }

    /**
     * Display ringtone title if enabled in <i>"Customize alarm display"</i> settings.
     */
    private void displayRingtoneTitle() {
        final Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        final Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        final Drawable musicIcon = AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        int iconSize = (int) dpToPx(24, getResources().getDisplayMetrics());
        final int ringtoneTitleColor = SettingsDAO.getRingtoneTitleColor(mPrefs);

        if (musicIcon != null) {
            musicIcon.setTint(ringtoneTitleColor);

            if (mIsTextShadowDisplayed) {
                // Convert the drawable to a bitmap
                Bitmap iconBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                Canvas iconCanvas = new Canvas(iconBitmap);
                musicIcon.setBounds(0, 0, iconSize, iconSize);
                musicIcon.draw(iconCanvas);

                // Create the alpha mask for the shadow
                Bitmap shadowBitmap = iconBitmap.extractAlpha();
                Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shadowPaint.setColor(mShadowColor);
                shadowPaint.setMaskFilter(new BlurMaskFilter(mShadowRadius * 1.5f, BlurMaskFilter.Blur.NORMAL));

                // Create the final bitmap with space for the shadow
                int finalWidth = iconSize + mShadowOffset;
                int finalHeight = iconSize + mShadowOffset;
                Bitmap finalBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
                Canvas finalCanvas = new Canvas(finalBitmap);

                // Draw the blurred shadow with an offset
                finalCanvas.drawBitmap(shadowBitmap, mShadowOffset, mShadowOffset, shadowPaint);

                // Draw the normal icon on top
                finalCanvas.drawBitmap(iconBitmap, 0, 0, null);

                // Apply the result to the ImageView
                mRingtoneIcon.setImageBitmap(finalBitmap);

                mRingtoneTitle.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            } else {
                mRingtoneIcon.setImageDrawable(musicIcon);
            }
        }

        mRingtoneTitle.setText(ringtone.getTitle(this));
        mRingtoneTitle.setTypeface(mGeneralBoldTypeface);
        mRingtoneTitle.setTextColor(ringtoneTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mRingtoneTitle.setSelected(true);
    }

    /**
     * Show alert after alarm has been snoozed or dismissed.
     */
    private void showAlert(final int titleResId, final String infoText) {
        mAlertView.setVisibility(VISIBLE);

        mAlertTitleView.setText(titleResId);
        mAlertTitleView.setTypeface(mGeneralBoldTypeface);
        mAlertTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        mAlertTitleView.setTextColor(mAlarmTitleColor);

        if (infoText != null) {
            mAlertInfoView.setVisibility(VISIBLE);
            mAlertInfoView.setText(infoText);
            mAlertInfoView.setTypeface(mGeneralBoldTypeface);
            mAlertInfoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
            mAlertInfoView.setTextColor(mAlarmTitleColor);
        }

        mContentView.setVisibility(GONE);

        mAlertView.setAlpha(0f);
        mAlertView.animate()
                .alpha(1f)
                .setDuration(ALERT_REVEAL_DURATION_MILLIS)
                .withEndAction(() -> mHandler.postDelayed(this::finishActivity, ALERT_DISMISS_DELAY_MILLIS))
                .start();
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

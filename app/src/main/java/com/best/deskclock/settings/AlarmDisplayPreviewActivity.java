/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_BLUR_INTENSITY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.BaseActivity;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmActivityBinding;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.PillView;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.button.MaterialButton;

import java.io.File;

public class AlarmDisplayPreviewActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener {

    private static final int DEFAULT_SNOOZE_VALUE = 10;
    private static final float TEXT_FADE_START_THRESHOLD = 0.5f;
    private static final int TRANSLATION_DURATION_START_DELAY = 1000;
    private static final int TRANSLATION_DURATION_DELAY = 400;
    private static final int TRANSLATION_DURATION_MILLIS = 1000;
    private static final int ALPHA_DURATION_MILLIS = 400;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2500;

    private AlarmActivityBinding mBinding;

    private String mAlarmFontPath;
    private Typeface mAlarmTypeface;
    private Typeface mAlarmBoldTypeface;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsFadeTransition;
    private float mAlarmTitleFontSize;
    private int mAlarmTitleColor;
    private int mAlarmButtonColor;
    private int mDefaultSnoozeMinutes;
    private int mSnoozeMinutes;
    private boolean mIsSwipeActionEnabled;
    private boolean mIsSnoozeSelectorDisplayed;

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
    private boolean mIsTextShadowDisplayed;
    private int mShadowColor;
    private int mShadowOffset;
    private float mShadowRadius;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = AlarmActivityBinding.inflate(getLayoutInflater());

        mVibrator = getSystemService(Vibrator.class);
        mAreSnoozedOrDismissedAlarmVibrationsEnabled = SettingsDAO.areSnoozedOrDismissedAlarmVibrationsEnabled(getPrefs());

        // Honor rotation on tablets; fix the orientation on phones.
        if (isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        initDefaultSnoozeValue();

        setContentView(mBinding.getRoot());

        initAlarmBackground();

        mAlarmFontPath = SettingsDAO.getAlarmFont(getPrefs());
        mIsFadeTransition = SettingsDAO.isFadeTransitionsEnabled(getPrefs());
        mIsSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(getPrefs());
        mIsSnoozeSelectorDisplayed = SettingsDAO.isSnoozeSelectorDisplayed(getPrefs());
        mAlarmTitleFontSize = SettingsDAO.getAlarmTitleFontSize(getPrefs());
        mAlarmTitleColor = SettingsDAO.getAlarmTitleColor(getPrefs());
        mAlarmButtonColor = SettingsDAO.getAlarmButtonColor(getPrefs(), this);
        mSnoozeMinusButtonColor = SettingsDAO.getSnoozeMinusButtonColor(getPrefs());
        mSnoozePlusButtonColor = SettingsDAO.getSnoozePlusButtonColor(getPrefs());
        mSnoozeMinusSymbolColor = SettingsDAO.getSnoozeMinusSymbolColor(getPrefs());
        mSnoozePlusSymbolColor = SettingsDAO.getSnoozePlusSymbolColor(getPrefs());
        mIsTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(getPrefs());
        mShadowColor = SettingsDAO.getAlarmShadowColor(getPrefs());
        mShadowOffset = SettingsDAO.getAlarmShadowOffset(getPrefs());
        mShadowRadius = mShadowOffset * 0.5f;

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
            mBinding.snoozeSelectorLayout.setVisibility(GONE);
        }

        initRingtoneTitle();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivity();
            }
        });

        applyWindowInsets();

        ThemeUtils.hideSystemBars(getWindow(), getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();

        resetAnimations();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        mTranslationAnimator = null;
        mVibrator = null;

        mBinding = null;

        super.onDestroy();
    }

    @Override
    public void onClick(@NonNull View view) {
        // If alarm swiping is disabled in settings, allow snooze/dismiss by tapping on respective buttons.
        if (!mIsSwipeActionEnabled) {
            if (view == mBinding.snoozeButton) {
                snooze();
            } else if (view == mBinding.dismissButton) {
                dismiss();
            }
        }

        if (mIsSnoozeSelectorDisplayed) {
            if (view == mBinding.snoozeSelectorPlus) {
                if (mSnoozeSelectorIndex < mSnoozeSelectorEntries.length - 1) {
                    mSnoozeSelectorIndex++;
                    updateSnoozeText();
                    updateSnoozeButtonsState();
                }
            } else if (view == mBinding.snoozeSelectorMinus) {
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
    public boolean onTouch(@NonNull View view, @NonNull MotionEvent event) {
        final int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            // Track the pointer that initiated the touch sequence.
            mInitialPointerIndex = event.getPointerId(event.getActionIndex());

            // Stop the translation
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }

            final int[] contentLocation = {0, 0};
            mBinding.contentView.getLocationOnScreen(contentLocation);

            mInitialTouchX = event.getRawX() - contentLocation[0];
        } else if (action == MotionEvent.ACTION_CANCEL) {
            // Clear the pointer index.
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            // Reset everything.
            resetAnimations();
            return true;
        }

        final int actionIndex = event.getActionIndex();
        if (mInitialPointerIndex == MotionEvent.INVALID_POINTER_ID || mInitialPointerIndex != event.getPointerId(actionIndex)) {
            // Ignore any pointers other than the initial one, bail early.
            return true;
        }

        final int[] contentLocation = {0, 0};
        mBinding.contentView.getLocationOnScreen(contentLocation);

        final float x = event.getRawX() - contentLocation[0];

        float deltaX = x - mInitialTouchX;

        // Limit movement within the parent
        float maxDeltaX = (getAvailableSlideZoneWidth() - mBinding.alarmButton.getWidth()) / 2f;
        deltaX = Math.max(-maxDeltaX, Math.min(deltaX, maxDeltaX));
        mBinding.alarmButton.setTranslationX(deltaX);

        if (Math.abs(deltaX) >= maxDeltaX) {
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }
        }

        updateTextAlpha(deltaX);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            if (mBinding.contentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
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

    @NonNull
    @Override
    protected UiConfig.Fonts getFontsConfig() {
        return new UiConfig.Fonts(
            getGeneralTypeface(),
            getGeneralBoldTypeface(),
            getAlarmTypeface(),
            null,
            null,
            null
        );
    }

    /**
     * This method adjusts the space occupied by the status bar, and adjust the display of the clock layout accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mBinding.clockLayout, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.displayCutout());

            v.setPadding(0, bars.top, 0, 0);
        });
    }

    private void initDefaultSnoozeValue() {
        mDefaultSnoozeMinutes = DEFAULT_SNOOZE_VALUE;
        mSnoozeMinutes = mDefaultSnoozeMinutes;
    }

    /**
     * Initializes the background.
     */
    private void initAlarmBackground() {
        final String getDarkMode = SettingsDAO.getDarkMode(getPrefs());
        final boolean isAmoledMode = isNight() && getDarkMode.equals(AMOLED_DARK_MODE);
        int alarmBackgroundColor = isAmoledMode
            ? SettingsDAO.getAlarmBackgroundAmoledColor(getPrefs())
            : SettingsDAO.getAlarmBackgroundColor(getPrefs(), this);

        String previewImage = getIntent().getStringExtra(AlarmUtils.EXTRA_PREVIEW_BACKGROUND_IMAGE);
        final String imagePath = TextUtils.isEmpty(previewImage)
            ? SettingsDAO.getAlarmBackgroundImage(getPrefs())
            : previewImage;

        // Apply a background image and a blur effect.
        if (TextUtils.isEmpty(imagePath)) {
            getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
        } else {
            mBinding.alarmBackgroundImage.setVisibility(View.VISIBLE);

            File imageFile = new File(imagePath);

            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    mBinding.alarmBackgroundImage.setImageBitmap(bitmap);

                    if (SdkUtils.isAtLeastAndroid12()) {
                        int blurIntensity =
                            getIntent().getIntExtra(AlarmUtils.EXTRA_PREVIEW_BLUR_INTENSITY, SettingsDAO.getAlarmBlurIntensity(getPrefs()));

                        if (blurIntensity != DEFAULT_BLUR_INTENSITY) {
                            RenderEffect blur = RenderEffect.createBlurEffect(blurIntensity, blurIntensity, Shader.TileMode.CLAMP);

                            mBinding.alarmBackgroundImage.setRenderEffect(blur);
                        }
                    }
                } else {
                    LogUtils.e("Bitmap null for path: " + imagePath);
                    getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
                }
            } else {
                LogUtils.e("Image file not found: " + imagePath);
                getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
            }
        }
    }

    /**
     * Initializes the digital or analog clock.
     */
    private void initAlarmClock() {
        final DataModel.ClockStyle alarmClockStyle = SettingsDAO.getAlarmClockStyle(getPrefs());
        final boolean isAlarmSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(getPrefs());
        int alarmClockColor = SettingsDAO.getAlarmClockColor(getPrefs());
        float alarmDigitalClockFontSize = SettingsDAO.getAlarmDigitalClockFontSize(getPrefs());

        AnalogClock analogClock = mBinding.analogClock;

        analogClock.configure(
            alarmClockStyle,
            SettingsDAO.getAlarmClockDial(getPrefs()),
            SettingsDAO.getAlarmClockDialMaterial(getPrefs()),
            SettingsDAO.getAlarmClockSecondHand(getPrefs()),
            getActiveAccentColor(),
            alarmClockColor,
            SettingsDAO.getAlarmSecondHandColor(getPrefs(), this),
            true
        );

        ClockUtils.setClockStyle(alarmClockStyle, mBinding.digitalClock, analogClock);

        int previewHour = getIntent().getIntExtra(AlarmUtils.EXTRA_PREVIEW_HOUR, -1);
        int previewMinute = getIntent().getIntExtra(AlarmUtils.EXTRA_PREVIEW_MINUTE, -1);

        if (alarmClockStyle == DataModel.ClockStyle.DIGITAL) {
            UiConfig.Fonts fonts = getFontsConfig();
            Typeface alarmFont = fonts.alarmClockFont() != null ? fonts.alarmClockFont() : fonts.general();
            Typeface amPmTypeface = getAlarmBoldTypeface();
            mBinding.digitalClock.setTypeface(alarmFont);
            ClockUtils.setDigitalClockTimeFormat(mBinding.digitalClock, false, 0.4f, amPmTypeface, "sans-serif", Typeface.BOLD, false);
            mBinding.digitalClock.applyUserPreferredTextSizeSp(alarmDigitalClockFontSize);
            mBinding.digitalClock.setTextColor(alarmClockColor);

            // Display a shadow if enabled in the settings
            if (mIsTextShadowDisplayed) {
                mBinding.digitalClock.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            }

            if (previewHour != -1 && previewMinute != -1) {
                mBinding.digitalClock.setStaticTime(previewHour, previewMinute);
            }
        } else {
            ClockUtils.adjustAnalogClockSize(
                mBinding.analogClock, getDisplayMetrics(), SettingsDAO.getAlarmAnalogClockSize(getPrefs()), isLandscape());
            ClockUtils.setAnalogClockSecondsEnabled(alarmClockStyle, mBinding.analogClock, isAlarmSecondHandDisplayed);

            if (previewHour != -1 && previewMinute != -1) {
                mBinding.analogClock.setStaticTime(previewHour, previewMinute);
            }
        }
    }

    /**
     * Initializes the alarm title.
     */
    private void initAlarmTitle() {
        String previewLabel = getIntent().getStringExtra(AlarmUtils.EXTRA_PREVIEW_LABEL);

        mBinding.alarmTitle.setText(TextUtils.isEmpty(previewLabel) ? getString(R.string.app_label) : previewLabel);
        mBinding.alarmTitle.setTypeface(getGeneralBoldTypeface());
        mBinding.alarmTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        mBinding.alarmTitle.setTextColor(mAlarmTitleColor);

        if (SettingsDAO.isAlarmTitleDisplayedOnSingleLine(getPrefs())) {
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mBinding.alarmTitle, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            mBinding.alarmTitle.setSingleLine(true);
            mBinding.alarmTitle.setSelected(true); // Allow text scrolling
            mBinding.alarmTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            mBinding.alarmTitle.setMarqueeRepeatLimit(-1);
            mBinding.alarmTitle.setHorizontallyScrolling(true);
        } else {
            mBinding.alarmTitle.setSingleLine(false);
            mBinding.alarmTitle.setSelected(false);
            mBinding.alarmTitle.setEllipsize(null);
            mBinding.alarmTitle.setHorizontallyScrolling(false);

            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                mBinding.alarmTitle,
                12,
                (int) mAlarmTitleFontSize,
                2,
                TypedValue.COMPLEX_UNIT_SP
            );
        }

        // Display a shadow if enabled in the settings
        if (mIsTextShadowDisplayed) {
            mBinding.alarmTitle.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
        }
    }

    /**
     * Initializes the slide mode.
     */
    private void initSlideModeUI() {
        mBinding.slideZoneLayout.setVisibility(VISIBLE);
        mBinding.snoozeButton.setVisibility(GONE);
        mBinding.dismissButton.setVisibility(GONE);

        initSlideColors();
        initSlideTexts();
        initSlideAnimations();
    }

    /**
     * Initializes the slide mode colors.
     */
    private void initSlideColors() {
        int slideZoneColor = SettingsDAO.getSlideZoneColor(getPrefs());

        Drawable background = AppCompatResources.getDrawable(this, R.drawable.bg_alarm_slide_zone);
        if (background != null) {
            DrawableCompat.setTint(background, slideZoneColor);
        }

        mBinding.slideZoneLayout.setBackground(background);
        mBinding.alarmButton.setBackgroundColor(mAlarmButtonColor);
    }

    /**
     * Initializes the slide mode texts.
     */
    private void initSlideTexts() {
        mBinding.alarmButton.setContentDescription(getString(R.string.description_direction_both));

        mBinding.snoozeText.setTypeface(getGeneralBoldTypeface());
        mBinding.snoozeText.setTextColor(SettingsDAO.getSnoozeTitleColor(getPrefs()));
        mBinding.snoozeText.setText(getString(R.string.button_action_snooze));

        mBinding.dismissText.setTypeface(getGeneralBoldTypeface());
        mBinding.dismissText.setTextColor(SettingsDAO.getDismissTitleColor(getPrefs()));
        mBinding.dismissText.setText(getString(R.string.button_action_dismiss));
    }

    /**
     * Initializes the slide mode animations.
     */
    private void initSlideAnimations() {
        mBinding.alarmButton.setOnTouchListener(this);

        mBinding.pill.setFillColor(ColorUtils.setAlphaComponent(mAlarmButtonColor, 128));

        mBinding.pill.post(() -> {
            mBinding.pill.setPillHeight(mBinding.alarmButton.getHeight()
                - mBinding.alarmButton.getInsetTop()
                - mBinding.alarmButton.getInsetBottom()
            );
            final float pillStretchWidth = getAvailableSlideZoneWidth() / 2f;
            final int originalFillColor = mBinding.pill.getFillColor();

            // Move to left
            AnimatorSet toLeftAnimator = new AnimatorSet();
            toLeftAnimator.playTogether(translationAnimator(mBinding.pill, pillStretchWidth,
                mBinding.pill.getPillCenterX() - pillStretchWidth / 2), alphaAnimator(mBinding.pill, originalFillColor)
            );
            toLeftAnimator.setStartDelay(TRANSLATION_DURATION_START_DELAY);
            toLeftAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

            // Apply alpha
            Animator alphaLeft = alphaAnimator(mBinding.pill, ColorUtils.setAlphaComponent(originalFillColor, 0));
            alphaLeft.setDuration(ALPHA_DURATION_MILLIS);

            // Reset position and alpha
            AnimatorSet resetAndRestoreLeft = new AnimatorSet();
            resetAndRestoreLeft.playTogether(translationAnimator(mBinding.pill, 0, mBinding.pill.getPillCenterX()),
                alphaAnimator(mBinding.pill, originalFillColor)
            );
            resetAndRestoreLeft.setDuration(0);

            // Move to right
            Animator toRightAnimator = translationAnimator(mBinding.pill, pillStretchWidth,
                mBinding.pill.getPillCenterX() + pillStretchWidth / 2);
            toRightAnimator.setStartDelay(TRANSLATION_DURATION_DELAY);
            toRightAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

            // Apply alpha
            Animator alphaRight = alphaAnimator(mBinding.pill, ColorUtils.setAlphaComponent(originalFillColor, 0));
            alphaRight.setDuration(ALPHA_DURATION_MILLIS);

            // Reset position and alpha
            AnimatorSet resetAndRestoreRight = new AnimatorSet();
            resetAndRestoreRight.playTogether(translationAnimator(mBinding.pill, 0, mBinding.pill.getPillCenterX()),
                alphaAnimator(mBinding.pill, originalFillColor)
            );
            resetAndRestoreRight.setDuration(0);

            // Sequence
            AnimatorSet translationSequence = new AnimatorSet();
            translationSequence.playSequentially(
                toLeftAnimator, alphaLeft, resetAndRestoreLeft, toRightAnimator, alphaRight, resetAndRestoreRight);
            translationSequence.setInterpolator(new AccelerateDecelerateInterpolator());
            // Listener to repeat animation if needed
            translationSequence.addListener(new AnimatorListenerAdapter() {

                private boolean wasCancelled = false;

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                    mBinding.pill.setFillColor(Color.TRANSPARENT);

                    wasCancelled = true;
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
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
        mBinding.slideZoneLayout.setVisibility(INVISIBLE);

        initSnoozeAndDismissButtons();
    }

    /**
     * Initializes the "Dismiss only" button.
     */
    private void initDismissOnlyButton() {
        mBinding.dismissOnlyButton.setVisibility(GONE);
    }

    /**
     * Initializes the "Snooze" and "Dismiss" buttons.
     */
    private void initSnoozeAndDismissButtons() {
        mBinding.snoozeButton.setBackgroundColor(SettingsDAO.getSnoozeButtonColor(getPrefs(), this));
        mBinding.snoozeButton.setText(getString(R.string.button_action_snooze));
        mBinding.snoozeButton.setTypeface(getGeneralBoldTypeface());
        mBinding.snoozeButton.setContentDescription(getString(R.string.description_snooze_button));
        mBinding.snoozeButton.setVisibility(VISIBLE);
        mBinding.snoozeButton.setOnClickListener(this);

        mBinding.dismissButton.setBackgroundColor(SettingsDAO.getDismissButtonColor(getPrefs(), this));
        mBinding.dismissButton.setText(getString(R.string.button_action_dismiss));
        mBinding.dismissButton.setTypeface(getGeneralBoldTypeface());
        mBinding.dismissButton.setContentDescription(getString(R.string.description_dismiss_button));
        mBinding.dismissButton.setVisibility(VISIBLE);
        mBinding.dismissButton.setOnClickListener(this);

        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mBinding.snoozeButton.setSelected(true);
        mBinding.dismissButton.setSelected(true);
    }

    /**
     * Initializes the snooze selector.
     */
    private void initSnoozeSelector() {
        mSnoozeSelectorValues = getResources().getIntArray(R.array.alarm_snooze_selector_values);
        mSnoozeSelectorEntries = new String[mSnoozeSelectorValues.length];

        for (int i = 0; i < mSnoozeSelectorValues.length; i++) {
            int snoozeValue = mSnoozeSelectorValues[i];

            if (snoozeValue == -1) {
                String defaultTimeStr = buildTimeString(mDefaultSnoozeMinutes);
                mSnoozeSelectorEntries[i] = String.format("%s (%s)", getString(R.string.label_default), defaultTimeStr);
            } else {
                String timeStr = buildTimeString(snoozeValue);
                mSnoozeSelectorEntries[i] = getString(R.string.alarm_alert_snooze_text) + " " + timeStr;
            }
        }

        initSnoozeSelectorStyle();
        initSnoozeSelectorListeners();
        updateSnoozeButtonsState();
        mBinding.snoozeSelectorLayout.setVisibility(VISIBLE);
    }

    /**
     * Initializes the snooze selector style.
     */
    private void initSnoozeSelectorStyle() {
        int snoozeZoneColor = SettingsDAO.getSnoozeZoneColor(getPrefs());
        int snoozeTextColor = SettingsDAO.getSnoozeSelectorTextColor(getPrefs());

        mBinding.snoozeSelectorText.setBackgroundTintList(ColorStateList.valueOf(snoozeZoneColor));
        mBinding.snoozeSelectorText.setTypeface(getGeneralBoldTypeface());
        mBinding.snoozeSelectorText.setTextColor(snoozeTextColor);

        styleSnoozeButton(mBinding.snoozeSelectorMinus, mSnoozeMinusButtonColor, mSnoozeMinusSymbolColor, true);

        styleSnoozeButton(mBinding.snoozeSelectorPlus, mSnoozePlusButtonColor, mSnoozePlusSymbolColor, true);
    }

    /**
     * Initializes the snooze selector listeners when the buttons are pressed and the selector area
     * is long-pressed.
     */
    private void initSnoozeSelectorListeners() {
        mBinding.snoozeSelectorText.setOnLongClickListener(v -> {
            snooze();
            return true;
        });

        mBinding.snoozeSelectorMinus.setOnClickListener(this);
        mBinding.snoozeSelectorPlus.setOnClickListener(this);
    }

    /**
     * Initializes the ringtone title.
     */
    private void initRingtoneTitle() {
        if (SettingsDAO.isRingtoneTitleDisplayed(getPrefs())) {
            mBinding.ringtoneLayout.setVisibility(VISIBLE);

            displayRingtoneTitle();
        } else {
            mBinding.ringtoneLayout.setVisibility(GONE);
        }
    }

    /**
     * Lazy loading for the standard alarm font.
     *
     * @return the alarm font.
     */
    protected final Typeface getAlarmTypeface() {
        if (mAlarmTypeface == null) {
            mAlarmTypeface = ThemeUtils.loadFont(mAlarmFontPath);
        }

        return mAlarmTypeface;
    }

    /**
     * Lazy loading for the bold alarm font (used for AM/PM).
     *
     * @return the bold alarm font.
     */
    protected final Typeface getAlarmBoldTypeface() {
        if (mAlarmBoldTypeface == null) {
            mAlarmBoldTypeface = ThemeUtils.boldTypeface(mAlarmFontPath);
        }
        return mAlarmBoldTypeface;
    }

    /**
     * Enables or disables the plus and minus buttons based on the current snooze selector index.
     */
    private void updateSnoozeButtonsState() {
        boolean minusEnabled = mSnoozeSelectorIndex > 0;
        boolean plusEnabled = mSnoozeSelectorIndex < mSnoozeSelectorEntries.length - 1;

        styleSnoozeButton(mBinding.snoozeSelectorMinus, mSnoozeMinusButtonColor, mSnoozeMinusSymbolColor, minusEnabled);

        styleSnoozeButton(mBinding.snoozeSelectorPlus, mSnoozePlusButtonColor, mSnoozePlusSymbolColor, plusEnabled);
    }

    /**
     * Applies visual styling to a snooze button, including background, symbol color, and
     * enabled/disabled state.
     *
     * @param button          the button to style
     * @param backgroundColor the background color when enabled
     * @param symbolColor     the symbol color when enabled
     * @param enabled         true to enable the button, false to disable it
     */
    private void styleSnoozeButton(@NonNull MaterialButton button, int backgroundColor, int symbolColor, boolean enabled) {
        button.setEnabled(enabled);
        button.setBackgroundTintList(ColorStateList.valueOf(enabled ? backgroundColor : Color.parseColor("#80808080")));
        button.setIconTint(ColorStateList.valueOf(enabled ? symbolColor : Color.parseColor("#60E6E0E9")));
    }

    private String buildTimeString(int totalMinutes) {
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;

        if (hour > 0) {
            return FormattedTextUtils.getNumberFormattedQuantityString(this, R.plurals.hours_short, hour);
        } else {
            return FormattedTextUtils.getNumberFormattedQuantityString(this, R.plurals.minutes_short, minute);
        }
    }

    /**
     * Updates the displayed snooze text according to the current selector index.
     */
    private void updateSnoozeText() {
        if (!mIsSnoozeSelectorDisplayed || mSnoozeSelectorEntries == null) {
            return;
        }

        mBinding.snoozeSelectorText.setText(mSnoozeSelectorEntries[mSnoozeSelectorIndex]);
    }

    /**
     * Returns the width available for animations or interactions in the slide area,
     * excluding the left and right paddings of the layout.
     */
    private float getAvailableSlideZoneWidth() {
        return mBinding.slideZoneLayout.getWidth() - mBinding.slideZoneLayout.getPaddingStart() - mBinding.slideZoneLayout.getPaddingEnd();
    }

    /**
     * Helper method to create a translation animation.
     */
    @NonNull
    private Animator translationAnimator(@NonNull View view, float targetWidth, float targetCenterX) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
            PropertyValuesHolder.ofFloat(PillView.PILL_WIDTH, targetWidth),
            PropertyValuesHolder.ofFloat(PillView.PILL_CENTER_X, targetCenterX));
    }

    /**
     * Helper method to create an alpha color change animation.
     */
    @NonNull
    private Animator alphaAnimator(@NonNull View view, int alphaColor) {
        return ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofObject(
            PillView.FILL_COLOR, AnimatorUtils.ARGB_EVALUATOR, alphaColor));
    }

    /**
     * Apply transparency to "Snooze" and "Dismiss" texts based on movement direction.
     */
    private void updateTextAlpha(float deltaX) {
        final View parentView = (View) mBinding.alarmButton.getParent();
        int parentPaddingHorizontal = parentView.getPaddingStart() + parentView.getPaddingLeft();
        int parentWidth = parentView.getWidth() - (parentPaddingHorizontal);
        float maxDeltaX = parentWidth - mBinding.alarmButton.getWidth();
        maxDeltaX /= 2f; // since the displacement is centered

        float threshold = TEXT_FADE_START_THRESHOLD * maxDeltaX;
        float absDeltaX = Math.abs(deltaX);

        if (absDeltaX <= threshold) {
            mBinding.snoozeText.setAlpha(1.0f);
            mBinding.dismissText.setAlpha(1.0f);
            return;
        }

        float fadeFraction = (absDeltaX - threshold) / (maxDeltaX - threshold);
        fadeFraction = Math.min(fadeFraction, 1.0f);
        float alpha = 1.0f - fadeFraction;

        boolean isRTL = mBinding.contentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        if ((deltaX > 0 && !isRTL) || (deltaX < 0 && isRTL)) {
            // Swipe right (Dismiss right side in LTR, left in RTL)
            mBinding.dismissText.setAlpha(alpha);
            mBinding.snoozeText.setAlpha(1.0f);
        } else {
            // Swipe left (Snooze left in LTR, right in RTL)
            mBinding.snoozeText.setAlpha(alpha);
            mBinding.dismissText.setAlpha(1.0f);
        }
    }

    /**
     * Set animators to initial values, reset text transparency and restart translation on pill view.
     */
    private void resetAnimations() {
        mBinding.snoozeText.setAlpha(1.0f);
        mBinding.dismissText.setAlpha(1.0f);

        mBinding.alarmButton.animate()
            .translationX(0)
            .setDuration(AnimatorUtils.SHORT_ANIMATION_DURATION)
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

        displayAlarmActionMessage(R.string.alarm_alert_snoozed_text, buildTimeString(mSnoozeSelectorIndex == 0 ? DEFAULT_SNOOZE_VALUE : mSnoozeMinutes));
    }

    /**
     * Perform dismiss animation.
     */
    private void dismiss() {
        if (mAreSnoozedOrDismissedAlarmVibrationsEnabled) {
            performSingleVibration();
        }

        displayAlarmActionMessage(R.string.alarm_alert_off_text, null);
    }

    /**
     * Perform single vibration if alarm is dismissed.
     */
    private void performSingleVibration() {
        if (SdkUtils.isAtLeastAndroid8()) {
            mVibrator.vibrate(VibrationEffect.createWaveform(new long[]{700, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //noinspection deprecation
            mVibrator.vibrate(new long[]{700, 500}, -1);
        }
    }

    /**
     * Perform double vibration if alarm is snoozed.
     */
    private void performDoubleVibration() {
        if (SdkUtils.isAtLeastAndroid8()) {
            mVibrator.vibrate(VibrationEffect.createWaveform(new long[]{700, 200, 100, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //noinspection deprecation
            mVibrator.vibrate(new long[]{700, 200, 100, 500}, -1);
        }
    }

    /**
     * Display ringtone title if enabled in <i>"Customize alarm display"</i> settings.
     */
    private void displayRingtoneTitle() {
        final String previewRingtoneStr = getIntent().getStringExtra(AlarmUtils.EXTRA_PREVIEW_RINGTONE);
        final Uri ringtoneUri;
        String ringtoneTitleText;
        final Drawable musicIcon;

        if (previewRingtoneStr == null) {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            ringtoneUri = previewRingtoneStr.isEmpty() ? null : Uri.parse(previewRingtoneStr);
        }

        if (ringtoneUri == null) {
            ringtoneTitleText = getString(R.string.silent_ringtone_title);
            musicIcon = AppCompatResources.getDrawable(this, R.drawable.ic_ringtone_silent);
        } else {
            ringtoneTitleText = getDataModel().getRingtoneTitle(ringtoneUri);
            musicIcon = AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        }

        int iconSize = (int) dpToPx(24, getDisplayMetrics());
        final int ringtoneTitleColor = SettingsDAO.getRingtoneTitleColor(getPrefs());

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
                mBinding.ringtoneIcon.setImageBitmap(finalBitmap);

                mBinding.ringtoneTitle.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            } else {
                mBinding.ringtoneIcon.setImageDrawable(musicIcon);
            }
        }

        mBinding.ringtoneTitle.setText(ringtoneTitleText);
        mBinding.ringtoneTitle.setTypeface(getGeneralBoldTypeface());
        mBinding.ringtoneTitle.setTextColor(ringtoneTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mBinding.ringtoneTitle.setSelected(true);
    }

    /**
     * Display a message after snoozing or dismissing the alarm.
     */
    private void displayAlarmActionMessage(int titleResId, @Nullable String descriptionText) {
        if (SettingsDAO.isAlarmActionMessageHidden(getPrefs())) {
            finishActivity();
            return;
        }

        mBinding.contentView.setVisibility(GONE);

        mBinding.actionMessageView.setVisibility(VISIBLE);

        mBinding.actionTitle.setText(titleResId);
        mBinding.actionTitle.setTypeface(getGeneralBoldTypeface());
        mBinding.actionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        mBinding.actionTitle.setTextColor(mAlarmTitleColor);

        if (descriptionText != null) {
            mBinding.actionDescription.setVisibility(VISIBLE);
            mBinding.actionDescription.setText(descriptionText);
            mBinding.actionDescription.setTypeface(getGeneralBoldTypeface());
            mBinding.actionDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
            mBinding.actionDescription.setTextColor(mAlarmTitleColor);
        }

        mBinding.actionMessageView.setAlpha(0f);
        mBinding.actionMessageView.animate()
            .alpha(1f)
            .setDuration(ALERT_REVEAL_DURATION_MILLIS)
            .withEndAction(() -> mHandler.postDelayed(this::finishActivity, ALERT_DISMISS_DELAY_MILLIS))
            .start();
    }

    private void finishActivity() {
        ThemeUtils.finishActivityWithTransition(this, mIsFadeTransition);
    }

}

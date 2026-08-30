/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.stopwatch;

import static android.R.attr.state_activated;
import static android.R.attr.state_pressed;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SW_ACTION;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_LAP;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_RESET;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_SHARE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_START_PAUSE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_DISPLAY_MILLISECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.uidata.UiDataModel.Tab.STOPWATCH;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.base.DeskClockFragment;
import com.best.deskclock.base.RunnableFragment;
import com.best.deskclock.data.Lap;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.data.StopwatchListener;
import com.best.deskclock.databinding.StopwatchFragmentBinding;
import com.best.deskclock.events.Events;
import com.best.deskclock.uicomponents.CustomTooltip;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/**
 * Fragment that shows the stopwatch and recorded laps.
 */
public final class StopwatchFragment extends DeskClockFragment implements RunnableFragment {

    private StopwatchFragmentBinding mBinding;

    /**
     * Milliseconds between redraws while running.
     */
    private static final int REDRAW_PERIOD_RUNNING = 25;

    /**
     * Milliseconds between redraws while running (without milliseconds displayed).
     */
    private static final int REDRAW_PERIOD_RUNNING_WITHOUT_MS = 100;

    /**
     * Milliseconds between redraws while paused.
     */
    private static final int REDRAW_PERIOD_PAUSED = 500;

    /**
     * Scheduled to update the stopwatch time and current lap time while stopwatch is running.
     */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /**
     * Updates the user interface in response to stopwatch changes.
     */
    private final StopwatchListener mStopwatchWatcher = new StopwatchWatcher();

    /**
     * The data source for the lap list.
     */
    private LapsAdapter mLapsAdapter;

    /**
     * The layout manager for the {@link #mLapsAdapter}.
     */
    private LinearLayoutManager mLapsLayoutManager;

    /**
     * Formats and displays the text in the stopwatch.
     */
    private StopwatchTextController mStopwatchTextController;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public StopwatchFragment() {
        super(STOPWATCH);
    }

    private String mStopwatchFontPath;
    private Typeface mStopwatchTypeface;
    private boolean mAreMillisecondsDisplayed;
    private String mVolumeUpAction;
    private String mVolumeUpActionAfterLongPress;
    private String mVolumeDownAction;
    private String mVolumeDownActionAfterLongPress;
    private boolean mIsVolumeUpLongPressed;
    private boolean mIsVolumeDownLongPressed;
    private boolean mAreSettingsChanged = false;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = (prefs, key) -> {
        if (key != null) {
            switch (key) {
                case KEY_SW_FONT, KEY_SW_DISPLAY_MILLISECONDS, KEY_SW_VOLUME_UP_ACTION, KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS,
                     KEY_SW_VOLUME_DOWN_ACTION, KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS -> {

                    mAreSettingsChanged = true;

                    if (isResumed()) {
                        applySettingsChanges();
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        refreshSettings();
    }

    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = StopwatchFragmentBinding.inflate(inflater, container, false);

        mBinding.stopwatchTimeWrapper.setOnTouchListener(new Utils.CircleTouchListener());
        mBinding.stopwatchTimeWrapper.setOnClickListener(new TimeClickListener());

        final int colorAccent = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mBinding.stopwatchTimeLayout.stopwatchTimeText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
            new int[][]{{-state_activated, -state_pressed}, {}},
            new int[]{textColorPrimary, colorAccent});
        mBinding.stopwatchTimeLayout.stopwatchTimeText.setTextColor(timeTextColor);
        mBinding.stopwatchTimeLayout.stopwatchHundredthsText.setTextColor(timeTextColor);

        UiConfig.CardStyle cardStyle = getCardStyleConfig();

        mBinding.lapsBackground.setBackground(ThemeUtils.cardBackground(requireContext(), getDisplayMetrics(),
            cardStyle.isBackgroundDisplayed(), cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode()));

        RecyclerView.ItemAnimator animator = mBinding.lapsList.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            // Disable flash/blinking during updates (notifyItemChanged)
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mLapsLayoutManager = new LinearLayoutManager(requireContext());
        mBinding.lapsList.setLayoutManager(mLapsLayoutManager);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        refreshSettings();

        applyStopwatchFont();

        applyMillisecondsVisibility();

        // Handle header text font
        TextView[] titles = {
            mBinding.lapHeaderLayout.lapTitle,
            mBinding.lapHeaderLayout.splitTitle,
            mBinding.lapHeaderLayout.totalTitle
        };
        for (TextView tv : titles) {
            tv.setTypeface(getGeneralBoldTypeface());
        }

        // Timer text serves as a virtual start/stop button.
        mStopwatchTextController = new StopwatchTextController(
            mBinding.stopwatchTimeLayout.stopwatchTimeText, mBinding.stopwatchTimeLayout.stopwatchHundredthsText);

        mStopwatchTextController.setMillisecondsDisplayed(mAreMillisecondsDisplayed);

        mLapsAdapter = new LapsAdapter(requireContext(), getDataModel(), getUiDataModel(), getFontsConfig());
        mBinding.lapsList.setAdapter(mLapsAdapter);

        getDataModel().addStopwatchListener(mStopwatchWatcher);

        updateTime();
        showOrHideLaps(getStopwatch().isReset());

        getPrefs().registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAreSettingsChanged) {
            applySettingsChanges();
        }

        final Intent intent = requireActivity().getIntent();
        if (intent != null) {
            final String action = intent.getAction();
            if (StopwatchService.ACTION_START_STOPWATCH.equals(action)) {
                getDataModel().startStopwatch();
                // Consume the intent
                requireActivity().setIntent(null);
            } else if (StopwatchService.ACTION_PAUSE_STOPWATCH.equals(action)) {
                getDataModel().pauseStopwatch();
                // Consume the intent
                requireActivity().setIntent(null);
            }
        }

        if (getView() != null) {
            getView().post(() -> {
                if (!isAdded()) {
                    return;
                }

                // Conservatively assume the data in the adapter has changed while the fragment was paused.
                mLapsAdapter.notifyDataSetChanged();

                // Synchronize the user interface with the data model.
                updateUI(FAB_AND_BUTTONS_IMMEDIATE);
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop all updates while the fragment is not visible.
        stopUpdatingTime();
    }

    @Override
    public void onDestroyView() {
        mBinding.stopwatchTimeWrapper.setOnClickListener(null);

        getDataModel().removeStopwatchListener(mStopwatchWatcher);

        getPrefs().unregisterOnSharedPreferenceChangeListener(mPrefListener);

        mBinding.lapsList.setAdapter(null);

        mStopwatchTextController = null;

        mAreSettingsChanged = false;

        mBinding = null;
        mLapsLayoutManager = null;

        super.onDestroyView();
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (mVolumeUpAction.equals(DEFAULT_SW_ACTION) && mVolumeUpActionAfterLongPress.equals(DEFAULT_SW_ACTION)) {
                        return false;
                    }
                    mIsVolumeUpLongPressed = event.getRepeatCount() >= 2;
                    return true;

                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (mVolumeDownAction.equals(DEFAULT_SW_ACTION) && mVolumeDownActionAfterLongPress.equals(DEFAULT_SW_ACTION)) {
                        return false;
                    }
                    mIsVolumeDownLongPressed = event.getRepeatCount() >= 2;
                    return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (mIsVolumeUpLongPressed) {
                    getVolumeUpActionAfterLongPress();
                } else {
                    getVolumeUpAction();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (mIsVolumeDownLongPressed) {
                    getVolumeDownActionAfterLongPress();
                } else {
                    getVolumeDownAction();
                }
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onFabClick() {
        toggleStopwatchState();
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        updateFab(fab);
    }

    @Override
    public void onMorphFab(@NonNull ImageView fab) {
        // Update the fab's drawable to match the current timer state.
        updateFab(fab);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setClickable(true);
        left.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_reset));
        left.setContentDescription(getString(R.string.reset));
        left.setOnClickListener(v -> doReset());

        switch (getStopwatch().getState()) {
            case RESET -> {
                left.setVisibility(INVISIBLE);
                right.setClickable(true);
                right.setVisibility(INVISIBLE);
            }
            case RUNNING -> {
                left.setVisibility(VISIBLE);
                final boolean canRecordLaps = canRecordMoreLaps();
                right.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_tab_stopwatch_static));
                right.setContentDescription(getString(R.string.sw_lap_button));
                right.setClickable(canRecordLaps);
                right.setVisibility(canRecordLaps ? VISIBLE : INVISIBLE);
                right.setOnClickListener(v -> doAddLap());
            }
            case PAUSED -> {
                left.setVisibility(VISIBLE);
                right.setClickable(true);
                right.setVisibility(VISIBLE);
                right.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_share));
                right.setContentDescription(getString(R.string.sw_share_button));
                right.setOnClickListener(v -> doShare());
            }
        }
    }

    @Override
    public void startRunnable() {
        startUpdatingTime();
    }

    @Override
    public void stopRunnable() {
        stopUpdatingTime();
    }

    private void refreshSettings() {
        String newFontPath = SettingsDAO.getStopwatchFont(getPrefs());

        if (!Objects.equals(mStopwatchFontPath, newFontPath)) {
            mStopwatchFontPath = newFontPath;
            mStopwatchTypeface = null;
        }

        mAreMillisecondsDisplayed = SettingsDAO.areMillisecondsDisplayed(getPrefs());
        mVolumeUpAction = SettingsDAO.getVolumeUpActionForStopwatch(getPrefs());
        mVolumeUpActionAfterLongPress = SettingsDAO.getVolumeUpActionAfterLongPressForStopwatch(getPrefs());
        mVolumeDownAction = SettingsDAO.getVolumeDownActionForStopwatch(getPrefs());
        mVolumeDownActionAfterLongPress = SettingsDAO.getVolumeDownActionAfterLongPressForStopwatch(getPrefs());
    }

    private void applyStopwatchFont() {
        mBinding.stopwatchTimeLayout.stopwatchTimeText.setTypeface(getStopwatchTypeface());
        mBinding.stopwatchTimeLayout.stopwatchHundredthsText.setTypeface(getStopwatchTypeface());
    }

    /**
     * Lazy loading for the standard stopwatch font.
     *
     * @return the stopwatch font.
     */
    private Typeface getStopwatchTypeface() {
        if (mStopwatchTypeface == null) {
            mStopwatchTypeface = ThemeUtils.loadFont(mStopwatchFontPath);
        }
        return mStopwatchTypeface;
    }

    private void applyMillisecondsVisibility() {
        mBinding.stopwatchTimeLayout.stopwatchHundredthsText.setVisibility(mAreMillisecondsDisplayed ? VISIBLE : GONE);

        if (mStopwatchTextController != null) {
            mStopwatchTextController.setMillisecondsDisplayed(mAreMillisecondsDisplayed);
        }
    }

    private void applySettingsChanges() {
        refreshSettings();
        applyStopwatchFont();
        applyMillisecondsVisibility();
        updateTime();

        if (mLapsAdapter != null) {
            mLapsAdapter.updateFonts(getFontsConfig());
        }

        mAreSettingsChanged = false;
    }

    /**
     * Updates the floating action button to reflect the current stopwatch state.
     * Sets the appropriate icon, content description, and ensures the button is visible.
     *
     * @param fab the ImageView used as the stopwatch action button
     */
    private void updateFab(@NonNull ImageView fab) {
        if (getStopwatch().isRunning()) {
            fab.setImageResource(R.drawable.ic_fab_pause);
            fab.setContentDescription(getString(R.string.sw_pause_button));
        } else {
            fab.setImageResource(R.drawable.ic_fab_play);
            fab.setContentDescription(getString(R.string.sw_start_button));
        }

        fab.setOnLongClickListener(v -> {
            CustomTooltip.showAbove(v, getGeneralTypeface(), getDisplayMetrics(), fab.getContentDescription().toString(), true);
            return true;
        });

        fab.setVisibility(VISIBLE);
    }

    /**
     * Start the stopwatch.
     */
    private void doStart() {
        Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
        Events.sendStopwatchEvent(R.string.action_start, R.string.label_deskclock);
        getDataModel().startStopwatch();
    }

    /**
     * Pause the stopwatch.
     */
    private void doPause() {
        Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
        Events.sendStopwatchEvent(R.string.action_pause, R.string.label_deskclock);
        getDataModel().pauseStopwatch();
    }

    /**
     * Reset the stopwatch.
     */
    private void doReset() {
        Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.CLOCK_TICK);

        final Stopwatch.State priorState = getStopwatch().getState();
        Events.sendStopwatchEvent(R.string.action_reset, R.string.label_deskclock);
        getDataModel().resetStopwatch();

        mBinding.stopwatchTimeLayout.stopwatchTimeText.setAlpha(1f);

        if (mAreMillisecondsDisplayed) {
            mBinding.stopwatchTimeLayout.stopwatchHundredthsText.setAlpha(1f);
        }

        if (priorState == Stopwatch.State.RUNNING) {
            updateFab(FAB_MORPH);
        }
    }

    /**
     * Send stopwatch time and lap times to an external sharing application.
     */
    private void doShare() {
        Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.CLOCK_TICK);

        // Disable the fab buttons to avoid double-taps on the share button.
        updateFab(BUTTONS_DISABLE);

        final String[] subjects = requireContext().getResources().getStringArray(R.array.sw_share_strings);
        final String subject = subjects[(int) (Math.random() * subjects.length)];
        final String text = mLapsAdapter.getShareText();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, text)
            .setType("text/plain");

        final String title = getString(R.string.sw_share_button);
        final Intent shareChooserIntent = Intent.createChooser(shareIntent, title);
        try {
            requireContext().startActivity(shareChooserIntent);
        } catch (ActivityNotFoundException activityNotFoundException) {
            LogUtils.e("Cannot share lap data because no suitable receiving Activity exists");
            updateFab(BUTTONS_IMMEDIATE);
        }
    }

    /**
     * Record and add a new lap ending now.
     */
    private void doAddLap() {
        Events.sendStopwatchEvent(R.string.action_lap, R.string.label_deskclock);

        // Record a new lap.
        final Lap lap = mLapsAdapter.addLap();
        if (lap == null) {
            return;
        }

        Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.CLOCK_TICK);

        // Update button states.
        updateFab(BUTTONS_IMMEDIATE);

        if (lap.getLapNumber() == 1) {
            // Child views from prior lap sets hang around and blit to the screen when adding the
            // first lap of the subsequent lap set. Remove those superfluous children here manually
            // to ensure they aren't seen as the first lap is drawn.
            mBinding.lapsList.removeAllViewsInLayout();

            // Start animating the reference lap.
            mBinding.stopwatchCircle.update();

            // Recording the first lap transitions the UI to display the laps list.
            showOrHideLaps(false);
        }

        // Ensure the newly added lap is visible on screen.
        mBinding.lapsList.scrollToPosition(0);
    }

    /**
     * Show or hide the list of laps.
     */
    private void showOrHideLaps(boolean clearLaps) {
        if (mBinding == null) {
            return;
        }

        if (clearLaps) {
            mLapsAdapter.clearLaps();
        }

        final boolean lapsVisible = mLapsAdapter.getItemCount() > 0;
        final int targetVisibility = lapsVisible ? VISIBLE : GONE;

        if (mBinding.lapsBackground.getVisibility() != targetVisibility) {
            TransitionManager.beginDelayedTransition(mBinding.getRoot());
            mBinding.lapsBackground.setVisibility(targetVisibility);
        }
    }

    private void adjustWakeLock() {
        if (isAdded() && getActivity() instanceof DeskClock deskClock) {
            deskClock.updateKeepScreenOn();
        }
    }

    /**
     * Either pause or start the stopwatch based on its current state.
     */
    private void toggleStopwatchState() {
        if (getStopwatch().isRunning()) {
            doPause();
        } else {
            doStart();
        }
    }

    private Stopwatch getStopwatch() {
        return getDataModel().getStopwatch();
    }

    private boolean canRecordMoreLaps() {
        return getDataModel().canAddMoreLaps();
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    public void startUpdatingTime() {
        if (mBinding == null || !isTabSelected() || getStopwatch().isReset()) {
            return;
        }

        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mBinding.stopwatchTimeLayout.stopwatchTimeText.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    public void stopUpdatingTime() {
        if (mBinding != null) {
            mBinding.stopwatchTimeLayout.stopwatchTimeText.removeCallbacks(mTimeUpdateRunnable);
        }
    }

    /**
     * Update all time displays based on a single snapshot of the stopwatch progress. This includes
     * the stopwatch time drawn in the circle, the current lap time and the total elapsed time in
     * the list of laps.
     */
    private void updateTime() {
        if (mBinding == null || mStopwatchTextController == null) {
            return;
        }

        // Compute the total time of the stopwatch.
        final Stopwatch stopwatch = getStopwatch();
        final long totalTime = stopwatch.getTotalTime();
        mStopwatchTextController.setTimeString(totalTime);

        // Explicitly reset alpha to 1f to ensure the text is visible when the stopwatch resumes.
        if (mBinding.stopwatchTimeLayout.stopwatchTimeText.getAlpha() != 1f) {
            mBinding.stopwatchTimeLayout.stopwatchTimeText.setAlpha(1f);
        }

        if (mAreMillisecondsDisplayed && mBinding.stopwatchTimeLayout.stopwatchHundredthsText.getAlpha() != 1f) {
            mBinding.stopwatchTimeLayout.stopwatchHundredthsText.setAlpha(1f);
        }

        // Update the current lap.
        final boolean currentLapIsVisible = mLapsLayoutManager.findFirstVisibleItemPosition() == 0;
        if (!stopwatch.isReset() && currentLapIsVisible) {
            mLapsAdapter.updateCurrentLap(mBinding.lapsList, totalTime);
        }
    }

    /**
     * Synchronize the UI state with the model data.
     */
    private void updateUI(@UpdateFabFlag int updateTypes) {
        // Draw the latest stopwatch and current lap times.
        updateTime();

        mBinding.stopwatchCircle.update();

        startUpdatingTime();

        // Adjust the visibility of the list of laps.
        final Stopwatch stopwatch = getStopwatch();
        showOrHideLaps(stopwatch.isReset());

        // Update button states.
        updateFab(updateTypes);
    }

    /**
     * Return the action to execute to the volume up button
     */
    private void getVolumeUpAction() {
        getVolumeButtonsActions(mVolumeUpAction);
    }

    /**
     * Return the action to execute to the volume up button after a long press
     */
    private void getVolumeUpActionAfterLongPress() {
        getVolumeButtonsActions(mVolumeUpActionAfterLongPress);
    }

    /**
     * Return the action to execute to the volume down button
     */
    private void getVolumeDownAction() {
        getVolumeButtonsActions(mVolumeDownAction);
    }

    /**
     * Return the action to execute to the volume down button after a long press
     */
    private void getVolumeDownActionAfterLongPress() {
        getVolumeButtonsActions(mVolumeDownActionAfterLongPress);
    }

    /**
     * Set actions for volume buttons
     */
    private void getVolumeButtonsActions(@NonNull String volumeAction) {
        switch (volumeAction) {
            case SW_ACTION_START_PAUSE -> {
                if (getStopwatch().isReset() || getStopwatch().isPaused()) {
                    doStart();
                } else {
                    doPause();
                }
            }
            case SW_ACTION_RESET -> {
                if (getStopwatch().isRunning() || getStopwatch().isPaused()) {
                    doReset();
                }
            }
            case SW_ACTION_LAP -> {
                if (getStopwatch().isRunning()) {
                    doAddLap();
                }
            }
            case SW_ACTION_SHARE -> {
                if (getStopwatch().isRunning()) {
                    doPause();
                    doShare();
                } else if (getStopwatch().isPaused()) {
                    doShare();
                }
            }
        }
    }

    /**
     * This runnable periodically updates times throughout the UI. It stops these updates when the
     * stopwatch is no longer running.
     */
    private final class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            if (mBinding == null) {
                return;
            }

            final long startTime = Utils.now();

            updateTime();

            // Blink text iff the stopwatch is paused and not pressed.
            final View touchTarget = mBinding.stopwatchCircle;
            final Stopwatch stopwatch = getStopwatch();
            final boolean blink = stopwatch.isPaused()
                && startTime % 1000 < 500
                && !touchTarget.isPressed();
            final float textTargetAlpha = blink ? 0f : 1f;

            if (mBinding.stopwatchTimeLayout.stopwatchTimeText.getAlpha() != textTargetAlpha) {
                mBinding.stopwatchTimeLayout.stopwatchTimeText.animate()
                    .alpha(textTargetAlpha)
                    .setDuration(AnimatorUtils.SHORT_ANIMATION_DURATION)
                    .start();

                if (mAreMillisecondsDisplayed) {
                    mBinding.stopwatchTimeLayout.stopwatchHundredthsText.animate()
                        .alpha(textTargetAlpha)
                        .setDuration(AnimatorUtils.SHORT_ANIMATION_DURATION)
                        .start();
                }
            }

            if (!stopwatch.isReset()) {
                final long period;

                if (stopwatch.isPaused()) {
                    period = REDRAW_PERIOD_PAUSED;
                } else {
                    period = mAreMillisecondsDisplayed ? REDRAW_PERIOD_RUNNING : REDRAW_PERIOD_RUNNING_WITHOUT_MS;
                }

                final long endTime = Utils.now();
                final long delay = Math.max(0, startTime + period - endTime);
                mBinding.stopwatchTimeLayout.stopwatchTimeText.postDelayed(this, delay);
            }
        }
    }

    /**
     * Update the user interface in response to a stopwatch change.
     */
    private class StopwatchWatcher implements StopwatchListener {
        @Override
        public void stopwatchUpdated(@NonNull Stopwatch after) {
            adjustWakeLock();

            if (after.isReset()) {
                if (getDataModel().isApplicationInForeground()) {
                    updateUI(BUTTONS_IMMEDIATE);
                }
                return;
            }
            if (getDataModel().isApplicationInForeground()) {
                updateUI(FAB_MORPH | BUTTONS_IMMEDIATE);
            }
        }
    }

    /**
     * Toggles stopwatch state when user taps stopwatch.
     */
    private final class TimeClickListener implements View.OnClickListener {
        @Override
        public void onClick(@NonNull View view) {
            Utils.performHapticFeedback(view, isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            if (getStopwatch().isRunning()) {
                getDataModel().pauseStopwatch();
            } else {
                getDataModel().startStopwatch();
            }
        }
    }

}

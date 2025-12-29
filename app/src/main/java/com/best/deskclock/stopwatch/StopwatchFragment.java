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

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SW_ACTION;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_LAP;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_RESET;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_SHARE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SW_ACTION_START_PAUSE;
import static com.best.deskclock.uidata.UiDataModel.Tab.STOPWATCH;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.best.deskclock.DeskClock;
import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.R;
import com.best.deskclock.RunnableFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Lap;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.data.StopwatchListener;
import com.best.deskclock.events.Events;
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

    /**
     * Milliseconds between redraws while running.
     */
    private static final int REDRAW_PERIOD_RUNNING = 25;

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
     * The data source for {@link #mLapsList}.
     */
    private LapsAdapter mLapsAdapter;

    /**
     * The layout manager for the {@link #mLapsAdapter}.
     */
    private LinearLayoutManager mLapsLayoutManager;

    /**
     * The view containing the stopwatch time in landscape mode.
     */
    private View mStopwatchLandscapeLayout;

    /**
     * Draws the reference lap while the stopwatch is running.
     */
    private StopwatchCircleView mStopwatchCircleView;

    /**
     * The View containing both TextViews of the stopwatch.
     */
    private View mStopwatchWrapper;

    /**
     * The view containing the lap list.
     */
    private View mLapsBackground;

    /**
     * Displays the recorded lap times.
     */
    private RecyclerView mLapsList;

    /**
     * Displays the current stopwatch time (seconds and above only).
     */
    private TextView mMainTimeText;

    /**
     * Displays the current stopwatch time (hundredths only).
     */
    private TextView mHundredthsTimeText;

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

    private Activity mActivity;
    private Context mContext;
    private SharedPreferences mPrefs;
    private DisplayMetrics mDisplayMetrics;
    private String mVolumeUpAction;
    private String mVolumeUpActionAfterLongPress;
    private String mVolumeDownAction;
    private String mVolumeDownActionAfterLongPress;
    private boolean mIsVolumeUpLongPressed;
    private boolean mIsVolumeDownLongPressed;
    private boolean mIsPortrait;
    private boolean mIsTablet;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        mContext = requireContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        Typeface stopwatchTypeface = ThemeUtils.loadFont(SettingsDAO.getStopwatchFont(mPrefs));
        Typeface boldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs));
        mDisplayMetrics = getResources().getDisplayMetrics();
        mIsPortrait = ThemeUtils.isPortrait();
        mIsTablet = ThemeUtils.isTablet();
        mLapsAdapter = new LapsAdapter(mContext);
        mLapsLayoutManager = new LinearLayoutManager(mContext);

        final View v = inflater.inflate(R.layout.stopwatch_fragment, container, false);

        mStopwatchLandscapeLayout = v.findViewById(R.id.stopwatch_landscape_layout);
        mStopwatchWrapper = v.findViewById(R.id.stopwatch_time_wrapper);
        mMainTimeText = v.findViewById(R.id.stopwatch_time_text);
        mHundredthsTimeText = v.findViewById(R.id.stopwatch_hundredths_text);
        mStopwatchCircleView = v.findViewById(R.id.stopwatch_circle);
        mLapsBackground = v.findViewById(R.id.laps_background);
        mLapsList = v.findViewById(R.id.laps_list);

        if (hasEnoughSpaceForCircle()) {
            mStopwatchCircleView.setVisibility(VISIBLE);
            mStopwatchWrapper.setOnTouchListener(new Utils.CircleTouchListener());
        }

        mStopwatchWrapper.setOnClickListener(new TimeClickListener());

        mMainTimeText.setTypeface(stopwatchTypeface);
        mHundredthsTimeText.setTypeface(stopwatchTypeface);
        final int colorAccent = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mMainTimeText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent});
        mMainTimeText.setTextColor(timeTextColor);
        mHundredthsTimeText.setTextColor(timeTextColor);

        mLapsBackground.setBackground(ThemeUtils.cardBackground(mContext));

        View lapHeader = v.findViewById(R.id.lap_header);
        if (lapHeader != null) {
            // Add space between the top of the header and the top of the background
            // equal to the lap padding
            final int padding = (int) dpToPx(mIsTablet ? 8 : 4, mDisplayMetrics);
            lapHeader.setPadding(0, padding, 0, padding);

            // Handle header text size and font
            final float textSize = mIsTablet ? 18 : 16;
            TextView[] titles = {
                    v.findViewById(R.id.lap_title),
                    v.findViewById(R.id.split_title),
                    v.findViewById(R.id.total_title)
            };

            for (TextView tv : titles) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                tv.setTypeface(boldTypeface);
            }
        }

        // Timer text serves as a virtual start/stop button.
        mStopwatchTextController = new StopwatchTextController(mMainTimeText, mHundredthsTimeText);

        ((SimpleItemAnimator) Objects.requireNonNull(mLapsList.getItemAnimator())).setSupportsChangeAnimations(false);
        mLapsList.setLayoutManager(mLapsLayoutManager);
        mLapsList.setAdapter(mLapsAdapter);

        DataModel.getDataModel().addStopwatchListener(mStopwatchWatcher);

        mVolumeUpAction = SettingsDAO.getVolumeUpActionForStopwatch(mPrefs);
        mVolumeUpActionAfterLongPress = SettingsDAO.getVolumeUpActionAfterLongPressForStopwatch(mPrefs);
        mVolumeDownAction = SettingsDAO.getVolumeDownActionForStopwatch(mPrefs);
        mVolumeDownActionAfterLongPress = SettingsDAO.getVolumeDownActionAfterLongPressForStopwatch(mPrefs);

        return v;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (mVolumeUpAction.equals(DEFAULT_SW_ACTION)
                            && mVolumeUpActionAfterLongPress.equals(DEFAULT_SW_ACTION)) {
                        return false;
                    }
                    mIsVolumeUpLongPressed = event.getRepeatCount() >= 2;
                    return true;

                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (mVolumeDownAction.equals(DEFAULT_SW_ACTION)
                            && mVolumeDownActionAfterLongPress.equals(DEFAULT_SW_ACTION)) {
                        return false;
                    }
                    mIsVolumeDownLongPressed = event.getRepeatCount() >= 2;
                    return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
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
    public void onResume() {
        super.onResume();

        mActivity = requireActivity();
        final Intent intent = mActivity.getIntent();
        if (intent != null) {
            final String action = intent.getAction();
            if (StopwatchService.ACTION_START_STOPWATCH.equals(action)) {
                DataModel.getDataModel().startStopwatch();
                // Consume the intent
                mActivity.setIntent(null);
            } else if (StopwatchService.ACTION_PAUSE_STOPWATCH.equals(action)) {
                DataModel.getDataModel().pauseStopwatch();
                // Consume the intent
                mActivity.setIntent(null);
            }
        }

        // Conservatively assume the data in the adapter has changed while the fragment was paused.
        mLapsAdapter.notifyDataSetChanged();

        // Synchronize the user interface with the data model.
        updateUI(FAB_AND_BUTTONS_IMMEDIATE);

        // In portrait mode, re‑apply dynamic layout constraints after the UI has been refreshed.
        // This ensures that the stopwatch circle and the laps list are resized correctly based on
        // the screen height and the FAB container.
        if (mIsPortrait) {
            applyPortraitConstraints(requireView());
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
        super.onDestroyView();

        DataModel.getDataModel().removeStopwatchListener(mStopwatchWatcher);
    }

    @Override
    public void onFabClick() {
        toggleStopwatchState();
    }

    @Override
    public void onFabLongClick(@NonNull ImageView fab) {
        fab.setHapticFeedbackEnabled(false);
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        updateFab(fab);
    }

    @Override
    public void onMorphFab(@NonNull ImageView fab) {
        // Update the fab's drawable to match the current timer state.
        updateFab(fab);
        // Animate the drawable.
        AnimatorUtils.startDrawableAnimation(fab);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        if (mContext != null) {
            left.setClickable(true);
            left.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_reset));
            left.setContentDescription(mContext.getString(R.string.reset));
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
                    right.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_tab_stopwatch_static));
                    right.setContentDescription(mContext.getString(R.string.sw_lap_button));
                    right.setClickable(canRecordLaps);
                    right.setVisibility(canRecordLaps ? VISIBLE : INVISIBLE);
                    right.setOnClickListener(v -> doAddLap());
                }
                case PAUSED -> {
                    left.setVisibility(VISIBLE);
                    right.setClickable(true);
                    right.setVisibility(VISIBLE);
                    right.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_share));
                    right.setContentDescription(mContext.getString(R.string.sw_share_button));
                    right.setOnClickListener(v -> doShare());
                }
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

    /**
     * Applies dynamic layout constraints when the device is in portrait mode.
     *
     * <p>This method limits the maximum height of the stopwatch circle and the
     * laps background based on the screen height. It also adjusts the bottom
     * margin of the laps background so that it remains above the FAB container.
     *
     * <p>The FAB container height is retrieved asynchronously using {@code post()}
     * to ensure that measurements are available before applying the constraints.</p>
     *
     * @param view the root view of the stopwatch fragment
     */
    private void applyPortraitConstraints(View view) {
        ConstraintLayout layout = view.findViewById(R.id.stopwatch_root);
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);

        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int maxCircleHeight = (int) (screenHeight * 0.40f);
        int maxLapsHeight = (int) (screenHeight * 0.35f);

        set.constrainMaxHeight(R.id.stopwatch_time_wrapper, maxCircleHeight);
        set.constrainMaxHeight(R.id.laps_background, maxLapsHeight);

        View fabContainer = ((DeskClock) requireActivity()).getFabContainer();
        fabContainer.post(() -> {
            int fabHeight = fabContainer.getHeight();
            int margin = fabHeight + (int) dpToPx(15, mDisplayMetrics);

            set.setMargin(R.id.laps_background, ConstraintSet.BOTTOM, margin);
            set.applyTo(layout);
        });
    }

    /**
     * Determines whether the device screen is wide enough to display the stopwatch circle
     * in landscape mode.
     *
     * @return {@code true} if the screen width meets the minimum required dp value.
     * {@code false} otherwise.
     */
    private boolean hasEnoughSpaceForCircle() {
        int minWidthDp = 320;
        float widthDp = mDisplayMetrics.widthPixels / mDisplayMetrics.density;

        return widthDp >= minWidthDp;
    }

    /**
     * @return {@code true} if the stopwatch circle view is currently visible.
     * {@code false} otherwise.
     */
    private boolean isCircleVisible() {
        return mStopwatchCircleView.getVisibility() == VISIBLE;
    }

    /**
     * Updates the floating action button to reflect the current stopwatch state.
     * Sets the appropriate icon, content description, and ensures the button is visible.
     *
     * @param fab the ImageView used as the stopwatch action button
     */
    private void updateFab(@NonNull ImageView fab) {
        if (mContext != null) {
            if (getStopwatch().isRunning()) {
                fab.setImageResource(R.drawable.ic_fab_pause);
                fab.setContentDescription(mContext.getString(R.string.sw_pause_button));
            } else {
                fab.setImageResource(R.drawable.ic_fab_play);
                fab.setContentDescription(mContext.getString(R.string.sw_start_button));
            }
            fab.setVisibility(VISIBLE);
        }
    }

    /**
     * Start the stopwatch.
     */
    private void doStart() {
        Events.sendStopwatchEvent(R.string.action_start, R.string.label_deskclock);
        DataModel.getDataModel().startStopwatch();
        Utils.setVibrationTime(mContext, 50);
    }

    /**
     * Pause the stopwatch.
     */
    private void doPause() {
        Events.sendStopwatchEvent(R.string.action_pause, R.string.label_deskclock);
        DataModel.getDataModel().pauseStopwatch();
        Utils.setVibrationTime(mContext, 50);
    }

    /**
     * Reset the stopwatch.
     */
    private void doReset() {
        final Stopwatch.State priorState = getStopwatch().getState();
        Events.sendStopwatchEvent(R.string.action_reset, R.string.label_deskclock);
        DataModel.getDataModel().resetStopwatch();
        mMainTimeText.setAlpha(1f);
        mHundredthsTimeText.setAlpha(1f);
        if (priorState == Stopwatch.State.RUNNING) {
            updateFab(FAB_MORPH);
        }
        Utils.setVibrationTime(mContext, 10);
    }

    /**
     * Send stopwatch time and lap times to an external sharing application.
     */
    private void doShare() {
        // Disable the fab buttons to avoid double-taps on the share button.
        updateFab(BUTTONS_DISABLE);

        final String[] subjects = mContext.getResources().getStringArray(R.array.sw_share_strings);
        final String subject = subjects[(int) (Math.random() * subjects.length)];
        final String text = mLapsAdapter.getShareText();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
                .setType("text/plain");

        final String title = mContext.getString(R.string.sw_share_button);
        final Intent shareChooserIntent = Intent.createChooser(shareIntent, title);
        try {
            mContext.startActivity(shareChooserIntent);
        } catch (ActivityNotFoundException anfe) {
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

        // Update button states.
        updateFab(BUTTONS_IMMEDIATE);

        if (lap.getLapNumber() == 1) {
            // Child views from prior lap sets hang around and blit to the screen when adding the
            // first lap of the subsequent lap set. Remove those superfluous children here manually
            // to ensure they aren't seen as the first lap is drawn.
            mLapsList.removeAllViewsInLayout();

            if (isCircleVisible()) {
                // Start animating the reference lap.
                mStopwatchCircleView.update();
            }

            // Recording the first lap transitions the UI to display the laps list.
            showOrHideLaps(false);
        }

        // Ensure the newly added lap is visible on screen.
        mLapsList.scrollToPosition(0);
    }

    /**
     * Show or hide the list of laps.
     */
    private void showOrHideLaps(boolean clearLaps) {
        final ViewGroup sceneRoot = (ViewGroup) getView();
        if (sceneRoot == null) {
            return;
        }

        TransitionManager.beginDelayedTransition(sceneRoot);

        if (clearLaps) {
            mLapsAdapter.clearLaps();
        }

        final boolean lapsVisible = mLapsAdapter.getItemCount() > 0;
        mLapsBackground.setVisibility(lapsVisible ? VISIBLE : GONE);

        if (mIsPortrait) {
            // When the lap list is visible, it includes the bottom padding. When it is absent the
            // appropriate bottom padding must be applied to the container.
            final int bottom = (int) dpToPx(lapsVisible ? 0 : 80, mDisplayMetrics);
            final int top = sceneRoot.getPaddingTop();
            final int left = sceneRoot.getPaddingLeft();
            final int right = sceneRoot.getPaddingRight();
            sceneRoot.setPadding(left, top, right, bottom);
            return;
        }

        // Handle margins dynamically for the landscape mode
        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) mStopwatchLandscapeLayout.getLayoutParams();

        final int marginStart = (int) dpToPx(mIsTablet ? 10 : 0, mDisplayMetrics);
        final int marginEnd = (int) dpToPx(mIsTablet ? 10 : 90, mDisplayMetrics);
        final int marginBottom = (int) dpToPx(mIsTablet ? 110 : 10, mDisplayMetrics);
        if (lapsVisible) {
            layoutParams.setMargins(marginStart, 0, marginEnd, marginBottom);
        } else {
            layoutParams.setMargins(0, 0, 0, marginBottom);
        }

        mStopwatchLandscapeLayout.setLayoutParams(layoutParams);

    }

    private void adjustWakeLock() {
        if (getStopwatch().isRunning() || SettingsDAO.shouldScreenRemainOn(mPrefs)) {
            ThemeUtils.keepScreenOn(mActivity);
        } else {
            ThemeUtils.releaseKeepScreenOn(mActivity);
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
        return DataModel.getDataModel().getStopwatch();
    }

    private boolean canRecordMoreLaps() {
        return DataModel.getDataModel().canAddMoreLaps();
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    public void startUpdatingTime() {
        if (!isTabSelected() || getStopwatch().isReset()) {
            return;
        }

        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mMainTimeText.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    public void stopUpdatingTime() {
        mMainTimeText.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * Update all time displays based on a single snapshot of the stopwatch progress. This includes
     * the stopwatch time drawn in the circle, the current lap time and the total elapsed time in
     * the list of laps.
     */
    private void updateTime() {
        // Compute the total time of the stopwatch.
        final Stopwatch stopwatch = getStopwatch();
        final long totalTime = stopwatch.getTotalTime();
        mStopwatchTextController.setTimeString(totalTime);

        // Explicitly reset alpha to 1f to ensure the text is visible when the stopwatch resumes.
        if (mMainTimeText.getAlpha() != 1f) {
            mMainTimeText.setAlpha(1f);
        }
        if (mHundredthsTimeText.getAlpha() != 1f) {
            mHundredthsTimeText.setAlpha(1f);
        }

        // Update the current lap.
        final boolean currentLapIsVisible = mLapsLayoutManager.findFirstVisibleItemPosition() == 0;
        if (!stopwatch.isReset() && currentLapIsVisible) {
            mLapsAdapter.updateCurrentLap(mLapsList, totalTime);
        }
    }

    /**
     * Synchronize the UI state with the model data.
     */
    private void updateUI(@UpdateFabFlag int updateTypes) {
        // Draw the latest stopwatch and current lap times.
        updateTime();

        if (isCircleVisible()) {
            mStopwatchCircleView.update();
        }

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
    private void getVolumeButtonsActions(String volumeAction) {
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
            final long startTime = Utils.now();

            updateTime();

            // Blink text iff the stopwatch is paused and not pressed.
            final View touchTarget = isCircleVisible() ? mStopwatchCircleView : mStopwatchWrapper;
            final Stopwatch stopwatch = getStopwatch();
            final boolean blink = stopwatch.isPaused()
                    && startTime % 1000 < 500
                    && !touchTarget.isPressed();
            final float textTargetAlpha = blink ? 0f : 1f;

            mMainTimeText.animate()
                    .alpha(textTargetAlpha)
                    .setDuration(200)
                    .start();

            mHundredthsTimeText.animate()
                    .alpha(textTargetAlpha)
                    .setDuration(200)
                    .start();

            if (!stopwatch.isReset()) {
                final long period = stopwatch.isPaused()
                        ? REDRAW_PERIOD_PAUSED
                        : REDRAW_PERIOD_RUNNING;
                final long endTime = Utils.now();
                final long delay = Math.max(0, startTime + period - endTime);
                mMainTimeText.postDelayed(this, delay);
            }
        }
    }

    /**
     * Update the user interface in response to a stopwatch change.
     */
    private class StopwatchWatcher implements StopwatchListener {
        @Override
        public void stopwatchUpdated(Stopwatch after) {
            adjustWakeLock();

            if (after.isReset()) {
                if (DataModel.getDataModel().isApplicationInForeground()) {
                    updateUI(BUTTONS_IMMEDIATE);
                }
                return;
            }
            if (DataModel.getDataModel().isApplicationInForeground()) {
                updateUI(FAB_MORPH | BUTTONS_IMMEDIATE);
            }
        }
    }

    /**
     * Toggles stopwatch state when user taps stopwatch.
     */
    private final class TimeClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (getStopwatch().isRunning()) {
                DataModel.getDataModel().pauseStopwatch();
            } else {
                DataModel.getDataModel().startStopwatch();
            }
            Utils.setVibrationTime(mContext, 50);
        }
    }

}

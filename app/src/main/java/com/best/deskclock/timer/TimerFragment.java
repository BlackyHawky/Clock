/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.view.View.ALPHA;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.TRANSLATION_Y;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SORT_TIMER_MANUALLY;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMER_CREATION_VIEW_SPINNER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.uidata.UiDataModel.Tab.TIMERS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.transition.TransitionManager;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.base.DeskClockFragment;
import com.best.deskclock.base.RunnableFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
import com.best.deskclock.databinding.TimerFragmentBinding;
import com.best.deskclock.events.Events;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.CustomTooltip;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import java.io.Serializable;
import java.util.List;

/**
 * Displays a vertical list of timers in all states.
 */
public final class TimerFragment extends DeskClockFragment implements RunnableFragment {

    private static final String EXTRA_TIMER_SETUP = "com.best.deskclock.action.TIMER_SETUP";

    private static final String KEY_TIMER_SETUP_STATE = "timer_setup_input";
    private static final String KEY_TIMER_ID_TO_DELETE = "timer_id_to_delete";
    private int mTimerIdToDelete = -1;
    private boolean mAreSettingsChanged = false;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = (prefs, key) -> {
        if (key != null) {
            switch (key) {
                case KEY_DISPLAY_LOW_ALARM_VOLUME_WARNING, KEY_TIMER_DURATION_FONT, KEY_DISPLAY_COMPACT_TIMERS, KEY_DISPLAY_TIMER_END_TIME,
                     KEY_INVERT_TIMER_BUTTON_POSITIONS, KEY_SINGLE_TIMER_MODE, KEY_SORT_TIMER, KEY_DISPLAY_TIMER_STATE_INDICATOR,
                     KEY_RUNNING_TIMER_INDICATOR_COLOR, KEY_PAUSED_TIMER_INDICATOR_COLOR, KEY_EXPIRED_TIMER_INDICATOR_COLOR,
                     KEY_MISSED_TIMER_INDICATOR_COLOR -> {

                    mAreSettingsChanged = true;

                    if (isResumed()) {
                        applySettingsChanges();
                    }
                }
            }
        }
    };

    private TimerFragmentBinding mBinding;

    private SharedPreferences mPrefs;
    private final TimerSettings mSettings = new TimerSettings();
    private boolean mIsManualSorting;
    private DisplayMetrics mDisplayMetrics;
    private Serializable mTimerSetupState;
    private TimerAdapter mAdapter;
    private ViewGroup mCurrentView;
    private TimerItemTouchHelper mTouchHelperCallback;
    private ItemTouchHelper mItemTouchHelper;
    private AlertDialog mActiveDialog = null;
    private boolean mIsTablet;
    private boolean mIsLandscape;

    /**
     * Updates the FABs in response to timers being added or removed.
     */
    private final TimerListener mTimerWatcher = new TimerWatcher();

    /**
     * {@code true} while this fragment is creating a new timer; {@code false} otherwise.
     */
    private boolean mCreatingTimer;

    /**
     * {@code true} when the value is greater than zero; {@code false} otherwise.
     * Useful for updating the FAB only once, rather than every time the spinner value changes.
     */
    private boolean isTimerValueValid = false;

    /**
     * @return an Intent that selects the timers tab with the setup screen for a new timer in place.
     */
    public static Intent createTimerSetupIntent(Context context) {
        return new Intent(context, DeskClock.class).putExtra(EXTRA_TIMER_SETUP, true);
    }

    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RingtoneUtils.VOLUME_CHANGED_ACTION.equals(intent.getAction())) {
                updateWarningBannerVisibility();
            }
        }
    };

    /**
     * The public no-arg constructor required by all fragments.
     */
    public TimerFragment() {
        super(TIMERS);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mPrefs = getDefaultSharedPreferences(requireContext());
        mDisplayMetrics = getResources().getDisplayMetrics();
        mIsTablet = ThemeUtils.isTablet();
        mIsLandscape = ThemeUtils.isLandscape();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = TimerFragmentBinding.inflate(inflater, container, false);

        mBinding.timerVolumeBanner.volumeWarningButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            RingtoneUtils.fixAlarmStreamLow(requireContext());
        });

        mBinding.timerRecyclerView.setLayoutManager(getLayoutManager(requireContext()));
        mBinding.timerRecyclerView.addItemDecoration(new GridSpacingItemDecoration(requireContext(), mDisplayMetrics));

        RecyclerView.ItemAnimator animator = mBinding.timerRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            // Disable flash/blinking during updates (notifyItemChanged)
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mBinding.timerSetupView.setFabContainer(this);
        mBinding.timerSpinnerSetupView.setOnChangeListener(() -> {
            if (hasValidInput() != isTimerValueValid) {
                isTimerValueValid = hasValidInput();
                updateFab(FAB_SHRINK_AND_EXPAND);
            }
        });

        if (savedInstanceState != null) {
            // If timer setup state is present, retrieve it to be later honored.
            mTimerSetupState = SdkUtils.isAtLeastAndroid13()
                ? savedInstanceState.getSerializable(KEY_TIMER_SETUP_STATE, int[].class)
                : savedInstanceState.getSerializable(KEY_TIMER_SETUP_STATE);

            mTimerIdToDelete = savedInstanceState.getInt(KEY_TIMER_ID_TO_DELETE, -1);
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(
            getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (isTabSelected() && mCurrentView != mBinding.timerContentView && hasTimers()) {
                        animateToView(mBinding.timerContentView, false);
                    } else {
                        setEnabled(false);
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        setEnabled(true);
                    }
                }
            });

        boolean createTimer = false;
        int showTimerId = -1;
        final Intent intent = requireActivity().getIntent();
        if (intent != null) {
            createTimer = intent.getBooleanExtra(EXTRA_TIMER_SETUP, false);
            showTimerId = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, -1);
        }

        if (showTimerId != -1) {
            mCurrentView = mBinding.timerContentView;
        } else if (!hasTimers() || createTimer || mTimerSetupState != null) {
            mCurrentView = getTimerCreationView();

            if (mTimerSetupState != null) {
                mBinding.timerSetupView.setState(mTimerSetupState);
            }
        } else {
            mCurrentView = mBinding.timerContentView;
        }

        if (mCurrentView == mBinding.timerContentView) {
            mBinding.timerContentView.setVisibility(View.VISIBLE);
            mBinding.timerSetupView.setVisibility(View.GONE);
            mBinding.timerSpinnerSetupView.setVisibility(View.GONE);
        } else {
            mBinding.timerContentView.setVisibility(View.GONE);
            mCurrentView.setVisibility(View.VISIBLE);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String generalFontPath = SettingsDAO.getGeneralFont(mPrefs);
        Typeface regularTypeface = ThemeUtils.loadFont(generalFontPath);
        Typeface boldTypeface = ThemeUtils.boldTypeface(generalFontPath);

        refreshSettings();

        mBinding.timerSetupView.updateTimerSetupTimeFont(mSettings.timerTimeTypeface);

        mBinding.timerVolumeBanner.volumeWarningText.setTypeface(boldTypeface);
        mBinding.timerVolumeBanner.volumeWarningButton.setTypeface(boldTypeface);

        mAdapter = new TimerAdapter(requireContext(), mPrefs, new TimerClickHandler(this), mIsTablet, mIsLandscape,
            regularTypeface, boldTypeface, mSettings);

        mBinding.timerRecyclerView.setAdapter(mAdapter);
        mAdapter.loadTimersAsync();
        DataModel.getDataModel().addTimerListener(mAdapter);

        DataModel.getDataModel().addTimerListener(mTimerWatcher);

        mTouchHelperCallback = new TimerItemTouchHelper(mAdapter, mBinding.timerRecyclerView, mIsTablet, mIsLandscape, mIsManualSorting);
        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        handleItemTouchHelper();

        mPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(RingtoneUtils.VOLUME_CHANGED_ACTION);
        if (SdkUtils.isAtLeastAndroid13()) {
            requireContext().registerReceiver(mVolumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(mVolumeReceiver, filter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isSystem24Hour = DataModel.getDataModel().is24HourFormat();

        if (mAreSettingsChanged || mSettings.is24HourFormat != isSystem24Hour) {
            applySettingsChanges();
        }

        // Examine the intent of the parent activity to determine which view to display.
        final Intent intent = requireActivity().getIntent();
        if (intent != null) {
            // These extras are single-use; remove them after honoring them.
            boolean createTimer = intent.getBooleanExtra(EXTRA_TIMER_SETUP, false);
            int showTimerId = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, -1);

            intent.removeExtra(EXTRA_TIMER_SETUP);
            intent.removeExtra(TimerService.EXTRA_TIMER_ID);

            if (showTimerId != -1) {
                mCurrentView = mBinding.timerContentView;
            } else if (createTimer) {
                mCurrentView = getTimerCreationView();
            }
        }

        if (getView() != null) {
            getView().post(() -> {
                if (!isAdded()) {
                    return;
                }

                // Choose the view to display in this fragment.
                if (mCurrentView == mBinding.timerContentView) {
                    showTimersView(FAB_AND_BUTTONS_IMMEDIATE);
                } else {
                    showCreateTimerView(FAB_AND_BUTTONS_IMMEDIATE);

                    if (mTimerSetupState != null) {
                        mBinding.timerSetupView.setState(mTimerSetupState);
                        mTimerSetupState = null;
                    }
                }

                if (mTimerIdToDelete != -1 && (mActiveDialog == null || !mActiveDialog.isShowing())) {
                    Timer timerToDelete = null;
                    for (Timer timer : DataModel.getDataModel().getTimers()) {
                        if (timer.getId() == mTimerIdToDelete) {
                            timerToDelete = timer;
                            break;
                        }
                    }

                    if (timerToDelete != null) {
                        mActiveDialog = warningDialogBeforeDeletingTimer(timerToDelete);
                        mActiveDialog.show();
                    } else {
                        mTimerIdToDelete = -1;
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopUpdatingTime();
    }

    @Override
    public void onStop() {
        super.onStop();

        requireContext().unregisterReceiver(mVolumeReceiver);
    }

    @Override
    public void onDestroyView() {
        if (mActiveDialog != null && mActiveDialog.isShowing()) {
            mActiveDialog.setOnDismissListener(null);
            mActiveDialog.dismiss();
            mActiveDialog = null;
        }

        DataModel.getDataModel().removeTimerListener(mAdapter);
        DataModel.getDataModel().removeTimerListener(mTimerWatcher);

        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);

        mBinding.timerSpinnerSetupView.setOnChangeListener(null);

        mBinding.timerRecyclerView.setAdapter(null);

        mAreSettingsChanged = false;

        mBinding = null;

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the timer creation view is visible, store the input for later restoration.
        if (mBinding != null && mCurrentView != mBinding.timerContentView) {
            mTimerSetupState = mBinding.timerSetupView.getState();
            outState.putSerializable(KEY_TIMER_SETUP_STATE, mTimerSetupState);
        }

        outState.putInt(KEY_TIMER_ID_TO_DELETE, mTimerIdToDelete);
    }

    @Override
    public void startRunnable() {
        startUpdatingTime();
    }

    @Override
    public void stopRunnable() {
        stopUpdatingTime();
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        updateFab(fab);

        fab.setOnLongClickListener(v -> {
            CustomTooltip.showAbove(v, fab.getContentDescription().toString(), true);
            return true;
        });
    }

    @Override
    public void onMorphFab(@NonNull ImageView fab) {
        // Update the fab's drawable to match the current timer state.
        updateFab(fab);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        if (mCurrentView == mBinding.timerContentView) {
            left.setVisibility(INVISIBLE);
            right.setVisibility(INVISIBLE);

        } else if (mCurrentView == getTimerCreationView()) {
            right.setVisibility(INVISIBLE);

            left.setClickable(true);
            left.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_cancel));
            left.setContentDescription(getString(android.R.string.cancel));
            // If no timers yet exist, the user is forced to create the first one.
            left.setVisibility(hasTimers() ? VISIBLE : INVISIBLE);
            left.setOnClickListener(v -> {
                resetTimerCreationViews();
                animateToView(mBinding.timerContentView, false);
                left.announceForAccessibility(getString(R.string.timer_canceled));
                Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.CLOCK_TICK);
            });
        }
    }

    @Override
    public void onFabClick() {
        if (mCurrentView == mBinding.timerContentView) {
            if (mSettings.isSingleTimerMode) {
                List<Timer> timers = DataModel.getDataModel().getTimers();

                if (!DataModel.getDataModel().getTimers().isEmpty()) {
                    Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                    DataModel.getDataModel().removeTimer(timers.get(0));
                }
            } else {
                animateToView(getTimerCreationView(), true);
            }
        } else if (mCurrentView == getTimerCreationView()) {
            mCreatingTimer = true;

            try {
                // Create the new timer.
                final long timerLength = getTimeInMillis();
                String defaultLabel = Utils.buildDefaultTimerLabel(requireContext(), timerLength);
                String defaultTimeToAddToTimer = String.valueOf(SettingsDAO.getDefaultTimeToAddToTimer(mPrefs));
                final Timer timer = DataModel.getDataModel().addTimer(
                    timerLength, defaultLabel, defaultTimeToAddToTimer, mSettings.isSingleTimerMode);
                Events.sendTimerEvent(R.string.action_create, R.string.label_deskclock);

                // Start the new timer.
                DataModel.getDataModel().startTimer(timer);
                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            } finally {
                mCreatingTimer = false;
            }

            // Return to the list of timers.
            animateToView(mBinding.timerContentView, false);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCurrentView == mBinding.timerSetupView) {
            return mBinding.timerSetupView.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Display the view that creates a new timer.
     */
    private void showCreateTimerView(int updateTypes) {
        // Stop periodic updates for active timers.
        stopUpdatingTime();

        // Show the creation view; hide the timer view.
        mBinding.timerContentView.setVisibility(GONE);

        // Reset all possible time picker views to be hidden in order to only show one of them later
        mBinding.timerSetupView.setVisibility(GONE);
        mBinding.timerSpinnerSetupView.setVisibility(GONE);

        // Record the fact that the create view is visible.
        mCurrentView = getTimerCreationView();
        mCurrentView.setVisibility(VISIBLE);

        // Update the fab and buttons.
        updateFab(updateTypes);
    }

    /**
     * Display the view that lists all existing timers.
     */
    private void showTimersView(int updateTypes) {
        // Clear any defunct timer creation state; the next timer creation starts fresh.
        mTimerSetupState = null;

        // Show the timer view; hide the creation view.
        mBinding.timerContentView.setVisibility(VISIBLE);
        mBinding.timerSetupView.setVisibility(GONE);
        mBinding.timerSpinnerSetupView.setVisibility(GONE);

        // Record the fact that the create view is visible.
        mCurrentView = mBinding.timerContentView;

        // Start periodic updates for active timers.
        startUpdatingTime();

        // Update the fab and buttons.
        updateFab(updateTypes);

        updateWarningBannerVisibility();
    }

    /**
     * @param toView      one of "timerView" or "timerSetup"
     * @param animateDown {@code true} if the views should animate upwards, otherwise downwards
     */
    private void animateToView(final View toView, final boolean animateDown) {
        if (mCurrentView == toView) {
            return;
        }

        final boolean toTimers = toView == mBinding.timerContentView;
        if (toTimers) {
            mBinding.timerContentView.setVisibility(VISIBLE);
        } else {
            getTimerCreationView().setVisibility(VISIBLE);
        }
        // Avoid double-taps by enabling/disabling the set of buttons active on the new view.
        updateFab(BUTTONS_DISABLE);

        final long animationDuration = 600;

        final ViewTreeObserver viewTreeObserver = toView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.removeOnPreDrawListener(this);
                }

                final float distanceY = requireView().getHeight() + requireView().getY();
                final float translationDistance = animateDown ? -distanceY : distanceY;

                toView.setTranslationY(-translationDistance);
                mCurrentView.setTranslationY(0f);
                toView.setAlpha(0f);
                mCurrentView.setAlpha(1f);

                final Animator translateCurrent = ObjectAnimator.ofFloat(mCurrentView, TRANSLATION_Y, translationDistance);
                final Animator translateNew = ObjectAnimator.ofFloat(toView, TRANSLATION_Y, 0f);
                final AnimatorSet translationAnimatorSet = new AnimatorSet();
                translationAnimatorSet.playTogether(translateCurrent, translateNew);
                translationAnimatorSet.setDuration(animationDuration);
                translationAnimatorSet.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

                final Animator fadeOutAnimator = ObjectAnimator.ofFloat(mCurrentView, ALPHA, 0f);
                fadeOutAnimator.setDuration(animationDuration / 2);
                fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);

                        // The fade-out animation and fab-shrinking animation should run together.
                        updateFab(FAB_AND_BUTTONS_SHRINK);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (toTimers) {
                            showTimersView(FAB_AND_BUTTONS_EXPAND);

                            resetTimerCreationViews();
                        } else {
                            showCreateTimerView(FAB_AND_BUTTONS_EXPAND);
                        }

                        // Update the fab and button states now that the correct view is visible and
                        // before the animation to expand the fab and buttons starts.
                        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    }
                });

                final Animator fadeInAnimator = ObjectAnimator.ofFloat(toView, ALPHA, 1f);
                fadeInAnimator.setDuration(animationDuration / 2);
                fadeInAnimator.setStartDelay(animationDuration / 2);

                final AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(fadeOutAnimator, fadeInAnimator, translationAnimatorSet);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mBinding.timerContentView.setTranslationY(0f);
                        mBinding.timerSetupView.setTranslationY(0f);
                        mBinding.timerSpinnerSetupView.setTranslationY(0f);
                        mBinding.timerContentView.setAlpha(1f);
                        mBinding.timerSetupView.setAlpha(1f);
                        mBinding.timerSpinnerSetupView.setAlpha(1f);
                    }
                });

                animatorSet.start();

                return true;
            }
        });
    }

    private void updateFab(@NonNull ImageView fab) {
        if (mCurrentView == mBinding.timerContentView) {
            if (mSettings.isSingleTimerMode) {
                fab.setImageResource(R.drawable.ic_delete);
                fab.setContentDescription(getString(R.string.delete));
            } else {
                fab.setImageResource(R.drawable.ic_add);
                fab.setContentDescription(getString(R.string.timer_add_timer));
            }

            fab.setVisibility(VISIBLE);
        } else if (mCurrentView == getTimerCreationView()) {
            if (hasValidInput()) {
                fab.setImageResource(R.drawable.ic_fab_play);
                fab.setContentDescription(getString(R.string.timer_start));
                fab.setVisibility(VISIBLE);
            } else {
                fab.setContentDescription(null);
                fab.setVisibility(INVISIBLE);
            }
        }
    }

    private boolean hasValidInput() {
        if (isSpinnerCreationView()) {
            return mBinding.timerSpinnerSetupView.getValue().toMillis() != 0;
        } else {
            return mBinding.timerSetupView.hasValidInput();
        }
    }

    private long getTimeInMillis() {
        if (isSpinnerCreationView()) {
            return mBinding.timerSpinnerSetupView.getValue().toMillis();
        } else {
            return mBinding.timerSetupView.getTimeInMillis();
        }
    }

    private ViewGroup getTimerCreationView() {
        if (isSpinnerCreationView()) {
            return mBinding.timerSpinnerSetupView;
        } else {
            return mBinding.timerSetupView;
        }
    }

    /**
     * Reset the state of timer creation views.
     */
    private void resetTimerCreationViews() {
        if (isSpinnerCreationView()) {
            mBinding.timerSpinnerSetupView.reset();
            isTimerValueValid = false;
        } else {
            mBinding.timerSetupView.reset();
        }
    }

    private boolean isSpinnerCreationView() {
        return SettingsDAO.getTimerCreationViewStyle(mPrefs).equals(TIMER_CREATION_VIEW_SPINNER_STYLE);
    }

    public void startUpdatingTime() {
        if (!isTabSelected() || !DataModel.getDataModel().hasActiveTimer()) {
            return;
        }

        if (mAdapter != null) {
            mAdapter.updateTime();
        }
    }

    public void stopUpdatingTime() {
        if (mAdapter != null) {
            mAdapter.stopAllUpdating();
        }
    }

    private void refreshSettings() {
        String timerFontPath = SettingsDAO.getTimerDurationFont(mPrefs);
        mSettings.timerTimeTypeface = ThemeUtils.boldTypeface(timerFontPath);

        mSettings.is24HourFormat = DataModel.getDataModel().is24HourFormat();
        mSettings.timerEndTimeFormatPattern = mSettings.is24HourFormat
            ? ClockUtils.get24ModeFormat(false, false)
            : ClockUtils.get12ModeFormat(requireContext(), 0.8f, false, false, false, true, false);

        mSettings.isSingleTimerMode = SettingsDAO.isSingleTimerModeEnabled(mPrefs);
        mSettings.isTimerEndTimeDisplayed = SettingsDAO.isTimerEndTimeDisplayed(mPrefs);
        mSettings.areTimerButtonPositionsInverted = SettingsDAO.areTimerButtonPositionsInverted(mPrefs);
        mSettings.isIndicatorStateDisplay = SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs);

        mSettings.colorPaused = SettingsDAO.getPausedTimerIndicatorColor(mPrefs);
        mSettings.colorRunning = SettingsDAO.getRunningTimerIndicatorColor(mPrefs);
        mSettings.colorExpired = SettingsDAO.getExpiredTimerIndicatorColor(mPrefs);
        mSettings.colorMissed = SettingsDAO.getMissedTimerIndicatorColor(mPrefs);

        mSettings.timerSorting = SettingsDAO.getTimerSortingPreference(mPrefs);
        mIsManualSorting = mSettings.timerSorting.equals(DEFAULT_SORT_TIMER_MANUALLY);
    }

    private void applySettingsChanges() {
        refreshSettings();

        if (mAdapter != null) {
            mAdapter.updateSettings(mSettings);
        }

        if (mBinding != null) {
            mBinding.timerSetupView.updateTimerSetupTimeFont(mSettings.timerTimeTypeface);
        }

        if (mTouchHelperCallback != null) {
            mTouchHelperCallback.setManualSorting(mIsManualSorting);
        }

        mAreSettingsChanged = false;
    }

    public void confirmAndDeleteTimer(Timer timer) {
        if (SettingsDAO.isWarningDisplayedBeforeDeletingTimer(mPrefs)) {
            mTimerIdToDelete = timer.getId();
            mActiveDialog = warningDialogBeforeDeletingTimer(timer);
            mActiveDialog.show();
        } else {
            DataModel.getDataModel().removeTimer(timer);
        }
    }

    private AlertDialog warningDialogBeforeDeletingTimer(Timer timer) {
        // Get the title of the timer if there is one; otherwise, get the total duration.
        final String dialogMessage;
        if (timer.getLabel().isEmpty()) {
            dialogMessage = getString(R.string.warning_dialog_message, timer.getTotalDuration());
        } else {
            dialogMessage = getString(R.string.warning_dialog_message, timer.getLabel());
        }

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_delete),
            getString(R.string.warning_dialog_title),
            dialogMessage,
            null,
            getString(android.R.string.ok),
            (d, w) -> DataModel.getDataModel().removeTimer(timer),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mTimerIdToDelete =  -1)),
            CustomDialog.SoftInputMode.NONE
        );
    }

    private boolean hasTimers() {
        return !DataModel.getDataModel().getTimers().isEmpty();
    }

    private boolean hasMultipleTimers() {
        return DataModel.getDataModel().getTimers().size() > 1;
    }

    private RecyclerView.LayoutManager getLayoutManager(Context context) {
        if (mIsTablet) {
            int spanCount = hasMultipleTimers() ? (mIsLandscape ? 3 : 2) : 1;
            return new GridLayoutManager(context, spanCount);
        }

        return new LinearLayoutManager(context, mIsLandscape
            ? LinearLayoutManager.HORIZONTAL
            : LinearLayoutManager.VERTICAL, false);
    }

    private void handleItemTouchHelper() {
        if (hasMultipleTimers()) {
            mItemTouchHelper.attachToRecyclerView(mBinding.timerRecyclerView);
        } else {
            mItemTouchHelper.attachToRecyclerView(null);
        }
    }

    private void adjustWakeLock() {
        if (isAdded() && getActivity() instanceof DeskClock deskClock) {
            deskClock.updateKeepScreenOn();
        }
    }

    /**
     * Updates the visibility of the volume warning banner.
     */
    public void updateWarningBannerVisibility() {
        boolean isStreamLow = RingtoneUtils.isAlarmStreamLow(requireContext());
        boolean shouldShow = SettingsDAO.isLowAlarmVolumeWarningDisplayed(mPrefs)
            && isStreamLow
            && DataModel.getDataModel().hasRunningTimer();

        int targetVisibility = shouldShow ? VISIBLE : GONE;

        if (mBinding.timerVolumeBanner.volumeWarningBanner.getVisibility() != targetVisibility) {
            mBinding.timerRecyclerView.post(() -> {
                TransitionManager.beginDelayedTransition(mBinding.timerContentView);
                mBinding.timerVolumeBanner.volumeWarningBanner.setVisibility(targetVisibility);
            });
        }
    }

    /**
     * Update the fab in response to the visible timer changing.
     */
    private class TimerWatcher implements TimerListener {
        @Override
        public void timerAdded(Timer timer) {
            // Ensure the timer list is displayed if the UI loaded faster than the database during app launch,
            // or if a timer was added externally.
            if (mCurrentView != mBinding.timerContentView && !mCreatingTimer) {
                showTimersView(FAB_AND_BUTTONS_IMMEDIATE);
            }

            // If the timer is being created via this fragment avoid adjusting the fab.
            // Timer setup view is about to be animated away in response to this timer creation.
            // Changes to the fab immediately preceding that animation are jarring.
            if (!mCreatingTimer) {
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            }

            // Required to adjust the layout for tablets that use either a GridLayoutManager or a LinearLayoutManager.
            if (mIsTablet && mBinding.timerRecyclerView.getLayoutManager() instanceof GridLayoutManager gridLayoutManager) {
                int newSpanCount = hasMultipleTimers() ? (mIsLandscape ? 3 : 2) : 1;

                if (gridLayoutManager.getSpanCount() != newSpanCount) {
                    gridLayoutManager.setSpanCount(newSpanCount);
                }
            }

            // Required to attach the ItemTouchHelper when there is more than one timer.
            handleItemTouchHelper();
        }

        @Override
        public void timerUpdated(Timer before, Timer after) {
            int position = mAdapter.getTimers().indexOf(after);
            boolean justStarted = before.isReset() && !after.isReset();
            boolean justPaused = !before.isPaused() && after.isPaused();
            boolean justReset = !before.isReset() && after.isReset();
            boolean justResumed = before.isPaused() && !after.isPaused();
            boolean justExpired = !before.isExpired() && after.isExpired();
            boolean timeAdded = before.getTotalLength() != after.getTotalLength();
            boolean stoppedExpired = (before.isExpired() && !after.isExpired()) || (before.isMissed() && !after.isMissed());

            RecyclerView.LayoutManager layoutManager = mBinding.timerRecyclerView.getLayoutManager();

            if (layoutManager != null && hasMultipleTimers() && position != RecyclerView.NO_POSITION) {
                if (justReset || stoppedExpired) {
                    if (mIsTablet && layoutManager instanceof LinearLayoutManager linearLayoutManager) {
                        int firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
                        View firstView = linearLayoutManager.findViewByPosition(firstVisible);
                        int offset = (firstView != null) ? firstView.getTop() : 0;

                        linearLayoutManager.scrollToPositionWithOffset(firstVisible, offset);
                    }
                } else if (justStarted || timeAdded || justResumed || justExpired) {
                    layoutManager.scrollToPosition(position);
                } else if ((justPaused) && layoutManager instanceof LinearLayoutManager linearLayoutManager) {
                    int firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
                    View firstView = linearLayoutManager.findViewByPosition(firstVisible);
                    int offset = (firstView != null) ? firstView.getTop() : 0;

                    linearLayoutManager.scrollToPositionWithOffset(firstVisible, offset);
                }
            }

            updateWarningBannerVisibility();

            adjustWakeLock();
        }

        @Override
        public void timerRemoved(Timer timer) {
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);

            if (mCurrentView == mBinding.timerContentView && mAdapter.getItemCount() == 0) {
                animateToView(getTimerCreationView(), true);
            }

            // Required to adjust the layout for tablets that use either a GridLayoutManager or a LinearLayoutManager.
            if (mIsTablet && mBinding.timerRecyclerView.getLayoutManager() instanceof GridLayoutManager gridLayoutManager) {
                int newSpanCount = hasMultipleTimers() ? (mIsLandscape ? 3 : 2) : 1;

                if (gridLayoutManager.getSpanCount() != newSpanCount) {
                    gridLayoutManager.setSpanCount(newSpanCount);
                }
            }

            // Required to detach the ItemTouchHelper when there is only one timer left.
            handleItemTouchHelper();

            updateWarningBannerVisibility();

            adjustWakeLock();
        }
    }

    /**
     * A custom {@link RecyclerView.ItemDecoration} that applies consistent and even spacing
     * between items in a grid layout for tablets.
     *
     * <p>It dynamically calculates the item offsets to ensure that both the internal
     * spacing between columns/rows and the outer edges of the grid are perfectly aligned.
     * This decoration only affects the layout if the {@link RecyclerView} uses a
     * {@link GridLayoutManager}.</p>
     */
    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int margin;
        private final int spacing;
        private final boolean mIsRTL;

        public GridSpacingItemDecoration(Context context, DisplayMetrics displayMetrics) {
            this.margin = (int) dpToPx(10, displayMetrics);
            this.spacing = (int) dpToPx(2, displayMetrics);
            this.mIsRTL = ThemeUtils.isRTL(context);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

            int position = parent.getChildAdapterPosition(view);

            if (position == RecyclerView.NO_POSITION) {
                position = parent.getChildLayoutPosition(view);
            }

            if (position < 0) {
                return;
            }

            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();

            if (layoutManager instanceof GridLayoutManager gridLayoutManager) {
                int spanCount = gridLayoutManager.getSpanCount();
                int column = position % spanCount;

                // Formula for having, for example, 10dp on the outside and 5dp on the inside
                int standardLeft = margin - column * margin / spanCount;
                int standardRight = (column + 1) * margin / spanCount;

                outRect.left = mIsRTL ? standardRight : standardLeft;
                outRect.right = mIsRTL ? standardLeft : standardRight;
                outRect.bottom = margin;
            } else if (layoutManager instanceof LinearLayoutManager linearLayoutManager) {
                if (linearLayoutManager.getOrientation() == RecyclerView.HORIZONTAL) {
                    int standardLeft = (position == 0) ? margin : spacing;
                    int itemCount = state.getItemCount();
                    int standardRight = (position == itemCount - 1) ? margin : 0;

                    outRect.left = mIsRTL ? standardRight : standardLeft;
                    outRect.right = mIsRTL ? standardLeft : standardRight;
                    outRect.bottom = margin;
                } else {
                    outRect.left = margin;
                    outRect.right = margin;
                    outRect.bottom = spacing;
                }
            }
        }
    }

}

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.best.deskclock;


import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_DRAGGING;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_SETTLING;
import static com.best.deskclock.AnimatorUtils.getScaleAnimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.best.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.best.deskclock.actionbarmenu.NightModeMenuItemController;
import com.best.deskclock.actionbarmenu.OptionsMenuManager;
import com.best.deskclock.actionbarmenu.SettingsMenuItemController;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.DataModel.SilentSetting;
import com.best.deskclock.data.DataModel.ThemeButtonBehavior;
import com.best.deskclock.data.OnSilentSettingsListener;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uidata.TabListener;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.widget.toast.SnackbarManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

/**
 * The main activity of the application which displays 4 different tabs contains alarms, world
 * clocks, timers and a stopwatch.
 */
public class DeskClock extends BaseActivity
        implements FabContainer, LabelDialogFragment.AlarmLabelDialogHandler {
    private static final String PERMISSION_POWER_OFF_ALARM =
            "org.codeaurora.permission.POWER_OFF_ALARM";
    private static final int CODE_FOR_ALARM_PERMISSION = 1;
    /**
     * Coordinates handling of context menu items.
     */
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();
    /**
     * Shrinks the {@link #mFab}, {@link #mLeftButton} and {@link #mRightButton} to nothing.
     */
    private final AnimatorSet mHideAnimation = new AnimatorSet();
    /**
     * Grows the {@link #mFab}, {@link #mLeftButton} and {@link #mRightButton} to natural sizes.
     */
    private final AnimatorSet mShowAnimation = new AnimatorSet();
    /**
     * Hides, updates, and shows only the {@link #mFab}; the buttons are untouched.
     */
    private final AnimatorSet mUpdateFabOnlyAnimation = new AnimatorSet();
    /**
     * Hides, updates, and shows only the {@link #mLeftButton} and {@link #mRightButton}.
     */
    private final AnimatorSet mUpdateButtonsOnlyAnimation = new AnimatorSet();
    /**
     * Automatically starts the {@link #mShowAnimation} after {@link #mHideAnimation} ends.
     */
    private final AnimatorListenerAdapter mAutoStartShowListener = new AutoStartShowListener();
    /**
     * Updates the user interface to reflect the selected tab from the backing model.
     */
    private final TabListener mTabChangeWatcher = new TabChangeWatcher();
    /**
     * Shows/hides a snackbar explaining which setting is suppressing alarms from firing.
     */
    private final OnSilentSettingsListener mSilentSettingChangeWatcher =
            new SilentSettingChangeWatcher();
    @SuppressLint("NonConstantResourceId")
    private final NavigationBarView.OnItemSelectedListener mNavigationListener
            = item -> {
        UiDataModel.Tab tab = null;
        switch (item.getItemId()) {
            case R.id.page_alarm:
                tab = UiDataModel.Tab.ALARMS;
                break;

            case R.id.page_clock:
                tab = UiDataModel.Tab.CLOCKS;
                break;

            case R.id.page_timer:
                tab = UiDataModel.Tab.TIMERS;
                break;

            case R.id.page_stopwatch:
                tab = UiDataModel.Tab.STOPWATCH;
                break;
        }

        if (tab != null) {
            UiDataModel.getUiDataModel().setSelectedTab(tab);
            return true;
        }

        return false;
    };
    private ThemeButtonBehavior mThemeBehavior;
    /**
     * Displays a snackbar explaining why alarms may not fire or may fire silently.
     */
    private Runnable mShowSilentSettingSnackbarRunnable;
    /**
     * The view to which snackbar items are anchored.
     */
    private View mSnackbarAnchor;
    /**
     * The current display state of the {@link #mFab}.
     */
    private FabState mFabState = FabState.SHOWING;
    /**
     * The single floating-action button shared across all tabs in the user interface.
     */
    private ImageView mFab;
    /**
     * The button left of the {@link #mFab} shared across all tabs in the user interface.
     */
    private Button mLeftButton;
    /**
     * The button right of the {@link #mFab} shared across all tabs in the user interface.
     */
    private Button mRightButton;
    /**
     * The ViewPager that pages through the fragments representing the content of the tabs.
     */
    private ViewPager mFragmentTabPager;
    /**
     * Generates the fragments that are displayed by the {@link #mFragmentTabPager}.
     */
    private FragmentTabPagerAdapter mFragmentTabPagerAdapter;
    /**
     * The view that displays the current tab's title
     */
    private TextView mTitleView;
    /**
     * The bottom navigation bar
     */
    private BottomNavigationView mBottomNavigation;
    /**
     * {@code true} when a settings change necessitates recreating this activity.
     */
    private boolean mRecreateActivity;

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        // Fragments may query the latest intent for information, so update the intent.
        setIntent(newIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mThemeBehavior = DataModel.getDataModel().getThemeButtonBehavior();
        if (mThemeBehavior == DataModel.ThemeButtonBehavior.DARK) {
            getTheme().applyStyle(R.style.Theme_DeskClock_Dark, true);
        }
        if (mThemeBehavior == DataModel.ThemeButtonBehavior.LIGHT) {
            getTheme().applyStyle(R.style.Theme_DeskClock_Light, true);
        }

        super.onCreate(savedInstanceState);


        setContentView(R.layout.desk_clock);
        mSnackbarAnchor = findViewById(R.id.content);

        checkPermissions();

        // Configure the toolbar.
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Configure the menu item controllers add behavior to the toolbar.
        mOptionsMenuManager.addMenuItemController(
                new NightModeMenuItemController(this), new SettingsMenuItemController(this));
        mOptionsMenuManager.addMenuItemController(
                MenuItemControllerFactory.getInstance().buildMenuItemControllers(this));

        // Inflate the menu during creation to avoid a double layout pass. Otherwise, the menu
        // inflation occurs *after* the initial draw and a second layout pass adds in the menu.
        onCreateOptionsMenu(toolbar.getMenu());

        // Configure the buttons shared by the tabs.
        mFab = findViewById(R.id.fab);
        mLeftButton = findViewById(R.id.left_button);
        mRightButton = findViewById(R.id.right_button);

        mFab.setOnClickListener(view -> getSelectedDeskClockFragment().onFabClick(mFab));
        mLeftButton.setOnClickListener(view -> getSelectedDeskClockFragment().onLeftButtonClick(mLeftButton));
        mRightButton.setOnClickListener(view -> getSelectedDeskClockFragment().onRightButtonClick(mRightButton));

        final long duration = UiDataModel.getUiDataModel().getShortAnimationDuration();

        final ValueAnimator hideFabAnimation = getScaleAnimator(mFab, 1f, 0f);
        final ValueAnimator showFabAnimation = getScaleAnimator(mFab, 0f, 1f);

        final ValueAnimator leftHideAnimation = getScaleAnimator(mLeftButton, 1f, 0f);
        final ValueAnimator rightHideAnimation = getScaleAnimator(mRightButton, 1f, 0f);
        final ValueAnimator leftShowAnimation = getScaleAnimator(mLeftButton, 0f, 1f);
        final ValueAnimator rightShowAnimation = getScaleAnimator(mRightButton, 0f, 1f);

        hideFabAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                getSelectedDeskClockFragment().onUpdateFab(mFab);
            }
        });

        leftHideAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                getSelectedDeskClockFragment().onUpdateFabButtons(mLeftButton, mRightButton);
            }
        });

        // Build the reusable animations that hide and show the fab and left/right buttons.
        // These may be used independently or be chained together.
        mHideAnimation
                .setDuration(duration)
                .play(hideFabAnimation)
                .with(leftHideAnimation)
                .with(rightHideAnimation);

        mShowAnimation
                .setDuration(duration)
                .play(showFabAnimation)
                .with(leftShowAnimation)
                .with(rightShowAnimation);

        // Build the reusable animation that hides and shows only the fab.
        mUpdateFabOnlyAnimation
                .setDuration(duration)
                .play(showFabAnimation)
                .after(hideFabAnimation);

        // Build the reusable animation that hides and shows only the buttons.
        mUpdateButtonsOnlyAnimation
                .setDuration(duration)
                .play(leftShowAnimation)
                .with(rightShowAnimation)
                .after(leftHideAnimation)
                .after(rightHideAnimation);

        // Customize the view pager.
        mFragmentTabPagerAdapter = new FragmentTabPagerAdapter(this);
        mFragmentTabPager = findViewById(R.id.desk_clock_pager);
        // Keep all four tabs to minimize jank.
        mFragmentTabPager.setOffscreenPageLimit(3);
        // Set Accessibility Delegate to null so view pager doesn't intercept movements and
        // prevent the fab from being selected.
        mFragmentTabPager.setAccessibilityDelegate(null);
        // Mirror changes made to the selected page of the view pager into UiDataModel.
        mFragmentTabPager.addOnPageChangeListener(new PageChangeWatcher());
        mFragmentTabPager.setAdapter(mFragmentTabPagerAdapter);

        // Mirror changes made to the selected tab into UiDataModel.
        mBottomNavigation = findViewById(R.id.bottom_view);
        mBottomNavigation.setOnItemSelectedListener(mNavigationListener);

        // Honor changes to the selected tab from outside entities.
        UiDataModel.getUiDataModel().addTabListener(mTabChangeWatcher);

        mTitleView = findViewById(R.id.title_view);
    }

    @Override
    protected void onStart() {
        DataModel.getDataModel().addSilentSettingsListener(mSilentSettingChangeWatcher);
        DataModel.getDataModel().setApplicationInForeground(true);


        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ViewPager does not save state; this honors the selected tab in the user interface.
        updateCurrentTab();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mRecreateActivity) {
            mRecreateActivity = false;

            // A runnable must be posted here or the new DeskClock activity will be recreated in a
            // paused state, even though it is the foreground activity.
            mFragmentTabPager.post(this::recreate);
        }
    }

    @Override
    protected void onStop() {
        DataModel.getDataModel().removeSilentSettingsListener(mSilentSettingChangeWatcher);
        if (!isChangingConfigurations()) {
            DataModel.getDataModel().setApplicationInForeground(false);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        UiDataModel.getUiDataModel().removeTabListener(mTabChangeWatcher);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mOptionsMenuManager.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * Called by the LabelDialogFormat class after the dialog is finished.
     */
    @Override
    public void onDialogLabelSet(Alarm alarm, String label, String tag) {
        final Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) frag).setLabel(alarm, label);
        }
    }

    /**
     * Listens for keyboard activity for the tab fragments to handle if necessary. A tab may want to
     * respond to key presses even if they are not currently focused.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return getSelectedDeskClockFragment().onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public void updateFab(@UpdateFabFlag int updateType) {
        final DeskClockFragment f = getSelectedDeskClockFragment();

        switch (updateType & FAB_ANIMATION_MASK) {
            case FAB_SHRINK_AND_EXPAND:
                mUpdateFabOnlyAnimation.start();
                break;
            case FAB_IMMEDIATE:
                f.onUpdateFab(mFab);
                break;
            case FAB_MORPH:
                f.onMorphFab(mFab);
                break;
        }
        if ((updateType & FAB_REQUEST_FOCUS_MASK) == FAB_REQUEST_FOCUS) {
            mFab.requestFocus();
        }
        switch (updateType & BUTTONS_ANIMATION_MASK) {
            case BUTTONS_IMMEDIATE:
                f.onUpdateFabButtons(mLeftButton, mRightButton);
                break;
            case BUTTONS_SHRINK_AND_EXPAND:
                mUpdateButtonsOnlyAnimation.start();
                break;
        }
        if ((updateType & BUTTONS_DISABLE_MASK) == BUTTONS_DISABLE) {
            mLeftButton.setClickable(false);
            mRightButton.setClickable(false);
        }
        switch (updateType & FAB_AND_BUTTONS_SHRINK_EXPAND_MASK) {
            case FAB_AND_BUTTONS_SHRINK:
                mHideAnimation.start();
                break;
            case FAB_AND_BUTTONS_EXPAND:
                mShowAnimation.start();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Recreate the activity if any settings have been changed
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SettingsMenuItemController.REQUEST_CHANGE_SETTINGS
                && resultCode == RESULT_OK) {
            mRecreateActivity = true;
        }
    }

    private void checkPermissions() {
        if (checkSelfPermission(PERMISSION_POWER_OFF_ALARM)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERMISSION_POWER_OFF_ALARM}, CODE_FOR_ALARM_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_FOR_ALARM_PERMISSION) {
            LogUtils.i("Power off alarm permission is granted.");
        }
    }

    /**
     * Configure the {@link #mFragmentTabPager} and {@link #mBottomNavigation} to display
     * UiDataModel's selected tab.
     */
    @SuppressLint("ResourceType")
    private void updateCurrentTab() {
        // Fetch the selected tab from the source of truth: UiDataModel.
        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        // Update the selected tab in the mBottomNavigation if it does not agree with UiDataModel.
        mBottomNavigation.setSelectedItemId(selectedTab.getPageResId());

        // Update the selected fragment in the viewpager if it does not agree with UiDataModel.
        for (int i = 0; i < mFragmentTabPagerAdapter.getCount(); i++) {
            final DeskClockFragment fragment = mFragmentTabPagerAdapter.getDeskClockFragment(i);
            if (fragment.isTabSelected() && mFragmentTabPager.getCurrentItem() != i) {
                mFragmentTabPager.setCurrentItem(i);
                break;
            }
        }

        mTitleView.setText(selectedTab.getLabelResId());
    }

    /**
     * @return the DeskClockFragment that is currently selected according to UiDataModel
     */
    private DeskClockFragment getSelectedDeskClockFragment() {
        for (int i = 0; i < mFragmentTabPagerAdapter.getCount(); i++) {
            final DeskClockFragment fragment = mFragmentTabPagerAdapter.getDeskClockFragment(i);
            if (fragment.isTabSelected()) {
                return fragment;
            }
        }
        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        throw new IllegalStateException("Unable to locate selected fragment (" + selectedTab + ")");
    }

    /**
     * @return a Snackbar that displays the message with the given id for 5 seconds
     */
    private Snackbar createSnackbar(@StringRes int messageId) {
        return Snackbar.make(mSnackbarAnchor, messageId, 5000 /* duration */);
    }

    /**
     * Models the interesting state of display the {@link #mFab} button may inhabit.
     */
    private enum FabState {SHOWING, HIDE_ARMED, HIDING}

    /**
     * As the view pager changes the selected page, update the model to record the new selected tab.
     */
    private final class PageChangeWatcher implements OnPageChangeListener {

        /**
         * The last reported page scroll state; used to detect exotic state changes.
         */
        private int mPriorState = SCROLL_STATE_IDLE;

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Only hide the fab when a non-zero drag distance is detected. This prevents
            // over-scrolling from needlessly hiding the fab.
            if (mFabState == FabState.HIDE_ARMED && positionOffsetPixels != 0) {
                mFabState = FabState.HIDING;
                mHideAnimation.start();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (mPriorState == SCROLL_STATE_IDLE && state == SCROLL_STATE_SETTLING) {
                // The user has tapped a tab button; play the hide and show animations linearly.
                mHideAnimation.addListener(mAutoStartShowListener);
                mHideAnimation.start();
                mFabState = FabState.HIDING;
            } else if (mPriorState == SCROLL_STATE_SETTLING && state == SCROLL_STATE_DRAGGING) {
                // The user has interrupted settling on a tab and the fab button must be re-hidden.
                if (mShowAnimation.isStarted()) {
                    mShowAnimation.cancel();
                }
                if (mHideAnimation.isStarted()) {
                    // Let the hide animation finish naturally; don't auto show when it ends.
                    mHideAnimation.removeListener(mAutoStartShowListener);
                } else {
                    // Start and immediately end the hide animation to jump to the hidden state.
                    mHideAnimation.start();
                    mHideAnimation.end();
                }
                mFabState = FabState.HIDING;

            } else if (state != SCROLL_STATE_DRAGGING && mFabState == FabState.HIDING) {
                // The user has lifted their finger; show the buttons now or after hide ends.
                if (mHideAnimation.isStarted()) {
                    // Finish the hide animation and then start the show animation.
                    mHideAnimation.addListener(mAutoStartShowListener);
                } else {
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    mShowAnimation.start();

                    // The animation to show the fab has begun; update the state to showing.
                    mFabState = FabState.SHOWING;
                }
            } else if (state == SCROLL_STATE_DRAGGING) {
                // The user has started a drag so arm the hide animation.
                mFabState = FabState.HIDE_ARMED;
            }

            // Update the last known state.
            mPriorState = state;
        }

        @Override
        public void onPageSelected(int position) {
            mFragmentTabPagerAdapter.getDeskClockFragment(position).selectTab();
        }
    }

    /**
     * If this listener is attached to {@link #mHideAnimation} when it ends, the corresponding
     * {@link #mShowAnimation} is automatically started.
     */
    private final class AutoStartShowListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            // Prepare the hide animation for its next use; by default do not auto-show after hide.
            mHideAnimation.removeListener(mAutoStartShowListener);

            // Update the buttons now that they are no longer visible.
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);

            // Automatically start the grow animation now that shrinking is complete.
            mShowAnimation.start();

            // The animation to show the fab has begun; update the state to showing.
            mFabState = FabState.SHOWING;
        }
    }


    /**
     * Shows/hides a snackbar as silencing settings are enabled/disabled.
     */
    private final class SilentSettingChangeWatcher implements OnSilentSettingsListener {
        @Override
        public void onSilentSettingsChange(SilentSetting before, SilentSetting after) {
            if (mShowSilentSettingSnackbarRunnable != null) {
                mSnackbarAnchor.removeCallbacks(mShowSilentSettingSnackbarRunnable);
                mShowSilentSettingSnackbarRunnable = null;
            }

            if (after == null) {
                SnackbarManager.dismiss();
            } else {
                mShowSilentSettingSnackbarRunnable = new ShowSilentSettingSnackbarRunnable(after);
                mSnackbarAnchor.postDelayed(mShowSilentSettingSnackbarRunnable, SECOND_IN_MILLIS);
            }
        }
    }

    /**
     * Displays a snackbar that indicates a system setting is currently silencing alarms.
     */
    private final class ShowSilentSettingSnackbarRunnable implements Runnable {

        private final SilentSetting mSilentSetting;

        private ShowSilentSettingSnackbarRunnable(SilentSetting silentSetting) {
            mSilentSetting = silentSetting;
        }

        public void run() {
            // Create a snackbar with a message explaining the setting that is silencing alarms.
            final Snackbar snackbar = createSnackbar(mSilentSetting.getLabelResId());

            // Set the associated corrective action if one exists.
            if (mSilentSetting.isActionEnabled(DeskClock.this)) {
                final int actionResId = mSilentSetting.getActionResId();
                snackbar.setAction(actionResId, mSilentSetting.getActionListener());
            }

            SnackbarManager.show(snackbar);
        }
    }


    /**
     * As the model reports changes to the selected tab, update the user interface.
     */
    private final class TabChangeWatcher implements TabListener {
        @Override
        public void selectedTabChanged(UiDataModel.Tab oldSelectedTab,
                                       UiDataModel.Tab newSelectedTab) {
            // Update the view pager and tab layout to agree with the model.
            updateCurrentTab();

            // Avoid sending events for the initial tab selection on launch and re-selecting a tab
            // after a configuration change.
            if (DataModel.getDataModel().isApplicationInForeground()) {
                switch (newSelectedTab) {
                    case ALARMS:
                        Events.sendAlarmEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case CLOCKS:
                        Events.sendClockEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case TIMERS:
                        Events.sendTimerEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case STOPWATCH:
                        Events.sendStopwatchEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                }
            }

            // If the hide animation has already completed, the buttons must be updated now when the
            // new tab is known. Otherwise they are updated at the end of the hide animation.
            if (!mHideAnimation.isStarted()) {
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            }
        }
    }
}

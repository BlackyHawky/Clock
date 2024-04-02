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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS;
import static android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;
import static android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT;
import static android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS;
import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_DRAGGING;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_SETTLING;
import static com.best.deskclock.AnimatorUtils.getScaleAnimator;
import static com.best.deskclock.settings.SettingsActivity.KEY_AMOLED_DARK_MODE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.best.deskclock.bedtime.BedtimeService;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.DataModel.SilentSetting;
import com.best.deskclock.data.OnSilentSettingsListener;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.settings.SettingsActivity;
import com.best.deskclock.stopwatch.StopwatchService;
import com.best.deskclock.timer.TimerService;
import com.best.deskclock.uidata.TabListener;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.widget.toast.SnackbarManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

/**
 * The main activity of the application which displays 5 different tabs contains alarms, world
 * clocks, timers, stopwatch and bedtime.
 */
public class DeskClock extends AppCompatActivity
        implements FabContainer, LabelDialogFragment.AlarmLabelDialogHandler {

    private static final String PERMISSION_POWER_OFF_ALARM = "org.codeaurora.permission.POWER_OFF_ALARM";
    private static final int CODE_FOR_POWER_OFF_ALARM = 1;

    public static final int REQUEST_CHANGE_SETTINGS = 10;

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
    private final OnSilentSettingsListener mSilentSettingChangeWatcher = new SilentSettingChangeWatcher();

    private final NavigationBarView.OnItemSelectedListener mNavigationListener = item -> {
        UiDataModel.Tab tab = null;
        int itemId = item.getItemId();
        if (itemId == R.id.page_alarm) {
            tab = UiDataModel.Tab.ALARMS;
        } else if (itemId == R.id.page_clock) {
            tab = UiDataModel.Tab.CLOCKS;
        } else if (itemId == R.id.page_timer) {
            tab = UiDataModel.Tab.TIMERS;
        } else if (itemId == R.id.page_stopwatch) {
            tab = UiDataModel.Tab.STOPWATCH;
        } else if (itemId == R.id.page_bedtime) {
            tab = UiDataModel.Tab.BEDTIME;
        }

        if (tab != null) {
            UiDataModel.getUiDataModel().setSelectedTab(tab);
            return true;
        }

        return false;
    };

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
    private ImageView mLeftButton;

    /**
     * The button right of the {@link #mFab} shared across all tabs in the user interface.
     */
    private ImageView mRightButton;

    /**
     * The ViewPager that pages through the fragments representing the content of the tabs.
     */
    private ViewPager mFragmentTabPager;

    /**
     * Generates the fragments that are displayed by the {@link #mFragmentTabPager}.
     */
    private FragmentTabPagerAdapter mFragmentTabPagerAdapter;

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
        super.onCreate(savedInstanceState);

        Utils.applyTheme(this);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        setContentView(R.layout.desk_clock);

        mSnackbarAnchor = findViewById(R.id.content);

        // Check the essential permissions to be granted by the user.
        checkPermissions();

        // Show dialog to present the main features of the application
        // and the necessary permissions to be granted by the user.
        firstRunDialog();

        // Displays the right tab if the application has been closed and then reopened from the notification.
        showTabFromNotifications();

        // Configure the buttons shared by the tabs.
        final Context context = getApplicationContext();
        final int fabSize = Utils.isTablet(context) ? 90 : Utils.isPortrait(context) ? 75 : 60;
        final int leftOrRightButtonSize = Utils.isTablet(context) ? 70 : Utils.isPortrait(context) ? 55 : 50;

        mFab = findViewById(R.id.fab);
        mFab.getLayoutParams().height = Utils.toPixel(fabSize, context);
        mFab.getLayoutParams().width = Utils.toPixel(fabSize, context);
        mFab.setScaleType(ImageView.ScaleType.CENTER);
        mFab.setOnClickListener(view -> getSelectedDeskClockFragment().onFabClick(mFab));

        mLeftButton = findViewById(R.id.left_button);
        mLeftButton.getLayoutParams().height = Utils.toPixel(leftOrRightButtonSize, context);
        mLeftButton.getLayoutParams().width = Utils.toPixel(leftOrRightButtonSize, context);
        mLeftButton.setScaleType(ImageView.ScaleType.CENTER);

        mRightButton = findViewById(R.id.right_button);
        mRightButton.getLayoutParams().height = Utils.toPixel(leftOrRightButtonSize, context);
        mRightButton.getLayoutParams().width = Utils.toPixel(leftOrRightButtonSize, context);
        mRightButton.setScaleType(ImageView.ScaleType.CENTER);

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
        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        mBottomNavigation = findViewById(R.id.bottom_view);
        mBottomNavigation.setOnItemSelectedListener(mNavigationListener);
        mBottomNavigation.setItemActiveIndicatorEnabled(false);
        mBottomNavigation.setItemIconTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_selected}, {android.R.attr.state_pressed}, {}},
                new int[]{getColor(R.color.md_theme_primary), getColor(R.color.md_theme_primary), getColor(R.color.md_theme_onBackground)})
        );
        if (Utils.isNight(getResources()) && getDarkMode.equals(KEY_AMOLED_DARK_MODE)) {
            mBottomNavigation.setBackgroundColor(Color.BLACK);
            mBottomNavigation.setItemTextColor(new ColorStateList(
                    new int[][]{{android.R.attr.state_selected}, {android.R.attr.state_pressed}, {}},
                    new int[]{getColor(R.color.md_theme_primary), getColor(R.color.md_theme_primary), Color.WHITE})
            );
        } else {
            mBottomNavigation.setBackgroundColor(getColor(R.color.md_theme_surface));
            mBottomNavigation.setItemTextColor(new ColorStateList(
                    new int[][]{{android.R.attr.state_selected}, {android.R.attr.state_pressed}, {}},
                    new int[]{getColor(R.color.md_theme_primary), getColor(R.color.md_theme_primary), getColor(R.color.md_theme_onBackground)})
            );
        }

        // Honor changes to the selected tab from outside entities.
        UiDataModel.getUiDataModel().addTabListener(mTabChangeWatcher);
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
        // Displays the right tab if the application has been minimized and then reopened from the notification.
        showTabFromNotifications();

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
        menu.add(0, Menu.NONE, 0, R.string.settings)
                .setIcon(R.drawable.ic_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        if (item.getItemId() == 0) {
            final Intent settingIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(settingIntent, REQUEST_CHANGE_SETTINGS);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        return getSelectedDeskClockFragment().onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void updateFab(@UpdateFabFlag int updateType) {
        final DeskClockFragment f = getSelectedDeskClockFragment();

        switch (updateType & FAB_ANIMATION_MASK) {
            case FAB_SHRINK_AND_EXPAND -> mUpdateFabOnlyAnimation.start();
            case FAB_IMMEDIATE -> f.onUpdateFab(mFab);
            case FAB_MORPH -> f.onMorphFab(mFab);
        }
        if ((updateType & FAB_REQUEST_FOCUS_MASK) == FAB_REQUEST_FOCUS) {
            mFab.requestFocus();
        }
        switch (updateType & BUTTONS_ANIMATION_MASK) {
            case BUTTONS_IMMEDIATE -> f.onUpdateFabButtons(mLeftButton, mRightButton);
            case BUTTONS_SHRINK_AND_EXPAND -> mUpdateButtonsOnlyAnimation.start();
        }
        if ((updateType & BUTTONS_DISABLE_MASK) == BUTTONS_DISABLE) {
            mLeftButton.setClickable(false);
            mRightButton.setClickable(false);
        }
        switch (updateType & FAB_AND_BUTTONS_SHRINK_EXPAND_MASK) {
            case FAB_AND_BUTTONS_SHRINK -> mHideAnimation.start();
            case FAB_AND_BUTTONS_EXPAND -> mShowAnimation.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Recreate the activity if any settings have been changed
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHANGE_SETTINGS && resultCode == RESULT_OK) {
            mRecreateActivity = true;
        }
    }

    public void firstRunDialog() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("FIRST_RUN_KEY", true);
        if (isFirstRun) {
            new AlertDialog.Builder(this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.dialog_title_for_the_first_launch)
                    .setMessage(R.string.dialog_message_for_the_first_launch)
                    .setPositiveButton(R.string.dialog_button_understood, (d, i) ->
                            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("FIRST_RUN_KEY", false)
                                    .apply()
                    )
                    .setCancelable(false)
                    .show();
        }
    }

    private void checkPermissions() {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Check permission for Power Off Alarm (only works if available in the device)
        if (checkSelfPermission(PERMISSION_POWER_OFF_ALARM) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERMISSION_POWER_OFF_ALARM}, CODE_FOR_POWER_OFF_ALARM);
        }


        // Check if Do Not Disturb is disabled in the device
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_do_not_disturb)
                    .setMessage(R.string.dialog_message_do_not_disturb)
                    .setPositiveButton(R.string.dialog_button_do_not_disturb, (dialog, position) ->
                            startActivity(new Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    .addFlags(FLAG_ACTIVITY_NEW_TASK)))
                    .setCancelable(false)
                    .show();
        }

        // Check if Ignore Battery Optimizations is disabled in the device
        if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_ignore_battery_optimization)
                    .setMessage(R.string.dialog_message_ignore_battery_optimization)
                    .setPositiveButton(R.string.dialog_button_ignore_battery_optimization, (dialog, position) ->
                            startActivity(new Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    .addFlags(FLAG_ACTIVITY_NEW_TASK)))
                    .setCancelable(false)
                    .show();
        }

        // Check if Notifications are disabled in the device
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_notifications)
                    .setMessage(R.string.dialog_message_notifications)
                    .setPositiveButton(R.string.dialog_button_notifications, (dialog, position) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startActivity(new Intent(ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(EXTRA_APP_PACKAGE, getPackageName())
                                    .addFlags(FLAG_ACTIVITY_NEW_TASK));
                        } else {
                            startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", getPackageName(), null))
                                    .addFlags(FLAG_ACTIVITY_NEW_TASK));
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

        // Check if Full Screen Notification is disabled in the device (for Android 14+ only)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!notificationManager.canUseFullScreenIntent()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_full_screen_intent)
                        .setMessage(R.string.dialog_message_full_screen_intent)
                        .setPositiveButton(R.string.dialog_button_full_screen_intent, (dialog, position) ->
                                startActivity(new Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                                        .setData(Uri.fromParts("package", getPackageName(), null))
                                        .addFlags(FLAG_ACTIVITY_NEW_TASK)))
                        .setCancelable(false)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_FOR_POWER_OFF_ALARM) {
            LogUtils.i("Power off alarm permission is granted.");
        }
    }

    private void showTabFromNotifications() {
        final Intent intent = getIntent();
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
                switch (action) {
                    case TimerService.ACTION_SHOW_TIMER -> {
                        Events.sendTimerEvent(R.string.action_show, label);
                        UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.TIMERS);
                    }
                    case StopwatchService.ACTION_SHOW_STOPWATCH -> {
                        Events.sendStopwatchEvent(R.string.action_show, label);
                        UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.STOPWATCH);
                    }
                    case BedtimeService.ACTION_SHOW_BEDTIME -> {
                        Events.sendBedtimeEvent(R.string.action_show, label);
                        UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.BEDTIME);
                    }
                }
            }
        }
    }

    /**
     * Configure the {@link #mBottomNavigation} to display UiDataModel's selected tab.
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

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(selectedTab.getLabelResId());
        }
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
        return Snackbar.make(mSnackbarAnchor, messageId, 5000);
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
        public void onSilentSettingsChange(SilentSetting after) {
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
        public void selectedTabChanged(UiDataModel.Tab newSelectedTab) {
            // Update the view pager and tab layout to agree with the model.
            updateCurrentTab();

            // Avoid sending events for the initial tab selection on launch and re-selecting a tab
            // after a configuration change.
            if (DataModel.getDataModel().isApplicationInForeground()) {
                switch (newSelectedTab) {
                    case ALARMS -> Events.sendAlarmEvent(R.string.action_show, R.string.label_deskclock);
                    case CLOCKS -> Events.sendClockEvent(R.string.action_show, R.string.label_deskclock);
                    case TIMERS -> Events.sendTimerEvent(R.string.action_show, R.string.label_deskclock);
                    case STOPWATCH -> Events.sendStopwatchEvent(R.string.action_show, R.string.label_deskclock);
                    case BEDTIME -> Events.sendBedtimeEvent(R.string.action_show, R.string.label_deskclock);
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

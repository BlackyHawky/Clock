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

package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.ViewPagerOnTabSelectedListener;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NightModeMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.actionbarmenu.SettingsMenuItemController;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.uidata.TabListener;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.uidata.UiDataModel.Tab;
import com.android.deskclock.widget.RtlViewPager;
import com.android.deskclock.widget.toast.SnackbarManager;

import static android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_ALARM;
import static android.media.RingtoneManager.TYPE_ALARM;
import static android.provider.Settings.System.CONTENT_URI;
import static android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
import static android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING;
import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;
import static android.support.v4.view.ViewPager.SCROLL_STATE_SETTLING;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.deskclock.AnimatorUtils.getAlphaAnimator;
import static com.android.deskclock.AnimatorUtils.getScaleAnimator;
import static com.android.deskclock.FabContainer.UpdateType.FAB_AND_BUTTONS_IMMEDIATE;

/**
 * The main activity of the application which displays 4 different tabs contains alarms, world
 * clocks, timers and a stopwatch.
 */
public class DeskClock extends BaseActivity
        implements FabContainer, LabelDialogFragment.AlarmLabelDialogHandler {

    /** The Uri to the settings entry that stores alarm stream volume. */
    private static final Uri VOLUME_URI = Uri.withAppendedPath(CONTENT_URI, "volume_alarm_speaker");

    /** The intent filter that identifies do-not-disturb change broadcasts. */
    @SuppressLint("NewApi")
    private static final IntentFilter DND_CHANGE_FILTER
            = new IntentFilter(ACTION_INTERRUPTION_FILTER_CHANGED);

    /** Models the interesting state of display the {@link #mFab} button may inhabit. */
    private enum FabState { SHOWING, HIDE_ARMED, HIDING }

    /** Coordinates handling of context menu items. */
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();

    /** Shrinks the {@link #mFab}, {@link #mLeftButton} and {@link #mRightButton} to nothing. */
    private final AnimatorSet mHideAnimation = new AnimatorSet();

    /** Grows the {@link #mFab}, {@link #mLeftButton} and {@link #mRightButton} to natural sizes. */
    private final AnimatorSet mShowAnimation = new AnimatorSet();

    /** Hides, updates, and shows only the {@link #mFab}; the buttons are untouched. */
    private final AnimatorSet mUpdateFabOnlyAnimation = new AnimatorSet();

    /** Automatically starts the {@link #mShowAnimation} after {@link #mHideAnimation} ends. */
    private final AnimatorListenerAdapter mAutoStartShowListener = new AutoStartShowListener();

    /** Updates the user interface to reflect the selected tab from the backing model. */
    private final TabListener mTabChangeWatcher = new TabChangeWatcher();

    /** Displays a snackbar explaining that the system default alarm ringtone is silent. */
    private final Runnable mShowSilentAlarmSnackbarRunnable = new ShowSilentAlarmSnackbarRunnable();

    /** Observes default alarm ringtone changes while the app is in the foreground. */
    private final ContentObserver mAlarmRingtoneChangeObserver = new AlarmRingtoneChangeObserver();

    /** Displays a snackbar explaining that the alarm volume is muted. */
    private final Runnable mShowMutedVolumeSnackbarRunnable = new ShowMutedVolumeSnackbarRunnable();

    /** Observes alarm volume changes while the app is in the foreground. */
    private final ContentObserver mAlarmVolumeChangeObserver = new AlarmVolumeChangeObserver();

    /** Displays a snackbar explaining that do-not-disturb is blocking alarms. */
    private final Runnable mShowDNDBlockingSnackbarRunnable = new ShowDNDBlockingSnackbarRunnable();

    /** Observes do-not-disturb changes while the app is in the foreground. */
    private final BroadcastReceiver mDoNotDisturbChangeReceiver = new DoNotDisturbChangeReceiver();

    /** Used to query the alarm volume and display the system control to change the alarm volume. */
    private AudioManager mAudioManager;

    /** Used to query the do-not-disturb setting value, also called "interruption filter". */
    private NotificationManager mNotificationManager;

    /** {@code true} permits the muted alarm volume snackbar to show when starting this activity. */
    private boolean mShowSilencedAlarmsSnackbar;

    /** The view to which snackbar items are anchored. */
    private View mSnackbarAnchor;

    /** The current display state of the {@link #mFab}. */
    private FabState mFabState = FabState.SHOWING;

    /** The single floating-action button shared across all tabs in the user interface. */
    private ImageView mFab;

    /** The button left of the {@link #mFab} shared across all tabs in the user interface. */
    private ImageButton mLeftButton;

    /** The button right of the {@link #mFab} shared across all tabs in the user interface. */
    private ImageButton mRightButton;

    /** The controller that shows the drop shadow when content is not scrolled to the top. */
    private DropShadowController mDropShadowController;

    /** The ViewPager that pages through the fragments representing the content of the tabs. */
    private RtlViewPager mFragmentTabPager;

    /** Generates the fragments that are displayed by the {@link #mFragmentTabPager}. */
    private TabFragmentAdapter mFragmentTabPagerAdapter;

    /** The container that stores the tab headers. */
    private TabLayout mTabLayout;

    /** {@code true} when a settings change necessitates recreating this activity. */
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

        setContentView(R.layout.desk_clock);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Don't show the volume muted snackbar on rotations.
        mShowSilencedAlarmsSnackbar = savedInstanceState == null;
        mSnackbarAnchor = findViewById(R.id.coordinator);

        // Configure the toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Configure the menu item controllers add behavior to the toolbar.
        mOptionsMenuManager
                .addMenuItemController(new NightModeMenuItemController(this))
                .addMenuItemController(new SettingsMenuItemController(this))
                .addMenuItemController(MenuItemControllerFactory.getInstance()
                        .buildMenuItemControllers(this));

        // Inflate the menu during creation to avoid a double layout pass. Otherwise, the menu
        // inflation occurs *after* the initial draw and a second layout pass adds in the menu.
        onCreateOptionsMenu(toolbar.getMenu());

        // Create the tabs that make up the user interface.
        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        for (int i = 0; i < UiDataModel.getUiDataModel().getTabCount(); i++) {
            final Tab tab = UiDataModel.getUiDataModel().getTab(i);
            mTabLayout.addTab(mTabLayout.newTab()
                    .setIcon(tab.getIconId())
                    .setContentDescription(tab.getContentDescriptionId()));
        }

        // Configure the buttons shared by the tabs.
        mFab = (ImageView) findViewById(R.id.fab);
        mLeftButton = (ImageButton) findViewById(R.id.left_button);
        mRightButton = (ImageButton) findViewById(R.id.right_button);

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedDeskClockFragment().onFabClick(mFab);
            }
        });
        mLeftButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedDeskClockFragment().onLeftButtonClick(mLeftButton);
            }
        });
        mRightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedDeskClockFragment().onRightButtonClick(mRightButton);
            }
        });

        // Build the reusable animations that hide and show the fab and left/right buttons.
        // These may be used independently or be chained together.
        final long duration = UiDataModel.getUiDataModel().getShortAnimationDuration();
        mHideAnimation
                .setDuration(duration)
                .play(getScaleAnimator(mFab, 1f, 0f))
                .with(getAlphaAnimator(mLeftButton, 1f, 0f))
                .with(getAlphaAnimator(mRightButton, 1f, 0f));

        mShowAnimation
                .setDuration(duration)
                .play(getScaleAnimator(mFab, 0f, 1f))
                .with(getAlphaAnimator(mLeftButton, 0f, 1f))
                .with(getAlphaAnimator(mRightButton, 0f, 1f));

        // Build the reusable animation that hides and shows only the fab.
        final ValueAnimator hideFabAnimation = getScaleAnimator(mFab, 1f, 0f);
        hideFabAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                getSelectedDeskClockFragment().onUpdateFab(mFab);
            }
        });
        final ValueAnimator showFabAnimation = getScaleAnimator(mFab, 0f, 1f);
        mUpdateFabOnlyAnimation
                .setDuration(duration)
                .play(showFabAnimation)
                .after(hideFabAnimation);

        // Customize the view pager.
        mFragmentTabPagerAdapter = new TabFragmentAdapter(this);
        mFragmentTabPager = (RtlViewPager) findViewById(R.id.desk_clock_pager);
        // Keep all four tabs to minimize jank.
        mFragmentTabPager.setOffscreenPageLimit(3);
        // Set Accessibility Delegate to null so view pager doesn't intercept movements and
        // prevent the fab from being selected.
        mFragmentTabPager.setAccessibilityDelegate(null);
        // Mirror changes made to the selected page of the view pager into UiDataModel.
        mFragmentTabPager.setOnRTLPageChangeListener(new PageChangeWatcher());
        mFragmentTabPager.setAdapter(mFragmentTabPagerAdapter);

        // Selecting a tab implicitly selects a page in the view pager.
        mTabLayout.setOnTabSelectedListener(new ViewPagerOnTabSelectedListener(mFragmentTabPager));

        // Honor changes to the selected tab from outside entities.
        UiDataModel.getUiDataModel().addTabListener(mTabChangeWatcher);

        // Update the next alarm time on app startup because the user might have altered the data.
        AlarmStateManager.updateNextAlarm(this);

        if (savedInstanceState == null) {
            // Set the background color to initially match the theme value so that we can
            // smoothly transition to the dynamic color.
            final int backgroundColor = ContextCompat.getColor(this, R.color.default_background);
            adjustAppColor(backgroundColor, false /* animate */);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mShowSilencedAlarmsSnackbar) {
            if (isDoNotDisturbBlockingAlarms()) {
                mSnackbarAnchor.postDelayed(mShowDNDBlockingSnackbarRunnable, SECOND_IN_MILLIS);
            } else if (isAlarmStreamMuted()) {
                mSnackbarAnchor.postDelayed(mShowMutedVolumeSnackbarRunnable, SECOND_IN_MILLIS);
            } else if (isSystemAlarmRingtoneSilent()) {
                mSnackbarAnchor.postDelayed(mShowSilentAlarmSnackbarRunnable, SECOND_IN_MILLIS);
            }
        }

        // Subsequent starts of this activity should show the snackbar by default.
        mShowSilencedAlarmsSnackbar = true;

        final ContentResolver cr = getContentResolver();
        // Watch for system alarm ringtone changes while the app is in the foreground.
        cr.registerContentObserver(DEFAULT_ALARM_ALERT_URI, false, mAlarmRingtoneChangeObserver);

        // Watch for alarm volume changes while the app is in the foreground.
        cr.registerContentObserver(VOLUME_URI, false, mAlarmVolumeChangeObserver);

        if (Utils.isMOrLater()) {
            // Watch for do-not-disturb changes while the app is in the foreground.
            registerReceiver(mDoNotDisturbChangeReceiver, DND_CHANGE_FILTER);
        }

        DataModel.getDataModel().setApplicationInForeground(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final View dropShadow = findViewById(R.id.drop_shadow);
        mDropShadowController = new DropShadowController(dropShadow, UiDataModel.getUiDataModel());

        // Honor the selected tab in case it changed while the app was paused.
        updateCurrentTab(UiDataModel.getUiDataModel().getSelectedTabIndex());
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mRecreateActivity) {
            mRecreateActivity = false;

            // A runnable must be posted here or the new DeskClock activity will be recreated in a
            // paused state, even though it is the foreground activity.
            mFragmentTabPager.post(new Runnable() {
                @Override
                public void run() {
                    recreate();
                }
            });
        }
    }

    @Override
    public void onPause() {
        mDropShadowController.stop();
        mDropShadowController = null;

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations()) {
            DataModel.getDataModel().setApplicationInForeground(false);
        }

        // Stop watching for system alarm ringtone changes while the app is in the background.
        getContentResolver().unregisterContentObserver(mAlarmRingtoneChangeObserver);

        // Stop watching for alarm volume changes while the app is in the background.
        getContentResolver().unregisterContentObserver(mAlarmVolumeChangeObserver);

        if (Utils.isMOrLater()) {
            // Stop watching for do-not-disturb changes while the app is in the background.
            unregisterReceiver(mDoNotDisturbChangeReceiver);
        }

        // Remove any scheduled work to show snackbars; it is no longer relevant.
        mSnackbarAnchor.removeCallbacks(mShowSilentAlarmSnackbarRunnable);
        mSnackbarAnchor.removeCallbacks(mShowDNDBlockingSnackbarRunnable);
        mSnackbarAnchor.removeCallbacks(mShowMutedVolumeSnackbarRunnable);
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
        if (getSelectedDeskClockFragment().onKeyDown(keyCode,event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * @param color the newly installed window background color
     */
    @Override
    protected void onAppColorChanged(@ColorInt int color) {
        super.onAppColorChanged(color);

        // Notify each fragment of the background color change.
        for (int i = 0; i < mFragmentTabPagerAdapter.getCount(); i++) {
            mFragmentTabPagerAdapter.getItem(i).onAppColorChanged(color);
        }
    }

    @Override
    public void updateFab(UpdateType updateType) {
        switch (updateType) {
            case DISABLE_BUTTONS: {
                mLeftButton.setEnabled(false);
                mRightButton.setEnabled(false);
                break;
            }
            case FAB_AND_BUTTONS_IMMEDIATE: {
                final DeskClockFragment f = getSelectedDeskClockFragment();
                f.onUpdateFab(mFab);
                f.onUpdateFabButtons(mLeftButton, mRightButton);
                break;
            }
            case FAB_AND_BUTTONS_MORPH: {
                final DeskClockFragment f = getSelectedDeskClockFragment();
                f.onUpdateFab(mFab);
                f.onMorphFabButtons(mLeftButton, mRightButton);
                break;
            }
            case FAB_ONLY_SHRINK_AND_EXPAND: {
                mUpdateFabOnlyAnimation.start();
                break;
            }
            case FAB_AND_BUTTONS_SHRINK_AND_EXPAND: {
                // Ensure there is never more than one mAutoStartShowListener registered.
                mHideAnimation.removeListener(mAutoStartShowListener);
                mHideAnimation.addListener(mAutoStartShowListener);
                mHideAnimation.start();
                break;
            }
            case FAB_REQUESTS_FOCUS: {
                mFab.requestFocus();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Recreate the activity if any settings have been changed
        if (requestCode == SettingsMenuItemController.REQUEST_CHANGE_SETTINGS
                && resultCode == RESULT_OK) {
            mRecreateActivity = true;
        }
    }

    /**
     * Configure the {@link #mFragmentTabPager} and {@link #mTabLayout} to display the tab at the
     * given {@code index}.
     *
     * @param index the index of the page to display
     */
    private void updateCurrentTab(int index) {
        final TabLayout.Tab tab = mTabLayout.getTabAt(index);
        if (tab != null && !tab.isSelected()) {
            tab.select();
        }
        if (mFragmentTabPager.getCurrentItem() != index) {
            mFragmentTabPager.setCurrentItem(index);
        }
    }

    private DeskClockFragment getSelectedDeskClockFragment() {
        final int index = UiDataModel.getUiDataModel().getSelectedTabIndex();
        return mFragmentTabPagerAdapter.getItem(index);
    }

    private boolean isSystemAlarmRingtoneSilent() {
        return RingtoneManager.getActualDefaultRingtoneUri(this, TYPE_ALARM) == null;
    }

    private void showSilentRingtoneSnackbar() {
        final OnClickListener changeClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        };

        SnackbarManager.show(
                createSnackbar(R.string.silent_default_alarm_ringtone)
                        .setAction(R.string.change_default_alarm_ringtone, changeClickListener)
        );
    }

    private boolean isAlarmStreamMuted() {
        return mAudioManager.getStreamVolume(STREAM_ALARM) <= 0;
    }

    private void showAlarmVolumeMutedSnackbar() {
        final OnClickListener unmuteClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the alarm volume to ~30% of max and show the slider UI.
                final int index = mAudioManager.getStreamMaxVolume(STREAM_ALARM) / 3;
                mAudioManager.setStreamVolume(STREAM_ALARM, index, FLAG_SHOW_UI);
            }
        };

        SnackbarManager.show(
                createSnackbar(R.string.alarm_volume_muted)
                        .setAction(R.string.unmute_alarm_volume, unmuteClickListener)
        );
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isDoNotDisturbBlockingAlarms() {
        if (!Utils.isMOrLater()) {
            return false;
        }
        return mNotificationManager.getCurrentInterruptionFilter() == INTERRUPTION_FILTER_NONE;
    }

    private void showDoNotDisturbIsBlockingAlarmsSnackbar() {
        SnackbarManager.show(createSnackbar(R.string.alarms_blocked_by_dnd));
    }

    /**
     * @return a Snackbar that displays the message with the given id for 5 seconds
     */
    private Snackbar createSnackbar(@StringRes int messageId) {
        return Snackbar.make(mSnackbarAnchor, messageId, 5000 /* duration */);
    }

    /**
     * As the view pager changes the selected page, update the model to record the new selected tab.
     */
    private final class PageChangeWatcher implements OnPageChangeListener {

        /** The last reported page scroll state; used to detect exotic state changes. */
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
            UiDataModel.getUiDataModel().setSelectedTabIndex(position);
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
     * Displays a snackbar that indicates the system default alarm ringtone currently silent and
     * offers an action that displays the system alarm ringtone setting to adjust it.
     */
    private final class ShowSilentAlarmSnackbarRunnable implements Runnable {
        @Override
        public void run() {
            showSilentRingtoneSnackbar();
        }
    }

    /**
     * Displays a snackbar that indicates the alarm volume is currently muted and offers an action
     * that displays the system volume control to adjust it.
     */
    private final class ShowMutedVolumeSnackbarRunnable implements Runnable {
        @Override
        public void run() {
            showAlarmVolumeMutedSnackbar();
        }
    }

    /**
     * Displays a snackbar that indicates the do-not-disturb setting is currently blocking alarms.
     */
    private final class ShowDNDBlockingSnackbarRunnable implements Runnable {
        @Override
        public void run() {
            showDoNotDisturbIsBlockingAlarmsSnackbar();
        }
    }

    /**
     * Observe changes to the system default alarm ringtone while the application is in the
     * foreground and show/hide the snackbar that warns when the ringtone is silent.
     */
    private final class AlarmRingtoneChangeObserver extends ContentObserver {
        private AlarmRingtoneChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (isSystemAlarmRingtoneSilent()) {
                showSilentRingtoneSnackbar();
            } else {
                SnackbarManager.dismiss();
            }
        }
    }

    /**
     * Observe changes to the alarm stream volume while the application is in the foreground and
     * show/hide the snackbar that warns when the alarm volume is muted.
     */
    private final class AlarmVolumeChangeObserver extends ContentObserver {
        private AlarmVolumeChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (isAlarmStreamMuted()) {
                showAlarmVolumeMutedSnackbar();
            } else {
                SnackbarManager.dismiss();
            }
        }
    }

    /**
     * Observe changes to the do-not-disturb setting while the application is in the foreground
     * and show/hide the snackbar that warns when the setting is blocking alarms.
     */
    private final class DoNotDisturbChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isDoNotDisturbBlockingAlarms()) {
                showDoNotDisturbIsBlockingAlarmsSnackbar();
            } else {
                SnackbarManager.dismiss();
            }
        }
    }

    /**
     * As the model reports changes to the selected tab, update the user interface.
     */
    private final class TabChangeWatcher implements TabListener {
        @Override
        public void selectedTabChanged(Tab oldSelectedTab, Tab newSelectedTab) {
            final int index = newSelectedTab.ordinal();

            // Update the view pager and tab layout to agree with the model.
            updateCurrentTab(index);

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

    /**
     * This adapter produces the DeskClockFragments that are the contents of the tabs.
     */
    private static final class TabFragmentAdapter extends FragmentPagerAdapter {

        private final FragmentManager mFragmentManager;
        private final Context mContext;

        public TabFragmentAdapter(AppCompatActivity activity) {
            super(activity.getFragmentManager());
            mContext = activity;
            mFragmentManager = activity.getFragmentManager();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            position = UiDataModel.getUiDataModel().getTabLayoutIndex(position);
            return super.instantiateItem(container, position);
        }

        @Override
        public DeskClockFragment getItem(int position) {
            final String tag = makeFragmentName(R.id.desk_clock_pager, position);
            Fragment fragment = mFragmentManager.findFragmentByTag(tag);
            if (fragment == null) {
                final Tab tab = UiDataModel.getUiDataModel().getTab(position);
                final String fragmentClassName = tab.getFragmentClassName();
                fragment = Fragment.instantiate(mContext, fragmentClassName);
            }
            return (DeskClockFragment) fragment;
        }

        @Override
        public int getCount() {
            return UiDataModel.getUiDataModel().getTabCount();
        }

        /** This implementation duplicated from {@link FragmentPagerAdapter#makeFragmentName}. */
        private String makeFragmentName(int viewId, long id) {
            return "android:switcher:" + viewId + ":" + id;
        }
    }
}
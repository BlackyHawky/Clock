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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NightModeMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.actionbarmenu.SettingsMenuItemController;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.DataModel.SilentSetting;
import com.android.deskclock.data.OnSilentSettingsListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.LogUtils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.uidata.TabListener;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.widget.toast.SnackbarManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_DRAGGING;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_SETTLING;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.deskclock.AnimatorUtils.getScaleAnimator;

/**
 * The main activity of the application which displays 4 different tabs contains alarms, world
 * clocks, timers and a stopwatch.
 */
public class DeskClock extends BaseActivity
        implements FabContainer, LabelDialogFragment.AlarmLabelDialogHandler {

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

    /** Hides, updates, and shows only the {@link #mLeftButton} and {@link #mRightButton}. */
    private final AnimatorSet mUpdateButtonsOnlyAnimation = new AnimatorSet();

    /** Updates the user interface to reflect the selected tab from the backing model. */
    private final TabListener mTabChangeWatcher = new TabChangeWatcher();

    /** Shows/hides a snackbar explaining which setting is suppressing alarms from firing. */
    private final OnSilentSettingsListener mSilentSettingChangeWatcher =
            new SilentSettingChangeWatcher();

    /** Displays a snackbar explaining why alarms may not fire or may fire silently. */
    private Runnable mShowSilentSettingSnackbarRunnable;

    /** The view to which snackbar items are anchored. */
    private View mSnackbarAnchor;

    /** The current display state of the {@link #mFab}. */
    private FabState mFabState = FabState.SHOWING;

    /** The single floating-action button shared across all tabs in the user interface. */
    private ImageView mFab;

    /** The button left of the {@link #mFab} shared across all tabs in the user interface. */
    private Button mLeftButton;

    /** The button right of the {@link #mFab} shared across all tabs in the user interface. */
    private Button mRightButton;

    /** The ViewPager that pages through the fragments representing the content of the tabs. */
    private ViewPager mFragmentTabPager;

    /** The view that displays the current tab's title */
    private TextView mTitleView;

    /** The bottom navigation bar */
    private BottomNavigationView mBottomNavigation;

    private FragmentUtils mFragmentUtils;

    /** {@code true} when a settings change necessitates recreating this activity. */
    private boolean mRecreateActivity;

    private static final String PERMISSION_POWER_OFF_ALARM =
            "org.codeaurora.permission.POWER_OFF_ALARM";

    private static final int CODE_FOR_ALARM_PERMISSION = 1;

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
        mSnackbarAnchor = findViewById(R.id.content);

        checkPermissions();

        // Configure the toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
        mFab = (ImageView) findViewById(R.id.fab);
        mLeftButton = (Button) findViewById(R.id.left_button);
        mRightButton = (Button) findViewById(R.id.right_button);

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

        mFragmentUtils = new FragmentUtils(this);
        // Mirror changes made to the selected tab into UiDataModel.
        mBottomNavigation = findViewById(R.id.bottom_view);
        mBottomNavigation.setOnNavigationItemSelectedListener(mNavigationListener);

        // Honor changes to the selected tab from outside entities.
        UiDataModel.getUiDataModel().addTabListener(mTabChangeWatcher);

        mTitleView = findViewById(R.id.title_view);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mNavigationListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            UiDataModel.Tab selectedTab = null;
            switch (item.getItemId()) {
                case R.id.page_alarm:
                    selectedTab = UiDataModel.Tab.ALARMS;
                    break;

                case R.id.page_clock:
                    selectedTab = UiDataModel.Tab.CLOCKS;
                    break;

                case R.id.page_timer:
                    selectedTab = UiDataModel.Tab.TIMERS;
                    break;

                case R.id.page_stopwatch:
                    selectedTab = UiDataModel.Tab.STOPWATCH;
                    break;
            }

            if (selectedTab != null) {
                UiDataModel.Tab currentTab = UiDataModel.getUiDataModel().getSelectedTab();
                DeskClockFragment currentFrag = mFragmentUtils.getDeskClockFragment(currentTab);
                DeskClockFragment selectedFrag = mFragmentUtils.getDeskClockFragment(selectedTab);

                int currentVisibility = currentFrag.getFabTargetVisibility();
                int targetVisibility = selectedFrag.getFabTargetVisibility();
                if (currentVisibility != targetVisibility) {
                    if (targetVisibility == View.VISIBLE) {
                        mShowAnimation.start();
                    } else {
                        mHideAnimation.start();
                    }
                }
                UiDataModel.getUiDataModel().setSelectedTab(selectedTab);
                return true;
            }

            return false;
        }
    };

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
        final Fragment frag = getSupportFragmentManager().findFragmentByTag(tag);
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
        return getSelectedDeskClockFragment().onKeyDown(keyCode,event)
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
        switch (updateType & FAB_REQUEST_FOCUS_MASK) {
            case FAB_REQUEST_FOCUS:
                mFab.requestFocus();
                break;
        }
        switch (updateType & BUTTONS_ANIMATION_MASK) {
            case BUTTONS_IMMEDIATE:
                f.onUpdateFabButtons(mLeftButton, mRightButton);
                break;
            case BUTTONS_SHRINK_AND_EXPAND:
                mUpdateButtonsOnlyAnimation.start();
                break;
        }
        switch (updateType & BUTTONS_DISABLE_MASK) {
            case BUTTONS_DISABLE:
                mLeftButton.setClickable(false);
                mRightButton.setClickable(false);
                break;
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
                                           String permissions[], int[] grantResults) {
        if (requestCode == CODE_FOR_ALARM_PERMISSION){
            LogUtils.i("Power off alarm permission is granted.");
        }
    }

    /**
     * Configure the {@link #mFragmentTabPager} and {@link #mBottomNavigation} to display
     * UiDataModel's selected tab.
     */
    private void updateCurrentTab() {
        // Fetch the selected tab from the source of truth: UiDataModel.
        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        // Update the selected tab in the mBottomNavigation if it does not agree with UiDataModel.
        mBottomNavigation.setSelectedItemId(selectedTab.getPageResId());
        mFragmentUtils.showFragment(selectedTab);
        mTitleView.setText(selectedTab.getLabelResId());
    }

    /**
     * @return the DeskClockFragment that is currently selected according to UiDataModel
     */
    private DeskClockFragment getSelectedDeskClockFragment() {
        return mFragmentUtils.getCurrentFragment();
    }

    /**
     * @return a Snackbar that displays the message with the given id for 5 seconds
     */
    private Snackbar createSnackbar(@StringRes int messageId) {
        return Snackbar.make(mSnackbarAnchor, messageId, 5000 /* duration */);
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
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.ViewPagerOnTabSelectedListener;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.deskclock.actionbarmenu.ActionBarMenuManager;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NightModeMenuItemController;
import com.android.deskclock.actionbarmenu.SettingMenuItemController;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.uidata.TabListener;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.uidata.UiDataModel.Tab;
import com.android.deskclock.widget.RtlViewPager;

/**
 * The main activity of the application which displays 4 different tabs contains alarms, world
 * clocks, timers and a stopwatch.
 */
public class DeskClock extends BaseActivity
        implements LabelDialogFragment.AlarmLabelDialogHandler,
        RingtonePickerDialogFragment.RingtoneSelectionListener {

    /** Coordinates handling of context menu items. */
    private final ActionBarMenuManager mActionBarMenuManager = new ActionBarMenuManager(this);

    /** The single floating-action button shared across all tabs in the user interface. */
    private ImageView mFab;

    /** The button left of the {@link #mFab} shared across all tabs in the user interface. */
    private ImageButton mLeftButton;

    /** The button right of the {@link #mFab} shared across all tabs in the user interface. */
    private ImageButton mRightButton;

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

        if (savedInstanceState == null) {
            // Set the background color to initially match the theme value so that we can
            // smoothly transition to the dynamic color.
            final int backgroundColor = getResources().getColor(R.color.default_background);
            setBackgroundColor(backgroundColor, false /* animate */);
        }

        setContentView(R.layout.desk_clock);

        // Configure the toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure the menu item controllers add behavior to the toolbar.
        mActionBarMenuManager
                .addMenuItemController(new SettingMenuItemController(this))
                .addMenuItemController(new NightModeMenuItemController(this))
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
                mFragmentTabPagerAdapter.getSelectedDeskClockFragment().onFabClick(view);
            }
        });
        mLeftButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mFragmentTabPagerAdapter.getSelectedDeskClockFragment().onLeftButtonClick(view);
            }
        });
        mRightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mFragmentTabPagerAdapter.getSelectedDeskClockFragment().onRightButtonClick(view);
            }
        });

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
        UiDataModel.getUiDataModel().addTabListener(new TabChangeWatcher());

        // Update the next alarm time on app startup because the user might have altered the data.
        AlarmStateManager.updateNextAlarm(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataModel.getDataModel().setApplicationInForeground(true);

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
        DataModel.getDataModel().setApplicationInForeground(false);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mActionBarMenuManager.createOptionsMenu(menu, getMenuInflater());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mActionBarMenuManager.prepareShowMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mActionBarMenuManager.handleMenuItemClick(item) || super.onOptionsItemSelected(item);
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
     * Called by the RingtonePickerDialogFragment class after the dialog is finished.
     */
    @Override
    public void onRingtoneSelected(Uri ringtoneUri, String fragmentTag) {
        final Fragment frag = getFragmentManager().findFragmentByTag(fragmentTag);
        if (frag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) frag).setRingtone(ringtoneUri);
        }
    }

    public ImageView getFab() { return mFab; }
    public ImageButton getLeftButton() { return mLeftButton; }
    public ImageButton getRightButton() { return mRightButton; }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Recreate the activity if any settings have been changed
        if (requestCode == SettingMenuItemController.REQUEST_CHANGE_SETTINGS
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

    /**
     * As the view pager changes the selected page, update the model to record the new selected tab.
     */
    private static class PageChangeWatcher implements OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageScrollStateChanged(int state) {}

        @Override
        public void onPageSelected(int position) {
            UiDataModel.getUiDataModel().setSelectedTabIndex(position);
        }
    }

    /**
     * As the model reports changes to the selected tab, update the user interface.
     */
    private class TabChangeWatcher implements TabListener {
        @Override
        public void selectedTabChanged(Tab oldSelectedTab, Tab newSelectedTab) {
            final int index = newSelectedTab.ordinal();

            // Update the view pager and tab layout to agree with the model.
            updateCurrentTab(index);

            // Avoid sending events for the initial tab selection on launch and reselecting a tab
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

            // Update the shared buttons to reflect the new tab.
            final DeskClockFragment f = (DeskClockFragment) mFragmentTabPagerAdapter.getItem(index);
            if (f != null) {
                f.setFabAppearance();
                f.setLeftRightButtonAppearance();
            }
        }
    }

    /**
     * This adapter produces the DeskClockFragments that are the contents of the tabs.
     */
    private static class TabFragmentAdapter extends FragmentPagerAdapter {

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
        public Fragment getItem(int position) {
            final String tag = makeFragmentName(R.id.desk_clock_pager, position);
            Fragment fragment = mFragmentManager.findFragmentByTag(tag);
            if (fragment == null) {
                final Tab tab = UiDataModel.getUiDataModel().getTab(position);
                final String fragmentClassName = tab.getFragmentClassName();
                fragment = Fragment.instantiate(mContext, fragmentClassName);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return UiDataModel.getUiDataModel().getTabCount();
        }

        /** This implementation duplicated from {@link FragmentPagerAdapter#makeFragmentName}. */
        private String makeFragmentName(int viewId, long id) {
            return "android:switcher:" + viewId + ":" + id;
        }

        private DeskClockFragment getSelectedDeskClockFragment() {
            final int index = UiDataModel.getUiDataModel().getSelectedTabIndex();
            return (DeskClockFragment) getItem(index);
        }
    }
}
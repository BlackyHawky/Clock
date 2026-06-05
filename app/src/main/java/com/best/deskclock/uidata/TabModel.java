/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uidata;

import static android.view.View.LAYOUT_DIRECTION_RTL;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TAB_TO_DISPLAY_INTEGER;
import static com.best.deskclock.uidata.UiDataModel.Tab;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.data.Timer;
import com.best.deskclock.provider.Alarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * All tab data is accessed via this model.
 */
final class TabModel {

    private final Context mContext;

    private final SharedPreferences mPrefs;

    /**
     * The listeners to notify when the selected tab is changed.
     */
    private final List<TabListener> mTabListeners = new ArrayList<>();

    /**
     * An enumerated value indicating the currently selected tab.
     */
    private Tab mSelectedTab;

    private final List<Tab> mActiveTabs = new ArrayList<>();

    TabModel(Context context) {
        mContext = context;
        mPrefs = getDefaultSharedPreferences(context);
        updateActiveTabs();
    }

    /**
     * Updates the list of visible tabs based on {@link SharedPreferences}.
     *
     * <p>If the currently selected tab becomes hidden, this method automatically selects the first
     * available tab. It also performs necessary background cleanups for newly hidden tabs,
     * such as disabling active alarms, resetting timers, and resetting the stopwatch.</p>
     *
     * @return {@code true} if the visibility of the tabs has changed, {@code false} otherwise.
     */
    boolean updateActiveTabs() {
        List<Tab> newActiveTabs = new ArrayList<>();

        final boolean isAlarmTabVisible = SettingsDAO.isAlarmTabVisible(mPrefs);
        final boolean isClockTabVisible = SettingsDAO.isClockTabVisible(mPrefs);
        final boolean isTimerTabVisible = SettingsDAO.isTimerTabVisible(mPrefs);
        final boolean isStopwatchTabVisible = SettingsDAO.isStopwatchTabVisible(mPrefs);

        if (isAlarmTabVisible) {
            newActiveTabs.add(Tab.ALARMS);
        }

        if (isClockTabVisible) {
            newActiveTabs.add(Tab.CLOCKS);
        }

        if (isTimerTabVisible) {
            newActiveTabs.add(Tab.TIMERS);
        }

        if (isStopwatchTabVisible) {
            newActiveTabs.add(Tab.STOPWATCH);
        }

        // This shouldn't happen because it's impossible to uncheck all the entries in the "Visible tabs" setting
        if (newActiveTabs.isEmpty()) {
            newActiveTabs.add(Tab.ALARMS);
        }

        if (mActiveTabs.equals(newActiveTabs)) {
            return false;
        }

        List<Tab> recentlyHiddenTabs = new ArrayList<>(mActiveTabs);
        recentlyHiddenTabs.removeAll(newActiveTabs);

        mActiveTabs.clear();
        mActiveTabs.addAll(newActiveTabs);

        // If the currently selected tab has just been hidden, switch to the first available tab
        if (mSelectedTab != null && !mActiveTabs.contains(mSelectedTab)) {
            setSelectedTab(mActiveTabs.get(0));
        }

        // Disable alarms if the Alarm tab is not visible
        if (recentlyHiddenTabs.contains(Tab.ALARMS)) {
            AppExecutors.getDiskIO().execute(() -> {
                final AlarmUpdateHandler alarmUpdateHandler = new AlarmUpdateHandler(mContext, null, null);
                final List<Alarm> alarms = Alarm.getAlarms(mContext.getContentResolver(), null);

                for (Alarm alarm : alarms) {
                    if (alarm.enabled) {
                        alarm.enabled = false;

                        alarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
                    }
                }
            });
        }

        // Reset running timers if the Timer tab is not visible
        if (recentlyHiddenTabs.contains(Tab.TIMERS)) {
            for (Timer timer : new ArrayList<>(DataModel.getDataModel().getTimers())) {
                if (!timer.isReset()) {
                    DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_deskclock);
                }
            }
        }

        // Reset running stopwatch if the Stopwatch tab is not visible
        if (recentlyHiddenTabs.contains(Tab.STOPWATCH)) {
            final Stopwatch stopwatch = DataModel.getDataModel().getStopwatch();
            if (!stopwatch.isReset()) {
                DataModel.getDataModel().resetStopwatch();
            }
        }

        return true;
    }

    /**
     * @param tabListener to be notified when the selected tab changes
     */
    void addTabListener(TabListener tabListener) {
        mTabListeners.add(tabListener);
    }

    /**
     * @param tabListener to no longer be notified when the selected tab changes
     */
    void removeTabListener(TabListener tabListener) {
        mTabListeners.remove(tabListener);
    }

    /**
     * @return the number of tabs
     */
    int getTabCount() {
        return mActiveTabs.size();
    }

    /**
     * @param ordinal the ordinal (left-to-right index) of the tab
     * @return the tab at the given {@code ordinal}
     */
    Tab getTab(int ordinal) {
        return mActiveTabs.get(ordinal);
    }

    /**
     * @param position the position of the tab in the user interface
     * @return the tab at the given {@code ordinal}
     */
    Tab getTabAt(int position) {
        final int ordinal;
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == LAYOUT_DIRECTION_RTL) {
            ordinal = getTabCount() - position - 1;
        } else {
            ordinal = position;
        }
        return getTab(ordinal);
    }

    /**
     * @param tab the tab to find
     * @return the current dynamic index of the tab, or -1 if hidden
     */
    int getTabIndex(Tab tab) {
        return mActiveTabs.indexOf(tab);
    }

    /**
     * @return an enumerated value indicating the currently selected primary tab
     */
    Tab getSelectedTab() {
        if (mSelectedTab == null) {
            mSelectedTab = TabDAO.getSelectedTab(mPrefs);

            // At startup: make sure the saved tab is visible
            if (!mActiveTabs.contains(mSelectedTab)) {
                mSelectedTab = mActiveTabs.get(0);
            }
        }
        return mSelectedTab;
    }

    /**
     * @param tab an enumerated value indicating the newly selected primary tab
     */
    void setSelectedTab(Tab tab) {
        final Tab oldSelectedTab = getSelectedTab();
        if (oldSelectedTab != tab) {
            mSelectedTab = tab;
            int tabIndex = SettingsDAO.getTabToDisplay(mPrefs);
            if (tabIndex == DEFAULT_TAB_TO_DISPLAY_INTEGER) {
                TabDAO.setSelectedTab(mPrefs, tab);
            }

            // Notify of the tab change.
            for (TabListener tl : mTabListeners) {
                tl.selectedTabChanged(tab);
            }
        }
    }

}

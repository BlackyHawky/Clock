// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.best.deskclock.R;

public class StopwatchSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mRecyclerViewPosition = -1;

    public static final String KEY_SW_VOLUME_UP_ACTION = "key_sw_volume_up_action";
    public static final String KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS = "key_sw_volume_up_action_after_long_press";
    public static final String KEY_SW_VOLUME_DOWN_ACTION = "key_sw_volume_down_action";
    public static final String KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS = "key_sw_volume_down_action_after_long_press";
    public static final String KEY_SW_DEFAULT_ACTION = "0";
    public static final String KEY_SW_ACTION_START_PAUSE = "1";
    public static final String KEY_SW_ACTION_RESET = "2";
    public static final String KEY_SW_ACTION_LAP = "3";
    public static final String KEY_SW_ACTION_SHARE = "4";

    ListPreference mVolumeUpActionPref;
    ListPreference mVolumeUpActionAfterLongPressPref;
    ListPreference mVolumeDownActionPref;
    ListPreference mVolumeDownActionAfterLongPressPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.stopwatch_channel);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_stopwatch);

        mVolumeUpActionPref = findPreference(KEY_SW_VOLUME_UP_ACTION);
        mVolumeUpActionAfterLongPressPref = findPreference(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS);
        mVolumeDownActionPref = findPreference(KEY_SW_VOLUME_DOWN_ACTION);
        mVolumeDownActionAfterLongPressPref = findPreference(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mRecyclerViewPosition != -1) {
            mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
            mAppBarLayout.setExpanded(mRecyclerViewPosition == 0, true);
        }
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLinearLayoutManager != null) {
            mRecyclerViewPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_SW_VOLUME_UP_ACTION, KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS,
                 KEY_SW_VOLUME_DOWN_ACTION, KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            }
        }

        return true;
    }

    private void refresh() {
        mVolumeUpActionPref.setOnPreferenceChangeListener(this);
        mVolumeUpActionPref.setSummary(mVolumeUpActionPref.getEntry());

        mVolumeUpActionAfterLongPressPref.setOnPreferenceChangeListener(this);
        mVolumeUpActionAfterLongPressPref.setSummary(mVolumeUpActionAfterLongPressPref.getEntry());

        mVolumeDownActionPref.setOnPreferenceChangeListener(this);
        mVolumeDownActionPref.setSummary(mVolumeDownActionPref.getEntry());

        mVolumeDownActionAfterLongPressPref.setOnPreferenceChangeListener(this);
        mVolumeDownActionAfterLongPressPref.setSummary(mVolumeDownActionAfterLongPressPref.getEntry());
    }

}

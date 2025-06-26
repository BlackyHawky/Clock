// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.WidgetUtils;
import com.best.alarmclock.standardwidgets.NextAlarmAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.Utils;
import com.rarepebble.colorpicker.ColorPreference;

public class NextAlarmWidgetSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    ColorPreference mBackgroundColorPref;
    ColorPreference mCustomTitleColorPref;
    ColorPreference mCustomAlarmTitleColorPref;
    ColorPreference mCustomAlarmColorPref;
    SwitchPreferenceCompat mShowBackgroundOnNextAlarmWidgetPref;
    SwitchPreferenceCompat mDefaultTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmColorPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.next_alarm_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_next_alarm_widget);

        mShowBackgroundOnNextAlarmWidgetPref = findPreference(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND);
        mBackgroundColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR);
        mDefaultTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR);
        mCustomTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR);
        mDefaultAlarmTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR);
        mCustomAlarmTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR);
        mDefaultAlarmColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR);
        mCustomAlarmColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR);
        mApplyHorizontalPaddingPref = findPreference(KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING);

        setupPreferences();

        requireActivity().setResult(Activity.RESULT_CANCELED);

        Intent intent = requireActivity().getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        saveCheckedPreferenceStates();

        updateNextAlarmWidget();
    }

    @Override
    public void onStop() {
        super.onStop();

        WidgetUtils.resetLaunchFlag();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND -> {
                mBackgroundColorPref.setVisible((boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR -> {
                mCustomTitleColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR -> {
                mCustomAlarmTitleColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR -> {
                mCustomAlarmColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING ->
                    Utils.setVibrationTime(requireContext(), 50);
        }

        requireContext().sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        mShowBackgroundOnNextAlarmWidgetPref.setOnPreferenceChangeListener(this);

        mBackgroundColorPref.setVisible(WidgetDAO.isBackgroundDisplayedOnNextAlarmWidget(mPrefs));
        mBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomTitleColorPref.setVisible(!WidgetDAO.isNextAlarmWidgetDefaultTitleColor(mPrefs));
        mCustomTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmTitleColorPref.setVisible(!WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(mPrefs));
        mCustomAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmColorPref.setVisible(!WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(mPrefs));
        mCustomAlarmColorPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mShowBackgroundOnNextAlarmWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnNextAlarmWidget(mPrefs));
        mDefaultTitleColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultTitleColor(mPrefs));
        mDefaultAlarmTitleColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(mPrefs));
        mDefaultAlarmColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isNextAlarmWidgetHorizontalPaddingApplied(mPrefs));
    }

    private void updateNextAlarmWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        NextAlarmAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}

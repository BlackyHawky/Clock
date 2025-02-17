// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.alarmclock.WidgetUtils.ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
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
    private int mRecyclerViewPosition = -1;

    ColorPreference mBackgroundColorPref;
    ColorPreference mCustomTitleColorPref;
    ColorPreference mCustomAlarmTitleColorPref;
    ColorPreference mCustomAlarmColorPref;
    EditTextPreference mNextAlarmWidgetMaxFontSizePref;
    SwitchPreferenceCompat mShowBackgroundOnNextAlarmWidgetPref;
    SwitchPreferenceCompat mDefaultTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmColorPref;

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
        mNextAlarmWidgetMaxFontSizePref = findPreference(KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE);

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

        if (mRecyclerViewPosition != -1) {
            mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
            mAppBarLayout.setExpanded(mRecyclerViewPosition == 0, true);
        }
        refresh();
        updateNextAlarmWidget();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLinearLayoutManager != null) {
            mRecyclerViewPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
        }
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
                if (mShowBackgroundOnNextAlarmWidgetPref.getSharedPreferences() != null) {
                    final boolean isNotBackgroundDisplayed = mShowBackgroundOnNextAlarmWidgetPref.getSharedPreferences()
                            .getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, false);
                    mBackgroundColorPref.setVisible(!isNotBackgroundDisplayed);
                }
                Utils.setVibrationTime(requireContext(), 50);
                requireContext().sendBroadcast(new Intent(ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED));
            }

            case KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR -> {
                if (mDefaultTitleColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultTitleColorPref.getSharedPreferences()
                            .getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, true);
                    mCustomTitleColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
                requireContext().sendBroadcast(new Intent(ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED));
            }

            case KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR -> {
                if (mDefaultAlarmTitleColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultAlarmTitleColorPref.getSharedPreferences()
                            .getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, true);
                    mCustomAlarmTitleColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
                requireContext().sendBroadcast(new Intent(ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED));
            }

            case KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR -> {
                if (mDefaultAlarmColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultAlarmColorPref.getSharedPreferences()
                            .getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, true);
                    mCustomAlarmColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
                requireContext().sendBroadcast(new Intent(ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED));
            }

            case KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE -> {
                final EditTextPreference digitalWidgetMaxClockFontSizePref = (EditTextPreference) pref;
                digitalWidgetMaxClockFontSizePref.setSummary(
                        requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                + newValue.toString()
                );
                requireContext().sendBroadcast(new Intent(ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED));
            }

            case KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR,
                 KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR,
                 KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR,
                 KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR ->
                    requireContext().sendBroadcast(new Intent(ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED));
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        mShowBackgroundOnNextAlarmWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnNextAlarmWidget(mPrefs));
        mBackgroundColorPref.setVisible(mShowBackgroundOnNextAlarmWidgetPref.isChecked());

        mNextAlarmWidgetMaxFontSizePref.setSummary(
                requireContext().getString(R.string.widget_max_clock_font_size_summary)
                        + WidgetDAO.getNextAlarmWidgetMaxFontSize(mPrefs));

        mDefaultTitleColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultTitleColor(mPrefs));
        mCustomTitleColorPref.setVisible(!mDefaultTitleColorPref.isChecked());

        mDefaultAlarmTitleColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(mPrefs));
        mCustomAlarmTitleColorPref.setVisible(!mDefaultAlarmTitleColorPref.isChecked());

        mDefaultAlarmColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(mPrefs));
        mCustomAlarmColorPref.setVisible(!mDefaultAlarmColorPref.isChecked());
    }

    private void refresh() {
        mShowBackgroundOnNextAlarmWidgetPref.setOnPreferenceChangeListener(this);

        mBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmColorPref.setOnPreferenceChangeListener(this);

        mNextAlarmWidgetMaxFontSizePref.setOnPreferenceChangeListener(this);
        mNextAlarmWidgetMaxFontSizePref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.selectAll();
        });
    }

    private void updateNextAlarmWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        NextAlarmAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}

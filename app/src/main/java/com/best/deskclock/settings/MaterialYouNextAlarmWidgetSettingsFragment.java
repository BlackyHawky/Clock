// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE;

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
import com.best.alarmclock.materialyouwidgets.MaterialYouNextAlarmAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.Utils;
import com.rarepebble.colorpicker.ColorPreference;

public class MaterialYouNextAlarmWidgetSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    ColorPreference mCustomTitleColorPref;
    ColorPreference mCustomAlarmTitleColorPref;
    ColorPreference mCustomAlarmColorPref;
    EditTextPreference mNextAlarmWidgetMaxFontSizePref;
    SwitchPreferenceCompat mDefaultTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.material_you_next_alarm_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_material_you_next_alarm_widget);

        mDefaultTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR);
        mCustomTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR);
        mDefaultAlarmTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR);
        mCustomAlarmTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR);
        mDefaultAlarmColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR);
        mCustomAlarmColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR);
        mNextAlarmWidgetMaxFontSizePref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE);

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

        refresh();
        updateMaterialYouNextAlarmWidget();
    }

    @Override
    public void onStop() {
        super.onStop();

        WidgetUtils.resetLaunchFlag();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR -> {
                if (mDefaultTitleColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultTitleColorPref.getSharedPreferences()
                            .getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, true);
                    mCustomTitleColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR -> {
                if (mDefaultAlarmTitleColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultAlarmTitleColorPref.getSharedPreferences()
                            .getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, true);
                    mCustomAlarmTitleColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR -> {
                if (mDefaultAlarmColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultAlarmColorPref.getSharedPreferences()
                            .getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, true);
                    mCustomAlarmColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE -> {
                final EditTextPreference digitalWidgetMaxClockFontSizePref = (EditTextPreference) pref;
                digitalWidgetMaxClockFontSizePref.setSummary(
                        requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                + newValue.toString()
                );
            }
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
        mNextAlarmWidgetMaxFontSizePref.setSummary(
                requireContext().getString(R.string.widget_max_clock_font_size_summary)
                        + WidgetDAO.getMaterialYouNextAlarmWidgetMaxFontSize(mPrefs)
        );

        mDefaultTitleColorPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetDefaultTitleColor(mPrefs));
        mCustomTitleColorPref.setVisible(!mDefaultTitleColorPref.isChecked());

        mDefaultAlarmTitleColorPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(mPrefs));
        mCustomAlarmTitleColorPref.setVisible(!mDefaultAlarmTitleColorPref.isChecked());

        mDefaultAlarmColorPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(mPrefs));
        mCustomAlarmColorPref.setVisible(!mDefaultAlarmColorPref.isChecked());
    }

    private void refresh() {
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

    private void updateMaterialYouNextAlarmWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        MaterialYouNextAlarmAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}

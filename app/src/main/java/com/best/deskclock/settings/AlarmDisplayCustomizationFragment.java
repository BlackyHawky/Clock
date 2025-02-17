// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_AMOLED_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SECONDS_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_TITLE_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_ALARM_SECONDS_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_RINGTONE_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_PREVIEW_ALARM;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.rarepebble.colorpicker.ColorPreference;

public class AlarmDisplayCustomizationFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private int mRecyclerViewPosition = -1;

    ListPreference mAlarmClockStyle;
    String[] mAlarmClockStyleValues;
    String mAnalogClock;
    SwitchPreferenceCompat mDisplaySecondsPref;
    ColorPreference mAlarmSecondsHandColorPref;
    ColorPreference mBackgroundColorPref;
    ColorPreference mBackgroundAmoledColorPref;
    EditTextPreference mAlarmClockFontSizePref;
    EditTextPreference mAlarmTitleFontSizePref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    Preference mPreviewAlarmPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.alarm_display_customization_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_alarm_display);

        mAlarmClockStyle = findPreference(KEY_ALARM_CLOCK_STYLE);
        mDisplaySecondsPref = findPreference(KEY_DISPLAY_ALARM_SECONDS_HAND);
        mBackgroundColorPref = findPreference(KEY_ALARM_BACKGROUND_COLOR);
        mBackgroundAmoledColorPref = findPreference(KEY_ALARM_BACKGROUND_AMOLED_COLOR);
        mAlarmSecondsHandColorPref = findPreference(KEY_ALARM_SECONDS_HAND_COLOR);
        mAlarmClockFontSizePref = findPreference(KEY_ALARM_CLOCK_FONT_SIZE);
        mAlarmTitleFontSizePref = findPreference(KEY_ALARM_TITLE_FONT_SIZE);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_RINGTONE_TITLE);
        mPreviewAlarmPref = findPreference(KEY_PREVIEW_ALARM);

        mAlarmClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mAlarmClockStyleValues[0];

        setupPreferences();
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
            case KEY_ALARM_CLOCK_STYLE -> {
                final int clockIndex = mAlarmClockStyle.findIndexOfValue((String) newValue);
                mAlarmClockStyle.setSummary(mAlarmClockStyle.getEntries()[clockIndex]);
                mAlarmClockFontSizePref.setVisible(!newValue.equals(mAnalogClock));
                mDisplaySecondsPref.setVisible(newValue.equals(mAnalogClock));
                mDisplaySecondsPref.setChecked(DataModel.getDataModel().isAlarmSecondsHandDisplayed());
                mAlarmSecondsHandColorPref.setVisible(newValue.equals(mAnalogClock)
                        && mDisplaySecondsPref.isChecked()
                );
            }

            case KEY_DISPLAY_ALARM_SECONDS_HAND -> {
                if (mDisplaySecondsPref.getSharedPreferences() != null) {
                    final boolean isAlarmSecondsHandDisplayed = mDisplaySecondsPref.getSharedPreferences()
                            .getBoolean(KEY_DISPLAY_ALARM_SECONDS_HAND, true);
                    mAlarmSecondsHandColorPref.setVisible(!isAlarmSecondsHandDisplayed);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ALARM_CLOCK_FONT_SIZE, KEY_ALARM_TITLE_FONT_SIZE -> {
                final EditTextPreference alarmFontSizePref = (EditTextPreference) pref;
                alarmFontSizePref.setSummary(newValue.toString());
            }

            case KEY_DISPLAY_RINGTONE_TITLE -> Utils.setVibrationTime(requireContext(), 50);
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        if (pref.getKey().equals(KEY_PREVIEW_ALARM)) {
            startActivity(new Intent(context, AlarmDisplayPreviewActivity.class));
            final boolean isFadeTransitionsEnabled = DataModel.getDataModel().isFadeTransitionsEnabled();
            if (isFadeTransitionsEnabled) {
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }

        return true;
    }

    private void refresh() {
        final int clockStyleIndex = mAlarmClockStyle.findIndexOfValue(DataModel.getDataModel()
                .getAlarmClockStyle().toString().toLowerCase());
        mAlarmClockStyle.setValueIndex(clockStyleIndex);
        mAlarmClockStyle.setSummary(mAlarmClockStyle.getEntries()[clockStyleIndex]);
        mAlarmClockStyle.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setChecked(DataModel.getDataModel().isAlarmSecondsHandDisplayed());
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mAlarmClockFontSizePref.setOnPreferenceChangeListener(this);
        mAlarmClockFontSizePref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.selectAll();
        });

        mAlarmTitleFontSizePref.setOnPreferenceChangeListener(this);
        mAlarmTitleFontSizePref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.selectAll();
        });

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mPreviewAlarmPref.setOnPreferenceClickListener(this);
    }

    private void setupPreferences() {
        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && getDarkMode.equals(AMOLED_DARK_MODE);
        mBackgroundAmoledColorPref.setVisible(isAmoledMode);
        mBackgroundColorPref.setVisible(!mBackgroundAmoledColorPref.isShown());

        final int clockStyleIndex = mAlarmClockStyle.findIndexOfValue(DataModel.getDataModel()
                .getAlarmClockStyle().toString().toLowerCase());
        // clockStyleIndex == 0 --> analog
        // clockStyleIndex == 1 --> digital
        mDisplaySecondsPref.setVisible(clockStyleIndex == 0);
        mDisplaySecondsPref.setChecked(DataModel.getDataModel().isAlarmSecondsHandDisplayed());
        mAlarmSecondsHandColorPref.setVisible(clockStyleIndex == 0 && mDisplaySecondsPref.isChecked());
        mAlarmClockFontSizePref.setVisible(clockStyleIndex == 1);
        mAlarmClockFontSizePref.setSummary(DataModel.getDataModel().getAlarmClockFontSize());
        mAlarmTitleFontSizePref.setSummary(DataModel.getDataModel().getAlarmTitleFontSize());
    }

}

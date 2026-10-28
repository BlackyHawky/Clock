// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.AnalogAppWidgetProvider;

public class AppWidgetAnalogSettingsFragment extends BaseSettingsScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    String[] mClockDialValues;
    String mClockDialFlower;
    String mClockDialSun;

    ListPreference mClockDialPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    ListPreference mClockSecondHandPref;
    SwitchPreferenceCompat mDefaultDialColorPref;
    ColorPickerPreference mDialColorPref;
    SwitchPreferenceCompat mDefaultHourHandColorPref;
    ColorPickerPreference mHourHandColorPref;
    SwitchPreferenceCompat mDefaultMinuteHandColorPref;
    ColorPickerPreference mMinuteHandColorPref;
    SwitchPreferenceCompat mDefaultSecondHandColorPref;
    ColorPickerPreference mSecondHandColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.analog_widget);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_analog_widget);

        mClockDialPref = findPreference(KEY_ANALOG_WIDGET_CLOCK_DIAL);
        mDisplaySecondsPref = findPreference(KEY_ANALOG_WIDGET_WITH_SECOND_HAND);
        mClockSecondHandPref = findPreference(KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND);
        mDefaultDialColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR);
        mDialColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_DIAL_COLOR);
        mDefaultHourHandColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR);
        mHourHandColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR);
        mDefaultMinuteHandColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR);
        mMinuteHandColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR);
        mDefaultSecondHandColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR);
        mSecondHandColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR);

        mClockDialValues = getResources().getStringArray(R.array.analog_widget_clock_dial_values);
        mClockDialSun = mClockDialValues[4];
        mClockDialFlower = mClockDialValues[5];

        setupPreferences();

        WidgetUtils.addFinishOnBackPressedIfLaunchedFromWidget(this);

        requireActivity().setResult(Activity.RESULT_CANCELED);

        Intent intent = requireActivity().getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
            }
        }

        invalidatePreferencesVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();

        saveCheckedPreferenceStates();

        updateAnalogWidget();

        invalidatePreferencesVisibility();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_ANALOG_WIDGET_CLOCK_DIAL -> {
                final int index = mClockDialPref.findIndexOfValue((String) newValue);
                mClockDialPref.setSummary(mClockDialPref.getEntries()[index]);
            }

            case KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_ANALOG_WIDGET_WITH_SECOND_HAND, KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR,
                 KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR, KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR,
                 KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), AnalogAppWidgetProvider.class);

        blockSearchResetForAWhile();

        if (getView() != null) {
            getView().post(() -> {
                if (isAdded()) {
                    invalidatePreferencesVisibility();
                }
            });
        }

        return true;
    }

    @Override
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        final boolean isAtLeastAndroid12 = SdkUtils.isAtLeastAndroid12();
        final boolean isSecondHandDisplayed = isAtLeastAndroid12 && mDisplaySecondsPref.isChecked();
        final String clockDial = mClockDialPref.getValue();
        final boolean isSunOrFlowerClockDial = clockDial != null && (clockDial.equals(mClockDialSun) || clockDial.equals(mClockDialFlower));

        return switch (key) {
            case KEY_ANALOG_WIDGET_WITH_SECOND_HAND, KEY_WIDGET_COLOR_CATEGORY -> isAtLeastAndroid12;
            case KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND -> isSecondHandDisplayed && !isSunOrFlowerClockDial;
            case KEY_ANALOG_WIDGET_CUSTOM_DIAL_COLOR -> !mDefaultDialColorPref.isChecked();
            case KEY_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR -> !mDefaultHourHandColorPref.isChecked();
            case KEY_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR -> !mDefaultMinuteHandColorPref.isChecked();
            case KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR -> isSecondHandDisplayed;
            case KEY_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR -> isSecondHandDisplayed && !mDefaultSecondHandColorPref.isChecked();
            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void setupPreferences() {
        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);

        mDefaultDialColorPref.setOnPreferenceChangeListener(this);

        mDialColorPref.setOnPreferenceChangeListener(this);

        mDefaultHourHandColorPref.setOnPreferenceChangeListener(this);

        mHourHandColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinuteHandColorPref.setOnPreferenceChangeListener(this);

        mMinuteHandColorPref.setOnPreferenceChangeListener(this);

        mDefaultSecondHandColorPref.setOnPreferenceChangeListener(this);

        mSecondHandColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplaySecondsPref.setChecked(WidgetDAO.isSecondHandDisplayedOnAnalogWidget(getPrefs()));
        mDefaultDialColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultDialColor(getPrefs()));
        mDefaultHourHandColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultHourHandColor(getPrefs()));
        mDefaultMinuteHandColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultMinuteHandColor(getPrefs()));
        mDefaultSecondHandColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultSecondHandColor(getPrefs()));
    }

    private void updateAnalogWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        AnalogAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}

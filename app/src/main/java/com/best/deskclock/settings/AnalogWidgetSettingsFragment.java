// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_WITH_SECOND_HAND;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.WidgetUtils;
import com.best.alarmclock.standardwidgets.AnalogAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

public class AnalogWidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    ListPreference mClockDialPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    ListPreference mClockSecondHandPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.analog_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_analog_widget);

        mClockDialPref = findPreference(KEY_ANALOG_WIDGET_CLOCK_DIAL);
        mDisplaySecondsPref = findPreference(KEY_ANALOG_WIDGET_WITH_SECOND_HAND);
        mClockSecondHandPref = findPreference(KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND);

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

        updateAnalogWidget();
    }

    @Override
    public void onStop() {
        super.onStop();

        WidgetUtils.resetLaunchFlag();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_ANALOG_WIDGET_CLOCK_DIAL, KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_ANALOG_WIDGET_WITH_SECOND_HAND -> {
                mClockSecondHandPref.setVisible((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        requireContext().sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));
        return true;
    }

    private void setupPreferences() {
        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setVisible(SdkUtils.isAtLeastAndroid12());
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setVisible(SdkUtils.isAtLeastAndroid12()
                && WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs));
        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplaySecondsPref.setChecked(WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs));
    }

    private void updateAnalogWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        AnalogAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }
}

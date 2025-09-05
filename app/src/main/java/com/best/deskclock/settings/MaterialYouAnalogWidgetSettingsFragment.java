// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.WidgetUtils;
import com.best.alarmclock.materialyouwidgets.MaterialYouAnalogAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

public class MaterialYouAnalogWidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    ListPreference mClockDialPref;
    SwitchPreferenceCompat mDisplaySecondsPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.analog_widget_material_you);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_material_you_analog_widget);

        mClockDialPref = findPreference(KEY_MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL);
        mDisplaySecondsPref = findPreference(KEY_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND);

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

        updateMaterialYouAnalogWidget();
    }

    @Override
    public void onStop() {
        super.onStop();

        WidgetUtils.resetLaunchFlag();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND ->
                    Utils.setVibrationTime(requireContext(), 50);
        }

        requireContext().sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));
        return true;
    }

    private void setupPreferences() {
        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setVisible(SdkUtils.isAtLeastAndroid12());
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplaySecondsPref.setChecked(WidgetDAO.isSecondHandDisplayedOnMaterialYouAnalogWidget(mPrefs));
    }

    private void updateMaterialYouAnalogWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        MaterialYouAnalogAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }
}

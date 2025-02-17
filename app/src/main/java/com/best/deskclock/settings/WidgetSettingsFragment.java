// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.alarmclock.WidgetUtils;
import com.best.deskclock.R;

public class WidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

    Preference mDigitalWidgetCustomizationPref;
    Preference mVerticalDigitalWidgetCustomizationPref;
    Preference mNextAlarmWidgetCustomizationPref;
    Preference mMaterialYouDigitalWidgetCustomizationPref;
    Preference mMaterialYouVerticalDigitalWidgetCustomizationPref;
    Preference mMaterialYouNextAlarmWidgetCustomizationPref;

    int mRecyclerViewPosition = -1;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.widgets_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_widgets);

        mDigitalWidgetCustomizationPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZATION);
        mVerticalDigitalWidgetCustomizationPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION);
        mNextAlarmWidgetCustomizationPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION);
        mMaterialYouDigitalWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION);
        mMaterialYouVerticalDigitalWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION);
        mMaterialYouNextAlarmWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION);
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
    public boolean onPreferenceClick(@NonNull Preference pref) {
        WidgetUtils.isLaunchedFromWidget = false;

        switch (pref.getKey()) {
            case KEY_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new DigitalWidgetSettingsFragment());

            case KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new VerticalDigitalWidgetSettingsFragment());

            case KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new NextAlarmWidgetSettingsFragment());

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new MaterialYouDigitalWidgetSettingsFragment());

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new MaterialYouVerticalDigitalWidgetSettingsFragment());

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new MaterialYouNextAlarmWidgetSettingsFragment());
        }

        return true;
    }

    private void refresh() {
        mDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mVerticalDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mNextAlarmWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mMaterialYouDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mMaterialYouVerticalDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mMaterialYouNextAlarmWidgetCustomizationPref.setOnPreferenceClickListener(this);
    }

}

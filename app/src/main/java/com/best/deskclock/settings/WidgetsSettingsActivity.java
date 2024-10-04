// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

public class WidgetsSettingsActivity extends CollapsingToolbarBaseActivity {

    public static final String PREFS_FRAGMENT_TAG = "widgets_settings_prefs_fragment";

    public static final String KEY_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_digital_widget_customization";
    public static final String KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_vertical_digital_widget_customization";
    public static final String KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION =
            "key_next_alarm_widget_customization";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_material_you_digital_widget_customization";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_material_you_vertical_digital_widget_customization";
    public static final String KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION =
            "key_material_you_next_alarm_widget_customization";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class PrefsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

        Preference mDigitalWidgetCustomizationPref;
        Preference mVerticalDigitalWidgetCustomizationPref;
        Preference mNextAlarmWidgetCustomizationPref;
        Preference mMaterialYouDigitalWidgetCustomizationPref;
        Preference mMaterialYouVerticalDigitalWidgetCustomizationPref;
        Preference mMaterialYouNextAlarmWidgetCustomizationPref;

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

            refresh();
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            final Context context = getActivity();
            if (context == null) {
                return false;
            }

            switch (pref.getKey()) {
                case KEY_DIGITAL_WIDGET_CUSTOMIZATION -> {
                    final Intent digitalWidgetCustomizationIntent =
                            new Intent(context, DigitalWidgetCustomizationActivity.class);
                    startActivity(digitalWidgetCustomizationIntent);
                    return true;
                }

                case KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION -> {
                    final Intent verticalDigitalWidgetCustomizationIntent =
                            new Intent(context, VerticalDigitalWidgetCustomizationActivity.class);
                    startActivity(verticalDigitalWidgetCustomizationIntent);
                    return true;
                }

                case KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION -> {
                    final Intent nextAlarmWidgetCustomizationIntent =
                            new Intent(context, NextAlarmWidgetCustomizationActivity.class);
                    startActivity(nextAlarmWidgetCustomizationIntent);
                    return true;
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION -> {
                    final Intent digitalWidgetMaterialYouCustomizationIntent =
                            new Intent(context, MaterialYouDigitalWidgetCustomizationActivity.class);
                    startActivity(digitalWidgetMaterialYouCustomizationIntent);
                    return true;
                }

                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION -> {
                    final Intent materialYouVerticalDigitalWidgetCustomizationIntent =
                            new Intent(context, MaterialYouVerticalDigitalWidgetCustomizationActivity.class);
                    startActivity(materialYouVerticalDigitalWidgetCustomizationIntent);
                    return true;
                }

                case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION -> {
                    final Intent materialYouNextAlarmWidgetCustomizationIntent =
                            new Intent(context, MaterialYouNextAlarmWidgetCustomizationActivity.class);
                    startActivity(materialYouNextAlarmWidgetCustomizationIntent);
                    return true;
                }
            }

            return false;
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
}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.data.WidgetModel.ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.materialyouwidgets.MaterialYouVerticalDigitalAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.rarepebble.colorpicker.ColorPreference;

public class MaterialYouVerticalDigitalWidgetCustomizationActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "material_you_vertical_digital_widget_customization_fragment";

    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR =
            "key_material_you_vertical_digital_widget_default_hours_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR =
            "key_material_you_vertical_digital_widget_custom_hours_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR =
            "key_material_you_vertical_digital_widget_default_minutes_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR =
            "key_material_you_vertical_digital_widget_custom_minutes_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR =
            "key_material_you_vertical_digital_widget_default_date_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR =
            "key_material_you_vertical_digital_widget_custom_date_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR =
            "key_material_you_vertical_digital_widget_default_next_alarm_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR =
            "key_material_you_vertical_digital_widget_custom_next_alarm_color";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE =
            "key_material_you_vertical_digital_widget_max_clock_font_size";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_FONT_SIZE = "70";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class PrefsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

        private int mAppWidgetId = INVALID_APPWIDGET_ID;

        ColorPreference mCustomHoursColorPref;
        ColorPreference mCustomMinutesColorPref;
        ColorPreference mCustomDateColorPref;
        ColorPreference mCustomNextAlarmColorPref;
        EditTextPreference mDigitalWidgetMaxClockFontSizePref;
        SwitchPreferenceCompat mDefaultHoursColorPref;
        SwitchPreferenceCompat mDefaultMinutesColorPref;
        SwitchPreferenceCompat mDefaultDateColorPref;
        SwitchPreferenceCompat mDefaultNextAlarmColorPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_customize_material_you_vertical_digital_widget);

            mDefaultHoursColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR);
            mCustomHoursColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR);
            mDefaultMinutesColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR);
            mCustomMinutesColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR);
            mDefaultDateColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR);
            mCustomDateColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
            mDefaultNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
            mCustomNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);
            mDigitalWidgetMaxClockFontSizePref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE);

            setupPreferences();

            requireActivity().setResult(RESULT_CANCELED);

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
            updateMaterialYouVerticalDigitalWidget();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR -> {
                    if (mDefaultHoursColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultHoursColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, true);
                        mCustomHoursColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR -> {
                    if (mDefaultMinutesColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultMinutesColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, true);
                        mCustomMinutesColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                    if (mDefaultDateColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultDateColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true);
                        mCustomDateColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                    if (mDefaultNextAlarmColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultNextAlarmColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true);
                        mCustomNextAlarmColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE -> {
                    final EditTextPreference digitalWidgetMaxClockFontSizePref = (EditTextPreference) pref;
                    digitalWidgetMaxClockFontSizePref.setSummary(
                            requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                    + newValue.toString()
                    );
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR,
                     KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR,
                     KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                     KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR ->
                        requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED));
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
            mDigitalWidgetMaxClockFontSizePref.setSummary(
                    requireContext().getString(R.string.widget_max_clock_font_size_summary)
                            + DataModel.getDataModel().getMaterialYouVerticalDigitalWidgetMaxClockFontSize()
            );

            mDefaultHoursColorPref.setChecked(DataModel.getDataModel().isMaterialYouVerticalDigitalWidgetDefaultHoursColor());
            mCustomHoursColorPref.setVisible(!mDefaultHoursColorPref.isChecked());

            mDefaultMinutesColorPref.setChecked(DataModel.getDataModel().isMaterialYouVerticalDigitalWidgetDefaultMinutesColor());
            mCustomMinutesColorPref.setVisible(!mDefaultMinutesColorPref.isChecked());

            mDefaultDateColorPref.setChecked(DataModel.getDataModel().isMaterialYouVerticalDigitalWidgetDefaultDateColor());
            mCustomDateColorPref.setVisible(!mDefaultDateColorPref.isChecked());

            mDefaultNextAlarmColorPref.setChecked(
                    DataModel.getDataModel().isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor()
            );
            mCustomNextAlarmColorPref.setVisible(!mDefaultNextAlarmColorPref.isChecked());
        }

        private void refresh() {
            mDefaultHoursColorPref.setOnPreferenceChangeListener(this);

            mCustomHoursColorPref.setOnPreferenceChangeListener(this);

            mDefaultMinutesColorPref.setOnPreferenceChangeListener(this);

            mCustomMinutesColorPref.setOnPreferenceChangeListener(this);

            mDefaultDateColorPref.setOnPreferenceChangeListener(this);

            mCustomDateColorPref.setOnPreferenceChangeListener(this);

            mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

            mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

            mDigitalWidgetMaxClockFontSizePref.setOnPreferenceChangeListener(this);
            mDigitalWidgetMaxClockFontSizePref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.selectAll();
            });
        }

        private void updateMaterialYouVerticalDigitalWidget() {
            AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
            MaterialYouVerticalDigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

            Intent result = new Intent();
            result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
            requireActivity().setResult(RESULT_OK, result);
        }
    }

}

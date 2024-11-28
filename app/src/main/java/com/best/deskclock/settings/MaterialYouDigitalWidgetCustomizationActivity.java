// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.data.WidgetModel.ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.materialyouwidgets.MaterialYouDigitalAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.rarepebble.colorpicker.ColorPreference;

import java.util.ArrayList;
import java.util.List;

public class MaterialYouDigitalWidgetCustomizationActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "material_you_digital_widget_customization_fragment";

    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED =
            "key_material_you_digital_widget_world_cities_displayed";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR =
            "key_material_you_digital_widget_default_clock_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR =
            "key_material_you_digital_widget_custom_clock_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR =
            "key_material_you_digital_widget_default_date_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR =
            "key_material_you_digital_widget_custom_date_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR =
            "key_material_you_digital_widget_default_next_alarm_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR =
            "key_material_you_digital_widget_custom_next_alarm_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR =
            "key_material_you_digital_widget_default_city_clock_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR =
            "key_material_you_digital_widget_custom_city_clock_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR =
            "key_material_you_digital_widget_default_city_name_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR =
            "key_material_you_digital_widget_custom_city_name_color";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE =
            "key_material_you_digital_widget_max_clock_font_size";
    public static final String KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_FONT_SIZE = "80";

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

        ColorPreference mCustomClockColorPref;
        ColorPreference mCustomDateColorPref;
        ColorPreference mCustomNextAlarmColorPref;
        ColorPreference mCustomCityClockColorPref;
        ColorPreference mCustomCityNameColorPref;
        EditTextPreference mDigitalWidgetMaxClockFontSizePref;
        SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;
        SwitchPreferenceCompat mDefaultClockColorPref;
        SwitchPreferenceCompat mDefaultDateColorPref;
        SwitchPreferenceCompat mDefaultNextAlarmColorPref;
        SwitchPreferenceCompat mDefaultCityClockColorPref;
        SwitchPreferenceCompat mDefaultCityNameColorPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_customize_material_you_digital_widget);

            mShowCitiesOnDigitalWidgetPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
            mDefaultClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR);
            mCustomClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR);
            mDefaultDateColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR);
            mCustomDateColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
            mDefaultNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
            mCustomNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);
            mDefaultCityClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR);
            mCustomCityClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR);
            mDefaultCityNameColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR);
            mCustomCityNameColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR);
            mDigitalWidgetMaxClockFontSizePref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE);

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
            updateMaterialYouDigitalWidget();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED -> {
                    if (mShowCitiesOnDigitalWidgetPref.getSharedPreferences() != null
                            && mDefaultCityClockColorPref.getSharedPreferences() != null
                            && mDefaultCityNameColorPref.getSharedPreferences() != null) {

                        final boolean areCitiesDisplayed =
                                mShowCitiesOnDigitalWidgetPref.getSharedPreferences().getBoolean(
                                        KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true);

                        final boolean isCityClockDefaultColors =
                                mDefaultCityClockColorPref.getSharedPreferences().getBoolean(
                                        KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, true);

                        final boolean isCityNameDefaultColor =
                                mDefaultCityNameColorPref.getSharedPreferences().getBoolean(
                                        KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, true);

                        mDefaultCityClockColorPref.setVisible(!areCitiesDisplayed);
                        mCustomCityClockColorPref.setVisible(!areCitiesDisplayed && !isCityClockDefaultColors);
                        mDefaultCityNameColorPref.setVisible(!areCitiesDisplayed);
                        mCustomCityNameColorPref.setVisible(!areCitiesDisplayed && !isCityNameDefaultColor);
                        mDigitalWidgetMaxClockFontSizePref.setEnabled(areCitiesDisplayed);
                        if (!areCitiesDisplayed) {
                            mDigitalWidgetMaxClockFontSizePref.setSummary(
                                    requireContext().getString(R.string.digital_widget_message_summary)
                            );
                        } else {
                            mDigitalWidgetMaxClockFontSizePref.setSummary(
                                    requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                            + DataModel.getDataModel().getMaterialYouDigitalWidgetMaxClockFontSize()
                            );
                        }
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR -> {
                    if (mDefaultClockColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultClockColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, true);
                        mCustomClockColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                    if (mDefaultDateColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultDateColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true);
                        mCustomDateColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                    if (mDefaultNextAlarmColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultNextAlarmColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true);
                        mCustomNextAlarmColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR -> {
                    if (mDefaultCityClockColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultCityClockColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, true);
                        mCustomCityClockColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR -> {
                    if (mDefaultCityNameColorPref.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDefaultCityNameColorPref.getSharedPreferences()
                                .getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, true);
                        mCustomCityNameColorPref.setVisible(isNotDefaultColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE -> {
                    final EditTextPreference digitalWidgetMaxClockFontSizePref = (EditTextPreference) pref;
                    digitalWidgetMaxClockFontSizePref.setSummary(
                            requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                    + newValue.toString()
                    );
                    requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
                }

                case KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR,
                     KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                     KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR,
                     KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR,
                     KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR ->
                        requireContext().sendBroadcast(new Intent(ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED));
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
            List<City> selectedCities = new ArrayList<>(DataModel.getDataModel().getSelectedCities());
            final boolean showHomeClock = DataModel.getDataModel().getShowHomeClock();
            mShowCitiesOnDigitalWidgetPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
            mDefaultCityClockColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
            mCustomCityClockColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
            mDefaultCityNameColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
            mCustomCityNameColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);

            mShowCitiesOnDigitalWidgetPref.setChecked(
                    DataModel.getDataModel().areWorldCitiesDisplayedOnMaterialYouDigitalWidget()
            );
            if (mShowCitiesOnDigitalWidgetPref.isShown()) {
                mDefaultCityClockColorPref.setChecked(
                        DataModel.getDataModel().isMaterialYouDigitalWidgetDefaultCityClockColor()
                );
                mDefaultCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked());
                mCustomCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked()
                        && !mDefaultCityClockColorPref.isChecked()
                );

                mDefaultCityNameColorPref.setChecked(
                        DataModel.getDataModel().isMaterialYouDigitalWidgetDefaultCityNameColor()
                );
                mDefaultCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked());
                mCustomCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked()
                        && !mDefaultCityNameColorPref.isChecked()
                );

                if (mShowCitiesOnDigitalWidgetPref.isChecked()) {
                    mDigitalWidgetMaxClockFontSizePref.setEnabled(false);
                    mDigitalWidgetMaxClockFontSizePref.setSummary(
                            requireContext().getString(R.string.digital_widget_message_summary)
                    );
                } else {
                    mDigitalWidgetMaxClockFontSizePref.setEnabled(true);
                    mDigitalWidgetMaxClockFontSizePref.setSummary(
                            requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                    + DataModel.getDataModel().getMaterialYouDigitalWidgetMaxClockFontSize()
                    );
                }
            } else {
                mDigitalWidgetMaxClockFontSizePref.setEnabled(true);
                mDigitalWidgetMaxClockFontSizePref.setSummary(
                        requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                + DataModel.getDataModel().getMaterialYouDigitalWidgetMaxClockFontSize()
                );
            }

            mDefaultClockColorPref.setChecked(DataModel.getDataModel().isMaterialYouDigitalWidgetDefaultClockColor());
            mCustomClockColorPref.setVisible(!mDefaultClockColorPref.isChecked());

            mDefaultDateColorPref.setChecked(DataModel.getDataModel().isMaterialYouDigitalWidgetDefaultDateColor());
            mCustomDateColorPref.setVisible(!mDefaultDateColorPref.isChecked());

            mDefaultNextAlarmColorPref.setChecked(
                    DataModel.getDataModel().isMaterialYouDigitalWidgetDefaultNextAlarmColor()
            );
            mCustomNextAlarmColorPref.setVisible(!mDefaultNextAlarmColorPref.isChecked());
        }

        private void refresh() {
            mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

            mDefaultClockColorPref.setOnPreferenceChangeListener(this);

            mCustomClockColorPref.setOnPreferenceChangeListener(this);

            mDefaultDateColorPref.setOnPreferenceChangeListener(this);

            mCustomDateColorPref.setOnPreferenceChangeListener(this);

            mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

            mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

            mDefaultCityClockColorPref.setOnPreferenceChangeListener(this);

            mCustomCityClockColorPref.setOnPreferenceChangeListener(this);

            mDefaultCityNameColorPref.setOnPreferenceChangeListener(this);

            mCustomCityNameColorPref.setOnPreferenceChangeListener(this);

            mDigitalWidgetMaxClockFontSizePref.setOnPreferenceChangeListener(this);
            mDigitalWidgetMaxClockFontSizePref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.selectAll();
            });
        }

        private void updateMaterialYouDigitalWidget() {
            AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
            MaterialYouDigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

            Intent result = new Intent();
            result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
            requireActivity().setResult(RESULT_OK, result);
        }
    }

}

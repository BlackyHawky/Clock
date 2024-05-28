// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.DigitalAppWidgetMaterialYouProvider;
import com.best.alarmclock.DigitalAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.rarepebble.colorpicker.ColorPreference;

public class DigitalWidgetCustomizationActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "digital_widget_customization_fragment";

    public static final String KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED = "key_digital_widget_world_cities_displayed";
    public static final String KEY_DIGITAL_WIDGET_CLOCK_DEFAULT_COLOR = "key_digital_widget_clock_default_color";
    public static final String KEY_DIGITAL_WIDGET_CLOCK_CUSTOM_COLOR = "key_digital_widget_clock_custom_color";
    public static final String KEY_DIGITAL_WIDGET_DATE_DEFAULT_COLOR = "key_digital_widget_date_default_color";
    public static final String KEY_DIGITAL_WIDGET_DATE_CUSTOM_COLOR = "key_digital_widget_date_custom_color";
    public static final String KEY_DIGITAL_WIDGET_NEXT_ALARM_DEFAULT_COLOR = "key_digital_widget_next_alarm_default_color";
    public static final String KEY_DIGITAL_WIDGET_NEXT_ALARM_CUSTOM_COLOR = "key_digital_widget_next_alarm_custom_color";
    public static final String KEY_DIGITAL_WIDGET_CITY_CLOCK_DEFAULT_COLOR = "key_digital_widget_city_clock_default_color";
    public static final String KEY_DIGITAL_WIDGET_CITY_CLOCK_CUSTOM_COLOR = "key_digital_widget_city_clock_custom_color";
    public static final String KEY_DIGITAL_WIDGET_CITY_NAME_DEFAULT_COLOR = "key_digital_widget_city_name_default_color";
    public static final String KEY_DIGITAL_WIDGET_CITY_NAME_CUSTOM_COLOR = "key_digital_widget_city_name_custom_color";
    public static final String KEY_DIGITAL_WIDGET_MESSAGE = "key_digital_widget_message";
    public static final String KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE = "key_digital_widget_max_clock_font_size";
    public static final String DEFAULT_DIGITAL_WIDGET_FONT_SIZE = "80";

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

    public static class PrefsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        private int mAppWidgetId = INVALID_APPWIDGET_ID;

        ColorPreference mClockCustomColor;
        ColorPreference mDateCustomColor;
        ColorPreference mNextAlarmCustomColor;
        ColorPreference mCityClockCustomColor;
        ColorPreference mCityNameCustomColor;
        EditTextPreference mDigitalWidgetMaxClockFontSizePref;
        Preference mDigitalWidgetMessagePref;
        SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;
        SwitchPreferenceCompat mClockDefaultColor;
        SwitchPreferenceCompat mDateDefaultColor;
        SwitchPreferenceCompat mNextAlarmDefaultColor;
        SwitchPreferenceCompat mCityClockDefaultColor;
        SwitchPreferenceCompat mCityNameDefaultColor;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            hidePreferences();

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
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.settings_customize_digital_widget);
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED -> {
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_WORLD_CITIES_DISPLAYED));

                    if (mShowCitiesOnDigitalWidgetPref.getSharedPreferences() != null
                            && mCityClockDefaultColor.getSharedPreferences() != null
                            && mCityNameDefaultColor.getSharedPreferences() != null) {

                        final boolean areCitiesDisplayed = mShowCitiesOnDigitalWidgetPref.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true);

                        final boolean isCityClockDefaultColors = mCityClockDefaultColor.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_CITY_CLOCK_DEFAULT_COLOR, true);

                        final boolean isCityNameDefaultColor = mCityNameDefaultColor.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_CITY_NAME_DEFAULT_COLOR, true);

                        mCityClockDefaultColor.setVisible(!areCitiesDisplayed);
                        mCityClockCustomColor.setVisible(!areCitiesDisplayed && !isCityClockDefaultColors);
                        mCityNameDefaultColor.setVisible(!areCitiesDisplayed);
                        mCityNameCustomColor.setVisible(!areCitiesDisplayed && !isCityNameDefaultColor);
                        mDigitalWidgetMessagePref.setVisible(areCitiesDisplayed);
                        mDigitalWidgetMaxClockFontSizePref.setVisible(areCitiesDisplayed);
                    }

                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_DIGITAL_WIDGET_CLOCK_DEFAULT_COLOR -> {
                    if (mClockDefaultColor.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mClockDefaultColor.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_CLOCK_DEFAULT_COLOR, true);
                        mClockCustomColor.setVisible(isNotDefaultColors);
                    }
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CLOCK_COLOR_CHANGED));
                }

                case KEY_DIGITAL_WIDGET_CLOCK_CUSTOM_COLOR ->
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CLOCK_COLOR_CHANGED));


                case KEY_DIGITAL_WIDGET_DATE_DEFAULT_COLOR -> {
                    if (mDateDefaultColor.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mDateDefaultColor.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, true);
                        mDateCustomColor.setVisible(isNotDefaultColors);
                    }
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_DATE_COLOR_CHANGED));
                }

                case KEY_DIGITAL_WIDGET_DATE_CUSTOM_COLOR ->
                        requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_DATE_COLOR_CHANGED));

                case KEY_DIGITAL_WIDGET_NEXT_ALARM_DEFAULT_COLOR -> {
                    if (mNextAlarmDefaultColor.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mNextAlarmDefaultColor.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_NEXT_ALARM_DEFAULT_COLOR, true);
                        mNextAlarmCustomColor.setVisible(isNotDefaultColors);
                    }
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_NEXT_ALARM_COLOR_CHANGED));
                }

                case KEY_DIGITAL_WIDGET_NEXT_ALARM_CUSTOM_COLOR ->
                        requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_NEXT_ALARM_COLOR_CHANGED));

                case KEY_DIGITAL_WIDGET_CITY_CLOCK_DEFAULT_COLOR -> {
                    if (mCityClockDefaultColor.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mCityClockDefaultColor.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_CITY_CLOCK_DEFAULT_COLOR, true);
                        mCityClockCustomColor.setVisible(isNotDefaultColors);
                    }
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CITY_CLOCK_COLOR_CHANGED));
                }

                case KEY_DIGITAL_WIDGET_CITY_CLOCK_CUSTOM_COLOR ->
                        requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CITY_CLOCK_COLOR_CHANGED));

                case KEY_DIGITAL_WIDGET_CITY_NAME_DEFAULT_COLOR -> {
                    if (mCityNameDefaultColor.getSharedPreferences() != null) {
                        final boolean isNotDefaultColors = mCityNameDefaultColor.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_CITY_NAME_DEFAULT_COLOR, true);
                        mCityNameCustomColor.setVisible(isNotDefaultColors);
                    }
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CITY_NAME_COLOR_CHANGED));
                }

                case KEY_DIGITAL_WIDGET_CITY_NAME_CUSTOM_COLOR ->
                        requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CITY_NAME_COLOR_CHANGED));

                case KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE -> {
                    final EditTextPreference digitalWidgetMaxClockFontSizePref = (EditTextPreference) pref;
                    digitalWidgetMaxClockFontSizePref.setSummary(
                            requireContext().getString(R.string.settings_digital_widget_max_clock_font_size_summary)
                                    + newValue.toString()
                                    + " "
                                    + "dp"
                    );
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CLOCK_FONT_SIZE_CHANGED));
                }
            }
            updateDigitalWidget();
            return true;
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (preference instanceof ColorPreference) {
                ((ColorPreference) preference).showDialog(this, 0);
            } else super.onDisplayPreferenceDialog(preference);
        }

        private void hidePreferences() {
            mShowCitiesOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
            mClockDefaultColor = findPreference(KEY_DIGITAL_WIDGET_CLOCK_DEFAULT_COLOR);
            mClockCustomColor = findPreference(KEY_DIGITAL_WIDGET_CLOCK_CUSTOM_COLOR);
            mDateDefaultColor = findPreference(KEY_DIGITAL_WIDGET_DATE_DEFAULT_COLOR);
            mDateCustomColor = findPreference(KEY_DIGITAL_WIDGET_DATE_CUSTOM_COLOR);
            mNextAlarmDefaultColor = findPreference(KEY_DIGITAL_WIDGET_NEXT_ALARM_DEFAULT_COLOR);
            mNextAlarmCustomColor = findPreference(KEY_DIGITAL_WIDGET_NEXT_ALARM_CUSTOM_COLOR);
            mCityClockDefaultColor = findPreference(KEY_DIGITAL_WIDGET_CITY_CLOCK_DEFAULT_COLOR);
            mCityClockCustomColor = findPreference(KEY_DIGITAL_WIDGET_CITY_CLOCK_CUSTOM_COLOR);
            mCityNameDefaultColor = findPreference(KEY_DIGITAL_WIDGET_CITY_NAME_DEFAULT_COLOR);
            mCityNameCustomColor = findPreference(KEY_DIGITAL_WIDGET_CITY_NAME_CUSTOM_COLOR);

            mDigitalWidgetMessagePref = findPreference(KEY_DIGITAL_WIDGET_MESSAGE);
            mDigitalWidgetMaxClockFontSizePref = findPreference(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE);

            mShowCitiesOnDigitalWidgetPref.setChecked(DataModel.getDataModel().areWorldCitiesDisplayedOnWidget());
            mDigitalWidgetMessagePref.setVisible(!mShowCitiesOnDigitalWidgetPref.isChecked());
            mDigitalWidgetMaxClockFontSizePref.setVisible(!mShowCitiesOnDigitalWidgetPref.isChecked());

            mClockDefaultColor.setChecked(DataModel.getDataModel().isDigitalWidgetClockDefaultColor());
            mClockCustomColor.setVisible(!mClockDefaultColor.isChecked());

            mDateDefaultColor.setChecked(DataModel.getDataModel().isDigitalWidgetDateDefaultColor());
            mDateCustomColor.setVisible(!mDateDefaultColor.isChecked());

            mNextAlarmDefaultColor.setChecked(DataModel.getDataModel().isDigitalWidgetNextAlarmDefaultColor());
            mNextAlarmCustomColor.setVisible(!mNextAlarmDefaultColor.isChecked());

            mCityClockDefaultColor.setChecked(DataModel.getDataModel().isDigitalWidgetCityClockDefaultColor());
            mCityClockDefaultColor.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked());
            mCityClockCustomColor.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked()
                    && !mCityClockDefaultColor.isChecked()
            );

            mCityNameDefaultColor.setChecked(DataModel.getDataModel().isDigitalWidgetCityNameDefaultColor());
            mCityNameDefaultColor.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked());
            mCityNameCustomColor.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked()
                    && !mCityNameDefaultColor.isChecked()
            );
        }

        private void refresh() {
            mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

            mClockDefaultColor.setOnPreferenceChangeListener(this);

            mClockCustomColor.setOnPreferenceChangeListener(this);

            mDateDefaultColor.setOnPreferenceChangeListener(this);

            mDateCustomColor.setOnPreferenceChangeListener(this);

            mNextAlarmDefaultColor.setOnPreferenceChangeListener(this);

            mNextAlarmCustomColor.setOnPreferenceChangeListener(this);

            mCityClockDefaultColor.setOnPreferenceChangeListener(this);

            mCityClockCustomColor.setOnPreferenceChangeListener(this);

            mCityNameDefaultColor.setOnPreferenceChangeListener(this);

            mCityNameCustomColor.setOnPreferenceChangeListener(this);

            final SpannableStringBuilder builderDigitalWidgetMessage = new SpannableStringBuilder();
            final String digitalWidgetMessage = requireContext().getString(R.string.settings_digital_widget_message);
            final Spannable spannableDigitalWidgetMessage = new SpannableString(digitalWidgetMessage);
            spannableDigitalWidgetMessage.setSpan(new StyleSpan(Typeface.ITALIC), 0, digitalWidgetMessage.length(), 0);
            spannableDigitalWidgetMessage.setSpan(new StyleSpan(Typeface.BOLD), 0, digitalWidgetMessage.length(), 0);
            builderDigitalWidgetMessage.append(spannableDigitalWidgetMessage);
            mDigitalWidgetMessagePref.setTitle(builderDigitalWidgetMessage);

            mDigitalWidgetMaxClockFontSizePref.setOnPreferenceChangeListener(this);
            mDigitalWidgetMaxClockFontSizePref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.selectAll();
            });
            mDigitalWidgetMaxClockFontSizePref.setSummary(
                    requireContext().getString(R.string.settings_digital_widget_max_clock_font_size_summary)
                            + DataModel.getDataModel().getDigitalWidgetMaxClockFontSize()
                            + " "
                            + "dp"
            );
        }

        private void updateDigitalWidget() {
            AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
            DigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);
            DigitalAppWidgetMaterialYouProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

            Intent result = new Intent();
            result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
            requireActivity().setResult(RESULT_OK, result);
        }
    }

}

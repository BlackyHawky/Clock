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

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.DigitalAppWidgetMaterialYouProvider;
import com.best.alarmclock.DigitalAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.Objects;

public class DigitalWidgetCustomizationActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "digital_widget_customization_fragment";

    public static final String KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED = "key_digital_widget_world_cities_displayed";
    public static final String KEY_DIGITAL_WIDGET_CLOCK_COLOR = "key_digital_widget_clock_color";
    public static final String KEY_DIGITAL_WIDGET_DATE_COLOR = "key_digital_widget_date_color";
    public static final String KEY_DIGITAL_WIDGET_NEXT_ALARM_COLOR = "key_digital_widget_next_alarm_color";
    public static final String KEY_DIGITAL_WIDGET_CITY_CLOCK_COLOR = "key_digital_widget_city_clock_color";
    public static final String KEY_DIGITAL_WIDGET_CITY_NAME_COLOR = "key_digital_widget_city_name_color";
    public static final String KEY_DIGITAL_WIDGET_MESSAGE = "key_digital_widget_message";
    public static final String KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE = "key_digital_widget_max_clock_font_size";

    public static final String DEFAULT_DIGITAL_WIDGET_FONT_SIZE = "80";

    public static final String DEFAULT_DIGITAL_WIDGET_COLOR = "0";
    public static final String BLUE_GRAY_DIGITAL_WIDGET_COLOR = "1";
    public static final String BROWN_DIGITAL_WIDGET_COLOR = "2";
    public static final String GREEN_DIGITAL_WIDGET_COLOR = "3";
    public static final String INDIGO_DIGITAL_WIDGET_COLOR = "4";
    public static final String ORANGE_DIGITAL_WIDGET_COLOR = "5";
    public static final String PINK_DIGITAL_WIDGET_COLOR = "6";
    public static final String RED_DIGITAL_WIDGET_COLOR = "7";

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

        EditTextPreference mDigitalWidgetMaxClockFontSizePref;
        Preference mDigitalWidgetMessagePref;
        SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;

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

                    if (mShowCitiesOnDigitalWidgetPref.getSharedPreferences() != null) {
                        final boolean areCitiesDisplayed = mShowCitiesOnDigitalWidgetPref.getSharedPreferences()
                                .getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true);
                        mDigitalWidgetMessagePref.setVisible(areCitiesDisplayed);
                        mDigitalWidgetMaxClockFontSizePref.setVisible(areCitiesDisplayed);
                    }

                    Utils.setVibrationTime(requireContext(), 50);
                }
                case KEY_DIGITAL_WIDGET_CLOCK_COLOR -> {
                    final ListPreference digitalWidgetClockColorPref = (ListPreference) pref;
                    final int index = digitalWidgetClockColorPref.findIndexOfValue((String) newValue);
                    digitalWidgetClockColorPref.setSummary(digitalWidgetClockColorPref.getEntries()[index]);
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CLOCK_COLOR_CHANGED));
                }
                case KEY_DIGITAL_WIDGET_DATE_COLOR -> {
                    final ListPreference digitalWidgetDateColorPref = (ListPreference) pref;
                    final int index = digitalWidgetDateColorPref.findIndexOfValue((String) newValue);
                    digitalWidgetDateColorPref.setSummary(digitalWidgetDateColorPref.getEntries()[index]);
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_DATE_COLOR_CHANGED));
                    requireActivity().setResult(RESULT_OK);
                }
                case KEY_DIGITAL_WIDGET_NEXT_ALARM_COLOR -> {
                    final ListPreference digitalWidgetNextAlarmColorPref = (ListPreference) pref;
                    final int index = digitalWidgetNextAlarmColorPref.findIndexOfValue((String) newValue);
                    digitalWidgetNextAlarmColorPref.setSummary(digitalWidgetNextAlarmColorPref.getEntries()[index]);
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_NEXT_ALARM_COLOR_CHANGED));
                }
                case KEY_DIGITAL_WIDGET_CITY_CLOCK_COLOR -> {
                    final ListPreference digitalWidgetCityClockColorPref = (ListPreference) pref;
                    final int index = digitalWidgetCityClockColorPref.findIndexOfValue((String) newValue);
                    digitalWidgetCityClockColorPref.setSummary(digitalWidgetCityClockColorPref.getEntries()[index]);
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CITY_CLOCK_COLOR_CHANGED));
                }
                case KEY_DIGITAL_WIDGET_CITY_NAME_COLOR -> {
                    final ListPreference digitalWidgetCityNameColorPref = (ListPreference) pref;
                    final int index = digitalWidgetCityNameColorPref.findIndexOfValue((String) newValue);
                    digitalWidgetCityNameColorPref.setSummary(digitalWidgetCityNameColorPref.getEntries()[index]);
                    requireContext().sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CITY_NAME_COLOR_CHANGED));
                }
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

        private void hidePreferences() {
            mShowCitiesOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
            mDigitalWidgetMessagePref = findPreference(KEY_DIGITAL_WIDGET_MESSAGE);
            mDigitalWidgetMaxClockFontSizePref = findPreference(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE);

            mShowCitiesOnDigitalWidgetPref.setChecked(DataModel.getDataModel().areWorldCitiesDisplayedOnWidget());
            mDigitalWidgetMessagePref.setVisible(!mShowCitiesOnDigitalWidgetPref.isChecked());
            mDigitalWidgetMaxClockFontSizePref.setVisible(!mShowCitiesOnDigitalWidgetPref.isChecked());
        }

        private void refresh() {
            mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

            final ListPreference digitalWidgetClockColorPref = findPreference(KEY_DIGITAL_WIDGET_CLOCK_COLOR);
            Objects.requireNonNull(digitalWidgetClockColorPref).setSummary(digitalWidgetClockColorPref.getEntry());
            digitalWidgetClockColorPref.setOnPreferenceChangeListener(this);

            final ListPreference digitalWidgetDateColorPref = findPreference(KEY_DIGITAL_WIDGET_DATE_COLOR);
            Objects.requireNonNull(digitalWidgetDateColorPref).setSummary(digitalWidgetDateColorPref.getEntry());
            digitalWidgetDateColorPref.setOnPreferenceChangeListener(this);

            final ListPreference digitalWidgetNextAlarmColorPref = findPreference(KEY_DIGITAL_WIDGET_NEXT_ALARM_COLOR);
            Objects.requireNonNull(digitalWidgetNextAlarmColorPref).setSummary(digitalWidgetNextAlarmColorPref.getEntry());
            digitalWidgetNextAlarmColorPref.setOnPreferenceChangeListener(this);

            final ListPreference digitalWidgetCityClockColorPref = findPreference(KEY_DIGITAL_WIDGET_CITY_CLOCK_COLOR);
            Objects.requireNonNull(digitalWidgetCityClockColorPref).setSummary(digitalWidgetCityClockColorPref.getEntry());
            digitalWidgetCityClockColorPref.setOnPreferenceChangeListener(this);

            final ListPreference digitalWidgetCityNameColorPref = findPreference(KEY_DIGITAL_WIDGET_CITY_NAME_COLOR);
            Objects.requireNonNull(digitalWidgetCityNameColorPref).setSummary(digitalWidgetCityNameColorPref.getEntry());
            digitalWidgetCityNameColorPref.setOnPreferenceChangeListener(this);

            final SpannableStringBuilder builderDigitalWidgetMessage = new SpannableStringBuilder();
            final String digitalWidgetMessage = requireContext().getString(R.string.settings_digital_widget_message);
            final Spannable spannableDigitalWidgetMessage = new SpannableString(digitalWidgetMessage);
            if (digitalWidgetMessage != null) {
                spannableDigitalWidgetMessage.setSpan(new StyleSpan(Typeface.ITALIC), 0, digitalWidgetMessage.length(), 0);
                spannableDigitalWidgetMessage.setSpan(new StyleSpan(Typeface.BOLD), 0, digitalWidgetMessage.length(), 0);
            }
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

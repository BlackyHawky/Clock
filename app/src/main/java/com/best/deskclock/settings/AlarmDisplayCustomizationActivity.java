package com.best.deskclock.settings;

import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AMOLED_DARK_MODE;

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
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.rarepebble.colorpicker.ColorPreference;

public class AlarmDisplayCustomizationActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "alarm_display_fragment";

    public static final String KEY_ALARM_CLOCK_STYLE = "key_alarm_clock_style";
    public static final String KEY_DISPLAY_ALARM_SECONDS_HAND = "key_display_alarm_seconds_hand";
    public static final String KEY_ALARM_BACKGROUND_COLOR = "key_alarm_background_color";
    public static final String KEY_ALARM_BACKGROUND_AMOLED_COLOR = "key_alarm_background_amoled_color";
    public static final String KEY_ALARM_CLOCK_COLOR = "key_alarm_clock_color";
    public static final String KEY_ALARM_SECONDS_HAND_COLOR = "key_alarm_seconds_hand_color";
    public static final String KEY_ALARM_TITLE_COLOR = "key_alarm_title_color";
    public static final String KEY_SNOOZE_BUTTON_COLOR = "key_snooze_button_color";
    public static final String KEY_DISMISS_BUTTON_COLOR = "key_dismiss_button_color";
    public static final String KEY_ALARM_BUTTON_COLOR = "key_alarm_button_color";
    public static final String KEY_PULSE_COLOR = "key_pulse_color";
    public static final String KEY_ALARM_CLOCK_FONT_SIZE = "key_alarm_clock_font_size";
    public static final String DEFAULT_ALARM_CLOCK_FONT_SIZE = "70";
    public static final String KEY_ALARM_TITLE_FONT_SIZE = "key_alarm_title_font_size";
    public static final String DEFAULT_ALARM_TITLE_FONT_SIZE = "26";
    public static final String KEY_DISPLAY_RINGTONE_TITLE = "key_display_ringtone_title";
    public static final String KEY_PREVIEW_ALARM = "key_preview_alarm";

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

    public static class PrefsFragment extends ScreenFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

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

            refresh();
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
            final boolean isAmoledMode = Utils.isNight(requireContext().getResources())
                    && getDarkMode.equals(KEY_AMOLED_DARK_MODE);
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
}

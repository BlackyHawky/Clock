package com.best.deskclock.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.rarepebble.colorpicker.ColorPreference;

import java.util.Objects;

public class AlarmDisplayCustomizationActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "alarm_display_fragment";

    public static final String KEY_ALARM_CLOCK_STYLE = "key_alarm_clock_style";
    public static final String KEY_DISPLAY_ALARM_SECONDS_HAND = "key_display_alarm_seconds_hand";
    public static final String KEY_ALARM_BACKGROUND_COLOR = "key_alarm_background_color";
    public static final String KEY_ALARM_CLOCK_COLOR = "key_alarm_clock_color";
    public static final String KEY_ALARM_SECONDS_HAND_COLOR = "key_alarm_seconds_hand_color";
    public static final String KEY_ALARM_TITLE_COLOR = "key_alarm_title_color";
    public static final String KEY_SNOOZE_BUTTON_COLOR = "key_snooze_button_color";
    public static final String KEY_DISMISS_BUTTON_COLOR = "key_dismiss_button_color";
    public static final String KEY_ALARM_BUTTON_COLOR = "key_alarm_button_color";
    public static final String KEY_PULSE_COLOR = "key_pulse_color";
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

    public static class PrefsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ListPreference mAlarmClockStyle;
        String[] mAlarmClockStyleValues;
        String mAnalogClock;
        SwitchPreferenceCompat mDisplaySecondsPref;
        ColorPreference mAlarmSecondsHandColorPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }

            mAlarmClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mAnalogClock = mAlarmClockStyleValues[0];
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            addPreferencesFromResource(R.xml.settings_alarm_display);

            setupPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();
            int bottomPadding = Utils.toPixel(20, requireContext());
            getListView().setPadding(0, 0, 0, bottomPadding);

            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_ALARM_CLOCK_STYLE -> {
                    final int clockIndex = mAlarmClockStyle.findIndexOfValue((String) newValue);
                    mAlarmClockStyle.setSummary(mAlarmClockStyle.getEntries()[clockIndex]);
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
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

            final Preference previewAlarmPref = findPreference(KEY_PREVIEW_ALARM);
            Objects.requireNonNull(previewAlarmPref).setOnPreferenceClickListener(this);
        }

        private void setupPreferences() {
            mAlarmClockStyle = findPreference(KEY_ALARM_CLOCK_STYLE);
            mDisplaySecondsPref = findPreference(KEY_DISPLAY_ALARM_SECONDS_HAND);
            mAlarmSecondsHandColorPref = findPreference(KEY_ALARM_SECONDS_HAND_COLOR);
            final int clockStyleIndex = mAlarmClockStyle.findIndexOfValue(DataModel.getDataModel()
                    .getAlarmClockStyle().toString().toLowerCase());
            // clockStyleIndex == 0 --> analog
            mDisplaySecondsPref.setVisible(clockStyleIndex == 0);
            mDisplaySecondsPref.setChecked(DataModel.getDataModel().isAlarmSecondsHandDisplayed());
            mAlarmSecondsHandColorPref.setVisible(clockStyleIndex == 0 && mDisplaySecondsPref.isChecked()
            );
        }
    }
}

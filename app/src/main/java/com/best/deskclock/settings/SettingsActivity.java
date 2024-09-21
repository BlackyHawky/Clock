/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

/**
 * Application settings
 */
public final class SettingsActivity extends CollapsingToolbarBaseActivity {

    public static final String PREFS_FRAGMENT_TAG = "settings_prefs_fragment";

    public static final String KEY_PERMISSION_MESSAGE = "key_permission_message";
    public static final String KEY_INTERFACE_CUSTOMIZATION = "key_interface_customization";
    public static final String KEY_CLOCK_SETTINGS = "key_clock_settings";
    public static final String KEY_ALARM_SETTINGS = "key_alarm_settings";
    public static final String KEY_TIMER_SETTINGS = "key_timer_settings";
    public static final String KEY_SCREENSAVER_SETTINGS = "key_screensaver_settings";
    public static final String KEY_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_digital_widget_customization";
    public static final String KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION =
            "key_digital_widget_material_you_customization";
    public static final String KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_material_you_vertical_digital_widget_customization";
    public static final String KEY_PERMISSIONS_MANAGEMENT = "key_permissions_management";

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

        public static final int REQUEST_CHANGE_SETTINGS = 10;

        Preference mInterfaceCustomizationPref;
        Preference mClockSettingsPref;
        Preference mAlarmSettingsPref;
        Preference mTimerSettingsPref;
        Preference mScreensaverSettings;
        Preference mDigitalWidgetCustomizationPref;
        Preference mDigitalWidgetMaterialYouCustomizationPref;
        Preference mMaterialYouVerticalDigitalWidgetCustomizationPref;
        Preference mPermissionsManagement;
        Preference mPermissionMessage;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);

            mInterfaceCustomizationPref = findPreference(KEY_INTERFACE_CUSTOMIZATION);
            mClockSettingsPref = findPreference(KEY_CLOCK_SETTINGS);
            mAlarmSettingsPref = findPreference(KEY_ALARM_SETTINGS);
            mTimerSettingsPref = findPreference(KEY_TIMER_SETTINGS);
            mScreensaverSettings = findPreference(KEY_SCREENSAVER_SETTINGS);
            mDigitalWidgetCustomizationPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZATION);
            mDigitalWidgetMaterialYouCustomizationPref = findPreference(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION);
            mMaterialYouVerticalDigitalWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION);
            mPermissionsManagement = findPreference(KEY_PERMISSIONS_MANAGEMENT);
            mPermissionMessage = findPreference(KEY_PERMISSION_MESSAGE);

            hidePreferences();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // By default, do not recreate the DeskClock activity
            requireActivity().setResult(RESULT_CANCELED);
        }

        @Override
        public void onResume() {
            super.onResume();

            refresh();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_CHANGE_SETTINGS && resultCode == RESULT_OK) {
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(RESULT_OK);
            }
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            final Context context = getActivity();
            if (context == null) {
                return false;
            }

            switch (pref.getKey()) {
                case KEY_INTERFACE_CUSTOMIZATION -> {
                    final Intent interfaceCustomizationIntent =
                            new Intent(context, InterfaceCustomizationActivity.class);
                    startActivityForResult(interfaceCustomizationIntent, REQUEST_CHANGE_SETTINGS);
                    return true;
                }

                case KEY_CLOCK_SETTINGS -> {
                    final Intent clockSettingsIntent = new Intent(context, ClockSettingsActivity.class);
                    startActivityForResult(clockSettingsIntent, REQUEST_CHANGE_SETTINGS);
                    return true;
                }

                case KEY_ALARM_SETTINGS -> {
                    final Intent alarmSettingsIntent = new Intent(context, AlarmSettingsActivity.class);
                    startActivityForResult(alarmSettingsIntent, REQUEST_CHANGE_SETTINGS);
                    return true;
                }

                case KEY_TIMER_SETTINGS -> {
                    final Intent timerSettingsIntent = new Intent(context, TimerSettingsActivity.class);
                    startActivity(timerSettingsIntent);
                    return true;
                }

                case KEY_SCREENSAVER_SETTINGS -> {
                    final Intent screensaverSettingsIntent =
                            new Intent(context, ScreensaverSettingsActivity.class);
                    startActivity(screensaverSettingsIntent);
                    return true;
                }

                case KEY_DIGITAL_WIDGET_CUSTOMIZATION -> {
                    final Intent digitalWidgetCustomizationIntent =
                            new Intent(context, DigitalWidgetCustomizationActivity.class);
                    startActivity(digitalWidgetCustomizationIntent);
                    return true;
                }

                case KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION -> {
                    final Intent digitalWidgetMaterialYouCustomizationIntent =
                            new Intent(context, DigitalWidgetMaterialYouCustomizationActivity.class);
                    startActivity(digitalWidgetMaterialYouCustomizationIntent);
                    return true;
                }

                case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION -> {
                    final Intent materialYouVerticalDigitalWidgetCustomizationIntent =
                            new Intent(context, MaterialYouVerticalDigitalWidgetCustomizationActivity.class);
                    startActivity(materialYouVerticalDigitalWidgetCustomizationIntent);
                    return true;
                }

                case KEY_PERMISSION_MESSAGE, KEY_PERMISSIONS_MANAGEMENT -> {
                    final Intent permissionsManagementIntent =
                            new Intent(context, PermissionsManagementActivity.class);
                    startActivity(permissionsManagementIntent);
                    return true;
                }
            }

            return false;
        }

        private void hidePreferences() {
            mPermissionMessage.setVisible(
                        PermissionsManagementActivity.areEssentialPermissionsNotGranted(requireContext())
            );
        }

        private void refresh() {
            mPermissionMessage.setVisible(
                    PermissionsManagementActivity.areEssentialPermissionsNotGranted(requireContext())
            );
            if (mPermissionMessage.isShown()) {
                final SpannableStringBuilder builderPermissionMessage = new SpannableStringBuilder();
                final String messagePermission = requireContext().getString(R.string.settings_permission_message);
                final Spannable spannableMessagePermission = new SpannableString(messagePermission);
                spannableMessagePermission.setSpan(
                        new ForegroundColorSpan(Color.RED), 0, messagePermission.length(), 0);
                spannableMessagePermission.setSpan(
                        new StyleSpan(Typeface.BOLD), 0, messagePermission.length(), 0);
                builderPermissionMessage.append(spannableMessagePermission);
                mPermissionMessage.setTitle(builderPermissionMessage);
                mPermissionMessage.setOnPreferenceClickListener(this);
            }

            mInterfaceCustomizationPref.setOnPreferenceClickListener(this);

            mClockSettingsPref.setOnPreferenceClickListener(this);

            mAlarmSettingsPref.setOnPreferenceClickListener(this);

            mTimerSettingsPref.setOnPreferenceClickListener(this);

            mScreensaverSettings.setOnPreferenceClickListener(this);

            mDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

            mDigitalWidgetMaterialYouCustomizationPref.setOnPreferenceClickListener(this);

            mMaterialYouVerticalDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

            mPermissionsManagement.setOnPreferenceClickListener(this);
        }
    }
}

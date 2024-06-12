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
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.Objects;

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
    public static final String KEY_SS_SETTINGS = "screensaver_settings";
    public static final String KEY_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_digital_widget_customization";
    public static final String KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION =
            "key_digital_widget_material_you_customization";
    public static final String KEY_PERMISSIONS_MANAGEMENT = "permissions_management";

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

    public static class PrefsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

        Preference mPermissionMessage;

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.settings);
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
            int topAndBottomPadding = Utils.toPixel(20, requireContext());
            getListView().setPadding(0, topAndBottomPadding, 0, topAndBottomPadding);

            refresh();
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
                    startActivity(interfaceCustomizationIntent);
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
                    return true;
                }

                case KEY_CLOCK_SETTINGS -> {
                    final Intent clockSettingsIntent = new Intent(context, ClockSettingsActivity.class);
                    startActivity(clockSettingsIntent);
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
                    return true;
                }

                case KEY_ALARM_SETTINGS -> {
                    final Intent alarmSettingsIntent = new Intent(context, AlarmSettingsActivity.class);
                    startActivity(alarmSettingsIntent);
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
                    return true;
                }

                case KEY_TIMER_SETTINGS -> {
                    final Intent timerSettingsIntent = new Intent(context, TimerSettingsActivity.class);
                    startActivity(timerSettingsIntent);
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
                    return true;
                }

                case KEY_SS_SETTINGS -> {
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
            mPermissionMessage = findPreference(KEY_PERMISSION_MESSAGE);

            if (mPermissionMessage != null) {
                mPermissionMessage.setVisible(
                        PermissionsManagementActivity.areEssentialPermissionsNotGranted(requireContext())
                );
            }
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

            final Preference interfaceCustomizationPref = findPreference(KEY_INTERFACE_CUSTOMIZATION);
            Objects.requireNonNull(interfaceCustomizationPref).setOnPreferenceClickListener(this);

            final Preference clockSettingsPref = findPreference(KEY_CLOCK_SETTINGS);
            Objects.requireNonNull(clockSettingsPref).setOnPreferenceClickListener(this);

            final Preference alarmSettingsPref = findPreference(KEY_ALARM_SETTINGS);
            Objects.requireNonNull(alarmSettingsPref).setOnPreferenceClickListener(this);

            final Preference timerSettingsPref = findPreference(KEY_TIMER_SETTINGS);
            Objects.requireNonNull(timerSettingsPref).setOnPreferenceClickListener(this);

            final Preference screensaverSettings = findPreference(KEY_SS_SETTINGS);
            Objects.requireNonNull(screensaverSettings).setOnPreferenceClickListener(this);

            final Preference digitalWidgetCustomizationPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZATION);
            Objects.requireNonNull(digitalWidgetCustomizationPref).setOnPreferenceClickListener(this);

            final Preference digitalWidgetMaterialYouCustomizationPref =
                    findPreference(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION);
            Objects.requireNonNull(digitalWidgetMaterialYouCustomizationPref).setOnPreferenceClickListener(this);

            final Preference permissionsManagement = findPreference(KEY_PERMISSIONS_MANAGEMENT);
            Objects.requireNonNull(permissionsManagement).setOnPreferenceClickListener(this);
        }
    }
}

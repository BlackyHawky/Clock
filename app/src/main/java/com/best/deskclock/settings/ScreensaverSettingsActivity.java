/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

/**
 * Settings for Clock screensaver
 */
public final class ScreensaverSettingsActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected String getActivityTitle() {
        // Already defined in the fragment.
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new ScreensaverSettingsFragment())
                .disallowAddToBackStack()
                .commit();
        }
    }

    public static class ScreensaverSettingsFragment extends BaseSettingsScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ListPreference mClockStylePref;
        ListPreference mClockDialPref;
        ListPreference mClockDialMaterialPref;
        ListPreference mClockSecondHandPref;
        SwitchPreferenceCompat mDisplaySecondsPref;
        SwitchPreferenceCompat mDisplayNextAlarmPref;
        SwitchPreferenceCompat mDisplayBatteryPref;
        SwitchPreferenceCompat mDisplayTextUppercasePref;
        SwitchPreferenceCompat mBoldDigitalClockPref;
        SwitchPreferenceCompat mClockDynamicColorPref;
        SwitchPreferenceCompat mItalicDigitalClockPref;
        SwitchPreferenceCompat mBoldBatteryPref;
        SwitchPreferenceCompat mItalicBatteryPref;
        SwitchPreferenceCompat mBoldDatePref;
        SwitchPreferenceCompat mItalicDatePref;
        SwitchPreferenceCompat mBoldNextAlarmPref;
        SwitchPreferenceCompat mItalicNextAlarmPref;
        Preference mDigitalClockFontPref;
        SwitchPreferenceCompat mKeepScreenOnPref;
        Preference mScreensaverBackgroundImagePref;
        CustomSliderPreference mScreensaverBlurIntensityPref;
        Preference mScreensaverPreviewPref;
        Preference mScreensaverMainSettingsPref;

        String[] mClockStyleValues;
        String mAnalogClock;
        String mMaterialAnalogClock;
        String mDigitalClock;

        private final ActivityResultLauncher<Intent> fontPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (isAdded() && mIsSearchMode) {
                    blockSearchResetForAWhile();
                }

                if (result.getResultCode() != RESULT_OK) {
                    return;
                }

                Intent intent = result.getData();
                final Uri sourceUri = intent == null ? null : intent.getData();
                if (sourceUri == null) {
                    return;
                }

                final Context appContext = requireContext().getApplicationContext();
                final int style = getAccentStyle();
                final Typeface font = getGeneralTypeface();
                final SharedPreferences prefs = getPrefs();

                // Take persistent permission
                appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                String safeTitle = FileUtils.toSafeFileName(FILE_SCREENSAVER_DIGITAL_CLOCK_FONT);
                String oldFontPath = prefs.getString(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, null);

                AppExecutors.getDiskIO().execute(() -> {
                    // Delete the old font if it exists
                    FileUtils.clearFile(oldFontPath);

                    // Clear the font cache
                    ThemeUtils.removeFontFromCache(oldFontPath);

                    // Copy the new font to the device's protected storage
                    Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                    // Save the new path
                    if (copiedUri != null) {
                        prefs.edit().putString(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, copiedUri.getPath()).apply();
                    }

                    AppExecutors.getMainThread().post(() -> {
                        if (copiedUri != null) {
                            CustomToast.show(appContext, style, font, R.string.custom_font_toast_message_selected);
                        } else {
                            CustomToast.show(appContext, style, font, R.string.font_message_error);
                        }

                        if (!isAdded() || mDigitalClockFontPref == null) {
                            return;
                        }

                        if (copiedUri != null) {
                            mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title_variant));
                        } else {
                            mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title));
                        }
                    });
                });
            });

        private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (isAdded() && mIsSearchMode) {
                    blockSearchResetForAWhile();
                }

                if (result.getResultCode() != RESULT_OK) {
                    return;
                }

                Intent intent = result.getData();
                final Uri sourceUri = intent == null ? null : intent.getData();
                if (sourceUri == null) {
                    return;
                }

                final Context appContext = requireContext().getApplicationContext();
                final int style = getAccentStyle();
                final Typeface font = getGeneralTypeface();
                final SharedPreferences prefs = getPrefs();

                // Take persistent permission
                appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                String safeTitle = FileUtils.toSafeFileName(FILE_SCREENSAVER_BACKGROUND);
                String oldImagePath = prefs.getString(KEY_SCREENSAVER_BACKGROUND_IMAGE, null);

                AppExecutors.getDiskIO().execute(() -> {
                    // Delete the old image if it exists
                    FileUtils.clearFile(oldImagePath);

                    // Copy the new image to the device's protected storage
                    Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                    AppExecutors.getMainThread().post(() -> {
                        if (!isAdded()
                            || mScreensaverBackgroundImagePref == null
                            || mScreensaverBlurIntensityPref == null) {
                            return;
                        }

                        // Save the new path
                        if (copiedUri != null) {
                            prefs.edit().putString(KEY_SCREENSAVER_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                            mScreensaverBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                            CustomToast.show(appContext, style, font, R.string.background_image_toast_message_selected);
                        } else {
                            CustomToast.show(appContext, style, font, R.string.image_message_error);
                            mScreensaverBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                        }

                        invalidatePreferencesVisibility();
                    });
                });
            });

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.screensaver_settings_title);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_screensaver);

            mClockStylePref = findPreference(KEY_SCREENSAVER_CLOCK_STYLE);
            mClockDialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL);
            mClockDialMaterialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL);
            mDigitalClockFontPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT);
            mDisplaySecondsPref = findPreference(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS);
            mClockSecondHandPref = findPreference(KEY_SCREENSAVER_CLOCK_SECOND_HAND);
            mDisplayNextAlarmPref = findPreference(KEY_SCREENSAVER_DISPLAY_NEXT_ALARM);
            mDisplayBatteryPref = findPreference(KEY_DISPLAY_SCREENSAVER_BATTERY);
            mClockDynamicColorPref = findPreference(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS);
            mDisplayTextUppercasePref = findPreference(KEY_SCREENSAVER_DISPLAY_TEXT_UPPERCASE);
            mBoldDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD);
            mItalicDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC);
            mBoldBatteryPref = findPreference(KEY_SCREENSAVER_BATTERY_IN_BOLD);
            mItalicBatteryPref = findPreference(KEY_SCREENSAVER_BATTERY_IN_ITALIC);
            mBoldDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_BOLD);
            mItalicDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_ITALIC);
            mBoldNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD);
            mItalicNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC);
            mKeepScreenOnPref = findPreference(KEY_SCREENSAVER_KEEP_SCREEN_ON);
            mScreensaverBackgroundImagePref = findPreference(KEY_SCREENSAVER_BACKGROUND_IMAGE);
            mScreensaverBlurIntensityPref = findPreference(KEY_SCREENSAVER_BLUR_INTENSITY);
            mScreensaverPreviewPref = findPreference(KEY_SCREENSAVER_PREVIEW);
            mScreensaverMainSettingsPref = findPreference(KEY_SCREENSAVER_DAYDREAM_SETTINGS);

            mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mAnalogClock = mClockStyleValues[0];
            mMaterialAnalogClock = mClockStyleValues[1];
            mDigitalClock = mClockStyleValues[2];

            setupPreferences();

            invalidatePreferencesVisibility();
        }

        @Override
        public void onResume() {
            super.onResume();

            restoreCustomFileDialogIfNeeded(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, mDigitalClockFontPref, fontPickerLauncher, null);

            restoreCustomFileDialogIfNeeded(KEY_SCREENSAVER_BACKGROUND_IMAGE, mScreensaverBackgroundImagePref, imagePickerLauncher,
                this::invalidatePreferencesVisibility);

            invalidatePreferencesVisibility();
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
            switch (pref.getKey()) {
                case KEY_SCREENSAVER_CLOCK_STYLE, KEY_SCREENSAVER_CLOCK_DIAL, KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL,
                     KEY_SCREENSAVER_CLOCK_SECOND_HAND -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_SCREENSAVER_DISPLAY_NEXT_ALARM, KEY_DISPLAY_SCREENSAVER_BATTERY, KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS,
                     KEY_SCREENSAVER_DISPLAY_TEXT_UPPERCASE, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                     KEY_SCREENSAVER_BATTERY_IN_BOLD, KEY_SCREENSAVER_BATTERY_IN_ITALIC, KEY_SCREENSAVER_DATE_IN_BOLD,
                     KEY_SCREENSAVER_DATE_IN_ITALIC, KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC,
                     KEY_SCREENSAVER_KEEP_SCREEN_ON, KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS ->
                    Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            blockSearchResetForAWhile();

            if (getView() != null) {
                getView().post(() -> {
                    if (isAdded()) {
                        invalidatePreferencesVisibility();
                    }
                });
            }

            return true;
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            final Context context = requireActivity();

            switch (pref.getKey()) {
                case KEY_SCREENSAVER_PREVIEW -> context.startActivity(
                    new Intent(context, ScreensaverActivity.class).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));

                case KEY_SCREENSAVER_DAYDREAM_SETTINGS -> {
                    final Intent dialogSSMainSettingsIntent = new Intent(Settings.ACTION_DREAM_SETTINGS);
                    dialogSSMainSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogSSMainSettingsIntent);
                }

                case KEY_SCREENSAVER_DIGITAL_CLOCK_FONT -> selectCustomFile(mDigitalClockFontPref, fontPickerLauncher,
                    SettingsDAO.getScreensaverDigitalClockFont(getPrefs()), KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, true, false, null);

                case KEY_SCREENSAVER_BACKGROUND_IMAGE -> selectCustomFile(mScreensaverBackgroundImagePref, imagePickerLauncher,
                    SettingsDAO.getScreensaverBackgroundImage(getPrefs()), KEY_SCREENSAVER_BACKGROUND_IMAGE, false, false,
                    this::invalidatePreferencesVisibility);
            }

            return true;
        }

        @Override
        protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
            final boolean isAnalogClock = mClockStylePref.getValue().equals(mAnalogClock);
            final boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
            final boolean isDigitalClock = mClockStylePref.getValue().equals(mDigitalClock);

            final boolean isNextAlarmDisplayed = mDisplayNextAlarmPref.isChecked();
            final boolean isBatteryDisplayed = mDisplayBatteryPref.isChecked();

            final boolean areDynamicColors = mClockDynamicColorPref != null && mClockDynamicColorPref.isChecked();
            final boolean displayGeneralColors = !SdkUtils.isAtLeastAndroid12() || !areDynamicColors || isMaterialAnalogClock;

            return switch (key) {
                case KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS -> SdkUtils.isAtLeastAndroid12() && !isMaterialAnalogClock;

                case KEY_SCREENSAVER_CLOCK_COLOR_PICKER -> !isMaterialAnalogClock && (!SdkUtils.isAtLeastAndroid12() || !areDynamicColors);

                case KEY_SCREENSAVER_DATE_COLOR_PICKER -> displayGeneralColors;

                case KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER -> isNextAlarmDisplayed && displayGeneralColors;

                case KEY_SCREENSAVER_BATTERY_COLOR_PICKER -> isBatteryDisplayed && displayGeneralColors;

                case KEY_SCREENSAVER_CLOCK_DIAL -> isAnalogClock;

                case KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL -> isMaterialAnalogClock;

                case KEY_SCREENSAVER_ANALOG_CLOCK_SIZE -> !isDigitalClock;

                case KEY_SCREENSAVER_CLOCK_SECOND_HAND -> isAnalogClock && mDisplaySecondsPref.isChecked();

                case KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD,
                     KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC -> isDigitalClock;

                case KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC -> isNextAlarmDisplayed;

                case KEY_SCREENSAVER_BATTERY_IN_BOLD, KEY_SCREENSAVER_BATTERY_IN_ITALIC -> isBatteryDisplayed;

                case KEY_SCREENSAVER_BLUR_INTENSITY -> SdkUtils.isAtLeastAndroid12()
                    && SettingsDAO.getScreensaverBackgroundImage(getPrefs()) != null;

                default -> super.isPreferenceVisible(pref, key);
            };
        }

        private void setupPreferences() {
            mClockStylePref.setSummary(mClockStylePref.getEntry());
            mClockStylePref.setOnPreferenceChangeListener(this);

            mClockDialPref.setSummary(mClockDialPref.getEntry());
            mClockDialPref.setOnPreferenceChangeListener(this);

            mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
            mClockDialMaterialPref.setOnPreferenceChangeListener(this);

            mDigitalClockFontPref.setTitle(getString(SettingsDAO.getScreensaverDigitalClockFont(getPrefs()) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
            mDigitalClockFontPref.setOnPreferenceClickListener(this);

            mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);
            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
            mClockSecondHandPref.setOnPreferenceChangeListener(this);

            mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);
            mDisplayBatteryPref.setOnPreferenceChangeListener(this);

            if (SdkUtils.isAtLeastAndroid12()) {
                mClockDynamicColorPref.setOnPreferenceChangeListener(this);
            }

            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicBatteryPref.setOnPreferenceChangeListener(this);

            mBoldBatteryPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            mBoldDatePref.setOnPreferenceChangeListener(this);

            mItalicDatePref.setOnPreferenceChangeListener(this);

            mBoldNextAlarmPref.setOnPreferenceChangeListener(this);

            mItalicNextAlarmPref.setOnPreferenceChangeListener(this);

            mKeepScreenOnPref.setOnPreferenceChangeListener(this);

            final String screensaverBackgroundImage = SettingsDAO.getScreensaverBackgroundImage(getPrefs());
            mScreensaverBackgroundImagePref.setTitle(getString(screensaverBackgroundImage == null
                ? R.string.background_image_title
                : R.string.background_image_title_variant));
            mScreensaverBackgroundImagePref.setOnPreferenceClickListener(this);

            mScreensaverPreviewPref.setOnPreferenceClickListener(this);

            mScreensaverMainSettingsPref.setOnPreferenceClickListener(this);
        }

    }

}

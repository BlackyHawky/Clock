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
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
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

        ColorPickerPreference mClockColorPref;
        ColorPickerPreference mBatteryColorPref;
        ColorPickerPreference mDateColorPref;
        ColorPickerPreference mNextAlarmColorPref;
        ListPreference mClockStylePref;
        ListPreference mClockDialPref;
        ListPreference mClockDialMaterialPref;
        ListPreference mClockSecondHandPref;
        SwitchPreferenceCompat mDisplaySecondsPref;
        SwitchPreferenceCompat mDisplayNextAlarmPref;
        SwitchPreferenceCompat mDisplayBatteryPref;
        CustomSliderPreference mDigitalClockFontSizePref;
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
        CustomSliderPreference mAnalogClockSizePref;
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
                            mScreensaverBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12());

                            CustomToast.show(appContext, style, font, R.string.background_image_toast_message_selected);
                        } else {
                            CustomToast.show(appContext, style, font, R.string.image_message_error);
                            mScreensaverBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                            mScreensaverBlurIntensityPref.setVisible(false);
                        }
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
            mAnalogClockSizePref = findPreference(KEY_SCREENSAVER_ANALOG_CLOCK_SIZE);
            mDisplaySecondsPref = findPreference(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS);
            mClockSecondHandPref = findPreference(KEY_SCREENSAVER_CLOCK_SECOND_HAND);
            mDisplayNextAlarmPref = findPreference(KEY_SCREENSAVER_DISPLAY_NEXT_ALARM);
            mDisplayBatteryPref = findPreference(KEY_DISPLAY_SCREENSAVER_BATTERY);
            mClockDynamicColorPref = findPreference(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS);
            mClockColorPref = findPreference(KEY_SCREENSAVER_CLOCK_COLOR_PICKER);
            mBatteryColorPref = findPreference(KEY_SCREENSAVER_BATTERY_COLOR_PICKER);
            mDateColorPref = findPreference(KEY_SCREENSAVER_DATE_COLOR_PICKER);
            mNextAlarmColorPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER);
            mDigitalClockFontSizePref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE);
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
        }

        @Override
        public void onResume() {
            super.onResume();

            restoreCustomFileDialogIfNeeded(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, mDigitalClockFontPref, fontPickerLauncher, null);

            restoreCustomFileDialogIfNeeded(KEY_SCREENSAVER_BACKGROUND_IMAGE, mScreensaverBackgroundImagePref, imagePickerLauncher, () ->
                mScreensaverBlurIntensityPref.setVisible(false));
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
            switch (pref.getKey()) {
                case KEY_SCREENSAVER_CLOCK_STYLE -> {
                    final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                    mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);

                    boolean isAnalogClock = newValue.equals(mAnalogClock);
                    boolean isMaterialAnalogClock = newValue.equals(mMaterialAnalogClock);
                    boolean isDigitalClock = newValue.equals(mDigitalClock);
                    boolean areDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(getPrefs());
                    boolean isNextAlarmDisplayed = SettingsDAO.isScreensaverNextAlarmDisplayed(getPrefs());
                    boolean isBatteryDisplayed = SettingsDAO.isScreensaverBatteryDisplayed(getPrefs());
                    boolean displayGeneralColors = !SdkUtils.isAtLeastAndroid12() || !areDynamicColors || isMaterialAnalogClock;

                    if (SdkUtils.isAtLeastAndroid12()) {
                        mClockDynamicColorPref.setVisible(!isMaterialAnalogClock);
                    }

                    mClockColorPref.setVisible(!isMaterialAnalogClock && (!SdkUtils.isAtLeastAndroid12() || !areDynamicColors));
                    mDateColorPref.setVisible(displayGeneralColors);
                    mNextAlarmColorPref.setVisible(isNextAlarmDisplayed && displayGeneralColors);
                    mBatteryColorPref.setVisible(isBatteryDisplayed && displayGeneralColors);

                    mClockDialPref.setVisible(isAnalogClock);
                    mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
                    mAnalogClockSizePref.setVisible(!isDigitalClock);
                    mClockSecondHandPref.setVisible(isAnalogClock
                        && SettingsDAO.areScreensaverClockSecondsDisplayed(getPrefs()));
                    mDigitalClockFontSizePref.setVisible(isDigitalClock);
                    mBoldDigitalClockPref.setVisible(isDigitalClock);
                    mItalicDigitalClockPref.setVisible(isDigitalClock);
                }

                case KEY_SCREENSAVER_DISPLAY_NEXT_ALARM -> {
                    Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                    boolean isNextAlarmDisplayed = (boolean) newValue;
                    boolean areDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(getPrefs());
                    boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
                    boolean displayGeneralColors = !SdkUtils.isAtLeastAndroid12() || !areDynamicColors || isMaterialAnalogClock;

                    mNextAlarmColorPref.setVisible(isNextAlarmDisplayed && displayGeneralColors);
                    mBoldNextAlarmPref.setVisible(isNextAlarmDisplayed);
                    mItalicNextAlarmPref.setVisible(isNextAlarmDisplayed);
                }

                case KEY_DISPLAY_SCREENSAVER_BATTERY -> {
                    Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                    boolean isBatteryVisible = (boolean) newValue;
                    boolean areDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(getPrefs());
                    boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
                    boolean displayGeneralColors = !SdkUtils.isAtLeastAndroid12() || !areDynamicColors || isMaterialAnalogClock;

                    mBatteryColorPref.setVisible(isBatteryVisible && displayGeneralColors);
                    mBoldBatteryPref.setVisible(isBatteryVisible);
                    mItalicBatteryPref.setVisible(isBatteryVisible);
                }

                case KEY_SCREENSAVER_CLOCK_DIAL, KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL, KEY_SCREENSAVER_CLOCK_SECOND_HAND -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS -> {
                    Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                    mClockSecondHandPref.setVisible((boolean) newValue
                        && SettingsDAO.getScreensaverClockStyle(getPrefs()) == DataModel.ClockStyle.ANALOG);
                }

                case KEY_SCREENSAVER_DISPLAY_TEXT_UPPERCASE, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                     KEY_SCREENSAVER_BATTERY_IN_BOLD, KEY_SCREENSAVER_BATTERY_IN_ITALIC, KEY_SCREENSAVER_DATE_IN_BOLD,
                     KEY_SCREENSAVER_DATE_IN_ITALIC, KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC,
                     KEY_SCREENSAVER_KEEP_SCREEN_ON ->
                    Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                case KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS -> {
                    Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                    boolean areDynamicColors = (boolean) newValue;
                    boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
                    boolean isNextAlarmDisplayed = SettingsDAO.isScreensaverNextAlarmDisplayed(getPrefs());
                    boolean isBatteryDisplayed = SettingsDAO.isScreensaverBatteryDisplayed(getPrefs());
                    boolean displayGeneralColors = !areDynamicColors || isMaterialAnalogClock;

                    mClockColorPref.setVisible(!isMaterialAnalogClock && !areDynamicColors);
                    mDateColorPref.setVisible(displayGeneralColors);
                    mNextAlarmColorPref.setVisible(isNextAlarmDisplayed && displayGeneralColors);
                    mBatteryColorPref.setVisible(isBatteryDisplayed && displayGeneralColors);
                }
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
                    SettingsDAO.getScreensaverDigitalClockFont(getPrefs()), KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, true, null);

                case KEY_SCREENSAVER_BACKGROUND_IMAGE -> selectCustomFile(mScreensaverBackgroundImagePref, imagePickerLauncher,
                    SettingsDAO.getScreensaverBackgroundImage(getPrefs()), KEY_SCREENSAVER_BACKGROUND_IMAGE, false, () ->
                        mScreensaverBlurIntensityPref.setVisible(false));
            }

            return true;
        }

        private void setupPreferences() {
            final boolean isAnalogClock = mClockStylePref.getValue().equals(mAnalogClock);
            final boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
            final boolean isDigitalClock = mClockStylePref.getValue().equals(mDigitalClock);
            final boolean isNextAlarmDisplayed = SettingsDAO.isScreensaverNextAlarmDisplayed(getPrefs());
            final boolean isBatteryDisplayed = SettingsDAO.isScreensaverBatteryDisplayed(getPrefs());
            final String screensaverBackgroundImage = SettingsDAO.getScreensaverBackgroundImage(getPrefs());
            final boolean areDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(getPrefs());
            final boolean displayGeneralColors = !SdkUtils.isAtLeastAndroid12() || !areDynamicColors || isMaterialAnalogClock;

            mClockStylePref.setSummary(mClockStylePref.getEntry());
            mClockStylePref.setOnPreferenceChangeListener(this);

            mClockDialPref.setVisible(isAnalogClock);
            mClockDialPref.setSummary(mClockDialPref.getEntry());
            mClockDialPref.setOnPreferenceChangeListener(this);

            mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
            mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
            mClockDialMaterialPref.setOnPreferenceChangeListener(this);

            mDigitalClockFontPref.setTitle(getString(SettingsDAO.getScreensaverDigitalClockFont(getPrefs()) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
            mDigitalClockFontPref.setOnPreferenceClickListener(this);

            mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

            mAnalogClockSizePref.setVisible(!isDigitalClock);

            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mClockSecondHandPref.setVisible(isAnalogClock && SettingsDAO.areScreensaverClockSecondsDisplayed(getPrefs()));
            mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
            mClockSecondHandPref.setOnPreferenceChangeListener(this);

            mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

            mDisplayBatteryPref.setOnPreferenceChangeListener(this);

            if (SdkUtils.isAtLeastAndroid12()) {
                mClockDynamicColorPref.setVisible(!isMaterialAnalogClock);
                mClockDynamicColorPref.setOnPreferenceChangeListener(this);
            }

            mClockColorPref.setVisible(!isMaterialAnalogClock && (!SdkUtils.isAtLeastAndroid12() || !areDynamicColors));

            mDateColorPref.setVisible(displayGeneralColors);

            mNextAlarmColorPref.setVisible(isNextAlarmDisplayed && displayGeneralColors);

            mBatteryColorPref.setVisible(isBatteryDisplayed && displayGeneralColors);

            mDigitalClockFontSizePref.setVisible(isDigitalClock);

            mBoldDigitalClockPref.setVisible(isDigitalClock);

            mItalicDigitalClockPref.setVisible(isDigitalClock);

            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicBatteryPref.setVisible(isBatteryDisplayed);
            mItalicBatteryPref.setOnPreferenceChangeListener(this);

            mBoldBatteryPref.setVisible(isBatteryDisplayed);
            mBoldBatteryPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            mBoldDatePref.setOnPreferenceChangeListener(this);

            mItalicDatePref.setOnPreferenceChangeListener(this);

            mBoldNextAlarmPref.setVisible(isNextAlarmDisplayed);
            mBoldNextAlarmPref.setOnPreferenceChangeListener(this);

            mItalicNextAlarmPref.setVisible(isNextAlarmDisplayed);
            mItalicNextAlarmPref.setOnPreferenceChangeListener(this);

            mKeepScreenOnPref.setOnPreferenceChangeListener(this);

            mScreensaverBackgroundImagePref.setTitle(getString(screensaverBackgroundImage == null
                ? R.string.background_image_title
                : R.string.background_image_title_variant));
            mScreensaverBackgroundImagePref.setOnPreferenceClickListener(this);

            mScreensaverBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12() && screensaverBackgroundImage != null);

            mScreensaverPreviewPref.setOnPreferenceClickListener(this);

            mScreensaverMainSettingsPref.setOnPreferenceClickListener(this);
        }

    }

}

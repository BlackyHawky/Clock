/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_SCREENSAVER_BATTERY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_SCREENSAVER_BLUR_EFFECT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_ANALOG_CLOCK_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BACKGROUND_IMAGE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BATTERY_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BATTERY_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BATTERY_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DAYDREAM_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_PREVIEW;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomListPreference;
import com.best.deskclock.settings.custompreference.CustomPreference;
import com.best.deskclock.settings.custompreference.CustomSeekbarPreference;
import com.best.deskclock.settings.custompreference.CustomSwitchPreference;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.SdkUtils;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new ScreensaverSettingsFragment())
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class ScreensaverSettingsFragment extends ScreenFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ColorPickerPreference mClockColorPref;
        ColorPickerPreference mBatteryColorPref;
        ColorPickerPreference mDateColorPref;
        ColorPickerPreference mNextAlarmColorPref;
        CustomListPreference mClockStylePref;
        CustomListPreference mClockDialPref;
        CustomListPreference mClockDialMaterialPref;
        CustomListPreference mClockSecondHandPref;
        CustomSwitchPreference mDisplaySecondsPref;
        CustomSwitchPreference mDisplayBatteryPref;
        CustomSeekbarPreference mDigitalClockFontSizePref;
        CustomSwitchPreference mBoldDigitalClockPref;
        CustomSwitchPreference mClockDynamicColorPref;
        CustomSwitchPreference mItalicDigitalClockPref;
        CustomSwitchPreference mBoldBatteryPref;
        CustomSwitchPreference mItalicBatteryPref;
        CustomSwitchPreference mBoldDatePref;
        CustomSwitchPreference mItalicDatePref;
        CustomSwitchPreference mBoldNextAlarmPref;
        CustomSwitchPreference mItalicNextAlarmPref;
        CustomSeekbarPreference mAnalogClockSizePref;
        CustomPreference mDigitalClockFontPref;
        CustomPreference mScreensaverBackgroundImagePref;
        CustomSwitchPreference mEnableScreensaverBlurEffectPref;
        CustomSeekbarPreference mScreensaverBlurIntensityPref;
        CustomPreference mScreensaverPreview;
        CustomPreference mScreensaverMainSettings;

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

                    // Take persistent permission
                    requireActivity().getContentResolver().takePersistableUriPermission(
                            sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

                    String safeTitle = Utils.toSafeFileName("screensaver_digital_clock_font");

                    // Delete the old font if it exists
                    clearFile(mPrefs.getString(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, null));

                    Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                    // Save the new path
                    if (copiedUri != null) {
                        mPrefs.edit().putString(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, copiedUri.getPath()).apply();
                        mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title_variant));

                        CustomToast.show(requireContext(), R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(requireContext(), "Error importing font");
                        mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });

        private final ActivityResultLauncher<Intent> imagePickerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK ) {
                        return;
                    }

                    Intent intent = result.getData();
                    final Uri sourceUri = intent == null ? null : intent.getData();
                    if (sourceUri == null) {
                        return;
                    }

                    // Take persistent permission
                    requireActivity().getContentResolver().takePersistableUriPermission(
                            sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

                    String safeTitle = Utils.toSafeFileName("screensaver_background");

                    // Delete the old image if it exists
                    clearFile(mPrefs.getString(KEY_SCREENSAVER_BACKGROUND_IMAGE, null));

                    // Copy the new image to the device's protected storage
                    Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                    // Save the new path
                    if (copiedUri != null) {
                        mPrefs.edit().putString(KEY_SCREENSAVER_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                        mScreensaverBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                        mEnableScreensaverBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12());
                        mScreensaverBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                                && SettingsDAO.isScreensaverBlurEffectEnabled(mPrefs));

                        CustomToast.show(requireContext(), R.string.background_image_toast_message_selected);
                    } else {
                        CustomToast.show(requireContext(), "Error importing image");
                        mScreensaverBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                        mEnableScreensaverBlurEffectPref.setVisible(false);
                        mScreensaverBlurIntensityPref.setVisible(false);
                    }
                });

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.screensaver_settings_title);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_screensaver);

            mClockStylePref = findPreference(KEY_SCREENSAVER_CLOCK_STYLE);
            mClockDialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL);
            mClockDialMaterialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL);
            mDigitalClockFontPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT);
            mAnalogClockSizePref = findPreference(KEY_SCREENSAVER_ANALOG_CLOCK_SIZE);
            mDisplaySecondsPref = findPreference(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS);
            mClockSecondHandPref = findPreference(KEY_SCREENSAVER_CLOCK_SECOND_HAND);
            mDisplayBatteryPref = findPreference(KEY_DISPLAY_SCREENSAVER_BATTERY);
            mClockDynamicColorPref = findPreference(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS);
            mClockColorPref = findPreference(KEY_SCREENSAVER_CLOCK_COLOR_PICKER);
            mBatteryColorPref = findPreference(KEY_SCREENSAVER_BATTERY_COLOR_PICKER);
            mDateColorPref = findPreference(KEY_SCREENSAVER_DATE_COLOR_PICKER);
            mNextAlarmColorPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER);
            mDigitalClockFontSizePref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE);
            mBoldDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD);
            mItalicDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC);
            mBoldBatteryPref = findPreference(KEY_SCREENSAVER_BATTERY_IN_BOLD);
            mItalicBatteryPref = findPreference(KEY_SCREENSAVER_BATTERY_IN_ITALIC);
            mBoldDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_BOLD);
            mItalicDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_ITALIC);
            mBoldNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD);
            mItalicNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC);
            mScreensaverBackgroundImagePref = findPreference(KEY_SCREENSAVER_BACKGROUND_IMAGE);
            mEnableScreensaverBlurEffectPref = findPreference(KEY_ENABLE_SCREENSAVER_BLUR_EFFECT);
            mScreensaverBlurIntensityPref = findPreference(KEY_SCREENSAVER_BLUR_INTENSITY);
            mScreensaverPreview = findPreference(KEY_SCREENSAVER_PREVIEW);
            mScreensaverMainSettings = findPreference(KEY_SCREENSAVER_DAYDREAM_SETTINGS);

            mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mAnalogClock = mClockStyleValues[0];
            mMaterialAnalogClock = mClockStyleValues[1];
            mDigitalClock = mClockStyleValues[2];

            setupPreferences();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_SCREENSAVER_CLOCK_STYLE -> {
                    final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                    mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);

                    boolean isAnalogClock = newValue.equals(mAnalogClock);
                    boolean isMaterialAnalogClock = newValue.equals(mMaterialAnalogClock);
                    boolean isDigitalClock = newValue.equals(mDigitalClock);
                    boolean areDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(mPrefs);
                    boolean isBatteryDisplayed = SettingsDAO.isScreensaverBatteryDisplayed(mPrefs);

                    if (SdkUtils.isAtLeastAndroid12()) {
                        mClockDynamicColorPref.setVisible(!isMaterialAnalogClock);
                        mClockColorPref.setVisible(!isMaterialAnalogClock && !areDynamicColors);
                        if (areDynamicColors) {
                            mBatteryColorPref.setVisible(isBatteryDisplayed && isMaterialAnalogClock);
                            mDateColorPref.setVisible(isMaterialAnalogClock);
                            mNextAlarmColorPref.setVisible(isMaterialAnalogClock);
                        }
                    } else {
                        mClockColorPref.setVisible(!isMaterialAnalogClock);
                    }

                    mClockDialPref.setVisible(isAnalogClock);
                    mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
                    mAnalogClockSizePref.setVisible(!isDigitalClock);
                    mClockSecondHandPref.setVisible(isAnalogClock
                            && SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
                    mDigitalClockFontPref.setVisible(isDigitalClock);
                    mDigitalClockFontSizePref.setVisible(isDigitalClock);
                    mBoldDigitalClockPref.setVisible(isDigitalClock);
                    mItalicDigitalClockPref.setVisible(isDigitalClock);
                }

                case KEY_DISPLAY_SCREENSAVER_BATTERY -> {
                    boolean isBatteryVisible = (boolean) newValue;

                    mBatteryColorPref.setVisible(isBatteryVisible);
                    mBoldBatteryPref.setVisible(isBatteryVisible);
                    mItalicBatteryPref.setVisible(isBatteryVisible);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_SCREENSAVER_CLOCK_DIAL, KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL,
                     KEY_SCREENSAVER_CLOCK_SECOND_HAND -> {
                    final CustomListPreference preference = (CustomListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS -> {
                    mClockSecondHandPref.setVisible((boolean) newValue
                            && SettingsDAO.getScreensaverClockStyle(mPrefs) == DataModel.ClockStyle.ANALOG);

                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                     KEY_SCREENSAVER_BATTERY_IN_BOLD, KEY_SCREENSAVER_BATTERY_IN_ITALIC,
                     KEY_SCREENSAVER_DATE_IN_BOLD, KEY_SCREENSAVER_DATE_IN_ITALIC,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC ->
                        Utils.setVibrationTime(requireContext(), 50);

                case KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS -> {
                    boolean areNotDynamicColors = !(boolean) newValue;

                    mClockColorPref.setVisible(areNotDynamicColors);
                    mBatteryColorPref.setVisible(areNotDynamicColors);
                    mDateColorPref.setVisible(areNotDynamicColors);
                    mNextAlarmColorPref.setVisible(areNotDynamicColors);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_ENABLE_SCREENSAVER_BLUR_EFFECT -> {
                    mScreensaverBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                            && (boolean) newValue
                            && SettingsDAO.getScreensaverBackgroundImage(mPrefs) != null);

                    Utils.setVibrationTime(requireContext(), 50);
                }
            }

            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            final Context context = requireActivity();

            switch (pref.getKey()) {
                case KEY_SCREENSAVER_PREVIEW -> context.startActivity(
                        new Intent(context, ScreensaverActivity.class)
                                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));

                case KEY_SCREENSAVER_DAYDREAM_SETTINGS -> {
                    final Intent dialogSSMainSettingsIntent = new Intent(Settings.ACTION_DREAM_SETTINGS);
                    dialogSSMainSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogSSMainSettingsIntent);
                }

                case KEY_SCREENSAVER_DIGITAL_CLOCK_FONT -> selectCustomFile(mDigitalClockFontPref,
                        fontPickerLauncher, SettingsDAO.getScreensaverDigitalClockFont(mPrefs),
                        KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, true, null);

                case KEY_SCREENSAVER_BACKGROUND_IMAGE -> selectCustomFile(mScreensaverBackgroundImagePref,
                        imagePickerLauncher, SettingsDAO.getScreensaverBackgroundImage(mPrefs),
                        KEY_SCREENSAVER_BACKGROUND_IMAGE, false,
                        mScreensaverBackgroundImagePref -> {

                    mEnableScreensaverBlurEffectPref.setVisible(false);
                    mScreensaverBlurIntensityPref.setVisible(false);
                });
            }

            return true;
        }

        private void setupPreferences() {
            final boolean isAnalogClock = mClockStylePref.getValue().equals(mAnalogClock);
            final boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
            final boolean isDigitalClock = mClockStylePref.getValue().equals(mDigitalClock);
            final boolean isBatteryDisplayed = SettingsDAO.isScreensaverBatteryDisplayed(mPrefs);
            final String screensaverBackgroundImage = SettingsDAO.getScreensaverBackgroundImage(mPrefs);

            mClockStylePref.setSummary(mClockStylePref.getEntry());
            mClockStylePref.setOnPreferenceChangeListener(this);

            mClockDialPref.setVisible(isAnalogClock);
            mClockDialPref.setSummary(mClockDialPref.getEntry());
            mClockDialPref.setOnPreferenceChangeListener(this);

            mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
            mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
            mClockDialMaterialPref.setOnPreferenceChangeListener(this);

            mDigitalClockFontPref.setVisible(isDigitalClock);
            mDigitalClockFontPref.setTitle(getString(SettingsDAO.getScreensaverDigitalClockFont(mPrefs) == null
                    ? R.string.custom_font_title
                    : R.string.custom_font_title_variant));
            mDigitalClockFontPref.setOnPreferenceClickListener(this);

            mAnalogClockSizePref.setVisible(!isDigitalClock);

            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mClockSecondHandPref.setVisible(isAnalogClock
                    && SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
            mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
            mClockSecondHandPref.setOnPreferenceChangeListener(this);

            mDisplayBatteryPref.setOnPreferenceChangeListener(this);

            if (SdkUtils.isAtLeastAndroid12()) {
                final boolean areScreensaverClockDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(mPrefs);
                mClockDynamicColorPref.setVisible(!isMaterialAnalogClock);
                mClockDynamicColorPref.setOnPreferenceChangeListener(this);
                mClockColorPref.setVisible(!areScreensaverClockDynamicColors && !isMaterialAnalogClock);
                mBatteryColorPref.setVisible(isBatteryDisplayed
                        && (!areScreensaverClockDynamicColors || isMaterialAnalogClock));
                mDateColorPref.setVisible(!areScreensaverClockDynamicColors || isMaterialAnalogClock);
                mNextAlarmColorPref.setVisible(!areScreensaverClockDynamicColors || isMaterialAnalogClock);
            } else {
                mClockColorPref.setVisible(!isMaterialAnalogClock);
            }

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

            mBoldNextAlarmPref.setOnPreferenceChangeListener(this);

            mItalicNextAlarmPref.setOnPreferenceChangeListener(this);

            mScreensaverBackgroundImagePref.setTitle(getString(screensaverBackgroundImage == null
                    ? R.string.background_image_title
                    : R.string.background_image_title_variant));
            mScreensaverBackgroundImagePref.setOnPreferenceClickListener(this);

            mEnableScreensaverBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    && screensaverBackgroundImage != null);
            mEnableScreensaverBlurEffectPref.setOnPreferenceChangeListener(this);

            mScreensaverBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    && screensaverBackgroundImage != null
                    && SettingsDAO.isScreensaverBlurEffectEnabled(mPrefs));

            mScreensaverPreview.setOnPreferenceClickListener(this);

            mScreensaverMainSettings.setOnPreferenceClickListener(this);
        }

    }
    
}

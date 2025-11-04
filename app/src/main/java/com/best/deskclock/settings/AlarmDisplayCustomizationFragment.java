// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.OVERRIDE_TRANSITION_OPEN;
import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_AMOLED_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BUTTON_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_DIAL_MATERIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DISPLAY_TEXT_SHADOW;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SECOND_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_IMAGE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SHADOW_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISMISS_BUTTON_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISMISS_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_ALARM_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_RINGTONE_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_BACKGROUND_IMAGE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_BLUR_EFFECT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_PREVIEW_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_RINGTONE_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SLIDE_ZONE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SNOOZE_BUTTON_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SNOOZE_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SWIPE_ACTION;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;
import com.rarepebble.colorpicker.ColorPreference;

import java.io.File;

public class AlarmDisplayCustomizationFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    String[] mAlarmClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;
    String mDigitalClock;

    ListPreference mAlarmClockStylePref;
    ListPreference mAlarmClockDialPref;
    ListPreference mAlarmClockDialMaterialPref;
    ListPreference mAlarmClockSecondHandPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mSwipeActionPref;
    ColorPreference mAlarmClockColorPref;
    ColorPreference mAlarmSecondHandColorPref;
    ColorPreference mSlideZoneColorPref;
    ColorPreference mAlarmButtonColorPref;
    ColorPreference mSnoozeTitleColorPref;
    ColorPreference mSnoozeButtonColorPref;
    ColorPreference mDismissTitleColorPref;
    ColorPreference mDismissButtonColorPref;
    ColorPreference mBackgroundColorPref;
    ColorPreference mBackgroundAmoledColorPref;
    CustomSeekbarPreference mAlarmDigitalClockFontSizePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    ColorPreference mShadowColorPref;
    Preference mShadowOffsetPref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    ColorPreference mRingtoneTitleColorPref;
    SwitchPreferenceCompat mEnableAlarmBackgroundImagePref;
    Preference mAlarmBackgroundImagePref;
    SwitchPreferenceCompat mEnableAlarmBlurEffectPref;
    Preference mAlarmBlurIntensityPref;
    Preference mPreviewAlarmPref;

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

                String safeTitle = Utils.toSafeFileName("alarm_background");

                // Delete the old image if it exists
                String oldPath = mPrefs.getString(KEY_ALARM_BACKGROUND_IMAGE, null);
                if (oldPath != null) {
                    File oldFile = new File(oldPath);
                    if (oldFile.exists() && oldFile.isFile()) {
                        boolean deleted = oldFile.delete();
                        if (!deleted) {
                            LogUtils.w("Unable to delete the old image: " + oldPath);
                        }
                    }
                }

                // Copy the new image to the device's protected storage
                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_ALARM_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                    mEnableAlarmBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12());
                    mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                            && SettingsDAO.isAlarmBlurEffectEnabled(mPrefs));

                    Toast.makeText(requireContext(), R.string.background_image_toast_message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Error importing image", Toast.LENGTH_SHORT).show();
                    mEnableAlarmBlurEffectPref.setVisible(false);
                    mAlarmBlurIntensityPref.setVisible(false);
                }
            });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.alarm_display_customization_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_alarm_display);

        mAlarmClockStylePref = findPreference(KEY_ALARM_CLOCK_STYLE);
        mAlarmClockDialPref = findPreference(KEY_ALARM_CLOCK_DIAL);
        mAlarmClockDialMaterialPref = findPreference(KEY_ALARM_CLOCK_DIAL_MATERIAL);
        mDisplaySecondsPref = findPreference(KEY_DISPLAY_ALARM_SECOND_HAND);
        mAlarmClockSecondHandPref = findPreference(KEY_ALARM_CLOCK_SECOND_HAND);
        mSwipeActionPref = findPreference(KEY_SWIPE_ACTION);
        mBackgroundColorPref = findPreference(KEY_ALARM_BACKGROUND_COLOR);
        mBackgroundAmoledColorPref = findPreference(KEY_ALARM_BACKGROUND_AMOLED_COLOR);
        mAlarmClockColorPref = findPreference(KEY_ALARM_CLOCK_COLOR);
        mAlarmSecondHandColorPref = findPreference(KEY_ALARM_SECOND_HAND_COLOR);
        mSlideZoneColorPref = findPreference(KEY_SLIDE_ZONE_COLOR);
        mAlarmButtonColorPref = findPreference(KEY_ALARM_BUTTON_COLOR);
        mSnoozeTitleColorPref = findPreference(KEY_SNOOZE_TITLE_COLOR);
        mSnoozeButtonColorPref = findPreference(KEY_SNOOZE_BUTTON_COLOR);
        mDismissTitleColorPref = findPreference(KEY_DISMISS_TITLE_COLOR);
        mDismissButtonColorPref = findPreference(KEY_DISMISS_BUTTON_COLOR);
        mAlarmDigitalClockFontSizePref = findPreference(KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE);
        mDisplayTextShadowPref = findPreference(KEY_ALARM_DISPLAY_TEXT_SHADOW);
        mShadowColorPref = findPreference(KEY_ALARM_SHADOW_COLOR);
        mShadowOffsetPref = findPreference(KEY_ALARM_SHADOW_OFFSET);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_RINGTONE_TITLE);
        mRingtoneTitleColorPref = findPreference(KEY_RINGTONE_TITLE_COLOR);
        mEnableAlarmBackgroundImagePref = findPreference(KEY_ENABLE_ALARM_BACKGROUND_IMAGE);
        mAlarmBackgroundImagePref = findPreference(KEY_ALARM_BACKGROUND_IMAGE);
        mEnableAlarmBlurEffectPref = findPreference(KEY_ENABLE_ALARM_BLUR_EFFECT);
        mAlarmBlurIntensityPref = findPreference(KEY_ALARM_BLUR_INTENSITY);
        mPreviewAlarmPref = findPreference(KEY_PREVIEW_ALARM);

        mAlarmClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mAlarmClockStyleValues[0];
        mMaterialAnalogClock = mAlarmClockStyleValues[1];
        mDigitalClock = mAlarmClockStyleValues[2];

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_ALARM_CLOCK_STYLE -> {
                final int clockIndex = mAlarmClockStylePref.findIndexOfValue((String) newValue);
                mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntries()[clockIndex]);
                mAlarmClockDialPref.setVisible(newValue.equals(mAnalogClock));
                mAlarmClockDialMaterialPref.setVisible(newValue.equals(mMaterialAnalogClock));
                mAlarmClockColorPref.setVisible(!newValue.equals(mMaterialAnalogClock));
                mAlarmDigitalClockFontSizePref.setVisible(newValue.equals(mDigitalClock));
                mDisplaySecondsPref.setVisible(!newValue.equals(mDigitalClock));
                mAlarmClockSecondHandPref.setVisible(newValue.equals(mAnalogClock)
                        && SettingsDAO.isAlarmSecondHandDisplayed(mPrefs));
                mAlarmSecondHandColorPref.setVisible(newValue.equals(mAnalogClock)
                        && SettingsDAO.isAlarmSecondHandDisplayed(mPrefs));
            }

            case KEY_ALARM_CLOCK_DIAL, KEY_ALARM_CLOCK_DIAL_MATERIAL, KEY_ALARM_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_DISPLAY_ALARM_SECOND_HAND -> {
                mAlarmClockSecondHandPref.setVisible((boolean) newValue
                        && SettingsDAO.getAlarmClockStyle(mPrefs) == DataModel.ClockStyle.ANALOG);
                mAlarmSecondHandColorPref.setVisible((boolean) newValue
                        && SettingsDAO.getAlarmClockStyle(mPrefs) != DataModel.ClockStyle.ANALOG_MATERIAL);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_SWIPE_ACTION -> {
                mSlideZoneColorPref.setVisible((boolean) newValue);
                mSnoozeTitleColorPref.setVisible((boolean) newValue);
                mSnoozeButtonColorPref.setVisible(!(boolean) newValue);
                mDismissTitleColorPref.setVisible((boolean) newValue);
                mDismissButtonColorPref.setVisible(!(boolean) newValue);
                mAlarmButtonColorPref.setVisible((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ALARM_DISPLAY_TEXT_SHADOW -> {
                mShadowColorPref.setVisible((boolean) newValue);
                mShadowOffsetPref.setVisible((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DISPLAY_RINGTONE_TITLE -> {
                mRingtoneTitleColorPref.setVisible((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ENABLE_ALARM_BACKGROUND_IMAGE -> {
                mAlarmBackgroundImagePref.setVisible((boolean) newValue);
                mEnableAlarmBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12()
                        && (boolean) newValue
                        && SettingsDAO.getAlarmBackgroundImage(mPrefs) != null);
                mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                        && (boolean) newValue
                        && SettingsDAO.getAlarmBackgroundImage(mPrefs) != null
                        && SettingsDAO.isAlarmBlurEffectEnabled(mPrefs));

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ENABLE_ALARM_BLUR_EFFECT -> {
                mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                        && (boolean) newValue
                        && SettingsDAO.getAlarmBackgroundImage(mPrefs) != null);

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

        switch (pref.getKey()) {
            case KEY_PREVIEW_ALARM -> {
                startActivity(new Intent(context, AlarmDisplayPreviewActivity.class));
                if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
                    if (SdkUtils.isAtLeastAndroid14()) {
                        requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                                R.anim.fade_in, R.anim.fade_out);
                    } else {
                        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                } else {
                    if (SdkUtils.isAtLeastAndroid14()) {
                        requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                                R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
                    } else {
                        requireActivity().overridePendingTransition(
                                R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
                    }
                }
            }
            case KEY_ALARM_BACKGROUND_IMAGE ->
                    imagePickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("image/*")
                            .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png"})
            );
        }

        return true;
    }

    private void setupPreferences() {
        mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntry());
        mAlarmClockStylePref.setOnPreferenceChangeListener(this);

        mAlarmClockDialPref.setVisible(mAlarmClockStylePref.getValue().equals(mAnalogClock));
        mAlarmClockDialPref.setSummary(mAlarmClockDialPref.getEntry());
        mAlarmClockDialPref.setOnPreferenceChangeListener(this);

        mAlarmClockDialMaterialPref.setVisible(mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock));
        mAlarmClockDialMaterialPref.setSummary(mAlarmClockDialMaterialPref.getEntry());
        mAlarmClockDialMaterialPref.setOnPreferenceChangeListener(this);

        final boolean isAmoledMode = ThemeUtils.isNight(getResources())
                && SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE);
        mBackgroundAmoledColorPref.setVisible(isAmoledMode);

        mBackgroundColorPref.setVisible(!isAmoledMode);

        mAlarmClockColorPref.setVisible(!mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock));

        mDisplaySecondsPref.setVisible(!mAlarmClockStylePref.getValue().equals(mDigitalClock));
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mAlarmClockSecondHandPref.setVisible(mAlarmClockStylePref.getValue().equals(mAnalogClock)
                && SettingsDAO.isAlarmSecondHandDisplayed(mPrefs));
        mAlarmClockSecondHandPref.setSummary(mAlarmClockSecondHandPref.getEntry());
        mAlarmClockSecondHandPref.setOnPreferenceChangeListener(this);

        mSwipeActionPref.setOnPreferenceChangeListener(this);

        int color = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);
        mAlarmSecondHandColorPref.setVisible(mAlarmClockStylePref.getValue().equals(mAnalogClock)
                && SettingsDAO.isAlarmSecondHandDisplayed(mPrefs));
        mAlarmSecondHandColorPref.setDefaultValue(color);

        boolean isSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(mPrefs);
        mSlideZoneColorPref.setVisible(isSwipeActionEnabled);

        mSnoozeTitleColorPref.setVisible(isSwipeActionEnabled);

        mSnoozeButtonColorPref.setVisible(!isSwipeActionEnabled);
        mSnoozeButtonColorPref.setDefaultValue(color);

        mDismissTitleColorPref.setVisible(isSwipeActionEnabled);

        mDismissButtonColorPref.setVisible(!isSwipeActionEnabled);
        mDismissButtonColorPref.setDefaultValue(color);

        mAlarmButtonColorPref.setVisible(isSwipeActionEnabled);
        mAlarmButtonColorPref.setDefaultValue(color);

        mAlarmDigitalClockFontSizePref.setVisible(mAlarmClockStylePref.getValue().equals(mDigitalClock));

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShadowColorPref.setVisible(SettingsDAO.isAlarmTextShadowDisplayed(mPrefs));

        mShadowOffsetPref.setVisible(SettingsDAO.isAlarmTextShadowDisplayed(mPrefs));

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mRingtoneTitleColorPref.setVisible(SettingsDAO.isRingtoneTitleDisplayed(mPrefs));

        mEnableAlarmBackgroundImagePref.setOnPreferenceChangeListener(this);

        mAlarmBackgroundImagePref.setVisible(SettingsDAO.isAlarmBackgroundImageEnabled(mPrefs));
        mAlarmBackgroundImagePref.setOnPreferenceClickListener(this);

        mEnableAlarmBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12()
                && SettingsDAO.isAlarmBackgroundImageEnabled(mPrefs)
                && SettingsDAO.getAlarmBackgroundImage(mPrefs) != null);
        mEnableAlarmBlurEffectPref.setOnPreferenceChangeListener(this);

        mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                && SettingsDAO.isAlarmBackgroundImageEnabled(mPrefs)
                && SettingsDAO.getAlarmBackgroundImage(mPrefs) != null
                && SettingsDAO.isAlarmBlurEffectEnabled(mPrefs));

        mPreviewAlarmPref.setOnPreferenceClickListener(this);
    }

}

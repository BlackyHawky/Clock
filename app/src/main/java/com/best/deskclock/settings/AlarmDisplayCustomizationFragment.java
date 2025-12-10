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
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SECOND_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_IMAGE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SHADOW_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISMISS_BUTTON_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISMISS_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_ALARM_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_RINGTONE_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_BLUR_EFFECT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_PREVIEW;
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
import com.best.deskclock.data.DataModel.ClockStyle;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    Preference mAlarmFontPref;
    SwitchPreferenceCompat mSwipeActionPref;
    ColorPickerPreference mAlarmClockColorPref;
    ColorPickerPreference mAlarmSecondHandColorPref;
    ColorPickerPreference mSlideZoneColorPref;
    ColorPickerPreference mAlarmButtonColorPref;
    ColorPickerPreference mSnoozeTitleColorPref;
    ColorPickerPreference mSnoozeButtonColorPref;
    ColorPickerPreference mDismissTitleColorPref;
    ColorPickerPreference mDismissButtonColorPref;
    ColorPickerPreference mBackgroundColorPref;
    ColorPickerPreference mBackgroundAmoledColorPref;
    CustomSeekbarPreference mAlarmDigitalClockFontSizePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    ColorPickerPreference mShadowColorPref;
    Preference mShadowOffsetPref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    ColorPickerPreference mRingtoneTitleColorPref;
    Preference mAlarmBackgroundImagePref;
    SwitchPreferenceCompat mEnableAlarmBlurEffectPref;
    Preference mAlarmBlurIntensityPref;
    Preference mAlarmPreviewPref;

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

                String safeTitle = Utils.toSafeFileName("alarm_font");

                // Delete the old font if it exists
                clearAlarmFontFile();

                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_ALARM_FONT, copiedUri.getPath()).apply();
                    mAlarmFontPref.setTitle(getString(R.string.custom_font_title_variant));

                    Toast.makeText(requireContext(), R.string.custom_font_toast_message_selected, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Error importing font", Toast.LENGTH_SHORT).show();
                    mAlarmFontPref.setTitle(getString(R.string.custom_font_title));
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

                String safeTitle = Utils.toSafeFileName("alarm_background");

                // Delete the old image if it exists
                clearAlarmBackgroundFile();

                // Copy the new image to the device's protected storage
                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_ALARM_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                    mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                    mEnableAlarmBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12());
                    mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                            && SettingsDAO.isAlarmBlurEffectEnabled(mPrefs));

                    Toast.makeText(requireContext(), R.string.background_image_toast_message_selected, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Error importing image", Toast.LENGTH_SHORT).show();
                    mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                    mEnableAlarmBlurEffectPref.setVisible(false);
                    mAlarmBlurIntensityPref.setVisible(false);
                }
            });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.display_settings_title);
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
        mAlarmFontPref = findPreference(KEY_ALARM_FONT);
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
        mAlarmBackgroundImagePref = findPreference(KEY_ALARM_BACKGROUND_IMAGE);
        mEnableAlarmBlurEffectPref = findPreference(KEY_ENABLE_ALARM_BLUR_EFFECT);
        mAlarmBlurIntensityPref = findPreference(KEY_ALARM_BLUR_INTENSITY);
        mAlarmPreviewPref = findPreference(KEY_ALARM_PREVIEW);

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
                boolean isAnalogClock = newValue.equals(mAnalogClock);
                boolean isMaterialAnalogClock = newValue.equals(mMaterialAnalogClock);
                boolean isDigitalClock = newValue.equals(mDigitalClock);
                boolean isSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(mPrefs);

                final int clockIndex = mAlarmClockStylePref.findIndexOfValue((String) newValue);
                mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntries()[clockIndex]);

                mAlarmClockDialPref.setVisible(isAnalogClock);
                mAlarmClockDialMaterialPref.setVisible(isMaterialAnalogClock);
                mAlarmClockColorPref.setVisible(!isMaterialAnalogClock);
                mAlarmFontPref.setVisible(isDigitalClock);
                mAlarmDigitalClockFontSizePref.setVisible(isDigitalClock);
                mDisplaySecondsPref.setVisible(!isDigitalClock);
                mAlarmClockSecondHandPref.setVisible(isAnalogClock && isSecondHandDisplayed);
                mAlarmSecondHandColorPref.setVisible(isAnalogClock && isSecondHandDisplayed);
            }

            case KEY_ALARM_CLOCK_DIAL, KEY_ALARM_CLOCK_DIAL_MATERIAL, KEY_ALARM_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_DISPLAY_ALARM_SECOND_HAND -> {
                boolean isSecondHandDisplayed = (boolean) newValue;
                ClockStyle alarmClockStyle = SettingsDAO.getAlarmClockStyle(mPrefs);

                mAlarmClockSecondHandPref.setVisible(isSecondHandDisplayed
                        && alarmClockStyle == ClockStyle.ANALOG);
                mAlarmSecondHandColorPref.setVisible(isSecondHandDisplayed
                        && alarmClockStyle != ClockStyle.ANALOG_MATERIAL);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_SWIPE_ACTION -> {
                boolean isSwipeActionEnabled = (boolean) newValue;

                mSlideZoneColorPref.setVisible(isSwipeActionEnabled);
                mSnoozeTitleColorPref.setVisible(isSwipeActionEnabled);
                mSnoozeButtonColorPref.setVisible(!isSwipeActionEnabled);
                mDismissTitleColorPref.setVisible(isSwipeActionEnabled);
                mDismissButtonColorPref.setVisible(!isSwipeActionEnabled);
                mAlarmButtonColorPref.setVisible(isSwipeActionEnabled);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ALARM_DISPLAY_TEXT_SHADOW -> {
                boolean isTextShadowDisplayed = (boolean) newValue;
                mShadowColorPref.setVisible(isTextShadowDisplayed);
                mShadowOffsetPref.setVisible(isTextShadowDisplayed);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DISPLAY_RINGTONE_TITLE -> {
                mRingtoneTitleColorPref.setVisible((boolean) newValue);

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
        if (preference instanceof ColorPickerPreference colorPickerPref) {
            colorPickerPref.showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        switch (pref.getKey()) {
            case KEY_ALARM_FONT -> {
                if (SettingsDAO.getAlarmFont(mPrefs) == null) {
                    selectAlarmFont();
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.custom_font_dialog_title)
                            .setMessage(R.string.custom_font_title_variant)
                            .setPositiveButton(getString(R.string.label_new_font), (dialog, which) ->
                                    selectAlarmFont())
                            .setNeutralButton(getString(R.string.delete), (dialog, which) ->
                                    deleteAlarmFont())
                            .show();
                }
            }

            case KEY_ALARM_BACKGROUND_IMAGE -> {
                if (SettingsDAO.getAlarmBackgroundImage(mPrefs) == null) {
                    selectImageBackground();
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.background_image_dialog_title)
                            .setMessage(R.string.background_image_title_variant)
                            .setPositiveButton(getString(R.string.label_new_image), (dialog, which) ->
                                    selectImageBackground())
                            .setNeutralButton(getString(R.string.delete), (dialog, which) ->
                                    deleteImageBackground())
                            .show();
                }
            }

            case KEY_ALARM_PREVIEW -> {
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
        }

        return true;
    }

    private void setupPreferences() {
        final boolean isAnalogClock = mAlarmClockStylePref.getValue().equals(mAnalogClock);
        final boolean isMaterialAnalogClock = mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock);
        final boolean isDigitalClock = mAlarmClockStylePref.getValue().equals(mDigitalClock);
        final boolean isSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(mPrefs);
        final boolean isSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(mPrefs);
        final boolean isTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(mPrefs);
        final String alarmBackgroundImage = SettingsDAO.getAlarmBackgroundImage(mPrefs);

        mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntry());
        mAlarmClockStylePref.setOnPreferenceChangeListener(this);

        mAlarmClockDialPref.setVisible(isAnalogClock);
        mAlarmClockDialPref.setSummary(mAlarmClockDialPref.getEntry());
        mAlarmClockDialPref.setOnPreferenceChangeListener(this);

        mAlarmClockDialMaterialPref.setVisible(isMaterialAnalogClock);
        mAlarmClockDialMaterialPref.setSummary(mAlarmClockDialMaterialPref.getEntry());
        mAlarmClockDialMaterialPref.setOnPreferenceChangeListener(this);

        final boolean isAmoledMode = ThemeUtils.isNight(getResources())
                && SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE);
        mBackgroundAmoledColorPref.setVisible(isAmoledMode);

        mBackgroundColorPref.setVisible(!isAmoledMode);

        mAlarmClockColorPref.setVisible(!isMaterialAnalogClock);

        mDisplaySecondsPref.setVisible(!isDigitalClock);
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mAlarmClockSecondHandPref.setVisible(isAnalogClock && isSecondHandDisplayed);
        mAlarmClockSecondHandPref.setSummary(mAlarmClockSecondHandPref.getEntry());
        mAlarmClockSecondHandPref.setOnPreferenceChangeListener(this);

        mAlarmFontPref.setVisible(isDigitalClock);
        mAlarmFontPref.setTitle(getString(SettingsDAO.getAlarmFont(mPrefs) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
        mAlarmFontPref.setOnPreferenceClickListener(this);

        mSwipeActionPref.setOnPreferenceChangeListener(this);

        int color = MaterialColors.getColor(
                requireContext(), com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);
        mAlarmSecondHandColorPref.setVisible(isAnalogClock && isSecondHandDisplayed);
        mAlarmSecondHandColorPref.setDefaultValue(color);

        mSlideZoneColorPref.setVisible(isSwipeActionEnabled);

        mSnoozeTitleColorPref.setVisible(isSwipeActionEnabled);

        mSnoozeButtonColorPref.setVisible(!isSwipeActionEnabled);
        mSnoozeButtonColorPref.setDefaultValue(color);

        mDismissTitleColorPref.setVisible(isSwipeActionEnabled);

        mDismissButtonColorPref.setVisible(!isSwipeActionEnabled);
        mDismissButtonColorPref.setDefaultValue(color);

        mAlarmButtonColorPref.setVisible(isSwipeActionEnabled);
        mAlarmButtonColorPref.setDefaultValue(color);

        mAlarmDigitalClockFontSizePref.setVisible(isDigitalClock);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShadowColorPref.setVisible(isTextShadowDisplayed);

        mShadowOffsetPref.setVisible(isTextShadowDisplayed);

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mRingtoneTitleColorPref.setVisible(SettingsDAO.isRingtoneTitleDisplayed(mPrefs));

        mAlarmBackgroundImagePref.setTitle(getString(alarmBackgroundImage == null
                ? R.string.background_image_title
                : R.string.background_image_title_variant));
        mAlarmBackgroundImagePref.setOnPreferenceClickListener(this);

        mEnableAlarmBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12()
                && alarmBackgroundImage != null);
        mEnableAlarmBlurEffectPref.setOnPreferenceChangeListener(this);

        mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                && alarmBackgroundImage != null
                && SettingsDAO.isAlarmBlurEffectEnabled(mPrefs));

        mAlarmPreviewPref.setOnPreferenceClickListener(this);
    }

    private void selectAlarmFont() {
        fontPickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"application/x-font-ttf", "application/x-font-otf", "font/ttf", "font/otf"})

        );
    }

    private void deleteAlarmFont() {
        clearAlarmFontFile();

        mPrefs.edit().remove(KEY_ALARM_FONT).apply();
        mAlarmFontPref.setTitle(getString(R.string.custom_font_title));

        Toast.makeText(requireContext(), R.string.custom_font_toast_message_deleted, Toast.LENGTH_SHORT).show();
    }

    private void clearAlarmFontFile() {
        String path = mPrefs.getString(KEY_ALARM_FONT, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    LogUtils.w("Unable to delete alarm font: " + path);
                }
            }
        }
    }

    private void selectImageBackground() {
        imagePickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png"})
        );
    }

    private void deleteImageBackground() {
        clearAlarmBackgroundFile();

        mPrefs.edit().remove(KEY_ALARM_BACKGROUND_IMAGE).apply();
        mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
        mEnableAlarmBlurEffectPref.setVisible(false);
        mAlarmBlurIntensityPref.setVisible(false);

        Toast.makeText(requireContext(), R.string.background_image_toast_message_deleted, Toast.LENGTH_SHORT).show();
    }

    private void clearAlarmBackgroundFile() {
        String path = mPrefs.getString(KEY_ALARM_BACKGROUND_IMAGE, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    LogUtils.w("Unable to delete alarm background image: " + path);
                }
            }
        }
    }

}

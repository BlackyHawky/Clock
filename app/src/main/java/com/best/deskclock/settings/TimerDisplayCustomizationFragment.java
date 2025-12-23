// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.OVERRIDE_TRANSITION_OPEN;
import static android.app.Activity.RESULT_OK;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_TIMER_RINGTONE_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_TIMER_STATE_INDICATOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_TIMER_BLUR_EFFECT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_EXPIRED_TIMER_INDICATOR_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_PAUSED_TIMER_INDICATOR_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_RUNNING_TIMER_INDICATOR_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_BACKGROUND_IMAGE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_COLOR_CATEGORY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_DISPLAY_TEXT_SHADOW;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_DURATION_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_FONT_CATEGORY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_PREVIEW;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_RINGTONE_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHADOW_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomPreference;
import com.best.deskclock.settings.custompreference.CustomPreferenceCategory;
import com.best.deskclock.settings.custompreference.CustomSeekbarPreference;
import com.best.deskclock.settings.custompreference.CustomSwitchPreference;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

public class TimerDisplayCustomizationFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    CustomPreference mTimerDurationFontPref;
    CustomSwitchPreference mTransparentBackgroundPref;
    CustomSwitchPreference mDisplayTimerStateIndicatorPref;
    CustomSwitchPreference mDisplayRingtoneTitlePref;
    CustomPreferenceCategory mTimerColorCategory;
    ColorPickerPreference mRunningTimerIndicatorColorPref;
    ColorPickerPreference mPausedTimerIndicatorColorPref;
    ColorPickerPreference mExpiredTimerIndicatorColorPref;
    ColorPickerPreference mRingtoneTitleColorPref;
    CustomPreferenceCategory mTimerFontCategory;
    CustomSwitchPreference mDisplayTextShadowPref;
    ColorPickerPreference mShadowColorPref;
    CustomSeekbarPreference mShadowOffsetPref;
    CustomPreference mTimerBackgroundImagePref;
    CustomSwitchPreference mEnableTimerBlurEffectPref;
    CustomSeekbarPreference mTimerBlurIntensityPref;
    CustomPreference mTimerPreviewPref;

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

                String safeTitle = Utils.toSafeFileName("timer_font");

                // Delete the old font if it exists
                clearFile(mPrefs.getString(KEY_TIMER_DURATION_FONT, null));

                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_TIMER_DURATION_FONT, copiedUri.getPath()).apply();
                    mTimerDurationFontPref.setTitle(getString(R.string.custom_font_title_variant));

                    CustomToast.show(requireContext(), R.string.custom_font_toast_message_selected);
                } else {
                    CustomToast.show(requireContext(), "Error importing font");
                    mTimerDurationFontPref.setTitle(getString(R.string.custom_font_title));
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

                String safeTitle = Utils.toSafeFileName("timer_background");

                // Delete the old image if it exists
                clearFile(mPrefs.getString(KEY_TIMER_BACKGROUND_IMAGE, null));

                // Copy the new image to the device's protected storage
                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_TIMER_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                    mTimerBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                    mEnableTimerBlurEffectPref.setVisible(SdkUtils.isAtLeastAndroid12());
                    mTimerBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                            && SettingsDAO.isTimerBlurEffectEnabled(mPrefs));

                    CustomToast.show(requireContext(), R.string.background_image_toast_message_selected);
                } else {
                    CustomToast.show(requireContext(), "Error importing image");
                    mTimerBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                    mEnableTimerBlurEffectPref.setVisible(false);
                    mTimerBlurIntensityPref.setVisible(false);
                }
            });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.display_settings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_timer_display);

        mTimerDurationFontPref = findPreference(KEY_TIMER_DURATION_FONT);
        mTransparentBackgroundPref = findPreference(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER);
        mDisplayTimerStateIndicatorPref = findPreference(KEY_DISPLAY_TIMER_STATE_INDICATOR);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_TIMER_RINGTONE_TITLE);
        mTimerColorCategory = findPreference(KEY_TIMER_COLOR_CATEGORY);
        mRunningTimerIndicatorColorPref = findPreference(KEY_RUNNING_TIMER_INDICATOR_COLOR);
        mPausedTimerIndicatorColorPref = findPreference(KEY_PAUSED_TIMER_INDICATOR_COLOR);
        mExpiredTimerIndicatorColorPref = findPreference(KEY_EXPIRED_TIMER_INDICATOR_COLOR);
        mRingtoneTitleColorPref = findPreference(KEY_TIMER_RINGTONE_TITLE_COLOR);
        mTimerFontCategory = findPreference(KEY_TIMER_FONT_CATEGORY);
        mDisplayTextShadowPref = findPreference(KEY_TIMER_DISPLAY_TEXT_SHADOW);
        mShadowColorPref = findPreference(KEY_TIMER_SHADOW_COLOR);
        mShadowOffsetPref = findPreference(KEY_TIMER_SHADOW_OFFSET);
        mTimerBackgroundImagePref = findPreference(KEY_TIMER_BACKGROUND_IMAGE);
        mEnableTimerBlurEffectPref = findPreference(KEY_ENABLE_TIMER_BLUR_EFFECT);
        mTimerBlurIntensityPref = findPreference(KEY_TIMER_BLUR_INTENSITY);
        mTimerPreviewPref = findPreference(KEY_TIMER_PREVIEW);

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER -> {
                boolean isNotBackgroundTransparent = !(boolean) newValue;
                boolean isNotTimerBackgroundImageNull = SettingsDAO.getTimerBackgroundImage(mPrefs) != null;
                boolean isAtLeastAndroid12 = SdkUtils.isAtLeastAndroid12();

                mTimerBackgroundImagePref.setVisible(isNotBackgroundTransparent);

                mEnableTimerBlurEffectPref.setVisible(isAtLeastAndroid12
                        && isNotBackgroundTransparent
                        && isNotTimerBackgroundImageNull);

                mTimerBlurIntensityPref.setVisible(isAtLeastAndroid12
                        && isNotBackgroundTransparent
                        && isNotTimerBackgroundImageNull
                        && SettingsDAO.isTimerBlurEffectEnabled(mPrefs));

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DISPLAY_TIMER_STATE_INDICATOR -> {
                boolean isTimerStateIndicatorDisplayed = (boolean) newValue;

                mTimerColorCategory.setVisible(isTimerStateIndicatorDisplayed
                        || SettingsDAO.isTimerRingtoneTitleDisplayed(mPrefs));
                mRunningTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);
                mPausedTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);
                mExpiredTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DISPLAY_TIMER_RINGTONE_TITLE -> {
                boolean isRingtoneTitleDisplayed = (boolean) newValue;
                boolean isTextShadowDisplayed = SettingsDAO.isTimerTextShadowDisplayed(mPrefs);

                mTimerColorCategory.setVisible(isRingtoneTitleDisplayed
                        || SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs));
                mRingtoneTitleColorPref.setVisible(isRingtoneTitleDisplayed);
                mTimerFontCategory.setVisible(isRingtoneTitleDisplayed);
                mDisplayTextShadowPref.setVisible(isRingtoneTitleDisplayed);
                mShadowColorPref.setVisible(isRingtoneTitleDisplayed && isTextShadowDisplayed);
                mShadowOffsetPref.setVisible(isRingtoneTitleDisplayed && isTextShadowDisplayed);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_TIMER_DISPLAY_TEXT_SHADOW -> {
                boolean displayTextShadow = (boolean) newValue;

                mShadowColorPref.setVisible(displayTextShadow);
                mShadowOffsetPref.setVisible(displayTextShadow);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ENABLE_TIMER_BLUR_EFFECT -> {
                mTimerBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12()
                        && (boolean) newValue
                        && SettingsDAO.getTimerBackgroundImage(mPrefs) != null);

                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        switch (pref.getKey()) {
            case KEY_TIMER_DURATION_FONT -> selectCustomFile(mTimerDurationFontPref, fontPickerLauncher,
                    SettingsDAO.getTimerDurationFont(mPrefs), KEY_TIMER_DURATION_FONT, true, null);

            case KEY_TIMER_BACKGROUND_IMAGE -> selectCustomFile(mTimerBackgroundImagePref, imagePickerLauncher,
                    SettingsDAO.getTimerBackgroundImage(mPrefs), KEY_TIMER_BACKGROUND_IMAGE, false,
                    mTimerBackgroundImagePref -> {
                mEnableTimerBlurEffectPref.setVisible(false);
                mTimerBlurIntensityPref.setVisible(false);
            });

            case KEY_TIMER_PREVIEW -> {
                startActivity(new Intent(context, TimerDisplayPreviewActivity.class));
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
        final boolean isTimerStateIndicatorDisplayed = SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs);
        final boolean isTimerRingtoneTitleDisplayed = SettingsDAO.isTimerRingtoneTitleDisplayed(mPrefs);
        final boolean isTimerTextShadowDisplayed = SettingsDAO.isTimerTextShadowDisplayed(mPrefs);

        mTimerDurationFontPref.setTitle(getString(SettingsDAO.getTimerDurationFont(mPrefs) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
        mTimerDurationFontPref.setOnPreferenceClickListener(this);

        mTransparentBackgroundPref.setOnPreferenceChangeListener(this);

        mDisplayTimerStateIndicatorPref.setOnPreferenceChangeListener(this);

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mTimerColorCategory.setVisible(isTimerStateIndicatorDisplayed || isTimerRingtoneTitleDisplayed);

        mRunningTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);

        mPausedTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);

        mExpiredTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);

        mRingtoneTitleColorPref.setVisible(isTimerRingtoneTitleDisplayed);

        mTimerFontCategory.setVisible(isTimerRingtoneTitleDisplayed);

        mDisplayTextShadowPref.setVisible(isTimerRingtoneTitleDisplayed);
        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShadowColorPref.setVisible(isTimerRingtoneTitleDisplayed && isTimerTextShadowDisplayed);

        mShadowOffsetPref.setVisible(isTimerRingtoneTitleDisplayed && isTimerTextShadowDisplayed);

        final boolean isNotBackgroundTransparent = !SettingsDAO.isTimerBackgroundTransparent(mPrefs);
        final boolean isTimerBackgroundImageNull = SettingsDAO.getTimerBackgroundImage(mPrefs) == null;
        final boolean isAtLeastAndroid12 = SdkUtils.isAtLeastAndroid12();

        mTimerBackgroundImagePref.setVisible(isNotBackgroundTransparent);
        mTimerBackgroundImagePref.setTitle(getString(isTimerBackgroundImageNull
                ? R.string.background_image_title
                : R.string.background_image_title_variant));
        mTimerBackgroundImagePref.setOnPreferenceClickListener(this);

        mEnableTimerBlurEffectPref.setVisible(isAtLeastAndroid12
                && isNotBackgroundTransparent
                && !isTimerBackgroundImageNull);
        mEnableTimerBlurEffectPref.setOnPreferenceChangeListener(this);

        mTimerBlurIntensityPref.setVisible(isAtLeastAndroid12
                && isNotBackgroundTransparent
                && !isTimerBackgroundImageNull
                && SettingsDAO.isTimerBlurEffectEnabled(mPrefs));

        mTimerPreviewPref.setOnPreferenceClickListener(this);
    }

}

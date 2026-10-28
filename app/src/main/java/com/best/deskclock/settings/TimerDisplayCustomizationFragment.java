// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

public class TimerDisplayCustomizationFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    SwitchPreferenceCompat mDisplayCompactTimersPref;
    SwitchPreferenceCompat mDisplayDeleteButtonPref;
    SwitchPreferenceCompat mDisplayTimerEndTimePref;
    SwitchPreferenceCompat mInvertTimerButtonPositionsPref;
    SwitchPreferenceCompat mTransparentBackgroundPref;
    SwitchPreferenceCompat mDisplayTimerStateIndicatorPref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    Preference mTimerBackgroundImagePref;
    CustomSliderPreference mTimerBlurIntensityPref;
    Preference mTimerPreviewPref;

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

            String safeTitle = FileUtils.toSafeFileName(FILE_TIMER_BACKGROUND);
            String oldImagePath = prefs.getString(KEY_TIMER_BACKGROUND_IMAGE, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old image if it exists
                FileUtils.clearFile(oldImagePath);

                // Copy the new image to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    prefs.edit().putString(KEY_TIMER_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, style, font, R.string.background_image_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, style, font, R.string.image_message_error);
                    }

                    if (!isAdded()
                        || mTimerBackgroundImagePref == null
                        || mTimerBlurIntensityPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mTimerBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                    } else {
                        mTimerBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                    }

                    invalidatePreferencesVisibility();
                });
            });
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.display_settings_title);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_timer_display);

        mDisplayCompactTimersPref = findPreference(KEY_DISPLAY_COMPACT_TIMERS);
        mDisplayDeleteButtonPref = findPreference(KEY_DISPLAY_TIMER_DELETE_BUTTON);
        mDisplayTimerEndTimePref = findPreference(KEY_DISPLAY_TIMER_END_TIME);
        mInvertTimerButtonPositionsPref = findPreference(KEY_INVERT_TIMER_BUTTON_POSITIONS);
        mTransparentBackgroundPref = findPreference(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER);
        mDisplayTimerStateIndicatorPref = findPreference(KEY_DISPLAY_TIMER_STATE_INDICATOR);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_TIMER_RINGTONE_TITLE);
        mDisplayTextShadowPref = findPreference(KEY_TIMER_DISPLAY_TEXT_SHADOW);
        mTimerBackgroundImagePref = findPreference(KEY_TIMER_BACKGROUND_IMAGE);
        mTimerBlurIntensityPref = findPreference(KEY_TIMER_BLUR_INTENSITY);
        mTimerPreviewPref = findPreference(KEY_TIMER_PREVIEW);

        setupPreferences();

        invalidatePreferencesVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();

        restoreCustomFileDialogIfNeeded(KEY_TIMER_BACKGROUND_IMAGE, mTimerBackgroundImagePref, imagePickerLauncher,
            this::invalidatePreferencesVisibility);

        invalidatePreferencesVisibility();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_DISPLAY_COMPACT_TIMERS, KEY_DISPLAY_TIMER_DELETE_BUTTON, KEY_DISPLAY_TIMER_END_TIME,
                 KEY_INVERT_TIMER_BUTTON_POSITIONS, KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER,
                 KEY_DISPLAY_TIMER_STATE_INDICATOR, KEY_DISPLAY_TIMER_RINGTONE_TITLE, KEY_TIMER_DISPLAY_TEXT_SHADOW ->
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
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        switch (pref.getKey()) {
            case KEY_TIMER_BACKGROUND_IMAGE -> selectCustomFile(mTimerBackgroundImagePref, imagePickerLauncher,
                SettingsDAO.getTimerBackgroundImage(getPrefs()), KEY_TIMER_BACKGROUND_IMAGE, false, false,
                this::invalidatePreferencesVisibility);

            case KEY_TIMER_PREVIEW -> {
                Intent previewIntent = new Intent(context, TimerDisplayPreviewActivity.class);

                ThemeUtils.startActivityWithTransition(requireContext(), previewIntent, SettingsDAO.isFadeTransitionsEnabled(getPrefs()));
            }
        }

        return true;
    }

    @Override
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        final boolean isSingleTimerMode = SettingsDAO.isSingleTimerModeEnabled(getPrefs());
        final boolean isTimerStateIndicatorDisplayed = mDisplayTimerStateIndicatorPref.isChecked();
        final boolean isTimerRingtoneTitleDisplayed = mDisplayRingtoneTitlePref.isChecked();
        final boolean isNotBackgroundTransparent = !mTransparentBackgroundPref.isChecked();

        return switch (key) {
            case KEY_DISPLAY_COMPACT_TIMERS -> !ThemeUtils.isTablet() && !isSingleTimerMode;

            case KEY_DISPLAY_TIMER_DELETE_BUTTON -> !isSingleTimerMode;

            case KEY_TIMER_COLOR_CATEGORY -> isTimerStateIndicatorDisplayed || isTimerRingtoneTitleDisplayed;

            case KEY_RUNNING_TIMER_INDICATOR_COLOR, KEY_PAUSED_TIMER_INDICATOR_COLOR, KEY_EXPIRED_TIMER_INDICATOR_COLOR,
                 KEY_MISSED_TIMER_INDICATOR_COLOR -> isTimerStateIndicatorDisplayed;

            case KEY_TIMER_RINGTONE_TITLE_COLOR, KEY_TIMER_FONT_CATEGORY, KEY_TIMER_DISPLAY_TEXT_SHADOW -> isTimerRingtoneTitleDisplayed;

            case KEY_TIMER_SHADOW_COLOR, KEY_TIMER_SHADOW_OFFSET -> isTimerRingtoneTitleDisplayed && mDisplayTextShadowPref.isChecked();

            case KEY_TIMER_BACKGROUND_IMAGE -> isNotBackgroundTransparent;

            case KEY_TIMER_BLUR_INTENSITY -> SdkUtils.isAtLeastAndroid12()
                && isNotBackgroundTransparent
                && SettingsDAO.getTimerBackgroundImage(getPrefs()) != null;

            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void setupPreferences() {
        mDisplayCompactTimersPref.setOnPreferenceChangeListener(this);

        mDisplayDeleteButtonPref.setOnPreferenceChangeListener(this);

        mDisplayTimerEndTimePref.setOnPreferenceChangeListener(this);

        mInvertTimerButtonPositionsPref.setOnPreferenceChangeListener(this);

        mTransparentBackgroundPref.setOnPreferenceChangeListener(this);

        mDisplayTimerStateIndicatorPref.setOnPreferenceChangeListener(this);

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        final boolean isTimerBackgroundImageNull = SettingsDAO.getTimerBackgroundImage(getPrefs()) == null;

        mTimerBackgroundImagePref.setTitle(getString(isTimerBackgroundImageNull
            ? R.string.background_image_title
            : R.string.background_image_title_variant));
        mTimerBackgroundImagePref.setOnPreferenceClickListener(this);

        mTimerPreviewPref.setOnPreferenceClickListener(this);
    }

}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Calendar;
import java.util.List;

public class AlarmDisplayCustomizationFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_PENDING_DIALOG_PREF_KEY = "pending_dialog_pref_key";

    private String mPendingDialogPrefKey = null;

    private AlarmUpdateHandler mAlarmUpdateHandler;

    private boolean mHasSpecificImages = false;

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
    SwitchPreferenceCompat mDisplaySnoozeSelectorPref;
    ColorPickerPreference mBackgroundColorPref;
    ColorPickerPreference mAlarmSecondHandColorPref;
    ColorPickerPreference mAlarmButtonColorPref;
    ColorPickerPreference mSnoozeButtonColorPref;
    ColorPickerPreference mDismissButtonColorPref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mDisplayAlarmActionMessagePref;
    SwitchPreferenceCompat mDisplayAlarmTitleOnSingleLinePref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    Preference mAlarmBackgroundImagePref;
    CustomSliderPreference mAlarmBlurIntensityPref;
    SwitchPreferenceCompat mEnablePerAlarmBackgroundImagePref;
    Preference mAlarmPreviewPref;

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

            String safeTitle = FileUtils.toSafeFileName(FILE_ALARM_BACKGROUND);
            String oldImagePath = prefs.getString(KEY_ALARM_BACKGROUND_IMAGE, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old image if it exists
                FileUtils.clearFile(oldImagePath);

                // Copy the new image to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    prefs.edit().putString(KEY_ALARM_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, style, font, R.string.background_image_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, style, font, R.string.image_message_error);
                    }

                    if (!isAdded()
                        || mAlarmBackgroundImagePref == null
                        || mAlarmBlurIntensityPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                        invalidatePreferencesVisibility();
                    } else {
                        mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                        checkSpecificImagesState();
                    }
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

        mAlarmUpdateHandler = new AlarmUpdateHandler(
            requireContext(), getPrefs(), getGeneralTypeface(), null, null, isVibrationsEnabled());

        addPreferencesFromResource(R.xml.settings_alarm_display);

        mAlarmClockStylePref = findPreference(KEY_ALARM_CLOCK_STYLE);
        mAlarmClockDialPref = findPreference(KEY_ALARM_CLOCK_DIAL);
        mAlarmClockDialMaterialPref = findPreference(KEY_ALARM_CLOCK_DIAL_MATERIAL);
        mDisplaySecondsPref = findPreference(KEY_DISPLAY_ALARM_SECOND_HAND);
        mAlarmClockSecondHandPref = findPreference(KEY_ALARM_CLOCK_SECOND_HAND);
        mSwipeActionPref = findPreference(KEY_SWIPE_ACTION);
        mDisplaySnoozeSelectorPref = findPreference(KEY_DISPLAY_SNOOZE_SELECTOR);
        mBackgroundColorPref = findPreference(KEY_ALARM_BACKGROUND_COLOR);
        mAlarmSecondHandColorPref = findPreference(KEY_ALARM_SECOND_HAND_COLOR);
        mAlarmButtonColorPref = findPreference(KEY_ALARM_BUTTON_COLOR);
        mSnoozeButtonColorPref = findPreference(KEY_SNOOZE_BUTTON_COLOR);
        mDismissButtonColorPref = findPreference(KEY_DISMISS_BUTTON_COLOR);
        mDisplayTextShadowPref = findPreference(KEY_ALARM_DISPLAY_TEXT_SHADOW);
        mDisplayAlarmActionMessagePref = findPreference(KEY_DISPLAY_ALARM_ACTION_MESSAGE);
        mDisplayAlarmTitleOnSingleLinePref = findPreference(KEY_DISPLAY_ALARM_TITLE_ON_SINGLE_LINE);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_RINGTONE_TITLE);
        mAlarmBackgroundImagePref = findPreference(KEY_ALARM_BACKGROUND_IMAGE);
        mAlarmBlurIntensityPref = findPreference(KEY_ALARM_BLUR_INTENSITY);
        mEnablePerAlarmBackgroundImagePref = findPreference(KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE);
        mAlarmPreviewPref = findPreference(KEY_ALARM_PREVIEW);

        mAlarmClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mAlarmClockStyleValues[0];
        mMaterialAnalogClock = mAlarmClockStyleValues[1];
        mDigitalClock = mAlarmClockStyleValues[2];

        if (savedInstanceState != null) {
            mPendingDialogPrefKey = savedInstanceState.getString(KEY_PENDING_DIALOG_PREF_KEY, null);
        }

        setupPreferences();

        checkSpecificImagesState();

        invalidatePreferencesVisibility();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_PENDING_DIALOG_PREF_KEY, mPendingDialogPrefKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPendingDialogPrefKey != null && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            triggerDisableSettingDialog();
        }

        restoreCustomFileDialogIfNeeded(
            KEY_ALARM_BACKGROUND_IMAGE, mAlarmBackgroundImagePref, imagePickerLauncher, this::onBackgroundImageDeleted);

        invalidatePreferencesVisibility();
    }

    @Override
    public void onDestroy() {
        mAlarmUpdateHandler = null;

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_ALARM_CLOCK_STYLE -> {
                final int clockIndex = mAlarmClockStylePref.findIndexOfValue((String) newValue);
                mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntries()[clockIndex]);
            }

            case KEY_ALARM_CLOCK_DIAL, KEY_ALARM_CLOCK_DIAL_MATERIAL, KEY_ALARM_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    final Context appContext = requireContext().getApplicationContext();
                    final ContentResolver cr = appContext.getContentResolver();
                    final int blurIntensity = SettingsDAO.getAlarmBlurIntensity(getPrefs());

                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);

                        for (Alarm alarm : currentAlarms) {
                            alarm.blurIntensity = blurIntensity;
                            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                        }
                    });
                } else {
                    triggerDisableSettingDialog();
                    return false;
                }
            }

            case KEY_DISPLAY_ALARM_SECOND_HAND, KEY_SWIPE_ACTION, KEY_DISPLAY_SNOOZE_SELECTOR,
                 KEY_ALARM_DISPLAY_TEXT_SHADOW, KEY_DISPLAY_RINGTONE_TITLE,
                 KEY_DISPLAY_ALARM_ACTION_MESSAGE, KEY_DISPLAY_ALARM_TITLE_ON_SINGLE_LINE ->
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
        switch (pref.getKey()) {
            case KEY_ALARM_BACKGROUND_IMAGE -> selectCustomFile(mAlarmBackgroundImagePref, imagePickerLauncher,
                SettingsDAO.getAlarmBackgroundImage(getPrefs()), KEY_ALARM_BACKGROUND_IMAGE, false, false,
                this::onBackgroundImageDeleted);

            case KEY_ALARM_PREVIEW -> {
                Intent previewIntent = new Intent(requireContext(), AlarmDisplayPreviewActivity.class);
                Calendar now = Calendar.getInstance();

                previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_HOUR, now.get(Calendar.HOUR_OF_DAY));
                previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_MINUTE, now.get(Calendar.MINUTE));

                ThemeUtils.startActivityWithTransition(requireContext(), previewIntent, SettingsDAO.isFadeTransitionsEnabled(getPrefs()));
            }
        }

        return true;
    }

    @Override
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        final boolean isAnalogClock = mAlarmClockStylePref.getValue().equals(mAnalogClock);
        final boolean isMaterialAnalogClock = mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock);
        final boolean isDigitalClock = mAlarmClockStylePref.getValue().equals(mDigitalClock);
        final boolean isSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(getPrefs());
        final boolean isSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(getPrefs());
        final boolean isSnoozeSelectorDisplayed = SettingsDAO.isSnoozeSelectorDisplayed(getPrefs());
        final boolean isTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(getPrefs());
        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && SettingsDAO.getDarkMode(getPrefs()).equals(AMOLED_DARK_MODE);

        return switch (key) {
            case KEY_ALARM_CLOCK_DIAL -> isAnalogClock;
            case KEY_ALARM_CLOCK_DIAL_MATERIAL -> isMaterialAnalogClock;
            case KEY_ALARM_BACKGROUND_AMOLED_COLOR -> isAmoledMode;
            case KEY_ALARM_BACKGROUND_COLOR -> !isAmoledMode;
            case KEY_ALARM_CLOCK_COLOR -> !isMaterialAnalogClock;
            case KEY_ALARM_ANALOG_CLOCK_SIZE, KEY_DISPLAY_ALARM_SECOND_HAND -> !isDigitalClock;
            case KEY_ALARM_CLOCK_SECOND_HAND, KEY_ALARM_SECOND_HAND_COLOR -> isAnalogClock && isSecondHandDisplayed;
            case KEY_SLIDE_ZONE_COLOR, KEY_ALARM_BUTTON_COLOR -> isSwipeActionEnabled;
            case KEY_SNOOZE_BUTTON_COLOR, KEY_DISMISS_BUTTON_COLOR -> !isSwipeActionEnabled;
            case KEY_SNOOZE_ZONE_COLOR, KEY_SNOOZE_MINUS_BUTTON_COLOR, KEY_SNOOZE_PLUS_BUTTON_COLOR, KEY_SNOOZE_SELECTOR_TEXT_COLOR,
                 KEY_SNOOZE_MINUS_SYMBOL_COLOR, KEY_SNOOZE_PLUS_SYMBOL_COLOR -> isSnoozeSelectorDisplayed;
            case KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE -> isDigitalClock;
            case KEY_ALARM_SHADOW_COLOR, KEY_ALARM_SHADOW_OFFSET -> isTextShadowDisplayed;
            case KEY_RINGTONE_TITLE_COLOR -> SettingsDAO.isRingtoneTitleDisplayed(getPrefs());
            case KEY_ALARM_BLUR_INTENSITY -> {
                if (SdkUtils.isBeforeAndroid12()) {
                    yield false;
                }

                if (!TextUtils.isEmpty(SettingsDAO.getAlarmBackgroundImage(getPrefs()))) {
                    yield true;
                }

                yield mHasSpecificImages;
            }
            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void setupPreferences() {
        mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntry());
        mAlarmClockStylePref.setOnPreferenceChangeListener(this);

        mAlarmClockDialPref.setSummary(mAlarmClockDialPref.getEntry());
        mAlarmClockDialPref.setOnPreferenceChangeListener(this);

        mAlarmClockDialMaterialPref.setSummary(mAlarmClockDialMaterialPref.getEntry());
        mAlarmClockDialMaterialPref.setOnPreferenceChangeListener(this);

        String activeAccentColor = ThemeUtils.getActiveAccentColor(
            requireContext(),
            SettingsDAO.isAutoNightAccentColorEnabled(getPrefs()),
            SettingsDAO.getNightAccentColor(getPrefs()),
            SettingsDAO.getAccentColor(getPrefs())
        );
        int defaultBackgroundColor = ThemeUtils.getNightBackgroundColor(requireContext(), activeAccentColor);
        mBackgroundColorPref.setDefaultValue(defaultBackgroundColor);

        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mAlarmClockSecondHandPref.setSummary(mAlarmClockSecondHandPref.getEntry());
        mAlarmClockSecondHandPref.setOnPreferenceChangeListener(this);

        mSwipeActionPref.setOnPreferenceChangeListener(this);
        mDisplaySnoozeSelectorPref.setOnPreferenceChangeListener(this);

        int color = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);
        mAlarmSecondHandColorPref.setDefaultValue(color);
        mAlarmButtonColorPref.setDefaultValue(color);
        mSnoozeButtonColorPref.setDefaultValue(color);
        mDismissButtonColorPref.setDefaultValue(color);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);
        mDisplayAlarmActionMessagePref.setOnPreferenceChangeListener(this);
        mDisplayAlarmTitleOnSingleLinePref.setOnPreferenceChangeListener(this);
        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        if (SettingsDAO.getAlarmBackgroundImage(getPrefs()) == null) {
            mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
        } else {
            mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
        }

        mAlarmBackgroundImagePref.setOnPreferenceClickListener(this);
        mEnablePerAlarmBackgroundImagePref.setOnPreferenceChangeListener(this);
        mAlarmPreviewPref.setOnPreferenceClickListener(this);
    }

    private void triggerDisableSettingDialog() {
        final Context appContext = requireContext().getApplicationContext();

        AppExecutors.getDiskIO().execute(() -> {
            final CustomizationState state = getAlarmCustomizationsState(appContext);

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || isDetached()) {
                    return;
                }

                if (state.hasAny()) {
                    mPendingDialogPrefKey = KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE;
                    showDisablePerAlarmSettingDialog(state);
                } else {
                    getPrefs().edit().putBoolean(KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE, false).apply();
                    mEnablePerAlarmBackgroundImagePref.setChecked(false);
                }
            });
        });
    }

    private void showDisablePerAlarmSettingDialog(@NonNull CustomizationState state) {
        String confirmAction = getString(R.string.confirm_action_prompt);
        String dialogMessage;

        if (state.hasSpecificImages) {
            String blurIntensityMessage = getString(R.string.blur_intensity_dialog_message_1);
            dialogMessage = getString(R.string.enable_per_alarm_background_image_dialog_message, blurIntensityMessage, confirmAction);
        } else {
            dialogMessage = getString(R.string.blur_intensity_dialog_message_2, confirmAction);
        }

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error),
            getString(R.string.warning),
            dialogMessage,
            null,
            getString(android.R.string.ok),
            (d, w) -> {
                final Context appContext = requireContext().getApplicationContext();
                final ContentResolver cr = appContext.getContentResolver();
                final int blurIntensity = SettingsDAO.getAlarmBlurIntensity(getPrefs());

                AppExecutors.getDiskIO().execute(() -> {
                    List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);

                    for (Alarm alarm : currentAlarms) {
                        // Delete only specific background images
                        if (!TextUtils.isEmpty(alarm.backgroundImage) && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                            FileUtils.clearFile(alarm.backgroundImage);
                        }

                        alarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
                        alarm.blurIntensity = blurIntensity;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }

                    AppExecutors.getMainThread().post(() -> {
                        CustomToast.show(
                            appContext, getAccentStyle(), getGeneralTypeface(), R.string.background_image_toast_message_deleted);
                        checkSpecificImagesState();
                    });
                });

                getPrefs().edit().putBoolean(KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE, false).apply();
                mEnablePerAlarmBackgroundImagePref.setChecked(false);
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mPendingDialogPrefKey = null)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void checkSpecificImagesState() {
        if (SdkUtils.isBeforeAndroid12() || !TextUtils.isEmpty(SettingsDAO.getAlarmBackgroundImage(getPrefs()))) {
            return;
        }

        final Context appContext = requireContext().getApplicationContext();

        AppExecutors.getDiskIO().execute(() -> {
            final CustomizationState state = getAlarmCustomizationsState(appContext);
            final boolean finalHasSpecificImages = state.hasSpecificImages;

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || isDetached() || mAlarmBlurIntensityPref == null) {
                    return;
                }

                mHasSpecificImages = finalHasSpecificImages;

                invalidatePreferencesVisibility();
            });
        });
    }

    private void onBackgroundImageDeleted() {
        // If the global image is deleted, the specific alarm images are deleted only if the
        // "Use a custom background image for each alarm" setting is disabled.
        if (!SettingsDAO.isPerAlarmBackgroundImageEnable(getPrefs())) {
            mHasSpecificImages = false;

            invalidatePreferencesVisibility();

            final Context appContext = requireContext().getApplicationContext();
            final ContentResolver cr = appContext.getContentResolver();

            AppExecutors.getDiskIO().execute(() -> {
                List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);

                for (Alarm alarm : currentAlarms) {
                    if (!TextUtils.isEmpty(alarm.backgroundImage)
                        && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                        FileUtils.clearFile(alarm.backgroundImage);
                    }
                    alarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                }
            });
        } else {
            checkSpecificImagesState();
        }
    }

    /**
     * Checks which specific customizations are applied to the alarms background images and the blur intensity.
     */
    @NonNull
    private CustomizationState getAlarmCustomizationsState(@NonNull Context context) {
        CustomizationState state = new CustomizationState();

        try {
            List<Alarm> currentAlarms = Alarm.getAlarms(context.getContentResolver(), null);
            int globalBlur = SettingsDAO.getAlarmBlurIntensity(getPrefs());

            for (Alarm alarm : currentAlarms) {
                if (!state.hasSpecificImages
                    && !TextUtils.isEmpty(alarm.backgroundImage)
                    && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                    state.hasSpecificImages = true;
                }

                if (!state.hasSpecificBlur && alarm.blurIntensity != globalBlur) {
                    state.hasSpecificBlur = true;
                }

                if (state.hasSpecificImages && state.hasSpecificBlur) {
                    break;
                }
            }
        } catch (Exception e) {
            LogUtils.e("Error checking for specific alarm customizations", e);
        }

        return state;
    }

    /**
     * Utility class for storing the state of background image customizations and blur intensity.
     */
    private static class CustomizationState {
        boolean hasSpecificImages = false;
        boolean hasSpecificBlur = false;

        boolean hasAny() {
            return hasSpecificImages || hasSpecificBlur;
        }
    }

}

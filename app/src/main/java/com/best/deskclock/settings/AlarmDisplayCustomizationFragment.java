// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.OVERRIDE_TRANSITION_OPEN;
import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.DataModel.ClockStyle;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.List;

public class AlarmDisplayCustomizationFragment extends ScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_PENDING_DIALOG_PREF_KEY = "pending_dialog_pref_key";

    private String mPendingDialogPrefKey = null;

    private AlarmUpdateHandler mAlarmUpdateHandler;

    String[] mAlarmClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;
    String mDigitalClock;

    ListPreference mAlarmClockStylePref;
    ListPreference mAlarmClockDialPref;
    ListPreference mAlarmClockDialMaterialPref;
    CustomSliderPreference mAnalogClockSizePref;
    ListPreference mAlarmClockSecondHandPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mSwipeActionPref;
    SwitchPreferenceCompat mDisplaySnoozeSelectorPref;
    ColorPickerPreference mBackgroundColorPref;
    ColorPickerPreference mBackgroundAmoledColorPref;
    ColorPickerPreference mAlarmClockColorPref;
    ColorPickerPreference mAlarmSecondHandColorPref;
    ColorPickerPreference mSlideZoneColorPref;
    ColorPickerPreference mAlarmButtonColorPref;
    ColorPickerPreference mSnoozeTitleColorPref;
    ColorPickerPreference mSnoozeButtonColorPref;
    ColorPickerPreference mDismissTitleColorPref;
    ColorPickerPreference mDismissButtonColorPref;
    ColorPickerPreference mSnoozeZoneColorPref;
    ColorPickerPreference mSnoozeMinusButtonColorPref;
    ColorPickerPreference mSnoozePlusButtonColorPref;
    ColorPickerPreference mSnoozeSelectorTextColorPref;
    ColorPickerPreference mSnoozeMinusSymbolColorPref;
    ColorPickerPreference mSnoozePlusSymbolColorPref;
    CustomSliderPreference mAlarmDigitalClockFontSizePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    ColorPickerPreference mShadowColorPref;
    CustomSliderPreference mShadowOffsetPref;
    SwitchPreferenceCompat mDisplayAlarmActionMessagePref;
    SwitchPreferenceCompat mDisplayAlarmTitleOnSingleLinePref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    ColorPickerPreference mRingtoneTitleColorPref;
    Preference mAlarmBackgroundImagePref;
    CustomSliderPreference mAlarmBlurIntensityPref;
    SwitchPreferenceCompat mEnablePerAlarmBackgroundImagePref;
    Preference mAlarmPreviewPref;

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

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = FileUtils.toSafeFileName(FILE_ALARM_BACKGROUND);
            String oldImagePath = mPrefs.getString(KEY_ALARM_BACKGROUND_IMAGE, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old image if it exists
                FileUtils.clearFile(oldImagePath);

                // Copy the new image to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_ALARM_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, R.string.background_image_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, "Error importing image");
                    }

                    if (!isAdded()
                        || mAlarmBackgroundImagePref == null
                        || mAlarmBlurIntensityPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                        mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12());
                    } else {
                        mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                        updateBlurPreferenceVisibility();
                    }
                });
            });
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.display_settings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlarmUpdateHandler = new AlarmUpdateHandler(requireContext(), null, null);

        addPreferencesFromResource(R.xml.settings_alarm_display);

        mAlarmClockStylePref = findPreference(KEY_ALARM_CLOCK_STYLE);
        mAlarmClockDialPref = findPreference(KEY_ALARM_CLOCK_DIAL);
        mAlarmClockDialMaterialPref = findPreference(KEY_ALARM_CLOCK_DIAL_MATERIAL);
        mAnalogClockSizePref = findPreference(KEY_ALARM_ANALOG_CLOCK_SIZE);
        mDisplaySecondsPref = findPreference(KEY_DISPLAY_ALARM_SECOND_HAND);
        mAlarmClockSecondHandPref = findPreference(KEY_ALARM_CLOCK_SECOND_HAND);
        mSwipeActionPref = findPreference(KEY_SWIPE_ACTION);
        mDisplaySnoozeSelectorPref = findPreference(KEY_DISPLAY_SNOOZE_SELECTOR);
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
        mSnoozeZoneColorPref = findPreference(KEY_SNOOZE_ZONE_COLOR);
        mSnoozeMinusButtonColorPref = findPreference(KEY_SNOOZE_MINUS_BUTTON_COLOR);
        mSnoozePlusButtonColorPref = findPreference(KEY_SNOOZE_PLUS_BUTTON_COLOR);
        mSnoozeSelectorTextColorPref = findPreference(KEY_SNOOZE_SELECTOR_TEXT_COLOR);
        mSnoozeMinusSymbolColorPref = findPreference(KEY_SNOOZE_MINUS_SYMBOL_COLOR);
        mSnoozePlusSymbolColorPref = findPreference(KEY_SNOOZE_PLUS_SYMBOL_COLOR);
        mAlarmDigitalClockFontSizePref = findPreference(KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE);
        mDisplayTextShadowPref = findPreference(KEY_ALARM_DISPLAY_TEXT_SHADOW);
        mShadowColorPref = findPreference(KEY_ALARM_SHADOW_COLOR);
        mShadowOffsetPref = findPreference(KEY_ALARM_SHADOW_OFFSET);
        mDisplayAlarmActionMessagePref = findPreference(KEY_DISPLAY_ALARM_ACTION_MESSAGE);
        mDisplayAlarmTitleOnSingleLinePref = findPreference(KEY_DISPLAY_ALARM_TITLE_ON_SINGLE_LINE);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_RINGTONE_TITLE);
        mRingtoneTitleColorPref = findPreference(KEY_RINGTONE_TITLE_COLOR);
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

        restoreCustomFileDialogIfNeeded(KEY_ALARM_BACKGROUND_IMAGE, mAlarmBackgroundImagePref, imagePickerLauncher, () ->
            mAlarmBlurIntensityPref.setVisible(false)
        );
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mAlarmClockStylePref, mAlarmClockDialPref, mAlarmClockDialMaterialPref, mAnalogClockSizePref,
            mAlarmClockSecondHandPref, mDisplaySecondsPref, mSwipeActionPref, mDisplaySnoozeSelectorPref, mBackgroundColorPref,
            mBackgroundAmoledColorPref, mAlarmClockColorPref, mAlarmSecondHandColorPref, mSlideZoneColorPref, mAlarmButtonColorPref,
            mSnoozeTitleColorPref, mSnoozeButtonColorPref, mDismissTitleColorPref, mDismissButtonColorPref, mSnoozeZoneColorPref,
            mSnoozeMinusButtonColorPref, mSnoozePlusButtonColorPref, mSnoozeSelectorTextColorPref, mSnoozeMinusSymbolColorPref,
            mSnoozePlusSymbolColorPref, mAlarmDigitalClockFontSizePref, mDisplayTextShadowPref, mShadowColorPref, mShadowOffsetPref,
            mDisplayAlarmActionMessagePref, mDisplayAlarmTitleOnSingleLinePref, mDisplayRingtoneTitlePref, mRingtoneTitleColorPref,
            mAlarmBackgroundImagePref, mAlarmBlurIntensityPref, mEnablePerAlarmBackgroundImagePref, mAlarmPreviewPref
        );

        nullifyAllPrefs();

        mAlarmUpdateHandler = null;

        super.onDestroy();
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
                mAnalogClockSizePref.setVisible(!isDigitalClock);
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

                mAlarmClockSecondHandPref.setVisible(isSecondHandDisplayed && alarmClockStyle == ClockStyle.ANALOG);
                mAlarmSecondHandColorPref.setVisible(isSecondHandDisplayed && alarmClockStyle != ClockStyle.ANALOG_MATERIAL);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SWIPE_ACTION -> {
                boolean isSwipeActionEnabled = (boolean) newValue;

                mSlideZoneColorPref.setVisible(isSwipeActionEnabled);
                mSnoozeTitleColorPref.setVisible(isSwipeActionEnabled);
                mSnoozeButtonColorPref.setVisible(!isSwipeActionEnabled);
                mDismissTitleColorPref.setVisible(isSwipeActionEnabled);
                mDismissButtonColorPref.setVisible(!isSwipeActionEnabled);
                mAlarmButtonColorPref.setVisible(isSwipeActionEnabled);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_DISPLAY_SNOOZE_SELECTOR -> {
                boolean isSnoozeSelectorDisplayed = (boolean) newValue;

                mSnoozeZoneColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozeMinusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozePlusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozeSelectorTextColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozeMinusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozePlusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_ALARM_DISPLAY_TEXT_SHADOW -> {
                boolean isTextShadowDisplayed = (boolean) newValue;
                mShadowColorPref.setVisible(isTextShadowDisplayed);
                mShadowOffsetPref.setVisible(isTextShadowDisplayed);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_DISPLAY_RINGTONE_TITLE -> {
                mRingtoneTitleColorPref.setVisible((boolean) newValue);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);

                        for (Alarm alarm : currentAlarms) {
                            alarm.blurIntensity = SettingsDAO.getAlarmBlurIntensity(mPrefs);
                            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                        }
                    });
                } else {
                    triggerDisableSettingDialog();
                    return false;
                }
            }

            case KEY_DISPLAY_ALARM_ACTION_MESSAGE, KEY_DISPLAY_ALARM_TITLE_ON_SINGLE_LINE ->
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
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
            case KEY_ALARM_BACKGROUND_IMAGE -> selectCustomFile(mAlarmBackgroundImagePref, imagePickerLauncher,
                SettingsDAO.getAlarmBackgroundImage(mPrefs), KEY_ALARM_BACKGROUND_IMAGE, false, () -> {
                // Actions to perform when deleting a background image

                // If the global image is deleted, the specific alarm images are deleted only if the
                // "Use a custom background image for each alarm" setting is disabled.
                if (!SettingsDAO.isPerAlarmBackgroundImageEnable(mPrefs)) {
                    mAlarmBlurIntensityPref.setVisible(false);

                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);

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
                    updateBlurPreferenceVisibility();
                }
            });

            case KEY_ALARM_PREVIEW -> {
                startActivity(new Intent(context, AlarmDisplayPreviewActivity.class));
                if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
                    if (SdkUtils.isAtLeastAndroid14()) {
                        requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
                    } else {
                        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                } else {
                    if (SdkUtils.isAtLeastAndroid14()) {
                        requireActivity().overrideActivityTransition(
                            OVERRIDE_TRANSITION_OPEN, R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
                    } else {
                        requireActivity().overridePendingTransition(R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
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
        final boolean isSnoozeSelectorDisplayed = SettingsDAO.isSnoozeSelectorDisplayed(mPrefs);
        final boolean isTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(mPrefs);

        mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntry());
        mAlarmClockStylePref.setOnPreferenceChangeListener(this);

        mAlarmClockDialPref.setVisible(isAnalogClock);
        mAlarmClockDialPref.setSummary(mAlarmClockDialPref.getEntry());
        mAlarmClockDialPref.setOnPreferenceChangeListener(this);

        mAlarmClockDialMaterialPref.setVisible(isMaterialAnalogClock);
        mAlarmClockDialMaterialPref.setSummary(mAlarmClockDialMaterialPref.getEntry());
        mAlarmClockDialMaterialPref.setOnPreferenceChangeListener(this);

        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE);
        mBackgroundAmoledColorPref.setVisible(isAmoledMode);

        mBackgroundColorPref.setVisible(!isAmoledMode);

        mAlarmClockColorPref.setVisible(!isMaterialAnalogClock);

        mAnalogClockSizePref.setVisible(!isDigitalClock);

        mDisplaySecondsPref.setVisible(!isDigitalClock);
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mAlarmClockSecondHandPref.setVisible(isAnalogClock && isSecondHandDisplayed);
        mAlarmClockSecondHandPref.setSummary(mAlarmClockSecondHandPref.getEntry());
        mAlarmClockSecondHandPref.setOnPreferenceChangeListener(this);

        mSwipeActionPref.setOnPreferenceChangeListener(this);

        mDisplaySnoozeSelectorPref.setOnPreferenceChangeListener(this);

        int color = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);
        mAlarmSecondHandColorPref.setVisible(isAnalogClock && isSecondHandDisplayed);
        mAlarmSecondHandColorPref.setDefaultValue(color);

        mSlideZoneColorPref.setVisible(isSwipeActionEnabled);

        mAlarmButtonColorPref.setVisible(isSwipeActionEnabled);
        mAlarmButtonColorPref.setDefaultValue(color);

        mSnoozeTitleColorPref.setVisible(isSwipeActionEnabled);

        mSnoozeButtonColorPref.setVisible(!isSwipeActionEnabled);
        mSnoozeButtonColorPref.setDefaultValue(color);

        mDismissTitleColorPref.setVisible(isSwipeActionEnabled);

        mDismissButtonColorPref.setVisible(!isSwipeActionEnabled);
        mDismissButtonColorPref.setDefaultValue(color);

        mSnoozeZoneColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozeMinusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozePlusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozeSelectorTextColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozeMinusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozePlusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);

        mAlarmDigitalClockFontSizePref.setVisible(isDigitalClock);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShadowColorPref.setVisible(isTextShadowDisplayed);

        mShadowOffsetPref.setVisible(isTextShadowDisplayed);

        mDisplayAlarmActionMessagePref.setOnPreferenceChangeListener(this);

        mDisplayAlarmTitleOnSingleLinePref.setOnPreferenceChangeListener(this);

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mRingtoneTitleColorPref.setVisible(SettingsDAO.isRingtoneTitleDisplayed(mPrefs));

        if (SettingsDAO.getAlarmBackgroundImage(mPrefs) == null) {
            mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
        } else {
            mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
        }

        mAlarmBackgroundImagePref.setOnPreferenceClickListener(this);

        updateBlurPreferenceVisibility();

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
                    mPrefs.edit().putBoolean(KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE, false).apply();
                    mEnablePerAlarmBackgroundImagePref.setChecked(false);
                }
            });
        });
    }

    private void showDisablePerAlarmSettingDialog(CustomizationState state) {
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

                AppExecutors.getDiskIO().execute(() -> {
                    List<Alarm> currentAlarms = Alarm.getAlarms(appContext.getContentResolver(), null);

                    for (Alarm alarm : currentAlarms) {
                        // Delete only specific background images
                        if (!TextUtils.isEmpty(alarm.backgroundImage) && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                            FileUtils.clearFile(alarm.backgroundImage);
                        }

                        alarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
                        alarm.blurIntensity = SettingsDAO.getAlarmBlurIntensity(mPrefs);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }

                    AppExecutors.getMainThread().post(() -> {
                        CustomToast.show(appContext, R.string.background_image_toast_message_deleted);
                        updateBlurPreferenceVisibility();
                    });
                });

                mPrefs.edit().putBoolean(KEY_ENABLE_PER_ALARM_BACKGROUND_IMAGE, false).apply();
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

    private void updateBlurPreferenceVisibility() {
        if (SdkUtils.isBeforeAndroid12()) {
            mAlarmBlurIntensityPref.setVisible(false);
            return;
        }

        String globalImagePath = SettingsDAO.getAlarmBackgroundImage(mPrefs);

        if (!TextUtils.isEmpty(globalImagePath)) {
            mAlarmBlurIntensityPref.setVisible(true);
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

                mAlarmBlurIntensityPref.setVisible(finalHasSpecificImages);
            });
        });
    }

    /**
     * Checks which specific customizations are applied to the alarms background images and the blur intensity.
     */
    private CustomizationState getAlarmCustomizationsState(Context context) {
        CustomizationState state = new CustomizationState();

        try {
            List<Alarm> currentAlarms = Alarm.getAlarms(context.getContentResolver(), null);
            int globalBlur = SettingsDAO.getAlarmBlurIntensity(mPrefs);

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

    private void nullifyAllPrefs() {
        mAlarmClockStylePref = null;
        mAlarmClockDialPref = null;
        mAlarmClockDialMaterialPref = null;
        mAnalogClockSizePref = null;
        mAlarmClockSecondHandPref = null;
        mDisplaySecondsPref = null;
        mSwipeActionPref = null;
        mDisplaySnoozeSelectorPref = null;
        mBackgroundColorPref = null;
        mBackgroundAmoledColorPref = null;
        mAlarmClockColorPref = null;
        mAlarmSecondHandColorPref = null;
        mSlideZoneColorPref = null;
        mAlarmButtonColorPref = null;
        mSnoozeTitleColorPref = null;
        mSnoozeButtonColorPref = null;
        mDismissTitleColorPref = null;
        mDismissButtonColorPref = null;
        mSnoozeZoneColorPref = null;
        mSnoozeMinusButtonColorPref = null;
        mSnoozePlusButtonColorPref = null;
        mSnoozeSelectorTextColorPref = null;
        mSnoozeMinusSymbolColorPref = null;
        mSnoozePlusSymbolColorPref = null;
        mAlarmDigitalClockFontSizePref = null;
        mDisplayTextShadowPref = null;
        mShadowColorPref = null;
        mShadowOffsetPref = null;
        mDisplayAlarmActionMessagePref = null;
        mDisplayAlarmTitleOnSingleLinePref = null;
        mDisplayRingtoneTitlePref = null;
        mRingtoneTitleColorPref = null;
        mAlarmBackgroundImagePref = null;
        mAlarmBlurIntensityPref = null;
        mEnablePerAlarmBackgroundImagePref = null;
        mAlarmPreviewPref = null;

        mAlarmClockStyleValues = null;
        mAnalogClock = null;
        mMaterialAnalogClock = null;
        mDigitalClock = null;
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

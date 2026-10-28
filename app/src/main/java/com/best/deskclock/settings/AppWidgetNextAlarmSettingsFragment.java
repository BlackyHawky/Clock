// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.NextAlarmAppWidgetProvider;

public class AppWidgetNextAlarmSettingsFragment extends BaseSettingsScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;
    SwitchPreferenceCompat mDefaultTitleColorPref;
    ColorPickerPreference mCustomTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmTitleColorPref;
    ColorPickerPreference mCustomAlarmTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmColorPref;
    ColorPickerPreference mCustomAlarmColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.next_alarm_widget);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_next_alarm_widget);

        mDisplayTextUppercasePref = findPreference(KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mDisplayTextShadowPref = findPreference(KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_SHADOW);
        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mApplyHorizontalPaddingPref = findPreference(KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOM_BACKGROUND_COLOR);
        mDefaultTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR);
        mCustomTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR);
        mDefaultAlarmTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR);
        mCustomAlarmTitleColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR);
        mDefaultAlarmColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR);
        mCustomAlarmColorPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR);

        setupPreferences();

        WidgetUtils.addFinishOnBackPressedIfLaunchedFromWidget(this);

        requireActivity().setResult(Activity.RESULT_CANCELED);

        Intent intent = requireActivity().getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
            }
        }

        invalidatePreferencesVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();

        saveCheckedPreferenceStates();

        updateNextAlarmWidget();

        invalidatePreferencesVisibility();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND,
                 KEY_NEXT_ALARM_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS,
                 KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_UPPERCASE,
                 KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING,
                 KEY_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR,
                 KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR,
                 KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR,
                 KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), NextAlarmAppWidgetProvider.class);

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
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        final boolean displayBackground = mShowBackgroundOnDigitalWidgetPref.isChecked();
        final boolean isCustomBgColor = !mDefaultBackgroundColorPref.isChecked();
        final boolean isRadiusCustomizable = mCustomizeBackgroundCornerRadiusPref.isChecked();

        return switch (key) {
            case KEY_NEXT_ALARM_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> SdkUtils.isAtLeastAndroid12()
                ? displayBackground
                : (displayBackground && isCustomBgColor);

            case KEY_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS -> SdkUtils.isAtLeastAndroid12()
                ? (displayBackground && isRadiusCustomizable)
                : (displayBackground && isCustomBgColor && isRadiusCustomizable);

            case KEY_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR -> displayBackground;

            case KEY_NEXT_ALARM_WIDGET_CUSTOM_BACKGROUND_COLOR -> displayBackground && isCustomBgColor;

            case KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR -> !mDefaultTitleColorPref.isChecked();

            case KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR -> !mDefaultAlarmTitleColorPref.isChecked();

            case KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR -> !mDefaultAlarmColorPref.isChecked();

            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnNextAlarmWidget(getPrefs()));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnNextAlarmWidget(getPrefs()));
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnNextAlarmWidget(getPrefs()));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isNextAlarmWidgetBackgroundCornerRadiusCustomizable(getPrefs()));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isNextAlarmWidgetHorizontalPaddingApplied(getPrefs()));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultBackgroundColor(getPrefs()));
        mDefaultTitleColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultTitleColor(getPrefs()));
        mDefaultAlarmTitleColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(getPrefs()));
        mDefaultAlarmColorPref.setChecked(WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(getPrefs()));
    }

    private void updateNextAlarmWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        NextAlarmAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}

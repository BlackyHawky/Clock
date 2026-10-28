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
import com.best.deskclock.widgets.VerticalAppWidgetProvider;

public class AppWidgetVerticalSettingsFragment extends BaseSettingsScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;
    SwitchPreferenceCompat mDefaultHoursColorPref;
    ColorPickerPreference mCustomHoursColorPref;
    SwitchPreferenceCompat mDefaultMinutesColorPref;
    ColorPickerPreference mCustomMinutesColorPref;
    SwitchPreferenceCompat mDefaultDateColorPref;
    ColorPickerPreference mCustomDateColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmColorPref;
    ColorPickerPreference mCustomNextAlarmColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.vertical_widget);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_vertical_widget);

        mDisplayTextUppercasePref = findPreference(KEY_VERTICAL_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mDisplayTextShadowPref = findPreference(KEY_VERTICAL_WIDGET_DISPLAY_TEXT_SHADOW);
        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_VERTICAL_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mDisplayDatePref = findPreference(KEY_VERTICAL_WIDGET_DISPLAY_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_VERTICAL_WIDGET_DISPLAY_NEXT_ALARM);
        mApplyHorizontalPaddingPref = findPreference(KEY_VERTICAL_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_VERTICAL_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOM_BACKGROUND_COLOR);
        mDefaultHoursColorPref = findPreference(KEY_VERTICAL_WIDGET_DEFAULT_HOURS_COLOR);
        mCustomHoursColorPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOM_HOURS_COLOR);
        mDefaultMinutesColorPref = findPreference(KEY_VERTICAL_WIDGET_DEFAULT_MINUTES_COLOR);
        mCustomMinutesColorPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOM_MINUTES_COLOR);
        mDefaultDateColorPref = findPreference(KEY_VERTICAL_WIDGET_DEFAULT_DATE_COLOR);
        mCustomDateColorPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOM_DATE_COLOR);
        mDefaultNextAlarmColorPref = findPreference(KEY_VERTICAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
        mCustomNextAlarmColorPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);

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

        updateVerticalDigitalWidget();

        invalidatePreferencesVisibility();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_VERTICAL_WIDGET_DISPLAY_BACKGROUND, KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS,
                 KEY_VERTICAL_WIDGET_DISPLAY_DATE, KEY_VERTICAL_WIDGET_DISPLAY_NEXT_ALARM, KEY_VERTICAL_WIDGET_DISPLAY_TEXT_UPPERCASE,
                 KEY_VERTICAL_WIDGET_DISPLAY_TEXT_SHADOW, KEY_VERTICAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                 KEY_VERTICAL_WIDGET_DEFAULT_BACKGROUND_COLOR, KEY_VERTICAL_WIDGET_DEFAULT_HOURS_COLOR,
                 KEY_VERTICAL_WIDGET_DEFAULT_MINUTES_COLOR, KEY_VERTICAL_WIDGET_DEFAULT_DATE_COLOR,
                 KEY_VERTICAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), VerticalAppWidgetProvider.class);

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
        final boolean displayDate = mDisplayDatePref.isChecked();
        final boolean displayNextAlarm = mDisplayNextAlarmPref.isChecked();

        return switch (key) {
            case KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> SdkUtils.isAtLeastAndroid12()
                ? displayBackground
                : (displayBackground && isCustomBgColor);

            case KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS -> SdkUtils.isAtLeastAndroid12()
                ? (displayBackground && isRadiusCustomizable)
                : (displayBackground && isCustomBgColor && isRadiusCustomizable);

            case KEY_VERTICAL_WIDGET_DEFAULT_BACKGROUND_COLOR -> displayBackground;

            case KEY_VERTICAL_WIDGET_CUSTOM_BACKGROUND_COLOR -> displayBackground && isCustomBgColor;

            case KEY_VERTICAL_WIDGET_CUSTOM_HOURS_COLOR -> !mDefaultHoursColorPref.isChecked();

            case KEY_VERTICAL_WIDGET_CUSTOM_MINUTES_COLOR -> !mDefaultMinutesColorPref.isChecked();

            case KEY_VERTICAL_WIDGET_DEFAULT_DATE_COLOR -> displayDate;

            case KEY_VERTICAL_WIDGET_CUSTOM_DATE_COLOR -> displayDate && !mDefaultDateColorPref.isChecked();

            case KEY_VERTICAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> displayNextAlarm;

            case KEY_VERTICAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR -> displayNextAlarm && !mDefaultNextAlarmColorPref.isChecked();

            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultHoursColorPref.setOnPreferenceChangeListener(this);

        mCustomHoursColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinutesColorPref.setOnPreferenceChangeListener(this);

        mCustomMinutesColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnVerticalWidget(getPrefs()));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnVerticalWidget(getPrefs()));
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnVerticalWidget(getPrefs()));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isVerticalWidgetBackgroundCornerRadiusCustomizable(getPrefs()));
        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnVerticalWidget(getPrefs()));
        mDisplayNextAlarmPref.setChecked(WidgetDAO.isNextAlarmDisplayedOnVerticalWidget(getPrefs()));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isVerticalWidgetHorizontalPaddingApplied(getPrefs()));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isVerticalWidgetDefaultBackgroundColor(getPrefs()));
        mDefaultHoursColorPref.setChecked(WidgetDAO.isVerticalWidgetDefaultHoursColor(getPrefs()));
        mDefaultMinutesColorPref.setChecked(WidgetDAO.isVerticalWidgetDefaultMinutesColor(getPrefs()));
        mDefaultDateColorPref.setChecked(WidgetDAO.isVerticalWidgetDefaultDateColor(getPrefs()));
        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isVerticalWidgetDefaultNextAlarmColor(getPrefs()));
    }

    private void updateVerticalDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        VerticalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}

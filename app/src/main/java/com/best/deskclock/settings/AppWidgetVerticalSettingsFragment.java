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
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
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
    CustomSliderPreference mBackgroundCornerRadiusPref;
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
        mBackgroundCornerRadiusPref = findPreference(KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS);
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
    }

    @Override
    public void onResume() {
        super.onResume();

        saveCheckedPreferenceStates();

        updateVerticalDigitalWidget();
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mDisplayTextUppercasePref, mDisplayTextShadowPref, mShowBackgroundOnDigitalWidgetPref,
            mCustomizeBackgroundCornerRadiusPref, mBackgroundCornerRadiusPref, mDisplayDatePref, mDisplayNextAlarmPref,
            mApplyHorizontalPaddingPref, mDefaultBackgroundColorPref, mCustomBackgroundColorPref, mDefaultHoursColorPref,
            mCustomHoursColorPref, mDefaultMinutesColorPref, mCustomMinutesColorPref, mDefaultDateColorPref, mCustomDateColorPref,
            mDefaultNextAlarmColorPref, mCustomNextAlarmColorPref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_VERTICAL_WIDGET_DISPLAY_BACKGROUND -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isVerticalWidgetDefaultBackgroundColor(getPrefs());
                boolean isRadiusCustomizable = WidgetDAO.isVerticalWidgetBackgroundCornerRadiusCustomizable(getPrefs());

                mCustomizeBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground
                    : displayBackground && isCustomColor);
                mBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground && isRadiusCustomizable
                    : displayBackground && isCustomColor && isRadiusCustomizable);
                mDefaultBackgroundColorPref.setVisible(displayBackground);
                mCustomBackgroundColorPref.setVisible(displayBackground && isCustomColor);
            }

            case KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
            }

            case KEY_VERTICAL_WIDGET_DISPLAY_DATE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mDefaultDateColorPref.setVisible((boolean) newValue);
                mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                    && !WidgetDAO.isVerticalWidgetDefaultDateColor(getPrefs()));
            }

            case KEY_VERTICAL_WIDGET_DISPLAY_NEXT_ALARM -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mDefaultNextAlarmColorPref.setVisible((boolean) newValue);
                mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                    && !WidgetDAO.isVerticalWidgetDefaultNextAlarmColor(getPrefs()));
            }

            case KEY_VERTICAL_WIDGET_DISPLAY_TEXT_UPPERCASE,
                 KEY_VERTICAL_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_VERTICAL_WIDGET_APPLY_HORIZONTAL_PADDING ->
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_VERTICAL_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnVerticalWidget(getPrefs());
                boolean isRadiusCustomizable = WidgetDAO.isVerticalWidgetBackgroundCornerRadiusCustomizable(getPrefs());

                mCustomBackgroundColorPref.setVisible(isCustomColor);

                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
                }
            }

            case KEY_VERTICAL_WIDGET_DEFAULT_HOURS_COLOR -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomHoursColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_VERTICAL_WIDGET_DEFAULT_MINUTES_COLOR -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomMinutesColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_VERTICAL_WIDGET_DEFAULT_DATE_COLOR -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomDateColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_VERTICAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomNextAlarmColorPref.setVisible(!(boolean) newValue);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), VerticalAppWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnVerticalWidget(getPrefs());
        boolean isBackgroundCornerRadiusCustomizable =
            WidgetDAO.isVerticalWidgetBackgroundCornerRadiusCustomizable(getPrefs());
        boolean isCustomColor = !WidgetDAO.isVerticalWidgetDefaultBackgroundColor(getPrefs());

        if (SdkUtils.isAtLeastAndroid12()) {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isBackgroundCornerRadiusCustomizable);
        } else {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isCustomColor);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible
                && isCustomColor
                && isBackgroundCornerRadiusCustomizable);
        }

        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setVisible(isBackgroundVisible);
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setVisible(isBackgroundVisible && isCustomColor);
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultHoursColorPref.setOnPreferenceChangeListener(this);

        mCustomHoursColorPref.setVisible(!WidgetDAO.isVerticalWidgetDefaultHoursColor(getPrefs()));
        mCustomHoursColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinutesColorPref.setOnPreferenceChangeListener(this);

        mCustomMinutesColorPref.setVisible(!WidgetDAO.isVerticalWidgetDefaultMinutesColor(getPrefs()));
        mCustomMinutesColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setVisible(WidgetDAO.isDateDisplayedOnVerticalWidget(getPrefs()));
        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
            && !WidgetDAO.isVerticalWidgetDefaultDateColor(getPrefs()));
        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setVisible(WidgetDAO.isNextAlarmDisplayedOnVerticalWidget(getPrefs()));
        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
            && !WidgetDAO.isVerticalWidgetDefaultNextAlarmColor(getPrefs()));
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

    private void nullifyAllPrefs() {
        mDisplayTextUppercasePref = null;
        mDisplayTextShadowPref = null;
        mShowBackgroundOnDigitalWidgetPref = null;
        mCustomizeBackgroundCornerRadiusPref = null;
        mBackgroundCornerRadiusPref = null;
        mDisplayDatePref = null;
        mDisplayNextAlarmPref = null;
        mApplyHorizontalPaddingPref = null;
        mDefaultBackgroundColorPref = null;
        mCustomBackgroundColorPref = null;
        mDefaultHoursColorPref = null;
        mCustomHoursColorPref = null;
        mDefaultMinutesColorPref = null;
        mCustomMinutesColorPref = null;
        mDefaultDateColorPref = null;
        mCustomDateColorPref = null;
        mDefaultNextAlarmColorPref = null;
        mCustomNextAlarmColorPref = null;
    }

}

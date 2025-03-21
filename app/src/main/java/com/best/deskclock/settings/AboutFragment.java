// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.controller.ThemeController;

import java.util.List;

public class AboutFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

    Preference mWhatsNewPreference;
    Preference mAboutFeatures;
    Preference mViewOnGitHub;
    Preference mTranslate;
    Preference mReadLicence;
    Preference mContributor1;
    Preference mContributor2;
    Preference mContributor3;
    Preference mContributor4;
    Preference mCredit1;
    Preference mCredit2;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.about_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_about);

        mWhatsNewPreference = findPreference(KEY_ABOUT_WHATS_NEW);
        mAboutFeatures = findPreference(KEY_ABOUT_FEATURES);
        mViewOnGitHub = findPreference(KEY_ABOUT_VIEW_ON_GITHUB);
        mTranslate = findPreference(KEY_ABOUT_TRANSLATE);
        mReadLicence = findPreference(KEY_ABOUT_READ_LICENCE);
        mContributor1 = findPreference(KEY_ABOUT_BLACKYHAWKY);
        mContributor2 = findPreference(KEY_ABOUT_QW123WH);
        mContributor3 = findPreference(KEY_ABOUT_ODMFL);
        mContributor4 = findPreference(KEY_ABOUT_NILSU11);
        mCredit1 = findPreference(KEY_ABOUT_LINEAGEOS);
        mCredit2 = findPreference(KEY_ABOUT_CRDROID);

        setupTitle();
        setupVersion();
        setupPreferences();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.reset_settings_title)
                .setIcon(R.drawable.ic_reset_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            final AlertDialog builder = new AlertDialog.Builder(requireContext())
                    .setIcon(R.drawable.ic_reset_settings)
                    .setTitle(R.string.reset_settings_title)
                    .setMessage(R.string.reset_settings_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> resetPreferences())
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            builder.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case KEY_ABOUT_WHATS_NEW -> {
                String version = BuildConfig.VERSION_NAME;
                if (BuildConfig.DEBUG) {
                    version = version.replace("-debug", "");
                }

                final String link = "https://github.com/BlackyHawky/Clock/releases/tag/" + version;
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setIcon(R.drawable.ic_about_update)
                        .setTitle(R.string.whats_new_title)
                        .setMessage(requireContext().getString(R.string.whats_new_dialog_message, link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_FEATURES -> {
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setIcon(R.drawable.ic_about_features)
                        .setTitle(R.string.features_title)
                        .setMessage(R.string.about_dialog_message)
                        .setPositiveButton(R.string.dialog_close, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_VIEW_ON_GITHUB -> {
                final String link = "https://github.com/BlackyHawky/Clock";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setIcon(R.drawable.ic_about_github)
                        .setTitle(R.string.about_github_link)
                        .setMessage(requireContext().getString(R.string.github_dialog_message, link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_TRANSLATE -> {
                final String link = "https://translate.codeberg.org/projects/clock";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setIcon(R.drawable.ic_about_translate)
                        .setTitle(R.string.about_translate_link)
                        .setMessage(requireContext().getString(R.string.translate_dialog_message, link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_READ_LICENCE -> {
                final String link = "https://github.com/BlackyHawky/Clock/blob/main/LICENSE-GPL-3";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setIcon(R.drawable.ic_about_license)
                        .setTitle(R.string.license)
                        .setMessage(requireContext().getString(R.string.license_dialog_message, link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_BLACKYHAWKY -> {
                final String link = "https://github.com/BlackyHawky";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setMessage(requireContext().getString(R.string.contributors_dialog_message, "BlackyHawky", link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_QW123WH -> {
                final String link = "https://github.com/qw123wh";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setMessage(requireContext().getString(R.string.contributors_dialog_message, "qw123wh", link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_ODMFL -> {
                final String link = "https://github.com/odmfl";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setMessage(requireContext().getString(R.string.contributors_dialog_message, "odmfl", link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_NILSU11 -> {
                final String link = "https://github.com/Nilsu11";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setMessage(requireContext().getString(R.string.contributors_dialog_message, "Nilsu11", link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_LINEAGEOS -> {
                final String link = "https://github.com/LineageOS";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setMessage(requireContext().getString(R.string.contributors_dialog_message, "LineageOS", link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }

            case KEY_ABOUT_CRDROID -> {
                final String link = "https://github.com/crdroidandroid";
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setMessage(requireContext().getString(R.string.contributors_dialog_message, "crDroid Android", link))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                builder.show();
            }
        }

        return true;
    }

    private void setupTitle() {
        Preference title = findPreference(KEY_ABOUT_TITLE);
        if (title == null) {
            return;
        }
        if (BuildConfig.DEBUG) {
            title.setTitle(R.string.about_debug_app_title);
        } else {
            title.setTitle(R.string.app_label);
        }
    }

    private void setupVersion() {
        Preference version = findPreference(KEY_ABOUT_VERSION);
        if (version == null) {
            return;
        }
        version.setSummary(BuildConfig.VERSION_NAME);
    }

    private void setupPreferences() {
        mWhatsNewPreference.setOnPreferenceClickListener(this);
        mAboutFeatures.setOnPreferenceClickListener(this);
        mViewOnGitHub.setOnPreferenceClickListener(this);
        mTranslate.setOnPreferenceClickListener(this);
        mReadLicence.setOnPreferenceClickListener(this);
        mContributor1.setOnPreferenceClickListener(this);
        mContributor2.setOnPreferenceClickListener(this);
        mContributor3.setOnPreferenceClickListener(this);
        mContributor4.setOnPreferenceClickListener(this);
        mCredit1.setOnPreferenceClickListener(this);
        mCredit2.setOnPreferenceClickListener(this);
    }

    private void resetPreferences() {
        Fragment settingsFragment =
                requireActivity().getSupportFragmentManager().findFragmentByTag(SettingsActivity.SettingsFragment.class.getSimpleName());

        if (settingsFragment == null) {
            animateAndShowFragment(new SettingsActivity.SettingsFragment());
        }

        // Adding a Handler ensures better fluidity for animations
        new Handler(requireContext().getMainLooper()).postDelayed(() -> {
            for (String key : preferencesList()) {
                mPrefs.edit().remove(key).apply();
            }

            requireContext().sendBroadcast(new Intent(ACTION_LANGUAGE_CODE_CHANGED));
            requireContext().sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));

            ThemeController.setNewSettingWithDelay();
        }, 400);

    }

    private List<String> preferencesList() {
        return List.of(
                // Interface
                KEY_THEME,
                KEY_DARK_MODE,
                KEY_ACCENT_COLOR,
                KEY_AUTO_NIGHT_ACCENT_COLOR,
                KEY_NIGHT_ACCENT_COLOR,
                KEY_CARD_BACKGROUND,
                KEY_CARD_BORDER,
                KEY_CUSTOM_LANGUAGE_CODE,
                KEY_TAB_TO_DISPLAY,
                KEY_VIBRATIONS,
                KEY_TAB_INDICATOR,
                KEY_FADE_TRANSITIONS,
                // Clock
                KEY_CLOCK_STYLE,
                KEY_CLOCK_DISPLAY_SECONDS,
                KEY_AUTO_HOME_CLOCK,
                KEY_HOME_TIME_ZONE,
                // Alarm
                KEY_DEFAULT_ALARM_RINGTONE,
                KEY_AUTO_SILENCE,
                KEY_ALARM_SNOOZE_DURATION,
                KEY_ALARM_VOLUME_SETTING,
                KEY_ALARM_CRESCENDO_DURATION,
                KEY_SWIPE_ACTION,
                KEY_VOLUME_BUTTONS,
                KEY_POWER_BUTTON,
                KEY_FLIP_ACTION,
                KEY_SHAKE_ACTION,
                KEY_SHAKE_INTENSITY,
                KEY_WEEK_START,
                KEY_ALARM_NOTIFICATION_REMINDER_TIME,
                KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT,
                KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS,
                KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM,
                KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT,
                KEY_MATERIAL_TIME_PICKER_STYLE,
                // Alarm Display Customization
                KEY_ALARM_CLOCK_STYLE,
                KEY_DISPLAY_ALARM_SECONDS_HAND,
                KEY_ALARM_BACKGROUND_COLOR,
                KEY_ALARM_BACKGROUND_AMOLED_COLOR,
                KEY_ALARM_CLOCK_COLOR,
                KEY_ALARM_SECONDS_HAND_COLOR,
                KEY_ALARM_TITLE_COLOR,
                KEY_SNOOZE_BUTTON_COLOR,
                KEY_DISMISS_BUTTON_COLOR,
                KEY_ALARM_BUTTON_COLOR,
                KEY_PULSE_COLOR,
                KEY_ALARM_CLOCK_FONT_SIZE,
                KEY_ALARM_TITLE_FONT_SIZE,
                KEY_DISPLAY_RINGTONE_TITLE,
                // Timer
                KEY_TIMER_RINGTONE,
                KEY_TIMER_AUTO_SILENCE,
                KEY_TIMER_CRESCENDO_DURATION,
                KEY_TIMER_VIBRATE,
                KEY_TIMER_VOLUME_BUTTONS_ACTION,
                KEY_TIMER_POWER_BUTTON_ACTION,
                KEY_TIMER_FLIP_ACTION,
                KEY_TIMER_SHAKE_ACTION,
                KEY_SORT_TIMER,
                KEY_DEFAULT_TIME_TO_ADD_TO_TIMER,
                KEY_KEEP_TIMER_SCREEN_ON,
                KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER,
                KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER,
                // Stopwatch
                KEY_SW_VOLUME_UP_ACTION,
                KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS,
                KEY_SW_VOLUME_DOWN_ACTION,
                KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS,
                // Screensaver
                KEY_SCREENSAVER_CLOCK_STYLE,
                KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS,
                KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS,
                KEY_SCREENSAVER_CLOCK_COLOR_PICKER,
                KEY_SCREENSAVER_DATE_COLOR_PICKER,
                KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER,
                KEY_SCREENSAVER_BRIGHTNESS,
                KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD,
                KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                KEY_SCREENSAVER_DATE_IN_BOLD,
                KEY_SCREENSAVER_DATE_IN_ITALIC,
                KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD,
                KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC,
                // Analog Widget
                KEY_ANALOG_WIDGET_WITH_SECOND_HAND,
                // Digital Widget
                KEY_DIGITAL_WIDGET_DISPLAY_SECONDS,
                KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND,
                KEY_DIGITAL_WIDGET_BACKGROUND_COLOR,
                KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED,
                KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR,
                KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR,
                KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR,
                KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR,
                KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR,
                KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR,
                KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR,
                KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR,
                KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR,
                KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE,
                // Next Alarm Widget
                KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND,
                KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR,
                KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR,
                KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR,
                KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR,
                KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR,
                KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR,
                KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR,
                KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE,
                // Vertical Digital Widget
                KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND,
                KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR,
                KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE,
                // Material You Analog Widget
                KEY_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND,
                // Material You Digital Widget
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_SECONDS_DISPLAYED,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE,
                // Material You Vertical Digital Widget
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE,
                // Material You Next Alarm Widget
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR,
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR,
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR,
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR,
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR,
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR,
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE
        );
    }
}

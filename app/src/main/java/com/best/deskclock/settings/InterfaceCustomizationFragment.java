// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.controller.Controller;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.tiles.AlarmTileService;
import com.best.deskclock.tiles.StopwatchTileService;
import com.best.deskclock.tiles.TimerTileService;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InterfaceCustomizationFragment extends ScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static boolean isLanguageChanged = false;

    ListPreference mThemePref;
    ListPreference mDarkModePref;
    Preference mGeneralFontPref;
    ListPreference mAccentColorPref;
    SwitchPreferenceCompat mAutoNightAccentColorPref;
    ListPreference mNightAccentColorPref;
    SwitchPreferenceCompat mCardBackgroundPref;
    SwitchPreferenceCompat mCardBorderPref;
    ListPreference mLanguageCodePref;
    MultiSelectListPreference mVisibleTabsPref;
    ListPreference mTabToDisplayPref;
    SwitchPreferenceCompat mVibrationPref;
    SwitchPreferenceCompat mToolbarTitlePref;
    ListPreference mTabTitleVisibilityPref;
    SwitchPreferenceCompat mTabIndicatorPref;
    SwitchPreferenceCompat mFadeTransitionsPref;
    SwitchPreferenceCompat mKeepScreenOnPref;

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

            final Context appContext = requireContext().getApplicationContext();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = Utils.toSafeFileName(FILE_GENERAL_FONT);
            String oldFontPath = mPrefs.getString(KEY_GENERAL_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                clearFile(oldFontPath);

                // Clear the font cache
                ThemeUtils.removeFontFromCache(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_GENERAL_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, "Error importing font");
                    }

                    if (!isAdded() || mGeneralFontPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mGeneralFontPref.setTitle(getString(R.string.custom_font_title_variant));
                    } else {
                        mGeneralFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });
            });
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.interface_customization_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_interface_customization);

        mThemePref = findPreference(KEY_THEME);
        mDarkModePref = findPreference(KEY_DARK_MODE);
        mGeneralFontPref = findPreference(KEY_GENERAL_FONT);
        mAccentColorPref = findPreference(KEY_ACCENT_COLOR);
        mAutoNightAccentColorPref = findPreference(KEY_AUTO_NIGHT_ACCENT_COLOR);
        mNightAccentColorPref = findPreference(KEY_NIGHT_ACCENT_COLOR);
        mCardBackgroundPref = findPreference(KEY_CARD_BACKGROUND);
        mCardBorderPref = findPreference(KEY_CARD_BORDER);
        mLanguageCodePref = findPreference(KEY_LANGUAGE_CODE);
        mVisibleTabsPref = findPreference(KEY_VISIBLE_TABS);
        mTabToDisplayPref = findPreference(KEY_TAB_TO_DISPLAY);
        mVibrationPref = findPreference(KEY_VIBRATIONS);
        mToolbarTitlePref = findPreference(KEY_TOOLBAR_TITLE);
        mTabTitleVisibilityPref = findPreference(KEY_TAB_TITLE_VISIBILITY);
        mTabIndicatorPref = findPreference(KEY_TAB_INDICATOR);
        mFadeTransitionsPref = findPreference(KEY_FADE_TRANSITIONS);
        mKeepScreenOnPref = findPreference(KEY_KEEP_SCREEN_ON);

        setupPreferences();

        sortListPreference(mAccentColorPref);
        if (mNightAccentColorPref.isShown()) {
            sortListPreference(mNightAccentColorPref);
        }
        sortListPreference(mLanguageCodePref);
    }

    @Override
    public void onResume() {
        super.onResume();

        restoreCustomFileDialogIfNeeded(KEY_GENERAL_FONT, mGeneralFontPref, fontPickerLauncher, null);

        if (isLanguageChanged) {
            WidgetUtils.updateAllDigitalWidgets(requireContext());
            isLanguageChanged = false;
        }
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mThemePref, mDarkModePref, mGeneralFontPref, mAccentColorPref, mAutoNightAccentColorPref,
            mNightAccentColorPref, mCardBackgroundPref, mCardBorderPref, mLanguageCodePref, mVisibleTabsPref, mTabToDisplayPref,
            mVibrationPref, mToolbarTitlePref, mTabTitleVisibilityPref, mTabIndicatorPref, mFadeTransitionsPref, mKeepScreenOnPref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_THEME, KEY_ACCENT_COLOR, KEY_DARK_MODE, KEY_NIGHT_ACCENT_COLOR, KEY_TAB_TITLE_VISIBILITY, KEY_TAB_TO_DISPLAY -> {
                final ListPreference listPreference = (ListPreference) pref;
                final int index = listPreference.findIndexOfValue((String) newValue);
                listPreference.setSummary(listPreference.getEntries()[index]);
            }

            case KEY_AUTO_NIGHT_ACCENT_COLOR, KEY_CARD_BACKGROUND, KEY_CARD_BORDER, KEY_FADE_TRANSITIONS, KEY_VIBRATIONS, KEY_TOOLBAR_TITLE,
                 KEY_TAB_INDICATOR, KEY_KEEP_SCREEN_ON -> Utils.setVibrationTime(requireContext(), 50);

            case KEY_LANGUAGE_CODE -> {
                final int index = mLanguageCodePref.findIndexOfValue((String) newValue);
                mLanguageCodePref.setSummary(mLanguageCodePref.getEntries()[index]);

                String languageCode = (String) newValue;

                if (languageCode.equals(DEFAULT_SYSTEM_LANGUAGE_CODE)) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
                }

                isLanguageChanged = true;
            }

            case KEY_VISIBLE_TABS -> {
                @SuppressWarnings("unchecked")
                Set<String> newSelectedTabs = (Set<String>) newValue;
                Set<String> oldSelectedTabs = SettingsDAO.getVisibleTabs(mPrefs);

                if (newSelectedTabs.isEmpty()) {
                    // This shouldn't happen because it's impossible to uncheck all the entries.
                    return false;
                }

                updateVisibleTabsSummary(newSelectedTabs);

                updateTabToDisplayPreference(newSelectedTabs);

                boolean wasClockVisible = oldSelectedTabs.contains(PreferencesDefaultValues.VISIBLE_TAB_CLOCK);
                boolean isClockVisible = newSelectedTabs.contains(PreferencesDefaultValues.VISIBLE_TAB_CLOCK);

                // If the setting has changed (checked or unchecked) and the cities is displayed on the digital widget,
                // refresh it.
                if (wasClockVisible != isClockVisible && WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(mPrefs)) {
                    WidgetUtils.scheduleWidgetUpdate(requireContext(), DigitalAppWidgetProvider.class);
                }

                requireView().post(() -> {
                    // Update the shortcuts (the ones that appear when long-pressing the app icon)
                    Controller.getController().updateShortcuts();

                    // Update the tiles
                    if (SdkUtils.isAtLeastAndroid7()) {
                        TileService.requestListeningState(requireContext(), new ComponentName(requireContext(), AlarmTileService.class));
                        TileService.requestListeningState(requireContext(), new ComponentName(requireContext(), TimerTileService.class));
                        TileService.requestListeningState(requireContext(), new ComponentName(requireContext(), StopwatchTileService.class));
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        if (pref.getKey().equals(KEY_GENERAL_FONT)) {
            selectCustomFile(mGeneralFontPref, fontPickerLauncher,
                SettingsDAO.getGeneralFont(mPrefs), KEY_GENERAL_FONT, true, null);
        }

        return true;
    }

    private void setupPreferences() {
        Set<String> visibleTabs = SettingsDAO.getVisibleTabs(mPrefs);

        mThemePref.setSummary(mThemePref.getEntry());
        mThemePref.setOnPreferenceChangeListener(this);

        mDarkModePref.setSummary(mDarkModePref.getEntry());
        mDarkModePref.setOnPreferenceChangeListener(this);

        mGeneralFontPref.setTitle(getString(SettingsDAO.getGeneralFont(mPrefs) == null
            ? R.string.custom_font_title
            : R.string.custom_font_title_variant));
        mGeneralFontPref.setOnPreferenceClickListener(this);

        mAccentColorPref.setSummary(mAccentColorPref.getEntry());
        mAccentColorPref.setOnPreferenceChangeListener(this);
        if (SettingsDAO.isAutoNightAccentColorEnabled(mPrefs)) {
            mAccentColorPref.setTitle(requireContext().getString(R.string.title_accent_color));
            mAccentColorPref.setDialogTitle(requireContext().getString(R.string.title_accent_color));
        } else {
            mAccentColorPref.setTitle(requireContext().getString(R.string.day_accent_color_title));
            mAccentColorPref.setDialogTitle(requireContext().getString(R.string.day_accent_color_title));
        }

        mAutoNightAccentColorPref.setOnPreferenceChangeListener(this);

        mNightAccentColorPref.setVisible(!SettingsDAO.isAutoNightAccentColorEnabled(mPrefs));
        mNightAccentColorPref.setSummary(mNightAccentColorPref.getEntry());
        mNightAccentColorPref.setOnPreferenceChangeListener(this);

        mCardBackgroundPref.setOnPreferenceChangeListener(this);

        mCardBorderPref.setOnPreferenceChangeListener(this);

        mToolbarTitlePref.setOnPreferenceChangeListener(this);

        mTabTitleVisibilityPref.setSummary(mTabTitleVisibilityPref.getEntry());
        mTabTitleVisibilityPref.setOnPreferenceChangeListener(this);

        mTabIndicatorPref.setOnPreferenceChangeListener(this);

        mLanguageCodePref.setSummary(mLanguageCodePref.getEntry());
        mLanguageCodePref.setOnPreferenceChangeListener(this);

        updateVisibleTabsSummary(visibleTabs);
        mVisibleTabsPref.setOnPreferenceChangeListener(this);

        updateTabToDisplayPreference(visibleTabs);
        mTabToDisplayPref.setOnPreferenceChangeListener(this);

        mVibrationPref.setVisible(DeviceUtils.hasVibrator(requireContext()));
        mVibrationPref.setOnPreferenceChangeListener(this);

        mFadeTransitionsPref.setOnPreferenceChangeListener(this);

        mKeepScreenOnPref.setOnPreferenceChangeListener(this);
    }

    private void sortListPreference(ListPreference listPreference) {
        if (listPreference != null) {

            CharSequence[] entries = listPreference.getEntries();
            CharSequence[] values = listPreference.getEntryValues();

            if (entries != null && values != null && entries.length > 1) {
                // Create a list of (entry, value) pairs to sort
                List<Pair> entryValuePairs = new ArrayList<>();

                // Add the first entry and value that should not be sorted
                entryValuePairs.add(new Pair(entries[0], values[0]));

                // Add the rest of the entries and values to sort (starting from the second element)
                for (int i = 1; i < entries.length; i++) {
                    entryValuePairs.add(new Pair(entries[i], values[i]));
                }

                // Sort elements starting from second (index 1)
                List<Pair> remainingPairs = entryValuePairs.subList(1, entryValuePairs.size());
                Collections.sort(remainingPairs, (pair1, pair2) ->
                    CharSequence.compare(pair1.entry.toString(), pair2.entry.toString()));

                CharSequence[] sortedEntries = new CharSequence[entries.length];
                CharSequence[] sortedValues = new CharSequence[values.length];

                // Place first entry and value (unsorted)
                sortedEntries[0] = entryValuePairs.get(0).entry;
                sortedValues[0] = entryValuePairs.get(0).value;

                // Copy sorted items
                for (int i = 1; i < entryValuePairs.size(); i++) {
                    sortedEntries[i] = entryValuePairs.get(i).entry;
                    sortedValues[i] = entryValuePairs.get(i).value;
                }

                // Update entries and sorted values in the ListPreference
                listPreference.setEntries(sortedEntries);
                listPreference.setEntryValues(sortedValues);
            }
        }
    }

    /**
     * Updates the summary of the "Visible tabs" preference to reflect the currently selected tabs.
     *
     * <p>The summary text is dynamically generated by concatenating the names of the visible tabs in their strict visual order
     * (e.g., "Alarm - Clock - Timer").</p>
     *
     * @param selectedTabs A set containing the string values of the currently visible tabs.
     */
    private void updateVisibleTabsSummary(Set<String> selectedTabs) {
        if (mVisibleTabsPref == null) return;

        List<String> labels = new ArrayList<>();

        if (selectedTabs.contains(VISIBLE_TAB_ALARM)) {
            labels.add(getString(R.string.menu_alarm));
        }

        if (selectedTabs.contains(VISIBLE_TAB_CLOCK)) {
            labels.add(getString(R.string.menu_clock));
        }

        if (selectedTabs.contains(VISIBLE_TAB_TIMER)) {
            labels.add(getString(R.string.menu_timer));
        }

        if (selectedTabs.contains(VISIBLE_TAB_STOPWATCH)) {
            labels.add(getString(R.string.menu_stopwatch));
        }

        String summary = String.join(" - ", labels);

        mVisibleTabsPref.setSummary(summary);
    }

    /**
     * Updates the "Tab to display" preference based on the currently visible tabs.
     *
     * <p>If only one tab is visible, this preference is hidden as it becomes redundant.
     * Otherwise, it dynamically populates the available options (always including "Last tab used") and ensures they follow
     * the standard visual order.</p>
     *
     * <p>If the previously selected default tab is no longer visible, it automatically falls back to
     * the "Last tab used" option.</p>
     *
     * @param visibleTabs A set containing the string values of the currently visible tabs.
     */
    private void updateTabToDisplayPreference(Set<String> visibleTabs) {
        // Scenario where only one tab is visible.
        if (visibleTabs.size() <= 1) {
            mTabToDisplayPref.setVisible(false);

            if (!visibleTabs.isEmpty()) {
                String singleTab = visibleTabs.iterator().next();
                String value = DEFAULT_TAB_TO_DISPLAY;
                switch (singleTab) {
                    case VISIBLE_TAB_ALARM -> value = TAB_TO_DISPLAY_ALARM;
                    case VISIBLE_TAB_CLOCK -> value = TAB_TO_DISPLAY_CLOCK;
                    case VISIBLE_TAB_TIMER -> value = TAB_TO_DISPLAY_TIMER;
                    case VISIBLE_TAB_STOPWATCH -> value = TAB_TO_DISPLAY_STOPWATCH;
                }

                mTabToDisplayPref.setValue(value);
            }

            return;
        }

        mTabToDisplayPref.setVisible(true);

        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        // "Last tab used" is always available
        entries.add(getString(R.string.last_tab_used_title));
        entryValues.add(DEFAULT_TAB_TO_DISPLAY);

        // Follow the visual order (Alarm -> Clock -> Timer -> Stopwatch)
        if (visibleTabs.contains(VISIBLE_TAB_ALARM)) {
            entries.add(getString(R.string.menu_alarm));
            entryValues.add(TAB_TO_DISPLAY_ALARM);
        }

        if (visibleTabs.contains(VISIBLE_TAB_CLOCK)) {
            entries.add(getString(R.string.menu_clock));
            entryValues.add(TAB_TO_DISPLAY_CLOCK);
        }

        if (visibleTabs.contains(VISIBLE_TAB_TIMER)) {
            entries.add(getString(R.string.menu_timer));
            entryValues.add(TAB_TO_DISPLAY_TIMER);
        }

        if (visibleTabs.contains(VISIBLE_TAB_STOPWATCH)) {
            entries.add(getString(R.string.menu_stopwatch));
            entryValues.add(TAB_TO_DISPLAY_STOPWATCH);
        }

        // Apply the new lists to mTabToDisplayPref
        mTabToDisplayPref.setEntries(entries.toArray(new CharSequence[0]));
        mTabToDisplayPref.setEntryValues(entryValues.toArray(new CharSequence[0]));

        String currentValue = mTabToDisplayPref.getValue();
        if (currentValue == null) {
            currentValue = DEFAULT_TAB_TO_DISPLAY;
        }

        if (!currentValue.equals(DEFAULT_TAB_TO_DISPLAY) && !entryValues.contains(currentValue)) {
            mTabToDisplayPref.setValue(DEFAULT_TAB_TO_DISPLAY);
            currentValue = DEFAULT_TAB_TO_DISPLAY;
        }

        int index = mTabToDisplayPref.findIndexOfValue(currentValue);
        if (index >= 0) {
            mTabToDisplayPref.setSummary(mTabToDisplayPref.getEntries()[index]);
        }
    }

    private void nullifyAllPrefs() {
        mThemePref = null;
        mDarkModePref = null;
        mGeneralFontPref = null;
        mAccentColorPref = null;
        mAutoNightAccentColorPref = null;
        mNightAccentColorPref = null;
        mCardBackgroundPref = null;
        mCardBorderPref = null;
        mLanguageCodePref = null;
        mVisibleTabsPref = null;
        mTabToDisplayPref = null;
        mVibrationPref = null;
        mToolbarTitlePref = null;
        mTabTitleVisibilityPref = null;
        mTabIndicatorPref = null;
        mFadeTransitionsPref = null;
        mKeepScreenOnPref = null;
    }

    /**
     * Internal class to store entry/value pairs
     */
    private record Pair(CharSequence entry, CharSequence value) {
    }

}

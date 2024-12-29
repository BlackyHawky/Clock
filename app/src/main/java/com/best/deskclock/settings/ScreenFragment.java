/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.AboutActivity.KEY_ABOUT_TITLE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ALARM_VOLUME_SETTING;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_BRIGHTNESS;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.Utils;

import java.util.Objects;

public class ScreenFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getPreferenceManager().setStorageDeviceProtected();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        int bottomPadding = Utils.toPixel(20, requireContext());
        int topPadding = Utils.toPixel(10, requireContext());
        getListView().setPadding(0, topPadding, 0, bottomPadding);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // this must be overridden, but is useless, because it's called during onCreate
        // so there is no possibility of calling setStorageDeviceProtected before this is called...
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);

        final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
        final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();

        if (preferenceScreen == null) return;
        int count = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            final Preference pref = preferenceScreen.getPreference(i);
            if (pref instanceof PreferenceCategory) {
                final int subPrefCount = ((PreferenceCategory) pref).getPreferenceCount();
                for (int j = 0; j < subPrefCount; j++) {
                    if (Objects.equals(((PreferenceCategory) pref).getPreference(j).getKey(), KEY_ALARM_VOLUME_SETTING)
                            || Objects.equals(((PreferenceCategory) pref).getPreference(j).getKey(), KEY_SCREENSAVER_BRIGHTNESS)) {
                        if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout_bordered);
                        } else if (isCardBackgroundDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout);
                        } else if (isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout_transparent_bordered);
                        } else {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout_transparent);
                        }
                    } else {
                        if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout_bordered);
                        } else if (isCardBackgroundDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout);
                        } else if (isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout_transparent_bordered);
                        } else {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout_transparent);
                        }
                    }
                }
            } else if (Objects.equals(pref.getKey(), KEY_ABOUT_TITLE)) {
                pref.setLayoutResource(R.layout.settings_about_title);
            } else {
                if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                    pref.setLayoutResource(R.layout.settings_preference_layout_bordered);
                } else if (isCardBackgroundDisplayed) {
                    pref.setLayoutResource(R.layout.settings_preference_layout);
                } else if (isCardBorderDisplayed) {
                    pref.setLayoutResource(R.layout.settings_preference_layout_transparent_bordered);
                } else {
                    pref.setLayoutResource(R.layout.settings_preference_layout_transparent);
                }
            }
        }
    }

}

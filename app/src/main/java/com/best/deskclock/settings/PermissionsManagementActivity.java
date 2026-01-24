// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS;
import static android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;
import static android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static android.provider.Settings.EXTRA_APP_PACKAGE;

import static com.best.deskclock.settings.PreferencesKeys.KEY_ESSENTIAL_PERMISSIONS_GRANTED;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FULL_SCREEN_NOTIFICATION_PERMISSION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_IGNORE_BATTERY_OPTIMIZATIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NOTIFICATION_PERMISSION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHOW_LOCKSCREEN_PERMISSION;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.settings.custompreference.PermissionsManagementPreference;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.PermissionUtils;
import com.best.deskclock.utils.SdkUtils;

/**
 * Manage the permissions required to ensure the application runs properly.
 */
public class PermissionsManagementActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected String getActivityTitle() {
        // Already defined in the fragment.
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PermissionsManagementFragment())
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class PermissionsManagementFragment extends ScreenFragment
            implements Preference.OnPreferenceClickListener {

        private static final String[] DYNAMIC_PERMISSION_KEYS = {
                KEY_IGNORE_BATTERY_OPTIMIZATIONS,
                KEY_NOTIFICATION_PERMISSION,
                KEY_FULL_SCREEN_NOTIFICATION_PERMISSION
        };

        PermissionsManagementPreference mIgnoreBatteryOptimizationsPref;
        PermissionsManagementPreference mNotificationPermissionPref;
        PermissionsManagementPreference mFullScreenNotificationPref;
        PermissionsManagementPreference mShowLockScreenPref;

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.permission_management_settings);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_permissions_management);

            mIgnoreBatteryOptimizationsPref = findPreference(KEY_IGNORE_BATTERY_OPTIMIZATIONS);
            mNotificationPermissionPref = findPreference(KEY_NOTIFICATION_PERMISSION);
            mFullScreenNotificationPref = findPreference(KEY_FULL_SCREEN_NOTIFICATION_PERMISSION);
            mShowLockScreenPref = findPreference(KEY_SHOW_LOCKSCREEN_PERMISSION);

            setupPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();

            updateEssentialPermissionsPref();

            for (String key : DYNAMIC_PERMISSION_KEYS) {
                updateSinglePreference(key);
            }
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            final Context context = getActivity();
            if (context == null) {
                return false;
            }

            switch (pref.getKey()) {
                case KEY_IGNORE_BATTERY_OPTIMIZATIONS -> launchIgnoreBatteryOptimizationsSettings();
                case KEY_NOTIFICATION_PERMISSION -> grantOrRevokeNotificationsPermission();
                case KEY_FULL_SCREEN_NOTIFICATION_PERMISSION -> grantOrRevokeFullScreenNotificationsPermission();
                case KEY_SHOW_LOCKSCREEN_PERMISSION -> grantShowOnLockScreenPermissionXiaomi();
            }

            return true;
        }

        private void setupPreferences() {
            mIgnoreBatteryOptimizationsPref.setOnPreferenceClickListener(this);

            mNotificationPermissionPref.setOnPreferenceClickListener(this);

            mFullScreenNotificationPref.setVisible(SdkUtils.isAtLeastAndroid14());
            mFullScreenNotificationPref.setOnPreferenceClickListener(this);

            mShowLockScreenPref.setVisible(DeviceUtils.isMiui());
            mShowLockScreenPref.setOnPreferenceClickListener(this);

            PermissionUtils.grantPowerOffPermissionForSupportedDevices(requireActivity());
        }

        /**
         * Grant or revoke Ignore Battery Optimizations permission.
         */
        private void launchIgnoreBatteryOptimizationsSettings() {
            @SuppressLint("BatteryLife") final Intent intentGrant =
                    new Intent().setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(
                            Uri.fromParts("package", requireContext().getPackageName(), null));

            final Intent intentRevoke =
                    new Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(FLAG_ACTIVITY_NEW_TASK);

            if (PermissionUtils.isIgnoringBatteryOptimizations(requireContext())) {
                displayRevocationDialog(intentRevoke);
            } else {
                startActivity(intentGrant);
            }
        }

        /**
         * Grant or revoke Notifications permission.
         */
        private void grantOrRevokeNotificationsPermission() {
            Intent intent = SdkUtils.isAtLeastAndroid8()
                    ? new Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, requireContext().getPackageName())
                    .addFlags(FLAG_ACTIVITY_NEW_TASK)
                    : new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

            if (PermissionUtils.areNotificationsEnabled(requireContext())) {
                displayRevocationDialog(intent);
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                CustomDialog.createSimpleDialog(
                        requireContext(),
                        R.drawable.ic_notifications,
                        R.string.notifications_dialog_title,
                        getString(R.string.notifications_dialog_text),
                        android.R.string.ok,
                        (d, w) -> startActivity(intent)
                ).show();
            } else {
                if (SdkUtils.isAtLeastAndroid13()) {
                    requireActivity().requestPermissions(new String[]{POST_NOTIFICATIONS}, 0);
                } else {
                    startActivity(intent);
                }
            }
        }

        /**
         * Grant or revoke Full Screen Notifications permission.
         */
        private void grantOrRevokeFullScreenNotificationsPermission() {
            if (SdkUtils.isAtLeastAndroid14()) {
                final Intent intent = new Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                        .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

                if (!PermissionUtils.areFullScreenNotificationsEnabled(requireContext())) {
                    startActivity(intent);
                } else {
                    displayRevocationDialog(intent);
                }
            }
        }

        /**
         * Grant the "Show On Lock Screen" permission for Xiaomi devices.
         */
        private void grantShowOnLockScreenPermissionXiaomi() {
            if (!DeviceUtils.isMiui()) {
                return;
            }

            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", requireContext().getPackageName());
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallbackIntent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(fallbackIntent);
            }
        }

        /**
         * Display dialog when user wants to revoke permission.
         */
        private void displayRevocationDialog(Intent intent) {
            CustomDialog.createSimpleDialog(
                    requireContext(),
                    R.drawable.ic_key_off,
                    R.string.permission_dialog_revoke_title,
                    getString(R.string.revoke_permission_dialog_message),
                    android.R.string.ok,
                    (d, w) -> startActivity(intent)
            ).show();
        }

        /**
         * Updates the shared preference indicating whether essential permissions are granted.
         * Checks the current permission status and saves the result to SharedPreferences.
         */
        private void updateEssentialPermissionsPref() {
            boolean granted = !PermissionUtils.areEssentialPermissionsNotGranted(requireContext());
            mPrefs.edit().putBoolean(KEY_ESSENTIAL_PERMISSIONS_GRANTED, granted).apply();
        }

        private void updateSinglePreference(String key) {
            Preference preference = findPreference(key);

            if (preference instanceof PermissionsManagementPreference permissionsManagementPref) {
                permissionsManagementPref.refreshState();
            }
        }

    }

}

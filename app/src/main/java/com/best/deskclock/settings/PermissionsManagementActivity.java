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
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_FOREGROUND_SERVICE;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.KeepAliveService;
import com.best.deskclock.R;
import com.best.deskclock.settings.custompreference.PermissionsManagementPreference;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.PermissionUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

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
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        private static final String KEY_SHOW_FOREGROUND_SERVICE_DIALOG = "show_foreground_service_dialog";
        private static final String KEY_SHOW_NOTIF_RATIONALE = "show_notif_rationale";
        private static final String KEY_PENDING_REVOCATION_PREF = "pending_revocation_pref";

        private boolean mShowForegroundServiceDialog = false;
        private boolean mShowNotificationRationaleDialog = false;
        private String mPendingRevocationPrefKey = null;

        private static final String[] DYNAMIC_PERMISSION_KEYS = {
            KEY_IGNORE_BATTERY_OPTIMIZATIONS,
            KEY_NOTIFICATION_PERMISSION,
            KEY_FULL_SCREEN_NOTIFICATION_PERMISSION
        };

        PermissionsManagementPreference mIgnoreBatteryOptimizationsPref;
        PermissionsManagementPreference mNotificationPermissionPref;
        PermissionsManagementPreference mFullScreenNotificationPref;
        PermissionsManagementPreference mShowLockScreenPref;
        SwitchPreferenceCompat mEnableForegroundServicePref;

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
            mEnableForegroundServicePref = findPreference(KEY_ENABLE_FOREGROUND_SERVICE);

            if (savedInstanceState != null) {
                mShowForegroundServiceDialog = savedInstanceState.getBoolean(KEY_SHOW_FOREGROUND_SERVICE_DIALOG, false);
                mShowNotificationRationaleDialog = savedInstanceState.getBoolean(KEY_SHOW_NOTIF_RATIONALE, false);
                mPendingRevocationPrefKey = savedInstanceState.getString(KEY_PENDING_REVOCATION_PREF);
            }

            setupPreferences();
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);

            outState.putBoolean(KEY_SHOW_FOREGROUND_SERVICE_DIALOG, mShowForegroundServiceDialog);
            outState.putBoolean(KEY_SHOW_NOTIF_RATIONALE, mShowNotificationRationaleDialog);
            outState.putString(KEY_PENDING_REVOCATION_PREF, mPendingRevocationPrefKey);
        }

        @Override
        public void onResume() {
            super.onResume();

            if (mShowForegroundServiceDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
                showForegroundServiceDialog();
            } else if (mShowNotificationRationaleDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
                showNotificationRationaleDialog();
            } else if (mPendingRevocationPrefKey != null && (mActiveDialog == null || !mActiveDialog.isShowing())) {

                switch (mPendingRevocationPrefKey) {
                    case KEY_NOTIFICATION_PERMISSION ->
                        displayRevocationDialog(KEY_NOTIFICATION_PERMISSION, getNotificationSettingsIntent());
                    case KEY_IGNORE_BATTERY_OPTIMIZATIONS ->
                        displayRevocationDialog(KEY_IGNORE_BATTERY_OPTIMIZATIONS, getIgnoreBatteryOptimizationsIntentRevoke());
                    case KEY_FULL_SCREEN_NOTIFICATION_PERMISSION ->
                        displayRevocationDialog(KEY_FULL_SCREEN_NOTIFICATION_PERMISSION, getFullScreenNotificationsIntent());
                }
            }

            updateEssentialPermissionsPref();

            for (String key : DYNAMIC_PERMISSION_KEYS) {
                updateSinglePreference(key);
            }
        }

        @Override
        public void onDestroy() {
            nullifyPreferenceListeners(mIgnoreBatteryOptimizationsPref, mNotificationPermissionPref, mFullScreenNotificationPref,
                mShowLockScreenPref, mEnableForegroundServicePref);

            nullifyAllPrefs();

            super.onDestroy();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            if (pref.getKey().equals(KEY_ENABLE_FOREGROUND_SERVICE)) {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    showForegroundServiceDialog();
                    return false;
                } else {
                    Utils.stopService(requireContext(), KeepAliveService.class);
                }
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

            mEnableForegroundServicePref.setOnPreferenceChangeListener(this);

            PermissionUtils.grantPowerOffAlarmPermission(requireActivity());
        }

        private void showForegroundServiceDialog() {
            mShowForegroundServiceDialog = true;

            mActiveDialog = CustomDialog.create(
                requireContext(),
                null,
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_notifications),
                getString(R.string.foreground_service_title),
                getString(R.string.foreground_service_dialog_message),
                null,
                getString(android.R.string.ok),
                (d, w) -> {
                    mPrefs.edit().putBoolean(KEY_ENABLE_FOREGROUND_SERVICE, true).apply();
                    mEnableForegroundServicePref.setChecked(true);
                    Utils.startService(requireContext(), KeepAliveService.class);
                },
                getString(android.R.string.cancel),
                null,
                null,
                null,
                (alertDialog -> alertDialog.setOnDismissListener(d -> mShowForegroundServiceDialog = false)),
                CustomDialog.SoftInputMode.NONE
            );

            mActiveDialog.show();
        }

        /**
         * Grant or revoke Ignore Battery Optimizations permission.
         */
        private void launchIgnoreBatteryOptimizationsSettings() {
            @SuppressLint("BatteryLife") final Intent intentGrant =
                new Intent().setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(
                    Uri.fromParts("package", requireContext().getPackageName(), null));

            if (PermissionUtils.isIgnoringBatteryOptimizations(requireContext())) {
                displayRevocationDialog(KEY_IGNORE_BATTERY_OPTIMIZATIONS, getIgnoreBatteryOptimizationsIntentRevoke());
            } else {
                startActivity(intentGrant);
            }
        }

        private Intent getIgnoreBatteryOptimizationsIntentRevoke() {
            return new Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

        /**
         * Grant or revoke Notifications permission.
         */
        private void grantOrRevokeNotificationsPermission() {
            if (PermissionUtils.areNotificationsEnabled(requireContext())) {
                displayRevocationDialog(KEY_NOTIFICATION_PERMISSION, getNotificationSettingsIntent());
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                showNotificationRationaleDialog();
            } else {
                if (SdkUtils.isAtLeastAndroid13()) {
                    requireActivity().requestPermissions(new String[]{POST_NOTIFICATIONS}, 0);
                } else {
                    startActivity(getNotificationSettingsIntent());
                }
            }
        }

        private void showNotificationRationaleDialog() {
            mShowNotificationRationaleDialog = true;

            mActiveDialog = CustomDialog.create(
                requireContext(),
                null,
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_notifications),
                getString(R.string.notifications_dialog_title),
                getString(R.string.notifications_dialog_text),
                null,
                getString(android.R.string.ok),
                (d, w) -> startActivity(getNotificationSettingsIntent()),
                getString(android.R.string.cancel),
                null,
                null,
                null,
                (alertDialog -> alertDialog.setOnDismissListener(d -> mShowNotificationRationaleDialog = false)),
                CustomDialog.SoftInputMode.NONE
            );

            mActiveDialog.show();
        }

        private Intent getNotificationSettingsIntent() {
            return SdkUtils.isAtLeastAndroid8()
                ? new Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, requireContext().getPackageName())
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
                : new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

        /**
         * Grant or revoke Full Screen Notifications permission.
         */
        private void grantOrRevokeFullScreenNotificationsPermission() {
            if (!PermissionUtils.areFullScreenNotificationsEnabled(requireContext())) {
                startActivity(getFullScreenNotificationsIntent());
            } else {
                displayRevocationDialog(KEY_FULL_SCREEN_NOTIFICATION_PERMISSION, getFullScreenNotificationsIntent());
            }
        }

        private Intent getFullScreenNotificationsIntent() {
            if (SdkUtils.isAtLeastAndroid14()) {
                return new Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.fromParts("package", requireContext().getPackageName(), null))
                    .addFlags(FLAG_ACTIVITY_NEW_TASK);
            }

            return null;
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
        private void displayRevocationDialog(String prefKey, Intent intent) {
            mPendingRevocationPrefKey = prefKey;

            mActiveDialog = CustomDialog.create(
                requireContext(),
                null,
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_key_off),
                getString(R.string.permission_dialog_revoke_title),
                getString(R.string.revoke_permission_dialog_message),
                null,
                getString(android.R.string.ok),
                (d, w) -> startActivity(intent),
                getString(android.R.string.cancel),
                null,
                null,
                null,
                (alertDialog -> alertDialog.setOnDismissListener(d -> mPendingRevocationPrefKey = null)),
                CustomDialog.SoftInputMode.NONE
            );

            mActiveDialog.show();
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

        private void nullifyAllPrefs() {
            mIgnoreBatteryOptimizationsPref = null;
            mNotificationPermissionPref = null;
            mFullScreenNotificationPref = null;
            mShowLockScreenPref = null;
            mEnableForegroundServicePref = null;
        }

    }

}

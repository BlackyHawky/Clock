// SPDX-License-Identifier: GPL-3.0-only
package com.best.deskclock.settings;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS;
import static android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;
import static android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT;
import static android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static android.provider.Settings.EXTRA_APP_PACKAGE;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;

import com.best.deskclock.R;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.google.android.material.card.MaterialCardView;

public class PermissionsManagementActivity extends CollapsingToolbarBaseActivity {

    MaterialCardView mIgnoreBatteryOptimizationsView;
    MaterialCardView mDoNotDisturbView;
    MaterialCardView mNotificationView;
    MaterialCardView mFullScreenNotificationsView;
    MaterialCardView mStorageView;

    ImageView mIgnoreBatteryOptimizationsDetails;
    ImageView mDoNotDisturbDetails;
    ImageView mNotificationDetails;
    ImageView mFullScreenNotificationsDetails;
    ImageView mStorageDetails;

    TextView mDoNotDisturbStatus;
    TextView mIgnoreBatteryOptimizationsStatus;
    TextView mNotificationStatus;
    TextView mFullScreenNotificationsStatus;
    TextView mStorageStatus;

    private static final String PERMISSION_POWER_OFF_ALARM = "org.codeaurora.permission.POWER_OFF_ALARM";

    private int clickCountOnStorageButton = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.permissions_management_activity);

        mIgnoreBatteryOptimizationsView = findViewById(R.id.IBO_view);
        mIgnoreBatteryOptimizationsView.setOnClickListener(v -> launchIgnoreBatteryOptimizationsSettings());
        mIgnoreBatteryOptimizationsDetails = findViewById(R.id.IBO_details_button);
        mIgnoreBatteryOptimizationsDetails.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                .setTitle(R.string.ignore_battery_optimizations_dialog_title)
                .setMessage(R.string.ignore_battery_optimizations_dialog_text)
                .setPositiveButton(R.string.permission_dialog_close_button, null)
                .show()
        );
        mIgnoreBatteryOptimizationsStatus = findViewById(R.id.IBO_status_text);

        mDoNotDisturbView = findViewById(R.id.DND_view);
        mDoNotDisturbView.setOnClickListener(v -> grantOrRevokeDNDPermission());
        mDoNotDisturbDetails = findViewById(R.id.DND_details_button);
        mDoNotDisturbDetails.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.DND_dialog_title)
                        .setMessage(R.string.DND_dialog_text)
                        .setPositiveButton(R.string.permission_dialog_close_button, null)
                        .show()
        );
        mDoNotDisturbStatus = findViewById(R.id.DND_status_text);

        mNotificationView = findViewById(R.id.notification_view);
        mNotificationView.setOnClickListener(v -> grantOrRevokeNotificationsPermission());
        mNotificationDetails = findViewById(R.id.notification_details_button);
        mNotificationDetails.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.notifications_dialog_title)
                        .setMessage(R.string.notifications_dialog_text)
                        .setPositiveButton(R.string.permission_dialog_close_button, null)
                        .show()
        );
        mNotificationStatus = findViewById(R.id.notification_status_text);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mFullScreenNotificationsView = findViewById(R.id.FSN_view);
            mFullScreenNotificationsView.setVisibility(View.VISIBLE);
            mFullScreenNotificationsView.setOnClickListener(v -> grantOrRevokeFullScreenNotificationsPermission());
            mFullScreenNotificationsDetails = findViewById(R.id.FSN_details_button);
            mFullScreenNotificationsDetails.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.FSN_dialog_title)
                            .setMessage(R.string.FSN_dialog_text)
                            .setPositiveButton(R.string.permission_dialog_close_button, null)
                            .show()
            );
            mFullScreenNotificationsStatus = findViewById(R.id.FSN_status_text);
        }

        mStorageView = findViewById(R.id.storage_view);
        mStorageView.setOnClickListener(v -> {
            /* If the user refuses authorization in the system dialog, Android will not allow
            this authorization request dialog to be created again.
            We therefore need to know how many times the button is clicked in order to display
            an alert dialog to access the storage settings. */
            clickCountOnStorageButton = clickCountOnStorageButton + 1;
            if (clickCountOnStorageButton > 1 && !isStoragePermissionsGranted(this)) {
                Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.storage_permission_dialog_title)
                        .setMessage(R.string.storage_permission_dialog_message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(intent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();

            } else {
                grantOrRevokeStoragePermission();
            }

        });
        mStorageDetails = findViewById(R.id.storage_details_button);
        mStorageDetails.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.storage_dialog_title)
                        .setMessage(R.string.storage_dialog_text)
                        .setPositiveButton(R.string.permission_dialog_close_button, null)
                        .show()
        );
        mStorageStatus = findViewById(R.id.storage_status_text);

        grantPowerOffPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setStatusText();
    }

    /**
     * Grant or revoke Ignore Battery Optimizations permission
     */
    private void launchIgnoreBatteryOptimizationsSettings() {
        @SuppressLint("BatteryLife")
        final Intent intentGrant = new Intent().setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.fromParts("package", getPackageName(), null));

        final Intent intentRevoke = new Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(FLAG_ACTIVITY_NEW_TASK);

        if (!isIgnoringBatteryOptimizations(this)) {
            startActivity(intentGrant);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_revoke_title)
                    .setMessage(R.string.revoke_permission_dialog_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(intentRevoke))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    /**
     * Grant or revoke Do Not Disturb permission
     */
    private void grantOrRevokeDNDPermission() {
        final Intent intent = new Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(FLAG_ACTIVITY_NEW_TASK);
        if (!isDNDPermissionGranted(this)) {
            startActivity(intent);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_revoke_title)
                    .setMessage(R.string.revoke_permission_dialog_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(intent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    /**
     * Grant or revoke Notifications permission
     */
    private void grantOrRevokeNotificationsPermission() {
        Intent intent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, getPackageName())
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
                : new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

        if (areNotificationsEnabled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_revoke_title)
                    .setMessage(R.string.revoke_permission_dialog_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(intent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int codeForPostNotification = 0;
                requestPermissions(new String[]{POST_NOTIFICATIONS}, codeForPostNotification);
            } else {
                startActivity(intent);
            }
        }
    }

    /**
     * Grant or revoke Full Screen Notifications permission
     */
    private void grantOrRevokeFullScreenNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            final Intent intent = new Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.fromParts("package", getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

            if (!areFullScreenNotificationsEnabled(this)) {
                startActivity(intent);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_dialog_revoke_title)
                        .setMessage(R.string.revoke_permission_dialog_message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(intent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        }
    }

    /**
     * Grant or revoke Storage permission
     */
    private void grantOrRevokeStoragePermission() {
        int codeForStorage = 0;
        String storagePermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermissions = READ_MEDIA_AUDIO;
        } else {
            storagePermissions = READ_EXTERNAL_STORAGE;
        }

        if (checkSelfPermission(storagePermissions) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{storagePermissions}, codeForStorage);
        } else {
            Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_revoke_title)
                    .setMessage(R.string.revoke_permission_dialog_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(intent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    /**
     * Grant or revoke Power Off Alarm permission (available only on specific devices)
     */
    private void grantPowerOffPermission() {
        int codeForPowerOffAlarm = 0;
        if (checkSelfPermission(PERMISSION_POWER_OFF_ALARM) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERMISSION_POWER_OFF_ALARM}, codeForPowerOffAlarm);
        }
    }

    /**
     * Set permission status
     */
    private void setStatusText() {
        mIgnoreBatteryOptimizationsStatus.setText(isIgnoringBatteryOptimizations(this)
                ? R.string.permission_granted
                : R.string.permission_denied);
        mIgnoreBatteryOptimizationsStatus.setTextColor(isIgnoringBatteryOptimizations(this)
                ? Color.parseColor("#66BB6A")
                : Color.parseColor("#EF5350"));

        mDoNotDisturbStatus.setText(isDNDPermissionGranted(this)
                ? R.string.permission_granted
                : R.string.permission_denied);
        mDoNotDisturbStatus.setTextColor(isDNDPermissionGranted(this)
                ? Color.parseColor("#66BB6A")
                : Color.parseColor("#EF5350"));

        mNotificationStatus.setText(areNotificationsEnabled(this)
                ? R.string.permission_granted
                : R.string.permission_denied);
        mNotificationStatus.setTextColor(areNotificationsEnabled(this)
                ? Color.parseColor("#66BB6A")
                : Color.parseColor("#EF5350"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mFullScreenNotificationsStatus.setText(areFullScreenNotificationsEnabled(this)
                    ? R.string.permission_granted
                    : R.string.permission_denied);
            mFullScreenNotificationsStatus.setTextColor(areFullScreenNotificationsEnabled(this)
                    ? Color.parseColor("#66BB6A")
                    : Color.parseColor("#EF5350"));
        }

        mStorageStatus.setText(isStoragePermissionsGranted(this)
                ? R.string.permission_granted
                : R.string.permission_denied);
        mStorageStatus.setTextColor(isStoragePermissionsGranted(this)
                ? Color.parseColor("#66BB6A")
                : Color.parseColor("#EF5350"));
    }

    /**
     * @return {@code true} when Ignore Battery Optimizations permission is granted; {@code false} otherwise
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        final PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    /**
     * @return {@code true} when Do Not Disturb permission is granted; {@code false} otherwise
     */
    public static boolean isDNDPermissionGranted(Context context) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        return notificationManager.isNotificationPolicyAccessGranted();
    }

    /**
     * @return {@code true} when Notifications permission is granted; {@code false} otherwise
     */
    public static boolean areNotificationsEnabled(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * @return {@code true} when Full Screen Notifications permission is granted; {@code false} otherwise
     */
    public static boolean areFullScreenNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            return notificationManager.canUseFullScreenIntent();
        }
        return false;
    }

    /**
     * @return {@code true} when Storage permission is granted; {@code false} otherwise
     */
    public static boolean isStoragePermissionsGranted(Context context) {
        int granted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? context.checkSelfPermission(READ_MEDIA_AUDIO)
                : context.checkSelfPermission(READ_EXTERNAL_STORAGE);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

}

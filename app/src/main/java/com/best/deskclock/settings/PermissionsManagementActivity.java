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

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_PERMISSIONS;

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
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

public class PermissionsManagementActivity extends CollapsingToolbarBaseActivity {

    MaterialCardView mIgnoreBatteryOptimizationsView;
    MaterialCardView mNotificationView;
    MaterialCardView mFullScreenNotificationsView;

    ImageView mIgnoreBatteryOptimizationsDetails;
    ImageView mNotificationDetails;
    ImageView mFullScreenNotificationsDetails;

    TextView mIgnoreBatteryOptimizationsStatus;
    TextView mNotificationStatus;
    TextView mFullScreenNotificationsStatus;

    private static final String PERMISSION_POWER_OFF_ALARM = "org.codeaurora.permission.POWER_OFF_ALARM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.permissions_management_activity);

        final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
        final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();

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

        if (isCardBackgroundDisplayed) {
            mIgnoreBatteryOptimizationsView.setCardBackgroundColor(
                    MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK)
            );
            mNotificationView.setCardBackgroundColor(
                    MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK)
            );

        } else {
            mIgnoreBatteryOptimizationsView.setCardBackgroundColor(Color.TRANSPARENT);
            mNotificationView.setCardBackgroundColor(Color.TRANSPARENT);
        }

        if (isCardBorderDisplayed) {
            mIgnoreBatteryOptimizationsView.setStrokeWidth(Utils.toPixel(2, this));
            mIgnoreBatteryOptimizationsView.setStrokeColor(
                    MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
            );

            mNotificationView.setStrokeWidth(Utils.toPixel(2, this));
            mNotificationView.setStrokeColor(
                    MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
            );
        }

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

            if (isCardBackgroundDisplayed) {
                mFullScreenNotificationsView.setCardBackgroundColor(
                        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK)
                );
            } else {
                mFullScreenNotificationsView.setCardBackgroundColor(Color.TRANSPARENT);
            }

            if (isCardBorderDisplayed) {
                mFullScreenNotificationsView.setStrokeWidth(Utils.toPixel(2, this));
                mFullScreenNotificationsView.setStrokeColor(
                        MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
                );
            }

        }

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
            setResult(REQUEST_CHANGE_PERMISSIONS);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_revoke_title)
                    .setMessage(R.string.revoke_permission_dialog_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        startActivity(intentRevoke);
                        setResult(REQUEST_CHANGE_PERMISSIONS);
                    })
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
                    .setPositiveButton(android.R.string.yes, (dialog, which) ->{
                        startActivity(intent);
                        setResult(REQUEST_CHANGE_PERMISSIONS);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int codeForPostNotification = 0;
                requestPermissions(new String[]{POST_NOTIFICATIONS}, codeForPostNotification);
            } else {
                startActivity(intent);
            }
            setResult(REQUEST_CHANGE_PERMISSIONS);
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
                setResult(REQUEST_CHANGE_PERMISSIONS);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_dialog_revoke_title)
                        .setMessage(R.string.revoke_permission_dialog_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            startActivity(intent);
                            setResult(REQUEST_CHANGE_PERMISSIONS);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
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
                ? this.getColor(R.color.colorGranted)
                : this.getColor(R.color.colorAlert));

        mNotificationStatus.setText(areNotificationsEnabled(this)
                ? R.string.permission_granted
                : R.string.permission_denied);
        mNotificationStatus.setTextColor(areNotificationsEnabled(this)
                ? this.getColor(R.color.colorGranted)
                : this.getColor(R.color.colorAlert));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mFullScreenNotificationsStatus.setText(areFullScreenNotificationsEnabled(this)
                    ? R.string.permission_granted
                    : R.string.permission_denied);
            mFullScreenNotificationsStatus.setTextColor(areFullScreenNotificationsEnabled(this)
                    ? this.getColor(R.color.colorGranted)
                    : this.getColor(R.color.colorAlert));
        }
    }

    /**
     * @return {@code true} when Ignore Battery Optimizations permission is granted; {@code false} otherwise
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        final PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
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
     * @return {@code true} when essential permissions are not granted; {@code false} otherwise
     */
    public static boolean areEssentialPermissionsNotGranted(Context context) {
        return !isIgnoringBatteryOptimizations(context)
                || !areNotificationsEnabled(context)
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !areFullScreenNotificationsEnabled(context);
    }

}

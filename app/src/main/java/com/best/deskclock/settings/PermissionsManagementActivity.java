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

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

    public static class PermissionsManagementFragment extends ScreenFragment {

        View mPermissionContainerView;

        MaterialCardView mIgnoreBatteryOptimizationsView;
        MaterialCardView mNotificationView;
        MaterialCardView mFullScreenNotificationsView;
        MaterialCardView mShowLockscreenView;

        ImageView mIgnoreBatteryOptimizationsDetails;
        ImageView mNotificationDetails;
        ImageView mFullScreenNotificationsDetails;
        ImageView mShowLockscreenDetails;

        TextView mIgnoreBatteryOptimizationsStatus;
        TextView mNotificationStatus;
        TextView mFullScreenNotificationsStatus;

        private static final String PERMISSION_POWER_OFF_ALARM = "org.codeaurora.permission.POWER_OFF_ALARM";

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.permission_management_settings);
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.permissions_management_activity, container, false);

            mPermissionContainerView = rootView.findViewById(R.id.permission_container);

            mIgnoreBatteryOptimizationsView = rootView.findViewById(R.id.IBO_view);
            mIgnoreBatteryOptimizationsView.setOnClickListener(v -> launchIgnoreBatteryOptimizationsSettings());

            mIgnoreBatteryOptimizationsDetails = rootView.findViewById(R.id.IBO_details_button);
            mIgnoreBatteryOptimizationsDetails.setOnClickListener(v ->
                    displayPermissionDetailsDialog(
                            R.drawable.ic_battery_settings,
                            R.string.ignore_battery_optimizations_dialog_title,
                            R.string.ignore_battery_optimizations_dialog_text));

            mIgnoreBatteryOptimizationsStatus = rootView.findViewById(R.id.IBO_status_text);

            mNotificationView = rootView.findViewById(R.id.notification_view);
            mNotificationView.setOnClickListener(v -> grantOrRevokeNotificationsPermission());

            mNotificationDetails = rootView.findViewById(R.id.notification_details_button);
            mNotificationDetails.setOnClickListener(v ->
                    displayPermissionDetailsDialog(
                            R.drawable.ic_notifications,
                            R.string.notifications_dialog_title,
                            R.string.notifications_dialog_text));

            mNotificationStatus = rootView.findViewById(R.id.notification_status_text);

            final boolean isCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(mPrefs);
            final boolean isCardBorderDisplayed = SettingsDAO.isCardBorderDisplayed(mPrefs);

            updateCardViews(isCardBackgroundDisplayed, isCardBorderDisplayed);

            if (SdkUtils.isAtLeastAndroid14()) {
                mFullScreenNotificationsView = rootView.findViewById(R.id.FSN_view);
                mFullScreenNotificationsView.setVisibility(View.VISIBLE);
                mFullScreenNotificationsView.setOnClickListener(v -> grantOrRevokeFullScreenNotificationsPermission());

                mFullScreenNotificationsDetails = rootView.findViewById(R.id.FSN_details_button);
                mFullScreenNotificationsDetails.setOnClickListener(v ->
                        displayPermissionDetailsDialog(
                                R.drawable.ic_fullscreen,
                                R.string.FSN_dialog_title,
                                R.string.FSN_dialog_text));

                mFullScreenNotificationsStatus = rootView.findViewById(R.id.FSN_status_text);

                updateFullScreenNotificationsCard(isCardBackgroundDisplayed, isCardBorderDisplayed);
            }

            if (DeviceUtils.isMiui()) {
                mShowLockscreenView = rootView.findViewById(R.id.show_lockscreen_view);
                mShowLockscreenView.setVisibility(View.VISIBLE);
                mShowLockscreenView.setOnClickListener(v -> grantShowOnLockScreenPermissionXiaomi());

                mShowLockscreenDetails = rootView.findViewById(R.id.show_lockscreen_button);
                mShowLockscreenDetails.setOnClickListener(v ->
                        displayPermissionDetailsDialog(
                                R.drawable.ic_screen_lock,
                                R.string.show_lockscreen_dialog_title,
                                R.string.show_lockscreen_dialog_text));

                updateShowLockscreenCard(isCardBackgroundDisplayed, isCardBorderDisplayed);
            }

            return rootView;
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            grantPowerOffPermission();

            applyWindowInsets();
        }

        @Override
        public void onResume() {
            super.onResume();

            updateEssentialPermissionsPref();

            setStatusText();
        }

        /**
         * This method adjusts the space occupied by system elements (such as the status bar,
         * navigation bar or screen notch) and adjust the display of the application interface
         * accordingly.
         */
        private void applyWindowInsets() {
            InsetsUtils.doOnApplyWindowInsets(mCoordinatorLayout, (v, insets) -> {
                // Get the system bar and notch insets
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() |
                        WindowInsetsCompat.Type.displayCutout());

                v.setPadding(bars.left, bars.top, bars.right, 0);

                mPermissionContainerView.setPadding(0, 0, 0, bars.bottom);
            });
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

            if (isIgnoringBatteryOptimizations(requireContext())) {
                displayRevocationDialog(intentRevoke);
            } else {
                startActivity(intentGrant);
            }
        }

        /**
         * Grant or revoke Notifications permission
         */
        private void grantOrRevokeNotificationsPermission() {
            Intent intent = SdkUtils.isAtLeastAndroid8()
                    ? new Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, requireContext().getPackageName())
                    .addFlags(FLAG_ACTIVITY_NEW_TASK)
                    : new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

            if (areNotificationsEnabled(requireContext())) {
                displayRevocationDialog(intent);
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setIcon(R.drawable.ic_notifications)
                        .setTitle(R.string.notifications_dialog_title)
                        .setMessage(R.string.notifications_dialog_text)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                startActivity(intent))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                if (SdkUtils.isAtLeastAndroid13()) {
                    requireActivity().requestPermissions(new String[]{POST_NOTIFICATIONS}, 0);
                } else {
                    startActivity(intent);
                }
            }
        }

        /**
         * Grant or revoke Full Screen Notifications permission
         */
        private void grantOrRevokeFullScreenNotificationsPermission() {
            if (SdkUtils.isAtLeastAndroid14()) {
                final Intent intent = new Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                        .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

                if (!areFullScreenNotificationsEnabled(requireContext())) {
                    startActivity(intent);
                } else {
                    displayRevocationDialog(intent);
                }
            }
        }

        /**
         * Grant or revoke Power Off Alarm permission (available only on specific devices)
         */
        private void grantPowerOffPermission() {
            int codeForPowerOffAlarm = 0;
            if (requireContext().checkSelfPermission(PERMISSION_POWER_OFF_ALARM) != PackageManager.PERMISSION_GRANTED) {
                requireActivity().requestPermissions(new String[]{PERMISSION_POWER_OFF_ALARM}, codeForPowerOffAlarm);
            }
        }

        /**
         * Grant Show On Lock Screen permission for Xiaomi devices
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
         * Display dialog when user wants to read the permission details.
         */
        private void displayPermissionDetailsDialog(int iconId, int titleId, int messageId) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setIcon(iconId)
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setPositiveButton(R.string.dialog_close, null)
                    .show();
        }

        /**
         * Display dialog when user wants to revoke permission.
         */
        private void displayRevocationDialog(Intent intent) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setIcon(R.drawable.ic_key_off)
                    .setTitle(R.string.permission_dialog_revoke_title)
                    .setMessage(R.string.revoke_permission_dialog_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            startActivity(intent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        /**
         * Set permission status
         */
        private void setStatusText() {
            mIgnoreBatteryOptimizationsStatus.setText(isIgnoringBatteryOptimizations(requireContext())
                    ? R.string.permission_granted
                    : R.string.permission_denied);
            mIgnoreBatteryOptimizationsStatus.setTextColor(isIgnoringBatteryOptimizations(requireContext())
                    ? requireContext().getColor(R.color.colorGranted)
                    : requireContext().getColor(R.color.colorAlert));

            mNotificationStatus.setText(areNotificationsEnabled(requireContext())
                    ? R.string.permission_granted
                    : R.string.permission_denied);
            mNotificationStatus.setTextColor(areNotificationsEnabled(requireContext())
                    ? requireContext().getColor(R.color.colorGranted)
                    : requireContext().getColor(R.color.colorAlert));

            if (SdkUtils.isAtLeastAndroid14()) {
                mFullScreenNotificationsStatus.setText(areFullScreenNotificationsEnabled(requireContext())
                        ? R.string.permission_granted
                        : R.string.permission_denied);
                mFullScreenNotificationsStatus.setTextColor(areFullScreenNotificationsEnabled(requireContext())
                        ? requireContext().getColor(R.color.colorGranted)
                        : requireContext().getColor(R.color.colorAlert));
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
            if (SdkUtils.isAtLeastAndroid13()) {
                return ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            } else {
                return NotificationManagerCompat.from(context).areNotificationsEnabled();
            }
        }

        /**
         * @return {@code true} when Full Screen Notifications permission is granted; {@code false} otherwise
         */
        public static boolean areFullScreenNotificationsEnabled(Context context) {
            if (SdkUtils.isAtLeastAndroid14()) {
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
                    || SdkUtils.isAtLeastAndroid14() && !areFullScreenNotificationsEnabled(context);
        }

        private void updateEssentialPermissionsPref() {
            boolean granted = !areEssentialPermissionsNotGranted(requireContext());
            mPrefs.edit().putBoolean(KEY_ESSENTIAL_PERMISSIONS_GRANTED, granted).apply();
        }

        private void updateCardViews(boolean isCardBackgroundDisplayed, boolean isCardBorderDisplayed) {
            if (isCardBackgroundDisplayed) {
                mIgnoreBatteryOptimizationsView.setCardBackgroundColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, Color.BLACK)
                );
                mNotificationView.setCardBackgroundColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, Color.BLACK)
                );
            } else {
                mIgnoreBatteryOptimizationsView.setCardBackgroundColor(Color.TRANSPARENT);
                mNotificationView.setCardBackgroundColor(Color.TRANSPARENT);
            }

            if (isCardBorderDisplayed) {
                mIgnoreBatteryOptimizationsView.setStrokeWidth(ThemeUtils.convertDpToPixels(2, requireContext()));
                mIgnoreBatteryOptimizationsView.setStrokeColor(MaterialColors.getColor(
                        requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK)
                );

                mNotificationView.setStrokeWidth(ThemeUtils.convertDpToPixels(2, requireContext()));
                mNotificationView.setStrokeColor(MaterialColors.getColor(
                        requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK)
                );
            }
        }

        private void updateFullScreenNotificationsCard(boolean isCardBackgroundDisplayed, boolean isCardBorderDisplayed) {
            if (isCardBackgroundDisplayed) {
                mFullScreenNotificationsView.setCardBackgroundColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, Color.BLACK));
            } else {
                mFullScreenNotificationsView.setCardBackgroundColor(Color.TRANSPARENT);
            }

            if (isCardBorderDisplayed) {
                mFullScreenNotificationsView.setStrokeWidth(ThemeUtils.convertDpToPixels(2, requireContext()));
                mFullScreenNotificationsView.setStrokeColor(MaterialColors.getColor(
                        requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK));
            }
        }

        private void updateShowLockscreenCard(boolean isCardBackgroundDisplayed, boolean isCardBorderDisplayed) {
            if (isCardBackgroundDisplayed) {
                mShowLockscreenView.setCardBackgroundColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, Color.BLACK));
            } else {
                mShowLockscreenView.setCardBackgroundColor(Color.TRANSPARENT);
            }

            if (isCardBorderDisplayed) {
                mShowLockscreenView.setStrokeWidth(ThemeUtils.convertDpToPixels(2, requireContext()));
                mShowLockscreenView.setStrokeColor(MaterialColors.getColor(
                        requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK));
            }
        }

    }

}

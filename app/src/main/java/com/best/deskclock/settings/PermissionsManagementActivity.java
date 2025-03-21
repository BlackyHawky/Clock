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
import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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

            if (MiuiCheck.isMiui()) {
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
        }

        @Override
        public void onResume() {
            super.onResume();

            setStatusText();
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

            if (!isIgnoringBatteryOptimizations(requireContext())) {
                startActivity(intentGrant);
                sendPermissionResult();
            } else {
                displayRevocationDialog(intentRevoke);
            }
        }

        /**
         * Grant or revoke Notifications permission
         */
        private void grantOrRevokeNotificationsPermission() {
            Intent intent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, requireContext().getPackageName())
                    .addFlags(FLAG_ACTIVITY_NEW_TASK)
                    : new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

            if (areNotificationsEnabled(requireContext())) {
                displayRevocationDialog(intent);
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.notifications_dialog_title)
                        .setMessage(R.string.notifications_dialog_text)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            startActivity(intent);
                            sendPermissionResult();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requireActivity().requestPermissions(new String[]{POST_NOTIFICATIONS}, 0);
                } else {
                    startActivity(intent);
                }
            }

            sendPermissionResult();
        }

        /**
         * Grant or revoke Full Screen Notifications permission
         */
        private void grantOrRevokeFullScreenNotificationsPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                final Intent intent = new Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                        .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

                if (!areFullScreenNotificationsEnabled(requireContext())) {
                    startActivity(intent);
                    sendPermissionResult();
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
            if (!MiuiCheck.isMiui()) {
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
            new AlertDialog.Builder(requireContext())
                    .setIcon(iconId)
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setPositiveButton(R.string.permission_dialog_close_button, null)
                    .show();
        }

        /**
         * Display dialog when user wants to revoke permission.
         */
        private void displayRevocationDialog(Intent intent) {
            new AlertDialog.Builder(requireContext())
                    .setIcon(R.drawable.ic_key_off)
                    .setTitle(R.string.permission_dialog_revoke_title)
                    .setMessage(R.string.revoke_permission_dialog_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        startActivity(intent);
                        sendPermissionResult();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        /**
         * Sends the result of a permission request to the calling activity or parent fragment.
         * If the permission is granted, a result indicating success is sent.
         * This can be used by the calling component to perform any subsequent actions based on the permission result.
         */
        private void sendPermissionResult() {
            if (requireActivity() instanceof SettingsActivity) {
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            } else {
                requireActivity().setResult(REQUEST_CHANGE_PERMISSIONS);
            }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            } else {
                return NotificationManagerCompat.from(context).areNotificationsEnabled();
            }
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
                mIgnoreBatteryOptimizationsView.setStrokeColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLACK)
                );

                mNotificationView.setStrokeWidth(ThemeUtils.convertDpToPixels(2, requireContext()));
                mNotificationView.setStrokeColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLACK)
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
                mFullScreenNotificationsView.setStrokeColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLACK));
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
                mShowLockscreenView.setStrokeColor(
                        MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLACK));
            }
        }

    }

    /**
     * Class called to check if the device is running MIUI.
     */
    public static class MiuiCheck {

        /**
         * Check if the device is running MIUI.
         * <p>
         * By default, HyperOS is excluded from verification.
         * If you want to include HyperOS in the verification, pass excludeHyperOS as false.
         *
         * @param excludeHyperOS Indicate whether to exclude HyperOS.
         * @return {@code true} if the device is running MIUI ; {@code false} otherwise.
         */
        public static boolean isMiui(boolean excludeHyperOS) {
            // Check if the device is from Xiaomi, Redmi or POCO.
            String brand = Build.BRAND.toLowerCase();
            Set<String> xiaomiBrands = new HashSet<>(Arrays.asList("xiaomi", "redmi", "poco"));
            if (!xiaomiBrands.contains(brand)) {
                return false;
            }

            // This feature is present in both MIUI and HyperOS.
            String miuiVersion = getProperty("ro.miui.ui.version.name");
            boolean isMiui = miuiVersion != null && !miuiVersion.trim().isEmpty();
            // This feature is exclusive to HyperOS and is not present in MIUI.
            String hyperOSVersion = getProperty("ro.mi.os.version.name");
            boolean isHyperOS = hyperOSVersion != null && !hyperOSVersion.trim().isEmpty();

            return isMiui && (!excludeHyperOS || !isHyperOS);
        }

        /**
         * Private method to get the value of a system property.
         */
        private static String getProperty(String property) {
            BufferedReader reader = null;
            try {
                Process process = Runtime.getRuntime().exec("getprop " + property);
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
                return reader.readLine();
            } catch (IOException ignored) {
                return null;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        /**
         * Overload of isMiui method with excludeHyperOS set to true by default.
         */
        public static boolean isMiui() {
            return isMiui(true);
        }

    }

}

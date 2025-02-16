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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

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

        private int mRecyclerViewPosition = -1;

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
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.ignore_battery_optimizations_dialog_title)
                            .setMessage(R.string.ignore_battery_optimizations_dialog_text)
                            .setPositiveButton(R.string.permission_dialog_close_button, null)
                            .show()
            );

            mIgnoreBatteryOptimizationsStatus = rootView.findViewById(R.id.IBO_status_text);

            mNotificationView = rootView.findViewById(R.id.notification_view);
            mNotificationView.setOnClickListener(v -> grantOrRevokeNotificationsPermission());

            mNotificationDetails = rootView.findViewById(R.id.notification_details_button);
            mNotificationDetails.setOnClickListener(v ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.notifications_dialog_title)
                            .setMessage(R.string.notifications_dialog_text)
                            .setPositiveButton(R.string.permission_dialog_close_button, null)
                            .show()
            );

            mNotificationStatus = rootView.findViewById(R.id.notification_status_text);

            final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
            final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();

            updateCardViews(isCardBackgroundDisplayed, isCardBorderDisplayed);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mFullScreenNotificationsView = rootView.findViewById(R.id.FSN_view);
                mFullScreenNotificationsView.setVisibility(View.VISIBLE);
                mFullScreenNotificationsView.setOnClickListener(v -> grantOrRevokeFullScreenNotificationsPermission());

                mFullScreenNotificationsDetails = rootView.findViewById(R.id.FSN_details_button);
                mFullScreenNotificationsDetails.setOnClickListener(v ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.FSN_dialog_title)
                                .setMessage(R.string.FSN_dialog_text)
                                .setPositiveButton(R.string.permission_dialog_close_button, null)
                                .show()
                );
                mFullScreenNotificationsStatus = rootView.findViewById(R.id.FSN_status_text);

                updateFullScreenNotificationsCard(isCardBackgroundDisplayed, isCardBorderDisplayed);
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

            if (mRecyclerViewPosition != -1) {
                mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
                mAppBarLayout.setExpanded(mRecyclerViewPosition == 0, true);
            }
            setStatusText();
        }

        @Override
        public void onPause() {
            super.onPause();

            if (mLinearLayoutManager != null) {
                mRecyclerViewPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
            }
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
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.permission_dialog_revoke_title)
                        .setMessage(R.string.revoke_permission_dialog_message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            startActivity(intentRevoke);
                            sendPermissionResult();
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
                    ? new Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, requireContext().getPackageName())
                    .addFlags(FLAG_ACTIVITY_NEW_TASK)
                    : new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", requireContext().getPackageName(), null)).addFlags(FLAG_ACTIVITY_NEW_TASK);

            if (areNotificationsEnabled(requireContext())) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.permission_dialog_revoke_title)
                        .setMessage(R.string.revoke_permission_dialog_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            startActivity(intent);
                            sendPermissionResult();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
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
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.permission_dialog_revoke_title)
                            .setMessage(R.string.revoke_permission_dialog_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                startActivity(intent);
                                sendPermissionResult();
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
            if (requireContext().checkSelfPermission(PERMISSION_POWER_OFF_ALARM) != PackageManager.PERMISSION_GRANTED) {
                requireActivity().requestPermissions(new String[]{PERMISSION_POWER_OFF_ALARM}, codeForPowerOffAlarm);
            }
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

    }

}

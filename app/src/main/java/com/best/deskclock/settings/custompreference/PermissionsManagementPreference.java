// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FULL_SCREEN_NOTIFICATION_PERMISSION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_IGNORE_BATTERY_OPTIMIZATIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NOTIFICATION_PERMISSION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHOW_LOCKSCREEN_PERMISSION;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.PermissionUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

public class PermissionsManagementPreference extends Preference {

    private Context mContext;

    private TextView mStatusState;

    public PermissionsManagementPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_preference_permission);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        mContext = getContext();
        SharedPreferences prefs = getDefaultSharedPreferences(mContext);
        String fontPath = SettingsDAO.getGeneralFont(prefs);
        Typeface regularTypeFace = ThemeUtils.loadFont(fontPath);

        final MaterialCardView prefCardView = (MaterialCardView) holder.findViewById(R.id.pref_card_view);
        final boolean isCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(prefs);
        final boolean isCardBorderDisplayed = SettingsDAO.isCardBorderDisplayed(prefs);

        float strokeWidth = dpToPx(2, mContext.getResources().getDisplayMetrics());

        if (isCardBackgroundDisplayed) {
            prefCardView.setCardBackgroundColor(MaterialColors.getColor(prefCardView, com.google.android.material.R.attr.colorSurface));
        } else {
            prefCardView.setCardBackgroundColor(Color.TRANSPARENT);
        }

        if (isCardBorderDisplayed) {
            prefCardView.setStrokeWidth((int) strokeWidth);
        } else {
            prefCardView.setStrokeWidth(0);
        }

        super.onBindViewHolder(holder);

        TextView prefTitle = (TextView) holder.findViewById(android.R.id.title);
        final TextView requirementTitle = (TextView) holder.findViewById(R.id.requirement_title);
        final TextView requirementAdvice = (TextView) holder.findViewById(R.id.requirement_advice);
        final TextView statusTitle = (TextView) holder.findViewById(R.id.status_title);
        mStatusState = (TextView) holder.findViewById(R.id.status_state);

        prefTitle.setTypeface(ThemeUtils.boldTypeface(fontPath));
        requirementTitle.setTypeface(regularTypeFace);
        requirementAdvice.setTypeface(regularTypeFace);
        statusTitle.setTypeface(regularTypeFace);
        mStatusState.setTypeface(regularTypeFace);

        if (isShowLockScreenPermissionPreference()) {
            statusTitle.setText(mContext.getString(R.string.permission_info_title));
            mStatusState.setVisibility(GONE);
        } else {
            statusTitle.setText(mContext.getString(R.string.permission_status_title));

            if (isIgnoreBatteryOtimizationsPreference()) {
                setStatusText(PermissionUtils.isIgnoringBatteryOptimizations(mContext));
            } else if (isNotificationPermissionPreference()) {
                setStatusText(PermissionUtils.areNotificationsEnabled(mContext));
            } else if (isFullScreenNotificationPermissionPreference()) {
                setStatusText(PermissionUtils.areFullScreenNotificationsEnabled(mContext));
            }

            mStatusState.setVisibility(VISIBLE);
        }

        ImageButton detailsButton = (ImageButton) holder.findViewById(R.id.details_button);
        detailsButton.setOnClickListener(v -> displayPermissionDetailsDialog());

    }

    /**
     * Sets the permission status text.
     */
    public void setStatusText(boolean isGranted) {
        if (isGranted) {
            mStatusState.setText(mContext.getString(R.string.permission_granted));
            mStatusState.setTextColor(mContext.getColor(R.color.colorGranted));
        } else {
            mStatusState.setText(mContext.getString(R.string.permission_denied));
            mStatusState.setTextColor(mContext.getColor(R.color.colorAlert));
        }
    }

    /**
     * @return {@code true} if the current preference is related to the battery optimizations;
     * {@code false} otherwise.
     */
    private boolean isIgnoreBatteryOtimizationsPreference() {
        return getKey().equals(KEY_IGNORE_BATTERY_OPTIMIZATIONS);
    }

    /**
     * @return {@code true} if the current preference is related to the notification permission;
     * {@code false} otherwise.
     */
    private boolean isNotificationPermissionPreference() {
        return getKey().equals(KEY_NOTIFICATION_PERMISSION);
    }

    /**
     * @return {@code true} if the current preference is related to the full screen notification
     * permission; {@code false} otherwise.
     */
    private boolean isFullScreenNotificationPermissionPreference() {
        return getKey().equals(KEY_FULL_SCREEN_NOTIFICATION_PERMISSION);
    }

    /**
     * @return {@code true} if the current preference is related to the "Show lockscreen" permission
     * for MIUI devices; {@code false} otherwise.
     */
    private boolean isShowLockScreenPermissionPreference() {
        return getKey().equals(KEY_SHOW_LOCKSCREEN_PERMISSION);
    }

    /**
     * Display dialog when user wants to read the permission details.
     */
     private void displayPermissionDetailsDialog() {
         int iconId;
         int titleId;
         int messageId;

         if (isIgnoreBatteryOtimizationsPreference()) {
             iconId = R.drawable.ic_battery_settings;
             titleId = R.string.ignore_battery_optimizations_dialog_title;
             messageId = R.string.ignore_battery_optimizations_dialog_text;
         } else if (isNotificationPermissionPreference()) {
             iconId = R.drawable.ic_notifications;
             titleId = R.string.notifications_dialog_title;
             messageId = R.string.notifications_dialog_text;
         } else if (isFullScreenNotificationPermissionPreference()) {
             iconId = R.drawable.ic_fullscreen;
             titleId = R.string.FSN_dialog_title;
             messageId = R.string.FSN_dialog_text;
         } else {
             iconId = R.drawable.ic_screen_lock;
             titleId = R.string.show_lockscreen_dialog_title;
             messageId = R.string.show_lockscreen_dialog_text;
         }

         CustomDialog.create(
                 mContext,
                 null,
                 AppCompatResources.getDrawable(mContext, iconId),
                 mContext.getString(titleId),
                 mContext.getString(messageId),
                 null,
                 mContext.getString(R.string.dialog_close),
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 CustomDialog.SoftInputMode.NONE
         ).show();
     }

    public void refreshState() {
        notifyChanged();
    }

}

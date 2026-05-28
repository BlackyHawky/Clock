// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FULL_SCREEN_NOTIFICATION_PERMISSION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_IGNORE_BATTERY_OPTIMIZATIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NOTIFICATION_PERMISSION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHOW_LOCKSCREEN_PERMISSION;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.SettingsPreferencePermissionBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.PermissionUtils;
import com.best.deskclock.utils.ThemeUtils;

public class PermissionsManagementPreference extends Preference {

    private SettingsPreferencePermissionBinding mBinding;

    private final Typeface mRegularTypeface;

    private AlertDialog mActiveDialog = null;

    public PermissionsManagementPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mRegularTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(context)));
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        super.onBindViewHolder(holder);

        mBinding = SettingsPreferencePermissionBinding.bind(holder.itemView);

        mBinding.requirementTitle.setTypeface(mRegularTypeface);
        mBinding.requirementAdvice.setTypeface(mRegularTypeface);
        mBinding.statusTitle.setTypeface(mRegularTypeface);
        mBinding.statusState.setTypeface(mRegularTypeface);

        if (isShowLockScreenPermissionPreference()) {
            mBinding.statusTitle.setText(getContext().getString(R.string.permission_info_title));
            mBinding.statusState.setVisibility(GONE);
        } else {
            mBinding.statusTitle.setText(getContext().getString(R.string.permission_status_title));

            if (isIgnoreBatteryOptimizationsPreference()) {
                setStatusText(PermissionUtils.isIgnoringBatteryOptimizations(getContext()));
            } else if (isNotificationPermissionPreference()) {
                setStatusText(PermissionUtils.areNotificationsEnabled(getContext()));
            } else if (isFullScreenNotificationPermissionPreference()) {
                setStatusText(PermissionUtils.areFullScreenNotificationsEnabled(getContext()));
            }

            mBinding.statusState.setVisibility(VISIBLE);
        }

        mBinding.detailsButton.setOnClickListener(v -> displayPermissionDetailsDialog());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        if (mActiveDialog == null || !mActiveDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        if (myState.isDialogShowing) {
            displayPermissionDetailsDialog();
        }
    }

    @Override
    public void onDetached() {
        if (mActiveDialog != null && mActiveDialog.isShowing()) {
            mActiveDialog.dismiss();
            mActiveDialog = null;
        }
        super.onDetached();
    }

    /**
     * Sets the permission status text.
     */
    public void setStatusText(boolean isGranted) {
        if (isGranted) {
            mBinding.statusState.setText(getContext().getString(R.string.permission_granted));
            mBinding.statusState.setTextColor(ContextCompat.getColor(getContext(), R.color.colorGranted));
        } else {
            mBinding.statusState.setText(getContext().getString(R.string.permission_denied));
            mBinding.statusState.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAlert));
        }
    }

    /**
     * @return {@code true} if the current preference is related to the battery optimizations;
     * {@code false} otherwise.
     */
    private boolean isIgnoreBatteryOptimizationsPreference() {
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

        if (isIgnoreBatteryOptimizationsPreference()) {
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

        mActiveDialog = CustomDialog.create(
            getContext(),
            null,
            AppCompatResources.getDrawable(getContext(), iconId),
            getContext().getString(titleId),
            getContext().getString(messageId),
            null,
            getContext().getString(R.string.dialog_close),
            null,
            null,
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    public void refreshState() {
        notifyChanged();
    }

    private static class SavedState extends BaseSavedState {
        boolean isDialogShowing;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);

            isDialogShowing = source.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeInt(isDialogShowing ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }

                @Override
                public SavedState createFromParcel(Parcel source) {
                    return new SavedState(source);
                }
            };
    }

}

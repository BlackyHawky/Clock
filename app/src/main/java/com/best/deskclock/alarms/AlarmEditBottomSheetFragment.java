// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.FILE_SPECIFIC_ALARM_BACKGROUND;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.IntentCompat;
import androidx.core.os.BundleCompat;
import androidx.core.util.Pair;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.databinding.AlarmEditBottomSheetBinding;
import com.best.deskclock.databinding.DeskClockBinding;
import com.best.deskclock.dialogfragment.AlarmDelayPickerDialogFragment;
import com.best.deskclock.dialogfragment.AlarmMathHardnessLevelDialogFragment;
import com.best.deskclock.dialogfragment.AlarmMissedRepeatLimitDialogFragment;
import com.best.deskclock.dialogfragment.AlarmSnoozeDurationDialogFragment;
import com.best.deskclock.dialogfragment.AlarmVolumeDialogFragment;
import com.best.deskclock.dialogfragment.AutoSilenceDurationDialogFragment;
import com.best.deskclock.dialogfragment.BlurIntensityDialogFragment;
import com.best.deskclock.dialogfragment.DatePickerDialogFragment;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.dialogfragment.MaterialTimePickerDialogFragment;
import com.best.deskclock.dialogfragment.SpinnerDatePickerDialogFragment;
import com.best.deskclock.dialogfragment.SpinnerTimePickerDialogFragment;
import com.best.deskclock.dialogfragment.VibrationPatternDialogFragment;
import com.best.deskclock.dialogfragment.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.settings.AlarmDisplayPreviewActivity;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.CustomTooltip;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;
import com.best.deskclock.widgets.NextAlarmAppWidgetProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class AlarmEditBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "alarm_edit_bottom_sheet";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_ALARM_ID = "arg_alarm_id";
    private static final String ARG_IS_NEW_ALARM = "arg_is_new_alarm";
    private static final String ARG_TAG = "arg_tag";
    public static final String SCROLL_TO_ALARM_ID = "scroll_to_alarm_id";
    public static final String REQUEST_KEY = "alarm_saved";

    private static final String KEY_SHOW_PAUSE_ALARM_NOTE_DIALOG = "show_pause_alarm_note_dialog";
    private static final String KEY_SHOW_DELETE_ALARM_AFTER_USE_NOTE_DIALOG = "show_delete_alarm_after_use_note_dialog";
    private static final String KEY_SHOW_AUTO_SILENCE_NOTE_DIALOG = "show_auto_silence_note_dialog";
    private static final String KEY_AUTO_SILENCE_DURATION = "auto_silence_duration";

    private AlarmEditBottomSheetBinding mBinding;
    private boolean mIsFadeTransition;
    private SharedPreferences mPrefs;
    private UiConfig.CardStyle mCardStyleConfig;
    private Typeface mGeneralTypeface;
    private Typeface mAlarmFont;
    private Typeface mAlarmBoldTypeface;
    private boolean mIsVibrationEnabled;
    private int mAccentStyle;
    private DisplayMetrics mDisplayMetrics;
    private DataModel mDataModel;
    private UiDataModel mUiDataModel;
    private Alarm mAlarm;
    private Alarm mOriginalAlarm;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private String mTag;

    private Drawable mDeleteAlarmAfterUseDrawableStart;
    private Drawable mDeleteAlarmAfterUseDrawableEnd;

    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private boolean mIs24HourFormat;
    private String mMaterialTimePickerStyle;
    private String mMaterialDatePickerStyle;
    private int mFirstDayOfWeek;
    private boolean mIsNewAlarm;
    private boolean mIsDeleted;

    private AlertDialog mActiveDialog = null;
    private boolean mShowPauseAlarmNoteDialog = false;
    private boolean mShowDeleteAlarmAfterUseNoteDialog = false;
    private boolean mShowAutoSilenceNoteDialog = false;
    private String mAutoSilenceDuration = null;

    @NonNull
    public static AlarmEditBottomSheetFragment newInstance(@NonNull Alarm alarm, long alarmId, @Nullable String tag, boolean isNewAlarm) {

        final Bundle args = new Bundle();

        args.putParcelable(ARG_ALARM, alarm);
        args.putLong(ARG_ALARM_ID, alarmId);
        args.putString(ARG_TAG, tag);
        args.putBoolean(ARG_IS_NEW_ALARM, isNewAlarm);

        final AlarmEditBottomSheetFragment fragment = new AlarmEditBottomSheetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static void show(@NonNull FragmentManager manager, @NonNull AlarmEditBottomSheetFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    private final ActivityResultLauncher<Intent> mRingtonePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = IntentCompat.getParcelableExtra(result.getData(), RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);

                mAlarm.alert = (uri != null) ? uri : RingtoneUtils.RINGTONE_SILENT;

                bindRingtone();
            }
        }
    );

    private final ActivityResultLauncher<Intent> mImagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK) {
                return;
            }

            Intent intent = result.getData();
            final Uri sourceUri = intent == null ? null : intent.getData();
            if (sourceUri == null) {
                return;
            }

            final Context appContext = requireContext().getApplicationContext();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = FileUtils.toSafeFileName(
                FILE_SPECIFIC_ALARM_BACKGROUND + "_" + mAlarm.id + "_" + System.currentTimeMillis()
            );
            String oldImagePath = mAlarm.backgroundImage;

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old image if it exists
                FileUtils.clearFile(oldImagePath);

                // Copy the new image to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mAlarm.backgroundImage = copiedUri.getPath();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, mAccentStyle, mGeneralTypeface, R.string.background_image_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, mAccentStyle, mGeneralTypeface, R.string.image_message_error);
                    }

                    if (!isAdded() || mBinding == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        bindAlarmBackgroundImage();
                        bindBlurIntensity();
                        updateFourthGroup();
                    }
                });
            });
        });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTag = requireArguments().getString(ARG_TAG);
        mIsNewAlarm = requireArguments().getBoolean(ARG_IS_NEW_ALARM, false);

        mPrefs = getDefaultSharedPreferences(requireContext());
        mIsVibrationEnabled = SettingsDAO.isVibrationsEnabled(mPrefs);
        mIsFadeTransition = SettingsDAO.isFadeTransitionsEnabled(mPrefs);
        mGeneralTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs));
        mAlarmFont = ThemeUtils.loadFont(SettingsDAO.getAlarmFont(mPrefs));
        mAlarmBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getAlarmFont(mPrefs));

        mAccentStyle = ThemeUtils.getAccentStyle(requireContext(),
            SettingsDAO.isAutoNightAccentColorEnabled(mPrefs),
            SettingsDAO.getAccentColor(mPrefs),
            SettingsDAO.getNightAccentColor(mPrefs));

        mCardStyleConfig = new UiConfig.CardStyle(
            SettingsDAO.isCardBackgroundDisplayed(mPrefs),
            SettingsDAO.isCardBorderDisplayed(mPrefs),
            SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE)
        );

        mDataModel = DataModel.getDataModel();
        mUiDataModel = UiDataModel.getUiDataModel();
        mDisplayMetrics = getResources().getDisplayMetrics();

        mDeleteAlarmAfterUseDrawableStart = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_clear);
        mDeleteAlarmAfterUseDrawableEnd = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_selector_checkbox);

        mFormat12 = ClockUtils.get12ModeFormat(false, 0.5f, mAlarmBoldTypeface, "sans-serif", Typeface.BOLD, false);
        mFormat24 = ClockUtils.get24ModeFormat(false, false);

        mMaterialTimePickerStyle = SettingsDAO.getMaterialTimePickerStyle(mPrefs);
        mMaterialDatePickerStyle = SettingsDAO.getMaterialDatePickerStyle(mPrefs);
        mFirstDayOfWeek = SettingsDAO.getFirstDayOfWeek(mPrefs);

        setupFragmentResultListeners();
    }

    @Override
    public void onStart() {
        super.onStart();

        DeskClock activity = (DeskClock) requireActivity();
        DeskClockBinding activityBinding = activity.getDeskClockBinding();

        mAlarmUpdateHandler = new AlarmUpdateHandler(
            requireContext(), mPrefs, mGeneralTypeface, null, activityBinding.contentView, mIsVibrationEnabled);
    }

    @Override
    public void onDestroyView() {
        mAlarmUpdateHandler = null;

        mBinding = null;

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        if (mAlarm != null) {
            outState.putParcelable(ARG_ALARM, mAlarm);
        }

        outState.putBoolean(KEY_SHOW_PAUSE_ALARM_NOTE_DIALOG, mShowPauseAlarmNoteDialog);
        outState.putBoolean(KEY_SHOW_DELETE_ALARM_AFTER_USE_NOTE_DIALOG, mShowDeleteAlarmAfterUseNoteDialog);
        outState.putBoolean(KEY_SHOW_AUTO_SILENCE_NOTE_DIALOG, mShowAutoSilenceNoteDialog);
        outState.putString(KEY_AUTO_SILENCE_DURATION, mAutoSilenceDuration);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        Window window = dialog.getWindow();
        if (window != null) {
            // Display within the cutout area
            ThemeUtils.allowDisplayCutout(window);

            // To prevent flickering when a 'MaterialAlertDialog' opens on top of this BottomSheet, remove the background dimming
            // caused by the BottomSheet. The 'MaterialAlertDialog' will handle this dimming.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // Prevent the BottomSheet from moving when the keyboard opens (for example, when editing the alarm label).
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }

        final Bundle bundleToUse = (savedInstanceState != null) ? savedInstanceState : requireArguments();
        Alarm alarmFromArguments = BundleCompat.getParcelable(bundleToUse, ARG_ALARM, Alarm.class);

        if (alarmFromArguments == null) {
            dismiss();
            return dialog;
        }

        mOriginalAlarm = new Alarm(alarmFromArguments);
        mAlarm = new Alarm(alarmFromArguments);

        mBinding = AlarmEditBottomSheetBinding.inflate(getLayoutInflater());

        dialog.setContentView(mBinding.getRoot());

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        if (savedInstanceState != null) {
            mShowPauseAlarmNoteDialog = savedInstanceState.getBoolean(KEY_SHOW_PAUSE_ALARM_NOTE_DIALOG);
            mShowDeleteAlarmAfterUseNoteDialog = savedInstanceState.getBoolean(KEY_SHOW_DELETE_ALARM_AFTER_USE_NOTE_DIALOG);
            mShowAutoSilenceNoteDialog = savedInstanceState.getBoolean(KEY_SHOW_AUTO_SILENCE_NOTE_DIALOG);
            mAutoSilenceDuration = savedInstanceState.getString(KEY_AUTO_SILENCE_DURATION);
        }

        ThemeUtils.applyFontToTextViews(mBinding.getRoot(), mGeneralTypeface);

        bindCustomDragHandleTooltip();
        bindClock();
        bindDaysOfWeekButtons();
        bindSelectedDate();
        bindPauseAlarm();
        bindLabel();
        bindRingtone();
        bindVibrator();
        bindVibrationPattern();
        bindFlash();
        bindDeleteAlarmAfterUse();
        bindAutoSilenceValue();
        bindSnoozeDurationValue();
        bindMissedAlarmRepeatLimit();
        bindAlarmHardnessLevel();
        bindCrescendoDuration();
        bindAlarmVolume();
        bindSpace();
        bindAlarmBackgroundImage();
        bindBlurIntensity();
        bindDeleteButton();
        bindDuplicateButton();
        bindPreviewButton();
        bindSaveButton();

        updateAllGroupBackgrounds();

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheetInternal != null) {
                bottomSheetInternal.setElevation(dpToPx(12, mDisplayMetrics));

                View parent = (View) bottomSheetInternal.getParent();

                if (parent != null) {
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(parent);
                    int topInset = insets != null
                        ? insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()).top
                        : 0;
                    int availableHeight = parent.getHeight() - topInset;
                    BottomSheetBehavior.from(bottomSheetInternal).setMaxHeight(availableHeight);
                }
            }
        });

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isSystem24Hour = mDataModel.is24HourFormat();

        if (mIs24HourFormat != isSystem24Hour) {
            mIs24HourFormat = mDataModel.is24HourFormat();
            mBinding.digitalClock.configure(mIs24HourFormat, mFormat12, mFormat24);
        }

        restoreMaterialTimePickerListener();
        restoreMaterialDatePickerListener();
        restoreMaterialDateRangePickerListener();

        if (mShowPauseAlarmNoteDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            showPauseAlarmNoteDialog();
        } else if (mShowDeleteAlarmAfterUseNoteDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            showDeleteAlarmAfterUseNoteDialog();
        } else if (mShowAutoSilenceNoteDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            showAutoSilenceNoteDialog(mAutoSilenceDuration);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (getActivity() != null && !getActivity().isChangingConfigurations()) {
            saveAlarmSettings();
        }

        // When the per-alarm volume feature is enabled, AlarmFragment temporarily "freezes"
        // its volume warning banner.
        // This prevents the banner from glitching or disappearing when the user tests
        // the alarm volume in the sub-dialog (AlarmVolumeDialogFragment).
        // Therefore, when this BottomSheet is fully dismissed, we must force the parent
        // AlarmFragment to re-evaluate the actual system volume.
        // This catches any system volume changes the user might have made
        // (e.g., using hardware buttons) while the UI was frozen, ensuring the banner state
        // remains perfectly synchronized.
        if (SettingsDAO.isPerAlarmVolumeEnabled(mPrefs)) {
            Fragment parentFragment = getParentFragmentManager().findFragmentByTag(mTag);
            if (parentFragment instanceof AlarmFragment alarmFragment) {
                alarmFragment.updateWarningBannerVisibility();
            }
        }

        if (mActiveDialog != null && mActiveDialog.isShowing()) {
            mActiveDialog.dismiss();
            mActiveDialog = null;
        }

        super.onDismiss(dialog);
    }

    private void bindCustomDragHandleTooltip() {
        CharSequence nativeText = mBinding.dragHandle.getContentDescription();
        String tooltipText = nativeText != null ? nativeText.toString() : "";

        TooltipCompat.setTooltipText(mBinding.dragHandle, null);

        mBinding.dragHandle.setOnLongClickListener(v -> {
            if (!tooltipText.isEmpty()) {
                CustomTooltip.showBelow(v, mGeneralTypeface, mDisplayMetrics, tooltipText);
            }
            return true;
        });
    }

    private void bindClock() {
        mIs24HourFormat = mDataModel.is24HourFormat();

        mBinding.digitalClock.configure(mIs24HourFormat, mFormat12, mFormat24);
        mBinding.digitalClock.setBackground(ThemeUtils.pillRippleDrawable(requireContext(), mDisplayMetrics, Color.TRANSPARENT));
        mBinding.digitalClock.setTime(mAlarm.hour, mAlarm.minutes);
        mBinding.digitalClock.setTypeface(mAlarmBoldTypeface);

        mBinding.digitalClock.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

            if (mMaterialTimePickerStyle.equals(SPINNER_TIME_PICKER_STYLE)) {
                final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(mAlarm.hour, mAlarm.minutes);
                SpinnerTimePickerDialogFragment.show(getChildFragmentManager(), fragment);
            } else {
                MaterialTimePickerDialogFragment.show(
                    requireContext(),
                    getChildFragmentManager(),
                    TAG,
                    mAlarm.hour,
                    mAlarm.minutes,
                    mMaterialTimePickerStyle,
                    mAlarmFont,
                    mGeneralTypeface
                );
            }
        });

        mBinding.digitalClock.setOnLongClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_delay, R.string.label_deskclock);

            final AlarmDelayPickerDialogFragment fragment = AlarmDelayPickerDialogFragment.newInstance(0, 0);
            AlarmDelayPickerDialogFragment.show(getChildFragmentManager(), fragment);

            return true;
        });
    }

    private void bindDaysOfWeekButtons() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<Integer> weekdays = SettingsDAO.getWeekdayOrder(mPrefs).getCalendarDays();

        mBinding.repeatDaysGroup.removeAllViews();

        final MaterialButton[] dayButtons = new MaterialButton[7];

        for (int i = 0; i < 7; i++) {
            MaterialButton dayButton = (MaterialButton) inflater.inflate(R.layout.day_button, mBinding.repeatDaysGroup, false);
            int weekday = weekdays.get(i);

            dayButton.setId(View.generateViewId());
            dayButton.setTypeface(mGeneralTypeface);
            dayButton.setText(mUiDataModel.getShortWeekday(weekday));
            dayButton.setContentDescription(mUiDataModel.getLongWeekday(weekday));

            mBinding.repeatDaysGroup.addView(dayButton);
            dayButtons[i] = dayButton;

            boolean isChecked = mAlarm.daysOfWeek.isBitOn(weekday);

            if (isChecked) {
                mBinding.repeatDaysGroup.check(dayButton.getId());
            }

            updateDaysOfWeekButtonVisuals(dayButtons[i], isChecked);
        }

        mBinding.repeatDaysGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            for (int i = 0; i < dayButtons.length; i++) {
                if (dayButtons[i].getId() == checkedId) {
                    Utils.performHapticFeedback(dayButtons[i], mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                    int weekday = weekdays.get(i);
                    mAlarm.daysOfWeek = mAlarm.daysOfWeek.setBit(weekday, isChecked);
                    updateDaysOfWeekButtonVisuals(dayButtons[i], isChecked);

                    if (!mAlarm.daysOfWeek.isRepeating()) {
                        mAlarm.pauseStartDate = 0;
                        mAlarm.pauseEndDate = 0;
                    }

                    if (mAlarm.daysOfWeek.getBits() == mOriginalAlarm.daysOfWeek.getBits()) {
                        // If the user has set the days exactly as they were originally, restore the original date to undo the change
                        // when saving the alarm.
                        mAlarm.year = mOriginalAlarm.year;
                        mAlarm.month = mOriginalAlarm.month;
                        mAlarm.day = mOriginalAlarm.day;
                    } else {
                        // Otherwise, set the date to today.
                        final Calendar now = Calendar.getInstance();
                        mAlarm.year = now.get(Calendar.YEAR);
                        mAlarm.month = now.get(Calendar.MONTH);
                        mAlarm.day = now.get(Calendar.DAY_OF_MONTH);
                    }

                    bindSelectedDate();
                    bindPauseAlarm();
                    bindDeleteAlarmAfterUse();
                    break;
                }
            }
        });
    }

    private void bindSelectedDate() {
        int openCalendarText = R.string.schedule_alarm_title;

        mBinding.scheduleAlarmLayout.setOnClickListener(v -> DatePickerDialogFragment.show(
            getChildFragmentManager(),
            mAlarm,
            mMaterialDatePickerStyle,
            mFirstDayOfWeek,
            mGeneralTypeface,
            this::applyDate)
        );

        if (mAlarm.daysOfWeek.isRepeating()) {
            clearSelectedDate(openCalendarText);
        } else if (mAlarm.isSpecifiedDate()) {
            if (mAlarm.isDateInThePast()) {
                clearSelectedDate(openCalendarText);
            } else {
                mBinding.scheduleAlarm.setText(AlarmUtils.formatAlarmDate(mAlarm));

                mBinding.cancelScheduledAlarm.setOnClickListener(v -> {
                    Calendar now = Calendar.getInstance();
                    mAlarm.year = now.get(Calendar.YEAR);
                    mAlarm.month = now.get(Calendar.MONTH);
                    mAlarm.day = now.get(Calendar.DAY_OF_MONTH);

                    bindSelectedDate();
                });
                mBinding.cancelScheduledAlarm.setVisibility(VISIBLE);
            }
        } else {
            clearSelectedDate(openCalendarText);
        }
    }

    private void bindPauseAlarm() {
        boolean isRepeating = mAlarm.daysOfWeek.isRepeating();

        mBinding.pauseAlarmLayout.setEnabled(isRepeating);
        mBinding.pauseAlarm.setEnabled(isRepeating);

        mAlarm.clearPauseIfExpired();

        if (isRepeating && mAlarm.isPauseSet()) {
            String dateRangeStr = AlarmUtils.formatPauseDateRange(requireContext(), mAlarm.pauseStartDate, mAlarm.pauseEndDate);

            mBinding.pauseAlarm.setText(getString(R.string.pause_alarm_range, dateRangeStr));

            mBinding.cancelPauseAlarm.setVisibility(View.VISIBLE);
        } else {
            mBinding.pauseAlarm.setText(R.string.pause_alarm_title);

            mBinding.cancelPauseAlarm.setVisibility(View.GONE);
        }

        View.OnClickListener showMaterialDateRangePicker = v -> DatePickerDialogFragment.showMaterialDateRangePicker(
            getChildFragmentManager(),
            mAlarm,
            mFirstDayOfWeek,
            mGeneralTypeface,
            (start, end) -> {
                mAlarm.pauseStartDate = start;
                mAlarm.pauseEndDate = end;
                bindPauseAlarm();
            }
        );

        View.OnClickListener resetPauseDate = v -> {
            mAlarm.pauseStartDate = 0;
            mAlarm.pauseEndDate = 0;
            bindPauseAlarm();
        };

        mBinding.pauseAlarmLayout.setOnClickListener(isRepeating ? showMaterialDateRangePicker : null);

        mBinding.cancelPauseAlarm.setOnClickListener(isRepeating ? resetPauseDate : null);

        mBinding.pauseAlarmNote.setVisibility(isRepeating ? GONE : VISIBLE);
        mBinding.pauseAlarmNote.setOnClickListener(isRepeating ? null : v -> showPauseAlarmNoteDialog());
    }

    private void bindLabel() {
        final boolean alarmLabelIsEmpty = mAlarm.label == null || mAlarm.label.isEmpty();

        mBinding.editLabel.setText(alarmLabelIsEmpty ? getString(R.string.add_label) : mAlarm.label);

        mBinding.editLabel.setContentDescription(alarmLabelIsEmpty
            ? getString(R.string.no_label_specified)
            : getString(R.string.label_description) + " " + mAlarm.label);

        mBinding.editLabel.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);

            final LabelDialogFragment fragment = LabelDialogFragment.newInstance(mAlarm.label, mAlarm.syncByLabel);
            LabelDialogFragment.show(getChildFragmentManager(), fragment);
        });
    }

    private void bindRingtone() {
        final String title = DataModel.getDataModel().getRingtoneTitle(mAlarm.alert);
        mBinding.chooseRingtone.setText(title);

        final String description = getString(R.string.ringtone_description);
        mBinding.chooseRingtone.setContentDescription(description + " " + title);

        final Drawable iconRingtone;
        if (RingtoneUtils.RINGTONE_SILENT.equals(mAlarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone_silent);
        } else if (RingtoneUtils.isRandomRingtone(mAlarm.alert) || RingtoneUtils.isRandomCustomRingtone(mAlarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_random);
        } else {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone);
        }

        mBinding.chooseRingtone.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRingtone, null, null, null);

        mBinding.chooseRingtone.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);
            final Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntent(requireContext(), mAlarm);
            mRingtonePickerLauncher.launch(intent);
        });
    }

    private void bindVibrator() {
        if (!DeviceUtils.hasVibrator(requireContext())) {
            mBinding.vibrateOnOff.setVisibility(GONE);
            mBinding.vibrationPatternLayout.setVisibility(GONE);
            return;
        }

        mBinding.vibrateOnOff.setVisibility(VISIBLE);

        mBinding.vibrateOnOff.setOnCheckedChangeListener(null);
        mBinding.vibrateOnOff.setChecked(mAlarm.vibrate);

        mBinding.vibrateOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Events.sendAlarmEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
            mAlarm.vibrate = isChecked;
            bindVibrationPattern();
            updateSecondGroup();
            if (isChecked) {
                Utils.setVibrationTime(requireContext(), mIsVibrationEnabled, 300);
            }
        });
    }

    private void bindVibrationPattern() {
        if (!mAlarm.vibrate || !SettingsDAO.isPerAlarmVibrationPatternEnabled(mPrefs)) {
            mBinding.vibrationPatternLayout.setVisibility(GONE);
            return;
        }

        mBinding.vibrationPatternLayout.setVisibility(VISIBLE);

        String vibrationPatternText = mAlarm.vibrationPattern;
        switch (vibrationPatternText) {
            case VIBRATION_PATTERN_SOFT -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_soft));
            case VIBRATION_PATTERN_STRONG -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_strong));
            case VIBRATION_PATTERN_HEARTBEAT -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_heartbeat));
            case VIBRATION_PATTERN_ESCALATING -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_escalating));
            case VIBRATION_PATTERN_TICK_TOCK -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_tick_tock));
            default -> mBinding.vibrationPatternValue.setText(getString(R.string.label_default));
        }

        View.OnClickListener openVibrationPatternFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_vibration_pattern, R.string.label_deskclock);

            final VibrationPatternDialogFragment fragment = VibrationPatternDialogFragment.newInstance(mAlarm.vibrationPattern);
            VibrationPatternDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.vibrationPatternLayout.setOnClickListener(openVibrationPatternFragment);
    }

    private void bindFlash() {
        if (!DeviceUtils.hasBackFlash(requireContext())) {
            mBinding.flashOnOff.setVisibility(GONE);
            return;
        }

        mBinding.flashOnOff.setVisibility(VISIBLE);
        mBinding.flashOnOff.setOnCheckedChangeListener(null);
        mBinding.flashOnOff.setChecked(mAlarm.flash);
        mBinding.flashOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.performHapticFeedback(buttonView, mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            Events.sendAlarmEvent(R.string.action_toggle_flash, R.string.label_deskclock);
            mAlarm.flash = isChecked;
        });
    }

    private void bindDeleteAlarmAfterUse() {
        final boolean isRepeating = mAlarm.daysOfWeek.isRepeating();

        mBinding.deleteAlarmAfterUseNote.setVisibility(isRepeating ? VISIBLE : GONE);
        mBinding.deleteAlarmAfterUseNote.setOnClickListener(isRepeating ? v -> showDeleteAlarmAfterUseNoteDialog() : null);

        mBinding.deleteAlarmAfterUse.setCompoundDrawablesRelativeWithIntrinsicBounds(
            mDeleteAlarmAfterUseDrawableStart,
            null,
            isRepeating ? null : mDeleteAlarmAfterUseDrawableEnd,
            null
        );

        mBinding.deleteAlarmAfterUseLayout.setEnabled(!isRepeating);
        mBinding.deleteAlarmAfterUse.setEnabled(!isRepeating);
        mBinding.deleteAlarmAfterUse.setOnCheckedChangeListener(null);
        mBinding.deleteAlarmAfterUse.setChecked(!isRepeating && mAlarm.deleteAfterUse);

        mBinding.deleteAlarmAfterUse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.performHapticFeedback(buttonView, mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            mAlarm.deleteAfterUse = isChecked;
        });
    }

    private void bindAutoSilenceValue() {
        if (SettingsDAO.isPerAlarmAutoSilenceDisabled(mPrefs)) {
            mBinding.autoSilenceDurationLayout.setVisibility(GONE);
            return;
        }

        int autoSilenceDuration = mAlarm.autoSilenceDuration;
        boolean hasMathMission = !mAlarm.mathHardnessLevel.equals(DEFAULT_MATH_HARDNESS_LEVEL);

        mBinding.autoSilenceDurationLayout.setVisibility(VISIBLE);
        mBinding.autoSilenceDurationLayout.setEnabled(!hasMathMission);
        mBinding.autoSilenceDurationTitle.setEnabled(!hasMathMission);
        mBinding.autoSilenceDurationValue.setEnabled(!hasMathMission);
        mBinding.autoSilenceDurationValue.setVisibility(hasMathMission ? GONE : VISIBLE);
        mBinding.autoSilenceNote.setVisibility(hasMathMission ? VISIBLE : GONE);

        if (hasMathMission) {
            String noteText;

            if (autoSilenceDuration == TIMEOUT_NEVER) {
                noteText = getString(R.string.label_never);
            } else {
                int m = Math.max(Math.max(autoSilenceDuration, 0), DEFAULT_AUTO_SILENCE_DURATION) / 60;
                noteText = getResources().getQuantityString(R.plurals.minutes_short, m, m);
            }

            mBinding.autoSilenceNote.setOnClickListener(v -> showAutoSilenceNoteDialog(noteText));
            mBinding.autoSilenceDurationLayout.setOnClickListener(null);
        } else {
            mBinding.autoSilenceDurationValue.setText(Utils.formatAutoSilenceDurationText(requireContext(), autoSilenceDuration));
            mBinding.autoSilenceNote.setOnClickListener(null);

            mBinding.autoSilenceDurationLayout.setOnClickListener(v -> {
                Events.sendAlarmEvent(R.string.action_set_auto_silence_duration, R.string.label_deskclock);

                final AutoSilenceDurationDialogFragment fragment =
                    AutoSilenceDurationDialogFragment.newInstance(mAlarm.autoSilenceDuration);

                AutoSilenceDurationDialogFragment.show(getChildFragmentManager(), fragment);
            });
        }
    }

    private void bindSnoozeDurationValue() {
        if (SettingsDAO.isPerAlarmSnoozeDurationDisabled(mPrefs)) {
            mBinding.snoozeDurationLayout.setVisibility(GONE);
            return;
        }

        int snoozeDuration = mAlarm.snoozeDuration;

        mBinding.snoozeDurationLayout.setVisibility(VISIBLE);

        mBinding.snoozeDurationValue.setText(formatSnoozeDurationText(snoozeDuration));

        mBinding.snoozeDurationLayout.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_snooze_duration, R.string.label_deskclock);

            final AlarmSnoozeDurationDialogFragment fragment = AlarmSnoozeDurationDialogFragment.newInstance(mAlarm.snoozeDuration);
            AlarmSnoozeDurationDialogFragment.show(getChildFragmentManager(), fragment);
        });
    }

    private void bindMissedAlarmRepeatLimit() {
        if (SettingsDAO.isPerAlarmMissedRepeatLimitDisabled(mPrefs)
            || mAlarm.autoSilenceDuration == TIMEOUT_NEVER
            || mAlarm.snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
            mBinding.missedAlarmRepeatLimitLayout.setVisibility(GONE);
            return;
        }

        int missedAlarmRepeatLimit = mAlarm.missedAlarmRepeatLimit;

        switch (missedAlarmRepeatLimit) {
            case 0 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.label_never));
            case 1 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_1_time));
            case 3 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_3_times));
            case 5 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_5_times));
            case 10 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_10_times));
            default -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.label_indefinitely));
        }

        mBinding.missedAlarmRepeatLimitLayout.setVisibility(VISIBLE);

        View.OnClickListener openAlarmMissedRepeatLimitFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_missed_alarm_repeat_limit, R.string.label_deskclock);

            final AlarmMissedRepeatLimitDialogFragment fragment =
                AlarmMissedRepeatLimitDialogFragment.newInstance(mAlarm.missedAlarmRepeatLimit);

            AlarmMissedRepeatLimitDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.missedAlarmRepeatLimitLayout.setOnClickListener(openAlarmMissedRepeatLimitFragment);
    }

    private void bindAlarmHardnessLevel() {
        if (SettingsDAO.isPerAlarmMathHardnessLevelDisabled(mPrefs)) {
            mBinding.mathHardnessLevelLayout.setVisibility(GONE);
            return;
        }

        mBinding.mathHardnessLevelLayout.setVisibility(VISIBLE);

        String mathHardnessLevelText = mAlarm.mathHardnessLevel;
        switch (mathHardnessLevelText) {
            case MATH_HARDNESS_LEVEL_EASY -> mBinding.mathHardnessLevelValue.setText(getString(R.string.math_hardness_level_easy));
            case MATH_HARDNESS_LEVEL_NORMAL -> mBinding.mathHardnessLevelValue.setText(getString(R.string.math_hardness_level_normal));
            case MATH_HARDNESS_LEVEL_HARD -> mBinding.mathHardnessLevelValue.setText(getString(R.string.math_hardness_level_hard));
            default -> mBinding.mathHardnessLevelValue.setText(getString(R.string.label_off));
        }

        View.OnClickListener openMathHardnessLevelDialogFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_math_hardness_level, R.string.label_deskclock);

            final AlarmMathHardnessLevelDialogFragment fragment =
                AlarmMathHardnessLevelDialogFragment.newInstance(mAlarm.mathHardnessLevel);
            AlarmMathHardnessLevelDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.mathHardnessLevelLayout.setOnClickListener(openMathHardnessLevelDialogFragment);
    }

    private void bindCrescendoDuration() {
        if (SettingsDAO.isPerAlarmCrescendoDurationDisabled(mPrefs)) {
            mBinding.crescendoDurationLayout.setVisibility(GONE);
            return;
        }

        int crescendoDuration = mAlarm.crescendoDuration;

        mBinding.crescendoDurationLayout.setVisibility(VISIBLE);

        mBinding.crescendoDurationValue.setText(Utils.formatCrescendoDurationText(requireContext(), crescendoDuration));

        mBinding.crescendoDurationLayout.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_crescendo_duration, R.string.label_deskclock);

            final VolumeCrescendoDurationDialogFragment fragment =
                VolumeCrescendoDurationDialogFragment.newInstance(mAlarm.crescendoDuration);

            VolumeCrescendoDurationDialogFragment.show(getChildFragmentManager(), fragment);
        });
    }

    private void bindAlarmVolume() {
        if (!SettingsDAO.isPerAlarmVolumeEnabled(mPrefs)) {
            mBinding.alarmVolumeLayout.setVisibility(GONE);
            return;
        }

        final AudioManager audioManager = requireContext().getApplicationContext().getSystemService(AudioManager.class);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        final int currentVolume = Math.min(mAlarm.alarmVolume, maxVolume);

        int volumePercent = (int) (((float) currentVolume / maxVolume) * 100);
        String formatted = String.format(Locale.getDefault(), "%d%%", volumePercent);
        mBinding.alarmVolumeValue.setText(formatted);

        Drawable icon = AppCompatResources.getDrawable(requireContext(), volumePercent < 50
            ? R.drawable.ic_volume_down
            : R.drawable.ic_volume_up);

        if (icon != null) {
            mBinding.alarmVolumeTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        mBinding.alarmVolumeLayout.setVisibility(VISIBLE);

        View.OnClickListener openVolumeFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_alarm_volume, R.string.label_deskclock);

            final AlarmVolumeDialogFragment fragment = AlarmVolumeDialogFragment.newInstance(mAlarm.alarmVolume, mAlarm.alert);
            AlarmVolumeDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.alarmVolumeLayout.setOnClickListener(openVolumeFragment);
    }

    private void bindSpace() {
        if (mBinding.autoSilenceDurationLayout.getVisibility() == GONE
            && mBinding.snoozeDurationLayout.getVisibility() == GONE
            && mBinding.missedAlarmRepeatLimitLayout.getVisibility() == GONE
            && mBinding.mathHardnessLevelLayout.getVisibility() == GONE
            && mBinding.crescendoDurationLayout.getVisibility() == GONE
            && mBinding.alarmVolumeLayout.getVisibility() == GONE) {
            mBinding.space.setVisibility(GONE);
            return;
        }

        mBinding.space.setVisibility(VISIBLE);
    }

    private void bindAlarmBackgroundImage() {
        if (!SettingsDAO.isPerAlarmBackgroundImageEnable(mPrefs)) {
            mBinding.alarmBackgroundImageLayout.setVisibility(GONE);
            return;
        }

        if (TextUtils.isEmpty(mAlarm.backgroundImage)) {
            mBinding.alarmBackgroundImageButton.setVisibility(GONE);
        } else {
            mBinding.alarmBackgroundImageButton.setVisibility(VISIBLE);
        }

        mBinding.alarmBackgroundImageLayout.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_background_image, R.string.label_deskclock);
            FileUtils.selectFile(mImagePickerLauncher, false);
        });

        mBinding.alarmBackgroundImageButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            final Context appContext = requireContext().getApplicationContext();
            final int style = mAccentStyle;
            final Typeface font = mGeneralTypeface;

            FileUtils.deleteCustomFile(appContext, style, font, mAlarm.backgroundImage, false);
            mAlarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
            bindAlarmBackgroundImage();
            bindBlurIntensity();
            updateFourthGroup();
        });
    }

    private void bindBlurIntensity() {
        final boolean hasGlobalImage = !TextUtils.isEmpty(SettingsDAO.getAlarmBackgroundImage(mPrefs));
        final boolean hasSpecificImage = !TextUtils.isEmpty(mAlarm.backgroundImage);

        if (SdkUtils.isBeforeAndroid12()
            || !SettingsDAO.isPerAlarmBackgroundImageEnable(mPrefs)
            || (!hasGlobalImage && !hasSpecificImage)) {
            mBinding.alarmBlurIntensityLayout.setVisibility(GONE);
            return;
        }

        int blurIntensity = mAlarm.blurIntensity;

        if (blurIntensity == DEFAULT_BLUR_INTENSITY) {
            mBinding.alarmBlurIntensityValue.setText(R.string.label_none);
        } else {
            mBinding.alarmBlurIntensityValue.setText(String.valueOf(blurIntensity));
        }

        Drawable icon = AppCompatResources.getDrawable(requireContext(), blurIntensity == DEFAULT_BLUR_INTENSITY
            ? R.drawable.ic_blur_off
            : blurIntensity < 50
              ? R.drawable.ic_blur_decrease
              : R.drawable.ic_blur_increase
        );

        if (icon != null) {
            mBinding.alarmBlurIntensityTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        mBinding.alarmBlurIntensityLayout.setVisibility(VISIBLE);

        View.OnClickListener openBlurIntensityFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_blur_intensity, R.string.label_deskclock);

            final BlurIntensityDialogFragment fragment = BlurIntensityDialogFragment.newInstance(mAlarm.blurIntensity);
            BlurIntensityDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.alarmBlurIntensityLayout.setOnClickListener(openBlurIntensityFragment);
    }

    private void bindDeleteButton() {
        mBinding.deleteButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            mIsDeleted = true;
            Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncDeleteAlarm(mAlarm);
            dismiss();
        });
    }

    private void bindDuplicateButton() {
        mBinding.duplicateButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            Events.sendAlarmEvent(R.string.action_duplicate, R.string.label_deskclock);

            Alarm duplicatedAlarm = new Alarm(mAlarm);
            duplicatedAlarm.id = Alarm.INVALID_ID;
            duplicatedAlarm.instanceState = AlarmInstance.SILENT_STATE;
            final AlarmUpdateHandler localUpdateHandler = mAlarmUpdateHandler;

            if (!TextUtils.isEmpty(duplicatedAlarm.backgroundImage) &&
                duplicatedAlarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {

                final Context appContext = requireContext().getApplicationContext();

                AppExecutors.getDiskIO().execute(() -> {
                    File sourceFile = new File(duplicatedAlarm.backgroundImage);

                    if (sourceFile.exists()) {
                        String safeTitle = FileUtils.toSafeFileName(
                            FILE_SPECIFIC_ALARM_BACKGROUND + "_dup_" + System.currentTimeMillis()
                        );
                        Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, Uri.fromFile(sourceFile), safeTitle);

                        if (copiedUri != null) {
                            duplicatedAlarm.backgroundImage = copiedUri.getPath();
                        } else {
                            duplicatedAlarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
                        }
                    } else {
                        duplicatedAlarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
                    }

                    if (localUpdateHandler != null) {
                        localUpdateHandler.asyncAddAlarm(duplicatedAlarm);
                    }
                });
            } else {
                if (localUpdateHandler != null) {
                    localUpdateHandler.asyncAddAlarm(duplicatedAlarm);
                }
            }

            dismiss();
        });
    }

    private void bindPreviewButton() {
        mBinding.previewButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            Intent previewIntent = new Intent(requireContext(), AlarmDisplayPreviewActivity.class);
            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_HOUR, mAlarm.hour);
            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_MINUTE, mAlarm.minutes);
            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_LABEL, mAlarm.label);
            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_RINGTONE, mAlarm.alert);
            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_BACKGROUND_IMAGE, mAlarm.backgroundImage);
            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_BLUR_INTENSITY, mAlarm.blurIntensity);

            if (RingtoneUtils.RINGTONE_SILENT.equals(mAlarm.alert)) {
                previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_RINGTONE, "");
            } else {
                previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_RINGTONE, mAlarm.alert.toString());
            }

            ThemeUtils.startActivityWithTransition(requireContext(), previewIntent, mIsFadeTransition);
        });
    }

    private void bindSaveButton() {
        mBinding.saveButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            Events.sendAlarmEvent(R.string.action_save, R.string.label_deskclock);

            dismiss();
        });
    }

    // ********************
    // ** HELPER METHODS **
    // ********************

    private void setupFragmentResultListeners() {
        FragmentManager childFragmentManager = getChildFragmentManager();

        childFragmentManager.setFragmentResultListener(MaterialTimePickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                int h = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                applyTime(h, m, false);
            });

        childFragmentManager.setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                int h = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                applyTime(h, m, false);
            });

        childFragmentManager.setFragmentResultListener(AlarmDelayPickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                int h = bundle.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_MINUTES);
                applyDelay(h, m);
            });

        childFragmentManager.setFragmentResultListener(SpinnerDatePickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                int year = bundle.getInt(SpinnerDatePickerDialogFragment.BUNDLE_KEY_YEAR);
                int month = bundle.getInt(SpinnerDatePickerDialogFragment.BUNDLE_KEY_MONTH);
                int day = bundle.getInt(SpinnerDatePickerDialogFragment.BUNDLE_KEY_DAY);

                applyDate(year, month, day);
            });

        childFragmentManager.setFragmentResultListener(LabelDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.label = bundle.getString(LabelDialogFragment.RESULT_LABEL);
                mAlarm.syncByLabel = bundle.getBoolean(LabelDialogFragment.RESULT_SYNC, false);
                bindLabel();
            });

        childFragmentManager.setFragmentResultListener(VibrationPatternDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                String selectedPattern = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);
                if (selectedPattern != null) {
                    mAlarm.vibrationPattern = selectedPattern;
                    bindVibrationPattern();
                }
            });

        childFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.autoSilenceDuration = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);
                bindAutoSilenceValue();
                bindMissedAlarmRepeatLimit();
                updateThirdGroup();
            });

        childFragmentManager.setFragmentResultListener(AlarmSnoozeDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.snoozeDuration = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);
                bindSnoozeDurationValue();
                bindMissedAlarmRepeatLimit();
                updateThirdGroup();
            });

        childFragmentManager.setFragmentResultListener(AlarmMissedRepeatLimitDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.missedAlarmRepeatLimit = bundle.getInt(AlarmMissedRepeatLimitDialogFragment.RESULT_MISSED_REPEAT_LIMIT);
                bindMissedAlarmRepeatLimit();
            });

        childFragmentManager.setFragmentResultListener(AlarmMathHardnessLevelDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.mathHardnessLevel = bundle.getString(AlarmMathHardnessLevelDialogFragment.RESULT_MATH_HARDNESS_LEVEL);
                bindAlarmHardnessLevel();
                bindAutoSilenceValue();
            });

        childFragmentManager.setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.crescendoDuration = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);
                bindCrescendoDuration();
            });

        childFragmentManager.setFragmentResultListener(AlarmVolumeDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.alarmVolume = bundle.getInt(AlarmVolumeDialogFragment.RESULT_VOLUME_VALUE);
                bindAlarmVolume();
            });

        childFragmentManager.setFragmentResultListener(BlurIntensityDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.blurIntensity = bundle.getInt(BlurIntensityDialogFragment.RESULT_BLUR_INTENSITY_VALUE);
                bindBlurIntensity();
            });
    }

    /**
     * Restores the positive button click listener for the Material time picker.
     *
     * <p>This ensures that the time selection callback is not lost and remains
     * functional after a configuration change, such as a screen rotation.</p>
     */
    private void restoreMaterialTimePickerListener() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(TAG);

        if (fragment instanceof MaterialTimePicker materialTimePicker) {
            materialTimePicker.clearOnPositiveButtonClickListeners();

            materialTimePicker.addOnPositiveButtonClickListener(dialog -> {
                Bundle result = new Bundle();
                result.putInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS, materialTimePicker.getHour());
                result.putInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES, materialTimePicker.getMinute());

                getChildFragmentManager().setFragmentResult(MaterialTimePickerDialogFragment.REQUEST_KEY, result);
            });
        }
    }

    /**
     * Restores the positive button click listener for the single date Material picker.
     *
     * <p>This prevents the dialog's confirmation button from becoming unresponsive
     * if the device is rotated while the picker is open.</p>
     */
    private void restoreMaterialDatePickerListener() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(DatePickerDialogFragment.TAG_DATE_PICKER);

        if (fragment instanceof MaterialDatePicker) {
            @SuppressWarnings("unchecked")
            MaterialDatePicker<Long> materialDatePicker = (MaterialDatePicker<Long>) fragment;

            materialDatePicker.clearOnPositiveButtonClickListeners();

            materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.setTimeInMillis(selection);

                applyDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                );
            });
        }
    }

    /**
     * Restores the positive button click listener for the Material date range picker.
     *
     * <p>This guarantees that the selected start and end dates are properly captured
     * and processed, even if a configuration change occurs while the dialog is visible.</p>
     */
    private void restoreMaterialDateRangePickerListener() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(DatePickerDialogFragment.TAG_DATE_RANGE_PICKER);

        if (fragment instanceof MaterialDatePicker) {
            @SuppressWarnings("unchecked")
            MaterialDatePicker<Pair<Long, Long>> materialDatePicker = (MaterialDatePicker<Pair<Long, Long>>) fragment;

            materialDatePicker.clearOnPositiveButtonClickListeners();

            materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection.first != null && selection.second != null) {
                    mAlarm.pauseStartDate = selection.first;
                    mAlarm.pauseEndDate = selection.second;
                    bindPauseAlarm();
                }
            });
        }
    }

    private void applyDelay(int hoursToAdd, int minutesToAdd) {
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.add(Calendar.HOUR_OF_DAY, hoursToAdd);
        alarmTime.add(Calendar.MINUTE, minutesToAdd);

        applyTime(alarmTime.get(Calendar.HOUR_OF_DAY), alarmTime.get(Calendar.MINUTE), true);
    }

    private void applyTime(int hour, int minute, boolean isFromDelay) {
        mAlarm.hour = hour;
        mAlarm.minutes = minute;

        if (isFromDelay) {
            mAlarm.daysOfWeek = Weekdays.fromBits(0);
        }

        Calendar currentCalendar = Calendar.getInstance();

        // Necessary when an existing alarm has been created in the past, and it is not enabled.
        // Even if the date is not specified, it is saved in AlarmInstance; we need to make
        // sure that the date is not in the past when changing time, in which case we reset
        // to the current date (an alarm cannot be scheduled in the past).
        // This is due to the change in the code made with commit : 6ac23cf.
        // Fix https://github.com/BlackyHawky/Clock/issues/299
        boolean mustResetDate = mAlarm.isDateInThePast() || (isFromDelay && mAlarm.isSpecifiedDate());

        if (mustResetDate) {
            mAlarm.year = currentCalendar.get(Calendar.YEAR);
            mAlarm.month = currentCalendar.get(Calendar.MONTH);
            mAlarm.day = currentCalendar.get(Calendar.DAY_OF_MONTH);

            bindSelectedDate();
        }

        if (isFromDelay) {
            bindDaysOfWeekButtons();
            bindDeleteAlarmAfterUse();
        }

        bindClock();
    }

    private void applyDate(int year, int month, int day) {
        if (mAlarm.daysOfWeek.isRepeating()) {
            mAlarm.daysOfWeek = Weekdays.NONE;
        }

        if (mAlarm.isPauseSet()) {
            mAlarm.pauseStartDate = 0;
            mAlarm.pauseEndDate = 0;
        }

        mAlarm.year = year;
        mAlarm.month = month;
        mAlarm.day = day;

        bindSelectedDate();
        bindDaysOfWeekButtons();
        bindPauseAlarm();
        bindDeleteAlarmAfterUse();
    }

    private void updateDaysOfWeekButtonVisuals(@NonNull MaterialButton dayButton, boolean isSelected) {
        final int backgroundColor = isSelected
            ? MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiary, Color.BLACK)
            : Color.TRANSPARENT;

        final ColorStateList strokeColor = ColorStateList.valueOf(MaterialColors.getColor(requireContext(), isSelected
            ? com.google.android.material.R.attr.colorTertiary
            : com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK)
        );

        final int textColor = MaterialColors.getColor(requireContext(), isSelected
            ? com.google.android.material.R.attr.colorSurfaceContainerLowest
            : com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK);

        dayButton.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        dayButton.setStrokeColor(strokeColor);
        dayButton.setTextColor(textColor);
    }

    private void clearSelectedDate(@StringRes int text) {
        mBinding.cancelScheduledAlarm.setVisibility(GONE);
        mBinding.scheduleAlarm.setText(getString(text));
    }

    @NonNull
    private String formatSnoozeDurationText(int duration) {
        if (duration == ALARM_SNOOZE_DURATION_DISABLED) {
            return getString(R.string.snooze_duration_none);
        }

        int h = duration / 60;
        int m = duration % 60;

        if (h > 0 && m > 0) {
            String hoursString = getResources().getQuantityString(R.plurals.hours_short, h, h);
            String minutesString = getResources().getQuantityString(R.plurals.minutes_short, m, m);
            return String.format("%s %s", hoursString, minutesString);
        } else if (h > 0) {
            return getResources().getQuantityString(R.plurals.hours_short, h, h);
        } else {
            return getResources().getQuantityString(R.plurals.minutes_short, m, m);
        }
    }

    private void saveAlarmSettings() {
        if (mIsDeleted || mAlarm == null || mOriginalAlarm == null) {
            return;
        }

        boolean timeChanged = mAlarm.hasTimeChanged(mOriginalAlarm);
        boolean minorFieldsChanged = mAlarm.hasMinorFieldsChanged(mOriginalAlarm);
        boolean isNewAlarmCreated = mIsNewAlarm && mAlarm.enabled;

        if (!timeChanged && !minorFieldsChanged) {
            if (isNewAlarmCreated) {
                mAlarmUpdateHandler.asyncUpdateAlarm(mAlarm, true, false);
            }
            return;
        }

        boolean updateWidgets = !Objects.equals(mAlarm.label, mOriginalAlarm.label);
        boolean minorUpdate = !timeChanged;
        boolean popToast = timeChanged || isNewAlarmCreated;

        if (timeChanged) {
            mAlarm.enabled = true;
        }

        AlarmVisualCache.invalidate(mAlarm.id);

        mAlarmUpdateHandler.asyncUpdateAlarm(mAlarm, popToast, minorUpdate);

        if (isAdded()) {
            Bundle result = new Bundle();
            result.putLong(SCROLL_TO_ALARM_ID, mAlarm.id);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }

        if (updateWidgets) {
            Context appContext = requireContext().getApplicationContext();

            if (WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(mPrefs) && WidgetDAO.isNextAlarmTitleDisplayedOnDigitalWidget(mPrefs)) {
                WidgetUtils.updateWidget(appContext, DigitalAppWidgetProvider.class);
            }

            WidgetUtils.updateWidget(appContext, NextAlarmAppWidgetProvider.class);
        }
    }

    private void updateSecondGroup() {
        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mDisplayMetrics,
            mCardStyleConfig.isBackgroundDisplayed(),
            mCardStyleConfig.isBorderDisplayed(),
            mCardStyleConfig.isAmoledDarkMode(),
            mBinding.vibrateOnOff,
            mBinding.vibrationPatternLayout,
            mBinding.flashOnOff,
            mBinding.deleteAlarmAfterUseLayout
        );
    }

    private void updateThirdGroup() {
        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mDisplayMetrics,
            mCardStyleConfig.isBackgroundDisplayed(),
            mCardStyleConfig.isBorderDisplayed(),
            mCardStyleConfig.isAmoledDarkMode(),
            mBinding.autoSilenceDurationLayout,
            mBinding.snoozeDurationLayout,
            mBinding.missedAlarmRepeatLimitLayout,
            mBinding.mathHardnessLevelLayout,
            mBinding.crescendoDurationLayout,
            mBinding.alarmVolumeLayout
        );
    }

    private void updateFourthGroup() {
        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mDisplayMetrics,
            mCardStyleConfig.isBackgroundDisplayed(),
            mCardStyleConfig.isBorderDisplayed(),
            mCardStyleConfig.isAmoledDarkMode(),
            mBinding.alarmBackgroundImageLayout,
            mBinding.alarmBlurIntensityLayout
        );
    }

    private void updateAllGroupBackgrounds() {
        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mDisplayMetrics,
            mCardStyleConfig.isBackgroundDisplayed(),
            mCardStyleConfig.isBorderDisplayed(),
            mCardStyleConfig.isAmoledDarkMode(),
            mBinding.scheduleAlarmLayout,
            mBinding.pauseAlarmLayout
        );

        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mDisplayMetrics,
            mCardStyleConfig.isBackgroundDisplayed(),
            mCardStyleConfig.isBorderDisplayed(),
            mCardStyleConfig.isAmoledDarkMode(),
            mBinding.editLabel,
            mBinding.chooseRingtone
        );

        updateSecondGroup();

        updateThirdGroup();

        updateFourthGroup();
    }

    private void showPauseAlarmNoteDialog() {
        mShowPauseAlarmNoteDialog = true;

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_help),
            getString(R.string.info),
            getString(R.string.pause_alarm_info_message),
            null,
            getString(android.R.string.ok),
            null,
            null,
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mShowPauseAlarmNoteDialog = false)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void showDeleteAlarmAfterUseNoteDialog() {
        mShowDeleteAlarmAfterUseNoteDialog = true;

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_help),
            getString(R.string.info),
            getString(R.string.delete_occasional_alarm_after_use_info_message),
            null,
            getString(android.R.string.ok),
            null,
            null,
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mShowDeleteAlarmAfterUseNoteDialog = false)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void showAutoSilenceNoteDialog(@NonNull String silenceAfterDuration) {
        mShowAutoSilenceNoteDialog = true;
        mAutoSilenceDuration = silenceAfterDuration;

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_help),
            getString(R.string.info),
            getString(R.string.auto_silence_info_message, silenceAfterDuration),
            null,
            getString(android.R.string.ok),
            null,
            null,
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> {
                mShowAutoSilenceNoteDialog = false;
                mAutoSilenceDuration = null;
            })),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

}

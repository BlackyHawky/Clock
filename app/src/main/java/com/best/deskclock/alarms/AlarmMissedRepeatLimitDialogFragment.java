// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

/**
 * DialogFragment to set a new repeat limit for missed alarms.
 */
public class AlarmMissedRepeatLimitDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of AlarmMissedRepeatLimitDialogFragment in the fragment manager.
     */
    private static final String TAG = "alarm_missed_repeat_count_dialog";

    private static final String ARG_SELECTED_COUNT = "selected_count";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_TAG = "arg_tag";

    private RadioGroup mRadioGroup;
    private Alarm mAlarm;
    private String mTag;

    /**
     * Creates a new instance of {@link AlarmMissedRepeatLimitDialogFragment} for use
     * in the expanded alarm view, where the snooze duration is configured for a specific alarm.
     *
     * @param alarm             The alarm instance being edited.
     * @param missedRepeatLimit The number of times a missed alarm can be repeated.
     * @param tag               A tag identifying the fragment in the fragment manager.
     */
    public static AlarmMissedRepeatLimitDialogFragment newInstance(Alarm alarm, int missedRepeatLimit,
                                                                   String tag) {
        final Bundle args = new Bundle();

        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);
        args.putInt(ARG_SELECTED_COUNT, missedRepeatLimit);

        final AlarmMissedRepeatLimitDialogFragment fragment = new AlarmMissedRepeatLimitDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Replaces any existing AlarmMissedRepeatLimitDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, AlarmMissedRepeatLimitDialogFragment fragment) {
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Finish any outstanding fragment work.
        manager.executePendingTransactions();

        final FragmentTransaction tx = manager.beginTransaction();

        // Remove existing instance of this DialogFragment if necessary.
        final Fragment existing = manager.findFragmentByTag(TAG);
        if (existing != null) {
            tx.remove(existing);
        }
        tx.addToBackStack(null);

        fragment.show(tx, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        int selectedCount = getLimitFromSelectedRadioButton();
        outState.putInt(ARG_SELECTED_COUNT, selectedCount);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();
        mAlarm = SdkUtils.isAtLeastAndroid13()
                ? args.getParcelable(ARG_ALARM, Alarm.class)
                : args.getParcelable(ARG_ALARM);
        mTag = args.getString(ARG_TAG);

        int selectedCount = requireArguments().getInt(ARG_SELECTED_COUNT, 0);
        if (savedInstanceState != null) {
            selectedCount = savedInstanceState.getInt(ARG_SELECTED_COUNT, selectedCount);
        }

        @SuppressLint("InflateParams")
        View dialogView = getLayoutInflater().inflate(R.layout.alarm_missed_repeat_limit_dialog, null);

        mRadioGroup = dialogView.findViewById(R.id.repeat_limit_radio_group);

        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
            View child = mRadioGroup.getChildAt(i);
            if (child instanceof RadioButton radioButton) {
                radioButton.setTypeface(typeface);
            }
        }

        selectRadioButtonForLimit(selectedCount);

        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            setMissedAlarmRepeatLimit();
            dismiss();
        });

        return CustomDialog.create(
                requireContext(),
                null,
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_repeat),
                getString(R.string.missed_alarm_repeat_limit_title),
                null,
                dialogView,
                null,
                null,
                getString(android.R.string.cancel),
                null,
                null,
                null,
                null,
                CustomDialog.SoftInputMode.NONE
        );
    }

    /**
     * Set the repeat limit.
     */
    private void setMissedAlarmRepeatLimit() {
        int missedAlarmRepeatLimit = getLimitFromSelectedRadioButton();

        if (mAlarm != null) {
            ((MissedAlarmRepeatLimitDialogHandler) requireActivity())
                    .onMissedAlarmRepeatLimitSet(mAlarm, missedAlarmRepeatLimit, mTag);
        }
    }

    private int getLimitFromSelectedRadioButton() {
        int id = mRadioGroup.getCheckedRadioButtonId();
        if (id == R.id.rb_1_time) {
            return 1;
        }
        else if (id == R.id.rb_3_times) {
            return 3;
        }
        else if (id == R.id.rb_5_times) {
            return 5;
        }
        else if (id == R.id.rb_10_times) {
            return 10;
        } else {
            // Never
            return -1;
        }
    }

    private void selectRadioButtonForLimit(int limit) {
        int id = switch (limit) {
            case 1 -> R.id.rb_1_time;
            case 3 -> R.id.rb_3_times;
            case 5 -> R.id.rb_5_times;
            case 10 -> R.id.rb_10_times;
            default -> R.id.rb_never;
        };

        mRadioGroup.check(id);
    }

    public interface MissedAlarmRepeatLimitDialogHandler {
        void onMissedAlarmRepeatLimitSet(Alarm alarm, int missedAlarmRepeatLimit, String tag);
    }

}

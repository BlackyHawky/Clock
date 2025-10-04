// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_PATTERN;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * DialogFragment to set a new vibration pattern for alarms.
 */
public class VibrationPatternDialogFragment extends DialogFragment {

    private static final String TAG = "vibration_pattern_dialog";

    private static final String ARG_PREF_KEY = "arg_pref_key";
    public static final String REQUEST_KEY = "request_key";
    public static final String RESULT_PATTERN_KEY = "result_pattern_key";
    public static final String RESULT_PREF_KEY = "result_pref_key";
    private static final String VIBRATION_PATTERN = "vibration_pattern";

    private String mSelectedPatternKey;
    private Vibrator mVibrator;

    /**
     * Creates a new instance of {@link VibrationPatternDialogFragment} for use
     * in the settings screen, allowing the user to choose a vibration pattern.
     *
     * @param key               The shared preference key used to identify the setting.
     * @param currentPatternKey The currently selected vibration pattern key,
     *                          which will be preselected in the dialog.
     * @return A configured instance of {@link VibrationPatternDialogFragment}.
     */

    public static VibrationPatternDialogFragment newInstance(String key, String currentPatternKey) {
        Bundle args = new Bundle();
        args.putString(ARG_PREF_KEY, key);
        args.putString(VIBRATION_PATTERN, currentPatternKey);

        VibrationPatternDialogFragment frag = new VibrationPatternDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Replaces any existing VibrationPatternDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, VibrationPatternDialogFragment fragment) {
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
        outState.putString(VIBRATION_PATTERN, mSelectedPatternKey);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        mSelectedPatternKey = requireArguments().getString(VIBRATION_PATTERN, DEFAULT_VIBRATION_PATTERN);
        if (savedInstanceState != null) {
            mSelectedPatternKey = savedInstanceState.getString(VIBRATION_PATTERN, mSelectedPatternKey);
        }

        View view = getLayoutInflater().inflate(R.layout.vibration_pattern_dialog, null);
        RadioGroup radioGroup = view.findViewById(R.id.vibration_options);
        RadioButton rbDefault = view.findViewById(R.id.vibration_pattern_default);
        RadioButton rbSoft = view.findViewById(R.id.vibration_pattern_soft);
        RadioButton rbStrong = view.findViewById(R.id.vibration_pattern_strong);
        RadioButton rbHeartbeat = view.findViewById(R.id.vibration_pattern_heartbeat);
        RadioButton rbEscalating = view.findViewById(R.id.vibration_pattern_escalating);
        RadioButton rbTickTock = view.findViewById(R.id.vibration_pattern_tick_tock);

        RadioButton[] buttons =
                {rbDefault, rbSoft, rbStrong, rbHeartbeat, rbEscalating, rbTickTock};

        String[] values = context.getResources().getStringArray(R.array.vibration_pattern_values);

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setTag(values[i]);
            if (values[i].equals(mSelectedPatternKey)) {
                buttons[i].setChecked(true);
            }
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = group.findViewById(checkedId);
            if (rb != null) {
                mSelectedPatternKey = (String) rb.getTag();
                mVibrator.cancel();
                ThemeUtils.cancelRadioButtonDrawableAnimations(group);
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.vibration_pattern_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mVibrator.cancel();
                    savePattern(mSelectedPatternKey);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> mVibrator.cancel())
                .setNeutralButton(R.string.preview_title, null);

        AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(dialogInterface -> {
            Button neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(v -> {
                if (mSelectedPatternKey != null) {
                    mVibrator.cancel();

                    long[] pattern = Utils.getVibrationPatternForKey(mSelectedPatternKey);

                    if (SdkUtils.isAtLeastAndroid8()) {
                        VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
                        mVibrator.vibrate(effect);
                    } else {
                        mVibrator.vibrate(pattern, 0);
                    }
                }
            });
        });

        return alertDialog;
    }

    @Override
    public void onStop() {
        super.onStop();

        mVibrator.cancel();
    }

    /**
     * Saves the selected vibration pattern by posting a fragment result.
     *
     * @param patternKey The key representing the selected vibration pattern.
     */
    private void savePattern(String patternKey) {
        Bundle result = new Bundle();
        result.putString(RESULT_PATTERN_KEY, patternKey);
        result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

}


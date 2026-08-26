// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_MATH_HARDNESS_LEVEL;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmMathHardnessLevelDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

public class AlarmMathHardnessLevelDialogFragment extends DialogFragment {

    private static final String TAG = "math_hardness_dialog";

    private static final String ARG_PREF_KEY = "arg_pref_key";
    public static final String REQUEST_KEY = "math_hardness_level_request_key";
    public static final String RESULT_MATH_HARDNESS_LEVEL = "result_math_hardness_level";
    public static final String RESULT_PREF_KEY = "result_pref_key";
    private static final String MATH_HARDNESS_LEVEL = "math_hardness_level";

    AlarmMathHardnessLevelDialogBinding mBinding;

    private String mPrefKey;
    private String mSelectedMathHardnessLevelKey;

    /**
     * Creates a new instance of {@link AlarmMathHardnessLevelDialogFragment} for use in the settings screen,
     * allowing the user to choose a math hardness.
     *
     * @param key               The shared preference key used to identify the setting.
     * @param mathHardnessLevel The currently selected math hardness key, which will be preselected in the dialog.
     * @return A configured instance of {@link AlarmMathHardnessLevelDialogFragment}.
     */
    @NonNull
    public static AlarmMathHardnessLevelDialogFragment newInstance(@NonNull String key, @NonNull String mathHardnessLevel) {
        Bundle args = new Bundle();
        args.putString(ARG_PREF_KEY, key);
        args.putString(MATH_HARDNESS_LEVEL, mathHardnessLevel);

        AlarmMathHardnessLevelDialogFragment frag = new AlarmMathHardnessLevelDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link AlarmMathHardnessLevelDialogFragment} for use in the alarm editing panel,
     * where the math hardness is configured for a specific alarm.
     *
     * @param mathHardnessLevel The math hardness.
     */
    @NonNull
    public static AlarmMathHardnessLevelDialogFragment newInstance(@NonNull String mathHardnessLevel) {
        final Bundle args = new Bundle();

        args.putString(MATH_HARDNESS_LEVEL, mathHardnessLevel);

        final AlarmMathHardnessLevelDialogFragment fragment = new AlarmMathHardnessLevelDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link AlarmMathHardnessLevelDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull AlarmMathHardnessLevelDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MATH_HARDNESS_LEVEL, mSelectedMathHardnessLevelKey);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();

        mPrefKey = args.getString(ARG_PREF_KEY, null);

        mSelectedMathHardnessLevelKey = args.getString(MATH_HARDNESS_LEVEL, DEFAULT_MATH_HARDNESS_LEVEL);
        if (savedInstanceState != null) {
            mSelectedMathHardnessLevelKey = savedInstanceState.getString(MATH_HARDNESS_LEVEL, mSelectedMathHardnessLevelKey);
        }

        mBinding = AlarmMathHardnessLevelDialogBinding.inflate(getLayoutInflater());

        RadioButton[] buttons = {
            mBinding.mathHardnessLevelOff, mBinding.mathHardnessLevelEasy, mBinding.mathHardnessLevelNormal, mBinding.mathHardnessLevelHard
        };

        String[] values = context.getResources().getStringArray(R.array.math_hardness_level_values);

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setTag(values[i]);
            buttons[i].setTypeface(typeface);

            if (values[i].equals(mSelectedMathHardnessLevelKey)) {
                buttons[i].setChecked(true);
            }
        }

        mBinding.mathHardnessLevelRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = group.findViewById(checkedId);
            if (rb != null) {
                mSelectedMathHardnessLevelKey = (String) rb.getTag();
                saveMathHardnessLevel(mSelectedMathHardnessLevelKey);
                dismiss();
            }
        });

        return CustomDialog.create(
            context,
            null,
            mPrefKey != null ? null : AppCompatResources.getDrawable(requireContext(), R.drawable.ic_calculate),
            getString(R.string.math_hardness_level_title),
            null,
            mBinding.getRoot(),
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

    @Override
    public void onDestroyView() {
        mBinding = null;

        super.onDestroyView();
    }

    /**
     * Saves the selected math hardness level by posting a fragment result.
     *
     * @param mathHardnessLevel The selected math mission hardness.
     */
    private void saveMathHardnessLevel(@NonNull String mathHardnessLevel) {
        Bundle result = new Bundle();
        result.putString(RESULT_MATH_HARDNESS_LEVEL, mathHardnessLevel);

        if (mPrefKey != null) {
            result.putString(RESULT_PREF_KEY, mPrefKey);
        }

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

}

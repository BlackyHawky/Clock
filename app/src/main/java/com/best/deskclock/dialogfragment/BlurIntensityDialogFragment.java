// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_BLUR_INTENSITY;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmBlurIntensityDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

public class BlurIntensityDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of BlurIntensityDialogFragment in the fragment manager.
     */
    public static final String TAG = "set_blur_intensity_dialog";

    public static final String REQUEST_KEY = "blur_intensity_request_key";
    public static final String RESULT_BLUR_INTENSITY_VALUE = "result_blur_intensity_value";
    private static final String ARG_ALARM_BLUR_INTENSITY_VALUE = "arg_alarm_blur_intensity_value";

    private AlarmBlurIntensityDialogBinding mBinding;
    private TextView mDialogTitle;
    private Button mDefaultButton;

    private int mCurrentIconResId = -1;

    /**
     * Creates a new instance of {@link BlurIntensityDialogFragment} for use
     * in the alarm editing panel, where the blur intensity value is configured for a specific alarm.
     *
     * @param blurIntensityValue The blur intensity value.
     */
    @NonNull
    public static BlurIntensityDialogFragment newInstance(int blurIntensityValue) {
        final Bundle args = new Bundle();
        args.putInt(ARG_ALARM_BLUR_INTENSITY_VALUE, blurIntensityValue);

        final BlurIntensityDialogFragment fragment = new BlurIntensityDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link BlurIntensityDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull BlurIntensityDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ARG_ALARM_BLUR_INTENSITY_VALUE, (int) mBinding.alarmBlurIntensitySlider.getValue());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();
        int blurIntensityValue = args.getInt(ARG_ALARM_BLUR_INTENSITY_VALUE, DEFAULT_BLUR_INTENSITY);

        if (savedInstanceState != null) {
            blurIntensityValue = savedInstanceState.getInt(ARG_ALARM_BLUR_INTENSITY_VALUE, blurIntensityValue);
        }

        mBinding = AlarmBlurIntensityDialogBinding.inflate(getLayoutInflater());

        mBinding.alarmBlurIntensityValue.setTypeface(typeface);

        mBinding.alarmBlurIntensitySlider.setValueTo(100f);
        mBinding.alarmBlurIntensitySlider.setValueFrom(0f);
        mBinding.alarmBlurIntensitySlider.setStepSize(1f);
        mBinding.alarmBlurIntensitySlider.setValue((float) blurIntensityValue);

        updateBlurIntensityText(blurIntensityValue);
        updateBlurIntensityButtonStates(blurIntensityValue);

        mBinding.sliderMinusIcon.setOnClickListener(v -> {
            float currentValue = mBinding.alarmBlurIntensitySlider.getValue();
            float minValue = mBinding.alarmBlurIntensitySlider.getValueFrom();
            float newValue = Math.max(minValue, currentValue - 5f);

            mBinding.alarmBlurIntensitySlider.setValue(newValue);
        });

        mBinding.sliderPlusIcon.setOnClickListener(v -> {
            float currentValue = mBinding.alarmBlurIntensitySlider.getValue();
            float maxValue = mBinding.alarmBlurIntensitySlider.getValueTo();
            float newValue = Math.min(maxValue, currentValue + 5f);

            mBinding.alarmBlurIntensitySlider.setValue(newValue);
        });

        mBinding.alarmBlurIntensitySlider.addOnChangeListener((slider, progress, fromUser) -> {
            updateDialogIcon((int) progress);
            updateBlurIntensityText((int) progress);
            updateBlurIntensityButtonStates((int) progress);
            updateDefaultButtonState((int) progress);
        });

        int initialIconResId = blurIntensityValue < 50
            ? R.drawable.ic_blur_decrease
            : R.drawable.ic_blur_increase;

        mCurrentIconResId = initialIconResId;

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), initialIconResId),
            getString(R.string.blur_intensity_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> {
                int blurIntensity = (int) mBinding.alarmBlurIntensitySlider.getValue();
                setBlurIntensityValue(blurIntensity);
            },
            getString(android.R.string.cancel),
            null,
            getString(R.string.label_default),
            (d, w) -> setBlurIntensityValue(DEFAULT_BLUR_INTENSITY),
            alertDialog -> {
                mDialogTitle = alertDialog.findViewById(R.id.dialog_title);
                mDefaultButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                int blurIntensity = (int) mBinding.alarmBlurIntensitySlider.getValue();

                updateDefaultButtonState(blurIntensity);
            },
            CustomDialog.SoftInputMode.NONE
        );

    }

    @Override
    public void onDestroyView() {
        mBinding.alarmBlurIntensitySlider.clearOnChangeListeners();
        mBinding.alarmBlurIntensitySlider.clearOnSliderTouchListeners();

        mBinding.sliderMinusIcon.setOnClickListener(null);
        mBinding.sliderPlusIcon.setOnClickListener(null);

        mBinding = null;

        mDialogTitle = null;
        mDefaultButton = null;

        mCurrentIconResId = -1;

        super.onDestroyView();
    }

    /**
     * Updates the text view displaying the current blur intensity value.
     *
     * @param currentBlurIntensity The current blur intensity value.
     */
    private void updateBlurIntensityText(int currentBlurIntensity) {
        if (currentBlurIntensity == DEFAULT_BLUR_INTENSITY) {
            mBinding.alarmBlurIntensityValue.setText(R.string.label_none);
        } else {
            mBinding.alarmBlurIntensityValue.setText(String.valueOf(currentBlurIntensity));
        }
    }

    /**
     * Enables or disables the plus/minus buttons based on the current slider progress.
     *
     * @param progress The current progress of the slider.
     */
    private void updateBlurIntensityButtonStates(int progress) {
        ThemeUtils.updateSliderButtonEnabledState(requireContext(), mBinding.sliderMinusIcon, progress > DEFAULT_BLUR_INTENSITY);
        ThemeUtils.updateSliderButtonEnabledState(requireContext(), mBinding.sliderPlusIcon, progress < 100);
    }

    /**
     * Updates the dialog icon based on the set blur intensity.
     *
     * @param currentBlurIntensity The current blur intensity value.
     */
    private void updateDialogIcon(int currentBlurIntensity) {
        int targetIconResId = currentBlurIntensity < 50
            ? R.drawable.ic_blur_decrease
            : R.drawable.ic_blur_increase;

        if (targetIconResId != mCurrentIconResId) {
            mCurrentIconResId = targetIconResId;

            mDialogTitle.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(requireContext(), targetIconResId), null, null, null);
            mDialogTitle.setCompoundDrawablePadding((int) dpToPx(18, getResources().getDisplayMetrics()));
        }
    }

    /**
     * Updates the dialog "Default" button based on the set blur intensity.
     *
     * @param currentBlurIntensity The current blur intensity value.
     */
    private void updateDefaultButtonState(int currentBlurIntensity) {
        if (mDefaultButton != null) {
            mDefaultButton.setEnabled(currentBlurIntensity != DEFAULT_BLUR_INTENSITY);
        }
    }

    /**
     * Set the blur intensity.
     */
    private void setBlurIntensityValue(int blurIntensityValue) {
        Bundle result = new Bundle();
        result.putInt(RESULT_BLUR_INTENSITY_VALUE, blurIntensityValue);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

}

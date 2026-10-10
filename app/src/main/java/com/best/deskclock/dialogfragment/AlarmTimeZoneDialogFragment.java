// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.databinding.DialogListPreferenceCustomBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.radiobutton.MaterialRadioButton;

/**
 * DialogFragment to set the alarm time zone.
 */
public class AlarmTimeZoneDialogFragment extends DialogFragment  {

    public static final String TAG = "AlarmTimeZoneDialogFragment";
    public static final String REQUEST_KEY = "request_alarm_time_zone";
    public static final String RESULT_TIMEZONE = "result_time_zone";
    private static final String ARG_SELECTED_TIMEZONE = "arg_selected_timezone";

    private DialogListPreferenceCustomBinding mBinding;

    /**
     * @param timeZone L'ID du fuseau actuel (ex: "America/New_York"), ou "" / null pour l'heure locale.
     */
    @NonNull
    public static AlarmTimeZoneDialogFragment newInstance(@Nullable String timeZone) {
        final Bundle args = new Bundle();
        args.putString(ARG_SELECTED_TIMEZONE, timeZone == null ? "" : timeZone);

        final AlarmTimeZoneDialogFragment fragment = new AlarmTimeZoneDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link AlarmTimeZoneDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull AlarmTimeZoneDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_SELECTED_TIMEZONE, getSelectedTimeZone());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Récupération des préférences (Police, etc.)
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        boolean isFlagEnabled = SettingsDAO.isCityFlagEnabled(prefs);

        String selectedTimeZone = requireArguments().getString(ARG_SELECTED_TIMEZONE, "");
        if (savedInstanceState != null) {
            selectedTimeZone = savedInstanceState.getString(ARG_SELECTED_TIMEZONE, selectedTimeZone);
        }

        mBinding = DialogListPreferenceCustomBinding.inflate(getLayoutInflater());

        long currentTime = System.currentTimeMillis();
        TimeZones timeZones = SettingsDAO.getTimeZones(requireContext(), currentTime, isFlagEnabled);
        int paddingStart = (int) dpToPx(20, getResources().getDisplayMetrics());
        int minHeight = (int) dpToPx(48, getResources().getDisplayMetrics());

        populateRadioGroup(timeZones, selectedTimeZone, typeface, paddingStart, minHeight);

        mBinding.listOptions.setOnCheckedChangeListener((group, checkedId) -> saveAndDismiss(getSelectedTimeZone()));

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_globe_clock),
            getString(R.string.timezone_title),
            null,
            mBinding.getRoot(),
            null,
            null,
            getString(android.R.string.cancel),
            null,
            getString(R.string.label_default),
            (d, w) -> saveAndDismiss(""),
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    @Override
    public void onDestroyView() {
        mBinding = null;

        super.onDestroyView();
    }

    private void populateRadioGroup(TimeZones timeZones, String selectedTimeZone, Typeface typeface, int paddingStart, int minHeight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        MaterialRadioButton checkedButton = null;

        for (int i = 0; i < timeZones.timeZoneIds().length; i++) {
            String tag = timeZones.timeZoneIds()[i].toString();
            String name = timeZones.timeZoneNames()[i].toString();

            MaterialRadioButton radioButton = new MaterialRadioButton(requireContext());
            radioButton.setId(View.generateViewId());
            radioButton.setLayoutParams(params);
            radioButton.setText(name);
            radioButton.setTag(tag);
            radioButton.setTypeface(typeface);
            radioButton.setPadding(paddingStart, 0, 0, 0);
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            radioButton.setMinHeight(minHeight);

            mBinding.listOptions.addView(radioButton);

            if (TextUtils.equals(selectedTimeZone, tag)) {
                radioButton.setChecked(true);
                checkedButton = radioButton;
            }
        }

        if (checkedButton != null && mBinding.listOptions.getParent() instanceof NestedScrollView scrollView) {
            final View finalCheckedButton = checkedButton;
            scrollView.post(() -> {
                int scrollToY = finalCheckedButton.getTop() - (scrollView.getHeight() / 3);
                scrollView.scrollTo(0, Math.max(0, scrollToY));
            });
        }
    }

    private String getSelectedTimeZone() {
        if (mBinding == null) {
            return "";
        }

        int checkedId = mBinding.listOptions.getCheckedRadioButtonId();

        if (checkedId != -1) {
            RadioButton radioButton = mBinding.listOptions.findViewById(checkedId);
            if (radioButton != null && radioButton.getTag() != null) {
                return radioButton.getTag().toString();
            }
        }

        return "";
    }

    private void saveAndDismiss(String timeZone) {
        Bundle result = new Bundle();
        result.putString(RESULT_TIMEZONE, timeZone);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

}

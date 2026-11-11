// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_FEATURES_TO_DISPLAY;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.DialogMultiListPreferenceCustomBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A dialog fragment that displays the selectable entries of a {@link MultiSelectListPreference} using a custom Material-styled layout.
 *
 * <p>The dialog highlights the currently selected value and updates the associated preference when the user selects a new option.
 * The change listener of the preference is invoked before applying the new value.</p>
 */
public class CustomMultiSelectListPreferenceDialogFragment extends DialogFragment {

    private static final String TAG = "custom_multi_list_pref_dialog";

    private static final String ARG_PREF_KEY = "arg_pref_key";
    private static final String ARG_TITLE = "title";
    private static final String ARG_ENTRIES = "entries";
    private static final String ARG_ENTRY_VALUES = "entry_values";
    private static final String ARG_CURRENT_VALUES = "current_values";
    private static final String SAVE_STATE_VALUES = "save_state_values";

    private MultiSelectListPreference preference;

    private final Set<String> mNewValues = new HashSet<>();

    /**
     * Creates a new instance of {@link CustomMultiSelectListPreferenceDialogFragment} for use
     * in the settings screen, allowing the user to select a value from a list of options.
     *
     * <p>This method extracts all necessary information from the provided
     * {@link MultiSelectListPreference}, including its key, title, entries, entry values,
     * and currently selected value. These details are then passed to the dialog so it
     * can display the correct options and highlight the active selection.</p>
     *
     * @param pref The {@link MultiSelectListPreference} associated with this dialog.
     *             Its configuration and current value will be used to initialize the dialog.
     * @return A fully configured instance of {@link CustomMultiSelectListPreferenceDialogFragment} ready to be displayed.
     */
    @NonNull
    public static CustomMultiSelectListPreferenceDialogFragment newInstance(@NonNull MultiSelectListPreference pref) {
        Bundle args = new Bundle();
        args.putString(ARG_PREF_KEY, pref.getKey());
        args.putCharSequence(ARG_TITLE, pref.getTitle());
        args.putCharSequenceArray(ARG_ENTRIES, pref.getEntries());
        args.putCharSequenceArray(ARG_ENTRY_VALUES, pref.getEntryValues());
        args.putStringArray(ARG_CURRENT_VALUES, pref.getValues().toArray(new String[0]));

        CustomMultiSelectListPreferenceDialogFragment frag = new CustomMultiSelectListPreferenceDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Displays {@link CustomMultiSelectListPreferenceDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull CustomMultiSelectListPreferenceDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(SAVE_STATE_VALUES, new ArrayList<>(mNewValues));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        resolvePreferenceIfNeeded();

        Bundle args = requireArguments();
        String key = args.getString(ARG_PREF_KEY);
        boolean isAlarmFeaturesPref = KEY_ALARM_FEATURES_TO_DISPLAY.equals(key);
        CharSequence title = args.getCharSequence(ARG_TITLE);
        CharSequence[] entries = args.getCharSequenceArray(ARG_ENTRIES);
        CharSequence[] entryValues = args.getCharSequenceArray(ARG_ENTRY_VALUES);

        mNewValues.clear();

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_STATE_VALUES)) {
            // Restoring after screen rotation
            ArrayList<String> savedValues = savedInstanceState.getStringArrayList(SAVE_STATE_VALUES);

            if (savedValues != null) {
                mNewValues.addAll(savedValues);
            }
        } else {
            // First opening: take the preference values
            String[] currentValuesArray = args.getStringArray(ARG_CURRENT_VALUES);

            if (currentValuesArray != null) {
                mNewValues.addAll(Arrays.asList(currentValuesArray));
            }
        }

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        DialogMultiListPreferenceCustomBinding binding = DialogMultiListPreferenceCustomBinding.inflate(getLayoutInflater());

        final List<MaterialCheckBox> checkBoxes = new ArrayList<>();

        for (int i = 0; i < Objects.requireNonNull(entries).length; i++) {
            MaterialCheckBox checkBox = new MaterialCheckBox(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            final String value = Objects.requireNonNull(entryValues)[i].toString();

            checkBox.setLayoutParams(params);
            checkBox.setMinHeight((int) dpToPx(48, getResources().getDisplayMetrics()));
            checkBox.setText(entries[i]);
            checkBox.setPadding((int) dpToPx(20, getResources().getDisplayMetrics()), 0, 0, 0);
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            checkBox.setTypeface(typeface);

            if (isAlarmFeaturesPref) {
                int iconResId = getIconForFeatureValue(value);
                if (iconResId != 0) {
                    checkBox.setCompoundDrawablesRelativeWithIntrinsicBounds(iconResId, 0, 0, 0);
                    checkBox.setCompoundDrawablePadding((int) dpToPx(10, getResources().getDisplayMetrics()));
                    TextViewCompat.setCompoundDrawableTintList(checkBox, checkBox.getTextColors());
                }
            }

            checkBox.setChecked(mNewValues.contains(value));
            // Prevent the CheckBox animation on the currently selected item when the Dialog opens.
            checkBox.jumpDrawablesToCurrentState();

            checkBox.setTag(value);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    mNewValues.add(value);
                } else {
                    mNewValues.remove(value);
                }

                updateCheckBoxesState(checkBoxes, isAlarmFeaturesPref);
            });

            checkBoxes.add(checkBox);
            binding.multiSelectionListOptions.addView(checkBox);
        }

        updateCheckBoxesState(checkBoxes, isAlarmFeaturesPref);

        int totalItems = Objects.requireNonNull(entries).length;
        String neutralText = isAlarmFeaturesPref ?
            getString(mNewValues.size() == totalItems ? R.string.uncheck_all : R.string.check_all) : null;

        return CustomDialog.create(
            context,
            null,
            null,
            title,
            null,
            binding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> {
                if (preference.callChangeListener(mNewValues)) {
                    preference.setValues(mNewValues);
                }
            },
            getString(android.R.string.cancel),
            null,
            isAlarmFeaturesPref ? neutralText : null,
            null,
            dialog -> {
                if (isAlarmFeaturesPref) {
                    Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    if (neutralButton != null) {
                        neutralButton.setOnClickListener(v -> {
                            boolean allChecked = mNewValues.size() == totalItems;

                            for (MaterialCheckBox checkBox : checkBoxes) {
                                checkBox.setChecked(!allChecked);
                            }
                        });
                    }
                }
            },
            CustomDialog.SoftInputMode.NONE
        );
    }

    @Override
    public void onDestroy() {
        preference = null;

        super.onDestroy();
    }

    private void resolvePreferenceIfNeeded() {
        if (preference != null) {
            return;
        }

        String key = requireArguments().getString(ARG_PREF_KEY);
        Fragment parent = getParentFragment();
        if (!(parent instanceof PreferenceFragmentCompat preferenceFragmentCompat) || key == null) {
            return;
        }

        Preference pref = preferenceFragmentCompat.findPreference(key);
        if (pref instanceof MultiSelectListPreference multiSelectListPreference) {
            preference = multiSelectListPreference;
        }
    }

    /**
     * Locks the last checkbox selected to prevent the user from unchecking it, and unlocks all checkboxes if there is more than one.
     */
    private void updateCheckBoxesState(@NonNull List<MaterialCheckBox> checkBoxes, boolean isAlarmFeaturesPref) {
        boolean isOnlyOneChecked = (mNewValues.size() == 1);

        for (MaterialCheckBox checkBox : checkBoxes) {
            String value = (String) checkBox.getTag();

            if (isAlarmFeaturesPref) {
                checkBox.setEnabled(true);
            } else {
                checkBox.setEnabled(!isOnlyOneChecked || !mNewValues.contains(value));
            }
        }

        if (isAlarmFeaturesPref && getDialog() instanceof AlertDialog alertDialog) {
            Button neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            if (neutralButton != null) {
                if (mNewValues.size() == checkBoxes.size()) {
                    neutralButton.setText(R.string.uncheck_all);
                } else {
                    neutralButton.setText(R.string.check_all);
                }
            }
        }
    }

    @DrawableRes
    private int getIconForFeatureValue(@NonNull String value) {
        return switch (value) {
            case VISIBLE_SCHEDULE_ALARM -> R.drawable.ic_calendar_clock;
            case VISIBLE_PAUSE_ALARM -> R.drawable.ic_alarm_pause;
            case VISIBLE_TIME_ZONE -> R.drawable.ic_globe_clock;
            case VISIBLE_ALARM_VIBRATE -> R.drawable.ic_vibrate;
            case VISIBLE_VIBRATION_PATTERN -> R.drawable.ic_earthquake;
            case VISIBLE_ALARM_FLASH -> R.drawable.ic_flash;
            case VISIBLE_DELETE_OCCASIONAL_ALARM -> R.drawable.ic_clear;
            case VISIBLE_DISABLE_DND -> R.drawable.ic_bedtime_off;
            case VISIBLE_AUTO_SILENCE -> R.drawable.ic_ringtone_off;
            case VISIBLE_SNOOZE_DURATION -> R.drawable.ic_snooze;
            case VISIBLE_MISSED_ALARM_REPEAT_LIMIT -> R.drawable.ic_repeat;
            case VISIBLE_VOLUME -> R.drawable.ic_volume_up;
            case VISIBLE_CRESCENDO_DURATION -> R.drawable.ic_crescendo;
            case VISIBLE_MATH_HARDNESS_LEVEL -> R.drawable.ic_calculate;
            case VISIBLE_BACKGROUND_IMAGE -> R.drawable.ic_image;
            case VISIBLE_TASK -> R.drawable.ic_task;
            case VISIBLE_LOCK -> R.drawable.ic_lock;
            default -> 0;
        };
    }

}

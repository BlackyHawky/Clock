// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
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
     * @return A fully configured instance of {@link CustomListPreferenceDialogFragment} ready to be displayed.
     */
    public static CustomMultiSelectListPreferenceDialogFragment newInstance(MultiSelectListPreference pref) {
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
    public static void show(FragmentManager manager, CustomMultiSelectListPreferenceDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        resolvePreferenceIfNeeded();

        Bundle args = requireArguments();
        CharSequence title = args.getCharSequence(ARG_TITLE);
        CharSequence[] entries = args.getCharSequenceArray(ARG_ENTRIES);
        CharSequence[] entryValues = args.getCharSequenceArray(ARG_ENTRY_VALUES);
        String[] currentValuesArray = args.getStringArray(ARG_CURRENT_VALUES);

        if (currentValuesArray != null) {
            mNewValues.addAll(Arrays.asList(currentValuesArray));
        }

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        @SuppressLint("InflateParams")
        View listView = getLayoutInflater().inflate(R.layout.dialog_multi_list_preference_custom, null);
        LinearLayout linearLayout = listView.findViewById(R.id.multi_selection_list_options);

        final List<MaterialCheckBox> checkBoxes = new ArrayList<>();

        for (int i = 0; i < Objects.requireNonNull(entries).length; i++) {
            MaterialCheckBox checkBox = new MaterialCheckBox(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            final String value = Objects.requireNonNull(entryValues)[i].toString();

            checkBox.setLayoutParams(params);
            checkBox.setText(entries[i]);
            checkBox.setPadding((int) dpToPx(20, getResources().getDisplayMetrics()), 0, 0, 0);
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            checkBox.setTypeface(typeface);
            checkBox.setChecked(mNewValues.contains(value));
            checkBox.setTag(value);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    mNewValues.add(value);
                } else {
                    mNewValues.remove(value);
                }

                updateCheckBoxesState(checkBoxes);
            });

            checkBoxes.add(checkBox);
            linearLayout.addView(checkBox);
        }

        updateCheckBoxesState(checkBoxes);

        return CustomDialog.create(
            context,
            null,
            null,
            title,
            null,
            listView,
            getString(android.R.string.ok),
            (d, w) -> {
                if (preference.callChangeListener(mNewValues)) {
                    preference.setValues(mNewValues);
                }
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );
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
    private void updateCheckBoxesState(List<MaterialCheckBox> checkBoxes) {
        boolean isOnlyOneChecked = (mNewValues.size() == 1);

        for (MaterialCheckBox checkBox : checkBoxes) {
            String value = (String) checkBox.getTag();

            checkBox.setEnabled(!isOnlyOneChecked || !mNewValues.contains(value));
        }
    }

}

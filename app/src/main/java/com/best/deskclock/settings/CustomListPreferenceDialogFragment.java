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
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.settings.custompreference.CustomListPreference;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.Objects;

/**
 * A dialog fragment that displays the selectable entries of a
 * {@link CustomListPreference} using a custom Material-styled layout.
 *
 * <p>The dialog highlights the currently selected value and updates the
 * associated preference when the user selects a new option. The change
 * listener of the preference is invoked before applying the new value.</p>
 */
public class CustomListPreferenceDialogFragment extends DialogFragment {

    private static final String TAG = "custom_list_pref_dialog";

    private static final String ARG_PREF_KEY = "arg_pref_key";
    private static final String ARG_TITLE = "title";
    private static final String ARG_ENTRIES = "entries";
    private static final String ARG_ENTRY_VALUES = "entry_values";
    private static final String ARG_CURRENT_VALUE = "current_value";

    private CustomListPreference preference;

    /**
     * Creates a new instance of {@link CustomListPreferenceDialogFragment} for use
     * in the settings screen, allowing the user to select a value from a list of options.
     *
     * <p>This method extracts all necessary information from the provided
     * {@link CustomListPreference}, including its key, title, entries, entry values,
     * and currently selected value. These details are then passed to the dialog so it
     * can display the correct options and highlight the active selection.</p>
     *
     * @param pref The {@link CustomListPreference} associated with this dialog.
     *             Its configuration and current value will be used to initialize
     *             the dialog.
     *
     * @return A fully configured instance of {@link CustomListPreferenceDialogFragment}
     *         ready to be displayed.
     */
    public static CustomListPreferenceDialogFragment newInstance(CustomListPreference pref) {
        Bundle args = new Bundle();
        args.putString(ARG_PREF_KEY, pref.getKey());
        args.putCharSequence(ARG_TITLE, pref.getTitle());
        args.putCharSequenceArray(ARG_ENTRIES, pref.getEntries());
        args.putCharSequenceArray(ARG_ENTRY_VALUES, pref.getEntryValues());
        args.putString(ARG_CURRENT_VALUE, pref.getValue());

        CustomListPreferenceDialogFragment frag = new CustomListPreferenceDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Replaces any existing CustomListPreferenceDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, CustomListPreferenceDialogFragment fragment) {
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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();

        resolvePreferenceIfNeeded();

        Bundle args = requireArguments();
        CharSequence title = args.getCharSequence(ARG_TITLE);
        CharSequence[] entries = args.getCharSequenceArray(ARG_ENTRIES);
        CharSequence[] entryValues = args.getCharSequenceArray(ARG_ENTRY_VALUES);
        String currentValue = args.getString(ARG_CURRENT_VALUE);

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        @SuppressLint("InflateParams")
        View listView = getLayoutInflater().inflate(R.layout.dialog_list_preference_custom, null);

        RadioGroup radioGroup = listView.findViewById(R.id.list_options);

        int currentIndex = -1;
        for (int i = 0; i < Objects.requireNonNull(entryValues).length; i++) {
            if (entryValues[i].equals(currentValue)) {
                currentIndex = i;
                break;
            }
        }

        for (int i = 0; i < Objects.requireNonNull(entries).length; i++) {
            MaterialRadioButton radioButton = new MaterialRadioButton(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            radioButton.setLayoutParams(params);
            radioButton.setText(entries[i]);
            radioButton.setTag(i);
            radioButton.setPadding((int) dpToPx(20, getResources().getDisplayMetrics()), 0, 0, 0);
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            radioButton.setTypeface(typeface);

            if (i == currentIndex) {
                radioButton.setChecked(true);
            }

            radioButton.setOnClickListener(v -> {
                int index = (int) v.getTag();
                String newValue = entryValues[index].toString();

                if (preference.callChangeListener(newValue)) {
                    preference.setValue(newValue);
                }

                dismiss();
            });

            radioGroup.addView(radioButton);
        }

        return CustomDialog.create(
                context,
                null,
                null,
                title,
                null,
                listView,
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
        if (pref instanceof CustomListPreference customListPreference) {
            preference = customListPreference;
        }
    }

}

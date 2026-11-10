// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.databinding.TimeZoneDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.Locale;

/**
 * DialogFragment to set the alarm time zone.
 */
public class TimeZoneDialogFragment extends DialogFragment  {

    /**
     * The tag that identifies instances of {@link TimeZoneDialogFragment} in the fragment manager.
     */
    public static final String TAG = "alarm_time_zone_dialog";

    public static final String REQUEST_KEY = "request_alarm_time_zone";
    public static final String RESULT_PREF_KEY = "result_pref_key";
    private static final String ARG_PREF_KEY = "arg_pref_key";
    public static final String RESULT_TIMEZONE = "result_time_zone";
    private static final String ARG_SELECTED_TIMEZONE = "arg_selected_timezone";

    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_IS_SEARCH_FOCUSED = "is_search_focused";

    private TimeZoneDialogBinding mBinding;

    private String mPrefKey;
    private boolean mIsSearchFocused = false;

    /**
     * Creates a new instance of {@link TimeZoneDialogFragment} for use in the settings screen,
     * allowing the user to choose a time zone.
     *
     * @param key      The shared preference key used to identify the setting.
     * @param timeZone The currently time zone, which will be preselected in the dialog.
     * @return A configured instance of {@link TimeZoneDialogFragment}.
     */
    @NonNull
    public static TimeZoneDialogFragment newInstance(@NonNull String key, @Nullable String timeZone) {
        Bundle args = new Bundle();
        args.putString(ARG_PREF_KEY, key);
        args.putString(ARG_SELECTED_TIMEZONE, timeZone == null ? "" : timeZone);

        TimeZoneDialogFragment frag = new TimeZoneDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link TimeZoneDialogFragment} for use in the alarm editing panel,
     * where the time zone is configured for a specific alarm.
     * @param timeZone The ID of the current time zone (e.g., "America/New_York"), or "" / null for local time.
     */
    @NonNull
    public static TimeZoneDialogFragment newInstance(@Nullable String timeZone) {
        final Bundle args = new Bundle();
        args.putString(ARG_SELECTED_TIMEZONE, timeZone == null ? "" : timeZone);

        final TimeZoneDialogFragment fragment = new TimeZoneDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link TimeZoneDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull TimeZoneDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_SELECTED_TIMEZONE, getSelectedTimeZone());

        if (mBinding != null) {
            outState.putString(KEY_SEARCH_QUERY, mBinding.searchView.getQuery().toString());
        }

        outState.putBoolean(KEY_IS_SEARCH_FOCUSED, mIsSearchFocused);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        boolean isFlagEnabled = SettingsDAO.isCityFlagEnabled(prefs);

        mPrefKey = requireArguments().getString(ARG_PREF_KEY, null);

        String selectedTimeZone = requireArguments().getString(ARG_SELECTED_TIMEZONE, "");
        if (savedInstanceState != null) {
            selectedTimeZone = savedInstanceState.getString(ARG_SELECTED_TIMEZONE, selectedTimeZone);
        }

        mBinding = TimeZoneDialogBinding.inflate(getLayoutInflater());

        long currentTime = System.currentTimeMillis();
        TimeZones timeZones = SettingsDAO.getTimeZones(requireContext(), currentTime, isFlagEnabled);
        int paddingStart = (int) dpToPx(20, getResources().getDisplayMetrics());
        int minHeight = (int) dpToPx(48, getResources().getDisplayMetrics());

        populateRadioGroup(timeZones, selectedTimeZone, typeface, paddingStart, minHeight);

        bindSearchBar(typeface, savedInstanceState);

        mBinding.listOptions.setOnCheckedChangeListener((group, checkedId) -> saveAndDismiss(getSelectedTimeZone()));

        boolean isPreference = (mPrefKey != null);

        return CustomDialog.create(
            requireContext(),
            null,
            isPreference ? null : AppCompatResources.getDrawable(requireContext(), R.drawable.ic_globe_clock),
            getString(isPreference ? R.string.home_time_zone : R.string.timezone_title),
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
        mIsSearchFocused = false;
        mBinding = null;

        super.onDestroyView();
    }

    private void populateRadioGroup(@NonNull TimeZones timeZones, @NonNull String selectedTimeZone, @NonNull Typeface typeface,
                                    int paddingStart, int minHeight) {

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
                // Prevent the RadioButton animation on the currently selected item when the Dialog opens.
                radioButton.jumpDrawablesToCurrentState();
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

    private void bindSearchBar(@NonNull Typeface typeface, @Nullable Bundle savedInstanceState) {
        mBinding.searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mBinding.searchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mBinding.searchView.setBackground(ThemeUtils.pillBackgroundFromAttr(
            requireContext(), getResources().getDisplayMetrics(), com.google.android.material.R.attr.colorSecondaryContainer)
        );

        TextView searchText = mBinding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTypeface(typeface);

            ThemeUtils.disableAutofill(searchText);
        }

        // Hide the bottom bar of the search field
        final View searchPlate = mBinding.searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackground(null);
        }

        mBinding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mBinding.searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTimeZones(newText);
                return true;
            }
        });

        mBinding.searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (isResumed()) {
                mIsSearchFocused = hasFocus;
            }

            if (getDialog() != null) {
                Window window = getDialog().getWindow();
                if (window == null) {
                    return;
                }

                if (hasFocus) {
                    //noinspection deprecation
                    int mode = !ThemeUtils.isTablet() && ThemeUtils.isPortrait()
                        // For phones in portrait mode, resize the window to keep the dialog buttons visible.
                        ? WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                        : WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

                    window.setSoftInputMode(mode);
                } else {
                    window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
            }
        });

        if (savedInstanceState != null) {
            String query = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            mIsSearchFocused = savedInstanceState.getBoolean(KEY_IS_SEARCH_FOCUSED, false);

            mBinding.searchView.setQuery(query, false);

            filterTimeZones(query);

            if (mIsSearchFocused) {
                mBinding.searchView.requestFocus();

                mBinding.searchView.postDelayed(() -> {
                    if (getDialog() != null) {
                        Utils.showKeyboard(getDialog().getWindow(), mBinding.searchView);
                    }
                }, Utils.UI_SETTLE_SHORT_DELAY_MS);
            } else {
                mBinding.searchView.clearFocus();
            }
        } else {
            mBinding.searchView.post(() -> mBinding.searchView.clearFocus());
        }
    }

    /**
     * Filters {@link MaterialRadioButton} by changing their visibility.
     */
    private void filterTimeZones(@NonNull String query) {
        if (mBinding == null) {
            return;
        }

        Locale locale = Utils.getLocaleFromContext(requireContext());
        String lowerCaseQuery = Utils.removeSpecialCharacters(query).toLowerCase(locale);
        int count = mBinding.listOptions.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = mBinding.listOptions.getChildAt(i);

            if (child instanceof MaterialRadioButton radioButton) {
                String rawName = radioButton.getText().toString();
                String name = Utils.removeSpecialCharacters(rawName).toLowerCase(locale);

                if (name.contains(lowerCaseQuery)) {
                    radioButton.setVisibility(View.VISIBLE);
                } else {
                    radioButton.setVisibility(View.GONE);
                }
            }
        }
    }

    private void saveAndDismiss(@NonNull String timeZone) {
        Bundle result = new Bundle();
        result.putString(RESULT_TIMEZONE, timeZone);

        if (mPrefKey != null) {
            result.putString(RESULT_PREF_KEY, mPrefKey);
        }

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

}

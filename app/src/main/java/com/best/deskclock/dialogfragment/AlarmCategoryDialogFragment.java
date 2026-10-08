// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.DialogMultiListPreferenceCustomBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.ClockContract;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DialogFragment to choose an alarm category.
 */
public class AlarmCategoryDialogFragment extends DialogFragment {

    public static final String TAG = "AlarmCategoryDialogFragment";
    public static final String REQUEST_KEY = "request_alarm_category";
    public static final String RESULT_CATEGORY_IDS = "result_category_ids";

    private static final String ARG_SELECTED_CATEGORIES = "arg_selected_categories";

    private DialogMultiListPreferenceCustomBinding mBinding;

    private long[] mSelectedCategoryIds;
    private Button mNeutralButton;

    /**
     * @param categoryIds Array of current alarm category IDs.
     */
    @NonNull
    public static AlarmCategoryDialogFragment newInstance(long[] categoryIds) {
        final Bundle args = new Bundle();
        args.putLongArray(ARG_SELECTED_CATEGORIES, categoryIds);

        final AlarmCategoryDialogFragment fragment = new AlarmCategoryDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link AlarmCategoryDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull AlarmCategoryDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLongArray(ARG_SELECTED_CATEGORIES, getSelectedCategoryIds());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        mSelectedCategoryIds = requireArguments().getLongArray(ARG_SELECTED_CATEGORIES);
        if (mSelectedCategoryIds == null) {
            mSelectedCategoryIds = new long[]{Alarm.DEFAULT_CATEGORY_ID};
        }

        if (savedInstanceState != null) {
            long[] savedIds = savedInstanceState.getLongArray(ARG_SELECTED_CATEGORIES);

            if (savedIds != null) {
                mSelectedCategoryIds = savedIds;
            }
        }

        mBinding = DialogMultiListPreferenceCustomBinding.inflate(getLayoutInflater());

        loadCategoriesAndPopulate(typeface);

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_folder),
            getString(R.string.label_category),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> saveAndDismiss(getSelectedCategoryIds()),
            getString(android.R.string.cancel),
            null,
            getString(R.string.label_all_categories),
            null,
            alertDialog -> {
                mNeutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                updateNeutralButtonState();

                mNeutralButton.setOnClickListener(v -> toggleAllCategories());
            },
            CustomDialog.SoftInputMode.NONE
        );
    }

    @Override
    public void onDestroyView() {
        mNeutralButton = null;

        mBinding = null;

        super.onDestroyView();
    }

    private void loadCategoriesAndPopulate(@NonNull Typeface typeface) {
        Context appContext = requireContext().getApplicationContext();
        ContentResolver cr = appContext.getContentResolver();

        AppExecutors.getDiskIO().execute(() -> {
            List<Long> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();

            ids.add(Alarm.DEFAULT_CATEGORY_ID);
            names.add(appContext.getString(R.string.label_default));

            try (Cursor cursor = cr.query(
                ClockContract.CategoryColumns.CONTENT_URI,
                new String[]{ClockContract.CategoryColumns._ID, ClockContract.CategoryColumns.NAME},
                null,
                null,
                ClockContract.CategoryColumns.SORT_ORDER + " ASC"
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idIndex = cursor.getColumnIndexOrThrow(ClockContract.CategoryColumns._ID);
                    int nameIndex = cursor.getColumnIndexOrThrow(ClockContract.CategoryColumns.NAME);
                    do {
                        ids.add(cursor.getLong(idIndex));
                        names.add(cursor.getString(nameIndex));
                    } while (cursor.moveToNext());
                }
            }

            AppExecutors.getMainThread().post(() -> {
                if (mBinding != null && isAdded()) {
                    populateCheckBoxes(ids, names, typeface);
                }
            });
        });
    }

    private void populateCheckBoxes(@NonNull List<Long> ids, @NonNull List<String> names, @NonNull Typeface typeface) {
        int paddingStart = (int) dpToPx(20, getResources().getDisplayMetrics());
        int minHeight = (int) dpToPx(48, getResources().getDisplayMetrics());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        MaterialCheckBox firstCheckedBox = null;

        Set<Long> selectedIdsSet = new HashSet<>();
        for (long id : mSelectedCategoryIds) {
            selectedIdsSet.add(id);
        }

        CompoundButton.OnCheckedChangeListener checkListener = (buttonView, isChecked) -> {
            updateCheckBoxesState();
            updateNeutralButtonState();
        };

        for (int i = 0; i < ids.size(); i++) {
            long categoryId = ids.get(i);
            String categoryName = names.get(i);

            MaterialCheckBox checkBox = new MaterialCheckBox(requireContext());
            checkBox.setId(View.generateViewId());
            checkBox.setLayoutParams(params);
            checkBox.setText(categoryName);
            checkBox.setTag(categoryId);
            checkBox.setTypeface(typeface);
            checkBox.setPadding(paddingStart, 0, 0, 0);
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            checkBox.setMinHeight(minHeight);

            mBinding.multiSelectionListOptions.addView(checkBox);

            if (selectedIdsSet.contains(categoryId)) {
                checkBox.setChecked(true);
                // Prevent the CheckBox animation on the currently selected item when the Dialog opens.
                checkBox.jumpDrawablesToCurrentState();

                if (firstCheckedBox == null) {
                    firstCheckedBox = checkBox;
                }
            }

            checkBox.setOnCheckedChangeListener(checkListener);
        }

        if (firstCheckedBox != null && mBinding.multiSelectionListOptions.getParent() instanceof NestedScrollView scrollView) {
            final View finalCheckedBox = firstCheckedBox;

            scrollView.post(() -> {
                int scrollToY = finalCheckedBox.getTop() - (scrollView.getHeight() / 3);
                scrollView.scrollTo(0, Math.max(0, scrollToY));
            });
        }

        updateCheckBoxesState();
        updateNeutralButtonState();
    }

    private long[] getSelectedCategoryIds() {
        if (mBinding == null) {
            return new long[]{Alarm.DEFAULT_CATEGORY_ID};
        }

        List<Long> checkedIds = new ArrayList<>();

        for (int i = 0; i < mBinding.multiSelectionListOptions.getChildCount(); i++) {
            View child = mBinding.multiSelectionListOptions.getChildAt(i);

            if (child instanceof MaterialCheckBox checkBox && checkBox.isChecked()) {
                if (checkBox.getTag() instanceof Long tag) {
                    checkedIds.add(tag);
                }
            }
        }

        if (checkedIds.isEmpty()) {
            return new long[]{Alarm.DEFAULT_CATEGORY_ID};
        }

        long[] result = new long[checkedIds.size()];
        for (int i = 0; i < checkedIds.size(); i++) {
            result[i] = checkedIds.get(i);
        }

        return result;
    }

    private void toggleAllCategories() {
        if (mBinding == null) {
            return;
        }

        boolean allChecked = true;
        for (int i = 0; i < mBinding.multiSelectionListOptions.getChildCount(); i++) {
            View child = mBinding.multiSelectionListOptions.getChildAt(i);
            if (child instanceof MaterialCheckBox checkBox && !checkBox.isChecked()) {
                allChecked = false;
                break;
            }
        }

        for (int i = 0; i < mBinding.multiSelectionListOptions.getChildCount(); i++) {
            View child = mBinding.multiSelectionListOptions.getChildAt(i);
            if (child instanceof MaterialCheckBox checkBox) {
                if (allChecked) {
                    boolean isDefault = (checkBox.getTag() instanceof Long tag) && (tag == Alarm.DEFAULT_CATEGORY_ID);
                    checkBox.setChecked(isDefault);
                } else {
                    checkBox.setChecked(true);
                }

                checkBox.jumpDrawablesToCurrentState();
            }
        }
    }

    private void updateCheckBoxesState() {
        if (mBinding == null) {
            return;
        }

        int checkedCount = 0;
        int childCount = mBinding.multiSelectionListOptions.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = mBinding.multiSelectionListOptions.getChildAt(i);
            if (child instanceof MaterialCheckBox checkBox && checkBox.isChecked()) {
                checkedCount++;
            }
        }

        boolean isOnlyOneChecked = (checkedCount == 1);

        for (int i = 0; i < childCount; i++) {
            View child = mBinding.multiSelectionListOptions.getChildAt(i);
            if (child instanceof MaterialCheckBox checkBox) {
                checkBox.setEnabled(!isOnlyOneChecked || !checkBox.isChecked());
            }
        }
    }

    private void updateNeutralButtonState() {
        if (mBinding == null || mNeutralButton == null) {
            return;
        }


        boolean allChecked = true;
        for (int i = 0; i < mBinding.multiSelectionListOptions.getChildCount(); i++) {
            View child = mBinding.multiSelectionListOptions.getChildAt(i);
            if (child instanceof MaterialCheckBox checkBox && !checkBox.isChecked()) {
                allChecked = false;
                break;
            }
        }

        if (allChecked) {
            mNeutralButton.setText(R.string.label_default);
        } else {
            mNeutralButton.setText(R.string.label_all_categories);
        }
    }

    private void saveAndDismiss(long[] categoryIds) {
        Bundle result = new Bundle();
        result.putLongArray(RESULT_CATEGORY_IDS, categoryIds);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

}

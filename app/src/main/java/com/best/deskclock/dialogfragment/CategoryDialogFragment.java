// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.DialogEditTextBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.ClockContract;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/**
 * DialogFragment to edit, rename or delete the category where the alarm is stored.
 */
public class CategoryDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of {@link CategoryDialogFragment} in the fragment manager.
     */
    public static final String TAG = "category_dialog";

    public static final String REQUEST_KEY = "category_request_key";
    public static final String RESULT_CATEGORY_ID = "result_category_id";
    public static final String RESULT_CATEGORY_NAME = "result_category_name";
    public static final String RESULT_ACTION = "result_action";

    public static final int ACTION_SAVE = 1;
    public static final int ACTION_DELETE = 2;

    private static final String ARG_CATEGORY_ID = "arg_category_id";
    private static final String ARG_CATEGORY_NAME = "arg_category_name";

    private DialogEditTextBinding mBinding;

    private long mCategoryId;
    private String mOriginalCategoryName = "";
    private Button mPositiveButton;

    private final TextWatcher mTextWatcher = new TextChangeListener();

    /**
     * Creates a new instance to create a new category.
     */
    @NonNull
    public static CategoryDialogFragment newInstance() {
        return newInstance(-1, "");
    }

    /**
     * Creates a new instance to rename or delete an existing category.
     */
    @NonNull
    public static CategoryDialogFragment newInstance(long categoryId, @NonNull String categoryName) {
        final Bundle args = new Bundle();
        args.putLong(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_CATEGORY_NAME, categoryName);

        final CategoryDialogFragment frag = new CategoryDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Displays {@link CategoryDialogFragment} with the given {@code fragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull CategoryDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mBinding != null && mBinding.edit.getText() != null) {
            outState.putString(ARG_CATEGORY_NAME, mBinding.edit.getText().toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Bundle args = requireArguments();
        mCategoryId = args.getLong(ARG_CATEGORY_ID, -1);
        mOriginalCategoryName = args.getString(ARG_CATEGORY_NAME, "");

        String currentName = mOriginalCategoryName;
        if (savedInstanceState != null) {
            currentName = savedInstanceState.getString(ARG_CATEGORY_NAME, currentName);
        }

        final boolean isEditing = mCategoryId > Alarm.DEFAULT_CATEGORY_ID;
        final CharSequence title = getString(isEditing ? R.string.rename_category : R.string.create_category);

        mBinding = DialogEditTextBinding.inflate(getLayoutInflater());

        mBinding.edit.setText(currentName);
        mBinding.edit.setTypeface(ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(requireContext()))));
        mBinding.edit.addTextChangedListener(mTextWatcher);
        mBinding.edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mBinding.edit.selectAll();
        mBinding.edit.requestFocus();
        mBinding.edit.setMaxLines(1);
        mBinding.edit.setHorizontallyScrolling(true);
        mBinding.edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mBinding.edit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mPositiveButton != null && mPositiveButton.isEnabled()) {
                    saveCategory();
                    dismiss();
                }

                return true;
            }

            return false;
        });

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), isEditing ? R.drawable.ic_bookmark_manager : R.drawable.ic_create_new_folder),
            title,
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> saveCategory(),
            getString(android.R.string.cancel),
            null,
            isEditing ? getString(R.string.delete) : null,
            (d, w) -> deleteCategory(),
            alertDialog -> {
                mPositiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                checkInputState(mBinding.edit.getText() != null ? mBinding.edit.getText().toString() : "");
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        mBinding.edit.requestFocus();
        mBinding.edit.postDelayed(() -> {
            if (getDialog() != null) {
                Utils.showKeyboard(getDialog().getWindow(), mBinding.edit);
            }
        }, Utils.UI_SETTLE_DELAY_MS);
    }

    @Override
    public void onDestroyView() {
        mBinding.edit.setOnEditorActionListener(null);
        mBinding.edit.removeTextChangedListener(mTextWatcher);

        mBinding = null;

        mPositiveButton = null;

        super.onDestroyView();
    }

    private void saveCategory() {
        String name = Objects.requireNonNull(mBinding.edit.getText()).toString().trim();

        if (TextUtils.isEmpty(name) || isNameDuplicateSync(name)) {
            return;
        }

        Bundle result = new Bundle();
        result.putLong(RESULT_CATEGORY_ID, mCategoryId);
        result.putString(RESULT_CATEGORY_NAME, name);
        result.putInt(RESULT_ACTION, ACTION_SAVE);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    private void deleteCategory() {
        Bundle result = new Bundle();
        result.putLong(RESULT_CATEGORY_ID, mCategoryId);
        result.putInt(RESULT_ACTION, ACTION_DELETE);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    private void checkInputState(String name) {
        String trimmedName = name.trim();

        if (trimmedName.isEmpty()) {
            updateDialogForValidInput();
            if (mPositiveButton != null) {
                mPositiveButton.setEnabled(false);
            }
            return;
        }

        if (trimmedName.equalsIgnoreCase(mOriginalCategoryName.trim())) {
            updateDialogForValidInput();
            return;
        }

        if (trimmedName.equalsIgnoreCase(getString(R.string.label_default))) {
            updateDialogForInvalidInput();
            return;
        }

        ContentResolver cr = requireContext().getApplicationContext().getContentResolver();

        AppExecutors.getDiskIO().execute(() -> {
            boolean isDuplicate = doesCategoryExistInDb(cr, trimmedName);

            AppExecutors.getMainThread().post(() -> {
                if (mBinding == null || !isAdded()) {
                    return;
                }

                String currentText = mBinding.edit.getText() != null ? mBinding.edit.getText().toString().trim() : "";
                if (!currentText.equalsIgnoreCase(trimmedName)) {
                    return;
                }

                if (isDuplicate) {
                    updateDialogForInvalidInput();
                } else {
                    updateDialogForValidInput();
                }
            });
        });
    }

    private boolean isNameDuplicateSync(String name) {
        if (name.equalsIgnoreCase(mOriginalCategoryName.trim())) {
            return false;
        }

        if (name.equalsIgnoreCase(getString(R.string.label_default))) {
            return true;
        }

        return doesCategoryExistInDb(requireContext().getContentResolver(), name);
    }

    private boolean doesCategoryExistInDb(ContentResolver cr, String name) {
        try (Cursor cursor = cr.query(
            ClockContract.CategoryColumns.CONTENT_URI,
            new String[]{ClockContract.CategoryColumns._ID},
            ClockContract.CategoryColumns.NAME + " = ? COLLATE NOCASE",
            new String[]{name},
            null
        )) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            LogUtils.e(TAG, "Error checking for duplicate category name", e);
            return false;
        }
    }

    private void updateDialogForInvalidInput() {
        if (getDialog() == null) {
            return;
        }

        AlertDialog alertDialog = (AlertDialog) getDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error), null, null, null);
            titleText.setCompoundDrawablePadding((int) (18 * getResources().getDisplayMetrics().density));

            titleText.setText(getString(R.string.category_error));
        }

        int invalidColor = ContextCompat.getColor(requireContext(), R.color.md_theme_error);
        mBinding.textInputLayout.setBoxStrokeColor(invalidColor);
        mBinding.textInputLayout.setHintTextColor(ColorStateList.valueOf(invalidColor));

        if (mPositiveButton != null) {
            mPositiveButton.setEnabled(false);
        }
    }

    private void updateDialogForValidInput() {
        if (getDialog() == null) {
            return;
        }

        AlertDialog alertDialog = (AlertDialog) getDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            final boolean isEditing = mCategoryId > Alarm.DEFAULT_CATEGORY_ID;

            titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                AppCompatResources.getDrawable(requireContext(),
                    isEditing ? R.drawable.ic_bookmark_manager : R.drawable.ic_create_new_folder),
                null, null, null);
            titleText.setCompoundDrawablePadding((int) (18 * getResources().getDisplayMetrics().density));

            titleText.setText(getString(isEditing ? R.string.rename_category : R.string.create_category));
        }

        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        mBinding.textInputLayout.setBoxStrokeColor(validColor);
        mBinding.textInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));

        if (mPositiveButton != null) {
            mPositiveButton.setEnabled(true);
        }
    }

    /**
     * A listener that reacts to text changes in the edit field.
     * <p>Enables or disables the default button depending on whether the edit text is empty.</p>
     */
    private class TextChangeListener implements TextWatcher {
        @Override
        public void onTextChanged(@Nullable CharSequence s, int start, int before, int count) {
            String name = s != null ? s.toString() : "";

            checkInputState(name);
        }

        @Override
        public void beforeTextChanged(@Nullable CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(@Nullable Editable s) {
        }
    }

}

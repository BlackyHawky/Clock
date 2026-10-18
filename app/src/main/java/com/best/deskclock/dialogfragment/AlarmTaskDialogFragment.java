// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
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
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmTaskDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/**
 * DialogFragment to set the task to perform after dismissing/snoozing the alarm.
 */
public class AlarmTaskDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of {@link AlarmTaskDialogFragment} in the fragment manager.
     */
    private static final String TAG = "alarm_task_dialog";


    public static final String REQUEST_KEY = "task_request_key";
    public static final String RESULT_NAME = "result_name";
    public static final String RESULT_URL = "result_url";

    private static final String ARG_TASK_NAME = "arg_name";
    private static final String ARG_TASK_URL = "arg_url";

    AlarmTaskDialogBinding mBinding;

    private Button mOkButton;

    private final TextWatcher mTextWatcher = new TextChangeListener();

    /**
     * Creates a new instance of {@link AlarmTaskDialogFragment} to edit the task of the given alarm.
     *
     * @param alarmTaskName the task name
     * @param alarmTaskUrl the task URL
     */
    @NonNull
    public static AlarmTaskDialogFragment newInstance(@NonNull String alarmTaskName, @NonNull String alarmTaskUrl) {
        final Bundle args = new Bundle();
        args.putString(ARG_TASK_NAME, alarmTaskName);
        args.putString(ARG_TASK_URL, alarmTaskUrl);

        final AlarmTaskDialogFragment frag = new AlarmTaskDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Displays {@link AlarmTaskDialogFragment} with the given {@code fragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull AlarmTaskDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as the dialog exists, save its state.
        outState.putString(ARG_TASK_NAME, Objects.requireNonNull(mBinding.editName.getText()).toString());
        outState.putString(ARG_TASK_URL, Objects.requireNonNull(mBinding.editUrl.getText()).toString());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Typeface generalTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(requireContext())));
        final Bundle args = requireArguments();
        String taskName = args.getString(ARG_TASK_NAME);
        String taskUrl = args.getString(ARG_TASK_URL);

        if (savedInstanceState != null) {
            taskName = savedInstanceState.getString(ARG_TASK_NAME, taskName);
            taskUrl = savedInstanceState.getString(ARG_TASK_URL, taskUrl);
        }

        mBinding = AlarmTaskDialogBinding.inflate(getLayoutInflater());

        mBinding.textInputLayoutName.setTypeface(generalTypeface);
        mBinding.editName.setTypeface(generalTypeface);
        mBinding.textInputLayoutUrl.setTypeface(generalTypeface);
        mBinding.editUrl.setTypeface(generalTypeface);

        TextView urlHelper = mBinding.textInputLayoutUrl.findViewById(com.google.android.material.R.id.textinput_helper_text);
        urlHelper.setTypeface(generalTypeface);

        mBinding.selectApp.setTypeface(generalTypeface);

        mBinding.editName.setText(taskName);
        mBinding.editName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mBinding.editName.selectAll();
        mBinding.editName.requestFocus();
        mBinding.editName.setMaxLines(2);
        mBinding.editName.setHorizontallyScrolling(false);
        mBinding.editName.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        mBinding.editUrl.setText(taskUrl);
        mBinding.editUrl.setInputType(InputType.TYPE_CLASS_TEXT);
        mBinding.editUrl.addTextChangedListener(mTextWatcher);
        mBinding.editUrl.setMaxLines(10);
        mBinding.editUrl.setHorizontallyScrolling(false);
        mBinding.editUrl.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mBinding.editUrl.setOnEditorActionListener(new ImeDoneListener());
        mBinding.editUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editUrl.selectAll();
            }
        });

        getChildFragmentManager().setFragmentResultListener(AlarmAppPickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                String appName = bundle.getString(AlarmAppPickerDialogFragment.RESULT_APP_NAME);
                String packageName = bundle.getString(AlarmAppPickerDialogFragment.RESULT_PACKAGE_NAME);

                mBinding.editName.setText(appName);

                String url = "app://" + packageName;
                mBinding.editUrl.setText(url);

                if (mOkButton != null) {
                    mOkButton.setEnabled(true);
                }
            });

        mBinding.selectApp.setOnClickListener(v ->
            AlarmAppPickerDialogFragment.show(getChildFragmentManager(), AlarmAppPickerDialogFragment.newInstance()));

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_task),
            getString(R.string.task_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> setTask(),
            getString(android.R.string.cancel),
            null,
            getString(R.string.delete),
            (d, w) -> applyTask("", ""),
            alertDialog -> {
                mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                String url = mBinding.editUrl.getText() != null ? mBinding.editUrl.getText().toString() : "";

                mOkButton.setEnabled(!isInvalidInput(url));
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        mBinding.editName.requestFocus();
        mBinding.editName.postDelayed(() -> {
            if (getDialog() != null) {
                Utils.showKeyboard(getDialog().getWindow(), mBinding.editName);
            }
        }, Utils.UI_SETTLE_DELAY_MS);
    }

    @Override
    public void onDestroyView() {
        mBinding.editUrl.setOnEditorActionListener(null);
        mBinding.editUrl.removeTextChangedListener(mTextWatcher);

        mOkButton = null;
        mBinding = null;

        super.onDestroyView();
    }

    /**
     * Set the task name and the task URL entered by the user.
     */
    private void setTask() {
        String taskName = Objects.requireNonNull(mBinding.editName.getText()).toString();
        String taskUrl = Objects.requireNonNull(mBinding.editUrl.getText()).toString();

        applyTask(taskName, taskUrl);
    }

    private void applyTask(@NonNull String name, @NonNull String url) {
        String trimmedName = name.trim();
        String trimmedUrl = url.trim();

        if (trimmedUrl.isEmpty()) {
            trimmedName = "";
        }

        Bundle result = new Bundle();
        result.putString(RESULT_NAME, trimmedName);
        result.putString(RESULT_URL, trimmedUrl);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    private boolean isInvalidInput(@NonNull String taskUrl) {
        String url = taskUrl.trim();

        if (url.isEmpty()) {
            return false;
        }

        return !taskUrl.trim().startsWith("http://")
            && !taskUrl.trim().startsWith("https://")
            && !taskUrl.trim().startsWith("app://");
    }

    /**
     * Update the dialog icon, title, and OK button for invalid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForInvalidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(
                requireContext(), R.drawable.ic_error), null, null, null);

            titleText.setText(getString(R.string.warning));
        }

        int invalidColor = ContextCompat.getColor(requireContext(), R.color.md_theme_error);

        mBinding.textInputLayoutUrl.setBoxStrokeColor(invalidColor);
        mBinding.textInputLayoutUrl.setHintTextColor(ColorStateList.valueOf(invalidColor));

        if (mOkButton != null) {
            mOkButton.setEnabled(false);
        }
    }

    /**
     * Update the dialog icon, title, and OK button for valid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForValidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();

        TextView titleText = alertDialog.findViewById(R.id.dialog_title);
        if (titleText != null) {
            titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(
                    requireContext(), R.drawable.ic_task), null, null, null);

            titleText.setText(getString(R.string.task_title));
        }

        int validColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

        mBinding.textInputLayoutUrl.setBoxStrokeColor(validColor);
        mBinding.textInputLayoutUrl.setHintTextColor(ColorStateList.valueOf(validColor));

        if (mOkButton != null) {
            mOkButton.setEnabled(true);
        }
    }

    /**
     * A listener that reacts to text changes in the url field.
     *
     * <p>Enables or disables the default button depending on whether the typed URL is valid.</p>
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(@Nullable CharSequence charSequence, int start, int before, int count) {
            String taskUrl = mBinding.editUrl.getText() != null ? mBinding.editUrl.getText().toString() : "";

            if (isInvalidInput(taskUrl)) {
                updateDialogForInvalidInput();
                return;
            }

            updateDialogForValidInput();
        }

        @Override
        public void beforeTextChanged(@Nullable CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(@Nullable Editable editable) {
        }
    }

    /**
     * Handles completing the new alarm task from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(@NonNull TextView v, int actionId, @Nullable KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String taskUrl = Objects.requireNonNull(mBinding.editUrl.getText()).toString();

                if (isInvalidInput(taskUrl)) {
                    updateDialogForInvalidInput();
                } else {
                    setTask();
                    dismiss();
                }

                return true;
            }

            return false;
        }
    }

}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_NEVER;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.provider.Alarm;
import com.best.deskclock.utils.SdkUtils;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

/**
 * DialogFragment to set the auto silence duration for alarms.
 */
public class AutoSilenceDurationDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of AutoSilenceDurationDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_auto_silence_duration_dialog";

    private static final String AUTO_SILENCE_DURATION = "auto_silence_duration_";
    private static final String ARG_PREF_KEY = AUTO_SILENCE_DURATION + "arg_pref_key";
    private static final String ARG_EDIT_AUTO_SILENCE_MINUTES =
            AUTO_SILENCE_DURATION + "arg_edit_auto_silence_minutes";
    private static final String ARG_END_OF_RINGTONE =
            AUTO_SILENCE_DURATION + "arg_end_of_ringtone";
    public static final String RESULT_PREF_KEY = AUTO_SILENCE_DURATION + "result_pref_key";
    public static final String REQUEST_KEY = AUTO_SILENCE_DURATION + "request_key";
    public static final String AUTO_SILENCE_DURATION_VALUE = AUTO_SILENCE_DURATION + "value";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_TAG = "arg_tag";

    private Context mContext;
    private Alarm mAlarm;
    private String mTag;
    private TextInputLayout mMinutesInputLayout;
    private TextInputEditText mEditMinutes;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private MaterialCheckBox mEndOfRingtoneCheckbox;

    /**
     * Creates a new instance of {@link AutoSilenceDurationDialogFragment} for use
     * in the settings screen, where the auto silence duration is configured independently
     * of a specific alarm.
     *
     * @param key             The shared preference key used to identify the setting.
     * @param totalMinutes    The crescendo duration in seconds.
     * @param isEndOfRingtone true if the auto silence duration should correspond
     *                        to the end of the ringtone playback rather than a fixed time.
     */
    public static AutoSilenceDurationDialogFragment newInstance(String key, int totalMinutes,
                                                                boolean isEndOfRingtone) {

        Bundle args = new Bundle();

        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_EDIT_AUTO_SILENCE_MINUTES, totalMinutes);
        args.putBoolean(ARG_END_OF_RINGTONE, isEndOfRingtone);

        AutoSilenceDurationDialogFragment frag = new AutoSilenceDurationDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates a new instance of {@link AutoSilenceDurationDialogFragment} for use
     * in the expanded alarm view, where the auto silence duration is configured for a specific alarm.
     *
     * @param alarm                The alarm instance being edited.
     * @param autoSilenceDuration  The silence duration in minutes.
     * @param isEndOfRingtone      true if the auto silence duration should correspond
     *                             to the end of the ringtone playback rather than a fixed time.
     * @param tag                  A tag identifying the fragment in the fragment manager.
     */
    public static AutoSilenceDurationDialogFragment newInstance(Alarm alarm, int autoSilenceDuration,
                                                                boolean isEndOfRingtone, String tag) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);
        args.putInt(ARG_EDIT_AUTO_SILENCE_MINUTES, autoSilenceDuration);
        args.putBoolean(ARG_END_OF_RINGTONE, isEndOfRingtone);

        final AutoSilenceDurationDialogFragment fragment = new AutoSilenceDurationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Replaces any existing AutoSilenceDurationDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, AutoSilenceDurationDialogFragment fragment) {
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEditMinutes != null) {
            String minutesStr = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);

            outState.putInt(ARG_EDIT_AUTO_SILENCE_MINUTES, minutes);
        }
        outState.putBoolean(ARG_END_OF_RINGTONE, mEndOfRingtoneCheckbox.isChecked());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = requireContext();

        final Bundle args = requireArguments();
        mAlarm = SdkUtils.isAtLeastAndroid13()
                ? args.getParcelable(ARG_ALARM, Alarm.class)
                : args.getParcelable(ARG_ALARM);
        mTag = args.getString(ARG_TAG);

        int editMinutes = args.getInt(ARG_EDIT_AUTO_SILENCE_MINUTES, 0);
        boolean isEndOfRingtone = args.getBoolean(ARG_END_OF_RINGTONE, false);

        if (savedInstanceState != null) {
            editMinutes = savedInstanceState.getInt(ARG_EDIT_AUTO_SILENCE_MINUTES, editMinutes);
            isEndOfRingtone = savedInstanceState.getBoolean(ARG_END_OF_RINGTONE, isEndOfRingtone);
        }

        View view = LayoutInflater.from(mContext).inflate(R.layout.alarm_auto_silence_duration_dialog, null);

        mMinutesInputLayout = view.findViewById(R.id.dialog_input_layout_minutes);
        mMinutesInputLayout.setHelperText(getString(R.string.timer_button_time_minutes_warning_box_text));

        mEditMinutes = view.findViewById(R.id.edit_minutes);
        mEndOfRingtoneCheckbox = view.findViewById(R.id.end_of_ringtone);

        mEditMinutes.setText(String.valueOf(editMinutes));
        if (editMinutes == ALARM_TIMEOUT_END_OF_RINGTONE) {
            mEditMinutes.setText("");
        } else if (editMinutes == ALARM_TIMEOUT_NEVER) {
            mEditMinutes.setText(String.valueOf(0));
        }
        mEditMinutes.setEnabled(!isEndOfRingtone);
        mEditMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditMinutes.selectAll();
        mEditMinutes.requestFocus();
        mEditMinutes.setOnEditorActionListener(new ImeDoneListener());
        mEditMinutes.addTextChangedListener(mTextWatcher);

        mEndOfRingtoneCheckbox.setChecked(isEndOfRingtone);
        mEndOfRingtoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mEditMinutes.setEnabled(!isChecked);

            if (isChecked) {
                mEditMinutes.setText("");
            } else {
                mEditMinutes.selectAll();
                mEditMinutes.requestFocus();
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditMinutes, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.auto_silence_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        setAutoSilenceDuration()
                )
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog = dialogBuilder.create();

        final Window alertDialogWindow = dialog.getWindow();
        if (alertDialogWindow != null) {
            alertDialogWindow.setSoftInputMode(SOFT_INPUT_ADJUST_PAN | SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mEndOfRingtoneCheckbox.isChecked()) {
            mEditMinutes.requestFocus();
            mEditMinutes.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(mEditMinutes, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop callbacks from the IME since there is no view to process them.
        mEditMinutes.setOnEditorActionListener(null);
        mEditMinutes.removeTextChangedListener(mTextWatcher);
    }

    /**
     * Set the auto silence duration.
     */
    private void setAutoSilenceDuration() {
        int minutes = 0;

        if (mEndOfRingtoneCheckbox.isChecked()) {
            minutes = ALARM_TIMEOUT_END_OF_RINGTONE;
        } else {
            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            if (!minutesText.isEmpty()) {
                minutes = Integer.parseInt(minutesText);
            }

            if (minutes == 0) {
                minutes = ALARM_TIMEOUT_NEVER;
            }
        }

        if (mAlarm != null) {
            ((AutoSilenceDurationDialogHandler) requireActivity())
                    .onDialogAutoSilenceDurationSet(mAlarm, minutes, mTag);
        } else {
            Bundle result = new Bundle();
            result.putInt(AUTO_SILENCE_DURATION_VALUE, minutes);
            result.putString(RESULT_PREF_KEY, requireArguments().getString(ARG_PREF_KEY));
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }
    }

    /**
     * @return {@code true} if:
     * <ul>
     *     <li>minutes are less than 0 or greater than 60</li>
     * </ul>
     * {@code false} otherwise.
     */
    private boolean isInvalidInput(String minutesText) {
        int minutes = 0;

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        return minutes < 0 || minutes > 60;
    }

    /**
     * Update the dialog icon and title for invalid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForInvalidInput() {
        final Drawable drawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_error);
        if (drawable != null) {
            drawable.setTint(MaterialColors.getColor(
                    mContext, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        AlertDialog alertDialog = (AlertDialog) requireDialog();
        alertDialog.setIcon(drawable);
        alertDialog.setTitle(getString(R.string.timer_time_warning_box_title));

        String minutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
        boolean minutesInvalid = (!minutesText.isEmpty() && Integer.parseInt(minutesText) < 0)
                || (!minutesText.isEmpty() && Integer.parseInt(minutesText) > 60);
        int invalidColor = ContextCompat.getColor(mContext, R.color.md_theme_error);
        int validColor = MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorPrimary, Color.BLACK);

        mMinutesInputLayout.setBoxStrokeColor(minutesInvalid ? invalidColor : validColor);
        mMinutesInputLayout.setHintTextColor(minutesInvalid
                ? ColorStateList.valueOf(invalidColor)
                : ColorStateList.valueOf(validColor));
    }

    /**
     * Update the dialog icon and title for valid entries.
     * The outline color of the edit box and the hint color are also changed.
     */
    private void updateDialogForValidInput() {
        AlertDialog alertDialog = (AlertDialog) requireDialog();
        alertDialog.setIcon(null);
        alertDialog.setTitle(getString(R.string.auto_silence_title));

        int validColor = MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorPrimary, Color.BLACK);
        mMinutesInputLayout.setBoxStrokeColor(validColor);
        mMinutesInputLayout.setHintTextColor(ColorStateList.valueOf(validColor));
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mEndOfRingtoneCheckbox.isChecked()) {
                updateDialogForValidInput();
                return;
            }

            String minutesText = mEditMinutes.getText() != null ? mEditMinutes.getText().toString() : "";

            if (isInvalidInput(minutesText)) {
                updateDialogForInvalidInput();
            } else {
                updateDialogForValidInput();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    /**
     * Handles completing the new auto silence duration from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputMinutesText = Objects.requireNonNull(mEditMinutes.getText()).toString();
                if (isInvalidInput(inputMinutesText)) {
                    updateDialogForInvalidInput();
                } else {
                    setAutoSilenceDuration();
                    dismiss();
                }
                return true;
            }

            return false;
        }
    }

    public interface AutoSilenceDurationDialogHandler {
        void onDialogAutoSilenceDurationSet(Alarm alarm, int autoSilenceDuration, String tag);
    }
}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.databinding.TimerDialogEditNewTimeBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * DialogFragment to set a new duration to the timer.
 */
public class TimerSetNewDurationDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of TimerSetNewDurationDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_new_duration_dialog";

    private static final String ARG_EDIT_HOURS = "arg_edit_hours";
    private static final String ARG_EDIT_MINUTES = "arg_edit_minutes";
    private static final String ARG_EDIT_SECONDS = "arg_edit_seconds";
    private static final String ARG_TIMER_ID = "arg_timer_id";

    private TimerDialogEditNewTimeBinding mBinding;

    private boolean mMaxLengthReduce;
    private int mTimerId;
    private final TextWatcher mTextWatcher = new TextChangeListener();
    private InputMethodManager mInput;

    public static TimerSetNewDurationDialogFragment newInstance(Timer timer) {
        final Bundle args = new Bundle();

        long remainingTime = timer.getRemainingTime();
        int hours = (int) TimeUnit.MILLISECONDS.toHours(remainingTime);
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60;

        args.putInt(ARG_EDIT_HOURS, hours);
        args.putInt(ARG_EDIT_MINUTES, minutes);
        args.putInt(ARG_EDIT_SECONDS, seconds);
        args.putInt(ARG_TIMER_ID, timer.getId());

        final TimerSetNewDurationDialogFragment frag = new TimerSetNewDurationDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Displays {@link TimerSetNewDurationDialogFragment}.
     */
    public static void show(FragmentManager manager, TimerSetNewDurationDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        outState.putString(ARG_EDIT_HOURS, Objects.requireNonNull(mBinding.editHours.getText()).toString());
        outState.putString(ARG_EDIT_MINUTES, Objects.requireNonNull(mBinding.editMinutes.getText()).toString());
        outState.putString(ARG_EDIT_SECONDS, Objects.requireNonNull(mBinding.editSeconds.getText()).toString());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = requireContext();
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        mTimerId = args.getInt(ARG_TIMER_ID, -1);

        int editHours = args.getInt(ARG_EDIT_HOURS, 0);
        int editMinutes = args.getInt(ARG_EDIT_MINUTES, 0);
        int editSeconds = args.getInt(ARG_EDIT_SECONDS, 0);
        if (savedInstanceState != null) {
            editHours = savedInstanceState.getInt(ARG_EDIT_HOURS, editHours);
            editMinutes = savedInstanceState.getInt(ARG_EDIT_MINUTES, editMinutes);
            editSeconds = savedInstanceState.getInt(ARG_EDIT_SECONDS, editSeconds);
        }

        mInput = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        mBinding = TimerDialogEditNewTimeBinding.inflate(getLayoutInflater());

        mBinding.textInputLayoutHours.setTypeface(typeFace);
        mBinding.textInputLayoutMinutes.setTypeface(typeFace);
        mBinding.textInputLayoutSeconds.setTypeface(typeFace);

        mBinding.editHours.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mBinding.editHours.setText(String.valueOf(editHours));
        mBinding.editHours.setTypeface(typeFace);
        mBinding.editHours.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editHours.setFilters(new InputFilter[]{
            new InputFilter.LengthFilter(3)
        });
        mBinding.editHours.selectAll();
        mBinding.editHours.requestFocus();
        mBinding.editHours.addTextChangedListener(mTextWatcher);
        mBinding.editHours.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editHours.selectAll();
            }
        });

        mBinding.editMinutes.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mBinding.editMinutes.setText(String.valueOf(editMinutes));
        mBinding.editMinutes.setTypeface(typeFace);
        mBinding.editMinutes.selectAll();
        mBinding.editMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editMinutes.addTextChangedListener(mTextWatcher);
        mBinding.editMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editMinutes.selectAll();
            }
        });

        mBinding.editSeconds.setText(String.valueOf(editSeconds));
        mBinding.editSeconds.setTypeface(typeFace);
        mBinding.editSeconds.selectAll();
        mBinding.editSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        mBinding.editSeconds.setOnEditorActionListener(new ImeDoneListener());
        mBinding.editSeconds.addTextChangedListener(mTextWatcher);
        mBinding.editSeconds.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mBinding.editSeconds.selectAll();
            }
        });

        return CustomDialog.create(
            context,
            null,
            AppCompatResources.getDrawable(context, R.drawable.ic_hourglass_top),
            getString(R.string.timer_time_box_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> setNewDuration(),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        mBinding.editHours.requestFocus();
        mBinding.editHours.postDelayed(() -> {
            if (mInput != null) {
                mInput.showSoftInput(mBinding.editHours, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    @Override
    public void onDestroyView() {
        // Stop callbacks from the IME since there is no view to process them.
        mBinding.editHours.setOnEditorActionListener(null);
        mBinding.editHours.removeTextChangedListener(mTextWatcher);
        mBinding.editHours.setOnFocusChangeListener(null);

        mBinding.editMinutes.setOnEditorActionListener(null);
        mBinding.editMinutes.removeTextChangedListener(mTextWatcher);
        mBinding.editMinutes.setOnFocusChangeListener(null);

        mBinding.editSeconds.setOnEditorActionListener(null);
        mBinding.editSeconds.removeTextChangedListener(mTextWatcher);
        mBinding.editSeconds.setOnFocusChangeListener(null);

        mInput = null;

        mBinding = null;

        super.onDestroyView();
    }

    /**
     * Sets the new duration to the timer.
     */
    private void setNewDuration() {
        String hoursText = Objects.requireNonNull(mBinding.editHours.getText()).toString();
        String minutesText = Objects.requireNonNull(mBinding.editMinutes.getText()).toString();
        String secondsText = Objects.requireNonNull(mBinding.editSeconds.getText()).toString();

        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        if (!hoursText.isEmpty()) {
            hours = Integer.parseInt(hoursText);
        }

        if (!minutesText.isEmpty()) {
            minutes = Integer.parseInt(minutesText);
        }

        if (!secondsText.isEmpty()) {
            seconds = Integer.parseInt(secondsText);
        }

        if ((hoursText.isEmpty() && minutesText.isEmpty() && secondsText.isEmpty()) || (hours == 0 && minutes == 0 && seconds == 0)) {
            seconds = 1;
        }

        if (mTimerId >= 0) {
            final Timer timer = DataModel.getDataModel().getTimer(mTimerId);
            if (timer != null) {
                int totalSeconds = hours * 3600 + minutes * 60 + seconds;
                long newLengthMillis = totalSeconds * 1000L;

                String currentLabel = timer.getLabel();
                String oldDefaultLabel = Utils.buildDefaultTimerLabel(requireContext(), timer.getLength());
                boolean shouldUpdateLabel = currentLabel != null && currentLabel.equals(oldDefaultLabel);

                DataModel.getDataModel().setNewTimerDuration(timer, newLengthMillis);

                if (shouldUpdateLabel) {
                    String newDefaultLabel = Utils.buildDefaultTimerLabel(requireContext(), newLengthMillis);

                    Timer updatedTimer = DataModel.getDataModel().getTimer(mTimerId);

                    DataModel.getDataModel().setTimerLabel(updatedTimer != null ? updatedTimer : timer, newDefaultLabel);
                }
            }
        }
    }

    /**
     * Alters the UI to indicate when input is valid or invalid.
     * Note: In the hours field, if the hours are equal to 24, the entry can be validated with
     * the enter key, otherwise the enter key will switch to the seconds field.
     */
    private class TextChangeListener implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (!mMaxLengthReduce) {
                mBinding.editHours.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(2)
                });

                mMaxLengthReduce = true;
            }

            String hoursText = mBinding.editHours.getText() != null ? mBinding.editHours.getText().toString() : "";
            if ("100".equals(hoursText)) {
                mBinding.editHours.setText(String.valueOf(99));
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
     * Handles completing the new duration from the IME keyboard.
     */
    private class ImeDoneListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setNewDuration();
                dismiss();

                return true;
            }

            return false;
        }
    }

}

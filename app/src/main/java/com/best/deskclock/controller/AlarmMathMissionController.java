// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.controller;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmActivity;
import com.best.deskclock.alarms.AlarmMathChallenge;
import com.best.deskclock.databinding.AlarmMathChallengeDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.Random;

public class AlarmMathMissionController {
    private final Context mContext;
    private final Callback mCallback;
    private final Typeface mGeneralBoldTypeface;
    private final DisplayMetrics mDisplayMetrics;
    private final boolean mIsVibrationEnabled;
    private AlertDialog mCurrentDialog;

    /**
     * Fully round keypad keys, without a separate pressed shape.
     */
    private static final ShapeAppearanceModel ROUND_KEY_SHAPE =
        ShapeAppearanceModel.builder().setAllCornerSizes(ShapeAppearanceModel.PILL).build();

    public AlarmMathMissionController(@NonNull Context context, @NonNull Typeface generalBoldTypeface,
                                      @NonNull DisplayMetrics displayMetrics, boolean isVibrationEnabled, @NonNull Callback callback) {
        mContext = context;
        mGeneralBoldTypeface = generalBoldTypeface;
        mDisplayMetrics = displayMetrics;
        mIsVibrationEnabled = isVibrationEnabled;
        mCallback = callback;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void requestMissionAction(int action, @NonNull String mathHardnessLevel, @NonNull Random random) {
        if (mCurrentDialog != null && mCurrentDialog.isShowing()) {
            return;
        }

        final AlarmMathChallenge[] activeChallenge = { AlarmMathChallenge.create(mathHardnessLevel, random) };

        final AlarmMathChallengeDialogBinding binding = AlarmMathChallengeDialogBinding.inflate(LayoutInflater.from(mContext));

        binding.mathChallengeTitle.setTypeface(mGeneralBoldTypeface);
        binding.mathChallengeTitle.setText(mContext.getString(action == AlarmActivity.MISSION_ACTION_SNOOZE
            ? R.string.math_challenge_title_snooze
            : R.string.math_challenge_title_dismiss));

        binding.mathChallengeText.setTypeface(mGeneralBoldTypeface);
        binding.mathChallengeText.setText(
            mContext.getString(R.string.math_challenge_prompt, activeChallenge[0].left(), activeChallenge[0].right())
        );

        binding.answerInputLayout.setTypeface(mGeneralBoldTypeface);
        binding.answerInput.setTypeface(mGeneralBoldTypeface);
        binding.answerInput.setShowSoftInputOnFocus(false);
        binding.answerInput.setLongClickable(false);
        binding.answerInput.requestFocus();

        // Select all text when double-tapping
        GestureDetector gestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (!TextUtils.isEmpty(binding.answerInput.getText())) {
                        binding.answerInput.selectAll();
                    }
                }

                return true;
            }
        });

        binding.answerInput.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // Block the display of the system menu (Copy, Paste, Autofill)
        ActionMode.Callback noPopupCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }

            @Override
            public void onDestroyActionMode(ActionMode mode) {}
        };

        binding.answerInput.setCustomSelectionActionModeCallback(noPopupCallback);
        binding.answerInput.setCustomInsertionActionModeCallback(noPopupCallback);

        if (SdkUtils.isAtLeastAndroid8()) {
            binding.answerInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        binding.mathChallengeError.setVisibility(INVISIBLE);
        binding.mathChallengeError.setTypeface(mGeneralBoldTypeface, Typeface.BOLD);

        buildKeypad(binding);

        mCurrentDialog = CustomDialog.create(
            mContext,
            null,
            null,
            null,
            null,
            binding.getRoot(),
            mContext.getString(android.R.string.ok),
            null,
            mContext.getString(android.R.string.cancel),
            (d, w) -> {
                mCurrentDialog = null;
                mCallback.onMissionCancelled();
            },
            mContext.getString(R.string.math_challenge_new_problem),
            null,
            alertDialog -> {
                alertDialog.setCancelable(false);

                if (alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                }

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                    validateAnswer(binding.answerInput, activeChallenge[0], binding.mathChallengeError, alertDialog, action));

                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    activeChallenge[0] = AlarmMathChallenge.create(mathHardnessLevel, random);

                    binding.mathChallengeText.setText(mContext.getString(R.string.math_challenge_prompt,
                        activeChallenge[0].left(), activeChallenge[0].right()));

                    binding.answerInput.setText("");
                    binding.mathChallengeError.setVisibility(INVISIBLE);

                    binding.answerInput.requestFocus();
                });
            },
            CustomDialog.SoftInputMode.NONE
        );

        mCurrentDialog.show();
    }

    private void buildKeypad(@NonNull AlarmMathChallengeDialogBinding binding) {
        final GridLayout keypad = binding.mathChallengeKeypad;
        keypad.removeAllViews();

        // 1 2 3
        // 4 5 6
        // 7 8 9
        // C 0 ⌫
        final String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "CLEAR", "0", "DELETE"};

        MaterialButton clearButton = null;
        MaterialButton deleteButton = null;

        for (String key : keys) {
            final MaterialButton button =
                new MaterialButton(mContext, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);

            button.setStrokeColorResource(R.color.icon_tint);
            button.setIconTintResource(R.color.icon_tint);
            button.setTextColor(ContextCompat.getColorStateList(mContext, R.color.icon_tint));
            button.setTypeface(mGeneralBoldTypeface);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            button.setInsetTop(0);
            button.setInsetBottom(0);
            button.setShapeAppearanceModel(ROUND_KEY_SHAPE);

            switch (key) {
                case "CLEAR" -> {
                    clearButton = button;

                    button.setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_clear));
                    button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                    button.setIconPadding(0);
                    button.setIconSize((int) dpToPx(32, mDisplayMetrics));
                    button.setContentDescription(mContext.getString(R.string.delete));
                    button.setEnabled(false);
                    button.setOnClickListener(v -> {
                        Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstants.CLOCK_TICK);
                        binding.answerInput.setText("");
                    });
                }

                case "DELETE" -> {
                    deleteButton = button;

                    button.setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_backspace));
                    button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                    button.setIconPadding(0);
                    button.setIconSize((int) dpToPx(32, mDisplayMetrics));
                    button.setContentDescription(mContext.getString(R.string.delete));
                    button.setEnabled(false);
                    button.setOnClickListener(v -> {
                        Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstants.CLOCK_TICK);

                        Editable text = binding.answerInput.getText();
                        if (!TextUtils.isEmpty(text)) {
                            int start = binding.answerInput.getSelectionStart();
                            int end = binding.answerInput.getSelectionEnd();

                            if (start >= 0 && end >= 0) {
                                int realStart = Math.min(start, end);
                                int realEnd = Math.max(start, end);

                                if (realStart != realEnd) {
                                    text.delete(realStart, realEnd);
                                } else if (realStart > 0) {

                                    text.delete(realStart - 1, realStart);
                                }
                            } else {
                                text.delete(text.length() - 1, text.length());
                            }
                        }
                    });
                }

                default -> {
                    button.setText(key);
                    button.setOnClickListener(v -> {
                        Utils.performHapticFeedback(v, mIsVibrationEnabled, HapticFeedbackConstants.CLOCK_TICK);

                        int start = binding.answerInput.getSelectionStart();
                        int end = binding.answerInput.getSelectionEnd();
                        Editable text = binding.answerInput.getText();

                        if (text != null && start >= 0 && end >= 0) {
                            int realStart = Math.min(start, end);
                            int realEnd = Math.max(start, end);

                            text.replace(realStart, realEnd, key);
                            binding.answerInput.setSelection(realStart + key.length());
                        } else {
                            binding.answerInput.append(key);
                        }
                    });
                }
            }

            final int margin = (int) dpToPx(4, mDisplayMetrics);
            final int height = (int) dpToPx(56, mDisplayMetrics);

            final GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));

            params.width = 0;
            params.height = height;
            params.setMargins(margin, margin, margin, margin);

            keypad.addView(button, params);
        }

        if (clearButton != null && deleteButton != null) {
            final MaterialButton finalClearBtn = clearButton;
            final MaterialButton finalDeleteBtn = deleteButton;

            binding.answerInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (binding.mathChallengeError.getVisibility() != INVISIBLE) {
                        binding.mathChallengeError.setVisibility(INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    boolean hasText = !TextUtils.isEmpty(s);

                    finalClearBtn.setEnabled(hasText);
                    finalDeleteBtn.setEnabled(hasText);

                    if (hasText) {
                        char lastDigit = s.charAt(s.length() - 1);
                        finalDeleteBtn.setContentDescription(
                            mContext.getString(R.string.timer_descriptive_delete, String.valueOf(lastDigit)));

                        finalClearBtn.setContentDescription(mContext.getString(R.string.delete));
                    } else {
                        finalDeleteBtn.setContentDescription(mContext.getString(R.string.delete));
                        finalClearBtn.setContentDescription(mContext.getString(R.string.delete));
                    }
                }
            });
        }
    }

    private void validateAnswer(@NonNull EditText answerInput, @NonNull AlarmMathChallenge challenge, @NonNull TextView mathChallengeError,
                                @NonNull AlertDialog dialog, int action) {

        final String answerText = answerInput.getText() == null ? "" : answerInput.getText().toString().trim();

        if (!challenge.matches(answerText)) {
            mathChallengeError.setVisibility(VISIBLE);
        } else {
            dialog.dismiss();
            mCurrentDialog = null;
            mCallback.onMissionResolved(action);
        }
    }

    public interface Callback {
        void onMissionResolved(int action);
        void onMissionCancelled();
    }

}

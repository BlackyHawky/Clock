// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.controller;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmActivity;
import com.best.deskclock.alarms.AlarmMathChallenge;
import com.best.deskclock.databinding.AlarmMathChallengeDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;

import java.util.Random;

public class AlarmMathMissionController {
    private final Context mContext;
    private final Callback mCallback;
    private final Typeface mGeneralBoldTypeface;
    private AlertDialog mCurrentDialog;

    public AlarmMathMissionController(Context context, Callback callback, Typeface generalBoldTypeface) {
        mContext = context;
        mCallback = callback;
        mGeneralBoldTypeface = generalBoldTypeface;
    }

    public void requestMissionAction(int action, String mathHardnessLevel, Random random) {
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
        binding.answerInput.requestFocus();
        binding.answerInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding.mathChallengeError.getVisibility() != INVISIBLE) {
                    binding.mathChallengeError.setVisibility(INVISIBLE);
                }
            }
        });

        binding.mathChallengeError.setVisibility(INVISIBLE);
        binding.mathChallengeError.setTypeface(mGeneralBoldTypeface, Typeface.BOLD);

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
            null,
            mContext.getString(R.string.math_challenge_new_problem),
            null,
            alertDialog -> {
                alertDialog.setCancelable(false);

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                    validateAnswer(binding.answerInput, activeChallenge[0], binding.mathChallengeError, alertDialog, action));

                binding.answerInput.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        validateAnswer(binding.answerInput, activeChallenge[0], binding.mathChallengeError, alertDialog, action);

                        return true;
                    }

                    return false;
                });

                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    activeChallenge[0] = AlarmMathChallenge.create(mathHardnessLevel, random);

                    binding.mathChallengeText.setText(mContext.getString(R.string.math_challenge_prompt,
                        activeChallenge[0].left(), activeChallenge[0].right()));

                    binding.answerInput.setText("");
                    binding.mathChallengeError.setVisibility(INVISIBLE);

                    binding.answerInput.requestFocus();
                });
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );

        mCurrentDialog.show();
    }

    private void validateAnswer(EditText answerInput, AlarmMathChallenge challenge, TextView mathChallengeError, AlertDialog dialog,
                                int action) {

        final String answerText = answerInput.getText() == null ? "" : answerInput.getText().toString().trim();

        if (!challenge.matches(answerText)) {
            mathChallengeError.setVisibility(VISIBLE);
            answerInput.selectAll();
        } else {
            dialog.dismiss();
            mCurrentDialog = null;
            mCallback.onMissionResolved(action);
        }
    }

    public interface Callback {
        void onMissionResolved(int action);
    }

}

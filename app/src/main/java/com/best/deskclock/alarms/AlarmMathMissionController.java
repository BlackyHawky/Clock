/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.best.deskclock.R;
import com.best.deskclock.provider.MathAlarmMission;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

final class AlarmMathMissionController {

    private final Context mContext;
    private final AlarmMissionControllerCallbacks mCallbacks;

    AlarmMathMissionController(Context context, AlarmMissionControllerCallbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
    }

    void requestMissionAction(int action, MathAlarmMission.MathChallenge challenge) {
        final View dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_edit_text, null);
        final EditText answerInput = dialogView.findViewById(android.R.id.edit);
        final View checkbox = dialogView.findViewById(R.id.sync_alarm_by_label);
        final View divider = dialogView.findViewById(R.id.divider);

        if (checkbox != null) {
            checkbox.setVisibility(View.GONE);
        }
        if (divider != null) {
            divider.setVisibility(View.GONE);
        }

        answerInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        answerInput.setHint(R.string.alarm_mission_answer_hint);

        final AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
            .setTitle(action == AlarmActivity.MISSION_ACTION_SNOOZE
                ? R.string.alarm_mission_title_snooze
                : R.string.alarm_mission_title_dismiss)
            .setMessage(mContext.getString(R.string.alarm_mission_math_prompt, challenge.left(), challenge.right()))
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .setCancelable(false);

        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(v -> {
                final String answerText = answerInput.getText() == null
                    ? ""
                    : answerInput.getText().toString().trim();

                if (!challenge.matches(answerText)) {
                    answerInput.setError(mContext.getString(R.string.alarm_mission_wrong_answer));
                    mCallbacks.onMissionMessage(R.string.alarm_mission_wrong_answer);
                    return;
                }

                dialog.dismiss();
                mCallbacks.onMissionResolved(action);
            }));

        dialog.show();
    }
}

/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;

import com.best.deskclock.R;
import com.best.deskclock.provider.AlarmMission;
import com.best.deskclock.utils.QrCaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

final class AlarmQrMissionController {

    private final SharedPreferences mPrefs;
    private final AlarmMissionControllerCallbacks mCallbacks;
    private final ActivityResultLauncher<ScanOptions> mQrScannerLauncher;

    private int mPendingMissionAction;
    private AlarmMission mPendingMission;

    AlarmQrMissionController(ActivityResultCaller caller, SharedPreferences prefs, AlarmMissionControllerCallbacks callbacks) {
        mPrefs = prefs;
        mCallbacks = callbacks;
        mQrScannerLauncher = caller.registerForActivityResult(new ScanContract(), result -> {
            if (result == null || result.getContents() == null || !ScanOptions.QR_CODE.equals(result.getFormatName())) {
                mCallbacks.onMissionMessage(R.string.alarm_mission_qr_scan_failed);
                return;
            }

            final String scannedValue = result.getContents().trim();
            if (scannedValue.isEmpty()) {
                mCallbacks.onMissionMessage(R.string.alarm_mission_qr_scan_failed);
                return;
            }

            if (mPendingMission == null) {
                return;
            }

            if (!mPendingMission.verifyInput(scannedValue, mPrefs)) {
                mCallbacks.onMissionMessage(R.string.alarm_mission_qr_wrong_code);
                return;
            }

            final int action = mPendingMissionAction;
            mPendingMission = null;
            mCallbacks.onMissionResolved(action);
        });
    }

    void requestMissionAction(int action, AlarmMission mission, Context context) {
        if (!mission.canStart(mPrefs)) {
            mCallbacks.onMissionMessage(R.string.alarm_mission_qr_settings_required);
            return;
        }

        mPendingMissionAction = action;
        mPendingMission = mission;

        final ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCaptureActivity(QrCaptureActivity.class);
        options.setPrompt(context.getString(R.string.alarm_mission_qr_prompt));
        options.setBeepEnabled(false);
        options.setOrientationLocked(false);

        mQrScannerLauncher.launch(options);
    }
}
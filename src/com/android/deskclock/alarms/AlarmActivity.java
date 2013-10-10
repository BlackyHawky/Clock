/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.alarms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.multiwaveview.GlowPadView;

/**
 * Alarm activity that pops up a visible indicator when the alarm goes off.
 */
public class AlarmActivity extends Activity {
    // AlarmActivity listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    // AlarmActivity listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    // Controller for GlowPadView.
    private class GlowPadController extends Handler implements GlowPadView.OnTriggerListener {
        private static final int PING_MESSAGE_WHAT = 101;
        private static final long PING_AUTO_REPEAT_DELAY_MSEC = 1200;

        public void startPinger() {
            sendEmptyMessage(PING_MESSAGE_WHAT);
        }

        public void stopPinger() {
            removeMessages(PING_MESSAGE_WHAT);
        }

        @Override
        public void handleMessage(Message msg) {
            ping();
            sendEmptyMessageDelayed(PING_MESSAGE_WHAT, PING_AUTO_REPEAT_DELAY_MSEC);
        }

        @Override
        public void onGrabbed(View v, int handle) {
            stopPinger();
        }

        @Override
        public void onReleased(View v, int handle) {
            startPinger();

        }

        @Override
        public void onTrigger(View v, int target) {
            switch (mGlowPadView.getResourceIdForTarget(target)) {
                case R.drawable.ic_alarm_alert_snooze:
                    snooze();
                    break;

                case R.drawable.ic_alarm_alert_dismiss:
                    dismiss();
                    break;
                default:
                    // Code should never reach here.
                    Log.e("Trigger detected on unhandled resource. Skipping.");
            }
        }

        @Override
        public void onGrabbedStateChange(View v, int handle) {
        }

        @Override
        public void onFinishFinalAnimation() {
        }
    }

    private AlarmInstance mInstance;
    private int mVolumeBehavior;
    private GlowPadView mGlowPadView;
    private GlowPadController glowPadController = new GlowPadController();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ALARM_SNOOZE_ACTION)) {
                snooze();
            } else if (action.equals(ALARM_DISMISS_ACTION)) {
                dismiss();
            } else if (action.equals(AlarmService.ALARM_DONE_ACTION)) {
                finish();
            } else {
                Log.i("Unknown broadcast in AlarmActivity: " + action);
            }
        }
    };

    private void snooze() {
        AlarmStateManager.setSnoozeState(this, mInstance);
    }

    private void dismiss() {
        AlarmStateManager.setDismissState(this, mInstance);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        long instanceId = AlarmInstance.getId(getIntent().getData());
        mInstance = AlarmInstance.getInstance(this.getContentResolver(), instanceId);
        if (mInstance != null) {
            Log.v("Displaying alarm for instance: " + mInstance);
        } else {
            // The alarm got deleted before the activity got created, so just finish()
            Log.v("Error displaying alarm for intent: " + getIntent());
            finish();
            return;
        }

        // Get the volume/camera button behavior setting
        final String vol =
                PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // In order to allow tablets to freely rotate and phones to stick
        // with "nosensor" (use default device orientation) we have to have
        // the manifest start with an orientation of unspecified" and only limit
        // to "nosensor" for phones. Otherwise we get behavior like in b/8728671
        // where tablets start off in their default orientation and then are
        // able to freely rotate.
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        updateLayout();

        // Register to get the alarm done/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }


    private void updateTitle() {
        final String titleText = mInstance.getLabelOrDefault(this);
        TextView tv = (TextView)findViewById(R.id.alertTitle);
        tv.setText(titleText);
        super.setTitle(titleText);
    }

    private void updateLayout() {
        final LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.alarm_alert, null);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(view);
        updateTitle();
        Utils.setTimeFormat((TextClock)(view.findViewById(R.id.digitalClock)),
                (int)getResources().getDimension(R.dimen.bottom_text_size));

        // Setup GlowPadController
        mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(glowPadController);
        glowPadController.startPinger();
    }

    private void ping() {
        mGlowPadView.ping();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glowPadController.startPinger();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glowPadController.stopPinger();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        Log.v("AlarmAlertFullScreen - dispatchKeyEvent " + event.getKeyCode());
        switch (event.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (mVolumeBehavior) {
                        case 1:
                            snooze();
                            break;

                        case 2:
                            dismiss();
                            break;

                        default:
                            break;
                    }
                }
                return true;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }
}

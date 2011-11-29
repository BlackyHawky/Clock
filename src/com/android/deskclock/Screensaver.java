/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.deskclock;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.animation.TimeInterpolator;
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.os.BatteryManager;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.Runnable;
import android.util.Log;

public class Screensaver extends Activity {
    static final boolean DEBUG = false;
    static final String TAG = "DeskClock/Screensaver";

    static int CLOCK_COLOR = 0xFF66AAFF;

    static final long MOVE_DELAY = 60000; // DeskClock.SCREEN_SAVER_MOVE_DELAY;
    static final long SLIDE_TIME = 10000;
    static final long FADE_TIME = 1000;

    static final boolean SLIDE = false;

    private View mContentView, mSaverView;

    private static TimeInterpolator mSlowStartWithBrakes =
        new TimeInterpolator() {
            public float getInterpolation(float x) {
                return (float)(Math.cos((Math.pow(x,3) + 1) * Math.PI) / 2.0f) + 0.5f;
            }
        };

    private Handler mHandler = new Handler();

    private boolean mPlugged = false;
    private final BroadcastReceiver mPowerIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                // Only keep the screen on if we're plugged in.
                boolean plugged = (0 != intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));
                if (plugged != mPlugged) {
                    if (DEBUG) Log.v(TAG, plugged ? "plugged in" : "unplugged");
                    mPlugged = plugged;
                    if (mPlugged) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            }
        }
    };

    private final Runnable mMoveSaverRunnable = new Runnable() {
        @Override
        public void run() {
            long delay = MOVE_DELAY;

//            Log.d("DeskClock/Screensaver",
//                    String.format("mContentView=(%d x %d) container=(%d x %d)",
//                        mContentView.getWidth(), mContentView.getHeight(),
//                        mSaverView.getWidth(), mSaverView.getHeight()
//                        ));
            final float xrange = mContentView.getWidth() - mSaverView.getWidth();
            final float yrange = mContentView.getHeight() - mSaverView.getHeight();

            if (xrange == 0 && yrange == 0) {
                delay = 500; // back in a split second
            } else {
                final int nextx = (int) (Math.random() * xrange);
                final int nexty = (int) (Math.random() * yrange);

                if (mSaverView.getAlpha() == 0f) {
                    // jump right there
                    mSaverView.setX(nextx);
                    mSaverView.setY(nexty);
                    ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f)
                        .setDuration(FADE_TIME)
                        .start();
                } else {
                    AnimatorSet s = new AnimatorSet();
                    Animator xMove   = ObjectAnimator.ofFloat(mSaverView,
                                         "x", mSaverView.getX(), nextx);
                    Animator yMove   = ObjectAnimator.ofFloat(mSaverView,
                                         "y", mSaverView.getY(), nexty);

                    Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", 1f, 0.85f);
                    Animator xGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleX", 0.85f, 1f);

                    Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", 1f, 0.85f);
                    Animator yGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleY", 0.85f, 1f);
                    AnimatorSet shrink = new AnimatorSet(); shrink.play(xShrink).with(yShrink);
                    AnimatorSet grow = new AnimatorSet(); grow.play(xGrow).with(yGrow);

                    Animator fadeout = ObjectAnimator.ofFloat(mSaverView, "alpha", 1f, 0f);
                    Animator fadein = ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f);


                    if (SLIDE) {
                        s.play(xMove).with(yMove);
                        s.setDuration(SLIDE_TIME);

                        s.play(shrink.setDuration(SLIDE_TIME/2));
                        s.play(grow.setDuration(SLIDE_TIME/2)).after(shrink);
                        s.setInterpolator(mSlowStartWithBrakes);
                    } else {
                        AccelerateInterpolator accel = new AccelerateInterpolator();
                        DecelerateInterpolator decel = new DecelerateInterpolator();

                        shrink.setDuration(FADE_TIME).setInterpolator(accel);
                        fadeout.setDuration(FADE_TIME).setInterpolator(accel);
                        grow.setDuration(FADE_TIME).setInterpolator(decel);
                        fadein.setDuration(FADE_TIME).setInterpolator(decel);
                        s.play(shrink);
                        s.play(fadeout);
                        s.play(xMove.setDuration(0)).after(FADE_TIME);
                        s.play(yMove.setDuration(0)).after(FADE_TIME);
                        s.play(fadein).after(FADE_TIME);
                        s.play(grow).after(FADE_TIME);
                    }
                    s.start();
                }

                long now = System.currentTimeMillis();
                long adjust = (now % 60000);
                delay = delay
                        + (MOVE_DELAY - adjust) // minute aligned
                        - (SLIDE ? 0 : FADE_TIME) // start moving before the fade
                        ;
                if (DEBUG) Log.d(TAG, 
                        "will move again in " + delay + " now=" + now + " adjusted by " + adjust);
            }

            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, delay);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        CLOCK_COLOR = getResources().getColor(R.color.screen_saver_color);
        setContentView(R.layout.desk_clock_saver);
        mSaverView = findViewById(R.id.saver_view);
        mContentView = (View) mSaverView.getParent();
        mSaverView.setAlpha(0);

        AndroidClockTextView timeDisplay = (AndroidClockTextView) findViewById(R.id.timeDisplay);
        if (timeDisplay != null) {
            timeDisplay.setTextColor(CLOCK_COLOR);
            AndroidClockTextView amPm = (AndroidClockTextView)findViewById(R.id.am_pm);
            if (amPm != null) amPm.setTextColor(CLOCK_COLOR);
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mPowerIntentReceiver, filter);
    }

    @Override
    public void onAttachedToWindow() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
              | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
              );
        mSaverView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }


    @Override
    public void onStop() {
        unregisterReceiver(mPowerIntentReceiver);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.post(mMoveSaverRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mMoveSaverRunnable);
    }

    @Override
    public void onUserInteraction() {
        finish();
    }
}

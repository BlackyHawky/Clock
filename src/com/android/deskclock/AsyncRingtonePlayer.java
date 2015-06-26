package com.android.deskclock;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

/**
 * Plays the alarm ringtone. Uses {@link Ringtone} in a separate thread so that this class can be
 * used from the main thread. Consequently, problems controlling the ringtone do not cause ANRs in
 * the main thread of the application.
 */
public class AsyncRingtonePlayer {

    private static final String TAG = "AsyncRingtonePlayer";

    // Message codes used with the ringtone thread.
    private static final int EVENT_PLAY = 1;
    private static final int EVENT_STOP = 2;
    private static final String RINGTONE_URI_KEY = "RINGTONE_URI_KEY";

    /** Handler running on the ringtone thread. */
    private Handler mHandler;

    /** The audio focus manager. Only used by the ringtone thread. */
    private AudioManager mAudioManager;

    /** The current ringtone. Only used by the ringtone thread. */
    private Ringtone mRingtone;

    /** The context. */
    private final Context mContext;

    public AsyncRingtonePlayer(Context context) {
        mContext = context;
    }

    /** Plays the ringtone. */
    public void play(Uri ringtoneUri) {
        LogUtils.d(TAG, "Posting play.");
        postMessage(EVENT_PLAY, ringtoneUri);
    }

    /** Stops playing the ringtone. */
    public void stop() {
        LogUtils.d(TAG, "Posting stop.");
        postMessage(EVENT_STOP, null);
    }

    /**
     * Posts a message to the ringtone-thread handler.
     *
     * @param messageCode The message to post.
     */
    private void postMessage(int messageCode, Uri ringtoneUri) {
        synchronized (this) {
            if (mHandler == null) {
                mHandler = getNewHandler();
            }

            final Message message = mHandler.obtainMessage(messageCode);
            if (ringtoneUri != null) {
                final Bundle bundle = new Bundle();
                bundle.putParcelable(RINGTONE_URI_KEY, ringtoneUri);
                message.setData(bundle);
            }
            message.sendToTarget();
        }
    }

    /**
     * Creates a new ringtone Handler running in its own thread.
     */
    private Handler getNewHandler() {
        final HandlerThread thread = new HandlerThread("ringtone-player");
        thread.start();

        return new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case EVENT_PLAY:
                        final Uri ringtoneUri = msg.getData().getParcelable(RINGTONE_URI_KEY);
                        handlePlay(ringtoneUri);
                        break;
                    case EVENT_STOP:
                        handleStop();
                        break;
                }
            }
        };
    }

    /**
     * Starts the actual playback of the ringtone. Executes on ringtone-thread.
     */
    private void handlePlay(Uri ringtoneUri) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            LogUtils.e(TAG, "Must not be on the main thread!", new IllegalStateException());
        }

        LogUtils.i(TAG, "Play ringtone.");

        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }

        // attempt to fetch the specified ringtone
        mRingtone = RingtoneManager.getRingtone(mContext, ringtoneUri);

        if (mRingtone == null) {
            // fall back to the default ringtone
            final Uri alarmRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            mRingtone = RingtoneManager.getRingtone(mContext, alarmRingtoneUri);
        }

        // if we don't have a ringtone at this point there isn't much recourse
        if (mRingtone == null) {
            LogUtils.i(TAG, "Unable to locate alarm ringtone.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mRingtone.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }

        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mRingtone.play();
    }

    /**
     * Stops the playback of the ringtone. Executes on the ringtone-thread.
     */
    private void handleStop() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            LogUtils.e(TAG, "Must not be on the main thread!", new IllegalStateException());
        }

        LogUtils.i(TAG, "Stop ringtone.");

        if (mRingtone != null && mRingtone.isPlaying()) {
            LogUtils.d(TAG, "Ringtone.stop() invoked.");
            mRingtone.stop();
        }

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
    }
}


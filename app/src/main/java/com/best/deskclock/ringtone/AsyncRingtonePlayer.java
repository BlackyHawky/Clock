// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only

package com.best.deskclock.ringtone;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.best.deskclock.R;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

/**
 * <p>This class controls playback of alarm ringtones using a dedicated background thread.
 * It ensures that all playback operations (start, stop, volume adjustment) are performed off
 * the main thread, avoiding potential ANRs (Application Not Responding errors).</p>
 *
 * <p>Ringtone playback is implemented using {@link MediaPlayer}, which provides greater control
 * over audio playback, such as better error handling, more consistent behavior across devices,
 * and accurate duration retrieval for crescendo effects.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *     <li>Runs all playback logic (including crescendo volume changes) on a background thread.</li>
 *     <li>Supports in-call playback mode with reduced volume to avoid disturbing phone calls.</li>
 *     <li>Handles playback failures gracefully by falling back to a built-in ringtone via
 *     {@link RingtoneUtils#getResourceUri(Context, int)}.</li>
 *     <li>Supports optional crescendo playback by progressively increasing the volume over a defined duration.</li>
 * </ul>
 *
 * <p>Compared to the previous {@link Ringtone}-based implementation, this version using {@link MediaPlayer}
 * allows for more robust playback across a wider range of Android versions and devices.
 * However, it may require permission to read external URIs if the ringtone is not a local resource.</p>
 */
public final class AsyncRingtonePlayer {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AsyncRingtonePlayer");

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;

    // Message codes used with the ringtone thread.
    private static final int EVENT_PLAY = 1;
    private static final int EVENT_STOP = 2;
    private static final int EVENT_VOLUME = 3;
    private static final String RINGTONE_URI_KEY = "RINGTONE_URI_KEY";
    private static final String CRESCENDO_DURATION_KEY = "CRESCENDO_DURATION_KEY";

    private final Context mContext;

    /**
     * Handler running on the ringtone thread.
     */
    private Handler mHandler;

    private MediaPlayerPlaybackDelegate mPlaybackDelegate;

    public AsyncRingtonePlayer(Context context) {
        mContext = context;
    }

    /**
     * @return <code>true</code> iff the device is currently in a telephone call
     */
    private static boolean isInTelephoneCall(AudioManager audioManager) {
          final int audioMode = audioManager.getMode();
          if (SdkUtils.isAtLeastAndroid13()) {
              return audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                      audioMode == AudioManager.MODE_COMMUNICATION_REDIRECT ||
                      audioMode == AudioManager.MODE_CALL_REDIRECT ||
                      audioMode == AudioManager.MODE_CALL_SCREENING ||
                      audioMode == AudioManager.MODE_IN_CALL;
          } else {
              return audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                      audioMode == AudioManager.MODE_IN_CALL;
          }
    }

    /**
     * @return Uri of the ringtone to play when the user is in a telephone call
     */
    private static Uri getInCallRingtoneUri(Context context) {
        return RingtoneUtils.getResourceUri(context, R.raw.alarm_expire);
    }

    /**
     * @return Uri of the ringtone to play when the chosen ringtone fails to play
     */
    private static Uri getFallbackRingtoneUri(Context context) {
        return RingtoneUtils.getResourceUri(context, R.raw.alarm_expire);
    }

    /**
     * @param currentTime current time of the device
     * @param stopTime    time at which the crescendo finishes
     * @param duration    length of time over which the crescendo occurs
     * @return the scalar volume value that produces a linear increase in volume (in decibels)
     */
    private static float computeVolume(long currentTime, long stopTime, long duration) {
        // Compute the percentage of the crescendo that has completed.
        final float elapsedCrescendoTime = stopTime - currentTime;
        final float fractionComplete = 1 - (elapsedCrescendoTime / duration);

        // Use the fraction to compute a target decibel between -40dB (near silent) and 0dB (max).
        final float gain = (fractionComplete * 40) - 40;

        // Convert the target gain (in decibels) into the corresponding volume scalar.
        final float volume = (float) Math.pow(10f, gain / 20f);

        LOGGER.v("Ringtone crescendo %,.2f%% complete (scalar: %f, volume: %f dB)",
                fractionComplete * 100, volume, gain);

        return volume;
    }

    /**
     * Plays the ringtone.
     */
    public void play(Uri ringtoneUri, long crescendoDuration) {
        LOGGER.d("Posting play.");
        postMessage(EVENT_PLAY, ringtoneUri, crescendoDuration, 0);
    }

    /**
     * Stops playing the ringtone.
     */
    public void stop() {
        LOGGER.d("Posting stop.");
        postMessage(EVENT_STOP, null, 0, 0);
    }

    /**
     * Schedules an adjustment of the playback volume 50ms in the future.
     */
    private void scheduleVolumeAdjustment() {
        LOGGER.v("Adjusting volume.");

        // Ensure we never have more than one volume adjustment queued.
        mHandler.removeMessages(EVENT_VOLUME);

        // Queue the next volume adjustment.
        postMessage(EVENT_VOLUME, null, 0, 50);
    }

    /**
     * Posts a message to the ringtone-thread handler.
     *
     * @param messageCode       the message to post
     * @param ringtoneUri       the ringtone in question, if any
     * @param crescendoDuration the length of time, in ms, over which to crescendo the ringtone
     * @param delayMillis       the amount of time to delay sending the message, if any
     */
    private void postMessage(int messageCode, Uri ringtoneUri, long crescendoDuration,
                             long delayMillis) {
        synchronized (this) {
            if (mHandler == null) {
                mHandler = getNewHandler();
            }

            final Message message = mHandler.obtainMessage(messageCode);
            if (ringtoneUri != null) {
                final Bundle bundle = new Bundle();
                bundle.putParcelable(RINGTONE_URI_KEY, ringtoneUri);
                bundle.putLong(CRESCENDO_DURATION_KEY, crescendoDuration);
                message.setData(bundle);
            }

            mHandler.sendMessageDelayed(message, delayMillis);
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
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case EVENT_PLAY:
                        final Bundle data = msg.getData();
                        final Uri ringtoneUri = SdkUtils.isAtLeastAndroid13()
                                ? data.getParcelable(RINGTONE_URI_KEY, Uri.class)
                                : data.getParcelable(RINGTONE_URI_KEY);
                        final long crescendoDuration = data.getLong(CRESCENDO_DURATION_KEY);
                        if (getPlaybackDelegate().play(mContext, ringtoneUri, crescendoDuration)) {
                            scheduleVolumeAdjustment();
                        }
                        break;
                    case EVENT_STOP:
                        getPlaybackDelegate().stop();
                        break;
                    case EVENT_VOLUME:
                        if (getPlaybackDelegate().adjustVolume()) {
                            scheduleVolumeAdjustment();
                        }
                        break;
                }
            }
        };
    }

    /**
     * Check if the executing thread is the one dedicated to controlling the ringtone playback.
     */
    private void checkAsyncRingtonePlayerThread() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            LOGGER.e("Must be on the AsyncRingtonePlayer thread!", new IllegalStateException());
        }
    }

    /**
     * @return the platform-specific playback delegate to use to play the ringtone.
     */
    private PlaybackDelegate getPlaybackDelegate() {
        checkAsyncRingtonePlayerThread();

        if (mPlaybackDelegate == null) {
            mPlaybackDelegate = new MediaPlayerPlaybackDelegate();
        }

        return mPlaybackDelegate;
    }

    /**
     * Loops playback of a ringtone using {@link MediaPlayer}.
     */
    private static class MediaPlayerPlaybackDelegate implements PlaybackDelegate {

        private AudioManager mAudioManager;
        private MediaPlayer mMediaPlayer;

        private long mCrescendoDuration = 0;
        private long mCrescendoStopTime = 0;

        @Override
        public boolean play(Context context, Uri ringtoneUri, long crescendoDuration) {
            mCrescendoDuration = crescendoDuration;

            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            boolean inCall = isInTelephoneCall(mAudioManager);

            if (inCall) {
                ringtoneUri = getInCallRingtoneUri(context);
            }

            mMediaPlayer = RingtoneUtils.createPreparedMediaPlayer(
                    context,
                    ringtoneUri,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    getFallbackRingtoneUri(context)
            );

            if (mMediaPlayer == null) {
                LOGGER.e("Unable to prepare MediaPlayer for ringtone.");
                return false;
            }

            mMediaPlayer.setLooping(true);

            if (inCall) {
                mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
            } else if (crescendoDuration > 0) {
                mMediaPlayer.setVolume(0f, 0f);
                mCrescendoStopTime = Utils.now() + crescendoDuration;
            } else {
                mMediaPlayer.setVolume(1f, 1f);
            }

            requestAudioFocus();
            mMediaPlayer.start();

            return crescendoDuration > 0;
        }

        @Override
        public void stop() {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            mCrescendoDuration = 0;
            mCrescendoStopTime = 0;

            abandonAudioFocus();
        }

        @Override
        public boolean adjustVolume() {
            if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
                return false;
            }

            long currentTime = Utils.now();
            if (currentTime > mCrescendoStopTime) {
                mMediaPlayer.setVolume(1f, 1f);
                return false;
            }

            float volume = computeVolume(currentTime, mCrescendoStopTime, mCrescendoDuration);
            mMediaPlayer.setVolume(volume, volume);

            return true;
        }

        private void requestAudioFocus() {
            if (SdkUtils.isAtLeastAndroid8()) {
                AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .build();

                mAudioManager.requestAudioFocus(focusRequest);
            } else {
                mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        }

        private void abandonAudioFocus() {
            if (mAudioManager != null) {
                if (SdkUtils.isAtLeastAndroid8()) {
                    AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build())
                            .build();

                    mAudioManager.abandonAudioFocusRequest(focusRequest);
                } else {
                    mAudioManager.abandonAudioFocus(null);
                }
            }
        }
    }

}

package com.android.deskclock;

import android.content.Context;
import android.media.AudioAttributes;
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
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * <p>Plays the alarm ringtone. Uses {@link Ringtone} in a separate thread so that this class can be
 * used from the main thread. Consequently, problems controlling the ringtone do not cause ANRs in
 * the main thread of the application.</p>
 *
 * <p>This class also serves a second purpose. It accomplishes alarm ringtone playback using two
 * different mechanisms depending on the underlying platform.</p>
 *
 * <ul>
 *     <li>Prior to the M platform release, ringtone playback is accomplished using
 *     {@link MediaPlayer}. android.permission.READ_EXTERNAL_STORAGE is required to play custom
 *     ringtones located on the SD card using this mechanism. {@link MediaPlayer} allows clients to
 *     adjust the volume of the stream and specify that the stream should be looped.</li>
 *
 *     <li>Starting with the M platform release, ringtone playback is accomplished using
 *     {@link Ringtone}. android.permission.READ_EXTERNAL_STORAGE is <strong>NOT</strong> required
 *     to play custom ringtones located on the SD card using this mechanism. {@link Ringtone} allows
 *     clients to adjust the volume of the stream and specify that the stream should be looped but
 *     those methods are marked @hide in M and thus invoked using reflection. Consequently, revoking
 *     the android.permission.READ_EXTERNAL_STORAGE permission has no effect on playback in M+.</li>
 * </ul>
 */
public class AsyncRingtonePlayer {

    private static final String TAG = "AsyncRingtonePlayer";

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;

    // Message codes used with the ringtone thread.
    private static final int EVENT_PLAY = 1;
    private static final int EVENT_STOP = 2;
    private static final String RINGTONE_URI_KEY = "RINGTONE_URI_KEY";

    /** Handler running on the ringtone thread. */
    private Handler mHandler;

    /** {@link MediaPlayerPlaybackDelegate} on pre M; {@link RingtonePlaybackDelegate} on M+ */
    private PlaybackDelegate mPlaybackDelegate;

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
                        getPlaybackDelegate().play(mContext, ringtoneUri);
                        break;
                    case EVENT_STOP:
                        getPlaybackDelegate().stop(mContext);
                        break;
                }
            }
        };
    }

    /**
     * @return <code>true</code> iff the device is currently in a telephone call
     */
    private static boolean isInTelephoneCall(Context context) {
        final TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getCallState() != TelephonyManager.CALL_STATE_IDLE;
    }

    /**
     * @return Uri of the ringtone to play when the user is in a telephone call
     */
    private static Uri getInCallRingtoneUri(Context context) {
        final String packageName = context.getPackageName();
        return Uri.parse("android.resource://" + packageName + "/" + R.raw.in_call_alarm);
    }

    /**
     * @return Uri of the ringtone to play when the chosen ringtone fails to play
     */
    private static Uri getFallbackRingtoneUri(Context context) {
        final String packageName = context.getPackageName();
        return Uri.parse("android.resource://" + packageName + "/" + R.raw.fallbackring);
    }

    /**
     * @return the platform-specific playback delegate to use to play the ringtone
     */
    private PlaybackDelegate getPlaybackDelegate() {
        if (mPlaybackDelegate == null) {
            if (Utils.isMOrLater()) {
                // Use the newer Ringtone-based playback delegate because it does not require
                // any permissions to read from the SD card. (M+)
                mPlaybackDelegate = new RingtonePlaybackDelegate();
            } else {
                // Fall back to the older MediaPlayer-based playback delegate because it is the only
                // way to force the looping of the ringtone before M. (pre M)
                mPlaybackDelegate = new MediaPlayerPlaybackDelegate();
            }
        }

        return mPlaybackDelegate;
    }

    /**
     * This interface abstracts away the differences between playing ringtones via {@link Ringtone}
     * vs {@link MediaPlayer}.
     */
    private interface PlaybackDelegate {
        void play(Context context, Uri ringtoneUri);
        void stop(Context context);
    }

    /**
     * Loops playback of a ringtone using {@link MediaPlayer}.
     */
    private static class MediaPlayerPlaybackDelegate implements PlaybackDelegate {

        /** The audio focus manager. Only used by the ringtone thread. */
        private AudioManager mAudioManager;

        /** Non-{@code null} while playing a ringtone; {@code null} otherwise. */
        private MediaPlayer mMediaPlayer;

        /**
         * Starts the actual playback of the ringtone. Executes on ringtone-thread.
         */
        @Override
        public void play(final Context context, Uri ringtoneUri) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                LogUtils.e(TAG, "Must not be on the main thread!", new IllegalStateException());
            }

            LogUtils.i(TAG, "Play ringtone via android.media.MediaPlayer.");

            if (mAudioManager == null) {
                mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            }

            Uri alarmNoise = ringtoneUri;
            // Fall back to the default alarm if the database does not have an alarm stored.
            if (alarmNoise == null) {
                alarmNoise = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                LogUtils.v("Using default alarm: " + alarmNoise.toString());
            }

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    LogUtils.e("Error occurred while playing audio. Stopping AlarmKlaxon.");
                    stop(context);
                    return true;
                }
            });

            try {
                // Check if we are in a call. If we are, use the in-call alarm resource at a
                // low volume to not disrupt the call.
                if (isInTelephoneCall(context)) {
                    LogUtils.v("Using the in-call alarm");
                    mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                    alarmNoise = getInCallRingtoneUri(context);
                }

                // If alarmNoise is a custom ringtone on the sd card the app must be granted
                // android.permission.READ_EXTERNAL_STORAGE. Pre-M this is ensured at app
                // installation time. M+, this permission can be revoked by the user any time.
                mMediaPlayer.setDataSource(context, alarmNoise);

                startAlarm(mMediaPlayer);
            } catch (Throwable t) {
                LogUtils.e("Use the fallback ringtone, original was " + alarmNoise, t);
                // The alarmNoise may be on the sd card which could be busy right now.
                // Use the fallback ringtone.
                try {
                    // Must reset the media player to clear the error state.
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(context, getFallbackRingtoneUri(context));
                    startAlarm(mMediaPlayer);
                } catch (Throwable t2) {
                    // At this point we just don't play anything.
                    LogUtils.e("Failed to play fallback ringtone", t2);
                }
            }
        }

        /**
         * Do the common stuff when starting the alarm.
         */
        private void startAlarm(MediaPlayer player) throws IOException {
            // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
            if (mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                if (Utils.isLOrLater()) {
                    player.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }

                player.setAudioStreamType(AudioManager.STREAM_ALARM);
                player.setLooping(true);
                player.prepare();
                mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                player.start();
            }
        }

        /**
         * Stops the playback of the ringtone. Executes on the ringtone-thread.
         */
        @Override
        public void stop(Context context) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                LogUtils.e(TAG, "Must not be on the main thread!", new IllegalStateException());
            }

            LogUtils.i(TAG, "Stop ringtone via android.media.MediaPlayer.");

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mAudioManager.abandonAudioFocus(null);
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }

    /**
     * Loops playback of a ringtone using {@link Ringtone}.
     */
    private static class RingtonePlaybackDelegate implements PlaybackDelegate {

        /** The audio focus manager. Only used by the ringtone thread. */
        private AudioManager mAudioManager;

        /** The current ringtone. Only used by the ringtone thread. */
        private Ringtone mRingtone;

        /** The method to adjust playback volume; cannot be null. */
        private Method mSetVolumeMethod;

        /** The method to adjust playback looping; cannot be null. */
        private Method mSetLoopingMethod;

        private RingtonePlaybackDelegate() {
            try {
                mSetVolumeMethod = Ringtone.class.getDeclaredMethod("setVolume", float.class);
            } catch (NoSuchMethodException nsme) {
                LogUtils.e(TAG, "Unable to locate method: Ringtone.setVolume(float).", nsme);
            }

            try {
                mSetLoopingMethod = Ringtone.class.getDeclaredMethod("setLooping", boolean.class);
            } catch (NoSuchMethodException nsme) {
                LogUtils.e(TAG, "Unable to locate method: Ringtone.setLooping(boolean).", nsme);
            }
        }

        /**
         * Starts the actual playback of the ringtone. Executes on ringtone-thread.
         */
        @Override
        public void play(Context context, Uri ringtoneUri) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                LogUtils.e(TAG, "Must not be on the main thread!", new IllegalStateException());
            }

            LogUtils.i(TAG, "Play ringtone via android.media.Ringtone.");

            if (mAudioManager == null) {
                mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            }

            final boolean inTelephoneCall = isInTelephoneCall(context);
            if (inTelephoneCall) {
                ringtoneUri = getInCallRingtoneUri(context);
            }

            // attempt to fetch the specified ringtone
            mRingtone = RingtoneManager.getRingtone(context, ringtoneUri);

            // Attempt to enable looping the ringtone.
            try {
                mSetLoopingMethod.invoke(mRingtone, true);
            } catch (Exception e) {
                LogUtils.e(TAG, "Unable to turn looping on for android.media.Ringtone", e);

                // Fall back to the default ringtone if looping could not be enabled.
                // (Default alarm ringtone most likely has looping tags set within the .ogg file)
                mRingtone = null;
            }

            if (mRingtone == null) {
                // fall back to the default ringtone
                final Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                mRingtone = RingtoneManager.getRingtone(context, defaultUri);
            }

            // if we don't have a ringtone at this point there isn't much recourse
            if (mRingtone == null) {
                LogUtils.i(TAG, "Unable to locate alarm ringtone.");
                return;
            }

            if (Utils.isLOrLater()) {
                mRingtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }

            // Attempt to adjust the ringtone volume if the user is in a telephone call.
            if (inTelephoneCall) {
                LogUtils.v("Using the in-call alarm");
                try {
                    mSetVolumeMethod.invoke(mRingtone, IN_CALL_VOLUME);
                } catch (Exception e) {
                    LogUtils.e(TAG, "Unable to set in-call volume for android.media.Ringtone", e);
                }
            }

            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mRingtone.play();
        }

        /**
         * Stops the playback of the ringtone. Executes on the ringtone-thread.
         */
        @Override
        public void stop(Context context) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                LogUtils.e(TAG, "Must not be on the main thread!", new IllegalStateException());
            }

            LogUtils.i(TAG, "Stop ringtone via android.media.Ringtone.");

            if (mRingtone != null && mRingtone.isPlaying()) {
                LogUtils.d(TAG, "Ringtone.stop() invoked.");
                mRingtone.stop();
            }

            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(null);
            }
        }
    }
}


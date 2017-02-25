package com.android.deskclock;

import android.annotation.SuppressLint;
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

import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
import static android.media.AudioManager.STREAM_ALARM;

/**
 * <p>This class controls playback of ringtones. Uses {@link Ringtone} or {@link MediaPlayer} in a
 * dedicated thread so that this class can be called from the main thread. Consequently, problems
 * controlling the ringtone do not cause ANRs in the main thread of the application.</p>
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
 *
 * <p>If either the {@link Ringtone} or {@link MediaPlayer} fails to play the requested audio, an
 * {@link #getFallbackRingtoneUri in-app fallback} is used because playing <strong>some</strong>
 * sort of noise is always preferable to remaining silent.</p>
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
    public void play(Uri ringtoneUri, long crescendoDuration) {
        LOGGER.d("Posting play.");
        postMessage(EVENT_PLAY, ringtoneUri, crescendoDuration, 0);
    }

    /** Stops playing the ringtone. */
    public void stop() {
        LOGGER.d("Posting stop.");
        postMessage(EVENT_STOP, null, 0, 0);
    }

    /** Schedules an adjustment of the playback volume 50ms in the future. */
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
     * @param messageCode the message to post
     * @param ringtoneUri the ringtone in question, if any
     * @param crescendoDuration the length of time, in ms, over which to crescendo the ringtone
     * @param delayMillis the amount of time to delay sending the message, if any
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
    @SuppressLint("HandlerLeak")
    private Handler getNewHandler() {
        final HandlerThread thread = new HandlerThread("ringtone-player");
        thread.start();

        return new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case EVENT_PLAY:
                        final Bundle data = msg.getData();
                        final Uri ringtoneUri = data.getParcelable(RINGTONE_URI_KEY);
                        final long crescendoDuration = data.getLong(CRESCENDO_DURATION_KEY);
                        if (getPlaybackDelegate().play(mContext, ringtoneUri, crescendoDuration)) {
                            scheduleVolumeAdjustment();
                        }
                        break;
                    case EVENT_STOP:
                        getPlaybackDelegate().stop(mContext);
                        break;
                    case EVENT_VOLUME:
                        if (getPlaybackDelegate().adjustVolume(mContext)) {
                            scheduleVolumeAdjustment();
                        }
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
        return Utils.getResourceUri(context, R.raw.alarm_expire);
    }

    /**
     * @return Uri of the ringtone to play when the chosen ringtone fails to play
     */
    private static Uri getFallbackRingtoneUri(Context context) {
        return Utils.getResourceUri(context, R.raw.alarm_expire);
    }

    /**
     * Check if the executing thread is the one dedicated to controlling the ringtone playback.
     */
    private void checkAsyncRingtonePlayerThread() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            LOGGER.e("Must be on the AsyncRingtonePlayer thread!",
                    new IllegalStateException());
        }
    }

    /**
     * @param currentTime current time of the device
     * @param stopTime time at which the crescendo finishes
     * @param duration length of time over which the crescendo occurs
     * @return the scalar volume value that produces a linear increase in volume (in decibels)
     */
    private static float computeVolume(long currentTime, long stopTime, long duration) {
        // Compute the percentage of the crescendo that has completed.
        final float elapsedCrescendoTime = stopTime - currentTime;
        final float fractionComplete = 1 - (elapsedCrescendoTime / duration);

        // Use the fraction to compute a target decibel between -40dB (near silent) and 0dB (max).
        final float gain = (fractionComplete * 40) - 40;

        // Convert the target gain (in decibels) into the corresponding volume scalar.
        final float volume = (float) Math.pow(10f, gain/20f);

        LOGGER.v("Ringtone crescendo %,.2f%% complete (scalar: %f, volume: %f dB)",
                fractionComplete * 100, volume, gain);

        return volume;
    }

    /**
     * @return the platform-specific playback delegate to use to play the ringtone
     */
    private PlaybackDelegate getPlaybackDelegate() {
        checkAsyncRingtonePlayerThread();

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
        /**
         * @return {@code true} iff a {@link #adjustVolume volume adjustment} should be scheduled
         */
        boolean play(Context context, Uri ringtoneUri, long crescendoDuration);

        /**
         * Stop any ongoing ringtone playback.
         */
        void stop(Context context);

        /**
         * @return {@code true} iff another volume adjustment should be scheduled
         */
        boolean adjustVolume(Context context);
    }

    /**
     * Loops playback of a ringtone using {@link MediaPlayer}.
     */
    private class MediaPlayerPlaybackDelegate implements PlaybackDelegate {

        /** The audio focus manager. Only used by the ringtone thread. */
        private AudioManager mAudioManager;

        /** Non-{@code null} while playing a ringtone; {@code null} otherwise. */
        private MediaPlayer mMediaPlayer;

        /** The duration over which to increase the volume. */
        private long mCrescendoDuration = 0;

        /** The time at which the crescendo shall cease; 0 if no crescendo is present. */
        private long mCrescendoStopTime = 0;

        /**
         * Starts the actual playback of the ringtone. Executes on ringtone-thread.
         */
        @Override
        public boolean play(final Context context, Uri ringtoneUri, long crescendoDuration) {
            checkAsyncRingtonePlayerThread();
            mCrescendoDuration = crescendoDuration;

            LOGGER.i("Play ringtone via android.media.MediaPlayer.");

            if (mAudioManager == null) {
                mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            }

            final boolean inTelephoneCall = isInTelephoneCall(context);
            Uri alarmNoise = inTelephoneCall ? getInCallRingtoneUri(context) : ringtoneUri;
            // Fall back to the system default alarm if the database does not have an alarm stored.
            if (alarmNoise == null) {
                alarmNoise = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                LOGGER.v("Using default alarm: " + alarmNoise.toString());
            }

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    LOGGER.e("Error occurred while playing audio. Stopping AlarmKlaxon.");
                    stop(context);
                    return true;
                }
            });

            try {
                // If alarmNoise is a custom ringtone on the sd card the app must be granted
                // android.permission.READ_EXTERNAL_STORAGE. Pre-M this is ensured at app
                // installation time. M+, this permission can be revoked by the user any time.
                mMediaPlayer.setDataSource(context, alarmNoise);

                return startPlayback(inTelephoneCall);
            } catch (Throwable t) {
                LOGGER.e("Using the fallback ringtone, could not play " + alarmNoise, t);
                // The alarmNoise may be on the sd card which could be busy right now.
                // Use the fallback ringtone.
                try {
                    // Must reset the media player to clear the error state.
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(context, getFallbackRingtoneUri(context));
                    return startPlayback(inTelephoneCall);
                } catch (Throwable t2) {
                    // At this point we just don't play anything.
                    LOGGER.e("Failed to play fallback ringtone", t2);
                }
            }

            return false;
        }

        /**
         * Prepare the MediaPlayer for playback if the alarm stream is not muted, then start the
         * playback.
         *
         * @param inTelephoneCall {@code true} if there is currently an active telephone call
         * @return {@code true} if a crescendo has started and future volume adjustments are
         *      required to advance the crescendo effect
         */
        private boolean startPlayback(boolean inTelephoneCall)
                throws IOException {
            // Do not play alarms if stream volume is 0 (typically because ringer mode is silent).
            if (mAudioManager.getStreamVolume(STREAM_ALARM) == 0) {
                return false;
            }

            // Indicate the ringtone should be played via the alarm stream.
            if (Utils.isLOrLater()) {
                mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }

            // Check if we are in a call. If we are, use the in-call alarm resource at a low volume
            // to not disrupt the call.
            boolean scheduleVolumeAdjustment = false;
            if (inTelephoneCall) {
                LOGGER.v("Using the in-call alarm");
                mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
            } else if (mCrescendoDuration > 0) {
                mMediaPlayer.setVolume(0, 0);

                // Compute the time at which the crescendo will stop.
                mCrescendoStopTime = Utils.now() + mCrescendoDuration;
                scheduleVolumeAdjustment = true;
            }

            mMediaPlayer.setAudioStreamType(STREAM_ALARM);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepare();
            mAudioManager.requestAudioFocus(null, STREAM_ALARM, AUDIOFOCUS_GAIN_TRANSIENT);
            mMediaPlayer.start();

            return scheduleVolumeAdjustment;
        }

        /**
         * Stops the playback of the ringtone. Executes on the ringtone-thread.
         */
        @Override
        public void stop(Context context) {
            checkAsyncRingtonePlayerThread();

            LOGGER.i("Stop ringtone via android.media.MediaPlayer.");

            mCrescendoDuration = 0;
            mCrescendoStopTime = 0;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(null);
            }
        }

        /**
         * Adjusts the volume of the ringtone being played to create a crescendo effect.
         */
        @Override
        public boolean adjustVolume(Context context) {
            checkAsyncRingtonePlayerThread();

            // If media player is absent or not playing, ignore volume adjustment.
            if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
                mCrescendoDuration = 0;
                mCrescendoStopTime = 0;
                return false;
            }

            // If the crescendo is complete set the volume to the maximum; we're done.
            final long currentTime = Utils.now();
            if (currentTime > mCrescendoStopTime) {
                mCrescendoDuration = 0;
                mCrescendoStopTime = 0;
                mMediaPlayer.setVolume(1, 1);
                return false;
            }

            // The current volume of the crescendo is the percentage of the crescendo completed.
            final float volume = computeVolume(currentTime, mCrescendoStopTime, mCrescendoDuration);
            mMediaPlayer.setVolume(volume, volume);
            LOGGER.i("MediaPlayer volume set to " + volume);

            // Schedule the next volume bump in the crescendo.
            return true;
        }
    }

    /**
     * Loops playback of a ringtone using {@link Ringtone}.
     */
    private class RingtonePlaybackDelegate implements PlaybackDelegate {

        /** The audio focus manager. Only used by the ringtone thread. */
        private AudioManager mAudioManager;

        /** The current ringtone. Only used by the ringtone thread. */
        private Ringtone mRingtone;

        /** The method to adjust playback volume; cannot be null. */
        private Method mSetVolumeMethod;

        /** The method to adjust playback looping; cannot be null. */
        private Method mSetLoopingMethod;

        /** The duration over which to increase the volume. */
        private long mCrescendoDuration = 0;

        /** The time at which the crescendo shall cease; 0 if no crescendo is present. */
        private long mCrescendoStopTime = 0;

        private RingtonePlaybackDelegate() {
            try {
                mSetVolumeMethod = Ringtone.class.getDeclaredMethod("setVolume", float.class);
            } catch (NoSuchMethodException nsme) {
                LOGGER.e("Unable to locate method: Ringtone.setVolume(float).", nsme);
            }

            try {
                mSetLoopingMethod = Ringtone.class.getDeclaredMethod("setLooping", boolean.class);
            } catch (NoSuchMethodException nsme) {
                LOGGER.e("Unable to locate method: Ringtone.setLooping(boolean).", nsme);
            }
        }

        /**
         * Starts the actual playback of the ringtone. Executes on ringtone-thread.
         */
        @Override
        public boolean play(Context context, Uri ringtoneUri, long crescendoDuration) {
            checkAsyncRingtonePlayerThread();
            mCrescendoDuration = crescendoDuration;

            LOGGER.i("Play ringtone via android.media.Ringtone.");

            if (mAudioManager == null) {
                mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            }

            final boolean inTelephoneCall = isInTelephoneCall(context);
            if (inTelephoneCall) {
                ringtoneUri = getInCallRingtoneUri(context);
            }

            // Attempt to fetch the specified ringtone.
            mRingtone = RingtoneManager.getRingtone(context, ringtoneUri);

            if (mRingtone == null) {
                // Fall back to the system default ringtone.
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                mRingtone = RingtoneManager.getRingtone(context, ringtoneUri);
            }

            // Attempt to enable looping the ringtone.
            try {
                mSetLoopingMethod.invoke(mRingtone, true);
            } catch (Exception e) {
                LOGGER.e("Unable to turn looping on for android.media.Ringtone", e);

                // Fall back to the default ringtone if looping could not be enabled.
                // (Default alarm ringtone most likely has looping tags set within the .ogg file)
                mRingtone = null;
            }

            // If no ringtone exists at this point there isn't much recourse.
            if (mRingtone == null) {
                LOGGER.i("Unable to locate alarm ringtone, using internal fallback ringtone.");
                ringtoneUri = getFallbackRingtoneUri(context);
                mRingtone = RingtoneManager.getRingtone(context, ringtoneUri);
            }

            try {
                return startPlayback(inTelephoneCall);
            } catch (Throwable t) {
                LOGGER.e("Using the fallback ringtone, could not play " + ringtoneUri, t);
                // Recover from any/all playback errors by attempting to play the fallback tone.
                mRingtone = RingtoneManager.getRingtone(context, getFallbackRingtoneUri(context));
                try {
                    return startPlayback(inTelephoneCall);
                } catch (Throwable t2) {
                    // At this point we just don't play anything.
                    LOGGER.e("Failed to play fallback ringtone", t2);
                }
            }

            return false;
        }

        /**
         * Prepare the Ringtone for playback, then start the playback.
         *
         * @param inTelephoneCall {@code true} if there is currently an active telephone call
         * @return {@code true} if a crescendo has started and future volume adjustments are
         *      required to advance the crescendo effect
         */
        private boolean startPlayback(boolean inTelephoneCall) {
            // Indicate the ringtone should be played via the alarm stream.
            if (Utils.isLOrLater()) {
                mRingtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }

            // Attempt to adjust the ringtone volume if the user is in a telephone call.
            boolean scheduleVolumeAdjustment = false;
            if (inTelephoneCall) {
                LOGGER.v("Using the in-call alarm");
                setRingtoneVolume(IN_CALL_VOLUME);
            } else if (mCrescendoDuration > 0) {
                setRingtoneVolume(0);

                // Compute the time at which the crescendo will stop.
                mCrescendoStopTime = Utils.now() + mCrescendoDuration;
                scheduleVolumeAdjustment = true;
            }

            mAudioManager.requestAudioFocus(null, STREAM_ALARM, AUDIOFOCUS_GAIN_TRANSIENT);

            mRingtone.play();

            return scheduleVolumeAdjustment;
        }

        /**
         * Sets the volume of the ringtone.
         *
         * @param volume a raw scalar in range 0.0 to 1.0, where 0.0 mutes this player, and 1.0
         *               corresponds to no attenuation being applied.
         */
        private void setRingtoneVolume(float volume) {
            try {
                mSetVolumeMethod.invoke(mRingtone, volume);
            } catch (Exception e) {
                LOGGER.e("Unable to set volume for android.media.Ringtone", e);
            }
        }

        /**
         * Stops the playback of the ringtone. Executes on the ringtone-thread.
         */
        @Override
        public void stop(Context context) {
            checkAsyncRingtonePlayerThread();

            LOGGER.i("Stop ringtone via android.media.Ringtone.");

            mCrescendoDuration = 0;
            mCrescendoStopTime = 0;

            if (mRingtone != null && mRingtone.isPlaying()) {
                LOGGER.d("Ringtone.stop() invoked.");
                mRingtone.stop();
            }

            mRingtone = null;

            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(null);
            }
        }

        /**
         * Adjusts the volume of the ringtone being played to create a crescendo effect.
         */
        @Override
        public boolean adjustVolume(Context context) {
            checkAsyncRingtonePlayerThread();

            // If ringtone is absent or not playing, ignore volume adjustment.
            if (mRingtone == null || !mRingtone.isPlaying()) {
                mCrescendoDuration = 0;
                mCrescendoStopTime = 0;
                return false;
            }

            // If the crescendo is complete set the volume to the maximum; we're done.
            final long currentTime = Utils.now();
            if (currentTime > mCrescendoStopTime) {
                mCrescendoDuration = 0;
                mCrescendoStopTime = 0;
                setRingtoneVolume(1);
                return false;
            }

            final float volume = computeVolume(currentTime, mCrescendoStopTime, mCrescendoDuration);
            setRingtoneVolume(volume);

            // Schedule the next volume bump in the crescendo.
            return true;
        }
    }
}
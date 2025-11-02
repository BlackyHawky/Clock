// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only

package com.best.deskclock.ringtone;

import static com.best.deskclock.utils.RingtoneUtils.IN_CALL_VOLUME;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private final Context mContext;

    /**
     * The executor used to schedule and execute tasks asynchronously in a single thread.
     */
    private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Represents the future of a scheduled volume adjustment task.
     */
    private ScheduledFuture<?> volumeAdjustmentFuture;

    private MediaPlayerPlaybackDelegate mPlaybackDelegate;

    public AsyncRingtonePlayer(Context context) {
        // Use a DirectBoot compatible context if supported
        mContext = Utils.getSafeStorageContext(context);
    }

    /**
     * Plays the ringtone.
     */
    public void play(Uri ringtoneUri, long crescendoDuration) {
        LOGGER.d("Executing play");
        mExecutor.execute(() -> {
            if (getPlaybackDelegate().play(mContext, ringtoneUri, crescendoDuration)) {
                scheduleVolumeAdjustment();
            }
        });    }

    /**
     * Stops playing the ringtone.
     */
    public void stop() {
        LOGGER.d("Executing stop");
        mExecutor.execute(() -> {
            getPlaybackDelegate().stop();
            cancelVolumeAdjustment();
        });
    }

    /**
     * Releases the resources associated with the `AsyncRingtonePlayer` by stopping the execution
     * of its `Executor.
     */
    public void shutdown() {
        if (mExecutor != null && !mExecutor.isShutdown()) {
            LOGGER.d("Releasing AsyncRingtonePlayer resources");
            mExecutor.shutdown();
        } else {
            LOGGER.d("No AsyncRingtonePlayer to release");
        }
    }

    /**
     * Schedules an adjustment of the playback volume 50ms in the future.
     */
    private void scheduleVolumeAdjustment() {
        LOGGER.v("Scheduling volume adjustment");

        cancelVolumeAdjustment();

        volumeAdjustmentFuture = mExecutor.scheduleWithFixedDelay(() -> {
            if (!getPlaybackDelegate().adjustVolume()) {
                cancelVolumeAdjustment();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancels any current task that adjusts the volume, if one is active.
     */
    private void cancelVolumeAdjustment() {
        if (volumeAdjustmentFuture != null && !volumeAdjustmentFuture.isCancelled()) {
            volumeAdjustmentFuture.cancel(true);
            volumeAdjustmentFuture = null;
        }
    }

    /**
     * @return the platform-specific playback delegate to use to play the ringtone.
     */
    private PlaybackDelegate getPlaybackDelegate() {
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
            boolean inCall = RingtoneUtils.isInTelephoneCall(mAudioManager);

            if (inCall) {
                ringtoneUri = RingtoneUtils.getInCallRingtoneUri(context);
            }

            if (RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).equals(ringtoneUri)) {
                ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
            }

            if (ringtoneUri == null || !RingtoneUtils.isRingtoneUriReadable(context, ringtoneUri)) {
                ringtoneUri = RingtoneUtils.getFallbackRingtoneUri(context);
            }

            LOGGER.d("AsyncRingtonePlayer - Playing ringtone URI: " + ringtoneUri);

            mMediaPlayer = RingtoneUtils.createPreparedMediaPlayer(
                    context,
                    ringtoneUri,
                    RingtoneUtils.getFallbackRingtoneUri(context)
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

            float volume = RingtoneUtils.computeVolume(currentTime, mCrescendoStopTime, mCrescendoDuration);
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
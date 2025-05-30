// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static androidx.media3.common.Player.REPEAT_MODE_ONE;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.best.deskclock.R;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;

import java.io.File;
import java.io.InputStream;

/**
 * <p>Controls the playback of alarm ringtones with advanced audio routing and volume management.</p>
 *
 * <p>Playback is powered by {@link ExoPlayer}, offering consistent behavior across Android versions
 * and audio scenarios.</p>
 *
 * <p><strong>Key features:</strong></p>
 * <ul>
 *     <li>Optional crescendo playback by gradually increasing volume over a configurable duration.</li>
 *     <li>In-call support with reduced playback volume.</li>
 *     <li>Automatic fallback to a default ringtone in case of failure.</li>
 *     <li>Automatic routing to Bluetooth devices if connected; otherwise to the speaker.</li>
 *     <li>Volume control:
 *         <ul>
 *             <li>When Bluetooth device is connected, media volume is increased to 70% if too low.</li>
 *             <li>When no Bluetooth device is connected, media volume is muted to isolate the alarm.</li>
 *             <li>Media volume is always restored to its original state on stop or routing change.</li>
 *         </ul>
 *     </li>
 *     <li>Handles dynamic device changes via {@link AudioDeviceCallback}.</li>
 *     <li>Note: Unlike {@link MediaPlayer}, ExoPlayer does not introduce a pause or silence
 *     between repeat cycles; playback is seamless unless a silent segment is manually inserted.</li>
 * </ul>
 */
@OptIn(markerClass = UnstableApi.class)
public final class RingtonePlayer {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("RingtonePlayer");

    private static final float IN_CALL_VOLUME = 0.12f;

    private final Context mContext;

    private ExoPlayer mExoPlayer;

    private final AudioManager mAudioManager;

    private AudioDeviceCallback mAudioDeviceCallback;

    private long mCrescendoDuration = 0;
    private long mCrescendoStopTime = 0;

    private int mOriginalMediaVolume = -1;
    private boolean mMediaVolumeModified = false;

    private final Handler mVolumeHandler = new Handler(Looper.getMainLooper());

    private final Runnable mVolumeAdjustmentRunnable = new Runnable() {
        @Override
        public void run() {
            if (adjustVolume()) {
                mVolumeHandler.postDelayed(this, 50);
            }
        }
    };

    /**
     * Constructs a new {@link RingtonePlayer} instance responsible for managing alarm playback,
     * volume control, and audio routing logic.
     *
     * <p>This initializes the audio manager and registers an {@link AudioDeviceCallback}
     * to dynamically respond to changes in audio output devices, such as Bluetooth connections.</p>
     */
    public RingtonePlayer(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        initAudioDeviceCallback();
    }

    /**
     * Listener attached to the {@link ExoPlayer} instance to handle playback state changes.
     *
     * <p>Once the player enters {@code STATE_READY}, this listener begins playback and sets
     * the volume appropriately based on the current context:</p>
     * <ul>
     *     <li>If a phone call is active, volume is reduced to a safe level (12.5%).</li>
     *     <li>If a crescendo duration is specified, playback starts silently and volume increases gradually.</li>
     *     <li>Otherwise, volume is immediately set to maximum (100%).</li>
     * </ul>
     */
    private final Player.Listener mPlayerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY) {
                mExoPlayer.play();

                if (isInTelephoneCall(mAudioManager)) {
                    mExoPlayer.setVolume(IN_CALL_VOLUME);
                } else if (mCrescendoDuration > 0) {
                    mCrescendoStopTime = System.currentTimeMillis() + mCrescendoDuration;
                    mExoPlayer.setVolume(0f);
                    mVolumeHandler.post(mVolumeAdjustmentRunnable);
                } else {
                    mExoPlayer.setVolume(1f);
                }
            }
        }
    };

    /**
     * Starts playback of the specified alarm ringtone, handling output routing,
     * volume adjustment, and crescendo effects.
     *
     * <p>This method configures an {@link ExoPlayer} instance to play the ringtone with
     * appropriate audio attributes based on the current audio environment.</p>
     *
     * <p><strong>Key behaviors:</strong></p>
     * <ul>
     *     <li>If a Bluetooth device is connected:
     *         <ul>
     *             <li>Audio is routed to the Bluetooth device.</li>
     *             <li>Media volume is raised to 70% of the max volume (if lower).</li>
     *         </ul>
     *     </li>
     *     <li>If no Bluetooth device is connected:
     *         <ul>
     *             <li>Audio is routed to the built-in speaker (if available).</li>
     *             <li>Media volume is temporarily muted (set to 0).</li>
     *         </ul>
     *     </li>
     *     <li>If the device is in a phone call, a fallback ringtone is used at reduced volume.</li>
     *     <li>The ringtone is played in loop mode, with optional crescendo over a configurable duration.</li>
     *     <li>The current media volume is saved and restored later in {@link #stop()}.</li>
     * </ul>
     */
    public void play(Uri ringtoneUri, long crescendoDuration) {
        if (mExoPlayer != null) {
            stop();
        }

        if (mAudioDeviceCallback == null) {
            initAudioDeviceCallback();
        }

        mCrescendoDuration = crescendoDuration;

        // If a Bluetooth device is connected, set the media volume to 70% of its maximum;
        // Otherwise, mute the media volume to so that the alarm can be heard properly
        int maxMediaVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (hasBluetoothDeviceConnected()) {
            mOriginalMediaVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int increasedVolume = (int) (maxMediaVolume * 0.7);

            if (mOriginalMediaVolume < increasedVolume) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, increasedVolume, 0);
                mMediaVolumeModified = true;
            }
        } else {
            mOriginalMediaVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int reducedVolume = 0;

            if (mOriginalMediaVolume > reducedVolume) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, reducedVolume, 0);
                mMediaVolumeModified = true;
            }
        }

        Context safeContext = mContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            safeContext = mContext.createDeviceProtectedStorageContext();
        }

        boolean isBluetooth = hasBluetoothDeviceConnected();
        mExoPlayer = new ExoPlayer.Builder(safeContext)
                .setAudioAttributes(buildAudioAttributes(isBluetooth), isBluetooth)
                .build();

        boolean inCall = isInTelephoneCall(mAudioManager);

        if (inCall) {
            ringtoneUri = getInCallRingtoneUri(safeContext);
        }

        if (RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).equals(ringtoneUri)) {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(safeContext, RingtoneManager.TYPE_ALARM);
        }

        if (ringtoneUri == null || !isUriAccessible(safeContext, ringtoneUri)) {
            ringtoneUri = getFallbackRingtoneUri(safeContext);
        }

        mExoPlayer.setMediaItem(MediaItem.fromUri(ringtoneUri));

        mExoPlayer.setRepeatMode(REPEAT_MODE_ONE);

        AudioDeviceInfo preferredDevice = findBluetoothDevice();

        if (preferredDevice == null) {
            preferredDevice = findSpeakerDevice(mAudioManager);
        }

        if (preferredDevice != null) {
            mExoPlayer.setPreferredAudioDevice(preferredDevice);
            LOGGER.v("Preferred audio device set at start: " + preferredDevice.getType());
        }

        mExoPlayer.addListener(mPlayerListener);

        mExoPlayer.prepare();
    }
    /**
     * Checks if a file is accessible via the specified context. If the phone is in Direct Boot mode,
     * it might not be able to access some files. This function allows using a safe fallback instead of playing a silent alarm.
     */
    private boolean isUriAccessible(Context context, Uri uri) {
        try {
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                return file.exists() && file.canRead();
            } else {
                try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                    return in != null;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Stops ringtone playback, restores previous media volume if it was modified,
     * removes ExoPlayer listeners and callbacks, and unregisters the audio device callback.
     *
     * <p>This method ensures proper cleanup after playback to avoid audio leaks and restores
     * the system state, including volume levels and registered listeners.</p>
     */
    public void stop() {
        if (mExoPlayer != null) {
            mExoPlayer.removeListener(mPlayerListener);
            mExoPlayer.stop();
            mExoPlayer.release();
            mExoPlayer = null;
        }

        mVolumeHandler.removeCallbacks(mVolumeAdjustmentRunnable);

        mCrescendoDuration = 0;
        mCrescendoStopTime = 0;

        // Restore the media volume to its original state
        if (mMediaVolumeModified && mOriginalMediaVolume >= 0) {
            int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (currentVolume != mOriginalMediaVolume) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOriginalMediaVolume, 0);
            }
            mMediaVolumeModified = false;
        }
        mOriginalMediaVolume = -1;

        if (mAudioDeviceCallback != null) {
            mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
            mAudioDeviceCallback = null;
        }
    }

    /**
     * @return {@code true} if volume was adjusted and the crescendo is still ongoing;
     * {@code false} if the crescendo is complete or playback is not active.
     */
    public boolean adjustVolume() {
        if (mExoPlayer == null) {
            return false;
        }

        int state = mExoPlayer.getPlaybackState();
        boolean isPlaying = (state == Player.STATE_READY || state == Player.STATE_BUFFERING)
                && mExoPlayer.getPlayWhenReady();

        if (!isPlaying) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime > mCrescendoStopTime) {
            mExoPlayer.setVolume(1f);
            return false;
        }

        float volume = computeVolume(currentTime, mCrescendoStopTime, mCrescendoDuration);
        mExoPlayer.setVolume(volume);

        return true;
    }

    /**
     * Registers an {@link AudioDeviceCallback} to monitor audio output device changes,
     * such as Bluetooth connections or disconnections.
     *
     * <p>When a Bluetooth device is connected, playback is forced to that device,
     * and media volume is increased for proper audibility. If the Bluetooth device is
     * removed, playback is redirected to the built-in speaker, and the media volume is restored.</p>
     */
    private void initAudioDeviceCallback() {
        if (mAudioDeviceCallback != null) {
            return;
        }

        mAudioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                for (AudioDeviceInfo device : addedDevices) {
                    if (isBluetoothDevice(device)) {
                        LOGGER.v("Bluetooth device connected: forcing playback to it");
                        if (mExoPlayer != null) {
                            mExoPlayer.setPreferredAudioDevice(device);

                            mExoPlayer.setAudioAttributes(buildAudioAttributes(true), true);

                            // Set the media volume to 70% of its maximum if the current volume is
                            // lower than this value so that the alarm can be heard properly
                            if (mAudioManager != null) {
                                int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                int increasedVolume = (int) (maxVolume * 0.7);
                                if (currentVolume < increasedVolume) {
                                    if (mOriginalMediaVolume == -1) {
                                        mOriginalMediaVolume = currentVolume;
                                    }
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, increasedVolume, 0);
                                    mMediaVolumeModified = true;
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                for (AudioDeviceInfo device : removedDevices) {
                    if (isBluetoothDevice(device)) {
                        LOGGER.v("Bluetooth device disconnected: switching back to speaker");
                        if (mExoPlayer != null) {
                            AudioDeviceInfo speaker = findSpeakerDevice(mAudioManager);

                            if (speaker != null) {
                                mExoPlayer.setPreferredAudioDevice(speaker);
                            }

                            mExoPlayer.setAudioAttributes(buildAudioAttributes(false), false);

                            // Restore the media volume to its original state
                            if (mMediaVolumeModified && mOriginalMediaVolume >= 0) {
                                int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                if (currentVolume > mOriginalMediaVolume) {
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOriginalMediaVolume, 0);
                                }
                                mMediaVolumeModified = false;
                            }
                        }
                    }
                }
            }
        };

        mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, new Handler(Looper.getMainLooper()));
    }

    /**
     * Builds and returns {@link AudioAttributes} for the ExoPlayer instance based on output target.
     *
     * <p>If a Bluetooth device is connected, usage is set to {@link C#USAGE_MEDIA} to enable playback.
     * Otherwise, {@link C#USAGE_ALARM} is used to ensure the ringtone plays over system alarm audio.</p>
     */
    private AudioAttributes buildAudioAttributes(boolean bluetoothConnected) {
        return new AudioAttributes.Builder()
                .setUsage(bluetoothConnected ? C.USAGE_MEDIA : C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
                .build();
    }

    /**
     * @return {@code true} if the device is currently in a telephone call. {@code false} otherwise.
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
        float fractionComplete = 1 - Math.max(0f, Math.min(1f, (stopTime - currentTime) / (float) duration));

        // Use the fraction to compute a target decibel between -40dB (near silent) and 0dB (max).
        final float gain = (fractionComplete * 40) - 40;

        // Convert the target gain (in decibels) into the corresponding volume scalar.
        final float volume = (float) Math.pow(10f, gain / 20f);

        LOGGER.v("Ringtone crescendo %,.2f%% complete (scalar: %f, volume: %f dB)",
                fractionComplete * 100, volume, gain);

        return volume;
    }

    /**
     * Searches for and returns the first connected Bluetooth output device.
     *
     * <p>Iterates through all available output audio devices to find one that matches
     * known Bluetooth device types (A2DP or SCO).</p>
     */
    private AudioDeviceInfo findBluetoothDevice() {
        for (AudioDeviceInfo device : mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (isBluetoothDevice(device)) {
                return device;
            }
        }

        return null;
    }

    /**
     * @return {@code true} if the Bluetooth device is of type A2DP or SCO Bluetooth.
     * {@code false} otherwise.
     */
    private static boolean isBluetoothDevice(AudioDeviceInfo device) {
        int type = device.getType();
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
    }

    /**
     * @return {@code true} if a Bluetooth output device is connected. {@code false} otherwise.
     */
    private boolean hasBluetoothDeviceConnected() {
        AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            if (isBluetoothDevice(device)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Searches for and returns the built-in speaker output device.
     */
    private AudioDeviceInfo findSpeakerDevice(AudioManager audioManager) {
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                return device;
            }
        }

        return null;
    }
}

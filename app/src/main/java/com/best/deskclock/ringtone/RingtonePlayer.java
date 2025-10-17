// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static androidx.media3.common.Player.REPEAT_MODE_ONE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE;
import static com.best.deskclock.utils.RingtoneUtils.IN_CALL_VOLUME;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;

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

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private ExoPlayer mExoPlayer;
    private final AudioManager mAudioManager;
    private AudioDeviceCallback mAudioDeviceCallback;

    private android.media.AudioFocusRequest mAudioFocusRequest;

    private boolean mIsAutoRoutingToBluetoothDeviceEnabled;
    private long mCrescendoDuration = 0;
    private long mCrescendoStopTime = 0;
    private int mOriginalMediaVolume = -1;
    private boolean mMediaVolumeModified = false;
    private boolean mIsCrescendoRunningForSystemMediaVolume = false;

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
     * Allows to detect when the preference related to automatic routing to Bluetooth devices changes,
     * in order to dynamically update the behavior of the ringtone player.
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {

                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE.equals(key)) {
                        mIsAutoRoutingToBluetoothDeviceEnabled =
                                SettingsDAO.isAutoRoutingToBluetoothDeviceEnabled(sharedPreferences);

                        if (mIsAutoRoutingToBluetoothDeviceEnabled) {
                            if (mAudioDeviceCallback == null) {
                                initAudioDeviceCallback();
                            }
                        } else {
                            if (mAudioDeviceCallback != null) {
                                mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
                                mAudioDeviceCallback = null;
                            }
                        }
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

        mPrefs = getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        mIsAutoRoutingToBluetoothDeviceEnabled = SettingsDAO.isAutoRoutingToBluetoothDeviceEnabled(mPrefs);
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

                if (RingtoneUtils.isInTelephoneCall(mAudioManager)) {
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
            stopSystemMediaVolumeCrescendo();
            stop();
        }

        if (mIsAutoRoutingToBluetoothDeviceEnabled && mAudioDeviceCallback == null) {
            initAudioDeviceCallback();
        }

        mCrescendoDuration = crescendoDuration;

        boolean isBluetooth = false;
        AudioDeviceInfo preferredDevice = null;

        if (mIsAutoRoutingToBluetoothDeviceEnabled) {
            isBluetooth = RingtoneUtils.hasBluetoothDeviceConnected(mContext, mPrefs);
            preferredDevice = findBluetoothDevice();
        }

        // If a Bluetooth device is connected, set the media volume;
        // Otherwise, mute the media volume to so that the alarm can be heard properly
        mOriginalMediaVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (isBluetooth) {
            if (SettingsDAO.shouldUseCustomMediaVolume(mPrefs)) {
                setMediaVolumeForBluetoothDevices(getBluetoothVolumeFromPrefs());
            }
        } else {
            int reducedVolume = 0;

            if (mOriginalMediaVolume > reducedVolume) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, reducedVolume, 0);
                mMediaVolumeModified = true;
            }
        }

        mExoPlayer = new ExoPlayer.Builder(mContext)
                .setAudioAttributes(buildAudioAttributes(isBluetooth), false)
                .build();

        requestAudioFocus(isBluetooth);

        boolean inCall = RingtoneUtils.isInTelephoneCall(mAudioManager);

        if (inCall) {
            ringtoneUri = RingtoneUtils.getInCallRingtoneUri(mContext);
        }

        if (RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).equals(ringtoneUri)) {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM);
        }

        if (ringtoneUri == null || !RingtoneUtils.isRingtoneUriReadable(mContext, ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getFallbackRingtoneUri(mContext);
        }

        LOGGER.d("RingtonePlayer - Playing ringtone URI: " + ringtoneUri);

        mExoPlayer.setMediaItem(MediaItem.fromUri(ringtoneUri));

        mExoPlayer.setRepeatMode(REPEAT_MODE_ONE);

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
     * Stops ringtone playback, restores previous media volume if it was modified,
     * removes ExoPlayer listeners and callbacks, and unregisters the audio device callback.
     *
     * <p>This method ensures proper cleanup after playback to avoid audio leaks and restores
     * the system state, including volume levels and registered listeners.</p>
     */
    public void stop() {
        stopSystemMediaVolumeCrescendo();

        if (mExoPlayer != null) {
            mExoPlayer.stop();
            mExoPlayer.removeListener(mPlayerListener);
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

        if (mIsAutoRoutingToBluetoothDeviceEnabled && mAudioDeviceCallback != null) {
            mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
            mAudioDeviceCallback = null;
        }

        abandonAudioFocus();
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

        float volume = RingtoneUtils.computeVolume(currentTime, mCrescendoStopTime, mCrescendoDuration);
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
        if (!mIsAutoRoutingToBluetoothDeviceEnabled || mAudioDeviceCallback != null) {
            return;
        }

        mAudioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                for (AudioDeviceInfo device : addedDevices) {
                    if (RingtoneUtils.isBluetoothDevice(device)) {
                        LOGGER.v("Bluetooth device connected: forcing playback to it");
                        if (mExoPlayer != null) {
                            mExoPlayer.setPreferredAudioDevice(device);

                            mExoPlayer.setAudioAttributes(buildAudioAttributes(true), false);

                            requestAudioFocus(true);

                            // Set the media volume for Bluetooth devices
                            if (SettingsDAO.shouldUseCustomMediaVolume(mPrefs) && mAudioManager != null) {
                                setMediaVolumeForBluetoothDevices(getBluetoothVolumeFromPrefs());
                            }
                        }
                    }
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                for (AudioDeviceInfo device : removedDevices) {
                    if (RingtoneUtils.isBluetoothDevice(device)) {
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
     * Adjusts the media volume when a Bluetooth device is connected.
     *
     * <p>If the current system media volume is lower than the target volume, this method starts
     * a smooth crescendo to the target volume to avoid a brief volume spike.
     * Otherwise, it immediately sets the volume to the target level.</p>
     *
     * @param targetVolume The desired media volume level for Bluetooth devices.
     */
    private void setMediaVolumeForBluetoothDevices(int targetVolume) {
        if (targetVolume <= 0 || targetVolume > mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
            return;
        }

        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (mOriginalMediaVolume == -1) {
            mOriginalMediaVolume = currentVolume;
        }

        if (currentVolume < targetVolume) {
            startSystemMediaVolumeCrescendo(targetVolume);
        } else {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
        }

        mMediaVolumeModified = true;
    }

    /**
     * Gradually increases the system media volume from the current level up to the specified target volume.
     *
     * <p>The crescendo lasts approximately 2 seconds and increments the volume in discrete steps.
     * If the target volume is less than or equal to the current volume, no action is taken.</p>
     *
     * @param targetVolume The target volume level to reach at the end of the crescendo.
     */
    private void startSystemMediaVolumeCrescendo(final int targetVolume) {
        final int startVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int volumeDiff = targetVolume - startVolume;
        if (volumeDiff <= 0) {
            return;
        }

        final int steps = Math.abs(volumeDiff);
        final long stepDuration = 2000 / steps;
        mIsCrescendoRunningForSystemMediaVolume = true;

        for (int i = 1; i <= steps; i++) {
            final int newVolume = startVolume + i;
            mVolumeHandler.postDelayed(() -> {
                if (mIsCrescendoRunningForSystemMediaVolume) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
                }
            }, stepDuration * i);
        }
    }

    /**
     * Stops any ongoing system media volume crescendo.
     *
     * <p>After calling this method, scheduled volume increases from a previous
     * call to {@link #startSystemMediaVolumeCrescendo(int)} will be cancelled.</p>
     */
    private void stopSystemMediaVolumeCrescendo() {
        mIsCrescendoRunningForSystemMediaVolume = false;
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
     * Builds the {@link android.media.AudioAttributes} used specifically for requesting audio focus,
     * based on whether a Bluetooth device is connected or not.
     *
     * @param bluetoothConnected {@code true} if a Bluetooth audio device is connected;
     * {@code false} otherwise.
     * @return An {@link android.media.AudioAttributes} instance configured for audio focus requests.
     */
    private android.media.AudioAttributes buildAudioFocusAttributes(boolean bluetoothConnected) {
        int usage = bluetoothConnected
                ? android.media.AudioAttributes.USAGE_MEDIA
                : android.media.AudioAttributes.USAGE_ALARM;

        return new android.media.AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    /**
     * Requests transient audio focus based on the output device (Bluetooth or speaker).
     * Uses the appropriate stream type and audio attributes for each Android API level.
     *
     * @param isBluetooth {@code true} if the output is routed through a Bluetooth device;
     * {@code false} for speaker.
     */
    private void requestAudioFocus(boolean isBluetooth) {
        android.media.AudioAttributes systemAttributes = buildAudioFocusAttributes(isBluetooth);

        if (SdkUtils.isAtLeastAndroid8()) {
            mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(systemAttributes)
                    .build();

            mAudioManager.requestAudioFocus(mAudioFocusRequest);
        } else {
            int streamType = isBluetooth ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_ALARM;
            mAudioManager.requestAudioFocus(null, streamType, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    /**
     * Abandons the previously acquired audio focus.
     */
    private void abandonAudioFocus() {
        if (SdkUtils.isAtLeastAndroid8() && mAudioFocusRequest != null) {
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
            mAudioFocusRequest = null;
        } else {
            mAudioManager.abandonAudioFocus(null);
        }
    }

    /**
     * @return the volume value when a Bluetooth device is connected.
     */
    private int getBluetoothVolumeFromPrefs() {
        int userVolume = SettingsDAO.getBluetoothVolumeValue(mPrefs);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return (int) (maxVolume * (userVolume / 100f));
    }

    /**
     * Searches for and returns the first connected Bluetooth output device.
     *
     * <p>Iterates through all available output audio devices to find one that matches
     * known Bluetooth device types (A2DP or SCO).</p>
     */
    private AudioDeviceInfo findBluetoothDevice() {
        if (!mIsAutoRoutingToBluetoothDeviceEnabled) {
            return null;
        }

        for (AudioDeviceInfo device : mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (RingtoneUtils.isBluetoothDevice(device)) {
                return device;
            }
        }

        return null;
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

    /**
     * Unregisters the preference change listener used to monitor changes
     * in user settings related to audio routing.
     *
     * <p>This should be called when the ringtone player is no longer needed
     * or when the surrounding component (e.g. Activity or Service) is being stopped,
     * to avoid memory leaks and unnecessary listener callbacks.</p>
     */
    public void stopListeningToPreferences() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
    }
}

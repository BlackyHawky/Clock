// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.net.Uri;

/**
 * This interface abstracts away the differences between playing ringtones via {@link Ringtone}
 * vs {@link MediaPlayer}.
 */
public interface PlaybackDelegate {

    /**
     * Start playing the ringtone.
     *
     * @return {@code true} if a {@link #adjustVolume volume adjustment} should be scheduled.
     * {@code false} otherwise.
     */
    boolean play(Context context, Uri ringtoneUri, long crescendoDuration);

    /**
     * Stop any ongoing ringtone playback.
     */
    void stop();

    /**
     * Dynamically adjusts the volume (e.g. to create a crescendo effect).
     *
     * @return {@code true} if another volume adjustment should be scheduled.
     * {@code false} otherwise.
     */
    boolean adjustVolume();
}

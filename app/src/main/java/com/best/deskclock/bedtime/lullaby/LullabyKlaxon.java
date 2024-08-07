package com.best.deskclock.bedtime.lullaby;

import android.media.AudioManager;

import androidx.annotation.DrawableRes;

import com.best.deskclock.R;
import com.best.music.AbstractMediaKlaxon;

public class LullabyKlaxon extends AbstractMediaKlaxon {

    private static LullabyKlaxon instance = null;

    public static synchronized LullabyKlaxon getKlaxon() {
        if (instance == null) {
            instance = new LullabyKlaxon();
        }
        return instance;
    }
    @DrawableRes
    public int toggle() {
        if (mPlayer.isPlaying()) {
            pause();
            return R.drawable.ic_fab_play;
        } else {
            play();
            return R.drawable.ic_fab_pause;
        }
    }

    @Override
    protected int getStream() {
        return AudioManager.STREAM_MUSIC;
    }
}

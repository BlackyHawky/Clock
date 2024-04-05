package com.best.deskclock.bedtime;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

import androidx.annotation.DrawableRes;

import com.best.deskclock.R;
import com.best.deskclock.ringtone.BaseKlaxon;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;

public class SleepKlaxon {
    @DrawableRes
    static int toggle(Context context, Uri uri) {
        if (BaseKlaxon.sStarted) {
            BaseKlaxon.stop(context);
            return R.drawable.ic_fab_play;
        } else {
            RingtonePreviewKlaxon.start(context, uri, AudioManager.STREAM_MUSIC);
            return R.drawable.ic_fab_pause;
        }
    }
}

package com.best.deskclock.bedtime.lullaby;

import android.net.Uri;

import com.best.deskclock.R;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.music.AbstractMediaKlaxon;
import com.best.music.AbstractPlayerService;

public class LullabyService extends AbstractPlayerService {
    @Override
    protected AbstractMediaKlaxon getKlaxon() {
        return LullabyKlaxon.getKlaxon();
    }

    @Override
    protected Uri getMusic() {
        DataSaver saver = DataSaver.getInstance(this);
        saver.restore();
        return saver.sleepUri;
    }

    @Override
    protected int stopString() {
        return R.string.stop_lullaby;
    }
}

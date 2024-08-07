package com.best.music;

import android.content.Context;
import android.net.Uri;

public interface Klaxon {
    public void start(Context context, Uri uri);
    public void stop ();
    public void pause();
    public void play();
    public boolean isPlaying();
}

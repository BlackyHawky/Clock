package com.best.music;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import com.best.deskclock.LogUtils;

import java.util.Collections;
import java.util.List;

public abstract class AbstractMediaKlaxon implements Klaxon {
    protected MediaPlayer mPlayer = null;
    private int trackIndex = 0;
    private List<Uri> tracks = null;
    private Uri currentTrack = null;
    private AudioManager audioManager;
    @Override
    public void start(Context context, Uri uri) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        boolean playlist = false;
        tracks = null;
        if (MediaUtils.isLocalTrackUri(uri.toString()) || MediaUtils.isAlarmUriValid(context, uri)) {
            playTrack(context, uri, true);
        } else if (MediaUtils.isLocalAlbumUri(uri.toString())) {
            playlist = true;
            tracks = MediaUtils.getAlbumSongs(context, uri);
        } else if (MediaUtils.isLocalArtistUri(uri.toString())) {
            playlist = true;
            tracks = MediaUtils.getArtistSongs(context, uri);
        } else if (MediaUtils.isLocalPlaylistUri(uri.toString())) {
            playlist = true;
            tracks = MediaUtils.getPlaylistSongs(context, uri);
        } else {
            playTrack(context, uri, true);
        }

        if (playlist && tracks != null && tracks.size() != 0) {
            shuffle(); //TODO: add option to let the children shuffle themselves
            trackIndex = 0;
            playTrack(context, tracks.get(0), false);
        }
    }

    public void shuffle() {
        if (tracks != null && !tracks.isEmpty()) {
            Collections.shuffle(tracks);
            trackIndex = tracks.indexOf(currentTrack);
        }
    }

    protected abstract int getStream();

    protected void playTrack(Context context, Uri uri, boolean looping) {
        currentTrack = uri;
        if (mPlayer != null) {
            mPlayer.reset();
        }
        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(getStream());
        audioManager.requestAudioFocus(null, getStream(), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        try {
            mPlayer.setDataSource(context, uri);
        } catch (Exception e) {
            LogUtils.e("AbstractMediaKlaxon.playTrack", e);
        }
        if (looping) {
            mPlayer.setLooping(true);
        } else if (tracks != null && tracks.size() != 0){
            mPlayer.setOnCompletionListener(mp -> {
                trackIndex++;
                if (trackIndex >= tracks.size()) {
                    trackIndex = 0;
                }
                playTrack(context, tracks.get(trackIndex), false);
            });
        }
        play();
    }

    @Override
    public void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    @Override
    public void play() {
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mPlayer.prepareAsync();
    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public MediaPlayer getmPlayer() {
        return mPlayer;
    }
}

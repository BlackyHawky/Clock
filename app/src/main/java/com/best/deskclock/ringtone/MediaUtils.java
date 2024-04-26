/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.best.deskclock.ringtone;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.best.deskclock.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaUtils {

    public static boolean isSpotifyUri(String uri) {
        return uri.startsWith("spotify:");
    }

    public static List<Uri> getRandomMusicFiles(Context context, int size) {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "RANDOM() LIMIT " + size);

        List<Uri> mediaFiles = new ArrayList<>(size);
        while (c.moveToNext()) {
            Uri mediaFile = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    String.valueOf(c.getLong(0)));
            mediaFiles.add(mediaFile);
        }
        c.close();
        return mediaFiles;
    }

    public static List<Uri> getAlbumSongs(Context context, Uri album) {
        String albumId = album.getLastPathSegment();
        String selection = MediaStore.Audio.Media.ALBUM_ID + " = " + Long.valueOf(albumId).longValue();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);

        List<Uri> albumFiles = new ArrayList<>();
        while (c.moveToNext()) {
            Uri mediaFile = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    String.valueOf(c.getLong(0)));
            albumFiles.add(mediaFile);
        }
        c.close();
        return albumFiles;
    }

    public static boolean checkAlbumExists(Context context, Uri album) {
        String albumId = album.getLastPathSegment();
        String selection = MediaStore.Audio.Media.ALBUM_ID + " = " + Long.valueOf(albumId).longValue();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);

        return c.getCount() != 0;
    }

    public static List<Uri> getArtistSongs(Context context, Uri artist) {
        String artistId = artist.getLastPathSegment();
        String selection = MediaStore.Audio.Media.ARTIST_ID + " = " + Long.valueOf(artistId).longValue();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);

        List<Uri> albumFiles = new ArrayList<>();
        while (c.moveToNext()) {
            Uri mediaFile = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    String.valueOf(c.getLong(0)));
            albumFiles.add(mediaFile);
        }
        c.close();
        return albumFiles;
    }

    public static Uri getArtistArtwork(Context context, Uri artist) {
        String artistId = artist.getLastPathSegment();
        String selection = MediaStore.Audio.Media.ARTIST_ID + " = " + Long.valueOf(artistId).longValue();

        String[] projection = {
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);

        Uri imageUri = null;
        if (c.moveToNext()) {
            Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");
            imageUri =  ContentUris.withAppendedId(sArtworkUri, c.getLong(0));
        }
        c.close();
        return imageUri;
    }

    public static Uri getSongArtwork(Context context, Uri song) {
        String songId = song.getLastPathSegment();
        String selection = MediaStore.Audio.Media._ID + " = " + Long.valueOf(songId);

        String[] projection = {
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);

        Uri imageUri = null;
        if (c.moveToNext()) {
            Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");
            imageUri = ContentUris.withAppendedId(sArtworkUri, c.getLong(0));
        }
        c.close();
        return imageUri;
    }

    public static Uri getAlbumArtwork(Uri album) {
        return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                Long.valueOf(album.getLastPathSegment()));
    }

    public static boolean checkArtistExists(Context context, Uri artist) {
        String artistId = artist.getLastPathSegment();
        String selection = MediaStore.Audio.Media.ARTIST_ID + " = " + Long.valueOf(artistId).longValue();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);
        return c.getCount() != 0;
    }

    public static List<Uri> getPlaylistSongs(Context context, Uri playlist) {
        String playlistId = playlist.getLastPathSegment();
        String selection = MediaStore.Audio.Playlists._ID + " = " + Long.valueOf(playlistId).longValue();

        String[] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        String[] projectionMembers = {
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members._ID
        };
        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        List<Uri> playlistFiles = new ArrayList<>();

        while (c.moveToNext()) {
            long id = c.getLong(0);
            Cursor c1 = context.getContentResolver().query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", id),
                    projectionMembers,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0 ",
                    null,
                    null);
            while (c1.moveToNext()) {
                Uri mediaFile = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(c1.getLong(0)));
                playlistFiles.add(mediaFile);
            }
            c1.close();
        }
        c.close();
        return playlistFiles;
    }

    public static boolean checkPlaylistExists(Context context, Uri playlist) {
        String playlistId = playlist.getLastPathSegment();
        String selection = MediaStore.Audio.Playlists._ID + " = " + Long.valueOf(playlistId).longValue();

        String[] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Playlists.NAME);
        return c.getCount() != 0;
    }

    public static String resolveTrackDesc(Context context, Uri track) {
        String trackId = track.getLastPathSegment();
        String selection = MediaStore.Audio.Media._ID + " = " + Long.valueOf(trackId).longValue();

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        String desc = null;
        while (c.moveToNext()) {
            desc = c.getString(0);
            desc = c.getString(1).equals(MediaStore.UNKNOWN_STRING) ? desc + " - " + c.getString(1) : desc;
            break;
        }
        c.close();
        return desc;
    }

    public static String resolveAlbum(Context context, Uri track) {
        String trackId = track.getLastPathSegment();
        String selection = MediaStore.Audio.Albums._ID + " = " + Long.valueOf(trackId).longValue();

        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        String title = null;
        while (c.moveToNext()) {
            title = c.getString(1);
            break;
        }
        c.close();
        return title;
    }

    public static String resolveAlbumDesc(Context context, Uri track) {
        String trackId = track.getLastPathSegment();
        String selection = MediaStore.Audio.Albums._ID + " = " + Long.valueOf(trackId).longValue();

        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ARTIST
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        String title = null;
        while (c.moveToNext()) {
            title = c.getString(1);
            break;
        }
        c.close();
        return title;
    }

    public static String resolveArtist(Context context, Uri track) {
        String trackId = track.getLastPathSegment();
        String selection = MediaStore.Audio.Artists._ID + " = " + Long.valueOf(trackId).longValue();

        String[] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        String title = null;
        while (c.moveToNext()) {
            title = c.getString(1);
            break;
        }
        c.close();
        return title;
    }

    public static String getRandomUriString() {
        return "random:/";
    }

    public static boolean isRandomUri(String uri) {
        return uri.startsWith(getRandomUriString());
    }

    public static int resolveLocalUriImage(final String uri) {
        if (isLocalAlbumUri(uri)) {
            return R.drawable.ic_media_albums;
        } else if (isLocalArtistUri(uri)) {
            return R.drawable.ic_media_artist;
        } else if (isLocalTrackUri(uri)) {
            return R.drawable.ic_media_song;
        } else if ((isStorageUri(uri)&&isStorageFileUri(uri))||isLocalPlaylistUri(uri)) {
            return R.drawable.ic_media_playlists;
        }
        return R.drawable.ic_ringtone;
    }

    public static boolean isLocalAlbumUri(String uri) {
        return uri.contains("/audio/albums/");
    }

    public static boolean isLocalArtistUri(String uri) {
        return uri.contains("/audio/artists/");
    }

    public static boolean isLocalTrackUri(String uri) {
        return uri.contains("/audio/media/");
    }

    public static boolean isLocalPlaylistUri(String uri) {
        return uri.contains("/audio/playlists/");
    }

    private static boolean isLocalMediaUri(String uri) {
        return isLocalAlbumUri(uri) || isLocalArtistUri(uri) || isLocalTrackUri(uri)
                || isStorageUri(uri) || isLocalPlaylistUri(uri);
    }

    public static boolean isLocalPlaylistType(final String uri) {
        if (isLocalAlbumUri(uri)) {
            return true;
        } else if (isLocalArtistUri(uri)) {
            return true;
        } else if (isStorageUri(uri)) {
            return true;
        } else if (isLocalPlaylistUri(uri)) {
            return true;
        }
        return false;
    }

    public static boolean isStorageUri(String uri) {
        return uri.startsWith("file:/");
    }

    public static boolean isStorageFileUri(String uri) {
        if (isStorageUri(uri)) {
            String path = Uri.parse(uri).getPath();
            File f = new File(path);
            if (f.isFile() && f.exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAlarmUriValid(Context context, Uri uri) {
        final RingtoneManager rm = new RingtoneManager(context);
        rm.setType(RingtoneManager.TYPE_ALL);
        return rm.getRingtonePosition(uri) != -1;
    }


    public static String getMediaTitle(Context context, Uri uri) {
        if (isLocalAlbumUri(uri.toString())) {
            return resolveAlbum(context, uri);
        } else if (isLocalArtistUri(uri.toString())) {
            return resolveArtist(context, uri);
        }
        return context.getString(R.string.unknown_ringtone_title);
    }

    public static boolean isValidAudioFile(String baseName) {
        // check if audio
        int idx = baseName.lastIndexOf(".");
        if (idx != -1) {
            String ext = baseName.substring(idx + 1);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mime != null && (mime.contains("audio") || mime.contains("ogg"))) {
                return true;
            }
        }
        return false;
    }
}
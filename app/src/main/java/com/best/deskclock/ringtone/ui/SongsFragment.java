package com.best.deskclock.ringtone.ui;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.best.deskclock.R;
import com.best.deskclock.ringtone.RingtoneItem;

import java.util.ArrayList;

public class SongsFragment extends BasePickerFragment {

    ArrayList<RingtoneItem> getList(Context context) {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);

        ArrayList<RingtoneItem> list = new ArrayList<RingtoneItem>();

        while (c.moveToNext()) {
            Uri track = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    String.valueOf(c.getLong(0)));
            String trackTitle = c.getString(1);
            String artistName = c.getString(2);
            String albumName = c.getString(3);
            Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");
            Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, c.getLong(4));

            RingtoneItem item = new RingtoneItem();
            item.title = trackTitle;
            if (!artistName.equals(MediaStore.UNKNOWN_STRING)) {
                item.desc = artistName + " - " + albumName;
            } else {
                item.desc = albumName;
            }
            item.uri = track.toString();
            item.iconId = R.drawable.ic_media_song;
            item.imageUri = albumArtUri.toString();
            list.add(item);
        }
        c.close();
        return list;
    }
}

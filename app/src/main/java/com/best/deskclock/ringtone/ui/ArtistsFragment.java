package com.best.deskclock.ringtone.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.best.deskclock.R;
import com.best.deskclock.ringtone.MediaUtils;
import com.best.deskclock.ringtone.RingtoneItem;

import java.util.ArrayList;

public class ArtistsFragment extends BasePickerFragment {
    ArrayList<RingtoneItem> getList(Context context) {
        String[] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST
        };
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST);

        ArrayList<RingtoneItem> list = new ArrayList<RingtoneItem>();

        while (cursor.moveToNext()) {
            Uri artist = Uri.withAppendedPath(
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    String.valueOf(cursor.getLong(0)));
            String artistName = cursor.getString(1);
            Uri artistArtUri = MediaUtils.getArtistArtwork(context, artist);
            if (!artistName.equals(MediaStore.UNKNOWN_STRING)) {
                RingtoneItem item = new RingtoneItem();
                item.title = artistName;
                item.uri = artist.toString();
                item.iconId = R.drawable.ic_media_artist;
                item.imageUri = artistArtUri.toString();
                list.add(item);
            }
        }
        cursor.close();
        return list;
    }
}

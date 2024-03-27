/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.best.deskclock.ringtone.ui;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.best.deskclock.R;
import com.best.deskclock.ringtone.RingtoneItem;

import java.util.ArrayList;

public class AlbumsFragment extends BasePickerFragment {

    ArrayList<RingtoneItem> getList(Context context) {
        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST
        };

        ArrayList<RingtoneItem> list = new ArrayList<RingtoneItem>();

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Albums.ALBUM);

        while (cursor.moveToNext()) {
            Uri album = Uri.withAppendedPath(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    String.valueOf(cursor.getLong(0)));
            String title = cursor.getString(1);
            String artist = cursor.getString(2);
            Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");
            Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, cursor.getLong(0));

            RingtoneItem item = new RingtoneItem();
            item.title = title;
            item.uri = album.toString();
            item.desc = artist;
            item.iconId = R.drawable.ic_media_albums;
            item.imageUri = albumArtUri.toString();
            list.add(item);
        }
        cursor.close();
        return list;
    }
}

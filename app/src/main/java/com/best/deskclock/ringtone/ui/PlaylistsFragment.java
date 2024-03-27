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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.best.deskclock.R;
import com.best.deskclock.ringtone.RingtoneItem;

import java.util.ArrayList;

public class PlaylistsFragment extends BasePickerFragment {

    ArrayList<RingtoneItem> getList(Context context) {
        String[] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Playlists.NAME);

        ArrayList<RingtoneItem> list = new ArrayList<RingtoneItem>();

        while (cursor.moveToNext()) {
            Uri artist = Uri.withAppendedPath(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    String.valueOf(cursor.getLong(0)));
            String artistName = cursor.getString(1);
            if (!artistName.equals(MediaStore.UNKNOWN_STRING)) {
                RingtoneItem item = new RingtoneItem();
                item.title = artistName;
                item.uri = artist.toString();
                item.iconId = R.drawable.ic_media_playlists;
                list.add(item);
            }
        }
        cursor.close();
        return list;
    }
}

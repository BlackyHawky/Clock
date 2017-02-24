/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.deskclock.data;

import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class encapsulates the transfer of data between {@link CustomRingtone} domain objects and
 * their permanent storage in {@link SharedPreferences}.
 */
final class CustomRingtoneDAO {

    /** Key to a preference that stores the set of all custom ringtone ids. */
    private static final String RINGTONE_IDS = "ringtone_ids";

    /** Key to a preference that stores the next unused ringtone id. */
    private static final String NEXT_RINGTONE_ID = "next_ringtone_id";

    /** Prefix for a key to a preference that stores the URI associated with the ringtone id. */
    private static final String RINGTONE_URI = "ringtone_uri_";

    /** Prefix for a key to a preference that stores the title associated with the ringtone id. */
    private static final String RINGTONE_TITLE = "ringtone_title_";

    private CustomRingtoneDAO() {}

    /**
     * @param uri points to an audio file located on the file system
     * @param title the title of the audio content at the given {@code uri}
     * @return the newly added custom ringtone
     */
    static CustomRingtone addCustomRingtone(SharedPreferences prefs, Uri uri, String title) {
        final long id = prefs.getLong(NEXT_RINGTONE_ID, 0);
        final Set<String> ids = getRingtoneIds(prefs);
        ids.add(String.valueOf(id));

        prefs.edit()
                .putString(RINGTONE_URI + id, uri.toString())
                .putString(RINGTONE_TITLE + id, title)
                .putLong(NEXT_RINGTONE_ID, id + 1)
                .putStringSet(RINGTONE_IDS, ids)
                .apply();

        return new CustomRingtone(id, uri, title, true);
    }

    /**
     * @param id identifies the ringtone to be removed
     */
    static void removeCustomRingtone(SharedPreferences prefs, long id) {
        final Set<String> ids = getRingtoneIds(prefs);
        ids.remove(String.valueOf(id));

        final SharedPreferences.Editor editor = prefs.edit();
        editor.remove(RINGTONE_URI + id);
        editor.remove(RINGTONE_TITLE + id);
        if (ids.isEmpty()) {
            editor.remove(RINGTONE_IDS);
            editor.remove(NEXT_RINGTONE_ID);
        } else {
            editor.putStringSet(RINGTONE_IDS, ids);
        }
        editor.apply();
    }

    /**
     * @return a list of all known custom ringtones
     */
    static List<CustomRingtone> getCustomRingtones(SharedPreferences prefs) {
        final Set<String> ids = prefs.getStringSet(RINGTONE_IDS, Collections.<String>emptySet());
        final List<CustomRingtone> ringtones = new ArrayList<>(ids.size());

        for (String id : ids) {
            final long idLong = Long.parseLong(id);
            final Uri uri = Uri.parse(prefs.getString(RINGTONE_URI + id, null));
            final String title = prefs.getString(RINGTONE_TITLE + id, null);
            ringtones.add(new CustomRingtone(idLong, uri, title, true));
        }

        return ringtones;
    }

    private static Set<String> getRingtoneIds(SharedPreferences prefs) {
        return new HashSet<>(prefs.getStringSet(RINGTONE_IDS, Collections.<String>emptySet()));
    }
}
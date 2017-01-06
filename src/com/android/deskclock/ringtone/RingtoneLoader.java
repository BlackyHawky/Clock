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

package com.android.deskclock.ringtone;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.media.RingtoneManager;
import android.net.Uri;

import com.android.deskclock.ItemAdapter;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.data.CustomRingtone;
import com.android.deskclock.data.DataModel;

import java.util.ArrayList;
import java.util.List;

import static android.media.AudioManager.STREAM_ALARM;
import static com.android.deskclock.Utils.RINGTONE_SILENT;

/**
 * Assembles the list of ItemHolders that back the RecyclerView used to choose a ringtone.
 */
class RingtoneLoader extends AsyncTaskLoader<List<ItemAdapter.ItemHolder<Uri>>> {

    private final Uri mDefaultRingtoneUri;
    private final String mDefaultRingtoneTitle;
    private List<CustomRingtone> mCustomRingtones;

    RingtoneLoader(Context context, Uri defaultRingtoneUri, String defaultRingtoneTitle) {
        super(context);
        mDefaultRingtoneUri = defaultRingtoneUri;
        mDefaultRingtoneTitle = defaultRingtoneTitle;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        mCustomRingtones = DataModel.getDataModel().getCustomRingtones();
        forceLoad();
    }

    @Override
    public List<ItemAdapter.ItemHolder<Uri>> loadInBackground() {
        // Prime the ringtone title cache for later access.
        DataModel.getDataModel().loadRingtoneTitles();
        DataModel.getDataModel().loadRingtonePermissions();

        // Fetch the standard system ringtones.
        final RingtoneManager ringtoneManager = new RingtoneManager(getContext());
        ringtoneManager.setType(STREAM_ALARM);

        Cursor systemRingtoneCursor;
        try {
            systemRingtoneCursor = ringtoneManager.getCursor();
        } catch (Exception e) {
            LogUtils.e("Could not get system ringtone cursor");
            systemRingtoneCursor = new MatrixCursor(new String[] {});
        }
        final int systemRingtoneCount = systemRingtoneCursor.getCount();
        // item count = # system ringtones + # custom ringtones + 2 headers + Add new music item
        final int itemCount = systemRingtoneCount + mCustomRingtones.size() + 3;

        final List<ItemAdapter.ItemHolder<Uri>> itemHolders = new ArrayList<>(itemCount);

        // Add the item holder for the Music heading.
        itemHolders.add(new HeaderHolder(R.string.your_sounds));

        // Add an item holder for each custom ringtone and also cache a pretty name.
        for (CustomRingtone ringtone : mCustomRingtones) {
            itemHolders.add(new CustomRingtoneHolder(ringtone));
        }

        // Add an item holder for the "Add new" music ringtone.
        itemHolders.add(new AddCustomRingtoneHolder());

        // Add an item holder for the Ringtones heading.
        itemHolders.add(new HeaderHolder(R.string.device_sounds));

        // Add an item holder for the silent ringtone.
        itemHolders.add(new SystemRingtoneHolder(RINGTONE_SILENT, null));

        // Add an item holder for the system default alarm sound.
        itemHolders.add(new SystemRingtoneHolder(mDefaultRingtoneUri, mDefaultRingtoneTitle));

        // Add an item holder for each system ringtone.
        for (int i = 0; i < systemRingtoneCount; i++) {
            final Uri ringtoneUri = ringtoneManager.getRingtoneUri(i);
            itemHolders.add(new SystemRingtoneHolder(ringtoneUri, null));
        }

        return itemHolders;
    }

    @Override
    protected void onReset() {
        super.onReset();
        mCustomRingtones = null;
    }
}
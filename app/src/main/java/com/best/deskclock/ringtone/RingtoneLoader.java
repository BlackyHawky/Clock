/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static android.media.AudioManager.STREAM_ALARM;

import static com.best.deskclock.utils.RingtoneUtils.RANDOM_CUSTOM_RINGTONE;
import static com.best.deskclock.utils.RingtoneUtils.RANDOM_RINGTONE;
import static com.best.deskclock.utils.RingtoneUtils.RINGTONE_SILENT;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.loader.content.AsyncTaskLoader;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;
import com.best.deskclock.data.CustomRingtone;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

        try (Cursor systemRingtoneCursor = ringtoneManager.getCursor()) {
            final int systemRingtoneCount = systemRingtoneCursor.getCount();
            // item count = # system ringtones + # custom ringtones + 2 headers + Add new music item
            final int itemCount = systemRingtoneCount + mCustomRingtones.size() + 3;

            final List<ItemAdapter.ItemHolder<Uri>> itemHolders = new ArrayList<>(itemCount);

            // Add the item holder for the Music heading.
            itemHolders.add(new HeaderHolder(R.string.your_sounds));

            // Add the item holder for the random custom ringtones only if at least 2 files can be read.
            int readableCount = 0;
            Iterator<CustomRingtone> iterator = mCustomRingtones.iterator();
            while (iterator.hasNext() && readableCount < 2) {
                CustomRingtone ringtone = iterator.next();
                if (RingtoneUtils.isRingtoneUriReadable(getContext(), ringtone.getUri())) {
                    readableCount++;
                }
            }

            if (readableCount >= 2) {
                itemHolders.add(new SystemRingtoneHolder(RANDOM_CUSTOM_RINGTONE, null));
            }

            // Add an item holder for each custom ringtone and also cache a pretty name.
            for (CustomRingtone ringtone : mCustomRingtones) {
                itemHolders.add(new CustomRingtoneHolder(ringtone));
            }

            // Add an item holder for the "Add new" music ringtone.
            itemHolders.add(new AddCustomRingtoneHolder());

            // Add an item holder for the Ringtones heading.
            itemHolders.add(new HeaderHolder(R.string.device_sounds));

            // Add the item holder for the random ringtones.
            itemHolders.add(new SystemRingtoneHolder(RANDOM_RINGTONE, null));

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
        } catch (Exception e) {
            LogUtils.e("Could not get system ringtone cursor");
            return new ArrayList<>();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        mCustomRingtones = null;
    }
}

/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.alarms.dataadapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.alarms.ScrollHandler;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

/**
 * Data adapter for alarm time items.
 */
public final class AlarmTimeAdapter extends RecyclerView.Adapter<AlarmTimeViewHolder> {
    private static final String TAG = "CwAlarm";
    private static final String KEY_EXPANDED_ID = "expandedId";
    private static final int VIEW_TYPE_ALARM_TIME_COLLAPSED = R.layout.alarm_time_collapsed;
    private static final int VIEW_TYPE_ALARM_TIME_EXPANDED = R.layout.alarm_time_expanded;

    private final Context mContext;
    private final LayoutInflater mInflater;

    private final AlarmTimeClickHandler mAlarmTimeClickHandler;
    private final ScrollHandler mScrollHandler;

    private final boolean mHasVibrator;
    private int mExpandedPosition = -1;
    private long mExpandedId = Alarm.INVALID_ID;
    private Cursor mCursor;

    public AlarmTimeAdapter(Context context, Bundle savedState,
            AlarmTimeClickHandler alarmTimeClickHandler, ScrollHandler smoothScrollController) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mScrollHandler = smoothScrollController;
        mAlarmTimeClickHandler = alarmTimeClickHandler;
        mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                .hasVibrator();
        if (savedState != null) {
            mExpandedId = savedState.getLong(KEY_EXPANDED_ID, Alarm.INVALID_ID);
        }

        setHasStableIds(true);
    }

    @Override
    public AlarmTimeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = mInflater.inflate(viewType, parent, false /* attachToRoot */);
        if (viewType == VIEW_TYPE_ALARM_TIME_COLLAPSED) {
            return new CollapsedAlarmViewHolder(v, mAlarmTimeClickHandler, this);
        } else {
            return new ExpandedAlarmViewHolder(v, mHasVibrator, mAlarmTimeClickHandler, this);
        }
    }

    @Override
    public void onViewRecycled(AlarmTimeViewHolder viewHolder) {
        super.onViewRecycled(viewHolder);
        viewHolder.clearData();
    }

    @Override
    public void onBindViewHolder(AlarmTimeViewHolder viewHolder, int position) {
        if (!mCursor.moveToPosition(position)) {
            LogUtils.e(TAG, "Failed to bind alarm " + position);
            return;
        }
        final Alarm alarm = new Alarm(mCursor);
        final AlarmInstance alarmInstance = alarm.canPreemptivelyDismiss()
                ? new AlarmInstance(mCursor, true /* joinedTable */) : null;
        viewHolder.bindAlarm(mContext, alarm, alarmInstance);
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) {
            return RecyclerView.NO_ID;
        }
        // TODO: Directly read id instead of instantiating Alarm object.
        return new Alarm(mCursor).id;
    }

    @Override
    public int getItemViewType(int position) {
        final long stableId = getItemId(position);
        return stableId != RecyclerView.NO_ID && stableId == mExpandedId
                ? VIEW_TYPE_ALARM_TIME_EXPANDED : VIEW_TYPE_ALARM_TIME_COLLAPSED;
    }

    public void saveInstance(Bundle outState) {
        outState.putLong(KEY_EXPANDED_ID, mExpandedId);
    }

    /**
     * Request the UI to expand the alarm at selected position and scroll it into view.
     */
    public void expand(int position) {
        final long stableId = getItemId(position);
        if (mExpandedId == stableId) {
            return;
        }
        mExpandedId = stableId;
        mScrollHandler.smoothScrollTo(position);
        if (mExpandedPosition >= 0) {
            notifyItemChanged(mExpandedPosition);
        }
        mExpandedPosition = position;
        notifyItemChanged(position);
    }

    public void collapse(int position) {
        mExpandedId = Alarm.INVALID_ID;
        mExpandedPosition = -1;
        notifyItemChanged(position);
    }

    /**
     * Swaps the adapter to a new data source.
     *
     * @param cursor A cursor generated by Cursor loader from {@link Alarm#getAlarmsCursorLoader}.
     */
    public void swapCursor(Cursor cursor) {
        if (mCursor == cursor) {
            return;
        }
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = cursor;
        notifyDataSetChanged();
    }
}

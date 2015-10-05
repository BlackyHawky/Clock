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

package com.android.deskclock.stopwatch;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;

import java.text.DecimalFormatSymbols;
import java.util.List;

/**
 * Displays a list of lap times in reverse order. That is, the newest lap is at the top, the oldest
 * lap is at the bottom.
 */
class LapsAdapter extends RecyclerView.Adapter<LapsAdapter.LapItemHolder> {

    private final LayoutInflater mInflater;
    private final Context mContext;

    /** Used to determine when the time format for the lap time column has changed length. */
    private int mLastFormattedLapTimeLength;

    /** Used to determine when the time format for the total time column has changed length. */
    private int mLastFormattedAccumulatedTimeLength;

    public LapsAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    /**
     * After recording the first lap, there is always a "current lap" in progress.
     *
     * @return 0 if no laps are yet recorded; lap count + 1 if any laps exist
     */
    @Override
    public int getItemCount() {
        final int lapCount = getLaps().size();
        final int currentLapCount = lapCount == 0 ? 0 : 1;
        return currentLapCount + lapCount;
    }

    @Override
    public LapItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = mInflater.inflate(R.layout.lap_view, parent, false /* attachToRoot */);
        return new LapItemHolder(v);
    }

    @Override
    public void onBindViewHolder(LapItemHolder viewHolder, int position) {
        final long lapTime;
        final int lapNumber;
        final long totalTime;

        // Lap will be null for the current lap.
        final Lap lap = position == 0 ? null : getLaps().get(position - 1);
        if (lap != null) {
            // For a recorded lap, merely extract the values to format.
            lapTime = lap.getLapTime();
            lapNumber = lap.getLapNumber();
            totalTime = lap.getAccumulatedTime();
        } else {
            // For the current lap, compute times relative to the stopwatch.
            totalTime = getStopwatch().getTotalTime();
            lapTime = DataModel.getDataModel().getCurrentLapTime(totalTime);
            lapNumber = getLaps().size() + 1;
        }

        // Bind data into the child views.
        viewHolder.lapTime.setText(formatLapTime(lapTime, true));
        viewHolder.accumulatedTime.setText(formatAccumulatedTime(totalTime, true));
        viewHolder.lapNumber.setText(formatLapNumber(getLaps().size() + 1, lapNumber));
    }

    @Override
    public long getItemId(int position) {
        final List<Lap> laps = getLaps();
        if (position == 0) {
            return laps.size() + 1;
        }

        return laps.get(position - 1).getLapNumber();
    }

    /**
     * @param rv the RecyclerView that contains the {@code childView}
     * @param totalTime time accumulated for the current lap and all prior laps
     */
    void updateCurrentLap(RecyclerView rv, long totalTime) {
        // If no laps exist there is nothing to do.
        if (getItemCount() == 0) {
            return;
        }

        final View currentLapView = rv.getChildAt(0);
        if (currentLapView != null) {
            // Compute the lap time using the total time.
            final long lapTime = DataModel.getDataModel().getCurrentLapTime(totalTime);

            final LapItemHolder holder = (LapItemHolder) rv.getChildViewHolder(currentLapView);
            holder.lapTime.setText(formatLapTime(lapTime, false));
            holder.accumulatedTime.setText(formatAccumulatedTime(totalTime, false));
        }
    }

    /**
     * Record a new lap and update this adapter to include it.
     *
     * @return a newly cleared lap
     */
    Lap addLap() {
        final Lap lap = DataModel.getDataModel().addLap();

        if (getItemCount() == 10) {
            // 10 total laps indicates all items switch from 1 to 2 digit lap numbers.
            notifyDataSetChanged();
        } else {
            // New current lap now exists.
            notifyItemInserted(0);

            // Prior current lap must be refreshed once with the true values in place.
            notifyItemChanged(1);
        }

        return lap;
    }

    /**
     * Remove all recorded laps and update this adapter.
     */
    void clearLaps() {
        DataModel.getDataModel().clearLaps();

        // Clear the computed time lengths related to the old recorded laps.
        mLastFormattedLapTimeLength = 0;
        mLastFormattedAccumulatedTimeLength = 0;

        notifyDataSetChanged();
    }

    /**
     * @return a formatted textual description of lap times and total time
     */
    String getShareText() {
        final Stopwatch stopwatch = getStopwatch();
        final long totalTime = stopwatch.getTotalTime();
        final String stopwatchTime = formatTime(totalTime, totalTime, ":");

        // Choose a size for the builder that is unlikely to be resized.
        final StringBuilder builder = new StringBuilder(1000);

        // Add the total elapsed time of the stopwatch.
        builder.append(mContext.getString(R.string.sw_share_main, stopwatchTime));
        builder.append("\n");

        final List<Lap> laps = getLaps();
        if (!laps.isEmpty()) {
            // Add a header for lap times.
            builder.append(mContext.getString(R.string.sw_share_laps));
            builder.append("\n");

            // Loop through the laps in the order they were recorded; reverse of display order.
            for (int i = laps.size() - 1; i >= 0; i--) {
                final Lap lap = laps.get(i);
                builder.append(lap.getLapNumber());
                builder.append(DecimalFormatSymbols.getInstance().getDecimalSeparator());
                builder.append(' ');
                builder.append(formatTime(lap.getLapTime(), lap.getLapTime(), " "));
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    /**
     * @param lapCount the total number of recorded laps
     * @param lapNumber the number of the lap being formatted
     * @return e.g. "# 7" if {@code lapCount} less than 10; "# 07" if {@code lapCount} is 10 or more
     */
    @VisibleForTesting
    String formatLapNumber(int lapCount, int lapNumber) {
        if (lapCount < 10) {
            return String.format("# %d", lapNumber);
        }

        return String.format("# %02d", lapNumber);
    }

    /**
     * @param maxTime the maximum amount of time; used to choose a time format
     * @param time the time to format guaranteed not to exceed {@code maxTime}
     * @param separator displayed between hours and minutes as well as minutes and seconds
     * @return a formatted version of the time
     */
    @VisibleForTesting
    String formatTime(long maxTime, long time, String separator) {
        long hundredths, seconds, minutes, hours;
        seconds = time / 1000;
        hundredths = (time - seconds * 1000) / 10;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;

        final char decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();

        if (maxTime < 10 * DateUtils.MINUTE_IN_MILLIS) {
            return String.format("%d%s%02d%s%02d",
                    minutes, separator, seconds, decimalSeparator, hundredths);
        } else if (maxTime < 60 * DateUtils.MINUTE_IN_MILLIS) {
            return String.format("%02d%s%02d%s%02d",
                    minutes, separator, seconds, decimalSeparator, hundredths);
        } else if (maxTime < 10 * DateUtils.HOUR_IN_MILLIS) {
            return String.format("%d%s%02d%s%02d%s%02d",
                    hours, separator, minutes, separator, seconds, decimalSeparator, hundredths);
        } else if (maxTime < 100 * DateUtils.HOUR_IN_MILLIS) {
            return String.format("%02d%s%02d%s%02d%s%02d",
                    hours, separator, minutes, separator, seconds, decimalSeparator, hundredths);
        }

        return String.format("%03d%s%02d%s%02d%s%02d",
                hours, separator, minutes, separator, seconds, decimalSeparator, hundredths);
    }

    /**
     * @param lapTime the lap time to be formatted
     * @param isBinding if the lap time is requested so it can be bound avoid notifying of data
     *                  set changes; they are not allowed to occur during bind
     * @return a formatted version of the lap time
     */
    private String formatLapTime(long lapTime, boolean isBinding) {
        // The longest lap dictates the way the given lapTime must be formatted.
        final long longestLapTime = Math.max(DataModel.getDataModel().getLongestLapTime(), lapTime);
        final String formattedTime = formatTime(longestLapTime, lapTime, " ");

        // If the newly formatted lap time has altered the format, refresh all laps.
        final int newLength = formattedTime.length();
        if (!isBinding && mLastFormattedLapTimeLength != newLength) {
            mLastFormattedLapTimeLength = newLength;
            notifyDataSetChanged();
        }

        return formattedTime;
    }

    /**
     * @param accumulatedTime the accumulated time to be formatted
     * @param isBinding if the lap time is requested so it can be bound avoid notifying of data
     *                  set changes; they are not allowed to occur during bind
     * @return a formatted version of the accumulated time
     */
    private String formatAccumulatedTime(long accumulatedTime, boolean isBinding) {
        final long totalTime = getStopwatch().getTotalTime();
        final long longestAccumulatedTime = Math.max(totalTime, accumulatedTime);
        final String formattedTime = formatTime(longestAccumulatedTime, accumulatedTime, " ");

        // If the newly formatted accumulated time has altered the format, refresh all laps.
        final int newLength = formattedTime.length();
        if (!isBinding && mLastFormattedAccumulatedTimeLength != newLength) {
            mLastFormattedAccumulatedTimeLength = newLength;
            notifyDataSetChanged();
        }

        return formattedTime;
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private List<Lap> getLaps() {
        return DataModel.getDataModel().getLaps();
    }

    /**
     * Cache the child views of each lap item view.
     */
    static final class LapItemHolder extends RecyclerView.ViewHolder {

        private final TextView lapNumber;
        private final TextView lapTime;
        private final TextView accumulatedTime;

        public LapItemHolder(View itemView) {
            super(itemView);

            lapTime = (TextView) itemView.findViewById(R.id.lap_time);
            lapNumber = (TextView) itemView.findViewById(R.id.lap_number);
            accumulatedTime = (TextView) itemView.findViewById(R.id.lap_total);
        }
    }
}
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
import com.android.deskclock.uidata.UiDataModel;

import java.text.DecimalFormatSymbols;
import java.util.List;

/**
 * Displays a list of lap times in reverse order. That is, the newest lap is at the top, the oldest
 * lap is at the bottom.
 */
class LapsAdapter extends RecyclerView.Adapter<LapsAdapter.LapItemHolder> {

    private static final long TEN_MINUTES = 10 * DateUtils.MINUTE_IN_MILLIS;
    private static final long HOUR = DateUtils.HOUR_IN_MILLIS;
    private static final long TEN_HOURS = 10 * HOUR;
    private static final long HUNDRED_HOURS = 100 * HOUR;

    /** A single space preceded by a zero-width LRM; This groups adjacent chars left-to-right. */
    private static final String LRM_SPACE = "\u200E ";

    /** Reusable StringBuilder that assembles a formatted time; alleviates memory churn. */
    private static final StringBuilder sTimeBuilder = new StringBuilder(12);

    private final LayoutInflater mInflater;
    private final Context mContext;

    /** Used to determine when the time format for the lap time column has changed length. */
    private int mLastFormattedLapTimeLength;

    /** Used to determine when the time format for the total time column has changed length. */
    private int mLastFormattedAccumulatedTimeLength;

    LapsAdapter(Context context) {
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
            final String separator = DecimalFormatSymbols.getInstance().getDecimalSeparator() + " ";
            for (int i = laps.size() - 1; i >= 0; i--) {
                final Lap lap = laps.get(i);
                builder.append(lap.getLapNumber());
                builder.append(separator);
                final long lapTime = lap.getLapTime();
                builder.append(formatTime(lapTime, lapTime, " "));
                builder.append("\n");
            }

            // Append the final lap
            builder.append(laps.size() + 1);
            builder.append(separator);
            final long lapTime = DataModel.getDataModel().getCurrentLapTime(totalTime);
            builder.append(formatTime(lapTime, lapTime, " "));
            builder.append("\n");
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
            return mContext.getString(R.string.lap_number_single_digit, lapNumber);
        } else {
            return mContext.getString(R.string.lap_number_double_digit, lapNumber);
        }
    }

    /**
     * @param maxTime the maximum amount of time; used to choose a time format
     * @param time the time to format guaranteed not to exceed {@code maxTime}
     * @param separator displayed between hours and minutes as well as minutes and seconds
     * @return a formatted version of the time
     */
    @VisibleForTesting
    static String formatTime(long maxTime, long time, String separator) {
        final int hours, minutes, seconds, hundredths;
        if (time <= 0) {
            // A negative time should be impossible, but is tolerated to avoid crashing the app.
            hours = minutes = seconds = hundredths = 0;
        } else {
            hours = (int) (time / DateUtils.HOUR_IN_MILLIS);
            int remainder = (int) (time % DateUtils.HOUR_IN_MILLIS);

            minutes = (int) (remainder / DateUtils.MINUTE_IN_MILLIS);
            remainder = (int) (remainder % DateUtils.MINUTE_IN_MILLIS);

            seconds = (int) (remainder / DateUtils.SECOND_IN_MILLIS);
            remainder = (int) (remainder % DateUtils.SECOND_IN_MILLIS);

            hundredths = remainder / 10;
        }

        final char decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();

        sTimeBuilder.setLength(0);

        // The display of hours and minutes varies based on maxTime.
        if (maxTime < TEN_MINUTES) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(minutes, 1));
        } else if (maxTime < HOUR) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(minutes, 2));
        } else if (maxTime < TEN_HOURS) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(hours, 1));
            sTimeBuilder.append(separator);
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(minutes, 2));
        } else if (maxTime < HUNDRED_HOURS) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(hours, 2));
            sTimeBuilder.append(separator);
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(minutes, 2));
        } else {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(hours, 3));
            sTimeBuilder.append(separator);
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(minutes, 2));
        }

        // The display of seconds and hundredths-of-a-second is constant.
        sTimeBuilder.append(separator);
        sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(seconds, 2));
        sTimeBuilder.append(decimalSeparator);
        sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(hundredths, 2));

        return sTimeBuilder.toString();
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
        final String formattedTime = formatTime(longestLapTime, lapTime, LRM_SPACE);

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
        final String formattedTime = formatTime(longestAccumulatedTime, accumulatedTime, LRM_SPACE);

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

        LapItemHolder(View itemView) {
            super(itemView);

            lapTime = (TextView) itemView.findViewById(R.id.lap_time);
            lapNumber = (TextView) itemView.findViewById(R.id.lap_number);
            accumulatedTime = (TextView) itemView.findViewById(R.id.lap_total);
        }
    }
}
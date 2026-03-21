/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CITY_NOTE;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.ItemTouchHelperContract;
import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.CityListener;
import com.best.deskclock.data.DataModel;

import java.util.Collections;
import java.util.List;

/**
 * This adapter lists all of the selected world clocks. Optionally, it also includes a clock at
 * the top for the home timezone if "Automatic home clock" is turned on in settings and the
 * current time at home does not match the current time in the timezone of the current location.
 * If the phone is in portrait mode it will also include the main clock at the top.
 */
public class SelectedCitiesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements CityListener, ItemTouchHelperContract {

    private final static int MAIN_CLOCK = R.layout.main_clock_frame;
    private final static int WORLD_CLOCK = R.layout.world_clock_item;
    public final String PAYLOAD_UPDATE_BACKGROUND = "PAYLOAD_UPDATE_BACKGROUND";

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final String mDateFormat;
    private final String mDateFormatForAccessibility;
    private final List<City> mCities;
    private final boolean mIsPortrait;
    private final boolean mShowHomeClock;

    public SelectedCitiesAdapter(Context context, String dateFormat, String dateFormatForAccessibility, List<City> cities,
                                 boolean showHomeClock, boolean isPortrait) {

        mContext = context;
        mPrefs = getDefaultSharedPreferences(context);
        mDateFormat = dateFormat;
        mDateFormatForAccessibility = dateFormatForAccessibility;
        mInflater = LayoutInflater.from(context);
        mCities = cities;
        mShowHomeClock = showHomeClock;
        mIsPortrait = isPortrait;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mIsPortrait) {
            return MAIN_CLOCK;
        }
        return WORLD_CLOCK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = mInflater.inflate(viewType, parent, false);
        if (viewType == WORLD_CLOCK) {
            return new CityViewHolder(view, this);
        } else if (viewType == MAIN_CLOCK) {
            return new MainClockViewHolder(view);
        }
        throw new IllegalArgumentException("View type not recognized");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_UPDATE_BACKGROUND) && holder instanceof CityViewHolder) {
            ((CityViewHolder) holder).updateBackground();
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final int viewType = getItemViewType(position);
        // Retrieve the city to bind.
        if (viewType == WORLD_CLOCK) {
            final City city;
            // If showing home clock, put it at the top
            if (mShowHomeClock && position == (mIsPortrait ? 1 : 0)) {
                city = getHomeCity();
            } else {
                final int positionAdjuster = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);
                city = getCities().get(position - positionAdjuster);
            }
            ((CityViewHolder) holder).bind(city);
        } else if (viewType == MAIN_CLOCK) {
            ((MainClockViewHolder) holder).bind(mContext, mDateFormat, mDateFormatForAccessibility, mCities, mShowHomeClock, mIsPortrait);
        } else {
            throw new IllegalArgumentException("Unexpected view type: " + viewType);
        }
    }

    @Override
    public int getItemCount() {
        final int mainClockCount = mIsPortrait ? 1 : 0;
        final int homeClockCount = mShowHomeClock ? 1 : 0;
        final int worldClockCount = getCities().size();
        return mainClockCount + homeClockCount + worldClockCount;
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        int offset = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mCities, i - offset, (i + 1) - offset);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mCities, i - offset, (i - 1) - offset);
            }
        }

        notifyItemMoved(fromPosition, toPosition);

        int worldClockCount = getItemCount() - offset;
        notifyItemRangeChanged(offset, worldClockCount, PAYLOAD_UPDATE_BACKGROUND);
    }

    @Override
    public void citiesChanged() {
        List<City> newCities = DataModel.getDataModel().getSelectedCities();

        if (!mCities.equals(newCities)) {
            mCities.clear();
            mCities.addAll(newCities);
        }

        notifyDataSetChanged();
    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
        // Draw a shadow under the city card when it's dragging
        viewHolder.itemView.setTranslationZ(dpToPx(6, mContext.getResources().getDisplayMetrics()));
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
        // Remove the shadow under the city card when the drag is complete.
        viewHolder.itemView.setTranslationZ(0f);
    }

    @Override
    public void onRowSaved() {
        DataModel.getDataModel().updateSelectedCitiesOrder(mCities);
    }

    private City getHomeCity() {
        return DataModel.getDataModel().getHomeCity();
    }

    public List<City> getCities() {
        return mCities;
    }

    public void refreshAlarm() {
        if (mIsPortrait && getItemCount() > 0) {
            notifyItemChanged(0);
        }
    }

    private int getCityPositionById(String cityId) {
        final int positionAdjuster = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);

        for (int i = 0; i < mCities.size(); i++) {
            if (mCities.get(i).getId().equals(cityId)) {
                return i + positionAdjuster;
            }
        }

        return RecyclerView.NO_POSITION;
    }

    public void setCityNote(String cityId, String note) {
        SharedPreferences.Editor editor = mPrefs.edit();
        String key = KEY_CITY_NOTE + cityId;

        if (note.trim().isEmpty()) {
            editor.remove(key);
        } else {
            editor.putString(key, note);
        }

        editor.apply();

        int position = getCityPositionById(cityId);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    @Nullable
    public String getCityNote(String cityId) {
        return mPrefs.getString(KEY_CITY_NOTE + cityId, null);
    }

}

/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.City;
import com.best.deskclock.data.CityListener;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.databinding.WorldClockItemBinding;
import com.best.deskclock.uicomponents.ItemTouchHelperContract;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This adapter lists all the selected world clocks. Optionally, it also includes a clock at
 * the top for the home timezone if:
 * <ul>
 *     <li>"Automatic home clock" is turned on in settings;</li>
 *     <li>The current time at home does not match the current time in the timezone of the current location. </li>
 * </ul>
 * If the phone is in portrait mode it will also include the main clock at the top.
 */
public class SelectedCitiesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements CityListener, ItemTouchHelperContract {

    public final String PAYLOAD_UPDATE_BACKGROUND = "PAYLOAD_UPDATE_BACKGROUND";

    private final DataModel mDataModel;
    private final CityNoteProvider mNoteProvider;
    private UiConfig.Fonts mFonts;
    private final UiConfig.Screen mScreen;
    private ClockSettings mSettings;
    private final List<City> mCities;
    private final boolean mHasBlackAccentColor;

    private final Drawable.ConstantState mBgSingle;
    private final Drawable.ConstantState mBgTop;
    private final Drawable.ConstantState mBgMiddle;
    private final Drawable.ConstantState mBgBottom;

    public SelectedCitiesAdapter(@NonNull Context context, @NonNull DataModel dataModel, @NonNull List<City> cities,
                                 @NonNull UiConfig.Fonts fonts, @NonNull UiConfig.Screen screen, @NonNull UiConfig.CardStyle cardStyle,
                                 @NonNull ClockSettings clockSettings, boolean hasBlackAccentColor,
                                 @NonNull CityNoteProvider noteProvider) {

        mDataModel = dataModel;
        mNoteProvider = noteProvider;
        mCities = new ArrayList<>(cities);
        mFonts = fonts;
        mScreen = screen;
        mHasBlackAccentColor = hasBlackAccentColor;
        mSettings = clockSettings;

        mBgSingle = ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
            cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 0, 1).getConstantState();
        // position=0, totalCount=3 -> Top
        mBgTop = ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
            cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 0, 3).getConstantState();
        // position=1, totalCount=3 -> Middle
        mBgMiddle = ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
            cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 1, 3).getConstantState();
        // position=2, totalCount=3 -> Bottom
        mBgBottom = ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
            cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 2, 3).getConstantState();
    }

    public UiConfig.Fonts getFonts() { return mFonts; }
    public UiConfig.Screen getScreen() { return mScreen; }
    public ClockSettings getSettings() { return mSettings; }
    public CityNoteProvider getNoteProvider() { return mNoteProvider; }
    public boolean hasBlackAccentColor() { return mHasBlackAccentColor; }
    public Drawable.ConstantState getBgSingle() { return mBgSingle; }
    public Drawable.ConstantState getBgTop() { return mBgTop; }
    public Drawable.ConstantState getBgMiddle() { return mBgMiddle; }
    public Drawable.ConstantState getBgBottom() { return mBgBottom; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        WorldClockItemBinding binding = WorldClockItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new CityViewHolder(binding, this);
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
        ((CityViewHolder) holder).applySettings();

        final City city;

        if (mSettings.showHomeClock && position == 0) {
            city = getHomeCity();
        } else {
            final int positionAdjuster = mSettings.showHomeClock ? 1 : 0;
            city = getCities().get(position - positionAdjuster);
        }
        ((CityViewHolder) holder).bind(city);
    }

    @Override
    public int getItemCount() {
        final int homeClockCount = mSettings.showHomeClock ? 1 : 0;
        final int worldClockCount = getCities().size();
        return homeClockCount + worldClockCount;
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        int offset = mSettings.showHomeClock ? 1 : 0;

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
        List<City> newCities = mDataModel.getSelectedCities();

        if (!mCities.equals(newCities)) {
            mCities.clear();
            mCities.addAll(newCities);
        }

        notifyDataSetChanged();
    }

    @Override
    public void onRowSelected(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Draw a shadow under the city card when it's dragging
        viewHolder.itemView.setTranslationZ(dpToPx(6, mScreen.metrics()));
    }

    @Override
    public void onRowClear(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Remove the shadow under the city card when the drag is complete.
        viewHolder.itemView.setTranslationZ(0f);
    }

    @Override
    public void onRowSaved() {
        mDataModel.updateSelectedCitiesOrder(mCities);
    }

    private City getHomeCity() {
        return mDataModel.getHomeCity();
    }

    public List<City> getCities() {
        return mCities;
    }

    private int getCityPositionById(String cityId) {
        if (mSettings.showHomeClock) {
            City homeCity = getHomeCity();
            if (homeCity != null && homeCity.getId().equals(cityId)) {
                return 0;
            }
        }

        final int positionAdjuster = mSettings.showHomeClock ? 1 : 0;

        for (int i = 0; i < mCities.size(); i++) {
            if (mCities.get(i).getId().equals(cityId)) {
                return i + positionAdjuster;
            }
        }

        return RecyclerView.NO_POSITION;
    }

    public void updateSettings(@NonNull ClockSettings settings) {
        mSettings = settings;
        notifyDataSetChanged();
    }

    public void updateFonts(@NonNull UiConfig.Fonts fonts) {
        mFonts = fonts;
        notifyDataSetChanged();
    }

    public void notifyCityNoteChanged(@NonNull String cityId) {
        int position = getCityPositionById(cityId);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    public interface CityNoteProvider {
        @Nullable
        String getNote(String cityId);
    }

}

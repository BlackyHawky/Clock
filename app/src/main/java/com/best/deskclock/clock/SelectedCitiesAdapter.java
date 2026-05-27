/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CITY_NOTE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.City;
import com.best.deskclock.data.CityListener;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.WorldClockItemBinding;
import com.best.deskclock.uicomponents.ItemTouchHelperContract;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;

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

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final List<City> mCities;
    public final boolean mIsPortrait;
    private final boolean mShowHomeClock;
    private final Typeface mRegularTypeface;
    private final Typeface mBoldTypeface;
    private final Typeface mDigitalClockTypeface;
    private final boolean mIsCityNoteEnabled;
    private final boolean mIsDigitalClock;
    private final boolean mHasBlackAccentColor;

    private final Drawable.ConstantState mBgSingle;
    private final Drawable.ConstantState mBgTop;
    private final Drawable.ConstantState mBgMiddle;
    private final Drawable.ConstantState mBgBottom;

    public SelectedCitiesAdapter(Context context, List<City> cities, boolean showHomeClock, boolean isPortrait) {
        mContext = context;
        mPrefs = getDefaultSharedPreferences(context);
        mCities = cities;
        mShowHomeClock = showHomeClock;
        mIsPortrait = isPortrait;
        String fontPath = SettingsDAO.getGeneralFont(mPrefs);
        mRegularTypeface = ThemeUtils.loadFont(fontPath);
        mBoldTypeface = ThemeUtils.boldTypeface(fontPath);
        mDigitalClockTypeface = SettingsDAO.getClockStyle(mPrefs) == DataModel.ClockStyle.DIGITAL
            ? ThemeUtils.loadFont(SettingsDAO.getDigitalClockFont(mPrefs))
            : null;
        mIsCityNoteEnabled = SettingsDAO.isCityNoteEnabled(mPrefs);
        mIsDigitalClock = SettingsDAO.getClockStyle(mPrefs) == DataModel.ClockStyle.DIGITAL;
        mHasBlackAccentColor = SettingsDAO.getAccentColor(mPrefs).equals(BLACK_ACCENT_COLOR);

        mBgSingle = ThemeUtils.expressiveCardBackground(context, 0, 1).getConstantState();
        // position=0, totalCount=3 -> Top
        mBgTop = ThemeUtils.expressiveCardBackground(context, 0, 3).getConstantState();
        // position=1, totalCount=3 -> Middle
        mBgMiddle = ThemeUtils.expressiveCardBackground(context, 1, 3).getConstantState();
        // position=2, totalCount=3 -> Bottom
        mBgBottom = ThemeUtils.expressiveCardBackground(context, 2, 3).getConstantState();
    }

    public Drawable.ConstantState getBgSingle() { return mBgSingle; }
    public Drawable.ConstantState getBgTop() { return mBgTop; }
    public Drawable.ConstantState getBgMiddle() { return mBgMiddle; }
    public Drawable.ConstantState getBgBottom() { return mBgBottom; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        WorldClockItemBinding binding = WorldClockItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new CityViewHolder(binding, this, mRegularTypeface, mBoldTypeface, mDigitalClockTypeface, mIsCityNoteEnabled,
            mIsDigitalClock, mHasBlackAccentColor);
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
        final City city;

        if (mShowHomeClock && position == 0) {
            city = getHomeCity();
        } else {
            final int positionAdjuster = mShowHomeClock ? 1 : 0;
            city = getCities().get(position - positionAdjuster);
        }
        ((CityViewHolder) holder).bind(city);
    }

    @Override
    public int getItemCount() {
        final int homeClockCount = mShowHomeClock ? 1 : 0;
        final int worldClockCount = getCities().size();
        return homeClockCount + worldClockCount;
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        int offset = mShowHomeClock ? 1 : 0;

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

    private int getCityPositionById(String cityId) {
        if (mShowHomeClock) {
            City homeCity = getHomeCity();
            if (homeCity != null && homeCity.getId().equals(cityId)) {
                return 0;
            }
        }

        final int positionAdjuster = mShowHomeClock ? 1 : 0;

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

            WidgetUtils.updateWidget(mContext, DigitalAppWidgetProvider.class);
        }
    }

    @Nullable
    public String getCityNote(String cityId) {
        return mPrefs.getString(KEY_CITY_NOTE + cityId, null);
    }

}

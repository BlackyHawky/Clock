// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.databinding.AlarmItemBinding;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmItemViewHolder> {

    private static final String PAYLOAD_UPDATE_BACKGROUND = "PAYLOAD_UPDATE_BACKGROUND";

    private final SharedPreferences mPrefs;
    private final Typeface mGeneralTypeface;
    private final Typeface mGeneralBoldTypeface;
    private final Typeface mAlarmClockTypeface;
    private final Locale mLocale;
    private final String mDatePattern;
    private final String mDatePatternWithYear;
    private List<AlarmItemHolder> mItems = new ArrayList<>();
    private final boolean mUseExpressiveBackground;

    private final Drawable.ConstantState mBgSingle;
    private final Drawable.ConstantState mBgTop;
    private final Drawable.ConstantState mBgMiddle;
    private final Drawable.ConstantState mBgBottom;
    private final Drawable.ConstantState mBgStandard;

    public AlarmAdapter(Context context, SharedPreferences prefs, Typeface generalTypeface, Typeface generalBoldTypeface,
                        Typeface alarmClockTypeface) {

        setHasStableIds(true);

        mPrefs = prefs;
        mGeneralTypeface = generalTypeface;
        mGeneralBoldTypeface = generalBoldTypeface;
        mAlarmClockTypeface = alarmClockTypeface;
        mLocale = Locale.getDefault();
        mDatePattern = DateFormat.getBestDateTimePattern(mLocale, AlarmItemViewHolder.SKELETON);
        mDatePatternWithYear = DateFormat.getBestDateTimePattern(mLocale, AlarmItemViewHolder.SKELETON_WITH_YEAR);
        mUseExpressiveBackground = !ThemeUtils.isTablet() && !ThemeUtils.isLandscape();

        if (mUseExpressiveBackground) {
            // Phone in portrait mode: generate the 4 expressive shapes with their ripple effect
            mBgSingle = ThemeUtils.rippleDrawable(
                context, ThemeUtils.expressiveCardBackground(context, 0, 1)).getConstantState();
            mBgTop = ThemeUtils.rippleDrawable(
                context, ThemeUtils.expressiveCardBackground(context, 0, 3)).getConstantState();
            mBgMiddle = ThemeUtils.rippleDrawable(
                context, ThemeUtils.expressiveCardBackground(context, 1, 3)).getConstantState();
            mBgBottom = ThemeUtils.rippleDrawable(
                context, ThemeUtils.expressiveCardBackground(context, 2, 3)).getConstantState();
            mBgStandard = null;
        } else {
            // Tablet / Landscape: all cards are standard
            mBgStandard = ThemeUtils.rippleDrawable(context, ThemeUtils.cardBackground(context)).getConstantState();
            mBgSingle = mBgTop = mBgMiddle = mBgBottom = null;
        }
    }

    public boolean isUseExpressiveBackground() { return mUseExpressiveBackground; }
    public Drawable.ConstantState getBgSingle() { return mBgSingle; }
    public Drawable.ConstantState getBgTop() { return mBgTop; }
    public Drawable.ConstantState getBgMiddle() { return mBgMiddle; }
    public Drawable.ConstantState getBgBottom() { return mBgBottom; }
    public Drawable.ConstantState getBgStandard() { return mBgStandard; }

    @NonNull
    @Override
    public AlarmItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AlarmItemBinding binding = AlarmItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new AlarmItemViewHolder(binding, this, mPrefs, mGeneralTypeface, mGeneralBoldTypeface, mAlarmClockTypeface,
            mLocale, mDatePattern, mDatePatternWithYear);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmItemViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_UPDATE_BACKGROUND)) {
            holder.updateBackground();
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmItemViewHolder holder, int position) {
        AlarmItemHolder itemHolder = mItems.get(position);
        holder.bind(itemHolder);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).itemId;
    }

    public void setItems(List<AlarmItemHolder> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    public void removeItem(AlarmItemHolder itemHolder) {
        int position = mItems.indexOf(itemHolder);
        if (position != -1) {
            mItems.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void swapItems(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mItems, i, i - 1);
            }
        }

        notifyItemMoved(fromPosition, toPosition);
        notifyItemRangeChanged(0, mItems.size(), PAYLOAD_UPDATE_BACKGROUND);
    }

    public List<AlarmItemHolder> getItems() {
        return mItems;
    }

}

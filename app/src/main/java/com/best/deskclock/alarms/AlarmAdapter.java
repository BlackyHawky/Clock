// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.Weekdays;
import com.best.deskclock.databinding.AlarmItemBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmItemViewHolder> {

    private static final String PAYLOAD_UPDATE_BACKGROUND = "PAYLOAD_UPDATE_BACKGROUND";

    private UiConfig.Fonts mFonts;
    private final UiConfig.DateFormat mDateFormat;
    private final UiConfig.Screen mScreen;
    private final UiConfig.Haptics mHaptics;
    private List<AlarmItemHolder> mItems = new ArrayList<>();
    private final AlarmStateProvider mStateProvider;
    private Weekdays.Order mWeekdayOrder;
    private boolean mIs24HourMode;
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private final boolean mUseExpressiveBackground;

    private final Drawable.ConstantState mBgSingle;
    private final Drawable.ConstantState mBgTop;
    private final Drawable.ConstantState mBgMiddle;
    private final Drawable.ConstantState mBgBottom;
    private final Drawable.ConstantState mBgStandard;

    public AlarmAdapter(@NonNull Context context, @NonNull UiConfig.Fonts fonts, @NonNull UiConfig.DateFormat dateConfig,
                        @NonNull UiConfig.Screen screen, @NonNull UiConfig.CardStyle cardStyle, @NonNull UiConfig.Haptics haptics,
                        @NonNull Weekdays.Order weekdayOrder, boolean is24HourMode, @NonNull AlarmStateProvider stateProvider) {

        setHasStableIds(true);

        mFonts = fonts;
        mDateFormat = dateConfig;
        mScreen = screen;
        mHaptics = haptics;
        mWeekdayOrder = weekdayOrder;
        mStateProvider = stateProvider;
        mIs24HourMode = is24HourMode;
        mUseExpressiveBackground = !screen.isTablet() && !screen.isLandscape();

        if (mUseExpressiveBackground) {
            // Phone in portrait mode: generate the 4 expressive shapes with their ripple effect
            mBgSingle = ThemeUtils.rippleDrawable(context,
                ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
                    cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 0, 1)).getConstantState();
            mBgTop = ThemeUtils.rippleDrawable(context,
                ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
                    cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 0, 3)).getConstantState();
            mBgMiddle = ThemeUtils.rippleDrawable(context,
                ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
                    cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 1, 3)).getConstantState();
            mBgBottom = ThemeUtils.rippleDrawable(context,
                ThemeUtils.expressiveCardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
                    cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode(), 2, 3)).getConstantState();
            mBgStandard = null;
        } else {
            // Tablet / Landscape: all cards are standard
            mBgStandard = ThemeUtils.rippleDrawable(
                context, ThemeUtils.cardBackground(context, screen.metrics(), cardStyle.isBackgroundDisplayed(),
                    cardStyle.isBorderDisplayed(), cardStyle.isAmoledDarkMode())).getConstantState();
            mBgSingle = mBgTop = mBgMiddle = mBgBottom = null;
        }

        generateTimeFormats();
    }

    public boolean is24HourFormat() { return mIs24HourMode; }
    public UiConfig.Fonts getFonts() { return mFonts; }
    public UiConfig.DateFormat getDateFormat() { return mDateFormat; }
    public UiConfig.Screen getScreen() { return mScreen; }
    public UiConfig.Haptics getHaptics() { return mHaptics; }
    public Weekdays.Order getWeekdayOrder() { return mWeekdayOrder; }
    public AlarmStateProvider getStateProvider() { return mStateProvider; }
    public boolean isUseExpressiveBackground() { return mUseExpressiveBackground; }
    public Drawable.ConstantState getBgSingle() { return mBgSingle; }
    public Drawable.ConstantState getBgTop() { return mBgTop; }
    public Drawable.ConstantState getBgMiddle() { return mBgMiddle; }
    public Drawable.ConstantState getBgBottom() { return mBgBottom; }
    public Drawable.ConstantState getBgStandard() { return mBgStandard; }
    public CharSequence getFormat12() { return mFormat12; }
    public CharSequence getFormat24() { return mFormat24; }

    @NonNull
    @Override
    public AlarmItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AlarmItemBinding binding = AlarmItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new AlarmItemViewHolder(binding, this);
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

    public void updateFonts(@NonNull UiConfig.Fonts fonts) {
        mFonts = fonts;
        generateTimeFormats();
        notifyDataSetChanged();
    }

    public void updateTimeFormat(boolean is24HourMode) {
        if (mIs24HourMode != is24HourMode) {
            mIs24HourMode = is24HourMode;
            notifyDataSetChanged();
        }
    }

    public void updateWeekdayOrder(@NonNull Weekdays.Order weekdayOrder) {
        if (mWeekdayOrder != weekdayOrder) {
            mWeekdayOrder = weekdayOrder;
            notifyDataSetChanged();
        }
    }

    private void generateTimeFormats() {
        Typeface alarmFont = mFonts.alarmClockFont() != null ? mFonts.alarmClockFont() : mFonts.bold();
        mFormat12 = ClockUtils.get12ModeFormat(false, 0.5f, alarmFont, "sans-serif", Typeface.BOLD, false);
        mFormat24 = ClockUtils.get24ModeFormat(false, false);
    }

    public void setItems(@NonNull List<AlarmItemHolder> items) {
        Iterator<AlarmItemHolder> iterator = items.iterator();

        while (iterator.hasNext()) {
            AlarmItemHolder holder = iterator.next();

            if (holder.item != null && AlarmVisualCache.isDismissed(holder.item.id)) {
                if (holder.item.isDeleteAfterUse()) {
                    // Remove the alarm from the list immediately!
                    iterator.remove();
                } else if (!holder.item.daysOfWeek.isRepeating()) {
                    // Standard one-time alarm. Just force the switch to OFF visually.
                    holder.item.enabled = false;
                }
            }
        }

        mItems = items;
        notifyDataSetChanged();
    }

    public void removeItem(@NonNull AlarmItemHolder itemHolder) {
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

    public interface AlarmStateProvider {
        boolean isRepeatDayStyleEnabled(long alarmId);
        boolean canPreemptivelyDismiss(@NonNull Alarm alarm);
        boolean isDismissButtonDisplayed();
    }

}

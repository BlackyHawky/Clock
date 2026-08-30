// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class RingtoneAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_SYSTEM_SOUND = R.layout.ringtone_item_sound;
    public static final int VIEW_TYPE_CUSTOM_SOUND = -R.layout.ringtone_item_sound;
    public static final int VIEW_TYPE_HEADER = R.layout.ringtone_item_header;
    public static final int VIEW_TYPE_BUTTON_TIP = Integer.MIN_VALUE;

    private List<RingtoneItem> mItems = new ArrayList<>();
    private final UiConfig.Fonts mFonts;
    private final OnRingtoneClickListener mListener;

    private final int mDisplayMetricsPadding;

    private final Drawable mRandomIcon;
    private final Drawable mErrorIcon;
    private final Drawable mSilentIcon;
    private final Drawable mAboutIcon;
    private final int mTipTextColor;
    private final String mTipText;
    private final Drawable.ConstantState mBgSelectedState;
    private final Drawable.ConstantState mBgUnselectedState;

    public RingtoneAdapter(@NonNull Context context, @NonNull UiConfig.Fonts fonts, @NonNull UiConfig.Screen screen,
                           boolean isAmoledDarkMode, @NonNull OnRingtoneClickListener listener) {

        mFonts = fonts;
        mListener = listener;
        mDisplayMetricsPadding = (int) dpToPx(20, screen.metrics());

        int colorSelected = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.BLACK);
        int colorUnselected;
        if (ThemeUtils.isNight(context.getResources()) && isAmoledDarkMode) {
            colorUnselected = Color.BLACK;
        } else {
            colorUnselected = MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK);
        }

        // Loading for the RingtoneViewHolder
        mRandomIcon = AppCompatResources.getDrawable(context, R.drawable.ic_random);

        Drawable error = AppCompatResources.getDrawable(context, R.drawable.ic_error);
        if (error != null) {
            mErrorIcon = error.mutate();
            mErrorIcon.setTint(ContextCompat.getColor(context, android.R.color.holo_red_light));
        } else {
            mErrorIcon = null;
        }

        Drawable silent = AppCompatResources.getDrawable(context, R.drawable.ic_ringtone_silent);
        if (silent != null) {
            mSilentIcon = silent.mutate();
            mSilentIcon.setTint(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK));
        } else {
            mSilentIcon = null;
        }

        mBgSelectedState = ThemeUtils.rippleDrawable(context, screen.metrics(), colorSelected).getConstantState();
        mBgUnselectedState = ThemeUtils.rippleDrawable(context, screen.metrics(), colorUnselected).getConstantState();

        // Loading for the AddButtonTipViewHolder
        mAboutIcon = AppCompatResources.getDrawable(context, R.drawable.ic_about);
        mTipTextColor = MaterialColors.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);
        mTipText = context.getString(R.string.button_tip_title);
    }

    public Drawable getRandomIcon() { return mRandomIcon; }
    public Drawable getErrorIcon() { return mErrorIcon; }
    public Drawable getSilentIcon() { return mSilentIcon; }
    public Drawable.ConstantState getBgSelectedState() { return mBgSelectedState; }
    public Drawable.ConstantState getBgUnselectedState() { return mBgUnselectedState; }
    public Drawable getAboutIcon() { return mAboutIcon; }
    public int getTipTextColor() { return mTipTextColor; }
    public String getTipText() { return mTipText; }
    public int getDisplayMetricsPadding() { return mDisplayMetricsPadding; }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(viewType, parent, false), mFonts);
        } else if (viewType == VIEW_TYPE_BUTTON_TIP) {
            return new AddButtonTipViewHolder(inflater.inflate(R.layout.ringtone_item_sound, parent, false), this, mFonts);
        } else {
            return new RingtoneViewHolder(inflater.inflate(Math.abs(viewType), parent, false), this, mFonts);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RingtoneItem item = mItems.get(position);

        if (holder instanceof RingtoneViewHolder ringtoneViewHolder) {
            ringtoneViewHolder.bind((RingtoneHolder) item, mListener);
        } else if (holder instanceof HeaderViewHolder headerViewHolder) {
            headerViewHolder.bind((HeaderHolder) item);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setItems(List<RingtoneItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    public List<RingtoneItem> getItems() {
        return mItems;
    }

    public interface RingtoneItem {
        int getViewType();
    }

    public interface OnRingtoneClickListener {
        void onRingtoneClick(@NonNull RingtoneHolder holder);

        void onRemoveRingtoneClick(@NonNull RingtoneHolder holder);
    }

}

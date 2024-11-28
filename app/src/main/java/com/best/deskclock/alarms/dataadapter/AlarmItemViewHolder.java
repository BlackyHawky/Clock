/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.dataadapter;

import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AMOLED_DARK_MODE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.ItemAnimator;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmTimeClickHandler;
import com.best.deskclock.bedtime.BedtimeFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.TextTime;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.Calendar;

/**
 * Abstract ViewHolder for alarm time items.
 */
public abstract class AlarmItemViewHolder extends ItemAdapter.ItemViewHolder<AlarmItemHolder>
        implements ItemAnimator.OnAnimateChangeListener {

    public static final float CLOCK_ENABLED_ALPHA = 1f;
    public static final float CLOCK_DISABLED_ALPHA = 0.63f;

    private final TextView editLabel;
    public final TextTime clock;
    public final CompoundButton onOff;
    public final TextView daysOfWeek;
    private final TextView upcomingInstanceLabel;
    public final ImageView arrow;

    public AlarmItemViewHolder(View itemView) {
        super(itemView);
        final Context context = itemView.getContext();

        editLabel = itemView.findViewById(R.id.edit_label);
        clock = itemView.findViewById(R.id.digital_clock);
        onOff = itemView.findViewById(R.id.onoff);
        daysOfWeek = itemView.findViewById(R.id.days_of_week);
        upcomingInstanceLabel = itemView.findViewById(R.id.upcoming_instance_label);
        arrow = itemView.findViewById(R.id.arrow);

        final MaterialCardView itemCardView = itemView.findViewById(R.id.item_card_view);
        final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
        final String darkMode = DataModel.getDataModel().getDarkMode();
        if (isCardBackgroundDisplayed) {
            itemCardView.setCardBackgroundColor(
                    MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.BLACK)
            );
        } else if (Utils.isNight(context.getResources()) && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
            itemCardView.setCardBackgroundColor(Color.BLACK);
        } else {
            itemCardView.setCardBackgroundColor(
                    MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK)
            );
        }

        final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();
        if (isCardBorderDisplayed) {
            itemCardView.setStrokeWidth(Utils.toPixel(2, context));
            itemCardView.setStrokeColor(
                    MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
            );
        }

        editLabel.setOnClickListener(view -> getAlarmTimeClickHandler().onEditLabelClicked(getItemHolder().item));

        onOff.setOnCheckedChangeListener((compoundButton, checked) ->
                getItemHolder().getAlarmTimeClickHandler().setAlarmEnabled(getItemHolder().item, checked));

        clock.setOnClickListener(v -> getAlarmTimeClickHandler().onClockClicked(getItemHolder().item));
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        final Alarm alarm = itemHolder.item;
        final Context context = itemView.getContext();
        bindEditLabel(context, alarm);
        bindOnOffSwitch(alarm);
        bindClock(alarm);
        bindRepeatText(context, alarm);
        bindUpcomingInstance(context, alarm);
        itemView.setContentDescription(clock.getText() + " " + alarm.getLabelOrDefault(context));
    }

    private void bindEditLabel(Context context, Alarm alarm) {
        if (alarm.equals(Alarm.getAlarmByLabel(context.getContentResolver(), BedtimeFragment.BEDTIME_LABEL))) {
            editLabel.setOnClickListener(null);
            editLabel.setBackgroundColor(Color.TRANSPARENT);
            editLabel.setText(context.getString(R.string.wakeup_alarm_label_visible));
            editLabel.setTypeface(Typeface.DEFAULT_BOLD);
            editLabel.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
            return;
        }

        if (alarm.label.isEmpty()) {
            editLabel.setText(context.getString(R.string.add_label));
            editLabel.setTypeface(Typeface.DEFAULT);
            editLabel.setAlpha(CLOCK_DISABLED_ALPHA);
        } else {
            editLabel.setText(alarm.label);
            editLabel.setContentDescription(alarm.label != null && !alarm.label.isEmpty()
                    ? context.getString(R.string.label_description) + " " + alarm.label
                    : context.getString(R.string.no_label_specified));
            editLabel.setTypeface(Typeface.DEFAULT_BOLD);
            editLabel.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        }
    }

    protected void bindOnOffSwitch(Alarm alarm) {
        if (onOff.isChecked() != alarm.enabled) {
            onOff.setChecked(alarm.enabled);
        }
    }

    protected void bindClock(Alarm alarm) {
        clock.setTime(alarm.hour, alarm.minutes);
        clock.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        clock.setTypeface(alarm.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    private void bindRepeatText(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final String daysOfWeekText = alarm.daysOfWeek.toString(context, weekdayOrder);
            final String contentDescription = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
            daysOfWeek.setVisibility(View.VISIBLE);
            daysOfWeek.setText(daysOfWeekText);
            daysOfWeek.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
            daysOfWeek.setContentDescription(contentDescription);
        } else {
            daysOfWeek.setVisibility(View.GONE);
        }
    }

    private void bindUpcomingInstance(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            upcomingInstanceLabel.setVisibility(View.GONE);
        } else {
            final String labelText;
            labelText = Alarm.isTomorrow(alarm, Calendar.getInstance())
                    ? context.getString(R.string.alarm_tomorrow)
                    : context.getString(R.string.alarm_today);
            upcomingInstanceLabel.setVisibility(View.VISIBLE);
            upcomingInstanceLabel.setText(labelText);
            upcomingInstanceLabel.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        }
    }

    private AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return getItemHolder().getAlarmTimeClickHandler();
    }

}

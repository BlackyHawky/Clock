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

import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;

import java.util.List;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * A ViewHolder containing views for an alarm item in expanded stated.
 */
public final class ExpandedAlarmViewHolder extends AlarmItemViewHolder {

    public static final int VIEW_TYPE = R.layout.alarm_time_expanded;

    public final CheckBox repeat;
    public final TextView editLabel;
    public final LinearLayout repeatDays;
    public final CompoundButton[] dayButtons = new CompoundButton[7];
    public final CheckBox vibrate;
    public final TextView ringtone;
    public final ImageButton delete;
    public final View hairLine;

    private final boolean mHasVibrator;

    public ExpandedAlarmViewHolder(View itemView, boolean hasVibrator) {
        super(itemView);

        itemView.setAccessibilityDelegate(
                new AlarmItemAccessibilityDelegate(R.string.collapse_description));
        final Context context = itemView.getContext();
        mHasVibrator = hasVibrator;
        final Resources.Theme theme = context.getTheme();
        int[] attrs = new int[] { android.R.attr.selectableItemBackground };

        final TypedArray typedArray = theme.obtainStyledAttributes(attrs);
        LayerDrawable background = new LayerDrawable(new Drawable[] {
                ContextCompat.getDrawable(context, R.drawable.alarm_background_expanded),
                typedArray.getDrawable(0) });
        itemView.setBackground(background);
        typedArray.recycle();

        delete = (ImageButton) itemView.findViewById(R.id.delete);
        repeat = (CheckBox) itemView.findViewById(R.id.repeat_onoff);
        vibrate = (CheckBox) itemView.findViewById(R.id.vibrate_onoff);
        ringtone = (TextView) itemView.findViewById(R.id.choose_ringtone);
        editLabel = (TextView) itemView.findViewById(R.id.edit_label);
        repeatDays = (LinearLayout) itemView.findViewById(R.id.repeat_days);
        hairLine = itemView.findViewById(R.id.hairline);

        // Build button for each day.
        final LayoutInflater inflater = LayoutInflater.from(context);
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflater.inflate(R.layout.day_button, repeatDays,
                    false /* attachToRoot */);
            final CompoundButton dayButton =
                    (CompoundButton) dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            repeatDays.addView(dayButtonFrame);
            dayButtons[i] = dayButton;
        }

        // Collapse handler
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().collapse();
            }
        });
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().collapse();
            }
        });
        // Edit time handler
        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAlarmTimeClickHandler().onClockClicked(getItemHolder().item);
            }
        });
        // Edit label handler
        editLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAlarmTimeClickHandler().onEditLabelClicked(getItemHolder().item);
            }
        });
        // Vibrator checkbox handler
        vibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAlarmTimeClickHandler().setAlarmVibrationEnabled(getItemHolder().item,
                        ((CheckBox) v).isChecked());
            }
        });
        // Ringtone editor handler
        ringtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAlarmTimeClickHandler().onRingtoneClicked(getItemHolder().item);
            }
        });
        // Delete alarm handler
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAlarmTimeClickHandler().onDeleteClicked(getItemHolder().item);
                v.announceForAccessibility(context.getString(R.string.alarm_deleted));
            }
        });
        // Repeat checkbox handler
        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean checked = ((CheckBox) view).isChecked();
                getAlarmTimeClickHandler().setAlarmRepeatEnabled(getItemHolder().item, checked);
            }
        });
        // Day buttons handler
        for (int i = 0; i < dayButtons.length; i++) {
            final int buttonIndex = i;
            dayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean isChecked = ((CompoundButton) view).isChecked();
                    getAlarmTimeClickHandler().setDayOfWeekEnabled(getItemHolder().item,
                            isChecked, buttonIndex);
                }
            });
        }
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();
        bindEditLabel(alarm);
        bindDaysOfWeekButtons(alarm);
        bindVibrator(alarm);
        bindRingtone(context, alarm);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
    }

    private void bindRingtone(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getAlarmRingtoneTitle(alarm.alert);
        ringtone.setText(title);

        final String description = context.getString(R.string.ringtone_description);
        ringtone.setContentDescription(description + " " + title);

        final boolean silent = Utils.RINGTONE_SILENT.equals(alarm.alert);
        final int startResId = silent ? R.drawable.ic_ringtone_silent : R.drawable.ic_ringtone;
        final Drawable startDrawable = Utils.getVectorDrawable(context, startResId);
        ringtone.setCompoundDrawablesWithIntrinsicBounds(startDrawable, null, null, null);
    }

    private void bindDaysOfWeekButtons(Alarm alarm) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = dayButtons[i];
            if (alarm.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(UiDataModel.getUiDataModel().getWindowBackgroundColor());
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(Color.WHITE);
            }
        }
        if (alarm.daysOfWeek.isRepeating()) {
            repeat.setChecked(true);
            repeatDays.setVisibility(View.VISIBLE);
        } else {
            repeat.setChecked(false);
            repeatDays.setVisibility(View.GONE);
        }
    }

    private void bindEditLabel(Alarm alarm) {
        if (alarm.label != null && alarm.label.length() > 0) {
            editLabel.setText(alarm.label);
        } else {
            editLabel.setText(R.string.label);
        }
    }

    private void bindVibrator(Alarm alarm) {
        if (!mHasVibrator) {
            vibrate.setVisibility(View.INVISIBLE);
        } else {
            vibrate.setVisibility(View.VISIBLE);
            vibrate.setChecked(alarm.vibrate);
        }
    }

    private AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return getItemHolder().getAlarmTimeClickHandler();
    }

    @Override
    public Animator onAnimateChange(final ViewHolder oldHolder, ViewHolder newHolder,
            long duration) {
        final boolean isExpanding = this == newHolder;

        if (Utils.isLMR1OrLater()) {
            arrow.setImageResource(isExpanding ? R.drawable.caret_toclose_animation
                    : R.drawable.caret_toopen_animation);
        }
        arrow.setVisibility(View.VISIBLE);
        clock.setVisibility(View.VISIBLE);
        onOff.setVisibility(View.VISIBLE);
        itemView.getBackground().setAlpha(isExpanding ? 0 : 255);
        setChangingViewsAlpha(isExpanding ? 0f : 1f);

        final Animator changeAnimatorSet = isExpanding
                ? createExpandingAnimator(oldHolder, duration)
                : createCollapsingAnimator(newHolder, duration);
        changeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AnimatorUtils.startDrawableAnimation(arrow);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                itemView.getBackground().setAlpha(255);
                clock.setVisibility(View.VISIBLE);
                onOff.setVisibility(View.VISIBLE);
                arrow.setVisibility(View.VISIBLE);
                arrow.setTranslationY(0f);
                setChangingViewsAlpha(1f);
                arrow.jumpDrawablesToCurrentState();
            }
        });
        return changeAnimatorSet;
    }

    private Animator createCollapsingAnimator(ViewHolder newHolder, long duration) {
        final boolean daysVisible = repeatDays.getVisibility() == View.VISIBLE;
        final View newView = newHolder.itemView;
        final int numberOfItems = countNumberOfItems();

        // Since the delete button's margin adds height to the overall container, we need to account
        // for that in calculating the arrow's movement.
        final ViewGroup.MarginLayoutParams deleteLayoutParams =
                (ViewGroup.MarginLayoutParams) delete.getLayoutParams();
        final View containerView = itemView.findViewById(R.id.alarm_container);
        float movement = (containerView.getBottom() - containerView.getTop()) + hairLine.getHeight()
                - (newView.getBottom() - newView.getTop()) - deleteLayoutParams.bottomMargin;

        // Create the animators.
        final Animator backgroundAnimator = ObjectAnimator.ofPropertyValuesHolder(itemView,
                PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 255, 0));
        backgroundAnimator.setDuration(duration);

        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(itemView,
                itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom(),
                newView.getLeft(), newView.getTop(), newView.getRight(), newView.getBottom());
        boundsAnimator.setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final long shortDuration = (long) (duration * ANIM_SHORT_DURATION_MULTIPLIER);
        final Animator repeatAnimation = ObjectAnimator.ofFloat(repeat, View.ALPHA, 0f)
                .setDuration(shortDuration);
        final Animator editLabelAnimation = ObjectAnimator.ofFloat(editLabel, View.ALPHA, 0f)
                .setDuration(shortDuration);
        final Animator repeatDaysAnimation = ObjectAnimator.ofFloat(repeatDays, View.ALPHA, 0f)
                .setDuration(shortDuration);
        final Animator vibrateAnimation = ObjectAnimator.ofFloat(vibrate, View.ALPHA, 0f)
                .setDuration(shortDuration);
        final Animator ringtoneAnimation = ObjectAnimator.ofFloat(ringtone, View.ALPHA, 0f)
                .setDuration(shortDuration);
        final Animator dismissAnimation = ObjectAnimator.ofFloat(preemptiveDismissButton,
                View.ALPHA, 0f).setDuration(shortDuration);
        final Animator deleteAnimation = ObjectAnimator.ofFloat(delete, View.ALPHA, 0f)
                .setDuration(shortDuration);
        final Animator hairLineAnimation = ObjectAnimator.ofFloat(hairLine, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator arrowAnimation = ObjectAnimator.ofFloat(arrow, View.TRANSLATION_Y, -movement)
                .setDuration(duration);
        arrowAnimation.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        // Set the staggered delays; use the first portion (duration * (1 - 1/4 - 1/6)) of the time,
        // so that the final animation, with a duration of 1/4 the total duration, finishes exactly
        // before the collapsed holder begins expanding.
        long startDelay = 0L;
        final long delayIncrement = (long) (duration * ANIM_LONG_DELAY_INCREMENT_MULTIPLIER)
                / (numberOfItems - 1);
        deleteAnimation.setStartDelay(startDelay);
        if (preemptiveDismissButton.getVisibility() == View.VISIBLE) {
            startDelay += delayIncrement;
            dismissAnimation.setStartDelay(startDelay);
        }
        hairLineAnimation.setStartDelay(startDelay);
        startDelay += delayIncrement;
        editLabelAnimation.setStartDelay(startDelay);
        startDelay += delayIncrement;
        vibrateAnimation.setStartDelay(startDelay);
        ringtoneAnimation.setStartDelay(startDelay);
        startDelay += delayIncrement;
        if (daysVisible) {
            repeatDaysAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }
        repeatAnimation.setStartDelay(startDelay);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(backgroundAnimator, boundsAnimator, repeatAnimation,
                repeatDaysAnimation, vibrateAnimation, ringtoneAnimation, editLabelAnimation,
                deleteAnimation, hairLineAnimation, dismissAnimation, arrowAnimation);
        return animatorSet;
    }

    private Animator createExpandingAnimator(ViewHolder oldHolder, long duration) {
        final boolean daysVisible = repeatDays.getVisibility() == View.VISIBLE;

        final Animator backgroundAnimator = ObjectAnimator.ofPropertyValuesHolder(itemView,
                PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 0, 255));
        backgroundAnimator.setDuration(duration);
        backgroundAnimator.setDuration(duration);

        // Since the delete button's margin adds height to the overall container, we need to account
        // for that in calculating the arrow's movement.
        final View oldView = oldHolder.itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(itemView,
                oldView.getLeft(), oldView.getTop(), oldView.getRight(), oldView.getBottom(),
                itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
        boundsAnimator.setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final ViewGroup.MarginLayoutParams deleteLayoutParams =
                (ViewGroup.MarginLayoutParams) delete.getLayoutParams();
        final View containerView = itemView.findViewById(R.id.alarm_container);
        float movement = (containerView.getBottom() - containerView.getTop()) + hairLine.getHeight()
                - (oldView.getBottom() - oldView.getTop()) - deleteLayoutParams.bottomMargin;

        final long longDuration = (long) (duration * ANIM_LONG_DURATION_MULTIPLIER);
        final Animator repeatAnimation = ObjectAnimator.ofFloat(repeat, View.ALPHA, 1f)
                .setDuration(longDuration);
        final Animator repeatDaysAnimation = ObjectAnimator.ofFloat(repeatDays, View.ALPHA, 1f)
                .setDuration(longDuration);
        final Animator ringtoneAnimation = ObjectAnimator.ofFloat(ringtone, View.ALPHA, 1f)
                .setDuration(longDuration);
        final Animator dismissAnimation = ObjectAnimator.ofFloat(preemptiveDismissButton,
                View.ALPHA, 1f).setDuration(longDuration);
        final Animator vibrateAnimation = ObjectAnimator.ofFloat(vibrate, View.ALPHA, 1f)
                .setDuration(longDuration);
        final Animator editLabelAnimation = ObjectAnimator.ofFloat(editLabel, View.ALPHA, 1f)
                .setDuration(longDuration);
        final Animator hairLineAnimation = ObjectAnimator.ofFloat(hairLine, View.ALPHA, 1f)
                .setDuration(longDuration);
        final Animator deleteAnimation = ObjectAnimator.ofFloat(delete, View.ALPHA, 1f)
                .setDuration(longDuration);
        final Animator arrowAnimation = ObjectAnimator.ofFloat(
                arrow, View.TRANSLATION_Y, -movement, 0f).setDuration(duration);
        arrowAnimation.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        // Set the stagger delays; delay the first by the amount of time it takes for the collapse
        // to complete, then stagger the expansion with the remaining time.
        long startDelay = (long) (duration * ANIM_STANDARD_DELAY_MULTIPLIER);
        final int numberOfItems = countNumberOfItems();
        final long delayIncrement = (long) (duration * ANIM_SHORT_DELAY_INCREMENT_MULTIPLIER)
                / (numberOfItems - 1);
        repeatAnimation.setStartDelay(startDelay);
        startDelay += delayIncrement;
        if (daysVisible) {
            repeatDaysAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }
        ringtoneAnimation.setStartDelay(startDelay);
        vibrateAnimation.setStartDelay(startDelay);
        startDelay += delayIncrement;
        editLabelAnimation.setStartDelay(startDelay);
        startDelay += delayIncrement;
        hairLineAnimation.setStartDelay(startDelay);
        if (preemptiveDismissButton.getVisibility() == View.VISIBLE) {
            dismissAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }
        deleteAnimation.setStartDelay(startDelay);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(backgroundAnimator, repeatAnimation, boundsAnimator,
                repeatDaysAnimation, vibrateAnimation, ringtoneAnimation, editLabelAnimation,
                deleteAnimation, hairLineAnimation, dismissAnimation, arrowAnimation);
        return animatorSet;
    }

    private int countNumberOfItems() {
        // Always between 4 and 6 items.
        int numberOfItems = 4;
        if (preemptiveDismissButton.getVisibility() == View.VISIBLE) {
            numberOfItems++;
        }
        if (repeatDays.getVisibility() == View.VISIBLE) {
            numberOfItems++;
        }
        return numberOfItems;
    }

    private void setChangingViewsAlpha(float alpha) {
        repeat.setAlpha(alpha);
        editLabel.setAlpha(alpha);
        repeatDays.setAlpha(alpha);
        vibrate.setAlpha(alpha);
        ringtone.setAlpha(alpha);
        hairLine.setAlpha(alpha);
        delete.setAlpha(alpha);
        preemptiveDismissButton.setAlpha(alpha);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mLayoutInflator;
        private final boolean mHasVibrator;

        public Factory(Context context, LayoutInflater layoutInflater) {
            mLayoutInflator = layoutInflater;
            mHasVibrator = ((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).hasVibrator();
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new ExpandedAlarmViewHolder(mLayoutInflator.inflate(
                    viewType, parent, false /* attachToRoot */), mHasVibrator);
        }
    }
}
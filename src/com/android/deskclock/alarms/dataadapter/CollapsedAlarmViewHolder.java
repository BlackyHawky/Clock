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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;
import java.util.List;

/**
 * A ViewHolder containing views for an alarm item in collapsed stated.
 */
public final class CollapsedAlarmViewHolder extends AlarmItemViewHolder {

    public static final int VIEW_TYPE = R.layout.alarm_time_collapsed;

    private final TextView alarmLabel;
    public final TextView daysOfWeek;
    private final TextView upcomingInstanceLabel;
    private final View hairLine;

    private CollapsedAlarmViewHolder(View itemView) {
        super(itemView);

        alarmLabel = (TextView) itemView.findViewById(R.id.label);
        daysOfWeek = (TextView) itemView.findViewById(R.id.days_of_week);
        upcomingInstanceLabel = (TextView) itemView.findViewById(R.id.upcoming_instance_label);
        hairLine = itemView.findViewById(R.id.hairline);

        // Expand handler
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                getItemHolder().expand();
            }
        });
        alarmLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                getItemHolder().expand();
            }
        });
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Events.sendAlarmEvent(R.string.action_expand, R.string.label_deskclock);
                getItemHolder().expand();
            }
        });
        // Edit time handler
        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().getAlarmTimeClickHandler().onClockClicked(getItemHolder().item);
                Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                getItemHolder().expand();
            }
        });

        itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onBindItemView(AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();
        bindRepeatText(context, alarm);
        bindReadOnlyLabel(context, alarm);
        bindUpcomingInstance(context, alarm);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
    }

    private void bindReadOnlyLabel(Context context, Alarm alarm) {
        if (alarm.label != null && alarm.label.length() != 0) {
            alarmLabel.setText(alarm.label);
            alarmLabel.setVisibility(View.VISIBLE);
            alarmLabel.setContentDescription(context.getString(R.string.label_description)
                    + " " + alarm.label);
        } else {
            alarmLabel.setVisibility(View.GONE);
        }
    }

    private void bindRepeatText(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final String daysOfWeekText = alarm.daysOfWeek.toString(context, weekdayOrder);
            daysOfWeek.setText(daysOfWeekText);

            final String string = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
            daysOfWeek.setContentDescription(string);

            daysOfWeek.setVisibility(View.VISIBLE);
        } else {
            daysOfWeek.setVisibility(View.GONE);
        }
    }

    private void bindUpcomingInstance(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            upcomingInstanceLabel.setVisibility(View.GONE);
        } else {
            upcomingInstanceLabel.setVisibility(View.VISIBLE);
            final String labelText = Alarm.isTomorrow(alarm, Calendar.getInstance()) ?
                    context.getString(R.string.alarm_tomorrow) :
                    context.getString(R.string.alarm_today);
            upcomingInstanceLabel.setText(labelText);
        }
    }

    @Override
    public Animator onAnimateChange(List<Object> payloads, int fromLeft, int fromTop, int fromRight,
            int fromBottom, long duration) {
        /* There are no possible partial animations for collapsed view holders. */
        return null;
    }

    @Override
    public Animator onAnimateChange(final ViewHolder oldHolder, ViewHolder newHolder,
            long duration) {
        if (!(oldHolder instanceof AlarmItemViewHolder)
                || !(newHolder instanceof AlarmItemViewHolder)) {
            return null;
        }

        final boolean isCollapsing = this == newHolder;
        setChangingViewsAlpha(isCollapsing ? 0f : 1f);

        final Animator changeAnimatorSet = isCollapsing
                ? createCollapsingAnimator((AlarmItemViewHolder) oldHolder, duration)
                : createExpandingAnimator((AlarmItemViewHolder) newHolder, duration);
        changeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
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

    private Animator createExpandingAnimator(AlarmItemViewHolder newHolder, long duration) {
        clock.setVisibility(View.INVISIBLE);
        onOff.setVisibility(View.INVISIBLE);
        arrow.setVisibility(View.INVISIBLE);

        final AnimatorSet alphaAnimatorSet = new AnimatorSet();
        alphaAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(alarmLabel, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(daysOfWeek, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(upcomingInstanceLabel, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(preemptiveDismissButton, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(hairLine, View.ALPHA, 0f));
        alphaAnimatorSet.setDuration((long) (duration * ANIM_SHORT_DURATION_MULTIPLIER));

        final View oldView = itemView;
        final View newView = newHolder.itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(oldView, oldView, newView)
                .setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimatorSet, boundsAnimator);
        return animatorSet;
    }

    private Animator createCollapsingAnimator(AlarmItemViewHolder oldHolder, long duration) {
        final AnimatorSet alphaAnimatorSet = new AnimatorSet();
        alphaAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(alarmLabel, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(daysOfWeek, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(upcomingInstanceLabel, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(preemptiveDismissButton, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(hairLine, View.ALPHA, 1f));
        final long standardDelay = (long) (duration * ANIM_STANDARD_DELAY_MULTIPLIER);
        alphaAnimatorSet.setDuration(standardDelay);
        alphaAnimatorSet.setStartDelay(duration - standardDelay);

        final View oldView = oldHolder.itemView;
        final View newView = itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(newView, oldView, newView)
                .setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final View oldArrow = oldHolder.arrow;
        final Rect oldArrowRect = new Rect(0, 0, oldArrow.getWidth(), oldArrow.getHeight());
        final Rect newArrowRect = new Rect(0, 0, arrow.getWidth(), arrow.getHeight());
        ((ViewGroup) newView).offsetDescendantRectToMyCoords(arrow, newArrowRect);
        ((ViewGroup) oldView).offsetDescendantRectToMyCoords(oldArrow, oldArrowRect);
        final float arrowTranslationY = oldArrowRect.bottom - newArrowRect.bottom;
        arrow.setTranslationY(arrowTranslationY);
        arrow.setVisibility(View.VISIBLE);
        clock.setVisibility(View.VISIBLE);
        onOff.setVisibility(View.VISIBLE);

        final Animator arrowAnimation = ObjectAnimator.ofFloat(arrow, View.TRANSLATION_Y, 0f)
                .setDuration(duration);
        arrowAnimation.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimatorSet, boundsAnimator, arrowAnimation);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AnimatorUtils.startDrawableAnimation(arrow);
            }
        });
        return animatorSet;
    }

    private void setChangingViewsAlpha(float alpha) {
        alarmLabel.setAlpha(alpha);
        daysOfWeek.setAlpha(alpha);
        upcomingInstanceLabel.setAlpha(alpha);
        hairLine.setAlpha(alpha);
        preemptiveDismissButton.setAlpha(alpha);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mLayoutInflater;

        public Factory(LayoutInflater layoutInflater) {
            mLayoutInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new CollapsedAlarmViewHolder(mLayoutInflater.inflate(
                    viewType, parent, false /* attachToRoot */));
        }
    }
}
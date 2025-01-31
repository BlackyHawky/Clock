/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.dataadapter;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.AnimatorUtils;

/**
 * A ViewHolder containing views for an alarm item in collapsed stated.
 */
public final class CollapsedAlarmViewHolder extends AlarmItemViewHolder {

    public static final int VIEW_TYPE = R.layout.alarm_time_collapsed;

    private CollapsedAlarmViewHolder(View itemView) {
        super(itemView);

        // Expand handler
        itemView.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
            getItemHolder().expand();
        });

        // Arrow handler
        arrow.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_expand, R.string.label_deskclock);
            getItemHolder().expand();
        });

        itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);

        // If this view is bound without coming from a ExpandedAlarmViewHolder (e.g.
        // when duplicating the alarm), the animation listeners won't do the showing
        // and therefore lead to unwanted non-visible state
        arrow.setVisibility(VISIBLE);
        editLabel.setVisibility(VISIBLE);
        clock.setVisibility(VISIBLE);
        onOff.setVisibility(VISIBLE);
        daysOfWeek.setVisibility(VISIBLE);
    }

    @Override
    public Animator onAnimateChange(final ViewHolder oldHolder, ViewHolder newHolder, long duration) {
        if (!(oldHolder instanceof AlarmItemViewHolder) || !(newHolder instanceof AlarmItemViewHolder)) {
            return null;
        }

        final boolean isCollapsing = this == newHolder;
        setChangingViewsAlpha(isCollapsing ? 0f : annotationsAlpha);

        final Animator changeAnimatorSet = isCollapsing
                ? createCollapsingAnimator((AlarmItemViewHolder) oldHolder, duration)
                : createExpandingAnimator((AlarmItemViewHolder) newHolder, duration);
        changeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                arrow.setTranslationY(0f);
                setChangingViewsAlpha(annotationsAlpha);
                arrow.jumpDrawablesToCurrentState();
            }
        });

        return changeAnimatorSet;
    }

    private Animator createExpandingAnimator(AlarmItemViewHolder newHolder, long duration) {
        final AnimatorSet alphaAnimatorSet = new AnimatorSet();
        alphaAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(preemptiveDismissButton, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(bottomPaddingView, View.ALPHA, 0f));
        alphaAnimatorSet.setDuration((long) (duration * ANIM_SHORT_DURATION_MULTIPLIER));

        final Animator boundsAnimator = getBoundsAnimator(itemView, newHolder.itemView, duration);
        final Animator editLabelAnimator = getBoundsAnimator(editLabel, newHolder.editLabel, duration);
        final Animator switchAnimator = getBoundsAnimator(onOff, newHolder.onOff, duration);
        final Animator clockAnimator = getBoundsAnimator(clock, newHolder.clock, duration);
        final Animator ellipseAnimator = getBoundsAnimator(daysOfWeek, newHolder.daysOfWeek, duration);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimatorSet, boundsAnimator, editLabelAnimator, switchAnimator,
                clockAnimator, ellipseAnimator);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                editLabel.setVisibility(INVISIBLE);
                clock.setVisibility(INVISIBLE);
                onOff.setVisibility(INVISIBLE);
                arrow.setVisibility(INVISIBLE);
                daysOfWeek.setVisibility(INVISIBLE);
            }
        });

        return animatorSet;
    }

    private Animator createCollapsingAnimator(AlarmItemViewHolder oldHolder, long duration) {
        final AnimatorSet alphaAnimatorSet = new AnimatorSet();
        alphaAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(daysOfWeek, View.ALPHA, annotationsAlpha),
                ObjectAnimator.ofFloat(preemptiveDismissButton, View.ALPHA, annotationsAlpha),
                ObjectAnimator.ofFloat(bottomPaddingView, View.ALPHA, annotationsAlpha));

        final long standardDelay = (long) (duration * ANIM_STANDARD_DELAY_MULTIPLIER);
        alphaAnimatorSet.setDuration(standardDelay);
        alphaAnimatorSet.setStartDelay(duration - standardDelay);

        final View newView = itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(newView, oldHolder.itemView, newView);
        boundsAnimator.setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final Animator arrowAnimation = ObjectAnimator.ofFloat(arrow, View.TRANSLATION_Y, 0f).setDuration(duration);
        arrowAnimation.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimatorSet, boundsAnimator, arrowAnimation);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                AnimatorUtils.startDrawableAnimation(arrow);
            }
        });

        return animatorSet;
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mLayoutInflater;

        public Factory(LayoutInflater layoutInflater) {
            mLayoutInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new CollapsedAlarmViewHolder(mLayoutInflater.inflate(viewType, parent, false));
        }
    }
}

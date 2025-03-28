/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static android.view.View.TRANSLATION_X;
import static android.view.View.TRANSLATION_Y;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.best.deskclock.utils.AnimatorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemAnimator extends SimpleItemAnimator {

    private final List<Animator> mAddAnimatorsList = new ArrayList<>();
    private final List<Animator> mRemoveAnimatorsList = new ArrayList<>();
    private final List<Animator> mChangeAnimatorsList = new ArrayList<>();
    private final List<Animator> mMoveAnimatorsList = new ArrayList<>();

    private final Map<ViewHolder, Animator> mAnimators = new ArrayMap<>();

    @Override
    public boolean animateRemove(final ViewHolder holder) {
        endAnimation(holder);

        final float prevAlpha = holder.itemView.getAlpha();

        final Animator removeAnimator = ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 0f);
        removeAnimator.setDuration(getRemoveDuration());
        removeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchRemoveStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(holder);
                holder.itemView.setAlpha(prevAlpha);
                dispatchRemoveFinished(holder);
            }
        });
        mRemoveAnimatorsList.add(removeAnimator);
        mAnimators.put(holder, removeAnimator);
        return true;
    }

    @Override
    public boolean animateAdd(final ViewHolder holder) {
        endAnimation(holder);

        final float prevAlpha = holder.itemView.getAlpha();
        holder.itemView.setAlpha(0f);

        final Animator addAnimator = ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 1f)
                .setDuration(getAddDuration());
        addAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchAddStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(holder);
                holder.itemView.setAlpha(prevAlpha);
                dispatchAddFinished(holder);
            }
        });
        mAddAnimatorsList.add(addAnimator);
        mAnimators.put(holder, addAnimator);
        return true;
    }

    @Override
    public boolean animateMove(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        endAnimation(holder);

        final int deltaX = toX - fromX;
        final int deltaY = toY - fromY;
        final long moveDuration = getMoveDuration();

        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }

        final View view = holder.itemView;
        final float prevTranslationX = view.getTranslationX();
        final float prevTranslationY = view.getTranslationY();
        view.setTranslationX(-deltaX);
        view.setTranslationY(-deltaY);

        final ObjectAnimator moveAnimator;
        if (deltaX != 0 && deltaY != 0) {
            final PropertyValuesHolder moveX = PropertyValuesHolder.ofFloat(TRANSLATION_X, 0f);
            final PropertyValuesHolder moveY = PropertyValuesHolder.ofFloat(TRANSLATION_Y, 0f);
            moveAnimator = ObjectAnimator.ofPropertyValuesHolder(holder.itemView, moveX, moveY);
        } else if (deltaX != 0) {
            final PropertyValuesHolder moveX = PropertyValuesHolder.ofFloat(TRANSLATION_X, 0f);
            moveAnimator = ObjectAnimator.ofPropertyValuesHolder(holder.itemView, moveX);
        } else {
            final PropertyValuesHolder moveY = PropertyValuesHolder.ofFloat(TRANSLATION_Y, 0f);
            moveAnimator = ObjectAnimator.ofPropertyValuesHolder(holder.itemView, moveY);
        }

        moveAnimator.setDuration(moveDuration);
        moveAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchMoveStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(holder);
                view.setTranslationX(prevTranslationX);
                view.setTranslationY(prevTranslationY);
                dispatchMoveFinished(holder);
            }
        });
        mMoveAnimatorsList.add(moveAnimator);
        mAnimators.put(holder, moveAnimator);

        return true;
    }

    @Override
    public boolean animateChange(@NonNull final ViewHolder oldHolder, @NonNull final ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {

        endAnimation(oldHolder);
        endAnimation(newHolder);

        if (oldHolder == newHolder) {
            dispatchChangeFinished(newHolder, false);
            return false;
        } else if (!(oldHolder instanceof OnAnimateChangeListener) || !(newHolder instanceof OnAnimateChangeListener)) {
            // Both holders must implement OnAnimateChangeListener in order to animate.
            dispatchChangeFinished(oldHolder, true);
            dispatchChangeFinished(newHolder, true);
            return false;
        }

        final long changeDuration = getChangeDuration();
        final Animator oldChangeAnimator = ((OnAnimateChangeListener) oldHolder).onAnimateChange(oldHolder, newHolder, changeDuration);
        if (oldChangeAnimator != null) {
            oldChangeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    dispatchChangeStarting(oldHolder, true);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.removeAllListeners();
                    mAnimators.remove(oldHolder);
                    dispatchChangeFinished(oldHolder, true);
                }
            });
            mAnimators.put(oldHolder, oldChangeAnimator);
            mChangeAnimatorsList.add(oldChangeAnimator);
        } else {
            dispatchChangeFinished(oldHolder, true);
        }

        final Animator newChangeAnimator = ((OnAnimateChangeListener) newHolder).onAnimateChange(oldHolder, newHolder, changeDuration);
        if (newChangeAnimator != null) {
            newChangeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    dispatchChangeStarting(newHolder, false);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.removeAllListeners();
                    mAnimators.remove(newHolder);
                    dispatchChangeFinished(newHolder, false);
                }
            });
            mAnimators.put(newHolder, newChangeAnimator);
            mChangeAnimatorsList.add(newChangeAnimator);
        } else {
            dispatchChangeFinished(newHolder, false);
        }

        return true;
    }

    @Override
    public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
        // Unused
        throw new IllegalStateException("This method should not be used");
    }

    @Override
    public void runPendingAnimations() {
        final AnimatorSet removeAnimatorSet = new AnimatorSet();
        removeAnimatorSet.playTogether(mRemoveAnimatorsList);
        mRemoveAnimatorsList.clear();

        final AnimatorSet addAnimatorSet = new AnimatorSet();
        addAnimatorSet.playTogether(mAddAnimatorsList);
        mAddAnimatorsList.clear();

        final AnimatorSet changeAnimatorSet = new AnimatorSet();
        changeAnimatorSet.playTogether(mChangeAnimatorsList);
        mChangeAnimatorsList.clear();

        final AnimatorSet moveAnimatorSet = new AnimatorSet();
        moveAnimatorSet.playTogether(mMoveAnimatorsList);
        mMoveAnimatorsList.clear();

        final AnimatorSet pendingAnimatorSet = new AnimatorSet();
        pendingAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                dispatchFinishedWhenDone();
            }
        });
        // Required order: removes, then changes & moves simultaneously, then additions. There are
        // redundant edges because changes or moves may be empty, causing the removes to incorrectly
        // play immediately.
        pendingAnimatorSet.play(removeAnimatorSet).before(changeAnimatorSet);
        pendingAnimatorSet.play(removeAnimatorSet).before(moveAnimatorSet);
        pendingAnimatorSet.play(changeAnimatorSet).with(moveAnimatorSet);
        pendingAnimatorSet.play(addAnimatorSet).after(changeAnimatorSet);
        pendingAnimatorSet.play(addAnimatorSet).after(moveAnimatorSet);
        pendingAnimatorSet.start();
    }

    @Override
    public void endAnimation(@NonNull ViewHolder holder) {
        final Animator animator = mAnimators.get(holder);

        mAnimators.remove(holder);
        mAddAnimatorsList.remove(animator);
        mRemoveAnimatorsList.remove(animator);
        mChangeAnimatorsList.remove(animator);
        mMoveAnimatorsList.remove(animator);

        if (animator != null) {
            animator.end();
        }

        dispatchFinishedWhenDone();
    }

    @Override
    public void endAnimations() {
        final List<Animator> animatorList = new ArrayList<>(mAnimators.values());
        for (Animator animator : animatorList) {
            animator.end();
        }
        dispatchFinishedWhenDone();
    }

    @Override
    public boolean isRunning() {
        return !mAnimators.isEmpty();
    }

    private void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull ViewHolder viewHolder, @NonNull List<Object> payloads) {
        final boolean defaultReusePolicy = super.canReuseUpdatedViewHolder(viewHolder, payloads);
        // Whenever we have a payload, this is an in-place animation.
        return !payloads.isEmpty() || defaultReusePolicy;
    }

    public interface OnAnimateChangeListener {
        Animator onAnimateChange(ViewHolder oldHolder, ViewHolder newHolder, long duration);
    }
}

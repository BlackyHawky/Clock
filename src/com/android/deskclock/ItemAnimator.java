/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.RecyclerView.State;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.view.View.TRANSLATION_Y;
import static android.view.View.TRANSLATION_X;

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
    public boolean animateChange(@NonNull final ViewHolder oldHolder,
            @NonNull final ViewHolder newHolder, @NonNull ItemHolderInfo preInfo,
            @NonNull ItemHolderInfo postInfo) {
        endAnimation(oldHolder);
        endAnimation(newHolder);

        final long changeDuration = getChangeDuration();
        List<Object> payloads = preInfo instanceof PayloadItemHolderInfo
                ? ((PayloadItemHolderInfo) preInfo).getPayloads() : null;

        if (oldHolder == newHolder) {
            final Animator animator = ((OnAnimateChangeListener) newHolder)
                    .onAnimateChange(payloads, preInfo.left, preInfo.top, preInfo.right,
                            preInfo.bottom, changeDuration);
            if (animator == null) {
                dispatchChangeFinished(newHolder, false);
                return false;
            }
            animator.addListener(new AnimatorListenerAdapter() {
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
            mChangeAnimatorsList.add(animator);
            mAnimators.put(newHolder, animator);
            return true;
        } else if (!(oldHolder instanceof OnAnimateChangeListener) ||
                !(newHolder instanceof OnAnimateChangeListener)) {
            // Both holders must implement OnAnimateChangeListener in order to animate.
            dispatchChangeFinished(oldHolder, true);
            dispatchChangeFinished(newHolder, true);
            return false;
        }

        final Animator oldChangeAnimator = ((OnAnimateChangeListener) oldHolder)
                .onAnimateChange(oldHolder, newHolder, changeDuration);
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

        final Animator newChangeAnimator = ((OnAnimateChangeListener) newHolder)
                .onAnimateChange(oldHolder, newHolder, changeDuration);
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
    public boolean animateChange(ViewHolder oldHolder,
            ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
        /* Unused */
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
    public void endAnimation(ViewHolder holder) {
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
    public @NonNull ItemHolderInfo recordPreLayoutInformation(@NonNull State state,
            @NonNull ViewHolder viewHolder, @AdapterChanges int changeFlags,
            @NonNull List<Object> payloads) {
        final ItemHolderInfo itemHolderInfo = super.recordPreLayoutInformation(state, viewHolder,
                changeFlags, payloads);
        if (itemHolderInfo instanceof PayloadItemHolderInfo) {
            ((PayloadItemHolderInfo) itemHolderInfo).setPayloads(payloads);
        }
        return itemHolderInfo;
    }

    @Override
    public ItemHolderInfo obtainHolderInfo() {
        return new PayloadItemHolderInfo();
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull ViewHolder viewHolder,
            @NonNull List<Object> payloads) {
        final boolean defaultReusePolicy = super.canReuseUpdatedViewHolder(viewHolder, payloads);
        // Whenever we have a payload, this is an in-place animation.
        return !payloads.isEmpty() || defaultReusePolicy;
    }

    private static final class PayloadItemHolderInfo extends ItemHolderInfo {
        private final List<Object> mPayloads = new ArrayList<>();

        void setPayloads(List<Object> payloads) {
            mPayloads.clear();
            mPayloads.addAll(payloads);
        }

        List<Object> getPayloads() {
            return mPayloads;
        }
    }

    public interface OnAnimateChangeListener {
        Animator onAnimateChange(ViewHolder oldHolder, ViewHolder newHolder, long duration);
        Animator onAnimateChange(List<Object> payloads, int fromLeft, int fromTop, int fromRight,
                int fromBottom, long duration);
    }
}
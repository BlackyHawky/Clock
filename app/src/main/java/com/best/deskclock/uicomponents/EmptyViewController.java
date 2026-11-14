/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionSet;

/**
 * Controller that displays empty view and handles animation appropriately.
 */
public final class EmptyViewController {

    private final Transition mEmptyViewTransition;
    private final View mContentView;
    private final View mEmptyView;
    private boolean mIsEmpty;

    /**
     * Constructor of the controller.
     *
     * @param contentView The view that should be displayed when empty view is hidden.
     * @param emptyView   The view that should be displayed when main view is empty.
     */
    public EmptyViewController(@NonNull View contentView, @NonNull View emptyView) {
        mContentView = contentView;
        mEmptyView = emptyView;
        mEmptyViewTransition = new TransitionSet()
            .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
            .addTarget(contentView)
            .addTarget(emptyView)
            .addTransition(new Fade(Fade.OUT))
            .addTransition(new Fade(Fade.IN));
    }

    /**
     * Sets the visibility state for the content and empty views.
     *
     * <p>Note: This method only updates visibility immediately. If an animation is desired,
     * a TransitionManager must be triggered by the caller before invoking this method.</p>
     *
     * @param isEmpty Whether the controller should transition into the empty state.
     */
    public void setEmpty(boolean isEmpty) {
        if (mIsEmpty == isEmpty) {
            return;
        }
        mIsEmpty = isEmpty;

        mEmptyView.setVisibility(mIsEmpty ? View.VISIBLE : View.GONE);
        mContentView.setVisibility(mIsEmpty ? View.GONE : View.VISIBLE);
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }

    public Transition getTransition() {
        return mEmptyViewTransition;
    }
}

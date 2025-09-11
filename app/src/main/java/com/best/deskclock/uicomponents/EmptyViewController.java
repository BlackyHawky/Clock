/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Controller that displays empty view and handles animation appropriately.
 */
public final class EmptyViewController {

    private static final int ANIMATION_DURATION = 300;
    private final Transition mEmptyViewTransition;
    private final ViewGroup mMainLayout;
    private final View mContentView;
    private final View mEmptyView;
    private boolean mIsEmpty;

    /**
     * Constructor of the controller.
     *
     * @param contentView The view that should be displayed when empty view is hidden.
     * @param emptyView   The view that should be displayed when main view is empty.
     */
    public EmptyViewController(ViewGroup mainLayout, View contentView, View emptyView) {
        mMainLayout = mainLayout;
        mContentView = contentView;
        mEmptyView = emptyView;
        mEmptyViewTransition = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
                .addTarget(contentView)
                .addTarget(emptyView)
                .addTransition(new Fade(Fade.OUT))
                .addTransition(new Fade(Fade.IN))
                .setDuration(ANIMATION_DURATION);
    }

    /**
     * Sets the state for the controller. If it's empty, it will display the empty view.
     *
     * @param isEmpty Whether or not the controller should transition into empty state.
     */
    public void setEmpty(boolean isEmpty) {
        if (mIsEmpty == isEmpty) {
            return;
        }
        mIsEmpty = isEmpty;
        // State changed, perform transition.
        TransitionManager.beginDelayedTransition(mMainLayout, mEmptyViewTransition);

        mEmptyView.setVisibility(mIsEmpty ? View.VISIBLE : View.GONE);
        mContentView.setVisibility(mIsEmpty ? View.GONE : View.VISIBLE);
    }
}

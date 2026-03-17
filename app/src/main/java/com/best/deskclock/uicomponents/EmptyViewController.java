/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.view.View;
import android.view.ViewGroup;

import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

/**
 * Controller that displays empty view and handles animation appropriately.
 */
public final class EmptyViewController {

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
                .addTransition(new Fade(Fade.IN));
    }

    /**
     * Sets the state for the controller. If it's empty, it will display the empty view.
     *
     * @param isEmpty        Whether or not the controller should transition into the empty state.
     * @param withTransition {@code true} to animate the state change using the predefined transition,
     *                       {@code false} to apply the visibility changes instantly without animation.
     */
    public void setEmpty(boolean isEmpty, boolean withTransition) {
        if (mIsEmpty == isEmpty) {
            return;
        }
        mIsEmpty = isEmpty;

        if (withTransition) {
            // State changed, perform transition.
            TransitionManager.beginDelayedTransition(mMainLayout, mEmptyViewTransition);
        }

        mEmptyView.setVisibility(mIsEmpty ? View.VISIBLE : View.GONE);
        mContentView.setVisibility(mIsEmpty ? View.GONE : View.VISIBLE);
    }

    public Transition getTransition() {
        return mEmptyViewTransition;
    }
}

/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widget.toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

/**
 * Custom {@link CoordinatorLayout.Behavior} that slides with the {@link Snackbar}.
 */
@Keep
public final class SnackbarSlidingBehavior extends CoordinatorLayout.Behavior<View> {

    public SnackbarSlidingBehavior(Context ignoredContext, AttributeSet ignoredAttrs) {
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        updateTranslationY(parent, child);
        return false;
    }

    @Override
    public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        updateTranslationY(parent, child);
    }

    @Override
    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull View child, int layoutDirection) {
        updateTranslationY(parent, child);
        return false;
    }

    private void updateTranslationY(CoordinatorLayout parent, View child) {
        float translationY = 0f;
        for (View dependency : parent.getDependencies(child)) {
            translationY = Math.min(translationY, dependency.getY() - child.getBottom());
        }
        child.setTranslationY(translationY);
    }
}

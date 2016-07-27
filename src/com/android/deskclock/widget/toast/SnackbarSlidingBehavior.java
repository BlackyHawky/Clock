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

package com.android.deskclock.widget.toast;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom {@link CoordinatorLayout.Behavior} that slides with the {@link Snackbar}.
 */
@Keep
public final class SnackbarSlidingBehavior extends CoordinatorLayout.Behavior<View> {

    public SnackbarSlidingBehavior(Context context, AttributeSet attrs) {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        updateTranslationY(parent, child);
        return false;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, View child, View dependency) {
        updateTranslationY(parent, child);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
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

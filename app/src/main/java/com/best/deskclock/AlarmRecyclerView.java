/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

/**
 * Thin wrapper around RecyclerView to prevent simultaneous layout passes, particularly during
 * animations.
 */
public class AlarmRecyclerView extends RecyclerView {

    private boolean mIgnoreRequestLayout;

    public AlarmRecyclerView(Context context) {
        this(context, null);
    }

    public AlarmRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlarmRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                // Disable scrolling/user action to prevent choppy animations.
                return Objects.requireNonNull(rv.getItemAnimator()).isRunning();
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mIgnoreRequestLayout = true;
        super.onLayout(changed, left, top, right, bottom);
        mIgnoreRequestLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mIgnoreRequestLayout &&
                (getItemAnimator() == null || !getItemAnimator().isRunning())) {
            super.requestLayout();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

}

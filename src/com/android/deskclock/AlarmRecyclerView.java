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

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 *  Thin wrapper around RecyclerView to prevent simultaneous layout passes, particularly during
 *  animations.
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
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                // Disable scrolling/user action to prevent choppy animations.
                return rv.getItemAnimator().isRunning();
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

}
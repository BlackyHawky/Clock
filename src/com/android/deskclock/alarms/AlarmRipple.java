/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.deskclock.alarms;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class AlarmRipple extends View {
    private static final int ALPHA = 150;
    private static final float OUTER_RADIUS = 400;

    private Paint mRipplePaint;
    private float mAlphaFactor;
    private float mCenterX;
    private float mCenterY;
    private float mTweenRadius;

    public AlarmRipple(Context context) {
        super(context);
    }

    public AlarmRipple(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlarmRipple(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AlarmRipple(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mRipplePaint == null) {
            mRipplePaint = new Paint();
            mRipplePaint.setAntiAlias(true);
            mRipplePaint.setColor(Color.WHITE);
        }

        final int alpha = (int) (ALPHA * mAlphaFactor + 0.5f);
        final float radius = lerp(0, OUTER_RADIUS, mTweenRadius);
        if (alpha > 0 && radius > 0) {
            mRipplePaint.setAlpha(alpha);
            mRipplePaint.setStyle(Style.FILL);
            canvas.drawCircle(mCenterX, mCenterY, radius, mRipplePaint);
        }
    }

    private float lerp(float start, float stop, float amount) {
        return start + (stop - start) * amount;
    }

    public void setCenterX(float x) {
        mCenterX = x;
    }

    public void setCenterY(float y) {
        mCenterY = y;
    }

    public void setAlphaFactor(float a) {
        mAlphaFactor = a;
        invalidate();
    }

    public void setRadiusGravity(float r) {
        mTweenRadius = r;
        invalidate();
    }
}
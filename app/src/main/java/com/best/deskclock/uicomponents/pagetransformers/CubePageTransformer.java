/*
 * SPDX-License-Identifier: Apache-2.0
 * Source: https://github.com/dipanshukr/Viewpager-Transformation/wiki/Cube-Out-Transformation
 * modified
 */

package com.best.deskclock.uicomponents.pagetransformers;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class CubePageTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(@NonNull View view, float position) {
        int pageWidth = view.getWidth();

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(0);
            view.setVisibility(INVISIBLE);
        } else if (position <= 0) { // [-1,0]
            view.setVisibility(VISIBLE);
            view.setAlpha(1);
            view.setPivotX(pageWidth);
            view.setRotationY(-90 * Math.abs(position));
        } else if (position < 1) { // (0,1]
            view.setVisibility(VISIBLE);
            view.setAlpha(1);
            view.setPivotX(0);
            view.setRotationY(90 * Math.abs(position));
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setVisibility(INVISIBLE);
            view.setAlpha(0);
        }
    }
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Source: https://github.com/dipanshukr/Viewpager-Transformation/wiki/Vertical-Flip-Transformation
 * modified
 */

package com.best.deskclock.uicomponents.pagetransformers;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class FlipPageTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(@NonNull View view, float position) {
        int pageWidth = view.getWidth();

        view.setTranslationX(-position * pageWidth);
        view.setCameraDistance(12000);

        if (position < 0.5 && position > -0.5) {
            view.setVisibility(VISIBLE);
        } else {
            view.setVisibility(INVISIBLE);
        }

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(0);
        } else if (position <= 0) { // [-1,0]
            view.setAlpha(1);
            view.setRotationY(180 * (1 - Math.abs(position) + 1));
        } else if (position <= 1) { // (0,1]
            view.setAlpha(1);
            view.setRotationY(-180 * (1 - Math.abs(position) + 1));
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(0);
        }
    }
}

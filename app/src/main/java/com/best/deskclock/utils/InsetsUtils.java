// SPDX-License-Identifier: GPL-3.0-only

// Source: Android Developers, Chris Banes
// https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
// Converted to Java and slightly modified

package com.best.deskclock.utils;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utility class for handling window insets in a consistent and reusable way.
 * <p>
 * This class provides helper methods to apply system window insets
 * (such as status bar, navigation bar, and display cutouts) to views
 * while preserving their initial padding.
 * <p>
 * It ensures that insets are requested when the view is attached to the window,
 * making it safe to call from early lifecycle methods such as onCreate().</p>
 */
public class InsetsUtils {

    /**
     * Method to apply insets to a view.
     */
    public static void doOnApplyWindowInsets(@NonNull View view, @NonNull OnApplyWindowInsetsListener listener) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            listener.onApply(v, insets);
            return WindowInsetsCompat.CONSUMED;
        });

        requestApplyInsetsWhenAttached(view);
    }

    /**
     * Interface for the inset listener.
     */
    public interface OnApplyWindowInsetsListener {
        void onApply(@NonNull View v, @NonNull WindowInsetsCompat insets);
    }

    /**
     * Request insets to be applied when the view is attached.
     */
    private static void requestApplyInsetsWhenAttached(@NonNull View view) {
        if (view.isAttachedToWindow()) {
            // If the view is already attached, the insets are applied immediately
            ViewCompat.requestApplyInsets(view);
        } else {
            // If the view is not attached, we add a listener to wait for the attachment
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    v.removeOnAttachStateChangeListener(this);
                    ViewCompat.requestApplyInsets(v);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                }
            });
        }
    }

    /**
     * Some flip phones do not report the cameras of the cover screen in the window insets
     * when the app uses the full cover screen, but the window metrics do.
     *
     * @return the height of the display cutout at the bottom of the window, or 0 if there is none.
     */
    public static int getBottomCutoutHeight(@NonNull Context context) {
        if (!SdkUtils.isAtLeastAndroid11()) {
            return 0;
        }

        WindowManager windowManager = context.getSystemService(WindowManager.class);
        if (windowManager == null) {
            return 0;
        }

        return getBottomCutoutHeight(windowManager.getCurrentWindowMetrics());
    }

    /**
     * @return the bottom display cutout inset of the given window metrics, never more than the window height.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public static int getBottomCutoutHeight(@NonNull WindowMetrics windowMetrics) {
        int cutoutHeight = windowMetrics.getWindowInsets().getInsets(WindowInsets.Type.displayCutout()).bottom;
        return Math.min(cutoutHeight, windowMetrics.getBounds().height());
    }

}

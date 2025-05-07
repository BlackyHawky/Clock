// SPDX-License-Identifier: GPL-3.0-only

// Source: Android Developers, Chris Banes
// https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
// Converted to Java and slightly modified

package com.best.deskclock.utils;

import android.view.View;

import androidx.annotation.NonNull;
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
    public static void doOnApplyWindowInsets(View view, OnApplyWindowInsetsListener listener) {
        InitialPadding initialPadding = recordInitialPaddingForView(view);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            listener.onApply(v, insets, initialPadding);
            return WindowInsetsCompat.CONSUMED;
        });

        requestApplyInsetsWhenAttached(view);
    }

    /**
     * Interface for the inset listener.
     */
    public interface OnApplyWindowInsetsListener {
        void onApply(View v, WindowInsetsCompat insets, InitialPadding initialPadding);
    }

    /**
     * Class to store the initial state of the view padding.
     */
    public record InitialPadding(int left, int top, int right, int bottom) {
    }

    /**
     * Save the initial state of the view padding.
     */
    private static InitialPadding recordInitialPaddingForView(View view) {
        return new InitialPadding(view.getPaddingLeft(), view.getPaddingTop(),
                view.getPaddingRight(), view.getPaddingBottom());
    }

    /**
     * Request insets to be applied when the view is attached.
     */
    private static void requestApplyInsetsWhenAttached(View view) {
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
                    // Do nothing
                }
            });
        }
    }
}

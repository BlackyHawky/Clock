// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uicomponents;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.DialogMessageCustomBinding;
import com.best.deskclock.databinding.DialogTitleCustomBinding;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CustomDialog {

    public static AlertDialog create(
        @NonNull Context context,
        @Nullable @StyleRes Integer styleRes,
        @Nullable Drawable icon,
        @Nullable CharSequence title,
        @Nullable CharSequence message,
        @Nullable View customView,
        @Nullable CharSequence positiveText,
        @Nullable DialogInterface.OnClickListener positiveListener,
        @Nullable CharSequence negativeText,
        @Nullable DialogInterface.OnClickListener negativeListener,
        @Nullable CharSequence neutralText,
        @Nullable DialogInterface.OnClickListener neutralListener,
        @Nullable OnDialogReady onDialogReady,
        @NonNull SoftInputMode softInputMode
    ) {

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        // Builder
        MaterialAlertDialogBuilder builder = (styleRes != null)
            ? new MaterialAlertDialogBuilder(context, styleRes)
            : new MaterialAlertDialogBuilder(context);

        // Title and icon
        if (title != null || icon != null) {
            DialogTitleCustomBinding titleBinding = DialogTitleCustomBinding.inflate(LayoutInflater.from(context));

            if (icon != null) {
                titleBinding.dialogTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                titleBinding.dialogTitle.setCompoundDrawablePadding((int) dpToPx(18, context.getResources().getDisplayMetrics()));
            }

            if (title != null) {
                titleBinding.dialogTitle.setText(title);
                titleBinding.dialogTitle.setTypeface(typeface);
            }

            builder.setCustomTitle(titleBinding.getRoot());
        }

        // Dialog view
        View dialogContent = null;

        // Message
        if (message != null) {
            DialogMessageCustomBinding messageBinding = DialogMessageCustomBinding.inflate(LayoutInflater.from(context));
            messageBinding.dialogMessage.setText(message);
            messageBinding.dialogMessage.setTypeface(typeface);

            dialogContent = messageBinding.getRoot();

            builder.setView(dialogContent);
        } else if (customView != null) {
            dialogContent = customView;

            builder.setView(dialogContent);
        }

        final View finalDialogContent = dialogContent;

        if (positiveText != null) {
            builder.setPositiveButton(positiveText, positiveListener);
        }
        if (negativeText != null) {
            builder.setNegativeButton(negativeText, negativeListener);
        }
        if (neutralText != null) {
            builder.setNeutralButton(neutralText, neutralListener);
        }

        AlertDialog dialog = builder.create();

        // Apply typeface to buttons
        dialog.setOnShowListener(d -> {
            if (finalDialogContent != null && finalDialogContent.findViewById(R.id.scroll_view) != null) {
                configureScrollView(finalDialogContent);
            }

            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            if (positive != null) {
                positive.setTypeface(typeface);
            }
            if (negative != null) {
                negative.setTypeface(typeface);
            }
            if (neutral != null) {
                neutral.setTypeface(typeface);
            }

            if (onDialogReady != null) {
                onDialogReady.onReady(dialog);
            }
        });

        // Soft input mode
        if (softInputMode == SoftInputMode.SHOW_KEYBOARD) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                );
            }
        }

        return dialog;
    }

    private static void configureScrollView(View dialogView) {
        NestedScrollView scrollView = dialogView.findViewById(R.id.scroll_view);

        boolean scrollable = scrollView.canScrollVertically(1) || scrollView.canScrollVertically(-1);

        if (scrollable) {
            scrollView.setScrollIndicators(View.SCROLL_INDICATOR_BOTTOM);
        }

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

            scrollView.setScrollIndicators(View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM);

            boolean atTop = !scrollView.canScrollVertically(-1);
            boolean atBottom = !scrollView.canScrollVertically(1);

            if (atTop) {
                scrollView.setScrollIndicators(View.SCROLL_INDICATOR_BOTTOM);
            }
            if (atBottom) {
                scrollView.setScrollIndicators(View.SCROLL_INDICATOR_TOP);
            }
        });
    }

    public interface OnDialogReady {
        void onReady(AlertDialog dialog);
    }

    public enum SoftInputMode {
        NONE,
        SHOW_KEYBOARD
    }

}

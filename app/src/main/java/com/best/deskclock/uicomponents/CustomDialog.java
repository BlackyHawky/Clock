// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uicomponents;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
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
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.NestedScrollView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
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

        LayoutInflater inflater = LayoutInflater.from(context);

        // Title
        @SuppressLint("InflateParams")
        View titleView = inflater.inflate(R.layout.dialog_title_custom, null);
        TextView titleText = titleView.findViewById(R.id.dialog_title);

        if (icon != null) {
            titleText.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            titleText.setCompoundDrawablePadding((int) dpToPx(18, context.getResources().getDisplayMetrics()));
        }

        if (title != null) {
            titleText.setText(title);
            titleText.setTypeface(typeface);
        }

        // Dialog view
        View dialogContent;

        if (message != null) {
            // Message
            @SuppressLint("InflateParams")
            View messageView = inflater.inflate(R.layout.dialog_message_custom, null);
            TextView messageText = messageView.findViewById(R.id.dialog_message);
            messageText.setText(message);
            messageText.setTypeface(typeface);

            dialogContent = messageView;
        } else {
            dialogContent = customView;
        }

        // Builder
        MaterialAlertDialogBuilder builder = (styleRes != null)
            ? new MaterialAlertDialogBuilder(context, styleRes)
            : new MaterialAlertDialogBuilder(context);

        builder
            .setCustomTitle(titleView)
            .setView(dialogContent);

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
            if (dialogContent != null && dialogContent.findViewById(R.id.scrollView) != null) {
                configureScrollView(dialogContent);
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

    public static AlertDialog createSimpleDialog(
        Context context,
        @DrawableRes int iconRes,
        @StringRes int titleRes,
        CharSequence message,
        @StringRes int positiveTextRes,
        DialogInterface.OnClickListener positiveListener
    ) {
        return create(
            context,
            null,
            AppCompatResources.getDrawable(context, iconRes),
            context.getString(titleRes),
            message,
            null,
            context.getString(positiveTextRes),
            positiveListener,
            context.getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            SoftInputMode.NONE
        );
    }

    private static void configureScrollView(View dialogView) {
        NestedScrollView scrollView = dialogView.findViewById(R.id.scrollView);

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

    public static class Builder {
        private final Context context;
        private Integer styleRes;
        private Drawable icon;
        private CharSequence title;
        private CharSequence message;
        private View customView;
        private CharSequence positiveText;
        private DialogInterface.OnClickListener positiveListener;
        private CharSequence negativeText;
        private DialogInterface.OnClickListener negativeListener;
        private CharSequence neutralText;
        private DialogInterface.OnClickListener neutralListener;
        private OnDialogReady onDialogReady;
        private SoftInputMode softInputMode = SoftInputMode.NONE;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        public Builder setStyle(@StyleRes int styleRes) {
            this.styleRes = styleRes;
            return this;
        }

        public Builder setIcon(@Nullable Drawable icon) {
            this.icon = icon;
            return this;
        }

        public Builder setIcon(@DrawableRes int iconRes) {
            this.icon = AppCompatResources.getDrawable(context, iconRes);
            return this;
        }

        public Builder setTitle(@StringRes int titleRes) {
            this.title = context.getString(titleRes);
            return this;
        }

        public Builder setTitle(@Nullable CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(@StringRes int messageRes) {
            this.message = context.getString(messageRes);
            return this;
        }

        public Builder setMessage(@Nullable CharSequence message) {
            this.message = message;
            return this;
        }

        public Builder setView(@Nullable View customView) {
            this.customView = customView;
            return this;
        }

        public Builder setPositiveButton(@StringRes int textRes, @Nullable DialogInterface.OnClickListener listener) {
            this.positiveText = context.getString(textRes);
            this.positiveListener = listener;
            return this;
        }

        public Builder setPositiveButton(@Nullable CharSequence text, @Nullable DialogInterface.OnClickListener listener) {
            this.positiveText = text;
            this.positiveListener = listener;
            return this;
        }

        public Builder setNegativeButton(@StringRes int textRes, @Nullable DialogInterface.OnClickListener listener) {
            this.negativeText = context.getString(textRes);
            this.negativeListener = listener;
            return this;
        }

        public Builder setNegativeButton(@Nullable CharSequence text, @Nullable DialogInterface.OnClickListener listener) {
            this.negativeText = text;
            this.negativeListener = listener;
            return this;
        }

        public Builder setNeutralButton(@StringRes int textRes, @Nullable DialogInterface.OnClickListener listener) {
            this.neutralText = context.getString(textRes);
            this.neutralListener = listener;
            return this;
        }

        public Builder setNeutralButton(@Nullable CharSequence text, @Nullable DialogInterface.OnClickListener listener) {
            this.neutralText = text;
            this.neutralListener = listener;
            return this;
        }

        public Builder setOnDialogReady(@Nullable OnDialogReady onDialogReady) {
            this.onDialogReady = onDialogReady;
            return this;
        }

        public Builder setSoftInputMode(@NonNull SoftInputMode softInputMode) {
            this.softInputMode = softInputMode;
            return this;
        }

        public AlertDialog build() {
            return create(
                context, styleRes, icon, title, message, customView,
                positiveText, positiveListener,
                negativeText, negativeListener,
                neutralText, neutralListener,
                onDialogReady, softInputMode
            );
        }

        public AlertDialog show() {
            AlertDialog dialog = build();
            dialog.show();
            return dialog;
        }
    }

    public interface OnDialogReady {
        void onReady(AlertDialog dialog);
    }

    public enum SoftInputMode {
        NONE,
        SHOW_KEYBOARD
    }

}

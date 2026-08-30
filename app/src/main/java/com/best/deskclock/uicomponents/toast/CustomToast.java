// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uicomponents.toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.best.deskclock.R;
import com.best.deskclock.utils.ThemeUtils;

/**
 * Utility class for displaying custom styled toasts using the application's
 * selected accent color and font preferences.
 *
 * <p>This class automatically applies the correct themed context so that the toast
 * respects the user's accent color, night mode settings, and custom font.</p>
 */
public class CustomToast {

    /**
     * Inflates and configures the custom toast layout.
     *
     * <p>This method applies the themed context, sets the message text,
     * applies the appropriate background and font, and returns the fully
     * prepared view ready to be used inside a Toast.</p>
     *
     * @param context     the base context used to resolve theme and resources
     * @param accentStyle the resolved accent color (taking auto night mode into account).
     * @param typeface    the {@link Typeface} applied to the text
     * @param message     the text to display inside the toast
     * @return a fully configured toast layout view
     */
    @NonNull
    private static View createLayout(@NonNull Context context, int accentStyle, @Nullable Typeface typeface, @NonNull String message) {

        Context themedContext = ThemeUtils.getThemedContext(context, accentStyle);
        DisplayMetrics displayMetrics = themedContext.getResources().getDisplayMetrics();

        @SuppressLint("InflateParams")
        View layout = LayoutInflater.from(themedContext).inflate(R.layout.custom_toast, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setBackground(
            ThemeUtils.pillBackgroundFromAttr(themedContext, displayMetrics, com.google.android.material.R.attr.colorSecondary)
        );
        text.setText(message);

        if (typeface != null) {
            text.setTypeface(typeface);
        }

        return layout;
    }

    /**
     * Creates a Toast instance using the given layout and duration.
     *
     * <p>The toast is built using the themed context to ensure proper
     * styling across light and dark modes. The returned Toast is not
     * shown automatically and must be displayed by the caller.</p>
     *
     * <p>Note: even if {@code Toast.setView(View)} is obsolete, its use is not problematic;
     * indeed, apps targeting API level 30 or higher that are in the background will
     * not have custom toast views displayed.</p>
     *
     * @param layout   the custom layout to display inside the toast
     * @param duration the toast duration (e.g., Toast.LENGTH_SHORT or Toast.LENGTH_LONG)
     * @return a configured Toast instance ready to be shown
     */
    @NonNull
    private static Toast buildToast(@NonNull View layout, int duration) {
        Toast toast = new Toast(layout.getContext());
        toast.setDuration(duration);
        //noinspection deprecation
        toast.setView(layout);

        return toast;
    }

    /**
     * Displays a short custom toast using a string resource.
     */
    public static void show(@NonNull Context context, int accentStyle, @Nullable Typeface typeface, @StringRes int messageRes) {
        View layout = createLayout(context, accentStyle, typeface, context.getString(messageRes));
        buildToast(layout, Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a long-duration custom toast with the given text.
     */
    public static void showLong(@NonNull Context context, int accentStyle, @Nullable Typeface typeface, @NonNull String message) {
        View layout = createLayout(context, accentStyle, typeface, message);
        buildToast(layout, Toast.LENGTH_LONG).show();
    }

    /**
     * Displays a long-duration custom toast while ensuring that any previously
     * shown toast is canceled before displaying the new one.
     *
     * <p>This is useful in situations where multiple toasts may be triggered in
     * quick succession, such as alarm snooze actions.</p>
     */
    public static void showLongWithManager(@NonNull Context context, int accentStyle, @Nullable Typeface typeface,
                                           @NonNull String message) {

        View layout = createLayout(context, accentStyle, typeface, message);
        Toast toast = buildToast(layout, Toast.LENGTH_LONG);

        ToastManager.setToast(toast);
        toast.show();
    }

}

package com.best.deskclock.uicomponents;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;

/**
 * Utility class for displaying custom tooltips anchored to views.
 *
 * <p>This class replaces the default system tooltips with fully customizable
 * popup tooltips that can appear above or below a target view. The tooltip
 * layout, colors, and typeface can be styled freely, allowing consistent
 * visual integration with the application's theme.</p>
 *
 * <p>Tooltips are shown for a short duration and automatically dismissed.</p>
 */
public class CustomTooltip {

    private static final int TOOLTIP_DURATION = 2000;

    /**
     * Displays a custom tooltip above the given anchor view.
     *
     * <p>This is a convenience method that delegates to the internal
     * {@link #show(View, String, Position)} method using the ABOVE position.</p>
     *
     * @param anchor the view above which the tooltip should appear
     * @param text   the text to display inside the tooltip
     */
    public static void showAbove(View anchor, String text) {
        show(anchor, text, Position.ABOVE);
    }

    /**
     * Displays a custom tooltip below the given anchor view.
     *
     * <p>This is a convenience method that delegates to the internal
     * {@link #show(View, String, Position)} method using the BELOW position.</p>
     *
     * @param anchor the view under which the tooltip should appear
     * @param text   the text to display inside the tooltip
     */
    public static void showBelow(View anchor, String text) {
        show(anchor, text, Position.BELOW);
    }

    /**
     * Internal method that creates and displays a custom tooltip anchored to a view.
     *
     * <p>The tooltip is horizontally centered relative to the anchor and positioned
     * either above or below it depending on the specified {@link Position}.</p>
     *
     * @param anchor   the view used as the reference point for positioning
     * @param text     the text to display inside the tooltip
     * @param position whether the tooltip should appear above or below the anchor
     */
    private static void show(View anchor, String text, Position position) {
        Context context = anchor.getContext();
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        // Inflate layout
        @SuppressLint("InflateParams")
        View tooltipView = LayoutInflater.from(context).inflate(R.layout.custom_tooltip, null);
        TextView tooltipText = tooltipView.findViewById(R.id.tooltip_text);
        tooltipText.setText(text);
        tooltipText.setTypeface(typeface);

        // Create a popup window
        PopupWindow popup = new PopupWindow(
                tooltipView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
        );

        popup.setBackgroundDrawable(ThemeUtils.pillBackgroundFromAttr(
                context, com.google.android.material.R.attr.colorSecondary)
        );
        popup.setOutsideTouchable(true);

        // Position
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);

        tooltipView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int tooltipWidth = tooltipView.getMeasuredWidth();
        int tooltipHeight = tooltipView.getMeasuredHeight();
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        // Horizontal centering
        int x = location[0] + (anchorWidth / 2) - (tooltipWidth / 2);

        // Vertical position
        int y = position == Position.BELOW
                ? location[1] + anchorHeight
                : location[1] - tooltipHeight;

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);

        tooltipView.postDelayed(popup::dismiss, TOOLTIP_DURATION);
    }

    private enum Position {ABOVE, BELOW}

}

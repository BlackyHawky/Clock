/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;

import com.best.deskclock.R;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilities for formatting strings.
 */
public class FormattedTextUtils {

    private FormattedTextUtils() {
    }

    /**
     * Applies a span over the length of the given text.
     *
     * @param text the {@link CharSequence} to be formatted
     * @param span the span to apply
     * @return the text with the span applied
     */
    public static CharSequence formatText(CharSequence text, Object span) {
        if (text == null) {
            return null;
        }

        final SpannableString formattedText = SpannableString.valueOf(text);
        formattedText.setSpan(span, 0, formattedText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return formattedText;
    }

    /**
     * Capitalizes the first letter of a string of characters according to the locale.
     */
    public static String capitalizeFirstLetter(String text, Locale locale) {

        if (text == null || text.isEmpty()) {
            return text != null ? text : "";
        }

        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return text.substring(0, i)
                    + text.substring(i, i + 1).toUpperCase(locale)
                    + text.substring(i + 1);
            }
        }

        return text;
    }

    /**
     * Retrieves a localized plural string resource and formats the injected quantity
     * according to the user's current locale.
     *
     * @param context  the context used to access the resources
     * @param id       the resource ID of the plural string (e.g., R.plurals.alarm_alert)
     * @param quantity the number used to evaluate the plural rule and to format into the string
     * @return the fully resolved and formatted plural string
     */
    public static String getNumberFormattedQuantityString(Context context, int id, int quantity) {
        final String localizedQuantity = NumberFormat.getInstance().format(quantity);
        return context.getResources().getQuantityString(id, quantity, localizedQuantity);
    }

    /**
     * @param context The context from which to obtain strings
     * @param hours   Hours to display (if any)
     * @param minutes Minutes to display (if any)
     * @param seconds Seconds to display
     * @return Provided time formatted as a String
     */
    public static String getTimeString(Context context, int hours, int minutes, int seconds) {
        if (hours != 0) {
            return context.getString(R.string.hours_minutes_seconds, hours, minutes, seconds);
        }
        if (minutes != 0) {
            return context.getString(R.string.minutes_seconds, minutes, seconds);
        }
        return context.getString(R.string.seconds_only, seconds);
    }
}

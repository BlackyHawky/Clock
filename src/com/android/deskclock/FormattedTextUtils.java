/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.text.Spannable;
import android.text.SpannableString;

/**
 * Utilities for formatting strings using spans.
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
}

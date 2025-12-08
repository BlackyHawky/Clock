// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uicomponents;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;

/**
 * CustomTypefaceSpan allows applying a specific {@link Typeface} to a span of text.
 *
 * <p>Unlike the default {@link android.text.style.TypefaceSpan} which historically only accepted
 * a font family name, this implementation can directly use a {@link Typeface} object
 * (including custom fonts loaded from file).</p>
 *
 * <p>On API levels prior to 28, {@link TypefaceSpan} does not provide a constructor
 * that accepts a {@link Typeface}. This class ensures consistent rendering of custom
 * typefaces across all supported Android versions.</p>
 *
 * <p>The selected text is rendered with the provided {@link Typeface}, both for measuring
 * and drawing, regardless of the platform version.</p>
 */
public class CustomTypefaceSpan extends TypefaceSpan {

    private final Typeface newTypeFace;

    public CustomTypefaceSpan(Typeface typeface) {
        super("");
        newTypeFace = typeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        applyCustomTypeFace(textPaint, newTypeFace);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        applyCustomTypeFace(textPaint, newTypeFace);
    }

    private static void applyCustomTypeFace(Paint paint, Typeface typeface) {
        paint.setTypeface(typeface);
    }

}

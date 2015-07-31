package com.android.deskclock;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

/**
 * Subclass of NumberPicker that allows customizing divider color.
 */
public class NumberPickerCompat extends NumberPicker {

    private static Field sSelectionDivider;
    private static boolean sTrySelectionDivider = true;

    public NumberPickerCompat(Context context) {
        this(context, null /* attrs */);
    }

    public NumberPickerCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        tintSelectionDivider(context);
    }

    public NumberPickerCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        tintSelectionDivider(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void tintSelectionDivider(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // Accent color in KK will stay system blue, so leave divider color matching.
            // The divider is correctly tinted to controlColorNormal in M.
            return;
        }

        if (sTrySelectionDivider) {
            final TypedArray a = context.obtainStyledAttributes(
                    new int[] { android.R.attr.colorControlNormal });
             // White is default color if colorControlNormal is not defined.
            final int color = a.getColor(0, Color.WHITE);
            a.recycle();

            try {
                if (sSelectionDivider == null) {
                    sSelectionDivider = NumberPicker.class.getDeclaredField("mSelectionDivider");
                    sSelectionDivider.setAccessible(true);
                }
                final Drawable selectionDivider = (Drawable) sSelectionDivider.get(this);
                if (selectionDivider != null) {
                    // setTint is API21+, but this will only be called in API21
                    selectionDivider.setTint(color);
                }
            } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
                LogUtils.e("Unable to set selection divider", e);
                sTrySelectionDivider = false;
            }
        }
    }

}

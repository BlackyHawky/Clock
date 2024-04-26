package com.best.deskclock.bedtime;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.DrawableRes;

import com.best.deskclock.R;

public class SleepKlaxon {
    @DrawableRes
    static int toggle(Context context, Uri uri) {
            return R.drawable.ic_fab_pause;
    }
}

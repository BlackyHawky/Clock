// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.tiles;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import com.best.deskclock.screensaver.ScreensaverActivity;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ScreensaverTileService extends TileService {

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override
    public void onClick() {
        super.onClick();

        final Intent intent = new Intent(this, ScreensaverActivity.class)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
                .addFlags(FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(intent);
        }
    }
}

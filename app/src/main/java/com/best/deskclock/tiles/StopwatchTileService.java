// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.tiles;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.best.deskclock.uidata.UiDataModel.Tab.STOPWATCH;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.events.Events;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.SdkUtils;

@RequiresApi(api = Build.VERSION_CODES.N)
public class StopwatchTileService extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile(getQsTile());
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override
    public void onClick() {
        super.onClick();

        final Intent intent = new Intent(this, DeskClock.class)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
                .addFlags(FLAG_ACTIVITY_CLEAR_TOP);

        UiDataModel.getUiDataModel().setSelectedTab(STOPWATCH);

        final int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
        if (DataModel.getDataModel().getStopwatch().isRunning()) {
            DataModel.getDataModel().pauseStopwatch();
            Events.sendStopwatchEvent(R.string.action_pause, label);
        } else {
            DataModel.getDataModel().startStopwatch();
            Events.sendStopwatchEvent(R.string.action_start, label);
        }

        if (SdkUtils.isAtLeastAndroid14()) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(intent);
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        updateTile(getQsTile());
    }

    public void onStopListening() {
        super.onStopListening();

        updateTile(getQsTile());
    }

    private void updateTile(Tile tile) {
        if (tile == null) {
            return;
        }

        final Stopwatch stopwatch = DataModel.getDataModel().getStopwatch();

        if (stopwatch.isReset()) {
            tile.setState(Tile.STATE_INACTIVE);
            if (SdkUtils.isAtLeastAndroid10()) {
                tile.setSubtitle(getString(R.string.shortcut_start_stopwatch_short));
            }
        } else {
            tile.setState(Tile.STATE_ACTIVE);
            if (stopwatch.isRunning()) {
                if (SdkUtils.isAtLeastAndroid10()) {
                    tile.setSubtitle(getString(R.string.shortcut_pause_stopwatch_short));
                }
            } else {
                if (SdkUtils.isAtLeastAndroid10()) {
                    tile.setSubtitle(getString(R.string.shortcut_start_stopwatch_short));
                }
            }
        }

        tile.updateTile();
    }
}

// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;

import androidx.annotation.NonNull;

public class AppWidgetCityService {

    public static class DigitalAppWidgetCityService extends RemoteViewsService {

        @Override
        public RemoteViewsFactory onGetViewFactory(@NonNull Intent i) {
            return new DigitalAppWidgetCityViewsFactory(getApplicationContext(), i);
        }
    }
}

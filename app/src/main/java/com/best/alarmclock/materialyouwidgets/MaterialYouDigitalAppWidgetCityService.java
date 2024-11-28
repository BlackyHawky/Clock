/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.alarmclock.materialyouwidgets;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class MaterialYouDigitalAppWidgetCityService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent i) {
        return new MaterialYouDigitalAppWidgetCityViewsFactory(getApplicationContext(), i);
    }
}

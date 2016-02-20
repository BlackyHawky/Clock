/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.alarmclock;

import android.content.Intent;
import android.content.res.Configuration;
import android.widget.RemoteViewsService;

import com.android.deskclock.data.DataModel;

public class DigitalAppWidgetService extends RemoteViewsService {

    public static final boolean LOGGING = false;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent i) {
        return new DigitalWidgetViewsFactory(getApplicationContext(), i);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Orientation possibly changed, so notify the widgets.
        sendBroadcast(new Intent(DataModel.ACTION_DIGITAL_WIDGET_CHANGED));
    }
}
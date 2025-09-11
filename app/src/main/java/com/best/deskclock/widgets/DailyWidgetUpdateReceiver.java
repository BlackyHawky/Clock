// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouDigitalAppWidgetProvider;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouVerticalDigitalAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.DigitalAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.VerticalDigitalAppWidgetProvider;

import java.util.Calendar;

/**
 * This class is responsible for handling the daily update of app widgets at midnight
 * and more mainly the widgets that display the date.
 *
 */
public class DailyWidgetUpdateReceiver extends BroadcastReceiver {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("WidgetUpdateReceiver");

    @Override
    public void onReceive(Context context, Intent intent) {
        LOGGER.i("onReceive:" + intent);

        WidgetUtils.updateWidget(context, DigitalAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, MaterialYouDigitalAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, VerticalDigitalAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, MaterialYouVerticalDigitalAppWidgetProvider.class);

        // Reschedule the alarm for the next day at midnight
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent newIntent = new Intent(context, DailyWidgetUpdateReceiver.class);
        PendingIntent newPendingIntent = PendingIntent.getBroadcast(
                context, 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), newPendingIntent);
    }

}

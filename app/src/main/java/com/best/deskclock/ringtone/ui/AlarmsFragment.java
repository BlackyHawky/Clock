package com.best.deskclock.ringtone.ui;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;

import com.best.deskclock.R;
import com.best.deskclock.ringtone.RingtoneItem;

import java.util.ArrayList;

public class AlarmsFragment extends BasePickerFragment {
    ArrayList<RingtoneItem> getList(Context context) {
        RingtoneManager ringtoneManager = new RingtoneManager(context);
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM);
        Cursor cursor = ringtoneManager.getCursor();
        ArrayList<RingtoneItem> list = new ArrayList<RingtoneItem>();

        while (cursor.moveToNext()) {
            RingtoneItem item = new RingtoneItem();
            item.title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            item.uri = String.valueOf(ringtoneManager.getRingtoneUri(cursor.getPosition()));
            item.iconId = R.drawable.ic_ringtone;
            list.add(item);
        }
        cursor.close();
        return list;
    }
}

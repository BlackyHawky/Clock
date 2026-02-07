/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uicomponents.selector.AlarmSelection;
import com.best.deskclock.uicomponents.selector.AlarmSelectionAdapter;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmSelectionActivity extends BaseActivity implements AlarmSelectionAdapter.OnAlarmClickListener {

    /**
     * Action used to signify alarm should be dismissed on selection.
     */
    public static final int ACTION_DISMISS = 0;
    public static final String EXTRA_ACTION = "com.best.deskclock.EXTRA_ACTION";
    public static final String EXTRA_ALARMS = "com.best.deskclock.EXTRA_ALARMS";
    /**
     * Used by default when an invalid action provided.
     */
    private static final int ACTION_INVALID = -1;
    private final List<AlarmSelection> mSelections = new ArrayList<>();

    private int mAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // this activity is shown if:
        // a) no search mode was specified in which case we show all
        // enabled alarms
        // b) if search mode was next and there was multiple alarms firing next
        // (at the same time) then we only show those alarms firing at the same time
        // c) if search mode was time and there are multiple alarms with that time
        // then we only show those alarms with that time

        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection_layout);

        final Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final Intent intent = getIntent();
        final Parcelable[] alarmsFromIntent = SdkUtils.isAtLeastAndroid13()
                ? intent.getParcelableArrayExtra(EXTRA_ALARMS, Alarm.class)
                : intent.getParcelableArrayExtra(EXTRA_ALARMS);
        mAction = intent.getIntExtra(EXTRA_ACTION, ACTION_INVALID);

        // reading alarms from intent
        // PickSelection is started only if there are more than 1 relevant alarm
        // so no need to check if alarmsFromIntent is empty
        if (alarmsFromIntent != null) {
            for (Parcelable parcelable : alarmsFromIntent) {
                final Alarm alarm = (Alarm) parcelable;

                // filling mSelections that go into the UI picker list
                mSelections.add(new AlarmSelection(alarm));
            }
        }

        AlarmSelectionAdapter adapter = new AlarmSelectionAdapter(this, mSelections, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAlarmClick(Alarm alarm) {
        processAlarmActionAsync(alarm);
        finish();
    }

    void processAlarmActionAsync(Alarm alarm) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            switch (mAction) {
                case ACTION_DISMISS -> HandleApiCalls.dismissAlarm(alarm, this);
                case ACTION_INVALID -> LogUtils.i("Invalid action");
            }
        });
    }

}

/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.alarmselection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.BaseActivity;
import com.best.deskclock.controller.HandleApiCalls;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.databinding.SelectionLayoutBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlarmSelectionActivity extends BaseActivity implements AlarmSelectionAdapter.OnAlarmClickListener {

    public static final String EXTRA_ACTION = "com.best.deskclock.EXTRA_ACTION";
    public static final String EXTRA_ALARMS = "com.best.deskclock.EXTRA_ALARMS";

    /**
     * Action used to signify alarm should be dismissed on selection.
     */
    public static final int ACTION_DISMISS = 0;

    /**
     * Used by default when an invalid action provided.
     */
    private static final int ACTION_INVALID = -1;

    private String mAlarmFontPath;
    private Typeface mAlarmBoldTypeface;
    private final List<AlarmSelection> mSelections = new ArrayList<>();

    private int mAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // This activity is shown if:
        // a) No search mode was specified in which case we show all enabled alarms.
        // b) If search mode was next and there was multiple alarms firing next
        // (at the same time) then we only show those alarms firing at the same time.
        // c) If search mode was time and there are multiple alarms with that time
        // then we only show those alarms with that time.

        super.onCreate(savedInstanceState);

        SelectionLayoutBinding binding = SelectionLayoutBinding.inflate(getLayoutInflater());

        mAlarmFontPath = SettingsDAO.getAlarmFont(getPrefs());

        setContentView(binding.getRoot());

        binding.cancelButton.setOnClickListener(v -> finish());

        binding.alarmRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        final Intent intent = getIntent();
        final Parcelable[] alarmsFromIntent = IntentCompat.getParcelableArrayExtra(intent, EXTRA_ALARMS, Alarm.class);

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

        Locale locale = getLocale();
        String pattern = DateFormat.getBestDateTimePattern(locale, "MMMd");
        String patternWithYear = DateFormat.getBestDateTimePattern(locale, "yyyyMMMMd");
        UiConfig.DateFormat dateFormat = new UiConfig.DateFormat(locale, pattern, patternWithYear);

        Weekdays.Order weekdayOrder = SettingsDAO.getWeekdayOrder(getPrefs());

        AlarmSelectionAdapter adapter = new AlarmSelectionAdapter(
            mSelections, getFontsConfig(), dateFormat, weekdayOrder, getDataModel().is24HourFormat(), this);
        binding.alarmRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onAlarmClick(@NonNull Alarm alarm) {
        processAlarmActionAsync(alarm);
        finish();
    }

    @NonNull
    @Override
    protected UiConfig.Fonts getFontsConfig() {
        return new UiConfig.Fonts(
            getGeneralTypeface(),
            getGeneralBoldTypeface(),
            getAlarmBoldTypeface(),
            null,
            null,
            null
        );
    }

    /**
     * Lazy loading for the bold alarm font (used for AM/PM).
     *
     * @return the bold alarm font.
     */
    protected final Typeface getAlarmBoldTypeface() {
        if (mAlarmBoldTypeface == null) {
            mAlarmBoldTypeface = ThemeUtils.boldTypeface(mAlarmFontPath);
        }

        return mAlarmBoldTypeface;
    }

    void processAlarmActionAsync(@NonNull Alarm alarm) {
        final Context appContext = getApplicationContext();
        final int action = mAction;

        AppExecutors.getDiskIO().execute(() -> {
            switch (action) {
                case ACTION_DISMISS -> HandleApiCalls.dismissAlarm(appContext, getPrefs(), alarm);
                case ACTION_INVALID -> LogUtils.i("Invalid action");
            }
        });
    }

}

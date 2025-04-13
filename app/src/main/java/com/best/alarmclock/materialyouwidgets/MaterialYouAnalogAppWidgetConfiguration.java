// SPDX-License-Identifier: GPL-3.0-only

package com.best.alarmclock.materialyouwidgets;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.R;

import com.google.android.material.card.MaterialCardView;

/**
 * This class launches the Material You analog widget configuration for Android12+ to choose whether to display the analog widget
 * with or without the second hand.
 * <p>
 * Earlier versions cannot display the second hand so this activity is not shown to the user.
 */
public class MaterialYouAnalogAppWidgetConfiguration extends AppCompatActivity {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    private ConstraintLayout mWidgetConfigLayout;
    SharedPreferences mPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        mPrefs = getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
            }
        }

        // As the second hand display is only available for Android12+, just complete the activity
        // for earlier versions and add the analog widget without this second hand.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(this, R.string.analog_widget_configuration_warning, Toast.LENGTH_LONG).show();
            onWidgetContainerClicked(false);
            finish();
        }

        setContentView(R.layout.material_you_analog_widget_configuration);

        mWidgetConfigLayout = findViewById(R.id.widget_config_layout);

        applyWindowInsets();

        MaterialCardView analogClockWithoutSecond = findViewById(R.id.container_without_second_hand);
        MaterialCardView analogClockWithSecond = findViewById(R.id.container_with_second_hand);

        int cardBackgroundColor = getResources().getColor(R.color.md_theme_inversePrimary, null);

        analogClockWithoutSecond.setCardBackgroundColor(cardBackgroundColor);
        analogClockWithSecond.setCardBackgroundColor(cardBackgroundColor);

        analogClockWithoutSecond.setOnClickListener(v -> onWidgetContainerClicked(false));
        analogClockWithSecond.setOnClickListener(v -> onWidgetContainerClicked(true));
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mWidgetConfigLayout, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void onWidgetContainerClicked(boolean isSecondHandDisplayed) {
        mPrefs.edit().putBoolean(KEY_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND, isSecondHandDisplayed).apply();

        AppWidgetManager wm = AppWidgetManager.getInstance(this);

        MaterialYouAnalogAppWidgetProvider.updateAppWidget(this, wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }

}

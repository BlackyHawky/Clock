// SPDX-License-Identifier: GPL-3.0-only

package com.best.alarmclock;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.best.deskclock.R;
import com.best.deskclock.settings.AnalogWidgetSettingsFragment;
import com.best.deskclock.settings.DigitalWidgetSettingsFragment;
import com.best.deskclock.settings.MaterialYouAnalogWidgetSettingsFragment;
import com.best.deskclock.settings.MaterialYouDigitalWidgetSettingsFragment;
import com.best.deskclock.settings.MaterialYouNextAlarmWidgetSettingsFragment;
import com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetSettingsFragment;
import com.best.deskclock.settings.NextAlarmWidgetSettingsFragment;
import com.best.deskclock.settings.VerticalDigitalWidgetSettingsFragment;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

/**
 * Class called when the user launches the widget configuration from the widget.
 */
public class WidgetConfiguration {

    public static class AnalogWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.analog_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new AnalogWidgetSettingsFragment());
        }
    }

    public static class DigitalWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new DigitalWidgetSettingsFragment());
        }
    }

    public static class VerticalDigitalWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new VerticalDigitalWidgetSettingsFragment());
        }
    }

    public static class NextAlarmWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new NextAlarmWidgetSettingsFragment());
        }
    }

    public static class MaterialYouAnalogWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.analog_widget_material_you);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new MaterialYouAnalogWidgetSettingsFragment());
        }
    }

    public static class MaterialYouDigitalWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new MaterialYouDigitalWidgetSettingsFragment());
        }
    }

    public static class MaterialYouVerticalDigitalWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new MaterialYouVerticalDigitalWidgetSettingsFragment());
        }
    }

    public static class MaterialYouNextAlarmWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new MaterialYouNextAlarmWidgetSettingsFragment());
        }
    }

    public static void showFragmentFromWidget(AppCompatActivity activity, Bundle savedInstanceState, Fragment fragment) {
        WidgetUtils.isLaunchedFromWidget = true;

        if (savedInstanceState == null) {
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

}

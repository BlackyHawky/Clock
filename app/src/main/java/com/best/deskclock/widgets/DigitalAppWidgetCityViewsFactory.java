// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;

/**
 * This factory produces entries in the world cities list view displayed at the bottom of the
 * digital widget. Each row is comprised of two world cities located side-by-side.
 */
public class DigitalAppWidgetCityViewsFactory extends BaseDigitalAppWidgetCityViewsFactory {

    public DigitalAppWidgetCityViewsFactory(Context context, Intent intent) {
        super(context, intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.world_clock_remote_list_item;
    }

    @Override
    protected int getCityViewId() {
        return R.id.widgetItem;
    }

    @Override
    protected int getLeftClockWithShadowId() {
        return R.id.leftClock;
    }

    @Override
    protected int getLeftClockForCustomColorId() {
        return R.id.leftClockForCustomColor;
    }

    @Override
    protected int getLeftClockNoShadowId() {
        return R.id.leftClockNoShadow;
    }

    @Override
    protected int getLeftClockNoShadowForCustomColorId() {
        return R.id.leftClockNoShadowForCustomColor;
    }

    @Override
    protected int getLeftCityNameWithShadowId() {
        return R.id.cityNameLeft;
    }

    @Override
    protected int getLeftCityNameForCustomColorId() {
        return R.id.cityNameLeftForCustomColor;
    }

    @Override
    protected int getLeftCityNameNoShadowId() {
        return R.id.cityNameLeftNoShadow;
    }

    @Override
    protected int getLeftCityNameNoShadowForCustomColorId() {
        return R.id.cityNameLeftNoShadowForCustomColor;
    }

    @Override
    protected int getLeftCityDayWithShadowId() {
        return R.id.cityDayLeft;
    }

    @Override
    protected int getLeftCityDayForCustomColorId() {
        return R.id.cityDayLeftForCustomColor;
    }

    @Override
    protected int getLeftCityDayNoShadowId() {
        return R.id.cityDayLeftNoShadow;
    }

    @Override
    protected int getLeftCityDayNoShadowForCustomColorId() {
        return R.id.cityDayLeftNoShadowForCustomColor;
    }

    @Override
    protected int getLeftCityNoteWithShadowId() {
        return R.id.cityNoteLeft;
    }

    @Override
    protected int getLeftCityNoteForCustomColorId() {
        return R.id.cityNoteLeftForCustomColor;
    }

    @Override
    protected int getLeftCityNoteNoShadowId() {
        return R.id.cityNoteLeftNoShadow;
    }

    @Override
    protected int getLeftCityNoteNoShadowForCustomColorId() {
        return R.id.cityNoteLeftNoShadowForCustomColor;
    }

    @Override
    protected int getRightClockWithShadowId() {
        return R.id.rightClock;
    }

    @Override
    protected int getRightClockForCustomColorId() {
        return R.id.rightClockForCustomColor;
    }

    @Override
    protected int getRightClockNoShadowId() {
        return R.id.rightClockNoShadow;
    }

    @Override
    protected int getRightClockNoShadowForCustomColorId() {
        return R.id.rightClockNoShadowForCustomColor;
    }

    @Override
    protected int getRightCityNameWithShadowId() {
        return R.id.cityNameRight;
    }

    @Override
    protected int getRightCityNameForCustomColorId() {
        return R.id.cityNameRightForCustomColor;
    }

    @Override
    protected int getRightCityNameNoShadowId() {
        return R.id.cityNameRightNoShadow;
    }

    @Override
    protected int getRightCityNameNoShadowForCustomColorId() {
        return R.id.cityNameRightNoShadowForCustomColor;
    }

    @Override
    protected int getRightCityDayWithShadowId() {
        return R.id.cityDayRight;
    }

    @Override
    protected int getRightCityDayForCustomColorId() {
        return R.id.cityDayRightForCustomColor;
    }

    @Override
    protected int getRightCityDayNoShadowId() {
        return R.id.cityDayRightNoShadow;
    }

    @Override
    protected int getRightCityDayNoShadowForCustomColorId() {
        return R.id.cityDayRightNoShadowForCustomColor;
    }

    @Override
    protected int getRightCityNoteWithShadowId() {
        return R.id.cityNoteRight;
    }

    @Override
    protected int getRightCityNoteForCustomColorId() {
        return R.id.cityNoteRightForCustomColor;
    }

    @Override
    protected int getRightCityNoteNoShadowId() {
        return R.id.cityNoteRightNoShadow;
    }

    @Override
    protected int getRightCityNoteNoShadowForCustomColorId() {
        return R.id.cityNoteRightNoShadowForCustomColor;
    }

    @Override
    protected int getCitySpacerId() {
        return R.id.citySpacer;
    }

    @Override
    protected boolean isTextUppercaseDisplayed(SharedPreferences prefs) {
        return WidgetDAO.isTextUppercaseDisplayedOnDigitalWidget(prefs);
    }

    @Override
    protected boolean isTextShadowDisplayed(SharedPreferences prefs) {
        return WidgetDAO.isTextShadowDisplayedOnDigitalWidget(prefs);
    }

    @Override
    protected boolean isDefaultCityClockColor(SharedPreferences prefs) {
        return WidgetDAO.isDigitalWidgetDefaultCityClockColor(prefs);
    }

    @Override
    protected int getCityClockColor(SharedPreferences prefs) {
        return WidgetDAO.getDigitalWidgetCustomCityClockColor(prefs);
    }

    @Override
    protected boolean isDefaultCityNameColor(SharedPreferences prefs) {
        return WidgetDAO.isDigitalWidgetDefaultCityNameColor(prefs);
    }

    @Override
    protected int getCityNameColor(SharedPreferences prefs) {
        return WidgetDAO.getDigitalWidgetCustomCityNameColor(prefs);
    }

    @Override
    protected boolean isDefaultCityNoteColor(SharedPreferences prefs) {
        return WidgetDAO.isDigitalWidgetDefaultCityNoteColor(prefs);
    }

    @Override
    protected int getCityNoteColor(SharedPreferences prefs) {
        return WidgetDAO.getDigitalWidgetCustomCityNoteColor(prefs);
    }

}

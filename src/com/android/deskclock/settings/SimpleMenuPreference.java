/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.deskclock.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.DropDownPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

/**
 * Bend {@link DropDownPreference} to support
 * <a href="https://material.google.com/components/menus.html#menus-behavior">Simple Menus</a>.
 */
public class SimpleMenuPreference extends DropDownPreference {

    private SimpleMenuAdapter mAdapter;

    public SimpleMenuPreference(Context context) {
        this(context, null);
    }

    public SimpleMenuPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dropdownPreferenceStyle);
    }

    public SimpleMenuPreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public SimpleMenuPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected ArrayAdapter createAdapter() {
        mAdapter = new SimpleMenuAdapter(getContext(), R.layout.simple_menu_dropdown_item);
        return mAdapter;
    }

    private static void restoreOriginalOrder(CharSequence[] array,
            int lastSelectedOriginalPosition) {
        final CharSequence item = array[0];
        System.arraycopy(array, 1, array, 0, lastSelectedOriginalPosition);
        array[lastSelectedOriginalPosition] = item;
    }

    private static void swapSelectedToFront(CharSequence[] array, int position) {
        final CharSequence item = array[position];
        System.arraycopy(array, 0, array, 1, position);
        array[0] = item;
    }

    private static void setSelectedPosition(CharSequence[] array, int lastSelectedOriginalPosition,
            int position) {
        final CharSequence item = array[position];
        restoreOriginalOrder(array, lastSelectedOriginalPosition);
        final int originalPosition = Utils.indexOf(array, item);
        swapSelectedToFront(array, originalPosition);
    }

    @Override
    public void setSummary(CharSequence summary) {
        final CharSequence[] entries = getEntries();
        final int index = Utils.indexOf(entries, summary);
        if (index == -1) {
            throw new IllegalArgumentException("Illegal Summary");
        }
        final int lastSelectedOriginalPosition = mAdapter.getLastSelectedOriginalPosition();
        mAdapter.setSelectedPosition(index);
        setSelectedPosition(entries, lastSelectedOriginalPosition, index);
        setSelectedPosition(getEntryValues(), lastSelectedOriginalPosition, index);
        super.setSummary(summary);
    }

    private final static class SimpleMenuAdapter extends ArrayAdapter<CharSequence> {

        /** The original position of the last selected element */
        private int mLastSelectedOriginalPosition = 0;

        SimpleMenuAdapter(Context context, int resource) {
            super(context, resource);
        }

        private void restoreOriginalOrder() {
            final CharSequence item = getItem(0);
            remove(item);
            insert(item, mLastSelectedOriginalPosition);
        }

        private void swapSelectedToFront(int position) {
            final CharSequence item = getItem(position);
            remove(item);
            insert(item, 0);
            mLastSelectedOriginalPosition = position;
        }

        int getLastSelectedOriginalPosition() {
            return mLastSelectedOriginalPosition;
        }

        void setSelectedPosition(int position) {
            setNotifyOnChange(false);
            final CharSequence item = getItem(position);
            restoreOriginalOrder();
            final int originalPosition = getPosition(item);
            swapSelectedToFront(originalPosition);
            notifyDataSetChanged();
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            final View view = super.getDropDownView(position, convertView, parent);
            if (position == 0) {
                view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white_08p));
            } else {
                view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
            }
            return view;
        }
    }
}

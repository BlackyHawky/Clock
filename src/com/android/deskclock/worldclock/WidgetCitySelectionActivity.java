/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.deskclock.worldclock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.alarmclock.CityAppWidgetProvider;
import com.android.deskclock.BaseActivity;
import com.android.deskclock.DropShadowController;
import com.android.deskclock.R;
import com.android.deskclock.actionbarmenu.ActionBarMenuManager;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.actionbarmenu.SearchMenuItemController;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

/**
 * This activity allows the user to select a single city for display in a widget.
 */
public final class WidgetCitySelectionActivity extends BaseActivity {

    /** Manages all action bar menu display and click handling. */
    private final ActionBarMenuManager mActionBarMenuManager = new ActionBarMenuManager();

    /** The list of all cities, indexed and possibly filtered. */
    private ListView mCitiesList;

    /** The adapter that presents all of the cities. */
    private CityAdapter mCitiesAdapter;

    /** Menu item controller for search view. */
    private SearchMenuItemController mSearchMenuItemController;

    /** The controller that shows the drop shadow when content is not scrolled to the top. */
    private DropShadowController mDropShadowController;

    /** Identifies the widget in which the selected city will be displayed. */
    private int mWidgetId = INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
        }

        setContentView(R.layout.cities_activity);
        mSearchMenuItemController =
                new SearchMenuItemController(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        mCitiesAdapter.filter(query);
                        updateFastScrolling();
                        return true;
                    }
                }, savedInstanceState);
        mCitiesAdapter = new CityAdapter(this);
        mActionBarMenuManager.addMenuItemController(new NavUpMenuItemController(this))
                .addMenuItemController(mSearchMenuItemController);
        mCitiesList = (ListView) findViewById(R.id.cities_list);
        mCitiesList.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        mCitiesList.setAdapter(mCitiesAdapter);

        updateFastScrolling();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        mSearchMenuItemController.saveInstance(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Recompute the contents of the adapter before displaying on screen.
        mCitiesAdapter.refresh();

        final View dropShadow = findViewById(R.id.drop_shadow);
        mDropShadowController = new DropShadowController(dropShadow, mCitiesList);
    }

    @Override
    public void onPause() {
        super.onPause();

        mDropShadowController.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mActionBarMenuManager.createOptionsMenu(menu, getMenuInflater());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mActionBarMenuManager.prepareShowMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mActionBarMenuManager.handleMenuItemClick(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fast scrolling is only enabled while no filtering is happening.
     */
    private void updateFastScrolling() {
        final boolean enabled = !mCitiesAdapter.isFiltering();
        mCitiesList.setFastScrollAlwaysVisible(enabled);
        mCitiesList.setFastScrollEnabled(enabled);
    }

    /**
     * This adapter presents data like so:
     *
     * <pre>
     * A City A1 (alphabetically first starting with A)
     *   City A2 (alphabetically second starting with A)
     *   ...
     * B City B1 (alphabetically first starting with B)
     *   City B2 (alphabetically second starting with B)
     *   ...
     * </pre>
     */
    private final class CityAdapter extends BaseAdapter implements View.OnClickListener,
            SectionIndexer {

        private final Context mContext;

        private final LayoutInflater mInflater;

        /** Orders the cities by name for presentation in the UI. */
        private final Comparator<City> mNameIndexComparator = new City.NameIndexComparator();

        /** The 12-hour time pattern for the current locale. */
        private final String mPattern12;

        /** The 24-hour time pattern for the current locale. */
        private final String mPattern24;

        /** {@code true} time should honor {@link #mPattern24}; {@link #mPattern12} otherwise. */
        private boolean mIs24HoursMode;

        /** A calendar used to format time in a particular timezone. */
        private final Calendar mCalendar;

        /** The list of cities which may be filtered by a search term. */
        private List<City> mFilteredCities = Collections.emptyList();

        /** The precomputed section headers. */
        private String[] mSectionHeaders;

        /** The corresponding location of each precomputed section header. */
        private Integer[] mSectionHeaderPositions;

        public CityAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);

            mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            final Locale locale = Locale.getDefault();
            mPattern24 = DateFormat.getBestDateTimePattern(locale, "Hm");

            String pattern12 = DateFormat.getBestDateTimePattern(locale, "hma");
            if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
                // There's an RTL layout bug that causes jank when fast-scrolling through
                // the list in 12-hour mode in an RTL locale. We can work around this by
                // ensuring the strings are the same length by using "hh" instead of "h".
                pattern12 = pattern12.replaceAll("h", "hh");
            }
            mPattern12 = pattern12;
        }

        @Override
        public int getCount() {
            return mFilteredCities.size();
        }

        @Override
        public City getItem(int position) {
            return mFilteredCities.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final City city = getItem(position);
            final TimeZone timeZone = city.getTimeZone();

            // Inflate a new view if necessary.
            if (view == null) {
                view = mInflater.inflate(R.layout.widget_city_list_item, parent, false);
                final TextView index = (TextView) view.findViewById(R.id.index);
                final TextView name = (TextView) view.findViewById(R.id.city_name);
                final TextView time = (TextView) view.findViewById(R.id.city_time);
                view.setTag(new CityItemHolder(index, name, time));
            }

            // Bind data into the child views.
            final CityItemHolder holder = (CityItemHolder) view.getTag();
            holder.name.setText(city.getName(), TextView.BufferType.SPANNABLE);
            holder.time.setText(getTimeCharSequence(timeZone));

            final boolean showIndex = getShowIndex(position);
            holder.index.setVisibility(showIndex ? View.VISIBLE : View.INVISIBLE);
            if (showIndex) {
                holder.index.setText(city.getIndexString());
                holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            }

            // skip checkbox and other animations
            view.jumpDrawablesToCurrentState();
            view.setOnClickListener(this);
            return view;
        }

        @Override
        public void onClick(View v) {
            final int position = mCitiesList.getPositionForView(v);
            if (position >= 0) {
                final City selectedCity = getItem(position);

                // Associate the widget id to the selected city.
                DataModel.getDataModel().setWidgetCity(mWidgetId, selectedCity);

                // Broadcast an intent to update the instance of the widget now that data exists.
                final Intent intent = new Intent(ACTION_APPWIDGET_UPDATE, null, mContext,
                        CityAppWidgetProvider.class);
                intent.putExtra(EXTRA_APPWIDGET_IDS, new int[] {mWidgetId});
                sendBroadcast(intent);

                // Indicate successful configuration of the app widget.
                final Intent result = new Intent().putExtra(EXTRA_APPWIDGET_ID, mWidgetId);
                setResult(RESULT_OK, result);
                finish();
            }
        }

        @Override
        public Object[] getSections() {
            if (mSectionHeaders == null) {
                // Make an educated guess at the expected number of sections.
                final int approximateSectionCount = getCount() / 5;
                final List<String> sections = new ArrayList<>(approximateSectionCount);
                final List<Integer> positions = new ArrayList<>(approximateSectionCount);

                for (int position = 0; position < getCount(); position++) {
                    // Add a section if this position should show the section index.
                    if (getShowIndex(position)) {
                        final City city = getItem(position);
                        sections.add(city.getIndexString());
                        positions.add(position);
                    }
                }

                mSectionHeaders = sections.toArray(new String[sections.size()]);
                mSectionHeaderPositions = positions.toArray(new Integer[positions.size()]);
            }
            return mSectionHeaders;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return getSections().length == 0 ? 0 : mSectionHeaderPositions[sectionIndex];
        }

        @Override
        public int getSectionForPosition(int position) {
            if (getSections().length == 0) {
                return 0;
            }

            for (int i = 0; i < mSectionHeaderPositions.length - 2; i++) {
                if (position < mSectionHeaderPositions[i]) continue;
                if (position >= mSectionHeaderPositions[i + 1]) continue;

                return i;
            }

            return mSectionHeaderPositions.length - 1;
        }

        /**
         * Clear the section headers to force them to be recomputed if they are now stale.
         */
        private void clearSectionHeaders() {
            mSectionHeaders = null;
            mSectionHeaderPositions = null;
        }

        /**
         * Rebuilds all internal data structures from scratch.
         */
        private void refresh() {
            // Update the 12/24 hour mode.
            mIs24HoursMode = DateFormat.is24HourFormat(mContext);

            // Recompute section headers.
            clearSectionHeaders();

            // Recompute filtered cities.
            filter(mSearchMenuItemController.getQueryText());
        }

        /**
         * Filter the cities using the given {@code queryText}.
         */
        private void filter(String queryText) {
            mSearchMenuItemController.setQueryText(queryText);
            final String query = City.removeSpecialCharacters(queryText.toUpperCase());
            final List<City> cities = DataModel.getDataModel().getAllCities();

            // Compute the filtered list of cities.
            final List<City> filteredCities;
            if (TextUtils.isEmpty(query)) {
                filteredCities = cities;
            } else {
                filteredCities = new ArrayList<>(cities.size());
                for (City city : cities) {
                    if (city.matches(query)) {
                        filteredCities.add(city);
                    }
                }
            }

            // Swap in the filtered list of cities and notify of the data change.
            mFilteredCities = filteredCities;
            notifyDataSetChanged();
        }

        private boolean isFiltering() {
            return !TextUtils.isEmpty(mSearchMenuItemController.getQueryText().trim());
        }

        private CharSequence getTimeCharSequence(TimeZone timeZone) {
            mCalendar.setTimeZone(timeZone);
            return DateFormat.format(mIs24HoursMode ? mPattern24 : mPattern12, mCalendar);
        }

        private boolean getShowIndex(int position) {
            // Indexes are never displayed on filtered cities.
            if (isFiltering()) {
                return false;
            }

            // The first entry is always a header.
            if (position == 0) {
                return true;
            }

            // Otherwise compare the city with its predecessor to test if it is a header.
            final City priorCity = getItem(position - 1);
            final City city = getItem(position);
            return mNameIndexComparator.compare(priorCity, city) != 0;
        }
    }

    /**
     * Cache the child views of each city item view.
     */
    private static final class CityItemHolder {

        private final TextView index;
        private final TextView name;
        private final TextView time;

        public CityItemHolder(TextView index, TextView name, TextView time) {
            this.index = index;
            this.name = name;
            this.time = time;
        }
    }
}
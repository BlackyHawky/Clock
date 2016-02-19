/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.deskclock.BaseActivity;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.actionbarmenu.AbstractMenuItemController;
import com.android.deskclock.actionbarmenu.ActionBarMenuManager;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.actionbarmenu.SearchMenuItemController;
import com.android.deskclock.actionbarmenu.SettingMenuItemController;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * This activity allows the user to alter the cities selected for display.
 *
 * Note, it is possible for two instances of this Activity to exist simultaneously:
 *
 * <ul>
 *     <li>Clock Tab-> Tap Floating Action Button</li>
 *     <li>Digital Widget -> Tap any city clock</li>
 * </ul>
 *
 * As a result, {@link #onResume()} conservatively refreshes itself from the backing
 * {@link DataModel} which may have changed since this activity was last displayed.
 */
public final class CitySelectionActivity extends BaseActivity {

    /** The list of all selected and unselected cities, indexed and possibly filtered. */
    private ListView mCitiesList;

    /** The adapter that presents all of the selected and unselected cities. */
    private CityAdapter mCitiesAdapter;

    /** Manages all action bar menu display and click handling. */
    private final ActionBarMenuManager mActionBarMenuManager = new ActionBarMenuManager(this);

    /** Menu item controller for search view. */
    private SearchMenuItemController mSearchMenuItemController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

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
        mCitiesAdapter = new CityAdapter(this, mSearchMenuItemController);
        mActionBarMenuManager.addMenuItemController(new NavUpMenuItemController(this))
                .addMenuItemController(mSearchMenuItemController)
                .addMenuItemController(new SortOrderMenuItemController())
                .addMenuItemController(new SettingMenuItemController(this))
                .addMenuItemController(MenuItemControllerFactory.getInstance()
                        .buildMenuItemControllers(this));
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
    }

    @Override
    public void onPause() {
        super.onPause();

        // Save the selected cities.
        DataModel.getDataModel().setSelectedCities(mCitiesAdapter.getSelectedCities());
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
     * This adapter presents data in 2 possible modes. If selected cities exist the format is:
     *
     * <pre>
     * Selected Cities
     *   City 1 (alphabetically first)
     *   City 2 (alphabetically second)
     *   ...
     * A City A1 (alphabetically first starting with A)
     *   City A2 (alphabetically second starting with A)
     *   ...
     * B City B1 (alphabetically first starting with B)
     *   City B2 (alphabetically second starting with B)
     *   ...
     * </pre>
     *
     * If selected cities do not exist, that section is removed and all that remains is:
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
    private static final class CityAdapter extends BaseAdapter implements View.OnClickListener,
            CompoundButton.OnCheckedChangeListener, SectionIndexer {

        /** The type of the single optional "Selected Cities" header entry. */
        private static final int VIEW_TYPE_SELECTED_CITIES_HEADER = 0;

        /** The type of each city entry. */
        private static final int VIEW_TYPE_CITY = 1;

        private final Context mContext;

        private final LayoutInflater mInflater;

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

        /** A mutable set of cities currently selected by the user. */
        private final Set<City> mUserSelectedCities = new ArraySet<>();

        /** The number of user selections at the top of the adapter to avoid indexing. */
        private int mOriginalUserSelectionCount;

        /** The precomputed section headers. */
        private String[] mSectionHeaders;

        /** The corresponding location of each precomputed section header. */
        private Integer[] mSectionHeaderPositions;

        /** Menu item controller for search. Search query is maintained here. */
        private final SearchMenuItemController mSearchMenuItemController;

        public CityAdapter(Context context, SearchMenuItemController searchMenuItemController) {
            mContext = context;
            mSearchMenuItemController = searchMenuItemController;
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
            final int headerCount = hasHeader() ? 1 : 0;
            return headerCount + mFilteredCities.size();
        }

        @Override
        public City getItem(int position) {
            if (hasHeader()) {
                final int itemViewType = getItemViewType(position);
                switch (itemViewType) {
                    case VIEW_TYPE_SELECTED_CITIES_HEADER:
                        return null;
                    case VIEW_TYPE_CITY:
                        return mFilteredCities.get(position - 1);
                }
                throw new IllegalStateException("unexpected item view type: " + itemViewType);
            }

            return mFilteredCities.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public synchronized View getView(int position, View view, ViewGroup parent) {
            final int itemViewType = getItemViewType(position);
            switch (itemViewType) {
                case VIEW_TYPE_SELECTED_CITIES_HEADER:
                    if (view == null) {
                        view = mInflater.inflate(R.layout.city_list_header, parent, false);
                    }
                    return view;

                case VIEW_TYPE_CITY:
                    final City city = getItem(position);
                    final TimeZone timeZone = city.getTimeZone();

                    // Inflate a new view if necessary.
                    if (view == null) {
                        view = mInflater.inflate(R.layout.city_list_item, parent, false);
                        final TextView index = (TextView) view.findViewById(R.id.index);
                        final TextView name = (TextView) view.findViewById(R.id.city_name);
                        final TextView time = (TextView) view.findViewById(R.id.city_time);
                        final CheckBox selected = (CheckBox) view.findViewById(R.id.city_onoff);
                        view.setTag(new CityItemHolder(index, name, time, selected));
                    }

                    // Bind data into the child views.
                    final CityItemHolder holder = (CityItemHolder) view.getTag();
                    holder.selected.setTag(city);
                    holder.selected.setChecked(mUserSelectedCities.contains(city));
                    holder.selected.setContentDescription(city.getName());
                    holder.selected.setOnCheckedChangeListener(this);
                    holder.name.setText(city.getName(), TextView.BufferType.SPANNABLE);
                    holder.time.setText(getTimeCharSequence(timeZone));

                    final boolean showIndex = getShowIndex(position);
                    holder.index.setVisibility(showIndex ? View.VISIBLE : View.INVISIBLE);
                    if (showIndex) {
                        switch (getCitySort()) {
                            case NAME:
                                holder.index.setText(city.getIndexString());
                                holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                                break;

                            case UTC_OFFSET:
                                holder.index.setText(Utils.getGMTHourOffset(timeZone, false));
                                holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                break;
                        }
                    }

                    // skip checkbox and other animations
                    view.jumpDrawablesToCurrentState();
                    view.setOnClickListener(this);
                    return view;
            }

            throw new IllegalStateException("unexpected item view type: " + itemViewType);
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return hasHeader() && position == 0 ? VIEW_TYPE_SELECTED_CITIES_HEADER : VIEW_TYPE_CITY;
        }

        @Override
        public void onCheckedChanged(CompoundButton b, boolean checked) {
            final City city = (City) b.getTag();
            if (checked) {
                mUserSelectedCities.add(city);
                b.announceForAccessibility(mContext.getString(R.string.city_checked,
                        city.getName()));
            } else {
                mUserSelectedCities.remove(city);
                b.announceForAccessibility(mContext.getString(R.string.city_unchecked,
                        city.getName()));
            }
        }

        @Override
        public void onClick(View v) {
            final CheckBox b = (CheckBox) v.findViewById(R.id.city_onoff);
            b.setChecked(!b.isChecked());
        }

        @Override
        public Object[] getSections() {
            if (mSectionHeaders == null) {
                // Make an educated guess at the expected number of sections.
                final int approximateSectionCount = getCount() / 5;
                final List<String> sections = new ArrayList<>(approximateSectionCount);
                final List<Integer> positions = new ArrayList<>(approximateSectionCount);

                // Add a section for the "Selected Cities" header if it exists.
                if (hasHeader()) {
                    sections.add("+");
                    positions.add(0);
                }

                for (int position = 0; position < getCount(); position++) {
                    // Add a section if this position should show the section index.
                    if (getShowIndex(position)) {
                        final City city = getItem(position);
                        switch (getCitySort()) {
                            case NAME:
                                sections.add(city.getIndexString());
                                break;
                            case UTC_OFFSET:
                                final TimeZone timezone = city.getTimeZone();
                                sections.add(Utils.getGMTHourOffset(timezone, Utils.isPreL()));
                                break;
                        }
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

            // Refresh the user selections.
            final List<City> selected = DataModel.getDataModel().getSelectedCities();
            mUserSelectedCities.clear();
            mUserSelectedCities.addAll(selected);
            mOriginalUserSelectionCount = selected.size();

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
            final String query = queryText.trim().toUpperCase();

            // Compute the filtered list of cities.
            final List<City> filteredCities;
            if (TextUtils.isEmpty(query)) {
                filteredCities = DataModel.getDataModel().getAllCities();
            } else {
                final List<City> unselected = DataModel.getDataModel().getUnselectedCities();
                filteredCities = new ArrayList<>(unselected.size());
                for (City city : unselected) {
                    if (city.getNameUpperCase().startsWith(query)) {
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

        private Collection<City> getSelectedCities() { return mUserSelectedCities; }
        private boolean hasHeader() { return !isFiltering() && mOriginalUserSelectionCount > 0; }

        private DataModel.CitySort getCitySort() {
            return DataModel.getDataModel().getCitySort();
        }

        private Comparator<City> getCitySortComparator() {
            return DataModel.getDataModel().getCityIndexComparator();
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

            if (hasHeader()) {
                // None of the original user selections should show their index.
                if (position <= mOriginalUserSelectionCount) {
                    return false;
                }

                // The first item after the original user selections must always show its index.
                if (position == mOriginalUserSelectionCount + 1) {
                    return true;
                }
            } else {
                // None of the original user selections should show their index.
                if (position < mOriginalUserSelectionCount) {
                    return false;
                }

                // The first item after the original user selections must always show its index.
                if (position == mOriginalUserSelectionCount) {
                    return true;
                }
            }

            // Otherwise compare the city with its predecessor to test if it is a header.
            final City priorCity = getItem(position - 1);
            final City city = getItem(position);
            return getCitySortComparator().compare(priorCity, city) != 0;
        }

        /**
         * Cache the child views of each city item view.
         */
        private static final class CityItemHolder {

            private final TextView index;
            private final TextView name;
            private final TextView time;
            private final CheckBox selected;

            public CityItemHolder(TextView index, TextView name, TextView time, CheckBox selected) {
                this.index = index;
                this.name = name;
                this.time = time;
                this.selected = selected;
            }
        }
    }

    private final class SortOrderMenuItemController extends AbstractMenuItemController {

        private static final int SORT_MENU_RES_ID = R.id.menu_item_sort;

        @Override
        public int getId() {
            return SORT_MENU_RES_ID;
        }

        @Override
        public void showMenuItem(Menu menu) {
            final MenuItem sortMenuItem = menu.findItem(SORT_MENU_RES_ID);
            final String title;
            if (DataModel.getDataModel().getCitySort() == DataModel.CitySort.NAME) {
                title = getString(R.string.menu_item_sort_by_gmt_offset);
            } else {
                title = getString(R.string.menu_item_sort_by_name);
            }
            sortMenuItem.setTitle(title);
            sortMenuItem.setVisible(true);
        }

        @Override
        public boolean handleMenuItemClick(MenuItem item) {
            // Save the new sort order.
            DataModel.getDataModel().toggleCitySort();

            // Section headers are influenced by sort order and must be cleared.
            mCitiesAdapter.clearSectionHeaders();

            // Honor the new sort order in the adapter.
            mCitiesAdapter.filter(mSearchMenuItemController.getQueryText());
            return true;
        }
    }
}

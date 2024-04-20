/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.worldclock;

import static android.view.Menu.NONE;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

/**
 * This activity allows the user to alter the cities selected for display.
 * <p/>
 * Note, it is possible for two instances of this Activity to exist simultaneously:
 * <p/>
 * <ul>
 * <li>Clock Tab-> Tap Floating Action Button</li>
 * <li>Digital Widget -> Tap any city clock</li>
 * </ul>
 * <p/>
 * As a result, {@link #onResume()} conservatively refreshes itself from the backing
 * {@link DataModel} which may have changed since this activity was last displayed.
 */
public final class CitySelectionActivity extends CollapsingToolbarBaseActivity {

    /**
     * The list of all selected and unselected cities, indexed and possibly filtered.
     */
    private ListView mCitiesList;
    /**
     * The adapter that presents all of the selected and unselected cities.
     */
    private CityAdapter mCitiesAdapter;
    /**
     * Menu item controller for search view.
     */
    private SearchMenuItemController mSearchMenuItemController;

    /**
     * The controller that shows the drop shadow when content is not scrolled to the top.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cities_activity);
        mSearchMenuItemController =
                new SearchMenuItemController(Objects.requireNonNull(getSupportActionBar()).getThemedContext(),
                        new SearchView.OnQueryTextListener() {
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
                        },
                        savedInstanceState);

        mCitiesAdapter = new CityAdapter(this, mSearchMenuItemController);
        addMenuProvider(mSearchMenuItemController);
        mCitiesList = findViewById(R.id.cities_list);
        mCitiesList.setAdapter(mCitiesAdapter);

        updateFastScrolling();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
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
        if (menu.size() == 1) {
            menu.getItem(0).setTitle(getMenuTitle()).setIcon(R.drawable.ic_sort)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            menu.add(Menu.FIRST, 1, Menu.FIRST, getMenuTitle()).setIcon(R.drawable.ic_sort)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            // Save the new sort order.
            DataModel.getDataModel().toggleCitySort();
            item.setTitle(getMenuTitle());

            // Section headers are influenced by sort order and must be cleared.
            mCitiesAdapter.clearSectionHeaders();

            // Honor the new sort order in the adapter.
            mCitiesAdapter.filter(mSearchMenuItemController.getQueryText());
        }

        return super.onOptionsItemSelected(item);
    }

    private int getMenuTitle() {
        if (DataModel.getDataModel().getCitySort() == DataModel.CitySort.NAME) {
            return R.string.menu_item_sort_by_gmt_offset;
        } else {
            return R.string.menu_item_sort_by_name;
        }
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
     * <p/>
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
     * <p/>
     * If selected cities do not exist, that section is removed and all that remains is:
     * <p/>
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

        /**
         * The type of the single optional "Selected Cities" header entry.
         */
        private static final int VIEW_TYPE_SELECTED_CITIES_HEADER = 0;

        /**
         * The type of each city entry.
         */
        private static final int VIEW_TYPE_CITY = 1;

        private final Context mContext;

        private final LayoutInflater mInflater;

        /**
         * The 12-hour time pattern for the current locale.
         */
        private final String mPattern12;

        /**
         * The 24-hour time pattern for the current locale.
         */
        private final String mPattern24;
        /**
         * A calendar used to format time in a particular timezone.
         */
        private final Calendar mCalendar;
        /**
         * A mutable set of cities currently selected by the user.
         */
        private final Set<City> mUserSelectedCities = new ArraySet<>();
        /**
         * Menu item controller for search. Search query is maintained here.
         */
        private final SearchMenuItemController mSearchMenuItemController;
        /**
         * {@code true} time should honor {@link #mPattern24}; {@link #mPattern12} otherwise.
         */
        private boolean mIs24HoursMode;
        /**
         * The list of cities which may be filtered by a search term.
         */
        private List<City> mFilteredCities = Collections.emptyList();
        /**
         * The number of user selections at the top of the adapter to avoid indexing.
         */
        private int mOriginalUserSelectionCount;
        /**
         * The precomputed section headers.
         */
        private String[] mSectionHeaders;
        /**
         * The corresponding location of each precomputed section header.
         */
        private Integer[] mSectionHeaderPositions;

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
        public View getView(int position, View view, ViewGroup parent) {
            final int itemViewType = getItemViewType(position);
            switch (itemViewType) {
                case VIEW_TYPE_SELECTED_CITIES_HEADER -> {
                    if (view == null) {
                        view = mInflater.inflate(R.layout.city_list_header, parent, false);
                    }
                    return view;
                }
                case VIEW_TYPE_CITY -> {
                    final City city = getItem(position);
                    if (city == null) {
                        throw new IllegalStateException("The desired city does not exist");
                    }
                    final TimeZone timeZone = city.getTimeZone();

                    // Inflate a new view if necessary.
                    if (view == null) {
                        view = mInflater.inflate(R.layout.city_list_item, parent, false);
                        final TextView index = view.findViewById(R.id.index);
                        final TextView name = view.findViewById(R.id.city_name);
                        final TextView time = view.findViewById(R.id.city_time);
                        final CheckBox selected = view.findViewById(R.id.city_onoff);
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
                            case NAME -> {
                                holder.index.setText(city.getIndexString());
                                holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                            }
                            case UTC_OFFSET -> {
                                holder.index.setText(Utils.getGMTHourOffset(timeZone, false));
                                holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            }
                        }
                    }

                    // skip checkbox and other animations
                    view.jumpDrawablesToCurrentState();
                    view.setOnClickListener(this);
                    return view;
                }
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
                b.announceForAccessibility(mContext.getString(R.string.city_checked, city.getName()));
            } else {
                mUserSelectedCities.remove(city);
                b.announceForAccessibility(mContext.getString(R.string.city_unchecked, city.getName()));
            }
        }

        @Override
        public void onClick(View v) {
            final CheckBox b = v.findViewById(R.id.city_onoff);
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
                        if (city == null) {
                            throw new IllegalStateException("The desired city does not exist");
                        }
                        switch (getCitySort()) {
                            case NAME -> sections.add(city.getIndexString());
                            case UTC_OFFSET -> {
                                final TimeZone timezone = city.getTimeZone();
                                sections.add(Utils.getGMTHourOffset(timezone, false));
                            }
                        }
                        positions.add(position);
                    }
                }

                mSectionHeaders = sections.toArray(new String[0]);
                mSectionHeaderPositions = positions.toArray(new Integer[0]);
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
            final String query = City.removeSpecialCharacters(queryText.toUpperCase());

            // Compute the filtered list of cities.
            final List<City> filteredCities;
            if (TextUtils.isEmpty(query)) {
                filteredCities = DataModel.getDataModel().getAllCities();
            } else {
                final List<City> unselected = DataModel.getDataModel().getUnselectedCities();
                filteredCities = new ArrayList<>(unselected.size());
                for (City city : unselected) {
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

        private Collection<City> getSelectedCities() {
            return mUserSelectedCities;
        }

        private boolean hasHeader() {
            return !isFiltering() && mOriginalUserSelectionCount > 0;
        }

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
        private record CityItemHolder(TextView index, TextView name, TextView time, CheckBox selected) {
        }

    }

    /**
     * Search menu class
     */
    public static final class SearchMenuItemController implements MenuProvider {

        private static final String KEY_SEARCH_QUERY = "search_query";
        private static final String KEY_SEARCH_MODE = "search_mode";

        private final Context mContext;
        private final SearchView.OnQueryTextListener mQueryListener;
        private final SearchModeChangeListener mSearchModeChangeListener;

        private String mQuery = "";
        private boolean mSearchMode;

        public SearchMenuItemController(Context context, SearchView.OnQueryTextListener queryListener,
                                        Bundle savedState) {
            mContext = context;
            mSearchModeChangeListener = new SearchModeChangeListener();
            mQueryListener = queryListener;

            if (savedState != null) {
                mSearchMode = savedState.getBoolean(KEY_SEARCH_MODE, false);
                mQuery = savedState.getString(KEY_SEARCH_QUERY, "");
            }
        }

        public void saveInstance(Bundle outState) {
            outState.putString(KEY_SEARCH_QUERY, mQuery);
            outState.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        }

        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            final SearchView searchView = new SearchView(mContext);
            searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            searchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            searchView.setQuery(mQuery, false);
            searchView.setOnCloseListener(mSearchModeChangeListener);
            searchView.setOnSearchClickListener(mSearchModeChangeListener);
            searchView.setOnQueryTextListener(mQueryListener);

            menu.add(NONE, 0, NONE, android.R.string.search_go)
                    .setActionView(searchView)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

            if (mSearchMode) {
                searchView.requestFocus();
                searchView.setIconified(false);
            }
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            // The search view is handled by {@link #mSearchListener}. Skip handling here.
            return false;
        }

        public String getQueryText() {
            return mQuery;
        }

        public void setQueryText(String query) {
            mQuery = query;
        }

        /**
         * Listener for user actions on search view.
         */
        private final class SearchModeChangeListener implements View.OnClickListener,
                SearchView.OnCloseListener {
            @Override
            public void onClick(View v) {
                mSearchMode = true;
            }

            @Override
            public boolean onClose() {
                mSearchMode = false;
                return false;
            }
        }
    }

}

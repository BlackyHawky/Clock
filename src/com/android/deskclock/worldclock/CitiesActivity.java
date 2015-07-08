/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.deskclock.worldclock;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.deskclock.BaseActivity;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Cities chooser for the world clock
 */
public class CitiesActivity extends BaseActivity implements OnCheckedChangeListener,
        View.OnClickListener, SearchView.OnQueryTextListener {

    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_SEARCH_MODE = "search_mode";
    private static final String KEY_LIST_POSITION = "list_position";

    private static final String PREF_SORT = "sort_preference";

    private static final int SORT_BY_NAME = 0;
    private static final int SORT_BY_GMT_OFFSET = 1;

    /**
     * This must be false for production. If true, turns on logging, test code,
     * etc.
     */
    static final boolean DEBUG = false;
    static final String TAG = "CitiesActivity";

    private LayoutInflater mFactory;
    private ListView mCitiesList;
    private CityAdapter mAdapter;
    private HashMap<String, CityObj> mUserSelectedCities;
    private Calendar mCalendar;

    private SearchView mSearchView;
    private StringBuffer mQueryTextBuffer = new StringBuffer();
    private boolean mSearchMode;
    private int mPosition = -1;

    private SharedPreferences mPrefs;
    private int mSortType;

    private String mSelectedCitiesHeaderString;

    /***
     * Adapter for a list of cities with the respected time zone. The Adapter
     * sorts the list alphabetically and create an indexer.
     ***/
    private class CityAdapter extends BaseAdapter implements Filterable, SectionIndexer {
        private static final int VIEW_TYPE_CITY = 0;
        private static final int VIEW_TYPE_HEADER = 1;

        private static final String DELETED_ENTRY = "C0";

        private List<CityObj> mDisplayedCitiesList;

        private CityObj[] mCities;
        private CityObj[] mSelectedCities;

        private final int mLayoutDirection;

        // A map that caches names of cities in local memory.  The names in this map are
        // preferred over the names of the selected cities stored in SharedPreferences, which could
        // be in a different language.  This map gets reloaded on a locale change, when the new
        // language's city strings are read from the xml file.
        private HashMap<String, String> mCityNameMap = new HashMap<String, String>();

        private String[] mSectionHeaders;
        private Integer[] mSectionPositions;

        private CityNameComparator mSortByNameComparator = new CityNameComparator();
        private CityGmtOffsetComparator mSortByTimeComparator = new CityGmtOffsetComparator();

        private final LayoutInflater mInflater;
        private boolean mIs24HoursMode; // AM/PM or 24 hours mode

        private final String mPattern12;
        private final String mPattern24;

        private int mSelectedEndPosition = 0;

        private Filter mFilter = new Filter() {

            @Override
            protected synchronized FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                String modifiedQuery = constraint.toString().trim().toUpperCase();

                ArrayList<CityObj> filteredList = new ArrayList<>();
                ArrayList<String> sectionHeaders = new ArrayList<>();
                ArrayList<Integer> sectionPositions = new ArrayList<>();

                // Update the list first when user using search filter
                final Collection<CityObj> selectedCities = mUserSelectedCities.values();
                mSelectedCities = selectedCities.toArray(new CityObj[selectedCities.size()]);
                // If the search query is empty, add in the selected cities
                if (TextUtils.isEmpty(modifiedQuery) && mSelectedCities != null) {
                    if (mSelectedCities.length > 0) {
                        sectionHeaders.add("+");
                        sectionPositions.add(0);
                        filteredList.add(new CityObj(mSelectedCitiesHeaderString,
                                mSelectedCitiesHeaderString, null, null));
                    }
                    for (CityObj city : mSelectedCities) {
                        city.isHeader = false;
                        filteredList.add(city);
                    }
                }

                final HashSet<String> selectedCityIds = new HashSet<>();
                for (CityObj c : mSelectedCities) {
                    selectedCityIds.add(c.mCityId);
                }
                mSelectedEndPosition = filteredList.size();

                long currentTime = System.currentTimeMillis();
                String val = null;
                int offset = -100000; //some value that cannot be a real offset
                for (CityObj city : mCities) {

                    // If the city is a deleted entry, ignore it.
                    if (city.mCityId.equals(DELETED_ENTRY)) {
                        continue;
                    }

                    // If the search query is empty, add section headers.
                    if (TextUtils.isEmpty(modifiedQuery)) {
                        if (!selectedCityIds.contains(city.mCityId)) {
                            // If the list is sorted by name, and the city has an index
                            // different than the previous city's index, update the section header.
                            if (mSortType == SORT_BY_NAME
                                    && !city.mCityIndex.equals(val)) {
                                val = city.mCityIndex.toUpperCase();
                                sectionHeaders.add(val);
                                sectionPositions.add(filteredList.size());
                                city.isHeader = true;
                            } else {
                                city.isHeader = false;
                            }

                            // If the list is sorted by time, and the gmt offset is different than
                            // the previous city's gmt offset, insert a section header.
                            if (mSortType == SORT_BY_GMT_OFFSET) {
                                TimeZone timezone = TimeZone.getTimeZone(city.mTimeZone);
                                int newOffset = timezone.getOffset(currentTime);
                                if (offset != newOffset) {
                                    offset = newOffset;
                                    // Because JB fastscroll only supports ~1 char strings
                                    // and KK ellipsizes strings, trim section headers to the
                                    // nearest hour.
                                    final String offsetString = Utils.getGMTHourOffset(timezone,
                                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT
                                            /* useShortForm */ );
                                    sectionHeaders.add(offsetString);
                                    sectionPositions.add(filteredList.size());
                                    city.isHeader = true;
                                } else {
                                    city.isHeader = false;
                                }
                            }

                            filteredList.add(city);
                        }
                    } else {
                        // If the city name begins with the non-empty query, add it into the list.
                        String cityName = city.mCityName.trim().toUpperCase();
                        if (city.mCityId != null && cityName.startsWith(modifiedQuery)) {
                            city.isHeader = false;
                            filteredList.add(city);
                        }
                    }
                }

                mSectionHeaders = sectionHeaders.toArray(new String[sectionHeaders.size()]);
                mSectionPositions = sectionPositions.toArray(new Integer[sectionPositions.size()]);

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mDisplayedCitiesList = (ArrayList<CityObj>) results.values;
                if (mPosition >= 0) {
                    mCitiesList.setSelectionFromTop(mPosition, 0);
                    mPosition = -1;
                }
                notifyDataSetChanged();
            }
        };

        public CityAdapter(
                Context context, LayoutInflater factory) {
            super();
            mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
            mInflater = factory;

            // Load the cities from xml.
            mCities = Utils.loadCitiesFromXml(context);

            // Reload the city name map with the recently parsed city names of the currently
            // selected language for use with selected cities.
            mCityNameMap.clear();
            for (CityObj city : mCities) {
                mCityNameMap.put(city.mCityId, city.mCityName);
            }

            // Re-organize the selected cities into an array.
            Collection<CityObj> selectedCities = mUserSelectedCities.values();
            mSelectedCities = selectedCities.toArray(new CityObj[selectedCities.size()]);

            // Override the selected city names in the shared preferences with the
            // city names in the updated city name map, which will always reflect the
            // current language.
            for (CityObj city : mSelectedCities) {
                String newCityName = mCityNameMap.get(city.mCityId);
                if (newCityName != null) {
                    city.mCityName = newCityName;
                }
            }

            mPattern24 = Utils.isJBMR2OrLater()
                    ? DateFormat.getBestDateTimePattern(Locale.getDefault(), "Hm")
                    : getString(R.string.time_format_24_mode);

            // There's an RTL layout bug that causes jank when fast-scrolling through
            // the list in 12-hour mode in an RTL locale. We can work around this by
            // ensuring the strings are the same length by using "hh" instead of "h".
            String pattern12 = Utils.isJBMR2OrLater()
                    ? DateFormat.getBestDateTimePattern(Locale.getDefault(), "hma")
                    : getString(R.string.time_format_12_mode);

            if (mLayoutDirection == View.LAYOUT_DIRECTION_RTL) {
                pattern12 = pattern12.replaceAll("h", "hh");
            }
            mPattern12 = pattern12;

            sortCities(mSortType);
            set24HoursMode(context);
        }

        public void toggleSort() {
            if (mSortType == SORT_BY_NAME) {
                sortCities(SORT_BY_GMT_OFFSET);
            } else {
                sortCities(SORT_BY_NAME);
            }
        }

        private void sortCities(final int sortType) {
            mSortType = sortType;
            Arrays.sort(mCities, sortType == SORT_BY_NAME ? mSortByNameComparator
                    : mSortByTimeComparator);
            if (mSelectedCities != null) {
                Arrays.sort(mSelectedCities, sortType == SORT_BY_NAME ? mSortByNameComparator
                        : mSortByTimeComparator);
            }
            mPrefs.edit().putInt(PREF_SORT, sortType).commit();
            mFilter.filter(mQueryTextBuffer.toString());
        }

        @Override
        public int getCount() {
            return mDisplayedCitiesList != null ? mDisplayedCitiesList.size() : 0;
        }

        @Override
        public Object getItem(int p) {
            if (mDisplayedCitiesList != null && p >= 0 && p < mDisplayedCitiesList.size()) {
                return mDisplayedCitiesList.get(p);
            }
            return null;
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public boolean isEnabled(int p) {
            return mDisplayedCitiesList != null && mDisplayedCitiesList.get(p).mCityId != null;
        }

        @Override
        public synchronized View getView(int position, View view, ViewGroup parent) {
            if (mDisplayedCitiesList == null || position < 0
                    || position >= mDisplayedCitiesList.size()) {
                return null;
            }
            CityObj c = mDisplayedCitiesList.get(position);
            // Header view: A CityObj with nothing but the "selected cities" label
            if (c.mCityId == null) {
                if (view == null) {
                    view = mInflater.inflate(R.layout.city_list_header, parent, false);
                }
            } else { // City view
                // Make sure to recycle a City view only
                if (view == null) {
                    view = mInflater.inflate(R.layout.city_list_item, parent, false);
                    final CityViewHolder holder = new CityViewHolder();
                    holder.index = (TextView) view.findViewById(R.id.index);
                    holder.name = (TextView) view.findViewById(R.id.city_name);
                    holder.time = (TextView) view.findViewById(R.id.city_time);
                    holder.selected = (CheckBox) view.findViewById(R.id.city_onoff);
                    view.setTag(holder);
                }
                view.setOnClickListener(CitiesActivity.this);
                CityViewHolder holder = (CityViewHolder) view.getTag();

                holder.selected.setTag(c);
                holder.selected.setChecked(mUserSelectedCities.containsKey(c.mCityId));
                holder.selected.setOnCheckedChangeListener(CitiesActivity.this);
                holder.name.setText(c.mCityName, TextView.BufferType.SPANNABLE);
                holder.time.setText(getTimeCharSequence(c.mTimeZone));
                if (c.isHeader) {
                    holder.index.setVisibility(View.VISIBLE);
                    if (mSortType == SORT_BY_NAME) {
                        holder.index.setText(c.mCityIndex);
                        holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                    } else { // SORT_BY_GMT_OFFSET
                        holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        holder.index.setText(Utils.getGMTHourOffset(
                                TimeZone.getTimeZone(c.mTimeZone), false));
                    }
                } else {
                    // If not a header, use the invisible index for left padding
                    holder.index.setVisibility(View.INVISIBLE);
                }
                // skip checkbox and other animations
                view.jumpDrawablesToCurrentState();
            }
            return view;
        }

        private CharSequence getTimeCharSequence(String timeZone) {
            mCalendar.setTimeZone(TimeZone.getTimeZone(timeZone));
            return DateFormat.format(mIs24HoursMode ? mPattern24 : mPattern12, mCalendar);
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (mDisplayedCitiesList.get(position).mCityId != null)
                    ? VIEW_TYPE_CITY : VIEW_TYPE_HEADER;
        }

        private class CityViewHolder {
            TextView index;
            TextView name;
            TextView time;
            CheckBox selected;
        }

        public void set24HoursMode(Context c) {
            mIs24HoursMode = DateFormat.is24HourFormat(c);
            notifyDataSetChanged();
        }

        @Override
        public int getPositionForSection(int section) {
            return !isEmpty(mSectionPositions) ? mSectionPositions[section] : 0;
        }


        @Override
        public int getSectionForPosition(int p) {
            final Integer[] positions = mSectionPositions;
            if (!isEmpty(positions)) {
                for (int i = 0; i < positions.length - 1; i++) {
                    if (p >= positions[i]
                            && p < positions[i + 1]) {
                        return i;
                    }
                }
                if (p >= positions[positions.length - 1]) {
                    return positions.length - 1;
                }
            }
            return 0;
        }

        @Override
        public Object[] getSections() {
            return mSectionHeaders;
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        private boolean isEmpty(Object[] array) {
            return array == null || array.length == 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        mFactory = LayoutInflater.from(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSortType = mPrefs.getInt(PREF_SORT, SORT_BY_NAME);
        mSelectedCitiesHeaderString = getString(R.string.selected_cities_label);
        if (savedInstanceState != null) {
            mQueryTextBuffer.append(savedInstanceState.getString(KEY_SEARCH_QUERY));
            mSearchMode = savedInstanceState.getBoolean(KEY_SEARCH_MODE);
            mPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        }
        updateLayout();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(KEY_SEARCH_QUERY, mQueryTextBuffer.toString());
        bundle.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        bundle.putInt(KEY_LIST_POSITION, mCitiesList.getFirstVisiblePosition());
    }

    private void updateLayout() {
        setContentView(R.layout.cities_activity);
        mCitiesList = (ListView) findViewById(R.id.cities_list);
        setFastScroll(TextUtils.isEmpty(mQueryTextBuffer.toString().trim()));
        mCitiesList.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        mUserSelectedCities = Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this));
        mAdapter = new CityAdapter(this, mFactory);
        mCitiesList.setAdapter(mAdapter);
    }

    private void setFastScroll(boolean enabled) {
        if (mCitiesList != null) {
            mCitiesList.setFastScrollAlwaysVisible(enabled);
            mCitiesList.setFastScrollEnabled(enabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.set24HoursMode(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this),
                mUserSelectedCities);
        Intent i = new Intent(Cities.WORLDCLOCK_UPDATE_INTENT);
        sendBroadcast(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        // No activity found to match the intent - ignore
                    }
                }
                return true;
            case R.id.menu_item_sort:
                if (mAdapter != null) {
                    mAdapter.toggleSort();
                    setFastScroll(TextUtils.isEmpty(mQueryTextBuffer.toString().trim()));
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cities_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }

        MenuItem searchMenu = menu.findItem(R.id.menu_item_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchMenu);
        mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mSearchView.setOnSearchClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mSearchMode = true;
            }
        });
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {

            @Override
            public boolean onClose() {
                mSearchMode = false;
                return false;
            }
        });
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setQuery(mQueryTextBuffer.toString(), false);
            if (mSearchMode) {
                mSearchView.requestFocus();
                mSearchView.setIconified(false);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sortMenuItem = menu.findItem(R.id.menu_item_sort);
        if (mSortType == SORT_BY_NAME) {
            sortMenuItem.setTitle(getString(R.string.menu_item_sort_by_gmt_offset));
        } else {
            sortMenuItem.setTitle(getString(R.string.menu_item_sort_by_name));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCheckedChanged(CompoundButton b, boolean checked) {
        CityObj c = (CityObj) b.getTag();
        if (checked) {
            mUserSelectedCities.put(c.mCityId, c);
        } else {
            mUserSelectedCities.remove(c.mCityId);
        }
    }

    @Override
    public void onClick(View v) {
        CompoundButton b = (CompoundButton) v.findViewById(R.id.city_onoff);
        boolean checked = b.isChecked();
        onCheckedChanged(b, checked);
        b.setChecked(!checked);
    }

    @Override
    public boolean onQueryTextChange(String queryText) {
        mQueryTextBuffer.setLength(0);
        mQueryTextBuffer.append(queryText);
        mCitiesList.setFastScrollEnabled(TextUtils.isEmpty(mQueryTextBuffer.toString().trim()));
        mAdapter.getFilter().filter(queryText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String arg0) {
        return false;
    }
}

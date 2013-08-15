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

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
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
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.deskclock.Alarms;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * Cities chooser for the world clock
 */
public class CitiesActivity extends Activity implements OnCheckedChangeListener,
        View.OnClickListener, OnQueryTextListener {

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

    /***
     * Adapter for a list of cities with the respected time zone. The Adapter
     * sorts the list alphabetically and create an indexer.
     ***/
    private class CityAdapter extends BaseAdapter implements Filterable, SectionIndexer {
        private static final String DELETED_ENTRY = "C0";
        private final HashMap<String, CityObj> mSelectedCitiesList;  // selected cities
        private List<CityObj> mDisplayedCitiesList;
        private CityObj[] mCities;

        private String[] mSortByNameSectionHeaders;
        private Integer[] mSortByNameSectionPositions;

        private String[] mSortByTimeSectionHeaders;
        private Integer[] mSortByTimeSectionPositions;

        private CityNameComparator mSortByNameComparator = new CityNameComparator();
        private CityGmtOffsetComparator mSortByGmtOffsetComparator = new CityGmtOffsetComparator();

        private final LayoutInflater mInflater;
        private boolean mIs24HoursMode; // AM/PM or 24 hours mode

        private Filter mFilter = new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                String modifiedQuery = constraint.toString().trim().toUpperCase();

                ArrayList<CityObj> filteredList = new ArrayList<CityObj>();
                int positionIndex = 0;
                int i = 0;
                while (i < mCities.length) {
                    CityObj city = mCities[i];

                    // If the city is a deleted entry, ignore it.
                    if (city.mCityId.equals(DELETED_ENTRY)) {
                        i++;
                        continue;
                    }

                    // If the search query is empty, add section headers
                    if (TextUtils.isEmpty(modifiedQuery)) {

                        // If the list is sorted by name, and the position index is correct,
                        // insert a section header into the list
                        if (mSortType == SORT_BY_NAME &&
                                mSortByNameSectionPositions.length > positionIndex &&
                                mSortByNameSectionPositions[positionIndex] == filteredList.size()) {
                            String name = mSortByNameSectionHeaders[positionIndex];
                            filteredList.add(new CityObj(name, null, null));
                            positionIndex++;
                            continue;
                        }

                        // If the list is sorted by time, and the position index is correct,
                        // insert a section header into the list
                        if (mSortType == SORT_BY_GMT_OFFSET &&
                                mSortByTimeSectionPositions.length > positionIndex &&
                                mSortByTimeSectionPositions[positionIndex] == filteredList.size()) {
                            String timezone = mSortByTimeSectionHeaders[positionIndex];
                            filteredList.add(new CityObj(null, timezone, null));
                            positionIndex++;
                            continue;
                        }
                    }

                    // If the city name begins with the query, add the city into the list.
                    // If the query is empty, the city will automatically be added to the list.
                    String cityName = city.mCityName.trim().toUpperCase();
                    if (city.mCityId != null && cityName.startsWith(modifiedQuery)) {
                        filteredList.add(city);
                    }
                    i++;
                }
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
                Context context, HashMap<String, CityObj> selectedList, LayoutInflater factory) {
            super();
            loadCities(context);
            mSelectedCitiesList = selectedList;
            mInflater = factory;
            mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            set24HoursMode(context);
        }


        private void loadCities(Context c) {
            mCities = Utils.loadCitiesFromXml(c);
            if (mCities == null) {
                return;
            }

            // Sort alphabetically and populate section headers for sort-by-name
            Arrays.sort(mCities, mSortByNameComparator);
            String val = null;
            ArrayList<String> sections = new ArrayList<String>();
            ArrayList<Integer> positions = new ArrayList<Integer>();
            int count = 0;
            for (CityObj city : mCities) {
                if (city.mCityId.equals(DELETED_ENTRY)) {
                    continue;
                }

                if (!city.mCityName.substring(0, 1).equals(val)) {
                    val = city.mCityName.substring(0, 1).toUpperCase();
                    sections.add(val);
                    positions.add(count);
                    count++;
                }
                count++;
            }
            mSortByNameSectionHeaders = sections.toArray(new String[sections.size()]);
            mSortByNameSectionPositions = positions.toArray(new Integer[positions.size()]);

            // Sort by GMT offset and populate section headers for sort-by-time
            Arrays.sort(mCities, mSortByGmtOffsetComparator);
            int offset = -100000; // some number that cannot be a real offset
            val = null;
            sections.clear();
            positions.clear();
            ArrayList<String> scrollLabels = new ArrayList<String>();
            count = 0;
            for (CityObj city : mCities) {
                if (city.mCityId.equals(DELETED_ENTRY)) {
                    continue;
                }

                TimeZone timezone = TimeZone.getTimeZone(city.mTimeZone);
                int newOffset = timezone.getRawOffset();
                if (newOffset != offset) {
                    offset = newOffset;
                    sections.add(Utils.getGMTHourOffset(timezone, true));
                    positions.add(count);
                    count++;
                }
                count++;
            }
            mSortByTimeSectionHeaders = sections.toArray(new String[sections.size()]);
            mSortByTimeSectionPositions = positions.toArray(new Integer[positions.size()]);

            sortCities(mSortType);
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
                    : mSortByGmtOffsetComparator);
            mPrefs.edit().putInt(PREF_SORT, sortType).commit();
            mFilter.filter(mQueryTextBuffer.toString());
        }

        @Override
        public int getCount() {
            return (mDisplayedCitiesList != null) ? mDisplayedCitiesList.size() : 0;
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
        public View getView(int position, View view, ViewGroup parent) {
            if (mDisplayedCitiesList == null || position < 0
                    || position >= mDisplayedCitiesList.size()) {
                return null;
            }
            CityObj c = mDisplayedCitiesList.get(position);
            // Header view: A CityObj with nothing but the first letter as the name
            if (c.mCityId == null) {
                if (view == null || view.findViewById(R.id.header) == null) {
                    view = mInflater.inflate(R.layout.city_list_header, parent, false);
                }
                TextView header = (TextView) view.findViewById(R.id.header);
                header.setText(mSortType == SORT_BY_NAME ? c.mCityName : c.mTimeZone);
            } else { // City view
                // Make sure to recycle a City view only
                if (view == null || view.findViewById(R.id.city_name) == null) {
                    view = mInflater.inflate(R.layout.city_list_item, parent, false);
                }
                view.setOnClickListener(CitiesActivity.this);
                TextView name = (TextView) view.findViewById(R.id.city_name);
                TextView tz = (TextView) view.findViewById(R.id.city_time);
                CheckBox cb = (CheckBox) view.findViewById(R.id.city_onoff);
                cb.setTag(c);
                cb.setChecked(mSelectedCitiesList.containsKey(c.mCityId));
                cb.setOnCheckedChangeListener(CitiesActivity.this);
                mCalendar.setTimeZone(TimeZone.getTimeZone(c.mTimeZone));
                tz.setText(DateFormat.format(mIs24HoursMode ? "k:mm" : "h:mmaa", mCalendar));
                name.setText(c.mCityName, TextView.BufferType.SPANNABLE);
            }
            return view;
        }

        public void set24HoursMode(Context c) {
            mIs24HoursMode = Alarms.get24HourMode(c);
            notifyDataSetChanged();
        }

        @Override
        public int getPositionForSection(int section) {
            Integer[] positions = mSortType == SORT_BY_NAME ? mSortByNameSectionPositions :
                mSortByTimeSectionPositions;
            return (positions != null) ? (Integer) positions[section] : 0;
        }


        @Override
        public int getSectionForPosition(int p) {
            Integer[] positions = mSortType == SORT_BY_NAME ? mSortByNameSectionPositions :
                mSortByTimeSectionPositions;
            if (positions != null) {
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
            return mSortType == SORT_BY_NAME ? mSortByNameSectionHeaders
                    : mSortByTimeSectionHeaders;
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFactory = LayoutInflater.from(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSortType = mPrefs.getInt(PREF_SORT, SORT_BY_NAME);
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
        mCitiesList.setFastScrollEnabled(TextUtils.isEmpty(mQueryTextBuffer.toString().trim()));
        mCitiesList.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        mUserSelectedCities = Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this));
        mAdapter = new CityAdapter(this, mUserSelectedCities, mFactory);
        mCitiesList.setAdapter(mAdapter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
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
                    mCitiesList.setFastScrollEnabled(
                            TextUtils.isEmpty(mQueryTextBuffer.toString().trim()));
                }
                return true;
            case android.R.id.home:
                Intent intent = new Intent(this, DeskClock.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
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
        mSearchView = (SearchView) searchMenu.getActionView();
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

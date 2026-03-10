// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only

package com.best.deskclock.worldclock;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CITY_NOTE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

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
public class CityAdapter extends BaseAdapter implements View.OnClickListener,
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
    private final SharedPreferences mPrefs;
    private final Typeface mRegularTypeface;
    private final Typeface mBoldTypeface;

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
    private final Set<City> mUserSelectedCities = new LinkedHashSet<>();

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

    private String mCurrentQueryText = "";

    private Comparator<City> mCachedComparator;
    private DataModel.CitySort mCachedCitySort;

    public CityAdapter(Context context) {
        mContext = context;
        mPrefs = getDefaultSharedPreferences(context);
        mInflater = LayoutInflater.from(context);

        String fontPath = SettingsDAO.getGeneralFont(mPrefs);
        mRegularTypeface = ThemeUtils.loadFont(fontPath);
        mBoldTypeface = ThemeUtils.boldTypeface(fontPath);

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

            return switch (itemViewType) {
                case VIEW_TYPE_SELECTED_CITIES_HEADER -> null;
                case VIEW_TYPE_CITY -> mFilteredCities.get(position - 1);
                default -> throw new IllegalStateException("unexpected item view type: " + itemViewType);
            };
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
                    view.setOnClickListener(null);

                    TextView cityListHeader = view.findViewById(R.id.city_list_header);
                    cityListHeader.setTypeface(mRegularTypeface);
                }
                return view;
            }

            case VIEW_TYPE_CITY -> {
                final City city = getItem(position);
                if (city == null) {
                    throw new IllegalStateException("The desired city does not exist");
                }

                final TimeZone timeZone = city.getTimeZone();
                CityItemHolder holder;

                // Inflate a new view if necessary.
                if (view == null) {
                    view = mInflater.inflate(R.layout.city_list_item, parent, false);
                    final TextView index = view.findViewById(R.id.index);
                    final TextView name = view.findViewById(R.id.city_name);
                    final TextView time = view.findViewById(R.id.city_time);
                    final CheckBox selected = view.findViewById(R.id.city_onoff);

                    index.setTypeface(mBoldTypeface);
                    name.setTypeface(mRegularTypeface);
                    time.setTypeface(mRegularTypeface);

                    holder = new CityItemHolder(index, name, time, selected);
                    view.setTag(holder);
                } else {
                    holder = (CityItemHolder) view.getTag();
                }

                // Bind data into the child views.
                holder.selected.setOnCheckedChangeListener(null);
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
                            final long now = System.currentTimeMillis();
                            holder.index.setText(getGMTHourOffset(timeZone, false, now));
                            holder.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        }
                    }
                }

                // Skip checkbox and other animations
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

            // Delete the associated note
            mPrefs.edit().remove(KEY_CITY_NOTE + city.getId()).apply();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof CityItemHolder holder) {
            holder.selected.toggle();
        }
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

            final long now = System.currentTimeMillis();

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
                            sections.add(getGMTHourOffset(timezone, false, now));
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
     * Returns string denoting the timezone hour offset (e.g. GMT -8:00)
     *
     * @param useShortForm Whether to return a short form of the header that rounds to the
     *                     nearest hour and excludes the "GMT" prefix
     */
    public static String getGMTHourOffset(TimeZone timezone, boolean useShortForm, long now) {
        final int gmtOffset = timezone.getOffset(now);

        final int absGmtOffset = Math.abs(gmtOffset);
        final long hour = absGmtOffset / DateUtils.HOUR_IN_MILLIS;
        final long min = (absGmtOffset % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS;

        final String sign = gmtOffset >= 0 ? "+" : "-";

        if (useShortForm) {
            return String.format(Locale.ENGLISH, "%s%d", sign, hour);
        } else {
            return String.format(Locale.ENGLISH, "UTC %s%d:%02d", sign, hour, min);
        }
    }

    /**
     * Clear the section headers to force them to be recomputed if they are now stale.
     */
    public void clearSectionHeaders() {
        mSectionHeaders = null;
        mSectionHeaderPositions = null;
    }

    /**
     * Rebuilds all internal data structures from scratch.
     */
    public void refresh() {
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
        filter(mCurrentQueryText);
    }

    /**
     * Filter the cities using the given {@code queryText}.
     */
    public void filter(String queryText) {
        mCurrentQueryText = queryText;

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

    public boolean isFiltering() {
        return !TextUtils.isEmpty(mCurrentQueryText.trim());
    }

    public Collection<City> getSelectedCities() {
        return mUserSelectedCities;
    }

    private boolean hasHeader() {
        return !isFiltering() && mOriginalUserSelectionCount > 0;
    }

    private DataModel.CitySort getCitySort() {
        return SettingsDAO.getCitySort(mPrefs);
    }

    private Comparator<City> getCitySortComparator() {
        DataModel.CitySort currentSort = getCitySort();

        if (mCachedComparator == null || mCachedCitySort != currentSort) {
            mCachedComparator = DataModel.getDataModel().getCityIndexComparator();
            mCachedCitySort = currentSort;
        }

        return mCachedComparator;
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

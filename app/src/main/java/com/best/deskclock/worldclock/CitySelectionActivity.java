/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.worldclock;

import static androidx.core.util.TypedValueCompat.dpToPx;

import static com.best.deskclock.settings.PreferencesKeys.KEY_CITY_NOTE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.BaseActivity;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.CitiesActivityBinding;
import com.best.deskclock.databinding.CityListHeaderMainTitleBinding;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.Locale;

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
public final class CitySelectionActivity extends BaseActivity {

    private static final String KEY_SEARCH_QUERY = "search_query";

    private CitiesActivityBinding mBinding;

    /**
     * The adapter that presents all the selected and unselected cities.
     */
    private CityAdapter mCitiesAdapter;

    public SearchView mSearchView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = CitiesActivityBinding.inflate(getLayoutInflater());

        setContentView(mBinding.getRoot());

        setSupportActionBar(mBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mSearchView = new SearchView(this);
        mSearchView.setLayoutParams(new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mSearchView.setQueryHint(getString(R.string.city_search_hint));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mSearchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mSearchView.setBackground(
            ThemeUtils.pillBackgroundFromAttr(this, getDisplayMetrics(), com.google.android.material.R.attr.colorSecondaryContainer));

        // Apply custom font to the search text
        TextView searchText = mSearchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchText.setTypeface(getGeneralTypeface());

        // Use a rounded icon for the search icon
        ImageView searchIcon = mSearchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (searchIcon != null) {
            searchIcon.setImageResource(R.drawable.ic_search);
        }

        // Hide the bottom bar of the search field
        View searchPlate = mSearchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackground(null);
        }

        mBinding.toolbar.addView(mSearchView);

        Locale locale = getLocale();
        String pattern24 = DateFormat.getBestDateTimePattern(locale, "Hm");
        String pattern12 = DateFormat.getBestDateTimePattern(locale, "hma");
        boolean is24HoursMode = DateFormat.is24HourFormat(this);

        if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
            // There's an RTL layout bug that causes jank when fast-scrolling through
            // the list in 12-hour mode in an RTL locale. We can work around this by
            // ensuring the strings are the same length by using "hh" instead of "h".
            pattern12 = pattern12.replace("h", "hh");
        }

        UiConfig.TimeFormat timeFormat = new UiConfig.TimeFormat(locale, pattern12, pattern24, is24HoursMode);

        mCitiesAdapter = new CityAdapter(this, getDataModel(), getFontsConfig(), timeFormat, new CityAdapter.CityAdapterProvider() {
            @Override
            public DataModel.CitySort getCitySort() {
                return SettingsDAO.getCitySort(getPrefs());
            }

            @Override
            public void onCityDeselected(@NonNull City city) {
                // Delete the associated note
                getPrefs().edit().remove(KEY_CITY_NOTE + city.getId()).apply();
            }
        }
        );

        mSearchView.post(() -> mSearchView.clearFocus());

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(@NonNull String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(@NonNull String query) {
                mCitiesAdapter.filter(query);
                updateFastScrolling();
                return true;
            }
        });

        CityListHeaderMainTitleBinding headerBinding =
            CityListHeaderMainTitleBinding.inflate(getLayoutInflater(), mBinding.citiesList, false);
        headerBinding.cityListHeaderMainTitle.setTypeface(getGeneralTypeface());
        headerBinding.cityListHeaderMainTitle.setOnClickListener(null);

        mBinding.citiesList.addHeaderView(headerBinding.getRoot());

        mBinding.citiesList.setAdapter(mCitiesAdapter);

        applyWindowInsets();

        updateFastScrolling();

        if (savedInstanceState != null) {
            String query = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            mSearchView.setQuery(query, false);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                ThemeUtils.finishActivityWithTransition(CitySelectionActivity.this, SettingsDAO.isFadeTransitionsEnabled(getPrefs()));
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);

        bundle.putString(KEY_SEARCH_QUERY, mSearchView.getQuery().toString());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Recompute the contents of the adapter before displaying on screen.
        boolean is24HoursMode = DateFormat.is24HourFormat(this);
        mCitiesAdapter.refresh(is24HoursMode);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Save the selected cities.
        getDataModel().setSelectedCities(mCitiesAdapter.getSelectedCities());
    }

    @Override
    protected void onDestroy() {
        mSearchView = null;

        mBinding = null;

        super.onDestroy();
    }

    @SuppressLint("AlwaysShowAction")
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        menu.add(Menu.NONE, 0, Menu.NONE, getMenuTitle()).setIcon(R.drawable.ic_sort).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        mBinding.toolbar.post(() -> ThemeUtils.applyToolbarTooltips(mBinding.toolbar, getGeneralTypeface(), getDisplayMetrics()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Save the new sort order.
        getDataModel().toggleCitySort();

        item.setTitle(getMenuTitle());

        // Section headers are influenced by sort order and must be cleared.
        mCitiesAdapter.clearSectionHeaders();

        // Honor the new sort order in the adapter.
        mCitiesAdapter.filter(mSearchView.getQuery().toString());

        return super.onOptionsItemSelected(item);
    }

    /**
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mBinding.citySelectionRootView, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);

            int bottomPadding = (int) dpToPx(10, getDisplayMetrics());
            mBinding.citiesList.setPadding(0, 0, 0, bars.bottom + bottomPadding);
        });
    }

    private int getMenuTitle() {
        if (SettingsDAO.getCitySort(getPrefs()) == DataModel.CitySort.NAME) {
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
        mBinding.citiesList.setFastScrollAlwaysVisible(enabled);
        mBinding.citiesList.setFastScrollEnabled(enabled);
    }

}

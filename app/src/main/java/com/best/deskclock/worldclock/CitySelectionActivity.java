/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.worldclock;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

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

    /**
     * The list of all selected and unselected cities, indexed and possibly filtered.
     */
    private ListView mCitiesList;
    /**
     * The adapter that presents all of the selected and unselected cities.
     */
    private CityAdapter mCitiesAdapter;

    private SharedPreferences mPrefs;
    private Toolbar mToolbar;
    private View mRootView;
    public SearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ThemeUtils.allowDisplayCutout(getWindow());

        mPrefs = getDefaultSharedPreferences(this);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs));

        setContentView(R.layout.cities_activity);

        mRootView = findViewById(R.id.city_selection_root_view);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mSearchView = new SearchView(this);
        mSearchView.setLayoutParams(new Toolbar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mSearchView.setQueryHint(getString(R.string.city_search_hint));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mSearchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mSearchView.setBackground(ThemeUtils.pillBackgroundFromAttr(
                this, com.google.android.material.R.attr.colorSecondaryContainer));

        // Apply custom font to the search text
        TextView searchText = mSearchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchText.setTypeface(typeface);

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

        mToolbar.addView(mSearchView);

        mSearchView.post(() -> mSearchView.clearFocus());

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        });

        mCitiesAdapter = new CityAdapter(this);

        mCitiesList = findViewById(R.id.cities_list);
        View headerMainTitleView = getLayoutInflater().inflate(
                R.layout.city_list_header_main_title, mCitiesList, false);
        TextView headerMainTitleText = headerMainTitleView.findViewById(R.id.city_list_header_main_title);
        headerMainTitleText.setTypeface(typeface);
        headerMainTitleText.setOnClickListener(null);

        mCitiesList.addHeaderView(headerMainTitleView);

        mCitiesList.setAdapter(mCitiesAdapter);

        applyWindowInsets();

        updateFastScrolling();

        if (savedInstanceState != null) {
            String query = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            mSearchView.setQuery(query, false);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
                    if (SdkUtils.isAtLeastAndroid14()) {
                        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,
                                R.anim.fade_in, R.anim.fade_out);
                    } else {
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                } else {
                    if (SdkUtils.isAtLeastAndroid14()) {
                        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,
                                R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                    } else {
                        overridePendingTransition(
                                R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                    }
                }
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
        mCitiesAdapter.refresh();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Save the selected cities.
        DataModel.getDataModel().setSelectedCities(mCitiesAdapter.getSelectedCities());
    }

    @SuppressLint("AlwaysShowAction")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, Menu.NONE, getMenuTitle()).setIcon(R.drawable.ic_sort)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        mToolbar.post(() -> ThemeUtils.applyToolbarTooltips(mToolbar));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Save the new sort order.
        DataModel.getDataModel().toggleCitySort();

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
        InsetsUtils.doOnApplyWindowInsets(mRootView, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() |
                    WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);

            int bottomPadding = (int) dpToPx(10, getResources().getDisplayMetrics());
            mCitiesList.setPadding(0, 0, 0, bars.bottom + bottomPadding);
        });
    }

    private int getMenuTitle() {
        if (SettingsDAO.getCitySort(mPrefs) == DataModel.CitySort.NAME) {
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

}

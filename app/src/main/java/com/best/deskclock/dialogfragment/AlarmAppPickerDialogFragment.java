// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmAppPickerDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * DialogFragment to select an app that will launch after dismissing/snoozing the alarm.
 */
public class AlarmAppPickerDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of {@link AlarmAppPickerDialogFragment} in the fragment manager.
     */
    public static final String TAG = "alarm_app_picker_dialog";

    public static final String REQUEST_KEY = "alarm_app_picker_request_key";
    public static final String RESULT_APP_NAME = "result_app_name";
    public static final String RESULT_PACKAGE_NAME = "result_package_name";
    private static final String KEY_SEARCH_QUERY = "search_query";

    private AlarmAppPickerDialogBinding mBinding;

    private AppListAdapter mAdapter;

    /**
     * Creates a new instance of {@link AlarmAppPickerDialogFragment} to select an app.
     */
    public static AlarmAppPickerDialogFragment newInstance() {
        return new AlarmAppPickerDialogFragment();
    }

    /**
     * Displays {@link AlarmAppPickerDialogFragment} with the given {@code fragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull AlarmAppPickerDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mBinding != null) {
            outState.putString(KEY_SEARCH_QUERY, mBinding.searchView.getQuery().toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Typeface generalTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(requireContext())));
        final Locale locale = Utils.getLocaleFromContext(requireContext());

        mBinding = AlarmAppPickerDialogBinding.inflate(getLayoutInflater());

        mBinding.appPickerRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        mAdapter = new AppListAdapter(generalTypeface, locale, appItem -> {
            Bundle result = new Bundle();
            result.putString(RESULT_APP_NAME, appItem.appName);
            result.putString(RESULT_PACKAGE_NAME, appItem.packageName);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        });

        mBinding.appPickerRecyclerView.setAdapter(mAdapter);

        loadInstalledApps(generalTypeface, savedInstanceState, locale);

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_apps),
            getString(R.string.select_app_title),
            null,
            mBinding.getRoot(),
            null, null,
            getString(android.R.string.cancel), null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    @Override
    public void onDestroyView() {
        mBinding = null;

        super.onDestroyView();
    }

    private void loadInstalledApps(@NonNull Typeface typeface, @Nullable Bundle savedInstanceState, @NonNull Locale locale) {
        mBinding.searchView.setVisibility(GONE);
        mBinding.appPickerRecyclerView.setVisibility(GONE);

        mBinding.loadingAppsText.setTypeface(typeface);

        mBinding.loadingContainer.setVisibility(VISIBLE);

        final PackageManager packageManager = requireContext().getPackageManager();
        final String currentPackageName = requireContext().getPackageName();

        AppExecutors.getDiskIO().execute(() -> {
            // Search only for applications that can be launched by the user
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
            List<AppItem> appList = new ArrayList<>();

            for (ResolveInfo info : resolveInfos) {
                // Exclude Clock from the list
                if (!info.activityInfo.packageName.equals(currentPackageName)) {
                    String appName = info.loadLabel(packageManager).toString();
                    String packageName = info.activityInfo.packageName;
                    Drawable icon = info.loadIcon(packageManager);
                    String searchName = Utils.removeSpecialCharacters(appName).toLowerCase(locale);

                    appList.add(new AppItem(appName, packageName, icon, searchName));
                }
            }

            final Collator collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);

            Collections.sort(appList, (a, b) -> collator.compare(a.appName, b.appName));

            AppExecutors.getMainThread().post(() -> {
                if (mBinding != null) {
                    mBinding.loadingContainer.setVisibility(GONE);
                    mBinding.searchView.setVisibility(VISIBLE);
                    mBinding.appPickerRecyclerView.setVisibility(VISIBLE);

                    mAdapter.setApps(appList);

                    bindSearchBar(typeface, savedInstanceState);
                }
            });
        });
    }

    private void bindSearchBar(@NonNull Typeface generalTypeface, @Nullable Bundle savedInstanceState) {
        mBinding.searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mBinding.searchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mBinding.searchView.setBackground(ThemeUtils.pillBackgroundFromAttr(
            requireContext(), getResources().getDisplayMetrics(), com.google.android.material.R.attr.colorSecondaryContainer)
        );

        // Apply custom font to the search text
        TextView searchText = mBinding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTypeface(generalTypeface);
        }

        // Hide the bottom bar of the search field
        View searchPlate = mBinding.searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackground(null);
        }

        mBinding.searchView.post(() -> mBinding.searchView.clearFocus());

        mBinding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(@NonNull String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(@NonNull String query) {
                if (mAdapter != null) {
                    mAdapter.filter(query);
                }

                return true;
            }
        });

        if (savedInstanceState != null) {
            String query = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            mBinding.searchView.setQuery(query, false);
        }
    }

    // ***************************************
    // ** INTERNAL CLASSES FOR THE APP LIST **
    // ***************************************

    private record AppItem(@NonNull String appName, @NonNull String packageName, @NonNull Drawable icon, @NonNull String searchName) {
    }

    private static class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {
        private List<AppItem> mAllApps = new ArrayList<>();
        private List<AppItem> mFilteredApps = new ArrayList<>();
        private final Typeface mGeneralTypeface;
        private final Locale mLocale;
        private final OnAppClickListener mListener;

        private AppListAdapter(@NonNull Typeface generalTypeFace, @NonNull Locale locale, @NonNull OnAppClickListener listener) {
            mGeneralTypeface = generalTypeFace;
            mLocale = locale;
            mListener = listener;
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alarm_app_list_item, parent, false);
            return new AppViewHolder(view, mGeneralTypeface);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            AppItem app = mFilteredApps.get(position);

            holder.appNameView.setText(app.appName);
            holder.appIconView.setImageDrawable(app.icon);
            holder.itemView.setOnClickListener(v -> mListener.onAppClick(app));
        }

        @Override
        public int getItemCount() {
            return mFilteredApps.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        void setApps(@NonNull List<AppItem> apps) {
            mAllApps = new ArrayList<>(apps);
            mFilteredApps = new ArrayList<>(apps);

            notifyDataSetChanged();
        }

        @SuppressLint("NotifyDataSetChanged")
        void filter(@NonNull String query) {
            mFilteredApps.clear();

            if (query.trim().isEmpty()) {
                mFilteredApps.addAll(mAllApps);
            } else {
                String cleanedQuery = Utils.removeSpecialCharacters(query).toLowerCase(mLocale);

                for (AppItem app : mAllApps) {
                    if (app.searchName.contains(cleanedQuery)) {
                        mFilteredApps.add(app);
                    }
                }
            }

            notifyDataSetChanged();
        }

        private static class AppViewHolder extends RecyclerView.ViewHolder {
            final ImageView appIconView;
            final TextView appNameView;

            private AppViewHolder(@NonNull View itemView, @NonNull Typeface typeface) {
                super(itemView);

                appIconView = itemView.findViewById(R.id.app_icon);
                appNameView = itemView.findViewById(R.id.app_name);

                appNameView.setTypeface(typeface);
            }
        }

        interface OnAppClickListener {
            void onAppClick(@NonNull AppItem app);
        }
    }
}

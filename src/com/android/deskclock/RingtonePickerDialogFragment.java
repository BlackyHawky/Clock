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

package com.android.deskclock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

/**
 * This ringtone picker offers some flexibility over the system ringtone picker. It can be themed
 * and it allows control of the ringtones that are displayed and their labels.
 */
public final class RingtonePickerDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener,
        FragmentCompat.OnRequestPermissionsResultCallback,
        LoaderManager.LoaderCallbacks<RingtoneManager> {

    private static final String ARGS_KEY_TITLE = "title";
    private static final String ARGS_KEY_DEFAULT_RINGTONE_TITLE = "default_ringtone_title";
    private static final String ARGS_KEY_DEFAULT_RINGTONE_URI = "default_ringtone_uri";
    private static final String ARGS_KEY_EXISTING_RINGTONE_URI = "existing_ringtone_uri";

    private static final String STATE_KEY_REQUESTING_PERMISSION = "requesting_permission";
    private static final String STATE_KEY_SELECTED_RINGTONE_URI = "selected_ringtone_uri";

    private boolean mRequestingPermission;
    private Uri mSelectedRingtoneUri;

    private RingtoneAdapter mRingtoneAdapter;
    private AlertDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore saved instance state.
        if (savedInstanceState != null) {
            mRequestingPermission = savedInstanceState.getBoolean(STATE_KEY_REQUESTING_PERMISSION);
            mSelectedRingtoneUri =
                    savedInstanceState.getParcelable(STATE_KEY_SELECTED_RINGTONE_URI);
        } else {
            // Initialize selection to the existing ringtone.
            mSelectedRingtoneUri = getArguments().getParcelable(ARGS_KEY_EXISTING_RINGTONE_URI);
        }
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        final Bundle args = getArguments();

        mRingtoneAdapter = new RingtoneAdapter(builder.getContext())
                .addStaticRingtone(R.string.silent_ringtone_title, Utils.RINGTONE_SILENT)
                .addStaticRingtone(args.getInt(ARGS_KEY_DEFAULT_RINGTONE_TITLE),
                        (Uri) args.getParcelable(ARGS_KEY_DEFAULT_RINGTONE_URI));
        mDialog = builder.setTitle(args.getInt(ARGS_KEY_TITLE))
                .setSingleChoiceItems(mRingtoneAdapter, -1, this /* listener */)
                .setPositiveButton(android.R.string.ok, this /* listener */)
                .setNegativeButton(android.R.string.cancel, null /* listener */)
                .create();

        return mDialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null
                && ContextCompat.checkSelfPermission(getActivity(), READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
            mRequestingPermission = true;
            FragmentCompat.requestPermissions(this, new String[] { READ_EXTERNAL_STORAGE },
                    0 /* requestCode */);
        } else if (!mRequestingPermission) {
            getLoaderManager().initLoader(0 /* id */, null /* args */, this /* callback */);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (final String permission : permissions) {
            if (READ_EXTERNAL_STORAGE.equals(permission)) {
                mRequestingPermission = false;

                // Show the dialog now that we've prompted the user for permissions.
                mDialog.show();

                getLoaderManager().initLoader(0 /* id */, null /* args */, this /* callback */);
                break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Disable the positive button until we have a valid selection (Note: this is the first
        // point in the fragment's lifecycle that the dialog *should* have all its views).
        final View positiveButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(!mRingtoneAdapter.isEmpty() && mSelectedRingtoneUri != null);
        }

        // Hide the dialog if we are currently requesting permissions.
        if (mRequestingPermission) {
            mDialog.hide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Allow the volume rocker to control the alarm stream volume while the picker is showing.
        mDialog.setVolumeControlStream(AudioManager.STREAM_ALARM);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_KEY_REQUESTING_PERMISSION, mRequestingPermission);
        outState.putParcelable(STATE_KEY_SELECTED_RINGTONE_URI, mSelectedRingtoneUri);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop playing the preview unless we are currently undergoing a configuration change
        // (e.g. orientation).
        final Activity activity = getActivity();
        if (activity != null && !activity.isChangingConfigurations()) {
            RingtonePreviewKlaxon.stop(activity);
        }
    }

    @Override
    public Loader<RingtoneManager> onCreateLoader(int id, Bundle args) {
        return new RingtoneManagerLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<RingtoneManager> loader, RingtoneManager ringtoneManager) {
        // Swap in the new ringtone manager.
        mRingtoneAdapter.setRingtoneManager(ringtoneManager);

        // Preserve the selected ringtone.
        final ListView listView = mDialog.getListView();
        final int checkedPosition = mRingtoneAdapter.getRingtonePosition(mSelectedRingtoneUri);
        if (checkedPosition != ListView.INVALID_POSITION) {
            listView.setItemChecked(checkedPosition, true);

            // Also scroll the list to the selected ringtone (this method is poorly named).
            listView.setSelection(checkedPosition);
        } else {
            // Can't find the selected ringtone, clear the current selection.
            mSelectedRingtoneUri = null;
            listView.clearChoices();
        }

        // Enable the positive button if we have a valid selection (Note: the positive button may
        // be null if this callback returns before onStart).
        final View positiveButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(mSelectedRingtoneUri != null);
        }

        // On M devices the checked view's drawable state isn't updated properly when it is first
        // bound, so we must use a blunt approach to force it to refresh correctly.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            listView.post(new Runnable() {
                @Override
                public void run() {
                    for (int i = listView.getChildCount() - 1; i >= 0; --i) {
                        listView.getChildAt(i).refreshDrawableState();
                    }
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<RingtoneManager> loader) {
        mRingtoneAdapter.setRingtoneManager(null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mSelectedRingtoneUri != null) {
                OnRingtoneSelectedListener listener = null;
                if (getParentFragment() instanceof OnRingtoneSelectedListener) {
                    listener = (OnRingtoneSelectedListener) getParentFragment();
                } else if (getActivity() instanceof OnRingtoneSelectedListener) {
                    listener = (OnRingtoneSelectedListener) getActivity();
                }

                if (listener != null) {
                    listener.onRingtoneSelected(getTag(), mSelectedRingtoneUri);
                }
            }
        } else if (which >= 0) {
            // Update the selected ringtone, enabling the positive button if valid.
            mSelectedRingtoneUri = mRingtoneAdapter.getItem(which);

            // Enable the positive button if we have a valid selection.
            final View positiveButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setEnabled(mSelectedRingtoneUri != null);

            // Play the preview for the clicked ringtone.
            if (mSelectedRingtoneUri == null
                    || mSelectedRingtoneUri.equals(Utils.RINGTONE_SILENT)) {
                RingtonePreviewKlaxon.stop(getActivity());
            } else {
                RingtonePreviewKlaxon.start(getActivity(), mSelectedRingtoneUri);
            }
        }
    }

    /**
     * Callback interface for when a ringtone is selected via a picker. Typically implemented by
     * the activity or fragment which launches the ringtone picker.
     */
    public interface OnRingtoneSelectedListener {
        /**
         * Called when the ringtone picker dialog is confirmed and dismissed.
         *
         * @param tag         the tag of the ringtone picker dialog fragment
         * @param ringtoneUri the uri of the ringtone that was picked
         */
        void onRingtoneSelected(String tag, Uri ringtoneUri);
    }

    public static final class Builder {

        private final Bundle mArgs = new Bundle();

        public Builder setTitle(@StringRes int titleId) {
            mArgs.putInt(ARGS_KEY_TITLE, titleId);
            return this;
        }

        public Builder setDefaultRingtoneTitle(@StringRes int titleId) {
            mArgs.putInt(ARGS_KEY_DEFAULT_RINGTONE_TITLE, titleId);
            return this;
        }

        public Builder setDefaultRingtoneUri(Uri ringtoneUri) {
            mArgs.putParcelable(ARGS_KEY_DEFAULT_RINGTONE_URI, ringtoneUri);
            return this;
        }

        public Builder setExistingRingtoneUri(Uri ringtoneUri) {
            mArgs.putParcelable(ARGS_KEY_EXISTING_RINGTONE_URI, ringtoneUri);
            return this;
        }

        public void show(FragmentManager fragmentManager, String tag) {
            final DialogFragment fragment = new RingtonePickerDialogFragment();
            fragment.setArguments(mArgs);
            fragment.show(fragmentManager, tag);
        }

        public void show(FragmentTransaction fragmentTransaction, String tag) {
            final DialogFragment fragment = new RingtonePickerDialogFragment();
            fragment.setArguments(mArgs);
            fragment.show(fragmentTransaction, tag);
        }
    }

    private static class RingtoneManagerLoader extends AsyncTaskLoader<RingtoneManager> {

        private RingtoneManager mRingtoneManager;
        private Cursor mRingtoneCursor;

        public RingtoneManagerLoader(Context context) {
            super(context);
        }

        @Override
        public RingtoneManager loadInBackground() {
            final RingtoneManager ringtoneManager = new RingtoneManager(getContext());
            ringtoneManager.setType(AudioManager.STREAM_ALARM);

            // Force the ringtone manager to load its ringtones. The cursor will be cached
            // internally by the ringtone manager.
            ringtoneManager.getCursor();

            return ringtoneManager;
        }

        @Override
        public void deliverResult(RingtoneManager ringtoneManager) {
            if (mRingtoneManager != ringtoneManager) {
                if (mRingtoneCursor != null && !mRingtoneCursor.isClosed()) {
                    mRingtoneCursor.close();
                }
                mRingtoneManager = ringtoneManager;
                mRingtoneCursor = mRingtoneManager.getCursor();
            }
            super.deliverResult(ringtoneManager);
        }

        @Override
        protected void onReset() {
            super.onReset();

            if (mRingtoneCursor != null && !mRingtoneCursor.isClosed()) {
                mRingtoneCursor.close();
                mRingtoneCursor = null;
            }
            mRingtoneManager = null;
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();

            if (mRingtoneManager != null) {
                deliverResult(mRingtoneManager);
            } else {
                forceLoad();
            }
        }
    }

    private static class RingtoneAdapter extends BaseAdapter {

        private final List<Pair<Integer, Uri>> mStaticRingtones;
        private final LayoutInflater mLayoutInflater;

        private RingtoneManager mRingtoneManager;
        private Cursor mRingtoneCursor;

        public RingtoneAdapter(Context context) {
            mStaticRingtones = new ArrayList<>(2 /* magic */);
            mLayoutInflater = LayoutInflater.from(context);
        }

        /**
         * Add a static ringtone item to display before the system ones.
         *
         * @param title the title to display for the ringtone
         * @param ringtoneUri the {@link Uri} for the ringtone
         * @return this object so method calls may be chained
         */
        public RingtoneAdapter addStaticRingtone(@StringRes int title, Uri ringtoneUri) {
            if (title != 0 && ringtoneUri != null) {
                mStaticRingtones.add(Pair.create(title, ringtoneUri));
                notifyDataSetChanged();
            }

            return this;
        }

        /**
         * Set the {@link RingtoneManager} to query for system ringtones.
         *
         * @param ringtoneManager the {@link RingtoneManager} to query for system ringtones
         * @return this object so method calls may be chained
         */
        public RingtoneAdapter setRingtoneManager(RingtoneManager ringtoneManager) {
            mRingtoneManager = ringtoneManager;
            mRingtoneCursor = ringtoneManager == null ? null : ringtoneManager.getCursor();
            notifyDataSetChanged();

            return this;
        }

        /**
         * Returns the position of the given ringtone uri.
         *
         * @param ringtoneUri the {@link Uri} to retrieve the position of
         * @return the ringtones position in the adapter
         */
        public int getRingtonePosition(Uri ringtoneUri) {
            if (ringtoneUri == null) {
                return ListView.INVALID_POSITION;
            }

            final int staticRingtoneCount = mStaticRingtones.size();
            for (int position = 0; position < staticRingtoneCount; ++position) {
                if (ringtoneUri.equals(mStaticRingtones.get(position).second)) {
                    return position;
                }
            }

            final int position = mRingtoneManager.getRingtonePosition(ringtoneUri);
            if (position != -1) {
                return position + staticRingtoneCount;
            }
            return ListView.INVALID_POSITION;
        }

        @Override
        public int getCount() {
            if (mRingtoneCursor == null) {
                return 0;
            }
            return mStaticRingtones.size() + mRingtoneCursor.getCount();
        }

        @Override
        public Uri getItem(int position) {
            final int staticRingtoneCount = mStaticRingtones.size();
            if (position < staticRingtoneCount) {
                return mStaticRingtones.get(position).second;
            }
            return mRingtoneManager.getRingtoneUri(position - staticRingtoneCount);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @SuppressLint("PrivateResource")
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                // Use AlertDialog's singleChoiceItemLayout directly here, if this breaks in the
                // future just copy the layout to DeskClock's res/.
                view = mLayoutInflater.inflate(R.layout.select_dialog_singlechoice_material,
                        parent, false /* attachToRoot */);
            }

            final TextView textView = (TextView) view.findViewById(android.R.id.text1);
            final int staticRingtoneCount = mStaticRingtones.size();
            if (position < staticRingtoneCount) {
                textView.setText(mStaticRingtones.get(position).first);
            } else {
                mRingtoneCursor.moveToPosition(position - staticRingtoneCount);
                textView.setText(mRingtoneCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
            }

            return view;
        }
    }
}

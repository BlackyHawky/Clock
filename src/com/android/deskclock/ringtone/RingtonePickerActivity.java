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

package com.android.deskclock.ringtone;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.deskclock.BaseActivity;
import com.android.deskclock.DropShadowController;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.ItemAdapter.OnItemClickedListener;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.RingtonePreviewKlaxon;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.alarms.AlarmUpdateHandler;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;

import java.util.List;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.media.RingtoneManager.TYPE_ALARM;
import static android.provider.OpenableColumns.DISPLAY_NAME;
import static com.android.deskclock.ItemAdapter.ItemViewHolder.Factory;
import static com.android.deskclock.ringtone.AddCustomRingtoneViewHolder.VIEW_TYPE_ADD_NEW;
import static com.android.deskclock.ringtone.HeaderViewHolder.VIEW_TYPE_ITEM_HEADER;
import static com.android.deskclock.ringtone.RingtoneViewHolder.VIEW_TYPE_CUSTOM_SOUND;
import static com.android.deskclock.ringtone.RingtoneViewHolder.VIEW_TYPE_SYSTEM_SOUND;

/**
 * This activity presents a set of ringtones from which the user may select one. The set includes:
 * <ul>
 *     <li>system ringtones from the Android framework</li>
 *     <li>a ringtone representing pure silence</li>
 *     <li>a ringtone representing a default ringtone</li>
 *     <li>user-selected audio files available as ringtones</li>
 * </ul>
 */
public class RingtonePickerActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<List<ItemAdapter.ItemHolder<Uri>>> {

    /** Key to an extra that defines resource id to the title of this activity. */
    private static final String EXTRA_TITLE = "extra_title";

    /** Key to an extra that identifies the alarm to which the selected ringtone is attached. */
    private static final String EXTRA_ALARM_ID = "extra_alarm_id";

    /** Key to an extra that identifies the selected ringtone. */
    private static final String EXTRA_RINGTONE_URI = "extra_ringtone_uri";

    /** Key to an extra that defines the uri representing the default ringtone. */
    private static final String EXTRA_DEFAULT_RINGTONE_URI = "extra_default_ringtone_uri";

    /** Key to an extra that defines the name of the default ringtone. */
    private static final String EXTRA_DEFAULT_RINGTONE_NAME = "extra_default_ringtone_name";

    /** Key to an instance state value indicating if the selected ringtone is currently playing. */
    private static final String STATE_KEY_PLAYING = "extra_is_playing";

    /** The controller that shows the drop shadow when content is not scrolled to the top. */
    private DropShadowController mDropShadowController;

    /** Generates the items in the activity context menu. */
    private OptionsMenuManager mOptionsMenuManager;

    /** Displays a set of selectable ringtones. */
    private RecyclerView mRecyclerView;

    /** Stores the set of ItemHolders that wrap the selectable ringtones. */
    private ItemAdapter<ItemAdapter.ItemHolder<Uri>> mRingtoneAdapter;

    /** The title of the default ringtone. */
    private String mDefaultRingtoneTitle;

    /** The uri of the default ringtone. */
    private Uri mDefaultRingtoneUri;

    /** The uri of the ringtone to select after data is loaded. */
    private Uri mSelectedRingtoneUri;

    /** {@code true} indicates the {@link #mSelectedRingtoneUri} must be played after data load. */
    private boolean mIsPlaying;

    /** Identifies the alarm to receive the selected ringtone; -1 indicates there is no alarm. */
    private long mAlarmId;

    /** The location of the custom ringtone to be removed. */
    private int mIndexOfRingtoneToRemove = RecyclerView.NO_POSITION;

    /**
     * @return an intent that launches the ringtone picker to edit the ringtone of the given
     *      {@code alarm}
     */
    public static Intent createAlarmRingtonePickerIntent(Context context, Alarm alarm) {
        return new Intent(context, RingtonePickerActivity.class)
                .putExtra(EXTRA_TITLE, R.string.alarm_sound)
                .putExtra(EXTRA_ALARM_ID, alarm.id)
                .putExtra(EXTRA_RINGTONE_URI, alarm.alert)
                .putExtra(EXTRA_DEFAULT_RINGTONE_URI, RingtoneManager.getDefaultUri(TYPE_ALARM))
                .putExtra(EXTRA_DEFAULT_RINGTONE_NAME, R.string.default_alarm_ringtone_title);
    }

    /**
     * @return an intent that launches the ringtone picker to edit the ringtone of all timers
     */
    public static Intent createTimerRingtonePickerIntent(Context context) {
        final DataModel dataModel = DataModel.getDataModel();
        return new Intent(context, RingtonePickerActivity.class)
                .putExtra(EXTRA_TITLE, R.string.timer_sound)
                .putExtra(EXTRA_RINGTONE_URI, dataModel.getTimerRingtoneUri())
                .putExtra(EXTRA_DEFAULT_RINGTONE_URI, dataModel.getDefaultTimerRingtoneUri())
                .putExtra(EXTRA_DEFAULT_RINGTONE_NAME, R.string.default_timer_ringtone_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ringtone_picker);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        mOptionsMenuManager = new OptionsMenuManager();
        mOptionsMenuManager.addMenuItemController(new NavUpMenuItemController(this))
                .addMenuItemController(MenuItemControllerFactory.getInstance()
                        .buildMenuItemControllers(this));

        final Context context = getApplicationContext();
        final Intent intent = getIntent();

        if (savedInstanceState != null) {
            mIsPlaying = savedInstanceState.getBoolean(STATE_KEY_PLAYING);
            mSelectedRingtoneUri = savedInstanceState.getParcelable(EXTRA_RINGTONE_URI);
        }

        if (mSelectedRingtoneUri == null) {
            mSelectedRingtoneUri = intent.getParcelableExtra(EXTRA_RINGTONE_URI);
        }

        mAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1);
        mDefaultRingtoneUri = intent.getParcelableExtra(EXTRA_DEFAULT_RINGTONE_URI);
        final int defaultRingtoneTitleId = intent.getIntExtra(EXTRA_DEFAULT_RINGTONE_NAME, 0);
        mDefaultRingtoneTitle = context.getString(defaultRingtoneTitleId);

        final LayoutInflater inflater = getLayoutInflater();
        final OnItemClickedListener listener = new ItemClickWatcher();
        final Factory ringtoneFactory = new RingtoneViewHolder.Factory(inflater);
        final Factory headerFactory = new HeaderViewHolder.Factory(inflater);
        final Factory addNewFactory = new AddCustomRingtoneViewHolder.Factory(inflater);
        mRingtoneAdapter = new ItemAdapter<>();
        mRingtoneAdapter.withViewTypes(headerFactory, null, VIEW_TYPE_ITEM_HEADER)
                .withViewTypes(addNewFactory, listener, VIEW_TYPE_ADD_NEW)
                .withViewTypes(ringtoneFactory, listener, VIEW_TYPE_SYSTEM_SOUND)
                .withViewTypes(ringtoneFactory, listener, VIEW_TYPE_CUSTOM_SOUND);

        mRecyclerView = (RecyclerView) findViewById(R.id.ringtone_content);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(mRingtoneAdapter);
        mRecyclerView.setItemAnimator(null);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mIndexOfRingtoneToRemove != RecyclerView.NO_POSITION) {
                    closeContextMenu();
                }
            }
        });

        final int titleResourceId = intent.getIntExtra(EXTRA_TITLE, 0);
        setTitle(context.getString(titleResourceId));

        getLoaderManager().initLoader(0 /* id */, null /* args */, this /* callback */);

        registerForContextMenu(mRecyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final View dropShadow = findViewById(R.id.drop_shadow);
        mDropShadowController = new DropShadowController(dropShadow, mRecyclerView);
    }

    @Override
    protected void onPause() {
        mDropShadowController.stop();
        mDropShadowController = null;

        if (mSelectedRingtoneUri != null) {
            if (mAlarmId != -1) {
                final Context context = getApplicationContext();
                final ContentResolver cr = getContentResolver();

                // Start a background task to fetch the alarm whose ringtone must be updated.
                new AsyncTask<Void, Void, Alarm>() {
                    @Override
                    protected Alarm doInBackground(Void... parameters) {
                        final Alarm alarm = Alarm.getAlarm(cr, mAlarmId);
                        if (alarm != null) {
                            alarm.alert = mSelectedRingtoneUri;
                        }
                        return alarm;
                    }

                    @Override
                    protected void onPostExecute(Alarm alarm) {
                        // Update the default ringtone for future new alarms.
                        DataModel.getDataModel().setDefaultAlarmRingtoneUri(alarm.alert);

                        // Start a second background task to persist the updated alarm.
                        new AlarmUpdateHandler(context, null, null)
                                .asyncUpdateAlarm(alarm, false, true);
                    }
                }.execute();
            } else {
                DataModel.getDataModel().setTimerRingtoneUri(mSelectedRingtoneUri);
            }
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations()) {
            stopPlayingRingtone(getSelectedRingtoneHolder(), false);
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_KEY_PLAYING, mIsPlaying);
        outState.putParcelable(EXTRA_RINGTONE_URI, mSelectedRingtoneUri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mOptionsMenuManager.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<ItemAdapter.ItemHolder<Uri>>> onCreateLoader(int id, Bundle args) {
        return new RingtoneLoader(getApplicationContext(), mDefaultRingtoneUri,
                mDefaultRingtoneTitle);
    }

    @Override
    public void onLoadFinished(Loader<List<ItemAdapter.ItemHolder<Uri>>> loader,
            List<ItemAdapter.ItemHolder<Uri>> itemHolders) {
        // Update the adapter with fresh data.
        mRingtoneAdapter.setItems(itemHolders);

        // Attempt to select the requested ringtone.
        final RingtoneHolder toSelect = getRingtoneHolder(mSelectedRingtoneUri);
        if (toSelect != null) {
            toSelect.setSelected(true);
            mSelectedRingtoneUri = toSelect.getUri();
            toSelect.notifyItemChanged();

            // Start playing the ringtone if indicated.
            if (mIsPlaying) {
                startPlayingRingtone(toSelect);
            }
        } else {
            // Clear the selection since it does not exist in the data.
            RingtonePreviewKlaxon.stop(this);
            mSelectedRingtoneUri = null;
            mIsPlaying = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ItemAdapter.ItemHolder<Uri>>> loader) {}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        final Uri uri = data == null ? null : data.getData();
        if (uri == null) {
            return;
        }

        // Bail if the permission to read (playback) the audio at the uri was not granted.
        final int flags = data.getFlags() & FLAG_GRANT_READ_URI_PERMISSION;
        if (flags != FLAG_GRANT_READ_URI_PERMISSION) {
            return;
        }

        // Start a task to fetch the display name of the audio content and add the custom ringtone.
        new AddCustomRingtoneTask(uri).execute();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Find the ringtone to be removed.
        final List<ItemAdapter.ItemHolder<Uri>> items = mRingtoneAdapter.getItems();
        final RingtoneHolder toRemove = (RingtoneHolder) items.get(mIndexOfRingtoneToRemove);
        mIndexOfRingtoneToRemove = RecyclerView.NO_POSITION;

        // Launch the confirmation dialog.
        final FragmentManager manager = getFragmentManager();
        final boolean hasPermissions = toRemove.hasPermissions();
        ConfirmRemoveCustomRingtoneDialogFragment.show(manager, toRemove.getUri(), hasPermissions);
        return true;
    }

    private RingtoneHolder getRingtoneHolder(Uri uri) {
        for (ItemAdapter.ItemHolder<Uri> itemHolder : mRingtoneAdapter.getItems()) {
            if (itemHolder instanceof RingtoneHolder) {
                final RingtoneHolder ringtoneHolder = (RingtoneHolder) itemHolder;
                if (ringtoneHolder.getUri().equals(uri)) {
                    return ringtoneHolder;
                }
            }
        }

        return null;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    RingtoneHolder getSelectedRingtoneHolder() {
        return getRingtoneHolder(mSelectedRingtoneUri);
    }

    /**
     * The given {@code ringtone} will be selected as a side-effect of playing the ringtone.
     *
     * @param ringtone the ringtone to be played
     */
    private void startPlayingRingtone(RingtoneHolder ringtone) {
        if (!ringtone.isPlaying() && !ringtone.isSilent()) {
            RingtonePreviewKlaxon.start(getApplicationContext(), ringtone.getUri());
            ringtone.setPlaying(true);
            mIsPlaying = true;
        }
        if (!ringtone.isSelected()) {
            ringtone.setSelected(true);
            mSelectedRingtoneUri = ringtone.getUri();
        }
        ringtone.notifyItemChanged();
    }

    /**
     * @param ringtone the ringtone to stop playing
     * @param deselect {@code true} indicates the ringtone should also be deselected;
     *      {@code false} indicates its selection state should remain unchanged
     */
    private void stopPlayingRingtone(RingtoneHolder ringtone, boolean deselect) {
        if (ringtone == null) {
            return;
        }

        if (ringtone.isPlaying()) {
            RingtonePreviewKlaxon.stop(this);
            ringtone.setPlaying(false);
            mIsPlaying = false;
        }
        if (deselect && ringtone.isSelected()) {
            ringtone.setSelected(false);
            mSelectedRingtoneUri = null;
        }
        ringtone.notifyItemChanged();
    }

    /**
     * Proceeds with removing the custom ringtone with the given uri.
     *
     * @param toRemove identifies the custom ringtone to be removed
     */
    private void removeCustomRingtone(Uri toRemove) {
        new RemoveCustomRingtoneTask(toRemove).execute();
    }

    /**
     * This DialogFragment informs the user of the side-effects of removing a custom ringtone while
     * it is in use by alarms and/or timers and prompts them to confirm the removal.
     */
    public static class ConfirmRemoveCustomRingtoneDialogFragment extends DialogFragment {

        private static final String ARG_RINGTONE_URI_TO_REMOVE = "arg_ringtone_uri_to_remove";
        private static final String ARG_RINGTONE_HAS_PERMISSIONS = "arg_ringtone_has_permissions";

        static void show(FragmentManager manager, Uri toRemove, boolean hasPermissions) {
            if (manager.isDestroyed()) {
                return;
            }

            final Bundle args = new Bundle();
            args.putParcelable(ARG_RINGTONE_URI_TO_REMOVE, toRemove);
            args.putBoolean(ARG_RINGTONE_HAS_PERMISSIONS, hasPermissions);

            final DialogFragment fragment = new ConfirmRemoveCustomRingtoneDialogFragment();
            fragment.setArguments(args);
            fragment.setCancelable(hasPermissions);
            fragment.show(manager, "confirm_ringtone_remove");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle arguments = getArguments();
            final Uri toRemove = arguments.getParcelable(ARG_RINGTONE_URI_TO_REMOVE);

            final DialogInterface.OnClickListener okListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((RingtonePickerActivity) getActivity()).removeCustomRingtone(toRemove);
                        }
                    };

            if (arguments.getBoolean(ARG_RINGTONE_HAS_PERMISSIONS)) {
                return new AlertDialog.Builder(getActivity())
                        .setPositiveButton(R.string.remove_sound, okListener)
                        .setNegativeButton(android.R.string.cancel, null /* listener */)
                        .setMessage(R.string.confirm_remove_custom_ringtone)
                        .create();
            } else {
                return new AlertDialog.Builder(getActivity())
                        .setPositiveButton(R.string.remove_sound, okListener)
                        .setMessage(R.string.custom_ringtone_lost_permissions)
                        .create();
            }
        }
    }

    /**
     * This click handler alters selection and playback of ringtones. It also launches the system
     * file chooser to search for openable audio files that may serve as ringtones.
     */
    private class ItemClickWatcher implements OnItemClickedListener {
        @Override
        public void onItemClicked(ItemAdapter.ItemViewHolder<?> viewHolder, int id) {
            switch (id) {
                case AddCustomRingtoneViewHolder.CLICK_ADD_NEW:
                    stopPlayingRingtone(getSelectedRingtoneHolder(), false);
                    startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("audio/*"), 0);
                    break;

                case RingtoneViewHolder.CLICK_NORMAL:
                    final RingtoneHolder oldSelection = getSelectedRingtoneHolder();
                    final RingtoneHolder newSelection = (RingtoneHolder) viewHolder.getItemHolder();

                    // Tapping the existing selection toggles playback of the ringtone.
                    if (oldSelection == newSelection) {
                        if (newSelection.isPlaying()) {
                            stopPlayingRingtone(newSelection, false);
                        } else {
                            startPlayingRingtone(newSelection);
                        }
                    } else {
                        // Tapping a new selection changes the selection and playback.
                        stopPlayingRingtone(oldSelection, true);
                        startPlayingRingtone(newSelection);
                    }
                    break;

                case RingtoneViewHolder.CLICK_LONG_PRESS:
                    mIndexOfRingtoneToRemove = viewHolder.getAdapterPosition();
                    break;

                case RingtoneViewHolder.CLICK_NO_PERMISSIONS:
                    ConfirmRemoveCustomRingtoneDialogFragment.show(getFragmentManager(),
                            ((RingtoneHolder) viewHolder.getItemHolder()).getUri(), false);
                    break;
            }
        }
    }

    /**
     * This task locates a displayable string in the background that is fit for use as the title of
     * the audio content. It adds a custom ringtone using the uri and title on the main thread.
     */
    private final class AddCustomRingtoneTask extends AsyncTask<Void, Void, String> {

        private final Uri mUri;
        private final Context mContext;

        private AddCustomRingtoneTask(Uri uri) {
            mUri = uri;
            mContext = getApplicationContext();
        }

        @Override
        protected String doInBackground(Void... voids) {
            final ContentResolver contentResolver = mContext.getContentResolver();

            // Take the long-term permission to read (playback) the audio at the uri.
            contentResolver.takePersistableUriPermission(mUri, FLAG_GRANT_READ_URI_PERMISSION);

            try (Cursor cursor = contentResolver.query(mUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    // If the file was a media file, return its title.
                    final int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    if (titleIndex != -1) {
                        return cursor.getString(titleIndex);
                    }

                    // If the file was a simple openable, return its display name.
                    final int displayNameIndex = cursor.getColumnIndex(DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        String title = cursor.getString(displayNameIndex);
                        final int dotIndex = title.lastIndexOf(".");
                        if (dotIndex > 0) {
                            title = title.substring(0, dotIndex);
                        }
                        return title;
                    }
                } else {
                    LogUtils.e("No ringtone for uri: %s", mUri);
                }
            } catch (Exception e) {
                LogUtils.e("Unable to locate title for custom ringtone: " + mUri, e);
            }

            return mContext.getString(R.string.unknown_ringtone_title);
        }

        @Override
        protected void onPostExecute(String title) {
            // Add the new custom ringtone to the data model.
            DataModel.getDataModel().addCustomRingtone(mUri, title);

            // When the loader completes, it must play the new ringtone.
            mSelectedRingtoneUri = mUri;
            mIsPlaying = true;

            // Reload the data to reflect the change in the UI.
            getLoaderManager().restartLoader(0 /* id */, null /* args */,
                    RingtonePickerActivity.this /* callback */);
        }
    }

    /**
     * Removes a custom ringtone with the given uri. Taking this action has side-effects because
     * all alarms that use the custom ringtone are reassigned to the Android system default alarm
     * ringtone. If the application's default alarm ringtone is being removed, it is reset to the
     * Android system default alarm ringtone. If the application's timer ringtone is being removed,
     * it is reset to the application's default timer ringtone.
     */
    private final class RemoveCustomRingtoneTask extends AsyncTask<Void, Void, Void> {

        private final Uri mRemoveUri;
        private Uri mSystemDefaultRingtoneUri;

        private RemoveCustomRingtoneTask(Uri removeUri) {
            mRemoveUri = removeUri;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mSystemDefaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            // Update all alarms that use the custom ringtone to use the system default.
            final ContentResolver cr = getContentResolver();
            final List<Alarm> alarms = Alarm.getAlarms(cr, null);
            for (Alarm alarm : alarms) {
                if (mRemoveUri.equals(alarm.alert)) {
                    alarm.alert = mSystemDefaultRingtoneUri;
                    // Start a second background task to persist the updated alarm.
                    new AlarmUpdateHandler(RingtonePickerActivity.this, null, null)
                            .asyncUpdateAlarm(alarm, false, true);
                }
            }

            try {
                // Release the permission to read (playback) the audio at the uri.
                cr.releasePersistableUriPermission(mRemoveUri, FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignore) {
                // If the file was already deleted from the file system, a SecurityException is
                // thrown indicating this app did not hold the read permission being released.
                LogUtils.w("SecurityException while releasing read permission for " + mRemoveUri);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            // Reset the default alarm ringtone if it was just removed.
            if (mRemoveUri.equals(DataModel.getDataModel().getDefaultAlarmRingtoneUri())) {
                DataModel.getDataModel().setDefaultAlarmRingtoneUri(mSystemDefaultRingtoneUri);
            }

            // Reset the timer ringtone if it was just removed.
            if (mRemoveUri.equals(DataModel.getDataModel().getTimerRingtoneUri())) {
                final Uri timerRingtoneUri = DataModel.getDataModel().getDefaultTimerRingtoneUri();
                DataModel.getDataModel().setTimerRingtoneUri(timerRingtoneUri);
            }

            // Remove the corresponding custom ringtone.
            DataModel.getDataModel().removeCustomRingtone(mRemoveUri);

            // Find the ringtone to be removed from the adapter.
            final RingtoneHolder toRemove = getRingtoneHolder(mRemoveUri);
            if (toRemove == null) {
                return;
            }

            // If the ringtone to remove is also the selected ringtone, adjust the selection.
            if (toRemove.isSelected()) {
                stopPlayingRingtone(toRemove, false);
                final RingtoneHolder defaultRingtone = getRingtoneHolder(mDefaultRingtoneUri);
                if (defaultRingtone != null) {
                    defaultRingtone.setSelected(true);
                    mSelectedRingtoneUri = defaultRingtone.getUri();
                    defaultRingtone.notifyItemChanged();
                }
            }

            // Remove the ringtone from the adapter.
            mRingtoneAdapter.removeItem(toRemove);
        }
    }
}

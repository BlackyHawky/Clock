/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.media.RingtoneManager.TYPE_ALARM;
import static android.provider.OpenableColumns.DISPLAY_NAME;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.ItemAdapter.ItemViewHolder.Factory;
import static com.best.deskclock.ringtone.AddCustomRingtoneViewHolder.VIEW_TYPE_ADD_NEW;
import static com.best.deskclock.ringtone.HeaderViewHolder.VIEW_TYPE_ITEM_HEADER;
import static com.best.deskclock.ringtone.RingtoneViewHolder.VIEW_TYPE_CUSTOM_SOUND;
import static com.best.deskclock.ringtone.RingtoneViewHolder.VIEW_TYPE_SYSTEM_SOUND;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.ItemAdapter.OnItemClickedListener;
import com.best.deskclock.ItemAdapter.OnItemLongClickedListener;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.data.CustomRingtone;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This activity presents a set of ringtones from which the user may select one. The set includes:
 * <ul>
 *     <li>system ringtones from the Android framework</li>
 *     <li>a ringtone representing pure silence</li>
 *     <li>a ringtone representing a default ringtone</li>
 *     <li>user-selected audio files available as ringtones</li>
 * </ul>
 */
public class RingtonePickerActivity extends CollapsingToolbarBaseActivity
        implements LoaderManager.LoaderCallbacks<List<ItemAdapter.ItemHolder<Uri>>> {

    /**
     * Key to an extra that defines resource id to the title of this activity.
     */
    private static final String EXTRA_TITLE = "extra_title";

    /**
     * Key to an extra that identifies the alarm to which the selected ringtone is attached.
     */
    private static final String EXTRA_ALARM_ID = "extra_alarm_id";

    /**
     * Key to an extra that identifies the selected ringtone.
     */
    private static final String EXTRA_RINGTONE_URI = "extra_ringtone_uri";

    /**
     * Key to an extra that defines the uri representing the default ringtone.
     */
    private static final String EXTRA_DEFAULT_RINGTONE_URI = "extra_default_ringtone_uri";

    /**
     * Key to an extra that defines the name of the default ringtone.
     */
    private static final String EXTRA_DEFAULT_RINGTONE_NAME = "extra_default_ringtone_name";

    /**
     * Key to an instance state value indicating if the selected ringtone is currently playing.
     */
    private static final String STATE_KEY_PLAYING = "extra_is_playing";

    /**
     * Stores the set of ItemHolders that wrap the selectable ringtones.
     */
    private ItemAdapter<ItemAdapter.ItemHolder<Uri>> mRingtoneAdapter;

    /**
     * Displays a set of selectable ringtones.
     */
    RecyclerView mRingtoneContent;

    /**
     * The title of the default ringtone.
     */
    private String mDefaultRingtoneTitle;

    /**
     * The uri of the default ringtone.
     */
    private Uri mDefaultRingtoneUri;

    /**
     * The uri of the ringtone to select after data is loaded.
     */
    private Uri mSelectedRingtoneUri;

    /**
     * {@code true} indicates the {@link #mSelectedRingtoneUri} must be played after data load.
     */
    private boolean mIsPlaying;

    /**
     * Identifies the alarm to receive the selected ringtone; -1 indicates there is no alarm.
     */
    private long mAlarmId;

    /**
     * Identifies the title of the ringtone picker activity that appears in the action bar.
     */
    private int mTitleResourceId;

    private FragmentManager mFragmentManager;

    private SharedPreferences mPrefs;

    /**
     * Callback for getting the result from Activity
     */
    private final ActivityResultLauncher<Intent> getActivityOnClick = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), (result) -> {
                if (result.getResultCode() != RESULT_OK ) {
                    return;
                }

                Intent intent = result.getData();
                final Uri uri = intent == null ? null : intent.getData();
                if (uri == null) {
                    return;
                }

                // Bail if the permission to read (playback) the audio at the uri was not granted.
                final int flags = intent.getFlags() & FLAG_GRANT_READ_URI_PERMISSION;
                if (flags != FLAG_GRANT_READ_URI_PERMISSION) {
                    return;
                }

                // Start a task to fetch the display name of the audio content and add the custom ringtone.
                addCustomRingtoneAsync(uri);
            });

    /**
     * Callback for getting the result from Activity
     */
    private final ActivityResultLauncher<Intent> getActivityOnLongClick = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), (result) -> {
                if (result.getResultCode() != RESULT_OK ) {
                    return;
                }

                Intent intent = result.getData();
                final Uri treeUri = intent == null ? null : intent.getData();
                if (treeUri == null) {
                    return;
                }

                // Take persistent permission
                getContentResolver().takePersistableUriPermission(
                        treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                // Start a task to fetch the display name of the audio content and add the custom ringtone.
                addCustomRingtonesFromFolderAsync(treeUri);
            });

    /**
     * @return an intent that launches the ringtone picker to edit the ringtone of the given
     * {@code alarm}
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

    /**
     * @return an intent that launches the ringtone picker to edit the ringtone of all alarms in the settings
     */
    public static Intent createAlarmRingtonePickerIntentForSettings(Context context) {
        final DataModel dataModel = DataModel.getDataModel();
        return new Intent(context, RingtonePickerActivity.class)
                .putExtra(EXTRA_TITLE, R.string.default_alarm_ringtone_title)
                .putExtra(EXTRA_RINGTONE_URI, dataModel.getAlarmRingtoneUriFromSettings())
                .putExtra(EXTRA_DEFAULT_RINGTONE_URI, dataModel.getDefaultAlarmRingtoneUriFromSettings())
                .putExtra(EXTRA_DEFAULT_RINGTONE_NAME, R.string.default_alarm_ringtone_title);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.alarm_sound);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getDefaultSharedPreferences(this);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.ringtone_picker);

        setVolumeControlStream(AudioManager.STREAM_ALARM);

        final Context context = getApplicationContext();
        final Intent intent = getIntent();

        if (savedInstanceState != null) {
            mIsPlaying = savedInstanceState.getBoolean(STATE_KEY_PLAYING);
            mSelectedRingtoneUri = SdkUtils.isAtLeastAndroid13()
                    ? savedInstanceState.getParcelable(EXTRA_RINGTONE_URI, Uri.class)
                    : savedInstanceState.getParcelable(EXTRA_RINGTONE_URI);
        }

        if (mSelectedRingtoneUri == null) {
            mSelectedRingtoneUri = SdkUtils.isAtLeastAndroid13()
                    ? intent.getParcelableExtra(EXTRA_RINGTONE_URI, Uri.class)
                    : intent.getParcelableExtra(EXTRA_RINGTONE_URI);
        }

        mAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1);

        mDefaultRingtoneUri = SdkUtils.isAtLeastAndroid13()
                ? intent.getParcelableExtra(EXTRA_DEFAULT_RINGTONE_URI, Uri.class)
                : intent.getParcelableExtra(EXTRA_DEFAULT_RINGTONE_URI);
        final int defaultRingtoneTitleId = intent.getIntExtra(EXTRA_DEFAULT_RINGTONE_NAME, 0);
        final Context localizedContext = Utils.getLocalizedContext(context);
        mDefaultRingtoneTitle = localizedContext.getString(defaultRingtoneTitleId);

        final LayoutInflater inflater = getLayoutInflater();
        final OnItemClickedListener listener = new ItemClickWatcher();
        final OnItemLongClickedListener onLongClickedListener = new ItemLongClickWatcher();
        final Factory ringtoneFactory = new RingtoneViewHolder.Factory(inflater);
        final Factory headerFactory = new HeaderViewHolder.Factory(inflater);
        final Factory addNewFactory = new AddCustomRingtoneViewHolder.Factory(inflater);

        mRingtoneAdapter = new ItemAdapter<>();
        mRingtoneAdapter.withViewTypes(headerFactory, null, null, VIEW_TYPE_ITEM_HEADER)
                .withViewTypes(addNewFactory, listener, onLongClickedListener, VIEW_TYPE_ADD_NEW)
                .withViewTypes(ringtoneFactory, listener, null, VIEW_TYPE_SYSTEM_SOUND)
                .withViewTypes(ringtoneFactory, listener, null, VIEW_TYPE_CUSTOM_SOUND);

        mRingtoneContent = findViewById(R.id.ringtone_content);
        mRingtoneContent.setLayoutManager(new LinearLayoutManager(context));
        mRingtoneContent.setAdapter(mRingtoneAdapter);
        mRingtoneContent.setItemAnimator(null);

        mTitleResourceId = intent.getIntExtra(EXTRA_TITLE, 0);
        setTitle(context.getString(mTitleResourceId));

        mFragmentManager = getSupportFragmentManager();

        LoaderManager.getInstance(this).initLoader(0, null, this);

        applyWindowInsets();
    }

    @Override
    protected void onPause() {
        if (mSelectedRingtoneUri != null) {
            if (mTitleResourceId == R.string.default_alarm_ringtone_title) {
                DataModel.getDataModel().setAlarmRingtoneUriFromSettings(mSelectedRingtoneUri);
            } else {
                if (mAlarmId != -1) {
                    final Context context = getApplicationContext();
                    final ContentResolver cr = getContentResolver();

                    // Start a background task to fetch the alarm whose ringtone must be updated.
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        final Alarm alarm = Alarm.getAlarm(cr, mAlarmId);
                        if (alarm != null) {
                            alarm.alert = mSelectedRingtoneUri;

                            handler.post(() -> {
                                DataModel.getDataModel().setSelectedAlarmRingtoneUri(alarm.alert);

                                // Start a second background task to persist the updated alarm.
                                new AlarmUpdateHandler(context, null, null)
                                        .asyncUpdateAlarm(alarm, false, true);
                            });
                        }
                    });
                } else {
                    DataModel.getDataModel().setTimerRingtoneUri(mSelectedRingtoneUri);
                }
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_KEY_PLAYING, mIsPlaying);
        outState.putParcelable(EXTRA_RINGTONE_URI, mSelectedRingtoneUri);
    }

    @NonNull
    @Override
    public Loader<List<ItemAdapter.ItemHolder<Uri>>> onCreateLoader(int id, Bundle args) {
        return new RingtoneLoader(getApplicationContext(), mDefaultRingtoneUri, mDefaultRingtoneTitle);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<ItemAdapter.ItemHolder<Uri>>> loader,
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
            RingtonePreviewKlaxon.stop(this, mPrefs);
            mSelectedRingtoneUri = null;
            mIsPlaying = false;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<ItemAdapter.ItemHolder<Uri>>> loader) {
    }

    /**
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mCoordinatorLayout, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() |
                    WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);

            int paddingTop = ThemeUtils.convertDpToPixels(14, this);
            int paddingBottom = ThemeUtils.convertDpToPixels(10, this);
            mRingtoneContent.setPadding(0, paddingTop, 0, bars.bottom + paddingBottom);
        });
    }

    private void onItemRemovedClicked(int indexOfRingtoneToRemove) {
        // Find the ringtone to be removed.
        final List<ItemAdapter.ItemHolder<Uri>> items = mRingtoneAdapter.getItems();
        final RingtoneHolder toRemove = (RingtoneHolder) items.get(indexOfRingtoneToRemove);

        // Launch the confirmation dialog.
        ConfirmRemoveCustomRingtoneDialogFragment.show(mFragmentManager, toRemove.getUri());
    }

    private RingtoneHolder getRingtoneHolder(Uri uri) {
        for (ItemAdapter.ItemHolder<Uri> itemHolder : mRingtoneAdapter.getItems()) {
            if (itemHolder instanceof final RingtoneHolder ringtoneHolder) {
                if (ringtoneHolder.getUri().equals(uri)) {
                    return ringtoneHolder;
                }
            }
        }

        return null;
    }

    @VisibleForTesting()
    RingtoneHolder getSelectedRingtoneHolder() {
        return getRingtoneHolder(mSelectedRingtoneUri);
    }

    /**
     * The given {@code ringtone} will be selected as a side-effect of playing the ringtone.
     *
     * @param ringtone the ringtone to be played
     */
    private void startPlayingRingtone(RingtoneHolder ringtone) {
        Uri ringtoneUri = ringtone.getUri();
        if (RingtoneUtils.isRandomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomRingtoneUri();
        } else if (RingtoneUtils.isRandomCustomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomCustomRingtoneUri();
        }

        if (!ringtone.isPlaying() && !ringtone.isSilent()) {
            if (RingtoneUtils.isRingtoneUriReadable(this, ringtoneUri)) {
                RingtonePreviewKlaxon.start(getApplicationContext(), mPrefs, ringtoneUri);
                ringtone.setPlaying(true);
                mIsPlaying = true;
            } else {
                ConfirmRemoveCustomRingtoneDialogFragment.show(mFragmentManager, ringtoneUri);
            }
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
     *                 {@code false} indicates its selection state should remain unchanged
     */
    private void stopPlayingRingtone(RingtoneHolder ringtone, boolean deselect) {
        if (ringtone == null) {
            return;
        }

        if (ringtone.isPlaying()) {
            RingtonePreviewKlaxon.stop(this, mPrefs);
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
     * This DialogFragment informs the user of the side-effects of removing a custom ringtone while
     * it is in use by alarms and/or timers and prompts them to confirm the removal.
     */
    public static class ConfirmRemoveCustomRingtoneDialogFragment extends DialogFragment {

        private static final String ARG_RINGTONE_URI_TO_REMOVE = "arg_ringtone_uri_to_remove";

        static void show(FragmentManager manager, Uri toRemove) {
            if (manager.isDestroyed()) {
                return;
            }

            final Bundle args = new Bundle();
            args.putParcelable(ARG_RINGTONE_URI_TO_REMOVE, toRemove);

            final DialogFragment fragment = new ConfirmRemoveCustomRingtoneDialogFragment();
            fragment.setArguments(args);
            fragment.show(manager, "confirm_ringtone_remove");
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle arguments = requireArguments();
            final Uri toRemove = SdkUtils.isAtLeastAndroid13()
                    ? arguments.getParcelable(ARG_RINGTONE_URI_TO_REMOVE, Uri.class)
                    : arguments.getParcelable(ARG_RINGTONE_URI_TO_REMOVE);

            final DialogInterface.OnClickListener okListener = (dialog, which) ->
                    ((RingtonePickerActivity) requireActivity()).removeCustomRingtoneAsync(toRemove);

            final Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error);
            if (drawable != null) {
                drawable.setTint(MaterialColors.getColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
            }

            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                    .setIcon(drawable)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.remove_sound, okListener)
                    .setNegativeButton(android.R.string.cancel, null);

            if (RingtoneUtils.isRingtoneUriReadable(requireContext(), toRemove)) {
                dialogBuilder.setMessage(R.string.confirm_remove_custom_ringtone);
            } else {
                dialogBuilder.setMessage(R.string.custom_ringtone_lost_permissions);
            }

            return dialogBuilder.create();
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
                case AddCustomRingtoneViewHolder.CLICK_ADD_NEW -> {
                    stopPlayingRingtone(getSelectedRingtoneHolder(), false);
                    getActivityOnClick.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("audio/*"));
                }
                case RingtoneViewHolder.CLICK_NORMAL -> {
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
                }
                case RingtoneViewHolder.CLICK_REMOVE -> onItemRemovedClicked(viewHolder.getBindingAdapterPosition());
            }
        }
    }

    /**
     * This long click handler alters selection and playback of ringtones. It also launches the system
     * folder chooser to search for openable audio files that may serve as ringtones.
     */
    private class ItemLongClickWatcher implements OnItemLongClickedListener {

        @Override
        public void onItemLongClicked(ItemAdapter.ItemViewHolder<?> viewHolder, int id) {
            if (id == AddCustomRingtoneViewHolder.CLICK_ADD_FOLDER) {
                stopPlayingRingtone(getSelectedRingtoneHolder(), false);
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                getActivityOnLongClick.launch(intent);
            }
        }
    }

    /**
     * This task locates a displayable string in the background that is fit for use as the title of
     * the audio content. It adds a custom ringtone using the uri and title on the main thread.
     */
    private void addCustomRingtoneAsync(Uri uri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            final Context context = getApplicationContext();
            final ContentResolver contentResolver = context.getContentResolver();
            String name = null;

            // Take the long-term permission to read (playback) the audio at the uri.
            contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION);

            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    // If the file was a media file, return its title.
                    final int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    if (titleIndex != -1) {
                        name = cursor.getString(titleIndex);
                    } else {
                        // If the file was a simple openable, return its display name.
                        final int displayNameIndex = cursor.getColumnIndex(DISPLAY_NAME);
                        if (displayNameIndex != -1) {
                            String displayName = cursor.getString(displayNameIndex);
                            final int dotIndex = displayName.lastIndexOf(".");
                            if (dotIndex > 0) {
                                displayName = displayName.substring(0, dotIndex);
                            }
                            name = displayName;
                        }
                    }
                } else {
                    LogUtils.e("No ringtone for uri: %s", uri);
                }
            } catch (Exception e) {
                LogUtils.e("Unable to locate title for custom ringtone: " + uri, e);
            }

            if (name == null) {
                name = context.getString(R.string.unknown_ringtone_title);
            }

            final String title = name;
            handler.post(() -> {
                // When the loader completes, it must play the new ringtone.
                mSelectedRingtoneUri = DataModel.getDataModel().customRingtoneToAdd(uri, title);
                mIsPlaying = true;

                // Reload the data to reflect the change in the UI.
                LoaderManager.getInstance(this).restartLoader(0, null, RingtonePickerActivity.this);
            });
        });
    }

    /**
     * This task locates a displayable string in the background that is fit for use as the title of
     * the audio content. It adds a custom ringtone using the uri and title on the main thread.
     */
    private void addCustomRingtonesFromFolderAsync(Uri treeUri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            // Convert the treeUri to a DocumentFile to browse the folder
            DocumentFile directory = DocumentFile.fromTreeUri(this, treeUri);
            if (directory == null || !directory.isDirectory()) {
                LogUtils.e("Invalid directory selected: %s", treeUri);
                return;
            }

            for (DocumentFile file : directory.listFiles()) {
                if (file.isFile() && file.getType() != null && file.getType().startsWith("audio/")) {
                    Uri fileUri = file.getUri();

                    String name = file.getName();
                    if (name != null && name.contains(".")) {
                        name = name.substring(0, name.lastIndexOf("."));
                    }

                    long size = file.length();

                    if (DataModel.getDataModel().isCustomRingtoneAlreadyAdded(name, size)) {
                        continue;
                    }

                    String finalName = name;

                    handler.post(() -> {
                        // Add the new custom ringtone to the data model.
                        DataModel.getDataModel().customRingtoneToAdd(fileUri, finalName);

                        // Reload the data to reflect the change in the UI.
                        LoaderManager.getInstance(this).restartLoader(0, null, RingtonePickerActivity.this);
                    });
                }
            }
        });
    }

    /**
     * Removes a custom ringtone with the given uri. Taking this action has side-effects because
     * all alarms that use the custom ringtone are reassigned to the Android system default alarm
     * ringtone. If the application's default alarm ringtone is being removed, it is reset to the
     * Android system default alarm ringtone. If the application's timer ringtone is being removed,
     * it is reset to the application's default timer ringtone.
     */
    private void removeCustomRingtoneAsync(Uri removeUri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            final Uri systemDefaultRingtoneUri =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            // Update all alarms that use the custom ringtone to use the system default.
            final ContentResolver cr = getContentResolver();
            final List<Alarm> alarms = Alarm.getAlarms(cr, null);
            for (Alarm alarm : alarms) {
                if (removeUri.equals(alarm.alert)) {
                    alarm.alert = systemDefaultRingtoneUri;
                    // Start a second background task to persist the updated alarm.
                    new AlarmUpdateHandler(RingtonePickerActivity.this, null, null)
                            .asyncUpdateAlarm(alarm, false, true);
                }
            }

            try {
                // Release the permission to read (playback) the audio at the uri.
                cr.releasePersistableUriPermission(removeUri, FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignore) {
                // If the file was already deleted from the file system, a SecurityException is
                // thrown indicating this app did not hold the read permission being released.
                LogUtils.w("SecurityException while releasing read permission for " + removeUri);
            }

            handler.post(() -> {
                // Reset the default alarm ringtone if it was just removed.
                if (removeUri.equals(DataModel.getDataModel().getAlarmRingtoneUriFromSettings())) {
                    DataModel.getDataModel().setAlarmRingtoneUriFromSettings(systemDefaultRingtoneUri);
                }

                // Reset the timer ringtone if it was just removed.
                if (removeUri.equals(DataModel.getDataModel().getTimerRingtoneUri())) {
                    final Uri timerRingtoneUri = DataModel.getDataModel().getDefaultTimerRingtoneUri();
                    DataModel.getDataModel().setTimerRingtoneUri(timerRingtoneUri);
                }

                // Remove the corresponding custom ringtone.
                DataModel.getDataModel().removeCustomRingtone(removeUri);

                // Find the ringtone to be removed from the adapter.
                final RingtoneHolder toRemove = getRingtoneHolder(removeUri);
                if (toRemove == null) {
                    return;
                }

                final List<CustomRingtone> customRingtones = DataModel.getDataModel().getCustomRingtones();
                int remainingCount = customRingtones.size();

                // If "Random Ringtone" is selected and there is only one ringtone left,
                // select that ringtone.
                // Otherwise, if the ringtone to remove is also the selected ringtone,
                // select the default system ringtone.
                if (RingtoneUtils.isRandomCustomRingtone(mSelectedRingtoneUri) && remainingCount == 1) {
                    Uri remainingUri = null;

                    for (CustomRingtone ringtone : customRingtones) {
                        if (!ringtone.getUri().equals(removeUri)) {
                            remainingUri = ringtone.getUri();
                            break;
                        }
                    }

                    if (remainingUri != null) {
                        mSelectedRingtoneUri = remainingUri;

                        RingtoneHolder remainingHolder = getRingtoneHolder(remainingUri);
                        if (remainingHolder != null) {
                            stopPlayingRingtone(toRemove, false);
                            remainingHolder.setSelected(true);
                            remainingHolder.notifyItemChanged();
                        }
                    }
                } else if (toRemove.isSelected()) {
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

                // Reload the data to reflect the change in the UI.
                LoaderManager.getInstance(this).restartLoader(0, null, RingtonePickerActivity.this);
            });
        });
    }
}

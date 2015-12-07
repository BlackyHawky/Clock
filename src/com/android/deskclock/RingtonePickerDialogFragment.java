/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.android.deskclock.data.DataModel;

import java.util.ArrayList;
import java.util.List;

/**
 * This ringtone picker offers some flexibility over the system ringtone picker. It can be themed,
 * and it allows control of the ringtones that are displayed and their labels.
 */
public class RingtonePickerDialogFragment extends DialogFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_OLD_RINGTONE_URI = "old_ringtone_uri";
    private static final String KEY_DEFAULT_RINGTONE_LABEL = "default_ringtone_label";
    private static final String KEY_DEFAULT_RINGTONE_URI = "default_ringtone_uri";
    private static final String KEY_FRAGMENT_TAG = "fragment_tag";
    private static final String KEY_SELECTED_INDEX = "selected_index";

    private int mSelectedIndex;

    public static DialogFragment newInstance(String title, String
            defaultRingtoneLabel, Uri defaultRingtoneUri, Uri oldRingtoneUri, String fragmentTag) {

        final Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putString(KEY_DEFAULT_RINGTONE_LABEL, defaultRingtoneLabel);
        args.putParcelable(KEY_DEFAULT_RINGTONE_URI, defaultRingtoneUri);
        args.putParcelable(KEY_OLD_RINGTONE_URI, oldRingtoneUri);
        args.putString(KEY_FRAGMENT_TAG, fragmentTag);
        args.putInt(KEY_SELECTED_INDEX, -1);

        final RingtonePickerDialogFragment fragment = new RingtonePickerDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final String title = bundle.getString(KEY_TITLE);
        final String defaultRingtoneLabel = bundle.getString(KEY_DEFAULT_RINGTONE_LABEL);
        final Uri defaultRingtoneUri = bundle.getParcelable(KEY_DEFAULT_RINGTONE_URI);
        final Uri oldRingtoneUri = bundle.getParcelable(KEY_OLD_RINGTONE_URI);
        final String fragmentTag = bundle.getString(KEY_FRAGMENT_TAG);
        mSelectedIndex = bundle.getInt(KEY_SELECTED_INDEX);
        final List<RingtoneItem> ringtones = new ArrayList<>(20);

        // Add option for "silent" ringtone.
        final String silentTitle = getString(R.string.silent_ringtone_title);
        final Uri silentUri = DataModel.getDataModel().getSilentRingtoneUri();
        ringtones.add(new RingtoneItem(silentTitle, silentUri));

        // Add option for default ringtone.
        if (defaultRingtoneLabel != null) {
            ringtones.add(new RingtoneItem(defaultRingtoneLabel, defaultRingtoneUri));
        }

        // Add system ringtones.
        final Context context = getActivity();
        final RingtoneManager rm = new RingtoneManager(context);
        rm.setType(RingtoneManager.TYPE_ALARM);
        final Cursor cursor = rm.getCursor();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            final String ringtoneTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            final Uri ringtoneUri = rm.getRingtoneUri(cursor.getPosition());
            ringtones.add(new RingtoneItem(ringtoneTitle, ringtoneUri));
        }

        // Extract the ringtone titles for the dialog to display.
        final CharSequence[] titles = new CharSequence[ringtones.size()];
        for (int i = 0; i < ringtones.size(); i++) {
            final RingtoneItem ringtone = ringtones.get(i);
            titles[i] = ringtone.title;
            if (mSelectedIndex < 0 && ringtone.uri.equals(oldRingtoneUri)) {
                mSelectedIndex = i;
            }
        }

        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Uri uri = ringtones.get(mSelectedIndex).uri;
                        RingtoneSelectionListener rsl = (RingtoneSelectionListener) getActivity();
                        rsl.onRingtoneSelected(uri, fragmentTag);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setSingleChoiceItems(titles, mSelectedIndex,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectRingtone(which, ringtones.get(which).uri);
                    }
                })
                .create();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations()) {
            RingtonePreviewKlaxon.stop(getActivity());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_INDEX, mSelectedIndex);
    }

    /**
     * Callback for ringtone click.
     */
    private void selectRingtone(int index, Uri ringtoneUri) {
        mSelectedIndex = index;
        final Context context = getActivity();
        RingtonePreviewKlaxon.stop(context);

        if (!DataModel.getDataModel().getSilentRingtoneUri().equals(ringtoneUri)) {
            RingtonePreviewKlaxon.start(context, ringtoneUri);
        }
    }

    private static class RingtoneItem {
        public final String title;
        public final Uri uri;

        public RingtoneItem(String title, Uri uri) {
            this.title = title;
            this.uri = uri;
        }
    }

    public interface RingtoneSelectionListener {
        /**
         * Called when the ringtone picker dialog is confirmed and dismissed.
         *
         * @param ringtoneUri the uri of the ringtone that was picked
         * @param fragmentTag the tag of the fragment that launched the dialog
         */
        void onRingtoneSelected(Uri ringtoneUri, String fragmentTag);
    }
}

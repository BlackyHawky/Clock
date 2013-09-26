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

package com.android.deskclock;

import android.app.Fragment;
import android.support.v4.widget.PopupMenuCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

public class DeskClockFragment extends Fragment {

    public void onPageChanged(int page) {
        // Do nothing here , only in derived classes
    }

    /**
     * Installs click and touch listeners on a fake overflow menu button.
     *
     * @param menuButton the fragment's fake overflow menu button
     */
    public void setupFakeOverflowMenuButton(View menuButton) {
        final PopupMenu fakeOverflow = new PopupMenu(menuButton.getContext(), menuButton) {
            @Override
            public void show() {
                getActivity().onPrepareOptionsMenu(getMenu());
                super.show();
            }
        };
        fakeOverflow.inflate(R.menu.desk_clock_menu);
        fakeOverflow.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener () {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return getActivity().onOptionsItemSelected(item);
            }
        });

        menuButton.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(fakeOverflow));
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fakeOverflow.show();
            }
        });
    }
}

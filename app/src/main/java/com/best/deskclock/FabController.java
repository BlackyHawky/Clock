// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

/**
 * Implementers of this interface are able to {@link #onUpdateFab configure the fab} and associated
 * {@link #onUpdateFabButtons left/right buttons} including setting them {@link View#INVISIBLE} if
 * they are unnecessary. Implementers also attach click handler logic to the {@link #onFabClick fab}.
 */
public interface FabController {

    /**
     * Configures the display of the fab component to match the current state of this controller.
     *
     * @param fab the fab component to be configured based on current state
     */
    void onUpdateFab(@NonNull ImageView fab);

    /**
     * Configures the display of the buttons to the left and right of the fab to match the current
     * state of this controller.
     *
     * @param left  button to the left of the fab to configure based on current state
     * @param right button to the right of the fab to configure based on current state
     */
    void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right);

    /**
     * Handles a click on the fab.
     */
    void onFabClick();

    /**
     * Handles a long click on the fab.
     */
    void onFabLongClick(@NonNull ImageView fab);

}

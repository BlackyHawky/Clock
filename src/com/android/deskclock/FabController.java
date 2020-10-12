package com.android.deskclock;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Implementers of this interface are able to {@link #onUpdateFab configure the fab} and associated
 * {@link #onUpdateFabButtons left/right buttons} including setting them {@link View#INVISIBLE} if
 * they are unnecessary. Implementers also attach click handler logic to the
 * {@link #onFabClick fab}, {@link #onLeftButtonClick left button} and
 * {@link #onRightButtonClick right button}.
 */
public interface FabController {

    /**
     * Configures the display of the fab component to match the current state of this controller.
     *
     * @param fab the fab component to be configured based on current state
     */
    void onUpdateFab(@NonNull ImageView fab);

    /**
     * Called before onUpdateFab when the fab should be animated.
     *
     * @param fab the fab component to be configured based on current state
     */
    void onMorphFab(@NonNull ImageView fab);

    /**
     * Configures the display of the buttons to the left and right of the fab to match the current
     * state of this controller.
     *
     * @param left button to the left of the fab to configure based on current state
     * @param right button to the right of the fab to configure based on current state
     */
    void onUpdateFabButtons(@NonNull Button left, @NonNull Button right);

    /**
     * Handles a click on the fab.
     *
     * @param fab the fab component on which the click occurred
     */
    void onFabClick(@NonNull ImageView fab);

    /**
     * Handles a click on the button to the left of the fab component.
     *
     * @param left the button to the left of the fab component
     */
    void onLeftButtonClick(@NonNull Button left);

    /**
     * Handles a click on the button to the right of the fab component.
     *
     * @param right the button to the right of the fab component
     */
    void onRightButtonClick(@NonNull Button right);

    /**
     *
     * @return the target visibility of the FAB component
     */
    int getFabTargetVisibility();
}

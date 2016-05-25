package com.android.deskclock;

/**
 * Implemented by containers that house the fab and its associated buttons. Also implemented by
 * containers that know how to contact the <strong>true</strong> fab container to ferry through
 * commands.
 */
public interface FabContainer {

    enum UpdateType {
        /** Signals just the fab should be "animated away", updated, and "animated back". */
        FAB_ONLY_SHRINK_AND_EXPAND,

        /** Signals the fab and buttons should be "animated away", updated, and "animated back". */
        FAB_AND_BUTTONS_SHRINK_AND_EXPAND,

        /** Signals that the fab and buttons should be updated in place with no animation. */
        FAB_AND_BUTTONS_IMMEDIATE,

        /** Signals that the fab and buttons should morph into a new state in place. **/
        FAB_AND_BUTTONS_MORPH,

        /** Disable the buttons of the fab so they do not respond to clicks. */
        DISABLE_BUTTONS,

        /** Signals that the fab should request focus. */
        FAB_REQUESTS_FOCUS
    }

    /**
     * Requests that this container update the fab and/or its buttons because their state has
     * changed. The update may be immediate or it may be animated depending on the choice of
     * {@code updateType}.
     *
     * @param updateType indicates the type of update to apply to the fab and its buttons
     */
    void updateFab(UpdateType updateType);
}
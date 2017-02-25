package com.android.deskclock;

import android.support.annotation.IntDef;

/**
 * Implemented by containers that house the fab and its associated buttons. Also implemented by
 * containers that know how to contact the <strong>true</strong> fab container to ferry through
 * commands.
 */
public interface FabContainer {

    /** Bit field for updates */

    /** Bit 0-1 */
    int FAB_ANIMATION_MASK = 0b11;
    /** Signals that the fab should be updated in place with no animation. */
    int FAB_IMMEDIATE = 0b1;
    /** Signals the fab should be "animated away", updated, and "animated back". */
    int FAB_SHRINK_AND_EXPAND = 0b10;
    /** Signals that the fab should morph into a new state in place. */
    int FAB_MORPH = 0b11;

    /** Bit 2 */
    int FAB_REQUEST_FOCUS_MASK = 0b100;
    /** Signals that the fab should request focus. */
    int FAB_REQUEST_FOCUS = 0b100;

    /** Bit 3-4 */
    int BUTTONS_ANIMATION_MASK = 0b11000;
    /** Signals that the buttons should be updated in place with no animation. */
    int BUTTONS_IMMEDIATE = 0b1000;
    /** Signals that the buttons should be "animated away", updated, and "animated back". */
    int BUTTONS_SHRINK_AND_EXPAND = 0b10000;

    /** Bit 5 */
    int BUTTONS_DISABLE_MASK = 0b100000;
    /** Disable the buttons of the fab so they do not respond to clicks. */
    int BUTTONS_DISABLE = 0b100000;

    /** Bit 6-7 */
    int FAB_AND_BUTTONS_SHRINK_EXPAND_MASK = 0b11000000;
    /** Signals the fab and buttons should be "animated away". */
    int FAB_AND_BUTTONS_SHRINK = 0b10000000;
    /** Signals the fab and buttons should be "animated back". */
    int FAB_AND_BUTTONS_EXPAND = 0b01000000;

    /** Convenience flags */
    int FAB_AND_BUTTONS_IMMEDIATE = FAB_IMMEDIATE | BUTTONS_IMMEDIATE;
    int FAB_AND_BUTTONS_SHRINK_AND_EXPAND = FAB_SHRINK_AND_EXPAND | BUTTONS_SHRINK_AND_EXPAND;

    @IntDef(
            flag = true,
            value = { FAB_IMMEDIATE, FAB_SHRINK_AND_EXPAND, FAB_MORPH, FAB_REQUEST_FOCUS,
                    BUTTONS_IMMEDIATE, BUTTONS_SHRINK_AND_EXPAND, BUTTONS_DISABLE,
                    FAB_AND_BUTTONS_IMMEDIATE, FAB_AND_BUTTONS_SHRINK_AND_EXPAND,
                    FAB_AND_BUTTONS_SHRINK, FAB_AND_BUTTONS_EXPAND }
    )
    @interface UpdateFabFlag {}

    /**
     * Requests that this container update the fab and/or its buttons because their state has
     * changed. The update may be immediate or it may be animated depending on the choice of
     * {@code updateTypes}.
     *
     * @param updateTypes indicates the types of update to apply to the fab and its buttons
     */
    void updateFab(@UpdateFabFlag int updateTypes);
}
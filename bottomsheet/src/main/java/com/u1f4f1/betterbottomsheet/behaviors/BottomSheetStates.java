package com.u1f4f1.betterbottomsheet.behaviors;

/**
 * Google makes it really hard to replace their stupid int defs with an enum, because they use
 * these constants for all kinds of animation calls within the framework.
 *
 * I'm leaving this in to make it easier to get the name from a constant while logging or in
 * the debugger.
 */
public enum BottomSheetStates {
    /**
     * The bottom sheet is dragging.
     */
    STATE_DRAGGING(1, "STATE_DRAGGING"),

    /**
     * The bottom sheet is settling.
     */
    STATE_SETTLING(2, "STATE_SETTLING"),

    /**
     * The bottom sheet is expanded.
     */
    STATE_EXPANDED(3, "STATE_EXPANDED"),

    /**
     * The bottom sheet is collapsed.
     */
    STATE_COLLAPSED(4, "STATE_COLLAPSED"),

    /**
     * The bottom sheet is hidden.
     */
    STATE_HIDDEN(5, "STATE_HIDDEN"),

    /**
     * The bottom sheet is expanded_half_way.
     */
    STATE_ANCHOR_POINT(6, "STATE_ANCHOR_POINT");

    private int id;
    private String value;

    BottomSheetStates(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public static BottomSheetStates fromInt(int state) {
        for (BottomSheetStates s : BottomSheetStates.values()) {
            if (s.id == state) {
                return s;
            }
        }

        // we want controls to default to this
        return STATE_HIDDEN;
    }

    @Override
    public String toString() {
        return value;
    }
}

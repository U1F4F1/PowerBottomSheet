package com.u1f4f1.betterbottomsheet.bottomsheet

/**
 * Google makes it really hard to replace their stupid int defs with an enum, because they use
 * these constants for all kinds of animation calls within the framework.

 * I'm leaving this in to make it easier to get the name from a constant while logging or in
 * the debugger.
 */
enum class BottomSheetStates(private val id: Int, private val value: String) {
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

    override fun toString(): String {
        return value
    }

    companion object {
        fun fromInt(state: Int): BottomSheetStates {
            // we want controls to default to this
            return BottomSheetStates.values().firstOrNull { it.id == state } ?: STATE_HIDDEN
        }
    }
}

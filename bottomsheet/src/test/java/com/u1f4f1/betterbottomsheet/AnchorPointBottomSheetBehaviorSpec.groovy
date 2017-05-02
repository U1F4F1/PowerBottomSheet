package com.u1f4f1.betterbottomsheet

import com.u1f4f1.betterbottomsheet.behaviors.AnchorPointBottomSheetBehavior
import spock.lang.Shared
import spock.lang.Specification

public class AnchorPointBottomSheetBehaviorSpec extends Specification {
    @Shared
    AnchorPointBottomSheetBehavior anchorPointBottomSheetBehavior;

    def setup() {
        anchorPointBottomSheetBehavior = new AnchorPointBottomSheetBehavior();

        anchorPointBottomSheetBehavior.parentHeight = 100
        anchorPointBottomSheetBehavior.maxOffset = 200
        anchorPointBottomSheetBehavior.anchorPoint = 666
        anchorPointBottomSheetBehavior.minOffset = 300
    }

    def "getNextStableState returns the next state the bottom sheet could settle while expanding or the current state"(int state, int expected) {
        expect:
        anchorPointBottomSheetBehavior.getNextStableState(state) == expected

        where:
        state                                                                                              | expected
        AnchorPointBottomSheetBehavior.STATE_HIDDEN       | AnchorPointBottomSheetBehavior.STATE_COLLAPSED
        AnchorPointBottomSheetBehavior.STATE_COLLAPSED    | AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT
        AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT | AnchorPointBottomSheetBehavior.STATE_EXPANDED
        AnchorPointBottomSheetBehavior.STATE_EXPANDED     | AnchorPointBottomSheetBehavior.STATE_EXPANDED
        AnchorPointBottomSheetBehavior.STATE_DRAGGING     | AnchorPointBottomSheetBehavior.STATE_DRAGGING
        AnchorPointBottomSheetBehavior.STATE_SETTLING     | AnchorPointBottomSheetBehavior.STATE_SETTLING
    }

    def "getPreviousStableState returns the next state the bottom sheet could settle at when collapsing or the current state"(int state, int expected) {
        expect:
        anchorPointBottomSheetBehavior.getPreviousStableState(state) == expected

        where:
        state                                                                                              | expected
        AnchorPointBottomSheetBehavior.STATE_EXPANDED     | AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT
        AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT | AnchorPointBottomSheetBehavior.STATE_COLLAPSED
        AnchorPointBottomSheetBehavior.STATE_COLLAPSED    | AnchorPointBottomSheetBehavior.STATE_HIDDEN
        AnchorPointBottomSheetBehavior.STATE_HIDDEN       | AnchorPointBottomSheetBehavior.STATE_HIDDEN
        AnchorPointBottomSheetBehavior.STATE_DRAGGING     | AnchorPointBottomSheetBehavior.STATE_HIDDEN
        AnchorPointBottomSheetBehavior.STATE_SETTLING     | AnchorPointBottomSheetBehavior.STATE_HIDDEN
    }

    def "getTopForState returns the value assigned to the expected variable or 0"(int state, int y) {
        expect:
        anchorPointBottomSheetBehavior.getTopForState(state) == y

        where:
        state                                                                                              | y
        AnchorPointBottomSheetBehavior.STATE_HIDDEN       | 100
        AnchorPointBottomSheetBehavior.STATE_COLLAPSED    | 200
        AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT | 666
        AnchorPointBottomSheetBehavior.STATE_EXPANDED     | 300
        AnchorPointBottomSheetBehavior.STATE_DRAGGING     | 0
        AnchorPointBottomSheetBehavior.STATE_SETTLING     | 0
    }
}

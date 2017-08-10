package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.view.View
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetState
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AnchorPointBottomSheetBehaviorStableStatesTest(val input: BottomSheetState, val expected: Boolean) {
    lateinit var behavior: AnchorPointBottomSheetBehavior<View>

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} is stable: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                    arrayOf(BottomSheetState.STATE_HIDDEN, true),
                    arrayOf(BottomSheetState.STATE_COLLAPSED, true),
                    arrayOf(BottomSheetState.STATE_ANCHOR_POINT, true),
                    arrayOf(BottomSheetState.STATE_EXPANDED, true),
                    arrayOf(BottomSheetState.STATE_DRAGGING, false),
                    arrayOf(BottomSheetState.STATE_SETTLING, false)
            )
        }
    }

    @Before
    fun setup() {
        behavior = AnchorPointBottomSheetBehavior()
    }

    @Test
    fun isStateStable() {
        behavior.isStateStable(input) shouldEqual expected
    }
}

@RunWith(Parameterized::class)
class AnchorPointBottomSheetBehaviorNextState(val input: BottomSheetState, val expected: BottomSheetState) {
    lateinit var behavior: AnchorPointBottomSheetBehavior<View>

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "the next stable state from {0} is {1}")
        fun data(): Collection<Array<Any>> {
            return listOf<Array<Any>>(
                    arrayOf(BottomSheetState.STATE_HIDDEN, BottomSheetState.STATE_COLLAPSED),
                    arrayOf(BottomSheetState.STATE_COLLAPSED, BottomSheetState.STATE_ANCHOR_POINT),
                    arrayOf(BottomSheetState.STATE_ANCHOR_POINT, BottomSheetState.STATE_EXPANDED),
                    arrayOf(BottomSheetState.STATE_EXPANDED, BottomSheetState.STATE_EXPANDED),
                    arrayOf(BottomSheetState.STATE_DRAGGING, BottomSheetState.STATE_DRAGGING),
                    arrayOf(BottomSheetState.STATE_SETTLING, BottomSheetState.STATE_SETTLING)
            )
        }
    }

    @Before
    fun setup() {
        behavior = AnchorPointBottomSheetBehavior()
    }

    @Test
    fun isStateStable() {
        behavior.getNextStableState(input) shouldEqual expected
    }
}

@RunWith(Parameterized::class)
class AnchorPointBottomSheetBehaviorPreviousState(val input: BottomSheetState, val expected: BottomSheetState) {
    lateinit var behavior: AnchorPointBottomSheetBehavior<View>

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "the previous stable state from {0} is {1}")
        fun data(): Collection<Array<Any>> {
            return listOf<Array<Any>>(
                    arrayOf(BottomSheetState.STATE_EXPANDED, BottomSheetState.STATE_ANCHOR_POINT),
                    arrayOf(BottomSheetState.STATE_ANCHOR_POINT, BottomSheetState.STATE_COLLAPSED),
                    arrayOf(BottomSheetState.STATE_COLLAPSED, BottomSheetState.STATE_HIDDEN),
                    arrayOf(BottomSheetState.STATE_HIDDEN, BottomSheetState.STATE_HIDDEN),
                    arrayOf(BottomSheetState.STATE_DRAGGING, BottomSheetState.STATE_HIDDEN),
                    arrayOf(BottomSheetState.STATE_SETTLING, BottomSheetState.STATE_HIDDEN)
            )
        }
    }

    @Before
    fun setup() {
        behavior = AnchorPointBottomSheetBehavior()
    }

    @Test
    fun isStateStable() {
        behavior.getPreviousStableState(input) shouldEqual expected
    }
}

@RunWith(Parameterized::class)
class AnchorPointBottomSheetBehaviorTopForState(val input: BottomSheetState, val expected: Int) {
    lateinit var behavior: AnchorPointBottomSheetBehavior<View>

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "the top for state {0} is {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                    arrayOf(BottomSheetState.STATE_HIDDEN, 100),
                    arrayOf(BottomSheetState.STATE_COLLAPSED, 200),
                    arrayOf(BottomSheetState.STATE_ANCHOR_POINT, 666),
                    arrayOf(BottomSheetState.STATE_EXPANDED, 300),
                    arrayOf(BottomSheetState.STATE_DRAGGING, 0),
                    arrayOf(BottomSheetState.STATE_SETTLING, 0)
            )
        }
    }

    @Before
    fun setup() {
        behavior = AnchorPointBottomSheetBehavior()
        behavior.parentHeight = 100
        behavior.maxOffset = 200
        behavior.anchorPoint = 666
        behavior.minOffset = 300
    }

    @Test
    fun isTopCorrect() {


        behavior.getTopForState(input) shouldEqual expected
    }
}
package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetState
import com.u1f4f1.betterbottomsheet.trace

class LandscapeAnchorPointBottomSheetBehavior<V : View>(context: Context, attrs: AttributeSet?) : AnchorPointBottomSheetBehavior<V>(context, attrs) {

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View?, dx: Int, dy: Int, consumed: IntArray) {
        attemptToActivateBottomsheet(child!!)

        val scrollingChild = nestedScrollingChildRef!!.get()
        if (target != scrollingChild) {
            return
        }

        val currentTop = child.top
        val newTop = currentTop - dy

        // Force stop at the anchor - do not go from collapsed to expanded in one scroll
        if (lastStableState === BottomSheetState.STATE_COLLAPSED && newTop < anchorPoint || lastStableState === BottomSheetState.STATE_EXPANDED && newTop > anchorPoint) {

            // eating all these events, don't move the view or update the callback for onSlide
            consumed[1] = dy

            nestedScrolled = true
            return
        }

        if (dy > 0) { // Upward
            trace("upward")
            if (newTop < minOffset) {
                trace("newTop < minOffset")
                consumed[1] = currentTop - minOffset
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.top, -consumed[1])
                setStateInternal(BottomSheetState.STATE_EXPANDED)
            } else {
                trace("newTop >= minOffset")
                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.top, -dy)
                setStateInternal(BottomSheetState.STATE_DRAGGING)
            }
        } else if (dy < 0) { // Downward
            trace("downward")
            if (!ViewCompat.canScrollVertically(target, -1)) {
                trace("can scroll vertically")
                if (newTop <= maxOffset || isHideable) {
                    trace("newTop <= maxOffset || hideable")
                    consumed[1] = dy
                    ViewCompat.offsetTopAndBottom(child, -dy)
                    trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.top, -dy)
                    setStateInternal(BottomSheetState.STATE_DRAGGING)
                } else {
                    trace("newTop > maxOffset || hideable")
                    consumed[1] = currentTop - maxOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
                    trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.top, -consumed[1])
                    setStateInternal(BottomSheetState.STATE_COLLAPSED)
                }
            }
        }

        dispatchOnSlide(child.top)
        lastNestedScrollDy = dy
        nestedScrolled = true
    }

    /**
     * Takes a [State] and returns the next stable state as the bottom sheet expands

     * @param currentState the last stable state of the bottom sheet
     * *
     * @return the next stable state that the sheet should settle at
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    override fun getNextStableState(currentState: BottomSheetState): BottomSheetState {
        return BottomSheetState.STATE_HIDDEN
    }

    /**
     * Takes a [State] and returns the next stable state as the bottom sheet contracts

     * @param currentState the last stable state of the bottom sheet
     * *
     * @return the next stable state that the sheet should settle at
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    override fun getPreviousStableState(currentState: BottomSheetState): BottomSheetState {
        return BottomSheetState.STATE_HIDDEN
    }

    /**
     * Returns a measured y position that the top of the bottom sheet should settle at

     * @param state the [State] that the sheet is going to settle at
     * *
     * @return the y position that the top of the sheet will be at once it is done settling
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    override fun getTopForState(state: BottomSheetState): Int {
        return 0
    }

    override fun setStateInternal(state: BottomSheetState) {
        // we never collapse this sheet
        if (state == BottomSheetState.STATE_COLLAPSED) {
            this.state = BottomSheetState.STATE_HIDDEN
            lastStableState = BottomSheetState.STATE_HIDDEN
        }
        super.setStateInternal(state)

        val bottomSheet = viewRef!!.get()
        attemptToActivateBottomsheet(bottomSheet as View)
    }
}

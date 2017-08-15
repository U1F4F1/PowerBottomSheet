package com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.os.Parcelable
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetState
import com.u1f4f1.powerbottomsheet.bottomsheet.SavedState

class TabletAnchorPointBottomSheetBehavior<V : View>(context: Context, attrs: AttributeSet?) : AnchorPointBottomSheetBehavior<V>(context, attrs) {

    override fun onRestoreInstanceState(parent: CoordinatorLayout?, child: V?, state: Parcelable?) {
        val ss = state as SavedState
        // Intermediate states are restored as the anchored state, collapse state is ignored
        if (ss.bottomSheetState === BottomSheetState.STATE_DRAGGING || ss.bottomSheetState === BottomSheetState.STATE_SETTLING || ss.bottomSheetState === BottomSheetState.STATE_COLLAPSED) {
            this.state = BottomSheetState.STATE_ANCHOR_POINT
            attemptToActivateBottomsheet(child!!)
        } else {
            this.state = ss.bottomSheetState
        }

        lastStableState = this.state
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        attemptToActivateBottomsheet(child)

        val scrollingChild = nestedScrollingChildRef!!.get()
        if (target !== scrollingChild) {
            return
        }

        val currentTop = child.top
        val newTop = currentTop - dy

        // Force stop at the anchor - do not collapse
        if (lastStableState === BottomSheetState.STATE_ANCHOR_POINT && newTop > anchorPoint) {
            consumed[1] = dy
            ViewCompat.offsetTopAndBottom(child, anchorPoint - currentTop)
            dispatchOnSlide(child.top)
            nestedScrolled = true
            return
        }

        if (dy > 0) { // Upward
            if (newTop < minOffset) {
                consumed[1] = currentTop - minOffset
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                setStateInternal(BottomSheetState.STATE_EXPANDED)
            } else {
                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                setStateInternal(BottomSheetState.STATE_DRAGGING)
            }
        } else if (dy < 0) { // Downward
            if (!ViewCompat.canScrollVertically(target, -1)) {
                if (newTop <= maxOffset || isHideable) {
                    consumed[1] = dy
                    ViewCompat.offsetTopAndBottom(child, -dy)
                    setStateInternal(BottomSheetState.STATE_DRAGGING)
                } else {
                    consumed[1] = currentTop - maxOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
                    setStateInternal(BottomSheetState.STATE_ANCHOR_POINT)
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
    override fun getNextStableState(currentState: BottomSheetState): BottomSheetState {
        when (currentState) {
            BottomSheetState.STATE_HIDDEN -> return BottomSheetState.STATE_ANCHOR_POINT
            BottomSheetState.STATE_ANCHOR_POINT -> return BottomSheetState.STATE_EXPANDED
            else -> return currentState
        }
    }

    /**
     * Takes a [State] and returns the next stable state as the bottom sheet contracts

     * @param currentState the last stable state of the bottom sheet
     * *
     * @return the next stable state that the sheet should settle at
     */
    override fun getPreviousStableState(currentState: BottomSheetState): BottomSheetState {
        when (currentState) {
            BottomSheetState.STATE_EXPANDED -> return BottomSheetState.STATE_ANCHOR_POINT
            BottomSheetState.STATE_ANCHOR_POINT -> return BottomSheetState.STATE_HIDDEN
            else -> return BottomSheetState.STATE_HIDDEN
        }
    }

    /**
     * Returns a measured y position that the top of the bottom sheet should settle at

     * @param state the [State] that the sheet is going to settle at
     * *
     * @return the y position that the top of the sheet will be at once it is done settling
     */
    override fun getTopForState(state: BottomSheetState): Int {
        when (state) {
            BottomSheetState.STATE_HIDDEN -> return parentHeight
            BottomSheetState.STATE_COLLAPSED -> return maxOffset
            BottomSheetState.STATE_ANCHOR_POINT -> return anchorPoint
            BottomSheetState.STATE_EXPANDED -> return minOffset
            else -> return 0
        }
    }

    override fun setStateInternal(state: BottomSheetState) {
        var s = state
        // we never collapse this sheet
        if (s == BottomSheetState.STATE_COLLAPSED) {
            s = BottomSheetState.STATE_ANCHOR_POINT
            lastStableState = BottomSheetState.STATE_ANCHOR_POINT
        }
        super.setStateInternal(s)
    }
}

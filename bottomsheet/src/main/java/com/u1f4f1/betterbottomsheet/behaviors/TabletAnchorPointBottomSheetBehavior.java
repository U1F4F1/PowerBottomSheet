package com.u1f4f1.betterbottomsheet.behaviors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class TabletAnchorPointBottomSheetBehavior<V extends View> extends AnchorPointBottomSheetBehavior<V> {
    @IntDef({STATE_EXPANDED, STATE_DRAGGING, STATE_ANCHOR_POINT, STATE_SETTLING, STATE_HIDDEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State { }

    @State int state = STATE_ANCHOR_POINT;

    public TabletAnchorPointBottomSheetBehavior() {
        super();
    }

    public TabletAnchorPointBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        AnchorPointBottomSheetBehavior.SavedState ss = (AnchorPointBottomSheetBehavior.SavedState) state;
        // Intermediate states are restored as the anchored state, collapse state is ignored
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING || ss.state == STATE_COLLAPSED) {
            this.state = STATE_ANCHOR_POINT;
            attemptToActivateBottomsheet(child);
        } else {
            this.state = ss.state;
        }

        lastStableState = this.state;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed) {
        attemptToActivateBottomsheet(child);

        View scrollingChild = nestedScrollingChildRef.get();
        if (target != scrollingChild) {
            return;
        }

        int currentTop = child.getTop();
        int newTop = currentTop - dy;

        // Force stop at the anchor - do not collapse
        if (lastStableState == STATE_ANCHOR_POINT && newTop > anchorPoint) {
            consumed[1] = dy;
            ViewCompat.offsetTopAndBottom(child, anchorPoint - currentTop);
            dispatchOnSlide(child.getTop());
            nestedScrolled = true;
            return;
        }

        if (dy > 0) { // Upward
            if (newTop < minOffset) {
                consumed[1] = currentTop - minOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dy < 0) { // Downward
            if (!ViewCompat.canScrollVertically(target, -1)) {
                if (newTop <= maxOffset || hideable) {
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    setStateInternal(STATE_DRAGGING);
                } else {
                    consumed[1] = currentTop - maxOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    setStateInternal(STATE_ANCHOR_POINT);
                }
            }
        }

        dispatchOnSlide(child.getTop());
        lastNestedScrollDy = dy;
        nestedScrolled = true;
    }

    /**
     * Takes a {@link State} and returns the next stable state as the bottom sheet expands
     *
     * @param currentState the last stable state of the bottom sheet
     * @return the next stable state that the sheet should settle at
     */
    @State
    @SuppressLint("SwitchIntDef")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    int getNextStableState(@State int currentState) {
        switch (currentState) {
            case STATE_HIDDEN:
                return STATE_ANCHOR_POINT;
            case STATE_ANCHOR_POINT:
                return STATE_EXPANDED;
            default:
                return currentState;
        }
    }

    /**
     * Takes a {@link State} and returns the next stable state as the bottom sheet contracts
     *
     * @param currentState the last stable state of the bottom sheet
     * @return the next stable state that the sheet should settle at
     */
    @State
    @SuppressLint("SwitchIntDef")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    int getPreviousStableState(@State int currentState) {
        switch (currentState) {
            case STATE_EXPANDED:
                return STATE_ANCHOR_POINT;
            case STATE_ANCHOR_POINT:
                return STATE_HIDDEN;
            default:
                return STATE_HIDDEN;
        }
    }

    /**
     * Returns a measured y position that the top of the bottom sheet should settle at
     *
     * @param state the {@link State} that the sheet is going to settle at
     * @return the y position that the top of the sheet will be at once it is done settling
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    int getTopForState(int state) {
        switch (state) {
            case STATE_HIDDEN:
                return parentHeight;
            case STATE_COLLAPSED:
                return maxOffset;
            case STATE_ANCHOR_POINT:
                return anchorPoint;
            case STATE_EXPANDED:
                return minOffset;
            default:
                return 0;
        }
    }

    @Override
    public void setState(@AnchorPointBottomSheetBehavior.State int state) {
        // we never collapse this sheet
        if (state == STATE_COLLAPSED) {
            state = STATE_ANCHOR_POINT;
            this.lastStableState = STATE_ANCHOR_POINT;
        }

        super.setState(state);
    }

    @Override
    void setStateInternal(@AnchorPointBottomSheetBehavior.State int state) {
        // we never collapse this sheet
        if (state == STATE_COLLAPSED) {
            state = STATE_ANCHOR_POINT;
            this.lastStableState = STATE_ANCHOR_POINT;
        }
        super.setStateInternal(state);

        View bottomSheet = viewRef.get();
        attemptToActivateBottomsheet(bottomSheet);
    }
}

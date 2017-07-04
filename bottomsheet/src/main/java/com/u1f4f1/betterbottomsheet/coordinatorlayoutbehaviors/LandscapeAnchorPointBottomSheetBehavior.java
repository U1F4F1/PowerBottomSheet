package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class LandscapeAnchorPointBottomSheetBehavior<V extends View> extends AnchorPointBottomSheetBehavior<V> {

    public LandscapeAnchorPointBottomSheetBehavior() {
        super();
    }

    public LandscapeAnchorPointBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
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

        // Force stop at the anchor - do not go from collapsed to expanded in one scroll
        if ((lastStableState == STATE_COLLAPSED && newTop < anchorPoint) ||
                (lastStableState == STATE_EXPANDED && newTop > anchorPoint)) {

            // eating all these events, don't move the view or update the callback for onSlide
            consumed[1] = dy;

            nestedScrolled = true;
            return;
        }

        if (dy > 0) { // Upward
            logger.trace("upward");
            if (newTop < minOffset) {
                logger.trace("newTop < minOffset");
                consumed[1] = currentTop - minOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                logger.trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.getTop(), -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                logger.trace("newTop >= minOffset");
                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                logger.trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.getTop(), -dy);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dy < 0) { // Downward
            logger.trace("downward");
            if (!ViewCompat.canScrollVertically(target, -1)) {
                logger.trace("can scroll vertically");
                if (newTop <= maxOffset || hideable) {
                    logger.trace("newTop <= maxOffset || hideable");
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    logger.trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.getTop(), -dy);
                    setStateInternal(STATE_DRAGGING);
                } else {
                    logger.trace("newTop > maxOffset || hideable");
                    consumed[1] = currentTop - maxOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    logger.trace("ViewCompat.offsetTopAndBottom(%s, %s)", child.getTop(), -consumed[1]);
                    setStateInternal(STATE_COLLAPSED);
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
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    int getNextStableState(@State int currentState) {
        switch (currentState) {
            case STATE_COLLAPSED:
            case STATE_EXPANDED:
            case STATE_ANCHOR_POINT:
            case STATE_DRAGGING:
            case STATE_SETTLING:
            case STATE_HIDDEN:
            default:
                return STATE_HIDDEN;
        }
    }

    /**
     * Takes a {@link State} and returns the next stable state as the bottom sheet contracts
     *
     * @param currentState the last stable state of the bottom sheet
     * @return the next stable state that the sheet should settle at
     */
    @State
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    int getPreviousStableState(@State int currentState) {
        switch (currentState) {
            case STATE_COLLAPSED:
            case STATE_EXPANDED:
            case STATE_ANCHOR_POINT:
            case STATE_DRAGGING:
            case STATE_SETTLING:
            case STATE_HIDDEN:
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
            state = STATE_HIDDEN;
            this.lastStableState = STATE_HIDDEN;
        }

        super.setState(state);
    }

    @Override
    void setStateInternal(@AnchorPointBottomSheetBehavior.State int state) {
        // we never collapse this sheet
        if (state == STATE_COLLAPSED) {
            state = STATE_HIDDEN;
            this.lastStableState = STATE_HIDDEN;
        }
        super.setStateInternal(state);

        View bottomSheet = viewRef.get();
        attemptToActivateBottomsheet(bottomSheet);
    }
}

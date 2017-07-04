/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.kylealanr.gesturedetectors.GestureDetectors;
import com.u1f4f1.betterbottomsheet.R;
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheet;
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetStates;

import org.jetbrains.annotations.Contract;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import inkapplicaitons.android.logger.ConsoleLogger;
import inkapplicaitons.android.logger.Logger;

/**
 * An interaction behavior plugin for a child view of {@link CoordinatorLayout} to make it work as
 * a bottom sheet.
 */
@SuppressWarnings("WeakerAccess")
@SuppressLint("PrivateResource")
public class AnchorPointBottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    /**
     * Callback for monitoring events about bottom sheets.
     */
    public interface BottomSheetStateCallback {

        /**
         * Called when the bottom sheet changes its state.
         *
         * @param bottomSheet The bottom sheet view.
         * @param newState    The new state. This will be one of {@link #STATE_DRAGGING},
         *                    {@link #STATE_SETTLING}, {@link #STATE_EXPANDED},
         *                    {@link #STATE_COLLAPSED}, or {@link #STATE_HIDDEN}.
         */
        void onStateChanged(@NonNull View bottomSheet, @State int newState);
    }

    /**
     * Callback for monitoring events about bottom sheets.
     */
    public interface BottomSheetSlideCallback {
        /**
         * Called when the bottom sheet is being dragged.
         *
         * @param bottomSheet The bottom sheet view.
         * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset
         *                    increases as this bottom sheet is moving upward. From 0 to 1 the sheet
         *                    is between collapsed and expanded states and from -1 to 0 it is
         *                    between hidden and collapsed states.
         */
        void onSlide(@NonNull View bottomSheet, float slideOffset);
    }

    /**
     * The bottom sheet is dragging.
     */
    public static final int STATE_DRAGGING = 1;

    /**
     * The bottom sheet is settling.
     */
    public static final int STATE_SETTLING = 2;

    /**
     * The bottom sheet is expanded.
     */
    public static final int STATE_EXPANDED = 3;

    /**
     * The bottom sheet is collapsed.
     */
    public static final int STATE_COLLAPSED = 4;

    /**
     * The bottom sheet is hidden.
     */
    public static final int STATE_HIDDEN = 5;

    /**
     * The bottom sheet is expanded_half_way.
     */
    public static final int STATE_ANCHOR_POINT = 6;

    @IntDef({STATE_EXPANDED, STATE_COLLAPSED, STATE_DRAGGING, STATE_ANCHOR_POINT, STATE_SETTLING, STATE_HIDDEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State { }

    static final int ANCHOR_POINT_AUTO = 700;
    protected int anchorPoint;

    @State int lastStableState = STATE_HIDDEN;

    /**
     * Peek at the 16:9 ratio keyline of its parent.
     *
     * This can be used as a parameter for {@link #setPeekHeight(int)}.
     * {@link #getPeekHeight()} will return this when the value is set.
     */
    public static final int PEEK_HEIGHT_AUTO = -1;

    static final float HIDE_THRESHOLD = 0.5f;

    static final float HIDE_FRICTION = 0.1f;

    protected float maximumVelocity;

    protected int peekHeight;
    protected boolean peekHeightAuto;
    protected int peekHeightMin;

    protected int minOffset;
    protected int maxOffset;

    protected boolean hideable;

    protected boolean skipCollapsed;

    // starting off the screen so we can animate the move up
    @State protected int state = STATE_HIDDEN;

    protected ViewDragHelper viewDragHelper;

    protected int lastNestedScrollDy;

    protected boolean nestedScrolled;
    protected int parentHeight;

    protected WeakReference<V> viewRef;

    protected WeakReference<View> nestedScrollingChildRef;
    protected boolean touchingScrollingChild;

    protected List<BottomSheetStateCallback> stateCallbacks = new CopyOnWriteArrayList<>();
    protected List<BottomSheetSlideCallback> slideCallbacks = new CopyOnWriteArrayList<>();

    protected SparseIntArray shouldScrollWithView = new SparseIntArray();

    protected VelocityTracker velocityTracker;
    protected boolean ignoreEvents;

    protected int activePointerId;

    protected int initialY;

    protected int height;

    protected boolean bottomSheetIsActive;

    protected GestureDetectorCompat gestureDetectorCompat;

    protected static final Logger logger = new ConsoleLogger("ANDROIDISBAD");

    /**
     * Default constructor for instantiating BottomSheetBehaviors.
     */
    @SuppressWarnings("unused")
    public AnchorPointBottomSheetBehavior() {
    }

    /**
     * Default constructor for inflating BottomSheetBehaviors from layout.
     *
     * @param context The {@link Context}.
     * @param attrs   The {@link AttributeSet}.
     */
    @SuppressWarnings("unused")
    public AnchorPointBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);

        height = context.getResources().getSystem().getDisplayMetrics().heightPixels;

        TypedArray a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.BottomSheetBehavior_Layout);
        TypedValue value = a.peekValue(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data);
        } else {
            setPeekHeight(a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO));
        }

        setHideable(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
        setSkipCollapsed(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));

        a = context.obtainStyledAttributes(attrs, R.styleable.AnchorPointBottomSheetBehavior);
        setAnchorPoint((int) a.getDimension(R.styleable.AnchorPointBottomSheetBehavior_anchorPoint, ANCHOR_POINT_AUTO));
        a.recycle();

        ViewConfiguration configuration = ViewConfiguration.get(context);
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();

        gestureDetectorCompat =
                new GestureDetectorCompat(context, new GestureDetectors.OnSingleTapUp(this::handleOnSingleTapUp));

        logger.info("height: %s, width: %s, anchorPoint: %s, peekHeight: %s, minOffset: %s, maxOffset: %s", Resources.getSystem().getDisplayMetrics().heightPixels, Resources.getSystem().getDisplayMetrics().widthPixels, anchorPoint, peekHeight, minOffset, maxOffset);
    }

    private boolean handleOnSingleTapUp(MotionEvent e) {
        if (state == STATE_COLLAPSED) {
            if (viewRef.get() != null) {
                ((BottomSheet) viewRef.get()).setActive(true);
                bottomSheetIsActive = true;
            }
            setState(STATE_ANCHOR_POINT);
            return true;
        }

        return false;
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        return new SavedState(super.onSaveInstanceState(parent, child), state);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Intermediate states are restored as collapsed state
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            this.state = STATE_COLLAPSED;
        } else {
            this.state = ss.state;
        }

        lastStableState = this.state;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            ViewCompat.setFitsSystemWindows(child, true);
        }

        int savedTop = child.getTop();
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection);
        // Offset the bottom sheet
        parentHeight = parent.getHeight();

        int peekHeight;
        if (peekHeightAuto) {
            if (peekHeightMin == 0) {
                peekHeightMin = parent.getResources().getDimensionPixelSize(android.support.design.R.dimen.design_bottom_sheet_peek_height_min);
            }
            peekHeight = Math.max(peekHeightMin, parentHeight - parent.getWidth() * 9 / 16);
        } else {
            peekHeight = this.peekHeight;
        }

        minOffset = Math.max(0, parentHeight - child.getHeight());
        maxOffset = Math.max(parentHeight - peekHeight, minOffset);

        if (state == STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, minOffset);
        } else if (hideable && state == STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, parentHeight);
        } else if (state == STATE_ANCHOR_POINT) {
            ViewCompat.offsetTopAndBottom(child, anchorPoint);
        } else if (state == STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, maxOffset);
        } else if (state == STATE_DRAGGING || state == STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.getTop());
        }

        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }

        viewRef = new WeakReference<>(child);
        nestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        // send this event to the GestureDetector here so we can react to an event without subscribing to updates
        if (event.getRawY() > height - peekHeight && state == STATE_COLLAPSED) {
            gestureDetectorCompat.onTouchEvent(event);
        }

        if (!child.isShown()) {
            ignoreEvents = true;
            return false;
        }
        int action = MotionEventCompat.getActionMasked(event);
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchingScrollingChild = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false;
                    return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                int initialX = (int) event.getX();
                initialY = (int) event.getY();

                if (state == STATE_ANCHOR_POINT) {
                    activePointerId = event.getPointerId(event.getActionIndex());
                    touchingScrollingChild = true;
                } else {
                    View scroll = nestedScrollingChildRef.get();
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = event.getPointerId(event.getActionIndex());
                        touchingScrollingChild = true;
                    }
                }

                ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID &&
                        !parent.isPointInChildBounds(child, initialX, initialY);
                break;
        }
        if (!ignoreEvents && viewDragHelper.shouldInterceptTouchEvent(event)) {
            return true;
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        View scroll = nestedScrollingChildRef.get();
        return action == MotionEvent.ACTION_MOVE && scroll != null &&
                !ignoreEvents && state != STATE_DRAGGING &&
                !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) &&
                Math.abs(initialY - event.getY()) > viewDragHelper.getTouchSlop();
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            return false;
        }
        int action = MotionEventCompat.getActionMasked(event);
        if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        viewDragHelper.processTouchEvent(event);
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the bottom sheet in case it is not captured and the touch slop is passed.
        if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            if (Math.abs(initialY - event.getY()) > viewDragHelper.getTouchSlop()) {
                viewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
            }
        }
        return !ignoreEvents;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        if (shouldScrollWithView.get(child.hashCode() + directTargetChild.hashCode(), -1) != -1) {
            return shouldScrollWithView.get(child.hashCode() + directTargetChild.hashCode()) == 1;
        }

        lastNestedScrollDy = 0;
        nestedScrolled = false;

        // directTargetChild is the direct parent of the NestedScrollingChild that got us here
        boolean directTargetChildDescendsFromChild = false;
        boolean directTargetChildIsChild = directTargetChild.equals(child);
        boolean verticalNestedScroll = (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;

        if (child instanceof ViewGroup) {
            directTargetChildDescendsFromChild = recursivelyCheckIfDescendedFrom(directTargetChild, ((ViewGroup) child));
        }

        // only handle scrolls for children of the Child that scroll vertically
        // the Child is what gets the Behavior attached to it, we don't want to scroll for different parents
        boolean shouldScroll = (directTargetChildDescendsFromChild || directTargetChildIsChild) && verticalNestedScroll;
        shouldScrollWithView.put(child.hashCode() + directTargetChild.hashCode(), shouldScroll ? 1 : 0);
        return shouldScroll;
    }

    boolean recursivelyCheckIfDescendedFrom(View decedent, ViewGroup ancestor) {
        while (!targetViewIsChildOf(decedent, ancestor)) {
            if (decedent.getParent() instanceof CoordinatorLayout) return false;

            targetViewIsChildOf(((View) decedent.getParent()), ancestor);
        }

        return true;
    }

    boolean targetViewIsChildOf(View potentialChild, ViewGroup potentialParent) {
        return potentialChild.getParent().equals(potentialParent);
    }

    @Override
    public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
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

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
        if (child.getTop() == minOffset) {
            setStateInternal(STATE_EXPANDED);
            lastStableState = STATE_EXPANDED;
            return;
        }

        if (child.getTop() == peekHeight) {
            setStateInternal(STATE_COLLAPSED);
            lastStableState = STATE_COLLAPSED;
            return;
        }

        if (target != nestedScrollingChildRef.get() || !nestedScrolled) {
            return;
        }

        int top;
        int targetState;

        // last nested scroll is the raw values in pixels of the last drag event
        // if will be negative if the user swiped down and positive if they swiped up
        float percentage = (float) lastNestedScrollDy / height;
        
        // attempt to snap to the right state with the current y velocity, but fall back to the
        // last movement by percentage of the screen
        if (getYVelocity() < -150) {
            // snap up
            logger.trace("velocity %s snapping up", getYVelocity());
            targetState = getNextStableState(lastStableState);
        } else if (getYVelocity() > 150) {
            //snap down
            logger.trace("velocity %s snapping down", getYVelocity());
            targetState = getPreviousStableState(lastStableState);
        } else {
            if (percentage > 0.01) {
                // snap up
                logger.trace("percentage moved %s snapping up", percentage);
                targetState = getNextStableState(lastStableState);
            } else if (percentage < -0.01) {
                //snap down
                logger.trace("percentage moved %s snapping down", percentage);
                targetState = getPreviousStableState(lastStableState);
            } else {
                // eventually fall all the way back to the last state if velocity is 0 and the
                // touch event only moved a small amount
                logger.trace("snapping to last stable state");
                targetState = lastStableState;
            }
        }

        top = getTopForState(targetState);
        this.lastStableState = targetState;
        if (viewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            logger.trace("smoothSlideViewTo %s to %s with state %s", child.getClass().getSimpleName(), top, BottomSheetStates.Companion.fromInt(targetState));
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, targetState));
        } else {
            setStateInternal(targetState);
        }

        nestedScrolled = false;
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
                return STATE_COLLAPSED;
            case STATE_COLLAPSED:
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
                return STATE_COLLAPSED;
            case STATE_COLLAPSED:
                return STATE_HIDDEN;
            default:
                return STATE_HIDDEN;
        }
    }

    boolean isStateStable(@State int currentState) {
        switch (currentState) {
            case STATE_ANCHOR_POINT:
            case STATE_COLLAPSED:
            case STATE_EXPANDED:
            case STATE_HIDDEN:
                return true;
            case STATE_DRAGGING:
            case STATE_SETTLING:
            default:
                return false;
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
                logger.debug("STATE_HIDDEN top: %s", parentHeight);
                return parentHeight;
            case STATE_COLLAPSED:
                logger.debug("STATE_COLLAPSED top: %s", maxOffset);
                return maxOffset;
            case STATE_ANCHOR_POINT:
                logger.debug("STATE_ANCHOR_POINT top: %s", anchorPoint);
                return anchorPoint;
            case STATE_EXPANDED:
                logger.debug("STATE_EXPANDED top: %s", minOffset);
                return minOffset;
            default:
                logger.debug("UNKNOWN_STATE top: %s", 0);
                return 0;
        }
    }

    void attemptToActivateBottomsheet(View view) {
        if (view instanceof BottomSheet) {
            activateBottomsheetIfTopAbovePeekHeight(((BottomSheet) view));
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void activateBottomsheetIfTopAbovePeekHeight(BottomSheet bottomSheet) {
        boolean shouldActivate = bottomSheet.getTop() < height - peekHeight || state != STATE_COLLAPSED;

        if (shouldActivate == bottomSheetIsActive) return;

        if (shouldActivate) {
            bottomSheet.setActive(true);
            bottomSheetIsActive = true;
        } else {
            bottomSheet.setActive(false);
            bottomSheetIsActive = false;
        }
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY) {
        return target == nestedScrollingChildRef.get() && (state != STATE_EXPANDED || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY));
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY, boolean consumed) {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }

    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels, or
     *                   {@link #PEEK_HEIGHT_AUTO} to configure the sheet to peek automatically
     *                   at 16:9 ratio keyline.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    public void setPeekHeight(int peekHeight) {
        boolean layout = false;
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!peekHeightAuto) {
                peekHeightAuto = true;
                layout = true;
            }
        } else if (peekHeightAuto || this.peekHeight != peekHeight) {
            peekHeightAuto = false;
            this.peekHeight = Math.max(0, peekHeight);
            maxOffset = parentHeight - peekHeight;
            layout = true;
        }

        if (layout && state == STATE_COLLAPSED && viewRef != null) {
            V view = viewRef.get();
            if (view != null) {
                view.requestLayout();
            }
        }
    }

    /**
     * Gets the height of the bottom sheet when it is collapsed.
     *
     * @return The height of the collapsed bottom sheet in pixels, or {@link #PEEK_HEIGHT_AUTO}
     * if the sheet is configured to peek automatically at 16:9 ratio keyline
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    @Contract(pure = true)
    public int getPeekHeight() {
        return peekHeightAuto ? PEEK_HEIGHT_AUTO : peekHeight;
    }

    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable {@code true} to make this bottom sheet hideable.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public void setHideable(boolean hideable) {
        this.hideable = hideable;
    }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return {@code true} if this bottom sheet can hide.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public boolean isHideable() {
        return hideable;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public void setSkipCollapsed(boolean skipCollapsed) {
        this.skipCollapsed = skipCollapsed;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once.
     *
     * @return Whether the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public boolean getSkipCollapsed() {
        return skipCollapsed;
    }

    /**
     * Sets a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    public void addBottomSheetStateCallback(BottomSheetStateCallback callback) {
        this.stateCallbacks.add(callback);
    }

    /**
     * Sets a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    public void addBottomSheetSlideCallback(BottomSheetSlideCallback callback) {
        this.slideCallbacks.add(callback);
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, or
     *              {@link #STATE_HIDDEN}.
     */
    public void setState(final @State int state) {
        logger.trace("setting state %s", BottomSheetStates.Companion.fromInt(state));

        if (state == this.state) {
            return;
        }

        if (isStateStable(state)) {
            this.state = state;
            this.lastStableState = state;
        }

        if (viewRef == null) {
            // The view is not laid out yet; modify state and let onLayoutChild handle it later
            logger.trace("view not laid out yet");
            return;
        }

        final V child = viewRef.get();
        if (child == null) {
            logger.trace("viewRef returned null");
            return;
        }

        attemptToActivateBottomsheet(child);

        // Start the animation; wait until a pending layout if there is one.
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            child.post(() -> startSettlingAnimation(child, state));
        } else {
            startSettlingAnimation(child, state);
        }
    }

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED}, {@link #STATE_DRAGGING},
     * and {@link #STATE_SETTLING}.
     */
    @State
    public final int getState() {
        return state;
    }

    public boolean isStable() {
        return isStateStable(state);
    }

    public void setAnchorPoint(int anchorPoint) {
        this.anchorPoint = anchorPoint;
    }

    public int getAnchorPoint() {
        return anchorPoint;
    }

    void setStateInternal(@State int state) {
        if (this.state == state) {
            return;
        }

        this.state = state;

        // only send stable states, post it to the views message queue
        if (isStateStable(state)) {
            View bottomSheet = viewRef.get();
            if (bottomSheet != null && stateCallbacks != null) {
                ((View) bottomSheet.getParent()).post(() -> {
                    for (BottomSheetStateCallback callback : stateCallbacks) {
                        callback.onStateChanged(bottomSheet, state);
                    }
                });
            }
        }
    }

    void reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    // based on current velocity, could be refactored to handle moving between states
    boolean shouldHide(View child, float yvel) {
        if (skipCollapsed) {
            return true;
        }

        if (child.getTop() < maxOffset) {
            // It should not hide, but collapse.
            return false;
        }

        final float newTop = child.getTop() + yvel * HIDE_FRICTION;
        return Math.abs(newTop - maxOffset) / (float) peekHeight > HIDE_THRESHOLD;
    }

    View findScrollingChild(View view) {
        if (view instanceof NestedScrollingChild) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }

        return null;
    }

    // tracks velocity from $CALL_SITE
    float getYVelocity() {
        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
        return VelocityTrackerCompat.getYVelocity(velocityTracker, activePointerId);
    }

    void startSettlingAnimation(View child, int state) {
        int top;
        if (state == STATE_COLLAPSED) {
            top = maxOffset;
        } else if (state == STATE_ANCHOR_POINT) {
            top = anchorPoint;
        } else if (state == STATE_EXPANDED) {
            top = minOffset;
        } else if (hideable && state == STATE_HIDDEN) {
            top = parentHeight;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }

        if (viewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            logger.trace("smoothSlideViewTo %s, %s", top, BottomSheetStates.Companion.fromInt(state));
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        } else {
            // we din't need to slide the view, so we aren't settling
            setStateInternal(state);
        }
    }

    final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (state == STATE_DRAGGING) {
                return false;
            }

            if (touchingScrollingChild) {
                return false;
            }

            if (state == STATE_EXPANDED && activePointerId == pointerId) {
                View scroll = nestedScrollingChildRef.get();
                if (scroll != null && ViewCompat.canScrollVertically(scroll, -1)) {
                    // Let the content scroll up
                    return false;
                }
            }

            return viewRef != null && viewRef.get() == child;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            dispatchOnSlide(top);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(STATE_DRAGGING);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top;
            @State int targetState;
            if (yvel < 0) { // Moving up
                logger.trace("view released while moving up");
                top = minOffset;
                targetState = STATE_EXPANDED;
            } else if (hideable && shouldHide(releasedChild, yvel)) {
                logger.trace("view released while we should hide the child view");
                top = parentHeight;
                targetState = STATE_HIDDEN;
            } else if (yvel == 0.f) {
                logger.trace("view released while not moving");
                int currentTop = releasedChild.getTop();
                logger.trace("abs(%s - %s) < abs(%s - %s) = %s", currentTop, minOffset, currentTop, maxOffset, Math.abs(currentTop - minOffset) < Math.abs(currentTop - maxOffset));
                if (Math.abs(currentTop - minOffset) < Math.abs(currentTop - maxOffset)) {
                    top = minOffset;
                    targetState = STATE_EXPANDED;
                } else {
                    top = maxOffset;
                    targetState = STATE_COLLAPSED;
                }
            } else {
                logger.trace("view released while moving down");
                top = maxOffset;
                targetState = STATE_COLLAPSED;
            }
            if (viewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top)) {
                logger.trace("settling captured view");
                setStateInternal(STATE_SETTLING);
                ViewCompat.postOnAnimation(releasedChild, new SettleRunnable(releasedChild, targetState));
            } else {
                logger.trace("setting captured view state without settling");
                setStateInternal(targetState);
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return constrain(top, minOffset, hideable ? parentHeight : maxOffset);
        }

        int constrain(int amount, int low, int high) {
            return amount < low ? low : (amount > high ? high : amount);
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return child.getLeft();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            if (hideable) {
                return parentHeight - minOffset;
            } else {
                return maxOffset - minOffset;
            }
        }
    };

    void dispatchOnSlide(int top) {
        View bottomSheet = viewRef.get();
        if (bottomSheet != null && slideCallbacks != null) {
            if (top > maxOffset) {
                for (BottomSheetSlideCallback callback : slideCallbacks) {
                    callback.onSlide(bottomSheet, (float) (maxOffset - top) / (parentHeight - maxOffset));
                }
            } else {
                for (BottomSheetSlideCallback callback : slideCallbacks) {
                    callback.onSlide(bottomSheet, (float) (maxOffset - top) / ((maxOffset - minOffset)));
                }
            }
        }
    }

    int getPeekHeightMin() {
        return peekHeightMin;
    }

    class SettleRunnable implements Runnable {

        private final View mView;

        @State private final int mTargetState;

        SettleRunnable(View view, @State int targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (viewDragHelper != null && viewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                setStateInternal(mTargetState);
            }
        }
    }

    protected static class SavedState extends AbsSavedState {
        @State
        final int state;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            state = source.readInt();
        }

        public SavedState(Parcelable superState, @State int state) {
            super(superState);
            this.state = state;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                });
    }

    /**
     * A utility function to get the {@link BottomSheetBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link BottomSheetBehavior}.
     * @return The {@link BottomSheetBehavior} associated with the {@code view}.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable <V extends View> AnchorPointBottomSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            logger.warn("The view is not a child of CoordinatorLayout");
            return null;
        }

        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof AnchorPointBottomSheetBehavior)) {
            logger.warn("The view is not associated with AnchorPointBottomSheetBehavior");
            return null;
        }

        return (AnchorPointBottomSheetBehavior<V>) behavior;
    }
}

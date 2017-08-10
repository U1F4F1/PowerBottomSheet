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

package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import android.support.annotation.VisibleForTesting
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.*
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.*
import com.kylealanr.gesturedetectors.GestureDetectors
import com.u1f4f1.betterbottomsheet.R
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheet
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetState
import com.u1f4f1.betterbottomsheet.bottomsheet.SavedState
import com.u1f4f1.betterbottomsheet.debug
import com.u1f4f1.betterbottomsheet.info
import com.u1f4f1.betterbottomsheet.trace
import inkapplicaitons.android.logger.ConsoleLogger
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * An interaction behavior plugin for a child view of [CoordinatorLayout] to make it work as
 * a bottom sheet.
 *
 * This file is at times a nightmare but this is what I ended up with after refactoring what was in
 * the support library. I'm going to try some weird with how this file is organized. I'm organizing this
 * with interfaces at the top with the member variables under each, variables about tracking state,
 * the definitions for the heights of each state, etc
 */
@SuppressLint("PrivateResource")
open class AnchorPointBottomSheetBehavior<V : View> : CoordinatorLayout.Behavior<V> {

    //region INTERFACE DEFINITIONS

    /**
     * Callback for monitoring events about bottom sheets.
     */
    interface BottomSheetStateCallback {

        /**
         * Called when the bottom sheet changes its state.

         * @param bottomSheet The bottom sheet view.
         * *
         * @param newState    The new state. This will be one of [.STATE_DRAGGING],
         * *                    [.STATE_SETTLING], [.STATE_EXPANDED],
         * *                    [.STATE_COLLAPSED], or [.STATE_HIDDEN].
         */
        fun onStateChanged(bottomSheet: View, newState: BottomSheetState)
    }

    /**
     * Callback for monitoring events about bottom sheets.
     */
    interface BottomSheetSlideCallback {
        /**
         * Called when the bottom sheet is being dragged.

         * @param bottomSheet The bottom sheet view.
         * *
         * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset
         * *                    increases as this bottom sheet is moving upward. From 0 to 1 the sheet
         * *                    is between collapsed and expanded states and from -1 to 0 it is
         * *                    between hidden and collapsed states.
         */
        fun onSlide(bottomSheet: View, slideOffset: Float)
    }

    /**
     * Callback for monitoring if the [BottomSheet] is active or not. This is the same concept as the
     * Google maps app, where we might need to change the background color of the top item in the [BottomSheet]
     * once the user starts dragging it past the [BottomSheetState.STATE_COLLAPSED] state
     */
    interface OnSheetActivatedListener {
        /**
         * Called when the [BottomSheet] transitions from the [BottomSheetState.STATE_COLLAPSED] to any other state.
         *
         * @param isActive is true if the user has 'activated' the [BottomSheet] by moving it away from
         * the [BottomSheetState.STATE_COLLAPSED], and will return false the next time the [BottomSheet]
         * settles successfully at [BottomSheetState.STATE_COLLAPSED]
         */
        fun isActivated(isActive: Boolean)
    }

    //endregion

    //region INNER CLASSES
    /**
     * Runnable that handles animating the [BottomSheet] to a stable [BottomSheetState]
     */
    internal inner class SettleRunnable(private val mView: View, private val targetState: BottomSheetState) : Runnable {

        override fun run() {
            if (viewDragHelper != null && viewDragHelper!!.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this)
            } else {
                setStateInternal(targetState)
            }
        }
    }

    internal val mDragCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (state == BottomSheetState.STATE_DRAGGING) {
                return false
            }

            if (touchingScrollingChild) {
                return false
            }

            if (state == BottomSheetState.STATE_EXPANDED && activePointerId == pointerId) {
                val scroll = nestedScrollingChildRef?.get()
                if (scroll != null && ViewCompat.canScrollVertically(scroll, -1)) {
                    // Let the content scroll up
                    return false
                }
            }

            return viewRef != null && viewRef!!.get() === child
        }

        override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
            dispatchOnSlide(top)
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(BottomSheetState.STATE_DRAGGING)
            }
        }

        // todo rewrite all of this
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val top: Int
            val targetState: BottomSheetState
            if (yvel < 0) { // Moving up
                trace("view released while moving up")
                top = minOffset
                targetState = BottomSheetState.STATE_EXPANDED
            } else if (isHideable && shouldHide(releasedChild, yvel)) {
                trace("view released while we should hide the child view")
                top = parentHeight
                targetState = BottomSheetState.STATE_HIDDEN
            } else if (yvel == 0f) {
                trace("view released while not moving")
                val currentTop = releasedChild.top
                trace("abs(%s - %s) < abs(%s - %s) = %s", currentTop, minOffset, currentTop, maxOffset, Math.abs(currentTop - minOffset) < Math.abs(currentTop - maxOffset))
                if (Math.abs(currentTop - minOffset) < Math.abs(currentTop - maxOffset)) {
                    top = minOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                } else {
                    top = maxOffset
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
            } else {
                trace("view released while moving down")
                top = maxOffset
                targetState = BottomSheetState.STATE_COLLAPSED
            }
            if (viewDragHelper!!.settleCapturedViewAt(releasedChild.left, top)) {
                trace("settling captured view")
                setStateInternal(BottomSheetState.STATE_SETTLING)
                ViewCompat.postOnAnimation(releasedChild, SettleRunnable(releasedChild, targetState))
            } else {
                trace("setting captured view state without settling")
                ConsoleLogger(0).debug("")
                setStateInternal(targetState)
            }
        }

        override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
            return constrain(top, minOffset, if (isHideable) parentHeight else maxOffset)
        }

        internal fun constrain(amount: Int, low: Int, high: Int): Int {
            return if (amount < low) low else if (amount > high) high else amount
        }

        override fun clampViewPositionHorizontal(child: View?, left: Int, dx: Int): Int {
            return child!!.left
        }

        override fun getViewVerticalDragRange(child: View?): Int {
            if (isHideable) {
                return parentHeight - minOffset
            } else {
                return maxOffset - minOffset
            }
        }
    }
    //endregion

    //region MEMBER CALLBACKS

    protected var stateCallbacks: MutableList<BottomSheetStateCallback>? = CopyOnWriteArrayList()

    /**
     * Sets a callback to be notified of bottom sheet events.

     * @param callback The callback to notify when bottom sheet events occur.
     */
    fun addBottomSheetStateCallback(callback: BottomSheetStateCallback) {
        this.stateCallbacks!!.add(callback)
    }

    protected var slideCallbacks: MutableList<BottomSheetSlideCallback>? = CopyOnWriteArrayList()

    /**
     * Sets a callback to be notified of bottom sheet events.

     * @param callback The callback to notify when bottom sheet events occur.
     */
    fun addBottomSheetSlideCallback(callback: BottomSheetSlideCallback) {
        this.slideCallbacks!!.add(callback)
    }

    var activeCallback: OnSheetActivatedListener? = null

    protected var viewDragHelper: ViewDragHelper? = null

    protected var velocityTracker: VelocityTracker? = null

    protected lateinit var gestureDetectorCompat: GestureDetectorCompat

    //endregion

    //region STATE TRACKING

    // starting off the screen so we can animate the move up
    open var state = BottomSheetState.STATE_HIDDEN
        get() = field

        /**
         * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
         * animation.

         * @param value One of [.STATE_COLLAPSED], [.STATE_EXPANDED], or [.STATE_HIDDEN].
         */
        set(value) {
            trace("var state = $value, was $field")

            if (value == this.state) {
                return
            }

            if (isStateStable(value)) {
                field = value
                this.lastStableState = value
            } else {
                return
            }

            if (viewRef == null) {
                // The view is not laid out yet; modify state and let onLayoutChild handle it later
                trace("view not laid out yet")
                return
            }

            val child = viewRef!!.get()
            if (child == null) {
                trace("viewRef returned null")
                return
            }

            attemptToActivateBottomsheet(child)

            // Start the animation; wait until a pending layout if there is one.
            val parent = child.parent
            if (parent != null && parent.isLayoutRequested && ViewCompat.isAttachedToWindow(child)) {
                child.post { startSettlingAnimation(child, value) }
            } else {
                startSettlingAnimation(child, value)
            }
        }

    internal var lastStableState = BottomSheetState.STATE_HIDDEN

    protected var touchingScrollingChild: Boolean = false

    protected var activePointerId: Int = 0

    protected var initialY: Int = 0

    protected var height: Int = 0

    protected var bottomSheetIsActive: Boolean = false

    // tracks velocity from $CALL_SITE
    internal val yVelocity: Float
        get() {
            velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity)
            return VelocityTrackerCompat.getYVelocity(velocityTracker, activePointerId)
        }

    val isStable: Boolean
        get() = isStateStable(state)

    internal open fun setStateInternal(state: BottomSheetState) {
        if (this.state == state) {
            return
        }

        this.state = state

        // only send stable states, post it to the views message queue
        if (isStateStable(state)) {
            val bottomSheet = viewRef!!.get()
            if (bottomSheet != null && stateCallbacks != null) {
                (bottomSheet.parent as View).post {
                    for (callback in stateCallbacks!!) {
                        callback.onStateChanged(bottomSheet, state)
                    }
                }
            }
        }
    }

    internal fun reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }

    //endregion

    //region HEIGHT DEFINITIONS

    protected var peekHeightAuto: Boolean = false
    protected var peekHeightMin: Int = 0

    var peekHeight: Int = 0
        /**
         * Gets the height of the bottom sheet when it is collapsed.

         * @return The height of the collapsed bottom sheet in pixels, or [.PEEK_HEIGHT_AUTO]
         * * if the sheet is configured to peek automatically at 16:9 ratio keyline
         * *
         * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
         */
        get() = if (peekHeightAuto) PEEK_HEIGHT_AUTO else field

        /**
         * Sets the height of the bottom sheet when it is collapsed.

         * @param value The height of the collapsed bottom sheet in pixels, or
         * *                   [.PEEK_HEIGHT_AUTO] to configure the sheet to peek automatically
         * *                   at 16:9 ratio keyline.
         * *
         * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
         */
        set(value) {
            var layout = false
            if (value == PEEK_HEIGHT_AUTO) {
                if (!peekHeightAuto) {
                    peekHeightAuto = true
                    field = PEEK_HEIGHT_AUTO
                    layout = true
                }
            } else {
                peekHeightAuto = false
                field = Math.max(0, value)
                maxOffset = parentHeight - value
                layout = true
            }

            if (layout && state == BottomSheetState.STATE_COLLAPSED && viewRef != null) {
                val view = viewRef!!.get()
                view?.requestLayout()
            }
        }

    var anchorPoint: Int = 0

    protected var lastNestedScrollDy: Int = 0

    protected var nestedScrolled: Boolean = false
    var parentHeight: Int = 0

    var minOffset: Int = 0
    var maxOffset: Int = 0

    //endregion

    //region BEHAVIOR OPTIONS

    /**
     * Whether this bottom sheet can hide when it is swiped down.
     *
     * set true to make this bottom sheet hideable.
     *
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    var isHideable: Boolean = false

    /**
     * Whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * set true if the bottom sheet should skip the collapsed state.
     *
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    var skipCollapsed: Boolean = false

    protected var maximumVelocity: Float = 0.toFloat()

    protected var ignoreEvents: Boolean = false

    //endregion

    //region VIEW REFERENCES
    protected var viewRef: WeakReference<V>? = null

    protected var nestedScrollingChildRef: WeakReference<View>? = null

    protected var shouldScrollWithView = SparseIntArray()
    //endregion

    //region OBJECT CREATION
    /**
     * Default constructor for instantiating BottomSheetBehaviors.
     */
    constructor()

    /**
     * Default constructor for inflating BottomSheetBehaviors from layout.

     * @param context The [Context].
     * *
     * @param attrs   The [AttributeSet].
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        height = context.resources.displayMetrics.heightPixels

        var a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.BottomSheetBehavior_Layout)
        val value = a.peekValue(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight)
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            peekHeight = value.data
        } else {
            peekHeight = a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO)
        }

        isHideable = a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false)
        skipCollapsed = a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false)

        a = context.obtainStyledAttributes(attrs, R.styleable.AnchorPointBottomSheetBehavior)
        anchorPoint = a.getDimension(R.styleable.AnchorPointBottomSheetBehavior_anchorPoint, ANCHOR_POINT_AUTO.toFloat()).toInt()
        a.recycle()

        val configuration = ViewConfiguration.get(context)
        maximumVelocity = configuration.scaledMaximumFlingVelocity.toFloat()

        gestureDetectorCompat = GestureDetectorCompat(context, GestureDetectors.OnSingleTapUp({ handleOnSingleTapUp(it) }))

        info("height: %s, width: %s, anchorPoint: %s, peekHeight: %s, minOffset: %s, maxOffset: %s", Resources.getSystem().displayMetrics.heightPixels, Resources.getSystem().displayMetrics.widthPixels, anchorPoint, peekHeight, minOffset, maxOffset)
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout?, child: V?): Parcelable {
        return SavedState(state)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout?, child: V?, state: Parcelable?) {
        // todo restore the position of the screen the sheet was on, and the state that we should animate too
        val ss = state as SavedState
        // Intermediate states are restored as collapsed state
        if (ss.bottomSheetState == BottomSheetState.STATE_DRAGGING || ss.bottomSheetState == BottomSheetState.STATE_SETTLING) {
            this.state = BottomSheetState.STATE_COLLAPSED
        } else {
            this.state = ss.bottomSheetState
        }

        lastStableState = this.state
    }
    //endregion

    //region LAMBDAS
    /**
     * [GestureDetectors] implementation that handles a single tap on to handle expanding the [BottomSheet]
     */
    open fun handleOnSingleTapUp(e: MotionEvent): Boolean {
        if (state == BottomSheetState.STATE_COLLAPSED) {
            if (viewRef!!.get() != null) {
                (viewRef!!.get() as BottomSheet).isActivated = true
                bottomSheetIsActive = true
            }
            state = BottomSheetState.STATE_ANCHOR_POINT
            return true
        }

        return false
    }
    //endregion

    //region VIEW HELPER METHODS
    internal fun recursivelyCheckIfDescendedFrom(decedent: View, ancestor: ViewGroup): Boolean {
        while (!targetViewIsChildOf(decedent, ancestor)) {
            if (decedent.parent is CoordinatorLayout) return false

            targetViewIsChildOf(decedent.parent as View, ancestor)
        }

        return true
    }

    internal fun targetViewIsChildOf(potentialChild: View, potentialParent: ViewGroup): Boolean {
        return potentialChild.parent == potentialParent
    }

    internal fun findScrollingChild(view: View): View? {
        if (view is NestedScrollingChild) {
            return view
        }

        if (view is ViewGroup) {
            val group = view
            var i = 0
            val count = group.childCount
            while (i < count) {
                val scrollingChild = findScrollingChild(group.getChildAt(i))
                if (scrollingChild != null) {
                    return scrollingChild
                }
                i++
            }
        }

        return null
    }
    //endregion

    //region BEHAVIOR CALLBACKS
    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            ViewCompat.setFitsSystemWindows(child, true)
        }

        val savedTop = child.top
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection)
        // Offset the bottom sheet
        parentHeight = parent.height

        val peekHeight: Int
        if (peekHeightAuto) {
            if (peekHeightMin == 0) {
                peekHeightMin = parent.resources.getDimensionPixelSize(android.support.design.R.dimen.design_bottom_sheet_peek_height_min)
            }
            peekHeight = Math.max(peekHeightMin, parentHeight - parent.width * 9 / 16)
        } else {
            peekHeight = this.peekHeight
        }

        minOffset = Math.max(0, parentHeight - child.height)
        maxOffset = Math.max(parentHeight - peekHeight, minOffset)

        if (state == BottomSheetState.STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, minOffset)
        } else if (isHideable && state == BottomSheetState.STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, parentHeight)
        } else if (state == BottomSheetState.STATE_ANCHOR_POINT) {
            ViewCompat.offsetTopAndBottom(child, anchorPoint)
        } else if (state == BottomSheetState.STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, maxOffset)
        } else if (state == BottomSheetState.STATE_DRAGGING || state == BottomSheetState.STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.top)
        }

        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, mDragCallback)
        }

        viewRef = WeakReference(child)
        nestedScrollingChildRef = WeakReference<View>(findScrollingChild(child))
        return true
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        // send this event to the GestureDetector here so we can react to an event without subscribing to updates
        if (event.rawY > height - peekHeight && state == BottomSheetState.STATE_COLLAPSED) {
            gestureDetectorCompat.onTouchEvent(event)
        }

        if (!child.isShown) {
            ignoreEvents = true
            return false
        }
        val action = MotionEventCompat.getActionMasked(event)
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchingScrollingChild = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false
                    return false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val initialX = event.x.toInt()
                initialY = event.y.toInt()

                if (state == BottomSheetState.STATE_ANCHOR_POINT) {
                    activePointerId = event.getPointerId(event.actionIndex)
                    touchingScrollingChild = true
                } else {
                    val scroll = nestedScrollingChildRef?.get()
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = event.getPointerId(event.actionIndex)
                        touchingScrollingChild = true
                    }
                }

                ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID && !parent.isPointInChildBounds(child, initialX, initialY)
            }
        }
        if (!ignoreEvents && viewDragHelper!!.shouldInterceptTouchEvent(event)) {
            return true
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        val scroll = nestedScrollingChildRef?.get()
        return action == MotionEvent.ACTION_MOVE && scroll != null &&
                !ignoreEvents && state != BottomSheetState.STATE_DRAGGING &&
                !parent.isPointInChildBounds(scroll, event.x.toInt(), event.y.toInt()) &&
                Math.abs(initialY - event.y) > viewDragHelper!!.touchSlop
    }

    override fun onTouchEvent(parent: CoordinatorLayout?, child: V?, event: MotionEvent?): Boolean {
        if (!child!!.isShown) {
            return false
        }
        val action = MotionEventCompat.getActionMasked(event!!)
        if (state == BottomSheetState.STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true
        }
        viewDragHelper!!.processTouchEvent(event)
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)
        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the bottom sheet in case it is not captured and the touch slop is passed.
        if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            if (Math.abs(initialY - event.y) > viewDragHelper!!.touchSlop) {
                viewDragHelper!!.captureChildView(child, event.getPointerId(event.actionIndex))
            }
        }
        return !ignoreEvents
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout?, child: V?, directTargetChild: View?, target: View?, nestedScrollAxes: Int): Boolean {
        if (shouldScrollWithView.get(child!!.hashCode() + directTargetChild!!.hashCode(), -1) != -1) {
            return shouldScrollWithView.get(child.hashCode() + directTargetChild.hashCode()) == 1
        }

        lastNestedScrollDy = 0
        nestedScrolled = false

        // directTargetChild is the direct parent of the NestedScrollingChild that got us here
        var directTargetChildDescendsFromChild = false
        val directTargetChildIsChild = directTargetChild == child
        val verticalNestedScroll = nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0

        if (child is ViewGroup) {
            directTargetChildDescendsFromChild = recursivelyCheckIfDescendedFrom(directTargetChild, child as ViewGroup)
        }

        // only handle scrolls for children of the Child that scroll vertically
        // the Child is what gets the Behavior attached to it, we don't want to scroll for different parents
        val shouldScroll = (directTargetChildDescendsFromChild || directTargetChildIsChild) && verticalNestedScroll
        shouldScrollWithView.put(child.hashCode() + directTargetChild.hashCode(), if (shouldScroll) 1 else 0)
        return shouldScroll
    }

    override fun onNestedScrollAccepted(coordinatorLayout: CoordinatorLayout?, child: V?, directTargetChild: View?, target: View?, nestedScrollAxes: Int) {
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes)
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View?, dx: Int, dy: Int, consumed: IntArray) {
        attemptToActivateBottomsheet(child as View)

        val scrollingChild = nestedScrollingChildRef?.get()
        if (target !== scrollingChild) {
            return
        }

        val currentTop = child.top
        val newTop = currentTop - dy

        // Force stop at the anchor - do not go from collapsed to expanded in one scroll
        if (lastStableState == BottomSheetState.STATE_COLLAPSED && newTop < anchorPoint || lastStableState == BottomSheetState.STATE_EXPANDED && newTop > anchorPoint) {

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

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout?, child: V?, target: View?, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View) {
        if (child.top == minOffset) {
            setStateInternal(BottomSheetState.STATE_EXPANDED)
            lastStableState = BottomSheetState.STATE_EXPANDED
            return
        }

        if (child.top == peekHeight) {
            setStateInternal(BottomSheetState.STATE_COLLAPSED)
            lastStableState = BottomSheetState.STATE_COLLAPSED
            return
        }

        if (target !== nestedScrollingChildRef?.get() || !nestedScrolled) {
            return
        }

        val top: Int
        val targetState: BottomSheetState

        // last nested scroll is the raw values in pixels of the last drag event
        // if will be negative if the user swiped down and positive if they swiped up
        val percentage = lastNestedScrollDy.toFloat() / height

        // attempt to snap to the right state with the current y velocity, but fall back to the
        // last movement by percentage of the screen
        if (yVelocity < -150) {
            // snap up
            trace("velocity %s snapping up", yVelocity)
            targetState = getNextStableState(lastStableState)
        } else if (yVelocity > 150) {
            //snap down
            trace("velocity %s snapping down", yVelocity)
            targetState = getPreviousStableState(lastStableState)
        } else {
            if (percentage > 0.01) {
                // snap up
                trace("percentage moved %s snapping up", percentage)
                targetState = getNextStableState(lastStableState)
            } else if (percentage < -0.01) {
                //snap down
                trace("percentage moved %s snapping down", percentage)
                targetState = getPreviousStableState(lastStableState)
            } else {
                // eventually fall all the way back to the last state if velocity is 0 and the
                // touch event only moved a small amount
                trace("snapping to last stable state")
                targetState = lastStableState
            }
        }

        top = getTopForState(targetState)
        this.lastStableState = targetState
        if (viewDragHelper!!.smoothSlideViewTo(child, child.left, top)) {
            trace("smoothSlideViewTo %s to %s with state %s", child.javaClass.simpleName, top, targetState)
            setStateInternal(BottomSheetState.STATE_SETTLING)
            ViewCompat.postOnAnimation(child, SettleRunnable(child, targetState))
        } else {
            setStateInternal(targetState)
        }

        nestedScrolled = false
    }

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout?, child: V?, target: View?, velocityX: Float, velocityY: Float): Boolean {
        trace("override fun onNestedPreFling($coordinatorLayout: CoordinatorLayout?, $child: V?, $target: View?, $velocityX: Float, $velocityY: Float): Boolean")
        return target === nestedScrollingChildRef?.get() && (state != BottomSheetState.STATE_EXPANDED || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY))
    }

    override fun onNestedFling(coordinatorLayout: CoordinatorLayout?, child: V?, target: View?, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
    }
    //endregion

    //region BOTTOMSHEET SPECIFIC METHODS
    /**
     * Takes a [State] and returns the next stable state as the bottom sheet expands

     * @param currentState the last stable state of the bottom sheet
     * *
     * @return the next stable state that the sheet should settle at
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal open fun getNextStableState(currentState: BottomSheetState): BottomSheetState {
        when (currentState) {
            BottomSheetState.STATE_HIDDEN -> return BottomSheetState.STATE_COLLAPSED
            BottomSheetState.STATE_COLLAPSED -> return BottomSheetState.STATE_ANCHOR_POINT
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
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal open fun getPreviousStableState(currentState: BottomSheetState): BottomSheetState {
        when (currentState) {
            BottomSheetState.STATE_EXPANDED -> return BottomSheetState.STATE_ANCHOR_POINT
            BottomSheetState.STATE_ANCHOR_POINT -> return BottomSheetState.STATE_COLLAPSED
            BottomSheetState.STATE_COLLAPSED -> return BottomSheetState.STATE_HIDDEN
            else -> return BottomSheetState.STATE_HIDDEN
        }
    }

    internal fun isStateStable(currentState: BottomSheetState): Boolean {
        when (currentState) {
            BottomSheetState.STATE_ANCHOR_POINT, BottomSheetState.STATE_COLLAPSED, BottomSheetState.STATE_EXPANDED, BottomSheetState.STATE_HIDDEN -> return true
            else -> return false
        }
    }

    /**
     * Returns a measured y position that the top of the bottom sheet should settle at

     * @param state the [State] that the sheet is going to settle at
     * *
     * @return the y position that the top of the sheet will be at once it is done settling
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal open fun getTopForState(state: BottomSheetState): Int {
        when (state) {
            BottomSheetState.STATE_HIDDEN -> {
                debug("STATE_HIDDEN top: %s", parentHeight)
                return parentHeight
            }
            BottomSheetState.STATE_COLLAPSED -> {
                debug("STATE_COLLAPSED top: %s", maxOffset)
                return maxOffset
            }
            BottomSheetState.STATE_ANCHOR_POINT -> {
                debug("STATE_ANCHOR_POINT top: %s", anchorPoint)
                return anchorPoint
            }
            BottomSheetState.STATE_EXPANDED -> {
                debug("STATE_EXPANDED top: %s", minOffset)
                return minOffset
            }
            else -> {
                debug("UNKNOWN_STATE top: %s", 0)
                return 0
            }
        }
    }

    protected fun attemptToActivateBottomsheet(view: View) {
        if (view is BottomSheet) {
            activateBottomsheetIfTopAbovePeekHeight(view)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    protected fun activateBottomsheetIfTopAbovePeekHeight(bottomSheet: BottomSheet) {
        val shouldActivate = bottomSheet.top < height - peekHeight || state != BottomSheetState.STATE_COLLAPSED

        if (shouldActivate == bottomSheetIsActive) return

        if (shouldActivate) {
            bottomSheet.isActivated = true
            bottomSheetIsActive = true
        } else {
            bottomSheet.isActivated = false
            bottomSheetIsActive = false
        }
    }

    // based on current velocity, could be refactored to handle moving between states
    internal fun shouldHide(child: View, yvel: Float): Boolean {
        if (skipCollapsed) {
            return true
        }

        if (child.top < maxOffset) {
            // It should not hide, but collapse.
            return false
        }

        val newTop = child.top + yvel * HIDE_FRICTION
        return Math.abs(newTop - maxOffset) / peekHeight.toFloat() > HIDE_THRESHOLD
    }

    internal open fun startSettlingAnimation(child: View, state: BottomSheetState) {
        val top: Int
        if (state == BottomSheetState.STATE_COLLAPSED) {
            top = maxOffset
        } else if (state == BottomSheetState.STATE_ANCHOR_POINT) {
            top = anchorPoint
        } else if (state == BottomSheetState.STATE_EXPANDED) {
            top = minOffset
        } else if (isHideable && state == BottomSheetState.STATE_HIDDEN) {
            top = parentHeight
        } else {
            throw IllegalArgumentException("Illegal state argument: " + state)
        }

        if (viewDragHelper!!.smoothSlideViewTo(child, child.left, top)) {
            setStateInternal(BottomSheetState.STATE_SETTLING)
            ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
        } else {
            // we din't need to slide the view, so we aren't settling
            setStateInternal(state)
        }
    }

    internal fun dispatchOnSlide(top: Int) {
        val bottomSheet = viewRef!!.get()
        if (bottomSheet != null && slideCallbacks != null) {
            if (top > maxOffset) {
                for (callback in slideCallbacks!!) {
                    callback.onSlide(bottomSheet, (maxOffset - top).toFloat() / (parentHeight - maxOffset))
                }
            } else {
                for (callback in slideCallbacks!!) {
                    callback.onSlide(bottomSheet, (maxOffset - top).toFloat() / (maxOffset - minOffset))
                }
            }
        }
    }
    //endregion

    //region CONSTANTS AND STATIC METHOD TO OBTAIN INSTANCE
    companion object {

        internal val ANCHOR_POINT_AUTO = 700

        /**
         * Peek at the 16:9 ratio keyline of its parent.

         * This can be used as a parameter for [.setPeekHeight].
         * [.getPeekHeight] will return this when the value is set.
         */
        val PEEK_HEIGHT_AUTO = -1

        internal val HIDE_THRESHOLD = 0.5f

        internal val HIDE_FRICTION = 0.1f

        /**
         * A utility function to get the [BottomSheetBehavior] associated with the `view`.

         * @param view The [View] with [BottomSheetBehavior].
         * *
         * @return The [BottomSheetBehavior] associated with the `view`.
         */
        fun <V : View> from(view: V): AnchorPointBottomSheetBehavior<*>? {
            val params = view.layoutParams as? CoordinatorLayout.LayoutParams ?: return null

            val behavior = params.behavior as? AnchorPointBottomSheetBehavior<*> ?: return null

            return behavior
        }
    }
    //endregion
}

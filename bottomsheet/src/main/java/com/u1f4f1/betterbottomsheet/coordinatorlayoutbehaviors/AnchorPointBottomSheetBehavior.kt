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
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import com.kylealanr.gesturedetectors.GestureDetectors
import com.u1f4f1.betterbottomsheet.*
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheet
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetState
import com.u1f4f1.betterbottomsheet.bottomsheet.SavedState
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An interaction behavior plugin for a child view of [CoordinatorLayout] to make it work as
 * a bottom sheet.
 */
@SuppressLint("PrivateResource")
open class AnchorPointBottomSheetBehavior<V : View> : CoordinatorLayout.Behavior<V> {

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

    protected open var lastStableState = BottomSheetState.STATE_HIDDEN
    protected val stateCallbacks: MutableList<BottomSheetStateCallback> = CopyOnWriteArrayList()
    protected val slideCallbacks: MutableList<BottomSheetSlideCallback> = CopyOnWriteArrayList()

    protected var shouldScrollWithView: MutableMap<Int, Int> = ConcurrentHashMap()
    protected var bottomSheetIsActive: Boolean = false

    protected lateinit var gestureDetectorCompat: GestureDetectorCompat

    protected var height: Int = 0

    var velocityTracker: VelocityTracker? = null
    var state = BottomSheetState.STATE_HIDDEN


    var peekHeight: Int = 0
    var anchorPosition: Int = 0
    protected var activePointerId = MotionEvent.INVALID_POINTER_ID

    lateinit var viewRef: WeakReference<View>
    lateinit var nestedScrollingChildRef: WeakReference<View?>
    var skipAnchorPoint = false
    var skipCollapsed = false
    var draggable = true
    var viewDragHelper: ViewDragHelper? = null
    var isHideable = false
    var ignoreEvents = false
    var initialY = 0
    var touchingScrollingChild = false
    var lastNestedScrollDy = 0
    var nestedScrolled = false
    val dragCallback = DragCallback()
    var maxOffset = 0
    var minOffset = 0
    var parentHeight = 0
    var anchorPoint = 0

    constructor()

    constructor(context: Context, attrs: AttributeSet?) {
        height = context.resources.displayMetrics.heightPixels

        var a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.BottomSheetBehavior_Layout)

        peekHeight = a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, 0)
        isHideable = a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false)
        skipCollapsed = a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false)

        a = context.obtainStyledAttributes(attrs, R.styleable.AnchorPointBottomSheetBehavior)
        anchorPoint = a.getDimension(R.styleable.AnchorPointBottomSheetBehavior_anchorPoint, ANCHOR_POINT_AUTO.toFloat()).toInt()
        a.recycle()

        gestureDetectorCompat = GestureDetectorCompat(context, GestureDetectors.OnSingleTapUp({ handleOnSingleTapUp(it) }))

        info("height: %s, width: %s, anchorPoint: %s, peekHeight: %s, minOffset: %s, maxOffset: %s", Resources.getSystem().displayMetrics.heightPixels, Resources.getSystem().displayMetrics.widthPixels, anchorPoint, peekHeight, minOffset, maxOffset)
    }

    protected open fun handleOnSingleTapUp(e: MotionEvent): Boolean {
        if (state == BottomSheetState.STATE_COLLAPSED) {
            if (viewRef.get() != null) {
                (viewRef.get() as BottomSheet).isActive = true
                bottomSheetIsActive = true
            }
            state = BottomSheetState.STATE_ANCHOR_POINT
            return true
        }

        return false
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout?, child: V?): Parcelable {
        return SavedState(state)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout?, child: V?, state: Parcelable?) {
        val ss = state as SavedState
        // Intermediate states are restored as collapsed state
        if (ss.bottomSheetState == BottomSheetState.STATE_DRAGGING || ss.bottomSheetState == BottomSheetState.STATE_SETTLING) {
            this.state = BottomSheetState.STATE_COLLAPSED
        } else {
            this.state = ss.bottomSheetState
        }

        lastStableState = this.state
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        if (state != BottomSheetState.STATE_DRAGGING && state != BottomSheetState.STATE_SETTLING) {
            if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
                ViewCompat.setFitsSystemWindows(child, true)
            }
            parent.onLayoutChild(child, layoutDirection)
        }
        parentHeight = parent.height
        minOffset = Math.max(0, parentHeight - child.height)
        maxOffset = Math.max(minOffset, parentHeight - peekHeight)
        anchorPosition = parentHeight - anchorPoint

        when (state) {
            BottomSheetState.STATE_ANCHOR_POINT ->
                ViewCompat.offsetTopAndBottom(child, parentHeight - anchorPosition)
            BottomSheetState.STATE_EXPANDED ->
                ViewCompat.offsetTopAndBottom(child, minOffset)
            BottomSheetState.STATE_HIDDEN ->
                ViewCompat.offsetTopAndBottom(child, parentHeight)
            BottomSheetState.STATE_COLLAPSED ->
                ViewCompat.offsetTopAndBottom(child, maxOffset)
            else -> { /* do nothing */ }
        }

        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCallback)
        }
        viewRef = WeakReference(child)
        val found: View? = findScrollingChild(child)
        nestedScrollingChildRef = WeakReference(found)
        return true
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        debug("onInterceptTouchEvent($parent: CoordinatorLayout, $child: V, $event: MotionEvent?)")
        // send this event to the GestureDetector here so we can react to an event without subscribing to updates
        if (event.rawY > height - peekHeight && state == BottomSheetState.STATE_COLLAPSED) {
            gestureDetectorCompat.onTouchEvent(event)
        }

        if (!draggable) {
            return false
        }
        if (!child.isShown) {
            return false
        }

        val action = MotionEventCompat.getActionMasked(event)
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchingScrollingChild = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
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
                    val scroll = nestedScrollingChildRef.get()
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = event.getPointerId(event.actionIndex)
                        touchingScrollingChild = true
                    }
                }
                ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID
                        && !parent.isPointInChildBounds(child, initialX, initialY)
            }

            else -> {
                // do nothing
            }
        }

        if (!ignoreEvents && viewDragHelper?.shouldInterceptTouchEvent(event)!!) {
            return true
        }

        val scroll = nestedScrollingChildRef.get()
        var touchSlop = 0
        viewDragHelper?.let {
            touchSlop = it.touchSlop
        }
        return action == MotionEvent.ACTION_MOVE
                && scroll != null
                && !ignoreEvents
                && state != BottomSheetState.STATE_DRAGGING
                && !parent.isPointInChildBounds(scroll, event.x.toInt(), event.y.toInt())
                && Math.abs(initialY - event.y) > touchSlop
    }

    @SuppressLint("Recycle")
    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        debug("onTouchEvent($parent: CoordinatorLayout, $child: V, $event: MotionEvent?)")
        if (!draggable) {
            return false
        }
        if (!child.isShown) {
            return false
        }
        val action = MotionEventCompat.getActionMasked(event)
        if (state == BottomSheetState.STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true
        }

        viewDragHelper?.processTouchEvent(event)
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            var touchSlop = 0
            viewDragHelper?.let {
                touchSlop = it.touchSlop
            }
            if (Math.abs(initialY - event.y) > touchSlop.toFloat()) {
                viewDragHelper?.captureChildView(child, event.getPointerId(event.actionIndex))
            }
        }
        return !ignoreEvents
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, directTargetChild: View, target: View, nestedScrollAxes: Int): Boolean {
        trace("onStartNestedScroll($coordinatorLayout: CoordinatorLayout, $child: V, $directTargetChild: View, $target: View, $nestedScrollAxes: Int)")
        if (shouldScrollWithView.getOrDefault(child.hashCode() + directTargetChild.hashCode(), -1) != -1) {
            return shouldScrollWithView[child.hashCode() + directTargetChild.hashCode()] == 1
        }

        lastNestedScrollDy = 0
        nestedScrolled = false

        // directTargetChild is the direct parent of the NestedScrollingChild that got us here
        var directTargetChildDescendsFromChild = false
        val directTargetChildIsChild = directTargetChild == child
        val verticalNestedScroll = nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0

        if (child is ViewGroup) {
            directTargetChildDescendsFromChild = recursivelyCheckIfDescendedFrom(directTargetChild, child)
        }

        // only handle scrolls for children of the Child that scroll vertically
        // the Child is what gets the Behavior attached to it, we don't want to scroll for different parents
        val shouldScroll = (directTargetChildDescendsFromChild || directTargetChildIsChild) && verticalNestedScroll
        shouldScrollWithView.put(child.hashCode() + directTargetChild.hashCode(), if (shouldScroll) 1 else 0)
        return shouldScroll
    }

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

    override fun onNestedScrollAccepted(coordinatorLayout: CoordinatorLayout?, child: V?, directTargetChild: View?, target: View?, nestedScrollAxes: Int) {
        trace("onNestedScrollAccepted($coordinatorLayout: CoordinatorLayout?, $child: V?, $directTargetChild: View?, $target: View?, $nestedScrollAxes: Int)")
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes)
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View?, dx: Int, dy: Int, consumed: IntArray) {
        trace("onNestedPreScroll($coordinatorLayout: CoordinatorLayout, $child: V, $target: View?, $dx: Int, $dy: Int, $consumed: IntArray)")

        child.let { attemptToActivateBottomsheet(child as View) }

        val scrollChild = nestedScrollingChildRef.get() ?: return
        if (target != scrollChild) {
            return
        }
        val currentTop = child.top
        val newTop = currentTop - dy
        if (dy > 0) {
            if (newTop < minOffset) {
                consumed[1] = currentTop - minOffset
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                setStateInternal(BottomSheetState.STATE_EXPANDED)
            } else {
                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                setStateInternal(BottomSheetState.STATE_DRAGGING)
            }
        } else if (dy < 0) {
            if (!ViewCompat.canScrollVertically(target, -1)) {
                if (newTop <= maxOffset || isHideable) {
                    consumed[1] = dy
                    ViewCompat.offsetTopAndBottom(child, -dy)
                    setStateInternal(BottomSheetState.STATE_DRAGGING)
                } else {
                    consumed[1] = currentTop - maxOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
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
            return
        }

        if (target != nestedScrollingChildRef.get() || !nestedScrolled) {
            return
        }

        val top: Int
        val targetState: BottomSheetState

        if (lastNestedScrollDy > 0) {
            val currentTop = child.top
            if (currentTop > parentHeight - anchorPosition) {
                if (skipAnchorPoint) {
                    top = minOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                } else {
                    top = parentHeight - anchorPosition
                    targetState = BottomSheetState.STATE_ANCHOR_POINT
                }
            } else {
                top = minOffset
                targetState = BottomSheetState.STATE_EXPANDED
            }
        } else if (isHideable && shouldHide(child, getYvelocity())) {
            top = parentHeight
            targetState = BottomSheetState.STATE_HIDDEN
        } else if (lastNestedScrollDy == 0) {
            val currentTop = child.top
            if (Math.abs(currentTop - minOffset) < Math.abs(currentTop - maxOffset)) {
                top = minOffset
                targetState = BottomSheetState.STATE_EXPANDED
            } else {
                if (skipAnchorPoint) {
                    top = minOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                } else {
                    top = maxOffset
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
            }
        } else {
            val currentTop = child.top
            if (currentTop > parentHeight - anchorPosition) {
                top = maxOffset
                targetState = BottomSheetState.STATE_COLLAPSED
            } else {
                if (skipAnchorPoint) {
                    top = maxOffset
                    targetState = BottomSheetState.STATE_COLLAPSED
                } else {
                    top = parentHeight - anchorPosition
                    targetState = BottomSheetState.STATE_ANCHOR_POINT
                }
            }
        }
        if (viewDragHelper?.smoothSlideViewTo(child, child.left, top)!!) {
            setStateInternal(BottomSheetState.STATE_SETTLING)
            ViewCompat.postOnAnimation(child, SettleRunnable(child, targetState))
        } else {
            setStateInternal(targetState)
        }
        nestedScrolled = false
    }

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
            bottomSheet.isActive = true
            bottomSheetIsActive = true
        } else {
            bottomSheet.isActive = false
            bottomSheetIsActive = false
        }
    }

    protected fun getYvelocity(): Float {
        velocityTracker?.computeCurrentVelocity(1000, 2000.0f)
        return VelocityTrackerCompat.getYVelocity(velocityTracker, activePointerId)
    }

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout?, child: V?, target: View?, velocityX: Float, velocityY: Float): Boolean {
        return target === nestedScrollingChildRef.get() && (state != BottomSheetState.STATE_EXPANDED || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY))
    }

    override fun onNestedFling(coordinatorLayout: CoordinatorLayout?, child: V?, target: View?, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
    }

    /**
     * Sets a callback to be notified of bottom sheet events.

     * @param callback The callback to notify when bottom sheet events occur.
     */
    fun addBottomSheetStateCallback(callback: BottomSheetStateCallback) {
        this.stateCallbacks.add(callback)
    }

    /**
     * Sets a callback to be notified of bottom sheet events.

     * @param callback The callback to notify when bottom sheet events occur.
     */
    fun addBottomSheetSlideCallback(callback: BottomSheetSlideCallback) {
        this.slideCallbacks.add(callback)
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
            val bottomSheet = viewRef.get()
            if (bottomSheet != null) {
                (bottomSheet.parent as View).post {
                    for (callback in stateCallbacks) {
                        callback.onStateChanged(bottomSheet, state)
                    }
                }
            }
        }
    }

    internal fun reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER
        velocityTracker?.let {
            it.recycle()
            velocityTracker = null
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

        setStateInternal(BottomSheetState.STATE_SETTLING)
        if (viewDragHelper!!.smoothSlideViewTo(child, child.left, top)) {
            ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
        }
    }

    inner class DragCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
            if (state == BottomSheetState.STATE_DRAGGING) {
                return false
            }
            if (touchingScrollingChild) {
                return false
            }
            if (state == BottomSheetState.STATE_EXPANDED && activePointerId == pointerId) {
                val scroll = nestedScrollingChildRef.get()
                if (scroll != null && ViewCompat.canScrollVertically(scroll, -1)) {
                    return false
                }
            }
            return viewRef.get() != null
        }

        override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
            dispatchOnSlide(top)
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(BottomSheetState.STATE_DRAGGING)
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val targetState : BottomSheetState
            val top : Int
            if (yvel < 0) {
                val currentTop = releasedChild.top
                if (Math.abs(currentTop - minOffset) < Math.abs(
                        currentTop - parentHeight + anchorPosition)) {
                    top = minOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                } else {
                    top = parentHeight - anchorPosition
                    targetState = BottomSheetState.STATE_ANCHOR_POINT
                }
            } else if (isHideable && shouldHide(releasedChild, yvel)) {
                top = parentHeight
                targetState = BottomSheetState.STATE_HIDDEN
            } else if (yvel == 0.0f) {
                val currentTop = releasedChild.top
                if (Math.abs(currentTop - minOffset) < Math.abs(
                        currentTop - parentHeight + anchorPosition)) {
                    top = minOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                } else if (Math.abs(currentTop - parentHeight + anchorPosition) < Math.abs(
                        currentTop - maxOffset)) {
                    if (skipAnchorPoint) {
                        top = maxOffset
                        targetState = BottomSheetState.STATE_COLLAPSED
                    } else {
                        top = parentHeight - anchorPosition
                        targetState = BottomSheetState.STATE_ANCHOR_POINT
                    }
                } else {
                    top = maxOffset
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
            } else {
                val currentTop = releasedChild.top
                if (Math.abs(currentTop - parentHeight + anchorPosition) < Math.abs(
                        currentTop - maxOffset)) {
                    if (skipAnchorPoint) {
                        top = maxOffset
                        targetState = BottomSheetState.STATE_COLLAPSED
                    } else {
                        top = parentHeight - anchorPosition
                        targetState = BottomSheetState.STATE_ANCHOR_POINT
                    }
                } else {
                    top = maxOffset
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
            }
            val settleCaptureViewAt = viewDragHelper?.settleCapturedViewAt(releasedChild.left, top)!!
            if (settleCaptureViewAt) {
                setStateInternal(BottomSheetState.STATE_SETTLING)
                ViewCompat.postOnAnimation(releasedChild, SettleRunnable(releasedChild, targetState))
            } else {
                setStateInternal(targetState)
            }
        }

        override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
            val offset = if (isHideable) {
                parentHeight
            } else {
                maxOffset
            }
            return constrain(top, minOffset, offset)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int = child.left

        private fun constrain(amount: Int, low: Int, high: Int): Int = if (amount < low) {
            low
        } else if (amount > high) {
            high
        } else {
            amount
        }

        override fun getViewVerticalDragRange(child: View?): Int = if (isHideable) {
            parentHeight - minOffset
        } else {
            maxOffset - minOffset
        }
    }

    internal fun dispatchOnSlide(top: Int) {
        val bottomSheet = viewRef.get()
        trace("dispatchOnSlide(%s: Int)", top)

        if (bottomSheet != null) {
            if (top > maxOffset) {
                for (callback in slideCallbacks) {
                    callback.onSlide(bottomSheet, (maxOffset - top).toFloat() / (parentHeight - maxOffset))
                    trace("callback.onSlide(bottomSheet, %s / %s)", (maxOffset - top).toFloat(), (parentHeight - maxOffset))
                }
            } else {
                for (callback in slideCallbacks) {
                    callback.onSlide(bottomSheet, (maxOffset - top).toFloat() / (maxOffset - minOffset))
                    trace("callback.onSlide(bottomSheet, %s / %s)", (maxOffset - top).toFloat(), (maxOffset - minOffset))
                }
            }
        }
    }

    protected inner class SettleRunnable(private val mView: View, private val targetState: BottomSheetState) : Runnable {

        override fun run() {
            if (viewDragHelper != null && viewDragHelper!!.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this)
            } else {
                setStateInternal(targetState)
            }
        }
    }

    companion object {

        internal val ANCHOR_POINT_AUTO = 700

        internal val HIDE_THRESHOLD = 0.5f

        internal val HIDE_FRICTION = 0.1f

        /**
         * A utility function to get the [BottomSheetBehavior] associated with the `view`.

         * @param view The [View] with [BottomSheetBehavior].
         * *
         * @return The [BottomSheetBehavior] associated with the `view`.
         */
        fun <V : View> from(view: V): AnchorPointBottomSheetBehavior<V>? {
            trace("fun <V : View> from($view: V): AnchorPointBottomSheetBehavior<V>? ")

            val params = view.layoutParams
            if (params !is CoordinatorLayout.LayoutParams) {
                warn("The view is not a child of CoordinatorLayout")
                return null
            }

            val behavior = params.behavior
            if (behavior !is AnchorPointBottomSheetBehavior<*>) {
                warn("The view is not associated with AnchorPointBottomSheetBehavior")
                return null
            }

            debug("return $behavior as AnchorPointBottomSheetBehavior<V>?")
            @Suppress("UNCHECKED_CAST")
            return behavior as AnchorPointBottomSheetBehavior<V>?
        }
    }
}

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

@file:Suppress("unused")

package com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import com.kylealanr.gesturedetectors.GestureDetectors
import com.u1f4f1.powerbottomsheet.R
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetState
import java.lang.ref.WeakReference

/**
 * An interaction behavior plugin for a child view of [CoordinatorLayout] to make it work as
 * a bottom sheet.
 */
class StandardBottomSheetBehavior<V : View> : AnchorPointBottomSheetBehavior<V> {

    /**
     * Default constructor for inflating BottomSheetBehaviors from layout.

     * @param context The [Context].
     * *
     * @param attrs   The [AttributeSet].
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.BottomSheetBehavior_Layout)
        peekHeight = a.getDimensionPixelSize(
                R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, 0)

        isHideable = a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false)
        skipCollapsed = a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false)

        a.recycle()

        gestureDetectorCompat = GestureDetectorCompat(context,
                GestureDetectors.OnSingleTapUp{ this.handleOnSingleTapUp(it) })
    }

    override fun handleOnSingleTapUp(e: MotionEvent): Boolean {
        if (state == BottomSheetState.STATE_COLLAPSED) {
            if (viewRef?.get() != null) {
                bottomSheetIsActive = true
            }
            state = BottomSheetState.STATE_EXPANDED
            return true
        }

        return false
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.setFitsSystemWindows(true)
        }
        val savedTop = child.top
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection)
        // Offset the bottom sheet
        parentHeight = parent.height
        val peekHeight: Int = this.peekHeight
        minOffset = Math.max(0, parentHeight - child.height)
        maxOffset = Math.max(parentHeight - peekHeight, minOffset)
        if (state == BottomSheetState.STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, minOffset)
        } else if (isHideable && state == BottomSheetState.STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, parentHeight)
        } else if (state == BottomSheetState.STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, maxOffset)
        } else if (state == BottomSheetState.STATE_DRAGGING || state == BottomSheetState.STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.top)
        }
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCalback)
        }
        viewRef = WeakReference(child)
        nestedScrollingChildRef = WeakReference(findScrollingChild(child)!!)
        return true
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        // send this event to the GestureDetector here so we can react to an event without subscribing to updates
        if (event.rawY > parent.height - peekHeight && state == BottomSheetState.STATE_COLLAPSED) {
            gestureDetectorCompat.onTouchEvent(event)
        }

        if (!child.isShown) {
            ignoreEvents = true
            return false
        }
        val action = event.actionMasked
        // Record the velocity
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
                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false
                    return false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val initialX = event.x.toInt()
                initialY = event.y.toInt()
                val scroll = nestedScrollingChildRef?.get()
                if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                    activePointerId = event.getPointerId(event.actionIndex)
                    touchingScrollingChild = true
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

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        attemptToActivateBottomsheet(child)

        val scrollingChild = nestedScrollingChildRef?.get()
        if (target !== scrollingChild) {
            return
        }
        val currentTop = child.top
        val newTop = currentTop - dy
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
            if (!target.canScrollVertically(-1)) {
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

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, type: Int) {
        if (child.top == minOffset) {
            setStateInternal(BottomSheetState.STATE_EXPANDED)
            return
        }
        if (target !== nestedScrollingChildRef!!.get() || !nestedScrolled) {
            return
        }
        val top: Int
        val targetState: BottomSheetState
        if (lastNestedScrollDy > 0) {
            top = minOffset
            targetState = BottomSheetState.STATE_EXPANDED
        } else if (isHideable && shouldHide(child, yVelocity)) {
            top = parentHeight
            targetState = BottomSheetState.STATE_HIDDEN
        } else if (lastNestedScrollDy == 0) {
            val currentTop = child.top
            if (Math.abs(currentTop - minOffset) < Math.abs(currentTop - maxOffset)) {
                top = minOffset
                targetState = BottomSheetState.STATE_EXPANDED
            } else {
                top = maxOffset
                targetState = BottomSheetState.STATE_COLLAPSED
            }
        } else {
            top = maxOffset
            targetState = BottomSheetState.STATE_COLLAPSED
        }
        if (viewDragHelper!!.smoothSlideViewTo(child, child.left, top)) {
            setStateInternal(BottomSheetState.STATE_SETTLING)
            ViewCompat.postOnAnimation(child, SettleRunnable(child, targetState))
        } else {
            setStateInternal(targetState)
        }
        nestedScrolled = false
    }

    override fun startSettlingAnimation(child: View, state: BottomSheetState) {
        val top: Int = if (state == BottomSheetState.STATE_COLLAPSED) {
            maxOffset
        } else if (state == BottomSheetState.STATE_EXPANDED) {
            minOffset
        } else if (isHideable && state == BottomSheetState.STATE_HIDDEN) {
            parentHeight
        } else {
            throw IllegalArgumentException("Illegal state argument: " + state)
        }
        setStateInternal(BottomSheetState.STATE_SETTLING)
        if (viewDragHelper!!.smoothSlideViewTo(child, child.left, top)) {
            ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
        }
    }
}

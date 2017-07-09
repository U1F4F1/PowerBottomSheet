package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup

import java.lang.ref.WeakReference

/**
 * ~ Licensed under the Apache License, Version 2.0 (the "License");
 * ~ you may not use this file except in compliance with the License.
 * ~ You may obtain a copy of the License at
 * ~
 * ~      http://www.apache.org/licenses/LICENSE-2.0
 * ~
 * ~ Unless required by applicable law or agreed to in writing, software
 * ~ distributed under the License is distributed on an "AS IS" BASIS,
 * ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * ~ See the License for the specific language governing permissions and
 * ~ limitations under the License.
 * ~
 * ~ https://github.com/miguelhincapie/CustomBottomSheetBehavior
 *
 *
 * This class only cares about hide or unhide the FAB because the anchor behavior is something
 * already in FAB.
 */
class ScrollAwareBehavior<V : View>(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<V>() {

    /**
     * One of the point used to set hide() or show() in FAB
     */
    private var offset: Float = 0.toFloat()

    /**
     * The FAB should be hidden when it reach [.offset] or when [AnchorPointBottomSheetBehavior]
     * is visually lower than [AnchorPointBottomSheetBehavior.getPeekHeight].
     * We got a reference to the object to allow change dynamically PeekHeight in BottomSheet and
     * got updated here.
     */
    private var bottomSheetBehaviorRef: WeakReference<AnchorPointBottomSheetBehavior<*>>? = null

    init {
        offset = 0f
        bottomSheetBehaviorRef = null

        val tv = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            offset = TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics).toFloat()
        }
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout?, child: V,
                                     directTargetChild: View?, target: View?, nestedScrollAxes: Int): Boolean {
        // Ensure we react to vertical scrolling
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: V, dependency: View?): Boolean {
        if (dependency is NestedScrollView) {
            if (AnchorPointBottomSheetBehavior.from(dependency) != null) {
                return true
            }
        }
        return false
    }

    override fun onDependentViewChanged(coordinatorLayout: CoordinatorLayout?, child: V, bottomSheet: View?): Boolean {
        if (bottomSheetBehaviorRef == null) {
            getBottomSheetBehavior(coordinatorLayout!!)
        }

        if (child is ViewGroup) {
            return onDependentViewChanged(coordinatorLayout, child, bottomSheet)
        }

        val DyFix = getDyBetweenChildAndDependency(child, bottomSheet!!)

        if (child.y + DyFix < offset) {
            child.visibility = View.INVISIBLE
        } else if (child.y + DyFix >= offset) {
            /*
             * We are calculating every time point in Y where BottomSheet get {@link BottomSheetBehaviorGoogleMapsLike#STATE_COLLAPSED}.
             * If PeekHeight change dynamically we can reflect the behavior asap.
             */
            if (bottomSheetBehaviorRef == null || bottomSheetBehaviorRef!!.get() == null) {
                getBottomSheetBehavior(coordinatorLayout!!)
            }

            val collapsedY = bottomSheet.height - bottomSheetBehaviorRef!!.get()!!.peekHeight

            if (child.y + DyFix > collapsedY) {

                child.visibility = View.INVISIBLE

            } else {
                child.visibility = View.VISIBLE
            }
        }

        return false
    }

    /**
     * if the view passed in is a view group, we look for a [FloatingActionButton] in its children
     * so we can show and hide it as we scroll. The case where we have a [FloatingActionButton]
     * wrapped in something like a framelayout so we can anchor it to an element in the middle of the
     * screen and still apply padding. In the event that we nest the [FloatingActionButton] in a
     * [ViewGroup] any [CoordinatorLayout.Behavior] will be
     * stripped, hence we add them to the parent in this case.
     */
    private fun onDependentViewChanged(coordinatorLayout: CoordinatorLayout, child: ViewGroup, bottomSheet: View): Boolean {
        val DyFix = getDyBetweenChildAndDependency(child, bottomSheet)

        if (child.y + DyFix < offset) {
            for (i in 0..child.childCount - 1) {
                val fab = child.getChildAt(i)
                (fab as? FloatingActionButton)?.hide()
            }
        } else if (child.y + DyFix >= offset) {
            if (bottomSheetBehaviorRef == null || bottomSheetBehaviorRef!!.get() == null) {
                getBottomSheetBehavior(coordinatorLayout)
            }

            val collapsedY = bottomSheet.height - (bottomSheetBehaviorRef!!.get()?.peekHeight ?: 0)

            if (child.y + DyFix > collapsedY) {
                for (i in 0..child.childCount - 1) {
                    val fab = child.getChildAt(i)
                    (fab as? FloatingActionButton)?.hide()
                }
            } else {
                for (i in 0..child.childCount - 1) {
                    val fab = child.getChildAt(i)
                    (fab as? FloatingActionButton)?.show()
                }
            }
        }

        return false
    }

    private fun getDyBetweenChildAndDependency(child: View, dependency: View): Int {
        if (dependency.y == 0f || dependency.y < offset) {
            return 0
        }

        if (dependency.y - child.y > child.height) {
            return Math.max(0, (dependency.y - child.height / 2 - child.y).toInt())
        } else {
            return 0
        }
    }

    /**
     * Look into the CoordiantorLayout for the [AnchorPointBottomSheetBehavior]

     * @param coordinatorLayout with app:layout_behavior= [AnchorPointBottomSheetBehavior]
     */
    private fun getBottomSheetBehavior(coordinatorLayout: CoordinatorLayout) {

        for (i in 0..coordinatorLayout.childCount - 1) {
            val child = coordinatorLayout.getChildAt(i)

            if (child is NestedScrollView) {

                try {
                    val temp = AnchorPointBottomSheetBehavior.from(child)
                    bottomSheetBehaviorRef = WeakReference<AnchorPointBottomSheetBehavior<*>>(temp)
                    break
                } catch (ignored: IllegalArgumentException) { }
            }
        }
    }
}
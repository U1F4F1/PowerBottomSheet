@file:Suppress("unused")

package com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetState

import java.lang.ref.WeakReference

class LandscapeBackdropBottomSheetBehavior<V : View> : CoordinatorLayout.Behavior<V>, AnchorPointBottomSheetBehavior.BottomSheetStateCallback {
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing [AnchorPointBottomSheetBehavior.peekHeight]
     * get changed dynamically we get the [NestedScrollView] that has
     * "app:layout_behavior=" [AnchorPointBottomSheetBehavior] inside the [CoordinatorLayout]
     */
    private var anchorPointBottomSheetBehaviorWeakReference: WeakReference<AnchorPointBottomSheetBehavior<*>>? = null

    private var backdropWeakReference: WeakReference<View>? = null

    /**
     * Following [.onDependentViewChanged]'s docs currentBackdropY just save the child Y
     * position.
     */
    private val currentBackdropY: Int = 0

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: V, dependency: View?): Boolean {
        if (dependency is NestedScrollView) {
            val anchorPointBottomSheetBehavior = AnchorPointBottomSheetBehavior.from(dependency)
            if (anchorPointBottomSheetBehavior != null) {
                anchorPointBottomSheetBehavior.addBottomSheetStateCallback(this)
                child.top = 0
                child.visibility = View.GONE
                backdropWeakReference = WeakReference(child)
            }
        }
        return false
    }

    override fun onStateChanged(bottomSheet: View, newState: BottomSheetState) {
        if (newState == BottomSheetState.STATE_ANCHOR_POINT || newState == BottomSheetState.STATE_COLLAPSED) {
            backdropWeakReference!!.get()!!.setVisibility(View.VISIBLE)
        } else {
            backdropWeakReference!!.get()!!.setVisibility(View.GONE)
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
                    anchorPointBottomSheetBehaviorWeakReference = WeakReference<AnchorPointBottomSheetBehavior<*>>(temp)
                    break
                } catch (ignored: IllegalArgumentException) { }
            }
        }
    }
}

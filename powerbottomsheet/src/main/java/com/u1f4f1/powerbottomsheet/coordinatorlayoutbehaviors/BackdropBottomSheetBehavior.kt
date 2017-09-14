package com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.opengl.Visibility
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import android.view.Window
import com.u1f4f1.powerbottomsheet.R
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheet
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetState
import com.u1f4f1.powerbottomsheet.debug
import com.u1f4f1.powerbottomsheet.info
import com.u1f4f1.powerbottomsheet.logLevel

import java.lang.ref.WeakReference
import android.opengl.ETC1.getHeight
import android.icu.lang.UCharacter.GraphemeClusterBreak.V



class BackdropBottomSheetBehavior<V : View>(context: Context?, attrs: AttributeSet?) : CoordinatorLayout.Behavior<V>(context, attrs) {

    var peekHeight = 0
    var anchorPointY = 0
    var currentChildY = 0

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: V, dependency: View): Boolean {
        val behavior = AnchorPointBottomSheetBehavior.Companion.from(dependency)

        if (behavior != null) {
            anchorPointY = behavior.anchorPoint
            peekHeight = behavior.peekHeight
            return true
        }

        return false
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {

        child.layoutParams.height = anchorPointY

        parent.onLayoutChild(child, layoutDirection)
        ViewCompat.offsetTopAndBottom(child, 0)
        return true
    }

    /**
     * [child] is the instance of the view that we're scrolling behind the [BottomSheet]
     * [dependency] the instance of the [BottomSheet]
     */
    @Suppress("UnnecessaryVariable")
    override fun onDependentViewChanged(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        super.onDependentViewChanged(parent, child, dependency)

        val activityLayout = parent
        val backdrop = child
        val bottomSheet = dependency

        if (bottomSheet is BottomSheet) {

            // sheet is expanded
            if (bottomSheet.y <= 0) {
                // preventing that overdraw son
                backdrop.visibility = View.INVISIBLE
            }

            // sheet is bellow the peek height, so we'll just align our top
            if (bottomSheet.y >= activityLayout.height - peekHeight) {
                currentChildY = bottomSheet.y.toInt()
                backdrop.y = bottomSheet.y

                // also no overdraw
                backdrop.visibility = View.INVISIBLE
            }

            val bottomSheetCollapsedHeight = bottomSheet.getHeight() - peekHeight

            val bottomSheetDistanceFromAnchorPoint = bottomSheet.y - anchorPointY
            val collapsedHeightAnchorOffset = bottomSheetCollapsedHeight - anchorPointY
            val numerator = bottomSheetDistanceFromAnchorPoint * bottomSheetCollapsedHeight

            val calculatedBackdropY = numerator / collapsedHeightAnchorOffset
            currentChildY = calculatedBackdropY.toInt()

            // we know we need the sheet to be showing by this point... so this is probably fine to just have here
            // this is a nice reset state for the backdrop so it isn't permanently fucked if callback hell takes over
            if (bottomSheet.y <= anchorPointY) {
                currentChildY = 0
                backdrop.y = 0f
            } else {
                backdrop.y = currentChildY.toFloat()
            }
        }

        // if we made it this far we should probably show the view
        backdrop.visibility = View.VISIBLE
        return true
    }
}

package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetState

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

 * This class will link the Backdrop element (that can be anything extending View) with a
 * NestedScrollView (the dependency). Whenever dependecy is moved, the backdrop will be moved too
 * behaving like parallax effect.

 * The backdrop need to be <bold>into</bold> a CoordinatorLayout and <bold>before</bold>
 * [AnchorPointBottomSheetBehavior] in the XML file to get same behavior like Google Maps.
 * It doesn't matter where the backdrop element start in XML, it will be moved following
 * Google Maps's parallax behavior.
 * @param <V>
</V> */
class BackdropBottomSheetBehavior<V : View>(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<V>(context, attrs) {
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing [AnchorPointBottomSheetBehavior.peekHeight]
     * get changed dynamically we get the [NestedScrollView] that has
     * "app:layout_behavior=" [AnchorPointBottomSheetBehavior] inside the [CoordinatorLayout]
     */
    private var behaviorGoogleMapsLikeWeakReference: WeakReference<AnchorPointBottomSheetBehavior<*>>? = null

    /**
     * Following [.onDependentViewChanged]'s docs currentBackdropY just save the child Y
     * position.
     */
    private var currentBackdropY: Int = 0

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: V?, dependency: View?): Boolean {
        if (dependency is NestedScrollView) {
            if (AnchorPointBottomSheetBehavior.from(dependency) != null) {
                return true
            }
        }
        return false
    }

    override fun onDependentViewChanged(coordinatorLayout: CoordinatorLayout?, backdrop: V?, bottomSheet: View?): Boolean {
        /*
          bottomSheetCollapsedHeight and anchorPointY are calculated every time looking for
          flexibility, in case that bottomSheet's height, backdrop's height or {@link BottomSheetBehaviorGoogleMapsLike#getPeekHeight()}'s
          value changes throught the time, I mean, you can have a {@link android.widget.ImageView}
          using images with different sizes and you don't want to resize them or so
         */
        if (behaviorGoogleMapsLikeWeakReference == null || behaviorGoogleMapsLikeWeakReference!!.get() == null) {
            getBottomSheetBehavior(coordinatorLayout!!)
        }

        if (behaviorGoogleMapsLikeWeakReference!!.get() is TabletAnchorPointBottomSheetBehavior<*>) {
            // eat touch events on this to prevent dragging this view to collapse the bottom sheet
            coordinatorLayout!!.setOnTouchListener { v, event -> true }
        }

        /*
         * mCollapsedY: Y position in where backdrop get hidden behind bottomSheet.
         * {@link BottomSheetBehaviorGoogleMapsLike#getPeekHeight()} and bottomSheetCollapsedHeight are the same point on screen.
         */
        val bottomSheetCollapsedHeight = bottomSheet!!.height - behaviorGoogleMapsLikeWeakReference!!.get()!!.peekHeight

        /*
         * anchorPointY: with top being Y=0, anchorPointY defines the point in Y where could
         * happen 2 things:
         * The backdrop should be moved behind bottomSheet view (when {@link #currentBackdropY} got
         * positive values) or the bottomSheet view overlaps the backdrop (when
         * {@link #currentBackdropY} got negative values)
         */
        val anchorPointY = behaviorGoogleMapsLikeWeakReference!!.get()!!.anchorPoint

        /*
         * lastCurrentChildY: Just to know if we need to return true or false at the end of this
         * method.
         */
        val lastCurrentChildY = currentBackdropY

        val bottomSheetDistanceFromAnchorPoint = bottomSheet.y - anchorPointY
        val collapsedHeightAnchorOffset = (bottomSheetCollapsedHeight - anchorPointY).toFloat()
        val numerator = bottomSheetDistanceFromAnchorPoint * bottomSheetCollapsedHeight

        val calculatedBackdropY = numerator / collapsedHeightAnchorOffset
        currentBackdropY = calculatedBackdropY.toInt()

        if (calculatedBackdropY <= 0 && behaviorGoogleMapsLikeWeakReference!!.get()!!.state === BottomSheetState.STATE_EXPANDED) {
            backdrop!!.visibility = View.INVISIBLE
        } else {
            backdrop!!.visibility = View.VISIBLE
        }

        if (calculatedBackdropY <= 0) {
            currentBackdropY = 0
            backdrop.y = 0f
        } else {
            backdrop.y = currentBackdropY.toFloat()
        }

        return lastCurrentChildY == currentBackdropY
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
                    val temp = AnchorPointBottomSheetBehavior.from(child)!!
                    behaviorGoogleMapsLikeWeakReference = WeakReference(temp)
                    break
                } catch (ignored: IllegalArgumentException) { }
            }
        }
    }
}
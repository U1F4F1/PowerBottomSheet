package com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.powerbottomsheet.R
import com.u1f4f1.powerbottomsheet.warn

/**
 * Copyright (C) 2017 Tetsuya Masuda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class MergedAppBarLayoutBehavior<V : View>(context: Context, attrs: AttributeSet?) : CoordinatorLayout.Behavior<V>(context, attrs) {
    var peekHeight = 300
    var anchorPointY = 600
    var currentChildY = 0
    var anchorPoint = 0

    var toolbar: Toolbar? = null
    var title = ""

    init {
        attrs?.let {
            var a = context.obtainStyledAttributes(it, android.support.design.R.styleable.BottomSheetBehavior_Layout)

            peekHeight = a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, 0)

            a = context.obtainStyledAttributes(attrs, R.styleable.AnchorPointBottomSheetBehavior)
            anchorPoint = a.getDimension(R.styleable.AnchorPointBottomSheetBehavior_anchorPoint, AnchorPointBottomSheetBehavior.ANCHOR_POINT_AUTO.toFloat()).toInt()
            a.recycle()

            val scrollInBehaviorParam = context.obtainStyledAttributes(it, R.styleable.MergedAppBarLayoutBehavior)
            title = scrollInBehaviorParam?.getString(R.styleable.MergedAppBarLayoutBehavior_title) ?: "Title"
            scrollInBehaviorParam.recycle()
        }
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean
            = AnchorPointBottomSheetBehavior.from(dependency) != null

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        parent.onLayoutChild(child, layoutDirection)
        anchorPointY = parent.height - anchorPoint

        if (child is AppBarLayout) {
            (0 until child.childCount).map {
                child.getChildAt(it)
            }.find {
                it is Toolbar
            }.let {
                toolbar = it as Toolbar
            }
        }

        return true
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        super.onDependentViewChanged(parent, child, dependency)

        val rate = (dependency.y - anchorPoint) / (parent.height - anchorPoint - peekHeight)
        currentChildY = -((child.height + child.paddingTop + child.paddingBottom + child.top + child.bottom) * (rate)).toInt()
        if (currentChildY <= 0) {
            child.y = currentChildY.toFloat()
        } else {
            child.y = 0f
            currentChildY = 0
        }

        val drawable = child.background.mutate()
        val bounds = drawable.bounds
        var heightRate = (bounds.bottom * 2 - dependency.y) / (bounds.bottom) - 1f

        heightRate = when {
            heightRate > 1f -> 1f
            heightRate < 0f -> 0f
            else -> heightRate
        }

        if (heightRate >= 1f) {
            toolbar?.title = title
        } else {
            toolbar?.title = ""
        }

        drawable.setBounds(0, (bounds.bottom - bounds.bottom * heightRate).toInt(), bounds.right, bounds.bottom)
        child.background = drawable

        return true
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout?, child: V, state: Parcelable?) {
        // no op
    }

    companion object {

        /**
         * A utility function to get the [BottomSheetBehavior] associated with the `view`.

         * @param view The [View] with [BottomSheetBehavior].
         * *
         * @return The [BottomSheetBehavior] associated with the `view`.
         */
        fun <V : View> from(view: V): MergedAppBarLayoutBehavior<V>? {

            val params = view.layoutParams
            if (params !is CoordinatorLayout.LayoutParams) {
                warn("The view is not a child of CoordinatorLayout")
                return null
            }

            val behavior = params.behavior
            if (behavior !is MergedAppBarLayoutBehavior<*>) {
                warn("The view is not associated with MergedAppBarLayoutBehavior")
                return null
            }

            @Suppress("UNCHECKED_CAST")
            return behavior as MergedAppBarLayoutBehavior<V>?
        }
    }
}

package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.betterbottomsheet.R
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetState

import java.lang.ref.WeakReference

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
class BackdropBottomSheetBehavior<V : View>(context: Context?, attrs: AttributeSet?) : CoordinatorLayout.Behavior<V>(
        context, attrs) {

    var peekHeight = 300
    var anchorPointY = 600
    var currentChildY = 0
    var anchorPoint = 0

    init {
        context?.let {
            attrs?.let {
                var a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.BottomSheetBehavior_Layout)

                peekHeight = a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, 0)

                a = context.obtainStyledAttributes(attrs, R.styleable.AnchorPointBottomSheetBehavior)
                anchorPoint = a.getDimension(R.styleable.AnchorPointBottomSheetBehavior_anchorPoint, AnchorPointBottomSheetBehavior.ANCHOR_POINT_AUTO.toFloat()).toInt()
                a.recycle()
            }
        }
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean =
            AnchorPointBottomSheetBehavior.from(dependency) != null

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        parent.onLayoutChild(child, layoutDirection)
        ViewCompat.offsetTopAndBottom(child, 0)
        anchorPointY = parent.height - anchorPoint
        return true
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: V,
                                        dependency: View): Boolean {
        super.onDependentViewChanged(parent, child, dependency)
        val rate = (parent.height - dependency.y - peekHeight) / (anchorPointY - peekHeight)
        currentChildY = ((parent.height + child.height) * (1f - rate)).toInt()
        if (currentChildY <= 0) {
            child.y = 0F
            currentChildY = 0
        } else {
            child.y = currentChildY.toFloat()
        }
        return true
    }

}
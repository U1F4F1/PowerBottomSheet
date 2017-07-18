package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheet

class ShrinkBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    val viewsThatOverlapByClassName: MutableMap<String, Boolean> = mutableMapOf()

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: FloatingActionButton?, dependency: View?): Boolean {
        if (dependency is BottomSheet) viewsThatOverlapByClassName.put(BottomSheet::class.java.simpleName, false)

        return dependency is BottomSheet
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout?, child: FloatingActionButton?, dependency: View?): Boolean {
        parent?.let {
            child?.let {
                dependency?.let {
                    val dependencies = parent.getDependencies(child)

                    for (i in 0..dependencies.size - 1) {
                        val view = dependencies[i] as? BottomSheet ?: continue

                        if (parent.doViewsOverlap(child, view)) {
                            viewsThatOverlapByClassName.put(view::class.java.simpleName, true)
                            child.hide()
                        } else {
                            child.show()
                        }
                    }
                }
            }
        }

        return false
    }

    private fun atLeastOneViewOverlapsTheFab(): Boolean {
        return viewsThatOverlapByClassName.all { !it.value }
    }
}
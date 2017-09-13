package com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.view.View
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheet

class ShrinkBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    private val viewsThatOverlapByClassName: MutableMap<String, Boolean> = mutableMapOf()

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: FloatingActionButton?, dependency: View?): Boolean {
        if (dependency is BottomSheet) viewsThatOverlapByClassName.put(BottomSheet::class.java.simpleName, false)

        return dependency is BottomSheet
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout?, child: FloatingActionButton?, dependency: View?): Boolean {
        parent?.let {
            child?.let {
                dependency?.let {
                    val dependencies = parent.getDependencies(child)

                    (0 until dependencies.size)
                            .asSequence()
                            .mapNotNull { dependencies[it] as? BottomSheet }
                            .forEach {
                                if (parent.doViewsOverlap(child, it)) {
                                    viewsThatOverlapByClassName.put(it::class.java.simpleName, true)
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

    private fun atLeastOneViewOverlapsTheFab(): Boolean =
            viewsThatOverlapByClassName.all { !it.value }
}
package com.u1f4f1.powerbottomsheet.bottomsheet

import android.annotation.TargetApi
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.view.View
import com.airbnb.epoxy.EpoxyAdapter
import com.airbnb.epoxy.EpoxyModel
import com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior
import com.u1f4f1.powerbottomsheet.trace
import java.util.concurrent.LinkedBlockingDeque

abstract class BottomSheetAdapter(val behavior: AnchorPointBottomSheetBehavior<*>?) : EpoxyAdapter() {
    val updates = LinkedBlockingDeque<Runnable>()
    protected lateinit var recyclerViewTransitionRunnable: Runnable

    init {
        val onBottomSheetStateChanged = object : AnchorPointBottomSheetBehavior.BottomSheetStateCallback {
            override fun onStateChanged(bottomSheet: View, newState: BottomSheetState) {
                for (i in 0 until updates.size) {
                    updates.pop()?.run()
                }
            }
        }

        behavior?.addBottomSheetStateCallback(onBottomSheetStateChanged)
    }

    override fun notifyModelsChanged() {
        if (behavior?.isStable != true) {
            updates.add(Runnable {
                super.notifyModelsChanged()
            })
            return
        }

        super.notifyModelsChanged()
    }

    @TargetApi(19)
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)

        trace("BottomSheetAdapter.onAttachedToRecyclerView(recyclerView: RecyclerView?)")
        recyclerViewTransitionRunnable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Runnable { TransitionManager.beginDelayedTransition(recyclerView) }
        } else {
            Runnable { /* no op */ }
        }
    }
}

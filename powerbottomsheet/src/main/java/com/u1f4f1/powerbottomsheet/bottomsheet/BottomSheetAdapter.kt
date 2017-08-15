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
    lateinit var recyclerViewTransitionRunnable: Runnable

    init {
        val onBottomSheetStateChanged = object : AnchorPointBottomSheetBehavior.BottomSheetStateCallback {
            override fun onStateChanged(bottomSheet: View, newState: BottomSheetState) {
                for (i in 0..updates.size - 1) {
                    updates.pop()?.run()
                }
            }
        }

        behavior?.addBottomSheetStateCallback(onBottomSheetStateChanged)
    }

    override fun notifyModelChanged(model: EpoxyModel<*>?) {
        if (!(behavior?.isStable ?: false)) {
            updates.add(Runnable {
                super.notifyModelChanged(model)
            })
            return
        }

        super.notifyModelChanged(model)
    }

    override fun notifyModelsChanged() {
        if (!(behavior?.isStable ?: false)) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            recyclerViewTransitionRunnable = Runnable { TransitionManager.beginDelayedTransition(recyclerView) }
        } else {
            // I would rather invoke an empty lambda than have to check for null everywhere
            recyclerViewTransitionRunnable = Runnable { }
        }
    }
}

package com.u1f4f1.betterbottomsheet.bottomsheet

import com.airbnb.epoxy.EpoxyAdapter
import com.airbnb.epoxy.EpoxyModel
import com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior
import java.util.concurrent.LinkedBlockingDeque

abstract class BottomSheetAdapter(val behavior: AnchorPointBottomSheetBehavior<*>) : EpoxyAdapter() {
    val updates = LinkedBlockingDeque<Runnable>()

    init {
        behavior.addBottomSheetStateCallback { _, _ ->
            run {
                for (i in 0..updates.size - 1) {
                    updates.pop()?.run()
                }
            }
        }
    }

    override fun notifyModelChanged(model: EpoxyModel<*>?) {
        if (!behavior.isStable) {
            updates.add(Runnable {
                super.notifyModelChanged(model)
            })
            return
        }

        super.notifyModelChanged(model)
    }

    override fun notifyModelChanged(model: EpoxyModel<*>?, payload: Any?) {
        if (!behavior.isStable) {
            updates.add(Runnable {
                super.notifyModelChanged(model, payload)
            })
            return
        }

        super.notifyModelChanged(model, payload)
    }

    override fun notifyModelsChanged() {
        if (!behavior.isStable) {
            updates.add(Runnable {
                super.notifyModelsChanged()
            })
            return
        }

        super.notifyModelsChanged()
    }
}

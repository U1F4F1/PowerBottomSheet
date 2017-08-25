package com.u1f4f1.sample

import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetAdapter
import com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior

class SampleAdapter(behavior: AnchorPointBottomSheetBehavior<*>?) : BottomSheetAdapter(behavior) {

    val headerModel = HeaderModel_()

    fun addFakeViews() {
        addModel(headerModel)

        for (i in 0..10) {
            addModel(ExpandingCardModel_().delayedTransitionRunnable { recyclerViewTransitionRunnable.run() })
        }
    }

    fun activateHeader(activate: Boolean) {
        headerModel.active = activate
        notifyModelChanged(headerModel)
    }
}

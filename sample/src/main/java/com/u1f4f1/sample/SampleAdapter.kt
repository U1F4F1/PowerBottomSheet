package com.u1f4f1.sample

import com.mooveit.library.Fakeit
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetAdapter
import com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior

class SampleAdapter(behavior: AnchorPointBottomSheetBehavior<*>?) : BottomSheetAdapter(behavior) {

    val headerModel = HeaderModel_()

    fun addHeader() = addModel(headerModel)

    fun addFakeViews() {
        addModel(ExpandingCardModel_().delayedTransitionRunnable { recyclerViewTransitionRunnable.run() })

        for (i in 0..10) {
            addModel(QuoteCardModel_().quote(Fakeit.rickAndMorty().quote()).attribution(Fakeit.rickAndMorty().character()))
        }
    }

    fun activateHeader(activate: Boolean) {
        headerModel.active = activate
        notifyModelChanged(headerModel)
    }
}

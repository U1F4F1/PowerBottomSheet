package com.u1f4f1.sample

import com.mooveit.library.Fakeit
import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetAdapter
import com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior
import com.u1f4f1.powerbottomsheet.debug
import java.util.*

class SampleAdapter(behavior: AnchorPointBottomSheetBehavior<*>?) : BottomSheetAdapter(behavior) {

    val headerModel = HeaderModel_()

    fun addFakeViews() {
        addModel(headerModel)

        for (i in 0..10) {
            addModel(
                    RatingModel_()
                            .buttonClickConsumer { Runnable {  } }
                            .rating(Random(System.currentTimeMillis()).nextFloat())
                            .text(Fakeit.rickAndMorty().quote())
            )
        }

        for (i in 0..10) {
            addModel(HoursModel_().buttonClickConsumer { Runnable { } })
        }
    }

    fun dataLoaded() {
        headerModel.finishedLoading()
        notifyModelChanged(headerModel)
        debug("data loaded")
    }
}

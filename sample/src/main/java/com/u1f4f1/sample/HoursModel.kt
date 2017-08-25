package com.u1f4f1.sample

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelClass

@EpoxyModelClass(layout = R.layout.model_business_hours)
abstract class HoursModel : EpoxyModel<HoursView>() {
    @EpoxyAttribute(hash = false)
    var buttonClickConsumer: Runnable? = null

    override fun bind(view: HoursView?) {
        view?.hoursContainer?.setOnClickListener({
            buttonClickConsumer!!.run()

            view.cardClicked()
        })
    }
}

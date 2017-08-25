package com.u1f4f1.sample

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelClass
import com.mooveit.library.Fakeit
import java.util.*

@EpoxyModelClass(layout = R.layout.model_header)
abstract class HeaderModel : EpoxyModel<HeaderView>() {

    @EpoxyAttribute var active: Boolean = false

    override fun bind(view: HeaderView, payloads: List<Any>?) {
        bind(view)
    }

    override fun bind(view: HeaderView) {
        if (active) {
            view.activate()
        } else {
            view.deactivate()
        }
    }

    override fun unbind(view: HeaderView?) {
        super.unbind(view)
    }
}

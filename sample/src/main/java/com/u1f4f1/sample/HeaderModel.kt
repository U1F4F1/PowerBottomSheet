package com.u1f4f1.sample

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelClass
import com.mooveit.library.Fakeit
import java.util.*

/**
 * This model shows an example of binding to a specific view type. In this case it is a custom view
 * we made, but it could also be another single view, like an EditText or Button.
 */
@EpoxyModelClass(layout = R.layout.model_header)
abstract class HeaderModel() : EpoxyModel<HeaderView>() {
    @EpoxyAttribute var title: String? = null
    @EpoxyAttribute var open: String? = null
    @EpoxyAttribute var close: String? = null
    @EpoxyAttribute var open24Hours: Boolean = false
    @EpoxyAttribute var address1: String? = null
    @EpoxyAttribute var address2: String? = null
    @EpoxyAttribute var distance: String? = null

    @EpoxyAttribute var active: Boolean = false
    @EpoxyAttribute var loading: Boolean = true

    override fun bind(view: HeaderView, payloads: List<Any>?) {
        bind(view)
    }

    override fun bind(view: HeaderView) {
        view.setPoiName(title)

        if (!loading) {
            view.hideProgressBar()
        }

        if (open24Hours) {
            view.setPoiHoursText()
        } else {
            view.setPoiHoursText()
        }

        view.setPoiAddress1(address1)
        view.setPoiAddress2(address2)

        view.setPoiDistance(distance)

        if (active) {
            view.activate()
        } else {
            view.deactivate()
        }
    }

    override fun unbind(view: HeaderView?) {
        super.unbind(view)

        loading = true
    }

    fun finishedLoading() {
        title = Fakeit.business().name()
        open24Hours = true
        address1 = Fakeit.address().streetAddress()
        address2 = "${Fakeit.address().city()}, ${Fakeit.address().stateAbbreviation()} ${Fakeit.address().zipCode()}"
        distance = "${Random(System.currentTimeMillis()).nextDouble()} mi"

        loading = false
    }
}

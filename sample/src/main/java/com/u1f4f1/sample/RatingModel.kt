package com.u1f4f1.sample

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelClass
import kotlinx.android.synthetic.main.poi_review_card.view.*

@EpoxyModelClass(layout = R.layout.model_rating)
abstract class RatingModel : EpoxyModel<RatingView>() {
    @EpoxyAttribute var buttonClickConsumer: Runnable? = null
    @EpoxyAttribute var rating: Float = 0.toFloat()
    @EpoxyAttribute var text: String? = null
    @EpoxyAttribute var updating: Boolean = false

    private var isInitialized = false

    override fun bind(view: RatingView, payloads: List<Any>?) {
        if (!isInitialized) bind(view)

        view.setRating(rating)
        view.setReview(text!!)

        if (updating) {
            view.expandCard()
        }

        isInitialized = true
    }

    override fun bind(view: RatingView?) {
        view!!.setRating(rating)
        view.setReview(text!!)

        if (updating) {
            view.expandCard()
            view.isUpdating(true)
        }

        view.rating_bar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                buttonClickConsumer!!.run()

                onRatingChanged(rating)

                view.expandCard()
            }
        }
    }

    private fun onRatingChanged(rating: Float) {
        this.rating = rating
    }
}

package com.u1f4f1.sample

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.poi_review_card.view.*

class RatingView(context: Context, attrs: AttributeSet) : ExpandingCard(context, attrs) {
    internal var accent: Int = 0

    override fun inflateLayout(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.poi_review_card, this)

        accent = ContextCompat.getColor(context, R.color.colorAccent)

        update_button.setOnClickListener({ v ->
            update_button!!.startAnimation()

            val success = Math.random() < 0.25

            if (success) {
                postDelayed({ this.success() }, 300)
            } else {
                postDelayed({ this.failed() }, 150)
            }
        })
    }

    private fun success() {
        review_input.isEnabled = false
        rating_bar.isEnabled = false
        update_button.doneLoadingAnimation(accent, BitmapFactory.decodeResource(resources, R.drawable.ic_check_white))

        // post this delayed so the button rests in its success state and also prevents the user from spamming the button
        update_button.postDelayed({
            update_button.revertAnimation({
                isUpdating(true)
                review_input.isEnabled = true
                rating_bar.isEnabled = true
            })
        }, 1000)
    }

    private fun failed() {
        update_button.revertAnimation({ update_button.text = "Retry" })
    }

    fun isUpdating(userIsUpdating: Boolean) {
        if (userIsUpdating) {
            update_button.text = "Update"
        }
    }

    fun setRating(rating: Float) {
        rating_bar.rating = rating
    }

    fun setReview(text: String) {
        review_input.setText(text)
    }
}

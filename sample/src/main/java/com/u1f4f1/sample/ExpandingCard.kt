package com.u1f4f1.sample

import android.content.Context
import android.support.annotation.Nullable
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.View
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelView
import kotlinx.android.synthetic.main.expanding_card.view.*

@ModelView(defaultLayout = R.layout.model_expanding)
class ExpandingCard(context: Context, attrs: AttributeSet?) : CardView(context, attrs) {
    private var isExpanded = false
    private var delayedTransitionRunnable: Runnable? = null

    init {
        View.inflate(context, R.layout.expanding_card, this)

        this.setOnClickListener {
            cardClicked()
        }
    }

    @CallbackProp
    fun setDelayedTransitionRunnable(@Nullable runnable: Runnable?) {
        this.delayedTransitionRunnable = runnable
    }

    fun cardClicked() {
        val stateSet = intArrayOf(android.R.attr.state_checked * if (isExpanded) -1 else 1)

        if (isExpanded) {
            status_header_arrow.setImageState(stateSet, true)
            collapseCard()
        } else {
            status_header_arrow.setImageState(stateSet, true)
            expandCard()
        }
        delayedTransitionRunnable?.run()
    }

    fun collapseCard() {
        expanding_container.visibility = View.GONE
        isExpanded = false
    }

    fun expandCard() {
        expanding_container.visibility = View.VISIBLE
        isExpanded = true
    }
}

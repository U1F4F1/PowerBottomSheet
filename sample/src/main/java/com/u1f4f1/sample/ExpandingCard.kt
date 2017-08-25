package com.u1f4f1.sample

import android.content.Context
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.hours_card.view.*

/**
 * Base class designed to handle expanding and collapsing [CardView]
 *
 * Subclasses must have a layout that includes a [R.id.status_header_arrow]
 * and a [R.id.expanding_container]
 */
abstract class ExpandingCard : CardView {
    internal var isExpanded = false

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflateLayout(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflateLayout(context, attrs)
    }

    /**
     * This will be called from each constructor in this base class.
     * This must call [butterknife.ButterKnife.bind] after inflating the required layout.
     *
     * @param context the context used to inflate the layout
     * @param attrs any attributes passed to views
     */
    abstract fun inflateLayout(context: Context, attrs: AttributeSet?)

    fun cardClicked() {
        val stateSet = intArrayOf(android.R.attr.state_checked * if (isExpanded) -1 else 1)

        if (isExpanded) {
            status_header_arrow.setImageState(stateSet, true)
            collapseCard()
        } else {
            status_header_arrow.setImageState(stateSet, true)
            expandCard()
        }
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

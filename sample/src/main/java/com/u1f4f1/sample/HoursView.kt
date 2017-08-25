package com.u1f4f1.sample

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.hours_card.view.*

class HoursView : ExpandingCard {
    var hoursContainer: ViewGroup = status_header

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun inflateLayout(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.hours_card, this)
    }
}
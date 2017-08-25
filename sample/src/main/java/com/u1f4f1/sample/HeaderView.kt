package com.u1f4f1.sample

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.header_card.view.*

class HeaderView(context: Context, attrs: AttributeSet) : CardView(context, attrs) {

    private val activeBackgroundColor: Int
    private val activeTextColor: Int

    private val inactiveBackgroundColor: Int
    private val inactiveTextColor: Int
    private val inactiveTextColorSecondary: Int

    private var active: Boolean = false

    init {
        View.inflate(context, R.layout.header_card, this)

        val a = context.obtainStyledAttributes(attrs, R.styleable.HeaderView)

        activeBackgroundColor = a.getColor(R.styleable.HeaderView_activeBackgroundColor, ContextCompat.getColor(getContext(), R.color.colorPrimary))
        activeTextColor = a.getColor(R.styleable.HeaderView_activeTextColor, ContextCompat.getColor(getContext(), R.color.white))

        inactiveBackgroundColor = a.getColor(R.styleable.HeaderView_inactiveBackgroundColor, ContextCompat.getColor(getContext(), R.color.white))
        inactiveTextColor = a.getColor(R.styleable.HeaderView_inactiveTextColor, ContextCompat.getColor(getContext(), R.color.colorPrimary))
        inactiveTextColorSecondary = a.getColor(R.styleable.HeaderView_inactiveTextColorSecondary, ContextCompat.getColor(getContext(), R.color.black))

        deactivate()

        a.recycle()
    }

    fun hideProgressBar() {
        progressBarTop.visibility = View.GONE
    }

    fun setPoiName(poiName: String?) {
        poi_name.text = poiName
    }

    fun setPoiHoursText() {
        poi_hours.text = context.getString(R.string.always_open)
        poi_hours.visibility = View.VISIBLE
        poi_hours_open.visibility = View.VISIBLE
        poi_hours_clock.visibility = View.VISIBLE
    }

    fun setPoiAddress1(address: String?) {
        poi_address_line1.text = address
    }

    fun setPoiAddress2(address2: String?) {
        poi_address_line2.text = address2
    }

    fun setPoiDistance(distance: String?) {
        poi_distance.text = distance
    }

    fun activate() {
        if (active) return

        setBackgroundColor(activeBackgroundColor)

        poi_name.setTextColor(activeTextColor)
        poi_hours_open.setTextColor(activeTextColor)
        poi_hours.setTextColor(activeTextColor)
        poi_distance.setTextColor(activeTextColor)
        poi_address_line1.setTextColor(activeTextColor)
        poi_address_line2.setTextColor(activeTextColor)

        poi_hours_clock.setColorFilter(activeTextColor)

        active = true
    }

    fun deactivate() {
        if (!active) return

        setBackgroundColor(inactiveBackgroundColor)
        poi_name.setTextColor(inactiveTextColorSecondary)
        poi_hours_open.setTextColor(inactiveTextColor)
        poi_hours.setTextColor(inactiveTextColor)
        poi_distance.setTextColor(inactiveTextColor)
        poi_address_line1.setTextColor(inactiveTextColor)
        poi_address_line2.setTextColor(inactiveTextColor)

        poi_hours_clock.setColorFilter(inactiveTextColor)

        active = false
    }
}

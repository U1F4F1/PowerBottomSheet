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

    fun activate() {
        if (active) return

        setBackgroundColor(activeBackgroundColor)
        header_text.setTextColor(activeTextColor)

        active = true
    }

    fun deactivate() {
        if (!active) return

        setBackgroundColor(inactiveBackgroundColor)
        header_text.setTextColor(inactiveTextColor)

        active = false
    }
}

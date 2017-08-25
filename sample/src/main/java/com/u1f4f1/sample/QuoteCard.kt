package com.u1f4f1.sample

import android.content.Context
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.View
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import kotlinx.android.synthetic.main.quote_card.view.*

@ModelView(defaultLayout = R.layout.model_quote)
class QuoteCard(context: Context, attrs: AttributeSet?) : CardView(context, attrs) {
    init {
        View.inflate(context, R.layout.quote_card, this)
    }

    @ModelProp
    fun setQuote(quote: String) {
        quote_body.text = quote
    }

    @ModelProp
    fun setAttribution(attribution: String) {
        quote_attribution.text = attribution
    }
}
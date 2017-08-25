package com.u1f4f1.sample

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheet


class SampleBottomSheet : BottomSheet {
    constructor(context: Context) : super(context) {
        View.inflate(context, R.layout.bottom_sheet, this)

        if (!isInEditMode) {
            this.recyclerView = findViewById(R.id.recyclerview)
        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        View.inflate(context, R.layout.bottom_sheet, this)

        if (!isInEditMode) {
            this.recyclerView = findViewById(R.id.recyclerview)
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        View.inflate(context, R.layout.bottom_sheet, this)

        if (!isInEditMode) {
            this.recyclerView = findViewById(R.id.recyclerview)
        }
    }
}

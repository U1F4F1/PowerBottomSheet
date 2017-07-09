package com.u1f4f1.sample;

import android.content.Context;
import android.util.AttributeSet;

import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheet;


public class SampleBottomSheet extends BottomSheet {
    public SampleBottomSheet(Context context) {
        super(context);
        inflate(context, R.layout.bottom_sheet, this);
    }

    public SampleBottomSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.bottom_sheet, this);
    }

    public SampleBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.bottom_sheet, this);
    }
}

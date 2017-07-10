package com.u1f4f1.sample;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HoursView extends ExpandingCard {
    @BindView(R.id.status_header) ViewGroup hoursContainer;

    public HoursView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HoursView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void inflateLayout(Context context, AttributeSet attrs) {
        inflate(context, R.layout.hours_card, this);
        ButterKnife.bind(this);
    }
}
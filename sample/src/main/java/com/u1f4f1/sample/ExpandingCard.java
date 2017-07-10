package com.u1f4f1.sample;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import butterknife.BindView;

/**
 * Base class designed to handle expanding and collapsing {@link CardView}
 *
 * Subclasses must have a layout that includes a {@link R.id#status_header_arrow}
 * and a {@link R.id#expanding_container}
 */
public abstract class ExpandingCard extends CardView {
    @BindView(R.id.status_header_arrow) protected ImageView arrowIcon;
    @BindView(R.id.expanding_container) protected ViewGroup expandingContainer;

    boolean isExpanded = false;

    public ExpandingCard(Context context) {
        super(context);
    }

    public ExpandingCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateLayout(context, attrs);
        expandingContainer.setVisibility(GONE);
    }

    public ExpandingCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateLayout(context, attrs);
        expandingContainer.setVisibility(GONE);
    }

    /**
     * This will be called from each constructor in this base class.
     * This must call {@link butterknife.ButterKnife#bind(View)} after inflating the required layout.
     *
     * @param context the context used to inflate the layout
     * @param attrs any attributes passed to views
     */
    public abstract void inflateLayout(Context context, AttributeSet attrs);

    public void cardClicked() {
        final int[] stateSet = {android.R.attr.state_checked * (isExpanded ? -1 : 1)};

        if (isExpanded) {
            arrowIcon.setImageState(stateSet, true);
            collapseCard();
        } else {
            arrowIcon.setImageState(stateSet, true);
            expandCard();
        }
    }

    public void collapseCard() {
        expandingContainer.setVisibility(GONE);
        isExpanded = false;
    }

    public void expandCard() {
        expandingContainer.setVisibility(VISIBLE);
        isExpanded = true;
    }
}

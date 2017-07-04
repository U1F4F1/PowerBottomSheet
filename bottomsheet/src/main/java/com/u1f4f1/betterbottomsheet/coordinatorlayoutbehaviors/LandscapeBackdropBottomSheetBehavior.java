package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;

public class LandscapeBackdropBottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> implements AnchorPointBottomSheetBehavior.BottomSheetStateCallback {
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing {@link AnchorPointBottomSheetBehavior#peekHeight}
     * get changed dynamically we get the {@link NestedScrollView} that has
     * "app:layout_behavior=" {@link AnchorPointBottomSheetBehavior} inside the {@link CoordinatorLayout}
     */
    private WeakReference<AnchorPointBottomSheetBehavior> anchorPointBottomSheetBehaviorWeakReference;

    private WeakReference<View> backdropWeakReference;

    /**
     * Following {@link #onDependentViewChanged}'s docs currentBackdropY just save the child Y
     * position.
     */
    private int currentBackdropY;

    public LandscapeBackdropBottomSheetBehavior() { super(); }

    public LandscapeBackdropBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof NestedScrollView) {
            AnchorPointBottomSheetBehavior anchorPointBottomSheetBehavior = AnchorPointBottomSheetBehavior.from(dependency);
            if (anchorPointBottomSheetBehavior != null) {
                anchorPointBottomSheetBehavior.addBottomSheetStateCallback(this);
                child.setTop(0);
                child.setVisibility(View.GONE);
                backdropWeakReference = new WeakReference<>(child);
            }
        }
        return false;
    }

//    @Override
//    public boolean onDependentViewChanged(CoordinatorLayout coordinatorLayout, View backdrop, View bottomSheet) {
//        if (anchorPointBottomSheetBehaviorWeakReference == null || anchorPointBottomSheetBehaviorWeakReference.get() == null) {
//            getBottomSheetBehavior(coordinatorLayout);
//        }
//
//        if (anchorPointBottomSheetBehaviorWeakReference.get() instanceof TabletAnchorPointBottomSheetBehavior) {
//            // eat touch events on this to prevent dragging this view to collapse the bottom sheet
//            coordinatorLayout.setOnTouchListener((v, event) -> true);
//        }
//
//        int bottomSheetCollapsedHeight =
//                Resources.getSystem().getDisplayMetrics().heightPixels - anchorPointBottomSheetBehaviorWeakReference.get().getPeekHeight();
//
//        /*
//         * anchorPointY: with top being Y=0, anchorPointY defines the point in Y where could
//         * happen 2 things:
//         * The backdrop should be moved behind bottomSheet view (when {@link #currentBackdropY} got
//         * positive values) or the bottomSheet view overlaps the backdrop (when
//         * {@link #currentBackdropY} got negative values)
//         */
//        int anchorPointY = anchorPointBottomSheetBehaviorWeakReference.get().getAnchorPoint();
//
//        /*
//         * lastCurrentChildY: Just to know if we need to return true or false at the end of this
//         * method.
//         */
//        int lastCurrentChildY = currentBackdropY;
//
//        float bottomSheetDistanceFromAnchorPoint = bottomSheet.getY() - anchorPointY;
//        float collapsedHeightAnchorOffset = bottomSheetCollapsedHeight - anchorPointY;
//        float numerator = bottomSheetDistanceFromAnchorPoint * bottomSheetCollapsedHeight;
//
//        float calculatedBackdropY = numerator / collapsedHeightAnchorOffset;
//        currentBackdropY = (int) calculatedBackdropY;
//
//        if (calculatedBackdropY <= 0 &&
//            anchorPointBottomSheetBehaviorWeakReference.get().getState() == AnchorPointBottomSheetBehavior.STATE_EXPANDED)
//        {
//            backdrop.setVisibility(View.INVISIBLE);
//        } else {
//            backdrop.setVisibility(View.VISIBLE);
//        }
//
//        if(calculatedBackdropY <= 0) {
//            currentBackdropY = 0;
//            backdrop.setY(0);
//        }
//        else {
//            backdrop.setY(currentBackdropY);
//        }
//
//        return (lastCurrentChildY == currentBackdropY);
//    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, @AnchorPointBottomSheetBehavior.State int newState) {
        if (newState == AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT || newState == AnchorPointBottomSheetBehavior.STATE_COLLAPSED) {
            backdropWeakReference.get().setVisibility(View.VISIBLE);
        } else {
            backdropWeakReference.get().setVisibility(View.GONE);
        }
    }

    /**
     * Look into the CoordiantorLayout for the {@link AnchorPointBottomSheetBehavior}
     * @param coordinatorLayout with app:layout_behavior= {@link AnchorPointBottomSheetBehavior}
     */
    private void getBottomSheetBehavior(@NonNull CoordinatorLayout coordinatorLayout) {

        for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
            View child = coordinatorLayout.getChildAt(i);

            if (child instanceof NestedScrollView) {

                try {
                    AnchorPointBottomSheetBehavior temp = AnchorPointBottomSheetBehavior.from(child);
                    anchorPointBottomSheetBehaviorWeakReference = new WeakReference<>(temp);
                    break;
                }
                catch (IllegalArgumentException ignored){}
            }
        }
    }
}

package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~      http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~
 ~ https://github.com/miguelhincapie/CustomBottomSheetBehavior
 *
 * This class will link the Backdrop element (that can be anything extending View) with a
 * NestedScrollView (the dependency). Whenever dependecy is moved, the backdrop will be moved too
 * behaving like parallax effect.
 *
 * The backdrop need to be <bold>into</bold> a CoordinatorLayout and <bold>before</bold>
 * {@link AnchorPointBottomSheetBehavior} in the XML file to get same behavior like Google Maps.
 * It doesn't matter where the backdrop element start in XML, it will be moved following
 * Google Maps's parallax behavior.
 * @param <V>
 */
public class BackdropBottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing {@link AnchorPointBottomSheetBehavior#peekHeight}
     * get changed dynamically we get the {@link NestedScrollView} that has
     * "app:layout_behavior=" {@link AnchorPointBottomSheetBehavior} inside the {@link CoordinatorLayout}
     */
    private WeakReference<AnchorPointBottomSheetBehavior> behaviorGoogleMapsLikeWeakReference;
    /**
     * Following {@link #onDependentViewChanged}'s docs currentBackdropY just save the child Y
     * position.
     */
    private int currentBackdropY;

    public BackdropBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof NestedScrollView) {
            if (AnchorPointBottomSheetBehavior.from(dependency) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout coordinatorLayout, View backdrop, View bottomSheet) {
        /*
          bottomSheetCollapsedHeight and anchorPointY are calculated every time looking for
          flexibility, in case that bottomSheet's height, backdrop's height or {@link BottomSheetBehaviorGoogleMapsLike#getPeekHeight()}'s
          value changes throught the time, I mean, you can have a {@link android.widget.ImageView}
          using images with different sizes and you don't want to resize them or so
         */
        if (behaviorGoogleMapsLikeWeakReference == null || behaviorGoogleMapsLikeWeakReference.get() == null) {
            getBottomSheetBehavior(coordinatorLayout);
        }

        if (behaviorGoogleMapsLikeWeakReference.get() instanceof TabletAnchorPointBottomSheetBehavior) {
            // eat touch events on this to prevent dragging this view to collapse the bottom sheet
            coordinatorLayout.setOnTouchListener((v, event) -> true);
        }

        /*
         * mCollapsedY: Y position in where backdrop get hidden behind bottomSheet.
         * {@link BottomSheetBehaviorGoogleMapsLike#getPeekHeight()} and bottomSheetCollapsedHeight are the same point on screen.
         */
        int bottomSheetCollapsedHeight = bottomSheet.getHeight() - behaviorGoogleMapsLikeWeakReference.get().getPeekHeight();

        /*
         * anchorPointY: with top being Y=0, anchorPointY defines the point in Y where could
         * happen 2 things:
         * The backdrop should be moved behind bottomSheet view (when {@link #currentBackdropY} got
         * positive values) or the bottomSheet view overlaps the backdrop (when
         * {@link #currentBackdropY} got negative values)
         */
        int anchorPointY = behaviorGoogleMapsLikeWeakReference.get().getAnchorPoint();

        /*
         * lastCurrentChildY: Just to know if we need to return true or false at the end of this
         * method.
         */
        int lastCurrentChildY = currentBackdropY;

        float bottomSheetDistanceFromAnchorPoint = bottomSheet.getY() - anchorPointY;
        float collapsedHeightAnchorOffset = bottomSheetCollapsedHeight - anchorPointY;
        float numerator = bottomSheetDistanceFromAnchorPoint * bottomSheetCollapsedHeight;

        float calculatedBackdropY = numerator / collapsedHeightAnchorOffset;
        currentBackdropY = (int) calculatedBackdropY;

        if (calculatedBackdropY <= 0 && behaviorGoogleMapsLikeWeakReference.get().getState() == AnchorPointBottomSheetBehavior.STATE_EXPANDED) {
            backdrop.setVisibility(View.INVISIBLE);
        } else {
            backdrop.setVisibility(View.VISIBLE);
        }

        if(calculatedBackdropY <= 0) {
            currentBackdropY = 0;
            backdrop.setY(0);
        }
        else {
            backdrop.setY(currentBackdropY);
        }

        return (lastCurrentChildY == currentBackdropY);
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
                    behaviorGoogleMapsLikeWeakReference = new WeakReference<>(temp);
                    break;
                }
                catch (IllegalArgumentException ignored){}
            }
        }
    }
}
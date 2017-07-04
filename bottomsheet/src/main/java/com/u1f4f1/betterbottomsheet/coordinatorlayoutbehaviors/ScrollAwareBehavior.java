package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

/**
 * ~ Licensed under the Apache License, Version 2.0 (the "License");
 * ~ you may not use this file except in compliance with the License.
 * ~ You may obtain a copy of the License at
 * ~
 * ~      http://www.apache.org/licenses/LICENSE-2.0
 * ~
 * ~ Unless required by applicable law or agreed to in writing, software
 * ~ distributed under the License is distributed on an "AS IS" BASIS,
 * ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * ~ See the License for the specific language governing permissions and
 * ~ limitations under the License.
 * ~
 * ~ https://github.com/miguelhincapie/CustomBottomSheetBehavior
 * <p>
 * This class only cares about hide or unhide the FAB because the anchor behavior is something
 * already in FAB.
 */
public class ScrollAwareBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    /**
     * One of the point used to set hide() or show() in FAB
     */
    private float offset;

    /**
     * The FAB should be hidden when it reach {@link #offset} or when {@link AnchorPointBottomSheetBehavior}
     * is visually lower than {@link AnchorPointBottomSheetBehavior#getPeekHeight()}.
     * We got a reference to the object to allow change dynamically PeekHeight in BottomSheet and
     * got updated here.
     */
    private WeakReference<AnchorPointBottomSheetBehavior> mBottomSheetBehaviorRef;

    public ScrollAwareBehavior(Context context, AttributeSet attrs) {
        super();
        offset = 0;
        mBottomSheetBehaviorRef = null;

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            offset = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
    }

    @Override
    public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout, final View child,
                                       final View directTargetChild, final View target, final int nestedScrollAxes) {
        // Ensure we react to vertical scrolling
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
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
    public boolean onDependentViewChanged(CoordinatorLayout coordinatorLayout, View child, View bottomSheet) {
        if (mBottomSheetBehaviorRef == null) {
            getBottomSheetBehavior(coordinatorLayout);
        }

        if (child instanceof ViewGroup) {
            return onDependentViewChanged(coordinatorLayout, ((ViewGroup) child), bottomSheet);
        }

        int DyFix = getDyBetweenChildAndDependency(child, bottomSheet);

        if ((child.getY() + DyFix) < offset) {
            child.setVisibility(View.INVISIBLE);
        } else if ((child.getY() + DyFix) >= offset) {
            /*
             * We are calculating every time point in Y where BottomSheet get {@link BottomSheetBehaviorGoogleMapsLike#STATE_COLLAPSED}.
             * If PeekHeight change dynamically we can reflect the behavior asap.
             */
            if (mBottomSheetBehaviorRef == null || mBottomSheetBehaviorRef.get() == null) {
                getBottomSheetBehavior(coordinatorLayout);
            }

            int collapsedY = bottomSheet.getHeight() - mBottomSheetBehaviorRef.get().getPeekHeight();

            if ((child.getY() + DyFix) > collapsedY) {

                child.setVisibility(View.INVISIBLE);

            } else {
                child.setVisibility(View.VISIBLE);
            }
        }

        return false;
    }

    /**
     * if the view passed in is a view group, we look for a {@link FloatingActionButton} in its children
     * so we can show and hide it as we scroll. The case where we have a {@link FloatingActionButton}
     * wrapped in something like a framelayout so we can anchor it to an element in the middle of the
     * screen and still apply padding. In the event that we nest the {@link FloatingActionButton} in a
     * {@link ViewGroup} any {@link CoordinatorLayout.Behavior} will be
     * stripped, hence we add them to the parent in this case.
     */
    private boolean onDependentViewChanged(CoordinatorLayout coordinatorLayout, ViewGroup child, View bottomSheet) {
        int DyFix = getDyBetweenChildAndDependency(child, bottomSheet);

        if ((child.getY() + DyFix) < offset) {
            for (int i = 0; i < child.getChildCount(); i++) {
                View fab = child.getChildAt(i);
                if (fab instanceof FloatingActionButton) {
                    ((FloatingActionButton) fab).hide();
                }
            }
        } else if ((child.getY() + DyFix) >= offset) {
            if (mBottomSheetBehaviorRef == null || mBottomSheetBehaviorRef.get() == null) {
                getBottomSheetBehavior(coordinatorLayout);
            }

            int collapsedY = bottomSheet.getHeight() - mBottomSheetBehaviorRef.get().getPeekHeight();

            if ((child.getY() + DyFix) > collapsedY) {
                for (int i = 0; i < child.getChildCount(); i++) {
                    View fab = child.getChildAt(i);
                    if (fab instanceof FloatingActionButton) {
                        ((FloatingActionButton) fab).hide();
                    }
                }
            } else {
                for (int i = 0; i < child.getChildCount(); i++) {
                    View fab = child.getChildAt(i);
                    if (fab instanceof FloatingActionButton) {
                        ((FloatingActionButton) fab).show();
                    }
                }
            }
        }

        return false;
    }

    private int getDyBetweenChildAndDependency(@NonNull View child, @NonNull View dependency) {
        if (dependency.getY() == 0 || dependency.getY() < offset) {
            return 0;
        }

        if ((dependency.getY() - child.getY()) > child.getHeight()) {
            return Math.max(0, (int) ((dependency.getY() - (child.getHeight() / 2)) - child.getY()));
        } else {
            return 0;
        }
    }

    /**
     * Look into the CoordiantorLayout for the {@link AnchorPointBottomSheetBehavior}
     *
     * @param coordinatorLayout with app:layout_behavior= {@link AnchorPointBottomSheetBehavior}
     */
    private void getBottomSheetBehavior(@NonNull CoordinatorLayout coordinatorLayout) {

        for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
            View child = coordinatorLayout.getChildAt(i);

            if (child instanceof NestedScrollView) {

                try {
                    AnchorPointBottomSheetBehavior temp = AnchorPointBottomSheetBehavior.from(child);
                    mBottomSheetBehaviorRef = new WeakReference<>(temp);
                    break;
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
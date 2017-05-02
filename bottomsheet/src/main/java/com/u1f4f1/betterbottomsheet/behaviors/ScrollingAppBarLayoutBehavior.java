package com.u1f4f1.betterbottomsheet.behaviors;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.u1f4f1.betterbottomsheet.R;

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
 */
public class ScrollingAppBarLayoutBehavior extends AppBarLayout.ScrollingViewBehavior {

    private boolean isInitialized = false;
    private Context context;
    private boolean isVisible = true;
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing {@link AnchorPointBottomSheetBehavior#peekHeight}
     * get changed dynamically we get the {@link NestedScrollView} that has
     * "app:layout_behavior=" {@link AnchorPointBottomSheetBehavior} inside the {@link CoordinatorLayout}
     */
    private WeakReference<AnchorPointBottomSheetBehavior> behaviorGoogleMapsLikeWeakReference;

    private ValueAnimator appBarValueAnimator;

    public ScrollingAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof NestedScrollView) {
            try {
                AnchorPointBottomSheetBehavior.from(dependency);
                return true;
            } catch (IllegalArgumentException ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (!isInitialized) {
            return init(parent, child, dependency);
        }

        if (behaviorGoogleMapsLikeWeakReference == null || behaviorGoogleMapsLikeWeakReference.get() == null) {
            getBottomSheetBehavior(parent);
        }

        setAppBarVisible((AppBarLayout) child, dependency.getY() >= dependency.getHeight() - behaviorGoogleMapsLikeWeakReference.get().getPeekHeight());

        return true;
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, View child) {
        return new SavedState(super.onSaveInstanceState(parent, child), isVisible);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, View child, Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(parent, child, savedState.getSuperState());
        this.isVisible = savedState.isVisible;
    }

    private boolean init(CoordinatorLayout parent, View child, View dependency) {
        /*
         * First we need to know if dependency view is upper or lower compared with
         * {@link BottomSheetBehaviorGoogleMapsLike#getPeekHeight()} Y position to know if need to show the AppBar at beginning.
         */
        getBottomSheetBehavior(parent);
        if (behaviorGoogleMapsLikeWeakReference == null || behaviorGoogleMapsLikeWeakReference.get() == null)
            getBottomSheetBehavior(parent);
        int mCollapsedY = dependency.getHeight() - behaviorGoogleMapsLikeWeakReference.get().getPeekHeight();
        isVisible = (dependency.getY() >= mCollapsedY);

        setStatusBarBackgroundVisible(isVisible);
        if (!isVisible) child.setY((int) child.getY() - child.getHeight() - getStatusBarHeight());
        isInitialized = true;
        /*
         * Following {@link #onDependentViewChanged} docs, we need to return true if the
         * Behavior changed the child view's size or position, false otherwise.
         * In our case we only move it if isVisible got false in this method.
         */
        return !isVisible;
    }

    public void setAppBarVisible(final AppBarLayout appBarLayout, final boolean visible) {

        if (visible == isVisible)
            return;

        if (appBarValueAnimator == null || !appBarValueAnimator.isRunning()) {

            appBarValueAnimator = ValueAnimator.ofFloat(
                    (int) appBarLayout.getY(),
                    visible ? (int) appBarLayout.getY() + appBarLayout.getHeight() + getStatusBarHeight() :
                            (int) appBarLayout.getY() - appBarLayout.getHeight() - getStatusBarHeight()
            );
            appBarValueAnimator.setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime));
            appBarValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    appBarLayout.setY((Float) animation.getAnimatedValue());

                }
            });
            appBarValueAnimator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    if (visible)
                        setStatusBarBackgroundVisible(true);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!visible)
                        setStatusBarBackgroundVisible(false);
                    isVisible = visible;
                    super.onAnimationEnd(animation);
                }
            });
            appBarValueAnimator.start();
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void setStatusBarBackgroundVisible(boolean visible) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (visible) {
                Window window = ((Activity) context).getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
            } else {
                Window window = ((Activity) context).getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(context, android.R.color.transparent));
            }
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
                    behaviorGoogleMapsLikeWeakReference = new WeakReference<>(temp);
                    break;
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }

    protected static class SavedState extends View.BaseSavedState {

        final boolean isVisible;

        public SavedState(Parcel source) {
            super(source);
            isVisible = source.readByte() != 0;
        }

        public SavedState(Parcelable superState, boolean visible) {
            super(superState);
            this.isVisible = visible;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (isVisible ? 1 : 0));
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
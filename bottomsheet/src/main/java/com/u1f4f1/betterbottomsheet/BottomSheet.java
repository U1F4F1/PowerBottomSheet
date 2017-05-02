package com.u1f4f1.betterbottomsheet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;

import com.u1f4f1.betterbottomsheet.behaviors.AnchorPointBottomSheetBehavior;

public class BottomSheet extends NestedScrollView implements AnchorPointBottomSheetBehavior.BottomSheetStateCallback {
    public static final String LOG_TAG = "BetterBottomSheet";

    private boolean isActive;

    private BottomSheetActiveCallback bottomSheetActiveCallback;

    public interface BottomSheetActiveCallback {
        void bottomSheetActive(boolean isActive);
    }

    public BottomSheet(Context context) {
        super(context);
    }

    public BottomSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        AnchorPointBottomSheetBehavior anchorPointBottomSheetBehavior = AnchorPointBottomSheetBehavior.from(this);
        if (anchorPointBottomSheetBehavior != null) {
            anchorPointBottomSheetBehavior.setBottomSheetStateCallback(this);
        }
    }

    public void setBottomSheetActiveCallback(BottomSheetActiveCallback bottomSheetActiveCallback) {
        this.bottomSheetActiveCallback = bottomSheetActiveCallback;
    }

    public void setActive(final boolean isActive) {
        this.isActive = isActive;

        ViewCompat.postOnAnimation(this, new Runnable() {
            @Override
            public void run() {
                if (bottomSheetActiveCallback != null) {
                    bottomSheetActiveCallback.bottomSheetActive(isActive);
                }
            }
        });
    }

    public boolean isActive() {
        return isActive;
    }

    public void reset() {
        ViewCompat.postOnAnimation(this, new Runnable() {
            @Override
            public void run() {
                setOverScrollMode(OVER_SCROLL_NEVER);
                fullScroll(FOCUS_UP);
            }
        });
    }

    /**
     * Force the {@link NestedScrollView} back to the top after we've collapsed
     */
    @Override
    public void onStateChanged(@NonNull View bottomSheet, @AnchorPointBottomSheetBehavior.State int newState) {
        if (newState == AnchorPointBottomSheetBehavior.STATE_COLLAPSED) {
            setActive(false);
            reset();
        }
    }
}

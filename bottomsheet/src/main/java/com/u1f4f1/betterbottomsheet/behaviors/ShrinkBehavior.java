package com.u1f4f1.betterbottomsheet.behaviors;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

import com.u1f4f1.betterbottomsheet.BottomSheet;

import java.util.List;

public class ShrinkBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    public ShrinkBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof BottomSheet;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        final List<View> dependencies = parent.getDependencies(child);

        for (int i = 0, z = dependencies.size(); i < z; i++) {
            final View view = dependencies.get(i);
            if (!(view instanceof BottomSheet)) {
                continue;
            }

            if (parent.doViewsOverlap(child, view)) {
                child.hide();
            } else {
                child.show();
            }
        }

        return false;
    }
}

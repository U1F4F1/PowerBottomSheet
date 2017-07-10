package com.u1f4f1.sample;

import android.util.Log;

import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetAdapter;
import com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior;

public class SampleAdapter extends BottomSheetAdapter {
    public SampleAdapter(AnchorPointBottomSheetBehavior<?> behavior) {
        super(behavior);
    }

    public void addFakeViews() {
        Log.v("ANDROIDISBAD", "adding fake views to adapter");
        for (int i = 0; i < 10; i++) {
            addModel(new HoursModel_().buttonClickConsumer(recyclerViewTransitionRunnable));
        }
    }
}

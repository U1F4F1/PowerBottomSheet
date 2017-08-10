package com.u1f4f1.sample;

import com.u1f4f1.powerbottomsheet.bottomsheet.BottomSheetAdapter;
import com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior;

import static com.u1f4f1.powerbottomsheet.LogKt.trace;

public class SampleAdapter extends BottomSheetAdapter {
    public SampleAdapter(AnchorPointBottomSheetBehavior<?> behavior) {
        super(behavior);
    }

    public void addFakeViews() {
        trace("adding fake views to adapter");
        for (int i = 0; i < 10; i++) {
            addModel(new HoursModel_().buttonClickConsumer(recyclerViewTransitionRunnable));
        }
    }
}

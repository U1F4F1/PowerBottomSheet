package com.u1f4f1.sample;

import android.view.View;

import com.airbnb.epoxy.EpoxyAttribute;
import com.airbnb.epoxy.EpoxyModel;
import com.airbnb.epoxy.EpoxyModelClass;

@EpoxyModelClass(layout = R.layout.model_business_hours)
public abstract class HoursModel extends EpoxyModel<HoursView> {
    @EpoxyAttribute(hash = false) public Runnable buttonClickConsumer;

    @Override
    public void bind(final HoursView view) {
        view.hoursContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClickConsumer.run();

                view.cardClicked();
            }
        });
    }
}

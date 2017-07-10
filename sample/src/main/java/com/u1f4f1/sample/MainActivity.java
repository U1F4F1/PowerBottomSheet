package com.u1f4f1.sample;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.airbnb.epoxy.EpoxyModel;
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheet;
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetAdapter;
import com.u1f4f1.betterbottomsheet.bottomsheet.BottomSheetState;
import com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    AnchorPointBottomSheetBehavior<BottomSheet> bottomSheetBehavior;
    SampleAdapter sampleAdapter;

    private BottomSheet bottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final int activeColor = ContextCompat.getColor(this, R.color.colorAccent);
        final int inactiveColor = ContextCompat.getColor(this, R.color.colorPrimaryDark);

        bottomSheet = ((BottomSheet) findViewById(R.id.bottom_sheet));

        bottomSheetBehavior = AnchorPointBottomSheetBehavior.Companion.from(bottomSheet);

        bottomSheet.setActivatedListener(new BottomSheet.OnSheetActivatedListener() {
            @Override
            public void isActivated(boolean isActive) {
                if (isActive) {
                    bottomSheet.setBackgroundColor(activeColor);
                } else {
                    bottomSheet.setBackgroundColor(inactiveColor);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sampleAdapter = new SampleAdapter(bottomSheetBehavior);
        bottomSheet.setupRecyclerview(sampleAdapter);
    }

    @OnClick(R.id.button)
    void onClick() {
        sampleAdapter.addFakeViews();
        bottomSheetBehavior.setState(BottomSheetState.STATE_COLLAPSED);
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetState.STATE_EXPANDED || bottomSheetBehavior.getState() == BottomSheetState.STATE_ANCHOR_POINT) {
            bottomSheetBehavior.setState(BottomSheetState.STATE_COLLAPSED);
            return;
        }

        if (bottomSheetBehavior.getState() == BottomSheetState.STATE_COLLAPSED) {
            bottomSheetBehavior.setState(BottomSheetState.STATE_HIDDEN);
            return;
        }

        if (bottomSheetBehavior.getState() == BottomSheetState.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetState.STATE_COLLAPSED);
            return;
        }

        super.onBackPressed();
    }
}

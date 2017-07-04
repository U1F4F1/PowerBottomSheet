//package com.u1f4f1.sample;
//
//import android.os.Bundle;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
//
//import com.u1f4f1.betterbottomsheet.BottomSheet;
//import com.u1f4f1.betterbottomsheet.behaviors.AnchorPointBottomSheetBehavior;
//
//public class MainActivity extends AppCompatActivity {
//
//    AnchorPointBottomSheetBehavior<BottomSheet> bottomSheetBehavior;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        final int activeColor = ContextCompat.getColor(this, R.color.colorAccent);
//        final int inactiveColor = ContextCompat.getColor(this, R.color.colorPrimaryDark);
//
//        final BottomSheet bottomSheet = ((BottomSheet) findViewById(R.id.bottom_sheet));
//
//        bottomSheetBehavior = AnchorPointBottomSheetBehavior.from(bottomSheet);
//        bottomSheetBehavior.setState(AnchorPointBottomSheetBehavior.STATE_COLLAPSED);
//        bottomSheet.setBottomSheetActiveCallback(new BottomSheet.BottomSheetActiveCallback() {
//            @Override
//            public void bottomSheetActive(boolean isActive) {
//                if (isActive) {
//                    bottomSheet.setBackgroundColor(activeColor);
//                } else {
//                    bottomSheet.setBackgroundColor(inactiveColor);
//                }
//            }
//        });
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (bottomSheetBehavior.getState() == AnchorPointBottomSheetBehavior.STATE_EXPANDED || bottomSheetBehavior.getState() == AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT) {
//            bottomSheetBehavior.setState(AnchorPointBottomSheetBehavior.STATE_COLLAPSED);
//            return;
//        }
//
//        if (bottomSheetBehavior.getState() == AnchorPointBottomSheetBehavior.STATE_COLLAPSED) {
//            bottomSheetBehavior.setState(AnchorPointBottomSheetBehavior.STATE_HIDDEN);
//            return;
//        }
//
//        if (bottomSheetBehavior.getState() == AnchorPointBottomSheetBehavior.STATE_HIDDEN) {
//            bottomSheetBehavior.setState(AnchorPointBottomSheetBehavior.STATE_COLLAPSED);
//            return;
//        }
//
//        super.onBackPressed();
//    }
//}

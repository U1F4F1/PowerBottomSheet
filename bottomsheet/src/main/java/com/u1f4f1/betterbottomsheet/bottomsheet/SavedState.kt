package com.u1f4f1.betterbottomsheet.bottomsheet

import android.os.Parcel
import android.os.Parcelable
import com.u1f4f1.betterbottomsheet.createParcel

data class SavedState(var bottomSheetState: BottomSheetState) : Parcelable {
    companion object {
        @JvmField @Suppress("unused")
        val CREATOR = createParcel { SavedState(it) }
    }

    constructor(parcelIn: Parcel) : this(BottomSheetState.fromInt(parcelIn.readInt()))

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(bottomSheetState.ordinal)
    }

    override fun describeContents() = 0
}
package com.u1f4f1.powerbottomsheet.bottomsheet

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

class SavedState(val bottomSheetState: Int): Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(bottomSheetState)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SavedState> {
        override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)

        override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
    }
}
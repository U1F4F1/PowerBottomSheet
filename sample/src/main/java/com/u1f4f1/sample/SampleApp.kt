package com.u1f4f1.sample

import android.app.Application

import com.facebook.stetho.Stetho
import com.mooveit.library.Fakeit
import java.util.*

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        Fakeit.initWithLocale(Locale.ENGLISH)
    }
}
